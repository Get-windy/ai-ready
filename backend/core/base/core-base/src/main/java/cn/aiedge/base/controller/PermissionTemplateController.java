package cn.aiedge.base.controller;

import cn.aiedge.base.entity.PermissionTemplate;
import cn.aiedge.base.service.PermissionTemplateService;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限模板控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "权限模板管理", description = "权限模板CRUD及应用接口")
@RestController
@RequestMapping("/api/permission-template")
@RequiredArgsConstructor
public class PermissionTemplateController {

    private final PermissionTemplateService permissionTemplateService;

    /**
     * 创建权限模板
     */
    @Operation(summary = "创建权限模板")
    @PostMapping
    @SaCheckPermission("permission-template:create")
    public Result<Long> createTemplate(@RequestBody PermissionTemplate template) {
        Long templateId = permissionTemplateService.createTemplate(template);
        return Result.ok("创建成功", templateId);
    }

    /**
     * 更新权限模板
     */
    @Operation(summary = "更新权限模板")
    @PutMapping("/{id}")
    @SaCheckPermission("permission-template:update")
    public Result<Void> updateTemplate(@PathVariable Long id, @RequestBody PermissionTemplate template) {
        template.setId(id);
        permissionTemplateService.updateTemplate(template);
        return Result.ok("更新成功", null);
    }

    /**
     * 删除权限模板
     */
    @Operation(summary = "删除权限模板")
    @DeleteMapping("/{id}")
    @SaCheckPermission("permission-template:delete")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        permissionTemplateService.deleteTemplate(id);
        return Result.ok("删除成功", null);
    }

    /**
     * 分页查询权限模板
     */
    @Operation(summary = "分页查询权限模板")
    @GetMapping("/page")
    @SaCheckPermission("permission-template:list")
    public Result<Page<PermissionTemplate>> pageTemplates(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam Long tenantId,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) Integer templateType,
            @RequestParam(required = false) Integer status) {
        Page<PermissionTemplate> page = new Page<>(current, size);
        Page<PermissionTemplate> result = permissionTemplateService.pageTemplates(page, tenantId, templateName, templateType, status);
        return Result.ok(result);
    }

    /**
     * 获取权限模板详情
     */
    @Operation(summary = "获取权限模板详情")
    @GetMapping("/{id}")
    @SaCheckPermission("permission-template:detail")
    public Result<PermissionTemplate> getTemplateDetail(@PathVariable Long id) {
        PermissionTemplate template = permissionTemplateService.getTemplateDetail(id);
        return Result.ok(template);
    }

    /**
     * 应用权限模板到角色
     */
    @Operation(summary = "应用权限模板到角色")
    @PostMapping("/apply-to-role")
    @SaCheckPermission("permission-template:apply")
    public Result<Void> applyTemplateToRole(@RequestParam Long templateId, @RequestParam Long roleId) {
        permissionTemplateService.applyTemplateToRole(templateId, roleId);
        return Result.ok("应用成功", null);
    }

    /**
     * 应用权限模板到用户
     */
    @Operation(summary = "应用权限模板到用户")
    @PostMapping("/apply-to-user")
    @SaCheckPermission("permission-template:apply")
    public Result<Void> applyTemplateToUser(@RequestParam Long templateId, @RequestParam Long userId) {
        permissionTemplateService.applyTemplateToUser(templateId, userId);
        return Result.ok("应用成功", null);
    }

    /**
     * 获取系统默认模板
     */
    @Operation(summary = "获取系统默认模板")
    @GetMapping("/system-templates")
    @SaCheckPermission("permission-template:list")
    public Result<List<PermissionTemplate>> getSystemTemplates() {
        List<PermissionTemplate> templates = permissionTemplateService.getSystemTemplates();
        return Result.ok(templates);
    }

    /**
     * 更新权限模板状态
     */
    @Operation(summary = "更新权限模板状态")
    @PutMapping("/{id}/status")
    @SaCheckPermission("permission-template:update-status")
    public Result<Void> updateTemplateStatus(@PathVariable Long id, @RequestParam Integer status) {
        permissionTemplateService.updateTemplateStatus(id, status);
        return Result.ok("状态更新成功", null);
    }
}