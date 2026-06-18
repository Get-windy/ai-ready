/**
 * 全页面自动扫描 - 通过菜单点击导航
 *
 * 策略：登录后获取所有菜单项，依次点击每个页面
 * 收集所有错误（控制台、网络、视觉）并生成报告
 */
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const BASE_URL = 'http://localhost:5656';
const SCREENSHOT_DIR = path.resolve(__dirname, '../../screenshots/scan');
const REPORT_DIR = path.resolve(__dirname, '../../scan-results');

// 错误收集
const allErrors = [];
let currentPage = '';

function addError(type, page, title, message, detail) {
  allErrors.push({ type, page, title, message, detail: detail || '', timestamp: new Date().toISOString() });
}

// 页面定义：菜单路径 → 路由
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
  '库存管理>库存成本调整': '/erp/stock-cost-adjust',
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
  '产品管理>定价审批': '/erp/pricing/approval',
  '产品管理>价格层级': '/erp/pricing/tiers',
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
  '系统管理>组织架构': '/system/department',
  '系统管理>岗位管理': '/system/position',
  '系统管理>用户管理': '/system/user',
  '系统管理>角色管理': '/system/role',
  '系统管理>权限管理': '/system/permission',
  '系统管理>菜单管理': '/system/menu',
  '系统管理>租户管理': '/system/tenant',
  '系统管理>租户审批': '/system/tenant-approval',
  '系统管理>字典管理': '/system/dict',
  '系统管理>系统配置': '/system/config',
  '系统管理>系统日志': '/system/log',
  '系统管理>数据导入': '/system/data-import',
  '系统管理>通知公告': '/notification',
  '工作流>流程监控': '/workflow/instance-monitor',
  '工作流>任务管理': '/workflow/task-management',
  '工作流>流程分析': '/workflow/process-analysis',
  '打印管理>打印模板': '/printing/template',
  '打印管理>打印链路': '/printing/chain',
  '打印管理>打印客户端': '/printing/client',
  '打印管理>打印任务': '/printing/task',
};

async function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

async function navigateByMenu(page, menuPath) {
  const parts = menuPath.split('>');

  if (parts.length === 1) {
    // 一级菜单（如工作台）
    const menuItem = page.locator('.ant-menu-item:visible').filter({ hasText: parts[0] }).first();
    if (await menuItem.isVisible({ timeout: 3000 }).catch(() => false)) {
      await menuItem.click();
      return true;
    }
  } else {
    // 二级菜单
    const parentName = parts[0];
    const childName = parts[1];

    // 查找并展开父菜单
    const submenu = page.locator('.ant-menu-submenu-title:visible').filter({ hasText: parentName }).first();
    if (await submenu.isVisible({ timeout: 3000 }).catch(() => false)) {
      // 检查是否已展开
      const isOpen = await submenu.evaluate(el =>
        el.closest('.ant-menu-submenu')?.classList.contains('ant-menu-submenu-open')
      ).catch(() => false);

      if (!isOpen) {
        await submenu.click();
        await sleep(500);
      }

      // 点击子菜单
      const childItem = page.locator('.ant-menu-item:visible').filter({ hasText: childName }).first();
      if (await childItem.isVisible({ timeout: 3000 }).catch(() => false)) {
        await childItem.click();
        return true;
      }
    }
  }
  return false;
}

