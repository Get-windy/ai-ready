// 针对性补抓：商城 → 商城设置 → 运费设置（物流子项） / 店铺设置（注册设置子项）
// 抓页面主区表单 + 所有 iframe + 网络 JSON 响应
// 用法: node _probe_trade_fix.js [outDir]
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const OUT = process.argv[2] || '../tool-results/ql361/pages';
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

// ---- 深度 DOM dump（自包含，可注入任意 frame） ----
const DUMP_FN = () => {
  const vis = (e) => !!(e && (e.offsetParent !== null || (e.getClientRects && e.getClientRects().length)));
  const norm = (s) => (s || '').replace(/\s+/g, ' ').trim();

  function labelOf(el) {
    let t = '';
    if (el.id) {
      try {
        const l = document.querySelector('label[for="' + CSS.escape(el.id) + '"]');
        if (l) t = norm(l.innerText);
      } catch (e) {}
    }
    if (!t) { const p = el.closest('label'); if (p) t = norm(p.innerText); }
    if (!t) {
      let prev = el.previousElementSibling, hop = 0;
      while (prev && hop < 4) {
        const s = norm(prev.innerText);
        if (s && s.length < 60) { t = s; break; }
        prev = prev.previousElementSibling; hop++;
      }
    }
    if (!t) {
      const par = el.parentElement;
      if (par) {
        const clone = par.cloneNode(true);
        clone.querySelectorAll('input,select,textarea,button').forEach(n => n.remove());
        const s = norm(clone.innerText);
        if (s && s.length < 80) t = s;
      }
    }
    return t.slice(0, 80);
  }

  function rowText(el) {
    const row = el.closest('tr, .lk-form-item, .form-item, .field, [class*=formitem], [class*=form-item], [class*=field-item], [class*=row]');
    return row ? norm(row.innerText).slice(0, 160) : '';
  }

  const controls = [];
  const sel = 'input, select, textarea, button, [role=combobox], [class*=combobox], [class*=switch], [class*=checkbox]';
  for (const el of document.querySelectorAll(sel)) {
    if (!vis(el)) continue;
    const tag = el.tagName.toLowerCase();
    const cls = String(el.className || '').slice(0, 70);
    const rec = {
      tag,
      type: el.type || '',
      cls,
      id: el.id || '',
      name: el.getAttribute('name') || '',
      label: labelOf(el),
      row: rowText(el),
      placeholder: el.getAttribute('placeholder') || '',
      value: (tag === 'select' || tag === 'input' || tag === 'textarea') ? String(el.value || '').slice(0, 80) : '',
      checked: (typeof el.checked === 'boolean') ? el.checked : undefined,
      disabled: !!el.disabled,
      title: el.getAttribute('title') || '',
      ariaLabel: el.getAttribute('aria-label') || ''
    };
    if (tag === 'select') rec.options = [...el.options].map(o => norm(o.text));
    if (tag === 'button' || el.getAttribute('role') === 'button') rec.btnText = norm(el.innerText).slice(0, 40);
    // 必填标记
    const lm = rec.label + ' ' + rec.row;
    rec.star = /\*|＊/.test(lm);
    controls.push(rec);
  }

  // 表格
  const tables = [...document.querySelectorAll('table')].filter(vis).map(t => {
    const head = [...t.querySelectorAll('thead th, thead td, tr:first-child th')].map(e => norm(e.innerText)).filter(Boolean);
    const rows = [...t.querySelectorAll('tbody tr, tr')].slice(0, 6).map(tr =>
      [...tr.querySelectorAll('td,th')].map(td => norm(td.innerText).slice(0, 60)));
    return { cls: String(t.className).slice(0, 60), head, rows };
  }).filter(t => t.head.length || t.rows.length);

  // 文本块（label 类元素）
  const labelEls = [...document.querySelectorAll('label, [class*=label], [class*=title], [class*=caption], legend')]
    .filter(vis).map(e => ({ cls: String(e.className).slice(0, 60), t: norm(e.innerText).slice(0, 80) }))
    .filter(e => e.t && e.t.length < 80);

  // 主区文本（去噪）
  const mainCand = [...document.querySelectorAll('[class*=content], [class*=main], [class*=panel], [class*=form], body')].filter(vis);
  mainCand.sort((a, b) => (b.innerText || '').length - (a.innerText || '').length);
  const mainText = mainCand[0] ? norm(mainCand[0].innerText).slice(0, 6000) : '';

  // iframe 列表
  const frames = [...document.querySelectorAll('iframe')].map(f => ({ src: f.getAttribute('src') || '', vis: vis(f), w: f.offsetWidth, h: f.offsetHeight }));

  return { url: location.href, title: document.title, controls, tables, labelEls: labelEls.slice(0, 300), mainText, frames };
};

