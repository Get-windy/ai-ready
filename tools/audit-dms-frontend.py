# -*- coding: utf-8 -*-
"""
配送（DMS）模块前端盘点（只读）：
  1. 配送域目录下所有 .vue 页面 + 行数
  2. 每个页面：dynamicRoutes 引用（componentMap 键/值、静态 import）
  3. sys_menu 中引用到该页面的菜单
  4. 判定：无路由引用且无菜单引用且无源码引用 => 孤儿页面候选
输出：tool-results/dms-frontend-audit.json
用法: python tools/audit-dms-frontend.py
"""
import os
import re
import io
import json
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src')
VIEWS = os.path.join(SRC, 'views')
ROUTES = os.path.join(SRC, 'router', 'dynamicRoutes.ts')
CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')

DMS_DIRS = [
    'dms', 'dispatch', 'trade/api-monitor', 'md/route',
    # 配送/配发收直达型入口会复用的目标视图（需一并盘点，判定是否有第二套实现）
    'order-center', 'sales/return-apply', 'sales/return-doc', 'erp/purchase',
]


def collect_pages():
    pages = []
    for d in DMS_DIRS:
        base = os.path.join(VIEWS, d.replace('/', os.sep))
        if not os.path.isdir(base):
            continue
        for r, _, files in os.walk(base):
            for f in files:
                if f.endswith('.vue'):
                    full = os.path.join(r, f)
                    rel = os.path.relpath(full, VIEWS).replace('\\', '/')[:-4]
                    pages.append({'rel': rel,
                                  'lines': sum(1 for _ in open(full, encoding='utf-8', errors='replace'))})
    return sorted(pages, key=lambda x: x['rel'])


def main():
    import psycopg2
    pages = collect_pages()
    src = open(ROUTES, encoding='utf-8').read()
    pairs = re.findall(r"'([^']+)'\s*:\s*\(\)\s*=>\s*import\(\s*'@/views/([^']+)'\s*\)", src)
    key2file = {k: (v[:-4] if v.endswith('.vue') else v) for k, v in pairs}
    all_imports = set()
    for m in re.findall(r"@/views/([^'\"]+)", src):
        all_imports.add(m[:-4] if m.endswith('.vue') else m)

    with psycopg2.connect(**CONN) as conn:
        with conn.cursor() as cur:
            cur.execute("""SELECT id, menu_name, path, component, list_path, display_mode,
                                  client_type, visible, status, parent_id
                           FROM sys_menu WHERE deleted = 0""")
            menus = cur.fetchall()

    menu_components = {}
    for mid, name, path, comp, lp, dm, ct, vis, st, pid in menus:
        for c in (comp, lp):
            if not c:
                continue
            p = c.replace('\\', '/')
            if p.startswith('views/'):
                p = p[len('views/'):]
            if p.endswith('.vue'):
                p = p[:-4]
            cands = [p, p + '/index']
            if p.endswith('/index'):
                cands.append(p[:-len('/index')])
            for cand in cands:
                if cand in key2file:
                    menu_components.setdefault(key2file[cand], []).append(
                        {'id': mid, 'name': name, 'path': path, 'dm': dm, 'vis': vis, 'st': st})
                    break

    all_text = {}
    for r, dirs, files in os.walk(SRC):
        dirs[:] = [d for d in dirs if d not in ('node_modules', 'dist')]
        for f in files:
            if not f.endswith(('.vue', '.ts', '.tsx', '.js')):
                continue
            full = os.path.join(r, f)
            try:
                all_text[full.replace('\\', '/')] = open(full, encoding='utf-8', errors='replace').read()
            except Exception:
                pass

    out = []
    for p in pages:
        rel = p['rel']
        own = os.path.join(VIEWS, rel.replace('/', os.sep) + '.vue').replace('\\', '/')
        base = rel.split('/')[-1]
        # 全路径引用 或 相对路径引用（./components/X.vue）—— 后者用 basename 兜底
        pat = re.compile(r"[./@][^'\"\s]*[/]" + re.escape(base) + r"(['\"]|\.vue)")
        hits = [k for k, v in all_text.items()
                if k != own and (rel in v or pat.search(v))]
        menus_ref = menu_components.get(rel, [])
        out.append({
            'rel': rel,
            'lines': p['lines'],
            'in_component_map_as_key': rel in key2file,
            'component_map_target_of': [k for k, v in key2file.items() if v == rel],
            'imported_in_routes_ts': rel in all_imports and rel in key2file,
            'menus': menus_ref,
            'referenced_by': [h.replace(SRC.replace('\\', '/') + '/', '') for h in hits][:12],
            'orphan_candidate': (rel not in key2file) and (not menus_ref)
                               and (not any(v == rel for v in key2file.values()))
                               and (not hits),
        })

    os.makedirs(os.path.join(ROOT, 'tool-results'), exist_ok=True)
    dest = os.path.join(ROOT, 'tool-results', 'dms-frontend-audit.json')
    with open(dest, 'w', encoding='utf-8') as f:
        json.dump({'pages': out}, f, ensure_ascii=False, indent=1)

    print('配送域前端页面总数: %d，总行数 %d' % (len(out), sum(x['lines'] for x in out)))
    print('孤儿候选（无路由键/无菜单/无源码引用）: %d' % sum(1 for x in out if x['orphan_candidate']))
    for x in out:
        if x['orphan_candidate']:
            print('   ORPHAN %-48s lines=%d' % (x['rel'], x['lines']))
    print()
    print('%-52s %-6s %-8s %-16s %s' % ('页面', '行数', '路由键', '菜单', '被引用'))
    for x in out:
        print('%-52s %-6d %-8s %-16s %s' % (
            x['rel'], x['lines'], 'Y' if x['in_component_map_as_key'] else '-',
            ','.join(sorted({str(m['id']) for m in x['menus']})) or '-',
            ','.join(x['referenced_by'][:3]) or '-'))
    print()
    print('输出: %s' % dest)


if __name__ == '__main__':
    main()
