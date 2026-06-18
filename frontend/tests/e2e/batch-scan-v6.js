/**
 * ERP 全流程扫描 v7 — 菜单导航验证 + 完整页面清单
 *
 * 每个页面：菜单导航 → 验证路由 → 检查渲染错误 → 生产级检查
 * 菜单导航失败即算错误（用户要求）。
 */

const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');
const http = require('http');

const BASE_URL = 'http://localhost:5656';
const API_BASE = 'http://localhost:5655';
const DIR = path.resolve(__dirname, '../../apps/pc-admin/scan-results');
fs.mkdirSync(DIR, { recursive: true });
const SCREENSHOT_DIR = path.join(DIR, 'screenshots');
fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });

const errors = { blocking: [], api: [], console: [], production: [], nav: [] };
function err(cat, page, type, detail) {
  errors[cat].push({ page, type, detail, time: new Date().toISOString() });
  console.log(`  [${cat}] ${page}: ${type} - ${(detail || '').slice(0, 150)}`);
}
function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

// ── 完整页面清单 ──────────────────────────────────────
// menuParent: 子菜单名称（null 表示在工作台默认展开区）
// route: 预期路由（用于校验，路由基于 dynamicRoutes.ts 的 componentMap + 菜单API返回数据）
const ALL_PAGES = [
  // 工作台（默认展开区）
  { name: '工作台', route: '/dashboard', menuParent: null, menuItem: '工作台', isSubmenuTitle: true },
  // 销售作业
  { name: '报价管理', route: '/crm/quotation', menuParent: null, menuItem: '报价管理' },
  // 销售管理
  { name: '销售订单', route: '/sale', menuParent: null, menuItem: '销售订单' },
  { name: '销售出库', route: '/erp/sale-outbound', menuParent: null, menuItem: '销售出库' },
  { name: '发货管理', route: '/erp/shipment', menuParent: null, menuItem: '发货管理' },
  { name: '退货处理', route: '/erp/return', menuParent: null, menuItem: '退货处理' },
  { name: '销售分析', route: '/erp/sales-analysis', menuParent: null, menuItem: '销售分析' },
  { name: '销售报表', route: '/erp/sales-report', menuParent: null, menuItem: '销售报表' },
  // 采购作业
  { name: '采购订单', route: '/purchase', menuParent: null, menuItem: '采购订单' },
  { name: '入库管理', route: '/erp/stock-in', menuParent: null, menuItem: '入库管理' },
  { name: '采购退货', route: '/erp/purchase-return', menuParent: null, menuItem: '采购退货' },
  { name: '采购换货', route: '/erp/purchase-exchange', menuParent: null, menuItem: '采购换货' },
  // 仓储作业
  { name: '库存查询', route: '/erp/stock', menuParent: null, menuItem: '库存查询' },
  { name: '库存盘点', route: '/erp/stocktake', menuParent: null, menuItem: '库存盘点' },
  { name: '库存调拨', route: '/erp/stock-transfer', menuParent: null, menuItem: '库存调拨' },
  { name: '库存成本调整', route: '/erp/stock-cost-adjust', menuParent: null, menuItem: '库存成本调整' },
  { name: '库存溢余处理', route: '/erp/stock-overflow', menuParent: null, menuItem: '库存溢余处理' },
  { name: '库存报损处理', route: '/erp/stock-damage', menuParent: null, menuItem: '库存报损处理' },
  { name: 'BOM管理', route: '/erp/stock-bom', menuParent: null, menuItem: 'BOM管理' },
  { name: '组装管理', route: '/erp/stock-assemble', menuParent: null, menuItem: '组装管理' },
  { name: '拆分管理', route: '/erp/stock-split', menuParent: null, menuItem: '拆分管理' },
  { name: '批次管理', route: '/erp/batch', menuParent: null, menuItem: '批次管理' },
  { name: '序列号管理', route: '/erp/serial', menuParent: null, menuItem: '序列号管理' },
  { name: '库存预警配置', route: '/erp/stock-alert-config', menuParent: null, menuItem: '库存预警配置' },
  { name: '补货管理', route: '/erp/stock/replenishment', menuParent: null, menuItem: '补货管理' },
  // 配送管理 - 配送模块
  { name: '配送仪表盘', route: '/dms', menuParent: '配送管理', menuItem: '配送仪表盘' },
  { name: '渠道管理', route: '/dms', menuParent: '配送管理', menuItem: '渠道管理' },
  { name: '骑手管理', route: '/dms', menuParent: '配送管理', menuItem: '骑手管理' },
  { name: '车辆管理', route: '/dms', menuParent: '配送管理', menuItem: '车辆管理' },
  { name: '配送路线', route: '/dms', menuParent: '配送管理', menuItem: '配送路线' },
  { name: '智能调度', route: '/dms', menuParent: '配送管理', menuItem: '智能调度' },
  { name: '订单池', route: '/dms', menuParent: '配送管理', menuItem: '订单池' },
  { name: '配送跟踪', route: '/dms', menuParent: '配送管理', menuItem: '配送跟踪' },
  { name: '实名认证', route: '/dms', menuParent: '配送管理', menuItem: '实名认证' },
  { name: '配送配置', route: '/dms', menuParent: '配送管理', menuItem: '配送配置' },
  // 客户关系(配送管理)
  { name: '线索管理', route: '/crm/lead', menuParent: '配送管理', menuItem: '线索管理' },
  { name: '商机管理', route: '/crm/opportunity', menuParent: '配送管理', menuItem: '商机管理' },
  { name: '客户档案', route: '/crm/customer', menuParent: '配送管理', menuItem: '客户档案' },
  { name: '合同管理', route: '/crm/contract', menuParent: '配送管理', menuItem: '合同管理' },
  { name: '发票管理', route: '/crm/invoice', menuParent: '配送管理', menuItem: '发票管理' },
  // 供应商(配送管理)
  { name: '供应商管理', route: '/supplier', menuParent: '配送管理', menuItem: '供应商管理' },
  { name: '供应商询价', route: '/supplier/inquiry', menuParent: '配送管理', menuItem: '供应商询价' },
  { name: '供应商绩效', route: '/supplier/performance', menuParent: '配送管理', menuItem: '供应商绩效' },
  // 财务管理(配送管理)
  { name: '科目管理', route: '/finance/subject', menuParent: '配送管理', menuItem: '科目管理' },
  { name: '凭证管理', route: '/finance/voucher', menuParent: '配送管理', menuItem: '凭证管理' },
  { name: '应收账款', route: '/finance/receivable', menuParent: '配送管理', menuItem: '应收账款' },
  { name: '应收明细', route: '/finance/accounts-receivable', menuParent: '配送管理', menuItem: '应收明细' },
  { name: '应收账龄分析', route: '/finance/accounts-receivable/aging-analysis', menuParent: '配送管理', menuItem: '应收账龄分析' },
  { name: '应付账款', route: '/finance/payable', menuParent: '配送管理', menuItem: '应付账款' },
  { name: '应付明细', route: '/finance/accounts-payable', menuParent: '配送管理', menuItem: '应付明细' },
  { name: '收款单管理', route: '/finance/receipt', menuParent: '配送管理', menuItem: '收款单管理' },
  { name: '付款单管理', route: '/finance/payment', menuParent: '配送管理', menuItem: '付款单管理' },
  { name: '预收款管理', route: '/finance/pre-receipt', menuParent: '配送管理', menuItem: '预收款管理' },
  { name: '预付款管理', route: '/finance/pre-payment', menuParent: '配送管理', menuItem: '预付款管理' },
  { name: '定金押金管理', route: '/finance/deposit', menuParent: '配送管理', menuItem: '定金押金管理' },
  { name: '收付款核销', route: '/finance/write-off', menuParent: '配送管理', menuItem: '收付款核销' },
  { name: '往来对冲', route: '/finance/offset', menuParent: '配送管理', menuItem: '往来对冲' },
  { name: '资金流水台账', route: '/finance/capital-flow', menuParent: '配送管理', menuItem: '资金流水台账' },
  { name: '财务报表', route: '/finance/report', menuParent: '配送管理', menuItem: '财务报表' },
  { name: '对账管理', route: '/finance/reconciliation', menuParent: '配送管理', menuItem: '对账管理' },
  // 费用管理(配送管理)
  { name: '费用申请', route: '/erp/expense/application', menuParent: '配送管理', menuItem: '费用申请' },
  { name: '费用报销', route: '/erp/expense/reimbursement', menuParent: '配送管理', menuItem: '费用报销' },
  { name: '费用审批', route: '/erp/expense/approval', menuParent: '配送管理', menuItem: '费用审批' },
  { name: '费用付款', route: '/erp/expense/payment', menuParent: '配送管理', menuItem: '费用付款' },
  { name: '费用统计', route: '/erp/expense/statistics', menuParent: '配送管理', menuItem: '费用统计' },
  // WMS仓储执行
  { name: '仓库管理', route: '/wms', menuParent: 'WMS仓储执行', menuItem: '仓库管理' },
  { name: '库位管理', route: '/wms', menuParent: 'WMS仓储执行', menuItem: '库位管理' },
  { name: '收货管理', route: '/wms', menuParent: 'WMS仓储执行', menuItem: '收货管理' },
  { name: '上架管理', route: '/wms', menuParent: 'WMS仓储执行', menuItem: '上架管理' },
  { name: '拣货管理', route: '/wms', menuParent: 'WMS仓储执行', menuItem: '拣货管理' },
  { name: '波次管理', route: '/wms', menuParent: 'WMS仓储执行', menuItem: '波次管理' },
  { name: '库内移库', route: '/wms', menuParent: 'WMS仓储执行', menuItem: '库内移库' },
  { name: '盘点管理', route: '/wms', menuParent: 'WMS仓储执行', menuItem: '盘点管理' },
  { name: '库内事件', route: '/wms', menuParent: 'WMS仓储执行', menuItem: '库内事件' },
  // 工作流
  { name: '产品管理', route: '/erp/product', menuParent: '工作流', menuItem: '产品管理' },
  { name: '往来单位', route: '/erp/partner', menuParent: '工作流', menuItem: '往来单位' },
  { name: '定价管理', route: '/erp/pricing', menuParent: '工作流', menuItem: '定价管理' },
  { name: '定价审批', route: '/erp/pricing/approval', menuParent: '工作流', menuItem: '定价审批' },
  { name: '价格层级', route: '/erp/pricing/tiers', menuParent: '工作流', menuItem: '价格层级' },
  { name: '流程监控', route: '/workflow', menuParent: '工作流', menuItem: '流程监控' },
  { name: '任务管理', route: '/workflow', menuParent: '工作流', menuItem: '任务管理' },
  { name: '流程分析', route: '/workflow', menuParent: '工作流', menuItem: '流程分析' },
  // 资产管理
  { name: '资产台账', route: '/fixed-asset', menuParent: '资产管理', menuItem: '资产台账' },
  { name: '资产分类', route: '/fixed-asset', menuParent: '资产管理', menuItem: '资产分类' },
  { name: '资产采购', route: '/fixed-asset', menuParent: '资产管理', menuItem: '资产采购' },
  { name: '折旧管理', route: '/fixed-asset', menuParent: '资产管理', menuItem: '折旧管理' },
  { name: '资产调拨', route: '/fixed-asset', menuParent: '资产管理', menuItem: '资产调拨' },
  { name: '资产处置', route: '/fixed-asset', menuParent: '资产管理', menuItem: '资产处置' },
  { name: '资产盘点', route: '/fixed-asset', menuParent: '资产管理', menuItem: '资产盘点' },
  { name: '资产报表', route: '/fixed-asset', menuParent: '资产管理', menuItem: '资产报表' },
  // 预算管理
  { name: '预算概览', route: '/budget', menuParent: '预算管理', menuItem: '预算概览' },
  { name: '预算模板', route: '/budget', menuParent: '预算管理', menuItem: '预算模板' },
  { name: '年度预算', route: '/budget', menuParent: '预算管理', menuItem: '年度预算' },
  { name: '预算调整', route: '/budget', menuParent: '预算管理', menuItem: '预算调整' },
  { name: '预算报表', route: '/budget', menuParent: '预算管理', menuItem: '预算报表' },
  // 商城管理
  { name: '商城配置', route: '/mall', menuParent: '商城管理', menuItem: '商城配置' },
  { name: '商品管理', route: '/mall', menuParent: '商城管理', menuItem: '商品管理' },
  { name: '订单管理', route: '/mall', menuParent: '商城管理', menuItem: '订单管理' },
  { name: '用户审核', route: '/mall', menuParent: '商城管理', menuItem: '用户审核' },
  { name: '轮播图管理', route: '/mall', menuParent: '商城管理', menuItem: '轮播图管理' },
  // 订单中心
  { name: '订单中心', route: '/order-center', menuParent: '订单中心', menuItem: '订单中心' },
  // 打印管理
  { name: '打印模板', route: '/printing', menuParent: '打印管理', menuItem: '打印模板' },
  { name: '打印链路', route: '/printing', menuParent: '打印管理', menuItem: '打印链路' },
  { name: '打印客户端', route: '/printing', menuParent: '打印管理', menuItem: '打印客户端' },
  { name: '打印任务', route: '/printing', menuParent: '打印管理', menuItem: '打印任务' },
  // 系统管理
  { name: '组织架构', route: '/system', menuParent: '系统管理', menuItem: '组织架构' },
  { name: '岗位管理', route: '/system', menuParent: '系统管理', menuItem: '岗位管理' },
  { name: '用户管理', route: '/system', menuParent: '系统管理', menuItem: '用户管理' },
  { name: '角色管理', route: '/system', menuParent: '系统管理', menuItem: '角色管理' },
  { name: '权限管理', route: '/system', menuParent: '系统管理', menuItem: '权限管理' },
  { name: '菜单管理', route: '/system', menuParent: '系统管理', menuItem: '菜单管理' },
  { name: '租户管理', route: '/system', menuParent: '系统管理', menuItem: '租户管理' },
  { name: '租户审批', route: '/system', menuParent: '系统管理', menuItem: '租户审批' },
  { name: '字典管理', route: '/system', menuParent: '系统管理', menuItem: '字典管理' },
  { name: '系统配置', route: '/system', menuParent: '系统管理', menuItem: '系统配置' },
  { name: '系统日志', route: '/system', menuParent: '系统管理', menuItem: '系统日志' },
  { name: '数据导入', route: '/system', menuParent: '系统管理', menuItem: '数据导入' },
  { name: '通知公告', route: '/notification', menuParent: '系统管理', menuItem: '通知公告' },
  // 图表
  { name: '图表', route: '/charts', menuParent: '图表', menuItem: '图表' },
];

