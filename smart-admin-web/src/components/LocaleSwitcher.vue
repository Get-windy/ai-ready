<template>
  <a-dropdown placement="bottomRight" :trigger="['click']">
    <div class="locale-switcher" :class="{ 'is-switching': isSwitching }" @click.prevent>
      <transition name="locale-fade" mode="out-in">
        <span :key="currentLocale" class="locale-content">
          <span class="locale-icon">{{ currentLocaleInfo?.icon }}</span>
          <span class="locale-name">{{ currentLocaleInfo?.name }}</span>
          <DownOutlined class="dropdown-icon" />
        </span>
      </transition>
    </div>
    
    <template #overlay>
      <a-menu
        v-model:selectedKeys="selectedKeys"
        @click="handleLocaleChange"
        class="locale-menu"
      >
        <a-menu-item
          v-for="locale in supportedLocales"
          :key="locale.code"
          class="locale-menu-item"
        >
          <span class="locale-option">
            <span class="locale-icon">{{ locale.icon }}</span>
            <span class="locale-name">{{ locale.name }}</span>
            <CheckOutlined 
              v-if="locale.code === currentLocale" 
              class="check-icon"
            />
          </span>
        </a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { CheckOutlined, DownOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import {
  getCurrentLocale,
  setI18nLanguage,
  getSupportedLocales,
  getLocaleInfo,
  type LocaleCode
} from '@/locales'

// 当前语言
const currentLocale = ref<LocaleCode>(getCurrentLocale())

// 是否正在切换
const isSwitching = ref(false)

// 选中的菜单项
const selectedKeys = ref([currentLocale.value])

// 支持的语言列表
const supportedLocales = getSupportedLocales()

// 当前语言信息
const currentLocaleInfo = computed(() => getLocaleInfo(currentLocale.value))

// 监听语言变化
watch(currentLocale, (newLocale) => {
  selectedKeys.value = [newLocale]
})

// 监听语言切换事件
function handleLocaleBeforeChange() {
  isSwitching.value = true
}

function handleLocaleChanged(event: CustomEvent) {
  const { locale } = event.detail
  currentLocale.value = locale
  isSwitching.value = false
  
  // 显示成功消息
  message.success(
    (getLocaleInfo(locale)?.name || locale)
  )
}

// 处理语言切换
async function handleLocaleChange({ key }: { key: string }) {
  const newLocale = key as LocaleCode
  
  if (newLocale === currentLocale.value) {
    return
  }
  
  try {
    isSwitching.value = true
    
    // 触发切换前事件
    window.dispatchEvent(new CustomEvent('locale:before-change', { detail: { locale: newLocale } }))
    
    await setI18nLanguage(newLocale)
    currentLocale.value = newLocale
    
    // 触发切换后事件
    window.dispatchEvent(new CustomEvent('locale:changed', { detail: { locale: newLocale } }))
    
    message.success(
      (getLocaleInfo(newLocale)?.name || newLocale)
    )
  } catch (error) {
    message.error('Language switch failed')
    console.error('Failed to change locale:', error)
  } finally {
    isSwitching.value = false
  }
}

onMounted(() => {
  window.addEventListener('locale:before-change', handleLocaleBeforeChange)
  window.addEventListener('locale:changed', handleLocaleChanged as EventListener)
})

onUnmounted(() => {
  window.removeEventListener('locale:before-change', handleLocaleBeforeChange)
  window.removeEventListener('locale:changed', handleLocaleChanged as EventListener)
})
</script>

<style scoped>
.locale-switcher {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 6px;
  transition: all 0.3s ease;
  background: transparent;
}

.locale-switcher:hover {
  background-color: rgba(24, 144, 255, 0.08);
}

.locale-switcher.is-switching {
  pointer-events: none;
  opacity: 0.7;
}

.locale-content {
  display: flex;
  align-items: center;
  gap: 6px;
}

.locale-icon {
  font-size: 18px;
}

.locale-name {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.85);
  font-weight: 500;
}

.dropdown-icon {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-left: 4px;
  transition: transform 0.3s ease;
}

.locale-switcher:hover .dropdown-icon {
  transform: translateY(2px);
}

/* 淡入淡出动画 */
.locale-fade-enter-active,
.locale-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.locale-fade-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}

.locale-fade-leave-to {
  opacity: 0;
  transform: translateX(10px);
}

/* 菜单样式 */
.locale-menu {
  min-width: 160px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.locale-menu-item {
  padding: 10px 16px !important;
  transition: all 0.2s ease;
}

.locale-menu-item:hover {
  background-color: rgba(24, 144, 255, 0.08);
}

.locale-menu-item.ant-menu-item-selected {
  background-color: rgba(24, 144, 255, 0.12);
}

.locale-option {
  display: flex;
  align-items: center;
  width: 100%;
}

.locale-option .locale-icon {
  font-size: 18px;
  margin-right: 10px;
}

.locale-option .locale-name {
  flex: 1;
}

.check-icon {
  color: #1890ff;
  font-size: 14px;
  margin-left: 8px;
}
</style>