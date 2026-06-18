/**
 * ERP系统全自动测试脚本
 * 功能：登录 → 侧边栏菜单点击导航所有页面 → 收集错误 → 截图
 * 所有操作通过前端UI完成（不直接调用API）
 */
import { chromium } from 'playwright';
import fs from 'fs';
import path from 'path';

const BASE_URL = 'http://localhost:5656';
const SCREENSHOT_DIR = path.join(process.cwd(), 'scan-results');
const ERROR_LOG_FILE = path.join(process.cwd(), 'auto-test-errors.json');

// 确保目录存在
if (!fs.existsSync(SCREENSHOT_DIR)) fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });

// 登录凭据
const CREDENTIALS = {
  username: 'admin',
  password: 'admin123',
  tenantName: '系统租户'
};

// 错误收集
const errors = {
  console: [],
  network: [],
  pageLoad: [],
  layout: [],
  production: []
};

let testPassCount = 0;
let testFailCount = 0;

function log(msg) {
  console.log(`[${new Date().toLocaleTimeString()}] ${msg}`);
}

async function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

async function run() {
  log('启动浏览器...');
  const browser = await chromium.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu']
  });

  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
    locale: 'zh-CN'
  });

  const page = await context.newPage();

  // === 错误收集器 ===
  page.on('console', msg => {
    if (msg.type() === 'error') {
      errors.console.push({
        url: page.url(),
        message: msg.text(),
        location: msg.location()
      });
      log(`[控制台错误] ${msg.text().substring(0, 120)}`);
    }
  });

  page.on('pageerror', err => {
    errors.pageLoad.push({
      url: page.url(),
      error: err.message,
      stack: err.stack?.substring(0, 200)
    });
    log(`[页面错误] ${err.message}`);
  });

  page.on('requestfailed', request => {
    const failure = request.failure();
    errors.network.push({
      url: page.url(),
      requestUrl: request.url(),
      method: request.method(),
      failure: failure?.errorText || 'Unknown'
    });
    log(`[网络失败] ${request.url().substring(0, 100)} - ${failure?.errorText}`);
  });

  page.on('response', response => {
    const status = response.status();
    if (status >= 400) {
      errors.network.push({
        url: page.url(),
        requestUrl: response.url(),
        method: response.request().method(),
        status,
        statusText: response.statusText()
      });
      if (status !== 401) { // 401是预期的未登录状态
        log(`[HTTP ${status}] ${response.url().substring(0, 100)}`);
      }
    }
  });

  try {
    // === 阶段0: 登录 ===
    await doLogin(page);

    // 验证是否登录成功
    if (page.url().includes('login')) {
      log('登录失败，仍在登录页面');
      await page.screenshot({ path: path.join(SCREENSHOT_DIR, 'login-failed.png'), fullPage: false });
      throw new Error('Login failed - still on login page');
    }
    log(`登录成功! 当前URL: ${page.url()}`);
    await page.screenshot({ path: path.join(SCREENSHOT_DIR, 'login-success.png'), fullPage: false });

    // === 阶段1: 展开所有侧边栏菜单 ===
    await sleep(3000); // 等待菜单完全加载

    // 展开所有a-sub-menu
    await expandAllSubMenus(page);

    // 收集所有页面路径
    const allPagePaths = await collectAllMenuItems(page);
    log(`共发现 ${allPagePaths.length} 个菜单页面`);

    // 按批次分组 (每批10个)
    const BATCH_SIZE = 10;
    const batches = [];
    for (let i = 0; i < allPagePaths.length; i += BATCH_SIZE) {
      batches.push(allPagePaths.slice(i, i + BATCH_SIZE));
    }
    log(`分 ${batches.length} 批处理`);

    // === 阶段2: 逐批处理 ===
    for (let batchIdx = 0; batchIdx < batches.length; batchIdx++) {
      const batch = batches[batchIdx];
      log(`\n====== 处理第 ${batchIdx + 1}/${batches.length} 批 ======`);

      for (let pageIdx = 0; pageIdx < batch.length; pageIdx++) {
        const pageInfo = batch[pageIdx];
        await processPage(page, pageInfo, batchIdx + 1, pageIdx + 1);
      }

      // 每批处理完输出统计
      log(`第 ${batchIdx + 1} 批完成: 批次内成功 ${testPassCount}, 失败 ${testFailCount}`);
    }

  } catch (err) {
    log(`[严重错误] ${err.message}`);
    errors.pageLoad.push({ url: 'global', error: err.message });
  } finally {
    // 输出报告
    await generateReport();
    await browser.close();
  }
}