// ── 获取验证码 ────────────────────────────────────────
async function fetchCaptcha() {
  return new Promise((resolve, reject) => {
    http.get(`${API_BASE}/api/auth/captcha`, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          const body = JSON.parse(data);
          if (body.code === 200 && body.data) {
            const svg = Buffer.from(body.data.img.replace('data:image/svg+xml;base64,', ''), 'base64').toString('utf-8');
            const texts = [...svg.matchAll(/<text[^>]*>([^<]+)<\/text>/g)];
            const code = texts.map(m => m[1]).join('');
            resolve({ uuid: body.data.uuid, code });
          } else reject(new Error('Captcha API failed'));
        } catch (e) { reject(e); }
      });
    }).on('error', reject);
  });
}

// ── 菜单导航 ──────────────────────────────────────────
async function navigateViaMenu(page, pg) {
  const { menuParent, menuItem, isSubmenuTitle } = pg;

  // 展开父级子菜单
  if (menuParent) {
    const sub = page.locator('.ant-menu-submenu-title').filter({ hasText: menuParent }).first();
    if (await sub.isVisible().catch(() => false)) {
      const expanded = await sub.getAttribute('aria-expanded').catch(() => null);
      if (expanded !== 'true') {
        await sub.click();
        await sleep(800);
      }
    }
  }

  if (isSubmenuTitle) {
    // 工作台等作为 submenu-title 显示, 点击即可导航
    const sub = page.locator('.ant-menu-submenu-title').filter({ hasText: menuItem }).first();
    if (await sub.isVisible().catch(() => false)) {
      await sub.click();
      await sleep(1500);
      return true;
    }
    return false;
  }

  // 标准菜单项
  const item = page.locator('.ant-menu-item').filter({ hasText: menuItem }).first();
  if (await item.isVisible().catch(() => false)) {
    await item.click();
    await sleep(1500);
    return true;
  }
  return false;
}

