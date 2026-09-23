# -*- coding: utf-8 -*-
"""
配送模块（DMS）权限授权（一次性执行脚本，2026-09-23）。

矩阵（与 tools/grant-storage-permissions.py 的仓储做法一致）：
  · SYSTEM_ADMIN  系统管理员   → 配送自有域全部权限（dms:* / delivery:* / md:route*）
  · DEPT_ADMIN    部门管理员   → 配送自有域只读权限（:list/:view/:detail/:query/:export）
  · SUPER_ADMIN   超级管理员   → 已有全部（通配符 * 直通，无需授权行）
  · E2E_T2_ADMIN  测试角色     → 不动

背景：配送域权限码此前**只授予 SUPER_ADMIN**（146/146），其余角色零授权
⇒ 非超管用户能看见菜单（菜单派生 fail-open）但调用任何配送接口都 403。
同为第三例（采购 PURCHASE_AUDIT / 仓储 STORAGE_AUDIT 同源）。

⚠️ 刻意**不处理 `trade:%`**（20 个：api-monitor / channel / external-order / inventory-sync）——
   那属交易模块审计范围（见 TRADE_MODULE_AUDIT_20260923.md），避免与并行会话冲突。
   受此影响：《API监控》页（90107，挂在配送菜单下）对非超管仍不可用。

幂等：按 (role_id, permission_id) 判重，重复执行只补差集。
用法: python tools/grant-dms-permissions.py
"""
import os
import json
import sys
import io

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

SA = 2065122951570362369   # 系统管理员
DA = 2065122951620694018   # 部门管理员

# 配送自有域（不含 trade:%）
SCOPE = ("(p.permission_code LIKE 'dms:%%' OR p.permission_code LIKE 'delivery:%%' "
         "OR p.permission_code LIKE 'md:route%%')")

GRANT_SQL = """
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time, create_by)
SELECT (SELECT COALESCE(max(id), 0) FROM sys_role_permission) + row_number() OVER (ORDER BY p.id),
       %(role)s, p.id, 0, now(), 1
FROM sys_permission p
WHERE p.deleted = 0 AND p.status = 0
  AND """ + SCOPE + """
  AND {cond}
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = %(role)s AND rp.permission_id = p.id)
"""

PLANS = [
    (SA, '1 = 1', '系统管理员 → 配送自有域全部权限'),
    (DA, "p.permission_code ~ ':(list|view|detail|query|export)$'", '部门管理员 → 配送自有域只读权限'),
]


def main():
    import psycopg2
    with psycopg2.connect(**CONN) as conn:
        with conn.cursor() as cur:
            # 1) 备份受影响角色的现有授权行
            cur.execute("""SELECT id, role_id, permission_id, tenant_id FROM sys_role_permission
                           WHERE role_id IN (%s, %s) ORDER BY id""", (SA, DA))
            cols = [d[0] for d in cur.description]
            before = [dict(zip(cols, map(str, r))) for r in cur.fetchall()]
            os.makedirs(os.path.join(ROOT, 'tool-results'), exist_ok=True)
            dest = os.path.join(ROOT, 'tool-results', 'role_permission_backup_before_dms_grant_20260923.json')
            with open(dest, 'w', encoding='utf-8') as f:
                json.dump(before, f, ensure_ascii=False, indent=1)
            print('已备份受影响角色现有授权 %d 行 → %s' % (len(before), os.path.basename(dest)))

            # 2) 授权
            for role_id, cond, label in PLANS:
                cur.execute(GRANT_SQL.format(cond=cond), {'role': role_id})
                print('%-34s 新增 %d 条' % (label, cur.rowcount))

            # 3) 回查
            cur.execute("""SELECT rp.role_id, count(*) FROM sys_role_permission rp
                           JOIN sys_permission p ON p.id = rp.permission_id
                           WHERE """ + SCOPE + """
                           GROUP BY rp.role_id ORDER BY rp.role_id""")
            print()
            print('=== 授权后各角色的配送自有域权限码数 ===')
            for rid, cnt in cur.fetchall():
                print('  role_id=%-22s %d' % (rid, cnt))


if __name__ == '__main__':
    main()
