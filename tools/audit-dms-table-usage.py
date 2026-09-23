# -*- coding: utf-8 -*-
"""
配送（DMS）模块数据表消费方盘点（只读）：
  对每张配送相关表，在 backend 全源码中找消费方：
    - @TableName("tbl") 实体 → 该实体被哪些文件引用
    - 手写 SQL / XML 中的表名
  行数（live）由 devdb 直查。
输出: tool-results/dms-table-usage.json
用法: python tools/audit-dms-table-usage.py
"""
import os
import io
import re
import json
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BACKEND = os.path.join(ROOT, 'backend')
CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')


def main():
    import psycopg2
    conn = psycopg2.connect(**CONN)
    cur = conn.cursor()
    cur.execute(r"""SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                    WHERE n.nspname='public' AND c.relkind='r' AND (
                      c.relname LIKE 'dms\_%' OR c.relname IN
                      ('erp_delivery_route','erp_route_point','erp_route','erp_route_area',
                       'erp_delivery_eta_notify','erp_freight_rule','erp_shipment_notify',
                       'erp_sale_order_logistics')) ORDER BY c.relname""")
    tables = [r[0] for r in cur.fetchall()]

    files = {}
    for r, dirs, fs in os.walk(BACKEND):
        dirs[:] = [d for d in dirs if d not in ('target', 'node_modules')]
        for f in fs:
            if f.endswith(('.java', '.xml')):
                p = os.path.join(r, f).replace('\\', '/')
                try:
                    files[p] = open(p, encoding='utf-8', errors='replace').read()
                except Exception:
                    pass

    out = []
    for t in tables:
        ent_files = [p for p, s in files.items()
                     if re.search(r'@TableName\s*\(\s*(?:value\s*=\s*)?["\']' + re.escape(t) + r'["\']', s)]
        ents = []
        for ep in ent_files:
            name = os.path.basename(ep)[:-5]
            ents.append(name)
        # 实体被引用情况
        ent_refs = {}
        for en in ents:
            pat = re.compile(r'\b' + re.escape(en) + r'\b')
            refs = [os.path.relpath(q, ROOT).replace('\\', '/')
                    for q, s in files.items()
                    if pat.search(s) and os.path.basename(q)[:-5] != en
                    and ('Mapper' in os.path.basename(q) or 'Service' in os.path.basename(q)
                         or 'Controller' in os.path.basename(q) or 'Listener' in os.path.basename(q)
                         or 'Job' in os.path.basename(q) or 'Handler' in os.path.basename(q))]
            ent_refs[en] = refs
        # 手写 SQL 引用（非 @TableName）
        sql_refs = [os.path.relpath(p, ROOT).replace('\\', '/') for p, s in files.items()
                    if not re.search(r'@TableName\s*\(\s*(?:value\s*=\s*)?["\']' + re.escape(t) + r'["\']', s)
                    and re.search(r'(?:from|join|into|update|table)\s+' + re.escape(t) + r'\b', s, re.I)]
        try:
            cur.execute('SELECT count(*) FROM "%s"' % t)
            live = cur.fetchone()[0]
        except Exception:
            conn.rollback()
            live = -1
        out.append({'table': t, 'entities': ents, 'entity_refs': ent_refs,
                    'sql_refs': sql_refs[:6], 'sql_ref_count': len(sql_refs), 'live': live})

    os.makedirs(os.path.join(ROOT, 'tool-results'), exist_ok=True)
    dest = os.path.join(ROOT, 'tool-results', 'dms-table-usage.json')
    json.dump(out, open(dest, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)

    print('%-30s %-8s %-30s %s' % ('表', 'live', '实体', '消费方(Service/Mapper/…)'))
    for o in out:
        refs = sorted({r.split('/')[-1] for rs in o['entity_refs'].values() for r in rs})
        print('%-30s %-8s %-30s %s' % (
            o['table'], o['live'], ','.join(o['entities']) or '(无实体)',
            ','.join(refs[:6]) or ('SQL×%d' % o['sql_ref_count'] if o['sql_ref_count'] else '<<< 无任何消费方')))
    print()
    print('输出: %s' % dest)


if __name__ == '__main__':
    main()
