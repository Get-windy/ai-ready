# 发票管理系统

## 概述

发票管理系统是AI-Ready ERP的核心财务模块，支持采购发票和销售发票的完整生命周期管理。

## 功能特性

### 1. 采购发票管理
- 采购发票录入和验证
- 发票与采购订单自动匹配
- 发票审核流程
- 发票付款跟踪

### 2. 销售发票管理
- 销售发票自动生成
- 发票与销售订单关联
- 发票开具和打印
- 发票收款跟踪

### 3. 发票生命周期管理
- 发票状态跟踪（草稿→待审核→已审核→已开具→已归档）
- 发票流转记录
- 发票变更管理
- 发票归档管理

### 4. 发票税务管理
- 税务合规检查
- 税务申报支持
- 发票认证管理
- 税务风险预警

### 5. 发票数据分析
- 发票统计分析
- 发票时效性分析
- 发票合规性分析
- 发票成本分析

## 技术架构

### 核心实体
- **Invoice**: 发票主表
- **InvoiceItem**: 发票明细表
- **InvoiceFlow**: 发票流转记录表

### 服务层
- **InvoiceService**: 发票核心服务
- **InvoiceRepository**: 数据访问层

### API接口
- **InvoiceController**: RESTful API控制器

## 数据库设计

### 表结构

#### finance_invoice（发票主表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| invoice_no | VARCHAR(20) | 发票号码 |
| invoice_type | TINYINT | 发票类型 |
| direction | TINYINT | 发票方向（采购/销售） |
| status | TINYINT | 发票状态 |
| amount | DECIMAL(18,2) | 金额（不含税） |
| tax_rate | DECIMAL(5,4) | 税率 |
| tax_amount | DECIMAL(18,2) | 税额 |
| total_amount | DECIMAL(18,2) | 价税合计 |

#### finance_invoice_item（发票明细表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| invoice_id | BIGINT | 发票ID |
| goods_name | VARCHAR(200) | 商品名称 |
| quantity | DECIMAL(18,4) | 数量 |
| unit_price | DECIMAL(18,4) | 单价 |
| amount | DECIMAL(18,2) | 金额 |

#### finance_invoice_flow（发票流转记录表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| invoice_id | BIGINT | 发票ID |
| operation_type | TINYINT | 操作类型 |
| before_status | TINYINT | 操作前状态 |
| after_status | TINYINT | 操作后状态 |

## API接口文档

### 基础CRUD
```
POST   /api/finance/invoice              # 创建发票
PUT    /api/finance/invoice/{id}         # 更新发票
DELETE /api/finance/invoice/{id}         # 删除发票
GET    /api/finance/invoice/{id}         # 查询发票
GET    /api/finance/invoice/no/{no}      # 按号码查询
GET    /api/finance/invoice/page         # 分页查询
```

### 业务流程
```
POST /api/finance/invoice/{id}/submit    # 提交审核
POST /api/finance/invoice/{id}/approve   # 审核通过
POST /api/finance/invoice/{id}/reject    # 审核拒绝
POST /api/finance/invoice/{id}/issue     # 开具发票
POST /api/finance/invoice/{id}/void      # 作废发票
POST /api/finance/invoice/{id}/red       # 红冲发票
POST /api/finance/invoice/{id}/certify   # 认证发票
POST /api/finance/invoice/{id}/verify    # 核销发票
POST /api/finance/invoice/{id}/print     # 打印发票
POST /api/finance/invoice/{id}/archive   # 归档发票
```

### 统计查询
```
GET /api/finance/invoice/pending-certify # 待认证列表
GET /api/finance/invoice/expiring        # 即将到期发票
GET /api/finance/invoice/statistics/amount # 金额统计
GET /api/finance/invoice/statistics/month  # 按月统计
```

## 发票状态流转

```
草稿(0) → 待审核(1) → 已审核(2) → 已开具(3) → 已归档(6)
                           ↓
                      已作废(4)
                           ↓
                      已红冲(5)
```

## 使用示例

### 创建发票
```java
InvoiceDTO dto = new InvoiceDTO();
dto.setInvoiceNo("12345678901234567890");
dto.setInvoiceType(1);
dto.setDirection(1);
dto.setInvoiceDate(LocalDate.now());
dto.setSellerName("销售方公司");
dto.setBuyerName("购买方公司");
dto.setAmount(new BigDecimal("10000"));
dto.setTaxRate(new BigDecimal("0.13"));

InvoiceItemDTO item = new InvoiceItemDTO();
item.setGoodsName("商品A");
item.setQuantity(new BigDecimal("10"));
item.setUnitPrice(new BigDecimal("1000"));
item.setTaxRate(new BigDecimal("0.13"));
dto.setItems(List.of(item));

Invoice invoice = invoiceService.createInvoice(dto);
```

### 提交审核
```java
Invoice invoice = invoiceService.submitForReview(invoiceId);
```

### 审核通过
```java
Invoice invoice = invoiceService.approve(invoiceId, "审核通过");
```

## 安装部署

### 数据库初始化
```sql
-- 执行数据库迁移脚本
source backend/finance/src/main/resources/db/migration/V1__create_invoice_tables.sql
```

### 服务启动
```bash
# 启动Finance模块
cd backend/finance
mvn spring-boot:run
```

## 测试

### 单元测试
```bash
cd backend/finance
mvn test
```

### 集成测试
```bash
cd backend/tests/tests
pytest tests/integration/test_invoice.py -v
```

## 注意事项

1. **发票号码唯一性**: 发票号码必须唯一，系统会自动校验
2. **状态流转**: 必须按照状态流转规则操作，不能跨状态操作
3. **金额计算**: 系统自动计算税额和价税合计，确保数据准确性
4. **红冲限制**: 只有已开具状态的发票才能红冲
5. **认证期限**: 注意发票认证期限（360天），系统会提前预警
