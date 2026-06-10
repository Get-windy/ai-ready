import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// 登录响应
export interface LoginResponse {
  token: string
  tokenName: string
  userId: number
  tenantId: number
  tenantName: string
  tenants: TenantInfo[]
}

// 用户信息
export interface UserInfo {
  userId: number
  username: string
  nickname?: string
  userType?: number
  avatar?: string
  email?: string
  phone?: string
  gender?: number
  status?: number
  deptId?: number
  deptName?: string
  tenantId?: number
  roles: string[]
  permissions: string[]
  createTime?: string
  updateTime?: string
  /** 密码是否已过期（需修改密码） */
  passwordExpired?: boolean
}

// 租户信息
export interface TenantInfo {
  id: number
  tenantName: string
  tenantCode: string
  status: number
}

// 登录表单
export interface LoginForm {
  username: string
  password: string
  tenantName: string
}

// 用户查询参数
export interface UserQuery {
  tenantId?: number
  username?: string
  nickname?: string
  phone?: string
  status?: number
  deptId?: number
  pageNum?: number
  pageSize?: number
}

// 用户API - 修复版
export const userApi = {
  // 登录 - 跳过认证刷新，避免登录失败时触发token刷新
  login(data: LoginForm): Promise<ApiResponse<LoginResponse>> {
    return request.post('/auth/login', {
      username: data.username,
      password: data.password,
      tenantName: data.tenantName
    }, {
      _skipAuthRefresh: true
    } as any)
  },

  // 获取当前用户可访问的租户列表（用于租户切换器）
  getTenants(): Promise<ApiResponse<TenantInfo[]>> {
    return request.get('/auth/tenants')
  },

  // 登出 - 跳过认证刷新，避免无限循环
  logout(): Promise<ApiResponse<void>> {
    return request.post('/auth/logout', {}, { 
      _skipAuthRefresh: true 
    } as any)
  },

  // 获取验证码 - 公开API，跳过认证刷新
  getCaptcha(): Promise<ApiResponse<{ img: string; uuid: string }>> {
    return request.get('/auth/captcha', {
      _skipAuthRefresh: true
    } as any)
  },

  // 获取当前用户信息 - 新增
  getUserInfo(): Promise<ApiResponse<UserInfo>> {
    return request.get('/auth/userinfo')
  },

  // 分页查询用户
  getPage(params: UserQuery): Promise<ApiResponse<PageResponse<UserInfo>>> {
    return request.get('/user/page', params)
  },

  // 获取用户详情
  getById(id: number): Promise<ApiResponse<UserInfo>> {
    return request.get(`/user/${id}`)
  },

  // 创建用户
  create(data: Partial<UserInfo> & { password: string }): Promise<ApiResponse<boolean>> {
    return request.post('/user', data)
  },

  // 更新用户
  update(id: number, data: Partial<UserInfo>): Promise<ApiResponse<boolean>> {
    return request.put(`/user/${id}`, data)
  },

  // 删除用户
  delete(id: number): Promise<ApiResponse<boolean>> {
    return request.delete(`/user/${id}`)
  },

  // 批量删除用户
  batchDelete(ids: number[]): Promise<ApiResponse<boolean>> {
    return request.delete('/user/batch', { data: ids })
  },

  // 重置密码
  resetPassword(id: number, newPassword: string): Promise<ApiResponse<boolean>> {
    return request.put(`/user/${id}/password/reset`, null, { params: { newPassword } })
  },

  // 修改密码
  changePassword(id: number, oldPassword: string, newPassword: string): Promise<ApiResponse<boolean>> {
    return request.put(`/user/${id}/password/change`, null, { params: { oldPassword, newPassword } })
  },

  // 分配角色
  assignRoles(id: number, roleIds: number[]): Promise<ApiResponse<boolean>> {
    return request.post(`/user/${id}/roles`, roleIds)
  },

  // 批量分配角色
  batchAssignRoles(userIds: number[], roleIds: number[]): Promise<ApiResponse<boolean>> {
    return request.post('/user/batch-assign-roles', { userIds, roleIds })
  },

  // 更新用户状态
  updateStatus(id: number, status: number): Promise<ApiResponse<boolean>> {
    return request.put(`/user/${id}/status`, null, { params: { status } })
  },

  // 获取用户列表
  getList(params?: UserQuery): Promise<ApiResponse<UserInfo[]>> {
    return request.get('/user/list', params)
  }
}

export default userApi