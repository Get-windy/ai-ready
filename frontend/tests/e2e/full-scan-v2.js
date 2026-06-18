/**
 * ERP 全页面自动扫描 v2
 *
 * 修复：
 * 1. 使用 page.evaluate 调用 Vue Router 导航（避免全页刷新）
 * 2. 点击前按 Escape 关闭 tooltip
 * 3. 使用 { force: true } 处理遮挡
 * 4. 更好的按钮匹配（排除全局搜索按钮）
 * 5. 更短的超时时间
 */
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const BASE_URL = 'http://localhost:5656';
const SCREENSHOT_DIR = path.resolve(__dirname, '../../screenshots/scan-v2');
const REPORT_DIR = path.resolve(__dirname, '../../scan-results');

const allErrors = [];
let currentPage = '';

function addError(type, page, title, message, detail) {
  allErrors.push({ type, page, title, message, detail: detail || '', timestamp: new Date().toISOString() });
}

// 页面清单
const MENU_PATHS = {
  '工作台': '/dashboard',
  '销售管理>销售订单': '/sale',
  '销售管理>销售出库': '/stock',
  '销售管理>发货管理': '/erp/shipment',
  '销售管理>退货处理': '/erp/return',
  '销售管理>销售分析': '/erp/sales-analysis',
  '销售管理>销售报表': '/erp/sales-report',
  '采购管理>采购订单': '/purchase',
  '采购管理>入库管理': '/erp/stock-in',
  '采购管理>采购退货': '/erp/purchase-return',
  '采购管理>采购换货': '/erp/purchase-exchange',
  '库存管理>库存查询': '/erp/stock',
  '库存管理>库存盘点': '/erp/stocktake',
  '库存管理>库存调拨': '/erp/stock-transfer',
  '库存管理>批次管理': '/erp/batch',
  '库存管理>序列号管理': '/erp/serial',
  '客户管理>线索管理': '/crm/lead',
  '客户管理>商机管理': '/crm/opportunity',
  '客户管理>客户档案': '/crm/customer',
  '客户管理>合同管理': '/crm/contract',
  '客户管理>报价管理': '/crm/quotation',
  '客户管理>发票管理': '/crm/invoice',
  '产品管理>产品管理': '/erp/product',
  '产品管理>往来单位': '/erp/partner',
  '产品管理>定价管理': '/erp/pricing',
  '财务管理>科目管理': '/finance/subject',
  '财务管理>凭证管理': '/finance/voucher',
  '财务管理>应收账款': '/finance/accounts-receivable',
  '财务管理>应付账款': '/finance/accounts-payable',
  '财务管理>收款单管理': '/finance/receipt',
  '财务管理>付款单管理': '/finance/payment',
  '财务管理>财务报表': '/finance/report',
  '费用管理>费用申请': '/erp/expense/application',
  '费用管理>费用报销': '/erp/expense/reimbursement',
  '费用管理>费用审批': '/erp/expense/approval',
  '费用管理>费用付款': '/erp/expense/payment',
  '费用管理>费用统计': '/erp/expense/statistics',
  '资产管理>资产台账': '/fixed-asset/asset',
  '资产管理>资产分类': '/fixed-asset/category',
  '资产管理>折旧管理': '/fixed-asset/depreciation',
  '资产管理>资产调拨': '/fixed-asset/transfer',
  '资产管理>资产处置': '/fixed-asset/disposal',
  '资产管理>资产盘点': '/fixed-asset/inventory',
  '资产管理>资产报表': '/fixed-asset/report',
  '预算管理>预算概览': '/budget',
  '预算管理>预算模板': '/budget/template',
  '预算管理>年度预算': '/budget/annual',
  '预算管理>预算调整': '/budget/adjustment',
  '预算管理>预算报表': '/budget/report',
  '供应商管理>供应商管理': '/supplier',
  '供应商管理>供应商询价': '/supplier/inquiry',
  '供应商管理>供应商绩效': '/supplier/performance',
  '系统管理>用户管理': '/system/user',
  '系统管理>角色管理': '/system/role',
  '系统管理>菜单管理': '/system/menu',
  '系统管理>租户管理': '/system/tenant',
  '系统管理>字典管理': '/system/dict',
  '系统管理>系统配置': '/system/config',
  '系统管理>系统日志': '/system/log',
  '系统管理>通知公告': '/notification',
  '产品管理>定价审批': '/erp/pricing/approval',
  '产品管理>价格层级': '/erp/pricing/tiers',
  '系统管理>组织架构': '/system/department',
  '系统管理>岗位管理': '/system/position',
  '系统管理>权限管理': '/system/permission',
  '系统管理>租户审批': '/system/tenant-approval',
  '系统管理>数据导入': '/system/data-import',
  '工作流>流程监控': '/workflow/instance-monitor',
  '工作流>任务管理': '/workflow/task-management',
  '工作流>流程分析': '/workflow/process-analysis',
};

function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

/**
 * 使用 Vue Router 导航到页面（避免全页刷新导致 SSO 中断）
 */
