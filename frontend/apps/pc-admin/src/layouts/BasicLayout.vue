<template>
  <a-layout class="basic-layout" aria-label="主导航布局">
    <!-- 桌面端 Mega Sidebar (140px) -->
    <div
      v-if="isDesktopView || isTabletView"
      class="mega-sidebar"
      @mouseleave="hoverState.handleTargetLeave()"
    >
      <div class="mega-sidebar-logo">
        <img src="@/assets/logo.svg" alt="logo" />
      </div>

      <div class="mega-sidebar-items">
        <div
          v-for="menu in userStore.menus"
          :key="menu.id"
          class="mega-sidebar-item"
          :class="{ active: isMenuActive(menu) }"
          @mouseenter="hoverState.handleTargetEnter(menu)"
        >
          <component :is="getIcon(menu.icon)" v-if="menu.icon" class="mega-sidebar-icon" />
          <span class="mega-sidebar-label">{{ menu.menuName }}</span>
          <div v-if="isMenuActive(menu)" class="mega-sidebar-active-bar" />
        </div>
      </div>

      <!-- MegaMenuPanel 弹出面板 -->
      <Teleport to="body">
        <MegaMenuPanel
          v-if="hoverState.isOpen.value && hoverState.hoveredItem.value"
          :menu-items="hoverState.hoveredItem.value.children || []"
          @panel-enter="hoverState.handlePanelEnter()"
          @panel-leave="hoverState.handlePanelLeave()"
        />
      </Teleport>
    </div>

    <a-drawer
      v-if="!isDesktopView && !isTabletView"
      v-model:open="mobileMenuVisible"
      placement="left"
      :closable="true"
      :width="256"
    >
      <a-menu
        v-model:selected-keys="selectedKeys"
        mode="inline"
        theme="dark"
        role="navigation"
        aria-label="移动端导航菜单"
      >
        <template v-for="menu in userStore.menus" :key="menu.id">
          <a-menu-item
            :key="menu.menuCode"
            @click="handleMobileMenuClick(getFirstLeafPath(menu) || '/')"
          >
            <component :is="getIcon(menu.icon)" v-if="menu.icon" />
            <span>{{ menu.menuName }}</span>
          </a-menu-item>
        </template>
      </a-menu>
    </a-drawer>

    <a-layout>
      <a-layout-header
        class="layout-header"
        role="banner"
        :style="{ height: headerHeight }"
      >
        <div class="header-left">
          <MenuOutlined
            v-if="!isDesktopView && !isTabletView"
            class="trigger"
            role="button"
            :aria-label="t('a11y.openMenu')"
            :aria-expanded="mobileMenuVisible"
            tabindex="0"
            @click="mobileMenuVisible = true"
            @keydown.enter="mobileMenuVisible = true"
            @keydown.space.prevent="mobileMenuVisible = true"
          />

          <!-- 全局搜索 -->
          <GlobalSearch />

          <!-- 面包屑 -->
          <a-breadcrumb v-if="isDesktopView || isTabletView">
            <a-breadcrumb-item>{{ currentTitle }}</a-breadcrumb-item>
          </a-breadcrumb>
          <span v-else class="mobile-title">{{ currentTitle }}</span>
        </div>

        <div class="header-right">
          <!-- 收藏按钮 -->
          <a-tooltip title="收藏">
            <a-button
              type="text"
              size="small"
              class="header-btn"
              @click="showFavorites = !showFavorites"
            >
              <template #icon><StarOutlined :style="{ color: showFavorites ? '#faad14' : undefined }" /></template>
            </a-button>
          </a-tooltip>

          <!-- 通知 -->
          <a-dropdown v-model:open="notif.showDropdown.value" placement="bottomRight" :trigger="['click']">
            <a-badge :count="notif.unreadCount.value" :dot="notif.unreadCount.value > 0" size="small">
              <a-tooltip title="通知">
                <a-button
                  type="text"
                  size="small"
                  class="header-btn"
                  @click="notif.showDropdown.value = !notif.showDropdown.value"
                >
                  <template #icon><BellOutlined /></template>
                </a-button>
              </a-tooltip>
            </a-badge>
            <template #overlay>
              <a-menu class="notification-dropdown">
                <a-menu-item key="header" disabled style="cursor: default; height: auto; padding: 8px 16px;">
                  <div style="display: flex; justify-content: space-between; align-items: center;">
                    <strong>通知</strong>
                    <span style="font-size: 12px; color: #999;">
                      <a @click.stop="notif.markAllAsRead()" style="margin-right: 8px;">全部已读</a>
                      <a @click.stop="notif.clearAll()">清空</a>
                    </span>
                  </div>
                </a-menu-item>

                <a-menu-item v-if="notif.notifications.value.length === 0" key="empty" disabled>
                  <div style="text-align: center; padding: 20px 0; color: #999;">
                    <BellOutlined style="font-size: 24px; display: block; margin-bottom: 8px;" />
                    暂无通知
                  </div>
                </a-menu-item>

                <a-menu-item
                  v-for="item in notif.notifications.value.slice(0, 10)"
                  :key="item.id"
                  :class="{ 'notif-unread': !item.read }"
                  style="height: auto; padding: 8px 16px; white-space: normal; border-bottom: 1px solid #f0f0f0;"
                  @click="notif.goToNotificationPage()"
                >
                  <div style="display: flex; justify-content: space-between; align-items: flex-start;">
                    <div style="flex: 1; min-width: 0;">
                      <div style="font-weight: 500; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                        <a-badge v-if="!item.read" status="processing" color="#1890ff" />
                        {{ item.title }}
                      </div>
                      <div style="font-size: 12px; color: #666; margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                        {{ item.content }}
                      </div>
                      <div style="font-size: 11px; color: #bbb; margin-top: 2px;">{{ formatTime(item.time) }}</div>
                    </div>
                    <a-button
                      v-if="!item.read"
                      type="link"
                      size="small"
                      style="padding: 0 4px; min-width: auto; flex-shrink: 0;"
                      @click.stop="notif.markAsRead(item.id)"
                    >
                      标已读
                    </a-button>
                  </div>
                </a-menu-item>

                <a-menu-item
                  v-if="notif.notifications.value.length > 0"
                  key="footer"
                  disabled
                  style="cursor: default; text-align: center; height: auto; padding: 6px 16px;"
                >
                  <a @click.stop="notif.goToNotificationPage()">
                    查看全部通知 ({{ notif.unreadCount.value }} 条未读)
                  </a>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>

          <LocaleSwitcher v-if="isDesktopView" />

          <div v-if="showTenantSwitcher" class="tenant-switcher">
            <a-select
              :value="userStore.tenantId"
              size="small"
              :loading="tenantSwitching"
              style="min-width: 140px;"
              @change="handleTenantSwitch"
            >
              <a-select-option
                v-for="t in userStore.userTenants"
                :key="t.id"
                :value="t.id"
              >
                {{ t.tenantName }}
              </a-select-option>
            </a-select>
          </div>

          <a-dropdown>
            <div class="user-info" role="button" :aria-label="t('a11y.userMenu')" tabindex="0">
              <a-avatar
                :size="isMobileView ? 28 : 32"
                :src="userStore.userInfo?.avatar"
              >
                {{ userStore.nickname?.charAt(0) }}
              </a-avatar>
              <span v-if="isDesktopView" class="username">{{ userStore.nickname }}</span>
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item key="profile" @click="navigateTo('/profile')">
                  <UserOutlined /> {{ t('user.profile') }}
                </a-menu-item>
                <a-menu-item key="settings" @click="navigateTo('/system/config')">
                  <SettingOutlined /> {{ t('menu.system') }}
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" @click="handleLogout">
                  <LogoutOutlined /> {{ t('menu.logout') }}
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <!-- 多标签页导航 -->
      <TabsView />

      <!-- 收藏面板抽屉 -->
      <a-drawer
        v-model:open="showFavorites"
        title="收藏夹"
        placement="right"
        :width="320"
      >
        <template v-if="recentStore.favoriteList.length === 0">
          <a-empty description="暂无收藏" />
        </template>
        <div v-else class="favorites-list">
          <div
            v-for="item in recentStore.favoriteList"
            :key="item.id"
            class="favorite-item"
            @click="navigateTo(item.path)"
          >
            <StarFilled class="favorite-star" />
            <span>{{ item.title }}</span>
          </div>
        </div>
        <template #extra>
          <a-button type="link" danger @click="clearAllFavorites">清除全部</a-button>
        </template>
      </a-drawer>

      <a-layout-content
        id="main-content"
        class="layout-content"
        role="main"
        :style="{
          padding: contentPadding,
          minHeight: contentMinHeight
        }"
      >
        <router-view v-slot="{ Component, route }">
          <transition name="fade" mode="out-in">
            <div :key="route.fullPath" style="height: 100%">
              <keep-alive :include="cachedRoutes">
                <component :is="Component" />
              </keep-alive>
            </div>
          </transition>
        </router-view>
      </a-layout-content>
    </a-layout>
  </a-layout>

