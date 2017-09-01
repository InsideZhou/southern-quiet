package com.ai.southernquiet.filesystem.driver;

import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.filesystem.*;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.gridfs.GridFsCriteria;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.util.CloseableIterator;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link FileSystem}的mongodb驱动。
 */
public class MongoDbFileSystem implements FileSystem {
    private static Logger logger = LoggerFactory.getLogger(MongoDbFileSystem.class);

    private MongoOperations mongoOperations;
    private GridFsOperations gridFsOperations;
    private GridFS gridFs;
    private String fileCollection;
    private String directoryCollection;
    private int fileSizeThreshold;

    public MongoDbFileSystem(MongoDbFileSystemAutoConfiguration.Properties properties, MongoOperations mongoOperations, GridFsOperations gridFsOperations, GridFS gridFS) {
        this.fileCollection = properties.getFileCollection();
        this.directoryCollection = properties.getDirectoryCollection();

        Integer threshHold = properties.getFileSizeThreshold();
        if (16 * 1024 * 1024 <= threshHold) {
            logger.warn("阈值{}无效，mongodb限制必须小于16m，目前使用默认值。", threshHold);
        }
        else {
            this.fileSizeThreshold = threshHold;
        }

        this.mongoOperations = mongoOperations;
        this.gridFsOperations = gridFsOperations;
        this.gridFs = gridFS;

        if (!mongoOperations.collectionExists(this.fileCollection)) {
            mongoOperations.createCollection(this.fileCollection);
        }

        if (!mongoOperations.collectionExists(this.directoryCollection)) {
            mongoOperations.createCollection(this.directoryCollection);
        }
    }

    @Override
    public void createDirectory(String path) {
        createAndGetDirectory(path);
    }

    @Override
    public void put(String path, InputStream stream) throws InvalidFileException {
        Assert.notNull(stream, "stream");

        String normalizedPath = FileSystem.normalizePath(path);
        put(stream, newPathQuery(normalizedPath), normalizedPath);
    }

    @Override
    public boolean exists(String path) {
        Query query = newPathQuery(FileSystem.normalizePath(path));

        return mongoOperations.exists(query, fileCollection)
            || mongoOperations.exists(query, directoryCollection);
    }

    @Override
    public InputStream openReadStream(String path) throws InvalidFileException {
        String normalizePath = FileSystem.normalizePath(path);
        FileMeta fileMeta = queryFileMeta(normalizePath);
        if (null == fileMeta) throw new InvalidFileException(path);

        if (null == fileMeta.getFileId()) {
            return new ByteArrayInputStream(fileMeta.getFileData().getData());
        }

        GridFSDBFile gridFSDBFile = gridFs.findOne(fileMeta.getFileId());
        if (null == gridFSDBFile) throw new InvalidFileException(path);
        return gridFSDBFile.getInputStream();
    }

