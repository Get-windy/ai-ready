-- ============================================================
-- V6.3.0: 按业务流向导入菜单种子数据
--
-- 设计原则：
--   1. 菜单按"业务关联度"和"操作流向"聚合，而非技术模块
--   2. display_group=1 的节点为纯展示分组，不影响路由注册
--   3. 所有菜单 client_type='pc-admin', tenant_id=0（全局）
--   4. 菜单ID范围 50000-59999，避免与 Snowflake 冲突
--   5. sort 排序号按操作流向自然排列（100 递增）
--
-- 一级菜单分组：
--   销售作业 → 采购作业 → 仓储作业 → WMS → 配送管理
--   → 客户关系 → 财务管理 → 费用管理 → 资产管理
--   → 预算管理 → 商城管理 → 订单中心 → 工作流
--   → 产品数据 → 打印管理 → 系统管理
-- ============================================================

-- ============================================================
-- 0. 工作台 (Dashboard)
-- ============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50010, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '工作台', 'dashboard', 0, 'dashboard', NULL, 'DashboardOutlined', 10, 1, 1, 'pc-admin', NULL, 0);

INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50011, 0, 50010, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '工作台', 'dashboard:index', 1, 'dashboard', 'views/dashboard/index.vue', 'DashboardOutlined', 100, 1, 1, 'pc-admin', NULL, 0);

-- ============================================================
-- 1. 销售作业 (Sales Operations) - display_group=1
--    流程：报价 → 订单 → 出库 → 发货 → 退货 → 分析报表
-- ============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50100, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '销售作业', 'sales-ops', 0, 'sales-ops', NULL, 'ShoppingOutlined', 20, 1, 1, 'pc-admin', 'sales', 1);

-- 报价管理 (CRM Quotation)
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50101, 0, 50100, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '报价管理', 'sales:quotation', 1, 'crm/quotation', 'views/crm/quotation/index.vue', 'DollarOutlined', 100, 1, 1, 'pc-admin', 'sales', 0);

-- 销售订单（独立列表页）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50103, 0, 50100, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '销售订单', 'sales:order', 1, 'erp/sale', 'views/erp/sale/index.vue', 'FileTextOutlined', 200, 1, 1, 'pc-admin', 'sales', 0);

-- 发货管理
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50105, 0, 50100, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '发货管理', 'sales:shipment', 1, 'erp/shipment', 'views/erp/shipment/index.vue', 'SendOutlined', 400, 1, 1, 'pc-admin', 'sales', 0);

-- 退货处理
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50106, 0, 50100, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '退货处理', 'sales:return', 1, 'erp/return', 'views/erp/return/index.vue', 'RollbackOutlined', 500, 1, 1, 'pc-admin', 'sales', 0);

-- 销售分析
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50107, 0, 50100, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '销售分析', 'sales:analysis', 1, 'erp/sales-analysis', 'views/erp/sales-analysis/index.vue', 'BarChartOutlined', 600, 1, 1, 'pc-admin', 'sales', 0);

-- 销售报表
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50108, 0, 50100, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '销售报表', 'sales:report', 1, 'erp/sales-report', 'views/erp/sales-report/index.vue', 'LineChartOutlined', 700, 1, 1, 'pc-admin', 'sales', 0);

-- ============================================================
-- 2. 采购作业 (Purchase Operations) - display_group=1
--    流程：询价/请购 → 采购订单 → 入库 → 换货 → 退货
-- ============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50200, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '采购作业', 'purchase-ops', 0, 'purchase-ops', NULL, 'ShoppingCartOutlined', 30, 1, 1, 'pc-admin', 'purchase', 1);

-- 采购订单
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50202, 0, 50200, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '采购订单', 'purchase:order', 1, 'erp/purchase', 'views/erp/purchase/index.vue', 'ShoppingCartOutlined', 100, 1, 1, 'pc-admin', 'purchase', 0);

-- 入库管理
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50203, 0, 50200, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '入库管理', 'purchase:stock-in', 1, 'erp/stock-in', 'views/erp/stock-in/index.vue', 'InboxOutlined', 200, 1, 1, 'pc-admin', 'purchase', 0);

