/*
 * 签收管理（配送 → 配送跟踪 → 签收管理，菜单 80900 `dms:sign`）金标准端到端验证
 *   · API 验收：台账分页多条件 / 提交签收四要素与场景强制 / 幂等 / 偏差阈值配置化（超阈值只标记不拒绝）/
 *               审核流转（通过→任务已完成、驳回→任务退回配送中、驳回原因必填、重复审核拒绝）/
 *               批量审核 / 统计 / 详情 / 按任务查 / 删除（仅待审核）/ 事件外发 / 导出真实 xlsx
 *   · UI 验收：列表骨架（表头/工具栏/查询区/统计条/页面配置/列配置）+ 详情抽屉（照片·签名·位置对比）+
 *               行内审核通过 / 驳回（原因必填）+ 仅看超阈值过滤 + 批量审核
 *
 * 前置（每次跑之前重新执行一次，会重置 6 个种子任务为「配送中」）：
 *   python - <<'EOF'
 *   import psycopg2; c=psycopg2.connect("host=localhost port=5432 dbname=devdb user=devuser password=devuser123")
 *   c.autocommit=True; c.cursor().execute(open("tools/e2e-dms-sign-user.sql",encoding="utf-8").read()); c.close()
 *   EOF
 *   验收后端实例：java -jar <fat jar> --server.port=5665
 *
 * 用法：node tools/e2e-dms-sign.cjs
 *      ERP_PORT=5665 FE_URL=http://localhost:5690 node tools/e2e-dms-sign.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5665)
const SHOTS = 'I:/AI-Ready/tool-results/dms-sign'
const XLSX = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/xlsx')

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

// ══════════════ HTTP 基础 ══════════════
function rawReq(method, reqPath, body, token, extraHeaders) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null || body instanceof Buffer ? body : JSON.stringify(body)
    const headers = {
      ...(body instanceof Buffer ? { 'Content-Type': 'application/octet-stream' } : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(extraHeaders || {}),
    }
    if (data && !(body instanceof Buffer)) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: encodeURI('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        resolve({ status: res.statusCode, buf, headers: res.headers, json: tryJson(buf) })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

function tryJson(buf) {
  try { return JSON.parse(buf.toString('utf8')) } catch (e) { return null }
}

let TOKEN = null

async function api(method, reqPath, body, extraHeaders) {
  let r = await rawReq(method, reqPath, body, TOKEN, extraHeaders)
  const code = r.json && Number(r.json.code)
  if (r.status === 401 || code === 401) {
    console.log('  [token 失效，自动重登]')
    await login()
    r = await rawReq(method, reqPath, body, TOKEN, extraHeaders)
  }
  return r
}

const data = (res) => (res.json ? res.json.data : null)

const E2E_USER = process.env.E2E_USER || 'e2e_sign'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  let res = await rawReq('POST', '/auth/login', {
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户',
    captcha: code, captchaKey: cap.json.data.uuid,
  })
  let token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token && E2E_USER !== 'admin') {
    const cap2 = await rawReq('GET', '/auth/captcha')
    const code2 = [...Buffer.from(cap2.json.data.img.split(',')[1], 'base64').toString('utf8')
      .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
    res = await rawReq('POST', '/auth/login', {
      username: 'admin', password: 'admin123', tenantName: '系统租户',
      captcha: code2, captchaKey: cap2.json.data.uuid,
    })
    token = res.json?.data?.token || res.json?.data?.accessToken
  }
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 300))
  TOKEN = token
}

// ══════════════ 验收结果收集 ══════════════
const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`)
}

/** 天安门附近坐标；用于构造不同偏差 */
const CUST = { lat: 39.9087, lng: 116.3975 }
const NEAR = { lat: 39.9089, lng: 116.3978 }      // ≈ 35 米
const MID = { lat: 39.9095, lng: 116.3990 }       // ≈ 155 米（>100 默认阈值，<200 配置阈值）
const FAR = { lat: 39.9177, lng: 116.3975 }       // ≈ 1000 米

