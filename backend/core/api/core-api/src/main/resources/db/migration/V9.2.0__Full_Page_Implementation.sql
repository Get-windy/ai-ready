-- =====================================================
-- V9.2.0: 全量菜单页面实现 - 替换 placeholder 指向实际页面
-- =====================================================
-- 将所有 component='views/common/placeholder/index.vue' 的菜单
-- 更新为指向各模块实际 Vue 页面组件
-- =====================================================

-- 销售模块 - 外勤拜访
UPDATE sys_menu SET component = 'views/sales/visit-plan/index.vue' WHERE id = 70001;
UPDATE sys_menu SET component = 'views/sales/visit-exec/index.vue' WHERE id = 70002;
UPDATE sys_menu SET component = 'views/sales/visit-review/index.vue' WHERE id = 70003;

-- 销售模块 - 订货业务
UPDATE sys_menu SET component = 'views/sales/return-apply/index.vue' WHERE id = 70011;
UPDATE sys_menu SET component = 'views/sales/pre-order/index.vue' WHERE id = 70012;

-- 销售模块 - 销售业务
UPDATE sys_menu SET component = 'views/sales/retail/index.vue' WHERE id = 70020;
UPDATE sys_menu SET component = 'views/sales/outbound/index.vue' WHERE id = 70021;
UPDATE sys_menu SET component = 'views/sales/return-doc/index.vue' WHERE id = 70022;
UPDATE sys_menu SET component = 'views/sales/exchange/index.vue' WHERE id = 70023;

-- 销售模块 - 销售查询
UPDATE sys_menu SET component = 'views/sales/doc-query/index.vue' WHERE id = 70030;
UPDATE sys_menu SET component = 'views/sales/detail-query/index.vue' WHERE id = 70031;
UPDATE sys_menu SET component = 'views/sales/price-track/index.vue' WHERE id = 70032;

-- 采购模块 - 采购准备
UPDATE sys_menu SET component = 'views/purchase/alert-replenish/index.vue' WHERE id = 70051;
UPDATE sys_menu SET component = 'views/purchase/shortage-replenish/index.vue' WHERE id = 70052;
UPDATE sys_menu SET component = 'views/purchase/smart-replenish/index.vue' WHERE id = 70053;
UPDATE sys_menu SET component = 'views/purchase/sales-driven/index.vue' WHERE id = 70054;

-- 采购模块 - 采购业务
UPDATE sys_menu SET component = 'views/purchase/inbound/index.vue' WHERE id = 70061;
UPDATE sys_menu SET component = 'views/purchase/return/index.vue' WHERE id = 70062;
UPDATE sys_menu SET component = 'views/purchase/exchange/index.vue' WHERE id = 70063;
UPDATE sys_menu SET component = 'views/purchase/cost-sharing/index.vue' WHERE id = 70064;

-- 采购模块 - 采购查询
UPDATE sys_menu SET component = 'views/purchase/doc-query/index.vue' WHERE id = 70070;
UPDATE sys_menu SET component = 'views/purchase/detail-query/index.vue' WHERE id = 70071;
UPDATE sys_menu SET component = 'views/purchase/price-track/index.vue' WHERE id = 70072;

-- 仓储模块 - 其他出入库
UPDATE sys_menu SET component = 'views/wh/other-outbound/index.vue' WHERE id = 70101;
UPDATE sys_menu SET component = 'views/wh/other-inbound/index.vue' WHERE id = 70102;
UPDATE sys_menu SET component = 'views/wh/transfer/index.vue' WHERE id = 70103;

-- 仓储模块 - 盘点
UPDATE sys_menu SET component = 'views/wh/damage/index.vue' WHERE id = 70110;
UPDATE sys_menu SET component = 'views/wh/overflow/index.vue' WHERE id = 70111;
UPDATE sys_menu SET component = 'views/wh/stocktake/index.vue' WHERE id = 70112;
UPDATE sys_menu SET component = 'views/wh/cost-adjust/index.vue' WHERE id = 70113;

-- 仓储模块 - 生产
UPDATE sys_menu SET component = 'views/wh/production-template/index.vue' WHERE id = 70120;
UPDATE sys_menu SET component = 'views/wh/assemble/index.vue' WHERE id = 70121;
UPDATE sys_menu SET component = 'views/wh/disassemble/index.vue' WHERE id = 70122;

-- 仓储模块 - 库存预警
UPDATE sys_menu SET component = 'views/wh/alert-query/index.vue' WHERE id = 70130;
UPDATE sys_menu SET component = 'views/wh/alert-config/index.vue' WHERE id = 70131;

-- 仓储模块 - 借进借出查询
UPDATE sys_menu SET component = 'views/wh/borrow-query/index.vue' WHERE id = 70142;

-- 配送模块
UPDATE sys_menu SET component = 'views/dispatch/query/index.vue' WHERE id = 70150;
UPDATE sys_menu SET component = 'views/dispatch/logistics-ship/index.vue' WHERE id = 70155;
UPDATE sys_menu SET component = 'views/dispatch/ship-query/index.vue' WHERE id = 70156;
UPDATE sys_menu SET component = 'views/dispatch/return-receive/index.vue' WHERE id = 70160;
UPDATE sys_menu SET component = 'views/dispatch/purchase-receive/index.vue' WHERE id = 70161;

