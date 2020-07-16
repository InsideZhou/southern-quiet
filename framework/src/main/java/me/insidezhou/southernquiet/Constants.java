package me.insidezhou.southernquiet;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;

public final class Constants {
    public final static String AMQP_DLX = "x-dead-letter-exchange";
    public final static String AMQP_DLK = "x-dead-letter-routing-key";
    public final static String AMQP_DEFAULT = "";

    public final static int AutoConfigLevel_Lowest = AutoConfigureOrder.DEFAULT_ORDER;
    public final static int AutoConfigLevel_Middle = AutoConfigureOrder.DEFAULT_ORDER - 1;
    public final static int AutoConfigLevel_Highest = AutoConfigureOrder.DEFAULT_ORDER - 2;
}
