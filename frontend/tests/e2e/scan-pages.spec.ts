/**
 * ERP 全页面自动扫描测试
 *
 * 策略：
 * 1. 通过 UI 真实登录
 * 2. 从侧边栏菜单获取所有页面路由
 * 3. 点击菜单项逐个访问页面
 * 4. 收集控制台错误、网络错误、布局问题
 * 5. 分批处理，每批 10 个页面
 */
import { test, expect, type Page, type BrowserContext } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

const BASE_URL = 'http://localhost:5656';

// ==================== 错误收集 ====================
interface TestError {
  type: 'blocking' | 'api' | 'interaction' | 'style' | 'production-grade' | 'backend';
  page: string;
  title: string;
  message: string;
  detail?: string;
}

const allErrors: TestError[] = [];
let pageIndex = 0;

function addError(type: TestError['type'], pageTitle: string, title: string, message: string, detail?: string) {
  allErrors.push({ type, page: pageTitle, title, message, detail });
}

// ==================== 页面清单 ====================
// 从 dynamicRoutes.ts 和侧边栏菜单提取的所有功能性页面
const ALL_PAGES = [
  // 工作台
  { route: '/dashboard', title: '工作台', menu: '工作台' },

  // ── 系统管理 ──
  { route: '/system/department', title: '组织架构', menu: '系统管理', submenu: '组织架构' },
  { route: '/system/position', title: '岗位管理', menu: '系统管理', submenu: '岗位管理' },
  { route: '/system/user', title: '用户管理', menu: '系统管理', submenu: '用户管理' },
  { route: '/system/role', title: '角色管理', menu: '系统管理', submenu: '角色管理' },
  { route: '/system/permission', title: '权限管理', menu: '系统管理', submenu: '权限管理' },
  { route: '/system/menu', title: '菜单管理', menu: '系统管理', submenu: '菜单管理' },
  { route: '/system/tenant', title: '租户管理', menu: '系统管理', submenu: '租户管理' },
  { route: '/system/tenant-approval', title: '租户审批', menu: '系统管理', submenu: '租户审批' },
  { route: '/system/dict', title: '字典管理', menu: '系统管理', submenu: '字典管理' },
  { route: '/system/config', title: '系统配置', menu: '系统管理', submenu: '系统配置' },
  { route: '/system/log', title: '系统日志', menu: '系统管理', submenu: '系统日志' },
  { route: '/system/data-import', title: '数据导入', menu: '系统管理', submenu: '数据导入' },
  { route: '/notification', title: '通知公告', menu: '系统管理', submenu: '通知公告' },

  // ── 工作流 ──
  { route: '/workflow/instance-monitor', title: '流程监控', menu: '工作流', submenu: '流程监控' },
  { route: '/workflow/task-management', title: '任务管理', menu: '工作流', submenu: '任务管理' },
  { route: '/workflow/process-analysis', title: '流程分析', menu: '工作流', submenu: '流程分析' },

  // ── 产品数据 ──
  { route: '/erp/product', title: '产品管理', menu: '产品数据', submenu: '产品管理' },
  { route: '/erp/partner', title: '往来单位', menu: '产品数据', submenu: '往来单位' },
  { route: '/erp/pricing', title: '定价管理', menu: '产品数据', submenu: '定价管理' },
  { route: '/erp/pricing/approval', title: '定价审批', menu: '产品数据', submenu: '定价审批' },
  { route: '/erp/pricing/tiers', title: '价格层级', menu: '产品数据', submenu: '价格层级' },

  // ── 采购管理 ──
  { route: '/purchase', title: '采购订单', menu: '采购管理', submenu: '采购订单' },
  { route: '/erp/stock-in', title: '入库管理', menu: '采购管理', submenu: '入库管理' },
  { route: '/erp/purchase-return', title: '采购退货', menu: '采购管理', submenu: '采购退货' },
  { route: '/erp/purchase-exchange', title: '采购换货', menu: '采购管理', submenu: '采购换货' },

  // ── 销售管理 ──
  { route: '/sale', title: '销售订单', menu: '销售管理', submenu: '销售订单' },
  { route: '/erp/sale-outbound', title: '销售出库', menu: '销售管理', submenu: '销售出库' },
  { route: '/erp/shipment', title: '发货管理', menu: '销售管理', submenu: '发货管理' },
  { route: '/erp/return', title: '退货处理', menu: '销售管理', submenu: '退货处理' },
  { route: '/erp/sales-analysis', title: '销售分析', menu: '销售管理', submenu: '销售分析' },
  { route: '/erp/sales-report', title: '销售报表', menu: '销售管理', submenu: '销售报表' },

  // ── CRM客户关系 ──
  { route: '/crm/lead', title: '线索管理', menu: '客户管理', submenu: '线索管理' },
  { route: '/crm/opportunity', title: '商机管理', menu: '客户管理', submenu: '商机管理' },
  { route: '/crm/customer', title: '客户档案', menu: '客户管理', submenu: '客户档案' },
  { route: '/crm/contract', title: '合同管理', menu: '客户管理', submenu: '合同管理' },
  { route: '/crm/quotation', title: '报价管理', menu: '客户管理', submenu: '报价管理' },
  { route: '/crm/invoice', title: '发票管理', menu: '客户管理', submenu: '发票管理' },

  // ── 库存管理 ──
  { route: '/erp/stock', title: '库存管理', menu: '库存管理', submenu: '库存管理' },
  { route: '/erp/stocktake', title: '库存盘点', menu: '库存管理', submenu: '库存盘点' },
  { route: '/erp/stock-cost-adjust', title: '成本调整', menu: '库存管理', submenu: '成本调整' },
  { route: '/erp/stock-overflow', title: '盘盈入库', menu: '库存管理', submenu: '盘盈入库' },
  { route: '/erp/stock-damage', title: '报损处理', menu: '库存管理', submenu: '报损处理' },
  { route: '/erp/stock-transfer', title: '调拨管理', menu: '库存管理', submenu: '调拨管理' },
  { route: '/erp/stock-replenishment', title: '智能补货', menu: '库存管理', submenu: '智能补货' },
  { route: '/erp/stock-alert-config', title: '预警配置', menu: '库存管理', submenu: '预警配置' },
  { route: '/erp/stock-bom', title: 'BOM管理', menu: '库存管理', submenu: 'BOM管理' },
  { route: '/erp/stock-assemble', title: '组装管理', menu: '库存管理', submenu: '组装管理' },
  { route: '/erp/stock-split', title: '拆分管理', menu: '库存管理', submenu: '拆分管理' },
  { route: '/erp/batch', title: '批次管理', menu: '库存管理', submenu: '批次管理' },
  { route: '/erp/serial', title: '序列号管理', menu: '库存管理', submenu: '序列号管理' },

  // ── 财务管理 ──
  { route: '/finance', title: '财务管理', menu: '财务管理', submenu: '财务管理' },
  { route: '/finance/accounts-receivable', title: '应收管理', menu: '财务管理', submenu: '应收管理' },
  { route: '/finance/accounts-payable', title: '应付管理', menu: '财务管理', submenu: '应付管理' },
  { route: '/finance/voucher', title: '凭证管理', menu: '财务管理', submenu: '凭证管理' },
  { route: '/finance/subject', title: '科目管理', menu: '财务管理', submenu: '科目管理' },
  { route: '/finance/receipt', title: '收款单', menu: '财务管理', submenu: '收款单' },
  { route: '/finance/payment', title: '付款单', menu: '财务管理', submenu: '付款单' },
  { route: '/finance/report', title: '财务报表', menu: '财务管理', submenu: '财务报表' },

  // ── 费用管理 ──
  { route: '/erp/expense/application', title: '费用申请', menu: '费用管理', submenu: '费用申请' },
  { route: '/erp/expense/reimbursement', title: '费用报销', menu: '费用管理', submenu: '费用报销' },
  { route: '/erp/expense/approval', title: '费用审批', menu: '费用管理', submenu: '费用审批' },
  { route: '/erp/expense/payment', title: '费用付款', menu: '费用管理', submenu: '费用付款' },
  { route: '/erp/expense/statistics', title: '费用统计', menu: '费用管理', submenu: '费用统计' },

  // ── 固定资产 ──
  { route: '/fixed-asset/asset', title: '资产列表', menu: '固定资产', submenu: '资产列表' },
  { route: '/fixed-asset/category', title: '资产分类', menu: '固定资产', submenu: '资产分类' },
  { route: '/fixed-asset/depreciation', title: '折旧管理', menu: '固定资产', submenu: '折旧管理' },
  { route: '/fixed-asset/transfer', title: '资产调拨', menu: '固定资产', submenu: '资产调拨' },
  { route: '/fixed-asset/disposal', title: '资产处置', menu: '固定资产', submenu: '资产处置' },
  { route: '/fixed-asset/inventory', title: '资产盘点', menu: '固定资产', submenu: '资产盘点' },
  { route: '/fixed-asset/report', title: '资产报表', menu: '固定资产', submenu: '资产报表' },

  // ── 预算管理 ──
  { route: '/budget', title: '预算管理', menu: '预算管理', submenu: '预算管理' },
  { route: '/budget/template', title: '预算模板', menu: '预算管理', submenu: '预算模板' },
  { route: '/budget/annual', title: '年度预算', menu: '预算管理', submenu: '年度预算' },
  { route: '/budget/adjustment', title: '预算调整', menu: '预算管理', submenu: '预算调整' },
  { route: '/budget/report', title: '预算报表', menu: '预算管理', submenu: '预算报表' },

  // ── 商城管理 ──
  { route: '/mall/config', title: '商城配置', menu: '商城管理', submenu: '商城配置' },
  { route: '/mall/banner', title: '轮播图', menu: '商城管理', submenu: '轮播图' },
  { route: '/mall/product', title: '商城商品', menu: '商城管理', submenu: '商城商品' },
  { route: '/mall/order', title: '商城订单', menu: '商城管理', submenu: '商城订单' },

  // ── 供应商管理 ──
  { route: '/supplier', title: '供应商管理', menu: '供应商管理', submenu: '供应商列表' },
  { route: '/supplier/inquiry', title: '供应商询价', menu: '供应商管理', submenu: '询价管理' },
  { route: '/supplier/performance', title: '供应商绩效', menu: '供应商管理', submenu: '绩效评价' },

  // ── 打印管理 ──
  { route: '/printing/template', title: '打印模板', menu: '打印管理', submenu: '打印模板' },
  { route: '/printing/chain', title: '打印链路', menu: '打印管理', submenu: '打印链路' },
  { route: '/printing/client', title: '打印客户端', menu: '打印管理', submenu: '打印客户端' },
  { route: '/printing/task', title: '打印任务', menu: '打印管理', submenu: '打印任务' },
];

