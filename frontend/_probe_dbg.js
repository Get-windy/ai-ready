const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' });
  const page = await ctx.newPage();
  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'networkidle', timeout: 45000 }).catch(()=>{});
  await page.fill('input[placeholder="请输入手机号/用户名"]', '15095664266').catch(()=>{});
  await page.fill('#pwd', 'Ysh790921').catch(()=>{});
  await page.click('button.logon_button').catch(()=>{});
  for (let i = 0; i < 25; i++) { await page.waitForTimeout(1000); if (!page.url().includes('/Account/Logon')) break; }
  await page.waitForTimeout(7000);
  await page.keyboard.press('Escape').catch(()=>{});
  await page.locator('a.menu-0-item', { hasText: '仓储' }).first().click({ force: true }).catch(()=>{});
  await page.waitForTimeout(1500);
  await page.evaluate(() => {
    const els = [...document.querySelectorAll('.popupmenu__nav-text')];
    const el = els.find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '借出单');
    if (el) el.click();
  }).catch(()=>{});
  await page.waitForTimeout(6000);
  // 截图
  await page.screenshot({ path: '../tool-results/ql361/dbg-borrowout.png' });
  // dump 页面结构
  const info = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const tabs = [...document.querySelectorAll('.dijitTab')].filter(vis).map(t => ({ cls: String(t.className).slice(0,60), text: (t.innerText||'').slice(0,20) }));
    const panes = [...document.querySelectorAll('.dijitTabPane')].filter(vis).length;
    const titleShezhi = [...document.querySelectorAll('[class*=title] [class*=shezhi]')].filter(vis).length;
    const gridShezhi = [...document.querySelectorAll('.dgrid-column-set .icon-shezhi2')].filter(vis).length;
    const configBtns = [...document.querySelectorAll('button')].filter(vis).filter(b => (b.innerText||'').includes('配置')).map(b => (b.innerText||'').slice(0,10));
    return { tabs, panes, titleShezhi, gridShezhi, configBtns };
  });
  console.log(JSON.stringify(info, null, 1));
  await browser.close();
})().catch(e => { console.error('FATAL', e.message); process.exit(1); });
