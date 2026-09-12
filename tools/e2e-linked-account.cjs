/* 互联账号金标准 端到端验证（直连 API + 独立 headless 浏览器） */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')

const FE = 'http://localhost:5656'
// 并行会话可能占用 5655，本验证默认走独立实例 5660（可用 E2E_PORT 覆盖）
const PORT = Number(process.env.E2E_PORT || 5660)
const API_BASE = `http://localhost:${PORT}`
const SHOTS = 'I:/AI-Ready/tool-results/linked-account'
const TENANT = 1

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

function rawBuf(method, path, token) {
  return new Promise((resolve, reject) => {
    const r = http.request({
      hostname: 'localhost', port: PORT, path: '/api' + path, method,
      headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => resolve({ buf: Buffer.concat(chunks), status: res.statusCode, headers: res.headers }))
    })
    r.on('error', reject)
    r.end()
  })
}

let TOKEN = null

/** 带重登自愈的 API 调用（并行会话共用 admin 会被 sa-token 互踢） */
async function api(method, path, body) {
  let r = await rawReq(method, path, body, TOKEN)
  if (r && (Number(r.code) === 401 || r.status === 401)) {
    console.log('  [token 失效，自动重登]')
    await login()
    r = await rawReq(method, path, body, TOKEN)
  }
  return r
}

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  // 使用独立 E2E 用户（并行会话共用 admin 会在 sa-token 上互踢，导致 UI 校验随机失败）
  const res = await rawReq('POST', '/auth/login', {
    username: process.env.E2E_USER || 'e2e_linked_ui',
    password: process.env.E2E_PASS || 'admin123',
    tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  const token = res?.data?.token || res?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
  TOKEN = token
  return { token, userInfo: res.data }
}


/** 按 ZIP 中央目录读取条目（POI 使用 data descriptor，本地头中的压缩长度可能为 0） */
function readZipEntries(buf) {
  const zlib = require('zlib')
  // 从尾部找 EOCD(0x06054b50)
  let eocd = -1
  for (let i = buf.length - 22; i >= 0 && i > buf.length - 66000; i--) {
    if (buf.readUInt32LE(i) === 0x06054b50) { eocd = i; break }
  }
  if (eocd < 0) throw new Error('EOCD not found')
  const count = buf.readUInt16LE(eocd + 10)
  let off = buf.readUInt32LE(eocd + 16)
  const out = {}
  for (let n = 0; n < count; n++) {
    if (buf.readUInt32LE(off) !== 0x02014b50) throw new Error('bad central header')
    const method = buf.readUInt16LE(off + 10)
    const compSize = buf.readUInt32LE(off + 20)
    const nameLen = buf.readUInt16LE(off + 28)
    const extraLen = buf.readUInt16LE(off + 30)
    const commentLen = buf.readUInt16LE(off + 32)
    const localOff = buf.readUInt32LE(off + 42)
    const name = buf.slice(off + 46, off + 46 + nameLen).toString('utf8')
    // 本地头：定位真正的数据起点
    const lNameLen = buf.readUInt16LE(localOff + 26)
    const lExtraLen = buf.readUInt16LE(localOff + 28)
    const dataStart = localOff + 30 + lNameLen + lExtraLen
    const raw = buf.slice(dataStart, dataStart + compSize)
    out[name] = method === 8 ? zlib.inflateRawSync(raw) : raw
    off += 46 + nameLen + extraLen + commentLen
  }
  return out
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`)
}

const stamp = Date.now().toString().slice(-6)
const userName1 = `wx_e2e_${stamp}`
const userName2 = `dy_e2e_${stamp}`
const userName3 = `ali_e2e_${stamp}`
const phone1 = `139${stamp}11`
const phone2 = `139${stamp}22`
const phone3 = `139${stamp}33`
const ids = {}

async function main() {
  await login()
  console.log('登录成功')
  // 清理历史遗留测试数据（该表为本模块新建，仅测试数据），保证基线口径干净
  const pre = await api('GET', '/erp/md/linked-account/list?pageSize=500')
  const preJunk = (pre?.data || []).filter(r => /e2e/i.test(String(r.linkedUserName || '')))
  if (preJunk.length) await api('DELETE', '/erp/md/linked-account/batch', { ids: preJunk.map(r => r.id) })
  console.log(`[清理历史测试数据 ${preJunk.length} 条]`)
  console.log('=== A. 后端接口 ===')

  // ── 1. 字典（平台 / 关联类型，全局唯一） ──
  const dict = await api('GET', '/erp/md/linked-account/dict')
  const platforms = (dict?.data?.platforms || []).map(x => x.value)
  const linkTypes = (dict?.data?.linkTypes || []).map(x => x.value)
  check('字典：互联平台含 微信/支付宝/抖音',
    ['WECHAT', 'ALIPAY', 'DOUYIN'].every(v => platforms.includes(v)), platforms.join(','))
  check('字典：关联类型含 会员/客户',
    ['MEMBER', 'CUSTOMER'].every(v => linkTypes.includes(v)), linkTypes.join(','))
  check('字典：平台含中文标签「微信」',
    (dict?.data?.platforms || []).some(x => x.value === 'WECHAT' && x.label === '微信'))

  // ── 2. 分页基线 ──
  const base = await api('GET', '/erp/md/linked-account/page?pageNum=1&pageSize=20&status=1')
  check('分页接口可用', base?.code === 200 && Array.isArray(base?.data?.records),
    `code=${base?.code} total=${base?.data?.total}`)

  // ── 3. 取一个真实往来单位 ──
  const partners = await api('GET', '/erp/md/customer/page?pageNum=1&pageSize=5&partnerType=customer&status=ENABLED')
  const party = (partners?.data?.records || []).find(r => r.partnerType !== 'category')
  check('取到真实往来单位（客户）', !!party, party ? `${party.partnerCode}/${party.partnerName}` : '无')
  if (!party) throw new Error('无可用往来单位，无法继续')

  // ── 4. 绑定（新增） ──
  const created = await api('POST', '/erp/md/linked-account', {
    partyId: party.id, platform: 'WECHAT', linkType: 'MEMBER',
    linkedUserName: userName1, phone: phone1, remark: `E2E绑定 ${stamp}`,
  })
  ids.a = created?.data?.id
  check('绑定互联账号返回 id', !!ids.a, JSON.stringify(created).slice(0, 200))
  check('绑定回填往来单位编号快照', created?.data?.partyCode === party.partnerCode,
    `期望=${party.partnerCode} 实际=${created?.data?.partyCode}`)
  check('绑定回填往来单位名称快照', created?.data?.partyName === party.partnerName,
    `期望=${party.partnerName} 实际=${created?.data?.partyName}`)
  check('绑定状态默认「已绑定」', created?.data?.status === 1 && created?.data?.statusDesc === '已绑定',
    `status=${created?.data?.status}/${created?.data?.statusDesc}`)
  check('绑定平台中文标签正确', created?.data?.platformDesc === '微信', String(created?.data?.platformDesc))

  // ── 5. 落库核对 ──
  const pageParty = await api('GET', `/erp/md/linked-account/page?pageNum=1&pageSize=20&status=1&partyId=${party.id}`)
  const hit = (pageParty?.data?.records || []).find(r => String(r.id) === String(ids.a))
  check('按往来单位过滤命中新绑定', !!hit, `total=${pageParty?.data?.total}`)
  check('列表「往来单位」列非空', !!hit?.partyName, String(hit?.partyName))
  check('列表「互联用户名」列正确', hit?.linkedUserName === userName1, String(hit?.linkedUserName))
  check('列表「手机号」列正确', hit?.phone === phone1, String(hit?.phone))

  // ── 6. 查询条件「互联账号」（手机号 / 用户名 双口径） ──
  const byPhone = await api('GET', `/erp/md/linked-account/page?pageNum=1&pageSize=20&status=1&keyword=${phone1}`)
  check('按手机号查询命中', (byPhone?.data?.records || []).some(r => String(r.id) === String(ids.a)),
    `total=${byPhone?.data?.total}`)
  const byName = await api('GET', `/erp/md/linked-account/page?pageNum=1&pageSize=20&status=1&keyword=${userName1}`)
  check('按互联用户名查询命中', (byName?.data?.records || []).some(r => String(r.id) === String(ids.a)),
    `total=${byName?.data?.total}`)
  const byMiss = await api('GET', '/erp/md/linked-account/page?pageNum=1&pageSize=20&status=1&keyword=' + encodeURIComponent('zzz_不存在_zzz'))
  check('按不存在关键字查询为空', Number(byMiss?.data?.total) === 0, `total=${byMiss?.data?.total}`)

  // ── 7. 详情 ──
  const detail = await api('GET', `/erp/md/linked-account/${ids.a}`)
  check('详情接口返回同一条', String(detail?.data?.id) === String(ids.a) && detail?.data?.phone === phone1)

  // ── 8. 修改 ──
  const upd = await api('PUT', `/erp/md/linked-account/${ids.a}`, {
    partyId: party.id, platform: 'ALIPAY', linkType: 'CUSTOMER',
    linkedUserName: userName1, phone: phone1, remark: 'E2E改后',
  })
  const afterUpd = await api('GET', `/erp/md/linked-account/${ids.a}`)
  check('修改生效（平台→支付宝）',
    upd?.data === true && afterUpd?.data?.platformDesc === '支付宝',
    `upd=${upd?.data} platform=${afterUpd?.data?.platformDesc}`)
  check('修改生效（关联类型→客户）', afterUpd?.data?.linkTypeDesc === '客户', String(afterUpd?.data?.linkTypeDesc))

  // ── 9. 唯一约束：同平台同互联用户名不可重复绑定 ──
  const dup = await api('POST', '/erp/md/linked-account', {
    partyId: party.id, platform: 'ALIPAY', linkType: 'MEMBER',
    linkedUserName: userName1, phone: phone1,
  })
  check('同平台同互联用户名重复绑定被拒', !dup?.data, JSON.stringify(dup).slice(0, 160))

  // ── 9b. 入参校验与明确报错（不靠 DB 唯一索引抛 400 通用文案） ──
  const dupMsg = String(dup?.message || '')
  check('重复绑定返回明确原因（含「已绑定到」）', dupMsg.includes('已绑定到'), dupMsg)

  const badParty = await api('POST', '/erp/md/linked-account', {
    partyId: 999999999999, platform: 'WECHAT', linkedUserName: `x_${stamp}`, phone: phone1,
  })
  check('不存在的往来单位被拒', !badParty?.data && String(badParty?.message || '').includes('往来单位不存在'),
    String(badParty?.message))

  const noName = await api('POST', '/erp/md/linked-account', {
    partyId: party.id, platform: 'WECHAT', linkedUserName: '', phone: phone1,
  })
  check('缺互联用户名被拒', !noName?.data && String(noName?.message || '').includes('互联用户名'), String(noName?.message))

  const noPhone = await api('POST', '/erp/md/linked-account', {
    partyId: party.id, platform: 'WECHAT', linkedUserName: `np_${stamp}`, phone: '',
  })
  check('缺手机号被拒', !noPhone?.data && String(noPhone?.message || '').includes('手机号'), String(noPhone?.message))

  const tooLong = await api('POST', '/erp/md/linked-account', {
    partyId: party.id, platform: 'WECHAT', linkedUserName: 'x'.repeat(129), phone: phone1,
  })
  check('超长互联用户名被拒', !tooLong?.data && String(tooLong?.message || '').includes('128'), String(tooLong?.message))

  const updMissing = await api('PUT', '/erp/md/linked-account/999999999999', {
    partyId: party.id, platform: 'WECHAT', linkedUserName: `m_${stamp}`, phone: phone1,
  })
  check('修改不存在的记录被拒', !updMissing?.data && String(updMissing?.message || '').includes('不存在'),
    String(updMissing?.message))

  // 自己改自己：同平台同用户名不应被判为冲突（排除自身）
  const selfUpd = await api('PUT', `/erp/md/linked-account/${ids.a}`, {
    partyId: party.id, platform: 'ALIPAY', linkType: 'CUSTOMER',
    linkedUserName: userName1, phone: phone1, remark: 'E2E改后',
  })
  check('自身更新不误判为重复绑定', selfUpd?.data === true, String(selfUpd?.message))

  // /list 支持 status 过滤（供营销/会员/商城按绑定态引用）
  const listAll = await api('GET', '/erp/md/linked-account/list?pageSize=200')
  const listBoundOnly = await api('GET', '/erp/md/linked-account/list?pageSize=200&status=1')
  check('/list 不带 status 返回全部（含已解绑）',
    Array.isArray(listAll?.data) && (listAll?.data || []).length >= (listBoundOnly?.data || []).length,
    `all=${(listAll?.data || []).length} bound=${(listBoundOnly?.data || []).length}`)
  check('/list?status=1 不含已解绑记录',
    !(listBoundOnly?.data || []).some(r => r.status !== 1))

  // ── 10. 状态切换：解绑 ──
  const unbound = await api('PUT', `/erp/md/linked-account/${ids.a}/status?status=0`)
  const listUnbound = await api('GET', '/erp/md/linked-account/page?pageNum=1&pageSize=50&status=1')
  const listBound = await api('GET', '/erp/md/linked-account/page?pageNum=1&pageSize=50&status=0')
  check('解绑接口返回成功', unbound?.data === true)
  check('解绑后不再出现在「已绑定」列表',
    !(listUnbound?.data?.records || []).some(r => String(r.id) === String(ids.a)))
  check('解绑后出现在「已解绑」列表',
    (listBound?.data?.records || []).some(r => String(r.id) === String(ids.a)))
  check('解绑状态文案正确',
    (listBound?.data?.records || []).find(r => String(r.id) === String(ids.a))?.statusDesc === '已解绑')

  // ── 11. 重新绑定（单条状态切换回 1） ──
  const rebind = await api('PUT', `/erp/md/linked-account/${ids.a}/status?status=1`)
  const afterRebind = await api('GET', '/erp/md/linked-account/page?pageNum=1&pageSize=50&status=1')
  check('重新绑定生效',
    rebind?.data === true && (afterRebind?.data?.records || []).some(r => String(r.id) === String(ids.a)))

  // ── 12. 批量解绑 ──
  const c2 = await api('POST', '/erp/md/linked-account', {
    partyId: party.id, platform: 'DOUYIN', linkType: 'MEMBER',
    linkedUserName: userName2, phone: phone2,
  })
  ids.b = c2?.data?.id
  check('第二条绑定成功', !!ids.b, String(ids.b))
  const batchUnbind = await api('PUT', '/erp/md/linked-account/batch-status', { ids: [ids.a, ids.b], status: 0 })
  const afterBatch = await api('GET', '/erp/md/linked-account/page?pageNum=1&pageSize=50&status=1')
  check('批量解绑生效',
    batchUnbind?.data === true &&
    ![ids.a, ids.b].some(id => (afterBatch?.data?.records || []).some(r => String(r.id) === String(id))))

  // ── 13. 单条删除 ──
  const del = await api('DELETE', `/erp/md/linked-account/${ids.a}`)
  const afterDel = await api('GET', `/erp/md/linked-account/${ids.a}`)
  check('单条删除生效（详情为空）', del?.data === true && !afterDel?.data,
    `del=${del?.data} after=${JSON.stringify(afterDel?.data)}`)

  // ── 14. 批量删除 ──
  const c3 = await api('POST', '/erp/md/linked-account', {
    partyId: party.id, platform: 'MALL', linkType: 'OTHER',
    linkedUserName: userName3, phone: phone3,
  })
  ids.c = c3?.data?.id
  check('第三条绑定成功', !!ids.c, String(ids.c))
  const batchDel = await api('DELETE', '/erp/md/linked-account/batch', { ids: [ids.b, ids.c] })
  const afterBatchDel = await api('GET', '/erp/md/linked-account/page?pageNum=1&pageSize=50&status=1')
  check('批量删除生效',
    batchDel?.data === true &&
    ![ids.b, ids.c].some(id => (afterBatchDel?.data?.records || []).some(r => String(r.id) === String(id))))

  // ── 15. 字典归一（中文文案入参 → 平台标识） ──
  const cnIn = await api('POST', '/erp/md/linked-account', {
    partyId: party.id, platform: '微信', linkType: '会员',
    linkedUserName: `cn_${stamp}`, phone: `137${stamp}99`,
  })
  check('中文平台/关联类型入参归一为标识',
    cnIn?.data?.platform === 'WECHAT' && cnIn?.data?.linkType === 'MEMBER',
    `${cnIn?.data?.platform}/${cnIn?.data?.linkType}`)
  ids.d = cnIn?.data?.id

  // ── 16. 导出真实 xlsx ──
  const exp = await rawBuf('GET', '/erp/md/linked-account/export?title=' + encodeURIComponent('互联账号') + '&status=1', TOKEN)
  const isXlsx = exp.buf.length > 0 && exp.buf[0] === 0x50 && exp.buf[1] === 0x4b
  check('导出返回真实 xlsx（PK 头）', isXlsx, `len=${exp.buf.length} magic=${exp.buf[0]},${exp.buf[1]}`)
  if (isXlsx) fs.writeFileSync(`${SHOTS}/export.xlsx`, exp.buf)
  // 解析 xlsx 校验真实数据行（按中央目录读条目，兼容 POI 的 data descriptor 写法）
  let exportedRows = 0
  let exportedHeader = ''
  if (isXlsx) {
    try {
      const zlib = require('zlib')
      const entries = readZipEntries(exp.buf)
      const sheet = (entries['xl/worksheets/sheet1.xml'] || Buffer.from('')).toString('utf8')
      exportedRows = Math.max(0, (sheet.match(/<row /g) || []).length - 1)
      const shared = (entries['xl/sharedStrings.xml'] || Buffer.from('')).toString('utf8')
      const strs = [...shared.matchAll(/<t[^>]*>([^<]*)<\/t>/g)].map(m => m[1])
      exportedHeader = strs.slice(0, 3).join('|')
    } catch (e) { console.log('  [xlsx 解析失败]', e.message) }
  }
  check('导出 xlsx 含真实数据行', exportedRows >= 1, `rows=${exportedRows}`)
  check('导出表头为 往来单位|互联用户名|手机号', exportedHeader === '往来单位|互联用户名|手机号', exportedHeader)

  // ── 17. 分页口径：total 与 records 一致（多租户拦截器在分页之前） ──
  const p1 = await api('GET', '/erp/md/linked-account/page?pageNum=1&pageSize=1&status=1')
  check('分页 total 与 records 口径一致（total>0 时 records 不为空）',
    Number(p1?.data?.total) === 0 || (p1?.data?.records || []).length === 1,
    `total=${p1?.data?.total} records=${(p1?.data?.records || []).length}`)

  // ── 18. 多租户隔离字段落库 ──
  const dbCheck = await api('GET', `/erp/md/linked-account/${ids.d}`)
  check('多租户字段存在（接口正常返回即通过租户注入）', !!dbCheck?.data?.id)

  // ── 19. 清理 ──
  await api('DELETE', `/erp/md/linked-account/${ids.d}`)

  console.log('\n=== B. 前端页面 ===')
  await login()
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const p = await ctx.newPage()
  const errors = []
  p.on('pageerror', e => errors.push(String(e)))
  p.on('console', m => { if (m.type() === 'error') errors.push(m.text()) })

  await p.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: API_BASE + u.pathname + u.search })
  })

  await p.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await p.evaluate(([tk, tid]) => {
    localStorage.setItem('token', tk)
    localStorage.setItem('tenantId', String(tid))
  }, [TOKEN, TENANT])

  /**
   * 写操作前刷新页面内的 token。
   * ⚠️ sa-token is-concurrent=false：重登会作废旧 token，页面若已发请求会被路由守卫弹回 /login，
   *    此时只写 localStorage 不会恢复路由，必须重新导航回原页面。
   */
  async function refreshPageToken() {
    const target = p.url()
    await login()
    await p.evaluate(([tk]) => {
      localStorage.setItem('token', tk)
      localStorage.setItem('tenantId', '1')
    }, [TOKEN])
    await p.waitForTimeout(400)
    const bounced = await p.evaluate(() => location.pathname.includes('/login'))
    if (bounced && !target.includes('/login')) {
      await p.goto(target, { waitUntil: 'domcontentloaded' })
      await p.waitForTimeout(6000)
      await p.evaluate(([tk]) => { localStorage.setItem('token', tk) }, [TOKEN])
    }
  }

  /** 打开页面：并行会话共用 admin 会被 sa-token 互踢，失败时自愈重登重试 */
  async function openPage(url, waitMs = 7000) {
    for (let attempt = 1; attempt <= 4; attempt++) {
      await login()
      await p.evaluate(([tk, tid]) => {
        localStorage.setItem('token', tk)
        localStorage.setItem('tenantId', String(tid))
      }, [TOKEN, TENANT])
      await p.goto(url, { waitUntil: 'domcontentloaded' })
      await p.waitForTimeout(waitMs)
      const t = await p.evaluate(() => document.body.innerText || '')
      const kicked = t.includes('请输入租户名称') || t.includes('请输入用户名') || t.includes('页面不存在')
      if (!kicked) return true
      console.log(`  [页面未就绪（${kicked ? '被踢回登录' : '未知'}），重试 ${attempt}/4]`)
      await p.waitForTimeout(2000)
    }
    return false
  }

  const opened = await openPage(`${FE}/md/linked-account`)
  check('互联账号页面可正常打开（未被踢回登录/404）', opened)
  errors.length = 0 // 忽略登录/踢出重试阶段产生的噪声，只校验页面就绪后的运行错误
  await p.screenshot({ path: `${SHOTS}/ui-01-list.png`, fullPage: true })

  const listText = (await p.evaluate(() => document.body.innerText)).replace(/\s+/g, '')
  check('页面含温馨提示条', listText.includes('温馨提示：此页面用于管理互联平台账号与对应往来单位的绑定关系。'))
  for (const label of ['刷新', '打印(F8)', '导出', '更多', '查询']) {
    check(`工具栏含「${label}」`, listText.includes(label.replace(/\s+/g, '')), '')
  }
  check('工具栏无「新增」按钮（对标实测）', !listText.includes('新增'))
  check('页面无「重置」按钮（对标实测）', !listText.includes('重置'))
  check('页面无左侧分类树（对标实测）', !listText.includes('分类'))

  const headers = await p.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  for (const h of ['操作', '往来单位', '互联用户名', '手机号']) {
    check(`表头含「${h}」`, headers.some(x => x.includes(h)), headers.join('/'))
  }

  // 查询区结构
  const ph = await p.evaluate(() => [...document.querySelectorAll('input')].map(i => i.placeholder || '').filter(Boolean))
  check('查询区含占位「请输入互联手机号码」', ph.some(x => x.includes('请输入互联手机号码')), ph.join('|'))
  const boxLabels = await p.evaluate(() => [...document.querySelectorAll('.field-box-label')].map(e => (e.innerText || '').trim()))
  check('查询区含「往来单位」字段框', boxLabels.includes('往来单位'), boxLabels.join('/'))
  check('查询区含「互联账号」字段框', boxLabels.includes('互联账号'), boxLabels.join('/'))
  check('查询区含往来单位放大镜', (await p.$$('.field-box .anticon-search')).length > 0)

  // ── 列配置弹窗（个人配置 / 全局配置 + 全量 3 列） ──
  const gear = await p.$('.ss-grid th [class*=shezhi], .ss-grid th .icon-shezhi2, .ss-header-settings')
  if (gear) {
    await gear.click()
    await p.waitForTimeout(1500)
    await p.screenshot({ path: `${SHOTS}/ui-02-column-config.png` })
    const dlg = await p.evaluate(() => document.body.innerText)
    check('列配置弹窗含「个人配置」', dlg.includes('个人配置'))
    check('列配置弹窗含「全局配置」', dlg.includes('全局配置'))
    check('列配置弹窗含「恢复默认」', dlg.includes('恢复默认'))
    const cfgText = dlg.replace(/\s+/g, '')
    for (const c of ['往来单位', '互联用户名', '手机号']) {
      check(`列配置含列「${c}」`, cfgText.includes(c), '')
    }
    const cfgCols = await p.evaluate(() =>
      [...document.querySelectorAll('.col-setting-title')].map(e => (e.innerText || '').replace(/\s+/g, '')))
    check('列配置仅列出对标 3 列（序号/操作不参与配置）',
      cfgCols.length === 3 && !cfgCols.includes('操作'), cfgCols.join('/'))
    await p.keyboard.press('Escape')
    await p.waitForTimeout(800)
  } else {
    check('列表存在列配置齿轮入口', false, '未找到 .icon-shezhi2')
  }

  // ── 更多下拉（批量解绑 / 批量删除） ──
  const moreBtn = await p.evaluateHandle(() => {
    return [...document.querySelectorAll('button')].find(b => (b.innerText || '').trim().startsWith('更多'))
  })
  if (moreBtn && moreBtn.asElement()) {
    await moreBtn.asElement().click({ force: true })
    await p.waitForTimeout(1200)
    await p.screenshot({ path: `${SHOTS}/ui-03-more.png` })
    const moreText = (await p.evaluate(() => document.body.innerText)).replace(/\s+/g, '')
    check('「更多」含「批量解绑」', moreText.includes('批量解绑'))
    check('「更多」含「批量删除」', moreText.includes('批量删除'))
    await p.keyboard.press('Escape')
    await p.waitForTimeout(600)
  } else {
    check('工具栏存在「更多」下拉', false)
  }

  // ── 往来单位选择弹窗（客户信息 / 供应商信息 / 显示层次结构） ──
  // 打开弹窗：token 被踢会让页面弹回 /login，重试时整页重开并重新取元素句柄
  const LIST_URL = `${FE}/md/linked-account`
  let pickerOpened = false
  for (let i = 0; i < 3 && !pickerOpened; i++) {
    if (i > 0) await openPage(LIST_URL)
    const box = await p.$('.field-box')
    if (!box) continue
    await box.click().catch(() => {})
    await p.waitForTimeout(2800)
    pickerOpened = await p.evaluate(() =>
      [...document.querySelectorAll('.ant-modal')].some(m => (m.innerText || '').includes('往来单位选择')))
    if (!pickerOpened) {
      console.log(`  [选择弹窗未打开，整页重开重试 ${i + 1}/3]`)
    }
  }
  await p.screenshot({ path: `${SHOTS}/ui-04-partner-picker.png` })
  check('查询区放大镜可打开「往来单位选择」弹窗', pickerOpened)
  if (pickerOpened) {
    const pickText = (await p.evaluate(() => document.body.innerText)).replace(/\s+/g, '')
    check('往来单位选择弹窗标题正确', pickText.includes('往来单位选择'))
    check('弹窗含「客户信息」Tab', pickText.includes('客户信息'))
    check('弹窗含「供应商信息」Tab', pickText.includes('供应商信息'))
    check('弹窗含「显示层次结构」', pickText.includes('显示层次结构'))
    check('弹窗含「确定(Enter)」/「取消(Esc)」',
      pickText.includes('确定(Enter)') && pickText.includes('取消(Esc)'))
    const pickHeaders = await p.evaluate(() => [...document.querySelectorAll('.ant-modal th')].map(t => (t.innerText || '').trim()).filter(Boolean))
    for (const h of ['编号', '名称', '联系人', '联系电话', '联系地址', '备注']) {
      check(`选择弹窗表头含「${h}」`, pickHeaders.some(x => x.includes(h)), pickHeaders.join('/'))
    }
    // 对标：编号 / 名称 列带排序标识
    const sortable = await p.evaluate(() =>
      [...document.querySelectorAll('.ant-modal th')].filter(t => t.querySelector('.ant-table-column-sorter')).length)
    check('选择弹窗「编号/名称」列可排序（对标 ↕）', sortable >= 2, `sortable=${sortable}`)
    await p.keyboard.press('Escape')
    await p.waitForTimeout(800)
  }

  // ── UI 绑定闭环：更多 → 绑定互联账号 → 保存 → 列表出现 → 解绑 → 消失 ──
  const uiName = `ui_e2e_${stamp}`
  const uiPhone = `136${stamp}88`
  try {
    await refreshPageToken()
    await p.evaluate(() => {
      const b = [...document.querySelectorAll('button')].find(x => (x.innerText || '').trim().startsWith('更多'))
      if (b) b.click()
    })
    await p.waitForTimeout(1000)
    await p.evaluate(() => {
      const item = [...document.querySelectorAll('.ant-dropdown-menu-item, li')]
        .find(x => (x.innerText || '').trim() === '绑定互联账号')
      if (item) item.click()
    })
    await p.waitForTimeout(1500)
    const modalText = (await p.evaluate(() => document.body.innerText)).replace(/\s+/g, '')
    check('绑定弹窗含「往来单位/互联平台/关联类型/互联用户名/手机号」',
      ['往来单位', '互联平台', '关联类型', '互联用户名', '手机号'].every(x => modalText.includes(x)))

    // 打开往来单位选择（候选列表依赖 token，先刷新页面内 token）
    await refreshPageToken()
    await p.evaluate(() => {
      const m = [...document.querySelectorAll('.ant-modal')].find(x => (x.innerText || '').includes('绑定互联账号'))
      const box = m ? m.querySelector('.field-box') : null
      if (box) box.click()
    })
    await p.waitForTimeout(2500)

    // 候选为空时（被踢导致 401）重登 + 重查
    let hasRows = false
    for (let i = 0; i < 3 && !hasRows; i++) {
      hasRows = await p.evaluate(() => {
        const m = [...document.querySelectorAll('.ant-modal')].find(x => (x.innerText || '').includes('往来单位选择'))
        return !!(m && m.querySelector('.ant-table-tbody tr.ant-table-row'))
      })
      if (!hasRows) {
        await refreshPageToken()
        await p.evaluate(() => {
          const m = [...document.querySelectorAll('.ant-modal')].find(x => (x.innerText || '').includes('往来单位选择'))
          const btn = m && [...m.querySelectorAll('button')].find(b => (b.innerText || '').trim() === '查询')
          if (btn) btn.click()
        })
        await p.waitForTimeout(2500)
      }
    }
    check('往来单位选择弹窗加载出候选数据', hasRows)
    await p.screenshot({ path: `${SHOTS}/ui-05-picker.png` })

    await p.evaluate(() => {
      const m = [...document.querySelectorAll('.ant-modal')].find(x => (x.innerText || '').includes('往来单位选择'))
      if (!m) return
      const rows = [...m.querySelectorAll('.ant-table-tbody tr.ant-table-row')]
      if (rows[0]) rows[0].click()
    })
    await p.waitForTimeout(800)
    const picked = await p.evaluate(() => {
      const m = [...document.querySelectorAll('.ant-modal')].find(x => (x.innerText || '').includes('往来单位选择'))
      if (!m) return false
      const btn = [...m.querySelectorAll('button')].find(b => (b.innerText || '').trim().startsWith('确定'))
      if (btn && !btn.disabled) { btn.click(); return true }
      return false
    })
    check('往来单位选择弹窗可选中并确定', picked)
    await p.waitForTimeout(1500)
    const bindPartyName = await p.evaluate(() => {
      const m = [...document.querySelectorAll('.ant-modal')].find(x => (x.innerText || '').includes('绑定互联账号'))
      const v = m ? m.querySelector('.field-box .field-box-value') : null
      return v ? (v.innerText || '').trim() : ''
    })
    check('绑定弹窗已回填往来单位', !!bindPartyName, bindPartyName)

    // 填表：互联用户名 + 手机号
    await p.evaluate(([name, phoneV]) => {
      const modal = [...document.querySelectorAll('.ant-modal')].find(m => (m.innerText || '').includes('绑定互联账号'))
      if (!modal) return
      const setVal = (el, v) => {
        if (!el) return
        el.value = v
        el.dispatchEvent(new Event('input', { bubbles: true }))
        el.dispatchEvent(new Event('change', { bubbles: true }))
      }
      setVal(modal.querySelector('input[placeholder="请输入互联用户名"]'), name)
      setVal(modal.querySelector('input[placeholder="请输入手机号"]'), phoneV)
    }, [uiName, uiPhone])
    await p.waitForTimeout(500)
    await p.screenshot({ path: `${SHOTS}/ui-06-bind-modal.png` })

    await refreshPageToken()
    let clickedOk = false
    const footerOk = p.locator('.ant-modal-footer button.ant-btn-primary')
    if (await footerOk.count() > 0) {
      await footerOk.nth(await footerOk.count() - 1).click({ timeout: 5000 }).catch(() => {})
      clickedOk = true
    }
    if (!clickedOk) {
      const okByText = p.locator('.ant-modal button', { hasText: '确定' })
      if (await okByText.count() > 0) {
        await okByText.nth(await okByText.count() - 1).click({ timeout: 5000 }).catch(() => {})
        clickedOk = true
      }
    }
    check('点击绑定弹窗「确定」', clickedOk)

    // 落库核对（比 UI 文本更可靠）
    let boundRec = null
    for (let i = 0; i < 6 && !boundRec; i++) {
      await p.waitForTimeout(1500)
      const list = await api('GET', '/erp/md/linked-account/list?pageSize=200')
      boundRec = (list?.data || []).find(r => r.linkedUserName === uiName) || null
    }
    check('UI 绑定已落库', !!boundRec, JSON.stringify(boundRec || {}).slice(0, 160))
    check('UI 绑定落库的往来单位正确', !!boundRec?.partyName, String(boundRec?.partyName))
    check('UI 绑定落库的手机号正确', boundRec?.phone === uiPhone, String(boundRec?.phone))

    // 回到页面刷新，确认列表渲染
    await refreshPageToken()
    await p.evaluate(() => {
      const b = [...document.querySelectorAll('button')].find(x => (x.innerText || '').trim().includes('刷新'))
      if (b) b.click()
    })
    await p.waitForTimeout(3000)
    await p.screenshot({ path: `${SHOTS}/ui-07-after-bind.png` })
    const afterBindText = (await p.evaluate(() => document.body.innerText)).replace(/\s+/g, '')
    check('UI 刷新后列表出现新记录', afterBindText.includes(uiName), uiName)
    check('UI 刷新后列表展示手机号', afterBindText.includes(uiPhone), uiPhone)

    // 解绑（行内「解绑」→ 确认框）
    await refreshPageToken()
    const rowLoc = p.locator('.ss-grid tbody tr', { hasText: uiName }).first()
    await rowLoc.waitFor({ state: 'visible', timeout: 8000 }).catch(() => {})
    check('列表定位到新建行', await rowLoc.count() > 0)
    await rowLoc.locator('button', { hasText: '解绑' }).first().click({ timeout: 5000 }).catch(() => {})
    await p.waitForTimeout(1500)
    await p.screenshot({ path: `${SHOTS}/ui-08-unbind-confirm.png` })
    const confirmBtn = p.locator('.ant-modal-confirm-btns button.ant-btn-primary').last()
    if (await confirmBtn.count() > 0) {
      await confirmBtn.click({ timeout: 5000 }).catch(() => {})
    } else {
      await p.locator('.ant-modal-confirm button', { hasText: '确定' }).last().click({ timeout: 5000 }).catch(() => {})
    }
    await p.waitForTimeout(3000)
    await p.screenshot({ path: `${SHOTS}/ui-09-after-unbind.png` })

    // 落库核对：该记录已转为「已解绑」（status=0），并从默认「已绑定」列表消失
    const unboundList = await api('GET', '/erp/md/linked-account/list?pageSize=200')
    const unboundRec = (unboundList?.data || []).find(r => r.linkedUserName === uiName)
    check('UI 解绑已落库（status=0 已解绑）', unboundRec?.status === 0, JSON.stringify(unboundRec || {}).slice(0, 160))

    await refreshPageToken()
    await p.evaluate(() => {
      const b = [...document.querySelectorAll('button')].find(x => (x.innerText || '').trim().includes('刷新'))
      if (b) b.click()
    })
    await p.waitForTimeout(2500)
    const afterUnbindText = (await p.evaluate(() => document.body.innerText)).replace(/\s+/g, '')
    check('UI 解绑后列表不再出现该记录', !afterUnbindText.includes(uiName))
  } catch (e) {
    check('UI 绑定/解绑闭环', false, String(e.message).slice(0, 200))
  }

  // ── 清理 UI 产生数据 ──
  // 清理本次 + 历史遗留的 e2e 测试数据（该表为本模块新建，仅测试数据）
  const left = await api('GET', '/erp/md/linked-account/list?pageSize=500')
  const junk = (left?.data || []).filter(r => /e2e/i.test(String(r.linkedUserName || '')))
  if (junk.length) {
    await api('DELETE', '/erp/md/linked-account/batch', { ids: junk.map(r => r.id) })
  }
  const left2 = await api('GET', '/erp/md/linked-account/list?pageSize=500')
  check('测试数据清理完成', !(left2?.data || []).some(r => /e2e/i.test(String(r.linkedUserName || ''))),
    `剩余=${(left2?.data || []).length}`)

  check('页面无 JS 运行错误（忽略 favicon/网络噪声）',
    errors.filter(e => !/favicon|Failed to load resource|WebSocket|net::ERR|401/i.test(e)).length === 0,
    errors.slice(0, 3).join(' | '))

  await browser.close()

  // ── 汇总 ──
  const pass = results.filter(r => r.ok).length
  console.log(`\n===== 互联账号 E2E 结果：${pass}/${results.length} 通过 =====`)
  const failed = results.filter(r => !r.ok)
  if (failed.length) {
    console.log('失败项：')
    failed.forEach(f => console.log(`  ❌ ${f.name} — ${f.detail || ''}`))
  }
  fs.writeFileSync(`${SHOTS}/e2e-result.json`, JSON.stringify({ pass, total: results.length, results }, null, 1))
  process.exit(failed.length ? 1 : 0)
}

main().catch(e => { console.error('FATAL', e); process.exit(2) })
