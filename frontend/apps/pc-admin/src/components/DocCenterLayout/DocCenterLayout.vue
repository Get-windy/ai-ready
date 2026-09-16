<template>
  <div
    class="doc-center-layout"
    @table-expand-change="onTableExpandChange"
  >
    <!-- ═══ 主Tab（工作流阶段）深色Tab栏 ═══ -->
    <div class="main-tabs tab-bar">
      <div class="tab-items">
        <div
          v-for="tab in mainTabs"
          :key="tab.key"
          :class="['tab-item', { active: activeMainTab === tab.key }]"
          @click="selectMainTab(tab.key)"
        >
          {{ tab.label }}
        </div>
      </div>
    </div>

    <!-- ═══ 子Tab（维度视图）居中，总高40px ══ -->
    <div
      v-if="hasSubTabs"
      class="sub-tabs"
    >
      <div class="sub-tab-inner">
        <a-tabs
          v-model:active-key="activeSubTab"
          size="small"
          @change="handleSubTabChange"
        >
          <a-tab-pane
            v-for="tab in subTabs"
            :key="tab.key"
            :tab="tab.label"
          />
        </a-tabs>
      </div>
    </div>

    <!-- ═══ 日期快捷 + 操作按钮（同一行，左日期右按钮） ═══ -->
    <div class="filter-toolbar">
      <div class="filter-toolbar-left">
        <slot name="toolbar-left">
          <a-space
            v-if="showDateShortcuts"
            :size="8"
          >
            <span
              v-for="d in dateShortcuts"
              :key="d.key"
              :class="['date-btn', { active: dateShortcut === d.key }]"
              @click="selectDateShortcut(d.key)"
            >
              {{ d.label }}
            </span>
          </a-space>
        </slot>
      </div>
      <div class="filter-toolbar-right">
        <slot
          name="toolbar-right"
          :active-main-tab="activeMainTab"
          :active-sub-tab="activeSubTab"
        >
          <a-space :size="8">
            <template
              v-for="btn in visibleToolbarButtons"
              :key="btn.key"
            >
              <a-dropdown v-if="btn.dropdownItems && btn.dropdownItems.length > 0">
                <a-button
                  :type="btn.type || 'default'"
                  size="small"
                  :danger="btn.danger"
                >
                  <component
                    :is="iconComponent(btn.icon)"
                    v-if="btn.icon"
                  />
                  {{ btn.label }}<DownOutlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item
                      v-for="item in btn.dropdownItems"
                      :key="item.key"
                      :danger="item.danger"
                      @click="emitToolbarAction(item.key)"
                    >
                      {{ item.label }}
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <a-button
                v-else
                :type="btn.type || 'default'"
                size="small"
                :danger="btn.danger"
                @click="emitToolbarAction(btn.key)"
              >
                <component
                  :is="iconComponent(btn.icon)"
                  v-if="btn.icon"
                />
                {{ btn.label }}
              </a-button>
            </template>
          </a-space>
        </slot>
      </div>
    </div>

    <!-- ══ 搜索区域 ═══ -->
    <div
      v-if="currentSearchFields.length > 0"
      class="search-area"
    >
      <!-- 搜索字段网格（含查询按钮和复选框） -->
      <div class="search-grid">
        <!-- 日期起止选择（始终显示在第一个） -->
        <div class="search-field-item">
          <a-range-picker
            v-model:value="dateRange"
            :placeholder="['开始日期', '结束日期']"
            size="small"
            @change="handleDateRangeChange"
          />
        </div>
        <template v-for="(field, index) in currentSearchFields" :key="field.key">
          <div
            v-if="searchExpanded || isFieldVisible(index)"
            class="search-field-item"
          >
            <a-input
              v-if="field.type === 'input'"
              v-model:value="(searchValues[field.key] as any)"
              :placeholder="typeof field.placeholder === 'string' ? field.placeholder : field.label"
              size="small"
              @press-enter="emitSearch"
            >
              <template v-if="field.suffix === 'search'" #suffix>
                <SearchOutlined />
              </template>
            </a-input>
            <div v-else-if="field.type === 'select'" class="search-select-wrap">
              <span class="search-select-label">{{ field.label }}</span>
              <a-select
                v-model:value="(searchValues[field.key] as any)"
                size="small"
                allow-clear
                :options="field.options"
              />
            </div>
            <a-range-picker
              v-else-if="field.type === 'dateRange'"
              v-model:value="(searchValues[field.key] as any)"
              size="small"
              style="width: 100%"
              :placeholder="(Array.isArray(field.placeholder) ? field.placeholder : ['开始日期', '结束日期']) as [string, string]"
            />
          </div>
        </template>

        <!-- 查询按钮（始终显示） -->
        <div class="search-field-item search-action-item">
          <a-space :size="8">
            <a-button type="primary" size="small" @click="emitSearch">查询</a-button>
            <a-button size="small" @click="emitRefresh">
              <ReloadOutlined />
            </a-button>
          </a-space>
        </div>

        <!-- 复选框（始终显示） -->
        <template v-for="cb in currentSearchCheckboxes" :key="cb.key">
          <div class="search-field-item search-checkbox-field">
            <a-checkbox
              v-model:checked="searchValues[cb.key]"
              class="search-checkbox-item"
            >
              {{ cb.label }}
            </a-checkbox>
          </div>
        </template>

        <!-- 额外插槽（始终显示） -->
        <div v-if="$slots['search-extra']" class="search-field-item search-extra-item">
          <slot name="search-extra" />
        </div>
      </div>

      <!-- 更多条件 -->
      <div v-if="hasMoreSearchFields" class="search-more-toggle">
        <a-button type="link" size="small" @click="searchExpanded = !searchExpanded">
          {{ searchExpanded ? '收起' : '更多条件' }}
          <DownOutlined v-if="!searchExpanded" />
          <UpOutlined v-else />
        </a-button>
      </div>
    </div>

    <!-- ═══ 统计卡片 ═══ -->
    <div
      v-if="currentStatCards.length > 0"
      class="stats-cards"
    >
      <div
        v-for="card in currentStatCards"
        :key="card.valueKey"
        class="stat-card"
        :style="{ borderLeftColor: card.color }"
      >
        <div class="stat-label">
          {{ card.label }}
        </div>
        <div class="stat-value">
          {{ formattedStat(card.valueKey) }}
        </div>
      </div>
    </div>

    <!-- ══ 履约统计卡片 ═══ -->
    <div
      v-if="currentFulfillmentStats.length > 0"
      class="fulfillment-stats"
    >
      <div
        v-for="item in currentFulfillmentStats"
        :key="item.valueKey"
        class="fulfillment-stat-card"
        :style="item.bgColor ? { background: item.bgColor } : {}"
      >
        <div class="fs-label">
          {{ item.label }}
        </div>
        <div class="fs-value">
          {{ fulfillmentData[item.valueKey] ?? 0 }}
        </div>
      </div>
      <div
        v-if="fulfillmentRate !== undefined"
        class="fulfillment-rate"
      >
        <span>履约率: {{ fulfillmentRate }}%</span>
        <span
          v-if="fulfillmentRevenue"
          style="margin-left: 16px"
        >
          预计补单后营收: ¥{{ fulfillmentRevenue?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}
        </span>
      </div>
    </div>

    <!-- ═══ 图表区域 ══ -->
    <div
      v-if="chartConfig?.visible"
      class="overview-chart"
    >
      <div class="chart-header">
        <h4>{{ chartConfig.title || '履约概览' }}</h4>
        <div
          v-if="chartConfig.legend"
          class="chart-legend"
        >
          <span
            v-for="item in chartConfig.legend"
            :key="item.label"
          >
            <span
              class="legend-dot"
              :style="{ background: item.color }"
            />
            {{ item.label }}
          </span>
        </div>
        <div
          v-if="chartConfig.showTimeToggle"
          class="time-chart-toggle"
        >
          <a-button-group size="small">
            <a-button
              :type="timeChartMode === 'day' ? 'primary' : 'default'"
              @click="setTimeMode('day')"
            >
              按天
            </a-button>
            <a-button
              :type="timeChartMode === 'week' ? 'primary' : 'default'"
              @click="setTimeMode('week')"
            >
              按周
            </a-button>
            <a-button
              :type="timeChartMode === 'month' ? 'primary' : 'default'"
              @click="setTimeMode('month')"
            >
              按月
            </a-button>
          </a-button-group>
        </div>
      </div>
      <div
        class="chart-content"
        :style="{ height: (chartConfig.height || 200) + 'px' }"
      >
        <slot name="chart" />
      </div>
    </div>

    <!-- ═══ 表格面板 ═══ -->
    <div class="table-panel">
      <div class="table-section">
        <slot name="table" />
      </div>
      <!-- ═══ 分页（表格展开显示时自动让位，表格长到页面底部） ═══ -->
      <div
        v-if="showPagination && !tableExpanded"
        class="table-pagination"
      >
        <StandardPagination
          variant="classic"
          :current="pageCurrent"
          :page-size="pageSize"
          :total="pageTotal"
          :page-size-options="numericPageSizeOptions"
          @change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  DownOutlined, SearchOutlined, ReloadOutlined, PlusOutlined,
  PrinterOutlined, BarChartOutlined, CheckOutlined,
  DeleteOutlined, ExportOutlined, UpOutlined, SettingOutlined,
} from '@ant-design/icons-vue'
import dayjs, { type Dayjs } from 'dayjs'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import type {
  DocMainTab, DocSubTab, DateShortcutItem, StatCardItem,
  SearchFieldItem, SearchCheckboxItem, ToolbarButtonItem, FulfillmentStatItem,
  ChartConfig, SearchConfigMap, SearchCheckboxConfigMap, ToolbarConfigMap,
  StatCardConfigMap, FulfillmentConfigMap,
} from './types'

