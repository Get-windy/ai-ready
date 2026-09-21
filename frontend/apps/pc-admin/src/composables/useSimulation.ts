/**
 * 权限模拟（以某用户身份预览）
 *
 * 用**模块级单例状态**实现跨组件同步：用户管理页点「以该用户身份预览」后，
 * 顶栏的模拟横幅会自动出现，无需事件总线或全局 store 接线。
 *
 * 模拟开启后由服务端会话承载，后续所有请求的权限判定都按被模拟用户计算
 * （后端 StpInterfaceImpl#resolveEffectiveUserId），因此**刷新页面、切换路由都持续生效**。
 */
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { simulationApi, type SimulationStatus } from '@/api/simulation'
import { useUserStore } from '@/stores/user'

/** 模块级共享状态（单例） */
const status = ref<SimulationStatus>({
  simulating: false,
  targetUserId: null,
  actualUserId: null,
  reason: null,
})
const loading = ref(false)

/** 兼容拦截器是否已拆包：已拆包时顶层就有 simulating 字段 */
function unwrap<T>(res: unknown): T | undefined {
  const raw = res as (T & { data?: T }) | undefined
  if (raw && typeof raw === 'object' && 'simulating' in raw) return raw as T
  return (raw?.data as T) ?? undefined
}

export function useSimulation() {
  // 在调用方（组件 setup）上下文解析 store，避免在模块作用域调用 Pinia
  const userStore = useUserStore()

  /**
   * 拉取当前模拟状态（应用启动、页面刷新后调用）。
   *
   * 无 system:simulate 权限的用户**不发起请求** —— 该接口自身有权限校验，
   * 否则每个普通用户每次进系统都会产生一个 403 与一条警告日志。
   */
  async function refresh() {
    if (!userStore.hasPermission('system:simulate')) return
    try {
      const data = unwrap<SimulationStatus>(await simulationApi.status())
      if (data) {
        status.value = data
      }
    } catch (err) {
      // 无 system:simulate 权限的普通用户拿不到状态，属正常情况，不打扰
      console.warn('[权限模拟] 查询模拟状态失败（可能无 system:simulate 权限）', err)
    }
  }

  /** 开始以目标用户身份预览 */
  async function start(targetUserId: number | string, reason?: string) {
    loading.value = true
    try {
      await simulationApi.start(targetUserId, reason)
      await refresh()
      message.success('已开始以该用户身份预览权限，可在顶栏结束')
    } catch (err: any) {
      message.error(err?.message || '开始权限预览失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  /** 结束预览 */
  async function stop() {
    loading.value = true
    try {
      await simulationApi.stop()
      await refresh()
      message.success('已结束权限预览')
    } catch (err: any) {
      message.error(err?.message || '结束权限预览失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  return { status, loading, refresh, start, stop }
}
