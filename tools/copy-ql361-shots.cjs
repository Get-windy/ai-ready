/* 把 ql361 抓取的弹窗截图归档到「企智连截图」资产树（与仓储/采购模块同构）
 * 用法： node tools/copy-ql361-shots.cjs
 * 来源： tool-results/ql361/<域>-live/shots/*.png     （域：商城 / 营销 / 分析）
 * 目标： C:\Users\Administrator\Pictures\编程软件截图\企智连截图\<模块>\<页名>\<原文件名>
 * 模块映射： 商城→交易模块、营销→营销模块、分析→分析模块
 */
const fs = require('fs')
const path = require('path')

const SRC = path.resolve(__dirname, '../tool-results/ql361')
const DST = 'C:/Users/Administrator/Pictures/编程软件截图/企智连截图'
const MAP = { '商城': '交易模块', '营销': '营销模块', '分析': '分析模块' }

/** 从弹窗文件名解析出「页名」：去掉 -<Tab>-列配置弹窗.png / -列配置弹窗.png / -页面配置弹窗.png 后缀 */
function pageOf(file) {
  return file
    .replace(/-(列配置弹窗|页面配置弹窗)\.png$/, '')
    .replace(/-[^-]+$/, (m, off, s) => {
      // 若去掉 Tab 名后仍能得到一个「已存在的页目录」，则说明这一段是 Tab 名
      return m
    })
}

let copied = 0, skipped = 0, dirs = new Set()
for (const [domain, mod] of Object.entries(MAP)) {
  const srcDir = path.join(SRC, `${domain}-live`, 'shots')
  if (!fs.existsSync(srcDir)) { console.log('skip (无 shots):', domain); continue }
  for (const f of fs.readdirSync(srcDir)) {
    if (!f.endsWith('.png')) continue
    // 依次尝试：无 Tab（<页名>-xxx）与含 Tab（<页名>-<Tab>-xxx）
    let page = null
    const m1 = f.match(/^(.+?)-(列配置弹窗|页面配置弹窗)\.png$/)
    if (m1) page = m1[1]
    const m2 = f.match(/^(.+?)-([^-]+)-(列配置弹窗|页面配置弹窗)\.png$/)
    const pageDirs = new Set(fs.readdirSync(path.join(DST, mod)).filter(d => fs.statSync(path.join(DST, mod, d)).isDirectory()))
    if (m2 && pageDirs.has(m2[1])) page = m2[1]           // 形如「订单处理-按单据-列配置弹窗」
    if (m1 && pageDirs.has(m1[1])) page = m1[1]           // 形如「商品上架-列配置弹窗」
    if (!page || !pageDirs.has(page)) { skipped++; console.log('  ? 无法归属:', f); continue }
    const outDir = path.join(DST, mod, page)
    fs.mkdirSync(outDir, { recursive: true })
    fs.copyFileSync(path.join(srcDir, f), path.join(outDir, f))
    dirs.add(`${mod}/${page}`)
    copied++
  }
}
console.log(`\n已复制 ${copied} 张弹窗图，跳过 ${skipped} 张，覆盖 ${dirs.size} 个页面目录`)
