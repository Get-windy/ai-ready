/**
 * 通用业务编号生成器
 *
 * 格式：前缀 + "-" + 8位年月日 + N位序列号（N>=3，自动扩展，永不溢出）
 * 示例：XSDD-20260621001, KH-20260621001
 *
 * 序列号递进规则（逐位用字母替换数字）：
 *   以3位为例：
 *   阶段1: 001~999           → 纯数字             (999个)
 *   阶段2: A01~Z99           → 第1位字母+后2位数字   (26×99=2574个)
 *   阶段3: AA1~ZZ9           → 前2位字母+第3位数字   (26²×9=6084个)
 *   阶段4: AAA~ZZZ           → 全字母              (26³=17576个)
 *   用完后自动扩展为4位：0001~9999 → A001~Z999 → ... → AAAA~ZZZZ
 *   以此类推，永不溢出
 *
 * 使用方式：
 *   import { generateCode } from '@/utils/codeGenerator'
 *   generateCode('XSDD')  // 销售订单：XSDD-20260621001
 *   generateCode('KH')    // 客户编号：KH-20260621001
 *   generateCode('PO')    // 采购订单：PO-20260621001
 *
 *   // 异步版本（从后端获取当日最大序号）
 *   const code = await generateCodeAsync('XSDD', '/api/sales/max-seq')
 */

/**
 * 计算宽度 w 下的序列号总容量
 * 阶段 k (1~w+1): 26^(k-1) × (10^(w-k+1) - 1)
 */
function totalCapacity(w: number): number {
  let total = 0
  for (let k = 1; k <= w + 1; k++) {
    total += Math.pow(26, k - 1) * (Math.pow(10, w - k + 1) - 1)
  }
  return total
}

/**
 * 将序列号编码为渐进式字母-数字字符串
 * 自动确定最小宽度（>=3），永不溢出
 */
function encodeSequence(seq: number): string {
  // 第一步：确定所需宽度（最小3位）
  let w = 3
  while (seq > totalCapacity(w)) {
    w++
  }

  // 第二步：在当前宽度内找到所在阶段
  let remaining = seq
  let stageK = 1
  while (stageK <= w + 1) {
    const stageCapacity = Math.pow(26, stageK - 1) * (Math.pow(10, w - stageK + 1) - 1)
    if (remaining <= stageCapacity) break
    remaining -= stageCapacity
    stageK++
  }

  // 第三步：编码
  const numLetters = stageK - 1   // 字母位数
  const numDigits = w - numLetters // 数字位数
  const maxNum = Math.pow(10, numDigits) - 1

  if (numDigits === 0) {
    // 全字母阶段（如 AAA~ZZZ）
    const letterIndex = remaining - 1
    let result = ''
    let idx = letterIndex
    for (let i = 0; i < w; i++) {
      result += String.fromCharCode(65 + (idx % 26))
      idx = Math.floor(idx / 26)
    }
    return result.split('').reverse().join('')
  }

  // 混合阶段：字母部分 + 数字部分
  const offset = remaining - 1
  const numPart = (offset % maxNum) + 1           // 数字部分的值 (1 ~ maxNum)
  const letterIndex = Math.floor(offset / maxNum)  // 字母组合索引 (0-based)

  // 生成字母部分
  let letters = ''
  if (numLetters > 0) {
    let idx = letterIndex
    for (let i = 0; i < numLetters; i++) {
      letters += String.fromCharCode(65 + (idx % 26))
      idx = Math.floor(idx / 26)
    }
    letters = letters.split('').reverse().join('') // 高位在前
  }

  // 生成数字部分（前补零）
  const numStr = String(numPart).padStart(numDigits, '0')

  return letters + numStr
}

/**
 * 生成当前日期字符串（8位：YYYYMMDD）
 */
function getTodayStr(): string {
  const now = new Date()
  const yyyy = now.getFullYear()
  const mm = String(now.getMonth() + 1).padStart(2, '0')
  const dd = String(now.getDate()).padStart(2, '0')
  return `${yyyy}${mm}${dd}`
}

/**
 * 通用业务编号生成（同步版，前端生成序列号）
 * @param prefix 编号前缀，如 'XSDD', 'KH', 'PO', 'CGDD'
 * @param seq    序列号（1-based），如从后端获取当日最大值+1
 * @returns 完整编号，如 XSDD-20260621001
 */
export function generateCode(prefix: string, seq: number): string {
  return `${prefix}-${getTodayStr()}${encodeSequence(seq)}`
}

/**
 * 通用业务编号生成（异步版，从后端获取当日最大序列号+1）
 * @param prefix  编号前缀
 * @param apiPath 后端接口路径，返回 { seq: number }
 * @returns 完整编号
 */
export async function generateCodeAsync(prefix: string, apiPath: string): Promise<string> {
  const { default: request } = await import('@/utils/request')
  try {
    const res = await request.get(apiPath, { params: { prefix, date: getTodayStr() } })
    const seq = res?.seq ?? res?.data?.seq ?? 1
    return generateCode(prefix, seq)
  } catch {
    // 后端不可用时降级为随机序号
    const seq = Math.floor(Math.random() * 999) + 1
    return generateCode(prefix, seq)
  }
}

/**
 * 通用业务编号生成（随机序列号，仅用于演示/预览）
 * @param prefix 编号前缀
 * @returns 完整编号（序列号随机）
 */
export function generateCodeDemo(prefix: string): string {
  const seq = Math.floor(Math.random() * 2000) + 1
  return generateCode(prefix, seq)
}
