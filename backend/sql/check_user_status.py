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
        
        print("检查用户状态...")
        
        cursor.execute("SELECT id, username, status, deleted FROM sys_user")
        users = cursor.fetchall()
        for user in users:
            print(f"用户: id={user[0]}, username={user[1]}, status={user[2]}, deleted={user[3]}")
        
        print("\n检查角色状态...")
        cursor.execute("SELECT id, role_name, status, deleted FROM sys_role")
        roles = cursor.fetchall()
        for role in roles:
            print(f"角色: id={role[0]}, role_name={role[1]}, status={role[2]}, deleted={role[3]}")
        
        print("\n检查菜单状态...")
        cursor.execute("SELECT id, menu_name, status, deleted FROM sys_menu LIMIT 10")
        menus = cursor.fetchall()
        for menu in menus:
            print(f"菜单: id={menu[0]}, menu_name={menu[1]}, status={menu[2]}, deleted={menu[3]}")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"错误: {e}")
        raise

if __name__ == "__main__":
    main()