package me.insidezhou.southernquiet.util;

public class GoldenRatioAmplifier implements Amplifier {
    private final long initialValue;

    public GoldenRatioAmplifier(long initialValue) {
        this.initialValue = initialValue;
    }

    @Override
    public long amplify(long index) {
        if (0 == index) return initialValue;

        double ratio = Math.pow(0.618, index);
        return (long) (initialValue / ratio);
    }
}