</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  MenuOutlined,
  StarOutlined,
  StarFilled,
  BellOutlined,
  LogoutOutlined
} from '@ant-design/icons-vue'
import { getIcon } from '@/utils/iconMap'
import { useUserStore } from '@/stores/user'
import { useHoverDelay } from '@/composables/useHoverDelay'
import MegaMenuPanel from '@/components/MegaMenuPanel/MegaMenuPanel.vue'
import type { MenuInfo } from '@/api/menu'
import { useTabsStore } from '@/stores/tabs'
import { useRecentStore } from '@/stores/recent'
import { userApi, type TenantInfo } from '@/api/user'
import LocaleSwitcher from '@/components/LocaleSwitcher.vue'
import GlobalSearch from '@/components/GlobalSearch/GlobalSearch.vue'
import TabsView from '@/components/TabsView/TabsView.vue'
import { useResponsive } from '@/composables/useResponsiveState'
import { useNotification } from '@/composables/useNotification'
import { getToken } from '@/utils/tokenRefresher'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const userStore = useUserStore()
const tabsStore = useTabsStore()
const recentStore = useRecentStore()
const { isMobileView, isTabletView, isDesktopView } = useResponsive()

const selectedKeys = ref(['dashboard'])
const mobileMenuVisible = ref(false)
const showFavorites = ref(false)