// ---- 全局深度 dump：主文档 + 所有 iframe ----
async function dumpAll(page) {
  const out = [];
  for (const fr of page.frames()) {
    let d;
    try { d = await fr.evaluate(DUMP_FN); } catch (e) { d = { url: fr.url(), err: e.message.slice(0, 120) }; }
    out.push(d);
  }
  return out;
}

async function dismissError(page) {
  // 关掉「网络资源请求失败」弹窗：点「我知道了」
  for (const fr of page.frames()) {
    try {
      await fr.evaluate(() => {
        const vis = e => e && e.offsetParent !== null;
        const btns = [...document.querySelectorAll('button, a, div[class*=btn], span[class*=btn]')].filter(vis);
        const ok = btns.find(b => /我知道了|确定|关闭/.test((b.innerText || '').trim()) && (b.innerText || '').trim().length < 12);
        if (ok) ok.click();
      });
    } catch (e) {}
  }
  await sleep(800);
}

async function clickText(page, text, opts = {}) {
  const done = { clicked: false, where: '' };
  for (const fr of page.frames()) {
    try {
      const r = await fr.evaluate(({ text, exact }) => {
        const vis = e => e && e.offsetParent !== null;
        const cands = [...document.querySelectorAll('a, li, span, div, td, button, label')].filter(vis)
          .filter(e => {
            const t = (e.innerText || '').replace(/\s+/g, '').trim();
            if (!t || t.length > 30) return false;
            return exact ? t === text : t.includes(text);
          });
        // 取最深的（文字最少的）一个
        cands.sort((a, b) => (a.innerText || '').length - (b.innerText || '').length);
        const el = cands[0];
        if (el) { el.click(); return { ok: true, t: (el.innerText || '').trim().slice(0, 30), cls: String(el.className).slice(0, 60) }; }
        return { ok: false };
      }, { text, exact: !!opts.exact });
      if (r && r.ok) { done.clicked = true; done.where = fr.url() + ' | ' + r.cls + ' | ' + r.t; break; }
    } catch (e) {}
  }
  return done;
}

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 }, locale: 'zh-CN' });
  const page = await ctx.newPage();

  const netLog = [];
  page.on('response', async (res) => {
    try {
      const u = res.url();
      const ct = String(res.headers()['content-type'] || '');
      if (/json/i.test(ct) && !/\.(png|jpg|gif|css|woff)/i.test(u)) {
        let body = '';
        try { body = (await res.text()).slice(0, 40000); } catch (e) { body = '<unreadable>'; }
        netLog.push({ kind: 'json', status: res.status(), url: u.slice(0, 300), body });
      }
    } catch (e) {}
  });
  page.on('requestfailed', (req) => netLog.push({ kind: 'failed', url: req.url().slice(0, 300), err: req.failure() ? req.failure().errorText : '?' }));
  page.on('pageerror', (e) => netLog.push({ kind: 'pageerror', msg: String(e.message).slice(0, 300) }));

  const report = { ts: new Date().toISOString(), entry: {}, pages: {}, netLog: [] };

  // ===== 登录 =====
  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'networkidle', timeout: 45000 }).catch(() => {});
  await page.fill('input[placeholder="请输入手机号/用户名"]', '15095664266').catch(() => {});
  await page.fill('#pwd', 'Ysh790921').catch(() => {});
  await page.click('button.logon_button').catch(() => {});
  for (let i = 0; i < 30; i++) { await sleep(1000); if (!page.url().includes('/Account/Logon')) break; }
  await sleep(8000);
  report.entry.afterLoginUrl = page.url();
  report.entry.domain = new URL(page.url()).host;
  await page.keyboard.press('Escape').catch(() => {});
  await dismissError(page);
  console.log('LOGIN OK ->', report.entry.domain);

  // ===== 打开菜单项 =====
  async function openMenuItem(item) {
    await page.locator('a.menu-0-item', { hasText: '商城' }).first().click({ force: true }).catch(() => {});
    await sleep(1800);
    const r = await page.evaluate((item) => {
      const vis = e => e && e.offsetParent !== null;
      const els = [...document.querySelectorAll('.popupmenu__nav-text')].filter(vis);
      const el = els.find(e => (e.innerText || '').replace(/\s+/g, '').trim() === item);
      if (el) { el.click(); return true; }
      return false;
    }, item).catch(() => false);
    await sleep(9000);
    return r;
  }

  async function activeTab() {
    return page.evaluate(() => {
      const vis = e => e && e.offsetParent !== null;
      const tabs = [...document.querySelectorAll('.dijitTab')].filter(vis);
      const act = tabs.find(t => t.classList.contains('dijitTabSelected'));
      return act ? (act.innerText || '').trim().slice(0, 30) : 'none';
    }).catch(() => 'err');
  }

  // ============ 目标 1：运费设置 ============
  console.log('\n=== 运费设置 ===');
  report.entry.freightMenuClicked = await openMenuItem('运费设置');
  report.pages.freight = { activeTab: await activeTab() };
  console.log('  activeTab:', report.pages.freight.activeTab);
  await page.screenshot({ path: path.join(OUT, '_fix_运费设置_01进入.png'), fullPage: false }).catch(() => {});
  await dismissError(page);
  await sleep(2000);

  // 关掉其它标签，只留当前
  await page.evaluate((item) => {
    const vis = e => e && e.offsetParent !== null;
    for (const t of [...document.querySelectorAll('.dijitTab')].filter(vis)) {
      const txt = (t.innerText || '').trim();
      if (t.classList.contains('dijitTabSelected') || txt.includes(item)) continue;
      const c = t.querySelector('.dijitTabCloseButton');
      if (c) c.click();
    }
  }, '运费设置').catch(() => {});
  await sleep(1500);

  report.pages.freight.afterDismiss = await dumpAll(page);
  // 左子项导航候选
  report.pages.freight.subNav = await page.evaluate(() => {
    const vis = e => e && e.offsetParent !== null;
    const cands = [...document.querySelectorAll('[class*=menu] li, [class*=nav] li, [class*=sidebar] *, [class*=left] li, [class*=submenu] *, [class*=tab]')]
      .filter(vis).map(e => ({ cls: String(e.className).slice(0, 70), t: (e.innerText || '').replace(/\s+/g, ' ').trim().slice(0, 40) }))
      .filter(e => e.t && e.t.length < 40);
    return cands.slice(0, 120);
  }).catch(e => ({ err: e.message }));

  // 尝试点击「物流」子项（可能已在默认激活态）
  const lw = await clickText(page, '物流', { exact: true });
  console.log('  click 物流:', JSON.stringify(lw));
  await sleep(6000);
  await dismissError(page);
  await sleep(1500);
  report.pages.freight.logistics = { click: lw, dump: await dumpAll(page) };
  await page.screenshot({ path: path.join(OUT, '_fix_运费设置_02物流子项.png') }).catch(() => {});

  // 再试开启「启用物流」开关后是否出现数据区
  const toggle = await page.evaluate(() => {
    const vis = e => e && e.offsetParent !== null;
    const labels = [...document.querySelectorAll('label, span, div')].filter(vis)
      .filter(e => (e.innerText || '').replace(/\s+/g, '').trim() === '启用物流');
    const out = [];
    for (const l of labels.slice(0, 3)) {
      const host = l.closest('tr, [class*=item], [class*=field], [class*=form], div');
      if (host) {
        const sw = host.querySelector('input[type=checkbox], [class*=switch], [class*=checkbox]');
        if (sw) { sw.click(); out.push('clicked'); }
      }
    }
    return out;
  }).catch(e => ['ERR ' + e.message]);
  console.log('  启用物流 toggle:', JSON.stringify(toggle));
  await sleep(6000);
  report.pages.freight.afterEnableLogistics = { toggle, dump: await dumpAll(page) };
  await page.screenshot({ path: path.join(OUT, '_fix_运费设置_03启用物流后.png') }).catch(() => {});

  // 子项「到店自提」（对照组）
  const pd = await clickText(page, '到店自提', { exact: true });
  await sleep(5000);
  report.pages.freight.pickup = { click: pd, dump: await dumpAll(page) };
  await page.screenshot({ path: path.join(OUT, '_fix_运费设置_04到店自提.png') }).catch(() => {});

  // 回到物流
  await clickText(page, '物流', { exact: true });
  await sleep(4000);

  // ============ 目标 2：店铺设置 ============
  console.log('\n=== 店铺设置 ===');
  report.entry.shopMenuClicked = await openMenuItem('店铺设置');
  report.pages.shop = { activeTab: await activeTab() };
  console.log('  activeTab:', report.pages.shop.activeTab);
  await page.screenshot({ path: path.join(OUT, '_fix_店铺设置_01进入.png') }).catch(() => {});
  await dismissError(page);
  await sleep(2000);
  await page.evaluate((item) => {
    const vis = e => e && e.offsetParent !== null;
    for (const t of [...document.querySelectorAll('.dijitTab')].filter(vis)) {
      const txt = (t.innerText || '').trim();
      if (t.classList.contains('dijitTabSelected') || txt.includes(item)) continue;
      const c = t.querySelector('.dijitTabCloseButton');
      if (c) c.click();
    }
  }, '店铺设置').catch(() => {});
  await sleep(1500);

  const reg = await clickText(page, '注册设置', { exact: true });
  console.log('  click 注册设置:', JSON.stringify(reg));
  await sleep(7000);
  await dismissError(page);
  await sleep(2000);
  report.pages.shop.register = { click: reg, dump: await dumpAll(page) };
  await page.screenshot({ path: path.join(OUT, '_fix_店铺设置_02注册设置.png') }).catch(() => {});

  // 再点一次（有些实现首次点击仅渲染骨架）
  await clickText(page, '注册设置', { exact: true });
  await sleep(6000);
  await dismissError(page);
  await sleep(2000);
  report.pages.shop.register2 = { dump: await dumpAll(page) };
  await page.screenshot({ path: path.join(OUT, '_fix_店铺设置_03注册设置二次.png') }).catch(() => {});

  // 刷新页面后再进（另一种尝试）
  await page.reload({ waitUntil: 'domcontentloaded' }).catch(() => {});
  await sleep(10000);
  await dismissError(page);
  await page.locator('a.menu-0-item', { hasText: '商城' }).first().click({ force: true }).catch(() => {});
  await sleep(1800);
  await page.evaluate(() => {
    const vis = e => e && e.offsetParent !== null;
    const el = [...document.querySelectorAll('.popupmenu__nav-text')].filter(vis).find(e => (e.innerText || '').replace(/\s+/g, '').trim() === '店铺设置');
    if (el) el.click();
  }).catch(() => {});
  await sleep(10000);
  await dismissError(page);
  const r2 = await clickText(page, '注册设置', { exact: true });
  await sleep(8000);
  await dismissError(page);
  await sleep(2000);
  report.pages.shop.register3_reload = { click: r2, dump: await dumpAll(page) };
  await page.screenshot({ path: path.join(OUT, '_fix_店铺设置_04注册设置刷新后.png') }).catch(() => {});

  report.netLog = netLog;
  fs.writeFileSync(path.join(OUT, '_probe_trade_fix_raw.json'), JSON.stringify(report, null, 1), 'utf-8');
  console.log('\nWROTE _probe_trade_fix_raw.json  netLog=', netLog.length);
  await browser.close();
  console.log('DONE');
})().catch(e => { console.error('FATAL', e.stack || e.message); process.exit(1); });
