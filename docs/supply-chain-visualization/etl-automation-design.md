# ETL自动化方案设计

## 1. 总体架构设计

### 1.1 架构目标
- 实现供应链数据的自动化抽取、转换、加载
- 支持实时和批量两种数据处理模式
- 保证数据质量，建立完整的监控体系
- 提供可扩展、可维护的ETL管道

### 1.2 技术架构图
```
[ERP数据库] → [CDC捕获] → [消息队列] → [流处理] → [实时存储]
    ↓                               ↓             ↓
[批量抽取] → [ETL作业] → [数据仓库] → [分析引擎] → [可视化]
```

### 1.3 核心组件
1. **数据抽取层**: Debezium(CDC) + Airflow(批量)
2. **消息队列层**: Apache Kafka
3. **数据处理层**: Apache Flink/Spark
4. **数据存储层**: ClickHouse(实时) + PostgreSQL(分析)
5. **调度监控层**: Apache Airflow + Prometheus

## 2. 实时数据处理管道

### 2.1 CDC数据捕获配置
```yaml
# debezium-connector-config.yml
connector:
  name: erp-cdc-connector
  connector.class: io.debezium.connector.postgresql.PostgresConnector
  database.hostname: erp-db-host
  database.port: 5432
  database.user: cdc_user
  database.password: ${CDC_PASSWORD}
  database.dbname: erp_db
  table.include.list: public.*
  snapshot.mode: initial
  slot.name: debezium_slot
```

### 2.2 Kafka主题设计
| 主题名称 | 分区数 | 副本数 | 保留策略 | 数据源 |
|----------|--------|--------|----------|--------|
| erp.purchase.order | 3 | 2 | 7天 | 采购订单表 |
| erp.stock.transaction | 5 | 2 | 7天 | 库存流水表 |
| erp.sales.order | 3 | 2 | 7天 | 销售订单表 |
| erp.supplier.performance | 1 | 2 | 30天 | 供应商绩效 |
| erp.data.quality.alerts | 1 | 2 | 14天 | 数据质量告警 |

### 2.3 Flink实时处理作业
```java
// 实时库存监控作业
public class RealtimeStockMonitoringJob {
    
    // 主要处理逻辑：
    // 1. 从Kafka读取库存变更事件
    // 2. 计算实时库存量
    // 3. 检测库存异常（低于安全库存）
    // 4. 输出到ClickHouse和告警系统
    
    @Override
    public void execute() {
        // 实现实时库存计算和监控
    }
}
```

## 3. 批量ETL设计

### 3.1 Airflow DAG设计
```python
# supply_chain_etl_dag.py
from airflow import DAG
from airflow.operators.python_operator import PythonOperator
from datetime import datetime, timedelta

default_args = {
    'owner': 'data_team',
    'depends_on_past': False,
    'start_date': datetime(2026, 5, 4),
    'email_on_failure': True,
    'email_on_retry': False,
    'retries': 3,
    'retry_delay': timedelta(minutes=5),
}

dag = DAG(
    'supply_chain_etl',
    default_args=default_args,
    description='供应链数据ETL流程',
    schedule_interval='0 2 * * *',  # 每天凌晨2点执行
    catchup=False,
)

def extract_purchase_data(**context):
    """抽取采购数据"""
    pass

def extract_stock_data(**context):
    """抽取库存数据"""
    pass

def extract_sales_data(**context):
    """抽取销售数据"""
    pass

def transform_data(**context):
    """数据转换"""
    pass

def load_to_data_warehouse(**context):
    """加载到数据仓库"""
    pass

def run_data_quality_checks(**context):
    """数据质量检查"""
    pass

# 定义任务
extract_purchase_task = PythonOperator(
    task_id='extract_purchase_data',
    python_callable=extract_purchase_data,
    dag=dag,
)

extract_stock_task = PythonOperator(
    task_id='extract_stock_data',
    python_callable=extract_stock_data,
    dag=dag,
)

extract_sales_task = PythonOperator(
    task_id='extract_sales_data',
    python_callable=extract_sales_data,
    dag=dag,
)

transform_task = PythonOperator(
    task_id='transform_data',
    python_callable=transform_data,
    dag=dag,
)

load_task = PythonOperator(
    task_id='load_to_data_warehouse',
    python_callable=load_to_data_warehouse,
    dag=dag,
)

quality_task = PythonOperator(
    task_id='run_data_quality_checks',
    python_callable=run_data_quality_checks,
    dag=dag,
)

# 定义依赖关系
extract_purchase_task >> transform_task
extract_stock_task >> transform_task
extract_sales_task >> transform_task
transform_task >> load_task >> quality_task
```

