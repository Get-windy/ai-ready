package cn.aiedge.tenant.controller;

import cn.aiedge.base.entity.SysTenant;
import cn.aiedge.base.mapper.TenantMapper;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 租户管理控制器
 * 提供租户的 CRUD、分页查询、状态管理等功能
 */
@RestController
@RequestMapping("/api/tenant")
@SaCheckLogin
@RequiredArgsConstructor
@Tag(name = "租户管理", description = "租户CRUD、分页查询、状态管理")
public class TenantController {

    private final TenantMapper tenantMapper;

    /**
     * 分页查询租户
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询租户")
    public Result<Map<String, Object>> getPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String tenantName,
            @RequestParam(required = false) String tenantCode,
            @RequestParam(required = false) Integer status) {

        Page<SysTenant> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<SysTenant>()
                .like(tenantName != null && !tenantName.isEmpty(), SysTenant::getTenantName, tenantName)
                .like(tenantCode != null && !tenantCode.isEmpty(), SysTenant::getTenantCode, tenantCode)
                .eq(status != null, SysTenant::getStatus, status)
                .orderByDesc(SysTenant::getCreateTime);

        Page<SysTenant> result = tenantMapper.selectPage(page, wrapper);

        Map<String, Object> data = Map.of(
            "records", result.getRecords(),
            "total", result.getTotal(),
            "current", result.getCurrent(),
            "size", result.getSize(),
            "pages", result.getPages()
        );

        return Result.ok(data);
    }

    /**
     * 获取租户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取租户详情")
    public Result<SysTenant> getById(@PathVariable Long id) {
        SysTenant tenant = tenantMapper.selectById(id);
        if (tenant == null || tenant.getDeleted() == 1) {
            return Result.fail(404, "租户不存在");
        }
        return Result.ok(tenant);
    }

    /**
     * 创建租户
     */
    @PostMapping
    @Operation(summary = "创建租户")
    @SaCheckPermission("system:tenant:create")
    public Result<Boolean> create(@RequestBody SysTenant tenant) {
        tenant.setDeleted(0);
        if (tenant.getStatus() == null) {
            tenant.setStatus(1);
        }
        int rows = tenantMapper.insert(tenant);
        return Result.ok(rows > 0);
    }

    /**
     * 更新租户
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新租户")
    @SaCheckPermission("system:tenant:update")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SysTenant tenant) {
        tenant.setId(id);
        int rows = tenantMapper.updateById(tenant);
        return Result.ok(rows > 0);
    }

    /**
     * 删除租户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除租户")
    @SaCheckPermission("system:tenant:delete")
    public Result<Boolean> delete(@PathVariable Long id) {
        int rows = tenantMapper.deleteById(id);
        return Result.ok(rows > 0);
    }

    /**
     * 批量删除租户
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除租户")
    @SaCheckPermission("system:tenant:delete")
    public Result<Boolean> batchDelete(@RequestBody List<Long> ids) {
        int rows = tenantMapper.deleteBatchIds(ids);
        return Result.ok(rows > 0);
    }

    /**
     * 更新租户状态
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "更新租户状态")
    @SaCheckPermission("system:tenant:update")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        SysTenant tenant = new SysTenant();
        tenant.setId(id);
        tenant.setStatus(status);
        int rows = tenantMapper.updateById(tenant);
        return Result.ok(rows > 0);
    }

    /**
     * 获取租户配置（占位，返回空配置）
     */
    @GetMapping("/{id}/config")
    @Operation(summary = "获取租户配置")
    public Result<Map<String, Object>> getConfig(@PathVariable Long id) {
        SysTenant tenant = tenantMapper.selectById(id);
        if (tenant == null || tenant.getDeleted() == 1) {
            return Result.fail(404, "租户不存在");
        }
        Map<String, Object> config = Map.of(
            "id", 0,
            "tenantId", id,
            "logo", "",
            "themeColor", "#1890ff",
            "features", "",
            "maxUsers", 100,
            "expireDate", ""
        );
        return Result.ok(config);
    }

    /**
     * 更新租户配置（占位）
     */
    @PutMapping("/{id}/config")
    @Operation(summary = "更新租户配置")
    public Result<Boolean> updateConfig(@PathVariable Long id, @RequestBody Map<String, Object> config) {
        // 配置存储暂未实现，返回成功占位
        return Result.ok(true);
    }
}
