# -*- coding: utf-8 -*-
"""
仓储模块前端盘点（只读）：
  1. 仓储相关目录下的所有 .vue 页面
  2. 每个页面是否被 dynamicRoutes.ts 引用（componentMap 键/值、静态 import）
  3. sys_menu 中引用到该页面的菜单
  4. 判定：无路由引用且无菜单引用 => 孤儿页面候选
输出：tool-results/storage-frontend-audit.json
用法: python tools/audit-storage-frontend.py
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

# 仓储模块的前端目录（对照 24 篇开发文档覆盖的功能）
STORAGE_DIRS = [
    'erp/stock-in', 'erp/stock-out', 'erp/stock-transfer', 'erp/stock-damage',
    'erp/stock-overflow', 'erp/stocktake', 'erp/stock-cost-adjust',
    'erp/stock-assemble', 'erp/stock-split', 'erp/stock-bom',
    'erp/stock-alert-config', 'erp/alert-query', 'erp/stock',
    'erp/batch', 'erp/serial', 'erp/column-config', 'erp/inventory',
    'wh', 'wms', 'quality', 'erp/warehouse',
]


def collect_pages():
    pages = []
    for d in STORAGE_DIRS:
        base = os.path.join(VIEWS, d.replace('/', os.sep))
        if not os.path.isdir(base):
            continue
        for r, _, files in os.walk(base):
            for f in files:
                if f.endswith('.vue'):
                    full = os.path.join(r, f)
                    rel = os.path.relpath(full, VIEWS).replace('\\', '/')[:-4]
                    pages.append({'rel': rel, 'lines': sum(1 for _ in open(full, encoding='utf-8', errors='replace'))})
    return sorted(pages, key=lambda x: x['rel'])


def parse_routes():
    src = open(ROUTES, encoding='utf-8').read()
    # componentMap 键 -> 导入文件
    pairs = re.findall(r"'([^']+)'\s*:\s*\(\)\s*=>\s*import\(\s*'@/views/([^']+)'\s*\)", src)
    key2file = {k: v[:-4] if v.endswith('.vue') else v for k, v in pairs}
    # 所有 @/views/xxx 导入（含静态路由里的 import）
    all_imports = set()
    for m in re.findall(r"@/views/([^'\"]+)", src):
        all_imports.add(m[:-4] if m.endswith('.vue') else m)
    return key2file, all_imports


def main():
    import psycopg2
    pages = collect_pages()
    key2file, all_imports = parse_routes()

    with psycopg2.connect(**CONN) as conn:
        with conn.cursor() as cur:
            cur.execute("""SELECT id, menu_name, path, component, list_path, display_mode,
                                  client_type, visible, status, parent_id
                           FROM sys_menu WHERE deleted = 0""")
            menus = cur.fetchall()

    # 菜单引用的组件路径（component + list_path，均走 getComponent 解析）
    menu_components = {}
    for mid, name, path, comp, lp, dm, ct, vis, st, pid in menus:
        for c in (comp, lp):
            if not c:
                continue
            # 与 dynamicRoutes.getComponent 的解析规则保持一致
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
                        {'id': mid, 'name': name, 'path': path, 'mode': dm, 'client_type': ct, 'visible': vis})
                    break

    # 全量源码扫描：页面是否被其它文件（页面/组件/工具）以路径字符串引用
    src_root = os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src')
    all_text = {}
    for r, dirs, files in os.walk(src_root):
        dirs[:] = [d for d in dirs if d not in ('node_modules', 'dist')]
        for f in files:
            if not f.endswith(('.vue', '.ts', '.tsx', '.js')):
                continue
            full = os.path.join(r, f)
            try:
                all_text[full.replace('\\', '/')] = open(full, encoding='utf-8', errors='replace').read()
            except Exception:
                pass
    referenced_by = {}
    for rel in [p['rel'] for p in pages]:
        own = os.path.join(VIEWS, rel.replace('/', os.sep) + '.vue').replace('\\', '/')
        hits = [k for k, v in all_text.items() if rel in v and k != own]
        referenced_by[rel] = hits

    out = []
    for p in pages:
        rel = p['rel']
        direct_key = rel in key2file
        target = key2file.get(rel)
        imported = rel in all_imports
        menus_ref = menu_components.get(rel, [])
        out.append({
            'rel': rel,
            'lines': p['lines'],
            'in_component_map_as_key': direct_key,
            'component_map_target_of': [k for k, v in key2file.items() if v == rel],
            'imported_in_routes_ts': imported,
            'menus': menus_ref,
            'referenced_by': [h.replace(os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src').replace('\\', '/') + '/', '') for h in referenced_by[rel]][:12],
            'orphan_candidate': (not imported) and (not menus_ref) and (not any(v == rel for v in key2file.values())) and (not referenced_by[rel]),
        })

    os.makedirs(os.path.join(ROOT, 'tool-results'), exist_ok=True)
    dest = os.path.join(ROOT, 'tool-results', 'storage-frontend-audit.json')
    with open(dest, 'w', encoding='utf-8') as f:
        json.dump({'pages': out}, f, ensure_ascii=False, indent=1)

    print('仓储前端页面总数: %d' % len(out))
    print('孤儿候选（无路由引用且无菜单引用）: %d' % sum(1 for x in out if x['orphan_candidate']))
    for x in out:
        if x['orphan_candidate']:
            print('   ORPHAN %-45s lines=%d' % (x['rel'], x['lines']))
    print('---')
    for x in out:
        print('%-52s lines=%-5d imported=%-5s menus=%-14s refby=%s' % (
            x['rel'], x['lines'], x['imported_in_routes_ts'],
            ','.join(sorted({str(m['id']) for m in x['menus']})) or '-',
            ','.join(x['referenced_by'][:3]) or '-'))
    print('输出: %s' % dest)


if __name__ == '__main__':
    main()
