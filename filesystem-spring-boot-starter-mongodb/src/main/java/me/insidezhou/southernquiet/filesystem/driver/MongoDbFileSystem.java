package me.insidezhou.southernquiet.filesystem.driver;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSUploadStream;
import me.insidezhou.southernquiet.filesystem.FileSystem;
import me.insidezhou.southernquiet.filesystem.*;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
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
    private static final SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(MongoDbFileSystem.class);

    private final MongoOperations mongoOperations;
    private final GridFsOperations gridFsOperations;
    private final GridFSBucket gridFSBucket;
    private final String pathCollection;
    private int fileSizeThreshold;

    public MongoDbFileSystem(MongoDbFileSystemAutoConfiguration.Properties properties, MongoOperations mongoOperations, GridFsOperations gridFsOperations, MongoDatabase mongoDatabase) {
        this.pathCollection = properties.getPathCollection();

        Integer threshHold = properties.getFileSizeThreshold();
        if (16 * 1024 * 1024 <= threshHold) {
            log.message("阈值无效，mongodb限制必须小于16m，目前使用默认值")
                .context("threshold", threshHold)
                .warn();
        }
        else {
            this.fileSizeThreshold = threshHold;
        }

        this.mongoOperations = mongoOperations;
        this.gridFsOperations = gridFsOperations;
        this.gridFSBucket = GridFSBuckets.create(mongoDatabase);

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

        NormalizedPath normalizedPath = new NormalizedPath(path);

        MongoPathMeta file = queryPathMeta(normalizedPath);
        if (null == file) {
            MongoPathMeta directory = createAndGetDirectory(normalizedPath.getParentPath());
            file = new MongoPathMeta(normalizedPath, stream);
            file.setId(ObjectId.get().toString());
            file.setParentId(directory.getId());

            Instant now = Instant.now();
            file.setCreationTime(now);
            file.setLastModifiedTime(now);
            file.setLastAccessTime(now);
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
            if (null != file.getFileId()) {
                gridFSBucket.delete(file.getFileId());
            }

            ObjectId objectId = gridFsOperations.store(stream, file.getPath());
            file.setFileId(objectId);
        }

        //务必保证fileId、fileData其中之一不为空，读取时会依赖这个假设。
        mongoOperations.upsert(newPathQuery(file), Update.fromDocument(new Document(file.toMap())), MongoPathMeta.class, pathCollection);
    }

    @Override
    public InputStream openReadStream(String path) throws InvalidFileException {
        MongoPathMeta pathMeta = meta(path);
        if (pathMeta == null) {
            pathMeta = metaBySymbolicLink(path);
        }

        if (null == pathMeta) throw new InvalidFileException(path);

        if (null == pathMeta.getFileId()) {
            return new ByteArrayInputStream(pathMeta.getFileData().getData());
        }

        GridFsResource resource = gridFsOperations.getResource(pathMeta.getPath());
        if (!resource.exists()) throw new InvalidFileException(path);

        try {
            return resource.getInputStream();
        }
        catch (IOException e) {
            throw new InvalidFileException(path, e);
        }
    }

    @Override
    public void createSymbolicLink(String symbolicLink, String targetPath) {
        NormalizedPath normalizedPath = new NormalizedPath(targetPath);
        MongoPathMeta file = queryPathMeta(normalizedPath);

        //更新保存软链接,获取文件时判断是否存在软链接,有则直接返回
        mongoOperations.updateFirst(newPathQuery(file), Update.update("symbolicLink",symbolicLink), MongoPathMeta.class, pathCollection);
    }

    @Override
    public OutputStream openWriteStream(String path) throws InvalidFileException {
        MongoPathMeta pathMeta = meta(path);

        if (null == pathMeta) {
            pathMeta = new MongoPathMeta(path);
            pathMeta.setDirectory(false);
        }

        if (pathMeta.isDirectory()) throw new InvalidFileException(path);

        MongoPathMeta mongoPathMeta = pathMeta;
        String candidateFilename = pathMeta.getPath() + "_" + System.nanoTime();

        GridFSUploadStream stream = gridFSBucket.openUploadStream(candidateFilename);

        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(stream) {
            @Override
            public void close() throws IOException {
                super.close();

                if (null != mongoPathMeta.getFileId()) {
                    gridFSBucket.delete(mongoPathMeta.getFileId());
                }

                gridFSBucket.rename(stream.getObjectId(), mongoPathMeta.getPath());

                mongoPathMeta.setFileId(stream.getObjectId());
                mongoPathMeta.setFileData(null);

                mongoOperations.upsert(
                    newPathQuery(mongoPathMeta),
                    Update.fromDocument(new Document(mongoPathMeta.toMap())),
                    MongoPathMeta.class,
                    pathCollection
                );
            }
        };

        if (null != pathMeta.getFileId()) {
            gridFSBucket.downloadToStream(pathMeta.getFileId(), bufferedOutputStream);
        }
        else if (null != pathMeta.getFileData()) {
            try {
                bufferedOutputStream.write(pathMeta.getFileData().getData());
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return bufferedOutputStream;
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

    @SuppressWarnings("unchecked")
    @Override
    public <M extends PathMeta> M meta(String path) {
        return (M) queryPathMeta(new NormalizedPath(path));
    }

    @SuppressWarnings("unchecked")
    public <M extends PathMeta> M metaBySymbolicLink(String symbolicLink) {
        return (M) queryPathMetaBySymbolicLink(symbolicLink);
    }

    @Override
    public Stream<MongoPathMeta> directories(String path, String search, boolean recursive, int offset, int limit, PathMetaSort sort) throws PathNotFoundException {
        NormalizedPath normalizePath = new NormalizedPath(path);
        MongoPathMeta root = queryPathMeta(normalizePath);
        if (null == root || !root.isDirectory()) throw new PathNotFoundException(path);

        Stream<MongoPathMeta> stream = subDirectories(root, search, recursive);

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

        List<String> directories = subDirectories(root, search, true).map(MongoPathMeta::getPath).collect(Collectors.toList());
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
        return Query.query(Criteria.where("name").is(normalizedPath.getName()).and("parent").is(normalizedPath.getParent()));
    }

    private Query newPathQuery(String symbolicLink) {
        return Query.query(Criteria.where("symbolicLink").is(symbolicLink));
    }

    private MongoPathMeta queryPathMeta(NormalizedPath normalizedPath) {
        return mongoOperations.findOne(newPathQuery(normalizedPath), MongoPathMeta.class, pathCollection);
    }

    private MongoPathMeta queryPathMetaBySymbolicLink(String symbolicLink) {
        return mongoOperations.findOne(newPathQuery(symbolicLink), MongoPathMeta.class, pathCollection);
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

    private Stream<MongoPathMeta> subDirectories(MongoPathMeta root, String search, boolean recursive) {
        Query query = Query.query(Criteria.where("parent").is(root.getPath()).and("isDirectory").is(true));
        if (StringUtils.hasText(search)) {
            query = query.addCriteria(Criteria.where("name").regex("*" + search + "*"));
        }

        if (!mongoOperations.exists(query, pathCollection)) return Stream.empty();

        Stream<MongoPathMeta> stream = iteratorToStream(mongoOperations.stream(query, MongoPathMeta.class, pathCollection));
        if (!recursive) return stream;

        List<MongoPathMeta> subDirectories = stream.collect(Collectors.toList());
        return Stream.concat(subDirectories.stream(), subDirectories.stream().flatMap(d -> subDirectories(d, search, true)));
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
            if (!meta.isDirectory()) {
                throw new RuntimeException(String.format("该路径%s指向一个已经存在的文件。", normalizedPath.getName()));
            }

            return meta;
        }

        meta = new MongoPathMeta(normalizedPath, null);

        if (!NormalizedPath.ROOT.equals(normalizedPath)) {
            MongoPathMeta parent = createAndGetDirectory(normalizedPath.getParentPath());
            meta.setParentId(parent.getId());
        }

        mongoOperations.insert(meta, pathCollection);

        return meta;
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
                gridFsOperations.delete(query);
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
                gridFSBucket.delete(existFile.getFileId());
            }

            mongoOperations.updateFirst(newPathQuery(destFileMeta), Update.fromDocument(new Document(destFileMeta.toMap())), pathCollection);
        }
        else {
            MongoPathMeta destFileMeta = sourceFileMeta.clone();
            destFileMeta.setParentId(directory.getId());
            destFileMeta.setParent(directory.getPath());

            if (null != destFileMeta.getFileId()) {
                ObjectId fileId;
                try {
                    fileId = gridFsOperations.store(
                        gridFsOperations.getResource(sourceFileMeta.getPath()).getInputStream(),
                        destFileMeta.getPath()
                    );
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
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
