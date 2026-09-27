#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
资料模块 API 封装「调用点 × 消费方」对账（只读）。

动机：tools/audit-md-contract.py 报出的断链里，有一部分是**无人调用的死封装**
（例如 erp.ts 里属于采购/销售的方法混进来了）。必须先判定「这个封装方法到底被
哪个页面调用」，才能区分：
  · 断链 + 有消费方  → 真缺陷（页面点下去 404）
  · 断链 + 无消费方  → 死封装（可删，不影响功能）
  · 路径存在 + 无消费方 → 无 UI 的能力

方法：按行扫 api 文件，维护 (export 对象, 方法名) 状态机，把每个 request 调用
      归属到 `对象.方法`；再在全前端视图层 grep `对象.方法` 的出现处。
输出: tool-results/md-audit/api-usage.json + stdout 报告
用法: 先跑 python tools/audit-md-contract.py 生成端点全集，再跑本脚本
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
OUT_DIR = os.path.join(ROOT, 'tool-results/md-audit')

FE_FILES = ['md.ts', 'erp/product.ts', 'erp/partner.ts', 'erp/productImage.ts',
            'erp/linkedAccount.ts', 'erp/warehousePlan.ts', 'erp/batch.ts', 'erp.ts',
            'options.ts', 'payment/md.ts', 'md-product-price.ts', 'pricing-approval.ts',
            'finance/index.ts']
# 只有这些文件整体属于资料域；erp.ts / finance/index.ts / options.ts 是多模块共用文件，
# 里面的方法要按「是否被资料页面调用」筛选，否则会把采购/销售/财务的调用算进来。
SHARED = {'erp.ts', 'finance/index.ts', 'options.ts', 'erp/product.ts'}

MD_PAGE_HINTS = ['views/md/', 'views/erp/product/', 'views/erp/pricing/approval/', 'views/erp/batch/']

REQ_CALL = re.compile(r"""request\.(get|post|put|delete|patch)\s*(?:<[^>]*>)?\s*\(\s*[`'"]([^`'"]+)[`'"]""", re.I)
OBJ_START = re.compile(r'^export\s+const\s+(\w+)\s*(?::[^=]+)?=\s*\{')
OBJ_START2 = re.compile(r'^export\s+const\s+(\w+)\s*=')
MEMBER = re.compile(r'^\s{2}(\w+)\s*\(')
METHOD_DEF = re.compile(r'^\s{2}(\w+)\s*\(')


def norm(p):
    p = p.split('?')[0].strip()
    p = re.sub(r'^/api(?=/|$)', '', p)
    p = re.sub(r'\$\{[^}]*\}', '{}', p)
    p = re.sub(r'\{[^}]*\}', '{}', p)
    p = re.sub(r'/+', '/', p)
    if not p.startswith('/'):
        p = '/' + p
    return p.rstrip('/').lower() or '/'


