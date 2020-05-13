package test.throttle.annotation;

import me.insidezhou.southernquiet.throttle.annotation.Throttle;

public class ThrottleAnnotationTestProcessor {
    private int countReturnObj;

    private synchronized int countReturnObjAdd(int i) {
        return countReturnObj += i;
    }

    private int countVoid = 0;

    private synchronized void countVoidAdd(int i) {
        countVoid += i;
    }

    @Throttle(threshold = 1)
    public Integer countBaseReturnObj(int i) {
        return countReturnObjAdd(i);
    }

    @Throttle(threshold = 1)
    public void countBaseVoid(int i) {
        countVoidAdd(i);
    }

    @Throttle(threshold = 1)
    public Integer timeBaseReturnObj(int i) {
        return countReturnObjAdd(i);
    }

    @Throttle(threshold = 1)
    public void timeBaseVoid(int i) {
        countVoidAdd(i);
    }

    public int getCountReturnObj() {
        return countReturnObj;
    }

    public void setCountReturnObj(int countReturnObj) {
        this.countReturnObj = countReturnObj;
    }

    public int getCountVoid() {
        return countVoid;
    }

    public void setCountVoid(int countVoid) {
        this.countVoid = countVoid;
    }
}
