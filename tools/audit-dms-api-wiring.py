# -*- coding: utf-8 -*-
"""
配送（DMS）模块前后端接口接线核对（只读）：
  1. 从后端审计产物读 dms / erp-delivery-route / core-base(trade) 的全部端点
  2. 从前端 src/api/** 与 src/views/**（含绕过 api 层的直调）提取 request.xxx 调用
  3. 双向比对：前端调了后端没有（失效接口）/ 后端配送端点前端从未调用（无消费方）
输出: tool-results/dms-api-wiring.json
用法: python tools/audit-dms-api-wiring.py
"""
import os
import re
import io
import json
import sys
import collections

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src')
BACKEND_JSON = os.path.join(ROOT, 'tool-results', 'dms-backend-audit.json')

# 注意：不能写成 request.xxx<T>(...) 的窄正则 —— 泛型可嵌套
# （request.get<Result<Record<string, any>>>('/x')），[^>]* 会匹配失败而漏报。
CALL = re.compile(r'request\s*\.\s*(get|post|put|delete|patch)\b')


def url_after(src, pos):
    """跳过泛型 <...>（可嵌套）与空白，取 request.xxx( 的第一个引号串"""
    i, n = pos, len(src)
    while i < n:
        c = src[i]
        if c.isspace():
            i += 1
            continue
        if c == '<':
            depth = 0
            while i < n:
                if src[i] == '<':
                    depth += 1
                elif src[i] == '>':
                    depth -= 1
                    if depth == 0:
                        i += 1
                        break
                i += 1
            continue
        break
    if i >= n or src[i] != '(':
        return None
    i += 1
    while i < n and src[i].isspace():
        i += 1
    if i < n and src[i] in '`\'"':
        q = src[i]
        j = src.find(q, i + 1)
        if j > i:
            return src[i + 1:j]
    return None
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


# 配送自有域（归一化后不含 /api）
DMS_PREFIXES = (
    '/dms/', '/dms',
    '/delivery/route',
    '/erp/md/route',
    '/trade/api-monitor', '/trade/inventory-sync', '/trade/external-order', '/trade/channel',
    '/erp/sale/logistics',
)


def is_dms(p):
    return any(p == x or p.startswith(x) for x in DMS_PREFIXES)


def main():
    data = json.load(open(BACKEND_JSON, encoding='utf-8'))
    backend = collections.defaultdict(list)
    for c in data['controllers']:
        for e in c['endpoints']:
            k = (e['verb'], norm(e['full']))
            if is_dms(k[1]):
                backend[k].append('%s#%s' % (c['class'], e['method']))

    front = collections.defaultdict(list)
    for r, dirs, files in os.walk(SRC):
        dirs[:] = [d for d in dirs if d not in ('node_modules', 'dist')]
        for f in files:
            if not f.endswith(('.ts', '.vue')):
                continue
            p = os.path.join(r, f)
            src = open(p, encoding='utf-8', errors='replace').read()
            rel = os.path.relpath(p, ROOT).replace('\\', '/')
            for m in CALL.finditer(src):
                verb = m.group(1).upper()
                url = url_after(src, m.end())
                if not url or not url.startswith('/'):
                    continue
                n = norm(url)
                if not is_dms(n):
                    continue
                line = src[:m.start()].count('\n') + 1
                front[(verb, n)].append('%s:%d' % (rel, line))

    missing = sorted(k for k in front if k not in backend)
    unused = sorted(k for k in backend if k not in front)

    print('后端配送域端点: %d' % len(backend))
    print('前端调用的不同 (verb,path): %d' % len(front))
    print()
    print('=' * 104)
    print('① 前端调用了但后端没有该端点（%d 个 —— 前端调用会 404/兜底）' % len(missing))
    print('=' * 104)
    for k in missing:
        print('  %-6s %-56s  %s' % (k[0], k[1], front[k][0]))
    print()
    print('=' * 104)
    print('② 后端端点前端未调用（%d 个 —— 可能是内部/司机端/渠道回调，需复核）' % len(unused))
    print('=' * 104)
    for k in unused:
        print('  %-6s %-56s  %s' % (k[0], k[1], backend[k][0]))

    os.makedirs(os.path.join(ROOT, 'tool-results'), exist_ok=True)
    dest = os.path.join(ROOT, 'tool-results', 'dms-api-wiring.json')
    with open(dest, 'w', encoding='utf-8') as f:
        json.dump({
            'front_calls': {('%s %s' % k): v for k, v in front.items()},
            'missing_backend': {('%s %s' % k): front[k] for k in missing},
            'unused_backend': {('%s %s' % k): backend[k] for k in unused},
        }, f, ensure_ascii=False, indent=1)
    print()
    print('输出: %s' % dest)


if __name__ == '__main__':
    main()
