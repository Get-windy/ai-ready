package cn.aiedge.api.controller;

import cn.aiedge.base.mapper.TenantMapper;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * 数据修复控制器 - 用于修复数据库编码问题和缺失的表/字段
 *
 * <p>注意：本控制器中的 DDL 操作（ALTER TABLE, CREATE TABLE 等）仅限管理员执行，
 * 已添加 {@code @SaCheckRole("admin")} 权限校验。</p>
 *
 * <p>SQL 兼容性说明（PostgreSQL）：</p>
 * <ul>
 *   <li>{@code ADD COLUMN IF NOT EXISTS} — 需要 PostgreSQL 9.6+</li>
 *   <li>{@code COMMENT ON COLUMN} — 所有版本均支持</li>
 *   <li>{@code CREATE TABLE IF NOT EXISTS} — 需要 PostgreSQL 9.1+</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/fix")
@RequiredArgsConstructor
public class DataFixController {

    private final JdbcTemplate jdbcTemplate;
    private final TenantMapper tenantMapper;

    @Operation(summary = "修复租户名称编码", description = "修复数据库中租户名称的中文乱码问题（仅限管理员）")
    @PostMapping("/tenant-name")
    @SaCheckLogin
    @SaCheckRole("admin")
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

    @Operation(summary = "修复数据库Schema", description = "添加缺失的表和字段（仅限管理员）。SQL兼容PostgreSQL 9.6+")
    @PostMapping("/schema")
    @SaCheckLogin
    @SaCheckRole("admin")
    public Result<String> fixSchema() {
        try {
            int totalUpdates = 0;

            // 1. 添加erp_product.sku字段
            try {
                jdbcTemplate.execute("ALTER TABLE erp_product ADD COLUMN IF NOT EXISTS sku VARCHAR(100)");
                jdbcTemplate.execute("COMMENT ON COLUMN erp_product.sku IS 'SKU(库存单位编码)'");
                totalUpdates++;
                log.info("添加erp_product.sku字段成功");
            } catch (Exception e) {
                log.warn("添加sku字段失败（可能已存在）: {}", e.getMessage());
            }

            // 2. 添加erp_stock.serial_no和sku字段
            try {
                jdbcTemplate.execute("ALTER TABLE erp_stock ADD COLUMN IF NOT EXISTS serial_no VARCHAR(100)");
                jdbcTemplate.execute("ALTER TABLE erp_stock ADD COLUMN IF NOT EXISTS sku VARCHAR(100)");
                totalUpdates++;
                log.info("添加erp_stock字段成功");
            } catch (Exception e) {
                log.warn("添加erp_stock字段失败: {}", e.getMessage());
            }

            // 3. 修复sys_print_chain_item表结构（删除错误的表，创建正确的）
            try {
                // 先检查表结构是否正确（是否有 item_id 列）
                boolean hasItemId = false;
                try {
                    jdbcTemplate.queryForObject(
                        "SELECT column_name FROM information_schema.columns " +
                        "WHERE table_name = 'sys_print_chain_item' AND column_name = 'item_id'",
                        String.class
                    );
                    hasItemId = true;
                } catch (Exception ignored) {
                    // 查询失败表示没有 item_id 列
                }

                if (!hasItemId) {
                    // 表结构错误，需要重建
                    log.info("sys_print_chain_item表结构错误，开始修复...");
                    jdbcTemplate.execute("DROP TABLE IF EXISTS sys_print_chain_item");
                    jdbcTemplate.execute("""
                        CREATE TABLE sys_print_chain_item (
                            item_id              BIGSERIAL       PRIMARY KEY,
                            chain_id             BIGINT          NOT NULL,
                            step_order           INTEGER         NOT NULL,
                            template_id          BIGINT          NOT NULL,
                            client_id            BIGINT          NOT NULL,
                            printer_name         VARCHAR(200),
                            screenshot_mode      VARCHAR(20)     NOT NULL DEFAULT 'DISABLED',
                            screenshot_confirm_timeout INTEGER   DEFAULT 300,
                            screenshot_config_json JSONB         DEFAULT '{}',
                            created_by           BIGINT,
                            created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
                            updated_at           TIMESTAMP       DEFAULT NOW(),
                            CONSTRAINT uk_sci_chain_step UNIQUE (chain_id, step_order),
                            CONSTRAINT ck_sci_step_order CHECK (step_order BETWEEN 1 AND 10)
                        )
                    """);
                    jdbcTemplate.execute("CREATE INDEX idx_sci_chain ON sys_print_chain_item(chain_id)");
                    log.info("sys_print_chain_item表修复成功");
                }
                totalUpdates++;
            } catch (Exception e) {
                log.warn("修复sys_print_chain_item表失败: {}", e.getMessage());
            }

            // 4. 创建erp_receipt表
            try {
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS erp_receipt (
                        id BIGSERIAL PRIMARY KEY,
                        tenant_id BIGINT DEFAULT 1,
                        receipt_no VARCHAR(100),
                        receipt_type INT DEFAULT 1,
                        customer_id BIGINT,
                        customer_name VARCHAR(200),
                        order_id BIGINT,
                        order_no VARCHAR(100),
                        invoice_id BIGINT,
                        invoice_no VARCHAR(100),
                        receipt_date DATE,
                        status INT DEFAULT 0,
                        receipt_amount DECIMAL(20,2) DEFAULT 0,
                        verified_amount DECIMAL(20,2) DEFAULT 0,
                        pending_amount DECIMAL(20,2) DEFAULT 0,
                        payment_method VARCHAR(50),
                        bank_account VARCHAR(100),
                        bank_name VARCHAR(200),
                        check_no VARCHAR(100),
                        transaction_no VARCHAR(100),
                        sales_person_id BIGINT,
                        sales_person_name VARCHAR(100),
                        department_id BIGINT,
                        department_name VARCHAR(100),
                        approved_by BIGINT,
                        approved_time TIMESTAMP,
                        approved_note VARCHAR(500),
                        verified_by BIGINT,
                        verified_time TIMESTAMP,
                        completed_by BIGINT,
                        completed_time TIMESTAMP,
                        remark VARCHAR(500),
                        internal_note VARCHAR(500),
                        source_type VARCHAR(50),
                        source_id BIGINT,
                        source_no VARCHAR(100),
                        pre_receipt_id BIGINT,
                        deposit_flag INT DEFAULT 0,
                        ext_info TEXT,
                        deleted INT DEFAULT 0,
                        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        create_by BIGINT,
                        update_by BIGINT,
                        version_no INT DEFAULT 0
                    )
                """);
                totalUpdates++;
                log.info("创建erp_receipt表成功");
            } catch (Exception e) {
                log.warn("创建erp_receipt表失败: {}", e.getMessage());
            }

            // 5. 创建erp_payment表
            try {
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS erp_payment (
                        id BIGSERIAL PRIMARY KEY,
                        tenant_id BIGINT DEFAULT 1,
                        payment_no VARCHAR(100),
                        payment_type INT DEFAULT 1,
                        supplier_id BIGINT,
                        supplier_name VARCHAR(200),
                        order_id BIGINT,
                        order_no VARCHAR(100),
                        invoice_id BIGINT,
                        invoice_no VARCHAR(100),
                        payment_date DATE,
                        status INT DEFAULT 0,
                        payment_amount DECIMAL(20,2) DEFAULT 0,
                        verified_amount DECIMAL(20,2) DEFAULT 0,
                        pending_amount DECIMAL(20,2) DEFAULT 0,
                        payment_method VARCHAR(50),
                        bank_account VARCHAR(100),
                        bank_name VARCHAR(200),
                        check_no VARCHAR(100),
                        transaction_no VARCHAR(100),
                        purchaser_id BIGINT,
                        purchaser_name VARCHAR(100),
                        department_id BIGINT,
                        department_name VARCHAR(100),
                        approved_by BIGINT,
                        approved_time TIMESTAMP,
                        approved_note VARCHAR(500),
                        verified_by BIGINT,
                        verified_time TIMESTAMP,
                        completed_by BIGINT,
                        completed_time TIMESTAMP,
                        remark VARCHAR(500),
                        internal_note VARCHAR(500),
                        source_type VARCHAR(50),
                        source_id BIGINT,
                        source_no VARCHAR(100),
                        pre_payment_id BIGINT,
                        deposit_flag INT DEFAULT 0,
                        ext_info TEXT,
                        deleted INT DEFAULT 0,
                        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        create_by BIGINT,
                        update_by BIGINT,
                        version_no INT DEFAULT 0
                    )
                """);
                totalUpdates++;
                log.info("创建erp_payment表成功");
            } catch (Exception e) {
                log.warn("创建erp_payment表失败: {}", e.getMessage());
            }

            // 6. 创建预收款/预付款表
            try {
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS erp_pre_receipt (
                        id BIGSERIAL PRIMARY KEY,
                        tenant_id BIGINT DEFAULT 1,
                        pre_receipt_no VARCHAR(100),
                        customer_id BIGINT,
                        customer_name VARCHAR(200),
                        pre_receipt_amount DECIMAL(20,2) DEFAULT 0,
                        used_amount DECIMAL(20,2) DEFAULT 0,
                        remaining_amount DECIMAL(20,2) DEFAULT 0,
                        receipt_date DATE,
                        status INT DEFAULT 0,
                        remark VARCHAR(500),
                        deleted INT DEFAULT 0,
                        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        create_by BIGINT,
                        update_by BIGINT
                    )
                """);
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS erp_pre_payment (
                        id BIGSERIAL PRIMARY KEY,
                        tenant_id BIGINT DEFAULT 1,
                        pre_payment_no VARCHAR(100),
                        supplier_id BIGINT,
                        supplier_name VARCHAR(200),
                        pre_payment_amount DECIMAL(20,2) DEFAULT 0,
                        used_amount DECIMAL(20,2) DEFAULT 0,
                        remaining_amount DECIMAL(20,2) DEFAULT 0,
                        payment_date DATE,
                        status INT DEFAULT 0,
                        remark VARCHAR(500),
                        deleted INT DEFAULT 0,
                        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        create_by BIGINT,
                        update_by BIGINT
                    )
                """);
                totalUpdates++;
                log.info("创建预收款/预付款表成功");
            } catch (Exception e) {
                log.warn("创建预收款/预付款表失败: {}", e.getMessage());
            }

