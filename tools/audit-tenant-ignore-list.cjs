#!/usr/bin/env node
/**
 * 门禁：**没有 `tenant_id` 列的表，必须登记 `IGNORE_TENANT_TABLES`**
 *
 * 【为什么需要它】这条坑在本仓已经咬过 **4 次**（`shop_user` → `shop_user_party_link` →
 * `party` → `party_cert`/`party_bank`/`party_address`），而且两次症状完全不同、都很难往
 * "少登记一个表名"上想：
 *   · **读炸**：拦截器给 SELECT 注入 `AND tenant_id = <会话租户>`，而表里要么没有这一列
 *     （SQL 报「字段 tenant_id 不存在」），要么**只有 0**（共享层的行全在 0 上）⇒ 一行都读不到
 *     （`shop_user` 表现为"商城登录查无此人"）；
 *   · **写炸**：拦截器还会给 **INSERT 自动补一列** `tenant_id` ⇒
 *     `INSERT INTO party_cert (..., deleted, tenant_id)` ⇒ 「字段 tenant_id 不存在」——
 *     而 PostgreSQL 里语句报错会让**整个事务**进入 aborted 状态、提交时静默变回滚
 *     ⇒ 同事务里刚 insert 的 `biz_party` 行**一起没了**（2026-09-27 双写实测）。
 *
 * 【判据】"这张表该不该按会话租户过滤"——**不是**"表重要不重要"。
 *   没有 `tenant_id` 列 ⇒ 它没有租户维度 ⇒ 拦截器碰它必错 ⇒ 必须进忽略清单。
 *   ⚠️ 反方向也要看：**有 `tenant_id` 却进了忽略清单**的表**不会被租户过滤**，
 *   若它其实是租户维度的，那就是跨租户串数据。这类只报"需人工确认理由"，不直接判违规
 *   （`agreement*` 就是合法例外：主档 tenant_id 恒 0，可见性靠代码**显式判定**）。
 *
 * 【怎么判断"被 MyBatis 用到"】只扫 **Java + mapper XML**（排除 `db/migration`：
 *   那只是建表、不代表运行期会读写；也排除 `tools/`：那些脚本走直连，不过拦截器）。
 *   抓两类引用：`@TableName("<表>")` 与 SQL 关键字后紧跟的表名，最后**与真库表名取交集**
 *   （把注释/字符串里的词噪声滤掉）。
 *
 * 用法：node tools/audit-tenant-ignore-list.cjs
 *   前置：本地 dev 库可连（走 tools/sql.cjs）。
 *   退出码：0 = 无违规；1 = 有违规（缺登记）；2 = 脚本自身失败。
 */

const { execFileSync } = require('child_process')
const fs = require('fs')
const path = require('path')

const REPO = path.resolve(__dirname, '..')
const CONFIG = path.join(REPO, 'backend/core/base/core-base/src/main/java/cn/aiedge/base/config/MyBatisPlusConfig.java')

class Fail extends Error {}

function sql(stmt) {
  try {
    return execFileSync('node', [path.join(REPO, 'tools', 'sql.cjs'), stmt],
      { cwd: REPO, encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 })
  } catch (e) {
    const first = (String(e.stderr || e.message || e).replace(/\r/g, '').split('\n')
      .map(l => l.trim()).filter(Boolean)[0]) || '(无输出)'
    throw new Fail(`SQL 失败: ${stmt.slice(0, 140)} —— ${first}`)
  }
}

/**
 * 取一组名字。
 * ⚠️ **必须 `string_agg` 成一行**：`tools/sql.cjs` 对 SELECT 有 **500 行硬上限且静默截断**
 * ——直接 `SELECT table_name FROM information_schema.tables` 会**正好返回 500 行并当作成功**，
 * 于是"真库里不存在的表"会凭空多出一堆假结论（本脚本第一版就这么骗了自己：
 * `sys_tenant` / `sys_role_menu` / `sys_user_tenant` 这些明明存在的表被判成"陈旧条目"）。
 */
