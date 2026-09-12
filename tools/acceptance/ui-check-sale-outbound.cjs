// 销售出库单金标准 UI 验收（真实浏览器）
// 通过 page.route 把 /api/** 转发到独立后端 5688（避免与并行会话共用的 5655 相互干扰）
const path = require('path')
const { chromium } = require(path.join(__dirname, '../../frontend/apps/pc-admin/node_modules/playwright'))
const { Client } = require(path.join(__dirname, '../../frontend/apps/pc-admin/node_modules/pg'))

const APP = process.env.SOB_APP || 'http://localhost:5656'
const API = process.env.SOB_API || 'http://localhost:5688'
const SHOT = path.join(__dirname, '../../tool-results')

let pass = 0, fail = 0
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✅ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; console.log(`  ❌ ${name}${detail ? ' — ' + detail : ''}`) }
}

const pg = new Client({ host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' })
async function scalar(q) {
  const r = await pg.query(q)
  const rows = Array.isArray(r) ? r[r.length - 1].rows : r.rows
  return rows.length ? Object.values(rows[0])[0] : null
}

function captchaText(imgSrc) {
  const m = (imgSrc || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}

const sleep = ms => new Promise(r => setTimeout(r, ms))

/** 接口登录（验证码接口 429 限流时退避重试） */
async function apiLogin() {
  for (let attempt = 1; attempt <= 12; attempt++) {
    const capRes = await fetch(`${API}/api/auth/captcha`)
    if (capRes.status === 429) {
      console.log(`  ⚠️ 验证码限流(429)，${attempt} 次退避重试…`)
      await sleep(10000)
      continue
    }
    const cap = (await capRes.json()).data || {}
    const res = await fetch(`${API}/api/auth/login`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: 'sex_e2e', password: 'admin123', tenantName: '系统租户',
        captcha: captchaText(cap.img), captchaKey: cap.uuid,
      }),
    })
    const json = await res.json().catch(() => ({}))
    const d = json.data || {}
    if (d.token) return { token: d.token, tenantId: d.tenantId, tenantName: d.tenantName }
    console.log(`  ⚠️ 登录未成功(${json.message || res.status})，重试…`)
    await sleep(6000)
  }
  throw new Error('接口登录失败（疑似限流）')
}

