<template>
  <div class="ar-control-panel" :class="{ 'ar-control-panel--loading': loading }">
    <!-- 加载进度条 -->
    <div v-if="loading" class="ar-cp-loading-bar">
      <div class="ar-cp-loading-bar-inner" />
    </div>

    <div class="ar-cp-top">
      <div class="ar-cp-top-left">
        <slot name="breadcrumb">
          <div class="ar-cp-breadcrumb">
            <span
              v-for="(item, index) in breadcrumbItems"
              :key="index"
              class="ar-cp-breadcrumb-item"
              :class="{ 'ar-cp-breadcrumb-item--active': index === breadcrumbItems.length - 1 }"
              @click="handleBreadcrumbClick(item, index)"
            >
              <span class="ar-cp-breadcrumb-text">{{ item.text }}</span>
              <span
                v-if="index < breadcrumbItems.length - 1"
                class="ar-cp-breadcrumb-separator"
              >/</span>
            </span>
          </div>
        </slot>
      </div>
      <div class="ar-cp-top-right">
        <slot name="view-switch">
          <div
            v-if="showViewSwitch && availableViews.length > 0"
            class="ar-cp-view-switch"
          >
            <button
              v-for="view in availableViews"
              :key="view.value"
              class="ar-cp-view-btn"
              :class="{ 'ar-cp-view-btn--active': currentView === view.value }"
              :title="view.label"
              @click="handleViewChange(view.value)"
            >
              <span class="ar-cp-view-icon">{{ view.icon }}</span>
              <span class="ar-cp-view-text">{{ view.label }}</span>
            </button>
          </div>
        </slot>
      </div>
    </div>

    <div class="ar-cp-bottom">
      <div class="ar-cp-bottom-left">
        <div
          v-if="selectedCount > 0"
          class="ar-cp-selected-info"
        >
          <span class="ar-cp-selected-count">已选中 {{ selectedCount }} 项</span>
          <button
            class="ar-cp-clear-btn"
            @click="handleClearSelection"
          >清除</button>
        </div>
        <div class="ar-cp-actions">
          <slot name="actions" />
        </div>
        <div
          v-if="selectedCount > 0"
          class="ar-cp-batch-actions"
        >
          <slot name="batch-actions" />
        </div>
      </div>
      <div class="ar-cp-bottom-right">
        <slot name="search">
          <div
            v-if="showSearch"
            class="ar-cp-search"
          >
            <input
              ref="searchInputRef"
              v-model="searchValue"
              type="text"
              class="ar-cp-search-input"
              :placeholder="searchPlaceholder"
              @input="handleSearchInput"
              @keyup.enter="handleSearchSubmit"
            />
            <button
              class="ar-cp-search-btn"
              @click="handleSearchSubmit"
            >搜索</button>
          </div>
        </slot>
        <slot name="filter">
          <button
            v-if="showFilterButton"
            class="ar-cp-filter-btn"
            @click="handleFilterToggle"
          >
            <span class="ar-cp-filter-icon">筛选</span>
            <span
              v-if="activeFilterCount > 0"
              class="ar-cp-filter-count"
            >{{ activeFilterCount }}</span>
          </button>
        </slot>
        <slot name="pagination">
          <div
            v-if="showPagination && totalItems > 0"
            class="ar-cp-pagination"
          >
            <span class="ar-cp-pagination-info">共 {{ totalItems }} 条</span>
            <button
              class="ar-cp-pagination-btn"
              :disabled="currentPage <= 1"
              @click="handlePageChange(currentPage - 1)"
            >上一页</button>
            <span class="ar-cp-pagination-info">{{ currentPage }} / {{ computedTotalPages }}</span>
            <button
              class="ar-cp-pagination-btn"
              :disabled="currentPage >= computedTotalPages"
              @click="handlePageChange(currentPage + 1)"
            >下一页</button>
            <select
              v-model="pageSizeValue"
              class="ar-cp-pagination-size"
              @change="handlePageSizeChange"
            >
              <option
                v-for="size in pageSizeOptions"
                :key="size"
                :value="size"
              >{{ size }}条/页</option>
            </select>
          </div>
        </slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import type { BreadcrumbItem, ViewOption } from './types'
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
  loading?: boolean
  searchDebounce?: number
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
  loading: false,
  searchDebounce: 300
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
  (e: 'update:searchValue', value: string): void
  (e: 'update:pageSize', size: number): void
}>()

// ── search ref ────────────────────────────────────────
const searchInputRef = ref<HTMLInputElement | null>(null)
const searchValue = ref(props.searchValue)
let debounceTimer: ReturnType<typeof setTimeout> | null = null

// ── page size sync ────────────────────────────────────
const pageSizeValue = ref(props.pageSize)

// ── computed total pages ──────────────────────────────
const computedTotalPages = computed(() => {
  if (props.totalPages > 0) return props.totalPages
  if (props.totalItems > 0 && props.pageSize > 0) {
    return Math.max(1, Math.ceil(props.totalItems / props.pageSize))
  }
  return 1
})

