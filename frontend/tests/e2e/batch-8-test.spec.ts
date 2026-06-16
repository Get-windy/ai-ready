/**
 * 第8批：预算管理 + 打印管理
 * 注意：每个测试使用独立 page，避免状态串扰
 */
import { test, type BrowserContext } from '@playwright/test';
import { loginViaUi } from './fixtures/auth.fixture';
import { assertPageQuality } from './utils/crud-helpers';

const BASE_URL = 'http://localhost:5656';
const PAGES = [
  { name: '预算管理', path: '/budget', menuText: '预算概览', parentMenu: '预算管理', hasTable: false, hasNewBtn: false },
  { name: '打印模板', path: '/printing/template', menuText: '打印模板', parentMenu: '打印管理', hasTable: true, hasNewBtn: true },
  { name: '打印链路', path: '/printing/chain', menuText: '打印链路', parentMenu: '打印管理', hasTable: true, hasNewBtn: true },
  { name: '打印客户端', path: '/printing/client', menuText: '打印客户端', parentMenu: '打印管理', hasTable: true, hasNewBtn: true },
  { name: '打印任务', path: '/printing/task', menuText: '打印任务', parentMenu: '打印管理', hasTable: true, hasNewBtn: false },
  { name: '打印设计器', path: '/printing/designer', menuText: '打印设计器', parentMenu: '打印管理', hasTable: false, hasNewBtn: false },
];

test.describe('第8批：预算 + 打印', () => {
  test.describe.configure({ mode: 'serial' });
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
  test.afterAll(async () => { await ctx.close(); console.log('\n第8批完成'); });
});
