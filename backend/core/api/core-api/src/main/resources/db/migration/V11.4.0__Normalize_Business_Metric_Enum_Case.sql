-- ============================================================
-- V11.4.0: 规范化 erp_business_metric 枚举值大小写
--
-- 背景：
--   BusinessMetric 实体使用 @Enumerated(EnumType.STRING)，
--   JPA 读取时按枚举 name()（大写）做 valueOf 转换。
--   历史数据（87万行）metric_type/status 为小写，
--   导致读取时 No enum constant 异常，/api/erp/metrics/dashboard 500。
--
-- 处置：
--   metric_type: order/inventory/user/sales/finance/purchase -> 大写
--   status: normal/active/inactive/deprecated -> 大写
--   幂等：重复执行无害。
-- ============================================================

UPDATE erp_business_metric SET metric_type = UPPER(metric_type) WHERE metric_type <> UPPER(metric_type);
UPDATE erp_business_metric SET status = UPPER(status) WHERE status <> UPPER(status);
-- period: realtime -> REALTIME 等（MINUTE_1 等含下划线常量，历史小写值仅 realtime，直接 UPDATE）
UPDATE erp_business_metric SET period = 'REALTIME' WHERE period = 'realtime';
