package cn.aiedge.base.config;

import cn.hutool.crypto.digest.BCrypt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库初始化器
 * 应用启动时自动检查并创建基础表
 *
 * 注意：sys_menu 相关操作延迟到 ApplicationReadyEvent 后执行，
 * 避免与 Flyway 迁移产生锁竞争（Flyway 需要排他锁修改 sys_menu 表）。
 */
@Slf4j
@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("开始检查数据库表...");

        try {
            // 先确保字段存在（每个列操作单独try-catch，避免一个失败中断全部）
            // 注意：sys_menu 字段的添加已移到 ensureMenuSchemaSafe() 中延迟执行
            ensureColumnsExistSafely();
        } catch (Exception e) {
            log.warn("检查/添加字段时出错（非致命）: {}", e.getMessage());
        }

        boolean tablesCreated = false;
        try {
            // 检查 sys_user 表是否存在
            if (!tableExists("sys_user")) {
                log.info("sys_user 表不存在，开始初始化数据库...");
                createBasicTables();
                tablesCreated = true;
                log.info("数据库初始化完成！");
            } else {
                log.info("数据库表已存在，检查并更新密码...");
                updateAdminPassword();
            }
        } catch (Exception e) {
            log.error("createBasicTables 失败: {}", e.getMessage());
        }

        // 始终确保默认数据存在（兜底：即使 schema.sql 未执行或 createBasicTables 失败，数据也应有）
        try {
            ensureDefaultData();
        } catch (Exception e) {
            log.error("ensureDefaultData 失败: {}", e.getMessage());
        }

        // 确保 MyBatis-Plus 实体对应的表存在（JPA ddl-auto 不会创建这些表）
        try {
            ensureMybatisPlusTables();
        } catch (Exception e) {
            log.error("ensureMybatisPlusTables 失败: {}", e.getMessage());
        }

        // 修复已知缺少的列
        try {
            fixErpPurchaseOrderColumns();
        } catch (Exception e) {
            log.warn("fixErpPurchaseOrderColumns 失败: {}", e.getMessage());
        }

        // 补充 erp_stock 缺失的列（实体有29个字段，基础表只有11列）
        try {
            fixErpStockMissingColumns();
        } catch (Exception e) {
            log.warn("fixErpStockMissingColumns 失败: {}", e.getMessage());
        }

        // 补充 erp_sale_order 缺失的列（实体有130个字段，基础表只有26列）
        try {
            fixErpSaleOrderMissingColumns();
        } catch (Exception e) {
            log.warn("fixErpSaleOrderMissingColumns 失败: {}", e.getMessage());
        }

        // 补充 erp_sale_outbound 缺失的列（实体有123个字段，基础表只有29列）
        try {
            fixErpSaleOutboundMissingColumns();
        } catch (Exception e) {
            log.warn("fixErpSaleOutboundMissingColumns 失败: {}", e.getMessage());
        }

        // 补充 batch_number 缺失的列
        try {
            safeAddColumn("batch_number", "expiration_date", "DATE");
        } catch (Exception e) {
            log.warn("添加 batch_number.expiration_date 失败: {}", e.getMessage());
        }

        // 补充 erp_product 可能缺少的列（schema.sql 创建的表不包含这些）
        try {
            fixErpProductMissingColumns();
        } catch (Exception e) {
            log.warn("fixErpProductMissingColumns 失败: {}", e.getMessage());
        }

        // 注意：ensureMenuData() 已移到 onApplicationReady() 中延迟执行
        // 避免与 Flyway 迁移产生锁竞争
    }

    /**
     * 应用启动完成后执行 sys_menu 相关操作。
     * 此时 Flyway 迁移已完成，不会有锁竞争问题。
     *
     * 执行内容：
     * 1. 添加 sys_menu 表的新字段（Mega Menu 改造）
     * 2. 确保菜单与超级管理员的关联存在
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("应用启动完成，开始执行 sys_menu 相关初始化...");

        // 确保 sys_menu 字段存在（Mega Menu 改造新增的字段）
        try {
            ensureMenuSchemaSafe();
        } catch (Exception e) {
            log.warn("ensureMenuSchemaSafe 失败: {}", e.getMessage());
        }

        // 确保菜单与超级管理员的关联存在（sys_role_menu）
        try {
            ensureMenuData();
        } catch (Exception e) {
            log.warn("ensureMenuData 失败: {}", e.getMessage());
        }

        log.info("sys_menu 相关初始化完成");
    }

    /**
     * 确保 sys_menu 表的字段存在（Mega Menu 改造新增字段）
     * 从 ensureColumnsExistSafely() 中分离出来，延迟到 ApplicationReadyEvent 后执行
     */
    private void ensureMenuSchemaSafe() {
        safeAddColumn("sys_menu", "display_mode", "SMALLINT DEFAULT 0");
        safeAddColumn("sys_menu", "list_path", "VARCHAR(255)");
        safeAddColumn("sys_menu", "tag_label", "VARCHAR(20)");
        safeAddColumn("sys_menu", "menu_level", "SMALLINT DEFAULT 0");
        safeAddColumn("sys_menu", "biz_flow_tag", "VARCHAR(50)");
        safeAddColumn("sys_menu", "display_group", "INTEGER DEFAULT 0");
        safeAddColumn("sys_menu", "link_icon", "VARCHAR(200)");
    }

    private void fixErpPurchaseOrderColumns() {
        String[] extraCols = {
            "multi_check_level1 BIGINT",
            "multi_check_level2 BIGINT",
            "multi_check_level3 BIGINT",
            "multi_check_level4 BIGINT",
            "multi_check_level5 BIGINT",
            "multi_check_level6 BIGINT",
            "multi_check_date1 TIMESTAMP",
            "multi_check_date2 TIMESTAMP",
            "multi_check_date3 TIMESTAMP",
            "multi_check_date4 TIMESTAMP",
            "multi_check_date5 TIMESTAMP",
            "multi_check_date6 TIMESTAMP",
            "cur_check_level INTEGER DEFAULT 0"
        };
        for (String colDef : extraCols) {
            String colName = colDef.split("\\s+")[0];
            safeAddColumn("erp_purchase_order", colName, colDef.substring(colName.length()).trim());
        }
    }

    /**
     * 修复 erp_product 表缺失的列（schema.sql 创建的表不含 category_id/product_grade_id 等）
     */
    private void fixErpProductMissingColumns() {
        String[][] extraCols = {
            {"category_id", "BIGINT"},
            {"product_grade_id", "BIGINT"},
            {"cost_price", "DECIMAL(18,2) DEFAULT 0"},
            {"standard_price", "DECIMAL(18,2) DEFAULT 0"},
            {"wholesale_price", "DECIMAL(18,2) DEFAULT 0"},
            {"image_url", "VARCHAR(255)"},
            {"barcode", "VARCHAR(255)"},
            {"product_type", "VARCHAR(255)"},
            {"is_batch_managed", "INTEGER DEFAULT 0"},
            {"is_serial_managed", "INTEGER DEFAULT 0"},
        };
        for (String[] col : extraCols) {
            safeAddColumn("erp_product", col[0], col[1]);
        }
    }

    /**
     * 修复 erp_stock 表缺失的列（DatabaseInitializer 创建的基础表只有11列，实体有29个字段）
     */
    private void fixErpStockMissingColumns() {
        String[][] extraCols = {
            {"available_quantity",  "DECIMAL(18,2) DEFAULT 0"},
            {"frozen_quantity",     "DECIMAL(18,4) DEFAULT 0"},
            {"safety_stock",        "DECIMAL(18,2) DEFAULT 0"},
            {"min_stock",           "DECIMAL(18,2) DEFAULT 0"},
            {"max_stock",           "DECIMAL(18,2) DEFAULT 0"},
            {"unit",                "VARCHAR(50)"},
            {"unit_price",          "DECIMAL(18,4) DEFAULT 0"},
            {"batch_no",            "VARCHAR(100)"},
            {"production_date",     "TIMESTAMP"},
            {"validity_date",       "TIMESTAMP"},
            {"serial_no",           "VARCHAR(100)"},
            {"sku",                 "VARCHAR(100)"},
            {"is_initial",          "INTEGER DEFAULT 0"},
            {"supplier_id",         "BIGINT"},
            {"supplier_name",       "VARCHAR(200)"},
            {"remark",              "TEXT"},
            {"create_by",           "BIGINT"},
            {"update_by",           "BIGINT"},
        };
        for (String[] col : extraCols) {
            safeAddColumn("erp_stock", col[0], col[1]);
        }

        // 确保 erp_stock_replenishment 表存在
        safeCreateTable("erp_stock_replenishment",
            "id BIGSERIAL PRIMARY KEY, tenant_id BIGINT DEFAULT 1, " +
            "product_code VARCHAR(50) NOT NULL, product_name VARCHAR(200) NOT NULL, " +
            "product_spec VARCHAR(200), product_unit VARCHAR(20), " +
            "warehouse_id BIGINT, warehouse_name VARCHAR(200), " +
            "current_qty DECIMAL(18,2) DEFAULT 0, safety_stock DECIMAL(18,2) DEFAULT 0, " +
            "shortage_qty DECIMAL(18,2) DEFAULT 0, avg_daily_sales DECIMAL(18,2) DEFAULT 0, " +
            "days_of_stock DECIMAL(10,2) DEFAULT 0, lead_time INT DEFAULT 0, " +
            "suggested_qty DECIMAL(18,2) DEFAULT 0, priority VARCHAR(10) DEFAULT 'MEDIUM', " +
            "reason VARCHAR(500), status VARCHAR(20) DEFAULT 'PENDING', " +
            "supplier_id BIGINT, supplier_name VARCHAR(200), created_order_no VARCHAR(100), " +
            "remark TEXT, deleted INT DEFAULT 0, " +
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "create_by BIGINT, update_by BIGINT, version_no INT DEFAULT 0");
    }

    /**
     * 修复 erp_sale_order 表缺失的列（基础建表语句只有26列，SaleOrder 实体有130个字段）
     * 仅列出建表语句之外的列；已存在的列由 columnExists 检查跳过，幂等安全
     */
    private void fixErpSaleOrderMissingColumns() {
        String[][] extraCols = {
            {"sale_type",                    "INTEGER DEFAULT 0"},
            {"product_amount",               "DECIMAL(18,2) DEFAULT 0"},
            {"discount_amount",              "DECIMAL(18,2) DEFAULT 0"},
            {"bill_amount",                  "DECIMAL(18,2) DEFAULT 0"},
            {"settled_amount",               "DECIMAL(18,2) DEFAULT 0"},
            {"total_quantity",               "DECIMAL(18,2) DEFAULT 0"},
            {"expected_ship_time",           "TIMESTAMP"},
            {"supplement_type",              "VARCHAR(50)"},
            {"generation_method",            "VARCHAR(50)"},
            {"source_order",                 "VARCHAR(200)"},
            {"order_source",                 "INTEGER"},
            {"buyer_remark",                 "VARCHAR(500)"},
            {"order_remark",                 "VARCHAR(500)"},
            {"original_order_id",            "BIGINT"},
            {"original_order_no",            "VARCHAR(100)"},
            {"customer_code",                "VARCHAR(100)"},
            {"customer_level",               "VARCHAR(100)"},
            {"customer_remark",              "VARCHAR(1000)"},
            {"customer_ticket",              "VARCHAR(20)"},
            {"bank_name",                    "VARCHAR(200)"},
            {"bank_account",                 "VARCHAR(100)"},
            {"tax_no",                       "VARCHAR(100)"},
            {"warehouse_name",               "VARCHAR(200)"},
            {"dept_name",                    "VARCHAR(200)"},
            {"promoter_id",                  "BIGINT"},
            {"promoter_name",                "VARCHAR(100)"},
            {"contact_name",                 "VARCHAR(100)"},
            {"contact_phone",                "VARCHAR(50)"},
            {"pickup_address",               "VARCHAR(500)"},
            {"settlement_method",            "VARCHAR(50)"},
            {"delivery_method",              "VARCHAR(50)"},
            {"delivery_route",               "VARCHAR(200)"},
            {"delivery_route_id",            "BIGINT"},
            {"driver_id",                    "BIGINT"},
            {"driver_name",                  "VARCHAR(100)"},
            {"delivery_vehicle",             "VARCHAR(50)"},
            {"freight_payer",                "VARCHAR(50)"},
            {"shipping_fee",                 "DECIMAL(18,4) DEFAULT 0"},
            {"logistics_company",            "VARCHAR(200)"},
            {"waybill_no",                   "VARCHAR(200)"},
            {"cod_amount",                   "DECIMAL(18,2) DEFAULT 0"},
            {"promo_discount",               "DECIMAL(18,2) DEFAULT 0"},
            {"coupon_amount",                "DECIMAL(18,2) DEFAULT 0"},
            {"direct_discount",              "DECIMAL(18,2) DEFAULT 0"},
            {"other_fee",                    "DECIMAL(18,2) DEFAULT 0"},
            {"deposit_account",              "VARCHAR(100)"},
            {"deposit_amount",               "DECIMAL(18,2) DEFAULT 0"},
            {"prev_advance",                 "DECIMAL(18,2) DEFAULT 0"},
            {"advance_balance",              "DECIMAL(18,2) DEFAULT 0"},
            {"deposit_account1",             "VARCHAR(100)"},
            {"deposit_account2",             "VARCHAR(100)"},
            {"deposit_account3",             "VARCHAR(100)"},
            {"deposit_account4",             "VARCHAR(100)"},
            {"credit_limit",                 "DECIMAL(18,2) DEFAULT 0"},
            {"available_credit",             "DECIMAL(18,2) DEFAULT 0"},
            {"prev_debt",                    "DECIMAL(18,2) DEFAULT 0"},
            {"payment_date",                 "DATE"},
            {"reconciliation_date",          "DATE"},
            {"member_card_no",               "VARCHAR(100)"},
            {"member_name",                  "VARCHAR(100)"},
            {"member_discount",              "INTEGER"},
            {"prev_points",                  "DECIMAL(18,2) DEFAULT 0"},
            {"sale_points",                  "DECIMAL(18,2) DEFAULT 0"},
            {"return_points",                "DECIMAL(18,2) DEFAULT 0"},
            {"exchange_points",              "DECIMAL(18,2) DEFAULT 0"},
            {"used_points",                  "DECIMAL(18,2) DEFAULT 0"},
            {"current_points",               "DECIMAL(18,2) DEFAULT 0"},
            {"shipped_quantity",             "DECIMAL(18,2) DEFAULT 0"},
            {"unshipped_quantity",           "DECIMAL(18,2) DEFAULT 0"},
            {"return_quantity",              "DECIMAL(18,2) DEFAULT 0"},
            {"return_amount",                "DECIMAL(18,2) DEFAULT 0"},
            {"total_weight",                 "DECIMAL(18,4) DEFAULT 0"},
            {"total_volume",                 "DECIMAL(18,6) DEFAULT 0"},
            {"summary",                      "VARCHAR(500)"},
            {"region",                       "VARCHAR(200)"},
            {"attachment",                   "TEXT"},
            {"ext_num1",                     "DECIMAL(18,2)"},
            {"ext_num2",                     "DECIMAL(18,2)"},
            {"ext_text1",                    "VARCHAR(500)"},
            {"ext_text2",                    "VARCHAR(500)"},
            {"ext_text3",                    "VARCHAR(500)"},
            {"ext_text4",                    "VARCHAR(500)"},
            {"ext_text5",                    "VARCHAR(500)"},
            {"footer_ext_text1",             "VARCHAR(500)"},
            {"footer_ext_text2",             "VARCHAR(500)"},
            {"auditor_id",                   "BIGINT"},
            {"auditor_name",                 "VARCHAR(100)"},
            {"audit_time",                   "TIMESTAMP"},
            {"submitter_id",                 "BIGINT"},
            {"submitter_name",               "VARCHAR(100)"},
            {"submit_time",                  "TIMESTAMP"},
            {"print_count",                  "INTEGER DEFAULT 0"},
            {"bookkeeping_time",             "TIMESTAMP"},
            {"creator_name",                 "VARCHAR(100)"},
            {"third_party_order_no",         "VARCHAR(200)"},
            {"product_brand",                "VARCHAR(200)"},
            {"industry_category",            "VARCHAR(200)"},
            {"supplement_status",            "VARCHAR(50)"},
            {"shipped_order_no",             "VARCHAR(100)"},
            {"original_amount",              "DECIMAL(18,2) DEFAULT 0"},
            {"remaining_unshipped_amount",   "DECIMAL(18,2) DEFAULT 0"},
            {"original_discount",            "DECIMAL(18,2) DEFAULT 0"},
            {"original_item_count",          "INTEGER DEFAULT 0"},
            {"unshipped_item_count",         "INTEGER DEFAULT 0"},
            {"original_quantity",            "DECIMAL(18,2) DEFAULT 0"},
            {"unshipped_quantity_items",     "DECIMAL(18,2) DEFAULT 0"},
            {"fulfillment_rate",             "DECIMAL(8,2) DEFAULT 0"},
            {"picking_warehouse",            "VARCHAR(200)"},
            {"collection_location",          "VARCHAR(200)"},
        };
        for (String[] col : extraCols) {
            safeAddColumn("erp_sale_order", col[0], col[1]);
        }
    }

    /**
     * 修复 erp_sale_outbound 表缺失的列（基础建表语句只有29列，SaleOutbound 实体有123个字段）
     * 仅列出建表语句之外的列；已存在的列由 columnExists 检查跳过，幂等安全
     */
    private void fixErpSaleOutboundMissingColumns() {
        String[][] extraCols = {
            {"generation_method",            "VARCHAR(100)"},
            {"summary",                      "VARCHAR(500)"},
            {"customer_code",                "VARCHAR(100)"},
            {"customer_level",               "VARCHAR(50)"},
            {"customer_remark",              "VARCHAR(500)"},
            {"bank_name",                    "VARCHAR(200)"},
            {"bank_account",                 "VARCHAR(100)"},
            {"tax_no",                       "VARCHAR(100)"},
            {"location",                     "VARCHAR(200)"},
            {"region",                       "VARCHAR(100)"},
            {"promo_discount",               "DECIMAL(18,2) DEFAULT 0"},
            {"coupon_amount",                "DECIMAL(18,2) DEFAULT 0"},
            {"direct_discount",              "DECIMAL(18,2) DEFAULT 0"},
            {"other_fee",                    "DECIMAL(18,2) DEFAULT 0"},
            {"rounding_amount",              "DECIMAL(18,2) DEFAULT 0"},
            {"total_weight",                 "DECIMAL(18,4) DEFAULT 0"},
            {"total_volume",                 "DECIMAL(18,4) DEFAULT 0"},
            {"return_quantity",              "DECIMAL(18,2) DEFAULT 0"},
            {"return_amount",                "DECIMAL(18,2) DEFAULT 0"},
            {"box_count",                    "INTEGER DEFAULT 0"},
            {"settlement_method",            "VARCHAR(50)"},
            {"settled_amount",               "DECIMAL(18,2) DEFAULT 0"},
            {"settlement_status",            "VARCHAR(50)"},
            {"advance_payment_amount",       "DECIMAL(18,2) DEFAULT 0"},
            {"prev_advance_payment",         "DECIMAL(18,2) DEFAULT 0"},
            {"used_advance_payment",         "DECIMAL(18,2) DEFAULT 0"},
            {"order_deposit",                "DECIMAL(18,2) DEFAULT 0"},
            {"available_advance_payment",    "DECIMAL(18,2) DEFAULT 0"},
            {"advance_payment_balance",      "DECIMAL(18,2) DEFAULT 0"},
            {"credit_limit",                 "DECIMAL(18,2) DEFAULT 0"},
            {"available_credit",             "DECIMAL(18,2) DEFAULT 0"},
            {"prev_arrears",                 "DECIMAL(18,2) DEFAULT 0"},
            {"current_arrears",              "DECIMAL(18,2) DEFAULT 0"},
            {"arrears_balance",              "DECIMAL(18,2) DEFAULT 0"},
            {"payment_account1",             "VARCHAR(200)"},
            {"payment_account2",             "VARCHAR(200)"},
            {"payment_account3",             "VARCHAR(200)"},
            {"payment_account4",             "VARCHAR(200)"},
            {"delivery_method",              "VARCHAR(100)"},
            {"logistics_company",            "VARCHAR(200)"},
            {"logistics_branch",             "VARCHAR(200)"},
            {"freight_payer",                "VARCHAR(100)"},
            {"freight",                      "DECIMAL(18,2) DEFAULT 0"},
            {"tracking_number",              "VARCHAR(200)"},
            {"waybill_no",                   "VARCHAR(200)"},
            {"cod_amount",                   "DECIMAL(18,2) DEFAULT 0"},
            {"delivery_order_no",            "VARCHAR(100)"},
            {"delivery_driver",              "VARCHAR(100)"},
            {"expected_ship_time",           "TIMESTAMP"},
            {"actual_ship_time",             "TIMESTAMP"},
            {"picking_by",                   "BIGINT"},
            {"picking_time",                 "TIMESTAMP"},
            {"packing_by",                   "BIGINT"},
            {"packing_time",                 "TIMESTAMP"},
            {"shipped_by",                   "BIGINT"},
            {"shipped_time",                 "TIMESTAMP"},
            {"approved_by",                  "BIGINT"},
            {"approved_time",                "TIMESTAMP"},
            {"approved_note",                "VARCHAR(500)"},
            {"completed_by",                 "BIGINT"},
            {"completed_time",               "TIMESTAMP"},
            {"member_card_no",               "VARCHAR(100)"},
            {"prev_points",                  "DECIMAL(18,2) DEFAULT 0"},
            {"member_generated_points",      "DECIMAL(18,2) DEFAULT 0"},
            {"member_exchange_points",       "DECIMAL(18,2) DEFAULT 0"},
            {"member_used_points",           "DECIMAL(18,2) DEFAULT 0"},
            {"current_points",               "DECIMAL(18,2) DEFAULT 0"},
            {"payment_date",                 "DATE"},
            {"reconciliation_date",          "DATE"},
            {"internal_note",                "VARCHAR(500)"},
            {"buyer_remark",                 "VARCHAR(500)"},
            {"bookkeeper_name",              "VARCHAR(100)"},
            {"creator_name",                 "VARCHAR(100)"},
            {"auditor_name",                 "VARCHAR(100)"},
            {"print_count",                  "INTEGER DEFAULT 0"},
            {"bookkeeping_time",             "TIMESTAMP"},
            {"print_time",                   "TIMESTAMP"},
            {"ext_num1",                     "DECIMAL(18,2)"},
            {"ext_num2",                     "DECIMAL(18,2)"},
            {"ext_num3",                     "DECIMAL(18,2)"},
            {"ext_num4",                     "DECIMAL(18,2)"},
            {"ext_num5",                     "DECIMAL(18,2)"},
            {"ext_text1",                    "VARCHAR(500)"},
            {"ext_text2",                    "VARCHAR(500)"},
            {"ext_text3",                    "VARCHAR(500)"},
            {"ext_text4",                    "VARCHAR(500)"},
            {"ext_text5",                    "VARCHAR(500)"},
            {"ext_partner",                  "BIGINT"},
            {"ext_staff",                    "BIGINT"},
            {"ext_dept",                     "BIGINT"},
            {"footer_ext_text1",             "VARCHAR(500)"},
            {"footer_ext_text2",             "VARCHAR(500)"},
            {"ext_info",                     "TEXT"},
            {"version_no",                   "INTEGER DEFAULT 0"},
        };
        for (String[] col : extraCols) {
            safeAddColumn("erp_sale_outbound", col[0], col[1]);
        }
    }

    /**
     * 确保 MyBatis-Plus 实体对应的表存在（JPA ddl-auto 不会创建这些表）
     */
    private void ensureMybatisPlusTables() {
        // sys_position_category
        safeCreateTable("sys_position_category", "id BIGINT PRIMARY KEY, tenant_id BIGINT DEFAULT 1, category_code VARCHAR(50), " +
            "category_name VARCHAR(100), parent_id BIGINT DEFAULT 0, ancestors VARCHAR(500) DEFAULT '0', " +
            "sort INTEGER DEFAULT 0, status INTEGER DEFAULT 1, description VARCHAR(255), " +
            "create_by VARCHAR(64), create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "update_by VARCHAR(64), update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "deleted INTEGER DEFAULT 0");

        // sys_position
        safeCreateTable("sys_position", "id BIGINT PRIMARY KEY, tenant_id BIGINT DEFAULT 1, position_code VARCHAR(50), " +
            "position_name VARCHAR(100), category_id BIGINT, dept_id BIGINT, level INTEGER DEFAULT 0, " +
            "sort INTEGER DEFAULT 0, status INTEGER DEFAULT 1, description VARCHAR(500), remark VARCHAR(255), " +
            "create_by VARCHAR(64), create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "update_by VARCHAR(64), update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, deleted INTEGER DEFAULT 0");

        // sys_user_position
        safeCreateTable("sys_user_position", "id BIGINT PRIMARY KEY, user_id BIGINT, position_id BIGINT, " +
            "is_primary INTEGER DEFAULT 0, create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

        // erp_purchase_order
        safeCreateTable("erp_purchase_order", "id BIGINT PRIMARY KEY, tenant_id BIGINT DEFAULT 1, order_no VARCHAR(100), " +
            "supplier_id BIGINT, supplier_name VARCHAR(200), order_date TIMESTAMP, delivery_date TIMESTAMP, " +
            "total_amount DECIMAL(18,2) DEFAULT 0, tax_amount DECIMAL(18,2) DEFAULT 0, " +
            "discount_amount DECIMAL(18,2) DEFAULT 0, paid_amount DECIMAL(18,2) DEFAULT 0, " +
            "status INTEGER DEFAULT 0, approval_status INTEGER DEFAULT 0, approval_user_id BIGINT, " +
            "approval_time TIMESTAMP, warehouse_id BIGINT, payment_method VARCHAR(50), " +
            "payment_status INTEGER DEFAULT 0, delivery_status INTEGER DEFAULT 0, remark VARCHAR(500), " +
            "deleted INTEGER DEFAULT 0, create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, create_by BIGINT, update_by BIGINT, " +
            "purchaser_id BIGINT, purchaser_name VARCHAR(100), dept_id BIGINT, currency_id BIGINT, " +
            "exchange_rate DECIMAL(18,6) DEFAULT 1, total_amount_with_tax DECIMAL(18,2) DEFAULT 0, " +
            "total_quantity DECIMAL(18,2) DEFAULT 0, received_amount DECIMAL(18,2) DEFAULT 0, " +
            "fulfillment_percent DECIMAL(5,2) DEFAULT 0, contract_id BIGINT, source_type INTEGER DEFAULT 0, " +
            "source_id BIGINT, source_bill_no VARCHAR(100), children_flag INTEGER DEFAULT 0, " +
            "sale_order_no VARCHAR(100), closed_flag INTEGER DEFAULT 0, cancellation_flag INTEGER DEFAULT 0, " +
            "tran_status INTEGER DEFAULT 0, order_affirm INTEGER DEFAULT 0, " +
            "payment_method_id BIGINT, require_provide VARCHAR(200), cash_discount VARCHAR(100), " +
            "settle_date TIMESTAMP, settle_method_id BIGINT, delivery_address VARCHAR(500), " +
            "last_modify_date TIMESTAMP, supplier_confirmed BOOLEAN DEFAULT FALSE, " +
            "supplier_confirm_time TIMESTAMP, shipped BOOLEAN DEFAULT FALSE, ship_time TIMESTAMP, " +
            "tracking_number VARCHAR(100), estimated_arrival_time TIMESTAMP, received BOOLEAN DEFAULT FALSE, " +
            "receive_time TIMESTAMP, received_quantity DECIMAL(18,2) DEFAULT 0, " +
            "quality_check_result VARCHAR(100), invoice_status INTEGER DEFAULT 0, " +
            "invoice_number VARCHAR(100), invoice_amount DECIMAL(18,2) DEFAULT 0, invoice_date TIMESTAMP, " +
            "ext_info TEXT");

        // erp_sale_order
        safeCreateTable("erp_sale_order", "id BIGINT PRIMARY KEY, tenant_id BIGINT DEFAULT 1, order_no VARCHAR(100), " +
            "customer_id BIGINT, customer_name VARCHAR(200), order_date TIMESTAMP, " +
            "expected_ship_date TIMESTAMP, status INTEGER DEFAULT 0, total_amount DECIMAL(18,2) DEFAULT 0, " +
            "tax_amount DECIMAL(18,2) DEFAULT 0, total_amount_with_tax DECIMAL(18,2) DEFAULT 0, " +
            "received_amount DECIMAL(18,2) DEFAULT 0, salesman_id BIGINT, salesman_name VARCHAR(100), " +
            "dept_id BIGINT, warehouse_id BIGINT, shipping_address VARCHAR(500), " +
            "receiver_name VARCHAR(100), receiver_phone VARCHAR(50), remark VARCHAR(500), " +
            "ext_info TEXT, deleted INTEGER DEFAULT 0, create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, create_by BIGINT, update_by BIGINT");

        // erp_purchase_exchange
        safeCreateTable("erp_purchase_exchange", "id BIGINT PRIMARY KEY, tenant_id BIGINT DEFAULT 1, " +
            "exchange_no VARCHAR(100), original_order_id BIGINT, original_order_no VARCHAR(100), " +
            "supplier_id BIGINT, supplier_name VARCHAR(200), exchange_date TIMESTAMP, " +
            "exchange_reason VARCHAR(500), exchange_type INTEGER DEFAULT 0, status INTEGER DEFAULT 0, " +
            "remark VARCHAR(500), total_amount DECIMAL(18,2) DEFAULT 0, created_by BIGINT, " +
            "created_by_name VARCHAR(100), approved_by BIGINT, approved_by_name VARCHAR(100), " +
            "approved_time TIMESTAMP, completed_time TIMESTAMP, deleted INTEGER DEFAULT 0, " +
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "version INTEGER DEFAULT 0");

        // erp_stock
        safeCreateTable("erp_stock", "id BIGINT PRIMARY KEY, tenant_id BIGINT DEFAULT 1, product_id BIGINT, " +
            "product_name VARCHAR(200), product_code VARCHAR(100), warehouse_id BIGINT, " +
            "warehouse_name VARCHAR(100), quantity DECIMAL(18,2) DEFAULT 0, " +
            "deleted INTEGER DEFAULT 0, create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

        // erp_purchase_inbound
        safeCreateTable("erp_purchase_inbound", "id BIGINT PRIMARY KEY, tenant_id BIGINT DEFAULT 1, " +
            "inbound_no VARCHAR(100), order_id BIGINT, order_no VARCHAR(100), " +
            "supplier_id BIGINT, supplier_name VARCHAR(200), contract_id BIGINT, contract_no VARCHAR(100), " +
            "inbound_date DATE, inbound_type INTEGER DEFAULT 0, status INTEGER DEFAULT 0, " +
            "total_quantity DECIMAL(18,2) DEFAULT 0, total_amount DECIMAL(18,2) DEFAULT 0, " +
            "tax_amount DECIMAL(18,2) DEFAULT 0, total_amount_with_tax DECIMAL(18,2) DEFAULT 0, " +
            "warehouse_id BIGINT, warehouse_name VARCHAR(100), purchaser_id BIGINT, " +
            "purchaser_name VARCHAR(100), department_id BIGINT, department_name VARCHAR(100), " +
            "remark VARCHAR(500), deleted INTEGER DEFAULT 0, " +
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "create_by BIGINT, update_by BIGINT");

        // erp_sale_outbound
        safeCreateTable("erp_sale_outbound", "id BIGINT PRIMARY KEY, tenant_id BIGINT DEFAULT 1, " +
            "outbound_no VARCHAR(100), order_id BIGINT, order_no VARCHAR(100), " +
            "customer_id BIGINT, customer_name VARCHAR(200), contact_id BIGINT, contact_name VARCHAR(100), " +
            "outbound_date DATE, outbound_type INTEGER DEFAULT 0, status INTEGER DEFAULT 0, " +
            "total_quantity DECIMAL(18,2) DEFAULT 0, total_amount DECIMAL(18,2) DEFAULT 0, " +
            "warehouse_id BIGINT, warehouse_name VARCHAR(100), sales_person_id BIGINT, " +
            "sales_person_name VARCHAR(100), department_id BIGINT, department_name VARCHAR(100), " +
            "shipping_address VARCHAR(500), receiver_name VARCHAR(100), receiver_phone VARCHAR(50), " +
            "remark VARCHAR(500), deleted INTEGER DEFAULT 0, " +
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "create_by BIGINT, update_by BIGINT");

        // erp_sale_pre_order 预订货单主表（符合第三范式，只存订单级数据）
        safeCreateTable("erp_sale_pre_order",
            "id BIGINT PRIMARY KEY, " +
            "tenant_id BIGINT NOT NULL DEFAULT 1, " +
            "order_no VARCHAR(100) NOT NULL, " +
            "customer_id BIGINT NOT NULL, " +
            "customer_name VARCHAR(200) NOT NULL, " +
            "customer_code VARCHAR(100), " +
            "bank_name VARCHAR(200), " +
            "bank_account VARCHAR(100), " +
            "tax_no VARCHAR(100), " +
            "warehouse_id BIGINT, " +
            "warehouse_name VARCHAR(100), " +
            "handler_id BIGINT, " +
            "handler_name VARCHAR(100), " +
            "dept_id BIGINT, " +
            "dept_name VARCHAR(100), " +
            "order_date DATE NOT NULL, " +
            "sale_type INTEGER DEFAULT 0, " +
            "receiver_name VARCHAR(100), " +
            "receiver_phone VARCHAR(50), " +
            "shipping_address VARCHAR(500), " +
            "customer_level VARCHAR(50), " +
            "status INTEGER NOT NULL DEFAULT 0, " +
            "settlement_status INTEGER NOT NULL DEFAULT 0, " +
            // 金额字段
            "total_amount DECIMAL(18,2) DEFAULT 0, " +
            "discounted_amount DECIMAL(18,2) DEFAULT 0, " +
            "order_amount DECIMAL(18,2) DEFAULT 0, " +
            // 预订金相关
            "received_deposit DECIMAL(18,2) DEFAULT 0, " +
            "unreceived_deposit DECIMAL(18,2) DEFAULT 0, " +
            "deposit_balance DECIMAL(18,2) DEFAULT 0, " +
            "deposit_account1 VARCHAR(200), " +
            "deposit_account2 VARCHAR(200), " +
            "deposit_account3 VARCHAR(200), " +
            "deposit_account4 VARCHAR(200), " +
            "deposit_amount DECIMAL(18,2) DEFAULT 0, " +
            "credit_limit DECIMAL(18,2) DEFAULT 0, " +
            "deposit_deadline DATE, " +
            // 数量汇总
            "pre_order_quantity DECIMAL(18,2) DEFAULT 0, " +
            "ordered_quantity DECIMAL(18,2) DEFAULT 0, " +
            "un_ordered_quantity DECIMAL(18,2) DEFAULT 0, " +
            "shipped_quantity DECIMAL(18,2) DEFAULT 0, " +
            "un_shipped_quantity DECIMAL(18,2) DEFAULT 0, " +
            // 其他
            "total_weight DECIMAL(18,2) DEFAULT 0, " +
            "total_volume DECIMAL(18,2) DEFAULT 0, " +
            "region VARCHAR(100), " +
            "summary VARCHAR(500), " +
            "remark VARCHAR(500), " +
            "customer_remark VARCHAR(500), " +
            "customer_ticket VARCHAR(200), " +
            "attachment VARCHAR(500), " +
            // 扩展字段
            "ext_num1 DECIMAL(18,2), " +
            "ext_num2 DECIMAL(18,2), " +
            "ext_text1 VARCHAR(200), " +
            "ext_text2 VARCHAR(200), " +
            "ext_text3 VARCHAR(200), " +
            "ext_info TEXT, " +
            // 审计字段
            "create_by BIGINT, " +
            "creator_name VARCHAR(100), " +
            "submit_by BIGINT, " +
            "submitter_name VARCHAR(100), " +
            "submit_time TIMESTAMP, " +
            "approved_by BIGINT, " +
            "auditor_name VARCHAR(100), " +
            "approved_time TIMESTAMP, " +
            "print_count INTEGER DEFAULT 0, " +
            // 系统字段
            "deleted INTEGER NOT NULL DEFAULT 0, " +
            "version_no INTEGER NOT NULL DEFAULT 0, " +
            "create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
            "update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
            "update_by BIGINT"
        );

        // 预订货单主表索引
        safeCreateIndex("erp_sale_pre_order", "idx_pre_order_tenant", "tenant_id, deleted", false);
        safeCreateIndex("erp_sale_pre_order", "uk_pre_order_no", "tenant_id, order_no", true);  // 唯一约束：同一租户下订单号唯一
        safeCreateIndex("erp_sale_pre_order", "idx_pre_order_customer", "tenant_id, customer_id, order_date", false);
        safeCreateIndex("erp_sale_pre_order", "idx_pre_order_status", "tenant_id, status, order_date", false);
        safeCreateIndex("erp_sale_pre_order", "idx_pre_order_date", "tenant_id, order_date", false);
        safeCreateIndex("erp_sale_pre_order", "idx_pre_order_handler", "tenant_id, handler_id, order_date", false);
        safeCreateIndex("erp_sale_pre_order", "idx_pre_order_settlement", "tenant_id, settlement_status", false);
        safeCreateIndex("erp_sale_pre_order", "idx_pre_order_deposit_deadline", "tenant_id, deposit_deadline", false);

        // erp_sale_pre_order_item 预订货单明细表（符合第三范式，只存行项级数据）
        safeCreateTable("erp_sale_pre_order_item",
            "id BIGINT PRIMARY KEY, " +
            "tenant_id BIGINT NOT NULL DEFAULT 1, " +
            "order_id BIGINT NOT NULL, " +  // 外键关联主表
            "line_no INTEGER NOT NULL, " +
            // 商品信息
            "product_id BIGINT NOT NULL, " +
            "product_name VARCHAR(200) NOT NULL, " +
            "product_code VARCHAR(100), " +
            "barcode VARCHAR(100), " +
            "image_url VARCHAR(500), " +
            "specification VARCHAR(200), " +
            "model VARCHAR(200), " +
            "origin VARCHAR(200), " +
            "brand VARCHAR(100), " +
            // 单位和换算
            "unit VARCHAR(50), " +
            "small_unit VARCHAR(50), " +
            "small_unit_quantity DECIMAL(18,2), " +
            "conversion_relation VARCHAR(100), " +
            "conversion_result DECIMAL(18,2), " +
            // 库存相关
            "region VARCHAR(100), " +
            "location VARCHAR(100), " +
            "available_stock DECIMAL(18,2) DEFAULT 0, " +
            "available_stock_conversion DECIMAL(18,2) DEFAULT 0, " +
            "book_stock DECIMAL(18,2) DEFAULT 0, " +
            // 数量
            "quantity DECIMAL(18,2) NOT NULL DEFAULT 0, " +
            "piece_quantity DECIMAL(18,2), " +
            "big_pack DECIMAL(18,2), " +
            "mid_pack DECIMAL(18,2), " +
            "small_pack DECIMAL(18,2), " +
            "ordered_quantity DECIMAL(18,2) DEFAULT 0, " +
            "un_ordered_quantity DECIMAL(18,2) DEFAULT 0, " +
            "shipped_quantity DECIMAL(18,2) DEFAULT 0, " +
            "un_shipped_quantity DECIMAL(18,2) DEFAULT 0, " +
            "terminate_quantity DECIMAL(18,2) DEFAULT 0, " +
            "terminate_amount DECIMAL(18,2) DEFAULT 0, " +
            // 价格相关
            "last_sale_date DATE, " +
            "retail_price DECIMAL(18,2), " +
            "wholesale_price DECIMAL(18,2), " +
            "min_sale_price DECIMAL(18,2), " +
            "unit_price DECIMAL(18,2) DEFAULT 0, " +
            "amount DECIMAL(18,2) DEFAULT 0, " +
            "small_unit_price DECIMAL(18,2), " +
            "discount_rate DECIMAL(18,2) DEFAULT 0, " +
            "discounted_price DECIMAL(18,2), " +
            "discounted_amount DECIMAL(18,2), " +
            // 成本相关
            "cost_price DECIMAL(18,2), " +
            "cost_amount DECIMAL(18,2), " +
            "gross_profit DECIMAL(18,2), " +
            // 体积重量
            "volume DECIMAL(18,2), " +
            "weight DECIMAL(18,2), " +
            // 商品属性
            "product_attribute VARCHAR(100), " +
            "gift BOOLEAN NOT NULL DEFAULT FALSE, " +
            "remark VARCHAR(500), " +
            // 8个价格等级
            "price_level1 DECIMAL(18,2), " +
            "price_level2 DECIMAL(18,2), " +
            "price_level3 DECIMAL(18,2), " +
            "price_level4 DECIMAL(18,2), " +
            "price_level5 DECIMAL(18,2), " +
            "price_level6 DECIMAL(18,2), " +
            "price_level7 DECIMAL(18,2), " +
            "price_level8 DECIMAL(18,2), " +
            // 扩展字段
            "ext_num1 DECIMAL(18,2), " +
            "ext_num2 DECIMAL(18,2), " +
            "ext_num3 DECIMAL(18,2), " +
            "ext_num4 DECIMAL(18,2), " +
            "ext_num5 DECIMAL(18,2), " +
            "ext_text1 VARCHAR(200), " +
            "ext_text2 VARCHAR(200), " +
            "ext_partner BIGINT, " +
            "ext_staff BIGINT, " +
            "ext_dept BIGINT, " +
            // 系统字段
            "deleted INTEGER NOT NULL DEFAULT 0, " +
            "create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
            "update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
            "update_by BIGINT"
        );

        // 预订货单明细表索引
        safeCreateIndex("erp_sale_pre_order_item", "idx_pre_order_item_tenant", "tenant_id, deleted", false);
        safeCreateIndex("erp_sale_pre_order_item", "idx_pre_order_item_order", "order_id, line_no", false);
        safeCreateIndex("erp_sale_pre_order_item", "idx_pre_order_item_product", "tenant_id, product_id", false);

        // batch_number
        safeCreateTable("batch_number", "id BIGINT PRIMARY KEY, batch_no VARCHAR(100), product_id BIGINT, " +
            "product_code VARCHAR(100), product_name VARCHAR(200), specification VARCHAR(100), " +
            "unit VARCHAR(50), quantity DECIMAL(18,2) DEFAULT 0, " +
            "production_date DATE, expiry_date DATE, expiration_date DATE, status INTEGER DEFAULT 1, " +
            "warehouse_id BIGINT, deleted INTEGER DEFAULT 0, " +
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

        // erp_product (complete schema)
        safeCreateTable("erp_product", "id BIGINT PRIMARY KEY, tenant_id BIGINT, " +
            "product_code VARCHAR(255), product_name VARCHAR(255), unit VARCHAR(255), " +
            "category VARCHAR(255), category_id BIGINT, spec VARCHAR(255), " +
            "product_grade_id BIGINT, cost_price DECIMAL(18,2), standard_price DECIMAL(18,2), " +
            "wholesale_price DECIMAL(18,2), image_url VARCHAR(255), barcode VARCHAR(255), " +
            "product_type VARCHAR(255), sku VARCHAR(255), has_grade_price INTEGER, " +
            "weight DECIMAL(18,2), volume DECIMAL(18,2), origin VARCHAR(255), " +
            "brand VARCHAR(255), tax_rate DECIMAL(18,2), purchase_price DECIMAL(18,2), " +
            "retail_price DECIMAL(18,2), shelf_life_days INTEGER, " +
            "is_batch_managed INTEGER, is_serial_managed INTEGER, " +
            "approval_status VARCHAR(255), approval_by BIGINT, approval_time TIMESTAMP, " +
            "status VARCHAR(255), remark VARCHAR(255), deleted INTEGER DEFAULT 0, " +
            "create_time TIMESTAMP, update_time TIMESTAMP, " +
            "create_by VARCHAR(255), update_by VARCHAR(255)");

        // erp_warehouse (from schema.sql, BIGSERIAL incompatible with H2)
        safeCreateTable("erp_warehouse", "id BIGINT PRIMARY KEY, tenant_id BIGINT DEFAULT 1, " +
            "warehouse_code VARCHAR(50) NOT NULL, warehouse_name VARCHAR(200) NOT NULL, " +
            "address VARCHAR(500), contact_person VARCHAR(100), contact_phone VARCHAR(20), " +
            "status VARCHAR(20) DEFAULT 'ENABLED', remark TEXT, deleted INTEGER DEFAULT 0, " +
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "create_by VARCHAR(50), update_by VARCHAR(50)");

        // erp_product_category (complete)
        safeCreateTable("erp_product_category", "id BIGINT PRIMARY KEY, tenant_id BIGINT, " +
            "category_code VARCHAR(255), category_name VARCHAR(255), parent_id BIGINT, " +
            "category_level INTEGER, sort_order INTEGER, icon VARCHAR(255), " +
            "status INTEGER, description VARCHAR(255), remark VARCHAR(255), " +
            "deleted INTEGER DEFAULT 0, create_by BIGINT, create_time TIMESTAMP, " +
            "update_by BIGINT, update_time TIMESTAMP");

        // erp_product_grade
        safeCreateTable("erp_product_grade", "id BIGINT PRIMARY KEY, tenant_id BIGINT, " +
            "grade_code VARCHAR(255), grade_name VARCHAR(255), grade_level INTEGER, " +
            "sort_order INTEGER, status INTEGER DEFAULT 1, description VARCHAR(255), " +
            "remark VARCHAR(255), deleted INTEGER DEFAULT 0, " +
            "create_by BIGINT, create_time TIMESTAMP, " +
            "update_by BIGINT, update_time TIMESTAMP");

        // erp_partner (complete)
        safeCreateTable("erp_partner", "id BIGINT PRIMARY KEY, tenant_id BIGINT, " +
            "partner_code VARCHAR(255), partner_name VARCHAR(255), partner_short_name VARCHAR(255), " +
            "partner_type VARCHAR(255), partner_category_id BIGINT, partner_grade_id BIGINT, " +
            "unified_social_code VARCHAR(255), tax_id VARCHAR(255), legal_person VARCHAR(255), " +
            "registered_capital DECIMAL(18,2), company_phone VARCHAR(255), " +
            "company_email VARCHAR(255), company_website VARCHAR(255), industry VARCHAR(255), " +
            "country VARCHAR(255), province VARCHAR(255), city VARCHAR(255), district VARCHAR(255), " +
            "detail_address VARCHAR(255), contact_person VARCHAR(255), contact_phone VARCHAR(255), " +
            "contact_email VARCHAR(255), payment_terms VARCHAR(255), credit_limit DECIMAL(18,2), " +
            "credit_days INTEGER, tax_rate DECIMAL(18,2), settle_type VARCHAR(255), " +
            "opening_balance DECIMAL(18,2), current_balance DECIMAL(18,2), " +
            "default_warehouse_id BIGINT, default_delivery_addr_id BIGINT, " +
            "source_channel VARCHAR(255), source_partner_id BIGINT, " +
            "first_order_time TIMESTAMP, last_order_time TIMESTAMP, " +
            "total_order_count INTEGER, total_order_amount DECIMAL(18,2), " +
            "remark VARCHAR(255), status VARCHAR(255), deleted INTEGER DEFAULT 0, " +
            "create_by BIGINT, create_time TIMESTAMP, " +
            "update_by BIGINT, update_time TIMESTAMP");

        // erp_partner_category (complete)
        safeCreateTable("erp_partner_category", "id BIGINT PRIMARY KEY, tenant_id BIGINT DEFAULT 1, " +
            "category_code VARCHAR(255), category_name VARCHAR(255), category_type VARCHAR(255), " +
            "parent_id BIGINT, category_level INTEGER, sort_order INTEGER, " +
            "status INTEGER DEFAULT 1, description VARCHAR(255), remark VARCHAR(500), " +
            "deleted INTEGER DEFAULT 0, create_by BIGINT, create_time TIMESTAMP, " +
            "update_by BIGINT, update_time TIMESTAMP");

        // erp_partner_grade (complete)
        safeCreateTable("erp_partner_grade", "id BIGINT PRIMARY KEY, tenant_id BIGINT DEFAULT 1, " +
            "grade_code VARCHAR(255), grade_name VARCHAR(255), grade_type VARCHAR(255), " +
            "grade_level INTEGER, sort_order INTEGER, " +
            "status INTEGER DEFAULT 1, description VARCHAR(255), remark VARCHAR(500), " +
            "deleted INTEGER DEFAULT 0, create_by BIGINT, create_time TIMESTAMP, " +
            "update_by BIGINT, update_time TIMESTAMP");

        // finance_ledger (complete)
        safeCreateTable("finance_ledger", "id BIGINT PRIMARY KEY, tenant_id BIGINT DEFAULT 1, " +
            "fiscal_year INTEGER, fiscal_period INTEGER, subject_id BIGINT, " +
            "subject_code VARCHAR(255), subject_name VARCHAR(255), " +
            "opening_debit DECIMAL(18,2) DEFAULT 0, opening_credit DECIMAL(18,2) DEFAULT 0, " +
            "period_debit DECIMAL(18,2) DEFAULT 0, period_credit DECIMAL(18,2) DEFAULT 0, " +
            "closing_debit DECIMAL(18,2) DEFAULT 0, closing_credit DECIMAL(18,2) DEFAULT 0, " +
            "closing_balance DECIMAL(18,2) DEFAULT 0, balance_direction INTEGER DEFAULT 0, " +
            "deleted_flag INTEGER DEFAULT 0, " +
            "created_by VARCHAR(255), created_at TIMESTAMP, " +
            "updated_by VARCHAR(255), updated_at TIMESTAMP, remark VARCHAR(500)");

        // finance_account_subject
        safeCreateTable("finance_account_subject", "id BIGINT PRIMARY KEY, " +
            "subject_code VARCHAR(255), subject_name VARCHAR(255), parent_id BIGINT, " +
            "level INTEGER, subject_type INTEGER, direction INTEGER, " +
            "is_leaf INTEGER DEFAULT 1, is_enabled INTEGER DEFAULT 1, " +
            "deleted_flag INTEGER DEFAULT 0, tenant_id BIGINT DEFAULT 1, remark VARCHAR(500), " +
            "created_by VARCHAR(255), created_at TIMESTAMP, " +
            "updated_by VARCHAR(255), updated_at TIMESTAMP");

        // finance_account
        safeCreateTable("finance_account", "id BIGINT PRIMARY KEY, " +
            "account_name VARCHAR(255), account_type INTEGER, " +
            "bank_name VARCHAR(255), bank_account VARCHAR(255), " +
            "balance DECIMAL(18,2) DEFAULT 0, status INTEGER DEFAULT 1, " +
            "currency VARCHAR(50), account_level INTEGER, " +
            "deleted_flag INTEGER DEFAULT 0, tenant_id VARCHAR(255), remark VARCHAR(500), " +
            "created_by VARCHAR(255), created_at TIMESTAMP, " +
            "updated_by VARCHAR(255), updated_at TIMESTAMP");

        // finance_report
        safeCreateTable("finance_report", "id BIGINT PRIMARY KEY, " +
            "report_no VARCHAR(100), report_name VARCHAR(255), report_type INTEGER, " +
            "report_period VARCHAR(50), status INTEGER DEFAULT 1, report_data TEXT, " +
            "file_url VARCHAR(500), generated_by VARCHAR(255), generated_at TIMESTAMP, " +
            "approved_by VARCHAR(255), approved_at TIMESTAMP, " +
            "deleted_flag INTEGER DEFAULT 0, tenant_id VARCHAR(255), remark VARCHAR(500), " +
            "created_by VARCHAR(255), created_at TIMESTAMP, " +
            "updated_by VARCHAR(255), updated_at TIMESTAMP");

        // finance_tax_declaration
        safeCreateTable("finance_tax_declaration", "id BIGINT PRIMARY KEY, " +
            "declaration_no VARCHAR(100), taxpayer_id VARCHAR(100), taxpayer_name VARCHAR(255), " +
            "tax_type INTEGER, declaration_period VARCHAR(50), " +
            "tax_amount DECIMAL(18,2) DEFAULT 0, paid_amount DECIMAL(18,2) DEFAULT 0, " +
            "declaration_date TIMESTAMP, payment_date TIMESTAMP, status INTEGER DEFAULT 1, " +
            "declaration_file VARCHAR(500), remark VARCHAR(500), " +
            "deleted_flag INTEGER DEFAULT 0, tenant_id VARCHAR(255), " +
            "created_by VARCHAR(255), created_at TIMESTAMP, " +
            "updated_by VARCHAR(255), updated_at TIMESTAMP");

        // finance_transaction
        safeCreateTable("finance_transaction", "id BIGINT PRIMARY KEY, " +
            "transaction_no VARCHAR(100), transaction_type INTEGER, " +
            "amount DECIMAL(18,2) DEFAULT 0, transaction_time TIMESTAMP, " +
            "credit_account_id BIGINT, debit_account_id BIGINT, " +
            "biz_type INTEGER, biz_id BIGINT, description VARCHAR(500), status INTEGER DEFAULT 1, " +
            "voucher_no VARCHAR(100), attachment_url VARCHAR(500), " +
            "approved_by VARCHAR(255), approved_at TIMESTAMP, " +
            "deleted_flag INTEGER DEFAULT 0, tenant_id VARCHAR(255), remark VARCHAR(500), " +
            "created_by VARCHAR(255), created_at TIMESTAMP, " +
            "updated_by VARCHAR(255), updated_at TIMESTAMP");

        // finance_voucher
        safeCreateTable("finance_voucher", "id BIGINT PRIMARY KEY, " +
            "voucher_no VARCHAR(100), voucher_date DATE, " +
            "fiscal_year INTEGER, fiscal_period INTEGER, attachments INTEGER DEFAULT 0, " +
            "prep_by VARCHAR(255), prep_at TIMESTAMP, " +
            "audit_by VARCHAR(255), audit_at TIMESTAMP, " +
            "post_by VARCHAR(255), post_at TIMESTAMP, " +
            "status VARCHAR(50), total_debit DECIMAL(18,2) DEFAULT 0, " +
            "total_credit DECIMAL(18,2) DEFAULT 0, " +
            "deleted_flag INTEGER DEFAULT 0, tenant_id VARCHAR(255), remark VARCHAR(500), " +
            "created_by VARCHAR(255), created_at TIMESTAMP, " +
            "updated_by VARCHAR(255), updated_at TIMESTAMP");

        // finance_voucher_item
        safeCreateTable("finance_voucher_item", "id BIGINT PRIMARY KEY, " +
            "voucher_id BIGINT, summary VARCHAR(500), " +
            "subject_id BIGINT, subject_code VARCHAR(255), subject_name VARCHAR(255), " +
            "debit_amount DECIMAL(18,2) DEFAULT 0, credit_amount DECIMAL(18,2) DEFAULT 0, " +
            "source_type VARCHAR(50), source_id BIGINT, source_no VARCHAR(100), " +
            "deleted_flag INTEGER DEFAULT 0, tenant_id VARCHAR(255), remark VARCHAR(500), " +
            "created_by VARCHAR(255), created_at TIMESTAMP, " +
            "updated_by VARCHAR(255), updated_at TIMESTAMP");

        // finance_payable
        safeCreateTable("finance_payable", "id BIGINT PRIMARY KEY, " +
            "source_type VARCHAR(50), source_id BIGINT, source_no VARCHAR(100), " +
            "supplier_id VARCHAR(100), supplier_name VARCHAR(255), " +
            "total_amount DECIMAL(18,2) DEFAULT 0, paid_amount DECIMAL(18,2) DEFAULT 0, " +
            "remaining_amount DECIMAL(18,2) DEFAULT 0, " +
            "due_date DATE, invoice_date DATE, invoice_no VARCHAR(100), status VARCHAR(50), " +
            "deleted_flag INTEGER DEFAULT 0, tenant_id VARCHAR(255), remark VARCHAR(500), " +
            "created_by VARCHAR(255), created_at TIMESTAMP, " +
            "updated_by VARCHAR(255), updated_at TIMESTAMP");

        // finance_receivable
        safeCreateTable("finance_receivable", "id BIGINT PRIMARY KEY, " +
            "source_type VARCHAR(50), source_id BIGINT, source_no VARCHAR(100), " +
            "customer_id VARCHAR(100), customer_name VARCHAR(255), " +
            "total_amount DECIMAL(18,2) DEFAULT 0, paid_amount DECIMAL(18,2) DEFAULT 0, " +
            "remaining_amount DECIMAL(18,2) DEFAULT 0, " +
            "due_date DATE, invoice_date DATE, invoice_no VARCHAR(100), status VARCHAR(50), " +
            "deleted_flag INTEGER DEFAULT 0, tenant_id VARCHAR(255), remark VARCHAR(500), " +
            "created_by VARCHAR(255), created_at TIMESTAMP, " +
            "updated_by VARCHAR(255), updated_at TIMESTAMP");

        // erp_supplier
        safeCreateTable("erp_supplier", "id BIGINT PRIMARY KEY, tenant_id VARCHAR(50), " +
            "supplier_code VARCHAR(100), supplier_name VARCHAR(200), short_name VARCHAR(100), " +
            "supplier_type INTEGER DEFAULT 0, enterprise_nature INTEGER DEFAULT 0, " +
            "credit_code VARCHAR(100), business_license VARCHAR(200), legal_person VARCHAR(100), " +
            "registered_capital DOUBLE PRECISION DEFAULT 0, establishment_date TIMESTAMP, " +
            "business_scope TEXT, contact_person VARCHAR(100), contact_phone VARCHAR(50), " +
            "contact_email VARCHAR(100), company_address VARCHAR(500), postal_code VARCHAR(20), " +
            "website VARCHAR(200), bank_name VARCHAR(200), bank_account VARCHAR(100), " +
            "tax_number VARCHAR(50), invoice_type INTEGER DEFAULT 0, payment_method INTEGER DEFAULT 0, " +
            "payment_period INTEGER DEFAULT 0, transport_method INTEGER DEFAULT 0, " +
            "cooperation_status INTEGER DEFAULT 1, supplier_level VARCHAR(50), " +
            "comprehensive_score DOUBLE PRECISION DEFAULT 0, certification_status INTEGER DEFAULT 0, " +
            "portal_status INTEGER DEFAULT 0, portal_account_id VARCHAR(100), remark VARCHAR(500), " +
            "status INTEGER DEFAULT 1, create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "create_by VARCHAR(64), update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "update_by VARCHAR(64), version INTEGER DEFAULT 0, deleted INTEGER DEFAULT 0, " +
            "category_tags TEXT, location_info TEXT, attachment_info TEXT, extend_info TEXT");

        // crm_customer
        safeCreateTable("crm_customer", "id BIGINT PRIMARY KEY, tenant_id BIGINT DEFAULT 1, customer_code VARCHAR(50), " +
            "customer_name VARCHAR(200), short_name VARCHAR(100), customer_type INTEGER, customer_source INTEGER, " +
            "industry_type INTEGER, province VARCHAR(50), city VARCHAR(50), district VARCHAR(50), " +
            "address VARCHAR(500), phone VARCHAR(50), fax VARCHAR(50), email VARCHAR(100), " +
            "website VARCHAR(200), legal_person VARCHAR(100), business_contact VARCHAR(100), " +
            "business_contact_phone VARCHAR(50), finance_contact VARCHAR(100), finance_contact_phone VARCHAR(50), " +
            "tax_number VARCHAR(50), bank_name VARCHAR(200), bank_account VARCHAR(50), " +
            "customer_level INTEGER, customer_level_desc VARCHAR(100), credit_limit DECIMAL(18,2), " +
            "current_debt DECIMAL(18,2), settlement_type INTEGER, settlement_days INTEGER, " +
            "status INTEGER DEFAULT 1, status_desc VARCHAR(100), sales_person_id BIGINT, " +
            "sales_person_name VARCHAR(100), deleted INTEGER DEFAULT 0, " +
            "create_by BIGINT, create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "update_by BIGINT, update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

        // erp_business_metric (erp-monitor module, MyBatis-Plus entity)
        safeCreateTable("erp_business_metric", "id BIGSERIAL PRIMARY KEY, tenant_id BIGINT DEFAULT 1, " +
            "metric_code VARCHAR(255), metric_name VARCHAR(255), metric_type VARCHAR(255), " +
            "metric_value DECIMAL(20,4), unit VARCHAR(50), period VARCHAR(50), " +
            "stat_time TIMESTAMP, dimension1_type VARCHAR(50), dimension1_value VARCHAR(100), " +
            "dimension2_type VARCHAR(50), dimension2_value VARCHAR(100), " +
            "dimension3_type VARCHAR(50), dimension3_value VARCHAR(100), " +
            "chain_ratio DECIMAL(10,4), year_ratio DECIMAL(10,4), " +
            "target_value DECIMAL(20,4), completion_rate DECIMAL(10,4), " +
            "status VARCHAR(50) DEFAULT 'normal', remark TEXT, deleted INTEGER DEFAULT 0, " +
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "create_by BIGINT, update_by BIGINT");

        // erp_pricing_strategy (price-engine module, MyBatis-Plus entity)
        safeCreateTable("erp_pricing_strategy", "strategy_id VARCHAR(64) PRIMARY KEY, " +
            "strategy_name VARCHAR(200), strategy_type VARCHAR(50), description VARCHAR(500), " +
            "priority INTEGER DEFAULT 100, enabled BOOLEAN DEFAULT TRUE, " +
            "effective_from TIMESTAMP, effective_to TIMESTAMP, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "created_by VARCHAR(64), updated_by VARCHAR(64), version INTEGER DEFAULT 1");

        // erp_discount_rule (price-engine module, MyBatis-Plus entity)
        safeCreateTable("erp_discount_rule", "rule_id VARCHAR(64) PRIMARY KEY, " +
            "rule_name VARCHAR(200), discount_type VARCHAR(50), description VARCHAR(500), " +
            "discount_rate DECIMAL(10,4) DEFAULT 0, fixed_discount_amount DECIMAL(18,2) DEFAULT 0, " +
            "min_discount_amount DECIMAL(18,2) DEFAULT 0, max_discount_amount DECIMAL(18,2) DEFAULT 999999.99, " +
            "condition_expression VARCHAR(500), priority INTEGER DEFAULT 100, enabled BOOLEAN DEFAULT TRUE, " +
            "effective_from TIMESTAMP, effective_to TIMESTAMP, " +
            "min_purchase_quantity INTEGER, min_purchase_amount DECIMAL(18,2) DEFAULT 0, " +
            "stackable BOOLEAN DEFAULT TRUE, max_stack_count INTEGER DEFAULT 0, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "created_by VARCHAR(64), updated_by VARCHAR(64), version INTEGER DEFAULT 1");

        // erp_price_calculation_request (price-engine module, MyBatis-Plus entity)
        safeCreateTable("erp_price_calculation_request", "request_id VARCHAR(64) PRIMARY KEY, " +
            "product_id VARCHAR(100), sku VARCHAR(100), product_name VARCHAR(200), category VARCHAR(100), " +
            "cost_price DECIMAL(18,2), base_price DECIMAL(18,2), market_reference_price DECIMAL(18,2), " +
            "customer_id VARCHAR(100), customer_name VARCHAR(200), customer_level VARCHAR(50), " +
            "quantity INTEGER DEFAULT 1, purchase_amount DECIMAL(18,2), " +
            "sales_channel VARCHAR(50), region VARCHAR(50), country VARCHAR(50), " +
            "calculation_time TIMESTAMP, order_type VARCHAR(50), order_id VARCHAR(100), " +
            "request_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, request_source VARCHAR(50)");

        // erp_price_calculation_result (price-engine module, MyBatis-Plus entity)
        safeCreateTable("erp_price_calculation_result", "result_id VARCHAR(64) PRIMARY KEY, " +
            "request_id VARCHAR(64), original_base_price DECIMAL(18,2), base_price DECIMAL(18,2), " +
            "primary_pricing_strategy_id VARCHAR(64), primary_pricing_strategy_name VARCHAR(200), " +
            "total_discount_amount DECIMAL(18,2) DEFAULT 0, total_discount_rate DECIMAL(10,4) DEFAULT 0, " +
            "discounted_price DECIMAL(18,2), final_price DECIMAL(18,2), " +
            "unit_price DECIMAL(18,2), total_price DECIMAL(18,2), cost_price DECIMAL(18,2), " +
            "gross_profit_margin DECIMAL(10,4), calculation_explanation VARCHAR(2000), " +
            "success BOOLEAN DEFAULT TRUE, error_message VARCHAR(500), error_code VARCHAR(50), " +
            "calculation_start_time TIMESTAMP, calculation_end_time TIMESTAMP, " +
            "calculation_duration_ms BIGINT, engine_version VARCHAR(50), " +
            "cached BOOLEAN DEFAULT FALSE, cache_key VARCHAR(200), suggestion VARCHAR(500)");

        // sys_data_scope — 自定义数据权限范围规则（角色级精细粒度控制）
        safeCreateTable("sys_data_scope", "id BIGINT PRIMARY KEY, tenant_id BIGINT DEFAULT 1, " +
            "role_id BIGINT NOT NULL, rule_type VARCHAR(50) NOT NULL DEFAULT 'DEPT', " +
            "target_table VARCHAR(100), target_field VARCHAR(100) DEFAULT 'dept_id', " +
            "dept_ids TEXT, custom_sql VARCHAR(1000), " +
            "remark VARCHAR(500), status INTEGER DEFAULT 1, " +
            "deleted INTEGER DEFAULT 0, version INTEGER DEFAULT 0, " +
            "create_by BIGINT, create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "update_by BIGINT, update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

        // sys_field_permission — 字段级权限（控制字段可见性和脱敏规则）
        safeCreateTable("sys_field_permission", "id BIGINT PRIMARY KEY, tenant_id BIGINT DEFAULT 1, " +
            "role_id BIGINT NOT NULL, target_table VARCHAR(100) NOT NULL, " +
            "target_field VARCHAR(100) NOT NULL, visible INTEGER DEFAULT 1, " +
            "mask_type VARCHAR(50), mask_char VARCHAR(10) DEFAULT '*', " +
            "mask_prefix_len INTEGER DEFAULT 0, mask_suffix_len INTEGER DEFAULT 0, " +
            "status INTEGER DEFAULT 1, " +
            "deleted INTEGER DEFAULT 0, version INTEGER DEFAULT 0, " +
            "create_by BIGINT, create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "update_by BIGINT, update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

        // sys_sod_rule — 职责分离规则（互斥角色配置）
        safeCreateTable("sys_sod_rule", "id BIGINT PRIMARY KEY, tenant_id BIGINT DEFAULT 1, " +
            "rule_name VARCHAR(100) NOT NULL, description VARCHAR(500), " +
            "conflict_role_ids TEXT NOT NULL, " +
            "status INTEGER DEFAULT 1, " +
            "deleted INTEGER DEFAULT 0, version INTEGER DEFAULT 0, " +
            "create_by BIGINT, create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "update_by BIGINT, update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
    }

    /**
     * 安全创建表：如果表不存在则创建
     */
    private void safeCreateTable(String tableName, String columnDefs) {
        try {
            if (!tableExists(tableName)) {
                log.info("创建 {} 表", tableName);
                jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + tableName + " (" + columnDefs + ")");
            }
        } catch (Exception e) {
            log.warn("创建 {} 表失败: {}", tableName, e.getMessage());
        }
    }

    /**
     * 安全创建索引：如果索引不存在则创建
     */
    private void safeCreateIndex(String tableName, String indexName, String columns, boolean unique) {
        try {
            // 检查索引是否已存在
            String checkSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = ? AND INDEX_NAME = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName, indexName);
            if (count == null || count == 0) {
                String uniqueStr = unique ? "UNIQUE " : "";
                String sql = "CREATE " + uniqueStr + "INDEX IF NOT EXISTS " + indexName + " ON " + tableName + " (" + columns + ")";
                jdbcTemplate.execute(sql);
                log.info("创建索引 {} on {}", indexName, tableName);
            }
        } catch (Exception e) {
            // H2 可能不支持 INFORMATION_SCHEMA.INDEXES，尝试直接创建
            try {
                String uniqueStr = unique ? "UNIQUE " : "";
                String sql = "CREATE " + uniqueStr + "INDEX IF NOT EXISTS " + indexName + " ON " + tableName + " (" + columns + ")";
                jdbcTemplate.execute(sql);
            } catch (Exception ex) {
                log.warn("创建索引 {} 失败: {}", indexName, ex.getMessage());
            }
        }
    }

    /**
     * 确保默认租户、管理员用户、角色等基础数据存在
     */
    private void ensureDefaultData() {
        try {
            // 检查默认租户是否存在
            Integer tenantCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_tenant WHERE id = 1", Integer.class);
            if (tenantCount == null || tenantCount == 0) {
                log.info("插入默认租户...");
                jdbcTemplate.execute(
                    "MERGE INTO sys_tenant (id, tenant_name, tenant_code, status, deleted, create_time) KEY(id) " +
                    "VALUES (1, '系统租户', 'SYSTEM', 1, 0, CURRENT_TIMESTAMP)");
            }
        } catch (Exception e) {
            log.warn("检查/插入默认租户失败: {}", e.getMessage());
        }

        try {
            // 检查默认管理员是否存在
            Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE id = 1", Integer.class);
            if (userCount == null || userCount == 0) {
                log.info("插入默认管理员用户...");
                String passwordHash = BCrypt.hashpw("admin123", BCrypt.gensalt());
                jdbcTemplate.execute(
                    "MERGE INTO sys_user (id, tenant_id, username, password, nickname, is_super_admin, status, deleted, create_time) KEY(id) " +
                    "VALUES (1, 1, 'admin', '" + passwordHash + "', '超级管理员', TRUE, 1, 0, CURRENT_TIMESTAMP)");
            }
        } catch (Exception e) {
            log.warn("检查/插入默认管理员失败: {}", e.getMessage());
        }

        try {
            // 检查默认角色是否存在
            Integer roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role WHERE id = 1", Integer.class);
            if (roleCount == null || roleCount == 0) {
                log.info("插入默认超级管理员角色...");
                jdbcTemplate.execute(
                    "MERGE INTO sys_role (id, tenant_id, role_code, role_name, role_type, data_scope, status, deleted, create_time) KEY(id) " +
                    "VALUES (1, 1, 'SUPER_ADMIN', '超级管理员', 0, 0, 0, 0, CURRENT_TIMESTAMP)");
            }
        } catch (Exception e) {
            log.warn("检查/插入默认角色失败: {}", e.getMessage());
        }

        try {
            // 检查用户-角色关联是否存在
            Integer urCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role WHERE id = 1", Integer.class);
            if (urCount == null || urCount == 0) {
                log.info("关联管理员与超级管理员角色...");
                jdbcTemplate.execute(
                    "MERGE INTO sys_user_role (id, user_id, role_id, tenant_id, create_time) KEY(id) " +
                    "VALUES (1, 1, 1, 1, CURRENT_TIMESTAMP)");
            }
        } catch (Exception e) {
            log.warn("检查/插入用户角色关联失败: {}", e.getMessage());
        }

        try {
            // 检查用户-租户关联是否存在
            Integer utCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user_tenant WHERE user_id = 1 AND tenant_id = 1", Integer.class);
            if (utCount == null || utCount == 0) {
                log.info("关联管理员与系统租户...");
                jdbcTemplate.execute(
                    "MERGE INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time) KEY(id) " +
                    "VALUES (1, 1, 1, TRUE, 1, CURRENT_TIMESTAMP)");
            }
        } catch (Exception e) {
            log.warn("检查/插入用户租户关联失败: {}", e.getMessage());
        }
    }

    /**
     * 安全地确保字段存在 —— 每个列操作单独捕获异常
     */
    private void ensureColumnsExistSafely() {
        // === sys_user 字段 ===
        safeAddColumn("sys_user", "last_login_time", "TIMESTAMP");
        safeAddColumn("sys_user", "last_login_ip", "VARCHAR(50)");
        safeAddColumn("sys_user", "login_count", "INTEGER DEFAULT 0");
        safeAddColumn("sys_user", "update_time", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        safeAddColumn("sys_user", "gender", "INTEGER DEFAULT 0");
        safeAddColumn("sys_user", "user_type", "INTEGER DEFAULT 1");
        safeAddColumn("sys_user", "ext_info", "TEXT");

        // === sys_role 字段 ===
        safeAddColumn("sys_role", "role_type", "INTEGER DEFAULT 1");
        safeAddColumn("sys_role", "data_scope", "INTEGER DEFAULT 0");
        safeAddColumn("sys_role", "sort", "INTEGER DEFAULT 0");
        safeAddColumn("sys_role", "remark", "VARCHAR(255)");
        safeAddColumn("sys_role", "update_time", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        safeAddColumn("sys_role", "create_by", "BIGINT");
        safeAddColumn("sys_role", "update_by", "BIGINT");

        // === sys_message 字段 ===
        safeAddColumn("sys_message", "send_status", "INTEGER DEFAULT 0");
        safeAddColumn("sys_message", "is_read", "INTEGER DEFAULT 0");
        safeAddColumn("sys_message", "retry_count", "INTEGER DEFAULT 0");
        safeAddColumn("sys_message", "fail_reason", "TEXT");
        safeAddColumn("sys_message", "send_time", "TIMESTAMP");
        safeAddColumn("sys_message", "msg_type", "INTEGER DEFAULT 1");
        safeAddColumn("sys_message", "receiver_id", "BIGINT");
        safeAddColumn("sys_message", "receiver_name", "VARCHAR(100)");
        safeAddColumn("sys_message", "receiver_contact", "VARCHAR(200)");
        safeAddColumn("sys_message", "template_code", "VARCHAR(50)");
        safeAddColumn("sys_message", "template_params", "TEXT");
        safeAddColumn("sys_message", "business_type", "VARCHAR(50)");
        safeAddColumn("sys_message", "business_id", "BIGINT");

        // === sys_department 字段 ===
        safeAddColumn("sys_department", "ancestors", "VARCHAR(500) DEFAULT '0'");
        safeAddColumn("sys_department", "leader_id", "BIGINT");
        safeAddColumn("sys_department", "leader_name", "VARCHAR(100)");
        safeAddColumn("sys_department", "phone", "VARCHAR(20)");
        safeAddColumn("sys_department", "email", "VARCHAR(100)");
        safeAddColumn("sys_department", "remark", "VARCHAR(500)");
        safeAddColumn("sys_department", "version", "INTEGER DEFAULT 0");
        safeAddColumn("sys_department", "dept_code", "VARCHAR(50)");
        safeAddColumn("sys_department", "parent_id", "BIGINT DEFAULT 0");
        safeAddColumn("sys_department", "sort", "INTEGER DEFAULT 0");
        safeAddColumn("sys_department", "create_by", "BIGINT");
        safeAddColumn("sys_department", "update_by", "BIGINT");

        // === sys_position 字段 ===
        safeAddColumn("sys_position", "dept_id", "BIGINT");
        safeAddColumn("sys_position", "level", "INTEGER DEFAULT 0");
        safeAddColumn("sys_position", "sort", "INTEGER DEFAULT 0");
        safeAddColumn("sys_position", "status", "INTEGER DEFAULT 1");
        safeAddColumn("sys_position", "remark", "VARCHAR(255)");

        // === sys_position_category 字段 ===
        safeAddColumn("sys_position_category", "ancestors", "VARCHAR(500) DEFAULT '0'");
        safeAddColumn("sys_position_category", "parent_id", "BIGINT DEFAULT 0");
        safeAddColumn("sys_position_category", "sort", "INTEGER DEFAULT 0");
        safeAddColumn("sys_position_category", "status", "INTEGER DEFAULT 1");
        safeAddColumn("sys_position_category", "description", "VARCHAR(255)");
        safeAddColumn("sys_position_category", "create_by", "VARCHAR(64)");
        safeAddColumn("sys_position_category", "update_by", "VARCHAR(64)");

        // === sys_menu 字段（Mega Menu 改造） ===
        safeAddColumn("sys_menu", "display_mode", "SMALLINT DEFAULT 0");
        safeAddColumn("sys_menu", "list_path", "VARCHAR(255)");
        safeAddColumn("sys_menu", "tag_label", "VARCHAR(20)");
        safeAddColumn("sys_menu", "menu_level", "SMALLINT DEFAULT 0");
        safeAddColumn("sys_menu", "biz_flow_tag", "VARCHAR(50)");
        safeAddColumn("sys_menu", "display_group", "INTEGER DEFAULT 0");
        safeAddColumn("sys_menu", "link_icon", "VARCHAR(200)");

        // === erp_sale_order 字段 ===
        String[] saleOrderColumns = {
            "customer_name            VARCHAR(200)",
            "salesman_name            VARCHAR(100)",
            "expected_ship_date       TIMESTAMP",
            "tax_amount               DECIMAL(18,2) DEFAULT 0",
            "total_amount_with_tax    DECIMAL(18,2) DEFAULT 0",
            "received_amount          DECIMAL(18,2) DEFAULT 0",
            "salesman_id              BIGINT",
            "dept_id                  BIGINT",
            "warehouse_id             BIGINT",
            "shipping_address         VARCHAR(500)",
            "receiver_name            VARCHAR(100)",
            "receiver_phone           VARCHAR(50)",
            "ext_info                 TEXT"
        };
        for (String colDef : saleOrderColumns) {
            String colName = colDef.split("\\s+")[0];
            safeAddColumn("erp_sale_order", colName, colDef.substring(colName.length()).trim());
        }

        // === erp_supplier 字段 ===
        String[] supplierColumns = {
            "supplier_type        INTEGER",
            "enterprise_nature     INTEGER",
            "credit_code           VARCHAR(500)",
            "business_license      VARCHAR(500)",
            "legal_person          VARCHAR(500)",
            "registered_capital    DOUBLE PRECISION",
            "establishment_date    TIMESTAMP",
            "business_scope        TEXT",
            "company_address       VARCHAR(500)",
            "postal_code           VARCHAR(50)",
            "website               VARCHAR(500)",
            "invoice_type          INTEGER",
            "payment_method        INTEGER",
            "payment_period        INTEGER",
            "transport_method      INTEGER",
            "cooperation_status    INTEGER DEFAULT 1",
            "supplier_level        VARCHAR(50)",
            "comprehensive_score   DOUBLE PRECISION DEFAULT 0",
            "certification_status  INTEGER DEFAULT 0",
            "portal_status         INTEGER DEFAULT 0",
            "portal_account_id     VARCHAR(200)",
            "version               INTEGER DEFAULT 0",
            "category_tags         TEXT",
            "location_info         TEXT",
            "attachment_info       TEXT",
            "extend_info           TEXT"
        };
        for (String colDef : supplierColumns) {
            String colName = colDef.split("\\s+")[0];
            if (tableExists("erp_supplier")) {
                safeAddColumn("erp_supplier", colName, colDef.substring(colName.length()).trim());
            }
        }
    }

    /**
     * 安全地添加列：如果列不存在则添加，捕获异常
     */
    private void safeAddColumn(String tableName, String columnName, String columnDef) {
        try {
            if (!columnExists(tableName, columnName)) {
                log.info("添加 {}.{} 字段", tableName, columnName);
                jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS " + columnName + " " + columnDef);
            }
        } catch (Exception e) {
            log.debug("添加 {}.{} 字段忽略（可能已存在）: {}", tableName, columnName, e.getMessage());
        }
    }

    // -- ensureColumnsExist() 已被 ensureColumnsExistSafely() 替代 --
    
    /**
     * 检查列是否存在
     */
    private boolean columnExists(String tableName, String columnName) {
        try {
            String sql = "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = '" + tableName + "' AND column_name = '" + columnName + "')";
            Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("检查列是否存在时出错: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查表是否存在
     */
    private boolean tableExists(String tableName) {
        try {
            // H2 stores table names in UPPERCASE in information_schema, while PostgreSQL uses lowercase.
            // Use case-insensitive comparison to work with both.
            String sql = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE LOWER(table_name) = LOWER('" + tableName + "'))";
            Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("检查表是否存在时出错: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 更新admin用户密码
     */
    private void updateAdminPassword() {
        try {
            // 使用Hutool BCrypt生成密码哈希（与初始创建密码一致）
            String passwordHash = BCrypt.hashpw("admin123", BCrypt.gensalt());

            jdbcTemplate.update("UPDATE sys_user SET password = ? WHERE username = 'admin' AND tenant_id = 1", passwordHash);
            log.info("Admin用户密码已更新");
        } catch (Exception e) {
            log.warn("更新密码失败: {}", e.getMessage());
        }
    }

    /**
     * 创建基础表（简化版）
     */
    private void createBasicTables() {
        log.info("创建基础数据库表...");
        
        // 创建租户表
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS sys_tenant (
                id BIGINT PRIMARY KEY,
                tenant_name VARCHAR(100) NOT NULL,
                tenant_code VARCHAR(50) UNIQUE NOT NULL,
                status INTEGER DEFAULT 0,
                deleted INTEGER DEFAULT 0,
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);
        
        // 创建用户表
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS sys_user (
                id BIGINT PRIMARY KEY,
                tenant_id BIGINT NOT NULL DEFAULT 1,
                username VARCHAR(50) NOT NULL,
                password VARCHAR(255) NOT NULL,
                nickname VARCHAR(50),
                email VARCHAR(100),
                phone VARCHAR(20),
                status INTEGER DEFAULT 0,
                deleted INTEGER DEFAULT 0,
                login_count INTEGER DEFAULT 0,
                last_login_time TIMESTAMP,
                last_login_ip VARCHAR(50),
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(tenant_id, username)
            )
        """);
        
        // 添加缺失的列（如果表已存在）
        try {
            jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS login_count INTEGER DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS last_login_time TIMESTAMP");
            jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS last_login_ip VARCHAR(50)");
            jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        } catch (Exception e) {
            log.debug("列可能已存在: {}", e.getMessage());
        }
        
        // 创建角色表
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS sys_role (
                id BIGINT PRIMARY KEY,
                tenant_id BIGINT NOT NULL DEFAULT 1,
                role_name VARCHAR(50) NOT NULL,
                role_code VARCHAR(50) NOT NULL,
                status INTEGER DEFAULT 0,
                deleted INTEGER DEFAULT 0,
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(tenant_id, role_code)
            )
        """);
        
        // 创建用户角色关联表
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS sys_user_role (
                id BIGINT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                role_id BIGINT NOT NULL,
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);
        
        // 插入默认租户
        jdbcTemplate.execute("""
            MERGE INTO sys_tenant (id, tenant_name, tenant_code, status) KEY(id)
            VALUES (1, '系统租户', 'SYSTEM', 1)
        """);
        
        // 生成BCrypt密码哈希
        String passwordHash = BCrypt.hashpw("admin123", BCrypt.gensalt());
        
        // 插入默认管理员用户
        jdbcTemplate.update("""
            MERGE INTO sys_user (id, tenant_id, username, password, nickname, status) KEY(id)
            VALUES (1, 1, 'admin', ?, '超级管理员', 1)
        """, passwordHash);
        
        // 插入默认角色
        jdbcTemplate.execute("""
            MERGE INTO sys_role (id, tenant_id, role_name, role_code, status) KEY(id)
            VALUES (1, 1, '超级管理员', 'SUPER_ADMIN', 0)
        """);
        
        // 关联用户和角色
        jdbcTemplate.execute("""
            MERGE INTO sys_user_role (id, user_id, role_id) KEY(id)
            VALUES (1, 1, 1)
        """);

        // 关联用户和租户
        jdbcTemplate.execute("""
            MERGE INTO sys_user_tenant (id, user_id, tenant_id, is_default, status) KEY(id)
            VALUES (1, 1, 1, TRUE, 1)
        """);

        log.info("基础表创建完成！Admin密码: admin123");
    }

    /**
     * 确保菜单与超级管理员的关联存在。
     *
     * 菜单树本身已由 Flyway 迁移维护（见 core-api/src/main/resources/db/migration），
     * 此处只做关联兜底。
     *
     * 历史说明：本方法曾内置一套 ID 1000-14004 的旧菜单种子（133 条 appendMenu），
     * 与 Flyway 的 6xxxx/7xxxx/8xxxx 体系并行，且用的是 H2 方言 `MERGE INTO ... KEY(id)`，
     * 在 PostgreSQL 上必然执行失败。已于 2026-09-19 清理，
     * 依据见仓库根目录 CLEANUP_SCOPE_20260919.md 的 DC-01。
     */
    private void ensureMenuData() {
        ensureRoleMenuAssociations();
    }

    /**
     * 将全部 tenant-admin 菜单关联到超级管理员角色
     */
    private void ensureRoleMenuAssociations() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1", Integer.class);
            if (count != null && count > 0) {
                log.info("角色菜单关联已存在 ({} 条)", count);
                return;
            }
        } catch (Exception e) {
            log.warn("检查角色菜单关联失败: {}", e.getMessage());
        }

        try {
            int inserted = jdbcTemplate.update(
                "INSERT INTO sys_role_menu (id, role_id, menu_id, tenant_id) " +
                "SELECT 100000 + m.id, 1, m.id, 0 FROM sys_menu m " +
                "WHERE m.client_type = 'tenant-admin' AND m.deleted = 0 " +
                "AND NOT EXISTS (SELECT 1 FROM sys_role_menu r WHERE r.role_id = 1 AND r.menu_id = m.id)"
            );
            log.info("关联 {} 条菜单到超级管理员角色", inserted);
        } catch (Exception e) {
            log.warn("插入角色菜单关联失败: {}", e.getMessage());
        }
    }
}