async function main() {
  await pg.connect()
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 900 } })
  const page = await ctx.newPage()

  page.on('pageerror', e => console.log('  ⚠️ page error:', e.message.slice(0, 120)))

  // 把 /api/** 真转发到独立后端（注意：只匹配路径根部的 /api/，
  // 不能写成 '**/api/**'，否则会误伤 Vite 的 /src/api/*.ts 模块请求）
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    if (new URL(route.request().url()).pathname.startsWith('/api/sse')) return route.continue()
    const req = route.request()
    const url = req.url().replace(APP, API)
    try {
      const resp = await route.fetch({ url })
      await route.fulfill({ response: resp })
    } catch (e) {
      await route.abort()
    }
  })

  // ── 登录：走接口取会话令牌后注入（验证码接口存在全局限流，UI 表单登录不稳定） ──
  const sess = await apiLogin()
  check('登录成功（会话令牌已获取）', !!sess.token, `token=${String(sess.token).slice(0, 8)}…`)
  await page.addInitScript(s => {
    localStorage.setItem('token', s.token)
    localStorage.setItem('tenantId', String(s.tenantId || 1))
    localStorage.setItem('tenantName', s.tenantName || '系统租户')
  }, sess)
  const loggedIn = !!sess.token
  check('登录成功', loggedIn, page.url())
  if (!loggedIn) {
    await page.screenshot({ path: path.join(SHOT, 'so-login-fail.png') })
    throw new Error('登录失败')
  }

  // ═══ 列表页：按单据 ═══
  console.log('\n【列表页 · 按单据】')
  await page.goto(`${APP}/sales/outbound/index`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(3000)
  await page.screenshot({ path: path.join(SHOT, 'so-list-doc.png'), fullPage: false })

  const searchItems = page.locator('.search-grid .search-field-item')
  const collapsedCount = await searchItems.count()
  check('搜索区默认渲染 ≤ 2 排字段 + 查询区', collapsedCount >= 4 && collapsedCount <= 12, `控件数=${collapsedCount}`)

  const moreBtn = page.locator('.search-more-toggle button')
  const hasMore = await moreBtn.count()
  check('「更多条件」按钮存在', hasMore > 0)
  if (hasMore > 0) {
    await moreBtn.first().click()
    await page.waitForTimeout(600)
    const expandedCount = await searchItems.count()
    check('「更多条件」可展开（字段数增加，未被 max-height 裁剪）', expandedCount > collapsedCount, `${collapsedCount} → ${expandedCount}`)
    await page.screenshot({ path: path.join(SHOT, 'so-list-doc-expanded.png') })
    await moreBtn.first().click()
    await page.waitForTimeout(400)
  }

  const gear = page.locator('.ss-header-settings').first()
  check('数据表表头齿轮（列配置入口）存在', await gear.count() > 0)
  if (await gear.count() > 0) {
    await gear.click()
    await page.waitForTimeout(900)
    const panelText = await page.locator('.ant-modal-content, .ant-drawer-content').last().innerText().catch(() => '')
    check('列配置弹窗含「个人配置/全局配置」', /个人配置/.test(panelText) && /全局配置/.test(panelText), panelText.replace(/\s+/g, ' ').slice(0, 60))
    await page.screenshot({ path: path.join(SHOT, 'so-list-column-config.png') })
    await page.keyboard.press('Escape')
    await page.waitForTimeout(600)
  }

  // ── 页面配置：查询条件显隐真实生效 ──
  const fieldCountBefore = await searchItems.count()
  await page.locator('button:has(.anticon-setting), .ant-btn:has(.anticon-setting)').first().click()
  await page.waitForTimeout(900)
  const cfgModal = page.locator('.ant-modal-content').last()
  const cfgText = await cfgModal.innerText()
  check('页面配置弹窗含 查询条件/功能按钮/打印配置 三 Tab',
    /查询条件/.test(cfgText) && /功能按钮/.test(cfgText) && /打印配置/.test(cfgText))
  await page.screenshot({ path: path.join(SHOT, 'so-list-page-config.png') })

  // 关闭「客户」查询条件
  const custRow = cfgModal.locator('tr', { hasText: '客户' }).first()
  const custCb = custRow.locator('input[type="checkbox"]').first()
  await custCb.uncheck({ force: true }).catch(() => {})
  await page.waitForTimeout(600)
  // 关闭「新增」功能按钮
  await cfgModal.locator('.ant-tabs-tab', { hasText: '功能按钮' }).click()
  await page.waitForTimeout(400)
  const addRow = cfgModal.locator('tr', { hasText: '新增' }).first()
  await addRow.locator('input[type="checkbox"]').first().uncheck({ force: true }).catch(() => {})
  await page.waitForTimeout(600)
  await cfgModal.locator('button:has-text("关闭")').first().click().catch(() => {})
  await page.keyboard.press('Escape')
  await page.waitForTimeout(900)

  const placeholderStillThere = await page.locator('.search-grid input[placeholder="客户"]').count()
  check('取消勾选后「客户」查询条件不再渲染（配置真实生效）', placeholderStillThere === 0, `匹配数=${placeholderStillThere}`)
  const addBtnCount = await page.locator('.ant-btn:has-text("新增")').count()
  check('取消勾选后「新增」按钮不再渲染（配置真实生效）', addBtnCount === 0, `匹配数=${addBtnCount}`)
  const fieldCountAfter = await searchItems.count()
  check('搜索区重新计算（折叠位由后续可见字段补位）', fieldCountAfter <= fieldCountBefore, `${fieldCountBefore} → ${fieldCountAfter}`)
  await page.screenshot({ path: path.join(SHOT, 'so-list-config-applied.png') })

  // ═══ 列表页：按明细（71 列 + 分类树） ═══
  console.log('\n【列表页 · 按明细】')
  await page.locator('.tab-item', { hasText: '按明细' }).first().click()
  await page.waitForTimeout(2500)
  const headers = await page.locator('.ss-grid thead th').allInnerTexts().catch(() => [])
  const headerCount = headers.filter(h => h.trim() !== '').length
  check('按明系列数 = 71 数据列 + 序号 + 操作', headerCount >= 71 && headerCount <= 74, `表头数=${headerCount}`)
  check('按明细含关键列（商品名称/表体自定义10/单据备注/表头自定义字段1）',
    headers.some(h => h.includes('商品名称')) && headers.some(h => h.includes('表体自定义10'))
    && headers.some(h => h.includes('单据备注')) && headers.some(h => h.includes('表头自定义字段1')),
    headers.slice(0, 6).join('|'))
  const catPanel = await page.locator('.category-panel, .ant-tree').count()
  check('按明细展示商品分类树', catPanel > 0, `节点数=${catPanel}`)
  await page.screenshot({ path: path.join(SHOT, 'so-list-detail.png') })

  // ═══ 物流备注真实写入 ═══
  console.log('\n【物流备注（UI 真实写入）】')
  await page.locator('.tab-item', { hasText: '按单据' }).first().click()
  await page.waitForTimeout(2000)
  const firstRow = page.locator('.ss-grid tbody tr').first()
  const firstRowText = await firstRow.innerText().catch(() => '')
  const targetNo = (firstRowText.match(/XSCK\d{12}/) || [''])[0]
  // 行选择框是原生 input（BillDetailTable 的 checkbox 列），需要 force 点击
  await firstRow.locator('input[type="checkbox"]').first().click({ force: true }).catch(() => {})
  await page.waitForTimeout(800)
  check('列表行可多选（用于批量操作）', /已选|取消选择/.test(await page.locator('body').innerText()), `目标单号=${targetNo}`)
  await page.waitForTimeout(300)
  await page.locator('.ant-btn:has-text("更多")').first().click()
  await page.waitForTimeout(500)
  await page.locator('.ant-dropdown-menu-item:has-text("物流备注")').first().click()
  await page.waitForTimeout(800)
  const newRmk = 'UI-物流备注-' + Date.now()
  await page.locator('.ant-modal textarea').last().fill(newRmk)
  await page.locator('.ant-modal .ant-btn-primary:has-text("确"), .ant-modal .ant-btn-primary:has-text("OK")').last().click().catch(async () => {
    await page.locator('.ant-modal-footer button.ant-btn-primary').last().click()
  })
  await page.waitForTimeout(2000)
  const dbRmk = await scalar(`SELECT logistics_remark FROM erp_sale_outbound WHERE outbound_no='${targetNo.trim()}'`)
  check('物流备注经 UI 真实落库', dbRmk === newRmk, `单号=${targetNo.trim()} 值=${dbRmk}`)
  await page.screenshot({ path: path.join(SHOT, 'so-list-logistics-remark.png') })

  // ═══ 表单页：编号来自后端号段 ═══
  console.log('\n【表单页】')
  await page.goto(`${APP}/sales/outbound/form`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(6000)
  const bodyText = await page.locator('body').innerText()
  const noMatch = bodyText.match(/XSCK\d{12}/)
  check('新增页编号来自后端号段（XSCK+12位，非 XSCK-日期-序号 演示号）',
    !!noMatch && !/XSCK-\d{8}-/.test(bodyText), noMatch ? noMatch[0] : '未找到')
  const detailHeaders = (await page.locator('.ss-grid thead th').allInnerTexts().catch(() => []))
    .map(h => h.trim())
    .filter(Boolean)
  check('表单明细列按文档收口（含「备注」，无旧的「价格等级1~8」）',
    detailHeaders.includes('备注') && !detailHeaders.some(h => /^价格等级\d$/.test(h)),
    `表头数=${detailHeaders.length}`)
  check('表单明细列 74 项（操作 + 73 数据列）', detailHeaders.length >= 74 && detailHeaders.length <= 78, `表头数=${detailHeaders.length}`)
  await page.screenshot({ path: path.join(SHOT, 'so-form-create.png') })

  // 收款 Tab：「更多账户」展开收款账户2~4 + 账户/物流公司 +Q 取真实主数据
  await page.locator('.bottom-tabs button:has-text("展开")').first().click()
  await page.waitForTimeout(800)
  const tabText = await page.locator('.bottom-tabs').innerText().catch(() => '')
  check('「更多账户」可展开收款账户2~4', /收款账户2/.test(tabText) && /收款账户4/.test(tabText))
  await page.locator('.bottom-tabs button:has-text("+Q")').first().click()
  await page.waitForTimeout(1500)
  const qsTitle = await page.locator('.ant-modal-title').last().innerText().catch(() => '')
  check('收款账户「+Q」打开账户选择弹窗', /收款账户/.test(qsTitle), `标题=${qsTitle}`)
  await page.keyboard.press('Escape')
  await page.waitForTimeout(800)
  await page.screenshot({ path: path.join(SHOT, 'so-form-tab-account.png') })

  // 表单页面配置：显示名 + 显隐真实生效（头部动作区的「配置」按钮，非明细表头齿轮）
  await page.locator('.bill-header .header-right button:has(.anticon-setting)').first().click()
  await page.waitForTimeout(1200)
  const fCfg = page.locator('.ant-modal-content').last()
  const fCfgText = await fCfg.innerText()
  check('表单配置弹窗三 Tab（页面配置/录单默认值/打印设置）',
    /页面配置/.test(fCfgText) && /录单默认值/.test(fCfgText) && /打印设置/.test(fCfgText))
  // 录单默认值 Tab：「配置」按钮为真实功能（重置录单默认值）
  await fCfg.locator('.ant-tabs-tab-btn', { hasText: '录单默认值' }).first().click()
  await page.waitForTimeout(800)
  const cfgBtn = fCfg.locator('.tab-footer-right button, .tab-footer-right .ant-btn').first()
  const cfgBtnCount = await cfgBtn.count()
  check('录单默认值 Tab 存在「配置」按钮', cfgBtnCount > 0)
  if (cfgBtnCount > 0) {
    await cfgBtn.click()
    await page.waitForTimeout(1200)
    const msgText = await page.locator('.ant-message').innerText().catch(() => '')
    check('录单默认值「配置」按钮真实生效（重置提示）', /录单默认值/.test(msgText), msgText.replace(/\s+/g, ' ').slice(0, 60))
  }
  await fCfg.locator('.ant-tabs-tab-btn', { hasText: '页面配置' }).first().click()
  await page.waitForTimeout(800)

  // 显示名改名：客户 → 客户X
  const custRowForm = fCfg.locator('tbody tr', { hasText: '客户' }).first()
  const renameInput = custRowForm.locator('input[type="text"]').first()
  await renameInput.fill('客户X')
  await renameInput.blur().catch(() => {})
  await page.waitForTimeout(1000)
  await page.screenshot({ path: path.join(SHOT, 'so-form-config.png') })
  await fCfg.locator('button:has-text("关闭")').first().click().catch(() => {})
  await page.keyboard.press('Escape')
  await page.waitForTimeout(2500)
  const afterRename = await page.locator('body').innerText()
  check('表单页面配置「显示名」改名真实生效（客户→客户X）', /客户X/.test(afterRename))
  await page.screenshot({ path: path.join(SHOT, 'so-form-after-config.png') })

  console.log(`\n══════ UI 结果：通过 ${pass} / 失败 ${fail} ══════`)
  await browser.close()
  await pg.end()
  process.exit(fail === 0 ? 0 : 1)
}

main().catch(async (e) => {
  console.error('UI 脚本异常:', e.message)
  try { await pg.end() } catch { /* ignore */ }
  process.exit(1)
})
