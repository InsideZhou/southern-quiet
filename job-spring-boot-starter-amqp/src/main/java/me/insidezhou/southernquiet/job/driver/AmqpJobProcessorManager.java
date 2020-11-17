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

    private final List<Tuple<RabbitListenerEndpoint, JobProcessor, String>> processorEndpoints = new ArrayList<>();
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
        processorEndpoints.forEach(tuple -> {
            RabbitListenerEndpoint endpoint = tuple.getFirst();
            JobProcessor processor = tuple.getSecond();
            String processorName = tuple.getThird();
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
                    getDeadRouting(amqpJobProperties.getNamePrefix(), processor, processorName),
                    AbstractAmqpJobArranger.getDelayRouting(amqpJobProperties.getNamePrefix(), processor.job()),
                    getRetryRouting(amqpJobProperties.getNamePrefix(), processor, processorName),
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
        String processorRouting = getProcessorRouting(jobProcessor, processorName);

        DelayedMessage delayedAnnotation = AnnotatedElementUtils.findMergedAnnotation(jobProcessor.job(), DelayedMessage.class);

        processorEndpoints.stream()
            .filter(processorEndpoint -> jobProcessor.job() == processorEndpoint.getSecond().job() && processorName.equals(processorEndpoint.getThird()))
            .findAny()
            .ifPresent(processorEndpoint -> log.message("任务处理器重复")
                .context(context -> {
                    context.put("queue", processorRouting);
                    context.put("processor", bean.getClass().getName());
                    context.put("processorName", processorName);
                    context.put("job", jobProcessor.job().getSimpleName());
                })
            );

        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
        endpoint.setId(UUID.randomUUID().toString());
        endpoint.setQueueNames(processorRouting);
        endpoint.setAdmin(amqpAdmin);

        declareExchangeAndQueue(jobProcessor, processorName);

        Class<?> jobClass = jobProcessor.job();
        ParameterizedTypeReference<?> typeReference = ParameterizedTypeReference.forType(jobClass);

        endpoint.setMessageListener(message -> {
            Object job = messageConverter.fromMessage(message, typeReference);

            log.message("接到任务")
                .context(context -> {
                    context.put("queue", endpoint.getQueueNames());
                    context.put("processor", bean.getClass().getName());
                    context.put("processorName", processorName);
                    context.put("processorId", endpoint.getId());
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

        processorEndpoints.add(new Tuple<>(endpoint, jobProcessor, processorName));
    }

    public static String getDeadRouting(String prefix, JobProcessor processor, String processorName) {
        return AbstractAmqpJobArranger.getRouting(
            prefix,
            suffix("DEAD." + AbstractAmqpJobArranger.getQueueSource(processor.job()), processorName));
    }

    public static String getRetryRouting(String prefix, JobProcessor processor, String processorName) {
        return AbstractAmqpJobArranger.getRouting(
            prefix,
            suffix("RETRY." + AbstractAmqpJobArranger.getQueueSource(processor.job()), processorName));
    }

    private String getProcessorRouting(JobProcessor processor, String processorName) {
        return suffix(AbstractAmqpJobArranger.getRouting(amqpJobProperties.getNamePrefix(), processor.job()), processorName);
    }

    private String getProcessorName(JobProcessor processor, Method method) {
        String processorName = processor.name();
        if (StringUtils.isEmpty(processorName)) {
            processorName = method.getName();
        }

        Assert.hasText(processorName, "处理器的名称不能为空");
        return processorName;
    }

    public static String suffix(String routing, String processorName) {
        return routing + "#" + processorName;
    }

    private void declareExchangeAndQueue(JobProcessor processor, String processorName) {
        String routing = AbstractAmqpJobArranger.getRouting(amqpJobProperties.getNamePrefix(), processor.job());
        String delayRouting = AbstractAmqpJobArranger.getDelayRouting(amqpJobProperties.getNamePrefix(), processor.job());
        String processorRouting = getProcessorRouting(processor, processorName);

        Exchange exchange = new FanoutExchange(routing, true, false);
        Queue queue = new Queue(processorRouting);

        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(processorRouting).noargs());

        Map<String, Object> deadQueueArgs = new HashMap<>();
        deadQueueArgs.put(Constants.AMQP_DLX, Constants.AMQP_DEFAULT);
        deadQueueArgs.put(Constants.AMQP_DLK, queue.getName());

        Queue deadRouting = new Queue(getDeadRouting(amqpJobProperties.getNamePrefix(), processor, processorName), true, false, false, deadQueueArgs);
        amqpAdmin.declareQueue(deadRouting);

        Map<String, Object> exchangeArguments = new HashMap<>();
        exchangeArguments.put(Constants.AMQP_DELAYED_TYPE, "direct");
        Exchange delayExchange = new CustomExchange(
            delayRouting,
            Constants.AMQP_DELAYED_EXCHANGE,
            true,
            false,
            exchangeArguments
        );

        amqpAdmin.declareExchange(delayExchange);

        Map<String, Object> retryQueueArgs = new HashMap<>();
        retryQueueArgs.put(Constants.AMQP_DLX, Constants.AMQP_DEFAULT);
        retryQueueArgs.put(Constants.AMQP_DLK, queue.getName());
        retryQueueArgs.put(Constants.AMQP_MESSAGE_TTL, 0); //这里的硬编码是为了消息到达队列之后立即转发至相应的工作队列。下同。
        Queue retryQueue = new Queue(
            getRetryRouting(amqpJobProperties.getNamePrefix(), processor, processorName),
            true,
            false,
            false,
            retryQueueArgs
        );
        amqpAdmin.declareQueue(retryQueue);
        amqpAdmin.declareBinding(BindingBuilder.bind(retryQueue).to(delayExchange).with(retryQueue.getName()).noargs());

        Map<String, Object> delayQueueArgs = new HashMap<>();
        delayQueueArgs.put(Constants.AMQP_DLX, routing);
        delayQueueArgs.put(Constants.AMQP_DLK, routing);
        delayQueueArgs.put(Constants.AMQP_MESSAGE_TTL, 0);
        Queue delayQueue = new Queue(
            AbstractAmqpJobArranger.getDelayRouting(amqpJobProperties.getNamePrefix(), processor.job()),
            true,
            false,
            false,
            delayQueueArgs
        );
        amqpAdmin.declareQueue(delayQueue);
        amqpAdmin.declareBinding(BindingBuilder.bind(delayQueue).to(delayExchange).with(delayQueue.getName()).noargs());
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
