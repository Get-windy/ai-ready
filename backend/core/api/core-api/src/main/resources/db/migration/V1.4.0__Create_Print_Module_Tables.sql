-- ============================================================
-- 可视化打印模块 V2 核心表
-- 包括：打印模板、打印客户端、打印链路、打印任务、截图任务
-- ============================================================

-- ============================================================
-- 1. 打印模板表 (sys_print_template)
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_print_template (
    template_id      BIGSERIAL       PRIMARY KEY,
    tenant_id        BIGINT          NOT NULL,
    page_code        VARCHAR(100)    NOT NULL,
    template_name    VARCHAR(200)    NOT NULL,

    -- 可视化设计器存储的完整模板 JSON
    -- 结构：{ width, height, margin, components: [{ type, x, y, w, h, field, style, formatConfig }] }
    template_json    JSONB           NOT NULL DEFAULT '{}',

    -- 纸张设置
    paper_size       VARCHAR(20)     NOT NULL DEFAULT 'A4',
    paper_width      NUMERIC(10,2),
    paper_height     NUMERIC(10,2),
    margin_top       NUMERIC(10,2)   DEFAULT 10.00,
    margin_bottom    NUMERIC(10,2)   DEFAULT 10.00,
    margin_left      NUMERIC(10,2)   DEFAULT 10.00,
    margin_right     NUMERIC(10,2)   DEFAULT 10.00,

    -- 状态
    status           VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    is_default       BOOLEAN         DEFAULT FALSE,
    version          INTEGER         NOT NULL DEFAULT 1,

    -- 审计字段
    created_by       BIGINT,
    created_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by       BIGINT,
    updated_at       TIMESTAMP       DEFAULT NOW(),
    deleted          INTEGER         DEFAULT 0,

    CONSTRAINT uk_st_tenant_name UNIQUE (tenant_id, page_code, template_name),
    CONSTRAINT ck_st_paper_size CHECK (paper_size IN ('A4', 'A5', 'CUSTOM')),
    CONSTRAINT ck_st_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'DISABLED'))
);

CREATE INDEX idx_st_tenant_page ON sys_print_template(tenant_id, page_code);
CREATE INDEX idx_st_status ON sys_print_template(status);

COMMENT ON TABLE sys_print_template IS '打印模板表——可视化设计器存储的模板定义';
COMMENT ON COLUMN sys_print_template.template_json IS '模板 JSON：包含纸张、边距、组件列表及每个组件的字段绑定、样式、格式化配置';
COMMENT ON COLUMN sys_print_template.page_code IS '业务页面标识，如 sale_order, purchase_order';
COMMENT ON COLUMN sys_print_template.paper_size IS '纸张规格: A4/A5/CUSTOM';
COMMENT ON COLUMN sys_print_template.status IS '状态: DRAFT=草稿 PUBLISHED=已发布 DISABLED=已禁用';

-- ============================================================
-- 2. 打印客户端表 (sys_print_client)
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_print_client (
    client_id        BIGSERIAL       PRIMARY KEY,
    tenant_id        BIGINT          NOT NULL,
    client_name      VARCHAR(200)    NOT NULL,
    client_code      VARCHAR(100)    NOT NULL,
    auth_key         VARCHAR(255)    NOT NULL,

    -- 在线状态
    status           VARCHAR(20)     NOT NULL DEFAULT 'OFFLINE',
    last_heartbeat   TIMESTAMP,
    client_ip        VARCHAR(50),
    client_version   VARCHAR(20),

    -- 打印机配置
    default_printer  VARCHAR(200),

    -- 审计字段
    created_by       BIGINT,
    created_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP       DEFAULT NOW(),
    deleted          INTEGER         DEFAULT 0,

    CONSTRAINT uk_sc_tenant_code UNIQUE (tenant_id, client_code),
    CONSTRAINT uk_sc_tenant_name UNIQUE (tenant_id, client_name),
    CONSTRAINT ck_sc_status CHECK (status IN ('ONLINE', 'OFFLINE', 'DISABLED'))
);

CREATE INDEX idx_sc_tenant ON sys_print_client(tenant_id, status);

COMMENT ON TABLE sys_print_client IS '打印客户端——Windows 本地打印客户端注册信息';
COMMENT ON COLUMN sys_print_client.client_code IS '客户端唯一编号（自动生成）';
COMMENT ON COLUMN sys_print_client.auth_key IS '认证密钥（注册时下发，用于API鉴权）';
COMMENT ON COLUMN sys_print_client.default_printer IS '默认打印机名称（操作系统中的打印机名）';

