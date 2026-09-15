-- ============================================================================
-- V11.152.0 其他往来单位（资料 → 往来单位 → 其他往来单位）金标准（2026-09-11）
--
-- 背景：其他往来单位走 biz_party.party_type=4（非客户/供应商/物流的往来单位），
--      是本系统自建页面（ql361 无对应菜单入口，无对标），按资料模块金标准实现：
--      「左单位分类树（可维护）+ 右数据表（表头齿轮列配置）+ 档案式表单」。
--
-- 缺口：biz_party_category 中 party_type=4 无任何分类记录（种子仅客户/供应商），
--      导致分类树恒为空、用户必须从零建分类才能归类。
--
-- 处理：
--   1) 按 sys_tenant 中「启用」的租户，各初始化一套其他往来单位分类
--      （银行/政府机构/劳务公司 等，用户可在页面上增删改）。
--      注意：biz_party_category 参与多租户隔离（MyBatisPlusConfig），
--      故不能像系统表那样固定 tenant_id=0，否则登录租户查询不到。
--   2) 存量 party_type=4 记录回填多重身份 roles='OTHER'
--      （列表「显示…」合并查询与 fromBody 的 roles 口径一致）。
--
-- 幂等：INSERT 前 NOT EXISTS 判重；UPDATE 仅补空值。仅新增数据，不改结构。
-- ============================================================================

-- ------------------------------------------------------------
-- 1. 预置其他往来单位分类（每个启用租户一套，可由用户维护）
-- ------------------------------------------------------------
INSERT INTO biz_party_category (tenant_id, category_code, category_name, party_type, parent_id, level, sort_order, status)
SELECT t.id, v.category_code, v.category_name, 4, 0, 1, v.sort_order, 1
FROM sys_tenant t
CROSS JOIN (VALUES
    ('wldwml001', '银行',      1),
    ('wldwml002', '政府机构',  2),
    ('wldwml003', '劳务公司',  3),
    ('wldwml004', '维修服务商', 4),
    ('wldwml005', '培训机构',  5),
    ('wldwml006', '财务公司',  6),
    ('wldwml007', '法律服务',  7),
    ('wldwml008', '保险公司',  8),
    ('wldwml009', 'IT服务商',  9),
    ('wldwml010', '咨询公司', 10),
    ('wldwml011', '仓储合作方', 11),
    ('wldwml012', '质检机构', 12),
    ('wldwml013', '其他',     13)
) AS v(category_code, category_name, sort_order)
WHERE t.status = 1
  AND NOT EXISTS (
      SELECT 1 FROM biz_party_category c
      WHERE c.tenant_id = t.id
        AND c.party_type = 4
        AND c.category_name = v.category_name
        AND c.deleted = 0
  );

-- ------------------------------------------------------------
-- 2. 存量其他往来单位补多重身份 OTHER
-- ------------------------------------------------------------
UPDATE biz_party
SET roles = 'OTHER'
WHERE party_type = 4
  AND deleted = 0
  AND (roles IS NULL OR roles = '');
