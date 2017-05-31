package com.ai.southernquiet.filesystem;

/**
 * 使用路径的哪个元信息进行排序。
 */
public enum PathMetaSort {
    Name,
    IsDirectory,
    CreationTime,
    LastModifiedTime,
    LastAccessTime,
    Size,
    NameDesc,
    IsDirectoryDesc,
    CreationTimeDesc,
    LastModifiedTimeDesc,
    LastAccessTimeDesc,
    SizeDesc
}
