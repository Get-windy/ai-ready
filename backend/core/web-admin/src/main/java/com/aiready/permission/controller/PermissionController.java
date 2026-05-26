package com.aiready.permission.controller;

import com.aiready.permission.annotation.RequirePermission;
import com.aiready.permission.entity.Permission;
import com.aiready.permission.service.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理控制器
 */
@RestController
@RequestMapping("/api/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * 分页查询权限列表
     */
    @GetMapping("/list")
    @RequirePermission("permission:list")
    public Page<Permission> list(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size,
                                  @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Permission::getDeleted, 0);
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Permission::getPermissionName, keyword)
                    .or()
                    .like(Permission::getPermissionCode, keyword));
        }
        
        wrapper.orderByAsc(Permission::getSortOrder);
        return permissionService.page(new Page<>(page, size), wrapper);
    }

    /**
     * 获取权限树
     */
    @GetMapping("/tree")
    @RequirePermission("permission:tree")
    public List<Permission> tree() {
        return permissionService.getPermissionTree();
    }

    /**
     * 根据ID获取权限详情
     */
    @GetMapping("/{id}")
    @RequirePermission("permission:detail")
    public Permission getById(@PathVariable Long id) {
        return permissionService.getById(id);
    }

    /**
     * 新增权限
     */
    @PostMapping
    @RequirePermission("permission:create")
    public Boolean save(@RequestBody Permission permission) {
        return permissionService.save(permission);
    }

    /**
     * 更新权限
     */
    @PutMapping("/{id}")
    @RequirePermission("permission:update")
    public Boolean update(@PathVariable Long id, @RequestBody Permission permission) {
        permission.setId(id);
        return permissionService.updateById(permission);
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    @RequirePermission("permission:delete")
    public Boolean delete(@PathVariable Long id) {
        return permissionService.removeById(id);
    }

    /**
     * 批量删除权限
     */
    @DeleteMapping("/batch")
    @RequirePermission("permission:delete")
    public Boolean deleteBatch(@RequestBody List<Long> ids) {
        return permissionService.removeByIds(ids);
    }

    /**
     * 分配角色权限
     */
    @PostMapping("/assign/{roleId}")
    @RequirePermission("permission:assign")
    public void assignRolePermissions(@PathVariable Long roleId, @RequestBody List<Long> permissionIds) {
        permissionService.assignRolePermissions(roleId, permissionIds);
    }

    /**
     * 获取角色的权限ID列表
     */
    @GetMapping("/role/{roleId}")
    @RequirePermission("permission:role")
    public List<Long> getRolePermissionIds(@PathVariable Long roleId) {
        return permissionService.getRolePermissionIds(roleId);
    }
}
