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
        
        print("更新角色状态...")
        
        # 更新所有角色的状态为1（启用）
        cursor.execute("UPDATE sys_role SET status = 1 WHERE deleted = 0")
        role_count = cursor.rowcount
        print(f"更新角色状态: {role_count} 条记录")
        
        conn.commit()
        
        # 验证更新结果
        cursor.execute("SELECT id, role_name, status FROM sys_role WHERE deleted = 0")
        roles = cursor.fetchall()
        for role in roles:
            print(f"角色: id={role[0]}, role_name={role[1]}, status={role[2]}")
        
        print("角色状态更新完成!")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"错误: {e}")
        raise

if __name__ == "__main__":
    main()