            // 7. 创建核销冲抵表
            try {
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS erp_write_off (
                        id BIGSERIAL PRIMARY KEY,
                        tenant_id BIGINT DEFAULT 1,
                        write_off_no VARCHAR(100),
                        write_off_type INT DEFAULT 1,
                        receipt_id BIGINT,
                        invoice_id BIGINT,
                        write_off_amount DECIMAL(20,2) DEFAULT 0,
                        write_off_date DATE,
                        status INT DEFAULT 0,
                        remark VARCHAR(500),
                        deleted INT DEFAULT 0,
                        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        create_by BIGINT,
                        update_by BIGINT
                    )
                """);
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS erp_offset (
                        id BIGSERIAL PRIMARY KEY,
                        tenant_id BIGINT DEFAULT 1,
                        offset_no VARCHAR(100),
                        offset_type INT DEFAULT 1,
                        source_id BIGINT,
                        target_id BIGINT,
                        offset_amount DECIMAL(20,2) DEFAULT 0,
                        offset_date DATE,
                        status INT DEFAULT 0,
                        remark VARCHAR(500),
                        deleted INT DEFAULT 0,
                        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        create_by BIGINT,
                        update_by BIGINT
                    )
                """);
                totalUpdates++;
                log.info("创建核销冲抵表成功");
            } catch (Exception e) {
                log.warn("创建核销冲抵表失败: {}", e.getMessage());
            }

            // 8. 创建会计科目表
            try {
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS erp_finance_subject (
                        id BIGSERIAL PRIMARY KEY,
                        tenant_id BIGINT DEFAULT 1,
                        subject_code VARCHAR(50),
                        subject_name VARCHAR(200),
                        subject_type INT DEFAULT 1,
                        parent_id BIGINT DEFAULT 0,
                        level INT DEFAULT 1,
                        is_leaf INT DEFAULT 0,
                        balance_direction INT DEFAULT 1,
                        status INT DEFAULT 1,
                        remark VARCHAR(500),
                        deleted INT DEFAULT 0,
                        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        create_by BIGINT,
                        update_by BIGINT
                    )
                """);
                totalUpdates++;
                log.info("创建会计科目表成功");
            } catch (Exception e) {
                log.warn("创建会计科目表失败: {}", e.getMessage());
            }

            return Result.ok("Schema修复完成，执行了 " + totalUpdates + " 个修复操作");

        } catch (Exception e) {
            log.error("Schema修复失败", e);
            return Result.fail(500, "修复失败: " + e.getMessage());
        }
    }
}