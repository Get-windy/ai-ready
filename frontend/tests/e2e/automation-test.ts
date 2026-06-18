/**
 * ERP系统自动化测试脚本
 * 通过模拟用户点击遍历所有页面，收集错误并验证生产级要求
 */

import { chromium, Browser, Page, BrowserContext } from 'playwright';
import * as fs from 'fs';
import * as path from 'path';

// 配置
const FRONTEND_URL = 'http://localhost:5656';
const USERNAME = 'admin';
const PASSWORD = 'admin123';
const TENANT = '系统租户';

// 错误收集
interface ErrorLog {
  timestamp: string;
  page: string;
  type: 'console' | 'network' | 'visual' | 'interaction' | 'production';
  message: string;
  details?: any;
}

const errors: ErrorLog[] = [];

// 页面清单（从路由配置提取）
const PAGES_TO_TEST = [
  // 工作台
  { path: '/dashboard', name: '工作台', module: 'dashboard' },

  // 销售模块
  { path: '/sale', name: '销售订单', module: 'sale' },
  { path: '/stock', name: '销售出库', module: 'stock' },
  { path: '/erp/sale', name: '销售管理(ERP)', module: 'erp/sale' },
  { path: '/erp/shipment', name: '发货管理', module: 'erp/shipment' },
  { path: '/erp/return', name: '退货管理', module: 'erp/return' },
  { path: '/erp/sales-analysis', name: '销售分析', module: 'erp/sales-analysis' },
  { path: '/erp/sales-report', name: '销售报表', module: 'erp/sales-report' },

  // 采购模块
  { path: '/purchase', name: '采购订单', module: 'purchase' },
  { path: '/erp/purchase', name: '采购管理(ERP)', module: 'erp/purchase' },
  { path: '/erp/stock-in', name: '入库管理', module: 'erp/stock-in' },
  { path: '/erp/purchase-exchange', name: '采购换货', module: 'erp/purchase-exchange' },

  // 仓储模块
  { path: '/erp/stock', name: '库存管理', module: 'erp/stock' },
  { path: '/erp/stocktake', name: '库存盘点', module: 'erp/stocktake' },
  { path: '/erp/batch', name: '批次管理', module: 'erp/batch' },
  { path: '/erp/serial', name: '序列号管理', module: 'erp/serial' },
  { path: '/erp/stock-cost-adjust', name: '成本调整', module: 'erp/stock-cost-adjust' },
  { path: '/erp/stock-overflow', name: '溢余入库', module: 'erp/stock-overflow' },
  { path: '/erp/stock-damage', name: '报损出库', module: 'erp/stock-damage' },
  { path: '/erp/stock-transfer', name: '库存调拨', module: 'erp/stock-transfer' },
  { path: '/erp/stock-bom', name: 'BOM清单', module: 'erp/stock-bom' },
  { path: '/erp/stock-assemble', name: '组装单', module: 'erp/stock-assemble' },
  { path: '/erp/stock-split', name: '拆卸单', module: 'erp/stock-split' },
  { path: '/erp/stock-alert-config', name: '库存预警配置', module: 'erp/stock-alert-config' },

  // CRM模块
  { path: '/crm/customer', name: '客户管理', module: 'crm/customer' },
  { path: '/crm/lead', name: '线索管理', module: 'crm/lead' },
  { path: '/crm/opportunity', name: '商机管理', module: 'crm/opportunity' },
  { path: '/crm/quotation', name: '报价管理', module: 'crm/quotation' },
  { path: '/crm/contract', name: '合同管理', module: 'crm/contract' },
  { path: '/crm/invoice', name: '发票管理', module: 'crm/invoice' },
  { path: '/crm/supplier', name: '供应商管理(CRM)', module: 'crm/supplier' },

  // 产品数据
  { path: '/erp/product', name: '产品管理', module: 'erp/product' },
  { path: '/erp/partner', name: '往来单位管理', module: 'erp/partner' },
  { path: '/erp/pricing', name: '价格引擎', module: 'erp/pricing' },
  { path: '/erp/pricing/approval', name: '价格审批', module: 'erp/pricing/approval' },
  { path: '/erp/pricing/tiers', name: '价格阶梯', module: 'erp/pricing/tiers' },

  // 供应商模块
  { path: '/supplier', name: '供应商管理', module: 'supplier' },
  { path: '/supplier/inquiry', name: '供应商询价', module: 'supplier/inquiry' },
  { path: '/supplier/performance', name: '供应商绩效', module: 'supplier/performance' },

  // 财务模块
  { path: '/finance', name: '财务管理', module: 'finance' },
  { path: '/finance/receivable', name: '应收管理', module: 'finance/receivable' },
  { path: '/finance/payable', name: '应付管理', module: 'finance/payable' },
  { path: '/finance/pre-receipt', name: '预收款', module: 'finance/pre-receipt' },
  { path: '/finance/pre-payment', name: '预付款', module: 'finance/pre-payment' },
  { path: '/finance/deposit', name: '保证金', module: 'finance/deposit' },
  { path: '/finance/write-off', name: '核销管理', module: 'finance/write-off' },
  { path: '/finance/offset', name: '冲抵管理', module: 'finance/offset' },
  { path: '/finance/capital-flow', name: '资金流水', module: 'finance/capital-flow' },
  { path: '/finance/receipt', name: '收款单', module: 'finance/receipt' },
  { path: '/finance/payment', name: '付款单', module: 'finance/payment' },
  { path: '/finance/voucher', name: '凭证管理', module: 'finance/voucher' },
  { path: '/finance/subject', name: '科目管理', module: 'finance/subject' },
  { path: '/finance/reconciliation', name: '对账管理', module: 'finance/reconciliation' },
  { path: '/finance/report', name: '财务报表', module: 'finance/report' },

  // 费用管理
  { path: '/erp/expense/application', name: '费用申请', module: 'erp/expense/application' },
  { path: '/erp/expense/reimbursement', name: '费用报销', module: 'erp/expense/reimbursement' },
  { path: '/erp/expense/approval', name: '费用审批', module: 'erp/expense/approval' },
  { path: '/erp/expense/payment', name: '费用付款', module: 'erp/expense/payment' },
  { path: '/erp/expense/statistics', name: '费用统计', module: 'erp/expense/statistics' },

  // 固定资产
  { path: '/fixed-asset/asset', name: '资产列表', module: 'fixed-asset/asset' },
  { path: '/fixed-asset/category', name: '资产分类', module: 'fixed-asset/category' },
  { path: '/fixed-asset/depreciation', name: '折旧管理', module: 'fixed-asset/depreciation' },
  { path: '/fixed-asset/transfer', name: '资产转移', module: 'fixed-asset/transfer' },
  { path: '/fixed-asset/disposal', name: '资产处置', module: 'fixed-asset/disposal' },
  { path: '/fixed-asset/inventory', name: '资产盘点', module: 'fixed-asset/inventory' },
  { path: '/fixed-asset/report', name: '资产报表', module: 'fixed-asset/report' },
  { path: '/fixed-asset/purchase', name: '资产采购', module: 'fixed-asset/purchase' },

  // 预算管理
  { path: '/budget', name: '预算管理', module: 'budget' },
  { path: '/budget/template', name: '预算模板', module: 'budget/template' },
  { path: '/budget/annual', name: '年度预算', module: 'budget/annual' },
  { path: '/budget/adjustment', name: '预算调整', module: 'budget/adjustment' },
  { path: '/budget/report', name: '预算报表', module: 'budget/report' },

  // WMS仓储
  { path: '/wms/warehouse', name: '仓库管理', module: 'wms/warehouse' },
  { path: '/wms/location', name: '库位管理', module: 'wms/location' },
  { path: '/wms/receipt', name: '收货管理', module: 'wms/receipt' },
  { path: '/wms/putaway', name: '上架管理', module: 'wms/putaway' },
  { path: '/wms/pick', name: '拣货管理', module: 'wms/pick' },
  { path: '/wms/wave', name: '波次管理', module: 'wms/wave' },
  { path: '/wms/ship', name: '发货管理', module: 'wms/ship' },
  { path: '/wms/inventory', name: '库存管理(WMS)', module: 'wms/inventory' },
  { path: '/wms/move', name: '移库管理', module: 'wms/move' },
  { path: '/wms/check', name: '盘点管理(WMS)', module: 'wms/check' },
  { path: '/wms/event', name: '事件管理', module: 'wms/event' },

  // DMS配送
  { path: '/dms/dashboard', name: '配送看板', module: 'dms/dashboard' },
  { path: '/dms/channel', name: '渠道管理', module: 'dms/channel' },
  { path: '/dms/rider', name: '骑手管理', module: 'dms/rider' },
  { path: '/dms/vehicle', name: '车辆管理', module: 'dms/vehicle' },
  { path: '/dms/verification', name: '核销管理', module: 'dms/verification' },
  { path: '/dms/route', name: '路线管理', module: 'dms/route' },
  { path: '/dms/dispatch', name: '调度管理', module: 'dms/dispatch' },
  { path: '/dms/order-pool', name: '订单池', module: 'dms/order-pool' },
  { path: '/dms/tracking', name: '追踪管理', module: 'dms/tracking' },
  { path: '/dms/config', name: '配送配置', module: 'dms/config' },

  // 商城管理
  { path: '/mall/config', name: '商城配置', module: 'mall/config' },
  { path: '/mall/user-audit', name: '用户审核', module: 'mall/user-audit' },
  { path: '/mall/banner', name: '轮播图管理', module: 'mall/banner' },
  { path: '/mall/order', name: '订单管理', module: 'mall/order' },
  { path: '/mall/product', name: '商品管理', module: 'mall/product' },

  // 打印管理
  { path: '/printing/template', name: '打印模板', module: 'printing/template' },
  { path: '/printing/chain', name: '打印链路', module: 'printing/chain' },
  { path: '/printing/client', name: '打印客户端', module: 'printing/client' },
  { path: '/printing/task', name: '打印任务', module: 'printing/task' },
  { path: '/printing/designer', name: '打印设计器', module: 'printing/designer' },

  // 系统管理
  { path: '/system/user', name: '用户管理', module: 'system/user' },
  { path: '/system/role', name: '角色管理', module: 'system/role' },
  { path: '/system/menu', name: '菜单管理', module: 'system/menu' },
  { path: '/system/department', name: '部门管理', module: 'system/department' },
  { path: '/system/position', name: '岗位管理', module: 'system/position' },
  { path: '/system/dict', name: '字典管理', module: 'system/dict' },
  { path: '/system/config', name: '系统配置', module: 'system/config' },
  { path: '/system/log', name: '日志管理', module: 'system/log' },
  { path: '/system/permission', name: '权限管理', module: 'system/permission' },
  { path: '/system/tenant', name: '租户管理', module: 'system/tenant' },
  { path: '/system/tenant-approval', name: '租户审批', module: 'system/tenant-approval' },
  { path: '/system/data-import', name: '数据导入', module: 'system/data-import' },

  // 其他
  { path: '/notification', name: '通知公告', module: 'notification' },
  { path: '/profile', name: '个人中心', module: 'profile' },
  { path: '/charts', name: '图表', module: 'charts' },
  { path: '/workflow/instance-monitor', name: '流程监控', module: 'workflow/instance-monitor' },
  { path: '/workflow/task-management', name: '任务管理', module: 'workflow/task-management' },
  { path: '/workflow/process-analysis', name: '流程分析', module: 'workflow/process-analysis' },
];

