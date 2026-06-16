/**
 * 第3批页面深度CRUD测试
 *
 * 页面：库存深化（成本调整、溢余、报损、预警、BOM、组装、拆分）+ 产品数据
 * 注意：每个测试使用独立 page，避免状态串扰
 */
import { test, type BrowserContext, type Page } from '@playwright/test';
import { loginViaUi } from './fixtures/auth.fixture';
import { assertPageQuality } from './utils/crud-helpers';

const BASE_URL = 'http://localhost:5656';
const BATCH_NUM = 3;

interface PageDef {
  name: string; path: string; menuText: string; parentMenu?: string;
  hasTable: boolean; hasNewBtn: boolean;
}

const pages: PageDef[] = [
  { name: '成本调整', path: '/erp/stock-cost-adjust', menuText: '库存成本调整', parentMenu: '仓储作业', hasTable: true, hasNewBtn: true },
  { name: '溢余管理', path: '/erp/stock-overflow', menuText: '库存溢余处理', parentMenu: '仓储作业', hasTable: true, hasNewBtn: true },
  { name: '报损管理', path: '/erp/stock-damage', menuText: '库存报损处理', parentMenu: '仓储作业', hasTable: true, hasNewBtn: true },
  { name: '库存预警配置', path: '/erp/stock-alert-config', menuText: '库存预警配置', parentMenu: '仓储作业', hasTable: true, hasNewBtn: true },
  { name: 'BOM管理', path: '/erp/stock-bom', menuText: 'BOM管理', parentMenu: '仓储作业', hasTable: true, hasNewBtn: true },
  { name: '组装管理', path: '/erp/stock-assemble', menuText: '组装管理', parentMenu: '仓储作业', hasTable: true, hasNewBtn: true },
  { name: '拆分管理', path: '/erp/stock-split', menuText: '拆分管理', parentMenu: '仓储作业', hasTable: true, hasNewBtn: true },
  { name: '产品管理', path: '/erp/product', menuText: '产品管理', hasTable: true, hasNewBtn: true },
  { name: '往来单位', path: '/erp/partner', menuText: '往来单位', hasTable: true, hasNewBtn: true },
  { name: '价格管理', path: '/erp/pricing', menuText: '定价管理', hasTable: false, hasNewBtn: false },
];

test.describe(`第${BATCH_NUM}批：库存深化+产品数据`, () => {
  // 共享 context（保持登录态），但每个测试使用独立 page
  let ctx: BrowserContext;

  test.beforeAll(async ({ browser }) => {
    ctx = await browser.newContext({ baseURL: BASE_URL, viewport: { width: 1920, height: 1080 } });
    const p = await ctx.newPage();
    await loginViaUi(p, 'admin', 'admin123', '系统租户');
    await p.close();
  });

  for (const pg of pages) {
    test(pg.name, async () => {
      const p = await ctx.newPage();
      await p.goto('/dashboard', { waitUntil: 'load', timeout: 20000 }).catch(() => {});
      await assertPageQuality(p, pg);
      await p.close();
    });
  }

  test.afterAll(async () => {
    await ctx.close();
    console.log(`\n========== 第${BATCH_NUM}批完成 ==========`);
  });
});