-- 财务模块 - 收入支出
UPDATE sys_menu SET component = 'views/finance/expense-doc/index.vue' WHERE id = 70201;
UPDATE sys_menu SET component = 'views/finance/other-income/index.vue' WHERE id = 70202;
UPDATE sys_menu SET component = 'views/finance/ar-ap-adjust/index.vue' WHERE id = 70203;
UPDATE sys_menu SET component = 'views/finance/account-delivery/index.vue' WHERE id = 70204;

-- 财务模块 - 账簿
UPDATE sys_menu SET component = 'views/finance/general-ledger/index.vue' WHERE id = 70220;
UPDATE sys_menu SET component = 'views/finance/detail-ledger/index.vue' WHERE id = 70221;
UPDATE sys_menu SET component = 'views/finance/balance-sheet/index.vue' WHERE id = 70222;
UPDATE sys_menu SET component = 'views/finance/aux-balance/index.vue' WHERE id = 70223;

-- 财务模块 - 财务报表
UPDATE sys_menu SET component = 'views/finance/balance-report/index.vue' WHERE id = 70230;
UPDATE sys_menu SET component = 'views/finance/profit-report/index.vue' WHERE id = 70231;

-- 财务模块 - 费用管理
UPDATE sys_menu SET component = 'views/finance/expense-apply/index.vue' WHERE id = 70240;
UPDATE sys_menu SET component = 'views/finance/expense-reimburse/index.vue' WHERE id = 70241;
UPDATE sys_menu SET component = 'views/finance/expense-approval/index.vue' WHERE id = 70242;
UPDATE sys_menu SET component = 'views/finance/expense-pay/index.vue' WHERE id = 70243;
UPDATE sys_menu SET component = 'views/finance/expense-stats/index.vue' WHERE id = 70244;

-- CRM模块 - 补充页面
UPDATE sys_menu SET component = 'views/crm/customer-follow/index.vue' WHERE id = 70302;
UPDATE sys_menu SET component = 'views/crm/customer-grade/index.vue' WHERE id = 70303;
UPDATE sys_menu SET component = 'views/crm/lead-convert/index.vue' WHERE id = 70311;
UPDATE sys_menu SET component = 'views/crm/opportunity-stage/index.vue' WHERE id = 70321;
UPDATE sys_menu SET component = 'views/crm/contract-approval/index.vue' WHERE id = 70341;
UPDATE sys_menu SET component = 'views/crm/funnel/index.vue' WHERE id = 70360;
UPDATE sys_menu SET component = 'views/crm/customer-analysis/index.vue' WHERE id = 70361;

-- 资料管理 - 商品管理
UPDATE sys_menu SET component = 'views/md/barcode/index.vue' WHERE id = 70502;
UPDATE sys_menu SET component = 'views/md/product-price/index.vue' WHERE id = 70503;
UPDATE sys_menu SET component = 'views/md/product-aux/index.vue' WHERE id = 70504;
UPDATE sys_menu SET component = 'views/md/image/index.vue' WHERE id = 70505;

-- 资料管理 - 往来单位
UPDATE sys_menu SET component = 'views/md/customer/index.vue' WHERE id = 70510;
UPDATE sys_menu SET component = 'views/md/supplier/index.vue' WHERE id = 70511;
UPDATE sys_menu SET component = 'views/md/logistics/index.vue' WHERE id = 70512;
UPDATE sys_menu SET component = 'views/md/linked-account/index.vue' WHERE id = 70513;

-- 资料管理 - 仓库管理
UPDATE sys_menu SET component = 'views/md/location/index.vue' WHERE id = 70520;

-- 资料管理 - 配送管理
UPDATE sys_menu SET component = 'views/md/route/index.vue' WHERE id = 70530;

-- 资料管理 - 财务账户
UPDATE sys_menu SET component = 'views/md/bank-account/index.vue' WHERE id = 70540;
UPDATE sys_menu SET component = 'views/md/expense-type/index.vue' WHERE id = 70541;
UPDATE sys_menu SET component = 'views/md/other-income/index.vue' WHERE id = 70542;
UPDATE sys_menu SET component = 'views/md/accounting-subject/index.vue' WHERE id = 70543;

-- 系统设置 - 期初录入
UPDATE sys_menu SET component = 'views/set/initial-stock/index.vue' WHERE id = 70550;
UPDATE sys_menu SET component = 'views/set/initial-finance/index.vue' WHERE id = 70551;

-- 系统设置 - 账套操作
UPDATE sys_menu SET component = 'views/set/rebuild/index.vue' WHERE id = 70560;
UPDATE sys_menu SET component = 'views/set/system-task/index.vue' WHERE id = 70561;

-- 系统设置 - 财务设置
UPDATE sys_menu SET component = 'views/set/accounting-period/index.vue' WHERE id = 70570;
