-- ───────────────────────────────────────────────────────────
-- V11.64.0 借出单菜单双入口配置
-- 借出单（80011）与借进单（80010）共用 wms_borrow_order 表结构与字段，
-- 仅需补齐菜单双入口：主菜单->新增表单(wh/borrow-out/form)，历史标签->列表(wh/borrow-out/index)。
-- ───────────────────────────────────────────────────────────

UPDATE sys_menu SET
    menu_code   = 'wh:borrow-out',
    path        = 'wh/borrow-out/form',
    list_path   = 'wh/borrow-out/index',
    component   = 'views/wh/borrow-out/form/index.vue',
    display_mode = 1,
    tag_label   = '历史',
    update_time = CURRENT_TIMESTAMP
WHERE id = 80011;
