# -*- coding: utf-8 -*-
"""
人力资源模块权限授权（一次性执行脚本，2026-09-23）。

依据：HR_MODULE_AUDIT_20260923.md §P0-2 / §6 第一批第 3 条。

背景：`hr:*` 共 40 条权限码此前**只授予 SUPER_ADMIN**（实测 40/40），
      SYSTEM_ADMIN / DEPT_ADMIN / ROLE_MQD2X2DI / E2E_T2_ADMIN 均为 0 条。
      叠加「菜单可见性从权限码派生」⇒ 非超管不仅调接口 403，连员工/考勤/请假/薪资/
      绩效/岗位编制这 6 个菜单都收不到。而历次 E2E 都用超管跑（走 `*` 通配）⇒ 假绿。

矩阵：
  · SUPER_ADMIN   超级管理员  → 已有全部（`*` 通配直通，无需授权行）        不动
  · SYSTEM_ADMIN  系统管理员  → HR 全部权限，**但排除 `hr:salary:*`**       本脚本
      └ 依据 V11.380.0 权限种子自己的注释：「薪资（薪资保密：与其余 HR 权限分开授予）」。
        薪酬类权限应由专门的薪酬角色按需授予，不随「系统管理员」默认放行。
  · DEPT_ADMIN    部门管理员  → HR 只读子集（:list/:view/:detail/:query/:export） 本脚本
      └ 与 tools/grant-storage-permissions.py 的部门管理员口径一致。
  · E2E_T2_ADMIN  测试角色    → 不动（属租户 2 的 E2E 夹具）

幂等：按 (role_id, permission_id) 判重，重复执行只补差集。
权限缓存：改完需等 L1 Caffeine(30s) + L2 Redis(5min) 过期，或重启后端，否则菜单/鉴权结果不变。

用法: python tools/grant-hr-permissions.py
"""
import io
import json
import os
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

SA = 2065122951570362369   # 系统管理员 SYSTEM_ADMIN
DA = 2065122951620694018   # 部门管理员 DEPT_ADMIN

HR_FILTER = "(p.permission_code LIKE 'hr:%%')"

# {cond} 为额外的权限码筛选条件；%(role)s 为角色 id
GRANT_SQL = """
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time, create_by)
SELECT (SELECT COALESCE(max(id), 0) FROM sys_role_permission) + row_number() OVER (ORDER BY p.id),
       %(role)s, p.id, 1, now(), 1
FROM sys_permission p
WHERE p.deleted = 0 AND p.status = 0
  AND """ + HR_FILTER + """
  AND {cond}
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = %(role)s AND rp.permission_id = p.id)
"""

PLANS = [
    (SA, "p.permission_code NOT LIKE 'hr:salary:%'",
     '系统管理员 → HR 全部（排除 hr:salary:* 薪资保密）'),
    # ⚠️ 只读子集也必须排除 hr:salary:* —— `hr:salary:list` 也以 `:list` 结尾，
    #    不排除就等于把「全租户员工工资可查」交给部门管理员，与薪资保密口径相悖。
    (DA, "p.permission_code NOT LIKE 'hr:salary:%' AND p.permission_code ~ ':(list|view|detail|query|export)$'",
     '部门管理员 → HR 只读子集（排除 hr:salary:*）'),
]


def main():
    import psycopg2
    with psycopg2.connect(**CONN) as conn:
        with conn.cursor() as cur:
            # 1) 备份受影响角色的现有授权行（回滚用）
            cur.execute("""SELECT id, role_id, permission_id, tenant_id FROM sys_role_permission
                           WHERE role_id IN (%s, %s) ORDER BY id""", (SA, DA))
            cols = [d[0] for d in cur.description]
            before = [dict(zip(cols, map(str, r))) for r in cur.fetchall()]
            os.makedirs(os.path.join(ROOT, 'tool-results'), exist_ok=True)
            dest = os.path.join(ROOT, 'tool-results', 'role_permission_backup_before_HR_20260923.json')
            with open(dest, 'w', encoding='utf-8') as f:
                json.dump(before, f, ensure_ascii=False, indent=1)
            print('已备份受影响角色现有授权 %d 行 → %s' % (len(before), os.path.basename(dest)))

            # 2) 授权
            # ⚠️ cond 里的 `%` 必须转义成 `%%`：本 SQL 模板中已有 `LIKE 'hr:%%'` 这类占位符转义，
            #    两处口径不一致会直接抛 "argument formats can't be mixed"（实踩）。
            for role_id, cond, label in PLANS:
                cur.execute(GRANT_SQL.format(cond=cond.replace('%', '%%')), {'role': role_id})
                print('%-44s 新增 %d 条' % (label, cur.rowcount))

            # 3) 回查：各角色持有的 HR 权限码数
            cur.execute("""SELECT r.role_code, count(*) FROM sys_role_permission rp
                           JOIN sys_role r ON r.id = rp.role_id
                           JOIN sys_permission p ON p.id = rp.permission_id
                           WHERE p.permission_code LIKE 'hr:%'
                           GROUP BY r.role_code ORDER BY 2 DESC""")
            cur.execute("SELECT count(*) FROM sys_permission WHERE permission_code LIKE 'hr:%' AND deleted = 0 AND status = 0")
            total = cur.fetchone()[0]
            cur.execute("""SELECT r.role_code, count(*) FROM sys_role_permission rp
                           JOIN sys_role r ON r.id = rp.role_id
                           JOIN sys_permission p ON p.id = rp.permission_id
                           WHERE p.permission_code LIKE 'hr:%' AND p.deleted = 0 AND p.status = 0
                           GROUP BY r.role_code ORDER BY 2 DESC""")
            print()
            print('=== 授权后各角色持有的 HR 权限码数（库中 hr:* 共 %d 条）===' % total)
            for code, cnt in cur.fetchall():
                print('  %-24s %d' % (code, cnt))
            print()
            print('⚠️ 权限缓存未失效：等 L1(30s)+L2(5min) 过期，或重启后端后再验证菜单与接口。')


if __name__ == '__main__':
    main()
