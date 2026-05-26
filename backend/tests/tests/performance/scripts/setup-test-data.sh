#!/bin/bash
# setup-test-data.sh
# 性能测试数据准备脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DATA_DIR="$SCRIPT_DIR/../data"
CONFIG_DIR="$SCRIPT_DIR/../config"
LOG_DIR="$SCRIPT_DIR/../logs"

# 创建日志目录
mkdir -p "$LOG_DIR"

# 日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_DIR/setup-data.log"
}

# 检查数据库连接
check_db_connection() {
    log "检查数据库连接..."
    
    # MySQL连接检查
    if mysql -h localhost -P 3306 -u root -p'TestEnv@2026' -e "SELECT 1" > /dev/null 2>&1; then
        log "✓ MySQL连接正常"
    else
        log "✗ MySQL连接失败"
        return 1
    fi
    
    # PostgreSQL连接检查
    if psql -h localhost -p 5432 -U postgres -d testdb -c "SELECT 1" > /dev/null 2>&1; then
        log "✓ PostgreSQL连接正常"
    else
        log "✗ PostgreSQL连接失败"
        return 1
    fi
    
    return 0
}

# 清理现有测试数据
cleanup_test_data() {
    log "清理现有测试数据..."
    
    # MySQL数据清理
    mysql -h localhost -P 3306 -u root -p'TestEnv@2026' << 'EOF'
USE testdb;
-- 禁用外键约束
SET FOREIGN_KEY_CHECKS = 0;

-- 清空测试表
TRUNCATE TABLE users;
TRUNCATE TABLE orders;
TRUNCATE TABLE order_items;
TRUNCATE TABLE products;
TRUNCATE TABLE inventory;
TRUNCATE TABLE purchases;
TRUNCATE TABLE finance_transactions;

-- 启用外键约束
SET FOREIGN_KEY_CHECKS = 1;
EOF
    
    # PostgreSQL数据清理
    psql -h localhost -p 5432 -U postgres -d testdb << 'EOF'
-- 清空测试表
TRUNCATE TABLE alert_rules CASCADE;
TRUNCATE TABLE ai_models CASCADE;
TRUNCATE TABLE monitoring_metrics CASCADE;
TRUNCATE TABLE performance_logs CASCADE;
EOF
    
    log "✓ 数据清理完成"
}

# 生成测试用户数据
generate_user_data() {
    log "生成测试用户数据..."
    
    local user_count=1000
    local output_file="$DATA_DIR/test-users.csv"
    
    # 创建CSV文件头
    echo "id,username,email,password_hash,full_name,role,status,created_at,updated_at" > "$output_file"
    
    # 生成用户数据
    for i in $(seq 1 $user_count); do
        local username="user$i"
        local email="user$i@example.com"
        local password_hash="\$2a\$10\$TestHash12345678901234567890"  # 模拟BCrypt哈希
        local full_name="User $i"
        
        # 分配角色
        local role="USER"
        if [ $((i % 10)) -eq 0 ]; then
            role="ADMIN"
        elif [ $((i % 5)) -eq 0 ]; then
            role="MANAGER"
        fi
        
        local status="ACTIVE"
        local created_at="2026-04-24 10:00:00"
        local updated_at="2026-04-24 10:00:00"
        
        echo "$i,$username,$email,$password_hash,$full_name,$role,$status,$created_at,$updated_at" >> "$output_file"
    done
    
    log "✓ 生成 $user_count 个用户数据"
}

# 生成测试订单数据
generate_order_data() {
    log "生成测试订单数据..."
    
    local order_count=5000
    local output_file="$DATA_DIR/test-orders.csv"
    
    # 创建CSV文件头
    echo "id,user_id,order_number,total_amount,status,payment_status,created_at,updated_at" > "$output_file"
    
    # 生成订单数据
    for i in $(seq 1 $order_count); do
        local user_id=$((RANDOM % 1000 + 1))
        local order_number="ORD-20260424-$(printf "%06d" $i)"
        local total_amount=$((RANDOM % 10000 + 100))
        local status="COMPLETED"
        
        # 随机分配状态
        local status_rand=$((RANDOM % 10))
        case $status_rand in
            0) status="PENDING" ;;
            1) status="PROCESSING" ;;
            2) status="SHIPPED" ;;
            3) status="DELIVERED" ;;
            *) status="COMPLETED" ;;
        esac
        
        local payment_status="PAID"
        local created_at="2026-04-24 10:00:00"
        local updated_at="2026-04-24 10:00:00"
        
        echo "$i,$user_id,$order_number,$total_amount,$status,$payment_status,$created_at,$updated_at" >> "$output_file"
    done
    
    log "✓ 生成 $order_count 个订单数据"
}

