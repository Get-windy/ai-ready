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
        
        print("检查用户密码...")
        cursor.execute("SELECT id, username, password FROM sys_user WHERE username = 'admin'")
        user = cursor.fetchone()
        if user:
            print(f"用户: id={user[0]}, username={user[1]}")
            print(f"密码: {user[2]}")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"错误: {e}")
        raise

if __name__ == "__main__":
    main()