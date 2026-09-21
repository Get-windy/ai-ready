-- =============================================================================
-- 补齐「业务语义权限码」：字段级（价格/成本）+ 动作级（反审核/红冲）+ 资金账户级
-- V11.432.0 · 2026-09-20
--
-- 【问题】原 sys_permission 里全是**技术粒度**的权限码（`模块:页面:动作`，如 `system:user:create`），
--   完全没有 ql361（及业界生产级 ERP）都有的**业务语义权限**：
--     · 看不到"成本价/进货价/批发价/零售价"该不该给 —— 库里 0 条相关码（实测）
--     · "改单价/改折扣/改税率"该不该给 —— 0 条
--     · "反审核/红冲/作废/改单据日期/看他人草稿" —— 0 条
--     · "选收款账户/看账户余额" —— 0 条
--   后果：角色权限页配不出这些能力，只能配"能不能进这个页面/调这个接口"。
--
-- 【对标依据】2026-09-19/20 实抓 ql361「角色权限设置」面板：
--   · 「价格权限」域 18 项（列结构 = `行号|所属模块|功能名称|允许`，**单列开关**）：
--       成本查看 / 采购单据不控制成本权限 / 修改单价 / 修改折扣 / 修改税率 /
--       采购单据不控制修改价格 / 预设进价查看 / 批发价查看 / 零售价查看 / 发货单查看单价 …
--   · 「特殊权限」域 27 项：单据配置 / 修改单据日期 / 单据日期大于当前日期 / 草稿单打印 /
--       反审核 / 红冲单据 / 对账标记 / 作废优惠券 / 客商合并 / 强制结算 / 销售开单查看库存 …
--   · 「全部操作员」页另有 **7 类数据权限**（仓库/调拨/部门/往来单位/商品/现金银行/客户级别），
--     那是"数据级"（已有 `sys_user_data_scope` 承载），与本迁移的"字段级/动作级"是两层，不要混。
--
-- 【业界口径】（三层模型，多来源一致）
--   ① 功能级：能不能用这个功能/页面      → 我方已有（454 条权限码）
--   ② 数据级：能看哪些部门/仓库/存货的数据 → 我方已有（7 类数据权限，但业务侧尚未消费）
--   ③ **字段级**：能不能看某个敏感字段（成本价、折扣、银行账号）→ **本迁移补齐**
--   参考：ERPNext 的 Perm Level（字段分级 0/1/2）+ Data Masking；Odoo 的字段级 groups；
--   SAP 的字段选择组 + Authorization Object + 组织级；用友 T+ 的"功能级+数据级+字段级"三层。
--   ⚠️ 共同提醒：前端隐藏只是体验层，**敏感数据必须后端强制过滤/脱敏**，否则直连接口可绕过。
--
-- 【命名口径】用**我方模块术语**命名（`模块:子模块:动作`），不照抄 ql361 的界面用语，
--   以便与既有 454 条保持一致的命名体系、并被权限矩阵的域归并规则正确归域。
--
-- 【id 号段实测依据】（devdb 2026-09-20）
--   SELECT count(*) FROM sys_permission WHERE id BETWEEN 91561 AND 91600;   → 0（整段空闲）
--   SELECT min(id), max(id), count(*) FROM sys_permission WHERE id >= 91500 AND id < 92000;
--     → 91501 / 91692 / 152（已占用 91550..91560 连续段，故从 **91561** 起分配）
--   ⚠️ sys_permission.id 无序列默认值，必须显式给值。
--
-- 【字段口径】tenant_id=1 / parent_id=0 / deleted=0 / permission_type=3（按钮型）
--   / api_path=NULL / method=NULL / visible=1 / **status=0（0=正常）** / sort 续 1034 起。
--
-- 【幂等】全部 `ON CONFLICT (id) DO NOTHING`。
-- 【不授权】不向任何角色授予这些新码 —— 租户应在「岗位权限」页自行勾选；
--   非超管在授码前看不到对应按钮属预期（超管走 `*` 通配不受影响）。
--
-- 【回滚】
--   DELETE FROM sys_permission WHERE id BETWEEN 91561 AND 91578;
--
-- 迁移版本号：实测该目录最大为 V11.431.0，故取 V11.432.0。
-- ⚠️ 本迁移只**登记权限码**，不等于这些权限已在业务代码里生效 —— 消费点（按权限显隐成本价、
--    校验改折扣、仓库数据过滤等）需逐模块接线，见《岗位权限开发文档》「仍未闭环」一节。
-- =============================================================================

-- ── ① 价格/成本类（字段级）────────────────────────────────────────────────
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
VALUES
 (91561, 1, 0, 0, now(), now(), '商品成本价查看',   'product:cost:view',           3, NULL, NULL, 1034, 1, 0),
 (91562, 1, 0, 0, now(), now(), '商品进货价查看',   'product:purchase-price:view', 3, NULL, NULL, 1035, 1, 0),
 (91563, 1, 0, 0, now(), now(), '商品批发价查看',   'product:wholesale-price:view',3, NULL, NULL, 1036, 1, 0),
 (91564, 1, 0, 0, now(), now(), '商品零售价查看',   'product:retail-price:view',   3, NULL, NULL, 1037, 1, 0),
 (91565, 1, 0, 0, now(), now(), '销售单价修改',     'sale:price:edit',             3, NULL, NULL, 1038, 1, 0),
 (91566, 1, 0, 0, now(), now(), '销售折扣修改',     'sale:discount:edit',          3, NULL, NULL, 1039, 1, 0),
 (91567, 1, 0, 0, now(), now(), '采购单价修改',     'purchase:price:edit',         3, NULL, NULL, 1040, 1, 0)
ON CONFLICT (id) DO NOTHING;

-- ── ② 单据动作类（特殊权限，动作级）────────────────────────────────────────
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
VALUES
 (91568, 1, 0, 0, now(), now(), '单据反审核',       'doc:unapprove',          3, NULL, NULL, 1041, 1, 0),
 (91569, 1, 0, 0, now(), now(), '单据红冲',         'doc:reverse',            3, NULL, NULL, 1042, 1, 0),
 (91570, 1, 0, 0, now(), now(), '单据作废',         'doc:void',               3, NULL, NULL, 1043, 1, 0),
 (91571, 1, 0, 0, now(), now(), '单据日期修改',     'doc:date:edit',          3, NULL, NULL, 1044, 1, 0),
 (91572, 1, 0, 0, now(), now(), '他人草稿查看',     'doc:draft:view-others',  3, NULL, NULL, 1045, 1, 0),
 (91573, 1, 0, 0, now(), now(), '强制结算',         'sale:settle:force',      3, NULL, NULL, 1046, 1, 0),
 (91574, 1, 0, 0, now(), now(), '客商合并',         'partner:merge',          3, NULL, NULL, 1047, 1, 0),
 (91575, 1, 0, 0, now(), now(), '草稿单打印',       'print:draft',            3, NULL, NULL, 1048, 1, 0)
ON CONFLICT (id) DO NOTHING;

-- ── ③ 资金账户类（字段/动作级）────────────────────────────────────────────
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
VALUES
 (91576, 1, 0, 0, now(), now(), '收款账户选择',     'receipt:account:select', 3, NULL, NULL, 1049, 1, 0),
 (91577, 1, 0, 0, now(), now(), '付款账户选择',     'payment:account:select', 3, NULL, NULL, 1050, 1, 0),
 (91578, 1, 0, 0, now(), now(), '账户余额查看',     'finance:balance:view',   3, NULL, NULL, 1051, 1, 0)
ON CONFLICT (id) DO NOTHING;
