/**
 * 第11批：系统管理（上）
 */
import { test, type Page } from '@playwright/test';
import { loginViaUi } from './fixtures/auth.fixture';
import { navigateToPage, waitForTable, checkTableHasRows, clickNewButton, isDetailOpen, resetPageState, checkProductionGrade } from './utils/crud-helpers';

const BASE_URL = 'http://localhost:5656';
const PAGES = [
  { name: '用户管理', path: '/system/user', menuText: '用户管理', parentMenu: '系统管理', hasTable: true, hasNewBtn: true },
  { name: '角色管理', path: '/system/role', menuText: '角色管理', parentMenu: '系统管理', hasTable: true, hasNewBtn: true },
  { name: '菜单管理', path: '/system/menu', menuText: '菜单管理', parentMenu: '系统管理', hasTable: true, hasNewBtn: true },
  { name: '系统配置', path: '/system/config', menuText: '系统配置', parentMenu: '系统管理', hasTable: true, hasNewBtn: false },
  { name: '部门管理', path: '/system/department', menuText: '组织架构', parentMenu: '系统管理', hasTable: true, hasNewBtn: true },
];

test.describe('第11批：系统管理（上）', () => {
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
  test.afterAll(() => console.log('\n第11批完成'));
});
