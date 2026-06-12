/**
 * 打印模板种子脚本
 *
 * 为所有已集成打印按钮的页面生成初始打印模板。
 *
 * 使用方式（二选一）：
 * 1. 在模板管理页面点击"初始化模板"按钮（推荐）
 * 2. 浏览器控制台执行：
 *    import('@/views/printing/seed-templates').then(m => m.seedAllTemplates())
 *
 * 注意：需要确保已登录且 API Token 有效。
 */

import { printingApi } from '@/api/printing'
import type { PrintTemplateCreateRequest } from '@/api/printing'

// ═══════════════════════════════════════════════════════════════
// 类型定义
// ═══════════════════════════════════════════════════════════════

interface TableColumnDef {
  header: string
  field: string
  width: number
}

interface FieldDef {
  label: string
  field: string
}

interface StatusDef {
  label: string
  color?: string
}

interface TemplateSpec {
  /** 页面编码数组（路由路径去除动态段，多个值表示同业务不同入口） */
  pageCodes: string[]
  /** 模板名称 */
  name: string
  /** 所属业务类型 */
  businessType: string
  /** 单据标题，如"销售订单" */
  docTitle: string
  /** 单据编号字段名 */
  docNoField: string
  /** 详情字段列表 */
  fields: FieldDef[]
  /** 表格列定义（带明细行的单据） */
  tableColumns?: TableColumnDef[]
  /** 表格数据源字段名 */
  tableDataSource?: string
  /** 状态映射（有状态流转的需生成节点模板） */
  statusMap?: Record<number, StatusDef>
  /** 合计字段 */
  totalFields?: FieldDef[]
  /** 列表页面表格列定义（列表页用） */
  listColumns?: TableColumnDef[]
}

// ═══════════════════════════════════════════════════════════════
// 模板规格：按业务模块组织
// pageCodes 数组包含该页面可能使用的所有路由路径 pattern
// ═══════════════════════════════════════════════════════════════

