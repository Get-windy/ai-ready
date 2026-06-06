<template>
  <a-layout class="basic-layout" aria-label="主导航布局">
    <a-layout-sider
      v-if="isDesktopView || isTabletView"
      :collapsed="siderCollapsed"
      @update:collapsed="handleSiderCollapsedChange"
      :trigger="null"
      collapsible
      theme="dark"
      :width="256"
      :collapsed-width="64"
    >
      <div class="logo">
        <img
          src="@/assets/logo.svg"
          alt="logo"
        >
        <span v-if="!collapsed">{{ t('app.name') }}</span>
      </div>

      <div v-if="!collapsed" class="sidebar-search" @click="openGlobalSearch">
        <SearchOutlined />
        <span class="sidebar-search-text">搜索菜单...</span>
      </div>

      <a-menu
        v-model:selected-keys="selectedKeys"
        v-model:open-keys="openKeys"
        mode="inline"
        theme="dark"
        role="navigation"
        aria-label="侧边栏导航菜单"
      >
        <template v-for="menu in userStore.menus" :key="menu.id">
          <a-menu-item
            v-if="menu.menuType === 1 && !menu.children?.length"
            :key="menu.menuCode"
            @click="navigateTo(menu.path || '/')"
          >
            <component :is="getIcon(menu.icon)" v-if="menu.icon" />
            <span>{{ menu.menuName }}</span>
          </a-menu-item>

          <a-sub-menu
            v-else-if="menu.menuType === 0"
            :key="menu.menuCode"
          >
            <template #icon>
              <component :is="getIcon(menu.icon)" v-if="menu.icon" />
            </template>
            <template #title>
              {{ menu.menuName }}
            </template>
            <template v-for="child in menu.children" :key="child.id">
              <a-menu-item
                v-if="child.menuType === 1"
                :key="child.menuCode"
                @click="navigateTo(child.path || '/')"
              >
                <component :is="getIcon(child.icon)" v-if="child.icon" />
                <span>{{ child.menuName }}</span>
              </a-menu-item>
            </template>
          </a-sub-menu>
        </template>
      </a-menu>
    </a-layout-sider>

    <a-drawer
      v-if="!isDesktopView && !isTabletView"
      v-model:open="mobileMenuVisible"
      placement="left"
      :closable="true"
      :width="256"
    >
      <a-menu
        v-model:selected-keys="selectedKeys"
        v-model:open-keys="openKeys"
        mode="inline"
        theme="dark"
        role="navigation"
        aria-label="移动端导航菜单"
      >
        <template v-for="menu in userStore.menus" :key="menu.id">
          <a-menu-item
            v-if="menu.menuType === 1 && !menu.children?.length"
            :key="menu.menuCode"
            @click="handleMobileMenuClick(menu.path || '/')"
          >
            <component :is="getIcon(menu.icon)" v-if="menu.icon" />
            <span>{{ menu.menuName }}</span>
          </a-menu-item>

          <a-sub-menu
            v-else-if="menu.menuType === 0"
            :key="menu.menuCode"
          >
            <template #icon>
              <component :is="getIcon(menu.icon)" v-if="menu.icon" />
            </template>
            <template #title>
              {{ menu.menuName }}
            </template>
            <template v-for="child in menu.children" :key="child.id">
              <a-menu-item
                v-if="child.menuType === 1"
                :key="child.menuCode"
                @click="handleMobileMenuClick(child.path || '/')"
              >
                <component :is="getIcon(child.icon)" v-if="child.icon" />
                <span>{{ child.menuName }}</span>
              </a-menu-item>
            </template>
          </a-sub-menu>
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
          <MenuUnfoldOutlined
            v-if="collapsed && (isDesktopView || isTabletView)"
            class="trigger"
            role="button"
            :aria-label="t('a11y.expandSidebar')"
            :aria-expanded="!collapsed"
            tabindex="0"
            @click="collapsed = !collapsed"
            @keydown.enter="collapsed = !collapsed"
            @keydown.space.prevent="collapsed = !collapsed"
          />
          <MenuFoldOutlined
            v-else-if="isDesktopView || isTabletView"
            class="trigger"
            role="button"
            :aria-label="t('a11y.collapseSidebar')"
            :aria-expanded="!collapsed"
            tabindex="0"
            @click="collapsed = !collapsed"
            @keydown.enter="collapsed = !collapsed"
            @keydown.space.prevent="collapsed = !collapsed"
          />
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
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <keep-alive :include="cachedRoutes">
              <component :is="Component" />
            </keep-alive>
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
  DashboardOutlined,
  ShopOutlined,
  TeamOutlined,
  SettingOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MenuOutlined,
  ShoppingOutlined,
  ShoppingCartOutlined,
  ContainerOutlined,
  FileTextOutlined,
  UserOutlined,
  SafetyOutlined,
  AccountBookOutlined,
  MoneyCollectOutlined,
  BarChartOutlined,
  AppstoreOutlined,
  BranchesOutlined,
  StarOutlined,
  StarFilled,
  UserAddOutlined,
  FileOutlined,
  InboxOutlined,
  SendOutlined,
  AuditOutlined,
  RestOutlined,
  DollarOutlined,
  CheckCircleOutlined,
  ApartmentOutlined,
  IdcardOutlined,
  UnorderedListOutlined,
  QuestionCircleOutlined,
  MonitorOutlined,
  LineChartOutlined,
  CheckSquareOutlined,
  RollbackOutlined,
  SwapOutlined,
  SearchOutlined,
  BellOutlined,
  LogoutOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
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

