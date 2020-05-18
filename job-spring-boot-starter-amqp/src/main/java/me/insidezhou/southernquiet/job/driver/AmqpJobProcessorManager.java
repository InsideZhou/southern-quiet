package me.insidezhou.southernquiet.job.driver;

import me.insidezhou.southernquiet.Constants;
import me.insidezhou.southernquiet.amqp.rabbit.AmqpAutoConfiguration;
import me.insidezhou.southernquiet.amqp.rabbit.AmqpMessageRecover;
import me.insidezhou.southernquiet.amqp.rabbit.DirectRabbitListenerContainerFactoryConfigurer;
import me.insidezhou.southernquiet.job.AmqpJobAutoConfiguration;
import me.insidezhou.southernquiet.job.JobProcessor;
import me.insidezhou.southernquiet.util.Amplifier;
import me.insidezhou.southernquiet.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.*;

public class AmqpJobProcessorManager extends AbstractJobProcessorManager implements Lifecycle, RabbitListenerConfigurer {
    private final static Logger log = LoggerFactory.getLogger(AmqpJobProcessorManager.class);

    private final SmartMessageConverter messageConverter;
    private final ConnectionFactory connectionFactory;

    private final List<Tuple<RabbitListenerEndpoint, JobProcessor, String>> listenerEndpoints = new ArrayList<>();
    private final AmqpAutoConfiguration.Properties amqpProperties;
    private final AmqpJobAutoConfiguration.Properties amqpJobProperties;
    private final Amplifier amplifier;

    private final RabbitProperties rabbitProperties;
    private final RabbitAdmin rabbitAdmin;
    private final RabbitTemplate rabbitTemplate;