// ── Router 导航（保底）─────────────────────────────────
async function navigateByRouter(page, route) {
  try {
    await page.evaluate(r => {
      const app = document.querySelector('#app');
      const router = app?.__vue_app__?.config?.globalProperties?.$router;
      if (router) router.push(r);
    }, route);
    await sleep(2000);
  } catch {
    await page.goto(`${BASE_URL}${route}`, { waitUntil: 'domcontentloaded', timeout: 10000 }).catch(() => {});
    await sleep(2000);
  }
}

// ── 检查页面 ──────────────────────────────────────────
async function checkPage(page, pg) {
  const { name, route } = pg;

  // 1. 白屏检测
  const bodyLen = (await page.locator('body').textContent().catch(() => '')).length;
  if (bodyLen < 10) { err('blocking', name, '白屏', ''); return; }

  // 2. 会话过期（跳转到登录页）
  if (page.url().includes('/login')) { err('blocking', name, '会话过期', ''); return; }

  // 3. Spin 持续加载
  const spin = page.locator('.ant-spin-spinning:visible');
  if (await spin.count().then(c => c > 0).catch(() => false)) {
    try { await spin.first().waitFor({ state: 'hidden', timeout: 5000 }); }
    catch { err('blocking', name, '持续加载', 'Spin>5s'); }
  }

  // 4. 错误组件
  const resultErr = page.locator('.ant-result-error:visible');
  if (await resultErr.count().then(c => c > 0).catch(() => false)) {
    const txt = await resultErr.first().textContent().catch(() => '');
    err('blocking', name, 'ErrorBoundary', txt.slice(0, 200));
  }
  const alertErr = page.locator('.ant-alert-error:visible');
  if (await alertErr.count().then(c => c > 0).catch(() => false)) {
    const txt = await alertErr.first().textContent().catch(() => '');
    err('blocking', name, '错误警告', txt.slice(0, 200));
  }

  // 5. 生产级检查
  // 5a. 表格边框
  const table = page.locator('.ant-table:visible').first();
  if (await table.isVisible().catch(() => false)) {
    const bw = await table.evaluate(el => {
      const s = getComputedStyle(el);
      return `${s.borderLeftWidth} ${s.borderTopWidth} ${s.borderRightWidth} ${s.borderBottomWidth}`;
    }).catch(() => '');
    if (bw === '0px 0px 0px 0px') err('production', name, '表格无边框', '');
  }

  // 5b. 按钮尺寸
  const largeBtns = await page.locator('.ant-btn:visible:not(.ant-btn-sm):not(.ant-btn-lg)').count().catch(() => 0);
  if (largeBtns > 8) err('production', name, '非small按钮数', `${largeBtns}个`);

  // 5c. Modal vs FullScreenDetail
  const modalCount = await page.locator('.ant-modal-wrap:visible').count().catch(() => 0);
  const fsdCount = await page.locator('.fullscreen-detail:visible').count().catch(() => 0);
  const drawerCount = await page.locator('.ant-drawer:visible').count().catch(() => 0);
  if (modalCount > 0) {
    const modalText = await page.locator('.ant-modal-wrap:visible .ant-modal-title').first().textContent().catch(() => '');
    err('production', name, '使用Modal', modalText ? `"${modalText}"` : `${modalCount}个`);
  }

  return { modalCount, fsdCount, drawerCount };
}

