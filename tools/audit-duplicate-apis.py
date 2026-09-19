# -*- coding: utf-8 -*-
"""
重复能力接口审计（只读）：找出「同一业务资源被多套接口实现」的情况。

判定：把接口路径按「资源尾部」聚合（如 /api/customer/page 与 /api/erp/md/customer/page
的资源尾部都是 customer/page），若同一尾部由 >=2 个不同 Controller 提供，即为重复候选。

这类重复正是"功能没打通、各模块各写一套"的直接证据。

用法: python tools/audit-duplicate-apis.py
"""
import os
import re
import sys
import io
import json
import collections

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')

CLS_MAP = re.compile(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?[{]?\s*"([^"]*)"')
MTH_MAP = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping\s*\(\s*(?:value\s*=\s*)?[{]?\s*"([^"]*)"')


def norm(p):
    p = re.sub(r'\{[^}]*\}', '{}', p)
    p = re.sub(r'/+', '/', p)
    return p.rstrip('/') or '/'


def tail_of(path):
    """资源尾部：去掉 /api、/api/v1、/api/erp、/api/erp/md 之类前缀，保留资源+动作"""
    seg = [s for s in path.split('/') if s]
    for skip in ('api', 'v1', 'v2'):
        if seg and seg[0] == skip:
            seg.pop(0)
    # 去掉一个模块层（erp/md/...）
    if len(seg) > 2 and seg[0] in ('erp', 'finance', 'crm', 'dms', 'wms', 'hr'):
        seg.pop(0)
        if seg and seg[0] in ('md', 'sale', 'stock', 'purchase', 'finance'):
            seg.pop(0)
    return '/'.join(seg)


def main():
    rows = []
    for dirpath, dirnames, filenames in os.walk(os.path.join(ROOT, 'backend')):
        dirnames[:] = [d for d in dirnames if d != 'target']
        if 'src/test' in dirpath.replace('\\', '/'):
            continue
        for fn in filenames:
            if not fn.endswith('.java'):
                continue
            p = os.path.join(dirpath, fn)
            try:
                src = open(p, encoding='utf-8', errors='ignore').read()
            except Exception:
                continue
            if '@RestController' not in src:
                continue
            m = CLS_MAP.search(src)
            base = m.group(1) if m else ''
            cls = fn[:-5]
            for mm in MTH_MAP.finditer(src):
                verb, sub = mm.group(1).upper(), mm.group(2)
                full = norm(base + '/' + sub)
                rows.append(dict(verb=verb, path=full, tail=tail_of(full),
                                 cls=cls, file=os.path.relpath(p, ROOT).replace('\\', '/')))

    groups = collections.defaultdict(list)
    for r in rows:
        groups[r['tail']].append(r)

    dup = {k: v for k, v in groups.items()
           if len(set(x['file'] for x in v)) >= 2 and not k.startswith('{}') and len(k) > 3}

    print('后端接口 %d 条，资源尾部 %d 个' % (len(rows), len(groups)))
    print('存在多套实现的资源尾部: %d 个' % len(dup))
    print()

    # 只关心"被前端调用的那套之外还有别的实现" —— 用 audit-api-usage 的结果标一下
    try:
        usage = json.load(open(os.path.join(ROOT, 'tools', 'audit-api-usage.json'), encoding='utf-8'))
        unused_paths = set(usage['unused_normal']) | set(usage['unused_internal'])
    except Exception:
        unused_paths = set()

    shown = 0
    lines = []
    for tail, items in sorted(dup.items()):
        files = sorted(set(x['file'] for x in items))
        # 全部实现都未被前端调用的，跳过（那属于「未接线」不是「重复」）
        if all(x['path'] in unused_paths for x in items):
            continue
        lines.append((tail, items))
    print('其中「至少有一套被前端使用」的重复组: %d 个' % len(lines))
    print()
    print('=== 重复实现明细（最多列 35 组）===')
    for tail, items in lines[:35]:
        print('  【%s】' % tail)
        seen = set()
        for x in items:
            key = (x['file'], x['verb'])
            if key in seen:
                continue
            seen.add(key)
            mark = '  (前端未调用)' if x['path'] in unused_paths else ''
            print('      %-6s %-46s %s%s' % (x['verb'], x['path'], x['cls'], mark))

    out = {t: [{'verb': x['verb'], 'path': x['path'], 'cls': x['cls'], 'file': x['file'],
                'frontend_unused': x['path'] in unused_paths} for x in v]
           for t, v in lines}
    with open(os.path.join(ROOT, 'tools', 'audit-duplicate-apis.json'), 'w', encoding='utf-8') as f:
        json.dump(out, f, ensure_ascii=False, indent=1)
    print()
    print('已写入 tools/audit-duplicate-apis.json')


if __name__ == '__main__':
    main()
