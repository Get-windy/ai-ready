# -*- coding: utf-8 -*-
"""
资料模块数据库规范盘点（只读）：
  1. 资料模块实体（@TableName）↔ 表结构逐列对账（缺列/多列）
  2. 每张表是否具备 tenant_id / deleted（租户隔离与逻辑删除的前提）
  3. 表是否在 IGNORE_TENANT_TABLES（多租户忽略清单）里
  4. 行数（区分「有数据」与「从未使用」）
输出: tool-results/md-audit/db-audit.json
用法: python tools/audit-md-db.py
"""
import os
import re
import io
import sys
import json

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BE = os.path.join(ROOT, 'backend')
CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')

# 资料模块的表名前缀（含跨模块共用的主数据表）
PREFIXES = ('erp_product', 'erp_partner', 'erp_md_', 'erp_warehouse', 'erp_batch',
            'erp_route', 'finance_account', 'erp_payment_method', 'erp_payment_channel',
            'party', 'wms_location', 'erp_linked_account', 'erp_product_price',
            'erp_expense_type', 'erp_other_income', 'biz_party')

TABLENAME = re.compile(r'@TableName\s*\(\s*(?:value\s*=\s*)?["\']([^"\']+)["\']')
FIELDNAME = re.compile(r'@(?:TableField|TableId)\s*\(\s*(?:value\s*=\s*)?["\']([^"\']+)["\']')


def scan_entities():
    """返回 {表名: 真实持久化列集合}

    ⚠️ 两个必须处理的误报源（2026-09-24 首版踩到）：
      1. `@TableField(exist = false)` 的 VO/透出字段不是表列，必须跳过；
      2. 实体类可以不写 `@TableName`（MyBatis-Plus 按类名驼峰转下划线推导表名），
         首版因此把 erp_partner/party 等误判为「无实体类」。
    """
    ents = {}
    for dirpath, _, files in os.walk(BE):
        if os.sep + 'target' + os.sep in dirpath + os.sep:
            continue
        for f in files:
            if not f.endswith('.java'):
                continue
            fp = os.path.join(dirpath, f)
            src = open(fp, encoding='utf-8', errors='ignore').read()
            cls_m = re.search(r'(?:public\s+)?(?:abstract\s+)?class\s+(\w+)', src)
            if not cls_m:
                continue
            m = TABLENAME.search(src)
            if m:
                table = m.group(1)
            else:
                # 仅当类上有 MyBatis-Plus 实体特征时才按类名推导
                if not re.search(r'@Table(Id|Field|Name)\b', src):
                    continue
                cls = cls_m.group(1)
                base = re.sub(r'(Entity|DO|PO)$', '', cls)
                table = re.sub(r'(?<!^)(?=[A-Z])', '_', base).lower()
            lines = src.split('\n')
            cols = set()
            for i, ln in enumerate(lines):
                fm = re.match(r'\s*private\s+[\w<>\[\],\.\$\s]+\s+(\w+)\s*[;=]', ln)
                if not fm:
                    continue
                name = fm.group(1)
                # 往上找该字段的注解块（连续注解/注释行）
                block = []
                j = i - 1
                while j >= 0 and (lines[j].strip().startswith('@') or lines[j].strip().startswith('//')
                                  or lines[j].strip().startswith('*') or lines[j].strip().startswith('/*')
                                  or lines[j].strip() == ''):
                    block.append(lines[j])
                    j -= 1
                block_src = ' '.join(block)
                if re.search(r'exist\s*=\s*false', block_src):
                    continue
                tf = FIELDNAME.search(block_src)
                if tf:
                    cols.add(tf.group(1))
                elif re.search(r'@Table(Field|Id)', block_src):
                    cols.add(re.sub(r'(?<!^)(?=[A-Z])', '_', name).lower())
                else:
                    cols.add(re.sub(r'(?<!^)(?=[A-Z])', '_', name).lower())
            ents.setdefault(table, set()).update(cols)
    return ents


