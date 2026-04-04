package cn.aiedge.notification.controller;

import cn.aiedge.notification.cache.NotificationTemplateCache;
import cn.aiedge.notification.entity.NotificationTemplate;
import cn.aiedge.notification.service.NotificationService;
import cn.aiedge.notification.template.TemplateRenderer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通知模板管理控制器
 * 提供模板的CRUD、预览、变量校验等增强功能
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
@RestController
@RequestMapping("/api/notification-templates")
@RequiredArgsConstructor
@Tag(name = "通知模板管理", description = "模板CRUD与预览接口")
public class NotificationTemplateController {

    private final NotificationService notificationService;
    private final NotificationTemplateCache templateCache;
    private final TemplateRenderer templateRenderer;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    // ==================== 模板CRUD ====================

    @PostMapping
    @Operation(summary = "创建通知模板")
    public ResponseEntity<NotificationTemplate> createTemplate(@RequestBody NotificationTemplate template) {
        // 校验模板编码唯一性
        if (notificationService.getTemplateByCode(template.getTemplateCode()) != null) {
            return ResponseEntity.badRequest().build();
        }
        
        // 自动提取变量定义
        if (template.getVariables() == null || template.getVariables().isEmpty()) {
            template.setVariables(extractVariablesJson(template.getTitle(), template.getContent()));
        }
        
        return ResponseEntity.ok(notificationService.createTemplate(template));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新通知模板")
    public ResponseEntity<NotificationTemplate> updateTemplate(
            @PathVariable Long id, @RequestBody NotificationTemplate template) {
        
        NotificationTemplate existing = notificationService.getTemplate(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        
        template.setId(id);
        
        // 更新变量定义
        template.setVariables(extractVariablesJson(template.getTitle(), template.getContent()));
        
        return ResponseEntity.ok(notificationService.updateTemplate(template));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知模板")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        notificationService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取模板详情")
    public ResponseEntity<NotificationTemplate> getTemplate(@PathVariable Long id) {
        NotificationTemplate template = notificationService.getTemplate(id);
        return template != null ? ResponseEntity.ok(template) : ResponseEntity.notFound().build();
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "根据编码获取模板")
    public ResponseEntity<NotificationTemplate> getTemplateByCode(@PathVariable String code) {
        NotificationTemplate template = notificationService.getTemplateByCode(code);
        return template != null ? ResponseEntity.ok(template) : ResponseEntity.notFound().build();
    }

    @GetMapping
    @Operation(summary = "获取所有模板")
    public ResponseEntity<List<NotificationTemplate>> getAllTemplates(
            @RequestParam(required = false) String notifyType) {
        
        List<NotificationTemplate> templates;
        if (notifyType != null && !notifyType.isEmpty()) {
            templates = notificationService.getTemplatesByType(notifyType);
        } else {
            templates = notificationService.getAllTemplates();
        }
        return ResponseEntity.ok(templates);
    }

    // ==================== 模板增强功能 ====================

    /**
     * 预览模板渲染结果
     */
    @PostMapping("/{id}/preview")
    @Operation(summary = "预览模板渲染")
    public ResponseEntity<TemplatePreviewResult> previewTemplate(
            @PathVariable Long id, @RequestBody Map<String, Object> variables) {
        
        NotificationTemplate template = notificationService.getTemplate(id);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }

        String title = templateRenderer.render(template.getTitle(), variables);
        String content = templateRenderer.render(template.getContent(), variables);

        return ResponseEntity.ok(new TemplatePreviewResult(
            template.getTemplateCode(),
            template.getNotifyType(),
            title,
            content,
            variables
        ));
    }

    /**
     * 根据编码预览模板
     */
    @PostMapping("/code/{code}/preview")
    @Operation(summary = "根据编码预览模板")
    public ResponseEntity<TemplatePreviewResult> previewTemplateByCode(
            @PathVariable String code, @RequestBody Map<String, Object> variables) {
        
        NotificationTemplate template = notificationService.getTemplateByCode(code);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }

        String title = templateRenderer.render(template.getTitle(), variables);
        String content = templateRenderer.render(template.getContent(), variables);

        return ResponseEntity.ok(new TemplatePreviewResult(
            template.getTemplateCode(),
            template.getNotifyType(),
            title,
            content,
            variables
        ));
    }

    /**
     * 校验模板变量
     */
    @PostMapping("/{id}/validate")
    @Operation(summary = "校验模板变量")
    public ResponseEntity<ValidationResult> validateVariables(
            @PathVariable Long id, @RequestBody Map<String, Object> variables) {
        
        NotificationTemplate template = notificationService.getTemplate(id);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }

        // 提取模板中的变量
        Set<String> requiredVars = extractVariables(template.getTitle(), template.getContent());
        Set<String> providedVars = variables != null ? variables.keySet() : Collections.emptySet();

        // 找出缺失的变量
        Set<String> missingVars = new TreeSet<>(requiredVars);
        missingVars.removeAll(providedVars);

        // 找出多余的变量
        Set<String> extraVars = new TreeSet<>(providedVars);
        extraVars.removeAll(requiredVars);

        boolean valid = missingVars.isEmpty();

        return ResponseEntity.ok(new ValidationResult(
            valid,
            requiredVars,
            missingVars,
            extraVars
        ));
    }

    /**
     * 提取模板变量列表
     */
    @GetMapping("/{id}/variables")
    @Operation(summary = "提取模板变量")
    public ResponseEntity<Set<String>> extractTemplateVariables(@PathVariable Long id) {
        NotificationTemplate template = notificationService.getTemplate(id);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }

        Set<String> variables = extractVariables(template.getTitle(), template.getContent());
        return ResponseEntity.ok(variables);
    }

    /**
     * 获取缓存统计
     */
    @GetMapping("/cache/stats")
    @Operation(summary = "获取模板缓存统计")
    public ResponseEntity<NotificationTemplateCache.CacheStats> getCacheStats() {
        return ResponseEntity.ok(templateCache.getStats());
    }

    /**
     * 刷新模板缓存
     */
    @PostMapping("/cache/refresh")
    @Operation(summary = "刷新模板缓存")
    public ResponseEntity<Void> refreshCache() {
        templateCache.refreshCache();
        return ResponseEntity.ok().build();
    }

    // ==================== 辅助方法 ====================

    private Set<String> extractVariables(String title, String content) {
        Set<String> variables = new LinkedHashSet<>();
        
        if (title != null) {
            Matcher matcher = VARIABLE_PATTERN.matcher(title);
            while (matcher.find()) {
                variables.add(matcher.group(1).trim());
            }
        }
        
        if (content != null) {
            Matcher matcher = VARIABLE_PATTERN.matcher(content);
            while (matcher.find()) {
                variables.add(matcher.group(1).trim());
            }
        }
        
        return variables;
    }

    private String extractVariablesJson(String title, String content) {
        Set<String> variables = extractVariables(title, content);
        if (variables.isEmpty()) {
            return "[]";
        }
        
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (String var : variables) {
            if (!first) json.append(",");
            json.append("\"").append(var).append("\"");
            first = false;
        }
        json.append("]");
        return json.toString();
    }

    // ==================== DTO ====================

    public record TemplatePreviewResult(
        String templateCode,
        String notifyType,
        String title,
        String content,
        Map<String, Object> usedVariables
    ) {}

    public record ValidationResult(
        boolean valid,
        Set<String> requiredVariables,
        Set<String> missingVariables,
        Set<String> extraVariables
    ) {}
}
