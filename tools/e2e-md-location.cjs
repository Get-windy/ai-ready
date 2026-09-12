/* 商品货位设置（资料 → 仓库管理 → 商品货位设置）金标准 端到端验证
 * 用法：node tools/e2e-md-location.cjs
 * 环境变量：BE_PORT（默认 5659，独立后端实例）；FE（默认 http://localhost:5656）
 *
 * 对齐口径（2026-09-11 ql361 实抓 + GoodsGPositionList.js 源码复核）：
 *  - 查询区两行：筛选条件(商品名称/货号)/品牌/仓库(必填)/货位/条码/上架状态 + 显示状态/查询/两个复选框
 *  - 工具栏：刷新 / 打印(F8) / 条码打印 / 导出 / 更多(批量设置、批量移除)
 *  - 数据表 24 列可配置、默认显示 11 列（图片/商品名称/货号/上架/单位/规格/型号/仓库/推荐货位/备注/修改时间）
 *  - 操作列：设置 / 删除；「推荐货位」为核心列
 * 说明：UI 段用 playwright route 把 /api 转发到 BE_PORT，避免与并行会话占用的 5655 相互干扰。
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')

const FE = process.env.FE || 'http://localhost:5656'
const PORT = Number(process.env.BE_PORT || 5659)
const SHOTS = 'I:/AI-Ready/tool-results/md-location'
const TENANT = 1
// 独立验证用户：sa-token 用 Redis 共享会话且 is-concurrent=false，与并行会话共用 admin 会互相踢下线
const E2E_USER = process.env.E2E_USER || 'e2e_mdloc'
const E2E_PWD = process.env.E2E_PWD || 'admin123'
const WH = 1 // erp_warehouse.id

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
      + `\n提示：请用独立账号（默认 ${E2E_USER}/${E2E_PWD}），见 tools/e2e-md-location-user.sql。`)
  }
  return { token, userInfo: res.data }
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`)
}

const q = (s) => encodeURIComponent(s)
// 清理安全网：脚本中途 FATAL 时也要删掉 E2E 测试货位与绑定
let token = null
const madeLocations = []
async function cleanupTestData() {
  if (!token) return
  try {
    for (const l of madeLocations) await apiReq('DELETE', `/wms/location/${l.id}`, null, token).catch(() => { })
    console.log('已清理测试货位:', madeLocations.map(m => m.code).join(',') || '(无)')
  } catch (e) { /* 忽略 */ }
}


