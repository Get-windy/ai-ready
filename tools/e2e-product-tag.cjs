/* 商品标签标准化验收：标准槽位 TAG_N + 用户自定义昵称
 * 运行：node tools/e2e-product-tag.cjs [port]
 * 覆盖：
 *   1) 标签字典 = 20 个标准槽位（TAG_1..TAG_20），昵称可自定义
 *   2) 改昵称不影响商品关联（商品侧存槽位编码）
 *   3) 商品 mall_tags 存编码并回读一致
 *   4) 「商品辅助资料 → 商品标签」的「对应商品」聚合按槽位匹配
 */
const http = require('http')

const PORT = Number(process.argv[2] || 5678)
const results = []
let token = ''

function apiReq(method, p, body) {
  return new Promise((resolve, reject) => {
    const data = body == null ? null : JSON.stringify(body)
    const headers = { 'Content-Type': 'application/json' }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    if (token) headers.Authorization = `Bearer ${token}`
    const r = http.request({ hostname: '127.0.0.1', port: PORT, path: '/api' + p, method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        try { resolve(JSON.parse(Buffer.concat(chunks).toString('utf8'))) }
        catch { resolve({ raw: true, status: res.statusCode }) }
      })
    })
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
    username: process.env.E2E_USER || 'e2e_product', password: process.env.E2E_PASS || 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  token = res?.data?.token || res?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
}

function check(name, ok, detail) {
  results.push({ name, ok })
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail !== undefined ? '  — ' + detail : ''}`)
}

const EXPECT_CODES = Array.from({ length: 20 }, (_, i) => `TAG_${i + 1}`)

async function main() {
  await login()
  console.log('登录成功\n')

  // ═══ A. 标准槽位字典 ═══
  const r1 = await apiReq('GET', '/erp/mall-tag/list')
  const tags = r1.data || []
  check('A1 标签字典返回 20 个标准槽位', tags.length === 20, `实际 ${tags.length}`)
  check('A2 槽位编码为 TAG_1..TAG_20 且升序',
    JSON.stringify(tags.map(t => t.tagCode)) === JSON.stringify(EXPECT_CODES),
    tags.map(t => t.tagCode).join(','))
  check('A3 每个槽位都有显示昵称', tags.every(t => !!t.tagName), tags.slice(0, 3).map(t => t.tagName).join('/'))
  const slot3 = tags.find(t => t.tagCode === 'TAG_3')
  const oldName = slot3?.tagName

  // ═══ B. 改昵称 ═══
  const stamp = Date.now()
  const newName = `昵称-${stamp % 10000}`
  const upd = await apiReq('PUT', `/erp/mall-tag/${slot3.id}`, { tagName: newName })
  const r2 = await apiReq('GET', '/erp/mall-tag/list')
  const tags2 = r2.data || []
  check('B1 修改标签昵称成功', upd.code === 200, JSON.stringify(upd).slice(0, 100))
  check('B2 改后槽位编码不变、昵称更新',
    tags2.find(t => t.tagCode === 'TAG_3')?.tagName === newName &&
    JSON.stringify(tags2.map(t => t.tagCode)) === JSON.stringify(EXPECT_CODES),
    tags2.find(t => t.tagCode === 'TAG_3')?.tagName)

  // ═══ C. 商品按槽位编码关联 ═══
  const catTree = await apiReq('GET', '/erp/product-category/tree')
  const flat = []
  const walk = (ns) => (ns || []).forEach(n => { flat.push(n); walk(n.children) })
  walk(catTree.data)
  const categoryId = flat[0]?.id

  const created = await apiReq('POST', '/erp/product/batch-create', {
    product: {
      productName: `E2E标签-${stamp}`, productCodeAlias: `E2ETAG${stamp}`,
      categoryId, industryCategory: '其他', productType: 'SINGLE',
      mallTags: 'TAG_1,TAG_3',
    },
    units: [{ unitName: '个', unitType: 'SMALL', isBaseUnit: 1, conversionRate: 1, sortOrder: 1 }],
  })
  const pid = created?.data?.productId
  check('C1 商品保存带标签（槽位编码）', created.code === 200 && !!pid, `productId=${pid}`)

  const page = await apiReq('GET', `/erp/product/page?pageNum=1&pageSize=20&keyword=${encodeURIComponent(`E2E标签-${stamp}`)}`)
  const row = (page?.data?.records || [])[0] || {}
  check('C2 商品 mall_tags 存的就是槽位编码', String(row.mallTags || '') === 'TAG_1,TAG_3', String(row.mallTags))

  // ═══ D. 「对应商品」按槽位聚合（改昵称后仍命中）═══
  const tagPage = await apiReq('GET', `/erp/mall-tag/page?pageNum=1&pageSize=50`)
  const vo3 = (tagPage?.data?.records || []).find(v => v.tagCode === 'TAG_3') || {}
  check('D1 改名后 TAG_3 的「对应商品」仍命中该商品',
    (vo3.productNames || '').includes(`E2E标签-${stamp}`),
    `count=${vo3.productCount} names=${(vo3.productNames || '').slice(0, 60)}`)

  // ═══ E. 还原 + 清理 ═══
  await apiReq('PUT', `/erp/mall-tag/${slot3.id}`, { tagName: oldName })
  const r3 = await apiReq('GET', '/erp/mall-tag/list')
  check('E1 昵称还原', (r3.data || []).find(t => t.tagCode === 'TAG_3')?.tagName === oldName)
  const del = await apiReq('DELETE', `/erp/product/${pid}`)
  check('E2 清理测试商品', del.code === 200)

  const pass = results.filter(r => r.ok).length
  console.log(`\n===== 商品标签标准化验收 ${pass}/${results.length} =====`)
  if (pass !== results.length) process.exit(1)
}

main().catch(e => { console.error('FATAL', e); process.exit(1) })
