package com.aiready.permission.controller;

import com.aiready.permission.annotation.RequirePermission;
import com.aiready.permission.entity.Role;
import com.aiready.permission.service.RoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 分页查询角色列表
     */
    @GetMapping("/list")
    @RequirePermission("role:list")
    public Page<Role> list(@RequestParam(defaultValue = "1") Integer page,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getDeleted, 0);
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Role::getRoleName, keyword)
                    .or()
                    .like(Role::getRoleCode, keyword));
        }
        
        wrapper.orderByAsc(Role::getSortOrder);
        return roleService.page(new Page<>(page, size), wrapper);
    }

    /**
     * 获取所有角色列表
     */
    @GetMapping("/all")
    @RequirePermission("role:list")
    public List<Role> all() {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getStatus, 1)
               .eq(Role::getDeleted, 0)
               .orderByAsc(Role::getSortOrder);
        return roleService.list(wrapper);
    }

    /**
     * 根据ID获取角色详情
     */
    @GetMapping("/{id}")
    @RequirePermission("role:detail")
    public Role getById(@PathVariable Long id) {
        return roleService.getById(id);
    }

    /**
     * 新增角色
     */
    @PostMapping
    @RequirePermission("role:create")
    public Boolean save(@RequestBody Role role) {
        return roleService.save(role);
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    @RequirePermission("role:update")
    public Boolean update(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        return roleService.updateById(role);
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @RequirePermission("role:delete")
    public Boolean delete(@PathVariable Long id) {
        return roleService.removeById(id);
    }

    /**
     * 批量删除角色
     */
    @DeleteMapping("/batch")
    @RequirePermission("role:delete")
    public Boolean deleteBatch(@RequestBody List<Long> ids) {
        return roleService.removeByIds(ids);
    }

    /**
     * 分配用户角色
     */
    @PostMapping("/assign/{userId}")
    @RequirePermission("role:assign")
    public void assignUserRoles(@PathVariable Long userId, @RequestBody List<Long> roleIds) {
        roleService.assignUserRoles(userId, roleIds);
    }

    /**
     * 获取用户的角色ID列表
     */
    @GetMapping("/user/{userId}")
    @RequirePermission("role:user")
    public List<Long> getUserRoleIds(@PathVariable Long userId) {
        return roleService.getUserRoleIds(userId);
    }

    /**
     * 根据角色编码获取角色
     */
    @GetMapping("/code/{roleCode}")
    @RequirePermission("role:detail")
    public Role getByRoleCode(@PathVariable String roleCode) {
        return roleService.getByRoleCode(roleCode);
    }
}
