// 补齐探针：注册设置三个下拉的选项全集 + 单位显示查询区下拉 + 商品上架「上架状态」 + 库存说明弹层
// 用法: node _probe_trade_fix5.js [outDir]
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const OUT = process.argv[2] || '../tool-results/ql361/pages';
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

// 读取本页所有可见下拉的选项全集 + 文档序前序文本作为 label
const READ = () => {
  const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
  const norm = s => (s || '').replace(/\s+/g, ' ').trim();

  function extractOpts(w) {
    let o = null;
    try { o = (w.get && w.get('options')) || w.options; } catch (e) {}
    if (!o) return null;
    let arr = Array.isArray(o) ? [...o] : Object.keys(o).map(k => ({ label: k, value: o[k] }));
    arr = arr.filter(x => x && typeof x === 'object');
    const res = arr.map(x => {
      if (x.value && typeof x.value === 'object' && 'label' in x.value) return { value: x.value.value, label: x.value.label, selected: !!x.value.selected };
      return { value: x.value !== undefined ? x.value : '', label: x.label !== undefined ? x.label : '', selected: !!x.selected };
    }).filter(x => x.label !== '' && x.label !== 'length' && x.label !== '_parent' && !/^_/.test(x.label));
    return res.length ? res : null;
  }

  const all = [...document.querySelectorAll('*')].filter(vis);
  const idxOf = new Map(); all.forEach((e, i) => idxOf.set(e, i));
  function prevTexts(el, n) {
    const i = idxOf.get(el); if (i === undefined) return [];
    const out = [];
    for (let j = i - 1; j >= 0 && out.length < n; j--) {
      const e = all[j];
      if (e.children.length === 0) { const t = norm(e.innerText); if (t && t.length <= 30) out.push(t); }
    }
    return out;
  }

  const out = [];
  const seen = new Set();
  for (const el of document.querySelectorAll('[role=listbox],[class*=dijitSelect],input[type=checkbox],input.dijitCheckBoxInput,select')) {
    if (!vis(el)) continue;
    if (el.classList.contains('dijitSelectValue') || el.classList.contains('dijitSelectLabel')) continue;
    const k = (el.id || '') + '#' + out.length;
    if (seen.has(k)) continue; seen.add(k);
    const rec = {
      tag: el.tagName.toLowerCase(), type: el.type || '', id: el.id || '',
      cls: String(el.className).slice(0, 90),
      prevTexts: prevTexts(el, 3).reverse(),
      declaredClass: '', options: null, checked: typeof el.checked === 'boolean' ? el.checked : undefined, inputValue: ''
    };
    const inn = el.querySelector ? el.querySelector('input.dijitInputInner, input[type=hidden][data-dojo-attach-point=valueNode]') : null;
    if (inn) rec.inputValue = inn.value;
    if (el.id && window.dijit && dijit.byId && dijit.byId(el.id)) {
      try {
        const w = dijit.byId(el.id);
        rec.declaredClass = w.declaredClass || '';
        rec.options = extractOpts(w);
        const dp = el.getAttribute('data-dojo-props') || '';
        const m = dp.match(/toolTipContent:'([^']{3,300})'/);
        if (m) rec.toolTipContent = m[1];
      } catch (e) { rec.dojoErr = String(e.message).slice(0, 60); }
    }
    out.push(rec);
  }
  const notes = [...document.querySelectorAll('.lk-form-tooltip-help,[class*=ToolTipContent],[class*=tooltip-content]')].filter(vis).map(e => norm(e.innerText)).filter(Boolean);
  return { widgets: out, notes: [...new Set(notes)] };
};

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 }, locale: 'zh-CN' });
  const page = await ctx.newPage();
  const R = { ts: new Date().toISOString(), pages: {} };

  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'networkidle', timeout: 45000 }).catch(() => {});
  await page.fill('input[placeholder="请输入手机号/用户名"]', '15095664266').catch(() => {});
  await page.fill('#pwd', 'Ysh790921').catch(() => {});
  await page.click('button.logon_button').catch(() => {});
  for (let i = 0; i < 30; i++) { await sleep(1000); if (!page.url().includes('/Account/Logon')) break; }
  await sleep(9000);
  await page.keyboard.press('Escape').catch(() => {});
  console.log('LOGIN OK');

  async function dismiss() {
    for (const fr of page.frames()) {
      try { await fr.evaluate(() => { const v = e => e && e.offsetParent !== null; const b = [...document.querySelectorAll('button,a,div,span,input[type=button]')].filter(v).find(x => /^(我知道了)$/.test((x.innerText || x.value || '').trim())); if (b) b.click(); }); } catch (e) {}
    }
    await sleep(600);
  }
  async function openItem(item) {
    await page.locator('a.menu-0-item', { hasText: '销售' }).first().click({ force: true }).catch(() => {});
    await sleep(800);
    await page.locator('a.menu-0-item', { hasText: '商城' }).first().click({ force: true }).catch(() => {});
    await sleep(1700);
    const ok = await page.evaluate((item) => {
      const v = e => e && e.offsetParent !== null;
      const el = [...document.querySelectorAll('.popupmenu__nav-text')].filter(v).find(e => (e.innerText || '').replace(/\s+/g, '').trim() === item);
      if (el) { el.click(); return true; } return false;
    }, item).catch(() => false);
    await sleep(10000); await dismiss(); await sleep(1500);
    return ok;
  }
  async function clickSub(txt) {
    const r = await page.evaluate((txt) => {
      const v = e => e && e.offsetParent !== null;
      const el = [...document.querySelectorAll('.tab-text')].filter(v).find(e => (e.innerText || '').trim() === txt);
      if (el) { el.click(); return true; } return false;
    }, txt).catch(() => false);
    await sleep(7000); await dismiss(); await sleep(1500);
    return r;
  }
  const dumpWidgets = async (tag) => {
    const r = await page.evaluate(READ).catch(e => ({ err: e.message }));
    console.log(`  [${tag}] widgets=${(r.widgets || []).length} notes=${JSON.stringify(r.notes || r.err)}`);
    for (const w of (r.widgets || [])) {
      if (w.options) console.log(`    OPT [${w.prevTexts.join(' < ')}] ${w.declaredClass} val=${w.inputValue} -> ${JSON.stringify(w.options)}`);
      else if (w.type === 'checkbox') console.log(`    CHK [${w.prevTexts.join(' < ')}] checked=${w.checked} ${w.toolTipContent ? 'TIP=' + w.toolTipContent : ''}`);
    }
    return r;
  };

  // ===== 1. 店铺设置 → 注册设置 =====
  console.log('\n=== 注册设置 ===');
  await openItem('店铺设置');
  await clickSub('店铺参数');
  await clickSub('注册设置');
  R.pages.reg = await dumpWidgets('注册设置');
  await page.screenshot({ path: path.join(OUT, '_fix5_注册设置.png'), fullPage: true }).catch(() => {});

  // ===== 2. 单位显示 =====
  console.log('\n=== 单位显示 ===');
  await openItem('单位显示');
  await sleep(4000); await dismiss(); await sleep(2000);
  R.pages.unit = await dumpWidgets('单位显示');
  await page.screenshot({ path: path.join(OUT, '_fix5_单位显示.png'), fullPage: true }).catch(() => {});

  // ===== 3. 商品上架 =====
  console.log('\n=== 商品上架 ===');
  await openItem('商品上架');
  await sleep(4000); await dismiss(); await sleep(2000);
  R.pages.onShelf = await dumpWidgets('商品上架');
  R.pages.onShelf.filterArea = await page.evaluate(() => {
    const v = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const t = [...document.querySelectorAll('[class*=filter],[class*=Filter],[class*=search],[class*=condition]')].filter(v)
      .map(e => (e.innerText || '').replace(/\s+/g, ' ').trim()).filter(x => x && x.length < 400);
    return [...new Set(t)].slice(0, 10);
  }).catch(() => []);
  console.log('  筛选区:', JSON.stringify(R.pages.onShelf.filterArea).slice(0, 600));
  await page.screenshot({ path: path.join(OUT, '_fix5_商品上架.png'), fullPage: true }).catch(() => {});

  // ===== 4. 库存显示方式「说明」弹层 =====
  console.log('\n=== 库存说明 ===');
  await openItem('店铺设置');
  await clickSub('店铺参数');
  await sleep(2000);
  const note = await page.evaluate(() => {
    const v = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const c = [...document.querySelectorAll('a,span,button,input[type=button],div')].filter(v).filter(e => (e.innerText || e.value || '').trim() === '说明');
    if (!c.length) return { ok: false };
    // 找「库存显示方式」那一行的说明
    let target = c[0];
    for (const el of c) {
      let n = el, txt = '';
      for (let i = 0; i < 6 && n; i++) { const p = n.previousElementSibling; if (p && (p.innerText || '').trim()) { txt = (p.innerText || '').trim(); break; } n = n.parentElement; }
      if (/库存显示方式/.test(txt)) { target = el; break; }
    }
    target.click();
    return { ok: true, n: c.length };
  }).catch(e => ({ ok: false, err: e.message }));
  await sleep(3000);
  R.pages.stockNote = {
    click: note,
    popups: await page.evaluate(() => {
      const v = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
      return [...document.querySelectorAll('[role=dialog],[class*=dijitTooltip],[class*=TooltipDialog],[class*=tooltipDialog],[class*=popup]')].filter(v)
        .map(e => ({ cls: String(e.className).slice(0, 70), t: (e.innerText || '').replace(/\s+/g, ' ').trim().slice(0, 600) })).filter(x => x.t && x.t.length > 5);
    }).catch(() => [])
  };
  console.log('  说明:', JSON.stringify(note), JSON.stringify(R.pages.stockNote.popups).slice(0, 800));
  await page.screenshot({ path: path.join(OUT, '_fix5_库存说明.png'), fullPage: true }).catch(() => {});

  fs.writeFileSync(path.join(OUT, '_probe_trade_fix5_raw.json'), JSON.stringify(R, null, 1), 'utf-8');
  console.log('\nWROTE _probe_trade_fix5_raw.json');
  await browser.close();
  console.log('DONE');
})().catch(e => { console.error('FATAL', e.stack || e.message); process.exit(1); });
