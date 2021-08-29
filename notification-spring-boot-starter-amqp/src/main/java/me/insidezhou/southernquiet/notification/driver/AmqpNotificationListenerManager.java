package me.insidezhou.southernquiet.notification.driver;

import me.insidezhou.southernquiet.Constants;
import me.insidezhou.southernquiet.amqp.rabbit.*;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import me.insidezhou.southernquiet.notification.AmqpNotificationAutoConfiguration;
import me.insidezhou.southernquiet.notification.NotificationListener;
import me.insidezhou.southernquiet.util.Amplifier;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
import java.util.stream.Collectors;

public class AmqpNotificationListenerManager extends AbstractNotificationListenerManager implements Lifecycle, RabbitListenerConfigurer {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(AmqpNotificationListenerManager.class);

    private final SmartMessageConverter messageConverter;
    private final CachingConnectionFactory cachingConnectionFactory;

    private final List<ListenerEndpoint> listenerEndpoints = new ArrayList<>();
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
        listenerEndpoints.stream().collect(Collectors.groupingBy(ListenerEndpoint::getRouting)).forEach((routing, group) -> {
            ListenerEndpoint endpoint = group.get(0);
            String listenerName = endpoint.getListenerName();

            NotificationListener listenerAnnotation = endpoint.getListenerAnnotation();
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
                    AbstractAmqpNotificationPublisher.getDelayRouting(amqpNotificationProperties.getNamePrefix(), listenerAnnotation.notification()),
                    getRetryRouting(amqpNotificationProperties.getNamePrefix(), listenerAnnotation, listenerName),
                    amqpProperties
                ),
                amqpProperties
            );

            DirectRabbitListenerContainerFactory factory = new DirectRabbitListenerContainerFactory();
            factory.setMessageConverter(messageConverter);
            factory.setAcknowledgeMode(amqpProperties.getAcknowledgeMode());
            factory.setConsumersPerQueue(listenerAnnotation.concurrency());
            containerFactoryConfigurer.configure(factory, cachingConnectionFactory);

            SimpleRabbitListenerEndpoint rabbitListenerEndpoint = new SimpleRabbitListenerEndpoint();
            rabbitListenerEndpoint.setId(UUID.randomUUID().toString());
            rabbitListenerEndpoint.setQueueNames(endpoint.getRouting());
            rabbitListenerEndpoint.setAdmin(amqpAdmin);
            rabbitListenerEndpoint.setMessageListener(endpoint.getMessageListener());

            registrar.registerEndpoint(rabbitListenerEndpoint, factory);
        });
    }

    @Override
    protected void initListener(NotificationListener listener, Object bean, Method method) {
        String listenerName = getListenerName(listener, method);
        String listenerRouting = getListenerRouting(listener, listenerName);

        listenerEndpoints.stream()
            .filter(listenerEndpoint -> listener.notification() == listenerEndpoint.getListenerAnnotation().notification() && listenerName.equals(listenerEndpoint.getListenerName()))
            .findAny()
            .ifPresent(listenerEndpoint -> log.message("监听器重复")
                .context(context -> {
                    context.put("queue", listenerRouting);
                    context.put("listener", bean.getClass().getName());
                    context.put("listenerName", listenerName);
                    context.put("notification", listener.notification().getSimpleName());
                })
                .warn());

        declareExchangeAndQueue(listener, listenerName);

        DelayedMessage delayedAnnotation = AnnotatedElementUtils.findMergedAnnotation(listener.notification(), DelayedMessage.class);

        ListenerEndpoint listenerEndpoint = new ListenerEndpoint();
        listenerEndpoint.setListenerName(listenerName);
        listenerEndpoint.setListenerAnnotation(listener);
        listenerEndpoint.setRouting(listenerRouting);
        listenerEndpoint.setMessageListener(generateMessageListener(
            ParameterizedTypeReference.forType(listener.notification()),
            listenerRouting,
            listener,
            bean,
            method,
            listenerName,
            delayedAnnotation
        ));

        listenerEndpoints.add(listenerEndpoint);
    }

    protected MessageListener generateMessageListener(ParameterizedTypeReference<?> typeReference,
                                                      String routing,
                                                      NotificationListener listener,
                                                      Object bean,
                                                      Method method,
                                                      String listenerName,
                                                      DelayedMessage delayedAnnotation
    ) {
        return message -> {
            Object notification = messageConverter.fromMessage(message, typeReference);

            onMessageReceived(routing, bean, listenerName, notification, message);

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
                            .context("notification", listener.notification())
                            .warn();

                        try {
                            return parameterClass.getDeclaredConstructor().newInstance();
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
        };
    }

    protected void onMessageReceived(
        String routing,
        Object bean,
        String listenerName,
        Object notification,
        Message message
    ) {
        log.message("收到通知")
            .context(context -> {
                context.put("queue", routing);
                context.put("listener", bean.getClass().getName());
                context.put("listenerName", listenerName);
                context.put("notification", notification.getClass().getSimpleName());
                context.put("message", message);
            })
            .debug();
    }

    public static String getDeadRouting(String prefix, NotificationListener listener, String listenerName) {
        return AbstractAmqpNotificationPublisher.getRouting(
            prefix,
            suffix("DEAD." + AbstractAmqpNotificationPublisher.getNotificationSource(listener.notification()), listenerName));
    }

    public static String getRetryRouting(String prefix, NotificationListener listener, String listenerName) {
        return AbstractAmqpNotificationPublisher.getRouting(
            prefix,
            suffix("RETRY." + AbstractAmqpNotificationPublisher.getNotificationSource(listener.notification()), listenerName));
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
        String delayRouting = AbstractAmqpNotificationPublisher.getDelayRouting(amqpNotificationProperties.getNamePrefix(), listener.notification());
        String listenerRouting = getListenerRouting(listener, listenerName);

        Exchange exchange = new FanoutExchange(routing, true, false);
        Queue queue = new Queue(listenerRouting);

        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(listenerRouting).noargs());

        Map<String, Object> deadQueueArgs = new HashMap<>();
        deadQueueArgs.put(Constants.AMQP_DLX, Constants.AMQP_DEFAULT);
        deadQueueArgs.put(Constants.AMQP_DLK, queue.getName());

        Queue deadRouting = new Queue(getDeadRouting(amqpNotificationProperties.getNamePrefix(), listener, listenerName), true, false, false, deadQueueArgs);
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
            getRetryRouting(amqpNotificationProperties.getNamePrefix(), listener, listenerName),
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
            AbstractAmqpNotificationPublisher.getDelayRouting(amqpNotificationProperties.getNamePrefix(), listener.notification()),
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

    static class ListenerEndpoint {
        private NotificationListener listenerAnnotation;
        private String listenerName;
        private String routing;
        private MessageListener messageListener;

        public NotificationListener getListenerAnnotation() {
            return listenerAnnotation;
        }

        public void setListenerAnnotation(NotificationListener listenerAnnotation) {
            this.listenerAnnotation = listenerAnnotation;
        }

        public String getListenerName() {
            return listenerName;
        }

        public void setListenerName(String listenerName) {
            this.listenerName = listenerName;
        }

        public String getRouting() {
            return routing;
        }

        public void setRouting(String routing) {
            this.routing = routing;
        }

        public MessageListener getMessageListener() {
            return messageListener;
        }

        public void setMessageListener(MessageListener messageListener) {
            this.messageListener = messageListener;
        }
    }
}
