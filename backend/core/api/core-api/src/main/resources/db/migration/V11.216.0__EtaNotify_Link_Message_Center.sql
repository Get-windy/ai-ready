-- =============================================================================
-- 配送 ETA 通知接入消息底座（PostgreSQL）
--
--   背景：短信发送能力**已在 core-base 消息底座实现**（`sys_message` + `MessageSendTask`
--         定时消费 + 重试 3 次 + `SmsSender` 网关适配），配送侧不应另建一套发送通道。
--   本迁移只做「业务台账 ↔ 消息底座」的关联：`erp_delivery_eta_notify.message_id`
--   指向 `sys_message.id`，发送结果由底座回写，业务侧按 message_id 联查展示。
--
--   幂等：ADD COLUMN IF NOT EXISTS。
-- =============================================================================

ALTER TABLE erp_delivery_eta_notify ADD COLUMN IF NOT EXISTS message_id BIGINT;

COMMENT ON COLUMN erp_delivery_eta_notify.message_id IS '投递到消息底座的消息ID（sys_message.id），发送结果由 MessageSendTask 回写';

CREATE INDEX IF NOT EXISTS idx_erp_eta_notify_message
    ON erp_delivery_eta_notify (message_id);
