-- 预收款单 status 与后端代码约定一致改为字符串
-- 后端 PreReceiptServiceImpl/PreReceiptMapper 均以 String 状态（draft/confirmed/received/offset/forfeited/refunded）
-- 与各业务单据（Receipt/Payment 用 Integer 枚举）不同，预收单为字符串状态机。
-- 历史建表误将 status 定义为 INTEGER，导致查询 status IN ('received','offset') 报
-- "invalid input syntax for type integer: received" → 500。此处改回 VARCHAR 以匹配代码约定。
ALTER TABLE erp_pre_receipt ALTER COLUMN status TYPE VARCHAR(50);
