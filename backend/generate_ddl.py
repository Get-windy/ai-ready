#!/usr/bin/env python3
"""读取 MyBatis-Plus 实体 Java 文件并生成 H2 兼容的 CREATE TABLE IF NOT EXISTS 语句。"""

import re
import os
import sys

ENTITY_DIR = r"i:\AI-Ready\backend\core\base\core-base\src\main\java\cn\aiedge\base\entity"
# Additional entity dirs to scan
EXTRA_DIRS = [
    r"i:\AI-Ready\backend\core\api\core-api\src\main\java\cn\aiedge\notification",
]

TYPE_MAP = {
    'Long': 'BIGINT',
    'long': 'BIGINT',
    'Integer': 'INTEGER',
    'int': 'INTEGER',
    'String': 'VARCHAR(500)',
    'Boolean': 'BOOLEAN',
    'boolean': 'BOOLEAN',
    'BigDecimal': 'DECIMAL(20,2)',
    'LocalDateTime': 'TIMESTAMP',
    'LocalDate': 'DATE',
    'LocalTime': 'TIME',
    'Double': 'DOUBLE',
    'double': 'DOUBLE',
    'Float': 'FLOAT',
    'float': 'FLOAT',
}

def parse_entity(filepath):
    """Parse a MyBatis-Plus entity Java file and return table name and columns."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Get table name
    table_match = re.search(r'@TableName\("(\w+)"\)', content)
    if not table_match:
        print(f"  [SKIP] No @TableName annotation: {os.path.basename(filepath)}", file=sys.stderr)
        return None, None

    table_name = table_match.group(1)
    class_match = re.search(r'class\s+(\w+)', content)
    class_name = class_match.group(1) if class_match else "Unknown"

    # Check if extends BaseEntity
    extends_base = 'extends BaseEntity' in content or 'extends BaseEntity<' in content

    print(f"  [OK] {class_name} -> {table_name} (extends BaseEntity: {extends_base})", file=sys.stderr)

    # Extract fields
    fields = []

    # Remove multi-line comments
    content_no_comments = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)

    # Find field patterns: private Type fieldName;
    # Also handle @TableField annotations
    lines = content_no_comments.split('\n')

    i = 0
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()

        # Check annotations
        is_table_field = False
        table_field_exist = False
        field_fill = None

        if '@TableField' in stripped:
            is_table_field = True
            exist_match = re.search(r'exist\s*=\s*(true|false)', stripped)
            if exist_match and exist_match.group(1) == 'false':
                table_field_exist = True
                while i < len(lines) and not re.search(r'private\s+\S+\s+\w+', lines[i]):
                    i += 1
                continue

        # Match field declaration
        field_match = re.search(r'private\s+(\S+(?:<[^>]*>)?)\s+(\w+)\s*[;=]', stripped)
        if field_match:
            field_type = field_match.group(1)
            field_name = field_match.group(2)

            # Skip serialVersionUID
            if field_name == 'serialVersionUID':
                i += 1
                continue

            # Map type
            sql_type = TYPE_MAP.get(field_type, None)
            if sql_type is None:
                # Try generic type
                base_type = field_type.split('<')[0]
                sql_type = TYPE_MAP.get(base_type, None)
                if sql_type is None:
                    # Could be a List, Set, etc. - skip as it's usually not a DB column
                    i += 1
                    continue

            # Determine if it's a standard base field
            standard_base_fields = {'id', 'createTime', 'updateTime', 'createBy', 'updateBy', 'deleted', 'tenantId'}
            if extends_base and field_name in standard_base_fields:
                i += 1
                continue

            # Convert camelCase to snake_case
            snake_name = re.sub(r'([A-Z])', r'_\1', field_name).lower()
            if snake_name.startswith('_'):
                snake_name = snake_name[1:]

            # Determine NOT NULL
            not_null = False
            default_val = None

            if field_name in ['status', 'deleted', 'sort']:
                default_val = '0'

            fields.append((field_name, snake_name, sql_type, not_null, default_val))

        i += 1

    return table_name, fields, extends_base

def generate_ddl(table_name, fields, extends_base):
    """Generate CREATE TABLE IF NOT EXISTS DDL for H2 (PostgreSQL compatibility)."""
    if not fields and not extends_base:
        return None

    lines = [f"CREATE TABLE IF NOT EXISTS {table_name} ("]
    cols = []

    if extends_base:
        cols.append("    id              BIGINT PRIMARY KEY")
        cols.append("    tenant_id       BIGINT NOT NULL DEFAULT 0")
        cols.append("    deleted         INTEGER NOT NULL DEFAULT 0")
        cols.append("    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
        cols.append("    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
        cols.append("    create_by       BIGINT")
        cols.append("    update_by       BIGINT")

    for field_name, snake_name, sql_type, not_null, default in fields:
        col = f"    {snake_name:20s} {sql_type}"
        if not_null:
            col += " NOT NULL"
        if default is not None:
            col += f" DEFAULT {default}"
        cols.append(col)

    lines.append(",\n".join(cols))
    lines.append(");\n")
    return "\n".join(lines)

def main():
    all_files = []

    # Scan main entity directory
    for f in sorted(os.listdir(ENTITY_DIR)):
        if f.endswith('.java') and f != 'BaseEntity.java':
            all_files.append(os.path.join(ENTITY_DIR, f))

    # Scan extra directories
    for d in EXTRA_DIRS:
        if os.path.exists(d):
            for root, dirs, files in os.walk(d):
                for f in files:
                    if f.endswith('.java'):
                        all_files.append(os.path.join(root, f))

    print(f"Scanning {len(all_files)} entity files...\n", file=sys.stderr)

    ddl_statements = []
    processed = 0

    for fp in all_files:
        try:
            table_name, fields, extends_base = parse_entity(fp)
            if table_name:
                ddl = generate_ddl(table_name, fields, extends_base)
                if ddl:
                    ddl_statements.append((table_name, ddl))
                processed += 1
        except Exception as e:
            print(f"  [ERR] {os.path.basename(fp)}: {e}", file=sys.stderr)

    print(f"\nProcessed {processed} entities. Generating DDL...\n", file=sys.stderr)

    # Print DDL statements grouped
    print("-- ===============================================")
    print("-- 核心框架表 - 由实体自动生成")
    print("-- Generated for H2 (PostgreSQL compatibility mode)")
    print("-- ===============================================\n")

    for table_name, ddl in ddl_statements:
        print(f"-- Table: {table_name}")
        print(ddl)

if __name__ == '__main__':
    main()
