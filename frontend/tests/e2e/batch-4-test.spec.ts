/**
 * 第4批页面深度CRUD测试
 * 页面：价格审批、价格层级、客户管理、线索管理、商机管理、报价、合同、发票、供应商、询价
 * 注意：每个测试使用独立 page，避免状态串扰
 */
import { test, type BrowserContext } from '@playwright/test';
import { loginViaUi } from './fixtures/auth.fixture';
import { assertPageQuality } from './utils/crud-helpers';

const BASE_URL = 'http://localhost:5656';
const PAGES = [
  { name: '价格审批', path: '/erp/pricing/approval', menuText: '定价审批', parentMenu: '产品数据', hasTable: true, hasNewBtn: false },
  { name: '价格层级', path: '/erp/pricing/tiers', menuText: '价格层级', parentMenu: '产品数据', hasTable: true, hasNewBtn: true },
  { name: '客户管理', path: '/crm/customer', menuText: '客户档案', parentMenu: '客户关系', hasTable: true, hasNewBtn: true },
  { name: '线索管理', path: '/crm/lead', menuText: '线索管理', parentMenu: '客户关系', hasTable: true, hasNewBtn: true },
  { name: '商机管理', path: '/crm/opportunity', menuText: '商机管理', parentMenu: '客户关系', hasTable: false, hasNewBtn: false },
  { name: '报价管理(CRM)', path: '/crm/quotation', menuText: '报价管理', parentMenu: '客户关系', hasTable: true, hasNewBtn: true },
  { name: '合同管理(CRM)', path: '/crm/contract', menuText: '合同管理', parentMenu: '客户关系', hasTable: true, hasNewBtn: true },
  { name: '发票管理(CRM)', path: '/crm/invoice', menuText: '发票管理', parentMenu: '客户关系', hasTable: true, hasNewBtn: true },
  { name: '供应商管理', path: '/supplier', menuText: '供应商管理', parentMenu: '客户关系', hasTable: true, hasNewBtn: true },
  { name: '供应商询价', path: '/supplier/inquiry', menuText: '供应商询价', parentMenu: '客户关系', hasTable: true, hasNewBtn: false },
];

test.describe('第4批：CRM + 供应商', () => {
  let ctx: BrowserContext;
  test.beforeAll(async ({ browser }) => {
    ctx = await browser.newContext({ baseURL: BASE_URL, viewport: { width: 1920, height: 1080 } });
    const p = await ctx.newPage();
    await loginViaUi(p, 'admin', 'admin123', '系统租户');
    await p.close();
  });
  for (const pg of PAGES) {
    test(pg.name, async () => {
      const p = await ctx.newPage();
      await p.goto('/dashboard', { waitUntil: 'load', timeout: 20000 }).catch(() => {});
      await assertPageQuality(p, pg);
      await p.close();
    });
  }
  test.afterAll(async () => { await ctx.close(); console.log('\n第4批完成'); });
});
