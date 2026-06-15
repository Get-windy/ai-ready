/**
 * 第8批：预算管理 + 打印管理
 */
import { test, type Page } from '@playwright/test';
import { loginViaUi } from './fixtures/auth.fixture';
import { navigateToPage, waitForTable, checkTableHasRows, clickNewButton, isDetailOpen, resetPageState, checkProductionGrade } from './utils/crud-helpers';

const BASE_URL = 'http://localhost:5656';
const PAGES = [
  { name: '预算管理', path: '/budget', menuText: '预算概览', parentMenu: '预算管理', hasTable: true, hasNewBtn: true },
  { name: '打印模板', path: '/printing/template', menuText: '打印模板', parentMenu: '打印管理', hasTable: true, hasNewBtn: true },
  { name: '打印链路', path: '/printing/chain', menuText: '打印链路', parentMenu: '打印管理', hasTable: true, hasNewBtn: true },
  { name: '打印客户端', path: '/printing/client', menuText: '打印客户端', parentMenu: '打印管理', hasTable: true, hasNewBtn: true },
  { name: '打印任务', path: '/printing/task', menuText: '打印任务', parentMenu: '打印管理', hasTable: true, hasNewBtn: false },
  { name: '打印设计器', path: '/printing/designer', menuText: '打印设计器', parentMenu: '打印管理', hasTable: false, hasNewBtn: true },
];

test.describe('第8批：预算 + 打印', () => {
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
  test.afterAll(() => console.log('\n第8批完成'));
});
