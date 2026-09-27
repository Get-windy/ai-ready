# -*- coding: utf-8 -*-
"""把一个 Flyway 迁移 .sql 直接应用到 devdb（用于让修复**立即**生效，无需重启后端）。

为什么需要它：Flyway 只在应用启动时执行迁移；本项目常年有实例在跑（且并行会话共用 devdb），
为了不打断别人，修数据类迁移可以先用本脚本"预应用"——迁移本身写成幂等（NOT EXISTS / 条件 UPDATE），
下次启动时 Flyway 再执行一遍不会产生重复数据。

用法：python tools/apply_sql_migration.py <迁移文件相对路径>
"""
import io
import re
import sys

import psycopg2

DSN = "host=localhost port=5432 dbname=devdb user=devuser password=devuser123"


def statements(sql_text):
    """去掉 `--` 行注释后按 `;` 切分（注释里的分号不能当分隔符）。"""
    lines = []
    for line in sql_text.splitlines():
        # 行内 `--` 注释：只在没有被引号包住时截断，够用即止
        idx = line.find("--")
        if idx >= 0:
            line = line[:idx]
        lines.append(line)
    body = "\n".join(lines)
    return [s.strip() for s in body.split(";") if s.strip()]


def main():
    path = sys.argv[1]
    with io.open(path, encoding="utf-8") as f:
        text = f.read()
    stmts = statements(text)
    conn = psycopg2.connect(DSN)
    conn.autocommit = False
    try:
        cur = conn.cursor()
        for i, st in enumerate(stmts, 1):
            cur.execute(st)
            head = re.sub(r"\s+", " ", st)[:70]
            print(f"[{i}] rowcount={cur.rowcount}  <- {head}...")
        conn.commit()
        print(f"OK 共执行 {len(stmts)} 条语句")
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