-- 采购退货
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50204, 0, 50200, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '采购退货', 'purchase:return', 1, 'erp/purchase-return', 'views/erp/purchase-return/index.vue', 'RollbackOutlined', 300, 1, 1, 'pc-admin', 'purchase', 0);

-- 采购换货
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50205, 0, 50200, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '采购换货', 'purchase:exchange', 1, 'erp/purchase-exchange', 'views/erp/purchase-exchange/index.vue', 'SwapOutlined', 400, 1, 1, 'pc-admin', 'purchase', 0);

-- ============================================================
-- 3. 仓储作业 (Warehouse Operations) - display_group=1
--    流程：库存查询 → 盘点 → 调拨 → 成本调整 → 预警
--         → BOM → 组装/拆分 → 溢损处理
-- ============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50300, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '仓储作业', 'warehouse-ops', 0, 'warehouse-ops', NULL, 'ContainerOutlined', 40, 1, 1, 'pc-admin', 'warehouse', 1);

-- 库存查询
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50301, 0, 50300, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库存查询', 'warehouse:stock', 1, 'erp/stock', 'views/erp/stock/index.vue', 'ContainerOutlined', 100, 1, 1, 'pc-admin', 'warehouse', 0);

-- 库存盘点
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50302, 0, 50300, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库存盘点', 'warehouse:stocktake', 1, 'erp/stocktake', 'views/erp/stocktake/index.vue', 'CheckSquareOutlined', 200, 1, 1, 'pc-admin', 'warehouse', 0);

-- 库存调拨
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50303, 0, 50300, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库存调拨', 'warehouse:transfer', 1, 'erp/stock-transfer', 'views/erp/stock-transfer/index.vue', 'SwapOutlined', 300, 1, 1, 'pc-admin', 'warehouse', 0);

-- 库存成本调整
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50304, 0, 50300, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库存成本调整', 'warehouse:cost-adjust', 1, 'erp/stock-cost-adjust', 'views/erp/stock-cost-adjust/index.vue', 'DollarOutlined', 400, 1, 1, 'pc-admin', 'warehouse', 0);

-- 库存溢余处理
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50305, 0, 50300, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库存溢余处理', 'warehouse:overflow', 1, 'erp/stock-overflow', 'views/erp/stock-overflow/index.vue', 'PlusCircleOutlined', 500, 1, 1, 'pc-admin', 'warehouse', 0);

-- 库存报损处理
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50306, 0, 50300, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库存报损处理', 'warehouse:damage', 1, 'erp/stock-damage', 'views/erp/stock-damage/index.vue', 'MinusCircleOutlined', 600, 1, 1, 'pc-admin', 'warehouse', 0);

-- 批次管理
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50307, 0, 50300, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '批次管理', 'warehouse:batch', 1, 'erp/batch', 'views/erp/batch/index.vue', 'BarcodeOutlined', 700, 1, 1, 'pc-admin', 'warehouse', 0);

-- 序列号管理
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50308, 0, 50300, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '序列号管理', 'warehouse:serial', 1, 'erp/serial', 'views/erp/serial/index.vue', 'NumberOutlined', 800, 1, 1, 'pc-admin', 'warehouse', 0);

-- BOM管理
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50309, 0, 50300, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'BOM管理', 'warehouse:bom', 1, 'erp/stock-bom', 'views/erp/stock-bom/index.vue', 'DeploymentUnitOutlined', 900, 1, 1, 'pc-admin', 'warehouse', 0);

-- 组装管理
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50310, 0, 50300, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '组装管理', 'warehouse:assemble', 1, 'erp/stock-assemble', 'views/erp/stock-assemble/index.vue', 'BuildOutlined', 1000, 1, 1, 'pc-admin', 'warehouse', 0);

-- 拆分管理
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50311, 0, 50300, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '拆分管理', 'warehouse:split', 1, 'erp/stock-split', 'views/erp/stock-split/index.vue', 'ScissorOutlined', 1100, 1, 1, 'pc-admin', 'warehouse', 0);

-- 库存预警配置
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50312, 0, 50300, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库存预警配置', 'warehouse:alert-config', 1, 'erp/stock-alert-config', 'views/erp/stock-alert-config/index.vue', 'AlertOutlined', 1200, 1, 1, 'pc-admin', 'warehouse', 0);