// 批量分组
function chunkArray<T>(arr: T[], size: number): T[][] {
  const chunks: T[][] = [];
  for (let i = 0; i < arr.length; i += size) {
    chunks.push(arr.slice(i, i + size));
  }
  return chunks;
}

const PAGE_BATCHES = chunkArray(ALL_PAGES, 5); // 每批5页便于调试

// ==================== 页面检查工具 ====================
async function checkDetailPageStandards(page: Page, pageTitle: string) {
  // 2.1 详情页整体结构检查
  // 检查是否使用全屏详情页（不应是Modal）
  const modal = page.locator('.ant-modal:visible');
  if (await modal.count().then(c => c > 0).catch(() => false)) {
    const modalTitle = await modal.locator('.ant-modal-title').textContent().catch(() => '');
    addError('production-grade', pageTitle, '详情页使用Modal而非全屏',
      `详情页 "${pageTitle}" 使用了小尺寸Modal承载，应改为全屏覆盖模式`);
  }

  // 检查底部固定操作栏
  const fixedBottom = page.locator('.detail-footer, .fixed-footer, [class*="footer"]:visible').last();
  if (await fixedBottom.isVisible().catch(() => false)) {
    const position = await fixedBottom.evaluate(el => window.getComputedStyle(el).position).catch(() => '');
    if (position !== 'fixed' && position !== 'sticky') {
      addError('production-grade', pageTitle, '操作栏未固定',
        `底部操作栏position=${position}，应为fixed或sticky`);
    }
  }

  // 2.2 组件尺寸检查
  // 按钮高度检查
  const buttons = page.locator('.ant-btn:visible');
  const btnCount = await buttons.count().catch(() => 0);
  for (let i = 0; i < Math.min(btnCount, 5); i++) {
    const btn = buttons.nth(i);
    const height = await btn.evaluate(el => window.getComputedStyle(el).height).catch(() => '');
    const hNum = parseInt(height);
    if (hNum > 32) {
      addError('production-grade', pageTitle, '按钮尺寸过大',
        `按钮高度 ${height}，应 ≤ 28px (small尺寸)`);
      break;
    }
  }

  // 2.3 表格边框检查
  const tables = page.locator('.ant-table:visible');
  if (await tables.count().then(c => c > 0).catch(() => false)) {
    const table = tables.first();
    const hasBorder = await table.evaluate(el => {
      const style = window.getComputedStyle(el);
      return style.border || style.borderWidth;
    }).catch(() => '');
    if (!hasBorder || hasBorder === '0px') {
      addError('production-grade', pageTitle, '表格缺少边框',
        '表格组件缺少清晰四边1px实线边框');
    }
  }

  // 2.4 数值列右对齐
  const numericCells = page.locator('.ant-table-cell:visible');
  const numericCount = await numericCells.count().catch(() => 0);
  for (let i = 0; i < Math.min(numericCount, 10); i++) {
    const cell = numericCells.nth(i);
    const text = await cell.textContent().catch(() => '');
    if (text && /^[\d,.\-￥$]+$/.test(text.trim())) {
      const align = await cell.evaluate(el => window.getComputedStyle(el).textAlign).catch(() => '');
      if (align !== 'right') {
        addError('production-grade', pageTitle, '数值列未右对齐',
          `数值 "${text.trim()}" 对齐方式为 ${align}，应为 right`);
        break;
      }
    }
  }

  // 2.5 加载状态检查
  const spin = page.locator('.ant-spin-spinning:visible');
  const spinTimeout = setTimeout(() => {}, 5000);
  try {
    await spin.waitFor({ state: 'hidden', timeout: 5000 });
  } catch {
    addError('blocking', pageTitle, '页面加载超时',
      '页面Spin加载状态超过5秒未消失');
  }

  // 2.6 空数据状态检查
  const emptyText = page.locator('.ant-empty:visible, .ant-table-empty:visible');
  if (await emptyText.isVisible().catch(() => false)) {
    console.log(`[${pageTitle}] 页面显示空数据状态`);
  }
}

