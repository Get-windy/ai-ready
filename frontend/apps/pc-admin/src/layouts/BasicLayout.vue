<template>
  <a-layout class="basic-layout" aria-label="主导航布局" :has-sider="true">
    <!-- 桌面端 Mega Sidebar (140px / 60px) -->
    <div
      v-if="isDesktopView || isTabletView"
      class="mega-sidebar"
      :class="{ 'mega-sidebar-collapsed': sidebarCollapsed }"
      @mouseleave="hoverState.handleTargetLeave()"
    >
      <div class="mega-sidebar-logo">
        <img src="@/assets/logo.svg" alt="logo" />
        <span v-show="!sidebarCollapsed" class="mega-sidebar-logo-text">企智连</span>
      </div>

      <div ref="sidebarItemsRef" class="mega-sidebar-items">
        <div
          v-for="menu in userStore.menus"
          :key="menu.id"
          class="mega-sidebar-item"
          :title="sidebarCollapsed ? menu.menuName : undefined"
          @mouseenter="handleSidebarHover(menu, $event.currentTarget as HTMLElement)"
          @click="handleSidebarClick(menu, $event.currentTarget as HTMLElement)"
        >
          <component :is="getIcon(menu.icon)" v-if="menu.icon" class="mega-sidebar-icon" />
          <span v-show="!sidebarCollapsed" class="mega-sidebar-label">{{ menu.menuName }}</span>
          <div v-if="!isDashboard(menu) && !sidebarCollapsed" class="mega-sidebar-hover-bar" />
        </div>
        <!-- Spacer: 动态增高以创造滚动空间 -->
        <div ref="sidebarSpacerRef" class="mega-sidebar-spacer" />
      </div>

      <!-- MegaMenuPanel 弹出面板（折叠时不显示） -->
      <Teleport to="body">
        <MegaMenuPanel
          v-if="!sidebarCollapsed && hoverState.isOpen.value && hoverState.hoveredItem.value"
          :menu-items="hoverState.hoveredItem.value.children || []"
          :trigger-pos="hoverState.triggerPos.value"
          @panel-enter="hoverState.handlePanelEnter()"
          @panel-leave="hoverState.handlePanelLeave()"
          @navigate="hoverState.close()"
          @measure="handlePanelMeasure"
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
          <component
            :is="sidebarCollapsed ? MenuUnfoldOutlined : MenuFoldOutlined"
            v-if="isDesktopView || isTabletView"
            class="trigger sidebar-trigger"
            role="button"
            :aria-label="sidebarCollapsed ? '展开侧边栏' : '折叠侧边栏'"
            tabindex="0"
            @click="sidebarCollapsed = !sidebarCollapsed"
            @keydown.enter="sidebarCollapsed = !sidebarCollapsed"
            @keydown.space.prevent="sidebarCollapsed = !sidebarCollapsed"
          />

          <!-- 常用菜单快捷下拉 -->
          <a-dropdown placement="bottomLeft" :trigger="['click']">
            <a-button type="text" size="small" class="quick-menu-btn">
              常用菜单
              <DownOutlined style="margin-left: 2px; font-size: 10px;" />
            </a-button>
            <template #overlay>
              <a-menu class="quick-menu-dropdown">
                <a-menu-item
                  v-for="item in quickMenuItems"
                  :key="item.path"
                  @click="navigateTo(item.path)"
                >
                  <component :is="getIcon(item.icon)" v-if="item.icon" style="margin-right: 6px;" />
                  {{ item.menuName }}
                </a-menu-item>
                <a-menu-item v-if="quickMenuItems.length === 0" disabled>
                  <span style="color: #999;">暂无常用菜单</span>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
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

          <!-- 帮助中心 -->
          <a-dropdown placement="bottomRight" :trigger="['click']">
            <QuestionCircleOutlined class="help-center-icon" role="button" aria-label="帮助中心" tabindex="0" />
            <template #overlay>
              <a-menu class="help-center-dropdown">
                <a-menu-item key="complaint">
                  <FileTextOutlined style="color: #ff8c00;" /> 投诉建议
                </a-menu-item>
                <a-menu-item key="online">
                  <CustomerServiceOutlined style="color: #ff8c00;" /> 在线咨询
                </a-menu-item>
                <a-menu-item key="print">
                  <PrinterOutlined style="color: #ff8c00;" /> 打印助手
                </a-menu-item>
                <a-menu-item key="desktop">
                  <DesktopOutlined style="color: #ff8c00;" /> 电脑端
                </a-menu-item>
                <a-menu-item key="mobile">
                  <MobileOutlined style="color: #ff8c00;" /> 移动端
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="phone" disabled class="help-phone-item">
                  <PhoneOutlined style="color: #ff4d4f;" /> 19115973320
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>

          <a-dropdown>
            <div class="user-info" role="button" :aria-label="t('a11y.userMenu')" tabindex="0">
              <span class="username">欢迎您，{{ userStore.nickname }}</span>
              <DownOutlined style="font-size: 10px; margin-left: 4px;" />
            </div>
            <template #overlay>
              <a-menu class="user-dropdown-menu">
                <a-menu-item key="profile" @click="navigateTo('/profile')">
                  <SettingOutlined style="color: #ff8c00;" /> 个人设置
                </a-menu-item>
                <a-menu-item key="fontsize">
                  <AppstoreOutlined style="color: #ff8c00;" /> 字体大小
                </a-menu-item>
                <a-menu-item key="refresh" @click="handleRefresh">
                  <ReloadOutlined style="color: #ff8c00;" /> 刷新
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="tempPwd">
                  <LockOutlined style="color: #ff4d4f;" /> 获取临时密码
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" @click="handleLogout">
                  <PoweroffOutlined style="color: #ff4d4f;" /> 退出
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
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  DownOutlined,
  QuestionCircleOutlined,
  StarOutlined,
  StarFilled,
  BellOutlined,
  SettingOutlined,
  AppstoreOutlined,
  ReloadOutlined,
  LockOutlined,
  PoweroffOutlined,
  FileTextOutlined,
  CustomerServiceOutlined,
  PrinterOutlined,
  DesktopOutlined,
  MobileOutlined,
  PhoneOutlined
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
const sidebarCollapsed = ref(false)

