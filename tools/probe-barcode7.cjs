// ql361 商品条码 v7：抓「条码/上架状态/显示状态」下拉真实选项 + 行级「修改」弹窗真实字段
const { chromium } = require('playwright');
const { loginQl361 } = require('./ql361-lib.cjs');
const fs = require('fs');
const path = require('path');

const OUT = path.join(__dirname, '..', 'tool-results', 'ql361', 'pages');
const SHOT = path.join(__dirname, '..', 'tool-results', 'ql361', 'barcode-shots');
fs.mkdirSync(SHOT, { recursive: true });
const log = (...a) => console.log('[probe7]', ...a);
const wait = (p, ms) => p.waitForTimeout(ms);

async function heal(page) {
  for (let i = 0; i < 3; i++) {
    const healed = await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null;
      const dlgs = [...document.querySelectorAll('div')].filter(vis);
      const d = dlgs.find(x => (x.innerText || '').trim().startsWith('重新登录') && (x.innerText || '').length < 200);
      if (!d) return false;
      const inp = d.querySelector('input');
      if (inp) { inp.value = 'Ysh790921'; inp.dispatchEvent(new Event('input', { bubbles: true })); inp.dispatchEvent(new Event('change', { bubbles: true })); }
      const btn = [...d.querySelectorAll('button,a,span,div')].find(b => (b.innerText || '').trim() === '确定');
      if (btn) btn.click();
      return true;
    }).catch(() => false);
    if (!healed) return false;
    log('healed relogin #' + (i + 1));
    await wait(page, 4000);
  }
  return true;
}

/** 读页面里全部 dijit Select/ComboBox 的 options（多路兜底） */
const READ_WIDGETS = () => {
  const out = [];
  let reg = null;
  try { reg = window.dijit && window.dijit.registry } catch (e) { }
  if (!reg) { try { reg = window.require('dijit/registry'); } catch (e) { } }
  if (reg) {
    try {
      reg.forEach((id, w) => {
        if (!w) return;
        const cls = w.declaredClass || '';
        if (!/Select|ComboBox|FilteringSelect|DropDown/.test(cls)) return;
        let opts = null;
        try { if (w.options && w.options.length) opts = w.options.map(o => ({ v: o.value, l: o.label || o.name || '' })); } catch (e) { }
        let domText = '';
        try { domText = ((w.domNode && w.domNode.innerText) || '').replace(/\s+/g, ' ').trim(); } catch (e) { }
        out.push({ id: String(id), cls, opts, domText });
      });
    } catch (e) { }
  }
  return out;
};