/**
 * 取单个值（第 2 行）。
 * ⚠️ **不能拿整段输出 `includes('t')` 判真假** —— 表头那行（`exists`）本身含字母 t，
 * 于是"每张表都命中"（本脚本第二版就这么把 32 张表全报成了跨租户风险）。
 */
function scalar(stmt) {
  const lines = sql(stmt).replace(/\r/g, '').split('\n').map(l => l.trim())
    .filter(l => l && !/^-+(\+-+)*$/.test(l) && !/^\(\d+ rows?\)$/.test(l))
  return lines.length > 1 ? lines[1] : ''
}

function nameList(innerSql) {
  const one = sql(`SELECT string_agg(t, ',' ORDER BY t) FROM (${innerSql}) x(t)`)
    .replace(/\r/g, '').split('\n').map(l => l.trim())
    .filter(l => l && !/^-+(\+-+)*$/.test(l) && !/^\(\d+ rows?\)$/.test(l))
  const raw = one.length > 1 ? one[1] : ''
  return raw ? raw.split(',') : []
}

/**
 * 从配置源码里读忽略清单。
 *
 * ⚠️ **必须先剥行注释、再剥块注释，顺序反了会把清单本身吃掉**（本脚本第一版实踩）：
 * 该文件的文档正文里出现过一个**字面量「斜杠+星号」**，而"先剥块注释"会让它一直吃到后面
 * 某个块注释结束符，把数组后半段（`party` / `party_cert` / `agreement*` 等）整段吞掉
 * ⇒ 门禁**静默**漏掉一半登记项，输出一堆"缺登记"假违规。
 * 加了下面的数量断言，让它下次直接报错而不是骗人。
 * （顺带：这段注释自己一度因为写了**块注释结束符的字面量**而把注释提前闭合 ⇒ 语法错误 ——
 *   在注释里描述"结束符"时不要真的把它打出来。）
 */
function ignoreList() {
  const src = fs.readFileSync(CONFIG, 'utf8').replace(/\r/g, '')
    .replace(/\/\/[^\n]*/g, '')            // ① 先剥行注释
    .replace(/\/\*[\s\S]*?\*\//g, '')      // ② 再剥块注释
  const start = src.indexOf('IGNORE_TENANT_TABLES')
  if (start < 0) throw new Fail('没在 MyBatisPlusConfig.java 里找到 IGNORE_TENANT_TABLES')
  const end = src.indexOf(');', start)
  if (end < 0) throw new Fail('没找到 IGNORE_TENANT_TABLES 的结尾 `);`')
  const list = [...src.slice(start, end).matchAll(/"([a-z0-9_]+)"/g)].map(m => m[1])
  // 断言"明显该在里面的几条"：解析一旦退化成残缺，宁可让门禁报错，也不要它报假违规
  for (const must of ['shop_user', 'party', 'party_cert']) {
    if (!list.includes(must)) {
      throw new Fail(`忽略清单解析可疑：连 ${must} 都没解析出来（实得 ${list.length} 条）——`
        + '多半是注释剥离又把清单吃掉了，别据此下结论')
    }
  }
  return list
}

/** 递归收集源码文件（排除 target / node_modules / 迁移 / tools） */
function walk(dir, out = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name)
    if (e.isDirectory()) {
      if (['target', 'node_modules', '.git', 'dist', 'build'].includes(e.name)) continue
      walk(p, out)
    } else if (/\.(java|xml)$/.test(e.name)) {
      out.push(p)
    }
  }
  return out
}

