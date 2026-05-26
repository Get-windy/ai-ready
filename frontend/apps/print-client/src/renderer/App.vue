<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import Sidebar from '@/components/layout/Sidebar.vue'
import Header from '@/components/layout/Header.vue'
import StatusIndicator from '@/components/common/StatusIndicator.vue'

const router = useRouter()
const connectionStatus = ref<'connected' | 'disconnected' | 'connecting'>('disconnected')
const taskCount = ref(0)

onMounted(() => {
  window.electronAPI.connection.getStatus().then((status: any) => {
    connectionStatus.value = status
  })
  
  window.electronAPI.tasks.getTasks({ status: 'pending' }).then((tasks: any[]) => {
    taskCount.value = tasks.length
  })
  
  window.electronAPI.events.onStatusUpdate((status: any) => {
    connectionStatus.value = status.connection
    taskCount.value = status.pendingTasks
  })
  
  window.electronAPI.events.onTaskReceived((task: any) => {
    taskCount.value++
  })
  
  window.electronAPI.events.onNavigate((path: string) => {
    router.push(path)
  })
})

onUnmounted(() => {
  window.electronAPI.events.removeAllListeners()
})
</script>

<template>
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
</style>