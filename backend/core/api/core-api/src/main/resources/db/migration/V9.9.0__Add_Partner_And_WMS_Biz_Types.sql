-- V9.9.0: 新增往来单位（按角色）和 WMS/仓储业务编号类型
-- 渐进式编码算法使 max_seq 仅为初始参考值，实际永不溢出

-- 客户编号（中文 KH / 英文 CUS）
INSERT INTO biz_number_sequence (biz_type, locale, seq_date, current_seq, prefix, seq_length, tenant_id)
VALUES
    ('KH',   'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'KH',   4, 1),
    ('KH',   'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'CUS',  4, 1)
ON CONFLICT (tenant_id, biz_type, locale) DO NOTHING;

-- 供应商编号（中文 GYS / 英文 SUP）
INSERT INTO biz_number_sequence (biz_type, locale, seq_date, current_seq, prefix, seq_length, tenant_id)
VALUES
    ('GYS',  'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'GYS',  4, 1),
    ('GYS',  'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'SUP',  4, 1)
ON CONFLICT (tenant_id, biz_type, locale) DO NOTHING;

-- 物流公司编号（中文 WLGS / 英文 LOG）
INSERT INTO biz_number_sequence (biz_type, locale, seq_date, current_seq, prefix, seq_length, tenant_id)
VALUES
    ('WLGS', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'WLGS', 4, 1),
    ('WLGS', 'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'LOG',  4, 1)
ON CONFLICT (tenant_id, biz_type, locale) DO NOTHING;

-- 通用往来单位编号（中文 WLDW / 英文 PTR）
INSERT INTO biz_number_sequence (biz_type, locale, seq_date, current_seq, prefix, seq_length, tenant_id)
VALUES
    ('WLDW', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'WLDW', 4, 1),
    ('WLDW', 'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'PTR',  4, 1)
ON CONFLICT (tenant_id, biz_type, locale) DO NOTHING;

-- WMS/仓储业务编号（中文拼音前缀）
INSERT INTO biz_number_sequence (biz_type, locale, seq_date, current_seq, prefix, seq_length, tenant_id)
VALUES
    ('WV', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'WV', 4, 1),
    ('WS', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'WS', 4, 1),
    ('RC', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'RC', 4, 1),
    ('PA', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'PA', 4, 1),
    ('PK', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'PK', 4, 1),
    ('MV', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'MV', 4, 1),
    ('WC', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'WC', 4, 1),
    ('PC', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'PC', 4, 1),
    ('RT', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'RT', 4, 1),
    ('IN', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'CGRK', 4, 1),
    ('SH', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'XSCK', 4, 1)
ON CONFLICT (tenant_id, biz_type, locale) DO NOTHING;

-- WMS/仓储业务编号（英文前缀）
INSERT INTO biz_number_sequence (biz_type, locale, seq_date, current_seq, prefix, seq_length, tenant_id)
VALUES
    ('WV', 'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'WV',  4, 1),
    ('WS', 'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'WS',  4, 1),
    ('RC', 'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'RC',  4, 1),
    ('PA', 'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'PA',  4, 1),
    ('PK', 'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'PK',  4, 1),
    ('MV', 'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'MV',  4, 1),
    ('WC', 'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'WC',  4, 1),
    ('PC', 'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'INV', 4, 1),
    ('RT', 'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'RO',  4, 1),
    ('IN', 'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'GR',  4, 1),
    ('SH', 'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'SH',  4, 1)
ON CONFLICT (tenant_id, biz_type, locale) DO NOTHING;
