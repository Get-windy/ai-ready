<template>
  <div class="global-search">
    <!-- 搜索触发按钮 -->
    <a-tooltip title="搜索 (Ctrl+K)">
      <a-button
        class="search-trigger"
        size="small"
        @click="openSearch"
        aria-label="全局搜索"
      >
        <template #icon><SearchOutlined /></template>
        <span class="search-trigger-text">搜索菜单和单据...</span>
        <span class="search-shortcut">Ctrl+K</span>
      </a-button>
    </a-tooltip>

    <!-- 搜索弹窗 -->
    <a-modal
      v-model:open="visible"
      :footer="null"
      :closable="false"
      :width="560"
      :destroy-on-close="true"
      class="global-search-modal"
      @cancel="handleClose"
    >
      <div class="search-container">
        <div class="search-input-wrapper">
          <SearchOutlined class="search-input-icon" />
          <input
            ref="searchInputRef"
            v-model="keyword"
            class="search-input"
            placeholder="搜索菜单、单据、客户、供应商..."
            @keydown="handleKeydown"
            @input="handleInput"
          />
          <a-button
            v-if="keyword"
            type="text"
            size="small"
            class="search-clear"
            @click="keyword = ''"
          >
            <CloseOutlined />
          </a-button>
        </div>

        <!-- 最近浏览（默认显示） -->
        <div v-if="!keyword" class="search-section">
          <div class="search-section-header">
            <span class="search-section-title">最近浏览</span>
            <a-button
              v-if="recentStore.recentList.length > 0"
              type="link"
              size="small"
              @click="recentStore.clearRecent()"
            >
              清除
            </a-button>
          </div>
          <div v-if="recentStore.recentList.length === 0" class="search-empty-hint">
            暂无浏览记录
          </div>
          <div
            v-for="item in recentStore.recentList.slice(0, 8)"
            :key="item.id"
            class="search-result-item"
            @click="navigateToItem(item)"
            @mouseenter="highlightIndex = -1"
          >
            <div class="result-item-icon">
              <component :is="getTypeIcon(item.type)" />
            </div>
            <div class="result-item-content">
              <div class="result-item-title">{{ item.title }}</div>
              <div v-if="item.bizNo" class="result-item-desc">{{ item.bizNo }}</div>
            </div>
            <div class="result-item-time">{{ formatTime(item.timestamp) }}</div>
          </div>
        </div>

        <!-- 搜索结果 -->
        <div v-else class="search-section">
          <div class="search-section-header">
            <span class="search-section-title">
              搜索结果 ({{ filteredResults.length }})
            </span>
          </div>
          <div v-if="filteredResults.length === 0" class="search-empty">
            <SearchOutlined class="search-empty-icon" />
            <p>未找到 "{{ keyword }}" 相关内容</p>
          </div>
          <div
            v-for="(item, index) in filteredResults"
            :key="item.id"
            class="search-result-item"
            :class="{ active: highlightIndex === index }"
            @click="navigateToItem(item)"
            @mouseenter="highlightIndex = index"
          >
            <div class="result-item-icon">
              <component :is="getTypeIcon(item.type)" />
            </div>
            <div class="result-item-content">
              <div class="result-item-title" v-html="highlightText(item.title)"></div>
              <div v-if="item.bizNo" class="result-item-desc" v-html="highlightText(item.bizNo)"></div>
            </div>
            <div class="result-item-type">{{ getTypeLabel(item.type) }}</div>
          </div>
        </div>

        <!-- 快捷导航提示 -->
        <div class="search-footer">
          <span><kbd>↑</kbd><kbd>↓</kbd> 导航</span>
          <span><kbd>Enter</kbd> 打开</span>
          <span><kbd>Esc</kbd> 关闭</span>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  SearchOutlined,
  CloseOutlined,
  FileTextOutlined,
  TeamOutlined,
  ShoppingCartOutlined,
  DollarOutlined,
  UserOutlined,
  AppstoreOutlined
} from '@ant-design/icons-vue'
import { useRecentStore, type RecentItem } from '@/stores/recent'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const recentStore = useRecentStore()
const userStore = useUserStore()

const visible = ref(false)
const keyword = ref('')
const highlightIndex = ref(-1)
const searchInputRef = ref<HTMLInputElement>()

interface SearchResult {
  id: string
  title: string
  path: string
  type: RecentItem['type']
  bizNo?: string
  keywords: string[]
}

// 构建所有可搜索的菜单项
const allMenus = computed<SearchResult[]>(() => {
  const results: SearchResult[] = []
  const traverse = (menus: any[], parentPath = '') => {
    for (const menu of menus) {
      const path = menu.path || parentPath
      if (menu.menuType === 1 && path) {
        results.push({
          id: `menu-${menu.id}`,
          title: menu.menuName,
          path,
          type: 'page',
          keywords: [menu.menuName, menu.menuCode || '']
        })
      }
      if (menu.children) {
        traverse(menu.children, path)
      }
    }
  }
  traverse(userStore.menus)
  return results
})

// 搜索结果过滤
const filteredResults = computed(() => {
  if (!keyword.value.trim()) return []
  const kw = keyword.value.toLowerCase().trim()
  return allMenus.value.filter(item =>
    item.keywords.some(k => k.toLowerCase().includes(kw)) ||
    item.title.toLowerCase().includes(kw) ||
    item.bizNo?.toLowerCase().includes(kw)
  )
})

function getTypeIcon(type: string) {
  const icons: Record<string, any> = {
    order: FileTextOutlined,
    customer: TeamOutlined,
    supplier: ShoppingCartOutlined,
    finance: DollarOutlined,
    product: AppstoreOutlined,
    page: UserOutlined
  }
  return icons[type] || AppstoreOutlined
}