// ── watch props ───────────────────────────────────────
watch(() => props.searchValue, (val) => {
  searchValue.value = val
})

watch(() => props.pageSize, (val) => {
  pageSizeValue.value = val
})

// ── handlers ──────────────────────────────────────────
const handleBreadcrumbClick = (item: BreadcrumbItem, index: number) => {
  if (index < props.breadcrumbItems.length - 1) {
    emit('breadcrumbClick', item, index)
  }
}

const handleViewChange = (view: string) => {
  emit('viewChange', view)
}

const handleClearSelection = () => {
  emit('clearSelection')
}

const handleSearchInput = () => {
  // debounce
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  debounceTimer = setTimeout(() => {
    emit('update:searchValue', searchValue.value)
    emit('searchInput', searchValue.value)
  }, props.searchDebounce)
}

const handleSearchSubmit = () => {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  emit('update:searchValue', searchValue.value)
  emit('searchSubmit', searchValue.value)
}

const handleFilterToggle = () => {
  emit('filterToggle')
}

const handlePageChange = (page: number) => {
  if (page < 1 || page > computedTotalPages.value) return
  emit('pageChange', page)
}

const handlePageSizeChange = () => {
  emit('update:pageSize', pageSizeValue.value)
  emit('pageSizeChange', pageSizeValue.value)
}

// ── keyboard shortcut ─────────────────────────────────
const handleGlobalKeydown = (e: KeyboardEvent) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    searchInputRef.value?.focus()
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleGlobalKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleGlobalKeydown)
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
})
</script>

<style lang="scss" scoped>
.ar-control-panel {
  background-color: var(--ar-bg-color, #ffffff);
  border-bottom: 1px solid var(--ar-border-color-light, #e4e7ed);
  padding: var(--ar-spacing-md, 12px) var(--ar-spacing-lg, 16px);
  display: flex;
  flex-direction: column;
  gap: var(--ar-spacing-md, 12px);
  position: relative;

  &--loading {
    pointer-events: none;
    opacity: 0.7;
  }
}

// 加载进度条
.ar-cp-loading-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background-color: var(--ar-fill-color-light, #f5f7fa);
  overflow: hidden;
  z-index: 1;
}

.ar-cp-loading-bar-inner {
  width: 40%;
  height: 100%;
  background-color: var(--ar-color-primary, #409eff);
  animation: ar-cp-loading-slide 1.5s ease-in-out infinite;
}

@keyframes ar-cp-loading-slide {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(350%);
  }
}

.ar-cp-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 32px;
}

.ar-cp-top-left {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-sm, 8px);
}

.ar-cp-breadcrumb {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-xs, 4px);
  font-size: var(--ar-font-size-base, 14px);
}