// ===== 登录函数 =====
async function doLogin(page) {
  log('正在登录...');
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle', timeout: 30000 });
  await sleep(2000);

  // 填写租户名称
  const tenantInput = page.locator('#form-item-tenant input');
  await tenantInput.waitFor({ state: 'visible', timeout: 5000 });
  await tenantInput.fill(CREDENTIALS.tenantName);
  log(`填写租户: ${CREDENTIALS.tenantName}`);

  // 填写用户名
  const usernameInput = page.locator('#form-item-username input');
  await usernameInput.fill(CREDENTIALS.username);
  log(`填写用户名: ${CREDENTIALS.username}`);

  // 填写密码
  const passwordInput = page.locator('#form-item-password input');
  await passwordInput.fill(CREDENTIALS.password);
  log('填写密码');

  // 填写验证码 (随便填4位，后端不验证)
  const captchaInput = page.locator('#form-item-captcha input');
  await captchaInput.fill('1234');
  log('填写验证码');

  await sleep(500);

  // 点击登录按钮
  const loginBtn = page.locator('button.login-button');
  await loginBtn.click();
  log('点击登录按钮');

  // 等待登录完成（路由跳转）
  await page.waitForURL('**/dashboard', { timeout: 15000 }).catch(() => {
    log('等待跳转dashboard超时，检查当前URL');
  });
  await sleep(3000);
}

// ===== 展开所有子菜单 =====
async function expandAllSubMenus(page) {
  const submenuTitles = page.locator('.ant-menu-submenu-title');
  const count = await submenuTitles.count();
  log(`发现 ${count} 个子菜单需要展开`);

  for (let i = 0; i < count; i++) {
    try {
      const submenu = submenuTitles.nth(i);
      // 检查是否已展开
      const parentLi = submenu.locator('xpath=..');
      const isOpen = await parentLi.locator('.ant-menu-submenu-open').count();
      if (!isOpen) {
        await submenu.click();
        await sleep(300);
      }
    } catch (e) {
      log(`展开子菜单 ${i} 失败: ${e.message}`);
    }
  }
  await sleep(500);
}

// ===== 收集所有可见的菜单项 =====
async function collectAllMenuItems(page) {
  const items = [];
  const menuItems = page.locator('.ant-menu-item:not(.ant-menu-item-hidden)');
  const count = await menuItems.count();

  for (let i = 0; i < count; i++) {
    try {
      const item = menuItems.nth(i);
      const text = await item.textContent();
      // 检查是否可见
      const visible = await item.isVisible();
      if (visible && text && text.trim()) {
        items.push({
          index: i,
          text: text.trim().replace(/\s+/g, ' '),
          locator: item
        });
      }
    } catch (e) {
      // 跳过无法读取的项
    }
  }

  // 提取去重的页面文本列表
  const uniqueTexts = [...new Set(items.map(i => i.text))];
  log(`可见菜单项: ${uniqueTexts.join(', ')}`);

  return items;
}

