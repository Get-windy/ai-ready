-- 清理「其他往来单位」验收产生的证件附件残留（仅限 E2E/金标准 命名的测试单位）
DELETE FROM erp_partner_attachment a
USING biz_party p
WHERE a.partner_id = p.id
  AND a.file_url LIKE '/api/file/view/%'
  AND (p.party_name LIKE 'E2E%' OR p.party_name LIKE '金标准%');