-- ============================================================
-- 3. 打印链路表 (sys_print_chain)
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_print_chain (
    chain_id         BIGSERIAL       PRIMARY KEY,
    tenant_id        BIGINT          NOT NULL,
    page_code        VARCHAR(100)    NOT NULL,
    chain_name       VARCHAR(200)    NOT NULL,
    description      VARCHAR(500),
    status           VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    sort_order       INTEGER         DEFAULT 0,

    created_by       BIGINT,
    created_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP       DEFAULT NOW(),
    deleted          INTEGER         DEFAULT 0,

    CONSTRAINT uk_sch_tenant_name UNIQUE (tenant_id, chain_name),
    CONSTRAINT ck_sch_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX idx_sch_tenant_page ON sys_print_chain(tenant_id, page_code);

COMMENT ON TABLE sys_print_chain IS '打印链路——定义打印步骤的编排，一个链路包含1~10个步骤';

-- ============================================================
-- 4. 打印链路明细表 (sys_print_chain_item)
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_print_chain_item (
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
    CONSTRAINT ck_sci_step_order CHECK (step_order BETWEEN 1 AND 10),
    CONSTRAINT fk_sci_chain FOREIGN KEY (chain_id) REFERENCES sys_print_chain(chain_id) ON DELETE CASCADE,
    CONSTRAINT fk_sci_template FOREIGN KEY (template_id) REFERENCES sys_print_template(template_id),
    CONSTRAINT fk_sci_client FOREIGN KEY (client_id) REFERENCES sys_print_client(client_id)
);

CREATE INDEX idx_sci_chain ON sys_print_chain_item(chain_id);

COMMENT ON TABLE sys_print_chain_item IS '打印链路明细——链路的每一步选择的模板、客户端和打印机';
COMMENT ON COLUMN sys_print_chain_item.screenshot_mode IS '截图模式: DISABLED=关闭 MANUAL_CONFIRM=手动确认 AUTO_CONFIRM=超时自动确认';
COMMENT ON COLUMN sys_print_chain_item.screenshot_confirm_timeout IS '截图确认超时秒数（仅 AUTO_CONFIRM 模式有效）';
COMMENT ON COLUMN sys_print_chain_item.screenshot_config_json IS '截图自定义配置：可自定义截图内容、布局、水印等';

-- ============================================================
-- 5. 打印任务表 (sys_print_task)
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_print_task (
    task_id          BIGSERIAL       PRIMARY KEY,
    task_code        VARCHAR(64)     NOT NULL,
    tenant_id        BIGINT          NOT NULL,

    -- 业务单据关联
    page_code        VARCHAR(100),
    document_type    VARCHAR(100),
    document_id      BIGINT,
    document_no      VARCHAR(100),
    data_json        JSONB           NOT NULL DEFAULT '{}',

    -- 链路关联
    chain_id         BIGINT,
    chain_item_id    BIGINT,
    step_order       INTEGER,

    -- 执行目标
    client_id        BIGINT,
    printer_name     VARCHAR(200),

    -- 模板
    template_id      BIGINT,

    -- 执行状态
    status           VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    priority         INTEGER         DEFAULT 5,
    retry_count      INTEGER         DEFAULT 0,
    max_retry        INTEGER         DEFAULT 3,
    error_message    TEXT,
    result_log       TEXT,

    -- 时间
    submit_time      TIMESTAMP       DEFAULT NOW(),
    start_time       TIMESTAMP,
    complete_time    TIMESTAMP,

    -- 截图关联（预留）
    screenshot_id    BIGINT,

    created_by       BIGINT,
    created_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP       DEFAULT NOW(),
    deleted          INTEGER         DEFAULT 0,

    CONSTRAINT uk_stask_code UNIQUE (task_code),
    CONSTRAINT ck_stask_status CHECK (status IN (
        'PENDING', 'QUEUED', 'PRINTING', 'COMPLETED',
        'FAILED', 'CANCELLED', 'WAITING_CONFIRM'
    )),
    CONSTRAINT fk_stask_chain FOREIGN KEY (chain_id) REFERENCES sys_print_chain(chain_id),
    CONSTRAINT fk_stask_client FOREIGN KEY (client_id) REFERENCES sys_print_client(client_id),
    CONSTRAINT fk_stask_template FOREIGN KEY (template_id) REFERENCES sys_print_template(template_id)
);

CREATE INDEX idx_stask_tenant_status ON sys_print_task(tenant_id, status);
CREATE INDEX idx_stask_client_status ON sys_print_task(client_id, status);
CREATE INDEX idx_stask_document ON sys_print_task(document_type, document_id);
CREATE INDEX idx_stask_chain ON sys_print_task(chain_id, status);

COMMENT ON TABLE sys_print_task IS '打印任务表——记录每次打印请求的完整信息';
COMMENT ON COLUMN sys_print_task.data_json IS '单据数据 JSON，用于模板渲染';
COMMENT ON COLUMN sys_print_task.chain_id IS '关联的打印链路，单次打印时为空';
COMMENT ON COLUMN sys_print_task.chain_item_id IS '当前执行的链路步骤 ID';

-- ============================================================
-- 6. 截图任务表 (sys_screenshot_task)
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_screenshot_task (
    screenshot_id    BIGSERIAL       PRIMARY KEY,
    task_code        VARCHAR(64),
    tenant_id        BIGINT          NOT NULL,

    -- 关联模板和单据数据
    template_id      BIGINT          NOT NULL,
    data_json        JSONB           NOT NULL DEFAULT '{}',
    page_code        VARCHAR(100),

    -- 生成结果
    image_url        VARCHAR(500),
    image_base64     TEXT,
    image_width      INTEGER,
    image_height     INTEGER,
    file_size        BIGINT,

    -- 状态
    status           VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    error_message    TEXT,
    duration_ms      INTEGER,

    created_by       BIGINT,
    created_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP       DEFAULT NOW(),
    deleted          INTEGER         DEFAULT 0,

    CONSTRAINT ck_ss_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT fk_ss_template FOREIGN KEY (template_id) REFERENCES sys_print_template(template_id)
);

CREATE INDEX idx_ss_tenant_status ON sys_screenshot_task(tenant_id, status);
CREATE INDEX idx_ss_template ON sys_screenshot_task(template_id);

COMMENT ON TABLE sys_screenshot_task IS '截图任务表——用于生成打印模板预览截图，预留微信发送扩展';
COMMENT ON COLUMN sys_screenshot_task.image_url IS '生成的截图图片 URL 或 CDN 路径';
COMMENT ON COLUMN sys_screenshot_task.image_base64 IS 'Base64 编码的图片数据（小图场景）';
