-- 营销模块 P1：短信合规四件套（退订名单 / 发送时段 / 频控 / 同意留痕）
--
-- 法规依据：
--   · 《通信短信息服务管理规定》——未经用户同意或请求不得发送商业性短消息；拒绝接收应立即停止
--   · 《网络交易监督管理办法》第 13/16 条——须明示身份与联系方式、提供显著简便免费的拒收方式，
--     **拒绝后不得换名义再发**（罚 5000~3 万元）
--   · 《广告法》第 43/62 条——未经同意不得向终端设备发送广告
--   · 2025-05 江苏南通中院判例——未同意发营销短信侵害个人信息权益（退订费 0.1 元 + 合理支出 800 元）
--   · 行业通行发送时段 8:00–21:00（深夜属加重责任情形）
--
-- 一、退订名单：回复 R / 明确拒绝的号码，**永久不再发送**（换名义也不行）
CREATE TABLE IF NOT EXISTS mkt_sms_opt_out (
    id           BIGINT PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL DEFAULT 0,
    mobile       VARCHAR(32)  NOT NULL,
    receiver_name VARCHAR(128),
    /** 退订来源：REPLY_R 回复R / MANUAL 人工登记 / CUSTOMER 客户主动要求 */
    source       VARCHAR(32),
    opt_out_time TIMESTAMP,
    remark       VARCHAR(255),
    deleted      INTEGER      NOT NULL DEFAULT 0,
    create_time  TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_mkt_sms_opt_out_mobile
    ON mkt_sms_opt_out (tenant_id, mobile) WHERE deleted = 0;

COMMENT ON TABLE mkt_sms_opt_out IS '短信退订名单（合规：拒绝后不得再发，含换名义）';

-- 二、发送设置补 4 列：发送时段 + 频控
ALTER TABLE mkt_sms_setting ADD COLUMN IF NOT EXISTS send_start_hour INTEGER;
ALTER TABLE mkt_sms_setting ADD COLUMN IF NOT EXISTS send_end_hour INTEGER;
ALTER TABLE mkt_sms_setting ADD COLUMN IF NOT EXISTS freq_limit_days INTEGER;
ALTER TABLE mkt_sms_setting ADD COLUMN IF NOT EXISTS freq_limit_count INTEGER;

COMMENT ON COLUMN mkt_sms_setting.send_start_hour IS '允许发送起始小时（默认 8，即 8:00）';
COMMENT ON COLUMN mkt_sms_setting.send_end_hour IS '允许发送截止小时（默认 21，即 21:00）';
COMMENT ON COLUMN mkt_sms_setting.freq_limit_days IS '频控窗口天数（默认 7）';
COMMENT ON COLUMN mkt_sms_setting.freq_limit_count IS '窗口内同一手机号最多接收条数（默认 3）';

-- 三、同意留痕：谁在什么时候、为哪个客户/号码同意了短信协议（举证用）
CREATE TABLE IF NOT EXISTS mkt_sms_consent (
    id           BIGINT PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL DEFAULT 0,
    partner_id   BIGINT,
    mobile       VARCHAR(32),
    /** 协议版本号（协议文本变更时递增，便于举证"当时同意的是哪一版"） */
    agreement_version VARCHAR(16),
    agreed_time  TIMESTAMP,
    agreed_by    VARCHAR(64),
    agreed_ip    VARCHAR(64),
    remark       VARCHAR(255),
    deleted      INTEGER      NOT NULL DEFAULT 0,
    create_time  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mkt_sms_consent_mobile
    ON mkt_sms_consent (tenant_id, mobile);

COMMENT ON TABLE mkt_sms_consent IS '短信发送同意留痕（合规举证：谁·何时·为谁·同意哪版协议）';

-- 四、既有台账补「发送时是否已校验合规」的痕迹（排障与举证）
ALTER TABLE mkt_sms_record ADD COLUMN IF NOT EXISTS compliance_note VARCHAR(255);

COMMENT ON COLUMN mkt_sms_record.compliance_note IS '发送时的合规校验结论（时段/频控/退订名单）';
