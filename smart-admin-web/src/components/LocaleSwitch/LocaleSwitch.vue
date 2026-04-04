<template>
  <a-dropdown :trigger="['click']">
    <a-button type="text" class="lang-switch-btn">
      <GlobalOutlined />
      <span class="lang-text">{{ currentLangLabel }}</span>
    </a-button>
    <template #overlay>
      <a-menu @click="handleLangChange">
        <a-menu-item v-for="lang in locales" :key="lang.value">
          <span class="lang-flag">{{ lang.flag }}</span>
          <span>{{ lang.label }}</span>
          <CheckOutlined v-if="currentLocale === lang.value" class="check-icon" />
        </a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { GlobalOutlined, CheckOutlined } from '@ant-design/icons-vue'
import { setLocale, getLocale, locales } from '@/locales'
import { message } from 'ant-design-vue'

const currentLocale = computed(() => getLocale())

const currentLangLabel = computed(() => {
  const lang = locales.find(l => l.value === currentLocale.value)
  return lang?.label || 'Language'
})

const handleLangChange = ({ key }: { key: string }) => {
  if (key !== currentLocale.value) {
    setLocale(key)
    message.success(`Language switched to ${locales.find(l => l.value === key)?.label}`)
    
    // 刷新页面以应用新语言
    window.location.reload()
  }
}
</script>

<style scoped>
.lang-switch-btn {
  display: flex;
  align-items: center;
  gap: 8px;
}

.lang-text {
  margin-left: 4px;
}

.lang-flag {
  margin-right: 8px;
}

.check-icon {
  margin-left: auto;
  color: #1890ff;
}
</style>