// Mega Menu hover 延迟控制
const hoverState = useHoverDelay(150, 300)

// 侧边栏菜单列表引用（用于自动滚动）
const sidebarItemsRef = ref<HTMLElement | null>(null)
const sidebarSpacerRef = ref<HTMLElement | null>(null)
// 保存的原始滚动位置（面板关闭时恢复）
let savedSidebarScrollTop: number | null = null
// 面板高度缓存（menuId → 实际像素高度，避免估算过高导致不必要的滚动）
const panelHeightCache = new Map<number, number>()

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
  const normalizedPath = currentPath.startsWith('/') ? currentPath : '/' + currentPath
  const findMenuName = (menus: any[], path: string): string => {
    for (const menu of menus) {
      const menuPath = menu.path ? (menu.path.startsWith('/') ? menu.path : '/' + menu.path) : ''
      if (menuPath === path) return menu.menuName
      if (menu.children) {
        const found = findMenuName(menu.children, path)
        if (found) return found
      }
    }
    return ''
  }
  // 优先从菜单树匹配，其次用路由 meta.title，最后回退到工作台
  return findMenuName(userStore.menus, normalizedPath) || (route.meta?.title as string) || t('menu.dashboard')
})

// 常用菜单（一级菜单 + 第一个叶子路径）
const quickMenuItems = computed(() => {
  return userStore.menus.map(menu => ({
    ...menu,
    path: menu.path || getFirstLeafPath(menu) || ''
  }))
})

const headerHeight = computed(() => {
  if (isMobileView.value) return '48px'
  if (isTabletView.value) return '45px'
  return '45px'
})

const contentPadding = computed(() => {
  if (isMobileView.value) return '8px'
  if (isTabletView.value) return '8px'
  return '0px'
})

const contentMinHeight = computed(() => {
  const header = parseInt(headerHeight.value.replace('px', ''))
  const padding = parseInt(contentPadding.value.replace('px', ''))
  const tabsHeight = 30
  return `calc(100vh - ${header + tabsHeight + padding * 2}px)`
})

// 初始化标签页（必须在 watcher 之前，否则 immediate: true 的 watcher 打开的 tab 会被 initTabs 重置掉）
tabsStore.initTabs()

// 初始化收藏和租户列表
onMounted(() => {
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
  // 双入口表单路径：等页面挂载后自动触发新增（延迟确保组件已挂载并注册事件监听）
  if (route.meta?.autoCreate) {
    setTimeout(() => {
      window.dispatchEvent(new Event('erp:create'))
    }, 200)
  }
}, { immediate: true })

