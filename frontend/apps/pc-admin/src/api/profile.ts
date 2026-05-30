import request, { type ApiResponse } from '@/utils/request'

// 用户基本信息
export interface ProfileInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  email: string
  phone: string
  gender: number // 0: 未知, 1: 男, 2: 女
  roleNames: string[]
  deptName: string
  createTime?: string
}

// 修改密码请求
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

// 偏好设置
export interface PreferenceSettings {
  language: string // zh-CN / en-US
  theme: string // light / dark
  layoutMode: string // side / top / mix
  primaryColor: string
  tagsView: boolean
  fixedHeader: boolean
  sidebarLogo: boolean
}

// 用户信息更新请求
export interface ProfileUpdateRequest {
  nickname?: string
  email?: string
  phone?: string
  gender?: number
  avatar?: string
}

// 个人中心API
export const profileApi = {
  // 获取用户个人信息
  getProfile(): Promise<ApiResponse<ProfileInfo>> {
    return request.get('/profile')
  },

  // 更新用户信息
  updateProfile(data: ProfileUpdateRequest): Promise<ApiResponse<ProfileInfo>> {
    return request.put('/profile', data)
  },

  // 修改密码
  changePassword(data: ChangePasswordRequest): Promise<ApiResponse<void>> {
    return request.put('/profile/password', data)
  },

  // 获取用户偏好设置
  getPreferences(): Promise<ApiResponse<PreferenceSettings>> {
    return request.get('/profile/preferences')
  },

  // 更新用户偏好设置
  updatePreferences(data: PreferenceSettings): Promise<ApiResponse<void>> {
    return request.put('/profile/preferences', data)
  },

  // 上传头像
  uploadAvatar(file: File): Promise<ApiResponse<string>> {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/profile/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

export default profileApi
