-- =============================================================================
-- 配送模块菜单补齐（PostgreSQL）
--   背景：配送模块后端/前端已有 19 个页面，但菜单树只有 14 个叶子项，
--         导致「配送仪表盘 / 路线规划 / 实名认证 / 智能调度 / 订单池 /
--               配送跟踪 / 渠道管理 / 配送配置」8 个页面**用户点不到**
--         （页面与 componentMap 均已就绪，仅缺 sys_menu 记录）。
--
--   分组口径（沿用现有 9 个分组，不新增一级分栏；括号内为菜单ID）：
--     · 配送业务 (60401)  ← 配送仪表盘（运营概览，与配送单/配送查询同组）
--     · 配送路线 (60501)  ← 路线规划（路径规划工具，与配送路线单同组）
--     · 人车管理 (60502)  ← 实名认证（人车绑定核验/预警/巡检，属"人车"治理）
--     · 调度管理 (60504)  ← 智能调度、订单池（调度三件套：任务+策略+池）
--     · 配送跟踪 (60505)  ← 配送跟踪（轨迹分页，与实时跟踪同组）
--     · 配送配置 (60506)  ← 渠道管理（运力渠道）、配送配置（租户级 KV）
--
--   ⚠️ 已知实现状态（挂菜单不等于功能可用，见《配送模块 README》§1.2）：
--     - 运行 jar 未包含 dms-delivery → 全部 /api/dms/* 500（**待修构建链**）
--     - 配送仪表盘：前端调 /dms/dashboard/*，后端无 DashboardController
--     - 配送跟踪：前端调 /dms/tracking/page，后端未实现该分页接口
--     - 智能调度：views/dms/dispatch 含硬编码假数据，待重写
--     - 订单池：前端只读，抢单/出价 UI 与后端 enableBid/settleBid 未暴露
--     - 渠道管理：5 个渠道适配器为 Stub，getAdapter() 全仓无调用
--   以上问题不改菜单挂载结论（先让能力可见，再逐项修复）。
--
--   幂等：ON CONFLICT (id) DO UPDATE；菜单为系统级 tenant_id=0，超管全量下发。
-- =============================================================================

INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      display_group, display_mode, list_path, tag_label, menu_level)
VALUES
 -- 配送业务
 ('80820', 0, 60401, '配送仪表盘', 'dms:dashboard',      1, 'dms/dashboard',    'views/dms/dashboard/index.vue',        'DashboardOutlined',    2, 0, 1, 1, 1, 'tenant-admin', 0, 0, NULL, NULL, 3),
 -- 配送路线
 ('80830', 0, 60501, '路线规划',   'dms:route-plan',     1, 'dms/route',        'views/dms/route/index.vue',            'EnvironmentOutlined',  2, 0, 1, 1, 1, 'tenant-admin', 0, 0, NULL, NULL, 3),
 -- 人车管理
 ('80840', 0, 60502, '实名认证',   'dms:verification',   1, 'dms/verification', 'views/dms/verification/index.vue',     'SafetyCertificateOutlined', 4, 0, 1, 1, 1, 'tenant-admin', 0, 0, NULL, NULL, 3),
 -- 调度管理
 ('80850', 0, 60504, '智能调度',   'dms:dispatch',       1, 'dms/dispatch',     'views/dms/dispatch/index.vue',         'ThunderboltOutlined',  2, 0, 1, 1, 1, 'tenant-admin', 0, 0, NULL, NULL, 3),
 ('80860', 0, 60504, '订单池',     'dms:order-pool',     1, 'dms/order-pool',   'views/dms/order-pool/index.vue',       'UnorderedListOutlined',3, 0, 1, 1, 1, 'tenant-admin', 0, 0, NULL, NULL, 3),
 -- 配送跟踪
 ('80870', 0, 60505, '配送跟踪',   'dms:tracking',       1, 'dms/tracking',     'views/dms/tracking/index.vue',         'AimOutlined',          2, 0, 1, 1, 1, 'tenant-admin', 0, 0, NULL, NULL, 3),
 -- 配送配置
 ('80880', 0, 60506, '渠道管理',   'dms:channel',        1, 'dms/channel',      'views/dms/channel/index.vue',          'ApiOutlined',          2, 0, 1, 1, 1, 'tenant-admin', 0, 0, NULL, NULL, 3),
 ('80890', 0, 60506, '配送配置',   'dms:config',         1, 'dms/config',       'views/dms/config/index.vue',           'SettingOutlined',      3, 0, 1, 1, 1, 'tenant-admin', 0, 0, NULL, NULL, 3)
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
  menu_level   = EXCLUDED.menu_level,
  deleted      = 0,
  update_time  = CURRENT_TIMESTAMP;
