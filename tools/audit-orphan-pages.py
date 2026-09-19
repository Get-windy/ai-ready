# -*- coding: utf-8 -*-
"""
孤儿页面审计：交叉比对 sys_menu（数据库菜单） / dynamicRoutes.ts（前端路由映射） / views（实际页面文件）
输出中间数据 JSON，供后续人工分类。

用法: python tools/audit-orphan-pages.py
"""
import os
import re
import json
import sys
import io

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP = os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src')
ROUTES = os.path.join(APP, 'router', 'dynamicRoutes.ts')
VIEWS = os.path.join(APP, 'views')

CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')


def norm(p):
    """组件路径归一化：去掉 views/ 前缀与 .vue 后缀，统一分隔符"""
    p = (p or '').replace('\\', '/')
    p = p.replace('views/', '')
    if p.endswith('.vue'):
        p = p[:-4]
    return p


def strip_comments(text):
    """去掉 // 行注释，避免注释里的示例代码被误提取"""
    out = []
    for line in text.split('\n'):
        # 跳过纯注释行（保留 URL 中的 // 不受影响，因为要求行首为 //）
        if line.lstrip().startswith('//'):
            continue
        out.append(line)
    return '\n'.join(out)


def parse_component_map(src):
    """提取 componentMap: '路径key': () => import('@/views/xxx.vue')"""
    body = src
    start = body.find('const componentMap')
    end = body.find('\n}', start)
    body = strip_comments(body[start:end])
    pat = re.compile(r"'([^']+)'\s*:\s*\(\)\s*=>\s*import\(\s*'@/views/([^']+)'\s*\)")
    result = {}
    for m in pat.finditer(body):
        key, file = m.group(1), m.group(2)
        result[key] = file
    return result


def parse_required_routes(src):
    """提取 getRequiredRoutes() 里的 path + 组件文件"""
    start = src.find('function getRequiredRoutes')
    end = src.find('function getFallbackRoutes')
    body = strip_comments(src[start:end])
    # 逐个 route 对象块提取
    results = []
    pat = re.compile(
        r"path:\s*'([^']+)'\s*,\s*\n\s*name:\s*'([^']+)'\s*,\s*\n\s*component:\s*\(\)\s*=>\s*import\(\s*'@/views/([^']+)'\s*\)",
        re.M)
    for m in pat.finditer(body):
        results.append({'path': m.group(1), 'name': m.group(2), 'component': m.group(3)})
    # meta 里 title 单独捞一遍按顺序对齐
    titles = re.findall(r"title:\s*'([^']+)'", body)
    for i, r in enumerate(results):
        r['title'] = titles[i] if i < len(titles) else ''
    return results


def parse_fallback_routes(src):
    start = src.find('function getFallbackRoutes')
    end = src.find('function getRouteModule')
    body = strip_comments(src[start:end])
    results = []
    pat = re.compile(r"path:\s*'([^']+)'[\s\S]{0,200}?import\(\s*'@/views/([^']+)'\s*\)")
    for m in pat.finditer(body):
        results.append({'path': m.group(1), 'component': m.group(2)})
    return results


def scan_views():
    """扫描 views 下所有 .vue 文件，返回相对路径列表"""
    files = []
    for dirpath, dirnames, filenames in os.walk(VIEWS):
        dirnames[:] = [d for d in dirnames if d not in ('node_modules', '__tests__')]
        for fn in filenames:
            if fn.endswith('.vue'):
                rel = os.path.relpath(os.path.join(dirpath, fn), VIEWS).replace('\\', '/')
                files.append(rel)
    return sorted(files)


def parse_constant_routes():
    """router/index.ts 的 constantRoutes 里的组件（登录/注册/403/404/500 等）"""
    src = open(os.path.join(APP, 'router', 'index.ts'), encoding='utf-8').read()
    return set(re.findall(r"import\(\s*'@/views/([^']+)'\s*\)", src))


def scan_local_imports():
    """
    扫描 src 下所有文件对 .vue 的 import。
    这是判定「页面」与「子组件」的关键：components/ 下的 Modal/Drawer/Panel 之类
    不在 componentMap 也不在菜单表里，但会被父页面 import —— 它们不是孤儿页面。
    """
    refs = set()
    for dirpath, dirnames, filenames in os.walk(APP):
        dirnames[:] = [d for d in dirnames if d != 'node_modules']
        for fn in filenames:
            if not fn.endswith(('.vue', '.ts', '.js', '.tsx')):
                continue
            p = os.path.join(dirpath, fn)
            try:
                s = open(p, encoding='utf-8', errors='ignore').read()
            except Exception:
                continue
            # 别名形式：@/views/xxx/yyy(.vue)
            for m in re.findall(r"@/views/([^'\"\s)]+?)(?:\.vue)?['\"]", s):
                refs.add(norm(m))
            # 相对形式：./components/Xxx.vue  ../Xxx.vue
            for m in re.findall(r"['\"](\.[^'\"]*?\.vue)['\"]", s):
                abs_p = os.path.normpath(os.path.join(dirpath, m))
                refs.add(norm(os.path.relpath(abs_p, VIEWS)))
    return refs


