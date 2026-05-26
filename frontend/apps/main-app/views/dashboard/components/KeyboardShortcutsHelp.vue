<template>
  <el-dialog
    v-model="visible"
    title="键盘快捷键"
    width="600px"
    :close-on-click-modal="true"
    :destroy-on-close="false"
    aria-labelledby="shortcuts-title"
  >
    <div class="shortcuts-help" role="dialog" aria-modal="true">
      <p class="help-description">
        使用键盘快捷键可以更高效地操作系统监控大盘。
      </p>

      <div class="shortcuts-section">
        <h3 id="shortcuts-title">全局快捷键</h3>
        <div class="shortcuts-list" role="list">
          <div 
            v-for="shortcut in globalShortcuts" 
            :key="shortcut.key"
            class="shortcut-item"
            role="listitem"
          >
            <div class="shortcut-keys">
              <kbd v-if="shortcut.ctrl" class="key">Ctrl</kbd>
              <kbd v-if="shortcut.alt" class="key">Alt</kbd>
              <kbd v-if="shortcut.shift" class="key">Shift</kbd>
              <kbd class="key">{{ formatKey(shortcut.key) }}</kbd>
            </div>
            <span class="shortcut-description">{{ shortcut.description }}</span>
          </div>
        </div>
      </div>

      <div class="shortcuts-section">
        <h3>导航快捷键</h3>
        <div class="shortcuts-list" role="list">
          <div 
            v-for="shortcut in navigationShortcuts" 
            :key="shortcut.key"
            class="shortcut-item"
            role="listitem"
          >
            <div class="shortcut-keys">
              <kbd v-if="shortcut.ctrl" class="key">Ctrl</kbd>
              <kbd v-if="shortcut.alt" class="key">Alt</kbd>
              <kbd v-if="shortcut.shift" class="key">Shift</kbd>
              <kbd class="key">{{ formatKey(shortcut.key) }}</kbd>
            </div>
            <span class="shortcut-description">{{ shortcut.description }}</span>
          </div>
        </div>
      </div>

      <div class="shortcuts-section">
        <h3>面板操作</h3>
        <div class="shortcuts-list" role="list">
          <div 
            v-for="shortcut in panelShortcuts" 
            :key="shortcut.key"
            class="shortcut-item"
            role="listitem"
          >
            <div class="shortcut-keys">
              <kbd v-if="shortcut.ctrl" class="key">Ctrl</kbd>
              <kbd v-if="shortcut.alt" class="key">Alt</kbd>
              <kbd v-if="shortcut.shift" class="key">Shift</kbd>
              <kbd class="key">{{ formatKey(shortcut.key) }}</kbd>
            </div>
            <span class="shortcut-description">{{ shortcut.description }}</span>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button type="primary" @click="visible = false">
        知道了
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

interface Shortcut {
  key: string
  ctrl?: boolean
  alt?: boolean
  shift?: boolean
  description: string
}

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const visible = ref(props.modelValue)

watch(() => props.modelValue, (val) => {
  visible.value = val
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const globalShortcuts: Shortcut[] = [
  { key: 'r', description: '刷新数据' },
  { key: 'k', ctrl: true, description: '打开搜索 (Ctrl+K)' },
  { key: 'Escape', description: '关闭弹窗/取消操作' },
  { key: '?', shift: true, description: '显示快捷键帮助 (Shift+?)' }
]

const navigationShortcuts: Shortcut[] = [
  { key: 'Tab', description: '在可聚焦元素间切换' },
  { key: 'Tab', shift: true, description: '反向切换焦点' },
  { key: 'ArrowUp', description: '向上导航' },
  { key: 'ArrowDown', description: '向下导航' },
  { key: 'ArrowLeft', description: '向左导航' },
  { key: 'ArrowRight', description: '向右导航' }
]

const panelShortcuts: Shortcut[] = [
  { key: 'e', description: '进入/退出编辑模式' },
  { key: 'Delete', description: '删除选中面板' },
  { key: 'Enter', description: '确认/展开面板' },
  { key: 's', ctrl: true, description: '保存配置 (Ctrl+S)' }
]

const formatKey = (key: string): string => {
  const keyMap: Record<string, string> = {
    'Escape': 'Esc',
    'ArrowUp': '↑',
    'ArrowDown': '↓',
    'ArrowLeft': '←',
    'ArrowRight': '→',
    'Delete': 'Del',
    'Enter': '↵'
  }
  return keyMap[key] || key.toUpperCase()
}
</script>

<style scoped>
.shortcuts-help {
  max-height: 60vh;
  overflow-y: auto;
}

.help-description {
  margin: 0 0 20px 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.shortcuts-section {
  margin-bottom: 24px;
}

.shortcuts-section h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  border-bottom: 1px solid var(--el-border-color-light);
  padding-bottom: 8px;
}

.shortcuts-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.shortcut-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}

.shortcut-keys {
  display: flex;
  gap: 4px;
  align-items: center;
}

.key {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  padding: 0 8px;
  background: var(--el-fill-color);
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 12px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  box-shadow: 0 1px 0 var(--el-border-color-darker);
}

.shortcut-description {
  font-size: 14px;
  color: var(--el-text-color-regular);
  text-align: right;
  flex: 1;
  margin-left: 16px;
}

/* High contrast mode */
:global(.high-contrast) .key {
  border-width: 2px;
  border-color: currentColor;
}

/* Reduced motion */
:global(.reduce-motion) .shortcut-item {
  transition: none;
}

/* Dark theme adjustments */
:global([data-theme="dark"]) .key {
  background: var(--el-fill-color-dark);
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.1);
}
</style>
