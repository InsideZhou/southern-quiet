package me.insidezhou.southernquiet.filesystem;

import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * 文件系统。
 */
@SuppressWarnings({"unused"})
public interface FileSystem {
    char PATH_SEPARATOR = '/';
    String PATH_SEPARATOR_STRING = String.valueOf(PATH_SEPARATOR);

    static <T extends PathMeta> Stream<T> sort(Stream<T> stream, PathMetaSort sort) {
        switch (sort) {
            case Name:
                return stream.sorted(Comparator.comparing(PathMeta::getName));
            case NameDesc:
                return stream.sorted(Comparator.comparing(PathMeta::getName).reversed());

            case IsDirectory:
                return stream.sorted(Comparator.comparing(PathMeta::isDirectory));
            case IsDirectoryDesc:
                return stream.sorted(Comparator.comparing(PathMeta::isDirectory).reversed());

            case CreationTime:
                return stream.sorted(Comparator.comparing(PathMeta::getCreationTime));
            case CreationTimeDesc:
                return stream.sorted(Comparator.comparing(PathMeta::getCreationTime).reversed());

            case LastAccessTime:
                return stream.sorted(Comparator.comparing(PathMeta::getLastAccessTime));
            case LastAccessTimeDesc:
                return stream.sorted(Comparator.comparing(PathMeta::getLastAccessTime).reversed());

            case LastModifiedTime:
                return stream.sorted(Comparator.comparing(PathMeta::getLastModifiedTime));
            case LastModifiedTimeDesc:
                return stream.sorted(Comparator.comparing(PathMeta::getLastModifiedTime).reversed());

            case Size:
                return stream.sorted(Comparator.comparing(PathMeta::getSize));
            case SizeDesc:
                return stream.sorted(Comparator.comparing(PathMeta::getSize).reversed());
            default:
                throw new RuntimeException();
        }
    }

    /**
     * 创建目录。目录已存在则忽略。
     *
     * @param path 路径
     */
    void createDirectory(String path);

    /**
     * 如果文件未存在，则创建；否则替换。
     *
     * @param path   要写入的路径
     * @param stream 输入流
     * @throws InvalidFileException 无效文件
     */
    void put(String path, InputStream stream) throws InvalidFileException;

