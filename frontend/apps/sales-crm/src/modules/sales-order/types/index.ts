// 销售订单管理类型定义
export interface SalesOrderItem {
  id: string
  productId: string
  productName: string
  productCode: string
  unitPrice: number
  quantity: number
  discount: number
  taxRate: number
  subtotal: number
  total: number
  specifications?: string
  remarks?: string
}

export interface SalesOrderCustomer {
  id: string
  name: string
  code: string
  contactPerson: string
  phone: string
  email?: string
  address?: string
  taxNumber?: string
}

export enum OrderStatus {
  DRAFT = 'draft', // 草稿
  PENDING = 'pending', // 待审核
  APPROVED = 'approved', // 已审核
  CONFIRMED = 'confirmed', // 已确认
  IN_PRODUCTION = 'in_production', // 生产中
  READY_FOR_SHIPMENT = 'ready_for_shipment', // 待发货
  SHIPPED = 'shipped', // 已发货
  DELIVERED = 'delivered', // 已送达
  COMPLETED = 'completed', // 已完成
  CANCELLED = 'cancelled', // 已取消
  RETURNED = 'returned', // 已退货
}

export enum PaymentStatus {
  UNPAID = 'unpaid', // 未付款
  PARTIALLY_PAID = 'partially_paid', // 部分付款
  PAID = 'paid', // 已付款
  OVERDUE = 'overdue', // 逾期
  REFUNDED = 'refunded', // 已退款
}

export interface SalesOrder {
  id: string
  orderNumber: string
  customer: SalesOrderCustomer
  orderDate: string
  expectedDeliveryDate?: string
  actualDeliveryDate?: string
  orderItems: SalesOrderItem[]
  subtotal: number
  discountAmount: number
  taxAmount: number
  shippingFee: number
  totalAmount: number
  currency: string
  orderStatus: OrderStatus
  paymentStatus: PaymentStatus
  priority?: 'low' | 'medium' | 'high' | 'urgent'
  remarks?: string
  attachments?: Array<{
    id: string
    name: string
    url: string
    type: string
    size: number
  }>
  createdBy: string
  createdAt: string
  updatedBy?: string
  updatedAt?: string
}

export interface OrderFilterCriteria {
  startDate?: string
  endDate?: string
  customerIds?: string[]
  orderStatuses?: OrderStatus[]
  paymentStatuses?: PaymentStatus[]
  priority?: string[]
  keyword?: string
  page: number
  pageSize: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

export interface OrderStatistics {
  totalOrders: number
  totalAmount: number
  pendingOrders: number
  completedOrders: number
  cancelledOrders: number
  averageOrderValue: number
  topCustomers: Array<{
    customerId: string
    customerName: string
    orderCount: number
    totalAmount: number
  }>
  topProducts: Array<{
    productId: string
    productName: string
    quantitySold: number
    totalAmount: number
  }>
  monthlyTrend: Array<{
    month: string
    orderCount: number
    totalAmount: number
  }>
}

export interface BulkOperationRequest {
  orderIds: string[]
  action: 'approve' | 'reject' | 'ship' | 'cancel' | 'print'
  remarks?: string
}

export interface ImportExportTemplate {
  columns: string[]
  requiredFields: string[]
  sampleData: Record<string, any>[]
}