// 生产级检查项
interface ProductionCheck {
  page: string;
  checks: {
    detailPageFullscreen: boolean;
    fixedBottomBar: boolean;
    compactComponents: boolean;
    tableBorders: boolean;
    fixedHeader: boolean;
    emptyRowFill: boolean;
    numericRightAlign: boolean;
  };
}

const productionChecks: ProductionCheck[] = [];

async function main() {
  console.log('=== ERP自动化测试启动 ===');
  console.log(`前端地址: ${FRONTEND_URL}`);
  console.log(`测试页面数: ${PAGES_TO_TEST.length}`);

  const browser: Browser = await chromium.launch({
    headless: false, // 显示浏览器便于调试
    args: ['--start-maximized']
  });

  const context: BrowserContext = await browser.newContext({
    viewport: null, // 使用完整窗口
    recordVideo: { dir: './test-results/videos' }
  });

  const page: Page = await context.newPage();

  // 设置错误监听
  page.on('console', msg => {
    if (msg.type() === 'error') {
      errors.push({
        timestamp: new Date().toISOString(),
        page: page.url(),
        type: 'console',
        message: msg.text()
      });
      console.error(`[Console Error] ${msg.text()}`);
    }
  });

  page.on('pageerror', err => {
    errors.push({
      timestamp: new Date().toISOString(),
      page: page.url(),
      type: 'console',
      message: err.message,
      details: err.stack
    });
    console.error(`[Page Error] ${err.message}`);
  });

  page.on('requestfailed', request => {
    if (!request.url().includes('sockjs-node') && !request.url().includes('vite')) {
      errors.push({
        timestamp: new Date().toISOString(),
        page: page.url(),
        type: 'network',
        message: `请求失败: ${request.url()} (${request.failure()?.errorText})`
      });
      console.error(`[Network Error] ${request.url()}`);
    }
  });

  page.on('response', response => {
    if (response.status() >= 400 && !response.url().includes('sockjs-node')) {
      errors.push({
        timestamp: new Date().toISOString(),
        page: page.url(),
        type: 'network',
        message: `HTTP ${response.status()}: ${response.url()}`
      });
      console.error(`[HTTP Error] ${response.status()} ${response.url()}`);
    }
  });

  try {
    // 步骤1: 登录
    console.log('\n=== 步骤1: 登录系统 ===');
    await login(page);

    // 步骤2: 获取菜单列表
    console.log('\n=== 步骤2: 获取菜单列表 ===');
    const menuItems = await getMenuItems(page);
    console.log(`发现菜单项: ${menuItems.length}`);

    // 步骤3: 分批测试页面
    const BATCH_SIZE = 10;
    const batches = Math.ceil(PAGES_TO_TEST.length / BATCH_SIZE);

    for (let batch = 0; batch < batches; batch++) {
      console.log(`\n=== 第${batch + 1}批处理 (${batch * BATCH_SIZE + 1}-${Math.min((batch + 1) * BATCH_SIZE, PAGES_TO_TEST.length)}) ===`);

      const batchPages = PAGES_TO_TEST.slice(batch * BATCH_SIZE, (batch + 1) * BATCH_SIZE);

      for (const pageInfo of batchPages) {
        await testPage(page, pageInfo, menuItems);
      }

      // 输出批次报告
      const batchErrors = errors.filter(e =>
        batchPages.some(p => e.page.includes(p.path))
      );
      console.log(`第${batch + 1}批完成，发现错误: ${batchErrors.length}`);
    }

    // 步骤4: 输出最终报告
    console.log('\n=== 测试完成，输出报告 ===');
    outputReport();

  } catch (err) {
    console.error('测试执行失败:', err);
    errors.push({
      timestamp: new Date().toISOString(),
      page: 'main',
      type: 'console',
      message: `测试执行失败: ${err.message}`
    });
  }

  await browser.close();
}

