# -*- coding: utf-8 -*-
"""
孤儿页面分类清单：把「前端存在但进不去」的页面按可达性分成三类，供逐页裁决。

  D1 完全死文件 —— 既不被任何路由/菜单引用，也不被其它文件 import
  D2 仅 URL 可达 —— 硬编码在 getRequiredRoutes() 里，菜单点不到（侧边栏只读后端菜单树）
  D3 组件映射孤儿 —— componentMap 里有映射，但既无菜单也无硬编码路由，且无人 import

排除项（这些不是"孤儿页面"）：
  - 详情/表单/编辑类（路径含 :id 或 /form、/detail、/edit、/create 结尾）—— 天然不该有菜单
  - 子组件（在 components/ 目录下，或被其它文件 import）
  - constantRoutes（登录/注册/403/404/500）

用法: python tools/classify-orphan-pages.py
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
CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')

DETAIL_PAT = re.compile(r'(:|/form|/detail|/edit|/create|/view$|Form$|Detail$)')
COMPONENT_DIR = re.compile(r'(^|/)components?/')


def norm(p):
    p = (p or '').replace('\\', '/').replace('views/', '')
    if p.endswith('.vue'):
        p = p[:-4]
    return p


def variants(n):
    out = {n}
    out.add(n[:-6] if n.endswith('/index') else n + '/index')
    return out


def looks_like_detail(key):
    """详情/表单类：天然不该有菜单项，不该按孤儿处理"""
    return bool(DETAIL_PAT.search(key))


def scan_router_targets():
    """
    扫描代码里 router.push / router.replace / <router-link to> 的目标路径。
    这些页面虽然不在菜单、也不在硬编码路由里，但用户能从别的页面点进去 —— 不能算死页面。
    """
    targets = set()
    pat = re.compile(r"""(?:router\.(?:push|replace)|to)\s*[=(]\s*[`'"]([^`'"?#]+)""")
    for dirpath, dirnames, filenames in os.walk(APP):
        dirnames[:] = [d for d in dirnames if d != 'node_modules']
        for fn in filenames:
            if not fn.endswith(('.vue', '.ts', '.js')):
                continue
            try:
                s = open(os.path.join(dirpath, fn), encoding='utf-8', errors='ignore').read()
            except Exception:
                continue
            for m in pat.findall(s):
                targets.add(m.strip().lstrip('/'))
    return targets


def main():
    data = json.load(open(os.path.join(ROOT, 'tools', 'audit-orphan-pages.json'), encoding='utf-8'))
    cmap = data['component_map']
    untracked = set(data['untracked_view_files'])
    required_paths = {r['component'] for r in data['required_routes']}

    push_targets = scan_router_targets()

    import psycopg2
    with psycopg2.connect(**CONN) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT component, list_path FROM sys_menu WHERE deleted = 0"
                        " AND ((component IS NOT NULL AND component <> '') OR list_path IS NOT NULL)")
            rows = cur.fetchall()
    db = set()
    for comp, lp in rows:
        if comp:
            db.add(norm(comp))
        if lp:
            db.add(norm(lp))

    db_all = set()
    for d in db:
        db_all |= variants(d)

    # requiredRoutes 覆盖的组件
    req_all = set()
    for r in required_paths:
        req_all |= variants(norm(r))

    def pushed(f):
        """该组件是否被 router.push / <router-link> 指向"""
        for v in variants(norm(f)):
            if v in push_targets or v + '/index' in push_targets:
                return True
            # push 目标常写成 /a/b（对应 a/b/index）
            if v.endswith('/index') and v[:-6] in push_targets:
                return True
        return False

    d1, d2, d3, d4 = [], [], [], []

    # D1：完全死文件（排除子组件目录）
    for f in sorted(untracked):
        if COMPONENT_DIR.search(f):
            continue
        if pushed(f):
            d4.append(('(仅 push 可达)', f))
        else:
            d1.append(f)

    # D2 / D3：遍历 componentMap
    for key, f in sorted(cmap.items()):
        if COMPONENT_DIR.search(f):
            continue
        if looks_like_detail(key) or looks_like_detail(f):
            continue
        v = variants(norm(f))
        in_db = bool(v & db_all)
        in_req = bool(v & req_all)
        if in_db:
            continue                      # 有菜单，正常
        if in_req:
            d2.append((key, f))
        elif pushed(f):
            d4.append((key, f))
        else:
            d3.append((key, f))

    # 去重（同一文件多个别名 key）
    def dedup(items):
        seen, out = set(), []
        for k, f in items:
            if f in seen:
                continue
            seen.add(f)
            out.append((k, f))
        return out

    d2 = dedup(d2)
    d3 = dedup(d3)
    d4 = dedup(d4)

    print('=' * 100)
    print('D1 完全死文件（%d 个）—— 无路由、无菜单、无人 import' % len(d1))
    print('=' * 100)
    for f in d1:
        print('  ' + f)

    print()
    print('=' * 100)
    print('D2 仅 URL 可达（%d 个）—— 硬编码在 requiredRoutes，侧边栏无入口' % len(d2))
    print('=' * 100)
    for k, f in d2:
        print('  %-38s %s' % (k, f))

    print()
    print('=' * 100)
    print('D4 仅 router.push 可达（%d 个）—— 无菜单/无硬编码路由，但从其它页面能点进去' % len(d4))
    print('=' * 100)
    for k, f in d4:
        print('  %-38s %s' % (k, f))

    print()
    print('=' * 100)
    print('D3 组件映射孤儿（%d 个）—— componentMap 有映射，但既无菜单也无硬编码路由' % len(d3))
    print('=' * 100)
    for k, f in d3:
        print('  %-38s %s' % (k, f))

    out = {
        'D1_dead_files': d1,
        'D2_url_only': [{'key': k, 'file': f} for k, f in d2],
        'D3_mapping_only': [{'key': k, 'file': f} for k, f in d3],
        'D4_push_only': [{'key': k, 'file': f} for k, f in d4],
    }
    dst = os.path.join(ROOT, 'tools', 'classify-orphan-pages.json')
    with open(dst, 'w', encoding='utf-8') as fh:
        json.dump(out, fh, ensure_ascii=False, indent=1)
    print()
    print('已写入 tools/classify-orphan-pages.json')


if __name__ == '__main__':
    main()
