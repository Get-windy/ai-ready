# 数据库迁移说明

## 手动执行SQL脚本

为了完成CRM-ERP客户关系管理的完整实现，需要手动执行以下SQL脚本来创建CRM-ERP客户映射表。

### 执行步骤

1. **连接到PostgreSQL数据库**
   - 服务器: localhost:5432
   - 数据库: devdb
   - 用户: devuser (或相应用户)

2. **执行数据库迁移脚本**
   ```bash
   psql -h localhost -p 5432 -U devuser -d devdb -f DB_MIGRATION_CRM_MAPPING.sql
   ```

或者使用以下方式：

```bash
# 使用psql命令行
psql -h localhost -p 5432 -U devuser -d devdb -c "\i DB_MIGRATION_CRM_MAPPING.sql"

# 或者直接复制并粘贴以下SQL内容到数据库客户端执行
```

### SQL脚本内容

```sql
-- 创建CRM-ERP客户映射表
-- 用于维护CRM客户/线索与ERP实体客户(Partner)之间的映射关系

CREATE TABLE IF NOT EXISTS crm_erp_customer_mapping (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,

    -- CRM侧信息
    crm_system VARCHAR(50) NOT NULL COMMENT 'CRM系统标识',
    crm_entity_type VARCHAR(50) NOT NULL COMMENT 'CRM实体类型 (CUSTOMER/LEAD/CONTACT等)',
    crm_id BIGINT NOT NULL COMMENT 'CRM系统中的实体ID',

    -- ERP侧信息
    erp_system VARCHAR(50) NOT NULL COMMENT 'ERP系统标识',
    erp_entity_type VARCHAR(50) NOT NULL COMMENT 'ERP实体类型 (PARTNER等)',
    erp_id BIGINT NOT NULL COMMENT 'ERP系统中的实体ID',

    -- 匹配相关信息
    match_rule VARCHAR(50) COMMENT '匹配规则 (TAX_NO/NAME_PHONE/MANUAL等)',
    match_confidence DECIMAL(5,4) COMMENT '匹配置信度',

    -- 同步状态
    sync_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '同步状态 (PENDING/SYNCED/FAILED/MANUAL_OVERRIDE)',
    sync_time TIMESTAMP WITH TIME ZONE COMMENT '同步时间',

    -- 扩展信息
    ext_info JSONB COMMENT '扩展信息',

    -- 标准字段
    deleted INTEGER DEFAULT 0 COMMENT '删除标记',
    create_by BIGINT COMMENT '创建人ID',
    create_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人ID',
    update_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 创建索引以优化查询性能
CREATE INDEX IF NOT EXISTS idx_crm_mapping_crm_info ON crm_erp_customer_mapping(crm_system, crm_entity_type, crm_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_crm_mapping_erp_info ON crm_erp_customer_mapping(erp_system, erp_entity_type, erp_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_crm_mapping_sync_status ON crm_erp_customer_mapping(sync_status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_crm_mapping_tenant ON crm_erp_customer_mapping(tenant_id) WHERE deleted = 0;

-- 添加表注释
COMMENT ON TABLE crm_erp_customer_mapping IS 'CRM-ERP客户映射表 - 维护CRM客户/线索与ERP实体客户之间的映射关系';
COMMENT ON COLUMN crm_erp_customer_mapping.id IS '主键ID';
COMMENT ON COLUMN crm_erp_customer_mapping.tenant_id IS '租户ID';
COMMENT ON COLUMN crm_erp_customer_mapping.crm_system IS 'CRM系统标识';
COMMENT ON COLUMN crm_erp_customer_mapping.crm_entity_type IS 'CRM实体类型';
COMMENT ON COLUMN crm_erp_customer_mapping.crm_id IS 'CRM系统中的实体ID';
COMMENT ON COLUMN crm_erp_customer_mapping.erp_system IS 'ERP系统标识';
COMMENT ON COLUMN crm_erp_customer_mapping.erp_entity_type IS 'ERP实体类型';
COMMENT ON COLUMN crm_erp_customer_mapping.erp_id IS 'ERP系统中的实体ID';
COMMENT ON COLUMN crm_erp_customer_mapping.match_rule IS '匹配规则';
COMMENT ON COLUMN crm_erp_customer_mapping.match_confidence IS '匹配置信度';
COMMENT ON COLUMN crm_erp_customer_mapping.sync_status IS '同步状态';
COMMENT ON COLUMN crm_erp_customer_mapping.sync_time IS '同步时间';
COMMENT ON COLUMN crm_erp_customer_mapping.ext_info IS '扩展信息(JSON)';
COMMENT ON COLUMN crm_erp_customer_mapping.deleted IS '删除标记';
COMMENT ON COLUMN crm_erp_customer_mapping.create_by IS '创建人ID';
COMMENT ON COLUMN crm_erp_customer_mapping.create_time IS '创建时间';
COMMENT ON COLUMN crm_erp_customer_mapping.update_by IS '更新人ID';
COMMENT ON COLUMN crm_erp_customer_mapping.update_time IS '更新时间';
```

### 注意事项

1. **备份数据库**：在执行任何结构变更前，请确保对数据库进行了完整备份
2. **检查数据库连接**：确保PostgreSQL服务正在运行
3. **验证权限**：确保您具有执行DDL语句的足够权限
4. **业务时段**：建议在业务低峰期执行，因为DDL操作可能会锁定表

### 验证步骤

执行完成后，可以通过以下SQL验证表是否创建成功：

```sql
-- 检查表是否存在
SELECT table_name FROM information_schema.tables WHERE table_name = 'crm_erp_customer_mapping';

-- 检查字段是否正确创建
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'crm_erp_customer_mapping'
ORDER BY ordinal_position;
```

完成数据库表的创建后，系统将完全支持CRM-ERP客户关系管理功能。