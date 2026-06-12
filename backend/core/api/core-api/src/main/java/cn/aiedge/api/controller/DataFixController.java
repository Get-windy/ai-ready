package cn.aiedge.api.controller;

import cn.aiedge.base.mapper.TenantMapper;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * 数据修复控制器 - 用于修复数据库编码问题
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/fix")
@RequiredArgsConstructor
public class DataFixController {

    private final JdbcTemplate jdbcTemplate;
    private final TenantMapper tenantMapper;

    @Operation(summary = "修复租户名称编码", description = "修复数据库中租户名称的中文乱码问题")
    @PostMapping("/tenant-name")
    @SaCheckLogin
    public Result<String> fixTenantName() {
        try {
            // 直接执行 SQL 更新，确保 UTF-8 编码
            int rows = jdbcTemplate.update(
                "UPDATE sys_tenant SET tenant_name = '系统租户' WHERE id = 1 AND tenant_code = 'SYSTEM'"
            );

            log.info("修复租户名称编码完成，更新行数: {}", rows);

            // 验证更新结果
            String tenantName = jdbcTemplate.queryForObject(
                "SELECT tenant_name FROM sys_tenant WHERE id = 1",
                String.class
            );

            return Result.ok("修复完成，当前租户名称: " + tenantName);

        } catch (Exception e) {
            log.error("修复租户名称编码失败", e);
            return Result.fail(500, "修复失败: " + e.getMessage());
        }
    }
}