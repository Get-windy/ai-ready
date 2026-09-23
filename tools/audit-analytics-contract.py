#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
分析模块 前后端接口契约比对（2026-09-23 分析审计用）

解决的问题：分析模块页面调用的路径，后端**到底存不存在**？

与 tools/audit-finance-contract.py 的差别：分析模块的取数接口**分散在 6 个后端模块**，
前端的 api 封装也分散（analytics*.ts + 各模块 api/*.ts）。所以这里
  ① 从 views/analytics/** 的 import 语句**自动发现**用到的 api 文件（而不是写死清单）
  ② 再扫这些 api 文件里的 request.get/post/... 调用
  ③ 另外把 views/analytics 里直接出现的 URL 字面量也扫进来
后端侧仍扫**整个 backend/**（否则会把调采购/销售/财务模块的接口全误判成断链）。

输出：断链明细 / 方法不匹配 / 双 api 前缀 → tool-results/analytics-audit/contract-compare.md
"""
import os
import re
import json
import collections

ROOT = 'I:/AI-Ready'
FE_SRC = os.path.join(ROOT, 'frontend/apps/pc-admin/src')
FE_API_DIR = os.path.join(FE_SRC, 'api')
VIEWS_ANALYTICS = os.path.join(FE_SRC, 'views/analytics')
BE_ROOT = os.path.join(ROOT, 'backend')


def norm(p):
    p = p.split('?')[0].strip()
    p = re.sub(r'^/api(?=/|$)', '', p)
    p = re.sub(r'\$\{[^}]*\}', '{}', p)
    p = re.sub(r'\{[^}]*\}', '{}', p)
    p = re.sub(r'/+', '/', p)
    if not p.startswith('/'):
        p = '/' + p
    return p.rstrip('/').lower() or '/'


# ── ① 自动发现 views/analytics 用到的 api 文件 ──
IMPORT_API = re.compile(r"""from\s+['"]@/api/([^'"]+)['"]""")
# ⚠️ 必须同时覆盖 `request.get(...)` 与简写 `get(...)`：本仓多数 api 文件用的是
#    文件顶部 `import { get, post } from '@/utils/request'` 的简写形式，
#    只认 `request.` 会漏掉一大半调用（第一版就是这么漏的：调用点从 589 掉到 120）。
REQ_CALL = re.compile(r"""(?:\brequest\.|\bhttp\.|(?<![\w.]))(get|post|put|delete|patch|del|remove)\s*(?:<[^>]*>)?\s*\(\s*[`'"]([/$][^`'"]*)[`'"]""", re.I)
# 直接写在 .vue 里的绝对路径字符串（/erp/... /docquery/... 等）
RAW_URL = re.compile(r"""[`'"](/?(?:api/)?(?:erp|docquery|finance|inventory|purchase|sale|stock|mall|marketing|report)/[A-Za-z0-9_\-/{}$.]*)[`'"]""")


def discover_api_files():
    keep = set()
    if not os.path.isdir(VIEWS_ANALYTICS):
        return []
    for dirpath, _, files in os.walk(VIEWS_ANALYTICS):
        for f in files:
            if not f.endswith(('.vue', '.ts')):
                continue
            src = open(os.path.join(dirpath, f), encoding='utf-8', errors='ignore').read()
            for m in IMPORT_API.finditer(src):
                keep.add(m.group(1))
    out = []
    for rel in sorted(keep):
        for ext in ('', '.ts', '/index.ts'):
            cand = os.path.join(FE_API_DIR, rel + ext)
            if os.path.isfile(cand):
                out.append(os.path.relpath(cand, FE_API_DIR).replace('\\', '/'))
                break
    return out


# ── ② 后端端点 ──
METHOD_ANN = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping\s*(?:\(\s*(?:value\s*=\s*)?(?:["\']([^"\']*)["\'])?|$)')
CLASS_MAP = re.compile(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']*)["\']')


def scan_backend():
    eps = {}
    for dirpath, _, files in os.walk(BE_ROOT):
        if 'target' in dirpath:
            continue
        for f in files:
            if not f.endswith('.java'):
                continue
            fp = os.path.join(dirpath, f)
            src = open(fp, encoding='utf-8', errors='ignore').read()
            if '@RestController' not in src and '@Controller' not in src:
                continue
            lines = src.split('\n')
            class_prefix = ''
            for ln in lines:
                m = CLASS_MAP.search(ln)
                if m and '@RequestMapping' in ln:
                    class_prefix = m.group(1)
                    break
            for i, ln in enumerate(lines):
                m = METHOD_ANN.search(ln)
                if not m:
                    continue
                http = m.group(1).upper()
                sub = m.group(2) or ''
                full = (class_prefix.rstrip('/') + '/' + sub.lstrip('/')) if class_prefix else sub
                eps.setdefault(norm(full), []).append({
                    'method': http,
                    'where': f'{os.path.relpath(fp, ROOT).replace(chr(92), "/")}:{i+1}',
                    'raw': full,
                })
    return eps


EXPORT_OBJ = re.compile(r'^export\s+const\s+(\w+)\s*[:=]')


def used_objects():
    """views/analytics 里 import 的 (api 相对路径, 对象名) 集合。

    必要性：一个 api 文件往往服务很多模块（如 erp/partner.ts 同时给资料模块和分析模块用）。
    若把整文件的调用都算进分析模块，会把资料模块的断链误报成本模块的问题。
    """
    used = collections.defaultdict(set)
    pat = re.compile(r"""import\s*\{([^}]*)\}\s*from\s*['"]@/api/([^'"]+)['"]""")
    for dirpath, _, files in os.walk(VIEWS_ANALYTICS):
        for f in files:
            if not f.endswith(('.vue', '.ts')):
                continue
            src = open(os.path.join(dirpath, f), encoding='utf-8', errors='ignore').read()
            for m in pat.finditer(src):
                names = [n.strip() for n in m.group(1).split(',') if n.strip()]
                for n in names:
                    used[m.group(2)].add(n.split(' as ')[-1].strip())
    return used


def scan_frontend(api_files, used):
    calls = []
    for rel in api_files:
        fp = os.path.join(FE_API_DIR, rel)
        if not os.path.exists(fp):
            continue
        lines = open(fp, encoding='utf-8', errors='ignore').read().split('\n')
        # 建立「本文件被分析页面引用的」对象 → 行区间
        want = set()
        for key, names in used.items():
            k = key.rstrip('/')
            r = rel[:-3] if rel.endswith('.ts') else rel
            if k == r or (r.endswith('/index') and k == r[:-6]):
                want |= names
        ranges = []
        start, name = None, None
        for i, ln in enumerate(lines):
            m = EXPORT_OBJ.match(ln)
            if m:
                if start is not None:
                    ranges.append((name, start, i - 1))
                name, start = m.group(1), i
            elif start is not None and ln.startswith('}'):
                ranges.append((name, start, i))
                start, name = None, None
        if start is not None:
            ranges.append((name, start, len(lines) - 1))
        # 文件里没有任何 export const 对象（纯函数导出）→ 全收，避免漏报
        cover = [(s, e) for (n, s, e) in ranges if n in want] if ranges else [(0, len(lines) - 1)]
        filtered = bool(ranges) and bool(want)

        for i, ln in enumerate(lines):
            if filtered and not any(s <= i <= e for s, e in cover):
                continue
            for m in REQ_CALL.finditer(ln):
                raw = m.group(2)
                tail = ln[m.end():].lstrip()
                if tail.startswith('+'):
                    raw += '{}'
                calls.append({'file': 'api/' + rel, 'line': i + 1, 'http': m.group(1).upper(),
                              'raw': raw, 'norm': norm(raw), 'double_api': raw.startswith('/api/')})
    # views/analytics 里直接写的 URL 字面量（排除注释里的说明性路径）
    for dirpath, _, files in os.walk(VIEWS_ANALYTICS):
        for f in files:
            if not f.endswith(('.vue', '.ts')):
                continue
            fp = os.path.join(dirpath, f)
            rel = os.path.relpath(fp, FE_SRC).replace('\\', '/')
            for i, ln in enumerate(open(fp, encoding='utf-8', errors='ignore').read().split('\n')):
                s = ln.strip()
                if s.startswith(('*', '//', '<!--')):
                    continue
                for m in RAW_URL.finditer(ln):
                    raw = m.group(1)
                    if 'request.' in ln and m.start() > ln.find('request.'):
                        continue
                    calls.append({'file': rel, 'line': i + 1, 'http': 'GET',
                                  'raw': raw, 'norm': norm(raw + ('/page' if raw.endswith('/page') else '')),
                                  'double_api': raw.startswith('/api/'), 'from_view': True})
    return calls


def main():
    api_files = discover_api_files()
    used = used_objects()
    eps = scan_backend()
    calls = scan_frontend(api_files, used)

    # 归一化去重（同一路径多次出现只报一次，保留首个位置）
    uniq = {}
    for c in calls:
        uniq.setdefault((c['norm'], c['http']), c)
    calls = list(uniq.values())

    # ⚠️ 路径拼接了变量基址（如 docActionApi 的 `${base}/${id}/submit`，base 由调用方给）
    #    静态无法判定真实路径 ⇒ 单列「动态路径」，不混进断链，避免假断链污染结论
    def is_dynamic(c):
        return c['norm'].startswith('/{}') or bool(re.search(r'\$\{base|\$\{prefix|\$\{url|\$\{root', c['raw'], re.I))

    # ⚠️ 从 .vue 里直接抠出的路径字面量**多数是前端路由**（如 `/finance/payment-doc/form`
    #    是跳转目标，不是接口）。这类只单列供人工确认，不计入断链统计，否则全是假断链。
    view_paths = [c for c in calls if c.get('from_view')]
    calls = [c for c in calls if not c.get('from_view')]
    dynamic = [c for c in calls if is_dynamic(c)]
    static_calls = [c for c in calls if not is_dynamic(c)]
    broken = [c for c in static_calls if c['norm'] not in eps]
    method_mismatch = [c for c in static_calls if c['norm'] in eps and c['http'] not in {e['method'] for e in eps[c['norm']]}]
    double = [c for c in calls if c.get('double_api')]

    lines = ['# 分析模块 · 前后端接口契约比对', '',
             f'- 自动发现的分析模块前端 api 文件（{len(api_files)} 个）：{", ".join(api_files)}', '',
             f'- 后端全仓端点（归一路径去重）**{len(eps)}** 个',
             f'- 前端调用点（去重后）**{len(calls)}** 处（其中动态拼接路径 {len(dynamic)} 处不参与判定）',
             f'- **断链（前端调用、后端无此路径）：{len(broken)} 处**',
             f'- 方法不匹配：{len(method_mismatch)} 处',
             f'- 双 `/api` 前缀：{len(double)} 处', '']

    lines += ['## 〇、动态拼接路径（静态不可判定，需人工确认）', '']
    lines += ([f'- `{c["file"]}:{c["line"]}` `{c["http"]} {c["raw"]}`' for c in dynamic] or ['（无）'])
    lines += ['']
    lines += ['## 〇-2、页面内直接出现的路径字面量（**多为前端路由跳转目标**，非接口，需人工区分）', '']
    lines += ([f'- `{c["file"]}:{c["line"]}` `{c["raw"]}`' for c in view_paths] or ['（无）'])
    lines += ['']

    lines += ['## 一、断链明细（前端有、后端无）', '']
    if broken:
        lines += ['| 前端位置 | 方法 | 路径 |', '|---|---|---|']
        for c in sorted(broken, key=lambda x: x['norm']):
            lines.append(f'| `{c["file"]}:{c["line"]}` | {c["http"]} | `{c["raw"]}` |')
    else:
        lines.append('（无）')

    lines += ['', '## 二、方法不匹配', '']
    if method_mismatch:
        lines += ['| 前端位置 | 前端方法 | 路径 | 后端可用方法 |', '|---|---|---|---|']
        for c in method_mismatch:
            ms = ', '.join(sorted({e['method'] for e in eps[c['norm']]}))
            lines.append(f'| `{c["file"]}:{c["line"]}` | {c["http"]} | `{c["raw"]}` | {ms} |')
    else:
        lines.append('（无）')

    lines += ['', '## 三、双 `/api` 前缀', '']
    lines += ([f'- `{c["file"]}:{c["line"]}` → `{c["raw"]}`' for c in double] or ['（无）'])

    lines += ['', '## 四、分析模块专属端点冗余候选（后端有、本次前端扫描未引用）', '']
    called = {c['norm'] for c in calls}
    analytics_eps = {k: v for k, v in eps.items()
                     if re.search(r'/(analytics|analysis|docquery|statistics)', k)}
    unused = sorted(k for k in analytics_eps if k not in called)
    lines.append(f'后端含 analytics/analysis/docquery/statistics 的路径共 **{len(analytics_eps)}** 个，其中未被本次前端扫描引用 **{len(unused)}** 个：')
    lines.append('')
    lines += ['| 路径 | 方法 | 位置 |', '|---|---|---|']
    for k in unused:
        for e in analytics_eps[k]:
            lines.append(f'| `{k}` | {e["method"]} | `{e["where"]}` |')

    out = os.path.join(ROOT, 'tool-results/analytics-audit/contract-compare.md')
    os.makedirs(os.path.dirname(out), exist_ok=True)
    open(out, 'w', encoding='utf-8').write('\n'.join(lines))
    print(json.dumps({'api_files': len(api_files), 'backend_eps': len(eps), 'frontend_calls': len(calls),
                      'broken': len(broken), 'method_mismatch': len(method_mismatch),
                      'double_api': len(double), 'analytics_unused': len(unused)}, ensure_ascii=False, indent=1))
    print('\n断链（前 20）：')
    for c in broken[:20]:
        print(f'  {c["file"]}:{c["line"]} {c["http"]} {c["raw"]}')


if __name__ == '__main__':
    main()
