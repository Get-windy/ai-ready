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
          <a-badge :count="unreadCount" :dot="unreadCount > 0" size="small">
            <a-tooltip title="通知">
              <a-button
                type="text"
                size="small"
                class="header-btn"
                @click="navigateTo('/notification')"
              >
                <template #icon><BellOutlined /></template>
              </a-button>
            </a-tooltip>
          </a-badge>

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
                v-for="t in tenantList"
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
const unreadCount = ref(0)

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

// 租户切换
const tenantList = ref<TenantInfo[]>([])
const tenantSwitching = ref(false)
const currentTenantName = computed(() => {
  const found = tenantList.value.find(t => t.id === userStore.tenantId)
  return found?.tenantName || '默认租户'
})
const showTenantSwitcher = computed(() => tenantList.value.length > 1)

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
  fetchUnreadCount()
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
})

// 加载租户列表
const fetchTenants = async () => {
  try {
    const res = await userApi.getTenants()
    if (res.data) tenantList.value = res.data
  } catch (error) {
    console.error('获取租户列表失败:', error)
  }
}

// 获取未读通知数
const fetchUnreadCount = async () => {
  try {
    // TODO: 接入真实通知API
    unreadCount.value = 0
  } catch { /* ignore */ }
}

// 切换租户
const handleTenantSwitch = async (tenantId: number) => {
  const target = tenantList.value.find(t => t.id === tenantId)
  if (!target) return
  tenantSwitching.value = true
  try {
    await userApi.getTenants()
    userStore.tenantId = tenantId
    localStorage.setItem('tenantId', String(tenantId))
    message.success('已切换到: ' + target.tenantName)
    window.location.reload()
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
  await userStore.logout()
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
  overflow-y: auto;
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
