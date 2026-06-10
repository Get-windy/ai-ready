package cn.aiedge.architecture.checker.rules;

import cn.aiedge.architecture.checker.core.CheckRule;
import cn.aiedge.architecture.checker.model.CheckResult;
import cn.aiedge.architecture.checker.model.ProjectContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 目录结构检查规则
 * 
 * <p>检查项目目录结构是否符合架构规范</p>
 * 
 * @author Architecture Team
 * @version 1.0.0
 * @since 2026-05-05
 */
@Slf4j
@Component
public class DirectoryStructureRule implements CheckRule {

    /**
     * 规则ID
     */
    public static final String RULE_ID = "directory-structure-001";
    
    /**
     * 规则名称
     */
    public static final String RULE_NAME = "目录结构合规性检查";
    
    /**
     * 规则描述
     */
    public static final String RULE_DESCRIPTION = "检查项目目录结构是否符合架构规范，包括模块位置、目录命名等";
    
    /**
     * 规则类别
     */
    public static final String RULE_CATEGORY = "directory";
    
    /**
     * 规则优先级
     */
    public static final int RULE_PRIORITY = 100;
    
    /**
     * 规则严重级别
     */
    public static final Severity RULE_SEVERITY = Severity.ERROR;

    @Override
    public String getId() {
        return RULE_ID;
    }

    @Override
    public String getName() {
        return RULE_NAME;
    }

    @Override
    public String getDescription() {
        return RULE_DESCRIPTION;
    }

    @Override
    public String getCategory() {
        return RULE_CATEGORY;
    }

    @Override
    public Severity getSeverity() {
        return RULE_SEVERITY;
    }

