# -*- coding: utf-8 -*-
"""
配送（DMS）模块菜单连通性校验（只读）：
  1. 模拟前端 dynamicRoutes.getComponent 的解析规则，判定每条配送菜单的
     component / list_path 能否落到磁盘真实存在的 .vue
  2. 双入口配置自洽性：display_mode=1 必须有 list_path + tag_label
  3. client_type / visible / status 异常
  4. 菜单树 + 菜单码/权限码命名体系对照
用法: python tools/audit-dms-menu.py
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

# 配送顶级 + 10 个分组
DMS_ROOT = 60005
DMS_GROUPS = [60401, 60501, 60502, 60402, 60403, 60504, 60505, 60506, 60507, 61506]
# 27 个叶子菜单（含跨域：70530 线路挂资料域、90107 API 监控挂配送域）
DMS_LEAF_IDS = [70150, 70155, 70156, 70160, 70161, 70162, 80700, 80730, 80740, 80750,
                80760, 80770, 80780, 80790, 80820, 80830, 80840, 80847, 80850, 80860,
                80870, 80880, 80890, 80900, 80910, 80920, 90107, 70530]


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
    sub = [m for m in all_menus if m['id'] in [DMS_ROOT] + DMS_GROUPS + DMS_LEAF_IDS]
    sub_ids = {m['id'] for m in sub}
    extra = [m for m in all_menus if m['parent_id'] in sub_ids and m['id'] not in sub_ids]
    sub += extra

    print('=' * 118)
    print('配送菜单树（%d 条 / 叶子 %d 条）' % (
        len(sub), len([m for m in sub if m['menu_type'] == 1])))
    print('=' * 118)
    for m in sorted(sub, key=lambda x: (str(x['parent_id']), x['sort'] or 0, x['id'])):
        pid = m['parent_id']
        pname = by_id[pid]['menu_name'] if pid in by_id else str(pid)
        print('[%s] id=%-7s %-14s type=%s dm=%s tag=%-4s vis=%s st=%s ct=%-13s code=%s' % (
            pname, m['id'], m['menu_name'], m['menu_type'], m['display_mode'],
            m['tag_label'] or '-', m['visible'], m['status'], m['client_type'], m['menu_code']))
        print('        path=%-32s component=%-44s list_path=%s' % (
            m['path'] or '-', m['component'] or '-', m['list_path'] or '-'))

    print()
    print('=' * 118)
    print('① component 无法解析到真实 .vue（点进去空白/兜底提示）')
    print('=' * 118)
    bad = 0
    for m in sub:
        if m['menu_type'] != 1 or not m['component']:
            continue
        how, f = resolve(m['component'], cmap)
        if not how:
            bad += 1
            print('  id=%-7s %-14s path=%-28s component=%s' % (m['id'], m['menu_name'], m['path'], m['component']))
    print('  小计: %d' % bad)

    print()
    print('=' * 118)
    print('② list_path 无法解析（双入口第二标签打不开）')
    print('=' * 118)
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
    print('=' * 118)
    print('③ 双入口配置自洽性')
    print('=' * 118)
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
    print('=' * 118)
    print('④ 可见性/状态/客户端 异常')
    print('=' * 118)
    for m in sub:
        if m['status'] != 1:
            print('  ! id=%-7s %-14s status=%s（不等于1会被菜单接口过滤）' % (m['id'], m['menu_name'], m['status']))
        if m['client_type'] not in ('tenant-admin', 'pc-admin'):
            print('  ! id=%-7s %-14s client_type=%s' % (m['id'], m['menu_name'], m['client_type']))
        if m['menu_type'] == 1 and m['visible'] == 0:
            print('  ! id=%-7s %-14s visible=0（路由注册但菜单不显示）' % (m['id'], m['menu_name']))

    print()
    print('=' * 118)
    print('⑤ 同目录 sort 并列（展示顺序不稳定）')
    print('=' * 118)
    by_parent = {}
    for m in sub:
        if m['menu_type'] == 1:
            by_parent.setdefault(m['parent_id'], []).append(m)
    for pid, items in by_parent.items():
        sorts = [x['sort'] for x in items]
        dup = {s for s in sorts if sorts.count(s) > 1}
        if dup:
            print('  parent=%s(%s) sort 并列 %s -> %s' % (
                pid, by_id[pid]['menu_name'] if pid in by_id else '?', sorted(dup),
                ', '.join('%s#%s' % (x['menu_name'], x['sort']) for x in items)))

    print()
    print('=' * 118)
    print('⑥ 菜单码 vs 权限码 命名体系对照（菜单码前缀统计）')
    print('=' * 118)
    pref = {}
    for m in sub:
        c = (m['menu_code'] or '').split(':')[0]
        pref[c] = pref.get(c, 0) + 1
    for k, v in sorted(pref.items(), key=lambda x: -x[1]):
        print('  %-24s %d' % (k or '(空)', v))

    print()
    print('=' * 118)
    print('⑦ 顶级 60005 子节点 sort 一览（分组排序）')
    print('=' * 118)
    for m in sorted([x for x in sub if x['parent_id'] == DMS_ROOT], key=lambda x: x['sort'] or 0):
        print('  sort=%-5s id=%-7s %s' % (m['sort'], m['id'], m['menu_name']))


if __name__ == '__main__':
    main()
