-- ============================================================
-- V11.65.0: 统一收货单枚举为前端/文档 0-based 约定
--
-- 背景：
--   收货单前端（wh/receiving-order）按文档以 0 为起点定义枚举：
--     source_type: 0-采购入库 1-生产入库 2-退货入库 3-调拨入库 4-其他
--     priority  : 0-普通 1-紧急 2-加急
--   而 V6.0.0 建表时 source_type/priority 以 1 为起点，且与前端枚举集合
--   不一致（数据库含"盘盈入库"、缺"生产入库/其他"）。
--   该表当前为空（无业务数据），可将默认值与列注释对齐到前端约定。
-- ============================================================

ALTER TABLE wms_receipt_task ALTER COLUMN source_type SET DEFAULT 0;
ALTER TABLE wms_receipt_task ALTER COLUMN priority SET DEFAULT 0;

COMMENT ON COLUMN wms_receipt_task.source_type IS '来源类型 0-采购入库 1-生产入库 2-退货入库 3-调拨入库 4-其他';
COMMENT ON COLUMN wms_receipt_task.priority IS '优先级 0-普通 1-紧急 2-加急';
