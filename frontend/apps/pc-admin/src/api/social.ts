import request, { type ApiResponse } from '@/utils/request'

/** 三方平台（钉钉 / 企业微信 / 飞书） */
export interface SocialProvider {
  platform: string
  name: string
}

/** 当前账号的三方绑定状态 */
export interface SocialBindingStatus {
  platform: string
  name: string
  /** 后端是否已配置该平台凭据（未配置则无法绑定） */
  configured: boolean
  /** 当前账号是否已绑定 */
  bound: boolean
  nickname?: string
  bindTime?: string
}

/**
 * 三方登录 / 账号绑定
 *
 * 后端未配置凭据的平台不会出现在 providers 里，前端也就不渲染入口 —— 避免死按钮。
 */
export const socialApi = {
  /** 可用的三方平台 */
  getProviders(): Promise<ApiResponse<SocialProvider[]>> {
    return request.get('/auth/social/providers')
  },

  /**
   * 取三方授权地址（拿到后由前端整页跳转过去）
   *
   * @param mode login=登录（免登录态）/ bind=绑定（需已登录）
   */
  getAuthorizeUrl(platform: string, mode: 'login' | 'bind' = 'login'): Promise<ApiResponse<string>> {
    return request.get(`/auth/social/${platform}/authorize-url`, { params: { mode } })
  },

  /** 用回调带回的一次性票据换取登录结果（Token 或「待选企业」） */
  exchange(ticket: string): Promise<ApiResponse<any>> {
    return request.post('/auth/social/exchange', { ticket }, { _skipAuthRefresh: true } as any)
  },

  /** 我的三方绑定状态 */
  getBindings(): Promise<ApiResponse<SocialBindingStatus[]>> {
    return request.get('/auth/social/bindings')
  },

  /** 解绑 */
  unbind(platform: string): Promise<ApiResponse<void>> {
    return request.delete(`/auth/social/bindings/${platform}`)
  }
}
