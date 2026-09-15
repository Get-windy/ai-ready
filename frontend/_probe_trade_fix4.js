// 终版探针：靠 dijit registry 读所有下拉选项全集
// 目标: 运费设置(物流/到店自提) · 店铺设置(注册设置) · 单位显示 · 商品上架 · 库存显示方式说明
// 用法: node _probe_trade_fix4.js [outDir]
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const OUT = process.argv[2] || '../tool-results/ql361/pages';
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

// 读取本页所有可见下拉组件（含选项全集）+ 附近 label 原文
const READ_WIDGETS = () => {
  const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
  const norm = s => (s || '').replace(/\s+/g, ' ').trim();
  const out = [];
  const seen = new Set();

  function labelFor(el) {
    // 1) 最近的 FormField 兄弟（ql361 的 layout: <div FormField>标题</div><div>控件</div>）
    let node = el, hops = 0;
    while (node && hops < 6) {
      const prev = node.previousElementSibling;
      if (prev) {
        const t = norm(prev.innerText);
        if (t && t.length <= 30) return t;
      }
      const par = node.parentElement;
      if (par) {
        const sib = [...par.children].filter(c => c !== node);
        for (const s of sib) {
          const t = norm(s.innerText);
          if (t && t.length <= 30 && !s.querySelector('input,select,button')) return t;
        }
      }
      node = node.parentElement; hops++;
    }
    return '';
  }

  for (const el of document.querySelectorAll('[role=listbox], [class*=dijitSelect], input[type=checkbox], input.dijitCheckBoxInput, [class*=dijitToggle]')) {
    if (!vis(el)) continue;
    if (el.classList.contains('dijitSelectValue') || el.classList.contains('dijitSelectLabel')) continue;
    const key = (el.id || '') + '|' + String(el.className).slice(0, 40) + '|' + out.length;
    if (seen.has(key)) continue; seen.add(key);

    const rec = {
      tag: el.tagName.toLowerCase(), type: el.type || '', id: el.id || '',
      cls: String(el.className).slice(0, 110),
      label: labelFor(el),
      declaredClass: '', options: null, checked: typeof el.checked === 'boolean' ? el.checked : undefined,
      inputValue: ''
    };
    if (el.id && window.dijit && dijit.byId && dijit.byId(el.id)) {
      try {
        const w = dijit.byId(el.id);
        rec.declaredClass = w.declaredClass || '';
        const o = (w.get && w.get('options')) || w.options;
        if (o && typeof o.length === 'number' && o.length) {
          rec.options = [...o].filter(x => x && x.value && typeof x.value === 'object' && 'label' in x.value)
            .map(x => ({ value: x.value.value, label: x.value.label, selected: !!x.value.selected }));
        }
        // 复选框/开关的 tooltip
        if (w.tooltip) rec.tooltip = String(w.tooltip).slice(0, 200);
        const ttc = el.getAttribute('data-dojo-props') || '';
        if (ttc && /toolTipContent/.test(ttc)) rec.toolTipContent = ttc.slice(0, 300);
      } catch (e) { rec.dojoErr = String(e.message).slice(0, 60); }
    }
    const inn = el.querySelector ? el.querySelector('input.dijitInputInner, input[type=hidden][data-dojo-attach-point=valueNode]') : null;
    if (inn) rec.inputValue = inn.value;
    if (el.type === 'checkbox') {
      const row = el.closest('tr,[class*=form-group],[class*=dijitInline]') || el.parentElement;
      rec.rowText = row ? norm(row.innerText).slice(0, 80) : '';
    }
    out.push(rec);
  }
  // 页面上的 tooltip 文本（红字/说明）
  const notes = [...document.querySelectorAll('[class*=tooltip], [class*=ToolTip], [style*=color:red], [style*=color: red], .lk-form-tooltip-help')]
    .filter(vis).map(e => norm(e.innerText)).filter(t => t && t.length < 300);
  return { widgets: out, notes: [...new Set(notes)] };
};

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 }, locale: 'zh-CN' });
  const page = await ctx.newPage();
  const R = { ts: new Date().toISOString(), pages: {} };

  page.on('pageerror', e => { });

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

  // ============ 1. 运费设置 ============
  console.log('\n=== 运费设置 ===');
  await openItem('运费设置');
  R.pages.freight = { opened: true };
  // 物流子项
  await clickSub('物流');
  // 记录开关（按 name 精确定位）
  R.pages.freight.toggles = await page.evaluate(() => {
    const v = e => e && e.offsetParent !== null;
    return [...document.querySelectorAll('input.dijitCheckBoxInput')].filter(v).map(c => {
      const row = c.closest('tr,[class*=form-group],[class*=dijitInline]') || c.parentElement;
      let lab = '';
      let node = c;
      for (let i = 0; i < 6 && node; i++) { const p = node.previousElementSibling; if (p && (p.innerText || '').trim() && (p.innerText || '').trim().length < 20) { lab = (p.innerText || '').trim(); break; } node = node.parentElement; }
      return { id: c.id, name: c.name, checked: c.checked, label: lab, rowText: row ? (row.innerText || '').replace(/\s+/g, ' ').trim().slice(0, 60) : '' };
    });
  }).catch(e => ({ err: e.message }));
  console.log('  toggles:', JSON.stringify(R.pages.freight.toggles));
  // 开「启用物流」+「自有配送」+「到店自提」，观察各自数据区
  R.pages.freight.enabledStates = await page.evaluate(() => {
    const res = {};
    for (const name of ['selfmention', 'isexpress', 'gostore_ask']) {
      const cb = [...document.querySelectorAll('input.dijitCheckBoxInput')].find(c => c.name === name);
      if (!cb) { res[name] = 'NOT_FOUND'; continue; }
      if (!cb.checked) cb.click();
      res[name] = cb.checked;
    }
    return res;
  }).catch(e => ({ err: e.message }));
  await sleep(7000); await dismiss(); await sleep(2000);
  console.log('  enabledStates:', JSON.stringify(R.pages.freight.enabledStates));
  R.pages.freight.widgets = await page.evaluate(READ_WIDGETS).catch(e => ({ err: e.message }));
  R.pages.freight.panelText = await page.evaluate(() => {
    const v = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const h = [...document.querySelectorAll('[class*=tab-list-main],[class*=dockbody],[class*=inner-content]')].filter(v).sort((a, b) => (b.innerText || '').length - (a.innerText || '').length)[0];
    return h ? (h.innerText || '') : '';
  }).catch(() => '');
  console.log('  widgets:');
  for (const w of (R.pages.freight.widgets.widgets || [])) {
    console.log(`    [${w.label}] ${w.declaredClass} id=${w.id} ${w.options ? 'OPTIONS=' + JSON.stringify(w.options.map(o => o.label)) : (w.type === 'checkbox' ? 'CHECKBOX checked=' + w.checked + ' row=' + w.rowText : '')}`);
  }
  console.log('  notes:', JSON.stringify(R.pages.freight.widgets.notes));
  await page.screenshot({ path: path.join(OUT, '_fix4_运费设置_物流全开.png'), fullPage: true }).catch(() => {});
  R.pages.freight.panelHtml = await page.evaluate(() => {
    const v = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const h = [...document.querySelectorAll('[class*=tab-list-main],[class*=dockbody],[class*=inner-content]')].filter(v).sort((a, b) => (b.innerText || '').length - (a.innerText || '').length)[0];
    return h ? h.outerHTML : '';
  }).catch(() => '');
  fs.writeFileSync(path.join(OUT, '_fix4_运费设置_物流_panel.html'), R.pages.freight.panelHtml, 'utf-8');
  // 自有配送数据区截图（关掉物流看自有配送）
  await page.evaluate(() => { const cb = [...document.querySelectorAll('input.dijitCheckBoxInput')].find(c => c.name === 'isexpress'); if (cb && cb.checked) cb.click(); });
  await sleep(5000); await dismiss(); await sleep(1500);
  R.pages.freight.selfDeliveryPanelText = await page.evaluate(() => {
    const v = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const h = [...document.querySelectorAll('[class*=tab-list-main],[class*=dockbody],[class*=inner-content]')].filter(v).sort((a, b) => (b.innerText || '').length - (a.innerText || '').length)[0];
    return h ? (h.innerText || '') : '';
  }).catch(() => '');
  await page.screenshot({ path: path.join(OUT, '_fix4_运费设置_自有配送.png'), fullPage: true }).catch(() => {});
  console.log('  自有配送面板:', (R.pages.freight.selfDeliveryPanelText || '').replace(/\n/g, ' | ').slice(0, 500));

  // 到店自提
  await clickSub('到店自提');
  R.pages.pickup = { widgets: await page.evaluate(READ_WIDGETS).catch(e => ({ err: e.message })), panelText: await page.evaluate(() => document.body.innerText.slice(0, 4000)).catch(() => '') };
  await page.screenshot({ path: path.join(OUT, '_fix4_运费设置_到店自提.png'), fullPage: true }).catch(() => {});
  console.log('  到店自提 widgets:', JSON.stringify(R.pages.pickup.widgets.widgets || []).slice(0, 800));

  // ============ 2. 店铺设置 → 注册设置 ============
  console.log('\n=== 店铺设置 ===');
  await openItem('店铺设置');
  await clickSub('店铺参数');   // 先激活页面（已证明有效路径）
  R.pages.shopParams = { widgets: await page.evaluate(READ_WIDGETS).catch(e => ({ err: e.message })) };
  console.log('  店铺参数 widgets:');
  for (const w of (R.pages.shopParams.widgets.widgets || [])) {
    console.log(`    [${w.label}] ${w.declaredClass} ${w.options ? 'OPTIONS=' + JSON.stringify(w.options.map(o => o.label)) : (w.type === 'checkbox' ? 'CHECKBOX checked=' + w.checked : '')}`);
  }
  // 库存显示方式「说明」按钮
  const noteRes = await page.evaluate(() => {
    const v = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const cands = [...document.querySelectorAll('a,span,button,input[type=button],div')].filter(v).filter(e => (e.innerText || e.value || '').trim() === '说明');
    if (!cands.length) return { ok: false };
    cands[0].click();
    return { ok: true, n: cands.length, cls: String(cands[0].className).slice(0, 70) };
  }).catch(e => ({ ok: false, err: e.message }));
  await sleep(2500);
  R.pages.stockNote = {
    click: noteRes,
    visibleDialogs: await page.evaluate(() => {
      const v = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
      return [...document.querySelectorAll('[class*=dialog],[class*=popup],[class*=Dialog],[class*=tooltip]')].filter(v)
        .map(e => ({ cls: String(e.className).slice(0, 70), t: (e.innerText || '').replace(/\s+/g, ' ').trim().slice(0, 400) }))
        .filter(x => x.t);
    }).catch(() => []),
    bodySnippet: await page.evaluate(() => (document.body.innerText || '').slice(0, 3000)).catch(() => '')
  };
  await page.screenshot({ path: path.join(OUT, '_fix4_库存显示方式_说明.png'), fullPage: true }).catch(() => {});
  console.log('  说明按钮:', JSON.stringify(noteRes), JSON.stringify(R.pages.stockNote.visibleDialogs).slice(0, 400));
  await page.keyboard.press('Escape').catch(() => {});
  await sleep(800);

  // 注册设置
  let regOk = false;
  for (let i = 1; i <= 3 && !regOk; i++) {
    R.pages.registerClick = await clickSub('注册设置');
    const b = await page.evaluate(() => document.body.innerText || '').catch(() => '');
    regOk = /允许注册账号/.test(b);
    console.log(`  注册设置 renders=${regOk} (try ${i})`);
    if (!regOk) { await clickSub('店铺参数'); await sleep(2000); }
  }
  R.pages.register = {
    rendered: regOk,
    widgets: await page.evaluate(READ_WIDGETS).catch(e => ({ err: e.message })),
    panelText: await page.evaluate(() => {
      const v = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
      const h = [...document.querySelectorAll('[class*=tab-list-main],[class*=dockbody],[class*=inner-content]')].filter(v).sort((a, b) => (b.innerText || '').length - (a.innerText || '').length)[0];
      return h ? (h.innerText || '') : '';
    }).catch(() => '')
  };
  R.pages.register.panelHtml = await page.evaluate(() => {
    const v = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const h = [...document.querySelectorAll('[class*=tab-list-main],[class*=dockbody],[class*=inner-content]')].filter(v).sort((a, b) => (b.innerText || '').length - (a.innerText || '').length)[0];
    return h ? h.outerHTML : '';
  }).catch(() => '');
  fs.writeFileSync(path.join(OUT, '_fix4_注册设置_panel.html'), R.pages.register.panelHtml, 'utf-8');
  console.log('  注册设置 panelText:', (R.pages.register.panelText || '').replace(/\n/g, ' | ').slice(0, 600));
  console.log('  注册设置 widgets:');
  for (const w of (R.pages.register.widgets.widgets || [])) {
    console.log(`    [${w.label}] ${w.declaredClass} id=${w.id} val=${w.inputValue} ${w.options ? 'OPTIONS=' + JSON.stringify(w.options) : (w.type === 'checkbox' ? 'CHECKBOX checked=' + w.checked : '')} ${w.toolTipContent || ''}`);
  }
  console.log('  注册设置 notes:', JSON.stringify(R.pages.register.widgets.notes));
  await page.screenshot({ path: path.join(OUT, '_fix4_注册设置.png'), fullPage: true }).catch(() => {});

  // ============ 3. 单位显示 ============
  console.log('\n=== 单位显示 ===');
  await openItem('单位显示');
  await sleep(4000); await dismiss(); await sleep(2000);
  R.pages.unitDisplay = {
    widgets: await page.evaluate(READ_WIDGETS).catch(e => ({ err: e.message })),
    panelText: await page.evaluate(() => document.body.innerText.slice(0, 6000)).catch(() => '')
  };
  console.log('  单位显示 widgets:');
  for (const w of (R.pages.unitDisplay.widgets.widgets || [])) {
    if (w.options || w.type === 'checkbox') console.log(`    [${w.label}] ${w.declaredClass} ${w.options ? 'OPTIONS=' + JSON.stringify(w.options) : 'CHECKBOX checked=' + w.checked}`);
  }
  await page.screenshot({ path: path.join(OUT, '_fix4_单位显示.png'), fullPage: true }).catch(() => {});
  // 列配置
  const gear = await page.evaluate(() => {
    const v = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const g = [...document.querySelectorAll('.dgrid-column-set .icon-shezhi2, [class*=shezhi]')].filter(v);
    if (!g.length) return { ok: false, n: 0 };
    g[0].click(); return { ok: true, n: g.length, cls: String(g[0].className).slice(0, 80) };
  }).catch(e => ({ ok: false, err: e.message }));
  await sleep(3000);
  R.pages.unitDisplayColCfg = {
    gear,
    dialogText: await page.evaluate(() => {
      const v = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
      return [...document.querySelectorAll('[role=dialog],[class*=dialog],[class*=Dialog],[class*=popup]')].filter(v)
        .map(e => ({ cls: String(e.className).slice(0, 60), t: (e.innerText || '').replace(/\n{2,}/g, '\n').trim().slice(0, 2500) }))
        .filter(x => x.t && x.t.length > 30);
    }).catch(() => []),
    checkedBoxes: await page.evaluate(() => {
      const v = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
      return [...document.querySelectorAll('[role=dialog] input[type=checkbox],[class*=dialog] input[type=checkbox]')].filter(v)
        .map(c => { const l = c.closest('label') || c.parentElement; return { checked: c.checked, label: l ? (l.innerText || '').replace(/\s+/g, ' ').trim().slice(0, 40) : '' }; });
    }).catch(() => [])
  };
  await page.screenshot({ path: path.join(OUT, '_fix4_单位显示_列配置.png'), fullPage: true }).catch(() => {});
  console.log('  列配置:', JSON.stringify(gear), JSON.stringify(R.pages.unitDisplayColCfg.dialogText).slice(0, 900));
  await page.keyboard.press('Escape').catch(() => {});

  // ============ 4. 商品上架 ============
  console.log('\n=== 商品上架 ===');
  await openItem('商品上架');
  await sleep(4000); await dismiss(); await sleep(2000);
  R.pages.onShelf = {
    widgets: await page.evaluate(READ_WIDGETS).catch(e => ({ err: e.message })),
    panelText: await page.evaluate(() => document.body.innerText.slice(0, 6000)).catch(() => '')
  };
  console.log('  商品上架 widgets:');
  for (const w of (R.pages.onShelf.widgets.widgets || [])) {
    if (w.options || w.type === 'checkbox') console.log(`    [${w.label}] ${w.declaredClass} ${w.options ? 'OPTIONS=' + JSON.stringify(w.options) : 'CHECKBOX checked=' + w.checked}`);
  }
  await page.screenshot({ path: path.join(OUT, '_fix4_商品上架.png'), fullPage: true }).catch(() => {});

  fs.writeFileSync(path.join(OUT, '_probe_trade_fix4_raw.json'), JSON.stringify(R, null, 1), 'utf-8');
  console.log('\nWROTE _probe_trade_fix4_raw.json');
  await browser.close();
  console.log('DONE');
})().catch(e => { console.error('FATAL', e.stack || e.message); process.exit(1); });