// ── 关闭所有弹窗 ──────────────────────────────────────
async function closeAll(page) {
  for (let i = 0; i < 5; i++) {
    await page.keyboard.press('Escape');
    await sleep(300);
  }
  const closeBtns = page.locator('.ant-modal-close:visible, .detail-close-btn:visible');
  const n = await closeBtns.count().catch(() => 0);
  for (let i = 0; i < n; i++) {
    await closeBtns.nth(0).click({ timeout: 2000 }).catch(() => {});
    await sleep(300);
  }
}

// ── 主流程 ────────────────────────────────────────────
async function main() {
  const batchNum = process.argv[2] ? parseInt(process.argv[2]) : 0; // 0=all
  const totalPages = ALL_PAGES.length;
  const batchSize = 10;
  const batches = [];
  for (let i = 0; i < totalPages; i += batchSize) batches.push(ALL_PAGES.slice(i, i + batchSize));

  if (batchNum > 0) {
    console.log(`\n分批扫描: 第${batchNum}批/${batches.length}\n`);
  } else {
    console.log(`\n全量扫描: ${totalPages}页, ${batches.length}批\n`);
  }

  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 800 } });
  const page = await ctx.newPage();

  // 收集控制台错误
  page.on('pageerror', e => err('blocking', 'PAGE', '未捕获异常', e.message.slice(0, 200)));
  page.on('console', msg => {
    if (msg.type() === 'error') {
      err('console', page.url().split('/').pop() || '', 'console.error', msg.text().slice(0, 200));
    }
  });

  // ── 登录 ────────────────────────────────────────────
  console.log('登录...');
  async function doLogin() {
    await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle', timeout: 15000 }).catch(async () => {
      await page.reload({ waitUntil: 'networkidle', timeout: 15000 }).catch(() => {});
    });
    await sleep(1000);
    await page.locator('input').first().waitFor({ state: 'visible', timeout: 10000 });
    const captcha = await fetchCaptcha().catch(() => null);
    const cc = captcha ? captcha.code : 'ABCD';
    await page.locator('input').nth(0).fill('系统租户');
    await page.locator('input').nth(1).fill('admin');
    await page.locator('input').nth(2).fill('admin123');
    await page.locator('input').nth(3).fill(cc);
    await page.locator('button').filter({ hasText: '登' }).first().click();
    await sleep(2000);
    try { await page.waitForURL('**/dashboard**', { timeout: 10000 }); } catch {}
    await sleep(3000);
  }

  await doLogin();
  for (let i = 0; i < 2; i++) {
    const menuItems = await page.locator('.ant-menu-item:visible').count().catch(() => 0);
    if (menuItems > 10) { console.log('  登录成功'); break; }
    console.log(`  重试登录(${i+1})...`);
    await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded', timeout: 15000 }).catch(() => {});
    await sleep(1000);
    await doLogin();
  }
  const menuItems = await page.locator('.ant-menu-item:visible').count().catch(() => 0);
  if (menuItems < 10) {
    console.error('  登录失败, 菜单项:', menuItems);
    await page.screenshot({ path: path.join(SCREENSHOT_DIR, 'login-failed.png') });
    await browser.close();
    process.exit(1);
  }
  console.log('  菜单项:', menuItems, '\n');

  // ── 扫描页面 ────────────────────────────────────────
  const pagesToScan = batchNum > 0 ? batches[batchNum - 1] : ALL_PAGES;

  for (const pg of pagesToScan) {
    const idx = ALL_PAGES.indexOf(pg) + 1;
    const { name, route, menuParent } = pg;
    console.log(`[${idx}/${totalPages}] ${name}`);

    try {
      // 通过菜单导航
      const navOk = await navigateViaMenu(page, pg);
      if (!navOk) {
        // 菜单导航失败 → 记录为错误
        err('nav', name, '菜单导航失败', `menuItem=${pg.menuItem}, menuParent=${pg.menuParent}, 使用router保底`);
        await navigateByRouter(page, route);
      }

      await sleep(2000);

      // 验证路由
      const currentUrl = page.url();
      if (!currentUrl.includes(route.replace(/\/$/, ''))) {
        err('nav', name, '路由不匹配', `期望=${route}, 实际=${currentUrl.replace(BASE_URL, '')}`);
      }

      // 检查页面
      const result = await checkPage(page, pg);
      if (result) {
        console.log(`  Modal=${result.modalCount} FSD=${result.fsdCount} Drawer=${result.drawerCount}`);
      }

      // 关闭弹窗
      await closeAll(page);

    } catch (e) {
      err('blocking', name, '扫描异常', e.message);
      await page.screenshot({ path: path.join(SCREENSHOT_DIR, `err-${name}.png`) });
    }
  }

  // ── 报告 ────────────────────────────────────────────
  console.log(`\n=== 扫描完成 ===`);
  const total = Object.values(errors).reduce((s, arr) => s + arr.length, 0);
  console.log(`${totalPages} pages processed, ${total} errors`);
  for (const [cat, arr] of Object.entries(errors)) {
    if (arr.length > 0) {
      console.log(`  ${cat}: ${arr.length}`);
      for (const e of arr) {
        console.log(`    [${e.page}] ${e.type}: ${(e.detail || '').slice(0, 100)}`);
      }
    }
  }
  fs.writeFileSync(path.join(DIR, 'scan-v6-report.json'), JSON.stringify(errors, null, 2));
  await browser.close();
  console.log('\nDone');
}

main().catch(e => {
  console.error('Fatal:', e);
  process.exit(1);
});
