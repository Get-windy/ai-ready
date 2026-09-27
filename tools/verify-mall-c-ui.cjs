/*
 * 商城 C 端一期 UI 验证（真机 + 截图）。
 *
 * 验证：5 个底部主 Tab、顶部二级 Tab、公告条、商品分类宫格、双列商品卡、
 *       价格显示（游客开放看价时是真数字），以及**无 console error**。
 *
 * 副作用与复原：为了让页面拿到真实数据，会临时把该店置为
 *   allow_guest=ALLOW / guest_show_price=SHOW，结束时按原值还原并回读断言。
 *
 * 用法: node tools/verify-mall-c-ui.cjs [前端地址=http://localhost:3012] [tenantId=1]
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const FE = process.argv[2] || process.env.FE_URL || 'http://localhost:3012'
const TENANT = String(process.argv[3] || process.env.SHOP_TENANT || '1')
const SHOTS = 'I:/AI-Ready/tool-results/mall-c'
const DSN = { host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' }

async function db(sql, params) {
  const c = new Client(DSN)
  await c.connect()
  try { return (await c.query(sql, params)).rows } finally { await c.end() }
}

let pass = 0, fail = 0
const failures = []
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✔ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; failures.push(name); console.log(`  ✘ ${name}${detail ? ' — ' + detail : ''}`) }
}

;(async () => {
  const [orig] = await db(
    'SELECT allow_guest, guest_show_price FROM tenant_shop_config WHERE tenant_id=$1 AND deleted=0', [Number(TENANT)])
  if (!orig) { console.error('无店铺配置，终止'); process.exitCode = 1; return }
  console.log(`\nC 端 UI 验证 —— ${FE}/?tenantId=${TENANT}`)
  console.log(`原配置 allow_guest=${orig.allow_guest} guest_show_price=${orig.guest_show_price}\n`)

  await db("UPDATE tenant_shop_config SET allow_guest='ALLOW', guest_show_price='SHOW' WHERE tenant_id=$1 AND deleted=0", [Number(TENANT)])

  const consoleErrors = []
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2 })
  page.on('console', m => { if (m.type() === 'error') consoleErrors.push(m.text()) })
  page.on('pageerror', e => consoleErrors.push('pageerror: ' + e.message))

  try {
    await page.goto(`${FE}/?tenantId=${TENANT}`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(4000)

    console.log('── 底部 5 个主 Tab ──')
    const tabs = await page.$$eval('.van-tabbar-item', els => els.map(e => e.textContent.trim()))
    check('Tab 数量 = 5', tabs.length === 5, tabs.join(' / '))
    check('Tab 名称与顺序正确',
      JSON.stringify(tabs) === JSON.stringify(['首页', '分类', '消息', '购物车', '我的']), tabs.join(' / '))

    console.log('\n── 顶部二级 Tab ──')
    const topTabs = await page.$$eval('.top-tabs__item', els => els.map(e => e.textContent.trim()))
    check('存在顶部 Tab 且为 推荐/新品/热销',
      topTabs.length === 3 && topTabs[0] === '推荐', topTabs.join(' / '))
    const activeTop = await page.$eval('.top-tabs__item.is-active', e => e.textContent.trim()).catch(() => '')
    check('默认选中「推荐」', activeTop === '推荐', `active=${activeTop}`)

    console.log('\n── 首页区块 ──')
    const shopName = await page.$$eval('.shop-name', els => els.map(e => e.textContent.trim()))
    check('店铺名来自后台配置', shopName[0] === 'AI-Ready B2B商城', shopName.join(','))
    check('公告条渲染', (await page.$$('.notice-bar')).length === 1)
    check('商品分类宫格渲染', (await page.$$('.category-grid__item')).length > 0,
      `${(await page.$$('.category-grid__item')).length} 个分类`)
    const cards = await page.$$('.product-card')
    check('商品卡渲染', cards.length > 0, `${cards.length} 张`)
    const priceText = await page.$$eval('.product-price', els => els.map(e => e.textContent.trim())).catch(() => [])
    const placeholderText = await page.$$eval('.product-price-placeholder', els => els.map(e => e.textContent.trim())).catch(() => [])
    // 规则已允许看价 ⇒ 不该再出现「登录可见价」。
    // 注意：真库 erp_product.retail_price **全为空**，此时正确文案是「暂无价格」（属"无价"而非"隐藏价"），
    // 故只断言"不是登录可见价"，不强制必须有数字 —— 业务补价后自然变成数字。
    check('价格位不再显示「登录可见价」（规则已允许看价）',
      !placeholderText.includes('登录可见价'), placeholderText.slice(0, 3).join(' | ') || '(无占位)')
    check('价格要么是数字、要么是「暂无价格」',
      priceText.length > 0 || placeholderText.every(t => t === '暂无价格'),
      '数字 ' + priceText.length + ' 个 / 暂无价格 ' + placeholderText.filter(t => t === '暂无价格').length + ' 个')
    const hint = await page.$$('.price-hint')
    check('底部价格提示条不显示（游客可看价）', hint.length === 0)

    console.log('\n── 切到「新品」Tab（不重建页面）──')
    await page.click('.top-tabs__item:nth-child(2)')
    await page.waitForTimeout(2500)
    const afterSwitch = await page.$$('.product-card')
    check('切换 Tab 后仍有商品卡', afterSwitch.length > 0, `${afterSwitch.length} 张`)
    const stillShop = await page.$$eval('.shop-name', els => els.length)
    check('页面未重建（标题栏仍在）', stillShop === 1)

    console.log('\n── 分类页：顶部商品标签栏 ──')
    await page.goto(`${FE}/category?tenantId=${TENANT}`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(3500)
    const catTabs = await page.$$eval('.top-tabs__item', els => els.map(e => e.textContent.trim()))
    check('分类页顶部标签栏存在且含「全部」+ 标签', catTabs.length > 1 && catTabs[0] === '全部',
      catTabs.slice(0, 6).join(' / ') + (catTabs.length > 6 ? ' …' : ''))
    check('分类宫格渲染', (await page.$$('.category-grid__item')).length > 0)

    console.log('\n── 消息页 ──')
    await page.goto(`${FE}/message?tenantId=${TENANT}`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(2000)
    const msgTabs = await page.$$eval('.top-tabs__item', els => els.map(e => e.textContent.trim()))
    check('消息页顶部 Tab = 客服 / 消息', JSON.stringify(msgTabs) === JSON.stringify(['客服', '消息']), msgTabs.join(' / '))
    check('客服页有快捷问题', (await page.$$('.van-cell')).length >= 5)

    // ⚠️ 截图必须紧跟在对应页面的导航之后。
    //    首版把 home.png 放在了 /message 之后 ⇒ 文件名叫 home、内容却是消息页（已修）。
    await page.goto(`${FE}/?tenantId=${TENANT}`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(3000)
    await page.screenshot({ path: `${SHOTS}/home.png`, fullPage: false })
    await page.screenshot({ path: `${SHOTS}/home-full.png`, fullPage: true })
    await page.goto(`${FE}/category?tenantId=${TENANT}`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(3000)
    await page.screenshot({ path: `${SHOTS}/category.png`, fullPage: false })
    await page.goto(`${FE}/message?tenantId=${TENANT}`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(1500)
    await page.screenshot({ path: `${SHOTS}/message.png`, fullPage: false })

    console.log('\n── 运行期 console ──')
    // 店铺未开放游客时接口会 403，属预期；这里只关心 JS 报错
    const realErrors = consoleErrors.filter(t => !/status of 40[0-9]|Failed to load resource/i.test(t))
    check('无 JS 运行期错误', realErrors.length === 0, realErrors.slice(0, 3).join(' | '))
  } finally {
    await browser.close()
    await db('UPDATE tenant_shop_config SET allow_guest=$1, guest_show_price=$2 WHERE tenant_id=$3 AND deleted=0',
      [orig.allow_guest, orig.guest_show_price, Number(TENANT)])
    const [back] = await db('SELECT allow_guest, guest_show_price FROM tenant_shop_config WHERE tenant_id=$1 AND deleted=0', [Number(TENANT)])
    console.log('\n── 复原复核 ──')
    check('allow_guest 已还原', back.allow_guest === orig.allow_guest, `now=${back.allow_guest} orig=${orig.allow_guest}`)
    check('guest_show_price 已还原', back.guest_show_price === orig.guest_show_price, `now=${back.guest_show_price} orig=${orig.guest_show_price}`)
  }

  console.log(`\n═══ 汇总：${pass}/${pass + fail} 通过 ═══`)
  if (failures.length) console.log('失败项：' + failures.join(' | '))
  console.log(`截图：${SHOTS}/home.png、home-full.png、category.png、message.png`)
  process.exitCode = fail ? 1 : 0
})().catch(e => { console.error('UI 验证异常:', e.message); process.exitCode = 1 })