const ALL_SPECS: TemplateSpec[] = [

  // ── 销售管理 ──────────────────────────────────────────
  {
    pageCodes: ['sale', 'erp/sale', 'sale/orders'],
    name: '销售订单列表_默认模板',
    businessType: 'sale_order',
    docTitle: '销售订单',
    docNoField: 'orderNo',
    fields: [
      { label: '客户名称', field: 'customerName' },
      { label: '订单日期', field: 'orderDate' },
      { label: '订单金额', field: 'totalAmountWithTax' },
      { label: '销售员', field: 'salesmanName' },
      { label: '状态', field: 'statusLabel' },
      { label: '备注', field: 'remark' },
    ],
    listColumns: [
      { header: '订单号', field: 'orderNo', width: 30 },
      { header: '客户', field: 'customerName', width: 25 },
      { header: '订单日期', field: 'orderDate', width: 20 },
      { header: '金额', field: 'totalAmountWithTax', width: 25 },
      { header: '状态', field: 'statusLabel', width: 15 },
      { header: '销售员', field: 'salesmanName', width: 15 },
    ],
  },
  {
    pageCodes: ['sale/order', 'sale/detail/OrderDetail'],
    name: '销售订单详情_默认模板',
    businessType: 'sale_order',
    docTitle: '销售订单',
    docNoField: 'orderNo',
    fields: [
      { label: '客户名称', field: 'customerName' },
      { label: '订单日期', field: 'orderDate' },
      { label: '销售员', field: 'salesperson' },
      { label: '预计发货日', field: 'deliveryDate' },
      { label: '订单金额', field: 'totalAmount' },
      { label: '折扣金额', field: 'discountAmount' },
      { label: '税额', field: 'taxAmount' },
      { label: '最终金额', field: 'finalAmount' },
      { label: '创建时间', field: 'createTime' },
      { label: '备注', field: 'remark' },
    ],
    tableColumns: [
      { header: '产品编码', field: 'productCode', width: 25 },
      { header: '产品名称', field: 'productName', width: 45 },
      { header: '规格', field: 'specification', width: 20 },
      { header: '数量', field: 'quantity', width: 15 },
      { header: '单价', field: 'unitPrice', width: 20 },
      { header: '折扣', field: 'discount', width: 15 },
      { header: '金额', field: 'amount', width: 20 },
      { header: '价税合计', field: 'totalAmount', width: 20 },
    ],
    tableDataSource: 'details',
    totalFields: [
      { label: '合计金额', field: 'totalAmount' },
    ],
    statusMap: {
      0: { label: '草稿' },
      1: { label: '审批中' },
      2: { label: '已通过' },
      3: { label: '已驳回' },
    },
  },
  {
    pageCodes: ['sale/exchange', 'erp/sale/exchange'],
    name: '销售换货_默认模板',
    businessType: 'sale_exchange',
    docTitle: '销售换货单',
    docNoField: 'exchangeNo',
    fields: [
      { label: '换货日期', field: 'exchangeDate' },
      { label: '客户名称', field: 'customerName' },
      { label: '原订单号', field: 'originalOrderNo' },
      { label: '换货原因', field: 'reason' },
      { label: '经手人', field: 'handler' },
      { label: '备注', field: 'remark' },
    ],
    tableColumns: [
      { header: '产品名称', field: 'productName', width: 50 },
      { header: '换出数量', field: 'outQuantity', width: 20 },
      { header: '换入数量', field: 'inQuantity', width: 20 },
      { header: '差价', field: 'priceDiff', width: 25 },
    ],
    tableDataSource: 'items',
  },
  {
    pageCodes: ['sale/return', 'erp/sale/return'],
    name: '销售退货_默认模板',
    businessType: 'sale_return',
    docTitle: '销售退货单',
    docNoField: 'returnNo',
    fields: [
      { label: '退货日期', field: 'returnDate' },
      { label: '客户名称', field: 'customerName' },
      { label: '原订单号', field: 'originalOrderNo' },
      { label: '退货原因', field: 'reason' },
      { label: '经手人', field: 'handler' },
      { label: '备注', field: 'remark' },
    ],
    tableColumns: [
      { header: '产品名称', field: 'productName', width: 50 },
      { header: '退货数量', field: 'quantity', width: 20 },
      { header: '单价', field: 'unitPrice', width: 20 },
      { header: '退货金额', field: 'amount', width: 25 },
    ],
    tableDataSource: 'items',
  },
  {
    pageCodes: ['sale/outbound', 'erp/sale/outbound'],
    name: '销售出库_默认模板',
    businessType: 'sale_outbound',
    docTitle: '销售出库单',
    docNoField: 'outboundNo',
    fields: [
      { label: '出库日期', field: 'outboundDate' },
      { label: '客户名称', field: 'customerName' },
      { label: '仓库', field: 'warehouseName' },
      { label: '销售订单号', field: 'orderNo' },
      { label: '经手人', field: 'handler' },
      { label: '备注', field: 'remark' },
    ],
    tableColumns: [
      { header: '产品名称', field: 'productName', width: 50 },
      { header: '规格型号', field: 'specification', width: 20 },
      { header: '出库数量', field: 'quantity', width: 20 },
      { header: '单价', field: 'unitPrice', width: 20 },
      { header: '金额', field: 'amount', width: 25 },
    ],
    tableDataSource: 'items',
  },
  {
    pageCodes: ['sale/quotation', 'erp/sale/quotation'],
    name: '销售报价_默认模板',
    businessType: 'sale_quotation',
    docTitle: '销售报价单',
    docNoField: 'quotationNo',
    fields: [
      { label: '报价日期', field: 'quotationDate' },
      { label: '客户名称', field: 'customerName' },
      { label: '联系人', field: 'contactPerson' },
      { label: '有效期至', field: 'validUntil' },
      { label: '销售员', field: 'salesperson' },
      { label: '备注', field: 'remark' },
    ],
    tableColumns: [
      { header: '产品名称', field: 'productName', width: 50 },
      { header: '规格', field: 'specification', width: 20 },
      { header: '数量', field: 'quantity', width: 15 },
      { header: '单价', field: 'unitPrice', width: 20 },
      { header: '金额', field: 'amount', width: 25 },
    ],
    tableDataSource: 'items',
    totalFields: [
      { label: '合计金额', field: 'totalAmount' },
    ],
  },
  {
    pageCodes: ['sale/receipt', 'erp/sale/receipt'],
    name: '销售收款_默认模板',
    businessType: 'sale_receipt',
    docTitle: '销售收款单',
    docNoField: 'receiptNo',
    fields: [
      { label: '收款日期', field: 'receiptDate' },
      { label: '客户名称', field: 'customerName' },
      { label: '收款方式', field: 'paymentMethod' },
      { label: '收款金额', field: 'amount' },
      { label: '关联订单', field: 'orderNo' },
      { label: '经手人', field: 'handler' },
      { label: '备注', field: 'remark' },
    ],
  },

  // ── 采购管理 ──────────────────────────────────────────
  {
    pageCodes: ['purchase', 'erp/purchase', 'purchase/orders'],
    name: '采购订单列表_默认模板',
    businessType: 'purchase_order',
    docTitle: '采购订单',
    docNoField: 'orderNo',
    fields: [
      { label: '供应商', field: 'supplierName' },
      { label: '订单日期', field: 'orderDate' },
      { label: '订单金额', field: 'totalAmount' },
      { label: '采购员', field: 'purchaserName' },
      { label: '状态', field: 'statusLabel' },
      { label: '备注', field: 'remark' },
    ],
    listColumns: [
      { header: '订单号', field: 'orderNo', width: 30 },
      { header: '供应商', field: 'supplierName', width: 25 },
      { header: '订单日期', field: 'orderDate', width: 20 },
      { header: '金额', field: 'totalAmount', width: 20 },
      { header: '状态', field: 'statusLabel', width: 15 },
      { header: '采购员', field: 'purchaserName', width: 15 },
    ],
  },
  {
    pageCodes: ['purchase/order', 'purchase/detail/OrderDetail'],
    name: '采购订单详情_默认模板',
    businessType: 'purchase_order',
    docTitle: '采购订单',
    docNoField: 'orderNo',
    fields: [
      { label: '供应商', field: 'supplierName' },
      { label: '订单日期', field: 'orderDate' },
      { label: '采购员', field: 'purchaserName' },
      { label: '预计到货日', field: 'expectedDeliveryDate' },
      { label: '付款方式', field: 'paymentMethod' },
      { label: '付款状态', field: 'paymentStatus' },
      { label: '订单金额', field: 'totalAmount' },
      { label: '税额', field: 'taxAmount' },
      { label: '价税合计', field: 'totalAmountWithTax' },
      { label: '备注', field: 'remark' },
    ],
    tableColumns: [
      { header: '产品编码', field: 'productCode', width: 25 },
      { header: '产品名称', field: 'productName', width: 45 },
      { header: '规格', field: 'specification', width: 20 },
      { header: '数量', field: 'quantity', width: 15 },
      { header: '单价', field: 'unitPrice', width: 20 },
      { header: '金额', field: 'amount', width: 25 },
      { header: '税率', field: 'taxRate', width: 15 },
      { header: '税额', field: 'taxAmount', width: 20 },
    ],
    tableDataSource: 'details',
    totalFields: [
      { label: '合计金额', field: 'totalAmount' },
      { label: '价税合计', field: 'totalAmountWithTax' },
    ],
    statusMap: {
      0: { label: '草稿' },
      1: { label: '审批中' },
      2: { label: '已通过' },
      3: { label: '已驳回' },
    },
  },
  {
    pageCodes: ['purchase/inbound', 'purchase/detail/inbound/InboundDetail', 'erp/stock-in'],
    name: '采购入库_默认模板',
    businessType: 'purchase_inbound',
    docTitle: '采购入库单',
    docNoField: 'inboundNo',
    fields: [
      { label: '入库日期', field: 'inboundDate' },
      { label: '供应商', field: 'supplierName' },
      { label: '仓库', field: 'warehouseName' },
      { label: '采购订单号', field: 'orderNo' },
      { label: '入库类型', field: 'inboundType' },
      { label: '经手人', field: 'handler' },
      { label: '备注', field: 'remark' },
    ],
    tableColumns: [
      { header: '产品名称', field: 'productName', width: 50 },
      { header: '规格', field: 'specification', width: 20 },
      { header: '入库数量', field: 'quantity', width: 20 },
      { header: '单价', field: 'unitPrice', width: 20 },
      { header: '金额', field: 'amount', width: 25 },
    ],
    tableDataSource: 'items',
  },
  {
    pageCodes: ['purchase/exchange', 'erp/purchase-exchange'],
    name: '采购换货_默认模板',
    businessType: 'purchase_exchange',
    docTitle: '采购换货单',
    docNoField: 'exchangeNo',
    fields: [
      { label: '换货日期', field: 'exchangeDate' },
      { label: '供应商', field: 'supplierName' },
      { label: '原订单号', field: 'originalOrderNo' },
      { label: '换货原因', field: 'reason' },
      { label: '经手人', field: 'handler' },
      { label: '备注', field: 'remark' },
    ],
    tableColumns: [
      { header: '产品名称', field: 'productName', width: 50 },
      { header: '换出数量', field: 'outQuantity', width: 20 },
      { header: '换入数量', field: 'inQuantity', width: 20 },
      { header: '差价', field: 'priceDiff', width: 25 },
    ],
    tableDataSource: 'items',
  },
  {
    pageCodes: ['purchase/return', 'erp/return'],
    name: '采购退货_默认模板',
    businessType: 'purchase_return',
    docTitle: '采购退货单',
    docNoField: 'returnNo',
    fields: [
      { label: '退货日期', field: 'returnDate' },
      { label: '供应商', field: 'supplierName' },
      { label: '原订单号', field: 'originalOrderNo' },
      { label: '退货原因', field: 'reason' },
      { label: '经手人', field: 'handler' },
      { label: '备注', field: 'remark' },
    ],
    tableColumns: [
      { header: '产品名称', field: 'productName', width: 50 },
      { header: '退货数量', field: 'quantity', width: 20 },
      { header: '单价', field: 'unitPrice', width: 20 },
      { header: '退货金额', field: 'amount', width: 25 },
    ],
    tableDataSource: 'items',
  },
  {
    pageCodes: ['purchase/payment'],
    name: '采购付款_默认模板',
    businessType: 'purchase_payment',
    docTitle: '采购付款单',
    docNoField: 'paymentNo',
    fields: [
      { label: '付款日期', field: 'paymentDate' },
      { label: '供应商', field: 'supplierName' },
      { label: '付款方式', field: 'paymentMethod' },
      { label: '付款金额', field: 'amount' },
      { label: '关联订单', field: 'orderNo' },
      { label: '经手人', field: 'handler' },
      { label: '备注', field: 'remark' },
    ],
  },
  {
    pageCodes: ['purchase/inquiry', 'purchase/detail/inquiry/InquiryDetail'],
    name: '采购询价_默认模板',
    businessType: 'purchase_inquiry',
    docTitle: '采购询价单',
    docNoField: 'inquiryNo',
    fields: [
      { label: '询价日期', field: 'inquiryDate' },
      { label: '供应商', field: 'supplierName' },
      { label: '有效期至', field: 'validUntil' },
      { label: '采购员', field: 'purchaserName' },
      { label: '备注', field: 'remark' },
    ],
    tableColumns: [
      { header: '产品名称', field: 'productName', width: 60 },
      { header: '规格', field: 'specification', width: 25 },
      { header: '数量', field: 'quantity', width: 20 },
    ],
    tableDataSource: 'items',
  },

  // ── CRM 模块 ──────────────────────────────────────────
  {
    pageCodes: ['crm/contract'],
    name: '合同_默认模板',
    businessType: 'contract',
    docTitle: '销售合同',
    docNoField: 'contractNo',
    fields: [
      { label: '合同名称', field: 'contractName' },
      { label: '客户名称', field: 'customerName' },
      { label: '合同类型', field: 'contractType' },
      { label: '开始日期', field: 'startDate' },
      { label: '结束日期', field: 'endDate' },
      { label: '合同金额', field: 'contractAmount' },
      { label: '付款方式', field: 'paymentMethod' },
      { label: '签订日期', field: 'signDate' },
      { label: '签订人', field: 'signPerson' },
      { label: '备注', field: 'remark' },
    ],
  },
  {
    pageCodes: ['crm/invoice'],
    name: '发票_默认模板',
    businessType: 'invoice',
    docTitle: '销售发票',
    docNoField: 'invoiceNo',
    fields: [
      { label: '客户名称', field: 'customerName' },
      { label: '开票日期', field: 'invoiceDate' },
      { label: '发票类型', field: 'invoiceType' },
      { label: '发票金额', field: 'amount' },
      { label: '税额', field: 'taxAmount' },
      { label: '价税合计', field: 'totalAmount' },
      { label: '开票人', field: 'issuer' },
      { label: '备注', field: 'remark' },
    ],
  },
  {
    pageCodes: ['crm/quotation'],
    name: '报价单_默认模板',
    businessType: 'quotation',
    docTitle: '销售报价单',
    docNoField: 'quotationNo',
    fields: [
      { label: '报价日期', field: 'quotationDate' },
      { label: '客户名称', field: 'customerName' },
      { label: '联系人', field: 'contactPerson' },
      { label: '有效期至', field: 'validUntil' },
      { label: '销售员', field: 'salesperson' },
      { label: '备注', field: 'remark' },
    ],
    tableColumns: [
      { header: '产品名称', field: 'productName', width: 50 },
      { header: '规格', field: 'specification', width: 20 },
      { header: '数量', field: 'quantity', width: 15 },
      { header: '单价', field: 'unitPrice', width: 20 },
      { header: '金额', field: 'amount', width: 25 },
    ],
    tableDataSource: 'items',
    totalFields: [
      { label: '合计金额', field: 'totalAmount' },
    ],
  },
  {
    pageCodes: ['crm/customer', 'crm/customer/detail/CustomerDetail'],
    name: '客户资料_默认模板',
    businessType: 'customer',
    docTitle: '客户资料卡',
    docNoField: 'customerNo',
    fields: [
      { label: '客户名称', field: 'customerName' },
      { label: '联系人', field: 'contactPerson' },
      { label: '联系电话', field: 'contactPhone' },
      { label: '电子邮箱', field: 'email' },
      { label: '客户等级', field: 'grade' },
      { label: '所属区域', field: 'region' },
      { label: '地址', field: 'address' },
      { label: '信用额度', field: 'creditLimit' },
      { label: '备注', field: 'remark' },
    ],
  },

  // ── 费用管理 ──────────────────────────────────────────
  {
    pageCodes: ['erp/expense/application'],
    name: '费用申请_默认模板',
    businessType: 'expense_application',
    docTitle: '费用申请单',
    docNoField: 'applicationNo',
    fields: [
      { label: '申请人', field: 'applicant' },
      { label: '所属部门', field: 'departmentName' },
      { label: '费用类型', field: 'expenseType' },
      { label: '申请金额', field: 'amount' },
      { label: '申请日期', field: 'applicationDate' },
      { label: '预计使用日期', field: 'expectedDate' },
      { label: '备注', field: 'remark' },
    ],
    statusMap: {
      0: { label: '草稿' },
      1: { label: '审批中' },
      2: { label: '已通过' },
      3: { label: '已驳回' },
    },
  },
  {
    pageCodes: ['erp/expense/reimbursement'],
    name: '费用报销_默认模板',
    businessType: 'expense_reimbursement',
    docTitle: '费用报销单',
    docNoField: 'reimbursementNo',
    fields: [
      { label: '报销人', field: 'applicant' },
      { label: '所属部门', field: 'departmentName' },
      { label: '报销金额', field: 'amount' },
      { label: '报销日期', field: 'reimbursementDate' },
      { label: '关联申请单', field: 'applicationNo' },
      { label: '备注', field: 'remark' },
    ],
  },
  {
    pageCodes: ['erp/expense/payment'],
    name: '费用付款_默认模板',
    businessType: 'expense_payment',
    docTitle: '费用付款单',
    docNoField: 'paymentNo',
    fields: [
      { label: '付款日期', field: 'paymentDate' },
      { label: '收款人', field: 'payee' },
      { label: '付款方式', field: 'paymentMethod' },
      { label: '付款金额', field: 'amount' },
      { label: '关联报销单', field: 'reimbursementNo' },
      { label: '备注', field: 'remark' },
    ],
  },

  // ── 财务管理 ──────────────────────────────────────────
  {
    pageCodes: ['finance/voucher', 'erp/finance/voucher'],
    name: '凭证列表_默认模板',
    businessType: 'voucher',
    docTitle: '记账凭证',
    docNoField: 'voucherNo',
    fields: [
      { label: '凭证日期', field: 'voucherDate' },
      { label: '摘要', field: 'summary' },
      { label: '制单人', field: 'createdBy' },
    ],
    listColumns: [
      { header: '凭证号', field: 'voucherNo', width: 25 },
      { header: '日期', field: 'voucherDate', width: 20 },
      { header: '摘要', field: 'summary', width: 40 },
      { header: '借方合计', field: 'debitTotal', width: 20 },
      { header: '贷方合计', field: 'creditTotal', width: 20 },
      { header: '状态', field: 'statusLabel', width: 15 },
    ],
  },
  {
    pageCodes: ['finance/voucher/detail', 'finance/voucher/VoucherDetail', 'erp/finance/voucher/detail'],
    name: '凭证详情_默认模板',
    businessType: 'voucher',
    docTitle: '记账凭证',
    docNoField: 'voucherNo',
    fields: [
      { label: '凭证日期', field: 'voucherDate' },
      { label: '年度', field: 'fiscalYear' },
      { label: '期间（月）', field: 'fiscalPeriod' },
      { label: '制单人', field: 'createdBy' },
      { label: '审核人', field: 'auditedBy' },
      { label: '过账人', field: 'postedBy' },
      { label: '摘要', field: 'summary', col: 2 } as any,
    ],
    tableColumns: [
      { header: '摘要', field: 'summary', width: 40 },
      { header: '会计科目', field: 'subjectName', width: 50 },
      { header: '借方金额', field: 'debitAmount', width: 35 },
      { header: '贷方金额', field: 'creditAmount', width: 35 },
    ],
    tableDataSource: 'entries',
    totalFields: [
      { label: '借方合计', field: 'debitTotal' },
      { label: '贷方合计', field: 'creditTotal' },
    ],
    statusMap: {
      0: { label: '草稿' },
      1: { label: '已审核' },
      2: { label: '已过账' },
      3: { label: '已冲销' },
    },
  },
  {
    pageCodes: ['finance/receivable', 'erp/finance/receivable'],
    name: '应收账款_默认模板',
    businessType: 'receivable',
    docTitle: '应收账款',
    docNoField: 'receiptNo',
    fields: [
      { label: '客户名称', field: 'customerName' },
      { label: '应收金额', field: 'amount' },
      { label: '已收金额', field: 'receivedAmount' },
      { label: '未收金额', field: 'balanceAmount' },
      { label: '到期日', field: 'dueDate' },
      { label: '关联订单', field: 'orderNo' },
      { label: '备注', field: 'remark' },
    ],
  },
  {
    pageCodes: ['finance/payable', 'erp/finance/payable'],
    name: '应付账款_默认模板',
    businessType: 'payable',
    docTitle: '应付账款',
    docNoField: 'paymentNo',
    fields: [
      { label: '供应商', field: 'supplierName' },
      { label: '应付金额', field: 'amount' },
      { label: '已付金额', field: 'paidAmount' },
      { label: '未付金额', field: 'balanceAmount' },
      { label: '到期日', field: 'dueDate' },
      { label: '关联订单', field: 'orderNo' },
      { label: '备注', field: 'remark' },
    ],
  },
  {
    pageCodes: ['finance/accounts-receivable', 'erp/finance/accounts-receivable'],
    name: '应收列表_默认模板',
    businessType: 'receivable',
    docTitle: '应收账款列表',
    docNoField: 'receiptNo',
    fields: [
      { label: '客户名称', field: 'customerName' },
      { label: '应收金额', field: 'amount' },
    ],
    listColumns: [
      { header: '客户名称', field: 'customerName', width: 25 },
      { header: '应收金额', field: 'amount', width: 20 },
      { header: '已收金额', field: 'receivedAmount', width: 20 },
      { header: '未收金额', field: 'balanceAmount', width: 20 },
      { header: '到期日', field: 'dueDate', width: 20 },
    ],
  },
  {
    pageCodes: ['finance/accounts-payable', 'erp/finance/accounts-payable'],
    name: '应付列表_默认模板',
    businessType: 'payable',
    docTitle: '应付账款列表',
    docNoField: 'paymentNo',
    fields: [
      { label: '供应商', field: 'supplierName' },
      { label: '应付金额', field: 'amount' },
    ],
    listColumns: [
      { header: '供应商', field: 'supplierName', width: 25 },
      { header: '应付金额', field: 'amount', width: 20 },
      { header: '已付金额', field: 'paidAmount', width: 20 },
      { header: '未付金额', field: 'balanceAmount', width: 20 },
      { header: '到期日', field: 'dueDate', width: 20 },
    ],
  },
  {
    pageCodes: ['finance/accounts-receivable/payment-record', 'erp/finance/accounts-receivable/payment-record'],
    name: '收款记录_默认模板',
    businessType: 'payment_record',
    docTitle: '收款记录',
    docNoField: 'receiptNo',
    fields: [
      { label: '收款日期', field: 'receiptDate' },
      { label: '客户名称', field: 'customerName' },
      { label: '收款金额', field: 'amount' },
      { label: '收款方式', field: 'paymentMethod' },
      { label: '关联应收单', field: 'receivableNo' },
      { label: '备注', field: 'remark' },
    ],
  },

  // ── 库存管理 ──────────────────────────────────────────
  {
    pageCodes: ['erp/stock-in', 'erp/stock-in/detail'],
    name: '入库单_默认模板',
    businessType: 'stock_in',
    docTitle: '入库单',
    docNoField: 'inboundNo',
    fields: [
      { label: '入库日期', field: 'inboundDate' },
      { label: '来源', field: 'sourceName' },
      { label: '仓库', field: 'warehouseName' },
      { label: '入库类型', field: 'inboundType' },
      { label: '经手人', field: 'handler' },
      { label: '备注', field: 'remark' },
    ],
    tableColumns: [
      { header: '产品名称', field: 'productName', width: 50 },
      { header: '规格', field: 'specification', width: 20 },
      { header: '入库数量', field: 'quantity', width: 20 },
      { header: '单价', field: 'unitPrice', width: 20 },
      { header: '金额', field: 'amount', width: 25 },
    ],
    tableDataSource: 'items',
    statusMap: {
      0: { label: '草稿' },
      1: { label: '已确认' },
      2: { label: '已完成' },
    },
  },
  {
    pageCodes: ['erp/shipment', 'erp/shipment/detail'],
    name: '出库单_默认模板',
    businessType: 'shipment',
    docTitle: '出库单',
    docNoField: 'shipmentNo',
    fields: [
      { label: '销售订单', field: 'orderNo' },
      { label: '客户名称', field: 'customerName' },
      { label: '仓库', field: 'warehouseName' },
      { label: '出库类型', field: 'shipmentType' },
      { label: '出库金额', field: 'totalAmount' },
      { label: '出库日期', field: 'shipmentDate' },
      { label: '联系人', field: 'contactPerson' },
      { label: '联系电话', field: 'contactPhone' },
      { label: '业务员', field: 'salesperson' },
      { label: '收货人', field: 'consignee' },
      { label: '收货地址', field: 'consigneeAddress' },
      { label: '物流单号', field: 'logisticsNo' },
      { label: '备注', field: 'remark' },
    ],
    tableColumns: [
      { header: '产品名称', field: 'productName', width: 50 },
      { header: '规格', field: 'specification', width: 20 },
      { header: '出库数量', field: 'quantity', width: 20 },
      { header: '单价', field: 'unitPrice', width: 20 },
      { header: '金额', field: 'amount', width: 25 },
    ],
    tableDataSource: 'items',
    statusMap: {
      0: { label: '草稿' },
      1: { label: '已确认' },
      2: { label: '已完成' },
    },
  },
  {
    pageCodes: ['erp/stocktake', 'erp/stocktake/detail'],
    name: '盘点单_默认模板',
    businessType: 'stocktake',
    docTitle: '库存盘点单',
    docNoField: 'stocktakeNo',
    fields: [
      { label: '盘点日期', field: 'stocktakeDate' },
      { label: '仓库', field: 'warehouseName' },
      { label: '盘点人', field: 'handler' },
      { label: '备注', field: 'remark' },
    ],
    tableColumns: [
      { header: '产品名称', field: 'productName', width: 50 },
      { header: '规格', field: 'specification', width: 20 },
      { header: '账面数量', field: 'bookQuantity', width: 20 },
      { header: '实盘数量', field: 'actualQuantity', width: 20 },
      { header: '差异', field: 'difference', width: 20 },
    ],
    tableDataSource: 'items',
    statusMap: {
      0: { label: '草稿' },
      1: { label: '已确认' },
      2: { label: '已完成' },
    },
  },

  // ── 固定资产 ──────────────────────────────────────────
  {
    pageCodes: ['fixed-asset/asset', 'fixed-asset/asset/detail', 'erp/fixed-asset/asset', 'erp/fixed-asset/asset/detail'],
    name: '固定资产_默认模板',
    businessType: 'fixed_asset',
    docTitle: '固定资产卡片',
    docNoField: 'assetCode',
    fields: [
      { label: '资产编码', field: 'assetCode' },
      { label: '资产名称', field: 'assetName' },
      { label: '资产类别', field: 'categoryName' },
      { label: '规格型号', field: 'specification' },
      { label: '所在部门', field: 'departmentName' },
      { label: '使用人', field: 'userName' },
      { label: '存放地点', field: 'location' },
      { label: '原值', field: 'originalValue' },
      { label: '累计折旧', field: 'accumulatedDepreciation' },
      { label: '净值', field: 'netValue' },
      { label: '启用日期', field: 'startDate' },
      { label: '使用年限', field: 'usefulLife' },
      { label: '状态', field: 'statusLabel' },
      { label: '备注', field: 'remark' },
    ],
  },
  {
    pageCodes: ['fixed-asset/disposal', 'erp/fixed-asset/disposal'],
    name: '资产处置_默认模板',
    businessType: 'fixed_asset_disposal',
    docTitle: '固定资产处置单',
    docNoField: 'disposalNo',
    fields: [
      { label: '处置单号', field: 'disposalNo' },
      { label: '资产编码', field: 'assetCode' },
      { label: '资产名称', field: 'assetName' },
      { label: '处置方式', field: 'disposalMethod' },
      { label: '处置日期', field: 'disposalDate' },
      { label: '处置金额', field: 'disposalAmount' },
      { label: '残值收入', field: 'residualIncome' },
      { label: '处置费用', field: 'disposalExpense' },
      { label: '处置损益', field: 'profitLoss' },
      { label: '经办人', field: 'handler' },
      { label: '备注', field: 'remark' },
    ],
  },
  {
    pageCodes: ['fixed-asset/transfer', 'erp/fixed-asset/transfer'],
    name: '资产调拨_默认模板',
    businessType: 'fixed_asset_transfer',
    docTitle: '固定资产调拨单',
    docNoField: 'transferNo',
    fields: [
      { label: '调拨单号', field: 'transferNo' },
      { label: '资产编码', field: 'assetCode' },
      { label: '资产名称', field: 'assetName' },
      { label: '调出部门', field: 'fromDepartment' },
      { label: '调入部门', field: 'toDepartment' },
      { label: '调拨日期', field: 'transferDate' },
      { label: '调拨原因', field: 'reason' },
      { label: '经办人', field: 'handler' },
      { label: '备注', field: 'remark' },
    ],
  },
  {
    pageCodes: ['fixed-asset/inventory', 'erp/fixed-asset/inventory'],
    name: '资产盘点_默认模板',
    businessType: 'fixed_asset_inventory',
    docTitle: '固定资产盘点表',
    docNoField: 'inventoryNo',
    fields: [
      { label: '盘点日期', field: 'inventoryDate' },
      { label: '盘点部门', field: 'departmentName' },
      { label: '盘点人', field: 'handler' },
      { label: '备注', field: 'remark' },
    ],
    tableColumns: [
      { header: '资产编码', field: 'assetCode', width: 25 },
      { header: '资产名称', field: 'assetName', width: 40 },
      { header: '账面数量', field: 'bookQuantity', width: 20 },
      { header: '实盘数量', field: 'actualQuantity', width: 20 },
      { header: '差异', field: 'difference', width: 20 },
    ],
    tableDataSource: 'items',
  },
  {
    pageCodes: ['fixed-asset/report', 'erp/fixed-asset/report'],
    name: '资产报表_默认模板',
    businessType: 'fixed_asset_report',
    docTitle: '固定资产报表',
    docNoField: 'reportNo',
    fields: [
      { label: '资产编码', field: 'assetCode' },
    ],
    listColumns: [
      { header: '资产编码', field: 'assetCode', width: 20 },
      { header: '资产名称', field: 'assetName', width: 30 },
      { header: '类别', field: 'categoryName', width: 20 },
      { header: '原值', field: 'originalValue', width: 20 },
      { header: '累计折旧', field: 'accumulatedDepreciation', width: 20 },
      { header: '净值', field: 'netValue', width: 20 },
      { header: '状态', field: 'statusLabel', width: 15 },
    ],
  },

  // ── 固定资产（补充） ────────────────────────────────
  {
    pageCodes: ['fixed-asset/category', 'erp/fixed-asset/category'],
    name: '资产分类_默认模板',
    businessType: 'fixed_asset_category',
    docTitle: '固定资产分类表',
    docNoField: 'categoryCode',
    fields: [
      { label: '分类名称', field: 'categoryName' },
      { label: '分类编码', field: 'categoryCode' },
      { label: '上级分类', field: 'parentName' },
      { label: '使用年限', field: 'usefulLife' },
      { label: '折旧方法', field: 'depreciationMethod' },
      { label: '状态', field: 'statusLabel' },
    ],
    listColumns: [
      { header: '分类名称', field: 'categoryName', width: 30 },
      { header: '分类编码', field: 'categoryCode', width: 20 },
      { header: '上级分类', field: 'parentName', width: 20 },
      { header: '使用年限', field: 'usefulLife', width: 15 },
      { header: '折旧方法', field: 'depreciationMethod', width: 20 },
      { header: '状态', field: 'statusLabel', width: 15 },
    ],
  },
  {
    pageCodes: ['fixed-asset/depreciation', 'erp/fixed-asset/depreciation'],
    name: '折旧记录_默认模板',
    businessType: 'fixed_asset_depreciation',
    docTitle: '固定资产折旧记录',
    docNoField: 'depreciationNo',
    fields: [
      { label: '资产编码', field: 'assetCode' },
      { label: '资产名称', field: 'assetName' },
      { label: '期间', field: 'period' },
      { label: '折旧日期', field: 'depreciationDate' },
      { label: '本期折旧', field: 'periodAmount' },
      { label: '累计折旧', field: 'accumulatedDepreciation' },
      { label: '净值', field: 'netValue' },
      { label: '资产原值', field: 'assetOriginalValue' },
    ],
    listColumns: [
      { header: '资产编码', field: 'assetCode', width: 20 },
      { header: '资产名称', field: 'assetName', width: 25 },
      { header: '期间', field: 'period', width: 15 },
      { header: '折旧日期', field: 'depreciationDate', width: 20 },
      { header: '本期折旧', field: 'periodAmount', width: 20 },
      { header: '累计折旧', field: 'accumulatedDepreciation', width: 20 },
      { header: '净值', field: 'netValue', width: 20 },
    ],
  },
  {
    pageCodes: ['fixed-asset/purchase', 'erp/fixed-asset/purchase'],
    name: '资产购置_默认模板',
    businessType: 'fixed_asset_purchase',
    docTitle: '固定资产购置申请单',
    docNoField: 'purchaseNo',
    fields: [
      { label: '申请标题', field: 'title' },
      { label: '资产名称', field: 'assetName' },
      { label: '规格型号', field: 'specification' },
      { label: '分类', field: 'categoryName' },
      { label: '数量', field: 'quantity' },
      { label: '预估金额', field: 'estimatedAmount' },
      { label: '实际金额', field: 'actualAmount' },
      { label: '申请人', field: 'applicantName' },
      { label: '部门', field: 'departmentName' },
      { label: '供应商', field: 'supplierName' },
      { label: '预计交付日', field: 'expectedDate' },
      { label: '购置原因', field: 'reason' },
      { label: '备注', field: 'remark' },
    ],
    listColumns: [
      { header: '申请单号', field: 'purchaseNo', width: 22 },
      { header: '申请标题', field: 'title', width: 25 },
      { header: '资产名称', field: 'assetName', width: 20 },
      { header: '数量', field: 'quantity', width: 10 },
      { header: '预估金额', field: 'estimatedAmount', width: 18 },
      { header: '申请人', field: 'applicantName', width: 15 },
      { header: '部门', field: 'departmentName', width: 15 },
      { header: '状态', field: 'statusLabel', width: 15 },
    ],
    statusMap: {
      0: { label: '草稿' },
      1: { label: '待审批' },
      2: { label: '已通过' },
      3: { label: '已驳回' },
    },
  },

  // ── 订单中心 ──────────────────────────────────────────
  {
    pageCodes: ['order-center'],
    name: '订单中心_默认模板',
    businessType: 'order_center',
    docTitle: '订单中心',
    docNoField: 'orderNo',
    fields: [],
    listColumns: [
      { header: '订单号', field: 'orderNo', width: 25 },
      { header: '类型', field: 'orderType', width: 15 },
      { header: '往来单位', field: 'partyName', width: 25 },
      { header: '金额', field: 'totalAmount', width: 20 },
      { header: '状态', field: 'statusLabel', width: 15 },
    ],
  },

  // ── 库存明细 ──────────────────────────────────────────
  {
    pageCodes: ['stock/detail/StockDetail', 'stock/detail'],
    name: '库存明细_默认模板',
    businessType: 'stock_item',
    docTitle: '库存明细表',
    docNoField: 'productCode',
    fields: [
      { label: '产品编码', field: 'productCode' },
      { label: '产品名称', field: 'productName' },
      { label: '规格型号', field: 'specification' },
      { label: '仓库', field: 'warehouseName' },
      { label: '当前库存', field: 'quantity' },
      { label: '可用库存', field: 'availableQuantity' },
      { label: '库存金额', field: 'totalAmount' },
    ],
  },

  // ── ERP 统一销售 ──────────────────────────────────────────
  {
    pageCodes: ['erp/sale', 'erp/sales-analysis', 'erp/sales-report'],
    name: 'ERP销售_默认模板',
    businessType: 'sales_order',
    docTitle: '销售订单',
    docNoField: 'orderNo',
    fields: [
      { label: '客户名称', field: 'customerName' },
      { label: '订单日期', field: 'orderDate' },
      { label: '订单金额', field: 'totalAmount' },
      { label: '销售员', field: 'salesperson' },
      { label: '备注', field: 'remark' },
    ],
    listColumns: [
      { header: '订单号', field: 'orderNo', width: 30 },
      { header: '客户', field: 'customerName', width: 30 },
      { header: '金额', field: 'totalAmount', width: 25 },
      { header: '状态', field: 'statusLabel', width: 20 },
      { header: '销售员', field: 'salesperson', width: 20 },
    ],
  },

  // ── 费用审批 ──────────────────────────────────────────
  {
    pageCodes: ['erp/expense/approval'],
    name: '费用审批_默认模板',
    businessType: 'expense_approval',
    docTitle: '费用审批单',
    docNoField: 'applicationNo',
    fields: [
      { label: '申请人', field: 'applicant' },
      { label: '所属部门', field: 'departmentName' },
      { label: '费用类型', field: 'expenseType' },
      { label: '申请金额', field: 'amount' },
      { label: '申请日期', field: 'applicationDate' },
      { label: '备注', field: 'remark' },
    ],
    statusMap: {
      0: { label: '草稿' },
      1: { label: '审批中' },
      2: { label: '已通过' },
      3: { label: '已驳回' },
    },
  },

  // ── 批次管理 ──────────────────────────────────────────
  {
    pageCodes: ['erp/batch'],
    name: '批次管理_默认模板',
    businessType: 'batch',
    docTitle: '批次详情',
    docNoField: 'batchNo',
    fields: [
      { label: '批次号', field: 'batchNo' },
      { label: '商品编码', field: 'productCode' },
      { label: '商品名称', field: 'productName' },
      { label: '规格', field: 'specification' },
      { label: '单位', field: 'unit' },
      { label: '总数量', field: 'totalQuantity' },
      { label: '可用数量', field: 'availableQuantity' },
      { label: '仓库', field: 'warehouseName' },
      { label: '生产日期', field: 'productionDate' },
      { label: '到期日期', field: 'expirationDate' },
      { label: '批次状态', field: 'batchStatus' },
      { label: '质量状态', field: 'qualityStatus' },
    ],
  },

  // ── 序列号管理 ──────────────────────────────────────────
  {
    pageCodes: ['erp/serial'],
    name: '序列号_默认模板',
    businessType: 'serial',
    docTitle: '序列号追溯',
    docNoField: 'serialNo',
    fields: [
      { label: '序列号', field: 'serialNo' },
      { label: '产品编码', field: 'productCode' },
      { label: '产品名称', field: 'productName' },
      { label: '规格型号', field: 'specification' },
      { label: '制造商', field: 'manufacturer' },
      { label: '批次号', field: 'batchNo' },
      { label: '所在仓库', field: 'warehouseName' },
      { label: '生产日期', field: 'manufacturingDate' },
      { label: '创建时间', field: 'createdAt' },
    ],
  },

  // ── 往来单位 ──────────────────────────────────────────
  {
    pageCodes: ['erp/partner'],
    name: '往来单位_默认模板',
    businessType: 'partner',
    docTitle: '往来单位',
    docNoField: 'partnerCode',
    fields: [
      { label: '单位名称', field: 'partnerName' },
      { label: '单位编码', field: 'partnerCode' },
      { label: '单位类型', field: 'partnerType' },
      { label: '联系人', field: 'contactPerson' },
      { label: '联系电话', field: 'contactPhone' },
      { label: '地址', field: 'address' },
      { label: '信用额度', field: 'creditAmount' },
      { label: '结算方式', field: 'settleType' },
      { label: '纳税人识别号', field: 'taxId' },
      { label: '法人代表', field: 'legalPerson' },
      { label: '备注', field: 'remark' },
    ],
  },

  // ── 库存列表 ──────────────────────────────────────────
  {
    pageCodes: ['erp/stock'],
    name: '库存列表_默认模板',
    businessType: 'stock',
    docTitle: '库存明细',
    docNoField: 'productCode',
    fields: [
      { label: '商品编码', field: 'productCode' },
      { label: '商品名称', field: 'productName' },
      { label: '规格', field: 'specification' },
      { label: '单位', field: 'unit' },
      { label: '库存数量', field: 'quantity' },
      { label: '最低库存', field: 'minStock' },
      { label: '最高库存', field: 'maxStock' },
      { label: '仓库', field: 'warehouseName' },
      { label: '最后入库', field: 'lastInboundDate' },
      { label: '最后出库', field: 'lastOutboundDate' },
    ],
  },

  // ── 商城订单 ──────────────────────────────────────────
  {
    pageCodes: ['erp/mall/order'],
    name: '商城订单_默认模板',
    businessType: 'mall_order',
    docTitle: '订货单',
    docNoField: 'orderNo',
    fields: [
      { label: '订单编号', field: 'orderNo' },
      { label: '客户名称', field: 'companyName' },
      { label: '联系人', field: 'contactPerson' },
      { label: '联系电话', field: 'contactPhone' },
      { label: '收货地址', field: 'deliveryAddress' },
      { label: '订单金额', field: 'totalAmount' },
      { label: '支付方式', field: 'paymentMethod' },
      { label: '订单状态', field: 'orderStatus' },
      { label: '下单时间', field: 'orderTime' },
      { label: '备注', field: 'remark' },
    ],
    tableColumns: [
      { header: '商品编码', field: 'productCode', width: 30 },
      { header: '商品名称', field: 'productName', width: 50 },
      { header: '规格', field: 'specification', width: 25 },
      { header: '数量', field: 'quantity', width: 20 },
      { header: '单价', field: 'unitPrice', width: 25 },
      { header: '小计', field: 'subtotal', width: 25 },
      { header: '备注', field: 'remark', width: 25 },
    ],
    tableDataSource: 'items',
    totalFields: [
      { label: '合计金额', field: 'totalAmount' },
    ],
    statusMap: {
      0: { label: '待付款', color: 'orange' },
      1: { label: '待审核', color: 'blue' },
      2: { label: '已通过', color: 'green' },
      3: { label: '已发货', color: 'cyan' },
      4: { label: '已完成', color: 'green' },
      5: { label: '已取消', color: 'red' },
    },
  },

  // ── 商城发货单 ────────────────────────────────────────
  {
    pageCodes: ['erp/mall/delivery'],
    name: '商城发货单_默认模板',
    businessType: 'mall_delivery',
    docTitle: '发货单',
    docNoField: 'deliveryNo',
    fields: [
      { label: '发货单号', field: 'deliveryNo' },
      { label: '关联订单', field: 'orderNo' },
      { label: '客户名称', field: 'companyName' },
      { label: '联系人', field: 'contactPerson' },
      { label: '联系电话', field: 'contactPhone' },
      { label: '收货地址', field: 'deliveryAddress' },
      { label: '物流公司', field: 'logisticsCompany' },
      { label: '物流单号', field: 'logisticsNo' },
      { label: '发货时间', field: 'deliveryTime' },
      { label: '备注', field: 'remark' },
    ],
    tableColumns: [
      { header: '商品编码', field: 'productCode', width: 30 },
      { header: '商品名称', field: 'productName', width: 55 },
      { header: '规格', field: 'specification', width: 25 },
      { header: '发货数量', field: 'quantity', width: 25 },
      { header: '单位', field: 'unit', width: 20 },
      { header: '备注', field: 'remark', width: 25 },
    ],
    tableDataSource: 'items',
  },
]

