/* 仓库规划 接口层端到端验证（资料 → 仓库管理 → 仓库规划）
 *
 * 后端：API_PORT（默认 5663，本会话独立实例）
 * 覆盖：仓库 CRUD/停用/导出 + 分类树 CRUD + 货位批量生成/停用/批量删除/导出
 * 账号：e2e_whplan（专用验收账号，避免与并行会话共用 admin 互相踢下线）
 */
const http = require('http')

const API_PORT = Number(process.env.API_PORT || 5663)
const LOGIN_USER = process.env.LOGIN_USER || 'e2e_whplan'
const TENANT = '系统租户'

let pass = 0
let fail = 0
const failures = []

function check(name, ok, extra) {
  if (ok) { pass++; console.log(`  ✓ ${name}`) } else {
    fail++; failures.push(name)
    console.log(`  ✗ ${name}${extra ? ' — ' + JSON.stringify(extra).slice(0, 200) : ''}`)
  }
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
        const buf = Buffer.concat(chunks).toString('utf8')
        try { resolve(JSON.parse(buf)) } catch { resolve({ raw: buf.slice(0, 300), status: res.statusCode }) }
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
    username: LOGIN_USER, password: 'admin123', tenantName: TENANT,
    captcha: code, captchaKey: cap.data.uuid,
  })
  if (!(res?.data?.token || res?.data?.accessToken) && LOGIN_USER !== 'admin') {
    const cap2 = await rawReq('GET', '/auth/captcha')
    const code2 = [...Buffer.from(cap2.data.img.split(',')[1], 'base64').toString('utf8')
      .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
    res = await rawReq('POST', '/auth/login', {
      username: 'admin', password: 'admin123', tenantName: TENANT,
      captcha: code2, captchaKey: cap2.data.uuid,
    })
  }
  TOKEN = res?.data?.token || res?.data?.accessToken || null
  return TOKEN
}

const stamp = Date.now().toString().slice(-6);

