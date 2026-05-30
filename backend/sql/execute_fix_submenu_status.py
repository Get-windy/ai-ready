import psycopg2

def fix_submenu_status():
    try:
        conn = psycopg2.connect(
            host="localhost",
            port=5432,
            database="devdb",
            user="devuser",
            password="devuser123"
        )
        
        cursor = conn.cursor()
        
        sql_file_path = r"i:\AI-Ready\backend\sql\fix_submenu_status.sql"
        
        with open(sql_file_path, 'r', encoding='utf-8') as f:
            sql_content = f.read()
        
        try:
            cursor.execute(sql_content)
            conn.commit()
            print("子菜单status字段修复完成！")
        except Exception as e:
            print(f"执行SQL失败: {e}")
            conn.rollback()
        
        cursor.execute("""
            SELECT id, menu_name, menu_code, path, parent_id, menu_type, visible, status, sort
            FROM sys_menu 
            WHERE id = 2 OR parent_id = 2
            ORDER BY parent_id, sort
        """)
        
        print("\n验证结果:")
        print("ID | 菜单名称 | 菜单代码 | 路径 | 父ID | 类型 | 可见 | 状态 | 排序")
        print("-" * 100)
        for row in cursor.fetchall():
            print(f"{row[0]} | {row[1]} | {row[2]} | {row[3]} | {row[4]} | {row[5]} | {row[6]} | {row[7]} | {row[8]}")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"数据库连接失败: {e}")

if __name__ == "__main__":
    fix_submenu_status()