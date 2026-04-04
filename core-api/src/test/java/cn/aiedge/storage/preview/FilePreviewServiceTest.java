package cn.aiedge.storage.preview;

import cn.aiedge.storage.model.FileTypeCategory;
import cn.aiedge.storage.model.StorageFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FilePreviewService 单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
class FilePreviewServiceTest {

    private FilePreviewService filePreviewService;

    @BeforeEach
    void setUp() {
        filePreviewService = new FilePreviewService();
    }

    @Test
    @DisplayName("获取预览信息 - 图片")
    void testGetPreviewInfo_Image() {
        // Given
        StorageFile file = StorageFile.builder()
                .fileId("test-123")
                .originalName("test.jpg")
                .extension("jpg")
                .contentType("image/jpeg")
                .accessUrl("http://example.com/test.jpg")
                .build();

        // When
        FilePreviewService.PreviewInfo info = filePreviewService.getPreviewInfo(file);

        // Then
        assertTrue(info.isPreviewable());
        assertEquals("image", info.getPreviewType());
        assertEquals("http://example.com/test.jpg", info.getPreviewUrl());
    }

    @Test
    @DisplayName("获取预览信息 - PNG图片")
    void testGetPreviewInfo_Png() {
        // Given
        StorageFile file = StorageFile.builder()
                .fileId("test-456")
                .originalName("test.png")
                .extension("png")
                .contentType("image/png")
                .accessUrl("http://example.com/test.png")
                .build();

        // When
        FilePreviewService.PreviewInfo info = filePreviewService.getPreviewInfo(file);

        // Then
        assertTrue(info.isPreviewable());
        assertEquals("image", info.getPreviewType());
    }

    @Test
    @DisplayName("获取预览信息 - PDF文档")
    void testGetPreviewInfo_Pdf() {
        // Given
        StorageFile file = StorageFile.builder()
                .fileId("test-789")
                .originalName("document.pdf")
                .extension("pdf")
                .contentType("application/pdf")
                .accessUrl("http://example.com/document.pdf")
                .build();

        // When
        FilePreviewService.PreviewInfo info = filePreviewService.getPreviewInfo(file);

        // Then
        assertTrue(info.isPreviewable());
        assertEquals("pdf", info.getPreviewType());
    }

    @Test
    @DisplayName("获取预览信息 - Word文档")
    void testGetPreviewInfo_Word() {
        // Given
        StorageFile file = StorageFile.builder()
                .fileId("test-word")
                .originalName("document.docx")
                .extension("docx")
                .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .accessUrl("http://example.com/document.docx")
                .build();

        // When
        FilePreviewService.PreviewInfo info = filePreviewService.getPreviewInfo(file);

        // Then
        assertTrue(info.isPreviewable());
        assertEquals("office", info.getPreviewType());
    }

    @Test
    @DisplayName("获取预览信息 - 视频")
    void testGetPreviewInfo_Video() {
        // Given
        StorageFile file = StorageFile.builder()
                .fileId("test-video")
                .originalName("video.mp4")
                .extension("mp4")
                .contentType("video/mp4")
                .accessUrl("http://example.com/video.mp4")
                .build();

        // When
        FilePreviewService.PreviewInfo info = filePreviewService.getPreviewInfo(file);

        // Then
        assertTrue(info.isPreviewable());
        assertEquals("video", info.getPreviewType());
    }

    @Test
    @DisplayName("获取预览信息 - 音频")
    void testGetPreviewInfo_Audio() {
        // Given
        StorageFile file = StorageFile.builder()
                .fileId("test-audio")
                .originalName("audio.mp3")
                .extension("mp3")
                .contentType("audio/mpeg")
                .accessUrl("http://example.com/audio.mp3")
                .build();

        // When
        FilePreviewService.PreviewInfo info = filePreviewService.getPreviewInfo(file);

        // Then
        assertTrue(info.isPreviewable());
        assertEquals("audio", info.getPreviewType());
    }

    @Test
    @DisplayName("获取预览信息 - 代码文件")
    void testGetPreviewInfo_Code() {
        // Given
        StorageFile file = StorageFile.builder()
                .fileId("test-code")
                .originalName("Main.java")
                .extension("java")
                .contentType("text/plain")
                .accessUrl("http://example.com/Main.java")
                .build();

        // When
        FilePreviewService.PreviewInfo info = filePreviewService.getPreviewInfo(file);

        // Then
        assertTrue(info.isPreviewable());
        assertEquals("code", info.getPreviewType());
    }

    @Test
    @DisplayName("获取预览信息 - 不支持的类型")
    void testGetPreviewInfo_NotSupported() {
        // Given
        StorageFile file = StorageFile.builder()
                .fileId("test-exe")
                .originalName("program.exe")
                .extension("exe")
                .contentType("application/octet-stream")
                .accessUrl("http://example.com/program.exe")
                .build();

        // When
        FilePreviewService.PreviewInfo info = filePreviewService.getPreviewInfo(file);

        // Then
        assertFalse(info.isPreviewable());
        assertEquals("none", info.getPreviewType());
    }

    @Test
    @DisplayName("获取预览信息 - 压缩包")
    void testGetPreviewInfo_Archive() {
        // Given
        StorageFile file = StorageFile.builder()
                .fileId("test-zip")
                .originalName("archive.zip")
                .extension("zip")
                .contentType("application/zip")
                .accessUrl("http://example.com/archive.zip")
                .build();

        // When
        FilePreviewService.PreviewInfo info = filePreviewService.getPreviewInfo(file);

        // Then
        assertFalse(info.isPreviewable());
    }

    @Test
    @DisplayName("获取预览信息 - null文件")
    void testGetPreviewInfo_NullFile() {
        // When
        FilePreviewService.PreviewInfo info = filePreviewService.getPreviewInfo(null);

        // Then
        assertFalse(info.isPreviewable());
    }
}
