package me.insidezhou.southernquiet;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;

public final class Constants {
    public final static String AMQP_DLX = "x-dead-letter-exchange";
    public final static String AMQP_DLK = "x-dead-letter-routing-key";

    public final static String AMQP_DEFAULT = "";
    public final static String AMQP_MESSAGE_TTL = "x-message-ttl";
    public final static String AMQP_PRIORITY_QUEUE = "x-max-priority";

    public final static String AMQP_DELAYED_TYPE = "x-delayed-type";
    public final static String AMQP_DELAYED_EXCHANGE = "x-delayed-message";

    public final static int AutoConfigLevel_Lowest = AutoConfigureOrder.DEFAULT_ORDER - 100;
    public final static int AutoConfigLevel_Middle = AutoConfigureOrder.DEFAULT_ORDER - 200;
    public final static int AutoConfigLevel_Highest = AutoConfigureOrder.DEFAULT_ORDER - 300;
}
