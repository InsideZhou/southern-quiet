package me.insidezhou.southernquiet.filesystem;

/**
 * 无效文件
 */
public class InvalidFileException extends FileSystemException {
    private final static long serialVersionUID = -7740627011844462423L;


    public InvalidFileException(String message) {
        super(message);
    }

    public InvalidFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
