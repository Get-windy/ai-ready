# -*- coding: utf-8 -*-
"""
资料模块（master-data）菜单连通性校验（只读）：
  1. 递归取 sys_menu 中「资料」(60011) 整棵子树
  2. 模拟前端 dynamicRoutes.getComponent 的解析规则，判定每条叶子的
     component / list_path 能否落到磁盘真实存在的 .vue
  3. 双入口配置自洽性 / client_type / visible / status / menu_level 异常
  4. menu_code 重复（前端路由名 = route_name || menu_code，重复且 path 不同会互相覆盖）
输出: tool-results/md-menu-audit.json
用法: python tools/audit-md-menu.py
"""
import os
import re
import io
import json
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VIEWS = os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src', 'views')
ROUTES = os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src', 'router', 'dynamicRoutes.ts')
CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')

MD_ROOT = 60011


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


def parse_route_names():
    """前端路由名 = route_name || menu_code，取 dynamicRoutes 里的 route_name 映射"""
    src = open(ROUTES, encoding='utf-8').read()
    return src


def main():
    import psycopg2
    cmap = load_component_map()
    with psycopg2.connect(**CONN) as conn:
        with conn.cursor() as cur:
            cur.execute("""SELECT id, parent_id, menu_name, menu_type, path, component, list_path,
                                  tag_label, display_mode, visible, status, client_type, sort,
                                  menu_code, menu_level, route_name
                           FROM sys_menu WHERE deleted = 0 ORDER BY parent_id, sort, id""")
            cols = [d[0] for d in cur.description]
            all_menus = [dict(zip(cols, r)) for r in cur.fetchall()]

    by_id = {m['id']: m for m in all_menus}
    children = {}
    for m in all_menus:
        children.setdefault(m['parent_id'], []).append(m)

    # 递归取 60011 子树
    sub, stack = [], [MD_ROOT]
    while stack:
        cur_id = stack.pop()
        for c in children.get(cur_id, []):
            sub.append(c)
            stack.append(c['id'])
    sub_ids = {m['id'] for m in sub}

    print('=' * 118)
    print('资料模块菜单树（根 60011，共 %d 条后代）' % len(sub))
    print('=' * 118)

    def dump(mid, depth):
        for m in sorted(children.get(mid, []), key=lambda x: (x['sort'] or 0, str(x['id']))):
            print('%s[%s] id=%-7s %-16s type=%s lvl=%-3s dm=%s tag=%-6s vis=%s st=%s ct=%-13s code=%s' % (
                '  ' * depth, by_id.get(mid, {}).get('menu_name', mid), m['id'], m['menu_name'],
                m['menu_type'], m['menu_level'], m['display_mode'], m['tag_label'] or '-',
                m['visible'], m['status'], m['client_type'], m['menu_code']))
            if m['menu_type'] == 1:
                print('%s     path=%-32s comp=%-44s list_path=%s' % (
                    '  ' * depth, m['path'] or '-', m['component'] or '-', m['list_path'] or '-'))
            dump(m['id'], depth + 1)

    dump(MD_ROOT, 0)

    leaves = [m for m in sub if m['menu_type'] == 1]
    print()
    print('=' * 118)
    print('① component 无法解析到真实 .vue（共 %d 个叶子）' % len(leaves))
    print('=' * 118)
    unresolved = []
    for m in leaves:
        if not m['component']:
            # 目录节点型叶子（component 为空但 path 指向已有页面）
            print('  ?  id=%-7s %-16s component 为空 path=%s' % (m['id'], m['menu_name'], m['path']))
            unresolved.append(m)
            continue
        how, f = resolve(m['component'], cmap)
        if not how:
            unresolved.append(m)
            print('  ✗  id=%-7s %-16s path=%-26s component=%s' % (
                m['id'], m['menu_name'], m['path'], m['component']))
        else:
            print('  ok id=%-7s %-16s -> %-46s (%s)' % (m['id'], m['menu_name'], f, how))
    print('  小计无法解析: %d' % len(unresolved))

    print()
    print('=' * 118)
    print('①b list_path（双入口第二路由）解析')
    print('=' * 118)
    for m in leaves:
        if m['display_mode'] != 1 or not m['list_path']:
            continue
        how, f = resolve(m['list_path'], cmap)
        tag = 'ok' if how else '✗ '
        print('  %s id=%-7s %-16s list_path=%-26s -> %s (%s)' % (
            tag, m['id'], m['menu_name'], m['list_path'], f or '【解析不到】', how or '-'))

    print()
    print('=' * 118)
    print('② 可见性 / 客户端 / menu_level / 双入口 异常')
    print('=' * 118)
    issues = []
    for m in sub:
        pid = m['parent_id']
        pname = by_id.get(pid, {}).get('menu_name', str(pid))
        if m['status'] != 1:
            issues.append(('status', m, 'status=%s' % m['status']))
        if m['client_type'] not in ('tenant-admin', 'pc-admin'):
            issues.append(('client_type', m, 'client_type=%s' % m['client_type']))
        if m['visible'] == 0:
            issues.append(('visible', m, 'visible=0'))
        if m['menu_level'] not in (0, None):
            issues.append(('menu_level', m, 'menu_level=%s' % m['menu_level']))
        if m['menu_type'] == 1:
            if m['display_mode'] == 1 and not m['list_path']:
                issues.append(('display_mode', m, 'display_mode=1 但 list_path 为空'))
            if m['display_mode'] == 1 and not m['tag_label']:
                issues.append(('display_mode', m, 'display_mode=1 但 tag_label 为空'))
            if m['display_mode'] == 0 and m['list_path']:
                issues.append(('display_mode', m, 'display_mode=0 但残留 list_path=%s' % m['list_path']))
    if not issues:
        print('  （无）')
    for kind, m, msg in issues:
        print('  ! [%s] id=%-7s %-16s parent=%-14s %s' % (kind, m['id'], m['menu_name'],
                                                          by_id.get(m['parent_id'], {}).get('menu_name', m['parent_id']), msg))

    print()
    print('=' * 118)
    print('③ menu_code 重复（全库范围）')
    print('=' * 118)
    dup = {}
    for m in all_menus:
        if m['menu_code']:
            dup.setdefault(m['menu_code'], []).append(m)
    for code, ms in sorted(dup.items()):
        if len(ms) > 1:
            inmd = any(m['id'] in sub_ids for m in ms)
            paths = {m['path'] for m in ms}
            flag = '⚠️ path 不同→后者覆盖前者' if len(paths) > 1 else '（path 相同，双入口无害）'
            print('  %s code=%-28s ids=%s  %s%s' % (
                '★' if inmd else ' ', code, [m['id'] for m in ms], flag,
                '  [含资料模块]' if inmd else ''))

    print()
    print('=' * 118)
    print('④ 双入口（同 path 多菜单 / 同 component 多菜单）')
    print('=' * 118)
    bycomp = {}
    for m in leaves:
        if m['component']:
            bycomp.setdefault(m['component'], []).append(m)
    for comp, ms in sorted(bycomp.items()):
        if len(ms) > 1:
            print('  component=%-46s ids=%s' % (comp, [m['id'] for m in ms]))
            for m in ms:
                print('      id=%-7s path=%-24s parent=%s(%s) code=%s' % (
                    m['id'], m['path'], m['parent_id'], by_id.get(m['parent_id'], {}).get('menu_name', '-'), m['menu_code']))

    os.makedirs(os.path.join(ROOT, 'tool-results'), exist_ok=True)
    with open(os.path.join(ROOT, 'tool-results', 'md-menu-audit.json'), 'w', encoding='utf-8') as f:
        json.dump({'subtree': sub, 'unresolved': [m['id'] for m in unresolved],
                   'issues': [{'kind': k, 'id': m['id'], 'name': m['menu_name'], 'msg': v} for k, m, v in issues]},
                  f, ensure_ascii=False, indent=1)
    print()
    print('输出: tool-results/md-menu-audit.json')


if __name__ == '__main__':
    main()
