# 智企连·AI-Ready 数据库初始化指南

## 概述

本目录包含智企连·AI-Ready系统的完整数据库初始化SQL脚本，包括核心基础表、ERP模块表、CRM模块表和初始化数据。

## SQL脚本文件说明

| 文件名 | 说明 | 表数量 |
|--------|------|--------|
| `00_init_all.sql` | 主脚本，导入所有SQL脚本 | - |
| `01_core_base.sql` | 核心基础表（用户、角色、权限、部门等） | 26 |
| `02_init_data.sql` | 初始化数据（管理员、角色、权限等） | - |
| `03_erp_modules.sql` | ERP模块表（采购、销售、库存、财务等） | 20 |
| `04_crm_modules.sql` | CRM模块表（客户、合同、报价、营销等） | 15 |
| `init_database.ps1` | PowerShell自动导入脚本 | - |

## 数据库连接信息

根据 `application-dev.yml` 配置：

- **主机**: localhost
- **端口**: 5432
- **数据库**: devdb
- **用户名**: devuser
- **密码**: Dev@2026#Local

## 导入方法

### 方法一：使用pgAdmin（推荐）

1. 打开 pgAdmin
2. 连接到 PostgreSQL 服务器
3. 选择数据库 `devdb`
4. 点击 Tools -> Query Tool
5. 打开 `00_init_all.sql` 文件
6. 点击 Execute 按钮

### 方法二：使用psql命令行

```bash
cd I:\AI-Ready\backend\sql
psql -h localhost -p 5432 -U devuser -d devdb -f 00_init_all.sql
```

### 方法三：使用PowerShell脚本

```powershell
cd I:\AI-Ready\backend\sql
.\init_database.ps1
```

### 方法四：逐个导入

```bash
cd I:\AI-Ready\backend\sql
psql -h localhost -p 5432 -U devuser -d devdb -f 01_core_base.sql
psql -h localhost -p 5432 -U devuser -d devdb -f 02_init_data.sql
psql -h localhost -p 5432 -U devuser -d devdb -f 03_erp_modules.sql
psql -h localhost -p 5432 -U devuser -d devdb -f 04_crm_modules.sql
```

## 初始化数据说明

### 默认用户

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| admin | admin123 | 超级管理员 | 拥有所有权限 |
| system | system123 | 超级管理员 | 系统内置用户 |

**注意**: 实际密码已加密存储，上述密码为示例。

### 默认角色

| 角色名 | 角色编码 | 说明 |
|--------|----------|------|
| 超级管理员 | SUPER_ADMIN | 拥有所有权限 |
| 管理员 | ADMIN | 拥有大部分权限 |
| 普通用户 | USER | 普通用户权限 |
| 财务 | FINANCE | 财务管理权限 |
| 采购 | PURCHASE | 采购管理权限 |
| 销售 | SALES | 销售管理权限 |
| 仓库 | WAREHOUSE | 仓库管理权限 |
| 客服 | CUSTOMER_SERVICE | 客服权限 |

### 默认部门

| 部门名 | 部门编码 | 说明 |
|--------|----------|------|
| 总公司 | ROOT | 根部门 |
| 财务部 | FINANCE | 财务部门 |
| 采购部 | PURCHASE | 采购部门 |
| 销售部 | SALES | 销售部门 |
| 仓库部 | WAREHOUSE | 仓库部门 |
| 客服部 | CUSTOMER_SERVICE | 客服部门 |
| 技术部 | TECH | 技术部门 |

## 表结构说明

### 核心基础表 (26个)

- `sys_user` - 用户表
- `sys_role` - 角色表
- `sys_permission` - 权限表
- `sys_user_role` - 用户角色关联表
- `sys_role_permission` - 角色权限关联表
- `sys_permission_template` - 权限模板表
- `sys_department` - 部门表
- `sys_menu` - 菜单表
- `sys_role_menu` - 角色菜单关联表
- `sys_data_permission` - 数据权限表
- `sys_role_inheritance` - 角色继承表
- `sys_system_log` - 系统日志表
- `sys_login_log` - 登录日志表
- `sys_oper_log` - 操作日志表
- `sys_dict_type` - 字典类型表
- `sys_dict_item` - 字典数据表
- `sys_project_config` - 系统配置表
- `sys_message` - 消息表
- `sys_message_template` - 消息模板表
- `sys_file` - 文件表
- `sys_file_permission` - 文件权限表
- `workflow_definition` - 工作流定义表
- `workflow_instance` - 工作流实例表
- `workflow_task` - 工作流任务表
- `sys_scheduled_task` - 定时任务表
- `sys_task_execute_log` - 任务执行日志表

