# -*- coding: utf-8 -*-
"""
菜单 list_path 完整性检查（只读）。

补 `check-menu-targets.py` 的缺口：那个脚本只检查 `sys_menu.component` 字段，
而 `display_mode=1`（双入口）的菜单，用户点菜单进的是 **list_path** 那条链路，
component 只用于「新增/编辑」表单页。只查 component 会漏掉 list_path 悬空。

本脚本复刻 `dynamicRoutes.ts:930-955` 的 `getComponent()` 三级兜底：
    ① componentMap[path]
    ② componentMap[path + '/index']
    ③ path 以 '/index' 结尾时，去掉再查
再兜底磁盘文件 `views/<path>/index.vue` 与 `views/<path>.vue`。

用法:
  python tools/check-menu-list-paths.py
  python tools/check-menu-list-paths.py --json
"""
import os
import re
import sys
import io
import json

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP = os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src')
VIEWS = os.path.join(APP, 'views')
ROUTES = os.path.join(APP, 'router', 'dynamicRoutes.ts')
CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')


def load_component_map():
    src = open(ROUTES, encoding='utf-8').read()
    return {m.group(1): m.group(2) for m in re.finditer(
        r"'([^']+)'\s*:\s*\(\)\s*=>\s*import\(\s*'@/views/([^']+)'\s*\)", src)}


def norm(p):
    p = (p or '').replace('\\', '/').replace('views/', '')
    return p[:-4] if p.endswith('.vue') else p


def resolvable(path, cmap):
    """返回 (是否可解析, 命中的分支说明)"""
    n = norm(path)
    for cand, how in ((n, '①原样'), (n + '/index', '②追加 /index')):
        if cand in cmap:
            f = cmap[cand]
            if os.path.isfile(os.path.join(VIEWS, f)):
                return True, '%s componentMap[%s]' % (how, cand)
            return False, '%s componentMap[%s] → 文件缺失(%s)' % (how, cand, f)
    if n.endswith('/index'):
        base = n[:-len('/index')]
        if base in cmap:
            f = cmap[base]
            if os.path.isfile(os.path.join(VIEWS, f)):
                return True, '③去 /index componentMap[%s]' % base
            return False, '③去 /index componentMap[%s] → 文件缺失(%s)' % (base, f)
    for cand in (os.path.join(VIEWS, n, 'index.vue'), os.path.join(VIEWS, n + '.vue')):
        if os.path.isfile(cand):
            return True, '文件直查(%s)' % os.path.relpath(cand, VIEWS).replace('\\', '/')
    return False, '四级兜底全部未命中'


def main():
    import psycopg2
    cmap = load_component_map()
    with psycopg2.connect(**CONN) as conn:
        with conn.cursor() as cur:
            cur.execute("""SELECT id, menu_name, path, component, list_path, client_type
                           FROM sys_menu
                           WHERE deleted = 0 AND list_path IS NOT NULL AND list_path <> ''
                           ORDER BY client_type, id""")
            rows = cur.fetchall()

    bad, ok_count = [], 0
    for mid, name, path, comp, lp, ctype in rows:
        ok, how = resolvable(lp, cmap)
        if ok:
            ok_count += 1
        else:
            bad.append({'id': str(mid), 'name': name, 'list_path': lp,
                        'component': comp, 'client_type': ctype, 'how': how})

    if '--json' in sys.argv:
        print(json.dumps({'total': len(rows), 'ok': ok_count, 'bad': bad},
                         ensure_ascii=False, indent=1))
        return

    print('=' * 100)
    print('有 list_path 的菜单：%d 条，其中可解析 %d 条、悬空 %d 条' % (len(rows), ok_count, len(bad)))
    print('=' * 100)
    for b in bad:
        print('  菜单 id=%-8s %-16s list_path=%s' % (b['id'], b['name'], b['list_path']))
        print('       component=%s' % b['component'])
        print('       └ %s' % b['how'])
    if not bad:
        print('  全部可解析 —— list_path 无悬空。')


if __name__ == '__main__':
    main()
