import axios from 'axios'
import { useUserStore } from '@/stores/user'
import { currentShopTenantId } from '@/utils/shop'

// ── 数据接口定义 ──
export interface BannerItem {
  id?: string | number
  imageUrl?: string
  title?: string
  linkUrl?: string
  sortOrder?: number
  status?: number
}

export interface CategoryItem {
  id: string | number
  name?: string
  icon?: string
}

export interface ProductItem {
  id: string | number
  name?: string
  description?: string
  price?: number
  originalPrice?: number
  image?: string
  images?: string[]
  detailImages?: string[]
  stock?: number
  salesCount?: number
  status?: string
  isHot?: boolean
  isNew?: boolean
  isPromotion?: boolean
  skus?: SkuItem[]
}

export interface SkuItem {
  id: string | number
  name?: string
  price?: number
  stock?: number
}

/**
 * 收货地址（与后端 AddressDTO / 表 mall_address 对齐）。
 * 2026-09-26：原先这里是 name/tel/province/city/district/detail 那一套，
 * 而表实际是 region（省市区一个串）+ address（详细）—— 对不上就会 500。
 */
export interface AddressItem {
  id: string | number
  consignee?: string
  phone?: string
  region?: string
  address?: string
  isDefault?: boolean
}

export interface OrderItem {
  id?: string | number
  productId?: string
  productName?: string
  productImage?: string
  image?: string
  name?: string
  price?: number
  quantity?: number
  subtotal?: number
  spec?: string
}

export interface OrderInfo {
  id?: string | number
  orderNo?: string
  totalAmount?: number
  productAmount?: number
  freight?: number
  status?: string
  paymentMethod?: string
  createTime?: string
  items?: OrderItem[]
  address?: AddressItem
  remark?: string
}

export interface LogisticsItem {
  time?: string
  content?: string
}

/**
 * 本实例的**响应拦截器已经把 `AxiosResponse` 解成业务体**（见下方 interceptors.response）。
 * 但 axios 的静态类型并不知道这件事 —— `axios.create()` 仍声明成返回 `AxiosResponse<T>`，
 * 于是 `request.get<{data: AddressItem[]}>(...)` 的 `res.data` 在类型上是整个信封
 * `{data: AddressItem[]}`、而**运行期**它就是里面的数组。
 *
 * 后果是"运行对、类型错"：调用方写 `res.data`（正确）却被 TS 判为把对象赋给数组。
 * 这里用一个最小的 `HttpClient` 接口把类型对齐到事实：**resolve 出来就是业务体**。
 * （不改成 `AxiosResponse` 是因为那会与拦截器的实际行为不符，反而要所有调用点加 `.data.data`。）
 */
interface HttpClient {
  get<T = any>(url: string, config?: any): Promise<T>
  post<T = any>(url: string, data?: any, config?: any): Promise<T>
  put<T = any>(url: string, data?: any, config?: any): Promise<T>
  delete<T = any>(url: string, config?: any): Promise<T>
}

const axiosInstance = axios.create({
  baseURL: '/api/v1/mall',
  timeout: 10000
})

axiosInstance.interceptors.request.use(
  (config: any) => {
    const userStore = useUserStore()
    if (userStore.token) {
      // ⚠️ 2026-09-26 修 P0：这里原本写的是自定义头 `Sa-Token: <token>`，
      //    而后端 sa-token 配置是 `token-name: Authorization`（+ `Bearer` 前缀），
      //    pc-admin 也是发 `Authorization: Bearer <token>`。
      //    头名对不上 ⇒ **登录成功但之后每个请求都 401**（真机实测：
      //    `Sa-Token` 头 401 / `Authorization: Bearer` 200），
      //    C 端所有需登录页面（购物车/我的/订单/地址/资料）实际都进不去。
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    // 店铺识别：游客没有会话，后端只能靠这个头知道"游客在逛哪家店"。
    // 不带 ⇒ 所有 C 端接口 400「无法确定店铺」（见 utils/shop.ts 注释）。
    const tenantId = currentShopTenantId()
    if (tenantId) {
      config.headers['X-Tenant-Id'] = tenantId
    }
    return config
  },
  (error: any) => Promise.reject(error)
)

axiosInstance.interceptors.response.use(
  (response: any) => response.data,
  (error: any) => {
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      // 重定向到登录页
      const currentPath = window.location.pathname
      if (currentPath !== '/login' && currentPath !== '/register') {
        window.location.href = '/login?redirect=' + encodeURIComponent(currentPath)
      }
    }
    return Promise.reject(error)
  }
)

/**
 * 对外请求器：**resolve 出来就是业务体**（拦截器已解包）。
 * 实例本体是 `axiosInstance`（保留 AxiosInstance 类型，拦截器要用），
 * 这里只做一层类型对齐，避免调用方被迫写 `res.data.data`。
 */
