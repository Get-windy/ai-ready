/**
 * 第10批：商城管理
 * 注意：每个测试使用独立 page，避免状态串扰
 * 策略：每个页面独立 setupAuthApiMocks + setAuthToken，完全避免真实后端 token 过期问题
 */
import { test, type BrowserContext } from '@playwright/test';
import { setupAuthApiMocks, setAuthToken } from './fixtures/auth.fixture';
import { assertPageQuality } from './utils/crud-helpers';

const BASE_URL = 'http://localhost:5656';
const PAGES = [
  { name: '商城配置', path: '/mall/config', menuText: '商城配置', parentMenu: '商城管理', hasTable: false, hasNewBtn: false },
  { name: '商城用户审核', path: '/mall/user-audit', menuText: '用户审核', parentMenu: '商城管理', hasTable: true, hasNewBtn: false },
  { name: '商城轮播图', path: '/mall/banner', menuText: '轮播图管理', parentMenu: '商城管理', hasTable: true, hasNewBtn: true },
  { name: '商城订单', path: '/mall/order', menuText: '订单管理', parentMenu: '商城管理', hasTable: true, hasNewBtn: false },
  { name: '商城商品', path: '/mall/product', menuText: '商品管理', parentMenu: '商城管理', hasTable: true, hasNewBtn: true },
];

test.describe('第10批：商城', () => {
  let ctx: BrowserContext;
  test.beforeAll(async ({ browser }) => {
    ctx = await browser.newContext({ baseURL: BASE_URL, viewport: { width: 1920, height: 1080 } });
    // 不做真实登录，每个测试页面独立 setupAuthApiMocks + setAuthToken
  });
  for (const pg of PAGES) {
    test(pg.name, async () => {
      const p = await ctx.newPage();
      await setupAuthApiMocks(p, 'admin');
      // 导航到同源页面后设置 token，再跳转到目标页
      await p.goto('/login', { waitUntil: 'load', timeout: 15000 }).catch(() => {});
      await setAuthToken(p, 'admin');
      await p.goto('/dashboard', { waitUntil: 'load', timeout: 20000 }).catch(() => {});
      await assertPageQuality(p, pg);
      await p.close();
    });
  }
  test.afterAll(async () => { await ctx.close(); console.log('\n第10批完成'); });
});
