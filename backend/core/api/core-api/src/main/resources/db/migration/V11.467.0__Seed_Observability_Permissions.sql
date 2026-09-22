-- 可观测性域权限码种子（E-01 erpobserv 批次，2026-09-21）
--
-- 【零码模块】库中**没有 `metrics:` / `monitor:` 前缀的码**（实测 0 条）。按 E-04 口径
--   现场建码：24 个待补端点 → 9 个码，**全部新增、无命中**，VALUES 占 118000~118008。
--
-- 【为什么必须补】E-01 要给 erp-observability 的裸控制器补 @SaCheckPermission，而
--   **没有码就补注解 ⇒ 该接口对所有非超管一律 403**（本仓铁律/历史事故）。
--
-- 【口径要点】全部写在 `tools/gen-module-permission-seed.py` 的 `MODULES['erpobserv']`：
--   · 两个控制器的类级路径**都只有容器段**（`/api/erp/metrics`、`/api/erp/monitor`）⇒
--     用 base_overrides 指定资源名 `metric` / `business-metric`，否则资源会取方法路径第一段，
--     产出 `metrics:dashboard:*`、`monitor:core:*`（core/order/user/sales 被当成"资源名"）。
--   · `POST /current/batch`、`POST /history`、`POST /query` 都是**读**（POST 传查询条件）
--     ⇒ 归 list；`POST /refresh`（刷新指标缓存）归 execute。若落 RULES 的 create 兜底，
--     会变成"能新建指标的人才能查历史"。
--   · ⚠️ **不复用** core-api 的 `system:monitor*`：那批是**平台监控**（`/api/monitor/**`），
--     本批是 **ERP 业务指标**（`/api/erp/metrics`、`/api/erp/monitor/**`），对象不同。
--   · 域 `metrics:` / `monitor:` **没有**进 `sys_module_permission` 的「模块→码前缀」映射，
--     按 ModuleEntitlementService 的口径「码无归属前缀 → 不拦」（该门只表达"卖没卖模块"），
--     故不会因为新域而多一道模块开关；若将来把它们归到某模块，需另写映射迁移。
--
-- 【id 号段】权限码 118000 起（工具槽位 slot=18）、角色关联 9680000 起。
--   实测 118000~118999 = 0 行、9680000~9689999 = 0 行，均空闲。
--
-- 【有意排除的端点】无 —— 两个控制器的 24 个端点全部补码。
--
-- 【遗留 / 拿不准】
--   · 这两个控制器在 pc-admin 里**零调用方**（实测 grep 无命中）：疑似给外部监控/大屏
--     或运维使用的 API。本批照样建码（一旦接线即生效），但"哪个角色该拿到这些码"
--     需要业务侧定；默认只关联超管角色（与其它批次一致）。
--   · `MetricsController` 与 `BusinessMetricController` 功能有重叠（都在读指标/仪表盘），
--     本批按两个控制器分别建 `metrics:metric:*` 与 `monitor:business-metric:*`，
--     未做码合并 —— 合并会掩盖"两套实现"这一现状，建议随重复实现清理一并裁决。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (118000, '可观测性指标详情', 'metrics:metric:detail', '/api/erp/metrics/current/{metricCode}', 'GET', 1200),
 (118001, '可观测性指标执行', 'metrics:metric:execute', '/api/erp/metrics/refresh', 'POST', 1201),
 (118002, '可观测性指标查询', 'metrics:metric:list', '/api/erp/metrics/type/{type}', 'GET', 1202),
 (118003, '可观测性指标查看', 'metrics:metric:view', '/api/erp/metrics/dashboard', 'GET', 1203),
 (118004, '可观测性业务指标新增', 'monitor:business-metric:create', '/api/erp/monitor/definitions', 'POST', 1204),
 (118005, '可观测性业务指标删除', 'monitor:business-metric:delete', '/api/erp/monitor/definitions/{id}', 'DELETE', 1205),
 (118006, '可观测性业务指标执行', 'monitor:business-metric:execute', '/api/erp/monitor/refresh', 'POST', 1206),
 (118007, '可观测性业务指标查询', 'monitor:business-metric:list', '/api/erp/monitor/query', 'POST', 1207),
 (118008, '可观测性业务指标查看', 'monitor:business-metric:view', '/api/erp/monitor/realtime', 'GET', 1208)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9680000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 118000 AND 118009
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