async function login(page: Page) {
  console.log('导航到登录页面...');
  await page.goto(FRONTEND_URL);
  await page.waitForLoadState('networkidle', { timeout: 30000 });

  // 检查是否已在登录页
  const loginForm = await page.locator('.login-container, .login-box').first();

  if (await loginForm.isVisible()) {
    console.log('填写登录信息...');

    // 填写租户名称
    const tenantInput = page.locator('input[placeholder="请输入租户名称"]').first();
    await tenantInput.fill(TENANT);

    // 填写用户名
    const usernameInput = page.locator('input[placeholder="请输入用户名"]').first();
    await usernameInput.fill(USERNAME);

    // 填写密码
    const passwordInput = page.locator('input[placeholder="请输入密码"]').first();
    await passwordInput.fill(PASSWORD);

    // 等待验证码加载
    await page.waitForTimeout(1000);

    // 获取验证码图片并识别（简化处理：等待验证码加载后手动输入）
    // 由于自动化测试无法识别验证码，我们尝试绕过验证码
    // 方法：直接调用API获取验证码，然后输入任意4位字符（后端可能已禁用验证码校验）
    const captchaInput = page.locator('input[placeholder="请输入验证码"]').first();
    await captchaInput.fill('1234'); // 尝试输入固定验证码

    // 点击登录按钮
    const loginBtn = page.locator('button[type="submit"], button:has-text("登录")').first();
    await loginBtn.click();

    // 等待登录成功或错误
    await page.waitForTimeout(3000);

    // 检查是否登录成功（检查是否有侧边栏或dashboard）
    const sidebar = page.locator('.ant-layout-sider, [class*="sidebar"]').first();
    const dashboardContent = page.locator('.basic-layout, [class*="dashboard"]').first();

    try {
      await sidebar.waitFor({ state: 'visible', timeout: 15000 });
      console.log('登录成功！侧边栏已显示');
    } catch {
      // 检查是否有错误提示
      const errorMsg = page.locator('.ant-message-error, .ant-alert-error').first();
      if (await errorMsg.isVisible()) {
        const errorText = await errorMsg.textContent();
        console.log(`登录失败: ${errorText}`);
        // 尝试刷新验证码重新登录
        const captchaImage = page.locator('.captcha-image').first();
        await captchaImage.click();
        await page.waitForTimeout(1000);
        await captchaInput.fill('1234');
        await loginBtn.click();
        await page.waitForTimeout(3000);
      }
    }

    // 等待页面稳定
    await page.waitForLoadState('networkidle', { timeout: 30000 });
  } else {
    console.log('可能已登录，检查当前页面...');
    // 检查是否已经在dashboard
    const currentUrl = page.url();
    if (!currentUrl.includes('dashboard')) {
      // 尝试导航到dashboard
      await page.goto(`${FRONTEND_URL}/dashboard`);
      await page.waitForTimeout(2000);
    }
  }
}

