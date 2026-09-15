-- ============================================================
-- V11.14.0: 借进借出明细/归还表补 version 乐观锁列
--
-- 背景：
--   WMS 实体统一继承含 @Version 的 BaseEntity，
--   V11.7.0 建表时仅 wms_borrow_order 有 version 列，
--   其余 3 表缺失导致 insert 报 "version 字段不存在"。
-- ============================================================

ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;
ALTER TABLE wms_borrow_return ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;
ALTER TABLE wms_borrow_return_item ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;
