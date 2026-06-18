/**
 * Phase 2 - 基于点击的前端全页面测试（v2）
 *
 * 测试策略：
 * 1. 通过UI登录
 * 2. 从后端API获取实际菜单数据
 * 3. 通过点击侧边栏菜单项导航
 * 4. 测试每个页面的加载、CRUD按钮、生产级标准
 * 5. 分批处理（每批10页）
 * 6. 收集所有错误并分类
 */
import { test, expect, type Page, type BrowserContext } from '@playwright/test';
import { loginViaUi } from './fixtures/auth.fixture';
import * as fs from 'fs';
import * as path from 'path';

// ==================== 配置 ====================
const BASE_URL = 'http://localhost:5656';

// ==================== 错误分类 ====================
interface TestError {
  type: 'blocking' | 'api' | 'interaction' | 'style' | 'production-grade' | 'backend';
  page: string;
  title: string;
  message: string;
  timestamp: string;
  detail?: string;
}

const allErrors: TestError[] = [];

function addError(type: TestError['type'], page: string, title: string, message: string, detail?: string) {
  allErrors.push({ type, page, title, message, timestamp: new Date().toISOString(), detail });
}

// ==================== 页面定义 ====================
interface PageDef {
  route: string;
  title: string;
  menuText: string;       // 菜单上显示的文字
  parentMenu?: string;    // 父级菜单（如果有）
  hasTable: boolean;
  hasForm: boolean;
  hasButtons: boolean;
  buttons?: string[];
}

