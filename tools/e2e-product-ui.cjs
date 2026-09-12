/* 商品列表页（资料模块布局）浏览器端验收
 * 前置：vite dev(5656) 已运行；后端商品实例(默认5677)已运行
 * 运行：node tools/e2e-product-ui.cjs [apiPort] [vitePort]
 */
const http = require('http')
const fs = require('fs')
const path = require('path')
const { chromium } = require('playwright')
const { bootstrapAuthedPage } = require('./e2e-ui-login.cjs')

const API = Number(process.argv[2] || 5677)
const VITE = Number(process.argv[3] || 5656)
const OUT = path.resolve(__dirname, '../tool-results/product-ui')
const results = []

let TOKEN = ''
function apiReq(method, p, body) {
  return new Promise((resolve, reject) => {
    const data = body == null ? null : JSON.stringify(body)
    const headers = { 'Content-Type': 'application/json' }
    if (TOKEN) headers.Authorization = `Bearer ${TOKEN}`
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: '127.0.0.1', port: API, path: '/api' + p, method, headers }, res => {
      const ch = []
      res.on('data', c => ch.push(c))
      res.on('end', () => { try { resolve(JSON.parse(Buffer.concat(ch).toString('utf8'))) } catch { resolve({}) } })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function getToken() {
  const cap = await apiReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await apiReq('POST', '/auth/login', {
    username: process.env.E2E_USER || 'e2e_product', password: process.env.E2E_PASS || 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  return res?.data?.token
}

function check(name, ok, detail) {
  results.push({ name, ok })
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail !== undefined ? '  — ' + detail : ''}`)
}

;(async () => {
  fs.mkdirSync(OUT, { recursive: true })
  const token = await getToken()
  TOKEN = token
  if (!token) throw new Error('接口登录失败')

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 } })
  const page = await ctx.newPage()

  // 共享环境：vite 代理固定指向 5655（会被并行会话占用），只注入 token 会导致部分请求
  // 落在别人实例上 → 401 → 跳登录页。改为「route 统一转发 + 页面内真实登录拿完整会话」。
  const boot = await bootstrapAuthedPage(page, API, VITE, { target: '/md/product/index', wait: 8000 })
  if (boot?.token) TOKEN = boot.token

  // 前端用 createWebHistory（非 hash 模式）
  const url = `http://localhost:${VITE}/md/product/index`

  const headersOf = () => page.$$eval('.ss-grid thead th', ths =>
    ths.map(t => (t.innerText || '').trim().replace(/\s+/g, ' ')).filter(Boolean))

  const waitRow = async (timeout = 20000) => {
    const t0 = Date.now()
    while (Date.now() - t0 < timeout) {
      const n = await page.$$eval('.ss-grid tbody tr', trs => trs.length).catch(() => 0)
      if (n > 0) return n
      await page.waitForTimeout(600)
    }
    return 0
  }

  // ── 自愈重登（共享环境：admin 被并行会话顶下线会跳登录页）──
  const isLoginPage = async () => (await page.locator('input[placeholder="请输入租户名称"]').count()) > 0
  const heal = async (label) => {
    if (!(await isLoginPage())) return false
    console.log(`  [heal] ${label}: 检测到登录页，重新登录取新 token`)
    const t = await getToken()
    if (!t) return false
    await page.evaluate(tk => {
      localStorage.setItem('token', tk)
      localStorage.setItem('tenantId', '1')
      localStorage.setItem('tenantName', '系统租户')
    }, t)
    await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForTimeout(7000)
    return true
  }
  await heal('首屏')

  const tabLabels = await page.$$eval('.tab-item', els => els.map(e => (e.innerText || '').trim()))
  check('UI1 四个子Tab渲染', JSON.stringify(tabLabels) === JSON.stringify(['全部商品', '套餐', '商品上架', '商品授权']),
    JSON.stringify(tabLabels))

  const searchLabels = await page.$$eval('.search-label', els => els.map(e => (e.innerText || '').trim()))
  const expectSearch = ['筛选条件', '品牌', '新增日期（起）', '新增日期（止）', '显示状态', '使用优惠券', '是否标品', '所属行业类别']
  check('UI2 查询区固定项齐全', expectSearch.every(s => searchLabels.includes(s)), JSON.stringify(searchLabels))
  const searchArea = await page.evaluate(() => {
    const sec = document.querySelector('.search-section')
    return sec ? sec.innerText.replace(/\s+/g, ' ') : ''
  })
  const hasQueryBtn = await page.$$eval('.search-section button', els => els.some(e => (e.innerText || '').replace(/\s/g, '') === '查询'))
  check('UI3 查询按钮 + 显示层次结构', hasQueryBtn && searchArea.includes('显示层次结构'), searchArea.slice(0, 80))

  // 显示状态默认「已启用」
  const statusText = await page.evaluate(() => {
    const items = [...document.querySelectorAll('.search-item')]
    for (const it of items) {
      const lab = it.querySelector('.search-label')
      if (lab && lab.innerText.includes('显示状态')) {
        const sel = it.querySelector('.ant-select-selection-item')
        return sel ? sel.innerText.trim() : ''
      }
    }
    return ''
  })
  check('UI4 显示状态默认已启用', statusText === '已启用', statusText)

  // 左侧分类树（商品分类 + 修改/删除/添加/收起）
  const treeTitle = await page.locator('.category-title').first().innerText().catch(() => '')
  check('UI5 左分类树标题=商品分类', treeTitle.trim() === '商品分类', treeTitle)
  const titleAttrs = await page.$$eval('.category-header-actions button', els => els.map(e => e.getAttribute('title') || ''))
  check('UI6 分类树头部按钮(修改/删除/新增/收起)', ['修改分类', '删除分类', '新增分类', '收起树形菜单'].every(t => titleAttrs.includes(t)),
    JSON.stringify(titleAttrs))
  const pathText = await page.locator('.category-breadcrumb').innerText().catch(() => '')
  check('UI7 当前路径展示', pathText.includes('当前路径'), pathText.replace(/\s+/g, ' '))

  // 全部商品 默认列
  const rows = await waitRow()
  check('UI8 全部商品列表有数据', rows > 0, `${rows} 行(含空行占位)`)
  const h1 = await headersOf()
  const expect1 = ['图片', '商品名称', '商品货号', '所属行业类别', '条码', '规格', '型号', '产地', '品牌', '单位', '可用库存', '换算关系', '备注']
  check('UI9 全部商品默认列(14列口径)', expect1.every(h => h1.includes(h)), JSON.stringify(h1))
  const hidden1 = ['默认仓库', '零售价', '批发价', '预设进价', '商品积分', '新增时间']
  check('UI10 全部商品隐藏列默认不显示', hidden1.every(h => !h1.includes(h)),
    hidden1.filter(h => h1.includes(h)).join(','))
  await page.screenshot({ path: path.join(OUT, 'tab-all.png') })

  // 列配置齿轮
  const gear = page.locator('.ss-grid thead .th-settings-btn, .ss-grid thead .icon-shezhi2').first()
  if (await gear.count()) {
    await gear.click({ force: true }).catch(() => {})
    await page.waitForTimeout(1200)
    const dlg = await page.locator('.ant-modal, .ant-drawer').first().innerText().catch(() => '')
    check('UI11 数据表头齿轮打开列配置(个人/全局)', dlg.includes('个人配置') || dlg.includes('全局配置'), dlg.replace(/\s+/g, ' ').slice(0, 60))
    await page.screenshot({ path: path.join(OUT, 'col-config.png') })
    await page.keyboard.press('Escape')
    await page.waitForTimeout(600)
  } else {
    check('UI11 数据表头齿轮打开列配置(个人/全局)', false, '未找到表头齿轮')
  }

  // 套餐 Tab
  await page.locator('.tab-item:has-text("套餐")').click()
  await page.waitForTimeout(3000)
  await heal('套餐Tab')
  const h2 = await headersOf()
  const expect2 = ['图片', '套餐名称', '套餐编号', '套餐金额', '套餐条码', '捆绑销售', '商品明细']
  check('UI12 套餐Tab 7列(全部默认显示)', expect2.every(h => h2.includes(h)), JSON.stringify(h2))
  const kitBtns = await page.$$eval('.toolbar-left button, .toolbar-right button', els => els.map(e => (e.innerText || '').trim()))
  check('UI13 套餐Tab 按钮组(新增/条码打印)', kitBtns.includes('新增') && kitBtns.some(b => b.includes('条码打印')), JSON.stringify(kitBtns))
  await page.screenshot({ path: path.join(OUT, 'tab-package.png') })

  // 商品上架 Tab
  await page.locator('.tab-item:has-text("商品上架")').click()
  await page.waitForTimeout(3000)
  await heal('商品上架Tab')
  const h3 = await headersOf()
  const expect3 = ['上架', '图片', '商品名称', '商品货号', '条码', '规格', '单位', '可用库存', '零售价', '批发价', '排序', '排序值', '起订量', '备注']
  // 标签列来自「商品辅助资料 → 商品标签」字典（标准槽位 TAG_1..TAG_20 + 用户自定义昵称）：
  // 基础 14 列 + 20 个标签槽位列 = 默认 34 列（另有操作列）
  const baseSet = new Set([...expect3, '操作'])
  const tagCols = h3.filter(h => !baseSet.has(h))
  check('UI14 商品上架默认列(基础 14 列 + 20 个标签槽位列)',
    expect3.every(h => h3.includes(h)) && tagCols.length === 20,
    `列数=${h3.length}，标签列 ${tagCols.length}：${tagCols.slice(0, 3).join('/')} … ${tagCols[tagCols.length - 1]}`)
  const shelfBtns = await page.$$eval('.toolbar-left button', els => els.map(e => (e.innerText || '').trim()))
  check('UI15 商品上架按钮组', shelfBtns.some(b => b.includes('批量上架')) && shelfBtns.some(b => b.includes('批量下架')), JSON.stringify(shelfBtns))
  const switchCount = await page.locator('.ss-grid tbody .ant-switch').count()
  check('UI16 商品上架「上架」列可切换', switchCount > 0, `${switchCount} 个开关`)

  // UI16b 标签列按槽位编码打标：造一个带 TAG_1 的商品 → 搜索定位 → 标签列应为 √
  const stampShelf = Date.now()
  const catTreeS = await apiReq('GET', '/erp/product-category/tree')
  const flatS = []
  const walkS = (ns) => (ns || []).forEach(n => { flatS.push(n); walkS(n.children) })
  walkS(catTreeS.data)
  const shelfName = `E2E上架标签-${stampShelf}`
  const createdS = await apiReq('POST', '/erp/product/batch-create', {
    product: {
      productName: shelfName, productCodeAlias: `E2ESHELF${stampShelf}`,
      categoryId: flatS[0]?.id, industryCategory: '其他', productType: 'SINGLE',
      mallTags: 'TAG_1', mallShelfStatus: 1,
    },
    units: [{ unitName: '个', unitType: 'SMALL', isBaseUnit: 1, conversionRate: 1, sortOrder: 1 }],
  })
  const shelfPid = createdS?.data?.productId
  console.log('[debug] 造数结果:', JSON.stringify(createdS).slice(0, 200))
  const searchBoxes = await page.$$eval('input[placeholder]', els => els.map(e => e.getAttribute('placeholder')))
  console.log('[debug] 查询区输入框:', JSON.stringify(searchBoxes.slice(0, 8)))
  await page.fill('input[placeholder*="商品名称"]', shelfName)
  await page.keyboard.press('Enter')
  await page.waitForTimeout(3500)
  await heal('标签打标查询')
  const tagCell = await page.evaluate((name) => {
    const head = [...document.querySelectorAll('.ss-grid thead th')].map(t => (t.innerText || '').trim())
    // 标签列紧跟基础默认列最后一个「备注」之后（fixed 操作列在最前，索引与 td 一致）
    const firstTagIdx = head.lastIndexOf('备注') + 1
    const rows = [...document.querySelectorAll('.ss-grid tbody tr')]
    const tr = rows.find(r => (r.innerText || '').includes(name))
    if (!tr || firstTagIdx <= 0) return null
    const tds = [...tr.querySelectorAll('td')]
    return tds.slice(firstTagIdx, firstTagIdx + 3).map(td => (td.innerText || '').trim())
  }, shelfName)
  check('UI16b 商品上架标签列按槽位编码打标(√)',
    Array.isArray(tagCell) && tagCell[0] === '√',
    tagCell ? `前3个标签列=${JSON.stringify(tagCell.slice(0, 3))}` : '未找到该商品行')
  await apiReq('DELETE', `/erp/product/${shelfPid}`)
  await page.fill('input[placeholder*="商品名称"]', '')
  await page.keyboard.press('Enter')
  await page.waitForTimeout(2500)
  await page.screenshot({ path: path.join(OUT, 'tab-shelf.png') })

  // 商品授权 Tab
  await page.locator('.tab-item:has-text("商品授权")').click()
  await page.waitForTimeout(3000)
  await heal('商品授权Tab')
  const h4 = await headersOf()
  const expect4 = ['图片', '商品名称', '商品货号', '屏蔽客户', '屏蔽级别', '屏蔽区域', '条码', '规格']
  check('UI17 商品授权默认列(8列)', expect4.every(h => h4.includes(h)), JSON.stringify(h4))
  const authBtns = await page.$$eval('.toolbar-left button', els => els.map(e => (e.innerText || '').trim()))
  check('UI18 商品授权按钮组(批量屏蔽/批量取消)', authBtns.some(b => b.includes('批量屏蔽')) && authBtns.some(b => b.includes('批量取消')),
    JSON.stringify(authBtns))
  await page.screenshot({ path: path.join(OUT, 'tab-auth.png') })

  // 回到全部商品，验证「更多」菜单与工具栏
  await page.locator('.tab-item:has-text("全部商品")').click()
  await page.waitForTimeout(3000)
  await heal('全部商品Tab')
  const allBtns = await page.$$eval('.toolbar-left button, .toolbar-right button', els => els.map(e => (e.innerText || '').trim()))
  check('UI19 全部商品按钮组(新增/导入/云导入/刷新/打印/导出/更多)',
    ['新增', '导入', '云导入', '刷新', '打印(F8)', '导出', '更多'].every(b => allBtns.some(x => x.includes(b))), JSON.stringify(allBtns))
  await page.locator('.toolbar-right button:has-text("更多")').first().click().catch(() => {})
  await page.waitForTimeout(800)
  const menuItems = await page.$$eval('.ant-dropdown-menu-item', els => els.map(e => (e.innerText || '').trim()).filter(Boolean))
  check('UI20 更多菜单(批量搬移/批量修改/停用/启用/批量删除)',
    ['批量搬移', '批量修改', '停用', '启用', '批量删除'].every(x => menuItems.includes(x)), JSON.stringify(menuItems))
  await page.keyboard.press('Escape')
  await page.screenshot({ path: path.join(OUT, 'tab-all-final.png') })

  const passed = results.filter(r => r.ok).length
  console.log(`\n===== 商品列表页 UI 验收 ${passed}/${results.length} =====`)
  const failed = results.filter(r => !r.ok)
  if (failed.length) {
    console.log('失败项：')
    failed.forEach(f => console.log(' - ' + f.name))
  }
  await browser.close()
  process.exit(failed.length ? 1 : 0)
})().catch(e => { console.error('脚本异常:', e.message); process.exit(1) })
