-- =====================================================
-- V9.1.0: Workflow 核心表补全 + 菜单修复
-- =====================================================
-- 目标:
-- 1. 创建 workflow_definition, workflow_instance, workflow_task 表
-- 2. 修复菜单指向实际页面
-- 3. 连接流程设计器到画布组件
-- =====================================================

-- =====================================================
-- Part 1: 创建工作流定义表
-- =====================================================
CREATE TABLE IF NOT EXISTS workflow_definition (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    flow_code            VARCHAR(50) NOT NULL,
    flow_name            VARCHAR(100) NOT NULL,
    flow_type            VARCHAR(20) DEFAULT 'APPROVAL',
    version              INTEGER DEFAULT 1,
    status               INTEGER DEFAULT 0,
    config_json          TEXT,
    description          VARCHAR(500),
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT
);

-- 补全可能缺失的列（表已由早期版本创建但缺少部分字段）
ALTER TABLE workflow_definition ADD COLUMN IF NOT EXISTS flow_code VARCHAR(50);
ALTER TABLE workflow_definition ADD COLUMN IF NOT EXISTS flow_name VARCHAR(100);
ALTER TABLE workflow_definition ADD COLUMN IF NOT EXISTS flow_type VARCHAR(20) DEFAULT 'APPROVAL';
ALTER TABLE workflow_definition ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 1;
ALTER TABLE workflow_definition ADD COLUMN IF NOT EXISTS status INTEGER DEFAULT 0;
ALTER TABLE workflow_definition ADD COLUMN IF NOT EXISTS config_json TEXT;
ALTER TABLE workflow_definition ADD COLUMN IF NOT EXISTS description VARCHAR(500);

COMMENT ON TABLE workflow_definition IS '工作流定义表';
COMMENT ON COLUMN workflow_definition.flow_code IS '流程编码（唯一）';
COMMENT ON COLUMN workflow_definition.flow_name IS '流程名称';
COMMENT ON COLUMN workflow_definition.flow_type IS '流程类型: APPROVAL审批, TRANSFER转交, REFUND退款';
COMMENT ON COLUMN workflow_definition.version IS '版本号';
COMMENT ON COLUMN workflow_definition.status IS '状态: 0草稿, 1已发布, 2已停用';
COMMENT ON COLUMN workflow_definition.config_json IS '流程配置JSON（节点、连线等）';

CREATE UNIQUE INDEX IF NOT EXISTS idx_wf_def_flow_code ON workflow_definition(flow_code, tenant_id);
CREATE INDEX IF NOT EXISTS idx_wf_def_tenant ON workflow_definition(tenant_id);

-- =====================================================
-- Part 2: 创建工作流实例表
-- =====================================================
CREATE TABLE IF NOT EXISTS workflow_instance (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    flow_id              BIGINT NOT NULL,
    biz_type             VARCHAR(50),
    biz_id               BIGINT,
    biz_no               VARCHAR(100),
    status               INTEGER DEFAULT 0,
    current_node         VARCHAR(50),
    initiator_id         BIGINT,
    initiator_name       VARCHAR(100),
    started_time         TIMESTAMP,
    completed_time       TIMESTAMP,
    form_data            TEXT,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT
);

-- 补全可能缺失的列
ALTER TABLE workflow_instance ADD COLUMN IF NOT EXISTS flow_id BIGINT;
ALTER TABLE workflow_instance ADD COLUMN IF NOT EXISTS biz_type VARCHAR(50);
ALTER TABLE workflow_instance ADD COLUMN IF NOT EXISTS biz_id BIGINT;
ALTER TABLE workflow_instance ADD COLUMN IF NOT EXISTS biz_no VARCHAR(100);
ALTER TABLE workflow_instance ADD COLUMN IF NOT EXISTS status INTEGER DEFAULT 0;
ALTER TABLE workflow_instance ADD COLUMN IF NOT EXISTS current_node VARCHAR(50);
ALTER TABLE workflow_instance ADD COLUMN IF NOT EXISTS initiator_id BIGINT;
ALTER TABLE workflow_instance ADD COLUMN IF NOT EXISTS initiator_name VARCHAR(100);
ALTER TABLE workflow_instance ADD COLUMN IF NOT EXISTS started_time TIMESTAMP;
ALTER TABLE workflow_instance ADD COLUMN IF NOT EXISTS completed_time TIMESTAMP;
ALTER TABLE workflow_instance ADD COLUMN IF NOT EXISTS form_data TEXT;

COMMENT ON TABLE workflow_instance IS '工作流实例表';
COMMENT ON COLUMN workflow_instance.flow_id IS '流程定义ID';
COMMENT ON COLUMN workflow_instance.biz_type IS '业务类型: SALE_ORDER, PURCHASE_ORDER, EXPENSE, LEAVE';
COMMENT ON COLUMN workflow_instance.biz_id IS '业务单据ID';
COMMENT ON COLUMN workflow_instance.biz_no IS '业务单据编号';
COMMENT ON COLUMN workflow_instance.status IS '实例状态: 0运行中, 1已完成, 2已驳回, 3已撤回, 4已终止';
COMMENT ON COLUMN workflow_instance.current_node IS '当前节点编码';
COMMENT ON COLUMN workflow_instance.initiator_id IS '发起人ID';
COMMENT ON COLUMN workflow_instance.initiator_name IS '发起人名称';
COMMENT ON COLUMN workflow_instance.form_data IS '表单数据(JSON)';