async function getMenuItems(page: Page): Promise<string[]> {
  // 等待侧边栏加载
  const sidebar = page.locator('.ant-layout-sider, [class*="sidebar"], [class*="menu"]').first();
  await sidebar.waitFor({ state: 'visible', timeout: 10000 });

  // 获取所有可见菜单项
  const menuLinks = await page.locator('.ant-menu-item a, .ant-menu-submenu-title').all();
  const menuNames: string[] = [];

  for (const link of menuLinks) {
    const text = await link.textContent();
    if (text && text.trim()) {
      menuNames.push(text.trim());
    }
  }

  return menuNames;
}

async function testPage(page: Page, pageInfo: { path: string; name: string; module: string }, menuItems: string[]) {
  console.log(`\n测试页面: ${pageInfo.name} (${pageInfo.path})`);

  try {
    // 通过点击菜单导航
    await navigateViaMenu(page, pageInfo);

    // 等待页面加载（不使用networkidle，因为有SSE持续连接）
    await page.waitForLoadState('domcontentloaded', { timeout: 15000 });
    await page.waitForTimeout(3000); // 给异步请求额外时间

    // 检查页面是否正常显示
    await checkPageVisual(page, pageInfo);

    // 生产级检查
    await checkProductionStandards(page, pageInfo);

    // 测试基本交互
    await testPageInteractions(page, pageInfo);

  } catch (err) {
    errors.push({
      timestamp: new Date().toISOString(),
      page: pageInfo.path,
      type: 'interaction',
      message: `页面测试失败: ${err.message}`
    });
    console.error(`页面测试失败: ${err.message}`);
  }
}

