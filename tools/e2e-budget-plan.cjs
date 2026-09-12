/* 预算编制金标准 端到端验证（独立 headless 浏览器） */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')

const FE = 'http://localhost:5656'
const PORT = 5655
const SHOTS = 'I:/AI-Ready/tool-results/budget-plan'
const STAMP = Date.now()
const DOC_DATE = new Date().toISOString().slice(0, 10)

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

  /** 重新登录并刷新 token（并行会话共用 admin 会互踢，导致 401） */
  async function relogin() {
    const s = await login()
    token = s.token
    userInfo = s.userInfo
    return s
  }

  // ═══════ A. 后端 API 闭环 ═══════
  const nextNo = await apiReq('GET', '/erp/budget/annual/next-no', null, token)
  const billNo = nextNo?.data
  check('next-no 生成单号', !!billNo && /^YSD-\d{8}-\d{3}$/.test(billNo), String(billNo))

  const subjects = await apiReq('GET', '/erp/finance/subject/list?pageSize=200', null, token)
  const subjectList = (subjects?.data?.records || subjects?.data || [])
  const subj1 = subjectList[0] || {}
  const subj2 = subjectList[1] || {}
  check('会计科目可查询（预算科目来源）', subjectList.length > 0, `科目数=${subjectList.length}`)

  const saveRes = await apiReq('POST', '/erp/budget/annual/save', {
    budgetNo: billNo,
    fiscalYear: new Date().getFullYear(),
    departmentId: '1',
    departmentName: '财务部',
    budgetDate: DOC_DATE,
    handlerId: userInfo?.id,
    handlerName: userInfo?.nickname || 'admin',
    creatorName: userInfo?.nickname || 'admin',
    description: `E2E预算编制-${STAMP}`,
    remark: '自动化验证',
    totalAmount: 30000,
    items: [
      { lineNo: 1, subjectId: subj1.id, subjectCode: subj1.code || '', subjectName: subj1.name || '差旅费', subjectType: '费用类', budgetAmount: 20000, remark: '差旅' },
      { lineNo: 2, subjectId: subj2.id, subjectCode: subj2.code || '', subjectName: subj2.name || '办公费', subjectType: '费用类', budgetAmount: 10000, remark: '办公' },
    ],
  }, token)
  const budgetId = saveRes?.data?.id
  check('保存预算编制单（含明细）', !!budgetId, `id=${budgetId} 单号=${saveRes?.data?.budgetNo}`)
  check('保存后状态=草稿', saveRes?.data?.status === 'draft', String(saveRes?.data?.status))
  check('保存后明细 2 行', (saveRes?.data?.items || []).length === 2, `明细=${(saveRes?.data?.items || []).length}`)
  check('本单金额汇总=30000', Number(saveRes?.data?.totalAmount) === 30000, String(saveRes?.data?.totalAmount))

  const detail = await apiReq('GET', `/erp/budget/annual/${budgetId}`, null, token)
  const firstItem = (detail?.data?.items || [])[0] || {}
  check('明细行号/剩余额度回写', firstItem.lineNo === 1 && Number(firstItem.remainingAmount) === Number(firstItem.budgetAmount),
    `lineNo=${firstItem.lineNo} 剩余=${firstItem.remainingAmount}`)

  const pageRes = await apiReq('GET', `/erp/budget/annual/page?pageNum=1&pageSize=10&budgetNo=${encodeURIComponent(billNo)}`, null, token)
  const body = pageRes?.data || {}
  check('多条件分页查询命中', Array.isArray(body.records) && body.records.length === 1, `total=${body.total}`)

  const pageByDept = await apiReq('GET', `/erp/budget/annual/page?pageNum=1&pageSize=10&departmentName=${encodeURIComponent('财务部')}`, null, token)
  check('按部门模糊查询', (pageByDept?.data?.records || []).length >= 1, `total=${pageByDept?.data?.total}`)

  const pageByDesc = await apiReq('GET', `/erp/budget/annual/page?pageNum=1&pageSize=10&description=${encodeURIComponent('E2E预算编制')}`, null, token)
  const descRecords = pageByDesc?.data?.records || []
  check('按摘要（独立字段）查询', descRecords.length >= 1 && descRecords.every(r => (r.description || '').includes('E2E预算编制')),
    `total=${pageByDesc?.data?.total}`)

  const pageByDate = await apiReq('GET', `/erp/budget/annual/page?pageNum=1&pageSize=10&dateStart=${DOC_DATE}&dateEnd=${DOC_DATE}`, null, token)
  check('按编制日期区间查询', (pageByDate?.data?.records || []).length >= 1, `total=${pageByDate?.data?.total}`)

  const pageDraft = await apiReq('GET', '/erp/budget/annual/page?pageNum=1&pageSize=10&status=draft', null, token)
  check('按状态查询(draft)', (pageDraft?.data?.records || []).some(r => r.id === budgetId), `total=${pageDraft?.data?.total}`)

  // 状态流转：提交 → 批量审批
  const submitRes = await apiReq('POST', `/erp/budget/annual/${budgetId}/submit`, null, token)
  check('提交审批', submitRes?.data?.status === 'submitted', String(submitRes?.data?.status))

  const approveRes = await apiReq('POST', '/erp/budget/annual/batch-approve', {
    ids: [budgetId], auditorId: userInfo?.id, auditorName: userInfo?.nickname || 'admin', auditRemark: 'E2E审批通过',
  }, token)
  const approveDetail = await apiReq('GET', `/erp/budget/annual/${budgetId}`, null, token)
  check('批量审批通过', approveDetail?.data?.status === 'approved', `affected=${approveRes?.data} status=${approveDetail?.data?.status}`)
  check('审批人/审批时间落库', !!approveDetail?.data?.auditorName && !!approveDetail?.data?.auditTime,
    `${approveDetail?.data?.auditorName} ${approveDetail?.data?.auditTime}`)

  // 打印计数
  await apiReq('POST', `/erp/budget/annual/${budgetId}/print`, null, token)
  const afterPrint = await apiReq('GET', `/erp/budget/annual/${budgetId}`, null, token)
  check('打印次数 +1', Number(afterPrint?.data?.printCount) >= 1, String(afterPrint?.data?.printCount))

  // 已审批禁止修改（草稿口径红线）
  const editLocked = await apiReq('POST', '/erp/budget/annual/save', { id: budgetId, totalAmount: 1 }, token)
  check('已审批单据禁止编辑', editLocked?.success === false || editLocked?.code >= 400, `code=${editLocked?.code} msg=${editLocked?.message}`)

  // 批量提交/驳回闭环：新建第二张
  const no2 = (await apiReq('GET', '/erp/budget/annual/next-no', null, token))?.data
  const save2 = await apiReq('POST', '/erp/budget/annual/save', {
    budgetNo: no2, fiscalYear: new Date().getFullYear(), departmentId: '2', departmentName: '销售部',
    budgetDate: DOC_DATE, description: `E2E驳回-${STAMP}`, totalAmount: 5000,
    items: [{ lineNo: 1, subjectName: '业务招待费', budgetAmount: 5000 }],
  }, token)
  const id2 = save2?.data?.id
  check('第二张预算单创建（next-no 递增）', !!id2 && no2 !== billNo, `${billNo} → ${no2}`)

  await apiReq('POST', '/erp/budget/annual/batch-submit', { ids: [id2] }, token)
  await apiReq('POST', '/erp/budget/annual/batch-reject', { ids: [id2], auditorId: userInfo?.id, auditorName: 'admin', auditRemark: 'E2E驳回' }, token)
  const rejDetail = await apiReq('GET', `/erp/budget/annual/${id2}`, null, token)
  check('批量驳回 → 已驳回', rejDetail?.data?.status === 'rejected', String(rejDetail?.data?.status))

  await relogin()
  const batchDelete = await apiReq('POST', '/erp/budget/annual/batch-delete', { ids: [id2] }, token)
  const afterDel = await apiReq('GET', `/erp/budget/annual/page?pageNum=1&pageSize=10&budgetNo=${encodeURIComponent(no2)}`, null, token)
  check('批量删除（已驳回可删）', (afterDel?.data?.records || []).length === 0, `affected=${batchDelete?.data}`)

  await relogin()
  const delApproved = await apiReq('POST', '/erp/budget/annual/batch-delete', { ids: [budgetId] }, token)
  check('已审批单据不可删除（保护）', Number(delApproved?.data) === 0, `affected=${delApproved?.data}`)

  // ═══════ B. 前端页面 ═══════
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 1000 } })
  const page = await ctx.newPage()
  const errs = []
  page.on('pageerror', e => errs.push(e.message))
  page.on('console', m => { if (m.type() === 'error') errs.push(m.text().slice(0, 200)) })

  /** 注入当前 token（新导航生效） */
  async function injectAuth() {
    if (!token) await relogin()
    await page.addInitScript(({ token, userInfo }) => {
      localStorage.setItem('token', token)
      localStorage.setItem('tenantId', String(userInfo?.tenantId || 1))
      localStorage.setItem('tenantName', userInfo?.tenantName || '系统租户')
    }, { token, userInfo })
  }

  /**
   * 带自愈重登的导航。
   * 背景：sa-token `is-concurrent=false`，并行会话共用 admin 账号会互相踢下线。
   * 策略：每次尝试都取最新 token 再导航，并用 waitForFunction 抢在「下一次被踢」之前的窗口完成渲染判定。
   */
  async function gotoAuth(url, readyCheck, tries = 8) {
    for (let i = 0; i < tries; i++) {
      await relogin()
      await injectAuth()
      try {
        await page.goto(url, { waitUntil: 'commit', timeout: 60000 })
        await page.waitForFunction(readyCheck, { timeout: 20000 })
        return true
      } catch {
        await page.waitForTimeout(500)
      }
    }
    return false
  }

  const listReady = await gotoAuth(
    FE + '/finance/budget-plan/index',
    () => document.querySelectorAll('tbody tr').length > 0,
  )
  check('列表页导航就绪', listReady)
  await page.waitForTimeout(2000)

  const listInfo = await page.evaluate(() => {
    const rows = [...document.querySelectorAll('tbody tr')]
    const headers = [...document.querySelectorAll('thead th')].map(t => t.innerText.trim()).filter(Boolean)
    return { rowCount: rows.length, headers, first: rows[0]?.innerText.replace(/\n+/g, ' | ') }
  })
  check('列表页渲染数据行', listInfo.rowCount > 0, `行数=${listInfo.rowCount}`)
  check('列表页表头（金标准列）', listInfo.headers.includes('单据编号') && listInfo.headers.includes('预算总额'), listInfo.headers.slice(0, 12).join(','))
  await page.screenshot({ path: `${SHOTS}/01-list.png`, fullPage: true })

  // 列配置弹窗
  const gearBtn = page.locator('button:has(.anticon-table)').first()
  if (await gearBtn.count()) {
    await gearBtn.click()
    await page.waitForTimeout(1200)
    const colPanel = await page.evaluate(() => document.body.innerText.includes('列') && document.querySelectorAll('.ant-modal, .ant-drawer').length > 0)
    check('列配置弹窗可打开', colPanel)
    await page.screenshot({ path: `${SHOTS}/02-column-config.png`, fullPage: false })
    await page.keyboard.press('Escape')
    await page.waitForTimeout(600)
  } else {
    check('列配置按钮存在', false)
  }

  // 页面配置弹窗
  const settingBtn = page.locator('button:has(.anticon-setting)').first()
  if (await settingBtn.count()) {
    await settingBtn.click()
    await page.waitForTimeout(1200)
    const pagePanel = await page.evaluate(() => document.body.innerText.includes('查询条件') || document.body.innerText.includes('功能按钮'))
    check('页面配置弹窗可打开', pagePanel)
    await page.screenshot({ path: `${SHOTS}/03-page-config.png`, fullPage: false })

    // 功能按钮开关实际生效：切到「功能按钮」Tab → 关掉「导出」→ 工具栏导出按钮消失
    await page.evaluate(() => {
      const tab = [...document.querySelectorAll('.ant-tabs-tab')].find(t => t.innerText.includes('功能按钮'))
      tab && tab.click()
    })
    await page.waitForTimeout(900)
    const toggled = await page.evaluate(() => {
      const row = [...document.querySelectorAll('tr')].find(r =>
        r.innerText.includes('导出') && r.querySelector('input[type="checkbox"]'))
      const cb = row && row.querySelector('input[type="checkbox"]')
      if (cb && cb.checked) { cb.click(); return true }
      return false
    })
    await page.waitForTimeout(900)
    await page.keyboard.press('Escape')
    await page.waitForTimeout(1000)
    const exportGone = await page.evaluate(() =>
      ![...document.querySelectorAll('button')].some(b => b.innerText.trim() === '导出'))
    check('页面配置「功能按钮」开关生效', toggled && exportGone, `toggled=${toggled} 导出按钮消失=${exportGone}`)

    // 恢复（避免污染后续断言）
    await settingBtn.click()
    await page.waitForTimeout(1000)
    await page.evaluate(() => {
      const tab = [...document.querySelectorAll('.ant-tabs-tab')].find(t => t.innerText.includes('功能按钮'))
      tab && tab.click()
    })
    await page.waitForTimeout(800)
    await page.evaluate(() => {
      const row = [...document.querySelectorAll('tr')].find(r =>
        r.innerText.includes('导出') && r.querySelector('input[type="checkbox"]'))
      const cb = row && row.querySelector('input[type="checkbox"]')
      if (cb && !cb.checked) cb.click()
    })
    await page.waitForTimeout(700)
    await page.keyboard.press('Escape')
    await page.waitForTimeout(800)
  } else {
    check('页面配置按钮存在', false)
  }

  // 表单页（新增）：就绪判定包含「自动取号已回填」，避免断言早于异步 next-no
  const formReady = await gotoAuth(
    FE + '/finance/budget-plan/form',
    () => document.body.innerText.includes('预算编制')
      && /YSD-\d{8}-\d{3}/.test(document.body.innerText)
      && document.querySelectorAll('tbody tr').length > 0,
  )
  check('表单页导航就绪', formReady)
  await page.waitForTimeout(1500)
  const formInfo = await page.evaluate(() => {
    const text = document.body.innerText
    const placeholders = [...document.querySelectorAll('input')].map(i => i.placeholder).filter(Boolean)
    const selectPh = [...document.querySelectorAll('.ant-select-selection-placeholder')].map(e => e.textContent.trim())
    return {
      hasTitle: text.includes('预算编制'),
      hasFiscalYear: text.includes('2026年') || placeholders.includes('财政年度') || selectPh.some(t => t.includes('财政年度')),
      hasDept: placeholders.includes('部门') || selectPh.some(t => t.includes('部门')),
      hasBillNo: /YSD-\d{8}-\d{3}/.test(text),
      hasDetailHeaders: text.includes('预算科目') && text.includes('预算金额'),
      rowCount: document.querySelectorAll('tbody tr').length,
    }
  })
  check('表单页标题/字段渲染', formInfo.hasTitle && formInfo.hasFiscalYear && formInfo.hasDept,
    `title=${formInfo.hasTitle} 年度=${formInfo.hasFiscalYear} 部门=${formInfo.hasDept}`)
  check('表单页自动取号（next-no）', formInfo.hasBillNo)
  check('表单页明细列（预算科目/预算金额）', formInfo.hasDetailHeaders, `明细行=${formInfo.rowCount}`)
  await page.screenshot({ path: `${SHOTS}/04-form.png`, fullPage: true })

  // 表单配置弹窗（三 Tab）
  const cfgBtn = page.locator('button:has(.anticon-setting)').first()
  if (await cfgBtn.count()) {
    await cfgBtn.click()
    await page.waitForTimeout(1200)
    const tabs = await page.evaluate(() => [...document.querySelectorAll('.ant-tabs-tab')].map(t => t.innerText.trim()))
    check('表单配置弹窗三 Tab', tabs.includes('页面配置') && tabs.includes('录单默认值') && tabs.includes('打印设置'), tabs.join('/'))
    await page.screenshot({ path: `${SHOTS}/05-form-config.png`, fullPage: false })
  } else {
    check('表单配置按钮存在', false)
  }

  // 表单编辑（已审批单 → 锁定）：gotoAuth 只负责「页面渲染出来」，业务断言在下方单独判定
  await gotoAuth(
    `${FE}/finance/budget-plan/form?id=${budgetId}`,
    () => document.body.innerText.includes('预算编制')
      && (document.body.innerText.includes('已进入审批流程') || document.querySelectorAll('input').length > 3),
  )
  await page.waitForTimeout(1500)
  const locked = await page.evaluate(() => document.body.innerText.includes('已进入审批流程'))
  check('已审批单据打开为只读锁定', locked)
  await page.screenshot({ path: `${SHOTS}/06-form-locked.png`, fullPage: true })

  // 排除「并行会话共用 admin 互踢」造成的 401 噪声，只关注页面自身错误
  const realErrs = errs.filter(e => !/401|Unauthorized|auth\/logout/i.test(e))
  check('前端无 JS 报错', realErrs.length === 0, realErrs.slice(0, 3).join(' | '))

  await browser.close()

  const failed = results.filter(r => !r.ok)
  console.log(`\n==== 汇总: ${results.length - failed.length}/${results.length} 通过 ====`)
  if (failed.length) {
    console.log('失败项:')
    failed.forEach(f => console.log(' -', f.name, f.detail || ''))
    process.exit(1)
  }
}

main().catch(e => { console.error('E2E 异常:', e); process.exit(2) })