    /**
     * 如果文件未存在，则创建；否则替换。
     *
     * @param path 要写入的路径
     * @param txt  输入文本
     * @throws InvalidFileException 无效文件
     */
    default void put(String path, CharSequence txt) throws InvalidFileException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(txt.toString().getBytes(StandardCharsets.UTF_8))) {
            put(path, byteArrayInputStream);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 检查路径是否存在。
     *
     * @param path 路径
     */
    default boolean exists(String path) {
        return null != meta(path);
    }

    /**
     * 使用UTF8编码读取文件。
     *
     * @param path 文件路径
     * @throws InvalidFileException 无效文件
     */
    default String read(String path) throws InvalidFileException {
        return read(path, StandardCharsets.UTF_8);
    }

    /**
     * @param path    文件路径
     * @param charset 文件编码
     * @throws InvalidFileException 无效文件
     */
    default String read(String path, Charset charset) throws InvalidFileException {
        try (InputStream inputStream = openReadStream(path)) {
            return StreamUtils.copyToString(inputStream, charset);
        }
        catch (IOException e) {
            throw new InvalidFileException(path);
        }
    }

    /**
     * 用流的方式读取文件内容，调用方负责流的关闭。
     *
     * @param path 路径
     * @throws InvalidFileException 无效文件
     */
    InputStream openReadStream(String path) throws InvalidFileException;

    /**
     * 用流的方式写入文件内容，调用方负责流的关闭。
     * <ul>
     * <li>当文件不存在时会自动创建。</li>
     * <li>以Append的方式写入。</li>
     * </ul>
     *
     * @param path 路径
     * @throws InvalidFileException 无效文件
     */
    OutputStream openWriteStream(String path) throws InvalidFileException;

    /**
     * 以 replaceExisting=false 的方式移动文件或目录。
     *
     * @see #move(String, String, boolean)
     */
    default void move(String source, String destination) throws FileSystemException {
        move(source, destination, false);
    }

    /**
     * 移动文件或目录，{@link PathMeta}保持不变。
     *
     * @param source          源路径
     * @param destination     目标路径
     * @param replaceExisting 如果true，替换现存的文件，否则忽略移动。
     * @throws PathNotFoundException 源路径不存在
     * @throws FileSystemException   文件系统操作失败
     */
    void move(String source, String destination, boolean replaceExisting) throws FileSystemException;

    /**
     * 以 replaceExisting=false 的方式复制文件或目录。
     *
     * @see #copy(String, String, boolean)
     */
    default void copy(String source, String destination) throws FileSystemException {
        copy(source, destination, false);
    }

    /**
     * 复制文件或目录，{@link PathMeta}保持不变。
     *
     * @param source          源路径
     * @param destination     目标路径
     * @param replaceExisting 如果true，替换现存的文件，否则忽略复制。
     * @throws PathNotFoundException 源路径不存在
     * @throws FileSystemException   文件系统操作失败
     */
    void copy(String source, String destination, boolean replaceExisting) throws FileSystemException;

    /**
     * 删除文件或目录。
     *
     * @param path 路径
     */
    void delete(String path);

    /**
     * 刷新文件或目录的 {@link PathMeta#getCreationTime()}。
     *
     * @param path 路径
     */
    void touchCreation(String path);

    /**
     * 刷新文件或目录的 {@link PathMeta#getLastModifiedTime()}。
     *
     * @param path 路径
     */
    void touchLastModified(String path);

    /**
     * 刷新文件或目录的 {@link PathMeta#getLastAccessTime()}。
     *
     * @param path 路径
     */
    void touchLastAccess(String path);

    /**
     * 获取路径的元信息。
     *
     * @param path 路径
     * @return 路径不存在时，返回null。
     */
    PathMeta meta(String path);

    /**
     * 获取目录下子目录，非递归。
     *
     * @see #directories(String, String, boolean, int, int, PathMetaSort)
     */
    default Stream<? extends PathMeta> directories(String path) throws PathNotFoundException {
        return directories(path, "", false);
    }

    /**
     * 获取目录下子目录，非递归。
     *
     * @see #directories(String, String, boolean, int, int, PathMetaSort)
     */
    default Stream<? extends PathMeta> directories(String path, String search) throws PathNotFoundException {
        return directories(path, search, false);
    }

    /**
     * 获取目录下子目录。
     *
     * @see #directories(String, String, boolean, int, int, PathMetaSort)
     */
    default Stream<? extends PathMeta> directories(String path, String search, boolean recursive) throws PathNotFoundException {
        return directories(path, search, recursive, -1, -1, null);
    }

    /**
     * 获取目录下子目录。
     *
     * @param path      目录路径
     * @param search    以contains方式查找目录名。如果为空，返回所有结果。
     * @param recursive 如果true，则递归搜索所有子目录。
     * @param offset    开始位置索引。小于0则忽略。
     * @param limit     数量限制。小于0则忽略。
     * @param sort      排序选项。选项之间是互斥的。
     * @throws PathNotFoundException 目录不存在
     */
    Stream<? extends PathMeta> directories(String path, String search, boolean recursive, int offset, int limit, PathMetaSort sort) throws PathNotFoundException;

    /**
     * 获取目录下文件，非递归。
     *
     * @see #files(String, String, boolean, int, int, PathMetaSort)
     */
    default Stream<? extends PathMeta> files(String path) throws PathNotFoundException {
        return files(path, "", false);
    }

    /**
     * 获取目录下文件，非递归。
     *
     * @see #files(String, String, boolean, int, int, PathMetaSort)
     */
    default Stream<? extends PathMeta> files(String path, String search) throws PathNotFoundException {
        return files(path, search, false);
    }

    /**
     * 获取目录下文件。
     *
     * @see #files(String, String, boolean, int, int, PathMetaSort)
     */
    default Stream<? extends PathMeta> files(String path, String search, boolean recursive) throws PathNotFoundException {
        return files(path, search, recursive, -1, -1, null);
    }

    /**
     * 获取目录下文件。
     *
     * @param path      目录路径
     * @param search    以contains方式查找文件名。如果为空，返回所有结果。
     * @param recursive 如果true，则递归搜索所有子目录。
     * @param offset    开始位置索引。小于0则忽略。
     * @param limit     数量限制。小于0则忽略。
     * @param sort      排序选项。选项之间是互斥的。
     * @throws PathNotFoundException 目录不存在
     */
    Stream<? extends PathMeta> files(String path, String search, boolean recursive, int offset, int limit, PathMetaSort sort) throws PathNotFoundException;
}
