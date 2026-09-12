#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""devdb 快速查询： python tools/dbq2.py "SELECT ..." """
import sys, io, os
import psycopg2

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')


def run(sql, params=None, fetch=True):
    with psycopg2.connect(**CONN) as conn:
        with conn.cursor() as cur:
            # 注意：不能传空 tuple（psycopg2 会对 SQL 做 % 格式化，SQL 里的 LIKE '%%' 会报错）
            if params:
                cur.execute(sql, params)
            else:
                cur.execute(sql)
            if fetch and cur.description:
                cols = [d[0] for d in cur.description]
                rows = cur.fetchall()
                print(' | '.join(cols))
                print('-' * 60)
                for r in rows:
                    print(' | '.join('' if v is None else str(v) for v in r))
                print(f'({len(rows)} rows)')
            else:
                print('affected', cur.rowcount)


if __name__ == '__main__':
    sql = sys.argv[1] if len(sys.argv) > 1 else 'SELECT 1'
    run(sql)
