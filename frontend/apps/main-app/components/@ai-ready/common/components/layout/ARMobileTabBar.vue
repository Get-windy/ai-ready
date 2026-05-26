<template>
  <nav class="ar-mobile-tab-bar">
    <div 
      v-for="item in tabs" 
      :key="item.path"
      :class="tabClass(item.path)"
      @click="handleClick(item)"
    >
      <el-icon class="ar-mobile-tab-bar__icon">
        <component :is="item.icon" />
      </el-icon>
      <span class="ar-mobile-tab-bar__label">{{ item.label }}</span>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

export interface TabItem {
  path: string
  label: string
  icon: string
}

const props = defineProps<{
  tabs: TabItem[]
}>()

const route = useRoute()
const router = useRouter()

const activePath = computed(() => route.path)

const tabClass = (path: string) => ({
  'ar-mobile-tab-bar__item': true,
  'ar-mobile-tab-bar__item--active': activePath.value === path,
})

const handleClick = (item: TabItem) => {
  if (activePath.value !== item.path) {
    router.push(item.path)
  }
}
</script>

<style scoped>
.ar-mobile-tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-around;
  align-items: center;
  height: 56px;
  background: #fff;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
  z-index: 1000;
}

.ar-mobile-tab-bar__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 16px;
  color: #909399;
  cursor: pointer;
  transition: all 0.3s;
  flex: 1;
}

.ar-mobile-tab-bar__item:active {
  background: rgba(64, 158, 255, 0.1);
}

.ar-mobile-tab-bar__item--active {
  color: var(--ar-primary);
}

.ar-mobile-tab-bar__icon {
  font-size: 20px;
}

.ar-mobile-tab-bar__label {
  font-size: 12px;
  line-height: 1;
}

/* 安全区域适配（iOS刘海屏） */
@supports (padding-bottom: env(safe-area-inset-bottom)) {
  .ar-mobile-tab-bar {
    padding-bottom: env(safe-area-inset-bottom);
    height: calc(56px + env(safe-area-inset-bottom));
  }
}

/* 深色模式 */
[data-theme='dark'] .ar-mobile-tab-bar {
  background: #1a1a1a;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.3);
}

[data-theme='dark'] .ar-mobile-tab-bar__item {
  color: rgba(255, 255, 255, 0.65);
}

[data-theme='dark'] .ar-mobile-tab-bar__item:active {
  background: rgba(64, 158, 255, 0.2);
}

[data-theme='dark'] .ar-mobile-tab-bar__item--active {
  color: var(--ar-primary);
}
</style>