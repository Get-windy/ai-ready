import psycopg2
import sys

def execute_sql_file():
    try:
        conn = psycopg2.connect(
            host="localhost",
            port=5432,
            database="devdb",
            user="devuser",
            password="devuser123"
        )
        
        cursor = conn.cursor()
        
        sql_file_path = r"i:\AI-Ready\backend\sql\menu_fix_script.sql"
        
        with open(sql_file_path, 'r', encoding='utf-8') as f:
            sql_content = f.read()
        
        try:
            cursor.execute(sql_content)
            conn.commit()
            print("菜单修复SQL脚本执行完成！")
        except Exception as e:
            print(f"执行SQL失败: {e}")
            conn.rollback()
            sys.exit(1)
        
        cursor.execute("""
            SELECT id, menu_name, menu_code, path, component, parent_id, menu_type, sort
            FROM sys_menu 
            WHERE id IN (2, 200, 201, 202) OR parent_id = 2
            ORDER BY parent_id, sort
        """)
        
        print("\n验证结果:")
        print("ID | 菜单名称 | 菜单代码 | 路径 | 组件 | 父ID | 类型 | 排序")
        print("-" * 100)
        for row in cursor.fetchall():
            print(f"{row[0]} | {row[1]} | {row[2]} | {row[3]} | {row[4]} | {row[5]} | {row[6]} | {row[7]}")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"数据库连接失败: {e}")
        sys.exit(1)

if __name__ == "__main__":
    execute_sql_file()