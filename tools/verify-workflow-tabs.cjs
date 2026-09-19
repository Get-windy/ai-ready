/*
 * 验证：菜单 803「我的待办」/ 804「我的已办」是否真的落到不同视图。
 *
 * 背景：两个菜单的 component 归一化后是同一个 key（views/workflow/task-management），
 * 落到同一个组件实例，组件靠路由（name=menu_code / path）决定初始 Tab。
 * 本脚本用真实浏览器访问两个 URL，抓取：
 *   ① document.title —— 来自路由 meta.title（即菜单名）
 *   ② 页内标题文本   —— pageTitle 计算属性
 *   ③ 激活的 Tab     —— 待办任务 / 已办任务
 * 三者交叉比对，判断是否真的区分开。
 *
 * 用法：node tools/verify-workflow-tabs.cjs
 *      E2E_USER=e2e_hr E2E_PWD=admin123 node tools/verify-workflow-tabs.cjs
 */
const http = require('http')

const PORT = Number(process.env.PORT || 5655)
const FE = process.env.FE_URL || 'http://localhost:5656'
const USER = process.env.E2E_USER || 'e2e_hr'
const PWD = process.env.E2E_PWD || 'admin123'
const TENANT = process.env.E2E_TENANT || '系统租户'

function req(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      host: 'localhost', port: PORT, path, method,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
      },
    }, res => {
      let buf = ''
      res.on('data', c => { buf += c })
      res.on('end', () => {
        let json = null
        try { json = JSON.parse(buf) } catch { /* 非 JSON */ }
        resolve({ status: res.statusCode, json, raw: buf })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function login() {
  const cap = await req('GET', '/api/auth/captcha')
  const b64 = cap.json?.data?.img?.split(',')[1] || ''
  const code = [...Buffer.from(b64, 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/api/auth/login', {
    username: USER, password: PWD, tenantName: TENANT,
    captcha: code, captchaKey: cap.json?.data?.uuid,
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 300))
  return token
}

/** 抓取一个路径的渲染结果 */
async function probe(page, path) {
  await page.goto(FE + path, { waitUntil: 'domcontentloaded' })
  // 等动态路由 + 菜单渲染（菜单接口 + 路由注册 + 组件挂载）
  await page.waitForTimeout(4000)
  return await page.evaluate(() => {
    const txt = (sel) => {
      const el = document.querySelector(sel)
      return el ? (el.textContent || '').trim().slice(0, 60) : null
    }
    // ⚠️ CategoryListLayout 的 Tab 是**自研 div**（.tab-item / .tab-item.active），
    // 不是 ant-design 的 a-tabs —— 用 .ant-tabs-tab 会抓空。
    const tabs = [...document.querySelectorAll('.tab-item')].map(el => ({
      text: (el.textContent || '').trim(),
      active: el.classList.contains('active'),
    }))
    return {
      url: location.pathname,
      docTitle: document.title,
      pageTitle: txt('.page-title') || txt('h2'),
      tabs,
      bodyHead: (document.body.innerText || '').replace(/\s+/g, ' ').slice(0, 120),
    }
  })
}

;(async () => {
  console.log('═'.repeat(78))
  console.log('验证：我的待办(/workflow/task) vs 我的已办(/workflow/done)')
  console.log('═'.repeat(78))

  const token = await login()
  console.log('登录成功，账号 =', USER)

  // playwright 装在 frontend 的 node_modules 下（tools 目录没有 node_modules），
  // 故与 e2e-hr.cjs 一致走绝对路径引入。
  const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
  const browser = await chromium.launch()
  const page = await browser.newPage()
  await page.addInitScript(([tok]) => {
    localStorage.setItem('token', tok)
    localStorage.setItem('tenantId', '1')
    localStorage.setItem('tenantName', '系统租户')
  }, [token])
  // SSE 与通知轮询是外壳级噪声，屏蔽掉避免干扰
  await page.route('**/api/sse/notifications*', r => r.fulfill({ status: 200, contentType: 'text/event-stream', body: '' }))
  await page.route('**/api/notification/**', r => r.fulfill({ status: 200, contentType: 'application/json', body: '{"code":200,"data":[]}' }))

  const a = await probe(page, '/workflow/task')
  const b = await probe(page, '/workflow/done')
  // 同源缺陷的另外两处：801「流程定义」下有 8011~8015、907「招聘管理」下有 9071~9073，
  // 都是 menuType=2/3 的按钮项，此前同样会把自己的 meta.title 盖到父级上。
  const c = await probe(page, '/workflow/definition')
  const d = await probe(page, '/hr/recruitment')
  await browser.close()

  const show = (label, r) => {
    console.log('\n【' + label + '】')
    console.log('  URL        :', r.url)
    console.log('  浏览器标题 :', r.docTitle)
    console.log('  页内标题   :', r.pageTitle)
    console.log('  激活的 Tab :', (r.tabs.find(t => t.active) || {}).text || '(未识别)')
    console.log('  全部 Tab   :', r.tabs.map(t => (t.active ? '[' + t.text + ']' : t.text)).join(' / '))
  }
  show('我的待办 · /workflow/task', a)
  show('我的已办 · /workflow/done', b)
  show('流程定义 · /workflow/definition（应显示「流程定义」而非「新增流程」等按钮名）', c)
  show('招聘管理 · /hr/recruitment（应显示「招聘管理」而非「新增」等按钮名）', d)

  console.log('\n--- 标题污染检查（修复前会显示按钮名）---')
  const badTitles = [
    ['/workflow/task', a.docTitle, '我的待办'],
    ['/workflow/done', b.docTitle, '我的已办'],
    ['/workflow/definition', c.docTitle, '流程定义'],
    ['/hr/recruitment', d.docTitle, '招聘管理'],
  ].filter(([, actual, expect]) => !String(actual).includes(expect))
  if (badTitles.length) {
    console.log('  ❌ 仍有标题被按钮项污染：')
    badTitles.forEach(([p, actual, expect]) => console.log(`     ${p} → 「${actual}」，期望含「${expect}」`))
  } else {
    console.log('  ✅ 四处标题均正确（按钮类型已不参与路由生成）')
  }

  console.log('\n' + '═'.repeat(78))
  const tabA = (a.tabs.find(t => t.active) || {}).text || ''
  const tabB = (b.tabs.find(t => t.active) || {}).text || ''
  const ok = tabA.includes('待办') && tabB.includes('已办') && !badTitles.length
  console.log(ok
    ? '✅ 结论：两个菜单落到了不同视图（待办 / 已办 各自激活）'
    : `❌ 结论：两个菜单未区分 —— 激活 Tab 分别是「${tabA}」「${tabB}」`)
  console.log('═'.repeat(78))
  process.exit(ok ? 0 : 1)
})().catch(e => { console.error('脚本异常:', e.message); process.exit(2) })
