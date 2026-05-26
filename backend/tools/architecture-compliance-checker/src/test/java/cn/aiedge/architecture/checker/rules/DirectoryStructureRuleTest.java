package cn.aiedge.architecture.checker.rules;

import cn.aiedge.architecture.checker.model.CheckResult;
import cn.aiedge.architecture.checker.model.ProjectContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 目录结构检查规则测试
 * 
 * @author Architecture Team
 * @version 1.0.0
 * @since 2026-05-05
 */
class DirectoryStructureRuleTest {

    @TempDir
    Path tempDir;
    
    private DirectoryStructureRule rule;
    
    @BeforeEach
    void setUp() {
        rule = new DirectoryStructureRule();
    }
    
    @Test
    void testRuleProperties() {
        assertEquals(DirectoryStructureRule.RULE_ID, rule.getId());
        assertEquals(DirectoryStructureRule.RULE_NAME, rule.getName());
        assertEquals(DirectoryStructureRule.RULE_DESCRIPTION, rule.getDescription());
        assertEquals(DirectoryStructureRule.RULE_CATEGORY, rule.getCategory());
        assertEquals(DirectoryStructureRule.RULE_SEVERITY, rule.getSeverity());
        assertEquals(DirectoryStructureRule.RULE_PRIORITY, rule.getPriority());
        assertTrue(rule.isEnabled());
    }
    
    @Test
    void testCheckWithValidDirectoryStructure() throws Exception {
        // 创建有效的目录结构
        Files.createDirectories(tempDir.resolve("backend/erp/purchase"));
        Files.createDirectories(tempDir.resolve("backend/core/common"));
        Files.createDirectories(tempDir.resolve("backend/tests/unit"));
        
        ProjectContext context = ProjectContext.builder()
            .projectInfo(ProjectContext.ProjectInfo.builder()
                .name("test-project")
                .type("java")
                .version("1.0.0")
                .build())
            .projectRoot(tempDir)
            .build();
        
        CheckResult result = rule.check(context);
        
        assertNotNull(result);
        assertEquals(CheckResult.CheckStatus.SUCCESS, result.getStatus());
        assertNotNull(result.getIssues());
        // 有效结构应该没有错误问题
        long errorCount = result.getIssues().stream()
            .filter(issue -> issue.getSeverity() == DirectoryStructureRule.RULE_SEVERITY.ERROR)
            .count();
        assertEquals(0, errorCount, "有效目录结构不应该有错误级别问题");
    }
    
    @Test
    void testCheckWithInvalidRootDirectory() throws Exception {
        // 创建无效的目录结构：业务模块在根目录
        Files.createDirectories(tempDir.resolve("purchase"));
        Files.createDirectories(tempDir.resolve("sale"));
        
        ProjectContext context = ProjectContext.builder()
            .projectInfo(ProjectContext.ProjectInfo.builder()
                .name("test-project")
                .type("java")
                .version("1.0.0")
                .build())
            .projectRoot(tempDir)
            .build();
        
        CheckResult result = rule.check(context);
        
        assertNotNull(result);
        assertEquals(CheckResult.CheckStatus.SUCCESS, result.getStatus());
        assertNotNull(result.getIssues());
        
        // 应该检测到业务模块在根目录的问题
        List<CheckResult.Issue> rootDirIssues = result.getIssues().stream()
            .filter(issue -> "业务模块在根目录".equals(issue.getType()))
            .toList();
        
        assertFalse(rootDirIssues.isEmpty(), "应该检测到业务模块在根目录的问题");
        assertTrue(rootDirIssues.size() >= 2, "应该检测到至少2个根目录问题");
    }
    
    @Test
    void testCheckWithInvalidModuleDirectory() throws Exception {
        // 创建无效的模块目录
        Files.createDirectories(tempDir.resolve("backend/invalid-module"));
        
        ProjectContext context = ProjectContext.builder()
            .projectInfo(ProjectContext.ProjectInfo.builder()
                .name("test-project")
                .type("java")
                .version("1.0.0")
                .build())
            .projectRoot(tempDir)
            .build();
        
        CheckResult result = rule.check(context);
        
        assertNotNull(result);
        assertEquals(CheckResult.CheckStatus.SUCCESS, result.getStatus());
        assertNotNull(result.getIssues());
        
        // 应该检测到未识别的模块目录问题
        List<CheckResult.Issue> moduleIssues = result.getIssues().stream()
            .filter(issue -> "未识别的模块目录".equals(issue.getType()))
            .toList();
        
        assertFalse(moduleIssues.isEmpty(), "应该检测到未识别的模块目录问题");
    }
    
    @Test
    void testCheckWithInvalidDirectoryNaming() throws Exception {
        // 创建命名不规范的目录
        Files.createDirectories(tempDir.resolve("backend/ERP")); // 大写
        Files.createDirectories(tempDir.resolve("backend/my_module")); // 下划线
        Files.createDirectories(tempDir.resolve("backend/测试目录")); // 中文
        
        ProjectContext context = ProjectContext.builder()
            .projectInfo(ProjectContext.ProjectInfo.builder()
                .name("test-project")
                .type("java")
                .version("1.0.0")
                .build())
            .projectRoot(tempDir)
            .build();
        
        CheckResult result = rule.check(context);
        
        assertNotNull(result);
        assertEquals(CheckResult.CheckStatus.SUCCESS, result.getStatus());
        assertNotNull(result.getIssues());
        
        // 应该检测到目录命名不规范的问题
        List<CheckResult.Issue> namingIssues = result.getIssues().stream()
            .filter(issue -> "目录命名不规范".equals(issue.getType()))
            .toList();
        
        assertFalse(namingIssues.isEmpty(), "应该检测到目录命名不规范的问题");
    }
    