// ── Props ──
const props = withDefaults(defineProps<{
  mainTabs: DocMainTab[]
  subTabs: DocSubTab[]
  mainTabsWithSubTabs?: string[]
  showDateShortcuts?: boolean
  dateShortcuts?: DateShortcutItem[]
  searchConfig?: SearchConfigMap
  searchCheckboxConfig?: SearchCheckboxConfigMap
  /** 需要隐藏的搜索字段key列表（由PageConfigPanel控制） */
  hiddenFieldKeys?: string[]
  toolbarConfig?: ToolbarConfigMap
  statCardConfig?: StatCardConfigMap
  fulfillmentConfig?: FulfillmentConfigMap
  chartConfig?: ChartConfig
  statsData?: Record<string, any>
  fulfillmentData?: Record<string, any>
  fulfillmentRate?: number
  fulfillmentRevenue?: number
  showPagination?: boolean
  pageTotal?: number
  pageSizeOptions?: string[]
  timeChartMode?: string
}>(), {
  mainTabsWithSubTabs: () => ['all'],
  showDateShortcuts: true,
  dateShortcuts: () => [
    { key: 'yesterday', label: '昨日' },
    { key: 'today', label: '今日' },
    { key: 'thisWeek', label: '本周' },
    { key: 'thisWeek2', label: '近一周' },
    { key: 'thisMonth', label: '本月' },
    { key: 'lastMonth', label: '上月' },
    { key: 'last3Month', label: '近三月' },
    { key: 'thisYear', label: '本年' },
  ],
  searchConfig: () => ({}),
  searchCheckboxConfig: () => ({}),
  hiddenFieldKeys: () => [],
  toolbarConfig: () => ({}),
  statCardConfig: () => ({}),
  fulfillmentConfig: () => ({}),
  chartConfig: () => ({ visible: false }),
  statsData: () => ({}),
  fulfillmentData: () => ({}),
  showPagination: true,
  pageTotal: 0,
  pageSizeOptions: () => ['20', '50', '100'],
  timeChartMode: 'day',
})

