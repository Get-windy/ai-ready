/**
 * 第7批：费用审批/付款/统计 + 固定资产
 */
import { test, type Page } from '@playwright/test';
import { loginViaUi } from './fixtures/auth.fixture';
import { navigateToPage, waitForTable, checkTableHasRows, clickNewButton, isDetailOpen, resetPageState, checkProductionGrade } from './utils/crud-helpers';

const BASE_URL = 'http://localhost:5656';
const PAGES = [
  { name: '费用审批', path: '/erp/expense/approval', menuText: '费用审批', hasTable: true, hasNewBtn: false },
  { name: '费用付款', path: '/erp/expense/payment', menuText: '费用付款', hasTable: true, hasNewBtn: false },
  { name: '费用统计', path: '/erp/expense/statistics', menuText: '费用统计', hasTable: false, hasNewBtn: false },
  { name: '资产列表', path: '/fixed-asset/asset', menuText: '资产台账', parentMenu: '资产管理', hasTable: true, hasNewBtn: true },
  { name: '资产分类', path: '/fixed-asset/category', menuText: '资产分类', parentMenu: '资产管理', hasTable: true, hasNewBtn: true },
  { name: '折旧管理', path: '/fixed-asset/depreciation', menuText: '折旧管理', parentMenu: '资产管理', hasTable: true, hasNewBtn: true },
  { name: '资产调拨', path: '/fixed-asset/transfer', menuText: '资产调拨', parentMenu: '资产管理', hasTable: true, hasNewBtn: true },
  { name: '资产处置', path: '/fixed-asset/disposal', menuText: '资产处置', parentMenu: '资产管理', hasTable: true, hasNewBtn: true },
  { name: '资产盘点', path: '/fixed-asset/inventory', menuText: '资产盘点', parentMenu: '资产管理', hasTable: true, hasNewBtn: true },
];

test.describe('第7批：费用 + 固定资产', () => {
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
  test.afterAll(() => console.log('\n第7批完成'));
});
