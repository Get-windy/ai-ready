// 销售退货申请页 UI 验收（独立 Chromium 实例，避免与 MCP 浏览器互斥）
// 覆盖：按单据/按明细双 Tab 列收口 · 页面配置按 Tab 独立 · 按明细无批量打印 · 扩展信息 Tab · 号段编号 · 编辑回填
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')

const BASE = 'http://localhost:5656'
const API = 'http://localhost:5655'

let pass = 0, fail = 0
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✅ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; console.log(`  ❌ ${name}${detail ? ' — ' + detail : ''}`) }
}

function extractCaptcha(img) {
  const m = (img || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}

async function apiLogin() {
  const cap = await fetch(`${API}/api/auth/captcha`).then(r => r.json())
  const cd = cap.data || cap
  const lg = await fetch(`${API}/api/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    // 独立验收账号（与并行会话共用 admin 会因 sa-token 单端登录互踢）
    body: JSON.stringify({ username: 'sra_e2e', password: 'admin123', tenantName: '系统租户', captcha: extractCaptcha(cd.img), captchaKey: cd.uuid }),
  }).then(r => r.json())
  const d = lg?.data || {}
  return { token: d.token || d.accessToken || d.tokenValue, tenantId: d.tenantId }
}

const MARK = 'UI-E2E-SRA'

async function main() {
  let { token, tenantId } = await apiLogin()
  if (!token) { console.log('登录失败'); process.exit(1) }

  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1760, height: 950 } })
  const errors = []
  page.on('console', m => { if (m.type() === 'error') errors.push(m.text()) })
  page.on('pageerror', e => errors.push(String(e)))

  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([tk, tid]) => { localStorage.setItem('token', tk); localStorage.setItem('tenantId', String(tid || 1)) }, [token, tenantId])

  // 打开页面（sa-token 单端登录，被并行会话踢下线时自愈重登）
  async function open(url, waitSel = '.ss-grid') {
    for (let i = 1; i <= 5; i++) {
      await page.goto(url, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(3500)
      const onLogin = await page.evaluate(() => !!document.querySelector('input[type="password"]')).catch(() => true)
      if (!onLogin) {
        await page.waitForSelector(waitSel, { timeout: 20000 }).catch(() => { })
        await page.waitForTimeout(2500)
        return true
      }
      console.log(`  ⚠️ 第 ${i} 次被踢下线，自动重登…`)
      const r = await apiLogin()
      await page.evaluate(([tk, tid]) => { localStorage.setItem('token', tk); localStorage.setItem('tenantId', String(tid || 1)) }, [r.token, r.tenantId])
    }
    return false
  }

  async function closeModal() {
    await page.evaluate(() => {
      const btns = [...document.querySelectorAll('.ant-modal-close')]
      btns.forEach(b => b.click())
    })
    await page.waitForTimeout(800)
    await page.waitForSelector('.ant-modal-wrap', { state: 'hidden', timeout: 5000 }).catch(() => { })
    await page.waitForTimeout(500)
  }

  const headTexts = () => page.$$eval('.ss-grid thead th', ths => ths.map(t => t.textContent.trim()))
  const btnTexts = () => page.$$eval('button', bs => bs.map(b => b.textContent.trim()).filter(Boolean))

  // ═══ 1. 列表页 · 按单据 ═══
  console.log('\n[1] 列表页 · 按单据 Tab')
  await open(`${BASE}/sales/return-apply/index`)
  const tabs = await page.$$eval('.tab-item', ts => ts.map(t => t.textContent.trim()))
  check('双 Tab「按单据/按明细」', tabs.length === 2 && tabs[0] === '按单据' && tabs[1] === '按明细', tabs.join('/'))

  let ths = await headTexts()
  const docFields = ths.filter(Boolean).filter(t => t !== '操作')
  check('按单据列 = 文档 47 字段', docFields.length === 47, `实际 ${docFields.length}`)
  for (const n of ['单据日期', '单据编号', '单据状态', '客户', '本单金额', '审核时间', '商品行数']) {
    check(`列「${n}」存在`, ths.includes(n))
  }
  const buttons = await btnTexts()
  for (const n of ['新增', '刷新', '批量打印', '打印(F8)', '导出', '配置']) {
    check(`按钮「${n}」存在`, buttons.includes(n))
  }
  // 打印(F8)：未选单据应给出真实业务提示（而非"打印功能开发中"桩）
  await page.click('button:has-text("打印(F8)")')
  await page.waitForTimeout(900)
  const printTip = await page.evaluate(() => document.body.innerText)
  check('打印(F8) 真实响应（提示先选单据）', /请先选择要打印的单据/.test(printTip), '')

  // ═══ 2. 列表页 · 按明细 ═══
  console.log('\n[2] 列表页 · 按明细 Tab')
  await page.click('.tab-item:nth-child(2)')
  await page.waitForTimeout(3000)
  ths = await headTexts()
  const detailFields = ths.filter(Boolean).filter(t => t !== '操作')
  check('按明细列 = 文档 62 字段（已收口 97→62）', detailFields.length === 62, `实际 ${detailFields.length}`)
  for (const n of ['商品名称', '货号', '条码', '规格', '型号', '产地', '单位', '小单位', '换算关系', '换算结果',
    '大包装', '中包装', '小包装', '退货数量', '已收数量', '未收数量', '终止数量', '终止金额',
    '单价', '小单位单价', '金额', '折扣(%)', '折后单价', '折后金额', '重量(kg)', '体积(m³)',
    '销售类型', '商品行属性', '明细备注', '单据备注']) {
    check(`列「${n}」存在`, ths.includes(n))
  }
  for (const n of ['价格等级1', '可用库存', '账面库存', '最近售价', '零售价', '批发价', '最低售价', '参考成本单价', '兑换积分', '赠品', '图片']) {
    check(`多余列「${n}」已移除`, !ths.includes(n))
  }
  const buttons2 = await btnTexts()
  check('按明细工具栏无「批量打印」', !buttons2.includes('批量打印'), buttons2.filter(b => ['批量打印', '打印(F8)'].includes(b)).join('/'))

  // ═══ 3. 页面配置弹窗（按 Tab 独立） ═══
  console.log('\n[3] 页面配置弹窗（查询条件按 Tab 独立 + 按明细功能按钮无批量打印）')
  await page.click('.tab-item:nth-child(1)')
  await page.waitForTimeout(2000)
  await page.click('button:has-text("配置")')
  await page.waitForSelector('.config-table tbody tr', { timeout: 10000 }).catch(() => { })
  await page.waitForTimeout(1200)
  const docQueryCount = await page.$$eval('.ant-tabs-tabpane-active .config-table tbody tr', rs => rs.length)
  check('按单据查询条件 = 27 项', docQueryCount === 27, `实际 ${docQueryCount}`)
  // 切到功能按钮 Tab
  await page.click('.config-tabs .ant-tabs-tab:has-text("功能按钮")').catch(() => { })
  await page.waitForSelector('.ant-tabs-tabpane-active .config-table tbody tr', { timeout: 10000 }).catch(() => { })
  await page.waitForTimeout(1500)
  const docBtnCount = await page.$$eval('.ant-tabs-tabpane-active .config-table tbody tr', rs => rs.length)
  check('按单据功能按钮 = 6 项', docBtnCount === 6, `实际 ${docBtnCount}`)
  await closeModal()
  await page.waitForTimeout(1200)

  await page.click('.tab-item:nth-child(2)')
  await page.waitForTimeout(1800)
  await page.click('button:has-text("配置")')
  await page.waitForSelector('.config-table tbody tr', { timeout: 10000 }).catch(() => { })
  await page.waitForTimeout(1200)
  await page.click('.config-tabs .ant-tabs-tab:has-text("查询条件")').catch(() => { })
  await page.waitForTimeout(800)
  const detailQueryCount = await page.$$eval('.ant-tabs-tabpane-active .config-table tbody tr', rs => rs.length)
  check('按明细查询条件 = 16 项', detailQueryCount === 16, `实际 ${detailQueryCount}`)
  await page.click('.config-tabs .ant-tabs-tab:has-text("功能按钮")').catch(() => { })
  await page.waitForSelector('.ant-tabs-tabpane-active .config-table tbody tr', { timeout: 10000 }).catch(() => { })
  await page.waitForTimeout(1500)
  const detailBtnTexts = await page.$$eval('.ant-tabs-tabpane-active .config-table tbody tr td:first-child', tds => tds.map(t => t.textContent.trim()))
  check('按明细功能按钮 = 5 项（无批量打印）', detailBtnTexts.length === 5 && !detailBtnTexts.includes('批量打印'),
    detailBtnTexts.join('/'))
  await closeModal()
  await page.waitForTimeout(1000)
  await page.screenshot({ path: 'sra-list-detail.png' })

  // ═══ 4. 表单页 · 新建 ═══
  console.log('\n[4] 表单页 · 新建（号段编号 + 扩展信息 Tab）')
  await open(`${BASE}/sales/return-apply/form`, '.bottom-tabs')
  const formTabs = await page.$$eval('.bottom-tabs .ant-tabs-tab', ts => ts.map(t => t.textContent.trim()))
  check('表单底部 Tab 含「扩展信息」', formTabs.includes('扩展信息'), formTabs.join('/'))
  for (const n of ['收款', '物流信息', '会员信息']) {
    check(`Tab「${n}」存在`, formTabs.includes(n))
  }
  const bodyText = await page.evaluate(() => document.body.innerText)
  const noMatch = bodyText.match(/XSTHSQD-\d{8}-\d{4}/)
  check('新建单据编号来自后端号段（非演示自增）', !!noMatch, noMatch ? noMatch[0] : '未找到 XSTHSQD- 编号')

  // 头部字段区：对标截图默认 2 行 10 字段，且不含制单信息/本单金额
  // 注意：InlineField 用 placeholder 承载字段名（视觉即截图里的灰字标签），故从 placeholder 读名字
  const readHeaderLabels = () => page.$$eval('.info-flow .inline-field', els => els.map(e => {
    const input = e.querySelector('input')
    const sel = e.querySelector('.ant-select-selection-placeholder')
    const picked = e.querySelector('.ant-select-selection-item, .ant-picker-input input')
    return (input?.placeholder || sel?.textContent || picked?.value || picked?.textContent || '').trim()
  }))
  const headerLabels = await readHeaderLabels()
  check('头部字段 = 10 项（截图 2 行）', headerLabels.length === 10, headerLabels.join('/'))
  for (const n of ['客户', '入库仓库', '经手人', '单据日期', '销售类型', '联系人', '联系电话', '联系地址', '预计收货', '审核人']) {
    check(`头部含「${n}」`, headerLabels.includes(n))
  }
  for (const n of ['制单人', '制单时间', '打印次数', '本单金额', '编号']) {
    check(`头部不含「${n}」`, !headerLabels.includes(n))
  }
  // 默认 2 行；窄屏下字段超 2 行应出现展开/收起
  const rowsIn = (h) => page.setViewportSize({ width: h, height: 950 }).then(() => page.waitForTimeout(700))
  await rowsIn(1760)
  const wideRows = await page.$$eval('.info-flow .inline-field', els => [...new Set(els.map(e => e.offsetTop))].length)
  check('宽屏(1760)下字段恰好 2 行', wideRows <= 2, `${wideRows} 行`)
  await rowsIn(1180)
  const narrowRows = await page.$$eval('.info-flow .inline-field', els => [...new Set(els.map(e => e.offsetTop))].length)
  const toggleShown = !!(await page.$('.info-flow-toggle'))
  check('窄屏(1180)下超 2 行显示展开/收起按钮', narrowRows > 2 && toggleShown, `${narrowRows} 行 toggle=${toggleShown}`)
  if (toggleShown) {
    await page.waitForTimeout(1200) // 等 ResizeObserver 回填折叠高度
    const collapsedH = await page.$eval('.info-flow', el => el.clientHeight)
    await page.click('.info-flow-toggle')
    await page.waitForTimeout(900)
    const expandedH = await page.$eval('.info-flow', el => el.clientHeight)
    check('点击展开后显示全部字段', expandedH > collapsedH, `${collapsedH} → ${expandedH}`)
    await page.click('.info-flow-toggle')
    await page.waitForTimeout(800)
    const recollapsedH = await page.$eval('.info-flow', el => el.clientHeight)
    check('点击收起后恢复 2 行', Math.abs(recollapsedH - collapsedH) <= 2, `${expandedH} → ${recollapsedH}`)
  }
  await rowsIn(1760)

  // 表单配置面板：默认勾选集与头部一致
  await page.click('.bill-header button:has-text("配置")')
  await page.waitForSelector('.field-name', { timeout: 10000 }).catch(() => { })
  await page.waitForTimeout(1200)
  const cfgRowCount = await page.$$eval('.field-name', els => els.length)
  const cfgChecked = await page.$$eval('tbody tr', rows => rows
    .filter(r => r.querySelector('.field-name'))
    .filter(r => r.querySelectorAll('td')[3]?.querySelector('.ant-checkbox-checked')).length)
  check('表单配置字段数 = 43（不含编号/制单信息/本单金额/备注）', cfgRowCount === 43, `实际 ${cfgRowCount}`)
  check('表单配置「显示」列默认勾选 = 17（头部 10 + 扩展信息 7）', cfgChecked === 17, `实际 ${cfgChecked}`)
  await closeModal()
  await page.waitForTimeout(800)

  // 扩展信息 Tab 内容
  await page.click('.bottom-tabs .ant-tabs-tab:has-text("扩展信息")')
  await page.waitForTimeout(1200)
  const extLabels = await page.$$eval('.ant-tabs-tabpane-active .tab-content-row .tab-field', fs => fs.map(f => f.textContent.trim()))
  check('扩展信息 Tab 含 7 个自定义字段', extLabels.length === 7, extLabels.join('/'))
  await page.screenshot({ path: 'sra-form-new.png', fullPage: false })

  // ═══ 5. 表单页 · 编辑回填 ═══
  console.log('\n[5] 表单页 · 编辑回填（真实单据）')
  const createRes = await fetch(`${API}/api/erp/sale/return`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify({
      customerId: 2, customerName: '客户甲', warehouseId: 1, warehouseName: '主仓库',
      handlerId: 1, handlerName: 'admin', orderDate: '2026-09-10T00:00:00', status: 0, remark: MARK,
      items: [{ productId: '2073239284579586050', productCode: 'SP-20260704-041', productName: '博多新米坊酒酿果味酱罐头', returnQuantity: 2, unitPrice: 50 }],
    }),
  }).then(r => r.json())
  const docId = createRes?.data?.id
  const docNo = createRes?.data?.returnNo
  check('夹具单据创建成功', !!docId && !!docNo, `${docNo}`)

  try {
    await open(`${BASE}/sales/return-apply/form/${docId}`, '.bottom-tabs')
    // 等待详情接口回填（客户/明细）
    await page.waitForFunction(() => document.body.innerText.includes('客户甲'), { timeout: 20000 }).catch(() => { })
    await page.waitForFunction(() => document.body.innerText.includes('博多'), { timeout: 20000 }).catch(() => { })
    await page.waitForTimeout(1500)
    const editText = await page.evaluate(() => document.body.innerText)
    check('编辑打开单据编号回填一致', editText.includes(docNo), `期望 ${docNo}`)
    check('编辑页回填客户/仓库', editText.includes('客户甲') && editText.includes('主仓库'), '')
    const gridRows = await page.$$eval('.ss-grid tbody tr', rs => rs.filter(r => r.textContent.includes('博多')).length)
    check('明细行回填商品', gridRows >= 1, `匹配行 ${gridRows}`)
    await page.screenshot({ path: 'sra-form-edit.png' })
  } finally {
    await fetch(`${API}/api/erp/sale/return/${docId}`, { method: 'DELETE', headers: { Authorization: `Bearer ${token}` } }).catch(() => { })
    console.log(`  清理：夹具单据 ${docNo} 已删除`)
  }

  // ═══ 6. 控制台错误 ═══
  console.log('\n[6] 控制台')
  const severe = errors.filter(e => !/401|Unauthorized|logout|Sentry|ResizeObserver|favicon/i.test(e))
  check('无严重控制台错误', severe.length === 0, severe.slice(0, 3).join(' | '))

  await browser.close()
  console.log(`\n══════ UI 验收：${pass} 通过 / ${fail} 失败 ══════`)
  process.exit(fail > 0 ? 1 : 0)
}

main().catch(e => { console.error('脚本异常:', e); process.exit(1) })
