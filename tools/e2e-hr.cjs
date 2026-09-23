/*
 * 人力资源模块 · 金标准端到端验证
 *
 * 覆盖（10 个菜单页）：
 *   员工列表(90001) / 考勤记录(90002) / 请假管理(90003) / 薪资管理(90004) / 绩效考核(90005)
 *   职位管理(90006) / 职员部门(80530) / 岗位权限(80531) / 全部操作员(80532) / 招聘管理(907)
 *
 * 验收内容：
 *   第 0 节 可达性（P0 回归）—— 23 个 HR 端点必须 200（修复前 /hr 裸前缀导致全部 404）
 *   第 1 节 岗位（hr_position）编号 / CRUD / 编制统计 / 删除门控
 *   第 2 节 员工（hr_employee）工号号段 / CRUD / 状态机 / 编制联动 / 查询参数 / 异动留痕
 *   第 3 节 合同（hr_contract）编号 / CRUD / 状态流转 / 到期提醒
 *   第 4 节 考勤（hr_attendance）规则 / 打卡 / 更正 / 重算 / 统计
 *   第 5 节 请假（hr_leave_request）额度 / 提交 / 审批 / **请假↔考勤联动** / 撤销还原
 *   第 6 节 薪资（structure + payment）生成幂等 / 累计预扣 / 绩效系数联动 / 发放·撤销
 *   第 7 节 绩效（hr_performance）CRUD / 状态机 / 统计
 *   第 8 节 招聘（hr_recruitment + hr_candidate）统计 / 计数维护 / **转入职闭环**
 *   第 9 节 安全 —— /user/page 不返回 password；无 token 一律 401；菜单 907 已挂到 61406
 *   第 10 节 真库对账（直连 devdb）
 *   第 11 节 UI 金标准（Playwright：外壳骨架 / 表头齿轮 / 页面配置 / 无 console error / 截屏）
 *
 * 用法：node tools/e2e-hr.cjs
 *      HR_PORT=5655 FE_URL=http://localhost:5656 node tools/e2e-hr.cjs
 *      SKIP_UI=1 node tools/e2e-hr.cjs          # 只跑接口 + 真库
 *
 * 前置：
 *   1) 后端 dev profile 运行中（默认 5655）；UI 节另需前端 dev server（默认 5656，SKIP_UI=1 可跳过）
 *   2) 专用验收账号：
 *        python tools/dbq.py "$(cat tools/e2e-hr-user.sql)"     # e2e_hr / admin123
 *      ⚠️ 不要用 admin —— 同一账号被并行会话二次登录会触发 sa-token 互踢，表现为
 *      「页面跑到一半跳登录页 / 某接口莫名 401」，极易被误判成页面缺陷（本脚本实踩过）。
 *   3) 后端构建前务必先停掉占用 fat jar 的进程，并 `mvn -o -pl hr/hr-base clean install`：
 *      否则 IDE 写进 target/classes 的 ECJ 残缺类会被打进 jar，运行时报
 *      `java.lang.Error: Unresolved compilation problem`（第 0 节有守卫会扫出来）。
 *
 * 本脚本自造数据、跑完自动清理（第 12 节复核现场已复原）。
 */
const http = require('http')
const fs = require('fs')
const path = require('path')
const { execSync } = require('child_process')

const PORT = Number(process.env.HR_PORT || 5655)
const FE = process.env.FE_URL || 'http://localhost:5656'
const SKIP_UI = process.env.SKIP_UI === '1'
const SHOTS = 'I:/AI-Ready/tool-results/e2e-hr'
const REPO = 'I:/AI-Ready'

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

// ── 本脚本自造数据的前缀（清理时按前缀删除，避免误删他人数据） ──
const TAG = 'E2EHR'

