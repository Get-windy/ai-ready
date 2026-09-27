#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
资料模块（master-data）前后端接口契约比对（2026-09-24 资料模块审计用）

复用 tools/audit-finance-contract.py 的方法：
  · 前端：扫资料模块用到的 api 封装文件 + 24 个叶子页面的内联 request 调用
  · 后端：扫**整个 backend**（资料页会调 wms/options/user 等模块接口，只扫本模块会误判断链）
  · 归一路径：去 /api 前缀、路径变量 → {}、合并斜杠、小写
输出：
  断链（前端有后端无）/ 方法不匹配 / 双 api 前缀 / 页面直连路径清单
  tool-results/md-audit/contract-compare.md
用法: python tools/audit-md-contract.py
"""
import os
import re
import io
import sys
import json

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FE_API_DIR = os.path.join(ROOT, 'frontend/apps/pc-admin/src/api')
FE_VIEW_DIR = os.path.join(ROOT, 'frontend/apps/pc-admin/src/views')
BE_ROOT = os.path.join(ROOT, 'backend')
OUT_DIR = os.path.join(ROOT, 'tool-results/md-audit')

# 资料模块 24 个菜单叶子用到的 api 封装
FE_FILES = ['md.ts', 'erp/product.ts', 'erp/partner.ts', 'erp/productImage.ts',
            'erp/linkedAccount.ts', 'erp/warehousePlan.ts', 'erp/batch.ts', 'erp.ts',
            'options.ts', 'payment/md.ts', 'md-product-price.ts', 'pricing-approval.ts',
            'finance/index.ts']

# 资料模块菜单叶子对应的页面（含双入口 form 页与共享组件）
MD_PAGES = [
    'md/accounting-subject/index.vue', 'md/bank-account/index.vue', 'md/barcode/index.vue',
    'md/customer/index.vue', 'md/customer/form.vue',
    'md/expense-type/index.vue', 'md/image/index.vue', 'md/linked-account/index.vue',
    'md/location/index.vue', 'md/logistics/index.vue', 'md/logistics/form.vue',
    'md/other-income/index.vue', 'md/partner/index.vue', 'md/partner/form.vue',
    'md/payment-account/index.vue', 'md/payment-channel/index.vue', 'md/payment-method/index.vue',
    'md/product-price/index.vue', 'md/product-supplement/index.vue', 'md/route/index.vue',
    'md/supplier/index.vue', 'md/supplier/form.vue', 'md/warehouse-plan/index.vue',
    'md/components/AttachmentUpload.vue', 'md/components/ContactList.vue',
    'md/components/PartnerListPage.vue', 'md/components/PartnerTypeSidebar.vue',
    'md/customer/components/ContactFormModal.vue', 'md/customer/components/MemberCardModal.vue',
    'erp/product/index.vue', 'erp/product/form.vue', 'erp/product/grade.vue',
    'erp/product/inventory-mode.vue', 'erp/pricing/approval/index.vue', 'erp/batch/index.vue',
]


def norm(p):
    p = p.split('?')[0].strip()
    p = re.sub(r'^/api(?=/|$)', '', p)
    p = re.sub(r'\$\{[^}]*\}', '{}', p)
    p = re.sub(r'\{[^}]*\}', '{}', p)
    p = re.sub(r'/+', '/', p)
    if not p.startswith('/'):
        p = '/' + p
    return p.rstrip('/').lower() or '/'


def match(path, eps):
    """前端把路径变量写死（`/import/v2/excel/customer`）而后端是模板（`{}`）时，
    直接全等比对会误报断链。这里额外尝试「把任一段替换为 {}」的候选。
    返回命中的后端路径，未命中返回 None。"""
    if path in eps:
        return path
    parts = [x for x in path.strip('/').split('/') if x != '']
    for i in range(len(parts)):
        cand = '/' + '/'.join(parts[:i] + ['{}'] + parts[i + 1:])
        if cand in eps:
            return cand
    return None


METHOD_ANN = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping\s*(?:\(\s*(?:value\s*=\s*)?(?:["\']([^"\']*)["\'])?|$)')
CLASS_MAP = re.compile(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']*)["\']')
REQ_CALL = re.compile(r"""request\.(get|post|put|delete|patch)\s*(?:<[^>]*>)?\s*\(\s*[`'"]([^`'"]+)[`'"]""", re.I)


def scan_backend():
    eps = {}
    for dirpath, _, files in os.walk(BE_ROOT):
        if os.sep + 'target' + os.sep in dirpath + os.sep:
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
                    'where': '%s:%d' % (os.path.relpath(fp, ROOT).replace('\\', '/'), i + 1),
                    'raw': full,
                })
    return eps


