# docs/old — 归档文档目录

> 本目录存放**已过时 / 重复 / 与项目现状或对标系统（ql361 v2.2）不符**的历史文档，从 `docs/` 主目录归档而来，保留以备考证，不再作为开发依据。

## 归档清单与原因

| 归档内容 | 原位置 | 归档原因 |
|---|---|---|
| `acceptance/`（R1~R5 验收报告） | `docs/acceptance-report-2026-07-23*.md` | 逐轮验收的迭代版本，被 `docs/acceptance-report-2026-07-23-r6.md`（最新）取代 |
| `对标系统实际检查报告.md` | `docs/对标系统实际检查报告.md` | 仅抽查了销售/采购订单两页，已被 `ql361对标/重点页面字段级对比附录.md` 全面取代 |
| `production-landing-progress.md` | `docs/production-landing-progress.md` | A/B/C 阶段攻坚进度日志，工作已完成；含环境凭据等敏感信息 |
| `page-implementation-master-plan.md` | `docs/page-implementation-master-plan.md` | 批次 0-6 总体规划；断点已清零、「收款单 59 行」等结论已过时 |
| `task-sheets/`（batch-0~6 任务单） | `docs/task-sheets/` | 逐页任务卡，7 批次已全部执行完毕 |
| 下列历史分析/设计/规划 | 本目录原有 | 旧 roadmap/architecture/design/gap-analysis，结论已过时 |

## 团队唯一事实源（当前有效）

- **标杆文档（手工整理，权威）**：`docs/Yh-Spec/XSMK/`（销售域 10 篇，销售全量列/表单字段的权威来源）
- 对标开发文档：`docs/Yh-Spec/ql361对标/<域>/`（145 篇）
- 对标执行手册：`docs/Yh-Spec/ql361对标/README.md`
- 页面映射总表：`docs/Yh-Spec/ql361对标/page-mapping.md`
- 字段级对比附录：`docs/Yh-Spec/ql361对标/重点页面字段级对比附录.md`
- 最新验收报告：`docs/acceptance-report-2026-07-23-r6.md`
- 系统总览与结构：根 `AGENTS.md`、`backend/docs/README.md`

> 需要追溯历史决策/失败教训时，再回到本目录查阅；日常开发一律以「唯一事实源」为准。
