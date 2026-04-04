<template>
  <a-layout class="basic-layout">
    <a-layout-sider
      v-model:collapsed="collapsed"
      :trigger="null"
      collapsible
      theme="dark"
    >
      <div class="logo">
        <img src="@/assets/logo.svg" alt="logo" v-if="!collapsed" />
        <span v-if="!collapsed">{{ t('app.name') }}</span>
      </div>
      
      <a-menu
        v-model:selectedKeys="selectedKeys"
        v-model:openKeys="openKeys"
        mode="inline"
        theme="dark"
      >
        <a-menu-item key="dashboard" @click="navigateTo('/dashboard')">
          <DashboardOutlined />
          <span>{{ t('menu.dashboard') }}</span>
        </a-menu-item>

        <a-sub-menu key="erp">
          <template #icon>
            <ShopOutlined />
          </template>
          <template #title>{{ t('menu.erp') }}</template>
          <a-menu-item key="purchase" @click="navigateTo('/erp/purchase')">
            {{ t('menu.purchase') }}
          </a-menu-item>
          <a-menu-item key="sale" @click="navigateTo('/erp/sale')">
            {{ t('menu.sale') }}
          </a-menu-item>
          <a-menu-item key="stock" @click="navigateTo('/erp/stock')">
            {{ t('menu.stock') }}
          </a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="crm">
          <template #icon>
            <TeamOutlined />
          </template>
          <template #title>{{ t('menu.crm') }}</template>
          <a-menu-item key="lead" @click="navigateTo('/crm/lead')">
            {{ t('menu.lead') }}
          </a-menu-item>
          <a-menu-item key="customer" @click="navigateTo('/crm/customer')">
            {{ t('menu.customer') }}
          </a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="system">
          <template #icon>
            <SettingOutlined />
          </template>
          <template #title>{{ t('menu.system') }}</template>
          <a-menu-item key="user" @click="navigateTo('/system/user')">
            {{ t('menu.user') }}
          </a-menu-item>
          <a-menu-item key="role" @click="navigateTo('/system/role')">
            {{ t('menu.role') }}
          </a-menu-item>
        </a-sub-menu>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="layout-header">
        <div class="header-left">
          <MenuUnfoldOutlined
            v-if="collapsed"
            class="trigger"
            @click="collapsed = !collapsed"
          />
          <MenuFoldOutlined
            v-else
            class="trigger"
            @click="collapsed = !collapsed"
          />
          <a-breadcrumb>
            <a-breadcrumb-item>{{ t('menu.dashboard') }}</a-breadcrumb-item>
            <a-breadcrumb-item>{{ currentTitle }}</a-breadcrumb-item>
          </a-breadcrumb>
        </div>

        <div class="header-right">
          <!-- 语言切换 -->
          <LocaleSwitcher />
          
          <!-- 用户下拉菜单 -->
          <a-dropdown>
            <div class="user-info">
              <a-avatar :src="userStore.userInfo?.avatar">
                {{ userStore.nickname?.charAt(0) }}
              </a-avatar>
              <span class="username">{{ userStore.nickname }}</span>
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item key="profile" @click="navigateTo('/profile')">
                  {{ t('user.profile') }}
                </a-menu-item>
                <a-menu-item key="settings">
                  {{ t('menu.system') }}
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" @click="handleLogout">
                  {{ t('login.logoutSuccess') }}
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <a-layout-content class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
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
  MenuUnfoldOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import LocaleSwitcher from '@/components/LocaleSwitcher.vue'

const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()

const collapsed = ref(false)
const selectedKeys = ref(['dashboard'])
const openKeys = ref(['erp'])

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

const navigateTo = (path: string) => {
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
}

.header-left {
  display: flex;
  align-items: center;
}

.trigger {
  font-size: 18px;
  cursor: pointer;
  transition: color 0.3s;
  margin-right: 16px;
}

.trigger:hover {
  color: #1890ff;
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
  margin: 24px;
  padding: 24px;
  background: #fff;
  border-radius: 4px;
  min-height: calc(100vh - 64px - 48px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>