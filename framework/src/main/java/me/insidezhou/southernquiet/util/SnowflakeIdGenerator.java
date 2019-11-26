package me.insidezhou.southernquiet.util;

import instep.util.LongIdGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * 基于twiiter snowflake算法、64bit、默认秒级精度的发号器
 * <p>
 * 0 - timestamp - highPadding - worker - lowPadding - sequence
 */
@SuppressWarnings("WeakerAccess")
public class SnowflakeIdGenerator extends LongIdGenerator implements IdGenerator {
    public final static long EPOCH = 1517414400L; //Thu Feb 01 2018 00:00:00 GMT, seconds

    public final static int TimestampBits = 32;
    public final static int HighPaddingBits = 0;
    public final static int WorkerIdBits = 12;
    public final static int LowPaddingBits = 0;

    public final static int SequenceStartRange = 0;

    public final static int TickAccuracy = 1000;

    public static int maxIntegerAtBits(int bits) {
        return ~(-1 << bits);
    }

    private int currentTimeAccuracy;

    public SnowflakeIdGenerator(int workerId, int timestampBits, int highPaddingBits, int workerIdBits, int lowPaddingBits, long epoch, int sequenceStartRange, @Nullable Random random, int tickAccuracy) {
        super(workerId, timestampBits, highPaddingBits, workerIdBits, lowPaddingBits, epoch, sequenceStartRange, random);

        currentTimeAccuracy = tickAccuracy;
    }

    @SuppressWarnings("unused")
    public SnowflakeIdGenerator(int workerId, int timestampBits, int highPaddingBits, int workerIdBits, int lowPaddingBits) {
        this(workerId,
            timestampBits,
            highPaddingBits,
            workerIdBits,
            lowPaddingBits,
            EPOCH,
            SequenceStartRange,
            null,
            TickAccuracy);
    }

    public SnowflakeIdGenerator(int workerId, Random random, int sequenceStartRange) {
        this(workerId,
            TimestampBits,
            HighPaddingBits,
            WorkerIdBits,
            LowPaddingBits,
            EPOCH,
            sequenceStartRange,
            random,
            TickAccuracy);
    }

    public SnowflakeIdGenerator(int workerId, long epoch) {
        this(workerId,
            TimestampBits,
            HighPaddingBits,
            WorkerIdBits,
            LowPaddingBits,
            epoch,
            SequenceStartRange,
            null,
            TickAccuracy);
    }

    public SnowflakeIdGenerator(int workerId) {
        this(workerId,
            TimestampBits,
            HighPaddingBits,
            WorkerIdBits,
            LowPaddingBits,
            EPOCH,
            SequenceStartRange,
            null,
            TickAccuracy);
    }

    @Override
    public long getTimestampFromId(long id) {
        return (id >>> getTimestampShift()) + EPOCH;
    }

    @Override
    public long getWorkerFromId(long id) {
        return (id << 1 + getTimestampBits() + getHighPaddingBits()) >>> (1 + getTimestampBits() + getHighPaddingBits() + getWorkerIdShift());
    }

    @Override
    public long getSequenceFromId(long id) {
        return (id << 64 - getSequenceBits()) >>> (64 - getSequenceBits());
    }

    @Override
    protected long timeGen() {
        return System.currentTimeMillis() / currentTimeAccuracy;
    }
}
