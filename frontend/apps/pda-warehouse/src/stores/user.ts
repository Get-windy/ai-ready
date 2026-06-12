import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

interface User {
  id: number
  phone: string
  nickname: string
  avatar: string
  token: string
}

export const useUserStore = defineStore('warehouse-user', () => {
  const user = ref<User | null>(null)
  const token = ref<string>('')
  const isLoggedIn = computed(() => !!token.value)

  const setUser = (userData: User) => {
    user.value = userData
    token.value = userData.token || ''
    if (userData.token) {
      localStorage.setItem('warehouse_token', userData.token)
      localStorage.setItem('warehouse_user', JSON.stringify(userData))
    }
  }

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('warehouse_token', newToken)
  }

  const logout = () => {
    user.value = null
    token.value = ''
    localStorage.removeItem('warehouse_token')
    localStorage.removeItem('warehouse_user')
  }

  const init = () => {
    const savedToken = localStorage.getItem('warehouse_token')
    const savedUser = localStorage.getItem('warehouse_user')
    if (savedToken && savedUser) {
      token.value = savedToken
      try {
        user.value = JSON.parse(savedUser)
      } catch {
        user.value = null
      }
    }
  }

  return {
    user,
    token,
    isLoggedIn,
    setUser,
    setToken,
    logout,
    init,
  }
})
