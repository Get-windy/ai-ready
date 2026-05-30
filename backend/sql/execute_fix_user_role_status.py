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
        
        print("开始修复用户和角色状态...")
        
        # 更新用户状态
        cursor.execute("UPDATE sys_user SET status = 1 WHERE status = 0 AND deleted = 0")
        user_count = cursor.rowcount
        print(f"更新用户状态: {user_count} 条记录")
        
        # 更新角色状态
        cursor.execute("UPDATE sys_role SET status = 1 WHERE status = 0 AND deleted = 0")
        role_count = cursor.rowcount
        print(f"更新角色状态: {role_count} 条记录")
        
        # 更新部门状态
        cursor.execute("UPDATE sys_department SET status = 1 WHERE status = 0 AND deleted = 0")
        dept_count = cursor.rowcount
        print(f"更新部门状态: {dept_count} 条记录")
        
        # 更新字典类型状态
        cursor.execute("UPDATE sys_dict_type SET status = 1 WHERE status = 0 AND deleted = 0")
        dict_type_count = cursor.rowcount
        print(f"更新字典类型状态: {dict_type_count} 条记录")
        
        # 更新字典数据状态
        cursor.execute("UPDATE sys_dict_item SET status = 1 WHERE status = 0 AND deleted = 0")
        dict_item_count = cursor.rowcount
        print(f"更新字典数据状态: {dict_item_count} 条记录")
        
        # 更新权限模板状态
        cursor.execute("UPDATE sys_permission_template SET status = 1 WHERE status = 0 AND deleted = 0")
        template_count = cursor.rowcount
        print(f"更新权限模板状态: {template_count} 条记录")
        
        # 更新项目配置状态
        cursor.execute("UPDATE sys_project_config SET status = 1 WHERE status = 0 AND deleted = 0")
        config_count = cursor.rowcount
        print(f"更新项目配置状态: {config_count} 条记录")
        
        conn.commit()
        
        # 验证更新结果
        cursor.execute("SELECT COUNT(*) FROM sys_user WHERE status = 1 AND deleted = 0")
        user_enabled = cursor.fetchone()[0]
        print(f"启用用户数: {user_enabled}")
        
        cursor.execute("SELECT COUNT(*) FROM sys_role WHERE status = 1 AND deleted = 0")
        role_enabled = cursor.fetchone()[0]
        print(f"启用角色数: {role_enabled}")
        
        cursor.execute("SELECT COUNT(*) FROM sys_menu WHERE status = 1 AND deleted = 0")
        menu_enabled = cursor.fetchone()[0]
        print(f"启用菜单数: {menu_enabled}")
        
        print("状态修复完成!")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"错误: {e}")
        raise

if __name__ == "__main__":
    main()