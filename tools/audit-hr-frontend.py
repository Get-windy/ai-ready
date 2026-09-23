# -*- coding: utf-8 -*-
"""
人力资源模块前端盘点（只读）：
  1. HR 相关目录下的所有 .vue 页面
  2. 每个页面是否被 dynamicRoutes.ts 引用 / sys_menu 引用 => 孤儿候选
  3. 页面调用的 /hr/... 接口 vs 后端 HrController 实际端点 => 断链
  4. 桩代码扫描（TODO / 假数据 / mock）
输出: tool-results/hr-frontend-audit.json
用法: python tools/audit-hr-frontend.py
"""
import os
import re
import io
import json
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PC = os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src')
VIEWS = os.path.join(PC, 'views')
ROUTES = os.path.join(PC, 'router', 'dynamicRoutes.ts')
CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')

HR_DIRS = ['hr', 'md/staff-dept']


def collect_pages():
    pages = []
    for d in HR_DIRS:
        base = os.path.join(VIEWS, d.replace('/', os.sep))
        if not os.path.isdir(base):
            continue
        for r, _, files in os.walk(base):
            if '.atcode' in r:
                continue
            for f in files:
                if f.endswith('.vue'):
                    full = os.path.join(r, f)
                    rel = os.path.relpath(full, VIEWS).replace('\\', '/')[:-4]
                    pages.append({'rel': rel, 'lines': sum(1 for _ in open(full, encoding='utf-8', errors='replace'))})
    return sorted(pages, key=lambda x: x['rel'])


def parse_routes():
    src = open(ROUTES, encoding='utf-8').read()
    pairs = re.findall(r"'([^']+)'\s*:\s*\(\)\s*=>\s*import\(\s*'@/views/([^']+)'\s*\)", src)
    key2file = {k: (v[:-4] if v.endswith('.vue') else v) for k, v in pairs}
    all_imports = set()
    for m in re.findall(r"@/views/([^'\"]+)", src):
        all_imports.add(m[:-4] if m.endswith('.vue') else m)
    return key2file, all_imports


STUB_PAT = [
    (re.compile(r'//\s*(TODO|FIXME|XXX|待实现|待补)', re.I), 'TODO注释'),
    (re.compile(r'Math\.random\(\)'), 'Math.random'),
    (re.compile(r'(mock|Mock)(Data|data|List)'), 'mock数据'),
    (re.compile(r'暂未实现|暂不支持|敬请期待|开发中'), '文案占位'),
    (re.compile(r'console\.(log|warn|error)\('), 'console 调用'),
]


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
            cur.execute("""SELECT rm.menu_id, r.role_code FROM sys_role_menu rm
                           JOIN sys_role r ON r.id = rm.role_id
                           WHERE rm.menu_id IN (60014,61401,61402,61403,61404,61405,61406,
                                                90001,90002,90003,90004,90005,90006,80530,80531,80532,907)""")
            role_menus = cur.fetchall()

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
                        {'id': mid, 'name': name, 'path': path, 'mode': dm, 'client_type': ct, 'visible': vis})
                    break

    # 全源码文本（用于引用检测 + 接口提取）
    all_text = {}
    for r, dirs, files in os.walk(PC):
        dirs[:] = [d for d in dirs if d not in ('node_modules', 'dist', '.atcode')]
        for f in files:
            if f.endswith(('.vue', '.ts', '.tsx', '.js')):
                full = os.path.join(r, f)
                try:
                    all_text[full.replace('\\', '/')] = open(full, encoding='utf-8', errors='replace').read()
                except Exception:
                    pass

    out = []
    for p in pages:
        rel = p['rel']
        own = os.path.join(VIEWS, rel.replace('/', os.sep) + '.vue').replace('\\', '/')
        src = all_text.get(own, '')
        stubs = []
        for pat, tag in STUB_PAT:
            n = len(pat.findall(src))
            if n:
                stubs.append('%s×%d' % (tag, n))
        # 页面里出现的 /hr/... 或 '/hr' 字面量
        urls = sorted(set(re.findall(r"""['"`](/hr/[A-Za-z0-9_\-/{}$\.]*)['"`]""", src)))
        urls += sorted(set(re.findall(r"""['"`](/api/hr/[A-Za-z0-9_\-/{}$\.]*)['"`]""", src)))
        out.append({
            'rel': rel,
            'lines': p['lines'],
            'in_component_map_as_key': rel in key2file,
            'component_map_target_of': [k for k, v in key2file.items() if v == rel],
            'imported_in_routes_ts': rel in all_imports,
            'menus': menu_components.get(rel, []),
            'referenced_by': [k.replace(PC.replace('\\', '/') + '/', '') for k in all_text
                              if rel in all_text[k] and k != own][:12],
            'stubs': stubs,
            'urls': urls,
            # 僵尸键：componentMap 里有这个键，但全库没有任何菜单的 component/list_path
            # 会解析到它（getComponent 只会按菜单里的字面路径找键）
            'zombie_key': (rel in key2file) and (not menu_components.get(rel)),
            'orphan_candidate': (not (rel in all_imports)) and (not menu_components.get(rel))
                                and (not any(v == rel for v in key2file.values())),
        })

    os.makedirs(os.path.join(ROOT, 'tool-results'), exist_ok=True)
    with open(os.path.join(ROOT, 'tool-results', 'hr-frontend-audit.json'), 'w', encoding='utf-8') as f:
        json.dump({'pages': out, 'role_menus': role_menus}, f, ensure_ascii=False, indent=1)

    print('HR 前端页面总数: %d' % len(out))
    print('孤儿候选: %d' % sum(1 for x in out if x['orphan_candidate']))
    print('僵尸键（componentMap 有键但无菜单解析到）: %d' % sum(1 for x in out if x['zombie_key']))
    print()
    for x in out:
        print('  %-42s lines=%-5d imported=%-5s menus=%-12s zombie=%-5s refby=%s' % (
            x['rel'], x['lines'], x['imported_in_routes_ts'],
            ','.join(sorted({str(m['id']) for m in x['menus']})) or '-', x['zombie_key'],
            ','.join(x['referenced_by'][:3]) or '-'))
        if x['stubs']:
            print('        stubs: %s' % ', '.join(x['stubs']))
        if x['urls']:
            print('        urls : %s' % ', '.join(x['urls']))
    print()
    print('--- HR 菜单的 role_menu 授权 ---')
    for mid, rc in role_menus:
        print('  menu=%-7s role=%s' % (mid, rc))
    print('输出: tool-results/hr-frontend-audit.json')


if __name__ == '__main__':
    main()
