// 抓取 ql361 物流公司「新增」表单弹窗（v5：处理重新登录 + 精确布局 dump）
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const OUT = path.join(__dirname, '..', 'tool-results', 'ql361', 'pages');
const SHOT = path.join(__dirname, '..', 'tool-results', 'ql361', 'logistics-shots');
const STATE = path.join(__dirname, '..', 'tool-results', 'ql361', 'state-logistics.json');
fs.mkdirSync(SHOT, { recursive: true });
const log = (...a) => console.log('[probe]', ...a);

async function handleRelogin(page) {
  for (let i = 0; i < 3; i++) {
    const has = await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null;
      return [...document.querySelectorAll('.dojoxDialog,[class*=Dialog]')].filter(vis)
        .some(e => (e.innerText || '').includes('重新登录'));
    }).catch(() => false);
    if (!has) return;
    log('  -> relogin dialog, filling password');
    await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null;
      const dlg = [...document.querySelectorAll('.dojoxDialog,[class*=Dialog]')].filter(vis)
        .find(e => (e.innerText || '').includes('重新登录'));
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
  const ctx = await browser.newContext({
    viewport: { width: 1680, height: 950 }, locale: 'zh-CN',
    storageState: fs.existsSync(STATE) ? STATE : undefined,
  });
  const page = await ctx.newPage();

  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'domcontentloaded', timeout: 60000 }).catch(() => {});
  await page.waitForTimeout(5000);
  if (page.url().includes('/Account/Logon')) {
    await page.fill('input[placeholder="请输入手机号/用户名"]', '15095664266').catch(() => {});
    await page.fill('#pwd', 'Ysh790921').catch(() => {});
    for (let i = 0; i < 25; i++) {
      const busy = await page.evaluate(() => (document.body.innerText || '').includes('安全验证组件初始化中')).catch(() => false);
      if (!busy) break;
      await page.waitForTimeout(1000);
    }
    for (let a = 0; a < 4; a++) {
      await page.click('button.logon_button').catch(() => {});
      for (let i = 0; i < 20; i++) { await page.waitForTimeout(1000); if (!page.url().includes('/Account/Logon')) break; }
      if (!page.url().includes('/Account/Logon')) break;
    }
  }
  log('url:', page.url());
  await page.waitForTimeout(9000);
  await page.keyboard.press('Escape').catch(() => {});
  await handleRelogin(page);
  await ctx.storageState({ path: STATE }).catch(() => {});

  await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true }).catch(() => {});
  await page.waitForTimeout(3000);
  await page.evaluate(() => {
    const el = [...document.querySelectorAll('.popupmenu__nav-text')]
      .find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '物流公司');
    if (el) el.click();
  }).catch(() => {});
  await page.waitForTimeout(8000);
  await handleRelogin(page);

  // 物理点击「新增」
  const pt = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const x = [...document.querySelectorAll('span')].filter(vis).find(e => (e.innerText || '').trim() === '新增' && e.id.endsWith('_label'));
    if (!x) return null;
    const r = x.getBoundingClientRect();
    return { x: Math.round(r.x + r.width / 2), y: Math.round(r.y + r.height / 2) };
  }).catch(() => null);
  if (pt) await page.mouse.click(pt.x, pt.y);
  log('clicked add at', JSON.stringify(pt));
  await page.waitForTimeout(10000);
  await handleRelogin(page);
  await page.waitForTimeout(2000);
  await page.screenshot({ path: path.join(SHOT, '50-form.png') }).catch(() => {});

  // 精确定位表单弹窗并 dump 结构
  const form = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null && el.offsetWidth > 0;
    const clean = s => (s || '').replace(/[ \t]+/g, ' ').replace(/\n{2,}/g, '\n').trim();
    const dlg = [...document.querySelectorAll('.dojoxDialog')].filter(vis)
      .find(d => (d.innerText || '').includes('物流公司编号') && (d.innerText || '').includes('纳税人信息'));
    if (!dlg) return { found: false, dialogs: [...document.querySelectorAll('.dojoxDialog')].filter(vis).map(d => clean(d.innerText).slice(0, 120)) };

    // 找出所有 form-item 级元素（label + 控件）
    const labelNodes = [...dlg.querySelectorAll('label')].filter(vis);
    const fields = labelNodes.map(l => {
      const r = l.getBoundingClientRect();
      const t = l.closest('tr,div');
      const inp = l.parentElement ? l.parentElement.querySelector('input,textarea,select') : null;
      return { label: clean(l.innerText), x: Math.round(r.x), y: Math.round(r.y), w: Math.round(r.width), type: inp ? (inp.tagName.toLowerCase() + ':' + (inp.type || '')) : '' };
    });
    // 段落标题（网点1 / 纳税人信息 等加粗标题）
    const heads = [...dlg.querySelectorAll('*')].filter(vis)
      .filter(e => e.children.length === 0 && (e.innerText || '').trim() && /^(网点\d+|纳税人信息|基础信息)/.test((e.innerText || '').trim()))
      .map(e => { const r = e.getBoundingClientRect(); return { t: clean(e.innerText), x: Math.round(r.x), y: Math.round(r.y), cls: String(e.className).slice(0, 40) }; });
    return {
      found: true,
      text: clean(dlg.innerText),
      rect: (() => { const r = dlg.getBoundingClientRect(); return { w: Math.round(r.width), h: Math.round(r.height) }; })(),
      fields, heads,
      checkboxes: [...dlg.querySelectorAll('input[type=checkbox]')].filter(vis).map(c => ({ id: c.id, checked: c.checked })),
      buttons: [...dlg.querySelectorAll('span,button')].filter(vis).map(b => clean(b.innerText)).filter(t => t && t.length < 14),
    };
  }).catch(e => ({ err: String(e) }));
  fs.writeFileSync(path.join(OUT, 'ziliao__logistics__form.json'), JSON.stringify(form, null, 2));
  log('FORM found:', form.found, 'rect:', JSON.stringify(form.rect));
  log('FIELDS:', JSON.stringify((form.fields || []).map(f => `${f.label} @(${f.x},${f.y}) ${f.type}`), null, 0));
  log('HEADS:', JSON.stringify(form.heads));
  log('BUTTONS:', JSON.stringify([...new Set(form.buttons || [])]));
  log('TEXT:\n' + (form.text || '').slice(0, 2500));

  await browser.close();
  log('DONE');
})().catch(e => { console.error('FATAL', e); process.exit(1); });