async function navigateTo(page, route) {
  try {
    await page.evaluate((r) => {
      const app = document.querySelector('#app');
      if (app && app.__vue_app__) {
        const router = app.__vue_app__.config.globalProperties.$router;
        if (router) {
          router.push(r);
          return true;
        }
      }
      return false;
    }, route);
    await sleep(2000);
    return true;
  } catch (e) {
    // fallback: window.location
    try {
      await page.goto(`${BASE_URL}${route}`, { waitUntil: 'domcontentloaded', timeout: 15000 });
      await sleep(2000);
      return true;
    } catch (e2) {
      return false;
    }
  }
}

/**
 * 通过点击侧边栏菜单导航
 */
async function navigateByMenu(page, menuPath) {
  const parts = menuPath.split('>');
  const isTopLevel = parts.length === 1;

  // 先按 Escape 关闭可能的 tooltip
  await page.keyboard.press('Escape');
  await sleep(300);

  if (isTopLevel) {
    // 一级菜单
    const items = page.locator('.ant-menu-item');
    const count = await items.count();
    for (let i = 0; i < count; i++) {
      const text = await items.nth(i).textContent().catch(() => '');
      if (text && text.includes(parts[0])) {
        await items.nth(i).click({ force: true, timeout: 5000 }).catch(() => {});
        return true;
      }
    }
    return false;
  }

  // 二级菜单
  const [parentName, childName] = parts;

  // 打开父菜单
  const submenus = page.locator('.ant-menu-submenu-title');
  const subCount = await submenus.count();
  for (let i = 0; i < subCount; i++) {
    const text = await submenus.nth(i).textContent().catch(() => '');
    if (text && text.includes(parentName)) {
      // 如果未展开则点击
      const isOpen = await submenus.nth(i).evaluate(el =>
        el.closest('.ant-menu-submenu')?.classList.contains('ant-menu-submenu-open')
      ).catch(() => false);
      if (!isOpen) {
        await submenus.nth(i).click({ force: true, timeout: 5000 }).catch(() => {});
        await sleep(500);
      }
      break;
    }
  }

  // 点击子菜单
  await sleep(300);
  const menuItems = page.locator('.ant-menu-item');
  const itemCount = await menuItems.count();
  for (let i = 0; i < itemCount; i++) {
    const text = await menuItems.nth(i).textContent().catch(() => '');
    if (text && text.includes(childName)) {
      await menuItems.nth(i).click({ force: true, timeout: 5000 }).catch(() => {});
      return true;
    }
  }
  return false;
}

/**
 * 检查当前页面状态和错误
 */
async function checkPage(page, pageName) {
  const errors = [];

  // 检查白屏
  const bodyText = (await page.locator('body').textContent().catch(() => '')) || '';
  if (bodyText.trim().length < 10) {
    addError('blocking', pageName, '页面白屏/内容为空', bodyText.slice(0, 100));
    return;
  }

  // 检查错误组件
  const errAlert = page.locator('.ant-alert-error:visible, .ant-result-error:visible');
  if (await errAlert.count().then(c => c > 0).catch(() => false)) {
    addError('blocking', pageName, '页面错误组件', (await errAlert.first().textContent().catch(() => '')) || '');
  }

  // 检查持续加载
  const spin = page.locator('.ant-spin-spinning:visible');
  try {
    await spin.waitFor({ state: 'hidden', timeout: 3000 });
  } catch {
    addError('blocking', pageName, '页面持续加载中', 'Spin超过3秒未消失');
  }

  // 检查表格边框
  const table = page.locator('.ant-table:visible').first();
  if (await table.isVisible().catch(() => false)) {
    const borderWidth = await table.evaluate(el => {
      const s = window.getComputedStyle(el);
      return s.borderLeftWidth + ' ' + s.borderRightWidth + ' ' + s.borderTopWidth + ' ' + s.borderBottomWidth;
    }).catch(() => '');
    if (borderWidth === '0px 0px 0px 0px' || borderWidth === '') {
      addError('production-grade', pageName, '表格缺少边框', 'ant-table border=0');
    }
  }

  // 检查登录状态
  const currentUrl = page.url();
  if (currentUrl.includes('/login')) {
    addError('blocking', pageName, '会话已过期，跳转回登录页', `当前URL: ${currentUrl}`);
  }
}

/**
 * 测试页面交互
 */
