// ql361 商品条码 v6：只抓 3 个查询下拉选项（登录带重试）
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const OUT = path.join(__dirname, '..', 'tool-results', 'ql361', 'pages');
const SHOT = path.join(__dirname, '..', 'tool-results', 'ql361', 'barcode-shots');
fs.mkdirSync(SHOT, { recursive: true });
const log = (...a) => console.log('[probe6]', ...a);
const wait = (p, ms) => p.waitForTimeout(ms);

async function tryLogin(page) {
  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'domcontentloaded', timeout: 60000 }).catch(() => {});
  await wait(page, 4000);
  for (let i = 0; i < 20; i++) { const t = await page.locator('text=安全验证组件初始化中').count().catch(() => 0); if (!t) break; await wait(page, 1000); }
  await page.fill('input[placeholder="请输入手机号/用户名"]', '15095664266').catch(() => {});
  await wait(page, 1000);
  await page.fill('#pwd', 'Ysh790921').catch(() => {});
  await wait(page, 1200);
  await page.click('button.logon_button').catch(() => {});
  for (let i = 0; i < 25; i++) { await wait(page, 1000); if (!page.url().includes('ogon')) break; }
  return !page.url().includes('ogon');
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' });
  const page = await ctx.newPage();

  let ok = false;
  for (let attempt = 1; attempt <= 3 && !ok; attempt++) {
    ok = await tryLogin(page);
    log(`login attempt ${attempt}:`, ok, page.url());
    if (!ok) await wait(page, 20000);
  }
  if (!ok) { await page.screenshot({ path: path.join(SHOT, 'v6-login-fail.png') }).catch(() => {}); await browser.close(); return; }

  await wait(page, 8000);
  await page.keyboard.press('Escape').catch(() => {});
  await page.evaluate(() => document.querySelectorAll('.lk-dialog-close,[aria-label=Close],[title=关闭],.dojoxDialogCloseIcon').forEach(b => b.click())).catch(() => {});
  await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    [...document.querySelectorAll('.dijitTabCloseButton, .dijitTabCloseButtonSingleton')].filter(vis).forEach(b => b.click());
  }).catch(() => {});
  await wait(page, 1500);

  await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true }).catch(() => {});
  await wait(page, 2500);
  await page.evaluate(() => {
    const items = [...document.querySelectorAll('.popupmenu__nav-text')];
    const el = items.find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '商品条码');
    if (el) el.click();
  }).catch(() => {});
  await wait(page, 14000);
  await page.screenshot({ path: path.join(SHOT, 'v6-01-list.png') }).catch(() => {});

  const selects = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const panel = [...document.querySelectorAll('.dijitTabPane')].filter(vis).find(p => (p.innerText || '').includes('上架状态')) || document.body;
    return [...panel.querySelectorAll('.dijitSelect')].filter(vis).map(d => ({ id: d.id, text: (d.innerText || '').trim().replace(/\s+/g, ' ') }));
  }).catch(() => []);
  fs.writeFileSync(path.join(OUT, 'ziliao__barcode__selects6.json'), JSON.stringify(selects, null, 2), 'utf8');
  log('SELECTS:', JSON.stringify(selects));

  const result = {};
  for (const it of selects) {
    if (!it.id) continue;
    await page.locator('#' + it.id).click({ force: true }).catch(() => {});
    await wait(page, 1800);
    const menu = await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null;
      const ms = [...document.querySelectorAll('.dijitMenuPopup, .dijitComboBoxMenuPopup, .dijitMenu, .dijitPopup')].filter(vis);
      const out = [];
      ms.forEach(m => [...m.querySelectorAll('.dijitMenuItem, .dijitMenuItemLabel, td, div')].forEach(t => {
        const s = (t.innerText || '').trim().replace(/\s+/g, ' ');
        if (s && s.length < 24 && !out.includes(s)) out.push(s);
      }));
      return out;
    }).catch(() => []);
    result[it.text] = menu;
    log('DD', it.text, '=>', JSON.stringify(menu));
    await page.mouse.click(900, 300).catch(() => {}); // 点页面空白关闭下拉
    await wait(page, 900);
  }
  fs.writeFileSync(path.join(OUT, 'ziliao__barcode__dd6.json'), JSON.stringify(result, null, 2), 'utf8');

  // 行级「修改」
  await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const panel = [...document.querySelectorAll('.dijitTabPane')].filter(vis).find(p => (p.innerText || '').includes('上架状态')) || document.body;
    const a = [...panel.querySelectorAll('a,button,span')].filter(vis).find(x => (x.innerText || '').trim() === '修改');
    if (a) a.click();
  }).catch(() => {});
  await wait(page, 6000);
  await page.screenshot({ path: path.join(SHOT, 'v6-02-rowedit.png') }).catch(() => {});
  const edit = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const dlgs = [...document.querySelectorAll('.dijitDialog, [role=dialog]')].filter(vis)
      .map(d => ({ text: (d.innerText || '').slice(0, 1500) }))
      .filter(d => d.text.length > 20 && !d.text.includes('列名'));
    dlgs.sort((a, b) => b.text.length - a.text.length);
    const fields = [...document.querySelectorAll('.dijitDialog input, .dijitDialog select')].filter(vis)
      .map(i => ({ tag: i.tagName, type: i.type || '', ph: i.placeholder || '', val: (i.value || '').slice(0, 40) }));
    return { top: dlgs[0] || null, fields };
  }).catch(e => ({ err: String(e) }));
  fs.writeFileSync(path.join(OUT, 'ziliao__barcode__rowedit6.json'), JSON.stringify(edit, null, 2), 'utf8');
  log('ROWEDIT:', (edit.top && edit.top.text || 'none').replace(/\n+/g, ' | ').slice(0, 600));

  await browser.close();
  log('DONE');
})().catch(e => { console.error('FATAL', e); process.exit(1); });
