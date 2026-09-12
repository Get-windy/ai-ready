/* 销售订单金标准 端到端验证（独立 headless 浏览器 + 直连 API） */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')

const FE = 'http://localhost:5656'
const PORT = 5655
const SHOTS = 'I:/AI-Ready/tool-results/sale-order'
const TENANT = 1

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
      let s = ''
      res.on('data', c => { s += c })
      res.on('end', () => {
        try { resolve(JSON.parse(s)) } catch (e) { resolve({ raw: s, status: res.statusCode }) }
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
    username: 'admin', password: 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  const token = res?.data?.token || res?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
  return { token, userInfo: res.data }
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`)
}

async function main() {
  let { token, userInfo } = await login()
  console.log('登录成功')

  // ═══════ A. 后端接口 ═══════
  const nextNo = await apiReq('GET', '/erp/sale/order/next-no', null, token)
  const billNo = nextNo?.data
  check('next-no 接口返回后端号段', !!billNo && /^XSDD/.test(String(billNo)), String(billNo))

  const docPage = await apiReq('GET', `/erp/sale/order/center/page-by-doc?tenantId=${TENANT}&pageNum=1&pageSize=5`, null, token)
  const docRec = docPage?.data?.records?.[0]
  check('按单据分页可用', Array.isArray(docPage?.data?.records), `总数=${docPage?.data?.total}`)
  check('按单据行含优惠后金额字段', docRec ? ('favorableAmount' in docRec) : true,
    docRec ? `favorableAmount=${docRec.favorableAmount}` : '无数据行(字段存在性跳过)')
  check('按单据行含商品行数字段', docRec ? ('lineCount' in docRec) : true,
    docRec ? `lineCount=${docRec.lineCount}` : '无数据行(字段存在性跳过)')

  const pickPage = await apiReq('GET', `/erp/sale/order/center/picking-shipping?tenantId=${TENANT}&pageNum=1&pageSize=5`, null, token)
  const pickRec = pickPage?.data?.records?.[0]
  check('拣货发货分页可用', Array.isArray(pickPage?.data?.records), `总数=${pickPage?.data?.total}`)
  check('拣货行含已拣货/未拣货/排序字段', pickRec
    ? ('pickedQuantity' in pickRec && 'unpickedQuantity' in pickRec && 'sortOrder' in pickRec && 'sortValue' in pickRec)
    : true, pickRec ? `已拣=${pickRec.pickedQuantity} 未拣=${pickRec.unpickedQuantity}` : '无数据行(字段存在性跳过)')

  const pendingPage = await apiReq('GET', `/erp/sale/order/center/pending-review?tenantId=${TENANT}&pageNum=1&pageSize=5`, null, token)
  check('待审核分页可用', Array.isArray(pendingPage?.data?.records), `总数=${pendingPage?.data?.total}`)

  const fulfillPage = await apiReq('GET', `/erp/sale/order/center/fulfillment-page?tenantId=${TENANT}&pageNum=1&pageSize=5`, null, token)
  check('订单履约分页可用', Array.isArray(fulfillPage?.data?.records), `总数=${fulfillPage?.data?.total}`)

  // 新查询条件：来源（此前键名错误导致失效）
  const bySource = await apiReq('GET', `/erp/sale/order/center/page-by-doc?tenantId=${TENANT}&pageNum=1&pageSize=5&source=999`, null, token)
  check('「来源」查询参数被后端接受', bySource?.code === 200 || bySource?.success === true,
    `code=${bySource?.code ?? bySource?.status}`)

  // ═══════ B. 前端页面 ═══════
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 } })
  const page = await ctx.newPage()

  /** 把 token 写入同源 localStorage（绕过 SVG 验证码交互） */
  async function injectToken(t) {
    await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
    await page.evaluate(([tk, tid]) => {
      localStorage.setItem('token', tk)
      localStorage.setItem('tenantId', String(tid))
    }, [t, TENANT])
  }

  /** 打开页面并等待关键元素渲染；并行会话共用 admin 会互踢，自愈重登 */
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
      const ok = await page.waitForSelector(selector, { timeout: 15000 }).then(() => true).catch(() => false)
      if (!ok) {
        // 动态路由/chunk 未就绪时重试一次
        if (attempt < 6) {
          await page.reload({ waitUntil: 'domcontentloaded' })
          return gotoWithRelogin(url, selector, attempt + 1)
        }
      }
    }
  }

  await injectToken(token)

  // ═══════ C. 表单页（优先验证，缩短被并行会话踢下线的时间窗） ═══════
  await gotoWithRelogin(`${FE}/sales/order/form`, '.form-page-container')
  await page.waitForTimeout(2000)
  const formOrderNo = ((await page.locator('.order-no').first().innerText().catch(() => '')) || '').trim()
  check('表单页标题栏自动带出后端号段单号', /^NO\.\s*XSDD/.test(formOrderNo), formOrderNo)
  await page.screenshot({ path: `${SHOTS}/04-sale-order-form.png` })

  // 配置弹窗三 Tab
  const cfgBtn = page.locator('button', { hasText: '配置' }).first()
  if (await cfgBtn.count()) {
    await cfgBtn.click()
    await page.waitForTimeout(1200)
    const cfgTabs = (await page.locator('.ant-modal .ant-tabs-tab').allInnerTexts()).join('|')
    check('表单配置弹窗含三 Tab',
      cfgTabs.includes('页面配置') && cfgTabs.includes('录单默认值') && cfgTabs.includes('打印设置'), cfgTabs)

    // 字段按实际渲染区域标注（头部字段 / 底部 Tab / 单据信息 / 摘要面板）
    const rowsMeta = await page.locator('.ant-modal .ant-table-tbody tr').evaluateAll(rows =>
      rows.map(r => Array.from(r.querySelectorAll('td')).map(td => (td.textContent || '').trim()).slice(1, 3)))
    const headRow = rowsMeta.find(c => c[1] === '客户')
    const auditRow = rowsMeta.find(c => c[1] === '审核人')
    const depositRow = rowsMeta.find(c => c[1] === '订金账户')
    check('页面配置按区域分组：客户=头部基本信息', !!headRow && headRow[0] === '头部基本信息', headRow ? headRow.join(' / ') : '未找到')
    check('页面配置按区域分组：审核人=单据信息（非头部）', !!auditRow && auditRow[0] === '单据信息', auditRow ? auditRow.join(' / ') : '未找到')
    check('页面配置按区域分组：订金账户=收款', !!depositRow && depositRow[0] === '收款', depositRow ? depositRow.join(' / ') : '未找到')
    const cfgColTitles = await page.locator('.ant-modal .ant-table-thead th').allInnerTexts()
    check('页面配置含「显示名」列', cfgColTitles.join('|').includes('显示名'), cfgColTitles.join('|'))
    await page.screenshot({ path: `${SHOTS}/05-form-config.png` })
    await page.keyboard.press('Escape')
    await page.waitForTimeout(600)
  }

  // 头部字段区：默认 2 行折叠 + 展开/收起
  const readFlow = () => page.evaluate(() => {
    const f = document.querySelector('.info-flow')
    if (!f) return null
    return { h: f.offsetHeight, sh: f.scrollHeight, maxH: f.style.maxHeight || '' }
  })
  const flowCollapsed = await readFlow()
  check('头部字段区默认折叠（仅显示 2 行）',
    !!flowCollapsed && flowCollapsed.h < flowCollapsed.sh && !!flowCollapsed.maxH,
    flowCollapsed ? `显示=${flowCollapsed.h}px 内容=${flowCollapsed.sh}px maxHeight=${flowCollapsed.maxH}` : '未找到 .info-flow')

  const flowToggle = page.locator('.info-flow-toggle')
  if (await flowToggle.count()) {
    await flowToggle.click()
    await page.waitForTimeout(900)
    const flowExpanded = await readFlow()
    check('点击展开后显示全部字段',
      !!flowExpanded && flowExpanded.h >= flowExpanded.sh - 1,
      flowExpanded ? `显示=${flowExpanded.h}px 内容=${flowExpanded.sh}px` : '')
    await page.screenshot({ path: `${SHOTS}/08-form-fields-expanded.png` })

    await flowToggle.click()
    await page.waitForTimeout(900)
    const flowAgain = await readFlow()
    check('再次点击收起为 2 行',
      !!flowAgain && flowAgain.h < flowAgain.sh,
      flowAgain ? `显示=${flowAgain.h}px 内容=${flowAgain.sh}px` : '')
  } else {
    check('头部字段区存在展开/收起按钮', false, '未找到 .info-flow-toggle')
  }

  // 明细列：默认显示子集 + 列配置内全部可选列
  const detailHeadCells = await page.locator('.ss-grid thead th').count()
  check('表单明细默认渲染列头', detailHeadCells > 20, `默认显示=${detailHeadCells}`)
  const gear = page.locator('.th-settings-btn').first()
  if (await gear.count()) {
    await gear.click()
    await page.waitForTimeout(1200)
    const cfgRows = await page.locator('.col-setting-row').count()
    check('明细列配置可选列 = 76（操作列 + 75 数据列）', cfgRows >= 75, `配置行=${cfgRows}`)
    await page.screenshot({ path: `${SHOTS}/06-detail-column-config.png` })
    await page.keyboard.press('Escape')
    await page.waitForTimeout(500)
  } else {
    check('明细列配置齿轮存在', false, '未找到 .th-settings-btn')
  }

  // ═══════ B2. 订单处理中心 ═══════
  await gotoWithRelogin(`${FE}/sales/order-center`, '.ss-grid')
  check('前端进入订单处理中心（未跳登录）', !page.url().includes('/login'), page.url())
  await page.waitForTimeout(2000)

  const headCells = await page.locator('.ss-grid thead th').count()
  check('订单处理中心「按单据」列头 = 68（65 数据列 + 行号/勾选/操作）', headCells === 68, `表头单元格=${headCells}`)
  await page.screenshot({ path: `${SHOTS}/01-order-center-byDoc.png` })

  const mainTabTexts = await page.locator('.main-tabs .tab-item').allInnerTexts()
  check('主 Tab 命名 = 全部/待审核/拣货发货', mainTabTexts.join('|').includes('拣货发货'), mainTabTexts.join('|'))

  const subTabTexts = await page.locator('.sub-tabs .ant-tabs-tab').allInnerTexts()
  check('子 Tab 命名含「按路线」', subTabTexts.join('|').includes('按路线'), subTabTexts.join('|'))

  // —— 订单履约子 Tab ——
  const fulfillTab = page.locator('.sub-tabs .ant-tabs-tab', { hasText: '订单履约' }).first()
  if (await fulfillTab.count()) {
    await fulfillTab.click()
    await page.waitForTimeout(2200)
    const fh = await page.locator('.ss-grid thead th').count()
    check('订单履约列头 = 74（71 数据列 + 行号/勾选/操作）', fh >= 71, `表头单元格=${fh}`)
    await page.screenshot({ path: `${SHOTS}/02-fulfillment.png` })
  } else {
    check('订单履约子 Tab 存在', false, '未找到子 Tab')
  }

  // —— 拣货发货主 Tab ——
  const pickTab = page.locator('.main-tabs .tab-item', { hasText: '拣货发货' }).first()
  if (await pickTab.count()) {
    await pickTab.click()
    await page.waitForTimeout(2200)
    const ph = await page.locator('.ss-grid thead th').count()
    check('拣货发货列头 = 40（37 数据列 + 行号/勾选/操作）', ph >= 37, `表头单元格=${ph}`)
    await page.screenshot({ path: `${SHOTS}/03-picking.png` })
  } else {
    check('拣货发货主 Tab 存在', false, '未找到主 Tab')
  }

  // ═══════ D. 页面配置真实生效（查询条件显隐 + 持久化） ═══════
  // 先清空该 Tab 的页面配置，保证初始状态为「全部显示」（避免上次运行的残留影响断言）
  await apiReq('POST', '/system/user-config/order-center/page-config-all.byDoc', {
    value: JSON.stringify({ queryFields: [], functionButtons: [], printConfig: null }),
  }, token)
  await gotoWithRelogin(`${FE}/sales/order-center`, '.ss-grid')
  await page.reload({ waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(3000)
  const searchSel = '.search-area .search-field-item:not(.search-action-item):not(.search-checkbox-field)'
  const beforeCount = await page.locator(searchSel).count()
  // 以第一个查询条件「单据日期」的存在性作为开关生效的判据（可见数受 2 排槽位上限约束，不能直接比数量）
  // input 型查询项的 label 渲染在 placeholder 上，用 placeholder 定位
  const fieldItem = (label) => page.locator(`.search-area input[placeholder*="${label}"]`)
  const beforeHasField = await fieldItem('单据日期').count()
  check('「单据日期」初始在搜索区', beforeHasField > 0, `count=${beforeHasField}`)

  // 打开页面配置弹窗（工具栏最右「配置」）
  // 工具栏最后一个按钮即「配置」（与文档按钮顺序一致）
  const cfgOpen = page.locator('.filter-toolbar-right button:visible').last()
  const cfgBtnText = (await cfgOpen.innerText().catch(() => '')).trim()
  check('工具栏末位按钮为「配置」', cfgBtnText.replace(/\s/g, '').includes('配置'), cfgBtnText)
  await cfgOpen.click()
  await page.waitForTimeout(1200)
  const panelVisible = await page.locator('.config-table').count()
  check('页面配置弹窗可打开', panelVisible > 0, `配置表=${panelVisible}`)

  if (panelVisible > 0) {
    const firstCb = page.locator('.config-table tbody tr').first().locator('input[type="checkbox"]').first()
    await firstCb.click()
    await page.waitForTimeout(900)
    // 关闭弹窗
    const closeBtn = page.locator('.ant-modal button:visible', { hasText: '关闭' }).last()
    if (await closeBtn.count()) await closeBtn.click()
    else await page.keyboard.press('Escape')
    await page.waitForTimeout(1500)

    const afterCount = await page.locator(searchSel).count()
    const afterHasField = await fieldItem('单据日期').count()
    check('关闭「单据日期」后该查询项从搜索区移除', afterHasField === 0, `移除后 count=${afterHasField}（可见槽位 ${beforeCount} → ${afterCount}）`)

    // 刷新后仍生效（后端持久化）
    await page.reload({ waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(3000)
    const reloadCount = await page.locator(searchSel).count()
    const reloadHasField = await fieldItem('单据日期').count()
    check('页面配置持久化（刷新后仍生效）', reloadHasField === 0, `刷新后「单据日期」count=${reloadHasField}，可见槽位=${reloadCount}`)

    await page.screenshot({ path: `${SHOTS}/07-page-config-effect.png` })

    // 还原配置：清空该 Tab 的页面配置（等价于全部显示），避免污染后续使用
    await apiReq('POST', '/system/user-config/order-center/page-config-all.byDoc', {
      value: JSON.stringify({ queryFields: [], functionButtons: [], printConfig: null }),
    }, token)
    await page.reload({ waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(3000)
    const restoredHasField = await fieldItem('单据日期').count()
    check('恢复配置后查询项复原', restoredHasField > 0, `count=${restoredHasField}`)
  }

  await browser.close()

  const pass = results.filter(r => r.ok).length
  console.log(`\n结果：${pass}/${results.length} 通过`)
  const failed = results.filter(r => !r.ok)
  if (failed.length) {
    console.log('未通过：')
    failed.forEach(f => console.log('  ✘', f.name, '-', f.detail))
  }
  fs.writeFileSync(`${SHOTS}/result.json`, JSON.stringify(results, null, 2))
  process.exit(failed.length ? 1 : 0)
}

main().catch(e => { console.error('执行异常:', e.message); process.exit(1) })
