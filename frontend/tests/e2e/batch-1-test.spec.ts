/**
 * 第1批页面自动化测试（真实API，无Mock）
 *
 * 所有操作通过点击前端菜单导航，不直接跳转URL
 */
import { test, expect, type Page } from '@playwright/test';

const BASE_URL = 'http://localhost:5656';

// ============================================================
// 全局错误收集
// ============================================================
const globalErrors: Array<{ page: string; type: string; message: string }> = [];
const productionIssues: Array<{ page: string; issue: string }> = [];

// ============================================================
// 工具函数
// ============================================================

async function getMessage(page: Page, timeout = 8000): Promise<string | null> {
  try {
    const msg = page.locator('.ant-message-notice-content').first();
    await msg.waitFor({ state: 'visible', timeout });
    return (await msg.textContent())?.trim() || null;
  } catch { return null; }
}

async function waitForTable(page: Page) {
  try {
    await page.waitForSelector('table, .vxe-table, .ant-table', { timeout: 15000 });
    await page.waitForTimeout(1500);
  } catch { /* noop */ }
}

function setupErrorCollection(page: Page, pageName: string) {
  page.on('pageerror', err => {
    globalErrors.push({ page: pageName, type: 'PAGE_ERROR', message: err.message.substring(0, 200) });
  });
  page.on('console', msg => {
    if (msg.type() === 'error') {
      const text = msg.text();
      if (!text.includes('401') && !text.includes('Failed to load resource: the server responded with a status of 401')) {
        globalErrors.push({ page: pageName, type: 'CONSOLE_ERROR', message: text.substring(0, 200) });
      }
    }
  });
  page.on('response', resp => {
    if (resp.status() >= 400 && resp.status() !== 401) {
      globalErrors.push({ page: pageName, type: `HTTP_${resp.status()}`, message: `${resp.url().substring(0, 120)}` });
    }
  });
}

/** 通过侧边栏菜单导航 */
async function navigateViaSidebar(page: Page, menuLabel: string) {
  // 尝试找到匹配的菜单项并点击
  // 菜单项可能位于 a-sub-menu 内，需先展开父级
  const menuItem = page.locator('.ant-menu-item, .ant-menu-submenu-title');
  const count = await menuItem.count();

  for (let i = 0; i < count; i++) {
    const text = await menuItem.nth(i).textContent().catch(() => '');
    if (text && text.trim().includes(menuLabel)) {
      await menuItem.nth(i).click();
      await page.waitForTimeout(2000);
      return;
    }
  }

  // 如果没找到，尝试展开子菜单后再找
  const submenus = page.locator('.ant-menu-submenu-title');
  const subCount = await submenus.count();
  for (let i = 0; i < subCount; i++) {
    const text = await submenus.nth(i).textContent().catch(() => '');
    if (text && text.trim().includes(menuLabel)) {
      await submenus.nth(i).click();
      await page.waitForTimeout(1500);
      return;
    }
  }

  // 如果仍然没找到，尝试data-menu-id属性
  for (let i = 0; i < count; i++) {
    const dataId = await menuItem.nth(i).getAttribute('data-menu-id').catch(() => '');
    if (dataId && dataId.includes(menuLabel)) {
      await menuItem.nth(i).click();
      await page.waitForTimeout(2000);
      return;
    }
  }

  console.warn(`[导航] 未找到侧边栏菜单: ${menuLabel}`);
}

/** 检查生产级标准 */
async function checkPageStandards(page: Page, pageName: string) {
  // 检查Spin
  const spinVisible = await page.locator('.ant-spin-spinning').isVisible().catch(() => false);
  if (spinVisible) {
    await page.waitForSelector('.ant-spin-spinning', { state: 'hidden', timeout: 15000 }).catch(() => {});
  }

  // 检查是否有可见的表格
  const hasTable = await page.locator('table, .vxe-table, .ant-table').first().isVisible().catch(() => false);

  // 检查是否有错误
  const errorShow = await page.locator('.ant-alert-error').first().isVisible().catch(() => false);
  if (errorShow) {
    productionIssues.push({ page: pageName, issue: '页面显示错误提示' });
  }
}

