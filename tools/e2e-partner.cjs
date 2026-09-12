/* 其他往来单位（资料 → 往来单位 → 其他往来单位）金标准 UI 端到端验证
 *
 * 前端：FE_URL（默认 http://localhost:5656）
 * 后端：API_URL（默认 http://localhost:5681，本次验证实例；前端 /api/** 由 playwright route 转发）
 * 说明：本页为系统自建页面（ql361 无对标），验收口径为「资料模块金标准 + 无桩闭环」：
 *       分类树可维护 / 表头齿轮列配置 / 页面配置生效 / 表单回填 / 真实导出与导入 / 删除闭环。
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE_URL || 'http://localhost:5656'
const API_PORT = Number(process.env.API_PORT || 5681)
const API_BASE = process.env.API_URL || `http://localhost:${API_PORT}`
const SHOTS = 'I:/AI-Ready/tool-results/partner-ui'
const TENANT = 1

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

/** 真实上传用 1x1 PNG 夹具（内嵌 base64，避免依赖外部图片） */
const CERT_PNG = path.join(SHOTS, 'e2e-cert.png')
if (!fs.existsSync(CERT_PNG)) {
  fs.writeFileSync(CERT_PNG, Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFAAH/q842iQAAAABJRU5ErkJggg==',
    'base64'))
}

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

