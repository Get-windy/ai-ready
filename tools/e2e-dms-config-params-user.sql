-- 配送参数（配送 → 配送配置 → 配送参数，菜单 80750 `dms:config-params`）E2E 专用账号与种子数据
-- 用法：每次跑 E2E 前整文件执行一次（把用例涉及的参数重置为可断言的已知值）。
--
-- 说明：本页测的是**已登记元数据**的参数（类型/范围/枚举校验依赖注册表），
--   故复用各业务域既有键并设为已知值；E2E 结束前会用 `/reset` 与批量保存还原（见脚本 §9）。

-- ═══ ① 清理历史 ═══
DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_config');
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_config');
DELETE FROM sys_user        WHERE username = 'e2e_config';

-- ═══ ② 验收账号 ═══
INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000010000, 1, 0, now(), now(), 'e2e_config', password, 'E2E参数', 'E2E参数',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000010001, 2099000000000010000, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000010002, 2099000000000010000, 1, true, 1, now(), now());

-- ═══ ③ 用例参数重置为已知值（租户 1 覆盖行）═══
INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
VALUES
  (1, 'dms.dispatch.max.concurrent',  '5',       '约束：单配送员最大并接在途单数(0=不限)',      'TENANT', 0, now(), now()),
  (1, 'dms.dispatch.strategy',        'NEAREST', '派单策略：NEAREST/BALANCED/SCORE/AREA',        'TENANT', 0, now(), now()),
  (1, 'dms.dispatch.require.online',  'true',    '约束：是否仅向「空闲」配送员派单',              'TENANT', 0, now(), now()),
  (1, 'dms.tracking.collect.hours',   '00:00-23:59', '实时跟踪合规：位置采集时段（支持跨天）',     'TENANT', 0, now(), now()),
  (1, 'map.amap.api-key',             'E2E-TEST-KEY-123456', '高德 Web API Key（E2E 敏感键明文，用于脱敏断言）', 'TENANT', 0, now(), now())
ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
  SET config_value = EXCLUDED.config_value, update_time = CURRENT_TIMESTAMP;

-- ═══ ④ 清理（验收结束后执行）═══
-- DELETE FROM sys_user_role   WHERE user_id = 2099000000000010000;
-- DELETE FROM sys_user_tenant WHERE user_id = 2099000000000010000;
-- DELETE FROM sys_user        WHERE id = 2099000000000010000;
-- 敏感键还原：UPDATE dms_config SET config_value='' WHERE tenant_id=1 AND config_key='map.amap.api-key';
