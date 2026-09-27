# -*- coding: utf-8 -*-
"""
「平台开模块 → 租户自助配权限」第二层链路打通（一次性执行脚本，2026-09-26）。

背景（配送模块审计 §4.1 定性修正）：
  本仓授权是两层 ——
    ① 模块授权（平台方）：sys_tenant_module + sys_module_permission → ModuleEntitlementService
    ② 权限（租户内管理员）：sys_role_permission
  实测：模块门已开（租户 1/2 均开通 dms 与 settings），但**第二层无人可用** ——
  除超管外没有任何角色持有 `tenant-admin:role:*` / `tenant-admin:permission:*`，
  ⇒ 租户管理员无法自助给本租户角色配权限 ⇒ 该租户所有角色都拿不到 dms 权限码 ⇒ 接口 403。
  这不是"平台忘了给配送授权"，而是**设计链路的第二层没启用**。

前置校验：`tenant-admin:` 前缀归属 **settings** 模块（见 sys_module_permission），
         租户 1/2 均已开通 settings ⇒ 模块门不会拦。脚本会先断言这点。

矩阵（最小集）：
  · SYSTEM_ADMIN   系统管理员（tid=1）→ 完整租户内管理（角色/权限/用户角色分配）
  · E2E_T2_ADMIN   租户2管理员（tid=2）→ 同上（★核心：让租户管理员能自助配置）
  · DEPT_ADMIN     部门管理员（tid=1）→ 只读子集
  · SUPER_ADMIN    超级管理员 → 通配符 *，无需授权行

幂等：按 (role_id, permission_id) 判重，重复执行只补差集。
用法: python tools/grant-tenant-admin-permissions.py
"""
import os
import json
import sys
import io

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

SYSTEM_ADMIN = 2065122951570362369
DEPT_ADMIN = 2065122951620694018
T2_ADMIN = 2099000000000009031

# 租户内「人/角色/权限」管理 —— 完整集（能配置本租户角色权限）
FULL = [
    'tenant-admin:role:list', 'tenant-admin:role:detail', 'tenant-admin:role:create',
    'tenant-admin:role:update', 'tenant-admin:role:delete',
    'tenant-admin:role:assign-permission', 'tenant-admin:role:assign-menu',
    'tenant-admin:permission:list', 'tenant-admin:permission:view', 'tenant-admin:permission:assign',
    'tenant-admin:user:list', 'tenant-admin:user:detail', 'tenant-admin:user:assign-role',
]
# 只读子集
READONLY = [
    'tenant-admin:role:list', 'tenant-admin:role:detail',
    'tenant-admin:permission:list', 'tenant-admin:permission:view',
    'tenant-admin:user:list',
]

PLANS = [
    (SYSTEM_ADMIN, FULL, '系统管理员 → 租户内人/角色/权限管理（完整）'),
    (T2_ADMIN, FULL, '租户2管理员 → 租户内人/角色/权限管理（完整）★'),
    (DEPT_ADMIN, READONLY, '部门管理员 → 同域只读'),
]

GRANT_SQL = """
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time, create_by)
SELECT (SELECT COALESCE(max(id), 0) FROM sys_role_permission) + row_number() OVER (ORDER BY p.id),
       %(role)s, p.id, 0, now(), 1
FROM sys_permission p
WHERE p.deleted = 0 AND p.status = 0
  AND p.permission_code = ANY(%(codes)s)
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = %(role)s AND rp.permission_id = p.id)
"""


def main():
    import psycopg2
    with psycopg2.connect(**CONN) as conn:
        with conn.cursor() as cur:
            # 0) 前置：确认 tenant-admin: 归属 settings，且目标角色所在租户已开通
            cur.execute("""SELECT module_code FROM sys_module_permission
                           WHERE permission_prefix = 'tenant-admin:' AND deleted = 0""")
            row = cur.fetchone()
            mod = row[0] if row else None
            print('tenant-admin: 归属模块 = %s' % mod)
            if mod != 'settings':
                print('!! 归属与预期不符，终止'); return
            cur.execute("""SELECT tenant_id, status FROM sys_tenant_module
                           WHERE module_code = 'settings' AND deleted = 0 ORDER BY tenant_id""")
            opens = cur.fetchall()
            print('settings 模块开通情况:', opens)
            for tid, st in opens:
                if st != 0:
                    print('!! 租户 %s 的 settings 非「正常」，模块门会拦，终止' % tid); return

            # 1) 备份
            cur.execute("""SELECT id, role_id, permission_id, tenant_id FROM sys_role_permission
                           WHERE role_id IN (%s, %s, %s) ORDER BY id""",
                        (SYSTEM_ADMIN, DEPT_ADMIN, T2_ADMIN))
            cols = [d[0] for d in cur.description]
            before = [dict(zip(cols, map(str, r))) for r in cur.fetchall()]
            os.makedirs(os.path.join(ROOT, 'tool-results'), exist_ok=True)
            dest = os.path.join(ROOT, 'tool-results', 'role_perm_backup_before_tenant_admin_grant_20260926.json')
            with open(dest, 'w', encoding='utf-8') as f:
                json.dump(before, f, ensure_ascii=False, indent=1)
            print('已备份受影响角色授权 %d 行 → %s' % (len(before), os.path.basename(dest)))

            # 2) 授权
            for role_id, codes, label in PLANS:
                cur.execute(GRANT_SQL, {'role': role_id, 'codes': codes})
                print('%-48s 新增 %d 条' % (label, cur.rowcount))

            # 3) 回查
            print()
            print('=== 授权后各角色持有 tenant-admin: 权限码数 ===')
            cur.execute("""SELECT rp.role_id, count(*) FROM sys_role_permission rp
                           JOIN sys_permission p ON p.id = rp.permission_id
                           WHERE p.permission_code LIKE 'tenant-admin:%%'
                           GROUP BY rp.role_id ORDER BY rp.role_id""")
            for rid, cnt in cur.fetchall():
                print('  role_id=%-22s %d' % (rid, cnt))


if __name__ == '__main__':
    main()
