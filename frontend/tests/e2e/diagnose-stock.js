/**
 * 诊断 /erp/stock 页面问题
 */
const { chromium } = require('playwright');

async function main() {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } });

  // 收集错误
  page.on('console', msg => {
    if (msg.type() === 'error') console.log('  [ERR]', msg.text().slice(0, 200));
  });
  page.on('pageerror', err => console.log('  [PAGE_ERR]', err.message));
  page.on('response', resp => {
    if (resp.status() >= 400) console.log('  [HTTP]', resp.status(), resp.url().slice(0, 100));
  });

  // 登录
  await page.goto('http://localhost:5656/login', { waitUntil: 'domcontentloaded' });
  await new Promise(r => setTimeout(r, 1500));

  if (await page.locator('input[placeholder="请输入租户名称"]').isVisible({ timeout: 3000 }).catch(() => false))
    await page.fill('input[placeholder="请输入租户名称"]', '系统租户');
  await page.fill('input[placeholder="请输入用户名"]', 'admin');
  await page.fill('input[placeholder="请输入密码"]', 'admin123');
  if (await page.locator('input[placeholder="请输入验证码"]').isVisible({ timeout: 2000 }).catch(() => false))
    await page.fill('input[placeholder="请输入验证码"]', 'ABCD');
  await page.click('button[type="submit"]');
  await page.waitForURL('**/dashboard**', { timeout: 20000 });
  await new Promise(r => setTimeout(r, 3000));

  console.log('已登录\n');

  // 测试导航到 /erp/stock
  console.log('1. 直接导航到 /erp/stock...');
  await page.goto('http://localhost:5656/erp/stock', { waitUntil: 'domcontentloaded', timeout: 15000 });
  await new Promise(r => setTimeout(r, 5000));
  console.log('   URL:', page.url());
  console.log('   标题:', await page.title());

  const body = await page.locator('body').textContent().catch(() => '');
  console.log('   内容长度:', body ? body.length : 0);

  // 检查是否有表格
  const antTable = page.locator('.ant-table');
  console.log('   ant-table:', await antTable.count().catch(() => 0));

  const spinning = page.locator('.ant-spin-spinning');
  console.log('   spinning:', await spinning.count().catch(() => 0));

  if (await spinning.count() > 0) {
    console.log('   等待Spin消失...');
    try {
      await spinning.waitFor({ state: 'hidden', timeout: 15000 });
      console.log('   Spin已消失');
    } catch {
      console.log('   Spin超时未消失');
    }
  }

  console.log('\n2. 尝试Vue Router导航到 /erp/stock...');
  await page.evaluate(() => {
    const app = document.querySelector('#app');
    if (app && app.__vue_app__) {
      const router = app.__vue_app__.config.globalProperties.$router;
      if (router) router.push('/erp/stock');
    }
  });
  await new Promise(r => setTimeout(r, 5000));
  console.log('   URL:', page.url());

  console.log('\n3. 尝试导航到 /erp/batch...');
  await page.evaluate(() => {
    const app = document.querySelector('#app');
    if (app && app.__vue_app__) {
      const router = app.__vue_app__.config.globalProperties.$router;
      if (router) router.push('/erp/batch');
    }
  });
  await new Promise(r => setTimeout(r, 3000));
  console.log('   URL:', page.url());

  console.log('\n诊断完成');
  await browser.close();
}

main().catch(e => { console.error(e); process.exit(1); });