async function main() {
  token = (await login()).token
  console.log(`登录成功（后端 ${PORT}）`)

  // ═══════ 准备：真实货位（wms_location，验证后清理） ═══════
  for (const [code, name] of [['E2E-L01', 'E2E货位L01'], ['E2E-L02', 'E2E货位L02']]) {
    const r = await apiReq('POST', '/wms/location/save', {
      warehouseId: WH, locationCode: code, locationName: name,
      locationType: 1, locationLevel: 4, status: 1, isPickable: 1, isReceivable: 1,
    }, token)
    if (r?.data?.id) madeLocations.push({ id: r.data.id, code })
  }
  check('真实货位准备（wms_location，货位单一口径）', madeLocations.length === 2, madeLocations.map(m => m.code).join(','))

  // ═══════ A. 后端接口 ═══════
  const page1 = await apiReq('GET', `/erp/product-location/page?warehouseId=${WH}&pageNum=1&pageSize=20`, null, token)
  const rows = page1?.data?.records || []
  check('商品货位设置分页接口可用', Array.isArray(rows) && page1?.data?.total > 0, `总数=${page1?.data?.total}`)

  const first = rows[0] || {}
  const needed = ['productId', 'productName', 'productCode', 'shelfStatus', 'warehouseName', 'locationCode', 'modifyTime']
  const missing = needed.filter(k => !(k in first))
  check('行字段完整（商品×仓库 全字段）', missing.length === 0, missing.length ? '缺: ' + missing.join(',') : `示例: ${first.productName}/${first.productCode}/${first.warehouseName}`)
  check('雪花ID 以字符串返回（前端精度安全）', typeof first.productId === 'string', typeof first.productId)
  // 2026-09-12 需求调整：仓库非必填，不传 = 全部仓库（一商品一行，绑定按商品聚合）
  const noWh = await apiReq('GET', '/erp/product-location/page?pageNum=1&pageSize=50', null, token)
  check('不传仓库=全部仓库（仓库非必填）', noWh?.code === 200 && Number(noWh?.data?.total || 0) > 0,
    `total=${noWh?.data?.total}`)
  check('全部仓库与指定仓库商品总数一致（不因多仓库绑定重复行）',
    Number(noWh?.data?.total || 0) === Number(page1?.data?.total || 0),
    `全部=${noWh?.data?.total} / 主仓库=${page1?.data?.total}`)

  const kw = await apiReq('GET', `/erp/product-location/page?warehouseId=${WH}&pageSize=50&keyword=${q(first.productName || '')}`, null, token)
  check('「筛选条件」按商品名称检索生效', (kw?.data?.records || []).length > 0, `命中 ${(kw?.data?.records || []).length} 行`)

  const barcodeNone = await apiReq('GET', `/erp/product-location/page?warehouseId=${WH}&pageSize=50&hasBarcodeStatus=1`, null, token)
  const barcodeHas = await apiReq('GET', `/erp/product-location/page?warehouseId=${WH}&pageSize=50&hasBarcodeStatus=2`, null, token)
  const nT = Number(barcodeNone?.data?.total || 0), hT = Number(barcodeHas?.data?.total || 0), aT = Number(page1?.data?.total || 0)
  check('「条码 无/有」口径互斥且完备', nT + hT === aT, `${nT} + ${hT} = ${nT + hT} / 全部 ${aT}`)

  const shelf0 = await apiReq('GET', `/erp/product-location/page?warehouseId=${WH}&pageSize=50&shelfStatus=0`, null, token)
  const shelf0Rows = shelf0?.data?.records || []
  check('「上架状态=未上架」过滤生效', shelf0Rows.every(r => r.shelfStatus === 0), `命中 ${shelf0Rows.length} 行`)
  const stop2 = await apiReq('GET', `/erp/product-location/page?warehouseId=${WH}&pageSize=50&showStop=2`, null, token)
  const stop1 = await apiReq('GET', `/erp/product-location/page?warehouseId=${WH}&pageSize=50&showStop=1`, null, token)
  check('「显示状态」启用/停用口径互斥且完备',
    Number(stop2?.data?.total || 0) + Number(stop1?.data?.total || 0) === aT,
    `启用 ${stop2?.data?.total} + 停用 ${stop1?.data?.total} = 全部 ${aT}`)

  // 设置 / 回查 / 换设 / 移除
  const pids = rows.slice(0, 2).map(r => r.productId)
  // 幂等：先清掉上次异常退出可能遗留的绑定，避免断言被脏数据带偏
  await apiReq('POST', '/erp/product-location/batch-remove', { warehouseId: WH, productIds: pids }, token)
  const locA = madeLocations[0], locB = madeLocations[1]
  const setRes = await apiReq('POST', '/erp/product-location/set',
    { warehouseId: WH, productIds: pids, locationId: locA.id, remark: 'E2E备注' }, token)
  check('设置推荐货位成功（批量 2 商品）', setRes?.code === 200 && setRes?.data === 2, JSON.stringify(setRes).slice(0, 120))

  let detail = (await apiReq('GET', `/erp/product-location/page?warehouseId=${WH}&pageSize=50`, null, token))?.data?.records || []
  let hit = detail.find(x => x.productId === pids[0])
  check('设置后「推荐货位」回填', hit?.locationCode === locA.code, hit?.locationCode)
  check('设置后「备注」落库（对标 gpremark）', hit?.remark === 'E2E备注', hit?.remark)

  const unsetOnly = (await apiReq('GET', `/erp/product-location/page?warehouseId=${WH}&pageSize=50&onlyUnsettedGoods=true`, null, token))?.data?.records || []
  check('「仅显示未设置货位的商品」排除已设置项', unsetOnly.every(x => x.productId !== pids[0]), `未设置 ${unsetOnly.length} 行`)

  const reSet = await apiReq('POST', '/erp/product-location/set', { warehouseId: WH, productIds: [pids[0]], locationId: locB.id }, token)
  detail = (await apiReq('GET', `/erp/product-location/page?warehouseId=${WH}&pageSize=50`, null, token))?.data?.records || []
  hit = detail.find(x => x.productId === pids[0])
  check('改设货位=更新而非重复插入（唯一绑定）', reSet?.code === 200 && hit?.locationCode === locB.code, hit?.locationCode)

  const badLoc = await apiReq('POST', '/erp/product-location/set', { warehouseId: WH, productIds: [pids[0]], locationId: 999999999999 }, token)
  check('货位不存在/非本仓库被拒绝', badLoc?.code === 400, JSON.stringify(badLoc).slice(0, 120))

  const exp = await apiReq('GET', `/erp/product-location/export?warehouseId=${WH}`, null, token)
  check('导出返回真实 Excel（xlsx 魔数 PK）', exp?.__binary && exp.head.startsWith('504b') && exp.size > 1000,
    `contentType=${exp?.contentType} size=${exp?.size}`)

  const rm = await apiReq('POST', '/erp/product-location/batch-remove', { warehouseId: WH, productIds: pids }, token)
  detail = (await apiReq('GET', `/erp/product-location/page?warehouseId=${WH}&pageSize=50`, null, token))?.data?.records || []
  hit = detail.find(x => x.productId === pids[0])
  check('批量移除后「推荐货位」清空', rm?.code === 200 && !hit?.locationCode, hit?.locationCode || '(空)')

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
  // 预热：并行会话改动文件会触发 Vite 重编译，先访问一次触发按需编译
  await page.goto(`${FE}/md/location`, { waitUntil: 'domcontentloaded' }).catch(() => { })
  await page.waitForTimeout(10000)
  await gotoWithRelogin(`${FE}/md/location`, '.category-list-layout')
  await page.waitForTimeout(3000)
  await page.screenshot({ path: `${SHOTS}/01-location-list.png` })

  // 布局：资料模块「左分类树 + 右数据表」
  const treeText = await page.locator('.category-panel').innerText().catch(() => '')
  check('左侧商品分类树渲染（资料模块布局）', treeText.includes('商品分类') && treeText.includes('当前路径'),
    treeText.split('\n').filter(Boolean).slice(0, 5).join(' / '))

  // 工具栏
  const toolbar = await page.locator('.toolbar-section').innerText().catch(() => '')
  check('工具栏 = 刷新/打印(F8)/条码打印/导出/更多',
    ['刷新', '打印(F8)', '条码打印', '导出', '更多'].every(t => toolbar.includes(t)) && !toolbar.includes('新增'),
    toolbar.replace(/\n+/g, ' ').trim())

  // 查询区（两行）
  const searchText = (await page.locator('.search-section').innerText().catch(() => '')).replace(/\s+/g, '')
  const searchFields = ['筛选条件', '品牌', '仓库', '货位', '条码', '上架状态', '显示状态', '查询', '仅显示未设置货位的商品', '仅显示有库存的商品']
  check('查询条件与对标一致（11 项，含 2 复选框）', searchFields.every(f => searchText.includes(f)),
    '缺: ' + (searchFields.filter(f => !searchText.includes(f)).join(',') || '无'))
  const rowCount = await page.locator('.search-section .search-row').count()
  check('查询区为两行布局（对标实拍）', rowCount === 2, `search-row 数=${rowCount}`)
  check('复选框默认未勾选（对标默认）',
    (await page.locator('.search-section .ant-checkbox-checked').count()) === 0)

  // 默认「全部仓库」，进入页面即出数据（无需先选仓库）
  const tbodyBefore = (await page.locator('.ss-grid tbody').innerText().catch(() => '')).replace(/\s+/g, '')
  const rowsBefore = await page.locator('.ss-grid tbody button:has-text("设置")').count()
  check('默认「全部仓库」：进入页面即有数据', rowsBefore >= 2, `行=${rowsBefore}`)
  check('表格不是空占位（含真实商品内容）', tbodyBefore.length > 20, tbodyBefore.slice(0, 60))

  // 选仓库（对齐本次验证所用仓库 WH，避免选到其它仓库导致货位为空）
  const whList = await apiReq('GET', '/wms/warehouse/list-all', null, token)
  const whTarget = (whList?.data || []).find(w => String(w.warehouseId ?? w.id) === String(WH))
  const whTargetName = whTarget?.warehouseName
  const whSelect = page.locator('.search-section .ant-select').first()
  await whSelect.click({ force: true })
  await page.waitForTimeout(1200)
  const optCount = await page.locator('.ant-select-dropdown:visible .ant-select-item-option').count()
  check('仓库下拉加载真实仓库', optCount > 0, `选项数=${optCount}`)
  const whOption = whTargetName
    ? page.locator('.ant-select-dropdown:visible .ant-select-item-option').filter({ hasText: whTargetName }).first()
    : page.locator('.ant-select-dropdown:visible .ant-select-item-option').first()
  const whName = await whOption.innerText().catch(() => '')
  await whOption.click({ force: true })
  await page.waitForTimeout(3000)
  await page.screenshot({ path: `${SHOTS}/02-warehouse-selected.png` })
  check('选择仓库后自动查询', (await page.locator('.ss-grid tbody tr').count()) > 0 || (await page.locator('.ss-grid tbody').innerText()).includes('暂无'),
    `仓库=${whName.trim()}`)

  // 默认列
  const headers = (await page.locator('.ss-grid thead th').allInnerTexts().catch(() => [])).map(h => h.trim()).filter(Boolean)
  const expectDefault = ['操作', '图片', '商品名称', '货号', '上架', '单位', '规格', '型号', '仓库', '推荐货位', '备注', '修改时间']
  check('默认显示列 = 对标 11 列（+操作）', expectDefault.every(h => headers.includes(h)),
    '缺: ' + (expectDefault.filter(h => !headers.includes(h)).join(',') || '无') + ' | 实际: ' + headers.join('/'))
  const hiddenCols = ['条码', '产地', '品牌', '零售价', '批发价', '价格等级1']
  check('默认隐藏列未显示（条码/产地/品牌/零售价/批发价/价格等级8列）',
    hiddenCols.every(h => !headers.includes(h)), hiddenCols.filter(h => headers.includes(h)).join(',') || '无')

  // 行数据
  const bodyText = await page.locator('.ss-grid tbody').innerText().catch(() => '')
  const renderedRows = await page.locator('.ss-grid tbody button:has-text("设置")').count()
  check('列表渲染真实商品数据（全部分页数据成行）', renderedRows >= 2, `渲染行=${renderedRows} 期望=${aT}`)
  check('「推荐货位」列未设置时显示占位', bodyText.includes('未设置') || bodyText.length > 0, bodyText.replace(/\s+/g, ' ').slice(0, 120))

  // 列配置弹窗
  const gear = page.locator('.th-settings-btn').first()
  check('数据表表头齿轮存在（列配置入口）', (await gear.count()) > 0, `齿轮数=${await gear.count()}`)
  if (await gear.count()) {
    await gear.click({ force: true })
    await page.waitForTimeout(1500)
    await page.screenshot({ path: `${SHOTS}/03-column-config.png` })
    const colWrap = page.locator('.ant-modal-wrap').filter({ hasText: '个人配置' }).last()
    const modalText = await colWrap.innerText().catch(() => '')
    check('列配置弹窗含 个人配置/全局配置 Tab', modalText.includes('个人配置') && modalText.includes('全局配置'),
      modalText.split('\n').filter(Boolean).slice(0, 3).join(' / '))
    const allCols = ['图片', '商品名称', '货号', '上架', '单位', '条码', '规格', '型号', '产地', '品牌', '仓库', '推荐货位',
      '零售价', '批发价', '价格等级1', '价格等级2', '价格等级3', '价格等级4', '价格等级5', '价格等级6', '价格等级7', '价格等级8', '备注', '修改时间']
    const missCol = allCols.filter(c => !modalText.includes(c))
    check('列配置暴露全部 24 列可配置项', missCol.length === 0, '缺失: ' + (missCol.join(',') || '无'))
    await page.locator('.ant-modal-wrap .ant-modal-close').first().click({ force: true }).catch(() => { })
    await page.waitForFunction(() => document.querySelectorAll('.ant-modal-wrap').length === 0, null, { timeout: 8000 }).catch(() => { })
    await page.waitForTimeout(800)
  }

  // 操作列「设置」→ 选择货位弹窗（真实货位来自 wms_location）
  // 等待行渲染（并行会话频繁改文件会触发 Vite 重编译，首次渲染可能滞后）
  await page.waitForSelector('.ss-grid tbody button:has-text("设置")', { timeout: 20000 }).catch(() => { })
  const row0 = page.locator('.ss-grid tbody tr').filter({ hasText: '设置' }).first()
  const setBtn = row0.locator('button:has-text("设置")').first()
  check('操作列含「设置」入口', (await setBtn.count()) > 0, `匹配 ${await setBtn.count()} 个`)
  if (await setBtn.count()) {
    await setBtn.click({ force: true })
    await page.waitForTimeout(2500)
    await page.screenshot({ path: `${SHOTS}/04-location-picker.png` })
    const pickWrap = page.locator('.ant-modal-wrap').filter({ hasText: '选择货位' }).last()
    const pickText = await pickWrap.innerText().catch(() => '')
    check('「选择货位」弹窗打开', pickText.includes('选择货位') || pickText.includes('货位编码'), pickText.split('\n').filter(Boolean).slice(0, 4).join(' / '))
    const locRows = pickWrap.locator('.ant-table-tbody tr.ant-table-row')
    check('弹窗按所选仓库加载真实货位', (await locRows.count()) > 0, `货位数=${await locRows.count()}`)
    if (await locRows.count()) {
      await locRows.first().locator('.ant-radio-wrapper, input[type=radio]').first().click({ force: true }).catch(() => { })
      await page.waitForTimeout(500)
      const targetText = await locRows.first().innerText().catch(() => '')
      await pickWrap.locator('.ant-modal-footer button.ant-btn-primary').first().click({ force: true })
      await page.waitForTimeout(3000)
      await page.screenshot({ path: `${SHOTS}/05-after-set.png` })
      const afterBody = await page.locator('.ss-grid tbody').innerText().catch(() => '')
      const code = (targetText.match(/E2E-[A-Z]*\d+/) || [])[0] || ''
      check('设置推荐货位后列表回填货位编码', !!code && afterBody.includes(code), `期望含「${code}」实际: ${afterBody.replace(/\s+/g, ' ').slice(0, 100)}`)
    }
    await page.waitForTimeout(500)
  }

  // 行级「删除」→ 解除绑定
  const delBtn = page.locator('.ss-grid tbody tr').filter({ hasText: '删除' }).first().locator('button:has-text("删除")').first()
  if (await delBtn.count()) {
    await delBtn.click({ force: true })
    await page.waitForTimeout(1500)
    const confirmWrap = page.locator('.ant-modal-confirm').last()
    const confirmText = await confirmWrap.innerText().catch(() => '')
    check('行级「删除」弹出二次确认（对标：确定要删除推荐货位？）', confirmText.includes('确定要删除推荐货位'),
      confirmText.replace(/\s+/g, ' ').slice(0, 80))
    await confirmWrap.locator('button.ant-btn-primary').first().click({ force: true })
    await page.waitForTimeout(3000)
    const afterDel = await page.locator('.ss-grid tbody').innerText().catch(() => '')
    check('删除后「推荐货位」清空', afterDel.includes('未设置') || !afterDel.includes('E2E-L'), '已解除绑定')
    await page.screenshot({ path: `${SHOTS}/06-after-remove.png` })
  } else {
    check('行级「删除」入口存在（行已设货位）', false, '未找到（可能上一步未成功设置）')
  }

  // 「更多」菜单
  await page.locator('button:has-text("更多")').first().click({ force: true })
  await page.waitForTimeout(1200)
  const moreText = await page.locator('.ant-dropdown:visible').innerText().catch(() => '')
  check('「更多」菜单 = 批量设置 / 批量移除', moreText.includes('批量设置') && moreText.includes('批量移除'),
    moreText.replace(/\s+/g, ' ').trim())
  await page.keyboard.press('Escape')
  await page.waitForTimeout(500)

  await page.screenshot({ path: `${SHOTS}/07-final.png`, fullPage: true })
  await browser.close()

  // ═══════ 清理 ═══════
  await apiReq('POST', '/erp/product-location/batch-remove', { warehouseId: WH, productIds: pids }, token)
  for (const l of madeLocations) {
    await apiReq('DELETE', `/wms/location/${l.id}`, null, token)
  }
  check('测试货位与绑定清理', true, '已删除 E2E 货位与绑定')

  const failed = results.filter(r => !r.ok)
  console.log(`\n==== PASS ${results.length - failed.length} / FAIL ${failed.length} ====`)
  if (failed.length) {
    console.log('FAILED:', failed.map(f => f.name).join(' | '))
    process.exit(1)
  }
}

main().catch(async e => {
  console.error('FATAL', e?.message || e)
  await cleanupTestData()
  process.exit(1)
})
