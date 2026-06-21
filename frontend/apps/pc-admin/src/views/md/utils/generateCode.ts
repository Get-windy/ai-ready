import { getCurrentLocale } from '@/locales'
import { generateCode, generateCodeDemo } from '@/utils/codeGenerator'
import request from '@/utils/request'

/** 编号前缀映射：中文租户用拼音首字母，英文租户用英文缩写 */
const PREFIX_CN: Record<string, string> = {
  customer: 'KH',
  supplier: 'GYS',
  logistics: 'WLGS',
  partner: 'WLDW',
}

const PREFIX_EN: Record<string, string> = {
  customer: 'CUS',
  supplier: 'SUP',
  logistics: 'LOG',
  partner: 'PTR',
}

/** 根据类型获取对应前缀 */
function getPrefix(type: 'customer' | 'supplier' | 'logistics' | 'partner'): string {
  const locale = getCurrentLocale()
  const isChinese = locale === 'zh-CN' || locale.startsWith('zh')
  return isChinese ? PREFIX_CN[type] : PREFIX_EN[type]
}

/**
 * 生成往来单位编号（同步版，仅用于演示）
 * 格式：前缀-8位年月日 + N位序列号（自动扩展）
 */
export function generatePartnerCode(
  type: 'customer' | 'supplier' | 'logistics' | 'partner',
  seq?: number,
): string {
  const prefix = getPrefix(type)
  if (seq !== undefined) {
    return generateCode(prefix, seq)
  }
  return generateCodeDemo(prefix)
}

/**
 * 生成往来单位编号（异步版，从后端获取当日最大序号+1）
 * 推荐在表单 onMounted 中使用
 */
export async function generatePartnerCodeAsync(
  type: 'customer' | 'supplier' | 'logistics' | 'partner',
): Promise<string> {
  const prefix = getPrefix(type)
  try {
    const res = await request.get('/api/erp/partner/next-seq', { params: { prefix } })
    const seq = (res as any)?.seq ?? (res as any)?.data?.seq ?? 1
    return generateCode(prefix, seq)
  } catch {
    // 后端不可用时降级为演示模式
    return generateCodeDemo(prefix)
  }
}
