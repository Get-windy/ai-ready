import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// 用户信息
export interface UserInfo {
  id: number
  tenantId: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  gender: number
  userType: number
  status: number
  deptId: number
  postId: number
  createTime: string
  roles?: string[]
  permissions?: string[]
}

// 登录表单
export interface LoginForm {
  username: string
  password: string
  tenantId: number
}

// 用户查询参数
export interface UserQuery {
  tenantId: number
  username?: string
  status?: number
  deptId?: number
  pageNum?: number
  pageSize?: number
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
  },

  // 分页查询用户
  getPage(params: UserQuery): Promise<ApiResponse<PageResponse<UserInfo>>> {
    return request.get('/user/page', { params })
  },

  // 获取用户详情
  getById(id: number): Promise<ApiResponse<UserInfo>> {
    return request.get(`/user/${id}`)
  },

  // 创建用户
  create(data: Partial<UserInfo> & { password: string }): Promise<ApiResponse<number>> {
    return request.post('/user', data)
  },

  // 更新用户
  update(id: number, data: Partial<UserInfo>): Promise<ApiResponse<void>> {
    return request.put(`/user/${id}`, data)
  },

  // 删除用户
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/user/${id}`)
  },

  // 批量删除用户
  batchDelete(ids: number[]): Promise<ApiResponse<void>> {
    return request.delete('/user/batch', { data: ids })
  },

  // 重置密码
  resetPassword(id: number, newPassword: string): Promise<ApiResponse<void>> {
    return request.put(`/user/${id}/password/reset`, null, { params: { newPassword } })
  },

  // 修改密码
  changePassword(id: number, oldPassword: string, newPassword: string): Promise<ApiResponse<void>> {
    return request.put(`/user/${id}/password/change`, null, { params: { oldPassword, newPassword } })
  },

  // 分配角色
  assignRoles(id: number, roleIds: number[]): Promise<ApiResponse<void>> {
    return request.post(`/user/${id}/roles`, roleIds)
  },

  // 更新用户状态
  updateStatus(id: number, status: number): Promise<ApiResponse<void>> {
    return request.put(`/user/${id}/status`, null, { params: { status } })
  }
}

export default userApi