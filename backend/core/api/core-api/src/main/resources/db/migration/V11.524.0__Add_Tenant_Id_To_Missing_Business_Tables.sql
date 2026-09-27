-- ============================================================
-- V11.524.0: 9 张"数据模型缺口"表补 tenant_id 列并回填
--
-- 背景：门禁 `node tools/audit-tenant-ignore-list.cjs` 报出 16 张"被 MyBatis 用到、
--   没有 tenant_id 列、又不在 IGNORE_TENANT_TABLES"的表。逐张判定后：
--     · 9 张属**数据模型缺口**（本该按租户切分，只是漏了列）→ 本迁移补列回填；
--     · 2 张纯技术日志（gateway_log / sys_system_log）→ 转登记忽略清单；
--     · 5 张定价引擎内存对象（erp_pricing_* / erp_discount_rule）→ 另案清理。
--   ⚠️ 这 9 张**不能登记忽略清单了事**：登记 = 所有租户可见，会把"本该隔离的数据"变成公开。
--
--   表                                    ← 回填依据（父表，均有 tenant_id）
--   erp_cost_sharing_expense_item         ← erp_cost_sharing          (cost_sharing_id → id)
--   erp_purchase_exchange_approval_record ← erp_purchase_exchange     (exchange_id → id)
--   fin_reconciliation_item               ← fin_reconciliation        (reconciliation_id → id)
--   kb_document                           ← kb_knowledge_base         (knowledge_base_id → id)
--   kb_document_chunk                     ← kb_document / kb_knowledge_base (document_id / knowledge_base_id)
--   report_schedule_log                   ← report_schedule           (schedule_id → id)
--   supplier_notification_config          ← erp_supplier              (supplier_id → id)
--   supplier_notification_record          ← erp_supplier              (supplier_id → id)
--   sys_file_permission                   ← sys_file                  (file_id → id；其 tenant_id 由 V9.35.0 补)
--
--   最典型的缺口是 `fin_reconciliation_item`：实体 `ReconciliationItem` 已声明 `tenantId` 字段，
--   表里却没有这一列（MP 全字段 insert 直接报「字段 tenant_id 不存在」）。
--
-- 回填策略：按父表关联回填；关联不到的孤儿行兜底置 1（= 系统平台租户，DOMAIN-MODEL §6.5），
--   并逐表 RAISE NOTICE **如实报出兜底行数**（不为 0 说明父表关联异常，需人工核查）。
-- 幂等：ADD COLUMN IF NOT EXISTS + UPDATE ... IS NULL + CREATE INDEX IF NOT EXISTS，可重复执行。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 补列
-- ------------------------------------------------------------
ALTER TABLE erp_cost_sharing_expense_item ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE erp_purchase_exchange_approval_record ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE fin_reconciliation_item ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE kb_document ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE kb_document_chunk ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE report_schedule_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE supplier_notification_config ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE supplier_notification_record ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE sys_file_permission ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

-- ------------------------------------------------------------
-- 2. 回填：继承父表租户
-- ------------------------------------------------------------
UPDATE erp_cost_sharing_expense_item i
SET tenant_id = p.tenant_id
FROM erp_cost_sharing p
WHERE i.cost_sharing_id = p.id AND i.tenant_id IS NULL;

UPDATE erp_purchase_exchange_approval_record i
SET tenant_id = p.tenant_id
FROM erp_purchase_exchange p
WHERE i.exchange_id = p.id AND i.tenant_id IS NULL;

UPDATE fin_reconciliation_item i
SET tenant_id = p.tenant_id
FROM fin_reconciliation p
WHERE i.reconciliation_id = p.id AND i.tenant_id IS NULL;

UPDATE kb_document i
SET tenant_id = p.tenant_id
FROM kb_knowledge_base p
WHERE i.knowledge_base_id = p.id AND i.tenant_id IS NULL;

