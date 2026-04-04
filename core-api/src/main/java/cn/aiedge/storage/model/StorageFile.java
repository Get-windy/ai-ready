package cn.aiedge.storage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 存储文件信息
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageFile {

    /**
     * 文件ID（唯一标识）
     */
    private String fileId;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 存储文件名
     */
    private String storageName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件扩展名
     */
    private String extension;

    /**
     * MIME类型
     */
    private String contentType;

    /**
     * MD5哈希值
     */
    private String md5Hash;

    /**
     * 存储类型（local/oss/s3）
     */
    private String storageType;

    /**
     * 访问URL
     */
    private String accessUrl;

    /**
     * 预览URL
     */
    private String previewUrl;

    /**
     * 上传者ID
     */
    private Long uploaderId;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 业务ID
     */
    private String bizId;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 过期时间（可选）
     */
    private LocalDateTime expireTime;

    /**
     * 获取文件类型类别
     */
    public FileTypeCategory getCategory() {
        return FileTypeCategory.fromExtension(extension);
    }

    /**
     * 是否支持预览
     */
    public boolean isPreviewable() {
        return getCategory().isPreviewable();
    }
}
