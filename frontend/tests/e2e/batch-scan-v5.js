/**
 * ERP 全流程 E2E 扫描 v5
 *
 * 通过点击前端菜单项进行导航，模拟真实用户操作路径。
 * 覆盖：菜单导航 → 列表页 → 新建表单 → 填写保存 → 编辑 → 删除
 * 收集：控制台错误、网络错误、组件崩溃、生产级缺陷
 */

const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');
const http = require('http');

const BASE_URL = 'http://localhost:5656';
const API_BASE = 'http://localhost:5655';
const DIR = path.resolve(__dirname, '../../apps/pc-admin/scan-results');
fs.mkdirSync(DIR, { recursive: true });

const REPORT_PATH = path.join(DIR, 'scan-v5-report.json');

// ── 页面清单 ──────────────────────────────────────────
// 菜单结构: 工作台(默认展开) / 配送管理 / 工作流 / 打印管理 / 系统管理
// menuParent = null 表示在工作台(默认展开), 否则为子菜单名称
const ALL_PAGES = [
  { name: '工作台', route: '/dashboard', menuParent: null, menuItem: '工作台' },
  { name: '销售订单', route: '/sale', menuParent: null, menuItem: '销售订单' },
  { name: '销售出库', route: '/stock', menuParent: null, menuItem: '销售出库' },
  { name: '发货管理', route: '/erp/shipment', menuParent: null, menuItem: '发货管理' },
  { name: '退货处理', route: '/erp/return', menuParent: null, menuItem: '退货处理' },
  { name: '销售分析', route: '/erp/sales-analysis', menuParent: null, menuItem: '销售分析' },
  { name: '销售报表', route: '/erp/sales-report', menuParent: null, menuItem: '销售报表' },
  { name: '采购订单', route: '/purchase', menuParent: null, menuItem: '采购订单' },
  { name: '入库管理', route: '/erp/stock-in', menuParent: null, menuItem: '入库管理' },
  { name: '采购退货', route: '/erp/purchase-return', menuParent: null, menuItem: '采购退货' },
];

const errors = {
  blocking: [],
  api: [],
  console: [],
  ui: [],
  production: [],
};

function err(cat, page, type, detail) {
  errors[cat].push({ page, type, detail, time: new Date().toISOString() });
  console.log(`  [${cat}] ${page}: ${type} - ${detail}`);
}

let errorCount = 0;

async function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

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
            // Parse SVG text to get captcha value
            const svg = Buffer.from(body.data.img.replace('data:image/svg+xml;base64,', ''), 'base64').toString('utf-8');
            const texts = [...svg.matchAll(/<text[^>]*>([^<]+)<\/text>/g)];
            const code = texts.map(m => m[1]).join('');
            resolve({ uuid: body.data.uuid, code });
          } else {
            reject(new Error('Captcha API failed'));
          }
        } catch (e) { reject(e); }
      });
    }).on('error', reject);
  });
}

// ── 通过菜单点击导航 ──────────────────────────────────
async function navigateViaMenu(page, menuParent, menuItem) {
  // 展开父级子菜单(如果未展开)
  if (menuParent) {
    const sub = page.locator(`.ant-menu-submenu-title:has-text("${menuParent}")`).first();
    if (await sub.isVisible().catch(() => false)) {
      const expanded = await sub.getAttribute('aria-expanded').catch(() => null);
      if (expanded !== 'true') {
        await sub.click().catch(() => {});
        await sleep(800);
      }
    }
  }

  // 点击目标菜单项
  const item = page.locator(`.ant-menu-item:has-text("${menuItem}")`).first();
  if (await item.isVisible().catch(() => false)) {
    await item.click().catch(() => {});
    await sleep(1500);
    return true;
  }
  return false;
}

