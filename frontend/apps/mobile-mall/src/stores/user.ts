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
    token.value = res.data?.token || ''
    user.value = res.data?.user || res.data
    localStorage.setItem('token', token.value)
    if (user.value) {
      localStorage.setItem('user', JSON.stringify(user.value))
    }
    return res
  }

  const logout = () => {
    user.value = null
    token.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    // 异步调用后端退出
    api.auth.logout().catch(() => {})
  }

  const fetchUserInfo = async () => {
    if (!token.value) return
    const res = await api.user.getInfo()
    user.value = res.data?.user || res.data
    if (user.value) {
      localStorage.setItem('user', JSON.stringify(user.value))
    }
  }

  const setUser = (u: User | null) => {
    user.value = u
    if (u) {
      localStorage.setItem('user', JSON.stringify(u))
    } else {
      localStorage.removeItem('user')
    }
  }

  const setToken = (t: string) => {
    token.value = t
    if (t) {
      localStorage.setItem('token', t)
    } else {
      localStorage.removeItem('token')
    }
  }

  const updateProfile = async (data: Partial<User>) => {
    const res = await api.user.updateProfile(data)
    user.value = res.data?.user || res.data
    return res
  }

  const init = () => {
    const savedToken = localStorage.getItem('token')
    if (savedToken) {
      token.value = savedToken
      const savedUser = localStorage.getItem('user')
      if (savedUser) {
        try { user.value = JSON.parse(savedUser) } catch {}
      }
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
    setUser,
    setToken,
    init
  }
})