// ═══════════════════════════════════════════════════════════════
// 模板生成函数
// ═══════════════════════════════════════════════════════════════

/**
 * 生成默认模板 JSON
 * A4 纵向，有效区域 190x277mm（四周 10mm 边距）
 */
function generateDefaultTemplate(spec: TemplateSpec): string {
  const components: Record<string, any>[] = []

  // ── 页眉 ──────────────────────────────────────────
  // 公司名称
  components.push({ type: 'label', x: 10, y: 5, w: 60, h: 6, content: '{{companyName}}', fontSize: 10, fontWeight: 'bold' })
  // 打印日期
  components.push({ type: 'label', x: 140, y: 5, w: 60, h: 5, content: '{{printDate}}', fontSize: 8, textAlign: 'right' })
  // 标题
  components.push({ type: 'label', x: 40, y: 14, w: 130, h: 10, content: spec.docTitle, fontSize: 18, fontWeight: 'bold', textAlign: 'center' })
  // 分隔线
  components.push({ type: 'line', x: 10, y: 26, w: 190, h: 0.5 })
  // 单据编号
  components.push({ type: 'label', x: 10, y: 29, w: 25, h: 5, content: '单据编号：', fontSize: 9 })
  components.push({ type: 'field', x: 35, y: 29, w: 60, h: 5, field: spec.docNoField, fontSize: 9 })

  // ── 主体：字段明细 ─────────────────────────────
  const hasListColumns = spec.listColumns && spec.listColumns.length > 0
  const isListStyle = hasListColumns && spec.fields.length <= 1

  if (!isListStyle && spec.fields.length > 0) {
    // 表单布局 - 两列
    let fieldStartY = 37
    const labelW = 28
    const valueW = 62
    const colGap = 5
    const col1X = 10
    const col2X = 105
    const rowH = 6

    spec.fields.forEach((f, idx) => {
      const col = idx % 2 === 0 ? 0 : 1
      const row = Math.floor(idx / 2)
      const x = col === 0 ? col1X : col2X
      const y = fieldStartY + row * rowH

      components.push({ type: 'label', x, y, w: labelW, h: 5, content: f.label + '：', fontSize: 9, textAlign: 'right', fontWeight: 'bold' })
      components.push({ type: 'field', x: x + labelW, y, w: valueW, h: 5, field: f.field, fontSize: 9 })
    })
  }

  // ── 明细表格 ──────────────────────────────────────
  const tableStartY = 85
  if (spec.tableColumns && spec.tableDataSource) {
    components.push({
      type: 'table',
      x: 10, y: tableStartY, w: 190, h: 120,
      columns: spec.tableColumns,
      dataSource: spec.tableDataSource,
      fontSize: 8,
    })

    // 合计行
    if (spec.totalFields) {
      const totalY = tableStartY + 125
      spec.totalFields.forEach((tf, i) => {
        const x = 100 + i * 50
        components.push({ type: 'label', x, y: totalY, w: 25, h: 5, content: tf.label + '：', fontSize: 9, fontWeight: 'bold', textAlign: 'right' })
        components.push({ type: 'field', x: x + 25, y: totalY, w: 40, h: 5, field: tf.field, fontSize: 9, fontWeight: 'bold' })
      })
    }
  }

  // ── 列表表格 ──────────────────────────────────────
  if (isListStyle && spec.listColumns) {
    components.push({
      type: 'table',
      x: 10, y: 40, w: 190, h: 180,
      columns: spec.listColumns,
      dataSource: 'records',
      fontSize: 8,
    })
  }

  // ── 页脚 ──────────────────────────────────────────
  const footerY = 240
  components.push({ type: 'line', x: 10, y: footerY, w: 190, h: 0.5 })
  components.push({ type: 'label', x: 10, y: footerY + 4, w: 25, h: 5, content: '制单人：', fontSize: 9 })
  components.push({ type: 'field', x: 35, y: footerY + 4, w: 35, h: 5, field: 'createdBy', fontSize: 9, border: false })
  components.push({ type: 'label', x: 80, y: footerY + 4, w: 25, h: 5, content: '审核人：', fontSize: 9 })
  components.push({ type: 'field', x: 105, y: footerY + 4, w: 35, h: 5, field: 'auditedBy', fontSize: 9, border: false })
  components.push({ type: 'label', x: 150, y: footerY + 4, w: 25, h: 5, content: '签章：', fontSize: 9 })
  // 页码
  components.push({ type: 'label', x: 145, y: footerY + 14, w: 55, h: 5, content: '第 {{pageNo}} / {{totalPages}} 页', fontSize: 8, textAlign: 'right' })

  return JSON.stringify({
    paperSize: 'A4',
    paperWidth: 210,
    paperHeight: 297,
    marginTop: 10,
    marginBottom: 10,
    marginLeft: 10,
    marginRight: 10,
    components,
  })
}