### 3.2 数据抽取策略

#### 3.2.1 全量抽取（首次或重建）
- 适用场景：首次数据迁移、数据仓库重建
- 执行频率：按需执行
- 数据量：全部历史数据
- 技术：PostgreSQL COPY命令、Sqoop

#### 3.2.2 增量抽取（日常）
- 适用场景：日常数据同步
- 执行频率：每小时/每天
- 增量字段：updated_at时间戳
- 技术：基于时间戳的WHERE条件

#### 3.2.3 变化数据捕获（实时）
- 适用场景：实时数据同步
- 执行频率：实时
- 技术：Debezium CDC
- 延迟：秒级

### 3.3 数据转换规则

#### 3.3.1 清洗规则
```sql
-- 数据清洗示例
WITH cleaned_data AS (
    SELECT 
        order_id,
        -- 去除空格
        TRIM(customer_name) AS customer_name,
        -- 标准化日期格式
        TO_DATE(order_date, 'YYYY-MM-DD') AS order_date,
        -- 处理空值
        COALESCE(order_amount, 0) AS order_amount,
        -- 统一状态编码
        CASE order_status
            WHEN 'pending' THEN 'PENDING'
            WHEN 'processing' THEN 'PROCESSING'
            WHEN 'shipped' THEN 'SHIPPED'
            ELSE 'UNKNOWN'
        END AS order_status
    FROM raw_orders
    WHERE 
        -- 过滤无效数据
        order_id IS NOT NULL
        AND order_date >= '2020-01-01'
)
```

#### 3.3.2 聚合规则
```sql
-- 销售数据聚合
CREATE VIEW daily_sales_summary AS
SELECT
    DATE(order_date) AS sale_date,
    product_category,
    COUNT(DISTINCT order_id) AS order_count,
    SUM(order_quantity) AS total_quantity,
    SUM(order_amount) AS total_amount,
    AVG(order_amount) AS avg_order_value
FROM cleaned_orders
WHERE order_status = 'COMPLETED'
GROUP BY DATE(order_date), product_category;
```

#### 3.3.3 数据关联
```sql
-- 供应链全链路关联
CREATE VIEW supply_chain_full_view AS
SELECT
    po.order_id AS purchase_order_id,
    po.supplier_id,
    s.supplier_name,
    po.order_date AS purchase_date,
    sr.receipt_id,
    sr.receipt_date,
    sr.received_quantity,
    si.stock_id,
    si.current_quantity,
    so.order_id AS sales_order_id,
    so.customer_id,
    c.customer_name,
    so.order_date AS sales_date
FROM purchase_orders po
LEFT JOIN suppliers s ON po.supplier_id = s.supplier_id
LEFT JOIN stock_receipts sr ON po.order_id = sr.purchase_order_id
LEFT JOIN stock_inventory si ON sr.product_id = si.product_id
LEFT JOIN sales_orders so ON si.product_id = so.product_id
LEFT JOIN customers c ON so.customer_id = c.customer_id;
```

## 4. 数据质量监控设计

### 4.1 质量检查点
```python
# data_quality_checks.py
class DataQualityValidator:
    
    def check_completeness(self, df, required_columns):
        """检查数据完整性"""
        missing_columns = [col for col in required_columns if col not in df.columns]
        return len(missing_columns) == 0, missing_columns
    
    def check_accuracy(self, df, validation_rules):
        """检查数据准确性"""
        errors = []
        for rule in validation_rules:
            if not rule.validate(df):
                errors.append(rule.error_message)
        return len(errors) == 0, errors
    
    def check_consistency(self, df1, df2, key_columns):
        """检查数据一致性"""
        # 检查两个数据集的关键字段是否一致
        pass
    
    def check_timeliness(self, data_timestamp, expected_delay):
        """检查数据时效性"""
        current_time = datetime.now()
        delay = (current_time - data_timestamp).total_seconds()
        return delay <= expected_delay, delay
```

