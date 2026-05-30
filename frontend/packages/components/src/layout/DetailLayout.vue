<template>
  <div class="ar-detail-layout" :class="{ 'ar-detail-layout--loading': loading }">
    <!-- 加载进度条 -->
    <div v-if="loading" class="ar-detail-loading-bar">
      <div class="ar-detail-loading-bar-inner" />
    </div>

    <!-- 错误状态 -->
    <div v-if="error" class="ar-detail-state ar-detail-state--error">
      <span class="ar-detail-state-icon">⚠️</span>
      <span class="ar-detail-state-text">{{ error }}</span>
      <button class="ar-detail-state-btn" @click="$emit('retry')">重试</button>
    </div>

    <template v-else>
      <div class="ar-detail-header">
        <div class="ar-detail-header-top">
          <div class="ar-detail-header-left">
            <slot name="breadcrumb">
              <div class="ar-detail-breadcrumb">
                <span
                  v-for="(item, index) in breadcrumbItems"
                  :key="index"
                  class="ar-detail-breadcrumb-item"
                  :class="{ 'ar-detail-breadcrumb-item--active': index === breadcrumbItems.length - 1 }"
                  @click="handleBreadcrumbClick(item, index)"
                >
                  <span class="ar-detail-breadcrumb-text">{{ item.text }}</span>
                  <span
                    v-if="index < breadcrumbItems.length - 1"
                    class="ar-detail-breadcrumb-separator"
                  >/</span>
                </span>
              </div>
            </slot>
          </div>
          <div class="ar-detail-header-right">
            <slot name="pager">
              <div
                v-if="showPager"
                class="ar-detail-pager"
              >
                <button
                  class="ar-detail-pager-btn"
                  :disabled="currentIndex <= 1"
                  @click="handlePrev"
                >上一条</button>
                <span class="ar-detail-pager-info">{{ currentIndex }} / {{ totalCount }}</span>
                <button
                  class="ar-detail-pager-btn"
                  :disabled="currentIndex >= totalCount"
                  @click="handleNext"
                >下一条</button>
              </div>
            </slot>
          </div>
        </div>

        <div class="ar-detail-header-bottom">
          <div class="ar-detail-header-info">
            <slot name="header-info">
              <h2 class="ar-detail-title">{{ title }}</h2>
              <transition name="ar-status-fade">
                <span
                  v-if="status"
                  class="ar-detail-status"
                  :class="`ar-detail-status--${statusType}`"
                >{{ status }}</span>
              </transition>
            </slot>
          </div>
          <div class="ar-detail-header-actions">
            <slot name="header-extra" />
            <slot name="actions" />
            <slot name="more-actions">
              <div
                v-if="showMoreActions && moreActions.length > 0"
                ref="moreActionsRef"
                class="ar-detail-more-actions"
              >
                <button
                  class="ar-detail-more-btn"
                  @click.stop="toggleMoreActions"
                >更多操作</button>
                <transition name="ar-more-menu-fade">
                  <div
                    v-if="moreActionsVisible"
                    class="ar-detail-more-menu"
                  >
                    <button
                      v-for="action in moreActions"
                      :key="action.key"
                      class="ar-detail-more-item"
                      :class="{ 'ar-detail-more-item--danger': action.danger }"
                      :disabled="action.disabled"
                      @click="handleMoreActionClick(action)"
                    >{{ action.label }}</button>
                  </div>
                </transition>
              </div>
            </slot>
          </div>
        </div>
      </div>

      <div class="ar-detail-body">
        <div class="ar-detail-tabs">
          <slot name="tabs">
            <div class="ar-detail-tabs-nav">
              <button
                v-for="tab in tabs"
                :key="tab.key"
                class="ar-detail-tab-btn"
                :class="{ 'ar-detail-tab-btn--active': activeTab === tab.key }"
                @click="handleTabChange(tab.key)"
              >
                {{ tab.label }}
                <span
                  v-if="tab.count !== undefined"
                  class="ar-detail-tab-count"
                >{{ tab.count }}</span>
              </button>
            </div>
          </slot>
        </div>

        <div class="ar-detail-content">
          <slot name="content">
            <div
              v-for="tab in tabs"
              :key="tab.key"
              v-show="activeTab === tab.key"
              class="ar-detail-tab-content"
            >
              <slot :name="`tab-${tab.key}`" />
            </div>
          </slot>
          <slot />
        </div>
      </div>

      <div
        v-if="showRelatedDocuments && relatedDocuments.length > 0"
        class="ar-detail-related"
      >
        <slot name="related">
          <div class="ar-detail-related-header">
            <h3 class="ar-detail-related-title">关联单据</h3>
          </div>
          <div class="ar-detail-related-body">
            <div
              v-for="doc in relatedDocuments"
              :key="doc.id"
              class="ar-detail-related-item"
              @click="handleRelatedClick(doc)"
            >
              <span class="ar-detail-related-type">{{ doc.type }}</span>
              <span class="ar-detail-related-no">{{ doc.no }}</span>
              <span
                v-if="doc.status"
                class="ar-detail-related-status"
              >{{ doc.status }}</span>
            </div>
          </div>
        </slot>
      </div>

      <div
        v-if="showActivityLog && activityLogs.length > 0"
        class="ar-detail-activity"
      >
        <slot name="activity">
          <div class="ar-detail-activity-header">
            <h3 class="ar-detail-activity-title">活动日志</h3>
          </div>
          <div class="ar-detail-activity-body">
            <div
              v-for="log in activityLogs"
              :key="log.id"
              class="ar-detail-activity-item"
            >
              <span class="ar-detail-activity-time">{{ log.time }}</span>
              <span class="ar-detail-activity-user">{{ log.user }}</span>
              <span class="ar-detail-activity-action">{{ log.action }}</span>
            </div>
          </div>
        </slot>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import type { BreadcrumbItem, TabItem, MoreAction, RelatedDocument, ActivityLog } from './types'

