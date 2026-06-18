<template>
  <div
    class="mega-menu-panel"
    :style="panelStyle"
    @mouseenter="$emit('panelEnter')"
    @mouseleave="$emit('panelLeave')"
  >
    <div class="mega-menu-columns">
      <div
        v-for="column in visibleColumns"
        :key="column.id"
        class="mega-menu-column"
      >
        <div class="mega-menu-column-header">
          <component :is="getIcon(column.icon)" v-if="column.icon" class="column-header-icon" />
          <span>{{ column.menuName }}</span>
        </div>
        <div class="mega-menu-column-items">
          <template v-for="item in column.children" :key="item.id">
            <!-- displayGroup=1: 子分组标题 -->
            <div
              v-if="item.displayGroup === 1"
              class="mega-menu-subgroup"
            >
              {{ item.menuName }}
            </div>

            <!-- 普通菜单项 (需权限校验) -->
            <div
              v-else-if="item.menuType === 1 && hasPermission(item)"
              class="mega-menu-item-row"
            >
              <a
                class="mega-menu-item-link"
                @click="navigateTo(item)"
              >
                {{ item.menuName }}
              </a>
              <a-button
                v-if="item.displayMode === 1 && item.listPath"
                size="small"
                :type="item.tagLabel === '添加' ? 'primary' : 'default'"
                ghost
                class="mega-menu-tag-btn"
                @click="navigateToList(item)"
              >
                {{ item.tagLabel || '历史' }}
              </a-button>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getIcon } from '@/utils/iconMap'
import type { MenuInfo } from '@/api/menu'
import type { TriggerPosition } from '@/composables/useHoverDelay'

const props = withDefaults(defineProps<{
  menuItems: MenuInfo[]
  triggerPos?: TriggerPosition | null
}>(), {
  menuItems: () => [],
  triggerPos: null
})

const emit = defineEmits<{
  panelEnter: []
  panelLeave: []
}>()

const router = useRouter()
const userStore = useUserStore()

/** 过滤出有子菜单项的二级目录作为列 */
const visibleColumns = computed(() => {
  return props.menuItems.filter(col => col.children?.length)
})

/** 面板定位样式 */
const panelStyle = computed(() => {
  const pos = props.triggerPos
  if (!pos) {
    return { left: '140px', top: '0' }
  }

  // 面板顶部与触发项顶部对齐
  const top = pos.top

  // 面板左边与侧边栏右边缘对齐
  const left = 140

  // 计算最大可用高度（从触发项顶部到视口底部）
  const availableHeight = window.innerHeight - top - 16
  // 如果可用高度太小（触发项在底部），向上展开
  const upwardShift = availableHeight < 200
    ? Math.max(0, pos.top + pos.height + 200 - window.innerHeight)
    : 0

  const adjustedTop = top - upwardShift

  return {
    left: `${left}px`,
    top: `${adjustedTop}px`,
    maxHeight: `${Math.min(availableHeight + upwardShift, window.innerHeight - 32)}px`,
  }
})

/** 权限校验 */
function hasPermission(item: MenuInfo): boolean {
  if (!item.menuCode) return true
  return userStore.hasPermission(item.menuCode)
}

/** 导航到菜单路由 */
function navigateTo(item: MenuInfo) {
  if (item.path) {
    router.push(item.path)
  }
}

/** 导航到双入口列表页 */
function navigateToList(item: MenuInfo) {
  if (item.listPath) {
    router.push(item.listPath)
  }
}
</script>

<style scoped>
.mega-menu-panel {
  position: fixed;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  box-shadow: 0 6px 16px -8px rgba(0, 0, 0, 0.08),
              0 9px 28px 0 rgba(0, 0, 0, 0.05),
              0 12px 48px 16px rgba(0, 0, 0, 0.03);
  z-index: 999;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px 20px;
}

.mega-menu-columns {
  display: flex;
  flex-wrap: nowrap;
  gap: 0;
  align-items: flex-start;
}

.mega-menu-column {
  min-width: 170px;
  max-width: 200px;
  padding: 0 16px;
  border-right: 1px solid #f0f0f0;
}

.mega-menu-column:last-child {
  border-right: none;
}

.mega-menu-column-header {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  padding: 0 0 12px 0;
  margin-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.column-header-icon {
  font-size: 16px;
  color: #1677ff;
}

.mega-menu-column-items {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

/* 子分组标题 */
.mega-menu-subgroup {
  font-size: 12px;
  font-weight: 500;
  color: #8c8c8c;
  padding: 10px 0 4px 0;
  margin-top: 2px;
}

/* 菜单项行 */
.mega-menu-item-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 5px 8px;
  border-radius: 6px;
  transition: background-color 0.15s;
  cursor: default;
}

.mega-menu-item-row:hover {
  background-color: #f5f5f5;
}

.mega-menu-item-link {
  flex: 1;
  font-size: 13px;
  color: #595959;
  text-decoration: none;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 22px;
}

.mega-menu-item-link:hover {
  color: #1677ff;
}

/* 双入口标签按钮 */
.mega-menu-tag-btn {
  flex-shrink: 0;
  font-size: 12px !important;
  height: 24px !important;
  line-height: 22px !important;
  padding: 0 10px !important;
  border-radius: 4px;
}

.mega-menu-tag-btn:hover {
  color: #1677ff;
  border-color: #1677ff;
}

/* 滚动条样式 */
.mega-menu-panel::-webkit-scrollbar {
  width: 6px;
}

.mega-menu-panel::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 3px;
}

.mega-menu-panel::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}
</style>