;(() => {
  try {
    const list = ignoreList()
    console.log(`忽略清单（IGNORE_TENANT_TABLES）：${list.length} 张表`)

    // 真库：全部业务表 / 没有 tenant_id 列的表
    const allTables = nameList(`SELECT table_name AS t FROM information_schema.tables
      WHERE table_schema='public' AND table_type='BASE TABLE'`)
    const all = new Set(allTables)
    const noTenant = nameList(`SELECT t.table_name AS t FROM information_schema.tables t
      WHERE t.table_schema='public' AND t.table_type='BASE TABLE'
        AND NOT EXISTS (SELECT 1 FROM information_schema.columns c
                         WHERE c.table_schema='public' AND c.table_name=t.table_name
                           AND c.column_name='tenant_id')`)
    const noTenantSet = new Set(noTenant)
    if (allTables.length < 500) {
      throw new Fail(`只读到 ${allTables.length} 张表（疑似又撞上 500 行截断），拒绝据此下结论`)
    }
    console.log(`真库：${allTables.length} 张表，其中**没有 tenant_id 列**的 ${noTenant.length} 张`)

    // 源码里"被 MyBatis 用到"的表名（与真库表名取交集，滤掉噪声）
    //
    // ⚠️ **Java 里必须只扫"像 SQL 的行"**，不能整文件扫（本脚本前两版都栽在这）：
    //   第一版把散文 `…… from supplier ……` 当成 SQL 引用；第二版好不容易剥了注释，
    //   又被**枚举里的英文描述**骗了 —— `PURCHASE_INVOICE("采购发票", "Purchase invoice from
    //   supplier", …)` ⇒ 凭空报出一个连实体都没有的 `supplier`。
    //   判据：这一行含 `#{` **或** 含**大写** SQL 关键字（本仓 SQL 一律大写；而英文散文是小写，
    //   大小写这一条就能把"描述句"与"真 SQL"分开）。
    //   mapper XML 是纯 SQL，不额外过滤。
    const SQLISH = /#\{|\b(SELECT|INSERT|UPDATE|DELETE|WHERE|VALUES|FROM|JOIN|INTO|SET)\b/
    const referenced = new Set()
    const skip = path.join(REPO, 'backend', 'core', 'api', 'core-api', 'src', 'main', 'resources', 'db')
    for (const f of walk(path.join(REPO, 'backend'))) {
      if (f.startsWith(skip)) continue
      const raw = fs.readFileSync(f, 'utf8').replace(/\r/g, '')
      const lines = raw.split('\n').filter(l => !/^\s*(\/\/|\*|\/\*)/.test(l))
      // ① `@TableName` 用**注释后的全文**匹配（⚠️ 不能只用 SQLISH 那份：`@TableName("kb_document")`
      //    这一行不含任何 SQL 关键字，会被 SQLISH 过滤掉 ⇒ 实体整批漏报，脚本会误报"无违规"）
      const code = f.endsWith('.java') ? lines.join('\n') : raw
      for (const m of code.matchAll(/@TableName\(\s*(?:value\s*=\s*)?"([a-z0-9_]+)"/g)) {
        if (all.has(m[1])) referenced.add(m[1])
      }
      // ② SQL 关键字只用**"像 SQL 的行"**那份（滤掉散文与英文描述里的 from XXX）
      const sqlText = f.endsWith('.java') ? lines.filter(l => SQLISH.test(l)).join('\n') : raw
      for (const m of sqlText.matchAll(/\b(?:FROM|INTO|UPDATE|JOIN)\s+([a-z][a-z0-9_]*)/gi)) {
        const t = m[1].toLowerCase()
        if (all.has(t)) referenced.add(t)
      }
    }
    console.log(`源码（Java + mapper XML，含 @TableName 与 SQL 关键字）：引用到真库表 ${referenced.size} 张`)

    // ── 违规：被用到、没有 tenant_id、又没登记 ──
    //    ⚠️ 与**基线**做差：本仓对这类"历史遗留、要逐张判定"的东西一律用**棘轮**（只能缩不能涨），
    //    同 `known-unauthorized-controllers.txt` 的处置。基线文件里每行必须带理由（见文件头）。
    const baselinePath = path.join(REPO, 'tools', 'tenant-ignore-list-baseline.txt')
    const baseline = fs.existsSync(baselinePath)
      ? fs.readFileSync(baselinePath, 'utf8').replace(/\r/g, '').split('\n')
        .map(l => l.trim()).filter(l => l && !l.startsWith('#'))
      : []
    const flagged = [...referenced].filter(t => noTenantSet.has(t) && !list.includes(t)).sort()
    const violations = flagged.filter(t => !baseline.includes(t))
    const staleBaseline = baseline.filter(t => !flagged.includes(t))

    // ── 反向：登记了但**有** tenant_id 列 ⇒ 这张表不会被租户过滤，需确认是否故意 ──
    //    但"有这一列"本身不是问题：共享层表的 `tenant_id` 是**哨兵 0**（§6.5），登记它恰恰是对的。
    //    真正要问的是：**表里有没有 `tenant_id > 0` 的行**（那才是真的按租户切分的数据）。
    const listedWithTenant = list.filter(t => all.has(t) && !noTenantSet.has(t) && /^[a-z0-9_]+$/.test(t)).sort()
    const realTenantData = []
    for (const t of listedWithTenant) {
      if (scalar(`SELECT EXISTS(SELECT 1 FROM ${t} WHERE tenant_id > 0)`) === 't') realTenantData.push(t)
    }
    // ── 清单里不存在的表（陈旧条目）──
    const listedMissing = list.filter(t => !all.has(t)).sort()

    console.log('\n── 结果 ──')
    if (violations.length) {
      console.log(`❌ **新增缺登记 ${violations.length} 张**（基线之外；被 MyBatis 用到、没有 tenant_id 列、又不在忽略清单）:`)
      for (const t of violations) console.log(`   - ${t}`)
      console.log('   ⇒ 后果：租户会话下读会报"字段 tenant_id 不存在"或一行都读不到；')
      console.log('     写更狠 —— 拦截器给 INSERT 自动补一列 tenant_id，报错后整个事务静默回滚。')
      console.log('   ⚠️ 但**不要自动补登记**：要逐张判定"它本来就该是共享/系统级（⇒ 登记）"还是')
      console.log('     "它本该按租户切分、只是漏了 tenant_id 列"（⇒ 那是数据模型缺口，登记会把越权读变成正常）。')
    } else {
      const known = flagged.length
      console.log(`✅ 无**新增**缺登记${known ? `（基线内已有 ${known} 张待逐张判定，见 tools/tenant-ignore-list-baseline.txt）` : ''}`)
    }
    if (staleBaseline.length) {
      console.log(`\n🧹 基线里有 ${staleBaseline.length} 张**已经不再命中**（棘轮可以缩了，删掉这些行）: `
        + staleBaseline.join(', '))
    }
    if (realTenantData.length) {
      console.log(`\n⚠️⚠️ 清单里有 ${realTenantData.length} 张表**确实存着 tenant_id > 0 的数据**，`
        + '却因登记而**不会被租户过滤** —— 这是"跨租户可见"的风险面，请逐张确认是故意的:')
      console.log('   ' + realTenantData.join(', '))
    }
    const sentinelOnly = listedWithTenant.filter(t => !realTenantData.includes(t))
    if (sentinelOnly.length) {
      console.log(`\n✅ 另有 ${sentinelOnly.length} 张登记表虽然**有** tenant_id 列、但数据全是 0 或空`
        + '（共享层哨兵，§6.5）⇒ 登记正确，无需处理')
    }
    if (listedMissing.length) {
      console.log(`\n⚠️ 清单里有 ${listedMissing.length} 张表在真库里**不存在**（陈旧条目，建议清掉）: `
        + listedMissing.join(', '))
    }
    process.exit(violations.length ? 1 : 0)
  } catch (e) {
    console.error(`脚本异常: ${e instanceof Fail ? e.message : e}`)
    process.exit(2)
  }
})()
