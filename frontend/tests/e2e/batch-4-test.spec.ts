/**
 * 第4批页面深度CRUD测试
 * 页面：价格审批、价格层级、客户管理、线索管理、商机管理、报价、合同、发票、供应商、询价
 */
import { test, type Page } from '@playwright/test';
import { loginViaUi } from './fixtures/auth.fixture';
import { navigateToPage, waitForTable, checkTableHasRows, clickNewButton, isDetailOpen, resetPageState, checkProductionGrade } from './utils/crud-helpers';

const BASE_URL = 'http://localhost:5656';
const PAGES = [
  { name: '价格审批', path: '/erp/pricing/approval', menuText: '定价审批', parentMenu: '产品数据', hasTable: true, hasNewBtn: false },
  { name: '价格层级', path: '/erp/pricing/tiers', menuText: '价格层级', parentMenu: '产品数据', hasTable: true, hasNewBtn: true },
  { name: '客户管理', path: '/crm/customer', menuText: '客户档案', parentMenu: '客户关系', hasTable: true, hasNewBtn: true },
  { name: '线索管理', path: '/crm/lead', menuText: '线索管理', parentMenu: '客户关系', hasTable: true, hasNewBtn: true },
  { name: '商机管理', path: '/crm/opportunity', menuText: '商机管理', parentMenu: '客户关系', hasTable: true, hasNewBtn: true },
  { name: '报价管理(CRM)', path: '/crm/quotation', menuText: '报价管理', parentMenu: '客户关系', hasTable: true, hasNewBtn: true },
  { name: '合同管理(CRM)', path: '/crm/contract', menuText: '合同管理', parentMenu: '客户关系', hasTable: true, hasNewBtn: true },
  { name: '发票管理(CRM)', path: '/crm/invoice', menuText: '发票管理', parentMenu: '客户关系', hasTable: true, hasNewBtn: true },
  { name: '供应商管理', path: '/supplier', menuText: '供应商管理', parentMenu: '客户关系', hasTable: true, hasNewBtn: true },
  { name: '供应商询价', path: '/supplier/inquiry', menuText: '供应商询价', parentMenu: '客户关系', hasTable: true, hasNewBtn: true },
];

test.describe('第4批：CRM + 供应商', () => {
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
  test.afterAll(() => console.log('\n第4批完成'));
});