# 生成测试库存数据
generate_inventory_data() {
    log "生成测试库存数据..."
    
    local inventory_count=10000
    local output_file="$DATA_DIR/test-inventory.csv"
    
    # 创建CSV文件头
    echo "id,product_id,warehouse_id,quantity,reserved_quantity,min_stock_level,max_stock_level,created_at,updated_at" > "$output_file"
    
    # 商品列表
    local products=("Laptop" "Phone" "Tablet" "Monitor" "Keyboard" "Mouse" "Headphones" "Charger" "Cable" "Adapter")
    
    # 生成库存数据
    for i in $(seq 1 $inventory_count); do
        local product_id=$((RANDOM % 1000 + 1))
        local warehouse_id=$((RANDOM % 5 + 1))
        local quantity=$((RANDOM % 1000 + 10))
        local reserved_quantity=$((RANDOM % 100))
        local min_stock_level=$((quantity / 10))
        local max_stock_level=$((quantity * 2))
        local created_at="2026-04-24 10:00:00"
        local updated_at="2026-04-24 10:00:00"
        
        echo "$i,$product_id,$warehouse_id,$quantity,$reserved_quantity,$min_stock_level,$max_stock_level,$created_at,$updated_at" >> "$output_file"
    done
    
    log "✓ 生成 $inventory_count 个库存数据"
}

# 生成测试财务数据
generate_finance_data() {
    log "生成测试财务数据..."
    
    local finance_count=10000
    local output_file="$DATA_DIR/test-finance.csv"
    
    # 创建CSV文件头
    echo "id,transaction_id,account_id,amount,transaction_type,description,status,created_at,updated_at" > "$output_file"
    
    # 生成财务数据
    for i in $(seq 1 $finance_count); do
        local transaction_id="TXN-20260424-$(printf "%08d" $i)"
        local account_id=$((RANDOM % 100 + 1))
        local amount=$((RANDOM % 100000 + 100))
        local transaction_type="REVENUE"
        
        # 随机分配交易类型
        local type_rand=$((RANDOM % 10))
        case $type_rand in
            0|1|2) transaction_type="REVENUE" ;;
            3|4|5) transaction_type="EXPENSE" ;;
            6|7) transaction_type="TRANSFER" ;;
            *) transaction_type="ADJUSTMENT" ;;
        esac
        
        local description="$transaction_type transaction #$i"
        local status="COMPLETED"
        local created_at="2026-04-24 10:00:00"
        local updated_at="2026-04-24 10:00:00"
        
        echo "$i,$transaction_id,$account_id,$amount,$transaction_type,$description,$status,$created_at,$updated_at" >> "$output_file"
    done
    
    log "✓ 生成 $finance_count 个财务数据"
}

# 导入数据到数据库
import_data_to_db() {
    log "导入数据到数据库..."
    
    # 导入用户数据到MySQL
    log "导入用户数据..."
    mysql -h localhost -P 3306 -u root -p'TestEnv@2026' testdb << 'EOF'
LOAD DATA LOCAL INFILE 'test-users.csv'
INTO TABLE users
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(id, username, email, password_hash, full_name, role, status, created_at, updated_at);
EOF
    
    # 导入订单数据
    log "导入订单数据..."
    mysql -h localhost -P 3306 -u root -p'TestEnv@2026' testdb << 'EOF'
LOAD DATA LOCAL INFILE 'test-orders.csv'
INTO TABLE orders
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(id, user_id, order_number, total_amount, status, payment_status, created_at, updated_at);
EOF
    
    # 导入库存数据
    log "导入库存数据..."
    mysql -h localhost -P 3306 -u root -p'TestEnv@2026' testdb << 'EOF'
LOAD DATA LOCAL INFILE 'test-inventory.csv'
INTO TABLE inventory
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(id, product_id, warehouse_id, quantity, reserved_quantity, min_stock_level, max_stock_level, created_at, updated_at);
EOF
    
    # 导入财务数据
    log "导入财务数据..."
    mysql -h localhost -P 3306 -u root -p'TestEnv@2026' testdb << 'EOF'
LOAD DATA LOCAL INFILE 'test-finance.csv'
INTO TABLE finance_transactions
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(id, transaction_id, account_id, amount, transaction_type, description, status, created_at, updated_at);
EOF
    
    log "✓ 数据导入完成"
}

