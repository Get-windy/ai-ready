/**
 * 第9批：WMS仓储执行
 * 注意：每个测试使用独立 page，避免状态串扰
 */
import { test, type BrowserContext } from '@playwright/test';
import { loginViaUi } from './fixtures/auth.fixture';
import { assertPageQuality } from './utils/crud-helpers';

const BASE_URL = 'http://localhost:5656';
const PAGES = [
  { name: 'WMS仓库', path: '/wms/warehouse', menuText: '仓库管理', parentMenu: 'WMS仓储执行', hasTable: true, hasNewBtn: true },
  { name: 'WMS库位', path: '/wms/location', menuText: '库位管理', parentMenu: 'WMS仓储执行', hasTable: true, hasNewBtn: true },
  { name: 'WMS收货', path: '/wms/receipt', menuText: '收货管理', parentMenu: 'WMS仓储执行', hasTable: true, hasNewBtn: true },
  { name: 'WMS上架', path: '/wms/putaway', menuText: '上架管理', parentMenu: 'WMS仓储执行', hasTable: true, hasNewBtn: true },
  { name: 'WMS拣货', path: '/wms/pick', menuText: '拣货管理', parentMenu: 'WMS仓储执行', hasTable: true, hasNewBtn: true },
  { name: 'WMS波次', path: '/wms/wave', menuText: '波次管理', parentMenu: 'WMS仓储执行', hasTable: true, hasNewBtn: true },
  { name: 'WMS发货', path: '/wms/ship', menuText: '发货管理', parentMenu: 'WMS仓储执行', hasTable: true, hasNewBtn: true },
  { name: 'WMS库存', path: '/wms/inventory', menuText: '库存查询', parentMenu: 'WMS仓储执行', hasTable: true, hasNewBtn: false },
  { name: 'WMS移库', path: '/wms/move', menuText: '库内移库', parentMenu: 'WMS仓储执行', hasTable: true, hasNewBtn: true },
  { name: 'WMS盘点', path: '/wms/check', menuText: '盘点管理', parentMenu: 'WMS仓储执行', hasTable: true, hasNewBtn: true },
];

test.describe('第9批：WMS', () => {
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
  test.afterAll(async () => { await ctx.close(); console.log('\n第9批完成'); });
});
