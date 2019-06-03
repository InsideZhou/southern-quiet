package me.insidezhou.southernquiet.filesystem;

public class PathNotFoundException extends FileSystemException {
    private final static long serialVersionUID = -7066503468436236283L;

    public PathNotFoundException(String message) {
        super(message);
    }

    public PathNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
