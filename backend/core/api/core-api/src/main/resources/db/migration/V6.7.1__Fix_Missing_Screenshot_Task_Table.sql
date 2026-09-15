-- 补充缺失的 sys_screenshot_task 表
-- 注意：移除外键约束以简化创建过程

CREATE TABLE IF NOT EXISTS sys_screenshot_task (
    screenshot_id    BIGSERIAL       PRIMARY KEY,
    task_code        VARCHAR(64),
    tenant_id        BIGINT          NOT NULL,
    template_id      BIGINT          NOT NULL,
    data_json        JSONB           NOT NULL DEFAULT '{}',
    page_code        VARCHAR(100),
    image_url        VARCHAR(500),
    image_base64     TEXT,
    image_width      INTEGER,
    image_height     INTEGER,
    file_size        BIGINT,
    status           VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    error_message    TEXT,
    duration_ms      INTEGER,
    created_by       BIGINT,
    created_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP       DEFAULT NOW(),
    deleted          INTEGER         DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_ss_tenant_status ON sys_screenshot_task(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_ss_template ON sys_screenshot_task(template_id);

COMMENT ON TABLE sys_screenshot_task IS '截图任务表';