// ==================== 菜单点击导航 ====================
async function navigateToPage(page: Page, pageDef: typeof ALL_PAGES[0]): Promise<boolean> {
  try {
    const { route, title, menu, submenu } = pageDef;

    // 如果是工作台（根路径），直接点击
    if (route === '/dashboard') {
      const dashboardMenu = page.locator('.ant-menu-item:visible').filter({ hasText: '工作台' }).first();
      if (await dashboardMenu.isVisible().catch(() => false)) {
        await dashboardMenu.click();
        await page.waitForTimeout(2000);
        return true;
      }
      // fallback: 直接导航
      await page.goto(`${BASE_URL}${route}`);
      await page.waitForTimeout(2000);
      return true;
    }

    // 展开父级菜单
    if (menu) {
      // 检查父菜单是否已展开
      const parentMenu = page.locator(`.ant-menu-submenu-title:visible`).filter({ hasText: menu }).first();
      if (await parentMenu.isVisible().catch(() => false)) {
        const isOpen = await parentMenu.evaluate(el =>
          el.closest('.ant-menu-submenu')?.classList.contains('ant-menu-submenu-open')
        ).catch(() => false);

        if (!isOpen) {
          await parentMenu.click();
          await page.waitForTimeout(500);
        }
      }
    }

    // 点击子菜单项
    if (submenu) {
      const menuItem = page.locator(`.ant-menu-item:visible`).filter({ hasText: submenu }).first();
      if (await menuItem.isVisible().catch(() => false)) {
        await menuItem.click();
        await page.waitForTimeout(2000);
        return true;
      }
    }

    // fallback: 直接导航
    await page.goto(`${BASE_URL}${route}`);
    await page.waitForTimeout(2000);
    console.warn(`[${title}] 菜单项未找到，通过URL直接导航`);
    return false;
  } catch (e: any) {
    console.error(`[${pageDef.title}] 导航失败:`, e.message);
    // fallback
    try {
      await page.goto(`${BASE_URL}${route}`);
      await page.waitForTimeout(2000);
    } catch {}
    return false;
  }
}

