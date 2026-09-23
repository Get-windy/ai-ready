# -*- coding: utf-8 -*-
"""
人力资源模块前后端接口接线对账（只读）：
  1. 从 api/hr/index.ts 提取前端调用的 (method, path)
  2. 从 tool-results/hr-backend-audit.json 取后端真实端点
  3. 双向差集：前端调了但后端没有（断链） / 后端有但前端没调（无 UI 的能力）
用法: 先跑 python tools/audit-hr-backend.py，再 python tools/audit-hr-api-wiring.py
"""
import os
import re
import io
import sys
import json

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
API_FILE = os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src', 'api', 'hr', 'index.ts')
BACKEND_JSON = os.path.join(ROOT, 'tool-results', 'hr-backend-audit.json')

src = open(API_FILE, encoding='utf-8').read()
calls = []
for m in re.finditer(r"request\.(get|post|put|delete)\(\s*[`'\"]([^`'\"]+)[`'\"]", src):
    verb = 'GET' if m.group(1) == 'get' else 'POST' if m.group(1) == 'post' else 'PUT' if m.group(1) == 'put' else 'DELETE'
    calls.append((verb, m.group(2)))

data = json.load(open(BACKEND_JSON, encoding='utf-8'))
backends = set()
for c in data['controllers']:
    for e in c['endpoints']:
        backends.add((e['verb'], e['full']))


def norm(p):
    """把前端路径归一化：/hr/positions/${id} → /api/hr/positions/*"""
    p = p.split('?')[0]
    p = re.sub(r'\$\{[^}]+\}', '*', p)
    if p.startswith('/api/'):
        p = p[4:]
    return p


bnorm = {}
for verb, full in backends:
    key = re.sub(r'\{[^}]+\}', '*', full)
    bnorm.setdefault((verb, key), full)

print('前端调用: %d 条' % len(calls))
print('后端端点: %d 个' % len(backends))
print()
print('=' * 96)
print('① 前端调了但后端不存在（断链 → 必 404）')
print('=' * 96)
miss = 0
for verb, p in calls:
    n = norm(p)
    if (verb, '/api' + n) not in bnorm:
        # 也允许无 /api 前缀的写法
        if (verb, n) not in bnorm:
            miss += 1
            print('  ✗ %-6s %s' % (verb, p))
print('  小计: %d' % miss)

print()
print('=' * 96)
print('② 后端有但前端 api/hr 未调用（能力已建、界面未接 或 由页面内联调用）')
print('=' * 96)
used = set()
for verb, p in calls:
    n = norm(p)
    used.add((verb, '/api' + n))
    used.add((verb, n))
un = 0
for verb, key in sorted(bnorm):
    if (verb, key) not in used and (verb, bnorm[(verb, key)]) not in used:
        un += 1
        print('  · %-6s %s' % (verb, bnorm[(verb, key)]))
print('  小计: %d' % un)
