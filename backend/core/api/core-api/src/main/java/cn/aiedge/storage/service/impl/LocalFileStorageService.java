package cn.aiedge.storage.service.impl;

import cn.aiedge.storage.config.StorageProperties;
import cn.aiedge.storage.model.StorageFile;
import cn.aiedge.storage.service.FileStorageService;
import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 本地文件存储服务实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private static final String STORAGE_TYPE = "local";
    private static final DateTimeFormatter DATE_PATH_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final StorageProperties properties;
    private final String basePath;

    public LocalFileStorageService(StorageProperties properties) {
        this.properties = properties;
        this.basePath = properties.getLocal().getBasePath();
        // 确保基础目录存在
        FileUtil.mkdir(basePath);
        log.info("Local storage initialized with base path: {}", basePath);
    }

    @Override
    public StorageFile upload(InputStream inputStream, String originalName, String contentType,
                              String bizType, String bizId) {
        String customPath = generateDatePath(bizType);
        return upload(inputStream, originalName, contentType, customPath, bizType, bizId);
    }

    @Override
    public StorageFile upload(InputStream inputStream, String originalName, String contentType,
                              String customPath, String bizType, String bizId) {
        try {
            // 生成文件ID和存储信息
            String fileId = generateFileId();
            String extension = FileUtil.extName(originalName);
            String storageName = fileId + (extension.isEmpty() ? "" : "." + extension);
            
            // 构建存储路径
            Path dirPath = Paths.get(basePath, customPath);
            Files.createDirectories(dirPath);
            
            Path filePath = dirPath.resolve(storageName);
            
            // 保存文件并计算MD5
            String md5Hash;
            long fileSize;
            try (OutputStream os = Files.newOutputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                fileSize = 0;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                    fileSize += bytesRead;
                }
            }
            
            // 计算MD5
            md5Hash = DigestUtil.md5Hex(filePath.toFile());
            
            // 构建返回信息
            String relativePath = customPath + "/" + storageName;
            String accessUrl = buildAccessUrl(relativePath);
            
            return StorageFile.builder()
                    .fileId(fileId)
                    .originalName(originalName)
                    .storageName(storageName)
                    .filePath(relativePath)
                    .fileSize(fileSize)
                    .extension(extension)
                    .contentType(contentType)
                    .md5Hash(md5Hash)
                    .storageType(STORAGE_TYPE)
                    .accessUrl(accessUrl)
                    .uploaderId(getCurrentUserId())
                    .bizType(bizType)
                    .bizId(bizId)
                    .uploadTime(LocalDateTime.now())
                    .build();
                    
        } catch (IOException e) {
            log.error("Failed to upload file: {}", originalName, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String fileId) {
        StorageFile fileInfo = getFileInfo(fileId);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在: " + fileId);
        }
        
        Path filePath = Paths.get(basePath, fileInfo.getFilePath());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("文件不存在: " + filePath);
        }
        
        try {
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            log.error("Failed to download file: {}", fileId, e);
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }
    }

    @Override
    public StorageFile getFileInfo(String fileId) {
        // 本地存储场景下，需要从数据库或元数据文件读取
        // 这里简化实现，实际应从数据库查询
        throw new UnsupportedOperationException("请使用FileRecordService查询文件信息");
    }

    @Override
    public boolean delete(String fileId) {
        try {
            StorageFile fileInfo = getFileInfo(fileId);
            if (fileInfo == null) {
                return false;
            }
            
            Path filePath = Paths.get(basePath, fileInfo.getFilePath());
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", fileId, e);
            return false;
        }
    }

    @Override
    public int deleteBatch(List<String> fileIds) {
        int count = 0;
        for (String fileId : fileIds) {
            if (delete(fileId)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean exists(String fileId) {
        StorageFile fileInfo = getFileInfo(fileId);
        if (fileInfo == null) {
            return false;
        }
        return Files.exists(Paths.get(basePath, fileInfo.getFilePath()));
    }

    @Override
    public String getAccessUrl(String fileId) {
        StorageFile fileInfo = getFileInfo(fileId);
        if (fileInfo == null) {
            return null;
        }
        return buildAccessUrl(fileInfo.getFilePath());
    }

    @Override
    public String getPresignedUrl(String fileId, int expireSeconds) {
        // 本地存储不支持预签名URL，返回普通访问URL
        return getAccessUrl(fileId);
    }

    @Override
    public StorageFile copy(String fileId, String targetBizType, String targetBizId) {
        try {
            StorageFile sourceFile = getFileInfo(fileId);
            if (sourceFile == null) {
                throw new RuntimeException("源文件不存在: " + fileId);
            }
            
            Path sourcePath = Paths.get(basePath, sourceFile.getFilePath());
            String targetPath = generateDatePath(targetBizType);
            String newFileId = generateFileId();
            String storageName = newFileId + "." + sourceFile.getExtension();
            
            Path targetDir = Paths.get(basePath, targetPath);
            Files.createDirectories(targetDir);
            Path targetFilePath = targetDir.resolve(storageName);
            
            Files.copy(sourcePath, targetFilePath);
            
            String relativePath = targetPath + "/" + storageName;
            
            return StorageFile.builder()
                    .fileId(newFileId)
                    .originalName(sourceFile.getOriginalName())
                    .storageName(storageName)
                    .filePath(relativePath)
                    .fileSize(sourceFile.getFileSize())
                    .extension(sourceFile.getExtension())
                    .contentType(sourceFile.getContentType())
                    .md5Hash(sourceFile.getMd5Hash())
                    .storageType(STORAGE_TYPE)
                    .accessUrl(buildAccessUrl(relativePath))
                    .uploaderId(getCurrentUserId())
                    .bizType(targetBizType)
                    .bizId(targetBizId)
                    .uploadTime(LocalDateTime.now())
                    .build();
                    
        } catch (IOException e) {
            log.error("Failed to copy file: {}", fileId, e);
            throw new RuntimeException("文件复制失败: " + e.getMessage(), e);
        }
    }

    @Override
    public StorageFile move(String fileId, String targetBizType, String targetBizId) {
        try {
            StorageFile sourceFile = getFileInfo(fileId);
            if (sourceFile == null) {
                throw new RuntimeException("源文件不存在: " + fileId);
            }
            
            Path sourcePath = Paths.get(basePath, sourceFile.getFilePath());
            String targetPath = generateDatePath(targetBizType);
            String storageName = sourceFile.getStorageName();
            
            Path targetDir = Paths.get(basePath, targetPath);
            Files.createDirectories(targetDir);
            Path targetFilePath = targetDir.resolve(storageName);
            
            Files.move(sourcePath, targetFilePath);
            
            String relativePath = targetPath + "/" + storageName;
            sourceFile.setFilePath(relativePath);
            sourceFile.setAccessUrl(buildAccessUrl(relativePath));
            sourceFile.setBizType(targetBizType);
            sourceFile.setBizId(targetBizId);
            
            return sourceFile;
            
        } catch (IOException e) {
            log.error("Failed to move file: {}", fileId, e);
            throw new RuntimeException("文件移动失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getStorageType() {
        return STORAGE_TYPE;
    }

    @Override
    public String getStorageName() {
        return "本地存储";
    }

    /**
     * 生成文件ID
     */
    private String generateFileId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成日期路径
     */
    private String generateDatePath(String bizType) {
        String dateStr = LocalDateTime.now().format(DATE_PATH_FORMAT);
        if (bizType != null && !bizType.isEmpty()) {
            return bizType + "/" + dateStr;
        }
        return dateStr;
    }

    /**
     * 构建访问URL
     */
    private String buildAccessUrl(String relativePath) {
        String baseUrl = properties.getLocal().getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            return "/files/" + relativePath;
        }
        return baseUrl + "/files/" + relativePath;
    }

    /**
     * 获取当前用户ID（简化实现）
     */
    private Long getCurrentUserId() {
        // 实际应从安全上下文获取
        return 0L;
    }
}
