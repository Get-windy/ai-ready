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
  level: string
  points: number
  balance: number
}

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const token = ref<string>('')
  const isLoggedIn = computed(() => !!token.value)

  const login = async (username: string, password: string) => {
    const res = await api.auth.login({ username, password })
    token.value = res.data.token
    user.value = res.data.user
    localStorage.setItem('token', res.data.token)
    return res
  }

  const logout = () => {
    user.value = null
    token.value = ''
    localStorage.removeItem('token')
  }

  const fetchUserInfo = async () => {
    if (!token.value) return
    const res = await api.user.getInfo()
    user.value = res.data
  }

  const updateProfile = async (data: Partial<User>) => {
    const res = await api.user.updateProfile(data)
    user.value = res.data
    return res
  }

  const init = () => {
    const savedToken = localStorage.getItem('token')
    if (savedToken) {
      token.value = savedToken
      fetchUserInfo()
    }
  }

  return {
    user,
    token,
    isLoggedIn,
    login,
    logout,
    fetchUserInfo,
    updateProfile,
    init
  }
})