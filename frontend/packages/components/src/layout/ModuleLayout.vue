<template>
  <div class="ar-module-layout">
    <ControlPanel
      :breadcrumb-items="breadcrumbItems"
      :current-view="currentView"
      :available-views="availableViews"
      :show-view-switch="showViewSwitch"
      :selected-count="selectedCount"
      :show-search="showSearch"
      :search-placeholder="searchPlaceholder"
      :search-value="searchValue"
      :show-filter-button="showFilterButton"
      :active-filter-count="activeFilterCount"
      :show-pagination="showPagination"
      :current-page="currentPage"
      :total-pages="totalPages"
      :page-size="pageSize"
      :page-size-options="pageSizeOptions"
      :total-items="totalItems"
      :loading="loading"
      @breadcrumb-click="handleBreadcrumbClick"
      @view-change="handleViewChange"
      @clear-selection="handleClearSelection"
      @search-input="handleSearchInput"
      @search-submit="handleSearchSubmit"
      @filter-toggle="handleFilterToggle"
      @page-change="handlePageChange"
      @page-size-change="handlePageSizeChange"
    >
      <template #breadcrumb>
        <slot name="breadcrumb" />
      </template>
      <template #view-switch>
        <slot name="view-switch" />
      </template>
      <template #actions>
        <slot name="actions" />
      </template>
      <template #batch-actions>
        <slot name="batch-actions" />
      </template>
      <template #search>
        <slot name="search" />
      </template>
      <template #filter>
        <slot name="filter" />
      </template>
      <template #pagination>
        <slot name="pagination" />
      </template>
    </ControlPanel>

    <!-- 模块级 Tab 导航 -->
    <div v-if="tabs && tabs.length > 0" class="ar-module-tabs-nav">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="ar-module-tab-btn"
        :class="{ 'ar-module-tab-btn--active': activeTab === tab.key }"
        :disabled="tab.disabled"
        @click="handleTabClick(tab.key)"
      >
        <span class="ar-module-tab-label">{{ tab.label }}</span>
        <span v-if="tab.count !== undefined" class="ar-module-tab-count">{{ tab.count }}</span>
      </button>
    </div>

    <!-- 状态提示 -->
    <div v-if="error" class="ar-module-state ar-module-state--error">
      <span class="ar-module-state-icon">⚠️</span>
      <span class="ar-module-state-text">{{ error }}</span>
      <slot name="error-actions">
        <button class="ar-module-state-btn" @click="$emit('retry')">重试</button>
      </slot>
    </div>

    <div v-else-if="empty && !loading" class="ar-module-state ar-module-state--empty">
      <span class="ar-module-state-icon">📭</span>
      <span class="ar-module-state-text">{{ emptyText || '暂无数据' }}</span>
      <slot name="empty-actions" />
    </div>

    <div v-else class="ar-module-content">
      <transition name="ar-search-panel-slide">
        <div
          v-if="showSearchPanel && !searchPanelCollapsed && !error"
          class="ar-module-search-panel"
        >
          <slot name="search-panel">
            <SearchPanel
              :filters="filters"
              :active-filters="activeFilters"
              @filter-change="handleFilterChange"
              @clear-all="handleClearAllFilters"
            />
          </slot>
          <button
            class="ar-module-search-toggle"
            @click="handleToggleSearchPanel"
          >收起筛选</button>
        </div>
      </transition>

      <div class="ar-module-view-content">
        <div
          v-if="currentView === 'list'"
          class="ar-module-view ar-module-view--list"
        >
          <slot name="list-view" />
        </div>
        <div
          v-if="currentView === 'kanban'"
          class="ar-module-view ar-module-view--kanban"
        >
          <slot name="kanban-view" />
        </div>
        <div
          v-if="currentView === 'calendar'"
          class="ar-module-view ar-module-view--calendar"
        >
          <slot name="calendar-view" />
        </div>
        <div
          v-if="currentView === 'graph'"
          class="ar-module-view ar-module-view--graph"
        >
          <slot name="graph-view" />
        </div>
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import ControlPanel from './ControlPanel.vue'
import SearchPanel from './SearchPanel.vue'
import type { BreadcrumbItem, ViewOption, FilterItem, ModuleTabsItem } from './types'
import { DEFAULT_VIEW_OPTIONS, DEFAULT_PAGE_SIZE_OPTIONS } from './types'

