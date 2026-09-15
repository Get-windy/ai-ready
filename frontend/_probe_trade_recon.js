// 侦察脚本：登录 → dump 顶部菜单与「商城」弹出菜单结构 → 记录 iframe/网络
// 用法: node _probe_trade_recon.js
const { chromium } = require('playwright');
const fs = require('fs');
const OUT = process.argv[2] || '../tool-results/ql361/pages';

const sleep = (ms) => new Promise(r => setTimeout(r, ms));

(async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' });
  const page = await ctx.newPage();

  const netLog = [];
  page.on('response', async (res) => {
    try {
      const u = res.url();
      const ct = (res.headers()['content-type'] || '');
      if (/\/(api|ashx|aspx|json|Mall|Shop|Freight|Logistics)/i.test(u) && /json|javascript|text/i.test(ct)) {
        netLog.push({ status: res.status(), url: u.slice(0, 300), ct: ct.slice(0, 60) });
      }
    } catch (e) {}
  });
  page.on('requestfailed', (req) => {
    netLog.push({ FAILED: req.failure() ? req.failure().errorText : '?', url: req.url().slice(0, 300) });
  });
  page.on('console', (m) => { if (m.type() === 'error') netLog.push({ CONSOLE: m.text().slice(0, 200) }); });

  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'networkidle', timeout: 45000 }).catch(() => {});
  await page.fill('input[placeholder="请输入手机号/用户名"]', '15095664266').catch(() => {});
  await page.fill('#pwd', 'Ysh790921').catch(() => {});
  await page.click('button.logon_button').catch(() => {});
  for (let i = 0; i < 30; i++) { await sleep(1000); if (!page.url().includes('/Account/Logon')) break; }
  await sleep(8000);
  console.log('URL after login:', page.url());
  await page.keyboard.press('Escape').catch(() => {});
  await page.evaluate(() => { document.querySelectorAll('.lk-dialog-close,[aria-label=Close],[title=关闭]').forEach(b => b.click()); }).catch(() => {});

  // 顶部菜单
  const topMenus = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    return [...document.querySelectorAll('a.menu-0-item')].filter(vis).map(e => (e.innerText || '').trim().replace(/\s+/g, ''));
  }).catch(e => ['ERR ' + e.message]);
  console.log('TOP MENUS:', JSON.stringify(topMenus));

  // 点「商城」
  await page.locator('a.menu-0-item', { hasText: '商城' }).first().click({ force: true }).catch(() => {});
  await sleep(2500);
  const popup = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const texts = [...document.querySelectorAll('.popupmenu__nav-text')].filter(vis).map(e => (e.innerText || '').trim().replace(/\s+/g, ''));
    const allTitles = [...document.querySelectorAll('[class*=popupmenu] [class*=title], .popupmenu__nav-item, .popupmenu__group')].filter(vis)
      .map(e => ({ cls: String(e.className).slice(0, 70), t: (e.innerText || '').trim().replace(/\s+/g, ' ').slice(0, 80) }));
    const allNav = [...document.querySelectorAll('[class*=popupmenu] *')].filter(vis)
      .filter(e => e.children.length === 0 && (e.innerText || '').trim())
      .map(e => ({ cls: String(e.className).slice(0, 70), t: (e.innerText || '').trim().replace(/\s+/g, '').slice(0, 30) }));
    return { navTexts: texts, titles: allTitles, leaves: allNav.slice(0, 200) };
  }).catch(e => ({ err: e.message }));
  console.log('POPUP:', JSON.stringify(popup, null, 1).slice(0, 8000));

  await page.screenshot({ path: `${OUT}/_recon_mall_menu.png` }).catch(() => {});
  fs.writeFileSync(`${OUT}/_probe_trade_recon.json`, JSON.stringify({ topMenus, popup, netLog }, null, 1), 'utf-8');
  console.log('NETLOG entries:', netLog.length);
  console.log(JSON.stringify(netLog.slice(0, 60), null, 1));
  await browser.close();
  console.log('DONE');
})().catch(e => { console.error('FATAL', e.message); process.exit(1); });
