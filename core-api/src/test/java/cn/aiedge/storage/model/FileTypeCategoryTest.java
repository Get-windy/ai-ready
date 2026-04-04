package cn.aiedge.storage.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileTypeCategory 单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
class FileTypeCategoryTest {

    @Test
    @DisplayName("根据扩展名获取类型 - 图片")
    void testFromExtension_Image() {
        assertEquals(FileTypeCategory.IMAGE, FileTypeCategory.fromExtension("jpg"));
        assertEquals(FileTypeCategory.IMAGE, FileTypeCategory.fromExtension("jpeg"));
        assertEquals(FileTypeCategory.IMAGE, FileTypeCategory.fromExtension("png"));
        assertEquals(FileTypeCategory.IMAGE, FileTypeCategory.fromExtension("gif"));
        assertEquals(FileTypeCategory.IMAGE, FileTypeCategory.fromExtension("webp"));
    }

    @Test
    @DisplayName("根据扩展名获取类型 - 文档")
    void testFromExtension_Document() {
        assertEquals(FileTypeCategory.DOCUMENT, FileTypeCategory.fromExtension("pdf"));
        assertEquals(FileTypeCategory.DOCUMENT, FileTypeCategory.fromExtension("doc"));
        assertEquals(FileTypeCategory.DOCUMENT, FileTypeCategory.fromExtension("docx"));
        assertEquals(FileTypeCategory.DOCUMENT, FileTypeCategory.fromExtension("xls"));
        assertEquals(FileTypeCategory.DOCUMENT, FileTypeCategory.fromExtension("xlsx"));
        assertEquals(FileTypeCategory.DOCUMENT, FileTypeCategory.fromExtension("ppt"));
        assertEquals(FileTypeCategory.DOCUMENT, FileTypeCategory.fromExtension("pptx"));
        assertEquals(FileTypeCategory.DOCUMENT, FileTypeCategory.fromExtension("txt"));
    }

    @Test
    @DisplayName("根据扩展名获取类型 - 视频")
    void testFromExtension_Video() {
        assertEquals(FileTypeCategory.VIDEO, FileTypeCategory.fromExtension("mp4"));
        assertEquals(FileTypeCategory.VIDEO, FileTypeCategory.fromExtension("avi"));
        assertEquals(FileTypeCategory.VIDEO, FileTypeCategory.fromExtension("mov"));
        assertEquals(FileTypeCategory.VIDEO, FileTypeCategory.fromExtension("mkv"));
    }

    @Test
    @DisplayName("根据扩展名获取类型 - 音频")
    void testFromExtension_Audio() {
        assertEquals(FileTypeCategory.AUDIO, FileTypeCategory.fromExtension("mp3"));
        assertEquals(FileTypeCategory.AUDIO, FileTypeCategory.fromExtension("wav"));
        assertEquals(FileTypeCategory.AUDIO, FileTypeCategory.fromExtension("flac"));
        assertEquals(FileTypeCategory.AUDIO, FileTypeCategory.fromExtension("aac"));
    }

    @Test
    @DisplayName("根据扩展名获取类型 - 压缩包")
    void testFromExtension_Archive() {
        assertEquals(FileTypeCategory.ARCHIVE, FileTypeCategory.fromExtension("zip"));
        assertEquals(FileTypeCategory.ARCHIVE, FileTypeCategory.fromExtension("rar"));
        assertEquals(FileTypeCategory.ARCHIVE, FileTypeCategory.fromExtension("7z"));
        assertEquals(FileTypeCategory.ARCHIVE, FileTypeCategory.fromExtension("tar"));
    }

    @Test
    @DisplayName("根据扩展名获取类型 - 代码")
    void testFromExtension_Code() {
        assertEquals(FileTypeCategory.CODE, FileTypeCategory.fromExtension("java"));
        assertEquals(FileTypeCategory.CODE, FileTypeCategory.fromExtension("py"));
        assertEquals(FileTypeCategory.CODE, FileTypeCategory.fromExtension("js"));
        assertEquals(FileTypeCategory.CODE, FileTypeCategory.fromExtension("ts"));
        assertEquals(FileTypeCategory.CODE, FileTypeCategory.fromExtension("json"));
        assertEquals(FileTypeCategory.CODE, FileTypeCategory.fromExtension("sql"));
    }

    @Test
    @DisplayName("根据扩展名获取类型 - 其他")
    void testFromExtension_Other() {
        assertEquals(FileTypeCategory.OTHER, FileTypeCategory.fromExtension("exe"));
        assertEquals(FileTypeCategory.OTHER, FileTypeCategory.fromExtension("bin"));
        assertEquals(FileTypeCategory.OTHER, FileTypeCategory.fromExtension("dat"));
    }

    @Test
    @DisplayName("根据扩展名获取类型 - 空值处理")
    void testFromExtension_EmptyOrNull() {
        assertEquals(FileTypeCategory.OTHER, FileTypeCategory.fromExtension(null));
        assertEquals(FileTypeCategory.OTHER, FileTypeCategory.fromExtension(""));
    }

    @Test
    @DisplayName("根据扩展名获取类型 - 大小写不敏感")
    void testFromExtension_CaseInsensitive() {
        assertEquals(FileTypeCategory.IMAGE, FileTypeCategory.fromExtension("JPG"));
        assertEquals(FileTypeCategory.IMAGE, FileTypeCategory.fromExtension("Png"));
        assertEquals(FileTypeCategory.DOCUMENT, FileTypeCategory.fromExtension("PDF"));
    }

