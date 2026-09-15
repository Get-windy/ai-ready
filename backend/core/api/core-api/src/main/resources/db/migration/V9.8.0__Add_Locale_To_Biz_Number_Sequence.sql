-- V9.8.0: 为业务编号序列表添加语言区域支持
-- 同一业务类型可以有不同语言的前缀（如 xsdd=中文, SO=英文）

-- 添加 locale 列（语言区域，默认 zh_CN）
ALTER TABLE biz_number_sequence ADD COLUMN IF NOT EXISTS locale VARCHAR(10) NOT NULL DEFAULT 'zh_CN';

-- 删除旧的索引和唯一约束
DROP INDEX IF EXISTS idx_biz_number_sequence_tenant_type_date;
DROP INDEX IF EXISTS uk_biz_number_sequence_tenant_type;

-- 创建包含 locale 的新索引
CREATE INDEX IF NOT EXISTS idx_biz_number_sequence_tenant_type_locale
ON biz_number_sequence(tenant_id, biz_type, locale);

-- 创建包含 locale 的唯一约束
CREATE UNIQUE INDEX IF NOT EXISTS uk_biz_number_sequence_tenant_type_locale
ON biz_number_sequence(tenant_id, biz_type, locale);

-- 将现有的英文前缀记录标记为 zh_CN（保持计数连续性）
-- SN, BN, INV, APP, SO, PO, RO, PARTNER 已经存在于 zh_CN

-- 插入中文拼音前缀的新业务类型（zh_CN 区域）
INSERT INTO biz_number_sequence (biz_type, locale, seq_date, current_seq, prefix, seq_length, tenant_id)
VALUES
    ('XSDD', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'XSDD', 4, 1),
    ('CGDD', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'CGDD', 4, 1),
    ('THDD', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'THDD', 4, 1),
    ('CGRK', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'CGRK', 4, 1),
    ('XSCK', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'XSCK', 4, 1),
    ('QTCK', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'QTCK', 4, 1),
    ('FP',   'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'FP',   5, 1),
    ('FPSQ', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'FPSQ', 5, 1),
    ('XLBH', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'XLBH', 6, 1),
    ('PCBH', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'PCBH', 4, 1),
    ('DWBM', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'DWBM', 4, 1)
ON CONFLICT (tenant_id, biz_type, locale) DO NOTHING;

-- 插入英文前缀的初始数据（en_US 区域）
INSERT INTO biz_number_sequence (biz_type, locale, seq_date, current_seq, prefix, seq_length, tenant_id)
VALUES
    ('SO',      'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'SO',  4, 1),
    ('PO',      'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'PO',  4, 1),
    ('RO',      'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'RO',  4, 1),
    ('GR',      'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'GR',  4, 1),
    ('SO_SHIP', 'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'SH',  4, 1),
    ('ADJ',     'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'ADJ', 4, 1),
    ('INV',     'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'INV', 5, 1),
    ('APP',     'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'APP', 5, 1),
    ('SN',      'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'SN',  6, 1),
    ('BN',      'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'B',   4, 1),
    ('PARTNER', 'en_US', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'P',   4, 1)
ON CONFLICT (tenant_id, biz_type, locale) DO NOTHING;
