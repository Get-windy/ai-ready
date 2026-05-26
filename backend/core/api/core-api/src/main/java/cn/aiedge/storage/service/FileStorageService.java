package cn.aiedge.storage.service;

import cn.aiedge.storage.model.StorageFile;

import java.io.InputStream;
import java.util.List;

/**
 * 文件存储服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface FileStorageService {

    /**
     * 上传文件
     *
     * @param inputStream 文件流
     * @param originalName 原始文件名
     * @param contentType 文件类型
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @return 存储文件信息
     */
    StorageFile upload(InputStream inputStream, String originalName, String contentType, 
                       String bizType, String bizId);

    /**
     * 上传文件（指定存储路径）
     *
     * @param inputStream 文件流
     * @param originalName 原始文件名
     * @param contentType 文件类型
     * @param customPath 自定义路径
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @return 存储文件信息
     */
    StorageFile upload(InputStream inputStream, String originalName, String contentType, 
                       String customPath, String bizType, String bizId);

    /**
     * 下载文件
     *
     * @param fileId 文件ID
     * @return 文件流
     */
    InputStream download(String fileId);

    /**
     * 获取文件信息
     *
     * @param fileId 文件ID
     * @return 存储文件信息
     */
    StorageFile getFileInfo(String fileId);

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     * @return 是否删除成功
     */
    boolean delete(String fileId);

    /**
     * 批量删除文件
     *
     * @param fileIds 文件ID列表
     * @return 删除成功的数量
     */
    int deleteBatch(List<String> fileIds);

    /**
     * 检查文件是否存在
     *
     * @param fileId 文件ID
     * @return 是否存在
     */
    boolean exists(String fileId);

    /**
     * 获取文件访问URL
     *
     * @param fileId 文件ID
     * @return 访问URL
     */
    String getAccessUrl(String fileId);

    /**
     * 获取临时访问URL（带签名，有时效）
     *
     * @param fileId 文件ID
     * @param expireSeconds 过期时间（秒）
     * @return 临时访问URL
     */
    String getPresignedUrl(String fileId, int expireSeconds);

    /**
     * 复制文件
     *
     * @param fileId 源文件ID
     * @param targetBizType 目标业务类型
     * @param targetBizId 目标业务ID
     * @return 新文件信息
     */
    StorageFile copy(String fileId, String targetBizType, String targetBizId);

    /**
     * 移动文件
     *
     * @param fileId 源文件ID
     * @param targetBizType 目标业务类型
     * @param targetBizId 目标业务ID
     * @return 更新后的文件信息
     */
    StorageFile move(String fileId, String targetBizType, String targetBizId);

    /**
     * 获取存储类型
     *
     * @return 存储类型标识
     */
    String getStorageType();

    /**
     * 获取存储名称
     *
     * @return 存储名称
     */
    String getStorageName();
}
