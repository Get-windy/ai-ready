-- V8.18.0: 创建业务编号序列表，支持按业务类型递增编号
-- 解决之前使用 Math.random() / System.currentTimeMillis() % N 导致的随机编号问题

CREATE TABLE IF NOT EXISTS biz_number_sequence (
    id              BIGSERIAL PRIMARY KEY,
    biz_type        VARCHAR(50) NOT NULL UNIQUE,
    seq_date        VARCHAR(8) NOT NULL,
    current_seq     INTEGER NOT NULL DEFAULT 0,
    max_seq         INTEGER NOT NULL DEFAULT 999999,
    prefix          VARCHAR(20) NOT NULL DEFAULT '',
    seq_length      INTEGER NOT NULL DEFAULT 3,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 初始化各业务类型的序列号记录
INSERT INTO biz_number_sequence (biz_type, seq_date, current_seq, prefix, seq_length)
VALUES
    ('SN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'SN', 6),
    ('BN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'B', 4),
    ('INV', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'INV', 5),
    ('APP', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'APP', 5),
    ('SO', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'SO', 4),
    ('PO', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'PO', 4),
    ('RO', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'RO', 4),
    ('PARTNER', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'P', 4)
ON CONFLICT (biz_type) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_biz_number_sequence_type_date ON biz_number_sequence(biz_type, seq_date);