.ar-cp-breadcrumb-item {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-xs, 4px);
  cursor: pointer;
  color: var(--ar-text-color-regular, #606266);
  transition: color var(--ar-transition-duration, 0.2s);

  &:hover:not(.ar-cp-breadcrumb-item--active) {
    color: var(--ar-color-primary, #409eff);
  }

  &--active {
    color: var(--ar-text-color-primary, #303133);
    cursor: default;
    font-weight: 500;
  }
}

.ar-cp-breadcrumb-separator {
  color: var(--ar-text-color-placeholder, #c0c4cc);
}

.ar-cp-top-right {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-sm, 8px);
}

.ar-cp-view-switch {
  display: flex;
  gap: var(--ar-spacing-xs, 4px);
  background-color: var(--ar-fill-color-light, #f5f7fa);
  border-radius: var(--ar-border-radius-base, 4px);
  padding: var(--ar-spacing-xs, 4px);
}

.ar-cp-view-btn {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-xs, 4px);
  padding: var(--ar-spacing-xs, 4px) var(--ar-spacing-sm, 8px);
  border: none;
  background: transparent;
  color: var(--ar-text-color-regular, #606266);
  font-size: var(--ar-font-size-small, 13px);
  cursor: pointer;
  border-radius: var(--ar-border-radius-small, 2px);
  transition: all var(--ar-transition-duration, 0.2s);

  &:hover {
    background-color: var(--ar-fill-color, #f0f2f5);
  }

  &--active {
    background-color: var(--ar-color-primary, #409eff);
    color: var(--ar-color-white, #ffffff);
  }
}

.ar-cp-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 32px;
  flex-wrap: wrap;
  gap: var(--ar-spacing-sm, 8px);
}

.ar-cp-bottom-left {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-md, 12px);
  flex-wrap: wrap;
}

.ar-cp-selected-info {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-sm, 8px);
  padding: var(--ar-spacing-xs, 4px) var(--ar-spacing-sm, 8px);
  background-color: var(--ar-color-primary-light-3, rgba(64, 158, 255, 0.1));
  border-radius: var(--ar-border-radius-base, 4px);
}

.ar-cp-selected-count {
  font-size: var(--ar-font-size-small, 13px);
  color: var(--ar-color-primary, #409eff);
  font-weight: 500;
}

.ar-cp-clear-btn {
  padding: var(--ar-spacing-xs, 4px);
  border: none;
  background: transparent;
  color: var(--ar-color-primary, #409eff);
  font-size: var(--ar-font-size-small, 13px);
  cursor: pointer;

  &:hover {
    color: var(--ar-color-primary-dark-2, #3a8ee6);
  }
}

.ar-cp-actions {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-sm, 8px);
}

.ar-cp-batch-actions {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-sm, 8px);
  padding-left: var(--ar-spacing-md, 12px);
  border-left: 1px solid var(--ar-border-color-light, #e4e7ed);
}

.ar-cp-bottom-right {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-md, 12px);
  flex-wrap: wrap;
}

.ar-cp-search {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-xs, 4px);
}

.ar-cp-search-input {
  width: 200px;
  padding: var(--ar-spacing-xs, 4px) var(--ar-spacing-sm, 8px);
  border: 1px solid var(--ar-border-color-base, #dcdfe6);
  border-radius: var(--ar-border-radius-base, 4px);
  font-size: var(--ar-font-size-base, 14px);
  color: var(--ar-text-color-primary, #303133);
  background-color: var(--ar-bg-color, #ffffff);
  transition: border-color var(--ar-transition-duration, 0.2s);

  &:focus {
    border-color: var(--ar-color-primary, #409eff);
    outline: none;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
  }

  &::placeholder {
    color: var(--ar-text-color-placeholder, #c0c4cc);
  }
}

.ar-cp-search-btn {
  padding: var(--ar-spacing-xs, 4px) var(--ar-spacing-md, 12px);
  border: 1px solid var(--ar-color-primary, #409eff);
  background-color: var(--ar-color-primary, #409eff);
  color: var(--ar-color-white, #ffffff);
  font-size: var(--ar-font-size-base, 14px);
  border-radius: var(--ar-border-radius-base, 4px);
  cursor: pointer;
  transition: all var(--ar-transition-duration, 0.2s);

  &:hover {
    background-color: var(--ar-color-primary-dark-2, #3a8ee6);
    border-color: var(--ar-color-primary-dark-2, #3a8ee6);
  }
}

.ar-cp-filter-btn {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-xs, 4px);
  padding: var(--ar-spacing-xs, 4px) var(--ar-spacing-sm, 8px);
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

.ar-cp-filter-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 var(--ar-spacing-xs, 4px);
  background-color: var(--ar-color-primary, #409eff);
  color: var(--ar-color-white, #ffffff);
  font-size: var(--ar-font-size-extra-small, 12px);
  border-radius: var(--ar-border-radius-circle, 50%);
}

.ar-cp-pagination {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-sm, 8px);
}

.ar-cp-pagination-btn {
  padding: var(--ar-spacing-xs, 4px) var(--ar-spacing-sm, 8px);
  border: 1px solid var(--ar-border-color-base, #dcdfe6);
  background-color: var(--ar-bg-color, #ffffff);
  color: var(--ar-text-color-regular, #606266);
  font-size: var(--ar-font-size-small, 13px);
  border-radius: var(--ar-border-radius-base, 4px);
  cursor: pointer;
  transition: all var(--ar-transition-duration, 0.2s);

  &:hover:not(:disabled) {
    border-color: var(--ar-color-primary, #409eff);
    color: var(--ar-color-primary, #409eff);
  }

  &:disabled {
    cursor: not-allowed;
    color: var(--ar-text-color-disabled, #c0c4cc);
    background-color: var(--ar-bg-color-disabled, #f5f7fa);
  }
}

.ar-cp-pagination-info {
  font-size: var(--ar-font-size-small, 13px);
  color: var(--ar-text-color-regular, #606266);
  white-space: nowrap;
}

.ar-cp-pagination-size {
  padding: var(--ar-spacing-xs, 4px) var(--ar-spacing-sm, 8px);
  border: 1px solid var(--ar-border-color-base, #dcdfe6);
  border-radius: var(--ar-border-radius-base, 4px);
  font-size: var(--ar-font-size-small, 13px);
  color: var(--ar-text-color-primary, #303133);
  background-color: var(--ar-bg-color, #ffffff);
  cursor: pointer;

  &:focus {
    border-color: var(--ar-color-primary, #409eff);
    outline: none;
  }
}

@media (max-width: 768px) {
  .ar-control-panel {
    padding: var(--ar-spacing-sm, 8px);
  }

  .ar-cp-top {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--ar-spacing-sm, 8px);
  }

  .ar-cp-top-right {
    width: 100%;
    justify-content: flex-start;
  }

  .ar-cp-bottom {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--ar-spacing-sm, 8px);
  }

  .ar-cp-bottom-left {
    width: 100%;
    flex-wrap: wrap;
  }

  .ar-cp-bottom-right {
    width: 100%;
    flex-wrap: wrap;
  }

  .ar-cp-search-input {
    width: 100%;
    max-width: 200px;
  }

  .ar-cp-pagination {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .ar-cp-view-text {
    display: none; // 移动端仅显示图标
  }
}
</style>
