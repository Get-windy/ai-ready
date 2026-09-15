-- ============================================================
-- V11.28.0: 植入商贸企业基础会计科目（业财集成凭证生成依赖）
--
-- 背景：业财集成网关生成凭证时按 subjectCode 查科目，
-- finance_account_subject 为空表导致凭证创建报"科目编码不存在"。
-- 本批科目覆盖五个业财集成服务实际引用的全部编码
-- （1002/1122/1403/1602/2202/2241/6001/6602/6604）+ 必备一级科目。
-- 科目类型：1-资产 2-负债 3-权益 4-成本 5-损益；方向：1-借方 2-贷方。
-- 幂等：按 (tenant_id, subject_code) NOT EXISTS 守卫。
-- ============================================================

INSERT INTO finance_account_subject (subject_code, subject_name, parent_id, level, subject_type, direction, is_leaf, is_enabled, deleted_flag, tenant_id, remark)
SELECT * FROM (VALUES
    ('1001', '库存现金',   NULL::bigint, 1, 1, 1, 1, 1, 0, 1, '资产类一级科目'),
    ('1002', '银行存款',   NULL::bigint, 1, 1, 1, 1, 1, 0, 1, '资产类一级科目'),
    ('1122', '应收账款',   NULL::bigint, 1, 1, 1, 1, 1, 0, 1, '资产类一级科目'),
    ('1403', '库存商品',   NULL::bigint, 1, 1, 1, 1, 1, 0, 1, '资产类一级科目'),
    ('1601', '固定资产',   NULL::bigint, 1, 1, 1, 1, 1, 0, 1, '资产类一级科目'),
    ('1602', '累计折旧',   NULL::bigint, 1, 1, 2, 1, 1, 0, 1, '资产备抵科目，贷方余额'),
    ('2202', '应付账款',   NULL::bigint, 1, 2, 2, 1, 1, 0, 1, '负债类一级科目'),
    ('2241', '其他应付款', NULL::bigint, 1, 2, 2, 1, 1, 0, 1, '负债类一级科目'),
    ('4001', '实收资本',   NULL::bigint, 1, 3, 2, 1, 1, 0, 1, '权益类一级科目'),
    ('4103', '本年利润',   NULL::bigint, 1, 3, 2, 1, 1, 0, 1, '权益类一级科目'),
    ('6001', '主营业务收入', NULL::bigint, 1, 5, 2, 1, 1, 0, 1, '损益类（收入）一级科目'),
    ('6401', '主营业务成本', NULL::bigint, 1, 4, 1, 1, 1, 0, 1, '成本类一级科目'),
    ('6602', '管理费用',   NULL::bigint, 1, 5, 1, 1, 1, 0, 1, '损益类（费用）一级科目'),
    ('6604', '折旧费',     NULL::bigint, 2, 5, 1, 1, 1, 0, 1, '管理费用-折旧费（固定资产计提）')
) AS seed(subject_code, subject_name, parent_id, level, subject_type, direction, is_leaf, is_enabled, deleted_flag, tenant_id, remark)
WHERE NOT EXISTS (
    SELECT 1 FROM finance_account_subject s
    WHERE s.tenant_id = seed.tenant_id AND s.subject_code = seed.subject_code
);

-- parent_id 按编码回填（6602 → 6604 的父级）
UPDATE finance_account_subject child
SET parent_id = parent.id
FROM finance_account_subject parent
WHERE child.subject_code = '6604' AND parent.subject_code = '6602'
  AND child.tenant_id = parent.tenant_id
  AND child.parent_id IS NULL;
