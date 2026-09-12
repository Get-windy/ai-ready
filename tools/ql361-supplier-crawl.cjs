/* ql361 供应商页面对标抓取：列表页 / 列配置弹窗 / 新增表单页
 * 运行： node tools/ql361-supplier-crawl.cjs
 * 输出： tool-results/ql361/supplier/*.png + *.json
 */
const { chromium } = require('playwright')
const fs = require('fs')
const path = require('path')

const OUT = path.resolve(__dirname, '../tool-results/ql361/supplier')
const BASE = process.env.QL361_BASE || 'https://www.ql361.com'
const ACCOUNT = process.env.QL361_USER || '15095664266'
const PASSWORD = process.env.QL361_PWD || 'Ysh790921'

function log(...a) { console.log('[crawl]', ...a) }

async function shot(page, name, fullPage = true) {
  await page.screenshot({ path: path.join(OUT, `${name}.png`), fullPage })
  log('screenshot ->', `${name}.png`)
}

async function dumpTable(page, name) {
  const data = await page.evaluate(() => {
    const out = { headers: [], rowCount: 0, firstRow: [], footText: '' }
    const ths = document.querySelectorAll('.el-table__header th, .ant-table-thead th, table thead th')
    ths.forEach(th => out.headers.push((th.innerText || '').trim().replace(/\s+/g, ' ')))
    const trs = document.querySelectorAll('.el-table__body tbody tr, .ant-table-tbody tr, table tbody tr')
    out.rowCount = trs.length
    if (trs[0]) {
      trs[0].querySelectorAll('td').forEach(td => out.firstRow.push((td.innerText || '').trim().replace(/\s+/g, ' ')))
    }
    return out
  })
  fs.writeFileSync(path.join(OUT, `${name}.json`), JSON.stringify(data, null, 2), 'utf8')
  log('table ->', name, 'headers=', data.headers.length, 'rows=', data.rowCount)
  return data
}

(async () => {
  fs.mkdirSync(OUT, { recursive: true })
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, deviceScaleFactor: 1 })
  const page = await ctx.newPage()
  page.on('console', m => { if (m.type() === 'error') log('console.error:', m.text().slice(0, 200)) })

  try {
    log('goto login')
    await page.goto(`${BASE}/Account/Logon`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForTimeout(2500)
    await shot(page, '00-login', false)

    // 填账号密码
    const inputs = page.locator('input')
    const n = await inputs.count()
    log('inputs on login page:', n)
    const visible = []
    for (let i = 0; i < n; i++) {
      const el = inputs.nth(i)
      if (await el.isVisible().catch(() => false)) {
        const attrs = await el.evaluate(e => ({
          type: e.type, name: e.name, id: e.id, ph: e.placeholder, cls: e.className,
        }))
        visible.push({ i, ...attrs })
      }
    }
    log('visible inputs:', JSON.stringify(visible))

    const userEl = page.locator('input[type="text"]:visible, input[name*="ccount"]:visible, input[id*="ccount"]:visible, input[placeholder*="账号"]:visible, input[placeholder*="用户"]:visible').first()
    const pwdEl = page.locator('input[type="password"]:visible').first()
    await userEl.fill(ACCOUNT)
    await pwdEl.fill(PASSWORD)
    await shot(page, '01-login-filled', false)

    const btn = page.locator('button:has-text("登录"), input[type="submit"], a:has-text("登录")').first()
    await btn.click()
    await page.waitForTimeout(6000)
    await shot(page, '02-after-login', false)
    log('url after login:', page.url())
    await browser.close()
  } catch (e) {
    log('ERROR:', e.message)
    await shot(page, 'zz-error', false).catch(() => {})
    await browser.close()
    process.exit(1)
  }
})()
