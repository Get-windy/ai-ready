/* 利润表金标准 端到端验证（独立 headless 浏览器，避开 MCP 浏览器占用） */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')

const FE = 'http://localhost:5656'
const API = 'http://localhost:5655/api'
const SHOTS = 'I:/AI-Ready/tool-results/profit-report'

function apiReq(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: 5655, path: '/api' + path, method,
      headers: {
        'Content-Type': 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }, res => { let s = ''; res.on('data', c => { s += c }); res.on('end', () => resolve(JSON.parse(s))) })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function login() {
  const cap = await apiReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await apiReq('POST', '/auth/login', {
    username: 'admin', password: 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  const token = res?.data?.token || res?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
  return { token, userInfo: res.data }
}

async function main() {
  const { token, userInfo } = await login()
  console.log('登录成功, 验证码 =', '已破解')

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1600, height: 950 } })
  const page = await ctx.newPage()

  const apiCalls = []
  page.on('response', async r => {
    if (r.status() >= 400) {
      console.log(`[HTTP ${r.status()}]`, r.url().replace('http://localhost:5656', ''))
    }
    if (r.url().includes('/income-statement-report')) {
      apiCalls.push({ url: r.url().replace('http://localhost:5656', ''), status: r.status() })
    }
  })
  page.on('pageerror', e => console.log('[pageerror]', e.message))
  page.on('console', m => { if (m.type() === 'error') console.log('[console.error]', m.text().slice(0, 200)) })

  await page.addInitScript(({ token, userInfo }) => {
    localStorage.setItem('token', token)
    localStorage.setItem('tenantId', String(userInfo?.tenantId || 1))
    localStorage.setItem('tenantName', userInfo?.tenantName || '系统租户')
  }, { token, userInfo })

  await page.goto(FE + '/finance/profit-report', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForSelector('tbody tr', { timeout: 60000 })
  await page.waitForTimeout(2500)

  const probe = await page.evaluate(() => {
    const rows = [...document.querySelectorAll('tbody tr')]
    const headers = [...document.querySelectorAll('thead th')].map(th => (th.textContent || '').trim())
    const gear = !!document.querySelector('.th-settings-btn')
    const toolbar = (document.querySelector('.toolbar-section') || document.body).textContent || ''
    return {
      url: location.pathname,
      title: (document.querySelector('.list-title') || {}).textContent,
      headers,
      rowCount: rows.length,
      firstRows: rows.slice(0, 10).map(tr => [...tr.querySelectorAll('td')].map(td => (td.textContent || '').trim())),
      hasGear: gear,
      hasRefresh: toolbar.includes('刷新'),
      hasPrint: toolbar.includes('打印(F8)'),
      hasExport: toolbar.includes('导出'),
      hasScheme: !!document.querySelector('.query-scheme-wrap'),
      radios: [...document.querySelectorAll('.ant-radio-button-wrapper')].map(e => e.textContent.trim()),
      labels: [...document.querySelectorAll('.search-label')].map(e => e.textContent.trim()),
      hasShowZero: !!document.querySelector('.search-checkbox-item input[type=checkbox]'),
      subjectLevel: (document.querySelector('.search-item .ant-select-selection-item') || {}).textContent,
      summaryBold: rows.slice(0, 10).map(tr => {
        const el = tr.querySelector('.item-name.is-total')
        return el ? getComputedStyle(el).fontWeight : ''
      }).filter(Boolean),
    }
  })
  console.log('\n=== 页面结构 ===')
  console.log(JSON.stringify(probe, null, 2))
  console.log('\nAPI 调用:', JSON.stringify(apiCalls))

  await page.screenshot({ path: SHOTS + '/01-default.png', fullPage: false })

  // ── 勾选「显示为0科目」 ──
  await page.click('.search-checkbox-item .ant-checkbox-wrapper')
  await page.waitForTimeout(2000)
  const rowsWithZero = await page.evaluate(() =>
    [...document.querySelectorAll('tbody tr')].map(tr => [...tr.querySelectorAll('td')].map(td => (td.textContent || '').trim())))
  console.log('\n=== 显示为0科目（勾选后）行数 =', rowsWithZero.length, '===')
  rowsWithZero.forEach(r => console.log(' |', r.join(' | ')))
  await page.screenshot({ path: SHOTS + '/02-show-zero.png', fullPage: false })

  // ── 科目层级切到 1 级 ──
  await page.click('.search-item .ant-select-selector')
  await page.waitForTimeout(600)
  await page.click('.ant-select-item-option[title="1级"]')
  await page.waitForTimeout(2000)
  const rowsLevel1 = await page.evaluate(() =>
    [...document.querySelectorAll('tbody tr')].map(tr => [...tr.querySelectorAll('td')].map(td => (td.textContent || '').trim())))
  console.log('\n=== 科目层级=1 行数 =', rowsLevel1.length, '===')
  rowsLevel1.forEach(r => console.log(' |', r.join(' | ')))
  await page.screenshot({ path: SHOTS + '/03-level1.png', fullPage: false })

  // ── 列配置弹窗（表头齿轮） ──
  await page.click('.th-settings-btn')
  await page.waitForTimeout(1200)
  const colPanel = await page.evaluate(() => {
    const m = document.querySelector('.ant-modal-content')
    if (!m) return null
    return {
      title: (m.querySelector('.ant-modal-title') || {}).textContent,
      tabs: [...m.querySelectorAll('.ant-tabs-tab')].map(e => e.textContent.trim()),
      cols: [...m.querySelectorAll('.col-config-table tbody tr')].map(tr => [...tr.querySelectorAll('td')].map(td => (td.textContent || '').trim())),
    }
  })
  console.log('\n=== 列配置弹窗 ===')
  console.log(JSON.stringify(colPanel, null, 2))
  await page.screenshot({ path: SHOTS + '/04-column-config.png', fullPage: false })
  await page.keyboard.press('Escape')
  await page.waitForTimeout(600)

  // ── 多会计月模式 ──
  await page.click('.ant-radio-button-wrapper:has-text("多会计月")')
  await page.waitForTimeout(2000)
  const multi = await page.evaluate(() => ({
    startLabel: [...document.querySelectorAll('.search-label')].map(e => e.textContent.trim()),
    rows: document.querySelectorAll('tbody tr').length,
  }))
  console.log('\n=== 多会计月模式 ===', JSON.stringify(multi))
  await page.screenshot({ path: SHOTS + '/05-multi-month.png', fullPage: false })

  console.log('\n最终 API 调用:', JSON.stringify(apiCalls, null, 2))
  await browser.close()
}

main().catch(e => { console.error('E2E FAILED:', e); process.exit(1) })
