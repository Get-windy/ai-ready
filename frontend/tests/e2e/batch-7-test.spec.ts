/**
 * 第7批：费用审批/付款/统计 + 固定资产
 * 注意：每个测试使用独立 page，避免状态串扰
 */
import { test, type BrowserContext } from '@playwright/test';
import { loginViaUi } from './fixtures/auth.fixture';
import { assertPageQuality } from './utils/crud-helpers';

const BASE_URL = 'http://localhost:5656';
const PAGES = [
  { name: '费用审批', path: '/erp/expense/approval', menuText: '费用审批', hasTable: true, hasNewBtn: false },
  { name: '费用付款', path: '/erp/expense/payment', menuText: '费用付款', hasTable: true, hasNewBtn: false },
  { name: '费用统计', path: '/erp/expense/statistics', menuText: '费用统计', hasTable: false, hasNewBtn: false },
  { name: '资产列表', path: '/fixed-asset/asset', menuText: '资产台账', parentMenu: '资产管理', hasTable: true, hasNewBtn: false },
  { name: '资产分类', path: '/fixed-asset/category', menuText: '资产分类', parentMenu: '资产管理', hasTable: true, hasNewBtn: true },
  { name: '折旧管理', path: '/fixed-asset/depreciation', menuText: '折旧管理', parentMenu: '资产管理', hasTable: true, hasNewBtn: false },
  { name: '资产调拨', path: '/fixed-asset/transfer', menuText: '资产调拨', parentMenu: '资产管理', hasTable: true, hasNewBtn: true },
  { name: '资产处置', path: '/fixed-asset/disposal', menuText: '资产处置', parentMenu: '资产管理', hasTable: true, hasNewBtn: true },
  { name: '资产盘点', path: '/fixed-asset/inventory', menuText: '资产盘点', parentMenu: '资产管理', hasTable: true, hasNewBtn: true },
];

test.describe('第7批：费用 + 固定资产', () => {
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
  test.afterAll(async () => { await ctx.close(); console.log('\n第7批完成'); });
});
