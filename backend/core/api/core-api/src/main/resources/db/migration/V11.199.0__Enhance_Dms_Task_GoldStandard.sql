-- 配送单（配送 → 配送业务 → 配送单[历史]）金标准增强
--
-- 背景：《配送单开发文档》对标状态 = ql361 无此页面（配发收→配送业务下只有「配送查询」），
--   本单据为本系统新增、按自主建模。数据落地 = dms_task（配送任务），本迁移补齐
--   列表台账列（对齐《配送查询》口径）+ 商品明细表 + 打印/制单人审计列。
-- 红线：不改动已有列语义（sign/settlement/payment/orderpool/execution/dispatch 均依赖 dms_task），
--   只做 ADD COLUMN IF NOT EXISTS 与新建明细表。

-- ─────────────────────────────────────────────
-- 1) dms_task 台账列补全
-- ─────────────────────────────────────────────
ALTER TABLE dms_task ADD COLUMN IF NOT EXISTS delivery_date     DATE;
ALTER TABLE dms_task ADD COLUMN IF NOT EXISTS rider_name        VARCHAR(100);
ALTER TABLE dms_task ADD COLUMN IF NOT EXISTS vehicle_id        BIGINT;
ALTER TABLE dms_task ADD COLUMN IF NOT EXISTS vehicle_name      VARCHAR(100);
ALTER TABLE dms_task ADD COLUMN IF NOT EXISTS deliveryman_id    BIGINT;
ALTER TABLE dms_task ADD COLUMN IF NOT EXISTS deliveryman_name  VARCHAR(100);
ALTER TABLE dms_task ADD COLUMN IF NOT EXISTS route_id          BIGINT;
ALTER TABLE dms_task ADD COLUMN IF NOT EXISTS route_area        VARCHAR(500);
ALTER TABLE dms_task ADD COLUMN IF NOT EXISTS order_count       INTEGER DEFAULT 0;
ALTER TABLE dms_task ADD COLUMN IF NOT EXISTS deposit_amount    NUMERIC(18,4) DEFAULT 0;
ALTER TABLE dms_task ADD COLUMN IF NOT EXISTS return_order_count INTEGER DEFAULT 0;
ALTER TABLE dms_task ADD COLUMN IF NOT EXISTS return_quantity   NUMERIC(18,4) DEFAULT 0;
ALTER TABLE dms_task ADD COLUMN IF NOT EXISTS return_amount     NUMERIC(18,4) DEFAULT 0;
ALTER TABLE dms_task ADD COLUMN IF NOT EXISTS box_quantity      NUMERIC(18,4) DEFAULT 0;
ALTER TABLE dms_task ADD COLUMN IF NOT EXISTS print_count       INTEGER DEFAULT 0;
ALTER TABLE dms_task ADD COLUMN IF NOT EXISTS creator_name      VARCHAR(100);
ALTER TABLE dms_task ADD COLUMN IF NOT EXISTS source_bill_no    VARCHAR(50);

COMMENT ON COLUMN dms_task.delivery_date      IS '指定配送日期（计划配送日，配送查询台账首列）';
COMMENT ON COLUMN dms_task.rider_name         IS '司机名称快照（司机与送货员是两个角色）';
COMMENT ON COLUMN dms_task.vehicle_id         IS '配送车辆ID（dms_vehicle.id）';
COMMENT ON COLUMN dms_task.vehicle_name       IS '配送车辆快照（车牌号）';
COMMENT ON COLUMN dms_task.deliveryman_id     IS '送货员ID（dms_rider.id；送货员与司机同为配送员档案，同一人可分别担任）';
COMMENT ON COLUMN dms_task.deliveryman_name   IS '送货员名称快照';
COMMENT ON COLUMN dms_task.route_id           IS '配送线路ID（erp_route.id，线路主数据）';
COMMENT ON COLUMN dms_task.route_area         IS '配送区域（线路档案配送区域快照）';
COMMENT ON COLUMN dms_task.order_count        IS '配送单量（本任务聚合的配送单据数）';
COMMENT ON COLUMN dms_task.deposit_amount     IS '订金金额（正向汇总）';
COMMENT ON COLUMN dms_task.return_order_count IS '退货单量（回流汇总）';
COMMENT ON COLUMN dms_task.return_quantity    IS '退货数量（回流汇总）';
COMMENT ON COLUMN dms_task.return_amount      IS '退货金额（回流汇总）';
COMMENT ON COLUMN dms_task.box_quantity       IS '装箱数量（任务维度装箱总量）';
COMMENT ON COLUMN dms_task.print_count        IS '打印次数';
COMMENT ON COLUMN dms_task.creator_name       IS '制单人名称快照';
COMMENT ON COLUMN dms_task.source_bill_no     IS '来源单据编号（与订单号区分：任务内单据可一对多）';

CREATE INDEX IF NOT EXISTS idx_dms_task_delivery_date ON dms_task (delivery_date);
CREATE INDEX IF NOT EXISTS idx_dms_task_tenant_status ON dms_task (tenant_id, status);

-- ─────────────────────────────────────────────
-- 2) 配送单商品明细
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS dms_task_item (
    id           BIGSERIAL    PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL DEFAULT 0,
    task_id      BIGINT       NOT NULL,
    line_no      INTEGER      NOT NULL DEFAULT 1,
    product_id   BIGINT,
    product_code VARCHAR(64),
    product_name VARCHAR(200),
    barcode      VARCHAR(64),
    spec         VARCHAR(200),
    unit         VARCHAR(20),
    quantity     NUMERIC(18,4) DEFAULT 0,
    unit_price   NUMERIC(18,4) DEFAULT 0,
    amount       NUMERIC(18,4) DEFAULT 0,
    weight       NUMERIC(18,4) DEFAULT 0,
    volume       NUMERIC(18,4) DEFAULT 0,
    remark       VARCHAR(500),
    deleted      SMALLINT     NOT NULL DEFAULT 0,
    create_by    BIGINT,
    create_time  TIMESTAMP    DEFAULT NOW(),
    update_by    BIGINT,
    update_time  TIMESTAMP    DEFAULT NOW()
);

COMMENT ON TABLE dms_task_item IS '配送单商品明细（dms_task 的明细行）';
COMMENT ON COLUMN dms_task_item.task_id    IS '配送任务ID（dms_task.id）';
COMMENT ON COLUMN dms_task_item.line_no    IS '行号（从 1 开始）';
COMMENT ON COLUMN dms_task_item.amount     IS '金额 = 数量 × 单价（后端按明细重新计算）';
COMMENT ON COLUMN dms_task_item.weight     IS '重量(kg)（数量 × 商品单位重量，无主数据时为 0）';
COMMENT ON COLUMN dms_task_item.volume     IS '体积(m³)（数量 × 商品单位体积，无主数据时为 0）';

CREATE INDEX IF NOT EXISTS idx_dms_task_item_task ON dms_task_item (task_id);
CREATE INDEX IF NOT EXISTS idx_dms_task_item_tenant ON dms_task_item (tenant_id);

-- 历史任务补 delivery_date（以创建日期回填，避免台账「指定配送日期」整列空白）
UPDATE dms_task SET delivery_date = create_time::date WHERE delivery_date IS NULL AND create_time IS NOT NULL;
UPDATE dms_task SET print_count = 0 WHERE print_count IS NULL;