/**
 * 生成状态节点模板 JSON
 */
function generateStatusTemplate(spec: TemplateSpec, _status: number, statusLabel: string): string {
  const base = JSON.parse(generateDefaultTemplate(spec))
  const components: Record<string, any>[] = base.components

  // 确定标题后缀和水印
  let titleSuffix = ''
  let watermark = ''
  let borderStyle: Record<string, any> = {}

  switch (statusLabel) {
    case '草稿':
      titleSuffix = '（申请单）'
      break
    case '审批中':
    case '待审批':
    case '已审核':
      titleSuffix = '（审批中）'
      watermark = '审批中'
      // 审批意见区
      components.push({ type: 'label', x: 10, y: 232, w: 25, h: 5, content: '审批意见：', fontSize: 9 })
      components.push({ type: 'field', x: 35, y: 232, w: 140, h: 20, field: 'approvalComment', fontSize: 9 })
      break
    case '已通过':
    case '已完成':
    case '已过账':
      titleSuffix = '（已生效）'
      watermark = '已生效'
      break
    case '已驳回':
    case '已关闭':
    case '已冲销':
      titleSuffix = '（已作废）'
      watermark = '已作废'
      borderStyle = { border: '2px solid #ff4d4f' }
      break
  }

  // 更新标题
  const titleComp = components.find(c => c.type === 'label' && c.fontSize === 18)
  if (titleComp) titleComp.content = spec.docTitle + titleSuffix

  // 水印
  if (watermark) {
    components.push({
      type: 'label', x: 40, y: 80, w: 130, h: 60,
      content: watermark,
      fontSize: 48, textAlign: 'center', opacity: 0.1, rotation: -30,
    })
  }

  base.components = components
  return JSON.stringify(base)
}

