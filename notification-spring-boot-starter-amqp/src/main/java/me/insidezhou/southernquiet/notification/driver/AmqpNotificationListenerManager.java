package me.insidezhou.southernquiet.notification.driver;

import me.insidezhou.southernquiet.amqp.rabbit.AmqpAutoConfiguration;
import me.insidezhou.southernquiet.amqp.rabbit.AmqpMessageRecover;
import me.insidezhou.southernquiet.amqp.rabbit.DirectRabbitListenerContainerFactoryConfigurer;
import me.insidezhou.southernquiet.notification.AmqpNotificationAutoConfiguration;
import me.insidezhou.southernquiet.notification.NotificationListener;
import me.insidezhou.southernquiet.util.Tuple;
import me.insidezhou.southernquiet.Constants;
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
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.*;

public class AmqpNotificationListenerManager extends AbstractListenerManager {
    private final static Logger log = LoggerFactory.getLogger(AmqpNotificationListenerManager.class);

    private MessageConverter messageConverter;
    private CachingConnectionFactory cachingConnectionFactory;

    private List<Tuple<RabbitListenerEndpoint, NotificationListener, String>> listenerEndpoints = new ArrayList<>();
    private RabbitAdmin rabbitAdmin;
    private AmqpNotificationPublisher publisher;
    private AmqpAutoConfiguration.Properties amqpProperties;
    private AmqpNotificationAutoConfiguration.Properties amqpNotificationProperties;

    private PlatformTransactionManager transactionManager;
    private RabbitProperties rabbitProperties;
    private ApplicationContext applicationContext;

    public AmqpNotificationListenerManager(RabbitAdmin rabbitAdmin,
                                           AmqpNotificationPublisher publisher,
                                           AmqpNotificationAutoConfiguration.Properties amqpNotificationProperties,
                                           AmqpAutoConfiguration.Properties amqpProperties,
                                           RabbitProperties rabbitProperties,
                                           PlatformTransactionManager transactionManager,
                                           RabbitConnectionFactoryBean factoryBean,
                                           ObjectProvider<ConnectionNameStrategy> connectionNameStrategy,
                                           ApplicationContext applicationContext
    ) {
        this.rabbitAdmin = rabbitAdmin;
        this.publisher = publisher;
        this.amqpNotificationProperties = amqpNotificationProperties;
        this.amqpProperties = amqpProperties;
        this.rabbitProperties = rabbitProperties;
        this.transactionManager = transactionManager;
        this.applicationContext = applicationContext;

        this.messageConverter = publisher.getMessageConverter();

        this.cachingConnectionFactory = AmqpAutoConfiguration.rabbitConnectionFactory(rabbitProperties, factoryBean, connectionNameStrategy);
        cachingConnectionFactory.setPublisherConfirms(false);
    }

    public void registerListeners(RabbitListenerEndpointRegistrar registrar) {
        initListener(applicationContext);

        RabbitTemplate rabbitTemplate = new RabbitTemplate(cachingConnectionFactory);

        listenerEndpoints.forEach(tuple -> {
            RabbitListenerEndpoint endpoint = tuple.getFirst();
            NotificationListener listenerAnnotation = tuple.getSecond();
            String listenerDefaultName = tuple.getThird();

            DirectRabbitListenerContainerFactoryConfigurer containerFactoryConfigurer = new DirectRabbitListenerContainerFactoryConfigurer(
                rabbitProperties,
                new AmqpMessageRecover(
                    rabbitTemplate,
                    rabbitAdmin,
                    Constants.AMQP_DEFAULT,
                    getDeadRouting(listenerAnnotation, listenerDefaultName),
                    amqpProperties
                ),
                amqpProperties
            );

            DirectRabbitListenerContainerFactory factory = new DirectRabbitListenerContainerFactory();
            if (listenerAnnotation.isTransactionEnabled()) {
                factory.setTransactionManager(transactionManager);
            }
            factory.setMessageConverter(publisher.getMessageConverter());
            containerFactoryConfigurer.configure(factory, cachingConnectionFactory);

            registrar.registerEndpoint(endpoint, factory);
        });
    }

    @Override
    protected void initListener(NotificationListener listener, Object bean, Method method) {
        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();

        String listenerDefaultName = method.getName();

        endpoint.setId(UUID.randomUUID().toString());
        endpoint.setQueueNames(getListenerRouting(listener, listenerDefaultName));
        endpoint.setAdmin(rabbitAdmin);

        declareExchangeAndQueue(listener, listenerDefaultName);

        endpoint.setMessageListener(message -> {
            Object notification = messageConverter.fromMessage(message);
            Object[] parameters = Arrays.stream(method.getParameters())
                .map(parameter -> {
                    Class<?> cls = parameter.getType();

                    if (cls.isInstance(notification)) {
                        return notification;
                    }
                    else if (cls.isInstance(listener)) {
                        return listener;
                    }

                    throw new UnsupportedOperationException("不支持在通知监听器中使用此类型的参数：" + parameter.toString());
                })
                .toArray();

            if (log.isDebugEnabled()) {
                log.debug(
                    "监听器收到通知: queue={}, listener={}#{}({}), notification={}, message={}",
                    endpoint.getQueueNames(),
                    bean.getClass().getSimpleName(),
                    listenerDefaultName,
                    endpoint.getId(),
                    notification.getClass().getSimpleName(),
                    message
                );
            }

            try {
                method.invoke(bean, parameters);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        listenerEndpoints.add(new Tuple<>(endpoint, listener, listenerDefaultName));
    }

    @SuppressWarnings("unchecked")
    private String getDeadSource(NotificationListener listener, String listenerDefaultName) {
        return suffix("DEAD." + publisher.getNotificationSource(listener.notification()), listener, listenerDefaultName);
    }

    private String getDeadRouting(NotificationListener listener, String listenerDefaultName) {
        return publisher.getRouting(amqpNotificationProperties.getNamePrefix(), getDeadSource(listener, listenerDefaultName));
    }

    @SuppressWarnings("unchecked")
    private String getListenerRouting(NotificationListener listener, String listenerDefaultName) {
        return suffix(publisher.getRouting(amqpNotificationProperties.getNamePrefix(), publisher.getNotificationSource(listener.notification())), listener, listenerDefaultName);
    }

    private String suffix(String routing, NotificationListener listener, String listenerDefaultName) {
        String listenerName = listener.name();
        if (StringUtils.isEmpty(listenerName)) {
            listenerName = listenerDefaultName;
        }

        Assert.hasText(listenerName, "监听器的名称不能为空");

        return routing + "#" + listenerName;
    }

    @SuppressWarnings("unchecked")
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
}
