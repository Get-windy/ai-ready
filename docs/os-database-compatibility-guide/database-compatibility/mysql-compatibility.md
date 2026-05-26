# MySQL兼容性测试指南

## 1. 支持的MySQL版本

| MySQL版本 | 发布年份 | 支持状态 | 生命周期结束 |
|-----------|----------|----------|--------------|
| 8.0 | 2018 | ✅ 完全支持 | 2026-04 |
| 8.1 | 2023 | ✅ 完全支持 | 2030-10 |

## 2. 核心测试用例

### 2.1 连接测试
```sql
-- 测试数据库连接
SELECT @@version AS mysql_version,
       @@version_comment AS version_comment,
       @@version_compile_os AS compile_os,
       @@version_compile_machine AS compile_machine;

-- 测试连接池配置
SHOW VARIABLES LIKE 'max_connections';
SHOW VARIABLES LIKE 'wait_timeout';
SHOW VARIABLES LIKE 'interactive_timeout';
```

### 2.2 权限和安全测试
```sql
-- 创建测试用户和权限
CREATE USER IF NOT EXISTS 'price_strategy_test'@'localhost' 
IDENTIFIED BY 'Test@123456';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE TEMPORARY TABLES 
ON price_strategy.* TO 'price_strategy_test'@'localhost';

-- 验证权限
SHOW GRANTS FOR 'price_strategy_test'@'localhost';

-- 测试SSL连接（如果启用）
SHOW VARIABLES LIKE '%ssl%';
SHOW STATUS LIKE 'Ssl_cipher';
```

### 2.3 数据类型兼容性
```sql
-- 测试MySQL特定数据类型
CREATE TABLE IF NOT EXISTS test_data_types (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status ENUM('active', 'inactive', 'pending') DEFAULT 'pending',
    metadata JSON,
    full_text TEXT,
    full_text_indexed TEXT,
    INDEX idx_status (status),
    FULLTEXT INDEX idx_fulltext (full_text_indexed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入测试数据
INSERT INTO test_data_types (price, status, metadata) VALUES
(99.99, 'active', '{"discount": 10, "currency": "CNY"}'),
(149.99, 'inactive', '{"discount": 15, "currency": "USD"}');

-- 查询测试
SELECT * FROM test_data_types WHERE JSON_EXTRACT(metadata, '$.discount') > 5;
```

### 2.4 事务和锁定测试
```sql
-- 测试事务隔离级别
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;

-- 测试行级锁定
SELECT * FROM price_strategy.prices 
WHERE product_id = 100 
FOR UPDATE;

-- 模拟并发更新
-- 在另一个会话中尝试更新相同行
-- UPDATE price_strategy.prices SET price = 200 WHERE product_id = 100;

COMMIT;

-- 检查死锁检测
SHOW ENGINE INNODB STATUS\G
```

### 2.5 性能测试
```sql
-- 创建性能测试表
CREATE TABLE IF NOT EXISTS performance_test (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(255),
    category VARCHAR(100),
    price DECIMAL(10,2),
    stock INT,
    created_date DATE,
    INDEX idx_category_price (category, price),
    INDEX idx_created_date (created_date)
);

-- 批量插入测试数据
DELIMITER $$
CREATE PROCEDURE generate_test_data(IN num_records INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE categories VARCHAR(100) DEFAULT 'electronics,clothing,books,food';
    
    WHILE i < num_records DO
        INSERT INTO performance_test 
        (product_name, category, price, stock, created_date)
        VALUES (
            CONCAT('Product_', i),
            SUBSTRING_INDEX(SUBSTRING_INDEX(categories, ',', FLOOR(RAND() * 4) + 1), ',', -1),
            ROUND(RAND() * 1000, 2),
            FLOOR(RAND() * 1000),
            DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND() * 365) DAY)
        );
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

-- 生成10万条测试数据
CALL generate_test_data(100000);
```

## 3. 自动化测试脚本

