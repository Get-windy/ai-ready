/**
 * ERP 全页面扫描 v4
 *
 * 修复:
 * - Spin检查: 先判断是否存在, 存在才等消失
 * - 不使用 fullPage 截屏(避免超大图片)
 * - 每页严格超时控制
 * - 退出前关闭所有弹窗
 */
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const BASE = 'http://localhost:5656';
const DIR = path.resolve(__dirname, '../../screenshots/scan-v4');
const ERRORS = [];
let curPage = '';

function err(type, page, title, msg, detail) {
  ERRORS.push({ type, page, title, msg: msg||'', detail: detail||'', ts: new Date().toISOString() });
}

const PAGES = [
  '工作台', '/dashboard',
  '销售订单', '/sale',
  '销售出库', '/stock',
  '发货管理', '/erp/shipment',
  '退货处理', '/erp/return',
  '销售分析', '/erp/sales-analysis',
  '销售报表', '/erp/sales-report',
  '采购订单', '/purchase',
  '入库管理', '/erp/stock-in',
  '采购退货', '/erp/purchase-return',
  '采购换货', '/erp/purchase-exchange',
  '库存管理', '/erp/stock',
  '库存盘点', '/erp/stocktake',
  '库存调拨', '/erp/stock-transfer',
  '批次管理', '/erp/batch',
  '序列号管理', '/erp/serial',
  '线索管理', '/crm/lead',
  '商机管理', '/crm/opportunity',
  '客户档案', '/crm/customer',
  '合同管理', '/crm/contract',
  '报价管理', '/crm/quotation',
  '发票管理', '/crm/invoice',
  '产品管理', '/erp/product',
  '往来单位', '/erp/partner',
  '定价管理', '/erp/pricing',
  '科目管理', '/finance/subject',
  '凭证管理', '/finance/voucher',
  '应收账款', '/finance/accounts-receivable',
  '应付账款', '/finance/accounts-payable',
  '收款单管理', '/finance/receipt',
  '付款单管理', '/finance/payment',
  '财务报表', '/finance/report',
  '费用申请', '/erp/expense/application',
  '费用报销', '/erp/expense/reimbursement',
  '费用审批', '/erp/expense/approval',
  '费用付款', '/erp/expense/payment',
  '费用统计', '/erp/expense/statistics',
  '资产台账', '/fixed-asset/asset',
  '资产分类', '/fixed-asset/category',
  '折旧管理', '/fixed-asset/depreciation',
  '资产调拨', '/fixed-asset/transfer',
  '资产处置', '/fixed-asset/disposal',
  '资产盘点', '/fixed-asset/inventory',
  '资产报表', '/fixed-asset/report',
  '预算管理', '/budget',
  '供应商管理', '/supplier',
  '供应商询价', '/supplier/inquiry',
  '供应商绩效', '/supplier/performance',
  '用户管理', '/system/user',
  '角色管理', '/system/role',
  '菜单管理', '/system/menu',
  '租户管理', '/system/tenant',
  '字典管理', '/system/dict',
  '系统配置', '/system/config',
  '系统日志', '/system/log',
  '通知公告', '/notification',
  '定价审批', '/erp/pricing/approval',
  '价格层级', '/erp/pricing/tiers',
  '组织架构', '/system/department',
  '岗位管理', '/system/position',
  '权限管理', '/system/permission',
  '租户审批', '/system/tenant-approval',
  '数据导入', '/system/data-import',
  '流程监控', '/workflow/instance-monitor',
  '任务管理', '/workflow/task-management',
  '流程分析', '/workflow/process-analysis',
  '打印模板', '/printing/template',
  '打印链路', '/printing/chain',
];

function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

async function nav(page, route) {
  try {
    await page.evaluate(r => {
      const app = document.querySelector('#app');
      const router = app?.__vue_app__?.config?.globalProperties?.$router;
      if (router) router.push(r);
    }, route);
    await sleep(2000);
  } catch {
    await page.goto(`${BASE}${route}`, { waitUntil: 'domcontentloaded', timeout: 10000 }).catch(() => {});
    await sleep(2000);
  }
}

async function closeModals(page) {
  for (let i = 0; i < 5; i++) {
    await page.keyboard.press('Escape');
    await sleep(200);
  }
}
async function check(page, name) {
  const body = (await page.locator('body').textContent().catch(() => '')) || '';
  if (body.trim().length < 10) { err('blocking', name, '白屏', ''); return false; }
  if (page.url().includes('/login')) { err('blocking', name, '会话过期', ''); return false; }

  const spin = page.locator('.ant-spin-spinning:visible');
  const sc = await spin.count().catch(() => 0);
  if (sc > 0) {
    try { await spin.waitFor({ state: 'hidden', timeout: 5000 }); }
    catch { err('blocking', name, '持续加载', 'Spin>5s'); }
  }

  const al = page.locator('.ant-alert-error:visible, .ant-result-error:visible');
  if (await al.count().then(c=>c>0).catch(()=>false)) {
    const txt = (await al.first().textContent().catch(()=>''))||'';
    err('blocking', name, '错误组件', txt.slice(0,200));
  }

  // 生产级检查
  const table = page.locator('.ant-table:visible').first();
  if (await table.isVisible().catch(()=>false)) {
    const bw = await table.evaluate(el => {
      const s = window.getComputedStyle(el);
      return `${s.borderLeftWidth} ${s.borderTopWidth} ${s.borderRightWidth} ${s.borderBottomWidth}`;
    }).catch(()=>'');
    if (bw === '0px 0px 0px 0px') err('production-grade', name, '表格无边框', bw);
  }

  return true;
}

