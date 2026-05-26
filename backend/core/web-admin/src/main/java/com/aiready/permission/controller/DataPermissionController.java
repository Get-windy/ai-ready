package com.aiready.permission.controller;

import com.aiready.permission.annotation.RequirePermission;
import com.aiready.permission.entity.DataPermissionRule;
import com.aiready.permission.service.DataPermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据权限规则管理控制器
 */
@RestController
@RequestMapping("/api/data-permission")
@RequiredArgsConstructor
public class DataPermissionController {

    private final DataPermissionService dataPermissionService;

    /**
     * 分页查询数据权限规则列表
     */
    @GetMapping("/list")
    @RequirePermission("data-permission:list")
    public Page<DataPermissionRule> list(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<DataPermissionRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataPermissionRule::getDeleted, 0);
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(DataPermissionRule::getRuleName, keyword)
                    .or()
                    .like(DataPermissionRule::getTableName, keyword));
        }
        
        return dataPermissionService.page(new Page<>(page, size), wrapper);
    }

    /**
     * 根据ID获取数据权限规则详情
     */
    @GetMapping("/{id}")
    @RequirePermission("data-permission:detail")
    public DataPermissionRule getById(@PathVariable Long id) {
        return dataPermissionService.getById(id);
    }

    /**
     * 根据表名获取数据权限规则
     */
    @GetMapping("/table/{tableName}")
    @RequirePermission("data-permission:detail")
    public DataPermissionRule getByTableName(@PathVariable String tableName) {
        return dataPermissionService.getRuleByTableName(tableName);
    }

    /**
     * 新增数据权限规则
     */
    @PostMapping
    @RequirePermission("data-permission:create")
    public Boolean save(@RequestBody DataPermissionRule rule) {
        return dataPermissionService.save(rule);
    }

    /**
     * 更新数据权限规则
     */
    @PutMapping("/{id}")
    @RequirePermission("data-permission:update")
    public Boolean update(@PathVariable Long id, @RequestBody DataPermissionRule rule) {
        rule.setId(id);
        return dataPermissionService.updateById(rule);
    }

    /**
     * 删除数据权限规则
     */
    @DeleteMapping("/{id}")
    @RequirePermission("data-permission:delete")
    public Boolean delete(@PathVariable Long id) {
        return dataPermissionService.removeById(id);
    }

    /**
     * 批量删除数据权限规则
     */
    @DeleteMapping("/batch")
    @RequirePermission("data-permission:delete")
    public Boolean deleteBatch(@RequestBody List<Long> ids) {
        return dataPermissionService.removeByIds(ids);
    }

    /**
     * 获取用户的数据权限范围
     */
    @GetMapping("/scope/{userId}")
    @RequirePermission("data-permission:scope")
    public Integer getDataScope(@PathVariable Long userId) {
        return dataPermissionService.getDataScope(userId);
    }

    /**
     * 获取用户的数据权限部门ID集合
     */
    @GetMapping("/depts/{userId}")
    @RequirePermission("data-permission:depts")
    public java.util.Set<Long> getPermissionDeptIds(@PathVariable Long userId) {
        return dataPermissionService.getPermissionDeptIds(userId);
    }
}