# 验证数据导入
verify_data_import() {
    log "验证数据导入..."
    
    # 验证MySQL数据
    local user_count=$(mysql -h localhost -P 3306 -u root -p'TestEnv@2026' testdb -N -e "SELECT COUNT(*) FROM users")
    local order_count=$(mysql -h localhost -P 3306 -u root -p'TestEnv@2026' testdb -N -e "SELECT COUNT(*) FROM orders")
    local inventory_count=$(mysql -h localhost -P 3306 -u root -p'TestEnv@2026' testdb -N -e "SELECT COUNT(*) FROM inventory")
    local finance_count=$(mysql -h localhost -P 3306 -u root -p'TestEnv@2026' testdb -N -e "SELECT COUNT(*) FROM finance_transactions")
    
    log "MySQL数据统计:"
    log "  - 用户: $user_count 条"
    log "  - 订单: $order_count 条"
    log "  - 库存: $inventory_count 条"
    log "  - 财务: $finance_count 条"
    
    # 验证PostgreSQL数据
    local ai_model_count=$(psql -h localhost -p 5432 -U postgres -d testdb -t -c "SELECT COUNT(*) FROM ai_models")
    local alert_rule_count=$(psql -h localhost -p 5432 -U postgres -d testdb -t -c "SELECT COUNT(*) FROM alert_rules")
    
    log "PostgreSQL数据统计:"
    log "  - AI模型: $ai_model_count 条"
    log "  - 告警规则: $alert_rule_count 条"
    
    log "✓ 数据验证完成"
}

# 生成性能测试配置
generate_performance_config() {
    log "生成性能测试配置..."
    
    local config_file="$CONFIG_DIR/test-scenarios.json"
    
    cat > "$config_file" << 'EOF'
{
  "performance_test_scenarios": {
    "environment": {
      "name": "Sprint 27+1 测试环境",
      "version": "1.0.0",
      "description": "性能基准测试环境配置"
    },
    "test_scenarios": [
      {
        "id": "scenario-001",
        "name": "单用户单请求基准测试",
        "description": "建立基础性能基准",
        "concurrent_users": 1,
        "ramp_up_time": 0,
        "duration_seconds": 60,
        "target_endpoints": [
          "/api/users/login",
          "/api/orders/create",
          "/api/inventory/query",
          "/api/finance/transactions"
        ],
        "expected_response_time_ms": 200,
        "expected_error_rate": 0
      },
      {
        "id": "scenario-002",
        "name": "并发用户测试",
        "description": "测试系统并发处理能力",
        "concurrent_users": 100,
        "ramp_up_time": 30,
        "duration_seconds": 300,
        "target_endpoints": [
          "/api/users/login",
          "/api/orders/create",
          "/api/inventory/update"
        ],
        "expected_response_time_ms": 500,
        "expected_error_rate": 0.1
      },
      {
        "id": "scenario-003",
        "name": "持续负载稳定性测试",
        "description": "验证系统在稳定负载下的表现",
        "concurrent_users": 50,
        "ramp_up_time": 60,
        "duration_seconds": 1800,
        "target_endpoints": [
          "/api/users/*",
          "/api/orders/*",
          "/api/inventory/*",
          "/api/finance/*"
        ],
        "expected_response_time_ms": 800,
        "expected_error_rate": 0.5
      },
      {
        "id": "scenario-004",
        "name": "数据库查询性能测试",
        "description": "测试数据库查询性能",
        "concurrent_users": 20,
        "ramp_up_time": 10,
        "duration_seconds": 600,
        "target_endpoints": [
          "/api/users/query",
          "/api/orders/query",
          "/api/inventory/query"
        ],
        "expected_response_time_ms": 100,
        "expected_error_rate": 0
      }
    ],
    "performance_thresholds": {
      "response_time": {
        "acceptable": 200,
        "warning": 500,
        "critical": 1000
      },
      "error_rate": {
        "acceptable": 0.1,
        "warning": 1,
        "critical": 5
      },
      "throughput": {
        "acceptable": 100,
        "warning": 50,
        "critical": 10
      },
      "resource_usage": {
        "cpu_acceptable": 60,
        "cpu_warning": 80,
        "cpu_critical": 90,
        "memory_acceptable": 70,
        "memory_warning": 85,
        "memory_critical": 95
      }
    }
  }
}
EOF
    
    log "✓ 性能测试配置生成完成"
}

# 主函数
main() {
    log "开始性能测试数据准备..."
    
    # 切换到数据目录
    cd "$DATA_DIR"
    
    # 检查数据库连接
    if ! check_db_connection; then
        log "错误: 数据库连接失败，请检查数据库服务状态"
        exit 1
    fi
    
    # 清理现有数据
    cleanup_test_data
    
    # 生成测试数据
    generate_user_data
    generate_order_data
    generate_inventory_data
    generate_finance_data
    
    # 导入数据到数据库
    import_data_to_db
    
    # 验证数据导入
    verify_data_import
    
    # 生成性能测试配置
    generate_performance_config
    
    log "✅ 性能测试数据准备完成"
    log "📊 数据统计:"
    log "  - 测试用户: 1000"
    log "  - 测试订单: 5000"
    log "  - 测试库存: 10000"
    log "  - 测试财务: 10000"
    log ""
    log "🔄 下一步: 等待测试环境服务就绪后执行性能测试"
}

# 执行主函数
main "$@"