def scan_calls(files, base_dir, kind):
    calls = []
    for rel in files:
        fp = os.path.join(base_dir, rel)
        if not os.path.exists(fp):
            calls.append({'file': rel, 'missing': True, 'kind': kind})
            continue
        lines = open(fp, encoding='utf-8', errors='ignore').read().split('\n')
        for i, ln in enumerate(lines):
            for m in REQ_CALL.finditer(ln):
                raw = m.group(2)
                tail = ln[m.end():].lstrip()
                if tail.startswith('+'):
                    raw = raw + '{}'
                calls.append({'file': rel, 'line': i + 1, 'http': m.group(1).upper(),
                              'raw': raw, 'norm': norm(raw), 'kind': kind,
                              'double_api': raw.startswith('/api/')})
    return calls


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    eps = scan_backend()
    api_calls = scan_calls(FE_FILES, FE_API_DIR, 'api')
    page_calls = scan_calls(MD_PAGES, FE_VIEW_DIR, 'page')
    calls = [c for c in api_calls + page_calls if 'missing' not in c]
    missing_files = [c['file'] for c in api_calls + page_calls if c.get('missing')]

    broken = [c for c in calls if not match(c['norm'], eps)]
    method_mismatch = []
    for c in calls:
        hit = match(c['norm'], eps)
        if hit and c['http'] not in {e['method'] for e in eps[hit]}:
            method_mismatch.append(c)
        c['hit'] = hit
    double_api = [c for c in calls if c.get('double_api')]

    lines = ['# 资料模块 · 前后端接口契约比对（2026-09-24）', '',
             '- 后端端点（归一路径去重）**%d** 个' % len(eps),
             '- 前端 api 封装调用 **%d** 处；页面内联调用 **%d** 处' % (
                 len([c for c in api_calls if 'missing' not in c]),
                 len([c for c in page_calls if 'missing' not in c])),
             '- **断链（前端有、后端无）：%d 处**' % len(broken),
             '- 方法不匹配：%d 处' % len(method_mismatch),
             '- 双 `/api` 前缀：%d 处' % len(double_api), '']
    if missing_files:
        lines += ['> ⚠️ 扫描清单里不存在的文件：%s' % ', '.join(sorted(set(missing_files))), '']

    lines += ['## 一、断链明细（前端有、后端无）', '']
    if broken:
        lines += ['| 来源 | 文件:行 | 方法 | 原始路径 | 归一化 |', '|---|---|---|---|---|']
        for c in broken:
            lines.append('| %s | `%s:%s` | %s | `%s` | `%s` |' % (
                c['kind'], c['file'], c.get('line', '-'), c['http'], c['raw'], c['norm']))
    else:
        lines.append('（无）')

    lines += ['', '## 二、方法不匹配', '']
    if method_mismatch:
        lines += ['| 来源 | 文件:行 | 前端方法 | 路径 | 后端可用方法 |', '|---|---|---|---|---|']
        for c in method_mismatch:
            ms = ', '.join(sorted({e['method'] for e in eps[c['hit']]}))
            lines.append('| %s | `%s:%s` | %s | `%s` | %s |' % (
                c['kind'], c['file'], c.get('line', '-'), c['http'], c['norm'], ms))
    else:
        lines.append('（无）')

    lines += ['', '## 三、页面内联 request 调用（非 api 封装，规范性观察）', '']
    if page_calls:
        lines += ['| 文件:行 | 方法 | 路径 | 后端是否存在 |', '|---|---|---|---|']
        for c in sorted(page_calls, key=lambda x: (x['file'], x.get('line', 0))):
            lines.append('| `%s:%s` | %s | `%s` | %s |' % (
                c['file'], c.get('line', '-'), c['http'], c['raw'],
                '是' if c['norm'] in eps else '**否**'))
    else:
        lines.append('（无）')

    open(os.path.join(OUT_DIR, 'contract-compare.md'), 'w', encoding='utf-8').write('\n'.join(lines))

    print('后端端点(归一后): %d' % len(eps))
    print('前端 api 调用: %d / 页面内联调用: %d' % (
        len([c for c in api_calls if 'missing' not in c]),
        len([c for c in page_calls if 'missing' not in c])))
    print('断链: %d   方法不匹配: %d   双api前缀: %d' % (len(broken), len(method_mismatch), len(double_api)))
    print()
    print('--- 断链明细 ---')
    for c in broken:
        print('  [%s] %s:%s %s %s' % (c['kind'], c['file'], c.get('line', '-'), c['http'], c['raw']))
    print()
    print('--- 方法不匹配 ---')
    for c in method_mismatch:
        print('  [%s] %s:%s %s %s (后端有 %s)' % (
            c['kind'], c['file'], c.get('line', '-'), c['http'], c['norm'],
            ','.join(sorted({e['method'] for e in eps[c['hit']]}))))
    if missing_files:
        print()
        print('--- 清单中不存在的文件 ---')
        for f in sorted(set(missing_files)):
            print('  %s' % f)
    print()
    print('输出: tool-results/md-audit/contract-compare.md')


if __name__ == '__main__':
    main()
