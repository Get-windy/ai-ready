<template>
  <el-dialog
    v-model="visible"
    title="搜索"
    width="600px"
    :show-close="true"
    :close-on-click-modal="true"
    :close-on-press-escape="true"
    class="search-dialog"
    aria-labelledby="search-title"
    @opened="focusInput"
  >
    <div class="search-container" role="search">
      <el-input
        ref="searchInput"
        v-model="searchQuery"
        placeholder="搜索面板、指标、告警..."
        :prefix-icon="Search"
        size="large"
        clearable
        aria-label="搜索"
        aria-describedby="search-hint"
        @input="handleSearch"
        @keydown.enter.prevent="selectFirstResult"
        @keydown.down.prevent="navigateResults(1)"
        @keydown.up.prevent="navigateResults(-1)"
      />
      <p id="search-hint" class="search-hint">
        输入关键词搜索，使用 ↑↓ 导航，Enter 选择，Esc 关闭
      </p>

      <!-- Search Results -->
      <div v-if="searchQuery" class="search-results" role="listbox" aria-label="搜索结果">
        <div v-if="filteredResults.length === 0" class="no-results">
          <el-empty description="未找到匹配的结果" />
        </div>

        <template v-else>
          <!-- Panels Section -->
          <div v-if="panelResults.length > 0" class="result-section">
            <h4 class="section-title">面板</h4>
            <div
              v-for="(item, index) in panelResults"
              :key="item.id"
              class="result-item"
              :class="{ 'is-active': activeIndex === getGlobalIndex('panel', index) }"
              role="option"
              :aria-selected="activeIndex === getGlobalIndex('panel', index)"
              @click="selectResult(item)"
              @mouseenter="activeIndex = getGlobalIndex('panel', index)"
            >
              <el-icon class="result-icon"><Grid /></el-icon>
              <div class="result-content">
                <span class="result-title">{{ item.title }}</span>
                <span class="result-desc">{{ item.type }}</span>
              </div>
              <kbd class="result-shortcut">Enter</kbd>
            </div>
          </div>

          <!-- Metrics Section -->
          <div v-if="metricResults.length > 0" class="result-section">
            <h4 class="section-title">指标</h4>
            <div
              v-for="(item, index) in metricResults"
              :key="item.id"
              class="result-item"
              :class="{ 'is-active': activeIndex === getGlobalIndex('metric', index) }"
              role="option"
              :aria-selected="activeIndex === getGlobalIndex('metric', index)"
              @click="selectResult(item)"
              @mouseenter="activeIndex = getGlobalIndex('metric', index)"
            >
              <el-icon class="result-icon"><TrendCharts /></el-icon>
              <div class="result-content">
                <span class="result-title">{{ item.label }}</span>
                <span class="result-desc">{{ item.metric }}</span>
              </div>
            </div>
          </div>

          <!-- Actions Section -->
          <div v-if="actionResults.length > 0" class="result-section">
            <h4 class="section-title">操作</h4>
            <div
              v-for="(item, index) in actionResults"
              :key="item.id"
              class="result-item"
              :class="{ 'is-active': activeIndex === getGlobalIndex('action', index) }"
              role="option"
              :aria-selected="activeIndex === getGlobalIndex('action', index)"
              @click="executeAction(item)"
              @mouseenter="activeIndex = getGlobalIndex('action', index)"
            >
              <el-icon class="result-icon"><component :is="item.icon" /></el-icon>
              <div class="result-content">
                <span class="result-title">{{ item.title }}</span>
                <span class="result-desc">{{ item.description }}</span>
              </div>
              <kbd v-if="item.shortcut" class="result-shortcut">{{ item.shortcut }}</kbd>
            </div>
          </div>
        </template>
      </div>

      <!-- Quick Actions (when no search query) -->
      <div v-else class="quick-actions">
        <h4 class="section-title">快速操作</h4>
        <div class="quick-actions-grid">
          <el-button
            v-for="action in quickActions"
            :key="action.id"
            class="quick-action-btn"
            @click="executeAction(action)"
          >
            <el-icon class="action-icon"><component :is="action.icon" /></el-icon>
            <span>{{ action.title }}</span>
          </el-button>
        </div>

        <h4 class="section-title" style="margin-top: 24px">最近访问</h4>
        <div class="recent-items">
          <div
            v-for="item in recentItems"
            :key="item.id"
            class="recent-item"
            @click="selectResult(item)"
          >
            <el-icon><Clock /></el-icon>
            <span>{{ item.title }}</span>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { Search, Grid, TrendCharts, Refresh, Setting, Download, Clock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

interface SearchResult {
  id: string
  type: 'panel' | 'metric' | 'action'
  title?: string
  label?: string
  metric?: string
  description?: string
  icon?: string
  shortcut?: string
  action?: () => void
}

const props = defineProps<{
  modelValue: boolean
  panels?: Array<{ id: string; title: string; type: string }>
  metrics?: Array<{ id: string; label: string; metric: string }>
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'select-panel': [panelId: string]
  'refresh': []
  'open-settings': []
  'export': []
}>()

const visible = ref(props.modelValue)
const searchQuery = ref('')
const searchInput = ref<HTMLInputElement>()
const activeIndex = ref(0)

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    searchQuery.value = ''
    activeIndex.value = 0
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

// Mock data for panels
const defaultPanels = [
  { id: 'p1', title: '系统健康度', type: 'kpi' },
  { id: 'p2', title: '平均响应时间', type: 'kpi' },
  { id: 'p3', title: '当前吞吐量', type: 'kpi' },
  { id: 'p4', title: '错误率', type: 'kpi' },
  { id: 'p5', title: 'CPU使用率', type: 'chart' },
  { id: 'p6', title: '内存使用情况', type: 'chart' },
  { id: 'p7', title: '请求趋势', type: 'chart' },
  { id: 'p8', title: '最近告警', type: 'alert' },
  { id: 'p9', title: '服务状态', type: 'status' }
]

// Mock data for metrics
const defaultMetrics = [
  { id: 'm1', label: '系统健康度', metric: 'health' },
  { id: 'm2', label: '平均响应时间', metric: 'response_time' },
  { id: 'm3', label: '吞吐量', metric: 'throughput' },
  { id: 'm4', label: '错误率', metric: 'error_rate' },
  { id: 'm5', label: 'CPU使用率', metric: 'cpu' },
  { id: 'm6', label: '内存使用率', metric: 'memory' },
  { id: 'm7', label: '磁盘使用率', metric: 'disk' },
  { id: 'm8', label: '活跃用户', metric: 'active_users' }
]

// Quick actions
const quickActions: SearchResult[] = [
  {
    id: 'a1',
    type: 'action',
    title: '刷新数据',
    description: '重新加载所有面板数据',
    icon: 'Refresh',
    shortcut: 'R',
    action: () => emit('refresh')
  },
  {
    id: 'a2',
    type: 'action',
    title: '打开设置',
    description: '配置面板和全局设置',
    icon: 'Setting',
    action: () => emit('open-settings')
  },
  {
    id: 'a3',
    type: 'action',
    title: '导出报表',
    description: '导出当前仪表板数据',
    icon: 'Download',
    action: () => emit('export')
  }
]

// Recent items (mock)
const recentItems = ref([
  { id: 'r1', title: '系统健康度' },
d: 'r2', title: '平均响应时间' },
d: 'r3', title: '最近告警' },
d: 'r4', title: '服务状态' }
])