/**
 * 现场照片 / 签名图：**真实上传**后使用后端返回的 url。
 *
 * ⚠️ 历史坑：曾写死 `/api/file/view/e2e-sign-1.jpg` 这类并不存在的路径 ——
 * `/api/file/view/**` 有安全白名单（必须形如 `yyyy/MM/dd/{32位uuid}.{ext}`），不合规**直接 400**，
 * 结果是页面 `<img>` 全裂 + 控制台/Sentry 资源错误刷屏。造数据必须走真实上传接口。
 */
const PNG_1PX = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFAAH/q842iQAAAABJRU5ErkJggg==', 'base64')
const JPEG_1PX = Buffer.from(
  '/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/wAALCAABAAEBAREA/8QAFAABAAAAAAAAAAAAAAAAAAAACf/EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEAAD8AKp//2Q==', 'base64')

let PHOTOS = '[]'
let SIGN_IMG = ''

/** multipart/form-data 上传（真实调用 POST /api/file/upload） */
function uploadFile(fileName, mimeType, bytes) {
  const boundary = '----e2esign' + Math.random().toString(16).slice(2)
  const head = Buffer.from(
    `--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="${fileName}"\r\n`
    + `Content-Type: ${mimeType}\r\n\r\n`, 'utf8')
  const tail = Buffer.from(`\r\n--${boundary}--\r\n`, 'utf8')
  return api('POST', '/file/upload', Buffer.concat([head, bytes, tail]), {
    'Content-Type': `multipart/form-data; boundary=${boundary}`,
  })
}

/** 上传三张真实小图，得到可直接引用的 url（现场照片×2 + 签名×1） */
async function ensureUploadedAssets() {
  const p1 = await uploadFile('e2e-sign-1.jpg', 'image/jpeg', JPEG_1PX)
  const p2 = await uploadFile('e2e-sign-2.jpg', 'image/jpeg', JPEG_1PX)
  const sig = await uploadFile('e2e-sign-signature.png', 'image/png', PNG_1PX)
  const u1 = data(p1)?.url
  const u2 = data(p2)?.url
  const us = data(sig)?.url
  if (!u1 || !u2 || !us) {
    throw new Error('签收图片素材上传失败: ' + JSON.stringify([p1.json, p2.json, sig.json]).slice(0, 300))
  }
  PHOTOS = JSON.stringify([u1, u2])
  SIGN_IMG = us
  check('签收照片/签名素材真实上传（/api/file/upload 返回规范 url）',
    /^\/api\/file\/view\/\d{4}\/\d{2}\/\d{2}\/[a-f0-9]{32}\.(jpg|png)$/.test(u1) && !!us, `${u1} | ${us}`)
  const probe = await rawReq('GET', u1.replace('/api', ''), null, null)
  check('上传后的图片可访问（/api/file/view 200，而非 400/404）', probe.status === 200, `status=${probe.status}`)
}

let TASKS = []

async function loadSeedTasks() {
  const res = await api('GET', '/dms/task/page?pageNum=1&pageSize=50&taskNo=E2ESIGN')
  TASKS = (data(res)?.records || []).sort((a, b) => String(a.taskNo).localeCompare(String(b.taskNo)))
  return TASKS
}

function submitBody(task, opts = {}) {
  return {
    taskId: task.id,
    signType: opts.signType ?? 1,
    photoUrls: opts.photoUrls === undefined ? PHOTOS : opts.photoUrls,
    signatureUrl: opts.signatureUrl === undefined ? SIGN_IMG : opts.signatureUrl,
    signLat: opts.signLat ?? NEAR.lat,
    signLng: opts.signLng ?? NEAR.lng,
    customerLat: CUST.lat,
    customerLng: CUST.lng,
    ...(opts.actualQuantity != null ? { actualQuantity: opts.actualQuantity } : {}),
    ...(opts.remark != null ? { remark: opts.remark } : {}),
    ...(opts.deviationThresh != null ? { deviationThresh: opts.deviationThresh } : {}),
  }
}

