import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// 用户信息
export interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  status: number
  roles: string[]
  permissions: string[]
}

// 登录表单
export interface LoginForm {
  username: string
  password: string
  tenantId: number
}

// 用户API
export const userApi = {
  // 登录
  login(data: LoginForm): Promise<ApiResponse<string>> {
    return request.post('/user/login', data)
  },

  // 登出
  logout(): Promise<ApiResponse<void>> {
    return request.post('/user/logout')
  },

  // 获取用户信息
  getUserInfo(): Promise<ApiResponse<UserInfo>> {
    return request.get('/user/info')
  }
}

export default userApi