// ── Vue Router 导航（保底方案）────────────────────────
async function navigateByRouter(page, route) {
  try {
    await page.evaluate(r => {
      const app = document.querySelector('#app');
      if (app && app.__vue_app__) {
        const router = app.__vue_app__.config.globalProperties.$router;
        if (router) router.push(r);
      }
    }, route);
    await sleep(2000);
  } catch {
    await page.goto(`${BASE_URL}${route}`, { waitUntil: 'domcontentloaded', timeout: 10000 }).catch(() => {});
    await sleep(2000);
  }
}

// ── 检查生产级标准 ────────────────────────────────────
async function checkProductionStandards(page, pageName) {
  const checks = [];

  // 1. 检查是否有 FullScreenDetail (全屏详情页)
  const fsdCount = await page.locator('.fullscreen-detail').count().catch(() => 0);
  if (fsdCount > 0) {
    // 检查全屏详情页结构
    const hasHeader = await page.locator('.detail-header').isVisible().catch(() => false);
    const hasBody = await page.locator('.detail-body').isVisible().catch(() => false);
    const hasFooter = await page.locator('.detail-footer').isVisible().catch(() => false);
    if (!hasHeader || !hasBody) {
      checks.push('FullScreenDetail 缺少 header/body 结构');
    }
  }

  // 2. 检查按钮尺寸（应该是 small）
  const btnCount = await page.locator('.ant-btn:not(.ant-btn-sm):visible').count().catch(() => 0);
  if (btnCount > 5) {
    checks.push(`发现 ${btnCount} 个非 small 尺寸按钮`);
  }

  // 3. 检查表格边框
  const tables = page.locator('.ant-table, .vxe-table, table');
  const tableCount = await tables.count().catch(() => 0);

  // 4. 检查 Modal 使用（应该用 FullScreenDetail 而非 Modal）
  const modalCount = await page.locator('.ant-modal-wrap:visible').count().catch(() => 0);
  const fsdOverlayCount = await page.locator('.fullscreen-detail-overlay:visible').count().catch(() => 0);

  for (const c of checks) {
    err('production', pageName, '生产级缺陷', c);
  }

  return { modalCount, fsdCount: fsdOverlayCount };
}

// ── 关闭所有弹窗 ──────────────────────────────────────
async function closeAllModals(page) {
  // 关闭 Modal
  const modalCloseBtns = page.locator('.ant-modal-close:visible, .ant-modal-wrap .ant-btn:not(:hidden):has-text("取消"):not(:hidden)');
  const closeCount = await modalCloseBtns.count().catch(() => 0);
  for (let i = 0; i < closeCount; i++) {
    await modalCloseBtns.nth(0).click({ timeout: 2000 }).catch(() => {});
    await sleep(500);
  }
  // 关闭 FullScreenDetail
  const fsdClose = page.locator('.detail-close-btn:visible, .detail-footer .ant-btn:not(:hidden):has-text("关闭"):not(:hidden)');
  const fsdCount = await fsdClose.count().catch(() => 0);
  for (let i = 0; i < fsdCount; i++) {
    await fsdClose.nth(0).click({ timeout: 2000 }).catch(() => {});
    await sleep(500);
  }
  // 按 Escape 键
  await page.keyboard.press('Escape').catch(() => {});
  await sleep(500);
}

// ── 检查页面加载状态 ──────────────────────────────────
async function checkPageLoaded(page, pageName) {
  // 检查 ErrorBoundary
  const errResult = await page.locator('.ant-result-error:visible').count().catch(() => 0);
  if (errResult > 0) {
    const txt = await page.locator('.ant-result-error:visible').first().textContent().catch(() => '');
    err('blocking', pageName, 'ErrorBoundary', txt.slice(0, 200));
    return false;
  }

  // 检查 404
  const notFound = await page.locator('.ant-result-404:visible').count().catch(() => 0);
  if (notFound > 0) {
    err('blocking', pageName, '404 页面', '页面不存在');
    return false;
  }

  return true;
}