// 面板关闭时恢复侧边栏滚动位置并收起 spacer
watch(
  () => hoverState.isOpen.value,
  (isOpen) => {
    if (!isOpen && savedSidebarScrollTop !== null && sidebarItemsRef.value && sidebarSpacerRef.value) {
      const container = sidebarItemsRef.value
      const spacer = sidebarSpacerRef.value
      // 收起 spacer
      spacer.style.height = '0px'
      // 恢复滚动位置
      container.scrollTo({ top: savedSidebarScrollTop, behavior: 'smooth' })
      savedSidebarScrollTop = null
    }
  }
)

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
  const normalizePath = (p: string) => p.startsWith('/') ? p : '/' + p
  const checkActive = (items: MenuInfo[]): boolean => {
    for (const item of items) {
      if (item.path && route.path.startsWith(normalizePath(item.path))) return true
      if (item.children?.length && checkActive(item.children)) return true
    }
    return false
  }
  if (menu.path && route.path.startsWith(normalizePath(menu.path))) return true
  if (menu.children?.length && checkActive(menu.children)) return true
  return false
}

/** 判断菜单是否为工作台/首页（叶子节点，无子菜单） */
function isDashboard(menu: MenuInfo): boolean {
  // 通过菜单名称或路径判断
  return menu.menuName === '工作台' || menu.menuName === '首页' || menu.path === '/dashboard'
}

/** 接收 MegaMenuPanel 实际高度并缓存 */
function handlePanelMeasure(height: number) {
  const menu = hoverState.hoveredItem.value
  if (menu?.id) {
    panelHeightCache.set(menu.id, height)
  }
}

/** 估算面板高度：优先用缓存，否则按最多子项列数计算 */
function estimatePanelHeight(menu: MenuInfo): number {
  if (menu.id && panelHeightCache.has(menu.id)) {
    return panelHeightCache.get(menu.id)!
  }
  const columns = (menu.children || []).filter(c => c.children?.length)
  let maxItems = 0
  for (const col of columns) {
    maxItems = Math.max(maxItems, col.children?.length ?? 0)
  }
  // 每子项约 28px + 列头 40px + 面板 padding 24px
  return Math.max(120, 40 + 24 + maxItems * 28)
}

/** 处理侧边栏悬停事件（只有非工作台的父级菜单才显示面板） */
function handleSidebarHover(menu: MenuInfo, el: HTMLElement) {
  // 工作台是叶子节点，不显示面板，但要关闭已展开的面板
  if (isDashboard(menu)) {
    hoverState.close()
    return
  }
  // 只有有子菜单的才显示面板
  if (menu.children?.length) {
    const rect = el.getBoundingClientRect()
    const viewportBottom = window.innerHeight
    const panelEstHeight = estimatePanelHeight(menu)
    const availableSpace = viewportBottom - rect.top - 16

    // 面板超出视口底部 → 通过 spacer 创造滚动空间
    if (availableSpace < panelEstHeight && sidebarItemsRef.value && sidebarSpacerRef.value) {
      const container = sidebarItemsRef.value
      const spacer = sidebarSpacerRef.value

      // 首次悬停时保存原始滚动位置（面板关闭时恢复）
      if (savedSidebarScrollTop === null) {
        savedSidebarScrollTop = container.scrollTop
      }

      // 直接测量所有子项的实际高度（不依赖 scrollHeight）
      let contentHeight = 0
      for (const child of Array.from(container.children)) {
        if (child !== spacer) {
          contentHeight += child.getBoundingClientRect().height
        }
      }
      // 加上 gap 高度（子项数量 - 1）× 2px
      const itemCount = container.children.length - 1 // 减去 spacer
      contentHeight += Math.max(0, itemCount - 1) * 2
      // 加上 padding
      const style = getComputedStyle(container)
      contentHeight += parseFloat(style.paddingTop) + parseFloat(style.paddingBottom)

      const neededScroll = panelEstHeight - availableSpace
      const spacerHeight = Math.max(0, container.clientHeight + neededScroll - contentHeight + 20)

      spacer.style.transition = 'none'
      spacer.style.height = spacerHeight + 'px'

      // 等两帧让布局生效
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          container.scrollTo({ top: neededScroll, behavior: 'instant' })
          // 滚动后捕获元素的正确位置，延迟打开面板
          hoverState.handleTargetEnter(menu, el, 200)
        })
      })
    } else {
      hoverState.handleTargetEnter(menu, el)
    }
  }
}