async function interact(page, name) {
  await closeModals(page);
  await sleep(300);

  const btns = page.locator('.ant-btn:visible');
  const n = await btns.count().catch(()=>0);

  // 找新增/新建按钮
  for (let i = 0; i < n; i++) {
    const txt = ((await btns.nth(i).textContent().catch(()=>''))||'').trim();
    if (!txt || txt === '查询' || txt === '重置' || txt === '刷新' || txt === '返回') continue;
    if (txt.includes('新增') || txt.includes('新建') || txt.includes('创建')) {
      console.log(`  [交互] ${txt}`);
      await btns.nth(i).click({ force: true, timeout: 5000 }).catch(()=>{});
      await sleep(2000);

      // 检查 Modal
      if (await page.locator('.ant-modal-wrap:visible').count().then(c=>c>0).catch(()=>false)) {
        err('production-grade', name, '使用Modal', `"${txt}"弹出Modal`);
        await closeModals(page);
      }
      break;
    }
  }
}

async function main() {
  console.log('ERP扫描 v4\n');
  fs.mkdirSync(DIR, {recursive: true});

  const browser = await chromium.launch({headless: true});
  const ctx = await browser.newContext({viewport: {width: 1280, height: 800}});
  const page = await ctx.newPage();

  page.on('console', msg => {
    if (msg.type()==='error') err('console', curPage, 'Console', msg.text().slice(0,200));
  });
  page.on('response', resp => {
    const s = resp.status();
    if (s >= 400) {
      const u = resp.url();
      if (!u.includes('/sse/') && !u.includes('/notification/'))
        err('api', curPage, `HTTP ${s}`, u.replace(BASE,''));
    }
  });
  page.on('pageerror', e => err('blocking', curPage, 'JS异常', e.message||''));

  // 登录
  console.log('登录...');
  await page.goto(`${BASE}/login`, {waitUntil: 'domcontentloaded'});
  await sleep(2000);
  await page.fill('input[placeholder="请输入租户名称"]', '系统租户').catch(()=>{});
  await page.fill('input[placeholder="请输入用户名"]', 'admin');
  await page.fill('input[placeholder="请输入密码"]', 'admin123');
  await page.fill('input[placeholder="请输入验证码"]', 'ABCD').catch(()=>{});
  await page.click('button[type="submit"]');
  await page.waitForURL('**/dashboard**', {timeout: 20000}).catch(()=>{console.error('登录失败'); return});
  await sleep(3000);
  console.log('  OK\n');

  // 扫描
  let ok = 0, fail = 0;
  for (let i = 0; i < PAGES.length; i += 2) {
    const name = PAGES[i], route = PAGES[i+1];
    curPage = name;
    const idx = i/2 + 1;
    console.log(`[${idx}/${PAGES.length/2}] ${name}`);

    try {
      await closeModals(page);
      await nav(page, route);
      await sleep(2000);
      const valid = await check(page, name);
      if (!valid) fail++;
      await interact(page, name);

      await page.screenshot({path: `${DIR}/${String(idx).padStart(2,'0')}-${name.replace(/[/\\]/g,'_')}.png`}).catch(()=>{});
      ok++;
    } catch(e) {
      fail++;
      err('blocking', name, '异常', e.message||'');
      console.log(`  ✗ ${e.message}`);
    }
  }

  // 报告
  const byType = {};
  ERRORS.forEach(e => { byType[e.type] = (byType[e.type]||0)+1; });

  console.log(`\n=== ${ok} OK, ${fail} Fail, ${ERRORS.length} Errors ===`);
  Object.entries(byType).sort((a,b)=>b[1]-a[1]).forEach(([t,c]) => console.log(`  ${t}: ${c}`));

  if (ERRORS.length) {
    console.log('\n错误:');
    ERRORS.forEach(e => console.log(`  [${e.type}] ${e.page}: ${e.msg ? e.msg.slice(0,100) : e.title}`));
  }

  fs.writeFileSync(path.resolve(__dirname, '../../scan-results/scan-v4-report.json'), JSON.stringify({
    ts: new Date().toISOString(), total: PAGES.length/2, ok, fail,
    errors: ERRORS, byType
  }, null, 2));

  console.log(`\n报告: scan-results/scan-v4-report.json`);
  await browser.close();
}

main();
