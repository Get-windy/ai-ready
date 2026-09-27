/**
 * 验证「单据打印」系统级接口（业务级渲染）端到端可用。
 *
 * 打的是这条链：
 *   页面只给 pageCode + 单据主键
 *     → DocumentPrintController
 *       → PrintDataProviderRegistry 找到装配器
 *         → SaleOrderPrintDataProvider 从库里装配标准数据包
 *           → PrintConfigService 取打印设置 → PrintBehaviorApplier 加工
 *             → FormatEngineImpl 渲染 HTML
 *
 * 用法：
 *   node tools/verify-print-document.cjs <pageCode> <documentId> [templateId]
 *   例：node tools/verify-print-document.cjs sale 1902885844
 *
 * ⚠️ 批量跑多个页面时**每次之间留 2~3 秒**：每个用例都会重新登录（取验证码 + 载权限），
 *    十几个用例首尾相接会把 dev 实例压出连接耗尽，表现为最后一个用例 "read ECONNRESET"
 *    —— 那是环境噪声不是功能故障（2026-09-27 实测：连跑 11 个必挂最后一个，加 sleep 3 全绿）。
 *
 * 环境变量：
 *   BE_PORT   后端端口（默认 5655；本脚本用独立端口启动的实例时不传错）
 *   PRINT_USER / PRINT_PASSWORD  登录账号（默认 e2e_settings/admin123）
 *     ⚠️ 必须用 e2e_* 专用账号：sa-token 配了 is-concurrent=false，用 admin 会把
 *        用户浏览器里的会话踢掉。
 */
const http = require('http')
const fs = require('fs')
const path = require('path')

const BE = Number(process.env.BE_PORT || 5655)
const USER = process.env.PRINT_USER || 'e2e_settings'
const PASS = process.env.PRINT_PASSWORD || 'admin123'
const OUT = path.resolve(__dirname, '../tool-results/print-template-v2')

const pageCode = process.argv[2]
const dataFileIdx = process.argv.indexOf('--data')
/** 结果集页没有单据主键：给一份样例数据走「前端渲染」路径（/v2/print/format/render） */
const dataFile = dataFileIdx > 0 ? process.argv[dataFileIdx + 1] : null
const documentId = dataFile ? null : process.argv[3]
const templateId = dataFile ? null : process.argv[4]
if (!pageCode || (!documentId && !dataFile)) {
  console.error('用法: node tools/verify-print-document.cjs <pageCode> <documentId> [templateId]')
  console.error('      node tools/verify-print-document.cjs <pageCode> --data <样例数据.json>')
  process.exit(1)
}

/**
 * 带自愈的请求：连接抖动重试一次，401 自动重登。
 *
 * 两种情况都会把「瞬时故障」伪装成「功能故障」，让批量验证给出假红：
 * · 连接级抖动（ECONNRESET/超时）—— 连续跑十几个用例时偶发（2026-09-27 实测）；
 * · 401 —— sa-token 配了 `is-concurrent: false`，同账号在别处登录会踢掉本会话。
 */
let TOKEN = null
async function req(method, p, body) {
  if (!TOKEN) {
    const info = await login()
    TOKEN = info.token || info.accessToken
  }
  let res
  try {
    res = await rawReq(method, p, body, TOKEN)
  } catch (e) {
    await new Promise(r => setTimeout(r, 800))
    res = await rawReq(method, p, body, TOKEN)
  }
  if (res.status === 401) {
    TOKEN = null
    const info = await login()
    TOKEN = info.token || info.accessToken
    res = await rawReq(method, p, body, TOKEN)
  }
  return res
}