// ── Emits ──
const emit = defineEmits<{
  'search': []
  'refresh': []
  'toolbar-action': [key: string]
  'page-change': [page: number, pageSize: number]
  'time-mode-change': [mode: string]
}>()

// ── v-model ──
const activeMainTab = defineModel<string>('activeMainTab', { default: 'all' })
const activeSubTab = defineModel<string>('activeSubTab', { default: 'byDoc' })
const dateShortcut = defineModel<string>('dateShortcut', { default: 'thisWeek2' })
const dateRange = defineModel<[Dayjs, Dayjs] | null>('dateRange', {
  default: () => [dayjs().subtract(7, 'day'), dayjs()],
})
const searchValues = defineModel<Record<string, any>>('searchValues', { default: () => ({}) })
const pageCurrent = defineModel<number>('pageCurrent', { default: 1 })
const pageSize = defineModel<number>('pageSize', { default: 20 })

// ─ 内部状态 ──
const timeChartMode = ref(props.timeChartMode || 'day')
const searchExpanded = ref(false)

/**
 * 表格展开联动：BillDetailTable 点「表格展开显示」时冒泡 table-expand-change，
 * 本组件据此隐藏分页区（表格下方让位，表格才能占满到页面底部）。业务页无需再接 expand-change。
 */
const tableExpanded = ref(false)
function onTableExpandChange(e: Event) {
  tableExpanded.value = !!(e as CustomEvent).detail
}

