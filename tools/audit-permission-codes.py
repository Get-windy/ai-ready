# -*- coding: utf-8 -*-
"""
权限码对账：代码里 @SaCheckPermission 引用的权限码 vs sys_permission 表中实际存在的权限码。

背景：若注解引用了库中不存在的权限码，该接口对**所有非超管用户**都是拒绝的
（StpInterfaceImpl 只认角色-权限关联里有的码）——即"补注解"如果不同时补种子数据，
会把功能直接锁死。所以补注解前必须先做这个对账。

用法: python tools/audit-permission-codes.py
"""
import os
import re
import sys
import io
import json
import collections

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BACKEND = os.path.join(ROOT, 'backend')
CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')

# 匹配 @SaCheckPermission("a:b:c") 与 @SaCheckPermission({"a","b"}) / value = {...}
ANNO = re.compile(r'@SaCheckPermission\s*\(([^)]*)\)', re.S)
STR = re.compile(r'"([^"]+)"')


def scan_java():
    refs = collections.defaultdict(list)   # code -> [文件:行]
    for dirpath, dirnames, filenames in os.walk(BACKEND):
        dirnames[:] = [d for d in dirnames if d != 'target']
        parts = dirpath.replace('\\', '/').split('/')
        if 'src/test' in '/'.join(parts):
            continue
        for fn in filenames:
            if not fn.endswith('.java'):
                continue
            p = os.path.join(dirpath, fn)
            try:
                src = open(p, encoding='utf-8').read()
            except Exception:
                continue
            for m in ANNO.finditer(src):
                for code in STR.findall(m.group(1)):
                    line = src[:m.start()].count('\n') + 1
                    rel = os.path.relpath(p, ROOT).replace('\\', '/')
                    refs[code].append('%s:%d' % (rel, line))
    return refs


def query_codes():
    import psycopg2
    with psycopg2.connect(**CONN) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT permission_code FROM sys_permission WHERE deleted = 0")
            return set(r[0] for r in cur.fetchall() if r[0])


def main():
    refs = scan_java()
    db = query_codes()

    used = set(refs)
    missing = sorted(c for c in used if c not in db)
    unused = sorted(c for c in db if c not in used)

    print('代码引用权限码: %d 个' % len(used))
    print('库中权限码:     %d 个' % len(db))
    print('交叉命中:       %d 个' % len(used & db))
    print()
    print('== 代码引用了但库中没有（这些接口目前对非超管一律拒绝）: %d 个 ==' % len(missing))
    for c in missing:
        locs = refs[c]
        print('  %-46s %2d处  %s' % (c, len(locs), locs[0]))
    print()
    print('== 库中有但代码未引用（%d 个，仅列前 60）==' % len(unused))
    for c in unused[:60]:
        print('  ' + c)
    if len(unused) > 60:
        print('  ... 其余 %d 个' % (len(unused) - 60))

    out = {
        'used': sorted(used),
        'db': sorted(db),
        'missing_in_db': missing,
        'unused_in_code': unused,
        'refs': {k: v for k, v in refs.items()},
    }
    dst = os.path.join(ROOT, 'tools', 'audit-permission-codes.json')
    with open(dst, 'w', encoding='utf-8') as f:
        json.dump(out, f, ensure_ascii=False, indent=1)
    print()
    print('已写入 tools/audit-permission-codes.json')


if __name__ == '__main__':
    main()
