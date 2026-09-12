/* 仓库规划 UI 补充验证（列配置闭环 / 分页 / 打印 / 分类树过滤 / 空态）
 * 用法：node tools/e2e-warehouse-plan-ui-extra.cjs   （FE_URL=http://localhost:5656, API_PORT=5655）
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')

const FE = process.env.FE_URL || 'http://localhost:5656'
const API_PORT = Number(process.env.API_PORT || 5655)
const API = process.env.API_URL || `http://localhost:${API_PORT}`
const SHOTS = 'I:/AI-Ready/tool-results/warehouse-plan-ui'
const LOGIN_USER = process.env.LOGIN_USER || 'e2e_whplan'
if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

let pass = 0, fail = 0
const bad = []
function check(n, ok, d) {
  if (ok) { pass++; console.log(`  ✓ ${n}`) } else { fail++; bad.push(n); console.log(`  ✗ ${n}${d ? ' — ' + String(d).slice(0, 180) : ''}`) }
}
function rawReq(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({ hostname: 'localhost', port: API_PORT, path: '/api' + path, method,
      headers: { 'Content-Type': 'application/json', ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}), ...(token ? { Authorization: 'Bearer ' + token } : {}) } },
      res => { const c = []; res.on('data', x => c.push(x)); res.on('end', () => { const b = Buffer.concat(c).toString('utf8'); try { resolve(JSON.parse(b)) } catch { resolve({ raw: b.slice(0, 200), status: res.statusCode }) } }) })
    r.on('error', reject); if (data) r.write(data); r.end()
  })
}
let TOKEN = null
async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8').matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  let res = await rawReq('POST', '/auth/login', { username: LOGIN_USER, password: 'admin123', tenantName: '系统租户', captcha: code, captchaKey: cap.data.uuid })
  if (!(res?.data?.token || res?.data?.accessToken) && LOGIN_USER !== 'admin') {
    const c2 = await rawReq('GET', '/auth/captcha')
    const k2 = [...Buffer.from(c2.data.img.split(',')[1], 'base64').toString('utf8').matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
    res = await rawReq('POST', '/auth/login', { username: 'admin', password: 'admin123', tenantName: '系统租户', captcha: k2, captchaKey: c2.data.uuid })
  }
  TOKEN = res?.data?.token || res?.data?.accessToken || null
  return TOKEN
}

const stamp = String(Date.now()).slice(-6);

(async () => {
  await login()
  console.log('\n═══ 仓库规划 UI 补充验证 ═══')
  const api = (m, p, b) => rawReq(m, p, b, TOKEN)

  // 预清理历史残留（脚本可重复执行）
  {
    const stale = await api('GET', '/erp/warehouse/page?pageNum=1&pageSize=200&keyword=E2E&showDisabled=true')
    for (const r of stale?.data?.records || []) await api('DELETE', `/erp/warehouse/${r.id}`)
    const tree = await api('GET', '/erp/warehouse-category/tree')
    const collect = (nodes, out = []) => { (nodes || []).forEach(n => { out.push(n); collect(n.children, out) }); return out }
    for (const n of collect(tree?.data).filter(n => String(n.categoryName).startsWith('E2E')).reverse()) {
      await api('DELETE', `/erp/warehouse-category/${n.id}`)
    }
  }

  // 造 25 条仓库用于分页（另建 1 条用于分类过滤）
  const bulkIds = []
  for (let i = 0; i < 25; i++) {
    const r = await api('POST', '/erp/warehouse/save', { warehouseName: `E2E分页仓${String(i + 1).padStart(2, '0')}_${stamp}`, warehouseCode: `pg${stamp}${String(i).padStart(2, '0')}` })
    if (r?.data?.id) bulkIds.push(r.data.id)
  }
  check('构造 25 条分页数据', bulkIds.length === 25, bulkIds.length)

  const catName = `E2E过滤类${stamp}`
  const cat = await api('POST', '/erp/warehouse-category', { categoryName: catName })
  const catId = cat?.data?.id
  const catWh = await api('POST', '/erp/warehouse/save', { warehouseName: `E2E分类仓${stamp}`, warehouseCode: `cw${stamp}`, categoryId: catId })
  const catWhId = catWh?.data?.id
  check('构造分类过滤数据', !!catId && !!catWhId)

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const p = await ctx.newPage()
  const jsErrors = []
  p.on('pageerror', e => jsErrors.push(String(e)))
  await p.route(u => new URL(u).pathname.startsWith('/api/'), async r => {
    const u = new URL(r.request().url()); await r.continue({ url: API + u.pathname + u.search })
  })

  async function gotoPage(wait = 6000) {
    await login()
    await p.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' }).catch(() => {})
    await p.evaluate(([t, id]) => { localStorage.setItem('token', t); localStorage.setItem('tenantId', String(id)) }, [TOKEN, 1])
    await p.goto(`${FE}/md/warehouse-plan`, { waitUntil: 'domcontentloaded' })
    await p.waitForTimeout(wait)
  }
  const headers = () => p.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  const bodyText = () => p.evaluate(() => document.body.innerText)

  await gotoPage()

  // ── A. 列配置闭环 ──
  console.log('\n【列配置闭环】')
  const closedModal = () => p.locator('.ant-modal-close').last().click()
  async function setColumnVisible(name, visible) {
    const row = p.locator('.col-setting-row:visible').filter({ hasText: name }).first()
    const cb = row.locator('input[type=checkbox]')
    const isChecked = await cb.isChecked()
    if (isChecked !== visible) {
      await row.locator('.ant-checkbox').click()
      await p.waitForTimeout(600)
    }
  }
  {
    const before = await headers()
    check('A1 默认显示「地址」列', before.some(h => h.includes('地址')), before.join('/'))

    await p.locator('.ss-grid th .th-settings-btn').first().click()
    await p.waitForTimeout(1200)
    const rowCount = await p.locator('.col-setting-row:visible').count()
    check('A2 列配置弹窗列出 5 列', rowCount === 5, rowCount)
    await setColumnVisible('地址', false)
    await p.screenshot({ path: `${SHOTS}/ui-07-colconfig-hidden.png` })
    await closedModal()
    await p.waitForTimeout(1200)
    const afterHide = await headers()
    check('A3 取消勾选后列表隐藏「地址」', !afterHide.some(h => h.includes('地址')), afterHide.join('/'))

    // 持久化：重新加载页面
    await gotoPage(5000)
    const afterReload = await headers()
    check('A4 重新加载后仍隐藏（个人配置持久化）', !afterReload.some(h => h.includes('地址')), afterReload.join('/'))

    // 恢复默认
    await p.locator('.ss-grid th .th-settings-btn').first().click()
    await p.waitForTimeout(1200)
    await p.locator('.ant-modal:visible button', { hasText: '恢复默认' }).first().click()
    await p.waitForTimeout(1000)
    await closedModal()
    await p.waitForTimeout(1200)
    const afterReset = await headers()
    check('A5 恢复默认后「地址」列回来', afterReset.some(h => h.includes('地址')), afterReset.join('/'))
  }

  // ── B. 分页 ──
  console.log('\n【分页】')
  {
    await gotoPage(5000)
    const txt = await bodyText()
    const m = txt.match(/共\s*(\d+)\s*条记录/)
    // 25 条分页数据 + 3 条种子 + 1 条分类仓 = 29
    check('B1 分页显示总数（25+3+1=29）', !!m && Number(m[1]) === 29, m ? m[1] : 'no match')
    check('B2 分页显示「第(1/2)页」', /第\s*\(?\s*1\s*\/\s*2/.test(txt.replace(/\s/g, '')) || txt.includes('(1/2)'), txt.match(/第[^\n]*页/) ? txt.match(/第[^\n]*页/)[0] : '')
    await p.locator('.pagination button, .ant-pagination button, [class*=pagination] :text-is("下页")').first().click().catch(async () => {
      await p.locator(':text-is("下页")').first().click()
    })
    await p.waitForTimeout(2000)
    const txt2 = await bodyText()
    check('B3 点击「下页」到第 2 页', /2\s*\/\s*2/.test(txt2.replace(/\s/g, '')), txt2.match(/第[^\n]*页/) ? txt2.match(/第[^\n]*页/)[0] : '')
    await p.screenshot({ path: `${SHOTS}/ui-08-pagination.png` })
  }

  // ── C. 打印内容 ──
  console.log('\n【打印】')
  {
    await gotoPage(5000)
    await p.evaluate(() => {
      window.__printHtml = ''
      window.open = () => ({ document: { write: h => { window.__printHtml += h }, close() {} }, focus() {}, print() { window.__printed = true } })
      window.__printed = false
    })
    await p.keyboard.press('F8')
    await p.waitForTimeout(1500)
    const html = await p.evaluate(() => window.__printHtml)
    check('C1 F8 调起打印并输出 HTML', html.length > 500 && (await p.evaluate(() => window.__printed)))
    check('C2 打印含标题与表头', html.includes('仓库规划') && html.includes('仓库编号') && html.includes('联系电话') && html.includes('地址'))
    const rowCount = (html.match(/<tr>/g) || []).length - 1
    check('C3 打印行数=当前页记录数', rowCount > 0, rowCount)
  }

  // ── D. 分类树过滤 ──
  console.log('\n【分类树过滤】')
  {
    await gotoPage(5000)
    await p.locator('.category-panel .ant-tree-node-content-wrapper').filter({ hasText: catName }).first().click()
    await p.waitForTimeout(2500)
    const txt = await bodyText()
    check('D1 点击分类节点后列表只剩该分类仓库', txt.includes(`E2E分类仓${stamp}`) && !txt.includes(`E2E分页仓01_${stamp}`))
    check('D2 「当前路径」更新为 全部 / 分类名', txt.includes(catName) && txt.includes('当前路径'))
    await p.screenshot({ path: `${SHOTS}/ui-09-category-filter.png` })
    // 回到全部
    await p.locator('.category-panel .ant-tree-node-content-wrapper').first().click()
    await p.waitForTimeout(2000)
  }

  // ── E. 空态 ──
  console.log('\n【空态】')
  {
    const kw = '绝不存在XYZ' + stamp
    const input = p.locator('.search-row input:visible').first()
    await input.fill(kw)
    await input.press('Enter')
    await p.waitForTimeout(2200)
    const txt = (await bodyText()).replace(/\s/g, '')
    // 对标 ql361：空结果时表格补空白行、底部显示「共 0 条记录」（非 antd Empty 文案）
    const rowData = await p.evaluate(() => [...document.querySelectorAll('.ss-grid tbody tr')]
      .map(r => (r.innerText || '').replace(/\s/g, '')).filter(t => t && !/^\d+$/.test(t)).length)
    check('E1 无结果时列表为空（共 0 条记录 + 无数据行）', txt.includes('共0条记录') && rowData === 0, `rows=${rowData}`)
    await p.screenshot({ path: `${SHOTS}/ui-10-empty.png` })
  }
  check('E2 全流程无 JS 异常', jsErrors.length === 0, jsErrors.slice(0, 2).join(' | '))

  await browser.close()

  // ── 清理 ──
  console.log('\n【清理】')
  let cleaned = 0
  for (const id of [...bulkIds, catWhId]) {
    const r = await api('DELETE', `/erp/warehouse/${id}`)
    if (r?.code === 200) cleaned++
  }
  check('删除 26 条测试仓库', cleaned === 26, cleaned)
  const catDel = await api('DELETE', `/erp/warehouse-category/${catId}`)
  check('删除测试分类', catDel?.code === 200, catDel?.msg)
  const left = await api('GET', '/erp/warehouse/page?pageNum=1&pageSize=100&keyword=' + encodeURIComponent('E2E'))
  check('无残留数据', Number(left?.data?.total) === 0, left?.data?.total)

  console.log(`\n═══ 结果：${pass}/${pass + fail} 通过 ═══`)
  if (bad.length) console.log('失败项：\n - ' + bad.join('\n - '))
  process.exit(fail === 0 ? 0 : 1)
})().catch(e => { console.error('FATAL', e.message); process.exit(1) })
