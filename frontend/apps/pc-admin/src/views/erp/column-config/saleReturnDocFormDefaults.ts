/**
 * 销售退货单 · 表单页字段配置默认值（共享，供 form.vue 与 SaleReturnDocFormConfig.vue 同时引用）
 *
 * 为什么单独抽文件：页面配置弹窗的「默认勾选」必须与表单页首屏实际渲染的字段完全一致，
 * 否则会出现「配置里没勾的字段却显示 / 勾了的字段不显示」以及「无存档时回退成显示全部字段」。
 * 两处共用一份默认值即可杜绝口径漂移。
 */

export interface SaleReturnDocPageField {
  key: string
  label: string
  displayName: string
  visible: boolean
  enterJump: boolean
}

/** localStorage 存储键（form.vue 与配置弹窗必须一致） */
export const SRD_FORM_CONFIG_KEY = 'sale-return-doc-form-page-config'
export const SRD_FORM_DEFAULTS_KEY = 'sale-return-doc-form-default-config'
export const SRD_FORM_PRINT_KEY = 'sale-return-doc-form-print-config'

/** 页面配置 53 字段（与开发文档「新增销售退货单表单页配置」列表一一对应） */
export const SALE_RETURN_DOC_PAGE_FIELDS: SaleReturnDocPageField[] = [
  { key: 'returnDocNo', label: '编号', displayName: '编号', visible: true, enterJump: false },
  { key: 'customerId', label: '客户', displayName: '客户', visible: true, enterJump: true },
  { key: 'customerCode', label: '客户编号', displayName: '客户编号', visible: false, enterJump: false },
  { key: 'bankName', label: '开户行', displayName: '开户行', visible: false, enterJump: false },
  { key: 'bankAccount', label: '银行账号', displayName: '银行账号', visible: false, enterJump: false },
  { key: 'taxNo', label: '税号', displayName: '税号', visible: false, enterJump: false },
  { key: 'warehouseId', label: '入库仓库', displayName: '入库仓库', visible: true, enterJump: true },
  { key: 'handlerId', label: '经手人', displayName: '经手人', visible: true, enterJump: true },
  { key: 'deptId', label: '部门', displayName: '部门', visible: false, enterJump: false },
  { key: 'orderDate', label: '单据日期', displayName: '单据日期', visible: true, enterJump: true },
  { key: 'salesType', label: '销售类型', displayName: '销售类型', visible: true, enterJump: true },
  { key: 'contactName', label: '联系人', displayName: '联系人', visible: true, enterJump: false },
  { key: 'contactPhone', label: '联系电话', displayName: '联系电话', visible: true, enterJump: false },
  { key: 'shippingAddress', label: '收货地址', displayName: '收货地址', visible: true, enterJump: false },
  { key: 'extNum1', label: '自定义字段1(数字)', displayName: '自定义字段1(数字)', visible: false, enterJump: false },
  { key: 'extNum2', label: '自定义字段2(数字)', displayName: '自定义字段2(数字)', visible: false, enterJump: false },
  { key: 'extText1', label: '自定义字段3(文本)', displayName: '自定义字段3(文本)', visible: false, enterJump: false },
  { key: 'extText2', label: '自定义字段4(文本)', displayName: '自定义字段4(文本)', visible: false, enterJump: false },
  { key: 'extText3', label: '自定义字段5(文本)', displayName: '自定义字段5(文本)', visible: false, enterJump: false },
  { key: 'summary', label: '摘要', displayName: '摘要', visible: false, enterJump: false },
  { key: 'paymentAccount1', label: '付款账户', displayName: '付款账户', visible: true, enterJump: false },
  { key: 'settledAmount', label: '付款金额', displayName: '付款金额', visible: true, enterJump: false },
  { key: 'paymentAccount2', label: '更多账户', displayName: '更多账户', visible: true, enterJump: false },
  { key: 'prevAdvance', label: '此前预收', displayName: '此前预收', visible: true, enterJump: false },
  { key: 'returnAdvance', label: '退回预收款', displayName: '退回预收款', visible: true, enterJump: false },
  { key: 'availableAdvance', label: '可用预收', displayName: '可用预收', visible: true, enterJump: false },
  { key: 'advanceBalance', label: '预收余额', displayName: '预收余额', visible: false, enterJump: false },
  { key: 'receivableReduce', label: '应收款减少', displayName: '应收款减少', visible: false, enterJump: false },
  { key: 'creditLimit', label: '信用额度', displayName: '信用额度', visible: false, enterJump: false },
  { key: 'availableCredit', label: '可用额度', displayName: '可用额度', visible: false, enterJump: false },
  { key: 'prevDebt', label: '此前欠款', displayName: '此前欠款', visible: false, enterJump: false },
  { key: 'currentDebt', label: '本次欠款', displayName: '本次欠款', visible: false, enterJump: false },
  { key: 'debtBalance', label: '欠款余额', displayName: '欠款余额', visible: false, enterJump: false },
  { key: 'collectionDeadline', label: '收款期限', displayName: '收款期限', visible: false, enterJump: false },
  { key: 'returnApplyId', label: '退货申请', displayName: '退货申请', visible: true, enterJump: false },
  { key: 'sourceOrder', label: '源单', displayName: '源单', visible: false, enterJump: false },
  { key: 'deliveryMethod', label: '配送方式', displayName: '配送方式', visible: true, enterJump: false },
  { key: 'deliveryRoute', label: '配送线路', displayName: '配送线路', visible: false, enterJump: false },
  { key: 'logisticsCompany', label: '物流公司', displayName: '物流公司', visible: true, enterJump: false },
  { key: 'freightPayer', label: '运费承担方', displayName: '运费承担方', visible: false, enterJump: false },
  { key: 'shippingFee', label: '运费', displayName: '运费', visible: false, enterJump: false },
  { key: 'waybillNo', label: '运单号', displayName: '运单号', visible: false, enterJump: false },
  { key: 'deliveryNo', label: '配送单', displayName: '配送单', visible: false, enterJump: false },
  { key: 'memberCardNo', label: '会员卡号', displayName: '会员卡号', visible: true, enterJump: false },
  { key: 'prevPoints', label: '此前积分', displayName: '此前积分', visible: false, enterJump: false },
  { key: 'memberGeneratedPoints', label: '产生积分', displayName: '产生积分', visible: false, enterJump: false },
  { key: 'memberExchangePoints', label: '兑换积分', displayName: '兑换积分', visible: false, enterJump: false },
  { key: 'memberUsedPoints', label: '使用积分', displayName: '使用积分', visible: false, enterJump: false },
  { key: 'currentPoints', label: '剩余积分', displayName: '剩余积分', visible: false, enterJump: false },
  { key: 'remark', label: '单据备注', displayName: '单据备注', visible: true, enterJump: false },
  { key: 'creatorName', label: '制单人', displayName: '制单人', visible: true, enterJump: false },
  { key: 'createTime', label: '制单时间', displayName: '制单时间', visible: true, enterJump: false },
  { key: 'printCount', label: '打印次数', displayName: '打印次数', visible: true, enterJump: false },
  { key: 'billAmount', label: '本单金额', displayName: '本单金额', visible: true, enterJump: false },
]

