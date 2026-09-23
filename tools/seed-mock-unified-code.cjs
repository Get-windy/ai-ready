#!/usr/bin/env node
/**
 * 给 `biz_party` 补**模拟**统一社会信用代码（阶段 3 的前置：识别键有值才谈得上归并）
 *
 * 【为什么需要它】阶段 3 的施工方案实测发现：`biz_party.unified_code` **152 行全为空**，
 *   而按名称归并是裁定④**明令禁止**的 ⇒ "自动归并"当时不可执行。
 *   用户 2026-09-23 明确：**当前库是平台测试用的模拟数据，可以直接把信用代码模拟上**。
 *
 * 【生成规则（三条硬要求）】
 *   ① **格式合法**：按 GB 32100-2015 生成 18 位、字符集与**校验位**都算对
 *      （否则将来加 CHECK 约束或写校验器时，这批模拟数据会第一个把它顶红）；
 *   ② **一眼可辨是模拟**：前 12 位固定 `91999999FAKE`
 *      （`91` = 工商企业 · `999999` = **不存在的行政区划** · `FAKE` = 明示假数据），
 *      后 5 位为序号 ⇒ **任何人看到都知道这不是真代码**，也便于一条 SQL 全部还原；
 *   ③ **幂等 + 可回退**：只填"当前为空"的行；`--revert` 按上述前缀把值还原为 NULL。
 *
 * 【只动一列】本脚本**只更新 `unified_code`**，不碰名称/租户/其他任何列。
 *
 * 用法：
 *   node tools/seed-mock-unified-code.cjs            # 补值（只填空的）
 *   node tools/seed-mock-unified-code.cjs --dry-run  # 只看会改哪些行，不写库
 *   node tools/seed-mock-unified-code.cjs --revert   # 按模拟前缀还原为 NULL
 *   前置：本地 dev 库可连（走 tools/sql.cjs）。
 *   退出码：0 = 成功；1 = 自检不通过；2 = 脚本自身失败。
 */

const { execFileSync } = require('child_process')
const path = require('path')

const REPO = path.resolve(__dirname, '..')

/** 模拟代码前缀（12 位）：91 工商企业 + 999999 不存在的区划 + FAKE 明示假数据。 */
const MOCK_PREFIX = '91999999FAKE'
/** GB 32100-2015 字符集（**不含 I O S V Z**）与加权因子 */
const CHARSET = '0123456789ABCDEFGHJKLMNPQRTUWXY'
const WEIGHTS = [1, 3, 9, 27, 19, 26, 16, 17, 20, 29, 25, 13, 8, 24, 10, 30, 28]

class Fail extends Error {}

function sql(stmt) {
  try {
    return execFileSync('node', [path.join(REPO, 'tools', 'sql.cjs'), stmt],
      { cwd: REPO, encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 })
  } catch (e) {
    const first = (String(e.stderr || e.message || e).replace(/\r/g, '').split('\n')
      .map(l => l.trim()).filter(Boolean)[0]) || '(无输出)'
    throw new Fail(`SQL 失败: ${stmt.slice(0, 120)} —— ${first}`)
  }
}

/** 取单值 */
function scalar(stmt) {
  const lines = sql(stmt).replace(/\r/g, '').split('\n').map(l => l.trim())
    .filter(l => l && !/^-+(\+-+)*$/.test(l) && !/^\(\d+ rows?\)$/.test(l))
  return lines.length > 1 ? lines[1] : '(空)'
}

/** 算 GB 32100 校验位（把 17 位补齐成合法 18 位） */
function checkDigit(first17) {
  let sum = 0
  for (let i = 0; i < 17; i++) {
    const idx = CHARSET.indexOf(first17[i])
    if (idx < 0) {
      throw new Fail(`第 ${i + 1} 位「${first17[i]}」不在 GB32100 字符集内`)
    }
    sum += idx * WEIGHTS[i]
  }
  const c = 31 - (sum % 31)
  return CHARSET[c === 31 ? 0 : c]
}

/** 生成第 n 个模拟代码（n 从 1 起；前 17 位 = 前缀 + 5 位序号） */
function mockCode(n) {
  const body = MOCK_PREFIX + String(n).padStart(5, '0')
  if (body.length !== 17) {
    throw new Fail(`模拟代码前 17 位长度应为 17，实际 ${body.length}（${body}）`)
  }
  return body + checkDigit(body)
}

/** 自检：长度 / 字符集 / 校验位 */
function assertValid(code) {
  if (code.length !== 18) throw new Fail(`长度应为 18：${code}`)
  for (const ch of code) {
    if (!CHARSET.includes(ch)) throw new Fail(`字符「${ch}」不在字符集内：${code}`)
  }
  if (checkDigit(code.slice(0, 17)) !== code[17]) throw new Fail(`校验位不对：${code}`)
}

