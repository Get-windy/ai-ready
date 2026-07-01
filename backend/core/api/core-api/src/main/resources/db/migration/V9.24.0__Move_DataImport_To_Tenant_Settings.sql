-- =====================================================
-- V9.24.0: 数据导入从系统管理移至租户设置模块
-- 新建"设置"顶级菜单（租户级），"数据录入"子目录
-- 将"期初录入"改名为"数据录入"，数据导入移入其下
-- =====================================================

-- 1. 删除系统管理下的旧"数据导入"菜单（将由新的16004替代）
DELETE FROM sys_menu WHERE id = 2012;

-- 2. 尝试将已存在的"期初录入"目录改名为"数据录入"
--    如果"期初录入"不存在（fresh install），DO块会跳过
DO $$
DECLARE
    v_found_id BIGINT;
BEGIN
    -- 查找可能存在的"期初录入"菜单
    SELECT id INTO v_found_id FROM sys_menu WHERE menu_name = '期初录入' AND deleted = 0 LIMIT 1;
    IF v_found_id IS NOT NULL THEN
        UPDATE sys_menu SET menu_name = '数据录入', menu_code = 'DataEntry' WHERE id = v_found_id;
    END IF;
END $$;

-- 3. 确保"设置"顶级菜单及其子菜单存在
--    DatabaseInitializer 启动时也会通过 MERGE 同步，此处保证迁移立即生效
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, deleted)
VALUES
    (16000, 0, 0, '设置', 'TenantSettings', 0, 'set', NULL, 'ToolOutlined', 175, 1, 0, 0),
    (16001, 0, 16000, '数据录入', 'DataEntry', 0, 'set/data-entry', NULL, 'DatabaseOutlined', 1, 1, 0, 0),
    (16002, 0, 16001, '期初库存', 'InitialStock', 1, 'set/initial-stock', 'set/initial-stock/index', 'AppstoreOutlined', 1, 1, 0, 0),
    (16003, 0, 16001, '期初财务', 'InitialFinance', 1, 'set/initial-finance', 'set/initial-finance/index', 'DollarOutlined', 2, 1, 0, 0),
    (16004, 0, 16001, '数据导入', 'DataImport', 1, 'system/data-import', 'system/data-import/index', 'ImportOutlined', 3, 1, 0, 0),
    (16005, 0, 16000, '系统重建', 'SetRebuild', 1, 'set/rebuild', 'set/rebuild/index', 'ToolOutlined', 2, 1, 0, 0),
    (16006, 0, 16000, '系统任务', 'SetSystemTask', 1, 'set/system-task', 'set/system-task/index', 'ScheduleOutlined', 3, 1, 0, 0),
    (16007, 0, 16000, '会计期间', 'SetAccountingPeriod', 1, 'set/accounting-period', 'set/accounting-period/index', 'CalendarOutlined', 4, 1, 0, 0),
    (16008, 0, 16000, '操作日志', 'SetOperationLog', 1, 'set/operation-log', 'set/operation-log/index', 'FileTextOutlined', 5, 1, 0, 0),
    (16009, 0, 16000, '系统参数', 'SetSysParams', 1, 'set/sys-params', 'set/sys-params/index', 'SettingOutlined', 6, 1, 0, 0),
    (16010, 0, 16000, '企业信息', 'SetCompanyInfo', 1, 'set/company-info', 'set/company-info/index', 'BankOutlined', 7, 1, 0, 0),
    (16011, 0, 16000, '菜单配置', 'SetMenuConfig', 1, 'set/menu-config', 'set/menu-config/index', 'MenuOutlined', 8, 1, 0, 0),
    (16012, 0, 16000, '审批配置', 'SetAuditConfig', 1, 'set/audit-config', 'set/audit-config/index', 'AuditOutlined', 9, 1, 0, 0),
    (16013, 0, 16000, '支付配置', 'SetPaymentConfig', 1, 'set/payment-config', 'set/payment-config/index', 'PayCircleOutlined', 10, 1, 0, 0),
    (16014, 0, 16000, '应用中心', 'SetAppCenter', 1, 'set/app-center', 'set/app-center/index', 'AppstoreOutlined', 11, 1, 0, 0)
ON CONFLICT (id) DO NOTHING;
