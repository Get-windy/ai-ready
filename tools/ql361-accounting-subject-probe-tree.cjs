// 会计科目：科目分类树点击 + 行内展开 + 编辑链接可用性
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright');
const fs = require('fs');
const path = require('path');

const OUT = path.resolve(__dirname, '../tool-results/ql361/accounting-subject');
fs.mkdirSync(OUT, { recursive: true });
const logs = [];
const log = (...a) => { const s = a.map(x => typeof x === 'string' ? x : JSON.stringify(x)).join(' '); logs.push(s); console.log(s); };

(async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' });
  const page = await ctx.newPage();

  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'networkidle', timeout: 45000 }).catch(() => {});
  await page.fill('input[placeholder="请输入手机号/用户名"]', '15095664266').catch(() => {});
  await page.fill('#pwd', 'Ysh790921').catch(() => {});
  await page.click('button.logon_button').catch(() => {});
  for (let i = 0; i < 25; i++) { await page.waitForTimeout(1000); if (!page.url().includes('/Account/Logon')) break; }
  await page.waitForTimeout(9000);
  await page.keyboard.press('Escape').catch(() => {});
  await page.evaluate(() => document.querySelectorAll('.dijitDialogCloseIcon').forEach(b => { try { b.click(); } catch (e) {} })).catch(() => {});
  await page.waitForTimeout(1500);
  log('LOGGED_IN');

  await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true });
  await page.waitForTimeout(2000);
  await page.evaluate(() => {
    const el = [...document.querySelectorAll('.popupmenu__nav-text')].find(e => /财务账户/.test((e.innerText || '').trim()));
    if (el) { el.dispatchEvent(new MouseEvent('mouseover', { bubbles: true })); el.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true })); }
  });
  await page.waitForTimeout(1500);
  await page.evaluate(() => {
    const el = [...document.querySelectorAll('.popupmenu__nav-text')].find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '会计科目');
    if (el) el.click();
  });
  await page.waitForTimeout(9000);
  await page.keyboard.press('Escape').catch(() => {});
  log('TITLE:', await page.title());

  // 定位当前激活的 会计科目 tabpanel
  const panelInfo = await page.evaluate(() => {
    const vis = e => e && e.offsetParent !== null;
    const panels = [...document.querySelectorAll('.dijitTabPane, [role=tabpanel], .tab-pane, [class*=tabContent]')].filter(vis);
    return panels.map(p => ({ cls: String(p.className).slice(0, 60), len: (p.innerText || '').length, hasAdd: /新增会计科目/.test(p.innerText || ''), head: (p.innerText || '').slice(0, 120) }));
  });
  log('PANELS:', panelInfo);

  const panelSel = '.dijitTabPane';
  const scope = page.locator(panelSel).filter({ hasText: '新增会计科目' }).first();
  await scope.screenshot({ path: path.join(OUT, '51-panel.png') }).catch(e => log('shot fail', e.message.split('\n')[0]));

  // dump 科目分类树 DOM
  const treeDom = await page.evaluate(() => {
    const vis = e => e && e.offsetParent !== null;
    const cands = [...document.querySelectorAll('[class*=tree] *')].filter(vis).slice(0, 60)
      .map(e => ({ tag: e.tagName, cls: String(e.className).slice(0, 70), txt: (e.innerText || '').trim().slice(0, 25) }))
      .filter(x => x.txt);
    return cands;
  });
  log('TREE DOM:', treeDom.slice(0, 40));
  fs.writeFileSync(path.join(OUT, '51-tree-dom.json'), JSON.stringify(treeDom, null, 1));

  // 点击「资产类」
  const clicked = await page.evaluate(() => {
    const vis = e => e && e.offsetParent !== null;
    const all = [...document.querySelectorAll('*')].filter(vis);
    const node = all.find(e => (e.innerText || '').trim() === '资产类');
    if (!node) return 'NOTFOUND';
    node.click();
    return String(node.className).slice(0, 60);
  });
  log('CLICK 资产类 =>', clicked);
  await page.waitForTimeout(3000);
  const after = await page.evaluate(() => {
    const txt = e => (e ? (e.innerText || '').trim().replace(/\s+/g, ' ') : '');
    const all = [...document.querySelectorAll('*')].filter(e => e.offsetParent !== null);
    const pathEl = all.find(e => /当前路径/.test(txt(e)) && txt(e).length < 120);
    const rows = [...document.querySelectorAll('.dgrid-row')].filter(e => e.offsetParent !== null).map(txt).filter(t => t.length > 3);
    return { path: pathEl ? txt(pathEl).slice(0, 120) : '', rowCount: rows.length, rows: rows.slice(0, 6) };
  });
  log('AFTER CLICK:', after);
  await page.screenshot({ path: path.join(OUT, '52-after-category.png') });

  // 行内操作列链接可用性 + 点第一行文件夹图标
  const acts = await page.evaluate(() => {
    const vis = e => e && e.offsetParent !== null;
    const rows = [...document.querySelectorAll('.dgrid-row')].filter(vis);
    return rows.slice(0, 5).map(r => {
      const links = [...r.querySelectorAll('a, span, i')].filter(e => /^(修改|删除|更多)$/.test((e.innerText || '').trim()))
        .map(e => ({ t: (e.innerText || '').trim(), cls: String(e.className).slice(0, 70), cursor: getComputedStyle(e).cursor, color: getComputedStyle(e).color, pe: getComputedStyle(e).pointerEvents }));
      return { rowTxt: (r.innerText || '').trim().replace(/\s+/g, ' ').slice(0, 40), links };
    });
  });
  log('ROW ACTIONS:', acts);
  fs.writeFileSync(path.join(OUT, '52-row-actions.json'), JSON.stringify(acts, null, 1));

  // 点第一行操作列的文件夹图标（展开）
  const expand = await page.evaluate(() => {
    const vis = e => e && e.offsetParent !== null;
    const row = [...document.querySelectorAll('.dgrid-row')].filter(vis)[0];
    if (!row) return 'NO ROW';
    const icons = [...row.querySelectorAll('i, span, img, div')].filter(vis).filter(e => /triangle|folder|icon-/.test(String(e.className)));
    if (!icons.length) return 'NO ICON';
    icons[0].click();
    return String(icons[0].className).slice(0, 70);
  });
  log('EXPAND CLICK:', expand);
  await page.waitForTimeout(3000);
  const afterExpand = await page.evaluate(() => {
    const txt = e => (e ? (e.innerText || '').trim().replace(/\s+/g, ' ') : '');
    const rows = [...document.querySelectorAll('.dgrid-row')].filter(e => e.offsetParent !== null).map(txt).filter(t => t.length > 3);
    return { rowCount: rows.length, rows: rows.slice(0, 8) };
  });
  log('AFTER EXPAND:', afterExpand);
  await page.screenshot({ path: path.join(OUT, '53-expanded.png') });

  // 尝试点最后一行“修改”
  const editTry = await page.evaluate(() => {
    const vis = e => e && e.offsetParent !== null;
    const links = [...document.querySelectorAll('a, span, i')].filter(vis).filter(e => /^修改$/.test((e.innerText || '').trim()));
    if (!links.length) return 'NO 修改';
    links[links.length - 1].click();
    return 'clicked ' + links.length;
  });
  log('EDIT TRY:', editTry);
  await page.waitForTimeout(4000);
  await page.screenshot({ path: path.join(OUT, '54-edit.png') });
  const dlg = await page.evaluate(() => {
    const txt = e => (e ? (e.innerText || '').trim() : '');
    const vis = e => e && e.offsetParent !== null;
    const visibles = [...document.querySelectorAll('[class*=dijitDialog]')].filter(vis).map(d => ({ cls: String(d.className).slice(0, 80), text: txt(d).slice(0, 1200) }));
    const inputs = [...document.querySelectorAll('input,select,textarea')].filter(vis).map(el => {
      const tr = el.closest('tr') || el.closest('[class*=item]');
      return { type: el.type, label: tr ? txt(tr).replace(/\s+/g, ' ').slice(0, 40) : '', val: el.value };
    });
    return { visibles, inputs };
  });
  log('EDIT DIALOG:', dlg.visibles.map(v => v.text.slice(0, 300)));
  log('EDIT INPUTS:', dlg.inputs);
  fs.writeFileSync(path.join(OUT, '54-edit.json'), JSON.stringify(dlg, null, 1));

  await browser.close();
  fs.writeFileSync(path.join(OUT, 'probe7.log'), logs.join('\n'));
})().catch(e => { console.error('FATAL', e.message); process.exit(1); });