/** 分页条数选项：页面传的是字符串数组（['20','50','100']），StandardPagination 要数字 */
const numericPageSizeOptions = computed(() =>
  props.pageSizeOptions.map(v => Number(v)).filter(v => !Number.isNaN(v))
)

function currentTabKey(): string {
  if (props.mainTabsWithSubTabs.includes(activeMainTab.value)) {
    return `${activeMainTab.value}.${activeSubTab.value}`
  }
  return activeMainTab.value
}

const hasSubTabs = computed(() =>
  props.subTabs.length > 0 && props.mainTabsWithSubTabs.includes(activeMainTab.value)
)

const currentSearchFields = computed<SearchFieldItem[]>(() => {
  const allFields = props.searchConfig[currentTabKey()] || []
  const hidden = new Set(props.hiddenFieldKeys || [])
  return allFields.filter(f => !hidden.has(f.key))
})

const currentSearchCheckboxes = computed<SearchCheckboxItem[]>(() =>
  props.searchCheckboxConfig[currentTabKey()] || []
)

/** 每行显示字段数（根据 span 计算，默认 span=3 时一行 8 个，但截图显示约 7 个） */
const SEARCH_FIELDS_PER_ROW = 7
/** 默认显示行数 */
const SEARCH_DEFAULT_ROWS = 2

/** 2 行总槽位数 = 14 */
const TOTAL_SLOTS = SEARCH_FIELDS_PER_ROW * SEARCH_DEFAULT_ROWS

/** 固定占用槽位数：日期范围(1) + 查询按钮+复选框(约4) */
const FIXED_SLOTS = 5

/** 搜索字段可用槽位 = 总槽位 - 固定占用 */
const availableSearchFieldSlots = computed(() =>
  Math.max(0, TOTAL_SLOTS - FIXED_SLOTS)
)

/** 判断字段是否可见（未超出可用槽位或已展开） */
function isFieldVisible(index: number): boolean {
  return index < availableSearchFieldSlots.value
}

/** 是否有更多字段需要展开（查询按钮和复选框不算在内） */
const hasMoreSearchFields = computed(() =>
  currentSearchFields.value.length > availableSearchFieldSlots.value
)

const visibleToolbarButtons = computed<ToolbarButtonItem[]>(() => {
  const buttons = props.toolbarConfig[currentTabKey()] || []
  return buttons.filter(btn => !btn.visibleFor || btn.visibleFor(activeMainTab.value, activeSubTab.value))
})

const currentStatCards = computed<StatCardItem[]>(() =>
  props.statCardConfig[currentTabKey()] || []
)

const currentFulfillmentStats = computed<FulfillmentStatItem[]>(() =>
  props.fulfillmentConfig[currentTabKey()] || []
)

function formattedStat(valueKey: string): string | number {
  const val = props.statsData[valueKey]
  if (val === undefined || val === null) return 0
  if (typeof val === 'number') {
    return val.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
  }
  return val
}

const iconMap: Record<string, any> = {
  PlusOutlined, PrinterOutlined, BarChartOutlined, CheckOutlined,
  DeleteOutlined, ExportOutlined, ReloadOutlined, SettingOutlined,
}

function iconComponent(name?: string): any {
  if (!name) return null
  return iconMap[name] || null
}

function selectMainTab(key: string) {
  activeMainTab.value = key
  searchExpanded.value = false
  pageCurrent.value = 1
}

function handleSubTabChange() {
  searchExpanded.value = false
  pageCurrent.value = 1
}

