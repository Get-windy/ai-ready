/**
 * 第6批：财务深化 + 费用
 */
import { test, type Page } from '@playwright/test';
import { loginViaUi } from './fixtures/auth.fixture';
import { navigateToPage, waitForTable, checkTableHasRows, clickNewButton, isDetailOpen, resetPageState, checkProductionGrade } from './utils/crud-helpers';

const BASE_URL = 'http://localhost:5656';
const PAGES = [
  { name: '冲账管理', path: '/finance/offset', menuText: '往来对冲', hasTable: true, hasNewBtn: true },
  { name: '资金流水', path: '/finance/capital-flow', menuText: '资金流水台账', hasTable: true, hasNewBtn: false },
  { name: '收款记录', path: '/finance/receipt', menuText: '收款单管理', hasTable: true, hasNewBtn: true },
  { name: '付款记录', path: '/finance/payment', menuText: '付款单管理', hasTable: true, hasNewBtn: true },
  { name: '会计科目', path: '/finance/subject', menuText: '科目管理', hasTable: true, hasNewBtn: true },
  { name: '凭证管理', path: '/finance/voucher', menuText: '凭证管理', hasTable: true, hasNewBtn: true },
  { name: '对账管理', path: '/finance/reconciliation', menuText: '对账管理', hasTable: true, hasNewBtn: true },
  { name: '财务报表', path: '/finance/report', menuText: '财务报表', hasTable: true, hasNewBtn: false },
  { name: '费用申请', path: '/erp/expense/application', menuText: '费用申请', hasTable: true, hasNewBtn: true },
  { name: '费用报销', path: '/erp/expense/reimbursement', menuText: '费用报销', hasTable: true, hasNewBtn: true },
];

test.describe('第6批：财务深化 + 费用', () => {
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
      console.log(`[结果] ${p.name} 行数=${rows} 新=${newOk ? 'Y' : 'N'} 详=${detailOk ? 'Y' : 'N'} 误=${errs.length}`);
    });
  }
  test.afterAll(() => console.log('\n第6批完成'));
});
