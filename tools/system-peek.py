# -*- coding: utf-8 -*-
"""按 key / 条件查看 tools/system-db.json 的片段。用法:
  python tools/system-peek.py menu_tree
  python tools/system-peek.py menu_tree 62001
  python tools/system-peek.py perm_sys tenant
"""
import json, io, sys

sys.stdout.reconfigure(encoding="utf-8")
D = json.load(io.open(r"I:\AI-Ready\tools\system-db.json", encoding="utf-8"))

key = sys.argv[1]
rows = D.get(key, [])
filt = sys.argv[2] if len(sys.argv) > 2 else None
if isinstance(rows, dict):
    print(json.dumps(rows, ensure_ascii=False)); sys.exit()
if filt:
    rows = [r for r in rows if any(filt in str(v) for v in r.values())]
print("### %s (%d)" % (key, len(rows)))
if rows:
    cols = list(rows[0].keys())
    if len(cols) > 6:
        for r in rows:
            print(" | ".join(str(r.get(c)) for c in cols[:6]) + " || " + " | ".join(str(r.get(c)) for c in cols[6:]))
    else:
        print(" | ".join(cols))
        for r in rows:
            print(" | ".join(str(r.get(c)) for c in cols))
