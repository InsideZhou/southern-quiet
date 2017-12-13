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
    private String pathCollection;
    private int fileSizeThreshold;

    public MongoDbFileSystem(MongoDbFileSystemAutoConfiguration.Properties properties, MongoOperations mongoOperations, GridFsOperations gridFsOperations, GridFS gridFS) {
        this.pathCollection = properties.getPathCollection();

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

        if (!mongoOperations.collectionExists(this.pathCollection)) {
            mongoOperations.createCollection(this.pathCollection);
        }
    }

    @Override
    public void createDirectory(String path) {
        createAndGetDirectory(new NormalizedPath(path));
    }

    @Override
    public void put(String path, InputStream stream) throws InvalidFileException {
        Assert.notNull(stream, "stream");

        put(new NormalizedPath(path), stream);
    }

    @Override
    public InputStream openReadStream(String path) throws InvalidFileException {
        MongoPathMeta pathMeta = meta(path);
        if (null == pathMeta) throw new InvalidFileException(path);

        if (null == pathMeta.getFileId()) {
            return new ByteArrayInputStream(pathMeta.getFileData().getData());
        }

        GridFSDBFile gridFSDBFile = gridFs.findOne(pathMeta.getFileId());
        if (null == gridFSDBFile) throw new InvalidFileException(path);
        return gridFSDBFile.getInputStream();
    }

    @Override
    public OutputStream openWriteStream(String path) throws InvalidFileException {
        MongoPathMeta pathMeta = meta(path);
        if (null == pathMeta || pathMeta.isDirectory()) throw new InvalidFileException(path);

        File tmp;
        try {
            tmp = File.createTempFile("sq_write_proxy", "");
            tmp.deleteOnExit();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (null == pathMeta.getFileId()) {
            try (FileOutputStream fileOutputStream = new FileOutputStream(tmp)) {
                fileOutputStream.write(pathMeta.getFileData().getData());
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            try {
                gridFs.findOne(pathMeta.getFileId()).writeTo(tmp);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            return new FileOutputStream(tmp, true) {
                @Override
                public void close() throws IOException {
                    super.close();

                    try (FileInputStream fileInputStream = new FileInputStream(tmp)) {
                        put(new NormalizedPath(path), fileInputStream);
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
        NormalizedPath normalizedSrc = new NormalizedPath(source);
        NormalizedPath normalizedDest = new NormalizedPath(destination);

        MongoPathMeta sourcePathMeta = queryPathMeta(normalizedSrc);
        if (null == sourcePathMeta) throw new PathNotFoundException(source);

        MongoPathMeta destPathMeta = queryPathMeta(normalizedDest);
        if (null == destPathMeta) {
            MongoPathMeta destDirectory = createAndGetDirectory(normalizedDest);

            if (sourcePathMeta.isDirectory()) {
                copyFromDirectoryToDirectory(sourcePathMeta, destDirectory, replaceExisting);
            }
            else {
                copyFileToDirectory(sourcePathMeta, destDirectory, replaceExisting);
            }
        }
        else {
            if (sourcePathMeta.isDirectory()) {
                if (!destPathMeta.isDirectory()) throw new FileSystemException("不能把目录移动或复制到文件。");

                copyFromDirectoryToDirectory(sourcePathMeta, destPathMeta, replaceExisting);
            }
            else {
                MongoPathMeta destDirectory = destPathMeta.isDirectory() ? destPathMeta : queryPathMeta(destPathMeta.getParentId());
                copyFileToDirectory(sourcePathMeta, destDirectory, replaceExisting);
            }
        }
    }

    @Override
    public void delete(String path) {
        delete(new NormalizedPath(path));
    }

    @Override
    public void touchCreation(String path) {
        touchPath(new NormalizedPath(path), meta -> meta.setCreationTime(Instant.now()));
    }

    @Override
    public void touchLastModified(String path) {
        touchPath(new NormalizedPath(path), meta -> meta.setLastModifiedTime(Instant.now()));
    }

    @Override
    public void touchLastAccess(String path) {
        touchPath(new NormalizedPath(path), meta -> meta.setLastAccessTime(Instant.now()));
    }

    @Override
    public MongoPathMeta meta(String path) {
        return queryPathMeta(new NormalizedPath(path));
    }

    @Override
    public Stream<MongoPathMeta> directories(String path, String search, boolean recursive, int offset, int limit, PathMetaSort sort) throws PathNotFoundException {
        NormalizedPath normalizePath = new NormalizedPath(path);
        MongoPathMeta root = queryPathMeta(normalizePath);
        if (null == root || !root.isDirectory()) throw new PathNotFoundException(path);

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
    public Stream<? extends PathMeta> files(String path, String search, boolean recursive, int offset, int limit, PathMetaSort sort) throws PathNotFoundException {
        NormalizedPath normalizePath = new NormalizedPath(path);
        MongoPathMeta root = queryPathMeta(normalizePath);
        if (null == root || !root.isDirectory()) throw new PathNotFoundException(path);

        Query query = new Query(Criteria.where("isDirectory").is(false));
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
            return iteratorToStream(mongoOperations.stream(query.addCriteria(criteria), MongoPathMeta.class, pathCollection));
        }

        List<String> directories = directories(root, "", true).map(PathMeta::getPath).collect(Collectors.toList());
        directories.add(root.getPath());
        query = query.addCriteria(Criteria.where("parent").in(directories));

        return iteratorToStream(mongoOperations.stream(query, MongoPathMeta.class, pathCollection));
    }

    private <T> Stream<T> iteratorToStream(Iterator<T> iterator) {
        return org.springframework.data.util.StreamUtils.createStreamFromIterator(iterator);
    }

    private Query newPathQuery(MongoPathMeta meta) {
        return Query.query(Criteria.where("_id").is(meta.getId()));
    }

    private Query newPathQuery(NormalizedPath normalizedPath) {
        return Query.query(Criteria.where("name").is(normalizedPath.getName()).and("parent").is(normalizedPath.getParentPath()));
    }

    private MongoPathMeta queryPathMeta(NormalizedPath normalizedPath) {
        return mongoOperations.findOne(newPathQuery(normalizedPath), MongoPathMeta.class, pathCollection);
    }

    private MongoPathMeta queryPathMeta(String pathName, String parentId) {
        Query query = Query.query(Criteria.where("name").is(pathName).and("parentId").is(parentId));

        return mongoOperations.findOne(query, MongoPathMeta.class, pathCollection);
    }

    private MongoPathMeta queryPathMeta(String pathId) {
        Query query = Query.query(Criteria.where("_id").is(pathId));

        return mongoOperations.findOne(query, MongoPathMeta.class, pathCollection);
    }

    private CloseableIterator<MongoPathMeta> getPathsInDirectory(MongoPathMeta directory) {
        return mongoOperations.stream(
            Query.query(Criteria.where("parent").is(directory.getPath())),
            MongoPathMeta.class,
            pathCollection
        );
    }

    private void touchPath(NormalizedPath normalizedPath, Consumer<MongoPathMeta> consumer) {
        MongoPathMeta pathMeta = queryPathMeta(normalizedPath);

        consumer.accept(pathMeta);

        mongoOperations.updateFirst(
            newPathQuery(pathMeta),
            Update.fromDocument(new Document(pathMeta.toMap())),
            pathCollection
        );
    }

    private Stream<MongoPathMeta> directories(MongoPathMeta root, String search, boolean recursive) throws PathNotFoundException {
        Query query = Query.query(Criteria.where("parent").is(root.getPath()).and("isDirectory").is(true));
        if (StringUtils.hasText(search)) {
            query = query.addCriteria(Criteria.where("name").regex("*" + search + "*"));
        }

        if (!mongoOperations.exists(query, pathCollection)) return Stream.empty();

        Stream<MongoPathMeta> stream = iteratorToStream(mongoOperations.stream(query, MongoPathMeta.class, pathCollection));
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
    private MongoPathMeta createAndGetDirectory(NormalizedPath normalizedPath) {
        MongoPathMeta meta = queryPathMeta(normalizedPath);
        if (null != meta) {
            if (!meta.isDirectory())
                throw new RuntimeException(String.format("该路径%s指向一个已经存在的文件。", normalizedPath.getName()));

            return meta;
        }

        createAndGetDirectory(normalizedPath.getParentPath());
        meta = new MongoPathMeta(normalizedPath, null);
        mongoOperations.insert(meta, pathCollection);

        return meta;
    }

    /**
     * 务必保证fileId、fileData其中之一不为空，读取时会依赖这个假设。
     */
    private void put(NormalizedPath normalizedPath, InputStream stream) throws InvalidFileException {
        MongoPathMeta file = queryPathMeta(normalizedPath);
        if (null == file) {
            MongoPathMeta directory = createAndGetDirectory(normalizedPath.getParentPath());
            file = new MongoPathMeta(normalizedPath, stream);
            file.setParentId(directory.getId());
        }
        else if (file.isDirectory()) {
            throw new InvalidFileException(normalizedPath.toString());
        }
        else {
            file.setLastModifiedTime(Instant.now());
            try {
                file.setSize(stream.available());
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (file.getSize() <= fileSizeThreshold) {
            try {
                file.setFileData(new Binary(StreamUtils.copyToByteArray(stream)));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            ObjectId objectId = gridFsOperations.store(stream, file.getPath());
            file.setFileId(objectId);
            gridFsOperations.delete(Query.query(GridFsCriteria.whereFilename().is(file.getPath())).addCriteria(GridFsCriteria.where("_id").ne(objectId)));
        }

        mongoOperations.upsert(newPathQuery(file), Update.fromDocument(new Document(file.toMap())), MongoPathMeta.class, pathCollection);
    }

    private void delete(NormalizedPath normalizedPath) {
        Query query = newPathQuery(normalizedPath);

        MongoPathMeta pathMeta = mongoOperations.findOne(query, MongoPathMeta.class, pathCollection);
        if (null == pathMeta) return;

        if (pathMeta.isDirectory()) {
            delete(normalizedPath.getParentPath());
        }
        else {
            mongoOperations.remove(query, pathCollection);

            if (null != pathMeta.getFileId()) {
                gridFs.remove(pathMeta.getFileId());
            }
        }
    }

    private void copyFileToDirectory(MongoPathMeta sourceFileMeta, MongoPathMeta directory, boolean replaceExisting) {
        MongoPathMeta existFile = queryPathMeta(sourceFileMeta.getName(), directory.getId());

        if (null != existFile) {
            if (!replaceExisting) return;

            MongoPathMeta destFileMeta = sourceFileMeta.clone();
            destFileMeta.setParentId(directory.getId());
            destFileMeta.setParent(directory.getPath());

            if (null != existFile.getFileId()) {
                gridFs.remove(existFile.getFileId());
            }

            mongoOperations.updateFirst(newPathQuery(destFileMeta), Update.fromDocument(new Document(destFileMeta.toMap())), pathCollection);
        }
        else {
            MongoPathMeta destFileMeta = sourceFileMeta.clone();
            destFileMeta.setParentId(directory.getId());
            destFileMeta.setParent(directory.getPath());

            if (null != destFileMeta.getFileId()) {
                ObjectId fileId = gridFsOperations.store(
                    gridFs.findOne(destFileMeta.getFileId()).getInputStream(),
                    destFileMeta.getPath()
                );
                destFileMeta.setFileId(fileId);
            }

            mongoOperations.insert(destFileMeta, pathCollection);
        }
    }

    private void copyFromDirectoryToDirectory(MongoPathMeta srcDirectory, MongoPathMeta destDirectory, boolean replaceExisting) {
        CloseableIterator<MongoPathMeta> iterator = getPathsInDirectory(srcDirectory);
        iterator.forEachRemaining(meta -> {
            if (meta.isDirectory()) {
                MongoPathMeta subDirectory = meta.clone();
                subDirectory.setParent(destDirectory.getPath());
                subDirectory.setParentId(destDirectory.getId());
                copyFromDirectoryToDirectory(meta, subDirectory, replaceExisting);
            }
            else {
                copyFileToDirectory(meta, destDirectory, replaceExisting);
            }
        });
    }
}
