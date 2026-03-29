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
            // 使用Hutool BCrypt生成密码哈希
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
            INSERT INTO sys_tenant (id, tenant_name, tenant_code, status)
            VALUES (1, '默认租户', 'default', 0)
            ON CONFLICT (id) DO NOTHING
        """);
        
        // 生成BCrypt密码哈希
        String passwordHash = BCrypt.hashpw("admin123", BCrypt.gensalt());
        
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
        
        log.info("基础表创建完成！Admin密码: admin123");
    }
}