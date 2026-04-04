package cn.aiedge.storage;

import cn.aiedge.storage.chunk.ChunkUploadController;
import cn.aiedge.storage.chunk.ChunkUploadInfo;
import cn.aiedge.storage.chunk.ChunkUploadService;
import cn.aiedge.storage.model.StorageFile;
import cn.aiedge.storage.permission.FilePermission;
import cn.aiedge.storage.permission.FilePermissionService;
import cn.aiedge.storage.preview.EnhancedPreviewService;
import cn.aiedge.storage.preview.FilePreviewService;
import cn.aiedge.storage.service.FileStorageService;
import cn.aiedge.storage.validator.FileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 文件管理模块单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@DisplayName("文件管理模块单元测试")
public class FileManagementTest {

    private FileStorageService fileStorageService;
    private FileValidator fileValidator;
    private FilePreviewService previewService;
    private EnhancedPreviewService enhancedPreviewService;
    private FilePermissionService permissionService;
    private ChunkUploadService chunkUploadService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileStorageService = mock(FileStorageService.class);
        fileValidator = mock(FileValidator.class);
        previewService = mock(FilePreviewService.class);
        enhancedPreviewService = mock(EnhancedPreviewService.class);
        permissionService = mock(FilePermissionService.class);
        chunkUploadService = mock(ChunkUploadService.class);
    }

    // ==================== 文件上传下载测试 ====================

    @Test
    @DisplayName("文件上传 - 成功")
    void testUploadSuccess() throws IOException {
        // 准备测试数据
        String fileName = "test.txt";
        String contentType = "text/plain";
        byte[] content = "Hello, World!".getBytes();
        InputStream inputStream = new ByteArrayInputStream(content);

        StorageFile expectedFile = StorageFile.builder()
                .fileId("1234567890")
                .originalName(fileName)
                .contentType(contentType)
                .fileSize((long) content.length)
                .build();

        when(fileStorageService.upload(any(), eq(fileName), eq(contentType), any(), any()))
                .thenReturn(expectedFile);

        // 执行测试
        StorageFile result = fileStorageService.upload(inputStream, fileName, contentType, null, null);

        // 验证结果
        assertNotNull(result);
        assertEquals(fileName, result.getOriginalName());
        assertEquals(contentType, result.getContentType());
        assertEquals(content.length, result.getFileSize());
    }

    @Test
    @DisplayName("文件下载 - 成功")
    void testDownloadSuccess() {
        // 准备测试数据
        String fileId = "1234567890";
        byte[] content = "Download content".getBytes();
        InputStream mockStream = new ByteArrayInputStream(content);

        StorageFile fileInfo = StorageFile.builder()
                .fileId(fileId)
                .originalName("test.txt")
                .contentType("text/plain")
                .fileSize((long) content.length)
                .build();

        when(fileStorageService.getFileInfo(fileId)).thenReturn(fileInfo);
        when(fileStorageService.download(fileId)).thenReturn(mockStream);

        // 执行测试
        InputStream result = fileStorageService.download(fileId);

        // 验证结果
        assertNotNull(result);
        assertNotNull(fileStorageService.getFileInfo(fileId));
    }

    @Test
    @DisplayName("文件下载 - 文件不存在")
    void testDownloadNotFound() {
        // 准备测试数据
        String fileId = "nonexistent";

        when(fileStorageService.getFileInfo(fileId)).thenReturn(null);

        // 执行测试
        StorageFile result = fileStorageService.getFileInfo(fileId);

        // 验证结果
        assertNull(result);
    }

    @Test
    @DisplayName("文件删除 - 成功")
    void testDeleteSuccess() {
        // 准备测试数据
        String fileId = "1234567890";

        when(fileStorageService.delete(fileId)).thenReturn(true);

        // 执行测试
        boolean result = fileStorageService.delete(fileId);

        // 验证结果
        assertTrue(result);
    }

    @Test
    @DisplayName("批量删除文件 - 成功")
    void testBatchDeleteSuccess() {
        // 准备测试数据
        List<String> fileIds = Arrays.asList("1", "2", "3");

        when(fileStorageService.deleteBatch(fileIds)).thenReturn(3);

        // 执行测试
        int result = fileStorageService.deleteBatch(fileIds);

        // 验证结果
        assertEquals(3, result);
    }

    // ==================== 文件预览测试 ====================

    @Test
    @DisplayName("图片预览 - 成功")
    void testImagePreview() {
        // 准备测试数据
        StorageFile imageFile = StorageFile.builder()
                .fileId("img123")
                .originalName("test.jpg")
                .extension("jpg")
                .contentType("image/jpeg")
                .accessUrl("http://example.com/test.jpg")
                .build();

        FilePreviewService.PreviewInfo expectedPreview = FilePreviewService.PreviewInfo.builder()
                .previewable(true)
                .previewType("image")
                .previewUrl("http://example.com/test.jpg")
                .build();

        when(previewService.getPreviewInfo(imageFile)).thenReturn(expectedPreview);

        // 执行测试
        FilePreviewService.PreviewInfo result = previewService.getPreviewInfo(imageFile);

        // 验证结果
        assertTrue(result.isPreviewable());
        assertEquals("image", result.getPreviewType());
    }

    @Test
    @DisplayName("PDF预览 - 成功")
    void testPdfPreview() {
        // 准备测试数据
        StorageFile pdfFile = StorageFile.builder()
                .fileId("pdf123")
                .originalName("document.pdf")
                .extension("pdf")
                .contentType("application/pdf")
                .accessUrl("http://example.com/document.pdf")
                .build();

        FilePreviewService.PreviewInfo expectedPreview = FilePreviewService.PreviewInfo.builder()
                .previewable(true)
                .previewType("pdf")
                .previewUrl("http://example.com/document.pdf")
                .build();

        when(previewService.getPreviewInfo(pdfFile)).thenReturn(expectedPreview);

        // 执行测试
        FilePreviewService.PreviewInfo result = previewService.getPreviewInfo(pdfFile);

        // 验证结果
        assertTrue(result.isPreviewable());
        assertEquals("pdf", result.getPreviewType());
    }

    @Test
    @DisplayName("缩略图生成 - 成功")
    void testThumbnailGeneration() throws IOException {
        // 准备测试数据
        byte[] imageData = createTestImageBytes();
        InputStream inputStream = new ByteArrayInputStream(imageData);

        when(enhancedPreviewService.generateThumbnail(any(), eq("jpg")))
                .thenReturn(imageData);

        // 执行测试
        byte[] result = enhancedPreviewService.generateThumbnail(inputStream, "jpg");

        // 验证结果
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    // ==================== 文件权限测试 ====================

    @Test
    @DisplayName("授权文件权限 - 成功")
    void testGrantPermission() {
        // 准备测试数据
        Long fileId = 1L;
        Long userId = 100L;

        FilePermission permission = new FilePermission();
        permission.setFileId(fileId);
        permission.setPermissionType(FilePermission.PermissionType.READ.getCode());
        permission.setPrincipalType(FilePermission.PrincipalType.USER.getCode());
        permission.setPrincipalId(userId);

        when(permissionService.grantPermission(
                eq(fileId),
                eq(FilePermission.PermissionType.READ),
                eq(FilePermission.PrincipalType.USER),
                eq(userId),
                any(),
                any()
        )).thenReturn(permission);

        // 执行测试
        FilePermission result = permissionService.grantPermission(
                fileId, FilePermission.PermissionType.READ,
                FilePermission.PrincipalType.USER, userId, 1L, null);

        // 验证结果
        assertNotNull(result);
        assertEquals(fileId, result.getFileId());
    }

    @Test
    @DisplayName("检查权限 - 有权限")
    void testHasPermission() {
        // 准备测试数据
        Long fileId = 1L;
        Long userId = 100L;

        when(permissionService.hasPermission(
                eq(fileId), eq(userId), eq(FilePermission.PermissionType.READ), any(), any()
        )).thenReturn(true);

        // 执行测试
        boolean result = permissionService.hasPermission(
                fileId, userId, FilePermission.PermissionType.READ, null, null);

        // 验证结果
        assertTrue(result);
    }

    @Test
    @DisplayName("检查权限 - 无权限")
    void testNoPermission() {
        // 准备测试数据
        Long fileId = 1L;
        Long userId = 100L;

        when(permissionService.hasPermission(
                eq(fileId), eq(userId), eq(FilePermission.PermissionType.DELETE), any(), any()
        )).thenReturn(false);

        // 执行测试
        boolean result = permissionService.hasPermission(
                fileId, userId, FilePermission.PermissionType.DELETE, null, null);

        // 验证结果
        assertFalse(result);
    }

    @Test
    @DisplayName("检查所有者权限")
    void testOwnerPermission() {
        // 准备测试数据
        StorageFile file = StorageFile.builder()
                .fileId("123")
                .uploaderId(100L)
                .build();

        when(permissionService.isOwner(file, 100L)).thenReturn(true);
        when(permissionService.isOwner(file, 200L)).thenReturn(false);

        // 执行测试 & 验证结果
        assertTrue(permissionService.isOwner(file, 100L));
        assertFalse(permissionService.isOwner(file, 200L));
    }

    // ==================== 分片上传测试 ====================

    @Test
    @DisplayName("初始化分片上传 - 成功")
    void testInitChunkUpload() {
        // 准备测试数据
        String fileName = "large_file.zip";
        Long fileSize = 100L * 1024 * 1024; // 100MB
        Integer chunkSize = 5 * 1024 * 1024; // 5MB

        ChunkUploadInfo expectedInfo = new ChunkUploadInfo();
        expectedInfo.setUploadId("test-upload-id");
        expectedInfo.setFileName(fileName);
        expectedInfo.setFileSize(fileSize);
        expectedInfo.setTotalChunks(20);

        when(chunkUploadService.initUpload(fileName, fileSize, null, chunkSize, null, null))
                .thenReturn(expectedInfo);

        // 执行测试
        ChunkUploadInfo result = chunkUploadService.initUpload(fileName, fileSize, null, chunkSize, null, null);

        // 验证结果
        assertNotNull(result);
        assertEquals(fileName, result.getFileName());
        assertEquals(20, result.getTotalChunks());
    }

    @Test
    @DisplayName("上传分片 - 成功")
    void testUploadChunk() throws IOException {
        // 准备测试数据
        String uploadId = "test-upload-id";
        Integer chunkNumber = 1;
        byte[] chunkData = "chunk data".getBytes();
        InputStream chunkStream = new ByteArrayInputStream(chunkData);

        ChunkUploadInfo expectedInfo = new ChunkUploadInfo();
        expectedInfo.setUploadId(uploadId);
        expectedInfo.setUploadedChunks(1);

        when(chunkUploadService.uploadChunk(uploadId, chunkNumber, null, chunkStream))
                .thenReturn(expectedInfo);

        // 执行测试
        ChunkUploadInfo result = chunkUploadService.uploadChunk(uploadId, chunkNumber, null, chunkStream);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getUploadedChunks());
    }

    @Test
    @DisplayName("合并分片 - 成功")
    void testMergeChunks() {
        // 准备测试数据
        String uploadId = "test-upload-id";

        StorageFile expectedFile = StorageFile.builder()
                .fileId("merged-file-id")
                .originalName("large_file.zip")
                .build();

        when(chunkUploadService.mergeChunks(uploadId)).thenReturn(expectedFile);

        // 执行测试
        StorageFile result = chunkUploadService.mergeChunks(uploadId);

        // 验证结果
        assertNotNull(result);
        assertEquals("large_file.zip", result.getOriginalName());
    }

    // ==================== 文件校验测试 ====================

    @Test
    @DisplayName("文件校验 - 有效文件")
    void testValidFile() {
        // 准备测试数据
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello".getBytes());

        FileValidator.ValidationResult validResult = FileValidator.ValidationResult.success();
        when(fileValidator.validate(file)).thenReturn(validResult);

        // 执行测试
        FileValidator.ValidationResult result = fileValidator.validate(file);

        // 验证结果
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("文件校验 - 无效扩展名")
    void testInvalidExtension() {
        // 准备测试数据
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.exe", "application/octet-stream", "content".getBytes());

        FileValidator.ValidationResult invalidResult = 
                FileValidator.ValidationResult.failure("不允许上传此类型的文件: exe");
        when(fileValidator.validate(file)).thenReturn(invalidResult);

        // 执行测试
        FileValidator.ValidationResult result = fileValidator.validate(file);

        // 验证结果
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("不允许"));
    }

    // ==================== 辅助方法 ====================

    private byte[] createTestImageBytes() throws IOException {
        // 创建一个简单的测试图片
        java.awt.image.BufferedImage image = 
                new java.awt.image.BufferedImage(100, 100, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = image.createGraphics();
        g2d.setColor(java.awt.Color.RED);
        g2d.fillRect(0, 0, 100, 100);
        g2d.dispose();

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }
}
