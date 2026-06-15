/**
 * 第9批：WMS仓储执行
 */
import { test, type Page } from '@playwright/test';
import { loginViaUi } from './fixtures/auth.fixture';
import { navigateToPage, waitForTable, checkTableHasRows, clickNewButton, isDetailOpen, resetPageState, checkProductionGrade } from './utils/crud-helpers';

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
  test.afterAll(() => console.log('\n第9批完成'));
});