function getTypeLabel(type: string): string {
  const labels: Record<string, string> = {
    order: '单据',
    customer: '客户',
    supplier: '供应商',
    finance: '财务',
    product: '产品',
    page: '功能'
  }
  return labels[type] || '其他'
}

function formatTime(timestamp: number): string {
  const diff = Date.now() - timestamp
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  const date = new Date(timestamp)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

function highlightText(text: string): string {
  if (!keyword.value.trim()) return text
  const kw = keyword.value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return text.replace(new RegExp(`(${kw})`, 'gi'), '<mark>$1</mark>')
}

function navigateToItem(item: RecentItem | SearchResult) {
  recentStore.addRecent({
    id: item.id,
    title: item.title,
    path: item.path,
    type: item.type,
    bizNo: item.bizNo
  })
  visible.value = false
  keyword.value = ''
  router.push(item.path)
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    const max = filteredResults.value.length
    highlightIndex.value = (highlightIndex.value + 1) % max
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    const max = filteredResults.value.length
    highlightIndex.value = (highlightIndex.value - 1 + max) % max
  } else if (e.key === 'Enter' && highlightIndex.value >= 0) {
    e.preventDefault()
    const item = filteredResults.value[highlightIndex.value]
    if (item) navigateToItem(item)
  } else if (e.key === 'Escape') {
    handleClose()
  }
}

function handleInput() {
  highlightIndex.value = filteredResults.value.length > 0 ? 0 : -1
}

function openSearch() {
  visible.value = true
  keyword.value = ''
  highlightIndex.value = -1
  nextTick(() => {
    searchInputRef.value?.focus()
  })
}

function handleClose() {
  visible.value = false
  keyword.value = ''
}

// 全局快捷键 Ctrl+K / Cmd+K
function handleGlobalKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    openSearch()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleGlobalKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleGlobalKeydown)
})
</script>

<style scoped>
.search-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--color-border-base, #d9d9d9);
  border-radius: 6px;
  background: var(--color-bg-container, #fff);
  padding: 4px 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.search-trigger:hover {
  border-color: var(--color-primary, #1890ff);
  background: var(--color-primary-bg, #e6f7ff);
}

.search-trigger-text {
  color: var(--color-text-tertiary, #999);
  font-size: 13px;
}

.search-shortcut {
  color: var(--color-text-quaternary, #ccc);
  font-size: 11px;
  border: 1px solid var(--color-border-base, #d9d9d9);
  border-radius: 3px;
  padding: 0 4px;
  line-height: 18px;
}

.global-search-modal :deep(.ant-modal-content) {
  padding: 0;
  border-radius: 8px;
  overflow: hidden;
}

.global-search-modal :deep(.ant-modal-body) {
  padding: 0;
}

.search-container {
  max-height: 500px;
  overflow-y: auto;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border-secondary, #f0f0f0);
  gap: 8px;
}

.search-input-icon {
  color: var(--color-text-tertiary, #999);
  font-size: 16px;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  background: transparent;
  color: var(--color-text, #333);
}

.search-input::placeholder {
  color: var(--color-text-quaternary, #bbb);
}

.search-clear {
  color: var(--color-text-tertiary, #999);
}

.search-section {
  padding: 8px 0;
}

.search-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 16px 8px;
}

.search-section-title {
  font-size: 12px;
  color: var(--color-text-tertiary, #999);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  cursor: pointer;
  transition: background 0.15s;
}

.search-result-item:hover,
.search-result-item.active {
  background: var(--color-primary-bg, #e6f7ff);
}

.result-item-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: var(--color-bg-container-secondary, #fafafa);
  color: var(--color-primary, #1890ff);
  font-size: 14px;
  flex-shrink: 0;
}

.result-item-content {
  flex: 1;
  min-width: 0;
}

.result-item-title {
  font-size: 14px;
  color: var(--color-text, #333);
  line-height: 1.4;
}

.result-item-title :deep(mark) {
  background: #fff3cd;
  color: #856404;
  padding: 0 2px;
  border-radius: 2px;
}

.result-item-desc {
  font-size: 12px;
  color: var(--color-text-tertiary, #999);
  margin-top: 2px;
}

.result-item-desc :deep(mark) {
  background: #fff3cd;
  color: #856404;
  padding: 0 2px;
  border-radius: 2px;
}

.result-item-time,
.result-item-type {
  font-size: 11px;
  color: var(--color-text-quaternary, #bbb);
  white-space: nowrap;
}

.search-empty {
  text-align: center;
  padding: 32px 16px;
  color: var(--color-text-tertiary, #999);
}

.search-empty-icon {
  font-size: 32px;
  margin-bottom: 8px;
  opacity: 0.4;
}

.search-empty p {
  margin: 0;
  font-size: 13px;
}

.search-empty-hint {
  padding: 8px 16px 12px;
  font-size: 13px;
  color: var(--color-text-tertiary, #999);
}

.search-footer {
  display: flex;
  gap: 16px;
  padding: 8px 16px;
  border-top: 1px solid var(--color-border-secondary, #f0f0f0);
  font-size: 11px;
  color: var(--color-text-quaternary, #bbb);
}

.search-footer kbd {
  border: 1px solid var(--color-border-base, #d9d9d9);
  border-radius: 3px;
  padding: 0 5px;
  font-size: 10px;
  line-height: 18px;
  font-family: inherit;
  background: var(--color-bg-container, #fff);
  margin: 0 2px;
}
</style>
