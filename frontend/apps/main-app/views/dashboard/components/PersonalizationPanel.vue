<template>
  <el-dialog
    v-model="visible"
    title="个性化配置"
    width="700px"
    :close-on-click-modal="true"
    :destroy-on-close="false"
    aria-labelledby="personalization-title"
  >
    <div class="personalization-panel" role="dialog" aria-modal="true">
      <el-tabs v-model="activeTab" type="border-card">
        <!-- Panel Layout Tab -->
        <el-tab-pane label="面板布局" name="layout">
          <div class="tab-content">
            <h3 id="personalization-title">自定义面板布局</h3>
            <p class="tab-description">
              拖拽调整面板顺序，点击设置图标自定义每个面板的配置。
            </p>

            <div class="layout-grid" role="list" aria-label="面板列表">
              <div
                v-for="panel in config.layouts"
                :key="panel.id"
                class="layout-item"
                role="listitem"
                :class="{ 'is-visible': panel.visible !== false }"
              >
                <div class="layout-item-header">
                  <el-icon class="drag-handle"><Rank /></el-icon>
                  <span class="panel-name">{{ panel.title }}</span>
                  <el-switch
                    v-model="panel.visible"
                    :active-value="true"
                    :inactive-value="false"
                    size="small"
                    @change="updatePanelVisibility(panel.id, $event)"
                    aria-label="显示/隐藏面板"
                  />
                </div>
                <div class="layout-item-config">
                  <el-select
                    v-model="panel.position.w"
                    size="small"
                    style="width: 100px"
                    @change="updatePanelWidth(panel.id, $event)"
                    aria-label="面板宽度"
                  >
                    <el-option label="窄 (3列)" :value="3" />
                    <el-option label="中 (4列)" :value="4" />
                    <el-option label="宽 (6列)" :value="6" />
                    <el-option label="全宽 (12列)" :value="12" />
                  </el-select>
                  <el-select
                    v-model="panel.position.h"
                    size="small"
                    style="width: 100px"
                    @change="updatePanelHeight(panel.id, $event)"
                    aria-label="面板高度"
                  >
                    <el-option label="矮 (2行)" :value="2" />
                    <el-option label="中 (3行)" :value="3" />
                    <el-option label="高 (4行)" :value="4" />
                    <el-option label="超高 (6行)" :value="6" />
                  </el-select>
                </div>
              </div>
            </div>

            <div class="grid-settings">
              <h4>网格设置</h4>
              <div class="settings-row">
                <el-form-item label="列数">
                  <el-slider
                    v-model="config.gridColumns"
                    :min="6"
                    :max="24"
                    :step="2"
                    show-stops
                  />
                </el-form-item>
                <el-form-item label="间距">
                  <el-slider
                    v-model="config.gridGap"
                    :min="8"
                    :max="32"
                    :step="4"
                    show-stops
                  />
                </el-form-item>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- Custom KPIs Tab -->
        <el-tab-pane label="自定义KPI" name="kpis">
          <div class="tab-content">
            <h3>自定义KPI指标</h3>
            <p class="tab-description">
              添加您关心的业务指标到仪表盘中。
            </p>

            <div class="kpi-list" role="list" aria-label="KPI列表">
              <div
                v-for="kpi in config.customKpis"
                :key="kpi.id"
                class="kpi-item"
                role="listitem"
              >
                <div class="kpi-info">
                  <span class="kpi-label">{{ kpi.label }}</span>
                  <el-tag size="small" type="info">{{ kpi.metric }}</el-tag>
                </div>
                <div class="kpi-actions">
                  <el-button
                    type="danger"
                    :icon="Delete"
                    circle
                    size="small"
                    @click="removeKpi(kpi.id)"
                    aria-label="删除KPI"
                  />
                </div>
              </div>
            </div>

            <el-divider />

            <h4>添加新KPI</h4>
            <el-form :model="newKpi" label-position="top" class="add-kpi-form">
              <el-form-item label="显示名称">
                <el-input
                  v-model="newKpi.label"
                  placeholder="例如：活跃用户"
                  aria-label="KPI显示名称"
                />
              </el-form-item>
              <el-form-item label="指标">
                <el-select
                  v-model="newKpi.metric"
                  placeholder="选择指标"
                  style="width: 100%"
                  aria-label="选择指标"
                >
                  <el-option
                    v-for="metric in availableMetrics"
                    :key="metric.id"
                    :label="metric.label"
                    :value="metric.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="格式">
                <el-select
                  v-model="newKpi.format"
                  placeholder="选择格式"
                  style="width: 100%"
                  aria-label="选择格式"
                >
                  <el-option label="数字" value="number" />
                  <el-option label="百分比" value="percentage" />
                  <el-option label="时长" value="duration" />
                  <el-option label="字节" value="bytes" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="addKpi" :disabled="!canAddKpi">
                  添加KPI
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- Saved Configs Tab -->
        <el-tab-pane label="保存的配置" name="presets">
          <div class="tab-content">
            <h3>保存的配置方案</h3>
            <p class="tab-description">
              保存当前配置或加载已保存的方案。
            </p>

            <div class="preset-list" role="list" aria-label="配置方案列表">
              <div
                v-for="preset in savedConfigs"
                :key="preset.id"
                class="preset-item"
                :class="{ 'is-default': preset.isDefault }"
                role="listitem"
              >
                <div class="preset-info">
                  <span class="preset-name">{{ preset.name }}</span>
                  <span v-if="preset.isDefault" class="preset-badge">默认</span>
                  <p class="preset-desc">{{ preset.description || '无描述' }}</p>
                  <span class="preset-date">
                    更新于 {{ formatDate(preset.updatedAt) }}
                  </span>
                </div>
                <div class="preset-actions">
                  <el-button
                    type="primary"
                    size="small"
                    @click="loadPreset(preset.id)"
                    aria-label="加载配置"
                  >
                    加载
                  </el-button>
                  <el-button
                    type="danger"
                    :icon="Delete"
                    circle
                    size="small"
                    @click="deletePreset(preset.id)"
                    aria-label="删除配置"
                  />
                </div>
              </div>
            </div>

            <el-divider />

            <h4>保存当前配置</h4>
            <el-form :model="newPreset" label-position="top" class="save-preset-form">
              <el-form-item label="配置名称">
                <el-input
                  v-model="newPreset.name"
                  placeholder="例如：我的监控面板"
                  aria-label="配置名称"
                />
              </el-form-item>
              <el-form-item label="描述（可选）">
                <el-input
                  v-model="newPreset.description"
                  type="textarea"
                  :rows="2"
                  placeholder="描述这个配置..."
                  aria-label="配置描述"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="savePreset" :disabled="!canSavePreset">
                  保存配置
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- Import/Export Tab -->
        <el-tab-pane label="导入/导出" name="import-export">
          <div class="tab-content">
            <h3>导入/导出配置</h3>
            <p class="tab-description">
              导出配置以备份或分享，或导入他人分享的配置。
            </p>

            <div class="export-section">
              <h4>导出配置</h4>
              <el-button type="primary" @click="exportConfig">
                导出为JSON
              </el-button>
              <el-button @click="copyShareLink">
                复制分享链接
              </el-button>
            </div>

            <el-divider />

            <div class="import-section">
              <h4>导入配置</h4>
              <el-input
                v-model="importData"
                type="textarea"
                :rows="6"
                placeholder="粘贴配置JSON..."
                aria-label="导入配置"
              />
              <el-button
                type="primary"
                @click="importConfig"
                :disabled="!importData"
                style="margin-top: 12px"
              >
                导入配置
              </el-button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" @click="saveAndClose">
        保存所有更改
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Rank, Delete } from '@element-plus/icons-vue'
import { usePersonalization, availableMetrics } from '../composables/usePersonalization'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const visible = ref(props.modelValue)
const activeTab = ref('layout')

