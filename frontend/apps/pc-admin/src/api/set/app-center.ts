import request from '@/utils/request'

/**
 * 设置 → 系统配置 → 应用中心（菜单 80625）—— 租户级「已安装/已开通模块」只读总览。
 *
 * · 后端 `SetAppCenterController`（`/api/set/app-center`）**只读**：无开通/停用/续费入口
 *   （授权下发属平台侧，在系统模块）。
 * · 租户 id 一律由后端从登录会话取，前端不传（传了也会被忽略，见控制器注释）。
 * · 路径一律写相对路径（axios baseURL 已是 /api，写成 /api/... 会双前缀 404）。
 * · 拦截器已拆包：`Result.data` 直接就是返回值，**不要再取 res.data**（历史 P0 陷阱）。
 */

/** 租户配额用量（sys_tenant_quota；表无本租户行时各字段为 null → 页面显示「未配置」） */
export interface AppCenterQuota {
  /** 用户数上限 */
  maxUsers: number | null
  /** 已用用户数 */
  usedUsers: number | null
  /** 存储配额（文本，如 "10GB"） */
  maxStorage: string | null
  /** 已用存储（文本，如 "3GB"） */
  usedStorage: string | null
  /** API 调用上限（每月） */
  maxApiCalls: number | null
  /** 已用 API 调用数 */
  usedApiCalls: number | null
}

/** 概览卡数据 */
export interface AppCenterOverview {
  /** 公司名称（sys_tenant.tenant_name） */
  companyName: string | null
  /** 租户编码 */
  tenantCode: string | null
  /** 租户等级/套餐标识（sys_tenant.level，可为空 → 「未设置」） */
  tenantLevel: string | null
  /** 到期日期（yyyy-MM-dd；null = 永久 / 未设置） */
  expireDate: string | null
  /** 距到期剩余天数（到期日期为空时为 null） */
  expireDays: number | null
  /** 配额用量 */
  quota: AppCenterQuota | null
  /**
   * 短信用量（真实来源：mkt_sms_setting 配额 + mkt_sms_record 本月发送记录）。
   * 无本租户配额行时 available=false，页面按 reason 如实说明（不显示 0 余量）
   */
  sms: AppCenterSmsUsage | null
  /** 物流查询次数：本系统确实无计量来源 → available 恒为 false，reason 说明缺什么 */
  logisticsQuery: AppCenterGapItem | null
  /** 系统已安装模块数（sys_module） */
  installedModuleCount: number
  /** 本租户已开通模块数（sys_tenant_module） */
  openedModuleCount: number
}

/**
 * 短信用量（后端 `/overview` 的 sms 段）。
 * 口径：配额 = mkt_sms_setting.quota_total，已用 = quota_used（发送侧每次发送累加），
 * 余量 = 配额 - 已用；本月发送 = mkt_sms_record 本月记录行数（与配额计数器是两套口径，各自标注）。
 */
export interface AppCenterSmsUsage {
  /** 是否有本租户的短信配额行 */
  available: boolean
  /** 总配额（条） */
  quotaTotal?: number | null
  /** 已用（条） */
  quotaUsed?: number | null
  /** 剩余（条）；配额/已用缺失时为 null（不当作 0） */
  quotaRemain?: number | null
  /** 本月发送记录条数（mkt_sms_record） */
  monthSent?: number | null
  /** 数据来源说明 */
  source?: string
  /** available=false 时的原因（如实告知缺什么） */
  reason?: string
}

/** 「本系统暂不提供的能力」条目（如实说明原因，不返回数字） */
export interface AppCenterGapItem {
  available: boolean
  reason?: string
}

/** 功能模块卡 */
export interface AppCenterModule {
  /** 模块编码（注册表口径，如 sale / purchase / warehouse） */
  moduleCode: string
  /** 模块名称 */
  moduleName: string | null
  /** 注册表版本号（不在注册表中的历史编码为 null） */
  version: string | null
  /** 注册表描述 */
  description: string | null
  /** 注册表状态：0 = 禁用，1 = 启用；null = 不在注册表 */
  sysStatus: number | null
  /** 注册表排序号 */
  sortOrder: number | null
  /** 本租户是否已开通（sys_tenant_module 有记录） */
  licensed: boolean
  /** 开通类型：permanent / auto_renew / manual（未开通为 null） */
  purchaseType: string | null
  /** 到期日期（yyyy-MM-dd；null = 永久，表注释口径） */
  expireDate: string | null
  /** 开通记录状态：0 = 正常，1 = 停用（未开通为 null） */
  licenseStatus: number | null
}

/** 「短信及其他」区的能力入口（本系统真实存在的租户端菜单） */
export interface AppCenterCapability {
  /** 菜单编码（sys_menu.menu_code） */
  menuCode: string
  /** 菜单名称 */
  menuName: string | null
  /** 路由路径（用于跳转） */
  path: string | null
  /** 图标名 */
  icon: string | null
}

export const appCenterApi = {
  /** 概览：公司名称 / 到期日期（含剩余天数）/ 配额用量 / 模块计数 / 短信用量 / 物流查询缺口说明 */
  overview(): Promise<AppCenterOverview> {
    return request.get('/set/app-center/overview')
  },

  /** 功能模块清单：系统已安装模块 + 本租户是否已开通 */
  modules(): Promise<AppCenterModule[]> {
    return request.get('/set/app-center/modules')
  },

  /** 「短信及其他」能力入口（仅返回本系统真实存在的菜单） */
  capabilities(): Promise<AppCenterCapability[]> {
    return request.get('/set/app-center/capabilities')
  }
}

export default appCenterApi
