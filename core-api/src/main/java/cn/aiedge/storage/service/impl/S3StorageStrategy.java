package cn.aiedge.storage.service.impl;

import cn.aiedge.storage.config.StorageProperties;
import cn.aiedge.storage.model.FileInfo;
import cn.aiedge.storage.service.StorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * AWS S3存储策略
 * 支持兼容S3协议的对象存储（如MinIO、Ceph等）
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "s3")
public class S3StorageStrategy implements StorageStrategy {

    private final StorageProperties.S3Config config;
    private S3Client s3Client;
    private S3Presigner s3Presigner;

    public S3StorageStrategy(StorageProperties properties) {
        this.config = properties.getS3();
        init();
    }

    private void init() {
        if (config.isEnabled()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(
                config.getAccessKeyId(),
                config.getSecretAccessKey()
            );
            
            this.s3Client = S3Client.builder()
                .region(Region.of(config.getRegion()))
                .endpointOverride(URI.create(config.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
            
            this.s3Presigner = S3Presigner.builder()
                .region(Region.of(config.getRegion()))
                .endpointOverride(URI.create(config.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
            
            log.info("S3客户端初始化成功: endpoint={}, bucket={}", 
                config.getEndpoint(), config.getBucketName());
        }
    }

    @Override
    public String getType() {
        return "s3";
    }

    @Override
    public FileInfo upload(InputStream inputStream, String fileName, String contentType) throws Exception {
        String filePath = generateFilePath(fileName);
        return upload(inputStream, filePath, fileName, contentType);
    }

    @Override
    public FileInfo upload(InputStream inputStream, String filePath, String fileName, String contentType) throws Exception {
        String fullPath = config.getPathPrefix() + filePath;
        
        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(config.getBucketName())
            .key(fullPath)
            .contentType(contentType)
            .metadata(java.util.Map.of("original-filename", fileName))
            .build();
        
        PutObjectResponse response = s3Client.putObject(
            putRequest,
            RequestBody.fromInputStream(inputStream, inputStream.available())
        );
        
        log.debug("S3上传成功: path={}, etag={}", fullPath, response.eTag());
        
        // 获取文件大小
        HeadObjectRequest headRequest = HeadObjectRequest.builder()
            .bucket(config.getBucketName())
            .key(fullPath)
            .build();
        HeadObjectResponse headResponse = s3Client.headObject(headRequest);
        
        return FileInfo.builder()
            .fileName(fileName)
            .filePath(fullPath)
            .fileSize(headResponse.contentLength())
            .contentType(contentType)
            .etag(response.eTag())
            .storageType("s3")
            .build();
    }

    @Override
    public InputStream download(String filePath) throws Exception {
        GetObjectRequest getRequest = GetObjectRequest.builder()
            .bucket(config.getBucketName())
            .key(filePath)
            .build();
        
        return s3Client.getObject(getRequest);
    }

    @Override
    public boolean delete(String filePath) throws Exception {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
            .bucket(config.getBucketName())
            .key(filePath)
            .build();
        
        s3Client.deleteObject(deleteRequest);
        log.debug("S3删除成功: path={}", filePath);
        return true;
    }

    @Override
    public int deleteBatch(List<String> filePaths) throws Exception {
        List<ObjectIdentifier> keys = filePaths.stream()
            .map(path -> ObjectIdentifier.builder().key(path).build())
            .toList();
        
        Delete delete = Delete.builder().objects(keys).build();
        DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
            .bucket(config.getBucketName())
            .delete(delete)
            .build();
        
        DeleteObjectsResponse response = s3Client.deleteObjects(deleteRequest);
        return response.deleted().size();
    }

    @Override
    public String getAccessUrl(String filePath, int expireSeconds) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(config.getBucketName())
            .key(filePath)
            .build();
        
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(expireSeconds))
            .getObjectRequest(getObjectRequest)
            .build();
        
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    @Override
    public String getPermanentUrl(String filePath) {
        return config.getEndpoint() + "/" + config.getBucketName() + "/" + filePath;
    }

    @Override
    public boolean exists(String filePath) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(config.getBucketName())
                .key(filePath)
                .build();
            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    public long getFileSize(String filePath) {
        HeadObjectRequest headRequest = HeadObjectRequest.builder()
            .bucket(config.getBucketName())
            .key(filePath)
            .build();
        HeadObjectResponse response = s3Client.headObject(headRequest);
        return response.contentLength();
    }

    @Override
    public boolean copy(String sourcePath, String targetPath) throws Exception {
        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
            .sourceBucket(config.getBucketName())
            .sourceKey(sourcePath)
            .destinationBucket(config.getBucketName())
            .destinationKey(targetPath)
            .build();
        
        s3Client.copyObject(copyRequest);
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

    /**
     * 销毁S3客户端
     */
    public void destroy() {
        if (s3Client != null) {
            s3Client.close();
        }
        if (s3Presigner != null) {
            s3Presigner.close();
        }
        log.info("S3客户端已关闭");
    }
}
