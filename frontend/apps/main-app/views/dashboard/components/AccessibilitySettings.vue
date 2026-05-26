<template>
  <el-dialog
    v-model="visible"
    title="无障碍设置"
    width="500px"
    :close-on-click-modal="true"
    :destroy-on-close="false"
    aria-labelledby="a11y-title"
  >
    <div class="accessibility-settings" role="dialog" aria-modal="true">
      <p class="settings-description" id="a11y-title">
        自定义您的无障碍体验，让系统更适合您的使用习惯。
      </p>

      <el-form :model="localConfig" label-position="top" class="settings-form">
        <!-- High Contrast -->
        <el-form-item>
          <div class="setting-item">
            <div class="setting-header">
              <div class="setting-icon">
                <el-icon :size="24"><View /></el-icon>
              </div>
              <div class="setting-info">
                <span class="setting-label">高对比度模式</span>
                <span class="setting-desc">增强文字与背景的对比度</span>
              </div>
            </div>
            <el-switch
              v-model="localConfig.highContrast"
              @change="updateConfig('highContrast', $event)"
              aria-label="启用高对比度模式"
            />
          </div>
        </el-form-item>

        <!-- Reduced Motion -->
        <el-form-item>
          <div class="setting-item">
            <div class="setting-header">
              <div class="setting-icon">
                <el-icon :size="24"><VideoPause /></el-icon>
              </div>
              <div class="setting-info">
                <span class="setting-label">减少动画</span>
                <span class="setting-desc">减弱或禁用界面动画效果</span>
              </div>
            </div>
            <el-switch
              v-model="localConfig.reduceMotion"
              @change="updateConfig('reduceMotion', $event)"
              aria-label="减少动画效果"
            />
          </div>
        </el-form-item>

        <!-- Large Text -->
        <el-form-item>
          <div class="setting-item">
            <div class="setting-header">
              <div class="setting-icon">
                <el-icon :size="24"><ZoomIn /></el-icon>
              </div>
              <div class="setting-info">
                <span class="setting-label">大字体模式</span>
                <span class="setting-desc">增大界面文字尺寸</span>
              </div>
            </div>
            <el-switch
              v-model="localConfig.largeText"
              @change="updateConfig('largeText', $event)"
              aria-label="启用大字体模式"
            />
          </div>
        </el-form-item>

        <!-- Screen Reader Announcements -->
        <el-form-item>
          <div class="setting-item">
            <div class="setting-header">
              <div class="setting-icon">
                <el-icon :size="24"><Microphone /></el-icon>
              </div>
              <div class="setting-info">
                <span class="setting-label">屏幕阅读器通知</span>
                <span class="setting-desc">启用动态内容朗读</span>
              </div>
            </div>
            <el-switch
              v-model="localConfig.screenReaderAnnouncements"
              @change="updateConfig('screenReaderAnnouncements', $event)"
              aria-label="启用屏幕阅读器通知"
            />
          </div>
        </el-form-item>
      </el-form>

      <!-- Accessibility Score -->
      <div class="a11y-score" role="region" aria-label="无障碍评分">
        <div class="score-header">
          <span class="score-title">当前无障碍评分</span>
          <span class="score-value" :class="scoreClass">{{ accessibilityScore }}分</span>
        </div>
        <el-progress
          :percentage="accessibilityScore"
          :status="scoreStatus"
          :stroke-width="10"
          aria-label="无障碍评分进度"
        />
        <p class="score-hint">
          {{ scoreHint }}
        </p>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" @click="saveAndClose">
        保存设置
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { View, VideoPause, ZoomIn, Microphone } from '@element-plus/icons-vue'
import { useAccessibility } from '../composables/useAccessibility'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const visible = ref(props.modelValue)
const { config, updateConfig: updateA11yConfig } = useAccessibility()

// Local copy for editing
const localConfig = ref({ ...config.value })

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    localConfig.value = { ...config.value }
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

// Update individual config item
const updateConfig = (key: keyof typeof localConfig.value, value: boolean) => {
  localConfig.value[key] = value
  updateA11yConfig({ [key]: value })
}

// Calculate accessibility score
const accessibilityScore = computed(() => {
  let score = 80 // Base score
  
  // Bonus for enabling accessibility features
  if (localConfig.value.highContrast) score += 5
  if (localConfig.value.reduceMotion) score += 5
  if (localConfig.value.largeText) score += 5
  if (localConfig.value.screenReaderAnnouncements) score += 5
  
  return Math.min(score, 100)
})

const scoreClass = computed(() => {
  if (accessibilityScore.value >= 90) return 'excellent'
  if (accessibilityScore.value >= 80) return 'good'
  if (accessibilityScore.value >= 60) return 'fair'
  return 'poor'
})

const scoreStatus = computed(() => {
  if (accessibilityScore.value >= 90) return 'success'
  if (accessibilityScore.value >= 80) return ''
  if (accessibilityScore.value >= 60) return 'warning'
  return 'exception'
})

const scoreHint = computed(() => {
  if (accessibilityScore.value >= 90) {
    return '优秀！您的无障碍设置已非常完善。'
  } else if (accessibilityScore.value >= 80) {
    return '良好。建议开启更多无障碍功能以提升体验。'
  } else if (accessibilityScore.value >= 60) {
    return '一般。请考虑启用无障碍功能。'
  }
  return '需要改进。建议启用无障碍功能。'
})

const saveAndClose = () => {
  updateA11yConfig(localConfig.value)
  visible.value = false
}
</script>

<style scoped>
.accessibility-settings {
  max-height: 70vh;
  overflow-y: auto;
}

.settings-description {
  margin: 0 0 20px 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.settings-form {
  margin-bottom: 24px;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-light);
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.setting-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  color: var(--el-color-primary);
}

.setting-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.setting-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.setting-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.a11y-score {
  padding: 16px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  border: 1px solid var(--el-border-color-light);
}

.score-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.score-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.score-value {
  font-size: 18px;
  font-weight: 600;
}

.score-value.excellent {
  color: var(--el-color-success);
}

.score-value.good {
  color: var(--el-color-primary);
}

.score-value.fair {
  color: var(--el-color-warning);
}

.score-value.poor {
  color: var(--el-color-danger);
}

.score-hint {
  margin: 12px 0 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  text-align: center;
}

/* High contrast mode */
:global(.high-contrast) .setting-item {
  border-width: 2px;
}

:global(.high-contrast) .a11y-score {
  border-width: 2px;
}

/* Reduced motion */
:global(.reduce-motion) .setting-icon,
:global(.reduce-motion) .el-switch {
  transition: none;
}

/* Large text mode */
:global(.large-text) .setting-label {
  font-size: 16px;
}

:global(.large-text) .setting-desc {
  font-size: 14px;
}

:global(.large-text) .score-title,
:global(.large-text) .score-value {
  font-size: 20px;
}