def main():
    import psycopg2
    ents = scan_entities()
    md_tables = sorted(t for t in ents if t.startswith(PREFIXES))
    # 库里实际存在的匹配表（可能有无实体的表）
    with psycopg2.connect(**CONN) as conn:
        with conn.cursor() as cur:
            cur.execute("""SELECT table_name FROM information_schema.tables
                           WHERE table_schema='public' ORDER BY table_name""")
            db_tables = [r[0] for r in cur.fetchall()]
            cur.execute("""SELECT table_name, column_name FROM information_schema.columns
                           WHERE table_schema='public' ORDER BY table_name, ordinal_position""")
            db_cols = {}
            for t, c in cur.fetchall():
                db_cols.setdefault(t, []).append(c)

    md_db_tables = sorted(t for t in db_tables if t.startswith(PREFIXES))
    only_db = sorted(set(md_db_tables) - set(ents))
    no_entity = [t for t in md_db_tables if t not in ents]

    # IGNORE_TENANT_TABLES
    ignore = set()
    for dirpath, _, files in os.walk(BE):
        if os.sep + 'target' + os.sep in dirpath + os.sep:
            continue
        for f in files:
            if not f.endswith(('.java', '.yml', '.yaml', '.properties')):
                continue
            src = open(os.path.join(dirpath, f), encoding='utf-8', errors='ignore').read()
            if 'IGNORE_TENANT_TABLES' in src or 'ignoreTenantTables' in src or 'tenant-ignore' in src:
                for t in re.findall(r'["\']([a-z_]{4,})["\']', src):
                    ignore.add(t)

    rows = []
    with psycopg2.connect(**CONN) as conn:
        with conn.cursor() as cur:
            for t in md_db_tables:
                cols = db_cols.get(t, [])
                n = None
                try:
                    cur.execute('SELECT count(*) FROM "%s"' % t)
                    n = cur.fetchone()[0]
                except Exception:
                    cur.rollback()
                ent_cols = ents.get(t, set())
                missing_in_db = sorted(c for c in ent_cols if c not in cols)
                extra_in_db = sorted(c for c in cols if c not in ent_cols)
                rows.append({
                    'table': t, 'rows': n,
                    'has_tenant': 'tenant_id' in cols,
                    'has_deleted': 'deleted' in cols,
                    'in_ignore_list': t in ignore,
                    'entity_cols': len(ent_cols), 'db_cols': len(cols),
                    'missing_in_db': missing_in_db, 'extra_in_db': extra_in_db,
                    'no_entity': t not in ents,
                })

    print('资料模块相关表: 库中 %d 张，其中有实体类 %d 张，无实体类 %d 张' % (
        len(rows), len([r for r in rows if not r['no_entity']]), len(rows) - len([r for r in rows if not r['no_entity']])))
    print()
    print('%-42s %8s %6s %6s %7s %s' % ('表名', '行数', '租户列', '删除列', '忽略清单', '备注'))
    print('-' * 118)
    for r in sorted(rows, key=lambda x: x['table']):
        notes = []
        if r['no_entity']:
            notes.append('无实体类')
        if r['missing_in_db']:
            notes.append('实体有库中无列: %s' % ','.join(r['missing_in_db'][:6]))
        if r['rows'] == 0:
            notes.append('空表')
        if not r['has_tenant']:
            notes.append('**无 tenant_id**')
        if not r['has_deleted']:
            notes.append('**无 deleted**')
        if r['in_ignore_list']:
            notes.append('在忽略清单')
        print('%-42s %8s %6s %6s %7s %s' % (
            r['table'], r['rows'], 'Y' if r['has_tenant'] else 'N',
            'Y' if r['has_deleted'] else 'N', 'Y' if r['in_ignore_list'] else '-', '; '.join(notes)))

    print()
    print('=== 缺 tenant_id 或 deleted 的表 ===')
    for r in rows:
        if not r['has_tenant'] or not r['has_deleted']:
            print('  %-42s tenant=%s deleted=%s rows=%s' % (
                r['table'], r['has_tenant'], r['has_deleted'], r['rows']))
    print()
    print('=== 库中有表但无实体类（僵尸表候选 / 或由别处动态访问）===')
    for t in no_entity:
        print('  %s' % t)

    os.makedirs(os.path.join(ROOT, 'tool-results/md-audit'), exist_ok=True)
    json.dump({'tables': rows, 'no_entity': no_entity, 'ignore_list': sorted(ignore)},
              open(os.path.join(ROOT, 'tool-results/md-audit/db-audit.json'), 'w', encoding='utf-8'),
              ensure_ascii=False, indent=1)
    print()
    print('输出: tool-results/md-audit/db-audit.json')


if __name__ == '__main__':
    main()
