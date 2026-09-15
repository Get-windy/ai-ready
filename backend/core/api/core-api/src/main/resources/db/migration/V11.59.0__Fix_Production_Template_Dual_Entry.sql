-- ═══════════════════════════════════════════════════════════════════
-- V11.59.0 生产模板双入口菜单修正（对齐 ql361 生产模板开发文档）
-- 文档双入口配置：
--   主菜单路径（点击进新增表单）= erp/stock-bom/form
--   标签「列表」路径（进列表）   = erp/stock-bom/index
-- 修正前：主入口=erp/stock-bom(列表)，列表标签=wh/production-template(旧版页)
-- ═══════════════════════════════════════════════════════════════════

UPDATE sys_menu
SET path = 'erp/stock-bom/form',
    component = 'erp/stock-bom/form',
    list_path = 'erp/stock-bom/index',
    tag_label = '列表',
    display_mode = 1,
    update_time = CURRENT_TIMESTAMP
WHERE id = 5014 AND display_mode = 1 AND deleted = 0;