// Mega Menu hover 延迟控制
const hoverState = useHoverDelay(150, 300)

// 通知系统（基于WebSocket实时推送）
const notif = useNotification()

// 缓存的路由（keep-alive）
const cachedRoutes = computed(() => {
  return tabsStore.tabs.filter(t => t.routeName && t.routeName !== route.name?.toString()).map(t => t.routeName!) || []
})

	// 侧边栏展开状态持久化（多标签页切换/刷新后恢复）（使用 store 中的用户可访问租户列表）
const tenantSwitching = ref(false)
const currentTenantName = computed(() => {
  const found = userStore.userTenants.find(t => t.id === userStore.tenantId)
  return found?.tenantName || userStore.tenantName || '默认租户'
})
const showTenantSwitcher = computed(() => userStore.userTenants.length > 1)


const currentTitle = computed(() => {
  const currentPath = route.path
  const findMenuName = (menus: any[], path: string): string => {
    for (const menu of menus) {
      if (menu.path === path) return menu.menuName
      if (menu.children) {
        const found = findMenuName(menu.children, path)
        if (found) return found
      }
    }
    return ''
  }
  return findMenuName(userStore.menus, currentPath) || t('menu.dashboard')
})

const headerHeight = computed(() => {
  if (isMobileView.value) return '48px'
  if (isTabletView.value) return '56px'
  return '56px'
})

const contentPadding = computed(() => {
  if (isMobileView.value) return '12px'
  if (isTabletView.value) return '16px'
  return '16px'
})

const contentMinHeight = computed(() => {
  const header = parseInt(headerHeight.value.replace('px', ''))
  const padding = parseInt(contentPadding.value.replace('px', ''))
  const tabsHeight = 36
  return `calc(100vh - ${header + tabsHeight + padding * 2}px)`
})

// 初始化标签页
onMounted(() => {
  tabsStore.initTabs()
  recentStore.initFavorites()
  fetchTenants()
})