async function checkPageStandards(page, pageName) {
  // 检查页面内容
  const bodyText = (await page.locator('body').textContent().catch(() => '')) || '';
  if (bodyText.trim().length < 10) {
    addError('blocking', pageName, '页面白屏或内容为空', `页面内容仅 ${bodyText.trim().length} 字符`);
    return;
  }

  // 检查JS错误
  // 检查ant-design-vue错误提示
  const errAlert = page.locator('.ant-alert-error:visible, .ant-result-error:visible');
  if (await errAlert.count().then(c => c > 0).catch(() => false)) {
    const errText = await errAlert.first().textContent().catch(() => '');
    addError('blocking', pageName, '页面错误组件', errText || '');
  }

  // 检查是否有Spin持续旋转（超过5秒）
  const spin = page.locator('.ant-spin-spinning:visible');
  if (await spin.count().then(c => c > 0).catch(() => false)) {
    try {
      await spin.waitFor({ state: 'hidden', timeout: 5000 });
    } catch {
      addError('blocking', pageName, '页面持续加载', 'Spin超过5秒未消失');
    }
  }

  // 生产级检查：表格边框
  const table = page.locator('.ant-table:visible').first();
  if (await table.isVisible().catch(() => false)) {
    const hasBorder = await table.evaluate(el => {
      const s = window.getComputedStyle(el);
      return s.border || s.borderWidth;
    }).catch(() => '');
    if (!hasBorder || hasBorder === '0px' || hasBorder === '0px none') {
      addError('production-grade', pageName, '表格缺少四边边框',
        'ant-table 没有可见的border样式');
    }
  }

  // 生产级检查：按钮尺寸
  const buttons = page.locator('.ant-btn:visible');
  const btnCount = await buttons.count().catch(() => 0);
  for (let i = 0; i < Math.min(btnCount, 3); i++) {
    const h = await buttons.nth(i).evaluate(el => window.getComputedStyle(el).height).catch(() => '');
    const hNum = parseInt(h);
    if (hNum > 28 && hNum < 40) {
      addError('production-grade', pageName, `按钮尺寸过大(${h})`,
        '操作按钮应使用small尺寸(≤28px)');
      break;
    }
  }

  // 生产级检查：表格空数据补齐
  const emptyRow = page.locator('.ant-table-placeholder:visible');
  if (await emptyRow.isVisible().catch(() => false)) {
    console.log(`  [${pageName}] 表格显示空数据占位行`);
  }
}

async function testPageInteractions(page, pageName, route) {
  // 查找"新增/新建"按钮
  const addBtn = page.locator('.ant-btn:visible').filter({ hasText: /新增|新建/ }).first();
  if (await addBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
    console.log(`  [${pageName}] 测试新增按钮`);
    await addBtn.click();
    await sleep(2000);

    // 检查弹出了什么
    const modal = page.locator('.ant-modal:visible');
    const drawer = page.locator('.ant-drawer:visible');

    if (await modal.count().then(c => c > 0).catch(() => false)) {
      addError('production-grade', pageName, '详情页使用Modal弹窗',
        `点击"新增"后弹出Modal而非全屏详情页`);
      // 关闭Modal
      const closeBtn = modal.locator('.ant-modal-close, .ant-btn:visible').filter({ hasText: /取消|关闭/ }).first();
      if (await closeBtn.isVisible().catch(() => false)) {
        await closeBtn.click();
        await sleep(1000);
      }
    }

    // 检查是否导航到了详情页（URL变化）
    const currentUrl = page.url();
    if (currentUrl !== route && !currentUrl.endsWith(route)) {
      console.log(`  [${pageName}] 导航到详情页: ${currentUrl}`);
      // 检查详情页结构
      await checkDetailPageLayout(page, pageName);
    }
  }

  // 查找"查询/搜索"按钮
  const searchBtn = page.locator('.ant-btn:visible').filter({ hasText: /查询|搜索/ }).first();
  if (await searchBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
    console.log(`  [${pageName}] 测试查询按钮`);
    await searchBtn.click();
    await sleep(2000);
  }
}

async function checkDetailPageLayout(page, pageName) {
  // 检查全屏覆盖模式
  // 详情页应有：顶部标题栏、中间内容区、底部固定操作栏
  const footer = page.locator('.ant-layout-footer:visible, .detail-footer:visible, .ant-pro-footer-bar:visible');
  if (await footer.isVisible().catch(() => false)) {
    const pos = await footer.evaluate(el => window.getComputedStyle(el).position).catch(() => '');
    if (pos !== 'fixed' && pos !== 'sticky') {
      addError('production-grade', pageName, '详情页操作栏未固定',
        `底部操作栏position=${pos}，应固定不随内容滚动`);
    }
  } else {
    addError('production-grade', pageName, '详情页缺少固定操作栏',
      '未找到底部固定操作栏');
  }
}

