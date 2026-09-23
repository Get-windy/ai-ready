# -*- coding: utf-8 -*-
"""
仓储模块菜单连通性校验（只读）：
  1. 模拟前端 dynamicRoutes.getComponent 的解析规则，判定每条仓储菜单的
     component / list_path 能否落到磁盘真实存在的 .vue
  2. 双入口配置自洽性：display_mode=1 必须有 list_path；有 list_path 却 display_mode=0
  3. client_type / visible / status 异常
  4. 菜单树
用法: python tools/audit-storage-menu.py
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

STORAGE_ROOTS = [60003]
STORAGE_GROUPS = [60301, 60302, 60303, 60304, 60305, 60311, 60312]
STORAGE_LEAF_IDS = [5001, 5002, 5003, 5008, 5009, 5010, 5011, 5013, 5014, 5015, 5016, 5017,
                    80010, 80011, 80012, 80013, 80014, 80015, 80016, 80018,
                    90201, 90202, 90203, 81005, 81012, 81013]


def load_component_map():
    src = open(ROUTES, encoding='utf-8').read()
    pairs = re.findall(r"'([^']+)'\s*:\s*\(\)\s*=>\s*import\(\s*'@/views/([^']+)'\s*\)", src)
    return {k: (v[:-4] if v.endswith('.vue') else v) for k, v in pairs}


def resolve(component_path, component_map):
    """返回 (命中方式, 文件相对 views 的路径) 或 (None, None)"""
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
    # 磁盘直查
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
                                  tag_label, display_mode, visible, status, client_type, sort, menu_code
                           FROM sys_menu WHERE deleted = 0 ORDER BY parent_id, sort, id""")
            cols = [d[0] for d in cur.description]
            all_menus = [dict(zip(cols, r)) for r in cur.fetchall()]

    by_id = {m['id']: m for m in all_menus}
    # 仓储子树
    sub = [m for m in all_menus if m['id'] in STORAGE_ROOTS + STORAGE_GROUPS + STORAGE_LEAF_IDS]
    sub_ids = {m['id'] for m in sub}
    # 补充：parent 在仓储树内但未被上面列表覆盖的
    extra = [m for m in all_menus if m['parent_id'] in sub_ids and m['id'] not in sub_ids]
    sub += extra

    print('=' * 110)
    print('仓储菜单树（%d 条）' % len(sub))
    print('=' * 110)
    for m in sorted(sub, key=lambda x: (str(x['parent_id']), x['sort'] or 0, x['id'])):
        pid = m['parent_id']
        pname = by_id[pid]['menu_name'] if pid in by_id else str(pid)
        print('[%s] id=%-7s %-14s type=%s dm=%s tag=%-4s vis=%s st=%s ct=%-13s' % (
            pname, m['id'], m['menu_name'], m['menu_type'], m['display_mode'],
            m['tag_label'] or '-', m['visible'], m['status'], m['client_type']))
        print('        path=%-30s component=%-42s list_path=%s' % (
            m['path'] or '-', m['component'] or '-', m['list_path'] or '-'))

    print()
    print('=' * 110)
    print('① component 无法解析到真实 .vue（点进去空白/兜底提示）')
    print('=' * 110)
    bad = 0
    for m in sub:
        if m['menu_type'] != 1 or not m['component']:
            continue
        how, f = resolve(m['component'], cmap)
        if not how:
            bad += 1
            print('  id=%-7s %-14s path=%-26s component=%s' % (m['id'], m['menu_name'], m['path'], m['component']))
    print('  小计: %d' % bad)

    print()
    print('=' * 110)
    print('② list_path 无法解析（双入口第二标签打不开）')
    print('=' * 110)
    bad = 0
    for m in sub:
        if not m['list_path']:
            continue
        how, f = resolve(m['list_path'], cmap)
        if not how:
            bad += 1
            print('  id=%-7s %-14s list_path=%s (dm=%s)' % (m['id'], m['menu_name'], m['list_path'], m['display_mode']))
    print('  小计: %d' % bad)

    print()
    print('=' * 110)
    print('③ 双入口配置自洽性')
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
        if dm == 1 and lp and m['path'] == lp:
            print('  ✗ id=%-7s %-14s path 与 list_path 相同(%s)' % (m['id'], m['menu_name'], lp))

    print()
    print('=' * 110)
    print('④ 可见性/状态/客户端 异常')
    print('=' * 110)
    for m in sub:
        if m['status'] != 1:
            print('  ! id=%-7s %-14s status=%s（不等于1会被菜单接口过滤）' % (m['id'], m['menu_name'], m['status']))
        if m['client_type'] not in ('tenant-admin', 'pc-admin'):
            print('  ! id=%-7s %-14s client_type=%s' % (m['id'], m['menu_name'], m['client_type']))
        if m['menu_type'] == 1 and m['visible'] == 0:
            print('  ! id=%-7s %-14s visible=0（路由注册但菜单不显示）' % (m['id'], m['menu_name']))


if __name__ == '__main__':
    main()
