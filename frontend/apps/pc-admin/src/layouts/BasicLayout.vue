<template>
  <a-layout class="basic-layout" aria-label="主导航布局">
    <a-layout-sider
      v-if="isDesktopView || isTabletView"
      v-model:collapsed="isTabletView ? true : collapsed"
      :trigger="null"
      collapsible
      theme="dark"
      :width="256"
      :collapsed-width="64"
    >
      <div class="logo">
        <img
          v-if="!collapsed"
          src="@/assets/logo.svg"
          alt="logo"
        >
        <span v-if="!collapsed">{{ t('app.name') }}</span>
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
          <a-breadcrumb v-if="isDesktopView || isTabletView">
            <a-breadcrumb-item>{{ t('menu.dashboard') }}</a-breadcrumb-item>
            <a-breadcrumb-item>{{ currentTitle }}</a-breadcrumb-item>
          </a-breadcrumb>
          <span
            v-else
            class="mobile-title"
          >{{ currentTitle }}</span>
        </div>

        <div class="header-right">
          <LocaleSwitcher v-if="isDesktopView" />
          
          <a-dropdown>
            <div class="user-info" role="button" :aria-label="t('a11y.userMenu')" tabindex="0">
              <a-avatar
                :size="isMobileView ? 28 : 32"
                :src="userStore.userInfo?.avatar"
              >
                {{ userStore.nickname?.charAt(0) }}
              </a-avatar>
              <span
                v-if="isDesktopView"
                class="username"
              >{{ userStore.nickname }}</span>
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item
                  key="profile"
                  @click="navigateTo('/profile')"
                >
                  {{ t('user.profile') }}
                </a-menu-item>
                <a-menu-item key="settings">
                  {{ t('menu.system') }}
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item
                  key="logout"
                  @click="handleLogout"
                >
                  {{ t('menu.logout') }}
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

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
          <transition
            name="fade"
            mode="out-in"
          >
            <component :is="Component" />
          </transition>
        </router-view>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
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
  RollbackOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import LocaleSwitcher from '@/components/LocaleSwitcher.vue'
import { useResponsive } from '@/composables/useResponsiveState'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const userStore = useUserStore()
const { isMobileView, isTabletView, isDesktopView } = useResponsive()

const collapsed = ref(false)
const selectedKeys = ref(['dashboard'])
const openKeys = ref(['erp'])
const mobileMenuVisible = ref(false)

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
  'RollbackOutlined': RollbackOutlined
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
  return '64px'
})

const contentPadding = computed(() => {
  if (isMobileView.value) return '12px'
  if (isTabletView.value) return '16px'
  return '24px'
})

const contentMinHeight = computed(() => {
  const header = parseInt(headerHeight.value.replace('px', ''))
  const padding = parseInt(contentPadding.value.replace('px', ''))
  return `calc(100vh - ${header + padding * 2}px)`
})

const navigateTo = (path: string) => {
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
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  padding: 0 16px;
}

.logo img {
  width: 32px;
  height: 32px;
  margin-right: 8px;
}

.layout-header {
  background: var(--color-bg-layout);
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: var(--shadow-base);
  position: sticky;
  top: 0;
  z-index: var(--z-index-sticky);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.trigger {
  font-size: 18px;
  cursor: pointer;
  transition: color 0.3s;
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
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.username {
  margin-left: 8px;
}

.layout-content {
  background: var(--color-bg-layout);
  border-radius: var(--border-radius-base);
  transition: all var(--motion-duration-base) var(--motion-ease-in-out);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .layout-header {
    padding: 0 16px;
  }
  
  .header-left {
    gap: 8px;
  }
}

@media (max-width: 576px) {
  .layout-header {
    padding: 0 12px;
  }
  
  .trigger {
    font-size: 20px;
  }
}
</style>