    @Test
    @DisplayName("根据MIME类型获取类型 - 图片")
    void testFromContentType_Image() {
        assertEquals(FileTypeCategory.IMAGE, FileTypeCategory.fromContentType("image/jpeg"));
        assertEquals(FileTypeCategory.IMAGE, FileTypeCategory.fromContentType("image/png"));
        assertEquals(FileTypeCategory.IMAGE, FileTypeCategory.fromContentType("image/gif"));
    }

    @Test
    @DisplayName("根据MIME类型获取类型 - 视频")
    void testFromContentType_Video() {
        assertEquals(FileTypeCategory.VIDEO, FileTypeCategory.fromContentType("video/mp4"));
        assertEquals(FileTypeCategory.VIDEO, FileTypeCategory.fromContentType("video/quicktime"));
        assertEquals(FileTypeCategory.VIDEO, FileTypeCategory.fromContentType("video/x-msvideo"));
    }

    @Test
    @DisplayName("根据MIME类型获取类型 - 音频")
    void testFromContentType_Audio() {
        assertEquals(FileTypeCategory.AUDIO, FileTypeCategory.fromContentType("audio/mpeg"));
        assertEquals(FileTypeCategory.AUDIO, FileTypeCategory.fromContentType("audio/wav"));
        assertEquals(FileTypeCategory.AUDIO, FileTypeCategory.fromContentType("audio/flac"));
    }

    @Test
    @DisplayName("根据MIME类型获取类型 - 文档")
    void testFromContentType_Document() {
        assertEquals(FileTypeCategory.DOCUMENT, FileTypeCategory.fromContentType("application/pdf"));
        assertEquals(FileTypeCategory.DOCUMENT, FileTypeCategory.fromContentType("text/plain"));
        assertEquals(FileTypeCategory.DOCUMENT, FileTypeCategory.fromContentType("application/msword"));
    }

    @Test
    @DisplayName("根据MIME类型获取类型 - 空值处理")
    void testFromContentType_EmptyOrNull() {
        assertEquals(FileTypeCategory.OTHER, FileTypeCategory.fromContentType(null));
        assertEquals(FileTypeCategory.OTHER, FileTypeCategory.fromContentType(""));
    }

    @Test
    @DisplayName("获取所有允许的扩展名")
    void testGetAllAllowedExtensions() {
        Set<String> allExtensions = FileTypeCategory.getAllAllowedExtensions();

        assertTrue(allExtensions.contains("jpg"));
        assertTrue(allExtensions.contains("png"));
        assertTrue(allExtensions.contains("pdf"));
        assertTrue(allExtensions.contains("mp4"));
        assertTrue(allExtensions.contains("mp3"));
        assertTrue(allExtensions.contains("zip"));
        assertTrue(allExtensions.contains("java"));
    }

    @Test
    @DisplayName("检查扩展名是否允许")
    void testIsAllowedExtension() {
        Set<String> allowed = Set.of("jpg", "png", "pdf");

        assertTrue(FileTypeCategory.isAllowedExtension("jpg", allowed));
        assertTrue(FileTypeCategory.isAllowedExtension("png", allowed));
        assertTrue(FileTypeCategory.isAllowedExtension("JPG", allowed)); // 大小写不敏感
        assertFalse(FileTypeCategory.isAllowedExtension("exe", allowed));
        assertFalse(FileTypeCategory.isAllowedExtension(null, allowed));
    }

    @Test
    @DisplayName("检查是否支持预览")
    void testIsPreviewable() {
        assertTrue(FileTypeCategory.IMAGE.isPreviewable());
        assertTrue(FileTypeCategory.DOCUMENT.isPreviewable());
        assertTrue(FileTypeCategory.VIDEO.isPreviewable());
        assertTrue(FileTypeCategory.AUDIO.isPreviewable());
        assertTrue(FileTypeCategory.CODE.isPreviewable());
        assertFalse(FileTypeCategory.ARCHIVE.isPreviewable());
        assertFalse(FileTypeCategory.OTHER.isPreviewable());
    }

    @Test
    @DisplayName("获取显示名称")
    void testGetDisplayName() {
        assertEquals("图片", FileTypeCategory.IMAGE.getDisplayName());
        assertEquals("文档", FileTypeCategory.DOCUMENT.getDisplayName());
        assertEquals("视频", FileTypeCategory.VIDEO.getDisplayName());
        assertEquals("音频", FileTypeCategory.AUDIO.getDisplayName());
        assertEquals("压缩包", FileTypeCategory.ARCHIVE.getDisplayName());
        assertEquals("代码", FileTypeCategory.CODE.getDisplayName());
        assertEquals("其他", FileTypeCategory.OTHER.getDisplayName());
    }

    @Test
    @DisplayName("获取类别代码")
    void testGetCategoryCode() {
        assertEquals("image", FileTypeCategory.IMAGE.getCategoryCode());
        assertEquals("document", FileTypeCategory.DOCUMENT.getCategoryCode());
        assertEquals("video", FileTypeCategory.VIDEO.getCategoryCode());
        assertEquals("audio", FileTypeCategory.AUDIO.getCategoryCode());
        assertEquals("archive", FileTypeCategory.ARCHIVE.getCategoryCode());
        assertEquals("code", FileTypeCategory.CODE.getCategoryCode());
        assertEquals("other", FileTypeCategory.OTHER.getCategoryCode());
    }
}
