-- =====================================================
-- V11.20.0: 工作流 V9.1.0 死列清理
-- =====================================================
-- 背景:
--   workflow_definition / workflow_instance / workflow_task 三表在 V9.1.0
--   以 ADD COLUMN IF NOT EXISTS 追加了第二套列（flow_*/biz_*/initiator_* 等）。
--   工作流引擎合并后，core-api 套（cn.aiedge.workflow）实体仅使用 V5.0.0 列集
--   + workflow_instance.form_data（存业务数据JSON），其余 V9.1.0 列无任何
--   Java 代码引用，判定为死列，本迁移全部删除。
--
-- 保留:
--   - V5.0.0 列集的全部列
--   - workflow_instance.form_data（core-api 侧已启用）
--
-- 说明:
--   - 全部使用 DROP ... IF EXISTS，幂等可重复执行
--   - 依赖死列的索引先显式删除（DROP COLUMN 也会级联删，显式删更清晰）
--   - 三表数据量小（definition 4 行 / instance 5 行 / task 15 行，测试数据）
-- =====================================================

-- -----------------------------------------------------
-- Part 1: workflow_definition 死列
-- -----------------------------------------------------
DROP INDEX IF EXISTS idx_wf_def_flow_code;

ALTER TABLE workflow_definition DROP COLUMN IF EXISTS flow_code;
ALTER TABLE workflow_definition DROP COLUMN IF EXISTS flow_name;
ALTER TABLE workflow_definition DROP COLUMN IF EXISTS flow_type;
ALTER TABLE workflow_definition DROP COLUMN IF EXISTS config_json;

-- -----------------------------------------------------
-- Part 2: workflow_instance 死列（保留 form_data）
-- -----------------------------------------------------
DROP INDEX IF EXISTS idx_wf_inst_flow;
DROP INDEX IF EXISTS idx_wf_inst_biz;
DROP INDEX IF EXISTS idx_wf_inst_initiator;

ALTER TABLE workflow_instance DROP COLUMN IF EXISTS flow_id;
ALTER TABLE workflow_instance DROP COLUMN IF EXISTS biz_type;
ALTER TABLE workflow_instance DROP COLUMN IF EXISTS biz_id;
ALTER TABLE workflow_instance DROP COLUMN IF EXISTS biz_no;
ALTER TABLE workflow_instance DROP COLUMN IF EXISTS current_node;
ALTER TABLE workflow_instance DROP COLUMN IF EXISTS initiator_id;
ALTER TABLE workflow_instance DROP COLUMN IF EXISTS initiator_name;
ALTER TABLE workflow_instance DROP COLUMN IF EXISTS started_time;
ALTER TABLE workflow_instance DROP COLUMN IF EXISTS completed_time;

-- -----------------------------------------------------
-- Part 3: workflow_task 死列
-- -----------------------------------------------------
ALTER TABLE workflow_task DROP COLUMN IF EXISTS node_code;
ALTER TABLE workflow_task DROP COLUMN IF EXISTS prev_task_id;
ALTER TABLE workflow_task DROP COLUMN IF EXISTS transferred_from;
ALTER TABLE workflow_task DROP COLUMN IF EXISTS action_time;
ALTER TABLE workflow_task DROP COLUMN IF EXISTS deadline;
