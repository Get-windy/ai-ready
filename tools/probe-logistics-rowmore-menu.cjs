// 抓取 ql361 物流公司「行内更多」下拉菜单（网格单元内 label.grid-cell-link，需真实鼠标事件）
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright');
const fs = require('fs');
const path = require('path');
const OUT = path.join(__dirname, '..', 'tool-results', 'ql361', 'pages');
const SHOT = path.join(__dirname, '..', 'tool-results', 'ql361', 'logistics-shots');
const log = (...a) => console.log('[probe]', ...a);

async function handleRelogin(page) {
  for (let i = 0; i < 3; i++) {
    const has = await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null;
      return [...document.querySelectorAll('.dojoxDialog,[class*=Dialog]')].filter(vis).some(e => (e.innerText || '').includes('重新登录'));
    }).catch(() => false);
    if (!has) return;
    await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null;
      const dlg = [...document.querySelectorAll('.dojoxDialog,[class*=Dialog]')].filter(vis).find(e => (e.innerText || '').includes('重新登录'));
      const pwd = dlg && dlg.querySelector('input[type=password]');
      if (pwd) { pwd.value = 'Ysh790921'; pwd.dispatchEvent(new Event('input', { bubbles: true })); }
      const ok = dlg && [...dlg.querySelectorAll('span,button')].find(b => (b.innerText || '').trim() === '确定');
      if (ok) ok.click();
    }).catch(() => {});
    await page.waitForTimeout(4000);
  }
}

const dumpMenus = (page) => page.evaluate(() => {
  const clean = s => (s || '').replace(/[ \t]+/g, ' ').replace(/\n{2,}/g, '\n').trim();
  const out = [];
  document.querySelectorAll('.dijitMenuPopup, .dijitPopup, .dijitMenu, [role=menu], .dijitTooltipDialogPopup').forEach(e => {
    const r = e.getBoundingClientRect();
    if (r.width < 8 || r.height < 5) return;
    out.push({
      cls: String(e.className).slice(0, 60),
      x: Math.round(r.x), y: Math.round(r.y), text: clean(e.innerText),
      items: [...e.querySelectorAll('.dijitMenuItem,[role=menuitem],tr,td')].map(i => clean(i.innerText)).filter(Boolean),
    });
  });
  return out;
});

(async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' });
  const page = await ctx.newPage();
  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'domcontentloaded', timeout: 60000 }).catch(() => {});
  await page.waitForTimeout(4000);
  await page.fill('input[placeholder="请输入手机号/用户名"]', '15095664266').catch(() => {});
  await page.fill('#pwd', 'Ysh790921').catch(() => {});
  for (let i = 0; i < 25; i++) {
    const busy = await page.evaluate(() => (document.body.innerText || '').includes('安全验证组件初始化中')).catch(() => false);
    if (!busy) break; await page.waitForTimeout(1000);
  }
  for (let a = 0; a < 4; a++) {
    await page.click('button.logon_button').catch(() => {});
    for (let i = 0; i < 20; i++) { await page.waitForTimeout(1000); if (!page.url().includes('/Account/Logon')) break; }
    if (!page.url().includes('/Account/Logon')) break;
  }
  await page.waitForTimeout(9000);
  await page.keyboard.press('Escape').catch(() => {});
  await handleRelogin(page);

  await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true }).catch(() => {});
  await page.waitForTimeout(2500);
  await page.evaluate(() => {
    const el = [...document.querySelectorAll('.popupmenu__nav-text')].find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '物流公司');
    if (el) el.click();
  }).catch(() => {});
  await page.waitForTimeout(8000);
  await handleRelogin(page);

  // 第 1 行行内「更多」
  const rect = await page.evaluate(() => {
    const norm = s => (s || '').replace(/\s+/g, '');
    const cands = [...document.querySelectorAll('.grid-cell-link, span, a, div')]
      .filter(e => !e.children.length && norm(e.innerText) === '更多' && e.getBoundingClientRect().width > 0)
      .filter(e => { const r = e.getBoundingClientRect(); return r.y > 180 || /grid-cell-link/.test(String(e.className)); });
    const el = cands[0];
    if (!el) return null;
    const r = el.getBoundingClientRect();
    return { x: Math.round(r.x + r.width / 2), y: Math.round(r.y + r.height / 2), cls: String(el.className).slice(0, 50) };
  });
  log('row 更多 rect:', JSON.stringify(rect));
  if (rect) {
    await page.mouse.move(rect.x, rect.y);
    await page.mouse.down();
    await page.mouse.up();
    await page.waitForTimeout(2500);
    await page.screenshot({ path: path.join(SHOT, '90-row-more.png') }).catch(() => {});
    let menus = await dumpMenus(page);
    log('AFTER CLICK:', JSON.stringify(menus));
    if (!menus.length) {
      // 再试右键
      await page.mouse.click(rect.x, rect.y, { button: 'right' });
      await page.waitForTimeout(2000);
      await page.screenshot({ path: path.join(SHOT, '91-row-more-right.png') }).catch(() => {});
      menus = await dumpMenus(page);
      log('AFTER RIGHT CLICK:', JSON.stringify(menus));
    }
    fs.writeFileSync(path.join(OUT, 'ziliao__logistics__rowmore.json'), JSON.stringify(menus, null, 2));
  }

  await browser.close();
  log('DONE');
})().catch(e => { console.error('FATAL', e); process.exit(1); });
