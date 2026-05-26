<template>
  <div class="theme-toggle" role="group" aria-label="主题切换">
    <el-tooltip content="浅色主题" placement="top">
      <el-button
        :type="currentTheme === 'light' ? 'primary' : 'default'"
        :icon="Sunny"
        circle
        size="small"
        @click="setTheme('light')"
        aria-label="切换到浅色主题"
        :aria-pressed="currentTheme === 'light'"
      />
    </el-tooltip>
    
    <el-tooltip content="深色主题" placement="top">
      <el-button
        :type="currentTheme === 'dark' ? 'primary' : 'default'"
        :icon="Moon"
        circle
        size="small"
        @click="setTheme('dark')"
        aria-label="切换到深色主题"
        :aria-pressed="currentTheme === 'dark'"
      />
    </el-tooltip>
    
    <el-tooltip content="跟随系统" placement="top">
      <el-button
        :type="currentTheme === 'auto' ? 'primary' : 'default'"
        :icon="Monitor"
        circle
        size="small"
        @click="setTheme('auto')"
        aria-label="跟随系统主题"
        :aria-pressed="currentTheme === 'auto'"
      />
    </el-tooltip>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Sunny, Moon, Monitor } from '@element-plus/icons-vue'
import { useTheme } from '../composables/useTheme'

const { theme, setTheme, isDark } = useTheme()

const currentTheme = computed(() => {
  if (theme.value === 'auto') return 'auto'
  return isDark.value ? 'dark' : 'light'
})
</script>

<style scoped>
.theme-toggle {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* High contrast mode */
:global(.high-contrast) .theme-toggle .el-button {
  border-width: 2px;
}

/* Reduced motion */
:global(.reduce-motion) .theme-toggle .el-button {
  transition: none;
}
</style>
