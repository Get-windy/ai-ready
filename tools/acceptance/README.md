# 模块验收脚本（金标准）

各业务模块的**端到端验收脚本**：接口断言 + 真实浏览器 UI 交互（Playwright）+ 直连数据库核对落库终态（pg），跑完输出 `通过 N / 失败 M`。

> 2026-09-12 从项目根目录统一迁移至此；迁移时已把脚本内的 `path.join(__dirname, 'frontend/…')` 等相对路径改成 `../../frontend/…`。
> **请勿再次移动本目录**（脚本用 `__dirname` 定位项目根）。

## 运行

```bash
node tools/acceptance/verify-md-customer.cjs
```

前置条件：

1. **后端实例**：多数脚本头部写死了专用端口（如 `5801`、`5601`），需按该端口起一个后端；部分脚本支持环境变量覆盖（如 `API_PORT`）。
2. **前端 Vite**：UI 类脚本需要 pc-admin 的 dev server（同样在脚本头部写死端口）。
3. **依赖**：脚本从 `frontend/apps/pc-admin/node_modules` 加载 `playwright` / `pg`，无需额外安装。

## 脚本清单

| 模块 | 脚本 |
|------|------|
| 资料 · 客户 | `verify-md-customer.cjs` |
| 资料 · 其他往来单位 | `verify-md-partner.cjs` |
| 资料 · 供应商 | `verify-supplier.cjs` |
| 资料 · 银行账户 | `verify-bank-account.cjs`、`bank-tree-shot.cjs` |
| 资料 · 通用 | `check-entity-columns.cjs` |
| 销售 · 出库 | `verify-sale-outbound.cjs`、`ui-check-sale-outbound.cjs` |
| 销售 · 预订货 | `verify-sale-preorder.cjs`、`verify-preorder-ui.cjs` |
| 销售 · 换货 | `verify-sale-exchange.cjs`、`verify-sale-exchange-ui.cjs`、`verify-sale-exchange-ui2.cjs` |
| 销售 · 退货申请 | `verify-sale-return-apply.cjs`、`verify-sale-return-apply-ui.cjs` |
| 销售 · 退货单 | `verify-sale-return-doc.cjs` |
| 销售 · 价格跟踪 | `verify-sale-price-track.cjs` |
| 销售 · 明细查询 | `verify-sales-detail-query.cjs`、`verify-sales-detail-query-ui.cjs` |
| 销售 · 零售 | `verify-retail-gold.cjs`、`retail-gold-form-check.cjs`、`retail-gold-ui-check.cjs` |
| 销售 · 广告投放 | `ad-e2e.cjs` |
| 单据查询 | `verify-doc-query.cjs`、`verify-doc-query-ui.cjs`、`fixture-doc-query.cjs` |
| 财务 · 预算执行 | `budget-exec-check.cjs`、`budget-exec-ui-check.cjs` |
| 财务 · 科目余额表 | `trial-balance-check.cjs`、`trial-balance-ui-check.cjs` |

> 后续新增的模块级 E2E 建议仍放在 `tools/` 下（如 `tools/e2e-<模块>.cjs`），本目录收纳的是历史上散落在根目录的那批。

## 收尾提醒

跑完请按 `AI_DEVELOPER_RULES.md` 的 **7.3 会话收尾纪律**：关闭自己启动的后端/Vite 实例，并清理自己产生的临时产物（截图落在 `tool-results/`，日志为 `*.log`）。
