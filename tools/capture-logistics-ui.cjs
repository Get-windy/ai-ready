// 物流公司最终 UI 截图（导入样例数据 → 列表/表单 → 清理）
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const XLSX = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/xlsx')
const PORT = Number(process.env.ERP_PORT || 5662)
const FE = process.env.FE_URL || 'http://localhost:5656'
const SHOTS = 'I:/AI-Ready/tool-results/logistics'

function rawReq(method, path, body, token, extra) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: PORT, path: '/api' + path, method,
      headers: { 'Content-Type': 'application/json', ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    }, res => { const c = []; res.on('data', d => c.push(d)); res.on('end', () => { const b = Buffer.concat(c); try { resolve(JSON.parse(b.toString('utf8'))) } catch (e) { resolve({ raw: b, status: res.statusCode }) } }) })
    r.on('error', reject); if (data) r.write(data); r.end()
  })
}

function multipart(fields, fileName, fileBuf, token) {
  return new Promise((resolve, reject) => {
    const boundary = '----cap' + Date.now()
    const parts = []
    for (const [k, v] of Object.entries(fields || {})) {
      parts.push(Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="${k}"\r\n\r\n${v}\r\n`))
    }
    parts.push(Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="${fileName}"\r\nContent-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n`))
    parts.push(fileBuf)
    parts.push(Buffer.from(`\r\n--${boundary}--\r\n`))
    const body = Buffer.concat(parts)
    const r = http.request({ hostname: 'localhost', port: PORT, path: '/api/erp/md/customer/import-excel?partnerType=logistics', method: 'POST',
      headers: { 'Content-Type': `multipart/form-data; boundary=${boundary}`, 'Content-Length': body.length, Authorization: `Bearer ${token}` } },
      res => { const c = []; res.on('data', d => c.push(d)); res.on('end', () => { try { resolve(JSON.parse(Buffer.concat(c).toString('utf8'))) } catch (e) { resolve({ raw: true }) } }) })
    r.on('error', reject); r.write(body); r.end()
  })
}

;(async () => {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8').matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const lg = await rawReq('POST', '/auth/login', { username: 'e2e_logistics', password: 'admin123', tenantName: '系统租户', captcha: code, captchaKey: cap.data.uuid })
  const token = lg.data.token

  // 用真实导入接口灌样例数据（顺带演示导入闭环）
  const rows = [
    ['导入结果', '物流公司编号(必填)', '物流公司名称(必填)', '联系人', '联系电话', '公司地址', '备注'],
    ['', 'WuLiu901', '顺丰速运', '王卫', '18193725885', '广东省深圳市福田区', ''],
    ['', 'WuLiu902', '德邦物流', '崔维星', '13399376550', '上海市青浦区', ''],
    ['', 'WuLiu903', '中通快运', '赖梅松', '09372688900', '浙江省杭州市', ''],
    ['', 'WuLiu904', '酒泉金派物流', '白玉成', '13563652109', '甘肃省酒泉市', '敦煌到花土沟'],
    ['', 'WuLiu905', '兰州全德物流', '贾永京', '13919828460', '甘肃省兰州市', ''],
  ]
  const ws = XLSX.utils.aoa_to_sheet(rows)
  const wb = XLSX.utils.book_new(); XLSX.utils.book_append_sheet(wb, ws, '物流公司信息')
  const buf = XLSX.write(wb, { type: 'buffer', bookType: 'xlsx' })
  const imp = await multipart({}, 'cap.xlsx', buf, token)
  console.log('import:', JSON.stringify(imp.data || imp).slice(0, 160))

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const p = await ctx.newPage()
  await p.route(u => new URL(u).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url()); await route.continue({ url: `http://localhost:${PORT}` + u.pathname + u.search })
  })
  await p.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await p.evaluate(([tk]) => { localStorage.setItem('token', tk); localStorage.setItem('tenantId', '1') }, [token])
  await p.goto(`${FE}/md/logistics/index`, { waitUntil: 'domcontentloaded' })
  await p.waitForTimeout(6000)
  await p.screenshot({ path: `${SHOTS}/final-01-list.png` })

  // 勾选首行后展开「更多」，体现批量菜单可用
  const cb = p.locator('.ss-grid tbody input[type=checkbox]').first()
  if (await cb.count()) await cb.check({ force: true }).catch(() => {})
  await p.evaluate(() => {
    const norm = s => (s || '').replace(/\s+/g, '')
    const el = [...document.querySelectorAll('button')].find(b => norm(b.innerText) === '更多')
    if (el) { el.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true })); el.click() }
  })
  await p.waitForTimeout(1200)
  await p.screenshot({ path: `${SHOTS}/final-02-more-menu.png` })
  await p.keyboard.press('Escape')

  // 表单页（编辑回填）
  const list = await rawReq('GET', '/erp/md/customer/page?partnerType=LOGISTICS&pageNum=1&pageSize=5', null, token)
  const one = (list?.data?.records || [])[0]
  if (one) {
    await p.goto(`${FE}/md/logistics/form/${one.id}`, { waitUntil: 'domcontentloaded' })
    await p.waitForTimeout(4500)
    await p.screenshot({ path: `${SHOTS}/final-03-form.png` })
  }

  await browser.close()

  // 清理样例数据
  const all = await rawReq('GET', '/erp/md/customer/page?partnerType=LOGISTICS&pageNum=1&pageSize=50&keyword=WuLiu9', null, token)
  for (const r of (all?.data?.records || [])) {
    await rawReq('DELETE', '/erp/md/customer/' + r.id, null, token)
    console.log('cleaned', r.partnerCode, r.partnerName)
  }
  console.log('DONE')
})().catch(e => { console.error('FATAL', e); process.exit(1) })
