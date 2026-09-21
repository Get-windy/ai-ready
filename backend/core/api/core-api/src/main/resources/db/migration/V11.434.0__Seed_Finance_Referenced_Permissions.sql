-- =============================================================================
-- 补齐 erp-finance 引用的 32 个缺失权限码（2026-09-20）
--
-- 【为什么必须先补种子】
--   erp-finance 域有 108 处 `@PreAuthorize("hasPermission('<资源>', '<权限码>')")`，
--   共引用 49 个权限码，其中 **32 个在 sys_permission 里根本不存在**（实测）。
--   本仓铁律：注解引用了库中没有的码 ⇒ 该接口对**所有非超管用户一律拒绝**
--   （历史事故：243 个引用仅 153 个在库 → 非超管全 403）。
--   因此本迁移是把这 108 处改用 `@SaCheckPermission` 的**前置动作，顺序不能反**：
--   先有种子，再让注解真正生效。
--
-- 【口径】与同域既有 finance 码保持一致（实测 90001-90033 段的字段取值）：
--   tenant_id=0、parent_id=0、permission_type=3、status=0（0=正常）、visible=1；
--   sort 接续现有最大 sort（1051）。
--
-- 【id 依据】实测 2026-09-20：
--   SELECT count(*) FROM sys_permission WHERE id BETWEEN 90034 AND 90065;  → 0（整段空闲）
--
-- ⚠️ 本迁移只补种子，**不改任何接口注解**；注解改造由同批次代码改动完成。
-- =============================================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
VALUES
 -- 辅助核算（4）
 (90034, 0, 0, 0, now(), now(), '辅助核算查询',   'finance:auxiliary:view',          3, NULL, NULL, 1052, 1, 0),
 (90035, 0, 0, 0, now(), now(), '辅助核算新增',   'finance:auxiliary:create',        3, NULL, NULL, 1053, 1, 0),
 (90036, 0, 0, 0, now(), now(), '辅助核算编辑',   'finance:auxiliary:update',        3, NULL, NULL, 1054, 1, 0),
 (90037, 0, 0, 0, now(), now(), '辅助核算删除',   'finance:auxiliary:delete',        3, NULL, NULL, 1055, 1, 0),
 -- 收付与往来（5）
 (90038, 0, 0, 0, now(), now(), '收款统计查询',   'finance:collection-stats:view',   3, NULL, NULL, 1056, 1, 0),
 (90039, 0, 0, 0, now(), now(), '往来余额查询',   'finance:partner-balance:view',    3, NULL, NULL, 1057, 1, 0),
 (90040, 0, 0, 0, now(), now(), '应付查询',       'finance:payable:view',            3, NULL, NULL, 1058, 1, 0),
 (90041, 0, 0, 0, now(), now(), '应付新增',       'finance:payable:create',          3, NULL, NULL, 1059, 1, 0),
 (90042, 0, 0, 0, now(), now(), '应付删除',       'finance:payable:delete',          3, NULL, NULL, 1060, 1, 0),
 (90043, 0, 0, 0, now(), now(), '应付核销',       'finance:payable:write-off',       3, NULL, NULL, 1061, 1, 0),
 (90044, 0, 0, 0, now(), now(), '应收查询',       'finance:receivable:view',         3, NULL, NULL, 1062, 1, 0),
 (90045, 0, 0, 0, now(), now(), '应收核销',       'finance:receivable:write-off',    3, NULL, NULL, 1063, 1, 0),
 (90046, 0, 0, 0, now(), now(), '应收坏账',       'finance:receivable:bad-debt',     3, NULL, NULL, 1064, 1, 0),
 -- 财务报表（3）
 (90047, 0, 0, 0, now(), now(), '财务报表查询',   'finance:report:view',             3, NULL, NULL, 1065, 1, 0),
 (90048, 0, 0, 0, now(), now(), '财务报表生成',   'finance:report:generate',         3, NULL, NULL, 1066, 1, 0),
 (90049, 0, 0, 0, now(), now(), '财务报表审核',   'finance:report:approve',          3, NULL, NULL, 1067, 1, 0),
 -- 税务（3）
 (90050, 0, 0, 0, now(), now(), '税务查询',       'finance:tax:view',                3, NULL, NULL, 1068, 1, 0),
 (90051, 0, 0, 0, now(), now(), '税务新增',       'finance:tax:create',              3, NULL, NULL, 1069, 1, 0),
 (90052, 0, 0, 0, now(), now(), '税务编辑',       'finance:tax:edit',                3, NULL, NULL, 1070, 1, 0),
 -- 财务交易（4）
 (90053, 0, 0, 0, now(), now(), '财务交易查询',   'finance:transaction:view',        3, NULL, NULL, 1071, 1, 0),
 (90054, 0, 0, 0, now(), now(), '财务交易新增',   'finance:transaction:create',      3, NULL, NULL, 1072, 1, 0),
 (90055, 0, 0, 0, now(), now(), '财务交易审核',   'finance:transaction:approve',     3, NULL, NULL, 1073, 1, 0),
 (90056, 0, 0, 0, now(), now(), '财务交易撤销',   'finance:transaction:revoke',      3, NULL, NULL, 1074, 1, 0),
 -- 会计凭证（7）
 (90057, 0, 0, 0, now(), now(), '凭证查询',       'finance:voucher:view',            3, NULL, NULL, 1075, 1, 0),
 (90058, 0, 0, 0, now(), now(), '凭证新增',       'finance:voucher:create',          3, NULL, NULL, 1076, 1, 0),
 (90059, 0, 0, 0, now(), now(), '凭证编辑',       'finance:voucher:edit',            3, NULL, NULL, 1077, 1, 0),
 (90060, 0, 0, 0, now(), now(), '凭证删除',       'finance:voucher:delete',          3, NULL, NULL, 1078, 1, 0),
 (90061, 0, 0, 0, now(), now(), '凭证审核',       'finance:voucher:audit',           3, NULL, NULL, 1079, 1, 0),
 (90062, 0, 0, 0, now(), now(), '凭证过账',       'finance:voucher:post',            3, NULL, NULL, 1080, 1, 0),
 (90063, 0, 0, 0, now(), now(), '凭证反冲',       'finance:voucher:reverse',         3, NULL, NULL, 1081, 1, 0),
 -- 支付渠道（被财务域引用，归属资料域但此处统一补入）（2）
 (90064, 0, 0, 0, now(), now(), '支付渠道查询',   'md:payment-channel:view',         3, NULL, NULL, 1082, 1, 0),
 (90065, 0, 0, 0, now(), now(), '支付渠道编辑',   'md:payment-channel:edit',         3, NULL, NULL, 1083, 1, 0);
