/**
 * ERP 全页面自动扫描 v3
 *
 * 改进：
 * - 每页完成后确保关闭所有弹窗/抽屉/Modal
 * - 更完善的错误捕获（每页独立 try-catch）
 * - 逐步截屏帮助调试
 * - 超时保护
 */
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const BASE_URL = 'http://localhost:5656';
const SCREENSHOT_DIR = path.resolve(__dirname, '../../screenshots/scan-v3');
const REPORT_DIR = path.resolve(__dirname, '../../scan-results');

const allErrors = [];
let currentPage = '';

function addError(type, page, title, msg, detail) {
  allErrors.push({ type, page, title, message: msg || '', detail: detail || '', ts: new Date().toISOString() });
}

const PAGES = [
  { name: '工作台', route: '/dashboard' },
  { name: '销售订单', route: '/sale' },
  { name: '销售出库', route: '/stock' },
  { name: '发货管理', route: '/erp/shipment' },
  { name: '退货处理', route: '/erp/return' },
  { name: '销售分析', route: '/erp/sales-analysis' },
  { name: '销售报表', route: '/erp/sales-report' },
  { name: '采购订单', route: '/purchase' },
  { name: '入库管理', route: '/erp/stock-in' },
  { name: '采购退货', route: '/erp/purchase-return' },
  { name: '采购换货', route: '/erp/purchase-exchange' },
  { name: '库存管理', route: '/erp/stock' },
  { name: '库存盘点', route: '/erp/stocktake' },
  { name: '库存调拨', route: '/erp/stock-transfer' },
  { name: '批次管理', route: '/erp/batch' },
  { name: '序列号管理', route: '/erp/serial' },
  { name: '线索管理', route: '/crm/lead' },
  { name: '商机管理', route: '/crm/opportunity' },
  { name: '客户档案', route: '/crm/customer' },
  { name: '合同管理', route: '/crm/contract' },
  { name: '报价管理', route: '/crm/quotation' },
  { name: '发票管理', route: '/crm/invoice' },
  { name: '产品管理', route: '/erp/product' },
  { name: '往来单位', route: '/erp/partner' },
  { name: '定价管理', route: '/erp/pricing' },
  { name: '科目管理', route: '/finance/subject' },
  { name: '凭证管理', route: '/finance/voucher' },
  { name: '应收账款', route: '/finance/accounts-receivable' },
  { name: '应付账款', route: '/finance/accounts-payable' },
  { name: '收款单管理', route: '/finance/receipt' },
  { name: '付款单管理', route: '/finance/payment' },
  { name: '财务报表', route: '/finance/report' },
  { name: '费用申请', route: '/erp/expense/application' },
  { name: '费用报销', route: '/erp/expense/reimbursement' },
  { name: '费用审批', route: '/erp/expense/approval' },
  { name: '费用付款', route: '/erp/expense/payment' },
  { name: '费用统计', route: '/erp/expense/statistics' },
  { name: '资产台账', route: '/fixed-asset/asset' },
  { name: '资产分类', route: '/fixed-asset/category' },
  { name: '折旧管理', route: '/fixed-asset/depreciation' },
  { name: '资产调拨', route: '/fixed-asset/transfer' },
  { name: '资产处置', route: '/fixed-asset/disposal' },
  { name: '资产盘点', route: '/fixed-asset/inventory' },
  { name: '资产报表', route: '/fixed-asset/report' },
  { name: '预算管理', route: '/budget' },
  { name: '供应商管理', route: '/supplier' },
  { name: '供应商询价', route: '/supplier/inquiry' },
  { name: '供应商绩效', route: '/supplier/performance' },
  { name: '用户管理', route: '/system/user' },
  { name: '角色管理', route: '/system/role' },
  { name: '菜单管理', route: '/system/menu' },
  { name: '租户管理', route: '/system/tenant' },
  { name: '字典管理', route: '/system/dict' },
  { name: '系统配置', route: '/system/config' },
  { name: '系统日志', route: '/system/log' },
  { name: '通知公告', route: '/notification' },
  { name: '定价审批', route: '/erp/pricing/approval' },
  { name: '价格层级', route: '/erp/pricing/tiers' },
  { name: '组织架构', route: '/system/department' },
  { name: '岗位管理', route: '/system/position' },
  { name: '权限管理', route: '/system/permission' },
  { name: '租户审批', route: '/system/tenant-approval' },
  { name: '数据导入', route: '/system/data-import' },
  { name: '流程监控', route: '/workflow/instance-monitor' },
  { name: '任务管理', route: '/workflow/task-management' },
  { name: '流程分析', route: '/workflow/process-analysis' },
  { name: '打印模板', route: '/printing/template' },
  { name: '打印链路', route: '/printing/chain' },
];

