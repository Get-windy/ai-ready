import psycopg2

def check_erp_menu():
    try:
        conn = psycopg2.connect(
            host="localhost",
            port=5432,
            database="devdb",
            user="devuser",
            password="devuser123"
        )
        
        cursor = conn.cursor()
        
        print("=== 1. 检查ERP菜单完整数据 ===")
        cursor.execute("""
            SELECT id, menu_name, menu_code, path, component, parent_id, menu_type, visible, status, sort, deleted
            FROM sys_menu 
            WHERE id = 2 OR parent_id = 2 OR menu_code LIKE 'erp:%'
            ORDER BY parent_id, sort
        """)
        
        print("ID | 菜单名称 | 菜单代码 | 路径 | 组件 | 父ID | 类型 | 可见 | 状态 | 排序 | 删除")
        print("-" * 120)
        for row in cursor.fetchall():
            print(f"{row[0]} | {row[1]} | {row[2]} | {row[3]} | {row[4]} | {row[5]} | {row[6]} | {row[7]} | {row[8]} | {row[9]} | {row[10]}")
        
        print("\n=== 2. 检查用户菜单权限 ===")
        cursor.execute("""
            SELECT m.id, m.menu_name, m.menu_code, m.parent_id, m.menu_type, m.status, m.deleted
            FROM sys_menu m
            LEFT JOIN sys_role_menu rm ON m.id = rm.menu_id
            LEFT JOIN sys_user_role ur ON rm.role_id = ur.role_id
            WHERE ur.user_id = 1 
              AND (m.id = 2 OR m.parent_id = 2 OR m.menu_code LIKE 'erp:%')
            ORDER BY m.parent_id, m.sort
        """)
        
        print("ID | 菜单名称 | 菜单代码 | 父ID | 类型 | 状态 | 删除")
        print("-" * 80)
        for row in cursor.fetchall():
            print(f"{row[0]} | {row[1]} | {row[2]} | {row[3]} | {row[4]} | {row[5]} | {row[6]}")
        
        print("\n=== 3. 检查角色菜单关联 ===")
        cursor.execute("""
            SELECT rm.menu_id, m.menu_name, rm.role_id
            FROM sys_role_menu rm
            LEFT JOIN sys_menu m ON rm.menu_id = m.id
            WHERE rm.menu_id IN (2, 200, 201, 202)
            ORDER BY rm.menu_id
        """)
        
        print("菜单ID | 菜单名称 | 角色ID")
        print("-" * 60)
        for row in cursor.fetchall():
            print(f"{row[0]} | {row[1]} | {row[2]}")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"错误: {e}")

if __name__ == "__main__":
    check_erp_menu()