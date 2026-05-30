import psycopg2

def check_current_menu():
    try:
        conn = psycopg2.connect(
            host="localhost",
            port=5432,
            database="devdb",
            user="devuser",
            password="devuser123"
        )
        
        cursor = conn.cursor()
        
        cursor.execute("""
            SELECT id, menu_name, menu_code, path, component, parent_id, menu_type, sort
            FROM sys_menu 
            WHERE deleted = 0
            ORDER BY parent_id, sort
        """)
        
        print("当前菜单结构:")
        print("ID | 菜单名称 | 菜单代码 | 路径 | 父ID | 类型 | 排序")
        print("-" * 100)
        for row in cursor.fetchall():
            print(f"{row[0]} | {row[1]} | {row[2]} | {row[3]} | {row[5]} | {row[6]} | {row[7]}")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"错误: {e}")

if __name__ == "__main__":
    check_current_menu()