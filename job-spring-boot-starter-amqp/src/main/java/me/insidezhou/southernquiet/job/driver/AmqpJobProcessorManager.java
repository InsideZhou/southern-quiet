package me.insidezhou.southernquiet.job.driver;

import me.insidezhou.southernquiet.Constants;
import me.insidezhou.southernquiet.amqp.rabbit.*;
import me.insidezhou.southernquiet.job.AmqpJobAutoConfiguration;
import me.insidezhou.southernquiet.job.JobProcessor;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import me.insidezhou.southernquiet.util.Amplifier;
import me.insidezhou.southernquiet.util.Tuple;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class AmqpJobProcessorManager extends AbstractJobProcessorManager implements Lifecycle, RabbitListenerConfigurer {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(AmqpJobProcessorManager.class);

    private final SmartMessageConverter messageConverter;
    private final ConnectionFactory connectionFactory;

    private final List<Tuple<RabbitListenerEndpoint, JobProcessor, String>> listenerEndpoints = new ArrayList<>();
    private final AmqpAutoConfiguration.Properties amqpProperties;
    private final AmqpJobAutoConfiguration.Properties amqpJobProperties;
    private final Amplifier amplifier;

    private final RabbitProperties rabbitProperties;
    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;

    public AmqpJobProcessorManager(AmqpAdmin amqpAdmin,
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
        this.amqpAdmin = amqpAdmin;
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
                    getDeadRouting(amqpJobProperties.getNamePrefix(), processor, listenerName),
                    AbstractAmqpJobArranger.getExchange(amqpJobProperties.getNamePrefix(), processor.job()),
                    getProcessorRouting(processor, listenerName),
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
        String processorName = getProcessorName(jobProcessor, method);
        String listenerRouting = getProcessorRouting(jobProcessor, processorName);

        DelayedMessage delayedAnnotation = AnnotatedElementUtils.findMergedAnnotation(jobProcessor.job(), DelayedMessage.class);

        listenerEndpoints.stream()
            .filter(listenerEndpoint -> jobProcessor.job() == listenerEndpoint.getSecond().job() && processorName.equals(listenerEndpoint.getThird()))
            .findAny()
            .ifPresent(listenerEndpoint -> log.message("任务处理器重复")
                .context(context -> {
                    context.put("queue", listenerRouting);
                    context.put("listener", bean.getClass().getName());
                    context.put("listenerName", processorName);
                    context.put("job", jobProcessor.job().getSimpleName());
                })
            );

        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
        endpoint.setId(UUID.randomUUID().toString());
        endpoint.setQueueNames(listenerRouting);
        endpoint.setAdmin(amqpAdmin);

        declareExchangeAndQueue(jobProcessor, processorName);

        Class<?> jobClass = jobProcessor.job();
        ParameterizedTypeReference<?> typeReference = ParameterizedTypeReference.forType(jobClass);

        endpoint.setMessageListener(message -> {
            Object job = messageConverter.fromMessage(message, typeReference);

            log.message("接到任务")
                .context(context -> {
                    context.put("queue", endpoint.getQueueNames());
                    context.put("listener", bean.getClass().getName());
                    context.put("listenerName", processorName);
                    context.put("listenerId", endpoint.getId());
                    context.put("job", job.getClass().getSimpleName());
                    context.put("message", message);
                })
                .debug();

            Object[] parameters = Arrays.stream(method.getParameters())
                .map(parameter -> {
                    Class<?> parameterClass = parameter.getType();

                    if (parameterClass.isInstance(job)) {
                        return job;
                    }
                    else if (parameterClass.isInstance(jobProcessor)) {
                        return jobProcessor;
                    }
                    else if (parameterClass.equals(DelayedMessage.class)) {
                        return delayedAnnotation;
                    }
                    else {
                        log.message("不支持在通知监听器中使用此类型的参数")
                            .context("parameter", parameter.getClass())
                            .context("job", jobClass)
                            .warn();

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
                log.message("任务处理器抛出异常").exception(e).error();

                throw e;
            }
            catch (InvocationTargetException e) {
                Throwable target = e.getTargetException();

                log.message("任务处理器抛出异常").exception(target).error();

                if (target instanceof RuntimeException) {
                    throw (RuntimeException) target;
                }

                throw new RuntimeException(target);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        listenerEndpoints.add(new Tuple<>(endpoint, jobProcessor, processorName));
    }

    public final static String DeadMark = "DEAD.";
    public final static String RetryMark = "RETRY.";

    public static String getDeadRouting(String prefix, JobProcessor processor, String processorName) {
        return AbstractAmqpNotificationPublisher.getRouting(
            prefix,
            suffix(DeadMark + AbstractAmqpNotificationPublisher.getNotificationSource(processor.job()), processorName));
    }

    public static String getRetryRouting(String prefix, JobProcessor processor, String processorName) {
        return AbstractAmqpNotificationPublisher.getRouting(
            prefix,
            suffix(RetryMark + AbstractAmqpNotificationPublisher.getNotificationSource(processor.job()), processorName));
    }

    private String getProcessorRouting(JobProcessor processor, String processorName) {
        return suffix(AbstractAmqpNotificationPublisher.getRouting(amqpJobProperties.getNamePrefix(), processor.job()), processorName);
    }

    private String getProcessorName(JobProcessor listener, Method method) {
        String listenerName = listener.name();
        if (StringUtils.isEmpty(listenerName)) {
            listenerName = method.getName();
        }

        Assert.hasText(listenerName, "处理器的名称不能为空");
        return listenerName;
    }

    public static String suffix(String routing, String listenerName) {
        return routing + "#" + listenerName;
    }

    private void declareExchangeAndQueue(JobProcessor processor, String processorName) {
        String listenerRouting = getProcessorRouting(processor, processorName);

        Map<String, Object> exchangeArguments = new HashMap<>();
        exchangeArguments.put(Constants.AMQP_DELAYED_TYPE, "fanout");
        Exchange exchange = new CustomExchange(
            AbstractAmqpJobArranger.getExchange(amqpJobProperties.getNamePrefix(), processor.job()),
            Constants.AMQP_DELAYED_EXCHANGE,
            true,
            false,
            exchangeArguments
        );

        Queue queue = new Queue(listenerRouting);

        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(listenerRouting).noargs());

        Map<String, Object> deadQueueArgs = new HashMap<>();
        deadQueueArgs.put(Constants.AMQP_DLX, Constants.AMQP_DEFAULT);
        deadQueueArgs.put(Constants.AMQP_DLK, queue.getName());

        Queue deadRouting = new Queue(getDeadRouting(amqpJobProperties.getNamePrefix(), processor, processorName), true, false, false, deadQueueArgs);
        amqpAdmin.declareQueue(deadRouting);
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
