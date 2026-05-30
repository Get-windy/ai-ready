import psycopg2

def check_erp_submenu():
    try:
        conn = psycopg2.connect(
            host="localhost",
            port=5432,
            database="devdb",
            user="devuser",
            password="devuser123"
        )
        
        cursor = conn.cursor()
        
        print("=== 1. 检查ERP菜单数据 ===")
        cursor.execute("""
            SELECT id, menu_name, menu_code, path, component, parent_id, menu_type, visible, status, sort
            FROM sys_menu 
            WHERE id = 2 OR parent_id = 2
            ORDER BY parent_id, sort
        """)
        
        print("ID | 菜单名称 | 菜单代码 | 路径 | 组件 | 父ID | 类型 | 可见 | 状态 | 排序")
        print("-" * 120)
        for row in cursor.fetchall():
            print(f"{row[0]} | {row[1]} | {row[2]} | {row[3]} | {row[4]} | {row[5]} | {row[6]} | {row[7]} | {row[8]} | {row[9]}")
        
        print("\n=== 2. 检查角色菜单权限 ===")
        cursor.execute("""
            SELECT rm.id, rm.role_id, rm.menu_id, m.menu_name
            FROM sys_role_menu rm
            LEFT JOIN sys_menu m ON rm.menu_id = m.id
            WHERE rm.menu_id IN (2, 200, 201, 202)
            ORDER BY rm.menu_id
        """)
        
        print("关联ID | 角色ID | 菜单ID | 菜单名称")
        print("-" * 60)
        for row in cursor.fetchall():
            print(f"{row[0]} | {row[1]} | {row[2]} | {row[3]}")
        
        print("\n=== 3. 检查用户角色关联 ===")
        cursor.execute("""
            SELECT ur.id, ur.user_id, ur.role_id, r.role_name
            FROM sys_user_role ur
            LEFT JOIN sys_role r ON ur.role_id = r.id
            WHERE ur.user_id = 1
        """)
        
        print("关联ID | 用户ID | 角色ID | 角色名称")
        print("-" * 60)
        for row in cursor.fetchall():
            print(f"{row[0]} | {row[1]} | {row[2]} | {row[3]}")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"错误: {e}")

if __name__ == "__main__":
    check_erp_submenu()