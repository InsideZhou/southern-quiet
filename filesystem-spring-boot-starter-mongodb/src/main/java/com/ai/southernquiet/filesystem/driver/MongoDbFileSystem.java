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
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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
    private String fileCollection = "FILE";
    private String directoryCollection = "DIRECTORY";
    private int fileSizeThreshold = 15 * 1024 * 1024;

    public MongoDbFileSystem(MongoDbFileSystemAutoConfiguration.Properties properties, MongoOperations mongoOperations, GridFsOperations gridFsOperations, GridFS gridFS) {
        String fileCollection = properties.getFileCollection();
        if (StringUtils.hasText(fileCollection)) {
            this.fileCollection = fileCollection;
        }

        String directoryCollection = properties.getDirectoryCollection();
        if (StringUtils.hasText(directoryCollection)) {
            this.directoryCollection = directoryCollection;
        }

        Integer threshHold = properties.getFileSizeThreshold();
        if (null != threshHold) {
            if (16 * 1024 * 1024 <= threshHold) {
                logger.warn("阈值{}无效，mongodb限制必须小于16m，目前使用默认值。", threshHold);
            }
            else {
                this.fileSizeThreshold = threshHold;
            }
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
    public void create(String path) {
        for (String current : generateDirectories(FileSystem.normalizePath(path))) {
            if (!mongoOperations.exists(newPathQuery(current), directoryCollection)) {
                MongoPathMeta meta = newPathMeta(current, null);
                meta.setDirectory(true);

                mongoOperations.insert(meta, directoryCollection);
            }
        }
    }

    @Override
    public void put(String path, InputStream stream) throws InvalidFileException {
        String normalizedPath = FileSystem.normalizePath(path);
        put(stream, newPathQuery(normalizedPath), normalizedPath);
    }

    @Override
    public void put(String path, CharSequence txt) throws InvalidFileException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(txt.toString().getBytes(StandardCharsets.UTF_8))) {
            put(path, byteArrayInputStream);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean exists(String path) {
        Query query = newPathQuery(FileSystem.normalizePath(path));

        return mongoOperations.exists(query, fileCollection)
            || mongoOperations.exists(query, directoryCollection);
    }

    @Override
    public String read(String path) throws InvalidFileException {
        return read(path, StandardCharsets.UTF_8);
    }

    @Override
    public String read(String path, Charset charset) throws InvalidFileException {
        try (InputStream inputStream = openReadStream(path)) {
            return StreamUtils.copyToString(inputStream, charset);
        }
        catch (IOException e) {
            throw new InvalidFileException(path);
        }
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
                }
            };
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void move(String source, String destination) throws FileSystemException {
        move(source, destination, false);
    }

    @Override
    public void move(String source, String destination, boolean replaceExisting) throws FileSystemException {
        copy(source, destination, replaceExisting);
        delete(source);
    }

    @Override
    public void copy(String source, String destination) throws FileSystemException {
        copy(source, destination, false);
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
                create(normalizedDest);

                destFileMeta = new FileMeta(sourceFileMeta);
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

                FileMeta newFile = new FileMeta(sourceFileMeta);
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
    public PathMeta meta(String path) {
        String normalizePath = FileSystem.normalizePath(path);
        MongoPathMeta pathMeta = queryFileMeta(normalizePath);

        if (null == pathMeta) {
            pathMeta = queryDirectory(normalizePath);
        }

        return pathMeta;
    }

    @Override
    public Stream<MongoPathMeta> directories(String path) throws PathNotFoundException {
        return directories(path, "", false, -1, -1, null);
    }

    @Override
    public Stream<? extends PathMeta> directories(String path, String search) throws PathNotFoundException {
        return directories(path, search, false, -1, -1, null);
    }

    @Override
    public Stream<? extends PathMeta> directories(String path, String search, boolean recursive) throws PathNotFoundException {
        return directories(path, search, recursive, -1, -1, null);
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
    public Stream<FileMeta> files(String path) throws PathNotFoundException {
        return files(path, "", false, -1, -1, null);
    }

    @Override
    public Stream<? extends PathMeta> files(String path, String search) throws PathNotFoundException {
        return files(path, search, false, -1, -1, null);
    }

    @Override
    public Stream<? extends PathMeta> files(String path, String search, boolean recursive) throws PathNotFoundException {
        return files(path, search, recursive, -1, -1, null);
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

        List<String> directories = directories(root, "", true).map(m -> m.getPath()).collect(Collectors.toList());
        directories.add(root.getPath());
        query = query.addCriteria(Criteria.where("parent").in(directories));

        return iteratorToStream(mongoOperations.stream(query, FileMeta.class, fileCollection));
    }

    private <T> Stream<T> iteratorToStream(Iterator<T> iterator) {
        return org.springframework.data.util.StreamUtils.createStreamFromIterator(iterator);
    }

    private MongoPathMeta newPathMeta(String normalizedPath, InputStream stream) {
        MongoPathMeta meta = new MongoPathMeta();
        Instant now = Instant.now();

        meta.setParent(FileSystem.getPathParent(normalizedPath));
        meta.setName(FileSystem.getPathName(normalizedPath));
        meta.setDirectory(false);
        meta.setCreationTime(now);
        meta.setLastAccessTime(now);
        meta.setLastModifiedTime(now);

        if (null == stream) {
            meta.setSize(-1);
        }
        else {
            try {
                meta.setSize(stream.available());
            }
            catch (IOException e) {
                meta.setSize(-1);
            }
        }

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

    private ObjectId replaceGridFsFile(InputStream stream, String normalizedPath) {
        gridFsOperations.delete(newGridFsQuery(normalizedPath));
        return gridFsOperations.store(stream, normalizedPath);
    }

    private List<String> generateDirectories(String normalizedPath) {
        List<String> result = new ArrayList<>();

        do {
            result.add(normalizedPath);
            normalizedPath = FileSystem.getPathParent(normalizedPath);
        }
        while (!"".equals(normalizedPath));

        Collections.reverse(result);
        return result;
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
    private void put(InputStream stream, Query query, String normalizedPath) {
        MongoPathMeta pathMeta = newPathMeta(normalizedPath, stream);
        create(pathMeta.getParent());

        FileMeta fileMeta = new FileMeta(pathMeta);

        if (fileMeta.getSize() >= 0 && fileMeta.getSize() <= fileSizeThreshold) {
            try {
                fileMeta.setFileData(new Binary(StreamUtils.copyToByteArray(stream)));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            fileMeta.setFileId(gridFsOperations.store(stream, normalizedPath));
        }

        mongoOperations.upsert(query, Update.fromDocument(new Document(fileMeta.toMap())), FileMeta.class, fileCollection);
        gridFsOperations.delete(newGridFsQuery(normalizedPath));
    }

    private void copyDirectoryToPath(String normalizedSrc, String normalizedDest, boolean replaceExisting) {
        if (!mongoOperations.exists(newPathQuery(normalizedDest), directoryCollection)) {
            create(normalizedDest);
        }

        CloseableIterator<FileMeta> iterator = getFileMetasInDirectory(normalizedSrc);

        if (replaceExisting) {
            iterator.forEachRemaining(meta -> {
                FileMeta destFileMeta = new FileMeta(meta);
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
                FileMeta destFileMeta = new FileMeta(meta);
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
}
