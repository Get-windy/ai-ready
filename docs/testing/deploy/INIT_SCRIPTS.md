# 环境初始化脚本说明

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: doc-writer  
**最后更新**: 2026-04-27

## 目录

1. [概述](#概述)
2. [初始化脚本清单](#初始化脚本清单)
3. [数据库初始化脚本](#数据库初始化脚本)
4. [服务配置初始化脚本](#服务配置初始化脚本)
5. [测试数据初始化脚本](#测试数据初始化脚本)
6. [监控告警初始化脚本](#监控告警初始化脚本)
7. [安全配置初始化脚本](#安全配置初始化脚本)
8. [脚本使用说明](#脚本使用说明)
9. [故障排查指南](#故障排查指南)

---

## 概述

### 脚本设计目标

测试环境初始化脚本旨在实现：

1. **自动化部署**: 一键完成环境初始化和配置
2. **标准化配置**: 确保环境配置一致性和可重复性
3. **快速恢复**: 支持环境快速重建和数据恢复
4. **版本管理**: 脚本版本化，支持回滚和追踪
5. **安全合规**: 符合安全最佳实践和合规要求

### 脚本架构

```
/opt/ai-ready/scripts/
├── init/                    # 初始化脚本目录
│   ├── 00-prerequisites.sh   # 前置条件检查
│   ├── 01-database.sh        # 数据库初始化
│   ├── 02-services.sh        # 服务配置
│   ├── 03-data.sh           # 测试数据初始化
│   ├── 04-monitoring.sh     # 监控配置
│   ├── 05-security.sh       # 安全配置
│   └── 99-validation.sh     # 环境验证
├── common/                 # 公共函数库
│   ├── logging.sh          # 日志函数
│   ├── validation.sh       # 验证函数
│   └── utils.sh           # 工具函数
├── config/                 # 配置文件模板
│   ├── database/
│   ├── services/
│   └── monitoring/
└── data/                   # 初始化数据
    ├── sql/
    ├── json/
    └── fixtures/
```

## 初始化脚本清单

### 核心初始化脚本

| 脚本名称 | 功能描述 | 执行顺序 | 执行时间 | 依赖 |
|----------|----------|----------|----------|------|
| `init-prerequisites.sh` | 检查前置条件 | 1 | 1分钟 | 无 |
| `init-database.sh` | 数据库初始化 | 2 | 5-10分钟 | 数据库服务 |
| `init-services.sh` | 服务配置初始化 | 3 | 3-5分钟 | 数据库 |
| `init-test-data.sh` | 测试数据初始化 | 4 | 2-5分钟 | 数据库、服务 |
| `init-monitoring.sh` | 监控告警初始化 | 5 | 3-5分钟 | 服务 |
| `init-security.sh` | 安全配置初始化 | 6 | 2-3分钟 | 服务 |
| `init-validation.sh` | 环境验证 | 7 | 2-3分钟 | 所有服务 |

### 辅助脚本

| 脚本名称 | 功能描述 | 使用场景 |
|----------|----------|----------|
| `reset-environment.sh` | 环境重置 | 环境清理和重建 |
| `backup-environment.sh` | 环境备份 | 定期备份或部署前备份 |
| `restore-environment.sh` | 环境恢复 | 从备份恢复环境 |
| `update-environment.sh` | 环境更新 | 更新环境配置和数据 |
| `health-check.sh` | 健康检查 | 环境状态检查 |

### 一键初始化脚本

```bash
#!/bin/bash
# init-all.sh - 一键初始化测试环境

set -e  # 遇到错误立即退出

echo "=== 开始测试环境初始化 ==="
echo "开始时间: $(date)"
echo ""

# 1. 前置条件检查
echo "步骤1: 检查前置条件..."
./scripts/init/00-prerequisites.sh

# 2. 数据库初始化
echo "步骤2: 初始化数据库..."
./scripts/init/01-database.sh

# 3. 服务配置初始化
echo "步骤3: 初始化服务配置..."
./scripts/init/02-services.sh

# 4. 测试数据初始化
echo "步骤4: 初始化测试数据..."
./scripts/init/03-data.sh

# 5. 监控告警初始化
echo "步骤5: 初始化监控告警..."
./scripts/init/04-monitoring.sh

# 6. 安全配置初始化
echo "步骤6: 初始化安全配置..."
./scripts/init/05-security.sh

# 7. 环境验证
echo "步骤7: 验证环境状态..."
./scripts/init/99-validation.sh

echo ""
echo "=== 测试环境初始化完成 ==="
echo "完成时间: $(date)"
echo "详细日志: /var/log/ai-ready/init-$(date +%Y%m%d).log"
```

## 数据库初始化脚本

### 数据库结构初始化

#### 主初始化脚本
```bash
#!/bin/bash
# init-database.sh - 数据库初始化脚本

set -e

# 导入公共函数
source ./scripts/common/logging.sh
source ./scripts/common/validation.sh

log_info "开始数据库初始化"

# 检查数据库连接
log_info "检查数据库连接..."
check_database_connection() {
    if ! docker exec test-env-postgres pg_isready -U postgres > /dev/null 2>&1; then
        log_error "数据库连接失败"
        return 1
    fi
    log_success "数据库连接正常"
}

# 创建数据库
log_info "创建数据库..."
create_database() {
    local db_name="ai_ready_test"
    
    docker exec test-env-postgres psql -U postgres -c "SELECT 1 FROM pg_database WHERE datname = '${db_name}'" | grep -q 1
    if [ $? -eq 0 ]; then
        log_info "数据库 ${db_name} 已存在"
    else
        docker exec test-env-postgres psql -U postgres -c "CREATE DATABASE ${db_name};"
        log_success "数据库 ${db_name} 创建成功"
    fi
}

# 执行SQL脚本
log_info "执行数据库表结构脚本..."
execute_sql_scripts() {
    local sql_dir="./scripts/config/database/sql"
    
    for sql_file in "${sql_dir}"/*.sql; do
        if [ -f "$sql_file" ]; then
            log_info "执行SQL脚本: $(basename "$sql_file")"
            docker exec -i test-env-postgres psql -U postgres -d ai_ready_test < "$sql_file"
            if [ $? -eq 0 ]; then
                log_success "SQL脚本执行成功: $(basename "$sql_file")"
            else
                log_error "SQL脚本执行失败: $(basename "$sql_file")"
                return 1
            fi
        fi
    done
}

# 创建数据库用户和权限
log_info "配置数据库用户和权限..."
setup_database_users() {
    local users=(
        "app_user:app_password:READWRITE"
        "readonly_user:readonly_password:READONLY"
        "migration_user:migration_password:ALL"
    )
    
    for user_info in "${users[@]}"; do
        IFS=':' read -r username password permissions <<< "$user_info"
        
        # 创建用户
        docker exec test-env-postgres psql -U postgres -d ai_ready_test \
            -c "DO \$\$
            BEGIN
                IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '${username}') THEN
                    CREATE USER ${username} WITH PASSWORD '${password}';
                END IF;
            END
            \$\$;"
        
        # 分配权限
        case $permissions in
            READWRITE)
                docker exec test-env-postgres psql -U postgres -d ai_ready_test \
                    -c "GRANT CONNECT ON DATABASE ai_ready_test TO ${username};
                        GRANT USAGE ON SCHEMA public TO ${username};
                        GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO ${username};
                        GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO ${username};"
                ;;
            READONLY)
                docker exec test-env-postgres psql -U postgres -d ai_ready_test \
                    -c "GRANT CONNECT ON DATABASE ai_ready_test TO ${username};
                        GRANT USAGE ON SCHEMA public TO ${username};
                        GRANT SELECT ON ALL TABLES IN SCHEMA public TO ${username};"
                ;;
            ALL)
                docker exec test-env-postgres psql -U postgres -d ai_ready_test \
                    -c "GRANT ALL PRIVILEGES ON DATABASE ai_ready_test TO ${username};
                        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ${username};
                        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ${username};"
                ;;
        esac
        
        log_success "数据库用户 ${username} 配置完成"
    done
}

# 创建索引
log_info "创建数据库索引..."
create_indexes() {
    local index_sql="./scripts/config/database/indexes.sql"
    
    if [ -f "$index_sql" ]; then
        log_info "创建数据库索引..."
        docker exec -i test-env-postgres psql -U postgres -d ai_ready_test < "$index_sql"
        log_success "数据库索引创建完成"
    fi
}

# 配置数据库参数
log_info "配置数据库参数..."
configure_database_parameters() {
    log_info "配置数据库性能参数..."
    
    # 调整共享缓冲区
    docker exec test-env-postgres psql -U postgres \
        -c "ALTER SYSTEM SET shared_buffers = '256MB';"
    
    # 调整工作内存
    docker exec test-env-postgres psql -U postgres \
        -c "ALTER SYSTEM SET work_mem = '16MB';"
    
    # 启用扩展统计
    docker exec test-env-postgres psql -U postgres \
        -c "ALTER SYSTEM SET track_activities = on;
            ALTER SYSTEM SET track_counts = on;
            ALTER SYSTEM SET track_io_timing = on;"
    
    # 重启数据库使配置生效
    docker-compose -f docker-compose.test.yml restart postgres
    sleep 10
    
    log_success "数据库参数配置完成"
}

# 主执行流程
main() {
    log_info "=== 数据库初始化开始 ==="
    
    check_database_connection
    create_database
    execute_sql_scripts
    setup_database_users
    create_indexes
    configure_database_parameters
    
    log_info "=== 数据库初始化完成 ==="
    
    # 输出数据库信息
    log_info "数据库信息:"
    docker exec test-env-postgres psql -U postgres -d ai_ready_test \
        -c "SELECT datname, pg_size_pretty(pg_database_size(datname)) as size, 
                   datconnlimit, encoding 
            FROM pg_database 
            WHERE datname = 'ai_ready_test';"
}

# 执行主函数
main "$@"
```

#### 数据库表结构脚本示例
```sql
-- scripts/config/database/sql/01-tables.sql
-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    phone VARCHAR(50),
    avatar_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED')),
    last_login_at TIMESTAMP,
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INT DEFAULT 1
);

-- 用户角色表
CREATE TABLE IF NOT EXISTS user_roles (
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by VARCHAR(36),
    PRIMARY KEY (user_id, role)
);

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(36) PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id),
    total_amount DECIMAL(15,2) NOT NULL CHECK (total_amount >= 0),
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'COMPLETED', 'CANCELLED', 'REFUNDED')),
    payment_method VARCHAR(50),
    payment_status VARCHAR(20),
    shipping_address JSONB,
    billing_address JSONB,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    version INT DEFAULT 1
);

-- 订单项表
CREATE TABLE IF NOT EXISTS order_items (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id VARCHAR(36) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL CHECK (unit_price >= 0),
    quantity INT NOT NULL CHECK (quantity > 0),
    subtotal DECIMAL(15,2) NOT NULL CHECK (subtotal >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 产品库存表
CREATE TABLE IF NOT EXISTS inventory (
    id VARCHAR(36) PRIMARY KEY,
    product_id VARCHAR(36) UNIQUE NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    warehouse_id VARCHAR(36) NOT NULL,
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved_quantity INT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    available_quantity INT GENERATED ALWAYS AS (quantity - reserved_quantity) STORED,
    min_stock_level INT DEFAULT 10,
    max_stock_level INT DEFAULT 1000,
    last_restocked_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 库存变更历史表
CREATE TABLE IF NOT EXISTS inventory_history (
    id VARCHAR(36) PRIMARY KEY,
    inventory_id VARCHAR(36) NOT NULL REFERENCES inventory(id),
    change_type VARCHAR(50) NOT NULL CHECK (change_type IN ('PURCHASE', 'SALE', 'RETURN', 'ADJUSTMENT', 'TRANSFER')),
    quantity_change INT NOT NULL,
    previous_quantity INT NOT NULL,
    new_quantity INT NOT NULL,
    reference_id VARCHAR(36),
    reason VARCHAR(500),
    operated_by VARCHAR(36),
    operated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 支付记录表
CREATE TABLE IF NOT EXISTS payments (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL REFERENCES orders(id),
    payment_number VARCHAR(50) UNIQUE NOT NULL,
    amount DECIMAL(15,2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) DEFAULT 'CNY',
    payment_method VARCHAR(50) NOT NULL,
    payment_gateway VARCHAR(50),
    gateway_transaction_id VARCHAR(100),
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'REFUNDED')),
    payer_info JSONB,
    gateway_response JSONB,
    paid_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_inventory_product_id ON inventory(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_warehouse_id ON inventory(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inventory_history_inventory_id ON inventory_history(inventory_id);
CREATE INDEX IF NOT EXISTS idx_inventory_history_operated_at ON inventory_history(operated_at);
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
```

#### 数据库索引优化脚本
```sql
-- scripts/config/database/indexes.sql
-- 性能优化索引
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_users_last_login_at ON users(last_login_at DESC);

-- 订单查询优化索引
CREATE INDEX IF NOT EXISTS idx_orders_user_status ON orders(user_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_created_completed ON orders(created_at, completed_at) WHERE completed_at IS NOT NULL;

-- 库存查询优化索引
CREATE INDEX IF NOT EXISTS idx_inventory_quantity ON inventory(quantity) WHERE quantity < min_stock_level;
CREATE INDEX IF NOT EXISTS idx_inventory_updated_at ON inventory(updated_at DESC);

-- 支付查询优化索引
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payments_paid_at ON payments(paid_at DESC) WHERE paid_at IS NOT NULL;

-- 全文搜索索引（如果需要）
-- CREATE INDEX idx_users_search ON users USING gin(to_tsvector('english', username || ' ' || email || ' ' || full_name));

-- 复合索引优化
CREATE INDEX IF NOT EXISTS idx_orders_comprehensive ON orders(user_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_inventory_comprehensive ON inventory(warehouse_id, product_id, quantity);

-- 外键索引（如果不存在）
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_history_reference_id ON inventory_history(reference_id);
```

### 数据库数据初始化

#### 基础数据初始化脚本
```bash
#!/bin/bash
# init-data.sh - 数据库数据初始化脚本

set -e

source ./scripts/common/logging.sh

log_info "开始数据库数据初始化"

# 插入基础数据
insert_basic_data() {
    log_info "插入基础数据..."
    
    # 插入测试用户
    docker exec -i test-env-postgres psql -U postgres -d ai_ready_test << EOF
        -- 插入管理员用户
        INSERT INTO users (id, username, email, password_hash, full_name, status) 
        VALUES (
            'admin_001',
            'admin@ai-ready.local',
            'admin@ai-ready.local',
            '\$2a\$10\$YourHashedPasswordHere', -- 密码: Admin@2024
            '系统管理员',
            'ACTIVE'
        ) ON CONFLICT (username) DO NOTHING;
        
        -- 插入测试用户
        INSERT INTO users (id, username, email, password_hash, full_name, status) 
        VALUES 
            ('user_001', 'tester@ai-ready.local', 'tester@ai-ready.local', '\$2a\$10\$TestHashedPassword', '测试工程师', 'ACTIVE'),
            ('user_002', 'developer@ai-ready.local', 'developer@ai-ready.local', '\$2a\$10\$DevHashedPassword', '开发工程师', 'ACTIVE'),
            ('user_003', 'qa@ai-ready.local', 'qa@ai-ready.local', '\$2a\$10\$QAHashedPassword', '质量工程师', 'ACTIVE')
        ON CONFLICT (username) DO NOTHING;
        
        -- 分配用户角色
        INSERT INTO user_roles (user_id, role) 
        VALUES 
            ('admin_001', 'ROLE_ADMIN'),
            ('user_001', 'ROLE_TESTER'),
            ('user_002', 'ROLE_DEVELOPER'),
            ('user_003', 'ROLE_QA')
        ON CONFLICT (user_id, role) DO NOTHING;
        
        -- 插入测试产品
        INSERT INTO inventory (id, product_id, product_name, warehouse_id, quantity, min_stock_level, max_stock_level)
        VALUES 
            ('inv_001', 'prod_001', '测试产品A', 'wh_001', 1000, 50, 5000),
            ('inv_002', 'prod_002', '测试产品B', 'wh_001', 500, 20, 2000),
            ('inv_003', 'prod_003', '测试产品C', 'wh_001', 200, 10, 1000),
            ('inv_004', 'prod_004', '测试产品D', 'wh_001', 100, 5, 500)
        ON CONFLICT (product_id) DO NOTHING;
EOF
    
    log_success "基础数据插入完成"
}

# 生成测试订单数据
generate_test_orders() {
    log_info "生成测试订单数据..."
    
    # 使用Python脚本生成测试数据
    python3 ./scripts/data/generate_test_orders.py \
        --user-count 10 \
        --order-count 100 \
        --product-count 20 \
        --output ./scripts/data/generated_orders.sql
    
    # 执行生成的SQL
    if [ -f "./scripts/data/generated_orders.sql" ]; then
        docker exec -i test-env-postgres psql -U postgres -d ai_ready_test < ./scripts/data/generated_orders.sql
        log_success "测试订单数据生成完成"
    else
        log_warning "测试订单数据生成失败，跳过此步骤"
    fi
}

# 生成测试支付数据
generate_test_payments() {
    log_info "生成测试支付数据..."
    
    python3 ./scripts/data/generate_test_payments.py \
        --order-count 100 \
        --output ./scripts/data/generated_payments.sql
    
    if [ -f "./scripts/data/generated_payments.sql" ]; then
        docker exec -i test-env-postgres psql -U postgres -d ai_ready_test < ./scripts/data/generated_payments.sql
        log_success "测试支付数据生成完成"
    fi
}

# 生成库存历史数据
generate_inventory_history() {
    log_info "生成库存历史数据..."
    
    python3 ./scripts/data/generate_inventory_history.py \
        --days 30 \
        --transactions 1000 \
        --output ./scripts/data/generated_inventory_history.sql
    
    if [ -f "./scripts/data/generated_inventory_history.sql" ]; then
        docker exec -i test-env-postgres psql -U postgres -d ai_ready_test < ./scripts/data/generated_inventory_history.sql
        log_success "库存历史数据生成完成"
    fi
}

# 数据验证
validate_data() {
    log_info "验证数据完整性..."
    
    local validation_result=$(docker exec test-env-postgres psql -U postgres -d ai_ready_test -t << EOF
        -- 检查用户数据
        SELECT '用户数量: ' || COUNT(*) FROM users;
        SELECT '活跃用户: ' || COUNT(*) FROM users WHERE status = 'ACTIVE';
        
        -- 检查产品数据
        SELECT '产品数量: ' || COUNT(*) FROM inventory;
        SELECT '低库存产品: ' || COUNT(*) FROM inventory WHERE quantity < min_stock_level;
        
        -- 检查订单数据
        SELECT '订单数量: ' || COUNT(*) FROM orders;
        SELECT '各状态订单: ' || status || ': ' || COUNT(*) FROM orders GROUP BY status;
        
        -- 检查支付数据
        SELECT '支付记录: ' || COUNT(*) FROM payments;
        SELECT '成功支付: ' || COUNT(*) FROM payments WHERE status = 'SUCCESS';
EOF
    )
    
    log_info "数据验证结果:"
    echo "$validation_result"
    
    # 检查关键数据是否存在
    local user_count=$(docker exec test-env-postgres psql -U postgres -d ai_ready_test -t -c "SELECT COUNT(*) FROM users;" | tr -d '[:space:]')
    if [ "$user_count" -ge 4 ]; then
        log_success "数据完整性验证通过"
    else
        log_error "数据完整性验证失败"
        return 1
    fi
}

# 主执行流程
main() {
    log_info "=== 数据库数据初始化开始 ==="
    
    insert_basic_data
    generate_test_orders
    generate_test_payments
    generate_inventory_history
    validate_data
    
    log_info "=== 数据库数据初始化完成 ==="
    
    # 输出数据统计
    log_info "数据统计:"
    docker exec test-env-postgres psql -U postgres -d ai_ready_test << EOF
        SELECT '用户' as category, COUNT(*) as count FROM users
        UNION ALL
        SELECT '订单', COUNT(*) FROM orders
        UNION ALL
        SELECT '产品', COUNT(*) FROM inventory
        UNION ALL
        SELECT '支付', COUNT(*) FROM payments
        ORDER BY category;
EOF
}

main "$@"
```

## 服务配置初始化脚本

### 服务配置文件初始化

#### 应用服务配置
```bash
#!/bin/bash
# init-services.sh - 服务配置初始化脚本

set -e

source ./scripts/common/logging.sh

log_info "开始服务配置初始化"

# 创建配置目录
create_config_directories() {
    log_info "创建配置目录..."
    
    local config_dirs=(
        "/opt/ai-ready/config"
        "/opt/ai-ready/config/application"
        "/opt/ai-ready/config/database"
        "/opt/ai-ready/config/security"
        "/opt/ai-ready/config/monitoring"
        "/opt/ai-ready/config/cache"
        "/opt/ai-ready/config/queue"
    )
    
    for dir in "${config_dirs[@]}"; do
        if [ ! -d "$dir" ]; then
            sudo mkdir -p "$dir"
            sudo chown -R $(whoami):$(whoami) "$dir"
            log_success "创建目录: $dir"
        fi
    done
}

# 生成应用配置文件
generate_application_config() {
    log_info "生成应用配置文件..."
    
    cat > /opt/ai-ready/config/application/application-test.yml << EOF
# 测试环境应用配置
spring:
  application:
    name: ai-ready-test
  profiles:
    active: test
    
  # 数据源配置
  datasource:
    url: jdbc:postgresql://postgres:5432/ai_ready_test
    username: app_user
    password: app_password
    driver-class-name: org.postgresql.Driver
    hikari:
      connection-timeout: 30000
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-test-query: SELECT 1
      pool-name: TestHikariPool
  
  # JPA配置
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
        generate_statistics: false
        cache:
          use_second_level_cache: false
          use_query_cache: false
  
  # Redis配置
  redis:
    host: redis
    port: 6379
    password: 
    database: 0
    timeout: 10000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: -1ms
  
  # 缓存配置
  cache:
    type: redis
    redis:
      time-to-live: 300000
      cache-null-values: false
      key-prefix: "test:cache:"
  
  # 安全配置
  security:
    jwt:
      secret: test-jwt-secret-key-2026-ai-ready-application
      expiration: 3600
      refresh-expiration: 86400
  
  # 服务发现配置
  cloud:
    loadbalancer:
      enabled: true
  
  # 监控配置
  management:
    endpoints:
      web:
        exposure:
          include: health,info,metrics,prometheus
        base-path: /actuator
    endpoint:
      health:
        show-details: always
        show-components: always
    metrics:
      export:
        prometheus:
          enabled: true
      tags:
        application: ai-ready-test
        environment: test

# 应用特定配置
app:
  test:
    environment: test
    data:
      initialization:
        enabled: true
        mode: always
    security:
      cors:
        allowed-origins: "*"
        allowed-methods: "*"
        allowed-headers: "*"
      rate-limiting:
        enabled: true
        requests-per-second: 100
    logging:
      level:
        com.aiready: DEBUG
        org.springframework: WARN
      file:
        path: /var/log/ai-ready
        name: application-test.log
        max-size: 100MB
        max-history: 30
    api:
      version: v1
      base-path: /api/v1
      documentation:
        enabled: true
        swagger-ui:
          enabled: true
          path: /swagger-ui.html
    business:
      order:
        auto-cancel-hours: 24
        max-items-per-order: 50
      payment:
        timeout-minutes: 30
        retry-count: 3
      inventory:
        low-stock-threshold: 20
        out-of-stock-threshold: 5

# 测试专用配置
test:
  mode: true
  features:
    bypass-auth: false
    mock-external-services: true
    fast-data-generation: true
  users:
    admin:
      username: admin@ai-ready.local
      password: Admin@2024
    tester:
      username: tester@ai-ready.local
      password: Tester@2024
  data:
    preload:
      enabled: true
      users: 10
      products: 50
      orders: 100
      payments: 80
EOF
    
    log_success "应用配置文件生成完成"
}

# 生成数据库配置
generate_database_config() {
    log_info "生成数据库配置文件..."
    
    cat > /opt/ai-ready/config/database/postgresql.conf << EOF
# PostgreSQL测试环境配置

# 连接和认证
listen_addresses = '*'
port = 5432
max_connections = 200
superuser_reserved_connections = 3

# 内存配置
shared_buffers = 256MB
work_mem = 16MB
maintenance_work_mem = 64MB
effective_cache_size = 1GB

# 预写日志
wal_level = replica
fsync = on
synchronous_commit = on
wal_buffers = 16MB
checkpoint_timeout = 5min
checkpoint_completion_target = 0.9

# 查询规划
random_page_cost = 1.1
effective_io_concurrency = 200
default_statistics_target = 100

# 日志配置
logging_collector = on
log_destination = 'stderr'
log_directory = '/var/log/postgresql'
log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'
log_rotation_age = 1d
log_rotation_size = 100MB
log_min_duration_statement = 1000
log_checkpoints = on
log_connections = on
log_disconnections = on
log_lock_waits = on
log_temp_files = 0

# 统计收集
track_activities = on
track_counts = on
track_io_timing = on
track_functions = all
shared_preload_libraries = 'pg_stat_statements'
pg_stat_statements.max = 10000
pg_stat_statements.track = all

# 自动清理
autovacuum = on
log_autovacuum_min_duration = 0
autovacuum_max_workers = 3
autovacuum_naptime = 1min
autovacuum_vacuum_threshold = 50
autovacuum_analyze_threshold = 50
autovacuum_vacuum_scale_factor = 0.2
autovacuum_analyze_scale_factor = 0.1
EOF
    
    cat > /opt/ai-ready/config/database/pg_hba.conf << EOF
# PostgreSQL客户端认证配置
# TYPE  DATABASE        USER            ADDRESS                 METHOD

# 本地连接
local   all             all                                     trust
host    all             all             127.0.0.1/32            trust
host    all             all             ::1/128                 trust

# Docker网络连接
host    all             all             172.0.0.0/8             md5
host    all             all             192.168.0.0/16          md5

# 应用服务连接
host    ai_ready_test   app_user        172.0.0.0/8             md5
host    ai_ready_test   readonly_user   172.0.0.0/8             md5

# 复制连接（如果需要）
# host    replication     replicator      172.0.0.0/8             md5
EOF
    
    log_success "数据库配置文件生成完成"
}

# 生成Redis配置
generate_redis_config() {
    log_info "生成Redis配置文件..."
    
    cat > /opt/ai-ready/config/cache/redis.conf << EOF
# Redis测试环境配置

# 基础配置
bind 0.0.0.0
port 6379
protected-mode no
daemonize no
pidfile /var/run/redis_6379.pid

# 日志配置
loglevel notice
logfile "/var/log/redis/redis-server.log"
syslog-enabled no

# 持久化配置
save 900 1
save 300 10
save 60 10000
stop-writes-on-bgsave-error yes
rdbcompression yes
rdbchecksum yes
dbfilename dump.rdb
dir /data

# 内存配置
maxmemory 1gb
maxmemory-policy allkeys-lru
maxmemory-samples 5

# 慢日志
slowlog-log-slower-than 10000
slowlog-max-len 128

# 客户端配置
timeout 0
tcp-keepalive 300

# 高级配置
hash-max-ziplist-entries 512
hash-max-ziplist-value 64
list-max-ziplist-size -2
list-compress-depth 0
set-max-intset-entries 512
zset-max-ziplist-entries 128
zset-max-ziplist-value 64
hll-sparse-max-bytes 3000
activerehashing yes
client-output-buffer-limit normal 0 0 0
client-output-buffer-limit slave 256mb 64mb 60
client-output-buffer-limit pubsub 32mb 8mb 60

# 监控配置
latency-monitor-threshold 100

# 集群配置（单节点）
cluster-enabled no
EOF
    
    log_success "Redis配置文件生成完成"
}

# 生成Nginx配置
generate_nginx_config() {
    log_info "生成Nginx配置文件..."
    
    cat > /opt/ai-ready/config/nginx/nginx.conf << EOF
# Nginx测试环境配置

user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/