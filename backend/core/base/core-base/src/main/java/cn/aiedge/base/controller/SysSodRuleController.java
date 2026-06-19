package cn.aiedge.base.controller;

import cn.aiedge.base.entity.SysSodRule;
import cn.aiedge.base.service.SysSodRuleService;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 职责分离规则控制器
 * <p>
 * 管理互斥角色配置。同一用户不能同时拥有互斥的角色组合。
 * 分配角色时自动验证，防止权限集中风险。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "职责分离规则", description = "互斥角色配置 CRUD")
@RestController
@RequestMapping("/api/sod-rule")
@RequiredArgsConstructor
public class SysSodRuleController {

    private final SysSodRuleService sysSodRuleService;

    @Operation(summary = "分页查询 SoD 规则")
    @GetMapping("/page")
    @SaCheckPermission("system:sod-rule:list")
    public Result<Page<SysSodRule>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<SysSodRule> page = new Page<>(pageNum, pageSize);
        Page<SysSodRule> result = sysSodRuleService.page(page,
                new LambdaQueryWrapper<SysSodRule>().orderByDesc(SysSodRule::getCreateTime));
        return Result.ok(result);
    }

    @Operation(summary = "获取 SoD 规则详情")
    @GetMapping("/{id}")
    @SaCheckPermission("system:sod-rule:list")
    public Result<SysSodRule> getById(@PathVariable Long id) {
        return Result.ok(sysSodRuleService.getById(id));
    }

    @Operation(summary = "创建 SoD 规则")
    @PostMapping
    @SaCheckPermission("system:sod-rule:create")
    public Result<Long> create(@RequestBody SysSodRule rule) {
        sysSodRuleService.save(rule);
        return Result.ok("创建成功", rule.getId());
    }

    @Operation(summary = "更新 SoD 规则")
    @PutMapping("/{id}")
    @SaCheckPermission("system:sod-rule:update")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysSodRule rule) {
        rule.setId(id);
        sysSodRuleService.updateById(rule);
        return Result.ok("更新成功", null);
    }

    @Operation(summary = "删除 SoD 规则")
    @DeleteMapping("/{id}")
    @SaCheckPermission("system:sod-rule:delete")
    public Result<Void> delete(@PathVariable Long id) {
        sysSodRuleService.removeById(id);
        return Result.ok("删除成功", null);
    }

    @Operation(summary = "验证角色分配是否违反 SoD 规则")
    @PostMapping("/validate")
    @SaCheckPermission("system:sod-rule:validate")
    public Result<java.util.List<SysSodRule>> validate(
            @RequestParam(required = false) Long userId,
            @RequestBody java.util.List<Long> roleIds) {
        java.util.List<SysSodRule> conflicts = sysSodRuleService.validateRoleAssignment(userId, roleIds);
        return Result.ok(conflicts);
    }
}
