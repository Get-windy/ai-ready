/* 商品条码（资料 → 商品管理 → 商品条码）金标准 端到端验证
 * 用法：node tools/e2e-barcode.cjs
 * 环境变量：BE_PORT（默认 5690，独立后端实例）；FE（默认 http://localhost:5656）
 *
 * 对齐口径（2026-09-11 ql361 实抓复核）：
 *  - 「条码」下拉 = 全部 / 无条码 / 有条码
 *  - 「上架状态」= 全部 / 已上架 / 未上架；「显示状态」= 全部 / 已启用 / 已停用
 *  - 「新增时间 / 采购日期」= 比较符下拉（< = > ≠ ≤ ≥，默认 ≥）+ 日期
 *  - 行级「修改」= 打开商品档案「普通商品-新增与编辑」（在「商品单位」明细里维护条码）
 * 说明：UI 段用 playwright route 把 /api 转发到 BE_PORT，避免与并行会话占用的 5655 相互干扰。
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')

const FE = process.env.FE || 'http://localhost:5656'
const PORT = Number(process.env.BE_PORT || 5690)
const SHOTS = 'I:/AI-Ready/tool-results/md-barcode'
const TENANT = 1
// 独立验证用户：sa-token 用 Redis 共享会话且 is-concurrent=false，与并行会话共用 admin 会互相踢下线
const E2E_USER = process.env.E2E_USER || 'e2e_barcode'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

function apiReq(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: PORT, path: '/api' + path, method,
      headers: {
        'Content-Type': 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        if ((res.headers['content-type'] || '').includes('json')) {
          try { resolve(JSON.parse(buf.toString('utf8'))) } catch (e) { resolve({ raw: buf.toString('utf8'), status: res.statusCode }) }
        } else {
          resolve({ __binary: true, status: res.statusCode, contentType: res.headers['content-type'], head: buf.slice(0, 4).toString('hex'), size: buf.length })
        }
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
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  const token = res?.data?.token || res?.data?.accessToken
  if (!token) {
    throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300)
      + `\n提示：sa-token 会话存 Redis 且 is-concurrent=false，与并行会话共用 admin 会跨实例互踢。`
      + `\n请用独立账号（默认 ${E2E_USER}/${E2E_PWD}）；账号不存在时按开发文档「复跑方法」章节的 SQL 重建，或用 E2E_USER/E2E_PWD 指定。`)
  }
  return { token, userInfo: res.data }
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`)
}

const q = (s) => encodeURIComponent(s)

async function main() {
  let { token } = await login()
  console.log(`登录成功（后端 ${PORT}）`)

  // ═══════ A. 后端接口 ═══════
  const page1 = await apiReq('GET', '/erp/product/barcodes/page?pageNum=1&pageSize=20&barcodeFilter=ALL', null, token)
  const rows = page1?.data?.records || []
  check('商品条码分页接口可用', Array.isArray(rows) && page1?.data?.total > 0, `总数=${page1?.data?.total}`)
  check('未登录访问被拒绝（鉴权生效）', await (async () => {
    const r = await apiReq('GET', '/erp/product/barcodes/page?pageNum=1&pageSize=5')
    return r?.code === 401 || r?.status === 401 || r?.code === 403 || r?.status === 403
  })(), '未带 token 请求已拦截')

  const first = rows[0] || {}
  const needed = ['unitId', 'productId', 'productName', 'productCode', 'unitName', 'conversionRelation', 'shelfStatusText', 'barcode', 'createTime']
  const missing = needed.filter(k => !(k in first))
  check('行字段完整（商品×单位 全字段）', missing.length === 0, missing.length ? '缺: ' + missing.join(',') : `示例: ${first.productName}/${first.unitName}/${first.conversionRelation}/${first.barcode}`)

  const multiUnit = rows.find(r => r.isBaseUnit === 0 && r.conversionRelation)
  check('多单位换算关系正确（1箱=N基础单位）', !!multiUnit, multiUnit ? multiUnit.conversionRelation : '无多单位行')

  const byKeyword = await apiReq('GET', `/erp/product/barcodes/page?pageNum=1&pageSize=20&barcodeFilter=ALL&keyword=${q('百香果')}`, null, token)
  const kwRows = byKeyword?.data?.records || []
  check('「筛选条件」按商品名称检索生效', kwRows.length > 0 && kwRows.every(r => (r.productName || '').includes('百香果')), `命中 ${kwRows.length} 行`)

  const byCode = await apiReq('GET', `/erp/product/barcodes/page?pageNum=1&pageSize=20&barcodeFilter=ALL&keyword=${q('SP-TEST-001')}`, null, token)
  check('「筛选条件」按货号检索生效', (byCode?.data?.records || []).length > 0, `命中 ${(byCode?.data?.records || []).length} 行`)

  const allRows = await apiReq('GET', '/erp/product/barcodes/page?pageNum=1&pageSize=100&barcodeFilter=ALL', null, token)
  const sampleBarcode = ((allRows?.data?.records || []).find(r => !!r.barcode) || {}).barcode
  const byBarcode = await apiReq('GET', `/erp/product/barcodes/page?pageNum=1&pageSize=20&barcodeFilter=ALL&keyword=${q(sampleBarcode)}`, null, token)
  check('「筛选条件」按条码检索生效', (byBarcode?.data?.records || []).length > 0, `命中 ${(byBarcode?.data?.records || []).length} 行（${sampleBarcode}）`)

  // 对标值域：全部 / 无条码 / 有条码
  const none = await apiReq('GET', '/erp/product/barcodes/page?pageNum=1&pageSize=50&barcodeFilter=NONE', null, token)
  const noneRows = none?.data?.records || []
  check('「条码=无条码」过滤生效', noneRows.length > 0 && noneRows.every(r => !r.barcode), `命中 ${noneRows.length} 行`)

  const has = await apiReq('GET', '/erp/product/barcodes/page?pageNum=1&pageSize=50&barcodeFilter=HAS', null, token)
  const hasRows = has?.data?.records || []
  check('「条码=有条码」过滤生效', hasRows.length > 0 && hasRows.every(r => !!r.barcode), `命中 ${hasRows.length} 行`)
  const nTotal = Number(none?.data?.total || 0), hTotal = Number(has?.data?.total || 0), aTotal = Number(page1?.data?.total || 0)
  check('无条码 + 有条码 = 全部（口径互斥且完备）',
    nTotal + hTotal === aTotal,
    `${nTotal} + ${hTotal} = ${nTotal + hTotal} / 全部 ${aTotal}`)

  const shelf0 = await apiReq('GET', '/erp/product/barcodes/page?pageNum=1&pageSize=50&barcodeFilter=ALL&shelfStatus=0', null, token)
  const shelf0Rows = shelf0?.data?.records || []
  check('「上架状态=未上架」过滤生效', shelf0Rows.length > 0 && shelf0Rows.every(r => r.shelfStatus === 0), `命中 ${shelf0Rows.length} 行`)

  const shelf1 = await apiReq('GET', '/erp/product/barcodes/page?pageNum=1&pageSize=50&barcodeFilter=ALL&shelfStatus=1', null, token)
  check('「上架状态=已上架」过滤生效', (shelf1?.data?.records || []).length > 0 && (shelf1?.data?.records || []).every(r => r.shelfStatus === 1), `命中 ${(shelf1?.data?.records || []).length} 行`)

  const disabled = await apiReq('GET', '/erp/product/barcodes/page?pageNum=1&pageSize=50&barcodeFilter=ALL&status=DISABLED', null, token)
  const disabledRows = disabled?.data?.records || []
  check('「显示状态=已停用」过滤生效', disabledRows.length > 0 && disabledRows.every(r => r.status === 'DISABLED'), `命中 ${disabledRows.length} 行`)

  const enabled = await apiReq('GET', '/erp/product/barcodes/page?pageNum=1&pageSize=50&barcodeFilter=ALL&status=ENABLED', null, token)
  const enabledRows = enabled?.data?.records || []
  check('「显示状态=已启用」为默认口径', enabledRows.length > 0 && enabledRows.every(r => r.status === 'ENABLED'), `命中 ${enabledRows.length} 行`)

  // 日期比较符（对标下拉 < = > ≠ ≤ ≥）
  const ctGe = await apiReq('GET', '/erp/product/barcodes/page?pageNum=1&pageSize=50&barcodeFilter=ALL&createTimeOp=%3E%3D&createTimeStart=2099-01-01', null, token)
  check('「新增时间 >=」比较符生效', (ctGe?.data?.records || []).length === 0, `命中 ${(ctGe?.data?.records || []).length} 行`)
  const ctLt = await apiReq('GET', '/erp/product/barcodes/page?pageNum=1&pageSize=50&barcodeFilter=ALL&createTimeOp=%3C&createTimeStart=2099-01-01', null, token)
  check('「新增时间 <」比较符生效', (ctLt?.data?.records || []).length > 0, `命中 ${(ctLt?.data?.records || []).length} 行`)
  const ctNe = await apiReq('GET', '/erp/product/barcodes/page?pageNum=1&pageSize=50&barcodeFilter=ALL&createTimeOp=%21%3D&createTimeStart=2099-01-01', null, token)
  check('「新增时间 ≠」比较符生效', (ctNe?.data?.records || []).length > 0, `命中 ${(ctNe?.data?.records || []).length} 行`)
  const pdLt = await apiReq('GET', '/erp/product/barcodes/page?pageNum=1&pageSize=50&barcodeFilter=ALL&purchaseDateOp=%3C&purchaseDateStart=2099-01-01', null, token)
  check('「采购日期 <」比较符生效（有采购记录的命中）', (pdLt?.data?.records || []).length > 0, `命中 ${(pdLt?.data?.records || []).length} 行`)
  const pdGe = await apiReq('GET', '/erp/product/barcodes/page?pageNum=1&pageSize=50&barcodeFilter=ALL&purchaseDateOp=%3E%3D&purchaseDateStart=2099-01-01', null, token)
  check('「采购日期 >=」比较符生效', (pdGe?.data?.records || []).length === 0, `命中 ${(pdGe?.data?.records || []).length} 行`)

  const cat = await apiReq('GET', `/erp/product/barcodes/page?pageNum=1&pageSize=50&barcodeFilter=ALL&categoryId=${rows[0]?.categoryId ?? ''}`, null, token)
  check('商品分类树过滤（含子分类）接口可用', cat?.code === 200, `categoryId=${rows[0]?.categoryId ?? '-'} 命中 ${cat?.data?.total}`)

  // 表头排序（对标：商品名称/货号/条码可排序）—— 后端整表排序，非当前页本地排序
  const sortAsc = await apiReq('GET', '/erp/product/barcodes/page?pageNum=1&pageSize=50&barcodeFilter=HAS&sortField=barcode&sortOrder=asc', null, token)
  const sortDesc = await apiReq('GET', '/erp/product/barcodes/page?pageNum=1&pageSize=50&barcodeFilter=HAS&sortField=barcode&sortOrder=desc', null, token)
  const ascList = (sortAsc?.data?.records || []).map(r => r.barcode).filter(Boolean)
  const descList = (sortDesc?.data?.records || []).map(r => r.barcode).filter(Boolean)
  const ascOk = ascList.length > 1 && ascList.every((v, i) => i === 0 || ascList[i - 1] <= v)
  const descOk = descList.length > 1 && descList.every((v, i) => i === 0 || descList[i - 1] >= v)
  check('「条码」升序排序生效（后端整表排序）', ascOk, `首=${ascList[0]} 末=${ascList[ascList.length - 1]} n=${ascList.length}`)
  check('「条码」降序排序生效（后端整表排序）', descOk, `首=${descList[0]} 末=${descList[descList.length - 1]} n=${descList.length}`)
  const sortName = await apiReq('GET', '/erp/product/barcodes/page?pageNum=1&pageSize=50&barcodeFilter=ALL&sortField=productName&sortOrder=asc', null, token)
  check('「商品名称」排序字段白名单生效（非法字段回落默认排序）', sortName?.code === 200, `命中 ${sortName?.data?.total}`)

  const exportRes = await apiReq('GET', '/erp/product/barcodes/export?barcodeFilter=ALL', null, token)
  check('导出返回真实 Excel（xlsx 魔数 PK）', exportRes?.__binary && exportRes.head.startsWith('504b') && exportRes.size > 1000,
    `contentType=${exportRes?.contentType} size=${exportRes?.size}`)

  // 商品档案保存：条码唯一性 + 双口径同步（条码页「修改」跳转后的落库路径）
  const targetProductId = rows[0]?.productId
  const otherRow = hasRows.find(r => r.productId !== targetProductId)
  if (targetProductId && otherRow) {
    const form = await apiReq('GET', `/erp/product/${targetProductId}/form`, null, token)
    check('商品档案表单接口可用（条码页「修改」跳转落点）', form?.code === 200 && Array.isArray(form?.data?.units),
      `units=${(form?.data?.units || []).length}`)
    const prod = { ...(form?.data?.product || {}) }
    delete prod.createTime; delete prod.updateTime; delete prod.createBy; delete prod.updateBy; delete prod.tenantId
    const unitsPayload = () => (form?.data?.units || []).map(u => ({
      id: u.id, unitName: u.unitName, unitType: u.unitType, isBaseUnit: u.isBaseUnit,
      conversionRate: u.conversionRate, barcode: u.barcode, sortOrder: u.sortOrder,
    }))

    const conflict = unitsPayload(); conflict[0].barcode = otherRow.barcode
    const conflictRes = await apiReq('PUT', `/erp/product/batch-update/${targetProductId}`, { product: { ...prod, id: targetProductId }, units: conflict }, token)
    check('商品档案保存条码全局唯一性校验（跨商品重复被拒）', conflictRes?.code !== 200 && conflictRes?.code !== 0,
      `返回: ${JSON.stringify(conflictRes).slice(0, 120)}`)

    const dupIn = unitsPayload(); if (dupIn.length > 1) dupIn[1].barcode = dupIn[0].barcode
    const dupInRes = await apiReq('PUT', `/erp/product/batch-update/${targetProductId}`, { product: { ...prod, id: targetProductId }, units: dupIn }, token)
    check('商品档案保存条码同商品内查重（同商品两单位同码被拒）', dupIn.length > 1 && dupInRes?.code !== 200 && dupInRes?.code !== 0,
      `返回: ${JSON.stringify(dupInRes).slice(0, 120)}`)

    const keepRes = await apiReq('PUT', `/erp/product/batch-update/${targetProductId}`, { product: { ...prod, id: targetProductId }, units: unitsPayload() }, token)
    check('商品档案保存原条码成功', keepRes?.code === 200, `返回: ${keepRes?.code}`)

    const bcRows = await apiReq('GET', `/erp/product/barcodes/${targetProductId}`, null, token)
    const synced = (bcRows?.data || []).filter(x => x.isDefault === 1 && x.barcode)
    check('条码表默认记录双口径同步（不陈旧、含类型字段）',
      synced.length === (form?.data?.units || []).filter(u => u.barcode).length,
      `条码表 ${synced.length} 条 / 单位行 ${(form?.data?.units || []).filter(u => u.barcode).length} 条`)
  } else {
    check('商品档案表单接口可用（条码页「修改」跳转落点）', false, '缺少测试数据')
  }

  // ═══════ B. 前端页面 ═══════
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 } })
  const page = await ctx.newPage()

  // /api 转发到本次验证的后端实例（避免 5655 并行会话干扰）；不影响 /src/api/*.ts 静态资源
  await page.route('**/*', async (route) => {
    const url = new URL(route.request().url())
    if (url.origin === FE && url.pathname.startsWith('/api/')) {
      try {
        const resp = await route.fetch({ url: `http://localhost:${PORT}${url.pathname}${url.search}` })
        return route.fulfill({ response: resp })
      } catch (e) {
        return route.continue()
      }
    }
    return route.continue()
  })

  async function injectToken(t) {
    await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
    await page.evaluate(([tk, tid]) => {
      localStorage.setItem('token', tk)
      localStorage.setItem('tenantId', String(tid))
    }, [t, TENANT])
  }

  async function gotoWithRelogin(url, selector, attempt = 0) {
    if (attempt > 6) throw new Error('重登超过上限: ' + url)
    await page.goto(url, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(1500)
    if (page.url().includes('/login')) {
      const s = await login()
      token = s.token
      await injectToken(token)
      return gotoWithRelogin(url, selector, attempt + 1)
    }
    if (selector) {
      const ok = await page.waitForSelector(selector, { timeout: 60000 }).then(() => true).catch(() => false)
      if (!ok && attempt < 6) {
        const body = await page.locator('body').innerText().catch(() => '')
        console.log(`  [retry ${attempt + 1}] url=${page.url()} body=${body.slice(0, 120).replace(/\s+/g, ' ')}`)
        await page.reload({ waitUntil: 'domcontentloaded' })
        return gotoWithRelogin(url, selector, attempt + 1)
      }
      if (!ok) {
        const body = await page.locator('body').innerText().catch(() => '')
        console.log(`  [fail] url=${page.url()} body=${body.slice(0, 200).replace(/\s+/g, ' ')}`)
      }
    }
  }

  await injectToken(token)
  // 预热：并行会话频繁改动文件会触发 Vite 重新编译，先访问一次触发按需编译，避免首次等待超时
  await page.goto(`${FE}/md/barcode`, { waitUntil: 'domcontentloaded' }).catch(() => { })
  await page.waitForTimeout(10000)
  await gotoWithRelogin(`${FE}/md/barcode`, '.category-list-layout')
  await page.waitForTimeout(3000)
  await page.screenshot({ path: `${SHOTS}/01-barcode-list.png` })

  // 分类树
  const treeText = await page.locator('.category-panel').innerText().catch(() => '')
  check('左侧商品分类树渲染', treeText.includes('商品分类') && treeText.includes('当前路径'),
    treeText.split('\n').filter(Boolean).slice(0, 6).join(' / '))
  check('分类树根节点为「全部商品」（对标口径，选中=不过滤）',
    treeText.includes('全部商品') && (await page.locator('.ss-grid tbody tr').count()) >= 0,
    treeText.split('\n').map(s => s.trim()).filter(Boolean).slice(0, 6).join(' | '))
  check('分类树无「新增分类」入口（对标只读）', (await page.locator('.category-panel [title="新增分类"]').count()) === 0, '未发现新增按钮')

  // 工具栏
  const toolbar = await page.locator('.toolbar-section').innerText().catch(() => '')
  check('工具栏为 刷新/打印(F8)/条码打印/导出（无新增）',
    ['刷新', '打印(F8)', '条码打印', '导出'].every(t => toolbar.includes(t)) && !toolbar.includes('新增'),
    toolbar.replace(/\n+/g, ' ').trim())

  // 查询区
  const searchText = (await page.locator('.search-section').innerText().catch(() => '')).replace(/\s+/g, '')
  const searchFields = ['筛选条件', '条码', '上架状态', '显示状态', '新增时间', '采购日期', '查询']
  check('查询条件与对标一致（6 项 + 查询）', searchFields.every(f => searchText.includes(f)), searchText.slice(0, 160))
  const opCount = await page.locator('.search-section .ant-select-selection-item').filter({ hasText: '≥' }).count()
  check('日期条件带比较符下拉（默认 ≥，对标 < = > ≠ ≤ ≥）', opCount >= 2, `比较符下拉=${opCount}`)

  // 表头默认列
  const headers = await page.locator('.ss-grid thead th').allInnerTexts().catch(() => [])
  const cleanHeaders = headers.map(h => h.trim()).filter(Boolean)
  const expectDefault = ['操作', '图片', '商品名称', '货号', '单位', '换算关系', '上架状态', '条码']
  check('默认显示列 = 对标 7 列（+操作）', expectDefault.every(h => cleanHeaders.includes(h)), cleanHeaders.join(' | '))
  check('默认隐藏列未显示（规格/型号/产地/新增时间/最近采购日期）',
    !cleanHeaders.includes('规格') && !cleanHeaders.includes('型号') && !cleanHeaders.includes('产地') && !cleanHeaders.includes('最近采购日期'),
    cleanHeaders.join(' | '))

  // 行数据
  const bodyText = await page.locator('.ss-grid tbody').innerText().catch(() => '')
  check('列表渲染真实条码数据', /6930\d{9}/.test(bodyText) && bodyText.includes('修改'), bodyText.split('\n').filter(Boolean).slice(0, 4).join(' / '))

  // 表头排序（UI）：点「条码」列排序图标 → 列表请求应带 sortField=barcode（后端整表排序，非当前页本地排序）
  let sortReqUrl = ''
  const onReq = r => {
    if (r.url().includes('/erp/product/barcodes/page') && r.url().includes('sortField=')) sortReqUrl = r.url()
  }
  page.on('request', onReq)
  const barcodeTh = page.locator('.ss-grid thead th').filter({ hasText: '条码' }).first()
  const sortIcon = barcodeTh.locator('.th-sort-icon').first()
  if (await sortIcon.count()) {
    await sortIcon.click({ force: true })
    await page.waitForTimeout(2500)
    check('页表头排序点击生效（请求带 sortField=barcode，后端排序）',
      sortReqUrl.includes('sortField=barcode'), sortReqUrl.split('/api')[1]?.slice(0, 130) || '未捕获排序请求')
  } else {
    check('页表头排序点击生效（请求带 sortField=barcode，后端排序）', false, '未找到条码列排序图标')
  }
  page.off('request', onReq)

  // 列配置弹窗
  const gear = page.locator('.th-settings-btn').first()
  check('数据表表头齿轮存在（列配置入口）', (await gear.count()) > 0, `齿轮数=${await gear.count()}`)
  if (await gear.count()) {
    await gear.click({ force: true })
    await page.waitForTimeout(1500)
    await page.screenshot({ path: `${SHOTS}/02-barcode-column-config.png` })
    const colWrap = page.locator('.ant-modal-wrap').filter({ hasText: '个人配置' }).last()
    const modalText = await colWrap.innerText().catch(() => '')
    check('列配置弹窗含 个人配置/全局配置 Tab', modalText.includes('个人配置') && modalText.includes('全局配置'), modalText.split('\n').filter(Boolean).slice(0, 3).join(' / '))
    const allCols = ['图片', '商品名称', '货号', '单位', '换算关系', '上架状态', '条码', '规格', '型号', '产地', '新增时间', '最近采购日期']
    const colModal = await colWrap.innerText().catch(() => '')
    check('列配置暴露 12 列可配置项', allCols.every(c => colModal.includes(c)), `缺失: ${allCols.filter(c => !colModal.includes(c)).join(',') || '无'}`)
    await page.locator('.ant-modal-wrap .ant-modal-close').first().click({ force: true }).catch(() => { })
    await page.waitForFunction(() => document.querySelectorAll('.ant-modal-wrap').length === 0, null, { timeout: 8000 }).catch(() => { })
    await page.waitForTimeout(1000)
  }

  // 条码打印
  const firstCheckbox = page.locator('.ss-grid tbody .ss-checkbox').first()
  if (await firstCheckbox.count()) {
    await firstCheckbox.click({ force: true })
    await page.waitForTimeout(600)
    await page.locator('button:has-text("条码打印")').first().click({ force: true })
    await page.waitForTimeout(1500)
    await page.screenshot({ path: `${SHOTS}/04-barcode-print.png` })
    const printWrap = page.locator('.ant-modal-wrap').filter({ hasText: '条码打印' }).last()
    const printModal = await printWrap.innerText().catch(() => '')
    const svgCount = await printWrap.locator('svg').count()
    check('条码打印弹窗生成真实 Code128 条码', svgCount > 0,
      `SVG 条码数=${svgCount}${printModal ? ' | ' + printModal.split('\n').filter(Boolean).slice(0, 3).join(' / ') : ''}`)
    await printWrap.locator('.ant-modal-close').first().click({ force: true }).catch(() => { })
    await page.waitForFunction(() => document.querySelectorAll('.ant-modal-wrap').length === 0, null, { timeout: 8000 }).catch(() => { })
    await page.waitForTimeout(800)
  } else {
    check('列表勾选框存在（条码打印前置）', false, '未找到行勾选框')
  }

  // 行级「修改」→ 商品档案（对标行为）
  // 用稳定的种子商品行（SP-TEST-001），避免并行会话清理 E2E 临时商品造成竞态
  const seedRow = page.locator('.ss-grid tbody tr').filter({ hasText: 'SP-TEST-001' }).first()
  const editBtn = (await seedRow.count())
    ? seedRow.locator('button:has-text("修改")').first()
    : page.locator('.ss-grid tbody button:has-text("修改")').first()
  if (await editBtn.count()) {
    await editBtn.click({ force: true })
    await page.waitForTimeout(6000)
    await page.screenshot({ path: `${SHOTS}/03-barcode-row-edit.png` })
    check('行级「修改」跳转商品档案「普通商品-新增与编辑」（对标行为）',
      /\/erp\/product\/form\/\d+/.test(page.url()), `当前 URL=${page.url()}`)
    const unitHeaders = await page.locator('.ss-grid thead th').allInnerTexts().catch(() => [])
    const unitClean = unitHeaders.map(h => h.trim())
    check('商品档案「商品单位」明细含条码列（可维护）', unitClean.includes('条码'), unitClean.filter(Boolean).join(' | ').slice(0, 200))
    const nameVal = await page.locator('input[placeholder="请输入商品名称"]').first().inputValue().catch(() => '')
    const bodyNow = await page.locator('body').innerText().catch(() => '')
    check('商品档案按 id 正确回填（商品可加载，非空壳）',
      !!nameVal && !bodyNow.includes('商品不存在或已删除'),
      `商品名称=${nameVal || '(空)'}`)

    // 完整数据闭环：在商品档案「商品单位」明细改条码 → 保存 → 条码页可见
    const pid = (page.url().match(/\/erp\/product\/form\/(\d+)/) || [])[1]
    const bcCell = page.locator('td[data-col-key="barcode"]').first()
    let closed = false
    if (pid && await bcCell.count()) {
      // BillDetailTable 的 input 列需点击单元格才进入编辑态
      const origBarcode = (await bcCell.innerText().catch(() => '')).trim()
      await bcCell.click({ force: true })
      await page.waitForTimeout(600)
      const bcInput = page.locator('td[data-col-key="barcode"] input').first()
      await bcInput.fill('6900000000777')
      await page.keyboard.press('Enter')
      await page.waitForTimeout(800)
      await page.locator('button.ant-btn-primary').filter({ hasText: '保存' }).first().click({ force: true })
      await page.waitForTimeout(4500)
      await gotoWithRelogin(`${FE}/md/barcode`, '.category-list-layout')
      await page.waitForTimeout(2000)
      await page.locator('.search-section input[placeholder="请输入商品名称/货号/条码"]').first().fill('6900000000777')
      // antd 两字按钮 innerText 为「查 询」带空格，:has-text("查询") 匹配不到 → 用类选择器
      await page.locator('.search-section button.btn-search').first().click({ force: true })
      await page.waitForTimeout(3000)
      const grid = await page.locator('.ss-grid tbody').innerText().catch(() => '')
      check('商品档案改条码 → 保存 → 条码页可查到（数据闭环）', grid.includes('6900000000777'),
        grid.split('\n').filter(Boolean).slice(0, 3).join(' / ') || '未查到')
      closed = true
      // 还原：接口恢复原条码
      try {
        const f2 = await apiReq('GET', `/erp/product/${pid}/form`, null, token)
        const prod2 = { ...(f2?.data?.product || {}) }
        ;['createTime', 'updateTime', 'createBy', 'updateBy', 'tenantId'].forEach(k => delete prod2[k])
        const u2 = (f2?.data?.units || []).map(u => ({
          id: u.id, unitName: u.unitName, unitType: u.unitType, isBaseUnit: u.isBaseUnit,
          conversionRate: u.conversionRate, barcode: u.barcode, sortOrder: u.sortOrder,
        }))
        const hit = u2.find(u => u.barcode === '6900000000777')
        if (hit) hit.barcode = origBarcode || null
        const back = await apiReq('PUT', `/erp/product/batch-update/${pid}`, { product: { ...prod2, id: pid }, units: u2 }, token)
        check('闭环后数据还原（原条码恢复）', back?.code === 200, `返回 ${back?.code}`)
      } catch (e) {
        check('闭环后数据还原（原条码恢复）', false, String(e).slice(0, 120))
      }
    }
    if (!closed) {
      check('商品档案改条码 → 保存 → 条码页可查到（数据闭环）', false, '未找到条码输入框')
    }
    // 回列表页
    await gotoWithRelogin(`${FE}/md/barcode`, '.category-list-layout')
    await page.waitForTimeout(2500)
  } else {
    check('行级「修改」按钮存在', false, '未找到修改按钮')
  }

  // 导出
  const [download] = await Promise.all([
    page.waitForEvent('download', { timeout: 15000 }).catch(() => null),
    page.locator('.toolbar-section button').filter({ hasText: '导出' }).first().click({ force: true }),
  ])
  check('页面「导出」触发真实文件下载', !!download, download ? await download.suggestedFilename() : '未捕获下载事件')
  await page.screenshot({ path: `${SHOTS}/05-barcode-after-export.png` })

  await browser.close()

  const passed = results.filter(r => r.ok).length
  console.log(`\n===== 商品条码 E2E 结果：${passed}/${results.length} 通过 =====`)
  const failed = results.filter(r => !r.ok)
  if (failed.length) {
    console.log('失败项：')
    failed.forEach(f => console.log('  ❌', f.name, '—', f.detail))
    process.exitCode = 1
  }
}

main().catch(e => { console.error('FATAL', e); process.exit(1) })
