-- =============================================================================
-- 渠道管理（配送 → 配送配置 → 渠道管理，dms:channel，菜单 80880）第二轮：对接能力落地
--   补齐《渠道管理开发文档》§7 待完善 1/3/4 与 §3.3 缺失接口：
--     1. 派单接线      —— dms_channel_order 外部单台账（幂等键唯一，支撑「同任务同渠道只下一次单」）
--     2. 回调安全      —— dms_channel_callback_log 回调日志（验签结果 / 非重复标记 / nonce 唯一）
--     3. 加密存储配套  —— 凭据加密开关与密钥键
--
--   迁移号说明：当日并行会话已占用 11.34x 段，本页取 11.350.0（`ls db/migration | tail` 避让）。
--   幂等：建表 IF NOT EXISTS / 索引 IF NOT EXISTS / 配置 ON CONFLICT DO UPDATE
-- =============================================================================

-- ── 1) 外部平台单台账（一次「向渠道下单」= 一行；幂等键 unique 保证重复提交不重复下单）─────
CREATE TABLE IF NOT EXISTS dms_channel_order (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL DEFAULT 0,
    channel_id       BIGINT       NOT NULL,
    channel_code     VARCHAR(64),
    task_id          BIGINT,
    task_no          VARCHAR(64),
    -- 幂等键 = taskNo:channelId（外部平台侧同一业务单只下一次）
    idem_key         VARCHAR(128) NOT NULL,
    -- 外部平台返回的单号（回调按此单号关联）
    channel_order_no VARCHAR(64),
    -- 单状态：0-待提交 1-已提交 2-已接单 3-配送中 4-已完成 5-已取消 6-提交失败
    order_status     INTEGER      NOT NULL DEFAULT 0,
    attempts         INTEGER      NOT NULL DEFAULT 0,
    last_error       VARCHAR(500),
    submit_time      TIMESTAMP,
    callback_time    TIMESTAMP,
    remark           VARCHAR(500),
    deleted          INTEGER      NOT NULL DEFAULT 0,
    create_by        BIGINT,
    create_time      TIMESTAMP    DEFAULT now(),
    update_by        BIGINT,
    update_time      TIMESTAMP    DEFAULT now(),
    version          INTEGER      DEFAULT 0
);

COMMENT ON TABLE  dms_channel_order                  IS '渠道外部单台账：向外部运力平台的下单记录（幂等键唯一）';
COMMENT ON COLUMN dms_channel_order.idem_key         IS '幂等键 = taskNo:channelId';
COMMENT ON COLUMN dms_channel_order.order_status     IS '0-待提交 1-已提交 2-已接单 3-配送中 4-已完成 5-已取消 6-提交失败';

-- 幂等：同租户同幂等键仅一行（软删除后可重建）
CREATE UNIQUE INDEX IF NOT EXISTS uk_dms_channel_order_idem
    ON dms_channel_order (tenant_id, idem_key) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dms_channel_order_task
    ON dms_channel_order (tenant_id, task_id);
CREATE INDEX IF NOT EXISTS idx_dms_channel_order_channel_no
    ON dms_channel_order (tenant_id, channel_id, channel_order_no);

-- ── 2) 回调日志（验签 / 时间戳 / 防重放 / 处理结果全程留痕，可追溯）──────────────────
CREATE TABLE IF NOT EXISTS dms_channel_callback_log (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL DEFAULT 0,
    channel_id       BIGINT,
    channel_code     VARCHAR(64),
    channel_order_no VARCHAR(64),
    task_id          BIGINT,
    task_no          VARCHAR(64),
    event_type       VARCHAR(32),
    external_status  VARCHAR(32),
    -- 平台请求号（防重放：同渠道同 nonce 只处理一次）
    nonce            VARCHAR(64),
    sign_ok          INTEGER      NOT NULL DEFAULT 0,
    replayed         INTEGER      NOT NULL DEFAULT 0,
    process_result   VARCHAR(32),
    process_message  VARCHAR(500),
    payload          VARCHAR(2000),
    receive_time     TIMESTAMP    DEFAULT now(),
    deleted          INTEGER      NOT NULL DEFAULT 0,
    create_time      TIMESTAMP    DEFAULT now(),
    update_time      TIMESTAMP    DEFAULT now()
);

COMMENT ON TABLE  dms_channel_callback_log             IS '渠道回调日志：验签/防重放/处理结果留痕（§3.4 回调安全）';
COMMENT ON COLUMN dms_channel_callback_log.sign_ok     IS '验签结果：0-失败 1-通过';
COMMENT ON COLUMN dms_channel_callback_log.replayed    IS '是否重放（同 nonce 重复投递）：0-否 1-是';

CREATE UNIQUE INDEX IF NOT EXISTS uk_dms_channel_callback_nonce
    ON dms_channel_callback_log (tenant_id, channel_id, nonce)
    WHERE deleted = 0 AND nonce IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_dms_channel_callback_time
    ON dms_channel_callback_log (tenant_id, channel_id, receive_time DESC);

-- ── 3) 配置中心（dms_config）：渠道对接的推送重试 / 降级 / 回调容差 / 凭据加密开关 ────────
INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
VALUES
  (0, 'dms.channel.push.retry', '2',
   '渠道派单：外部平台下单失败重试次数（指数退避，默认 2）', 'TENANT', 0, now(), now()),
  (0, 'dms.channel.push.fallback', 'true',
   '渠道派单：外部渠道不可用时是否降级回自有运力派单（默认 true）', 'TENANT', 0, now(), now()),
  (0, 'dms.channel.callback.tolerance.seconds', '300',
   '渠道回调：时间戳容差（秒，防重放，默认 300）', 'TENANT', 0, now(), now()),
  (0, 'dms.channel.config.encrypt', 'true',
   '渠道凭据：config_json 敏感值是否加密存储（默认 true）', 'TENANT', 0, now(), now()),
  (0, 'dms.channel.config.encrypt-key', '',
   '渠道凭据加密密钥（留空=使用内置默认密钥，生产必须显式配置）', 'GLOBAL', 0, now(), now())
ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
  SET config_desc = EXCLUDED.config_desc,
      scope       = EXCLUDED.scope,
      update_time = CURRENT_TIMESTAMP;

-- 同时下发到系统租户(1)，与既有 dms.* 双行口径保持一致
INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
SELECT 1, config_key, config_value, config_desc, scope, 0, now(), now()
FROM dms_config
WHERE tenant_id = 0
  AND config_key IN ('dms.channel.push.retry', 'dms.channel.push.fallback',
                     'dms.channel.callback.tolerance.seconds', 'dms.channel.config.encrypt',
                     'dms.channel.config.encrypt-key')
  AND deleted = 0
ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
  SET config_desc = EXCLUDED.config_desc,
      update_time  = CURRENT_TIMESTAMP;