/** 归底部区域渲染的字段：收款 Tab / 物流 Tab / 会员 Tab / 备注区 / 单据信息行 —— 不进头部字段区 */
export const SRD_BOTTOM_AREA_KEYS = new Set([
  // 收款 Tab
  'paymentAccount1', 'settledAmount', 'paymentAccount2', 'prevAdvance', 'returnAdvance',
  'availableAdvance', 'advanceBalance', 'receivableReduce', 'creditLimit', 'availableCredit',
  'prevDebt', 'currentDebt', 'debtBalance', 'collectionDeadline',
  // 物流 Tab
  'deliveryMethod', 'deliveryRoute', 'logisticsCompany', 'freightPayer', 'shippingFee', 'waybillNo', 'deliveryNo',
  // 会员 Tab
  'memberCardNo', 'prevPoints', 'memberGeneratedPoints', 'memberExchangePoints', 'memberUsedPoints', 'currentPoints',
  // 备注区 / 单据信息行 / 页脚本单金额
  'remark', 'creatorName', 'createTime', 'printCount', 'billAmount',
])

/** 头部默认可见字段（无存档时的首屏口径，与页面配置默认勾选完全一致） */
export const SRD_DEFAULT_VISIBLE_HEAD_KEYS = SALE_RETURN_DOC_PAGE_FIELDS
  .filter(f => f.visible && !SRD_BOTTOM_AREA_KEYS.has(f.key))
  .map(f => f.key)

/**
 * 读取生效的字段显隐配置：
 * - 有存档 → 存档为准（按 key 合并，新增字段回落默认）
 * - 无存档 → 使用默认勾选（不再回退成「全部显示」）
 */
export function loadSaleReturnDocFieldVisibility(): SaleReturnDocPageField[] {
  let saved: any[] = []
  try {
    const raw = localStorage.getItem(SRD_FORM_CONFIG_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      saved = Array.isArray(parsed) ? parsed : (parsed.pageFields || [])
    }
  } catch {
    saved = []
  }
  if (!Array.isArray(saved) || saved.length === 0) {
    return SALE_RETURN_DOC_PAGE_FIELDS.map(f => ({ ...f }))
  }
  const savedMap = new Map<string, boolean>(saved.map((f: any) => [String(f.key), f.visible !== false]))
  const merged = SALE_RETURN_DOC_PAGE_FIELDS.map(f => ({
    ...f,
    visible: savedMap.has(f.key) ? Boolean(savedMap.get(f.key)) : f.visible,
  }))
  // 存档里出现但默认表里没有的字段（历史遗留）直接丢弃，避免口径漂移
  return merged
}
