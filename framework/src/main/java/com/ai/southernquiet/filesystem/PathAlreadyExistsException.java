package com.ai.southernquiet.filesystem;

public class PathAlreadyExistsException extends FileSystemException {
    private final static long serialVersionUID = -3641435326452253077L;

    public PathAlreadyExistsException(String message) {
        super(message);
    }

    public PathAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