def query_menu():
    import psycopg2
    with psycopg2.connect(**CONN) as conn:
        with conn.cursor() as cur:
            cur.execute("""
                SELECT id, tenant_id, parent_id, menu_name, menu_code, menu_type,
                       path, component, route_name, redirect, visible, status,
                       client_type, list_path, display_mode
                FROM sys_menu WHERE deleted = 0 ORDER BY client_type, parent_id, sort, id
            """)
            cols = [d[0] for d in cur.description]
            rows = [dict(zip(cols, [None if v is None else str(v) for v in r])) for r in cur.fetchall()]
    return rows


def main():
    src = open(ROUTES, encoding='utf-8').read()
    cmap = parse_component_map(src)
    required = parse_required_routes(src)
    fallback = parse_fallback_routes(src)
    views = scan_views()
    menus = query_menu()

    # 前端已知页面集合 = componentMap 的值 + requiredRoutes + constantRoutes
    mapped_files = set(cmap.values())
    required_files = set(r['component'] for r in required)
    constant_files = parse_constant_routes()
    # 被其它文件 import 的 .vue（子组件、弹窗、抽屉等）——它们不是页面，不该算孤儿
    local_refs = scan_local_imports()

    # DB 引用的组件
    db_components = set()
    for m in menus:
        c = m.get('component')
        if c:
            db_components.add(c.replace('\\', '/').replace('views/', '').replace('.vue', ''))

    # componentMap key 归一化后与 DB 对比（norm 为模块级函数）
    db_norm = set(norm(c) for c in db_components)

    mapped_norm = {}
    for k, v in cmap.items():
        mapped_norm.setdefault(norm(k), []).append(v)

    orphan_keys = []   # componentMap 有映射，但 DB 菜单未引用
    for k in sorted(mapped_norm):
        if k not in db_norm and (k + '/index') not in db_norm and \
           (k[:-6] if k.endswith('/index') else None) not in db_norm:
            orphan_keys.append({'key': k, 'files': sorted(set(mapped_norm[k]))})

    # views 下存在文件，但既不被任何路由/菜单引用，也不被其它文件 import
    all_refs = set(mapped_files) | db_norm | required_files | constant_files
    all_refs_norm = set(norm(x) for x in all_refs) | local_refs
    # 也登记去掉 /index 的形式
    all_refs_norm |= set(x[:-6] if x.endswith('/index') else x for x in list(all_refs_norm))

    untracked = []
    for v in views:
        vn = norm(v)
        if vn in all_refs_norm:
            continue
        # 变体匹配：xxx/index.vue 与 xxx 互通
        alt = vn[:-6] if vn.endswith('/index') else vn + '/index'
        if alt in all_refs_norm:
            continue
        untracked.append(v)

    # DB 引用但 componentMap 无映射（反向：DB 菜单配置了不存在的组件）
    cmap_keys_all = set(cmap.keys())
    cmap_keys_all |= set(k[:-6] if k.endswith('/index') else k + '/index' for k in list(cmap_keys_all))
    missing_in_frontend = sorted(c for c in db_norm if c not in cmap_keys_all)

    out = {
        'summary': {
            'componentMap_keys': len(cmap),
            'views_vue_files': len(views),
            'db_menu_rows': len(menus),
            'db_distinct_components': len(db_norm),
            'required_routes': len(required),
            'orphan_keys': len(orphan_keys),
            'untracked_view_files': len(untracked),
            'db_missing_in_frontend': len(missing_in_frontend),
        },
        'orphan_keys': orphan_keys,
        'untracked_view_files': untracked,
        'db_missing_in_frontend': missing_in_frontend,
        'required_routes': required,
        'fallback_routes': fallback,
        'component_map': cmap,
    }

    dst = os.path.join(ROOT, 'tools', 'audit-orphan-pages.json')
    with open(dst, 'w', encoding='utf-8') as f:
        json.dump(out, f, ensure_ascii=False, indent=1)

    s = out['summary']
    print('== 概览 ==')
    for k, v in s.items():
        print(f'  {k}: {v}')
    print('\n== 孤儿组件映射（前端有，DB菜单无）==')
    for o in orphan_keys:
        print(f"  {o['key']}  ->  {', '.join(o['files'])}")
    print('\n== 无任何引用的页面文件 ==')
    for u in untracked:
        print('  ' + u)
    print('\n== DB 菜单引用了但前端无映射 ==')
    for m in missing_in_frontend:
        print('  ' + m)
    print('\n结果已写入 tools/audit-orphan-pages.json')


if __name__ == '__main__':
    main()
