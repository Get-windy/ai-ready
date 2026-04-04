package cn.aiedge.storage.service.impl;

import cn.aiedge.storage.config.StorageProperties;
import cn.aiedge.storage.model.FileInfo;
import cn.aiedge.storage.service.StorageStrategy;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO存储策略
 * 兼容S3协议的开源对象存储
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "minio")
public class MinioStorageStrategy implements StorageStrategy {

    private final StorageProperties.S3Config config;
    private MinioClient minioClient;

    public MinioStorageStrategy(StorageProperties properties) {
        this.config = properties.getS3();
        init();
    }

    private void init() {
        if (config.isEnabled()) {
            this.minioClient = MinioClient.builder()
                .endpoint(config.getEndpoint())
                .credentials(config.getAccessKeyId(), config.getSecretAccessKey())
                .region(config.getRegion() != null ? config.getRegion() : "us-east-1")
                .build();
            
            // 确保bucket存在
            try {
                boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(config.getBucketName())
                    .build());
                
                if (!exists) {
                    minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(config.getBucketName())
                        .build());
                    log.info("MinIO bucket创建成功: {}", config.getBucketName());
                }
            } catch (Exception e) {
                log.error("检查/创建MinIO bucket失败", e);
            }
            
            log.info("MinIO客户端初始化成功: endpoint={}, bucket={}", 
                config.getEndpoint(), config.getBucketName());
        }
    }

    @Override
    public String getType() {
        return "minio";
    }

    @Override
    public FileInfo upload(InputStream inputStream, String fileName, String contentType) throws Exception {
        String filePath = generateFilePath(fileName);
        return upload(inputStream, filePath, fileName, contentType);
    }

    @Override
    public FileInfo upload(InputStream inputStream, String filePath, String fileName, String contentType) throws Exception {
        String fullPath = config.getPathPrefix() + filePath;
        
        ObjectWriteResponse response = minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(config.getBucketName())
                .object(fullPath)
                .stream(inputStream, inputStream.available(), -1)
                .contentType(contentType)
                .build()
        );
        
        log.debug("MinIO上传成功: path={}, etag={}", fullPath, response.etag());
        
        // 获取文件大小
        StatObjectResponse stat = minioClient.statObject(
            StatObjectArgs.builder()
                .bucket(config.getBucketName())
                .object(fullPath)
                .build()
        );
        
        return FileInfo.builder()
            .fileName(fileName)
            .filePath(fullPath)
            .fileSize(stat.size())
            .contentType(contentType)
            .etag(response.etag())
            .storageType("minio")
            .build();
    }

    @Override
    public InputStream download(String filePath) throws Exception {
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(config.getBucketName())
                .object(filePath)
                .build()
        );
    }

    @Override
    public boolean delete(String filePath) throws Exception {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(config.getBucketName())
                .object(filePath)
                .build()
        );
        log.debug("MinIO删除成功: path={}", filePath);
        return true;
    }

    @Override
    public int deleteBatch(List<String> filePaths) throws Exception {
        List<DeleteObject> objects = filePaths.stream()
            .map(DeleteObject::new)
            .toList();
        
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(
            RemoveObjectsArgs.builder()
                .bucket(config.getBucketName())
                .objects(objects)
                .build()
        );
        
        int count = 0;
        for (Result<DeleteError> result : results) {
            DeleteError error = result.get();
            if (error == null) {
                count++;
            } else {
                log.warn("MinIO删除失败: {}", error.message());
            }
        }
        return count;
    }

    @Override
    public String getAccessUrl(String filePath, int expireSeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(config.getBucketName())
                    .object(filePath)
                    .expiry(expireSeconds, TimeUnit.SECONDS)
                    .build()
            );
        } catch (Exception e) {
            log.error("获取MinIO预签名URL失败", e);
            return null;
        }
    }

    @Override
    public String getPermanentUrl(String filePath) {
        return config.getEndpoint() + "/" + config.getBucketName() + "/" + filePath;
    }

    @Override
    public boolean exists(String filePath) {
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(config.getBucketName())
                    .object(filePath)
                    .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public long getFileSize(String filePath) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(config.getBucketName())
                    .object(filePath)
                    .build()
            );
            return stat.size();
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public boolean copy(String sourcePath, String targetPath) throws Exception {
        minioClient.copyObject(
            CopyObjectArgs.builder()
                .bucket(config.getBucketName())
                .object(targetPath)
                .source(
                    CopySource.builder()
                        .bucket(config.getBucketName())
                        .object(sourcePath)
                        .build()
                )
                .build()
        );
        return true;
    }

    @Override
    public boolean move(String sourcePath, String targetPath) throws Exception {
        copy(sourcePath, targetPath);
        delete(sourcePath);
        return true;
    }

    private String generateFilePath(String fileName) {
        String ext = getFileExtension(fileName);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        Date now = new Date();
        String datePath = String.format("%tY/%tm/%td/", now, now, now);
        return datePath + uuid + (ext.isEmpty() ? "" : "." + ext);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex + 1) : "";
    }
}
