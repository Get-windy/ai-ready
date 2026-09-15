/*
 * 配送参数（配送 → 配送配置 → 配送参数，菜单 80750 `dms:config-params`）金标准端到端验证
 *   · API 验收：元数据注册表（分组/类型/默认值/范围/单位/生效方式）/ 参数中心分页多条件 / 行内元数据 /
 *               敏感键脱敏与「留空=不修改」/ 类型校验（数字越界·非法枚举·非法布尔·非法时间范围）/
 *               批量保存（事务）/ 恢复默认 / 变更历史 / 回滚
 *   · UI 验收：左参数分组树 + 表头 + 类型化控件（数字/开关/下拉/文本）+ 行内改动计数 + 批量保存二次确认 +
 *               恢复默认 / 变更历史抽屉 + 列配置 / 页面配置 + 统计
 *
 * 前置（每次跑之前整文件执行一次）：
 *   python - <<'EOF'
 *   import psycopg2; c=psycopg2.connect("host=localhost port=5432 dbname=devdb user=devuser password=devuser123")
 *   c.autocommit=True; c.cursor().execute(open("tools/e2e-dms-config-params-user.sql",encoding="utf-8").read()); c.close()
 *   EOF
 *   验收后端实例：java -jar <fat jar> --server.port=5665
 *
 * 用法：node tools/e2e-dms-config-params.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5665)
const SHOTS = 'I:/AI-Ready/tool-results/dms-config-params'

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

/** 用例使用的已登记键（E2E 会临时改值，结束前用 reset 恢复默认） */
const K_CONCURRENT = 'dms.dispatch.max.concurrent'      // NUMBER 0..100，默认 5
const K_STRATEGY = 'dms.dispatch.strategy'              // ENUM，默认 NEAREST
const K_ONLINE_ONLY = 'dms.dispatch.require.online'     // BOOLEAN，默认 true
const K_COLLECT_HOURS = 'dms.tracking.collect.hours'    // TIME_RANGE，默认 00:00-23:59
const K_SECRET = 'map.amap.api-key'                     // TEXT + secret

