/**
 * 第10批：商城管理
 */
import { test, type Page } from '@playwright/test';
import { loginViaUi } from './fixtures/auth.fixture';
import { navigateToPage, waitForTable, checkTableHasRows, clickNewButton, isDetailOpen, resetPageState, checkProductionGrade } from './utils/crud-helpers';

const BASE_URL = 'http://localhost:5656';
const PAGES = [
  { name: '商城配置', path: '/mall/config', menuText: '商城配置', parentMenu: '商城管理', hasTable: false, hasNewBtn: false },
  { name: '商城用户审核', path: '/mall/user-audit', menuText: '用户审核', parentMenu: '商城管理', hasTable: true, hasNewBtn: false },
  { name: '商城轮播图', path: '/mall/banner', menuText: '轮播图管理', parentMenu: '商城管理', hasTable: true, hasNewBtn: true },
  { name: '商城订单', path: '/mall/order', menuText: '订单管理', parentMenu: '商城管理', hasTable: true, hasNewBtn: false },
  { name: '商城商品', path: '/mall/product', menuText: '商品管理', parentMenu: '商城管理', hasTable: true, hasNewBtn: true },
];

test.describe('第10批：商城', () => {
  test.describe.configure({ mode: 'serial' });
  let page: Page;
  test.beforeAll(async ({ browser }) => {
    const ctx = await browser.newContext({ baseURL: BASE_URL, viewport: { width: 1920, height: 1080 } });
    page = await ctx.newPage();
    await loginViaUi(page, 'admin', 'admin123', '系统租户');
    await page.waitForTimeout(2000);
  });
  for (const p of PAGES) {
    test(p.name, async () => {
      const errs: string[] = [];
      const menuOk = await navigateToPage(page, p);
      let rows = -1;
      if (p.hasTable) { await waitForTable(page); const t = await checkTableHasRows(page); rows = t.count; if (!t.visible) errs.push('表格未渲染'); }
      errs.push(...await checkProductionGrade(page));
      let newOk = false, detailOk = false;
      if (p.hasNewBtn) { newOk = await clickNewButton(page); if (newOk) { await page.waitForTimeout(800); detailOk = await isDetailOpen(page); if (!detailOk) errs.push('未弹出详情'); } else errs.push('未找到新增按钮'); }
      await resetPageState(page);
      console.log(`[结果] ${p.name} 行=${rows} 新=${newOk ? 'Y' : 'N'} 详=${detailOk ? 'Y' : 'N'} 误=${errs.length}`);
    });
  }
  test.afterAll(() => console.log('\n第10批完成'));
});
