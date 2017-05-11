package com.ai.southernquiet.filesystem;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

/**
 * 文件系统。
 */
public interface FileSystem {
    String PATH_SEPARATOR = "/";

    /**
     * 创建目录。目录已存在则忽略。
     *
     * @param path 路径
     */
    void create(String path);

    /**
     * 创建文件。
     *
     * @param path   要写入的路径
     * @param stream 输入流
     * @throws PathAlreadyExistsException 文件已存在
     */
    void create(String path, InputStream stream) throws PathAlreadyExistsException;

    /**
     * 创建文件。
     *
     * @param path 要写入的路径
     * @param txt  输入文本
     * @throws PathAlreadyExistsException 文件已存在
     */
    void create(String path, CharSequence txt) throws PathAlreadyExistsException;

    /**
     * 如果文件未存在，则创建；否则替换。
     *
     * @param path   要写入的路径
     * @param stream 输入流
     */
    void put(String path, InputStream stream);

    /**
     * 如果文件未存在，则创建；否则替换。
     *
     * @param path 要写入的路径
     * @param txt  输入文本
     */
    void put(String path, CharSequence txt);

    /**
     * 检查路径是否存在。
     *
     * @param path 路径
     */
    boolean exists(String path);

    /**
     * 获取路径。
     *
     * @param path 路径
     */
    PathMeta getPath(String path);

    /**
     * @param path 文件路径
     * @throws PathNotFoundException 文件不存在
     */
    InputStream read(String path) throws PathNotFoundException;

    /**
     * 使用UTF8编码读取文件。
     *
     * @param path 文件路径
     * @throws PathNotFoundException 文件不存在
     */
    String readString(String path) throws PathNotFoundException;

    /**
     * @param path    文件路径
     * @param charset 文件编码
     * @throws PathNotFoundException 文件不存在
     */
    String readString(String path, Charset charset) throws PathNotFoundException;

    /**
     * @see #move(String, String, boolean)
     */
    void move(String source, String destination) throws FileSystemException;

    /**
     * 移动文件或目录。
     *
     * @param source       源路径
     * @param destination  目标路径
     * @param ignoreExists 如果true，则替换现存的文件，目录则合并。
     * @throws PathNotFoundException      源路径不存在
     * @throws PathAlreadyExistsException 目标路径已存在
     * @throws FileSystemException        文件系统操作失败
     */
    void move(String source, String destination, boolean ignoreExists) throws FileSystemException;


    /**
     * @see #copy(String, String, boolean)
     */
    void copy(String source, String destination) throws FileSystemException;

    /**
     * 复制文件或目录。
     *
     * @param source       源路径
     * @param destination  目标路径
     * @param ignoreExists 如果true，则替换现存的文件，目录则合并。
     * @throws PathNotFoundException      源路径不存在
     * @throws PathAlreadyExistsException 目标路径已存在
     * @throws FileSystemException        文件系统操作失败
     */
    void copy(String source, String destination, boolean ignoreExists) throws FileSystemException;

    /**
     * 删除文件或目录。
     *
     * @param path 路径
     */
    void delete(String path);

    /**
     * 刷新文件或目录的 {@link PathMeta#getLastAccessTime()}。
     *
     * @param path 路径
     */
    void touch(String path);

    /**
     * 获取路径的元信息。
     *
     * @param path 路径
     * @throws PathNotFoundException 路径不存在
     */
    PathMeta meta(String path) throws PathNotFoundException;

    /**
     * @see #paths(String, boolean, String)
     */
    List<PathMeta> paths(String path) throws PathNotFoundException;

    /**
     * @see #paths(String, boolean, String)
     */
    List<PathMeta> paths(String path, String search) throws PathNotFoundException;

    /**
     * 获取目录下所有子路径。
     *
     * @param path      目录路径
     * @param recursive 如果true，则递归搜索所有子目录，广度优先。
     * @param search    要搜索的名称。如果为空，返回所有结果。
     * @return 文件名中包含 {@code search} 的文件列表。
     * @throws PathNotFoundException 目录不存在
     */
    List<PathMeta> paths(String path, boolean recursive, String search) throws PathNotFoundException;

    /**
     * @see #files(String, boolean, String)
     */
    List<PathMeta> files(String path) throws PathNotFoundException;

    /**
     * @see #files(String, boolean, String)
     */
    List<PathMeta> files(String path, String search) throws PathNotFoundException;

    /**
     * 获取目录下所有文件路径。
     *
     * @param path      目录路径
     * @param recursive 如果true，则递归搜索所有子目录，广度优先。
     * @param search    要搜索的名称。如果为空，返回所有结果。
     * @return 文件名中包含 {@code search} 的文件列表。
     * @throws PathNotFoundException 目录不存在
     */
    List<PathMeta> files(String path, boolean recursive, String search) throws PathNotFoundException;
}
