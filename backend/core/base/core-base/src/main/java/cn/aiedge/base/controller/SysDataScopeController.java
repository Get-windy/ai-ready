package cn.aiedge.base.controller;

import cn.aiedge.base.entity.SysDataScope;
import cn.aiedge.base.service.SysDataScopeService;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据权限范围控制器
 * <p>
 * 管理角色级数据范围规则（sys_data_scope），用于 CUSTOM 类型的数据权限。
 * 支持部门树选择、自定义 SQL、表级精准控制。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "数据权限范围", description = "自定义数据权限范围规则 CRUD")
@RestController
@RequestMapping("/api/data-scope")
@RequiredArgsConstructor
public class SysDataScopeController {

    private final SysDataScopeService sysDataScopeService;

    @Operation(summary = "分页查询数据权限规则")
    @GetMapping("/page")
    @SaCheckPermission("tenant-admin:data-scope:list")
    public Result<Page<SysDataScope>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam Long roleId) {
        Page<SysDataScope> page = new Page<>(pageNum, pageSize);
        Page<SysDataScope> result = sysDataScopeService.page(page,
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysDataScope>()
                        .eq(SysDataScope::getRoleId, roleId)
                        .orderByDesc(SysDataScope::getCreateTime));
        return Result.ok(result);
    }

    @Operation(summary = "获取角色的数据权限规则列表")
    @GetMapping("/list")
    @SaCheckPermission("tenant-admin:data-scope:list")
    public Result<List<SysDataScope>> list(@RequestParam Long roleId) {
        List<SysDataScope> list = sysDataScopeService.getByRoleIds(List.of(roleId));
        return Result.ok(list);
    }

    @Operation(summary = "保存角色数据权限规则（全量覆盖）")
    @PostMapping("/save")
    @SaCheckPermission("tenant-admin:data-scope:assign")
    public Result<Void> save(@RequestParam Long roleId, @RequestBody List<SysDataScope> scopes) {
        sysDataScopeService.saveRoleDataScopes(roleId, scopes);
        return Result.ok("保存成功", null);
    }

    @Operation(summary = "删除数据权限规则")
    @DeleteMapping("/{id}")
    @SaCheckPermission("tenant-admin:data-scope:delete")
    public Result<Void> delete(@PathVariable Long id) {
        sysDataScopeService.removeById(id);
        return Result.ok("删除成功", null);
    }
}