const collapsed = ref(false)
const selectedKeys = ref(['dashboard'])
const openKeys = ref(['erp'])
const mobileMenuVisible = ref(false)
const showFavorites = ref(false)

// 通知系统（基于WebSocket实时推送）
const notif = useNotification()

// 缓存的路由（keep-alive）
const cachedRoutes = computed(() => {
  return tabsStore.tabs.filter(t => t.routeName && t.routeName !== route.name?.toString()).map(t => t.routeName!) || []
})

const siderCollapsed = computed(() => isTabletView.value ? true : collapsed.value)
const handleSiderCollapsedChange = (value: boolean) => {
  if (!isTabletView.value) {
    collapsed.value = value
  }
}

// 租户切换（使用 store 中的用户可访问租户列表）
const tenantSwitching = ref(false)
const currentTenantName = computed(() => {
  const found = userStore.userTenants.find(t => t.id === userStore.tenantId)
  return found?.tenantName || userStore.tenantName || '默认租户'
})
const showTenantSwitcher = computed(() => userStore.userTenants.length > 1)

const iconMap: Record<string, any> = {
  'DashboardOutlined': DashboardOutlined,
  'ShopOutlined': ShopOutlined,
  'TeamOutlined': TeamOutlined,
  'SettingOutlined': SettingOutlined,
  'ShoppingOutlined': ShoppingOutlined,
  'ShoppingCartOutlined': ShoppingCartOutlined,
  'ContainerOutlined': ContainerOutlined,
  'FileTextOutlined': FileTextOutlined,
  'UserOutlined': UserOutlined,
  'SafetyOutlined': SafetyOutlined,
  'AccountBookOutlined': AccountBookOutlined,
  'MoneyCollectOutlined': MoneyCollectOutlined,
  'BarChartOutlined': BarChartOutlined,
  'AppstoreOutlined': AppstoreOutlined,
  'BranchesOutlined': BranchesOutlined,
  'StarOutlined': StarOutlined,
  'UserAddOutlined': UserAddOutlined,
  'FileOutlined': FileOutlined,
  'InboxOutlined': InboxOutlined,
  'SendOutlined': SendOutlined,
  'AuditOutlined': AuditOutlined,
  'RestOutlined': RestOutlined,
  'DollarOutlined': DollarOutlined,
  'CheckCircleOutlined': CheckCircleOutlined,
  'ApartmentOutlined': ApartmentOutlined,
  'IdcardOutlined': IdcardOutlined,
  'UnorderedListOutlined': UnorderedListOutlined,
  'QuestionCircleOutlined': QuestionCircleOutlined,
  'MonitorOutlined': MonitorOutlined,
  'LineChartOutlined': LineChartOutlined,
  'CheckSquareOutlined': CheckSquareOutlined,
  'RollbackOutlined': RollbackOutlined,
  'SwapOutlined': SwapOutlined
}

const getIcon = (iconName?: string) => {
  if (!iconName) return null
  return iconMap[iconName] || DashboardOutlined
}

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
  // 同步菜单展开/选中状态（防止菜单折叠）
  syncMenuKeys(path)
}, { immediate: true })

// 根据当前路由同步菜单选中项和展开项
function syncMenuKeys(path: string) {
  const findMenuKeys = (menus: any[], targetPath: string, parentCodes: string[]): { selected: string; open: string[] } | null => {
    for (const menu of menus) {
      if (menu.menuType === 0) {
        // 子菜单：在 children 中递归查找
        const found = findMenuKeys(menu.children || [], targetPath, [...parentCodes, menu.menuCode])
        if (found) return found
      } else if (menu.menuType === 1 && menu.path === targetPath) {
        return { selected: menu.menuCode, open: parentCodes }
      }
    }
    return null
  }

  const result = findMenuKeys(userStore.menus, path, [])
  if (result) {
    selectedKeys.value = [result.selected]
    openKeys.value = result.open
  }
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

const navigateTo = (path: string) => {
  showFavorites.value = false
  router.push(path)
}

const handleMobileMenuClick = (path: string) => {
  mobileMenuVisible.value = false
  router.push(path)
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
  min-height: 100vh;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  padding: 0 16px;
  gap: 8px;
}

.logo img {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

.sidebar-search {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px 16px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.65);
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.sidebar-search:hover {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
}

.sidebar-search-text {
  opacity: 0.65;
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
