/* 仓库规划（资料 → 仓库管理 → 仓库规划）金标准 UI 端到端验证
 *
 * 前端：FE_URL（默认 http://localhost:5656）
 * 后端：API_PORT（默认 5663，本会话独立实例；前端 /api/** 由 playwright route 转发）
 * 口径：对标 ql361 —— 双 Tab（1.仓库 / 2.货位）+ 左仓库分类树 + 列配置弹窗
 *      + 仓库信息弹窗（9 字段）+ 货位批量生成 + 真实导出/打印
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')

const FE = process.env.FE_URL || 'http://localhost:5656'
const API_PORT = Number(process.env.API_PORT || 5663)
const API_BASE = process.env.API_URL || `http://localhost:${API_PORT}`
const SHOTS = 'I:/AI-Ready/tool-results/warehouse-plan-ui'
const TENANT = 1
const LOGIN_USER = process.env.LOGIN_USER || 'e2e_whplan'

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

function rawReq(method, path, body, token) {
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
        const buf = Buffer.concat(chunks).toString('utf8')
        try { resolve(JSON.parse(buf)) } catch { resolve({ raw: buf.slice(0, 200), status: res.statusCode }) }
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
    username: LOGIN_USER, password: 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  if (!(res?.data?.token || res?.data?.accessToken) && LOGIN_USER !== 'admin') {
    const cap2 = await rawReq('GET', '/auth/captcha')
    const code2 = [...Buffer.from(cap2.data.img.split(',')[1], 'base64').toString('utf8')
      .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
    res = await rawReq('POST', '/auth/login', {
      username: 'admin', password: 'admin123', tenantName: '系统租户',
      captcha: code2, captchaKey: cap2.data.uuid,
    })
  }
  TOKEN = res?.data?.token || res?.data?.accessToken || null
  if (!TOKEN) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 200))
  return TOKEN
}

async function api(method, path, body) {
  if (!TOKEN) await login()
  let r = await rawReq(method, path, body, TOKEN)
  if (Number(r?.code) === 401 || r?.status === 401) {
    await login()
    r = await rawReq(method, path, body, TOKEN)
  }
  return r
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 600) : ''}`)
}

const stamp = String(Date.now()).slice(-6)
const whName = `E2E仓${stamp}`
const whName2 = `E2E仓${stamp}改`
const catName = `E2E分类${stamp}`
let whId = null
let catId = null

const flat = s => String(s || '').replace(/\s+/g, '')

async function main() {
  await login()
  console.log('登录成功\n═══ UI 验收：仓库规划 ═══')

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const p = await ctx.newPage()
  const pageErrors = []
  p.on('pageerror', e => pageErrors.push(String(e)))

  await p.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: API_BASE + u.pathname + u.search })
  })

  async function openPage(url, waitMs = 5000) {
    for (let attempt = 0; attempt < 4; attempt++) {
      await login()
      await p.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' }).catch(() => {})
      await p.evaluate(([tk, tid]) => {
        localStorage.setItem('token', tk)
        localStorage.setItem('tenantId', String(tid))
      }, [TOKEN, TENANT])
      await p.goto(url, { waitUntil: 'domcontentloaded' })
      await p.waitForTimeout(waitMs)
      if (!new URL(p.url()).pathname.includes('/login')) return
      console.log('  [被踢回登录页，重试注入 token]')
      await p.waitForTimeout(1200)
    }
    throw new Error('openPage 多次重试后仍未进入业务页: ' + url)
  }

  const bodyText = () => p.evaluate(() => document.body.innerText)
  const gridHeaders = () => p.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  const modalText = () => p.evaluate(() => {
    const ms = [...document.querySelectorAll('.ant-modal')].filter(m => m.offsetParent !== null)
    return ms.length ? ms[ms.length - 1].innerText : ''
  })
  const visibleModal = () => p.locator('.ant-modal:visible').last()
  const rowOf = (name) => p.locator('.ss-grid tbody tr:visible').filter({ hasText: name }).first()

  // 预清理：删除历史 E2E 残留（脚本可重复执行）
  {
    const stale = await api('GET', '/erp/warehouse/page?pageNum=1&pageSize=100&keyword=E2E%E4%BB%93&showDisabled=true')
    for (const r of stale?.data?.records || []) {
      const loc = await api('GET', `/wms/location/plan-page?pageNum=1&pageSize=200&warehouseId=${r.id}&showDisabled=true`)
      const ids = (loc?.data?.records || []).map(x => x.id)
      if (ids.length) await api('POST', '/wms/location/batch-delete', ids)
      await api('DELETE', `/erp/warehouse/${r.id}`)
    }
    const tree = await api('GET', '/erp/warehouse-category/tree')
    const collect = (nodes, out = []) => { (nodes || []).forEach(n => { out.push(n); collect(n.children, out) }); return out }
    for (const n of collect(tree?.data).filter(n => String(n.categoryName).startsWith('E2E分类')).reverse()) {
      await api('DELETE', `/erp/warehouse-category/${n.id}`)
    }
  }

  // ═══ A. Tab1 页面骨架 ═══
  await openPage(`${FE}/md/warehouse-plan`)
  await p.screenshot({ path: `${SHOTS}/ui-01-list-warehouse.png` })
  let txt = flat(await bodyText())
  check('A1 页面可访问且标题为仓库规划', txt.includes('仓库规划'))
  check('A2 Tab 条含「1.仓库」与「2.货位」', txt.includes('1.仓库') && txt.includes('2.货位'))
  for (const label of ['新增', '刷新', '打印(F8)', '导出', '筛选条件', '查询', '显示停用', '显示层次结构']) {
    check(`A3 Tab1 工具栏/查询区含「${label}」`, txt.includes(flat(label)))
  }
  check('A4 左侧树标题为「仓库分类」', txt.includes('仓库分类'))
  check('A5 树根节点为「全部」且显示当前路径', txt.includes('当前路径') && txt.includes('全部'))
  const headers1 = await gridHeaders()
  for (const h of ['操作', '仓库编号', '仓库名称', '联系人', '联系电话', '地址']) {
    check(`A6 Tab1 表头含「${h}」`, headers1.some(x => x.includes(h)), headers1.join('/'))
  }
  check('A7 Tab1 表格列数=5（不含操作/序号）', headers1.filter(h => !/^$/.test(h) && h !== '操作').length >= 5)
  check('A8 无页面配置齿轮（对标实测：本页无配置按钮）', !txt.includes('页面配置'))

  // A9 表头排序（对标：仓库编号/仓库名称带排序箭头，点击可切换升降序）
  {
    // 仓库编号是第 3 列（0=序号/1=操作/2=仓库编号）
    const firstCode = () => p.evaluate(() => {
      const tr = document.querySelector('.ss-grid tbody tr')
      const tds = tr ? tr.querySelectorAll('td') : []
      return tds[2] ? (tds[2].innerText || '').trim() : ''
    })
    const sortIcon = p.locator('.ss-grid th').filter({ hasText: '仓库编号' }).first().locator('.th-sort-icon')
    await sortIcon.click()
    await p.waitForTimeout(700)
    const asc = await firstCode()
    await sortIcon.click()
    await p.waitForTimeout(700)
    const desc = await firstCode()
    check('A9 仓库编号表头排序生效（升/降序首行不同）', !!asc && !!desc && asc !== desc, `${asc} | ${desc}`)
    await sortIcon.click()
    await p.waitForTimeout(500)
  }

  // ═══ B. 列配置弹窗 ═══
  {
    const gear = await p.$('.ss-grid th .th-settings-btn')
    check('B1 表头存在列配置齿轮', !!gear)
    if (gear) {
      await gear.click()
      await p.waitForTimeout(1200)
      await p.screenshot({ path: `${SHOTS}/ui-02-col-config.png` })
      const mt = flat(await modalText())
      check('B2 列配置弹窗打开（个人配置/全局配置）', mt.includes('个人配置') && mt.includes('全局配置'), mt.slice(0, 80))
      for (const col of ['仓库编号', '仓库名称', '联系人', '联系电话', '地址']) {
        check(`B3 列配置含「${col}」`, mt.includes(flat(col)))
      }
      await p.keyboard.press('Escape')
      await p.waitForTimeout(600)
    }
  }

  // ═══ C. 新增仓库弹窗（对标 9 字段） ═══
  await p.locator('button:has-text("新增")').first().click()
  await p.waitForTimeout(1500)
  await p.screenshot({ path: `${SHOTS}/ui-03-add-warehouse.png` })
  let mt = flat(await modalText())
  check('C1 弹窗标题为「仓库信息」', mt.includes('仓库信息'), mt.slice(0, 60))
  for (const f of ['仓库名称', '仓库编号', '所属分类', '上级仓库', '助记码', '联系人', '电话', '地址', '邮编', '备注']) {
    check(`C2 表单含字段「${f}」`, mt.includes(flat(f)))
  }
  check('C3 按钮为 保存(Enter)/取消(Esc)', mt.includes('保存(Enter)') && mt.includes('取消(Esc)'))
  {
    const codeVal = await p.locator('.ant-modal .ant-form-item').filter({ hasText: '仓库编号' }).first().locator('input').inputValue()
    check('C4 仓库编号自动带出 ck 前缀', /^ck\d+$/.test(codeVal), codeVal)
  }
  // 填写并保存
  {
    const modal = visibleModal()
    await modal.locator('.ant-form-item').filter({ hasText: '仓库名称' }).first().locator('input').fill(whName)
    await modal.locator('.ant-form-item').filter({ hasText: '联系人' }).first().locator('input').fill('验收员')
    await modal.locator('.ant-form-item').filter({ hasText: '电话' }).first().locator('input').fill('13900000000')
    await modal.locator('button:has-text("保存(Enter)")').click()
    await p.waitForTimeout(2500)
  }
  txt = flat(await bodyText())
  check('C5 新增后列表出现该仓库', txt.includes(flat(whName)), whName)
  {
    const list = await api('GET', `/erp/warehouse/page?pageNum=1&pageSize=50&keyword=${encodeURIComponent(whName)}`)
    whId = list?.data?.records?.[0]?.id
    check('C6 后端落库且带联系人', !!whId && list.data.records[0].contactPerson === '验收员', JSON.stringify(list?.data?.records?.[0] || {}).slice(0, 120))
  }

  // C7 「显示层次结构」：设为某仓库下级后，列表中子仓库紧随父仓库
  {
    const parents = await api('GET', '/erp/warehouse/page?pageNum=1&pageSize=50&keyword=WH001')
    const parentId = parents?.data?.records?.[0]?.id
    const setParent = await api('POST', '/erp/warehouse/update', { id: whId, warehouseName: whName, parentId })
    check('C7 设置上级仓库 200', setParent?.code === 200 && setParent.data === true, setParent)
    const hier = await api('GET', '/erp/warehouse/page?pageNum=1&pageSize=50&showHierarchy=true')
    const rows = hier?.data?.records || []
    const pi = rows.findIndex(r => String(r.id) === String(parentId))
    const ci = rows.findIndex(r => String(r.id) === String(whId))
    check('C8 「显示层次结构」子仓库紧随父仓库', pi >= 0 && ci === pi + 1, `parentIdx=${pi} childIdx=${ci}`)
    const flatList = await api('GET', '/erp/warehouse/page?pageNum=1&pageSize=50&showHierarchy=false')
    check('C9 关闭层次结构后仍可正常分页', flatList?.code === 200 && Array.isArray(flatList?.data?.records))
  }

  // ═══ D. 分类树维护 ═══
  {
    await p.locator('.category-header .ant-btn').filter({ has: p.locator('.anticon-plus') }).first().click()
    await p.waitForTimeout(1200)
    mt = flat(await modalText())
    check('D1 分类弹窗含 分类名称/所属分类/层级编号', ['分类名称', '所属分类', '层级编号'].every(f => mt.includes(flat(f))), mt.slice(0, 80))
    const modal = visibleModal()
    await modal.locator('.ant-form-item').filter({ hasText: '分类名称' }).first().locator('input').fill(catName)
    await modal.locator('button:has-text("保存(Enter)")').click()
    await p.waitForTimeout(2000)
    const treeTxt = flat(await p.evaluate(() => (document.querySelector('.category-panel') || {}).innerText || ''))
    check('D2 新增分类出现在左树', treeTxt.includes(flat(catName)), treeTxt.slice(0, 80))
    const catRes = await api('GET', '/erp/warehouse-category/tree')
    const findCatByName = (nodes, want) => {
      for (const n of nodes || []) {
        if (n.categoryName === want) return n
        const c = findCatByName(n.children, want)
        if (c) return c
      }
      return null
    }
    const findCat = (nodes) => findCatByName(nodes, catName)
    const catNode = findCat(catRes?.data)
    catId = catNode?.id
    check('D3 分类落库且自动生成层级编号', !!catId && !!catNode.categoryCode, JSON.stringify(catNode || {}).slice(0, 100))

    // D4 修改分类（树头部「修改」按钮，作用于当前选中节点）
    await p.locator('.category-panel .ant-tree-node-content-wrapper').filter({ hasText: catName }).first().click()
    await p.waitForTimeout(700)
    await p.locator('.category-header .ant-btn').filter({ has: p.locator('.anticon-edit') }).first().click()
    await p.waitForTimeout(1500)
    const catModal = visibleModal()
    const nameVal = await catModal.locator('.ant-form-item').filter({ hasText: '分类名称' }).first().locator('input').inputValue().catch(() => '')
    const codeVal = await catModal.locator('.ant-form-item').filter({ hasText: '层级编号' }).first().locator('input').inputValue().catch(() => '')
    check('D4 修改分类弹窗回填名称与层级编号', nameVal === catName && codeVal === catNode.categoryCode, `${nameVal}/${codeVal}`)
    await catModal.locator('.ant-form-item').filter({ hasText: '分类名称' }).first().locator('input').fill(`${catName}改`)
    await catModal.locator('button:has-text("保存(Enter)")').click()
    await p.waitForTimeout(2000)
    const catRes2 = await api('GET', '/erp/warehouse-category/tree')
    const renamed = findCatByName(catRes2?.data, `${catName}改`)
    check('D5 分类改名落库', !!renamed, JSON.stringify({ want: `${catName}改`, got: renamed?.categoryName, err: catRes2?.msg }))

    // D6 删除分类（树头部「删除」按钮 + 确认）
    await p.locator('.category-header .ant-btn').filter({ has: p.locator('.anticon-close') }).first().click()
    await p.waitForTimeout(900)
    await p.locator('.ant-popconfirm .ant-btn-primary').last().click()
    await p.waitForTimeout(2000)
    const catRes3 = await api('GET', '/erp/warehouse-category/tree')
    check('D6 删除分类生效', !findCatByName(catRes3?.data, `${catName}改`), catRes3?.data?.length)
    catId = null
  }

  // ═══ E. 行操作（停用 / 显示停用 / 删除） ═══
  {
    // 定位该行的「停用」链接
    const row = rowOf(whName)
    await row.locator('a', { hasText: '停用' }).first().click()
    await p.waitForTimeout(2000)
    let list = await api('GET', `/erp/warehouse/page?pageNum=1&pageSize=50&keyword=${encodeURIComponent(whName)}`)
    check('E1 行内「停用」生效（未勾选显示停用时不再返回）', Number(list?.data?.total) === 0, list?.data?.total)
    list = await api('GET', `/erp/warehouse/page?pageNum=1&pageSize=50&keyword=${encodeURIComponent(whName)}&showDisabled=true`)
    check('E2 勾选「显示停用」后可见', Number(list?.data?.total) === 1)
    // UI：勾选显示停用
    await p.locator('.search-row .ant-checkbox-wrapper', { hasText: '显示停用' }).first().click()
    await p.waitForTimeout(1800)
    txt = flat(await bodyText())
    check('E3 勾选后 UI 列表出现停用仓库', txt.includes(flat(whName)))
    // 启用
    const row2 = rowOf(whName)
    await row2.locator('a', { hasText: '启用' }).first().click()
    await p.waitForTimeout(1800)
    list = await api('GET', `/erp/warehouse/page?pageNum=1&pageSize=50&keyword=${encodeURIComponent(whName)}`)
    check('E4 行内「启用」生效', Number(list?.data?.total) === 1 && list.data.records[0].status === 1)
    // 修改
    await rowOf(whName).locator('a', { hasText: '修改' }).first().click()
    await p.waitForTimeout(1500)
    const modal = visibleModal()
    await modal.locator('.ant-form-item').filter({ hasText: '仓库名称' }).first().locator('input').fill(whName2)
    await modal.locator('button:has-text("保存(Enter)")').click()
    await p.waitForTimeout(2200)
    const detail = await api('GET', `/erp/warehouse/${whId}`)
    check('E5 行内「修改」经弹窗回写', detail?.data?.warehouseName === whName2, detail?.data?.warehouseName)
  }

  // ═══ F. 导出 / 打印 ═══
  {
    const dl = p.waitForEvent('download', { timeout: 15000 }).catch(() => null)
    await p.locator('button:has-text("导出")').first().click()
    const d = await dl
    check('F1 导出触发 xlsx 下载', !!d && /\.xlsx$/.test(d.suggestedFilename()), d?.suggestedFilename())
    // 打印：拦截 window.open
    await p.evaluate(() => { window.__printed = false; const o = window.open; window.open = () => ({ document: { write() {}, close() {} }, focus() {}, print() { window.__printed = true } }) })
    await p.keyboard.press('F8')
    await p.waitForTimeout(1500)
    check('F2 F8 触发打印', await p.evaluate(() => window.__printed === true))
  }

  // ═══ G. Tab2 货位 ═══
  await p.locator('.tab-item', { hasText: '2.货位' }).first().click()
  await p.waitForTimeout(2500)
  await p.screenshot({ path: `${SHOTS}/ui-04-tab2-location.png` })
  txt = flat(await bodyText())
  check('G1 切到货位 Tab', txt.includes('新增货位'))
  for (const label of ['新增货位', '刷新', '打印(F8)', '批量删除', '更多', '仓库', '货位编号', '显示停用', '查询']) {
    check(`G2 货位工具栏/查询区含「${label}」`, txt.includes(flat(label)))
  }
  const headers2 = await gridHeaders()
  for (const h of ['操作', '所属仓库', '货位编号', '备注']) {
    check(`G3 货位表头含「${h}」`, headers2.some(x => x.includes(h)), headers2.join('/'))
  }
  check('G4 货位 Tab 无左侧分类树', !(await p.locator('.category-panel').isVisible().catch(() => false)))

  // 新增货位（批量生成）
  await p.locator('button:has-text("新增货位")').first().click()
  await p.waitForTimeout(1500)
  await p.screenshot({ path: `${SHOTS}/ui-05-add-location.png` })
  mt = flat(await modalText())
  for (const f of ['仓库', '通道号', '生成数', '货架号', '货架层数', '货位列号', '备注', '保存(Enter)', '取消(Esc)']) {
    check(`G5 货位弹窗含「${f}」`, mt.includes(flat(f)))
  }
  check('G6 弹窗含编号规则说明（通道及货架/货架层与列/生成数量）',
    ['通道及货架', '货架层与列', '生成数量', 'A1', '101'].every(x => mt.includes(flat(x))), mt.slice(0, 100))
  {
    const modal = visibleModal()
    // 选择仓库（可搜索下拉：输入关键字后选中唯一项）
    await modal.locator('.ant-select-selector').first().click()
    await p.waitForTimeout(800)
    await p.keyboard.type(whName2)
    await p.waitForTimeout(1200)
    const dropdown = p.locator('.ant-select-dropdown:not(.ant-select-dropdown-hidden)').last()
    await dropdown.locator('.ant-select-item-option').filter({ hasText: whName2 }).first().click()
    await p.waitForTimeout(600)
    // 通道号 A / 货架号 01 / 层数 1 / 列 01 默认值即可，仅把货架生成数改成 1、列生成数 2
    const items = modal.locator('.ant-form-item')
    await items.nth(5).locator('input').fill('1')   // 货架层数
    await p.waitForTimeout(300)
    await modal.locator('button:has-text("保存(Enter)")').click()
    await p.waitForTimeout(2500)
  }
  {
    const loc = await api('GET', `/wms/location/plan-page?pageNum=1&pageSize=50&warehouseId=${whId}&showDisabled=true`)
    const rows = loc?.data?.records || []
    check('G7 生成货位落库（A1-101 格式）', rows.length > 0 && rows.some(r => r.locationCode === 'A1-101'), rows.map(r => r.locationCode).join(','))
    check('G8 货位所属仓库名称快照正确', rows.every(r => flat(r.warehouseName).includes(flat(whName2))), rows[0]?.warehouseName)
  }
  // 刷新 UI，确认货位出现
  await p.locator('button:has-text("刷新")').first().click()
  await p.waitForTimeout(2000)
  txt = flat(await bodyText())
  check('G9 UI 货位列表出现新货位', txt.includes('A1-101'))
  await p.screenshot({ path: `${SHOTS}/ui-06-location-list.png` })

  // G11-G13 货位行内操作（停用 / 启用 / 修改备注）
  {
    const locRow = p.locator('.ss-grid tbody tr:visible').filter({ hasText: 'A1-101' }).first()
    await locRow.locator('a', { hasText: '停用' }).first().click()
    await p.waitForTimeout(2000)
    let loc = await api('GET', `/wms/location/plan-page?pageNum=1&pageSize=50&warehouseId=${whId}`)
    check('G11 货位行内「停用」生效', (loc?.data?.records || []).every(r => r.locationCode !== 'A1-101'))
    // 停用后行从默认列表消失，需勾选「显示停用」才能再启用（与对标「显示停用」勾选项配套）
    await p.locator('.search-row .ant-checkbox-wrapper', { hasText: '显示停用' }).first().click()
    await p.waitForTimeout(2000)
    await locRow.locator('a', { hasText: '启用' }).first().click()
    await p.waitForTimeout(2000)
    loc = await api('GET', `/wms/location/plan-page?pageNum=1&pageSize=50&warehouseId=${whId}`)
    check('G12 货位行内「启用」生效', (loc?.data?.records || []).some(r => r.locationCode === 'A1-101'))

    await locRow.locator('a', { hasText: '修改' }).first().click()
    await p.waitForTimeout(1500)
    mt = flat(await modalText())
    const locModal = visibleModal()
    const codeReadonly = await locModal.locator('.ant-form-item').filter({ hasText: '货位编号' }).first().locator('input').inputValue().catch(() => '')
    const whReadonly = await locModal.locator('.ant-form-item').filter({ hasText: '仓库' }).first().locator('input').inputValue().catch(() => '')
    check('G13 货位修改弹窗含只读仓库/编号与可编辑备注',
      mt.includes('货位编号') && mt.includes('备注') && codeReadonly === 'A1-101' && !!whReadonly, `${codeReadonly}/${whReadonly}`)
    await locModal.locator('.ant-form-item').filter({ hasText: '备注' }).first().locator('input').fill('E2E货位备注')
    await visibleModal().locator('button:has-text("保存(Enter)")').click()
    await p.waitForTimeout(2000)
    loc = await api('GET', `/wms/location/plan-page?pageNum=1&pageSize=50&warehouseId=${whId}`)
    const target = (loc?.data?.records || []).find(r => r.locationCode === 'A1-101')
    check('G14 货位备注落库', target?.remark === 'E2E货位备注', target?.remark)
  }

  // 批量删除
  {
    await p.locator('.ss-grid tbody .ss-checkbox').first().click()
    await p.waitForTimeout(600)
    await p.locator('button:has-text("批量删除")').first().click()
    await p.waitForTimeout(2200)
    const loc = await api('GET', `/wms/location/plan-page?pageNum=1&pageSize=50&warehouseId=${whId}&showDisabled=true`)
    check('G10 批量删除货位生效', Number(loc?.data?.total) === 0, loc?.data?.total)
  }

  // ═══ H. 清理 + 页面无 JS 异常 ═══
  {
    const loc = await api('GET', `/wms/location/plan-page?pageNum=1&pageSize=100&warehouseId=${whId}&showDisabled=true`)
    const ids = (loc?.data?.records || []).map(r => r.id)
    if (ids.length) await api('POST', '/wms/location/batch-delete', ids)
    await api('DELETE', `/erp/warehouse/${whId}`)
    if (catId) await api('DELETE', `/erp/warehouse-category/${catId}`)
    const list = await api('GET', `/erp/warehouse/page?pageNum=1&pageSize=50&keyword=${encodeURIComponent(whName2)}&showDisabled=true`)
    check('H1 验收数据清理完成', Number(list?.data?.total) === 0, list?.data?.total)
  }
  check('H2 页面无 JS 运行时异常', pageErrors.length === 0, pageErrors.slice(0, 2).join(' | '))

  await browser.close()
  const ok = results.filter(r => r.ok).length
  console.log(`\n═══ 结果：${ok}/${results.length} 通过 ═══`)
  const bad = results.filter(r => !r.ok).map(r => r.name)
  if (bad.length) console.log('失败项：\n - ' + bad.join('\n - '))
  process.exit(bad.length ? 1 : 0)
}

main().catch(e => { console.error('FATAL', e.message); process.exit(1) })
