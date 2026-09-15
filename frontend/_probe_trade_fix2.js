// 精修补抓：运费设置(物流数据区) + 店铺设置(注册设置下拉选项) + 追加目标A/B
// 用法: node _probe_trade_fix2.js [outDir]
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const OUT = process.argv[2] || '../tool-results/ql361/pages';
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

const R = { ts: new Date().toISOString(), steps: [], results: {} };
const LOG = [];
const log = (...a) => { const s = a.join(' '); console.log(s); LOG.push(s); };

// ---------- 页面内工具 ----------
const P = {
  // 仅主文档：列可见的 dijit 组合框（预览商城在独立 frame 内，天然被排除）
  combos: () => {
    const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const out = [];
    for (const inp of document.querySelectorAll('input.dijitInputInner, input[role=combobox], select')) {
      if (!vis(inp)) continue;
      const wrap = inp.closest('.dijitComboBox, .dijitSelect, .dijitDropDownButton, td, .form-group, tr') || inp.parentElement;
      const clone = wrap ? wrap.cloneNode(true) : null;
      if (clone) clone.querySelectorAll('input,button,select').forEach(n => n.remove());
      out.push({
        idx: out.length,
        id: inp.id || '',
        cls: String(inp.className).slice(0, 60),
        value: String(inp.value || ''),
        wrapCls: wrap ? String(wrap.className).slice(0, 80) : '',
        wrapText: clone ? (clone.innerText || '').replace(/\s+/g, ' ').trim().slice(0, 80) : '',
        readonly: !!inp.readOnly,
        disabled: !!inp.disabled
      });
    }
    return out;
  },
  // dump 开关/复选框所在行的完整结构
  switchHtml: () => {
    const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const out = [];
    for (const el of document.querySelectorAll('label, span, div, td')) {
      if (!vis(el)) continue;
      const t = (el.innerText || '').replace(/\s+/g, '').trim();
      if (t !== '启用物流' && t !== '自有配送' && t !== '允许注册账号') continue;
      const row = el.closest('tr, .form-group, [class*=form-item], [class*=field], .dijitInline') || el.parentElement;
      if (row) out.push({ anchorText: t, rowCls: String(row.className).slice(0, 80), rowHtml: row.outerHTML.slice(0, 2500) });
    }
    return out;
  },
  dumpPanel: () => {
    const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    // 配置面板容器：选可见且含「保存」按钮的最内层容器
    const cands = [...document.querySelectorAll('[class*=tab-list-main], [class*=dockbody], [class*=panel], [class*=content], [class*=form-content], form')].filter(vis);
    cands.sort((a, b) => (a.innerText || '').length - (b.innerText || '').length);
    const host = cands.find(c => /保存/.test(c.innerText || '')) || cands[cands.length - 1];
    return {
      hostCls: host ? String(host.className).slice(0, 120) : '',
      text: host ? (host.innerText || '').replace(/\n{2,}/g, '\n').trim() : '',
      html: host ? host.outerHTML.slice(0, 30000) : '',
      tables: host ? [...host.querySelectorAll('table')].map(t => ({
        cls: String(t.className).slice(0, 60),
        head: [...t.querySelectorAll('thead th,thead td,tr:first-child th')].map(e => (e.innerText || '').replace(/\s+/g, ' ').trim()),
        rows: [...t.querySelectorAll('tbody tr, tr')].slice(0, 8).map(tr => [...tr.querySelectorAll('td,th')].map(td => (td.innerText || '').replace(/\s+/g, ' ').trim().slice(0, 60)))
      })) : [],
      controls: host ? [...host.querySelectorAll('input,select,textarea,button,[class*=switch]')].map(e => ({
        tag: e.tagName.toLowerCase(), type: e.type || '', cls: String(e.className).slice(0, 60),
        value: String(e.value || '').slice(0, 60), checked: typeof e.checked === 'boolean' ? e.checked : undefined,
        text: (e.innerText || '').replace(/\s+/g, ' ').trim().slice(0, 40)
      })) : []
    };
  },
  // dump 可见的下拉弹层选项
  popupOptions: () => {
    const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const pops = [...document.querySelectorAll('.dijitPopup, .dijitComboBoxMenu, .dijitMenu, [class*=dropdown], [class*=popup], [role=listbox]')].filter(vis);
    return pops.map(p => ({
      cls: String(p.className).slice(0, 80),
      items: [...p.querySelectorAll('*')].filter(e => e.children.length === 0 && (e.innerText || '').trim())
        .map(e => ({ t: (e.innerText || '').replace(/\s+/g, ' ').trim(), cls: String(e.className).slice(0, 50), v: e.getAttribute('value') || e.getAttribute('data-value') || '' }))
        .filter(x => x.t && x.t.length < 60)
    })).filter(x => x.items.length);
  }
};

