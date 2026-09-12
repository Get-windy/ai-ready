/* 会计科目（资料 → 财务账户 → 会计科目）金标准 端到端验证
 * 运行： node tools/e2e-accounting-subject.cjs [api|ui|all]
 * 后端：独立实例 5666（共享环境 5655 被并行会话占用）
 */
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE || 'http://localhost:5656'
const PORT = Number(process.env.BE_PORT || 5666)
const SHOTS = 'I:/AI-Ready/tool-results/accounting-subject'
const MODE = process.argv[2] || 'all'

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

function apiReq(method, p, body, token, opts = {}) {
  return new Promise((resolve, reject) => {
    const data = body == null ? null : (Buffer.isBuffer(body) ? body : JSON.stringify(body))
    const r = http.request({
      hostname: 'localhost', port: PORT, path: '/api' + p, method,
      headers: {
        'Content-Type': opts.contentType || 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        const ct = res.headers['content-type'] || ''
        if (opts.binary) {
          resolve({ status: res.statusCode, buffer: buf, contentType: ct, disposition: res.headers['content-disposition'] || '' })
          return
        }
        try { resolve(JSON.parse(buf.toString('utf8'))) } catch { resolve({ raw: buf.toString('utf8'), status: res.statusCode }) }
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

// 专用验收账号（tools/e2e-accounting-subject-user.sql 创建）：避免与并行会话共用 admin 互踢；
// 未创建时回退 admin（多会话并行下可能被踢，脚本内已带自愈重登）。
const LOGIN_USER = process.env.E2E_USER || 'e2e_subject'
const LOGIN_PASS = process.env.E2E_PASS || 'admin123'

async function doLogin(username, password) {
  const cap = await apiReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await apiReq('POST', '/auth/login', {
    username, password, tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  const token = res?.data?.token || res?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
  return token
}

let _fallbackWarned = false
async function login() {
  try {
    return await doLogin(LOGIN_USER, LOGIN_PASS)
  } catch (e) {
    if (LOGIN_USER === 'admin') throw e
    if (!_fallbackWarned) {
      _fallbackWarned = true
      console.log(`⚠️ 专用账号 ${LOGIN_USER} 不可用（可用 tools/e2e-accounting-subject-user.sql 创建），回退 admin —— 并行会话下可能互相踢下线`)
    }
    return await doLogin('admin', 'admin123')
  }
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`)
}

async function withRelogin(fn) {
  let token = await login()
  try { return await fn(token) } catch (e) {
    if (/登录|401|未登录/.test(String(e.message))) {
      token = await login()
      return await fn(token)
    }
    throw e
  }
}

function flatten(nodes, out = []) {
  for (const n of nodes || []) {
    out.push(n)
    flatten(n.children, out)
  }
  return out
}

const created = []

async function apiSuite() {
  await withRelogin(async (token) => {
    const T = (p, q) => apiReq('GET', '/erp/finance/subject' + p + (q || ''), null, token)
    const P = (p, b) => apiReq('POST', '/erp/finance/subject' + p, b, token)
    const U = (p, b) => apiReq('PUT', '/erp/finance/subject' + p, b, token)
    const D = (p) => apiReq('DELETE', '/erp/finance/subject' + p, null, token)

    // ── 0. 清理历史残留测试科目（幂等） ──
    const leftovers = await T('/tree', '?hierarchical=false&includeDisabled=true')
    const stale = flatten(leftovers.data).filter(x => /^T9/.test(String(x.subjectCode || '')))
      .sort((a, b) => (b.level || 0) - (a.level || 0))
    for (const s of stale) { await D('/' + s.id).catch(() => {}) }
    if (stale.length) check('清理历史残留测试科目', true, `删除 ${stale.length} 条`)

    // ── 1. 树形查询 ──
    const tree = await T('/tree')
    const flat0 = flatten(tree.data)
    check('GET /tree 返回科目树', Array.isArray(tree.data) && flat0.length >= 14, `根 ${tree.data?.length} / 全部 ${flat0.length}`)
    const sample = flat0.find(s => s.subjectCode === '1001')
    check('科目含新增字段(助记码/科目全名/核算项)',
      sample && 'mnemonicCode' in sample && 'fullName' in sample && 'auxiliaryTypeId' in sample,
      JSON.stringify({ fn: sample?.fullName, mc: sample?.mnemonicCode, at: sample?.auxiliaryTypeId }))
    const child = flat0.find(s => s.subjectCode === '6604')
    check('子科目层级=2 且科目全名=上级链', child?.level === 2 && child?.fullName === '管理费用/折旧费', `${child?.level} / ${child?.fullName}`)
    check('子科目返回上级科目编码/名称', child?.parentCode === '6602' && child?.parentName === '管理费用', `${child?.parentCode} ${child?.parentName}`)

    // ── 2. 平铺模式（显示层次结构=关） ──
    const flatRes = await T('/tree', '?hierarchical=false')
    check('hierarchical=false 返回平铺列表', Array.isArray(flatRes.data) && flatRes.data.every(x => x.children === undefined || x.children === null || x.children.length === 0),
      `rows=${flatRes.data?.length}`)

    // ── 3. 科目分类过滤 ──
    const asset = await T('/tree', '?subjectType=1&hierarchical=false')
    check('subjectType=1 只返回资产类', (asset.data || []).length === 6 && (asset.data || []).every(x => x.subjectType === 1), `rows=${asset.data?.length}`)

    // ── 4. 关键字（编号/名称/助记码） ──
    const kw = await T('/list', '?keyword=' + encodeURIComponent('累计'))
    check('关键字按名称匹配', (kw.data || []).length === 1 && kw.data[0].subjectCode === '1602', JSON.stringify((kw.data || []).map(x => x.subjectCode)))
    const kwCode = await T('/list', '?keyword=6602')
    check('关键字按编号匹配', (kwCode.data || []).length === 1 && kwCode.data[0].subjectName === '管理费用')

    // ── 5. 新增（顶级科目，继承科目分类） ──
    const CODE1 = 'T9001'
    // 前端实际调用无尾斜杠（/erp/finance/subject），必须与带尾斜杠写法同样可用
    const create1 = await P('', { subjectCode: CODE1, subjectName: 'E2E测试科目', subjectType: 1, direction: 1, mnemonicCode: 'E2ECSKM', auxiliaryTypeId: 3 })
    const id1 = create1?.data?.id
    if (id1) created.push(id1)
    check('新增顶级科目成功', create1.code === 200 && !!id1, JSON.stringify(create1).slice(0, 160))
    check('新增自动派生 level=1 / 科目全名 / 核算项名称',
      create1?.data?.level === 1 && create1?.data?.fullName === 'E2E测试科目' && create1?.data?.auxiliaryTypeName === '客户',
      `L${create1?.data?.level} ${create1?.data?.fullName} ${create1?.data?.auxiliaryTypeName}`)
    check('新增默认叶子=true 启用=true', create1?.data?.isLeaf === true && create1?.data?.isEnabled === true)

    // ── 6. 新增下级（继承分类 + 科目全名拼装 + 上级转非叶子） ──
    const CODE2 = 'T9001.01'
    const create2 = await P('/', { subjectCode: CODE2, subjectName: 'E2E下级科目', parentId: id1, direction: 1 })
    const id2 = create2?.data?.id
    if (id2) created.push(id2)
    check('新增下级成功(分类沿上级继承)', create2.code === 200 && create2?.data?.subjectType === 1, `type=${create2?.data?.subjectType}`)
    check('下级 level=2 且科目全名=上级链', create2?.data?.level === 2 && create2?.data?.fullName === 'E2E测试科目/E2E下级科目', `${create2?.data?.level} ${create2?.data?.fullName}`)
    const afterCreate2 = await T('/' + id1)
    check('上级科目 isLeaf 自动转 false', afterCreate2?.data?.isLeaf === false, String(afterCreate2?.data?.isLeaf))

    // ── 7. 编号唯一 ──
    const dup = await P('/', { subjectCode: CODE1, subjectName: '重复编号', subjectType: 1 })
    check('科目编号重复被拒', dup.code !== 200 && /已存在/.test(dup.message || ''), dup.message)

    // ── 8. 无分类新增被拒 ──
    const noType = await P('/', { subjectCode: 'T9999', subjectName: '无分类' })
    check('未选科目分类新增被拒', noType.code !== 200 && /科目分类/.test(noType.message || ''), noType.message)

    // ── 9. 更新（清空科目全名 → 自动重算；改助记码/核算项） ──
    const upd = await U('/' + id1, { subjectCode: CODE1, subjectName: 'E2E测试科目改名', fullName: '', mnemonicCode: 'GM', auxiliaryTypeId: 1, direction: 2 })
    check('更新后科目全名自动重算/核算项可改',
      upd?.data?.subjectName === 'E2E测试科目改名' && upd?.data?.fullName === 'E2E测试科目改名'
      && upd?.data?.auxiliaryTypeName === '部门' && upd?.data?.direction === 2,
      `${upd?.data?.fullName} / ${upd?.data?.auxiliaryTypeName} / ${upd?.data?.direction}`)
    const kidRenamed = await T('/' + id2)
    check('下级科目全名随上级联动', kidRenamed?.data?.fullName === 'E2E测试科目改名/E2E下级科目', kidRenamed?.data?.fullName)

    // ── 9b. 核算项可清空（页面整体提交 null 即「不核算」） ──
    const cleared = await U('/' + id1, { subjectCode: CODE1, subjectName: 'E2E测试科目改名', auxiliaryTypeId: null, direction: 2 })
    check('核算项可清空为「不核算」', cleared?.data?.auxiliaryTypeId == null && !cleared?.data?.auxiliaryTypeName,
      `aux=${cleared?.data?.auxiliaryTypeId} name=${cleared?.data?.auxiliaryTypeName}`)
    await U('/' + id1, { subjectCode: CODE1, subjectName: 'E2E测试科目改名', auxiliaryTypeId: 3, direction: 2 })

    // ── 10. 停用/启用 + 显示停用过滤 ──
    await U('/' + id1 + '/enable?enabled=false')
    const withoutDisabled = await T('/tree', '?hierarchical=false&includeDisabled=false&subjectType=1')
    const withDisabled = await T('/tree', '?hierarchical=false&includeDisabled=true&subjectType=1')
    check('显示停用=否 过滤停用科目',
      !(withoutDisabled.data || []).some(x => x.id === id1) && (withDisabled.data || []).some(x => x.id === id1),
      `off=${(withoutDisabled.data || []).length} on=${(withDisabled.data || []).length}`)
    const enabled = await U('/' + id1 + '/enable?enabled=true')
    check('启用科目生效', enabled?.data?.isEnabled === true)

    // ── 11. 有子科目不可删除 ──
    const delParent = await D('/' + id1)
    check('有子科目不可删除', delParent.code !== 200 && /子科目/.test(delParent.message || ''), delParent.message)

    // ── 12. 删除子科目（上级恢复叶子） ──
    const delChild = await D('/' + id2)
    check('删除子科目成功', delChild.code === 200)
    const parentBack = await T('/' + id1)
    check('上级科目恢复 isLeaf=true', parentBack?.data?.isLeaf === true, String(parentBack?.data?.isLeaf))

    // ── 13. 导出真实 xlsx ──
    const exp = await apiReq('GET', '/erp/finance/subject/export?hierarchical=false', null, token, { binary: true })
    const isXlsx = exp.buffer?.slice(0, 2)?.toString() === 'PK' && /spreadsheetml/.test(exp.contentType)
    check('导出返回真实 xlsx 流', isXlsx, `${exp.contentType} ${exp.buffer?.length}B`)
    check('导出文件名含日期', /filename\*=UTF-8''/.test(exp.disposition), exp.disposition)
    const xlsxPath = path.join(SHOTS, 'export.xlsx')
    fs.writeFileSync(xlsxPath, exp.buffer)
    check('导出内容非空(>2KB)', exp.buffer.length > 2000, `${exp.buffer.length}B`)

    // ── 14. 删除顶级科目 ──
    const delRoot = await D('/' + id1)
    check('删除顶级科目成功', delRoot.code === 200)
    const after = await T('/tree', '?hierarchical=false')
    check('删除后列表不再包含该科目', !flatten(after.data).some(x => x.subjectCode === CODE1))

    // 兼容：凭证/账簿等既有调用方仍可无参取全量树
    const legacy = await T('/tree')
    check('GET /tree 无参兼容旧调用方(含停用/全量)', flatten(legacy.data).length >= 14, `nodes=${flatten(legacy.data).length}`)
  })
}

async function uiSuite() {
  const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
  console.log('\n=== UI 验收（vite ' + FE + ' → api ' + PORT + '） ===')
  const ui = []
  const ck = (name, ok, detail = '') => { ui.push({ name, ok, detail }); console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`) }

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const page = await ctx.newPage()
  page.on('pageerror', e => console.log('[pageerror]', (e.message || '').slice(0, 200)))

  // 打印窗口拦截：捕获 window.open 的 HTML
  let printHtml = ''
  await page.addInitScript(() => {
    window.__printHtml = ''
    const orig = window.open
    window.open = function () {
      const fake = { document: { write: (h) => { window.__printHtml += h }, close: () => {} }, focus: () => {}, print: () => { window.__printCalled = true } }
      return fake
    }
  })

  // vite 代理指向 5655；本会话后端 5666，用精确前缀转发（勿用 **/api/** 会误伤 /src/api/*.ts）
  await page.route(`${FE}/api/**`, async (route) => {
    const target = route.request().url().replace(`${FE}/api/`, `http://localhost:${PORT}/api/`)
    try { await route.continue({ url: target }) } catch { await route.abort() }
  })

  const token = await login()
  await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([t]) => {
    localStorage.setItem('token', t)
    localStorage.setItem('access_token', t)
    localStorage.setItem('tenantId', '1')
    localStorage.setItem('tenantName', '系统租户')
    localStorage.setItem('userTenants', JSON.stringify([{ id: 1, tenantName: '系统租户' }]))
  }, [token])

  let curToken = token
  const gotoPage = async (attempt = 0) => {
    await page.goto(`${FE}/md/accounting-subject`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(4500)
    const onLogin = page.url().includes('/login')
    const noPage = (await page.locator('text=新增会计科目').count()) === 0
    if (onLogin || noPage) {
      if (attempt > 4) throw new Error('重登超过上限，页面未打开')
      curToken = await login()
      await page.evaluate(([t]) => { localStorage.setItem('token', t); localStorage.setItem('access_token', t); localStorage.setItem('tenantId', '1'); localStorage.setItem('tenantName', '系统租户') }, [curToken])
      return gotoPage(attempt + 1)
    }
  }
  // 前置清理：上一轮残留的测试科目（T 开头）通过接口删除，保证基线干净
  // ⚠️ sa-token is-concurrent=false：同一账号再次登录会踢掉上一个 token，本套件全程只登录一次
  const tk = token
  const staleTree = await apiReq('GET', '/erp/finance/subject/tree?hierarchical=false&includeDisabled=true', null, tk)
  const staleRows = (staleTree?.data || []).filter(x => /^T9|^T\d/.test(String(x.subjectCode || '')))
    .sort((a, b) => (b.level || 0) - (a.level || 0))
  for (const r of staleRows) await apiReq('DELETE', `/erp/finance/subject/${r.id}`, null, tk)
  if (staleRows.length) console.log(`（前置清理残留测试科目 ${staleRows.length} 条）`)

  await gotoPage()
  await page.waitForTimeout(1500)
  await page.screenshot({ path: `${SHOTS}/ui-01-page.png` })

  const txt = async (sel) => (await page.locator(sel).first().innerText().catch(() => '')).trim()

  // ── 1. 工具栏（对标：新增会计科目 / 刷新 / 打印(F8) / 导出） ──
  const btns = (await page.locator('button').allInnerTexts()).map(s => s.trim()).filter(Boolean)
  ck('工具栏含 新增会计科目 / 刷新 / 打印(F8) / 导出',
    ['新增会计科目', '刷新', '打印(F8)', '导出'].every(b => btns.some(t => t.includes(b))), btns.slice(0, 8).join(' | '))

  // ── 2. 查询区（对标：筛选条件 + 显示停用 + 显示层次结构） ──
  ck('查询区含 筛选条件 / 显示停用 / 显示层次结构',
    (await page.locator('text=筛选条件').count()) > 0 &&
    (await page.locator('text=显示停用').count()) > 0 &&
    (await page.locator('text=显示层次结构').count()) > 0)
  const ph = await page.locator('input[placeholder="请输入科目名称/编号"]').count()
  ck('筛选输入框 placeholder = 请输入科目名称/编号', ph > 0)
  const hierChecked = await page.evaluate(() => {
    const cb = [...document.querySelectorAll('input[type=checkbox]')].find(el => /显示层次结构/.test((el.closest('label') || el.parentElement)?.innerText || ''))
    return !!cb && cb.checked
  })
  ck('「显示层次结构」默认勾选', hierChecked)

  // ── 3. 左侧科目分类面板 ──
  const cats = (await page.locator('.category-node').allInnerTexts()).map(s => s.trim())
  ck('左侧科目分类含 全部/资产类/负债类/所有者权益类/成本类/损益类',
    ['全部', '资产类', '负债类', '所有者权益类', '成本类', '损益类'].every(c => cats.includes(c)), cats.join('/'))
  ck('面板底部显示「当前路径」', (await txt('.category-breadcrumb')).includes('当前路径') && (await txt('.category-breadcrumb')).includes('全部'), await txt('.category-breadcrumb'))

  // ── 4. 数据表列（对标：操作 | 科目编号 | 科目名称 | 核算项） ──
  const headers = await page.$$eval('.ss-grid thead th', ths => ths.map(t => (t.innerText || '').trim()).filter(Boolean))
  ck('数据表含 操作/科目编号/科目名称/核算项',
    ['操作', '科目编号', '科目名称', '核算项'].every(h => headers.includes(h)), headers.join(' | '))
  ck('表头齿轮（列配置入口）存在', (await page.locator('.th-settings-btn').count()) > 0)

  // ── 5. 树形渲染（默认展开全部层级：可见行数 = 启用科目总数；折旧费 缩进在 管理费用 下） ──
  // 断言不写死条数：科目数据会随业务/并行会话变化，改为按接口同口径动态比对
  const rowTexts = async () => page.$$eval('.ss-grid tbody tr', trs => trs.map(tr => (tr.innerText || '').trim().replace(/\s+/g, ' ')).filter(t => t && !/^(\d+)$/.test(t)))
  const liveSubjects = (await apiReq('GET', '/erp/finance/subject/tree?hierarchical=false&includeDisabled=false', null, tk))?.data || []
  let rows = await rowTexts()
  ck(`树形默认展开：全量启用科目可见（${liveSubjects.length} 条）`, rows.length === liveSubjects.length, `rows=${rows.length}`)
  const depth = await page.evaluate(() => {
    const trs = [...document.querySelectorAll('.ss-grid tbody tr')]
    const child = trs.find(tr => /折旧费/.test(tr.innerText || ''))
    const cell = child?.querySelector('.subject-name-cell')
    return cell ? (cell.getAttribute('style') || '') : ''
  })
  ck('子科目按层级缩进（折旧费 paddingLeft>0）', /padding-left:\s*(1[6-9]|[2-9]\d|\d{3,})px/.test(depth), depth)
  ck('父科目行显示展开/收起箭头', (await page.locator('.tree-toggle').count()) >= 1, `carets=${await page.locator('.tree-toggle').count()}`)

  // ── 6. 列配置弹窗（个人配置/全局配置，3 列） ──
  const gear = page.locator('.th-settings-btn').first()
  await gear.click({ force: true })
  await page.waitForTimeout(1800)
  await page.screenshot({ path: `${SHOTS}/ui-02-colconfig.png` })
  const dlgText = await page.locator('.ant-modal-content:visible, .ant-drawer-content:visible').first().innerText().catch(() => '')
  ck('列配置弹窗含 个人配置/全局配置 双 Tab', /个人配置/.test(dlgText) && /全局配置/.test(dlgText))
  ck('列配置可配列 = 3 列（科目编号/科目名称/核算项）',
    ['科目编号', '科目名称', '核算项'].every(c => dlgText.includes(c)), dlgText.replace(/\s+/g, ' ').slice(0, 150))
  // 关闭列配置弹窗（组件 Modal 不响应 Esc，点右上角关闭）
  const closeCfg = page.locator('.ant-modal-close').last()
  if (await closeCfg.count()) await closeCfg.click({ force: true })
  await page.waitForTimeout(1200)

  // ── 7. 科目分类过滤 ──
  await page.locator('.category-node').filter({ hasText: '资产类' }).first().click()
  await page.waitForTimeout(2500)
  rows = await rowTexts()
  ck('点击「资产类」后只显示资产类科目(6) 且 当前路径=资产类',
    rows.length === 6 && (await txt('.category-breadcrumb')).includes('资产类'), `rows=${rows.length} path=${await txt('.category-breadcrumb')}`)
  await page.screenshot({ path: `${SHOTS}/ui-03-category-asset.png` })

  // ── 8. 显示层次结构 关闭 → 平铺 ──
  await page.evaluate(() => {
    const cb = [...document.querySelectorAll('input[type=checkbox]')].find(el => /显示层次结构/.test((el.closest('label') || el.parentElement)?.innerText || ''))
    cb && cb.click()
  })
  await page.waitForTimeout(2000)
  ck('取消「显示层次结构」后无展开箭头（平铺）', (await page.locator('.tree-toggle').count()) === 0)
  await page.screenshot({ path: `${SHOTS}/ui-04-flat.png` })
  await page.evaluate(() => {
    const cb = [...document.querySelectorAll('input[type=checkbox]')].find(el => /显示层次结构/.test((el.closest('label') || el.parentElement)?.innerText || ''))
    cb && cb.click()
  })
  await page.waitForTimeout(2000)

  // ── 9. 关键字查询 ──
  await page.locator('.category-node').filter({ hasText: '全部' }).first().click()
  await page.waitForTimeout(1500)
  await page.fill('input[placeholder="请输入科目名称/编号"]', '累计')
  await page.locator('.btn-search').first().click()
  await page.waitForTimeout(2500)
  rows = await rowTexts()
  ck('按名称查询「累计」命中 1 条', rows.length === 1 && rows[0].includes('累计折旧'), rows.join('|').slice(0, 80))
  await page.fill('input[placeholder="请输入科目名称/编号"]', '')
  await page.locator('.btn-search').first().click()
  await page.waitForTimeout(2000)

  // ── 10. 未选分类新增 → 对标提示 ──
  const msgPromise = page.waitForSelector('.ant-message-notice-content', { timeout: 6000 }).catch(() => null)
  await page.locator('.btn-add').click()
  const msgEl = await msgPromise
  const msgText = msgEl ? (await msgEl.innerText()).trim() : ''
  ck('未选科目分类点新增 → 提示「请在科目分类下操作新增。」', /请在科目分类下操作新增/.test(msgText), msgText)
  await page.waitForTimeout(3200)

  // ── 11. 新增会计科目（选中分类 → 编辑器 6 字段） ──
  await page.locator('.category-node').filter({ hasText: '资产类' }).first().click()
  await page.waitForTimeout(2000)
  await page.locator('.btn-add').click()
  await page.waitForTimeout(1800)
  await page.screenshot({ path: `${SHOTS}/ui-05-editor.png` })
  const editorText = await page.locator('.ant-modal-content:visible').first().innerText().catch(() => '')
  ck('编辑器字段 = 科目编号*/科目名称*/助记码/科目全名/核算项/借-贷',
    ['科目编号', '科目名称', '助记码', '科目全名', '核算项', '借/贷'].every(f => editorText.includes(f)),
    editorText.replace(/\s+/g, ' ').slice(0, 180))
  const stamp = Date.now().toString().slice(-6)
  const CODE = 'T' + stamp
  // 精确匹配本科目编号（避免命中下级 Txxxxxx.01 的子串）
  const codeRe = new RegExp(CODE + '(?![\d.])')
  const hasCode = (rs) => rs.some(r => codeRe.test(r))
  await page.locator('.ant-modal-content:visible input').nth(0).fill(CODE)
  await page.locator('.ant-modal-content:visible input').nth(1).fill('UI验收科目')
  await page.locator('.ant-modal-content:visible input').nth(2).fill('UIYS')
  // 核算项下拉：选「客户」
  await page.locator('.ant-modal-content:visible .ant-select').first().click()
  await page.waitForTimeout(900)
  await page.locator('.ant-select-item-option').filter({ hasText: '客户' }).first().click()
  await page.waitForTimeout(400)
  await page.locator('.ant-modal-content:visible .ant-modal-footer button.ant-btn-primary').last().click()
  await page.waitForTimeout(3000)
  rows = await rowTexts()
  const createdRow = rows.find(r => r.includes(CODE))
  ck('新增科目成功并出现在列表', !!createdRow, createdRow || `未找到 ${CODE}`)
  ck('新增行「核算项」显示 客户', !!createdRow && createdRow.includes('客户'), createdRow || '')
  await page.screenshot({ path: `${SHOTS}/ui-06-created.png` })

  // ── 12. 修改科目 ──
  const myRow = page.locator('.ss-grid tbody tr').filter({ hasText: CODE }).first()
  await myRow.locator('text=修改').first().click()
  await page.waitForTimeout(1800)
  const editPrefill = await page.locator('.ant-modal-content:visible input').nth(0).inputValue()
  ck('修改弹窗回填科目编号', editPrefill === CODE, editPrefill)
  await page.locator('.ant-modal-content:visible input').nth(1).fill('UI验收科目改名')
  await page.locator('.ant-modal-content:visible .ant-modal-footer button.ant-btn-primary').last().click()
  await page.waitForTimeout(3000)
  rows = await rowTexts()
  ck('修改科目名称生效', rows.some(r => r.includes('UI验收科目改名')), '')

  // ── 13. 新增下级（更多 → 新增下级） ──
  const row2 = page.locator('.ss-grid tbody tr').filter({ hasText: CODE }).first()
  await row2.locator('text=更多').first().click()
  await page.waitForTimeout(900)
  await page.locator('.ant-dropdown-menu-item').filter({ hasText: '新增下级' }).first().click()
  await page.waitForTimeout(1800)
  const childEditor = await page.locator('.ant-modal-content:visible').first().innerText().catch(() => '')
  ck('新增下级弹窗显示上级科目（只读）', /上级科目/.test(childEditor), childEditor.replace(/\s+/g, ' ').slice(0, 120))
  await page.locator('.ant-modal-content:visible input').nth(0).fill(CODE + '.01')
  await page.locator('.ant-modal-content:visible input').nth(1).fill('UI下级科目')
  await page.locator('.ant-modal-content:visible .ant-modal-footer button.ant-btn-primary').last().click()
  await page.waitForTimeout(3000)
  rows = await rowTexts()
  ck('新增下级成功（列表出现下级科目）', rows.some(r => r.includes('UI下级科目')), '')

  // ── 14. 有子科目时删除按钮禁用 ──
  const parentRow = page.locator('.ss-grid tbody tr').filter({ hasText: CODE }).first()
  const delDisabled = await parentRow.locator('button:has-text("删除")').first().isDisabled().catch(() => false)
  ck('有子科目的行「删除」为禁用态', delDisabled)
  await page.screenshot({ path: `${SHOTS}/ui-07-child.png` })

  // ── 15. 更多 → 停用：默认（显示停用=否）应从列表消失 ──
  await parentRow.locator('text=更多').first().click()
  await page.waitForTimeout(900)
  await page.locator('.ant-dropdown-menu-item').filter({ hasText: '停用' }).first().click()
  let gone = false
  for (let i = 0; i < 10 && !gone; i++) {
    await page.waitForTimeout(800)
    rows = await rowTexts()
    gone = !hasCode(rows)
  }
  ck('更多→停用后科目从列表消失（显示停用=否）', gone, `rows=${rows.length}`)
  await page.screenshot({ path: `${SHOTS}/ui-08-disabled.png` })

  // ── 15b. 勾选「显示停用」→ 停用科目重现并有停用标记；再启用 ──
  await page.evaluate(() => {
    const cb = [...document.querySelectorAll('input[type=checkbox]')].find(el => /显示停用/.test((el.closest('label') || el.parentElement)?.innerText || ''))
    cb && cb.click()
  })
  await page.waitForTimeout(2500)
  rows = await rowTexts()
  const shownDisabled = hasCode(rows)
  const tagged = await page.locator('.subject-disabled').count()
  ck('勾选「显示停用」后停用科目重现且有停用样式', shownDisabled && tagged > 0, `rows=${rows.length} tagged=${tagged}`)
  await page.screenshot({ path: `${SHOTS}/ui-08b-show-disabled.png` })
  const reEnableRow = page.locator('.ss-grid tbody tr').filter({ hasText: CODE }).first()
  await reEnableRow.locator('text=更多').first().click()
  await page.waitForTimeout(900)
  await page.locator('.ant-dropdown-menu-item').filter({ hasText: '启用' }).first().click()
  await page.waitForTimeout(2500)
  const taggedAfter = await page.locator('.subject-disabled').count()
  ck('更多→启用后科目恢复启用（停用样式消失）', taggedAfter === 0, `tagged=${taggedAfter}`)
  // 关闭「显示停用」回到默认
  await page.evaluate(() => {
    const cb = [...document.querySelectorAll('input[type=checkbox]')].find(el => /显示停用/.test((el.closest('label') || el.parentElement)?.innerText || ''))
    cb && cb.click()
  })
  await page.waitForTimeout(2000)

  // ── 16. 打印（F8 键） ──
  await page.keyboard.press('F8')
  await page.waitForTimeout(1500)
  printHtml = await page.evaluate(() => window.__printHtml || '')
  ck('F8 打印生成真实打印文档（含表头与当前筛选下的科目数据）',
    /会计科目/.test(printHtml) && /科目编号/.test(printHtml) && /科目名称/.test(printHtml) && new RegExp(CODE).test(printHtml),
    `html=${printHtml.length}B`)

  // ── 17. 导出（真实 xlsx 下载） ──
  const dlPromise = page.waitForEvent('download', { timeout: 20000 }).catch(() => null)
  await page.locator('button:has-text("导出")').first().click()
  const dl = await dlPromise
  let dlName = ''
  if (dl) { dlName = dl.suggestedFilename(); await dl.saveAs(path.join(SHOTS, 'ui-export.xlsx')).catch(() => {}) }
  ck('导出触发下载且文件名为 xlsx', !!dl && /\.xlsx$/.test(dlName), dlName || '未捕获下载')

  // ── 18. 行内「删除」闭环（UI 删除下级 → 上级恢复可删） ──
  const clickRowAction = (rowText, action) => page.evaluate(([m, a]) => {
    const all = [...document.querySelectorAll('.ss-grid tbody tr')]
    const tr = all.find(t => new RegExp(m).test(t.innerText || ''))
    if (!tr) return 'no-row[' + all.map(t => (t.innerText || '').replace(/\s+/g, ' ').slice(0, 40)).join(' ~ ').slice(0, 400) + ']'
    const btn = [...tr.querySelectorAll('button')].find(b => new RegExp(a).test((b.innerText || '').replace(/\s/g, '')))
    if (!btn) return 'no-btn'
    if (btn.disabled) return 'disabled'
    btn.click()
    return 'ok'
  }, [rowText, action])
  const confirmDel = async () => {
    try {
      await page.locator('.ant-modal-confirm-btns').first().waitFor({ state: 'visible', timeout: 6000 })
    } catch {
      await page.screenshot({ path: `${SHOTS}/ui-09-confirm-missing.png` })
      return 'no-confirm'
    }
    const r = await page.evaluate(() => {
      const box = document.querySelector('.ant-modal-confirm-btns')
      if (!box) return 'no-box'
      const btns = [...box.querySelectorAll('button')]
      const ok = btns.find(b => /确定|确认/.test((b.innerText || '').replace(/\s/g, '')))
      if (!ok) return 'btns:' + btns.map(b => b.className + '|' + b.innerText).join(' ;; ')
      ok.click()
      return 'ok'
    })
    await page.waitForTimeout(2500)
    return r
  }

  const delChildState = await clickRowAction('UI下级科目', '^删除$')
  await page.waitForTimeout(1000)
  const confirmed = await confirmDel()
  rows = await rowTexts()
  ck('行内「删除」下级科目（含确认弹窗）生效', delChildState === 'ok' && confirmed && !rows.some(r => r.includes('UI下级科目')),
    `click=${delChildState} confirm=${confirmed}`)

  let delParentState = 'no-row'
  for (let i = 0; i < 3 && delParentState === 'no-row'; i++) {
    delParentState = await clickRowAction(CODE + '(?![\d.])', '^删除$')
    if (delParentState === 'no-row') await page.waitForTimeout(1500)
  }
  if (delParentState === 'ok') { await page.waitForTimeout(1000); await confirmDel() }

  // 兜底：极端情况下（并发/重渲染）用接口清理残留测试科目，保证共享 devdb 基线干净
  const leftOver = await apiReq('GET', '/erp/finance/subject/tree?hierarchical=false&includeDisabled=true', null, tk)
  const remain = (leftOver?.data || []).filter(x => /^T\d/.test(String(x.subjectCode || '')))
    .sort((a, b) => (b.level || 0) - (a.level || 0))
  for (const r of remain) await apiReq('DELETE', `/erp/finance/subject/${r.id}`, null, tk)
  if (remain.length) console.log(`（接口兜底清理残留 ${remain.length} 条）`)
  await page.locator('button:has-text("刷新")').first().click()
  await page.waitForTimeout(3000)
  rows = await rowTexts()
  ck('删除测试科目（清理完成）', !hasCode(rows), `click=${delParentState} 兜底=${remain.length}`)

  await browser.close()
  fs.writeFileSync(path.join(SHOTS, 'ui-results.json'), JSON.stringify(ui, null, 1))
  const ok = ui.filter(r => r.ok).length
  console.log(`\nUI 结果：${ok}/${ui.length} 通过`)
  ui.filter(r => !r.ok).forEach(f => console.log(' - ' + f.name + ' :: ' + f.detail))
  return ui
}

function xlsxSummary(file) {
  // 极简 xlsx 校验：读 sharedStrings 里的表头，确认导出列与页面一致
  const { execFileSync } = require('child_process')
  try {
    const out = execFileSync('python', ['-c', `
import zipfile,sys
z=zipfile.ZipFile(r'${file}')
names=z.namelist()
ss=[n for n in names if 'sharedStrings' in n]
txt=z.read(ss[0]).decode('utf-8') if ss else ''
import re
print('|'.join(re.findall(r'<t[^>]*>([^<]*)</t>', txt)[:12]))
print('sheets:', [n for n in names if n.startswith('xl/worksheets')])
`], { encoding: 'utf8' })
    return out.trim()
  } catch (e) { return 'xlsx parse fail: ' + e.message }
}

async function main() {
  if (MODE === 'api' || MODE === 'all') {
    console.log('=== API 验收（' + PORT + '） ===')
    await apiSuite()
    const sum = results.filter(r => r.ok).length
    console.log(`\nAPI 结果：${sum}/${results.length} 通过`)
    console.log('导出内容抽样: ' + xlsxSummary(path.join(SHOTS, 'export.xlsx')))
    const failed = results.filter(r => !r.ok)
    if (failed.length) {
      console.log('\n失败项：')
      failed.forEach(f => console.log(' - ' + f.name + ' :: ' + f.detail))
    }
    fs.writeFileSync(path.join(SHOTS, 'api-results.json'), JSON.stringify(results, null, 1))
    // 清理残留
    if (created.length) console.log('（已创建并删除的测试科目 id: ' + created.join(',') + '）')
    if (failed.length) process.exitCode = 1
  }
  if (MODE === 'ui' || MODE === 'all') {
    const ui = await uiSuite()
    if (ui.some(r => !r.ok)) process.exitCode = 1
  }
}

main().catch(e => { console.error('FATAL', e); process.exit(1) })
