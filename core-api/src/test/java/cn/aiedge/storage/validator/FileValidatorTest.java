package cn.aiedge.storage.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileValidator 单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
class FileValidatorTest {

    private FileValidator fileValidator;
    private cn.aiedge.storage.config.StorageProperties properties;

    @BeforeEach
    void setUp() {
        properties = new cn.aiedge.storage.config.StorageProperties();
        fileValidator = new FileValidator(properties);
    }

    @Test
    @DisplayName("校验有效文件 - 成功")
    void testValidate_ValidFile() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new byte[1024]);

        // When
        FileValidator.ValidationResult result = fileValidator.validate(file);

        // Then
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("校验空文件 - 失败")
    void testValidate_EmptyFile() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new byte[0]);

        // When
        FileValidator.ValidationResult result = fileValidator.validate(file);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("空"));
    }

    @Test
    @DisplayName("校验超大文件 - 失败")
    void testValidate_TooLargeFile() {
        // Given - 创建一个超过默认100MB限制的文件
        byte[] largeContent = new byte[101 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", largeContent);

        // When
        FileValidator.ValidationResult result = fileValidator.validate(file);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("大小超过限制"));
    }

    @Test
    @DisplayName("校验禁止的扩展名 - 失败")
    void testValidate_DeniedExtension() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.exe", "application/octet-stream", new byte[1024]);

        // When
        FileValidator.ValidationResult result = fileValidator.validate(file);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("不允许"));
    }

    @Test
    @DisplayName("校验允许的扩展名 - 成功")
    void testValidate_AllowedExtension() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", new byte[1024]);

        // When
        FileValidator.ValidationResult result = fileValidator.validate(file);

        // Then
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("校验扩展名 - 空扩展名")
    void testValidateExtension_EmptyExtension() {
        // When
        FileValidator.ValidationResult result = fileValidator.validateExtension("");

        // Then
        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("校验扩展名 - null")
    void testValidateExtension_Null() {
        // When
        FileValidator.ValidationResult result = fileValidator.validateExtension(null);

        // Then
        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("校验扩展名 - 在黑名单中")
    void testValidateExtension_InDenyList() {
        // When
        FileValidator.ValidationResult result = fileValidator.validateExtension("bat");

        // Then
        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("校验扩展名 - 在白名单中")
    void testValidateExtension_InAllowList() {
        // When
        FileValidator.ValidationResult result = fileValidator.validateExtension("png");

        // Then
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("校验文件内容 - JPEG魔数")
    void testValidateContent_JpegMagic() throws Exception {
        // Given - JPEG文件头
        byte[] jpegHeader = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(jpegHeader);

        // When
        FileValidator.ValidationResult result = fileValidator.validateContent(bis, "jpg");

        // Then
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("校验文件内容 - PNG魔数")
    void testValidateContent_PngMagic() throws Exception {
        // Given - PNG文件头
        byte[] pngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(pngHeader);

        // When
        FileValidator.ValidationResult result = fileValidator.validateContent(bis, "png");

        // Then
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("校验文件内容 - PDF魔数")
    void testValidateContent_PdfMagic() throws Exception {
        // Given - PDF文件头 (%PDF)
        byte[] pdfHeader = new byte[]{0x25, 0x50, 0x44, 0x46};
        java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(pdfHeader);

        // When
        FileValidator.ValidationResult result = fileValidator.validateContent(bis, "pdf");

        // Then
        assertTrue(result.isValid());
    }
}
