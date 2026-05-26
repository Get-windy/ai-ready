import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { SalesOrder, OrderFilterCriteria, OrderStatistics, BulkOperationRequest } from '../types'

export const useSalesOrderStore = defineStore('salesOrder', () => {
  // 状态
  const orders = ref<SalesOrder[]>([])
  const currentOrder = ref<SalesOrder | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)
  const filterCriteria = ref<OrderFilterCriteria>({
    page: 1,
    pageSize: 20,
    sortBy: 'createdAt',
    sortOrder: 'desc'
  })
  const totalCount = ref(0)
  const statistics = ref<OrderStatistics | null>(null)

  // 计算属性
  const totalPages = computed(() => Math.ceil(totalCount.value / filterCriteria.value.pageSize))
  const filteredOrders = computed(() => {
    let result = orders.value

    // 按关键字过滤
    if (filterCriteria.value.keyword) {
      const keyword = filterCriteria.value.keyword.toLowerCase()
      result = result.filter(order =>
        order.orderNumber.toLowerCase().includes(keyword) ||
        order.customer.name.toLowerCase().includes(keyword) ||
        order.customer.contactPerson?.toLowerCase().includes(keyword) ||
        order.customer.phone?.includes(keyword)
      )
    }

    // 按订单状态过滤
    if (filterCriteria.value.orderStatuses?.length) {
      result = result.filter(order => filterCriteria.value.orderStatuses!.includes(order.orderStatus))
    }

    // 按付款状态过滤
    if (filterCriteria.value.paymentStatuses?.length) {
      result = result.filter(order => filterCriteria.value.paymentStatuses!.includes(order.paymentStatus))
    }

    // 按客户过滤
    if (filterCriteria.value.customerIds?.length) {
      result = result.filter(order => filterCriteria.value.customerIds!.includes(order.customer.id))
    }

    // 按优先级过滤
    if (filterCriteria.value.priority?.length) {
      result = result.filter(order => order.priority && filterCriteria.value.priority!.includes(order.priority))
    }

    // 按日期范围过滤
    if (filterCriteria.value.startDate) {
      result = result.filter(order => order.orderDate >= filterCriteria.value.startDate!)
    }
    if (filterCriteria.value.endDate) {
      result = result.filter(order => order.orderDate <= filterCriteria.value.endDate!)
    }

    // 排序
    if (filterCriteria.value.sortBy) {
      result.sort((a, b) => {
        const aValue = getNestedValue(a, filterCriteria.value.sortBy!)
        const bValue = getNestedValue(b, filterCriteria.value.sortBy!)
        
        if (aValue < bValue) return filterCriteria.value.sortOrder === 'asc' ? -1 : 1
        if (aValue > bValue) return filterCriteria.value.sortOrder === 'asc' ? 1 : -1
        return 0
      })
    }

    return result
  })

  // 辅助函数
  const getNestedValue = (obj: any, path: string) => {
    return path.split('.').reduce((acc, part) => acc?.[part], obj)
  }

  // Actions - 模拟API调用
  const fetchOrders = async () => {
    try {
      isLoading.value = true
      error.value = null

      // 模拟API调用延迟
      await new Promise(resolve => setTimeout(resolve, 500))

      // 模拟数据
      orders.value = generateMockOrders(50)
      totalCount.value = orders.value.length

      console.log('Fetched orders:', orders.value.length)
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch orders'
      console.error('Error fetching orders:', err)
    } finally {
      isLoading.value = false
    }
  }

  const fetchOrderById = async (id: string) => {
    try {
      isLoading.value = true
      error.value = null

      // 模拟API调用延迟
      await new Promise(resolve => setTimeout(resolve, 300))

      // 查找订单或使用模拟数据
      const existingOrder = orders.value.find(order => order.id === id)
      if (existingOrder) {
        currentOrder.value = existingOrder
      } else {
        // 生成模拟订单
        currentOrder.value = generateMockOrder(id)
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch order'
      console.error('Error fetching order:', err)
    } finally {
      isLoading.value = false
    }
  }

  const createOrder = async (orderData: Partial<SalesOrder>) => {
    try {
      isLoading.value = true
      error.value = null

      // 模拟API调用延迟
      await new Promise(resolve => setTimeout(resolve, 800))

      const newOrder: SalesOrder = {
        id: `order_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
        orderNumber: `SO${new Date().getFullYear()}${String(new Date().getMonth() + 1).padStart(2, '0')}${String(orders.value.length + 1).padStart(5, '0')}`,
        ...orderData,
        orderDate: new Date().toISOString().split('T')[0],
        createdAt: new Date().toISOString(),
        createdBy: 'current_user',
      } as SalesOrder

      orders.value.unshift(newOrder)
      totalCount.value += 1
      currentOrder.value = newOrder

      return newOrder
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to create order'
      console.error('Error creating order:', err)
      throw err
    } finally {
      isLoading.value = false
    }
  }

  const updateOrder = async (id: string, updates: Partial<SalesOrder>) => {
    try {
      isLoading.value = true
      error.value = null

      // 模拟API调用延迟
      await new Promise(resolve => setTimeout(resolve, 500))

      const index = orders.value.findIndex(order => order.id === id)
      if (index !== -1) {
        orders.value[index] = {
          ...orders.value[index],
          ...updates,
          updatedAt: new Date().toISOString(),
          updatedBy: 'current_user'
        }

        if (currentOrder.value?.id === id) {
          currentOrder.value = orders.value[index]
        }
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to update order'
      console.error('Error updating order:', err)
      throw err
    } finally {
      isLoading.value = false
    }
  }

  const deleteOrder = async (id: string) => {
    try {
      isLoading.value = true
      error.value = null

      // 模拟API调用延迟
      await new Promise(resolve => setTimeout(resolve, 300))

      orders.value = orders.value.filter(order => order.id !== id)
      totalCount.value -= 1

      if (currentOrder.value?.id === id) {
        currentOrder.value = null
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to delete order'
      console.error('Error deleting order:', err)
      throw err
    } finally {
      isLoading.value = false
    }
  }

  const fetchStatistics = async () => {
    try {
      isLoading.value = true
      error.value = null

      // 模拟API调用延迟
      await new Promise(resolve => setTimeout(resolve, 800))

      // 生成模拟统计
      statistics.value = generateMockStatistics()

      return statistics.value
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch statistics'
      console.error('Error fetching statistics:', err)
      throw err
    } finally {
      isLoading.value = false
    }
  }

  const performBulkOperations = async (request: BulkOperationRequest) => {
    try {
      isLoading.value = true
      error.value = null

      // 模拟API调用延迟
      await new Promise(resolve => setTimeout(resolve, 1000))

      // 更新订单状态
      orders.value.forEach(order => {
        if (request.orderIds.includes(order.id)) {
          switch (request.action) {
            case 'approve':
              order.orderStatus = 'approved'
              break
            case 'reject':
              order.orderStatus = 'cancelled'
              break
            case 'ship':
              order.orderStatus = 'shipped'
              break
            case 'cancel':
              order.orderStatus = 'cancelled'
              break
          }
        }
      })

      return { success: true, processedCount: request.orderIds.length }
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to perform bulk operations'
      console.error('Error performing bulk operations:', err)
      throw err
    } finally {
      isLoading.value = false
    }
  }

  const updateFilter = (newFilter: Partial<OrderFilterCriteria>) => {
    filterCriteria.value = {
      ...filterCriteria.value,
      ...newFilter
    }
  }

  const resetFilter = () => {
    filterCriteria.value = {
      page: 1,
      pageSize: 20,
      sortBy: 'createdAt',
      sortOrder: 'desc'
    }
  }

  const clearError = () => {
    error.value = null
  }

  // 模拟数据生成函数
  const generateMockOrder = (id?: string): SalesOrder => {
    const orderId = id || `order_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    const orderNumber = `SO${new Date().getFullYear()}${String(new Date().getMonth() + 1).padStart(2, '0')}${String(Math.floor(Math.random() * 10000)).padStart(5, '0')}`
    
    return {
      id: orderId,
      orderNumber,
      customer: {
        id: `customer_${Math.floor(Math.random() * 1000)}`,
        name: `客户${Math.floor(Math.random() * 100)}`,
        code: `CUST${String(Math.floor(Math.random() * 1000)).padStart(4, '0')}`,
        contactPerson: `联系人${Math.floor(Math.random() * 100)}`,
        phone: `138${String(Math.floor(Math.random() * 100000000)).padStart(8, '0')}`,
        email: `customer${Math.floor(Math.random() * 100)}@example.com`,
        address: `地址${Math.floor(Math.random() * 100)}`
      },
      orderDate: new Date(Date.now() - Math.floor(Math.random() * 30) * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      expectedDeliveryDate: new Date(Date.now() + Math.floor(Math.random() * 14) * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      orderItems: Array.from({ length: Math.floor(Math.random() * 5) + 1 }, (_, i) => ({
        id: `item_${i}`,
        productId: `product_${Math.floor(Math.random() * 1000)}`,
        productName: `产品${Math.floor(Math.random() * 100)}`,
        productCode: `P${String(Math.floor(Math.random() * 10000)).padStart(5, '0')}`,
        unitPrice: Math.floor(Math.random() * 1000) + 100,
        quantity: Math.floor(Math.random() * 10) + 1,
        discount: Math.floor(Math.random() * 20),
        taxRate: 0.13,
        subtotal: 0,
        total: 0
      })).map(item => ({
        ...item,
        subtotal: item.unitPrice * item.quantity,
        total: item.unitPrice * item.quantity * (1 - item.discount / 100) * (1 + item.taxRate)
      })),
      subtotal: 0,
      discountAmount: 0,
      taxAmount: 0,
      shippingFee: Math.floor(Math.random() * 100),
      totalAmount: 0,
      currency: 'CNY',
      orderStatus: ['draft', 'pending', 'approved', 'confirmed', 'shipped', 'completed'][Math.floor(Math.random() * 6)] as any,
      paymentStatus: ['unpaid', 'partially_paid', 'paid'][Math.floor(Math.random() * 3)] as any,
      priority: ['low', 'medium', 'high'][Math.floor(Math.random() * 3)] as any,
      remarks: `备注${Math.floor(Math.random() * 100)}`,
      createdBy: `user_${Math.floor(Math.random() * 10)}`,
      createdAt: new Date(Date.now() - Math.floor(Math.random() * 30) * 24 * 60 * 60 * 1000).toISOString()
    }
  }

  const generateMockOrders = (count: number): SalesOrder[] => {
    return Array.from({ length: count }, (_, i) => generateMockOrder())
  }

  const generateMockStatistics = (): OrderStatistics => {
    return {
      totalOrders: 156,
      totalAmount: 2456789.50,
      pendingOrders: 23,
      completedOrders: 89,
      cancelledOrders: 12,
      averageOrderValue: 15748.65,
      topCustomers: [
        { customerId: 'customer_001', customerName: '客户A', orderCount: 25, totalAmount: 456789 },
        { customerId: 'customer_002', customerName: '客户B', orderCount: 18, totalAmount: 389456 },
        { customerId: 'customer_003', customerName: '客户C', orderCount: 15, totalAmount: 312345 }
      ],
      topProducts: [
        { productId: 'product_001', productName: '产品A', quantitySold: 156, totalAmount: 234567 },
        { productId: 'product_002', productName: '产品B', quantitySold: 123, totalAmount: 189456 },
        { productId: 'product_003', productName: '产品C', quantitySold: 98, totalAmount: 156789 }
      ],
      monthlyTrend: Array.from({ length: 12 }, (_, i) => ({
        month: `${i + 1}月`,
        orderCount: Math.floor(Math.random() * 30) + 10,
        totalAmount: Math.floor(Math.random() * 500000) + 100000
      }))
    }
  }

  return {
    // 状态
    orders,
    currentOrder,
    isLoading,
    error,
    filterCriteria,
    totalCount,
    statistics,

    // 计算属性
    totalPages,
    filteredOrders,

    // Actions
    fetchOrders,
    fetchOrderById,
    createOrder,
    updateOrder,
    deleteOrder,
    fetchStatistics,
    performBulkOperations,
    updateFilter,
    resetFilter,
    clearError
  }
})