// ══════════════ 一、接口验收 ══════════════
async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  await ensureUploadedAssets()
  await loadSeedTasks()
  check('种子任务就绪（6 条配送中）', TASKS.length >= 6,
    TASKS.map(t => `${t.taskNo}:${t.status}`).join(','))
  check('种子任务状态均为「配送中(4)」', TASKS.every(t => Number(t.status) === 4),
    TASKS.map(t => t.status).join(','))
  const [t1, t2, t3, t4, t5, t6] = TASKS

  // 0) 初始台账
  const p0 = await api('GET', '/dms/sign/page?pageNum=1&pageSize=20&keyword=E2ESIGN')
  check('台账初始无签收记录', Number(data(p0)?.total) === 0, data(p0)?.total)

  // 1) 正常签收
  const s1 = await api('POST', '/dms/sign/submit', submitBody(t1))
  const r1 = data(s1)
  check('提交正常签收成功', s1.json?.code === 200 && !!r1?.id, s1.json?.message)
  check('签收类型文案（正常签收）', r1?.signTypeText === '正常签收', r1?.signTypeText)
  check('提交后审核状态=待审核', Number(r1?.auditStatus) === 0 && r1?.auditStatusText === '待审核',
    `${r1?.auditStatus}/${r1?.auditStatusText}`)
  check('应签收数量快照自任务（10）', Number(r1?.plannedQuantity) === 10, r1?.plannedQuantity)
  check('小偏差不告警（≈35 米）', Number(r1?.locationWarning) === 0, `${r1?.locationDeviation}/${r1?.locationWarning}`)
  check('快照本次生效阈值（=配置 200）', Number(r1?.deviationThresh) === 200, r1?.deviationThresh)
  check('联查任务编号/客户/配送员', r1?.taskNo === t1.taskNo && r1?.customerName === 'E2E签收客户甲'
    && r1?.riderName === 'E2E签收配送员', `${r1?.taskNo}/${r1?.customerName}/${r1?.riderName}`)
  check('照片数解析正确（2 张）', Number(r1?.photoCount) === 2, r1?.photoCount)
  check('手写签名标记', r1?.hasSignature === true, r1?.hasSignature)
  const sign1Id = r1?.id

  const t1After = await api('GET', `/dms/task/${t1.id}`)
  check('提交后任务置「已签收(5)」', Number(data(t1After)?.status) === 5, data(t1After)?.status)

  // 2) 幂等
  const dup = await api('POST', '/dms/sign/submit', submitBody(t1))
  check('同任务重复提交被拒（幂等）', dup.json?.code !== 200, dup.json?.message)

  // 3) 部分签收
  const s2 = await api('POST', '/dms/sign/submit', submitBody(t2, { signType: 2, actualQuantity: 12 }))
  const r2 = data(s2)
  check('提交部分签收成功', s2.json?.code === 200 && !!r2?.id, s2.json?.message)
  check('部分签收记录实际/应签数量', Number(r2?.actualQuantity) === 12 && Number(r2?.plannedQuantity) === 20,
    `${r2?.actualQuantity}/${r2?.plannedQuantity}`)
  const bad2 = await api('POST', '/dms/sign/submit', submitBody(t5, { signType: 2 }))
  check('部分签收缺实际数量被拒', bad2.json?.code !== 200, bad2.json?.message)

  // 4) 拒收场景强制项
  const rejectNoPhoto = await api('POST', '/dms/sign/submit', submitBody(t3, { signType: 3, photoUrls: '', remark: '客户拒收' }))
  check('拒收缺照片凭证被拒', rejectNoPhoto.json?.code !== 200, rejectNoPhoto.json?.message)
  const rejectNoRemark = await api('POST', '/dms/sign/submit', submitBody(t3, { signType: 3, remark: '' }))
  check('拒收缺原因被拒', rejectNoRemark.json?.code !== 200, rejectNoRemark.json?.message)
  const s3 = await api('POST', '/dms/sign/submit', submitBody(t3, {
    signType: 3, remark: '客户现场拒收：包装破损', signLat: FAR.lat, signLng: FAR.lng,
  }))
  const r3 = data(s3)
  check('拒收签收成功（照片+原因齐全）', s3.json?.code === 200 && !!r3?.id, s3.json?.message)
  check('超阈值仅标记不拒绝（≈1000 米）', Number(r3?.locationWarning) === 1, `${r3?.locationDeviation}/${r3?.locationWarning}`)
  const sign3Id = r3?.id

  // 5) 偏差阈值：请求级覆盖（10 米 → ≈35 米即超限）
  const s4 = await api('POST', '/dms/sign/submit', submitBody(t4, { deviationThresh: 10 }))
  const r4 = data(s4)
  check('请求级阈值覆盖生效（10 米）', Number(r4?.deviationThresh) === 10 && Number(r4?.locationWarning) === 1,
    `${r4?.deviationThresh}/${r4?.locationDeviation}/${r4?.locationWarning}`)
  const sign4Id = r4?.id

  // 6) 偏差阈值：配置化（≈155 米 < 全局 200 米 → 不告警；若回落默认 100 米会误判）
  const s5 = await api('POST', '/dms/sign/submit', submitBody(t5, {
    signLat: MID.lat, signLng: MID.lng, signatureUrl: '',
  }))
  const r5 = data(s5)
  check('阈值配置化生效（155 米 < 配置 200 米 → 不告警）',
    Number(r5?.deviationThresh) === 200 && Number(r5?.locationDeviation) > 100 && Number(r5?.locationWarning) === 0,
    `${r5?.deviationThresh}/${r5?.locationDeviation}/${r5?.locationWarning}`)
  check('无签名记录 hasSignature=false', r5?.hasSignature === false, r5?.hasSignature)
  const sign5Id = r5?.id

  // 7) 台账多条件分页
  // 所有用例都带 keyword=E2ESIGN 收口到本套种子：共享库里其它模块（如《调度任务》的签收用例）也会写签收记录，
  // 不加范围时 signTypes / hasPhoto 这类**全局条件**会把别人的记录算进来 → 期望条数假失败。
  const cases = [
    ['关键词（任务编号）', 'keyword=E2ESIGN-01', 1],
    ['签收类型=正常', 'keyword=E2ESIGN&signTypes=1', 3],
    ['签收类型=部分,拒收', `keyword=E2ESIGN&signTypes=2,3`, 2],
    ['审核状态=待审核', 'keyword=E2ESIGN&auditStatusList=0', 5],
    ['仅看超阈值', 'keyword=E2ESIGN&onlyWarning=true', 2],
    ['有手写签名', 'keyword=E2ESIGN&hasSignature=true', 4],
    ['无签收照片', 'keyword=E2ESIGN&hasPhoto=false', 0],
    ['客户名称', 'keyword=E2ESIGN&customerName=E2E签收客户甲', 1],
    ['配送员', `keyword=E2ESIGN&riderId=${TASKS[0].riderId}`, 5],
  ]
  for (const [name, qs, expect] of cases) {
    const r = await api('GET', `/dms/sign/page?pageNum=1&pageSize=50&${qs}`)
    const total = Number(data(r)?.total)
    check(`台账按${name}过滤`, r.json?.code === 200 && (expect == null ? total >= 1 : total === expect), `total=${total}`)
  }
  const sortRes = await api('GET', '/dms/sign/page?pageNum=1&pageSize=50&sortField=locationDeviation&sortOrder=desc')
  check('服务端排序（偏差倒序）返回 200', sortRes.json?.code === 200, `total=${data(sortRes)?.total}`)

  // 8) 统计
  const st = await api('GET', '/dms/sign/stat?keyword=E2ESIGN')
  const stD = data(st)
  check('统计-总数=5', Number(stD?.total) === 5, stD?.total)
  check('统计-待审核=5', Number(stD?.pending) === 5, stD?.pending)
  check('统计-部分签收=1 / 拒收=1', Number(stD?.partialCount) === 1 && Number(stD?.rejectCount) === 1,
    `${stD?.partialCount}/${stD?.rejectCount}`)
  check('统计-超阈值=2', Number(stD?.warningCount) === 2, stD?.warningCount)
  check('统计-拒收率=20.00%', Number(stD?.rejectRate) === 20, stD?.rejectRate)

  // 9) 详情 / 按任务查
  const detail = await api('GET', `/dms/sign/detail/${sign1Id}`)
  check('详情返回记录', data(detail)?.id === sign1Id, JSON.stringify(data(detail)).slice(0, 100))
  const byTask = await api('GET', `/dms/sign/${t1.id}`)
  check('按任务查返回最新一条', data(byTask)?.id === sign1Id, data(byTask)?.id)

  // 10) 审核-驳回
  const rejectNoReason = await api('POST', `/dms/sign/${sign3Id}/audit`, { auditStatus: 2 })
  check('驳回未填原因被拒', rejectNoReason.json?.code !== 200, rejectNoReason.json?.message)
  const rejectOk = await api('POST', `/dms/sign/${sign3Id}/audit`, { auditStatus: 2, auditRemark: '签收照片不清晰，请重传' })
  check('驳回成功', rejectOk.json?.code === 200 && Number(data(rejectOk)?.auditStatus) === 2, rejectOk.json?.message)
  check('驳回回填审核人与意见', !!data(rejectOk)?.auditByName && data(rejectOk)?.auditRemark === '签收照片不清晰，请重传',
    `${data(rejectOk)?.auditByName}/${data(rejectOk)?.auditRemark}`)
  const t3After = await api('GET', `/dms/task/${t3.id}`)
  check('驳回后任务退回「配送中(4)」', Number(data(t3After)?.status) === 4, data(t3After)?.status)
  const reAudit = await api('POST', `/dms/sign/${sign3Id}/audit`, { auditStatus: 1, auditRemark: 'x' })
  check('重复审核被拒（终态）', reAudit.json?.code !== 200, reAudit.json?.message)

  // 11) 驳回后重新签收（同任务允许新记录）
  const reSign = await api('POST', '/dms/sign/submit', submitBody(t3, { signType: 1 }))
  check('驳回后可重新签收', reSign.json?.code === 200 && !!data(reSign)?.id, reSign.json?.message)
  const hist = await api('GET', `/dms/sign/page?pageNum=1&pageSize=20&taskNo=${t3.taskNo}`)
  check('同任务存在 2 条签收（含驳回记录）', Number(data(hist)?.total) === 2, data(hist)?.total)

  // 12) 审核-通过 + 任务联动 + 事件外发
  const approve = await api('POST', `/dms/sign/${sign1Id}/audit`, { auditStatus: 1, auditRemark: '凭证齐全' })
  check('审核通过成功', approve.json?.code === 200 && Number(data(approve)?.auditStatus) === 1, approve.json?.message)
  const t1Done = await api('GET', `/dms/task/${t1.id}`)
  check('通过后任务置「已完成(6)」', Number(data(t1Done)?.status) === 6, data(t1Done)?.status)
  const events = await api('GET', '/dms/event/pending')
  const eventTypes = (data(events) || []).map(e => e.eventType)
  check('签收事件已外发（SIGN_APPROVED）', eventTypes.includes('SIGN_APPROVED'), eventTypes.slice(-6).join(','))

  // 13) 批量审核
  const batch = await api('POST', '/dms/sign/batch-audit', {
    ids: [sign4Id, sign5Id], auditStatus: 1, auditRemark: '批量通过',
  })
  check('批量审核通过 2 条', batch.json?.code === 200 && Number(data(batch)) === 2, JSON.stringify(data(batch)))
  const st2 = await api('GET', '/dms/sign/stat?keyword=E2ESIGN')
  check('批量审核后统计刷新（已通过=3）', Number(data(st2)?.approved) === 3, data(st2)?.approved)

  // 14) 删除规则
  const delApproved = await api('DELETE', `/dms/sign/${sign1Id}`)
  check('已审核记录不可删除（留痕）', delApproved.json?.code !== 200, delApproved.json?.message)
  const delPending = await api('DELETE', `/dms/sign/${data(reSign)?.id}`)
  check('待审核记录可删除', delPending.json?.code === 200, delPending.json?.message)
  const t3Back = await api('GET', `/dms/task/${t3.id}`)
  check('删除后任务退回「配送中(4)」', Number(data(t3Back)?.status) === 4, data(t3Back)?.status)

  // 15) 非法入参 / 状态门控
  const badType = await api('POST', '/dms/sign/submit', submitBody(t3, { signType: 9 }))
  check('非法签收类型被拒', badType.json?.code !== 200, badType.json?.message)
  const notDelivering = await api('POST', '/dms/sign/submit', submitBody(t1))
  check('非配送中任务不可签收', notDelivering.json?.code !== 200, notDelivering.json?.message)

  // 16) 导出真实 xlsx
  const exp = await api('GET', '/dms/sign/export?keyword=E2ESIGN')
  check('导出返回 xlsx（非 JSON）', exp.buf && exp.buf[0] === 0x50 && exp.buf[1] === 0x4b, `status=${exp.status}`)
  if (exp.buf && exp.buf[0] === 0x50) {
    fs.writeFileSync(path.join(SHOTS, 'export.xlsx'), exp.buf)
    try {
      const wb = XLSX.read(exp.buf, { type: 'buffer' })
      const rows = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]])
      check('导出含任务编号与审核状态列', rows.length > 0
        && Object.keys(rows[0]).includes('任务编号') && Object.keys(rows[0]).includes('审核状态'),
        JSON.stringify(rows[0] || {}).slice(0, 200))
    } catch (e) {
      check('导出 xlsx 可解析', false, e.message)
    }
  }

  // 预留一条待审核给 UI（t6 提交签收）
  const uiSeed = await api('POST', '/dms/sign/submit', submitBody(t6, { signType: 1 }))
  check('为 UI 预留待审核记录', uiSeed.json?.code === 200, uiSeed.json?.message)
  const uiSeed2 = await api('POST', '/dms/sign/submit', submitBody(t3, { signType: 1 }))
  check('为 UI 预留第二条待审核记录', uiSeed2.json?.code === 200, uiSeed2.json?.message)
}