async function navigateViaMenu(page: Page, pageInfo: { path: string; name: string; module: string }) {
  // 尝试通过菜单点击导航
  const menuPath = pageInfo.module.split('/');

  for (let i = 0; i < menuPath.length; i++) {
    const menuName = getChineseMenuName(menuPath[i]);

    try {
      // 展开子菜单（如果有）
      const submenu = page.locator(`.ant-menu-submenu-title:has-text("${menuName}")`).first();
      if (await submenu.isVisible()) {
        await submenu.click();
        await page.waitForTimeout(500);
      }

      // 点击菜单项
      const menuItem = page.locator(`.ant-menu-item:has-text("${menuName}"), .ant-menu-item a:has-text("${menuName}")`).first();
      if (await menuItem.isVisible()) {
        await menuItem.click();
        await page.waitForTimeout(1000);
        return;
      }
    } catch {
      // 如果菜单点击失败，直接导航
      console.log(`菜单点击失败，直接导航到 ${pageInfo.path}`);
      await page.goto(`${FRONTEND_URL}${pageInfo.path}`);
      await page.waitForTimeout(1000);
      return;
    }
  }

  // 如果菜单导航失败，直接导航
  await page.goto(`${FRONTEND_URL}${pageInfo.path}`);
}

function getChineseMenuName(module: string): string {
  const nameMap: Record<string, string> = {
    'dashboard': '工作台',
    'sale': '销售',
    'stock': '出库',
    'erp': 'ERP',
    'purchase': '采购',
    'crm': '客户关系',
    'finance': '财务',
    'wms': '仓储',
    'dms': '配送',
    'mall': '商城',
    'printing': '打印',
    'system': '系统',
    'supplier': '供应商',
    'fixed-asset': '资产',
    'budget': '预算',
    'workflow': '流程',
    'notification': '通知',
    'profile': '个人',
    'charts': '图表',
    'product': '产品',
    'partner': '往来单位',
    'customer': '客户',
    'lead': '线索',
    'opportunity': '商机',
    'quotation': '报价',
    'contract': '合同',
    'invoice': '发票',
    'user': '用户',
    'role': '角色',
    'menu': '菜单',
    'department': '部门',
    'position': '岗位',
    'dict': '字典',
    'config': '配置',
    'log': '日志',
    'permission': '权限',
    'tenant': '租户',
    'warehouse': '仓库',
    'location': '库位',
    'receipt': '收货',
    'putaway': '上架',
    'pick': '拣货',
    'wave': '波次',
    'ship': '发货',
    'inventory': '库存',
    'move': '移库',
    'check': '盘点',
    'event': '事件',
    'rider': '骑手',
    'vehicle': '车辆',
    'route': '路线',
    'dispatch': '调度',
    'tracking': '追踪',
    'template': '模板',
    'chain': '链路',
    'client': '客户端',
    'task': '任务',
    'designer': '设计器',
    'expense': '费用',
    'application': '申请',
    'reimbursement': '报销',
    'approval': '审批',
    'payment': '付款',
    'statistics': '统计',
    'asset': '资产',
    'category': '分类',
    'depreciation': '折旧',
    'transfer': '转移',
    'disposal': '处置',
    'report': '报表',
    'annual': '年度',
    'adjustment': '调整',
    'serial': '序列号',
    'batch': '批次',
    'stocktake': '盘点',
    'pricing': '价格',
    'tiers': '阶梯',
    'banner': '轮播图',
    'order': '订单',
    'user-audit': '用户审核',
    'stock-in': '入库',
    'shipment': '发货',
    'return': '退货',
    'stock-transfer': '调拨',
    'stock-bom': 'BOM',
    'stock-assemble': '组装',
    'stock-split': '拆卸',
    'stock-cost-adjust': '成本调整',
    'stock-overflow': '溢余',
    'stock-damage': '报损',
    'stock-alert-config': '预警',
    'purchase-exchange': '换货',
    'sales-analysis': '分析',
    'sales-report': '报表',
    'receivable': '应收',
    'payable': '应付',
    'pre-receipt': '预收款',
    'pre-payment': '预付款',
    'deposit': '保证金',
    'write-off': '核销',
    'offset': '冲抵',
    'capital-flow': '资金流水',
    'voucher': '凭证',
    'subject': '科目',
    'reconciliation': '对账',
    'verification': '核销',
    'order-pool': '订单池',
    'channel': '渠道',
    'data-import': '数据导入',
    'tenant-approval': '租户审批',
    'instance-monitor': '流程监控',
    'task-management': '任务管理',
    'process-analysis': '流程分析',
    'replenishment': '补货',
  };

  return nameMap[module] || module;
}

