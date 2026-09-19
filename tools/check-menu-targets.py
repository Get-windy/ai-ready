# -*- coding: utf-8 -*-
"""
菜单完整性检查（只读）：
  1. sys_menu.component 指向的 .vue 文件在磁盘上是否真的存在（坏菜单 = 点进去空白/404）
  2. sys_menu 里 menu_name 重复的菜单项（同一功能挂了两处，往往是架构变更残留）
  3. 同一 .vue 组件被多个菜单引用（重复挂载）

用法: python tools/check-menu-targets.py
"""
import os
import re
import sys
import io
import collections

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VIEWS = os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src', 'views')
CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')


def norm(p):
    p = (p or '').replace('\\', '/').replace('views/', '')
    if p.endswith('.vue'):
        p = p[:-4]
    return p


def main():
    import psycopg2
    with psycopg2.connect(**CONN) as conn:
        with conn.cursor() as cur:
            cur.execute("""SELECT id, menu_name, path, component, client_type, parent_id
                           FROM sys_menu WHERE deleted = 0 AND component IS NOT NULL AND component <> ''
                           ORDER BY id""")
            rows = cur.fetchall()

    # componentMap 的别名：菜单里的 component 可能指向一个已删除的文件，
    # 但 dynamicRoutes.ts 里配了别名把它重定向到现存的新实现
    # （先例：菜单 62204 的 `admin/monitor/log` → `system/log/index.vue`）。
    # 不把这一层算进来会误报"坏菜单"。
    aliases = set()
    routes_file = os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src', 'router', 'dynamicRoutes.ts')
    try:
        rsrc = open(routes_file, encoding='utf-8').read()
        for m in re.findall(r"'([^']+)'\s*:\s*\(\)\s*=>\s*import\(\s*'@/views/([^']+)'\s*\)", rsrc):
            aliases.add(m[0])          # 别名 key 本身即代表"这个 component 值是可解析的"
    except Exception:
        pass

    missing, by_name, by_comp = [], collections.defaultdict(list), collections.defaultdict(list)
    for mid, name, path, comp, ctype, pid in rows:
        n = norm(comp)
        cand = [n, n + '/index']
        exists = any(os.path.isfile(os.path.join(VIEWS, c + '.vue')) for c in cand)
        # 文件不存在，但 componentMap 里有别名兜底 → 不算坏菜单
        if not exists and (n in aliases or n + '/index' in aliases):
            exists = True
        if not exists:
            missing.append((mid, name, path, comp, ctype))
        by_name[name].append((mid, path, comp))
        by_comp[n].append((mid, name, path))

    print('=' * 100)
    print('① 菜单指向的组件文件不存在（%d 条）—— 点进去必然空白或 404' % len(missing))
    print('=' * 100)
    for mid, name, path, comp, ctype in missing:
        print('  菜单 id=%-8s %-18s path=%-28s component=%s' % (mid, name, path or '-', comp))

    dup = {k: v for k, v in by_name.items() if len(v) > 1}
    print()
    print('=' * 100)
    print('② menu_name 重复的菜单项（%d 组）—— 同一功能挂了两处' % len(dup))
    print('=' * 100)
    for name, items in sorted(dup.items()):
        print('  「%s」' % name)
        for mid, path, comp in items:
            print('      id=%-8s path=%-28s component=%s' % (mid, path or '-', comp))

    multi = {k: v for k, v in by_comp.items() if len(v) > 1}
    print()
    print('=' * 100)
    print('③ 同一组件被多个菜单引用（%d 个组件）' % len(multi))
    print('=' * 100)
    for comp, items in sorted(multi.items()):
        print('  %s' % comp)
        for mid, name, path in items:
            print('      id=%-8s %-18s path=%s' % (mid, name, path or '-'))


if __name__ == '__main__':
    main()
