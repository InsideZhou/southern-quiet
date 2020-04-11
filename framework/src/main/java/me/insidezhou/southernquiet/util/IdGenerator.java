package me.insidezhou.southernquiet.util;

public interface IdGenerator {
    long generate();

    long getTicksFromId(long id);

    long getTimestampFromId(long id);

    int getWorkerFromId(long id);

    int getSequenceFromId(long id);
}
