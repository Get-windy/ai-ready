/**
 * 预订货单打印端到端：造一张单 → 走「单据打印」接口渲染 → 断言内容。
 *
 * 为什么要造单：dev 库里 `erp_sale_pre_order` 是空的，没有 documentId 就没法验证
 * 「pageCode=sale-pre-order → 装配器 → 模板 → HTML」这条链。
 * 造单走的是真实创建接口（含外键校验），不是手写 SQL 塞行。
 *
 * 用法：node tools/e2e-sale-pre-order-print.cjs
 * 环境：后端 :5655（BE_PORT 可改）、前端不参与。
 * 依赖的真实数据：客户 id=1（散客）、商品 id=990000000000000001/2（dev 库里未删除的两个）。
 * ⚠️ 别用 erp_product 里 deleted=1 的行 —— @TableLogic 会让装配器的 selectById 返回 null，
 *    表现为创建接口 500「商品不存在：N」。
 *
 * ⚠️ 每个用例都要重新登录（取验证码 + 载权限），别把它串在别的用例后面连跑。
 */
const http = require('http')
const fs = require('fs')
const path = require('path')

const BE = Number(process.env.BE_PORT || 5655)
const USER = process.env.PRINT_USER || 'e2e_settings'
const PASS = process.env.PRINT_PASSWORD || 'admin123'
const OUT = path.resolve(__dirname, '../tool-results/print-template-v2')

