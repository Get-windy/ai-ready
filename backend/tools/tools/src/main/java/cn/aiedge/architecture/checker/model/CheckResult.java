package cn.aiedge.architecture.checker.model;

import cn.aiedge.architecture.checker.core.CheckRule;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 检查结果
 * 
 * <p>包含单次检查的所有结果信息</p>
 * 
 * @author Architecture Team
 * @version 1.0.0
 * @since 2026-05-05
 */
@Data
@Builder
public class CheckResult {

    /**
     * 检查ID
     */
    private String checkId;
    
    /**
     * 项目上下文
     */
    private ProjectContext projectContext;
    
    /**
     * 检查规则ID
     */
    private String ruleId;
    
    /**
     * 检查规则名称
     */
    private String ruleName;
    
    /**
     * 检查规则类别
     */
    private String ruleCategory;
    
    /**
     * 检查规则严重级别
     */
    private CheckRule.Severity ruleSeverity;
    
    /**
     * 检查开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 检查结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 检查耗时（毫秒）
     */
    private long durationMs;
    
    /**
     * 检查状态
     */
    private CheckStatus status;
    
    /**
     * 发现的问题列表
     */
    private List<Issue> issues;
    
    /**
     * 检查统计信息
     */
    private Statistics statistics;
    
    /**
     * 检查错误信息（如果检查失败）
     */
    private String errorMessage;
    
    /**
     * 检查堆栈跟踪（如果检查失败）
     */
    private String stackTrace;
    
    /**
     * 检查状态枚举
     */
    public enum CheckStatus {
        /**
         * 检查成功完成
         */
        SUCCESS,
        
        /**
         * 检查失败（发生异常）
         */
        FAILED,
        
        /**
         * 检查超时
         */
        TIMEOUT,
        
        /**
         * 检查被取消
         */
        CANCELLED,
        
        /**
         * 检查跳过（如文件大小超限）
         */
        SKIPPED
    }
    
    /**
     * 问题详情
     */
    @Data
    @Builder
    public static class Issue {
        /**
         * 问题ID
         */
        private String issueId;
        
        /**
         * 问题类型
         */
        private String type;
        
        /**
         * 问题严重级别
         */
        private CheckRule.Severity severity;
        
        /**
         * 问题位置信息
         */
        private Location location;
        
        /**
         * 问题描述
         */
        private String description;
        
        /**
         * 问题详情
         */
        private String details;
        
        /**
         * 问题原因分析
         */
        private String cause;
        
        /**
         * 修复建议
         */
        private String fixSuggestion;
        
        /**
         * 修复难度（1-5，1最容易，5最困难）
         */
        private int fixDifficulty;
        
        /**
         * 修复优先级（1-5，1最高，5最低）
         */
        private int fixPriority;
        
        /**
         * 问题代码示例（如果有）
         */
        private String codeExample;
        
        /**
         * 修复后代码示例（如果有）
         */
        private String fixedCodeExample;
        
        /**
         * 是否已修复
         */
        private boolean fixed;
        
        /**
         * 修复时间（如果已修复）
         */
        private LocalDateTime fixedAt;
        
        /**
         * 修复者（如果已修复）
         */
        private String fixedBy;
        
        /**
         * 问题元数据
         */
        private Metadata metadata;
        
        /**
         * 问题位置信息
         */
        @Data
        @Builder
        public static class Location {
            /**
             * 文件路径（相对于项目根目录）
             */
            private String filePath;
            
            /**
             * 文件绝对路径
             */
            private String absolutePath;
            
            /**
             * 行号（从1开始）
             */
            private int line;
            
            /**
             * 列号（从1开始）
             */
            private int column;
            
            /**
             * 结束行号
             */
            private int endLine;
            
            /**
             * 结束列号
             */
            private int endColumn;
            
            /**
             * 代码片段
             */
            private String codeSnippet;
            
            /**
             * 模块名称
             */
            private String module;
            
            /**
             * 包名（对于Java文件）
             */
            private String packageName;
            
            /**
             * 类名（对于Java文件）
             */
            private String className;
            
            /**
             * 方法名（对于Java文件）
             */
            private String methodName;
        }
        
        /**
         * 问题元数据
         */
        @Data
        @Builder
        public static class Metadata {
            /**
             * 创建时间
             */
            private LocalDateTime createdAt;
            
            /**
             * 最后更新时间
             */
            private LocalDateTime updatedAt;
            
            /**
             * 标签列表
             */
            private List<String> tags;
            
            /**
             * 自定义属性
             */
            private Object customProperties;
            
            /**
             * 关联的规则配置
             */
            private Object ruleConfig;
            
            /**
             * 问题来源（如manual, automated, imported等）
             */
            private String source;
            
            /**
             * 问题置信度（0.0-1.0）
             */
            private double confidence;
            
            /**
             * 是否自动生成
             */
            private boolean autoGenerated;
        }
    }
    
    /**
     * 检查统计信息
     */
    @Data
    @Builder
    public static class Statistics {
        /**
         * 总检查文件数
         */
        private int totalFiles;
        
        /**
         * 总检查规则数
         */
        private int totalRules;
        
        /**
         * 成功检查规则数
         */
        private int successRules;
        
        /**
         * 失败检查规则数
         */
        private int failedRules;
        
        /**
         * 跳过检查规则数
         */
        private int skippedRules;
        
        /**
         * 总发现问题数
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
         * 平均问题数每文件
         */
        private double avgIssuesPerFile;
        
        /**
         * 平均问题数每规则
         */
        private double avgIssuesPerRule;
        
        /**
         * 检查覆盖率（%）
         */
        private double coveragePercent;
        
        /**
         * 缓存命中次数
         */
        private int cacheHits;
        
        /**
         * 缓存未命中次数
         */
        private int cacheMisses;
        
        /**
         * 缓存命中率（%）
         */
        private double cacheHitRate;
        
        /**
         * 内存使用峰值（MB）
         */
        private double peakMemoryUsageMb;
        
        /**
         * CPU使用率峰值（%）
         */
        private double peakCpuUsagePercent;
        
        /**
         * I/O读取量（KB）
         */
        private long ioReadKb;
        
        /**
         * I/O写入量（KB）
         */
        private long ioWriteKb;
    }
}