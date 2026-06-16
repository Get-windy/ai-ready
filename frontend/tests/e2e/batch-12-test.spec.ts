/**
 * 第12批：系统管理（下）+ 工作流 + 通知
 * 注意：每个测试使用独立 page，避免状态串扰
 */
import { test, type BrowserContext } from '@playwright/test';
import { loginViaUi } from './fixtures/auth.fixture';
import { assertPageQuality } from './utils/crud-helpers';

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
  { name: '通知公告', path: '/notification/index', menuText: '通知公告', parentMenu: '系统管理', hasTable: true, hasNewBtn: false },
];

test.describe('第12批：系统管理（下）', () => {
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
  test.afterAll(async () => { await ctx.close(); console.log('\n第12批完成'); });
});