UPDATE kb_document_chunk i
SET tenant_id = p.tenant_id
FROM kb_document p
WHERE i.document_id = p.id AND i.tenant_id IS NULL;
-- 兜底再走 kb_knowledge_base：document 行缺失时，chunk 自己也有 knowledge_base_id 可定位租户
UPDATE kb_document_chunk i
SET tenant_id = p.tenant_id
FROM kb_knowledge_base p
WHERE i.knowledge_base_id = p.id AND i.tenant_id IS NULL;

UPDATE report_schedule_log i
SET tenant_id = p.tenant_id
FROM report_schedule p
WHERE i.schedule_id = p.id AND i.tenant_id IS NULL;

UPDATE supplier_notification_config i
SET tenant_id = p.tenant_id
FROM erp_supplier p
WHERE i.supplier_id = p.id AND i.tenant_id IS NULL;

UPDATE supplier_notification_record i
SET tenant_id = p.tenant_id
FROM erp_supplier p
WHERE i.supplier_id = p.id AND i.tenant_id IS NULL;

UPDATE sys_file_permission i
SET tenant_id = p.tenant_id
FROM sys_file p
WHERE i.file_id = p.id AND i.tenant_id IS NULL;

-- ------------------------------------------------------------
-- 3. 兜底：父表关联不到的孤儿行置 1（系统平台租户，§6.5），并如实 NOTICE 行数
--    （当前这 9 张表均为 0 行，预期全部 NOTICE 不触发；触发了说明父表关联有问题）
-- ------------------------------------------------------------
DO $$
DECLARE n BIGINT;
BEGIN
    UPDATE erp_cost_sharing_expense_item SET tenant_id = 1 WHERE tenant_id IS NULL;
    GET DIAGNOSTICS n = ROW_COUNT;
    IF n > 0 THEN RAISE NOTICE 'V11.524.0 兜底: erp_cost_sharing_expense_item 关联不到父表, % 行置 tenant_id=1', n; END IF;

    UPDATE erp_purchase_exchange_approval_record SET tenant_id = 1 WHERE tenant_id IS NULL;
    GET DIAGNOSTICS n = ROW_COUNT;
    IF n > 0 THEN RAISE NOTICE 'V11.524.0 兜底: erp_purchase_exchange_approval_record 关联不到父表, % 行置 tenant_id=1', n; END IF;

    UPDATE fin_reconciliation_item SET tenant_id = 1 WHERE tenant_id IS NULL;
    GET DIAGNOSTICS n = ROW_COUNT;
    IF n > 0 THEN RAISE NOTICE 'V11.524.0 兜底: fin_reconciliation_item 关联不到父表, % 行置 tenant_id=1', n; END IF;

    UPDATE kb_document SET tenant_id = 1 WHERE tenant_id IS NULL;
    GET DIAGNOSTICS n = ROW_COUNT;
    IF n > 0 THEN RAISE NOTICE 'V11.524.0 兜底: kb_document 关联不到父表, % 行置 tenant_id=1', n; END IF;

    UPDATE kb_document_chunk SET tenant_id = 1 WHERE tenant_id IS NULL;
    GET DIAGNOSTICS n = ROW_COUNT;
    IF n > 0 THEN RAISE NOTICE 'V11.524.0 兜底: kb_document_chunk 关联不到父表, % 行置 tenant_id=1', n; END IF;

    UPDATE report_schedule_log SET tenant_id = 1 WHERE tenant_id IS NULL;
    GET DIAGNOSTICS n = ROW_COUNT;
    IF n > 0 THEN RAISE NOTICE 'V11.524.0 兜底: report_schedule_log 关联不到父表, % 行置 tenant_id=1', n; END IF;

    UPDATE supplier_notification_config SET tenant_id = 1 WHERE tenant_id IS NULL;
    GET DIAGNOSTICS n = ROW_COUNT;
    IF n > 0 THEN RAISE NOTICE 'V11.524.0 兜底: supplier_notification_config 关联不到父表, % 行置 tenant_id=1', n; END IF;

    UPDATE supplier_notification_record SET tenant_id = 1 WHERE tenant_id IS NULL;
    GET DIAGNOSTICS n = ROW_COUNT;
    IF n > 0 THEN RAISE NOTICE 'V11.524.0 兜底: supplier_notification_record 关联不到父表, % 行置 tenant_id=1', n; END IF;

    UPDATE sys_file_permission SET tenant_id = 1 WHERE tenant_id IS NULL;
    GET DIAGNOSTICS n = ROW_COUNT;
    IF n > 0 THEN RAISE NOTICE 'V11.524.0 兜底: sys_file_permission 关联不到父表, % 行置 tenant_id=1', n; END IF;
