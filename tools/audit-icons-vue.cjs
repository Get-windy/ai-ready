/* 静态审计：全前端源码里从 @ant-design/icons-vue 导入的图标名，是否真的存在于**已安装版本**。
 *
 * 为什么需要：这类文件是从某个 UI 设计稿/别处拷来的常见写法，若引用了当前版本**已移除/不存在**的导出，
 * 该路由的动态 import() 会抛 SyntaxError，vue-router 直接中止导航 ——
 * 症状是**整页白屏 + 从其它页面下钻/跳转该页时 URL 不变**，控制台报
 *   "The requested module '.../@ant-design_icons-vue.js' does not provide an export named 'XxxOutlined'"
 * 编译器与 vite 构建都**不报错**（只有运行时按需加载才暴露），所以加一道静态闸门。
 *
 * 背景案例（2026-09-13）：`@ant-design/icons-vue` 升到 v7.0.1 后 `AlarmOutlined` 已不存在，
 * `views/dms/dispatch-task/index.vue` 仍导入它 → 调度任务页白屏、配送仪表盘两处下钻失效。
 *
 * 用法：node tools/audit-icons-vue.cjs
 * 退出码：0 = 全部存在；1 = 存在缺失（打印文件与图标名）
 */
const fs = require('fs')
const path = require('path')

const SRC = path.resolve(__dirname, '../frontend/apps/pc-admin/src')
const PNPM = path.resolve(__dirname, '../frontend/node_modules/.pnpm')

/** 解析已安装的 @ant-design/icons-vue 及其 icons 目录（pnpm 布局） */
function resolveIconsDir() {
  const cands = fs.readdirSync(PNPM).filter(d => d.startsWith('@ant-design+icons-vue@'))
  if (!cands.length) throw new Error('未找到已安装的 @ant-design/icons-vue（' + PNPM + '）')
  const dir = path.join(PNPM, cands[0], 'node_modules/@ant-design/icons-vue/es/icons')
  if (!fs.existsSync(dir)) throw new Error('图标目录不存在：' + dir)
  return { dir, version: cands[0] }
}

function walk(dir, out = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, e.name)
    if (e.isDirectory()) walk(full, out)
    else if (/\.(vue|ts|tsx|js)$/.test(e.name)) out.push(full)
  }
  return out
}

const { dir, version } = resolveIconsDir()
const available = new Set(fs.readdirSync(dir).filter(f => f.endsWith('.js')).map(f => f.replace(/\.js$/, '')))

const bad = []
let imports = 0, names = 0
for (const file of walk(SRC)) {
  const text = fs.readFileSync(file, 'utf8')
  const re = /import\s*\{([^}]*)\}\s*from\s*['"]@ant-design\/icons-vue['"]/g
  let m
  while ((m = re.exec(text))) {
    imports++
    for (const raw of m[1].split(',')) {
      const name = raw.trim().split(/\s+as\s+/)[0].trim()
      if (!name) continue
      names++
      if (!available.has(name)) bad.push({ file: path.relative(SRC, file).replace(/\\/g, '/'), name })
    }
  }
}

console.log(`图标包：${version}（可用 ${available.size} 个）`)
console.log(`扫描：${imports} 处 icons-vue 导入 / ${names} 个图标名`)
if (!bad.length) {
  console.log('全部图标名均存在 ✅')
  process.exit(0)
}
console.log(`\n缺失的图标名 ${bad.length} 处（会导致整页白屏，必须改名或换图标）：`)
for (const b of bad) console.log(`  ✗ ${b.file} → ${b.name}`)
process.exit(1)
