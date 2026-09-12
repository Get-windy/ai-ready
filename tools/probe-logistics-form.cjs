// 抓取 ql361 物流公司「新增」表单页（v3：稳健登录 + 会话复用 + 不关闭标签）
// 用法: NODE_PATH=... node tools/probe-logistics-form.cjs
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const OUT = path.join(__dirname, '..', 'tool-results', 'ql361', 'pages');
const SHOT = path.join(__dirname, '..', 'tool-results', 'ql361', 'logistics-shots');
const STATE = path.join(__dirname, '..', 'tool-results', 'ql361', 'state-logistics.json');
fs.mkdirSync(SHOT, { recursive: true });
const log = (...a) => console.log('[probe]', ...a);

async function doLogin(page) {
  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'domcontentloaded', timeout: 60000 }).catch(() => {});
  await page.waitForTimeout(6000);
  await page.fill('input[placeholder="请输入手机号/用户名"]', '15095664266').catch(() => {});
  await page.fill('#pwd', 'Ysh790921').catch(() => {});
  // 等安全验证组件初始化完成
  for (let i = 0; i < 25; i++) {
    const busy = await page.evaluate(() => (document.body.innerText || '').includes('安全验证组件初始化中')).catch(() => false);
    if (!busy) break;
    await page.waitForTimeout(1000);
  }
  await page.waitForTimeout(2000);
  await page.screenshot({ path: path.join(SHOT, '30-logon.png') }).catch(() => {});
  for (let attempt = 0; attempt < 4; attempt++) {
    await page.click('button.logon_button').catch(() => {});
    for (let i = 0; i < 20; i++) {
      await page.waitForTimeout(1000);
      if (!page.url().includes('/Account/Logon')) return true;
    }
    // 可能弹了安全验证，等一等再试
    await page.screenshot({ path: path.join(SHOT, `31-logon-retry${attempt}.png`) }).catch(() => {});
  }
  return false;
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({
    viewport: { width: 1680, height: 950 }, locale: 'zh-CN',
    storageState: fs.existsSync(STATE) ? STATE : undefined,
  });
  const page = await ctx.newPage();

  const ok = await doLogin(page);
  log('login ok:', ok, page.url());
  if (!ok) { await browser.close(); process.exit(2); }

  await page.waitForTimeout(10000);
  await page.keyboard.press('Escape').catch(() => {});
  await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    [...document.querySelectorAll('.dojoxDialogCloseIcon, .lk-dialog-close')].filter(vis).forEach(b => b.click());
  }).catch(() => {});
  await ctx.storageState({ path: STATE }).catch(() => {});
  log('state saved');

  // 打开 资料 → 物流公司
  await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true }).catch(() => {});
  await page.waitForTimeout(3000);
  const menuTxt = await page.evaluate(() => [...document.querySelectorAll('.popupmenu__nav-text')].map(e => (e.innerText || '').trim()).slice(0, 40));
  log('menu items:', JSON.stringify(menuTxt));
  await page.evaluate(() => {
    const el = [...document.querySelectorAll('.popupmenu__nav-text')]
      .find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '物流公司');
    if (el) el.click();
  }).catch(() => {});
  await page.waitForTimeout(8000);
  await page.screenshot({ path: path.join(SHOT, '20-list-check.png') }).catch(() => {});

  const clicked = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const b = [...document.querySelectorAll('button, a, span')].filter(vis)
      .find(x => (x.innerText || '').trim() === '新增' && x.offsetWidth > 0);
    if (b) { b.click(); return true; }
    return false;
  }).catch(() => false);
  log('clicked add:', clicked);
  await page.waitForTimeout(10000);
  await page.screenshot({ path: path.join(SHOT, '21-form-viewport.png') }).catch(() => {});
  await page.screenshot({ path: path.join(SHOT, '22-form-fullpage.png'), fullPage: true }).catch(() => {});

  const tabsAfter = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    return [...document.querySelectorAll('.dijitTab')].filter(vis).map(t => (t.innerText || '').trim());
  }).catch(() => []);
  log('tabs after add:', JSON.stringify(tabsAfter));

  const data = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const clean = s => (s || '').replace(/[ \t]+/g, ' ').replace(/\n{2,}/g, '\n').trim();
    const pane = [...document.querySelectorAll('.dijitTabPane')].filter(vis)
      .filter(p => !(p.innerText || '').includes('本月收入') && (p.innerText || '').includes('物流'))
      .sort((a, b) => (b.innerText || '').length - (a.innerText || '').length)[0];
    const scope = pane || document.body;
    const inputs = [...scope.querySelectorAll('input,textarea,select')].filter(vis).map(i => {
      const row = i.closest('tr, .form-item, .field, .lk-form-item, div');
      return { tag: i.tagName.toLowerCase(), type: i.type || '', id: i.id || '', name: i.name || '',
        ph: i.placeholder || '', val: i.value || '', rowText: row ? clean(row.innerText).slice(0, 60) : '' };
    });
    const labels = [...scope.querySelectorAll('label')].filter(vis).map(l => clean(l.innerText)).filter(Boolean);
    const chips = [...scope.querySelectorAll('legend, .lk-title, [class*=title]')].filter(vis).map(l => clean(l.innerText)).filter(t => t && t.length < 20);
    const buttons = [...scope.querySelectorAll('button,a')].filter(vis).map(b => clean(b.innerText)).filter(t => t && t.length < 12);
    return { text: clean(scope.innerText).slice(0, 10000), inputs, labels, chips, buttons: [...new Set(buttons)] };
  }).catch(e => ({ err: String(e) }));
  fs.writeFileSync(path.join(OUT, 'ziliao__logistics__form.json'), JSON.stringify(data, null, 2));
  log('LABELS:', JSON.stringify(data.labels));
  log('CHIPS:', JSON.stringify(data.chips));
  log('BUTTONS:', JSON.stringify(data.buttons));
  log('INPUTS:', JSON.stringify((data.inputs || []).map(i => `${i.rowText} >> [${i.type}] ph=${i.ph} val=${i.val}`)));
  log('TEXT:\n', (data.text || '').slice(0, 5000));

  await browser.close();
  log('DONE');
})().catch(e => { console.error('FATAL', e); process.exit(1); });
