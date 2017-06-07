package com.ai.southernquiet.filesystem.driver;

import com.ai.southernquiet.FrameworkProperties;
import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.filesystem.*;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import org.bson.Document;
import org.bson.types.ObjectId;
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
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private MongoOperations mongoOperations;
    private GridFsOperations gridFsOperations;
    private GridFS gridFs;
    private String fileCollection = "DEFAULT";
    private String directoryCollection = "DIRECTORY";

    public MongoDbFileSystem(FrameworkProperties properties, MongoOperations mongoOperations, GridFsOperations gridFsOperations, GridFS gridFS) {
        String fileCollection = properties.getFileSystem().getMongodb().getFileCollection();
        if (StringUtils.hasText(fileCollection)) {
            this.fileCollection = fileCollection;
        }

        String directoryCollection = properties.getFileSystem().getMongodb().getDirectoryCollection();
        if (StringUtils.hasText(directoryCollection)) {
            this.directoryCollection = directoryCollection;
        }

        this.mongoOperations = mongoOperations;
        this.gridFsOperations = gridFsOperations;
        this.gridFs = gridFS;
    }

    @Override
    public void create(String path) {
        if (!StringUtils.hasText(path)) return;

        for (String current : generateCollections(path)) {
            if (!mongoOperations.exists(newDirectoryQuery(current), directoryCollection)) {
                MongoPathMeta meta = newPathMeta(current, null);
                meta.setDirectory(true);

                mongoOperations.insert(meta, directoryCollection);
            }
        }
    }

    @Override
    public void put(String path, InputStream stream) throws InvalidFileException {
        put(stream, newFileQuery(path), path);
    }

    @Override
    public void put(String path, CharSequence txt) throws InvalidFileException {
        put(path, new ByteArrayInputStream(txt.toString().getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public boolean exists(String path) {
        return mongoOperations.exists(newFileQuery(path), fileCollection)
            || mongoOperations.exists(newDirectoryQuery(path), directoryCollection);
    }

    @Override
    public String read(String path) throws InvalidFileException {
        return read(path, StandardCharsets.UTF_8);
    }

    @Override
    public String read(String path, Charset charset) throws InvalidFileException {
        try {
            return StreamUtils.copyToString(openReadStream(path), charset);
        }
        catch (IOException e) {
            throw new InvalidFileException(path);
        }
    }

    @Override
    public InputStream openReadStream(String path) throws InvalidFileException {
        FileMeta fileMeta = queryFileMeta(path);
        if (null == fileMeta) throw new InvalidFileException(path);

        GridFSDBFile gridFSDBFile = gridFs.findOne(fileMeta.getFileId());
        if (null == gridFSDBFile) throw new InvalidFileException(path);
        return gridFSDBFile.getInputStream();
    }

    @Override
    public OutputStream openWriteStream(String path) throws InvalidFileException {
        Query query = newFileQuery(path);

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
            try {
                gridFs.findOne(fileMeta.getFileId()).writeTo(tmp);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        OutputStream out;
        try {
            out = new FileOutputStream(tmp);
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }

            @Override
            public void close() throws IOException {
                super.close();

                put(new FileInputStream(tmp), query, path);
            }
        };
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

        FileMeta sourceFileMeta = queryFileMeta(source);
        FileMeta destFileMeta = queryFileMeta(destination);

        if (null == destFileMeta) {
            if (null == sourceFileMeta) {
                copyDirectoryToPath(source, destination, replaceExisting);
            }
            else {
                create(destination);

                destFileMeta = newFileMeta(sourceFileMeta, null);
                destFileMeta.setParent(destination);

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

                FileMeta newFile = newFileMeta(sourceFileMeta, fileId);
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
        Query fileQuery = newFileQuery(path);
        Query directoryQuery = newDirectoryQuery(path);

        FileMeta fileMeta = mongoOperations.findOne(fileQuery, FileMeta.class, fileCollection);
        MongoPathMeta directory = mongoOperations.findOne(directoryQuery, MongoPathMeta.class, directoryCollection);

        if (null != fileMeta) {
            mongoOperations.remove(fileQuery, fileCollection);
            gridFs.remove(fileMeta.getFileId());
        }
        else if (null != directory) {
            mongoOperations.remove(directoryQuery, directoryCollection);

            getFileMetasInDirectory(directory).forEachRemaining(meta -> {
                mongoOperations.remove(newFileQuery(meta.getPath()), this.fileCollection);
                gridFs.remove(meta.getFileId());
            });
        }
    }

    @Override
    public void touchCreation(String path) {
        touchPath(path, meta -> meta.setCreationTime(Instant.now()));
    }

    @Override
    public void touchLastModified(String path) {
        touchPath(path, meta -> meta.setLastModifiedTime(Instant.now()));
    }

    @Override
    public void touchLastAccess(String path) {
        touchPath(path, meta -> meta.setLastAccessTime(Instant.now()));
    }

    @Override
    public PathMeta meta(String path) {
        MongoPathMeta pathMeta = queryFileMeta(path);

        if (null == pathMeta) {
            pathMeta = queryDirectory(path);
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
        MongoPathMeta root = queryDirectory(path);
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
        MongoPathMeta root = queryDirectory(path);
        if (null == root) throw new PathNotFoundException(path);

        Query query = new Query();
        if (StringUtils.hasText(search)) {
            query = query.addCriteria(Criteria.where("name").regex("*" + search + "*"));
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

    private String getParent(String path) {
        Path parent = Paths.get(path).getParent();

        return FileSystem.normalizePath(parent.toString());
    }

    private String getPathName(String path) {
        return Paths.get(path).getFileName().toString();
    }

    private MongoPathMeta newPathMeta(String path, InputStream stream) {
        MongoPathMeta meta = new MongoPathMeta();
        Instant now = Instant.now();

        meta.setParent(getParent(path));
        meta.setName(getPathName(path));
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

    private FileMeta newFileMeta(MongoPathMeta meta, ObjectId fileId) {
        FileMeta fileMeta = new FileMeta(meta);
        fileMeta.setFileId(fileId);
        return fileMeta;
    }

    private Query newFileQuery(String path) {
        return Query.query(Criteria.where("name").is(getPathName(path)).and("parent").is(getParent(path)));
    }

    private Query newDirectoryQuery(String path) {
        return Query.query(Criteria.where("path").is(FileSystem.normalizePath(path)));
    }

    private Query newPathQuery(MongoPathMeta meta) {
        return Query.query(Criteria.where("_id").is(meta.getId()));
    }

    private Query newGridFsQuery(String path) {
        return Query.query(GridFsCriteria.whereFilename().is(FileSystem.normalizePath(path)));
    }

    private FileMeta queryFileMeta(String path) {
        return mongoOperations.findOne(newFileQuery(path), FileMeta.class, fileCollection);
    }

    private MongoPathMeta queryDirectory(String path) {
        return mongoOperations.findOne(newDirectoryQuery(path), MongoPathMeta.class, directoryCollection);
    }

    private ObjectId replaceGridFsFile(InputStream stream, String path) {
        gridFsOperations.delete(newGridFsQuery(path));
        return gridFsOperations.store(stream, FileSystem.normalizePath(path));
    }

    private List<String> generateCollections(String path) {
        List<String> result = new ArrayList<>();
        result.add(path);

        Path parent = Paths.get(path).getParent();
        while (null != parent) {
            result.add(parent.toString());
            parent = parent.getParent();
        }

        Collections.reverse(result);

        return result;
    }

    private CloseableIterator<FileMeta> getFileMetasInDirectory(String path) {
        return mongoOperations.stream(
            Query.query(Criteria.where("parent").is(FileSystem.normalizePath(path))),
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

    private void put(InputStream stream, Query query, String path) {
        MongoPathMeta pathMeta = newPathMeta(path, stream);
        create(pathMeta.getParent());

        gridFsOperations.delete(newGridFsQuery(path));
        FileMeta fileMeta = newFileMeta(pathMeta, gridFsOperations.store(stream, FileSystem.normalizePath(path)));

        mongoOperations.upsert(query, Update.fromDocument(new Document(fileMeta.toMap())), FileMeta.class, fileCollection);
    }

    private void copyDirectoryToPath(String source, String destination, boolean replaceExisting) {
        if (!mongoOperations.exists(newDirectoryQuery(destination), directoryCollection)) {
            create(destination);
        }

        CloseableIterator<FileMeta> iterator = getFileMetasInDirectory(source);

        if (replaceExisting) {
            iterator.forEachRemaining(meta -> {
                FileMeta destFileMeta = new FileMeta(meta);
                destFileMeta.setParent(destination);

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
                destFileMeta.setParent(destination);

                if (mongoOperations.exists(newFileQuery(destFileMeta.getPath()), fileCollection)) return;

                ObjectId fileId = gridFsOperations.store(
                    gridFs.findOne(meta.getFileId()).getInputStream(),
                    destFileMeta.getPath()
                );
                destFileMeta.setFileId(fileId);

                mongoOperations.insert(destFileMeta, fileCollection);
            });
        }
    }

    private void touchPath(String path, Consumer<MongoPathMeta> consumer) {
        FileMeta fileMeta = queryFileMeta(path);

        MongoPathMeta pathMeta;
        String collection;

        if (null != fileMeta) {
            pathMeta = fileMeta;
            collection = fileCollection;
        }
        else {
            MongoPathMeta directory = queryDirectory(path);

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
