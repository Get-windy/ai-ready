-- ═══════════════════════════════════════════════════════════════════
-- V11.57.0 成本调价单补列（对标报溢单/报损单结构）
-- 成本调价单：单号前缀 CBTJD-，仅调成本不动数量
-- 记账后库存商品成本单价由调前成本价改为调后成本价
-- ═══════════════════════════════════════════════════════════════════

-- ── 主表：补经手人/部门/摘要/附件/记账/制单/打印/取消原因 ──
ALTER TABLE erp_stock_cost_adjust ADD COLUMN IF NOT EXISTS handler_id BIGINT;
ALTER TABLE erp_stock_cost_adjust ADD COLUMN IF NOT EXISTS handler_name VARCHAR(64);
ALTER TABLE erp_stock_cost_adjust ADD COLUMN IF NOT EXISTS dept_id BIGINT;
ALTER TABLE erp_stock_cost_adjust ADD COLUMN IF NOT EXISTS dept_name VARCHAR(100);
ALTER TABLE erp_stock_cost_adjust ADD COLUMN IF NOT EXISTS summary VARCHAR(500);
ALTER TABLE erp_stock_cost_adjust ADD COLUMN IF NOT EXISTS attachment VARCHAR(1000);
ALTER TABLE erp_stock_cost_adjust ADD COLUMN IF NOT EXISTS bookkeeper_id BIGINT;
ALTER TABLE erp_stock_cost_adjust ADD COLUMN IF NOT EXISTS bookkeeper_name VARCHAR(64);
ALTER TABLE erp_stock_cost_adjust ADD COLUMN IF NOT EXISTS bookkeeping_time TIMESTAMP;
ALTER TABLE erp_stock_cost_adjust ADD COLUMN IF NOT EXISTS creator_name VARCHAR(64);
ALTER TABLE erp_stock_cost_adjust ADD COLUMN IF NOT EXISTS print_count INT DEFAULT 0;
ALTER TABLE erp_stock_cost_adjust ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(200);

CREATE INDEX IF NOT EXISTS idx_stock_cost_adjust_handler  ON erp_stock_cost_adjust (handler_id);
CREATE INDEX IF NOT EXISTS idx_stock_cost_adjust_dept     ON erp_stock_cost_adjust (dept_id);
CREATE INDEX IF NOT EXISTS idx_stock_cost_adjust_no       ON erp_stock_cost_adjust (adjust_no);
CREATE INDEX IF NOT EXISTS idx_stock_cost_adjust_status   ON erp_stock_cost_adjust (status);

-- ── 明细表：补租户/口味/型号/产地/品牌 ──
ALTER TABLE erp_stock_cost_adjust_item ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT 1;
ALTER TABLE erp_stock_cost_adjust_item ADD COLUMN IF NOT EXISTS taste VARCHAR(64);
ALTER TABLE erp_stock_cost_adjust_item ADD COLUMN IF NOT EXISTS model VARCHAR(64);
ALTER TABLE erp_stock_cost_adjust_item ADD COLUMN IF NOT EXISTS origin VARCHAR(64);
ALTER TABLE erp_stock_cost_adjust_item ADD COLUMN IF NOT EXISTS brand VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_stock_cost_adjust_item_parent  ON erp_stock_cost_adjust_item (adjust_id);
CREATE INDEX IF NOT EXISTS idx_stock_cost_adjust_item_product ON erp_stock_cost_adjust_item (product_id);
