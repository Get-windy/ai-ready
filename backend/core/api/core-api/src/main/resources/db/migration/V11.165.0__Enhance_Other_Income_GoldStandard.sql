-- 其他收入（资料 → 财务账户 → 其他收入，菜单 70542）金标准补齐
--
-- 口径（红线）：本页是「收入类会计科目」视图，直接复用 finance_account_subject，
--   收入类 = subject_type=5（损益类） AND direction=2（贷方）；
--   严禁另建收入类型字典表（与《其他收入单》fin_other_income_doc 严格区分）。
--
-- 1) 菜单归一：单入口（display_mode=0），组件指向金标准页面，幂等
UPDATE sys_menu
SET component    = 'views/md/other-income/index.vue',
    display_mode = 0,
    status       = 1,
    visible      = 1,
    update_time  = CURRENT_TIMESTAMP
WHERE id = 70542;

-- 2) 补收入类科目种子数据（对标 ql361 实测示例 4 条），幂等：科目编号已存在则跳过
INSERT INTO finance_account_subject
    (id, subject_code, subject_name, parent_id, level, subject_type, direction,
     is_leaf, is_enabled, mnemonic_code, full_name, deleted_flag, tenant_id,
     created_at, updated_at)
SELECT 900001, '6101', '公允价值变动损益', NULL, 1, 5, 2,
       true, true, 'GYJZBDSY', '公允价值变动损益', 0, 1,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM finance_account_subject WHERE subject_code = '6101' AND deleted_flag = 0
);

INSERT INTO finance_account_subject
    (id, subject_code, subject_name, parent_id, level, subject_type, direction,
     is_leaf, is_enabled, mnemonic_code, full_name, deleted_flag, tenant_id,
     created_at, updated_at)
SELECT 900002, '6111', '投资收益', NULL, 1, 5, 2,
       true, true, 'TZSY', '投资收益', 0, 1,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM finance_account_subject WHERE subject_code = '6111' AND deleted_flag = 0
);

INSERT INTO finance_account_subject
    (id, subject_code, subject_name, parent_id, level, subject_type, direction,
     is_leaf, is_enabled, mnemonic_code, full_name, deleted_flag, tenant_id,
     created_at, updated_at)
SELECT 900003, '6301', '营业外收入', NULL, 1, 5, 2,
       true, true, 'YYWSR', '营业外收入', 0, 1,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM finance_account_subject WHERE subject_code = '6301' AND deleted_flag = 0
);

INSERT INTO finance_account_subject
    (id, subject_code, subject_name, parent_id, level, subject_type, direction,
     is_leaf, is_enabled, mnemonic_code, full_name, deleted_flag, tenant_id,
     created_at, updated_at)
SELECT 900004, '6902', '利息收入', NULL, 1, 5, 2,
       true, true, 'LXSR', '利息收入', 0, 1,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM finance_account_subject WHERE subject_code = '6902' AND deleted_flag = 0
);
