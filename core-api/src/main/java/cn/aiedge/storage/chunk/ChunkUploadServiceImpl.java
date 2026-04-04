package cn.aiedge.storage.chunk;

import cn.aiedge.storage.model.StorageFile;
import cn.aiedge.storage.service.FileStorageService;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分片上传服务实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkUploadServiceImpl implements ChunkUploadService {

    private static final int DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int UPLOAD_EXPIRE_HOURS = 24; // 24小时过期

    // 使用内存存储上传信息（生产环境应使用Redis）
    private final Map<String, ChunkUploadInfo> uploadCache = new ConcurrentHashMap<>();

    private final FileStorageService fileStorageService;
    private final ChunkStorageProperties properties;

    @Override
    public ChunkUploadInfo initUpload(String fileName, Long fileSize, String fileMd5,
                                        Integer chunkSize, String bizType, String bizId) {
        // 生成上传ID
        String uploadId = UUID.randomUUID().toString().replace("-", "");

        // 计算分片数
        int actualChunkSize = chunkSize != null ? chunkSize : DEFAULT_CHUNK_SIZE;
        int totalChunks = (int) Math.ceil((double) fileSize / actualChunkSize);

        // 创建上传信息
        ChunkUploadInfo uploadInfo = new ChunkUploadInfo();
        uploadInfo.setUploadId(uploadId);
        uploadInfo.setFileName(fileName);
        uploadInfo.setFileSize(fileSize);
        uploadInfo.setFileMd5(fileMd5);
        uploadInfo.setChunkSize(actualChunkSize);
        uploadInfo.setTotalChunks(totalChunks);
        uploadInfo.setUploadedChunks(0);
        uploadInfo.setUploadedBytes(0L);
        uploadInfo.setStatus("uploading");
        uploadInfo.setBizType(bizType);
        uploadInfo.setBizId(bizId);
        uploadInfo.setCreateTime(LocalDateTime.now());
        uploadInfo.setUpdateTime(LocalDateTime.now());
        uploadInfo.setExpireTime(LocalDateTime.now().plusHours(UPLOAD_EXPIRE_HOURS));
        uploadInfo.setChunks(new HashMap<>());

        // 初始化分片信息
        for (int i = 1; i <= totalChunks; i++) {
            ChunkUploadInfo.ChunkInfo chunkInfo = new ChunkUploadInfo.ChunkInfo();
            chunkInfo.setChunkNumber(i);
            chunkInfo.setChunkSize((long) Math.min(actualChunkSize, 
                    fileSize - (long) (i - 1) * actualChunkSize));
            chunkInfo.setUploaded(false);
            uploadInfo.getChunks().put(i, chunkInfo);
        }

        // 创建临时存储目录
        Path tempDir = getTempDir(uploadId);
        try {
            Files.createDirectories(tempDir);
        } catch (IOException e) {
            throw new RuntimeException("创建临时目录失败: " + e.getMessage(), e);
        }

        // 缓存上传信息
        uploadCache.put(uploadId, uploadInfo);

        log.info("Chunk upload initialized: uploadId={}, fileName={}, totalChunks={}",
                uploadId, fileName, totalChunks);

        return uploadInfo;
    }

    @Override
    public ChunkUploadInfo uploadChunk(String uploadId, Integer chunkNumber, String chunkMd5,
                                         InputStream inputStream) {
        ChunkUploadInfo uploadInfo = getUploadInfo(uploadId);
        if (uploadInfo == null) {
            throw new RuntimeException("上传任务不存在: " + uploadId);
        }

        if (uploadInfo.isCompleted()) {
            throw new RuntimeException("上传任务已完成: " + uploadId);
        }

        // 检查分片序号
        if (chunkNumber < 1 || chunkNumber > uploadInfo.getTotalChunks()) {
            throw new RuntimeException("无效的分片序号: " + chunkNumber);
        }

        // 检查分片是否已上传
        ChunkUploadInfo.ChunkInfo chunkInfo = uploadInfo.getChunks().get(chunkNumber);
        if (Boolean.TRUE.equals(chunkInfo.getUploaded())) {
            log.info("Chunk already uploaded: uploadId={}, chunkNumber={}", uploadId, chunkNumber);
            return uploadInfo;
        }

        try {
            // 保存分片
            Path chunkPath = getChunkPath(uploadId, chunkNumber);
            long bytesWritten = Files.copy(inputStream, chunkPath);

            // 验证分片MD5
            String actualMd5 = DigestUtil.md5Hex(chunkPath.toFile());
            if (StrUtil.isNotBlank(chunkMd5) && !chunkMd5.equalsIgnoreCase(actualMd5)) {
                Files.deleteIfExists(chunkPath);
                throw new RuntimeException("分片MD5校验失败");
            }

            // 更新分片信息
            chunkInfo.setChunkMd5(actualMd5);
            chunkInfo.setUploaded(true);
            chunkInfo.setUploadTime(LocalDateTime.now());
            chunkInfo.setStoragePath(chunkPath.toString());

            // 更新上传信息
            uploadInfo.setUploadedChunks(uploadInfo.getUploadedChunks() + 1);
            uploadInfo.setUploadedBytes(uploadInfo.getUploadedBytes() + bytesWritten);
            uploadInfo.setUpdateTime(LocalDateTime.now());

            log.info("Chunk uploaded: uploadId={}, chunkNumber={}, progress={}%",
                    uploadId, chunkNumber, uploadInfo.getProgress());

            return uploadInfo;

        } catch (IOException e) {
            log.error("Failed to upload chunk: uploadId={}, chunkNumber={}", uploadId, chunkNumber, e);
            throw new RuntimeException("分片上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public StorageFile mergeChunks(String uploadId) {
        ChunkUploadInfo uploadInfo = getUploadInfo(uploadId);
        if (uploadInfo == null) {
            throw new RuntimeException("上传任务不存在: " + uploadId);
        }

        // 检查是否所有分片都已上传
        if (!uploadInfo.isCompleted()) {
            throw new RuntimeException("尚有分片未上传完成");
        }

        try {
            // 创建临时合并文件
            Path mergedFile = getTempDir(uploadId).resolve(uploadInfo.getFileName());
            
            try (OutputStream os = Files.newOutputStream(mergedFile)) {
                // 按顺序合并分片
                for (int i = 1; i <= uploadInfo.getTotalChunks(); i++) {
                    ChunkUploadInfo.ChunkInfo chunkInfo = uploadInfo.getChunks().get(i);
                    Path chunkPath = Paths.get(chunkInfo.getStoragePath());
                    Files.copy(chunkPath, os);
                }
            }

            // 验证合并后的文件MD5
            String actualMd5 = DigestUtil.md5Hex(mergedFile.toFile());
            if (StrUtil.isNotBlank(uploadInfo.getFileMd5()) && 
                !uploadInfo.getFileMd5().equalsIgnoreCase(actualMd5)) {
                throw new RuntimeException("文件MD5校验失败");
            }

            // 上传到存储服务
            try (InputStream is = Files.newInputStream(mergedFile)) {
                StorageFile storageFile = fileStorageService.upload(
                        is,
                        uploadInfo.getFileName(),
                        Files.probeContentType(mergedFile),
                        uploadInfo.getBizType(),
                        uploadInfo.getBizId()
                );

                // 更新上传信息
                uploadInfo.setStatus("completed");
                uploadInfo.setUpdateTime(LocalDateTime.now());

                log.info("Chunks merged: uploadId={}, fileId={}", uploadId, storageFile.getFileId());

                return storageFile;
            }

        } catch (IOException e) {
            log.error("Failed to merge chunks: uploadId={}", uploadId, e);
            throw new RuntimeException("分片合并失败: " + e.getMessage(), e);
        } finally {
            // 清理临时文件
            cleanupTempFiles(uploadId);
        }
    }

    @Override
    public ChunkUploadInfo getUploadInfo(String uploadId) {
        return uploadCache.get(uploadId);
    }

    @Override
    public boolean abortUpload(String uploadId) {
        ChunkUploadInfo uploadInfo = uploadCache.remove(uploadId);
        if (uploadInfo != null) {
            cleanupTempFiles(uploadId);
            log.info("Upload aborted: uploadId={}", uploadId);
            return true;
        }
        return false;
    }

    @Override
    public List<ChunkUploadInfo> getPendingUploads(String bizType, String bizId) {
        return uploadCache.values().stream()
                .filter(info -> "uploading".equals(info.getStatus()))
                .filter(info -> bizType == null || bizType.equals(info.getBizType()))
                .filter(info -> bizId == null || bizId.equals(info.getBizId()))
                .toList();
    }

    @Override
    public StorageFile checkFileExists(String fileMd5) {
        // 实际应从数据库查询相同MD5的文件
        // 这里简化实现
        return null;
    }

    @Override
    public int cleanupExpiredUploads() {
        LocalDateTime now = LocalDateTime.now();
        int count = 0;

        Iterator<Map.Entry<String, ChunkUploadInfo>> iterator = uploadCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ChunkUploadInfo> entry = iterator.next();
            ChunkUploadInfo info = entry.getValue();

            if (info.getExpireTime() != null && now.isAfter(info.getExpireTime())) {
                iterator.remove();
                cleanupTempFiles(info.getUploadId());
                count++;
            }
        }

        log.info("Cleaned up {} expired uploads", count);
        return count;
    }

    /**
     * 获取临时目录路径
     */
    private Path getTempDir(String uploadId) {
        String basePath = properties != null ? properties.getTempPath() : System.getProperty("java.io.tmpdir");
        return Paths.get(basePath, "chunks", uploadId);
    }

    /**
     * 获取分片文件路径
     */
    private Path getChunkPath(String uploadId, Integer chunkNumber) {
        return getTempDir(uploadId).resolve(String.format("chunk_%d.part", chunkNumber));
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFiles(String uploadId) {
        try {
            Path tempDir = getTempDir(uploadId);
            if (Files.exists(tempDir)) {
                FileUtil.del(tempDir.toFile());
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup temp files: uploadId={}", uploadId, e);
        }
    }
}
