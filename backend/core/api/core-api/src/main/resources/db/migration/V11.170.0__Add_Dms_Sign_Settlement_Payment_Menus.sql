-- =============================================================================
-- 配送模块菜单补齐（二）：签收 / 结算 / 收款（PostgreSQL）
--   背景：后端 `sign` / `settlement` / `payment` 三个模块**接口已存在**，
--         但管理端既无 api 封装、也无页面、更无菜单 —— 履约与资金闭环缺落点。
--         本次补齐 3 个菜单 + 1 个新分组，并同步新增前端页面与 api。
--
--   分组归属：
--     · 签收管理   → 配送跟踪 (60505)，sort 3（履约闭环：实时跟踪 → 配送跟踪 → 签收管理）
--     · 结算收款   → **新增分组 60507**，sort 450（资金侧独立成组，插在配送跟踪400 与配送配置500 之间）
--         ├─ 配送结算 (80910)
--         └─ 收款管理 (80920)
--
--   ⚠️ 实现状态（挂菜单 ≠ 全功能可用，详见各页开发文档 §5）：
--     · 签收：后端仅「提交签收 / 按任务查签收」，**无台账分页、无审核流转接口**
--             （DmsSign 已有 auditStatus 字段待用）；司机端已有签收执行页。
--     · 结算：费率硬编码（SettlementService:39-48），推 ERP 仅写 outbox。
--     · 收款：二维码为示例串（PaymentService:48）待接真实支付渠道。
--
--   幂等：ON CONFLICT (id) DO UPDATE；菜单为系统级 tenant_id=0，超管全量下发。
-- =============================================================================

-- ① 新增分组「结算收款」
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      display_group, display_mode, menu_level)
VALUES ('60507', 0, 60005, '结算收款', 'mega:dms:settlement', 0, NULL, NULL, 'WalletOutlined', 450, 0, 1, 1, 1, 'tenant-admin', 0, 0, 0)
ON CONFLICT (id) DO UPDATE SET
  parent_id = EXCLUDED.parent_id, menu_name = EXCLUDED.menu_name, menu_code = EXCLUDED.menu_code,
  icon = EXCLUDED.icon, sort = EXCLUDED.sort, visible = EXCLUDED.visible, status = EXCLUDED.status,
  deleted = 0, update_time = CURRENT_TIMESTAMP;

-- ② 三个菜单项
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      display_group, display_mode, list_path, tag_label, menu_level)
VALUES
 ('80900', 0, 60505, '签收管理', 'dms:sign',       1, 'dms/sign',       'views/dms/sign/index.vue',       'FileDoneOutlined',   3, 0, 1, 1, 1, 'tenant-admin', 0, 0, NULL, NULL, 3),
 ('80910', 0, 60507, '配送结算', 'dms:settlement', 1, 'dms/settlement', 'views/dms/settlement/index.vue', 'MoneyCollectOutlined', 1, 0, 1, 1, 1, 'tenant-admin', 0, 0, NULL, NULL, 3),
 ('80920', 0, 60507, '收款管理', 'dms:payment',    1, 'dms/payment',    'views/dms/payment/index.vue',    'TransactionOutlined', 2, 0, 1, 1, 1, 'tenant-admin', 0, 0, NULL, NULL, 3)
ON CONFLICT (id) DO UPDATE SET
  parent_id    = EXCLUDED.parent_id,
  menu_name    = EXCLUDED.menu_name,
  menu_code    = EXCLUDED.menu_code,
  menu_type    = EXCLUDED.menu_type,
  path         = EXCLUDED.path,
  component    = EXCLUDED.component,
  icon         = EXCLUDED.icon,
  sort         = EXCLUDED.sort,
  visible      = EXCLUDED.visible,
  status       = EXCLUDED.status,
  display_mode = EXCLUDED.display_mode,
  list_path    = EXCLUDED.list_path,
  tag_label    = EXCLUDED.tag_label,
  menu_level   = EXCLUDED.menu_level,
  deleted      = 0,
  update_time  = CURRENT_TIMESTAMP;