interface Props {
  breadcrumbItems?: BreadcrumbItem[]
  title?: string
  status?: string
  statusType?: 'success' | 'warning' | 'danger' | 'info' | 'default'
  showPager?: boolean
  currentIndex?: number
  totalCount?: number
  showMoreActions?: boolean
  moreActions?: MoreAction[]
  tabs?: TabItem[]
  activeTab?: string
  showRelatedDocuments?: boolean
  relatedDocuments?: RelatedDocument[]
  showActivityLog?: boolean
  activityLogs?: ActivityLog[]
  loading?: boolean
  error?: string | null
}

const props = withDefaults(defineProps<Props>(), {
  breadcrumbItems: () => [],
  title: '',
  status: '',
  statusType: 'default',
  showPager: true,
  currentIndex: 1,
  totalCount: 1,
  showMoreActions: true,
  moreActions: () => [
    { key: 'print', label: '打印' },
    { key: 'copy', label: '复制' },
    { key: 'delete', label: '删除', danger: true }
  ],
  tabs: () => [
    { key: 'basic', label: '基本信息' },
    { key: 'detail', label: '明细信息' },
    { key: 'accounting', label: '会计信息' }
  ],
  activeTab: 'basic',
  showRelatedDocuments: true,
  relatedDocuments: () => [],
  showActivityLog: true,
  activityLogs: () => [],
  loading: false,
  error: null
})

const emit = defineEmits<{
  (e: 'breadcrumbClick', item: BreadcrumbItem, index: number): void
  (e: 'prev'): void
  (e: 'next'): void
  (e: 'tabChange', key: string): void
  (e: 'moreActionClick', action: MoreAction): void
  (e: 'relatedClick', doc: RelatedDocument): void
  (e: 'update:activeTab', key: string): void
  (e: 'retry'): void
}>()

const moreActionsVisible = ref(false)
const moreActionsRef = ref<HTMLElement | null>(null)
const activeTab = ref(props.activeTab)

watch(() => props.activeTab, (val) => {
  activeTab.value = val
})

