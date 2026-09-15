-- ============================================================
-- V9.10.0: 添加工作流菜单项
--
-- 变更说明：
--   在"设置 > 工作流"(61206)列下添加工作流相关页面菜单
--   包括：任务管理、流程监控、流程分析
--
-- 菜单 ID 规划：
--   62501-62503 — 工作流页面菜单
-- ============================================================

-- 工作流页面菜单（挂在 61206 "工作流" 列下）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (62501, 0, 61206, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '任务管理', 'workflow:task', 1, 'workflow/task-management', 'views/workflow/task-management.vue', 'ScheduleOutlined', 100, 1, 1, 'pc-admin', 0, 1),
  (62502, 0, 61206, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '流程监控', 'workflow:instance', 1, 'workflow/instance-monitor', 'views/workflow/instance-monitor.vue', 'MonitorOutlined', 200, 1, 1, 'pc-admin', 0, 1),
  (62503, 0, 61206, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '流程分析', 'workflow:analysis', 1, 'workflow/process-analysis', 'views/workflow/process-analysis.vue', 'BarChartOutlined', 300, 1, 1, 'pc-admin', 0, 1)
ON CONFLICT (id) DO NOTHING;
