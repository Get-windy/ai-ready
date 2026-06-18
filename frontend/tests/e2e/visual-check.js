/**
 * 视觉检查各页面截图
 */
const { chromium } = require('playwright');

async function main() {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1280, height: 800 } });

  // 登录
  await page.goto('http://localhost:5656/login', { waitUntil: 'domcontentloaded' });
  await new Promise(r => setTimeout(r, 1500));

  await page.fill('input[placeholder="请输入租户名称"]', '系统租户').catch(() => {});
  await page.fill('input[placeholder="请输入用户名"]', 'admin');
  await page.fill('input[placeholder="请输入密码"]', 'admin123');
  await page.fill('input[placeholder="请输入验证码"]', 'ABCD').catch(() => {});
  await page.click('button[type="submit"]');
  await page.waitForURL('**/dashboard**', { timeout: 20000 });
  await new Promise(r => setTimeout(r, 3000));

  // 截图关键页面
  const pages = [
    '/dashboard',
    '/sale',
    '/purchase',
    '/erp/product',
    '/crm/customer',
    '/system/user',
    '/erp/stock',
    '/erp/expense/application',
    '/finance',
    '/fixed-asset/asset',
    '/erp/partner',
    '/budget',
    '/supplier',
  ];

  for (const route of pages) {
    await page.evaluate((r) => {
      const app = document.querySelector('#app');
      if (app && app.__vue_app__) {
        const router = app.__vue_app__.config.globalProperties.$router;
        if (router) router.push(r);
      }
    }, route);
    await new Promise(r => setTimeout(r, 3000));

    const name = route.replace(/\//g, '-');
    await page.screenshot({ path: `screenshots/check-${name}.png` });
    console.log(`${route} -> ${name}.png (${page.url()})`);
  }

  await browser.close();
}

main().catch(e => console.error(e));