function selectDateShortcut(key: string) {
  dateShortcut.value = key
  const now = dayjs()
  const map: Record<string, [Dayjs, Dayjs]> = {
    yesterday: [now.subtract(1, 'day'), now.subtract(1, 'day')],
    today: [now, now],
    thisWeek: [now.startOf('week'), now],
    thisWeek2: [now.subtract(7, 'day'), now],
    thisMonth: [now.startOf('month'), now],
    lastMonth: [now.subtract(1, 'month').startOf('month'), now.subtract(1, 'month').endOf('month')],
    last3Month: [now.subtract(3, 'month'), now],
    thisYear: [now.startOf('year'), now],
  }
  if (map[key]) dateRange.value = map[key]
  emitSearch()
}

function handleDateRangeChange() {
  dateShortcut.value = ''
  pageCurrent.value = 1
  emitSearch()
}

function emitSearch() { pageCurrent.value = 1; emit('search') }
function emitRefresh() { emit('refresh') }

function handlePageChange(page: number, size: number) {
  pageCurrent.value = page
  pageSize.value = size
  emit('page-change', page, size)
}

function emitToolbarAction(key: string) { emit('toolbar-action', key) }
function setTimeMode(mode: string) { timeChartMode.value = mode; emit('time-mode-change', mode) }
</script>

<style scoped>
/* ── 整体布局 ── */
.doc-center-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #f0f2f5;
  overflow: hidden;
}

/* ─ 主Tab：深色Tab栏 ─ */
.tab-bar {
  background: #4a4a4a;
  padding: 0;
  flex-shrink: 0;
  position: relative;
  z-index: 3;
}
.tab-items {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 4px;
  height: 38px;
  padding: 0 20px;
}
.tab-item {
  padding: 6px 22px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: color 0.2s;
  user-select: none;
  white-space: nowrap;
  position: relative;
  border-radius: 6px 6px 0 0;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-bottom: none;
}
.tab-item:hover:not(.active) {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}
.tab-item.active {
  color: #333;
  background: #fff;
  font-weight: 500;
  border-color: #fff;
  padding-bottom: 8px;
  margin-bottom: -1px;
  z-index: 2;
}
.tab-item.active::before,
.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  width: 9999px;
  height: 1px;
  background: rgba(255, 255, 255, 0.15);
}
.tab-item.active::before { right: 100%; }
.tab-item.active::after { left: 100%; }

/* ── 子Tab：居中，总高40px，主Tab与子Tab之间3px间距 ── */
.sub-tabs {
  background: #fff;
  flex-shrink: 0;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 3px;
  border-bottom: 1px solid #e8e8e8;
  overflow: hidden;
}
.sub-tab-inner {
  display: flex;
  align-items: center;
  height: 100%;
}
.sub-tabs :deep(.ant-tabs) {
  height: 100%;
}
.sub-tabs :deep(.ant-tabs-nav) {
  margin: 0 !important;
  height: 40px;
  display: flex;
  align-items: center;
}
.sub-tabs :deep(.ant-tabs-nav::before) {
  border-bottom: none !important;
}
.sub-tabs :deep(.ant-tabs-nav-list) {
  display: flex;
  align-items: center;
}
.sub-tabs :deep(.ant-tabs-tab) {
  padding: 2px 14px;
  font-size: 12px;
  line-height: 20px;
  color: #595959;
  margin: 0 2px;
}
.sub-tabs :deep(.ant-tabs-tab:hover) {
  color: #1890ff;
}
.sub-tabs :deep(.ant-tabs-tab-active) {
  color: #1890ff;
}
.sub-tabs :deep(.ant-tabs-ink-bar) {
  height: 2px;
  background: #1890ff;
}
.sub-tabs :deep(.ant-tabs-nav-operations),
.sub-tabs :deep(.ant-tabs-extra-content) {
  display: none;
}

