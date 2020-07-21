package me.insidezhou.southernquiet.notification.driver;

import me.insidezhou.southernquiet.Constants;
import me.insidezhou.southernquiet.amqp.rabbit.*;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import me.insidezhou.southernquiet.notification.AmqpNotificationAutoConfiguration;
import me.insidezhou.southernquiet.notification.NotificationListener;
import me.insidezhou.southernquiet.util.Amplifier;
import me.insidezhou.southernquiet.util.Tuple;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import org.springframework.beans.factory.ObjectProvider;
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

public class AmqpNotificationListenerManager extends AbstractNotificationListenerManager implements Lifecycle, RabbitListenerConfigurer {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(AmqpNotificationListenerManager.class);

    private final SmartMessageConverter messageConverter;
    private final CachingConnectionFactory cachingConnectionFactory;

    private final List<Tuple<RabbitListenerEndpoint, NotificationListener, String>> listenerEndpoints = new ArrayList<>();
    private final AmqpAutoConfiguration.Properties amqpProperties;
    private final AmqpNotificationAutoConfiguration.Properties amqpNotificationProperties;
    private final Amplifier amplifier;

    private final RabbitProperties rabbitProperties;
    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;

    public AmqpNotificationListenerManager(AmqpAdmin amqpAdmin,
                                           AmqpNotificationPublisher<?> publisher,
                                           Amplifier amplifier,
                                           AmqpNotificationAutoConfiguration.Properties amqpNotificationProperties,
                                           AmqpAutoConfiguration.Properties amqpProperties,
                                           RabbitProperties rabbitProperties,
                                           RabbitConnectionFactoryBean factoryBean,
                                           ObjectProvider<ConnectionNameStrategy> connectionNameStrategy,
                                           ApplicationContext applicationContext
    ) {
        super(applicationContext);

        this.amplifier = amplifier;
        this.amqpAdmin = amqpAdmin;
        this.amqpNotificationProperties = amqpNotificationProperties;
        this.amqpProperties = amqpProperties;
        this.rabbitProperties = rabbitProperties;

        this.messageConverter = publisher.getMessageConverter();

        this.cachingConnectionFactory = AmqpAutoConfiguration.rabbitConnectionFactory(rabbitProperties, factoryBean, connectionNameStrategy);
        this.rabbitTemplate = new RabbitTemplate(cachingConnectionFactory);
    }

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
        listenerEndpoints.forEach(tuple -> {
            RabbitListenerEndpoint endpoint = tuple.getFirst();
            NotificationListener listenerAnnotation = tuple.getSecond();
            String listenerName = tuple.getThird();
            Amplifier amplifier = this.amplifier;

            if (!StringUtils.isEmpty(listenerAnnotation.amplifierBeanName())) {
                amplifier = applicationContext.getBean(listenerAnnotation.amplifierBeanName(), Amplifier.class);
            }

            DirectRabbitListenerContainerFactoryConfigurer containerFactoryConfigurer = new DirectRabbitListenerContainerFactoryConfigurer(
                rabbitProperties,
                new AmqpMessageRecover(
                    rabbitTemplate,
                    amplifier,
                    Constants.AMQP_DEFAULT,
                    getDeadRouting(amqpNotificationProperties.getNamePrefix(), listenerAnnotation, listenerName),
                    Constants.AMQP_DEFAULT,
                    getRetryRouting(amqpNotificationProperties.getNamePrefix(), listenerAnnotation, listenerName),
                    amqpProperties
                ),
                amqpProperties
            );

            DirectRabbitListenerContainerFactory factory = new DirectRabbitListenerContainerFactory();
            factory.setMessageConverter(messageConverter);
            factory.setAcknowledgeMode(amqpProperties.getAcknowledgeMode());
            containerFactoryConfigurer.configure(factory, cachingConnectionFactory);

            registrar.registerEndpoint(endpoint, factory);
        });
    }

    @Override
    protected void initListener(NotificationListener listener, Object bean, Method method) {
        String listenerName = getListenerName(listener, method);
        String listenerRouting = getListenerRouting(listener, listenerName);

        DelayedMessage delayedAnnotation = AnnotatedElementUtils.findMergedAnnotation(listener.notification(), DelayedMessage.class);

        listenerEndpoints.stream()
            .filter(listenerEndpoint -> listener.notification() == listenerEndpoint.getSecond().notification() && listenerName.equals(listenerEndpoint.getThird()))
            .findAny()
            .ifPresent(listenerEndpoint -> log.message("监听器重复")
                .context(context -> {
                    context.put("queue", listenerRouting);
                    context.put("listener", bean.getClass().getName());
                    context.put("listenerName", listenerName);
                    context.put("notification", listener.notification().getSimpleName());
                })
                .warn());

        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
        endpoint.setId(UUID.randomUUID().toString());
        endpoint.setQueueNames(listenerRouting);
        endpoint.setAdmin(amqpAdmin);

        declareExchangeAndQueue(listener, listenerName);

        Class<?> notificationClass = listener.notification();
        ParameterizedTypeReference<?> typeReference = ParameterizedTypeReference.forType(notificationClass);

        endpoint.setMessageListener(message -> {
            Object notification = messageConverter.fromMessage(message, typeReference);

            log.message("收到通知")
                .context(context -> {
                    context.put("queue", endpoint.getQueueNames());
                    context.put("listener", bean.getClass().getName());
                    context.put("listenerName", listenerName);
                    context.put("listenerId", endpoint.getId());
                    context.put("notification", notification.getClass().getSimpleName());
                    context.put("message", message);
                })
                .debug();

            Object[] parameters = Arrays.stream(method.getParameters())
                .map(parameter -> {
                    Class<?> parameterClass = parameter.getType();

                    if (parameterClass.isInstance(notification)) {
                        return notification;
                    }
                    else if (parameterClass.isInstance(listener)) {
                        return listener;
                    }
                    else if (parameterClass.equals(DelayedMessage.class)) {
                        return delayedAnnotation;
                    }
                    else {
                        log.message("不支持在通知监听器中使用此类型的参数")
                            .context("parameter", parameter.getClass())
                            .context("notification", notificationClass)
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
                log.message("通知处理器抛出异常").exception(e).error();
                throw e;
            }
            catch (InvocationTargetException e) {
                Throwable target = e.getTargetException();

                log.message("通知处理器抛出异常").exception(target).error();

                if (target instanceof RuntimeException) {
                    throw (RuntimeException) target;
                }

                throw new RuntimeException(target);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        listenerEndpoints.add(new Tuple<>(endpoint, listener, listenerName));
    }

    public final static String DeadMark = "DEAD.";
    public final static String RetryMark = "RETRY.";

    public static String getDeadRouting(String prefix, NotificationListener listener, String listenerName) {
        return AbstractAmqpNotificationPublisher.getRouting(
            prefix,
            suffix(DeadMark + AbstractAmqpNotificationPublisher.getNotificationSource(listener.notification()), listenerName));
    }

    public static String getRetryRouting(String prefix, NotificationListener listener, String listenerName) {
        return AbstractAmqpNotificationPublisher.getRouting(
            prefix,
            suffix(RetryMark + AbstractAmqpNotificationPublisher.getNotificationSource(listener.notification()), listenerName));
    }

    private String getListenerRouting(NotificationListener listener, String listenerName) {
        return suffix(AbstractAmqpNotificationPublisher.getRouting(amqpNotificationProperties.getNamePrefix(), listener.notification()), listenerName);
    }

    private String getListenerName(NotificationListener listener, Method method) {
        String listenerName = listener.name();
        if (StringUtils.isEmpty(listenerName)) {
            listenerName = method.getName();
        }

        Assert.hasText(listenerName, "监听器的名称不能为空");
        return listenerName;
    }

    public static String suffix(String routing, String listenerName) {
        return routing + "#" + listenerName;
    }

    private void declareExchangeAndQueue(NotificationListener listener, String listenerName) {
        String routing = AbstractAmqpNotificationPublisher.getRouting(amqpNotificationProperties.getNamePrefix(), listener.notification());
        String delayRouting = AbstractAmqpNotificationPublisher.getDelayedRouting(amqpNotificationProperties.getNamePrefix(), listener.notification());
        String listenerRouting = getListenerRouting(listener, listenerName);

        Exchange exchange = new FanoutExchange(AbstractAmqpNotificationPublisher.getExchange(amqpNotificationProperties.getNamePrefix(), listener.notification()));
        Queue queue = new Queue(listenerRouting);

        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(listenerRouting).noargs());

        Map<String, Object> deadQueueArgs = new HashMap<>();
        deadQueueArgs.put(Constants.AMQP_DLX, Constants.AMQP_DEFAULT);
        deadQueueArgs.put(Constants.AMQP_DLK, queue.getName());

        Queue deadRouting = new Queue(getDeadRouting(amqpNotificationProperties.getNamePrefix(), listener, listenerName), true, false, false, deadQueueArgs);
        amqpAdmin.declareQueue(deadRouting);

        Map<String, Object> retryQueueArgs = new HashMap<>();
        retryQueueArgs.put(Constants.AMQP_DLX, Constants.AMQP_DEFAULT);
        retryQueueArgs.put(Constants.AMQP_DLK, queue.getName());

        Queue retryRouting = new Queue(getRetryRouting(amqpNotificationProperties.getNamePrefix(), listener, listenerName), true, false, false, retryQueueArgs);
        amqpAdmin.declareQueue(retryRouting);

        Map<String, Object> delayQueueArgs = new HashMap<>();
        delayQueueArgs.put(Constants.AMQP_DLX, exchange.getName());
        delayQueueArgs.put(Constants.AMQP_DLK, routing);

        Queue delayQueue = new Queue(delayRouting, true, false, false, delayQueueArgs);
        amqpAdmin.declareQueue(delayQueue);
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
}
