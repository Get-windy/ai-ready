import psycopg2

def update_all_tables_status():
    try:
        conn = psycopg2.connect(
            host="localhost",
            port=5432,
            database="devdb",
            user="devuser",
            password="devuser123"
        )
        
        cursor = conn.cursor()
        
        sql_file_path = r"i:\AI-Ready\backend\sql\update_all_tables_status.sql"
        
        with open(sql_file_path, 'r', encoding='utf-8') as f:
            sql_content = f.read()
        
        try:
            cursor.execute(sql_content)
            conn.commit()
            print("所有表的status字段已按方案A标准更新完成！")
            print("方案A：status=1=启用，status=0=停用（遵循国内ERP惯例）")
        except Exception as e:
            print(f"执行SQL失败: {e}")
            conn.rollback()
        
        cursor.execute("""
            SELECT 'sys_menu' as table_name, COUNT(*) as total_count, 
                   SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as enabled_count,
                   SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) as disabled_count
            FROM sys_menu WHERE deleted = 0
            
            UNION ALL
            
            SELECT 'sys_permission' as table_name, COUNT(*) as total_count,
                   SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as enabled_count,
                   SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) as disabled_count
            FROM sys_permission WHERE deleted = 0
            
            UNION ALL
            
            SELECT 'sys_user' as table_name, COUNT(*) as total_count,
                   SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as enabled_count,
                   SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) as disabled_count
            FROM sys_user WHERE deleted = 0
            
            UNION ALL
            
            SELECT 'sys_role' as table_name, COUNT(*) as total_count,
                   SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as enabled_count,
                   SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) as disabled_count
            FROM sys_role WHERE deleted = 0
        """)
        
        print("\n验证结果:")
        print("表名 | 总记录数 | 启用数(status=1) | 停用数(status=0)")
        print("-" * 80)
        for row in cursor.fetchall():
            print(f"{row[0]} | {row[1]} | {row[2]} | {row[3]}")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"数据库连接失败: {e}")

if __name__ == "__main__":
    update_all_tables_status()