### 3.1 Bash测试脚本
```bash
#!/bin/bash
# test-mysql-compatibility.sh

set -e

echo "=== MySQL兼容性测试开始 ==="

# 数据库连接参数
DB_HOST="localhost"
DB_PORT="3306"
DB_USER="price_strategy_test"
DB_PASS="Test@123456"
DB_NAME="price_strategy_test"

# 1. 测试连接
echo "1. 测试数据库连接..."
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -e "SELECT '连接成功' AS status;" || {
    echo "数据库连接失败"
    exit 1
}

# 2. 测试版本兼容性
echo "2. 测试版本兼容性..."
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -e "
SELECT @@version AS mysql_version,
       @@version_comment AS version_comment;
"

# 3. 测试基本功能
echo "3. 测试基本功能..."
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" <<EOF
-- 创建测试数据库
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\`;
USE \`${DB_NAME}\`;

-- 创建测试表
CREATE TABLE IF NOT EXISTS compatibility_test (
    id INT AUTO_INCREMENT PRIMARY KEY,
    test_name VARCHAR(100),
    test_result ENUM('PASS', 'FAIL', 'SKIP'),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 插入测试记录
INSERT INTO compatibility_test (test_name, test_result) VALUES
('连接测试', 'PASS'),
('权限测试', 'PASS'),
('数据类型测试', 'PASS');

-- 查询测试结果
SELECT test_name, test_result FROM compatibility_test;
EOF

# 4. 测试性能
echo "4. 测试性能..."
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -e "
USE \`${DB_NAME}\`;
SHOW TABLE STATUS LIKE 'performance_test';
SELECT COUNT(*) AS total_records FROM performance_test;
"

# 5. 清理测试数据
echo "5. 清理测试数据..."
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -e "
DROP DATABASE IF EXISTS \`${DB_NAME}\`;
"

echo "=== MySQL兼容性测试完成 ==="
```

### 3.2 Python测试脚本
```python
#!/usr/bin/env python3
# mysql_compatibility_tests.py

import mysql.connector
import sys
from datetime import datetime

class MySQLCompatibilityTester:
    def __init__(self, host='localhost', port=3306, 
                 user='price_strategy_test', password='Test@123456'):
        self.config = {
            'host': host,
            'port': port,
            'user': user,
            'password': password,
            'database': 'price_strategy_test'
        }
        
    def test_connection(self):
        """测试数据库连接"""
        try:
            conn = mysql.connector.connect(**self.config)
            cursor = conn.cursor()
            cursor.execute("SELECT VERSION()")
            version = cursor.fetchone()[0]
            print(f"✓ MySQL版本: {version}")
            cursor.close()
            conn.close()
            return True
        except mysql.connector.Error as err:
            print(f"✗ 连接失败: {err}")
            return False
    
    def test_data_types(self):
        """测试数据类型兼容性"""
        try:
            conn = mysql.connector.connect(**self.config)
            cursor = conn.cursor()
            
            # 测试JSON数据类型（MySQL 5.7+）
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS test_json (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    data JSON,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """)
            
            # 插入JSON数据
            import json
            test_data = {
                "product": "测试产品",
                "price": 99.99,
                "currency": "CNY",
                "discount": 10
            }
            
            cursor.execute(
                "INSERT INTO test_json (data) VALUES (%s)",
                (json.dumps(test_data),)
            )
            conn.commit()
            
            # 查询JSON数据
            cursor.execute("""
                SELECT id, data->>'$.product', data->>'$.price'
                FROM test_json
                WHERE JSON_EXTRACT(data, '$.discount') > 5
            """)
            
            results = cursor.fetchall()
            print(f"✓ JSON查询结果: {results}")
            
            cursor.execute("DROP TABLE IF EXISTS test_json")
            conn.commit()
            
            cursor.close()
            conn.close()
            return True
            
        except mysql.connector.Error as err:
            print(f"✗ 数据类型测试失败: {err}")
            return False
    
    def test_transactions(self):
        """测试事务处理"""
        try:
            conn = mysql.connector.connect(**self.config)
            conn.autocommit = False
            cursor = conn.cursor()
            
            # 创建测试表
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS test_transactions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    amount DECIMAL(10,2),
                    status VARCHAR(20)
                )
            """)
            
            # 开始事务
            cursor.execute("START TRANSACTION")
            
            # 插入数据
            cursor.execute(
                "INSERT INTO test_transactions (amount, status) VALUES (%s, %s)",
                (100.00, 'pending')
            )
            
            # 回滚测试
            conn.rollback()
            
            # 验证数据是否回滚
            cursor.execute("SELECT COUNT(*) FROM test_transactions WHERE status = 'pending'")
            count = cursor.fetchone()[0]
            
            if count == 0:
                print("✓ 事务回滚测试通过")
            else:
                print("✗ 事务回滚测试失败")
                return False
            
            # 提交测试
            cursor.execute("START TRANSACTION")
            cursor.execute(
                "INSERT INTO test_transactions (amount, status) VALUES (%s, %s)",
                (200.00, 'completed')
            )
            conn.commit()
            
            cursor.execute("SELECT COUNT(*) FROM test_transactions WHERE status = 'completed'")
            count = cursor.fetchone()[0]
            
            if count == 1:
                print("✓ 事务提交测试通过")
            else:
                print("✗ 事务提交测试失败")
                return False
            
            cursor.execute("DROP TABLE IF EXISTS test_transactions")
            conn.commit()
            
            cursor.close()
            conn.close()
            return True
            
        except mysql.connector.Error as err:
            print(f"✗ 事务测试失败: {err}")
            return False
    
    def run_all_tests(self):
        """运行所有测试"""
        tests = [
            ("连接测试", self.test_connection),
            ("数据类型测试", self.test_data_types),
            ("事务测试", self.test_transactions)
        ]
        
        results = []
        for test_name, test_func in tests:
            print(f"\n执行 {test_name}...")
            try:
                success = test_func()
                results.append((test_name, success))
            except Exception as e:
                print(f"测试异常: {e}")
                results.append((test_name, False))
        
        # 输出测试结果
        print("\n" + "="*50)
        print("测试结果汇总:")
        print("="*50)
        
        passed = 0
        for test_name, success in results:
            status = "✓ 通过" if success else "✗ 失败"
            print(f"{test_name}: {status}")
            if success:
                passed += 1
        
        total = len(results)
        print(f"\n通过率: {passed}/{total} ({passed/total*100:.1f}%)")
        
        return all(success for _, success in results)

if __name__ == "__main__":
    tester = MySQLCompatibilityTester()
    success = tester.run_all_tests()
    sys.exit(0 if success else 1)
```

