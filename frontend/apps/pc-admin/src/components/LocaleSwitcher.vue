<template>
  <a-dropdown
    placement="bottomRight"
    :trigger="['click']"
  >
    <GlobalOutlined
      class="locale-switcher-icon"
      :class="{ 'is-switching': isSwitching }"
      role="button"
      aria-label="切换语言"
      tabindex="0"
    />

    <template #overlay>
      <a-menu
        v-model:selected-keys="selectedKeys"
        class="locale-menu"
        @click="handleLocaleChange"
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
import { CheckOutlined, GlobalOutlined } from '@ant-design/icons-vue'
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
    (getLocaleInfo(locale)?.labelNative || locale)
  )
}

// 处理语言切换
async function handleLocaleChange({ key }: Record<string, any>) {
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
      (getLocaleInfo(newLocale)?.labelNative || newLocale)
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
.locale-switcher-icon {
  font-size: 18px;
  color: rgba(0, 0, 0, 0.65);
  cursor: pointer;
  transition: color 0.2s;
}

.locale-switcher-icon:hover {
  color: var(--color-primary);
}

.locale-switcher-icon.is-switching {
  pointer-events: none;
  opacity: 0.7;
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