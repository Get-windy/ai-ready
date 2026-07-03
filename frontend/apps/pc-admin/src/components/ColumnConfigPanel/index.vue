<template>
  <a-modal
    v-model:open="localShowPanel"
    title="列配置"
    :footer="null"
    :mask-closable="true"
    :closable="true"
    width="600px"
    centered
  >
    <div class="col-settings-panel-modal">
      <div class="col-panel-body">
        <div
          v-for="(setting, si) in settingsColumns"
          :key="setting.key"
          class="col-setting-row"
          :class="{ 'col-setting-ghost': !setting.visible }"
        >
          <!-- 可见性复选框 -->
          <a-checkbox
            v-model:checked="setting.visible"
            :disabled="isLockedColumn(setting.key)"
            @change="emit('change')"
          />
          <!-- 列标题（可拖拽排序） -->
          <span
            class="col-setting-title"
            draggable="true"
            @dragstart="onDragStart(si)"
            @dragover.prevent="onDragOver(si)"
            @drop="emitDrop(si)"
          >
            <span class="drag-handle">⠿</span>
            {{ setting.title || setting.key }}
          </span>
          <!-- 冻结选择 -->
          <a-select
            v-model:value="setting.fixed"
            class="col-freeze-select"
            @change="emit('change')"
            size="small"
          >
            <a-select-option value="">不冻结</a-select-option>
            <a-select-option value="left">冻结左侧</a-select-option>
            <a-select-option value="right">冻结右侧</a-select-option>
          </a-select>
          <!-- 宽度调整 -->
          <a-input-number
            v-model:value="setting.width"
            class="col-width-input"
            :min="40"
            :max="500"
            @change="emit('change')"
            size="small"
          />
        </div>
      </div>
      <div class="col-panel-footer">
        <a-button size="middle" @click="emit('reset')">恢复默认</a-button>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ColumnSetting } from '@/composables/useColumnConfig'

defineOptions({ name: 'ColumnConfigPanel' })

const props = defineProps<{
  open: boolean
  settingsColumns: ColumnSetting[]
  isLockedColumn: (key: string) => boolean
}>()

const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'change'): void
  (e: 'reset'): void
  (e: 'dragEnd', from: number, to: number): void
}>()

let dragIndex = -1
function onDragStart(index: number) { dragIndex = index }
function onDragOver(index: number) {
  if (dragIndex === -1 || dragIndex === index) return
  emit('dragEnd', { from: dragIndex, to: index })
  dragIndex = index
}
function emitDrop() { dragIndex = -1 }

const localShowPanel = computed({
  get: () => props.open,
  set: (v) => emit('update:open', v),
})
</script>

<style scoped>
.col-settings-panel-modal {
  max-height: 440px;
  display: flex;
  flex-direction: column;
}
.col-panel-body {
  flex: 1;
  overflow-y: auto;
}
.col-setting-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 4px;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}
.col-setting-row:hover {
  background: #fafafa;
}
.col-setting-ghost {
  opacity: 0.4;
}
.col-setting-title {
  flex: 1;
  font-size: 13px;
  cursor: grab;
  user-select: none;
  display: flex;
  align-items: center;
  gap: 4px;
}
.drag-handle {
  color: #bbb;
  font-size: 14px;
  letter-spacing: 2px;
}
.col-freeze-select {
  width: 100px;
}
.col-width-input {
  width: 80px;
}
.col-panel-footer {
  padding: 12px 4px 0;
  text-align: right;
  border-top: 1px solid #f0f0f0;
  margin-top: 8px;
}
</style>
