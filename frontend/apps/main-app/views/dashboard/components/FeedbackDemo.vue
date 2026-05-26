<template>
  <div class="feedback-demo">
    <h2 class="demo-title">反馈组件演示</h2>
    <p class="demo-subtitle">AI-Ready 测试环境基础体验优化组件</p>
    
    <!-- 组件导航 -->
    <div class="demo-nav">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="骨架屏" name="skeleton">
          <ARSkeletonDemo />
        </el-tab-pane>
        <el-tab-pane label="加载指示器" name="loading">
          <ARLoadingDemo />
        </el-tab-pane>
        <el-tab-pane label="错误提示" name="error">
          <ARErrorDemo />
        </el-tab-pane>
        <el-tab-pane label="空状态" name="empty">
          <AREmptyDemo />
        </el-tab-pane>
        <el-tab-pane label="告警组件" name="alert">
          <ARAlertDemo />
        </el-tab-pane>
        <el-tab-pane label="KPI卡片" name="kpi">
          <ARKpiDemo />
        </el-tab-pane>
      </el-tabs>
    </div>
    
    <!-- 主题切换 -->
    <div class="theme-switcher">
      <el-switch
        v-model="isDarkMode"
        active-text="深色模式"
        inactive-text="浅色模式"
        @change="toggleTheme"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import ARSkeletonDemo from './demo/ARSkeletonDemo.vue'
import ARLoadingDemo from './demo/ARLoadingDemo.vue'
import ARErrorDemo from './demo/ARErrorDemo.vue'
import AREmptyDemo from './demo/AREmptyDemo.vue'
import ARAlertDemo from './demo/ARAlertDemo.vue'
import ARKpiDemo from './demo/ARKpiDemo.vue'

const activeTab = ref('skeleton')
const isDarkMode = ref(false)

const handleTabClick = (tab: any) => {
  console.log('切换到:', tab.props.name)
}

const toggleTheme = (value: boolean) => {
  const html = document.documentElement
  if (value) {
    html.setAttribute('data-theme', 'dark')
  } else {
    html.removeAttribute('data-theme')
  }
}

onMounted(() => {
  // 检查系统主题偏好
  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
  isDarkMode.value = prefersDark
  if (prefersDark) {
    toggleTheme(true)
  }
})
</script>

<style scoped>
.feedback-demo {
  padding: 24px;
  background: #f5f7fa;
  min-height: 100vh;
}

.demo-title {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  text-align: center;
}

.demo-subtitle {
  margin: 0 0 32px 0;
  font-size: 16px;
  color: #606266;
  text-align: center;
}

.demo-nav {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}

.theme-switcher {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px 0 rgba(0, 0, 0, 0.1);
}

/* 深色模式 */
[data-theme='dark'] .feedback-demo {
  background: #1a1a1a;
}

[data-theme='dark'] .demo-title {
  color: #e8e8e8;
}

[data-theme='dark'] .demo-subtitle {
  color: #a8a8a8;
}

[data-theme='dark'] .demo-nav,
[data-theme='dark'] .theme-switcher {
  background: #1f1f1f;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.3);
}
</style>