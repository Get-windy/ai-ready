# -*- coding: utf-8 -*-
"""
人力资源模块数据库盘点（只读）：
  1. hr_* 表清单 + 行数 + 是否有 tenant_id / deleted 列
  2. HR 用到的实体类 @TableName 与表结构逐列对账（缺列/多列）
  3. HR 权限码：代码中出现的 vs sys_permission 库中存在的 vs 角色授权
  4. 菜单树 / 角色授权对账
输出: tool-results/hr-db-audit.json 与 stdout 报告
用法: python tools/audit-hr-db.py
"""
import os
import re
import io
import json
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HR_SRC = os.path.join(ROOT, 'backend', 'hr', 'hr-base', 'src', 'main', 'java')
CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')


def q(cur, sql):
    cur.execute(sql)
    cols = [d[0] for d in cur.description]
    return [dict(zip(cols, r)) for r in cur.fetchall()]


def main():
    import psycopg2
    conn = psycopg2.connect(**CONN)
    cur = conn.cursor()
    out = {}

    # ---------- 1. hr_* 表 ----------
    cur.execute("""SELECT table_name FROM information_schema.tables
                   WHERE table_schema='public' AND table_name LIKE 'hr\_%' ESCAPE '\\'
                   ORDER BY table_name""")
    tables = [r[0] for r in cur.fetchall()]
    tinfo = []
    for t in tables:
        cur.execute("SELECT count(*) FROM information_schema.columns WHERE table_name=%s", (t,))
        ncol = cur.fetchone()[0]
        cur.execute("SELECT column_name FROM information_schema.columns WHERE table_name=%s", (t,))
        cols = {r[0] for r in cur.fetchall()}
        try:
            cur.execute('SELECT count(*) FROM "%s" WHERE deleted = 0' % t)
            nrow = cur.fetchone()[0]
        except Exception:
            conn.rollback()
            cur.execute('SELECT count(*) FROM "%s"' % t)
            nrow = cur.fetchone()[0]
        tinfo.append({'table': t, 'cols': ncol, 'rows_not_deleted': nrow,
                      'has_tenant': 'tenant_id' in cols, 'has_deleted': 'deleted' in cols})
    out['tables'] = tinfo

    print('=' * 100)
    print('① hr_* 表清单（%d 张）' % len(tinfo))
    print('=' * 100)
    for t in tinfo:
        print('  %-30s 列=%-3d 有效行=%-6d tenant_id=%-5s deleted=%s' % (
            t['table'], t['cols'], t['rows_not_deleted'], t['has_tenant'], t['has_deleted']))

    # ---------- 2. 实体 @TableName 对账 ----------
    ents = []
    for r, _, files in os.walk(HR_SRC):
        for f in files:
            if not f.endswith('.java'):
                continue
            src = open(os.path.join(r, f), encoding='utf-8', errors='replace').read()
            tm = re.search(r'@TableName\s*\(\s*(?:value\s*=\s*)?"([^"]+)"', src)
            if not tm:
                continue
            name = f[:-5]
            # 字段：private 声明 + @TableField(exist=false) 排除
            fields = []
            for m in re.finditer(r'(@TableField\s*\([^)]*\)\s*)?private\s+[\w<>\[\],\.]+\s+(\w+)\s*;', src):
                if m.group(1) and 'exist' in m.group(1) and 'false' in m.group(1):
                    continue
                fields.append(m.group(2))
            ents.append({'class': name, 'table': tm.group(1), 'fields': fields,
                         'file': os.path.relpath(os.path.join(r, f), ROOT).replace('\\', '/')})
    out['entities'] = ents

    print()
    print('=' * 100)
    print('② 实体 @TableName ↔ 实际表 对账（%d 个实体）' % len(ents))
    print('=' * 100)
    for e in sorted(ents, key=lambda x: x['table']):
        cur.execute("SELECT to_regclass(%s)", (e['table'],))
        exists = cur.fetchone()[0] is not None
        if not exists:
            print('  ✗ %-26s -> 表 %s 不存在  (%s)' % (e['class'], e['table'], e['file']))
            continue
        cur.execute("SELECT column_name FROM information_schema.columns WHERE table_name=%s", (e['table'],))
        dbcols = {r[0] for r in cur.fetchall()}
        # 驼峰转下划线
        def snake(n):
            return re.sub(r'(?<!^)(?=[A-Z])', '_', n).lower()
        missing = [x for x in e['fields'] if snake(x) not in dbcols and x not in dbcols]
        if missing:
            print('  ! %-26s -> %-28s 实体有字段但表无对应列: %s' % (e['class'], e['table'], ', '.join(missing)))
    print('  （无输出的行即为一致）')

    # ---------- 3. 权限码对账 ----------
    db_perms = {r['permission_code']: r for r in q(cur, """SELECT permission_code, permission_name, permission_type, status, deleted
                   FROM sys_permission ORDER BY permission_code""")}
    code_perms = {}
    for r, _, files in os.walk(HR_SRC):
        for f in files:
            if not f.endswith('.java'):
                continue
            src = open(os.path.join(r, f), encoding='utf-8', errors='replace').read()
            for m in re.finditer(r'@Requires?Permission\s*\(\s*(?:value\s*=\s*)?"([^"]+)"', src):
                code_perms.setdefault(m.group(1), []).append(f[:-5])

    hr_db = {k: v for k, v in db_perms.items() if k.startswith('hr:') or k.startswith('hr-') or k.startswith('md:staff')}
    missing_in_db = sorted(set(code_perms) - set(db_perms))
    print()
    print('=' * 100)
    print('③ 权限码对账')
    print('=' * 100)
    print('  代码中引用（HR 模块）: %d 个' % len(code_perms))
    print('  库中 hr*/md:staff* 权限码: %d 个' % len(hr_db))
    print('  ✗ 代码引用但库中不存在: %d 个' % len(missing_in_db))
    for c in missing_in_db:
        print('      %-34s used by %s' % (c, ','.join(sorted(set(code_perms[c])))))
    print()
    print('  --- 库中 HR 权限码明细 ---')
    for c in sorted(hr_db):
        r = hr_db[c]
        # 语义实测：sys_permission 是「0=启用」
        flag = '' if r['deleted'] == 0 and r['status'] == 0 else '   <<< 非启用/已删'
        print('    %-34s %-30s type=%-10s%s' % (c, r['permission_name'], r['permission_type'], flag))
    # 代码里完全没用到的库中 HR 权限码
    unused = sorted(set(hr_db) - set(code_perms))
    print()
    print('  ! 库中存在但代码未引用: %d 个' % len(unused))
    for c in unused:
        print('      %s' % c)
    out['perm_missing_in_db'] = missing_in_db
    out['perm_unused_in_code'] = unused

    # ---------- 4. 权限 -> 角色授权 ----------
    cur.execute("""SELECT r.role_code, count(*) FROM sys_role_permission rp
                   JOIN sys_role r ON r.id = rp.role_id
                   JOIN sys_permission p ON p.id = rp.permission_id
                   WHERE p.permission_code LIKE 'hr:%%' OR p.permission_code LIKE 'md:staff%%'
                   GROUP BY r.role_code ORDER BY 2 DESC""")
    grant = q(cur, """SELECT r.role_code, r.tenant_id, count(*) AS n FROM sys_role_permission rp
                   JOIN sys_role r ON r.id = rp.role_id
                   JOIN sys_permission p ON p.id = rp.permission_id
                   WHERE p.permission_code LIKE 'hr:%' OR p.permission_code LIKE 'md:staff%'
                   GROUP BY r.role_code, r.tenant_id ORDER BY 3 DESC""")
    print()
    print('=' * 100)
    print('④ HR 权限的角色授权情况')
    print('=' * 100)
    total = len(hr_db)
    for g in grant:
        print('  角色 %-24s tenant=%-4s 授了 %d/%d' % (g['role_code'], g['tenant_id'], g['n'], total))
    out['grants'] = grant
    out['hr_perm_total'] = total

    # ---------- 5. 用户角色 ----------
    users = q(cur, """SELECT u.username, u.is_super_admin, array_agg(r.role_code) AS roles
                      FROM sys_user u LEFT JOIN sys_user_role ur ON ur.user_id = u.id
                      LEFT JOIN sys_role r ON r.id = ur.role_id
                      WHERE u.deleted = 0 GROUP BY u.username, u.is_super_admin ORDER BY u.username""")
    print()
    print('=' * 100)
    print('⑤ 用户与角色')
    print('=' * 100)
    for u in users:
        print('  %-22s super=%-5s roles=%s' % (u['username'], u['is_super_admin'], u['roles']))

    os.makedirs(os.path.join(ROOT, 'tool-results'), exist_ok=True)
    with open(os.path.join(ROOT, 'tool-results', 'hr-db-audit.json'), 'w', encoding='utf-8') as f:
        json.dump(out, f, ensure_ascii=False, indent=1, default=str)
    cur.close()
    conn.close()
    print()
    print('输出: tool-results/hr-db-audit.json')


if __name__ == '__main__':
    main()