def load_backend_eps():
    """复用契约脚本的端点扫描（这里重跑一遍，保持脚本独立）"""
    be = os.path.join(ROOT, 'backend')
    ann = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping\s*(?:\(\s*(?:value\s*=\s*)?(?:["\']([^"\']*)["\'])?|$)')
    cm = re.compile(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']*)["\']')
    eps = {}
    for dirpath, _, files in os.walk(be):
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
            prefix = ''
            for ln in lines:
                m = cm.search(ln)
                if m and '@RequestMapping' in ln:
                    prefix = m.group(1)
                    break
            for i, ln in enumerate(lines):
                m = ann.search(ln)
                if not m:
                    continue
                sub = m.group(2) or ''
                full = (prefix.rstrip('/') + '/' + sub.lstrip('/')) if prefix else sub
                eps.setdefault(norm(full), set()).add(m.group(1).upper())
    return eps


def scan_api_file(rel):
    fp = os.path.join(FE_API_DIR, rel)
    if not os.path.exists(fp):
        return []
    lines = open(fp, encoding='utf-8', errors='ignore').read().split('\n')
    out = []
    obj = None
    method = None
    for i, ln in enumerate(lines):
        if OBJ_START.match(ln) or OBJ_START2.match(ln):
            m = OBJ_START.match(ln) or OBJ_START2.match(ln)
            obj = m.group(1)
            method = None
            continue
        m = METHOD_DEF.match(ln)
        if m and obj:
            method = m.group(1)
        for mm in REQ_CALL.finditer(ln):
            raw = mm.group(2)
            tail = ln[mm.end():].lstrip()
            if tail.startswith('+'):
                raw = raw + '{}'
            out.append({'file': rel, 'line': i + 1, 'obj': obj, 'method': method,
                        'http': mm.group(1).upper(), 'raw': raw, 'norm': norm(raw)})
    return out


def scan_view_usage():
    """全视图层文本（含组件），用于 grep `obj.method` 的消费方"""
    texts = {}
    for dirpath, dirs, files in os.walk(FE_VIEW_DIR):
        dirs[:] = [d for d in dirs if d not in ('node_modules', 'dist')]
        for f in files:
            if not f.endswith(('.vue', '.ts')):
                continue
            fp = os.path.join(dirpath, f)
            rel = os.path.relpath(fp, ROOT).replace('\\', '/')
            texts[rel] = open(fp, encoding='utf-8', errors='ignore').read()
    return texts


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    eps = load_backend_eps()
    texts = scan_view_usage()

    rows = []
    for rel in FE_FILES:
        for c in scan_api_file(rel):
            if not c['obj'] or not c['method']:
                continue
            usage = []
            needle = '%s.%s' % (c['obj'], c['method'])
            for vrel, txt in texts.items():
                if needle in txt:
                    usage.append(vrel)
            md_usage = [u for u in usage if any(h in u for h in MD_PAGE_HINTS)]
            c['usage_all'] = usage
            c['usage_md'] = md_usage
            c['exists_backend'] = c['norm'] in eps
            c['method_ok'] = c['norm'] in eps and c['http'] in eps[c['norm']]
            rows.append(c)

    md_rows = [r for r in rows if r['usage_md']]
    broken_md = [r for r in md_rows if not r['exists_backend']]
    mismatch_md = [r for r in md_rows if r['exists_backend'] and not r['method_ok']]
    dead_api = [r for r in rows if r['file'] in SHARED and not r['usage_all']]

    print('资料模块 api 封装调用点: %d（其中被资料页面使用: %d）' % (len(rows), len(md_rows)))
    print()
    print('=== A. 断链且被资料页面调用（真缺陷）===')
    for r in broken_md:
        print('  %s:%s  %s %s' % (r['file'], r['line'], r['http'], r['raw']))
        for u in r['usage_md']:
            print('        ← %s' % u)
    print('  小计: %d' % len(broken_md))
    print()
    print('=== B. 方法不匹配且被资料页面调用 ===')
    for r in mismatch_md:
        print('  %s:%s  %s %s (后端有 %s)' % (r['file'], r['line'], r['http'], r['raw'],
                                              ','.join(sorted(eps[r['norm']]))))
        for u in r['usage_md']:
            print('        ← %s' % u)
    print('  小计: %d' % len(mismatch_md))
    print()
    print('=== C. 共用文件里零消费方的封装（死 API 候选，仅列 erp.ts/finance/options/product）===')
    for r in dead_api:
        print('  %s:%s  %s.%s  %s %s' % (r['file'], r['line'], r['obj'], r['method'], r['http'], r['raw']))
    print('  小计: %d' % len(dead_api))

    with open(os.path.join(OUT_DIR, 'api-usage.json'), 'w', encoding='utf-8') as f:
        json.dump({'rows': rows, 'broken_md': [r['file'] + ':' + str(r['line']) for r in broken_md],
                   'mismatch_md': [r['file'] + ':' + str(r['line']) for r in mismatch_md],
                   'dead_api_shared': [r['file'] + ':' + str(r['line']) for r in dead_api]},
                  f, ensure_ascii=False, indent=1)
    print()
    print('输出: tool-results/md-audit/api-usage.json')


if __name__ == '__main__':
    main()
