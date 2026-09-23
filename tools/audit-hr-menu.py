# -*- coding: utf-8 -*-
"""
人力资源模块菜单连通性校验（只读）：
  1. 模拟前端 dynamicRoutes.getComponent 的解析规则，判定每条 HR 菜单的
     component / list_path 能否落到磁盘真实存在的 .vue
  2. 双入口配置自洽性 / client_type / visible / status / menu_level 异常
  3. 菜单树打印
用法: python tools/audit-hr-menu.py
"""
import os
import re
import io
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VIEWS = os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src', 'views')
ROUTES = os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src', 'router', 'dynamicRoutes.ts')
CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')

HR_ROOT = 60014
HR_GROUPS = [61401, 61402, 61403, 61404, 61405, 61406]


def load_component_map():
    src = open(ROUTES, encoding='utf-8').read()
    pairs = re.findall(r"'([^']+)'\s*:\s*\(\)\s*=>\s*import\(\s*'@/views/([^']+)'\s*\)", src)
    return {k: (v[:-4] if v.endswith('.vue') else v) for k, v in pairs}


def resolve(component_path, component_map):
    if not component_path:
        return None, None
    p = component_path.replace('\\', '/')
    if p.startswith('views/'):
        p = p[len('views/'):]
    if p.endswith('.vue'):
        p = p[:-4]
    cands = [(p, 'exact'), (p + '/index', 'append-index')]
    if p.endswith('/index'):
        cands.append((p[:-len('/index')], 'strip-index'))
    for cand, how in cands:
        if cand in component_map:
            f = component_map[cand]
            if os.path.isfile(os.path.join(VIEWS, f + '.vue')):
                return how, f
    for cand in (p, p + '/index'):
        if os.path.isfile(os.path.join(VIEWS, cand + '.vue')):
            return 'disk', cand
        if os.path.isfile(os.path.join(VIEWS, cand, 'index.vue')):
            return 'disk-index', cand + '/index'
    return None, None


def main():
    import psycopg2
    cmap = load_component_map()
    with psycopg2.connect(**CONN) as conn:
        with conn.cursor() as cur:
            cur.execute("""SELECT id, parent_id, menu_name, menu_type, path, component, list_path,
                                  tag_label, display_mode, visible, status, client_type, sort,
                                  menu_code, menu_level
                           FROM sys_menu WHERE deleted = 0 ORDER BY parent_id, sort, id""")
            cols = [d[0] for d in cur.description]
            all_menus = [dict(zip(cols, r)) for r in cur.fetchall()]

    by_id = {m['id']: m for m in all_menus}
    sub = [m for m in all_menus if m['id'] in [HR_ROOT] + HR_GROUPS]
    sub_ids = {m['id'] for m in sub}
    sub += [m for m in all_menus if m['parent_id'] in sub_ids and m['id'] not in sub_ids]

    print('=' * 110)
    print('人力资源菜单树（%d 条）' % len(sub))
    print('=' * 110)
    for m in sorted(sub, key=lambda x: (str(x['parent_id']), x['sort'] or 0, x['id'])):
        pid = m['parent_id']
        pname = by_id[pid]['menu_name'] if pid in by_id else str(pid)
        print('[%s] id=%-7s %-14s type=%s lvl=%-4s dm=%s tag=%-4s vis=%s st=%s ct=%-13s code=%s' % (
            pname, m['id'], m['menu_name'], m['menu_type'], m['menu_level'], m['display_mode'],
            m['tag_label'] or '-', m['visible'], m['status'], m['client_type'], m['menu_code']))
        print('        path=%-30s component=%-42s list_path=%s' % (
            m['path'] or '-', m['component'] or '-', m['list_path'] or '-'))

    print()
    print('=' * 110)
    print('① component 无法解析到真实 .vue')
    print('=' * 110)
    bad = 0
    for m in sub:
        if m['menu_type'] != 1 or not m['component']:
            continue
        how, f = resolve(m['component'], cmap)
        if not how:
            bad += 1
            print('  id=%-7s %-14s path=%-26s component=%s' % (m['id'], m['menu_name'], m['path'], m['component']))
        else:
            print('  ok id=%-7s %-14s -> %s (%s)' % (m['id'], m['menu_name'], f, how))
    print('  小计: %d' % bad)

    print()
    print('=' * 110)
    print('② 双入口 / 可见性 / 客户端 / menu_level 异常')
    print('=' * 110)
    for m in sub:
        if m['menu_type'] != 1:
            continue
        dm, lp, tag = m['display_mode'], m['list_path'], m['tag_label']
        if dm == 1 and not lp:
            print('  ✗ id=%-7s %-14s display_mode=1 但 list_path 为空' % (m['id'], m['menu_name']))
        if dm == 1 and not tag:
            print('  ✗ id=%-7s %-14s display_mode=1 但 tag_label 为空' % (m['id'], m['menu_name']))
        if dm == 0 and lp:
            print('  ! id=%-7s %-14s display_mode=0 但残留 list_path=%s' % (m['id'], m['menu_name'], lp))
        if m['status'] != 1:
            print('  ! id=%-7s %-14s status=%s' % (m['id'], m['menu_name'], m['status']))
        if m['client_type'] not in ('tenant-admin', 'pc-admin'):
            print('  ! id=%-7s %-14s client_type=%s' % (m['id'], m['menu_name'], m['client_type']))
        if m['visible'] == 0:
            print('  ! id=%-7s %-14s visible=0' % (m['id'], m['menu_name']))
        if m['menu_level'] not in (0, None):
            print('  ! id=%-7s %-14s menu_level=%s（非0需确认是否被过滤）' % (m['id'], m['menu_name'], m['menu_level']))

    print()
    print('=' * 110)
    print('③ menu_code 命名一致性')
    print('=' * 110)
    for m in sub:
        print('  id=%-7s code=%-28s name=%s' % (m['id'], m['menu_code'], m['menu_name']))


if __name__ == '__main__':
    main()
