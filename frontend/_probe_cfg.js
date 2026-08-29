// 对标系统配置抓取探针：抓取页面配置(查询条件/表单字段) + 数据表列配置(全量列)
// 用法: node _probe_cfg.js "<域>" "<组>" "<页面名>" [--tag <标签>]
const { chromium } = require('playwright');
const fs = require('fs');

const domain = process.argv[2];
const group = process.argv[3];
const item = process.argv[4];
const tag = process.argv[5] || '';

(async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' });
  const page = await ctx.newPage();

  // 1. 登录
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
  await page.evaluate(() => { document.querySelectorAll('.lk-dialog-close,[aria-label=Close],[title=关闭]').forEach(b => b.click()); }).catch(() => {});
  console.log('LOGGED_IN');

  // 2. 打开菜单
  await page.locator('a.menu-0-item', { hasText: domain }).first().click({ force: true }).catch(() => {});
  await page.waitForTimeout(1500);
  await page.evaluate(({ group, item }) => {
    const els = [...document.querySelectorAll('.popupmenu__nav-text')];
    const el = els.find(e => (e.innerText || '').trim().replace(/\s+/g, '') === item) ||
               els.find(e => (e.innerText || '').trim().includes(item));
    if (el) el.click();
  }, { group, item }).catch(() => {});
  await page.waitForTimeout(4500);
  console.log('OPENED_PAGE');

  // 3. 抓取页面配置弹窗（顶部齿轮）
  async function grabPageConfig() {
    const gear = page.locator('.title_bar .icon-shezhi2, .title-bar .icon-shezhi2, .lk-titlebar .icon-shezhi2, [class*=title] [class*=shezhi]').first();
    const cnt = await page.locator('[class*=title] [class*=shezhi], .title_bar .icon-shezhi2').count();
    if (cnt === 0) return { pageConfig: 'NO_GEAR' };
    await gear.click({ force: true }).catch(() => {});
    await page.waitForTimeout(2200);
    const data = await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null;
      // 找打开的弹窗（最大的可见 dialog/popup）
      const dialogs = [...document.querySelectorAll('[role=dialog], [class*=dialog], [class*=popup], [class*=drawer]')]
        .filter(vis).map(c => ({ cls: String(c.className).slice(0, 60), text: (c.innerText || '') }))
        .filter(d => d.text && d.text.length > 30);
      dialogs.sort((a, b) => b.text.length - a.text.length);
      const main = dialogs[0] || {};
      const lines = (main.text || '').split('\n').map(s => s.trim()).filter(s => s && s.length < 45);
      const checks = [...document.querySelectorAll('[role=dialog] label, [class*=dialog] label, [class*=popup] label, [class*=dialog] [class*=checkbox], [role=dialog] [class*=checkbox]')]
        .filter(vis).map(e => (e.innerText || '').trim()).filter(t => t && t.length < 40);
      const tabs = [...document.querySelectorAll('[role=tab], [class*=dialog] [class*=tab], [class*=tab-nav]')]
        .filter(vis).map(e => (e.innerText || '').trim()).filter(t => t && t.length < 15);
      return { checks, tabs, lines, dialogClass: main.cls };
    });
    // 关闭弹窗
    await page.keyboard.press('Escape').catch(() => {});
    await page.waitForTimeout(800);
    return { pageConfig: data };
  }

  // 4. 抓取列配置弹窗（表头齿轮）
  async function grabColumnConfig() {
    const gear = page.locator('.dgrid-column-set .icon-shezhi2, .dgrid-header .icon-shezhi2, .grid-header [class*=shezhi]').first();
    const cnt = await page.locator('.dgrid-column-set .icon-shezhi2, .dgrid-header .icon-shezhi2').count();
    if (cnt === 0) return { columnConfig: 'NO_GEAR' };
    await gear.click({ force: true }).catch(() => {});
    await page.waitForTimeout(2200);
    const data = await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null;
      const dialogs = [...document.querySelectorAll('[role=dialog], [class*=dialog], [class*=popup], [class*=drawer]')]
        .filter(vis).map(c => ({ cls: String(c.className).slice(0, 60), text: (c.innerText || '') }))
        .filter(d => d.text && d.text.length > 30);
      dialogs.sort((a, b) => b.text.length - a.text.length);
      const main = dialogs[0] || {};
      const lines = (main.text || '').split('\n').map(s => s.trim()).filter(s => s && s.length < 45);
      const checks = [...document.querySelectorAll('[role=dialog] label, [class*=dialog] label, [class*=popup] label, [class*=dialog] [class*=checkbox], [role=dialog] [class*=checkbox]')]
        .filter(vis).map(e => (e.innerText || '').trim()).filter(t => t && t.length < 40);
      const tabs = [...document.querySelectorAll('[role=tab], [class*=dialog] [class*=tab], [class*=tab-nav]')]
        .filter(vis).map(e => (e.innerText || '').trim()).filter(t => t && t.length < 15);
      return { checks, tabs, lines, dialogClass: main.cls };
    });
    return { columnConfig: data };
  }

  const pageCfg = await grabPageConfig();
  const colCfg = await grabColumnConfig();

  const result = { domain, group, item, tag, ts: new Date().toISOString(), ...pageCfg, ...colCfg };
  const outFile = `../tool-results/ql361/pages/${domain}__${group}__${item}__cfg.json`;
  fs.writeFileSync(outFile, JSON.stringify(result, null, 1), 'utf-8');
  console.log('PAGECONFIG:', JSON.stringify(pageCfg.pageConfig, null, 1).slice(0, 2000));
  console.log('COLUMNCONFIG:', JSON.stringify(colCfg.columnConfig, null, 1).slice(0, 2000));
  console.log('SAVED:', outFile);
  await browser.close();
})().catch(e => { console.error('FATAL', e.message); process.exit(1); });
