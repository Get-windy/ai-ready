-- V8.16.0: 修复"添加"标签双入口菜单的 path/listPath 错位
-- 规则：MegaMenuPanel 中"添加"标签 → 主按钮跳 listPath（列表页），标签按钮跳 path（表单页）
-- 因此 path 应指向表单组件路由，listPath 应指向列表页路由
--
-- 受影响项：
--   50503 骑手管理, 50504 车辆管理 (path/listPath 互换)
--   80200 客户(CRM), 80210 线索(CRM), 80220 商机(CRM), 80230 合同(CRM) (listPath 指向表单→改为列表)
--   80510 客户(资料), 80511 供应商(资料), 80512 物流公司(资料) (path/listPath 互换)
--   80513 其他往来单位 (path→md/partner/form 表单, listPath=md/partner/index 列表)

-- 1. 50503 骑手管理: path↔listPath
UPDATE sys_menu SET path = 'dms/rider/form', list_path = 'dms/rider', update_time = CURRENT_TIMESTAMP WHERE id = 50503;

-- 2. 50504 车辆管理: path↔listPath
UPDATE sys_menu SET path = 'dms/vehicle/form', list_path = 'dms/vehicle', update_time = CURRENT_TIMESTAMP WHERE id = 50504;

-- 3. 80200 客户(CRM): listPath 从 crm/customer/form → crm/customer (列表页)
UPDATE sys_menu SET list_path = 'crm/customer', update_time = CURRENT_TIMESTAMP WHERE id = 80200;

-- 4. 80210 线索(CRM): listPath 从 crm/lead/form → crm/lead (列表页)
UPDATE sys_menu SET list_path = 'crm/lead', update_time = CURRENT_TIMESTAMP WHERE id = 80210;

-- 5. 80220 商机(CRM): listPath 从 crm/opportunity/form → crm/opportunity (列表页)
UPDATE sys_menu SET list_path = 'crm/opportunity', update_time = CURRENT_TIMESTAMP WHERE id = 80220;

-- 6. 80230 合同(CRM): listPath 从 crm/contract/form → crm/contract (列表页)
UPDATE sys_menu SET list_path = 'crm/contract', update_time = CURRENT_TIMESTAMP WHERE id = 80230;

-- 7. 80510 客户(资料): path↔listPath (path→md/customer/form 表单, listPath→md/customer 列表)
UPDATE sys_menu SET path = 'md/customer/form', list_path = 'md/customer', update_time = CURRENT_TIMESTAMP WHERE id = 80510;

-- 8. 80511 供应商(资料): path↔listPath (path→md/supplier/form 表单, listPath→md/supplier 列表)
UPDATE sys_menu SET path = 'md/supplier/form', list_path = 'md/supplier', update_time = CURRENT_TIMESTAMP WHERE id = 80511;

-- 9. 80512 物流公司(资料): path↔listPath (path→md/logistics/form 表单, listPath→md/logistics 列表)
UPDATE sys_menu SET path = 'md/logistics/form', list_path = 'md/logistics', update_time = CURRENT_TIMESTAMP WHERE id = 80512;

-- 10. 80513 其他往来单位: path→md/partner/form (表单), listPath=md/partner/index (列表) 保持不变
UPDATE sys_menu SET path = 'md/partner/form', update_time = CURRENT_TIMESTAMP WHERE id = 80513;
