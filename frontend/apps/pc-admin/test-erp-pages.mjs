/**
 * ERP系统自动化测试脚本
 * 功能：登录 -> 扫描所有页面 -> 收集控制台错误和网络请求错误
 */

import { chromium } from 'playwright';
import fs from 'fs';

const BASE_URL = 'http://localhost:5656';
const LOGIN_URL = `${BASE_URL}/login`;

// 登录凭证
const credentials = {
  username: 'admin',
  password: 'admin123',
  tenant: '系统租户'
};

// 页面路由列表（从动态路由配置提取）
const pageRoutes = [
  // 工作台
  '/dashboard',

  // 销售模块
  '/sale',
  '/erp/sale',
  '/stock',
  '/erp/shipment',
  '/erp/return',
  '/erp/sales-analysis',
  '/erp/sales-report',

  // 采购模块
  '/purchase',
  '/erp/purchase',
  '/erp/stock-in',
  '/erp/purchase-exchange',

  // 库存模块
  '/erp/stock',
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

  // 财务模块
  '/finance',
  '/finance/receivable',
  '/finance/payable',
  '/finance/pre-receipt',
  '/finance/pre-payment',
  '/finance/deposit',
  '/finance/write-off',
  '/finance/offset',
  '/finance/capital-flow',
  '/finance/receipt',
  '/finance/payment',
  '/finance/report',
  '/finance/voucher',
  '/finance/subject',
  '/finance/accounts-receivable',
  '/finance/accounts-payable',
  '/finance/reconciliation',

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

  // 预算模块
  '/budget',
  '/budget/template',
  '/budget/annual',
  '/budget/adjustment',
  '/budget/report',

  // 产品数据
  '/erp/product',
  '/erp/partner',
  '/erp/pricing',
  '/erp/pricing/approval',
  '/erp/pricing/tiers',

  // 供应商
  '/supplier',
  '/supplier/inquiry',
  '/supplier/performance',

  // 商城
  '/mall/config',
  '/mall/user-audit',
  '/mall/banner',
  '/mall/order',
  '/mall/product',

  // WMS仓储
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

  // DMS配送
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

  // 打印模块
  '/printing/template',
  '/printing/chain',
  '/printing/client',
  '/printing/task',
  '/printing/designer',

  // 系统管理
  '/system/user',
  '/system/role',
  '/system/menu',
  '/system/department',
  '/system/position',
  '/system/config',
  '/system/dict',
  '/system/log',
  '/system/permission',
  '/system/tenant',
  '/system/tenant-approval',
  '/system/data-import',
  '/admin/sys/permissions',
  '/admin/tenant/permissions',

  // 工作流
  '/workflow/instance-monitor',
  '/workflow/task-management',
  '/workflow/process-analysis',

  // 其他
  '/notification',
  '/profile',
  '/charts',
  '/erp/dashboard',
];

