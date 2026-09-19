-- 营销模块 → 营销活动 → 发短信（菜单 80310）
-- 对标 ql361 三 Tab：`发短信`（表单）/ `短信历史`（7 列）/ `短信模板管理`（模版标题·模版内容·短信类型·最后修改时间）
-- 口径：
--   · mkt_sms_setting 单行：公司签名 + 短信配额（对标实测「目前剩余短信 100 条」= quota_total - quota_used）
--   · mkt_sms_template：短信模板管理 Tab
--   · mkt_sms_record：营销短信台账（一行 = 一个接收人一条短信），承载「经手人 / 发送批次」这两个营销域事实；
--     **投递状态不在本表重复存储** —— 发送统一走平台底座 MessageService.sendSms（落 sys_message，由 MessageSendTask
--     消费与重试），本表仅存 message_id 外键，「发送状态 / 失败原因」两列实时 join sys_message 读取（单一真源）。
CREATE TABLE IF NOT EXISTS mkt_sms_setting (
    id           BIGINT PRIMARY KEY,
    tenant_id    BIGINT NOT NULL DEFAULT 0,
    sign_name    VARCHAR(64),
    quota_total  INTEGER,
    quota_used   INTEGER,
    deleted      INTEGER NOT NULL DEFAULT 0,
    create_time  TIMESTAMP,
    update_time  TIMESTAMP
);

COMMENT ON TABLE mkt_sms_setting IS '短信设置（公司签名 + 短信配额，营销→营销活动→发短信）';
COMMENT ON COLUMN mkt_sms_setting.quota_total IS '短信总配额（条）';
COMMENT ON COLUMN mkt_sms_setting.quota_used IS '已使用（条）';

CREATE TABLE IF NOT EXISTS mkt_sms_template (
    id               BIGINT PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL DEFAULT 0,
    template_title   VARCHAR(128) NOT NULL,
    template_content VARCHAR(500),
    sms_type         VARCHAR(32),
    status           INTEGER,
    deleted          INTEGER      NOT NULL DEFAULT 0,
    create_by        BIGINT,
    create_time      TIMESTAMP,
    update_by        BIGINT,
    update_time      TIMESTAMP
);

COMMENT ON TABLE mkt_sms_template IS '短信模板（营销→营销活动→发短信→短信模板管理 Tab）';
COMMENT ON COLUMN mkt_sms_template.sms_type IS '短信类型：NOTICE 通知短信 / MARKETING 营销短信';

CREATE TABLE IF NOT EXISTS mkt_sms_record (
    id            BIGINT PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL DEFAULT 0,
    batch_no      VARCHAR(64),
    message_id    BIGINT,
    receiver_id   BIGINT,
    receiver_name VARCHAR(128),
    mobile        VARCHAR(32),
    content       VARCHAR(500),
    sms_type      VARCHAR(32),
    sign_name     VARCHAR(64),
    handler_id    BIGINT,
    handler_name  VARCHAR(64),
    deleted       INTEGER      NOT NULL DEFAULT 0,
    create_time   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mkt_sms_record_batch ON mkt_sms_record (tenant_id, batch_no);

COMMENT ON TABLE mkt_sms_record IS '营销短信台账（营销→营销活动→发短信→短信历史 Tab）';
COMMENT ON COLUMN mkt_sms_record.message_id IS '平台消息ID（sys_message.id）——发送状态/失败原因的单一真源';
COMMENT ON COLUMN mkt_sms_record.handler_name IS '经手人（对标「短信历史」列的经手人）';
