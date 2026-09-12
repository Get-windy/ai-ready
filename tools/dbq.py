# -*- coding: utf-8 -*-
"""devdb 简易查询工具: python tools/dbq.py "SELECT ..." """
import sys, json
import psycopg2

DSN = "host=localhost port=5432 dbname=devdb user=devuser password=devuser123"

def run(sql):
    conn = psycopg2.connect(DSN)
    cur = conn.cursor()
    cur.execute(sql)
    if cur.description:
        cols = [d[0] for d in cur.description]
        rows = cur.fetchall()
        out = [dict(zip(cols, [str(v) if v is not None else None for v in r])) for r in rows]
        print(json.dumps(out, ensure_ascii=False, indent=1))
    else:
        conn.commit()
        print("OK", cur.rowcount)
    cur.close(); conn.close()

if __name__ == "__main__":
    run(sys.argv[1])
