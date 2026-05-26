package cn.aiedge.architecture.checker.model;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 项目上下文
 * 
 * <p>包含项目信息和检查执行上下文</p>
 * 
 * @author Architecture Team
 * @version 1.0.0
 * @since 2026-05-05
 */
@Data
@Builder
public class ProjectContext {

    /**
     * 项目基本信息
     */
    private ProjectInfo projectInfo;
    
    /**
     * 项目根目录路径
     */
    private Path projectRoot;
    
    /**
     * 项目文件列表
     */
    private List<ProjectFile> files;
    
    /**
     * 检查配置
     */
    private CheckConfiguration checkConfiguration;
    
    /**
     * 检查统计信息
     */
    private CheckStatistics statistics;
    
    /**
     * 检查开始时间
     */
    private LocalDateTime checkStartTime;
    
    /**
     * 检查上下文数据（运行时缓存）
     */
    @Builder.Default
    private Map<String, Object> contextData = Map.of();
    
    /**
     * 项目信息类
     */
    @Data
    @Builder
    public static class ProjectInfo {
        /**
         * 项目名称
         */
        private String name;
        
        /**
         * 项目类型（java, web, mobile等）
         */
        private String type;
        
        /**
         * 项目版本
         */
        private String version;
        
        /**
         * 项目描述
         */
        private String description;
        
        /**
         * 项目所有者
         */
        private String owner;
        
        /**
         * 项目创建时间
         */
        private LocalDateTime createdAt;
        
        /**
         * 项目最后修改时间
         */
        private LocalDateTime lastModifiedAt;
        
        /**
         * 项目技术栈
         */
        private List<String> techStack;
        
        /**
         * 项目模块列表
         */
        private List<String> modules;
        
        /**
         * 项目依赖项
         */
        private Map<String, String> dependencies;
    }
    
    /**
     * 项目文件信息
     */
    @Data
    @Builder
    public static class ProjectFile {
        /**
         * 文件路径（相对于项目根目录）
         */
        private Path relativePath;
        
        /**
         * 文件绝对路径
         */
        private Path absolutePath;
        
        /**
         * 文件名
         */
        private String fileName;
        
        /**
         * 文件扩展名
         */
        private String extension;
        
        /**
         * 文件大小（字节）
         */
        private long size;
        
        /**
         * 文件最后修改时间
         */
        private LocalDateTime lastModified;
        
        /**
         * 文件类型（java, xml, yaml, properties等）
         */
        private String fileType;
        
        /**
         * 文件编码
         */
        private String encoding;
        
        /**
         * 文件行数
         */
        private int lineCount;
        
        /**
         * 文件内容哈希（用于缓存）
         */
        private String contentHash;
        
        /**
         * 文件解析后的抽象语法树（AST）
         */
        private Object ast;
        
        /**
         * 文件元数据
         */
        private Map<String, Object> metadata;
    }
    
    /**
     * 检查配置类
     */
    @Data
    @Builder
    public static class CheckConfiguration {
        /**
         * 检查规则列表
         */
        private List<String> ruleIds;
        
        /**
         * 检查类别列表
         */
        private List<String> categories;
        
        /**
         * 检查严重级别阈值
         */
        private String severityThreshold;
        
        /**
         * 检查模式（full, incremental, sample）
         */
        private String checkMode;
        
        /**
         * 并行检查线程数
         */
        private int parallelThreads;
        
        /**
         * 超时时间（秒）
         */
        private int timeoutSeconds;
        
        /**
         * 最大文件大小（KB）
         */
        private int maxFileSizeKb;
        
        /**
         * 排除目录列表
         */
        private List<String> excludeDirectories;
        
        /**
         * 排除文件模式列表
         */
        private List<String> excludeFilePatterns;
        
        /**
         * 包含文件模式列表
         */
        private List<String> includeFilePatterns;
        
        /**
         * 检查深度限制
         */
        private int maxDepth;
        
        /**
         * 是否启用缓存
         */
        private boolean cacheEnabled;
        
        /**
         * 是否启用详细日志
         */
        private boolean verboseLogging;
        
        /**
         * 自定义规则配置
         */
        private Map<String, Object> customConfig;
    }
    
    /**
     * 检查统计信息
     */
    @Data
    @Builder
    public static class CheckStatistics {
        /**
         * 总文件数
         */
        private int totalFiles;
        
        /**
         * 已检查文件数
         */
        private int checkedFiles;
        
        /**
         * 跳过文件数
         */
        private int skippedFiles;
        
        /**
         * 错误文件数
         */
        private int errorFiles;
        
        /**
         * 总问题数
         */
        private int totalIssues;
        
        /**
         * 错误级别问题数
         */
        private int errorIssues;
        
        /**
         * 警告级别问题数
         */
        private int warningIssues;
        
        /**
         * 信息级别问题数
         */
        private int infoIssues;
        
        /**
         * 检查总耗时（毫秒）
         */
        private long totalTimeMs;
        
        /**
         * 平均检查时间（毫秒/文件）
         */
        private long averageTimePerFileMs;
        
        /**
         * 内存使用峰值（MB）
         */
        private double peakMemoryUsageMb;
        
        /**
         * CPU使用率峰值（%）
         */
        private double peakCpuUsagePercent;
        
        /**
         * 缓存命中率（%）
         */
        private double cacheHitRatePercent;
        
        /**
         * 检查进度（0-100）
         */
        private double progressPercent;
    }
}