-- 添加商品编号序列（SP前缀，格式：SP-20260703-001）
INSERT INTO biz_number_sequence (biz_type, locale, tenant_id, seq_date, current_seq, prefix, seq_length)
VALUES ('SP', 'zh_CN', 1, TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'SP', 3)
ON CONFLICT (tenant_id, biz_type, locale) DO NOTHING;
