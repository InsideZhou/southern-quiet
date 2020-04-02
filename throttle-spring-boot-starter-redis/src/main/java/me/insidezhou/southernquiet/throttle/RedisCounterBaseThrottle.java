package me.insidezhou.southernquiet.throttle;

public class RedisCounterBaseThrottle implements Throttle {

    @Override
    public boolean open(String key, long threshold) {
        //FIXME LIANGYI 实现计数器节流阀
        return false;
    }

}
