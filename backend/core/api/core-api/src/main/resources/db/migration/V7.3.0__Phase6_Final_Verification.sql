-- V7.3.0: Phase 6 最终验证
-- 检查所有 displayMode=1 菜单项的 path/component/listPath 是否正确对齐

SELECT
  id,
  menu_name,
  path,
  component,
  list_path,
  tag_label,
  display_mode,
  CASE
    WHEN component IS NULL THEN '⚠️ 组件为空'
    WHEN component LIKE '%placeholder%' THEN '📋 placeholder'
    ELSE '✅ 已实现'
  END AS status
FROM sys_menu
WHERE display_mode = 1 AND deleted = 0
ORDER BY id;

-- 统计
SELECT
  COUNT(*) AS total,
  SUM(CASE WHEN component LIKE '%placeholder%' THEN 1 ELSE 0 END) AS placeholder_count,
  SUM(CASE WHEN component NOT LIKE '%placeholder%' AND component IS NOT NULL THEN 1 ELSE 0 END) AS implemented_count
FROM sys_menu
WHERE display_mode = 1 AND deleted = 0;
