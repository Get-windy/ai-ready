/**
 * ERP系统快速自动化测试脚本
 * 只测试关键页面，收集控制台错误和网络错误
 */

const { chromium } = require('playwright');

const FRONTEND_URL = 'http://localhost:5656';
const USERNAME = 'admin';
const PASSWORD = 'admin123';
const TENANT = '系统租户';

const errors = [];

// 关键页面列表（第一批测试）
const KEY_PAGES = [
  '/dashboard',
  '/erp/product',
  '/erp/partner',
  '/sale',
  '/purchase',
  '/erp/stock',
  '/crm/customer',
  '/crm/lead',
  '/crm/opportunity',
  '/crm/quotation',
  '/crm/contract',
  '/system/user',
  '/system/role',
  '/system/menu',
  '/system/department',
  '/system/dict',
  '/system/config',
  '/finance/receivable',
  '/finance/payable',
  '/finance/voucher',
  '/finance/receipt',
  '/finance/payment',
  '/wms/warehouse',
  '/wms/location',
  '/wms/receipt',
  '/wms/putaway',
  '/wms/pick',
  '/wms/ship',
  '/wms/inventory',
  '/wms/move',
  '/wms/check',
  '/erp/stocktake',
  '/erp/batch',
  '/erp/serial',
  '/erp/stock-cost-adjust',
  '/erp/stock-overflow',
  '/erp/stock-damage',
  '/erp/stock-transfer',
  '/erp/stock-bom',
  '/erp/stock-assemble',
  '/erp/stock-split',
  '/erp/expense/application',
  '/erp/expense/reimbursement',
  '/erp/pricing',
  '/erp/purchase-exchange',
  '/erp/stock-in',
  '/erp/shipment',
  '/erp/return',
  '/erp/sales-analysis',
  '/erp/sales-report',
  '/fixed-asset/asset',
  '/fixed-asset/category',
  '/budget',
  '/budget/template',
  '/budget/annual',
  '/mall/config',
  '/mall/order',
  '/mall/product',
  '/printing/template',
  '/printing/chain',
  '/printing/task',
  '/dms/dashboard',
  '/dms/rider',
  '/dms/vehicle',
  '/dms/dispatch',
  '/dms/tracking',
  '/workflow/instance-monitor',
  '/workflow/task-management',
  '/notification',
  '/profile',
];

async function main() {
  console.log('=== ERP快速自动化测试 ===');

  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();

  // 收集错误
  page.on('console', msg => {
    if (msg.type() === 'error') {
      errors.push({ type: 'console', message: msg.text() });
      console.error(`[Console] ${msg.text()}`);
    }
  });

  page.on('pageerror', err => {
    errors.push({ type: 'page', message: err.message });
    console.error(`[PageError] ${err.message}`);
  });

  page.on('response', response => {
    if (response.status() >= 400 && !response.url().includes('sse')) {
      errors.push({ type: 'http', url: response.url(), status: response.status() });
      console.error(`[HTTP ${response.status()}] ${response.url()}`);
    }
  });

  try {
    // 登录
    console.log('\n--- 登录 ---');
    await page.goto(FRONTEND_URL);
    await page.waitForTimeout(2000);

    // 填写登录表单
    await page.waitForSelector('input[placeholder="请输入租户名称"]', { timeout: 10000 });
    await page.fill('input[placeholder="请输入租户名称"]', TENANT);
    await page.waitForTimeout(500);
    await page.fill('input[placeholder="请输入用户名"]', USERNAME);
    await page.waitForTimeout(500);
    await page.fill('input[placeholder="请输入密码"]', PASSWORD);
    await page.waitForTimeout(500);
    await page.fill('input[placeholder="请输入验证码"]', '1234');
    await page.waitForTimeout(500);

    // 点击登录按钮
    const loginBtn = page.locator('button[type="submit"]');
    await loginBtn.waitFor({ timeout: 10000 });
    await loginBtn.click();
    await page.waitForTimeout(5000);

    // 检查登录是否成功
    const sidebar = await page.locator('.ant-layout-sider').isVisible();
    if (!sidebar) {
      console.log('登录失败，检查错误');
      const errorMsg = await page.locator('.ant-message-error').textContent().catch(() => '');
      console.log(`错误信息: ${errorMsg}`);
    } else {
      console.log('登录成功');
    }

    // 测试关键页面
    console.log('\n--- 测试关键页面 ---');
    for (const path of KEY_PAGES) {
      console.log(`测试: ${path}`);
      try {
        await page.goto(`${FRONTEND_URL}${path}`);
        await page.waitForTimeout(3000);

        // 检查页面是否正常
        const hasContent = await page.locator('.ant-layout-content').isVisible().catch(() => false);
        const hasError = await page.locator('.ant-alert-error, .ant-message-error').isVisible().catch(() => false);

        if (!hasContent) {
          errors.push({ type: 'visual', page: path, message: '页面内容未显示' });
          console.log(`  [错误] 页面内容未显示`);
        } else if (hasError) {
          const errText = await page.locator('.ant-alert-error, .ant-message-error').textContent().catch(() => '未知错误');
          errors.push({ type: 'visual', page: path, message: errText });
          console.log(`  [错误] ${errText}`);
        } else {
          console.log(`  [正常]`);
        }
      } catch (e) {
        errors.push({ type: 'navigation', page: path, message: e.message });
        console.log(`  [失败] ${e.message}`);
      }
    }

    // 输出报告
    console.log('\n=== 测试报告 ===');
    console.log(`总页面数: ${KEY_PAGES.length}`);
    console.log(`错误数: ${errors.length}`);
    console.log(`控制台错误: ${errors.filter(e => e.type === 'console').length}`);
    console.log(`HTTP错误: ${errors.filter(e => e.type === 'http').length}`);
    console.log(`页面错误: ${errors.filter(e => e.type === 'page').length}`);
    console.log(`视觉错误: ${errors.filter(e => e.type === 'visual').length}`);

    if (errors.length > 0) {
      console.log('\n--- 错误详情 ---');
      errors.forEach(e => console.log(JSON.stringify(e)));
    }

  } catch (e) {
    console.error('测试失败:', e);
  }

  await browser.close();
}

main();