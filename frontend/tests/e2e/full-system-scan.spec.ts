/**
 * ERP系统自动化测试脚本
 * 目的：扫描所有页面，收集错误并修复
 */

import { test, Page, BrowserContext } from '@playwright/test';
import { loginViaUi } from './fixtures/auth.fixture';
import * as fs from 'fs';

// 测试配置
const LOGIN_CREDENTIALS = {
  tenantName: '系统租户',
  username: 'admin',
  password: 'admin123'
};

// 错误收集器
interface ErrorLog {
  type: 'console' | 'network' | 'visual' | 'backend';
  page: string;
  message: string;
  timestamp: string;
  stack?: string;
  url?: string;
  status?: number;
}

const errorLogs: ErrorLog[] = [];

// 页面清单（从 dynamicRoutes.ts 提取）
const PAGE_LIST = [
  // 工作台
  '/dashboard',

  // 销售模块
  '/sale',
  '/erp/sale',
  '/stock',
  '/erp/stock',
  '/erp/sales-analysis',
  '/erp/sales-report',
  '/erp/shipment',
  '/erp/return',

  // 采购模块
  '/purchase',
  '/erp/purchase',
  '/erp/stock-in',
  '/erp/purchase-exchange',

  // 库存模块
  '/erp/stocktake',
  '/erp/batch',
  '/erp/serial',
  '/erp/stock-cost-adjust',
  '/erp/stock-overflow',
  '/erp/stock-damage',
  '/erp/stock-transfer',
  '/erp/stock-replenishment',
  '/erp/stock-alert-config',
  '/erp/stock-bom',
  '/erp/stock-assemble',
  '/erp/stock-split',

  // CRM模块
  '/crm/customer',
  '/crm/lead',
  '/crm/opportunity',
  '/crm/quotation',
  '/crm/contract',
  '/crm/invoice',
  '/crm/supplier',
  '/erp/partner',

  // 财务模块
  '/finance',
  '/finance/receivable',
  '/finance/payable',
  '/finance/accounts-receivable',
  '/finance/accounts-payable',
  '/finance/pre-receipt',
  '/finance/pre-payment',
  '/finance/deposit',
  '/finance/write-off',
  '/finance/offset',
  '/finance/capital-flow',
  '/finance/receipt',
  '/finance/payment',
  '/finance/report',
  '/finance/subject',
  '/finance/voucher',
  '/finance/reconciliation',
  '/finance/reports',

  // 费用模块
  '/erp/expense/application',
  '/erp/expense/reimbursement',
  '/erp/expense/approval',
  '/erp/expense/payment',
  '/erp/expense/statistics',

  // 固定资产
  '/fixed-asset',
  '/fixed-asset/asset',
  '/fixed-asset/category',
  '/fixed-asset/depreciation',
  '/fixed-asset/transfer',
  '/fixed-asset/disposal',
  '/fixed-asset/inventory',
  '/fixed-asset/report',
  '/fixed-asset/purchase',

  // 预算管理
  '/budget',
  '/budget/template',
  '/budget/annual',
  '/budget/adjustment',
  '/budget/report',

  // 产品数据
  '/erp/product',
  '/erp/pricing',
  '/erp/pricing/approval',
  '/erp/pricing/tiers',

  // 商城管理
  '/mall/config',
  '/mall/user-audit',
  '/mall/banner',
  '/mall/order',
  '/mall/product',

  // 打印模块
  '/printing/template',
  '/printing/chain',
  '/printing/client',
  '/printing/task',
  '/printing/designer',

  // WMS仓储管理
  '/wms/warehouse',
  '/wms/location',
  '/wms/receipt',
  '/wms/putaway',
  '/wms/pick',
  '/wms/wave',
  '/wms/ship',
  '/wms/inventory',
  '/wms/move',
  '/wms/check',
  '/wms/event',

  // DMS配送管理
  '/dms/dashboard',
  '/dms/channel',
  '/dms/rider',
  '/dms/vehicle',
  '/dms/verification',
  '/dms/route',
  '/dms/dispatch',
  '/dms/order-pool',
  '/dms/config',
  '/dms/tracking',

  // 系统管理
  '/system/user',
  '/system/role',
  '/system/menu',
  '/system/permission',
  '/system/config',
  '/system/dict',
  '/system/department',
  '/system/position',
  '/system/log',
  '/system/tenant',
  '/system/tenant-approval',
  '/system/data-import',

  // 工作流
  '/workflow/instance-monitor',
  '/workflow/task-management',
  '/workflow/process-analysis',

  // 其他
  '/notification',
  '/profile',
  '/charts',
  '/order-center',
];

