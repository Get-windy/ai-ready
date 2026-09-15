-- ============================================================
-- V11.72.0: 质量标准菜单双入口对齐
--
-- 背景：菜单 90202（quality:standard）为单入口（V9.5.0 注册），
--   点击菜单直接进列表页。与质检单 90201 / 报损单 5009 / 报溢单 5010
--   保持一致，实现「主菜单→新建表单、[历史]→列表」的双入口模式。
--   质量标准独立表单页 quality/standard/form（BillFormPage 五区布局）
--   与 next-no 取号已就绪。
-- ============================================================

UPDATE sys_menu SET
  path = 'quality/standard/form',
  component = 'quality/standard/form',
  list_path = 'quality/standard/list',
  tag_label = '历史',
  display_mode = 1,
  update_time = CURRENT_TIMESTAMP
WHERE id = 90202;
