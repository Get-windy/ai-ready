-- Fix menu management page showing fewer menus than homepage
-- Root cause: 76 menus had tenant_id=1 but management page queries tenant_id=0
-- All system-defined menus should have tenant_id=0

UPDATE sys_menu SET tenant_id = 0 WHERE tenant_id != 0 AND deleted = 0;
