# -*- coding: utf-8 -*-
"""
重复表候选的结构/数据比对（只读）：把 audit-duplicate-tables.py 找出的候选组，
逐组看「列差在哪、数据是否同一批」，用于裁定「完全重复 / 部分重复 / 看似重复实则不同」。

判定辅助：
  - 列集合差（A 独有 / B 独有）—— 独有列越多，越可能是不同业务而非重复
  - 主键交集 —— 两表都非空且 id 有交集，说明确实在存同一批数据
  - 最后写入时间 —— 长期不写的表偏向废弃
  - 唯一索引/外键 —— 被引用方不能直接删

用法: python tools/audit-duplicate-tables-diff.py
"""
import os
import re
import sys
import io
import json
import collections

import psycopg2

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')

# 业务审计字段：两表都有是常态，不算「业务字段重合」的证据
AUDIT_COLS = {'id', 'tenant_id', 'create_time', 'update_time', 'create_by', 'update_by',
              'deleted', 'remark', 'version', 'is_deleted', 'created_at', 'updated_at'}

# 用于判断「是否同一批数据」的业务键候选。刻意不用 id：自增 id 必然重叠，是伪证据。
KEY_COLS = ('code', 'config_key', 'param_key', 'key', 'name', 'item_name', 'grade_name',
            'warehouse_name', 'supplier_name', 'customer_name', 'username', 'login_name',
            'real_name', 'title', 'no', 'phone', 'mobile', 'email')


def main():
    src = os.path.join(ROOT, 'tools', 'audit-duplicate-tables.json')
    data = json.load(open(src, encoding='utf-8'))
    tables = data['tables']

    groups = []
    for key, v in data['name_duplicate_groups'].items():
        groups.append(('表名同类: ' + key, [m['name'] for m in v], 'name'))
    for key, v in data['struct_duplicate_groups'].items():
        members = [m['name'] for m in v['members']]
        # 已由表名组覆盖的不重复列
        if any(sorted(members) == sorted(g[1]) for g in groups):
            continue
        groups.append(('结构相似: ' + key, members, 'struct'))

    out = []
    with psycopg2.connect(**CONN) as conn, conn.cursor() as cur:
        cur.execute("SET statement_timeout = '15s'")
        for title, members, kind in groups:
            rec = dict(title=title, kind=kind, members=[])
            keyvals = collections.defaultdict(dict)
            for t in members:
                info = dict(name=t, rows=tables[t]['rows'], col_count=tables[t]['col_count'],
                            comment=tables[t].get('comment', ''),
                            refs=tables[t]['refs'], fk_in=tables[t].get('fk_in', []))
                # 精确行数 + 主键集合（样本上限 500，够判交集）
                try:
                    cur.execute('SELECT count(*) FROM public."%s"' % t)
                    info['rows'] = cur.fetchone()[0]
                except Exception:
                    conn.rollback()
                    info['rows'] = None
                # 业务键取样：判断两表是否在存同一批数据
                cols = set(tables[t]['cols'])
                for kc in KEY_COLS:
                    if kc not in cols or not info['rows']:
                        continue
                    try:
                        cur.execute('SELECT DISTINCT "%s"::text FROM public."%s" '
                                    'WHERE "%s" IS NOT NULL LIMIT 500' % (kc, t, kc))
                        keyvals[t][kc] = {r[0] for r in cur.fetchall()}
                    except Exception:
                        conn.rollback()
                rec['members'].append(info)

            # 列差 + 业务键交集
            colsets = {t: set(tables[t]['cols']) for t in members}
            allkv = {t: set().union(*keyvals[t].values()) if keyvals[t] else set() for t in members}
            for i, a in enumerate(members):
                for b in members[i + 1:]:
                    both_alive = all((m['rows'] or 0) > 0 for m in rec['members'] if m['name'] in (a, b))
                    shared_keys = [kc for kc in KEY_COLS if kc in keyvals[a] and kc in keyvals[b]]
                    if both_alive and shared_keys:
                        kc = shared_keys[0]
                        inter = keyvals[a][kc] & keyvals[b][kc]
                        same_data, key_col, inter_n = bool(inter), kc, len(inter)
                    elif both_alive and allkv[a] and allkv[b]:
                        # 列名不同但语义相同（config_key vs param_key）：把所有业务键值合并后再比
                        inter = allkv[a] & allkv[b]
                        same_data, key_col, inter_n = bool(inter), '跨列合并', len(inter)
                    else:
                        same_data, key_col, inter_n = None, None, None
                    rec.setdefault('pairs', []).append(dict(
                        a=a, b=b,
                        only_a=sorted(colsets[a] - colsets[b]),
                        only_b=sorted(colsets[b] - colsets[a]),
                        business_overlap=sorted((colsets[a] & colsets[b]) - AUDIT_COLS),
                        key_col=key_col,
                        key_intersection=inter_n,
                        same_data=same_data,
                    ))
            out.append(rec)

    md = ['# 重复表候选：结构与数据比对', '',
          '> 由 `tools/audit-duplicate-tables-diff.py` 生成；数据来源 devdb.public。',
          '> 「存同类数据」按业务键（name/code/key 等，采样上限 500）比对；',
          '> 刻意**不用 id**——自增 id 必然重叠，是伪证据。', '']
    for rec in out:
        md.append('## %s' % rec['title'])
        md.append('')
        md.append('| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |')
        md.append('|---|---|---|---|---|---|---|---|')
        for m in rec['members']:
            r = m['refs']
            md.append('| `%s` | %s | %s | %s | %d | %d | %d | %s |' % (
                m['name'], m['rows'], m['col_count'], (m['comment'] or '')[:24],
                r.get('entity', 0), r.get('mapper_xml', 0), r.get('raw_sql', 0),
                ','.join(m['fk_in'][:2]) or '-'))
        md.append('')
        for p in rec.get('pairs', []):
            diff_a = ', '.join(p['only_a'][:8]) or '（无）'
            diff_b = ', '.join(p['only_b'][:8]) or '（无）'
            same = {True: '**是**', False: '否', None: '无法判定（有一方为空 / 无共有的业务键列）'}[p['same_data']]
            md.append('- `%s` vs `%s`：存同类数据 = %s%s' % (
                p['a'], p['b'], same,
                '（按 `%s` 比对，交集 %d 条）' % (p['key_col'], p['key_intersection'])
                if p['key_col'] else ''))
            md.append('  - 共有业务列 %d 个，`%s` 独有：%s' % (len(p['business_overlap']), p['a'], diff_a))
            md.append('  - `%s` 独有：%s' % (p['b'], diff_b))
        md.append('')

    path = os.path.join(ROOT, 'tools', 'audit-duplicate-tables-diff.md')
    with open(path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(md))
    print('已写入 %s（%d 组）' % (os.path.relpath(path, ROOT), len(out)))

    for rec in out:
        print()
        print('## %s' % rec['title'])
        for m in rec['members']:
            print('   %-42s %s行 %s列 实体%d/查询%d  %s' % (
                m['name'], m['rows'], m['col_count'],
                m['refs'].get('entity', 0), m['refs'].get('raw_sql', 0), m['comment'][:28]))
        for p in rec.get('pairs', []):
            if p['same_data']:
                print('   -> %s 与 %s 按 %s 有交集 %d 条（同一批数据）'
                      % (p['a'], p['b'], p['key_col'], p['key_intersection']))


if __name__ == '__main__':
    main()
