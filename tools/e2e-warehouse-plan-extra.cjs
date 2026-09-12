/* 仓库规划 补充验证（边界 / 业务规则 / 导出内容 / 跨模块引用）
 * 用法：API_PORT=5655 node tools/e2e-warehouse-plan-extra.cjs
 * 产出：tool-results/warehouse-plan-ui/export-warehouse.xlsx、export-location.xlsx（供 python 校验内容）
 */
const http = require('http')
const fs = require('fs')

const API_PORT = Number(process.env.API_PORT || 5655)
const LOGIN_USER = process.env.LOGIN_USER || 'e2e_whplan'
const OUT = 'I:/AI-Ready/tool-results/warehouse-plan-ui'
if (!fs.existsSync(OUT)) fs.mkdirSync(OUT, { recursive: true })

let pass = 0, fail = 0
const bad = []
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✓ ${name}`) } else { fail++; bad.push(name); console.log(`  ✗ ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`) }
}

function rawReq(method, path, body, token, raw) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: API_PORT, path: '/api' + path, method,
      headers: {
        'Content-Type': 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        if (raw) return resolve({ status: res.statusCode, headers: res.headers, buf: Buffer.concat(chunks) })
        const b = Buffer.concat(chunks).toString('utf8')
        try { resolve(JSON.parse(b)) } catch { resolve({ raw: b.slice(0, 200), status: res.statusCode }) }
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

let TOKEN = null
async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  let res = await rawReq('POST', '/auth/login', {
    username: LOGIN_USER, password: 'admin123', tenantName: '系统租户', captcha: code, captchaKey: cap.data.uuid,
  })
  if (!(res?.data?.token || res?.data?.accessToken) && LOGIN_USER !== 'admin') {
    const cap2 = await rawReq('GET', '/auth/captcha')
    const code2 = [...Buffer.from(cap2.data.img.split(',')[1], 'base64').toString('utf8')
      .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
    res = await rawReq('POST', '/auth/login', { username: 'admin', password: 'admin123', tenantName: '系统租户', captcha: code2, captchaKey: cap2.data.uuid })
  }
  TOKEN = res?.data?.token || res?.data?.accessToken || null
  return TOKEN
}

const stamp = String(Date.now()).slice(-6);

(async () => {
  console.log('\n═══ 仓库规划 补充验证 ═══')
  const t = await login()
  check('登录成功', !!t)
  if (!t) process.exit(1)
  const api = (m, p, b) => rawReq(m, p, b, t)

  // ── 1. 查询边界 ──
  console.log('\n【查询边界】')
  const pct = await api('GET', '/erp/warehouse/page?pageNum=1&pageSize=20&keyword=%25')
  check('keyword 含 % 不报错', pct?.code === 200, pct?.msg)
  const und = await api('GET', '/erp/warehouse/page?pageNum=1&pageSize=20&keyword=_')
  check('keyword 含 _ 不报错', und?.code === 200, und?.msg)
  const long = await api('GET', '/erp/warehouse/page?pageNum=1&pageSize=20&keyword=' + encodeURIComponent('长'.repeat(300)))
  check('超长 keyword 不报错', long?.code === 200, long?.msg)
  const overflow = await api('GET', '/erp/warehouse/page?pageNum=999&pageSize=20')
  check('越界页码返回空列表且 total 正确', overflow?.code === 200 && (overflow.data.records || []).length === 0 && Number(overflow.data.total) >= 0, overflow?.data?.total)
  const size1 = await api('GET', '/erp/warehouse/page?pageNum=1&pageSize=1')
  check('pageSize=1 只返回 1 条', size1?.code === 200 && (size1.data.records || []).length === 1)
  const none = await api('GET', '/erp/warehouse/page?pageNum=1&pageSize=20&keyword=' + encodeURIComponent('绝对不存在的仓库XYZ'))
  check('无结果时返回空数组（前端走空态）', none?.code === 200 && (none.data.records || []).length === 0 && Number(none.data.total) === 0)

  // ── 2. 显示层次结构开关 ──
  console.log('\n【显示层次结构】')
  const parent = await api('POST', '/erp/warehouse/save', { warehouseName: `E2E父仓${stamp}`, warehouseCode: `ep${stamp}` })
  const parentId = parent?.data?.id
  const child = await api('POST', '/erp/warehouse/save', { warehouseName: `E2E子仓${stamp}`, warehouseCode: `ec${stamp}`, parentId })
  const childId = child?.data?.id
  check('建立父子仓库', !!parentId && !!childId)

  const hierOn = await api('GET', `/erp/warehouse/page?pageNum=1&pageSize=100&showHierarchy=true&keyword=${encodeURIComponent('E2E')}`)
  const rowsOn = hierOn?.data?.records || []
  check('勾选层次结构：子仓紧随父仓', rowsOn.findIndex(r => String(r.id) === String(childId)) === rowsOn.findIndex(r => String(r.id) === String(parentId)) + 1,
    rowsOn.map(r => r.warehouseName).join(','))

  const hierOff = await api('GET', `/erp/warehouse/page?pageNum=1&pageSize=100&showHierarchy=false&keyword=${encodeURIComponent('E2E')}`)
  check('关闭层次结构：仍返回两条（平铺）', hierOff?.code === 200 && (hierOff.data.records || []).length === 2, hierOff?.data?.total)

  // ── 3. 业务规则：父子 / 分类成环 / 库存保护 ──
  console.log('\n【业务规则】')
  const delParent = await api('DELETE', `/erp/warehouse/${parentId}`)
  check('有下级仓库时禁止删除父仓', delParent?.code !== 200, delParent?.msg)

  const catA = await api('POST', '/erp/warehouse-category', { categoryName: `E2E甲${stamp}` })
  const catB = await api('POST', '/erp/warehouse-category', { categoryName: `E2E乙${stamp}`, parentId: catA?.data?.id })
  const cyc = await api('PUT', `/erp/warehouse-category/${catA?.data?.id}`, { categoryName: `E2E甲${stamp}`, parentId: catB?.data?.id })
  check('分类成环被拒绝（父不能是自己的下级）', cyc?.code !== 200, cyc?.msg)
  const self = await api('PUT', `/erp/warehouse-category/${catA?.data?.id}`, { categoryName: `E2E甲${stamp}`, parentId: catA?.data?.id })
  check('分类上级不能是自己', self?.code !== 200, self?.msg)
  await api('DELETE', `/erp/warehouse-category/${catB?.data?.id}`)
  await api('DELETE', `/erp/warehouse-category/${catA?.data?.id}`)

  const nextCode = await api('GET', '/erp/warehouse/next-code')
  check('next-code 与已有编号不冲突', nextCode?.code === 200 && /^ck\d+$/.test(String(nextCode.data)), nextCode?.data)
  const dupCreate = await api('POST', '/erp/warehouse/save', { warehouseName: '重复编号仓', warehouseCode: String(nextCode.data) })
  check('新建后可立即用该编号（编号未被占用）', dupCreate?.code === 200, dupCreate?.msg)
  if (dupCreate?.data?.id) await api('DELETE', `/erp/warehouse/${dupCreate.data.id}`)

  // ── 4. 导出内容（落盘供 python 校验） ──
  console.log('\n【导出内容】')
  const whExp = await rawReq('GET', `/erp/warehouse/export?keyword=${encodeURIComponent('E2E')}`, null, t, true)
  fs.writeFileSync(`${OUT}/export-warehouse.xlsx`, whExp.buf)
  check('仓库导出落盘（>1KB）', whExp.buf.length > 1000, whExp.buf.length)

  const gen = await api('POST', '/wms/location/generate', {
    warehouseId: parentId, warehouseName: `E2E父仓${stamp}`,
    channelNo: 'A', channelCount: 1, shelfNo: '01', shelfCount: 1, layerCount: 1, columnNo: '01', columnCount: 1,
  })
  check('生成 1 个货位', Number(gen?.data) === 1, gen?.data)
  const locExp = await rawReq('GET', `/wms/location/export?warehouseId=${parentId}`, null, t, true)
  fs.writeFileSync(`${OUT}/export-location.xlsx`, locExp.buf)
  check('货位导出落盘（>1KB）', locExp.buf.length > 1000, locExp.buf.length)

  // ── 5. 跨模块引用 ──
  console.log('\n【跨模块引用】')
  const listAll = await api('GET', '/erp/warehouse/list')
  check('/erp/warehouse/list 正常（其它模块统一入口）', listAll?.code === 200 && Array.isArray(listAll.data) && listAll.data.length > 0, listAll?.data?.length)
  check('list 返回的 status 为整数（停用语义一致）', (listAll?.data || []).every(w => typeof w.status === 'number'), typeof listAll?.data?.[0]?.status)
  const stockWh = await api('GET', '/erp/stock/warehouses')
  check('/erp/stock/warehouses 正常（库存模块入口）', stockWh?.code === 200 || stockWh?.status === 200, stockWh?.code)
  const locByWh = await api('GET', `/wms/location/list-by-warehouse/${parentId}`)
  check('/wms/location/list-by-warehouse 正常（WMS 既有入口未被破坏）', locByWh?.code === 200 && (locByWh.data || []).length === 1, locByWh?.data?.length)
  const locOld = await api('GET', `/wms/location/page?pageNum=1&pageSize=10&warehouseId=${parentId}`)
  check('/wms/location/page 既有分页未被破坏', locOld?.code === 200 && Array.isArray(locOld?.data?.records), locOld?.msg)

  // ── 6. 清理 ──
  console.log('\n【清理】')
  const locList = await api('GET', `/wms/location/plan-page?pageNum=1&pageSize=100&warehouseId=${parentId}&showDisabled=true`)
  const ids = (locList?.data?.records || []).map(r => r.id)
  if (ids.length) await api('POST', '/wms/location/batch-delete', ids)
  const delChild = await api('DELETE', `/erp/warehouse/${childId}`)
  check('删除子仓 200', delChild?.code === 200 && delChild.data === true, delChild)
  const delParent2 = await api('DELETE', `/erp/warehouse/${parentId}`)
  check('子仓删除后父仓可删除', delParent2?.code === 200 && delParent2.data === true, delParent2)
  const finalPage = await api('GET', '/erp/warehouse/page?pageNum=1&pageSize=100&keyword=' + encodeURIComponent('E2E'))
  check('E2E 数据清理干净', Number(finalPage?.data?.total) === 0, finalPage?.data?.total)

  console.log(`\n═══ 结果：${pass}/${pass + fail} 通过 ═══`)
  if (bad.length) console.log('失败项：\n - ' + bad.join('\n - '))
  process.exit(fail === 0 ? 0 : 1)
})().catch(e => { console.error('FATAL', e); process.exit(1) })