const request = axiosInstance as unknown as HttpClient

// ── 身份体系 ──
export interface IdentityItem {
  partyId?: number
  type?: 'MEMBER' | 'ENTERPRISE'
  name?: string
  code?: string
  memberCardNo?: string
  phone?: string
}

export const api = {
  /** 店铺配置（2026-09-26 新增，`GET /v1/mall/shop/config`，游客可达）
   *  返回白名单字段：店铺名/logo/主题色/展示与交易开关；不含任何凭据。 */
  shop: {
    getConfig: () => request.get('/shop/config')
  },

  /** 公告（`GET /v1/mall/notice/list`，游客可达；原管理端路径 /erp/mall/notice 已迁移） */
  notice: {
    getList: (limit = 10) => request.get('/notice/list', { params: { limit } })
  },

  /** 商品标签（`GET /v1/mall/tags`，游客可达）
   *  用途：**分类页最顶部的标签栏** + 商品卡角标 code→name 翻译 */
  tag: {
    getList: () => request.get('/tags')
  },

  auth: {
    login: (data: { username: string; password: string }) => request.post('/auth/login', data),
    register: (data: any) => request.post('/auth/register', data),
    logout: () => request.post('/auth/logout'),
    refreshToken: () => request.post('/auth/refresh-token'),
    /** 获取当前用户可切换的所有身份（个人会员 + 企业客户） */
    getIdentities: () => request.get<{ data: IdentityItem[] }>('/auth/identities'),
    /** 切换下单身份 */
    switchIdentity: (partyId: number) => request.post('/auth/switch-identity', null, { params: { partyId } })
  },

  user: {
    getInfo: () => request.get('/user/info'),
    updateProfile: (data: any) => request.put('/user/profile', data),
    getAddresses: () => request.get<{ data: AddressItem[] }>('/user/addresses'),
    addAddress: (data: any) => request.post('/user/addresses', data),
    updateAddress: (id: string, data: any) => request.put(`/user/addresses/${id}`, data),
    deleteAddress: (id: string) => request.delete(`/user/addresses/${id}`),
    setDefaultAddress: (id: string) => request.put(`/user/addresses/${id}/default`)
  },

  product: {
    getList: (params: any) => request.get('/products', { params }),
    getDetail: (id: string) => request.get(`/products/${id}`),
    getCategories: () => request.get('/products/categories'),
    getCategoryProducts: (categoryId: string, params: any) =>
      request.get(`/products/categories/${categoryId}/products`, { params }),
    search: (keyword: string, params: any) => request.get('/products/search', { params: { keyword, ...params } }),
    getRecommendations: () => request.get('/products/recommendations'),
    getHotProducts: () => request.get('/products/hot'),
    getBanners: () => request.get('/products/banners')
  },

  cart: {
    getList: () => request.get('/cart'),
    add: (data: { productId: string; quantity: number }) => request.post('/cart', data),
    update: (id: string, quantity: number) => request.put(`/cart/${id}`, { quantity }),
    remove: (id: string) => request.delete(`/cart/${id}`),
    clear: () => request.delete('/cart'),
    checkStock: (items: any[]) => request.post('/cart/check-stock', { items })
  },

  order: {
    create: (data: any) => request.post('/orders', data),
    getList: (params: any) => request.get('/orders', { params }),
    getDetail: (id: string) => request.get(`/orders/${id}`),
    cancel: (id: string) => request.put(`/orders/${id}/cancel`),
    confirm: (id: string) => request.put(`/orders/${id}/confirm`),
    getPaymentMethods: () => request.get('/orders/payment-methods'),
    // ⚠️ 2026-09-23 删除 `pay`：后端 `POST /orders/{id}/pay` 已移除（它只把订单置为已付、
    //    不产生支付记录，等于"白拿单"通道）。付款一律走下面的 payment.createPayment +
    //    渠道回调驱动订单状态。详见 TRADE_MODULE_AUDIT_20260923.md P0-5。
    /** F-06 物流信息：发货信息（物流公司/运单号）+ traces（真实轨迹待承运商对接） */
    track: (id: string) => request.get(`/orders/${id}/track`),
    /** F-05 订单状态计数：五宫格 / 顶部 Tab 角标（口径与列表一致） */
    getCounts: () => request.get('/orders/counts')
  },

  payment: {
    /** 发起支付：拿到渠道参数后跳转/唤起收银台 */
    createPayment: (data: any) => request.post('/payments', data),
    /** 轮询支付结果（回调由渠道服务端到服务端调用，前端只查状态） */
    getPaymentStatus: (id: string) => request.get(`/payments/${id}/status`)
    // ⚠️ 2026-09-23 删除 `callback`：支付回调只能由渠道服务器调用后端，
    //    前端调用它既无意义（会被 Sa-Token 拦成 401）又会被误当成"支付完成"的信号。
  }
}

export default api