// ==================== 交互式测试 ====================
async function testPageInteractions(page: Page, pageDef: typeof ALL_PAGES[0]) {
  const { title, route } = pageDef;

  // 检查是否存在表格
  const hasTable = await page.locator('.ant-table:visible').count().then(c => c > 0).catch(() => false);

  // 检查是否有操作按钮
  const buttons = page.locator('.ant-btn:visible');
  const btnTexts: string[] = [];
  const btnCount = await buttons.count().catch(() => 0);
  for (let i = 0; i < btnCount; i++) {
    const text = await buttons.nth(i).textContent().catch(() => '');
    if (text?.trim()) btnTexts.push(text.trim());
  }

  console.log(`[${title}] 按钮列表: [${btnTexts.join(', ')}]`);

  // 测试"新增/新建"按钮
  const addBtn = page.locator('.ant-btn:visible').filter({ hasText: /新增|新建|创建|添加/ }).first();
  if (await addBtn.isVisible().catch(() => false)) {
    console.log(`[${title}] 点击新增按钮`);
    await addBtn.click();
    await page.waitForTimeout(2000);

    // 检查是否弹出了详情页或对话框
    const detailModal = page.locator('.ant-modal:visible, .ant-drawer:visible');
    if (await detailModal.count().then(c => c > 0).catch(() => false)) {
      console.log(`[${title}] 新增操作弹出对话框/抽屉`);
      // 关闭
      const closeBtn = page.locator('.ant-modal-close:visible, .ant-drawer-close:visible, .ant-btn:visible').filter({ hasText: /取消|关闭/ }).first();
      if (await closeBtn.isVisible().catch(() => false)) {
        await closeBtn.click();
        await page.waitForTimeout(1000);
      }
    }
  }

  // 测试"查询/搜索"按钮
  const searchBtn = page.locator('.ant-btn:visible').filter({ hasText: /查询|搜索|刷新/ }).first();
  if (await searchBtn.isVisible().catch(() => false)) {
    console.log(`[${title}] 点击查询按钮`);
    await searchBtn.click();
    await page.waitForTimeout(2000);
  }

  // 测试分页
  const pagination = page.locator('.ant-pagination:visible');
  if (await pagination.count().then(c => c > 0).catch(() => false)) {
    const nextBtn = pagination.locator('.ant-pagination-next:not(.ant-pagination-disabled)');
    if (await nextBtn.isVisible().catch(() => false)) {
      console.log(`[${title}] 测试下一页`);
      await nextBtn.click();
      await page.waitForTimeout(2000);
    }
    // 回到第一页
    const firstPage = pagination.locator('.ant-pagination-item-1:visible');
    if (await firstPage.isVisible().catch(() => false)) {
      await firstPage.click();
      await page.waitForTimeout(1000);
    }
  }

  // 检查并报告空数据
  const empty = page.locator('.ant-empty:visible');
  if (await empty.count().then(c => c > 0).catch(() => false)) {
    console.log(`[${title}] 页面显示空数据提示`);
  }
}

