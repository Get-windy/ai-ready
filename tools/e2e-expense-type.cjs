/* 费用类型（资料 → 财务账户 → 费用类型）金标准 端到端验证
 * 运行： node tools/e2e-expense-type.cjs [api|ui|all]
 * 后端：独立实例 5681（共享环境 5655 被并行会话占用）；前端 vite 5656
 *
 * 口径（对标 ql361 实测）：费用类型 = 费用类会计科目视图
 *   finance_account_subject 中 subject_type=5（损益类）且 direction=1（借方）
 *   ql361 费用类型页与会计科目页同接口 cc.erp.bll.bas.account.getlist，仅 bastype=fee/account 不同
 */
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE || 'http://localhost:5656'
const PORT = Number(process.env.BE_PORT || 5681)
const SHOTS = 'I:/AI-Ready/tool-results/expense-type'
const MODE = process.argv[2] || 'all'
const API = '/erp/md/expense-type'

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

// 专用验收账号（tools/e2e-expense-type-user.sql 创建）：避免与并行会话共用 admin 互踢
const LOGIN_USER = process.env.E2E_USER || 'e2e_expense'
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
      console.log(`⚠️ 专用账号 ${LOGIN_USER} 不可用（可用 tools/e2e-expense-type-user.sql 创建），回退 admin —— 并行会话下可能互相踢下线`)
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
    const T = (p, q) => apiReq('GET', API + p + (q || ''), null, token)
    const P = (p, b) => apiReq('POST', API + p, b, token)
    const U = (p, b) => apiReq('PUT', API + p, b, token)
    const D = (p) => apiReq('DELETE', API + p, null, token)

    // ── 0. 清理历史残留测试科目（前缀 ET9，幂等） ──
    const leftovers = await T('/tree', '?hierarchical=false&includeDisabled=true')
    const stale = flatten(leftovers.data).filter(x => /^ET9/.test(String(x.subjectCode || '')))
      .sort((a, b) => (b.level || 0) - (a.level || 0))
    for (const s of stale) { await D('/' + s.id).catch(() => {}) }
    if (stale.length) check('清理历史残留测试科目', true, `删除 ${stale.length} 条`)

    // ── 1. 口径：只返回 损益类(5) + 借方(1) ──
    const tree = await T('/tree')
    const flat0 = flatten(tree.data)
    check('GET /tree 返回费用类科目树', Array.isArray(tree.data) && flat0.length >= 2, `根 ${tree.data?.length} / 全部 ${flat0.length}`)
    check('全部科目 subjectType=5(损益类) 且 direction=1(借方)',
      flat0.length > 0 && flat0.every(x => x.subjectType === 5 && x.direction === 1),
      JSON.stringify(flat0.map(x => `${x.subjectCode}:${x.subjectType}/${x.direction}`).slice(0, 6)))
    check('费用类科目含 6602 管理费用', flat0.some(x => x.subjectCode === '6602'), JSON.stringify(flat0.map(x => x.subjectCode).slice(0, 10)))
    check('损益类贷方(收入类 6001) 不在费用类型内', !flat0.some(x => x.subjectCode === '6001'))
    check('资产类(1001) 不在费用类型内', !flat0.some(x => x.subjectCode === '1001'))
    const child = flat0.find(x => x.subjectCode === '6604')
    if (child) {
      check('子科目层级=2 且科目全名=上级链', child.level === 2 && child.fullName === '管理费用/折旧费', `${child.level} / ${child.fullName}`)
      check('子科目返回上级科目编码/名称', child.parentCode === '6602' && child.parentName === '管理费用', `${child.parentCode} ${child.parentName}`)
    }

    // ── 2. 口径不可被请求参数覆盖（后端固定 5/1） ──
    const forced = await T('/tree', '?hierarchical=false&subjectType=1&direction=2')
    check('请求传 subjectType=1 仍只返回费用类(口径由后端固定)',
      (forced.data || []).length > 0 && (forced.data || []).every(x => x.subjectType === 5 && x.direction === 1),
      `rows=${forced.data?.length}`)

    // ── 3. 平铺模式（显示层次结构=关） ──
    const flatRes = await T('/tree', '?hierarchical=false')
    check('hierarchical=false 返回平铺列表', Array.isArray(flatRes.data) && flatRes.data.every(x => x.children === undefined || x.children === null || x.children.length === 0),
      `rows=${flatRes.data?.length}`)

    // ── 4. 关键字（编号/名称） ──
    const kwName = await T('/list', '?keyword=' + encodeURIComponent('管理费用'))
    check('关键字按名称匹配', (kwName.data || []).length >= 1 && (kwName.data || []).every(x => /管理费用/.test(x.subjectName)), JSON.stringify((kwName.data || []).map(x => x.subjectCode)))
    const kwCode = await T('/list', '?keyword=6602')
    check('关键字按编号匹配', (kwCode.data || []).length >= 1 && (kwCode.data || []).some(x => x.subjectCode === '6602'))

    // ── 5. 核算项可选项 ──
    const aux = await T('/aux-types')
    check('GET /aux-types 返回核算项可选项', Array.isArray(aux.data), `n=${aux.data?.length}`)

    // ── 6. 新增费用（无尾斜杠，与前端一致） ──
    const CODE1 = 'ET9001'
    const create1 = await P('', { subjectCode: CODE1, subjectName: 'E2E费用科目', mnemonicCode: 'E2EFY', direction: 1, auxiliaryTypeId: 3 })
    const id1 = create1?.data?.id
    if (id1) created.push(id1)
    check('新增费用成功（无尾斜杠路径）', create1.code === 200 && !!id1, JSON.stringify(create1).slice(0, 160))
    check('新增强制 损益类(5) 且方向缺省借方(1)',
      create1?.data?.subjectType === 5 && create1?.data?.direction === 1,
      `type=${create1?.data?.subjectType} dir=${create1?.data?.direction}`)
    check('新增自动派生 level=1 / 科目全名 / 核算项名称',
      create1?.data?.level === 1 && create1?.data?.fullName === 'E2E费用科目' && create1?.data?.auxiliaryTypeName === '客户',
      `L${create1?.data?.level} ${create1?.data?.fullName} ${create1?.data?.auxiliaryTypeName}`)
    check('新增默认叶子=true 启用=true', create1?.data?.isLeaf === true && create1?.data?.isEnabled === true)

    // ── 7. 新增下级 ──
    const CODE2 = 'ET9001.01'
    const create2 = await P('/', { subjectCode: CODE2, subjectName: 'E2E费用下级', parentId: id1 })
    const id2 = create2?.data?.id
    if (id2) created.push(id2)
    check('新增下级成功(损益类沿上级继承)', create2.code === 200 && create2?.data?.subjectType === 5, `type=${create2?.data?.subjectType}`)
    check('下级 level=2 且科目全名=上级链', create2?.data?.level === 2 && create2?.data?.fullName === 'E2E费用科目/E2E费用下级', `${create2?.data?.level} ${create2?.data?.fullName}`)
    const afterCreate2 = await T('/' + id1)
    check('上级科目 isLeaf 自动转 false', afterCreate2?.data?.isLeaf === false, String(afterCreate2?.data?.isLeaf))

    // ── 8. 编号唯一 / 必填 ──
    const dup = await P('/', { subjectCode: CODE1, subjectName: '重复编号' })
    check('科目编号重复被拒', dup.code !== 200 && /已存在/.test(dup.message || ''), dup.message)
    const noCode = await P('/', { subjectName: '无编号' })
    check('科目编号为空被拒', noCode.code !== 200, noCode.message)

    // ── 9. 更新（改名/助记码/核算项） ──
    const upd = await U('/' + id1, { subjectCode: CODE1, subjectName: 'E2E费用科目改名', mnemonicCode: 'GM', auxiliaryTypeId: 1, direction: 1 })
    check('更新后名称/助记码/核算项生效',
      upd?.data?.subjectName === 'E2E费用科目改名' && upd?.data?.mnemonicCode === 'GM' && upd?.data?.auxiliaryTypeName === '部门',
      `${upd?.data?.subjectName} / ${upd?.data?.mnemonicCode} / ${upd?.data?.auxiliaryTypeName}`)
    const kidRenamed = await T('/' + id2)
    check('下级科目全名随上级联动', kidRenamed?.data?.fullName === 'E2E费用科目改名/E2E费用下级', kidRenamed?.data?.fullName)
    const cleared = await U('/' + id1, { subjectCode: CODE1, subjectName: 'E2E费用科目改名', auxiliaryTypeId: null, direction: 1 })
    check('核算项可清空为「不核算」', cleared?.data?.auxiliaryTypeId == null && !cleared?.data?.auxiliaryTypeName,
      `aux=${cleared?.data?.auxiliaryTypeId} name=${cleared?.data?.auxiliaryTypeName}`)

    // ── 10. 越界保护：非费用类科目不可操作 ──
    const allSubj = await apiReq('GET', '/erp/finance/subject/tree?hierarchical=false&includeDisabled=true', null, token)
    const income = flatten(allSubj.data).find(x => x.subjectCode === '6001')   // 损益类贷方
    const asset = flatten(allSubj.data).find(x => x.subjectCode === '1001')    // 资产类
    if (income) {
      const touchedIncome = await U('/' + income.id, { subjectCode: '6001', subjectName: '主营业务收入' })
      check('收入类科目(5/2)不在本页可操作范围', touchedIncome.code !== 200 && /费用科目不存在/.test(touchedIncome.message || ''), touchedIncome.message)
    }
    if (asset) {
      const touchedAsset = await D('/' + asset.id)
      check('资产类科目(1/1)不在本页可操作范围', touchedAsset.code !== 200 && /费用科目不存在/.test(touchedAsset.message || ''), touchedAsset.message)
    }

    // ── 11. 停用/启用 + 显示停用过滤 ──
    await U('/' + id1 + '/enable?enabled=false')
    const withoutDisabled = await T('/tree', '?hierarchical=false&includeDisabled=false')
    const withDisabled = await T('/tree', '?hierarchical=false&includeDisabled=true')
    check('显示停用=否 过滤停用科目',
      !(withoutDisabled.data || []).some(x => x.id === id1) && (withDisabled.data || []).some(x => x.id === id1),
      `off=${(withoutDisabled.data || []).length} on=${(withDisabled.data || []).length}`)
    const enabled = await U('/' + id1 + '/enable?enabled=true')
    check('启用科目生效', enabled?.data?.isEnabled === true)

    // ── 12. 有子科目不可删除 / 删除子科目后上级恢复叶子 ──
    const delParent = await D('/' + id1)
    check('有子科目不可删除', delParent.code !== 200 && /子科目/.test(delParent.message || ''), delParent.message)
    const delChild = await D('/' + id2)
    check('删除子科目成功', delChild.code === 200, delChild.message)
    const parentBack = await T('/' + id1)
    check('上级科目恢复 isLeaf=true', parentBack?.data?.isLeaf === true, String(parentBack?.data?.isLeaf))

    // ── 13. 导出真实 xlsx ──
    const exp = await apiReq('GET', API + '/export?hierarchical=false', null, token, { binary: true })
    const isXlsx = exp.buffer?.slice(0, 2)?.toString() === 'PK' && /spreadsheetml/.test(exp.contentType)
    check('导出返回真实 xlsx 流', isXlsx, `${exp.contentType} ${exp.buffer?.length}B`)
    check('导出文件名为「费用类型_日期.xlsx」', /filename\*=UTF-8''/.test(exp.disposition) && /%E8%B4%B9%E7%94%A8%E7%B1%BB%E5%9E%8B/.test(exp.disposition), exp.disposition)
    const xlsxPath = path.join(SHOTS, 'api-export.xlsx')
    fs.writeFileSync(xlsxPath, exp.buffer)
    check('导出内容非空(>2KB)', exp.buffer.length > 2000, `${exp.buffer.length}B`)
    check('导出表头=科目编号/科目名称/核算项', /科目编号/.test(xlsxSummary(xlsxPath)) && /科目名称/.test(xlsxSummary(xlsxPath)) && /核算项/.test(xlsxSummary(xlsxPath)), xlsxSummary(xlsxPath).slice(0, 120))

    // ── 14. 删除顶级科目 ──
    const delRoot = await D('/' + id1)
    check('删除顶级科目成功', delRoot.code === 200, delRoot.message)
    const after = await T('/tree', '?hierarchical=false')
    check('删除后列表不再包含该科目', !flatten(after.data).some(x => x.subjectCode === CODE1))

    // 收尾：确保测试科目已清空（共享 devdb 基线干净）
    const remain = flatten((await T('/tree', '?hierarchical=false&includeDisabled=true')).data).filter(x => /^ET9/.test(String(x.subjectCode || '')))
    for (const r of remain) { await apiReq('DELETE', `${API}/${r.id}`, null, token).catch(() => {}) }
    if (remain.length) console.log(`（收尾清理残留 ${remain.length} 条）`)
  })
}

