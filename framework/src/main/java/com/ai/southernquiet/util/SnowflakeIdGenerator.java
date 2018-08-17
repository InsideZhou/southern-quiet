package com.ai.southernquiet.util;

import java.util.Random;

/**
 * 基于twiiter snowflake算法、64bit、秒级精度的发号器
 * <p>
 * 0 - timestamp - highPadding - worker - lowPadding - sequence
 */
public class SnowflakeIdGenerator implements IdGenerator {
    public final static long EPOCH = 1517414400L; //Thu Feb 01 2018 00:00:00 GMT, seconds

    public final static int TimestampBits = 32;
    public final static int HighPaddingBits = 0;
    public final static int WorkerIdBits = 12;
    public final static int LowPaddingBits = 0;

    public final static int SequenceStartRange = 0;

    public static int maxIntegerAtBits(int bits) {
        return ~(-1 << bits);
    }

    private Random random;
    private long epoch;
    private int sequenceStartRange;

    private int workerId;

    public SnowflakeIdGenerator(int workerId, int timestampBits, int highPaddingBits, int workerIdBits, int lowPaddingBits, long epoch, Random random, int sequenceStartRange) {
        int sequenceBits = 63 - timestampBits - highPaddingBits - workerIdBits - lowPaddingBits;

        maxWorkerId = maxIntegerAtBits(workerIdBits);
        maxSequenceValue = maxIntegerAtBits(sequenceBits);
        workerIdShift = sequenceBits + lowPaddingBits;
        timestampShift = sequenceBits + lowPaddingBits + workerIdBits + highPaddingBits;

        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException("worker Id can't be greater than maxWorkerId or less than 0, maxWorkerId=" + maxWorkerId);
        }

        this.random = random;
        this.epoch = epoch;
        this.sequenceStartRange = sequenceStartRange;

        this.workerId = workerId;
    }

    public SnowflakeIdGenerator(int workerId, int timestampBits, int highPaddingBits, int workerIdBits, int lowPaddingBits) {
        this(workerId,
            timestampBits,
            highPaddingBits,
            workerIdBits,
            lowPaddingBits,
            EPOCH,
            null,
            SequenceStartRange);
    }

    public SnowflakeIdGenerator(int workerId, Random random, int sequenceStartRange) {
        this(workerId,
            TimestampBits,
            HighPaddingBits,
            WorkerIdBits,
            LowPaddingBits,
            EPOCH,
            random,
            sequenceStartRange);
    }

    public SnowflakeIdGenerator(int workerId, long epoch) {
        this(workerId,
            TimestampBits,
            HighPaddingBits,
            WorkerIdBits,
            LowPaddingBits,
            epoch,
            null,
            SequenceStartRange);
    }

    public SnowflakeIdGenerator(int workerId) {
        this(workerId,
            TimestampBits,
            HighPaddingBits,
            WorkerIdBits,
            LowPaddingBits,
            EPOCH,
            null,
            SequenceStartRange);
    }


    private int maxWorkerId = -1;

    private int maxSequenceValue = -1;

    private int workerIdShift = -1;

    private int timestampShift = -1;

    private int sequence = 0;

    private long lastTimestamp = -1L;

    public synchronized long generate() {
        long timestamp = timeGen();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards.  Refusing to generate id for ${lastTimestamp - timestamp} seconds");
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & maxSequenceValue;

            if (0 == sequence) {
                timestamp = nextTick(lastTimestamp);
            }
        }
        else if (null != random) {
            sequence = random.nextInt(sequenceStartRange);
        }
        else {
            sequence = sequenceStartRange;
        }

        lastTimestamp = timestamp;

        return ((timestamp - epoch) << timestampShift) | (workerId << workerIdShift) | sequence;
    }

    private long nextTick(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis() / 1000;
    }

    public int getMaxWorkerId() {
        return maxWorkerId;
    }

    private void setMaxWorkerId(int maxWorkerId) {
        this.maxWorkerId = maxWorkerId;
    }

    public int getMaxSequenceValue() {
        return maxSequenceValue;
    }

    private void setMaxSequenceValue(int maxSequenceValue) {
        this.maxSequenceValue = maxSequenceValue;
    }

    public int getWorkerIdShift() {
        return workerIdShift;
    }

    private void setWorkerIdShift(int workerIdShift) {
        this.workerIdShift = workerIdShift;
    }

    public int getTimestampShift() {
        return timestampShift;
    }

    private void setTimestampShift(int timestampShift) {
        this.timestampShift = timestampShift;
    }

    public int getSequence() {
        return sequence;
    }

    private void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public long getLastTimestamp() {
        return lastTimestamp;
    }

    private void setLastTimestamp(long lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }
}