// ==================== 测试主体 ====================
test.describe('ERP全页面批处理扫描', () => {
  let browserContext: BrowserContext;

  test.beforeAll(async ({ browser }) => {
    browserContext = await browser.newContext({
      viewport: { width: 1920, height: 1080 },
      locale: 'zh-CN',
    });
  });

  // 每批测试一个页面
  for (let batchIdx = 0; batchIdx < PAGE_BATCHES.length; batchIdx++) {
    const batch = PAGE_BATCHES[batchIdx];

    test.describe(`第 ${batchIdx + 1} 批 (${batch.map(p => p.title).join(', ')})`, () => {

      // 第一个页面负责登录
      if (batchIdx === 0) {
        test('登录系统', async ({ page }) => {
          // 收集控制台错误
          const consoleErrors: string[] = [];
          page.on('console', msg => {
            if (msg.type() === 'error') {
              consoleErrors.push(msg.text());
            }
          });

          // 收集网络请求失败
          page.on('response', response => {
            if (response.status() >= 400) {
              addError('api', '登录', `HTTP ${response.status()}`,
                `${response.url()} returned ${response.status()}`);
            }
          });

          // 导航到登录页
          await page.goto(`${BASE_URL}/login`);
          await page.waitForLoadState('networkidle');
          await page.waitForTimeout(1000);

          // 等待登录表单出现
          await page.waitForSelector('input[placeholder="请输入用户名"]', { timeout: 15000 });

          // 填写租户名称
          const tenantInput = page.locator('input[placeholder="请输入租户名称"]');
          if (await tenantInput.isVisible({ timeout: 3000 }).catch(() => false)) {
            await tenantInput.fill('系统租户');
          }

          // 填写用户名
          await page.fill('input[placeholder="请输入用户名"]', 'admin');

          // 填写密码
          await page.fill('input[placeholder="请输入密码"]', 'admin123');

          // 填写验证码（后端不验证，任意4位即可）
          const captchaInput = page.locator('input[placeholder="请输入验证码"]');
          if (await captchaInput.isVisible({ timeout: 3000 }).catch(() => false)) {
            await captchaInput.fill('ABCD');
          }

          // 点击登录
          await page.click('button[type="submit"]');

          // 等待跳转到dashboard
          try {
            await page.waitForURL('**/dashboard**', { timeout: 20000 });
          } catch {
            const screenshot = await page.screenshot();
            const errorMsg = await page.locator('.ant-message-notice-content').textContent().catch(() => '未知错误');
            test.fail(true, `登录失败: ${errorMsg}`);
          }

          // 等待布局渲染
          await page.waitForSelector('.ant-layout, .basic-layout, .ant-pro-layout', { timeout: 15000 });
          await page.waitForTimeout(2000);

          // 检查是否有控制台错误
          if (consoleErrors.length > 0) {
            console.warn(`登录页控制台错误: ${consoleErrors.join(' | ')}`);
          }

          console.log('登录成功！');
        });
      } else {
        // 非第一批：确保已登录
        test.beforeEach(async ({ page }) => {
          // 检查是否在登录页
          const currentUrl = page.url();
          if (currentUrl.includes('/login')) {
            // 已登出，重新登录
            await page.goto(`${BASE_URL}/dashboard`);
            await page.waitForTimeout(2000);
          }
        });
      }

      // 测试本批次每个页面
      for (const pageDef of batch) {
        test(`测试 ${pageDef.title} (${pageDef.route})`, async ({ page }) => {
          pageIndex++;
          const consoleErrors: string[] = [];
          let hasVisualError = false;

          // 监听控制台错误
          page.on('console', msg => {
            if (msg.type() === 'error') {
              consoleErrors.push(msg.text());
            }
          });

          // 监听网络错误
          const networkErrors: { url: string; status: number }[] = [];
          page.on('response', response => {
            if (response.status() >= 400) {
              networkErrors.push({ url: response.url(), status: response.status() });
            }
          });

          // 监听页面错误
          page.on('pageerror', error => {
            hasVisualError = true;
            addError('blocking', pageDef.title, '页面JS错误', error.message, error.stack);
          });

          // 通过菜单导航到页面
          const navigated = await navigateToPage(page, pageDef);

          // 等待页面渲染
          await page.waitForLoadState('networkidle');
          await page.waitForTimeout(3000);

          // 检查页面是否正常渲染
          const bodyText = await page.locator('body').textContent().catch(() => '');
          const isWhiteScreen = bodyText?.trim().length === 0;
          if (isWhiteScreen) {
            addError('blocking', pageDef.title, '页面白屏', '页面内容为空');
          }

          // 检查是否有ant-design-vue的错误提示
          const errorAlert = page.locator('.ant-alert-error:visible, .ant-result-error:visible');
          if (await errorAlert.count().then(c => c > 0).catch(() => false)) {
            const errText = await errorAlert.first().textContent().catch(() => '');
            addError('blocking', pageDef.title, '页面错误提示', errText || '页面显示错误提示');
          }

          // 记录控制台错误
          for (const err of consoleErrors) {
            addError('blocking', pageDef.title, '控制台错误', err);
          }

          // 记录网络错误
          for (const ne of networkErrors) {
            addError('api', pageDef.title, `HTTP ${ne.status}`, ne.url);
          }

          // 检查生产级标准
          await checkDetailPageStandards(page, pageDef.title);

          // 交互测试
          await testPageInteractions(page, pageDef);

          // 检查加载中的Spin是否消失了
          const spinning = page.locator('.ant-spin-spinning');
          try {
            await spinning.waitFor({ state: 'hidden', timeout: 5000 });
          } catch {
            addError('blocking', pageDef.title, '页面持续loading', 'Spin超过5秒未消失');
          }

          // 当前URL
          console.log(`[${pageDef.title}] 当前URL: ${page.url()}`);
        });
      }
    });
  }

  test.afterAll(async () => {
    // 生成报告
    const reportPath = path.resolve(__dirname, '../../scan-results/scan-report.json');
    fs.mkdirSync(path.dirname(reportPath), { recursive: true });

    const report = {
      timestamp: new Date().toISOString(),
      totalPages: ALL_PAGES.length,
      totalErrors: allErrors.length,
      errorsByType: {} as Record<string, number>,
      errors: allErrors,
    };

    for (const err of allErrors) {
      report.errorsByType[err.type] = (report.errorsByType[err.type] || 0) + 1;
    }

    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));

    console.log(`\n========== 扫描报告 ==========`);
    console.log(`总计页面: ${ALL_PAGES.length}`);
    console.log(`总计错误: ${allErrors.length}`);
    for (const [type, count] of Object.entries(report.errorsByType)) {
      console.log(`  ${type}: ${count}`);
    }
    console.log(`报告已保存: ${reportPath}`);
  });
});