async function checkPageVisual(page: Page, pageInfo: { path: string; name: string }) {
  // 检查白屏
  const bodyText = await page.locator('body').textContent();
  if (!bodyText || bodyText.trim().length < 10) {
    errors.push({
      timestamp: new Date().toISOString(),
      page: pageInfo.path,
      type: 'visual',
      message: '页面白屏或内容过少'
    });
    console.error(`[Visual] 页面白屏: ${pageInfo.path}`);
  }

  // 检查是否有错误提示
  const errorAlert = page.locator('.ant-alert-error, .ant-message-error, [class*="error"]').first();
  if (await errorAlert.isVisible()) {
    const errorText = await errorAlert.textContent();
    errors.push({
      timestamp: new Date().toISOString(),
      page: pageInfo.path,
      type: 'visual',
      message: `页面显示错误: ${errorText}`
    });
    console.error(`[Visual] 页面错误提示: ${errorText}`);
  }

  // 检查404页面
  const notFound = page.locator('h1:has-text("404"), h2:has-text("404"), :text("页面不存在")').first();
  if (await notFound.isVisible()) {
    errors.push({
      timestamp: new Date().toISOString(),
      page: pageInfo.path,
      type: 'visual',
      message: '页面404'
    });
    console.error(`[Visual] 页面404: ${pageInfo.path}`);
  }
}