function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

/**
 * 路由导航
 */
async function navigateTo(page, route) {
  try {
    await page.evaluate((r) => {
      const appEl = document.querySelector('#app');
      if (appEl && appEl.__vue_app__) {
        const router = appEl.__vue_app__.config.globalProperties.$router;
        if (router) { router.push(r); return true; }
      }
      return false;
    }, route);
    await sleep(2000);
    return true;
  } catch (e) {
    try {
      await page.goto(`${BASE_URL}${route}`, { waitUntil: 'domcontentloaded', timeout: 15000 });
      await sleep(2000);
      return true;
    } catch (e2) { return false; }
  }
}

/**
 * 关闭所有弹窗
 */
async function closeAllModals(page) {
  // Escape 键关闭弹窗
  for (let i = 0; i < 3; i++) {
    await page.keyboard.press('Escape');
    await sleep(500);
  }

  // 点击 Modal 关闭按钮
  const closeBtns = page.locator('.ant-modal-close:visible, .ant-drawer-close:visible');
  const cCount = await closeBtns.count().catch(() => 0);
  for (let i = 0; i < cCount; i++) {
    await closeBtns.nth(0).click({ force: true, timeout: 3000 }).catch(() => {});
    await sleep(300);
  }

  // 点击取消按钮
  const cancelBtns = page.locator('.ant-btn:visible').filter({ hasText: /取消|关闭/ });
  const cancelCount = await cancelBtns.count().catch(() => 0);
  for (let i = 0; i < cancelCount; i++) {
    await cancelBtns.nth(0).click({ force: true, timeout: 3000 }).catch(() => {});
    await sleep(300);
  }
}

/**
 * 检查页面状态
 */
async function checkPage(page, pageName) {
  const body = (await page.locator('body').textContent().catch(() => '')) || '';
  if (body.trim().length < 10) {
    addError('blocking', pageName, '页面白屏', 'body内容极少');
    return false;
  }

  // 检查错误提示
  const errAlert = page.locator('.ant-alert-error:visible, .ant-result-error:visible, .ant-result-warning:visible');
  if (await errAlert.count().then(c => c > 0).catch(() => false)) {
    const txt = (await errAlert.first().textContent().catch(() => '')) || '';
    addError('blocking', pageName, '页面错误组件', txt.slice(0, 200));
  }

  // 检查加载状态
  const spin = page.locator('.ant-spin-spinning:visible');
  try {
    await spin.waitFor({ state: 'hidden', timeout: 5000 });
  } catch {
    addError('blocking', pageName, '页面持续加载', 'Spin超时');
  }

  // 检查是否已登出
  if (page.url().includes('/login')) {
    addError('blocking', pageName, '会话过期', '跳转到登录页');
    return false;
  }

  return true;
}

/**
 * 测试交互
 */
async function testInteractions(page, pageName) {
  await page.keyboard.press('Escape');
  await sleep(300);

  // 查找新增/新建按钮（排除全局搜索等）
  const btns = page.locator('.ant-btn:visible');
  const n = await btns.count().catch(() => 0);

  let clicked = false;
  for (let i = 0; i < n; i++) {
    const txt = ((await btns.nth(i).textContent().catch(() => '')) || '').trim();
    if (!txt) continue;
    // 匹配新增/新建，排除导航类按钮
    if (/^(新增|新建|创建|添加|增加)/.test(txt)) {
      console.log(`  [交互] 点击"${txt}"`);
      await btns.nth(i).click({ force: true, timeout: 5000 }).catch(() => {});
      await sleep(2000);

      // 检查是否弹出 Modal
      const modalWrap = page.locator('.ant-modal-wrap:visible');
      if (await modalWrap.count().then(c => c > 0).catch(() => false)) {
        addError('production-grade', pageName, '新增使用Modal', `点击"${txt}"后弹出Modal`);
        // 关闭
        await page.keyboard.press('Escape');
        await sleep(500);
      }
      clicked = true;
      break;
    }
  }

  if (!clicked) {
    // 寻找包含"新增"或"新建"的按钮（如"新建订单"）
    for (let i = 0; i < n; i++) {
      const txt = ((await btns.nth(i).textContent().catch(() => '')) || '').trim();
      if (!txt || txt === '查询' || txt === '重置' || txt === '刷新') continue;
      if (txt.includes('新增') || txt.includes('新建')) {
        console.log(`  [交互] 点击"${txt}"`);
        await btns.nth(i).click({ force: true, timeout: 5000 }).catch(() => {});
        await sleep(2000);
        break;
      }
    }
  }

  // 关闭可能打开的弹窗
  await closeAllModals(page);
}

