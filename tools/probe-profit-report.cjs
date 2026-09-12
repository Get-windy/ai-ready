/* 利润表金标准接口探针：自动破解 SVG 验证码登录并查询报表 */
const http = require('http')

const BASE = process.env.API_BASE || 'http://localhost:5655/api'

function req(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost',
      port: Number(new URL(BASE).port),
      path: new URL(BASE).pathname + path,
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }, res => {
      let s = ''
      res.on('data', c => { s += c })
      res.on('end', () => resolve({ status: res.statusCode, body: s }))
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

function solveCaptcha(svg) {
  const chars = [...svg.matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1])
  return chars.join('')
}

async function main() {
  const cap = await req('GET', '/auth/captcha')
  const capData = JSON.parse(cap.body).data
  const code = solveCaptcha(Buffer.from(capData.img.split(',')[1], 'base64').toString('utf8'))

  const login = await req('POST', '/auth/login', {
    username: 'admin',
    password: 'admin123',
    tenantName: '系统租户',
    captcha: code,
    captchaKey: capData.uuid,
  })
  const loginBody = JSON.parse(login.body)
  const token = loginBody?.data?.token || loginBody?.data?.accessToken
  if (!token) {
    console.error('LOGIN FAILED:', login.status, login.body.slice(0, 300))
    process.exit(1)
  }
  console.log('LOGIN OK, captcha =', code)

  const queries = JSON.parse(process.env.QUERIES || '[{}]')
  for (const q of queries) {
    const qs = new URLSearchParams({
      fiscalYear: '2026',
      fiscalPeriod: '9',
      periodMode: 'single',
      subjectLevel: '2',
      showZero: 'false',
      ...q,
    }).toString()
    const res = await req('GET', `/erp/finance/report/v2/income-statement-report?${qs}`, null, token)
    console.log(`\n===== ${qs} -> ${res.status} =====`)
    const parsed = JSON.parse(res.body)
    const d = parsed.data
    if (!d) { console.log(res.body.slice(0, 500)); continue }
    console.log(`口径: ${d.fiscalYear}年 期间${d.startPeriod}~${d.endPeriod} 层级${d.subjectLevel} 显示0=${d.showZero} hasData=${d.hasData}`)
    console.log('rowNo | itemCode              | indent | summary | itemName                          | 本期发生额 | 本年累计')
    for (const r of d.rows) {
      console.log([
        String(r.rowNo).padStart(5),
        String(r.itemCode ?? '').padEnd(21),
        String(r.indentLevel).padStart(6),
        String(r.summaryRow).padStart(7),
        String(r.itemName ?? '').padEnd(33),
        String(r.currentAmount).padStart(12),
        String(r.cumulativeAmount).padStart(12),
      ].join(' | '))
    }
    console.log('段汇总: 营业收入=%s 其他收入=%s 成本=%s 费用=%s 营业利润=%s 利润总额=%s 净利润=%s',
      d.revenueTotal, d.otherIncomeTotal, d.costTotal, d.expenseTotal, d.operatingProfit, d.totalProfit, d.netProfit)
  }
}

main().catch(e => { console.error(e); process.exit(1) })