    @Override
    public OutputStream openWriteStream(String path) throws InvalidFileException {
        String normalizePath = FileSystem.normalizePath(path);
        Query query = newPathQuery(normalizePath);

        File tmp;
        try {
            tmp = File.createTempFile("sq_write_proxy", "");
            tmp.deleteOnExit();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        FileMeta fileMeta = mongoOperations.findOne(query, FileMeta.class, fileCollection);
        if (null != fileMeta) {
            if (null == fileMeta.getFileId()) {
                try (FileOutputStream fileOutputStream = new FileOutputStream(tmp)) {
                    fileOutputStream.write(fileMeta.getFileData().getData());
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else {
                try {
                    gridFs.findOne(fileMeta.getFileId()).writeTo(tmp);
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        try {
            return new FileOutputStream(tmp, true) {
                @Override
                public void close() throws IOException {
                    super.close();

                    try (FileInputStream fileInputStream = new FileInputStream(tmp)) {
                        put(fileInputStream, query, normalizePath);
                    }
                    catch (InvalidFileException e) {
                        throw new IOException(e);
                    }
                }
            };
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void move(String source, String destination, boolean replaceExisting) throws FileSystemException {
        copy(source, destination, replaceExisting);
        delete(source);
    }

    @Override
    public void copy(String source, String destination, boolean replaceExisting) throws FileSystemException {
        if (!exists(source)) throw new PathNotFoundException(source);

        String normalizedSrc = FileSystem.normalizePath(source);
        String normalizedDest = FileSystem.normalizePath(destination);

        FileMeta sourceFileMeta = queryFileMeta(normalizedSrc);
        FileMeta destFileMeta = queryFileMeta(normalizedDest);

        if (null == destFileMeta) {
            if (null == sourceFileMeta) {
                copyDirectoryToPath(normalizedSrc, normalizedDest, replaceExisting);
            }
            else {
                createDirectory(normalizedDest);

                destFileMeta = new FileMeta();
                BeanUtils.copyProperties(sourceFileMeta, destFileMeta);
                destFileMeta.setParent(normalizedDest);

                ObjectId fileId = gridFsOperations.store(
                    gridFs.findOne(sourceFileMeta.getFileId()).getInputStream(),
                    destFileMeta.getPath()
                );
                destFileMeta.setFileId(fileId);

                mongoOperations.insert(destFileMeta, fileCollection);
            }
        }
        else if (null != sourceFileMeta) {
            if (replaceExisting) {
                ObjectId fileId = gridFsOperations.store(
                    gridFs.findOne(sourceFileMeta.getFileId()).getInputStream(),
                    destFileMeta.getPath()
                );

                FileMeta newFile = new FileMeta();
                BeanUtils.copyProperties(sourceFileMeta, newFile);
                newFile.setFileId(fileId);
                newFile.setParent(destFileMeta.getParent());

                mongoOperations.updateMulti(
                    newPathQuery(destFileMeta),
                    Update.fromDocument(new Document(newFile.toMap())),
                    FileMeta.class,
                    fileCollection);

                gridFs.remove(destFileMeta.getFileId());
            }

        }
        else {
            throw new FileSystemException("不能把目录移动或复制到文件。");
        }
    }

    @Override
    public void delete(String path) {
        Query query = newPathQuery(FileSystem.normalizePath(path));

        FileMeta fileMeta = mongoOperations.findOne(query, FileMeta.class, fileCollection);
        MongoPathMeta directory = mongoOperations.findOne(query, MongoPathMeta.class, directoryCollection);

        if (null != fileMeta) {
            mongoOperations.remove(query, fileCollection);

            if (null != fileMeta.getFileId()) {
                gridFs.remove(fileMeta.getFileId());
            }
        }
        else if (null != directory) {
            mongoOperations.remove(query, directoryCollection);

            getFileMetasInDirectory(directory).forEachRemaining(meta -> {
                mongoOperations.remove(newPathQuery(meta.getPath()), this.fileCollection);

                if (null != meta.getFileId()) {
                    gridFs.remove(meta.getFileId());
                }
            });
        }
    }

    @Override
    public void touchCreation(String path) {
        touchPath(FileSystem.normalizePath(path), meta -> meta.setCreationTime(Instant.now()));
    }

    @Override
    public void touchLastModified(String path) {
        touchPath(FileSystem.normalizePath(path), meta -> meta.setLastModifiedTime(Instant.now()));
    }

    @Override
    public void touchLastAccess(String path) {
        touchPath(FileSystem.normalizePath(path), meta -> meta.setLastAccessTime(Instant.now()));
    }

    @Override
    public MongoPathMeta meta(String path) {
        String normalizePath = FileSystem.normalizePath(path);
        MongoPathMeta pathMeta = queryFileMeta(normalizePath);

        if (null == pathMeta) {
            pathMeta = queryDirectory(normalizePath);
        }

        return pathMeta;
    }

    @Override
    public Stream<MongoPathMeta> directories(String path, String search, boolean recursive, int offset, int limit, PathMetaSort sort) throws PathNotFoundException {
        String normalizePath = FileSystem.normalizePath(path);
        MongoPathMeta root = queryDirectory(normalizePath);
        if (null == root) throw new PathNotFoundException(path);

        Stream<MongoPathMeta> stream = directories(root, search, recursive);

        if (null != sort) {
            stream = FileSystem.sort(stream, sort);
        }

        if (offset > 0) {
            stream = stream.skip(offset);
        }

        if (limit > 0) {
            stream = stream.limit(limit);
        }

        return stream;
    }

    @Override
    public Stream<FileMeta> files(String path, String search, boolean recursive, int offset, int limit, PathMetaSort sort) throws PathNotFoundException {
        String normalizePath = FileSystem.normalizePath(path);
        MongoPathMeta root = queryDirectory(normalizePath);
        if (null == root) throw new PathNotFoundException(path);

        Query query = new Query();
        if (StringUtils.hasText(search)) {
            query = query.addCriteria(Criteria.where("name").regex(".*" + search + ".*"));
        }

        if (null != sort) {
            query = sort(query, sort);
        }

        if (offset > 0) {
            query = query.skip(offset);
        }

        if (limit > 0) {
            query = query.limit(limit);
        }

        if (!recursive) {
            Criteria criteria = Criteria.where("parent").is(root.getPath());
            return iteratorToStream(mongoOperations.stream(query.addCriteria(criteria), FileMeta.class, fileCollection));
        }

        List<String> directories = directories(root, "", true).map(PathMeta::getPath).collect(Collectors.toList());
        directories.add(root.getPath());
        query = query.addCriteria(Criteria.where("parent").in(directories));

        return iteratorToStream(mongoOperations.stream(query, FileMeta.class, fileCollection));
    }

    private <T> Stream<T> iteratorToStream(Iterator<T> iterator) {
        return org.springframework.data.util.StreamUtils.createStreamFromIterator(iterator);
    }

    private MongoPathMeta newPathMeta(String normalizedPath, InputStream stream) {
        PathMeta pathMeta;
        try {
            pathMeta = FileSystem.newPathMeta(normalizedPath, stream);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        MongoPathMeta meta = new MongoPathMeta();
        BeanUtils.copyProperties(pathMeta, meta);

        return meta;
    }

    private Query newPathQuery(String normalizedPath) {
        return Query.query(Criteria.where("name").is(FileSystem.getPathName(normalizedPath)).and("parent").is(FileSystem.getPathParent(normalizedPath)));
    }

    private Query newPathQuery(MongoPathMeta meta) {
        return Query.query(Criteria.where("_id").is(meta.getId()));
    }

    private Query newGridFsQuery(String normalizedPath) {
        return Query.query(GridFsCriteria.whereFilename().is(FileSystem.normalizePath(normalizedPath)));
    }

    private FileMeta queryFileMeta(String normalizedPath) {
        return mongoOperations.findOne(newPathQuery(normalizedPath), FileMeta.class, fileCollection);
    }

    private MongoPathMeta queryDirectory(String normalizedPath) {
        return mongoOperations.findOne(newPathQuery(normalizedPath), MongoPathMeta.class, directoryCollection);
    }

    private CloseableIterator<FileMeta> getFileMetasInDirectory(String normalizedPath) {
        return mongoOperations.stream(
            Query.query(Criteria.where("parent").is(normalizedPath)),
            FileMeta.class,
            fileCollection
        );
    }

    private CloseableIterator<FileMeta> getFileMetasInDirectory(MongoPathMeta directory) {
        return mongoOperations.stream(
            Query.query(Criteria.where("parent").is(directory.getPath())),
            FileMeta.class,
            fileCollection
        );
    }

    /**
     * 务必保证fileId、fileData其中之一不为空，读取时会依赖这个假设。
     */
    private void put(InputStream stream, Query query, String normalizedPath) throws InvalidFileException {
        MongoPathMeta pathMeta = meta(normalizedPath);

        FileMeta fileMeta = new FileMeta();
        if (null == pathMeta) {
            createAndGetDirectory(FileSystem.getPathParent(normalizedPath));
            pathMeta = newPathMeta(normalizedPath, stream);
            BeanUtils.copyProperties(pathMeta, fileMeta);
        }
        else {
            BeanUtils.copyProperties(pathMeta, fileMeta);
            fileMeta.setLastModifiedTime(Instant.now());
            try {
                fileMeta.setSize(stream.available());
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (pathMeta.isDirectory()) throw new InvalidFileException(normalizedPath);

        if (fileMeta.getSize() >= 0 && fileMeta.getSize() <= fileSizeThreshold) {
            try {
                fileMeta.setFileData(new Binary(StreamUtils.copyToByteArray(stream)));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            ObjectId objectId = gridFsOperations.store(stream, normalizedPath);
            fileMeta.setFileId(objectId);
            gridFsOperations.delete(newGridFsQuery(normalizedPath).addCriteria(GridFsCriteria.where("_id").ne(objectId)));
        }

        mongoOperations.upsert(query, Update.fromDocument(new Document(fileMeta.toMap())), FileMeta.class, fileCollection);
    }

    private void copyDirectoryToPath(String normalizedSrc, String normalizedDest, boolean replaceExisting) {
        if (!mongoOperations.exists(newPathQuery(normalizedDest), directoryCollection)) {
            createDirectory(normalizedDest);
        }

        CloseableIterator<FileMeta> iterator = getFileMetasInDirectory(normalizedSrc);

        if (replaceExisting) {
            iterator.forEachRemaining(meta -> {
                FileMeta destFileMeta = new FileMeta();
                BeanUtils.copyProperties(meta, destFileMeta);
                destFileMeta.setParent(normalizedDest);

                ObjectId fileId = gridFsOperations.store(
                    gridFs.findOne(meta.getFileId()).getInputStream(),
                    destFileMeta.getPath()
                );
                destFileMeta.setFileId(fileId);

                FileMeta exists = queryFileMeta(destFileMeta.getPath());

                if (null != exists) {
                    gridFs.remove(exists.getFileId());

                    mongoOperations.upsert(
                        newPathQuery(exists),
                        Update.fromDocument(new Document(destFileMeta.toMap())),
                        fileCollection
                    );
                }
                else {
                    mongoOperations.insert(destFileMeta, fileCollection);
                }
            });
        }
        else {
            iterator.forEachRemaining(meta -> {
                FileMeta destFileMeta = new FileMeta();
                BeanUtils.copyProperties(meta, destFileMeta);
                destFileMeta.setParent(normalizedDest);

                if (mongoOperations.exists(newPathQuery(destFileMeta.getPath()), fileCollection)) return;

                ObjectId fileId = gridFsOperations.store(
                    gridFs.findOne(meta.getFileId()).getInputStream(),
                    destFileMeta.getPath()
                );
                destFileMeta.setFileId(fileId);

                mongoOperations.insert(destFileMeta, fileCollection);
            });
        }
    }

    private void touchPath(String normalizedPath, Consumer<MongoPathMeta> consumer) {
        FileMeta fileMeta = queryFileMeta(normalizedPath);

        MongoPathMeta pathMeta;
        String collection;

        if (null != fileMeta) {
            pathMeta = fileMeta;
            collection = fileCollection;
        }
        else {
            MongoPathMeta directory = queryDirectory(normalizedPath);

            if (null != directory) {
                pathMeta = directory;
                collection = directoryCollection;
            }
            else {
                return;
            }
        }

        consumer.accept(pathMeta);

        mongoOperations.updateMulti(
            newPathQuery(pathMeta),
            Update.fromDocument(new Document(pathMeta.toMap())),
            collection
        );
    }

    private Stream<MongoPathMeta> directories(MongoPathMeta root, String search, boolean recursive) throws PathNotFoundException {
        Query query = Query.query(Criteria.where("parent").is(root.getPath()));
        if (StringUtils.hasText(search)) {
            query = query.addCriteria(Criteria.where("name").regex("*" + search + "*"));
        }

        if (!mongoOperations.exists(query, directoryCollection)) return Stream.empty();

        Stream<MongoPathMeta> stream = iteratorToStream(mongoOperations.stream(query, MongoPathMeta.class, directoryCollection));
        if (!recursive) return stream;

        Stream<MongoPathMeta> subStream = stream.flatMap(d -> {
            try {
                return directories(d, search, true);
            }
            catch (PathNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        return Stream.concat(stream, subStream);
    }

    private Query sort(Query query, PathMetaSort sort) {
        switch (sort) {
            case Name:
                return query.with(Sort.by(Sort.Order.asc("name")));
            case NameDesc:
                return query.with(Sort.by(Sort.Order.desc("name")));

            case IsDirectory:
                return query.with(Sort.by(Sort.Order.asc("isDirectory")));
            case IsDirectoryDesc:
                return query.with(Sort.by(Sort.Order.desc("isDirectory")));

            case CreationTime:
                return query.with(Sort.by(Sort.Order.asc("creationTime")));
            case CreationTimeDesc:
                return query.with(Sort.by(Sort.Order.desc("creationTime")));

            case LastAccessTime:
                return query.with(Sort.by(Sort.Order.asc("lastAccessTime")));
            case LastAccessTimeDesc:
                return query.with(Sort.by(Sort.Order.desc("lastAccessTime")));

            case LastModifiedTime:
                return query.with(Sort.by(Sort.Order.asc("lastModifiedTime")));
            case LastModifiedTimeDesc:
                return query.with(Sort.by(Sort.Order.desc("lastModifiedTime")));

            case Size:
                return query.with(Sort.by(Sort.Order.asc("size")));
            case SizeDesc:
                return query.with(Sort.by(Sort.Order.desc("size")));
            default:
                throw new RuntimeException();
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private MongoPathMeta createAndGetDirectory(String path) {
        MongoPathMeta meta = meta(path);
        if (null != meta) {
            if (!meta.isDirectory()) throw new RuntimeException(String.format("该路径%s指向一个已经存在的文件。", path));

            return meta;
        }

        String normalizedPath = FileSystem.normalizePath(path);
        createAndGetDirectory(FileSystem.getPathParent(normalizedPath));
        meta = newPathMeta(normalizedPath, null);
        mongoOperations.insert(meta, directoryCollection);

        return meta;
    }
}
