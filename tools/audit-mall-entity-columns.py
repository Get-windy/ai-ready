# -*- coding: utf-8 -*-
"""
商城模块「实体字段 ↔ 真表列」一致性扫描（只读）。

为什么需要它：2026-09-26 修 C 端时连续踩了三次同一类坑 ——
  mall_address 映射了 6 个不存在的列（customer_id/province/...）、
  接着是 BaseEntity 带来的 create_by、然后是 is_default 类型不匹配、再是 mall_cart 的
  customer_id/subtotal/checked。**每修一处、真机再暴露一处**，每轮要等一次全量构建。
本脚本把这一整类问题一次扫完：拿 Java 实体（含继承的父类字段）推出的列清单，
去比对 information_schema.columns，报告「实体有、表没有」与「表有、实体没有（非致命）」。

规则：
  · @TableName("t")            → 目标表
  · @TableField("col")         → 列名
  · @TableField(exist = false) → 跳过（派生字段）
  · 其余字段名按驼峰转下划线
  · 继承 BaseEntity 的额外带上 id/tenant_id/create_by/create_time/update_by/update_time/deleted

用法: python tools/audit-mall-entity-columns.py [模块目录，默认 backend/erp/erp-mall]
"""
import io
import os
import re
import sys

import psycopg2

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DSN = "host=localhost port=5432 dbname=devdb user=devuser password=devuser123"

BASE_ENTITY_COLS = {
    'id', 'tenant_id', 'create_by', 'create_time', 'update_by', 'update_time', 'deleted'
}


def snake(name):
    return re.sub(r'(?<!^)(?=[A-Z])', '_', name).lower()


def scan_entity(path):
    src = io.open(path, encoding='utf-8', errors='replace').read()
    m = re.search(r'@TableName\(\s*"([^"]+)"\s*\)', src)
    if not m:
        return None
    table = m.group(1)
    body = src
    cols = set()
    # 逐个字段块：抓 @TableField(...) + 紧跟的 private 声明
    for fm in re.finditer(
            r'((?:@\w+(?:\([^)]*\))?\s*)*)private\s+[\w<>\[\].,\s]+\s+(\w+)\s*(?:=[^;]*)?;', body):
        anns, field = fm.group(1) or '', fm.group(2)
        # static 字段不是列（serialVersionUID 会被驼峰转换成一个假列名）
        if field == 'serialVersionUID' or re.search(r'static', fm.group(0)):
            continue
        if re.search(r'@TableField\([^)]*exist\s*=\s*false', anns):
            continue
        tf = re.search(r'@TableField\(\s*(?:value\s*=\s*)?"([^"]+)"', anns)
        if tf:
            cols.add(tf.group(1))
        else:
            cols.add(snake(field))
    if re.search(r'extends\s+BaseEntity', src):
        cols |= BASE_ENTITY_COLS
    return table, cols


def main():
    base = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, 'backend', 'erp', 'erp-mall')
    ent_dir = None
    for dp, dn, fn in os.walk(base):
        if dp.replace('\\', '/').endswith('/model'):
            ent_dir = dp
            break
    if not ent_dir:
        print('未找到 model 目录', file=sys.stderr)
        return 1

    conn = psycopg2.connect(DSN)
    cur = conn.cursor()
    cur.execute("SELECT table_name, column_name FROM information_schema.columns WHERE table_schema='public'")
    real = {}
    for t, c in cur.fetchall():
        real.setdefault(t, set()).add(c)

    print(f'扫描实体目录：{os.path.relpath(ent_dir, ROOT)}\n')
    bad = 0
    for fn in sorted(os.listdir(ent_dir)):
        if not fn.endswith('.java') or fn.startswith('Base'):
            continue
        r = scan_entity(os.path.join(ent_dir, fn))
        if not r:
            continue
        table, cols = r
        if table not in real:
            print(f'✘ {fn:<34} 表 {table} 不存在')
            bad += 1
            continue
        missing = sorted(c for c in cols if c not in real[table])
        if missing:
            bad += 1
            print(f'✘ {fn:<34} {table}: 实体声明了表里没有的列 → {missing}')
        else:
            extra = sorted(c for c in real[table] if c not in cols and c not in ('create_by', 'update_by'))
            note = f'（表另有未映射列：{extra}）' if extra else ''
            print(f'✔ {fn:<34} {table}: {len(cols)} 列全部存在 {note}')

    print()
    if bad:
        print(f'⚠️ {bad} 个实体与表结构不一致 —— 这类不一致会让**整个接口 500**（MP 把列名拼进 SQL）')
    else:
        print('✅ 全部实体与真表结构一致')
    cur.close()
    conn.close()
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main())