async function mainDump(page, label) {
  let d = null;
  try { d = await page.evaluate(P.dumpPanel); } catch (e) { d = { err: e.message }; }
  return d;
}

async function openMallItem(page, item, attempt) {
  // 先点别的顶部菜单「解除」当前页，再回商城
  await page.locator('a.menu-0-item', { hasText: '销售' }).first().click({ force: true }).catch(() => {});
  await sleep(900);
  await page.locator('a.menu-0-item', { hasText: '商城' }).first().click({ force: true }).catch(() => {});
  await sleep(1800);
  const ok = await page.evaluate((item) => {
    const vis = e => e && e.offsetParent !== null;
    const el = [...document.querySelectorAll('.popupmenu__nav-text')].filter(vis)
      .find(e => (e.innerText || '').replace(/\s+/g, '').trim() === item);
    if (el) { el.click(); return true; }
    return false;
  }, item).catch(() => false);
  await sleep(10000);
  return ok;
}

async function dismissError(page) {
  for (const fr of page.frames()) {
    try {
      await fr.evaluate(() => {
        const vis = e => e && e.offsetParent !== null;
        const btns = [...document.querySelectorAll('button, a, div[class*=btn], span[class*=btn], input[type=button]')].filter(vis);
        const ok = btns.find(b => /^(我知道了|确定|关闭)$/.test((b.innerText || b.value || '').trim()));
        if (ok) ok.click();
      });
    } catch (e) {}
  }
  await sleep(700);
}

async function ensureRendered(page, mustHave, tries = 3) {
  for (let i = 1; i <= tries; i++) {
    const ok = await page.evaluate((mustHave) => {
      const b = document.body.innerText || '';
      return mustHave.every(s => b.includes(s)) && b.length > 200;
    }, mustHave).catch(() => false);
    if (ok) return { ok: true, tries: i };
    log(`    渲染未就绪(retry ${i}/${tries})`);
    await dismissError(page);
    await sleep(3000);
  }
  return { ok: false, tries };
}