async function testInteractions(page, pageName) {
  // 按 Escape 清理 tooltip
  await page.keyboard.press('Escape');
  await sleep(300);

  // 查找"新增/新建"按钮（排除全局搜索）
  const buttons = page.locator('.ant-btn:not(.search-trigger):visible');
  const btnCount = await buttons.count().catch(() => 0);

  // 找新增按钮
  for (let i = 0; i < btnCount; i++) {
    const text = (await buttons.nth(i).textContent().catch(() => '')) || '';
    if (/新增|新建/.test(text)) {
      console.log(`  [${pageName}] 点击[${text.trim()}]`);
      await buttons.nth(i).click({ force: true, timeout: 5000 }).catch(() => {});
      await sleep(2000);

      // 如果弹出 Modal 则关闭
      const modal = page.locator('.ant-modal-wrap:visible');
      if (await modal.count().then(c => c > 0).catch(() => false)) {
        addError('production-grade', pageName, '新增使用Modal弹窗', '应使用全屏详情页');
        await page.keyboard.press('Escape');
        await sleep(500);
      }
      break;
    }
  }

  // 找查询按钮（排除全局搜索）
  for (let i = 0; i < btnCount; i++) {
    const text = (await buttons.nth(i).textContent().catch(() => '')) || '';
    if (/查询|搜索/.test(text) && !text.includes('全局')) {
      console.log(`  [${pageName}] 点击[${text.trim()}]`);
      await buttons.nth(i).click({ force: true, timeout: 5000 }).catch(() => {});
      await sleep(1500);
      break;
    }
  }
}

async function main() {
  console.log('ERP 全页面扫描 v2\n');

  fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
  fs.mkdirSync(REPORT_DIR, { recursive: true });

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
  });
  const page = await context.newPage();

  // 收集错误
  page.on('console', msg => {
    if (msg.type() === 'error') {
      addError('interaction', currentPage, '控制台错误', msg.text().slice(0, 300));
    }
  });
  page.on('response', response => {
    if (response.status() >= 400 && response.status() < 600) {
      const url = response.url();
      if (!url.includes('/sse/') && !url.includes('/notification/')) {
        addError('api', currentPage, `HTTP ${response.status()}`, url.replace(BASE_URL, ''));
      }
    }
  });
  page.on('pageerror', error => {
    addError('blocking', currentPage, '页面JS异常', error.message || '');
  });

  try {
    // ===== 登录 =====
    console.log('登录系统...');
    await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded' });
    await sleep(2000);

    const tInput = page.locator('input[placeholder="请输入租户名称"]');
    if (await tInput.isVisible({ timeout: 3000 }).catch(() => false))
      await tInput.fill('系统租户');
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    const cInput = page.locator('input[placeholder="请输入验证码"]');
    if (await cInput.isVisible({ timeout: 2000 }).catch(() => false))
      await cInput.fill('ABCD');
    await page.click('button[type="submit"]');

    try {
      await page.waitForURL('**/dashboard**', { timeout: 20000 });
    } catch {
      console.error('登录失败');
      await page.screenshot({ path: `${SCREENSHOT_DIR}/00-login-failed.png` });
      return;
    }
    await sleep(3000);
    console.log('  ✓ 登录成功\n');

    // ===== 扫描页面 =====
    const entries = Object.entries(MENU_PATHS);
    console.log(`扫描 ${entries.length} 个页面\n`);

    let success = 0;
    let pageIdx = 0;

    // 先扫描 dashboard
    await navigateTo(page, '/dashboard');
    await sleep(2000);
    currentPage = '工作台';

    for (const [menuPath, route] of entries) {
      pageIdx++;
      currentPage = menuPath;
      console.log(`[${pageIdx}/${entries.length}] ${menuPath}`);

      try {
        // 重置：关闭可能打开的弹窗
        await page.keyboard.press('Escape');
        await sleep(300);

        // 导航到页面（优先用 router，回退 goto）
        await navigateTo(page, route);
        await sleep(2000);

        // 检查页面
        await checkPage(page, menuPath);

        // 交互测试
        await testInteractions(page, menuPath);

        // 截屏
        const safeName = menuPath.replace(/[\\/> ]/g, '_').slice(0, 50);
        await page.screenshot({
          path: `${SCREENSHOT_DIR}/${String(pageIdx).padStart(2, '0')}-${safeName}.png`,
          fullPage: true
        }).catch(() => {});

        success++;
      } catch (e) {
        addError('blocking', menuPath, '扫描异常', e.message || '');
        console.log(`  ✗ ${e.message}`);
      }
    }

    // ===== 报告 =====
    console.log(`\n扫描完成: ${success}/${entries.length}, 错误: ${allErrors.length}`);

    const byType = {};
    for (const e of allErrors) {
      byType[e.type] = (byType[e.type] || 0) + 1;
    }
    console.log('\n错误分类:');
    for (const [t, c] of Object.entries(byType).sort((a, b) => b[1] - a[1]))
      console.log(`  ${t}: ${c}`);

    if (allErrors.length > 0) {
      console.log('\n错误列表:');
      allErrors.forEach(e => console.log(`  [${e.type}] ${e.page}: ${e.title}`));
    }

    // 保存报告
    fs.writeFileSync(`${REPORT_DIR}/scan-v2-report.json`, JSON.stringify({
      timestamp: new Date().toISOString(),
      totalPages: entries.length,
      successCount: success,
      failCount: entries.length - success,
      totalErrors: allErrors.length,
      errorsByType: byType,
      errors: allErrors,
    }, null, 2));

    console.log(`\n报告: ${REPORT_DIR}/scan-v2-report.json`);
    console.log(`截图: ${SCREENSHOT_DIR}/`);

  } catch (e) {
    console.error('FATAL:', e);
  } finally {
    await browser.close();
  }
}

main();
