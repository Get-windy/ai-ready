// 单点补抓：运费设置 → 物流 → 只开「自有配送」，看是否出现配送范围/运费配置
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const OUT = process.argv[2] || '../tool-results/ql361/pages';
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

const SNAP = () => {
  const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
  const norm = s => (s || '').replace(/\s+/g, ' ').trim();
  function opts(w) {
    let o = null; try { o = (w.get && w.get('options')) || w.options; } catch (e) {}
    if (!o) return null;
    const arr = Array.isArray(o) ? [...o] : Object.keys(o).map(k => ({ label: k, value: o[k] }));
    const r = arr.filter(x => x && typeof x === 'object').map(x => {
      if (x.value && typeof x.value === 'object' && 'label' in x.value) return { value: x.value.value, label: x.value.label, selected: !!x.value.selected };
      return { value: x.value !== undefined ? x.value : '', label: x.label !== undefined ? x.label : '', selected: !!x.selected };
    }).filter(x => x.label !== '' && !/^(length|_parent)$/.test(x.label) && !/^_/.test(x.label));
    return r.length ? r : null;
  }
  const dds = [];
  for (const el of document.querySelectorAll('[role=listbox],[class*=dijitSelect],input.dijitInputInner')) {
    if (!vis(el)) continue;
    if (el.classList.contains('dijitSelectValue') || el.classList.contains('dijitSelectLabel')) continue;
    const w = (el.id && window.dijit && dijit.byId(el.id)) ? dijit.byId(el.id) : null;
    const disp = el.querySelector && el.querySelector('.dijitSelectLabel');
    const o = w ? opts(w) : null;
    if (o || (el.className || '').includes('InputInner')) dds.push({
      cls: String(el.className).slice(0, 60), id: el.id, declaredClass: w ? w.declaredClass : '',
      disp: disp ? norm(disp.innerText) : '', val: el.value || '', options: o
    });
  }
  const host = [...document.querySelectorAll('.tab-list-main,[class*=dockbody]')].filter(vis).sort((a, b) => (b.innerText || '').length - (a.innerText || '').length)[0];
  const toggles = [...document.querySelectorAll('input.dijitCheckBoxInput')].filter(vis).map(c => ({
    id: c.id, name: c.name, checked: c.checked,
    text: (c.closest('label') || c.parentElement).innerText ? norm((c.closest('label') || c.parentElement).innerText) : ''
  }));
  const tables = [...document.querySelectorAll('table.dgrid-row-table')].map(t => ({
    head: [...t.querySelectorAll('th')].map(th => norm(th.innerText)).filter(Boolean),
    rows: [...t.querySelectorAll('tbody tr')].slice(0, 4).map(tr => [...tr.querySelectorAll('td')].map(td => norm(td.innerText).slice(0, 40)))
  })).filter(t => t.head.length || t.rows.length);
  const tips = [...document.querySelectorAll('[data-dojo-props*=toolTipContent]')].map(e => {
    const m = (e.getAttribute('data-dojo-props') || '').match(/toolTipContent:'([^']+)'/);
    return m ? { label: norm(e.innerText), tip: m[1] } : null;
  }).filter(Boolean);
  return { panelText: host ? host.innerText : '', toggles, dropdowns: dds, tables, tips };
};

(async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 }, locale: 'zh-CN' });
  const page = await ctx.newPage();
  const R = {};
  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'networkidle', timeout: 45000 }).catch(() => {});
  await page.fill('input[placeholder="请输入手机号/用户名"]', '15095664266').catch(() => {});
  await page.fill('#pwd', 'Ysh790921').catch(() => {});
  await page.click('button.logon_button').catch(() => {});
  for (let i = 0; i < 30; i++) { await sleep(1000); if (!page.url().includes('/Account/Logon')) break; }
  await sleep(9000); await page.keyboard.press('Escape').catch(() => {});
  console.log('LOGIN OK');
  await page.locator('a.menu-0-item', { hasText: '商城' }).first().click({ force: true }).catch(() => {});
  await sleep(1700);
  await page.evaluate(() => { const v = e => e && e.offsetParent !== null; const el = [...document.querySelectorAll('.popupmenu__nav-text')].filter(v).find(e => (e.innerText || '').replace(/\s+/g, '').trim() === '运费设置'); if (el) el.click(); }).catch(() => {});
  await sleep(10000);
  for (const fr of page.frames()) { try { await fr.evaluate(() => { const v = e => e && e.offsetParent !== null; const b = [...document.querySelectorAll('button,a,div,span,input[type=button]')].filter(v).find(x => /^(我知道了)$/.test((x.innerText || x.value || '').trim())); if (b) b.click(); }); } catch (e) {} }
  await sleep(1500);
  // 只开「自有配送」
  R.on = await page.evaluate(() => {
    const cb = [...document.querySelectorAll('input.dijitCheckBoxInput')].find(c => c.name === 'selfmention');
    if (!cb) return { found: false };
    if (!cb.checked) cb.click();
    return { found: true, checked: cb.checked, id: cb.id };
  });
  await sleep(7000);
  R.snapSelfDeliveryOnly = await page.evaluate(SNAP);
  await page.screenshot({ path: path.join(OUT, '_fix8_运费设置_仅自有配送开.png'), fullPage: true }).catch(() => {});
  console.log('=== 仅「自有配送」开 ===');
  console.log('panelText:', (R.snapSelfDeliveryOnly.panelText || '').replace(/\n/g, ' | '));
  console.log('toggles:', JSON.stringify(R.snapSelfDeliveryOnly.toggles));
  console.log('dropdowns:', JSON.stringify(R.snapSelfDeliveryOnly.dropdowns));
  console.log('tables:', JSON.stringify(R.snapSelfDeliveryOnly.tables).slice(0, 800));
  console.log('tips:', JSON.stringify(R.snapSelfDeliveryOnly.tips));
  // 再开「启用物流」（两个都开）
  await page.evaluate(() => { const cb = [...document.querySelectorAll('input.dijitCheckBoxInput')].find(c => c.name === 'isexpress'); if (cb && !cb.checked) cb.click(); });
  await sleep(7000);
  R.snapBoth = await page.evaluate(SNAP);
  R.snapBoth.tips = R.snapBoth.tips;
  await page.screenshot({ path: path.join(OUT, '_fix8_运费设置_两个开关都开.png'), fullPage: true }).catch(() => {});
  console.log('\n=== 两个开关都开 ===');
  console.log('panelText:', (R.snapBoth.panelText || '').replace(/\n/g, ' | ').slice(0, 900));
  console.log('tips:', JSON.stringify(R.snapBoth.tips));
  console.log('dropdowns:', JSON.stringify(R.snapBoth.dropdowns.map(d => ({ disp: d.disp, val: d.val, options: d.options ? d.options.map(o => o.label) : null }))));
  fs.writeFileSync(path.join(OUT, '_probe_trade_fix8_raw.json'), JSON.stringify(R, null, 1), 'utf-8');
  console.log('\nWROTE _probe_trade_fix8_raw.json');
  await browser.close(); console.log('DONE');
})().catch(e => { console.error('FATAL', e.stack || e.message); process.exit(1); });
