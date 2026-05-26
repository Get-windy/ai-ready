import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '@/api'

export interface User {
  id: string
  username: string
  nickname: string
  phone: string
  email: string
  avatar: string
  department: string
  position: string
  roles: string[]
  permissions: string[]
}

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const token = ref<string>('')
  const isLoggedIn = computed(() => !!token.value)
  
  const pendingApprovalCount = ref(0)
  const todayOrderCount = ref(0)
  const customerCount = ref(0)
  
  const login = async (username: string, password: string) => {
    const res = await api.auth.login({ username, password })
    token.value = res.data.token
    user.value = res.data.user
    localStorage.setItem('token', res.data.token)
    return res
  }
  
  const logout = async () => {
    await api.auth.logout()
    user.value = null
    token.value = ''
    localStorage.removeItem('token')
  }
  
  const fetchUserInfo = async () => {
    if (!token.value) return
    const res = await api.user.getInfo()
    user.value = res.data
  }
  
  const fetchDashboardData = async () => {
    const res = await api.dashboard.getKpiData()
    pendingApprovalCount.value = res.data.pendingApprovals || 0
    todayOrderCount.value = res.data.todayOrders || 0
    customerCount.value = res.data.totalCustomers || 0
  }
  
  const hasPermission = (permission: string) => {
    return user.value?.permissions?.includes(permission) || false
  }
  
  const init = () => {
    const savedToken = localStorage.getItem('token')
    if (savedToken) {
      token.value = savedToken
      fetchUserInfo()
      fetchDashboardData()
    }
  }
  
  return {
    user,
    token,
    isLoggedIn,
    pendingApprovalCount,
    todayOrderCount,
    customerCount,
    login,
    logout,
    fetchUserInfo,
    fetchDashboardData,
    hasPermission,
    init
  }
})