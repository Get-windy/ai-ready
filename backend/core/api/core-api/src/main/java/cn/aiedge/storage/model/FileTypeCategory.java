package cn.aiedge.storage.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件类型分类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Getter
public enum FileTypeCategory {

    /**
     * 图片
     */
    IMAGE("图片", Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico"), true, "image"),

    /**
     * 文档
     */
    DOCUMENT("文档", Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt"), true, "document"),

    /**
     * 视频
     */
    VIDEO("视频", Set.of("mp4", "avi", "mov", "wmv", "flv", "mkv", "webm"), true, "video"),

    /**
     * 音频
     */
    AUDIO("音频", Set.of("mp3", "wav", "flac", "aac", "ogg", "wma"), true, "audio"),

    /**
     * 压缩包
     */
    ARCHIVE("压缩包", Set.of("zip", "rar", "7z", "tar", "gz", "bz2"), false, "archive"),

    /**
     * 代码文件
     */
    CODE("代码", Set.of("java", "py", "js", "ts", "html", "css", "json", "xml", "sql", "md"), true, "code"),

    /**
     * 其他
     */
    OTHER("其他", Set.of(), false, "other");

    private final String displayName;
    private final Set<String> extensions;
    private final boolean previewable;
    private final String categoryCode;

    FileTypeCategory(String displayName, Set<String> extensions, boolean previewable, String categoryCode) {
        this.displayName = displayName;
        this.extensions = extensions;
        this.previewable = previewable;
        this.categoryCode = categoryCode;
    }

    /**
     * 根据扩展名获取文件类型
     */
    public static FileTypeCategory fromExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return OTHER;
        }
        String ext = extension.toLowerCase().replace(".", "");
        return Arrays.stream(values())
                .filter(category -> category.extensions.contains(ext))
                .findFirst()
                .orElse(OTHER);
    }

    /**
     * 根据MIME类型获取文件类型
     */
    public static FileTypeCategory fromContentType(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return OTHER;
        }
        String type = contentType.toLowerCase();
        if (type.startsWith("image/")) {
            return IMAGE;
        }
        if (type.startsWith("video/")) {
            return VIDEO;
        }
        if (type.startsWith("audio/")) {
            return AUDIO;
        }
        if (type.contains("pdf") || type.contains("document") || type.contains("spreadsheet") 
            || type.contains("presentation") || type.contains("text")) {
            return DOCUMENT;
        }
        if (type.contains("zip") || type.contains("rar") || type.contains("compressed")) {
            return ARCHIVE;
        }
        return OTHER;
    }

    /**
     * 获取所有允许的扩展名
     */
    public static Set<String> getAllAllowedExtensions() {
        return Arrays.stream(values())
                .flatMap(category -> category.extensions.stream())
                .collect(Collectors.toSet());
    }

    /**
     * 判断扩展名是否允许
     */
    public static boolean isAllowedExtension(String extension, Set<String> allowedExtensions) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        String ext = extension.toLowerCase().replace(".", "");
        return allowedExtensions.contains(ext);
    }
}