// 点击某个组合框并 dump 选项
async function clickComboAndDumpOptions(page, idx) {
  const info = await page.evaluate((idx) => {
    const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const list = [...document.querySelectorAll('input.dijitInputInner, input[role=combobox], select')].filter(vis);
    const el = list[idx];
    if (!el) return { ok: false };
    el.scrollIntoView({ block: 'center' });
    const arrow = el.closest('.dijitComboBox, .dijitSelect') ? el.closest('.dijitComboBox, .dijitSelect').querySelector('.dijitArrowButton, .dijitArrowButtonInner, [class*=ArrowButton]') : null;
    const target = arrow || el;
    target.click();
    return { ok: true, usedArrow: !!arrow, val: el.value, id: el.id };
  }, idx).catch(e => ({ ok: false, err: e.message }));
  await sleep(1500);
  const opts = await page.evaluate(P.popupOptions).catch(e => [{ err: e.message }]);
  await page.keyboard.press('Escape').catch(() => {});
  await sleep(400);
  return { info, opts };
}

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 }, locale: 'zh-CN' });
  const page = await ctx.newPage();
  const netLog = [];
  page.on('response', async (res) => {
    try {
      const ct = String(res.headers()['content-type'] || '');
      const u = res.url();
      if (/json/i.test(ct) && !/\.(png|jpg|gif|css|woff)/i.test(u)) {
        let body = ''; try { body = (await res.text()).slice(0, 20000); } catch (e) { body = '<unread>'; }
        netLog.push({ status: res.status(), url: u.slice(0, 250), body });
      }
    } catch (e) {}
  });

  // ===== 登录 =====
  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'networkidle', timeout: 45000 }).catch(() => {});
  await page.fill('input[placeholder="请输入手机号/用户名"]', '15095664266').catch(() => {});
  await page.fill('#pwd', 'Ysh790921').catch(() => {});
  await page.click('button.logon_button').catch(() => {});
  for (let i = 0; i < 30; i++) { await sleep(1000); if (!page.url().includes('/Account/Logon')) break; }
  await sleep(8000);
  await page.keyboard.press('Escape').catch(() => {});
  await dismissError(page);
  R.entry = { url: page.url(), host: new URL(page.url()).host };
  log('LOGIN OK ->', R.entry.host);

  // ============================================================
  // 目标 1：运费设置 → 物流（先 dump 开关 DOM，再真实点亮）
  // ============================================================
  log('\n=== [1] 运费设置 → 物流 ===');
  await openMallItem(page, '运费设置', 1);
  await dismissError(page);
  await sleep(2000);
  const fr1 = await ensureRendered(page, ['运费设置', '启用物流']);
  R.results.freightRender = fr1;
  await page.screenshot({ path: path.join(OUT, '_fix2_运费设置_01物流初始.png') }).catch(() => {});

  R.results.freightSwitchHtml = await page.evaluate(P.switchHtml).catch(e => [{ err: e.message }]);
  log('  开关行 HTML 抓取条数:', R.results.freightSwitchHtml.length);

  // 真实点亮「启用物流」
  const flip = await page.evaluate(() => {
    const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const res = {};
    const findRow = (txt) => {
      for (const el of document.querySelectorAll('label, span, div, td')) {
        if (!vis(el)) continue;
        if ((el.innerText || '').replace(/\s+/g, '').trim() === txt) {
          const row = el.closest('tr, .form-group, [class*=form-item], [class*=field], .dijitInline') || el.parentElement;
          if (row) return row;
        }
      }
      return null;
    };
    for (const name of ['启用物流', '自有配送']) {
      const row = findRow(name);
      if (!row) { res[name] = 'ROW_NOT_FOUND'; continue; }
      const before = {
        cb: row.querySelector('input[type=checkbox]') ? row.querySelector('input[type=checkbox]').checked : null,
        cls: [...row.querySelectorAll('[class*=switch],[class*=Switch],[class*=toggle]')].map(e => String(e.className)).join(';'),
        checkedCls: [...row.querySelectorAll('[class*=checked],[class*=on],[class*=active]')].map(e => String(e.className).join).join(';')
      };
      // 依次尝试：dijit checkbox 的 label、switch 元素、checkbox input 本体
      const cb = row.querySelector('input[type=checkbox]');
      const sw = row.querySelector('[class*=switch],[class*=Switch],[class*=toggle],[class*=Toggle]');
      const lab = row.querySelector('label');
      let clicked = [];
      if (cb && !cb.checked) { cb.click(); clicked.push('input'); }
      if (sw && clicked.length === 0) { sw.click(); clicked.push('switch'); }
      if (lab && clicked.length === 0) { lab.click(); clicked.push('label'); }
      const after = {
        cb: cb ? cb.checked : null,
        cls: [...row.querySelectorAll('[class*=switch],[class*=Switch],[class*=toggle]')].map(e => String(e.className)).join(';')
      };
      res[name] = { before, after, clicked };
    }
    return res;
  }).catch(e => ({ err: e.message }));
  R.results.freightFlip = flip;
  log('  flip:', JSON.stringify(flip));
  await sleep(5000);
  await dismissError(page);
  await sleep(2000);
  await page.screenshot({ path: path.join(OUT, '_fix2_运费设置_02启用物流开.png') }).catch(() => {});

  R.results.freightAfterEnable = await mainDump(page, 'logistics-on');
  log('  after-enable text:', (R.results.freightAfterEnable.text || '').replace(/\n/g, ' | ').slice(0, 400));

  // 若仍无数据区，点保存再看
  const hasDataArea = /物流方式|运费模板|计价|阶梯|模板/.test(R.results.freightAfterEnable.text || '');
  if (!hasDataArea) {
    log('  仍无数据区 → 点底部「保存」');
    await page.evaluate(() => {
      const vis = e => e && e.offsetParent !== null;
      const b = [...document.querySelectorAll('button, input[type=button], a, span, div')].filter(vis)
        .filter(e => ((e.innerText || e.value || '').trim() === '保存')).pop();
      if (b) b.click();
    }).catch(() => {});
    await sleep(8000);
    await dismissError(page);
    await sleep(3000);
    await page.screenshot({ path: path.join(OUT, '_fix2_运费设置_03保存后.png') }).catch(() => {});
    R.results.freightAfterSave = await mainDump(page, 'logistics-saved');
    log('  after-save text:', (R.results.freightAfterSave.text || '').replace(/\n/g, ' | ').slice(0, 400));
  }

  // 到店自提（对照）
  await page.evaluate(() => {
    const vis = e => e && e.offsetParent !== null;
    const el = [...document.querySelectorAll('.tab-text, .nav-tablistcontainer *, [class*=tab-text]')].filter(vis)
      .find(e => (e.innerText || '').trim() === '到店自提');
    if (el) el.click();
  }).catch(() => {});
  await sleep(5000);
  await dismissError(page);
  R.results.freightPickup = await mainDump(page, 'pickup');
  await page.screenshot({ path: path.join(OUT, '_fix2_运费设置_04到店自提.png') }).catch(() => {});

  // ============================================================
  // 目标 2：店铺设置 → 注册设置（下拉选项）
  // ============================================================
  log('\n=== [2] 店铺设置 → 注册设置 ===');
  await openMallItem(page, '店铺设置', 1);
  await dismissError(page);
  await sleep(2000);
  // 先停在「店铺参数」让页面激活，再切「注册设置」
  const fr2 = await ensureRendered(page, ['店铺参数', '注册设置'], 3);
  R.results.shopRender = fr2;
  R.results.shopParams = await mainDump(page, 'params');
  await page.screenshot({ path: path.join(OUT, '_fix2_店铺设置_01店铺参数.png') }).catch(() => {});

  let regOk = false;
  for (let attempt = 1; attempt <= 4 && !regOk; attempt++) {
    log(`  切「注册设置」尝试 ${attempt}`);
    await page.evaluate(() => {
      const vis = e => e && e.offsetParent !== null;
      const el = [...document.querySelectorAll('.tab-text, .nav-tablistcontainer *, [class*=tab-text]')].filter(vis)
        .find(e => (e.innerText || '').trim() === '注册设置');
      if (el) el.click();
    }).catch(() => {});
    await sleep(6000);
    await dismissError(page);
    await sleep(2000);
    const t = await page.evaluate(() => document.body.innerText || '').catch(() => '');
    regOk = /允许注册账号|买家注册默认级别|买家账号注册审核/.test(t);
    log('    注册设置渲染:', regOk);
    if (!regOk && attempt === 2) {
      // 换入口重开
      log('    → 重开页签');
      await openMallItem(page, '店铺设置', attempt);
      await dismissError(page);
      await sleep(3000);
    }
  }
  R.results.registerRendered = regOk;
  await page.screenshot({ path: path.join(OUT, '_fix2_店铺设置_02注册设置.png') }).catch(() => {});
  R.results.registerPanel = await mainDump(page, 'register');
  log('  register text:', (R.results.registerPanel.text || '').replace(/\n/g, ' | ').slice(0, 500));

  // 下拉选项
  const combos = await page.evaluate(P.combos).catch(e => [{ err: e.message }]);
  R.results.registerCombos = combos;
  log('  combos:', JSON.stringify(combos));
  R.results.registerComboOptions = [];
  for (let i = 0; i < (combos || []).length; i++) {
    const o = await clickComboAndDumpOptions(page, i);
    o.combo = combos[i];
    R.results.registerComboOptions.push(o);
    log(`  combo#${i} ${combos[i].value} ->`, JSON.stringify(o.opts).slice(0, 500));
  }
  await page.screenshot({ path: path.join(OUT, '_fix2_店铺设置_03注册设置下拉.png') }).catch(() => {});

  // 库存显示方式「说明」按钮
  await page.evaluate(() => {
    const vis = e => e && e.offsetParent !== null;
    const el = [...document.querySelectorAll('.tab-text, .nav-tablistcontainer *, [class*=tab-text]')].filter(vis)
      .find(e => (e.innerText || '').trim() === '店铺参数');
    if (el) el.click();
  }).catch(() => {});
  await sleep(4000);
  const noteClick = await page.evaluate(() => {
    const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const cands = [...document.querySelectorAll('a, span, button, div, label')].filter(vis)
      .filter(e => (e.innerText || '').replace(/\s+/g, '').trim() === '说明');
    if (!cands.length) return { ok: false, n: 0 };
    cands[0].click();
    return { ok: true, n: cands.length, cls: String(cands[0].className).slice(0, 60) };
  }).catch(e => ({ ok: false, err: e.message }));
  await sleep(2500);
  R.results.stockDisplayNote = { click: noteClick, popup: await page.evaluate(P.popupOptions).catch(() => []), bodyHas: await page.evaluate(() => document.body.innerText.slice(0, 3000)).catch(() => '') };
  await page.screenshot({ path: path.join(OUT, '_fix2_店铺设置_04库存说明.png') }).catch(() => {});
  log('  说明按钮:', JSON.stringify(noteClick));
  await page.keyboard.press('Escape').catch(() => {});
  await sleep(500);

  // ============================================================
  // 追加目标 A：单位显示
  // ============================================================
  log('\n=== [A] 单位显示 ===');
  await openMallItem(page, '单位显示', 1);
  await dismissError(page);
  await sleep(6000);
  await ensureRendered(page, ['单位显示'], 3);
  await page.screenshot({ path: path.join(OUT, '_fix2_单位显示_01.png') }).catch(() => {});
  R.results.unitDisplay = {
    panel: await mainDump(page, 'unit'),
    combos: await page.evaluate(P.combos).catch(e => [{ err: e.message }])
  };
  R.results.unitDisplayComboOptions = [];
  for (let i = 0; i < (R.results.unitDisplay.combos || []).length; i++) {
    const o = await clickComboAndDumpOptions(page, i);
    o.combo = R.results.unitDisplay.combos[i];
    R.results.unitDisplayComboOptions.push(o);
    log(`  unit combo#${i} [${R.results.unitDisplay.combos[i].wrapText}] val=${R.results.unitDisplay.combos[i].value} ->`, JSON.stringify(o.opts).slice(0, 400));
    await page.screenshot({ path: path.join(OUT, `_fix2_单位显示_02下拉${i}.png`) }).catch(() => {});
  }
  // 列配置弹窗
  const gearA = await page.evaluate(() => {
    const vis = e => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
    const g = [...document.querySelectorAll('.dgrid-column-set .icon-shezhi2, .dgrid-header .icon-shezhi2, [class*=shezhi], [class*=column-set]')].filter(vis);
    if (!g.length) return { ok: false, n: 0 };
    g[0].click(); return { ok: true, n: g.length, cls: String(g[0].className).slice(0, 70) };
  }).catch(e => ({ ok: false, err: e.message }));
  await sleep(3000);
  R.results.unitDisplayColCfg = { gear: gearA, panel: await mainDump(page, 'colcfg') };
  await page.screenshot({ path: path.join(OUT, '_fix2_单位显示_03列配置.png') }).catch(() => {});
  log('  列配置齿轮:', JSON.stringify(gearA), 'text:', (R.results.unitDisplayColCfg.panel.text || '').replace(/\n/g, ' | ').slice(0, 500));
  await page.keyboard.press('Escape').catch(() => {});

  // ============================================================
  // 追加目标 B：商品上架 → 上架状态
  // ============================================================
  log('\n=== [B] 商品上架 ===');
  await openMallItem(page, '商品上架', 1);
  await dismissError(page);
  await sleep(6000);
  await ensureRendered(page, ['商品上架'], 3);
  await page.screenshot({ path: path.join(OUT, '_fix2_商品上架_01.png') }).catch(() => {});
  R.results.onShelf = { panel: await mainDump(page, 'onshelf'), combos: await page.evaluate(P.combos).catch(e => [{ err: e.message }]) };
  R.results.onShelfComboOptions = [];
  for (let i = 0; i < (R.results.onShelf.combos || []).length; i++) {
    const o = await clickComboAndDumpOptions(page, i);
    o.combo = R.results.onShelf.combos[i];
    R.results.onShelfComboOptions.push(o);
    log(`  shelf combo#${i} [${R.results.onShelf.combos[i].wrapText}] val=${R.results.onShelf.combos[i].value} ->`, JSON.stringify(o.opts).slice(0, 400));
    await page.screenshot({ path: path.join(OUT, `_fix2_商品上架_02下拉${i}.png`) }).catch(() => {});
  }

  R.netLog = netLog;
  R.log = LOG;
  fs.writeFileSync(path.join(OUT, '_probe_trade_fix2_raw.json'), JSON.stringify(R, null, 1), 'utf-8');
  log('\nWROTE _probe_trade_fix2_raw.json');
  await browser.close();
  console.log('DONE');
})().catch(e => { console.error('FATAL', e.stack || e.message); process.exit(1); });
