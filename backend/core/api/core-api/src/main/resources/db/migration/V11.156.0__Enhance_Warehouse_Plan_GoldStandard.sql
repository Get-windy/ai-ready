-- =============================================================================
-- 仓库规划 金标准升级（对标 ql361：资料 → 仓库管理 → 仓库规划）
--   1. erp_warehouse 补列（所属分类/父级/助记码/邮编/排序），status 归一为整数 0/1
--   2. 新增 erp_warehouse_category 仓库分类树（复制 erp_product_category 范式）
--   3. wms_location 补列（停用开关 / 系统内置标记 / 仓库名称快照）
-- 迁移版本：V11.156.0
-- =============================================================================

-- ── 1. 仓库主数据（erp_warehouse）补列 ──────────────────────────────────────
ALTER TABLE erp_warehouse ADD COLUMN IF NOT EXISTS parent_id BIGINT DEFAULT 0;
ALTER TABLE erp_warehouse ADD COLUMN IF NOT EXISTS category_id BIGINT DEFAULT 0;
ALTER TABLE erp_warehouse ADD COLUMN IF NOT EXISTS easy_code VARCHAR(50);
ALTER TABLE erp_warehouse ADD COLUMN IF NOT EXISTS zip_code VARCHAR(20);
ALTER TABLE erp_warehouse ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;

COMMENT ON COLUMN erp_warehouse.parent_id IS '上级仓库ID（0=顶级，用于“显示层次结构”树形展示）';
COMMENT ON COLUMN erp_warehouse.category_id IS '所属分类ID（erp_warehouse_category.id，0=未分类）';
COMMENT ON COLUMN erp_warehouse.easy_code IS '助记码';
COMMENT ON COLUMN erp_warehouse.zip_code IS '邮编';
COMMENT ON COLUMN erp_warehouse.sort_order IS '排序号（升序）';

-- status 由 VARCHAR('ENABLED') 归一为整数 1-启用 0-停用（全仓库仅种子数据写入，无读取点）
ALTER TABLE erp_warehouse ALTER COLUMN status DROP DEFAULT;
ALTER TABLE erp_warehouse ALTER COLUMN status TYPE INTEGER
    USING (CASE WHEN status::text IN ('ENABLED', '1') THEN 1 ELSE 0 END);
ALTER TABLE erp_warehouse ALTER COLUMN status SET DEFAULT 1;
COMMENT ON COLUMN erp_warehouse.status IS '状态 1-启用 0-停用';

CREATE INDEX IF NOT EXISTS idx_erp_warehouse_category ON erp_warehouse(category_id);
CREATE INDEX IF NOT EXISTS idx_erp_warehouse_parent ON erp_warehouse(parent_id);
CREATE INDEX IF NOT EXISTS idx_erp_warehouse_code ON erp_warehouse(tenant_id, warehouse_code);

-- ── 2. 仓库分类树 ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS erp_warehouse_category (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT DEFAULT 1,
    category_code   VARCHAR(50),
    category_name   VARCHAR(100) NOT NULL,
    parent_id       BIGINT DEFAULT 0,
    category_level  INTEGER DEFAULT 1,
    sort_order      INTEGER DEFAULT 0,
    status          INTEGER DEFAULT 1,
    remark          VARCHAR(500),
    deleted         INTEGER DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by       BIGINT,
    update_by       BIGINT
);

COMMENT ON TABLE erp_warehouse_category IS '仓库分类树（资料 → 仓库管理 → 仓库规划 左侧树）';
COMMENT ON COLUMN erp_warehouse_category.parent_id IS '上级分类ID（0=顶级）';
COMMENT ON COLUMN erp_warehouse_category.category_level IS '层级（1=顶级）';
COMMENT ON COLUMN erp_warehouse_category.status IS '状态 1-启用 0-停用';

CREATE INDEX IF NOT EXISTS idx_wh_category_parent ON erp_warehouse_category(parent_id);
CREATE INDEX IF NOT EXISTS idx_wh_category_tenant ON erp_warehouse_category(tenant_id);

-- ── 3. 货位（wms_location）补列 ─────────────────────────────────────────────
ALTER TABLE wms_location ADD COLUMN IF NOT EXISTS is_enabled INTEGER DEFAULT 1;
ALTER TABLE wms_location ADD COLUMN IF NOT EXISTS is_builtin INTEGER DEFAULT 0;
ALTER TABLE wms_location ADD COLUMN IF NOT EXISTS warehouse_name VARCHAR(200);

COMMENT ON COLUMN wms_location.is_enabled IS '是否启用 1-启用 0-停用（对标“显示停用”勾选项）';
COMMENT ON COLUMN wms_location.is_builtin IS '是否系统内置 1-内置（不可编辑/删除） 0-可维护';
COMMENT ON COLUMN wms_location.warehouse_name IS '所属仓库名称快照（列表展示用）';

-- 存量数据补齐：仓库名称快照
UPDATE wms_location l
   SET warehouse_name = w.warehouse_name
  FROM erp_warehouse w
 WHERE l.warehouse_id = w.id
   AND (l.warehouse_name IS NULL OR l.warehouse_name = '');
