package cn.aiedge.test.connectionpool.report;

import cn.aiedge.test.connectionpool.metrics.PoolMetricsCollector.PoolMetrics;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 性能测试报告生成器
 * 生成Markdown格式的性能测试报告
 */
@Slf4j
@Component
public class PerformanceReportGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 生成完整的性能测试报告
     */
    public String generateReport(ReportData reportData) {
        StringBuilder report = new StringBuilder();
        
        // 报告标题
        report.append("# 数据库连接池性能测试报告\n\n");
        report.append("**测试项目**: ").append(reportData.getProjectName()).append("\n");
        report.append("**测试时间**: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n");
        report.append("**测试环境**: ").append(reportData.getEnvironment()).append("\n");
        report.append("**测试执行者**: ").append(reportData.getTester()).append("\n\n");
        
        // 目录
        report.append("## 目录\n\n");
        report.append("1. [执行摘要](#1-执行摘要)\n");
        report.append("2. [测试环境](#2-测试环境)\n");
        report.append("3. [测试场景](#3-测试场景)\n");
        report.append("4. [性能测试结果](#4-性能测试结果)\n");
        report.append("5. [连接池配置分析](#5-连接池配置分析)\n");
        report.append("6. [优化建议](#6-优化建议)\n");
        report.append("7. [结论](#7-结论)\n\n");
        
        // 1. 执行摘要
        report.append("## 1. 执行摘要\n\n");
        report.append(generateExecutiveSummary(reportData));
        
        // 2. 测试环境
        report.append("## 2. 测试环境\n\n");
        report.append(generateEnvironmentSection(reportData));
        
        // 3. 测试场景
        report.append("## 3. 测试场景\n\n");
        report.append(generateTestScenariosSection(reportData));
        
        // 4. 性能测试结果
        report.append("## 4. 性能测试结果\n\n");
        report.append(generateResultsSection(reportData));
        
        // 5. 连接池配置分析
        report.append("## 5. 连接池配置分析\n\n");
        report.append(generateConfigurationAnalysis(reportData));
        
        // 6. 优化建议
        report.append("## 6. 优化建议\n\n");
        report.append(generateOptimizationSuggestions(reportData));
        
        // 7. 结论
        report.append("## 7. 结论\n\n");
        report.append(generateConclusion(reportData));
        
        return report.toString();
    }

    /**
     * 生成执行摘要
     */
    private String generateExecutiveSummary(ReportData reportData) {
        StringBuilder summary = new StringBuilder();
        
        summary.append("### 测试目标\n\n");
        summary.append("本次测试旨在评估AI-Ready项目测试环境数据库连接池（HikariCP）在不同并发场景下的性能表现，")
               .append("确保系统在高并发场景下的稳定性和可靠性。\n\n");
        
        summary.append("### 关键发现\n\n");
        
        // 计算通过率
        int passedTests = 0;
        int totalTests = reportData.getTestResults().size();
        for (TestResultData result : reportData.getTestResults()) {
            if (result.isPassed()) {
                passedTests++;
            }
        }
        double passRate = totalTests > 0 ? (double) passedTests / totalTests * 100 : 0;
        
        summary.append("- **测试通过率**: ").append(String.format("%.1f%%", passRate))
               .append(" (").append(passedTests).append("/").append(totalTests).append(")\n");
        
        // 平均连接获取时间
        double avgAcquisitionTime = reportData.getTestResults().stream()
                .mapToDouble(TestResultData::getAvgAcquisitionTimeMs)
                .average()
                .orElse(0);
        summary.append("- **平均连接获取时间**: ").append(String.format("%.2f ms", avgAcquisitionTime))
               .append(avgAcquisitionTime <= 100 ? " ✅ 符合要求（≤100ms）" : " ❌ 超出阈值").append("\n");
        
        // 平均连接池利用率
        double avgUtilization = reportData.getPoolMetricsList().stream()
                .mapToDouble(PoolMetrics::getPoolUtilization)
                .average()
                .orElse(0);
        summary.append("- **平均连接池利用率**: ").append(String.format("%.2f%%", avgUtilization))
               .append(avgUtilization >= 80 ? " ✅ 符合要求（≥80%）" : " ❌ 低于预期").append("\n");
        
        // 连接泄漏检测
        summary.append("- **连接泄漏**: ").append(reportData.isLeakDetected() ? "❌ 检测到泄漏" : "✅ 无泄漏").append("\n");
        
        summary.append("\n### 总体评估\n\n");
        if (passRate >= 90 && avgAcquisitionTime <= 100 && !reportData.isLeakDetected()) {
            summary.append("✅ **测试通过** - 连接池性能表现良好，符合生产环境要求。\n\n");
        } else if (passRate >= 70) {
            summary.append("⚠️ **测试有条件通过** - 连接池基本可用，但存在优化空间。\n\n");
        } else {
            summary.append("❌ **测试未通过** - 连接池存在严重性能问题，需要优化。\n\n");
        }
        
        return summary.toString();
    }

    /**
     * 生成测试环境章节
     */
    private String generateEnvironmentSection(ReportData reportData) {
        StringBuilder env = new StringBuilder();
        
        env.append("### 硬件环境\n\n");
        env.append("| 项目 | 配置 |\n");
        env.append("|------|------|\n");
        env.append("| CPU | ").append(reportData.getCpuInfo()).append(" |\n");
        env.append("| 内存 | ").append(reportData.getMemoryInfo()).append(" |\n");
        env.append("| 磁盘 | ").append(reportData.getDiskInfo()).append(" |\n");
        env.append("| 网络 | ").append(reportData.getNetworkInfo()).append(" |\n\n");
        
        env.append("### 软件环境\n\n");
        env.append("| 项目 | 版本 |\n");
        env.append("|------|------|\n");
        env.append("| 操作系统 | ").append(reportData.getOsInfo()).append(" |\n");
        env.append("| Java | ").append(reportData.getJavaVersion()).append(" |\n");
        env.append("| Spring Boot | ").append(reportData.getSpringBootVersion()).append(" |\n");
        env.append("| HikariCP | ").append(reportData.getHikariVersion()).append(" |\n");
        env.append("| 数据库 | ").append(reportData.getDatabaseInfo()).append(" |\n\n");
        
        return env.toString();
    }

    /**
     * 生成测试场景章节
     */
    private String generateTestScenariosSection(ReportData reportData) {
        StringBuilder scenarios = new StringBuilder();
        
        scenarios.append("本次测试包含以下场景：\n\n");
        scenarios.append("| 场景 | 并发线程数 | 迭代次数 | 描述 |\n");
        scenarios.append("|------|-----------|---------|------|\n");
        
        for (TestResultData result : reportData.getTestResults()) {
            scenarios.append("| ").append(result.getScenarioName()).append(" | ")
                    .append(result.getThreadCount()).append(" | ")
                    .append(result.getTotalIterations()).append(" | ")
                    .append(result.getDescription()).append(" |\n");
        }
        
        scenarios.append("\n");
        return scenarios.toString();
    }

    /**
     * 生成测试结果章节
     */
    private String generateResultsSection(ReportData reportData) {
        StringBuilder results = new StringBuilder();
        
        // 汇总表
        results.append("### 性能测试汇总\n\n");
        results.append("| 测试场景 | 并发数 | 平均获取时间(ms) | P95(ms) | P99(ms) | 成功率(%) | 吞吐量(ops/s) | 状态 |\n");
        results.append("|---------|-------|-----------------|---------|---------|----------|--------------|------|\n");
        
        for (TestResultData result : reportData.getTestResults()) {
            results.append("| ").append(result.getScenarioName()).append(" | ")
                   .append(result.getThreadCount()).append(" | ")
                   .append(String.format("%.2f", result.getAvgAcquisitionTimeMs())).append(" | ")
                   .append(String.format("%.2f", result.getP95TimeMs())).append(" | ")
                   .append(String.format("%.2f", result.getP99TimeMs())).append(" | ")
                   .append(String.format("%.2f", result.getSuccessRate())).append(" | ")
                   .append(String.format("%.2f", result.getThroughput())).append(" | ")
                   .append(result.isPassed() ? "✅ 通过" : "❌ 失败").append(" |\n");
        }
        
        results.append("\n");
        return results.toString();
    }

    /**
     * 生成配置分析章节
     */
    private String generateConfigurationAnalysis(ReportData reportData) {
        StringBuilder analysis = new StringBuilder();
        
        analysis.append("### 当前连接池配置\n\n");
        analysis.append("```yaml\n");
        analysis.append("# HikariCP配置\n");
        analysis.append("hikari:\n");
        analysis.append("  maximum-pool-size: ").append(reportData.getMaxPoolSize()).append("\n");
        analysis.append("  minimum-idle: ").append(reportData.getMinIdle()).append("\n");
        analysis.append("  connection-timeout: ").append(reportData.getConnectionTimeout()).append("ms\n");
        analysis.append("  idle-timeout: ").append(reportData.getIdleTimeout()).append("ms\n");
        analysis.append("  max-lifetime: ").append(reportData.getMaxLifetime()).append("ms\n");
        analysis.append("  leak-detection-threshold: ").append(reportData.getLeakDetectionThreshold()).append("ms\n");
        analysis.append("  connection-test-query: SELECT 1\n");
        analysis.append("```\n\n");
        
        analysis.append("### 配置分析\n\n");
        analysis.append("1. **连接池大小**: ")
               .append(reportData.getMaxPoolSize() >= 50 ? "✅ 配置合理" : "⚠️ 建议增加")
               .append("\n\n");
        analysis.append("2. **连接超时**: ")
               .append(reportData.getConnectionTimeout() <= 30000 ? "✅ 配置合理" : "⚠️ 建议缩短")
               .append("\n\n");
        analysis.append("3. **空闲超时**: ")
               .append(reportData.getIdleTimeout() >= 300000 ? "✅ 配置合理" : "⚠️ 建议增加")
               .append("\n\n");
        analysis.append("4. **最大生命周期**: ")
               .append(reportData.getMaxLifetime() >= 1800000 ? "✅ 配置合理" : "⚠️ 建议增加")
               .append("\n\n");
        analysis.append("5. **泄漏检测**: ")
               .append(reportData.getLeakDetectionThreshold() > 0 ? "✅ 已启用" : "❌ 未启用")
               .append("\n\n");
        
        return analysis.toString();
    }

    /**
     * 生成优化建议章节
     */
    private String generateOptimizationSuggestions(ReportData reportData) {
        StringBuilder suggestions = new StringBuilder();
        
        suggestions.append("### 配置优化建议\n\n");
        
        // 根据测试结果生成建议
        double avgAcquisitionTime = reportData.getTestResults().stream()
                .mapToDouble(TestResultData::getAvgAcquisitionTimeMs)
                .average()
                .orElse(0);
        
        if (avgAcquisitionTime > 50) {
            suggestions.append("1. **连接获取时间优化**\n\n");
            suggestions.append("   - 当前平均获取时间较长（").append(String.format("%.2f ms", avgAcquisitionTime)).append("）\n");
            suggestions.append("   - 建议增加 `minimum-idle` 值，保持更多空闲连接\n");
            suggestions.append("   - 考虑增加 `maximum-pool-size` 以应对突发流量\n\n");
        }
        
        double avgUtilization = reportData.getPoolMetricsList().stream()
                .mapToDouble(PoolMetrics::getPoolUtilization)
                .average()
                .orElse(0);
        
        if (avgUtilization < 80) {
            suggestions.append("2. **连接池利用率优化**\n\n");
            suggestions.append("   - 当前平均利用率较低（").append(String.format("%.2f%%", avgUtilization)).append("）\n");
            suggestions.append("   - 可以适当减少 `maximum-pool-size` 以节省资源\n");
            suggestions.append("   - 或增加并发负载以提高利用率\n\n");
        } else if (avgUtilization > 95) {
            suggestions.append("2. **连接池利用率优化**\n\n");
            suggestions.append("   - 当前平均利用率过高（").append(String.format("%.2f%%", avgUtilization)).append("）\n");
            suggestions.append("   - 建议增加 `maximum-pool-size` 以避免连接耗尽\n");
            suggestions.append("   - 考虑优化应用代码，减少连接持有时间\n\n");
        }
        
        suggestions.append("3. **通用优化建议**\n\n");
        suggestions.append("   - 启用连接池监控（Micrometer + Prometheus）\n");
        suggestions.append("   - 设置合理的 `leak-detection-threshold` 检测连接泄漏\n");
        suggestions.append("   - 定期审查慢查询，优化SQL性能\n");
        suggestions.append("   - 考虑使用连接池预热策略\n\n");
        
        return suggestions.toString();
    }

    /**
     * 生成结论章节
     */
    private String generateConclusion(ReportData reportData) {
        StringBuilder conclusion = new StringBuilder();
        
        // 计算统计数据
        int totalTests = reportData.getTestResults().size();
        int passedTests = (int) reportData.getTestResults().stream().filter(TestResultData::isPassed).count();
        double passRate = totalTests > 0 ? (double) passedTests / totalTests * 100 : 0;
        
        conclusion.append("### 测试结果总结\n\n");
        conclusion.append("- **总测试数**: ").append(totalTests).append("\n");
        conclusion.append("- **通过测试数**: ").append(passedTests).append("\n");
        conclusion.append("- **失败测试数**: ").append(totalTests - passedTests).append("\n");
        conclusion.append("- **通过率**: ").append(String.format("%.1f%%", passRate)).append("\n\n");
        
        conclusion.append("### 最终结论\n\n");
        if (passRate >= 90 && !reportData.isLeakDetected()) {
            conclusion.append("✅ **测试通过**\n\n");
            conclusion.append("数据库连接池性能表现优秀，所有关键指标均符合预期。连接池配置合理，能够稳定应对高并发场景。");
        } else if (passRate >= 70) {
            conclusion.append("⚠️ **测试有条件通过**\n\n");
            conclusion.append("数据库连接池基本满足需求，但存在优化空间。建议按照优化建议章节进行调整。");
        } else {
            conclusion.append("❌ **测试未通过**\n\n");
            conclusion.append("数据库连接池存在严重性能问题，需要立即优化。请优先处理连接获取时间和连接泄漏问题。");
        }
        
        conclusion.append("\n\n---\n\n");
        conclusion.append("*报告生成时间: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("*\n");
        
        return conclusion.toString();
    }

    /**
     * 保存报告到文件
     */
    public void saveReportToFile(String reportContent, String filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.print(reportContent);
        }
        log.info("性能测试报告已保存到: {}", filePath);
    }

    // ========== 数据类 ==========
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportData {
        // 基本信息
        private String projectName;
        private String environment;
        private String tester;
        
        // 环境信息
        private String cpuInfo;
        private String memoryInfo;
        private String diskInfo;
        private String networkInfo;
        private String osInfo;
        private String javaVersion;
        private String springBootVersion;
        private String hikariVersion;
        private String databaseInfo;
        
        // 配置信息
        private int maxPoolSize;
        private int minIdle;
        private long connectionTimeout;
        private long idleTimeout;
        private long maxLifetime;
        private long leakDetectionThreshold;
        
        // 测试结果
        private List<TestResultData> testResults;
        private List<PoolMetrics> poolMetricsList;
        private boolean leakDetected;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestResultData {
        private String scenarioName;
        private String description;
        private int threadCount;
        private int totalIterations;
        private int successCount;
        private int failureCount;
        private double avgAcquisitionTimeMs;
        private double minAcquisitionTimeMs;
        private double maxAcquisitionTimeMs;
        private double p95TimeMs;
        private double p99TimeMs;
        private double successRate;
        private double throughput;
        private long totalTestTimeMs;
        private boolean passed;
        private PoolMetrics poolMetrics;
    }
}