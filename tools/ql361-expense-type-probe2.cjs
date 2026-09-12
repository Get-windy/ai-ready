// 费用类型 二次探针：网络抓包（判定数据源）+ 新增费用弹窗 + 与会计科目页对比
// 用法: cd frontend && node ../tools/ql361-expense-type-probe2.cjs
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright');
const fs = require('fs');
const path = require('path');

const OUT = path.resolve(__dirname, '../tool-results/ql361/expense-type');
fs.mkdirSync(OUT, { recursive: true });

async function login(page) {
  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'networkidle', timeout: 45000 }).catch(() => {});
  await page.fill('input[placeholder="请输入手机号/用户名"]', '15095664266').catch(() => {});
  await page.fill('#pwd', 'Ysh790921').catch(() => {});
  await page.click('button.logon_button').catch(() => {});
  for (let i = 0; i < 25; i++) {
    await page.waitForTimeout(1000);
    if (!page.url().includes('/Account/Logon')) break;
  }
  await page.waitForTimeout(8000);
  await page.keyboard.press('Escape').catch(() => {});
  await page.evaluate(() => document.querySelectorAll('.dojoxDialogCloseIcon,.lk-dialog-close,[title=关闭]').forEach(b => b.click())).catch(() => {});
  await page.waitForTimeout(1500);
}

async function openMdPage(page, name) {
  await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true });
  await page.waitForTimeout(2000);
  const grp = page.locator('.popupmenu__nav-text', { hasText: '财务账户' }).first();
  if (await grp.count()) { await grp.hover().catch(() => {}); await page.waitForTimeout(1200); }
  const ok = await page.evaluate((n) => {
    const el = [...document.querySelectorAll('.popupmenu__nav-text')].find(e => (e.innerText || '').trim().replace(/\s+/g, '') === n);
    if (el) { el.click(); return true; }
    return false;
  }, name);
  await page.waitForTimeout(6000);
  await page.evaluate(() => {
    [...document.querySelectorAll('.dijitTab')].forEach(t => {
      if (!/dijitTabChecked|dijitTabActive/.test(t.className)) {
        const c = t.querySelector('.dijitTabCloseButton, [class*=Close]'); if (c) c.click();
      }
    });
  }).catch(() => {});
  await page.waitForTimeout(2000);
  await page.keyboard.press('Escape').catch(() => {});
  return ok;
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' });
  const page = await ctx.newPage();

  const net = [];
  page.on('request', req => {
    const u = req.url();
    if (/\.(ashx|json|aspx|do)|Get|List|Grid|Data/i.test(u) && !/\.(js|css|png|jpg|gif|svg|woff|ico)/i.test(u)) {
      let body = '';
      try { body = req.postData() || ''; } catch (e) { /* noop */ }
      net.push({ m: req.method(), url: u, body: body.slice(0, 1500) });
    }
  });

  await login(page);
  console.log('LOGGED_IN', page.url());

  // ── 费用类型 ──
  net.length = 0;
  console.log('OPEN 费用类型:', await openMdPage(page, '费用类型'));
  await page.waitForTimeout(2000);
  fs.writeFileSync(path.join(OUT, '10-net-expense-type.json'), JSON.stringify(net, null, 1));
  console.log('NET 费用类型:', JSON.stringify(net.slice(-25)));

  // 新增费用弹窗
  const addClick = await page.evaluate(() => {
    const btns = [...document.querySelectorAll('button, [class*=btn], a[class*=button]')]
      .filter(e => e.offsetParent !== null && /新增费用/.test(e.innerText || ''));
    if (btns.length) { btns[0].click(); return true; }
    return false;
  });
  console.log('CLICK 新增费用:', addClick);
  await page.waitForTimeout(3500);
  await page.screenshot({ path: path.join(OUT, '10-add-dialog.png') });
  const addInfo = await page.evaluate(() => {
    const txt = el => (el ? (el.innerText || '').trim() : '');
    const vis = el => el && el.offsetParent !== null;
    const titles = [...document.querySelectorAll('.dijitDialogTitleBar, .dijitDialogTitle')].filter(vis).map(txt);
    const dlg = [...document.querySelectorAll('.dijitDialog')].filter(vis)
      .map(d => ({ cls: String(d.className).slice(0, 60), text: txt(d).slice(0, 2000) }))
      .sort((a, b) => b.text.length - a.text.length)[0];
    const fields = [...document.querySelectorAll('.dijitDialog input, .dijitDialog select, .dijitDialog textarea')].filter(vis)
      .map(el => {
        const row = el.closest('tr, [class*=item], .dijitContentPane > div');
        return { label: row ? txt(row).split('\n')[0].slice(0, 40) : '', type: el.type, val: el.value, ph: el.getAttribute('placeholder') || '' };
      });
    const labels = [...document.querySelectorAll('.dijitDialog label, .dijitDialog .field-label, .dijitDialog td')].filter(vis).map(txt).filter(t => t && t.length < 30).slice(0, 40);
    return { titles, dlg, fields, labels };
  });
  fs.writeFileSync(path.join(OUT, '10-add-dialog.json'), JSON.stringify(addInfo, null, 1));
  console.log('ADD TITLES:', JSON.stringify(addInfo.titles));
  console.log('ADD DIALOG TEXT:', JSON.stringify(addInfo.dlg));
  console.log('ADD FIELDS:', JSON.stringify(addInfo.fields));
  console.log('ADD LABELS:', JSON.stringify(addInfo.labels));
  await page.screenshot({ path: path.join(OUT, '10-add-dialog-full.png'), fullPage: true });

  await page.keyboard.press('Escape').catch(() => {});
  await page.waitForTimeout(1500);

  // ── 会计科目（对比数据源） ──
  net.length = 0;
  console.log('OPEN 会计科目:', await openMdPage(page, '会计科目'));
  await page.waitForTimeout(2000);
  fs.writeFileSync(path.join(OUT, '11-net-accounting-subject.json'), JSON.stringify(net, null, 1));
  console.log('NET 会计科目:', JSON.stringify(net.slice(-25)));
  const accRows = await page.evaluate(() => {
    const txt = el => (el ? (el.innerText || '').trim().replace(/\s+/g, ' ') : '');
    const vis = el => el && el.offsetParent !== null;
    return [...document.querySelectorAll('.dgrid-row')].filter(vis).map(tr => txt(tr)).slice(0, 60);
  });
  fs.writeFileSync(path.join(OUT, '11-accounting-subject-rows.json'), JSON.stringify(accRows, null, 1));
  console.log('会计科目 ROWS:', JSON.stringify(accRows));

  await browser.close();
  console.log('DONE ->', OUT);
})().catch(e => { console.error('FATAL', e.message); process.exit(1); });
