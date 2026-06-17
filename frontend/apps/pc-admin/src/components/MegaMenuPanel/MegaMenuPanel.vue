<template>
  <div
    class="mega-menu-panel"
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
              class="mega-menu-item"
            >
              <div class="mega-menu-item-row">
                <a class="mega-menu-item-link" @click="navigateTo(item)">
                  {{ item.menuName }}
                  <LinkOutlined v-if="item.linkIcon" class="link-icon-indicator" />
                </a>
                <a-button
                  v-if="item.displayMode === 1 && item.listPath"
                  size="small"
                  class="mega-menu-tag-btn"
                  @click="navigateToList(item)"
                >
                  {{ item.tagLabel || '列表' }}
                </a-button>
              </div>
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
import { LinkOutlined } from '@ant-design/icons-vue'
import type { MenuInfo } from '@/api/menu'

const props = withDefaults(defineProps<{
  menuItems: MenuInfo[]
}>(), {
  menuItems: () => []
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
  left: 140px;
  top: 0;
  height: 100vh;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  box-shadow: 4px 0 12px rgba(0, 0, 0, 0.08);
  z-index: 999;
  overflow-y: auto;
  padding: 20px 24px;
}

.mega-menu-columns {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 24px;
  align-items: start;
}

.mega-menu-column {
  min-width: 160px;
}

.mega-menu-column-header {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  padding: 0 0 12px 0;
  margin-bottom: 8px;
  border-bottom: 2px solid #e8e8e8;
  display: flex;
  align-items: center;
  gap: 6px;
}

.column-header-icon {
  font-size: 16px;
  color: #409eff;
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
  color: #606266;
  padding: 8px 8px 4px 8px;
  margin-top: 4px;
  border-left: 3px solid #ff4d4f;
}

/* 菜单项行 */
.mega-menu-item-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.mega-menu-item-row:hover {
  background-color: #f5f7fa;
}

.mega-menu-item-link {
  flex: 1;
  font-size: 13px;
  color: #606266;
  text-decoration: none;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mega-menu-item-link:hover {
  color: #409eff;
}

.link-icon-indicator {
  font-size: 12px;
  color: #999;
  margin-left: 4px;
}

/* 双入口标签按钮 */
.mega-menu-tag-btn {
  flex-shrink: 0;
  font-size: 11px;
  padding: 0 8px;
  height: 22px;
  line-height: 22px;
  border-radius: 4px;
  min-width: 36px;
}

.mega-menu-tag-btn:hover {
  color: #409eff;
  border-color: #409eff;
}
</style>
