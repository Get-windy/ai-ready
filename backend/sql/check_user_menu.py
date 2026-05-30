import psycopg2

def main():
    try:
        conn = psycopg2.connect(
            host="localhost",
            port=5432,
            database="devdb",
            user="devuser",
            password="devuser123"
        )
        cursor = conn.cursor()
        
        print("检查用户角色分配...")
        cursor.execute("SELECT * FROM sys_user_role WHERE user_id = 1")
        user_roles = cursor.fetchall()
        print(f"用户角色: {user_roles}")
        
        print("\n检查角色菜单分配...")
        cursor.execute("SELECT * FROM sys_role_menu WHERE role_id IN (SELECT role_id FROM sys_user_role WHERE user_id = 1)")
        role_menus = cursor.fetchall()
        print(f"角色菜单: {role_menus[:20]}...")  # 只显示前20条
        
        print("\n检查菜单状态...")
        cursor.execute("SELECT id, menu_name, status, deleted FROM sys_menu WHERE deleted = 0 LIMIT 10")
        menus = cursor.fetchall()
        for menu in menus:
            print(f"菜单: id={menu[0]}, menu_name={menu[1]}, status={menu[2]}")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"错误: {e}")
        raise

if __name__ == "__main__":
    main()