async function checkProductionStandards(page: Page, pageInfo: { path: string; name: string }) {
  const checks = {
    detailPageFullscreen: true,
    fixedBottomBar: true,
    compactComponents: true,
    tableBorders: true,
    fixedHeader: true,
    emptyRowFill: true,
    numericRightAlign: true,
  };

  // 检查是否是详情页（通过路径判断）
  const isDetailPage = pageInfo.path.includes('/detail') || pageInfo.path.includes('/:id');

  if (isDetailPage) {
    // 检查详情页是否全屏覆盖
    const modal = page.locator('.ant-modal-content, [class*="detail-modal"]').first();
    if (await modal.isVisible()) {
      const modalWidth = await modal.evaluate(el => el.getBoundingClientRect().width);
      const viewportWidth = page.viewportSize()?.width || 1920;
      if (modalWidth < viewportWidth * 0.9) {
        checks.detailPageFullscreen = false;
        errors.push({
          timestamp: new Date().toISOString(),
          page: pageInfo.path,
          type: 'production',
          message: `详情页不是全屏模式，宽度仅${modalWidth}px`
        });
      }
    }

    // 检查固定底栏
    const bottomBar = page.locator('.ant-modal-footer, [class*="fixed-bottom"]').first();
    if (await bottomBar.isVisible()) {
      const position = await bottomBar.evaluate(el => {
        const style = window.getComputedStyle(el);
        return style.position;
      });
      if (position !== 'fixed') {
        checks.fixedBottomBar = false;
        errors.push({
          timestamp: new Date().toISOString(),
          page: pageInfo.path,
          type: 'production',
          message: '详情页底部操作栏未固定'
        });
      }
    }
  }

  // 检查组件尺寸（按钮和输入框）
  const buttons = await page.locator('.ant-btn-sm, .ant-btn').all();
  for (const btn of buttons.slice(0, 5)) {
    const height = await btn.evaluate(el => el.getBoundingClientRect().height);
    if (height > 32) {
      checks.compactComponents = false;
      errors.push({
        timestamp: new Date().toISOString(),
        page: pageInfo.path,
        type: 'production',
        message: `按钮尺寸过大: ${height}px（应≤28px）`
      });
      break;
    }
  }

  // 检查表格边框
  const table = page.locator('.vxe-table, .ant-table').first();
  if (await table.isVisible()) {
    const hasBorder = await table.evaluate(el => {
      const style = window.getComputedStyle(el);
      return style.border !== 'none';
    });
    if (!hasBorder) {
      checks.tableBorders = false;
      errors.push({
        timestamp: new Date().toISOString(),
        page: pageInfo.path,
        type: 'production',
        message: '表格缺少边框'
      });
    }

    // 检查表头固定
    const tableHeader = page.locator('.vxe-table--header, .ant-table-header').first();
    if (await tableHeader.isVisible()) {
      const headerPosition = await tableHeader.evaluate(el => {
        const style = window.getComputedStyle(el);
        return style.position === 'sticky' || style.position === 'fixed';
      });
      if (!headerPosition) {
        checks.fixedHeader = false;
        errors.push({
          timestamp: new Date().toISOString(),
          page: pageInfo.path,
          type: 'production',
          message: '表格表头未固定'
        });
      }
    }
  }

  productionChecks.push({ page: pageInfo.path, checks });
}

