/**
 * 店铺（租户）识别。
 *
 * <p><b>为什么必须有这个东西</b>：商城是「一租户一店」的 B2B 模型，后端
 *（`MallGuestAccess.currentShopTenantId()`）解析当前逛的是哪家店，顺序是
 * ① 登录会话里的 tenantId → ② 请求头 `X-Tenant-Id`。游客没有会话，
 * 所以**必须**带上这个头，否则所有 C 端接口一律
 * `400 无法确定店铺：请携带 X-Tenant-Id 请求头，或先登录`
 *（2026-09-23 实测就是这条，见 TRADE_MODULE_AUDIT_20260923.md）。
 *
 * <p>识别优先级（高 → 低）：
 * <ol>
 *   <li>URL 查询参数 `?tenantId=1`（扫码进店 / 分享链接带店，**最高优先级**，
 *       且会写回 localStorage 便于后续刷新沿用）；</li>
 *   <li>localStorage 里上次记住的店铺（用户刷新/二次访问不再需要带参数）；</li>
 *   <li>环境变量 `VITE_DEFAULT_TENANT_ID`（单店部署 / 开发环境兜底）。</li>
 * </ol>
 *
 * <p>⚠️ 明确**不做**的事：不按域名解析（需要 DNS/反代配合，见设计方案 §十），
 * 也不默认回落某个租户 —— 回落等于"蒙一个店"，会把 A 店的商品展示给 B 店的访客。
 * 取不到就返回 null，让后端按"无法确定店铺"拒绝，前端据此提示。
 */

const STORAGE_KEY = 'mall_shop_tenant_id'

/** 只允许纯数字的租户 id，避免把任意字符串塞进请求头 */
function normalize(raw: unknown): string | null {
  if (raw === null || raw === undefined) return null
  const s = String(raw).trim()
  return /^\d+$/.test(s) ? s : null
}

/**
 * 解析并**记住**当前店铺租户 id；解析不到返回 null。
 * 建议在应用启动时调用一次（会顺带把 URL 参数落到 localStorage）。
 */
export function resolveShopTenantId(): string | null {
  // ① URL 参数（扫码/分享链接）
  try {
    const fromUrl = normalize(new URLSearchParams(window.location.search).get('tenantId'))
    if (fromUrl) {
      localStorage.setItem(STORAGE_KEY, fromUrl)
      return fromUrl
    }
  } catch {
    // 非浏览器环境（SSR/测试）忽略
  }

  // ② 上次记住的
  const cached = normalize(localStorage.getItem(STORAGE_KEY))
  if (cached) return cached

  // ③ 环境变量兜底（单店部署/开发）
  const fromEnv = normalize(import.meta.env?.VITE_DEFAULT_TENANT_ID)
  if (fromEnv) {
    localStorage.setItem(STORAGE_KEY, fromEnv)
    return fromEnv
  }

  return null
}

/** 只读当前店铺 id（不写副作用），供请求拦截器使用 */
export function currentShopTenantId(): string | null {
  return normalize(localStorage.getItem(STORAGE_KEY))
}

/** 手动设置店铺（例如"切换店铺"入口） */
export function setShopTenantId(tenantId: string | number): void {
  const v = normalize(tenantId)
  if (v) localStorage.setItem(STORAGE_KEY, v)
}
