// 直连 API + Dojo 组件选项读取：拿到完整（不截断）的字段/枚举
// 用法: node _probe_trade_api.js [outDir]
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const OUT = process.argv[2] || '../tool-results/ql361/pages';
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 }, locale: 'zh-CN' });
  const page = await ctx.newPage();

  // 捕获一个完整的 api.cc URL 模板（含 tok/s）
  let apiTemplate = null;
  page.on('request', (req) => {
    const u = req.url();
    if (u.includes('api.cc') && u.includes('___method=') && u.includes('tok=')) {
      if (!apiTemplate || u.includes('ShopSettingFreight')) apiTemplate = u;
    }
  });

  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'networkidle', timeout: 45000 }).catch(() => {});
  await page.fill('input[placeholder="请输入手机号/用户名"]', '15095664266').catch(() => {});
  await page.fill('#pwd', 'Ysh790921').catch(() => {});
  await page.click('button.logon_button').catch(() => {});
  for (let i = 0; i < 30; i++) { await sleep(1000); if (!page.url().includes('/Account/Logon')) break; }
  await sleep(9000);
  await page.keyboard.press('Escape').catch(() => {});
  console.log('LOGIN OK');

  async function openMallItem(item) {
    await page.locator('a.menu-0-item', { hasText: '商城' }).first().click({ force: true }).catch(() => {});
    await sleep(1600);
    await page.evaluate((item) => {
      const vis = e => e && e.offsetParent !== null;
      const el = [...document.querySelectorAll('.popupmenu__nav-text')].filter(vis)
        .find(e => (e.innerText || '').replace(/\s+/g, '').trim() === item);
      if (el) el.click();
    }, item).catch(() => {});
    await sleep(10000);
    for (const fr of page.frames()) {
      try { await fr.evaluate(() => { const vis = e => e && e.offsetParent !== null; const b = [...document.querySelectorAll('button,a,div,span,input[type=button]')].filter(vis).find(x => /^(我知道了)$/.test((x.innerText || x.value || '').trim())); if (b) b.click(); }); } catch (e) {}
    }
    await sleep(1500);
  }

  // 打开「物流」子项 + 点亮「启用物流」
  async function enableLogisticsTab() {
    // 点「物流」子项
    await page.evaluate(() => {
      const vis = e => e && e.offsetParent !== null;
      const el = [...document.querySelectorAll('.tab-text')].filter(vis).find(e => (e.innerText || '').trim() === '物流');
      if (el) el.click();
    }).catch(() => {});
    await sleep(4000);
    const st = await page.evaluate(() => {
      const cb = document.querySelector('#dijit_form_CheckBox_3') ||
        [...document.querySelectorAll('input[type=checkbox]')].find(c => c.name === 'isexpress');
      if (!cb) return { found: false };
      if (!cb.checked) { cb.click(); }
      return { found: true, checked: cb.checked, name: cb.name, id: cb.id };
    }).catch(e => ({ err: e.message }));
    await sleep(5000);
    const st2 = await page.evaluate(() => {
      const cb = [...document.querySelectorAll('input[type=checkbox]')].find(c => c.name === 'isexpress');
      return cb ? { checked: cb.checked, id: cb.id } : { missing: true };
    }).catch(e => ({ err: e.message }));
    return { first: st, after: st2 };
  }

  const R = { ts: new Date().toISOString(), api: {}, widgets: {}, dom: {} };

  // ---------- 运费设置 ----------
  console.log('\n--- 运费设置 ---');
  await openMallItem('运费设置');
  R.logisticsSwitches = await page.evaluate(() => {
    const out = [];
    for (const cb of document.querySelectorAll('input[type=checkbox]')) {
      const row = cb.closest('tr,.form-group,.dijitInline,[class*=form-item],[class*=field]') || cb.parentElement;
      out.push({ id: cb.id, name: cb.name, checked: cb.checked, rowText: row ? (row.innerText || '').replace(/\s+/g, ' ').trim().slice(0, 60) : '' });
    }
    return out;
  }).catch(e => ({ err: e.message }));
  R.enable = await enableLogisticsTab();
  console.log('  enable:', JSON.stringify(R.enable));
  await page.screenshot({ path: path.join(OUT, '_fix3_物流_启用后.png') }).catch(() => {});

  // 完整面板 HTML（不截断）
  R.dom.freightHtml = await page.evaluate(() => {
    const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const host = [...document.querySelectorAll('[class*=tab-list-main],[class*=dockbody],form,[class*=inner-content]')].filter(vis)
      .sort((a, b) => (b.innerText || '').length - (a.innerText || '').length)[0];
    return host ? host.outerHTML : '';
  }).catch(e => 'ERR ' + e.message);
  fs.writeFileSync(path.join(OUT, '_fix3_运费设置_物流_panel.html'), R.dom.freightHtml, 'utf-8');

  // 列出本页所有 dijit 下拉组件（含运费计算方式）+ 尝试读 options
  R.widgets.freight = await page.evaluate(() => {
    const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const out = [];
    for (const el of document.querySelectorAll('[role=listbox], [class*=dijitSelect], select, [data-dojo-attach-point*=_popupStateNode]')) {
      if (!vis(el)) continue;
      const rec = { tag: el.tagName.toLowerCase(), id: el.id, cls: String(el.className).slice(0, 120), ariaExpanded: el.getAttribute('aria-expanded') };
      // 附近 label
      let lab = '';
      const host = el.closest('[class*=form-group],[class*=FormField],tr,div');
      if (host) {
        const prev = host.previousElementSibling;
        lab = prev ? (prev.innerText || '').replace(/\s+/g, ' ').trim().slice(0, 40) : '';
        if (!lab) {
          const c = host.cloneNode(true); c.querySelectorAll('input,select,button').forEach(n => n.remove());
          lab = (c.innerText || '').replace(/\s+/g, ' ').trim().slice(0, 40);
        }
      }
      rec.label = lab;
      // Dojo registry 尝试
      try {
        if (window.dijit && el.id && dijit.byId(el.id)) {
          const w = dijit.byId(el.id);
          rec.declaredClass = w.declaredClass;
          const opts = (w.get && w.get('options')) || w.options;
          if (opts) rec.options = (Array.isArray(opts) ? opts : Object.keys(opts).map(k => ({ label: k, value: opts[k] })))
            .map(o => ({ label: o.label, value: o.value }));
          rec.props = Object.keys(w).filter(k => /option|store|data|items|list|value|text/i.test(k)).slice(0, 25);
        }
      } catch (e) { rec.dojoErr = e.message.slice(0, 80); }
      // data-dojo-props
      const dp = el.getAttribute('data-dojo-props');
      if (dp) rec.dojoProps = dp.slice(0, 1500);
      const inn = el.querySelector('input.dijitInputInner');
      if (inn) rec.inputValue = inn.value;
      out.push(rec);
    }
    return out;
  }).catch(e => ({ err: e.message }));

  // 逐个打开下拉并 dump 弹层（只取与选中组件同源的弹层）
  R.dropdowns = [];
  const nW = Array.isArray(R.widgets.freight) ? R.widgets.freight.length : 0;
  for (let i = 0; i < nW; i++) {
    const w = R.widgets.freight[i];
    const r = await page.evaluate(({ id, idx }) => {
      const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
      const els = [...document.querySelectorAll('[role=listbox],[class*=dijitSelect]')].filter(vis);
      const el = els[idx];
      if (!el) return { ok: false };
      // 关掉已开弹层
      document.body.click();
      const arrow = el.querySelector('[class*=ArrowButton]') || el;
      for (const ev of ['mousedown', 'mouseup', 'click']) {
        arrow.dispatchEvent(new MouseEvent(ev, { bubbles: true, cancelable: true, view: window }));
      }
      return { ok: true, id: el.id, arrowCls: String(arrow.className).slice(0, 60) };
    }, { id: w.id, idx: i }).catch(e => ({ ok: false, err: e.message }));
    await sleep(1600);
    const pop = await page.evaluate(() => {
      const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
      const pops = [...document.querySelectorAll('.dijitPopup, [class*=dropdown], [class*=popup], [role=listbox] ul, .dijitMenu')].filter(vis);
      const res = [];
      for (const p of pops) {
        const items = [...p.querySelectorAll('li,tr,div,span,a')].filter(e => e.children.length === 0 && (e.innerText || '').trim())
          .map(e => (e.innerText || '').replace(/\s+/g, ' ').trim()).filter(t => t && t.length < 40);
        if (items.length) res.push({ popCls: String(p.className).slice(0, 70), items: [...new Set(items)] });
      }
      return res;
    }).catch(e => [{ err: e.message }]);
    R.dropdowns.push({ idx: i, w, open: r, pop });
    console.log(`  dd#${i} [${w.label}] open=${JSON.stringify(r)} pops=${JSON.stringify(pop).slice(0, 300)}`);
    await page.screenshot({ path: path.join(OUT, `_fix3_运费设置_物流_dd${i}.png`) }).catch(() => {});
    await page.keyboard.press('Escape').catch(() => {});
    await sleep(600);
  }

  // ---------- 直连 API：完整数据 ----------
  R.apiTemplate = apiTemplate;
  if (apiTemplate) {
    const methods = [
      'cc.erp.bll.MallFreight.GetFreight',
      'CC.ERP.BLL.DH.Baseinfo.B2B2CSettings.GetSettings',
      'CC.ERP.BLL.DH.Baseinfo.B2B2CSettings.GetRegCoupons',
      'cc.erp.bll.bas.BasGoodsTags.getcanusedatalist',
      'cc.erp.bll.bas.dealertype.search',
      'cc.erp.bll.bas.GoodsUnit.GetMultiUnitList'
    ];
    for (const m of methods) {
      const url = apiTemplate.replace(/___method=[^&]*/i, '___method=' + m);
      const body = await page.evaluate(async (url) => {
        try { const r = await fetch(url, { credentials: 'include' }); return await r.text(); }
        catch (e) { return 'FETCH_ERR ' + e.message; }
      }, url).catch(e => 'EVAL_ERR ' + e.message);
      const safe = m.replace(/[^\w\.]/g, '_');
      fs.writeFileSync(path.join(OUT, `_api_${safe}.json`), String(body), 'utf-8');
      R.api[m] = { url, len: String(body).length };
      console.log(`  API ${m} -> ${String(body).length} chars`);
    }
  } else {
    console.log('  !! 未捕获 api.cc URL 模板');
  }

  // ---------- 店铺设置 → 注册设置 ----------
  console.log('\n--- 店铺设置/注册设置 ---');
  await openMallItem('店铺设置');
  await sleep(3000);
  await page.evaluate(() => {
    const vis = e => e && e.offsetParent !== null;
    const el = [...document.querySelectorAll('.tab-text')].filter(vis).find(e => (e.innerText || '').trim() === '店铺参数');
    if (el) el.click();
  }).catch(() => {});
  await sleep(5000);
  await page.evaluate(() => {
    const vis = e => e && e.offsetParent !== null;
    const el = [...document.querySelectorAll('.tab-text')].filter(vis).find(e => (e.innerText || '').trim() === '注册设置');
    if (el) el.click();
  }).catch(() => {});
  await sleep(8000);
  await page.screenshot({ path: path.join(OUT, '_fix3_注册设置.png') }).catch(() => {});
  R.dom.registerHtml = await page.evaluate(() => {
    const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const host = [...document.querySelectorAll('[class*=tab-list-main],[class*=dockbody],form,[class*=inner-content]')].filter(vis)
      .sort((a, b) => (b.innerText || '').length - (a.innerText || '').length)[0];
    return host ? host.outerHTML : '';
  }).catch(e => 'ERR ' + e.message);
  fs.writeFileSync(path.join(OUT, '_fix3_注册设置_panel.html'), R.dom.registerHtml, 'utf-8');
  R.widgets.register = await page.evaluate(() => {
    const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const out = [];
    for (const el of document.querySelectorAll('[role=listbox],[class*=dijitSelect],select,input[type=checkbox],input[type=button]')) {
      if (!vis(el)) continue;
      const rec = { tag: el.tagName.toLowerCase(), type: el.type || '', id: el.id, name: el.name || '', cls: String(el.className).slice(0, 90) };
      const c = el.closest('[class*=form-group],[class*=FormField],tr');
      let lab = '';
      if (c) { const p = c.previousElementSibling; lab = p ? (p.innerText || '').replace(/\s+/g, ' ').trim().slice(0, 40) : ''; }
      if (!lab && c) { const cc = c.cloneNode(true); cc.querySelectorAll('input,select,button').forEach(n => n.remove()); lab = (cc.innerText || '').replace(/\s+/g, ' ').trim().slice(0, 50); }
      rec.label = lab;
      const inn = el.querySelector ? el.querySelector('input.dijitInputInner') : null;
      if (inn) rec.inputValue = inn.value;
      const dp = el.getAttribute('data-dojo-props');
      if (dp) rec.dojoProps = dp.slice(0, 1200);
      try {
        if (window.dijit && el.id && dijit.byId(el.id)) {
          const w = dijit.byId(el.id);
          rec.declaredClass = w.declaredClass;
          const o = (w.get && w.get('options')) || w.options;
          if (o) rec.options = (Array.isArray(o) ? o : Object.keys(o).map(k => ({ label: k, value: o[k] }))).map(x => ({ label: x.label, value: x.value }));
        }
      } catch (e) {}
      out.push(rec);
    }
    return out;
  }).catch(e => ({ err: e.message }));
  // 注册设置下拉逐个打开
  R.registerDropdowns = [];
  const regSelects = await page.evaluate(() => {
    const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    return [...document.querySelectorAll('[role=listbox],[class*=dijitSelect]')].filter(vis).map((e, i) => ({ i, id: e.id }));
  }).catch(() => []);
  for (const s of regSelects) {
    const r = await page.evaluate((idx) => {
      const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
      const els = [...document.querySelectorAll('[role=listbox],[class*=dijitSelect]')].filter(vis);
      const el = els[idx]; if (!el) return { ok: false };
      const arrow = el.querySelector('[class*=ArrowButton]') || el;
      for (const ev of ['mousedown', 'mouseup', 'click']) arrow.dispatchEvent(new MouseEvent(ev, { bubbles: true, cancelable: true, view: window }));
      return { ok: true, id: el.id };
    }, s.i).catch(e => ({ ok: false, err: e.message }));
    await sleep(1600);
    const pop = await page.evaluate(() => {
      const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
      const pops = [...document.querySelectorAll('.dijitPopup,[class*=dropdown],[class*=popup]')].filter(vis);
      const res = [];
      for (const p of pops) {
        const items = [...p.querySelectorAll('li,tr,div,span,a')].filter(e => e.children.length === 0 && (e.innerText || '').trim())
          .map(e => (e.innerText || '').replace(/\s+/g, ' ').trim()).filter(t => t && t.length < 40);
        if (items.length) res.push({ popCls: String(p.className).slice(0, 70), items: [...new Set(items)] });
      }
      return res;
    }).catch(e => [{ err: e.message }]);
    R.registerDropdowns.push({ sel: s, open: r, pop });
    console.log(`  reg dd#${s.i} open=${JSON.stringify(r)} pops=${JSON.stringify(pop).slice(0, 400)}`);
    await page.screenshot({ path: path.join(OUT, `_fix3_注册设置_dd${s.i}.png`) }).catch(() => {});
    await page.keyboard.press('Escape').catch(() => {});
    await sleep(600);
  }

  fs.writeFileSync(path.join(OUT, '_probe_trade_api_raw.json'), JSON.stringify(R, null, 1), 'utf-8');
  console.log('\nWROTE _probe_trade_api_raw.json');
  await browser.close();
  console.log('DONE');
})().catch(e => { console.error('FATAL', e.stack || e.message); process.exit(1); });
