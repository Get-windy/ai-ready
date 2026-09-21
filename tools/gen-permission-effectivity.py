# -*- coding: utf-8 -*-
"""
生成「权限生效性清单」到后端 resources，供「角色权限设置」矩阵标注哪些权限码勾了不生效。

背景（2026-09-20 盘点）：
  sys_permission 库里 474 条权限码，其中 229 条**没有任何消费方** ——
  勾进角色也不控制任何东西，界面上却是可勾选的，等于在说谎。
  消费方只有两类：
    ① 后端 @SaCheckPermission / @RequirePermission 注解（接口级）
    ② 前端 v-permission 指令 / checkPermission() 调用（按钮级）
  两者都没有 → 判为「未生效」。

注意口径：
  permission_type = 1 的 11 条 `xxx:manage`（采购管理/销售管理…）是**分组节点**，
  不是可授权的功能点，不参与生效性判定（否则会被误标 11 条）。

用法: python tools/gen-permission-effectivity.py
产出: backend/core/base/core-base/src/main/resources/permission-effectivity.json
"""
import os
import re
import sys
import io
import json
import collections

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BACKEND = os.path.join(ROOT, 'backend')
FRONTEND = os.path.join(ROOT, 'frontend', 'apps')
CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')
DST = os.path.join(BACKEND, 'core', 'base', 'core-base', 'src', 'main', 'resources',
                   'permission-effectivity.json')

# ── 后端：注解里的权限码 ───────────────────────────────────────────────
# ⚠️ 2026-09-21 修正：这里原来只认 `@RequirePermission`（**少一个 s**），
#    而本仓实际用的是 `@RequiresPermission`（core-base 的 `RequiresPermission` 注解 +
#    `PermissionAspect` 真实拦截；15 个控制器 / 92 个码在用）。
#    漏认的后果：这些**已被真实注解保护**的码被判成「僵尸码」，虚增了 E-02 的规模
#    —— hr 域那 35 个"僵尸码"就是这么来的（HrController 里有 77 处 @RequiresPermission）。
#    两种拼写都收，避免再因笔误造成同类误判。
BACKEND_ANNO = re.compile(r'@(?:SaCheckPermission|RequiresPermission|RequirePermission)\s*\(([^)]*)\)', re.S)
STR_LITERAL = re.compile(r'"([^"]+)"')

# ── 前端：v-permission 指令 + 权限判断函数调用 ─────────────────────────
#   v-permission="'a:b:c'" / v-permission.disabled="'a:b:c'" / v-permission="['a','b']"
FRONT_DIRECTIVE = re.compile(r'v-permission(?:\.\w+)?\s*=\s*"([^"]*)"')
FRONT_FUNC = re.compile(r'\b(?:checkPermission|hasPermission|canOperate|checkAnyPermission|hasAnyPermission)'
                        r'\s*\(\s*[\'"]((?:[a-zA-Z0-9_-]+:)+[a-zA-Z0-9_-]+)[\'"]')
# 形如 'a:b:c' 的权限码字面量（用于从指令表达式的数组/三元里提取）
PERM_LITERAL = re.compile(r'[\'"]([a-zA-Z0-9_-]+(?::[a-zA-Z0-9_-]+){1,3})[\'"]')


def scan_backend():
    """返回 {权限码: [位置, ...]}"""
    refs = collections.defaultdict(list)
    for dirpath, dirnames, filenames in os.walk(BACKEND):
        dirnames[:] = [d for d in dirnames if d != 'target']
        norm = dirpath.replace('\\', '/')
        if '/src/test' in norm:
            continue
        for fn in filenames:
            if not fn.endswith('.java'):
                continue
            p = os.path.join(dirpath, fn)
            try:
                src = open(p, encoding='utf-8').read()
            except Exception:
                continue
            for m in BACKEND_ANNO.finditer(src):
                for code in STR_LITERAL.findall(m.group(1)):
                    line = src[:m.start()].count('\n') + 1
                    rel = os.path.relpath(p, ROOT).replace('\\', '/')
                    refs[code].append('%s:%d' % (rel, line))
    return refs


def scan_frontend():
    """扫描 pc-admin 的 .vue/.ts，返回 {权限码: [位置, ...]}"""
    refs = collections.defaultdict(list)
    if not os.path.isdir(FRONTEND):
        return refs
    for dirpath, dirnames, filenames in os.walk(FRONTEND):
        dirnames[:] = [d for d in dirnames
                       if d not in ('node_modules', 'dist', 'test-results', 'playwright-report')]
        for fn in filenames:
            if not (fn.endswith('.vue') or fn.endswith('.ts')):
                continue
            p = os.path.join(dirpath, fn)
            try:
                src = open(p, encoding='utf-8').read()
            except Exception:
                continue
            rel = os.path.relpath(p, ROOT).replace('\\', '/')
            for i, line_txt in enumerate(src.split('\n'), 1):
                for m in FRONT_DIRECTIVE.finditer(line_txt):
                    for code in PERM_LITERAL.findall(m.group(1)):
                        refs[code].append('%s:%d' % (rel, i))
                for code in FRONT_FUNC.findall(line_txt):
                    refs[code].append('%s:%d' % (rel, i))
    # 同一位置可能被两条正则各命中一次，去重
    return {k: sorted(set(v)) for k, v in refs.items()}


def query_permissions():
    """返回 [(code, permission_type), ...]"""
    import psycopg2
    with psycopg2.connect(**CONN) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT permission_code, permission_type FROM sys_permission "
                        "WHERE deleted = 0 AND permission_code IS NOT NULL")
            return [(r[0], r[1]) for r in cur.fetchall()]


def main():
    backend_refs = scan_backend()
    frontend_refs = scan_frontend()
    rows = query_permissions()

    ineffective, group_nodes, effective = [], [], []
    for code, ptype in sorted(set(rows)):
        if ptype == 1:
            # 分组节点（采购管理/销售管理…）：不是可授权功能点，不判定生效性
            group_nodes.append(code)
            continue
        b = len(backend_refs.get(code, []))
        f = len(frontend_refs.get(code, []))
        if b or f:
            effective.append(code)
            continue
        ineffective.append(code)

    out = {
        'generatedAt': __import__('datetime').date.today().isoformat(),
        'summary': {
            'db': len(rows),
            'effective': len(effective),
            'ineffective': len(ineffective),
            'groupNodes': len(group_nodes),
        },
        # 前端矩阵据此标灰：勾了也不会被任何代码检查
        'ineffective': ineffective,
        'groupNodes': group_nodes,
        # 每个生效权限码的引用条数，供 tooltip 显示「被 N 处接口 / M 处按钮使用」
        'refCounts': {
            c: {'backend': len(backend_refs.get(c, [])), 'frontend': len(frontend_refs.get(c, []))}
            for c in effective
        },
    }

    os.makedirs(os.path.dirname(DST), exist_ok=True)
    with open(DST, 'w', encoding='utf-8') as fp:
        json.dump(out, fp, ensure_ascii=False, indent=1)

    s = out['summary']
    print('库中权限码:     %d' % s['db'])
    print('生效（有消费方）: %d  (后端注解 %d 处 / 前端指令 %d 处)'
          % (s['effective'], sum(len(v) for v in backend_refs.values()),
             sum(len(v) for v in frontend_refs.values())))
    print('未生效:         %d  ← 前端矩阵将标灰' % s['ineffective'])
    print('分组节点(排除):  %d' % s['groupNodes'])
    print()
    print('已写入 %s' % os.path.relpath(DST, ROOT).replace('\\', '/'))


if __name__ == '__main__':
    main()
