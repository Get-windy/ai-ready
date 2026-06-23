# 手动执行数据库迁移说明

## 概述
此文档说明了如何手动执行销售订单表单生产级重构所需的数据库变更。

## 数据库迁移脚本

以下是要执行的SQL语句，用于添加价格等级相关字段到销售订单明细表：

```sql
-- 1. 添加价格等级相关字段
ALTER TABLE erp_sale_order_item
ADD COLUMN IF NOT EXISTS customer_grade_code VARCHAR(20),
ADD COLUMN IF NOT EXISTS customer_grade_name VARCHAR(100),
ADD COLUMN IF NOT EXISTS price_grade_code VARCHAR(20),
ADD COLUMN IF NOT EXISTS price_source VARCHAR(50),
ADD COLUMN IF NOT EXISTS calculated_price DECIMAL(18,6),
ADD COLUMN IF NOT EXISTS discount_applied JSON;

-- 2. 添加索引以优化查询性能
CREATE INDEX IF NOT EXISTS idx_soi_customer_grade ON erp_sale_order_item(customer_grade_code);
CREATE INDEX IF NOT EXISTS idx_soi_price_grade ON erp_sale_order_item(price_grade_code);
CREATE INDEX IF NOT EXISTS idx_soi_product_id ON erp_sale_order_item(product_id);
CREATE INDEX IF NOT EXISTS idx_soi_order_id ON erp_sale_order_item(order_id);

-- 3. 更新表注释
COMMENT ON TABLE erp_sale_order_item IS '销售订单明细表 - 生产级分布式存储';

-- 4. 为新增字段添加注释
COMMENT ON COLUMN erp_sale_order_item.customer_grade_code IS '客户等级代码 - 快照存储';
COMMENT ON COLUMN erp_sale_order_item.customer_grade_name IS '客户等级名称 - 快照存储';
COMMENT ON COLUMN erp_sale_order_item.price_grade_code IS '价格等级代码 - 快照存储';
COMMENT ON COLUMN erp_sale_order_item.price_source IS '价格来源 - 快照存储';
COMMENT ON COLUMN erp_sale_order_item.calculated_price IS '计算得出的单价 - 快照存储';
COMMENT ON COLUMN erp_sale_order_item.discount_applied IS '应用的折扣信息(JSON) - 快照存储';

-- 5. 为现有重要字段添加注释（补充说明分布策略）
COMMENT ON COLUMN erp_sale_order_item.image IS '图片 - 通过产品ID关联获取';
COMMENT ON COLUMN erp_sale_order_item.barcode IS '条码 - 通过产品ID关联获取';
COMMENT ON COLUMN erp_sale_order_item.origin IS '产地 - 通过产品ID关联获取';
COMMENT ON COLUMN erp_sale_order_item.brand IS '品牌 - 通过产品ID关联获取';
COMMENT ON COLUMN erp_sale_order_item.retail_price IS '零售价 - 通过产品ID关联获取';
COMMENT ON COLUMN erp_sale_order_item.wholesale_price IS '批发价 - 通过产品ID关联获取';
COMMENT ON COLUMN erp_sale_order_item.available_stock IS '可用库存 - 通过库存服务API实时获取';
COMMENT ON COLUMN erp_sale_order_item.book_stock IS '账面库存 - 通过库存服务API实时获取';
COMMENT ON COLUMN erp_sale_order_item.cost_amount IS '成本金额 - 通过计算(quantity * cost_price)得出';
COMMENT ON COLUMN erp_sale_order_item.gross_profit IS '参考毛利 - 通过计算(amount - cost_amount)得出';
```

## 执行前注意事项

1. **备份数据库**：在执行任何结构变更前，请确保对数据库进行了完整备份
2. **检查数据库连接**：确保PostgreSQL服务正在运行
3. **验证权限**：确保您具有执行DDL语句的足够权限
4. **业务时段**：建议在业务低峰期执行，因为DDL操作可能会锁定表

## 如何执行

### 使用psql命令行
```bash
psql -h localhost -p 5432 -U devuser -d devdb -f migration_script.sql
```

### 使用PgAdmin或其他GUI工具
1. 连接到目标数据库
2. 打开SQL查询窗口
3. 复制并粘贴上述SQL语句
4. 执行查询

## 验证步骤

执行完成后，验证变更是否成功：

```sql
-- 验证新字段是否已添加
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'erp_sale_order_item'
  AND column_name IN ('customer_grade_code', 'customer_grade_name', 'price_grade_code', 
                      'price_source', 'calculated_price', 'discount_applied');

-- 验证索引是否已创建
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'erp_sale_order_item'
  AND indexname LIKE '%soi_%';
```

## 故障排除

如果执行过程中出现问题：

1. **权限错误**：检查数据库用户权限设置
2. **表或字段已存在**：脚本中使用了IF NOT EXISTS，但仍可能出现冲突，可检查表结构后重试
3. **锁等待超时**：在业务低峰期执行，或暂时断开影响该表的应用连接
4. **磁盘空间不足**：确保有足够的磁盘空间完成表结构变更

## 回滚计划

如需回滚，可执行以下SQL（谨慎使用）：

```sql
-- 删除新增字段（按相反顺序）
ALTER TABLE erp_sale_order_item
DROP COLUMN IF EXISTS customer_grade_code,
DROP COLUMN IF EXISTS customer_grade_name,
DROP COLUMN IF EXISTS price_grade_code,
DROP COLUMN IF EXISTS price_source,
DROP COLUMN IF EXISTS calculated_price,
DROP COLUMN IF EXISTS discount_applied;

-- 删除新增索引
DROP INDEX IF EXISTS idx_soi_customer_grade;
DROP INDEX IF EXISTS idx_soi_price_grade;
DROP INDEX IF EXISTS idx_soi_product_id;
DROP INDEX IF EXISTS idx_soi_order_id;
```

## 后续步骤

完成数据库迁移后，请执行以下操作：

1. 重新启动应用程序
2. 运行集成测试验证功能正常
3. 检查应用程序日志是否有异常
4. 验证新功能是否按预期工作
```