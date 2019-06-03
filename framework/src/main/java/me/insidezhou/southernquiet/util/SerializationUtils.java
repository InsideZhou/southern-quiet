package me.insidezhou.southernquiet.util;

import org.nustaq.serialization.FSTConfiguration;

public abstract class SerializationUtils {
    private final static FSTConfiguration fstConf = FSTConfiguration.createDefaultConfiguration();

    /**
     * Serialize the given object to a byte array.
     *
     * @param object the object to serialize
     * @return an array of bytes representing the object in a portable fashion
     */
    public static byte[] serialize(Object object) {
        if (object == null) {
            return null;
        }

        return fstConf.asByteArray(object);
    }

    /**
     * Deserialize the byte array into an object.
     *
     * @param bytes a serialized object
     * @return the result of deserializing the bytes
     */
    public static Object deserialize(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        return fstConf.asObject(bytes);
    }
}