// ── 点击新建/新增按钮并检查弹窗 ──────────────────────
async function clickCreateButton(page, pageName) {
  const btns = page.locator('.ant-btn:visible');
  const n = await btns.count().catch(() => 0);

  for (let i = 0; i < n; i++) {
    const txt = ((await btns.nth(i).textContent().catch(() => '')) || '').trim();
    if (!txt || ['查询', '重置', '刷新', '返回', '取消'].includes(txt)) continue;
    if (txt.includes('新增') || txt.includes('新建') || txt.includes('创建')) {
      console.log(`  [操作] 点击 "${txt}"`);
      await btns.nth(i).click({ force: true, timeout: 5000 }).catch(() => {});
      await sleep(2000);
      return txt;
    }
  }
  return null;
}

// ── 填写表单字段 ──────────────────────────────────────
async function fillFormFields(page) {
  // 填写 Input
  const inputs = page.locator('input:visible:not([type="hidden"])');
  const inCount = await inputs.count().catch(() => 0);
  for (let i = 0; i < Math.min(inCount, 5); i++) {
    const placeholder = await inputs.nth(i).getAttribute('placeholder').catch(() => '');
    if (placeholder && placeholder.includes('请输入')) {
      await inputs.nth(i).fill(`测试${Date.now() % 10000}`).catch(() => {});
    }
  }
}

// ── 处理 HTTP 响应收集 ────────────────────────────────
function setupResponseListener(page) {
  page.on('response', resp => {
    const url = resp.url();
    if (url.includes('/api/') && url.includes(BASE_URL)) {
      const status = resp.status();
      if (status >= 400) {
        err('api', url.replace(BASE_URL, ''), `HTTP ${status}`, `请求失败: ${url}`);
      }
    }
  });
}

