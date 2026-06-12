<template>
  <div class="tabs-view" @contextmenu.prevent="handleContextMenu">
    <div ref="tabsScrollRef" class="tabs-scroll">
      <div
        v-for="tab in tabsStore.tabs"
        :key="tab.path"
        class="tabs-tab"
        :class="{
          active: tabsStore.activeTabPath === tab.path,
          pinned: tab.pinned,
          dirty: tab.dirty
        }"
        @click="switchTab(tab)"
        @mouseup.middle="handleMiddleClick(tab)"
        draggable="false"
      >
        <LoadingOutlined v-if="tab.loading" class="tab-loading" />
        <div class="tab-title">
          <span class="tab-title-text">{{ tab.title }}</span>
          <span v-if="tab.dirty" class="tab-dirty-dot">●</span>
        </div>
        <CloseOutlined
          v-if="!tab.pinned"
          class="tab-close"
          @click.stop="closeTab(tab)"
        />
      </div>
    </div>

    <!-- 标签页操作菜单 -->
    <a-dropdown
      v-model:open="contextMenuVisible"
      :trigger="['contextmenu']"
    >
      <div style="display: none" />
      <template #overlay>
        <a-menu @click="handleContextAction">
          <a-menu-item key="close-current">
            <CloseOutlined /> 关闭当前
          </a-menu-item>
          <a-menu-item key="close-others">
            <FileTextOutlined /> 关闭其他
          </a-menu-item>
          <a-menu-item key="close-right">
            <ArrowRightOutlined /> 关闭右侧
          </a-menu-item>
          <a-menu-divider />
          <a-menu-item key="close-all">
            <DeleteOutlined /> 关闭全部
          </a-menu-item>
          <a-menu-divider />
          <a-menu-item key="pin-current">
            <PushpinOutlined /> {{ contextTab?.pinned ? '取消固定' : '固定标签页' }}
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  CloseOutlined,
  LoadingOutlined,
  FileTextOutlined,
  ArrowRightOutlined,
  DeleteOutlined,
  PushpinOutlined
} from '@ant-design/icons-vue'
import { useTabsStore, type TabItem } from '@/stores/tabs'

const router = useRouter()
const route = useRoute()
const tabsStore = useTabsStore()

const contextMenuVisible = ref(false)
const contextTab = ref<TabItem | null>(null)
const tabsScrollRef = ref<HTMLDivElement>()

function switchTab(tab: TabItem) {
  if (tabsStore.activeTabPath !== tab.path) {
    tabsStore.activeTabPath = tab.path
    router.push(tab.path)
  }
}

function closeTab(tab: TabItem) {
  if (tab.pinned) return
  const nextPath = tabsStore.closeTab(tab.path)
  router.push(nextPath)
}

function handleMiddleClick(tab: TabItem) {
  if (!tab.pinned) {
    closeTab(tab)
  }
}

function handleContextMenu(e: MouseEvent) {
  const target = (e.target as HTMLElement).closest('.tabs-tab')
  if (target) {
    const path = target.getAttribute('data-path')
    contextTab.value = tabsStore.tabs.find(t => t.path === path) || null
    if (contextTab.value) {
      contextMenuVisible.value = true
    }
  }
}

function handleContextAction({ key }: Record<string, any>) {
  const tab = contextTab.value
  if (!tab) return

  switch (key) {
    case 'close-current':
      closeTab(tab)
      break
    case 'close-others':
      tabsStore.closeOtherTabs(tab.path)
      if (tabsStore.activeTabPath !== route.path) {
        router.push(tabsStore.activeTabPath)
      }
      break
    case 'close-right':
      tabsStore.closeRightTabs(tab.path)
      if (tabsStore.activeTabPath !== route.path) {
        router.push(tabsStore.activeTabPath)
      }
      break
    case 'close-all':
      tabsStore.closeAllTabs()
      router.push(tabsStore.activeTabPath)
      break
    case 'pin-current':
      tab.pinned = !tab.pinned
      break
  }
  contextMenuVisible.value = false
}
</script>

<style scoped>
.tabs-view {
  display: flex;
  align-items: center;
  background: var(--color-bg-container, #fff);
  border-bottom: 1px solid var(--color-border-secondary, #f0f0f0);
  padding: 0;
  height: 36px;
  user-select: none;
}

.tabs-scroll {
  display: flex;
  align-items: center;
  overflow-x: auto;
  overflow-y: hidden;
  flex: 1;
  scrollbar-width: none;
}

.tabs-scroll::-webkit-scrollbar {
  display: none;
}

.tabs-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  height: 36px;
  font-size: 13px;
  color: var(--color-text-secondary, #666);
  cursor: pointer;
  border-right: 1px solid var(--color-border-secondary, #f0f0f0);
  white-space: nowrap;
  transition: all 0.15s;
  position: relative;
  flex-shrink: 0;
}

.tabs-tab:hover {
  background: var(--color-bg-layout, #f5f5f5);
}

.tabs-tab.active {
  color: var(--color-primary, #1890ff);
  background: var(--color-primary-bg, #e6f7ff);
  border-bottom: 2px solid var(--color-primary, #1890ff);
}

.tabs-tab.pinned .tab-title-text::before {
  content: '📌 ';
  font-size: 11px;
}

.tab-title {
  display: flex;
  align-items: center;
  gap: 4px;
}

.tab-title-text {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tab-dirty-dot {
  color: var(--color-warning, #faad14);
  font-size: 10px;
}

.tab-loading {
  font-size: 12px;
  color: var(--color-primary, #1890ff);
  animation: spin 1s linear infinite;
}

.tab-close {
  font-size: 10px;
  color: var(--color-text-quaternary, #bbb);
  padding: 2px;
  border-radius: 3px;
  opacity: 0;
  transition: all 0.15s;
}

.tabs-tab:hover .tab-close {
  opacity: 1;
}

.tab-close:hover {
  background: var(--color-border-base, #d9d9d9);
  color: var(--color-text, #333);
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