interface Props {
  breadcrumbItems?: BreadcrumbItem[]
  currentView?: string
  availableViews?: ViewOption[]
  showViewSwitch?: boolean
  selectedCount?: number
  showSearch?: boolean
  searchPlaceholder?: string
  searchValue?: string
  showFilterButton?: boolean
  activeFilterCount?: number
  showPagination?: boolean
  currentPage?: number
  totalPages?: number
  pageSize?: number
  pageSizeOptions?: number[]
  totalItems?: number
  showSearchPanel?: boolean
  searchPanelCollapsed?: boolean
  filters?: FilterItem[]
  activeFilters?: Record<string, any>
  tabs?: ModuleTabsItem[]
  activeTab?: string
  loading?: boolean
  error?: string | null
  empty?: boolean
  emptyText?: string
}

const props = withDefaults(defineProps<Props>(), {
  breadcrumbItems: () => [],
  currentView: 'list',
  availableViews: () => DEFAULT_VIEW_OPTIONS,
  showViewSwitch: true,
  selectedCount: 0,
  showSearch: true,
  searchPlaceholder: '搜索...',
  searchValue: '',
  showFilterButton: true,
  activeFilterCount: 0,
  showPagination: true,
  currentPage: 1,
  totalPages: 1,
  pageSize: 10,
  pageSizeOptions: () => DEFAULT_PAGE_SIZE_OPTIONS,
  totalItems: 0,
  showSearchPanel: false,
  searchPanelCollapsed: false,
  filters: () => [],
  activeFilters: () => ({}),
  tabs: () => [],
  activeTab: '',
  loading: false,
  error: null,
  empty: false,
  emptyText: ''
})

const emit = defineEmits<{
  (e: 'breadcrumbClick', item: BreadcrumbItem, index: number): void
  (e: 'viewChange', view: string): void
  (e: 'clearSelection'): void
  (e: 'searchInput', value: string): void
  (e: 'searchSubmit', value: string): void
  (e: 'filterToggle'): void
  (e: 'pageChange', page: number): void
  (e: 'pageSizeChange', size: number): void
  (e: 'searchPanelToggle', collapsed: boolean): void
  (e: 'filterChange', key: string, value: any): void
  (e: 'clearAllFilters'): void
  (e: 'tabChange', key: string): void
  (e: 'update:activeTab', key: string): void
  (e: 'retry'): void
}>()

const searchPanelCollapsed = ref(props.searchPanelCollapsed)

watch(() => props.searchPanelCollapsed, (val) => {
  searchPanelCollapsed.value = val
})

// ── handlers ──────────────────────────────────────────
const handleBreadcrumbClick = (item: BreadcrumbItem, index: number) => {
  emit('breadcrumbClick', item, index)
}
const handleViewChange = (view: string) => {
  emit('viewChange', view)
}
const handleClearSelection = () => {
  emit('clearSelection')
}
const handleSearchInput = (value: string) => {
  emit('searchInput', value)
}
const handleSearchSubmit = (value: string) => {
  emit('searchSubmit', value)
}
const handleFilterToggle = () => {
  // 统一的筛选面板切换 - 替代旧的 searchPanelToggle 和 filterToggle 二重奏
  searchPanelCollapsed.value = !searchPanelCollapsed.value
  emit('searchPanelToggle', searchPanelCollapsed.value)
  emit('filterToggle')
}
const handleToggleSearchPanel = () => {
  // 仅切换面板，不触发 filterToggle
  searchPanelCollapsed.value = !searchPanelCollapsed.value
  emit('searchPanelToggle', searchPanelCollapsed.value)
}
const handlePageChange = (page: number) => {
  emit('pageChange', page)
}
const handlePageSizeChange = (size: number) => {
  emit('pageSizeChange', size)
}
const handleFilterChange = (key: string, value: any) => {
  emit('filterChange', key, value)
}
const handleClearAllFilters = () => {
  emit('clearAllFilters')
}
const handleTabClick = (key: string) => {
  emit('update:activeTab', key)
  emit('tabChange', key)
}
</script>

<style lang="scss" scoped>
.ar-module-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--ar-bg-color-page, #f2f3f5);
}

