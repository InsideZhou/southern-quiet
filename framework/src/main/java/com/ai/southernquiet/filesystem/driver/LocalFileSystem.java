package com.ai.southernquiet.filesystem.driver;

import com.ai.southernquiet.Constant;
import com.ai.southernquiet.FrameworkProperties;
import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.filesystem.FileSystemException;
import com.ai.southernquiet.filesystem.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 基于操作系统本地文件系统的驱动.
 */
@Component
@Qualifier(Constant.DEFAULT_DRIVER_QUALIFIER)
public class LocalFileSystem implements FileSystem {
    private String workingRoot;

    public LocalFileSystem(FrameworkProperties frameworkProperties) throws IOException {
        String workingRoot = frameworkProperties.getFileSystem().getDefaultDriver().getWorkingRoot();
        if (!workingRoot.endsWith(FileSystem.PATH_SEPARATOR)) {
            workingRoot += FileSystem.PATH_SEPARATOR;
        }

        Path workingPath = Paths.get(workingRoot);
        if (Files.notExists(workingPath)) {
            Files.createDirectory(workingPath);
        }

        this.workingRoot = workingRoot;
    }

    @Override
    public void create(String path) {
        try {
            Files.createDirectories(getWorkingPath(path));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void create(String path, InputStream stream) throws PathAlreadyExistsException {
        Path workingPath = getWorkingPath(path);
        if (Files.exists(workingPath)) throw new PathAlreadyExistsException(path);

        try {
            Files.write(workingPath, StreamUtils.copyToByteArray(stream), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void create(String path, CharSequence txt) throws PathAlreadyExistsException {
        Path workingPath = getWorkingPath(path);
        if (Files.exists(workingPath)) throw new PathAlreadyExistsException(path);

        try {
            Files.write(workingPath, txt.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void put(String path, InputStream stream) throws InvalidFileException {
        Path workingPath = getWorkingPath(path);

        try {
            Files.write(workingPath, StreamUtils.copyToByteArray(stream));
        }
        catch (IOException e) {
            throw new InvalidFileException(path, e);
        }
    }

    @Override
    public void put(String path, CharSequence txt) throws InvalidFileException {
        Path workingPath = getWorkingPath(path);

        try {
            Files.write(workingPath, txt.toString().getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e) {
            throw new InvalidFileException(path, e);
        }
    }

    @Override
    public boolean exists(String path) {
        Path workingPath = getWorkingPath(path);
        return Files.exists(workingPath);
    }

    @Override
    public InputStream read(String path) throws InvalidFileException {
        Path workingPath = getWorkingPath(path);

        try {
            return new ByteArrayInputStream(Files.readAllBytes(workingPath));
        }
        catch (IOException e) {
            throw new InvalidFileException(path, e);
        }
    }

    @Override
    public String readString(String path) throws InvalidFileException {
        return readString(path, StandardCharsets.UTF_8);
    }

    @Override
    public String readString(String path, Charset charset) throws InvalidFileException {
        Path workingPath = getWorkingPath(path);

        try {
            return new String(Files.readAllBytes(workingPath), charset);
        }
        catch (IOException e) {
            throw new InvalidFileException(path, e);
        }
    }

    @Override
    public InputStream openReadStream(String path) throws InvalidFileException {
        Path workingPath = getWorkingPath(path);

        try {
            return Files.newInputStream(workingPath);
        }
        catch (IOException e) {
            throw new InvalidFileException(path, e);
        }
    }

    @Override
    public OutputStream openWriteStream(String path) throws InvalidFileException {
        Path workingPath = getWorkingPath(path);

        try {
            return Files.newOutputStream(workingPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        }
        catch (IOException e) {
            throw new InvalidFileException(path, e);
        }
    }

    @Override
    public void move(String source, String destination) throws FileSystemException {
        move(source, destination, false);
    }

    @Override
    public void move(String source, String destination, boolean replaceExisting) throws FileSystemException {
        moveOrCopy(true, source, destination, replaceExisting);
    }

    @Override
    public void copy(String source, String destination) throws FileSystemException {
        copy(source, destination, false);
    }

    @Override
    public void copy(String source, String destination, boolean replaceExisting) throws FileSystemException {
        moveOrCopy(false, source, destination, replaceExisting);
    }

    @Override
    public void delete(String path) {
        Path workingPath = getWorkingPath(path);

        if (Files.notExists(workingPath)) return;

        if (!Files.isDirectory(workingPath)) {
            try {
                Files.deleteIfExists(workingPath);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }

            return;
        }

        try {
            Files.walkFileTree(workingPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void touchCreation(String path) {
        Path workingPath = getWorkingPath(path);
        BasicFileAttributeView attributes = Files.getFileAttributeView(workingPath, BasicFileAttributeView.class);
        try {
            attributes.setTimes(null, null, FileTime.from(Instant.now()));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void touchLastModified(String path) {
        Path workingPath = getWorkingPath(path);
        try {
            Files.setLastModifiedTime(workingPath, FileTime.from(Instant.now()));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void touchLastAccess(String path) {
        Path workingPath = getWorkingPath(path);
        BasicFileAttributeView attributes = Files.getFileAttributeView(workingPath, BasicFileAttributeView.class);
        try {
            attributes.setTimes(null, FileTime.from(Instant.now()), null);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PathMeta meta(String path) {
        Path workingPath = getWorkingPath(path);
        if (Files.notExists(workingPath)) return null;

        File file = workingPath.toFile();

        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(workingPath, BasicFileAttributes.class);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        PathMeta meta = new PathMeta();

        meta.setPath(path);
        meta.setName(file.getName());
        meta.setDirectory(file.isDirectory());
        meta.setCreationTime(attributes.creationTime().toInstant());
        meta.setLastAccessTime(attributes.lastAccessTime().toInstant());
        meta.setLastModifiedTime(attributes.lastModifiedTime().toInstant());

        if (file.isFile()) {
            meta.setSize(file.length());
        }

        return meta;
    }

    @Override
    public List<PathMeta> paths(String path) throws PathNotFoundException {
        return paths(path, null, false);
    }

    @Override
    public List<PathMeta> paths(String path, String search) throws PathNotFoundException {
        return paths(path, search, false);
    }

    @Override
    public List<PathMeta> paths(String path, String search, boolean recursive) throws PathNotFoundException {
        Stream<Path> stream = pathStream(path, search, recursive, false);
        return stream.map(p -> meta(p.toString())).collect(Collectors.toList());
    }

    @Override
    public List<PathMeta> files(String path) throws PathNotFoundException {
        return files(path, null, false);
    }

    @Override
    public List<PathMeta> files(String path, String search) throws PathNotFoundException {
        return files(path, search, false);
    }

    @Override
    public List<PathMeta> files(String path, String search, boolean recursive) throws PathNotFoundException {
        Stream<Path> stream = pathStream(path, search, recursive, true);
        return stream.map(p -> meta(p.toString())).collect(Collectors.toList());
    }

    private Path getWorkingPath(String path) {
        return Paths.get(workingRoot + path);
    }

    private void moveOrCopy(boolean move, String source, String destination, boolean replaceExisting) throws FileSystemException {
        Path src = getWorkingPath(source);
        Path dest = getWorkingPath(destination);

        if (Files.notExists(src)) throw new PathNotFoundException(source);

        if (Files.notExists(dest)) {
            try {
                moveOrCopy(move, src, dest);
            }
            catch (IOException e) {
                throw new FileSystemException(source + " " + destination, e);
            }

            return;
        }

        Stream<Path> stream;
        if (Files.isDirectory(src)) {
            if (!Files.isDirectory(dest)) throw new FileSystemException("不能把目录移动或复制到文件。");

            try {
                stream = Files.walk(src).filter(p -> !Files.isDirectory(p));
            }
            catch (IOException e) {
                throw new FileSystemException(source + " " + destination, e);
            }
        }
        else {
            stream = Stream.of(src);
        }

        if (replaceExisting) {
            stream.forEach(path -> {
                Path target = dest.resolve(path.relativize(src));
                try {
                    moveOrCopy(move, src, target, StandardCopyOption.REPLACE_EXISTING);
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        else {
            stream.forEach(path -> {
                Path target = dest.resolve(path.relativize(src));
                if (Files.exists(target)) return;

                try {
                    moveOrCopy(move, src, target);
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void moveOrCopy(boolean move, Path src, Path dest, CopyOption... options) throws IOException {
        Set<CopyOption> opts = new HashSet<>(Arrays.asList(options));
        opts.add(StandardCopyOption.COPY_ATTRIBUTES);

        if (move) {
            Files.move(src, dest, opts.toArray(new CopyOption[opts.size()]));
        }
        else {
            Files.copy(src, dest, opts.toArray(new CopyOption[opts.size()]));
        }
    }

    private Stream<Path> pathStream(String path, String search, boolean recursive, boolean fileOnly) throws PathNotFoundException {
        Path workingPath = getWorkingPath(path);
        if (Files.notExists(workingPath)) throw new PathNotFoundException(path);

        Stream<Path> stream;
        try {
            if (recursive) {
                stream = Files.walk(workingPath);
            }
            else {
                stream = Files.list(workingPath);
            }

            if (fileOnly) {
                stream = stream.filter(Files::isDirectory);
            }

            if (StringUtils.hasText(search)) {
                stream = stream.filter(p -> p.getFileName().toString().contains(search));
            }

            return stream;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}