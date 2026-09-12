// ql361 商品条码 v3：稳健登录（等安全验证组件）+ 处理重新登录 + 抓下拉选项/列配置/行级修改
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const OUT = path.join(__dirname, '..', 'tool-results', 'ql361', 'pages');
const SHOT = path.join(__dirname, '..', 'tool-results', 'ql361', 'barcode-shots');
fs.mkdirSync(SHOT, { recursive: true });
const log = (...a) => console.log('[probe3]', ...a);
const wait = (p, ms) => p.waitForTimeout(ms);

async function heal(page) {
  for (let i = 0; i < 3; i++) {
    const healed = await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null;
      const dlgs = [...document.querySelectorAll('[class*=Dialog],[role=dialog],div')].filter(vis);
      const d = dlgs.find(x => (x.innerText || '').trim().startsWith('重新登录') && (x.innerText || '').length < 200);
      if (!d) return false;
      const inp = d.querySelector('input');
      if (inp) { inp.value = 'Ysh790921'; inp.dispatchEvent(new Event('input', { bubbles: true })); inp.dispatchEvent(new Event('change', { bubbles: true })); }
      const btn = [...d.querySelectorAll('button,a,span,div')].find(b => (b.innerText || '').trim() === '确定');
      if (btn) btn.click();
      return true;
    }).catch(() => false);
    if (!healed) return false;
    log('healed relogin dialog #' + (i + 1));
    await wait(page, 4000);
  }
  return true;
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' });
  const page = await ctx.newPage();
  page.on('console', m => { if (m.type() === 'error') log('console.error:', m.text().slice(0, 160)); });

  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'domcontentloaded', timeout: 60000 }).catch(() => {});
  await wait(page, 4000);
  // 等待安全验证组件就绪
  for (let i = 0; i < 25; i++) {
    const t = await page.locator('text=安全验证组件初始化中').count().catch(() => 0);
    if (!t) break;
    await wait(page, 1000);
  }
  await page.fill('input[placeholder="请输入手机号/用户名"]', '15095664266').catch(() => {});
  await page.waitForTimeout(1200);
  await page.fill('#pwd', 'Ysh790921').catch(() => {});
  await page.waitForTimeout(1500);
  await page.click('button.logon_button').catch(() => {});
  for (let i = 0; i < 40; i++) { await wait(page, 1000); if (!page.url().includes('/Account/Logon')) break; }
  await wait(page, 10000);
  await page.keyboard.press('Escape').catch(() => {});
  await page.evaluate(() => document.querySelectorAll('.lk-dialog-close,[aria-label=Close],[title=关闭],.dojoxDialogCloseIcon').forEach(b => b.click())).catch(() => {});
  await heal(page);
  log('LOGGED_IN', page.url());
  if (page.url().includes('/Account/Logon')) {
    await page.screenshot({ path: path.join(SHOT, 'v3-login-fail.png') }).catch(() => {});
    log('LOGIN FAILED - abort'); await browser.close(); return;
  }

  // 关闭其它标签
  await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    [...document.querySelectorAll('.dijitTabCloseButton, .dijitTabCloseButtonSingleton')].filter(vis).forEach(b => b.click());
  }).catch(() => {});
  await wait(page, 1500);

  // 资料 → 商品条码
  await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true }).catch(() => {});
  await wait(page, 2500);
  await page.evaluate(() => {
    const items = [...document.querySelectorAll('.popupmenu__nav-text')];
    const el = items.find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '商品条码');
    if (el) el.click();
  }).catch(() => {});
  await wait(page, 12000);
  await heal(page);
  await wait(page, 3000);
  await page.screenshot({ path: path.join(SHOT, 'v3-01-list.png'), fullPage: false }).catch(() => {});

  // 列表基础信息 + 行数据
  const list = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const panes = [...document.querySelectorAll('.dijitTabPane')].filter(vis);
    const panel = panes.find(p => (p.innerText || '').includes('上架状态')) || panes[0];
    if (!panel) return { found: false };
    const rows = [...panel.querySelectorAll('.dgrid-row, table tbody tr')].filter(vis).slice(0, 5)
      .map(tr => [...tr.querySelectorAll('td')].map(td => (td.innerText || '').trim().replace(/\s+/g, ' ')));
    return { found: true, rows, text: (panel.innerText || '').slice(0, 2500) };
  }).catch(e => ({ err: String(e) }));
  fs.writeFileSync(path.join(OUT, 'ziliao__barcode__rows.json'), JSON.stringify(list, null, 2), 'utf8');
  log('ROWS:', JSON.stringify(list.rows));
  log('TEXT head:', (list.text || '').slice(0, 600).replace(/\n+/g, ' | '));

  // 下拉：逐个 point 点击「条码/上架状态/显示状态」后的容器
  const ddInfo = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const panel = [...document.querySelectorAll('.dijitTabPane')].filter(vis).find(p => (p.innerText || '').includes('上架状态')) || document.body;
    const sels = [...panel.querySelectorAll('select')].filter(vis);
    const dijits = [...panel.querySelectorAll('.dijitSelect, .dijitComboBox, .dijitDropDownButton, .dijitValidationTextBox')].filter(vis).map(d => ({
      cls: String(d.className).slice(0, 60), id: d.id, text: (d.innerText || '').trim().slice(0, 30),
    }));
    return { selectCount: sels.length, dijits };
  }).catch(e => ({ err: String(e) }));
  fs.writeFileSync(path.join(OUT, 'ziliao__barcode__ddinfo.json'), JSON.stringify(ddInfo, null, 2), 'utf8');
  log('DD INFO:', JSON.stringify(ddInfo));

  // 依次点开每个 dijit 下拉（用 dojo registry 读 options）
  const opts = await page.evaluate(() => {
    try {
      const reg = require('dojo/_base/registry');
      const out = [];
      reg.forEach((id, w) => {
        if (!w) return;
        const cls = w.declaredClass || '';
        if (!/Select|ComboBox|DropDownButton|FilteringSelect/.test(cls)) return;
        let o = null;
        try { if (w.options) o = w.options.map(x => x.label || x.name || '').filter(Boolean); } catch (e) { }
        out.push({ id, cls, opts: o || [] });
      });
      return out;
    } catch (e) { return [{ err: String(e) }]; }
  }).catch(e => [{ err: String(e) }]);
  fs.writeFileSync(path.join(OUT, 'ziliao__barcode__ddopts.json'), JSON.stringify(opts, null, 2), 'utf8');
  log('DROPDOWN WIDGETS:', JSON.stringify(opts));

  // 列配置弹窗（多策略点击）
  const clicked = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const panel = [...document.querySelectorAll('.dijitTabPane')].filter(vis).find(p => (p.innerText || '').includes('上架状态')) || document.body;
    const gears = [...panel.querySelectorAll('.icon-shezhi2')].filter(vis);
    if (!gears.length) return 'no-gear';
    gears[0].scrollIntoView({ block: 'center' });
    gears[0].click();
    return 'clicked:' + gears.length;
  }).catch(e => 'err:' + e.message);
  log('GEAR CLICK:', clicked);
  await wait(page, 4000);
  await heal(page);
  await page.screenshot({ path: path.join(SHOT, 'v3-02-colcfg.png') }).catch(() => {});
  const colCfg = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const cands = [...document.querySelectorAll('div,section,table')].filter(vis)
      .filter(d => { const t = d.innerText || ''; return t.includes('全局配置') || t.includes('个人配置'); });
    cands.sort((a, b) => (a.innerText || '').length - (b.innerText || '').length);
    const win = cands[0];
    if (!win) return { found: false, texts: [...document.querySelectorAll('[role=dialog]')].map(d => (d.innerText || '').slice(0, 100)) };
    const rows = [...win.querySelectorAll('tr')].map(tr => {
      const tds = [...tr.querySelectorAll('td')].map(td => (td.innerText || '').trim().replace(/\s+/g, ''));
      const cb = tr.querySelector('input[type=checkbox]');
      return { tds, checked: cb ? cb.checked : null };
    }).filter(r => r.tds.length >= 2);
    return { found: true, text: (win.innerText || '').slice(0, 2000), rows };
  }).catch(e => ({ err: String(e) }));
  fs.writeFileSync(path.join(OUT, 'ziliao__barcode__colcfg3.json'), JSON.stringify(colCfg, null, 2), 'utf8');
  log('COLCFG found=', colCfg.found, 'rows=', JSON.stringify((colCfg.rows || []).map(r => r.tds.join('|') + (r.checked === null ? '' : r.checked ? '[x]' : '[ ]'))));
  await page.keyboard.press('Escape').catch(() => {});
  await wait(page, 1500);

  // 行级「修改」
  const modClicked = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const panel = [...document.querySelectorAll('.dijitTabPane')].filter(vis).find(p => (p.innerText || '').includes('上架状态')) || document.body;
    const a = [...panel.querySelectorAll('a,button,span')].filter(vis).find(x => (x.innerText || '').trim() === '修改');
    if (!a) return false;
    a.click(); return true;
  }).catch(e => 'err:' + e.message);
  log('MOD CLICK:', modClicked);
  await wait(page, 5000);
  await heal(page);
  await page.screenshot({ path: path.join(SHOT, 'v3-03-rowedit.png') }).catch(() => {});
  const edit = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const dlgs = [...document.querySelectorAll('[class*=Dialog],[role=dialog],[class*=window]')].filter(vis)
      .map(d => ({ text: (d.innerText || '').slice(0, 3000), cls: String(d.className).slice(0, 60), w: d.offsetWidth, h: d.offsetHeight }))
      .filter(d => d.text.length > 30);
    dlgs.sort((a, b) => b.text.length - a.text.length);
    const inputs = [...document.querySelectorAll('[class*=Dialog] input,[class*=Dialog] select')].filter(vis)
      .map(i => ({ name: i.name || i.id || '', ph: i.placeholder || '', val: i.value || '', type: i.type || i.tagName }));
    return { top: dlgs[0] || null, all: dlgs.map(d => ({ cls: d.cls, w: d.w, h: d.h })), inputs };
  }).catch(e => ({ err: String(e) }));
  fs.writeFileSync(path.join(OUT, 'ziliao__barcode__rowedit3.json'), JSON.stringify(edit, null, 2), 'utf8');
  log('ROWEDIT:', (edit.top && edit.top.text || '').slice(0, 800).replace(/\n+/g, ' | '));
  log('ROWEDIT inputs:', JSON.stringify(edit.inputs));

  await browser.close();
  log('DONE');
})().catch(e => { console.error('FATAL', e); process.exit(1); });
