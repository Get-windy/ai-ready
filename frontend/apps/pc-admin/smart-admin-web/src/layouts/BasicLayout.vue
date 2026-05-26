<template>
  <a-layout class="basic-layout">
    <!-- 侧边栏 - PC端 -->
    <a-layout-sider
      v-if="isDesktopView"
      v-model:collapsed="collapsed"
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
      >
        <a-menu-item
          key="dashboard"
          @click="navigateTo('/dashboard')"
        >
          <DashboardOutlined />
          <span>{{ t('menu.dashboard') }}</span>
        </a-menu-item>

        <a-sub-menu key="erp">
          <template #icon>
            <ShopOutlined />
          </template>
          <template #title>
            {{ t('menu.erp') }}
          </template>
          <a-menu-item
            key="purchase"
            @click="navigateTo('/erp/purchase')"
          >
            {{ t('menu.purchase') }}
          </a-menu-item>
          <a-menu-item
            key="sale"
            @click="navigateTo('/erp/sale')"
          >
            {{ t('menu.sale') }}
          </a-menu-item>
          <a-menu-item
            key="stock"
            @click="navigateTo('/erp/stock')"
          >
            {{ t('menu.stock') }}
          </a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="crm">
          <template #icon>
            <TeamOutlined />
          </template>
          <template #title>
            {{ t('menu.crm') }}
          </template>
          <a-menu-item
            key="lead"
            @click="navigateTo('/crm/lead')"
          >
            {{ t('menu.lead') }}
          </a-menu-item>
          <a-menu-item
            key="customer"
            @click="navigateTo('/crm/customer')"
          >
            {{ t('menu.customer') }}
          </a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="system">
          <template #icon>
            <SettingOutlined />
          </template>
          <template #title>
            {{ t('menu.system') }}
          </template>
          <a-menu-item
            key="user"
            @click="navigateTo('/system/user')"
          >
            {{ t('menu.user') }}
          </a-menu-item>
          <a-menu-item
            key="role"
            @click="navigateTo('/system/role')"
          >
            {{ t('menu.role') }}
          </a-menu-item>
        </a-sub-menu>
      </a-menu>
    </a-layout-sider>

    <!-- 移动端抽屉菜单 -->
    <a-drawer
      v-if="!isDesktopView"
      v-model:open="mobileMenuVisible"
      placement="left"
      :closable="false"
      :width="256"
    >
      <a-menu
        v-model:selected-keys="selectedKeys"
        v-model:open-keys="openKeys"
        mode="inline"
        theme="dark"
      >
        <a-menu-item
          key="dashboard"
          @click="handleMobileMenuClick('/dashboard')"
        >
          <DashboardOutlined />
          <span>{{ t('menu.dashboard') }}</span>
        </a-menu-item>

        <a-sub-menu key="erp">
          <template #icon>
            <ShopOutlined />
          </template>
          <template #title>
            {{ t('menu.erp') }}
          </template>
          <a-menu-item
            key="purchase"
            @click="handleMobileMenuClick('/erp/purchase')"
          >
            {{ t('menu.purchase') }}
          </a-menu-item>
          <a-menu-item
            key="sale"
            @click="handleMobileMenuClick('/erp/sale')"
          >
            {{ t('menu.sale') }}
          </a-menu-item>
          <a-menu-item
            key="stock"
            @click="handleMobileMenuClick('/erp/stock')"
          >
            {{ t('menu.stock') }}
          </a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="crm">
          <template #icon>
            <TeamOutlined />
          </template>
          <template #title>
            {{ t('menu.crm') }}
          </template>
          <a-menu-item
            key="lead"
            @click="handleMobileMenuClick('/crm/lead')"
          >
            {{ t('menu.lead') }}
          </a-menu-item>
          <a-menu-item
            key="customer"
            @click="handleMobileMenuClick('/crm/customer')"
          >
            {{ t('menu.customer') }}
          </a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="system">
          <template #icon>
            <SettingOutlined />
          </template>
          <template #title>
            {{ t('menu.system') }}
          </template>
          <a-menu-item
            key="user"
            @click="handleMobileMenuClick('/system/user')"
          >
            {{ t('menu.user') }}
          </a-menu-item>
          <a-menu-item
            key="role"
            @click="handleMobileMenuClick('/system/role')"
          >
            {{ t('menu.role') }}
          </a-menu-item>
        </a-sub-menu>
      </a-menu>
    </a-drawer>

    <a-layout>
      <a-layout-header
        class="layout-header"
        :style="{ height: headerHeight }"
      >
        <div class="header-left">
          <MenuUnfoldOutlined
            v-if="collapsed && isDesktopView"
            class="trigger"
            @click="collapsed = !collapsed"
          />
          <MenuFoldOutlined
            v-else-if="isDesktopView"
            class="trigger"
            @click="collapsed = !collapsed"
          />
          <MenuOutlined
            v-if="!isDesktopView"
            class="trigger"
            @click="mobileMenuVisible = true"
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
          <!-- 语言切换 - 隐藏在移动端 -->
          <LocaleSwitcher v-if="isDesktopView" />
          
          <!-- 用户下拉菜单 -->
          <a-dropdown>
            <div class="user-info">
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
                  {{ t('login.logoutSuccess') }}
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <a-layout-content 
        class="layout-content"
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
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  DashboardOutlined,
  ShopOutlined,
  TeamOutlined,
  SettingOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MenuOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import LocaleSwitcher from '@/components/LocaleSwitcher.vue'
import { useResponsive } from '@/composables/useResponsiveState'

const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()
const { isMobileView, isTabletView, isDesktopView } = useResponsive()

const collapsed = ref(false)
const selectedKeys = ref(['dashboard'])
const openKeys = ref(['erp'])
const mobileMenuVisible = ref(false)

const currentTitle = computed(() => {
  const menuMap: Record<string, string> = {
    dashboard: t('menu.dashboard'),
    purchase: t('menu.purchase'),
    sale: t('menu.sale'),
    stock: t('menu.stock'),
    lead: t('menu.lead'),
    customer: t('menu.customer'),
    user: t('menu.user'),
    role: t('menu.role')
  }
  return menuMap[selectedKeys.value[0]] || ''
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
  background: #fff;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 10;
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
  color: #1890ff;
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
  background: #fff;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式样式 */
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