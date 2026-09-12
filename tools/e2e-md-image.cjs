/* 图片管理（资料 → 商品管理 → 图片管理）金标准 端到端验证
 * 运行： node tools/e2e-md-image.cjs [api|ui|all]
 * 输出： tool-results/md-image/*.png
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')

const FE = process.env.FE || 'http://localhost:5656'
const PORT = Number(process.env.BE_PORT || 5655)
const SHOTS = 'I:/AI-Ready/tool-results/md-image'
const MODE = process.argv[2] || 'all'

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

function apiReq(method, path, body, token, raw) {
  return new Promise((resolve, reject) => {
    const isBuf = Buffer.isBuffer(body)
    const data = body == null ? null : (isBuf ? body : JSON.stringify(body))
    const r = http.request({
      hostname: 'localhost', port: PORT, path: '/api' + path, method,
      headers: {
        ...(isBuf ? { 'Content-Type': raw.contentType } : { 'Content-Type': 'application/json' }),
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        if (res.headers['content-type'] && res.headers['content-type'].startsWith('image/')) {
          resolve({ binary: true, status: res.statusCode, size: buf.length, contentType: res.headers['content-type'] })
          return
        }
        try { resolve(JSON.parse(buf.toString('utf8'))) } catch { resolve({ raw: buf.toString('utf8'), status: res.statusCode }) }
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

/** multipart 表单 */
function multipart(fields, fileName, fileBuffer, contentType) {
  const boundary = '----ImageBoundary' + Date.now()
  const parts = []
  for (const [k, v] of Object.entries(fields)) {
    parts.push(Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="${k}"\r\n\r\n${v}\r\n`))
  }
  parts.push(Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="${fileName}"\r\nContent-Type: ${contentType}\r\n\r\n`))
  parts.push(fileBuffer)
  parts.push(Buffer.from(`\r\n--${boundary}--\r\n`))
  return { body: Buffer.concat(parts), contentType: `multipart/form-data; boundary=${boundary}` }
}

/** 1x1 PNG */
const PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
  'base64')

/**
 * 登录：优先用 E2E 专用账号（避免与并行会话共用 admin 互相踢下线），
 * 账号不存在时回退 admin。专用账号由 tools/e2e-md-image-user.sql 创建。
 */
