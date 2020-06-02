package me.insidezhou.southernquiet.notification.driver;

import me.insidezhou.southernquiet.Constants;
import me.insidezhou.southernquiet.amqp.rabbit.AmqpAutoConfiguration;
import me.insidezhou.southernquiet.amqp.rabbit.AmqpMessageRecover;
import me.insidezhou.southernquiet.amqp.rabbit.DelayedMessage;
import me.insidezhou.southernquiet.amqp.rabbit.DirectRabbitListenerContainerFactoryConfigurer;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import me.insidezhou.southernquiet.notification.AmqpNotificationAutoConfiguration;
import me.insidezhou.southernquiet.notification.NotificationListener;
import me.insidezhou.southernquiet.util.Amplifier;
import me.insidezhou.southernquiet.util.Tuple;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
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
    private final RabbitAdmin rabbitAdmin;
    private final RabbitTemplate rabbitTemplate;

    public AmqpNotificationListenerManager(RabbitAdmin rabbitAdmin,
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
        this.rabbitAdmin = rabbitAdmin;
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
                    getDeadRouting(listenerAnnotation, listenerName),
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
        String listenerDefaultName = method.getName();
        String listenerName = getListenerName(listener, listenerDefaultName);
        String listenerRouting = getListenerRouting(listener, listenerDefaultName);

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
        endpoint.setAdmin(rabbitAdmin);

        declareExchangeAndQueue(listener, listenerDefaultName);

        Class<?> notificationClass = listener.notification();
        ParameterizedTypeReference<?> typeReference = ParameterizedTypeReference.forType(notificationClass);

        endpoint.setMessageListener(message -> {
            Object notification = messageConverter.fromMessage(message, typeReference);

            log.message("监听器收到通知")
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

    private String getDeadSource(NotificationListener listener, String listenerDefaultName) {
        return suffix("DEAD." + AmqpNotificationPublisher.getNotificationSource(listener.notification()), listener, listenerDefaultName);
    }

    private String getDeadRouting(NotificationListener listener, String listenerDefaultName) {
        return AmqpNotificationPublisher.getRouting(amqpNotificationProperties.getNamePrefix(), getDeadSource(listener, listenerDefaultName));
    }

    private String getListenerRouting(NotificationListener listener, String listenerDefaultName) {
        return suffix(AmqpNotificationPublisher.getRouting(amqpNotificationProperties.getNamePrefix(), listener.notification()), listener, listenerDefaultName);
    }

    private String getListenerName(NotificationListener listener, String listenerDefaultName) {
        String listenerName = listener.name();
        if (StringUtils.isEmpty(listenerName)) {
            listenerName = listenerDefaultName;
        }

        Assert.hasText(listenerName, "监听器的名称不能为空");
        return listenerName;
    }

    private String suffix(String routing, NotificationListener listener, String listenerDefaultName) {
        return routing + "#" + getListenerName(listener, listenerDefaultName);
    }

    private void declareExchangeAndQueue(NotificationListener listener, String listenerDefaultName) {
        String routing = AmqpNotificationPublisher.getRouting(amqpNotificationProperties.getNamePrefix(), listener.notification());
        String delayRouting = AmqpNotificationPublisher.getDelayedRouting(amqpNotificationProperties.getNamePrefix(), listener.notification());
        String listenerRouting = getListenerRouting(listener, listenerDefaultName);

        Exchange exchange = new FanoutExchange(AmqpNotificationPublisher.getExchange(amqpNotificationProperties.getNamePrefix(), listener.notification()));
        Queue queue = new Queue(listenerRouting);

        rabbitAdmin.declareExchange(exchange);
        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(listenerRouting).noargs());

        Map<String, Object> deadQueueArgs = new HashMap<>();
        deadQueueArgs.put(Constants.AMQP_DLX, Constants.AMQP_DEFAULT);
        deadQueueArgs.put(Constants.AMQP_DLK, queue.getName());

        Queue deadRouting = new Queue(getDeadRouting(listener, listenerDefaultName), true, false, false, deadQueueArgs);
        rabbitAdmin.declareQueue(deadRouting);

        Map<String, Object> delayQueueArgs = new HashMap<>();
        delayQueueArgs.put(Constants.AMQP_DLX, exchange.getName());
        delayQueueArgs.put(Constants.AMQP_DLK, routing);

        Queue delayQueue = new Queue(delayRouting, true, false, false, delayQueueArgs);
        rabbitAdmin.declareQueue(delayQueue);
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