### 4.2 监控指标定义
| 指标类别 | 指标名称 | 计算方法 | 告警阈值 |
|----------|----------|----------|----------|
| 完整性 | 空值率 | 空值记录数/总记录数 | >5% |
| 准确性 | 数据错误率 | 错误记录数/总记录数 | >1% |
| 一致性 | 关联完整性 | 关联失败记录数/总记录数 | >0% |
| 时效性 | 数据延迟 | 当前时间-数据时间戳 | >5分钟 |
| 可用性 | 任务成功率 | 成功任务数/总任务数 | <95% |

### 4.3 告警规则配置
```yaml
# alert_rules.yml
alert_rules:
  - name: data_completeness_alert
    type: completeness
    metric: null_value_rate
    threshold: 0.05
    severity: warning
    notification_channels: [email, slack]
    
  - name: data_accuracy_alert
    type: accuracy
    metric: error_rate
    threshold: 0.01
    severity: critical
    notification_channels: [email, slack, phone]
    
  - name: data_timeliness_alert
    type: timeliness
    metric: data_delay_minutes
    threshold: 5
    severity: warning
    notification_channels: [email]
```

## 5. 性能优化策略

### 5.1 抽取性能优化
1. **并行抽取**: 多表并行抽取
2. **增量优化**: 基于时间范围的分区抽取
3. **连接优化**: 使用连接池，合理设置超时时间

### 5.2 转换性能优化
1. **内存优化**: 合理设置批次大小
2. **算法优化**: 使用高效的数据处理算法
3. **缓存优化**: 热点数据缓存

### 5.3 加载性能优化
1. **批量加载**: 使用批量INSERT语句
2. **索引管理**: 加载前禁用索引，加载后重建
3. **分区策略**: 按时间分区管理大表

## 6. 容错与恢复机制

### 6.1 错误处理策略
1. **重试机制**: 可配置的重试次数和间隔
2. **错误隔离**: 一个任务失败不影响其他任务
3. **错误记录**: 详细记录错误信息和上下文

### 6.2 数据恢复机制
1. **检查点**: 定期保存处理状态
2. **重放机制**: 支持从检查点重新处理
3. **数据修复**: 提供数据修复工具和流程

### 6.3 监控与告警
1. **实时监控**: Prometheus + Grafana
2. **日志收集**: ELK Stack
3. **告警通知**: 邮件、Slack、短信

## 7. 部署与运维

### 7.1 环境配置
```docker
# docker-compose.yml
version: '3.8'
services:
  kafka:
    image: confluentinc/cp-kafka:latest
    ports:
      - "9092:9092"
    
  airflow:
    image: apache/airflow:latest
    ports:
      - "8080:8080"
    
  flink:
    image: flink:latest
    ports:
      - "8081:8081"
    
  clickhouse:
    image: clickhouse/clickhouse-server:latest
    ports:
      - "8123:8123"
      - "9000:9000"
```

### 7.2 配置管理
1. **环境变量**: 敏感信息通过环境变量配置
2. **配置文件**: 使用配置中心管理配置
3. **版本控制**: 所有配置纳入版本控制

### 7.3 运维监控
1. **健康检查**: 定期检查各组件健康状态
2. **性能监控**: 监控系统资源使用情况
3. **容量规划**: 基于数据增长趋势进行容量规划

## 8. 实施计划

### 8.1 第一阶段（1周）
1. 搭建基础ETL环境
2. 实现核心数据抽取
3. 建立基础数据质量检查

### 8.2 第二阶段（2周）
1. 完善实时数据处理
2. 建立完整的数据转换规则
3. 实现数据质量监控

### 8.3 第三阶段（1周）
1. 性能优化和调优
2. 完善监控告警体系
3. 文档编写和培训

---

**设计完成时间**: 2026-05-04  
**设计者**: AI-Ready团队  
**版本**: 1.0