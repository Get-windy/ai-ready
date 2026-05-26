package cn.aiedge.storage.preview;

import cn.aiedge.storage.model.FileTypeCategory;
import cn.aiedge.storage.model.StorageFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件预览服务
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class FilePreviewService {

    /**
     * 获取预览信息
     *
     * @param file 文件信息
     * @return 预览信息
     */
    public PreviewInfo getPreviewInfo(StorageFile file) {
        if (file == null) {
            return PreviewInfo.notSupported();
        }

        FileTypeCategory category = file.getCategory();
        if (!category.isPreviewable()) {
            return PreviewInfo.notSupported();
        }

        return switch (category) {
            case IMAGE -> buildImagePreview(file);
            case DOCUMENT -> buildDocumentPreview(file);
            case VIDEO -> buildVideoPreview(file);
            case AUDIO -> buildAudioPreview(file);
            case CODE -> buildCodePreview(file);
            default -> PreviewInfo.notSupported();
        };
    }

    /**
     * 构建图片预览
     */
    private PreviewInfo buildImagePreview(StorageFile file) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("previewType", "image");
        extra.put("src", file.getAccessUrl());
        extra.put("alt", file.getOriginalName());

        return PreviewInfo.builder()
                .previewable(true)
                .previewType("image")
                .previewUrl(file.getAccessUrl())
                .mimeType(file.getContentType())
                .extra(extra)
                .build();
    }

    /**
     * 构建文档预览
     */
    private PreviewInfo buildDocumentPreview(StorageFile file) {
        String extension = file.getExtension().toLowerCase();
        Map<String, Object> extra = new HashMap<>();

        if ("pdf".equals(extension)) {
            // PDF可以直接预览
            extra.put("previewType", "pdf");
            extra.put("src", file.getAccessUrl());
            return PreviewInfo.builder()
                    .previewable(true)
                    .previewType("pdf")
                    .previewUrl(file.getAccessUrl())
                    .mimeType(file.getContentType())
                    .extra(extra)
                    .build();
        } else if (extension.matches("doc|docx|xls|xlsx|ppt|pptx")) {
            // Office文档，可以使用Office Online或第三方服务预览
            String previewUrl = buildOfficePreviewUrl(file.getAccessUrl());
            extra.put("previewType", "office");
            extra.put("src", previewUrl);
            extra.put("originalUrl", file.getAccessUrl());

            return PreviewInfo.builder()
                    .previewable(true)
                    .previewType("office")
                    .previewUrl(previewUrl)
                    .mimeType(file.getContentType())
                    .extra(extra)
                    .build();
        } else {
            // 纯文本
            extra.put("previewType", "text");
            extra.put("src", file.getAccessUrl());

            return PreviewInfo.builder()
                    .previewable(true)
                    .previewType("text")
                    .previewUrl(file.getAccessUrl())
                    .mimeType(file.getContentType())
                    .extra(extra)
                    .build();
        }
    }

    /**
     * 构建视频预览
     */
    private PreviewInfo buildVideoPreview(StorageFile file) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("previewType", "video");
        extra.put("src", file.getAccessUrl());
        extra.put("type", file.getContentType());

        return PreviewInfo.builder()
                .previewable(true)
                .previewType("video")
                .previewUrl(file.getAccessUrl())
                .mimeType(file.getContentType())
                .extra(extra)
                .build();
    }

    /**
     * 构建音频预览
     */
    private PreviewInfo buildAudioPreview(StorageFile file) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("previewType", "audio");
        extra.put("src", file.getAccessUrl());
        extra.put("type", file.getContentType());

        return PreviewInfo.builder()
                .previewable(true)
                .previewType("audio")
                .previewUrl(file.getAccessUrl())
                .mimeType(file.getContentType())
                .extra(extra)
                .build();
    }

    /**
     * 构建代码预览
     */
    private PreviewInfo buildCodePreview(StorageFile file) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("previewType", "code");
        extra.put("src", file.getAccessUrl());
        extra.put("language", getLanguageByExtension(file.getExtension()));

        return PreviewInfo.builder()
                .previewable(true)
                .previewType("code")
                .previewUrl(file.getAccessUrl())
                .mimeType(file.getContentType())
                .extra(extra)
                .build();
    }

    /**
     * 构建Office文档预览URL
     * 使用Microsoft Office Online服务
     */
    private String buildOfficePreviewUrl(String fileUrl) {
        // 实际应使用配置的预览服务地址
        // 这里使用Microsoft Office Online作为示例
        return "https://view.officeapps.live.com/op/view.aspx?src=" + 
               Base64.getEncoder().encodeToString(fileUrl.getBytes());
    }

    /**
     * 根据扩展名获取代码语言
     */
    private String getLanguageByExtension(String extension) {
        return switch (extension.toLowerCase()) {
            case "java" -> "java";
            case "py" -> "python";
            case "js" -> "javascript";
            case "ts" -> "typescript";
            case "html" -> "html";
            case "css" -> "css";
            case "json" -> "json";
            case "xml" -> "xml";
            case "sql" -> "sql";
            case "md" -> "markdown";
            default -> "text";
        };
    }

    /**
     * 预览信息
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PreviewInfo {
        /**
         * 是否可预览
         */
        private boolean previewable;

        /**
         * 预览类型
         */
        private String previewType;

        /**
         * 预览URL
         */
        private String previewUrl;

        /**
         * MIME类型
         */
        private String mimeType;

        /**
         * 额外信息
         */
        private Map<String, Object> extra;

        public static PreviewInfo notSupported() {
            return PreviewInfo.builder()
                    .previewable(false)
                    .previewType("none")
                    .build();
        }
    }
}