function rawReq(method, reqPath, body, token) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null || typeof body === 'string' ? body : JSON.stringify(body)
    const headers = { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: encodeURI('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => resolve({ status: res.statusCode, buf: Buffer.concat(chunks), json: tryJson(Buffer.concat(chunks)) }))
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

function tryJson(buf) { try { return JSON.parse(buf.toString('utf8')) } catch (e) { return null } }

let TOKEN = null
async function api(method, reqPath, body) {
  let r = await rawReq(method, reqPath, body, TOKEN)
  if (r.status === 401 || (r.json && Number(r.json.code) === 401)) {
    console.log('  [token 失效，自动重登]')
    await login()
    r = await rawReq(method, reqPath, body, TOKEN)
  }
  return r
}
const data = (res) => (res.json ? res.json.data : null)

const E2E_USER = process.env.E2E_USER || 'e2e_config'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  let res = await rawReq('POST', '/auth/login', {
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户', captcha: code, captchaKey: cap.json.data.uuid,
  })
  let token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token && E2E_USER !== 'admin') {
    const cap2 = await rawReq('GET', '/auth/captcha')
    const code2 = [...Buffer.from(cap2.json.data.img.split(',')[1], 'base64').toString('utf8')
      .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
    res = await rawReq('POST', '/auth/login', {
      username: 'admin', password: 'admin123', tenantName: '系统租户', captcha: code2, captchaKey: cap2.json.data.uuid,
    })
    token = res.json?.data?.token || res.json?.data?.accessToken
  }
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 300))
  TOKEN = token
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`)
}

async function valueOf(key) {
  const r = await api('GET', `/dms/config/page?pageNum=1&pageSize=200&keyword=${key}`)
  const row = (data(r)?.records || []).find(x => x.configKey === key)
  return row ? String(row.configValue ?? '') : null
}

// ══════════════ 一、接口验收 ══════════════
async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  // 1) 元数据注册表
  const meta = await api('GET', '/dms/config/meta')
  const groups = data(meta)?.groups || []
  const items = data(meta)?.items || []
  check('元数据返回分组树（含计数）', meta.json?.code === 200 && groups.length >= 6
    && groups.every(g => g.key && g.text && g.count != null), JSON.stringify(groups.slice(0, 3)))
  check('元数据登记项 ≥ 30 且含类型', items.length >= 30 && items.every(i => i.valueType), items.length)
  const metaConcurrent = items.find(i => i.configKey === K_CONCURRENT) || {}
  check('NUMBER 元数据含范围与单位', metaConcurrent.valueType === 'NUMBER'
    && Number(metaConcurrent.min) === 0 && Number(metaConcurrent.max) === 100 && metaConcurrent.unit === '单',
    JSON.stringify(metaConcurrent).slice(0, 160))
  const metaStrategy = items.find(i => i.configKey === K_STRATEGY) || {}
  check('ENUM 元数据含枚举项', metaStrategy.valueType === 'ENUM' && (metaStrategy.options || []).length === 4,
    (metaStrategy.options || []).map(o => o.value).join(','))
  check('元数据含生效方式（热生效）', items.every(i => !!i.effect) && items.some(i => i.effect === 'HOT'), '')

  // 2) 参数中心分页（多条件）
  const all = await api('GET', '/dms/config/page?pageNum=1&pageSize=200')
  check('分页返回全部参数（≥30）', Number(data(all)?.total) >= 30, data(all)?.total)
  const firstRow = (data(all)?.records || [])[0] || {}
  check('行内含元数据（参数名/类型/默认值/生效方式）',
    !!firstRow.name && !!firstRow.valueType && firstRow.effect != null, JSON.stringify(firstRow).slice(0, 160))
  const pGroup = await api('GET', '/dms/config/page?pageNum=1&pageSize=200&group=DISPATCH')
  const groupRows = data(pGroup)?.records || []
  // 断言与「元数据注册表」对齐（派单策略键数随注册表增长，勿硬编码）
  const dispatchMetaCount = items.filter(i => i.group === 'DISPATCH').length
  check(`按分组过滤（派单策略 ${dispatchMetaCount} 项）`, Number(data(pGroup)?.total) === groupRows.length
    && Number(data(pGroup)?.total) >= 9 && Number(data(pGroup)?.total) <= dispatchMetaCount
    && groupRows.every(r => r.group === 'DISPATCH'), data(pGroup)?.total)
  const pBool = await api('GET', '/dms/config/page?pageNum=1&pageSize=200&valueType=BOOLEAN')
  check('按参数类型过滤（BOOLEAN）',
    (data(pBool)?.records || []).every(r => r.valueType === 'BOOLEAN') && Number(data(pBool)?.total) >= 4,
    data(pBool)?.total)
  const pKw = await api('GET', '/dms/config/page?pageNum=1&pageSize=200&keyword=超速')
  check('按关键词检索（参数名/说明）', Number(data(pKw)?.total) >= 1
    && (data(pKw)?.records || []).some(r => r.configKey.includes('speed')), data(pKw)?.total)
  const pCfg = await api('GET', '/dms/config/page?pageNum=1&pageSize=200&configuredOnly=true')
  check('仅看已配置过滤生效',
    (data(pCfg)?.records || []).every(r => String(r.configValue || '').length > 0), data(pCfg)?.total)
  const pPage = await api('GET', '/dms/config/page?pageNum=2&pageSize=5')
  check('分页生效（第 2 页 5 条）', (data(pPage)?.records || []).length === 5, (data(pPage)?.records || []).length)
  const pEditable = await api('GET', '/dms/config/page?pageNum=1&pageSize=200&editable=true')
  check('按是否可改过滤（全部可改命中 ≥30）', Number(data(pEditable)?.total) >= 30
    && (data(pEditable)?.records || []).every(r => r.editable === true), data(pEditable)?.total)
  const pOverride = await api('GET', '/dms/config/page?pageNum=1&pageSize=200&tenantOverride=true')
  check('按作用域过滤（仅租户覆盖行）', Number(data(pOverride)?.total) >= 1
    && (data(pOverride)?.records || []).every(r => r.tenantOverride === true), data(pOverride)?.total)
  const pTime = await api('GET', '/dms/config/page?pageNum=1&pageSize=200&updateTimeStart=2000-01-01&updateTimeEnd=2999-12-31')
  check('按更新时间区间过滤（命中）', Number(data(pTime)?.total) >= 30, data(pTime)?.total)
  const pTime2 = await api('GET', '/dms/config/page?pageNum=1&pageSize=200&updateTimeStart=2999-01-01')
  check('按更新时间区间过滤（未来日期命中 0）', Number(data(pTime2)?.total) === 0, data(pTime2)?.total)

  // 3) 敏感键脱敏
  const secretRow = (data(all)?.records || []).find(r => r.configKey === K_SECRET) || {}
  check('敏感键标记 secret', secretRow.secret === true, secretRow.secret)
  check('敏感键值脱敏（非明文）',
    String(secretRow.configValue || '') !== 'E2E-TEST-KEY-123456' && String(secretRow.configValue || '').includes('*'),
    secretRow.configValue)

  // 4) 类型校验（后端前置）
  const badNumber = await api('PUT', '/dms/config/batch', [{ configKey: K_CONCURRENT, configValue: '999' }])
  check('数字越界被拒', badNumber.json?.code !== 200, badNumber.json?.message)
  const badNumber2 = await api('PUT', '/dms/config/batch', [{ configKey: K_CONCURRENT, configValue: 'abc' }])
  check('非数字被拒', badNumber2.json?.code !== 200, badNumber2.json?.message)
  const badEnum = await api('PUT', '/dms/config/batch', [{ configKey: K_STRATEGY, configValue: 'XXX' }])
  check('非法枚举被拒', badEnum.json?.code !== 200, badEnum.json?.message)
  const badBool = await api('PUT', '/dms/config/batch', [{ configKey: K_ONLINE_ONLY, configValue: 'maybe' }])
  check('非法布尔被拒', badBool.json?.code !== 200, badBool.json?.message)
  const badRange = await api('PUT', '/dms/config/batch', [{ configKey: K_COLLECT_HOURS, configValue: '25:00-26:00' }])
  check('非法时间范围被拒', badRange.json?.code !== 200, badRange.json?.message)
  const emptyBatch = await api('PUT', '/dms/config/batch', [])
  check('空批量被拒', emptyBatch.json?.code !== 200, emptyBatch.json?.message)

  // 5) 批量保存（事务）+ 生效
  const batch = await api('PUT', '/dms/config/batch', [
    { configKey: K_CONCURRENT, configValue: '8' },
    { configKey: K_STRATEGY, configValue: 'BALANCED' },
    { configKey: K_ONLINE_ONLY, configValue: 'false' },
  ])
  check('批量保存 3 项成功', batch.json?.code === 200 && Number(data(batch)) === 3, JSON.stringify(data(batch)))
  check('批量保存后值已生效（并发 8 / 策略 BALANCED / 仅空闲 false）',
    (await valueOf(K_CONCURRENT)) === '8' && (await valueOf(K_STRATEGY)) === 'BALANCED'
    && (await valueOf(K_ONLINE_ONLY)) === 'false',
    `${await valueOf(K_CONCURRENT)}/${await valueOf(K_STRATEGY)}/${await valueOf(K_ONLINE_ONLY)}`)
  const partial = await api('PUT', '/dms/config/batch', [
    { configKey: K_CONCURRENT, configValue: '7' },
    { configKey: K_STRATEGY, configValue: 'BOGUS' },
  ])
  check('批量中任一项非法则整体拒绝（事务）',
    partial.json?.code !== 200 && (await valueOf(K_CONCURRENT)) === '8', partial.json?.message)

  // 6) 敏感键「留空=不修改」
  const secretBatch = await api('PUT', '/dms/config/batch', [{ configKey: K_SECRET, configValue: '' }])
  check('敏感键留空批量保存=不修改', secretBatch.json?.code === 200, secretBatch.json?.message)
  const secretList = await api('GET', `/dms/config/page?pageNum=1&pageSize=200&keyword=${K_SECRET}`)
  const secretRow2 = (data(secretList)?.records || []).find(r => r.configKey === K_SECRET) || {}
  check('敏感键留空保存后仍为「已配置」（未被空值覆盖）',
    secretRow2.configured === true && String(secretRow2.configValue || '').includes('*'),
    `configured=${secretRow2.configured}/value=${secretRow2.configValue}`)

  // 7) 恢复默认
  const reset = await api('POST', `/dms/config/${K_CONCURRENT}/reset`)
  check('恢复默认成功', reset.json?.code === 200, reset.json?.message)
  check('恢复后值 = 元数据默认值（5，且落在租户覆盖行）',
    (await valueOf(K_CONCURRENT)) === '5', await valueOf(K_CONCURRENT))
  const resetList = await api('GET', '/dms/config/page?pageNum=1&pageSize=200&keyword=' + K_CONCURRENT)
  const resetRow = (data(resetList)?.records || []).find(r => r.configKey === K_CONCURRENT) || {}
  check('恢复默认写入租户覆盖行（回归：曾误写全局行）', resetRow.tenantOverride === true,
    `override=${resetRow.tenantOverride}/value=${resetRow.configValue}`)
  const resetUnknown = await api('POST', '/dms/config/not.registered.key/reset')
  check('未登记元数据的键不可恢复默认', resetUnknown.json?.code !== 200, resetUnknown.json?.message)

  // 8) 变更历史 + 回滚
  // ⚠️ 历史/回滚不传 tenantId → 服务端按当前登录租户解析（回归项）
  const hist = await api('GET', `/dms/config/${K_STRATEGY}/history`)
  const histRows = data(hist) || []
  check('变更历史可查（含批量保存记录）', hist.json?.code === 200 && histRows.length >= 1,
    `${histRows.length} 条 / ${histRows[0]?.changeType}`)
  const rollbackTarget = histRows.find(r => r.changeType === 'UPDATE') || histRows[0]
  if (rollbackTarget) {
    const rb = await api('POST', `/dms/config/${K_STRATEGY}/rollback?historyId=${rollbackTarget.id}`)
    check('回滚到历史版本成功', rb.json?.code === 200, rb.json?.message)
    check('回滚后值与历史一致', (await valueOf(K_STRATEGY)) === String(rollbackTarget.oldValue ?? ''),
      `${await valueOf(K_STRATEGY)} ← ${rollbackTarget.oldValue}`)
  } else {
    check('回滚到历史版本成功', false, '无可用历史记录')
  }

  // 9) 参数集导出 / 导入（JSON）
  const exp = await api('GET', '/dms/config/export')
  const expData = data(exp) || {}
  check('导出参数集（结构 + 计数一致）',
    exp.json?.code === 200 && expData.schema === 'dms-config-export/v1'
    && Array.isArray(expData.items) && expData.items.length >= 30 && expData.count === expData.items.length,
    `schema=${expData.schema}/count=${expData.count}`)
  const expSecret = (expData.items || []).find(i => i.configKey === K_SECRET) || {}
  check('导出物不含敏感键明文', expSecret.secret === true && String(expSecret.configValue || '') === '',
    `secret=${expSecret.secret}/value=${expSecret.configValue}`)
  const expGroup = await api('GET', '/dms/config/export?group=DISPATCH')
  const expGroupItems = data(expGroup)?.items || []
  check('导出支持按分组过滤', expGroupItems.length === Number(data(expGroup)?.count)
    && expGroupItems.length >= 9 && expGroupItems.every(i => i.group === 'DISPATCH'), data(expGroup)?.count)
  const expKw = await api('GET', '/dms/config/export?keyword=超速')
  check('导出支持按关键词过滤', (data(expKw)?.items || []).length >= 1
    && (data(expKw)?.items || []).every(i => i.configKey.includes('speed')), data(expKw)?.count)

  // 9.1 导入预览（PREVIEW 不落库 / 逐项失败原因）
  const prev = await api('POST', '/dms/config/import',
    { mode: 'PREVIEW', items: [{ configKey: K_CONCURRENT, configValue: '9' }] })
  const prevRow = (data(prev)?.preview || [])[0] || {}
  check('导入预览返回差异（changed=1，含前后值）',
    prev.json?.code === 200 && Number(data(prev)?.changed) === 1
    && prevRow.oldValue === '5' && prevRow.newValue === '9' && prevRow.action === 'UPDATE',
    `${prevRow.oldValue}→${prevRow.newValue}/${prevRow.action}`)
  check('导入预览不落库（值仍为 5）', (await valueOf(K_CONCURRENT)) === '5', await valueOf(K_CONCURRENT))
  const prevBad = await api('POST', '/dms/config/import',
    { mode: 'PREVIEW', items: [{ configKey: K_CONCURRENT, configValue: '999' }] })
  check('导入非法值逐项反馈原因（不整体抛错）',
    prevBad.json?.code === 200 && (data(prevBad)?.failed || []).length === 1
    && String(data(prevBad).failed[0].reason).includes('不能大于'), JSON.stringify(data(prevBad)?.failed))
  const prevUnknown = await api('POST', '/dms/config/import',
    { mode: 'PREVIEW', items: [{ configKey: 'not.registered.key', configValue: '1' }] })
  check('导入未登记键被拒（仅支持已有参数）', (data(prevUnknown)?.failed || []).length === 1
    && String(data(prevUnknown).failed[0].reason).includes('不存在'), JSON.stringify(data(prevUnknown)?.failed))
  const prevSecret = await api('POST', '/dms/config/import', {
    mode: 'PREVIEW',
    items: [{ configKey: K_SECRET, configValue: '' }, { configKey: K_SECRET, configValue: '****123456' }],
  })
  check('导入敏感键留空/掩码=跳过（不写掩码）',
    Number(data(prevSecret)?.skipped) === 2 && Number(data(prevSecret)?.changed) === 0,
    JSON.stringify(data(prevSecret)).slice(0, 160))

  // 9.2 导入应用（APPLY 落库 + 审计 + 整体拒绝 + 不覆盖）
  // ⚠️ 幂等回灌放在其它 APPLY 用例之前：此时状态与导出时刻一致
  const reImport = await api('POST', '/dms/config/import', { mode: 'APPLY', items: expData.items || [] })
  check('导出物可直接回灌（幂等：零变更）',
    reImport.json?.code === 200 && Number(data(reImport)?.changed) === 0,
    JSON.stringify(data(reImport)).slice(0, 160))
  const applyRes = await api('POST', '/dms/config/import',
    { mode: 'APPLY', items: [{ configKey: K_STRATEGY, configValue: 'AREA' }] })
  check('导入落库成功（changed=1）', applyRes.json?.code === 200 && Number(data(applyRes)?.changed) === 1,
    JSON.stringify(data(applyRes)).slice(0, 160))
  check('导入后值生效（策略 AREA）', (await valueOf(K_STRATEGY)) === 'AREA', await valueOf(K_STRATEGY))
  const impHist = await api('GET', `/dms/config/${K_STRATEGY}/history`)
  check('导入写入 IMPORT 审计', (data(impHist) || []).some(h => h.changeType === 'IMPORT'),
    (data(impHist) || []).map(h => h.changeType).join(','))
  const applyPartial = await api('POST', '/dms/config/import', {
    mode: 'APPLY',
    items: [{ configKey: K_CONCURRENT, configValue: '12' }, { configKey: K_STRATEGY, configValue: 'BOGUS' }],
  })
  check('导入含非法项整体拒绝（合法项也未落库）',
    applyPartial.json?.code === 200 && (data(applyPartial)?.failed || []).length === 1
    && (await valueOf(K_CONCURRENT)) === '5' && (await valueOf(K_STRATEGY)) === 'AREA',
    JSON.stringify(data(applyPartial)?.failed))
  const applyNoOverwrite = await api('POST', '/dms/config/import', {
    mode: 'APPLY', overwrite: false, items: [{ configKey: K_STRATEGY, configValue: 'SCORE' }],
  })
  check('overwrite=false 时已覆盖项跳过',
    Number(data(applyNoOverwrite)?.skipped) === 1 && (await valueOf(K_STRATEGY)) === 'AREA',
    JSON.stringify(data(applyNoOverwrite)).slice(0, 160))
  const impEmpty = await api('POST', '/dms/config/import', { mode: 'APPLY', items: [] })
  check('空导入被拒', impEmpty.json?.code !== 200, impEmpty.json?.message)

  // 10) 还原受影响的参数（避免影响其它模块的运行时口径）
  await api('PUT', '/dms/config/batch', [
    { configKey: K_CONCURRENT, configValue: '5' },
    { configKey: K_STRATEGY, configValue: 'NEAREST' },
    { configKey: K_ONLINE_ONLY, configValue: 'true' },
  ])
  check('测试后已还原相关参数', (await valueOf(K_CONCURRENT)) === '5' && (await valueOf(K_STRATEGY)) === 'NEAREST',
    `${await valueOf(K_CONCURRENT)}/${await valueOf(K_STRATEGY)}`)
}

// ══════════════ 二、UI 验收 ══════════════
async function uiSuite() {
  console.log('\n═══ 二、UI 验收 ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
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
      await page.waitForSelector('.ss-grid th', { timeout: 60000 }).catch(() => {})
      await page.waitForTimeout(1200)
      const kicked = page.url().includes('/login') || (await page.locator('input[type=password]').count()) > 0
      if (!kicked) return
      console.log('  [页面被互踢，自愈重登]')
    }
  }

  const bodyText = () => page.locator('body').innerText().then(t => t.replace(/\s+/g, ''))

  await openPage(`${FE}/dms/config-params`)
  const txt0 = await bodyText()
  check('页面可打开（无 404/白屏）', !txt0.includes('页面不存在') && txt0.length > 50, page.url())
  check('左分组树渲染（参数分组 / 派单策略 / 配送跟踪）',
    txt0.includes('参数分组') && txt0.includes('派单策略') && txt0.includes('配送跟踪'), txt0.slice(0, 120))
  const headers = await page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  for (const h of ['参数名', '参数键', '当前值', '默认值', '生效方式', '说明']) {
    check(`表头含「${h}」`, headers.some(x => x.includes(h)), headers.join('/'))
  }
  check('查询区含类型/生效方式/仅看已配置', txt0.includes('参数类型') && txt0.includes('生效方式') && txt0.includes('仅看已配置'), '')
  check('查询区含作用域/是否可改/更新时间',
    txt0.includes('作用域') && txt0.includes('是否可改') && txt0.includes('更新时间'), '')
  check('工具栏含保存修改/本组恢复默认/刷新',
    txt0.includes('保存修改') && txt0.includes('本组恢复默认') && txt0.includes('刷新'), txt0.slice(0, 140))
  check('工具栏含导入参数/导出参数（参数集迁移入口）',
    txt0.includes('导入参数') && txt0.includes('导出参数'), txt0.slice(0, 200))
  check('列表出现种子参数（派单策略/单人在途上限）',
    txt0.includes('派单策略') && txt0.includes('单人在途上限'), '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-list.png'), fullPage: true })

  // 类型化控件：NUMBER → input-number；BOOLEAN → switch；ENUM → select
  const numberInputs = await page.locator('.ss-grid input.ant-input-number-input').count()
  check('NUMBER 参数渲染为数字控件', numberInputs > 0, `count=${numberInputs}`)
  const switches = await page.locator('.ss-grid .ant-switch').count()
  check('BOOLEAN 参数渲染为开关', switches > 0, `count=${switches}`)
  const selects = await page.locator('.ss-grid .ant-select-selection-item').count()
  check('ENUM 参数渲染为下拉', selects > 0, `count=${selects}`)

  // 行内改值 → 出现未保存计数 → 批量保存
  const firstNumber = page.locator('.ss-grid input.ant-input-number-input').first()
  await firstNumber.fill('6')
  await firstNumber.blur()
  await page.waitForTimeout(800)
  check('行内修改后出现「未保存」提示与按钮计数', (await bodyText()).includes('未保存'), '')
  await page.locator('button:has-text("保存修改")').first().click()
  await page.waitForTimeout(1200)
  const confirm = page.locator('.ant-modal-confirm:visible').last()
  check('批量保存二次确认（含 diff 预览）',
    (await confirm.innerText()).includes('确认保存参数修改') && (await confirm.innerText()).includes('→'), '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-save-confirm.png'), fullPage: true })
  await confirm.locator('.ant-modal-confirm-btns .ant-btn-primary').click()
  await page.waitForTimeout(3000)
  check('批量保存成功提示', (await bodyText()).includes('已保存'), '')

  // 恢复默认（行内）
  const resetBtn = page.locator('.ss-grid tbody tr button:has-text("恢复默认")').first()
  if (await resetBtn.count()) {
    await resetBtn.click()
    await page.waitForTimeout(1200)
    const resetConfirm = page.locator('.ant-modal-confirm:visible').last()
    check('恢复默认二次确认', (await resetConfirm.innerText()).includes('恢复默认值'), '')
    await resetConfirm.locator('.ant-modal-confirm-btns .ant-btn-primary').click()
    await page.waitForTimeout(2500)
    check('恢复默认后提示成功', (await bodyText()).includes('已恢复默认'), '')
  } else {
    check('恢复默认二次确认', false, '未找到恢复默认按钮')
  }

  // 变更历史抽屉
  await page.locator('.ss-grid tbody tr button:has-text("变更历史")').first().click()
  await page.waitForTimeout(2500)
  const drawer = page.locator('.ant-drawer-content:visible').last()
  const drawerText = (await drawer.innerText()).replace(/\s+/g, '')
  check('变更历史抽屉打开（含变更类型/操作人/回滚入口）',
    drawerText.includes('变更历史') && drawerText.includes('变更类型') && drawerText.includes('回滚到此版本'),
    drawerText.slice(0, 160))
  await page.screenshot({ path: path.join(SHOTS, 'ui-history.png'), fullPage: true })
  await page.locator('.ant-drawer-close').first().click()
  await page.waitForTimeout(1000)

  // 列配置 + 页面配置
  const gearCount = await page.evaluate(() => document.querySelectorAll('.ss-grid .th-settings-btn').length)
  check('数据表带表头齿轮（列配置入口）', gearCount > 0, `count=${gearCount}`)
  await page.locator('.ss-grid .th-settings-btn').first().click()
  await page.waitForTimeout(1500)
  const colPanel = page.locator('.ant-modal-content:visible').last()
  const colText = (await colPanel.innerText()).replace(/\s+/g, '')
  check('列配置弹窗含「个人配置/全局配置」双 Tab', colText.includes('个人配置') && colText.includes('全局配置'), colText.slice(0, 60))
  await page.screenshot({ path: path.join(SHOTS, 'ui-colconfig.png'), fullPage: true })
  await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
  await page.waitForTimeout(800)

  await page.locator('button[title="页面配置"]').first().click()
  await page.waitForTimeout(1200)
  const pageCfg = page.locator('.ant-modal-content:visible').last()
  const cfgText = (await pageCfg.innerText()).replace(/\s+/g, '')
  check('页面配置弹窗含「查询条件/功能按钮」双 Tab', cfgText.includes('查询条件') && cfgText.includes('功能按钮'), cfgText.slice(0, 60))
  await pageCfg.locator('.ant-tabs-tab:has-text("功能按钮")').click()
  await page.waitForTimeout(700)
  const btnText = (await pageCfg.innerText()).replace(/\s+/g, '')
  check('页面配置含功能按钮项（保存修改/本组恢复默认）',
    btnText.includes('保存修改') && btnText.includes('本组恢复默认'), btnText.slice(0, 140))
  check('页面配置含导入/导出按钮项',
    btnText.includes('导入参数') && btnText.includes('导出参数'), btnText.slice(0, 200))
  await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
  await page.waitForTimeout(800)

  // 导出参数：弹窗 → 导出 JSON（触发浏览器下载）
  await page.locator('button:has-text("导出参数")').first().click()
  await page.waitForTimeout(1200)
  const exportModal = page.locator('.ant-modal-content:visible').last()
  const exportText = (await exportModal.innerText()).replace(/\s+/g, '')
  check('导出弹窗（范围选择 + 敏感键说明）',
    exportText.includes('当前筛选结果') && exportText.includes('敏感键'), exportText.slice(0, 120))
  const [download] = await Promise.all([
    page.waitForEvent('download', { timeout: 20000 }).catch(() => null),
    exportModal.locator('.ant-modal-footer .ant-btn-primary').click(),
  ])
  check('导出触发 JSON 下载', !!download && /dms-config-params-.*\.json/.test(download.suggestedFilename()),
    download ? download.suggestedFilename() : 'no download')
  await page.waitForTimeout(1500)

  // 导入参数：粘贴 JSON → 预览差异 → 确认导入
  await page.locator('button:has-text("导入参数")').first().click()
  await page.waitForTimeout(1200)
  const importModal = page.locator('.ant-modal-content:visible').last()
  await importModal.locator('textarea').first().fill(
    JSON.stringify({ items: [{ configKey: 'dms.dispatch.strategy', configValue: 'SCORE' }] }))
  await importModal.locator('button:has-text("预览差异")').click()
  await page.waitForTimeout(2500)
  const previewText = (await importModal.innerText()).replace(/\s+/g, '')
  check('导入预览展示差异（参数键/导入值/动作）',
    previewText.includes('dms.dispatch.strategy') && previewText.includes('SCORE')
    && (previewText.includes('更新覆盖') || previewText.includes('新增覆盖')), previewText.slice(0, 200))
  await page.screenshot({ path: path.join(SHOTS, 'ui-import-preview.png'), fullPage: true })
  await importModal.locator('.ant-modal-footer .ant-btn-primary').click()
  await page.waitForTimeout(1500)
  check('导入确认后提示成功并刷新', (await bodyText()).includes('导入完成'), '')
  // 还原为默认策略，避免影响其它模块
  await api('PUT', '/dms/config/batch', [{ configKey: 'dms.dispatch.strategy', configValue: 'NEAREST' }])
  check('UI 导入后已还原策略参数', (await valueOf('dms.dispatch.strategy')) === 'NEAREST', await valueOf('dms.dispatch.strategy'))

  // 分组过滤：切到「派单策略」
  await page.locator('.category-panel .ant-tree-node-content-wrapper', { hasText: '派单策略' }).first().click()
  await page.waitForTimeout(2500)
  const groupTxt = await bodyText()
  check('左分组过滤生效（仅派单策略参数）',
    groupTxt.includes('单人在途上限') && !groupTxt.includes('超速阈值'), groupTxt.slice(0, 120))
  await page.screenshot({ path: path.join(SHOTS, 'ui-group.png'), fullPage: true })

  await browser.close()
}

// ══════════════ 主流程 ══════════════
;(async () => {
  try {
    await login()
    console.log('登录成功（' + E2E_USER + '）')
    const probe = await api('GET', '/dms/config/meta')
    if ((data(probe)?.items || []).length < 30) {
      throw new Error('元数据/种子缺失：请确认后端已含 DmsConfigMetaRegistry，并执行 tools/e2e-dms-config-params-user.sql')
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
