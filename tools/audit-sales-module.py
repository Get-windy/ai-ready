# -*- coding: utf-8 -*-
"""
销售模块（sales）专项审计脚本 —— 只读，不改任何文件。

审计三件事：
  ① 前端调用 → 后端是否存在（断链检查）：前端 `request.get/post/...` 里的路径，
     在 erp-sales 模块的端点集合里能不能找到匹配（支持路径参数 `{id}` 与模板串 `${x}`）。
  ② 后端端点 → 前端是否调用（冗余候选）：erp-sales 的端点在销售前端里有没有调用方。
     注意「未调用 ≠ 冗余」：导出下载（window.open）、定时任务、回调、被其它模块调用的不算。
  ③ 端点权限注解覆盖：哪些端点上没有 @SaCheckPermission / @PreAuthorize。

用法:
  python tools/audit-sales-module.py            # 全部三段
  python tools/audit-sales-module.py --links    # 只看断链
  python tools/audit-sales-module.py --unused   # 只看无人调用
  python tools/audit-sales-module.py --perms    # 只看权限覆盖
"""
import os
import re
import io
import sys
import json

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BACKEND_SALES = os.path.join(ROOT, 'backend', 'erp', 'erp-sales', 'src', 'main', 'java')
FRONTEND_SRC = os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src')

# 销售前端涉及的文件（页面目录 + API 定义文件）
FRONT_DIRS = [
    os.path.join(FRONTEND_SRC, 'views', 'sales'),
    os.path.join(FRONTEND_SRC, 'views', 'erp', 'sale'),
]
FRONT_FILES = [
    os.path.join(FRONTEND_SRC, 'api', 'erp.ts'),
    os.path.join(FRONTEND_SRC, 'api', 'order.ts'),
    os.path.join(FRONTEND_SRC, 'api', 'analytics-sales.ts'),
    os.path.join(FRONTEND_SRC, 'api', 'sales-analysis.ts'),
]

# 后端方法级注解 → HTTP 动词
# ⚠️ 必须区分「注解自带括号」与「无括号形式」：
#   @PostMapping("/x")        → 值在自身括号里
#   @PostMapping              → 无参，路径取类级前缀
#   早期版本用 `([^)]*)\)` 会跨过换行吃到**下面一行**的 @SaCheckPermission("xxx")，
#   把权限码当成路径（实测吐过 `/api/sale/promotion/sale:promotion:create` 这种假路径）。
VERB_RE = re.compile(
    r'@(Get|Post|Put|Delete|Patch)Mapping\s*(?:\(\s*(?:value\s*=\s*)?(["\'])([^"\']*)\2\s*\))?')
BASE_RE = re.compile(r'@RequestMapping\(\s*(?:value\s*=\s*)?["\']([^"\']+)["\']')
PERM_RE = re.compile(r'@(SaCheckPermission|PreAuthorize|SaCheckRole|SaCheckLogin|SaIgnore)\b')


def collect_backend_endpoints():
    """扫 erp-sales 模块，返回端点列表 [{verb, path, file, line, perms}]"""
    eps = []
    for dirpath, _dirnames, filenames in os.walk(BACKEND_SALES):
        for fn in filenames:
            if not fn.endswith('.java'):
                continue
            full = os.path.join(dirpath, fn)
            src = open(full, encoding='utf-8', errors='replace').read()
            base_m = BASE_RE.search(src)
            base = base_m.group(1) if base_m else ''
            rel = os.path.relpath(full, BACKEND_SALES).replace('\\', '/')
            for m in VERB_RE.finditer(src):
                verb = m.group(1).upper()
                sub = m.group(3) or ''
                path = (base.rstrip('/') + '/' + sub.lstrip('/')).rstrip('/')
                if not path:
                    path = base
                line = src[:m.start()].count('\n') + 1
                # 权限注解块：**必须向前 + 向后都看**。
                # 本仓两种写法都存在：
                #   @PostMapping                      ← 在映射前
                #   @SaCheckPermission("sale:x:create")
                # 与
                #   @GetMapping("/page")
                #   @SaCheckPermission("sale:price:edit")   ← 在映射后
                # 只看前 900 字符会把后者误判为「裸端点」（实测假阴性 3 条）。
                # 向后只取到本方法签名（`public/private/protected`）为止，避免吃到下一个方法。
                fwd = src[m.end():m.end() + 1200]
                cut = re.search(r'\n\s*(?:public|private|protected)\s', fwd)
                anns = PERM_RE.findall(src[max(0, m.start() - 900):m.start()]) + \
                    PERM_RE.findall(fwd[:cut.start()] if cut else fwd)
                eps.append({
                    'verb': verb, 'path': path, 'file': rel, 'line': line,
                    'anns': sorted(set(anns)),
                })
    return eps


