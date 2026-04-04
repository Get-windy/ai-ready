package cn.aiedge.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置属性
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * 存储类型：local/oss/s3
     */
    private String type = "local";

    /**
     * 本地存储配置
     */
    private LocalConfig local = new LocalConfig();

    /**
     * OSS存储配置
     */
    private OssConfig oss = new OssConfig();

    /**
     * S3存储配置
     */
    private S3Config s3 = new S3Config();

    /**
     * 文件上传配置
     */
    private UploadConfig upload = new UploadConfig();

    @Data
    public static class LocalConfig {
        /**
         * 存储基础路径
         */
        private String basePath = "./uploads";

        /**
         * 访问基础URL
         */
        private String baseUrl;
    }

    @Data
    public static class OssConfig {
        /**
         * 是否启用
         */
        private boolean enabled = false;

        /**
         * Endpoint
         */
        private String endpoint;

        /**
         * Access Key ID
         */
        private String accessKeyId;

        /**
         * Access Key Secret
         */
        private String accessKeySecret;

        /**
         * Bucket名称
         */
        private String bucketName;

        /**
         * 访问域名
         */
        private String domain;

        /**
         * 存储路径前缀
         */
        private String pathPrefix = "";
    }

    @Data
    public static class S3Config {
        /**
         * 是否启用
         */
        private boolean enabled = false;

        /**
         * Region
         */
        private String region;

        /**
         * Endpoint
         */
        private String endpoint;

        /**
         * Access Key ID
         */
        private String accessKeyId;

        /**
         * Secret Access Key
         */
        private String secretAccessKey;

        /**
         * Bucket名称
         */
        private String bucketName;

        /**
         * 存储路径前缀
         */
        private String pathPrefix = "";
    }

    @Data
    public static class UploadConfig {
        /**
         * 最大文件大小（字节）
         */
        private long maxFileSize = 100 * 1024 * 1024; // 100MB

        /**
         * 最大请求大小（字节）
         */
        private long maxRequestSize = 500 * 1024 * 1024; // 500MB

        /**
         * 允许的文件扩展名
         */
        private String allowedExtensions = "jpg,jpeg,png,gif,bmp,webp,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,zip,rar,mp4,mp3";

        /**
         * 禁止的文件扩展名
         */
        private String deniedExtensions = "exe,bat,cmd,sh,js,vbs,php,asp,aspx,jsp";

        /**
         * 是否检查文件内容
         */
        private boolean checkContent = true;
    }
}
