-- ============================================================
-- V5.1.1: 修复租户名称编码问题
--
-- 问题：Flyway 执行时 UTF-8 编码未正确处理，
-- 导致 sys_tenant.tenant_name 中文字符变成乱码
--
-- 解决：直接更新为正确的 UTF-8 中文值
-- ============================================================

-- 修复系统租户名称
UPDATE sys_tenant
SET tenant_name = '系统租户'
WHERE id = 1;

-- 如果有其他租户的乱码问题，也需要修复
-- 可以通过以下查询识别乱码记录：
-- SELECT id, tenant_name, tenant_code FROM sys_tenant
-- WHERE tenant_name LIKE '%绉%' OR tenant_name NOT SIMILAR TO '[a-zA-Z0-9\u4e00-\u9fa5]+';