/** 待补的行（只认未删 + 信用代码为空） */
function pendingRows() {
  return sql(`SELECT id, party_name FROM biz_party
    WHERE deleted = 0 AND (unified_code IS NULL OR btrim(unified_code) = '')
    ORDER BY id`).replace(/\r/g, '').split('\n').slice(2)
    .map(l => l.trim()).filter(l => l && !/^\(\d+ rows?\)$/.test(l))
    .map(l => {
      const [id, name] = l.split('|').map(s => s.trim())
      return { id, name: name || '(无名称)' }
    })
}

/** 模拟代码是否已有重复（真重复就得先解决，不能靠 CHECK 才发现） */
function duplicateMock() {
  return Number(scalar(`SELECT count(*) FROM (
    SELECT unified_code FROM biz_party
     WHERE deleted = 0 AND unified_code LIKE '${MOCK_PREFIX}%'
     GROUP BY unified_code HAVING count(*) > 1) x`))
}

;(() => {
  const args = process.argv.slice(2)
  const dryRun = args.includes('--dry-run')
  const revert = args.includes('--revert')
  console.log(revert ? '还原模拟信用代码（按前缀置回 NULL）'
    : `补模拟信用代码${dryRun ? '（--dry-run：只看不改）' : ''}`)

  try {
    // 前置：表和列在不在
    const hasCol = Number(scalar(`SELECT count(*) FROM information_schema.columns
      WHERE table_schema='public' AND table_name='biz_party' AND column_name='unified_code'`))
    if (hasCol !== 1) {
      console.error('前置不成立：biz_party.unified_code 不存在')
      process.exit(2)
    }

    if (revert) {
      const n = Number(scalar(`SELECT count(*) FROM biz_party WHERE unified_code LIKE '${MOCK_PREFIX}%'`))
      if (n === 0) {
        console.log('  没有带模拟前缀的行，无需还原')
        process.exit(0)
      }
      sql(`UPDATE biz_party SET unified_code = NULL WHERE unified_code LIKE '${MOCK_PREFIX}%'`)
      const left = Number(scalar(`SELECT count(*) FROM biz_party WHERE unified_code LIKE '${MOCK_PREFIX}%'`))
      if (left !== 0) {
        console.error(`还原不彻底：还剩 ${left} 行`)
        process.exit(1)
      }
      console.log(`  ✅ 已把 ${n} 行的模拟信用代码还原为 NULL（其余列未动）`)
      process.exit(0)
    }

    const rows = pendingRows()
    console.log(`  待补行数：${rows.length}（仅 deleted=0 且 unified_code 为空）`)
    if (rows.length === 0) {
      console.log('  ✅ 没有需要补的行（幂等：已有值的一律不动）')
      process.exit(0)
    }

    // 序号接着已有的模拟行往下排（保证不重号）
    const used = Number(scalar(`SELECT count(*) FROM biz_party WHERE unified_code LIKE '${MOCK_PREFIX}%'`))
    const assigns = rows.map((r, i) => ({ ...r, code: mockCode(used + i + 1) }))
    for (const a of assigns) {
      assertValid(a.code)
    }
    console.log(`  生成 ${assigns.length} 个模拟代码（已逐个自检：18 位 / 字符集 / 校验位）`)
    console.log(`  样例：${assigns[0].code}  ←  ${assigns[0].name}`)

    if (dryRun) {
      console.log('  （--dry-run）未写库。')
      process.exit(0)
    }

    // 逐行单值更新（不做批量 CASE：出问题时能一眼看出是哪个 id 写坏了）
    for (const a of assigns) {
      if (!/^\d+$/.test(a.id)) {
        throw new Fail(`id 不是纯数字，拒绝拼 SQL：${a.id}`)
      }
      sql(`UPDATE biz_party SET unified_code = '${a.code}' WHERE id = ${a.id} AND deleted = 0`)
    }

    // 回查断言：补了多少、还剩多少空、有没有重复
    const stillEmpty = Number(scalar(`SELECT count(*) FROM biz_party
      WHERE deleted = 0 AND (unified_code IS NULL OR btrim(unified_code) = '')`))
    const dup = duplicateMock()
    const dupAll = Number(scalar(`SELECT count(*) FROM (
      SELECT unified_code FROM biz_party WHERE deleted = 0 AND unified_code IS NOT NULL
      GROUP BY unified_code HAVING count(*) > 1) x`))
    console.log(`  回查：未删行里仍空 ${stillEmpty} 行 · 模拟码重复 ${dup} 组 · 全表信用代码重复 ${dupAll} 组`)
    if (stillEmpty !== 0 || dup !== 0 || dupAll !== 0) {
      console.error('  ❌ 回查不通过（应全部为 0）—— 请看上面的数字，必要时用 --revert 还原')
      process.exit(1)
    }
    console.log('  ✅ 已补齐且无重复。还原方式：node tools/seed-mock-unified-code.cjs --revert')
    process.exit(0)
  } catch (e) {
    console.error(`脚本异常: ${e instanceof Fail ? e.message : e}`)
    process.exit(2)
  }
})()
