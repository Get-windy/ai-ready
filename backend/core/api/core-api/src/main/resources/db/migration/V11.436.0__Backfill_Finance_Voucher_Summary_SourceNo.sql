-- =============================================================================
-- 回填 finance_voucher 的「摘要」与「来源单号」（2026-09-20）
--
-- 【问题】业务记账链路 BusinessAccountingServiceImpl 组装 VoucherDTO 时只调了
--   setRemark(request.getSummary())，**没有**传 setSummary / setSourceNo；而
--   VoucherServiceImpl#create 读的是 dto.getSummary() 与 dto.getSourceNo()。
--   字段名对不上 ⇒ devdb 实测：finance_voucher 共 69 行，summary 0/69 非空、
--   source_no 0/69 非空（但**行级** finance_voucher_item.source_no 有 137/145 非空）。
--
-- 【影响】凭证列表「摘要」「来源单据」列恒空；财务无法从凭证反查业务单据
--   （VoucherQuery.sourceNo 查询条件永远筛不出结果）。
--
-- 【代码修复】BusinessAccountingServiceImpl#createVoucherFromBusiness 已补传
--   setSummary / setSourceNo（同批提交），本迁移只负责把历史空值补齐。
--
-- 【回填口径】
--   · summary：remark 列存的就是同一份摘要（原代码把 summary 误传给了 remark），直接复制。
--   · source_no：finance_voucher 无 source_type/source_id 列，故从行级
--     finance_voucher_item.source_no 取该凭证下最早的一条回填。
--
-- 【幂等】仅更新空值行，可重复执行。
-- =============================================================================

UPDATE finance_voucher
SET summary = remark
WHERE (summary IS NULL OR summary = '')
  AND remark IS NOT NULL
  AND remark <> '';

UPDATE finance_voucher v
SET source_no = i.source_no
FROM (
    SELECT voucher_id, MIN(source_no) AS source_no
    FROM finance_voucher_item
    WHERE source_no IS NOT NULL
      AND source_no <> ''
    GROUP BY voucher_id
) i
WHERE v.id = i.voucher_id
  AND (v.source_no IS NULL OR v.source_no = '');