let TOKEN = null
function rawReq(method, p, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: BE, path: '/api' + p, method,
      headers: {
        'Content-Type': 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }, res => {
      const c = []
      res.on('data', d => c.push(d))
      res.on('end', () => {
        const s = Buffer.concat(c).toString('utf8')
        try { resolve({ status: res.statusCode, body: JSON.parse(s), raw: s }) }
        catch { resolve({ status: res.statusCode, body: s, raw: s }) }
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

/**
 * 取单据 id。
 *
 * ⚠️ 雪花 ID（本库是 990000000000000001 这种量级）超过 JS 安全整数 2^53。
 * **读**没问题：后端把 Long 序列化成字符串（`"id":"2104057297693716482"`）；
 * **写**必须发字符串 —— 写成字面量数字会被 JS 静默取整成 ...000，
 * 后端拿到的就是另一个 id（实测报「商品不存在：990000000000000000」）。
 */
function docIdOf(res) {
  const id = res.body?.data?.id
  return id == null ? null : String(id)
}

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.body.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const r = await rawReq('POST', '/auth/login', {
    username: USER, password: PASS, tenantName: '系统租户',
    captcha: code, captchaKey: cap.body.data.uuid,
  })
  const info = r.body?.data
  if (!info?.token && !info?.accessToken) {
    throw new Error('登录失败: ' + JSON.stringify(r.body).slice(0, 300))
  }
  return info.token || info.accessToken
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok })
  console.log(`  ${ok ? '✓' : '✗'} ${name}${detail ? ' — ' + detail : ''}`)
}

;(async () => {
  fs.mkdirSync(OUT, { recursive: true })
  console.log(`预订货单打印端到端 (BE :${BE})`)
  TOKEN = await login()
  console.log(`  登录成功：${USER}`)

  // ── ① 取号 + 造单 ──
  console.log('\n① 造一张预订货单（真实创建接口）')
  const noRes = await rawReq('GET', '/erp/sale/pre-order/next-no', null, TOKEN)
  const orderNo = noRes.body?.data
  check('取到单号', !!orderNo, orderNo || JSON.stringify(noRes.body).slice(0, 200))
  if (!orderNo) process.exit(1)

  const createRes = await rawReq('POST', '/erp/sale/pre-order', {
    orderNo,
    customerId: 1,
    orderDate: new Date().toISOString().slice(0, 10),
    warehouseName: '主仓库',
    handlerName: 'E2E经手人',
    deptName: '销售一部',
    receiverName: 'E2E收货人',
    receiverPhone: '13800000000',
    shippingAddress: 'E2E测试市测试路 1 号',
    summary: 'E2E 预订摘要',
    remark: 'E2E 单据备注（打印验证用）',
    totalAmount: 300,
    orderAmount: 300,
    depositAmount: 100,
    receivedDeposit: 50,
    unreceivedDeposit: 50,
    items: [
      // ⚠️ product_name 在库上是 NOT NULL 且创建接口不按 productId 回填 —— 页面是靠
      //    选商品时带过来的快照字段（这里必须一起给，否则撞 NOT NULL → 400）
      { productId: '990000000000000001', productName: '博多家园百香果果酱', productCode: 'SP-TEST-001',
        unit: '瓶', specification: '1kg*6瓶', quantity: 10, unitPrice: 20, amount: 200, remark: 'E2E 行备注甲' },
      { productId: '990000000000000002', productName: '海鲜全家福', productCode: 'SP-TEST-002',
        unit: '袋', specification: '500g', quantity: 2, unitPrice: 50, amount: 100 },
    ],
  }, TOKEN)
  const docId = docIdOf(createRes)
  check('创建成功', createRes.status === 200 && !!docId,
    `HTTP ${createRes.status} id=${docId || JSON.stringify(createRes.body).slice(0, 200)}`)
  if (!docId) process.exit(1)

  // ── ② 取模板 ──
  console.log('\n② GET /v2/print/documents/sale-pre-order/templates')
  const tpl = await rawReq('GET', '/v2/print/documents/sale-pre-order/templates', null, TOKEN)
  const tplBody = tpl.body?.data || {}
  const tpls = tplBody.templates || []
  check('接口 200', tpl.status === 200, `HTTP ${tpl.status}`)
  check('该页有已发布模板', tpls.length > 0, tpls.map(t => t.templateName).join(', '))
  check('后端已注册装配器（supported=true）', tplBody.supported === true, `supported=${tplBody.supported}`)
  if (!tpls.length) process.exit(1)

  // ── ③ 按单据渲染 ──
  console.log('\n③ POST /v2/print/documents/sale-pre-order/{id}/render')
  const r = await rawReq('POST', `/v2/print/documents/sale-pre-order/${docId}/render`, null, TOKEN)
  const data = r.body?.data || {}
  const html = data.html || ''
  check('接口 200', r.status === 200, `HTTP ${r.status}`)
  check('返回了 HTML', html.length > 500, `${html.length} 字节`)
  if (!html) {
    console.log('\n后端返回：', JSON.stringify(r.body).slice(0, 600))
    process.exit(1)
  }

  // ── ④ 内容断言（打不出来 / 打出空白单，这里就该红）──
  console.log('\n④ 渲染内容')
  check('是完整 HTML 文档', /<!DOCTYPE html>/i.test(html) && /<html[\s>]/i.test(html))
  check('带单据编号', data.documentNo === orderNo && html.includes(orderNo), `documentNo=${data.documentNo}`)
  check('标题是预订货单', html.includes('预订货单'))
  for (const label of ['预订数量', '金额合计', '已收预订金', '本单金额大写']) {
    check(`含「${label}」`, html.includes(label))
  }
  for (const text of ['E2E经手人', 'E2E 预订摘要', '博多家园百香果果酱', '海鲜全家福', 'E2E 行备注甲']) {
    check(`含数据「${text}」`, html.includes(text))
  }
  check('明细表有表头（每页重印）', (html.match(/<thead>/g) || []).length >= 1)
  check('含金额合计与大写', /300\.00/.test(html) && /[零壹贰叁肆伍陆柒捌玖]/.test(html))
  check('有页码', /第\d+页\s*\/\s*共\d+页/.test(html))

  const outFile = path.join(OUT, `_render_sale-pre-order_${docId}.html`)
  fs.writeFileSync(outFile, html, 'utf8')
  console.log(`\n单据 id：${docId}（单号 ${orderNo}）`)
  console.log(`HTML 已写出：${outFile}`)

  const failed = results.filter(x => !x.ok)
  console.log('')
  if (failed.length) {
    console.log(`✗ ${failed.length}/${results.length} 项未通过：${failed.map(f => f.name).join(' / ')}`)
    process.exit(1)
  }
  console.log(`✓ 全部 ${results.length} 项通过`)
})().catch(e => {
  console.error('✗ 异常：', e.message)
  process.exit(1)
})
