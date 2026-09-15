// 最终校验：把每个下拉与它的 label 精确绑定（读最近的 filter-item 容器）
// 目标页: 单位显示 / 商品上架 / 店铺设置(注册设置)
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const OUT = process.argv[2] || '../tool-results/ql361/pages';
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

const BIND = () => {
  const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
  const norm = s => (s || '').replace(/\s+/g, ' ').trim();
  function extractOpts(w) {
    let o = null; try { o = (w.get && w.get('options')) || w.options; } catch (e) {}
    if (!o) return null;
    let arr = Array.isArray(o) ? [...o] : Object.keys(o).map(k => ({ label: k, value: o[k] }));
    const res = arr.filter(x => x && typeof x === 'object').map(x => {
      if (x.value && typeof x.value === 'object' && 'label' in x.value) return { value: x.value.value, label: x.value.label, selected: !!x.value.selected };
      return { value: x.value !== undefined ? x.value : '', label: x.label !== undefined ? x.label : '', selected: !!x.selected };
    }).filter(x => x.label !== '' && !/^(length|_parent)$/.test(x.label) && !/^_/.test(x.label));
    return res.length ? res : null;
  }
  const out = [];
  for (const el of document.querySelectorAll('[role=listbox],[class*=dijitSelect]')) {
    if (!vis(el)) continue;
    if (el.classList.contains('dijitSelectValue') || el.classList.contains('dijitSelectLabel')) continue;
    if (!el.id || !window.dijit || !dijit.byId(el.id)) continue;
    const w = dijit.byId(el.id);
    const opts = extractOpts(w);
    // 精确 label：向上找第一个含 label/title/filter-item 的容器，取其自身直接文本
    let label = '';
    let n = el;
    for (let i = 0; i < 6 && n; i++) {
      const par = n.parentElement;
      if (!par) break;
      const sibs = [...par.children].filter(c => c !== n && !c.contains(el));
      for (const s of sibs) {
        const t = norm(s.innerText);
        if (t && t.length <= 24 && !s.querySelector('input,select,table')) { label = t; break; }
      }
      if (label) break;
      n = par;
    }
    const disp = el.querySelector('.dijitSelectLabel');
    out.push({
      id: el.id, declaredClass: w.declaredClass || '',
      label, displayedValue: disp ? norm(disp.innerText) : '',
      options: opts
    });
  }
  // 页面筛选区整段文本（按 filter 容器切）
  const filterTexts = [...document.querySelectorAll('[class*=filter],[class*=Filter],[class*=condition],[class*=query]')].filter(vis)
    .map(e => norm(e.innerText)).filter(t => t && t.length < 600);
  return { dropdowns: out, filterTexts: [...new Set(filterTexts)].slice(0, 8), bodyStart: (document.body.innerText || '').slice(0, 2500) };
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
  async function dismiss() { for (const fr of page.frames()) { try { await fr.evaluate(() => { const v = e => e && e.offsetParent !== null; const b = [...document.querySelectorAll('button,a,div,span,input[type=button]')].filter(v).find(x => /^(我知道了)$/.test((x.innerText || x.value || '').trim())); if (b) b.click(); }); } catch (e) {} } await sleep(600); }
  async function openItem(item) {
    await page.locator('a.menu-0-item', { hasText: '销售' }).first().click({ force: true }).catch(() => {}); await sleep(800);
    await page.locator('a.menu-0-item', { hasText: '商城' }).first().click({ force: true }).catch(() => {}); await sleep(1700);
    await page.evaluate((item) => { const v = e => e && e.offsetParent !== null; const el = [...document.querySelectorAll('.popupmenu__nav-text')].filter(v).find(e => (e.innerText || '').replace(/\s+/g, '').trim() === item); if (el) el.click(); }, item).catch(() => {});
    await sleep(10000); await dismiss(); await sleep(1500);
  }
  async function clickSub(t) { await page.evaluate((t) => { const v = e => e && e.offsetParent !== null; const el = [...document.querySelectorAll('.tab-text')].filter(v).find(e => (e.innerText || '').trim() === t); if (el) el.click(); }, t).catch(() => {}); await sleep(7000); await dismiss(); await sleep(1500); }

  for (const [key, item, sub] of [['unit', '单位显示', null], ['onShelf', '商品上架', null], ['reg', '店铺设置', '注册设置']]) {
    console.log(`\n=== ${key} (${item}${sub ? ' > ' + sub : ''}) ===`);
    await openItem(item);
    if (sub) { await clickSub('店铺参数'); await clickSub(sub); }
    else { await sleep(4000); await dismiss(); await sleep(2000); }
    R[key] = await page.evaluate(BIND).catch(e => ({ err: e.message }));
    for (const d of (R[key].dropdowns || [])) {
      console.log(`  [${d.label}] disp=${d.displayedValue} ${d.declaredClass}`);
      console.log(`      opts: ${JSON.stringify((d.options || []).map(o => o.label + '(' + o.value + ')' + (o.selected ? '*' : '')))}`);
    }
    console.log('  bodyStart:', (R[key].bodyStart || '').replace(/\n/g, ' | ').slice(300, 1400));
    await page.screenshot({ path: path.join(OUT, `_fix6_${key}.png`), fullPage: true }).catch(() => {});
  }
  fs.writeFileSync(path.join(OUT, '_probe_trade_fix6_raw.json'), JSON.stringify(R, null, 1), 'utf-8');
  console.log('\nWROTE _probe_trade_fix6_raw.json');
  await browser.close();
  console.log('DONE');
})().catch(e => { console.error('FATAL', e.stack || e.message); process.exit(1); });
