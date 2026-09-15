-- ============================================================
-- V6.17.0: 创建租户套餐、配额、开发模板表
-- 替换此前内存模拟数据的真实数据库表
-- ============================================================

-- 1. 租户套餐表
CREATE TABLE IF NOT EXISTS sys_tenant_package (
    id BIGSERIAL PRIMARY KEY,
    package_name VARCHAR(100) NOT NULL,
    package_code VARCHAR(50) NOT NULL UNIQUE,
    purchase_type VARCHAR(20) DEFAULT 'monthly',
    price BIGINT DEFAULT 0,
    max_users INT DEFAULT 10,
    storage_quota INT DEFAULT 10,
    api_call_limit INT DEFAULT 10000,
    status INT DEFAULT 1,
    description VARCHAR(500),
    sort_order INT DEFAULT 0,
    tenant_id BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    deleted INT DEFAULT 0
);
COMMENT ON TABLE sys_tenant_package IS '租户套餐表';
COMMENT ON COLUMN sys_tenant_package.purchase_type IS '购买类型: monthly/yearly/perpetual';
COMMENT ON COLUMN sys_tenant_package.price IS '价格（分）';
COMMENT ON COLUMN sys_tenant_package.status IS '状态: 1=启用 0=停用';

-- 2. 租户配额表
CREATE TABLE IF NOT EXISTS sys_tenant_quota (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_name VARCHAR(100),
    tenant_code VARCHAR(50),
    max_users INT DEFAULT 10,
    max_storage VARCHAR(20) DEFAULT '10GB',
    max_api_calls INT DEFAULT 10000,
    used_users INT DEFAULT 0,
    used_storage VARCHAR(20) DEFAULT '0GB',
    used_api_calls INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE sys_tenant_quota IS '租户配额表';
COMMENT ON COLUMN sys_tenant_quota.max_storage IS '最大存储配额';
COMMENT ON COLUMN sys_tenant_quota.used_storage IS '已用存储量';

-- 3. 开发模板表
CREATE TABLE IF NOT EXISTS dev_template (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50),
    type VARCHAR(50) DEFAULT 'entity',
    content TEXT,
    description VARCHAR(500),
    version VARCHAR(20) DEFAULT '1.0',
    enabled BOOLEAN DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);
COMMENT ON TABLE dev_template IS '开发模板表';
COMMENT ON COLUMN dev_template.type IS '模板类型: entity/controller/service/mapper/frontend';

-- ============================================================
-- 种子数据
-- ============================================================

-- sys_tenant_package 初始套餐
INSERT INTO sys_tenant_package (package_name, package_code, purchase_type, price, max_users, storage_quota, api_call_limit, status, description, sort_order)
VALUES
('基础版', 'basic', 'monthly', 9900, 10, 5, 10000, 1, '适合初创团队的基础功能套餐', 1),
('专业版', 'professional', 'yearly', 99900, 50, 50, 100000, 1, '适合成长型企业的专业功能套餐', 2),
('企业版', 'enterprise', 'perpetual', 499900, 200, 500, 1000000, 1, '适合大型企业的全功能套餐', 3)
ON CONFLICT (package_code) DO NOTHING;

-- sys_tenant_quota 初始配额
INSERT INTO sys_tenant_quota (tenant_id, tenant_name, tenant_code, max_users, max_storage, max_api_calls, used_users, used_storage, used_api_calls)
VALUES
(1, '默认租户', 'DEFAULT', 10, '10GB', 10000, 3, '3GB', 2500),
(2, '演示租户', 'DEMO', 20, '50GB', 50000, 5, '8GB', 12000),
(3, '测试租户', 'TEST', 5, '5GB', 5000, 2, '1GB', 800);

-- dev_template 初始模板
INSERT INTO dev_template (name, code, type, content, description, version, enabled) VALUES
('实体类模板', 'entity', 'entity', 'package ${package};\n\npublic class ${className} {\n    private ${idType} id;\n}', '标准JPA实体类模板', '1.0', TRUE),
('Controller模板', 'controller', 'controller', 'package ${package};\n\n@RestController\n@RequestMapping("/api/${module}")\npublic class ${className}Controller {\n}', '标准REST Controller模板', '1.0', TRUE),
('Service模板', 'service', 'service', 'package ${package};\n\n@Service\npublic class ${className}ServiceImpl implements ${className}Service {\n}', '标准Service实现模板', '1.0', TRUE),
('Mapper模板', 'mapper', 'mapper', 'package ${package};\n\n@Mapper\npublic interface ${className}Mapper extends BaseMapper<${entity}> {\n}', '标准MyBatis-Plus Mapper模板', '1.0', TRUE),
('Vue页面模板', 'vue-page', 'frontend', '<template>\n  <PageContainer>\n  </PageContainer>\n</template>', '标准Vue管理页面模板', '1.1', TRUE),
('Vue表单模板', 'vue-form', 'frontend', '<template>\n  <a-form :model="form" layout="vertical">\n  </a-form>\n</template>', '标准Vue表单模板', '1.0', TRUE),
('API接口模板', 'api-ts', 'frontend', 'import request from ''@/utils/request'';\n\nexport const ${module}Api = {\n};', '标准TypeScript API层模板', '1.0', FALSE);
