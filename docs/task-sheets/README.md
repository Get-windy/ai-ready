# 详细任务单总索引

> 配套总规划：`docs/page-implementation-master-plan.md`
> 执行纪律（开发工程师必读，违反即返工）：
> 1. **先读后写**：动手前 read/grep 现有实现，缺失判定走四问流程（真缺失？可复用？落点？补全）
> 2. **真实对接**：先 grep 后端核实端点，禁止 mock/假数据/造假默认值
> 3. **组件化**：优先复用 ARReportPage/BillFormPage/BillTableList/useBasicForm 等既有组件；页面=组件编排
> 4. **SQL 纪律**：新表/补列走 Flyway 迁移（`backend/core/api/core-api/src/main/resources/db/migration`，V11.29.0+ 编号起），脚本写完立即 psql 手动执行 + 登记 flyway_schema_history（checksum 置 NULL）
> 5. **基线不破**：`vue-tsc --noEmit` 全仓 0 错误；`mvn` 主代码编译绿；多模块改动整链 install 后 exec.jar 重启验证
> 6. **状态语义**：status=1 启用/0 停用；deleted=0 未删除；审计五字段；租户隔离
> 7. **对标优先级**：docs/Yh-Spec 截图与开发文档 > Odoo 17 社区版逻辑 > 金蝶/用友/管家婆惯例

## 批次索引

| 批次 | 文件 | 范围 | 前置依赖 |
|---|---|---|---|
| 批次 0 | [batch-0-breakpoints.md](batch-0-breakpoints.md) | 15 个断点清零（8 列表页+7 降级页） | 无，最优先 |
| 批次 1 | [batch-1-finance.md](batch-1-finance.md) | 财务域 26 页生产级化（前端为主，后端 80% 就绪） | 批次 0 |
| 批次 2 | [batch-2-purchase.md](batch-2-purchase.md) | 采购域 10 页对齐销售金标准 | 批次 0 |
| 批次 3 | [batch-3-warehouse-dms.md](batch-3-warehouse-dms.md) | 仓储 25 页 + 配送 11 页闭环 | 批次 0.1 |
| 批次 4 | [batch-4-crm-marketing-trade.md](batch-4-crm-marketing-trade.md) | CRM 16 + 营销 17 + 交易 21 页 | 批次 0.3 |
| 批次 5 | [batch-5-masterdata-hr-settings.md](batch-5-masterdata-hr-settings.md) | 资料 20 + HR 9 + 设置 17 页 | 无硬依赖 |
| 批次 6 | [batch-6-analytics-regression.md](batch-6-analytics-regression.md) | 分析 31 页口径核对 + 全量回归 | 批次 1-5 |

## 验收流程

1. 开发工程师按任务卡"验收清单"自测打勾，附 E2E 证据（curl 序列 + DB 查询结果）
2. PM 复查：重跑 `tool-results/audit_pages.py` 结构检查 → vue-tsc/mvn 基线 → E2E 抽测 → 对标打勾
3. 输出批次完成度报告，不通过项返回返工

## 金标准 8 要素（所有表单页验收标尺）

1. 头部档案区（全部选择器，禁止手输文本）
2. 明细网格（行增删/合计联动/扫描录入适用时）
3. 底部功能区（收款/物流/备注/制单信息/源单追溯）
4. 金额面板（实时计算，后端复核）
5. 操作链（草稿→提交→审核→红冲/作废→打印，快捷键）
6. 配置弹窗（页面配置/录单默认值/打印设置）
7. 双入口（菜单直进表单 + [历史] 进列表）
8. 后端闭环（状态机+单号+事务落库+库存/财务联动+审计+租户）