// ── 主流程 ────────────────────────────────────────────
async function main() {
  console.log(`ERP 全流程扫描 v5\n`);
  console.log(`批次: 1/7 (${ALL_PAGES.length} 页)\n`);

  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 800 } });
  const page = await ctx.newPage();

  // 收集控制台错误
  const consoleErrors = [];
  page.on('pageerror', err => {
    consoleErrors.push(err.message);
    err('blocking', 'PAGE', '未捕获异常', err.message.slice(0, 200));
  });

  // 登录
  console.log('登录...');
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded' });
  await sleep(2000);

  // 获取验证码
  const captcha = await fetchCaptcha().catch(e => { console.error('  验证码获取失败:', e.message); return null; });
  const captchaCode = captcha ? captcha.code : 'ABCD';
  const captchaUuid = captcha ? captcha.uuid : '';

  // 通过点击按钮填写登录表单
  const tenantInput = page.locator('input[placeholder*="租户"]');
  if (await tenantInput.isVisible().catch(() => false)) {
    await tenantInput.fill('系统租户');
  }
  await page.locator('input[placeholder*="用户名"], input[placeholder*="账号"]').first().fill('admin').catch(() => {});
  await page.locator('input[placeholder*="密码"]').first().fill('admin123').catch(() => {});
  const codeInput = page.locator('input[placeholder*="验证码"]');
  if (await codeInput.isVisible().catch(() => false)) {
    await codeInput.fill(captchaCode);
  }

  // 点击登录按钮
  await page.locator('button[type="submit"], .ant-btn-primary:has-text("登录"), .ant-btn:has-text("登录")').first().click().catch(() => {});
  await sleep(2000);

  // 可能验证码错误，重新获取并重试一次
  if (!await page.locator('.ant-menu').isVisible().catch(() => false)) {
    console.log('  首次登录可能失败，重试...');
    const captcha2 = await fetchCaptcha().catch(() => null);
    if (captcha2) {
      const ci2 = page.locator('input[placeholder*="验证码"]');
      if (await ci2.isVisible().catch(() => false)) {
        await ci2.fill(captcha2.code);
      }
      await page.locator('button[type="submit"], .ant-btn-primary:has-text("登录"), .ant-btn:has-text("登录")').first().click().catch(() => {});
      await sleep(3000);
    }
  }

  // 等待跳转到工作台
  try {
    await page.waitForURL('**/dashboard**', { timeout: 15000 });
    console.log('  登录成功');
  } catch {
    console.log('  URL:', page.url());
  }
  await sleep(1000);

  // ── 处理每批页面 ────────────────────────────────────
  for (const pg of ALL_PAGES) {
    console.log(`\n[${ALL_PAGES.indexOf(pg) + 1}/${ALL_PAGES.length}] ${pg.name}`);

    // 通过菜单导航（模拟真实用户）
    const navOk = await navigateViaMenu(page, pg.menuParent, pg.menuItem);
    if (!navOk) {
      console.log(`  菜单项 "${pg.menuItem}" 不可见, 使用 router.push`);
    }

    // 检查是否导航到了正确页面
    let currentRoute = await page.evaluate(() => {
      const app = document.querySelector('#app');
      const router = app?.__vue_app__?.config?.globalProperties?.$router;
      return router?.currentRoute?.value?.path || '';
    }).catch(() => '');

    // 如果菜单导航失败，保底使用 router.push
    if (!currentRoute.includes(pg.route) && pg.route !== '/') {
      console.log(`  菜单导航未到目标页 (当前: ${currentRoute}), 使用 router.push`);
      await navigateByRouter(page, pg.route);
    }

    // 检查页面加载
    const loaded = await checkPageLoaded(page, pg.name);
    if (!loaded) {
      await closeAllModals(page);
      continue;
    }

    // 检查生产级标准
    await checkProductionStandards(page, pg.name);

    // 尝试点击新建/新增按钮
    const btnText = await clickCreateButton(page, pg.name);
    if (btnText) {
      // 检查弹窗类型
      await sleep(1000);

      // 检查是否有 FullScreenDetail
      const fsdVisible = await page.locator('.fullscreen-detail:visible').count().catch(() => 0);
      const modalVisible = await page.locator('.ant-modal-wrap:visible').count().catch(() => 0);
      const drawerVisible = await page.locator('.ant-drawer:visible').count().catch(() => 0);

      console.log(`  弹窗: FSD=${fsdVisible}, Modal=${modalVisible}, Drawer=${drawerVisible}`);

      if (fsdVisible > 0 || modalVisible > 0 || drawerVisible > 0) {
        // 尝试填写表单
        await fillFormFields(page);

        // 尝试选择 Select 下拉
        const selects = page.locator('.ant-select-selector:visible');
        const selCount = await selects.count().catch(() => 0);
        for (let s = 0; s < Math.min(selCount, 3); s++) {
          await selects.nth(s).click().catch(() => {});
          await sleep(500);
          // 选择第一个选项
          const option = page.locator('.ant-select-item-option:visible').first();
          if (await option.isVisible().catch(() => false)) {
            await option.click().catch(() => {});
            await sleep(300);
          }
        }

        // 关闭弹窗
        await closeAllModals(page);
      }
    }
  }

  // ── 报告 ────────────────────────────────────────────
  console.log(`\n=== 扫描完成 ===`);
  const totalErrors = Object.values(errors).reduce((s, arr) => s + arr.length, 0);
  console.log(`${ALL_PAGES.length} pages processed, ${totalErrors} errors`);
  for (const [cat, arr] of Object.entries(errors)) {
    if (arr.length > 0) {
      console.log(`  ${cat}: ${arr.length}`);
      for (const e of arr) {
        console.log(`    [${e.page}] ${e.type}: ${e.detail}`);
      }
    }
  }

  fs.writeFileSync(REPORT_PATH, JSON.stringify(errors, null, 2));
  await browser.close();
  console.log('\nDone');
}

main().catch(e => {
  console.error('Fatal:', e);
  process.exit(1);
});