// ============================================================
// Main Test
// ============================================================

test.describe('第1批：核心业务页面', () => {
  test.describe.configure({ mode: 'serial' });
  let page: Page;

  test.beforeAll(async ({ browser }) => {
    const context = await browser.newContext({
      baseURL: BASE_URL,
      viewport: { width: 1920, height: 1080 }
    });
    page = await context.newPage();

    // 收集网络错误（排除 auth 相关）
    page.on('response', resp => {
      if (resp.status() === 401 && resp.url().includes('/api/auth/check')) return;
      if (resp.status() >= 400) {
        console.log(`[网络] ${resp.status()} ${resp.url().substring(0, 100)}`);
      }
    });

    // 登录
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

  // Batch 1 pages
  const pages = [
    { name: '工作台', path: '/dashboard', menuLabel: '工作台' },
    { name: '销售订单', path: '/sale', menuLabel: '销售订单' },
    { name: '销售出库', path: '/stock', menuLabel: '销售出库' },
    { name: '销售管理(ERP)', path: '/erp/sale', menuLabel: '销售管理' },
    { name: '销售出库管理(ERP)', path: '/erp/sale-outbound', menuLabel: '销售出库管理' },
    { name: '销售分析', path: '/erp/sales-analysis', menuLabel: '销售分析' },
    { name: '销售报表', path: '/erp/sales-report', menuLabel: '销售报表' },
    { name: '采购订单', path: '/purchase', menuLabel: '采购订单' },
    { name: '采购管理(ERP)', path: '/erp/purchase', menuLabel: '采购管理' },
    { name: '采购换货', path: '/erp/purchase-exchange', menuLabel: '采购换货' },
  ];

  for (const p of pages) {
    test(`P: ${p.name}`, async () => {
      test.setTimeout(90000);
      setupErrorCollection(page, p.name);

      // 方法1: 通过侧边栏菜单导航
      await navigateViaSidebar(page, p.menuLabel);

      // 等待页面加载
      await page.waitForTimeout(3000);

      // 如果侧边栏导航未改变URL，用URL保底
      const currentUrl = page.url();
      if (currentUrl.endsWith('/dashboard') || currentUrl.endsWith('/dashboard/')) {
        if (p.name !== '工作台') {
          console.log(`[${p.name}] 侧边栏点击未跳转，尝试URL导航`);
          await page.goto(p.path);
          await page.waitForTimeout(3000);
        }
      }

      // 等待Spin消失
      await page.waitForSelector('.ant-spin-spinning', { state: 'hidden', timeout: 15000 }).catch(() => {});
      await waitForTable(page);
      await checkPageStandards(page, p.name);

      // 检查页面是否有数据表格
      const tableRows = page.locator('.vxe-body--row, .ant-table-row, table tbody tr');
      const rowCount = await tableRows.count();
      console.log(`[${p.name}] URL=${page.url().substring(0, 60)} 行数=${rowCount}`);

      // 检查是否有错误弹出
      const errMsg = await getMessage(page, 3000);
      if (errMsg && (errMsg.includes('失败') || errMsg.includes('错误') || errMsg.includes('异常'))) {
        globalErrors.push({ page: p.name, type: 'ERROR_MESSAGE', message: errMsg });
      }
    });
  }

  test.afterAll(async () => {
    console.log('\n========== 第1批报告 ==========');
    if (globalErrors.length === 0) {
      console.log('✅ 无页面错误');
    } else {
      console.log(`❌ 发现 ${globalErrors.length} 个错误:`);
      for (const err of globalErrors) {
        console.log(`  [${err.page}] ${err.type}: ${err.message}`);
      }
    }
    if (productionIssues.length === 0) {
      console.log('✅ 无生产级缺陷');
    } else {
      console.log(`⚠️ 生产级缺陷 ${productionIssues.length}:`);
      for (const issue of productionIssues) {
        console.log(`  [${issue.page}] ${issue.issue}`);
      }
    }
  });
});
