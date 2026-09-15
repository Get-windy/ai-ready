-- ============================================================================
-- 零售单 金标准增强（2026-09-10）
--
-- 背景：零售单开发文档「最终裁决方案」红线要求单据编号必须来自后端号段
--      （GET /api/sales/retail/next-no → RetailOrderServiceImpl.generateRetailNo），
--      原先实现为 Service 内 count(*) 拼接的 LS+yyyyMMdd+4位流水，无并发安全且未接入
--      biz_number_sequence 统一号段。
--
-- 本脚本只做号段初始化，不改表结构（erp_retail_order / _item / _payment 列已齐备）。
-- 全部语句可重复执行。
-- ============================================================================

-- ------------------------------------------------------------
-- 零售单号段：LSD → LS-20260910-0001（4 位流水，按日重置）
-- ------------------------------------------------------------
INSERT INTO biz_number_sequence (biz_type, locale, seq_date, current_seq, prefix, seq_length, tenant_id, max_seq)
VALUES ('LSD', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'LS', 4, 1, 999999)
ON CONFLICT (tenant_id, biz_type, locale) DO NOTHING;

-- 英文环境号段（与其它单据保持一致的双语言初始化）
INSERT INTO biz_number_sequence (biz_type, locale, seq_date, current_seq, prefix, seq_length, tenant_id, max_seq)
VALUES ('LSD', 'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'RT', 4, 1, 999999)
ON CONFLICT (tenant_id, biz_type, locale) DO NOTHING;
