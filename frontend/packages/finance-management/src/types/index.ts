// 财务管理类型定义

// 会计凭证类型
export interface AccountingEntry {
  id: string
  entryDate: string
  voucherNumber: string
  summary: string
  debitAmount: number
  creditAmount: number
  accountCode: string
  accountName: string
  status: 'draft' | 'posted' | 'reversed'
  createdBy: string
  createdAt: string
  updatedAt: string
}

// 预算类型
export interface BudgetItem {
  id: string
  budgetCode: string
  budgetName: string
  fiscalYear: number
  period: 'monthly' | 'quarterly' | 'yearly'
  plannedAmount: number
  actualAmount: number
  variance: number
  variancePercentage: number
  status: 'draft' | 'approved' | 'executing' | 'closed'
}

// 支付交易类型
export interface PaymentTransaction {
  id: string
  transactionId: string
  paymentDate: string
  payer: string
  payee: string
  amount: number
  currency: string
  paymentMethod: 'alipay' | 'wechat' | 'bank_transfer' | 'cash'
  status: 'pending' | 'processing' | 'completed' | 'failed' | 'refunded'
  description?: string
}

// 税务计算类型
export interface TaxCalculation {
  taxableAmount: number
  taxRate: number
  taxAmount: number
  totalAmount: number
  taxType: 'vat' | 'income' | 'business' | 'other'
  calculationMethod: 'inclusive' | 'exclusive'
}

// 财务报表类型
export interface FinancialReport {
  id: string
  reportType: 'balance_sheet' | 'income_statement' | 'cash_flow' | 'budget_variance'
  period: string
  generatedAt: string
  data: Record<string, any>
  format: 'pdf' | 'excel' | 'csv'
  downloadUrl?: string
}

// API响应类型
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

// 分页参数
export interface PaginationParams {
  page: number
  pageSize: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

// 分页响应
export interface PaginatedResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}