(async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' });
  const page = await ctx.newPage();
  page.on('console', m => { if (m.type() === 'error') log('console.error:', m.text().slice(0, 120)); });

  const r = await loginQl361(page, ctx, log);
  log('LOGIN', JSON.stringify(r));
  if (!r.ok) { await page.screenshot({ path: path.join(SHOT, 'v7-login-fail.png') }).catch(() => { }); await browser.close(); return; }

  await wait(page, 9000);
  await page.keyboard.press('Escape').catch(() => { });
  await page.evaluate(() => document.querySelectorAll('.lk-dialog-close,[aria-label=Close],[title=关闭],.dojoxDialogCloseIcon').forEach(b => b.click())).catch(() => { });
  await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    [...document.querySelectorAll('.dijitTabCloseButton, .dijitTabCloseButtonSingleton')].filter(vis).forEach(b => b.click());
  }).catch(() => { });
  await wait(page, 1500);
  await heal(page);

  // 资料 → 商品条码
  await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true }).catch(() => { });
  await wait(page, 3000);
  await page.evaluate(() => {
    const items = [...document.querySelectorAll('.popupmenu__nav-text')];
    const el = items.find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '商品条码');
    if (el) el.click();
  }).catch(() => { });
  await wait(page, 15000);
  await heal(page);
  await wait(page, 3000);
  await page.screenshot({ path: path.join(SHOT, 'v7-01-list.png') }).catch(() => { });

  // ── 1) 下拉选项（registry / DOM 双路） ──
  const widgets = await page.evaluate(READ_WIDGETS).catch(() => []);
  fs.writeFileSync(path.join(OUT, 'ziliao__barcode__widgets7.json'), JSON.stringify(widgets, null, 2), 'utf8');
  log('WIDGETS:', JSON.stringify(widgets.map(w => ({ id: w.id, cls: w.cls.split(' ').pop(), opts: (w.opts || []).map(o => o.l).slice(0, 20), dom: w.domText.slice(0, 30) }))));

  const doms = await page.evaluate(() => {
    const vis = el => { if (!el) return false; const r = el.getBoundingClientRect(); return r.width > 0 && r.height > 0 && el.offsetParent !== null; };
    return [...document.querySelectorAll('.dijitSelect')].filter(vis).map(d => ({ id: d.id, text: (d.innerText || '').replace(/\s+/g, ' ').trim() }));
  }).catch(() => []);
  fs.writeFileSync(path.join(OUT, 'ziliao__barcode__doms7.json'), JSON.stringify(doms, null, 2), 'utf8');
  log('DOM SELECTS:', JSON.stringify(doms));

  // 逐一点开 DOM 下拉，读弹层（registry 取不到 options 时的兜底）
  const ddResult = {};
  for (const d of doms.slice(0, 6)) {
    if (!d.id) continue;
    await page.locator('#' + d.id + ' .dijitArrowButton, #' + d.id).first().click({ force: true }).catch(() => { });
    await wait(page, 1500);
    const menu = await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null;
      const pops = [...document.querySelectorAll('.dijitPopup, .dijitMenuPopup, .dijitComboBoxMenuPopup, .dijitSelectMenu')].filter(vis);
      const out = [];
      pops.forEach(p => {
        [...p.querySelectorAll('.dijitMenuItem, .dijitMenuItemLabel, td, table tr')].forEach(t => {
          const s = (t.innerText || '').trim().replace(/\s+/g, ' ');
          if (s && s.length <= 24 && !out.includes(s)) out.push(s);
        });
      });
      return out;
    }).catch(() => []);
    ddResult[d.text] = menu;
    log('DD', JSON.stringify(d.text), '=>', JSON.stringify(menu));
    if (menu.length) await page.screenshot({ path: path.join(SHOT, 'v7-dd-' + d.id.replace(/[^\w]/g, '_') + '.png') }).catch(() => { });
    await page.keyboard.press('Escape').catch(() => { });
    await wait(page, 800);
  }
  fs.writeFileSync(path.join(OUT, 'ziliao__barcode__dd7.json'), JSON.stringify(ddResult, null, 2), 'utf8');

  // ── 2) 行级「修改」弹窗 ──
  const clicked = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const panes = [...document.querySelectorAll('.dijitTabPane')].filter(vis);
    const panel = panes.find(p => (p.innerText || '').includes('上架状态')) || panes[panes.length - 1] || document.body;
    const a = [...panel.querySelectorAll('a,button,span,div')].filter(vis)
      .find(x => (x.innerText || '').trim() === '修改' && x.children.length === 0);
    if (!a) return 'not-found';
    a.scrollIntoView({ block: 'center' });
    a.click();
    return 'clicked';
  }).catch(e => 'err:' + e.message);
  log('MOD CLICK:', clicked);
  await wait(page, 8000);
  await heal(page);
  await page.screenshot({ path: path.join(SHOT, 'v7-02-rowedit.png') }).catch(() => { });

  const edit = await page.evaluate(() => {
    const vis = el => { if (!el) return false; const r = el.getBoundingClientRect(); return r.width > 0 && r.height > 0 && el.offsetParent !== null; };
    const dlgRoots = [...document.querySelectorAll('.dijitDialog, [role=dialog], .dijitDialogPaneContent, .lk-dialog')].filter(vis);
    const cands = dlgRoots
      .map(d => ({ cls: String(d.className).slice(0, 70), text: (d.innerText || '').slice(0, 2000), title: (d.querySelector('.dijitDialogTitle, .dijitDialogTitleBar') || {}).innerText || '' }))
      .filter(d => d.text.trim().length > 15 && !d.text.includes('列名') && !d.text.includes('重新登录'));
    cands.sort((a, b) => b.text.length - a.text.length);
    const scope = dlgRoots.find(d => !(d.innerText || '').includes('列名')) || document;
    const fields = [...scope.querySelectorAll('input, select, textarea')].filter(vis).map(i => ({
      tag: i.tagName, id: i.id || '', name: i.name || '', type: i.type || '',
      ph: i.placeholder || '', val: (i.value || '').slice(0, 60),
      label: (() => { let p = i.closest('label') || i.parentElement; for (let k = 0; k < 4 && p; k++) { const t = (p.innerText || '').replace(/\s+/g, ' ').trim(); if (t && t.length < 40) return t; p = p.parentElement } return '' })(),
    }));
    // 所有标签文本（dojo 表单常用 label 独立节点）
    const labels = [...scope.querySelectorAll('label, .dijitLabel, td, .field-label')].filter(vis)
      .map(l => (l.innerText || '').replace(/\s+/g, ' ').trim()).filter(t => t && t.length < 30);
    return { top: cands[0] || null, allDialogs: cands.slice(0, 5), fields, labels: [...new Set(labels)].slice(0, 120) };
  }).catch(e => ({ err: String(e) }));
  fs.writeFileSync(path.join(OUT, 'ziliao__barcode__rowedit7.json'), JSON.stringify(edit, null, 2), 'utf8');
  log('ROWEDIT top:', (edit.top && edit.top.text || 'none').replace(/\n+/g, ' | ').slice(0, 700));
  log('ROWEDIT fields:', JSON.stringify(edit.fields));
  log('ROWEDIT labels:', JSON.stringify(edit.labels));

  await browser.close();
  log('DONE');
})().catch(e => { console.error('FATAL', e); process.exit(1); });