function rawReq(method, p, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: BE, path: '/api' + p, method,
      headers: {
        'Content-Type': 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }, res => {
      const c = []
      res.on('data', d => c.push(d))
      res.on('end', () => {
        const s = Buffer.concat(c).toString('utf8')
        try { resolve({ status: res.statusCode, body: JSON.parse(s) }) }
        catch { resolve({ status: res.statusCode, body: s }) }
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function login() {
  // ⚠️ 这里必须用 rawReq：login 是在 req 的 401 重登分支里被调用的，
  //    再用 req 就成了自己调自己 → 栈溢出（本脚本改出过这个 bug）。
  const cap = await rawReq('GET', '/auth/captcha')
  if (!cap.body?.data?.img) throw new Error('取验证码失败: ' + JSON.stringify(cap.body).slice(0, 200))
  const code = [...Buffer.from(cap.body.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const r = await rawReq('POST', '/auth/login', {
    username: USER, password: PASS, tenantName: '系统租户',
    captcha: code, captchaKey: cap.body.data.uuid,
  })
  const info = r.body?.data
  if (!info) throw new Error('登录失败: ' + JSON.stringify(r.body).slice(0, 300))
  return info
}

const results = []
const notes = []
function check(name, ok, detail) {
  results.push({ name, ok })
  console.log(`  ${ok ? '✓' : '✗'} ${name}${detail ? ' — ' + detail : ''}`)
}
/** 只提示、不计入失败（用于「模板形态决定该不该有」的项） */
function note(name, detail) {
  notes.push(name)
  console.log(`  · ${name}${detail ? ' — ' + detail : ''}`)
}

;(async () => {
  console.log(`验证单据打印：pageCode=${pageCode} documentId=${documentId}`
    + (templateId ? ` templateId=${templateId}` : '') + ` (BE :${BE})`)
  fs.mkdirSync(OUT, { recursive: true })

  // 登录一次并把 token 交给 req 统一维护（它在 401 时会自己重登）
  const info = await login()
  TOKEN = info.token || info.accessToken
  console.log(`  登录成功：${info.username || USER}`)

  // ── 结果集模式：没有单据主键，取该页模板 + 样例数据走 /format/render ──
  if (dataFile) {
    const sample = JSON.parse(fs.readFileSync(dataFile, 'utf8'))
    console.log(`\n① GET /v2/print/documents/${pageCode}/templates`)
    const t = await req('GET', `/v2/print/documents/${encodeURIComponent(pageCode)}/templates`, null)
    const body = t.body?.data || {}
    const tpls = body.templates || []
    check('接口 200', t.status === 200, `HTTP ${t.status}`)
    check('该页有已发布模板', tpls.length > 0, `${tpls.length} 个: ${tpls.map(x => x.templateName).join(', ')}`)
    if (!tpls.length) process.exit(1)
    const tpl = tpls.find(x => x.isDefault) || tpls[0]
    note('模板形态', String(tpl.templateJson || '').includes('"sections"') ? 'v2 分区流式' : 'v1 绝对坐标')
    note('该页是否后端可装配', `supported=${body.supported}`)

    console.log('\n② POST /v2/print/format/render（结果集：列由数据给）')
    const r = await req('POST', '/v2/print/format/render', {
      templateJson: tpl.templateJson, dataJson: JSON.stringify(sample),
    })
    const html = r.body?.data?.html || ''
    check('接口 200', r.status === 200, `HTTP ${r.status}`)
    check('返回了 HTML', html.length > 300, `${html.length} 字节`)

    console.log('\n③ 内容断言')
    check('标题来自数据（title）', !sample.title || html.includes(sample.title), sample.title)
    const cols = sample.columns || []
    const missing = cols.map(c => c.title || c.header || c.key || c.field)
      .filter(x => x && !html.includes(x))
    check(`表头来自数据（${cols.length} 列）`, cols.length > 0 && missing.length === 0,
      missing.length ? `缺: ${missing.join(', ')}` : cols.map(c => c.title || c.header).join(' / '))
    const firstRow = (sample.rows || [])[0] || {}
    const shown = Object.values(firstRow).filter(v => v !== null && v !== undefined && v !== '')
      .map(String).filter(v => html.includes(v))
    check('首行数据被打出来', shown.length > 0, shown.slice(0, 4).join(' | '))
    check('有页码', /第\d+页\s*\/\s*共\d+页/.test(html))

    const outFile = path.join(OUT, `_render_${pageCode.replace(/[:\/]/g, '-')}_resultset.html`)
    fs.writeFileSync(outFile, html, 'utf8')
    console.log(`\nHTML 已写出：${outFile}`)
    const failed = results.filter(x => !x.ok)
    console.log('')
    if (failed.length) { console.log(`✗ ${failed.length}/${results.length} 项未通过`); process.exit(1) }
    console.log(`✓ 全部 ${results.length} 项通过`)
    return
  }

  // ── ① 页面级信息：已发布模板 + 是否后端可装配 ──
  console.log('\n① GET /v2/print/documents/{pageCode}/templates')
  const tplRes = await req('GET', `/v2/print/documents/${encodeURIComponent(pageCode)}/templates`, null)
  const tplBody = tplRes.body?.data || {}
  check('接口 200', tplRes.status === 200, `HTTP ${tplRes.status}`)
  check('后端已注册该页面装配器（supported=true）', tplBody.supported === true,
    `supported=${tplBody.supported}`)
  const tpls = tplBody.templates || []
  check('有已发布模板', tpls.length > 0, `${tpls.length} 个: ${tpls.map(t => t.templateName).join(', ')}`)
  check('返回了 defaultTemplateId', tplBody.defaultTemplateId != null, String(tplBody.defaultTemplateId))
  if (!tpls.length) { console.log('\n✗ 没有已发布模板，后续渲染无法验证'); process.exit(1) }

  // ── ② 按单据渲染 ──
  console.log('\n② POST /v2/print/documents/{pageCode}/{documentId}/render')
  const qs = templateId ? `?templateId=${templateId}` : ''
  const r = await req('POST',
    `/v2/print/documents/${encodeURIComponent(pageCode)}/${documentId}/render${qs}`, null)
  const body = r.body?.data || {}
  const html = body.html || ''
  check('接口 200', r.status === 200, `HTTP ${r.status}`)
  check('返回了 HTML', html.length > 500, `${html.length} 字节`)
  if (!html) {
    console.log('\n后端返回：', JSON.stringify(r.body).slice(0, 600))
    process.exit(1)
  }

  // ── ③ 渲染内容断言 ──
  //
  // 分两档，因为「该不该有合计/页码」取决于**模板形态**，不是引擎能力：
  //   v2（sections，手建标准模板）—— 分区流式，本来就该有合计/大写/页码 → 硬断言；
  //   v1（components，ql361 转换产物）—— 绝对坐标套打式，可能压根没定义这些区，
  //      断言只会得到一个没有信息量的红。
  const picked = tpls.find(t => t.templateId === body.templateId)
  const pickedJson = typeof picked?.templateJson === 'string' ? picked.templateJson : ''
  const isStandardV2 = pickedJson.includes('"sections"')
  console.log(`\n③ 渲染内容（模板形态：${isStandardV2 ? 'v2 分区流式' : 'v1 绝对坐标'}）`)
  const contentCheck = isStandardV2 ? check : note

  check('是完整 HTML 文档', /<!DOCTYPE html>/i.test(html) && /<html[\s>]/i.test(html))
  contentCheck('带单据编号', !!body.documentNo && html.includes(body.documentNo),
    `documentNo=${body.documentNo}`)
  const theads = (html.match(/<thead>/g) || []).length
  contentCheck('明细表有表头（每页重印）', theads >= 1, `${theads} 个 <thead>`)
  contentCheck('有合计行', html.includes('合计'), html.includes('<tfoot') ? '含 <tfoot>' : '无 tfoot')
  contentCheck('金额大写非空', /[零壹贰叁肆伍陆柒捌玖]/.test(html) && /元/.test(html))
  contentCheck('有页码', /第\d+页\s*\/\s*共\d+页/.test(html),
    (html.match(/第\d+页\s*\/\s*共\d+页/) || [''])[0])
  check('模板名回传正确', !!body.templateName, body.templateName)
  if (!isStandardV2) {
    console.log('  ⚠️ 当前模板是绝对坐标（v1）形态，未断言合计/大写/页码 ——')
    console.log('     要这几项请用 v2 标准模板：见 tool-results/print-template-v2/sale.json')
  }

  const outFile = path.join(OUT, `_render_${pageCode}_${documentId}_tpl${body.templateId}.html`)
  fs.writeFileSync(outFile, html, 'utf8')
  console.log(`\nHTML 已写出：${outFile}`)

  // 大写金额单独摘出来看一眼（最容易错的就是它）
  const rmb = html.match(/[零壹贰叁肆伍陆柒捌玖拾佰仟万亿元角分整]{2,}/g) || []
  if (rmb.length) console.log('  大写金额片段:', rmb.slice(0, 3).join(' | '))

  const failed = results.filter(x => !x.ok)
  console.log('')
  if (failed.length) {
    console.log(`✗ ${failed.length}/${results.length} 项未通过`)
    process.exit(1)
  }
  console.log(`✓ 全部 ${results.length} 项通过`)
})().catch(e => { console.error('✗', e.message); process.exit(1) })
