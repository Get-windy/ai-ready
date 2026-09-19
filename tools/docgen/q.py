import sys, psycopg2, json
conn = psycopg2.connect(host='localhost', port=5432, user='devuser', password='devuser123', dbname='devdb')
cur = conn.cursor()
sql = sys.stdin.read()
cur.execute(sql)
cols = [d[0] for d in cur.description] if cur.description else []
rows = cur.fetchall()
print('\t'.join(cols))
for r in rows:
    print('\t'.join('' if v is None else str(v) for v in r))
