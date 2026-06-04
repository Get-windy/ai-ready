/**
 * 通用业务错误处理工具
 * 统一错误显示逻辑，避免各处重复写 error?.response?.data?.message
 */
import { message } from 'ant-design-vue'

/**
 * 显示业务操作错误信息
 * @param error - catch 到的错误对象
 * @param fallback - 未获取到后端消息时的默认文字
 */
export function handleError(error: any, fallback: string = '操作失败') {
  const errMsg = error?.response?.data?.message || error?.message || fallback
  message.error(errMsg)
}

/**
 * 显示成功消息（简写）
 */
export function handleSuccess(msg: string = '操作成功') {
  message.success(msg)
}

/**
 * 带 Loading 状态的异步操作封装
 * @param loadingRef - 用于控制 loading 状态的 ref
 * @param fn - 要执行的异步操作
 * @param successMsg - 成功提示（传空字符串则不提示）
 * @param fallbackError - 失败提示
 */
export async function withLoading<T>(
  loadingRef: { value: boolean },
  fn: () => Promise<T>,
  successMsg: string = '',
  fallbackError: string = '操作失败'
): Promise<T | undefined> {
  loadingRef.value = true
  try {
    const result = await fn()
    if (successMsg) message.success(successMsg)
    return result
  } catch (error: any) {
    handleError(error, fallbackError)
    return undefined
  } finally {
    loadingRef.value = false
  }
}

export default { handleError, handleSuccess, withLoading }
