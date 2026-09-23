#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
财务模块 前后端接口契约比对（2026-09-23 财务审计用）

解决的问题：前端 api 封装里写的路径，后端**到底存不存在**？
本仓已多次踩坑（如销售模块 3 处配置弹窗调用不存在的接口、双 `/api` 前缀），
所以这里做双向比对：前端有而后端无 = 断链；后端有而前端零引用 = 冗余候选。

方法：
  · 前端：扫 apps/pc-admin/src/api 下财务相关文件，正则提取 request.get/post/put/delete('路径')
          并与同一文件里导出的 api 对象名、被哪些页面 import 关联。
  · 后端：扫 erp-finance / erp-budget，拼接类级 @RequestMapping + 方法级 @XxxMapping，
          再配类级 @RequestMapping 的可选前缀（context-path）。
  · 归一路径：去掉 /api 前缀、去掉路径变量 `{id}` → `{}`、去掉可选 `?`、合并连续斜杠、统一小写。
输出：断链清单、冗余候选、以及比对统计。
"""
import os
import re
import json
import collections

ROOT = 'I:/AI-Ready'
FE_API_DIR = os.path.join(ROOT, 'frontend/apps/pc-admin/src/api')
# ⚠️ 必须扫**整个后端**：财务前端也会调采购/销售/营销/wms 等模块的接口
#    （如 finance/index.ts 里的 `/purchase/doc-query/page`、`/sales/doc-query/page`），
#    只扫财务两个模块会把它们全部误判成断链（第一版就是这么错的：报 431 条断链）。
BE_ROOT = os.path.join(ROOT, 'backend')
BE_DIRS = [BE_ROOT]

FE_FILES = ['finance/index.ts', 'finance/ar-ap-adjust.ts', 'finance/auxiliary.ts',
            'finance/cash-transfer.ts', 'budget.ts', 'analytics-finance.ts', 'erp.ts',
            'finance/payment.ts', 'finance/invoice.ts']


def norm(p):
    p = p.split('?')[0].strip()
    p = re.sub(r'^/api(?=/|$)', '', p)
    p = re.sub(r'\$\{[^}]*\}', '{}', p)
    p = re.sub(r'\{[^}]*\}', '{}', p)
    p = re.sub(r'/+', '/', p)
    if not p.startswith('/'):
        p = '/' + p
    return p.rstrip('/').lower() or '/'


# ── 后端端点 ──
METHOD_ANN = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping\s*(?:\(\s*(?:value\s*=\s*)?(?:["\']([^"\']*)["\'])?|$)')
CLASS_MAP = re.compile(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']*)["\']')
SIMPLE_CLASS_MAP = re.compile(r'@RequestMapping\s*\(\s*["\']([^"\']*)["\']\s*\)')


def scan_backend():
    eps = {}   # norm_path -> list of (METHOD, file:line, raw)
    for base in BE_DIRS:
        for dirpath, _, files in os.walk(base):
            for f in files:
                if not f.endswith('.java'):
                    continue
                fp = os.path.join(dirpath, f)
                src = open(fp, encoding='utf-8', errors='ignore').read()
                if '@RestController' not in src and '@Controller' not in src:
                    continue
                lines = src.split('\n')
                class_prefix = ''
                for i, ln in enumerate(lines):
                    m = CLASS_MAP.search(ln) or SIMPLE_CLASS_MAP.search(ln)
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
                    key = norm(full)
                    eps.setdefault(key, []).append({
                        'method': http,
                        'where': f'{os.path.relpath(fp, ROOT).replace(chr(92), "/")}:{i+1}',
                        'raw': full,
                    })
    return eps


# ── 前端调用 ──
REQ_CALL = re.compile(r"""request\.(get|post|put|delete|patch)\s*(?:<[^>]*>)?\s*\(\s*[`'"]([^`'"]+)[`'"]""", re.I)
EXPORT_OBJ = re.compile(r'export\s+const\s+(\w+)\s*=|^const\s+(\w+)\s*=\s*\{', re.M)


def scan_frontend():
    calls = []
    for rel in FE_FILES:
        fp = os.path.join(FE_API_DIR, rel)
        if not os.path.exists(fp):
            calls.append({'file': rel, 'missing': True})
            continue
        src = open(fp, encoding='utf-8', errors='ignore').read()
        lines = src.split('\n')
        for i, ln in enumerate(lines):
            for m in REQ_CALL.finditer(ln):
                raw = m.group(2)
                # ⚠️ 拼接写法 `'/erp/xxx/' + id`：字面量只到引号为止，若后面紧跟 `+`，
                #    说明还有一段路径变量，必须补上 `{}`，否则会被误判成断链
                #    （第一版就是这么误报的：`/erp/capital-flow/reconcile/` + id）。
                tail = ln[m.end():].lstrip()
                if tail.startswith('+'):
                    raw = raw + '{}'
                calls.append({
                    'file': rel,
                    'line': i + 1,
                    'http': m.group(1).upper(),
                    'raw': raw,
                    'norm': norm(raw),
                    'double_api': raw.startswith('/api/'),
                })
    return calls


def main():
    eps = scan_backend()
    calls = scan_frontend()

    broken = [c for c in calls if 'missing' not in c and c['norm'] not in eps]
    # 方法不匹配（路径在，但 HTTP 方法后端没有）
    method_mismatch = []
    for c in calls:
        if 'missing' in c or c['norm'] not in eps:
            continue
        methods = {e['method'] for e in eps[c['norm']]}
        if c['http'] not in methods and c['http'] not in ('GET',) or (c['http'] not in methods and c['http'] == 'GET'):
            if c['http'] not in methods:
                method_mismatch.append(c)

    out = {
        'backend_endpoints': len(eps),
        'frontend_calls': len([c for c in calls if 'missing' not in c]),
        'broken_calls': len(broken),
        'method_mismatch': len(method_mismatch),
        'double_api_prefix': [c for c in calls if c.get('double_api')],
    }
    lines = ['# 财务模块 · 前后端接口契约比对', '',
             f'- 后端端点（归一路径后去重）**{len(eps)}** 个；前端调用 **{out["frontend_calls"]}** 处',
             f'- **断链（前端调用、后端无此路径）：{len(broken)} 处**',
             f'- 方法不匹配（路径在、HTTP 方法不在）：{len(method_mismatch)} 处',
             f'- 双 `/api` 前缀：{len(out["double_api_prefix"])} 处', '']

    lines.append('## 一、断链明细（前端有、后端无）')
    lines.append('')
    if broken:
        lines.append('| 前端文件:行 | 方法 | 原始路径 | 归一化 |')
        lines.append('|---|---|---|---|')
        for c in broken:
            lines.append(f'| `api/{c["file"]}:{c["line"]}` | {c["http"]} | `{c["raw"]}` | `{c["norm"]}` |')
    else:
        lines.append('（无）')

    lines.append('')
    lines.append('## 二、方法不匹配')
    lines.append('')
    if method_mismatch:
        lines.append('| 前端文件:行 | 前端方法 | 路径 | 后端可用方法 |')
        lines.append('|---|---|---|---|')
        for c in method_mismatch:
            ms = ', '.join(sorted({e['method'] for e in eps[c['norm']]}))
            lines.append(f'| `api/{c["file"]}:{c["line"]}` | {c["http"]} | `{c["raw"]}` | {ms} |')
    else:
        lines.append('（无）')

    lines.append('')
    lines.append('## 三、双 `/api` 前缀调用点')
    lines.append('')
    if out['double_api_prefix']:
        for c in out['double_api_prefix']:
            lines.append(f'- `api/{c["file"]}:{c["line"]}` → `{c["raw"]}`')
    else:
        lines.append('（无 —— 财务模块不存在双前缀问题）')

    lines.append('')
    lines.append('## 四、端点侧冗余候选（后端有、前端零引用）')
    lines.append('')
    called = {c['norm'] for c in calls if 'missing' not in c}
    unused = sorted(k for k in eps if k not in called)
    lines.append(f'共 **{len(unused)}** 个后端路径前端 api 未引用（可能由页面直连、或属死端点，需人工判定）：')
    lines.append('')
    lines.append('| 路径 | 方法 | 位置 |')
    lines.append('|---|---|---|')
    for k in unused[:200]:
        for e in eps[k]:
            lines.append(f'| `{k}` | {e["method"]} | `{e["where"]}` |')
    if len(unused) > 200:
        lines.append(f'| … | 其余 {len(unused) - 200} 个省略 | |')

    open(os.path.join(ROOT, 'tool-results/finance-audit/contract-compare.md'), 'w', encoding='utf-8').write('\n'.join(lines))
    print(json.dumps(out, ensure_ascii=False, indent=2))
    print('\n断链前 15:')
    for c in broken[:15]:
        print(f'  {c["file"]}:{c["line"]} {c["http"]} {c["raw"]}')


if __name__ == '__main__':
    main()
