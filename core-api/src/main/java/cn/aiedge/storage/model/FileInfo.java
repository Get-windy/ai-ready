package cn.aiedge.storage.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件元数据实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@TableName("sys_file")
public class FileInfo {

    /**
     * 文件ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 文件名称（原始文件名）
     */
    private String fileName;

    /**
     * 存储文件名（唯一标识）
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
     * 文件类型（MIME类型）
     */
    private String fileType;

    /**
     * 文件扩展名
     */
    private String fileExtension;

    /**
     * 存储类型：LOCAL, OSS, MINIO, S3
     */
    private String storageType;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 文件MD5值
     */
    private String fileMd5;

    /**
     * 下载URL（临时或永久）
     */
    private String downloadUrl;

    /**
     * 缩略图URL
     */
    private String thumbnailUrl;

    /**
     * 业务类型（product, customer, order等）
     */
    private String bizType;

    /**
     * 业务ID
     */
    private Long bizId;

    /**
     * 上传者ID
     */
    private Long uploadUserId;

    /**
     * 上传者名称
     */
    private String uploadUserName;

    /**
     * 访问权限：PUBLIC, PRIVATE, PROTECTED
     */
    private String accessLevel;

    /**
     * 下载次数
     */
    private Integer downloadCount;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 备注信息
     */
    private String remark;
}
