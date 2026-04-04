package cn.aiedge.storage.validator;

import cn.aiedge.storage.config.StorageProperties;
import cn.aiedge.storage.model.FileTypeCategory;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件校验器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class FileValidator {

    private final StorageProperties.UploadConfig uploadConfig;
    private final Set<String> allowedExtensions;
    private final Set<String> deniedExtensions;

    // 文件魔数（文件头）映射
    private static final FileMagic[] FILE_MAGICS = {
            // 图片
            new FileMagic(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, "jpg", FileTypeCategory.IMAGE),
            new FileMagic(new byte[]{0x89, 0x50, 0x4E, 0x47}, "png", FileTypeCategory.IMAGE),
            new FileMagic(new byte[]{0x47, 0x49, 0x46, 0x38}, "gif", FileTypeCategory.IMAGE),
            new FileMagic(new byte[]{0x42, 0x4D}, "bmp", FileTypeCategory.IMAGE),
            new FileMagic(new byte[]{0x52, 0x49, 0x46, 0x46}, "webp", FileTypeCategory.IMAGE),
            // 文档
            new FileMagic(new byte[]{0x25, 0x50, 0x44, 0x46}, "pdf", FileTypeCategory.DOCUMENT),
            new FileMagic(new byte[]{0x50, 0x4B, 0x03, 0x04}, "zip", FileTypeCategory.ARCHIVE), // 也用于docx/xlsx/pptx
            new FileMagic(new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}, "doc", FileTypeCategory.DOCUMENT), // doc/xls/ppt
            // 压缩包
            new FileMagic(new byte[]{0x52, 0x61, 0x72, 0x21}, "rar", FileTypeCategory.ARCHIVE),
            new FileMagic(new byte[]{0x37, 0x7A, (byte) 0xBC, (byte) 0xAF}, "7z", FileTypeCategory.ARCHIVE),
            // 视频
            new FileMagic(new byte[]{0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70}, "mp4", FileTypeCategory.VIDEO),
            new FileMagic(new byte[]{0x1A, 0x45, (byte) 0xDF, (byte) 0xA3}, "mkv", FileTypeCategory.VIDEO),
            // 音频
            new FileMagic(new byte[]{0x49, 0x44, 0x33}, "mp3", FileTypeCategory.AUDIO),
            new FileMagic(new byte[]{(byte) 0xFF, (byte) 0xFB}, "mp3", FileTypeCategory.AUDIO),
            new FileMagic(new byte[]{0x52, 0x49, 0x46, 0x46}, "wav", FileTypeCategory.AUDIO),
    };

    public FileValidator(StorageProperties properties) {
        this.uploadConfig = properties.getUpload();
        this.allowedExtensions = parseExtensions(uploadConfig.getAllowedExtensions());
        this.deniedExtensions = parseExtensions(uploadConfig.getDeniedExtensions());
    }

    /**
     * 校验文件
     *
     * @param file 上传的文件
     * @return 校验结果
     */
    public ValidationResult validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.failure("文件为空");
        }

        // 校验文件大小
        if (file.getSize() > uploadConfig.getMaxFileSize()) {
            return ValidationResult.failure("文件大小超过限制: " + formatSize(uploadConfig.getMaxFileSize()));
        }

        String originalName = file.getOriginalFilename();
        String extension = FileUtil.extName(originalName);

        // 校验文件扩展名
        ValidationResult extensionResult = validateExtension(extension);
        if (!extensionResult.isValid()) {
            return extensionResult;
        }

        // 校验文件内容（魔数检查）
        if (uploadConfig.isCheckContent()) {
            try {
                ValidationResult contentResult = validateContent(file.getInputStream(), extension);
                if (!contentResult.isValid()) {
                    return contentResult;
                }
            } catch (IOException e) {
                log.warn("Failed to read file content for validation: {}", originalName, e);
                return ValidationResult.failure("无法读取文件内容");
            }
        }

        return ValidationResult.success();
    }

    /**
     * 校验文件扩展名
     */
    public ValidationResult validateExtension(String extension) {
        if (StrUtil.isBlank(extension)) {
            return ValidationResult.failure("文件扩展名为空");
        }

        String ext = extension.toLowerCase();

        // 检查是否在黑名单中
        if (deniedExtensions.contains(ext)) {
            return ValidationResult.failure("不允许上传此类型的文件: " + extension);
        }

        // 检查是否在白名单中
        if (!allowedExtensions.isEmpty() && !allowedExtensions.contains(ext)) {
            return ValidationResult.failure("不允许上传此类型的文件: " + extension);
        }

        return ValidationResult.success();
    }

    /**
     * 校验文件内容（魔数检查）
     */
    public ValidationResult validateContent(InputStream inputStream, String claimedExtension) {
        try {
            byte[] header = new byte[8];
            int bytesRead = inputStream.read(header);
            
            if (bytesRead < 2) {
                return ValidationResult.failure("文件内容无效");
            }

            // 查找匹配的文件类型
            for (FileMagic magic : FILE_MAGICS) {
                if (matchesMagic(header, magic.magic)) {
                    // 验证扩展名是否与文件内容匹配
                    if (magic.category == FileTypeCategory.ARCHIVE && 
                        (claimedExtension.equalsIgnoreCase("docx") || 
                         claimedExtension.equalsIgnoreCase("xlsx") || 
                         claimedExtension.equalsIgnoreCase("pptx"))) {
                        // Office文档也是zip格式，跳过此项检查
                        return ValidationResult.success();
                    }
                    if (!magic.extension.equalsIgnoreCase(claimedExtension)) {
                        log.warn("File extension mismatch: claimed={}, actual={}", claimedExtension, magic.extension);
                        // 警告但不拒绝
                    }
                    return ValidationResult.success();
                }
            }

            // 未找到匹配的魔数，允许但记录
            log.info("Unknown file magic number for extension: {}", claimedExtension);
            return ValidationResult.success();

        } catch (IOException e) {
            log.error("Error reading file header", e);
            return ValidationResult.failure("读取文件头失败");
        }
    }

    /**
     * 检查文件头是否匹配魔数
     */
    private boolean matchesMagic(byte[] header, byte[] magic) {
        if (header.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (header[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 解析扩展名列表
     */
    private Set<String> parseExtensions(String extensions) {
        if (StrUtil.isBlank(extensions)) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(extensions.toLowerCase().split(",")));
    }

    /**
     * 格式化文件大小
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 文件魔数定义
     */
    private record FileMagic(byte[] magic, String extension, FileTypeCategory category) {}

    /**
     * 校验结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, "校验通过");
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
