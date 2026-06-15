/**
 * 第3批页面深度CRUD测试
 *
 * 页面：库存深化（成本调整、溢余、报损、预警、BOM、组装、拆分）+ 产品数据
 */
import { test, type Page } from '@playwright/test';
import { loginViaUi } from './fixtures/auth.fixture';
import {
  navigateToPage, waitForTable, checkTableHasRows,
  clickNewButton, isDetailOpen, resetPageState,
  checkProductionGrade
} from './utils/crud-helpers';

const BASE_URL = 'http://localhost:5656';
const BATCH_NUM = 3;

const pages: BatchPage[] = [
  { name: '成本调整', path: '/erp/stock-cost-adjust', menuText: '库存成本调整', parentMenu: '仓储作业', hasTable: true, hasForm: true, hasNewButton: true, hasEditButton: true, hasDeleteButton: true, hasSearch: true },
  { name: '溢余管理', path: '/erp/stock-overflow', menuText: '库存溢余处理', parentMenu: '仓储作业', hasTable: true, hasForm: true, hasNewButton: true, hasEditButton: true, hasDeleteButton: true, hasSearch: true },
  { name: '报损管理', path: '/erp/stock-damage', menuText: '库存报损处理', parentMenu: '仓储作业', hasTable: true, hasForm: true, hasNewButton: true, hasEditButton: true, hasDeleteButton: true, hasSearch: true },
  { name: '库存预警配置', path: '/erp/stock-alert-config', menuText: '库存预警配置', parentMenu: '仓储作业', hasTable: true, hasForm: true, hasNewButton: true, hasEditButton: true, hasDeleteButton: false, hasSearch: true },
  { name: 'BOM管理', path: '/erp/stock-bom', menuText: 'BOM管理', parentMenu: '仓储作业', hasTable: true, hasForm: true, hasNewButton: true, hasEditButton: true, hasDeleteButton: true, hasSearch: true },
  { name: '组装管理', path: '/erp/stock-assemble', menuText: '组装管理', parentMenu: '仓储作业', hasTable: true, hasForm: true, hasNewButton: true, hasEditButton: true, hasDeleteButton: true, hasSearch: true },
  { name: '拆分管理', path: '/erp/stock-split', menuText: '拆分管理', parentMenu: '仓储作业', hasTable: true, hasForm: true, hasNewButton: true, hasEditButton: true, hasDeleteButton: true, hasSearch: true },
  { name: '产品管理', path: '/erp/product', menuText: '产品管理', hasTable: true, hasForm: true, hasNewButton: true, hasEditButton: true, hasDeleteButton: true, hasSearch: true },
  { name: '往来单位', path: '/erp/partner', menuText: '往来单位', hasTable: true, hasForm: true, hasNewButton: true, hasEditButton: true, hasDeleteButton: true, hasSearch: true },
  { name: '价格管理', path: '/erp/pricing', menuText: '定价管理', hasTable: true, hasForm: true, hasNewButton: true, hasEditButton: true, hasDeleteButton: true, hasSearch: true },
];

test.describe(`第${BATCH_NUM}批：库存深化+产品数据`, () => {
  test.describe.configure({ mode: 'serial' });
  let page: Page;

  test.beforeAll(async ({ browser }) => {
    const context = await browser.newContext({ baseURL: BASE_URL, viewport: { width: 1920, height: 1080 } });
    page = await context.newPage();

    console.log('[登录] 开始...');
    await loginViaUi(page, 'admin', 'admin123', '系统租户');
    console.log('[登录] 成功');
    await page.waitForTimeout(2000);
  });

  for (const pg of pages) {
    test(`${pg.name}`, async () => {
      const rowErrors: string[] = [];

      // 1. 导航
      const menuOk = await navigateToPage(page, pg);
      console.log(`[${pg.name}] 菜单=${menuOk ? 'Y' : 'N'} URL=${page.url().substring(0, 60)}`);

      // 2. 表格
      let rows = -1;
      if (pg.hasTable) {
        await waitForTable(page);
        const ti = await checkTableHasRows(page);
        rows = ti.count;
        if (!ti.visible) rowErrors.push('表格未渲染');
      }

      // 3. 生产级检查
      const prodIssues = await checkProductionGrade(page);
      rowErrors.push(...prodIssues);

      // 4. 新建按钮
      let newOk = false;
      let detailOk = false;
      if (pg.hasNewButton) {
        newOk = await clickNewButton(page);
        if (newOk) {
          await page.waitForTimeout(800);
          detailOk = await isDetailOpen(page);
          if (!detailOk) rowErrors.push('点击新增后未弹出详情');
        } else {
          rowErrors.push('未找到新增按钮');
        }
      }

      // 5. 重置
      await resetPageState(page);

      if (rowErrors.length > 0) {
        rowErrors.forEach(e => console.log(`  [错误] ${e}`));
      }
      console.log(`[结果] ${pg.name} 行数=${rows} 新增=${newOk ? 'Y' : 'N'} 详情=${detailOk ? 'Y' : 'N'} 错误=${rowErrors.length}`);
    });
  }

  test.afterAll(async () => {
    console.log(`\n========== 第${BATCH_NUM}批完成 ==========`);
  });
});
