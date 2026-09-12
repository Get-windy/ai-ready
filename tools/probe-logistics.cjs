// 抓取 ql361 物流公司页面：列表结构 + 列配置弹窗 + 新增表单页
// 用法: node tools/probe-logistics.cjs
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const OUT = path.join(__dirname, '..', 'tool-results', 'ql361', 'pages');
const SHOT = path.join(__dirname, '..', 'tool-results', 'ql361', 'logistics-shots');
fs.mkdirSync(SHOT, { recursive: true });

const log = (...a) => console.log('[probe]', ...a);

(async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' });
  const page = await ctx.newPage();

  // ── 登录 ──
  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'domcontentloaded', timeout: 45000 }).catch(() => {});
  await page.waitForTimeout(3000);
  await page.fill('input[placeholder="请输入手机号/用户名"]', '15095664266').catch(() => {});
  await page.fill('#pwd', 'Ysh790921').catch(() => {});
  await page.click('button.logon_button').catch(() => {});
  for (let i = 0; i < 30; i++) {
    await page.waitForTimeout(1000);
    if (!page.url().includes('/Account/Logon')) break;
  }
  await page.waitForTimeout(9000);
  await page.keyboard.press('Escape').catch(() => {});
  await page.evaluate(() => {
    document.querySelectorAll('.lk-dialog-close,[aria-label=Close],[title=关闭],.dojoxDialogCloseIcon').forEach(b => b.click());
  }).catch(() => {});
  log('LOGGED_IN', page.url());

  // ── 关闭其它已打开标签，只留当前 ──
  await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    [...document.querySelectorAll('.dijitTabCloseButton, .dijitTabCloseButtonSingleton')].filter(vis).forEach(b => b.click());
  }).catch(() => {});
  await page.waitForTimeout(1500);

  // ── 打开 资料 → 物流公司 ──
  await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true }).catch(() => {});
  await page.waitForTimeout(2000);
  await page.screenshot({ path: path.join(SHOT, '00-menu-ziliao.png') }).catch(() => {});
  await page.evaluate(() => {
    const items = [...document.querySelectorAll('.popupmenu__nav-text')];
    const el = items.find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '物流公司');
    if (el) el.click();
  }).catch(() => {});
  await page.waitForTimeout(7000);
  await page.screenshot({ path: path.join(SHOT, '01-list.png') }).catch(() => {});

  const activeTab = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const t = [...document.querySelectorAll('.dijitTab')].filter(vis).find(x => x.classList.contains('dijitTabSelected'));
    return t ? (t.innerText || '').trim() : 'none';
  }).catch(() => 'err');
  log('ACTIVE_TAB:', activeTab);

  // ── 列表页可见结构 ──
  const listInfo = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const panel = [...document.querySelectorAll('.dijitTabPane')].filter(vis)
      .find(p => (p.innerText || '').includes('物流公司编号') || (p.innerText || '').includes('物流公司名称'));
    const scope = panel || document.body;
    const headers = [...scope.querySelectorAll('th')].filter(vis).map(t => (t.innerText || '').trim().replace(/\s+/g, ' ')).filter(Boolean);
    const gearCount = [...scope.querySelectorAll('.icon-shezhi2')].filter(vis).length;
    const titleGear = [...document.querySelectorAll('[class*=title] [class*=shezhi]')].filter(vis).length;
    return { headers, text: (scope.innerText || '').slice(0, 3000), gearCount, titleGear };
  }).catch(e => ({ err: String(e) }));
  fs.writeFileSync(path.join(OUT, 'ziliao__logistics__list.json'), JSON.stringify(listInfo, null, 2));
  log('LIST headers:', JSON.stringify(listInfo.headers));

  // ── 列配置弹窗 ──
  const colGear = page.locator('.dijitTabPane:visible .icon-shezhi2, .dijitTabPane:visible [class*=shezhi]').first();
  const colCnt = await page.locator('.dijitTabPane:visible .icon-shezhi2').count();
  log('column gear count:', colCnt);
  if (colCnt > 0) {
    await colGear.click({ force: true }).catch(() => {});
    await page.waitForTimeout(2500);
    await page.screenshot({ path: path.join(SHOT, '02-column-config.png') }).catch(() => {});
    const colCfg = await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null;
      const dlgs = [...document.querySelectorAll('[role=dialog],[class*=Dialog],[class*=dialog]')].filter(vis)
        .map(d => ({ text: d.innerText || '', cls: String(d.className).slice(0, 50) }))
        .filter(d => d.text.includes('配置') || d.text.includes('列名'));
      dlgs.sort((a, b) => b.text.length - a.text.length);
      const win = dlgs[0];
      if (!win) return { found: false };
      const rows = [...document.querySelectorAll('[role=dialog] tr, [class*=Dialog] tr, [class*=dialog] tr')].filter(vis).map(tr => {
        const tds = [...tr.querySelectorAll('td')].filter(vis).map(td => (td.innerText || '').trim());
        const cb = tr.querySelector('input[type=checkbox]');
        return { tds, checked: cb ? cb.checked : null };
      }).filter(r => r.tds.length >= 2);
      return { found: true, cls: win.cls, text: win.text, rows };
    }).catch(e => ({ err: String(e) }));
    fs.writeFileSync(path.join(OUT, 'ziliao__logistics__colcfg.json'), JSON.stringify(colCfg, null, 2));
    log('COLUMNS:', JSON.stringify((colCfg.rows || []).map(r => r.tds.join('|') + (r.checked === null ? '' : r.checked ? '[x]' : '[ ]'))));
    await page.keyboard.press('Escape').catch(() => {});
    await page.waitForTimeout(1000);
  }

  // ── 新增表单页 ──
  const addBtn = page.locator('.dijitTabPane:visible button:has-text("新增"), .dijitTabPane:visible a:has-text("新增")').first();
  await addBtn.click({ force: true }).catch(() => {});
  await page.waitForTimeout(6000);
  await page.screenshot({ path: path.join(SHOT, '03-form.png') }).catch(() => {});
  const formInfo = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const dlgs = [...document.querySelectorAll('[role=dialog],[class*=Dialog],[class*=dialog],[class*=window]')].filter(vis)
      .map((d, i) => ({ i, text: (d.innerText || '').slice(0, 5000), cls: String(d.className).slice(0, 60), w: d.offsetWidth, h: d.offsetHeight }))
      .filter(d => d.text.length > 60);
    dlgs.sort((a, b) => b.text.length - a.text.length);
    const labels = [...document.querySelectorAll('[role=dialog] label, [class*=Dialog] label, [class*=dialog] label')]
      .filter(vis).map(l => (l.innerText || '').trim()).filter(Boolean);
    const inputs = [...document.querySelectorAll('[role=dialog] input, [class*=Dialog] input, [class*=dialog] input')]
      .filter(vis).map(inp => ({ name: inp.name || inp.id || '', ph: inp.placeholder || '', val: inp.value || '', type: inp.type || '' }));
    const tabs = [...document.querySelectorAll('[role=tab]')].filter(vis).map(t => (t.innerText || '').trim()).filter(t => t && t.length < 20);
    return { top: dlgs[0] || null, allTexts: dlgs.map(d => ({ cls: d.cls, w: d.w, h: d.h, len: d.text.length })), labels, inputs, tabs };
  }).catch(e => ({ err: String(e) }));
  fs.writeFileSync(path.join(OUT, 'ziliao__logistics__form.json'), JSON.stringify(formInfo, null, 2));
  log('FORM labels:', JSON.stringify(formInfo.labels));
  log('FORM top text:\n', (formInfo.top && formInfo.top.text || '').slice(0, 2500));

  await browser.close();
  log('DONE');
})().catch(e => { console.error('FATAL', e); process.exit(1); });
