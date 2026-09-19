-- =============================================================================
-- 系统模块 · 权限码种子补齐（README §7.1 P0-16）
--
-- 背景：系统模块 30 页涉及的后端控制器方法级 `@SaCheckPermission` 共引用 58 个权限码，
--   而 `sys_permission` 中**一个都没有**（2026-09-18 全量扫描 `@SaCheckPermission` 与库对比）。
--   后果：非超管账号对系统模块的任何接口调用都会 403（超管靠 UnifiedPermissionCacheService
--   的 `*` 通配绕过），且「角色-权限」配置页看不到这些码 —— 权限清单不完整、不可审计。
--
-- 口径：permission_type=3（接口权限）；tenant_id=1（平台自身）；api_path/method 留空
--   （逐个从控制器回填意义有限且控制器众多易失配，以 `permission_code` 为准）。
--   同时授权给超级管理员角色（role_id=1）—— 与 V11.394.0 / V11.395.0 口径一致：
--   超管另有 `*` 通配，这里只为让权限清单完整可审计；普通租户角色由租户管理员自行勾选。
--
-- id 段：sys_permission 91501~91558 / sys_role_permission 9159001~9159058
--   （已核 devdb 两段均未被占用；新增固定 id 的种子前务必实测占用）。
-- =============================================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
SELECT v.id, 1, 0, 0, now(), now(), v.name, v.code, 3, NULL, NULL, v.sort, 1, 0
FROM (VALUES
    (91501::BIGINT, '数据权限分配', 'data-permission:assign', 900),
    (91502::BIGINT, '数据权限校验', 'data-permission:check', 901),
    (91503::BIGINT, '数据权限新增', 'data-permission:create', 902),
    (91504::BIGINT, '数据权限删除', 'data-permission:delete', 903),
    (91505::BIGINT, '数据权限查询', 'data-permission:list', 904),
    (91506::BIGINT, '数据权限修改', 'data-permission:update', 905),
    (91507::BIGINT, '数据权限启停', 'data-permission:update-status', 906),
    (91508::BIGINT, '数据权限查看', 'data-permission:view', 907),
    (91509::BIGINT, '备份管理新增', 'datasource:backup:create', 908),
    (91510::BIGINT, '备份管理删除', 'datasource:backup:delete', 909),
    (91511::BIGINT, '备份管理查询', 'datasource:backup:list', 910),
    (91512::BIGINT, '备份管理恢复', 'datasource:backup:restore', 911),
    (91513::BIGINT, '清理规则新增', 'datasource:cleanup:create', 912),
    (91514::BIGINT, '清理规则删除', 'datasource:cleanup:delete', 913),
    (91515::BIGINT, '清理规则执行', 'datasource:cleanup:execute', 914),
    (91516::BIGINT, '清理规则查询', 'datasource:cleanup:list', 915),
    (91517::BIGINT, '清理规则修改', 'datasource:cleanup:update', 916),
    (91518::BIGINT, '慢查询导出', 'datasource:slowquery:export', 917),
    (91519::BIGINT, '慢查询查询', 'datasource:slowquery:list', 918),
    (91520::BIGINT, '同步任务新增', 'datasource:sync:create', 919),
    (91521::BIGINT, '同步任务删除', 'datasource:sync:delete', 920),
    (91522::BIGINT, '同步任务执行', 'datasource:sync:execute', 921),
    (91523::BIGINT, '同步任务查询', 'datasource:sync:list', 922),
    (91524::BIGINT, '同步任务修改', 'datasource:sync:update', 923),
    (91525::BIGINT, '登录日志详情', 'log:login:detail', 924),
    (91526::BIGINT, '登录日志统计', 'log:login:stats', 925),
    (91527::BIGINT, '租户套餐新增', 'platform:tenant-package:create', 926),
    (91528::BIGINT, '租户套餐删除', 'platform:tenant-package:delete', 927),
    (91529::BIGINT, '租户套餐查询', 'platform:tenant-package:list', 928),
    (91530::BIGINT, '租户套餐修改', 'platform:tenant-package:update', 929),
    (91531::BIGINT, '配额管理新增', 'platform:tenant-quota:create', 930),
    (91532::BIGINT, '配额管理删除', 'platform:tenant-quota:delete', 931),
    (91533::BIGINT, '配额管理查询', 'platform:tenant-quota:list', 932),
    (91534::BIGINT, '配额管理修改', 'platform:tenant-quota:update', 933),
    (91535::BIGINT, '菜单管理新增', 'system:menu:create', 934),
    (91536::BIGINT, '菜单管理删除', 'system:menu:delete', 935),
    (91537::BIGINT, '菜单管理详情', 'system:menu:detail', 936),
    (91538::BIGINT, '菜单管理查询', 'system:menu:list', 937),
    (91539::BIGINT, '菜单管理修改', 'system:menu:update', 938),
    (91540::BIGINT, '菜单管理启停', 'system:menu:update-status', 939),
    (91541::BIGINT, '模块管理新增', 'system:module:create', 940),
    (91542::BIGINT, '模块管理删除', 'system:module:delete', 941),
    (91543::BIGINT, '模块管理查询', 'system:module:list', 942),
    (91544::BIGINT, '模块管理修改', 'system:module:update', 943),
    (91545::BIGINT, '角色管理分配菜单', 'system:role:assign-menu', 944),
    (91546::BIGINT, '角色管理分配权限', 'system:role:assign-permission', 945),
    (91547::BIGINT, '角色管理详情', 'system:role:detail', 946),
    (91548::BIGINT, '角色管理启停', 'system:role:update-status', 947),
    (91549::BIGINT, '租户管理审核', 'system:tenant:approve', 948),
    (91550::BIGINT, '租户管理新增', 'system:tenant:create', 949),
    (91551::BIGINT, '租户管理删除', 'system:tenant:delete', 950),
    (91552::BIGINT, '租户管理修改', 'system:tenant:update', 951),
    (91553::BIGINT, '用户管理分配角色', 'system:user:assign-role', 952),
    (91554::BIGINT, '用户管理重置密码', 'system:user:reset-password', 953),
    (91555::BIGINT, '用户管理启停', 'system:user:update-status', 954),
    (91556::BIGINT, '租户菜单授权分配', 'tenant:menu:assign', 955),
    (91557::BIGINT, '租户菜单授权查询', 'tenant:menu:query', 956),
    (91558::BIGINT, '租户菜单授权移除', 'tenant:menu:remove', 957)
) AS v(id, name, code, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9159000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code IN (
        'data-permission:assign',
        'data-permission:check',
        'data-permission:create',
        'data-permission:delete',
        'data-permission:list',
        'data-permission:update',
        'data-permission:update-status',
        'data-permission:view',
        'datasource:backup:create',
        'datasource:backup:delete',
        'datasource:backup:list',
        'datasource:backup:restore',
        'datasource:cleanup:create',
        'datasource:cleanup:delete',
        'datasource:cleanup:execute',
        'datasource:cleanup:list',
        'datasource:cleanup:update',
        'datasource:slowquery:export',
        'datasource:slowquery:list',
        'datasource:sync:create',
        'datasource:sync:delete',
        'datasource:sync:execute',
        'datasource:sync:list',
        'datasource:sync:update',
        'log:login:detail',
        'log:login:stats',
        'platform:tenant-package:create',
        'platform:tenant-package:delete',
        'platform:tenant-package:list',
        'platform:tenant-package:update',
        'platform:tenant-quota:create',
        'platform:tenant-quota:delete',
        'platform:tenant-quota:list',
        'platform:tenant-quota:update',
        'system:menu:create',
        'system:menu:delete',
        'system:menu:detail',
        'system:menu:list',
        'system:menu:update',
        'system:menu:update-status',
        'system:module:create',
        'system:module:delete',
        'system:module:list',
        'system:module:update',
        'system:role:assign-menu',
        'system:role:assign-permission',
        'system:role:detail',
        'system:role:update-status',
        'system:tenant:approve',
        'system:tenant:create',
        'system:tenant:delete',
        'system:tenant:update',
        'system:user:assign-role',
        'system:user:reset-password',
        'system:user:update-status',
        'tenant:menu:assign',
        'tenant:menu:query',
        'tenant:menu:remove'
)
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                WHERE rp.role_id = 1 AND rp.permission_id = p.id);
