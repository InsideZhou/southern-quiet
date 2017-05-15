package com.ai.southernquiet.logging;

import ch.qos.logback.core.OutputStreamAppender;
import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.filesystem.InvalidFileException;

import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link FileSystem}可以在运行时设置，如为null，则把日志输出到 {@link System#out}。
 */
public class FileAppender<E> extends OutputStreamAppender<E> {
    private FileSystem fileSystem;
    private String filePath;

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(FileSystem fileSystem) {
        OutputStream out;
        if (null == fileSystem) {
            out = System.out;
        }
        else {
            try {
                out = fileSystem.openWriteStream(getFilePath());
            }
            catch (InvalidFileException e) {
                throw new RuntimeException(e);
            }

            this.fileSystem = fileSystem;
        }

        if (isStarted()) {
            setOutputStream(out);
        }
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void start() {
        FileSystem fileSystem = getFileSystem();

        try {
            if (null == fileSystem) {
                setOutputStream(System.out);
            }
            else {
                setOutputStream(fileSystem.openWriteStream(getFilePath()));
            }
        }
        catch (InvalidFileException e) {
            throw new RuntimeException(e);
        }

        super.start();
    }

    @Override
    public void stop() {
        super.stop();

        OutputStream out = getOutputStream();
        if (null != out) {
            try {
                out.close();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