def collect_frontend_calls():
    """扫销售前端文件，返回 [{path, verb, file, line}]，路径已去掉 /api 前缀"""
    calls = []
    files = []
    for d in FRONT_DIRS:
        for dirpath, dirnames, filenames in os.walk(d):
            dirnames[:] = [x for x in dirnames if x != '.atcode' and x != 'node_modules']
            for fn in filenames:
                if fn.endswith(('.vue', '.ts', '.js')):
                    files.append(os.path.join(dirpath, fn))
    for f in FRONT_FILES:
        if os.path.exists(f):
            files.append(f)

    call_re = re.compile(r'request\.(get|post|put|delete|patch)\s*[<(]')
    # 兼容 request({ url: '...', method: 'get' })
    obj_re = re.compile(r'url\s*:\s*[`\'"]([^`\'"]+)[`\'"]')
    str_re = re.compile(r'[`\'"](/[^`\'"]*)[`\'"]')
    for f in files:
        src = open(f, encoding='utf-8', errors='replace').read()
        rel = os.path.relpath(f, ROOT).replace('\\', '/')
        for m in call_re.finditer(src):
            verb = m.group(1).upper()
            seg = src[m.end():m.end() + 400]
            sm = str_re.search(seg)
            if not sm:
                continue
            path = sm.group(1)
            line = src[:m.start()].count('\n') + 1
            calls.append({'verb': verb, 'path': path, 'file': rel, 'line': line})
        for m in obj_re.finditer(src):
            line = src[:m.start()].count('\n') + 1
            calls.append({'verb': 'ANY', 'path': m.group(1), 'file': rel, 'line': line})
    return calls


def collect_frontend_path_literals():
    """
    扫**全部前端应用**的源码，抽出所有以 `/` 开头的字符串字面量，作为「有没有前端消费方」的
    判断池。

    为什么要全量而不是只看 `request.get(...)`：本仓大量路径是通过**配置对象**传递的，例如
    `views/sales/pre-order/form.vue: codeApiPath: '/erp/sale/pre-order/next-no'`
    —— 它由 `useBillForm` 内部发起请求，按 `request.get` 扫会误判成「无人调用」。
    """
    apps = os.path.join(ROOT, 'frontend', 'apps')
    lits = set()
    pat = re.compile(r'[`\'"](/[A-Za-z0-9_\-/{}.$:\[\]]{2,120})[`\'"]')
    for dirpath, dirnames, filenames in os.walk(apps):
        dirnames[:] = [d for d in dirnames if d not in ('node_modules', 'dist', '.atcode', 'test-results')]
        for fn in filenames:
            if not fn.endswith(('.vue', '.ts', '.js')):
                continue
            try:
                src = open(os.path.join(dirpath, fn), encoding='utf-8', errors='replace').read()
            except OSError:
                continue
            for m in pat.finditer(src):
                lits.add(m.group(1))
    return lits


def norm(p):
    """归一化路径：去掉 /api 前缀、模板变量、查询串、结尾斜杠"""
    p = p.split('?')[0]
    p = re.sub(r'^/api', '', p)
    p = re.sub(r'\$\{[^}]*\}', '{x}', p)
    p = re.sub(r'/\{x\}', '/{x}', p)
    return p.rstrip('/') or '/'


def to_regex(p):
    """把 /a/{id}/b 变成正则"""
    p = norm(p)
    parts = []
    for seg in p.split('/'):
        if seg.startswith('{') and seg.endswith('}'):
            parts.append(r'[^/]+')
        elif seg == '{x}':
            parts.append(r'[^/]+')
        else:
            parts.append(re.escape(seg))
    return re.compile('^' + '/'.join(parts) + '$')


def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else '--all'
    eps = collect_backend_endpoints()
    calls = collect_frontend_calls()
    ep_res = [(e, to_regex(e['path'])) for e in eps]

    # 只保留销售域的前端调用。
    # ⚠️ 用**前缀白名单**而不是「路径里含 return/sale」的关键词匹配：
    #   后者会把 `/erp/purchase/return/*`（采购退货）也捞进销售报告。
    sales_key = re.compile(r'^/(?:api/)?(?:erp/)?(?:sale(?:/|-out)|sales/|return/|sale/promotion)')
    scalls = [c for c in calls if sales_key.search(c['path'])]

    out = []
    if mode in ('--all', '--links'):
        broken = []
        for c in scalls:
            target = norm(c['path'])
            hit = any(r.match(target) for _e, r in ep_res)
            if not hit:
                broken.append(c)
        out.append(('① 前端调用后端不存在的路径（断链）', broken,
                    lambda c: f"  {c['verb']:5s} {c['path']:55s} {c['file']}:{c['line']}"))

    if mode in ('--all', '--unused'):
        used = set()
        for c in scalls:
            target = norm(c['path'])
            for e, r in ep_res:
                if r.match(target):
                    used.add((e['verb'], e['path']))
        # 判断池 = 销售目录里的 request 调用 + 全部前端源码里的路径字面量（配置对象传递的也算）
        lits = collect_frontend_path_literals()
        for e in eps:
            if (e['verb'], e['path']) in used:
                continue
            if any(to_regex(l).match(norm(e['path'])) for l in lits):
                used.add((e['verb'], e['path']))
        unused = [e for e in eps if (e['verb'], e['path']) not in used]
        out.append(('② 后端端点在整个前端里找不到调用方（冗余候选，需人工甄别导出/回调/定时任务）',
                    unused,
                    lambda e: f"  {e['verb']:5s} {e['path']:60s} {e['file']}:{e['line']}"))

    if mode in ('--all', '--perms'):
        naked = [e for e in eps if not e['anns']]
        out.append(('③ 无任何鉴权注解的端点', naked,
                    lambda e: f"  {e['verb']:5s} {e['path']:60s} {e['file']}:{e['line']}"))

    print(f"后端 erp-sales 端点 {len(eps)} 个；前端销售相关调用 {len(scalls)} 处\n")
    for title, items, fmt in out:
        print('=' * 100)
        print(f"{title}（{len(items)} 条）")
        print('=' * 100)
        for it in items:
            print(fmt(it))
        print()


if __name__ == '__main__':
    main()