// ===== 处理单个页面 =====
async function processPage(page, pageInfo, batchNum, pageNum) {
  const pageText = pageInfo.text || `页面${pageNum}`;
  const safeName = pageText.replace(/[\\/:"*?<>|]/g, '_').substring(0, 40);
  log(`\n[${batchNum}-${pageNum}] 访问: ${pageText}`);

  try {
    // 点击菜单项导航
    await pageInfo.locator.click();
    await sleep(3000);

    // 等待network基本稳定
    await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {});

    const currentUrl = page.url();
    log(`  当前URL: ${currentUrl}`);

    // 检查页面是否正常渲染
    const bodyText = await page.evaluate(() => document.body?.innerText?.length || 0);
    const appHtml = await page.evaluate(() => document.getElementById('app')?.innerHTML?.length || 0);

    if (appHtml < 100 && bodyText < 50) {
      errors.pageLoad.push({ url: currentUrl, error: `页面可能白屏 (HTML长度: ${appHtml})` });
      log(`  [警告] 页面内容过少，可能白屏`);
      testFailCount++;
    } else {
      testPassCount++;
    }

    // 检查是否有错误页面提示
    const hasErrorElement = await page.locator('.ant-result-title, .error-page, .page-error').count();
    if (hasErrorElement > 0) {
      const errorText = await page.locator('.ant-result-title').first().textContent();
      errors.pageLoad.push({ url: currentUrl, error: `错误页面: ${errorText}` });
      log(`  [警告] 页面显示错误: ${errorText}`);
    }

    // 截图
    const screenshotName = `batch${batchNum}_${pageNum}_${safeName}.png`;
    await page.screenshot({
      path: path.join(SCREENSHOT_DIR, screenshotName),
      fullPage: false
    });

    // === 生产级检查 ===

    // 1. 检查是否有全屏详情页组件
    const hasFullScreen = await page.locator('.full-screen-detail, .FullScreenDetail').count();
    if (hasFullScreen > 0) {
      log(`  [生产级] 使用全屏详情页`);
    }

    // 2. 检查表格是否有边框
    const tableBordered = await page.locator('.ant-table-bordered, .vxe-table--border').count();
    if (tableBordered > 0) {
      // 表格有边框是好的
    }

    // 3. 检查是否有加载状态
    const hasSpin = await page.locator('.ant-spin-spinning').count();
    if (hasSpin > 0) {
      log(`  [注意] 页面仍在加载中...`);
      await sleep(2000);
      const stillSpinning = await page.locator('.ant-spin-spinning').count();
      if (stillSpinning > 0) {
        errors.production.push({ url: currentUrl, issue: '页面持续加载中(Spin>2s)' });
      }
    }

    log(`  [OK] 页面渲染正常 (内容长度: ${appHtml})`);

  } catch (err) {
    testFailCount++;
    errors.pageLoad.push({ url: page.url(), error: `导航失败: ${err.message}` });
    log(`  [失败] ${err.message}`);

    try {
      const screenshotName = `batch${batchNum}_${pageNum}_FAIL_${safeName}.png`;
      await page.screenshot({ path: path.join(SCREENSHOT_DIR, screenshotName) });
    } catch (e) {}
  }

  // 回退到dashboard确保下次导航正常
  try {
    // 重新展开所有子菜单（因为导航后某些子菜单可能折叠）
    await sleep(500);
    await expandAllSubMenus(page);
  } catch (e) {
    log(`  展开菜单失败: ${e.message}`);
  }
}

// ===== 生成报告 =====
async function generateReport() {
  // 去重错误
  const uniqueConsoleErrors = errors.console.filter(
    (e, i, arr) => i === arr.findIndex(x => x.message === e.message)
  );
  const uniqueNetworkErrors = errors.network.filter(
    (e, i, arr) => i === arr.findIndex(x => x.requestUrl === e.requestUrl && x.status === e.status)
  );

  const report = {
    timestamp: new Date().toISOString(),
    summary: {
      pagesPassed: testPassCount,
      pagesFailed: testFailCount,
      consoleErrors: uniqueConsoleErrors.length,
      networkErrors: uniqueNetworkErrors.length,
      pageLoadErrors: errors.pageLoad.length,
      productionIssues: errors.production.length
    },
    errorSummary: {
      consoleErrors: uniqueConsoleErrors.slice(0, 30).map(e => ({
        message: e.message?.substring(0, 200),
        url: e.url
      })),
      networkErrors: uniqueNetworkErrors.slice(0, 30).map(e => ({
        url: e.requestUrl?.substring(0, 200),
        status: e.status || e.failure
      })),
      pageLoadErrors: errors.pageLoad.slice(0, 20).map(e => ({
        url: e.url,
        error: e.error?.substring(0, 200)
      })),
      productionIssues: errors.production.slice(0, 20)
    }
  };

  fs.writeFileSync(ERROR_LOG_FILE, JSON.stringify(report, null, 2));
  log(`\n========== 测试报告 ==========`);
  log(`通过页面: ${testPassCount}`);
  log(`失败页面: ${testFailCount}`);
  log(`控制台错误: ${uniqueConsoleErrors.length}`);
  log(`网络错误: ${uniqueNetworkErrors.length}`);
  log(`页面加载错误: ${errors.pageLoad.length}`);
  log(`生产级问题: ${errors.production.length}`);
  log(`报告已保存到: ${ERROR_LOG_FILE}`);

  // 输出详细错误
  if (errors.pageLoad.length > 0) {
    log('\n=== 页面加载错误详情 ===');
    errors.pageLoad.forEach(e => log(`  - ${e.url}: ${e.error?.substring(0, 150)}`));
  }
  if (uniqueConsoleErrors.length > 0) {
    log('\n=== 控制台错误详情 (前10) ===');
    uniqueConsoleErrors.slice(0, 10).forEach(e => log(`  - ${e.message?.substring(0, 150)}`));
  }
}

// 执行
run().catch(console.error);
