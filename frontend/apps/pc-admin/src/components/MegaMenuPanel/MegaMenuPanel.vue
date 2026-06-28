<template>
  <div
    ref="panelRef"
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
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getIcon } from '@/utils/iconMap'
import type { MenuInfo } from '@/api/menu'
import type { TriggerPosition } from '@/composables/useHoverDelay'

/** 面板距视口顶部/底部的安全边距 (px) */
const VIEWPORT_PAD = 16

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
  navigate: []
  measure: [height: number]
}>()

const router = useRouter()
const userStore = useUserStore()
const panelRef = ref<HTMLElement | null>(null)

/** 面板自然高度（不受 max-height 约束时的真实高度） */
const panelNaturalHeight = ref(0)
/** 是否需要限高（面板自然高度超出可用空间时启用） */
const shouldConstrain = ref(false)
/** 限高后的实际 top 值 */
const constrainedTop = ref<number | null>(null)

/**
 * 测量面板自然高度并计算定位策略
 *
 * 业界做法（Floating UI / Ant Design / Amazon）：
 * ① 自然高度 <= 视口可用高度：面板完整展示，顶部尽量对齐触发项
 *    - 若底部超出视口 → 整体上移（保持自然高度，不滚动）
 * ② 自然高度 > 视口可用高度：限高 + 内部滚动
 *    - 定位：优先与触发项对齐；若底部超出则贴顶
 */
function measureAndPosition() {
  const el = panelRef.value
  if (!el) return

  // 先解除高度约束，测出真实自然高度
  el.style.maxHeight = 'none'
  const naturalH = el.scrollHeight
  panelNaturalHeight.value = naturalH
  emit('measure', naturalH)

  const availableH = window.innerHeight - VIEWPORT_PAD * 2
  const pos = props.triggerPos
  const desiredTop = pos ? pos.top : 0

  if (naturalH <= availableH) {
    // ① 面板不超高：完整展示，不需要内部滚动
    shouldConstrain.value = false

    // 计算从触发项顶部对齐时，面板底部是否超出视口
    if (desiredTop + naturalH > window.innerHeight - VIEWPORT_PAD) {
      // 面板底部超出 → 整体上移，使底部贴安全边距
      constrainedTop.value = Math.max(VIEWPORT_PAD, window.innerHeight - VIEWPORT_PAD - naturalH)
    } else {
      // 正常对齐触发项
      constrainedTop.value = null
    }
  } else {
    // ② 面板超高：限高 + 内部滚动
    shouldConstrain.value = true

    if (desiredTop + naturalH <= window.innerHeight - VIEWPORT_PAD) {
      // 从触发项对齐时面板底部不超出 → 保持对齐（面板会被限高截断）
      constrainedTop.value = desiredTop
    } else {
      // 超出视口 → 面板顶部贴安全边距
      constrainedTop.value = VIEWPORT_PAD
    }
  }
}

onMounted(() => {
  nextTick(measureAndPosition)
})

/** 触发位置变化时（悬停不同侧边栏项）重新计算 */
watch(() => props.triggerPos, () => {
  // 重置状态，等 DOM 更新后重新测量
  shouldConstrain.value = false
  constrainedTop.value = null
  nextTick(measureAndPosition)
})

/** 过滤出有子菜单项的二级目录作为列 */
const visibleColumns = computed(() => {
  return props.menuItems.filter(col => col.children?.length)
})

/** 面板定位样式 */
const panelStyle = computed(() => {
  const pos = props.triggerPos
  const left = 140

  if (!pos) {
    return { left: `${left}px`, top: '0' }
  }

  const top = constrainedTop.value ?? pos.top

  const style: Record<string, string> = {
    left: `${left}px`,
    top: `${top}px`,
  }

  // 超高时：限高 + 内部滚动 + 防穿透
  if (shouldConstrain.value) {
    const maxH = window.innerHeight - top - VIEWPORT_PAD
    style.maxHeight = `${Math.max(200, maxH)}px`
    style.overflowY = 'auto'
    style.overscrollBehavior = 'contain'
  }

  return style
})

/** 权限校验 */
function hasPermission(item: MenuInfo): boolean {
  if (!item.menuCode) return true
  return userStore.hasPermission(item.menuCode)
}

/** 导航到菜单路由 — 添加标签时主按钮跳列表页，历史/列表标签跳表单页 */
function navigateTo(item: MenuInfo) {
  // 添加标签：主按钮跳 listPath（列表页）；历史/列表标签：主按钮跳 path（表单页）
  const isAdd = item.tagLabel === '添加'
  const target = isAdd ? item.listPath : item.path
  if (target) {
    const path = target.startsWith('/') ? target : '/' + target
    emit('navigate')
    router.push(path)
  }
}

/** 导航到双入口标签页 — 添加标签时标签按钮跳表单页，历史/列表标签跳列表页 */
function navigateToList(item: MenuInfo) {
  // 添加标签：标签按钮跳 path（表单页）；历史/列表标签：标签按钮跳 listPath（列表页）
  const isAdd = item.tagLabel === '添加'
  const target = isAdd ? item.path : item.listPath
  if (target) {
    const path = target.startsWith('/') ? target : '/' + target
    emit('navigate')
    router.push(path)
  }
}
</script>

<style scoped>
.mega-menu-panel {
  position: fixed;
  background: #fff;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.15),
              0 1px 6px rgba(0, 0, 0, 0.10);
  z-index: 999;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 12px 8px;
  /* 防止内部滚动穿透到页面 — 滚到面板顶/底时不会带动背景页面 */
  overscroll-behavior: contain;
}

.mega-menu-columns {
  display: flex;
  flex-wrap: nowrap;
  gap: 0;
  align-items: flex-start;
}

.mega-menu-column {
  min-width: 140px;
  max-width: 180px;
  padding: 0 10px;
  border-right: 1px solid #f0f0f0;
}

.mega-menu-column:last-child {
  border-right: none;
}

.mega-menu-column-header {
  font-size: 13px;
  font-weight: 600;
  color: #262626;
  padding: 0 0 8px 0;
  margin-bottom: 6px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.column-header-icon {
  font-size: 14px;
  color: #1677ff;
}

.mega-menu-column-items {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

/* 子分组标题 */
.mega-menu-subgroup {
  font-size: 12px;
  font-weight: 500;
  color: #8c8c8c;
  padding: 8px 0 3px 0;
  margin-top: 2px;
}

/* 菜单项行 */
.mega-menu-item-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  padding: 4px 6px;
  border-radius: 4px;
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

/* 双入口标签按钮 - 始终显示边框和文字 */
.mega-menu-tag-btn {
  flex-shrink: 0;
  font-size: 11px !important;
  height: 20px !important;
  line-height: 18px !important;
  padding: 0 6px !important;
  border-radius: 3px;
  border: 1px solid #d9d9d9 !important;
  color: #595959 !important;
  background: #fafafa !important;
}

.mega-menu-tag-btn:hover {
  color: #1677ff !important;
  border-color: #1677ff !important;
  background: #fff !important;
}

.mega-menu-tag-btn.ant-btn-primary {
  border-color: #1677ff !important;
  color: #1677ff !important;
  background: #e6f4ff !important;
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