    @Test
    void testCheckWithNonExistentDirectory() throws Exception {
        // 使用不存在的目录
        Path nonExistentDir = tempDir.resolve("non-existent");
        
        ProjectContext context = ProjectContext.builder()
            .projectInfo(ProjectContext.ProjectInfo.builder()
                .name("test-project")
                .type("java")
                .version("1.0.0")
                .build())
            .projectRoot(nonExistentDir)
            .build();
        
        CheckResult result = rule.check(context);
        
        assertNotNull(result);
        // 由于目录不存在，检查应该失败
        assertEquals(CheckResult.CheckStatus.FAILED, result.getStatus());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("失败"), "应该返回失败信息");
    }
    
    @Test
    void testGetFixSuggestion() {
        ProjectContext context = ProjectContext.builder()
            .projectInfo(ProjectContext.ProjectInfo.builder()
                .name("test-project")
                .type("java")
                .version("1.0.0")
                .build())
            .projectRoot(tempDir)
            .build();
        
        String suggestion = rule.getFixSuggestion(context);
        
        assertNotNull(suggestion);
        assertFalse(suggestion.isEmpty());
        assertTrue(suggestion.contains("目录结构修复建议"), "应该包含修复建议标题");
        assertTrue(suggestion.contains("业务模块必须放在正确的目录下"), "应该包含具体的修复建议");
    }
    
    @Test
    void testCheckResultStructure() throws Exception {
        Files.createDirectories(tempDir.resolve("backend/erp/purchase"));
        
        ProjectContext context = ProjectContext.builder()
            .projectInfo(ProjectContext.ProjectInfo.builder()
                .name("test-project")
                .type("java")
                .version("1.0.0")
                .build())
            .projectRoot(tempDir)
            .build();
        
        CheckResult result = rule.check(context);
        
        // 验证检查结果结构
        assertNotNull(result.getCheckId());
        assertNotNull(result.getStartTime());
        assertNotNull(result.getEndTime());
        assertTrue(result.getDurationMs() >= 0);
        assertEquals(DirectoryStructureRule.RULE_ID, result.getRuleId());
        assertEquals(DirectoryStructureRule.RULE_NAME, result.getRuleName());
        assertEquals(DirectoryStructureRule.RULE_CATEGORY, result.getRuleCategory());
        assertEquals(DirectoryStructureRule.RULE_SEVERITY, result.getRuleSeverity());
        assertNotNull(result.getIssues());
        assertNotNull(result.getStatistics());
        
        // 验证统计信息
        CheckResult.Statistics stats = result.getStatistics();
        assertTrue(stats.getTotalIssues() >= 0);
        assertTrue(stats.getErrorIssues() >= 0);
        assertTrue(stats.getWarningIssues() >= 0);
        assertTrue(stats.getInfoIssues() >= 0);
    }
    
    @Test
    void testIssueStructure() throws Exception {
        Files.createDirectories(tempDir.resolve("purchase")); // 创建根目录下的业务模块
        
        ProjectContext context = ProjectContext.builder()
            .projectInfo(ProjectContext.ProjectInfo.builder()
                .name("test-project")
                .type("java")
                .version("1.0.0")
                .build())
            .projectRoot(tempDir)
            .build();
        
        CheckResult result = rule.check(context);
        
        assertFalse(result.getIssues().isEmpty(), "应该至少有一个问题");
        
        CheckResult.Issue issue = result.getIssues().get(0);
        
        // 验证问题结构
        assertNotNull(issue.getIssueId());
        assertNotNull(issue.getType());
        assertNotNull(issue.getSeverity());
        assertNotNull(issue.getDescription());
        assertNotNull(issue.getCause());
        assertNotNull(issue.getFixSuggestion());
        assertTrue(issue.getFixDifficulty() >= 1 && issue.getFixDifficulty() <= 5);
        assertTrue(issue.getFixPriority() >= 1 && issue.getFixPriority() <= 5);
        assertFalse(issue.isFixed());
        
        // 验证位置信息
        CheckResult.Issue.Location location = issue.getLocation();
        assertNotNull(location);
        assertNotNull(location.getFilePath());
        assertTrue(location.getLine() >= 0);
        assertTrue(location.getColumn() >= 0);
        
        // 验证元数据
        CheckResult.Issue.Metadata metadata = issue.getMetadata();
        assertNotNull(metadata);
        assertNotNull(metadata.getCreatedAt());
        assertNotNull(metadata.getTags());
        assertTrue(metadata.getConfidence() >= 0.0 && metadata.getConfidence() <= 1.0);
        assertTrue(metadata.isAutoGenerated());
    }
}