-- 补货管理
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50313, 0, 50300, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '补货管理', 'warehouse:replenishment', 1, 'erp/stock/replenishment', 'views/erp/stock/replenishment/index.vue', 'StockOutlined', 1300, 1, 1, 'pc-admin', 'warehouse', 0);

-- ============================================================
-- 4. WMS 仓储执行 (Warehouse Execution) - 保留原有独立模块
-- ============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50400, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'WMS仓储执行', 'wms', 0, 'wms', NULL, 'BankOutlined', 50, 1, 1, 'pc-admin', 'wms', 0);

INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50401, 0, 50400, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '仓库管理', 'wms:warehouse', 1, 'wms/warehouse', 'views/wms/warehouse/index.vue', 'BankOutlined', 100, 1, 1, 'pc-admin', 'wms', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50402, 0, 50400, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库位管理', 'wms:location', 1, 'wms/location', 'views/wms/location/index.vue', 'EnvironmentOutlined', 200, 1, 1, 'pc-admin', 'wms', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50403, 0, 50400, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '收货管理', 'wms:receipt', 1, 'wms/receipt', 'views/wms/receipt/index.vue', 'InboxOutlined', 300, 1, 1, 'pc-admin', 'wms', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50404, 0, 50400, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '上架管理', 'wms:putaway', 1, 'wms/putaway', 'views/wms/putaway/index.vue', 'VerticalAlignTopOutlined', 400, 1, 1, 'pc-admin', 'wms', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50405, 0, 50400, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '拣货管理', 'wms:pick', 1, 'wms/pick', 'views/wms/pick/index.vue', 'ShoppingOutlined', 500, 1, 1, 'pc-admin', 'wms', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50406, 0, 50400, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '波次管理', 'wms:wave', 1, 'wms/wave', 'views/wms/wave/index.vue', 'PartitionOutlined', 600, 1, 1, 'pc-admin', 'wms', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50407, 0, 50400, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '发货管理', 'wms:ship', 1, 'wms/ship', 'views/wms/ship/index.vue', 'SendOutlined', 700, 1, 1, 'pc-admin', 'wms', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50408, 0, 50400, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库存查询', 'wms:inventory', 1, 'wms/inventory', 'views/wms/inventory/index.vue', 'ContainerOutlined', 800, 1, 1, 'pc-admin', 'wms', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50409, 0, 50400, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库内移库', 'wms:move', 1, 'wms/move', 'views/wms/move/index.vue', 'SwapOutlined', 900, 1, 1, 'pc-admin', 'wms', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50410, 0, 50400, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '盘点管理', 'wms:check', 1, 'wms/check', 'views/wms/check/index.vue', 'CheckSquareOutlined', 1000, 1, 1, 'pc-admin', 'wms', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50411, 0, 50400, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库内事件', 'wms:event', 1, 'wms/event', 'views/wms/event/index.vue', 'AlertOutlined', 1100, 1, 1, 'pc-admin', 'wms', 0);

-- ============================================================
-- 5. 配送管理 (Delivery Management) - 保留原有独立模块
-- ============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50500, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '配送管理', 'delivery', 0, 'dms', NULL, 'CarOutlined', 60, 1, 1, 'pc-admin', 'delivery', 0);

INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50501, 0, 50500, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '配送仪表盘', 'delivery:dashboard', 1, 'dms/dashboard', 'views/dms/dashboard/index.vue', 'DashboardOutlined', 100, 1, 1, 'pc-admin', 'delivery', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50502, 0, 50500, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '渠道管理', 'delivery:channel', 1, 'dms/channel', 'views/dms/channel/index.vue', 'ApartmentOutlined', 200, 1, 1, 'pc-admin', 'delivery', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50503, 0, 50500, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '骑手管理', 'delivery:rider', 1, 'dms/rider', 'views/dms/rider/index.vue', 'UserOutlined', 300, 1, 1, 'pc-admin', 'delivery', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50504, 0, 50500, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '车辆管理', 'delivery:vehicle', 1, 'dms/vehicle', 'views/dms/vehicle/index.vue', 'TruckOutlined', 400, 1, 1, 'pc-admin', 'delivery', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50505, 0, 50500, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '配送路线', 'delivery:route', 1, 'dms/route', 'views/dms/route/index.vue', 'RouteOutlined', 500, 1, 1, 'pc-admin', 'delivery', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50506, 0, 50500, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '智能调度', 'delivery:dispatch', 1, 'dms/dispatch', 'views/dms/dispatch/index.vue', 'SyncOutlined', 600, 1, 1, 'pc-admin', 'delivery', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50507, 0, 50500, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '订单池', 'delivery:order-pool', 1, 'dms/order-pool', 'views/dms/order-pool/index.vue', 'OrderedListOutlined', 700, 1, 1, 'pc-admin', 'delivery', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50508, 0, 50500, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '配送跟踪', 'delivery:tracking', 1, 'dms/tracking', 'views/dms/tracking/index.vue', 'CompassOutlined', 800, 1, 1, 'pc-admin', 'delivery', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50509, 0, 50500, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '实名认证', 'delivery:verification', 1, 'dms/verification', 'views/dms/verification/index.vue', 'SafetyOutlined', 900, 1, 1, 'pc-admin', 'delivery', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50510, 0, 50500, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '配送配置', 'delivery:config', 1, 'dms/config', 'views/dms/config/index.vue', 'SettingOutlined', 1000, 1, 1, 'pc-admin', 'delivery', 0);

-- ============================================================
-- 6. 客户关系 (Customer Relations) - display_group=1
--    流程：线索 → 商机 → 客户 → 合同 → 发票
--         → 供应商 → 询价 → 绩效
-- ============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50600, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '客户关系', 'customer-ops', 0, 'customer-ops', NULL, 'TeamOutlined', 70, 1, 1, 'pc-admin', 'customer', 1);

INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50601, 0, 50600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '线索管理', 'customer:lead', 1, 'crm/lead', 'views/crm/lead/index.vue', 'UserAddOutlined', 100, 1, 1, 'pc-admin', 'customer', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50602, 0, 50600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '商机管理', 'customer:opportunity', 1, 'crm/opportunity', 'views/crm/opportunity/index.vue', 'BulbOutlined', 200, 1, 1, 'pc-admin', 'customer', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50603, 0, 50600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '客户档案', 'customer:customer', 1, 'crm/customer', 'views/crm/customer/index.vue', 'TeamOutlined', 300, 1, 1, 'pc-admin', 'customer', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50604, 0, 50600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '合同管理', 'customer:contract', 1, 'crm/contract', 'views/crm/contract/index.vue', 'FileTextOutlined', 400, 1, 1, 'pc-admin', 'customer', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50605, 0, 50600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '发票管理', 'customer:invoice', 1, 'crm/invoice', 'views/crm/invoice/index.vue', 'DollarOutlined', 500, 1, 1, 'pc-admin', 'customer', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50606, 0, 50600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '供应商管理', 'customer:supplier', 1, 'supplier', 'views/supplier/index.vue', 'TeamOutlined', 600, 1, 1, 'pc-admin', 'customer', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50607, 0, 50600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '供应商询价', 'customer:supplier-inquiry', 1, 'supplier/inquiry', 'views/supplier/inquiry/index.vue', 'SearchOutlined', 700, 1, 1, 'pc-admin', 'customer', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50608, 0, 50600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '供应商绩效', 'customer:supplier-performance', 1, 'supplier/performance', 'views/supplier/performance/index.vue', 'StarOutlined', 800, 1, 1, 'pc-admin', 'customer', 0);

-- ============================================================
-- 7. 财务管理 (Finance & Accounting) - display_group=1
--    流程：科目 → 凭证 → 应收 → 应付 → 收款
--         → 付款 → 核销 → 对冲 → 报表 → 对账
-- ============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50700, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '财务管理', 'finance-ops', 0, 'finance-ops', NULL, 'DollarOutlined', 80, 1, 1, 'pc-admin', 'finance', 1);

INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50701, 0, 50700, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '科目管理', 'finance:subject', 1, 'finance/subject', 'views/finance/subject/index.vue', 'FileTextOutlined', 100, 1, 1, 'pc-admin', 'finance', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50702, 0, 50700, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '凭证管理', 'finance:voucher', 1, 'finance/voucher', 'views/finance/voucher/index.vue', 'FileTextOutlined', 200, 1, 1, 'pc-admin', 'finance', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50703, 0, 50700, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '应收账款', 'finance:receivable', 1, 'finance/receivable', 'views/finance/receivable/index.vue', 'DollarOutlined', 300, 1, 1, 'pc-admin', 'finance', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50704, 0, 50700, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '应付账款', 'finance:payable', 1, 'finance/payable', 'views/finance/payable/index.vue', 'DollarOutlined', 400, 1, 1, 'pc-admin', 'finance', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50705, 0, 50700, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '应收明细', 'finance:ar-detail', 1, 'finance/accounts-receivable', 'views/finance/accounts-receivable/index.vue', 'DollarOutlined', 310, 1, 1, 'pc-admin', 'finance', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50706, 0, 50700, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '应收账龄分析', 'finance:ar-aging', 1, 'finance/accounts-receivable/aging-analysis', 'views/finance/accounts-receivable/aging-analysis.vue', 'BarChartOutlined', 320, 1, 1, 'pc-admin', 'finance', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50707, 0, 50700, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '应付明细', 'finance:ap-detail', 1, 'finance/accounts-payable', 'views/finance/accounts-payable/index.vue', 'DollarOutlined', 410, 1, 1, 'pc-admin', 'finance', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50708, 0, 50700, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '收款单管理', 'finance:receipt', 1, 'finance/receipt', 'views/finance/receipt/index.vue', 'DollarOutlined', 500, 1, 1, 'pc-admin', 'finance', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50709, 0, 50700, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '付款单管理', 'finance:payment', 1, 'finance/payment', 'views/finance/payment/index.vue', 'DollarOutlined', 600, 1, 1, 'pc-admin', 'finance', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50710, 0, 50700, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '预收款管理', 'finance:pre-receipt', 1, 'finance/pre-receipt', 'views/finance/pre-receipt/index.vue', 'DollarOutlined', 700, 1, 1, 'pc-admin', 'finance', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50711, 0, 50700, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '预付款管理', 'finance:pre-payment', 1, 'finance/pre-payment', 'views/finance/pre-payment/index.vue', 'DollarOutlined', 800, 1, 1, 'pc-admin', 'finance', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50712, 0, 50700, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '定金押金管理', 'finance:deposit', 1, 'finance/deposit', 'views/finance/deposit/index.vue', 'DollarOutlined', 900, 1, 1, 'pc-admin', 'finance', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50713, 0, 50700, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '收付款核销', 'finance:write-off', 1, 'finance/write-off', 'views/finance/write-off/index.vue', 'CheckCircleOutlined', 1000, 1, 1, 'pc-admin', 'finance', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50714, 0, 50700, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '往来对冲', 'finance:offset', 1, 'finance/offset', 'views/finance/offset/index.vue', 'SwapOutlined', 1100, 1, 1, 'pc-admin', 'finance', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50715, 0, 50700, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '资金流水台账', 'finance:capital-flow', 1, 'finance/capital-flow', 'views/finance/capital-flow/index.vue', 'FileTextOutlined', 1200, 1, 1, 'pc-admin', 'finance', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50716, 0, 50700, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '财务报表', 'finance:report', 1, 'finance/report', 'views/finance/report/index.vue', 'BarChartOutlined', 1300, 1, 1, 'pc-admin', 'finance', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50717, 0, 50700, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '对账管理', 'finance:reconciliation', 1, 'finance/reconciliation', 'views/finance/reconciliation/index.vue', 'AuditOutlined', 1400, 1, 1, 'pc-admin', 'finance', 0);

-- ============================================================
-- 8. 费用管理 (Expense Management) - display_group=1
--    流程：费用申请 → 费用报销 → 审批 → 付款 → 统计
-- ============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50800, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '费用管理', 'expense-ops', 0, 'expense-ops', NULL, 'MoneyCollectOutlined', 90, 1, 1, 'pc-admin', 'expense', 1);

INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50801, 0, 50800, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '费用申请', 'expense:application', 1, 'erp/expense/application', 'views/erp/expense/application/index.vue', 'FileAddOutlined', 100, 1, 1, 'pc-admin', 'expense', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50802, 0, 50800, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '费用报销', 'expense:reimbursement', 1, 'erp/expense/reimbursement', 'views/erp/expense/reimbursement/index.vue', 'DollarOutlined', 200, 1, 1, 'pc-admin', 'expense', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50803, 0, 50800, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '费用审批', 'expense:approval', 1, 'erp/expense/approval', 'views/erp/expense/approval/index.vue', 'AuditOutlined', 300, 1, 1, 'pc-admin', 'expense', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50804, 0, 50800, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '费用付款', 'expense:payment', 1, 'erp/expense/payment', 'views/erp/expense/payment/index.vue', 'DollarOutlined', 400, 1, 1, 'pc-admin', 'expense', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50805, 0, 50800, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '费用统计', 'expense:statistics', 1, 'erp/expense/statistics', 'views/erp/expense/statistics/index.vue', 'BarChartOutlined', 500, 1, 1, 'pc-admin', 'expense', 0);

-- ============================================================
-- 9. 资产管理 (Fixed Asset Management)
-- ============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50900, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '资产管理', 'asset', 0, 'fixed-asset', NULL, 'BankOutlined', 100, 1, 1, 'pc-admin', 'asset', 0);

INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50901, 0, 50900, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '资产台账', 'asset:list', 1, 'fixed-asset/asset', 'views/fixed-asset/asset/index.vue', 'BankOutlined', 100, 1, 1, 'pc-admin', 'asset', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50902, 0, 50900, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '资产分类', 'asset:category', 1, 'fixed-asset/category', 'views/fixed-asset/category/index.vue', 'AppstoreOutlined', 200, 1, 1, 'pc-admin', 'asset', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50903, 0, 50900, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '资产采购', 'asset:purchase', 1, 'fixed-asset/purchase', 'views/fixed-asset/purchase/index.vue', 'ShoppingCartOutlined', 300, 1, 1, 'pc-admin', 'asset', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50904, 0, 50900, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '折旧管理', 'asset:depreciation', 1, 'fixed-asset/depreciation', 'views/fixed-asset/depreciation/index.vue', 'DollarOutlined', 400, 1, 1, 'pc-admin', 'asset', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50905, 0, 50900, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '资产调拨', 'asset:transfer', 1, 'fixed-asset/transfer', 'views/fixed-asset/transfer/index.vue', 'SwapOutlined', 500, 1, 1, 'pc-admin', 'asset', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50906, 0, 50900, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '资产处置', 'asset:disposal', 1, 'fixed-asset/disposal', 'views/fixed-asset/disposal/index.vue', 'DeleteOutlined', 600, 1, 1, 'pc-admin', 'asset', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50907, 0, 50900, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '资产盘点', 'asset:inventory', 1, 'fixed-asset/inventory', 'views/fixed-asset/inventory/index.vue', 'CheckSquareOutlined', 700, 1, 1, 'pc-admin', 'asset', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (50908, 0, 50900, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '资产报表', 'asset:report', 1, 'fixed-asset/report', 'views/fixed-asset/report/index.vue', 'BarChartOutlined', 800, 1, 1, 'pc-admin', 'asset', 0);

-- ============================================================
-- 10. 预算管理 (Budget Management)
-- ============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51000, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '预算管理', 'budget', 0, 'budget', NULL, 'FundOutlined', 110, 1, 1, 'pc-admin', 'budget', 0);

INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51001, 0, 51000, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '预算概览', 'budget:dashboard', 1, 'budget', 'views/budget/index.vue', 'FundOutlined', 100, 1, 1, 'pc-admin', 'budget', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51002, 0, 51000, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '预算模板', 'budget:template', 1, 'budget/template', 'views/budget/template/index.vue', 'FileTextOutlined', 200, 1, 1, 'pc-admin', 'budget', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51003, 0, 51000, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '年度预算', 'budget:annual', 1, 'budget/annual', 'views/budget/annual/index.vue', 'DollarOutlined', 300, 1, 1, 'pc-admin', 'budget', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51004, 0, 51000, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '预算调整', 'budget:adjustment', 1, 'budget/adjustment', 'views/budget/adjustment/index.vue', 'EditOutlined', 400, 1, 1, 'pc-admin', 'budget', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51005, 0, 51000, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '预算报表', 'budget:report', 1, 'budget/report', 'views/budget/report/index.vue', 'BarChartOutlined', 500, 1, 1, 'pc-admin', 'budget', 0);

-- ============================================================
-- 11. 商城管理 (Mall Management)
-- ============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51100, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '商城管理', 'mall', 0, 'mall', NULL, 'ShopOutlined', 120, 1, 1, 'pc-admin', 'mall', 0);

INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51101, 0, 51100, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '商城配置', 'mall:config', 1, 'mall/config', 'views/erp/mall/config/index.vue', 'SettingOutlined', 100, 1, 1, 'pc-admin', 'mall', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51102, 0, 51100, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '商品管理', 'mall:product', 1, 'mall/product', 'views/erp/mall/product/index.vue', 'AppstoreOutlined', 200, 1, 1, 'pc-admin', 'mall', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51103, 0, 51100, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '订单管理', 'mall:order', 1, 'mall/order', 'views/erp/mall/order/index.vue', 'ShoppingCartOutlined', 300, 1, 1, 'pc-admin', 'mall', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51104, 0, 51100, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '用户审核', 'mall:user-audit', 1, 'mall/user-audit', 'views/erp/mall/user-audit/index.vue', 'AuditOutlined', 400, 1, 1, 'pc-admin', 'mall', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51105, 0, 51100, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '轮播图管理', 'mall:banner', 1, 'mall/banner', 'views/erp/mall/banner/index.vue', 'PictureOutlined', 500, 1, 1, 'pc-admin', 'mall', 0);

-- ============================================================
-- 12. 订单中心 (Order Center)
-- ============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51200, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '订单中心', 'orders', 0, 'order-center', NULL, 'UnorderedListOutlined', 130, 1, 1, 'pc-admin', 'orders', 0);

INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51201, 0, 51200, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '订单中心', 'orders:index', 1, 'order-center', 'views/order-center/index.vue', 'UnorderedListOutlined', 100, 1, 1, 'pc-admin', 'orders', 0);

-- ============================================================
-- 13. 工作流 (Workflow)
-- ============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51300, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '工作流', 'workflow', 0, 'workflow', NULL, 'AuditOutlined', 140, 1, 1, 'pc-admin', 'workflow', 0);

INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51301, 0, 51300, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '流程监控', 'workflow:monitor', 1, 'workflow/instance-monitor', 'views/workflow/instance-monitor.vue', 'MonitorOutlined', 100, 1, 1, 'pc-admin', 'workflow', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51302, 0, 51300, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '任务管理', 'workflow:task', 1, 'workflow/task-management', 'views/workflow/task-management.vue', 'AuditOutlined', 200, 1, 1, 'pc-admin', 'workflow', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51303, 0, 51300, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '流程分析', 'workflow:analysis', 1, 'workflow/process-analysis', 'views/workflow/process-analysis.vue', 'BarChartOutlined', 300, 1, 1, 'pc-admin', 'workflow', 0);

-- ============================================================
-- 14. 产品数据 (Product Data) - display_group=1
-- ============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51400, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '产品数据', 'product-data', 0, 'product-data', NULL, 'AppstoreOutlined', 150, 1, 1, 'pc-admin', 'product', 1);

INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51401, 0, 51400, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '产品管理', 'product:product', 1, 'erp/product', 'views/erp/product/index.vue', 'AppstoreOutlined', 100, 1, 1, 'pc-admin', 'product', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51402, 0, 51400, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '往来单位', 'product:partner', 1, 'erp/partner', 'views/erp/partner/index.vue', 'TeamOutlined', 200, 1, 1, 'pc-admin', 'product', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51403, 0, 51400, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '定价管理', 'product:pricing', 1, 'erp/pricing', 'views/erp/pricing/index.vue', 'DollarOutlined', 300, 1, 1, 'pc-admin', 'product', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51404, 0, 51400, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '定价审批', 'product:pricing-approval', 1, 'erp/pricing/approval', 'views/erp/pricing/approval/index.vue', 'AuditOutlined', 400, 1, 1, 'pc-admin', 'product', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51405, 0, 51400, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '价格层级', 'product:pricing-tiers', 1, 'erp/pricing/tiers', 'views/erp/pricing/tiers/index.vue', 'PullRequestOutlined', 500, 1, 1, 'pc-admin', 'product', 0);

-- ============================================================
-- 15. 打印管理 (Printing)
-- ============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51500, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '打印管理', 'printing', 0, 'printing', NULL, 'PrinterOutlined', 160, 1, 1, 'pc-admin', 'printing', 0);

INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51501, 0, 51500, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '打印模板', 'printing:template', 1, 'printing/template', 'views/printing/template/index.vue', 'FileTextOutlined', 100, 1, 1, 'pc-admin', 'printing', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51502, 0, 51500, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '打印链路', 'printing:chain', 1, 'printing/chain', 'views/printing/chain/index.vue', 'LinkOutlined', 200, 1, 1, 'pc-admin', 'printing', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51503, 0, 51500, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '打印客户端', 'printing:client', 1, 'printing/client', 'views/printing/client/index.vue', 'LaptopOutlined', 300, 1, 1, 'pc-admin', 'printing', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51504, 0, 51500, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '打印任务', 'printing:task', 1, 'printing/task', 'views/printing/task/index.vue', 'AuditOutlined', 400, 1, 1, 'pc-admin', 'printing', 0);

-- ============================================================
-- 16. 系统管理 (System Settings)
-- ============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51600, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '系统管理', 'system', 0, 'system', NULL, 'SettingOutlined', 170, 1, 1, 'pc-admin', 'system', 0);

INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51601, 0, 51600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '组织架构', 'system:dept', 1, 'system/department', 'views/system/department/index.vue', 'ApartmentOutlined', 100, 1, 1, 'pc-admin', 'system', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51602, 0, 51600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '岗位管理', 'system:position', 1, 'system/position', 'views/system/position/index.vue', 'IdcardOutlined', 200, 1, 1, 'pc-admin', 'system', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51603, 0, 51600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '用户管理', 'system:user', 1, 'system/user', 'views/system/user/index.vue', 'UserOutlined', 300, 1, 1, 'pc-admin', 'system', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51604, 0, 51600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '角色管理', 'system:role', 1, 'system/role', 'views/system/role/index.vue', 'SafetyOutlined', 400, 1, 1, 'pc-admin', 'system', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51605, 0, 51600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '权限管理', 'system:permission', 1, 'system/permission', 'views/system/permission/index.vue', 'LockOutlined', 500, 1, 1, 'pc-admin', 'system', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51606, 0, 51600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '菜单管理', 'system:menu', 1, 'system/menu', 'views/system/menu/index.vue', 'MenuOutlined', 600, 1, 1, 'pc-admin', 'system', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51607, 0, 51600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '租户管理', 'system:tenant', 1, 'system/tenant', 'views/system/tenant/index.vue', 'ApartmentOutlined', 700, 1, 1, 'pc-admin', 'system', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51608, 0, 51600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '租户审批', 'system:tenant-approval', 1, 'system/tenant-approval', 'views/system/tenant-approval/index.vue', 'SafetyOutlined', 800, 1, 1, 'pc-admin', 'system', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51609, 0, 51600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '字典管理', 'system:dict', 1, 'system/dict', 'views/system/dict/index.vue', 'BookOutlined', 900, 1, 1, 'pc-admin', 'system', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51610, 0, 51600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '系统配置', 'system:config', 1, 'system/config', 'views/system/config/index.vue', 'SettingOutlined', 1000, 1, 1, 'pc-admin', 'system', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51611, 0, 51600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '系统日志', 'system:log', 1, 'system/log', 'views/system/log/index.vue', 'FileTextOutlined', 1100, 1, 1, 'pc-admin', 'system', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51612, 0, 51600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '数据导入', 'system:data-import', 1, 'system/data-import', 'views/system/data-import/index.vue', 'ImportOutlined', 1200, 1, 1, 'pc-admin', 'system', 0);
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51613, 0, 51600, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '通知公告', 'system:notification', 1, 'notification', 'views/notification/index.vue', 'BellOutlined', 1300, 1, 1, 'pc-admin', 'system', 0);

-- ============================================================
-- 17. 其他独立页面
-- ============================================================
-- 图表
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51700, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '图表', 'charts', 0, 'charts', NULL, 'BarChartOutlined', 180, 1, 1, 'pc-admin', NULL, 0);

INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, biz_flow_tag, display_group)
VALUES
  (51701, 0, 51700, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '图表', 'charts:index', 1, 'charts', 'views/charts/index.vue', 'BarChartOutlined', 100, 1, 1, 'pc-admin', NULL, 0);

-- ============================================================
-- 完成
-- ============================================================