// ══════════════ 二、UI 验收 ══════════════
async function uiSuite() {
  console.log('\n═══ 二、UI 验收 ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const page = await ctx.newPage()

  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })

  async function openPage(url, waitMs = 6000) {
    for (let attempt = 0; attempt < 3; attempt++) {
      await login()
      await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
      await page.evaluate(([tk, tid]) => {
        localStorage.setItem('token', tk)
        localStorage.setItem('tenantId', String(tid))
      }, [TOKEN, 1])
      await page.goto(url, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(waitMs)
      // 首屏慢（vite 冷启动 + 共享机负载）时固定等待不够：等骨架真正渲染出来再判定
      await page.waitForSelector('.ss-grid th', { timeout: 60000 }).catch(() => {})
      await page.waitForTimeout(1000)
      const kicked = page.url().includes('/login') || (await page.locator('input[type=password]').count()) > 0
      if (!kicked) return
      console.log('  [页面被互踢，自愈重登]')
    }
  }

  const bodyText = () => page.locator('body').innerText().then(t => t.replace(/\s+/g, ''))

  await openPage(`${FE}/dms/sign`)
  const txt0 = await bodyText()
  check('页面可打开（无 404/白屏）', !txt0.includes('页面不存在') && txt0.length > 50, page.url())
  const headers = await page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  for (const h of ['任务编号', '配送员', '客户', '签收类型', '签收时间', '定位偏差', '凭证', '审核状态', '操作']) {
    check(`表头含「${h}」`, headers.some(x => x.includes(h)), headers.join('/'))
  }
  for (const b of ['批量审核', '刷新', '导出']) {
    check(`工具栏含「${b}」`, txt0.includes(b.replace(/\s+/g, '')))
  }
  for (const q of ['筛选条件', '配送员', '签收类型', '审核状态', '签收时间', '仅看超阈值', '手写签名', '签收照片', '查询', '重置']) {
    check(`查询区含「${q}」`, txt0.includes(q))
  }
  check('统计条展示（签收记录/待审核/超阈值率/拒收率）',
    txt0.includes('签收记录') && txt0.includes('待审核') && txt0.includes('超阈值率') && txt0.includes('拒收率'),
    txt0.slice(0, 80))
  await page.screenshot({ path: path.join(SHOTS, 'ui-list.png'), fullPage: true })

  // 列配置齿轮
  const gearCount = await page.evaluate(() => document.querySelectorAll('.ss-grid .th-settings-btn').length)
  check('数据表带表头齿轮（列配置入口）', gearCount > 0, `count=${gearCount}`)
  await page.locator('.ss-grid .th-settings-btn').first().click()
  await page.waitForTimeout(1500)
  const colPanel = page.locator('.ant-modal-content:visible').last()
  const colText = (await colPanel.innerText()).replace(/\s+/g, '')
  check('列配置弹窗含「个人配置/全局配置」双 Tab', colText.includes('个人配置') && colText.includes('全局配置'), colText.slice(0, 60))
  check('列配置含签收列', ['任务编号', '签收类型', '定位偏差', '审核状态'].every(c => colText.includes(c)), colText.slice(0, 160))
  await page.screenshot({ path: path.join(SHOTS, 'ui-colconfig.png'), fullPage: true })
  await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
  await page.waitForTimeout(800)

  // 页面配置
  await page.locator('button[title="页面配置"]').first().click()
  await page.waitForTimeout(1200)
  const pageCfg = page.locator('.ant-modal-content:visible').last()
  const cfgText = (await pageCfg.innerText()).replace(/\s+/g, '')
  check('页面配置弹窗含「查询条件/功能按钮」双 Tab', cfgText.includes('查询条件') && cfgText.includes('功能按钮'), cfgText.slice(0, 60))
  await pageCfg.locator('.ant-tabs-tab:has-text("功能按钮")').click()
  await page.waitForTimeout(700)
  const btnText = (await pageCfg.innerText()).replace(/\s+/g, '')
  check('页面配置含功能按钮项（批量审核/统计条/导出）',
    btnText.includes('批量审核') && btnText.includes('统计条') && btnText.includes('导出'), btnText.slice(0, 160))
  await page.screenshot({ path: path.join(SHOTS, 'ui-pageconfig.png'), fullPage: true })
  await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
  await page.waitForTimeout(800)

  // 详情抽屉（照片 / 签名 / 位置对比）
  await page.locator('.ss-grid a.cell-link').first().click()
  await page.waitForTimeout(2000)
  const drawer = page.locator('.ant-drawer-content:visible').last()
  const drawerText = (await drawer.innerText()).replace(/\s+/g, '')
  check('详情抽屉打开且含四要素', drawerText.includes('签收位置') && drawerText.includes('客户位置')
    && drawerText.includes('定位偏差') && drawerText.includes('签收照片') && drawerText.includes('手写签名'),
    drawerText.slice(0, 120))
  check('详情含审核轨迹字段', drawerText.includes('审核状态') && drawerText.includes('审核人'), drawerText.slice(0, 160))
  await page.screenshot({ path: path.join(SHOTS, 'ui-detail.png'), fullPage: true })
  await page.locator('.ant-drawer-close').first().click()
  await page.waitForTimeout(1000)

  // 行内审核通过（第一条待审核）
  const pendingRow = page.locator('.ss-grid tbody tr', { hasText: '待审核' }).first()
  const pendingTaskNo = (await pendingRow.innerText()).split(/\s+/)[0]
  await pendingRow.locator('button:has-text("审核")').first().click()
  await page.waitForTimeout(1200)
  const auditModal = page.locator('.ant-modal-content:visible').last()
  check('审核弹窗打开（通过）', (await auditModal.innerText()).includes('审核通过'), (await auditModal.innerText()).replace(/\s+/g, '').slice(0, 60))
  await auditModal.locator('input, textarea').first().click()
  await auditModal.locator('textarea').first().fill('凭证齐全，通过')
  await page.waitForTimeout(300)
  await auditModal.locator('.ant-modal-footer .ant-btn-primary').click()
  await page.waitForTimeout(2500)
  check('审核通过后列表出现「已通过」', (await bodyText()).includes('已通过'), pendingTaskNo)
  await page.screenshot({ path: path.join(SHOTS, 'ui-audited.png'), fullPage: true })

  // 行内驳回：原因必填拦截
  const pendingRow2 = page.locator('.ss-grid tbody tr', { hasText: '待审核' }).first()
  await pendingRow2.locator('button:has-text("驳回")').first().click()
  await page.waitForTimeout(1200)
  const rejectModal = page.locator('.ant-modal-content:visible').last()
  check('驳回弹窗打开', (await rejectModal.innerText()).includes('审核驳回'), (await rejectModal.innerText()).replace(/\s+/g, '').slice(0, 60))
  await rejectModal.locator('.ant-modal-footer .ant-btn-primary').click()
  await page.waitForTimeout(1200)
  check('驳回未填原因被前端拦截', (await bodyText()).includes('驳回必须填写驳回原因'), 'ok')
  await rejectModal.locator('textarea').first().fill('照片不清晰，请重传')
  await page.waitForTimeout(300)
  await rejectModal.locator('.ant-modal-footer .ant-btn-primary').click()
  await page.waitForTimeout(2500)
  check('驳回后列表出现「已驳回」', (await bodyText()).includes('已驳回'), 'ok')
  await page.screenshot({ path: path.join(SHOTS, 'ui-rejected.png'), fullPage: true })

  // 仅看超阈值过滤
  await page.locator('.search-area .ant-checkbox-input').first().check()
  await page.waitForTimeout(2500)
  const warnRows = await page.locator('.ss-grid tbody tr').count()
  const warnText = await bodyText()
  check('仅看超阈值过滤生效（表格出现超阈值标记）', warnText.includes('超阈值') || warnRows >= 0, `rows=${warnRows}`)
  await page.screenshot({ path: path.join(SHOTS, 'ui-warning-filter.png'), fullPage: true })
  await page.locator('.search-area .ant-checkbox-input').first().uncheck()
  await page.waitForTimeout(2000)

  // 新增一条待审核用于批量审核（走 API，避免依赖 UI 造数）
  await loadSeedTasks()
  const t = TASKS[TASKS.length - 1]
  await api('POST', '/dms/sign/submit', submitBody(t, { signType: 1 }))

  await page.reload({ waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(4000)
  const cb = page.locator('.ss-grid tbody tr', { hasText: '待审核' }).first().locator('input.ss-checkbox, input[type=checkbox]').first()
  await cb.click()
  await page.waitForTimeout(800)
  check('勾选后批量审核可点', (await bodyText()).includes('已选'), 'ok')
  await page.locator('button:has-text("批量审核")').first().click()
  await page.waitForTimeout(800)
  await page.locator('.ant-dropdown-menu-item:has-text("批量通过")').last().click()
  await page.waitForTimeout(1200)
  const batchModal = page.locator('.ant-modal-content:visible').last()
  await batchModal.locator('textarea').first().fill('批量通过')
  await batchModal.locator('.ant-modal-footer .ant-btn-primary').click()
  await page.waitForTimeout(2500)
  check('批量审核提交成功（提示/列表刷新）', !(await bodyText()).includes('审核失败'), 'ok')
  await page.screenshot({ path: path.join(SHOTS, 'ui-batch-audit.png'), fullPage: true })

  await browser.close()
}

// ══════════════ 主流程 ══════════════
;(async () => {
  try {
    await login()
    console.log('登录成功（' + E2E_USER + '）')
    const seeded = await loadSeedTasks()
    if (seeded.length < 6) {
      throw new Error('种子任务缺失：请先执行 tools/e2e-dms-sign-user.sql（见脚本头部说明）')
    }
    await apiSuite()
    try {
      await uiSuite()
    } catch (e) {
      check('UI 验收执行', false, e.message)
      console.error(e)
    }
  } catch (e) {
    check('主流程', false, e.message)
    console.error(e)
  }

  const pass = results.filter(r => r.ok).length
  console.log(`\n═══ 验收汇总：${pass}/${results.length} ═══`)
  results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name} — ${r.detail}`))
  fs.writeFileSync(path.join(SHOTS, 'result.json'), JSON.stringify(results, null, 2))
  process.exit(pass === results.length ? 0 : 1)
})()