// 分批处理（每批10个页面）
function splitIntoBatches(pages: string[], batchSize: number = 10): string[][] {
  const batches: string[][] = [];
  for (let i = 0; i < pages.length; i += batchSize) {
    batches.push(pages.slice(i, i + batchSize));
  }
  return batches;
}

// 错误收集函数
function collectConsoleError(page: Page, pagePath: string) {
  page.on('console', msg => {
    if (msg.type() === 'error') {
      errorLogs.push({
        type: 'console',
        page: pagePath,
        message: msg.text(),
        timestamp: new Date().toISOString(),
        stack: msg.location?.toString()
      });
      console.error(`[Console Error] ${pagePath}: ${msg.text()}`);
    }
  });
}

function collectNetworkError(page: Page, pagePath: string) {
  page.on('response', response => {
    if (response.status() >= 400 && response.status() !== 401) {
      const url = response.url();
      // 过滤掉预期的404（如验证码刷新等）
      if (!url.includes('/captcha') && !url.includes('/favicon')) {
        errorLogs.push({
          type: 'network',
          page: pagePath,
          message: `HTTP ${response.status()}`,
          timestamp: new Date().toISOString(),
          url: url,
          status: response.status()
        });
        console.error(`[Network Error] ${pagePath}: ${response.status()} ${url}`);
      }
    }
  });

  page.on('requestfailed', request => {
    errorLogs.push({
      type: 'network',
      page: pagePath,
      message: `Request failed: ${request.failure()?.errorText || 'Unknown'}`,
      timestamp: new Date().toISOString(),
      url: request.url()
    });
    console.error(`[Request Failed] ${pagePath}: ${request.url()}`);
  });
}

// 登录函数（委托给已验证的 loginViaUi）
async function login(page: Page): Promise<boolean> {
  try {
    console.log('[登录] 开始登录流程...');
    await loginViaUi(page, LOGIN_CREDENTIALS.username, LOGIN_CREDENTIALS.password, LOGIN_CREDENTIALS.tenantName);
    console.log('[登录] 登录成功，当前页面:', page.url());
    return true;
  } catch (error) {
    console.error('[登录] 登录过程出错:', error);
    await page.screenshot({ path: 'test-results/login-failed.png' }).catch(() => {});
    return false;
  }
}

