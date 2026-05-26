package com.aiready.permission.controller;

import com.aiready.permission.annotation.RequirePermission;
import com.aiready.permission.entity.Organization;
import com.aiready.permission.service.DataPermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织机构管理控制器
 */
@RestController
@RequestMapping("/api/organization")
@RequiredArgsConstructor
public class OrganizationController {

    private final DataPermissionService dataPermissionService;

    /**
     * 分页查询组织机构列表
     */
    @GetMapping("/list")
    @RequirePermission("organization:list")
    public Page<Organization> list(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer size,
                                   @RequestParam(required = false) String keyword) {
        // 这里需要使用OrganizationService，暂时使用DataPermissionService中的方法
        LambdaQueryWrapper<Organization> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Organization::getDeleted, 0);
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Organization::getOrgName, keyword)
                    .or()
                    .like(Organization::getOrgCode, keyword));
        }
        
        wrapper.orderByAsc(Organization::getSortOrder);
        // 注意：这里需要注入OrganizationService，暂时注释掉
        // return organizationService.page(new Page<>(page, size), wrapper);
        return null;
    }

    /**
     * 获取组织机构树
     */
    @GetMapping("/tree")
    @RequirePermission("organization:tree")
    public List<Organization> tree() {
        return dataPermissionService.getOrganizationTree();
    }

    /**
     * 根据编码获取组织机构
     */
    @GetMapping("/code/{orgCode}")
    @RequirePermission("organization:detail")
    public Organization getByCode(@PathVariable String orgCode) {
        return dataPermissionService.getOrganizationByCode(orgCode);
    }

    /**
     * 获取部门及其子部门ID集合
     */
    @GetMapping("/children/{deptId}")
    @RequirePermission("organization:children")
    public java.util.Set<Long> getDeptAndChildrenIds(@PathVariable Long deptId) {
        return dataPermissionService.getDeptAndChildrenIds(deptId);
    }
}
