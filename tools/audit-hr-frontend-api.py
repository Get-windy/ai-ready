# -*- coding: utf-8 -*-
"""
人力资源模块前端 API 消费方扫描（只读）：
  api/hr/index.ts 里导出的每个 xxxApi.method 是否被任何页面/组件调用。
  0 次 = 前端死 API（或仅由脚本使用）。
用法: python tools/audit-hr-frontend-api.py
"""
import os
import re
import io
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PC = os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src')
API_FILE = os.path.join(PC, 'api', 'hr', 'index.ts')

api = open(API_FILE, encoding='utf-8').read()

names = []
for m in re.finditer(r'export const (\w+Api) = \{', api):
    obj = m.group(1)
    start = m.end()
    depth = 1
    i = start
    while i < len(api) and depth > 0:
        if api[i] == '{':
            depth += 1
        elif api[i] == '}':
            depth -= 1
        i += 1
    body = api[start:i]
    for mm in re.finditer(r'^\s{2}(\w+)\s*\(', body, re.M):
        names.append((obj, mm.group(1)))

usage = {}
for r, dirs, files in os.walk(PC):
    dirs[:] = [d for d in dirs if d not in ('node_modules', 'dist')]
    for f in files:
        if not f.endswith(('.vue', '.ts')):
            continue
        p = os.path.join(r, f)
        if os.path.normpath(p) == os.path.normpath(API_FILE):
            continue
        try:
            usage[p] = open(p, encoding='utf-8', errors='replace').read()
        except Exception:
            pass

unused = []
for obj, meth in sorted(names):
    pat = re.compile(r'\b' + re.escape(obj) + r'\s*\.\s*' + re.escape(meth) + r'\b')
    hits = [k for k, v in usage.items() if pat.search(v)]
    if not hits:
        unused.append('%s.%s' % (obj, meth))

print('api/hr 方法总数: %d' % len(names))
print('无任何页面调用的方法: %d' % len(unused))
for u in unused:
    print('  - %s' % u)
