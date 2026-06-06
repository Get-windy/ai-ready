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
            // 先确保字段存在
            ensureColumnsExist();
            
            // 检查 sys_user 表是否存在
            if (!tableExists("sys_user")) {
                log.info("sys_user 表不存在，开始初始化数据库...");
                createBasicTables();
                log.info("数据库初始化完成！");
            } else {
                log.info("数据库表已存在，检查并更新密码...");
                // 更新admin用户密码
                updateAdminPassword();
            }
        } catch (Exception e) {
            log.error("数据库初始化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 确保必要的字段存在
     */
    private void ensureColumnsExist() {
        log.info("检查并添加缺失的数据库字段...");
        
        // ========== sys_user 表字段 ==========
        // 检查 last_login_time 字段
        if (!columnExists("sys_user", "last_login_time")) {
            log.info("添加 sys_user.last_login_time 字段");
            jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN last_login_time TIMESTAMP");
        }
        
        // 检查 last_login_ip 字段
        if (!columnExists("sys_user", "last_login_ip")) {
            log.info("添加 sys_user.last_login_ip 字段");
            jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN last_login_ip VARCHAR(50)");
        }
        
        // 检查 login_count 字段
        if (!columnExists("sys_user", "login_count")) {
            log.info("添加 sys_user.login_count 字段");
            jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN login_count INTEGER DEFAULT 0");
        }
        
        // 检查 update_time 字段
        if (!columnExists("sys_user", "update_time")) {
            log.info("添加 sys_user.update_time 字段");
            jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        }
        
        // 检查 gender 字段
        if (!columnExists("sys_user", "gender")) {
            log.info("添加 sys_user.gender 字段");
            jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN gender INTEGER DEFAULT 0");
        }
        
        // 检查 user_type 字段
        if (!columnExists("sys_user", "user_type")) {
            log.info("添加 sys_user.user_type 字段");
            jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN user_type INTEGER DEFAULT 1");
        }
        
        // 检查 ext_info 字段
        if (!columnExists("sys_user", "ext_info")) {
            log.info("添加 sys_user.ext_info 字段");
            jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN ext_info JSONB");
        }
        
        // ========== sys_role 表字段 ==========
        // 检查 role_type 字段
        if (!columnExists("sys_role", "role_type")) {
            log.info("添加 sys_role.role_type 字段");
            jdbcTemplate.execute("ALTER TABLE sys_role ADD COLUMN role_type INTEGER DEFAULT 1");
        }
        
        // 检查 data_scope 字段
        if (!columnExists("sys_role", "data_scope")) {
            log.info("添加 sys_role.data_scope 字段");
            jdbcTemplate.execute("ALTER TABLE sys_role ADD COLUMN data_scope INTEGER DEFAULT 0");
        }
        
        // 检查 sort 字段
        if (!columnExists("sys_role", "sort")) {
            log.info("添加 sys_role.sort 字段");
            jdbcTemplate.execute("ALTER TABLE sys_role ADD COLUMN sort INTEGER DEFAULT 0");
        }
        
        // 检查 remark 字段
        if (!columnExists("sys_role", "remark")) {
            log.info("添加 sys_role.remark 字段");
            jdbcTemplate.execute("ALTER TABLE sys_role ADD COLUMN remark VARCHAR(255)");
        }
        
        // 检查 update_time 字段
        if (!columnExists("sys_role", "update_time")) {
            log.info("添加 sys_role.update_time 字段");
            jdbcTemplate.execute("ALTER TABLE sys_role ADD COLUMN update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        }
        
        // 检查 create_by 字段
        if (!columnExists("sys_role", "create_by")) {
            log.info("添加 sys_role.create_by 字段");
            jdbcTemplate.execute("ALTER TABLE sys_role ADD COLUMN create_by BIGINT");
        }

        // 检查 update_by 字段
        if (!columnExists("sys_role", "update_by")) {
            log.info("添加 sys_role.update_by 字段");
            jdbcTemplate.execute("ALTER TABLE sys_role ADD COLUMN update_by BIGINT");
        }

        // ========== sys_message 表字段 ==========
        // 检查 send_status 字段（老表可能缺这个字段）
        if (!columnExists("sys_message", "send_status")) {
            log.info("添加 sys_message.send_status 字段");
            jdbcTemplate.execute("ALTER TABLE sys_message ADD COLUMN send_status INTEGER DEFAULT 0");
        }

        // 检查 is_read 字段
        if (!columnExists("sys_message", "is_read")) {
            log.info("添加 sys_message.is_read 字段");
            jdbcTemplate.execute("ALTER TABLE sys_message ADD COLUMN is_read INTEGER DEFAULT 0");
        }

        // 检查 retry_count 字段
        if (!columnExists("sys_message", "retry_count")) {
            log.info("添加 sys_message.retry_count 字段");
            jdbcTemplate.execute("ALTER TABLE sys_message ADD COLUMN retry_count INTEGER DEFAULT 0");
        }

        // 检查 fail_reason 字段
        if (!columnExists("sys_message", "fail_reason")) {
            log.info("添加 sys_message.fail_reason 字段");
            jdbcTemplate.execute("ALTER TABLE sys_message ADD COLUMN fail_reason TEXT");
        }

        // 检查 send_time 字段
        if (!columnExists("sys_message", "send_time")) {
            log.info("添加 sys_message.send_time 字段");
            jdbcTemplate.execute("ALTER TABLE sys_message ADD COLUMN send_time TIMESTAMP");
        }

        // 检查 msg_type 字段
        if (!columnExists("sys_message", "msg_type")) {
            log.info("添加 sys_message.msg_type 字段");
            jdbcTemplate.execute("ALTER TABLE sys_message ADD COLUMN msg_type INTEGER DEFAULT 1");
        }

        // 检查 receiver_id 字段
        if (!columnExists("sys_message", "receiver_id")) {
            log.info("添加 sys_message.receiver_id 字段");
            jdbcTemplate.execute("ALTER TABLE sys_message ADD COLUMN receiver_id BIGINT");
        }

        // 检查 receiver_name 字段
        if (!columnExists("sys_message", "receiver_name")) {
            log.info("添加 sys_message.receiver_name 字段");
            jdbcTemplate.execute("ALTER TABLE sys_message ADD COLUMN receiver_name VARCHAR(100)");
        }

        // 检查 receiver_contact 字段
        if (!columnExists("sys_message", "receiver_contact")) {
            log.info("添加 sys_message.receiver_contact 字段");
            jdbcTemplate.execute("ALTER TABLE sys_message ADD COLUMN receiver_contact VARCHAR(200)");
        }

        // 检查 template_code 字段
        if (!columnExists("sys_message", "template_code")) {
            log.info("添加 sys_message.template_code 字段");
            jdbcTemplate.execute("ALTER TABLE sys_message ADD COLUMN template_code VARCHAR(50)");
        }

        // 检查 template_params 字段
        if (!columnExists("sys_message", "template_params")) {
            log.info("添加 sys_message.template_params 字段");
            jdbcTemplate.execute("ALTER TABLE sys_message ADD COLUMN template_params TEXT");
        }

        // 检查 business_type 字段
        if (!columnExists("sys_message", "business_type")) {
            log.info("添加 sys_message.business_type 字段");
            jdbcTemplate.execute("ALTER TABLE sys_message ADD COLUMN business_type VARCHAR(50)");
        }

        // 检查 business_id 字段
        if (!columnExists("sys_message", "business_id")) {
            log.info("添加 sys_message.business_id 字段");
            jdbcTemplate.execute("ALTER TABLE sys_message ADD COLUMN business_id BIGINT");
        }

        // ========== sys_department 表字段 ==========
        // 检查 ancestors 字段
        if (!columnExists("sys_department", "ancestors")) {
            log.info("添加 sys_department.ancestors 字段");
            jdbcTemplate.execute("ALTER TABLE sys_department ADD COLUMN ancestors VARCHAR(500) DEFAULT '0'");
        }

        // 检查 leader_id 字段
        if (!columnExists("sys_department", "leader_id")) {
            log.info("添加 sys_department.leader_id 字段");
            jdbcTemplate.execute("ALTER TABLE sys_department ADD COLUMN leader_id BIGINT");
        }

        // 检查 leader_name 字段
        if (!columnExists("sys_department", "leader_name")) {
            log.info("添加 sys_department.leader_name 字段");
            jdbcTemplate.execute("ALTER TABLE sys_department ADD COLUMN leader_name VARCHAR(100)");
        }

        // 检查 phone 字段
        if (!columnExists("sys_department", "phone")) {
            log.info("添加 sys_department.phone 字段");
            jdbcTemplate.execute("ALTER TABLE sys_department ADD COLUMN phone VARCHAR(20)");
        }

        // 检查 email 字段
        if (!columnExists("sys_department", "email")) {
            log.info("添加 sys_department.email 字段");
            jdbcTemplate.execute("ALTER TABLE sys_department ADD COLUMN email VARCHAR(100)");
        }

        // 检查 remark 字段
        if (!columnExists("sys_department", "remark")) {
            log.info("添加 sys_department.remark 字段");
            jdbcTemplate.execute("ALTER TABLE sys_department ADD COLUMN remark VARCHAR(500)");
        }

        // 检查 version 字段（乐观锁）
        if (!columnExists("sys_department", "version")) {
            log.info("添加 sys_department.version 字段");
            jdbcTemplate.execute("ALTER TABLE sys_department ADD COLUMN version INTEGER DEFAULT 0");
        }

        // 检查 dept_code 字段
        if (!columnExists("sys_department", "dept_code")) {
            log.info("添加 sys_department.dept_code 字段");
            jdbcTemplate.execute("ALTER TABLE sys_department ADD COLUMN dept_code VARCHAR(50)");
        }

        // 检查 parent_id 字段
        if (!columnExists("sys_department", "parent_id")) {
            log.info("添加 sys_department.parent_id 字段");
            jdbcTemplate.execute("ALTER TABLE sys_department ADD COLUMN parent_id BIGINT DEFAULT 0");
        }

        // 检查 sort 字段
        if (!columnExists("sys_department", "sort")) {
            log.info("添加 sys_department.sort 字段");
            jdbcTemplate.execute("ALTER TABLE sys_department ADD COLUMN sort INTEGER DEFAULT 0");
        }

        // 检查 create_by 字段
        if (!columnExists("sys_department", "create_by")) {
            log.info("添加 sys_department.create_by 字段");
            jdbcTemplate.execute("ALTER TABLE sys_department ADD COLUMN create_by BIGINT");
        }

        // 检查 update_by 字段
        if (!columnExists("sys_department", "update_by")) {
            log.info("添加 sys_department.update_by 字段");
            jdbcTemplate.execute("ALTER TABLE sys_department ADD COLUMN update_by BIGINT");
        }

        // ========== sys_position 表字段 ==========
        // 检查 dept_id 字段
        if (!columnExists("sys_position", "dept_id")) {
            log.info("添加 sys_position.dept_id 字段");
            jdbcTemplate.execute("ALTER TABLE sys_position ADD COLUMN dept_id BIGINT");
        }

        // 检查 level 字段
        if (!columnExists("sys_position", "level")) {
            log.info("添加 sys_position.level 字段");
            jdbcTemplate.execute("ALTER TABLE sys_position ADD COLUMN level INTEGER DEFAULT 0");
        }

        // 检查 sort 字段（旧表可能有 sort_order 但无 sort）
        if (!columnExists("sys_position", "sort")) {
            log.info("添加 sys_position.sort 字段");
            jdbcTemplate.execute("ALTER TABLE sys_position ADD COLUMN sort INTEGER DEFAULT 0");
        }

        // 检查 status 字段
        if (!columnExists("sys_position", "status")) {
            log.info("添加 sys_position.status 字段");
            jdbcTemplate.execute("ALTER TABLE sys_position ADD COLUMN status INTEGER DEFAULT 1");
        }

        // 检查 remark 字段
        if (!columnExists("sys_position", "remark")) {
            log.info("添加 sys_position.remark 字段");
            jdbcTemplate.execute("ALTER TABLE sys_position ADD COLUMN remark VARCHAR(255)");
        }

        // ========== sys_position_category 表字段 ==========
        // 检查 ancestors 字段
        if (!columnExists("sys_position_category", "ancestors")) {
            log.info("添加 sys_position_category.ancestors 字段");
            jdbcTemplate.execute("ALTER TABLE sys_position_category ADD COLUMN ancestors VARCHAR(500) DEFAULT '0'");
        }

        // 检查 parent_id 字段
        if (!columnExists("sys_position_category", "parent_id")) {
            log.info("添加 sys_position_category.parent_id 字段");
            jdbcTemplate.execute("ALTER TABLE sys_position_category ADD COLUMN parent_id BIGINT DEFAULT 0");
        }

        // 检查 sort 字段（旧表可能有 sort_order 但无 sort）
        if (!columnExists("sys_position_category", "sort")) {
            log.info("添加 sys_position_category.sort 字段");
            jdbcTemplate.execute("ALTER TABLE sys_position_category ADD COLUMN sort INTEGER DEFAULT 0");
        }

        // 检查 status 字段
        if (!columnExists("sys_position_category", "status")) {
            log.info("添加 sys_position_category.status 字段");
            jdbcTemplate.execute("ALTER TABLE sys_position_category ADD COLUMN status INTEGER DEFAULT 1");
        }

        // 检查 description 字段
        if (!columnExists("sys_position_category", "description")) {
            log.info("添加 sys_position_category.description 字段");
            jdbcTemplate.execute("ALTER TABLE sys_position_category ADD COLUMN description VARCHAR(255)");
        }

        // 检查 create_by 字段
        if (!columnExists("sys_position_category", "create_by")) {
            log.info("添加 sys_position_category.create_by 字段");
            jdbcTemplate.execute("ALTER TABLE sys_position_category ADD COLUMN create_by VARCHAR(64)");
        }

        // 检查 update_by 字段
        if (!columnExists("sys_position_category", "update_by")) {
            log.info("添加 sys_position_category.update_by 字段");
            jdbcTemplate.execute("ALTER TABLE sys_position_category ADD COLUMN update_by VARCHAR(64)");
        }

        // ========== erp_sale_order 表字段（Flyway 被禁用，手动补齐） ==========
        if (tableExists("erp_sale_order")) {
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
                if (!columnExists("erp_sale_order", colName)) {
                    log.info("添加 erp_sale_order.{} 字段", colName);
                    jdbcTemplate.execute("ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS " + colDef);
                }
            }
        }

        // ========== erp_supplier 表字段（由 Flyway 迁移 V1.2.0 定义，但 Flyway 在 dev 配置中被禁用） ==========
        if (tableExists("erp_supplier")) {
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
                if (!columnExists("erp_supplier", colName)) {
                    log.info("添加 erp_supplier.{} 字段", colName);
                    jdbcTemplate.execute("ALTER TABLE erp_supplier ADD COLUMN IF NOT EXISTS " + colDef);
                }
            }
        }
    }
    
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
            String sql = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = '" + tableName + "')";
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
            String passwordHash = BCrypt.hashpw("Admin@123", BCrypt.gensalt());

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
            INSERT INTO sys_tenant (id, tenant_name, tenant_code, status)
            VALUES (1, '默认租户', 'default', 0)
            ON CONFLICT (id) DO NOTHING
        """);
        
        // 生成BCrypt密码哈希
        String passwordHash = BCrypt.hashpw("Admin@123", BCrypt.gensalt());
        
        // 插入默认管理员用户
        jdbcTemplate.update("""
            INSERT INTO sys_user (id, tenant_id, username, password, nickname, status)
            VALUES (1, 1, 'admin', ?, '超级管理员', 0)
            ON CONFLICT (tenant_id, username) DO UPDATE SET password = ?
        """, passwordHash, passwordHash);
        
        // 插入默认角色
        jdbcTemplate.execute("""
            INSERT INTO sys_role (id, tenant_id, role_name, role_code, status)
            VALUES (1, 1, '超级管理员', 'SUPER_ADMIN', 0)
            ON CONFLICT (tenant_id, role_code) DO NOTHING
        """);
        
        // 关联用户和角色
        jdbcTemplate.execute("""
            INSERT INTO sys_user_role (id, user_id, role_id)
            VALUES (1, 1, 1)
            ON CONFLICT DO NOTHING
        """);
        
        log.info("基础表创建完成！Admin密码: Admin@123");
    }
}