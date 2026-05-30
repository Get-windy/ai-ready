import psycopg2

def check_role_menu_structure():
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
            SELECT column_name, data_type 
            FROM information_schema.columns 
            WHERE table_name = 'sys_role_menu' 
            ORDER BY ordinal_position
        """)
        
        print("sys_role_menu表结构:")
        print("字段名 | 数据类型")
        print("-" * 40)
        for row in cursor.fetchall():
            print(f"{row[0]} | {row[1]}")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"错误: {e}")

if __name__ == "__main__":
    check_role_menu_structure()