package cn.aiedge.storage.chunk;

import cn.aiedge.storage.model.StorageFile;
import cn.aiedge.storage.service.FileStorageService;

import java.io.InputStream;
import java.util.List;

/**
 * 分片上传服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface ChunkUploadService {

    /**
     * 初始化分片上传
     *
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param fileMd5 文件MD5
     * @param chunkSize 分片大小（字节）
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @return 上传信息
     */
    ChunkUploadInfo initUpload(String fileName, Long fileSize, String fileMd5,
                                Integer chunkSize, String bizType, String bizId);

    /**
     * 上传分片
     *
     * @param uploadId 上传ID
     * @param chunkNumber 分片序号（从1开始）
     * @param chunkMd5 分片MD5
     * @param inputStream 分片数据
     * @return 更新后的上传信息
     */
    ChunkUploadInfo uploadChunk(String uploadId, Integer chunkNumber, String chunkMd5,
                                 InputStream inputStream);

    /**
     * 合并分片
     *
     * @param uploadId 上传ID
     * @return 合并后的文件信息
     */
    StorageFile mergeChunks(String uploadId);

    /**
     * 获取上传信息
     *
     * @param uploadId 上传ID
     * @return 上传信息
     */
    ChunkUploadInfo getUploadInfo(String uploadId);

    /**
     * 取消上传
     *
     * @param uploadId 上传ID
     * @return 是否成功
     */
    boolean abortUpload(String uploadId);

    /**
     * 获取未完成的上传列表
     *
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @return 上传信息列表
     */
    List<ChunkUploadInfo> getPendingUploads(String bizType, String bizId);

    /**
     * 检查分片是否已上传（秒传支持）
     *
     * @param fileMd5 文件MD5
     * @return 已存在的文件信息，不存在返回null
     */
    StorageFile checkFileExists(String fileMd5);

    /**
     * 清理过期的分片上传
     *
     * @return 清理的数量
     */
    int cleanupExpiredUploads();
}
