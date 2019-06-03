package me.insidezhou.southernquiet.filesystem;

public class FileSystemException extends Exception {
    private final static long serialVersionUID = -4353990872019691052L;

    public FileSystemException(String message) {
        super(message);
    }

    public FileSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
