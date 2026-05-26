import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '@/api'

interface User {
  id: number
  phone: string
  nickname: string
  avatar: string
  token: string
}

interface Order {
  id: string
  orderNo: string
  customerName: string
  customerPhone: string
  address: string
  status: string
  totalAmount: number
  createTime: string
}

interface DeliveryStats {
  todayDeliveries: number
  todayCompleted: number
  todayIncome: number
  totalDeliveries: number
  totalIncome: number
}

export const useUserStore = defineStore('delivery-user', () => {
  const user = ref<User | null>(null)
  const token = ref<string>('')
  const isLoggedIn = computed(() => !!token.value)
  const stats = ref<DeliveryStats>({
    todayDeliveries: 0,
    todayCompleted: 0,
    todayIncome: 0,
    totalDeliveries: 0,
    totalIncome: 0
  })

  const setUser = (userData: User) => {
    user.value = userData
    token.value = userData.token || ''
    if (userData.token) {
      localStorage.setItem('delivery_token', userData.token)
      localStorage.setItem('delivery_user', JSON.stringify(userData))
    }
  }

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('delivery_token', newToken)
  }

  const logout = () => {
    user.value = null
    token.value = ''
    localStorage.removeItem('delivery_token')
    localStorage.removeItem('delivery_user')
  }

  const init = () => {
    const savedToken = localStorage.getItem('delivery_token')
    const savedUser = localStorage.getItem('delivery_user')
    if (savedToken && savedUser) {
      token.value = savedToken
      try {
        user.value = JSON.parse(savedUser)
      } catch {
        user.value = null
      }
    }
  }

  const loadStats = async () => {
    try {
      const res = await api.user.getStatistics()
      stats.value = res.data || stats.value
    } catch {
    }
  }

  return {
    user,
    token,
    isLoggedIn,
    stats,
    setUser,
    setToken,
    logout,
    init,
    loadStats
  }
})

export const useOrderStore = defineStore('delivery-order', () => {
  const orders = ref<Order[]>([])
  const currentOrder = ref<Order | null>(null)
  const loading = ref(false)

  const pendingOrders = computed(() => 
    orders.value.filter(o => o.status === 'pending')
  )

  const inProgressOrders = computed(() => 
    orders.value.filter(o => o.status === 'in_progress')
  )

  const completedOrders = computed(() => 
    orders.value.filter(o => o.status === 'completed')
  )

  const loadOrders = async () => {
    loading.value = true
    try {
      const res = await api.order.getList()
      orders.value = res.data || []
    } catch {
      orders.value = []
    } finally {
      loading.value = false
    }
  }

  const loadOrderDetail = async (orderId: string) => {
    loading.value = true
    try {
      const res = await api.order.getDetail(orderId)
      currentOrder.value = res.data || null
    } catch {
      currentOrder.value = null
    } finally {
      loading.value = false
    }
  }

  const acceptOrder = async (orderId: string) => {
    try {
      await api.order.accept(orderId)
      const order = orders.value.find(o => o.id === orderId)
      if (order) {
        order.status = 'accepted'
      }
    } catch {
      throw new Error('接单失败')
    }
  }

  const startDelivery = async (orderId: string) => {
    try {
      await api.order.startDelivery(orderId)
      const order = orders.value.find(o => o.id === orderId)
      if (order) {
        order.status = 'in_progress'
      }
    } catch {
      throw new Error('开始配送失败')
    }
  }

  const completeDelivery = async (orderId: string) => {
    try {
      await api.order.completeDelivery(orderId)
      const order = orders.value.find(o => o.id === orderId)
      if (order) {
        order.status = 'completed'
      }
    } catch {
      throw new Error('完成配送失败')
    }
  }

  const clearCurrentOrder = () => {
    currentOrder.value = null
  }

  return {
    orders,
    currentOrder,
    loading,
    pendingOrders,
    inProgressOrders,
    completedOrders,
    loadOrders,
    loadOrderDetail,
    acceptOrder,
    startDelivery,
    completeDelivery,
    clearCurrentOrder
  }
})