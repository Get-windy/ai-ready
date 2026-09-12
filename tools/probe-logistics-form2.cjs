// 抓取 ql361 物流公司「新增」表单页（v4：物理坐标点击 + 全量 dump）
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const OUT = path.join(__dirname, '..', 'tool-results', 'ql361', 'pages');
const SHOT = path.join(__dirname, '..', 'tool-results', 'ql361', 'logistics-shots');
const STATE = path.join(__dirname, '..', 'tool-results', 'ql361', 'state-logistics.json');
fs.mkdirSync(SHOT, { recursive: true });
const log = (...a) => console.log('[probe]', ...a);

(async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({
    viewport: { width: 1680, height: 950 }, locale: 'zh-CN',
    storageState: fs.existsSync(STATE) ? STATE : undefined,
  });
  const page = await ctx.newPage();

  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'domcontentloaded', timeout: 60000 }).catch(() => {});
  await page.waitForTimeout(5000);
  if (page.url().includes('/Account/Logon')) {
    await page.fill('input[placeholder="请输入手机号/用户名"]', '15095664266').catch(() => {});
    await page.fill('#pwd', 'Ysh790921').catch(() => {});
    for (let i = 0; i < 25; i++) {
      const busy = await page.evaluate(() => (document.body.innerText || '').includes('安全验证组件初始化中')).catch(() => false);
      if (!busy) break;
      await page.waitForTimeout(1000);
    }
    for (let a = 0; a < 4; a++) {
      await page.click('button.logon_button').catch(() => {});
      for (let i = 0; i < 20; i++) { await page.waitForTimeout(1000); if (!page.url().includes('/Account/Logon')) break; }
      if (!page.url().includes('/Account/Logon')) break;
    }
  }
  log('url:', page.url());
  await page.waitForTimeout(10000);
  await page.keyboard.press('Escape').catch(() => {});
  await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    [...document.querySelectorAll('.dojoxDialogCloseIcon, .lk-dialog-close')].filter(vis).forEach(b => b.click());
  }).catch(() => {});
  await ctx.storageState({ path: STATE }).catch(() => {});

  // 资料 → 物流公司
  await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true }).catch(() => {});
  await page.waitForTimeout(3000);
  await page.evaluate(() => {
    const el = [...document.querySelectorAll('.popupmenu__nav-text')]
      .find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '物流公司');
    if (el) el.click();
  }).catch(() => {});
  await page.waitForTimeout(8000);
  await page.screenshot({ path: path.join(SHOT, '40-list.png') }).catch(() => {});

  // dump 新增按钮
  const btnInfo = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const cands = [...document.querySelectorAll('button,a,span,div')].filter(vis)
      .filter(x => (x.innerText || '').trim() === '新增');
    return cands.slice(0, 6).map(x => {
      const r = x.getBoundingClientRect();
      return { tag: x.tagName, cls: String(x.className).slice(0, 80), id: x.id,
        x: Math.round(r.x + r.width / 2), y: Math.round(r.y + r.height / 2), w: Math.round(r.width), h: Math.round(r.height),
        html: x.outerHTML.slice(0, 300) };
    });
  }).catch(e => ({ err: String(e) }));
  log('ADD BTN:', JSON.stringify(btnInfo, null, 1));
  fs.writeFileSync(path.join(OUT, 'ziliao__logistics__addbtn.json'), JSON.stringify(btnInfo, null, 2));

  const target = Array.isArray(btnInfo) ? btnInfo[0] : null;
  if (target && target.w > 0) {
    await page.mouse.click(target.x, target.y);
    log('physical click at', target.x, target.y);
  }
  await page.waitForTimeout(12000);
  await page.screenshot({ path: path.join(SHOT, '41-after-add.png') }).catch(() => {});
  await page.screenshot({ path: path.join(SHOT, '42-after-add-full.png'), fullPage: true }).catch(() => {});

  const tabsAfter = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    return [...document.querySelectorAll('.dijitTab')].filter(vis).map(t => (t.innerText || '').trim());
  }).catch(() => []);
  log('tabs:', JSON.stringify(tabsAfter));

  const data = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const clean = s => (s || '').replace(/[ \t]+/g, ' ').replace(/\n{2,}/g, '\n').trim();
    // 收集所有可见 pane 的文本，取"非列表页"的最大者
    const panes = [...document.querySelectorAll('.dijitTabPane,[role=dialog],[class*=Dialog]')].filter(vis)
      .map(p => ({ cls: String(p.className).slice(0, 60), text: clean(p.innerText) }))
      .filter(p => p.text.length > 100);
    panes.sort((a, b) => b.text.length - a.text.length);
    return { panes: panes.slice(0, 5).map(p => ({ cls: p.cls, len: p.text.length, head: p.text.slice(0, 400) })) };
  }).catch(e => ({ err: String(e) }));
  log('PANES:', JSON.stringify(data, null, 1));

  // 尝试定位表单输入控件
  const formDump = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const clean = s => (s || '').replace(/[ \t]+/g, ' ').trim();
    const inputs = [...document.querySelectorAll('input,textarea,select')].filter(vis).map(i => {
      const box = i.closest('tr, .lk-form-item, .form-item, div');
      return { type: i.type || i.tagName.toLowerCase(), id: i.id, name: i.name, ph: i.placeholder || '', val: i.value || '', ctx: box ? clean(box.innerText).slice(0, 40) : '' };
    });
    const labels = [...document.querySelectorAll('label')].filter(vis).map(l => clean(l.innerText)).filter(Boolean);
    return { inputs, labels };
  }).catch(e => ({ err: String(e) }));
  fs.writeFileSync(path.join(OUT, 'ziliao__logistics__form.json'), JSON.stringify(formDump, null, 2));
  log('LABELS:', JSON.stringify(formDump.labels));
  log('INPUTS:', JSON.stringify((formDump.inputs || []).map(i => `${i.ctx} >> [${i.type}] ph=${i.ph} val=${i.val}`)));

  await browser.close();
  log('DONE');
})().catch(e => { console.error('FATAL', e); process.exit(1); });
