"""
扫描所有 Java 实体类，为数据库中缺失的表生成 CREATE TABLE DDL。
输出到 stdout，直接作为 Flyway 迁移文件内容。
"""
import os, re, glob, sys
from collections import OrderedDict

BACKEND = r"i:\AI-Ready\backend"

# Java 类型 → PostgreSQL 类型映射
TYPE_MAP = {
    'Long': 'BIGINT',
    'long': 'BIGINT',
    'Integer': 'INTEGER',
    'int': 'INTEGER',
    'Short': 'SMALLINT',
    'short': 'SMALLINT',
    'Double': 'DOUBLE PRECISION',
    'double': 'DOUBLE PRECISION',
    'Float': 'REAL',
    'float': 'REAL',
    'Boolean': 'BOOLEAN',
    'boolean': 'BOOLEAN',
    'String': 'VARCHAR',
    'BigDecimal': 'NUMERIC(18,4)',
    'LocalDateTime': 'TIMESTAMP',
    'LocalDate': 'DATE',
    'LocalTime': 'TIME',
    'Date': 'TIMESTAMP',
    'byte[]': 'BYTEA',
}

def parse_entity(filepath):
    """解析一个实体类文件，返回 (table_name, fields, id_type, has_logic_delete)"""
    with open(filepath, encoding='utf-8') as f:
        content = f.read()

    # 提取表名
    m = re.search(r'@TableName\("([^"]+)"\)', content)
    if not m:
        return None
    table_name = m.group(1)

    # 提取类体（去掉方法，只保留字段）
    # 先找类声明后的第一个 {
    class_match = re.search(r'class\s+\w+[^{]*\{', content)
    if not class_match:
        return None
    body_start = class_match.end()

    # 提取字段声明（简化：以 ; 结尾，有类型，不含 ( )）
    fields = []
    has_logic_delete = False

    # 找所有字段声明
    # 匹配模式：可选注解 + 类型 + 字段名 + 可选 ;
    field_pattern = re.compile(
        r'((?:\s*@\w+(?:\([^)]*\))?\s*)*)'  # 注解们
        r'\s*(private|protected)\s+'           # 访问修饰符
        r'([\w<>\[\],\s?]+?)\s+'              # 类型
        r'(\w+)\s*;',                          # 字段名
        re.MULTILINE
    )

    for fm in field_pattern.finditer(content[body_start:]):
        annotations = fm.group(1)
        field_type = fm.group(3).strip()
        field_name = fm.group(4).strip()

        # 跳过 serialVersionUID
        if field_name == 'serialVersionUID':
            continue

        # 处理泛型（如 List<String>）→ 存为 TEXT
        if '<' in field_type:
            pg_type = 'TEXT'
        else:
            pg_type = TYPE_MAP.get(field_type, 'VARCHAR(500)')

        # 注解分析
        is_id = '@TableId' in annotations
        is_logic = '@TableLogic' in annotations
        is_fill_insert = 'FieldFill.INSERT' in annotations
        is_fill_both = 'FieldFill.INSERT_UPDATE' in annotations

        # ID 类型
        id_type = None
        if is_id:
            m2 = re.search(r'type\s*=\s*IdType\.(\w+)', annotations)
            if m2:
                id_type = m2.group(1)
            else:
                id_type = 'NONE'

        if is_logic:
            has_logic_delete = True

        # 默认值
        default_val = None
        dm = re.search(r"defaultValue\s*=\s*\"([^\"]*)\"", annotations)
        if dm:
            default_val = dm.group(1)

        # nullable（@TableField 或基本类型）
        nullable = True
        if field_type in ('long', 'int', 'double', 'float', 'boolean', 'short'):
            nullable = False
        if is_id and id_type in ('AUTO',):
            nullable = False

        # VARCHAR 长度
        varchar_len = 500
        lm = re.search(r'length\s*=\s*(\d+)', annotations)
        if lm:
            varchar_len = int(lm.group(1))
        elif field_type == 'String':
            # 根据字段名猜测合理长度
            name_lower = field_name.lower()
            if 'name' in name_lower or 'title' in name_lower:
                varchar_len = 200
            elif 'code' in name_lower or 'no' in name_lower or 'type' in name_lower:
                varchar_len = 50
            elif 'url' in name_lower or 'path' in name_lower:
                varchar_len = 1000
            elif 'remark' in name_lower or 'desc' in name_lower or 'content' in name_lower or 'address' in name_lower:
                varchar_len = 2000
            elif 'status' in name_lower or 'level' in name_lower:
                varchar_len = 20
            elif 'phone' in name_lower or 'mobile' in name_lower:
                varchar_len = 30
            elif 'email' in name_lower:
                varchar_len = 100

        if pg_type == 'VARCHAR':
            pg_type = f'VARCHAR({varchar_len})'

        fields.append({
            'name': field_name,
            'java_type': field_type,
            'pg_type': pg_type,
            'nullable': nullable,
            'is_id': is_id,
            'id_type': id_type,
            'is_logic': is_logic,
            'default_val': default_val,
            'annotations': annotations,
        })

    return table_name, fields, has_logic_delete


def to_snake(name):
    """camelCase → snake_case"""
    s1 = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', name)
    return re.sub('([a-z0-9])([A-Z])', r'\1_\2', s1).lower()


