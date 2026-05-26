package cn.aiedge.storage.chunk;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分片上传信息
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class ChunkUploadInfo {

    /**
     * 上传ID（唯一标识一次分片上传）
     */
    private String uploadId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件总大小
     */
    private Long fileSize;

    /**
     * 文件MD5（完整文件）
     */
    private String fileMd5;

    /**
     * 分片大小
     */
    private Integer chunkSize;

    /**
     * 总分片数
     */
    private Integer totalChunks;

    /**
     * 已上传分片数
     */
    private Integer uploadedChunks;

    /**
     * 已上传字节数
     */
    private Long uploadedBytes;

    /**
     * 上传状态：uploading, completed, aborted
     */
    private String status;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 业务ID
     */
    private String bizId;

    /**
     * 上传者ID
     */
    private Long uploaderId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 分片信息
     */
    private java.util.Map<Integer, ChunkInfo> chunks;

    /**
     * 计算上传进度
     */
    public double getProgress() {
        if (fileSize == null || fileSize == 0) {
            return 0;
        }
        return (double) uploadedBytes / fileSize * 100;
    }

    /**
     * 是否上传完成
     */
    public boolean isCompleted() {
        return "completed".equals(status) || 
               (uploadedChunks != null && totalChunks != null && uploadedChunks.equals(totalChunks));
    }

    /**
     * 分片信息
     */
    @Data
    public static class ChunkInfo {
        /**
         * 分片序号（从1开始）
         */
        private Integer chunkNumber;

        /**
         * 分片大小
         */
        private Long chunkSize;

        /**
         * 分片MD5
         */
        private String chunkMd5;

        /**
         * 是否已上传
         */
        private Boolean uploaded;

        /**
         * 上传时间
         */
        private LocalDateTime uploadTime;

        /**
         * 分片存储路径
         */
        private String storagePath;
    }
}
