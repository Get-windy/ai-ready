/* 商品「新增表单 · 商品单位」验收脚本
 * 运行：node tools/e2e-product-unit.cjs [port]
 * 覆盖：
 *   1) 价格等级标准槽位（GRADE_1..8，接口升序返回，昵称可自定义）
 *   2) 商品单位 8 个等级价 ↔ grade_price_1..8 列严格对应
 *   3) 商品单位「重量（kg）」「体积（m³）」保存与回读（V11.154.0）
 *   4) 单位类型（小/中/大）与换算关系闭环
 */
const http = require('http')

const PORT = Number(process.argv[2] || 5677)
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

const EXPECT_CODES = ['GRADE_1', 'GRADE_2', 'GRADE_3', 'GRADE_4', 'GRADE_5', 'GRADE_6', 'GRADE_7', 'GRADE_8']

async function main() {
  await login()
  console.log('登录成功\n')

  // ═══ A. 价格等级标准槽位 ═══
  const gr = await apiReq('GET', '/erp/product-grade/list')
  const grades = gr.data || []
  check('A1 价格等级接口返回 8 条', grades.length === 8, `实际 ${grades.length}`)
  check('A2 等级编码为标准槽位 GRADE_1..8 且升序',
    JSON.stringify(grades.map(g => g.gradeCode)) === JSON.stringify(EXPECT_CODES),
    grades.map(g => g.gradeCode).join(','))
  check('A3 等级名称（用户自定义昵称）非空', grades.every(g => !!g.gradeName),
    grades.map(g => g.gradeName).join('/'))

  // ═══ B. 新增商品 + 3 个单位（含等级价 / 重量 / 体积）═══
  const stamp = Date.now()
  const catTree = await apiReq('GET', '/erp/product-category/tree')
  const flat = []
  const walk = (ns) => (ns || []).forEach(n => { flat.push(n); walk(n.children) })
  walk(catTree.data)
  const categoryId = flat[0]?.id

  const units = [
    {
      unitName: '袋', unitType: 'SMALL', isBaseUnit: 1, conversionRate: 1, barcode: `U${stamp}1`,
      sortOrder: 1, presetPurchasePrice: 10, referenceCost: 9, recentPurchasePrice: 10.5,
      wholesalePrice: 12, retailPrice: 15, minSalePrice: 13, minDiscount: 95,
      gradePrice1: 15.1, gradePrice2: 15.2, gradePrice3: 15.3, gradePrice4: 15.4,
      gradePrice5: 15.5, gradePrice6: 15.6, gradePrice7: 15.7, gradePrice8: 15.8,
      weight: 0.5, volume: 0.0012,
    },
    {
      unitName: '箱', unitType: 'MEDIUM', isBaseUnit: 0, conversionRate: 4, barcode: `U${stamp}2`,
      sortOrder: 2, presetPurchasePrice: 38, retailPrice: 56, wholesalePrice: 46,
      gradePrice1: 56.1, gradePrice2: 56.2, gradePrice3: 56.3, gradePrice4: 56.4,
      gradePrice5: 56.5, gradePrice6: 56.6, gradePrice7: 56.7, gradePrice8: 56.8,
      weight: 2, volume: 0.0048,
    },
    {
      unitName: '托盘', unitType: 'LARGE', isBaseUnit: 0, conversionRate: 96, barcode: `U${stamp}3`,
      sortOrder: 3, presetPurchasePrice: 880, retailPrice: 1280,
      weight: 48, volume: 0.12,
    },
    {
      // 第 4 个槽位：类型 UNIT_4（前端「单位4」），验证扩展槽位落库回读
      unitName: '整车', unitType: 'UNIT_4', isBaseUnit: 0, conversionRate: 960, barcode: `U${stamp}4`,
      sortOrder: 4, presetPurchasePrice: 8800, retailPrice: 12800,
      weight: 480, volume: 1.2,
    },
  ]

  const created = await apiReq('POST', '/erp/product/batch-create', {
    product: {
      productName: `E2E单位-${stamp}`, productCodeAlias: `E2EU${stamp}`,
      categoryId, industryCategory: '其他', productType: 'SINGLE',
      isStandardProduct: 1, defaultSalesUnitId: null,
      weight: 1, volume: 1,
    },
    units,
  })
  const pid = created?.data?.productId
  check('B1 新增商品(含单位)成功', created.code === 200 && !!pid, `productId=${pid}`)

  // 单位保存后把常用单位指到小单位
  const form1 = await apiReq('GET', `/erp/product/${pid}/form`)
  const savedUnits = form1?.data?.units || []
  check('B2 表单接口返回 4 个单位', savedUnits.length === 4,
    savedUnits.map(u => `${u.unitName}/${u.unitType}`).join(','))
  const base = savedUnits.find(u => u.unitName === '袋') || {}
  const box = savedUnits.find(u => u.unitName === '箱') || {}
  const tray = savedUnits.find(u => u.unitName === '托盘') || {}
  const truck = savedUnits.find(u => u.unitName === '整车') || {}

  check('B2b 第 4 槽位类型按扩展槽位 UNIT_4 落库', truck.unitType === 'UNIT_4', String(truck.unitType))
  check('B2c 第 4 槽位换算关系/重量/体积落库',
    Number(truck.conversionRate) === 960 && Number(truck.weight) === 480 && Number(truck.volume) === 1.2,
    `${truck.conversionRate}/${truck.weight}/${truck.volume}`)

  check('B3 单位「重量（kg）」落库回读', base.weight != null && Number(base.weight) === 0.5,
    `袋=${base.weight} 箱=${box.weight} 托盘=${tray.weight}`)
  check('B4 单位「体积（m³）」落库回读', base.volume != null && Number(base.volume) === 0.0012,
    `袋=${base.volume} 箱=${box.volume} 托盘=${tray.volume}`)
  check('B5 大单位重量/体积独立于小单位', Number(tray.weight) === 48 && Number(tray.volume) === 0.12,
    `托盘=${tray.weight}kg/${tray.volume}m³`)

  // ═══ C. 8 个等级价与标准槽位严格对应 ═══
  const expectBag = [15.1, 15.2, 15.3, 15.4, 15.5, 15.6, 15.7, 15.8]
  const gotBag = [1, 2, 3, 4, 5, 6, 7, 8].map(i => Number(base[`gradePrice${i}`]))
  check('C1 袋(基本单位) 8 个等级价逐槽位一致',
    JSON.stringify(gotBag) === JSON.stringify(expectBag), gotBag.join(','))

  const expectBox = [56.1, 56.2, 56.3, 56.4, 56.5, 56.6, 56.7, 56.8]
  const gotBox = [1, 2, 3, 4, 5, 6, 7, 8].map(i => Number(box[`gradePrice${i}`]))
  check('C2 箱(换算单位) 8 个等级价逐槽位一致',
    JSON.stringify(gotBox) === JSON.stringify(expectBox), gotBox.join(','))

  // ═══ D. 列表 gradePriceMap 以 GRADE_N 为键（基本单位回退口径）═══
  const page = await apiReq('GET', `/erp/product/page?pageNum=1&pageSize=50&keyword=${encodeURIComponent(`E2E单位-${stamp}`)}`)
  const row = (page?.data?.records || [])[0] || {}
  const gpm = row.gradePriceMap || {}
  check('D1 列表 gradePriceMap 键为标准 GRADE_1..8',
    EXPECT_CODES.every(c => c in gpm), JSON.stringify(gpm))
  check('D2 列表等级价与基本单位一致',
    Number(gpm.GRADE_1) === 15.1 && Number(gpm.GRADE_8) === 15.8,
    `G1=${gpm.GRADE_1} G8=${gpm.GRADE_8}`)

  // ═══ E. 编辑：改重量/体积 + 等级价 ═══
  const editUnits = savedUnits.map(u => ({ ...u, weight: u.unitName === '箱' ? 2.2 : u.weight, volume: u.unitName === '箱' ? 0.005 : u.volume }))
  const boxIdx = editUnits.findIndex(u => u.unitName === '箱')
  editUnits[boxIdx].gradePrice8 = 66.8
  const upd = await apiReq('PUT', `/erp/product/batch-update/${pid}`, {
    product: { ...row, id: pid },
    units: editUnits,
  })
  check('E1 编辑商品(改重量/体积/等级价)成功', upd.code === 200, JSON.stringify(upd).slice(0, 120))

  const form2 = await apiReq('GET', `/erp/product/${pid}/form`)
  const box2 = (form2?.data?.units || []).find(u => u.unitName === '箱') || {}
  check('E2 编辑后重量/体积回读', Number(box2.weight) === 2.2 && Number(box2.volume) === 0.005,
    `${box2.weight}kg/${box2.volume}m³`)
  check('E3 编辑后第 8 槽位等级价回读', Number(box2.gradePrice8) === 66.8, `G8=${box2.gradePrice8}`)

  // ═══ F. 自定义昵称不影响槽位 ═══
  const g1 = grades[0]
  await apiReq('PUT', `/erp/product-grade/${g1.id}`, { gradeName: `昵称-${stamp % 1000}` })
  const gr2 = await apiReq('GET', '/erp/product-grade/list')
  const grades2 = gr2.data || []
  check('F1 改昵称后编码顺序不变',
    JSON.stringify(grades2.map(g => g.gradeCode)) === JSON.stringify(EXPECT_CODES),
    grades2.map(g => `${g.gradeCode}:${g.gradeName}`).join(',').slice(0, 120))
  await apiReq('PUT', `/erp/product-grade/${g1.id}`, { gradeName: g1.gradeName })

  // ═══ G. 清理 ═══
  const del = await apiReq('DELETE', `/erp/product/${pid}`)
  const after = await apiReq('GET', `/erp/product/${pid}/form`)
  check('G1 清理测试商品', del.code === 200 && after.code !== 200, `delete=${del.code} after=${after.code}`)

  const pass = results.filter(r => r.ok).length
  console.log(`\n===== 商品单位/价格等级验收 ${pass}/${results.length} =====`)
  if (pass !== results.length) process.exit(1)
}

main().catch(e => { console.error('FATAL', e); process.exit(1) })
