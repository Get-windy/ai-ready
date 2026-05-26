<template>
  <div class="dashboard-layout" :class="[themeClass, layoutClass]">
    <!-- 顶部导航栏 -->
    <header class="dashboard-header">
      <div class="header-left">
        <h1 class="dashboard-title">库存管理仪表板</h1>
        <span class="dashboard-subtitle">实时监控 · 智能预警 · 数据分析</span>
      </div>
      
      <div class="header-right">
        <!-- 日期范围选择器 -->
        <DateRangePicker
          v-model="dateRange"
          :presets="datePresets"
          @change="handleDateRangeChange"
        />
        
        <!-- 主题切换器 -->
        <ThemeSwitcher
          v-model="currentTheme"
          :themes="availableThemes"
          @change="handleThemeChange"
        />
        
        <!-- 刷新按钮 -->
        <el-button
          type="primary"
          :loading="loading"
          :icon="Refresh"
          @click="refreshDashboard"
        >
          刷新数据
        </el-button>
      </div>
    </header>

    <!-- 筛选面板 -->
    <section class="filter-section" v-if="showFilters">
      <FilterPanel
        v-model="filters"
        :categories="categories"
        :warehouses="warehouses"
        @filter-change="handleFilterChange"
        @reset="resetFilters"
      />
    </section>

    <!-- 仪表板主体 -->
    <main class="dashboard-main">
      <!-- 网格布局容器 -->
      <DashboardGrid
        ref="gridRef"
        :widgets="activeWidgets"
        :layout="gridLayout"
        :editable="layoutEditable"
        @layout-change="handleLayoutChange"
        @widget-add="handleWidgetAdd"
        @widget-remove="handleWidgetRemove"
      >
        <!-- 默认小部件插槽 -->
        <template #default="{ widget }">
          <WidgetContainer
            :widget="widget"
            :loading="loading"
            @configure="handleWidgetConfigure"
            @export="handleWidgetExport"
          >
            <template #content>
              <component
                :is="getWidgetComponent(widget.type)"
                :widget="widget"
                :data="getWidgetData(widget.id)"
                :loading="loading"
              />
            </template>
          </WidgetContainer>
        </template>
      </DashboardGrid>
    </main>

    <!-- 底部状态栏 -->
    <footer class="dashboard-footer">
      <div class="footer-left">
        <span class="last-update">
          最后更新: {{ lastUpdateTime }}
        </span>
        <span class="data-status" :class="dataStatusClass">
          {{ dataStatusText }}
        </span>
      </div>
      
      <div class="footer-right">
        <el-button
          type="text"
          :icon="Setting"
          @click="toggleLayoutEdit"
        >
          {{ layoutEditable ? '保存布局' : '编辑布局' }}
        </el-button>
        
        <ExportControls
          :dashboard-data="dashboardData"
          :widgets="activeWidgets"
          @export-pdf="handleExportPDF"
          @export-excel="handleExportExcel"
        />
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { Refresh, Setting } from '@element-plus/icons-vue'
import { useDashboardStore } from '../../stores/dashboard.store'
import { useInventoryStore } from '../../stores/inventory.store'
import { useThemeStore } from '../../stores/theme.store'
import { useBreakpoints } from '../../composables/useBreakpoints'

// 组件导入
import DateRangePicker from '../controls/DateRangePicker.vue'
import ThemeSwitcher from '../controls/ThemeSwitcher.vue'
import FilterPanel from '../controls/FilterPanel.vue'
import DashboardGrid from './DashboardGrid.vue'
import WidgetContainer from './WidgetContainer.vue'
import ExportControls from '../controls/ExportControls.vue'

// 小部件组件
import InventoryOverview from '../charts/InventoryOverview.vue'
import InventoryTrendChart from '../charts/InventoryTrendChart.vue'
import CategoryDistribution from '../charts/CategoryDistribution.vue'
import TurnoverHeatmap from '../charts/TurnoverHeatmap.vue'
import AlertNotifications from '../charts/AlertNotifications.vue'

// 类型导入
import type { DashboardLayout, WidgetConfig, FilterOptions, DateRange } from '../../types/dashboard.types'
import type { InventoryData } from '../../types/inventory.types'

// Store
const dashboardStore = useDashboardStore()
const inventoryStore = useInventoryStore()
const themeStore = useThemeStore()

// 响应式断点
const { breakpoint } = useBreakpoints()

// 响应式数据
const dateRange = ref<DateRange>({
  start: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000), // 30天前
  end: new Date()
})

const filters = ref<FilterOptions>({
  categories: [],
  warehouses: [],
  minStock: 0,
  maxStock: 10000,
  includeZeroStock: false
})

const layoutEditable = ref(false)
const loading = ref(false)
const gridRef = ref<InstanceType<typeof DashboardGrid>>()

// 计算属性
const themeClass = computed(() => `theme-${themeStore.currentTheme}`)
const layoutClass = computed(() => `layout-${breakpoint.value}`)

const activeWidgets = computed(() => dashboardStore.activeWidgets)
const gridLayout = computed(() => dashboardStore.layout)
const currentTheme = computed({
  get: () => themeStore.currentTheme,
  set: (value) => themeStore.setTheme(value)
})

const availableThemes = computed(() => themeStore.availableThemes)
const categories = computed(() => inventoryStore.categories)
const warehouses = computed(() => inventoryStore.warehouses)
const dashboardData = computed(() => inventoryStore.inventoryData)

