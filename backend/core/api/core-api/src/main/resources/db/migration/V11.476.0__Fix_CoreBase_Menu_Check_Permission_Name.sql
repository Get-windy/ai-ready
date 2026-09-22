-- 修正 V11.471.0 里 `system:menu:check` 的显示名（2026-09-21，E-01 corebase 批次修正批）
--
-- 【为什么要单独一批】V11.471.0 生成时，`RESOURCE_LABEL` 里还没有 `menu` 这个键，
--   于是 `label_for()` 直接落了资源名的英文原文 ⇒ 该码在权限矩阵里显示成
--   「平台基础**menu**校验」（与本族既有码「菜单管理新增」的中文风格不一致）。
--
-- 【为什么不直接改 V11.471.0】它**已经被 Flyway 应用过**（`flyway_schema_history` 记了校验和），
--   再改文件内容会让下次启动报 `Migration checksum mismatch` ⇒ 必须用新增版本修正
--   （同 V11.469.0 修正 PriceApprovalController 那批的先例）。
--   同时已给生成器的 `RESOURCE_LABEL` 补上 `'menu': '菜单'`，
--   避免**将来**重新生成时再拼出半英文名。
--
-- 【范围】只改这 1 行，且带 `permission_name = 旧值` 的守卫（幂等，重复执行不再改）。
-- =============================================================

UPDATE sys_permission
SET permission_name = '平台基础菜单校验',
    update_time = now()
WHERE permission_code = 'system:menu:check'
  AND permission_name = '平台基础menu校验';
