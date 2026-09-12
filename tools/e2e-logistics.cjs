/* 物流公司金标准 端到端验证（直连 API + 独立 headless 浏览器） */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5655)
const API_BASE = process.env.API_URL || `http://localhost:${PORT}`
const SHOTS = 'I:/AI-Ready/tool-results/logistics'
const TENANT = 1
const XLSX = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/xlsx')

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

function rawReq(method, path, body, token) {
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
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        try { resolve(JSON.parse(buf.toString('utf8'))) } catch (e) { resolve({ raw: buf, status: res.statusCode }) }
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

let TOKEN = null

/** 带重登自愈的 API 调用（并行会话共用 admin 会被 sa-token 互踢） */
async function api(method, path, body, token) {
  let r = await rawReq(method, path, body, TOKEN)
  if (r && (Number(r.code) === 401 || r.status === 401)) {
    console.log('  [token 失效，自动重登]')
    await login()
    r = await rawReq(method, path, body, TOKEN)
  }
  return r
}

/** 专用验收账号（tools/e2e-logistics-user.sql 创建），避免与并行会话共用 admin 互相踢下线 */
const E2E_USER = process.env.E2E_USER || 'e2e_logistics'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  let res = await rawReq('POST', '/auth/login', {
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  if (!(res?.data?.token || res?.data?.accessToken)) {
    // 专用账号不可用时回退 admin（并发环境下会被互踢，仅作兜底）
    const cap2 = await rawReq('GET', '/auth/captcha')
    const code2 = [...Buffer.from(cap2.data.img.split(',')[1], 'base64').toString('utf8')
      .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
    res = await rawReq('POST', '/auth/login', {
      username: 'admin', password: 'admin123', tenantName: '系统租户',
      captcha: code2, captchaKey: cap2.data.uuid,
    })
  }
  const token = res?.data?.token || res?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
  TOKEN = token
  return { token, userInfo: res.data }
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`)
}

const stamp = Date.now().toString().slice(-6)
const partyName = `E2E物流${stamp}`
const partyName2 = `E2E物流改名${stamp}`
let createdId = null

async function main() {
  const { token } = await login()
  console.log('登录成功\n=== A. 后端接口 ===')

  // ── 1. 编号号段 ──
  const nextSeq = await api('GET', '/erp/md/customer/next-seq?prefix=WuLiu', null, token)
  const seq = Number(nextSeq?.data?.seq)
  check('next-seq（WuLiu）返回可用序号', Number.isFinite(seq) && seq > 0, `seq=${seq}`)
  const expectCode = 'WuLiu' + String(seq).padStart(3, '0')
  check('编号拼接符合对标口径（WuLiu + 3 位）', /^WuLiu\d{3,}$/.test(expectCode), expectCode)

  // ── 2. 新增（含纳税人信息） ──
  const createRes = await api('POST', '/erp/md/customer', {
    partnerType: 'LOGISTICS',
    partnerCode: expectCode,
    partnerName: partyName,
    partnerShortName: 'E2E简称',
    mnemonicCode: 'E2EWL',
    remark: 'E2E备注-可检索',
    companyFullName: 'E2E物流有限公司',
    taxNumber: '91620100E2E0001X',
    address: '甘肃省酒泉市E2E路1号',
    phone: '0937-0000000',
    bankAddress: '酒泉市E2E支行',
    bankAccount: '6222000000000001',
    status: 'ENABLED',
  }, token)
  createdId = createRes?.data?.id
  check('新增返回记录 id（供子表挂接）', !!createdId && Number(createdId) > 0, `id=${createdId}`)
  check('新增回读编号正确', createRes?.data?.partnerCode === expectCode, String(createRes?.data?.partnerCode))

  // ── 3. 网点（biz_party_contact） ──
  const branch1 = await api('POST', '/erp/partner/contacts', {
    partyId: createdId, contactName: 'E2E网点一', linkman: '张三',
    phone: '13800000001', detailAddress: '酒泉市网点一路1号', isPrimary: 1, status: 1,
  }, token)
  const branch2 = await api('POST', '/erp/partner/contacts', {
    partyId: createdId, contactName: 'E2E网点二', linkman: '李四',
    phone: '13800000002', detailAddress: '酒泉市网点二路2号', isPrimary: 0, status: 1,
  }, token)
  check('网点新增成功（2 条）', branch1?.data === true && branch2?.data === true,
    `b1=${JSON.stringify(branch1?.data)} b2=${JSON.stringify(branch2?.data)}`)

  const contacts = await api('GET', `/erp/partner/contacts/by-party/${createdId}`, null, token)
  const list = Array.isArray(contacts?.data) ? contacts.data : []
  check('网点回读含网点名称/联系人/电话/地址', list.length === 2
    && list.some(c => c.contactName === 'E2E网点一' && c.linkman === '张三' && c.phone === '13800000001'
      && c.detailAddress === '酒泉市网点一路1号'),
  JSON.stringify(list.map(c => `${c.contactName}|${c.linkman}|${c.phone}|${c.detailAddress}`)))

  // ── 4. 列表（主联系人/电话/地址 带出 + 备注模糊） ──
  const page = await api('GET', `/erp/md/customer/page?partnerType=LOGISTICS&keyword=${encodeURIComponent(partyName)}&pageNum=1&pageSize=20`, null, token)
  const row = (page?.data?.records || []).find(r => r.id === createdId)
  check('列表按名称命中新增记录', !!row, `total=${page?.data?.total}`)
  check('列表带出主网点联系人', row?.contactPerson === '张三', String(row?.contactPerson))
  check('列表带出主网点联系电话', row?.contactPhone === '13800000001', String(row?.contactPhone))
  check('列表带出主网点地址', row?.address === '酒泉市网点一路1号', String(row?.address))
  check('列表带出助记码', row?.mnemonicCode === 'E2EWL', String(row?.mnemonicCode))

  const pageByRemark = await api('GET', `/erp/md/customer/page?partnerType=LOGISTICS&keyword=${encodeURIComponent('E2E备注-可检索')}&pageNum=1&pageSize=20`, null, token)
  check('筛选条件支持备注模糊匹配', (pageByRemark?.data?.records || []).some(r => r.id === createdId),
    `total=${pageByRemark?.data?.total}`)

  // ── 5. 详情（纳税人信息回读） ──
  const detail = await api('GET', `/erp/md/customer/${createdId}`, null, token)
  const d = detail?.data || {}
  check('详情回读公司全称', d.companyFullName === 'E2E物流有限公司', String(d.companyFullName))
  check('详情回读纳税人识别号', d.taxNumber === '91620100E2E0001X', String(d.taxNumber))
  check('详情回读地址', d.address === '酒泉市网点一路1号', String(d.address))
  check('详情回读开户行地址', d.bankAddress === '酒泉市E2E支行', String(d.bankAddress))
  check('详情回读开户行账号', d.bankAccount === '6222000000000001', String(d.bankAccount))
  check('详情回读简称', d.partnerShortName === 'E2E简称', String(d.partnerShortName))

  // ── 6. 更新 ──
  const upd = await api('PUT', `/erp/md/customer/${createdId}`, {
    partnerType: 'LOGISTICS', partnerCode: expectCode, partnerName: partyName2,
    mnemonicCode: 'E2EWL2', companyFullName: 'E2E物流有限公司（改）',
  }, token)
  const detail2 = await api('GET', `/erp/md/customer/${createdId}`, null, token)
  check('更新生效（名称/助记码/公司全称）', upd?.data === true
    && detail2?.data?.partnerName === partyName2
    && detail2?.data?.mnemonicCode === 'E2EWL2'
    && detail2?.data?.companyFullName === 'E2E物流有限公司（改）',
    `${detail2?.data?.partnerName}|${detail2?.data?.mnemonicCode}`)

  // ── 7. 停用 / 显示停用过滤 ──
  await api('PUT', `/erp/md/customer/${createdId}/status?status=DISABLED`, null, token)
  const enabledPage = await api('GET', `/erp/md/customer/page?partnerType=LOGISTICS&status=ENABLED&keyword=${encodeURIComponent(partyName2)}&pageNum=1&pageSize=20`, null, token)
  check('停用后「仅启用」列表不再包含', !(enabledPage?.data?.records || []).some(r => r.id === createdId),
    `total=${enabledPage?.data?.total}`)
  const allPage = await api('GET', `/erp/md/customer/page?partnerType=LOGISTICS&keyword=${encodeURIComponent(partyName2)}&pageNum=1&pageSize=20`, null, token)
  check('「显示停用」时可见', (allPage?.data?.records || []).some(r => r.id === createdId), `total=${allPage?.data?.total}`)

  // ── 8. 导出真实 Excel ──
  const exp = await api('GET', `/erp/md/customer/export?partnerType=LOGISTICS&title=${encodeURIComponent('物流公司')}`, null, token)
  const isXlsx = Buffer.isBuffer(exp?.raw) && exp.raw.length > 200 && exp.raw[0] === 0x50 && exp.raw[1] === 0x4B
  check('导出返回真实 xlsx（PK 魔数）', !!isXlsx, exp?.raw ? `${(exp.raw.length / 1024).toFixed(1)}KB type=${exp.raw.slice(0, 2).toString()}` : 'no buffer')

  // ── 9. 删除 ──
  const del = await api('DELETE', `/erp/md/customer/${createdId}`, null, token)
  const after = await api('GET', `/erp/md/customer/${createdId}`, null, token)
  check('删除生效（详情为空）', del?.data === true && !after?.data, `del=${del?.data} after=${JSON.stringify(after?.data)}`)

  console.log('\n=== B. 前端页面 ===')
  await login() // 刷新 token（并行会话共用 admin 会被互踢）
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const p = await ctx.newPage()
  const errors = []
  p.on('pageerror', e => errors.push(String(e)))
  p.on('console', m => { if (m.type() === 'error') errors.push(m.text()) })

  // 把前端 /api/** 转发到本次验证使用的后端实例（避免并行会话共用 5655 时口径不一致）
  await p.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: API_BASE + u.pathname + u.search })
  })

  await p.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await p.evaluate(([tk, tid]) => {
    localStorage.setItem('token', tk)
    localStorage.setItem('tenantId', String(tid))
  }, [TOKEN, TENANT])

  /** 导航前重登并注入最新 token（并行会话共用 admin 会持续互踢） */
  async function openPage(url, waitMs = 6000) {
    for (let attempt = 0; attempt < 2; attempt++) {
      await login()
      await p.evaluate(([tk, tid]) => {
        localStorage.setItem('token', tk)
        localStorage.setItem('tenantId', String(tid))
      }, [TOKEN, TENANT])
      await p.goto(url, { waitUntil: 'domcontentloaded' })
      await p.waitForTimeout(waitMs)
      // 自愈：被并行会话互踢时整页重登再进一次
      const kicked = p.url().includes('/login') || (await p.locator('input[type=password]').count()) > 0
      if (!kicked) return
      console.log('  [页面被互踢，自愈重登]')
    }
  }

  await openPage(`${FE}/md/logistics/index`)
  await p.screenshot({ path: `${SHOTS}/ui-01-list.png` })

  const listText = (await p.evaluate(() => document.body.innerText)).replace(/\s+/g, '')
  for (const label of ['新增', '导入', '刷新', '打印(F8)', '导出', '更多', '筛选条件', '查询', '显示停用']) {
    check(`列表页含「${label}」`, listText.includes(label.replace(/\s+/g, '')))
  }
  const headers = await p.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  for (const h of ['物流公司编号', '物流公司名称', '联系人', '联系电话', '物流公司地址', '备注']) {
    check(`表头含「${h}」`, headers.some(x => x.includes(h)), headers.join('/'))
  }
  check('列表无左侧分类树（对标物流公司页无分类）', !listText.includes('物流分类'), '')

  // 行首勾选列（对标列表左侧：序号 / 勾选 / 操作）
  const checkboxCount = await p.locator('.ss-grid th input[type=checkbox], .ss-grid .ss-checkbox-header').count()
  check('数据表含行首勾选列', checkboxCount > 0, `count=${checkboxCount}`)

  // 经典分页栏（对标：首页/上页/第(x/y)页/下页/尾页/跳转/共N条记录/每页显示N行）
  for (const t of ['首页', '上页', '下页', '尾页', '跳转到', '条记录', '每页显示']) {
    check(`分页栏含「${t}」`, listText.includes(t))
  }

  // 工具栏「更多」下拉（对标实测：停用 / 启用）
  await p.evaluate(() => {
    const norm = s => (s || '').replace(/\s+/g, '')
    const el = [...document.querySelectorAll('button, .ant-dropdown-trigger')].find(b => norm(b.innerText) === '更多')
    if (!el) return
    for (const type of ['mouseenter', 'mouseover', 'mousedown', 'mouseup']) {
      el.dispatchEvent(new MouseEvent(type, { bubbles: true }))
    }
    el.click()
  })
  await p.waitForTimeout(1200)
  await p.screenshot({ path: `${SHOTS}/ui-08-more-menu.png` })
  const menuItems = await p.evaluate(() => [...document.querySelectorAll('.ant-dropdown-menu-item')]
    .map(e => (e.innerText || '').replace(/\s+/g, '')).filter(Boolean))
  check('工具栏「更多」含「停用」', menuItems.includes('停用'), menuItems.join('/'))
  check('工具栏「更多」含「启用」', menuItems.includes('启用'), menuItems.join('/'))
  await p.keyboard.press('Escape')
  await p.waitForTimeout(600)

  // 列配置弹窗（rowNo 表头齿轮）
  const gear = await p.$('.ss-grid th [class*=shezhi], .ss-header-settings, .ss-grid th .icon-shezhi2')
  if (gear) {
    await gear.click()
    await p.waitForTimeout(1500)
    await p.screenshot({ path: `${SHOTS}/ui-02-column-config.png` })
    const dlg = await p.evaluate(() => document.body.innerText)
    check('列配置弹窗含「个人配置」', dlg.includes('个人配置'))
    check('列配置弹窗含「全局配置」', dlg.includes('全局配置'))
    check('列配置弹窗含「恢复默认」', dlg.includes('恢复默认'))
    await p.keyboard.press('Escape')
    await p.waitForTimeout(800)
  } else {
    check('列表存在列配置齿轮入口', false, '未找到 .icon-shezhi2')
  }

  // ── 「更多 → 停用」功能闭环：勾选行 → 批量停用 → 状态真实落库 ──
  {
    const batchName = `E2E批量停用${stamp}`
    const created = await api('POST', '/erp/md/customer', {
      partnerType: 'LOGISTICS', partnerCode: `WuLiu${stamp}`, partnerName: batchName, status: 'ENABLED',
    }, token)
    const batchId = created?.data?.id
    check('批量停用用例数据创建成功', !!batchId, `id=${batchId}`)

    await openPage(`${FE}/md/logistics/index`, 6000)
    await p.locator('input[placeholder*="物流公司名称"]').first().fill(batchName)
    await p.locator('button').filter({ hasText: /查\s*询/ }).first().click()
    await p.waitForTimeout(2500)

    const headerCb = p.locator('.ss-grid th input[type=checkbox]').first()
    if (await headerCb.count()) await headerCb.check({ force: true })
    await p.waitForTimeout(800)

    await p.evaluate(() => {
      const norm = s => (s || '').replace(/\s+/g, '')
      const el = [...document.querySelectorAll('button')].find(b => norm(b.innerText) === '更多')
      if (el) { el.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true })); el.click() }
    })
    await p.waitForTimeout(1200)
    await p.locator('.ant-dropdown-menu-item').filter({ hasText: /停\s*用/ }).first().click()
    await p.waitForTimeout(1000)
    await p.locator('.ant-modal button').filter({ hasText: /确\s*定/ }).first().click()
    await p.waitForTimeout(3000)

    const afterBatch = await api('GET', `/erp/md/customer/${batchId}`, null, token)
    check('「更多→停用」对勾选行生效（状态已落库）', afterBatch?.data?.status === 'DISABLED', String(afterBatch?.data?.status))
    check('批量停用无 JS 报错', errors.length === 0, errors.slice(0, 2).join(' | '))
    if (batchId) await api('DELETE', `/erp/md/customer/${batchId}`, null, token)
  }

  // ── 导入向导（对标「基本信息导入」三步：下载模板 / 导入Excel / 完成） ──
  {
    await openPage(`${FE}/md/logistics/index`, 6000)
    await p.locator('button:has-text("导入")').first().click()
    await p.waitForTimeout(1500)
    await p.screenshot({ path: `${SHOTS}/ui-05-import-step1.png` })
    const stepText = (await p.evaluate(() => document.body.innerText)).replace(/\s+/g, '')
    check('导入弹窗标题「基本信息导入」', stepText.includes('基本信息导入'))
    check('导入向导三步与对标一致', ['下载模板', '导入Excel', '完成'].every(s => stepText.includes(s)))
    check('导入向导说明文案对齐对标',
      stepText.includes('1.请使用MicrosoftExcel对模板进行编辑，请勿编辑首行黑体标题。')
      && stepText.includes('2.可参考模板内首行黑体标题的批注信息，帮助您更正确录入数据。'))
    check('步骤1含「下载模版」入口', stepText.includes('下载模版'))

    // 真实下载模板
    const dl = p.waitForEvent('download', { timeout: 20000 }).catch(() => null)
    await p.locator('.ant-modal button:has-text("下载模版")').first().click()
    const d = await dl
    check('下载模板返回真实 xlsx 文件', !!d && /\.xlsx$/.test(d.suggestedFilename() || ''),
      d ? d.suggestedFilename() : 'no download')

    // 步骤2：选择文件
    await p.locator('.ant-modal button:has-text("下一步")').first().click()
    await p.waitForTimeout(1200)
    await p.screenshot({ path: `${SHOTS}/ui-06-import-step2.png` })
    const step2 = (await p.evaluate(() => document.body.innerText)).replace(/\s+/g, '')
    check('步骤2含「上一步 / 下一步 / 退出」', ['上一步', '下一步', '退出'].every(s => step2.includes(s)))

    // 生成真实导入文件并上传
    const ws = XLSX.utils.aoa_to_sheet([
      ['导入结果', '物流公司编号(必填)', '物流公司名称(必填)', '联系人', '联系电话', '公司地址', '备注'],
      ['', `E2EUI${stamp}`, `E2E界面导入${stamp}`, '孙七', '13700000077', '界面导入路7号', 'UI导入备注'],
      ['', `E2EUI${stamp}`, '', '', '', '', ''],
    ])
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '物流公司信息')
    const tmpFile = `${SHOTS}/import-ui-${stamp}.xlsx`
    XLSX.writeFile(wb, tmpFile)

    await p.setInputFiles('.ant-modal input[type=file]', tmpFile)
    await p.waitForTimeout(800)
    await p.locator('.ant-modal button:has-text("下一步")').last().click()
    await p.waitForTimeout(3000)
    await p.screenshot({ path: `${SHOTS}/ui-07-import-step3.png` })
    const step3 = (await p.evaluate(() => document.body.innerText)).replace(/\s+/g, '')
    check('步骤3展示导入成功统计', step3.includes('导入完成') && step3.includes('成功1条'),
      step3.slice(0, 120))
    // antd 会把两字中文按钮渲染成「完 成」，用正则容错
    await p.locator('.ant-modal button').filter({ hasText: /完\s*成/ }).first().click()
    await p.waitForTimeout(2000)

    // 落库校验
    const importedPage = await api('GET', `/erp/md/customer/page?partnerType=LOGISTICS&pageNum=1&pageSize=10&keyword=${encodeURIComponent('E2E界面导入' + stamp)}`, null, token)
    const impRow = (importedPage?.data?.records || [])[0]
    check('界面导入记录已真实落库', !!impRow, `total=${importedPage?.data?.total}`)
    check('界面导入带出联系人与地址', impRow?.contactPerson === '孙七' && impRow?.address === '界面导入路7号',
      `${impRow?.contactPerson}|${impRow?.address}`)
    if (impRow) await api('DELETE', `/erp/md/customer/${impRow.id}`, null, token)
    check('导入流程无 JS 报错', errors.length === 0, errors.slice(0, 2).join(' | '))
  }

  // 新增表单页
  await openPage(`${FE}/md/logistics/form`, 5000)
  await p.screenshot({ path: `${SHOTS}/ui-03-form.png` })
  const formText = (await p.evaluate(() => document.body.innerText)).replace(/\s+/g, '')
  for (const label of ['物流公司编号', '物流公司名称', '助记码', '简称', '备注',
    '网点', '网点名称', '联系人', '联系电话', '联系地址',
    '纳税人信息', '公司全称', '纳税人识别号', '地址', '电话', '开户行地址', '开户行账号',
    '保存(Enter)', '取消(Esc)']) {
    check(`表单页含「${label}」`, formText.includes(label.replace(/\s+/g, '')))
  }
  const codeVal = await p.evaluate(() => {
    const el = [...document.querySelectorAll('input')].find(i => (i.value || '').startsWith('WuLiu'))
    return el ? el.value : ''
  })
  check('表单编号自动按 WuLiu 前缀生成', /^WuLiu\d{3,}$/.test(codeVal), codeVal)

  // 表单快捷键：Enter = 保存（对标按钮文案「保存(Enter)」）
  const enterName = `E2E回车保存${stamp}`
  await p.locator('input[placeholder="请输入物流公司名称"]').fill(enterName)
  await p.keyboard.press('Enter')
  await p.waitForTimeout(3000)
  const enterPage = await api('GET', `/erp/md/customer/page?partnerType=LOGISTICS&pageNum=1&pageSize=5&keyword=${encodeURIComponent(enterName)}`, null, token)
  const enterRow = (enterPage?.data?.records || [])[0]
  check('表单 Enter 键保存生效（对标 保存(Enter)）', !!enterRow, `total=${enterPage?.data?.total}`)
  if (enterRow) await api('DELETE', `/erp/md/customer/${enterRow.id}`, null, token)

  // ── 编辑回填（金标准：表单打开能带出主档 + 网点 + 纳税人信息） ──
  const editName = `E2E编辑回填${stamp}`
  const editCreate = await api('POST', '/erp/md/customer', {
    partnerType: 'LOGISTICS', partnerCode: 'WuLiu' + stamp,
    partnerName: editName, partnerShortName: '回填简称', mnemonicCode: 'HB',
    remark: '回填备注', companyFullName: '回填公司全称', taxNumber: 'HB-TAX-001',
    address: '回填地址', phone: '0937-1111111', bankAddress: '回填开户行地址',
    bankAccount: 'HB-ACC-001', status: 'ENABLED',
  }, token)
  const editId = editCreate?.data?.id
  await api('POST', '/erp/partner/contacts', {
    partyId: editId, contactName: '回填网点', linkman: '王五',
    phone: '13900000009', detailAddress: '回填网点地址', isPrimary: 1, status: 1,
  }, token)

  await openPage(`${FE}/md/logistics/form/${editId}`, 5000)
  await p.screenshot({ path: `${SHOTS}/ui-04-form-edit.png` })
  const vals = await p.evaluate(() => [...document.querySelectorAll('input,textarea')].map(i => i.value).filter(Boolean))
  const joined = vals.join('|')
  check('编辑页回填编号', joined.includes('WuLiu' + stamp), joined.slice(0, 80))
  check('编辑页回填名称', joined.includes(editName))
  check('编辑页回填助记码/简称', joined.includes('HB') && joined.includes('回填简称'))
  check('编辑页回填纳税人信息', joined.includes('回填公司全称') && joined.includes('HB-TAX-001') && joined.includes('回填开户行地址'))
  check('编辑页回填网点', joined.includes('回填网点') && joined.includes('王五') && joined.includes('13900000009'))
  check('编辑仍无 JS 报错', errors.length === 0, errors.slice(0, 2).join(' | '))

  // 清理编辑验证数据
  await api('DELETE', `/erp/md/customer/${editId}`, null, token)

  await browser.close()

  const passed = results.filter(r => r.ok).length
  console.log(`\n══════ 结果：${passed}/${results.length} 通过 ══════`)
  const failed = results.filter(r => !r.ok)
  if (failed.length) {
    console.log('失败项：')
    failed.forEach(f => console.log(`  ❌ ${f.name} — ${f.detail}`))
  }
  fs.writeFileSync(`${SHOTS}/e2e-result.json`, JSON.stringify(results, null, 2))
  process.exit(failed.length ? 1 : 0)
}

main().catch(e => { console.error('FATAL', e); process.exit(2) })
