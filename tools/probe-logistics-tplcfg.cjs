// 抓取 ql361 物流公司 行内「更多 → 模板配置」弹窗内容
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

  await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true }).catch(() => {});
  await page.waitForTimeout(2500);
  await page.evaluate(() => {
    const el = [...document.querySelectorAll('.popupmenu__nav-text')].find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '物流公司');
    if (el) el.click();
  }).catch(() => {});
  await page.waitForTimeout(8000);
  await handleRelogin(page);

  // 打开第 1 行「更多」（真实鼠标事件，先点 更多 再点 模板配置）
  const pos = await page.evaluate(() => {
    const norm = s => (s || '').replace(/\s+/g, '');
    const cands = [...document.querySelectorAll('.grid-cell-link')].filter(e => norm(e.innerText) === '更多');
    const el = cands[0];
    if (!el) return null;
    const r = el.getBoundingClientRect();
    return { x: Math.round(r.x + r.width / 2), y: Math.round(r.y + r.height / 2) };
  });
  log('更多 at', JSON.stringify(pos));
  if (!pos) { await browser.close(); return; }
  await page.mouse.move(pos.x, pos.y);
  await page.mouse.down(); await page.mouse.up();
  await page.waitForTimeout(1500);
  await page.screenshot({ path: path.join(SHOT, '92-row-menu.png') }).catch(() => {});

  // 点「模板配置」
  const tpl = await page.evaluate(() => {
    const norm = s => (s || '').replace(/\s+/g, '');
    const el = [...document.querySelectorAll('*')].filter(e => !e.children.length && norm(e.innerText) === '模板配置')
      .map(e => { const r = e.getBoundingClientRect(); return { x: Math.round(r.x + r.width / 2), y: Math.round(r.y + r.height / 2), w: Math.round(r.width) } })
      .filter(e => e.w > 0)[0];
    return el || null;
  });
  log('模板配置 at', JSON.stringify(tpl));
  if (tpl) {
    await page.mouse.move(tpl.x, tpl.y);
    await page.mouse.down(); await page.mouse.up();
    await page.waitForTimeout(5000);
    await handleRelogin(page);
    await page.screenshot({ path: path.join(SHOT, '93-tpl-config.png') }).catch(() => {});
    const info = await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null && el.offsetWidth > 2;
      const clean = s => (s || '').replace(/[ \t]+/g, ' ').replace(/\n{2,}/g, '\n').trim();
      const dlgs = [...document.querySelectorAll('.dojoxDialog,.lkgrid-dialog,[class*=Dialog]')].filter(vis)
        .map(e => {
          const r = e.getBoundingClientRect();
          return { cls: String(e.className).slice(0, 60), w: Math.round(r.width), h: Math.round(r.height), text: clean(e.innerText).slice(0, 1500),
            buttons: [...e.querySelectorAll('span,button')].filter(vis).map(b => clean(b.innerText)).filter(t => t && t.length < 16) };
        }).filter(d => d.text.length > 5);
      return dlgs;
    }).catch(e => [{ err: String(e) }]);
    fs.writeFileSync(path.join(OUT, 'ziliao__logistics__tplcfg.json'), JSON.stringify(info, null, 2));
    log('TPL CFG:', JSON.stringify(info.map(d => ({ cls: d.cls, w: d.w, h: d.h, text: d.text, buttons: [...new Set(d.buttons || [])] })).slice(0, 3)));
  }

  await browser.close();
  log('DONE');
})().catch(e => { console.error('FATAL', e); process.exit(1); });
