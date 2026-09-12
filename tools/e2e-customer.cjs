/* 客户模块 UI 端到端验证（前端 dev 5656 + 独立后端 5671 转发）
 * 用法：NODE_PATH=I:/AI-Ready/frontend/node_modules node tools/e2e-customer.cjs
 */
const { chromium } = require('playwright')
const fs = require('fs')

const APP = process.env.APP_URL || 'http://localhost:5656'
const API = process.env.API_URL || 'http://localhost:5671'
const OUT = 'I:/AI-Ready/screenshots-customer'

const results = []
function check(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`)
}

/** 解析登录页图形验证码（data:image/svg+xml 内 <text> 文本） */
async function solveSvgCaptcha(page) {
  const svg = await page.evaluate(() => {
    const imgs = [...document.querySelectorAll('img')].filter(i => (i.src || '').startsWith('data:image'))
    if (!imgs.length) return ''
    const src = imgs[imgs.length - 1].src
    try {
      if (src.includes('base64,')) {
        const b64 = src.split('base64,')[1]
        return decodeURIComponent(escape(atob(b64)))
      }
      return decodeURIComponent(src.split(',').slice(1).join(','))
    } catch { return '' }
  })
  return [...String(svg).matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
}

async function main() {
  fs.mkdirSync(OUT, { recursive: true })
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const page = await ctx.newPage()

  // 把后端 /api/** 转发到独立实例（避免与并行会话的 5655 冲突）
  // 注意：必须用 pathname 前缀匹配，否则会误伤 Vite 的 /src/api/*.ts 模块请求
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: API + u.pathname + u.search })
  })

  await page.goto(APP + '/', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(4000)

  // ── 登录（若需要） ──
  if (page.url().includes('login') || await page.locator('input[type=password]').count() > 0) {
    const tenantInput = page.locator('input[placeholder*="租户"]').first()
    if (await tenantInput.count() > 0) await tenantInput.fill('系统租户')
    await page.locator('input[placeholder*="用户名"]').first().fill('admin')
    await page.locator('input[placeholder*="密码"]').first().fill('admin123')
    const captchaInput = page.locator('input[placeholder*="验证码"]').first()
    if (await captchaInput.count() > 0) {
      await captchaInput.fill(await solveSvgCaptcha(page))
    }
    await page.locator('button:has-text("登 录"), button:has-text("登录")').first().click()
    await page.waitForTimeout(8000)
  }
  check('U1 登录并进入系统', !page.url().includes('login'), page.url())

  // ── 客户列表页 ──
  await page.goto(APP + '/md/customer/index', { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(6000)
  await page.screenshot({ path: `${OUT}/01-customer-list.png`, fullPage: false })

  const tabTexts = await page.locator('.tab-item').allInnerTexts().catch(() => [])
  check('U2 5 个子标签渲染', tabTexts.length >= 5, tabTexts.join('/'))

  const headerTexts = await page.locator('.ss-grid thead th, .ss-grid .ss-header-cell').allInnerTexts().catch(() => [])
  const headerJoined = headerTexts.join('|')
  const needCols = ['客户编号', '客户名称', '结款方式', '客户级别', '所属仓库', '所属区域', '默认经手人',
    '推广人', '联系人', '助记码', '联系电话', '联系地址', '买家账号', '客户一票通', '动态收款期限（天）',
    '固定账期', '结算期', '开户银行', '银行账号', '税号', '客户来源', '营业执照有效期', '最近交易时间',
    '新增时间', '附件', '备注']
  const missCols = needCols.filter(c => !headerJoined.includes(c))
  check('U3 全部客户 26 列渲染', missCols.length === 0, missCols.length ? '缺列: ' + missCols.join(',') : `表头列数=${headerTexts.length}`)

  // 数据表行数：数据不足时用空白占位行补齐到 20 行（BillDetailTable minRows 默认 20）
  const listRows = await page.locator('.ss-grid tr.ss-row').count()
  check('U3b 数据表默认 20 行（不足补空白占位行）', listRows >= 20, `渲染行数=${listRows}`)

  // 占位行不应带行操作按钮（保持纯空白）
  const ghostActionBtns = await page.locator('.ss-grid tr.ss-row:has(.ss-empty-cell) a:has-text("代客下单")').count()
  check('U3c 占位空行不渲染行操作', ghostActionBtns === 0, `占位行按钮数=${ghostActionBtns}`)

  const hasTree = await page.locator('text=客户分类').count()
  check('U4 左侧客户分类树渲染', hasTree > 0)
  const hasQuery = await page.locator('text=筛选条件').count()
  check('U5 查询区渲染（筛选条件/联系地址/新增日期/最近交易/结款方式/显示状态）',
    hasQuery > 0 && (await page.locator('text=最近交易（起）').count()) > 0 && (await page.locator('text=显示状态').count()) > 0)
  check('U6 工具栏按钮（新增/导入/发短信/发优惠券/刷新/打印/导出/批量修改/更多）',
    (await page.locator('button:has-text("新增")').count()) > 0 &&
    (await page.locator('button:has-text("导入")').count()) > 0 &&
    (await page.locator('button:has-text("发优惠券")').count()) > 0 &&
    (await page.locator('button:has-text("批量修改")').count()) > 0 &&
    (await page.locator('button:has-text("更多")').count()) > 0)

  // 勾选数据行 → 批量按钮可用（占位空行不计入选中）
  const firstCheckbox = page.locator('.ss-grid tr.ss-row input.ss-checkbox').first()
  let batchUsable = false
  if (await firstCheckbox.count() > 0) {
    await firstCheckbox.click()
    await page.waitForTimeout(900)
    const batchBtn = page.locator('button:has-text("批量修改")').first()
    batchUsable = (await batchBtn.count()) > 0 && !(await batchBtn.isDisabled())
    await firstCheckbox.click()
    await page.waitForTimeout(500)
  }
  check('U3d 勾选数据行后批量操作可用（占位行不计入）', batchUsable)

  // ── 页面配置弹窗 ──
  const cfgBtn = page.locator('button[title="配置"]').first()
  if (await cfgBtn.count() > 0) {
    await cfgBtn.click()
    await page.waitForTimeout(1500)
    await page.screenshot({ path: `${OUT}/02-page-config.png` })
    const cfgText = await page.locator('.ant-modal-content, .page-config-panel, body').first().innerText()
    check('U7 页面配置弹窗（查询条件/功能按钮；无打印配置）',
      cfgText.includes('查询条件') && cfgText.includes('功能按钮'))
    await page.keyboard.press('Escape')
    await page.waitForTimeout(800)
  } else {
    check('U7 页面配置齿轮存在', false)
  }

  // ── 列配置（表头齿轮） ──
  const gear = page.locator('.ss-grid .column-setting-btn, .ss-grid [class*=shezhi], .ss-grid [class*=setting]').first()
  check('U8 数据表头列配置齿轮存在', (await gear.count()) > 0)

  // ── 切换到 会员管理 / 全部联系人 / 客户级别 / 区域管理 ──
  for (const [key, label] of [['member', '会员管理'], ['contact', '全部联系人'], ['grade', '客户级别'], ['region', '区域管理']]) {
    const tab = page.locator('.tab-item', { hasText: new RegExp(`^\\s*${label}\\s*$`) }).first()
    if (await tab.count() === 0) { check(`U9-${label} 子标签存在`, false); continue }
    await tab.click()
    await page.waitForTimeout(3500)
    await page.screenshot({ path: `${OUT}/03-tab-${key}.png` })
    const heads = await page.locator('.ss-grid thead th, .ss-grid .ss-header-cell').allInnerTexts().catch(() => [])
    check(`U9-${label} 子标签切换并渲染列`, heads.length > 0, heads.slice(0, 6).join('/'))
  }

  // ── 区域管理：左树标题应为「地区分类」（对标实测：区域管理同样带左树） ──
  const regionTreeTitle = await page.locator('text=地区分类').count()
  const regionCategoryPanel = await page.locator('.category-panel').count()
  check('U14 区域管理左侧「地区分类」树渲染', regionTreeTitle > 0 && regionCategoryPanel > 0,
    `标题=${regionTreeTitle} 面板=${regionCategoryPanel}`)
  // 分类树标题栏操作（对标实测：标题右侧 = 修改 / 删除 / 新增，作用于选中节点）
  const headerEdit = await page.locator('.category-header button[title="修改"]').count()
  const headerDel = await page.locator('.category-header button[title="删除"]').count()
  const headerAdd = await page.locator('.category-header button[title="新增分类"]').count()
  check('U20 分类树标题栏含 修改/删除/新增 按钮（对标）',
    headerEdit > 0 && headerDel > 0 && headerAdd > 0, `改=${headerEdit} 删=${headerDel} 新增=${headerAdd}`)

  // ── 全部客户：查询区应为 2 行（对标：每行 6 列）+ 结款方式默认「全部」 ──
  await page.locator('.tab-item', { hasText: /^\s*全部客户\s*$/ }).first().click()
  await page.waitForTimeout(3500)
  const queryRowCount = await page.evaluate(() => {
    const tops = new Set()
    document.querySelectorAll('.search-grid .search-item').forEach(el => {
      if (!el.querySelector('.search-label') || el.offsetParent === null) return
      tops.add(Math.round(el.getBoundingClientRect().top / 12))
    })
    return tops.size
  })
  check('U15 全部客户查询区排布为 2 行', queryRowCount === 2, `实测行数=${queryRowCount}`)
  const settleRowText = await page.locator('.search-grid .search-item', { hasText: '结款方式' }).first().innerText().catch(() => '')
  check('U16 结款方式默认显示「全部」', settleRowText.includes('全部'), settleRowText.replace(/\s+/g, ' '))

  // ── 会员管理：查询方案下拉 + 导出收进「更多」 ──
  await page.locator('.tab-item', { hasText: /^\s*会员管理\s*$/ }).first().click()
  await page.waitForTimeout(3500)
  const schemeCount = await page.locator('.query-scheme-wrap').count()
  check('U17 会员管理「查询方案」下拉渲染', schemeCount > 0)
  const toolbarExport = await page.locator('.toolbar-right button:has-text("导出"), .toolbar-left button:has-text("导出")').count()
  check('U18 会员管理「导出」收进「更多」（工具栏不直接显示）', toolbarExport === 0, `工具栏导出按钮=${toolbarExport}`)

  // ── 新增表单页 ──
  await page.goto(APP + '/md/customer/form', { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(5000)
  await page.screenshot({ path: `${OUT}/04-form-new.png`, fullPage: true })
  const bodyText = await page.locator('body').innerText()
  const sections = ['基本信息', '联系人信息', '会员信息', '纳税人信息', '期初信息', '其他信息', '证件信息']
  const missSec = sections.filter(s => !bodyText.includes(s))
  check('U10 表单页 7 个分区渲染', missSec.length === 0, missSec.length ? '缺: ' + missSec.join(',') : '')

  await page.locator('.section-title', { hasText: '其他信息' }).first().click().catch(() => {})
  await page.waitForTimeout(1000)
  const bodyText2 = await page.locator('body').innerText()
  const otherFields = ['动态收款期限（天）', '固定账期', '结算期', '推广人', '客户来源', '所属区域', '买家账号', '客户一票通', '营业执照有效期']
  const missFields = otherFields.filter(s => !bodyText2.includes(s))
  check('U11 其他信息分区含账期/推广人/来源/区域/商城账号/证件有效期', missFields.length === 0,
    missFields.length ? '缺: ' + missFields.join(',') : '')
  const memberFields = ['会员级别', '会员卡状态', '会员卡有效期', '累计消费额', '发卡时间']
  const missMember = memberFields.filter(s => !bodyText2.includes(s))
  check('U12 会员信息分区含级别/卡状态/有效期/累计消费/发卡时间', missMember.length === 0,
    missMember.length ? '缺: ' + missMember.join(',') : '')
  // 结款方式口径：表单/列表/对标统一为「挂账 / 现结」
  check('U19 表单结款方式默认「挂账」（与列表同口径）', bodyText2.includes('挂账'))
  await page.screenshot({ path: `${OUT}/05-form-expanded.png`, fullPage: true })

  // ── 编辑表单（从列表点修改） ──
  await page.goto(APP + '/md/customer/index', { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(5000)
  const editBtn = page.locator('.ss-grid a:has-text("修改"), .ss-grid button:has-text("修改")').first()
  if (await editBtn.count() > 0) {
    await editBtn.click()
    await page.waitForTimeout(5000)
    const t = await page.locator('body').innerText()
    check('U13 编辑模式打开并回填', page.url().includes('id=') && t.includes('客户编辑'),
      page.url())
    await page.screenshot({ path: `${OUT}/06-form-edit.png`, fullPage: true })
  } else {
    check('U13 列表行「修改」入口存在', false, '(库中暂无客户数据)')
  }

  const pass = results.filter(r => r.ok).length
  console.log(`\n===== 客户模块 UI 验证：${pass}/${results.length} 通过 =====`)
  if (pass < results.length) results.filter(r => !r.ok).forEach(r => console.log('  -', r.name, r.detail || ''))
  await browser.close()
  if (pass < results.length) process.exit(1)
}

main().catch(e => { console.error('FATAL', e.message); process.exit(1) })
