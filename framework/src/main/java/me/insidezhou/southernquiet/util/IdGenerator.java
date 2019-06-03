package me.insidezhou.southernquiet.util;

public interface IdGenerator {
    long generate();

    long getTimestampFromId(long id);

    long getWorkerFromId(long id);

    long getSequenceFromId(long id);
}
