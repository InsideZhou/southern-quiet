package me.insidezhou.southernquiet.throttle;

@SuppressWarnings("WeakerAccess")
public class DefaultCountBasedThrottle implements Throttle {

    DefaultCountBasedThrottle() {
    }

    private long counter = 0;

    @Override
    public synchronized boolean open(long threshold) {
        if (threshold <= 0) {
            return true;
        }

//      下面第一行是counter，后几行是threshold分别为1、2、3时，节流器在第几个counter打开
//        1 2 3 4 5 6 7 8 9 10
//        1   3   5   7   9       threshold=1
//        1     4     7     10    threshold=2
//        1       5       9       threshold=3

        if (counter <= 0) {
            counter = 1;
            return true;
        }

        counter++;

        //注意，这里要加 1 （看上面的示例可以更方便理解算法）
        threshold += 1;

        if (counter - threshold >= 1) {
            counter = 1;
            return true;
        }else {
            return false;
        }
    }

}
