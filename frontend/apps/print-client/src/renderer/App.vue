<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Sidebar from '@/components/layout/Sidebar.vue'
import Header from '@/components/layout/Header.vue'
import StatusIndicator from '@/components/common/StatusIndicator.vue'

const router = useRouter()
const route = useRoute()

const connectionStatus = ref<'connected' | 'disconnected' | 'connecting'>('disconnected')
const taskCount = ref(0)
const isLoggedIn = ref(false)

// 当前路由是否是认证页面（登录/注册）
const isAuthPage = computed(() => {
  return route.path === '/login' || route.path === '/register'
})

onMounted(async () => {
  // 检查登录状态
  const authState = await window.electronAPI.auth.getAuthState()
  isLoggedIn.value = authState.isLoggedIn

  // 自动登录检查（如果已登录但页面未刷新，此步跳过）
  if (!isLoggedIn.value && route.path !== '/login' && route.path !== '/register') {
    router.push('/login')
    return
  }

  // 仅非登录页初始化连接状态
  if (isLoggedIn.value) {
    try {
      const statusResp = await window.electronAPI.connection.getStatus()
      connectionStatus.value = statusResp?.isConnected ? 'connected' : 'disconnected'

      const tasks = await window.electronAPI.tasks.getTasks()
      taskCount.value = tasks.filter((t: any) => t.status === 'pending' || t.status === 'queued').length
    } catch {
      // 忽略连接错误
    }

    window.electronAPI.events.onStatusUpdate((status: any) => {
      connectionStatus.value = status.isConnected ? 'connected' : 'disconnected'
      taskCount.value = status.pendingTasks ?? 0
    })

    window.electronAPI.events.onTaskReceived((task: any) => {
      taskCount.value++
    })

    window.electronAPI.events.onNavigate((path: string) => {
      router.push(path)
    })
  }
})

onUnmounted(() => {
  window.electronAPI.events.removeAllListeners()
})
</script>

<template>
  <!-- 登录/注册页面使用独立布局 -->
  <template v-if="isAuthPage">
    <router-view />
  </template>

  <!-- 主布局（需要登录） -->
  <template v-else-if="isLoggedIn">
    <div class="print-client-app">
      <Sidebar :task-count="taskCount" />

      <div class="main-container">
        <Header :connection-status="connectionStatus" />

        <main class="content-area">
          <router-view v-slot="{ Component }">
            <keep-alive>
              <component :is="Component" />
            </keep-alive>
          </router-view>
        </main>
      </div>

      <StatusIndicator :status="connectionStatus" />
    </div>
  </template>

  <!-- 加载中 -->
  <template v-else>
    <div class="loading-screen">
      <div class="loading-spinner"></div>
      <p class="loading-text">正在加载...</p>
    </div>
  </template>
</template>

<style lang="scss">
.print-client-app {
  display: flex;
  height: 100vh;
  background: #f5f7fa;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content-area {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.loading-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: #f5f7fa;

  .loading-spinner {
    width: 36px;
    height: 36px;
    border: 3px solid #e0e0e0;
    border-top-color: #1988fa;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  .loading-text {
    margin-top: 16px;
    color: #969799;
    font-size: 14px;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
