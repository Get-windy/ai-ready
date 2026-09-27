# -*- coding: utf-8 -*-
"""
资料模块死代码审计（只读）：
  A. 前端：资料相关目录下每个 .vue 的引用情况（componentMap 键/值、sys_menu、其它源文件 import）
     三者皆无 → 孤儿页面候选；有 componentMap 键但无菜单解析到 → 僵尸键
  B. 后端：资料模块 controller 中「前端从未调用」的端点（复用 tools/audit-api-usage.json）
  C. 前端 api 封装：零消费方的方法（复用 tool-results/md-audit/api-usage.json）
输出: tool-results/md-audit/deadcode.json
用法: 先跑 audit-api-usage.py / audit-md-api-usage.py / audit-md-permission.py，再跑本脚本
"""
import os
import re
import io
import sys
import json

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PC = os.path.join(ROOT, 'frontend/apps/pc-admin/src')
VIEWS = os.path.join(PC, 'views')
ROUTES = os.path.join(PC, 'router/dynamicRoutes.ts')
OUT_DIR = os.path.join(ROOT, 'tool-results/md-audit')
CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')

# 资料模块相关的前端目录
DIRS = ['md', 'erp/product', 'erp/pricing/approval', 'erp/batch', 'erp/partner']


def collect_pages():
    pages = []
    for d in DIRS:
        base = os.path.join(VIEWS, d.replace('/', os.sep))
        if not os.path.isdir(base):
            continue
        for r, dirs, files in os.walk(base):
            dirs[:] = [x for x in dirs if x not in ('.atcode', 'node_modules')]
            for f in files:
                if f.endswith('.vue'):
                    full = os.path.join(r, f)
                    rel = os.path.relpath(full, VIEWS).replace('\\', '/')[:-4]
                    pages.append({'rel': rel, 'lines': sum(1 for _ in open(full, encoding='utf-8', errors='replace'))})
    return sorted(pages, key=lambda x: x['rel'])


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    src = open(ROUTES, encoding='utf-8').read()
    pairs = re.findall(r"'([^']+)'\s*:\s*\(\)\s*=>\s*import\(\s*'@/views/([^']+)'\s*\)", src)
    key2file = {k: (v[:-4] if v.endswith('.vue') else v) for k, v in pairs}
    all_imports = set()
    for m in re.findall(r"@/views/([^'\"]+)", src):
        all_imports.add(m[:-4] if m.endswith('.vue') else m)

    import psycopg2
    with psycopg2.connect(**CONN) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT id, menu_name, path, component, list_path, display_mode FROM sys_menu WHERE deleted=0")
            menus = cur.fetchall()
    menu_targets = {}   # 页面 rel -> [菜单]
    for mid, name, path, comp, lp, dm in menus:
        for c in (comp, lp):
            if not c:
                continue
            p = c.replace('\\', '/')
            if p.startswith('views/'):
                p = p[len('views/'):]
            if p.endswith('.vue'):
                p = p[:-4]
            for cand in (p, p + '/index', p[:-6] if p.endswith('/index') else p):
                if cand in key2file:
                    menu_targets.setdefault(key2file[cand], []).append(
                        {'id': mid, 'name': name, 'path': path, 'dm': dm})
                    break

    # 全源码文本（引用检测）
    texts = {}
    for r, dirs, files in os.walk(PC):
        dirs[:] = [d for d in dirs if d not in ('node_modules', 'dist')]
        for f in files:
            if f.endswith(('.vue', '.ts', '.tsx', '.js')):
                fp = os.path.join(r, f)
                texts[fp.replace('\\', '/')] = open(fp, encoding='utf-8', errors='replace').read()

    pages = collect_pages()
    out = []
    for p in pages:
        rel = p['rel']
        own = os.path.join(VIEWS, rel.replace('/', os.sep) + '.vue').replace('\\', '/')
        # 引用判定：用文件名（去掉目录与扩展名）匹配——源码里通常是相对路径 import
        # （`./components/Foo.vue`），完整 rel 匹配不到会导致孤儿误报。
        base = rel.split('/')[-1]
        refby = [k.replace(PC.replace('\\', '/') + '/', '')
                 for k, v in texts.items() if base in v and k != own]
        menus_here = menu_targets.get(rel, [])
        in_map_key = rel in key2file
        zombie = in_map_key and not menus_here
        orphan = (not menus_here) and (rel not in all_imports) \
                 and (not any(v == rel for v in key2file.values())) and not refby
        out.append({'rel': rel, 'lines': p['lines'], 'menus': menus_here,
                    'in_component_map': in_map_key, 'zombie_key': zombie,
                    'referenced_by': refby[:8], 'orphan': orphan})

    print('资料相关前端文件: %d 个' % len(out))
    print('孤儿页面候选: %d' % sum(1 for x in out if x['orphan']))
    print('僵尸键: %d' % sum(1 for x in out if x['zombie_key']))
    print()
    print('%-46s %6s %-14s %-8s %-8s %s' % ('页面', '行数', '菜单', '映射键', '孤儿', '被引用'))
    print('-' * 130)
    for x in out:
        print('%-46s %6d %-14s %-8s %-8s %s' % (
            x['rel'], x['lines'],
            ','.join(str(m['id']) for m in x['menus']) or '-',
            'Y' if x['in_component_map'] else '-',
            'Y' if x['orphan'] else '-',
            ','.join(x['referenced_by'][:2]) or '-'))

    # B. 后端未调用端点
    usage = json.load(open(os.path.join(ROOT, 'tools/audit-api-usage.json'), encoding='utf-8'))
    perm = json.load(open(os.path.join(OUT_DIR, 'permission-audit.json'), encoding='utf-8'))
    ctrl_files = {c['file'] for c in perm['relevant']}
    print()
    print('=' * 130)
    print('B. 资料模块 controller 中「前端从未调用」的端点')
    print('=' * 130)
    b = []
    for path, items in usage['unused_normal'].items():
        for method, f, line in items:
            if f in ctrl_files:
                b.append({'path': path, 'method': method, 'file': f, 'line': line})
    for x in sorted(b, key=lambda y: (y['file'], y['line'])):
        print('  %-6s %-58s %s:%s' % (x['method'], x['path'], os.path.basename(x['file']), x['line']))
    print('  小计: %d' % len(b))

    # C. 前端零消费方封装
    au = json.load(open(os.path.join(OUT_DIR, 'api-usage.json'), encoding='utf-8'))
    dead = [r for r in au['rows'] if not r['usage_all']]
    print()
    print('C. 前端 api 封装零消费方: %d 条（明细见 api-usage.json 的 C 段）' % len(dead))

    json.dump({'pages': out, 'backend_unused': b, 'frontend_dead_api': len(dead)},
              open(os.path.join(OUT_DIR, 'deadcode.json'), 'w', encoding='utf-8'),
              ensure_ascii=False, indent=1)
    print()
    print('输出: tool-results/md-audit/deadcode.json')


if __name__ == '__main__':
    main()
