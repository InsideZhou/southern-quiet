package com.ai.southernquiet.notification.driver;

import com.ai.southernquiet.amqp.rabbit.AmqpAutoConfiguration;
import com.ai.southernquiet.amqp.rabbit.AmqpMessageRecover;
import com.ai.southernquiet.amqp.rabbit.DirectRabbitListenerContainerFactoryConfigurer;
import com.ai.southernquiet.notification.NotificationListener;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.ai.southernquiet.Constants.AMQP_DLK;
import static com.ai.southernquiet.Constants.AMQP_DLX;

public class AmqpNotificationListenerManager extends AbstractListenerManager {
    private MessageConverter messageConverter;
    private ConnectionFactory connectionFactory;

    private Map<RabbitListenerEndpoint, NotificationListener> listenerEndpoints = new HashMap<>();
    private RabbitAdmin rabbitAdmin;
    private AmqpNotificationPublisher publisher;
    private AmqpAutoConfiguration.Properties amqpProperties;

    private RabbitProperties rabbitProperties;
    private ApplicationContext applicationContext;

    public AmqpNotificationListenerManager(ConnectionFactory connectionFactory,
                                           RabbitAdmin rabbitAdmin,
                                           AmqpNotificationPublisher publisher,
                                           AmqpAutoConfiguration.Properties amqpProperties,
                                           RabbitProperties rabbitProperties,
                                           ApplicationContext applicationContext
    ) {
        this.connectionFactory = connectionFactory;
        this.rabbitAdmin = rabbitAdmin;
        this.publisher = publisher;
        this.amqpProperties = amqpProperties;
        this.rabbitProperties = rabbitProperties;
        this.applicationContext = applicationContext;

        this.messageConverter = publisher.getMessageConverter();
    }

    public void registerListeners(RabbitListenerEndpointRegistrar registrar) {
        initListener(applicationContext);

        listenerEndpoints.forEach((endpoint, notificationCls) -> {
            DirectRabbitListenerContainerFactoryConfigurer containerFactoryConfigurer = new DirectRabbitListenerContainerFactoryConfigurer();
            containerFactoryConfigurer.setRabbitProperties(rabbitProperties);
            containerFactoryConfigurer.setMessageRecoverer(new AmqpMessageRecover(
                publisher.getRabbitTemplate(),
                getDeadExchange(notificationCls),
                getDeadRouting(notificationCls),
                amqpProperties
            ));

            DirectRabbitListenerContainerFactory factory = new DirectRabbitListenerContainerFactory();
            factory.setMessageConverter(publisher.getMessageConverter());
            containerFactoryConfigurer.configure(factory, connectionFactory);

            registrar.registerEndpoint(endpoint, factory);
        });
    }

    @Override
    protected void initListener(NotificationListener listener, Object bean, Method method) {
        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();

        endpoint.setId(UUID.randomUUID().toString());
        endpoint.setQueueNames(getListenerRouting(listener));
        endpoint.setAdmin(rabbitAdmin);

        declareExchangeAndQueue(listener);

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

            try {
                method.invoke(bean, parameters);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        listenerEndpoints.put(endpoint, listener);
    }

    @SuppressWarnings("unchecked")
    private String getDeadSource(NotificationListener listener) {
        return suffix("DEAD." + publisher.getNotificationSource(listener.notification()), listener);
    }

    private String getDeadExchange(NotificationListener listener) {
        return publisher.getExchange(getDeadSource(listener));
    }

    private String getDeadRouting(NotificationListener listener) {
        return publisher.getRouting(getDeadSource(listener));
    }

    @SuppressWarnings("unchecked")
    private String getListenerRouting(NotificationListener listener) {
        return suffix(publisher.getRouting(publisher.getNotificationSource(listener.notification())), listener);
    }

    private String suffix(String routing, NotificationListener listener) {
        return routing + "#" + listener.name();
    }

    @SuppressWarnings("unchecked")
    private void declareExchangeAndQueue(NotificationListener listener) {
        Assert.hasText(listener.name(), "监听器的名称不能为空");

        String routing = getListenerRouting(listener);

        Exchange exchange = publisher.declareExchange(listener.notification());
        Queue queue = new Queue(routing);

        rabbitAdmin.declareExchange(exchange);
        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(routing).noargs());


        Map<String, Object> deadQueueArgs = new HashMap<>();
        deadQueueArgs.put(AMQP_DLX, exchange.getName());
        deadQueueArgs.put(AMQP_DLK, queue.getName());

        Exchange deadExchange = new DirectExchange(getDeadExchange(listener));
        Queue deadRouting = new Queue(getDeadRouting(listener), true, false, false, deadQueueArgs);

        rabbitAdmin.declareExchange(deadExchange);
        rabbitAdmin.declareQueue(deadRouting);
        rabbitAdmin.declareBinding(BindingBuilder.bind(deadRouting).to(deadExchange).with(deadRouting.getName()).noargs());
    }
}