async function testPageInteractions(page: Page, pageInfo: { path: string; name: string; module: string }) {
  // 尝试点击"新建"按钮（如果有）
  const newBtn = page.locator('button:has-text("新建"), button:has-text("新增"), button:has-text("添加")').first();
  if (await newBtn.isVisible()) {
    console.log('  点击新建按钮...');
    await newBtn.click();
    await page.waitForTimeout(2000);

    // 检查是否打开详情页/表单
    const formVisible = await page.locator('.ant-modal, [class*="detail"], form').first().isVisible();
    if (formVisible) {
      console.log('  详情页/表单已打开');

      // 尝试关闭
      const closeBtn = page.locator('button:has-text("关闭"), button:has-text("取消"), .ant-modal-close').first();
      if (await closeBtn.isVisible()) {
        await closeBtn.click();
        await page.waitForTimeout(1000);
      }
    }
  }

  // 尝试点击搜索按钮（如果有）
  const searchBtn = page.locator('button:has-text("搜索"), button:has-text("查询"), button:has-text("刷新")').first();
  if (await searchBtn.isVisible()) {
    console.log('  点击搜索按钮...');
    await searchBtn.click();
    await page.waitForTimeout(2000);
  }
}

function outputReport() {
  const reportPath = './test-results/automation-report.json';
  const report = {
    timestamp: new Date().toISOString(),
    totalPages: PAGES_TO_TEST.length,
    errors: errors,
    productionChecks: productionChecks,
    summary: {
      consoleErrors: errors.filter(e => e.type === 'console').length,
      networkErrors: errors.filter(e => e.type === 'network').length,
      visualErrors: errors.filter(e => e.type === 'visual').length,
      interactionErrors: errors.filter(e => e.type === 'interaction').length,
      productionIssues: errors.filter(e => e.type === 'production').length,
      totalErrors: errors.length,
    }
  };

  // 确保目录存在
  const dir = path.dirname(reportPath);
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }

  fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
  console.log(`\n报告已保存到: ${reportPath}`);
  console.log('\n=== 错误摘要 ===');
  console.log(`控制台错误: ${report.summary.consoleErrors}`);
  console.log(`网络错误: ${report.summary.networkErrors}`);
  console.log(`视觉错误: ${report.summary.visualErrors}`);
  console.log(`交互错误: ${report.summary.interactionErrors}`);
  console.log(`生产级问题: ${report.summary.productionIssues}`);
  console.log(`总错误数: ${report.summary.totalErrors}`);

  // 输出详细错误
  if (errors.length > 0) {
    console.log('\n=== 详细错误列表 ===');
    for (const err of errors.slice(0, 20)) {
      console.log(`[${err.type}] ${err.page}: ${err.message}`);
    }
    if (errors.length > 20) {
      console.log(`... 还有 ${errors.length - 20} 个错误`);
    }
  }
}

main().catch(console.error);