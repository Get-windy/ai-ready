package cn.aiedge.storage.service.impl;

import cn.aiedge.storage.config.StorageProperties;
import cn.aiedge.storage.model.FileInfo;
import cn.aiedge.storage.service.StorageStrategy;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 阿里云OSS存储策略
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "oss")
public class OssStorageStrategy implements StorageStrategy {

    private final StorageProperties.OssConfig config;
    private OSS ossClient;

    public OssStorageStrategy(StorageProperties properties) {
        this.config = properties.getOss();
        init();
    }

    private void init() {
        if (config.isEnabled()) {
            this.ossClient = new OSSClientBuilder().build(
                config.getEndpoint(),
                config.getAccessKeyId(),
                config.getAccessKeySecret()
            );
            log.info("OSS客户端初始化成功: endpoint={}, bucket={}", 
                config.getEndpoint(), config.getBucketName());
        }
    }

    @Override
    public String getType() {
        return "oss";
    }

    @Override
    public FileInfo upload(InputStream inputStream, String fileName, String contentType) throws Exception {
        String filePath = generateFilePath(fileName);
        return upload(inputStream, filePath, fileName, contentType);
    }

    @Override
    public FileInfo upload(InputStream inputStream, String filePath, String fileName, String contentType) throws Exception {
        // 添加路径前缀
        String fullPath = config.getPathPrefix() + filePath;
        
        // 设置元数据
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentDisposition("attachment;filename=" + fileName);
        
        // 上传文件
        PutObjectResult result = ossClient.putObject(
            config.getBucketName(), 
            fullPath, 
            inputStream, 
            metadata
        );
        
        log.debug("OSS上传成功: path={}, etag={}", fullPath, result.getETag());
        
        // 获取文件大小
        long size = ossClient.getObjectMetadata(config.getBucketName(), fullPath).getContentLength();
        
        return FileInfo.builder()
            .fileName(fileName)
            .filePath(fullPath)
            .fileSize(size)
            .contentType(contentType)
            .etag(result.getETag())
            .storageType("oss")
            .build();
    }

    @Override
    public InputStream download(String filePath) throws Exception {
        OSSObject ossObject = ossClient.getObject(config.getBucketName(), filePath);
        return ossObject.getObjectContent();
    }

    @Override
    public boolean delete(String filePath) throws Exception {
        ossClient.deleteObject(config.getBucketName(), filePath);
        log.debug("OSS删除成功: path={}", filePath);
        return true;
    }

    @Override
    public int deleteBatch(List<String> filePaths) throws Exception {
        int count = 0;
        for (String path : filePaths) {
            try {
                delete(path);
                count++;
            } catch (Exception e) {
                log.warn("批量删除失败: path={}", path, e);
            }
        }
        return count;
    }

    @Override
    public String getAccessUrl(String filePath, int expireSeconds) {
        Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000L);
        URL url = ossClient.generatePresignedUrl(config.getBucketName(), filePath, expiration);
        return url.toString();
    }

    @Override
    public String getPermanentUrl(String filePath) {
        if (config.getDomain() != null && !config.getDomain().isEmpty()) {
            return config.getDomain() + "/" + filePath;
        }
        return "https://" + config.getBucketName() + "." + config.getEndpoint() + "/" + filePath;
    }

    @Override
    public boolean exists(String filePath) {
        return ossClient.doesObjectExist(config.getBucketName(), filePath);
    }

    @Override
    public long getFileSize(String filePath) {
        return ossClient.getObjectMetadata(config.getBucketName(), filePath).getContentLength();
    }

    @Override
    public boolean copy(String sourcePath, String targetPath) throws Exception {
        ossClient.copyObject(config.getBucketName(), sourcePath, config.getBucketName(), targetPath);
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
     * 销毁OSS客户端
     */
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("OSS客户端已关闭");
        }
    }
}
