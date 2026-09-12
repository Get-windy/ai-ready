<template>
  <a-modal
    v-model:open="localShowPanel"
    title="数据表列配置"
    :footer="null"
    :mask-closable="true"
    :closable="true"
    width="750px"
    centered
  >
    <a-tabs v-model:active-key="activeTab" class="col-config-tabs">
      <!-- ═══ 个人配置 Tab ═══ -->
      <a-tab-pane key="personal" tab="个人配置">
        <div class="tab-tip">只对当前操作员有效，在全局配置内配置字段显示、排序</div>
        <table class="col-config-table">
          <thead>
            <tr>
              <th style="width: 40px;">序号</th>
              <th style="width: 40px;">上级</th>
              <th>列名</th>
              <th style="width: 120px;">显示名</th>
              <th style="width: 50px;">显示</th>
              <th style="width: 80px;">回车跳转</th>
              <th style="width: 100px;">公式内容</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(setting, si) in personalSettings"
              :key="setting.key"
              :class="{ 'row-ghost': !setting.visible }"
            >
              <td class="cell-center">{{ si + 1 }}</td>
              <td class="cell-center">
                <span class="drag-handle" draggable="true" @dragstart="onDragStart('personal', si)" @dragover.prevent="onDragOver('personal', si)" @drop="onDrop('personal')">⠿</span>
              </td>
              <td>{{ setting.title || setting.key }}</td>
              <td>
                <span class="display-name-readonly">{{ setting.displayName || setting.title || setting.key }}</span>
              </td>
              <td class="cell-center">
                <a-checkbox
                  v-model:checked="setting.visible"
                  :disabled="isLockedColumn(setting.key)"
                  @change="emit('change')"
                />
              </td>
              <td class="cell-center">
                <a-checkbox v-model:checked="setting.enterJump" @change="emit('change')" />
              </td>
              <td>
                <span class="formula-readonly">{{ setting.formula || '' }}</span>
              </td>
            </tr>
          </tbody>
        </table>
        <div class="col-panel-footer">
          <a-button size="middle" @click="emit('reset')">恢复默认</a-button>
        </div>
      </a-tab-pane>

      <!-- ═══ 全局配置 Tab ═══ -->
      <a-tab-pane key="global" tab="全局配置">
        <div class="tab-tip">系统级配置，无单据配置权限的操作员只能在已配置范围内进行操作</div>
        <table class="col-config-table">
          <thead>
            <tr>
              <th style="width: 40px;">序号</th>
              <th style="width: 40px;">上级</th>
              <th>列名</th>
              <th style="width: 120px;">显示名</th>
              <th style="width: 50px;">显示</th>
              <th style="width: 80px;">回车跳转</th>
              <th style="width: 100px;">公式内容</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(setting, si) in globalSettings"
              :key="setting.key"
              :class="{ 'row-ghost': !setting.visible }"
            >
              <td class="cell-center">{{ si + 1 }}</td>
              <td class="cell-center">
                <span class="drag-handle" draggable="true" @dragstart="onDragStart('global', si)" @dragover.prevent="onDragOver('global', si)" @drop="onDrop('global')">⠿</span>
              </td>
              <td>{{ setting.title || setting.key }}</td>
              <td>
                <a-input
                  v-model:value="setting.displayName"
                  size="small"
                  style="width: 100%"
                  @change="emit('change')"
                />
              </td>
              <td class="cell-center">
                <a-checkbox
                  v-model:checked="setting.visible"
                  :disabled="isLockedColumn(setting.key)"
                  @change="emit('change')"
                />
              </td>
              <td class="cell-center">
                <a-checkbox v-model:checked="setting.enterJump" @change="emit('change')" />
              </td>
              <td>
                <a-input
                  v-model:value="setting.formula"
                  size="small"
                  placeholder="公式"
                  style="width: 100%"
                  @change="emit('change')"
                />
              </td>
            </tr>
          </tbody>
        </table>
        <div class="col-panel-footer">
          <a-button size="middle" @click="emit('reset')">恢复默认</a-button>
        </div>
      </a-tab-pane>
    </a-tabs>
  </a-modal>
</template>


<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { ColumnSetting } from '@/composables/useColumnConfig'

defineOptions({ name: 'ColumnConfigPanel' })

// 扩展的列配置类型
export interface ExtendedColumnSetting extends ColumnSetting {
  displayName?: string
  enterJump?: boolean
  formula?: string
}

const props = defineProps<{
  open: boolean
  settingsColumns: ColumnSetting[]
  isLockedColumn: (key: string) => boolean
  /** 全局配置持久化键名（传入后全局配置将保存到后端 API） */
  globalConfigKey?: string
}>()

const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'change'): void
  (e: 'reset'): void
  (e: 'dragEnd', from: number, to: number): void
  /** 全局配置变更时触发（用于后端持久化） */
  (e: 'globalConfigChange', settings: ExtendedColumnSetting[]): void
}>()

const activeTab = ref('personal')

// 个人配置和全局配置的本地副本（带扩展字段）
const personalSettings = ref<ExtendedColumnSetting[]>([])
const globalSettings = ref<ExtendedColumnSetting[]>([])

