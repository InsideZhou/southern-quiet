package me.insidezhou.southernquiet.logging;

import ch.qos.logback.core.OutputStreamAppender;
import me.insidezhou.southernquiet.filesystem.FileSystem;
import me.insidezhou.southernquiet.filesystem.InvalidFileException;

/**
 * {@link FileSystem}可以在运行时设置，如为null，则不输出日志。
 */
public class FileAppender<E> extends OutputStreamAppender<E> {
    private FileSystem fileSystem;
    private String file;

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;

        if (!isStarted()) {
            start();
        }
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public void start() {
        FileSystem fileSystem = getFileSystem();

        try {
            if (null != fileSystem) {
                setOutputStream(fileSystem.openWriteStream(getFile()));

                super.start();
            }
        }
        catch (InvalidFileException e) {
            throw new RuntimeException(e);
        }
    }
}
