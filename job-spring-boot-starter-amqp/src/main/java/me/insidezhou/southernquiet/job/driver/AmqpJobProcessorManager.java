package me.insidezhou.southernquiet.job.driver;

import me.insidezhou.southernquiet.Constants;
import me.insidezhou.southernquiet.amqp.rabbit.AmqpAutoConfiguration;
import me.insidezhou.southernquiet.job.AmqpJobAutoConfiguration;
import me.insidezhou.southernquiet.job.JobProcessor;
import me.insidezhou.southernquiet.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpoint;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmqpJobProcessorManager extends AbstractJobProcessorManager implements Lifecycle {
    private final static Logger log = LoggerFactory.getLogger(AmqpJobProcessorManager.class);

    private final SmartMessageConverter messageConverter;
    private final CachingConnectionFactory cachingConnectionFactory;

    private final List<Tuple<RabbitListenerEndpoint, JobProcessor, String>> listenerEndpoints = new ArrayList<>();
    private final AmqpAdmin amqpAdmin;
    private final AmqpAutoConfiguration.Properties amqpProperties;
    private final AmqpJobAutoConfiguration.Properties amqpJobProperties;

    private final RabbitProperties rabbitProperties;

    private final RabbitTemplate rabbitTemplate;

    public AmqpJobProcessorManager(AmqpAdmin rabbitAdmin,
                                   AmqpJobArranger<?> jobArranger,
                                   AmqpJobAutoConfiguration.Properties amqpJobProperties,
                                   AmqpAutoConfiguration.Properties amqpProperties,
                                   RabbitProperties rabbitProperties,
                                   RabbitConnectionFactoryBean factoryBean,
                                   ObjectProvider<ConnectionNameStrategy> connectionNameStrategy,
                                   ApplicationContext applicationContext
    ) {
        super(applicationContext);

        this.amqpAdmin = rabbitAdmin;
        this.amqpJobProperties = amqpJobProperties;
        this.amqpProperties = amqpProperties;
        this.rabbitProperties = rabbitProperties;

        this.messageConverter = jobArranger.getMessageConverter();

        this.cachingConnectionFactory = AmqpAutoConfiguration.rabbitConnectionFactory(rabbitProperties, factoryBean, connectionNameStrategy);
        this.rabbitTemplate = new RabbitTemplate(cachingConnectionFactory);
    }

    @Override
    protected void initProcessor(JobProcessor listener, Object bean, Method method) {
        throw new UnsupportedOperationException();
    }

    private String getDeadSource(JobProcessor listener, String listenerDefaultName) {
        return suffix("DEAD." + AmqpJobArranger.getJobSource(listener.job()), listener, listenerDefaultName);
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

        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(routing).noargs());

        Map<String, Object> deadQueueArgs = new HashMap<>();
        deadQueueArgs.put(Constants.AMQP_DLX, Constants.AMQP_DEFAULT);
        deadQueueArgs.put(Constants.AMQP_DLK, queue.getName());

        Queue deadRouting = new Queue(getDeadRouting(listener, listenerDefaultName), true, false, false, deadQueueArgs);
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