    @Override
    public int getPriority() {
        return RULE_PRIORITY;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public CheckResult check(ProjectContext context) {
        log.info("开始执行目录结构检查: {}", context.getProjectInfo().getName());
        
        long startTime = System.currentTimeMillis();
        List<CheckResult.Issue> issues = new ArrayList<>();
        
        try {
            Path projectRoot = context.getProjectRoot();
            
            // 检查根目录下不允许直接存在业务模块
            issues.addAll(checkRootDirectory(projectRoot));
            
            // 检查模块目录位置
            issues.addAll(checkModuleLocations(projectRoot));
            
            // 检查目录命名规范
            issues.addAll(checkDirectoryNaming(projectRoot));
            
            // 检查重复目录
            issues.addAll(checkDuplicateDirectories(projectRoot));
            
            // 检查目录层次结构
            issues.addAll(checkDirectoryHierarchy(projectRoot));
            
        } catch (Exception e) {
            log.error("目录结构检查失败", e);
            return CheckResult.builder()
                .checkId(generateCheckId())
                .projectContext(context)
                .ruleId(getId())
                .ruleName(getName())
                .ruleCategory(getCategory())
                .ruleSeverity(getSeverity())
                .startTime(java.time.LocalDateTime.now())
                .endTime(java.time.LocalDateTime.now())
                .durationMs(System.currentTimeMillis() - startTime)
                .status(CheckResult.CheckStatus.FAILED)
                .issues(issues)
                .errorMessage("目录结构检查失败: " + e.getMessage())
                .stackTrace(getStackTrace(e))
                .build();
        }
        
        long endTime = System.currentTimeMillis();
        
        return CheckResult.builder()
            .checkId(generateCheckId())
            .projectContext(context)
            .ruleId(getId())
            .ruleName(getName())
            .ruleCategory(getCategory())
            .ruleSeverity(getSeverity())
            .startTime(java.time.LocalDateTime.now())
            .endTime(java.time.LocalDateTime.now())
            .durationMs(endTime - startTime)
            .status(CheckResult.CheckStatus.SUCCESS)
            .issues(issues)
            .statistics(CheckResult.Statistics.builder()
                .totalFiles(countFiles(context))
                .totalIssues(issues.size())
                .errorIssues((int) issues.stream()
                    .filter(issue -> issue.getSeverity() == Severity.ERROR)
                    .count())
                .warningIssues((int) issues.stream()
                    .filter(issue -> issue.getSeverity() == Severity.WARNING)
                    .count())
                .infoIssues((int) issues.stream()
                    .filter(issue -> issue.getSeverity() == Severity.INFO)
                    .count())
                .build())
            .build();
    }

    @Override
    public String getFixSuggestion(ProjectContext context) {
        return """
            目录结构修复建议：
            1. 业务模块必须放在正确的目录下（backend/erp/, backend/ai/, backend/crm/等）
            2. 测试代码必须放在tests/目录下
            3. 避免在根目录下创建业务模块目录
            4. 目录命名使用小写字母和连字符，避免中文和特殊字符
            5. 遵循项目的目录层次结构规范
            """;
    }

    /**
     * 检查根目录
     */
    private List<CheckResult.Issue> checkRootDirectory(Path projectRoot) throws IOException {
        List<CheckResult.Issue> issues = new ArrayList<>();
        
        // 根目录下不应该有业务模块目录
        List<String> disallowedRootDirs = List.of(
            "purchase", "sale", "stock", "order", "invoice", 
            "customer", "supplier", "product", "user", "auth"
        );
        
        Files.list(projectRoot)
            .filter(Files::isDirectory)
            .forEach(dir -> {
                String dirName = dir.getFileName().toString();
                if (disallowedRootDirs.contains(dirName.toLowerCase())) {
                    issues.add(createIssue(
                        "业务模块在根目录",
                        Severity.ERROR,
                        "业务模块 '" + dirName + "' 不应直接放在根目录下",
                        "根据架构规范，业务模块应该放在对应的模块目录下（如backend/erp/）",
                        "将目录移动到正确的模块目录下",
                        CheckResult.Issue.Location.builder()
                            .filePath(dirName)
                            .line(0)
                            .column(0)
                            .build()
                    ));
                }
            });
        
        return issues;
    }

    /**
     * 检查模块目录位置
     */
    private List<CheckResult.Issue> checkModuleLocations(Path projectRoot) throws IOException {
        List<CheckResult.Issue> issues = new ArrayList<>();
        
        // 检查backend目录下的模块位置
        Path backendDir = projectRoot.resolve("backend");
        if (Files.exists(backendDir) && Files.isDirectory(backendDir)) {
            
            // 检查backend目录下的子目录
            Files.list(backendDir)
                .filter(Files::isDirectory)
                .forEach(dir -> {
                    String dirName = dir.getFileName().toString();
                    
                    // 检查是否符合允许的模块类型
                    List<String> allowedModules = List.of("core", "erp", "ai", "crm", "user", "tests", "scripts", "docs");
                    if (!allowedModules.contains(dirName)) {
                        issues.add(createIssue(
                            "未识别的模块目录",
                            Severity.WARNING,
                            "目录 '" + dirName + "' 不是标准的模块目录",
                            "建议使用标准模块目录名称：core, erp, ai, crm, user, tests, scripts, docs",
                            "考虑重命名目录或移动到正确的位置",
                            CheckResult.Issue.Location.builder()
                                .filePath("backend/" + dirName)
                                .line(0)
                                .column(0)
                                .build()
                        ));
                    }
                });
        }
        
        return issues;
    }

    /**
     * 检查目录命名规范
     */
    private List<CheckResult.Issue> checkDirectoryNaming(Path currentDir) throws IOException {
        List<CheckResult.Issue> issues = new ArrayList<>();
        
        if (Files.isDirectory(currentDir)) {
            Files.list(currentDir)
                .filter(Files::isDirectory)
                .forEach(dir -> {
                    String dirName = dir.getFileName().toString();
                    
                    // 检查命名规范：只允许小写字母、数字、连字符
                    if (!dirName.matches("^[a-z0-9-]+$")) {
                        issues.add(createIssue(
                            "目录命名不规范",
                            Severity.WARNING,
                            "目录 '" + dirName + "' 命名不符合规范",
                            "目录名应该只包含小写字母、数字和连字符，避免大写字母、中文和特殊字符",
                            "将目录重命名为符合规范的名称",
                            CheckResult.Issue.Location.builder()
                                .filePath(projectRoot.relativize(dir).toString())
                                .line(0)
                                .column(0)
                                .build()
                        ));
                    }
                    
                    // 递归检查子目录
                    try {
                        issues.addAll(checkDirectoryNaming(dir));
                    } catch (IOException e) {
                        log.warn("检查目录命名失败: {}", dir, e);
                    }
                });
        }
        
        return issues;
    }

    /**
     * 检查重复目录
     */
    private List<CheckResult.Issue> checkDuplicateDirectories(Path projectRoot) {
        // 简化的重复目录检查逻辑
        List<CheckResult.Issue> issues = new ArrayList<>();
        // 实际实现需要遍历所有目录并检查重复
        return issues;
    }

    /**
     * 检查目录层次结构
     */
    private List<CheckResult.Issue> checkDirectoryHierarchy(Path projectRoot) {
        // 简化的目录层次结构检查逻辑
        List<CheckResult.Issue> issues = new ArrayList<>();
        // 实际实现需要检查目录层次结构是否符合规范
        return issues;
    }

    /**
     * 创建问题实例
     */
    private CheckResult.Issue createIssue(
            String type, 
            Severity severity, 
            String description, 
            String cause, 
            String fixSuggestion,
            CheckResult.Issue.Location location) {
        
        return CheckResult.Issue.builder()
            .issueId(generateIssueId(type, location))
            .type(type)
            .severity(severity)
            .location(location)
            .description(description)
            .cause(cause)
            .fixSuggestion(fixSuggestion)
            .fixDifficulty(3) // 中等难度
            .fixPriority(severity == Severity.ERROR ? 1 : 3)
            .fixed(false)
            .metadata(CheckResult.Issue.Metadata.builder()
                .createdAt(java.time.LocalDateTime.now())
                .tags(List.of("directory", "structure"))
                .source("automated")
                .confidence(0.9)
                .autoGenerated(true)
                .build())
            .build();
    }

    /**
     * 生成检查ID
     */
    private String generateCheckId() {
        return "check_" + System.currentTimeMillis() + "_" + RULE_ID;
    }

    /**
     * 生成问题ID
     */
    private String generateIssueId(String type, CheckResult.Issue.Location location) {
        return "issue_" + type.hashCode() + "_" + 
               (location.getFilePath() != null ? location.getFilePath().hashCode() : 0) + "_" +
               System.currentTimeMillis();
    }

    /**
     * 获取堆栈跟踪
     */
    private String getStackTrace(Exception e) {
        log.error("获取堆栈跟踪失败, rule={}", RULE_NAME, e);
        return e.getMessage() != null ? e.getMessage() : e.getClass().getName();
    }

    /**
     * 计算文件数量（简化实现）
     */
    private int countFiles(ProjectContext context) {
        return context.getFiles() != null ? context.getFiles().size() : 0;
    }

    // 临时变量，实际实现中需要从上下文获取
    private Path projectRoot;
}