/* ─ 日期快捷 + 操作按钮（同一行） ── */
.filter-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  height: 40px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}
.filter-toolbar-left,
.filter-toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.date-btn {
  cursor: pointer;
  padding: 2px 8px;
  font-size: 12px;
  color: #666;
  border-radius: 2px;
  transition: all 0.2s;
  white-space: nowrap;
}
.date-btn:hover { color: #1890ff; }
.date-btn.active {
  color: #1890ff;
  border-bottom: 2px solid #1890ff;
}

/* ── 搜索区域 ── */
.search-area {
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.search-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.search-field-item {
  display: flex;
  width: calc(100% / 7 - 12px);
  min-width: 0;
}

.search-field-item :deep(.ant-input-wrapper),
.search-field-item :deep(.ant-input-affix-wrapper) {
  width: 100%;
  font-size: 13px;
}

.search-field-item :deep(.ant-select) {
  width: 100%;
}

.search-field-item :deep(.ant-select .ant-select-selector) {
  font-size: 13px;
}

.search-field-item :deep(.ant-picker) {
  width: 100%;
}

/* 下拉框：标签在边框内左侧 */
.search-select-wrap {
  display: flex;
  align-items: center;
  width: 100%;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: #fff;
}
.search-select-wrap:hover {
  border-color: #4096ff;
}
.search-select-label {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  white-space: nowrap;
  flex-shrink: 0;
  padding-left: 8px;
}
.search-select-wrap :deep(.ant-select) {
  flex: 1;
  min-width: 0;
}
.search-select-wrap :deep(.ant-select .ant-select-selector) {
  border: none !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  display: flex;
  align-items: center;
}

.search-action-item {
  width: auto;
  flex-shrink: 0;
}

/* 复选框项 */
.search-checkbox-field {
  width: auto;
  flex-shrink: 0;
}

.search-checkbox-item {
  font-size: 12px;
  white-space: nowrap;
}

.search-checkbox-item :deep(.ant-checkbox-label) {
  font-size: 12px;
}

/* 额外插槽项 */
.search-extra-item {
  width: auto;
  flex-shrink: 0;
}

/* ── 更多条件 ─ */
.search-more-toggle {
  display: flex;
  justify-content: center;
  padding-top: 4px;
}

/* ── 统计卡片 ── */
.stats-cards {
  display: flex;
  gap: 12px;
  padding: 5px 16px;
  height: 70px;
  align-items: center;
  flex-shrink: 0;
  flex-wrap: wrap;
}
.stat-card {
  padding: 8px 20px;
  height: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-radius: 6px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-left: 3px solid #1890ff;
  min-width: 100px;
  text-align: center;
  flex-shrink: 0;
  box-sizing: border-box;
}
.stat-label { font-size: 12px; color: #8c8c8c; white-space: nowrap; }
.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #262626;
  font-family: 'SF Mono', 'Monaco', 'Menlo', monospace;
}

/* ─ 履约统计 ── */
.fulfillment-stats {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  flex-wrap: wrap;
  align-items: center;
  flex-shrink: 0;
}
.fulfillment-stat-card {
  padding: 12px 16px;
  background: #f0f5ff;
  border-radius: 6px;
  min-width: 140px;
}
.fs-label { font-size: 12px; color: #595959; }
.fs-value { font-size: 20px; font-weight: 600; color: #262626; }
.fulfillment-rate { font-size: 14px; color: #262626; margin-left: auto; white-space: nowrap; }

/* ── 图表区域 ── */
.overview-chart { padding: 12px 16px; flex-shrink: 0; background: #fff; }
.chart-header { display: flex; align-items: center; gap: 16px; margin-bottom: 8px; }
.chart-header h4 { margin: 0; font-size: 14px; color: #262626; }
.chart-legend { display: flex; gap: 16px; font-size: 12px; color: #666; }
.legend-dot { display: inline-block; width: 12px; height: 12px; border-radius: 2px; margin-right: 4px; vertical-align: middle; }
.time-chart-toggle { margin-left: auto; }
.chart-content {
  background: #fafafa;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  min-height: 100px;
}

/* ── 表格面板 ── */
.table-panel {
  flex: 1;
  min-width: 0;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.table-section { flex: 1; min-height: 0; overflow: hidden; }

/* ── 分页 ─ */
/* 与「商城订单」等页统一走 StandardPagination 经典分页栏（首页/上页/第(x/y)页/下页/尾页/跳转/共 N 条记录/每页显示 N 行），
   分页栏自带边框与内边距，这里只保留让位与底色，避免出现双层边框。 */
.table-pagination {
  background: #fafafa;
  border-top: 1px solid #e8e8e8;
  flex-shrink: 0;
}
.table-pagination :deep(.standard-pagination) {
  border-top: none;
  background: transparent;
  padding: 8px 16px;
}

/* ── 紧凑尺寸 ── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
}
</style>
