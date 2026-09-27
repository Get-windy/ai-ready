-- V11.519.0 补齐「前端 v-permission 在用、库里没有」的权限点（共 29 个）
--
-- 背景：v-permission 校验不到就 removeChild（不是置灰）⇒ 码不存在 = 按钮对所有人
--       隐藏且不报错（管理员也看不见）。后端侧已实测 missing_in_db=0，所以这些都是
--       前端在用的功能点，属**系统级权限点定义缺失**：系统模块是系统级的，其余模块是
--       租户级的，但模块使用权由系统授予，租户无权凭空造权限点。
--
-- ⚠️ 版本号原拟 V11.515.0，实测与并行会话的 Grant_Analytics_Permissions 撞车
--    （Flyway 遇同版本号直接拒绝启动）⇒ 改到空闲的 V11.519.0。
--
-- 口径：tenant_id=0（系统级定义）/ permission_type=3（按钮级）/ visible=1 / status=0
-- 授权：只授 SYSTEM_ADMIN（租户内最高管理角色）；SUPER_ADMIN 走 * 通配无需授权行；
--       其余角色按「租户超管自行分配」的模型，不在本迁移里替租户决定。
--
-- 【回滚】DELETE FROM sys_role_permission WHERE id BETWEEN 9169200 AND 9169229;
--         DELETE FROM sys_permission     WHERE id BETWEEN 131000 AND 131028;

-- ── ① 权限点定义（已存在则跳过，可重复执行）──
INSERT INTO sys_permission
  (id, tenant_id, parent_id, deleted, create_time, update_time, permission_name,
   permission_code, permission_type, path, component, icon, api_path, method,
   sort, visible, status, remark)
VALUES
  (131000, 0, 0, 0, now(), now(), '批次-删除',
   'erp:batch:delete', 3, NULL, NULL, NULL, NULL, NULL,
   2000, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131001, 0, 0, 0, now(), now(), '固定资产-计提折旧',
   'erp:fixed-asset:asset:depreciate', 3, NULL, NULL, NULL, NULL, NULL,
   2001, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131002, 0, 0, 0, now(), now(), '折旧-计提计算',
   'erp:fixed-asset:depreciation:calculate', 3, NULL, NULL, NULL, NULL, NULL,
   2002, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131003, 0, 0, 0, now(), now(), '固资采购-验收',
   'erp:fixed-asset:purchase:accept', 3, NULL, NULL, NULL, NULL, NULL,
   2003, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131004, 0, 0, 0, now(), now(), '固资报表-查询',
   'erp:fixed-asset:report:query', 3, NULL, NULL, NULL, NULL, NULL,
   2004, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131005, 0, 0, 0, now(), now(), '往来单位-启用停用',
   'erp:partner:toggle-status', 3, NULL, NULL, NULL, NULL, NULL,
   2005, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131006, 0, 0, 0, now(), now(), '商品-库存模式设置',
   'erp:product:inventory-mode', 3, NULL, NULL, NULL, NULL, NULL,
   2006, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131007, 0, 0, 0, now(), now(), '序列号-删除',
   'erp:serial:delete', 3, NULL, NULL, NULL, NULL, NULL,
   2007, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131008, 0, 0, 0, now(), now(), '序列号-导出',
   'erp:serial:export', 3, NULL, NULL, NULL, NULL, NULL,
   2008, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131009, 0, 0, 0, now(), now(), '押金-查看',
   'finance:deposit:view', 3, NULL, NULL, NULL, NULL, NULL,
   2009, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131010, 0, 0, 0, now(), now(), '核销单-删除',
   'finance:offset:delete', 3, NULL, NULL, NULL, NULL, NULL,
   2010, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131011, 0, 0, 0, now(), now(), '核销单-编辑',
   'finance:offset:edit', 3, NULL, NULL, NULL, NULL, NULL,
   2011, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131012, 0, 0, 0, now(), now(), '应付-核销',
   'finance:payable:writeoff', 3, NULL, NULL, NULL, NULL, NULL,
   2012, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131013, 0, 0, 0, now(), now(), '付款单-取消',
   'finance:payment:cancel', 3, NULL, NULL, NULL, NULL, NULL,
   2013, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131014, 0, 0, 0, now(), now(), '付款单-完成',
   'finance:payment:complete', 3, NULL, NULL, NULL, NULL, NULL,
   2014, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131015, 0, 0, 0, now(), now(), '预付款-冲抵',
   'finance:pre-payment:offset', 3, NULL, NULL, NULL, NULL, NULL,
   2015, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131016, 0, 0, 0, now(), now(), '预付款-收回',
   'finance:pre-payment:recover', 3, NULL, NULL, NULL, NULL, NULL,
   2016, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131017, 0, 0, 0, now(), now(), '预付款-退款',
   'finance:pre-payment:refund', 3, NULL, NULL, NULL, NULL, NULL,
   2017, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131018, 0, 0, 0, now(), now(), '收款单-取消',
   'finance:receipt:cancel', 3, NULL, NULL, NULL, NULL, NULL,
   2018, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131019, 0, 0, 0, now(), now(), '收款单-完成',
   'finance:receipt:complete', 3, NULL, NULL, NULL, NULL, NULL,
   2019, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131020, 0, 0, 0, now(), now(), '应收-坏账',
   'finance:receivable:baddebt', 3, NULL, NULL, NULL, NULL, NULL,
   2020, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131021, 0, 0, 0, now(), now(), '应收-核销',
   'finance:receivable:writeoff', 3, NULL, NULL, NULL, NULL, NULL,
   2021, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131022, 0, 0, 0, now(), now(), '核销-编辑',
   'finance:writeoff:edit', 3, NULL, NULL, NULL, NULL, NULL,
   2022, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131023, 0, 0, 0, now(), now(), '订单中心-批量导出',
   'order:center:batchexport', 3, NULL, NULL, NULL, NULL, NULL,
   2023, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131024, 0, 0, 0, now(), now(), '销售退货单-打印',
   'sale:return-doc:print', 3, NULL, NULL, NULL, NULL, NULL,
   2024, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131025, 0, 0, 0, now(), now(), '供应商询价-接受报价',
   'supplier:inquiry:acceptquotation', 3, NULL, NULL, NULL, NULL, NULL,
   2025, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131026, 0, 0, 0, now(), now(), '供应商询价-拒绝报价',
   'supplier:inquiry:rejectquotation', 3, NULL, NULL, NULL, NULL, NULL,
   2026, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131027, 0, 0, 0, now(), now(), '供应商绩效-评分',
   'supplier:performance:evaluate', 3, NULL, NULL, NULL, NULL, NULL,
   2027, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐'),
  (131028, 0, 0, 0, now(), now(), '供应商门户-进入',
   'supplier:portal', 3, NULL, NULL, NULL, NULL, NULL,
   2028, 1, 0, '2026-09-27 前端 v-permission 漂移修复补齐')
ON CONFLICT (id) DO NOTHING;

-- ── ② 授给系统管理员（租户内最高管理角色）──
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT
  9169200 + row_number() OVER (ORDER BY p.id),
  r.id, p.id, 1, now()
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'SYSTEM_ADMIN'
  AND p.id BETWEEN 131000 AND 131028
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);
