// 费用类型（资料→财务账户→费用类型）对标页面实时探针
// 用法: cd frontend && node ../tools/ql361-expense-type-probe.cjs
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright');
const fs = require('fs');
const path = require('path');

const OUT = path.resolve(__dirname, '../tool-results/ql361/expense-type');
fs.mkdirSync(OUT, { recursive: true });

const dumpStructure = (page) => page.evaluate(() => {
  const txt = el => (el ? (el.innerText || '').trim().replace(/\s+/g, ' ') : '');
  const vis = el => el && el.offsetParent !== null;
  const r = { url: location.href };

  r.desktopTabs = [...document.querySelectorAll('.dijitTab')].filter(vis).map(txt).filter(Boolean);

  const panels = [...document.querySelectorAll('[role=tabpanel], .dijitTabPane')].filter(vis);
  const panel = panels.sort((a, b) => (b.innerText || '').length - (a.innerText || '').length)[0];
  r.panelText = (panel?.innerText || '').slice(0, 3000);

  r.buttons = [...new Set([...document.querySelectorAll('button, [class*=btn], a[class*=button], [role=button]')]
    .filter(vis).map(txt).filter(t => t && t.length < 20))];

  r.inputs = [...document.querySelectorAll('input, select, textarea')].filter(vis)
    .map(el => ({ type: el.type, ph: el.getAttribute('placeholder') || '', val: el.value, cls: String(el.className).slice(0, 50) }));

  r.checkboxes = [...document.querySelectorAll('input[type=checkbox]')].filter(el => el.offsetParent !== null)
    .map(el => {
      let label = '';
      const id = el.id;
      if (id) { const l = document.querySelector(`label[for="${id}"]`); if (l) label = txt(l); }
      if (!label) { const p = el.closest('label') || el.parentElement; if (p) label = txt(p); }
      return { label: label.slice(0, 30), checked: el.checked };
    });

  // 左侧分类树（若有）
  const catPanel = [...document.querySelectorAll('*')].find(e => vis(e) && /分类|类型/.test(txt(e)) && txt(e).length < 300);
  r.categoryPanelText = catPanel ? txt(catPanel) : '';
  r.categoryTree = [...document.querySelectorAll('.dijitTreeNode, [class*=treeNode], [class*=tree-node]')].filter(vis)
    .map(e => txt(e)).filter(t => t && t.length < 30).slice(0, 40);

  r.headers = [...document.querySelectorAll('.dgrid-header .dgrid-cell, [role=columnheader], th')].filter(vis).map(txt).filter(Boolean);

  const rows = [...document.querySelectorAll('.dgrid-row, tbody tr')].filter(vis).slice(0, 20);
  r.rows = rows.map(tr => [...tr.querySelectorAll('.dgrid-cell, td')].map(td => txt(td)).slice(0, 12));

  r.footerText = [...document.querySelectorAll('[class*=footer], [class*=pagination], [class*=pager]')].filter(vis).map(txt).filter(Boolean).slice(0, 6);

  return r;
});

const dumpDialog = (page) => page.evaluate(() => {
  const txt = el => (el ? (el.innerText || '').trim() : '');
  const vis = el => el && el.offsetParent !== null;
  const dialogs = [...document.querySelectorAll('[role=dialog], [class*=dialog], [class*=Dialog], [class*=popup]')]
    .filter(vis).map(c => ({ cls: String(c.className).slice(0, 80), text: txt(c) }))
    .filter(d => d.text && d.text.length > 10);
  dialogs.sort((a, b) => b.text.length - a.text.length);
  const all = dialogs.slice(0, 3).map(d => ({ cls: d.cls, text: d.text.slice(0, 2500) }));
  const rowChecks = [...document.querySelectorAll('[role=dialog] tr, [class*=dialog] tr, [class*=Dialog] tr')]
    .filter(vis).map(tr => {
      const cbs = [...tr.querySelectorAll('input[type=checkbox]')];
      const cells = [...tr.querySelectorAll('td')].map(td => txt(td));
      return { cells, checked: cbs.map(c => c.checked) };
    }).filter(r => r.cells.length > 1);
  const formFields = [...document.querySelectorAll('[role=dialog] input, [class*=dialog] input, [class*=Dialog] input')]
    .filter(vis).map(el => {
      const p = el.closest('[class*=item], tr, div');
      const label = p ? txt(p).split('\n')[0].slice(0, 30) : '';
      return { label, type: el.type, ph: el.getAttribute('placeholder') || '', val: el.value, readonly: el.readOnly, cls: String(el.className).slice(0, 40) };
    });
  const selects = [...document.querySelectorAll('[role=dialog] select, [class*=Dialog] select')].filter(vis)
    .map(el => ({ cls: String(el.className).slice(0, 40), options: [...el.options].map(o => o.text).slice(0, 20), val: el.value }));
  const radioLabels = [...document.querySelectorAll('[role=dialog] label, [class*=Dialog] label')].filter(vis).map(txt).filter(t => t && t.length < 20);
  return { dialogs: all, rowChecks, formFields, selects, radioLabels };
});