// 路由变化时同步标签页
watch(() => route.path, (path) => {
  tabsStore.openTab({
    path,
    title: currentTitle.value,
    routeName: route.name?.toString()
  })
  // 记录最近浏览
  if (path !== '/dashboard' && path !== '/login') {
    recentStore.addRecent({
      id: `page-${path}`,
      title: currentTitle.value,
      path,
      type: 'page'
    })
  }
  // 同步菜单展开/选中状态
  syncMenuKeys(path)
}, { immediate: true })

// 根据当前路由同步移动端菜单选中项
function syncMenuKeys(path: string) {
  for (const menu of userStore.menus) {
    const found = findMenuCodeByPath(menu, path)
    if (found) {
      selectedKeys.value = [found]
      return
    }
  }
  selectedKeys.value = ['dashboard']
}

/** 递归查找菜单项中匹配路径的 menuCode */
function findMenuCodeByPath(menu: MenuInfo, targetPath: string): string | null {
  if (menu.path === targetPath) return menu.menuCode
  if (menu.children?.length) {
    for (const child of menu.children) {
      const result = findMenuCodeByPath(child, targetPath)
      if (result) return result
    }
  }
  return null
}

/** 判断顶级菜单是否处于激活状态（当前路由是否在该菜单子树下） */
function isMenuActive(menu: MenuInfo): boolean {
  const checkActive = (items: MenuInfo[]): boolean => {
    for (const item of items) {
      if (item.path && route.path.startsWith(item.path)) return true
      if (item.children?.length && checkActive(item.children)) return true
    }
    return false
  }
  if (menu.path && route.path.startsWith(menu.path)) return true
  if (menu.children?.length && checkActive(menu.children)) return true
  return false
}

/** 获取菜单树的第一个叶子路径（移动端点击一级菜单用） */
function getFirstLeafPath(menu: MenuInfo): string | null {
  if (menu.path && menu.menuType === 1) return menu.path
  if (menu.children?.length) {
    for (const child of menu.children) {
      const path = getFirstLeafPath(child)
      if (path) return path
    }
  }
  return null
}

// 加载租户列表（从 store 或 API 刷新）
const fetchTenants = async () => {
  // 无 token 时不请求（token 过期或未登录）
  if (!getToken()) return
  try {
    const res = await userApi.getTenants()
    if (res.data) {
      userStore.userTenants = res.data
      localStorage.setItem('userTenants', JSON.stringify(res.data))
    }
  } catch (error) {
    console.error('获取租户列表失败:', error)
  }
}

