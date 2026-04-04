package cn.aiedge.storage.preview;

import cn.aiedge.storage.model.StorageFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 增强的文件预览服务
 * 支持缩略图生成、水印添加
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedPreviewService {

    private static final int THUMBNAIL_WIDTH = 200;
    private static final int THUMBNAIL_HEIGHT = 200;

    /**
     * 生成图片缩略图
     *
     * @param inputStream 原图输入流
     * @param format 图片格式
     * @return 缩略图字节数组
     */
    public byte[] generateThumbnail(InputStream inputStream, String format) {
        try {
            BufferedImage originalImage = ImageIO.read(inputStream);
            if (originalImage == null) {
                throw new IOException("无法读取图片");
            }

            // 计算缩略图尺寸（保持比例）
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            int[] thumbnailSize = calculateThumbnailSize(originalWidth, originalHeight);

            // 创建缩略图
            BufferedImage thumbnail = new BufferedImage(
                    thumbnailSize[0], thumbnailSize[1], BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = thumbnail.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(originalImage, 0, 0, thumbnailSize[0], thumbnailSize[1], null);
            g2d.dispose();

            // 输出为字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String outputFormat = normalizeFormat(format);
            ImageIO.write(thumbnail, outputFormat, baos);
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Failed to generate thumbnail", e);
            throw new RuntimeException("缩略图生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 添加水印
     *
     * @param inputStream 原图输入流
     * @param watermarkText 水印文字
     * @param format 图片格式
     * @return 加水印后的图片字节数组
     */
    public byte[] addWatermark(InputStream inputStream, String watermarkText, String format) {
        try {
            BufferedImage originalImage = ImageIO.read(inputStream);
            if (originalImage == null) {
                throw new IOException("无法读取图片");
            }

            Graphics2D g2d = originalImage.createGraphics();

            // 设置水印样式
            g2d.setColor(new Color(255, 255, 255, 128)); // 半透明白色
            g2d.setFont(new Font("Arial", Font.BOLD, 24));

            // 计算水印位置（右下角）
            FontMetrics fontMetrics = g2d.getFontMetrics();
            int textWidth = fontMetrics.stringWidth(watermarkText);
            int textHeight = fontMetrics.getHeight();
            int x = originalImage.getWidth() - textWidth - 20;
            int y = originalImage.getHeight() - textHeight;

            // 绘制水印
            g2d.drawString(watermarkText, x, y);
            g2d.dispose();

            // 输出为字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String outputFormat = normalizeFormat(format);
            ImageIO.write(originalImage, outputFormat, baos);
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Failed to add watermark", e);
            throw new RuntimeException("水印添加失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成预览图（支持多种格式）
     *
     * @param storageFile 文件信息
     * @param inputStream 文件流
     * @return 预览结果
     */
    public PreviewResult generatePreview(StorageFile storageFile, InputStream inputStream) {
        String extension = storageFile.getExtension().toLowerCase();
        Map<String, Object> metadata = new HashMap<>();

        try {
            // 图片类型
            if (isImage(extension)) {
                byte[] thumbnail = generateThumbnail(inputStream, extension);
                String base64Thumbnail = Base64.getEncoder().encodeToString(thumbnail);
                String dataUrl = "data:image/" + extension + ";base64," + base64Thumbnail;

                metadata.put("thumbnailUrl", dataUrl);
                metadata.put("previewType", "image");
                metadata.put("previewUrl", storageFile.getAccessUrl());

                return PreviewResult.success("image", storageFile.getAccessUrl(), metadata);
            }

            // PDF类型
            if ("pdf".equals(extension)) {
                metadata.put("previewType", "pdf");
                metadata.put("previewUrl", storageFile.getAccessUrl());
                return PreviewResult.success("pdf", storageFile.getAccessUrl(), metadata);
            }

            // Office类型
            if (extension.matches("doc|docx|xls|xlsx|ppt|pptx")) {
                String officePreviewUrl = buildOfficePreviewUrl(storageFile.getAccessUrl());
                metadata.put("previewType", "office");
                metadata.put("previewUrl", officePreviewUrl);
                return PreviewResult.success("office", officePreviewUrl, metadata);
            }

            // 视频类型
            if (isVideo(extension)) {
                metadata.put("previewType", "video");
                metadata.put("previewUrl", storageFile.getAccessUrl());
                return PreviewResult.success("video", storageFile.getAccessUrl(), metadata);
            }

            // 音频类型
            if (isAudio(extension)) {
                metadata.put("previewType", "audio");
                metadata.put("previewUrl", storageFile.getAccessUrl());
                return PreviewResult.success("audio", storageFile.getAccessUrl(), metadata);
            }

            // 代码/文本类型
            if (isCode(extension) || isText(extension)) {
                metadata.put("previewType", "code");
                metadata.put("language", getLanguage(extension));
                return PreviewResult.success("code", storageFile.getAccessUrl(), metadata);
            }

            // 不支持预览
            return PreviewResult.unsupported();

        } catch (Exception e) {
            log.error("Failed to generate preview for file: {}", storageFile.getFileId(), e);
            return PreviewResult.error("预览生成失败: " + e.getMessage());
        }
    }

    /**
     * 计算缩略图尺寸（保持宽高比）
     */
    private int[] calculateThumbnailSize(int originalWidth, int originalHeight) {
        double widthRatio = (double) THUMBNAIL_WIDTH / originalWidth;
        double heightRatio = (double) THUMBNAIL_HEIGHT / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);

        int width = (int) (originalWidth * ratio);
        int height = (int) (originalHeight * ratio);

        return new int[]{width, height};
    }

    /**
     * 标准化图片格式
     */
    private String normalizeFormat(String format) {
        if (format == null) return "jpg";
        return switch (format.toLowerCase()) {
            case "jpg", "jpeg" -> "jpg";
            case "png" -> "png";
            case "gif" -> "gif";
            case "webp" -> "webp";
            default -> "jpg";
        };
    }

    /**
     * 构建Office预览URL
     */
    private String buildOfficePreviewUrl(String fileUrl) {
        return "https://view.officeapps.live.com/op/view.aspx?src=" + 
               Base64.getEncoder().encodeToString(fileUrl.getBytes());
    }

    private boolean isImage(String ext) {
        return ext.matches("jpg|jpeg|png|gif|bmp|webp|svg");
    }

    private boolean isVideo(String ext) {
        return ext.matches("mp4|avi|mov|wmv|flv|mkv|webm");
    }

    private boolean isAudio(String ext) {
        return ext.matches("mp3|wav|ogg|flac|aac|m4a");
    }

    private boolean isCode(String ext) {
        return ext.matches("java|py|js|ts|html|css|json|xml|sql|md|go|rs|cpp|c|h|php|rb");
    }

    private boolean isText(String ext) {
        return ext.matches("txt|log|csv|xml|json|md");
    }

    private String getLanguage(String ext) {
        return switch (ext.toLowerCase()) {
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
            case "go" -> "go";
            case "rs" -> "rust";
            case "cpp", "c" -> "cpp";
            case "php" -> "php";
            case "rb" -> "ruby";
            default -> "text";
        };
    }

    /**
     * 预览结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PreviewResult {
        private boolean success;
        private String previewType;
        private String previewUrl;
        private String errorMessage;
        private Map<String, Object> metadata;

        public static PreviewResult success(String type, String url, Map<String, Object> metadata) {
            return PreviewResult.builder()
                    .success(true)
                    .previewType(type)
                    .previewUrl(url)
                    .metadata(metadata)
                    .build();
        }

        public static PreviewResult unsupported() {
            return PreviewResult.builder()
                    .success(false)
                    .previewType("none")
                    .errorMessage("不支持预览此类型文件")
                    .build();
        }

        public static PreviewResult error(String message) {
            return PreviewResult.builder()
                    .success(false)
                    .previewType("none")
                    .errorMessage(message)
                    .build();
        }
    }
}