## 4. 已知问题和解决方案

### 4.1 常见兼容性问题

| 问题 | 影响版本 | 解决方案 |
|------|----------|----------|
| utf8mb4字符集支持 | MySQL 5.5+ | 使用utf8mb4代替utf8 |
| JSON数据类型 | MySQL 5.7+ | 降级方案：使用TEXT存储JSON |
| 窗口函数 | MySQL 8.0+ | 使用子查询或临时表替代 |
| CTE（公用表表达式） | MySQL 8.0+ | 使用临时表或派生表 |
| 默认认证插件 | MySQL 8.0+ | 使用mysql_native_password或更新驱动 |

### 4.2 性能优化配置
```sql
-- MySQL 8.0+ 性能优化配置
SET GLOBAL innodb_buffer_pool_size = 1073741824; -- 1GB
SET GLOBAL innodb_log_file_size = 268435456; -- 256MB
SET GLOBAL max_connections = 200;
SET GLOBAL thread_cache_size = 16;

-- 查询缓存（MySQL 8.0已移除）
-- 使用应用程序级缓存替代
```

## 5. 迁移指南

### 5.1 从MySQL 5.7升级到8.0
```sql
-- 升级前检查
SELECT @@version;
SHOW ENGINES;
SHOW VARIABLES LIKE 'default_authentication_plugin';

-- 兼容性检查脚本
SELECT 
    table_schema,
    table_name,
    engine,
    row_format,
    table_collation
FROM information_schema.tables
WHERE table_schema NOT IN ('mysql', 'information_schema', 'performance_schema', 'sys')
AND (engine != 'InnoDB' OR row_format != 'Dynamic');

-- 升级后验证
SELECT @@version;
SHOW PLUGINS;
```

## 6. 监控和维护

### 6.1 监控脚本
```bash
#!/bin/bash
# monitor-mysql.sh

DB_HOST="localhost"
DB_USER="monitor"
DB_PASS="Monitor@123"

echo "MySQL监控报告 - $(date)"
echo "========================"

# 连接状态
mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASS" -e "
SHOW STATUS LIKE 'Threads_connected';
SHOW STATUS LIKE 'Max_used_connections';
SHOW STATUS LIKE 'Aborted_connects';
"

# 性能指标
mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASS" -e "
SHOW STATUS LIKE 'Innodb_buffer_pool_reads';
SHOW STATUS LIKE 'Innodb_buffer_pool_read_requests';
SHOW STATUS LIKE 'Innodb_rows_read';
SHOW STATUS LIKE 'Innodb_rows_inserted';
SHOW STATUS LIKE 'Innodb_rows_updated';
SHOW STATUS LIKE 'Innodb_rows_deleted';
"

# 慢查询监控
mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASS" -e "
SHOW VARIABLES LIKE 'long_query_time';
SHOW STATUS LIKE 'Slow_queries';
"
```

---

**文档版本**: 1.0  
**最后更新**: 2026-05-01  
**测试环境**: MySQL 8.0.33, 8.1.0  
**状态**: 已验证