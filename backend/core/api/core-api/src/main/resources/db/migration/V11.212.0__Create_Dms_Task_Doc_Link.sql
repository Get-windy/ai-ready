-- 配送单 ← 上游单据 结构化关联（配送单 = 运输执行单，货权与金额口径归上游）
--
-- 建模依据（业界）：SAP TM 三层 —— Delivery（ERP/LE 出库单，管货权/库存/金额）
--   → Freight Unit（要运什么，携带重量/体积/件数）→ Freight Order（怎么运：承运商/车辆/路线/
--   停靠点/计划与实际时间/费用）。SAP 官方明确 TM 单据不与 SD/MM 单据做 1:1 映射、
--   不重复存放销售单据行项目；即时配送（达达/美团）同样只在配送单上放描述性物品信息。
--
-- 本迁移对应「演进第二步」：把原先「配送单自己存商品明细 + 单价金额，并由明细汇总表头金额」
--   的双口径模型，改为「配送单 = 一批上游单据的运输批次」：
--     · 本表存 配送单 ↔ 上游单据 的 1:N 关联（单据号 + 该单据的数量/金额/重量/体积/箱数快照）
--     · 表头 发货数量(dms_task.total_quantity)/发货金额(goods_amount)/重量(total_weight)/
--       体积(total_volume) 一律由本表聚合，保证与上游单据同口径
--     · 商品明细不再由配送单录入，改为「查看」时穿透上游单据明细（单一数据源）
--
-- 兼容：无上游单据的临时配送仍可走 dms_task_item（定位为装载/货物描述，见第三步）。
-- 幂等：IF NOT EXISTS。

CREATE TABLE IF NOT EXISTS dms_task_doc (
    id            BIGSERIAL      PRIMARY KEY,
    tenant_id     BIGINT         NOT NULL DEFAULT 0,
    task_id       BIGINT         NOT NULL,
    -- 1-销售出库单 2-销售退货单 3-调拨单
    doc_type      SMALLINT       NOT NULL DEFAULT 1,
    doc_id        BIGINT,
    doc_no        VARCHAR(64)    NOT NULL,
    doc_date      DATE,
    customer_name VARCHAR(200),
    quantity      NUMERIC(18, 4) NOT NULL DEFAULT 0,
    amount        NUMERIC(18, 4) NOT NULL DEFAULT 0,
    weight        NUMERIC(18, 4) NOT NULL DEFAULT 0,
    volume        NUMERIC(18, 4) NOT NULL DEFAULT 0,
    box_count     INTEGER        NOT NULL DEFAULT 0,
    remark        VARCHAR(500),
    deleted       INTEGER        DEFAULT 0,
    create_time   TIMESTAMP      DEFAULT NOW(),
    update_time   TIMESTAMP      DEFAULT NOW(),
    create_by     BIGINT,
    update_by     BIGINT,
    version       INTEGER        DEFAULT 0
);

COMMENT ON TABLE  dms_task_doc              IS '配送单内上游单据关联（1:N；配送单表头数量/金额/重量/体积的唯一聚合来源）';
COMMENT ON COLUMN dms_task_doc.doc_type     IS '1-销售出库单 2-销售退货单 3-调拨单';
COMMENT ON COLUMN dms_task_doc.doc_id       IS '上游单据主键（用于穿透查看明细）';
COMMENT ON COLUMN dms_task_doc.doc_no       IS '上游单据编号（销售出库单 XSCKD- / XSCK…）';
COMMENT ON COLUMN dms_task_doc.quantity     IS '该单据的发货数量快照';
COMMENT ON COLUMN dms_task_doc.amount       IS '该单据的发货金额快照（货值口径归上游，配送单不重算）';
COMMENT ON COLUMN dms_task_doc.box_count    IS '该单据的装箱数量快照';

CREATE INDEX IF NOT EXISTS idx_dms_task_doc_task ON dms_task_doc (tenant_id, task_id);
CREATE INDEX IF NOT EXISTS idx_dms_task_doc_no   ON dms_task_doc (doc_no);

-- 历史配送单：把已有的「来源单据编号」字符串（可能多单号，逗号/顿号/分号/空格分隔）回填成关联行，
-- 聚合口径与旧字符串解析保持一致（数量/金额等留 0，避免臆造上游数据；重新保存时按真实上游刷新）。
INSERT INTO dms_task_doc (tenant_id, task_id, doc_type, doc_no, customer_name, create_time, update_time)
SELECT COALESCE(t.tenant_id, 0), t.id, 1, TRIM(part), t.customer_name, NOW(), NOW()
FROM dms_task t
CROSS JOIN LATERAL regexp_split_to_table(t.source_bill_no, '[,，、;；\s]+') AS part
WHERE t.deleted = 0
  AND t.source_bill_no IS NOT NULL
  AND TRIM(part) <> ''
  AND NOT EXISTS (
      SELECT 1 FROM dms_task_doc d
      WHERE d.task_id = t.id AND d.doc_no = TRIM(part) AND d.deleted = 0
  );
