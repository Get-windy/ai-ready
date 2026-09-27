-- 2026-09-27 配送模块（DMS）审计清理
-- 报告：DMS_MODULE_AUDIT_20260923.md §6.1（僵尸表）/ §5.4（脏数据）
--
-- ── 一、删除 5 张僵尸表 ──────────────────────────────────────────────────
-- 来源：`V9.10.1__Add_Visit_CostSharing_Dispatch_Tables.sql`（早期按「配送自建单据表」建模）。
-- 后续实际走了「复用存量单据」路线（发货复用订单处理中心、收货复用销售/采购单据），
-- 这 5 张被整套抛弃，但从未删表、也未删建表迁移。
--
-- 判据（`tools/audit-dms-table-usage.py`，2026-09-23 全 backend 扫描）：
--   · 无 `@TableName` 实体、无 Mapper、无任何手写 SQL / XML 引用
--   · live = 0（devdb 实测 0 行 ⇒ 删表无数据损失）
--
-- 既有处置口径：`TABLE_DUPLICATE_AUDIT.md:212` 曾登记为「备份 + 观察 1–2 个发布周期」；
-- 2026-09-27 用户拍板**直接删除**。DDL 仍保留在 V9.10.1 中，如需恢复可参照该文件重建。

DROP TABLE IF EXISTS dms_dispatch_record;
DROP TABLE IF EXISTS dms_logistics_ship;
DROP TABLE IF EXISTS dms_purchase_receive;
DROP TABLE IF EXISTS dms_return_receive;
DROP TABLE IF EXISTS dms_ship_order;

-- ── 二、清理 scheduled_task 的垃圾演示数据 ────────────────────────────────
-- 12 行 `job_key` 为空的历史演示记录（同一批 6 条**完整重复两遍**），
-- 描述均为「历史演示数据：未绑定 job_key，已停用」。
-- 真实任务只有 5 条：`dms.dispatch.escalateOverdue` + 4 个营销作业。
-- 备份：`tool-results/scheduled_task_backup_before_clean_20260927.json`
--
-- 注：`scheduled_task` 已登记在 `MyBatisPlusConfig.IGNORE_TENANT_TABLES`，本 DELETE 全库生效。

DELETE FROM scheduled_task WHERE job_key IS NULL OR job_key = '';
