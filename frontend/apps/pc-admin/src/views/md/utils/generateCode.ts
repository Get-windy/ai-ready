import { getCurrentLocale } from '@/locales'
import { generateCode, generateCodeDemo } from '@/utils/codeGenerator'
import request from '@/utils/request'

/** 往来单位编号类型（物流公司单列，见下方 generateLogisticsCodeAsync） */
type PartnerCodeType = 'customer' | 'supplier' | 'partner'

/** 编号前缀映射：中文租户用拼音首字母，英文租户用英文缩写 */
const PREFIX_CN: Record<PartnerCodeType, string> = {
  customer: 'KH',
  supplier: 'GYS',
  partner: 'WLDW',
}

const PREFIX_EN: Record<PartnerCodeType, string> = {
  customer: 'CUS',
  supplier: 'SUP',
  partner: 'PTR',
}

/** 根据类型获取对应前缀 */
function getPrefix(type: PartnerCodeType): string {
  const locale = getCurrentLocale()
  const isChinese = locale === 'zh-CN' || locale.startsWith('zh')
  return isChinese ? PREFIX_CN[type] : PREFIX_EN[type]
}

/**
 * 生成往来单位编号（同步版，仅用于演示）
 * 格式：前缀-8位年月日 + N位序列号（自动扩展）
 */
export function generatePartnerCode(type: PartnerCodeType, seq?: number): string {
  const prefix = getPrefix(type)
  if (seq !== undefined) {
    return generateCode(prefix, seq)
  }
  return generateCodeDemo(prefix)
}

/** 从后端取号（当日最大序号 +1），失败返回 1 */
async function fetchNextSeq(prefix: string): Promise<number> {
  try {
    const res: any = await request.get('/erp/md/customer/next-seq', { params: { prefix } })
    return Number((res as any)?.seq ?? (res as any)?.data?.seq ?? 1) || 1
  } catch {
    return 1
  }
}

/**
 * 生成往来单位编号（异步版，从后端获取当日最大序号+1）
 * 推荐在表单 onMounted 中使用
 */
export async function generatePartnerCodeAsync(type: PartnerCodeType): Promise<string> {
  const prefix = getPrefix(type)
  const seq = await fetchNextSeq(prefix)
  return generateCode(prefix, seq)
}

/**
 * 生成物流公司编号（异步版）。
 *
 * ⚠️ 物流公司**不套用**上面的「前缀-YYYYMMDD-序号」口径，这是有据可依的有意差异：
 * 《物流公司开发文档》L186/L229 记录了对标实测结果 —— 编号为 `WuLiu` + 3 位补零
 * （如 `WuLiu069`），并明确否定了初稿猜测的 `WLGS`/`LOG` 前缀。
 * 相应地，PREFIX_CN/PREFIX_EN 里不再保留物流项（2026-09-26 清理）。
 */
export async function generateLogisticsCodeAsync(): Promise<string> {
  const prefix = 'WuLiu'
  const seq = await fetchNextSeq(prefix)
  return prefix + String(seq).padStart(3, '0')
}