### ERP模块表 (20个)

- `biz_party` - 往来单位表
- `biz_party_category` - 往来单位分类表
- `biz_party_contact` - 往来单位联系人表
- `biz_customer_grade` - 客户等级表
- `erp_purchase_order` - 采购订单表
- `erp_sale_order` - 销售订单表
- `erp_stock` - 库存表
- `erp_stock_check` - 库存盘点表
- `erp_stock_transfer` - 库存调拨表
- `batch_number` - 批次号表
- `serial_number` - 序列号表
- `invoice` - 发票表
- `finance_account` - 财务账户表
- `finance_transaction` - 财务交易表
- `fin_receivable` - 应收账款表
- `fin_payable` - 应付账款表
- `erp_payment` - 付款单表
- `erp_receipt` - 收款单表
- `erp_supplier` - 供应商表
- `erp_pricing_strategy` - 价格策略表

### CRM模块表 (15个)

- `crm_customer` - 客户表
- `crm_customer_lead` - 客户线索表
- `crm_customer_opportunity` - 商机表
- `crm_customer_follow_up` - 客户跟进表
- `crm_customer_pool` - 客户公海池表
- `crm_contract` - 合同表
- `crm_contract_clause` - 合同条款表
- `crm_contract_payment` - 合同付款计划表
- `crm_contract_attachment` - 合同附件表
- `crm_contract_change` - 合同变更表
- `crm_quotation` - 报价单表
- `crm_quotation_item` - 报价明细表
- `crm_quotation_template` - 报价模板表
- `crm_marketing_campaign` - 营销活动表
- `crm_marketing_channel` - 营销渠道表

## 验证导入结果

导入完成后，可以执行以下SQL验证：

```sql
-- 查询表数量
SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema = 'public';

-- 查询用户数量
SELECT COUNT(*) AS user_count FROM sys_user;

-- 查询角色数量
SELECT COUNT(*) AS role_count FROM sys_role;

-- 查询权限数量
SELECT COUNT(*) AS permission_count FROM sys_permission;

-- 查询部门数量
SELECT COUNT(*) AS dept_count FROM sys_department;

-- 查询字典类型数量
SELECT COUNT(*) AS dict_type_count FROM sys_dict_type;
```

## 常见问题

### 1. psql命令不可用

**解决方案**: 
- 安装 PostgreSQL 客户端工具
- 或使用 pgAdmin 图形界面
- 或使用 PowerShell 脚本（需要安装 PostgreSQL ODBC 驱动）

### 2. 数据库连接失败

**解决方案**:
- 检查 PostgreSQL 服务是否启动
- 检查数据库连接配置是否正确
- 检查防火墙是否阻止连接

### 3. 表已存在错误

**解决方案**:
- SQL脚本使用 `CREATE TABLE IF NOT EXISTS`，不会重复创建
- 如需重新创建，先删除表：`DROP TABLE IF EXISTS table_name CASCADE;`

### 4. 权限不足

**解决方案**:
- 使用超级用户连接数据库
- 或授予用户足够权限：`GRANT ALL PRIVILEGES ON DATABASE devdb TO devuser;`

## 生产环境注意事项

1. **修改默认密码**: 导入后立即修改 admin 用户密码
2. **删除测试数据**: 生产环境删除测试用户和数据
3. **配置权限**: 根据实际需求配置角色和权限
4. **备份数据**: 定期备份数据库
5. **监控日志**: 监控系统日志和操作日志

## 技术支持

如有问题，请联系：
- Email: dev@ai-ready.cn
- 项目文档: I:\AI-Ready\AGENTS.md

---

**智企连·AI-Ready** - 企业智能管理系统