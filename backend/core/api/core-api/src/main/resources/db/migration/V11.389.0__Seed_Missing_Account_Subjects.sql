-- ============================================================
-- V11.389.0: 补齐业财集成实际引用、但基础科目种子里漏掉的会计科目
--
-- 背景：V11.28.0 的种子清单是手写的，只覆盖了当时的「五个业财集成服务」，
-- 之后各模块陆续引用新的科目编码却没有同步补种子，导致这些凭证一路报
-- 「科目编码不存在」并把整笔业务回滚（凭证与业务同事务，这是设计使然）。
--
-- 本次按「源码里真实 setSubjectCode / SUBJECT_* 常量」逐个取证补齐：
--   2203 预收账款      ← 储值卡开卡·充值（借 1001/1002 贷 2203）、预收款单记账
--   1123 预付账款      ← 付款单记账（PaymentAccountingService，借 1123）
--   1405 库存商品      ← 销售出库结转成本（SaleOutboundServiceImpl，贷 1405）
--   1901 待处理财产损溢 ← 对账差异处理（ReconciliationServiceImpl）
--
-- 科目类型：1-资产 2-负债 3-权益 4-成本 5-损益；方向：1-借方 2-贷方。
-- ⚠️ is_leaf / is_enabled 列现为 boolean（V11.28.0 用 1/0 写入是历史遗留，靠 int→bool 隐式转换侥幸通过；
--    在 VALUES 派生表里没有隐式转换，必须写 true/false）。
-- 幂等：按 (tenant_id, subject_code) NOT EXISTS 守卫，可重复执行。
-- ============================================================

INSERT INTO finance_account_subject (subject_code, subject_name, parent_id, level, subject_type, direction,
                                     is_leaf, is_enabled, deleted_flag, tenant_id, remark)
SELECT * FROM (VALUES
    ('1123', '预付账款',       NULL::bigint, 1, 1, 1, true, true, 0, 1, '资产类一级科目（预付给供应商）'),
    ('1405', '库存商品',       NULL::bigint, 1, 1, 1, true, true, 0, 1, '资产类一级科目（销售出库结转成本对方科目）'),
    ('1901', '待处理财产损溢', NULL::bigint, 1, 1, 1, true, true, 0, 1, '资产类一级科目（对账差异过渡）'),
    ('2203', '预收账款',       NULL::bigint, 1, 2, 2, true, true, 0, 1, '负债类一级科目（预收款/储值卡余额形成的负债）')
) AS seed(subject_code, subject_name, parent_id, level, subject_type, direction, is_leaf, is_enabled, deleted_flag,
          tenant_id, remark)
WHERE NOT EXISTS (
    SELECT 1 FROM finance_account_subject s
    WHERE s.tenant_id = seed.tenant_id AND s.subject_code = seed.subject_code AND s.deleted_flag = 0
);
