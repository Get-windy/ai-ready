// 收尾：注册设置「买家注册默认分类」控件结构 + 店铺参数全部下拉选项 + 商品上架筛选标签清单
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const OUT = process.argv[2] || '../tool-results/ql361/pages';
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

const ALL = () => {
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
  const dds = [], others = [];
  for (const el of document.querySelectorAll('[role=listbox],[role=combobox],[class*=dijitSelect],[class*=ItemField],input[type=text][class*=InputInner]')) {
    if (!vis(el)) continue;
    if (el.classList.contains('dijitSelectValue') || el.classList.contains('dijitSelectLabel')) continue;
    const w = (el.id && window.dijit && dijit.byId(el.id)) ? dijit.byId(el.id) : null;
    const rec = { id: el.id || '', cls: String(el.className).slice(0, 80), declaredClass: w ? w.declaredClass : '', role: el.getAttribute('role') || '' };
    const disp = el.querySelector && el.querySelector('.dijitSelectLabel');
    rec.disp = disp ? norm(disp.innerText) : '';
    const o = w ? opts(w) : null;
    if (o) { rec.options = o; dds.push(rec); }
    else if (el.tagName === 'INPUT') {
      const c = el.closest('[class*=ItemField],[class*=form-group],td,.dijitInline');
      const cl = c ? c.cloneNode(true) : null; if (cl) cl.querySelectorAll('input,select,button').forEach(n => n.remove());
      others.push({ ...rec, val: el.value, containerText: cl ? norm(cl.innerText).slice(0, 60) : '' });
    }
  }
  return { dropdowns: dds, inputs: others.slice(0, 40), body: (document.body.innerText || '').slice(0, 3000) };
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
  async function dismiss() { for (const fr of page.frames()) { try { await fr.evaluate(() => { const v = e => e && e.offsetParent !== null; const b = [...document.querySelectorAll('button,a,div,span,input[type=button]')].filter(v).find(x => /^(我知道了)$/.test((x.innerText || x.value || '').trim())); if (b) b.click(); }); } catch (e) {} } await sleep(600); }
  async function openItem(item) {
    await page.locator('a.menu-0-item', { hasText: '销售' }).first().click({ force: true }).catch(() => {}); await sleep(800);
    await page.locator('a.menu-0-item', { hasText: '商城' }).first().click({ force: true }).catch(() => {}); await sleep(1700);
    await page.evaluate((item) => { const v = e => e && e.offsetParent !== null; const el = [...document.querySelectorAll('.popupmenu__nav-text')].filter(v).find(e => (e.innerText || '').replace(/\s+/g, '').trim() === item); if (el) el.click(); }, item).catch(() => {});
    await sleep(10000); await dismiss(); await sleep(1500);
  }
  async function clickSub(t) { await page.evaluate((t) => { const v = e => e && e.offsetParent !== null; const el = [...document.querySelectorAll('.tab-text')].filter(v).find(e => (e.innerText || '').trim() === t); if (el) el.click(); }, t).catch(() => {}); await sleep(7000); await dismiss(); await sleep(1500); }
  const show = async (key, tag) => {
    R[key] = await page.evaluate(ALL).catch(e => ({ err: e.message }));
    console.log(`\n--- ${tag} --- dropdowns=${(R[key].dropdowns || []).length}`);
    for (const d of (R[key].dropdowns || [])) console.log(`  disp=${d.disp} [${d.declaredClass}] ${JSON.stringify((d.options || []).map(o => o.label + '(' + o.value + ')' + (o.selected ? '*' : ''))).slice(0, 420)}`);
    console.log('  inputs:', JSON.stringify((R[key].inputs || []).map(i => i.val + ' @' + i.containerText)).slice(0, 700));
  };

  console.log('LOGIN OK');
  // 店铺设置: 店铺参数 + 注册设置
  await openItem('店铺设置');
  await clickSub('店铺参数');
  await show('shopParams', '店铺设置 > 店铺参数');
  await page.screenshot({ path: path.join(OUT, '_fix7_店铺参数.png'), fullPage: true }).catch(() => {});
  await clickSub('注册设置');
  await show('register', '店铺设置 > 注册设置');
  R.regHtml = await page.evaluate(() => {
    const v = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const rows = [...document.querySelectorAll('[class*=form-group],[class*=FormField]')].filter(v);
    const host = rows.length ? rows[0].closest('form,[class*=dockbody],[class*=inner-content]') : null;
    return host ? host.outerHTML.slice(0, 60000) : '';
  }).catch(() => '');
  fs.writeFileSync(path.join(OUT, '_fix7_注册设置_panel.html'), R.regHtml, 'utf-8');
  await page.screenshot({ path: path.join(OUT, '_fix7_注册设置.png'), fullPage: true }).catch(() => {});
  // 商品上架
  await openItem('商品上架');
  await sleep(4000); await dismiss(); await sleep(2000);
  await show('onShelf', '商品上架');
  R.onShelfFilterLabels = await page.evaluate(() => {
    const v = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const els = [...document.querySelectorAll('label,[class*=label],[class*=Label],[class*=filter] span,[class*=filter] div')].filter(v);
    const t = els.map(e => (e.innerText || '').replace(/\s+/g, ' ').trim()).filter(x => x && x.length < 20);
    return [...new Set(t)].slice(0, 60);
  }).catch(() => []);
  console.log('  商品上架 候选label:', JSON.stringify(R.onShelfFilterLabels));
  await page.screenshot({ path: path.join(OUT, '_fix7_商品上架.png'), fullPage: true }).catch(() => {});
  fs.writeFileSync(path.join(OUT, '_probe_trade_fix7_raw.json'), JSON.stringify(R, null, 1), 'utf-8');
  console.log('\nWROTE _probe_trade_fix7_raw.json');
  await browser.close(); console.log('DONE');
})().catch(e => { console.error('FATAL', e.stack || e.message); process.exit(1); });
