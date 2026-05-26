<template>
  <aside 
    :class="sidebarClass"
    :style="sidebarStyle"
  >
    <!-- Logo区域 -->
    <div class="ar-sidebar__logo">
      <img src="/logo.svg" alt="AI-Ready" />
      <span v-if="!collapsed" class="ar-sidebar__logo-text">AI-Ready</span>
    </div>
    
    <!-- 菜单区域 -->
    <nav class="ar-sidebar__menu">
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        router
        @select="handleSelect"
      >
        <template v-for="item in menuItems" :key="item.path">
          <!-- 无子菜单 -->
          <el-menu-item 
            v-if="!item.children"
            :index="item.path"
          >
            <el-icon class="ar-sidebar__icon"><component :is="item.icon" /></el-icon>
            <template #title>
              <span class="ar-sidebar__title">{{ item.title }}</span>
            </template>
          </el-menu-item>
          
          <!-- 有子菜单 -->
          <el-sub-menu v-else :index="item.path">
            <template #title>
              <el-icon class="ar-sidebar__icon"><component :is="item.icon" /></el-icon>
              <span class="ar-sidebar__title">{{ item.title }}</span>
            </template>
            <el-menu-item 
              v-for="child in item.children"
              :key="child.path"
              :index="child.path"
            >
              <span class="ar-sidebar__title">{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </nav>
    
    <!-- 底部操作区 -->
    <div class="ar-sidebar__footer">
      <el-button 
        class="ar-sidebar__toggle"
        type="text"
        @click="toggleCollapse"
      >
        <el-icon class="ar-sidebar__toggle-icon">
          <Fold v-if="!collapsed" />
          <Expand v-else />
        </el-icon>
      </el-button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Fold, Expand } from '@element-plus/icons-vue'

export interface MenuItem {
  path: string
  title: string
  icon: string
  children?: MenuItem[]
}

const props = defineProps<{
  menuItems: MenuItem[]
}>()

const emit = defineEmits<{
  menuSelect: [path: string]
  toggleCollapse: [collapsed: boolean]
}>()

const route = useRoute()
const collapsed = ref(false)

const activeMenu = computed(() => route.path)

const sidebarClass = computed(() => ({
  'ar-sidebar': true,
  'ar-sidebar--collapsed': collapsed.value,
}))

const sidebarStyle = computed(() => ({
  width: collapsed.value ? '64px' : '240px',
}))

const toggleCollapse = () => {
  collapsed.value = !collapsed.value
  emit('toggleCollapse', collapsed.value)
}

const handleSelect = (index: string) => {
  emit('menuSelect', index)
}
</script>

<style scoped>
.ar-sidebar {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #001529;
  color: #fff;
  transition: width 0.3s;
  z-index: 100;
}

/* Logo区域 */
.ar-sidebar__logo {
  display: flex;
  align-items: center;
  padding: 16px;
  height: 64px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.ar-sidebar__logo img {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
}

.ar-sidebar__logo-text {
  margin-left: 12px;
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
}

/* 菜单区域 */
.ar-sidebar__menu {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.ar-sidebar__menu :deep(.el-menu) {
  background: transparent;
  border: none;
}

.ar-sidebar__menu :deep(.el-menu-item),
.ar-sidebar__menu :deep(.el-sub-menu__title) {
  color: rgba(255, 255, 255, 0.65);
  border-radius: 4px;
  margin: 4px 8px;
}

.ar-sidebar__menu :deep(.el-menu-item:hover),
.ar-sidebar__menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.ar-sidebar__menu :deep(.el-menu-item.is-active) {
  background: var(--ar-primary);
  color: #fff;
}

.ar-sidebar__icon {
  font-size: 18px;
}

.ar-sidebar__title {
  font-size: 14px;
}

/* 底部操作区 */
.ar-sidebar__footer {
  display: flex;
  justify-content: center;
  padding: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.ar-sidebar__toggle {
  color: rgba(255, 255, 255, 0.65);
}

.ar-sidebar__toggle:hover {
  color: #fff;
}

.ar-sidebar__toggle-icon {
  font-size: 20px;
}

/* 折叠状态 */
.ar-sidebar--collapsed .ar-sidebar__logo-text {
  display: none;
}

.ar-sidebar--collapsed .ar-sidebar__logo {
  justify-content: center;
}

.ar-sidebar--collapsed .ar-sidebar__logo img {
  margin: 0;
}

/* 滚动条样式 */
.ar-sidebar__menu::-webkit-scrollbar {
  width: 6px;
}

.ar-sidebar__menu::-webkit-scrollbar-track {
  background: transparent;
}

.ar-sidebar__menu::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.ar-sidebar__menu::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 响应式 */
@media (max-width: 768px) {
  .ar-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    z-index: 1000;
  }
  
  .ar-sidebar--collapsed {
    width: 64px !important;
  }
}
</style>