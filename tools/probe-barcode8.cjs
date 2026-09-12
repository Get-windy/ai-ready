// ql361 商品条码 v8：补抓 ① 分类树节点（含根节点「全部商品」）②「条码打印」弹窗 ③「打印(F8)」弹窗
const { chromium } = require('playwright');
const { loginQl361 } = require('./ql361-lib.cjs');
const fs = require('fs');
const path = require('path');

const OUT = path.join(__dirname, '..', 'tool-results', 'ql361', 'pages');
const SHOT = path.join(__dirname, '..', 'tool-results', 'ql361', 'barcode-shots');
fs.mkdirSync(SHOT, { recursive: true });
const log = (...a) => console.log('[probe8]', ...a);
const wait = (p, ms) => p.waitForTimeout(ms);

async function heal(page) {
  for (let i = 0; i < 3; i++) {
    const healed = await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null;
      const d = [...document.querySelectorAll('div')].filter(vis)
        .find(x => (x.innerText || '').trim().startsWith('重新登录') && (x.innerText || '').length < 200);
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

/** dump 当前所有可见弹层/对话框 */
const DUMP_DIALOG = () => {
  const vis = el => { if (!el) return false; const r = el.getBoundingClientRect(); return r.width > 40 && r.height > 30 && el.offsetParent !== null; };
  const roots = [...document.querySelectorAll('.dijitDialog, .lk-dialog, [role=dialog], .dojoxDialog')].filter(vis);
  const out = roots.map(d => ({
    cls: String(d.className).slice(0, 70),
    w: d.offsetWidth, h: d.offsetHeight,
    title: ((d.querySelector('.dijitDialogTitle, .dijitDialogTitleBar, .lk-dialog-title') || {}).innerText || '').trim().slice(0, 40),
    text: (d.innerText || '').replace(/\n{2,}/g, '\n').slice(0, 1200),
  })).filter(d => d.text.trim().length > 10);
  out.sort((a, b) => b.text.length - a.text.length);
  const btns = [...document.querySelectorAll('.dijitDialog button, .dijitDialog .dijitButtonNode, .dijitDialog input[type=button]')].filter(vis)
    .map(b => (b.innerText || b.value || '').trim()).filter(Boolean);
  const fields = [...document.querySelectorAll('.dijitDialog input, .dijitDialog select, .dijitDialog textarea')].filter(vis)
    .map(i => ({ tag: i.tagName, type: i.type || '', val: (i.value || '').slice(0, 30), ph: i.placeholder || '' }));
  return { top: out[0] || null, all: out.slice(0, 4), btns: [...new Set(btns)].slice(0, 20), fields: fields.slice(0, 25) };
};

(async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' });
  const page = await ctx.newPage();
  page.on('console', m => { if (m.type() === 'error') log('console.error:', m.text().slice(0, 110)); });

  const r = await loginQl361(page, ctx, log);
  log('LOGIN', JSON.stringify(r));
  if (!r.ok) { await page.screenshot({ path: path.join(SHOT, 'v8-login-fail.png') }).catch(() => { }); await browser.close(); return; }

  await wait(page, 9000);
  await page.keyboard.press('Escape').catch(() => { });
  await page.evaluate(() => document.querySelectorAll('.lk-dialog-close,[aria-label=Close],[title=关闭],.dojoxDialogCloseIcon').forEach(b => b.click())).catch(() => { });
  await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    [...document.querySelectorAll('.dijitTabCloseButton, .dijitTabCloseButtonSingleton')].filter(vis).forEach(b => b.click());
  }).catch(() => { });
  await wait(page, 1500);
  await heal(page);

  await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true }).catch(() => { });
  await wait(page, 3000);
  await page.evaluate(() => {
    const el = [...document.querySelectorAll('.popupmenu__nav-text')].find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '商品条码');
    if (el) el.click();
  }).catch(() => { });
  await wait(page, 15000);
  await heal(page);
  await wait(page, 3000);
  await page.screenshot({ path: path.join(SHOT, 'v8-01-list.png') }).catch(() => { });

  // ── ① 分类树节点 ──
  const tree = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const panes = [...document.querySelectorAll('.dijitTabPane')].filter(vis);
    const panel = panes.find(p => (p.innerText || '').includes('上架状态')) || panes[panes.length - 1] || document.body;
    // 分类树容器：含「全部商品」文本的最上层可滚动块
    const cands = [...panel.querySelectorAll('div,ul')].filter(d => vis(d) && (d.innerText || '').includes('全部商品'));
    cands.sort((a, b) => (a.innerText || '').length - (b.innerText || '').length);
    const box = cands[0];
    const items = box ? [...box.querySelectorAll('div,li,span')].filter(vis)
      .map(e => (e.innerText || '').trim().replace(/\s+/g, ''))
      .filter(t => t && t.length <= 12 && !/^\d+$/.test(t)) : [];
    return {
      found: !!box,
      raw: box ? (box.innerText || '').split('\n').map(s => s.trim()).filter(Boolean).slice(0, 40) : [],
      dedup: [...new Set(items)].slice(0, 40),
      panelText: (panel.innerText || '').split('\n').map(s => s.trim()).filter(Boolean).slice(0, 30),
    };
  }).catch(e => ({ err: String(e) }));
  fs.writeFileSync(path.join(OUT, 'ziliao__barcode__tree8.json'), JSON.stringify(tree, null, 2), 'utf8');
  log('TREE raw:', JSON.stringify(tree.raw));
  log('TREE dedup:', JSON.stringify(tree.dedup));

  // ── ② 条码打印：先勾选第一行再点按钮 ──
  const checked = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const panes = [...document.querySelectorAll('.dijitTabPane')].filter(vis);
    const panel = panes.find(p => (p.innerText || '').includes('上架状态')) || panes[panes.length - 1] || document.body;
    const boxes = [...panel.querySelectorAll('input[type=checkbox]')].filter(vis);
    if (!boxes.length) return 'no-checkbox:' + panel.querySelectorAll('input').length;
    boxes[0].click();
    return 'checked';
  }).catch(e => 'err:' + e.message);
  log('CHECK:', checked);
  await wait(page, 1200);

  const printBtn = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const a = [...document.querySelectorAll('a,button,span')].filter(vis)
      .find(x => (x.innerText || '').trim().replace(/\s+/g, '') === '条码打印');
    if (!a) return false;
    a.click(); return true;
  }).catch(e => 'err:' + e.message);
  log('条码打印 click:', printBtn);
  await wait(page, 6000);
  await heal(page);
  await page.screenshot({ path: path.join(SHOT, 'v8-02-barcode-print.png') }).catch(() => { });
  const bcPrint = await page.evaluate(DUMP_DIALOG).catch(e => ({ err: String(e) }));
  fs.writeFileSync(path.join(OUT, 'ziliao__barcode__barcodeprint8.json'), JSON.stringify(bcPrint, null, 2), 'utf8');
  log('条码打印 top:', (bcPrint.top && bcPrint.top.text || 'none').replace(/\n+/g, ' | ').slice(0, 600));
  log('条码打印 btns:', JSON.stringify(bcPrint.btns));

  // 关闭弹窗
  await page.keyboard.press('Escape').catch(() => { });
  await wait(page, 1500);

  // ── ③ 打印(F8) ──
  const f8Btn = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const a = [...document.querySelectorAll('a,button,span')].filter(vis)
      .find(x => (x.innerText || '').trim().replace(/\s+/g, '') === '打印(F8)' || (x.innerText || '').trim().replace(/\s+/g, '') === '打印');
    if (!a) return false;
    a.click(); return true;
  }).catch(e => 'err:' + e.message);
  log('打印(F8) click:', f8Btn);
  await wait(page, 6000);
  await heal(page);
  await page.screenshot({ path: path.join(SHOT, 'v8-03-print.png') }).catch(() => { });
  const printDlg = await page.evaluate(DUMP_DIALOG).catch(e => ({ err: String(e) }));
  fs.writeFileSync(path.join(OUT, 'ziliao__barcode__print8.json'), JSON.stringify(printDlg, null, 2), 'utf8');
  log('打印 top:', (printDlg.top && printDlg.top.text || 'none').replace(/\n+/g, ' | ').slice(0, 600));
  log('打印 btns:', JSON.stringify(printDlg.btns));

  await browser.close();
  log('DONE');
})().catch(e => { console.error('FATAL', e); process.exit(1); });