// ═══════════════════════════════════════════════════════════════
// 种子执行函数
// ═══════════════════════════════════════════════════════════════

interface SeedResult {
  pageCode: string
  templateName: string
  success: boolean
  error?: string
}

/**
 * 为单个模板规格创建模板
 */
async function seedSpec(spec: TemplateSpec): Promise<SeedResult[]> {
  const results: SeedResult[] = []

  // 为每个 pageCode 创建默认模板
  for (const pageCode of spec.pageCodes) {
    const defaultJson = generateDefaultTemplate(spec)
    try {
      const req: PrintTemplateCreateRequest = {
        pageCode,
        templateName: `[默认] ${spec.docTitle}`,
        templateJson: defaultJson,
        paperSize: 'A4',
        paperWidth: 210,
        paperHeight: 297,
        margins: { top: 10, bottom: 10, left: 10, right: 10 },
      }
      await printingApi.createTemplate(req)
      results.push({ pageCode, templateName: `[默认] ${spec.docTitle}`, success: true })
    } catch (err: any) {
      // 409可能表示已存在同名模板
      if (err?.response?.status === 409 || err?.message?.includes('已存在')) {
        results.push({ pageCode, templateName: `[默认] ${spec.docTitle}`, success: true })
      } else {
        console.error(`❌ [默认] ${spec.docTitle} → pageCode=${pageCode}`, err?.message)
        results.push({ pageCode, templateName: `[默认] ${spec.docTitle}`, success: false, error: err?.message })
      }
    }
  }

  // 有状态流转的生成节点模板（只对第一个 pageCode 生成）
  if (spec.statusMap && spec.pageCodes.length > 0) {
    const primaryPageCode = spec.pageCodes[0]
    for (const [status, info] of Object.entries(spec.statusMap)) {
      const nodeJson = generateStatusTemplate(spec, Number(status), info.label)
      const nodeName = `[${info.label}] ${spec.docTitle}`

      try {
        const req: PrintTemplateCreateRequest = {
          pageCode: primaryPageCode,
          templateName: nodeName,
          templateJson: nodeJson,
          paperSize: 'A4',
          paperWidth: 210,
          paperHeight: 297,
          margins: { top: 10, bottom: 10, left: 10, right: 10 },
        }
        await printingApi.createTemplate(req)
        results.push({ pageCode: primaryPageCode, templateName: nodeName, success: true })
      } catch (err: any) {
        if (err?.response?.status === 409 || err?.message?.includes('已存在')) {
          results.push({ pageCode: primaryPageCode, templateName: nodeName, success: true })
        } else {
          console.error(`❌ [节点] ${nodeName} → pageCode=${primaryPageCode}`, err?.message)
          results.push({ pageCode: primaryPageCode, templateName: nodeName, success: false, error: err?.message })
        }
      }
    }
  }

  return results
}

