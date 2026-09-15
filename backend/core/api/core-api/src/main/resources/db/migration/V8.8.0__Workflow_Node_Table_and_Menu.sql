-- V8.8.0 工作流节点表 + 工作流菜单项

-- ─────────────────────────────────────────────────────────────────────
-- 1. 创建工作流节点表
-- ─────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS workflow_node (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT,
    definition_id        BIGINT NOT NULL,
    node_code            VARCHAR(50),
    node_name            VARCHAR(100) NOT NULL,
    node_type            INTEGER NOT NULL DEFAULT 2,
    node_order           INTEGER DEFAULT 0,
    assignee_type        INTEGER DEFAULT 1,
    assignee_ids         TEXT,
    role_ids             TEXT,
    condition_expr       TEXT,
    next_node_id         BIGINT,
    branch_nodes         TEXT,
    can_reject           INTEGER DEFAULT 1,
    can_transfer         INTEGER DEFAULT 1,
    time_limit           INTEGER,
    timeout_action       INTEGER DEFAULT 3,
    description          VARCHAR(500)
);

COMMENT ON TABLE workflow_node IS '工作流节点表 - 流程节点定义';
COMMENT ON COLUMN workflow_node.id IS '节点ID';
COMMENT ON COLUMN workflow_node.definition_id IS '流程定义ID';
COMMENT ON COLUMN workflow_node.node_code IS '节点编码';
COMMENT ON COLUMN workflow_node.node_name IS '节点名称';
COMMENT ON COLUMN workflow_node.node_type IS '节点类型: 1-开始 2-审批 3-抄送 4-条件分支 5-结束';
COMMENT ON COLUMN workflow_node.node_order IS '节点顺序';
COMMENT ON COLUMN workflow_node.assignee_type IS '审批人类型: 1-指定人员 2-部门负责人 3-上级领导 4-申请人自选 5-流程发起人';
COMMENT ON COLUMN workflow_node.assignee_ids IS '审批人ID列表(JSON)';
COMMENT ON COLUMN workflow_node.role_ids IS '审批角色ID列表(JSON)';
COMMENT ON COLUMN workflow_node.condition_expr IS '条件表达式(JSON)';
COMMENT ON COLUMN workflow_node.next_node_id IS '下一节点ID';
COMMENT ON COLUMN workflow_node.branch_nodes IS '分支节点列表(JSON)';
COMMENT ON COLUMN workflow_node.can_reject IS '是否可驳回: 0-否 1-是';
COMMENT ON COLUMN workflow_node.can_transfer IS '是否可转交: 0-否 1-是';
COMMENT ON COLUMN workflow_node.time_limit IS '审批时限(小时)';
COMMENT ON COLUMN workflow_node.timeout_action IS '超时处理: 1-自动通过 2-自动驳回 3-提醒';
COMMENT ON COLUMN workflow_node.description IS '节点描述';

CREATE INDEX idx_workflow_node_definition ON workflow_node(definition_id);
CREATE INDEX idx_workflow_node_tenant ON workflow_node(tenant_id);

-- ─────────────────────────────────────────────────────────────────────
-- 2. 创建工作流菜单项
-- ─────────────────────────────────────────────────────────────────────

-- 工作流主菜单 (id=800)
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, component, path, icon, sort, status)
VALUES (800, 0, '工作流程', 'workflow', 0, 'Layout', '/workflow', 'workflow', 6, 1)
ON CONFLICT (id) DO NOTHING;

-- 流程定义菜单 (id=801)
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, component, path, icon, sort, status)
VALUES (801, 800, '流程定义', 'workflow-definition', 1, 'views/workflow/definition/list', '/workflow/definition', NULL, 1, 1)
ON CONFLICT (id) DO NOTHING;

-- 流程定义按钮权限
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, component, path, icon, sort, status)
VALUES
(8011, 801, '新增流程', 'workflow-definition-create', 2, NULL, NULL, NULL, 1, 1),
(8012, 801, '编辑流程', 'workflow-definition-edit', 2, NULL, NULL, NULL, 2, 1),
(8013, 801, '删除流程', 'workflow-definition-delete', 2, NULL, NULL, NULL, 3, 1),
(8014, 801, '发布流程', 'workflow-definition-publish', 2, NULL, NULL, NULL, 4, 1),
(8015, 801, '停用流程', 'workflow-definition-disable', 2, NULL, NULL, NULL, 5, 1)
ON CONFLICT (id) DO NOTHING;

-- 流程实例菜单 (id=802)
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, component, path, icon, sort, status)
VALUES (802, 800, '流程实例', 'workflow-instance', 1, 'views/workflow/instance/list', '/workflow/instance', NULL, 2, 1)
ON CONFLICT (id) DO NOTHING;

-- 我的待办菜单 (id=803)
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, component, path, icon, sort, status)
VALUES (803, 800, '我的待办', 'my-task', 1, 'views/workflow/task/my-task', '/workflow/task', NULL, 3, 1)
ON CONFLICT (id) DO NOTHING;

-- 我的已办菜单 (id=804)
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, component, path, icon, sort, status)
VALUES (804, 800, '我的已办', 'my-done', 1, 'views/workflow/task/my-done', '/workflow/done', NULL, 4, 1)
ON CONFLICT (id) DO NOTHING;

-- 任务审批按钮权限
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, component, path, icon, sort, status)
VALUES
(8031, 803, '审批通过', 'workflow-task-approve', 2, NULL, NULL, NULL, 1, 1),
(8032, 803, '审批驳回', 'workflow-task-reject', 2, NULL, NULL, NULL, 2, 1),
(8033, 803, '转交任务', 'workflow-task-transfer', 2, NULL, NULL, NULL, 3, 1),
(8034, 803, '撤回流程', 'workflow-instance-withdraw', 2, NULL, NULL, NULL, 4, 1)
ON CONFLICT (id) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────
-- 3. 添加 WorkflowNode Mapper XML
-- ─────────────────────────────────────────────────────────────────────
-- Mapper XML 文件需要单独创建，此处仅创建表结构