// ── Tab 导航 ──────────────────────────────────────────
.ar-module-tabs-nav {
  display: flex;
  gap: 0;
  background-color: var(--ar-bg-color, #ffffff);
  border-bottom: 1px solid var(--ar-border-color-light, #e4e7ed);
  padding: 0 var(--ar-spacing-lg, 16px);
  overflow-x: auto;

  &::-webkit-scrollbar {
    height: 0;
  }
}

.ar-module-tab-btn {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-xs, 4px);
  padding: var(--ar-spacing-md, 12px) var(--ar-spacing-lg, 16px);
  border: none;
  background: transparent;
  color: var(--ar-text-color-regular, #606266);
  font-size: var(--ar-font-size-base, 14px);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all var(--ar-transition-duration, 0.2s);
  white-space: nowrap;
  flex-shrink: 0;

  &:hover:not(:disabled) {
    color: var(--ar-color-primary, #409eff);
  }

  &:disabled {
    cursor: not-allowed;
    color: var(--ar-text-color-disabled, #c0c4cc);
  }

  &--active {
    color: var(--ar-color-primary, #409eff);
    border-bottom-color: var(--ar-color-primary, #409eff);
    font-weight: 500;
  }
}

.ar-module-tab-label {
  font-size: var(--ar-font-size-base, 14px);
}

.ar-module-tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  background-color: var(--ar-color-info-lighter, #f4f4f5);
  color: var(--ar-text-color-secondary, #909399);
  font-size: var(--ar-font-size-extra-small, 12px);
  border-radius: 9px;
}

// ── 状态提示 ──────────────────────────────────────────
.ar-module-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--ar-spacing-md, 12px);
  padding: var(--ar-spacing-xl, 48px);
  text-align: center;

  &--error {
    color: var(--ar-color-danger, #f56c6c);
  }

  &--empty {
    color: var(--ar-text-color-secondary, #909399);
  }
}

.ar-module-state-icon {
  font-size: 48px;
  line-height: 1;
}

.ar-module-state-text {
  font-size: var(--ar-font-size-base, 14px);
  color: var(--ar-text-color-regular, #606266);
}

.ar-module-state-btn {
  padding: var(--ar-spacing-xs, 4px) var(--ar-spacing-md, 12px);
  border: 1px solid var(--ar-border-color-base, #dcdfe6);
  background-color: var(--ar-bg-color, #ffffff);
  color: var(--ar-text-color-regular, #606266);
  font-size: var(--ar-font-size-base, 14px);
  border-radius: var(--ar-border-radius-base, 4px);
  cursor: pointer;
  transition: all var(--ar-transition-duration, 0.2s);

  &:hover {
    border-color: var(--ar-color-primary, #409eff);
    color: var(--ar-color-primary, #409eff);
  }
}

// ── 内容区域 ──────────────────────────────────────────
.ar-module-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.ar-module-search-panel {
  width: 240px;
  min-width: 240px;
  background-color: var(--ar-bg-color, #ffffff);
  border-right: 1px solid var(--ar-border-color-light, #e4e7ed);
  display: flex;
  flex-direction: column;
}

.ar-module-search-toggle {
  padding: var(--ar-spacing-sm, 8px);
  border: none;
  background-color: var(--ar-fill-color-light, #f5f7fa);
  color: var(--ar-text-color-regular, #606266);
  font-size: var(--ar-font-size-small, 13px);
  cursor: pointer;
  transition: all var(--ar-transition-duration, 0.2s);

  &:hover {
    background-color: var(--ar-fill-color, #f0f2f5);
    color: var(--ar-color-primary, #409eff);
  }
}

// SearchPanel 滑入滑出过渡
.ar-search-panel-slide-enter-active,
.ar-search-panel-slide-leave-active {
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1),
    min-width 0.25s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.2s ease;
  overflow: hidden;
}

.ar-search-panel-slide-enter-from,
.ar-search-panel-slide-leave-to {
  width: 0 !important;
  min-width: 0 !important;
  opacity: 0;
}

.ar-module-view-content {
  flex: 1;
  overflow: auto;
  padding: var(--ar-spacing-lg, 16px);
  background-color: var(--ar-bg-color, #ffffff);
}

.ar-module-view {
  width: 100%;
  height: 100%;
}

@media (max-width: 768px) {
  .ar-module-tabs-nav {
    padding: 0 var(--ar-spacing-sm, 8px);
  }

  .ar-module-tab-btn {
    padding: var(--ar-spacing-sm, 8px) var(--ar-spacing-md, 12px);
  }

  .ar-module-search-panel {
    position: fixed;
    left: 0;
    top: 0;
    width: 280px !important;
    min-width: 280px !important;
    height: 100vh;
    z-index: var(--ar-index-modal, 4000);
    box-shadow: var(--ar-box-shadow-base, 0 2px 12px 0 rgba(0, 0, 0, 0.1));

    &.ar-search-panel-slide-leave-active {
      // 移动端用 slide-left 动画
      transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }
    &.ar-search-panel-slide-leave-to {
      width: 280px !important;
      min-width: 280px !important;
      transform: translateX(-100%);
    }
  }

  .ar-module-view-content {
    padding: var(--ar-spacing-sm, 8px);
  }
}
</style>
