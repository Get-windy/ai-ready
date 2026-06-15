/**
 * 第5批：供应商绩效、财务核心
 */
import { test, type Page } from '@playwright/test';
import { loginViaUi } from './fixtures/auth.fixture';
import { navigateToPage, waitForTable, checkTableHasRows, clickNewButton, isDetailOpen, resetPageState, checkProductionGrade } from './utils/crud-helpers';

const BASE_URL = 'http://localhost:5656';
const PAGES = [
  { name: '供应商绩效', path: '/supplier/performance', menuText: '供应商绩效', parentMenu: '客户关系', hasTable: true, hasNewBtn: true },
  { name: '财务管理', path: '/finance', menuText: '财务管理', hasTable: false, hasNewBtn: false },
  { name: '应收账款', path: '/finance/accounts-receivable', menuText: '应收明细', hasTable: true, hasNewBtn: false },
  { name: '应付账款', path: '/finance/accounts-payable', menuText: '应付明细', hasTable: true, hasNewBtn: false },
  { name: '收款管理', path: '/finance/receivable', menuText: '收款单管理', hasTable: true, hasNewBtn: true },
  { name: '付款管理', path: '/finance/payable', menuText: '付款单管理', hasTable: true, hasNewBtn: true },
  { name: '预收款', path: '/finance/pre-receipt', menuText: '预收款管理', hasTable: true, hasNewBtn: true },
  { name: '预付款', path: '/finance/pre-payment', menuText: '预付款管理', hasTable: true, hasNewBtn: true },
  { name: '保证金', path: '/finance/deposit', menuText: '定金押金管理', hasTable: true, hasNewBtn: true },
  { name: '核销管理', path: '/finance/write-off', menuText: '收付款核销', hasTable: true, hasNewBtn: true },
];

test.describe('第5批：财务核心', () => {
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
      console.log(`[${p.name}] 菜单=${menuOk ? 'Y' : 'N'}`);
      let rows = -1;
      if (p.hasTable) { await waitForTable(page); const t = await checkTableHasRows(page); rows = t.count; if (!t.visible) errs.push('表格未渲染'); }
      errs.push(...await checkProductionGrade(page));
      let newOk = false, detailOk = false;
      if (p.hasNewBtn) { newOk = await clickNewButton(page); if (newOk) { await page.waitForTimeout(800); detailOk = await isDetailOpen(page); if (!detailOk) errs.push('未弹出详情'); } else errs.push('未找到新增按钮'); }
      await resetPageState(page);
      console.log(`[结果] ${p.name} 行数=${rows} 新增=${newOk ? 'Y' : 'N'} 详情=${detailOk ? 'Y' : 'N'} 错误=${errs.length}`);
    });
  }
  test.afterAll(() => console.log('\n第5批完成'));
});