// 测试单个页面（快速扫描——仅检查页面能否加载、是否有控制台/网络错误）
async function testPage(page: Page, pagePath: string): Promise<ErrorLog[]> {
  const pageErrors: ErrorLog[] = [];

  // 使用一次性事件处理器，避免跨页面累积重复监听
  const onConsole = (msg: any) => {
    if (msg.type() === 'error') {
      pageErrors.push({
        type: 'console',
        page: pagePath,
        message: msg.text(),
        timestamp: new Date().toISOString(),
        stack: msg.location?.toString()
      });
    }
  };

  const onResponse = (response: any) => {
    if (response.status() >= 400 && response.status() !== 401) {
      const url = response.url();
      if (!url.includes('/captcha') && !url.includes('/favicon')) {
        pageErrors.push({
          type: 'network',
          page: pagePath,
          message: `HTTP ${response.status()}`,
          timestamp: new Date().toISOString(),
          url: url,
          status: response.status()
        });
      }
    }
  };

  page.on('console', onConsole);
  page.on('response', onResponse);

  try {
    console.log(`[测试] 开始测试页面: ${pagePath}`);

    // 访问页面
    await page.goto(pagePath, { waitUntil: 'load', timeout: 10_000 });
    // 短暂等待页面渲染
    await page.waitForTimeout(1500);

    // 检查是否有错误弹窗（使用 count 替代 isVisible 避免 strict mode 冲突）
    const errorCount = await page.locator('.ant-message-error').count();
    if (errorCount > 0) {
      pageErrors.push({
        type: 'visual',
        page: pagePath,
        message: `检测到 ${errorCount} 个错误消息弹窗`,
        timestamp: new Date().toISOString()
      });
    }

    console.log(`[测试] 页面 ${pagePath} 完成，发现 ${pageErrors.length} 个错误`);
    return pageErrors;
  } catch (error: any) {
    console.error(`[测试] 页面 ${pagePath} 测试失败:`, error.message);
    pageErrors.push({
      type: 'console',
      page: pagePath,
      message: `页面访问失败: ${error.message}`,
      timestamp: new Date().toISOString()
    });
    return pageErrors;
  } finally {
    // 清理事件监听
    page.off('console', onConsole);
    page.off('response', onResponse);
  }
}

// 主测试套件
test.describe('ERP系统全页面扫描测试', () => {
  // 串行执行，避免多 worker 并发登录导致 token 冲突
  test.describe.configure({ mode: 'serial' });
  let context: BrowserContext;
  let page: Page;

  // 为常见 API 路径设置 mock，避免页面加载时因后端不可用而弹出错误消息
  async function setupApiMocks(p: Page) {
    await p.route(/\/api\//, async route => {
      const url = new URL(route.request().url());
      // 只拦截 /api/ 开头的路径，不拦截 JS 模块或其他资源
      if (!url.pathname.startsWith('/api/')) {
        return route.fallback();
      }

      const method = route.request().method();
      const path = url.pathname;

      // POST/GET 的列表查询返回空数组
      if (path.includes('/list') || path.includes('/page')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: { records: [], total: 0, pages: 0 } })
        });
      } else if (path.includes('/stat') || path.includes('/stats') || path.includes('/kpi')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: {} })
        });
      } else if (method === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: null })
        });
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: null })
        });
      }
    });
  }

  test.beforeAll(async ({ browser, baseURL }) => {
    context = await browser.newContext({ baseURL });
    page = await context.newPage();

    // 登录（使用真实 UI 登录，依赖后端认证接口）
    const loginSuccess = await login(page);
    if (!loginSuccess) {
      throw new Error('登录失败，无法继续测试');
    }

    // 登录成功后设置 API mock，避免后续页面扫描时的网络请求错误
    await setupApiMocks(page);
  });

  test.afterAll(async () => {
    // 保存错误报告
    const reportPath = 'test-results/error-report.json';
    fs.writeFileSync(reportPath, JSON.stringify(errorLogs, null, 2));
    console.log(`[报告] 错误报告已保存到 ${reportPath}`);

    await context.close();
  });

  // 分批测试
  const batches = splitIntoBatches(PAGE_LIST, 5);

  for (let batchIndex = 0; batchIndex < batches.length; batchIndex++) {
    const batch = batches[batchIndex];

    test(`第 ${batchIndex + 1} 批页面测试 (${batch.length} 个页面)`, async () => {
      // 每批次 5 页 × ~12s = ~60s，设置 5 分钟超时
      test.setTimeout(300_000);
      console.log(`\n========== 第 ${batchIndex + 1} 批测试开始 ==========`);

      for (const pagePath of batch) {
        const errors = await testPage(page, pagePath);
        errorLogs.push(...errors);

        // 保存页面截图
        const screenshotPath = `test-results/screenshots/${pagePath.replace('/', '_')}.png`;
        await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {});
      }

      console.log(`[报告] 第 ${batchIndex + 1} 批完成，累计错误: ${errorLogs.length}`);
    });
  }
});

// 导出页面清单和分批信息
export { PAGE_LIST, splitIntoBatches, errorLogs };