def generate_ddl(table_name, fields, has_logic_delete):
    """生成 CREATE TABLE 语句"""
    lines = []
    lines.append(f"CREATE TABLE IF NOT EXISTS {table_name} (")

    col_lines = []
    pk_col = None
    pk_auto = False

    for f in fields:
        col_name = to_snake(f['name'])
        col_type = f['pg_type']
        nullable = f['nullable']

        if f['is_id']:
            pk_col = col_name
            if f['id_type'] == 'AUTO':
                col_type = 'BIGSERIAL'
                pk_auto = True
                nullable = False
            elif f['id_type'] == 'ASSIGN_ID':
                col_type = 'BIGINT'
                nullable = False
            else:
                nullable = False

        parts = [f"    {col_name}"]
        if pk_auto:
            parts.append(col_type)
        else:
            parts.append(col_type)

        if not nullable:
            parts.append("NOT NULL")

        if f['default_val'] is not None:
            dv = f['default_val']
            if f['pg_type'] in ('INTEGER', 'BIGINT', 'SMALLINT', 'BOOLEAN', 'REAL', 'DOUBLE PRECISION', 'NUMERIC(18,4)'):
                parts.append(f"DEFAULT {dv}")
            elif dv.lower() in ('true', 'false'):
                parts.append(f"DEFAULT {dv}")
            else:
                parts.append(f"DEFAULT '{dv}'")
        elif f['is_logic']:
            parts.append("DEFAULT 0")

        # 注释（从字段名推断）
        col_lines.append(" ".join(parts))

    if pk_col:
        col_lines.append(f"    PRIMARY KEY ({pk_col})")

    lines.append(",\n".join(col_lines))
    lines.append(f") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;")
    return "\n".join(lines)


def generate_pg_ddl(table_name, fields, has_logic_delete, table_comment=None):
    """生成 PostgreSQL CREATE TABLE 语句"""
    lines = []
    lines.append(f"CREATE TABLE IF NOT EXISTS {table_name} (")

    col_lines = []
    pk_col = None
    pk_auto = False
    comments = []

    for f in fields:
        col_name = to_snake(f['name'])
        col_type = f['pg_type']
        nullable = f['nullable']

        if f['is_id']:
            pk_col = col_name
            if f['id_type'] == 'AUTO':
                col_type = 'BIGSERIAL'
                pk_auto = True
                nullable = False
            elif f['id_type'] == 'ASSIGN_ID':
                col_type = 'BIGINT'
                nullable = False

        parts = [f"    {col_name}"]
        if pk_auto:
            parts.append(col_type)
        else:
            parts.append(col_type)

        if not nullable and not f['is_id']:
            parts.append("NOT NULL")

        if f['default_val'] is not None and not pk_auto:
            dv = f['default_val']
            if col_type in ('INTEGER', 'BIGINT', 'SMALLINT', 'BOOLEAN', 'REAL', 'DOUBLE PRECISION') or col_type.startswith('NUMERIC'):
                parts.append(f"DEFAULT {dv}")
            elif dv.lower() in ('true', 'false'):
                parts.append(f"DEFAULT {dv}")
            else:
                parts.append(f"DEFAULT '{dv}'")
        elif f['is_logic']:
            parts.append("DEFAULT 0")

        # 字段注释
        comment_name = f['name']
        comments.append((col_name, comment_name))

        col_lines.append(" ".join(parts))

    if pk_col:
        col_lines.append(f"    PRIMARY KEY ({pk_col})")

    lines.append(",\n".join(col_lines))
    lines.append(");")

    # 表和字段注释
    if table_comment:
        lines.append(f"COMMENT ON TABLE {table_name} IS '{table_comment}';")

    return "\n".join(lines)


def scan_all_entities():
    """扫描所有实体类，返回 {table_name: (fields, has_logic_delete, filepath)}"""
    result = {}
    for root, dirs, files in os.walk(BACKEND):
        if 'target' in root or '.git' in root:
            continue
        for f in files:
            if not f.endswith('.java'):
                continue
            path = os.path.join(root, f)
            try:
                parsed = parse_entity(path)
                if parsed:
                    table_name, fields, has_logic_delete = parsed
                    if fields:
                        result[table_name] = (fields, has_logic_delete, path)
            except Exception as e:
                print(f"WARN: 解析失败 {path}: {e}", file=sys.stderr)
    return result


if __name__ == '__main__':
    # 1. 获取数据库中已有的表
    import psycopg2
    conn = psycopg2.connect(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')
    cur = conn.cursor()
    cur.execute("SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'")
    db_tables = set(r[0] for r in cur.fetchall())
    conn.close()

    # 2. 扫描所有实体
    all_entities = scan_all_entities()
    print(f"-- 扫描到 {len(all_entities)} 个实体，数据库已有 {len(db_tables)} 张表", file=sys.stderr)

    missing = {k: v for k, v in all_entities.items() if k not in db_tables}
    print(f"-- 缺失表: {len(missing)} 张", file=sys.stderr)

    # 3. 输出 DDL
    print(f"-- ============================================================")
    print(f"-- V9.32.0 补齐所有缺失表（共 {len(missing)} 张）")
    print(f"-- 自动生成自 Java 实体类 @TableName 定义")
    print(f"-- ============================================================")
    print()

    for table_name in sorted(missing.keys()):
        fields, has_logic_delete, filepath = missing[table_name]
        # 从文件路径推断模块/注释
        rel = filepath.replace(BACKEND, '').replace('\\', '/')
        print(f"-- 来源: {rel}")
        print(generate_pg_ddl(table_name, fields, has_logic_delete))
        print()
