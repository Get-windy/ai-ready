import psycopg2

def check_role_menu_permissions():
    try:
        conn = psycopg2.connect(
            host="localhost",
            port=5432,
            database="devdb",
            user="devuser",
            password="devuser123"
        )
        
        cursor = conn.cursor()
        
        print("=== 1. 检查用户1的角色 ===")
        cursor.execute("""
            SELECT ur.user_id, ur.role_id, r.role_name
            FROM sys_user_role ur
            LEFT JOIN sys_role r ON ur.role_id = r.id
            WHERE ur.user_id = 1
        """)
        
        print("用户ID | 角色ID | 角色名称")
        print("-" * 60)
        for row in cursor.fetchall():
            print(f"{row[0]} | {row[1]} | {row[2]}")
        
        print("\n=== 2. 检查角色1的菜单权限 ===")
        cursor.execute("""
            SELECT rm.menu_id, m.menu_name, m.menu_code, m.parent_id
            FROM sys_role_menu rm
            LEFT JOIN sys_menu m ON rm.menu_id = m.id
            WHERE rm.role_id = 1
            ORDER BY m.parent_id, m.sort
        """)
        
        print("菜单ID | 菜单名称 | 菜单代码 | 父ID")
        print("-" * 80)
        for row in cursor.fetchall():
            print(f"{row[0]} | {row[1]} | {row[2]} | {row[3]}")
        
        print("\n=== 3. 检查ERP菜单是否在角色权限中 ===")
        cursor.execute("""
            SELECT m.id, m.menu_name, m.menu_code, 
                   CASE WHEN rm.menu_id IS NOT NULL THEN '有权限' ELSE '无权限' END as permission_status
            FROM sys_menu m
            LEFT JOIN sys_role_menu rm ON m.id = rm.menu_id AND rm.role_id = 1
            WHERE m.id IN (2, 200, 201, 202)
            ORDER BY m.id
        """)
        
        print("菜单ID | 菜单名称 | 菜单代码 | 权限状态")
        print("-" * 80)
        for row in cursor.fetchall():
            print(f"{row[0]} | {row[1]} | {row[2]} | {row[3]}")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"错误: {e}")

if __name__ == "__main__":
    check_role_menu_permissions()