(async () => {
  console.log('\n═══ 仓库规划 接口验证 ═══')
  const token = await login()
  check('登录成功（专用验收账号）', !!token)
  if (!token) process.exit(1)

  const api = (m, p, b) => rawReq(m, p, b, token)

  // ── 1. 仓库列表 ──
  console.log('\n【仓库主数据】')
  const page0 = await api('GET', '/erp/warehouse/page?pageNum=1&pageSize=20')
  check('仓库分页查询 200 且有数据', page0?.code === 200 && Array.isArray(page0?.data?.records) && page0.data.records.length > 0, page0?.msg)
  const totalBefore = Number(page0?.data?.total || 0)

  const kw = await api('GET', '/erp/warehouse/page?pageNum=1&pageSize=20&keyword=' + encodeURIComponent('主仓库'))
  check('筛选条件（名称模糊）生效', kw?.code === 200 && (kw?.data?.records || []).length >= 1)

  const kwCode = await api('GET', '/erp/warehouse/page?pageNum=1&pageSize=20&keyword=WH001')
  check('筛选条件（编号模糊）生效', kwCode?.code === 200 && (kwCode?.data?.records || []).length >= 1)

  // ── 2. 编号生成 ──
  const nextCode = await api('GET', '/erp/warehouse/next-code')
  check('next-code 返回 ck 前缀编号', nextCode?.code === 200 && /^ck\d+$/.test(String(nextCode.data)), nextCode?.data)

  // ── 3. 新增 ──
  const createRes = await api('POST', '/erp/warehouse/save', {
    warehouseName: `E2E仓库${stamp}`,
    warehouseCode: `e2e${stamp}`,
    contactPerson: '验收员',
    contactPhone: '13800000000',
    address: 'E2E测试地址',
    easyCode: 'E2E',
    zipCode: '730000',
    remark: 'E2E自动化创建',
  })
  const newId = createRes?.data?.id
  check('新增仓库 200 且返回主键', createRes?.code === 200 && !!newId, createRes)

  const detail = await api('GET', `/erp/warehouse/${newId}`)
  check('仓库详情回读字段一致', detail?.code === 200 && detail.data.warehouseName === `E2E仓库${stamp}`
    && detail.data.zipCode === '730000' && detail.data.easyCode === 'E2E', detail?.data)

  // ── 4. 编号唯一校验 ──
  const dup = await api('POST', '/erp/warehouse/save', { warehouseName: '重复编号仓', warehouseCode: `e2e${stamp}` })
  check('仓库编号重复被拒绝', dup?.code !== 200, dup?.msg)

  // ── 5. 数量 +1 ──
  const page1 = await api('GET', '/erp/warehouse/page?pageNum=1&pageSize=20')
  check('列表总数 +1', Number(page1?.data?.total) === totalBefore + 1, { before: totalBefore, after: page1?.data?.total })

  // ── 6. 修改 ──
  const upd = await api('POST', '/erp/warehouse/update', {
    id: newId, warehouseName: `E2E仓库${stamp}改`, warehouseCode: `e2e${stamp}`,
    contactPerson: '验收员2', address: 'E2E新地址', zipCode: '730001',
  })
  check('修改仓库 200', upd?.code === 200 && upd.data === true, upd)
  const detail2 = await api('GET', `/erp/warehouse/${newId}`)
  check('修改后字段落库', detail2?.data?.warehouseName === `E2E仓库${stamp}改` && detail2?.data?.zipCode === '730001')

  // ── 7. 停用 / 显示停用 ──
  const off = await api('POST', `/erp/warehouse/${newId}/status?status=0`)
  check('停用仓库 200', off?.code === 200 && off.data === true, off)
  const pageNoDisabled = await api('GET', '/erp/warehouse/page?pageNum=1&pageSize=50')
  const hasDisabled = (pageNoDisabled?.data?.records || []).some(r => String(r.id) === String(newId))
  check('未勾选「显示停用」时不返回停用仓库', !hasDisabled)
  const pageWithDisabled = await api('GET', '/erp/warehouse/page?pageNum=1&pageSize=50&showDisabled=true')
  const hasDisabled2 = (pageWithDisabled?.data?.records || []).some(r => String(r.id) === String(newId))
  check('勾选「显示停用」后返回停用仓库', hasDisabled2)

  const on = await api('POST', `/erp/warehouse/${newId}/status?status=1`)
  check('启用仓库 200', on?.code === 200 && on.data === true, on)

  // ── 8. 显示层次结构 ──
  const hier = await api('GET', '/erp/warehouse/page?pageNum=1&pageSize=50&showHierarchy=true')
  check('显示层次结构查询 200', hier?.code === 200 && Array.isArray(hier?.data?.records), hier?.msg)

  // ── 9. 导出 ──
  const exp = await rawReq('GET', '/erp/warehouse/export', null, token, true)
  const isXlsx = String(exp.headers['content-type'] || '').includes('spreadsheetml')
  check('仓库导出返回真实 xlsx（PK 头）', exp.status === 200 && isXlsx && exp.buf.length > 1000 && exp.buf.slice(0, 2).toString() === 'PK',
    { status: exp.status, ct: exp.headers['content-type'], size: exp.buf.length })

  // ── 10. 分类树 ──
  console.log('\n【仓库分类树】')
  const catCreate = await api('POST', '/erp/warehouse-category', { categoryName: `E2E分类${stamp}` })
  const catId = catCreate?.data?.id
  check('新增分类 200 且自动生成层级编号', catCreate?.code === 200 && !!catId && !!catCreate.data.categoryCode, catCreate?.data)

  const childCreate = await api('POST', '/erp/warehouse-category', { categoryName: `E2E子分类${stamp}`, parentId: catId })
  const childId = childCreate?.data?.id
  check('新增子分类（层级+1）', childCreate?.code === 200 && childCreate.data.categoryLevel === 2, childCreate?.data)

  const tree = await api('GET', '/erp/warehouse-category/tree')
  const foundNode = JSON.stringify(tree?.data || []).includes(`E2E分类${stamp}`)
  check('分类树包含新增节点', tree?.code === 200 && foundNode)

  const catDel = await api('DELETE', `/erp/warehouse-category/${catId}`)
  check('删除含子分类的父分类被拒绝', catDel?.code !== 200, catDel?.msg)

  const catUpd = await api('PUT', `/erp/warehouse-category/${childId}`, { categoryName: `E2E子分类${stamp}改`, parentId: catId })
  check('修改分类 200', catUpd?.code === 200 && catUpd.data === true, catUpd)

  const childDel = await api('DELETE', `/erp/warehouse-category/${childId}`)
  check('删除子分类 200', childDel?.code === 200 && childDel.data === true, childDel)
  const catDel2 = await api('DELETE', `/erp/warehouse-category/${catId}`)
  check('删除父分类 200', catDel2?.code === 200 && catDel2.data === true, catDel2)

  // ── 11. 仓库挂分类 ──
  const catForWh = await api('POST', '/erp/warehouse-category', { categoryName: `E2E挂靠分类${stamp}` })
  const catForWhId = catForWh?.data?.id
  await api('POST', '/erp/warehouse/update', { id: newId, warehouseCode: `e2e${stamp}`, warehouseName: `E2E仓库${stamp}改`, categoryId: catForWhId })
  const pageByCat = await api('GET', `/erp/warehouse/page?pageNum=1&pageSize=20&categoryId=${catForWhId}`)
  check('按分类过滤仓库生效', pageByCat?.code === 200 && (pageByCat?.data?.records || []).length === 1, pageByCat?.data?.total)

  const catDel3 = await api('DELETE', `/erp/warehouse-category/${catForWhId}`)
  check('分类被仓库引用时删除被拒绝', catDel3?.code !== 200, catDel3?.msg)

  // ── 12. 货位 ──
  console.log('\n【货位（2.货位）】')
  const gen = await api('POST', '/wms/location/generate', {
    warehouseId: newId, warehouseName: `E2E仓库${stamp}改`,
    channelNo: 'A', channelCount: 1, shelfNo: '01', shelfCount: 2,
    layerCount: 2, columnNo: '01', columnCount: 2, remark: 'E2E生成',
  })
  const genCount = Number(gen?.data ?? 0)
  check('批量生成货位 = 通道×货架×层×列 = 1×2×2×2 = 8', gen?.code === 200 && genCount === 8, gen)

  const locPage = await api('GET', `/wms/location/plan-page?pageNum=1&pageSize=50&warehouseId=${newId}`)
  const locRows = locPage?.data?.records || []
  check('货位列表返回 8 条', locPage?.code === 200 && locRows.length === 8, { total: locPage?.data?.total })
  check('货位编号格式 A1-101', locRows.some(r => r.locationCode === 'A1-101'), locRows.map(r => r.locationCode).slice(0, 4))
  check('货位带所属仓库名称快照', locRows.every(r => r.warehouseName), locRows[0]?.warehouseName)

  // A1-2xx = 货架 1 的第 2 层（2 列），共 2 条
  const locFilter = await api('GET', `/wms/location/plan-page?pageNum=1&pageSize=50&locationCode=A1-2`)
  check('货位编号模糊查询生效', locFilter?.code === 200 && (locFilter?.data?.records || []).length === 2
    && (locFilter?.data?.records || []).every(r => String(r.locationCode).includes('A1-2')), locFilter?.data?.total)

  const locOne = locRows[0]
  const locUpd = await api('POST', '/wms/location/update', { id: locOne.id, remark: 'E2E备注修改' })
  check('货位备注修改 200', locUpd?.code === 200 && locUpd.data === true, locUpd)

  const locOff = await api('POST', `/wms/location/${locOne.id}/enabled?isEnabled=0`)
  check('停用货位 200', locOff?.code === 200 && locOff.data === true, locOff)
  const locPageNoDisabled = await api('GET', `/wms/location/plan-page?pageNum=1&pageSize=50&warehouseId=${newId}`)
  check('未勾选「显示停用」时不返回停用货位', (locPageNoDisabled?.data?.records || []).length === 7, locPageNoDisabled?.data?.total)
  const locPageDisabled = await api('GET', `/wms/location/plan-page?pageNum=1&pageSize=50&warehouseId=${newId}&showDisabled=true`)
  check('勾选「显示停用」后返回 8 条', (locPageDisabled?.data?.records || []).length === 8, locPageDisabled?.data?.total)

  const locExp = await rawReq('GET', `/wms/location/export?warehouseId=${newId}`, null, token, true)
  check('货位导出返回真实 xlsx（PK 头）', locExp.status === 200 && String(locExp.headers['content-type'] || '').includes('spreadsheetml')
    && locExp.buf.slice(0, 2).toString() === 'PK', { status: locExp.status, size: locExp.buf.length })

  const batchIds = (locPageDisabled?.data?.records || []).map(r => r.id)
  const batchDel = await api('POST', '/wms/location/batch-delete', batchIds.slice(0, 3))
  check('批量删除货位 3 条', batchDel?.code === 200 && Number(batchDel.data) === 3, batchDel)
  const locAfterBatch = await api('GET', `/wms/location/plan-page?pageNum=1&pageSize=50&warehouseId=${newId}&showDisabled=true`)
  check('批量删除后剩 5 条', (locAfterBatch?.data?.records || []).length === 5, locAfterBatch?.data?.total)

  const locDelOne = await api('DELETE', `/wms/location/${(locAfterBatch?.data?.records || [])[0].id}`)
  check('单条删除货位 200', locDelOne?.code === 200, locDelOne)

  // ── 13. 删除仓库（清理） ──
  console.log('\n【清理】')
  const restLoc = await api('GET', `/wms/location/plan-page?pageNum=1&pageSize=50&warehouseId=${newId}&showDisabled=true`)
  const restIds = (restLoc?.data?.records || []).map(r => r.id)
  if (restIds.length) await api('POST', '/wms/location/batch-delete', restIds)
  const whDel = await api('DELETE', `/erp/warehouse/${newId}`)
  check('删除仓库 200', whDel?.code === 200 && whDel.data === true, whDel)
  const catDel4 = await api('DELETE', `/erp/warehouse-category/${catForWhId}`)
  check('仓库删除后分类可删除', catDel4?.code === 200 && catDel4.data === true, catDel4)

  const pageFinal = await api('GET', '/erp/warehouse/page?pageNum=1&pageSize=20')
  check('数据回到初始总数', Number(pageFinal?.data?.total) === totalBefore, { before: totalBefore, after: pageFinal?.data?.total })

  console.log(`\n═══ 结果：${pass}/${pass + fail} 通过 ═══`)
  if (failures.length) console.log('失败项：\n - ' + failures.join('\n - '))
  process.exit(fail === 0 ? 0 : 1)
})().catch(e => { console.error('FATAL', e); process.exit(1) })