const focusInput = () => {
 nextTick(() => {
    searchInput.value?.focus()
  })
}

const handleSearch = () => {
  activeIndex.value = 0
}

const getGlobalIndex = (type: 'panel' | 'metric' | 'action', index: number): number => {
  if (type === 'panel') return index
  if (type === 'metric') return panelResults.value.length + index
  return panelResults.value.length + metricResults.value.length + index
}

const navigateResults = (direction: number) => {
  const total = panelResults.value.length + metricResults.value.length + actionResults.value.length
  if (total === 0) return
  let newIndex = activeIndex.value + direction
  if (newIndex < 0) newIndex = total - 1
  if (newIndex >= total) newIndex = 0
  activeIndex.value = newIndex
}

const selectFirstResult = () => {
  if (filteredResults.value.length > 0) {
    selectResult(filteredResults.value[0])
  }
}

const selectResult = (item: SearchResult) => {
  if (item.type === 'panel') {
    emit('select-panel', item.id)
  } else if (item.type === 'metric') {
    ElMessage.info(`查看指标: ${item.label}`)
  }
  visible.value = false
}

const executeAction = (item: SearchResult) => {
  item.action?.()
  visible.value = false
}

const filteredResults = computed(() => {
  if (!searchQuery.value) return []
  const query = searchQuery.value.toLowerCase()
  return [
    ...panelResults.value,
    ...metricResults.value,
    ...actionResults.value
  ].filter(item => 
    item.title.toLowerCase().includes(query) ||
    (item.label & item.label.toLowerCase().includes(query)) ||
    item.description?.toLowerCase().includes(query)
  )
})