/** 处理侧边栏点击事件（叶子节点和"工作台"直接导航，其他有子菜单则触发悬停面板） */
function handleSidebarClick(menu: MenuInfo, el: HTMLElement) {
  if (isDashboard(menu)) {
    // 工作台/首页始终直接导航
    router.push('/dashboard')
    return
  }
  if (!menu.children?.length && menu.path) {
    // 叶子节点直接导航
    const path = menu.path.startsWith('/') ? menu.path : '/' + menu.path
    router.push(path)
  } else if (menu.children?.length) {
    // 有子菜单的一级菜单，点击触发悬停面板展开
    hoverState.handleTargetEnter(menu, el)
  }
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

const handleRefresh = () => {
  router.go(0)
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

/* ── Mega Sidebar (140px / 60px) ─────────────────────────── */
.mega-sidebar {
  width: 140px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #001529;
  overflow: hidden;
  flex-shrink: 0;
  transition: width 0.25s ease;
}

/* 折叠状态：60px 只保留图标 */
.mega-sidebar.mega-sidebar-collapsed {
  width: 60px;
}

.mega-sidebar-logo {
  height: 55px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  flex-shrink: 0;
}

.mega-sidebar-collapsed .mega-sidebar-logo {
  padding: 0;
  justify-content: center;
}

.mega-sidebar-logo img {
  width: 28px;
  height: 28px;
}

.mega-sidebar-logo-text {
  font-size: 16px;
  font-weight: 700;
  color: #1890ff;
  margin-left: 6px;
  white-space: nowrap;
  letter-spacing: 1px;
}

.mega-sidebar-items {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 4px 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

/* Spacer: 动态增高创造滚动空间，让底部菜单项可以滚到更高位置 */
.mega-sidebar-spacer {
  flex-shrink: 0;
  flex-grow: 0;
  height: 0;
  transition: height 0.15s ease;
}

/* 滚动条样式 — 细窄半透明，避免挤占菜单空间 */
.mega-sidebar-items::-webkit-scrollbar {
  width: 4px;
}

.mega-sidebar-items::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 2px;
}

.mega-sidebar-items::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

.mega-sidebar-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  height: 45px;
  padding: 0 12px 0 28px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.65);
  transition: all 0.2s;
  user-select: none;
  flex-shrink: 0;
  flex-grow: 0;
}

/* 折叠状态下菜单项只居中显示图标 */
.mega-sidebar-collapsed .mega-sidebar-item {
  padding: 0;
  justify-content: center;
}

.mega-sidebar-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.mega-sidebar-item:hover .mega-sidebar-active-bar {
  opacity: 0.5;
}

.mega-sidebar-item.active {
  color: #fff;
  /* 只用红色竖条指示激活状态，不用背景高亮，避免与 hover 混淆 */
}

.mega-sidebar-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.mega-sidebar-label {
  font-size: 15px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 80px;
}

.mega-sidebar-hover-bar {
  position: absolute;
  left: 0;
  top: 6px;
  bottom: 6px;
  width: 3px;
  background: #ff4d4f;
  border-radius: 0 2px 2px 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.mega-sidebar-item:hover .mega-sidebar-hover-bar {
  opacity: 1;
}

.layout-header {
  background: var(--color-bg-layout);
  padding: 0 10px;
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
  gap: 6px;
  flex: 1;
  min-width: 0;
}

.quick-menu-btn {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.85);
  padding: 2px 8px;
}

.quick-menu-btn:hover {
  color: var(--color-primary);
}

.quick-menu-dropdown :deep(.anticon) {
  font-size: 14px;
}

.trigger {
  font-size: 14px;
  cursor: pointer;
  transition: color 0.3s;
  flex-shrink: 0;
}

.trigger:hover {
  color: var(--color-primary);
}

.sidebar-trigger {
  padding: 4px;
  border-radius: 4px;
}

.sidebar-trigger:hover {
  background: rgba(0, 0, 0, 0.06);
}

.sidebar-trigger:focus {
  outline: none;
  box-shadow: none;
}

.mobile-title {
  font-size: 13px;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 6px;
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
  padding: 2px 6px;
  border-radius: 6px;
  transition: background 0.2s;
}

.user-info:hover {
  background: var(--color-bg-container-secondary, #f5f5f5);
}

.username {
  font-size: 13px;
  color: #ff4d4f;
  font-weight: 500;
}

.user-dropdown-menu :deep(.anticon) {
  font-size: 15px;
  margin-right: 8px;
}

.user-dropdown-menu :deep(.ant-menu-item) {
  padding: 8px 16px;
}

.help-center-icon {
  font-size: 18px;
  color: rgba(0, 0, 0, 0.65);
  cursor: pointer;
  transition: color 0.2s;
}

.help-center-icon:hover {
  color: var(--color-primary);
}

.help-center-dropdown :deep(.anticon) {
  font-size: 16px;
  margin-right: 8px;
}

.help-center-dropdown :deep(.ant-menu-item) {
  padding: 8px 16px;
}

.help-phone-item {
  font-weight: 600;
  font-size: 14px;
  color: #333;
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