// 从后端实际菜单数据生成的页面定义
// 路由 → 页面定义映射
const ROUTE_PAGES: Record<string, PageDef> = {
  '/dashboard': { route: '/dashboard', title: '工作台', menuText: '工作台', hasTable: false, hasForm: false, hasButtons: false },

  // 系统管理
  '/system/department': { route: '/system/department', title: '组织架构', menuText: '组织架构', parentMenu: '系统管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/system/position': { route: '/system/position', title: '岗位管理', menuText: '岗位管理', parentMenu: '系统管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/system/user': { route: '/system/user', title: '用户管理', menuText: '用户管理', parentMenu: '系统管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/system/role': { route: '/system/role', title: '角色管理', menuText: '角色管理', parentMenu: '系统管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/system/permission': { route: '/system/permission', title: '权限管理', menuText: '权限管理', parentMenu: '系统管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/system/menu': { route: '/system/menu', title: '菜单管理', menuText: '菜单管理', parentMenu: '系统管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/system/tenant': { route: '/system/tenant', title: '租户管理', menuText: '租户管理', parentMenu: '系统管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/system/tenant-approval': { route: '/system/tenant-approval', title: '租户审批', menuText: '租户审批', parentMenu: '系统管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['审核'] },
  '/system/dict': { route: '/system/dict', title: '字典管理', menuText: '字典管理', parentMenu: '系统管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/system/config': { route: '/system/config', title: '系统配置', menuText: '系统配置', parentMenu: '系统管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['编辑'] },
  '/system/log': { route: '/system/log', title: '系统日志', menuText: '系统日志', parentMenu: '系统管理', hasTable: true, hasForm: false, hasButtons: false },
  '/system/data-import': { route: '/system/data-import', title: '数据导入', menuText: '数据导入', parentMenu: '系统管理', hasTable: false, hasForm: true, hasButtons: true, buttons: ['导入'] },
  '/notification': { route: '/notification', title: '通知公告', menuText: '通知公告', parentMenu: '系统管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['发布'] },

  // 工作流
  '/workflow/instance-monitor': { route: '/workflow/instance-monitor', title: '流程监控', menuText: '流程监控', parentMenu: '工作流', hasTable: true, hasForm: false, hasButtons: false },
  '/workflow/task-management': { route: '/workflow/task-management', title: '任务管理', menuText: '任务管理', parentMenu: '工作流', hasTable: true, hasForm: false, hasButtons: false },
  '/workflow/process-analysis': { route: '/workflow/process-analysis', title: '流程分析', menuText: '流程分析', parentMenu: '工作流', hasTable: false, hasForm: false, hasButtons: false },

  // 产品数据
  '/erp/product': { route: '/erp/product', title: '产品管理', menuText: '产品管理', parentMenu: '产品数据', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/partner': { route: '/erp/partner', title: '往来单位', menuText: '往来单位', parentMenu: '产品数据', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/pricing': { route: '/erp/pricing', title: '定价管理', menuText: '定价管理', parentMenu: '产品数据', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/pricing/approval': { route: '/erp/pricing/approval', title: '定价审批', menuText: '定价审批', parentMenu: '产品数据', hasTable: true, hasForm: false, hasButtons: false },
  '/erp/pricing/tiers': { route: '/erp/pricing/tiers', title: '价格层级', menuText: '价格层级', parentMenu: '产品数据', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },

  // 采购
  '/purchase': { route: '/purchase', title: '采购管理', menuText: '采购管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/purchase': { route: '/erp/purchase', title: '采购订单', menuText: '采购订单', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/stock-in': { route: '/erp/stock-in', title: '入库管理', menuText: '入库管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/purchase-return': { route: '/erp/purchase-return', title: '采购退货', menuText: '采购退货', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/purchase-exchange': { route: '/erp/purchase-exchange', title: '采购换货', menuText: '采购换货', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },

  // 销售
  '/sale': { route: '/sale', title: '销售管理', menuText: '销售管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/sale': { route: '/erp/sale', title: '销售订单', menuText: '销售订单', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/sale-outbound': { route: '/erp/sale-outbound', title: '销售出库', menuText: '销售出库', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/shipment': { route: '/erp/shipment', title: '发货管理', menuText: '发货管理', hasTable: true, hasForm: false, hasButtons: false },
  '/erp/return': { route: '/erp/return', title: '退货处理', menuText: '退货处理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/sales-analysis': { route: '/erp/sales-analysis', title: '销售分析', menuText: '销售分析', hasTable: false, hasForm: false, hasButtons: false },
  '/erp/sales-report': { route: '/erp/sales-report', title: '销售报表', menuText: '销售报表', hasTable: false, hasForm: false, hasButtons: false },

  // CRM
  '/crm/lead': { route: '/crm/lead', title: '线索管理', menuText: '线索管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/crm/opportunity': { route: '/crm/opportunity', title: '商机管理', menuText: '商机管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/crm/customer': { route: '/crm/customer', title: '客户档案', menuText: '客户档案', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/crm/contract': { route: '/crm/contract', title: '合同管理', menuText: '合同管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/crm/invoice': { route: '/crm/invoice', title: '发票管理', menuText: '发票管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/crm/quotation': { route: '/crm/quotation', title: '报价管理', menuText: '报价管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/supplier': { route: '/supplier', title: '供应商管理', menuText: '供应商管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/supplier/inquiry': { route: '/supplier/inquiry', title: '供应商询价', menuText: '供应商询价', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/supplier/performance': { route: '/supplier/performance', title: '供应商绩效', menuText: '供应商绩效', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },

  // 库存管理
  '/erp/stock': { route: '/erp/stock', title: '库存查询', menuText: '库存查询', hasTable: true, hasForm: false, hasButtons: false },
  '/erp/stocktake': { route: '/erp/stocktake', title: '库存盘点', menuText: '库存盘点', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/stock-transfer': { route: '/erp/stock-transfer', title: '库存调拨', menuText: '库存调拨', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/stock-cost-adjust': { route: '/erp/stock-cost-adjust', title: '库存成本调整', menuText: '库存成本调整', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/stock-overflow': { route: '/erp/stock-overflow', title: '库存溢余处理', menuText: '库存溢余处理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/stock-damage': { route: '/erp/stock-damage', title: '库存报损处理', menuText: '库存报损处理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/batch': { route: '/erp/batch', title: '批次管理', menuText: '批次管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/serial': { route: '/erp/serial', title: '序列号管理', menuText: '序列号管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/stock-alert-config': { route: '/erp/stock-alert-config', title: '库存预警配置', menuText: '库存预警配置', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/stock/replenishment': { route: '/erp/stock/replenishment', title: '补货管理', menuText: '补货管理', hasTable: true, hasForm: false, hasButtons: false },

  // WMS
  '/wms/warehouse': { route: '/wms/warehouse', title: '仓库管理', menuText: '仓库管理', parentMenu: 'WMS仓储执行', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/wms/location': { route: '/wms/location', title: '库位管理', menuText: '库位管理', parentMenu: 'WMS仓储执行', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/wms/receipt': { route: '/wms/receipt', title: '收货管理', menuText: '收货管理', parentMenu: 'WMS仓储执行', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/wms/putaway': { route: '/wms/putaway', title: '上架管理', menuText: '上架管理', parentMenu: 'WMS仓储执行', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/wms/pick': { route: '/wms/pick', title: '拣货管理', menuText: '拣货管理', parentMenu: 'WMS仓储执行', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/wms/wave': { route: '/wms/wave', title: '波次管理', menuText: '波次管理', parentMenu: 'WMS仓储执行', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/wms/ship': { route: '/wms/ship', title: '发货管理', menuText: '发货管理', parentMenu: 'WMS仓储执行', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/wms/inventory': { route: '/wms/inventory', title: '库存查询', menuText: '库存查询', parentMenu: 'WMS仓储执行', hasTable: true, hasForm: false, hasButtons: false },
  '/wms/move': { route: '/wms/move', title: '库内移库', menuText: '库内移库', parentMenu: 'WMS仓储执行', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/wms/check': { route: '/wms/check', title: '盘点管理', menuText: '盘点管理', parentMenu: 'WMS仓储执行', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/wms/event': { route: '/wms/event', title: '库内事件', menuText: '库内事件', parentMenu: 'WMS仓储执行', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },

  // DMS
  '/dms/dashboard': { route: '/dms/dashboard', title: '配送仪表盘', menuText: '配送仪表盘', parentMenu: '配送管理', hasTable: false, hasForm: false, hasButtons: false },
  '/dms/channel': { route: '/dms/channel', title: '渠道管理', menuText: '渠道管理', parentMenu: '配送管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/dms/rider': { route: '/dms/rider', title: '骑手管理', menuText: '骑手管理', parentMenu: '配送管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/dms/vehicle': { route: '/dms/vehicle', title: '车辆管理', menuText: '车辆管理', parentMenu: '配送管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/dms/route': { route: '/dms/route', title: '配送路线', menuText: '配送路线', parentMenu: '配送管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/dms/dispatch': { route: '/dms/dispatch', title: '智能调度', menuText: '智能调度', parentMenu: '配送管理', hasTable: true, hasForm: false, hasButtons: false },
  '/dms/order-pool': { route: '/dms/order-pool', title: '订单池', menuText: '订单池', parentMenu: '配送管理', hasTable: true, hasForm: false, hasButtons: false },
  '/dms/tracking': { route: '/dms/tracking', title: '配送跟踪', menuText: '配送跟踪', parentMenu: '配送管理', hasTable: true, hasForm: false, hasButtons: false },
  '/dms/verification': { route: '/dms/verification', title: '实名认证', menuText: '实名认证', parentMenu: '配送管理', hasTable: true, hasForm: false, hasButtons: false },
  '/dms/config': { route: '/dms/config', title: '配送配置', menuText: '配送配置', parentMenu: '配送管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },

  // 财务
  '/finance/subject': { route: '/finance/subject', title: '科目管理', menuText: '科目管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/finance/voucher': { route: '/finance/voucher', title: '凭证管理', menuText: '凭证管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/finance/receivable': { route: '/finance/receivable', title: '应收账款', menuText: '应收账款', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/finance/accounts-receivable': { route: '/finance/accounts-receivable', title: '应收明细', menuText: '应收明细', hasTable: true, hasForm: false, hasButtons: false },
  '/finance/accounts-receivable/aging-analysis': { route: '/finance/accounts-receivable/aging-analysis', title: '应收账龄分析', menuText: '应收账龄分析', hasTable: true, hasForm: false, hasButtons: false },
  '/finance/payable': { route: '/finance/payable', title: '应付账款', menuText: '应付账款', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/finance/accounts-payable': { route: '/finance/accounts-payable', title: '应付明细', menuText: '应付明细', hasTable: true, hasForm: false, hasButtons: false },
  '/finance/receipt': { route: '/finance/receipt', title: '收款单管理', menuText: '收款单管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/finance/payment': { route: '/finance/payment', title: '付款单管理', menuText: '付款单管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/finance/pre-receipt': { route: '/finance/pre-receipt', title: '预收款管理', menuText: '预收款管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/finance/pre-payment': { route: '/finance/pre-payment', title: '预付款管理', menuText: '预付款管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/finance/deposit': { route: '/finance/deposit', title: '定金押金管理', menuText: '定金押金管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/finance/write-off': { route: '/finance/write-off', title: '收付款核销', menuText: '收付款核销', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/finance/offset': { route: '/finance/offset', title: '往来对冲', menuText: '往来对冲', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/finance/capital-flow': { route: '/finance/capital-flow', title: '资金流水台账', menuText: '资金流水台账', hasTable: true, hasForm: false, hasButtons: false },
  '/finance/report': { route: '/finance/report', title: '财务报表', menuText: '财务报表', hasTable: true, hasForm: false, hasButtons: false },
  '/finance/reconciliation': { route: '/finance/reconciliation', title: '对账管理', menuText: '对账管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },

  // 费用
  '/erp/expense/application': { route: '/erp/expense/application', title: '费用申请', menuText: '费用申请', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/expense/reimbursement': { route: '/erp/expense/reimbursement', title: '费用报销', menuText: '费用报销', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/erp/expense/approval': { route: '/erp/expense/approval', title: '费用审批', menuText: '费用审批', hasTable: true, hasForm: false, hasButtons: false },
  '/erp/expense/payment': { route: '/erp/expense/payment', title: '费用付款', menuText: '费用付款', hasTable: true, hasForm: false, hasButtons: false },
  '/erp/expense/statistics': { route: '/erp/expense/statistics', title: '费用统计', menuText: '费用统计', hasTable: false, hasForm: false, hasButtons: false },

  // 固定资产
  '/fixed-asset/asset': { route: '/fixed-asset/asset', title: '资产台账', menuText: '资产台账', parentMenu: '资产管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/fixed-asset/category': { route: '/fixed-asset/category', title: '资产分类', menuText: '资产分类', parentMenu: '资产管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/fixed-asset/purchase': { route: '/fixed-asset/purchase', title: '资产采购', menuText: '资产采购', parentMenu: '资产管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/fixed-asset/depreciation': { route: '/fixed-asset/depreciation', title: '折旧管理', menuText: '折旧管理', parentMenu: '资产管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/fixed-asset/transfer': { route: '/fixed-asset/transfer', title: '资产调拨', menuText: '资产调拨', parentMenu: '资产管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/fixed-asset/disposal': { route: '/fixed-asset/disposal', title: '资产处置', menuText: '资产处置', parentMenu: '资产管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/fixed-asset/inventory': { route: '/fixed-asset/inventory', title: '资产盘点', menuText: '资产盘点', parentMenu: '资产管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/fixed-asset/report': { route: '/fixed-asset/report', title: '资产报表', menuText: '资产报表', parentMenu: '资产管理', hasTable: true, hasForm: false, hasButtons: false },

  // 预算
  '/budget': { route: '/budget', title: '预算概览', menuText: '预算概览', parentMenu: '预算管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/budget/template': { route: '/budget/template', title: '预算模板', menuText: '预算模板', parentMenu: '预算管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/budget/annual': { route: '/budget/annual', title: '年度预算', menuText: '年度预算', parentMenu: '预算管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/budget/adjustment': { route: '/budget/adjustment', title: '预算调整', menuText: '预算调整', parentMenu: '预算管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/budget/report': { route: '/budget/report', title: '预算报表', menuText: '预算报表', parentMenu: '预算管理', hasTable: true, hasForm: false, hasButtons: false },

  // 商城
  '/mall/config': { route: '/mall/config', title: '商城配置', menuText: '商城配置', parentMenu: '商城管理', hasTable: false, hasForm: true, hasButtons: true, buttons: ['保存'] },
  '/mall/product': { route: '/mall/product', title: '商品管理', menuText: '商品管理', parentMenu: '商城管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/mall/order': { route: '/mall/order', title: '订单管理', menuText: '订单管理', parentMenu: '商城管理', hasTable: true, hasForm: false, hasButtons: false },
  '/mall/user-audit': { route: '/mall/user-audit', title: '用户审核', menuText: '用户审核', parentMenu: '商城管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['审核'] },
  '/mall/banner': { route: '/mall/banner', title: '轮播图管理', menuText: '轮播图管理', parentMenu: '商城管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },

  // 打印
  '/printing/template': { route: '/printing/template', title: '打印模板', menuText: '打印模板', parentMenu: '打印管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/printing/chain': { route: '/printing/chain', title: '打印链路', menuText: '打印链路', parentMenu: '打印管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/printing/client': { route: '/printing/client', title: '打印客户端', menuText: '打印客户端', parentMenu: '打印管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },
  '/printing/task': { route: '/printing/task', title: '打印任务', menuText: '打印任务', parentMenu: '打印管理', hasTable: true, hasForm: false, hasButtons: true, buttons: ['新增'] },

  // 图表 + 订单中心
  '/charts': { route: '/charts', title: '图表', menuText: '图表', hasTable: false, hasForm: false, hasButtons: false },
  '/order-center': { route: '/order-center', title: '订单中心', menuText: '订单中心', hasTable: true, hasForm: false, hasButtons: false },
};

// 处理重复路由：销售出库和发货管理同时出现在销售和仓储下
// '/erp/shipment' is in 销售作业 (via 发货管理), use the one in 销售作业 (displayGroup=1)
// '/erp/stock-in' is in 采购作业 (入库管理)
// '/erp/return' is in 销售作业 (退货处理)
// '/erp/batch' is in 仓储作业 (批次管理)
// We set parentMenu for duplicates:
const DUPLICATE_ROUTES: Record<string, string | undefined> = {
  '/erp/shipment': undefined, // 销售作业 (displayGroup=1, top level)
  '/erp/stock-in': undefined, // 采购作业 (displayGroup=1, top level)
  '/erp/return': undefined,   // 销售作业 (displayGroup=1, top level)
  '/erp/batch': undefined,    // 仓储作业 (displayGroup=1, top level)
  '/erp/serial': undefined,   // 仓储作业 (displayGroup=1, top level)
  '/erp/stock': undefined,    // 仓储作业 (displayGroup=1, top level)
  '/erp/stock-transfer': undefined,
  '/erp/stocktake': undefined,
  '/erp/stock-cost-adjust': undefined,
  '/erp/stock-overflow': undefined,
  '/erp/stock-damage': undefined,
  '/erp/stock-alert-config': undefined,
  '/erp/stock/replenishment': undefined,
  '/erp/stock-bom': undefined,
  '/erp/stock-assemble': undefined,
  '/erp/stock-split': undefined,
};

// Apply parentMenu overrides for duplicates
for (const [route, parent] of Object.entries(DUPLICATE_ROUTES)) {
  if (ROUTE_PAGES[route]) {
    ROUTE_PAGES[route].parentMenu = parent;
  }
}

const ALL_PAGES = Object.values(ROUTE_PAGES);

// 排序：先按功能分组
const PAGE_ORDER = [
  '/dashboard',
  // 销售作业
  '/crm/quotation', '/sale', '/erp/sale', '/erp/sale-outbound', '/erp/shipment', '/erp/return', '/erp/sales-analysis', '/erp/sales-report',
  // 采购作业
  '/purchase', '/erp/purchase', '/erp/stock-in', '/erp/purchase-return', '/erp/purchase-exchange',
  // 仓储作业
  '/erp/stock', '/erp/stocktake', '/erp/stock-transfer', '/erp/stock-cost-adjust', '/erp/stock-overflow', '/erp/stock-damage', '/erp/batch', '/erp/serial', '/erp/stock-alert-config', '/erp/stock/replenishment',
  // 客户关系
  '/crm/lead', '/crm/opportunity', '/crm/customer', '/crm/contract', '/crm/invoice', '/supplier', '/supplier/inquiry', '/supplier/performance',
  // 财务管理
  '/finance/subject', '/finance/voucher', '/finance/receivable', '/finance/accounts-receivable', '/finance/accounts-receivable/aging-analysis', '/finance/payable', '/finance/accounts-payable', '/finance/receipt', '/finance/payment', '/finance/pre-receipt', '/finance/pre-payment', '/finance/deposit', '/finance/write-off', '/finance/offset', '/finance/capital-flow', '/finance/report', '/finance/reconciliation',
  // 费用管理
  '/erp/expense/application', '/erp/expense/reimbursement', '/erp/expense/approval', '/erp/expense/payment', '/erp/expense/statistics',
  // WMS
  '/wms/warehouse', '/wms/location', '/wms/receipt', '/wms/putaway', '/wms/pick', '/wms/wave', '/wms/ship', '/wms/inventory', '/wms/move', '/wms/check', '/wms/event',
  // DMS
  '/dms/dashboard', '/dms/channel', '/dms/rider', '/dms/vehicle', '/dms/route', '/dms/dispatch', '/dms/order-pool', '/dms/tracking', '/dms/verification', '/dms/config',
  // 产品数据
  '/erp/product', '/erp/partner', '/erp/pricing', '/erp/pricing/approval', '/erp/pricing/tiers',
  // 资产管理
  '/fixed-asset/asset', '/fixed-asset/category', '/fixed-asset/purchase', '/fixed-asset/depreciation', '/fixed-asset/transfer', '/fixed-asset/disposal', '/fixed-asset/inventory', '/fixed-asset/report',
  // 预算管理
  '/budget', '/budget/template', '/budget/annual', '/budget/adjustment', '/budget/report',
  // 商城
  '/mall/config', '/mall/product', '/mall/order', '/mall/user-audit', '/mall/banner',
  // 打印
  '/printing/template', '/printing/chain', '/printing/client', '/printing/task',
  // 订单中心 + 工作流
  '/order-center', '/workflow/instance-monitor', '/workflow/task-management', '/workflow/process-analysis',
  // 系统
  '/system/department', '/system/position', '/system/user', '/system/role', '/system/permission', '/system/menu', '/system/tenant', '/system/tenant-approval', '/system/dict', '/system/config', '/system/log', '/system/data-import', '/notification',
  // 图表
  '/charts',
];

const SORTED_PAGES = PAGE_ORDER.map(r => ROUTE_PAGES[r]).filter(Boolean);

// ==================== 辅助函数 ====================

async function login(page: Page): Promise<boolean> {
  try {
    console.log('[登录] 开始...');
    await loginViaUi(page, 'admin', 'admin123', '系统租户');
    console.log('[登录] 成功, URL:', page.url());
    return true;
  } catch (error: any) {
    console.error('[登录] 失败:', error.message);
    await page.screenshot({ path: 'test-results/login-failed.png' }).catch(() => {});
    return false;
  }
}

/** 通过点击侧边栏菜单导航 - 使用实际菜单文本 */
async function navigateByMenuClick(page: Page, pageDef: PageDef): Promise<boolean> {
  try {
    const menuText = pageDef.menuText;
    const parentText = pageDef.parentMenu;

    // 等待侧边栏渲染
    await page.waitForSelector('.ant-menu', { timeout: 10000 }).catch(() => {
      throw new Error('侧边栏菜单未找到');
    });

    // 如果有父级菜单，需要先展开
    if (parentText) {
      // 查找并展开父级菜单 (a-sub-menu)
      const subMenu = page.locator('.ant-menu-submenu-title', { hasText: parentText }).first();
      if (await subMenu.isVisible({ timeout: 3000 }).catch(() => false)) {
        // 检查是否已展开（submenu是否包含ant-menu-submenu-open class）
        const parentSubMenu = subMenu.locator('..').locator('..'); // Go up to .ant-menu-submenu
        const isOpen = await parentSubMenu.evaluate(el => el.classList.contains('ant-menu-submenu-open')).catch(() => false);
        if (!isOpen) {
          await subMenu.click();
          await page.waitForTimeout(500); // Wait for animation
        }
      } else {
        console.log(`  [菜单] 父级菜单 "${parentText}" 不可见（可能是 displayGroup）`);
      }
    }

    // 查找并点击目标菜单项
    // 优先查找 ant-menu-item（最精确）
    let menuItem = page.locator('.ant-menu-item', { hasText: menuText }).first();

    if (!(await menuItem.isVisible({ timeout: 2000 }).catch(() => false))) {
      // 也许在已展开的子菜单中
      menuItem = page.locator('.ant-menu-submenu .ant-menu-item', { hasText: menuText }).first();
    }

    if (await menuItem.isVisible({ timeout: 2000 }).catch(() => false)) {
      await menuItem.click();
      await page.waitForTimeout(2000);
      return true;
    }

    // 最后的尝试：使用 aria-label 或 role 查找
    console.log(`  [菜单] 无法找到 "${menuText}"，尝试使用URL导航`);
    return false;
  } catch (error: any) {
    console.log(`  [菜单] 导航失败: ${error.message}`);
    return false;
  }
}

/** 通过URL导航（备选方案） */
async function navigateByUrl(page: Page, route: string): Promise<void> {
  try {
    await page.goto(route, { waitUntil: 'load', timeout: 15000 });
    await page.waitForTimeout(2000);
  } catch (error: any) {
    console.log(`  [URL] 导航失败: ${error.message}`);
  }
}

/** 检查页面是否成功加载 */
async function checkPageLoaded(page: Page, pageDef: PageDef): Promise<boolean> {
  try {
    const currentUrl = page.url();
    const routePath = pageDef.route;
    if (!currentUrl.includes(routePath) && !currentUrl.includes(routePath.substring(1))) {
      addError('blocking', pageDef.route, pageDef.title, `路由不匹配: 期望 ${routePath}, 实际 ${currentUrl.substring(0, 80)}`);
      return false;
    }
    return true;
  } catch {
    return false;
  }
}

/** 检查表格 */
async function checkTable(page: Page, pageDef: PageDef): Promise<void> {
  if (!pageDef.hasTable) return;
  try {
    const hasVxeTable = await page.locator('.vxe-table').isVisible({ timeout: 1500 }).catch(() => false);
    const hasAntTable = await page.locator('.ant-table').isVisible({ timeout: 1500 }).catch(() => false);
    if (!hasVxeTable && !hasAntTable) {
      // 可能是动态加载的表格，等待一下再检查
      await page.waitForTimeout(2000);
      const hasVxeTable2 = await page.locator('.vxe-table').isVisible({ timeout: 1000 }).catch(() => false);
      const hasAntTable2 = await page.locator('.ant-table').isVisible({ timeout: 1000 }).catch(() => false);
      if (!hasVxeTable2 && !hasAntTable2) {
        addError('production-grade', pageDef.route, pageDef.title, '页面声明有表格但未检测到 vxe-table 或 ant-table');
      }
    }
  } catch { /* ignore */ }
}

/** 检查操作按钮可见性 */
async function checkButtons(page: Page, pageDef: PageDef): Promise<void> {
  if (!pageDef.buttons || pageDef.buttons.length === 0) return;
  for (const btnText of pageDef.buttons) {
    try {
      const btn = page.locator('button', { hasText: btnText }).or(page.locator('.ant-btn', { hasText: btnText })).first();
      const visible = await btn.isVisible({ timeout: 2000 }).catch(() => false);
      if (!visible) {
        addError('interaction', pageDef.route, pageDef.title, `按钮 "${btnText}" 不可见`);
      } else {
        console.log(`  [按钮] 找到 "${btnText}"`);
      }
    } catch {
      addError('interaction', pageDef.route, pageDef.title, `检查按钮 "${btnText}" 时出错`);
    }
  }
}

/** 生产级标准检查 */
async function checkProductionGrade(page: Page, pageDef: PageDef): Promise<void> {
  // 固定头部
  try {
    const header = page.locator('.layout-header, .ant-layout-header').first();
    const pos = await header.evaluate(el => getComputedStyle(el).position).catch(() => '');
    if (pos !== 'sticky' && pos !== 'fixed') {
      addError('production-grade', pageDef.route, pageDef.title, '页面头部未固定');
    }
  } catch { /* ignore */ }

  // 检查错误消息弹窗
  try {
    const errorMsg = page.locator('.ant-message-error').first();
    if (await errorMsg.isVisible({ timeout: 500 }).catch(() => false)) {
      const text = await errorMsg.textContent();
      if (text) addError('blocking', pageDef.route, pageDef.title, `错误提示: ${text.substring(0, 100)}`);
    }
  } catch { /* ignore */ }
}

/** 检查空状态 */
async function checkEmptyState(page: Page, pageDef: PageDef): Promise<void> {
  if (!pageDef.hasTable) return;
  try {
    const vxeRows = await page.locator('.vxe-body--row').count().catch(() => 0);
    const antRows = await page.locator('.ant-table-tbody tr.ant-table-row').count().catch(() => 0);
    if (vxeRows === 0 && antRows === 0) {
      const hasEmpty = await page.locator('.ant-empty, .vxe-empty--wrapper').isVisible({ timeout: 500 }).catch(() => false);
      if (!hasEmpty) {
        console.log(`  [提示] ${pageDef.route}: 无数据行且无空状态组件`);
      }
    }
  } catch { /* ignore */ }
}

/** 尝试点击页面上的按钮进行基本交互 */
async function tryClickButtons(page: Page, pageDef: PageDef): Promise<void> {
  if (!pageDef.buttons) return;
  for (const btnText of pageDef.buttons) {
    try {
      const btn = page.locator('button', { hasText: btnText }).or(page.locator('.ant-btn', { hasText: btnText })).first();
      if (await btn.isVisible({ timeout: 1000 }).catch(() => false)) {
        console.log(`  [交互] 点击 "${btnText}"`);
        await btn.click();
        await page.waitForTimeout(1000);

        // 检查是否弹出了对话框/抽屉/模态框
        const modal = page.locator('.ant-modal, .ant-drawer, .ant-modal-wrap').first();
        if (await modal.isVisible({ timeout: 2000 }).catch(() => false)) {
          console.log(`  [交互] 弹出窗口已打开`);
          // 关闭弹窗
          const closeBtn = modal.locator('.ant-modal-close, .ant-drawer-close, button.ant-btn:has-text("取消")').first();
          if (await closeBtn.isVisible({ timeout: 1000 }).catch(() => false)) {
            await closeBtn.click();
            await page.waitForTimeout(500);
          }
        }
      }
    } catch { /* ignore */ }
  }
}

// ==================== 主测试 ====================

test.describe('Phase 2 - 全页面点击交互测试', () => {
  test.describe.configure({ mode: 'serial' });

  let context: BrowserContext;
  let page: Page;

  const consoleErrors: Map<string, number> = new Map();

  test.beforeAll(async ({ browser }) => {
    context = await browser.newContext({ baseURL: BASE_URL, viewport: { width: 1920, height: 1080 } });
    page = await context.newPage();

    page.on('console', msg => {
      if (msg.type() === 'error') {
        const url = page.url();
        consoleErrors.set(url, (consoleErrors.get(url) || 0) + 1);
        console.error(`[控制台错误] ${msg.text().substring(0, 150)}`);
      }
    });

    page.on('response', response => {
      if (response.status() >= 400 && response.status() !== 401 && !response.url().includes('/captcha') && !response.url().includes('/favicon')) {
        console.log(`  [网络] ${response.status()} ${response.url().substring(0, 80)}`);
      }
    });

    const loginOk = await login(page);
    if (!loginOk) throw new Error('登录失败');
    await page.waitForTimeout(2000);
  });

  test.afterAll(async () => {
    const report = {
      timestamp: new Date().toISOString(),
      totalPages: ALL_PAGES.length,
      totalErrors: allErrors.length,
      errorsByType: {} as Record<string, number>,
      errors: allErrors
    };
    for (const err of allErrors) {
      report.errorsByType[err.type] = (report.errorsByType[err.type] || 0) + 1;
    }

    const reportPath = 'test-results/click-based-error-report.json';
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
    console.log(`\n===== 最终报告 =====`);
    console.log(`总页面: ${ALL_PAGES.length}`);
    console.log(`总错误: ${allErrors.length}`);
    console.log(`错误分类: ${JSON.stringify(report.errorsByType)}`);
    console.log(`报告文件: ${reportPath}`);
    await context.close();
  });

  // 分批测试
  const BATCH_SIZE = 10;
  const batches: PageDef[][] = [];
  for (let i = 0; i < SORTED_PAGES.length; i += BATCH_SIZE) {
    batches.push(SORTED_PAGES.slice(i, i + BATCH_SIZE));
  }

  batches.forEach((batch, batchIndex) => {
    const startId = batchIndex * BATCH_SIZE + 1;
    const endId = Math.min(startId + BATCH_SIZE - 1, SORTED_PAGES.length);

    test(`Batch ${batchIndex + 1} (${startId}-${endId}, ${batch.length}页)`, async () => {
      test.setTimeout(300_000);

      console.log(`\n========== Batch ${batchIndex + 1} ==========`);

      for (const pageDef of batch) {
        console.log(`\n[${pageDef.route}] ${pageDef.title}`);

        // 1. 尝试菜单点击导航
        const menuOk = await navigateByMenuClick(page, pageDef);
        if (!menuOk) {
          // Fallback to URL navigation
          await navigateByUrl(page, pageDef.route);
        }

        // 2. 验证页面
        await checkPageLoaded(page, pageDef);
        await checkTable(page, pageDef);
        await checkButtons(page, pageDef);
        await checkProductionGrade(page, pageDef);
        await checkEmptyState(page, pageDef);

        // 3. 截图
        const safeName = pageDef.route.replace(/\//g, '_').replace(/^_/, '');
        await page.screenshot({ path: `test-results/screenshots/p2_${safeName}.png`, fullPage: true }).catch(() => {});

        // 4. 尝试点击按钮进行交互
        await tryClickButtons(page, pageDef);

        console.log(`  [完成]`);
      }

      console.log(`\n========== Batch ${batchIndex + 1} 完成, 累计错误: ${allErrors.length} ==========`);
    });
  });
});

export { SORTED_PAGES, allErrors };
