package com.familytree.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for MinIO file storage operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * Initialize bucket on application startup
     */
    @PostConstruct
    public void init() {
        try {
            createBucketIfNotExists();
            log.info("MinIO bucket '{}' is ready", bucketName);
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket", e);
        }
    }

    /**
     * Create bucket if it doesn't exist
     */
    private void createBucketIfNotExists() throws Exception {
        boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build()
        );

        if (!bucketExists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
            log.info("Created MinIO bucket: {}", bucketName);
        }
    }

    /**
     * Upload file to MinIO
     * @param file the multipart file
     * @param objectName the object name (path) in MinIO
     * @return the storage path
     */
    public String uploadFile(MultipartFile file, String objectName) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("Uploaded file to MinIO: {}", objectName);
            return objectName;
        }
    }

    /**
     * Upload file from input stream
     * @param inputStream the input stream
     * @param objectName the object name
     * @param contentType the content type
     * @param size the file size
     * @return the storage path
     */
    public String uploadFile(InputStream inputStream, String objectName, String contentType, long size) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(inputStream, size, -1)
                        .contentType(contentType)
                        .build()
        );

        log.info("Uploaded file to MinIO: {}", objectName);
        return objectName;
    }

    /**
     * Download file from MinIO
     * @param objectName the object name
     * @return the file input stream
     */
    public InputStream downloadFile(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * Delete file from MinIO
     * @param objectName the object name
     */
    public void deleteFile(String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );

        log.info("Deleted file from MinIO: {}", objectName);
    }

    /**
     * Get file metadata
     * @param objectName the object name
     * @return the stat object response
     */
    public StatObjectResponse getFileMetadata(String objectName) throws Exception {
        return minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * Check if file exists
     * @param objectName the object name
     * @return true if file exists
     */
    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * List files in a directory
     * @param prefix the directory prefix
     * @return list of object names
     */
    public List<String> listFiles(String prefix) throws Exception {
        List<String> objectNames = new ArrayList<>();

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .build()
        );

        for (Result<Item> result : results) {
            Item item = result.get();
            objectNames.add(item.objectName());
        }

        return objectNames;
    }

    /**
     * Delete all files in a directory
     * @param prefix the directory prefix
     */
    public void deleteDirectory(String prefix) throws Exception {
        List<String> files = listFiles(prefix);
        for (String file : files) {
            deleteFile(file);
        }
        log.info("Deleted directory from MinIO: {}", prefix);
    }

    /**
     * Generate unique object name for file
     * @param treeId the tree ID
     * @param individualId the individual ID
     * @param filename the original filename
     * @return the object name
     */
    public String generateObjectName(UUID treeId, UUID individualId, String filename) {
        String extension = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = filename.substring(dotIndex);
        }

        String uniqueId = UUID.randomUUID().toString();
        return String.format("%s/%s/%s%s", treeId, individualId, uniqueId, extension);
    }

    /**
     * Generate thumbnail object name
     * @param originalObjectName the original object name
     * @return the thumbnail object name
     */
    public String generateThumbnailName(String originalObjectName) {
        int dotIndex = originalObjectName.lastIndexOf('.');
        if (dotIndex > 0) {
            return originalObjectName.substring(0, dotIndex) + "_thumb" + originalObjectName.substring(dotIndex);
        }
        return originalObjectName + "_thumb";
    }
}
