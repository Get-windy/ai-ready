/**
 * 写操作（删除/批量删除等）的「后端到底有没有真的做成」判定。
 *
 * 背景：本仓部分写接口把成败放在**返回值**里，而不是靠 HTTP 状态码表达 ——
 *   · `DELETE /api/config/{configKey}`、`POST /api/config/batch-delete` 返回裸 boolean
 *     （false = 配置不存在 / 内置配置不允许删 / 未删满请求条数）；
 *   · `DELETE /api/dict/item/{id}` 等返回 `{ success: boolean }`；
 *   · 其余多数接口返回 `Result.ok(...)` 或 void（HTTP 200 即视为成功）。
 *
 * 前端若只看「请求没抛异常」就弹「删除成功」，会在**什么都没删**的情况下骗过使用者
 * （2026-09-24 系统模块审计 P1：配置 / 字典 / 岗位 / 角色 / 权限 / 菜单 共 6 处同型）。
 *
 * 用法：
 * ```ts
 * const res = await configApi.delete(key)
 * if (isWriteFailed(res)) { message.error('删除失败：内置配置不允许删除'); return }
 * message.success('删除成功')
 * ```
 */
export function isWriteFailed(res: unknown): boolean {
  // 形态一：裸 boolean
  if (res === false) return true
  // 形态二：{ success: false } / { success: true }
  if (res !== null && typeof res === 'object' && (res as { success?: unknown }).success === false) return true
  // 其余（true / Result 包装 / void / undefined）按成功处理：后端抛业务异常时已被拦截器 reject
  return false
}

export default isWriteFailed
