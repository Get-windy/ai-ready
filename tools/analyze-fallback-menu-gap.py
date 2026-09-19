# -*- coding: utf-8 -*-
"""
分析 getRequiredRoutes() 里那些「无参数的页面类路由」，判断它们到底是：
  A. 菜单表里已有等价菜单（只是路径写法不同）→ 硬编码属于纯冗余，可直接删
  B. 菜单表里确实没有 → 需要决定"补菜单"还是"删页面"

判定依据：该路由指向的 .vue 组件文件，是否被 sys_menu.component 引用过（含别名归一化）。

用法: python tools/analyze-fallback-menu-gap.py
"""
import os
import re
import sys
import io
import json

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')


def norm(p):
    p = (p or '').replace('\\', '/')
    p = p.replace('views/', '')
    if p.endswith('.vue'):
        p = p[:-4]
    return p


def variants(n):
    """组件的等价写法（xxx/index 与 xxx 互通）"""
    out = {n}
    if n.endswith('/index'):
        out.add(n[:-6])
    else:
        out.add(n + '/index')
    return out


def main():
    data = json.load(open(os.path.join(ROOT, 'tools', 'audit-orphan-pages.json'), encoding='utf-8'))
    req = data['required_routes']

    # 无参数的「页面类」路由
    pages = []
    for r in req:
        p = r['path']
        if ':' in p or p.endswith('/create') or p.endswith('/form') or '/form/' in p or '/detail' in p:
            continue
        pages.append(r)

    import psycopg2
    with psycopg2.connect(**CONN) as conn:
        with conn.cursor() as cur:
            cur.execute("""SELECT id, menu_name, path, component, client_type, parent_id, tenant_id
                           FROM sys_menu WHERE deleted = 0 AND component IS NOT NULL AND component <> ''""")
            rows = cur.fetchall()

    dbmap = {}
    for mid, name, path, comp, ctype, pid, tid in rows:
        dbmap.setdefault(norm(comp), []).append(
            dict(id=mid, name=name, path=path, client_type=ctype, parent_id=pid, tenant_id=tid))

    have, missing = [], []
    for r in pages:
        n = norm(r['component'])
        hit = []
        for v in variants(n):
            hit = dbmap.get(v, [])
            if hit:
                break
        if hit:
            have.append((r, hit))
        else:
            missing.append(r)

    print('无参数页面类路由共 %d 条' % len(pages))
    print()
    print('=== A 类：菜单表已有菜单引用同一组件（%d 条）→ 硬编码是冗余 ===' % len(have))
    for r, hit in have:
        h = hit[0]
        print('  路由 %-30s → 菜单 id=%-8s %-16s path=%s' % (r['path'], h['id'], h['name'], h['path']))
        if len(hit) > 1:
            others = ', '.join('%s(id=%s)' % (x['name'], x['id']) for x in hit[1:])
            print('      另有 %d 条菜单引用同一组件: %s' % (len(hit) - 1, others))
    print()
    print('=== B 类：菜单表确实没有（%d 条）→ 需决定补菜单还是删页面 ===' % len(missing))
    for r in missing:
        print('  路由 %-30s 组件 %-42s %s' % (r['path'], r['component'], r['title']))

    out = {
        'has_menu': [{'path': r['path'], 'component': r['component'], 'title': r['title'],
                      'menus': h} for r, h in have],
        'no_menu': missing,
    }
    dst = os.path.join(ROOT, 'tools', 'analyze-fallback-menu-gap.json')
    with open(dst, 'w', encoding='utf-8') as f:
        json.dump(out, f, ensure_ascii=False, indent=1)
    print()
    print('已写入 tools/analyze-fallback-menu-gap.json')


if __name__ == '__main__':
    main()
