package me.insidezhou.southernquiet.notification.driver;

import me.insidezhou.southernquiet.Constants;
import me.insidezhou.southernquiet.amqp.rabbit.AmqpAutoConfiguration;
import me.insidezhou.southernquiet.amqp.rabbit.AmqpMessageRecover;
import me.insidezhou.southernquiet.amqp.rabbit.DirectRabbitListenerContainerFactoryConfigurer;
import me.insidezhou.southernquiet.notification.AmqpNotificationAutoConfiguration;
import me.insidezhou.southernquiet.notification.NotificationListener;
import me.insidezhou.southernquiet.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
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
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.*;

public class AmqpNotificationListenerManager extends AbstractListenerManager implements Lifecycle {
    private final static Logger log = LoggerFactory.getLogger(AmqpNotificationListenerManager.class);

    private final SmartMessageConverter messageConverter;
    private final CachingConnectionFactory cachingConnectionFactory;

    private final List<Tuple<RabbitListenerEndpoint, NotificationListener, String>> listenerEndpoints = new ArrayList<>();
    private final RabbitAdmin rabbitAdmin;
    private final AmqpNotificationPublisher<?> publisher;
    private final AmqpAutoConfiguration.Properties amqpProperties;
    private final AmqpNotificationAutoConfiguration.Properties amqpNotificationProperties;

    private final RabbitProperties rabbitProperties;
    private final ApplicationContext applicationContext;

    private final RabbitTemplate rabbitTemplate;

    public AmqpNotificationListenerManager(RabbitAdmin rabbitAdmin,
                                           AmqpNotificationPublisher<?> publisher,
                                           AmqpNotificationAutoConfiguration.Properties amqpNotificationProperties,
                                           AmqpAutoConfiguration.Properties amqpProperties,
                                           RabbitProperties rabbitProperties,
                                           RabbitConnectionFactoryBean factoryBean,
                                           ObjectProvider<ConnectionNameStrategy> connectionNameStrategy,
                                           ApplicationContext applicationContext
    ) {
        this.rabbitAdmin = rabbitAdmin;
        this.publisher = publisher;
        this.amqpNotificationProperties = amqpNotificationProperties;
        this.amqpProperties = amqpProperties;
        this.rabbitProperties = rabbitProperties;
        this.applicationContext = applicationContext;

        this.messageConverter = publisher.getMessageConverter();

        this.cachingConnectionFactory = AmqpAutoConfiguration.rabbitConnectionFactory(rabbitProperties, factoryBean, connectionNameStrategy);
        this.rabbitTemplate = new RabbitTemplate(cachingConnectionFactory);
    }

    public void registerListeners(RabbitListenerEndpointRegistrar registrar) {
        initListener(applicationContext);

        listenerEndpoints.forEach(tuple -> {
            RabbitListenerEndpoint endpoint = tuple.getFirst();
            NotificationListener listenerAnnotation = tuple.getSecond();
            String listenerName = tuple.getThird();

            DirectRabbitListenerContainerFactoryConfigurer containerFactoryConfigurer = new DirectRabbitListenerContainerFactoryConfigurer(
                rabbitProperties,
                new AmqpMessageRecover(
                    rabbitTemplate,
                    rabbitAdmin,
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

        listenerEndpoints.stream()
            .filter(listenerEndpoint -> listener.notification() == listenerEndpoint.getSecond().notification() && listenerName.equals(listenerEndpoint.getThird()))
            .findAny()
            .ifPresent(listenerEndpoint -> log.warn(
                "监听器重复: queue={}, listener={}#{}, notification={}",
                listenerRouting,
                bean.getClass().getName(),
                listenerDefaultName,
                listener.notification().getSimpleName()
            ));

        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
        endpoint.setId(UUID.randomUUID().toString());
        endpoint.setQueueNames(listenerRouting);
        endpoint.setAdmin(rabbitAdmin);

        declareExchangeAndQueue(listener, listenerDefaultName);

        Class<?> notificationClass = listener.notification();
        ParameterizedTypeReference<?> typeReference = ParameterizedTypeReference.forType(notificationClass);

        endpoint.setMessageListener(message -> {
            Object notification = messageConverter.fromMessage(message, typeReference);

            if (log.isDebugEnabled()) {
                log.debug(
                    "监听器收到通知: queue={}, listener={}#{}({}), notification={}, message={}",
                    endpoint.getQueueNames(),
                    bean.getClass().getName(),
                    listenerDefaultName,
                    endpoint.getId(),
                    notification.getClass().getSimpleName(),
                    message
                );
            }

            Object[] parameters = Arrays.stream(method.getParameters())
                .map(parameter -> {
                    Class<?> parameterClass = parameter.getType();

                    if (parameterClass.isInstance(notification)) {
                        return notification;
                    }
                    else if (parameterClass.isInstance(listener)) {
                        return listener;
                    }

                    throw new UnsupportedOperationException("不支持在通知监听器中使用此类型的参数：parameter=" + parameter.getClass() + ", notification=" + notificationClass);
                })
                .toArray();

            try {
                method.invoke(bean, parameters);
            }
            catch (RuntimeException e) {
                log.error("通知处理器抛出异常", e);

                throw e;
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        listenerEndpoints.add(new Tuple<>(endpoint, listener, listenerName));
    }

    private String getDeadSource(NotificationListener listener, String listenerDefaultName) {
        return suffix("DEAD." + publisher.getNotificationSource(listener.notification()), listener, listenerDefaultName);
    }

    private String getDeadRouting(NotificationListener listener, String listenerDefaultName) {
        return publisher.getRouting(amqpNotificationProperties.getNamePrefix(), getDeadSource(listener, listenerDefaultName));
    }

    private String getListenerRouting(NotificationListener listener, String listenerDefaultName) {
        return suffix(publisher.getRouting(amqpNotificationProperties.getNamePrefix(), listener.notification()), listener, listenerDefaultName);
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
        String routing = getListenerRouting(listener, listenerDefaultName);

        Exchange exchange = new FanoutExchange(publisher.getExchange(amqpNotificationProperties.getNamePrefix(), listener.notification()));
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
}