async function main() {
  console.log('ERP 全页面扫描 v3\n');

  fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
  fs.mkdirSync(REPORT_DIR, { recursive: true });

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const page = await context.newPage();

  // 错误收集
  page.on('console', msg => {
    if (msg.type() === 'error')
      addError('console', currentPage, '控制台错误', msg.text().slice(0, 300));
  });
  page.on('response', resp => {
    if (resp.status() >= 400) {
      const u = resp.url();
      if (!u.includes('/sse/') && !u.includes('/notification/'))
        addError('api', currentPage, `HTTP ${resp.status()}`, u.replace(BASE_URL, ''));
    }
  });
  page.on('pageerror', err => {
    addError('blocking', currentPage, 'JS异常', err.message || '');
  });

  try {
    // 登录
    console.log('登录...');
    await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded' });
    await sleep(2000);

    if (await page.locator('input[placeholder="请输入租户名称"]').isVisible({ timeout: 3000 }).catch(() => false))
      await page.fill('input[placeholder="请输入租户名称"]', '系统租户');
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    if (await page.locator('input[placeholder="请输入验证码"]').isVisible({ timeout: 2000 }).catch(() => false))
      await page.fill('input[placeholder="请输入验证码"]', 'ABCD');
    await page.click('button[type="submit"]');

    try { await page.waitForURL('**/dashboard**', { timeout: 20000 }); }
    catch { console.error('登录失败!'); return; }
    await sleep(3000);
    console.log('  OK\n');

    // 扫描各页
    let ok = 0, fail = 0;
    for (let idx = 0; idx < PAGES.length; idx++) {
      const { name, route } = PAGES[idx];
      currentPage = name;
      console.log(`[${idx+1}/${PAGES.length}] ${name} (${route})`);

      try {
        // 先关闭可能的残余弹窗
        await closeAllModals(page);

        // 导航
        await navigateTo(page, route);
        await sleep(2000);

        // 检查
        const valid = await checkPage(page, name);
        if (!valid) fail++;

        // 交互
        await testInteractions(page, name);

        // 截图
        const safeName = name.replace(/[/\\]/g, '_');
        await page.screenshot({
          path: `${SCREENSHOT_DIR}/${String(idx+1).padStart(2,'0')}-${safeName}.png`,
          fullPage: true
        }).catch(() => {});

        ok++;
      } catch (e) {
        fail++;
        addError('blocking', name, '页面扫描异常', e.message || '');
        console.log(`  ✗ ${e.message}`);
      }
    }

    // 报告
    const byType = {};
    for (const e of allErrors) byType[e.type] = (byType[e.type] || 0) + 1;

    console.log(`\n=== 完成: ${ok} OK, ${fail} Fail, ${allErrors.length} Errors ===`);
    for (const [t, c] of Object.entries(byType).sort((a, b) => b[1] - a[1]))
      console.log(`  ${t}: ${c}`);

    fs.writeFileSync(`${REPORT_DIR}/scan-v3-report.json`, JSON.stringify({
      ts: new Date().toISOString(), total: PAGES.length, ok, fail,
      errors: allErrors, byType
    }, null, 2));

    // 报告详情
    if (allErrors.length > 0) {
      console.log('\n所有错误:');
      for (const e of allErrors)
        console.log(`  [${e.type}] ${e.page}: ${e.title} ${e.message ? '- ' + e.message.slice(0, 100) : ''}`);
    }

    console.log(`\n报告: ${REPORT_DIR}/scan-v3-report.json`);
  } catch (e) {
    console.error('FATAL:', e);
  } finally {
    await browser.close();
  }
}

main();