    public AmqpJobProcessorManager(RabbitAdmin rabbitAdmin,
                                   AmqpJobArranger<?> jobArranger,
                                   Amplifier amplifier,
                                   AmqpJobAutoConfiguration.Properties amqpJobProperties,
                                   AmqpAutoConfiguration.Properties amqpProperties,
                                   RabbitTransactionManager transactionManager,
                                   RabbitProperties rabbitProperties,
                                   ApplicationContext applicationContext
    ) {
        super(applicationContext);

        this.amplifier = amplifier;
        this.rabbitAdmin = rabbitAdmin;
        this.amqpJobProperties = amqpJobProperties;
        this.amqpProperties = amqpProperties;
        this.rabbitProperties = rabbitProperties;

        this.messageConverter = jobArranger.getMessageConverter();

        this.connectionFactory = transactionManager.getConnectionFactory();
        this.rabbitTemplate = new RabbitTemplate(connectionFactory);
    }

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
        listenerEndpoints.forEach(tuple -> {
            RabbitListenerEndpoint endpoint = tuple.getFirst();
            JobProcessor processor = tuple.getSecond();
            String listenerName = tuple.getThird();
            Amplifier amplifier = this.amplifier;

            if (!StringUtils.isEmpty(processor.amplifierBeanName())) {
                amplifier = applicationContext.getBean(processor.amplifierBeanName(), Amplifier.class);
            }

            DirectRabbitListenerContainerFactoryConfigurer containerFactoryConfigurer = new DirectRabbitListenerContainerFactoryConfigurer(
                rabbitProperties,
                new AmqpMessageRecover(
                    rabbitTemplate,
                    amplifier,
                    Constants.AMQP_DEFAULT,
                    getDeadRouting(processor, listenerName),
                    amqpProperties
                ),
                amqpProperties
            );

            DirectRabbitListenerContainerFactory factory = new DirectRabbitListenerContainerFactory();
            factory.setMessageConverter(messageConverter);
            factory.setAcknowledgeMode(amqpProperties.getAcknowledgeMode());
            containerFactoryConfigurer.configure(factory, connectionFactory);

            registrar.registerEndpoint(endpoint, factory);
        });
    }

    @Override
    protected void initProcessor(JobProcessor jobProcessor, Object bean, Method method) {
        String listenerDefaultName = method.getName();
        String listenerName = getListenerName(jobProcessor, listenerDefaultName);
        String listenerRouting = getListenerRouting(jobProcessor, listenerDefaultName);

        listenerEndpoints.stream()
            .filter(listenerEndpoint -> jobProcessor.job() == listenerEndpoint.getSecond().job() && listenerName.equals(listenerEndpoint.getThird()))
            .findAny()
            .ifPresent(listenerEndpoint -> log.warn(
                "监听器重复: queue={}, listener={}#{}, job={}",
                listenerRouting,
                bean.getClass().getName(),
                listenerDefaultName,
                jobProcessor.job().getSimpleName()
            ));

        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
        endpoint.setId(UUID.randomUUID().toString());
        endpoint.setQueueNames(listenerRouting);
        endpoint.setAdmin(rabbitAdmin);

        declareExchangeAndQueue(jobProcessor, listenerDefaultName);

        Class<?> jobClass = jobProcessor.job();
        ParameterizedTypeReference<?> typeReference = ParameterizedTypeReference.forType(jobClass);

        endpoint.setMessageListener(message -> {
            Object job = messageConverter.fromMessage(message, typeReference);

            if (log.isDebugEnabled()) {
                log.debug(
                    "监听器收到通知: queue={}, listener={}#{}({}), job={}, message={}",
                    endpoint.getQueueNames(),
                    bean.getClass().getName(),
                    listenerDefaultName,
                    endpoint.getId(),
                    job.getClass().getSimpleName(),
                    message
                );
            }

            Object[] parameters = Arrays.stream(method.getParameters())
                .map(parameter -> {
                    Class<?> parameterClass = parameter.getType();

                    if (parameterClass.isInstance(job)) {
                        return job;
                    }
                    else if (parameterClass.isInstance(jobProcessor)) {
                        return jobProcessor;
                    }
                    else {
                        log.warn("不支持在任务监听器中使用此类型的参数\tparameter={}, job={}", parameter.getClass(), jobClass);

                        try {
                            return parameterClass.newInstance();
                        }
                        catch (Exception e) {
                            return null;
                        }
                    }
                })
                .toArray();

            try {
                method.invoke(bean, parameters);
            }
            catch (RuntimeException e) {
                log.error("任务处理器抛出异常", e);

                throw e;
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        listenerEndpoints.add(new Tuple<>(endpoint, jobProcessor, listenerName));
    }

    private String getDeadSource(JobProcessor listener, String listenerDefaultName) {
        return suffix("DEAD." + AmqpJobArranger.getQueueSource(listener.job()), listener, listenerDefaultName);
    }

    private String getDeadRouting(JobProcessor listener, String listenerDefaultName) {
        return AmqpJobArranger.getRouting(amqpJobProperties.getNamePrefix(), getDeadSource(listener, listenerDefaultName));
    }

    private String getListenerRouting(JobProcessor listener, String listenerDefaultName) {
        return suffix(AmqpJobArranger.getRouting(amqpJobProperties.getNamePrefix(), listener.job()), listener, listenerDefaultName);
    }

    private String getListenerName(JobProcessor listener, String listenerDefaultName) {
        String listenerName = listener.name();
        if (StringUtils.isEmpty(listenerName)) {
            listenerName = listenerDefaultName;
        }

        Assert.hasText(listenerName, "处理器的名称不能为空");
        return listenerName;
    }

    private String suffix(String routing, JobProcessor listener, String listenerDefaultName) {
        return routing + "#" + getListenerName(listener, listenerDefaultName);
    }

    private void declareExchangeAndQueue(JobProcessor listener, String listenerDefaultName) {
        String routing = getListenerRouting(listener, listenerDefaultName);

        Exchange exchange = new FanoutExchange(AmqpJobArranger.getExchange(amqpJobProperties.getNamePrefix(), listener.job()));
        Queue queue = new Queue(routing);

        rabbitAdmin.declareExchange(exchange);
        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(routing).noargs());

        Map<String, Object> deadQueueArgs = new HashMap<>();
        deadQueueArgs.put(Constants.AMQP_DLX, Constants.AMQP_DEFAULT);
        deadQueueArgs.put(Constants.AMQP_DLK, queue.getName());

        Queue deadRouting = new Queue(getDeadRouting(listener, listenerDefaultName), true, false, false, deadQueueArgs);
        rabbitAdmin.declareQueue(deadRouting);
    }

    @Override
    public void start() {
        rabbitTemplate.start();
    }

    @Override
    public void stop() {
        rabbitTemplate.stop();
    }

    @Override
    public boolean isRunning() {
        return rabbitTemplate.isRunning();
    }

    public RabbitTemplate getRabbitTemplate() {
        return rabbitTemplate;
    }
}
