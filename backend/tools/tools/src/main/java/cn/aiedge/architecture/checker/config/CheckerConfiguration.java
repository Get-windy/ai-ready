package cn.aiedge.architecture.checker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * 架构检查工具配置类
 * 
 * <p>读取应用程序配置，支持通过application.yml或命令行参数配置</p>
 * 
 * @author Architecture Team
 * @version 1.0.0
 * @since 2026-05-05
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "architecture.checker")
public class CheckerConfiguration {

    /**
     * 项目配置
     */
    private ProjectConfig project = new ProjectConfig();
    
    /**
     * 检查规则配置
     */
    private RulesConfig rules = new RulesConfig();
    
    /**
     * 输出配置
     */
    private OutputConfig output = new OutputConfig();
    
    /**
     * 缓存配置
     */
    private CacheConfig cache = new CacheConfig();
    
    /**
     * 性能配置
     */
    private PerformanceConfig performance = new PerformanceConfig();

    /**
     * 项目配置类
     */
    @Data
    public static class ProjectConfig {
        /**
         * 项目名称
         */
        private String name = "default-project";
        
        /**
         * 项目根路径
         */
        private String basePath = ".";
        
        /**
         * 源文件编码
         */
        private String encoding = "UTF-8";
        
        /**
         * 支持的语言列表
         */
        private List<String> supportedLanguages = List.of("java", "xml", "yaml", "properties");
        
        /**
         * 排除目录列表
         */
        private List<String> excludeDirectories = List.of(
            ".git", ".svn", ".idea", "node_modules", "target", "build", "out", "dist"
        );
        
        /**
         * 排除文件模式列表
         */
        private List<String> excludeFilePatterns = List.of(
            ".*\\.log$", ".*\\.jar$", ".*\\.class$", ".*\\.iml$"
        );
    }

    /**
     * 检查规则配置类
     */
    @Data
    public static class RulesConfig {
        /**
         * 是否启用目录结构检查
         */
        private boolean directoryCheckEnabled = true;
        
        /**
         * 是否启用包命名检查
         */
        private boolean packageCheckEnabled = true;
        
        /**
         * 是否启用API路径检查
         */
        private boolean apiCheckEnabled = true;
        
        /**
         * 是否启用代码规范检查
         */
        private boolean codeCheckEnabled = true;
        
        /**
         * 严格模式（发现错误即终止）
         */
        private boolean strictMode = false;
        
        /**
         * 规则文件路径
         */
        private String rulesFile = ".architecture-rules.yaml";
        
        /**
         * 自定义规则目录
         */
        private String customRulesDirectory = "custom-rules";
        
        /**
         * 规则优先级映射
         */
        private Map<String, Integer> rulePriorities = Map.of(
            "directory", 100,
            "package", 90,
            "api", 80,
            "code", 70
        );
    }

    /**
     * 输出配置类
     */
    @Data
    public static class OutputConfig {
        /**
         * 输出格式（json, html, markdown, console）
         */
        private String format = "console";
        
        /**
         * 输出目录
         */
        private String directory = "./reports";
        
        /**
         * 输出文件名（不含扩展名）
         */
        private String filename = "architecture-compliance-report";
        
        /**
         * 是否包含摘要信息
         */
        private boolean includeSummary = true;
        
        /**
         * 是否包含详细问题信息
         */
        private boolean includeDetails = true;
        
        /**
         * 是否包含修复建议
         */
        private boolean includeSuggestions = true;
        
        /**
         * 是否包含统计数据
         */
        private boolean includeStatistics = true;
        
        /**
         * 最大问题数量（0表示无限制）
         */
        private int maxIssues = 0;
        
        /**
         * 输出文件编码
         */
        private String encoding = "UTF-8";
    }

    /**
     * 缓存配置类
     */
    @Data
    public static class CacheConfig {
        /**
         * 是否启用缓存
         */
        private boolean enabled = true;
        
        /**
         * 缓存目录
         */
        private String directory = "./.architecture-checker-cache";
        
        /**
         * 缓存最大大小（MB）
         */
        private int maxSizeMb = 100;
        
        /**
         * 缓存过期时间（分钟）
         */
        private int expireMinutes = 60;
        
        /**
         * 缓存清理间隔（分钟）
         */
        private int cleanupIntervalMinutes = 30;
        
        /**
         * 是否启用内存缓存
         */
        private boolean memoryCacheEnabled = true;
        
        /**
         * 内存缓存最大条目数
         */
        private int memoryCacheMaxEntries = 1000;
    }

    /**
     * 性能配置类
     */
    @Data
    public static class PerformanceConfig {
        /**
         * 并行检查线程数
         */
        private int parallelThreads = 4;
        
        /**
         * 最大文件大小（KB），超过此大小的文件跳过深度检查
         */
        private int maxFileSizeKb = 1024;
        
        /**
         * 超时时间（秒）
         */
        private int timeoutSeconds = 300;
        
        /**
         * 内存限制（MB）
         */
        private int memoryLimitMb = 512;
        
        /**
         * 是否启用增量检查
         */
        private boolean incrementalCheck = true;
        
        /**
         * 增量检查缓存天数
         */
        private int incrementalCacheDays = 7;
        
        /**
         * 是否启用采样检查（大项目）
         */
        private boolean samplingEnabled = false;
        
        /**
         * 采样比例（0.0-1.0）
         */
        private double samplingRatio = 0.1;
    }
}