// 错误收集
const errors = {
  console: [],
  network: [],
  pageLoad: [],
};

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function runTest() {
  console.log('启动浏览器...');
  const browser = await chromium.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });

  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
  });

  const page = await context.newPage();

  // 收集控制台错误
  page.on('console', msg => {
    if (msg.type() === 'error') {
      errors.console.push({
        url: page.url(),
        message: msg.text(),
        location: msg.location(),
      });
      console.log(`[Console Error] ${msg.text()}`);
    }
  });

  // 收集网络请求错误
  page.on('requestfailed', request => {
    const failure = request.failure();
    errors.network.push({
      url: page.url(),
      requestUrl: request.url(),
      method: request.method(),
      failure: failure?.errorText || 'Unknown',
    });
    console.log(`[Network Failed] ${request.url()} - ${failure?.errorText}`);
  });

  // 收集响应错误
  page.on('response', response => {
    const status = response.status();
    if (status >= 400) {
      errors.network.push({
        url: page.url(),
        requestUrl: response.url(),
        method: response.request().method(),
        status: status,
        statusText: response.statusText(),
      });
      console.log(`[HTTP Error] ${status} ${response.url()}`);
    }
  });

  try {
    // 1. 登录
    console.log(`访问登录页: ${LOGIN_URL}`);
    await page.goto(LOGIN_URL, { waitUntil: 'networkidle', timeout: 30000 });
    await sleep(2000);

    // 检查登录表单
    console.log('填写登录表单...');

    // 查找并填写用户名
    const usernameInput = await page.$('input[type="text"]') || await page.$('input[placeholder*="用户名"]') || await page.$('input[placeholder*="账号"]');
    if (usernameInput) {
      await usernameInput.fill(credentials.username);
    } else {
      console.log('找不到用户名输入框');
      // 截图
      await page.screenshot({ path: 'login-page-no-input.png' });
    }

    // 查找并填写密码
    const passwordInput = await page.$('input[type="password"]');
    if (passwordInput) {
      await passwordInput.fill(credentials.password);
    }

    // 查找租户选择（如果有）
    const tenantSelect = await page.$('select') || await page.$('.ant-select');
    if (tenantSelect) {
      try {
        // 尝试选择租户
        await tenantSelect.click();
        await sleep(500);
        const tenantOption = await page.$(`text=${credentials.tenant}`);
        if (tenantOption) {
          await tenantOption.click();
        }
      } catch (e) {
        console.log('租户选择失败，跳过');
      }
    }

    // 点击登录按钮
    await sleep(1000);
    const loginBtn = await page.$('button[type="submit"]') || await page.$('.login-btn') || await page.$('button:has-text("登录")') || await page.$('button:has-text("登 录")');
    if (loginBtn) {
      await loginBtn.click();
    } else {
      console.log('找不到登录按钮');
      await page.screenshot({ path: 'login-page-no-btn.png' });
    }

    // 等待登录完成
    console.log('等待登录完成...');
    await sleep(5000);

    // 检查是否登录成功（检查URL是否变化或是否有token）
    const currentUrl = page.url();
    if (currentUrl.includes('login')) {
      console.log('登录可能失败，当前仍在登录页');
      await page.screenshot({ path: 'login-failed.png' });

      // 检查是否有错误提示
      const errorMsg = await page.$('.ant-message-error') || await page.$('.error-message');
      if (errorMsg) {
        const errorText = await errorMsg.textContent();
        console.log(`登录错误: ${errorText}`);
        errors.pageLoad.push({ url: LOGIN_URL, error: `登录失败: ${errorText}` });
      }
    } else {
      console.log(`登录成功，当前URL: ${currentUrl}`);
      await page.screenshot({ path: 'login-success.png' });
    }

    // 2. 扫描页面
    let successCount = 0;
    let failCount = 0;

    for (const route of pageRoutes) {
      const url = `${BASE_URL}${route}`;
      console.log(`\n访问页面: ${route}`);

      try {
        await page.goto(url, { waitUntil: 'networkidle', timeout: 15000 });
        await sleep(2000);

        // 检查页面状态
        const pageTitle = await page.title();
        const pageContent = await page.content();

        // 检查是否有白屏（页面内容极少）
        if (pageContent.length < 500) {
          errors.pageLoad.push({ url, error: '页面内容过少，可能白屏' });
          console.log(`[白屏警告] ${route}`);
        }

        // 检查是否有404/500错误页面
        if (pageTitle.includes('404') || pageTitle.includes('500') || pageTitle.includes('错误')) {
          errors.pageLoad.push({ url, error: `错误页面: ${pageTitle}` });
          console.log(`[错误页面] ${route} - ${pageTitle}`);
        }

        // 截图
        const screenshotPath = `screenshots/${route.replace(/\//g, '_')}.png`;
        await page.screenshot({ path: screenshotPath, fullPage: false });

        successCount++;
        console.log(`[成功] ${route}`);

      } catch (err) {
        failCount++;
        errors.pageLoad.push({ url, error: err.message });
        console.log(`[失败] ${route} - ${err.message}`);

        try {
          await page.screenshot({ path: `screenshots/error_${route.replace(/\//g, '_')}.png` });
        } catch (e) {
          console.log('截图失败');
        }
      }
    }

    console.log(`\n========== 测试完成 ==========`);
    console.log(`成功页面: ${successCount}`);
    console.log(`失败页面: ${failCount}`);
    console.log(`控制台错误: ${errors.console.length}`);
    console.log(`网络错误: ${errors.network.length}`);

    // 写入错误报告
    const report = {
      timestamp: new Date().toISOString(),
      summary: {
        totalPages: pageRoutes.length,
        successCount,
        failCount,
        consoleErrors: errors.console.length,
        networkErrors: errors.network.length,
      },
      errors,
    };

    fs.writeFileSync('test-report.json', JSON.stringify(report, null, 2));
    console.log('\n错误报告已保存到 test-report.json');

    // 按类型分组输出错误摘要
    if (errors.console.length > 0) {
      console.log('\n=== 控制台错误摘要 ===');
      errors.console.slice(0, 10).forEach(e => {
        console.log(`- ${e.url}: ${e.message}`);
      });
    }

    if (errors.network.length > 0) {
      console.log('\n=== 网络错误摘要 ===');
      const grouped = {};
      errors.network.forEach(e => {
        const key = e.status ? `${e.status}` : e.failure || 'Unknown';
        grouped[key] = (grouped[key] || 0) + 1;
      });
      Object.entries(grouped).forEach(([key, count]) => {
        console.log(`- ${key}: ${count}次`);
      });

      // 输出具体的404错误
      const notFound404s = errors.network.filter(e => e.status === 404);
      if (notFound404s.length > 0) {
        console.log('\n404错误详情:');
        notFound404s.slice(0, 20).forEach(e => {
          console.log(`  ${e.requestUrl}`);
        });
      }
    }

    if (errors.pageLoad.length > 0) {
      console.log('\n=== 页面加载错误 ===');
      errors.pageLoad.forEach(e => {
        console.log(`- ${e.url}: ${e.error}`);
      });
    }

  } catch (err) {
    console.log(`测试执行错误: ${err.message}`);
    errors.pageLoad.push({ url: 'global', error: err.message });
  }

  await browser.close();
  return errors;
}

runTest().catch(console.error);