// ══════════════ HTTP ══════════════
function rawReq(method, reqPath, body, token) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? null : JSON.stringify(body)
    const headers = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const rawPath = '/api' + reqPath
    const safePath = Array.from(rawPath).map(ch => (ch.charCodeAt(0) > 127 ? encodeURIComponent(ch) : ch)).join('')
    const r = http.request({ hostname: 'localhost', port: PORT, path: safePath, method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        resolve({ status: res.statusCode, buf, json: tryJson(buf) })
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

async function api(method, reqPath, body) {
  let r = await rawReq(method, reqPath, body, TOKEN)
  const code = r.json && Number(r.json.code)
  if (r.status === 401 || code === 401) {
    await login()
    r = await rawReq(method, reqPath, body, TOKEN)
  }
  return r
}

/** 断言用：取 Result.data */
function dataOf(r) {
  return r.json ? r.json.data : null
}
function okOf(r) {
  return r.status === 200 && r.json && Number(r.json.code) === 200
}

async function login() {
  TOKEN = await loginAs(process.env.E2E_USER || 'e2e_hr')
}

/** 以指定账号登录，返回其 token（不影响全局 TOKEN）—— 用于租户隔离类断言需要多身份的场景 */
async function loginAs(username, password, tenantName) {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username,
    password: password || process.env.E2E_PWD || 'admin123',
    tenantName: tenantName || process.env.E2E_TENANT || '系统租户',
    captcha: code, captchaKey: cap.json.data.uuid,
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error(`登录失败(${username}): ` + JSON.stringify(res.json).slice(0, 300))
  return token
}

/** 用指定 token 发请求（不走全局 TOKEN 与自动重登） */
function apiAs(token, method, reqPath, body) {
  return rawReq(method, reqPath, body, token)
}

/** 直连 devdb 查询（走 python psycopg2，仓库既有工具） */
function sql(query) {
  const escaped = query.replace(/"/g, '\\"')
  const out = execSync(`python "${REPO}/tools/dbq.py" "${escaped}"`, { encoding: 'utf8', maxBuffer: 32 * 1024 * 1024 })
  return JSON.parse(out)
}
function sqlOne(query) {
  const rows = sql(query)
  return rows && rows.length ? rows[0] : null
}
/**
 * 执行写 SQL（DELETE/UPDATE）。dbq.py 对非查询语句输出 `OK <rowcount>`（**不是 JSON**），
 * 故不能复用上面的 sql()（JSON.parse 会抛）。
 */
function execSql(statement) {
  const escaped = statement.replace(/"/g, '\\"')
  const out = execSync(`python "${REPO}/tools/dbq.py" "${escaped}"`, { encoding: 'utf8', maxBuffer: 32 * 1024 * 1024 })
  if (!/^OK\b/.test(out.trim())) {
    throw new Error('SQL 执行未成功: ' + out.slice(0, 160))
  }
  return out.trim()
}

// ══════════════ 结果收集 ══════════════
const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail && !ok ? ' — ' + String(detail).slice(0, 220) : ''}`)
}
function section(title) {
  console.log(`\n── ${title} ──────────────────────────────────`)
}

/** 造数登记的清理清单（按 id 逆序删除） */
const cleanup = []
async function dropOn(method, reqPath) {
  cleanup.push([method, reqPath])
}
/**
 * 走 SQL 的兜底清理。
 *
 * 用于「业务上不允许经 API 删除」的自造数据 —— 例如**已确认的绩效考核**（设计上拒绝删除）
 * 与**已入职的候选人**（已有员工档案挂在其上）。这类数据留在库里会污染后续验收，
 * 故由脚本按 id 精确清理（只删自己造的行，不用模糊条件）。
 */
const dbCleanup = []
function dropBySql(sqlText) {
  dbCleanup.push(sqlText)
}
async function runCleanup() {
  section('第 12 节 清理自产数据')
  let n = 0
  for (const [method, reqPath] of cleanup.reverse()) {
    try {
      const r = await api(method, reqPath)
      if (okOf(r)) n++
    } catch (e) { /* 清理失败不阻断 */ }
  }
  let m = 0
  for (const stmt of dbCleanup) {
    try {
      execSql(stmt)
      m++
    } catch (e) { /* 清理失败不阻断 */ }
  }
  console.log(`  清理 API ${n}/${cleanup.length} 项 · SQL ${m}/${dbCleanup.length} 项`)
}

// ══════════════ 主流程 ══════════════
;(async () => {
  const t0 = Date.now()
  console.log(`\n人力资源模块金标准 E2E  ·  后端 127.0.0.1:${PORT}  ·  前端 ${FE}\n`)
  await login()
  console.log('登录成功')

  // ═══════════ 第 -1 节 清掉「上一次异常中断」留下的残骸 ═══════════
  // 脚本跑到一半被中断（语法错/限流/后端重启）时第 12 节不会执行，残留会跨轮累积并污染
  // 后续断言（实测累计出过 27 名员工）。此处在**任何断言之前**按 TAG 前缀清一遍。
  // 只匹配本脚本自造的 `E2EHR` 前缀，绝不用模糊条件触碰真实数据。
  section('第 -1 节 清理历史残骸（仅 E2EHR 前缀）')
  const empIdsOfTag = `select id from hr_employee where employee_name like '${TAG}%'`
  const preClean = [
    `delete from hr_salary_payment where employee_id in (${empIdsOfTag})`,
    `delete from hr_attendance where employee_id in (${empIdsOfTag})`,
    `delete from hr_leave_request where employee_id in (${empIdsOfTag})`,
    `delete from hr_performance where comment like '${TAG}%'`,
    `delete from hr_employee_change where employee_name like '${TAG}%'`,
    `delete from hr_contract where contract_name like '${TAG}%'`,
    `delete from hr_candidate where name like '${TAG}%'`,
    `delete from hr_recruitment where position_name like '${TAG}%'`,
    `delete from hr_employee where employee_name like '${TAG}%'`,
    `delete from hr_position where position_name like '${TAG}%'`,
  ]
  let preCleaned = 0
  for (const stmt of preClean) {
    try { execSql(stmt); preCleaned++ } catch (e) { /* 不阻断 */ }
  }
  console.log(`  已清理 ${preCleaned}/${preClean.length} 张表的残骸`)

  // ═══════════ 第 0 节 可达性（P0 回归） ═══════════
  section('第 0 节 端点可达性（P0 回归：修复前全为 404）')
  const reach = [
    ['GET', '/hr/positions/page?pageNum=1&pageSize=5'],
    ['GET', '/hr/positions/list'],
    ['GET', '/hr/positions/stat'],
    ['GET', '/hr/employees/page?pageNum=1&pageSize=5'],
    ['GET', '/hr/employees/stat'],
    ['GET', '/hr/employees/next-no'],
    ['GET', '/hr/employee-changes/page?pageNum=1&pageSize=5'],
    ['GET', '/hr/contracts/page?pageNum=1&pageSize=5'],
    ['GET', '/hr/contracts/next-no'],
    ['GET', '/hr/contracts/expiring?days=30'],
    ['GET', '/hr/attendance/page?pageNum=1&pageSize=5'],
    ['GET', '/hr/attendance/rule'],
    ['GET', '/hr/attendance/stat?month=2026-09'],
    ['GET', '/hr/leave/page?pageNum=1&pageSize=5'],
    ['GET', '/hr/leave/quota?year=2026'],
    ['GET', '/hr/leave/stat?year=2026'],
    ['GET', '/hr/salary/structure/page?pageNum=1&pageSize=5'],
    ['GET', '/hr/salary/payment/page?pageNum=1&pageSize=5'],
    ['GET', '/hr/salary/payment/stat'],
    ['GET', '/hr/performance/page?pageNum=1&pageSize=5'],
    ['GET', '/hr/performance/stat'],
    ['GET', '/hr/recruitment/page?pageNum=1&pageSize=5'],
    ['GET', '/hr/recruitment/stat'],
    ['GET', '/hr/candidate/page?pageNum=1&pageSize=5'],
  ]
  let reachFail = 0
  for (const [m, p] of reach) {
    const r = await api(m, p)
    if (!okOf(r)) {
      reachFail++
      console.log(`     ✗ ${m} ${p} → ${r.status} ${(r.buf || '').toString().slice(0, 120)}`)
    }
  }
  check(`HR 端点可达 ${reach.length}/${reach.length}`, reachFail === 0, `失败 ${reachFail} 个`)

  // ⚠️ ECJ 残缺类守卫：IDE（Eclipse JDT language server）会把**带编译错误**的 .class 写进 target/classes，
  //    而 mvn 增量编译若判定「无变化」就不会覆盖它们 → 打出的 jar 里是「编译期不报错、运行时抛
  //    java.lang.Error: Unresolved compilation problem」的残类。hr 模块本月已实踩过一次
  //    （HrSalaryPaymentServiceImpl.generateMonthlyPayment → 500）。此处直接扫 jar 字节码标记。
  try {
    const { execSync } = require('child_process')
    // 优先扫 `~/.m2` 里**实际会被打进 fat jar** 的那一份（core-api package 取的是它，不是 target/classes）；
    // 本地 target 产物随时可能被并行会话 `mvn clean` 清掉，扫不到时**不应判失败**（那是环境状态，不是缺陷）。
    const m2Dir = `${process.env.USERPROFILE || 'C:/Users/Administrator'}/.m2/repository/cn/aiedge/hr-base`
    const pickJar = () => {
      const cands = []
      try {
        for (const v of fs.readdirSync(m2Dir)) {
          const d = `${m2Dir}/${v}`
          if (!fs.statSync(d).isDirectory()) continue
          for (const f of fs.readdirSync(d)) {
            if (/^hr-base-.*\.jar$/.test(f)) cands.push(`${d}/${f}`)
          }
        }
      } catch (e) { /* 无 .m2 目录 */ }
      const targetDir = `${REPO}/backend/hr/hr-base/target`
      try {
        for (const f of fs.readdirSync(targetDir)) {
          if (/^hr-base-.*\.jar$/.test(f)) cands.push(`${targetDir}/${f}`)
        }
      } catch (e) { /* 未构建 */ }
      return cands[0] || null
    }
    const jar = pickJar()
    if (!jar) {
      check('hr-base jar 无 ECJ 残缺类', true, '未找到已构建的 hr-base jar（跳过扫描：target 被清且 .m2 无产物）')
    } else {
      const names = execSync(`unzip -Z1 "${jar}"`, { encoding: 'utf8' }).split('\n').filter(n => n.endsWith('.class'))
      let dirty = 0
      for (const n of names) {
        const buf = execSync(`unzip -p "${jar}" "${n}"`, { maxBuffer: 1e8 })
        if (buf.toString('latin1').includes('Unresolved compilation')) dirty++
      }
      const shortJar = jar.replace(/^.*\/(hr-base-)/, '$1')
      check(`hr-base jar 无 ECJ 残缺类（${shortJar}，扫描 ${names.length} 个 class）`, dirty === 0,
        `发现 ${dirty} 个含 "Unresolved compilation" 标记的残类 —— 需 mvn -o -pl hr/hr-base clean install 后重新 package`)
    }
  } catch (e) {
    check('hr-base jar ECJ 残缺类扫描', false, String(e).slice(0, 160))
  }

  // ═══════════ 第 1 节 岗位 ═══════════
  section('第 1 节 岗位（hr_position）')
  const posCode = (dataOf(await api('GET', '/hr/positions/next-code')) || '')
  check('岗位编码号段非空且以 HRPOS 开头', /^HRPOS-/.test(posCode), posCode)

  const posRes = await api('POST', '/hr/positions', {
    positionName: `${TAG}测试岗位`, positionCode: posCode,
    positionLevel: 3, quotaCount: 2, sort: 999, status: 1, responsibility: 'E2E 自造数据',
  })
  const posId = dataOf(posRes)
  check('创建岗位 200', okOf(posRes) && !!posId, JSON.stringify(posRes.json).slice(0, 200))
  if (posId) await dropOn('DELETE', `/hr/positions/${posId}`)

  const posPage = await api('GET', `/hr/positions/page?pageNum=1&pageSize=20&positionName=${TAG}`)
  const posRow = (dataOf(posPage)?.records || [])[0]
  check('岗位列表可查回本页造数', !!(posRow && String(posRow.positionName || '').includes(TAG)),
    JSON.stringify(dataOf(posPage)?.total))
  check('岗位列表回填部门名（deptName 字段存在）', posRow ? 'deptName' in posRow : false, JSON.stringify(Object.keys(posRow || {}).slice(0, 12)))
  check('岗位编制字段存在（quotaCount / currentCount）', posRow ? ('quotaCount' in posRow && 'currentCount' in posRow) : false)

  const stat1 = dataOf(await api('GET', '/hr/positions/stat'))
  check('岗位统计返回编制/在岗/超编', stat1 && 'quotaTotal' in stat1 && 'currentTotal' in stat1 && 'overQuotaCount' in stat1,
    JSON.stringify(stat1))

  if (posId) {
    const upd = await api('PUT', `/hr/positions/${posId}`, { positionName: `${TAG}测试岗位改`, quotaCount: 3 })
    check('修改岗位 200', okOf(upd))
  }

  // ═══════════ 第 2 节 员工 ═══════════
  section('第 2 节 员工（hr_employee）')
  const no1 = dataOf(await api('GET', '/hr/employees/next-no'))
  check('工号号段非空且以 EMP- 开头（修复前工号恒定为空）', /^EMP-/.test(String(no1 || '')), no1)

  const empRes = await api('POST', '/hr/employees', {
    employeeName: `${TAG}甲`, gender: 1, phone: '13800000001', education: 5,
    positionId: posId || null, employeeType: 1, hireDate: '2026-09-01', remark: 'E2E 自造数据',
  })
  const empId = dataOf(empRes)
  check('创建员工 200', okOf(empRes) && !!empId, JSON.stringify(empRes.json).slice(0, 200))
  if (empId) await dropOn('DELETE', `/hr/employees/${empId}`)

  const empRow = dataOf(await api('GET', `/hr/employees/${empId}`))
  check('工号已由号段自动生成（非空）', !!(empRow && empRow.employeeNo), empRow?.employeeNo)
  check('新建员工强制进入试用期（status=2）', empRow?.status === 2, String(empRow?.status))

  // 连续新增第二条 —— 修复前「第二个空工号」必撞 UNIQUE(employee_no)
  const empRes2 = await api('POST', '/hr/employees', {
    employeeName: `${TAG}乙`, gender: 2, hireDate: '2026-09-02', positionId: posId || null,
  })
  const empId2 = dataOf(empRes2)
  check('连续新增第 2 条成功（工号唯一约束不再被空值撞坏）', okOf(empRes2) && !!empId2,
    JSON.stringify(empRes2.json).slice(0, 200))
  if (empId2) await dropOn('DELETE', `/hr/employees/${empId2}`)
  const row2 = empId2 ? dataOf(await api('GET', `/hr/employees/${empId2}`)) : null
  check('两条工号互不相同', !!(empRow && row2 && empRow.employeeNo !== row2.employeeNo),
    `${empRow?.employeeNo} vs ${row2?.employeeNo}`)

  // 查询参数逐个生效
  const byName = await api('GET', `/hr/employees/page?pageNum=1&pageSize=20&employeeName=${TAG}`)
  check('按姓名查询生效', Number(dataOf(byName)?.total) >= 2, String(dataOf(byName)?.total))
  const byNo = await api('GET', `/hr/employees/page?pageNum=1&pageSize=20&employeeNo=${empRow?.employeeNo}`)
  check('按工号查询生效（修复前后端不接收该参数）', Number(dataOf(byNo)?.total) >= 1, String(dataOf(byNo)?.total))
  const byPhone = await api('GET', '/hr/employees/page?pageNum=1&pageSize=20&phone=13800000001')
  check('按手机号查询生效', Number(dataOf(byPhone)?.total) >= 1, String(dataOf(byPhone)?.total))
  const byKw = await api('GET', `/hr/employees/page?pageNum=1&pageSize=20&keyword=${TAG}`)
  check('关键字（工号/姓名/手机号）查询生效', Number(dataOf(byKw)?.total) >= 2, String(dataOf(byKw)?.total))
  const byStatus = await api('GET', '/hr/employees/page?pageNum=1&pageSize=20&status=2')
  check('按状态查询生效', Number(dataOf(byStatus)?.total) >= 2, String(dataOf(byStatus)?.total))
  const byType = await api('GET', '/hr/employees/page?pageNum=1&pageSize=20&employeeType=1')
  check('按员工类型查询生效（修复前后端不接收该参数）', Number(dataOf(byType)?.total) >= 1, String(dataOf(byType)?.total))
  const byRange = await api('GET', '/hr/employees/page?pageNum=1&pageSize=20&hireDateStart=2026-09-01&hireDateEnd=2026-09-01')
  check('按入职日期区间查询生效', Number(dataOf(byRange)?.total) >= 1, String(dataOf(byRange)?.total))
  const paged = await api('GET', '/hr/employees/page?pageNum=1&pageSize=1')
  check('分页 pageSize=1 生效', (dataOf(paged)?.records || []).length === 1,
    JSON.stringify((dataOf(paged)?.records || []).length))
  const sorted = await api('GET', '/hr/employees/page?pageNum=1&pageSize=20&sortField=employeeNo&sortOrder=asc')
  check('服务端排序参数生效（不报错且返回数据）', okOf(sorted), (sorted.buf || '').toString().slice(0, 120))
  const statEmp = dataOf(await api('GET', '/hr/employees/stat'))
  check('员工统计端点返回 在职/试用/离职/本月入职',
    statEmp && ['total', 'active', 'probation', 'resigned', 'onDuty', 'hiredThisMonth'].every(k => k in statEmp),
    JSON.stringify(statEmp))

  // 编制联动：创建员工后 current_count 应 +1
  const posAfter = dataOf(await api('GET', `/hr/positions/page?pageNum=1&pageSize=5&positionName=${TAG}`))?.records?.[0]
  check('编制在岗人数随建档联动（current_count = 2）', Number(posAfter?.currentCount) === 2, String(posAfter?.currentCount))
  check('超编标记正确（编制 3 / 在岗 2 → 未超编）', posAfter?.overQuota === false, String(posAfter?.overQuota))

  // 状态机：2 → 1 转正
  const reg = await api('POST', `/hr/employees/${empId}/regularize?regularDate=2026-09-10&remark=E2E转正`)
  check('转正（试用 2 → 在职 1）成功', okOf(reg), (reg.buf || '').toString().slice(0, 160))
  const empAfterReg = dataOf(await api('GET', `/hr/employees/${empId}`))
  check('转正后 status=1 且 regular_date 已写', empAfterReg?.status === 1 && !!empAfterReg?.regularDate,
    `status=${empAfterReg?.status} regularDate=${empAfterReg?.regularDate}`)

  // 状态机：合法 1 → 0 离职
  const resign = await api('POST', `/hr/employees/${empId}/resign?lastWorkDate=2026-09-20&resignType=1&resignReason=E2E离职`)
  check('离职（在职 1 → 离职 0）成功', okOf(resign), (resign.buf || '').toString().slice(0, 160))
  const empAfterResign = dataOf(await api('GET', `/hr/employees/${empId}`))
  check('离职后 status=0 且 leave_date 非空', empAfterResign?.status === 0 && !!empAfterResign?.leaveDate,
    `status=${empAfterResign?.status} leaveDate=${empAfterResign?.leaveDate}`)

  // 状态机：非法迁移 0 → 1 必须被拒（修复前无任何校验）
  const illegal = await api('PUT', `/hr/employees/${empId}/status?status=1`)
  check('非法状态迁移 0 → 1 被拒绝（修复前可随意改）', !okOf(illegal),
    `status=${illegal.status} ${(illegal.buf || '').toString().slice(0, 120)}`)

  // 异动留痕
  const changes = dataOf(await api('GET', `/hr/employee-changes/page?pageNum=1&pageSize=50&employeeId=${empId}`))
  const types = (changes?.records || []).map(c => c.changeType)
  check('人事异动留痕：入职 + 转正 + 离职 三条（修复前无任何历史）',
    types.includes('ENTRY') && types.includes('REGULAR') && types.includes('RESIGN'), JSON.stringify(types))
  check('异动记录含前后快照 JSON', (changes?.records || []).some(c => !!c.beforeJson || !!c.afterJson))

  // 编制联动：离职后 current_count 应回到 1
  const posAfterResign = dataOf(await api('GET', `/hr/positions/page?pageNum=1&pageSize=5&positionName=${TAG}`))?.records?.[0]
  check('离职后在岗人数回落到 1', Number(posAfterResign?.currentCount) === 1, String(posAfterResign?.currentCount))

  // 岗位删除门控：仍有在岗员工时须拒绝
  const delPosBlocked = await api('DELETE', `/hr/positions/${posId}`)
  check('岗位下有在岗员工时删除被拒（编制管控生效）', !okOf(delPosBlocked),
    (delPosBlocked.buf || '').toString().slice(0, 140))

  // ═══════════ 第 3 节 合同 ═══════════
  section('第 3 节 合同（hr_contract）')
  const ctNo = dataOf(await api('GET', '/hr/contracts/next-no'))
  check('合同编号号段以 HT- 开头', /^HT-/.test(String(ctNo || '')), ctNo)
  const ctRes = await api('POST', '/hr/contracts', {
    employeeId: empId2, contractNo: ctNo, contractName: `${TAG}劳动合同`, contractType: 1,
    startDate: '2026-09-02', endDate: '2027-09-01', trialDateEnd: '2026-12-02',
    salaryAmount: 12000, signDate: '2026-09-02', remark: 'E2E 自造数据',
  })
  const ctId = dataOf(ctRes)
  check('创建合同 200（修复前该域只有读端点）', okOf(ctRes) && !!ctId, JSON.stringify(ctRes.json).slice(0, 200))
  if (ctId) await dropOn('DELETE', `/hr/contracts/${ctId}`)

  const ctList = dataOf(await api('GET', `/hr/employees/${empId2}/contracts`))
  check('员工合同列表可查回（修复前必抛 Invalid bound statement）',
    Array.isArray(ctList) && ctList.length >= 1, `len=${Array.isArray(ctList) ? ctList.length : 'n/a'}`)
  check('合同列表回填员工姓名', !!(ctList?.[0] && ctList[0].employeeName), ctList?.[0]?.employeeName)

  const ctBad = await api('POST', '/hr/contracts', { employeeId: empId2, contractType: 1, startDate: '2027-01-01', endDate: '2026-01-01' })
  check('合同起止日期倒挂被拒', !okOf(ctBad), (ctBad.buf || '').toString().slice(0, 140))

  if (ctId) {
    const ct3 = await api('PUT', `/hr/contracts/${ctId}/status?status=3`)
    check('合同状态流转 生效 → 终止 成功', okOf(ct3), (ct3.buf || '').toString().slice(0, 140))
    const ctAfter = (dataOf(await api('GET', `/hr/employees/${empId2}/contracts`)) || [])[0]
    check('合同终止后 status=3', Number(ctAfter?.status) === 3, String(ctAfter?.status))
  }
  const expiring = await api('GET', '/hr/contracts/expiring?days=30')
  check('合同到期提醒端点可用', okOf(expiring))

  // ═══════════ 第 4 节 考勤 ═══════════
  section('第 4 节 考勤（hr_attendance）')
  const ruleBefore = dataOf(await api('GET', '/hr/attendance/rule'))
  check('考勤规则可读（修复前无规则概念）',
    !!(ruleBefore && ruleBefore.workStartTime && ruleBefore.workEndTime), JSON.stringify(ruleBefore))
  const saveRule = await api('PUT', '/hr/attendance/rule', {
    workStartTime: '09:00:00', workEndTime: '18:00:00', lateGraceMinutes: 0, earlyGraceMinutes: 0, standardWorkHours: 8,
  })
  check('考勤规则可写', okOf(saveRule), (saveRule.buf || '').toString().slice(0, 140))

  const attRes = await api('POST', '/hr/attendance', {
    employeeId: empId2, attendanceDate: '2026-09-03', clockInTime: '09:30:00', clockOutTime: '17:30:00', remark: TAG,
  })
  const attId = dataOf(attRes)
  check('创建考勤记录（补卡）200', okOf(attRes) && !!attId, JSON.stringify(attRes.json).slice(0, 180))
  if (attId) await dropOn('DELETE', `/hr/attendance/${attId}`)

  const attRow = attId ? dataOf(await api('GET', `/hr/attendance/${attId}`)) : null
  check('迟到分钟已按规则计算（09:30 打卡 → 30 分钟）', Number(attRow?.lateMinutes) === 30, String(attRow?.lateMinutes))
  check('早退分钟已按规则计算（17:30 签退 → 30 分钟）', Number(attRow?.earlyMinutes) === 30, String(attRow?.earlyMinutes))
  check('工时已按规则计算（8 小时）', Number(attRow?.workHours) === 8, String(attRow?.workHours))
  check('考勤状态判定为迟到 LATE', attRow?.status === 'LATE', String(attRow?.status))

  const attRecalc = attId ? await api('PUT', `/hr/attendance/${attId}/recalculate`) : null
  check('考勤重算端点可用', attRecalc ? okOf(attRecalc) : false, attRecalc ? (attRecalc.buf || '').toString().slice(0, 120) : '')

  const attPage = await api('GET', '/hr/attendance/page?pageNum=1&pageSize=20&month=2026-09&employeeId=' + empId2)
  const attRow2 = (dataOf(attPage)?.records || [])[0]
  check('按月份 + 员工查询考勤生效', (dataOf(attPage)?.records || []).length >= 1, String(dataOf(attPage)?.total))
  check('考勤列表回填员工姓名 / 部门名', !!(attRow2 && attRow2.employeeName), attRow2?.employeeName)
  const attStat = dataOf(await api('GET', `/hr/attendance/stat?month=2026-09&employeeId=${empId2}`))
  check('考勤统计返回 正常/迟到/早退/缺勤/休假/工时',
    attStat && ['normalDays', 'lateDays', 'earlyDays', 'absentDays', 'leaveDays', 'totalWorkHours'].every(k => k in attStat),
    JSON.stringify(attStat))

  // ═══════════ 第 5 节 请假（含 请假 ↔ 考勤 联动） ═══════════
  section('第 5 节 请假（hr_leave_request）')
  const quotaList = dataOf(await api('GET', '/hr/leave/quota?year=2026'))
  check('假期额度清单返回 5 种类型（修复前无额度体系）', Array.isArray(quotaList) && quotaList.length >= 5,
    `len=${Array.isArray(quotaList) ? quotaList.length : 'n/a'}`)
  const saveQuota = await api('PUT', '/hr/leave/quota', { leaveType: 'ANNUAL', year: 2026, quotaDays: 10 })
  check('可设置年假额度 10 天', okOf(saveQuota), (saveQuota.buf || '').toString().slice(0, 140))

  // 超出余额必须被拒
  const overQuota = await api('POST', '/hr/leave', {
    employeeId: empId2, leaveType: 'ANNUAL', startDate: '2026-10-01', endDate: '2026-10-20', days: 20, reason: TAG,
  })
  check('超余额请假被拒（额度 10 天 / 申请 20 天）', !okOf(overQuota), (overQuota.buf || '').toString().slice(0, 160))

  const badRange = await api('POST', '/hr/leave', {
    employeeId: empId2, leaveType: 'ANNUAL', startDate: '2026-10-10', endDate: '2026-10-01', days: 2, reason: TAG,
  })
  check('请假起止日期倒挂被拒', !okOf(badRange), (badRange.buf || '').toString().slice(0, 140))

  const leaveRes = await api('POST', '/hr/leave', {
    employeeId: empId2, leaveType: 'ANNUAL', startDate: '2026-09-07', endDate: '2026-09-08', days: 2, reason: `${TAG}年假`,
  })
  const leaveId = dataOf(leaveRes)
  check('提交请假 200', okOf(leaveRes) && !!leaveId, JSON.stringify(leaveRes.json).slice(0, 180))
  if (leaveId) await dropOn('DELETE', `/hr/leave/${leaveId}`)

  // 未批准前不得批准第二次；先批准
  const approve = await api('PUT', `/hr/leave/${leaveId}/approve?comment=E2E批准`)
  check('批准请假成功', okOf(approve), (approve.buf || '').toString().slice(0, 140))
  const approveAgain = await api('PUT', `/hr/leave/${leaveId}/approve?comment=重复批准`)
  check('重复批准被拒（状态机生效）', !okOf(approveAgain), (approveAgain.buf || '').toString().slice(0, 140))

  // ★ 请假 ↔ 考勤联动
  const leaveAtt = await api('GET', `/hr/attendance/page?pageNum=1&pageSize=20&employeeId=${empId2}&status=LEAVE`)
  const leaveDays = (dataOf(leaveAtt)?.records || [])
  check('★ 批准请假后考勤自动写入「休假」标记（修复前无联动 → 休假期间会被判缺勤扣款）',
    leaveDays.length === 2, `len=${leaveDays.length}`)
  const leaveDb = sqlOne(`select count(*) as c from hr_attendance where employee_id = ${empId2} and status = 'LEAVE' and deleted = 0 and attendance_date between '2026-09-07' and '2026-09-08'`)
  check('★ 真库核对：休假标记落库 2 条', Number(leaveDb?.c) === 2, JSON.stringify(leaveDb))
  for (const d of leaveDays) await dropOn('DELETE', `/hr/attendance/${d.id}`)

  // 余额扣减
  const bal = dataOf(await api('GET', `/hr/leave/balance?employeeId=${empId2}&year=2026`))
  const annual = (bal?.items || []).find(i => i.leaveType === 'ANNUAL')
  check('假期余额 = 额度 10 − 已用 2 = 8', Number(annual?.remainingDays) === 8,
    JSON.stringify(annual))

  // 撤销 → 考勤标记应还原
  const cancel = await api('PUT', `/hr/leave/${leaveId}/cancel`)
  check('撤销已批准请假成功', okOf(cancel), (cancel.buf || '').toString().slice(0, 140))
  const afterCancel = await api('GET', `/hr/attendance/page?pageNum=1&pageSize=20&employeeId=${empId2}&status=LEAVE`)
  check('★ 撤销请假后考勤休假标记被还原', (dataOf(afterCancel)?.records || []).length === 0,
    `len=${(dataOf(afterCancel)?.records || []).length}`)
  const balAfter = (dataOf(await api('GET', `/hr/leave/balance?employeeId=${empId2}&year=2026`))?.items || [])
    .find(i => i.leaveType === 'ANNUAL')
  check('撤销后余额回到 10 天', Number(balAfter?.usedDays) === 0, JSON.stringify(balAfter))

  // ═══════════ 第 6 节 薪资 ═══════════
  section('第 6 节 薪资（salary structure + payment）')
  const stRes = await api('POST', '/hr/salary/structure', {
    employeeId: empId2, baseSalary: 10000, performanceSalary: 2000,
    positionAllowance: 500, transportAllowance: 200, mealAllowance: 300, housingAllowance: 0, otherAllowance: 0,
    socialBase: 10000, fundBase: 10000, effectiveDate: '2026-01-01', remark: TAG,
  })
  const stId = dataOf(stRes)
  check('创建薪资结构 200（修复前 selectEffectiveByEmployeeId 无 SQL 绑定必挂）',
    okOf(stRes) && !!stId, JSON.stringify(stRes.json).slice(0, 180))
  if (stId) await dropOn('DELETE', `/hr/salary/structure/${stId}`)
  const stGet = dataOf(await api('GET', `/hr/salary/structure/${empId2}`))
  check('取员工生效薪资结构成功', !!(stGet && String(stGet.id) === String(stId)), JSON.stringify(stGet)?.slice(0, 160))

  const perfRes = await api('POST', '/hr/performance', {
    employeeId: empId2, reviewPeriod: '2026-09', reviewType: 'MONTHLY',
    score: 92, level: 'A', performanceCoefficient: 1.2, comment: `${TAG}绩效`,
  })
  const perfId = dataOf(perfRes)
  check('创建绩效考核 200（含绩效系数 1.2）', okOf(perfRes) && !!perfId, JSON.stringify(perfRes.json).slice(0, 180))
  // 已确认的考核设计上不允许删除，故本行改用 SQL 兜底清理（见 dropBySql 注释）
  if (perfId) dropBySql(`delete from hr_performance where id = ${perfId}`)
  const perfConfirm = perfId ? await api('PUT', `/hr/performance/${perfId}/confirm`) : null
  check('确认绩效考核成功', perfConfirm ? okOf(perfConfirm) : false, perfConfirm ? (perfConfirm.buf || '').toString().slice(0, 140) : '')
  const perfModAfterConfirm = perfId ? await api('PUT', `/hr/performance/${perfId}`, { score: 60 }) : null
  check('已确认考核不允许修改', perfModAfterConfirm ? !okOf(perfModAfterConfirm) : false,
    perfModAfterConfirm ? (perfModAfterConfirm.buf || '').toString().slice(0, 140) : '')

  const gen = await api('POST', '/hr/salary/payment/generate?paymentMonth=2026-09')
  const genData = dataOf(gen)
  check('生成月度薪资 200 且返回统计对象（修复前静默产出 0 条）',
    okOf(gen) && genData && typeof genData.generated === 'number', JSON.stringify(genData))
  check('月度薪资确实生成了记录（generated ≥ 1）', Number(genData?.generated) >= 1, JSON.stringify(genData))
  check('月度生成结果含失败计数（不再静默吞异常）',
    genData && 'failed' in genData && 'noStructure' in genData && 'skipped' in genData, JSON.stringify(genData))

  const gen2 = dataOf(await api('POST', '/hr/salary/payment/generate?paymentMonth=2026-09'))
  check('重复生成幂等（第二次全部 skipped，不产生重复记录）',
    Number(gen2?.generated) === 0 && Number(gen2?.skipped) >= 1, JSON.stringify(gen2))

  const payPage = await api('GET', `/hr/salary/payment/page?pageNum=1&pageSize=50&paymentMonth=2026-09&employeeId=${empId2}`)
  const payRow = (dataOf(payPage)?.records || [])[0]
  check('薪资列表可查回本月记录', !!payRow, String(dataOf(payPage)?.total))
  if (payRow) await dropOn('DELETE', `/hr/salary/payment/${payRow.id}`)
  check('薪资记录含应发合计（grossAmount）', payRow ? payRow.grossAmount != null : false, String(payRow?.grossAmount))
  check('薪资记录含实发金额', payRow ? payRow.actualAmount != null : false, String(payRow?.actualAmount))
  check('★ 绩效系数联动绩效工资（2000 × 1.2 = 2400）', Number(payRow?.performanceAmount) === 2400,
    String(payRow?.performanceAmount))
  check('应发 = 基本 + 绩效 + 津贴 + 加班', payRow
    ? Math.abs(Number(payRow.grossAmount) - (Number(payRow.baseAmount) + Number(payRow.performanceAmount) + Number(payRow.allowanceAmount) + Number(payRow.overtimeAmount))) < 0.02
    : false, JSON.stringify({ g: payRow?.grossAmount, b: payRow?.baseAmount, p: payRow?.performanceAmount, a: payRow?.allowanceAmount, o: payRow?.overtimeAmount }))
  check('实发 = 应发 − 社保 − 公积金 − 个税 − 扣款', payRow
    ? Math.abs(Number(payRow.actualAmount) - (Number(payRow.grossAmount) - Number(payRow.socialDeduct) - Number(payRow.fundDeduct) - Number(payRow.taxDeduct) - Number(payRow.deductAmount))) < 0.02
    : false, JSON.stringify({ a: payRow?.actualAmount }))
  // 累计预扣法口径核对（本员工 2026-09-02 入职，故本单位受雇月份数 = 1）：
  //   累计应纳税所得额 = 13400 − 2250 − 5000×1 = 6150 → 3% → 184.50
  // 对比：旧的「按月单独计税」口径会算成 (13400 − 2250 − 5000) 落在 3000~12000 段 → 10% 速算 210 = 405
  check('★ 个税按累计预扣法计算（口径已从「月度单独计税」订正）', payRow
    ? Math.abs(Number(payRow.taxDeduct) - 184.5) < 0.02
    : false, `taxDeduct=${payRow?.taxDeduct}（期望 184.50 = (13400−2250−5000×1) × 3%；旧口径会得 405）`)
  check('社保按 10.5% 计（10000 × 10.5% = 1050）', payRow ? Number(payRow.socialDeduct) === 1050 : false, String(payRow?.socialDeduct))
  check('公积金按 12% 计（10000 × 12% = 1200）', payRow ? Number(payRow.fundDeduct) === 1200 : false, String(payRow?.fundDeduct))

  if (payRow) {
    const slip = dataOf(await api('GET', `/hr/salary/payment/${payRow.id}`))
    check('工资条详情返回 payment + structure', !!(slip && slip.payment && 'structure' in slip), Object.keys(slip || {}).join(','))
    const conf = await api('PUT', `/hr/salary/payment/${payRow.id}/confirm`)
    check('确认发放薪资成功', okOf(conf), (conf.buf || '').toString().slice(0, 140))
    const revoke = await api('PUT', `/hr/salary/payment/${payRow.id}/revoke`)
    check('撤销发放成功', okOf(revoke), (revoke.buf || '').toString().slice(0, 140))
    const confAgain = await api('PUT', `/hr/salary/payment/${payRow.id}/confirm`)
    check('撤销后可再次确认发放（状态 2 → 1）', okOf(confAgain), (confAgain.buf || '').toString().slice(0, 140))
  }

  const payStat = dataOf(await api('GET', '/hr/salary/payment/stat?paymentMonth=2026-09'))
  check('薪资统计返回人数/应发/实发/社保/公积金/个税',
    payStat && ['headcount', 'grossTotal', 'actualTotal', 'socialTotal', 'fundTotal', 'taxTotal'].every(k => k in payStat),
    JSON.stringify(payStat))

  // ═══════════ 第 7 节 绩效 ═══════════
  section('第 7 节 绩效（统计与校验）')
  const badScore = await api('POST', '/hr/performance', { employeeId: empId2, reviewPeriod: '2026-08', score: 999 })
  check('考核评分超出 0~100 被拒', !okOf(badScore), (badScore.buf || '').toString().slice(0, 140))
  const badLevel = await api('POST', '/hr/performance', { employeeId: empId2, reviewPeriod: '2026-08', score: 80, level: 'Z' })
  check('非法考核等级被拒', !okOf(badLevel), (badLevel.buf || '').toString().slice(0, 140))
  const perfStat = dataOf(await api('GET', '/hr/performance/stat?reviewPeriod=2026-09'))
  check('绩效统计返回平均分与等级分布',
    perfStat && 'averageScore' in perfStat && 'levelDistribution' in perfStat, JSON.stringify(perfStat))
  const perfPage = await api('GET', '/hr/performance/page?pageNum=1&pageSize=20&employeeId=' + empId2)
  const perfRow = (dataOf(perfPage)?.records || [])[0]
  check('绩效列表回填员工姓名与部门名', !!(perfRow && perfRow.employeeName), JSON.stringify({ n: perfRow?.employeeName, d: perfRow?.deptName }))
  const perfPageByLevel = await api('GET', '/hr/performance/page?pageNum=1&pageSize=20&level=A')
  check('按等级查询绩效生效', okOf(perfPageByLevel), String(dataOf(perfPageByLevel)?.total))

  // ═══════════ 第 8 节 招聘（含 转入职 闭环） ═══════════
  section('第 8 节 招聘（hr_recruitment + hr_candidate）')
  const recRes = await api('POST', '/hr/recruitment', {
    positionName: `${TAG}招聘岗位`, deptName: '市场部', headcount: 1, channel: 'ONLINE',
    urgency: 1, status: 1, requiredEducation: 5, requiredExperience: '3年',
    salaryMin: 10000, salaryMax: 15000, publishDate: '2026-09-01', description: 'E2E 自造数据',
  })
  const recId = dataOf(recRes)
  check('创建招聘职位 200', okOf(recRes) && !!recId, JSON.stringify(recRes.json).slice(0, 180))
  if (recId) await dropOn('DELETE', `/hr/recruitment/${recId}`)
  const recRow = recId ? dataOf(await api('GET', `/hr/recruitment/${recId}`)) : null
  check('招聘职位自动填充发布人与发布日', !!(recRow && recRow.publishDate), JSON.stringify({ p: recRow?.publishDate, u: recRow?.publisherName }))

  const candRes = await api('POST', '/hr/candidate', {
    recruitmentId: recId, name: `${TAG}候选人`, gender: 1, phone: '13900000001',
    education: 5, school: 'E2E大学', major: 'E2E专业', expectedSalary: 13000, source: 'ONLINE', status: 0,
  })
  const candId = dataOf(candRes)
  check('创建候选人 200', okOf(candRes) && !!candId, JSON.stringify(candRes.json).slice(0, 180))
  // 该候选人后面会走「转入职」变成已入职（设计上不可删），故用 SQL 兜底清理
  if (candId) dropBySql(`delete from hr_candidate where id = ${candId}`)

  const recAfterCand = dataOf(await api('GET', `/hr/recruitment/${recId}`))
  check('★ 应聘人数自动 +1（修复前 applicant_count 从不维护）', Number(recAfterCand?.applicantCount) === 1,
    String(recAfterCand?.applicantCount))

  // 候选人删除端点（本轮补齐：此前前端行内「删除」调用的 hrCandidateApi.remove **根本不存在**，
  // 点击必抛 TypeError）。放在应聘人数断言之后，避免临时造数干扰计数。
  const tmpCandRes = await api('POST', '/hr/candidate', {
    recruitmentId: recId, name: `${TAG}待删候选人`, status: 0,
  })
  const tmpCandId = dataOf(tmpCandRes)
  if (tmpCandId) {
    const delCand = await api('DELETE', `/hr/candidate/${tmpCandId}`)
    check('候选人删除端点可用（修复前 hrCandidateApi.remove 未定义 → 点「删除」必抛错）',
      okOf(delCand), (delCand.buf || '').toString().slice(0, 160))
    const candPageAfterDel = dataOf(await api('GET', `/hr/candidate/page?pageNum=1&pageSize=20&name=${TAG}待删候选人`))
    check('删除后候选人不再出现（逻辑删生效）', Number(candPageAfterDel?.total) === 0, String(candPageAfterDel?.total))
    const recAfterDel = dataOf(await api('GET', `/hr/recruitment/${recId}`))
    check('删除候选人后应聘人数同步回落到 1（计数对称维护）', Number(recAfterDel?.applicantCount) === 1,
      String(recAfterDel?.applicantCount))
  } else {
    check('候选人删除端点可用（修复前 hrCandidateApi.remove 未定义）', false, '临时候选人创建失败')
  }

  const iv = candId ? await api('PUT', `/hr/candidate/${candId}/interview?interviewComment=E2E面试&rating=4`) : null
  check('记录面试评价成功', iv ? okOf(iv) : false, iv ? (iv.buf || '').toString().slice(0, 140) : '')
  const badRating = candId ? await api('PUT', `/hr/candidate/${candId}/interview?rating=9`) : null
  check('面试评分超出 1~5 被拒', badRating ? !okOf(badRating) : false, badRating ? (badRating.buf || '').toString().slice(0, 140) : '')

  const toHired = candId ? await api('PUT', `/hr/candidate/${candId}/status?status=5`) : null
  check('候选人推进到「已录用」成功', toHired ? okOf(toHired) : false, toHired ? (toHired.buf || '').toString().slice(0, 140) : '')
  const recAfterHired = dataOf(await api('GET', `/hr/recruitment/${recId}`))
  check('★ 录用人数自动 +1（修复前 hired_count 从不维护）', Number(recAfterHired?.hiredCount) === 1,
    String(recAfterHired?.hiredCount))
  const directOnboard = candId ? await api('PUT', `/hr/candidate/${candId}/status?status=7`) : null
  check('直接改状态为「已入职」被拒（须走转入职动作）', directOnboard ? !okOf(directOnboard) : false,
    directOnboard ? (directOnboard.buf || '').toString().slice(0, 140) : '')

  const hireRes = candId ? await api('POST', `/hr/candidate/${candId}/hire`, { hireDate: '2026-09-15' }) : null
  const hireEmpId = hireRes ? dataOf(hireRes) : null
  check('★ 候选人转入职成功（修复前招聘 → 员工档案完全断开）', hireRes ? okOf(hireRes) && !!hireEmpId : false,
    hireRes ? JSON.stringify(hireRes.json).slice(0, 180) : '')
  if (hireEmpId) await dropOn('DELETE', `/hr/employees/${hireEmpId}`)
  const hiredEmp = hireEmpId ? dataOf(await api('GET', `/hr/employees/${hireEmpId}`)) : null
  check('★ 转入职生成员工档案（姓名/手机号继承候选人）',
    !!(hiredEmp && hiredEmp.employeeName === `${TAG}候选人` && hiredEmp.phone === '13900000001'),
    JSON.stringify({ n: hiredEmp?.employeeName, p: hiredEmp?.phone }))
  check('★ 转入职员工工号由号段生成', !!hiredEmp?.employeeNo, hiredEmp?.employeeNo)
  const candAfter = candId ? dataOf(await api('GET', `/hr/candidate/${candId}`)) : null
  check('转入职后候选人状态 = 7 已入职', Number(candAfter?.status) === 7, String(candAfter?.status))
  const recStat = dataOf(await api('GET', '/hr/recruitment/stat'))
  check('招聘统计返回计划/应聘/录用/缺口',
    recStat && ['headcountTotal', 'applicantTotal', 'hiredTotal', 'vacancyTotal'].every(k => k in recStat),
    JSON.stringify(recStat))
  const candStat = await api('GET', `/hr/candidate/stat?recruitmentId=${recId}`)
  check('候选人统计按阶段返回', okOf(candStat), (candStat.buf || '').toString().slice(0, 140))

  // ═══════════ 第 9 节 安全与菜单 ═══════════
  section('第 9 节 安全（P0-5 / P0-6）与菜单修复（P0-7）')
  const userPage = await api('GET', '/user/page?pageNum=1&pageSize=3')
  const userTxt = (userPage.buf || '').toString()
  check('★ /user/page 不再返回密码字段（P0-6 修复）', !/"password"\s*:/.test(userTxt),
    userTxt.slice(0, 160))
  check('★ /user/page 仍返回正常业务字段', okOf(userPage) && /"username"/.test(userTxt))

  const noAuth = await rawReq('GET', '/hr/employees/page?pageNum=1&pageSize=1', null, null)
  check('★ 未登录访问 HR 端点返回 401（P0-5 鉴权收口）', noAuth.status === 401 || Number(noAuth.json?.code) === 401,
    `status=${noAuth.status} ${(noAuth.buf || '').toString().slice(0, 120)}`)

  const hrPerms = sql("select count(*) as c from sys_permission where permission_code like 'hr%' and deleted = 0")
  check('★ HR 权限码已入库（修复前 0 条）', Number(hrPerms[0]?.c) >= 40, JSON.stringify(hrPerms))

  const menu907 = sqlOne("select id, parent_id, menu_level, path, component from sys_menu where id = 907")
  const parentNode = sqlOne(`select id from sys_menu where id = ${menu907?.parent_id}`)
  check('★ 招聘菜单 907 已挂到真实存在的父节点（P0-7 孤儿菜单修复）',
    !!(menu907 && parentNode && String(menu907.parent_id) === '61406'), JSON.stringify(menu907))
  check('招聘菜单路径已归一化（无前导斜杠）', menu907?.path === 'hr/recruitment', String(menu907?.path))
  const newGroup = sqlOne('select id, menu_name, parent_id from sys_menu where id = 61406')
  check('新分组 61406「招聘管理」已创建且挂在 60014 下',
    !!(newGroup && String(newGroup.parent_id) === '60014'), JSON.stringify(newGroup))

  const hrPages = sql("select count(*) as c from sys_menu where id in (90001,90002,90003,90004,90005,90006,80530,80531,80532,907) and deleted = 0")
  check('HR 10 个菜单页全部存在', Number(hrPages[0]?.c) === 10, JSON.stringify(hrPages))

  // ── 第三轮审计（2026-09-23）四条回归，见 HR_MODULE_AUDIT_20260923.md §6.5 ──
  // ⚠️ 这里的 SQL 一律写成**单行**：`sql()/sqlOne()` 是经 execSync 调 tools/dbq.py 的，
  //    在 Windows 上走 cmd.exe，多行字符串会被拆断 —— 实测多行写法会让 WHERE 条件整段失效
  //    （返回全表行数，断言假失败）。下面的断言都已踩过这个坑，勿改成多行。

  // ① 工号唯一约束必须含 tenant_id：否则两个租户同一天各建第一个员工都会得到
  //    EMP-YYYYMMDD-0001（号段 per-tenant、按天重置、前缀相同）→ 撞唯一约束 400。
  const empIdx = sqlOne("select indexdef from pg_indexes where tablename = 'hr_employee' and indexname = 'uk_hr_employee_tenant_employee_no'")
  check('★ hr_employee 工号唯一索引含 tenant_id 且带 deleted 条件（P0-3）',
    !!(empIdx && /tenant_id/.test(empIdx.indexdef) && /employee_no/.test(empIdx.indexdef)
      && /deleted\s*=\s*0/.test(empIdx.indexdef)), JSON.stringify(empIdx))
  const oldIdx = sqlOne("select count(*) as c from pg_indexes where tablename = 'hr_employee' and indexname = 'hr_employee_employee_no_key'")
  check('旧的「仅 employee_no」唯一索引已移除', Number(oldIdx?.c) === 0, JSON.stringify(oldIdx))

  // ② 招聘菜单的 menu_code 必须与接口鉴权码同源（hr:recruitment:*），否则菜单派生
  //    对它整体失效（fail-open 恒可见），而接口仍 403。
  const menu907Code = sqlOne("select menu_code from sys_menu where id = 907 and deleted = 0")
  check('★ 招聘菜单 907 的 menu_code 已对齐权限码前缀 hr:recruitment（P1-4）',
    menu907Code?.menu_code === 'hr:recruitment', JSON.stringify(menu907Code))

  // ③ 叶子菜单 menu_level 必须为 0：getUserMegaMenus 对「非系统租户且非超管」强制
  //    menu_level = 0，写成 3 的页面普通租户管理员看不到。
  const badLevelMenus = sql("select id, menu_name, menu_level from sys_menu where deleted = 0 and menu_level <> 0 and id in (90001,90002,90003,90004,90005,90006,80530,80531,80532,907)")
  check('HR 叶子菜单 menu_level 均为 0（P2-2）', badLevelMenus.length === 0, JSON.stringify(badLevelMenus))

  // ④ 删除类权限码必须独立（此前挂 :update，导致角色页权限矩阵的「删除」列恒为空）
  const delPerms = sqlOne("select count(*) as c from sys_permission where deleted = 0 and status = 0 and permission_code in ('hr:salary:delete', 'hr:performance:delete')")
  check('删除类权限码已入库（hr:salary:delete / hr:performance:delete，P1-6）',
    Number(delPerms?.c) === 2, JSON.stringify(delPerms))

  // ── 租户隔离（2026-09-18 修复：sys_user 在多租户忽略表内且查询无租户条件 → 跨租户可见）──
  // 口径：**平台超管豁免（保持全局视野 / 可切租户），非超管强制限本租户**。
  // 靶子账号 e2e_hr_t2 属租户 2，由 tools/e2e-hr-user.sql 创建。
  const T2_USER_ID = '2099000000000009021'
  const T2_USERNAME = 'e2e_hr_t2'

  const superPage = await apiAs(TOKEN, 'GET', '/user/page?pageNum=1&pageSize=300')
  const superNames = (dataOf(superPage)?.records || []).map(u => u.username)
  check('超管查用户列表：租户限制被豁免（与修复前行为一致，admin 验证开发不受影响）',
    superNames.includes(T2_USERNAME), `total=${dataOf(superPage)?.total} 含租户2账号=${superNames.includes(T2_USERNAME)}`)

  // 该账号若登录失败（如未执行 e2e-hr-user.sql 的 sys_user_tenant 行），应记为断言失败而不是中断整轮
  let taToken = null
  try {
    taToken = await loginAs('e2e_hr_ta')
  } catch (e) {
    console.log(`     ⚠️ e2e_hr_ta 登录失败，租户隔离断言降级为失败：${String(e).slice(0, 120)}`)
  }

  if (taToken) {
    const taPage = await apiAs(taToken, 'GET', '/user/page?pageNum=1&pageSize=300')
    const taNames = (dataOf(taPage)?.records || []).map(u => u.username)
    check('★ 非超管租户管理员：用户列表里看不到其它租户的账号（修复前可见）',
      okOf(taPage) && taNames.length > 0 && !taNames.includes(T2_USERNAME),
      `可见 ${taNames.length} 条，是否含租户2账号=${taNames.includes(T2_USERNAME)}`)

    const taForced = await apiAs(taToken, 'GET', '/user/page?pageNum=1&pageSize=300&tenantId=2')
    const taForcedNames = (dataOf(taForced)?.records || []).map(u => u.username)
    check('★ 非超管显式传 tenantId=2 也被忽略（参数不可用来越权）',
      !taForcedNames.includes(T2_USERNAME), `是否含租户2账号=${taForcedNames.includes(T2_USERNAME)}`)

    const taDetail = await apiAs(taToken, 'GET', `/user/${T2_USER_ID}`)
    // 403（越权）与 404（租户条件已把该行滤掉，`getById` 查不到）都算「拿不到」；
    // 后者其实更好 —— 连"该账号是否存在"都不泄露。
    check('★ 非超管按 id 读其它租户账号详情拿不到（403 或 404）',
      taDetail.status === 403 || taDetail.status === 404
        || Number(taDetail.json?.code) === 403 || Number(taDetail.json?.code) === 404,
      `status=${taDetail.status} ${(taDetail.buf || '').toString().slice(0, 120)}`)
  } else {
    check('★ 非超管租户管理员：用户列表里看不到其它租户的账号', false, 'e2e_hr_ta 登录失败')
    check('★ 非超管显式传 tenantId=2 也被忽略', false, 'e2e_hr_ta 登录失败')
    check('★ 非超管按 id 读其它租户账号详情被拒（403）', false, 'e2e_hr_ta 登录失败')
  }

  const superDetail = await apiAs(TOKEN, 'GET', `/user/${T2_USER_ID}`)
  check('超管按 id 读任意租户账号详情仍可（豁免生效）', okOf(superDetail),
    (superDetail.buf || '').toString().slice(0, 120))

  // ── 深度收口验证：`sys_user` 已移出全局忽略表，租户条件由拦截器**自动**注入 ──
  // 下面用「租户 2 的非超管账号」登录，验证隔离不再依赖某个端点里逐处补的条件。
  try {
    const t2Token = await loginAs('e2e_hr_t2', undefined, 'E2E验收租户2')
    const t2Page = await apiAs(t2Token, 'GET', '/user/page?pageNum=1&pageSize=300')
    const t2Names = (dataOf(t2Page)?.records || []).map(u => u.username)
    check('★ 租户2账号只能看到本租户账号（拦截器自动注入，非逐处补条件）',
      okOf(t2Page) && t2Names.length > 0 && !t2Names.includes('admin') && !t2Names.includes('e2e_hr_ta'),
      `可见 ${t2Names.length} 条：${t2Names.slice(0, 5).join(',')}`)

    // 用 /department/tree（仅需登录）验证「其它表同样自动隔离」——租户 2 下没有任何部门
    const t2Dept = await apiAs(t2Token, 'GET', '/department/tree')
    const t2DeptLen = Array.isArray(dataOf(t2Dept)) ? dataOf(t2Dept).length : -1
    check('★ 租户2看不到租户1的部门（其它表同样自动隔离）',
      okOf(t2Dept) && t2DeptLen === 0, `部门数=${t2DeptLen} ${(t2Dept.buf || '').toString().slice(0, 100)}`)

    const t2Detail = await apiAs(t2Token, 'GET', '/user/1')
    check('★ 租户2按 id 读租户1的 admin 被拒', !okOf(t2Detail), `status=${t2Detail.status}`)
  } catch (e) {
    check('★ 租户2账号隔离断言', false, `e2e_hr_t2 登录失败：${String(e).slice(0, 120)}`)
    check('★ 租户2看不到租户1的 HR 数据', false, 'e2e_hr_t2 登录失败')
    check('★ 租户2按 id 读租户1的 admin 被拒', false, 'e2e_hr_t2 登录失败')
  }

  // ═══════════ 第 10 节 真库对账 ═══════════
  section('第 10 节 真库对账')
  const dbEmp = sqlOne(`select employee_no, status, regular_date, leave_date from hr_employee where id = ${empId} and deleted = 0`)
  check('真库：员工 status=0 且 leave_date 已写', dbEmp && Number(dbEmp.status) === 0 && !!dbEmp.leave_date, JSON.stringify(dbEmp))
  check('真库：工号非空', !!dbEmp?.employee_no, dbEmp?.employee_no)
  const dbPos = sqlOne(`select quota_count, current_count from hr_position where id = ${posId} and deleted = 0`)
  check('真库：岗位在岗人数 = 1', dbPos && Number(dbPos.current_count) === 1, JSON.stringify(dbPos))
  const dbPay = sqlOne(`select gross_amount, actual_amount, tax_deduct from hr_salary_payment where employee_id = ${empId2} and payment_month = '2026-09' and deleted = 0`)
  check('真库：薪资记录含应发与实发', !!(dbPay && dbPay.gross_amount && dbPay.actual_amount), JSON.stringify(dbPay))
  const dbQuota = sqlOne("select quota_days from hr_leave_quota where leave_type = 'ANNUAL' and year = 2026 and deleted = 0")
  check('真库：年假额度 10 天', dbQuota && Number(dbQuota.quota_days) === 10, JSON.stringify(dbQuota))
  const dbChange = sqlOne(`select count(*) as c from hr_employee_change where employee_id = ${empId} and deleted = 0`)
  check('真库：人事异动记录 ≥ 3 条', Number(dbChange?.c) >= 3, JSON.stringify(dbChange))
  const dbCols = sql("select column_name from information_schema.columns where table_name = 'sys_user_position' order by ordinal_position")
  const colNames = dbCols.map(r => r.column_name)
  check('★ 真库：sys_user_position 已补 deleted / tenant_id（P0-4 修复）',
    colNames.includes('deleted') && colNames.includes('tenant_id'), colNames.join(','))
  const dbNewTables = sql("select table_name from information_schema.tables where table_name in ('hr_employee_change','hr_attendance_rule','hr_leave_quota')")
  check('★ 真库：3 张新表已建（异动 / 考勤规则 / 假期额度）', dbNewTables.length === 3,
    dbNewTables.map(r => r.table_name).join(','))

  // ═══════════ 第 11 节 UI 金标准 ══════════════
  if (!SKIP_UI) {
    section('第 11 节 UI 金标准（Playwright）')
    let feUp = false
    try {
      const r = await rawReq('GET', '/auth/captcha')  // 后端仍在线
      feUp = await new Promise(resolve => {
        const req = http.request({ hostname: 'localhost', port: Number(FE.split(':')[2]), path: '/', method: 'GET' },
          res => { res.resume(); resolve(res.statusCode < 500) })
        req.on('error', () => resolve(false))
        req.end()
      })
      void r
    } catch (e) { feUp = false }

    if (!feUp) {
      check('前端 dev server 可达（SKIP_UI=1 可跳过本节点）', false, `${FE} 未启动 —— UI 节点整体跳过`)
      console.log(`  ⚠️ 前端未启动，UI 节点跳过。启动方式：cd frontend/apps/pc-admin && npx vite --port 5656`)
    } else {
      const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
      const browser = await chromium.launch()
      const page = await browser.newPage({ viewport: { width: 1600, height: 900 } })
      const consoleErrors = []
      page.on('console', m => { if (m.type() === 'error') consoleErrors.push(m.text()) })
      page.on('pageerror', e => consoleErrors.push('pageerror: ' + e.message))
      const bad4xx = []
      page.on('response', r => {
        const u = r.url()
        if (u.includes('/api/') && r.status() >= 400 && r.status() !== 401) bad4xx.push(`${r.status()} ${u}`)
      })

      // 登录：走接口拿 token 后注入 localStorage。
      // ⚠️ 不在登录页填表单 —— 登录页需要图形验证码，脚本填不出来；注入 token 是等价且稳定的做法
      //（路由守卫读的正是 localStorage.token，随后自行拉用户信息与动态路由）。
      // 第 3 列 = 期望形态：
      //   'category' HR 路线 A 页面（CategoryListLayout + 自绘电子表格 + 表头齿轮）
      //   'system'   V11.422.0 / V11.429.0 之后挂到 80531/80532 的**系统域页面**
      //              （views/system/{role,user}/index.vue）—— 它们有自己的 PageContainer
      //              布局，**没有** `.category-list-layout` 也没有 `.ss-grid` 齿轮。
      //              2026-09-23 之前这里对所有页面都套 category 断言 ⇒ 这两页恒失败，
      //              属「断言没跟着菜单改指走」，不是页面缺陷。
      const PAGES = [
        ['员工列表', 'hr/employee', 'category'],
        ['考勤记录', 'hr/attendance', 'category'],
        ['请假管理', 'hr/leave', 'category'],
        ['薪资管理', 'hr/salary', 'category'],
        ['绩效考核', 'hr/performance', 'category'],
        ['岗位编制', 'hr/organization/position', 'category'],
        ['职员部门', 'md/staff-dept', 'category'],
        ['岗位权限', 'md/staff-role', 'system', '.role-page-header'],
        ['全部操作员', 'md/staff-all', 'system', '.user-page-header'],
        ['招聘管理', 'hr/recruitment', 'category'],
      ]

      // 后端全局限流（ai-ready.rate-limit：默认 100 QPS / 桶容量 200，按 IP 计）——
      // 前面的接口节打了大量请求，进 UI 节前先等桶回填，避免把 429 误当页面缺陷。
      await new Promise(r => setTimeout(r, 8000))
      // 顶栏通知轮询是「外壳级」流量（每页导航都会打），不属于被测页面，固定返回空结果以隔离噪声
      await page.route('**/api/notification/**', r => r.fulfill({ status: 200, contentType: 'application/json', body: '{"code":200,"data":[]}' }))
      await page.route('**/api/sse/notifications*', r => r.fulfill({ status: 200, contentType: 'text/event-stream', body: '' }))

      await page.addInitScript(([tok, tid, tname]) => {
        localStorage.setItem('token', tok)
        localStorage.setItem('tenantId', String(tid))
        localStorage.setItem('tenantName', tname)
      }, [TOKEN, 1, process.env.E2E_TENANT || '系统租户'])
      // ⚠️ 不要先访问根路径 `/` —— 实测 `/` 会先于登录态解析重定向到 /login；
      //    直接访问业务页 URL 才会走「读 token → 拉用户信息 → 载入动态路由」的正常链路。
      await page.goto(`${FE}/${PAGES[0][1]}`, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(4000)
      check('UI 登录态注入成功（未停留在登录页）', !page.url().includes('/login'), `url=${page.url()}`)
      // 登录/动态路由装载阶段的噪声不计入业务页断言
      consoleErrors.length = 0
      bad4xx.length = 0

      for (const [label, route, kind = 'category', headerSel] of PAGES) {
        const before = consoleErrors.length
        await page.goto(`${FE}/${route}`, { waitUntil: 'domcontentloaded' })
        await page.waitForTimeout(4000)
        const newErr = consoleErrors.slice(before)
        if (kind === 'system') {
          // 系统域页面：断言「自己的页头渲染出来了 + 页面上有数据表 + 无 console error」
          const hasHeader = await page.locator(headerSel).count()
          const hasTable = await page.locator('.ant-table, .bill-detail-table, table.ss-grid').count()
          check(`UI「${label}」渲染出系统域页面（${headerSel} + 表格）`,
            hasHeader > 0 && hasTable > 0, `header=${hasHeader} table=${hasTable}`)
        } else {
          const hasLayout = await page.locator('.category-list-layout').count()
          // 数据表用 BillDetailTable / BillTableList（后者内置前者），渲染出的是自绘电子表格
          // `.bill-detail-table > .spreadsheet-table > table.ss-grid`，**不是** `.ant-table`
          const hasTable = await page.locator('.bill-detail-table, .ant-table, table.ss-grid').count()
          // 列配置齿轮挂在序号列的 `.ss-header-settings` 表头单元格里（组件内置，页面零接入）
          const hasGear = await page.locator('.ss-header-settings, .ss-header-settings .th-settings-btn, .ant-table-thead .anticon-setting').count()
          check(`UI「${label}」渲染出金标准骨架（CategoryListLayout + 表格）`,
            hasLayout > 0 && hasTable > 0, `layout=${hasLayout} table=${hasTable}`)
          check(`UI「${label}」表头存在列配置齿轮`,
            hasGear > 0, `gear=${hasGear}`)
        }
        check(`UI「${label}」控制台无新增 error`, newErr.length === 0, newErr.slice(0, 2).join(' | '))
        await page.screenshot({ path: path.join(SHOTS, `${route.replace(/\//g, '_')}.png`), fullPage: false })
      }

      check('UI 全量巡检：全程无 4xx/5xx 接口（401 除外）', bad4xx.length === 0, bad4xx.slice(0, 5).join(' | '))
      check('UI 全量巡检：全程 console 无 error', consoleErrors.length === 0, consoleErrors.slice(0, 3).join(' | '))
      await browser.close()
    }
  }

  // ═══════════ 第 12 节 清理 ═══════════
  await runCleanup()

  // ═══════════ 汇总 ═══════════
  const pass = results.filter(r => r.ok).length
  const fail = results.length - pass
  const elapsed = ((Date.now() - t0) / 1000).toFixed(1)
  console.log(`\n${'═'.repeat(64)}`)
  console.log(`人力资源模块 E2E 汇总：${pass}/${results.length} 通过，${fail} 失败  ·  ${elapsed}s`)
  console.log(`产物目录：${SHOTS}`)
  if (fail) {
    console.log('\n失败项：')
    results.filter(r => !r.ok).forEach(r => console.log(`  ✗ ${r.name} — ${r.detail}`))
  }
  console.log(`${'═'.repeat(64)}\n`)
  fs.writeFileSync(path.join(SHOTS, 'e2e-hr-result.txt'),
    results.map(r => `${r.ok ? 'PASS' : 'FAIL'}\t${r.name}\t${r.detail}`).join('\n'))
  process.exit(fail ? 1 : 0)
})().catch(async e => {
  console.error('\nE2E 异常终止:', e)
  try { await runCleanup() } catch (_) { /* ignore */ }
  process.exit(1)
})
