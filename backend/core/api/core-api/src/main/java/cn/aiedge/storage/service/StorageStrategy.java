package cn.aiedge.storage.service;

import cn.aiedge.storage.model.FileInfo;

import java.io.InputStream;
import java.util.List;

/**
 * 文件存储策略接口
 * 支持多种存储后端实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface StorageStrategy {

    /**
     * 获取存储类型
     *
     * @return 存储类型标识
     */
    String getType();

    /**
     * 上传文件
     *
     * @param inputStream 文件输入流
     * @param fileName    文件名
     * @param contentType 文件类型
     * @return 文件信息
     */
    FileInfo upload(InputStream inputStream, String fileName, String contentType) throws Exception;

    /**
     * 上传文件（指定路径）
     *
     * @param inputStream 文件输入流
     * @param filePath    文件路径
     * @param fileName    文件名
     * @param contentType 文件类型
     * @return 文件信息
     */
    FileInfo upload(InputStream inputStream, String filePath, String fileName, String contentType) throws Exception;

    /**
     * 下载文件
     *
     * @param filePath 文件路径
     * @return 文件输入流
     */
    InputStream download(String filePath) throws Exception;

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    boolean delete(String filePath) throws Exception;

    /**
     * 批量删除文件
     *
     * @param filePaths 文件路径列表
     * @return 删除成功数量
     */
    int deleteBatch(List<String> filePaths) throws Exception;

    /**
     * 获取文件访问URL
     *
     * @param filePath 文件路径
     * @param expireSeconds 过期时间（秒）
     * @return 访问URL
     */
    String getAccessUrl(String filePath, int expireSeconds);

    /**
     * 获取永久访问URL
     *
     * @param filePath 文件路径
     * @return 访问URL
     */
    String getPermanentUrl(String filePath);

    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     * @return 是否存在
     */
    boolean exists(String filePath);

    /**
     * 获取文件大小
     *
     * @param filePath 文件路径
     * @return 文件大小（字节）
     */
    long getFileSize(String filePath);

    /**
     * 复制文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return 是否复制成功
     */
    boolean copy(String sourcePath, String targetPath) throws Exception;

    /**
     * 移动文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return 是否移动成功
     */
    boolean move(String sourcePath, String targetPath) throws Exception;
}
