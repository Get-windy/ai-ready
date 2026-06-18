/**
 * 第2批页面自动化测试（真实API，无Mock）
 *
 * 页面清单：
 *  1. 采购退货 /erp/purchase-return
 *  2. 入库管理 /erp/stock-in
 *  3. 库存管理(ERP) /erp/stock
 *  4. 发货管理 /erp/shipment
 *  5. 库存盘点 /erp/stocktake
 *  6. 退货管理 /erp/return
 *  7. 批次管理 /erp/batch
 *  8. 序列号管理 /erp/serial
 *  9. 智能补货 /erp/stock/replenishment
 *  10. 库存调拨 /erp/stock-transfer
 */
import { test, expect, type Page } from '@playwright/test';

const BASE_URL = 'http://localhost:5656';

const globalErrors: Array<{ page: string; type: string; message: string }> = [];
const productionIssues: Array<{ page: string; issue: string }> = [];

function setupErrorCollection(page: Page, pageName: string) {
  page.on('pageerror', err => {
    globalErrors.push({ page: pageName, type: 'PAGE_ERROR', message: err.message.substring(0, 200) });
  });
  page.on('response', resp => {
    const status = resp.status();
    if (status >= 400) {
      const url = resp.url().substring(0, 100);
      if (!url.includes('/auth/') && !url.includes('/sse/')) {
        globalErrors.push({ page: pageName, type: `HTTP_${status}`, message: url });
      }
    }
  });
}

async function waitForLoad(page: Page) {
  await page.waitForTimeout(2000);
  try {
    await page.waitForSelector('.ant-spin-spinning', { state: 'hidden', timeout: 10000 });
  } catch {}
}

async function navigateViaMenuFallback(page: Page, path: string) {
  await page.goto(path);
  await waitForLoad(page);
}

async function checkPageContent(page: Page, pageName: string) {
  // 检查表格
  const tables = page.locator('.vxe-table, .ant-table, table');
  const hasTable = await tables.first().isVisible().catch(() => false);

  const rows = page.locator('.vxe-body--row, .ant-table-row, table tbody tr');
  const rowCount = await rows.count();

  console.log(`[${pageName}] 表格=${hasTable} 行数=${rowCount}`);

  // 检查是否有Spin还在转（数据加载问题）
  const isSpinning = await page.locator('.ant-spin-spinning').isVisible().catch(() => false);
  if (isSpinning) {
    productionIssues.push({ page: pageName, issue: '页面一直处于加载状态' });
  }

  return { hasTable, rowCount };
}

test.describe('第2批：采购仓储页面', () => {
  test.describe.configure({ mode: 'serial' });
  let page: Page;

  test.beforeAll(async ({ browser }) => {
    const context = await browser.newContext({
      baseURL: BASE_URL,
      viewport: { width: 1920, height: 1080 }
    });
    page = await context.newPage();

    // Login
    console.log('[登录] 开始...');
    await page.goto('/login');
    await page.waitForSelector('input[placeholder="请输入用户名"]', { timeout: 15000 });
    await page.waitForSelector('.captcha-image img', { timeout: 10000 }).catch(() => {});

    const ti = page.locator('input[placeholder="请输入租户名称"]');
    if (await ti.isVisible({ timeout: 3000 }).catch(() => false)) await ti.fill('系统租户');
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    const ci = page.locator('input[placeholder="请输入验证码"]');
    if (await ci.isVisible({ timeout: 3000 }).catch(() => false)) await ci.fill('ABCD');

    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard**', { timeout: 25000 });
    await page.waitForSelector('.basic-layout, .ant-layout', { timeout: 15000 });
    console.log('[登录] 成功');
  });

  const pages = [
    { name: '采购退货', path: '/erp/purchase-return' },
    { name: '入库管理', path: '/erp/stock-in' },
    { name: '库存管理(ERP)', path: '/erp/stock' },
    { name: '发货管理', path: '/erp/shipment' },
    { name: '库存盘点', path: '/erp/stocktake' },
    { name: '退货管理', path: '/erp/return' },
    { name: '批次管理', path: '/erp/batch' },
    { name: '序列号管理', path: '/erp/serial' },
    { name: '智能补货', path: '/erp/stock/replenishment' },
    { name: '库存调拨', path: '/erp/stock-transfer' },
  ];

  for (const p of pages) {
    test(`P: ${p.name}`, async () => {
      test.setTimeout(60000);
      setupErrorCollection(page, p.name);

      // Navigate via URL (due to sidebar complexity)
      await navigateViaMenuFallback(page, p.path);
      const { hasTable, rowCount } = await checkPageContent(page, p.name);

      // For pages with data, try basic operations
      if (hasTable && rowCount >= 0) {
        // Try clicking "新建" or "新增" button
        const newBtn = page.locator('button').filter({ hasText: /新建|新增|创建/ }).first();
        if (await newBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
          console.log(`[${p.name}] 找到新建按钮`);
        }

        // Check for search/query button
        const searchBtn = page.locator('button').filter({ hasText: /查询|搜索|刷新/ }).first();
        if (await searchBtn.isVisible({ timeout: 1000 }).catch(() => false)) {
          console.log(`[${p.name}] 找到查询按钮`);
        }
      }

      const currentUrl = page.url();
      console.log(`[${p.name}] URL: ${currentUrl.substring(0, 70)}`);
    });
  }

  test.afterAll(async () => {
    console.log('\n========== 第2批报告 ==========');
    if (globalErrors.length === 0) {
      console.log('✅ 无页面错误');
    } else {
      console.log(`❌ 发现 ${globalErrors.length} 个错误:`);
      for (const err of globalErrors) {
        console.log(`  [${err.page}] ${err.type}: ${err.message}`);
      }
    }
  });
});