// ── handlers ──────────────────────────────────────────
const handleBreadcrumbClick = (item: BreadcrumbItem, index: number) => {
  if (index < props.breadcrumbItems.length - 1) {
    emit('breadcrumbClick', item, index)
  }
}

const handlePrev = () => {
  emit('prev')
}

const handleNext = () => {
  emit('next')
}

const toggleMoreActions = () => {
  moreActionsVisible.value = !moreActionsVisible.value
}

const handleTabChange = (key: string) => {
  activeTab.value = key
  emit('update:activeTab', key)
  emit('tabChange', key)
}

const handleMoreActionClick = (action: MoreAction) => {
  emit('moreActionClick', action)
  moreActionsVisible.value = false
}

const handleRelatedClick = (doc: RelatedDocument) => {
  emit('relatedClick', doc)
}

// ── click-outside to close more-actions ───────────────
const handleClickOutside = (e: MouseEvent) => {
  if (moreActionsRef.value && !moreActionsRef.value.contains(e.target as Node)) {
    moreActionsVisible.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside, true)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside, true)
})
</script>

<style lang="scss" scoped>
.ar-detail-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--ar-bg-color, #ffffff);

  &--loading {
    pointer-events: none;
    opacity: 0.7;
  }
}

// 加载进度条
.ar-detail-loading-bar {
  height: 2px;
  background-color: var(--ar-fill-color-light, #f5f7fa);
  overflow: hidden;
}

.ar-detail-loading-bar-inner {
  width: 40%;
  height: 100%;
  background-color: var(--ar-color-primary, #409eff);
  animation: ar-detail-loading-slide 1.5s ease-in-out infinite;
}

@keyframes ar-detail-loading-slide {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(350%); }
}

// 状态提示
.ar-detail-state {
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
}

.ar-detail-state-icon {
  font-size: 48px;
  line-height: 1;
}

.ar-detail-state-text {
  font-size: var(--ar-font-size-base, 14px);
  color: var(--ar-text-color-regular, #606266);
}

.ar-detail-state-btn {
  padding: var(--ar-spacing-xs, 4px) var(--ar-spacing-md, 12px);
  border: 1px solid var(--ar-border-color-base, #dcdfe6);
  background-color: var(--ar-bg-color, #ffffff);
  color: var(--ar-text-color-regular, #606266);
  font-size: var(--ar-font-size-base, 14px);
  border-radius: var(--ar-border-radius-base, 4px);
  cursor: pointer;

  &:hover {
    border-color: var(--ar-color-primary, #409eff);
    color: var(--ar-color-primary, #409eff);
  }
}

// ── Header ────────────────────────────────────────────
.ar-detail-header {
  border-bottom: 1px solid var(--ar-border-color-light, #e4e7ed);
}

.ar-detail-header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--ar-spacing-md, 12px) var(--ar-spacing-lg, 16px);
  background-color: var(--ar-fill-color-light, #f5f7fa);
}

.ar-detail-header-left {
  display: flex;
  align-items: center;
}

.ar-detail-breadcrumb {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-xs, 4px);
  font-size: var(--ar-font-size-base, 14px);
}

.ar-detail-breadcrumb-item {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-xs, 4px);
  cursor: pointer;
  color: var(--ar-text-color-regular, #606266);
  transition: color var(--ar-transition-duration, 0.2s);

  &:hover:not(.ar-detail-breadcrumb-item--active) {
    color: var(--ar-color-primary, #409eff);
  }

  &--active {
    color: var(--ar-text-color-primary, #303133);
    cursor: default;
  }
}

.ar-detail-breadcrumb-separator {
  color: var(--ar-text-color-placeholder, #c0c4cc);
}

.ar-detail-header-right {
  display: flex;
  align-items: center;
}

.ar-detail-pager {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-sm, 8px);
}

.ar-detail-pager-btn {
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

.ar-detail-pager-info {
  font-size: var(--ar-font-size-small, 13px);
  color: var(--ar-text-color-regular, #606266);
}

.ar-detail-header-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--ar-spacing-lg, 16px);
}

.ar-detail-header-info {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-md, 12px);
}

.ar-detail-title {
  font-size: var(--ar-font-size-large, 18px);
  font-weight: var(--ar-font-weight-primary, 500);
  color: var(--ar-text-color-primary, #303133);
  margin: 0;
}

.ar-detail-status {
  display: inline-flex;
  align-items: center;
  padding: 2px var(--ar-spacing-sm, 8px);
  font-size: var(--ar-font-size-small, 13px);
  border-radius: var(--ar-border-radius-base, 4px);

  &--success {
    background-color: var(--ar-color-success-lighter, #f0f9eb);
    color: var(--ar-color-success, #67c23a);
  }

  &--warning {
    background-color: var(--ar-color-warning-lighter, #fdf6ec);
    color: var(--ar-color-warning, #e6a23c);
  }

  &--danger {
    background-color: var(--ar-color-danger-lighter, #fef0f0);
    color: var(--ar-color-danger, #f56c6c);
  }

  &--info {
    background-color: var(--ar-color-info-lighter, #f4f4f5);
    color: var(--ar-color-info, #909399);
  }

  &--default {
    background-color: var(--ar-fill-color-light, #f5f7fa);
    color: var(--ar-text-color-regular, #606266);
  }
}

// status fade transition
.ar-status-fade-enter-active,
.ar-status-fade-leave-active {
  transition: opacity 0.2s ease;
}
.ar-status-fade-enter-from,
.ar-status-fade-leave-to {
  opacity: 0;
}

.ar-detail-header-actions {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-sm, 8px);
}

// ── More Actions ──────────────────────────────────────
.ar-detail-more-actions {
  position: relative;
}

.ar-detail-more-btn {
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

.ar-detail-more-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: var(--ar-spacing-xs, 4px);
  background-color: var(--ar-bg-color, #ffffff);
  border: 1px solid var(--ar-border-color-light, #e4e7ed);
  border-radius: var(--ar-border-radius-base, 4px);
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: var(--ar-index-popper, 2000);
  min-width: 120px;
}

.ar-detail-more-item {
  display: block;
  width: 100%;
  padding: var(--ar-spacing-sm, 8px) var(--ar-spacing-md, 12px);
  border: none;
  background: transparent;
  color: var(--ar-text-color-regular, #606266);
  font-size: var(--ar-font-size-base, 14px);
  text-align: left;
  cursor: pointer;
  transition: background-color var(--ar-transition-duration, 0.2s);

  &:hover:not(:disabled) {
    background-color: var(--ar-fill-color-light, #f5f7fa);
  }

  &:disabled {
    cursor: not-allowed;
    color: var(--ar-text-color-disabled, #c0c4cc);
  }

  &--danger {
    color: var(--ar-color-danger, #f56c6c);

    &:hover:not(:disabled) {
      background-color: var(--ar-color-danger-lighter, #fef0f0);
    }
  }
}

// more menu fade
.ar-more-menu-fade-enter-active,
.ar-more-menu-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.ar-more-menu-fade-enter-from,
.ar-more-menu-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

// ── Tabs ──────────────────────────────────────────────
.ar-detail-body {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.ar-detail-tabs {
  border-bottom: 1px solid var(--ar-border-color-light, #e4e7ed);
}

.ar-detail-tabs-nav {
  display: flex;
  padding: 0 var(--ar-spacing-lg, 16px);
}

.ar-detail-tab-btn {
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

  &:hover {
    color: var(--ar-color-primary, #409eff);
  }

  &--active {
    color: var(--ar-color-primary, #409eff);
    border-bottom-color: var(--ar-color-primary, #409eff);
  }
}

.ar-detail-tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  background-color: var(--ar-color-info-lighter, #f4f4f5);
  color: var(--ar-text-color-secondary, #909399);
  font-size: 12px;
  border-radius: 9px;
}

.ar-detail-content {
  flex: 1;
  overflow: auto;
  padding: var(--ar-spacing-lg, 16px);
}

.ar-detail-tab-content {
  width: 100%;
}

// ── Related Documents ─────────────────────────────────
.ar-detail-related {
  border-top: 1px solid var(--ar-border-color-light, #e4e7ed);
  padding: var(--ar-spacing-lg, 16px);
}

.ar-detail-related-header {
  margin-bottom: var(--ar-spacing-md, 12px);
}

.ar-detail-related-title {
  font-size: var(--ar-font-size-medium, 16px);
  font-weight: var(--ar-font-weight-primary, 500);
  color: var(--ar-text-color-primary, #303133);
  margin: 0;
}

.ar-detail-related-body {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ar-spacing-sm, 8px);
}

.ar-detail-related-item {
  display: inline-flex;
  align-items: center;
  gap: var(--ar-spacing-xs, 4px);
  padding: var(--ar-spacing-xs, 4px) var(--ar-spacing-sm, 8px);
  background-color: var(--ar-fill-color-light, #f5f7fa);
  border-radius: var(--ar-border-radius-base, 4px);
  cursor: pointer;
  transition: all var(--ar-transition-duration, 0.2s);

  &:hover {
    background-color: var(--ar-color-primary-light-3, rgba(64, 158, 255, 0.1));
    color: var(--ar-color-primary, #409eff);
  }
}

.ar-detail-related-type {
  font-size: var(--ar-font-size-small, 13px);
  color: var(--ar-text-color-secondary, #909399);
}

.ar-detail-related-no {
  font-size: var(--ar-font-size-base, 14px);
  color: var(--ar-text-color-primary, #303133);
}

.ar-detail-related-status {
  font-size: var(--ar-font-size-extra-small, 12px);
  color: var(--ar-text-color-placeholder, #c0c4cc);
}

// ── Activity Log ──────────────────────────────────────
.ar-detail-activity {
  border-top: 1px solid var(--ar-border-color-light, #e4e7ed);
  padding: var(--ar-spacing-lg, 16px);
}

.ar-detail-activity-header {
  margin-bottom: var(--ar-spacing-md, 12px);
}

.ar-detail-activity-title {
  font-size: var(--ar-font-size-medium, 16px);
  font-weight: var(--ar-font-weight-primary, 500);
  color: var(--ar-text-color-primary, #303133);
  margin: 0;
}

.ar-detail-activity-body {
  display: flex;
  flex-direction: column;
  gap: var(--ar-spacing-sm, 8px);
}

.ar-detail-activity-item {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-md, 12px);
  padding: var(--ar-spacing-sm, 8px);
  background-color: var(--ar-fill-color-lighter, #fafafa);
  border-radius: var(--ar-border-radius-base, 4px);
}

.ar-detail-activity-time {
  font-size: var(--ar-font-size-extra-small, 12px);
  color: var(--ar-text-color-placeholder, #c0c4cc);
}

.ar-detail-activity-user {
  font-size: var(--ar-font-size-small, 13px);
  color: var(--ar-color-primary, #409eff);
}

.ar-detail-activity-action {
  font-size: var(--ar-font-size-base, 14px);
  color: var(--ar-text-color-regular, #606266);
}

@media (max-width: 768px) {
  .ar-detail-header-top {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--ar-spacing-sm, 8px);
    padding: var(--ar-spacing-sm, 8px);
  }

  .ar-detail-header-bottom {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--ar-spacing-md, 12px);
    padding: var(--ar-spacing-sm, 8px);
  }

  .ar-detail-tabs-nav {
    flex-wrap: wrap;
    padding: 0 var(--ar-spacing-sm, 8px);
  }

  .ar-detail-tab-btn {
    padding: var(--ar-spacing-sm, 8px) var(--ar-spacing-md, 12px);
  }

  .ar-detail-content {
    padding: var(--ar-spacing-sm, 8px);
  }

  .ar-detail-related {
    padding: var(--ar-spacing-sm, 8px);
  }

  .ar-detail-activity {
    padding: var(--ar-spacing-sm, 8px);
  }
}
</style>