END $$;

-- ------------------------------------------------------------
-- 4. 索引（与父表关联查询同用 tenant_id 过滤）
-- ------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_erp_cost_sharing_expense_item_tenant ON erp_cost_sharing_expense_item (tenant_id);
CREATE INDEX IF NOT EXISTS idx_erp_purchase_exchange_approval_record_tenant ON erp_purchase_exchange_approval_record (tenant_id);
CREATE INDEX IF NOT EXISTS idx_fin_reconciliation_item_tenant ON fin_reconciliation_item (tenant_id);
CREATE INDEX IF NOT EXISTS idx_kb_document_tenant ON kb_document (tenant_id);
CREATE INDEX IF NOT EXISTS idx_kb_document_chunk_tenant ON kb_document_chunk (tenant_id);
CREATE INDEX IF NOT EXISTS idx_report_schedule_log_tenant ON report_schedule_log (tenant_id);
CREATE INDEX IF NOT EXISTS idx_supplier_notification_config_tenant ON supplier_notification_config (tenant_id);
CREATE INDEX IF NOT EXISTS idx_supplier_notification_record_tenant ON supplier_notification_record (tenant_id);
CREATE INDEX IF NOT EXISTS idx_sys_file_permission_tenant ON sys_file_permission (tenant_id);

-- ------------------------------------------------------------
-- 5. 自检：断言**不变量**（列存在 + 无 NULL），不做"跨表行数相等"那类快照断言
-- ------------------------------------------------------------
DO $$
DECLARE missing INT;
DECLARE null_rows BIGINT;
BEGIN
    SELECT count(*) INTO missing FROM information_schema.columns
     WHERE table_schema = 'public' AND column_name = 'tenant_id'
       AND table_name IN ('erp_cost_sharing_expense_item', 'erp_purchase_exchange_approval_record',
                          'fin_reconciliation_item', 'kb_document', 'kb_document_chunk',
                          'report_schedule_log', 'supplier_notification_config',
                          'supplier_notification_record', 'sys_file_permission');
    IF missing <> 9 THEN
        RAISE EXCEPTION 'V11.524.0 自检失败：应补 9 列 tenant_id，实际只找到 % 列', missing;
    END IF;

    SELECT count(*) INTO null_rows FROM (
        SELECT 1 FROM erp_cost_sharing_expense_item WHERE tenant_id IS NULL
        UNION ALL SELECT 1 FROM erp_purchase_exchange_approval_record WHERE tenant_id IS NULL
        UNION ALL SELECT 1 FROM fin_reconciliation_item WHERE tenant_id IS NULL
        UNION ALL SELECT 1 FROM kb_document WHERE tenant_id IS NULL
        UNION ALL SELECT 1 FROM kb_document_chunk WHERE tenant_id IS NULL
        UNION ALL SELECT 1 FROM report_schedule_log WHERE tenant_id IS NULL
        UNION ALL SELECT 1 FROM supplier_notification_config WHERE tenant_id IS NULL
        UNION ALL SELECT 1 FROM supplier_notification_record WHERE tenant_id IS NULL
        UNION ALL SELECT 1 FROM sys_file_permission WHERE tenant_id IS NULL
    ) x;
    IF null_rows > 0 THEN
        RAISE EXCEPTION 'V11.524.0 自检失败：9 张表仍有 % 行 tenant_id IS NULL', null_rows;
    END IF;

    RAISE NOTICE 'V11.524.0 自检通过：9 张表 tenant_id 列齐备且无 NULL，索引已建';
END $$;
