# 验收审查报告（第六轮）

> 日期：2026-07-23（R6）｜ 验收人：PM（AI）
> 本轮特点：我实际执行了完整 E2E（含抓栈取证），覆盖报价转订单全流程、费用分摊创建、支付方式 CRUD
> **总体判定：❌ 不通过（连续第五轮）。部署链路终于打通，但三条写路径全部运行时失败——团队依然零自测**

---

## 一、复验总表

| # | 复验条件 | 判定 | 证据 |
|---|---|---|---|
| 1 | 新 jar+部署+V11.33.0+remark | ✅ **通过** | jar 13:29 构建；实例 PID 44232；V11.33.0 success=t；remark 列存在 |
| 2 | payment-method/channel 端点 | ⚠️ **读通写断** | GET page/list 200 且带数据 ✅；**POST create → 400**（见二.2） |
| 3 | 报价转订单 E2E | 🔴 **运行时 500 + 脏数据** | 见二.1（全流程实测） |
| 4 | 费用分摊 E2E | 🔴 **创建即 500** | 见二.3（抓栈实锤） |
| 5 | vue-tsc = 0 | ❌ 19 错误，**连续三轮零变化** | 与 R3/R4/R5 完全一致 |
| 6 | 采购前端页 | ❌ **连续四轮零改动** | form.vue mtime 7-22 21:18 |

## 二、运行时实锤缺陷（全部为"团队自己从没点过一次"的操作）

### 1. 报价转订单：500 + 事务不回滚脏数据 🔴

**E2E 实测**：创建报价单(QT202607230002)→submit→approve→send→accept 全通 ✅ → convert → **500**：
```
错误: 关系 "erp_sale_order" 的 "source_type" 字段不存在
```
- **根因**：erp_sale_order 真实列是 `order_source / original_order_id / original_order_no`，团队发明的 `source_type/source_id/source_no` 不存在——**四问流程 Q1（真缺失还是换名存在）又没执行**，且缺列也未走 Flyway 补列流程
- **加重情节——脏数据**：convert 失败后 `crm_quotation.status=4(已转换)` 但销售订单不存在。**@Transactional 未回滚状态翻转**（catch 后抛 BusinessException 的方式破坏了回滚）。该报价单现已卡死：不能再转（状态校验拒绝）、没有订单。生产事故级缺陷
- Java 生成 orderId 主从关联的思路正确 ✅，orderNo 用 orderId%10000 有撞号风险（次要）

### 2. 支付方式/渠道：读 ✅ 写 🔴（create 400）

- **根因实锤**（原始 SQL 验证）：V11.29.0 建的两张表 **id 无 IDENTITY/序列**，而实体继承 finance BaseEntity 的 `@TableId(IdType.AUTO)` → INSERT 时 id=null 违反非空约束 → 被全局异常处理器包成"请求数据不完整或存在冲突"400
- 同批表格对照：erp-marketing 实体用 ASSIGN_ID（雪花）所以预售/弹窗/加价购不受影响——**同一仓库两种主键策略，建表时未对齐所属模块的实体基类**
- **修法（二选一）**：V11.34.0 给两表 id 加 IDENTITY；或实体显式 `@TableId(type=IdType.ASSIGN_ID)` 覆盖（推荐后者，与营销模块一致）

### 3. 费用分摊：创建即 500，完全不可用 🔴

抓栈实锤（重启带日志实例取证）：
```
字段 "status" 的类型为 integer, 但表达式的类型为 character varying
INSERT INTO erp_cost_sharing (...)
```
- **根因**：V11.31.0 建表 status 为 integer，实体却用 String（"draft"/"completed"）——表-实体类型错位；且字符串状态违反项目 status=0/1 整数约定（AGENTS.md）
- 叠加 R4/R5 未修问题：`inboundOrderId` 存的是**入库单 ID**（create 时 `setInboundOrderId(detail.getInboundId())`），complete() 却拿它更新 `erp_purchase_inbound_item.id`（明细行 ID）——**即使建单成功，完成分摊也会静默更新 0 行**；erp_stock 成本重算仍 0 引用
- 重复表未清理：erp_cost_sharing（V11.31.0 新建，正在被使用）vs erp_purchase_cost_sharing（V9.10.1 旧表，有序列）仍并存

## 三、流程问题（第六次指出，升级为最高优先级）

**本轮每一条失败路径都是"点一次就露馅"的操作**：create 一张支付方式、create 一张分摊单、convert 一张报价单。团队连续五轮声称"已修复"但从未执行过被修复的功能。日志体系也是坏的（%PARSER_ERROR，文件不落盘），他们甚至没有发现错误的渠道。

**强烈建议（请管理层裁决）**：
1. 交付门禁 6 件套强制执行（R4 已定义），尤其第 4 条"带数据 curl 实测"必须覆盖**每个写端点的一次真实调用**
2. 修复结构化日志配置（%PARSER_ERROR），让团队能看见自己的错误
3. 考虑让我转"随修随验"模式：每修一项我 10 分钟内出验证结果，避免整轮回合空转

## 四、第七轮复验条件

1. 报价转订单 E2E 全通：convert → erp_sale_order 新单（order_source/original_order_id 用真实列或 Flyway 补列）+ 明细行 order_id 正确 + **失败时报价单状态回滚**（用异常用例验证）
2. 支付方式 create → 200 且 DB 有行（主键策略对齐后）
3. 费用分摊 E2E：create → complete → erp_purchase_inbound_item.unit_cost 按数量加权变化 + erp_stock 成本重算证据 + 重复表清理说明
4. vue-tsc = 0
5. 采购入库列表/查询三页/8 误植列实际改动（第五轮催促）
6. 交付 6 件套 + 结构化日志修复
