// 销售模块「页面跳转目标是否真的存在路由」验证脚本（只读，不改任何业务数据）。
//
// 背景：前端 `dynamicRoutes.ts` 的路由表由「后端菜单树 + getRequiredRoutes() 显式补的路由」两部分组成，
// 而页面里 `router.push()` 的目标是硬编码字符串。两者不校验就会出现「按钮点了没反应」——
// 路由守卫 `guard.ts` 对 `to.matched.length === 0` 的处理是**静默 replace 到 /dashboard**
// （不是显示 404 页），所以症状是"点一下跳回工作台"，很难被发现。
//
// 本脚本用真实浏览器逐个打开目标 URL，比较跳转前后的 URL 判定路由是否存在。
//
// 前置：① 后端 5655 在跑；② 前端 dev server 5656 在跑。
// 用法：node tools/verify-sales-routes.cjs
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')

const BASE = process.env.BASE || 'http://localhost:5656'
const API = process.env.ERP_API || 'http://localhost:5655'

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
    body: JSON.stringify({
      username: process.env.E2E_USER || 'sra_e2e',
      password: process.env.E2E_PASS || 'admin123',
      tenantName: '系统租户',
      captcha: extractCaptcha(cd.img), captchaKey: cd.uuid,
    }),
  }).then(r => r.json())
  const d = lg?.data || {}
  return { token: d.token || d.accessToken || d.tokenValue, tenantId: d.tenantId }
}

// 被检查的跳转目标（来源见每条的「出处」注释，均为页面里真实的 router.push 目标）
const TARGETS = [
  ['/sales/return-doc/create', '销售退货单列表「新增」按钮 · return-doc/index.vue:690'],
  ['/sales/return-doc/form/1', '销售退货单列表「详情/编辑/打印」· return-doc/index.vue:694,698,948'],
  ['/sales/return-doc', '销售退货单表单「历史」· return-doc/form.vue:1053'],
  ['/sales/outbound/create', '单据查询「复制出库单」· doc-query/index.vue:1109'],
  ['/sales/outbound', '销售出库单表单「历史」· outbound/form.vue:1092'],
  ['/sales/outbound/form/1', '销售出库单列表「编辑」· outbound/index.vue:1241'],
  ['/sales/order/form', '销售订单列表「新增」· order-center/index.vue:1002'],
  ['/sales/order/form/1', '订单中心「详情」· order-center/index.vue:1324 / 退货申请跳源单 · return-apply/form.vue:934'],
  ['/sales/outbound/form/1', '退货申请跳源出库单 · return-apply/form.vue:936'],
  ['/sales/return-apply/create', '销售退货申请列表「新增」· return-apply/index.vue:1055'],
  ['/sales/return-apply/form/1', '销售退货申请列表「编辑」· return-apply/index.vue:1114'],
  ['/sales/return-apply', '销售退货申请表单「历史」· return-apply/form.vue:1010'],
  ['/sales/retail/create', '零售单列表「新增」· retail/index.vue:767'],
  ['/sales/retail/form/1', '零售单列表「详情/编辑」· retail/index.vue:768'],
  ['/sales/pre-order/create', '预订货单列表「新增」· pre-order/index.vue:951'],
  ['/sales/pre-order/form/1', '预订货单列表「编辑」· pre-order/index.vue:952'],
  ['/sales/exchange/form', '销售换货单列表「新增/编辑」· exchange/index.vue:785,789'],
  ['/sales/exchange/index', '销售换货单表单「历史」· exchange/form.vue:1430'],
  ['/sales/doc-query', '菜单 70030 销售单据查询'],
  ['/sales/detail-query', '菜单 70031 销售明细查询'],
  ['/sales/price-track', '菜单 70032 销售价格跟踪'],
  ['/sales/order-center', '菜单 80050 订单处理中心'],
]

async function main() {
  let { token, tenantId } = await apiLogin()
  if (!token) { console.log('登录失败：检查 5655 后端与账号 sra_e2e'); process.exit(1) }

  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1600, height: 900 } })
  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([tk, tid]) => {
    localStorage.setItem('token', tk)
    localStorage.setItem('tenantId', String(tid || 1))
  }, [token, tenantId])

  // 先打开一次工作台，让动态路由完成首次加载（路由表加载后守卫才不会把已知页面也判成 404）
  await page.goto(`${BASE}/dashboard`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(4000)

  for (const [target, source] of TARGETS) {
    await page.goto(`${BASE}${target}`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(2200)
    const finalUrl = new URL(page.url()).pathname
    // ⚠️ 判定不能只看 URL：dynamicRoutes 在 Layout 下注册了 catch-all
    //    `/:pathMatch(.*)*` → 404.vue，**未注册的路径 URL 也不会变**，只是渲染 404 页。
    //    第一版脚本只比 URL，22 条全部误判为「通过」。
    const probe = await page.evaluate(() => ({
      notFound: !!document.querySelector('.not-found'),
      text: (document.body.innerText || '').slice(0, 400),
    })).catch(() => ({ notFound: false, text: '' }))
    const is404 = probe.notFound || probe.text.includes('您访问的页面不存在')
    const compMissing = probe.text.includes('页面组件未找到')
    const bounced = finalUrl === '/dashboard' || finalUrl === '/403'
    const ok = !bounced && !is404 && !compMissing
    let why = ''
    if (bounced) why = `被守卫弹到 ${finalUrl}`
    else if (is404) why = '渲染了 404 页（catch-all 命中，路由未注册）'
    else if (compMissing) why = '组件未解析（componentMap 缺键）'
    check(`${target}   〔${source}〕`, ok, why)
  }

  await browser.close()
  console.log(`\n通过 ${pass} / 共 ${pass + fail}`)
  process.exit(fail ? 1 : 0)
}

main().catch(e => { console.error(e); process.exit(1) })