const lastUpdateTime = computed(() => {
  return dashboardStore.lastUpdateTime
    ? new Date(dashboardStore.lastUpdateTime).toLocaleString()
    : '从未更新'
})

const dataStatusClass = computed(() => {
  if (loading.value) return 'status-loading'
  if (dashboardStore.error) return 'status-error'
  return 'status-success'
})

const dataStatusText = computed(() => {
  if (loading.value) return '数据加载中...'
  if (dashboardStore.error) return '数据加载失败'
  return '数据已同步'
})

const showFilters = computed(() => {
  return breakpoint.value !== 'xs' && breakpoint.value !== 'sm'
})

// 预设日期范围
const datePresets = [
  { label: '今天', value: 'today' },
  { label: '昨天', value: 'yesterday' },
  { label: '最近7天', value: 'last7days' },
  { label: '最近30天', value: 'last30days' },
  { label: '本月', value: 'thisMonth' },
  { label: '上个月', value: 'lastMonth' }
]

// 小部件类型映射
const widgetComponentMap = {
  'inventory-overview': InventoryOverview,
  'trend-chart': InventoryTrendChart,
  'category-distribution': CategoryDistribution,
  'turnover-heatmap': TurnoverHeatmap,
  'alert-notifications': AlertNotifications
}

// 方法
const getWidgetComponent = (type: string) => {
  return widgetComponentMap[type as keyof typeof widgetComponentMap] || InventoryOverview
}

const getWidgetData = (widgetId: string) => {
  // 根据小部件ID获取特定数据
  return dashboardData.value
}

const refreshDashboard = async () => {
  loading.value = true
  try {
    await dashboardStore.refreshData(dateRange.value, filters.value)
  } catch (error) {
    console.error('刷新数据失败:', error)
  } finally {
    loading.value = false
  }
}

const handleDateRangeChange = (range: DateRange) => {
  dateRange.value = range
  refreshDashboard()
}

const handleThemeChange = (theme: string) => {
  themeStore.setTheme(theme)
}

const handleFilterChange = (newFilters: FilterOptions) => {
  filters.value = newFilters
  refreshDashboard()
}

const resetFilters = () => {
  filters.value = {
    categories: [],
    warehouses: [],
    minStock: 0,
    maxStock: 10000,
    includeZeroStock: false
  }
  refreshDashboard()
}

const handleLayoutChange = (layout: DashboardLayout) => {
  dashboardStore.updateLayout(layout)
}

const handleWidgetAdd = (widget: WidgetConfig) => {
  dashboardStore.addWidget(widget)
}

const handleWidgetRemove = (widgetId: string) => {
  dashboardStore.removeWidget(widgetId)
}

const handleWidgetConfigure = (widget: WidgetConfig) => {
  // 打开小部件配置对话框
  console.log('配置小部件:', widget)
}

const handleWidgetExport = (widget: WidgetConfig) => {
  // 导出单个小部件数据
  console.log('导出小部件:', widget)
}

const toggleLayoutEdit = () => {
  layoutEditable.value = !layoutEditable.value
  if (!layoutEditable.value && gridRef.value) {
    // 保存布局
    const layout = gridRef.value.getCurrentLayout()
    dashboardStore.updateLayout(layout)
  }
}

const handleExportPDF = () => {
  console.log('导出PDF')
}

const handleExportExcel = () => {
  console.log('导出Excel')
}

// 生命周期
onMounted(async () => {
  // 初始加载数据
  await refreshDashboard()
  
  // 设置自动刷新
  const refreshInterval = setInterval(refreshDashboard, 5 * 60 * 1000) // 5分钟
  
  onUnmounted(() => {
    clearInterval(refreshInterval)
  })
})
</script>

<style scoped>
.dashboard-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: var(--color-background);
  color: var(--color-text-primary);
  transition: background-color 0.3s ease, color 0.3s ease;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  background-color: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}

.header-left {
  display: flex;
  flex-direction: column;
}

.dashboard-title {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
}

.dashboard-subtitle {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-top: 0.25rem;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.filter-section {
  padding: 1rem 1.5rem;
  background-color: var(--color-surface-secondary);
  border-bottom: 1px solid var(--color-border);
}

.dashboard-main {
  flex: 1;
  overflow: auto;
  padding: 1.5rem;
  position: relative;
}

.dashboard-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1.5rem;
  background-color: var(--color-surface);
  border-top: 1px solid var(--color-border);
  font-size: var(--font-size-sm);
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.last-update {
  color: var(--color-text-secondary);
}

.data-status {
  padding: 0.25rem 0.5rem;
  border-radius: var(--border-radius-sm);
  font-size: var(--font-size-xs);
}

.status-loading {
  background-color: var(--color-info-light);
  color: var(--color-info);
}

.status-success {
  background-color: var(--color-success-light);
  color: var(--color-success);
}

.status-error {
  background-color: var(--color-danger-light);
  color: var(--color-danger);
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

/* 响应式样式 */
@media (max-width: 768px) {
  .dashboard-header {
    flex-direction: column;
    gap: 1rem;
    padding: 1rem;
  }
  
  .header-right {
    width: 100%;
    justify-content: space-between;
  }
  
  .dashboard-footer {
    flex-direction: column;
    gap: 0.5rem;
    text-align: center;
  }
  
  .footer-left,
  .footer-right {
    width: 100%;
    justify-content: center;
  }
}

@media (max-width: 576px) {
  .dashboard-main {
    padding: 1rem;
  }
  
  .header-right {
    flex-wrap: wrap;
  }
}
</style>