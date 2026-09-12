// 抓取 ql361 物流公司工具栏「更多」下拉菜单（dojo DropDownButton）
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
  log('url:', page.url());

  await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true }).catch(() => {});
  await page.waitForTimeout(2500);
  await page.evaluate(() => {
    const el = [...document.querySelectorAll('.popupmenu__nav-text')].find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '物流公司');
    if (el) el.click();
  }).catch(() => {});
  await page.waitForTimeout(8000);
  await handleRelogin(page);

  // 用 dojo registry 精确触发工具栏「更多」下拉
  const fired = await page.evaluate(() => {
    const w = window;
    const reg = w.require && w.require('dijit/registry');
    if (!reg) return 'no-registry';
    const all = reg.toArray ? reg.toArray() : [];
    const cands = all.filter(x => x.declaredClass && /DropDownButton|MenuButton/.test(x.declaredClass));
    const info = cands.map(x => ({ cls: x.declaredClass, label: x.label || (x.titleNode && x.titleNode.innerText) || '' }));
    const target = cands.find(x => ((x.label || (x.titleNode && x.titleNode.innerText) || '').trim() === '更多'));
    if (!target) return { info };
    target.toggleDropDown ? target.toggleDropDown() : target.openDropDown();
    return { info, opened: true };
  }).catch(e => ({ err: String(e) }));
  log('DOJO:', JSON.stringify(fired).slice(0, 400));
  await page.waitForTimeout(2000);
  await page.screenshot({ path: path.join(SHOT, '80-toolbar-more.png') }).catch(() => {});

  const menu = await page.evaluate(() => {
    const clean = s => (s || '').replace(/[ \t]+/g, ' ').replace(/\n{2,}/g, '\n').trim();
    const out = [];
    document.querySelectorAll('.dijitMenuPopup, .dijitPopup, .dijitMenu, [role=menu]').forEach(e => {
      const r = e.getBoundingClientRect();
      if (r.width < 10 || r.height < 5) return;
      out.push({ cls: String(e.className).slice(0, 60), x: Math.round(r.x), y: Math.round(r.y), w: Math.round(r.width), h: Math.round(r.height), text: clean(e.innerText), items: [...e.querySelectorAll('.dijitMenuItem,[role=menuitem]')].map(i => clean(i.innerText)).filter(Boolean) });
    });
    return out;
  }).catch(e => [{ err: String(e) }]);
  fs.writeFileSync(path.join(OUT, 'ziliao__logistics__moremenu.json'), JSON.stringify(menu, null, 2));
  log('MENU:', JSON.stringify(menu));

  await browser.close();
  log('DONE');
})().catch(e => { console.error('FATAL', e); process.exit(1); });
