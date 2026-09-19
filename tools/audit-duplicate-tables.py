# -*- coding: utf-8 -*-
"""
数据表重复审计（只读）：找出「结构相似 / 名字相似 / 存同类数据」的重复表候选。

判定口径（三路取并集，互为佐证）：
  A. 表名归一化相同：去审计后缀(_bak/_old/_copy/_tmp...)、去模块前缀(erp_/biz_/sys_)、
     去复数尾 s 后同名 —— 如 erp_warehouse / warehouse / warehouse_bak 归一组。
  B. 列结构相似：两表列名集合的 Jaccard 相似度 >= 0.70，且列数 >= 4。
  C. 行数/数据重合：候选组内抽样主键交集（由 audit-duplicate-tables-samples.py 完成）。

引用统计分四类，取决于「哪里引用了这张表」：
  migration   —— Flyway 迁移（建表脚本，不算业务引用）
  java        —— 实体/Mapper 接口/raw SQL 字符串
  mapper_xml  —— MyBatis XML
  frontend    —— 前端源码
  sql_other   —— backend/sql、scripts 等散落脚本

用法: python tools/audit-duplicate-tables.py
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

# 表名归一化：这些后缀只表示「备份/历史/临时」，不表示业务差异
AUDIT_SUFFIX = ('_bak', '_backup', '_old', '_copy', '_tmp', '_temp', '_test', '_new', '_v1', '_v2')
# 这些前缀是模块命名空间，去掉后可能撞名（撞名即候选）
MODULE_PREFIX = ('erp_', 'biz_', 'sys_', 'crm_', 'wms_', 'dms_', 'md_')

# 框架自带的元数据表：不属于业务表，删不得，也不参与重复判定
SYSTEM_TABLES = {'flyway_schema_history', 'databasechangelog', 'databasechangeloglock'}

SKIP_DIRS = {'node_modules', 'target', 'dist', '.git', '__pycache__', '.atcode',
             'test-results', 'playwright-report', 'uploads', 'logs', 'tool-results', 'coverage'}
SCAN_EXT = {'.java', '.xml', '.sql', '.yml', '.yaml', '.ts', '.vue', '.js'}
MAX_FILE_SIZE = 2 * 1024 * 1024

IDENT = re.compile(r'[A-Za-z_][A-Za-z0-9_]*')


def camel(name):
    """order_info -> orderInfo / OrderInfo，两侧都收"""
    parts = [p for p in name.split('_') if p]
    if len(parts) < 2:
        return []
    low = parts[0] + ''.join(p[:1].upper() + p[1:] for p in parts[1:])
    return [low, low[:1].upper() + low[1:]]


def norm_name(name):
    n = name.lower()
    for suf in AUDIT_SUFFIX:
        if n.endswith(suf) and len(n) > len(suf) + 2:
            n = n[: -len(suf)]
            break
    for pre in MODULE_PREFIX:
        if n.startswith(pre) and len(n) > len(pre) + 2:
            n = n[len(pre):]
            break
    if n.endswith('s') and not n.endswith('ss') and len(n) > 4:
        n = n[:-1]
    return n


def load_schema():
    """表 + 列 + 行数 + 注释 + 外键/视图依赖"""
    with psycopg2.connect(**CONN) as conn, conn.cursor() as cur:
        cur.execute("""
            SELECT c.relname,
                   COALESCE(pg_catalog.obj_description(c.oid, 'pg_class'), ''),
                   GREATEST(c.reltuples::bigint, 0),
                   c.relkind
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p')
            ORDER BY c.relname
        """)
        tables = {}
        for name, comment, rows, _kind in cur.fetchall():
            tables[name] = dict(name=name, comment=comment, rows=int(rows), cols=[], refs={}, fk_in=[], fk_out=[], used_by_views=[])

        cur.execute("""
            SELECT table_name, column_name
            FROM information_schema.columns
            WHERE table_schema = 'public'
            ORDER BY table_name, ordinal_position
        """)
        for tname, col in cur.fetchall():
            if tname in tables:
                tables[tname]['cols'].append(col)

        # 谁引用我（子表 -> 父表）
        cur.execute("""
            SELECT tc.table_name AS child, ccu.table_name AS parent
            FROM information_schema.table_constraints tc
            JOIN information_schema.constraint_column_usage ccu
              ON ccu.constraint_name = tc.constraint_name AND ccu.table_schema = tc.table_schema
            WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = 'public'
        """)
        for child, parent in cur.fetchall():
            if parent in tables and child != parent:
                tables[parent]['fk_in'].append(child)
            if child in tables:
                tables[child]['fk_out'].append(parent)

        # 视图 / 物化视图依赖了哪些表
        try:
            cur.execute("""
                SELECT DISTINCT view_name, table_name
                FROM information_schema.view_table_usage
                WHERE view_schema = 'public'
            """)
            for view, tname in cur.fetchall():
                if tname in tables:
                    tables[tname]['used_by_views'].append(view)
        except Exception:
            pass

        # reltuples 只是估算：未 analyze 的表恒为 0，会把有数据的表误判成空表。0 行的逐一精确点算。
        zeros = [t for t, v in tables.items() if v['rows'] == 0]
        for t in zeros:
            n, ok = exact_count(cur, t)
            tables[t]['rows'] = n
            tables[t]['rows_exact'] = ok
        print('精确点算 0 行表: %d 张（其中 %d 张实为有数据）'
              % (len(zeros), sum(1 for t in zeros if tables[t]['rows'] > 0)))
    return tables


def scan_code(tables):
    """扫代码统计表名引用；按引用位置分类"""
    alias = {}
    for t in tables:
        for a in camel(t):
            alias.setdefault(a, t)

    # 词边界匹配：`erp-supplier-portal` 里的 supplier 不算引用（两侧还有 -/_，说明是更大的名字的一部分）
    def build(names):
        return re.compile(r'(?<![A-Za-z0-9_$.-])(%s)(?![A-Za-z0-9_$-])'
                          % '|'.join(re.escape(n) for n in sorted(names, key=len, reverse=True)))

    tbl_re = build(tables.keys())
    camel_re = build(alias.keys()) if alias else None

    def walk(root, kind_default):
        for dirpath, dirnames, filenames in os.walk(root):
            dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
            rel_dir = os.path.relpath(dirpath, ROOT).replace('\\', '/')
            is_migration = 'resources/db/migration' in rel_dir
            is_mapper = '/resources/mapper' in rel_dir
            # resources 根下的 *.sql 是 schema.sql / data.sql 一类初始化种子，不是业务引用
            is_seed_dir = re.search(r'/src/(main|test)/resources$', rel_dir) is not None
            kind = 'migration' if is_migration else kind_default
            for fn in filenames:
                ext = os.path.splitext(fn)[1].lower()
                if ext not in SCAN_EXT:
                    continue
                # 构建文件里的 supplier/sales 是模块名片段，不是表名引用
                if fn.endswith('pom.xml') or '.flattened-pom' in fn:
                    continue
                p = os.path.join(dirpath, fn)
                try:
                    if os.path.getsize(p) > MAX_FILE_SIZE:
                        continue
                    text = open(p, encoding='utf-8', errors='ignore').read()
                except Exception:
                    continue
                if kind == 'frontend':
                    # 前端不直连数据库：命中的表名一律记为同名标识符（`permissions`、`users` 变量）。
                    # 之前拿它做 SQL 上下文判断，被 `import ... from` / 数组 `.join()` 大面积误报。
                    frags, body = [], text
                else:
                    frags = sql_fragments(text, ext)
                    body = '\n'.join(frags)
                hit = set(tbl_re.findall(body))
                if camel_re is not None:
                    hit |= {alias[c] for c in camel_re.findall(body)}
                # 实体注解在全文里（不是字符串），单独补一次
                if ext in ('.java', '.xml'):
                    hit |= {t for t in tbl_re.findall(text) if is_entity(text, t)}
                if not hit:
                    continue
                rel = os.path.relpath(p, ROOT).replace('\\', '/')
                for t in hit:
                    if is_migration:
                        k = 'migration'
                    elif ext == '.sql' and is_seed_dir:
                        k = 'seed'
                    elif is_mapper and ext == '.xml':
                        k = 'mapper_xml'
                    elif is_entity(text, t):
                        k = 'entity'
                    elif kind == 'frontend':
                        k = 'frontend_name'
                    elif in_sql_context(frags, t):
                        k = 'raw_sql'
                    elif in_ddl_context(body, t):
                        k = 'ddl'
                    else:
                        k = 'java_other'
                    tables[t]['refs'].setdefault(k, []).append(rel)

    walk(os.path.join(ROOT, 'backend'), 'java')
    walk(os.path.join(ROOT, 'frontend'), 'frontend')
    return tables


DDL_CTX = r'\b(create\s+table|truncate|alter\s+table|drop\s+table)\s+(if\s+not\s+exists\s+)?%s\b'
SQL_WORDS = re.compile(r'\b(select|insert|update|delete|from|where|join|on|set|values|group\s+by|order\s+by)\b', re.I)


STR_LIT = re.compile(r'"""(?:.|\n)*?"""|"(?:[^"\\\n]|\\.)*"')
XML_COMMENT = re.compile(r'<!--(?:.|\n)*?-->')
SQL_VERBS = {'select', 'insert', 'update', 'delete'}


def sql_fragments(text, ext):
    """取出可能承载 SQL 的片段列表（Java 是字符串字面量列表，XML/SQL 是全文）。"""
    if ext == '.java':
        return STR_LIT.findall(text)
    if ext in ('.xml', '.html'):
        return [XML_COMMENT.sub('', text)]
    return [text]


def has_table(frag, t):
    # 左侧排除 `.`：`p.roles` 是 biz_party 的列名，不是 roles 表（短表名误报的主要来源）
    return re.search(r'(?<![A-Za-z0-9_$.-])%s(?![A-Za-z0-9_$-])' % re.escape(t), frag) is not None


def in_sql_context(frags, t):
    """表名与其 SQL 语义必须在**同一段文本**里。

    Java 的 SQL 常写成 `"SELECT a " + "FROM t " + "WHERE ..."`，所以取相邻 3 个字符串
    拼起来看；不能把整个文件的字符串混在一起统计，否则 `expect(users)` 这类断言
    只要同文件别处有个 select 就被判成引用。
    """
    for i, f in enumerate(frags):
        if not has_table(f, t):
            continue
        chunk = '\n'.join(frags[i:i + 3])
        words = {w.lower() for w in SQL_WORDS.findall(chunk)}
        if len(words) >= 3 and (words & SQL_VERBS):
            return True
    return False


def in_ddl_context(body, t):
    """CREATE TABLE IF NOT EXISTS 也算建表，不算业务引用"""
    return re.search(DDL_CTX % re.escape(t), body, re.I) is not None


def is_entity(text, t):
    """实体注解有两套：MyBatis-Plus 的 @TableName 与 JPA 的 @Table —— 混用，必须都认"""
    e = re.escape(t)
    return bool(re.search(r'@TableName\s*\(\s*(?:value\s*=\s*)?"%s"' % e, text)
                or re.search(r'@Table\s*\(\s*(?:name\s*=\s*)?"%s"' % e, text))


def exact_count(cur, table):
    """reltuples 未 analyze 时为 0，会把有数据的表误判成空表 —— 对 0 行的表精确点一下"""
    cur.execute("SET LOCAL statement_timeout = '8s'")
    try:
        cur.execute('SELECT count(*) FROM public."%s"' % table)
        return cur.fetchone()[0], True
    except Exception:
        cur.connection.rollback()
        return 0, False


def jaccard(a, b):
    if not a or not b:
        return 0.0
    sa, sb = set(a), set(b)
    return len(sa & sb) / float(len(sa | sb))


def main():
    tables = load_schema()
    print('public 表总数: %d' % len(tables))
    scan_code(tables)
    print('代码引用扫描完成')

    # A. 表名归一化分组
    name_groups = collections.defaultdict(list)
    for t in sorted(tables):
        name_groups[norm_name(t)].append(t)
    name_dup = {k: sorted(v) for k, v in name_groups.items() if len(v) >= 2}

    # B. 列结构相似分组（并查集）
    names = sorted(tables)
    parent = {n: n for n in names}

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    def union(a, b):
        ra, rb = find(a), find(b)
        if ra != rb:
            parent[rb] = ra

    pairs = []
    for i, a in enumerate(names):
        ca = tables[a]['cols']
        if len(ca) < 4:
            continue
        for b in names[i + 1:]:
            cb = tables[b]['cols']
            if len(cb) < 4:
                continue
            # 列数相差过大直接跳过
            if min(len(ca), len(cb)) / float(max(len(ca), len(cb))) < 0.6:
                continue
            s = jaccard(ca, cb)
            if s >= 0.70:
                pairs.append((round(s, 3), a, b))
                union(a, b)

    struct_groups = collections.defaultdict(list)
    for n in names:
        struct_groups[find(n)].append(n)
    struct_dup = {k: sorted(v) for k, v in struct_groups.items() if len(v) >= 2}
    pair_map = collections.defaultdict(list)
    for s, a, b in pairs:
        pair_map[find(a)].append((s, a, b))

    # 汇总每张表的引用强度
    KINDS = ('migration', 'seed', 'entity', 'mapper_xml', 'raw_sql', 'ddl',
             'java_other', 'frontend', 'frontend_name', 'sql_other')

    def ref_stat(t):
        r = tables[t]['refs']
        d = {k: len(r.get(k, [])) for k in KINDS}
        d.update(fk_in=len(tables[t]['fk_in']), views=len(tables[t]['used_by_views']))
        return d

    def payload(t):
        r = tables[t]['refs']
        return dict(name=t, norm=norm_name(t), rows=tables[t]['rows'],
                    rows_exact=tables[t].get('rows_exact'),
                    col_count=len(tables[t]['cols']), comment=tables[t]['comment'],
                    cols=sorted(tables[t]['cols']), refs=ref_stat(t),
                    ref_files={k: sorted(set(r.get(k, [])))[:6] for k in KINDS if r.get(k)},
                    fk_in=sorted(set(tables[t]['fk_in'])),
                    fk_out=sorted(set(tables[t]['fk_out'])),
                    used_by_views=sorted(set(tables[t]['used_by_views'])))

    def bucket(t):
        """三档：有查询代码=活表；有实体无查询=未接线；彻底无引用=孤儿。

        刻意不把 java_other / frontend_name 算作引用 —— 它们是同名变量（`users`、`permissions`），
        误报率极高，算进来会把死表洗成「在用」。
        """
        if t in SYSTEM_TABLES:
            return 'system'
        r = tables[t]['refs']
        if r.get('mapper_xml') or r.get('raw_sql') or r.get('frontend'):
            return 'live'
        if r.get('entity'):
            return 'entity_only'
        return 'orphan'

    buckets = collections.defaultdict(list)
    for t in sorted(tables):
        buckets[bucket(t)].append(t)

    out = dict(
        generated_for='devdb.public',
        table_count=len(tables),
        bucket_counts={k: len(v) for k, v in buckets.items()},
        bucket_tables={k: v for k, v in buckets.items()},
        name_duplicate_groups={k: [payload(t) for t in v] for k, v in sorted(name_dup.items())},
        struct_duplicate_groups={k: dict(members=[payload(t) for t in v],
                                         pairs=[dict(sim=s, a=a, b=b) for s, a, b in pair_map.get(k, [])])
                                 for k, v in sorted(struct_dup.items(), key=lambda kv: sorted(kv[1]))},
        tables={t: payload(t) for t in sorted(tables)},
    )
    path = os.path.join(ROOT, 'tools', 'audit-duplicate-tables.json')
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(out, f, ensure_ascii=False, indent=1)

    def tag(t):
        r = tables[t]['refs']
        bits = []
        for k, s in (('entity', '实体'), ('mapper_xml', 'xml'), ('raw_sql', '查询'),
                     ('ddl', '建表'), ('java_other', '同名'), ('sql_other', '脚本'),
                     ('frontend', '前端sql'), ('frontend_name', '前端同名')):
            if r.get(k):
                bits.append('%s%d' % (s, len(set(r[k]))))
        return '%d行[%s]' % (tables[t]['rows'], ','.join(bits) if bits else '无代码引用')

    print()
    print('=== A. 表名归一化后同名的组：%d 组 ===' % len(name_dup))
    for k, v in sorted(name_dup.items()):
        print('  %-24s %s' % (k, '  '.join('%s(%s)' % (t, tag(t)) for t in v)))

    print()
    print('=== B. 列结构相似(Jaccard>=0.70)：%d 组 ===' % len(struct_dup))
    for k, v in sorted(struct_dup.items(), key=lambda kv: -max((s for s, _, _ in pair_map.get(kv[0], [])), default=0)):
        sims = sorted((s for s, _, _ in pair_map.get(k, [])), reverse=True)
        label = '  '.join('%s(%d列,%s)' % (t, len(tables[t]['cols']), tag(t)) for t in v[:6])
        print('  组(最高相似 %.2f, 成员 %d): %s' % (sims[0] if sims else 0, len(v), label))

    print()
    print('=== 引用分档 ===')
    for k, label in (('live', '有查询代码'), ('entity_only', '有实体但无任何查询'), ('orphan', '无实体无查询')):
        names = buckets[k]
        print('  %-22s %3d 张%s' % (label, len(names), '：' + ', '.join(names[:12]) + (' …' if len(names) > 12 else '') if k != 'live' else ''))

    print()
    print('已写入 %s' % os.path.relpath(path, ROOT))


if __name__ == '__main__':
    main()