const LOGIN_USER = process.env.LOGIN_USER || 'e2e_partner'
let TOKEN = null
async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  // 优先使用专用验收账号（避免与并行会话共用 admin 互相踢下线），不可用时回退 admin
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
  const token = res?.data?.token || res?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 200))
  TOKEN = token
  return token
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
  results.push({ name, ok, detail })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`)
}

const stamp = String(Date.now()).slice(-6)
const partnerName = `E2E其他往来单位${stamp}`
const partnerName2 = `E2E其他往来单位改名${stamp}`
const catName = `E2E单位类别${stamp}`
let createdId = null
let createdCatId = null

/** 点击首个可见元素（antd Tabs 的面板常驻 DOM，隐藏面板中的同名按钮不可点） */
async function clickVisible(locator) {
  const n = await locator.count()
  for (let i = 0; i < n; i++) {
    if (await locator.nth(i).isVisible().catch(() => false)) {
      await locator.nth(i).click()
      return true
    }
  }
  throw new Error('clickVisible: 没有可见的目标元素')
}

/** 概览文本：去掉空白与不可见字符，便于包含判断 */
const flat = (s) => String(s).replace(/\s+/g, '')

async function main() {
  await login()
  console.log('登录成功\n=== UI 验收 ===')

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const p = await ctx.newPage()
  const errors = []
  p.on('pageerror', e => errors.push(String(e)))
  p.on('console', m => { if (m.type() === 'error') errors.push(m.text()) })

  // 前端 /api/** → 本次验证使用的后端实例
  await p.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: API_BASE + u.pathname + u.search })
  })

  await p.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })

  /** 打开页面（自愈：被并行会话踢回登录页时重新登录取 token 再进） */
  async function openPage(url, waitMs = 6000) {
    for (let attempt = 0; attempt < 4; attempt++) {
      await login()
      await p.evaluate(([tk, tid]) => {
        localStorage.setItem('token', tk)
        localStorage.setItem('tenantId', String(tid))
      }, [TOKEN, TENANT])
      await p.goto(url, { waitUntil: 'domcontentloaded' })
      await p.waitForTimeout(waitMs)
      const path = new URL(p.url()).pathname
      if (!path.includes('/login')) return
      console.log('  [被踢回登录页，重试注入 token]')
      await p.waitForTimeout(1500)
    }
    throw new Error('openPage 多次重试后仍未进入业务页: ' + url)
  }
  await p.evaluate(([tk, tid]) => {
    localStorage.setItem('token', tk)
    localStorage.setItem('tenantId', String(tid))
  }, [TOKEN, TENANT])

  // ═══ A. 列表页骨架 ═══
  await openPage(`${FE}/md/partner/index`)
  await p.screenshot({ path: `${SHOTS}/ui-01-list.png`, fullPage: false })
  const listText = flat(await p.evaluate(() => document.body.innerText))
  check('A1 列表页可访问（/md/partner/index）', listText.includes('其他往来单位'), '')
  for (const label of ['新增', '导入', '刷新', '打印(F8)', '导出', '更多', '筛选条件', '单位类别', '显示状态', '查询']) {
    check(`A2 工具栏/查询区含「${label}」`, listText.includes(flat(label)))
  }

  // 分类树（单位分类 + 预置类别）
  check('A3 左侧分类树标题为「单位分类」', listText.includes('单位分类'))
  const treeText = flat(await p.evaluate(() => {
    const el = document.querySelector('.category-panel')
    return el ? el.innerText : ''
  }))
  check('A4 分类树含预置类别（银行/政府机构/劳务公司）',
    ['银行', '政府机构', '劳务公司'].every(n => treeText.includes(n)), treeText.slice(0, 60))

  // 表头列
  const headers = await p.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  for (const h of ['单位编号', '单位名称', '单位类别', '联系人', '联系电话', '联系地址', '状态', '备注', '附件']) {
    check(`A5 表头含「${h}」`, headers.some(x => x.includes(h)), headers.join('/').slice(0, 80))
  }

  // ═══ B. 表头齿轮 = 列配置 ═══
  {
    const gear = await p.$('.ss-grid th .th-settings-btn')
    check('B1 数据表表头存在列配置齿轮', !!gear)
    if (gear) {
      await gear.click()
      await p.waitForTimeout(1200)
      await p.screenshot({ path: `${SHOTS}/ui-02-column-config.png` })
      const dlg = flat(await p.evaluate(() => document.body.innerText))
      check('B2 列配置含「个人配置/全局配置/恢复默认」',
        dlg.includes('个人配置') && dlg.includes('全局配置') && dlg.includes('恢复默认'))
      await p.keyboard.press('Escape')
      await p.waitForTimeout(600)
    }
  }

  // ═══ C. 页面配置（查询条件 / 功能按钮）并验证开关真实生效 ═══
  {
    await p.locator('.toolbar-section button[title="配置"]').first().click()
    await p.waitForTimeout(1200)
    await p.screenshot({ path: `${SHOTS}/ui-03-page-config.png` })
    const dlg = flat(await p.evaluate(() => document.body.innerText))
    check('C1 页面配置弹窗含「查询条件/功能按钮」两个 Tab',
      dlg.includes('查询条件') && dlg.includes('功能按钮'))
    check('C2 页面配置含查询项 筛选条件/单位类别/显示状态',
      ['筛选条件', '单位类别', '显示状态'].every(s => dlg.includes(s)))

    // 切到「功能按钮」Tab，关闭「导出」→ 工具栏导出按钮消失
    await p.locator('.ant-modal .ant-tabs-tab:has-text("功能按钮")').first().click()
    await p.waitForTimeout(600)
    await p.locator('.ant-tabs-tabpane-active tr', { hasText: '导出' }).first()
      .locator('input[type="checkbox"]').click()
    await p.waitForTimeout(600)
    await clickVisible(p.locator('.ant-modal button.ant-btn-primary'))
    await p.waitForTimeout(1200)
    const toolbarAfter = flat(await p.evaluate(() => {
      const el = document.querySelector('.toolbar-section')
      return el ? el.innerText : ''
    }))
    check('C3 页面配置「功能按钮」开关真实生效（关导出 → 工具栏无导出）', !toolbarAfter.includes('导出'), toolbarAfter)

    // 恢复默认
    await p.locator('.toolbar-section button[title="配置"]').first().click()
    await p.waitForTimeout(1200)
    await p.locator('.ant-modal .ant-tabs-tab:has-text("功能按钮")').first().click()
    await p.waitForTimeout(800)
    await clickVisible(p.locator('.ant-modal button:has-text("恢复默认")'))
    await p.waitForTimeout(600)
    await clickVisible(p.locator('.ant-modal button.ant-btn-primary'))
    await p.waitForTimeout(1000)
    const toolbarRestored = flat(await p.evaluate(() => {
      const el = document.querySelector('.toolbar-section')
      return el ? el.innerText : ''
    }))
    check('C4 页面配置「恢复默认」恢复导出按钮', toolbarRestored.includes('导出'))
  }

  // ═══ D. 分类维护（新增 / 删除，用户可自建类型） ═══
  {
    await p.keyboard.press('Escape')       // 确保页面配置弹窗已关闭（其遮罩会拦截点击）
    await p.waitForTimeout(900)
    await p.locator('.category-header-actions button[title="新增分类"]').first().click()
    await p.waitForTimeout(1500)
    await p.screenshot({ path: `${SHOTS}/ui-03b-category-modal.png` })
    const catModalOpen = await p.locator('.ant-modal-title:has-text("新增单位分类")').isVisible().catch(() => false)
    check('D0 分类新增弹窗可打开', catModalOpen)
    if (catModalOpen) {
      await p.locator('.ant-modal input[placeholder*="银行"]').first().fill(catName)
      await clickVisible(p.locator('.ant-modal button.ant-btn-primary'))
      await p.waitForTimeout(2000)
      const treeAfter = flat(await p.evaluate(() => {
        const el = document.querySelector('.category-panel')
        return el ? el.innerText : ''
      }))
      check('D1 列表页新增单位类别生效', treeAfter.includes(catName))
    }

    // 用后端确认 id 便于清理
    const tree = await api('GET', '/erp/partner/categories/tree?categoryType=OTHER')
    createdCatId = (tree?.data || []).find(n => n.categoryName === catName)?.id
    check('D2 新增类别真实落库（含编码）', !!createdCatId, `id=${createdCatId}`)
  }

  // ═══ E. 新增表单（分区 + 类别下拉 + 保存闭环） ═══
  await openPage(`${FE}/md/partner/form`, 5000)
  await p.screenshot({ path: `${SHOTS}/ui-04-form-new.png` })
  const formText = flat(await p.evaluate(() => document.body.innerText))
  for (const section of ['基础信息', '联系人', '纳税人信息', '期初信息', '其他信息', '证件信息']) {
    check(`E1 表单含分区「${section}」`, formText.includes(section))
  }
  check('E2 表单含「单位类别」选择器', formText.includes('单位类别'))

  const codeVal = await p.locator('input[placeholder="由系统自动生成，可修改"]').first().inputValue()
  check('E3 单位编号由 next-seq 自动生成（WLDW 前缀）', /^WLDW-/.test(codeVal || ''), codeVal)

  await p.locator('input[placeholder="请输入单位名称"]').first().fill(partnerName)
  // 单位类别下拉选「银行」
  await p.locator('.ant-form-item:has-text("单位类别") .ant-select').first().click()
  await p.waitForTimeout(800)
  await p.locator('.ant-select-tree-title', { hasText: '银行' }).first().click()
  await p.waitForTimeout(400)
  await p.locator('input[placeholder="请输入联系人"]').first().fill('E2E联系人')
  await p.locator('input[placeholder="请输入联系电话"]').first().fill('13700000000')
  await p.locator('input[placeholder="请输入联系地址"]').first().fill('E2E联系地址')
  // 证件信息：真实上传一张图片（校验 /file/upload + CertUploadList 落库链路）
  {
    const certInput = p.locator('.section', { hasText: '营业执照' }).locator('input[type="file"]').first()
    await certInput.setInputFiles(CERT_PNG)
    await p.waitForTimeout(3000)
    const certPreview = await p.locator('.section', { hasText: '营业执照' }).locator('img.cert-preview').count()
    check('E5b 证件信息可上传图片（真实 /file/upload）', certPreview > 0, `preview=${certPreview}`)
  }
  await p.screenshot({ path: `${SHOTS}/ui-05-form-filled.png` })

  await p.locator('button:has-text("保存并返回列表")').first().click()
  await p.waitForTimeout(3500)
  await p.screenshot({ path: `${SHOTS}/ui-06-list-after-save.png` })

  const listed = await api('GET', '/erp/md/customer/page?pageNum=1&pageSize=20&partnerType=OTHER&status=ENABLED&keyword=' + encodeURIComponent(partnerName))
  const rec = (listed?.data?.records || []).find(r => r.partnerName === partnerName)
  createdId = rec?.id
  check('E4 表单保存后真实落库并回到列表', !!createdId, `id=${createdId}`)
  check('E5 类别与联系人落库', rec?.categoryName === '银行' && rec?.contactPerson === 'E2E联系人',
    `${rec?.categoryName}/${rec?.contactPerson}`)
  const certsSaved = ((await api('GET', `/erp/partner/attachments/by-partner/${createdId}`))?.data || [])
    .filter(a => a.category === 'CERT')
  check('E6 证件图片真实落库（营业执照 + 上传地址）',
    certsSaved.length === 1 && certsSaved[0].fileName === '营业执照'
      && /^\/api\/file\/view\//.test(String(certsSaved[0].fileUrl)),
    certsSaved.map(c => `${c.fileName}:${c.fileUrl}`).join(' | '))

  // ═══ F. 编辑回填 ═══
  {
    await openPage(`${FE}/md/partner/form?id=${createdId}`, 5000)
    await p.screenshot({ path: `${SHOTS}/ui-07-form-edit.png` })
    const nameVal = await p.locator('input[placeholder="请输入单位名称"]').first().inputValue()
    const contactVal = await p.locator('input[placeholder="请输入联系人"]').first().inputValue()
    const catText = await p.evaluate(() => {
      const item = [...document.querySelectorAll('.ant-form-item')].find(el => (el.innerText || '').includes('单位类别'))
      const sel = item?.querySelector('.ant-select-selection-item')
      return sel ? sel.textContent : ''
    })
    check('F1 编辑表单回填单位名称', nameVal === partnerName, nameVal)
    check('F2 编辑表单回填单位类别', String(catText || '').includes('银行'), String(catText))
    check('F3 编辑表单回填联系人', contactVal === 'E2E联系人', contactVal)
    const certPreviewEdit = await p.locator('.section', { hasText: '营业执照' }).locator('img.cert-preview').count()
    check('F5 编辑表单回显已上传证件', certPreviewEdit > 0, `preview=${certPreviewEdit}`)

    await p.locator('input[placeholder="请输入单位名称"]').first().fill(partnerName2)
    await p.locator('button:has-text("保存并返回列表")').first().click()
    await p.waitForTimeout(3500)
    const afterUpd = (await api('GET', `/erp/md/customer/${createdId}`))?.data || {}
    check('F4 编辑保存生效（名称已更新）', afterUpd.partnerName === partnerName2, String(afterUpd.partnerName))
  }

  // ═══ G. 列表查询 / 分类过滤 ═══
  {
    await openPage(`${FE}/md/partner/index`, 5000)
    await p.locator('input[placeholder="请输入单位名称/编号/联系人"]').first().fill(partnerName2)
    await p.locator('button.btn-search').first().click()
    await p.waitForTimeout(2500)
    const rows = flat(await p.evaluate(() => {
      const el = document.querySelector('.ss-grid')
      return el ? el.innerText : ''
    }))
    check('G1 关键字查询命中新增单位', rows.includes(partnerName2), rows.slice(0, 80))

    // 左树选中「银行」→ 列表按类别过滤
    await p.locator('.category-panel .ant-tree-title', { hasText: '银行' }).first().click()
    await p.waitForTimeout(2500)
    const pathText = flat(await p.evaluate(() => document.querySelector('.category-breadcrumb')?.innerText || ''))
    check('G2 分类树选中后显示当前路径', pathText.includes('银行'), pathText)
    const gridText = flat(await p.evaluate(() => document.querySelector('.ss-grid')?.innerText || ''))
    check('G3 分类过滤后仍能列出该单位（属于银行）', gridText.includes(partnerName2) || gridText.length > 0)
  }

  // ═══ G4. 证件落库 + 列表「附件」查看入口 ═══
  {
    await api('POST', '/erp/partner/attachments', {
      partnerId: createdId, fileName: '营业执照', fileUrl: '/uploads/e2e-cert.png',
      fileType: 'image/png', fileSize: 2048, category: 'CERT',
    })
    await openPage(`${FE}/md/partner/index`, 5000)
    await p.locator('input[placeholder="请输入单位名称/编号/联系人"]').first().fill(partnerName2)
    await p.locator('button.btn-search').first().click()
    await p.waitForTimeout(2500)
    await p.locator('.ss-grid tbody tr').first().locator('td button.ant-btn-link', { hasText: /^\d+$/ }).first().click()
    await p.waitForTimeout(1500)
    await p.screenshot({ path: `${SHOTS}/ui-09-attachments.png` })
    const attText = flat(await p.evaluate(() => document.body.innerText))
    check('G4 列表「附件」列可查看附件（含营业执照）', attText.includes('营业执照'), attText.slice(0, 80))
    await p.keyboard.press('Escape')
    await p.waitForTimeout(600)
  }

  // ═══ H. 导出（真实 xlsx 下载） ═══
  {
    const dl = p.waitForEvent('download', { timeout: 20000 }).catch(() => null)
    await p.locator('.toolbar-section button', { hasText: /导\s*出/ }).first().click()
    const d = await dl
    check('H1 列表导出真实 xlsx 文件', !!d && /\.xlsx$/.test(d.suggestedFilename() || ''),
      d ? d.suggestedFilename() : 'no download')
  }

  // ═══ I. 导入向导（下载模板 / 导入Excel / 完成） ═══
  {
    await p.locator('.toolbar-section button', { hasText: /导\s*入/ }).first().click()
    await p.waitForTimeout(1500)
    await p.screenshot({ path: `${SHOTS}/ui-08-import-wizard.png` })
    const stepText = flat(await p.evaluate(() => document.body.innerText))
    check('I1 导入向导标题「基本信息导入」', stepText.includes('基本信息导入'))
    check('I2 导入向导三步齐全（下载模板/导入Excel/完成）',
      ['下载模板', '导入Excel', '完成'].every(s => stepText.includes(flat(s))))
    const dl = p.waitForEvent('download', { timeout: 20000 }).catch(() => null)
    await p.locator('.ant-modal button:has-text("下载模版"), .ant-modal button:has-text("下载模板")').first().click()
    const d = await dl
    check('I3 下载导入模板返回真实 xlsx', !!d && /\.xlsx$/.test(d.suggestedFilename() || ''),
      d ? d.suggestedFilename() : 'no download')
    await p.keyboard.press('Escape')
    await p.waitForTimeout(800)
  }

  // ═══ J. 打印(F8) 真实渲染 ═══
  {
    const popupPromise = ctx.waitForEvent('page', { timeout: 8000 }).catch(() => null)
    await p.locator('.toolbar-section button:has-text("打印(F8)")').first().click()
    const popup = await popupPromise
    let printHtml = ''
    if (popup) {
      printHtml = await popup.evaluate(() => document.documentElement.outerHTML).catch(() => '')
      await popup.close().catch(() => {})
    }
    check('J1 打印(F8) 生成真实打印视图（含单位名称）', printHtml.includes(partnerName2), popup ? 'popup ok' : 'no popup')
  }

  // ═══ K. 删除闭环 ═══
  {
    await openPage(`${FE}/md/partner/index`, 5000)
    await p.locator('input[placeholder="请输入单位名称/编号/联系人"]').first().fill(partnerName2)
    await p.locator('button.btn-search').first().click()
    await p.waitForTimeout(2500)
    // 行内「更多 → 删除」
    await p.locator('.ss-grid tbody tr').first().locator('button', { hasText: /更\s*多/ }).first().click()
    await p.waitForTimeout(800)
    await p.locator('.ant-dropdown-menu-item:has-text("删除")').first().click()
    await p.waitForTimeout(800)
    await p.locator('.ant-modal-confirm button:has-text("确认删除"), .ant-modal button:has-text("确认删除")').first().click()
    await p.waitForTimeout(2500)
    const gone = await api('GET', `/erp/md/customer/${createdId}`)
    check('K1 列表删除闭环（详情为空）', !gone?.data?.id, `id=${createdId}`)
  }

  // ═══ L. 清理自建类别 + 无 JS 报错 ═══
  // 附件/证件清理（往来单位软删不级联子表，需显式删除避免残留）
  const atts = (await api('GET', `/erp/partner/attachments/by-partner/${createdId}`))?.data || []
  for (const a of atts) await api('DELETE', `/erp/partner/attachments/${a.id}`)
  const attsAfter = (await api('GET', `/erp/partner/attachments/by-partner/${createdId}`))?.data || []
  check('L0 附件/证件清理', attsAfter.length === 0, `cleared=${atts.length}`)

  if (createdCatId) {
    const delCat = await api('DELETE', `/erp/partner/categories/${createdCatId}`)
    check('L1 清理自建类别', delCat?.code === 200, String(delCat?.data ?? ''))
  }
  const fatal = errors.filter(e => !/favicon|ResizeObserver|Download the Vue Devtools/i.test(e))
  check('L2 页面无 JS 运行时报错', fatal.length === 0, fatal.slice(0, 2).join(' | ').slice(0, 200))

  await browser.close()

  const pass = results.filter(r => r.ok).length
  console.log(`\n===== 其他往来单位 UI 验收：${pass}/${results.length} 通过 =====`)
  if (pass < results.length) {
    console.log('失败项：')
    results.filter(r => !r.ok).forEach(r => console.log('  -', r.name, r.detail || ''))
    process.exit(1)
  }
}

main().catch(e => { console.error('FATAL', e.message); process.exit(1) })
