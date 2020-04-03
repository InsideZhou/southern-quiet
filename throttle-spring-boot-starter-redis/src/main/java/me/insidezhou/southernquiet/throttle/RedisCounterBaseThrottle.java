package me.insidezhou.southernquiet.throttle;

import io.lettuce.core.RedisCommandExecutionException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 使用redis实现的计数器节流器
 */
public class RedisCounterBaseThrottle implements Throttle {

    private StringRedisTemplate stringRedisTemplate;

    public RedisCounterBaseThrottle(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String KEY_PRE = "Throttle_Counter_";

    /**
     * 以次数为依据打开节流器，上次打开之后必须至少节流了指定次数才能再次打开，如果打开失败返回false。
     */
    @Override
    public boolean open(String key, long threshold) {
        if (threshold <= 0) {
            return true;
        }

        key = KEY_PRE + key;

        //注：第一次调用redis的increment调用时，默认返回1。
        Long counterL;
        try {
            counterL = stringRedisTemplate.opsForValue().increment(key);
        } catch (RedisCommandExecutionException e) {
            //超出最大值Long.MAX_VALUE了，重置（一般不会发生，但还是要写增强鲁棒性）
            stringRedisTemplate.opsForValue().set(key, "1");
            counterL = 1L;
        }

        //设置key一天后过期删除（如果1天内没有用到这个key，就释放吧，不留在redis里）
        stringRedisTemplate.expire(key, 1, TimeUnit.DAYS);

//      下面第一行是counter，后几行是threshold分别为1、2、3时，节流器在第几个counter打开
//        1 2 3 4 5 6 7 8 9 10
//        1   3   5   7   9       threshold=1
//        1     4     7     10    threshold=2
//        1       5       9       threshold=3

        long counter = counterL == null ? 0 : counterL;

        //注意，这里要加 1 （看上面的示例可以更方便理解算法）
        threshold += 1;

        return counter % threshold == 1;
    }

}
