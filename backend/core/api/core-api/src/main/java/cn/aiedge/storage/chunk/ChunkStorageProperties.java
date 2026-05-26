package cn.aiedge.storage.chunk;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 分片上传配置
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage.chunk")
public class ChunkStorageProperties {

    /**
     * 临时文件存储路径
     */
    private String tempPath = System.getProperty("java.io.tmpdir") + "/file-chunks";

    /**
     * 默认分片大小（字节）
     */
    private Integer defaultChunkSize = 5 * 1024 * 1024; // 5MB

    /**
     * 最大分片大小（字节）
     */
    private Integer maxChunkSize = 100 * 1024 * 1024; // 100MB

    /**
     * 最小分片大小（字节）
     */
    private Integer minChunkSize = 1024 * 1024; // 1MB

    /**
     * 上传任务过期时间（小时）
     */
    private Integer expireHours = 24;

    /**
     * 是否启用秒传
     */
    private Boolean enableInstantUpload = true;

    /**
     * 并发合并线程数
     */
    private Integer mergeThreads = 4;
}
