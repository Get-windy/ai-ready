/**
 * 第12批：系统管理（下）+ 工作流 + 通知
 */
import { test, type Page } from '@playwright/test';
import { loginViaUi } from './fixtures/auth.fixture';
import { navigateToPage, waitForTable, checkTableHasRows, clickNewButton, isDetailOpen, resetPageState, checkProductionGrade } from './utils/crud-helpers';

const BASE_URL = 'http://localhost:5656';
const PAGES = [
  { name: '字典管理', path: '/system/dict', menuText: '字典管理', parentMenu: '系统管理', hasTable: true, hasNewBtn: true },
  { name: '日志管理', path: '/system/log', menuText: '系统日志', parentMenu: '系统管理', hasTable: true, hasNewBtn: false },
  { name: '权限管理', path: '/system/permission', menuText: '权限管理', parentMenu: '系统管理', hasTable: true, hasNewBtn: true },
  { name: '岗位管理', path: '/system/position', menuText: '岗位管理', parentMenu: '系统管理', hasTable: true, hasNewBtn: true },
  { name: '租户管理', path: '/system/tenant', menuText: '租户管理', parentMenu: '系统管理', hasTable: true, hasNewBtn: true },
  { name: '数据导入', path: '/system/data-import', menuText: '数据导入', parentMenu: '系统管理', hasTable: false, hasNewBtn: false },
  { name: '流程监控', path: '/workflow/instance-monitor', menuText: '流程监控', parentMenu: '工作流', hasTable: true, hasNewBtn: false },
  { name: '任务管理', path: '/workflow/task-management', menuText: '任务管理', parentMenu: '工作流', hasTable: true, hasNewBtn: false },
  { name: '流程分析', path: '/workflow/process-analysis', menuText: '流程分析', parentMenu: '工作流', hasTable: false, hasNewBtn: false },
  { name: '通知公告', path: '/notification/index', menuText: '通知公告', parentMenu: '系统管理', hasTable: true, hasNewBtn: true },
];

test.describe('第12批：系统管理（下）', () => {
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
  test.afterAll(() => console.log('\n第12批完成'));
});