function xlsxSummary(file) {
  const { execFileSync } = require('child_process')
  try {
    return execFileSync('python', ['-c', `
import zipfile,re
z=zipfile.ZipFile(r'${file}')
txt=''
for n in z.namelist():
    if 'sharedStrings' in n:
        txt=z.read(n).decode('utf-8')
print('|'.join(re.findall(r'<t[^>]*>([^<]*)</t>', txt)[:12]))
`], { encoding: 'utf8', env: { ...process.env, PYTHONIOENCODING: 'utf-8' } }).trim()
  } catch (e) { return 'xlsx parse fail: ' + e.message }
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

  // 打印窗口拦截
  await page.addInitScript(() => {
    window.__printHtml = ''
    window.open = function () {
      return {
        document: { write: (h) => { window.__printHtml += h }, close: () => {} },
        focus: () => {}, print: () => { window.__printCalled = true },
      }
    }
  })

  // vite 代理指向 5655；本会话后端 5681，用精确前缀转发（勿用 **/api/** 会误伤 /src/api/*.ts）
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
    await page.goto(`${FE}/md/expense-type`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(4500)
    const onLogin = page.url().includes('/login')
    const noPage = (await page.locator('text=新增费用').count()) === 0
    if (onLogin || noPage) {
      if (attempt > 4) throw new Error('重登超过上限，页面未打开')
      curToken = await login()
      await page.evaluate(([t]) => { localStorage.setItem('token', t); localStorage.setItem('access_token', t); localStorage.setItem('tenantId', '1'); localStorage.setItem('tenantName', '系统租户') }, [curToken])
      return gotoPage(attempt + 1)
    }
  }

  await gotoPage()
  await page.waitForTimeout(1500)
  await page.screenshot({ path: `${SHOTS}/ui-01-page.png` })

  const txt = async (sel) => (await page.locator(sel).first().innerText().catch(() => '')).trim()
  const rowTexts = async () => page.$$eval('.ss-grid tbody tr', trs => trs.map(tr => (tr.innerText || '').trim().replace(/\s+/g, ' ')).filter(t => t && !/^(\d+)$/.test(t)))

  // ── 1. 工具栏 ──
  const btns = (await page.locator('button').allInnerTexts()).map(s => s.trim()).filter(Boolean)
  ck('工具栏含 新增费用 / 刷新 / 打印(F8) / 导出',
    ['新增费用', '刷新', '打印(F8)', '导出'].every(b => btns.some(t => t.includes(b))), btns.slice(0, 8).join(' | '))
  ck('无左侧分类面板（对标费用类型页无分类树）', (await page.locator('.category-node').count()) === 0)

  // ── 2. 查询区 ──
  ck('查询区含 筛选条件 / 显示停用 / 显示层次结构',
    (await page.locator('text=筛选条件').count()) > 0 &&
    (await page.locator('text=显示停用').count()) > 0 &&
    (await page.locator('text=显示层次结构').count()) > 0)
  ck('筛选输入框 placeholder = 请输入科目名称/编号', (await page.locator('input[placeholder="请输入科目名称/编号"]').count()) > 0)
  const cbs = await page.evaluate(() => [...document.querySelectorAll('input[type=checkbox]')].map(el => ({
    label: (el.closest('label') || el.parentElement)?.innerText || '', checked: el.checked,
  })))
  ck('「显示层次结构」默认勾选', cbs.some(c => /显示层次结构/.test(c.label) && c.checked))
  ck('「显示停用」默认不勾选', cbs.some(c => /显示停用/.test(c.label) && !c.checked))

  // ── 3. 数据表列 ──
  const headers = await page.$$eval('.ss-grid thead th', ths => ths.map(t => (t.innerText || '').trim()).filter(Boolean))
  ck('数据表含 操作/科目编号/科目名称/核算项',
    ['操作', '科目编号', '科目名称', '核算项'].every(h => headers.includes(h)), headers.join(' | '))
  ck('表头齿轮（列配置入口）存在', (await page.locator('.th-settings-btn').count()) > 0)

  // ── 4. 口径渲染：只显示费用类科目 ──
  let rows = await rowTexts()
  ck('列表渲染费用类科目（含 6602 管理费用）', rows.some(r => r.includes('6602')), rows.join(' | ').slice(0, 160))
  ck('列表不含收入类科目 6001 主营业务收入', !rows.some(r => r.includes('6001')))
  await page.screenshot({ path: `${SHOTS}/ui-02-rows.png` })

  // ── 5. 树形：折旧费 缩进在 管理费用 下 ──
  const depth = await page.evaluate(() => {
    const trs = [...document.querySelectorAll('.ss-grid tbody tr')]
    const child = trs.find(tr => /折旧费/.test(tr.innerText || ''))
    const cell = child?.querySelector('.subject-name-cell')
    return cell ? (cell.getAttribute('style') || '') : ''
  })
  ck('子科目按层级缩进（折旧费 paddingLeft>0）', /padding-left:\s*(1[6-9]|[2-9]\d|\d{3,})px/.test(depth), depth || '未找到折旧费行')
  ck('父科目行显示展开/收起箭头', (await page.locator('.tree-toggle').count()) >= 1, `carets=${await page.locator('.tree-toggle').count()}`)

  // ── 6. 列配置弹窗（个人配置/全局配置，3 列） ──
  await page.locator('.th-settings-btn').first().click({ force: true })
  await page.waitForTimeout(1800)
  await page.screenshot({ path: `${SHOTS}/ui-03-colconfig.png` })
  const dlgText = await page.locator('.ant-modal-content:visible').first().innerText().catch(() => '')
  ck('列配置弹窗含 个人配置/全局配置 双 Tab', /个人配置/.test(dlgText) && /全局配置/.test(dlgText))
  ck('列配置可配列 = 3 列（科目编号/科目名称/核算项）',
    ['科目编号', '科目名称', '核算项'].every(c => dlgText.includes(c)), dlgText.replace(/\s+/g, ' ').slice(0, 150))
  const closeCfg = page.locator('.ant-modal-close').last()
  if (await closeCfg.count()) await closeCfg.click({ force: true })
  await page.waitForTimeout(1200)

  // ── 7. 显示层次结构 关闭 → 平铺 ──
  await page.evaluate(() => {
    const cb = [...document.querySelectorAll('input[type=checkbox]')].find(el => /显示层次结构/.test((el.closest('label') || el.parentElement)?.innerText || ''))
    cb && cb.click()
  })
  await page.waitForTimeout(2200)
  ck('取消「显示层次结构」后无展开箭头（平铺）', (await page.locator('.tree-toggle').count()) === 0)
  const flatRows = await rowTexts()
  ck('平铺模式仍渲染全部费用类科目（含 6604 折旧费）', flatRows.some(r => r.includes('6604')), `rows=${flatRows.length}`)
  await page.screenshot({ path: `${SHOTS}/ui-04-flat.png` })
  await page.evaluate(() => {
    const cb = [...document.querySelectorAll('input[type=checkbox]')].find(el => /显示层次结构/.test((el.closest('label') || el.parentElement)?.innerText || ''))
    cb && cb.click()
  })
  await page.waitForTimeout(2200)

  // ── 8. 关键字查询 ──
  await page.fill('input[placeholder="请输入科目名称/编号"]', '管理费用')
  await page.locator('.btn-search').first().click()
  await page.waitForTimeout(2500)
  rows = await rowTexts()
  ck('按名称查询「管理费用」命中', rows.length >= 1 && rows.every(r => /管理费用|折旧费/.test(r)), rows.join('|').slice(0, 100))
  await page.fill('input[placeholder="请输入科目名称/编号"]', '')
  await page.locator('.btn-search').first().click()
  await page.waitForTimeout(2200)

  // ── 9. 新增费用（编辑器 = 会计科目 6 字段） ──
  await page.locator('.btn-add').click()
  await page.waitForTimeout(1800)
  await page.screenshot({ path: `${SHOTS}/ui-05-editor.png` })
  const editorText = await page.locator('.ant-modal-content:visible').first().innerText().catch(() => '')
  ck('编辑器字段 = 科目编号*/科目名称*/助记码/科目全名/核算项/借-贷',
    ['科目编号', '科目名称', '助记码', '科目全名', '核算项', '借/贷'].every(f => editorText.includes(f)),
    editorText.replace(/\s+/g, ' ').slice(0, 180))
  const dirChecked = await page.evaluate(() => {
    const el = [...document.querySelectorAll('.ant-modal-content input[type=radio]')].find(r => r.checked)
    return el ? el.value : ''
  })
  ck('新增费用预置方向 = 借方(1)', dirChecked === '1', `dir=${dirChecked}`)

  const stamp = Date.now().toString().slice(-6)
  const CODE = 'ET' + stamp
  const codeRe = new RegExp(CODE + '(?![\\d.])')
  const hasCode = (rs) => rs.some(r => codeRe.test(r))
  await page.locator('.ant-modal-content:visible input').nth(0).fill(CODE)
  await page.locator('.ant-modal-content:visible input').nth(1).fill('UI费用科目')
  await page.locator('.ant-modal-content:visible input').nth(2).fill('UIFY')
  await page.locator('.ant-modal-content:visible .ant-select').first().click()
  await page.waitForTimeout(900)
  await page.locator('.ant-select-item-option').filter({ hasText: '客户' }).first().click()
  await page.waitForTimeout(400)
  await page.locator('.ant-modal-content:visible .ant-modal-footer button.ant-btn-primary').last().click()
  await page.waitForTimeout(3200)
  rows = await rowTexts()
  const createdRow = rows.find(r => r.includes(CODE))
  ck('新增费用成功并出现在列表', !!createdRow, createdRow || `未找到 ${CODE}`)
  ck('新增行「核算项」显示 客户', !!createdRow && createdRow.includes('客户'), createdRow || '')
  await page.screenshot({ path: `${SHOTS}/ui-06-created.png` })

  // ── 10. 修改 ──
  const myRow = page.locator('.ss-grid tbody tr').filter({ hasText: CODE }).first()
  await myRow.locator('text=修改').first().click()
  await page.waitForTimeout(1800)
  const editPrefill = await page.locator('.ant-modal-content:visible input').nth(0).inputValue()
  ck('修改弹窗回填科目编号', editPrefill === CODE, editPrefill)
  await page.locator('.ant-modal-content:visible input').nth(1).fill('UI费用科目改名')
  await page.locator('.ant-modal-content:visible .ant-modal-footer button.ant-btn-primary').last().click()
  await page.waitForTimeout(3200)
  rows = await rowTexts()
  ck('修改费用科目名称生效', rows.some(r => r.includes('UI费用科目改名')))

  // ── 11. 新增下级 ──
  const row2 = page.locator('.ss-grid tbody tr').filter({ hasText: CODE }).first()
  await row2.locator('text=更多').first().click()
  await page.waitForTimeout(900)
  await page.locator('.ant-dropdown-menu-item').filter({ hasText: '新增下级' }).first().click()
  await page.waitForTimeout(1800)
  const childEditor = await page.locator('.ant-modal-content:visible').first().innerText().catch(() => '')
  ck('新增下级弹窗显示上级科目（只读）', /上级科目/.test(childEditor), childEditor.replace(/\s+/g, ' ').slice(0, 120))
  await page.locator('.ant-modal-content:visible input').nth(0).fill(CODE + '.01')
  await page.locator('.ant-modal-content:visible input').nth(1).fill('UI费用下级')
  await page.locator('.ant-modal-content:visible .ant-modal-footer button.ant-btn-primary').last().click()
  await page.waitForTimeout(3200)
  rows = await rowTexts()
  ck('新增下级成功（列表出现下级科目）', rows.some(r => r.includes('UI费用下级')))
  const parentRow = page.locator('.ss-grid tbody tr').filter({ hasText: CODE }).first()
  const delDisabled = await parentRow.locator('button:has-text("删除")').first().isDisabled().catch(() => false)
  ck('有子科目的行「删除」为禁用态', delDisabled)
  await page.screenshot({ path: `${SHOTS}/ui-07-child.png` })

  // ── 12. 更多 → 停用 / 显示停用 / 启用 ──
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
  await page.evaluate(() => {
    const cb = [...document.querySelectorAll('input[type=checkbox]')].find(el => /显示停用/.test((el.closest('label') || el.parentElement)?.innerText || ''))
    cb && cb.click()
  })
  await page.waitForTimeout(2500)
  rows = await rowTexts()
  const tagged = await page.locator('.subject-disabled').count()
  ck('勾选「显示停用」后停用科目重现且有停用样式', hasCode(rows) && tagged > 0, `rows=${rows.length} tagged=${tagged}`)
  await page.screenshot({ path: `${SHOTS}/ui-08-show-disabled.png` })
  const reEnableRow = page.locator('.ss-grid tbody tr').filter({ hasText: CODE }).first()
  await reEnableRow.locator('text=更多').first().click()
  await page.waitForTimeout(900)
  await page.locator('.ant-dropdown-menu-item').filter({ hasText: '启用' }).first().click()
  await page.waitForTimeout(2500)
  ck('更多→启用后科目恢复启用（停用样式消失）', (await page.locator('.subject-disabled').count()) === 0)
  await page.evaluate(() => {
    const cb = [...document.querySelectorAll('input[type=checkbox]')].find(el => /显示停用/.test((el.closest('label') || el.parentElement)?.innerText || ''))
    cb && cb.click()
  })
  await page.waitForTimeout(2000)

  // ── 13. 打印（F8） ──
  await page.keyboard.press('F8')
  await page.waitForTimeout(1500)
  const printHtml = await page.evaluate(() => window.__printHtml || '')
  ck('F8 打印生成真实打印文档（含表头与科目数据）',
    /费用类型/.test(printHtml) && /科目编号/.test(printHtml) && /科目名称/.test(printHtml) && new RegExp(CODE).test(printHtml),
    `html=${printHtml.length}B`)

  // ── 14. 导出（真实 xlsx 下载） ──
  const dlPromise = page.waitForEvent('download', { timeout: 20000 }).catch(() => null)
  await page.locator('button:has-text("导出")').first().click()
  const dl = await dlPromise
  let dlName = ''
  if (dl) { dlName = dl.suggestedFilename(); await dl.saveAs(path.join(SHOTS, 'ui-export.xlsx')).catch(() => {}) }
  ck('导出触发下载且文件名为 费用类型_*.xlsx', !!dl && /费用类型_.*\.xlsx$/.test(dlName), dlName || '未捕获下载')

  // ── 15. 行内删除闭环（UI 删下级 → 上级可删） ──
  const clickRowAction = (rowText, action) => page.evaluate(([m, a]) => {
    const all = [...document.querySelectorAll('.ss-grid tbody tr')]
    const tr = all.find(t => new RegExp(m).test(t.innerText || ''))
    if (!tr) return 'no-row'
    const btn = [...tr.querySelectorAll('button')].find(b => new RegExp(a).test((b.innerText || '').replace(/\s/g, '')))
    if (!btn) return 'no-btn'
    if (btn.disabled) return 'disabled'
    btn.click()
    return 'ok'
  }, [rowText, action])
  const confirmDel = async () => {
    try {
      await page.locator('.ant-modal-confirm-btns').first().waitFor({ state: 'visible', timeout: 6000 })
    } catch { return 'no-confirm' }
    const r = await page.evaluate(() => {
      const box = document.querySelector('.ant-modal-confirm-btns')
      if (!box) return 'no-box'
      const ok = [...box.querySelectorAll('button')].find(b => /确定|确认/.test((b.innerText || '').replace(/\s/g, '')))
      if (!ok) return 'no-ok'
      ok.click()
      return 'ok'
    })
    await page.waitForTimeout(2500)
    return r
  }
  const delChildState = await clickRowAction('UI费用下级', '^删除$')
  await page.waitForTimeout(1000)
  await confirmDel()
  rows = await rowTexts()
  ck('行内「删除」下级科目（含确认弹窗）生效', delChildState === 'ok' && !rows.some(r => r.includes('UI费用下级')), `click=${delChildState}`)

  let delParentState = 'no-row'
  for (let i = 0; i < 3 && delParentState === 'no-row'; i++) {
    delParentState = await clickRowAction(CODE + '(?![\\d.])', '^删除$')
    if (delParentState === 'no-row') await page.waitForTimeout(1500)
  }
  if (delParentState === 'ok') { await page.waitForTimeout(1000); await confirmDel() }

  // 兜底：接口清理残留（共享 devdb 基线干净）
  const tk = curToken
  const leftover = await apiReq('GET', `${API}/tree?hierarchical=false&includeDisabled=true`, null, tk)
  const remain = flatten(leftover?.data).filter(x => /^ET9/.test(String(x.subjectCode || '')) || /UI费用/.test(String(x.subjectName || '')))
    .sort((a, b) => (b.level || 0) - (a.level || 0))
  for (const r of remain) await apiReq('DELETE', `${API}/${r.id}`, null, tk).catch(() => {})
  if (remain.length) console.log(`（接口兜底清理残留 ${remain.length} 条）`)
  await page.locator('button:has-text("刷新")').first().click()
  await page.waitForTimeout(3000)
  rows = await rowTexts()
  ck('删除测试科目（清理完成）', !hasCode(rows), `click=${delParentState} 兜底=${remain.length}`)
  await page.screenshot({ path: `${SHOTS}/ui-09-final.png` })

  await browser.close()
  fs.writeFileSync(path.join(SHOTS, 'ui-results.json'), JSON.stringify(ui, null, 1))
  const ok = ui.filter(r => r.ok).length
  console.log(`\nUI 结果：${ok}/${ui.length} 通过`)
  ui.filter(r => !r.ok).forEach(f => console.log(' - ' + f.name + ' :: ' + f.detail))
  return ui
}

async function main() {
  if (MODE === 'api' || MODE === 'all') {
    console.log('=== API 验收（' + PORT + '） ===')
    await apiSuite()
    fs.writeFileSync(path.join(SHOTS, 'api-results.json'), JSON.stringify(results, null, 1))
    const ok = results.filter(r => r.ok).length
    console.log(`\nAPI 结果：${ok}/${results.length} 通过`)
    results.filter(r => !r.ok).forEach(f => console.log(' - ' + f.name + ' :: ' + f.detail))
    if (created.length) console.log('（已创建并删除的测试科目 id: ' + created.join(',') + '）')
  }
  if (MODE === 'ui' || MODE === 'all') {
    const ui = await uiSuite()
    const all = [...results, ...ui]
    const ok = all.filter(r => r.ok).length
    console.log(`\n合计：${ok}/${all.length} 通过`)
    if (all.some(r => !r.ok)) process.exitCode = 1
  }
  if (MODE === 'api') {
    if (results.some(r => !r.ok)) process.exitCode = 1
  }
}

main().catch(e => { console.error('FATAL', e.message); process.exit(1) })