(async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' });
  const page = await ctx.newPage();

  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'networkidle', timeout: 45000 }).catch(() => {});
  await page.fill('input[placeholder="请输入手机号/用户名"]', '15095664266').catch(() => {});
  await page.fill('#pwd', 'Ysh790921').catch(() => {});
  await page.click('button.logon_button').catch(() => {});
  for (let i = 0; i < 25; i++) {
    await page.waitForTimeout(1000);
    if (!page.url().includes('/Account/Logon')) break;
  }
  await page.waitForTimeout(8000);
  console.log('LOGGED_IN', page.url());
  await page.keyboard.press('Escape').catch(() => {});
  await page.evaluate(() => {
    document.querySelectorAll('.dojoxDialogCloseIcon,.lk-dialog-close,[title=关闭],[aria-label=Close]').forEach(b => b.click());
  }).catch(() => {});
  await page.waitForTimeout(1500);

  // 打开 资料 菜单
  await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true });
  await page.waitForTimeout(2000);
  const menuTexts = await page.evaluate(() => [...document.querySelectorAll('.popupmenu__nav-text')].map(e => (e.innerText || '').trim()));
  console.log('MENU ITEMS:', JSON.stringify(menuTexts));
  fs.writeFileSync(path.join(OUT, 'menu-items.json'), JSON.stringify(menuTexts, null, 1));

  const grp = page.locator('.popupmenu__nav-text', { hasText: '财务账户' }).first();
  if (await grp.count()) { await grp.hover().catch(() => {}); await page.waitForTimeout(1200); }

  const clicked = await page.evaluate(() => {
    const el = [...document.querySelectorAll('.popupmenu__nav-text')].find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '费用类型');
    if (el) { el.click(); return true; }
    return false;
  });
  console.log('CLICK 费用类型:', clicked);
  await page.waitForTimeout(6000);

  await page.evaluate(() => {
    const tabs = [...document.querySelectorAll('.dijitTab')];
    tabs.forEach(t => {
      const isActive = /dijitTabChecked|dijitTabActive/.test(t.className);
      if (!isActive) {
        const close = t.querySelector('.dijitTabCloseButton, [class*=Close]');
        if (close) close.click();
      }
    });
  }).catch(() => {});
  await page.waitForTimeout(2500);
  await page.keyboard.press('Escape').catch(() => {});
  await page.waitForTimeout(500);

  await page.screenshot({ path: path.join(OUT, '01-page.png'), fullPage: false });
  const struct = await dumpStructure(page);
  fs.writeFileSync(path.join(OUT, '01-structure.json'), JSON.stringify(struct, null, 1));
  console.log('HEADERS:', JSON.stringify(struct.headers));
  console.log('BUTTONS:', JSON.stringify(struct.buttons));
  console.log('CHECKBOXES:', JSON.stringify(struct.checkboxes));
  console.log('INPUTS:', JSON.stringify(struct.inputs));
  console.log('CATEGORY TREE:', JSON.stringify(struct.categoryTree));
  console.log('ROWS:', JSON.stringify(struct.rows));
  console.log('FOOTER:', JSON.stringify(struct.footerText));

  // 列配置弹窗
  const gearInfo = await page.evaluate(() => {
    const gears = [...document.querySelectorAll('.icon-shezhi2, [class*=shezhi]')].filter(e => e.offsetParent !== null);
    return gears.map(g => ({ cls: String(g.className).slice(0, 60), title: g.getAttribute('title') || '', rect: g.getBoundingClientRect().toJSON() }));
  });
  fs.writeFileSync(path.join(OUT, '02-gears.json'), JSON.stringify(gearInfo, null, 1));
  console.log('GEARS:', JSON.stringify(gearInfo));

  const headerGear = page.locator('.dgrid-header .icon-shezhi2, .dgrid-column-set .icon-shezhi2').first();
  if (await headerGear.count()) {
    await headerGear.click({ force: true }).catch(() => {});
    await page.waitForTimeout(2500);
    await page.screenshot({ path: path.join(OUT, '03-column-config.png') });
    const cfg = await dumpDialog(page);
    fs.writeFileSync(path.join(OUT, '03-column-config.json'), JSON.stringify(cfg, null, 1));
    console.log('COLUMN CONFIG DIALOG:', JSON.stringify(cfg.dialogs));
    console.log('COLUMN CONFIG CHECKS:', JSON.stringify(cfg.rowChecks));
    await page.keyboard.press('Escape').catch(() => {});
    await page.waitForTimeout(1200);
  } else {
    console.log('NO HEADER GEAR');
  }

  // 树展开/收起按钮探测
  const treeBtns = await page.evaluate(() => [...document.querySelectorAll('[class*=icon-triangle], [class*=tree]')]
    .filter(e => e.offsetParent !== null).map(e => ({ cls: String(e.className).slice(0, 60), title: e.getAttribute('title') || '' })).slice(0, 20));
  console.log('TREE BTNS:', JSON.stringify(treeBtns));
  fs.writeFileSync(path.join(OUT, '04-tree-buttons.json'), JSON.stringify(treeBtns, null, 1));

  // 点“修改”打开编辑弹窗
  const editClicked = await page.evaluate(() => {
    const links = [...document.querySelectorAll('.dgrid-row a, .dgrid-row span, .dgrid-row button')]
      .filter(e => e.offsetParent !== null && /^修改$/.test((e.innerText || '').trim()));
    if (links.length) { links[links.length - 1].click(); return true; }
    return false;
  });
  console.log('CLICK 修改:', editClicked);
  await page.waitForTimeout(3000);
  await page.screenshot({ path: path.join(OUT, '05-edit-dialog.png') });
  const editDlg = await dumpDialog(page);
  fs.writeFileSync(path.join(OUT, '05-edit-dialog.json'), JSON.stringify(editDlg, null, 1));
  console.log('EDIT DIALOG:', JSON.stringify(editDlg.dialogs));
  console.log('EDIT FIELDS:', JSON.stringify(editDlg.formFields));
  await page.keyboard.press('Escape').catch(() => {});
  await page.waitForTimeout(1200);

  // 点“新增费用”
  await page.evaluate(() => {
    const btns = [...document.querySelectorAll('button, [class*=btn]')].filter(e => e.offsetParent !== null && /新增费用|新增/.test(e.innerText || ''));
    if (btns[0]) btns[0].click();
  }).catch(() => {});
  await page.waitForTimeout(3000);
  await page.screenshot({ path: path.join(OUT, '06-add-dialog.png') });
  const addDlg = await dumpDialog(page);
  fs.writeFileSync(path.join(OUT, '06-add-dialog.json'), JSON.stringify(addDlg, null, 1));
  console.log('ADD DIALOG:', JSON.stringify(addDlg.dialogs));
  console.log('ADD FIELDS:', JSON.stringify(addDlg.formFields));

  // 探查页面内部 js 数据源（用于判定是否枚举/科目）
  const pageSrc = await page.evaluate(() => {
    const scripts = [...document.querySelectorAll('script')].map(s => s.src).filter(Boolean);
    return { scripts: scripts.slice(0, 60) };
  });
  fs.writeFileSync(path.join(OUT, '07-scripts.json'), JSON.stringify(pageSrc, null, 1));

  await browser.close();
  console.log('DONE ->', OUT);
})().catch(e => { console.error('FATAL', e.message); process.exit(1); });
