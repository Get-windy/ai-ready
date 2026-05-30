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
        
        print("检查 user 表...")
        
        # 检查 user 表是否存在
        cursor.execute("""
            SELECT table_name FROM information_schema.tables 
            WHERE table_schema = 'public' AND table_name = 'user'
        """)
        result = cursor.fetchone()
        if result:
            print("user 表存在")
            cursor.execute("SELECT id, username, status, deleted FROM \"user\"")
            users = cursor.fetchall()
            for user in users:
                print(f"用户: id={user[0]}, username={user[1]}, status={user[2]}, deleted={user[3]}")
        else:
            print("user 表不存在")
        
        print("\n检查 sys_user 表...")
        cursor.execute("SELECT id, username, status, deleted FROM sys_user")
        users = cursor.fetchall()
        for user in users:
            print(f"用户: id={user[0]}, username={user[1]}, status={user[2]}, deleted={user[3]}")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"错误: {e}")
        raise

if __name__ == "__main__":
    main()