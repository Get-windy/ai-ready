import psycopg2

def update_status_to_scheme_a():
    try:
        conn = psycopg2.connect(
            host="localhost",
            port=5432,
            database="devdb",
            user="devuser",
            password="devuser123"
        )
        
        cursor = conn.cursor()
        
        sql_file_path = r"i:\AI-Ready\backend\sql\update_status_to_scheme_a.sql"
        
        with open(sql_file_path, 'r', encoding='utf-8') as f:
            sql_content = f.read()
        
        try:
            cursor.execute(sql_content)
            conn.commit()
            print("status字段已按方案A标准更新完成！")
            print("方案A：status=1=启用，status=0=停用（遵循国内ERP惯例）")
        except Exception as e:
            print(f"执行SQL失败: {e}")
            conn.rollback()
        
        cursor.execute("""
            SELECT id, menu_name, menu_code, status, 
                   CASE 
                     WHEN status = 1 THEN '启用'
                     WHEN status = 0 THEN '停用'
                     ELSE '未知'
                   END as status_text
            FROM sys_menu 
            WHERE deleted = 0
            ORDER BY parent_id, sort
        """)
        
        print("\n验证结果:")
        print("ID | 菜单名称 | 菜单代码 | status | 状态")
        print("-" * 80)
        for row in cursor.fetchall():
            print(f"{row[0]} | {row[1]} | {row[2]} | {row[3]} | {row[4]}")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"数据库连接失败: {e}")

if __name__ == "__main__":
    update_status_to_scheme_a()