CREATE INDEX IF NOT EXISTS idx_wf_inst_flow ON workflow_instance(flow_id);
CREATE INDEX IF NOT EXISTS idx_wf_inst_biz ON workflow_instance(biz_type, biz_id);
CREATE INDEX IF NOT EXISTS idx_wf_inst_initiator ON workflow_instance(initiator_id);
CREATE INDEX IF NOT EXISTS idx_wf_inst_status ON workflow_instance(status);
CREATE INDEX IF NOT EXISTS idx_wf_inst_tenant ON workflow_instance(tenant_id);

-- =====================================================
-- Part 3: 创建工作流任务表
-- =====================================================
CREATE TABLE IF NOT EXISTS workflow_task (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    instance_id          BIGINT NOT NULL,
    node_code            VARCHAR(50),
    node_name            VARCHAR(100),
    assignee_id          BIGINT,
    assignee_name        VARCHAR(100),
    status               INTEGER DEFAULT 0,
    comment              TEXT,
    prev_task_id         BIGINT,
    transferred_from     BIGINT,
    action_time          TIMESTAMP,
    deadline             TIMESTAMP,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT
);

-- 补全可能缺失的列
ALTER TABLE workflow_task ADD COLUMN IF NOT EXISTS instance_id BIGINT;
ALTER TABLE workflow_task ADD COLUMN IF NOT EXISTS node_code VARCHAR(50);
ALTER TABLE workflow_task ADD COLUMN IF NOT EXISTS node_name VARCHAR(100);
ALTER TABLE workflow_task ADD COLUMN IF NOT EXISTS assignee_id BIGINT;
ALTER TABLE workflow_task ADD COLUMN IF NOT EXISTS assignee_name VARCHAR(100);
ALTER TABLE workflow_task ADD COLUMN IF NOT EXISTS status INTEGER DEFAULT 0;
ALTER TABLE workflow_task ADD COLUMN IF NOT EXISTS comment TEXT;
ALTER TABLE workflow_task ADD COLUMN IF NOT EXISTS prev_task_id BIGINT;
ALTER TABLE workflow_task ADD COLUMN IF NOT EXISTS transferred_from BIGINT;
ALTER TABLE workflow_task ADD COLUMN IF NOT EXISTS action_time TIMESTAMP;
ALTER TABLE workflow_task ADD COLUMN IF NOT EXISTS deadline TIMESTAMP;

COMMENT ON TABLE workflow_task IS '工作流任务表';
COMMENT ON COLUMN workflow_task.instance_id IS '流程实例ID';
COMMENT ON COLUMN workflow_task.node_code IS '节点编码';
COMMENT ON COLUMN workflow_task.node_name IS '节点名称';
COMMENT ON COLUMN workflow_task.assignee_id IS '处理人ID';
COMMENT ON COLUMN workflow_task.assignee_name IS '处理人名称';
COMMENT ON COLUMN workflow_task.status IS '任务状态: 0待处理, 1已同意, 2已驳回, 3已转交, 4已撤回';
COMMENT ON COLUMN workflow_task.comment IS '审批意见';
COMMENT ON COLUMN workflow_task.prev_task_id IS '前一任务ID';
COMMENT ON COLUMN workflow_task.transferred_from IS '转交来源人';
COMMENT ON COLUMN workflow_task.deadline IS '处理截止时间';

CREATE INDEX IF NOT EXISTS idx_wf_task_instance ON workflow_task(instance_id);
CREATE INDEX IF NOT EXISTS idx_wf_task_assignee ON workflow_task(assignee_id);
CREATE INDEX IF NOT EXISTS idx_wf_task_status ON workflow_task(status);
CREATE INDEX IF NOT EXISTS idx_wf_task_tenant ON workflow_task(tenant_id);

-- =====================================================
-- Part 4: 修复菜单指向实际页面
-- =====================================================

-- 更新工作流菜单,指向已实现的页面
UPDATE sys_menu SET component = 'views/workflow/instance-monitor' WHERE id = 802 AND component = 'views/workflow/instance/list';
UPDATE sys_menu SET component = 'views/workflow/task-management' WHERE id = 803 AND component = 'views/workflow/task/my-task';

-- 添加流程设计菜单指向画布页面
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, component, path, icon, sort, status)
VALUES (805, 800, '流程设计', 'workflow-designer', 1, 'views/workflow/definition/designer', '/workflow/designer', NULL, 5, 1)
ON CONFLICT (id) DO NOTHING;

-- 添加流程分析菜单
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, component, path, icon, sort, status)
VALUES (806, 800, '流程分析', 'workflow-analysis', 1, 'views/workflow/process-analysis', '/workflow/analysis', NULL, 6, 1)
ON CONFLICT (id) DO NOTHING;

-- 修复 mega 菜单中流程设计占位符
UPDATE sys_menu SET component = 'views/workflow/definition/designer' WHERE id = 80610;