// 同步外部 settingsColumns 到内部副本
watch(() => props.settingsColumns, (cols) => {
  const toExtended = (c: ColumnSetting): ExtendedColumnSetting => ({
    ...c,
    displayName: (c as any).displayName || c.title || c.key,
    enterJump: (c as any).enterJump || false,
    formula: (c as any).formula || '',
  })
  personalSettings.value = cols.map(toExtended)
  // 全局配置：先尝试从后端加载，降级到 localStorage
  const loadGlobal = (savedData?: ExtendedColumnSetting[]) => {
    if (savedData && savedData.length > 0) {
      globalSettings.value = savedData
    } else {
      const savedGlobal = localStorage.getItem('col-config-global-' + (cols[0]?.key || ''))
      if (savedGlobal) {
        try {
          globalSettings.value = JSON.parse(savedGlobal)
        } catch {
          globalSettings.value = cols.map(toExtended)
        }
      } else {
        globalSettings.value = cols.map(toExtended)
      }
    }
  }
  if (props.globalConfigKey) {
    // 尝试从后端加载全局配置
    import('@/api/erp').then(({ userPageConfigApi }) => {
      userPageConfigApi.get('col-config', props.globalConfigKey!).then((raw: any) => {
        if (raw && typeof raw === 'string') {
          try {
            const parsed = JSON.parse(raw)
            if (Array.isArray(parsed) && parsed.length > 0) {
              loadGlobal(parsed)
              return
            }
          } catch { /* ignore */ }
        }
        loadGlobal()
      }).catch(() => loadGlobal())
    }).catch(() => loadGlobal())
  } else {
    loadGlobal()
  }
}, { immediate: true, deep: true })

// 个人配置变更时同步到外部
watch(personalSettings, (val) => {
  // 将扩展字段回写到原始 settingsColumns
  val.forEach((s) => {
    const original = props.settingsColumns.find(c => c.key === s.key)
    if (original) {
      original.visible = s.visible
      ;(original as any).displayName = s.displayName
      ;(original as any).enterJump = s.enterJump
      ;(original as any).formula = s.formula
    }
  })
  emit('change')
}, { deep: true })

// 全局配置变更时持久化并同步到外部（显示名、回车跳转、公式等）
watch(globalSettings, (val) => {
  const key = 'col-config-global-' + (val[0]?.key || '')
  localStorage.setItem(key, JSON.stringify(val))
  // 如果提供了 globalConfigKey，同步到后端
  if (props.globalConfigKey && val.length > 0) {
    emit('globalConfigChange', [...val])
  }
  // 写回 settingsColumns，让 useColumnConfig 持久化扩展字段
  val.forEach((s) => {
    const original = props.settingsColumns.find(c => c.key === s.key)
    if (original) {
      ;(original as any).displayName = s.displayName
      ;(original as any).enterJump = s.enterJump
      ;(original as any).formula = s.formula
    }
  })
  emit('change')
}, { deep: true })

// 拖拽排序
let dragIndex = -1
let dragSource: 'personal' | 'global' = 'personal'
function onDragStart(source: 'personal' | 'global', index: number) {
  dragIndex = index
  dragSource = source
}
function onDragOver(source: 'personal' | 'global', index: number) {
  if (dragIndex === -1 || dragIndex === index || source !== dragSource) return
  const list = source === 'personal' ? personalSettings.value : globalSettings.value
  const from = dragIndex
  const to = index
  const item = list.splice(from, 1)[0]
  list.splice(to, 0, item)
  dragIndex = to
}
function onDrop(_source: 'personal' | 'global') {
  dragIndex = -1
  emit('change')
}

const localShowPanel = computed({
  get: () => props.open,
  set: (v) => emit('update:open', v),
})
</script>

<style scoped>
.col-config-tabs :deep(.ant-tabs-content) {
  max-height: 480px;
  overflow-y: auto;
}
.tab-tip {
  padding: 8px 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  margin-bottom: 12px;
  font-size: 12px;
  color: #888;
}
.col-config-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.col-config-table thead th {
  background: #fafafa;
  padding: 8px 6px;
  border-bottom: 1px solid #e8e8e8;
  text-align: left;
  font-weight: 500;
  font-size: 12px;
  color: #666;
}
.col-config-table tbody td {
  padding: 6px;
  border-bottom: 1px solid #f5f5f5;
  vertical-align: middle;
}
.col-config-table tbody tr:hover {
  background: #fafafa;
}
.row-ghost {
  opacity: 0.4;
}
.cell-center {
  text-align: center;
}
.drag-handle {
  color: #bbb;
  font-size: 14px;
  letter-spacing: 2px;
  cursor: grab;
  user-select: none;
}
.display-name-readonly {
  display: block;
  padding: 2px 4px;
  background: #f5f5f5;
  border-radius: 2px;
  font-size: 12px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.formula-readonly {
  display: block;
  font-size: 11px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 90px;
}
.col-panel-footer {
  padding: 12px 4px 0;
  text-align: right;
  border-top: 1px solid #f0f0f0;
  margin-top: 8px;
}
</style>