/**
 * 种子入口：为所有业务页面生成模板
 */
export async function seedAllTemplates(): Promise<{ total: number; success: number; fail: number; results: SeedResult[] }> {
  console.warn('══════════════════════════════════════════════════')
  console.warn(`  打印模板种子脚本启动`)
  console.warn(`  待处理业务类型: ${ALL_SPECS.length}`)
  console.warn('══════════════════════════════════════════════════')

  const allResults: SeedResult[] = []

  for (let i = 0; i < ALL_SPECS.length; i++) {
    const spec = ALL_SPECS[i]
    const results = await seedSpec(spec)
    allResults.push(...results)
  }

  const total = allResults.length
  const success = allResults.filter(r => r.success).length
  const fail = total - success

  console.warn('\n══════════════════════════════════════════════════')
  console.warn(`  种子脚本执行完成`)
  console.warn(`  模板操作: ${total}`)
  console.warn(`  成功: ${success}`)
  console.warn(`  失败: ${fail}`)
  if (fail > 0) {
    console.warn('  失败明细:')
    allResults.filter(r => !r.success).forEach(r => {
      console.warn(`    - [${r.pageCode}] ${r.templateName}: ${r.error}`)
    })
  }
  console.warn('══════════════════════════════════════════════════\n')

  return { total, success, fail, results: allResults }
}

// 控制台快捷执行
if (typeof window !== 'undefined') {
  (window as any).seedAllTemplates = seedAllTemplates
}