/** 格式化时间显示 */
function formatTime(timeStr: string): string {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = Date.now()
  const diff = now - date.getTime()

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`

  const d = `${date.getMonth() + 1}/${date.getDate()}`
  if (date.getFullYear() === new Date(now).getFullYear()) return d
  return `${date.getFullYear()}/${d}`
}

// 切换租户
const handleTenantSwitch = async (tenantId: number) => {
  const target = userStore.userTenants.find(t => t.id === tenantId)
  if (!target) return
  tenantSwitching.value = true
  try {
    await userApi.getTenants()
    userStore.tenantId = tenantId
    localStorage.setItem('tenantId', String(tenantId))
    localStorage.setItem('tenantName', target.tenantName)
    userStore.tenantName = target.tenantName
    await userStore.getUserInfo()
    message.success('已切换到: ' + target.tenantName)
    router.push('/dashboard')
  } catch (error) {
    console.error('切换租户失败:', error)
    message.error('切换租户失败，请重试')
  } finally {
    tenantSwitching.value = false
  }
}

const navigateTo = (path: string, parentPath?: string) => {
  showFavorites.value = false
  // 如果路径是相对路径（不以 / 开头），尝试通过路由器查找完整路径
  // 处理后端返回 path="index" 的默认子路由场景
  if (!path.startsWith('/')) {
    // 用 router.getRoutes() 精确匹配路径（支持动态注册的路由）
    const allRoutes = router.getRoutes()
    const tryPaths: string[] = []

    // 如果有父级路径，优先用父路径+子路径构造
    if (parentPath) {
      const base = parentPath.startsWith('/') ? parentPath : '/' + parentPath
      tryPaths.push(`${base}/${path}`)        // /parent/path/child
      tryPaths.push(base)                     // /parent/path (path=index or path=parent)
    }
    tryPaths.push('/' + path)                 // /absolute-path

    for (const p of tryPaths) {
      const found = allRoutes.find(r => r.path === p)
      if (found) {
        router.push(found.path)
        return
      }
    }

    // 最后尝试模糊匹配以 path 结尾的路由
    const matched = allRoutes.find(r => r.path.endsWith('/' + path))
    if (matched) {
      router.push(matched.path)
      return
    }
  }
  // 确保路径以 / 开头，避免Vue Router相对路径解析导致路径叠加
  const absolutePath = path.startsWith('/') ? path : '/' + path
  router.push(absolutePath)
}

const handleMobileMenuClick = (path: string) => {
  mobileMenuVisible.value = false
  // 确保路径以 / 开头，避免Vue Router相对路径解析导致路径叠加
  const absolutePath = path.startsWith('/') ? path : '/' + path
  router.push(absolutePath)
}

const handleLogout = async () => {
  try {
    await userStore.logout()
  } catch {
    // logout API 失败（如 token 过期），直接清除本地状态
    userStore.token = ''
    userStore.userInfo = null
  }
  router.push('/login')
}

const openGlobalSearch = () => {
  // 触发全局搜索的快捷键
  window.dispatchEvent(new KeyboardEvent('keydown', { key: 'k', ctrlKey: true }))
}

const clearAllFavorites = () => {
  recentStore.favoriteItems = []
  recentStore.initFavorites()
  showFavorites.value = false
}
</script>

<style scoped>
.basic-layout {
  height: 100vh;
  overflow: hidden;
}

/* 主内容区域：整体不滚动 */
.basic-layout > .ant-layout {
  height: 100vh;
  overflow: hidden;
}

/* ── Mega Sidebar (140px) ──────────────────────────── */
.mega-sidebar {
  width: 140px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #001529;
  overflow: hidden;
  flex-shrink: 0;
}

.mega-sidebar-logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  flex-shrink: 0;
}

.mega-sidebar-logo img {
  width: 28px;
  height: 28px;
}

.mega-sidebar-items {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mega-sidebar-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 45px;
  padding: 0 8px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.65);
  transition: all 0.2s;
  user-select: none;
}

.mega-sidebar-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.mega-sidebar-item.active {
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
}

.mega-sidebar-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.mega-sidebar-label {
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 72px;
}

.mega-sidebar-active-bar {
  position: absolute;
  left: 0;
  top: 6px;
  bottom: 6px;
  width: 3px;
  background: #ff4d4f;
  border-radius: 0 2px 2px 0;
}

.layout-header {
  background: var(--color-bg-layout);
  padding: 0 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.trigger {
  font-size: 18px;
  cursor: pointer;
  transition: color 0.3s;
  flex-shrink: 0;
}

.trigger:hover {
  color: var(--color-primary);
}

.mobile-title {
  font-size: 16px;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.header-btn {
  display: flex;
  align-items: center;
  justify-content: center;
}

.tenant-switcher :deep(.ant-select) {
  font-size: 13px;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.user-info:hover {
  background: var(--color-bg-container-secondary, #f5f5f5);
}

.username {
  margin-left: 8px;
  font-size: 14px;
}

.layout-content {
  background: var(--color-bg-layout);
  border-radius: 0;
  transition: all var(--motion-duration-base) var(--motion-ease-in-out);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* 确保路由页面根元素填满内容区并作为 flex 容器 */
.layout-content > :deep(*) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.favorites-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.favorite-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
  font-size: 14px;
}

.favorite-item:hover {
  background: var(--color-primary-bg, #e6f7ff);
}

.favorite-star {
  color: #faad14;
  font-size: 14px;
}

.link-icon-indicator {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
  margin-left: 4px;
  vertical-align: middle;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .layout-header {
    padding: 0 12px;
  }
  .header-left { gap: 6px; }
}

@media (max-width: 576px) {
  .layout-header { padding: 0 8px; }
  .trigger { font-size: 20px; }
}
</style>
