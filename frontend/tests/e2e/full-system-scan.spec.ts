/**
 * ERP系统自动化测试脚本
 * 目的：扫描所有页面，收集错误并修复
 */

import { test, expect, Page, BrowserContext } from '@playwright/test';
import fs from 'fs';
import path from 'path';

// 测试配置
const BASE_URL = 'http://localhost:5656';
const API_BASE = 'http://localhost:5655';
const LOGIN_CREDENTIALS = {
  tenantName: '默认租户',
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

// 登录函数
async function login(page: Page): Promise<boolean> {
  try {
    console.log('[登录] 开始登录流程...');

    // 访问登录页
    await page.goto(BASE_URL + '/login', { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);

    // 检查是否已经登录
    const currentUrl = page.url();
    if (!currentUrl.includes('/login')) {
      console.log('[登录] 已登录，跳过登录流程');
      return true;
    }

    // 填写租户名称
    const tenantInput = page.locator('input[placeholder*="租户"]').first();
    if (await tenantInput.isVisible()) {
      await tenantInput.fill(LOGIN_CREDENTIALS.tenantName);
      await page.waitForTimeout(500);
    }

    // 填写用户名
    const usernameInput = page.locator('input[placeholder*="用户名"]').first();
    await usernameInput.fill(LOGIN_CREDENTIALS.username);
    await page.waitForTimeout(500);

    // 填写密码
    const passwordInput = page.locator('input[placeholder*="密码"]').first();
    await passwordInput.fill(LOGIN_CREDENTIALS.password);
    await page.waitForTimeout(500);

    // 处理验证码 - 尝试OCR或手动输入
    // 由于自动化测试环境，我们尝试绕过验证码或使用简单验证码
    const captchaInput = page.locator('input[placeholder*="验证码"]').first();
    if (await captchaInput.isVisible()) {
      // 尝试获取验证码图片并分析
      const captchaImg = page.locator('.captcha-image img').first();
      if (await captchaImg.isVisible()) {
        // 在开发环境中，验证码可能被禁用或有固定值
        // 尝试常见测试验证码值
        const testCaptchaValues = ['1234', '0000', 'test', 'abcd', 'ABCD'];
        let captchaSuccess = false;

        for (const captchaValue of testCaptchaValues) {
          try {
            await captchaInput.fill(captchaValue);
            await page.waitForTimeout(300);

            // 点击登录按钮
            const loginBtn = page.locator('button:has-text("登 录")').first();
            await loginBtn.click();
            await page.waitForTimeout(2000);

            // 检查是否成功登录
            const newUrl = page.url();
            if (!newUrl.includes('/login') && !await page.locator('.ant-message-error').isVisible()) {
              console.log(`[登录] 验证码 ${captchaValue} 成功`);
              captchaSuccess = true;
              break;
            }

            // 刷新验证码
            const refreshBtn = page.locator('.captcha-image').first();
            if (await refreshBtn.isVisible()) {
              await refreshBtn.click();
              await page.waitForTimeout(1000);
            }
          } catch (e) {
            console.log(`[登录] 验证码 ${captchaValue} 失败，尝试下一个`);
          }
        }

        if (!captchaSuccess) {
          console.log('[登录] 无法自动处理验证码，保存截图');
          await page.screenshot({ path: 'test-results/login-captcha.png' });
          return false;
        }
      }
    } else {
      // 无验证码，直接登录
      const loginBtn = page.locator('button:has-text("登 录")').first();
      await loginBtn.click();
      await page.waitForTimeout(2000);
    }

    // 验证登录成功
    const finalUrl = page.url();
    if (finalUrl.includes('/login')) {
      console.log('[登录] 登录失败，仍在登录页');
      return false;
    }

    console.log('[登录] 登录成功，当前页面:', finalUrl);
    return true;
  } catch (error) {
    console.error('[登录] 登录过程出错:', error);
    return false;
  }
}

// 测试单个页面
async function testPage(page: Page, pagePath: string): Promise<ErrorLog[]> {
  const pageErrors: ErrorLog[] = [];

  try {
    console.log(`[测试] 开始测试页面: ${pagePath}`);

    // 设置错误监听
    page.on('console', msg => {
      if (msg.type() === 'error') {
        pageErrors.push({
          type: 'console',
          page: pagePath,
          message: msg.text(),
          timestamp: new Date().toISOString(),
          stack: msg.location?.toString()
        });
      }
    });

    page.on('response', response => {
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
    });

    // 访问页面
    await page.goto(BASE_URL + pagePath, { waitUntil: 'networkidle', timeout: 30000 });
    await page.waitForTimeout(3000);

    // 检查页面是否白屏
    const bodyContent = await page.locator('body').innerHTML();
    if (!bodyContent || bodyContent.trim().length < 100) {
      pageErrors.push({
        type: 'visual',
        page: pagePath,
        message: '页面可能白屏',
        timestamp: new Date().toISOString()
      });
    }

    // 尝试点击常见按钮
    const buttons = await page.locator('button:visible').all();
    for (const btn of buttons.slice(0, 5)) {
      try {
        const btnText = await btn.textContent() || '';
        if (btnText && !btnText.includes('删除') && !btnText.includes('退出')) {
          await btn.click({ timeout: 2000 });
          await page.waitForTimeout(1000);
        }
      } catch (e) {
        // 按钮点击失败，忽略
      }
    }

    // 检查是否有错误弹窗
    const errorAlert = page.locator('.ant-message-error, .ant-alert-error');
    if (await errorAlert.isVisible()) {
      const alertText = await errorAlert.textContent() || '';
      pageErrors.push({
        type: 'visual',
        page: pagePath,
        message: `错误弹窗: ${alertText}`,
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
  }
}

// 主测试套件
test.describe('ERP系统全页面扫描测试', () => {
  let context: BrowserContext;
  let page: Page;

  test.beforeAll(async ({ browser }) => {
    context = await browser.newContext();
    page = await context.newPage();

    // 登录
    const loginSuccess = await login(page);
    if (!loginSuccess) {
      throw new Error('登录失败，无法继续测试');
    }
  });

  test.afterAll(async () => {
    // 保存错误报告
    const reportPath = 'test-results/error-report.json';
    fs.writeFileSync(reportPath, JSON.stringify(errorLogs, null, 2));
    console.log(`[报告] 错误报告已保存到 ${reportPath}`);

    await context.close();
  });

  // 分批测试
  const batches = splitIntoBatches(PAGE_LIST, 10);

  for (let batchIndex = 0; batchIndex < batches.length; batchIndex++) {
    const batch = batches[batchIndex];

    test(`第 ${batchIndex + 1} 批页面测试 (${batch.length} 个页面)`, async () => {
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