const {
  config,
  savedConfigs,
  updateConfig,
  updatePanelLayout,
  addCustomKpi,
  removeCustomKpi,
  saveAsPreset,
  loadPreset,
  deletePreset,
  exportConfig: exportConfigData,
  importConfig: importConfigData,
  generateShareLink
} = usePersonalization()

watch(() => props.modelValue, (val) => {
  visible.value = val
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

// New KPI form
const newKpi = ref({
  label: '',
  metric: '',
  format: 'number' as const
})

const canAddKpi = computed(() => {
  return newKpi.value.label && newKpi.value.metric
})

const addKpi = () => {
  if (canAddKpi.value) {
    addCustomKpi({
      id: `kpi-${Date.now()}`,
      ...newKpi.value
    })
    newKpi.value = { label: '', metric: '', format: 'number' }
    ElMessage.success('KPI已添加')
  }
}

const removeKpi = (id: string) => {
  removeCustomKpi(id)
  ElMessage.success('KPI已删除')
}

// Panel layout updates
const updatePanelVisibility = (panelId: string, visible: boolean) => {
  updatePanelLayout(panelId, { visible })
}

const updatePanelWidth = (panelId: string, width: number) => {
  updatePanelLayout(panelId, { position: { ...config.value.layouts.find(p => p.id === panelId)!.position, w: width } })
}

const updatePanelHeight = (panelId: string, height: number) => {
  updatePanelLayout(panelId, { position: { ...config.value.layouts.find(p => p.id === panelId)!.position, h: height } })
}

// Preset management
const newPreset = ref({
  name: '',
  description: ''
})

const canSavePreset = computed(() => newPreset.value.name.trim().length > 0)

const savePreset = () => {
  if (canSavePreset.value) {
    saveAsPreset(newPreset.value.name, newPreset.value.description)
    newPreset.value = { name: '', description: '' }
    ElMessage.success('配置已保存')
  }
}

// Import/Export
const importData = ref('')

const exportConfig = () => {
  const data = exportConfigData()
  const blob = new Blob([data], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `dashboard-config-${new Date().toISOString().split('T')[0]}.json`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('配置已导出')
}

const copyShareLink = async () => {
  const link = generateShareLink()
  try {
    await navigator.clipboard.writeText(link)
    ElMessage.success('分享链接已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

const importConfig = () => {
  if (importData.value) {
    const success = importConfigData(importData.value)
    if (success) {
      ElMessage.success('配置已导入')
      importData.value = ''
    } else {
      ElMessage.error('导入失败，请检查JSON格式')
    }
  }
}

// Utility
const formatDate = (dateString: string): string => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN')
}

const saveAndClose = () => {
  updateConfig(config.value)
  visible.value = false
  ElMessage.success('配置已保存')
}
</script>

<style scoped>
.personalization-panel {
  max-height: 70vh;
  overflow-y: auto;
}

.tab-content {
  padding: 16px 0;
}

.tab-content h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
}

.tab-content h4 {
  margin: 16px 0 12px 0;
  font-size: 14px;
  font-weight: 500;
}

.tab-description {
  margin: 0 0 16px 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

/* Layout Grid */
.layout-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 24px;
}

.layout-item {
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  border: 1px solid var(--el-border-color-light);
  opacity: 0.6;
  transition: opacity 0.2s;
}

.layout-item.is-visible {
  opacity: 1;
}

.layout-item-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.drag-handle {
  cursor: grab;
  color: var(--el-text-color-secondary);
}

.drag-handle:active {
  cursor: grabbing;
}

.panel-name {
  flex: 1;
  font-weight: 500;
}

.layout-item-config {
  display: flex;
  gap: 12px;
  padding-left: 36px;
}

.grid-settings {
  padding: 16px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
}

.settings-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

/* KPI List */
.kpi-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.kpi-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
}

.kpi-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.kpi-label {
  font-weight: 500;
}

.add-kpi-form {
  max-width: 400px;
}

/* Preset List */
.preset-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.preset-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  border: 1px solid transparent;
}

.preset-item.is-default {
  border-color: var(--el-color-primary);
}

.preset-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.preset-name {
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
}

.preset-badge {
  font-size: 11px;
  padding: 2px 6px;
  background: var(--el-color-primary);
  color: white;
  border-radius: 4px;
}

.preset-desc {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.preset-date {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}

.preset-actions {
  display: flex;
  gap: 8px;
}

.save-preset-form {
  max-width: 400px;
}

/* Import/Export */
.export-section,
.import-section {
  padding: 16px 0;
}

/* High contrast mode */
:global(.high-contrast) .layout-item,
:global(.high-contrast) .kpi-item,
:global(.high-contrast) .preset-item,
:global(.high-contrast) .grid-settings {
  border-width: 2px;
}

/* Reduced motion */
:global(.reduce-motion) .layout-item,
:global(.reduce-motion) .kpi-item,
:global(.reduce-motion) .preset-item {
  transition: none;
}
</style>
