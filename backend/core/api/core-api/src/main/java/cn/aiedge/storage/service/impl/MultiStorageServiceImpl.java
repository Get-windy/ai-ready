package cn.aiedge.storage.service.impl;

import cn.aiedge.storage.config.StorageProperties;
import cn.aiedge.storage.model.FileInfo;
import cn.aiedge.storage.model.StorageFile;
import cn.aiedge.storage.service.FileStorageService;
import cn.aiedge.storage.service.StorageStrategy;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件存储服务实现
 * 支持多存储后端切换
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiStorageServiceImpl implements FileStorageService {

    private final Map<String, StorageStrategy> strategyMap = new ConcurrentHashMap<>();
    private final StorageProperties properties;
    private StorageStrategy defaultStrategy;

    /**
     * 注册存储策略
     */
    public void registerStrategy(StorageStrategy strategy) {
        strategyMap.put(strategy.getType(), strategy);
        log.info("注册存储策略: type={}", strategy.getType());
    }

    /**
     * 设置默认存储策略
     */
    public void setDefaultStrategy(String type) {
        StorageStrategy strategy = strategyMap.get(type);
        if (strategy != null) {
            this.defaultStrategy = strategy;
            log.info("设置默认存储策略: type={}", type);
        }
    }

    private StorageStrategy getStrategy() {
        if (defaultStrategy == null) {
            defaultStrategy = strategyMap.get(properties.getType());
        }
        if (defaultStrategy == null) {
            throw new IllegalStateException("未配置存储策略: " + properties.getType());
        }
        return defaultStrategy;
    }

    @Override
    public StorageFile upload(InputStream inputStream, String originalName, String contentType, 
                               String bizType, String bizId) {
        return upload(inputStream, originalName, contentType, null, bizType, bizId);
    }

    @Override
    @Transactional
    public StorageFile upload(InputStream inputStream, String originalName, String contentType,
                               String customPath, String bizType, String bizId) {
        try {
            // 生成文件ID
            String fileId = IdUtil.fastSimpleUUID();
            
            // 计算存储路径
            String filePath = customPath;
            if (StrUtil.isBlank(filePath)) {
                filePath = generateFilePath(originalName, bizType);
            }
            
            // 执行上传
            FileInfo fileInfo = getStrategy().upload(inputStream, filePath, originalName, contentType);
            
            // 构建存储文件记录
            StorageFile storageFile = StorageFile.builder()
                .fileId(fileId)
                .fileName(originalName)
                .filePath(fileInfo.getFilePath())
                .fileSize(fileInfo.getFileSize())
                .contentType(contentType)
                .fileExtension(FileUtil.extName(originalName))
                .bizType(bizType)
                .bizId(bizId)
                .storageType(getStrategy().getType())
                .etag(fileInfo.getEtag())
                .createTime(LocalDateTime.now())
                .build();
            
            log.info("文件上传成功: fileId={}, fileName={}, size={}", 
                fileId, originalName, fileInfo.getFileSize());
            
            return storageFile;
            
        } catch (Exception e) {
            log.error("文件上传失败: fileName={}", originalName, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String fileId) {
        try {
            StorageFile fileInfo = getFileInfo(fileId);
            if (fileInfo == null) {
                throw new IllegalArgumentException("文件不存在: " + fileId);
            }
            return getStrategy().download(fileInfo.getFilePath());
        } catch (Exception e) {
            log.error("文件下载失败: fileId={}", fileId, e);
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }
    }

    @Override
    public StorageFile getFileInfo(String fileId) {
        // TODO: 从数据库查询文件信息
        return null;
    }

    @Override
    public boolean delete(String fileId) {
        try {
            StorageFile fileInfo = getFileInfo(fileId);
            if (fileInfo == null) {
                return false;
            }
            
            boolean deleted = getStrategy().delete(fileInfo.getFilePath());
            if (deleted) {
                // TODO: 删除数据库记录
                log.info("文件删除成功: fileId={}", fileId);
            }
            return deleted;
        } catch (Exception e) {
            log.error("文件删除失败: fileId={}", fileId, e);
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
        return getStrategy().exists(fileInfo.getFilePath());
    }

    @Override
    public String getAccessUrl(String fileId) {
        StorageFile fileInfo = getFileInfo(fileId);
        if (fileInfo == null) {
            return null;
        }
        return getStrategy().getPermanentUrl(fileInfo.getFilePath());
    }

    @Override
    public String getPresignedUrl(String fileId, int expireSeconds) {
        StorageFile fileInfo = getFileInfo(fileId);
        if (fileInfo == null) {
            return null;
        }
        return getStrategy().getAccessUrl(fileInfo.getFilePath(), expireSeconds);
    }

    @Override
    public StorageFile copy(String fileId, String targetBizType, String targetBizId) {
        try {
            StorageFile source = getFileInfo(fileId);
            if (source == null) {
                throw new IllegalArgumentException("源文件不存在: " + fileId);
            }
            
            String targetPath = generateFilePath(source.getFileName(), targetBizType);
            getStrategy().copy(source.getFilePath(), targetPath);
            
            String newFileId = IdUtil.fastSimpleUUID();
            StorageFile newFile = StorageFile.builder()
                .fileId(newFileId)
                .fileName(source.getFileName())
                .filePath(targetPath)
                .fileSize(source.getFileSize())
                .contentType(source.getContentType())
                .fileExtension(source.getFileExtension())
                .bizType(targetBizType)
                .bizId(targetBizId)
                .storageType(getStrategy().getType())
                .createTime(LocalDateTime.now())
                .build();
            
            log.info("文件复制成功: sourceId={}, targetId={}", fileId, newFileId);
            return newFile;
            
        } catch (Exception e) {
            log.error("文件复制失败: fileId={}", fileId, e);
            throw new RuntimeException("文件复制失败: " + e.getMessage(), e);
        }
    }

    @Override
    public StorageFile move(String fileId, String targetBizType, String targetBizId) {
        try {
            StorageFile source = getFileInfo(fileId);
            if (source == null) {
                throw new IllegalArgumentException("源文件不存在: " + fileId);
            }
            
            String targetPath = generateFilePath(source.getFileName(), targetBizType);
            getStrategy().move(source.getFilePath(), targetPath);
            
            // 更新数据库记录
            source.setFilePath(targetPath);
            source.setBizType(targetBizType);
            source.setBizId(targetBizId);
            
            log.info("文件移动成功: fileId={}", fileId);
            return source;
            
        } catch (Exception e) {
            log.error("文件移动失败: fileId={}", fileId, e);
            throw new RuntimeException("文件移动失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getStorageType() {
        return getStrategy().getType();
    }

    @Override
    public String getStorageName() {
        return getStrategy().getType().toUpperCase() + " Storage";
    }

    /**
     * 生成文件存储路径
     */
    private String generateFilePath(String fileName, String bizType) {
        String ext = FileUtil.extName(fileName);
        String uuid = IdUtil.fastSimpleUUID();
        LocalDateTime now = LocalDateTime.now();
        
        String datePath = String.format("%d/%02d/%02d", 
            now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        
        String bizPath = StrUtil.isNotBlank(bizType) ? bizType + "/" : "";
        
        return bizPath + datePath + "/" + uuid + (StrUtil.isNotBlank(ext) ? "." + ext : "");
    }
}