const panelResults = computed(() => {
  if (!searchQuery.value) return []
  const query = searchQuery.value.toLowerCase()
  return defaultPanels.filter(p => 
    p.title.toLowerCase().includes(query)
  )
})

const metricResults = computed(() => {
  if (!searchQuery.value) return []
  const query = searchQuery.value.toLowerCase()
  return defaultMetrics.filter(m => 
    m.label.toLowerCase().includes(query) ||
    m.metric.toLowerCase().includes(query)
  )
})

const actionResults = computed(() => {
  if (!searchQuery.value) return []
  const query = searchQuery.value.toLowerCase()
  return quickActions.filter(a => 
    a.title.toLowerCase().includes(query) ||
    a.description?.toLowerCase().includes(query)
  )
})
</script>

<style scoped>
.search-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.search-container {
  padding: 20px;
}

.search-input {
  width: 100%;
}

.search-hint {
  margin: 8px 0 16px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  text-align: center;
}

.search-results {
  max-height: 400px;
 overflow-y: auto;
}

.result-section {
  margin-bottom: 16px;
}

.section-title {
  margin: 0 0 8px 0;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.result-item:hover,
.result-item.is-active {
  background: var(--el-fill-color-light);
}

.result-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: var(--el-fill-color);
  border-radius: 6px;
  color: var(--el-color-primary);
}

.result-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.result-title {
  font-weight: 500;
  font-size: 14px;
}

.result-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.result-shortcut {
  font-family: monospace;
  font-size: 11px;
  padding: 2px 6px;
  background: var(--el-fill-color);
  border-radius: 3px;
  color: var(--el-text-color-secondary);
}

.no-results {
  padding: 40px 0;
  text-align: center;
}

.quick-actions {
  padding: 20px 0;
}

.quick-actions-grid {
 display: grid;
 grid-template-columns: repeat(3, 1fr);
 gap: 12px;
}

.quick-action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  height: auto;
}

.quick-action-btn.el-icon {
  font-size: 24px;
}

.recent-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.recent-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.recent-item:hover {
  background: var(--el-fill-color-light);
}

/* High contrast */
:global(.high-contrast) .result-item,
:global(.high-contrast) .quick-action-btn,
:global(.high-contrast) .recent-item {
  border: 1px solid var(--el-border-color);
}

:global(.high-contrast) .result-item.is-active {
  border-color: var(--el-color-primary);
  border-width: 2px;
}

/* Reduced motion */
:global(.reduce-motion) .result-item,
:global(.reduce-motion) .recent-item {
  transition: none;
}
</style>