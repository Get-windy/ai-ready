package cn.aiedge.base.config;

import cn.hutool.crypto.digest.BCrypt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库初始化器
 * 应用启动时自动检查并创建基础表
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

        // 补充 erp_stock 缺失的列（实体有27个字段，基础表只有11列）
        try {
            fixErpStockMissingColumns();
        } catch (Exception e) {
            log.warn("fixErpStockMissingColumns 失败: {}", e.getMessage());
        }

        // 补充 batch_number 可能缺少的列
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

        // 确保默认菜单数据存在（sys_menu 和 sys_role_menu）
        try {
            ensureMenuData();
        } catch (Exception e) {
            log.warn("ensureMenuData 失败: {}", e.getMessage());
        }
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
     * 修复 erp_stock 表缺失的列（DatabaseInitializer 创建的基础表只有11列，实体有27个字段）
     */
    private void fixErpStockMissingColumns() {
        String[][] extraCols = {
            {"available_quantity",  "DECIMAL(18,2) DEFAULT 0"},
            {"frozen_quantity",     "DECIMAL(18,4) DEFAULT 0"},
            {"safety_stock",        "DECIMAL(18,2) DEFAULT 0"},
            {"min_stock",           "DECIMAL(18,2) DEFAULT 0"},
            {"max_stock",           "DECIMAL(18,2) DEFAULT 0"},
            {"unit",                "VARCHAR(50)"},
            {"batch_no",            "VARCHAR(100)"},
            {"production_date",     "TIMESTAMP"},
            {"validity_date",       "TIMESTAMP"},
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

        // === fin_payable 字段（带修复类型逻辑） ===
        String[][] finPayableColumns = {
            {"supplier_name",        "VARCHAR(500) DEFAULT NULL"},
            {"contract_id",          "BIGINT DEFAULT NULL"},
            {"contract_no",          "VARCHAR(200) DEFAULT NULL"},
            {"original_amount",      "DECIMAL(18,2) DEFAULT 0"},
            {"paid_amount",          "DECIMAL(18,2) DEFAULT 0"},
            {"remaining_amount",     "DECIMAL(18,2) DEFAULT 0"},
            {"bill_date",            "DATE DEFAULT NULL"},
            {"due_date",             "DATE DEFAULT NULL"},
            {"overdue_days",         "INTEGER DEFAULT 0"},
            {"deleted",              "INTEGER DEFAULT 0"},
        };
        for (String[] col : finPayableColumns) {
            safeAddColumn("fin_payable", col[0], col[1]);
        }

        // 修复 fin_payable.create_by/update_by 类型
        fixFinPayableColumnType("create_by");
        fixFinPayableColumnType("update_by");
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
     * 修复 fin_payable 表字段类型：实体中为 String(VARCHAR)，但数据库为 bigint
     */
    private void fixFinPayableColumnType(String columnName) {
        try {
            String checkSql = "SELECT data_type FROM information_schema.columns WHERE table_name = 'fin_payable' AND column_name = '" + columnName + "'";
            String dataType = jdbcTemplate.queryForObject(checkSql, String.class);
            if ("bigint".equals(dataType) || "integer".equals(dataType)) {
                log.info("修复 fin_payable.{} 字段类型: {} → VARCHAR(64)", columnName, dataType);
                jdbcTemplate.execute("ALTER TABLE fin_payable ALTER COLUMN " + columnName + " TYPE VARCHAR(64)");
            }
        } catch (Exception e) {
            log.warn("修复 fin_payable.{} 字段类型失败: {}", columnName, e.getMessage());
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
     * 确保 pc-admin 菜单数据存在
     * 若 sys_menu 表中尚无 pc-admin 客户端菜单，则批量插入默认菜单树
     * 并将全部菜单关联到超级管理员角色（role_id=1）
     */
    private void ensureMenuData() {
        // 检查菜单数据是否已存在
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_menu WHERE client_type = 'pc-admin' AND deleted = 0", Integer.class);
            if (count != null && count > 0) {
                log.info("pc-admin 菜单数据已存在 ({} 条)，跳过初始化", count);
                return;
            }
        } catch (Exception e) {
            log.warn("检查菜单数据失败: {}", e.getMessage());
        }

        log.info("初始化 pc-admin 菜单树...");
        StringBuilder sql = new StringBuilder();
        sql.append("MERGE INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, deleted) KEY(id) VALUES ");

        // ═══════════════ 工作台 ═══════════════
        appendMenu(sql, 1000, 0, "工作台", "Dashboard", 1, "dashboard", "dashboard/index", "DashboardOutlined", 1);

        // ═══════════════ 系统管理 ═══════════════
        appendMenu(sql, 2000, 0, "系统管理", "System", 0, "system", null, "SettingOutlined", 10);
        appendMenu(sql, 2001, 2000, "用户管理", "SystemUser", 1, "system/user", "system/user/index", "UserOutlined", 1);
        appendMenu(sql, 2002, 2000, "角色管理", "SystemRole", 1, "system/role", "system/role/index", "SafetyOutlined", 2);
        appendMenu(sql, 2003, 2000, "菜单管理", "SystemMenu", 1, "system/menu", "system/menu/index", "MenuOutlined", 3);
        appendMenu(sql, 2004, 2000, "权限管理", "SystemPermission", 1, "system/permission", "system/permission/index", "LockOutlined", 4);
        appendMenu(sql, 2005, 2000, "部门管理", "SystemDepartment", 1, "system/department", "system/department/index", "ApartmentOutlined", 5);
        appendMenu(sql, 2006, 2000, "岗位管理", "SystemPosition", 1, "system/position", "system/position/index", "IdcardOutlined", 6);
        appendMenu(sql, 2007, 2000, "系统配置", "SystemConfig", 1, "system/config", "system/config/index", "SettingOutlined", 7);
        appendMenu(sql, 2008, 2000, "字典管理", "SystemDict", 1, "system/dict", "system/dict/index", "BookOutlined", 8);
        appendMenu(sql, 2009, 2000, "系统日志", "SystemLog", 1, "system/log", "system/log/index", "FileTextOutlined", 9);
        appendMenu(sql, 2010, 2000, "租户管理", "SystemTenant", 1, "system/tenant", "system/tenant/index", "TeamOutlined", 10);
        appendMenu(sql, 2011, 2000, "租户审批", "SystemTenantApproval", 1, "system/tenant-approval", "system/tenant-approval/index", "SafetyOutlined", 11);
        appendMenu(sql, 2012, 2000, "数据导入", "DataImport", 1, "system/data-import", "system/data-import/index", "ImportOutlined", 12);

        // ═══════════════ 采购管理 ═══════════════
        appendMenu(sql, 3000, 0, "采购管理", "Purchase", 0, "erp/purchase", null, "ShoppingCartOutlined", 20);
        appendMenu(sql, 3001, 3000, "采购订单", "PurchaseOrder", 1, "erp/purchase", "erp/purchase/index", "ShoppingCartOutlined", 1);
        appendMenu(sql, 3002, 3000, "采购换货", "PurchaseExchange", 1, "erp/purchase-exchange", "erp/purchase-exchange/index", "SwapOutlined", 2);

        // ═══════════════ 销售管理 ═══════════════
        appendMenu(sql, 4000, 0, "销售管理", "Sale", 0, "erp/sale", null, "ShoppingOutlined", 30);
        appendMenu(sql, 4001, 4000, "销售订单", "SaleOrder", 1, "erp/sale", "erp/sale/index", "ShoppingOutlined", 1);
        appendMenu(sql, 4002, 4000, "销售分析", "SalesAnalysis", 1, "erp/sales-analysis", "erp/sales-analysis/index", "BarChartOutlined", 2);
        appendMenu(sql, 4003, 4000, "销售报表", "SalesReport", 1, "erp/sales-report", "erp/sales-report/index", "LineChartOutlined", 3);

        // ═══════════════ 库存管理 ═══════════════
        appendMenu(sql, 5000, 0, "库存管理", "Stock", 0, "erp/stock", null, "ContainerOutlined", 40);
        appendMenu(sql, 5001, 5000, "库存列表", "StockList", 1, "erp/stock", "erp/stock/index", "ContainerOutlined", 1);
        appendMenu(sql, 5002, 5000, "入库管理", "StockIn", 1, "erp/stock-in", "erp/stock-in/index", "InboxOutlined", 2);
        appendMenu(sql, 5003, 5000, "库存盘点", "Stocktake", 1, "erp/stocktake", "erp/stocktake/index", "CheckSquareOutlined", 3);
        appendMenu(sql, 5004, 5000, "退货管理", "Return", 1, "erp/return", "erp/return/index", "RollbackOutlined", 4);
        appendMenu(sql, 5005, 5000, "发货管理", "Shipment", 1, "erp/shipment", "erp/shipment/index", "SendOutlined", 5);
        appendMenu(sql, 5006, 5000, "批次管理", "Batch", 1, "erp/batch", "erp/batch/index", "BarcodeOutlined", 6);
        appendMenu(sql, 5007, 5000, "序列号管理", "Serial", 1, "erp/serial", "erp/serial/index", "NumberOutlined", 7);
        appendMenu(sql, 5008, 5000, "成本调整", "CostAdjust", 1, "erp/stock-cost-adjust", "erp/stock-cost-adjust/index", "DollarOutlined", 8);
        appendMenu(sql, 5009, 5000, "溢余管理", "Overflow", 1, "erp/stock-overflow", "erp/stock-overflow/index", "PlusCircleOutlined", 9);
        appendMenu(sql, 5010, 5000, "报损管理", "Damage", 1, "erp/stock-damage", "erp/stock-damage/index", "MinusCircleOutlined", 10);
        appendMenu(sql, 5011, 5000, "调拨管理", "Transfer", 1, "erp/stock-transfer", "erp/stock-transfer/index", "SwapOutlined", 11);
        appendMenu(sql, 5012, 5000, "补货管理", "Replenishment", 1, "erp/stock-replenishment", "erp/stock-replenishment/index", "ShoppingCartOutlined", 12);
        appendMenu(sql, 5013, 5000, "库存预警", "AlertConfig", 1, "erp/stock-alert-config", "erp/stock-alert-config/index", "AlertOutlined", 13);
        appendMenu(sql, 5014, 5000, "BOM管理", "StockBom", 1, "erp/stock-bom", "erp/stock-bom/index", "DeploymentUnitOutlined", 14);
        appendMenu(sql, 5015, 5000, "组装管理", "Assemble", 1, "erp/stock-assemble", "erp/stock-assemble/index", "ToolOutlined", 15);
        appendMenu(sql, 5016, 5000, "拆分管理", "Split", 1, "erp/stock-split", "erp/stock-split/index", "ScissorOutlined", 16);

        // ═══════════════ 财务管理 ═══════════════
        appendMenu(sql, 6000, 0, "财务管理", "Finance", 0, "finance", null, "DollarOutlined", 50);
        appendMenu(sql, 6001, 6000, "财务总览", "FinanceIndex", 1, "finance", "finance/index", "DollarOutlined", 1);
        appendMenu(sql, 6002, 6000, "科目管理", "FinanceSubject", 1, "finance/subject", "finance/subject/index", "FileTextOutlined", 2);
        appendMenu(sql, 6003, 6000, "凭证管理", "FinanceVoucher", 1, "finance/voucher", "finance/voucher/index", "FileTextOutlined", 3);
        appendMenu(sql, 6004, 6000, "财务报表", "FinanceReport", 1, "finance/report", "finance/report/index", "BarChartOutlined", 4);
        appendMenu(sql, 6005, 6000, "应收账款", "Receivable", 1, "finance/receivable", "finance/receivable/index", "DollarOutlined", 5);
        appendMenu(sql, 6006, 6000, "应付账款", "Payable", 1, "finance/payable", "finance/payable/index", "DollarOutlined", 6);
        appendMenu(sql, 6007, 6000, "收款单", "Receipt", 1, "finance/receipt", "finance/receipt/index", "DollarOutlined", 7);
        appendMenu(sql, 6008, 6000, "付款单", "Payment", 1, "finance/payment", "finance/payment/index", "DollarOutlined", 8);
        appendMenu(sql, 6009, 6000, "预收款", "PreReceipt", 1, "finance/pre-receipt", "finance/pre-receipt/index", "DollarOutlined", 9);
        appendMenu(sql, 6010, 6000, "预付款", "PrePayment", 1, "finance/pre-payment", "finance/pre-payment/index", "DollarOutlined", 10);
        appendMenu(sql, 6011, 6000, "定金押金", "Deposit", 1, "finance/deposit", "finance/deposit/index", "DollarOutlined", 11);
        appendMenu(sql, 6012, 6000, "收付款核销", "WriteOff", 1, "finance/write-off", "finance/write-off/index", "CheckCircleOutlined", 12);
        appendMenu(sql, 6013, 6000, "往来对冲", "Offset", 1, "finance/offset", "finance/offset/index", "SwapOutlined", 13);
        appendMenu(sql, 6014, 6000, "资金流水", "CapitalFlow", 1, "finance/capital-flow", "finance/capital-flow/index", "FileTextOutlined", 14);
        appendMenu(sql, 6015, 6000, "对账管理", "Reconciliation", 1, "finance/reconciliation", "finance/reconciliation/index", "AuditOutlined", 15);
        appendMenu(sql, 6016, 6000, "应收账龄分析", "AccountsReceivable", 1, "finance/accounts-receivable", "finance/accounts-receivable/index", "DollarOutlined", 16);
        appendMenu(sql, 6017, 6000, "应付账龄分析", "AccountsPayable", 1, "finance/accounts-payable", "finance/accounts-payable/index", "DollarOutlined", 17);

        // ═══════════════ CRM ═══════════════
        appendMenu(sql, 7000, 0, "CRM", "Crm", 0, "crm", null, "TeamOutlined", 60);
        appendMenu(sql, 7001, 7000, "客户管理", "CrmCustomer", 1, "crm/customer", "crm/customer/index", "TeamOutlined", 1);
        appendMenu(sql, 7002, 7000, "合同管理", "CrmContract", 1, "crm/contract", "crm/contract/index", "FileTextOutlined", 2);
        appendMenu(sql, 7003, 7000, "线索管理", "CrmLead", 1, "crm/lead", "crm/lead/index", "FireOutlined", 3);
        appendMenu(sql, 7004, 7000, "商机管理", "CrmOpportunity", 1, "crm/opportunity", "crm/opportunity/index", "BulbOutlined", 4);
        appendMenu(sql, 7005, 7000, "报价管理", "CrmQuotation", 1, "crm/quotation", "crm/quotation/index", "DollarOutlined", 5);
        appendMenu(sql, 7006, 7000, "发票管理", "CrmInvoice", 1, "crm/invoice", "crm/invoice/index", "FileTextOutlined", 6);

        // ═══════════════ 供应商管理 ═══════════════
        appendMenu(sql, 8000, 0, "供应商管理", "Supplier", 0, "supplier", null, "TeamOutlined", 70);
        appendMenu(sql, 8001, 8000, "供应商列表", "SupplierIndex", 1, "supplier", "supplier/index", "TeamOutlined", 1);
        appendMenu(sql, 8002, 8000, "供应商询价", "SupplierInquiry", 1, "supplier/inquiry", "supplier/inquiry/index", "QuestionCircleOutlined", 2);
        appendMenu(sql, 8003, 8000, "供应商绩效", "SupplierPerformance", 1, "supplier/performance", "supplier/performance/index", "StarOutlined", 3);

        // ═══════════════ ERP管理 ═══════════════
        appendMenu(sql, 9000, 0, "ERP管理", "Erp", 0, "erp", null, "AppstoreOutlined", 80);
        appendMenu(sql, 9001, 9000, "ERP仪表盘", "ErpDashboard", 1, "erp/dashboard", "erp/dashboard/index", "DashboardOutlined", 1);
        appendMenu(sql, 9002, 9000, "产品管理", "ErpProduct", 1, "erp/product", "erp/product/index", "AppstoreOutlined", 2);
        appendMenu(sql, 9003, 9000, "往来单位", "ErpPartner", 1, "erp/partner", "erp/partner/index", "TeamOutlined", 3);
        appendMenu(sql, 9004, 9000, "客户等级定价", "PricingGrade", 1, "erp/pricing/customer-grade", "erp/pricing/index", "DollarOutlined", 4);
        appendMenu(sql, 9005, 9000, "定价审批", "PricingApproval", 1, "erp/pricing/approval", "erp/pricing/approval/index", "AuditOutlined", 5);
        appendMenu(sql, 9006, 9000, "价格层级", "PricingTiers", 1, "erp/pricing/tiers", "erp/pricing/tiers/index", "PullRequestOutlined", 6);
        appendMenu(sql, 9007, 9000, "批量价格", "PriceBatch", 1, "erp/product/price-batch", "erp/product/price-batch", "DollarOutlined", 7);
        appendMenu(sql, 9008, 9000, "库存模式", "InventoryMode", 1, "erp/product/inventory-mode", "erp/product/inventory-mode", "SettingOutlined", 8);

        // ═══════════════ 固定资产 ═══════════════
        appendMenu(sql, 10000, 0, "固定资产", "FixedAsset", 0, "fixed-asset", null, "BankOutlined", 90);
        appendMenu(sql, 10001, 10000, "资产总览", "FaIndex", 1, "fixed-asset", "fixed-asset/index", "BankOutlined", 1);
        appendMenu(sql, 10002, 10000, "资产管理", "FaAsset", 1, "fixed-asset/asset", "fixed-asset/asset/index", "AppstoreOutlined", 2);
        appendMenu(sql, 10003, 10000, "资产分类", "FaCategory", 1, "fixed-asset/category", "fixed-asset/category/index", "UnorderedListOutlined", 3);
        appendMenu(sql, 10004, 10000, "折旧管理", "FaDepreciation", 1, "fixed-asset/depreciation", "fixed-asset/depreciation/index", "CalculatorOutlined", 4);
        appendMenu(sql, 10005, 10000, "资产调拨", "FaTransfer", 1, "fixed-asset/transfer", "fixed-asset/transfer/index", "SwapOutlined", 5);
        appendMenu(sql, 10006, 10000, "资产处置", "FaDisposal", 1, "fixed-asset/disposal", "fixed-asset/disposal/index", "DeleteOutlined", 6);
        appendMenu(sql, 10007, 10000, "资产盘点", "FaInventory", 1, "fixed-asset/inventory", "fixed-asset/inventory/index", "CheckSquareOutlined", 7);
        appendMenu(sql, 10008, 10000, "资产报表", "FaReport", 1, "fixed-asset/report", "fixed-asset/report/index", "BarChartOutlined", 8);
        appendMenu(sql, 10009, 10000, "资产采购", "FaPurchase", 1, "fixed-asset/purchase", "fixed-asset/purchase/index", "ShoppingCartOutlined", 9);

        // ═══════════════ 费用管理 ═══════════════
        appendMenu(sql, 11000, 0, "费用管理", "Expense", 0, "erp/expense", null, "DollarOutlined", 100);
        appendMenu(sql, 11001, 11000, "费用申请", "ExpenseApply", 1, "erp/expense/application", "erp/expense/application/index", "FileTextOutlined", 1);
        appendMenu(sql, 11002, 11000, "费用报销", "ExpenseReimburse", 1, "erp/expense/reimbursement", "erp/expense/reimbursement/index", "DollarOutlined", 2);
        appendMenu(sql, 11003, 11000, "费用审批", "ExpenseApproval", 1, "erp/expense/approval", "erp/expense/approval/index", "AuditOutlined", 3);
        appendMenu(sql, 11004, 11000, "费用付款", "ExpensePayment", 1, "erp/expense/payment", "erp/expense/payment/index", "DollarOutlined", 4);
        appendMenu(sql, 11005, 11000, "费用统计", "ExpenseStats", 1, "erp/expense/statistics", "erp/expense/statistics/index", "BarChartOutlined", 5);

        // ═══════════════ 预算管理 ═══════════════
        appendMenu(sql, 12000, 0, "预算管理", "Budget", 0, "budget", null, "FundOutlined", 110);
        appendMenu(sql, 12001, 12000, "预算总览", "BudgetIndex", 1, "budget", "budget/index", "FundOutlined", 1);
        appendMenu(sql, 12002, 12000, "预算模板", "BudgetTemplate", 1, "budget/template", "budget/template/index", "FileTextOutlined", 2);
        appendMenu(sql, 12003, 12000, "年度预算", "BudgetAnnual", 1, "budget/annual", "budget/annual/index", "CalendarOutlined", 3);
        appendMenu(sql, 12004, 12000, "预算调整", "BudgetAdjust", 1, "budget/adjustment", "budget/adjustment/index", "EditOutlined", 4);
        appendMenu(sql, 12005, 12000, "预算报表", "BudgetReport", 1, "budget/report", "budget/report/index", "BarChartOutlined", 5);

        // ═══════════════ 商城管理 ═══════════════
        appendMenu(sql, 13000, 0, "商城管理", "Mall", 0, "erp/mall", null, "ShopOutlined", 120);
        appendMenu(sql, 13001, 13000, "商城配置", "MallConfig", 1, "erp/mall/config", "erp/mall/config/index", "SettingOutlined", 1);
        appendMenu(sql, 13002, 13000, "用户审核", "MallUserAudit", 1, "erp/mall/user-audit", "erp/mall/user-audit/index", "AuditOutlined", 2);
        appendMenu(sql, 13003, 13000, "轮播图管理", "MallBanner", 1, "erp/mall/banner", "erp/mall/banner/index", "PictureOutlined", 3);
        appendMenu(sql, 13004, 13000, "订单管理", "MallOrder", 1, "erp/mall/order", "erp/mall/order/index", "ShoppingCartOutlined", 4);
        appendMenu(sql, 13005, 13000, "商品管理", "MallProduct", 1, "erp/mall/product", "erp/mall/product/index", "AppstoreOutlined", 5);

        // ═══════════════ 打印管理 ═══════════════
        appendMenu(sql, 14000, 0, "打印管理", "Printing", 0, "printing", null, "PrinterOutlined", 130);
        appendMenu(sql, 14001, 14000, "打印模板", "PrintTemplate", 1, "printing/template", "printing/template/index", "FileTextOutlined", 1);
        appendMenu(sql, 14002, 14000, "打印链路", "PrintChain", 1, "printing/chain", "printing/chain/index", "LinkOutlined", 2);
        appendMenu(sql, 14003, 14000, "打印客户端", "PrintClient", 1, "printing/client", "printing/client/index", "LaptopOutlined", 3);
        appendMenu(sql, 14004, 14000, "打印任务", "PrintTask", 1, "printing/task", "printing/task/index", "AuditOutlined", 4);

        // ═══════════════ 工作流管理 ═══════════════
        appendMenu(sql, 14100, 0, "工作流管理", "Workflow", 0, "workflow", null, "AuditOutlined", 135);
        appendMenu(sql, 14101, 14100, "实例监控", "WorkflowMonitor", 1, "workflow/instance-monitor", "workflow/instance-monitor", "AuditOutlined", 1);
        appendMenu(sql, 14102, 14100, "任务管理", "WorkflowTask", 1, "workflow/task-management", "workflow/task-management", "AuditOutlined", 2);
        appendMenu(sql, 14103, 14100, "流程分析", "WorkflowAnalysis", 1, "workflow/process-analysis", "workflow/process-analysis", "AuditOutlined", 3);

        // ═══════════════ 独立页面 ═══════════════
        appendMenu(sql, 15000, 0, "通知公告", "Notification", 1, "notification", "notification/index", "BellOutlined", 140);
        appendMenu(sql, 15100, 0, "个人中心", "Profile", 1, "profile", "profile/index", "UserOutlined", 150);
        appendMenu(sql, 15200, 0, "图表", "Charts", 1, "charts", "charts/index", "BarChartOutlined", 160);
        appendMenu(sql, 15300, 0, "订单中心", "OrderCenter", 1, "order-center", "order-center/index", "ShoppingCartOutlined", 170);

        // 移除末尾逗号
        sql.setLength(sql.length() - 1);

        try {
            jdbcTemplate.execute(sql.toString());
            log.info("pc-admin 菜单数据初始化完成");
        } catch (Exception e) {
            log.error("插入菜单数据失败: {}", e.getMessage());
            return;
        }

        // 关联菜单与超级管理员角色
        ensureRoleMenuAssociations();
    }

    /**
     * 将全部 pc-admin 菜单关联到超级管理员角色
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
                "SELECT 100000 + m.id, 1, m.id, 1 FROM sys_menu m " +
                "WHERE m.client_type = 'pc-admin' AND m.deleted = 0 " +
                "AND NOT EXISTS (SELECT 1 FROM sys_role_menu r WHERE r.role_id = 1 AND r.menu_id = m.id)"
            );
            log.info("关联 {} 条菜单到超级管理员角色", inserted);
        } catch (Exception e) {
            log.warn("插入角色菜单关联失败: {}", e.getMessage());
        }
    }

    /**
     * 向 MERGE INTO 追加一个 VALUES 元组
     */
    private void appendMenu(StringBuilder sb, long id, long parentId, String name, String code, int type, String path, String component, String icon, int sort) {
        sb.append("(").append(id).append(", 1, ").append(parentId).append(", '")
          .append(name.replace("'", "''")).append("', '")
          .append(code.replace("'", "''")).append("', ").append(type).append(", '")
          .append(path.replace("'", "''")).append("', ");
        if (component != null && !component.isEmpty()) {
            sb.append("'").append(component.replace("'", "''")).append("'");
        } else {
            sb.append("NULL");
        }
        sb.append(", '").append(icon.replace("'", "''")).append("', ")
          .append(sort).append(", 1, 1, 'pc-admin', 0),");
    }
}