# -*- coding: utf-8 -*-
"""
仓储模块前后端接口接线核对（只读）：
  1. 从后端审计产物读 erp-stock / wms 模块的全部端点
  2. 从前端 src/api/** 提取所有 request.get/post/... 调用
  3. 双向比对：前端调了后端没有（失效接口）/ 后端仓储端点前端从未调用（无消费方）
输出: tool-results/storage-api-wiring.json
用法: python tools/audit-storage-api-wiring.py
"""
import os
import re
import io
import json
import sys
import collections

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
API_DIR = os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src', 'api')
BACKEND_JSON = os.path.join(ROOT, 'tool-results', 'storage-backend-audit.json')

CALL = re.compile(r'request\s*\.\s*(get|post|put|delete|patch)\s*(?:<[^>]*>)?\s*\(\s*[`\'"]([^`\'"]+)')
PLACEHOLDER = re.compile(r'\$\{[^}]*\}|\{[^}]*\}')


def norm(u):
    u = u.strip()
    if u.startswith('/api/'):
        u = u[len('/api'):]
    elif u == '/api':
        u = '/'
    u = PLACEHOLDER.sub('{}', u)
    u = re.sub(r'/+$', '', u)
    if not u.startswith('/'):
        u = '/' + u
    return u


# 仓储域的路径前缀（归一化后，不含 /api）。erp-stock 模块里混着商品/商城/营销控制器，
# 这些不属于仓储，必须排除，否则比对结果全是噪音。
STORAGE_PREFIXES = (
    '/erp/stock', '/erp/warehouse', '/erp/product-location',
    '/wms/', '/v1/warehouse/',
)


def is_storage(p):
    return p.startswith(STORAGE_PREFIXES)


def main():
    data = json.load(open(BACKEND_JSON, encoding='utf-8'))
    backend = collections.defaultdict(list)
    for c in data['controllers']:
        if c['module'] not in ('erp-stock', 'wms'):
            continue
        for e in c['endpoints']:
            k = (e['verb'], norm(e['full']))
            if is_storage(k[1]):
                backend[k].append('%s#%s' % (c['class'], e['method']))

    front = collections.defaultdict(list)
    for r, dirs, files in os.walk(API_DIR):
        dirs[:] = [d for d in dirs if d != 'node_modules']
        for f in files:
            if not f.endswith('.ts'):
                continue
            p = os.path.join(r, f)
            src = open(p, encoding='utf-8', errors='replace').read()
            rel = os.path.relpath(p, ROOT).replace('\\', '/')
            for m in CALL.finditer(src):
                verb, url = m.group(1).upper(), m.group(2)
                if not url.startswith('/'):
                    continue
                n = norm(url)
                if not is_storage(n):
                    continue
                line = src[:m.start()].count('\n') + 1
                front[(verb, n)].append('%s:%d' % (rel, line))

    missing = sorted(k for k in front if k not in backend)
    unused = sorted(k for k in backend if k not in front)

    print('后端 erp-stock/wms 端点: %d' % len(backend))
    print('前端 api/ 调用的不同 (verb,path): %d' % len(front))
    print()
    print('=' * 100)
    print('① 前端调用了但仓储后端没有该端点（%d 个 —— 前端调用会 404/兜底）' % len(missing))
    print('=' * 100)
    for k in missing:
        print('  %-6s %-58s  %s' % (k[0], k[1], front[k][0]))
    print()
    print('=' * 100)
    print('② 仓储后端端点前端 api/ 未调用（%d 个 —— 可能是 PDA/移动端/内部调用，需复核）' % len(unused))
    print('=' * 100)
    for k in unused:
        print('  %-6s %-58s  %s' % (k[0], k[1], backend[k][0]))

    os.makedirs(os.path.join(ROOT, 'tool-results'), exist_ok=True)
    dest = os.path.join(ROOT, 'tool-results', 'storage-api-wiring.json')
    with open(dest, 'w', encoding='utf-8') as f:
        json.dump({
            'front_calls': {('%s %s' % k): v for k, v in front.items()},
            'missing_backend': ['%s %s' % k for k in missing],
            'unused_backend': {('%s %s' % k): v for k, v in unused_dict(unused, backend).items()},
        }, f, ensure_ascii=False, indent=1)
    print()
    print('输出: %s' % dest)


def unused_dict(keys, backend):
    return {k: backend[k] for k in keys}


if __name__ == '__main__':
    main()
