-- =====================================================
-- V6.9.0: 修复 sys_print_chain_item 表结构错误
-- 错误: V6.8.0 创建了错误的表结构，主键是 id 而非 item_id
-- 正确结构来自 V1.4.0
-- =====================================================

-- 删除错误的表结构，重新创建正确的
DROP TABLE IF EXISTS sys_print_chain_item;

-- 创建正确的表结构（来自 V1.4.0）
CREATE TABLE sys_print_chain_item (
    item_id              BIGSERIAL       PRIMARY KEY,
    chain_id             BIGINT          NOT NULL,
    step_order           INTEGER         NOT NULL,
    template_id          BIGINT          NOT NULL,
    client_id            BIGINT          NOT NULL,
    printer_name         VARCHAR(200),

    -- 截图确认模式
    -- DISABLED: 不截图
    -- MANUAL_CONFIRM: 截图后等待用户手动确认
    -- AUTO_CONFIRM:   截图后超时自动确认（超时时间由 screenshot_confirm_timeout 控制）
    screenshot_mode      VARCHAR(20)     NOT NULL DEFAULT 'DISABLED',
    screenshot_confirm_timeout INTEGER   DEFAULT 300,
    -- 截图自定义配置 JSON：可自定义截图内容、布局、水印等
    screenshot_config_json JSONB         DEFAULT '{}',

    created_by           BIGINT,
    created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP       DEFAULT NOW(),

    CONSTRAINT uk_sci_chain_step UNIQUE (chain_id, step_order),
    CONSTRAINT ck_sci_step_order CHECK (step_order BETWEEN 1 AND 10)
);

CREATE INDEX idx_sci_chain ON sys_print_chain_item(chain_id);

COMMENT ON TABLE sys_print_chain_item IS '打印链路明细——链路的每一步选择的模板、客户端和打印机';
COMMENT ON COLUMN sys_print_chain_item.screenshot_mode IS '截图模式: DISABLED=关闭 MANUAL_CONFIRM=手动确认 AUTO_CONFIRM=超时自动确认';
COMMENT ON COLUMN sys_print_chain_item.screenshot_confirm_timeout IS '截图确认超时秒数（仅 AUTO_CONFIRM 模式有效）';
COMMENT ON COLUMN sys_print_chain_item.screenshot_config_json IS '截图自定义配置：可自定义截图内容、布局、水印等';