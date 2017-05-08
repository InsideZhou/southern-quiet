package com.ai.southernquiet.filesystem;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

/**
 * 文件系统。
 */
public interface FileSystem {
    /**
     * 创建目录。目录已存在则忽略。
     *
     * @param path 路径
     * @throws FileSystemException 文件系统操作失败
     */
    void create(String path) throws FileSystemException;

    /**
     * 创建文件。
     *
     * @param path   要写入的路径
     * @param stream 输入流
     * @throws PathAlreadyExistsException 文件已存在
     * @throws FileSystemException        文件系统操作失败
     */
    void create(String path, InputStream stream) throws FileSystemException;

    /**
     * 创建文件。
     *
     * @param path 要写入的路径
     * @param txt  输入文本
     * @throws PathAlreadyExistsException 文件已存在
     * @throws FileSystemException        文件系统操作失败
     */
    void create(String path, CharSequence txt) throws FileSystemException;

    /**
     * 如果文件未存在，则创建；否则替换。
     *
     * @param path   要写入的路径
     * @param stream 输入流
     * @throws FileSystemException 文件系统操作失败
     */
    void put(String path, InputStream stream) throws FileSystemException;

    /**
     * 如果文件未存在，则创建；否则替换。
     *
     * @param path 要写入的路径
     * @param txt  输入文本
     * @throws FileSystemException 文件系统操作失败
     */
    void put(String path, CharSequence txt) throws FileSystemException;

    /**
     * 检查路径是否存在。
     *
     * @param path 路径
     * @throws FileSystemException 文件系统操作失败
     */
    boolean exists(String path) throws FileSystemException;

    /**
     * @param path 文件路径
     * @throws PathNotFoundException 文件不存在
     * @throws FileSystemException   文件系统操作失败
     */
    InputStream read(String path) throws FileSystemException;

    /**
     * 使用UTF8编码读取文件。
     *
     * @param path 文件路径
     * @throws PathNotFoundException 文件不存在
     * @throws FileSystemException   文件系统操作失败
     */
    String readString(String path) throws FileSystemException;

    /**
     * @param path    文件路径
     * @param charset 文件编码
     * @throws PathNotFoundException 文件不存在
     * @throws FileSystemException   文件系统操作失败
     */
    String readString(String path, Charset charset) throws FileSystemException;

    /**
     * @throws PathAlreadyExistsException 目标路径已存在
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
     * @throws PathAlreadyExistsException 目标路径已存在
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
     * @throws FileSystemException 文件系统操作失败
     */
    void delete(String path) throws FileSystemException;

    /**
     * 获取路径的元信息。
     *
     * @param path 路径
     * @throws PathNotFoundException 路径不存在
     * @throws FileSystemException   文件系统操作失败
     */
    PathMeta meta(String path) throws FileSystemException;

    /**
     * @see #paths(String, boolean)
     */
    List<PathMeta> paths(String path) throws FileSystemException;

    /**
     * 获取目录下所有子路径。
     *
     * @param path      目录路径
     * @param recursive 如果true，则递归搜索所有子目录，广度优先。
     * @throws PathNotFoundException 目录不存在
     * @throws FileSystemException   文件系统操作失败
     */
    List<PathMeta> paths(String path, boolean recursive) throws FileSystemException;

    /**
     * @see #files(String, boolean)
     */
    List<PathMeta> files(String path) throws FileSystemException;

    /**
     * 获取目录下所有文件。
     *
     * @param path      目录路径
     * @param recursive 如果true，则递归搜索所有子目录，广度优先。
     * @throws PathNotFoundException 目录不存在
     * @throws FileSystemException   文件系统操作失败
     */
    List<PathMeta> files(String path, boolean recursive) throws FileSystemException;
}