async function main() {
  console.log('========================================');
  console.log('  ERP 全页面自动扫描');
  console.log('========================================\n');

  // 确保目录存在
  fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
  fs.mkdirSync(REPORT_DIR, { recursive: true });

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
  });
  const page = await context.newPage();

  // 收集控制台错误
  page.on('console', msg => {
    if (msg.type() === 'error') {
      addError('interaction', currentPage, '控制台错误', msg.text().slice(0, 500));
    }
  });

  // 收集网络请求错误
  page.on('response', response => {
    if (response.status() >= 400 && response.status() < 600) {
      const url = response.url();
      // 过滤掉sse/notification等长连接
      if (!url.includes('/sse/') && !url.includes('/notification')) {
        addError('api', currentPage, `HTTP ${response.status()}`, url);
      }
    }
  });

  // 收集页面错误
  page.on('pageerror', error => {
    addError('blocking', currentPage, '页面JS异常', error.message, error.stack);
  });

  try {
    // ==================== 1. 登录 ====================
    console.log('阶段1: 登录系统');
    await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded' });
    await sleep(2000);

    // 填写登录表单
    const tenantInput = page.locator('input[placeholder="请输入租户名称"]');
    if (await tenantInput.isVisible({ timeout: 3000 }).catch(() => false)) {
      await tenantInput.fill('系统租户');
    }
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    const captchaInput = page.locator('input[placeholder="请输入验证码"]');
    if (await captchaInput.isVisible({ timeout: 2000 }).catch(() => false)) {
      await captchaInput.fill('ABCD');
    }
    await page.click('button[type="submit"]');

    // 等待跳转到dashboard
    try {
      await page.waitForURL('**/dashboard**', { timeout: 20000 });
    } catch {
      console.error('登录失败: 未跳转到dashboard');
      await page.screenshot({ path: `${SCREENSHOT_DIR}/00-login-failed.png` });
      return;
    }
    await sleep(3000);
    console.log('   ✓ 登录成功\n');

    // 截屏仪表盘
    await page.screenshot({ path: `${SCREENSHOT_DIR}/00-dashboard.png`, fullPage: true });

    // ==================== 2. 扫描页面 ====================
    console.log(`阶段2: 扫描 ${Object.keys(MENU_PATHS).length} 个页面\n`);

    let successCount = 0;
    let failCount = 0;
    let pageIdx = 0;

    for (const [menuPath, route] of Object.entries(MENU_PATHS)) {
      pageIdx++;
      const pageName = menuPath.replace('>', '/');
      currentPage = pageName;

      console.log(`[${pageIdx}/${Object.keys(MENU_PATHS).length}] ${pageName}`);

      try {
        // 通过菜单点击导航
        let navigated = await navigateByMenu(page, menuPath);

        if (!navigated) {
          // fallback: 直接URL导航
          console.log(`  菜单未找到，使用URL: ${route}`);
          await page.goto(`${BASE_URL}${route}`, { waitUntil: 'domcontentloaded', timeout: 15000 });
          await sleep(2000);
        }

        // 等待页面渲染完成
        await sleep(2000);

        // 检查页面渲染
        const currentUrl = page.url();
        console.log(`  URL: ${currentUrl}`);

        // 检查页面标准
        await checkPageStandards(page, pageName);

        // 交互测试
        await testPageInteractions(page, pageName, currentUrl);

        // 截屏
        const safeName = pageName.replace(/[/>]/g, '_');
        await page.screenshot({
          path: `${SCREENSHOT_DIR}/${String(pageIdx).padStart(2, '0')}-${safeName}.png`,
          fullPage: true
        });

        successCount++;
      } catch (e) {
        failCount++;
        addError('blocking', pageName, '页面导航异常', e.message);
        console.log(`  ✗ 错误: ${e.message}`);
      }

      console.log('');
    }

    // ==================== 3. 生成报告 ====================
    console.log('========================================');
    console.log('  扫描完成');
    console.log(`  成功: ${successCount}, 失败: ${failCount}`);
    console.log(`  总错误: ${allErrors.length}`);
    console.log('========================================\n');

    // 按类型统计
    const byType = {};
    for (const err of allErrors) {
      byType[err.type] = (byType[err.type] || 0) + 1;
    }
    console.log('错误分类统计:');
    for (const [type, count] of Object.entries(byType).sort((a, b) => b[1] - a[1])) {
      console.log(`  ${type}: ${count}`);
    }

    // 列出所有错误
    if (allErrors.length > 0) {
      console.log('\n详细错误列表:');
      for (const err of allErrors) {
        console.log(`  [${err.type}] ${err.page}: ${err.title}`);
        if (err.message) console.log(`     ${err.message.slice(0, 200)}`);
      }
    }

    // 保存报告
    const report = {
      timestamp: new Date().toISOString(),
      totalPages: Object.keys(MENU_PATHS).length,
      successCount,
      failCount,
      totalErrors: allErrors.length,
      errorsByType: byType,
      errors: allErrors,
    };
    fs.writeFileSync(`${REPORT_DIR}/scan-report.json`, JSON.stringify(report, null, 2));
    console.log(`\n报告已保存: ${REPORT_DIR}/scan-report.json`);

  } catch (e) {
    console.error('扫描异常:', e);
  } finally {
    await browser.close();
  }
}

main();
