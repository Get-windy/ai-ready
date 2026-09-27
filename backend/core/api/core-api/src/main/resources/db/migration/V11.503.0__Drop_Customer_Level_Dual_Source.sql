-- V11.503.0: 客户级别收敛到单一数据源 biz_customer_grade
--
-- 背景（2026-09-24 资料模块审计 · D-7）：
--   同一业务概念「客户级别」曾有三处数据源：
--     ① erp_customer_level  —— 资料 → 往来单位 → 客户 → 「客户级别」子标签的 CRUD（CustomerLevelController）
--     ② biz_customer_grade  —— 客户表单下拉 / 列表筛选 / 商城买家账号 / 店铺设置 / 客户分析 共 6 处读取
--     ③ erp_partner_grade   —— 仅被 ProductPriceQueryMapper 的客户级别下拉 UNION 引用
--   后果：在子标签新建的级别不会出现在任何下拉里，在表单里维护的级别也看不到子标签。
--   ② 的字段是 ① 的超集（多 grade_level/icon/color/point_rate/credit_limit/免运费等电商属性），
--   且 ③ 早在本迁移之前就被遗留表 V9.13.2 计划 DROP（却又被 DatabaseInitializer 的旧种子重建）。
--
-- 处置：唯一保留 ②（biz_customer_grade，biz_ 前缀符合当前主数据命名体系），
--       下线 ① 的表与权限码、② 之外的死 UNION 支。
-- 代码侧同步：删除 erp-partner 的 cn.aiedge.erp.customer 包（5 个文件），
--             CustomerGradeController 补 /page、启用过滤、编码唯一与删除引用保护。

-- 1) 旧权限码（其端点 /api/erp/customer/level/* 已随 CustomerLevelController 一并删除）
DELETE FROM sys_role_permission
 WHERE permission_id IN (SELECT id FROM sys_permission
                          WHERE permission_code LIKE 'party:customer-level:%');

UPDATE sys_permission
   SET deleted = 1, status = 0, update_time = NOW()
 WHERE permission_code LIKE 'party:customer-level:%' AND deleted = 0;

-- 2) 旧表下线（两表在收敛前均已 0 行）
DROP TABLE IF EXISTS erp_customer_level CASCADE;
DROP TABLE IF EXISTS erp_partner_grade CASCADE;