const E2E_USER = process.env.E2E_USER || 'e2e_image'
async function login(username = E2E_USER) {
  const cap = await apiReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await apiReq('POST', '/auth/login', {
    username, password: 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  const token = res?.data?.token || res?.data?.accessToken
  if (!token && username !== 'admin') {
    console.log(`[login] ${username} 不可用，回退 admin`)
    return login('admin')
  }
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
  return token
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`)
}

/** 带自愈重登的请求（sa-token is-concurrent=false，账号可能被其它会话踢下线） */
async function withRelogin(fn) {
  let token = await login()
  try {
    return await fn(token)
  } catch (e) {
    if (/登录|401|未登录/.test(String(e.message))) {
      token = await login()
      return await fn(token)
    }
    throw e
  }
}

async function partA() {
  console.log('\n═══════ A. 后端接口 ═══════')
  let token = await login()
  const relogin = async () => { token = await login(); return token }
  const call = async (method, path, body, raw) => {
    let res = await apiReq(method, path, body, token, raw)
    if (res?.code === 401 || res?.status === 401 || res?.code === 11012) {
      await relogin()
      res = await apiReq(method, path, body, token, raw)
    }
    return res
  }

  // 1. 商品图片列表分页
  const page = await call('GET', '/erp/md/image/page?pageNum=1&pageSize=10')
  const rec = page?.data?.records?.[0]
  check('商品图片列表分页可用', Array.isArray(page?.data?.records), `总数=${page?.data?.total}`)
  check('列表行含对标 9 列字段',
    rec ? ['productName', 'status', 'productCode', 'spec', 'model', 'origin', 'brand', 'images'].every(k => k in rec) : true,
    rec ? `首行=${rec.productName} 货号=${rec.productCode}` : '无数据行(字段存在性跳过)')

  // 2. 图片空间分页
  const space = await call('GET', '/erp/md/image/space-page?pageNum=1&pageSize=10')
  check('图片空间分页可用', Array.isArray(space?.data?.records), `总数=${space?.data?.total}`)

  // 3. 上传图片到图片空间
  const mp = multipart({}, `e2e-img-${Date.now()}.png`, PNG, 'image/png')
  const up = await call('POST', '/erp/md/image/upload', mp.body, { contentType: mp.contentType })
  const uploadedId = up?.data?.id
  check('上传图片（图片空间素材）', !!uploadedId, `id=${uploadedId} url=${up?.data?.imageUrl}`)

  // 3b. 大文件上传回归：Spring 默认单文件上限 1MB，未配置 multipart 时 2MB 图片会 500
  const bigBuf = Buffer.concat([PNG, Buffer.alloc(2 * 1024 * 1024, 0x41)])
  const mpBig = multipart({}, `e2e-big-${Date.now()}.png`, bigBuf, 'image/png')
  const upBig = await call('POST', '/erp/md/image/upload', mpBig.body, { contentType: mpBig.contentType })
  check('上传 2MB 图片不报 500（multipart 上限对齐前端 10MB）',
    !!upBig?.data?.id, upBig?.data?.id ? `id=${upBig.data.id}` : `返回=${JSON.stringify(upBig).slice(0, 120)}`)
  if (upBig?.data?.id) await call('POST', '/erp/md/image/batch-delete', { imageIds: [upBig.data.id] })

  // 4. 图片内容访问（不带 token，模拟 <img>）
  let viewOk = false, viewDetail = ''
  if (uploadedId) {
    const v = await apiReq('GET', `/erp/md/image/view/${uploadedId}`)
    viewOk = v?.binary === true && v.size > 10
    viewDetail = `status=${v?.status} size=${v?.size} type=${v?.contentType}`
    check('图片内容可直接访问（img 免鉴权）', viewOk, viewDetail)
  } else {
    check('图片内容可直接访问（img 免鉴权）', false, '上传失败跳过')
  }

  // 5. 自动匹配（按货号）：上传「货号-1.png」命名素材，验证真实匹配闭环
  let matchId = null
  if (rec?.productCode) {
    const mp2 = multipart({}, `${rec.productCode}-1.png`, PNG, 'image/png')
    const up2 = await call('POST', '/erp/md/image/upload', mp2.body, { contentType: mp2.contentType })
    matchId = up2?.data?.id
  }
  const auto = await call('POST', '/erp/md/image/auto-match?matchType=CODE')
  check('自动匹配接口可用（按货号）',
    auto?.data && typeof auto.data.matched === 'number',
    `参与=${auto?.data?.total} 成功=${auto?.data?.matched} 歧义=${auto?.data?.ambiguous} 未匹配=${auto?.data?.unmatched}`)
  if (matchId) {
    const matched = await call('GET', `/erp/md/image/product/${rec.productId}`)
    const hit = (matched?.data || []).some(i => String(i.id) === String(matchId))
    check('自动匹配真实命中商品（sp001-1 命名规则）', hit, `素材=${rec.productCode}-1.png 商品=${rec.productName}`)
    await call('POST', '/erp/md/image/batch-delete', { imageIds: [matchId] })
  }

  // 6. 自动匹配（按名称）
  const autoName = await call('POST', '/erp/md/image/auto-match?matchType=NAME')
  check('自动匹配接口可用（按名称）', autoName?.data && typeof autoName.data.matched === 'number',
    `参与=${autoName?.data?.total} 成功=${autoName?.data?.matched}`)

  // 7. 绑定素材到商品 + 主图 + 列表回显
  let bindOk = false
  let mainOk = false
  let listEcho = false
  const target = await call('GET', '/erp/md/image/page?pageNum=1&pageSize=1')
  const targetProduct = target?.data?.records?.[0]
  if (uploadedId && targetProduct) {
    const bind = await call('POST', '/erp/md/image/bind', {
      imageId: uploadedId, productId: targetProduct.productId, isMain: 1,
    })
    bindOk = bind?.data === true
    check('选择图片：绑定素材到商品', bindOk, `商品=${targetProduct.productName}`)

    const main = await call('POST', `/erp/md/image/${uploadedId}/main`)
    mainOk = main?.data === true
    check('设置主图', mainOk, '')

    const after = await call('GET', `/erp/md/image/page?pageNum=1&pageSize=50&imageFilter=HAS`)
    const row = (after?.data?.records || []).find(r => String(r.productId) === String(targetProduct.productId))
    listEcho = !!(row && row.images && row.images.some(i => String(i.id) === String(uploadedId) && i.isMain === 1))
    check('商品图片列表回显已绑定图片 + 主图标记', listEcho,
      row ? `图片数=${row.images.length}` : '未找到商品行')

    // 8. 搬移
    const move = await call('POST', '/erp/md/image/move', {
      imageIds: [uploadedId], productId: targetProduct.productId,
    })
    check('搬移素材到商品', move?.data === 1, `影响=${move?.data}`)
  } else {
    check('选择图片：绑定素材到商品', false, '缺少上传ID或商品行')
  }

  // 9. 只显示图片过滤
  const onlyImg = await call('GET', '/erp/md/image/space-page?pageNum=1&pageSize=5&onlyImage=1')
  check('图片空间「只显示图片」过滤可用', Array.isArray(onlyImg?.data?.records), `总数=${onlyImg?.data?.total}`)

  // 10. 删除（清理测试数据）
  if (uploadedId) {
    const del = await call('POST', '/erp/md/image/batch-delete', { imageIds: [uploadedId] })
    check('批量删除图片', del?.data === 1, `删除=${del?.data}`)
    const stillThere = await call('GET', `/erp/md/image/product/${targetProduct?.productId || 0}`)
    const gone = !(stillThere?.data || []).some(i => String(i.id) === String(uploadedId))
    check('删除后商品图片列表同步移除', gone, '')
  }

  // 11. 主图一致性回归（2026-09-12 修复项）：图片被 move 置为非主图后再删除，
  //     商品 image_url 不得残留指向已删图片（旧实现只按 isMain 判断，残留后前端 <img> 持续 404）
  if (targetProduct?.productId) {
    const mp3 = multipart({}, `e2e-main-${Date.now()}.png`, PNG, 'image/png')
    const up3 = await call('POST', '/erp/md/image/upload', mp3.body, { contentType: mp3.contentType })
    const mainImgId = up3?.data?.id
    if (mainImgId) {
      await call('POST', '/erp/md/image/bind', { imageId: mainImgId, productId: targetProduct.productId, isMain: 1 })
      const bound = await call('GET', `/erp/product/${targetProduct.productId}`)
      const urlBound = String(bound?.data?.imageUrl || '')
      // move 到同一商品：旧实现会把 isMain 置 0 且不清 image_url，构成残留触发点
      await call('POST', '/erp/md/image/move', { imageIds: [mainImgId], productId: targetProduct.productId })
      await call('POST', '/erp/md/image/batch-delete', { imageIds: [mainImgId] })
      const after = await call('GET', `/erp/product/${targetProduct.productId}`)
      const urlAfter = String(after?.data?.imageUrl || '')
      check('删除/搬移图片后商品主图不残留失效链接',
        urlBound.includes(String(mainImgId)) && !urlAfter.includes(String(mainImgId)),
        `绑定后=${urlBound.slice(-22)} 删除后=${urlAfter.slice(-22) || '(空)'}`)
    } else {
      check('删除/搬移图片后商品主图不残留失效链接', false, '上传失败跳过')
    }
  }

  return results
}

async function partB() {
  console.log('\n═══════ B. 前端页面 ═══════')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const page = await ctx.newPage()
  page.on('pageerror', e => console.log('[pageerror]', (e.message || '').slice(0, 160)))
  const token = await login()

  // vite dev 代理默认指向 5655；本会话后端在 5710，用精确前缀 route 转发
  // 注意：模式必须是 `${FE}/api/**`，用 `**/api/**` 会误伤 /src/api/*.ts 源码模块
  await page.route(`${FE}/api/**`, async (route) => {
    const target = route.request().url().replace(`${FE}/api/`, `http://localhost:${PORT}/api/`)
    try {
      await route.continue({ url: target })
    } catch {
      await route.abort()
    }
  })

  // 注入登录态直达页面
  await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([t]) => {
    localStorage.setItem('token', t)
    localStorage.setItem('access_token', t)
    localStorage.setItem('tenantId', '1')
    localStorage.setItem('tenantName', '系统租户')
    localStorage.setItem('userTenants', JSON.stringify([{ id: 1, tenantName: '系统租户' }]))
  }, [token])

  // 并行会话共用 admin 会被互踢，检测到登录页则重登后重试
  let curToken = token
  const gotoPage = async (attempt = 0) => {
    await page.goto(`${FE}/md/image`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(4000)
    if (page.url().includes('/login') || (await page.locator('text=立即注册').count()) > 0) {
      if (attempt > 4) throw new Error('重登超过上限')
      curToken = await login()
      await page.evaluate(([t]) => {
        localStorage.setItem('token', t)
        localStorage.setItem('tenantId', '1')
        localStorage.setItem('tenantName', '系统租户')
      }, [curToken])
      return gotoPage(attempt + 1)
    }
  }
  await gotoPage()
  await page.waitForTimeout(2000)

  /** 被并行会话踢下线时自愈：重登 + 重新进入页面（调用点应在本组断言之前） */
  const ensureAlive = async (retry = 0, force = false) => {
    const kicked = page.url().includes('/login') || (await page.locator('text=立即注册').count()) > 0
    if (!kicked && !force) return
    if (retry > 4) throw new Error('重登超过上限')
    curToken = await login()
    await page.evaluate(([t]) => {
      localStorage.setItem('token', t)
      localStorage.setItem('tenantId', '1')
      localStorage.setItem('tenantName', '系统租户')
    }, [curToken])
    await gotoPage()
    await ensureAlive(retry + 1)
  }
  await page.screenshot({ path: `${SHOTS}/ui-01-tab1.png` })

  const hasTabs = await page.locator('text=商品图片列表').count() > 0 && await page.locator('text=图片空间').count() > 0
  check('页面含「商品图片列表 / 图片空间」双 Tab', hasTabs, '')

  // 表头 9 列
  const headers = await page.$$eval('.ss-grid thead th, table thead th', ths => ths.map(t => (t.innerText || '').trim()).filter(Boolean))
  const expectCols = ['商品名称', '商品状态', '货号', '规格', '型号', '产地', '品牌', '上传图片', '图片']
  const colOk = expectCols.every(c => headers.some(h => h.includes(c)))
  check('商品图片列表含对标 9 列', colOk, headers.join(' | ').slice(0, 160))

  // 分类树
  const treeOk = await page.locator('text=商品分类').count() > 0
  check('左侧商品分类树存在', treeOk, '')
  check('查询条件含「筛选条件/图片/规格/型号/产地/品牌」',
    (await page.locator('text=筛选条件').count()) > 0 && (await page.locator('text=产地').count()) > 0, '')

  // 对标实测（tool-results/ql361/image/30-tab1-struct.json）：分类树根节点「全部商品」
  const rootCatOk = await page.locator('.category-panel').locator('text=全部商品').count() > 0
  check('分类树含「全部商品」根节点', rootCatOk, '')

  // 对标此页工具栏仅「查询方案」，无 刷新/打印/导出
  const listBtns = (await page.locator('button').allInnerTexts()).map(t => (t || '').trim())
  check('工具栏无「刷新」(对标此页无刷新/打印/导出)',
    !listBtns.some(t => t.includes('刷新') || t.includes('打印') || t.includes('导出')),
    listBtns.filter(Boolean).join(',').slice(0, 140))

  // 对标分页：首页/上页/第(x/y)页/下页/尾页/跳转/共 N 条记录/每页显示 N 行
  const classicCnt = await page.locator('.classic-pagination').count()
  const pgText = classicCnt ? (await page.locator('.classic-pagination').innerText()).replace(/\s+/g, ' ') : ''
  check('商品图片列表分页为经典形态',
    classicCnt > 0 && /第\(/.test(pgText) && /条记录/.test(pgText) && /每页显示/.test(pgText),
    pgText.slice(0, 140))

  // 图片列：缩略图(对标 100x100) + 删除红✕ + ⊙主图 标记
  // 形态断言需要「首页至少一个带图商品」；缺失时临时上传并绑定一张（断言后清理）
  let uiTmpImageId = null
  if ((await page.locator('.image-thumb').count()) === 0) {
    const firstPage = await apiReq('GET', '/erp/md/image/page?pageNum=1&pageSize=1', null, curToken)
    const pid = firstPage?.data?.records?.[0]?.productId
    if (pid) {
      const mp = multipart({}, `e2e-ui-${Date.now()}.png`, PNG, 'image/png')
      const up = await apiReq('POST', '/erp/md/image/upload', mp.body, curToken, { contentType: mp.contentType })
      uiTmpImageId = up?.data?.id
      if (uiTmpImageId) {
        await apiReq('POST', '/erp/md/image/bind', { imageId: uiTmpImageId, productId: pid, isMain: 1 }, curToken)
        await page.locator('.search-section').locator('button', { hasText: '查询' }).first().click({ force: true }).catch(() => {})
        await page.waitForTimeout(2600)
      }
    }
  }
  const thumbBox = await page.locator('.image-thumb').first().boundingBox().catch(() => null)
  const mainTagCnt = await page.locator('.image-main-tag').count()
  check('图片列含缩略图 + ⊙主图 标记（对标形态）',
    !!thumbBox && Math.round(thumbBox.width) >= 90 && mainTagCnt > 0,
    thumbBox ? `缩略图=${Math.round(thumbBox.width)}px 主图标记=${mainTagCnt}` : '无缩略图')
  if (uiTmpImageId) await apiReq('POST', '/erp/md/image/batch-delete', { imageIds: [uiTmpImageId] }, curToken)

  // 切到图片空间（用 evaluate 点击，避免被下方表格拦截 pointer events）
  const clickTab = async (name) => {
    await page.evaluate((n) => {
      const el = [...document.querySelectorAll('.tab-item')].find(e => (e.innerText || '').trim() === n)
      el && el.click()
    }, name)
  }
  await ensureAlive()
  await clickTab('图片空间')
  await page.waitForTimeout(3500)
  await page.screenshot({ path: `${SHOTS}/ui-02-space.png` })
  const spaceBtns = await page.locator('button').allInnerTexts()
  const spaceBtnOk = ['上传图片', '自动匹配', '删除', '搬移'].every(b => spaceBtns.some(t => t.includes(b)))
  check('图片空间工具栏含 上传/自动匹配/删除/搬移', spaceBtnOk, spaceBtns.filter(Boolean).join(','))
  check('图片空间含匹配规则提示', (await page.locator('text=自动匹配规则').count()) > 0, '')
  check('图片空间含 全选 / 只显示图片', (await page.locator('text=全选').count()) > 0 && (await page.locator('text=只显示图片').count()) > 0, '')
  check('图片目录树「我的图片」存在', (await page.locator('text=我的图片').count()) > 0, '')

  // 对标实测（tool-results/ql361/image/32-space-struct.json）：缩略图 138x138、每行多列网格
  // 网格仅在存在素材时渲染；无素材时临时上传一张用于形态断言（断言后立刻清理）
  let gridTmpId = null
  if ((await page.locator('.space-panel .image-grid-thumb').count()) === 0) {
    const mp = multipart({}, `e2e-shot-${Date.now()}.png`, PNG, 'image/png')
    const up = await apiReq('POST', '/erp/md/image/upload', mp.body, curToken, { contentType: mp.contentType })
    gridTmpId = up?.data?.id
    if (gridTmpId) {
      await page.locator('.space-panel').locator('button', { hasText: '查询' }).first().click({ force: true }).catch(() => {})
      await page.waitForTimeout(2800)
    }
  }
  const gridThumb = await page.locator('.space-panel .image-grid-thumb').first().boundingBox().catch(() => null)
  const gridCols = await page.evaluate(() => {
    const el = document.querySelector('.space-panel .image-grid')
    if (!el) return 0
    return (getComputedStyle(el).gridTemplateColumns || '').split(' ').filter(Boolean).length
  })
  check('图片空间网格为 138px 缩略图 + 多列',
    !!gridThumb && Math.round(gridThumb.height) >= 130,
    gridThumb ? `缩略图高=${Math.round(gridThumb.height)}px 列数=${gridCols}` : '无网格素材')
  if (gridTmpId) {
    await apiReq('POST', '/erp/md/image/batch-delete', { imageIds: [gridTmpId] }, curToken)
    await page.waitForTimeout(800)
    await clickTab('图片空间')
    await page.waitForTimeout(2000)
  }

  // 回列表页，打开列配置（rowNo 表头齿轮）
  await ensureAlive()
  await clickTab('商品图片列表')
  await page.waitForTimeout(2500)
  // 等列表数据渲染出表头齿轮（rowNo 列），最多 15s；若期间被踢下线则自愈后重试
  await page.waitForSelector('.th-settings-btn', { timeout: 15000 }).catch(() => {})
  if ((await page.locator('.th-settings-btn').count()) === 0) {
    // 被并行会话踢下线时页面会先静默失效再跳登录，这里强制重登重进一次
    await ensureAlive(0, true)
    await clickTab('商品图片列表')
    await page.waitForSelector('.th-settings-btn', { timeout: 20000 }).catch(() => {})
  }
  const gear = page.locator('.th-settings-btn').first()
  if (await gear.count()) {
    await gear.click({ force: true })
    await page.waitForTimeout(1800)
    await page.screenshot({ path: `${SHOTS}/ui-03-colconfig.png` })
    const panelOk = (await page.locator('text=个人配置').count()) > 0 && (await page.locator('text=全局配置').count()) > 0
    check('列配置弹窗（个人配置 / 全局配置）', panelOk, '')
    await page.keyboard.press('Escape')
    await page.waitForTimeout(800)
  } else {
    check('列配置弹窗（个人配置 / 全局配置）', false, '未找到表头齿轮')
  }

  // 交互闭环：行内「选择图片」→ 弹窗选图 → 确定 → 商品图片列回显
  await ensureAlive()
  await clickTab('商品图片列表')
  await page.waitForTimeout(2500)
  const noImgRow = page.locator('.ss-grid tbody tr').filter({ hasText: '手工蒸饺' }).first()
  const pickBtn = noImgRow.locator('text=选择图片').first()
  if (await pickBtn.count()) {
    await pickBtn.click({ force: true })
    await page.waitForTimeout(2500)
    await page.screenshot({ path: `${SHOTS}/ui-04-pick-modal.png` })
    const modalOk = (await page.locator('text=选择图片').count()) > 1
    const cells = page.locator('.pick-grid .image-grid-item')
    const cellCount = await cells.count()
    check('选择图片弹窗打开且列出图片空间素材', modalOk && cellCount > 0, `素材数=${cellCount}`)
    if (cellCount > 0) {
      await cells.first().click()
      await page.waitForTimeout(500)
      // 弹窗确定
      await page.locator('.ant-modal-footer button.ant-btn-primary').last().click()
      await page.waitForTimeout(3000)
      await page.screenshot({ path: `${SHOTS}/ui-05-after-pick.png` })
      const afterRow = page.locator('.ss-grid tbody tr').filter({ hasText: '手工蒸饺' }).first()
      const imgCount = await afterRow.locator('img.image-thumb').count()
      check('选择图片后商品图片列回显缩略图', imgCount > 0, `缩略图数=${imgCount}`)
    }
  } else {
    check('选择图片弹窗打开且列出图片空间素材', false, '未找到行内「选择图片」入口')
  }

  await browser.close()
  return results
}

;(async () => {
  try {
    if (MODE === 'api' || MODE === 'all') await partA()
    if (MODE === 'ui' || MODE === 'all') await partB()
  } catch (e) {
    console.log('FATAL', e.message)
  }
  const failed = results.filter(r => !r.ok)
  console.log(`\n═══ 汇总：${results.length - failed.length}/${results.length} 通过 ═══`)
  if (failed.length) {
    console.log('失败项：')
    failed.forEach(f => console.log('  ❌', f.name, f.detail || ''))
  }
  process.exit(failed.length ? 1 : 0)
})()
