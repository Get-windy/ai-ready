<template>
  <ErrorBoundary @error="handleError"><PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <span class="page-header__breadcrumb">系统设置 / 库存管理模式</span>
          <h2 class="page-header__title">库存管理模式配置</h2>
        </div>
        <div class="page-header__right">
          <a-space :size="12">
            <span class="data-status">
              <a-badge :status="loading ? 'processing' : 'success'" />
              <span v-if="lastUpdateTime" class="update-time">数据更新: {{ lastUpdateTime }}</span>
              <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
                <SyncOutlined /> {{ autoRefreshCountdown }}s
              </span>
            </span>
            <a-button size="small" :loading="loading" @click="debounceClick('refresh', fetchMode)">
              <ReloadOutlined /> 刷新
            </a-button>
          
                <span class="shortcut-hints">
                  <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
                </span>
          </a-space>
        </div>
      </div>

        </template>

    <a-row :gutter="24">
      <a-col :span="16">
        <!-- 当前模式 -->
        <a-card title="当前配置" class="config-card">
          <a-result v-if="!loading" :status="modeIcon" :title="`当前模式：${currentModeLabel}`">
            <template #icon>
              <component :is="modeIconComponent" :style="{ fontSize: '48px' }" />
            </template>
            <template #extra>
              <a-space>
                <a-select v-model:value="selectedMode" :options="modeOptions" style="width: 200px" size="small" />
                <a-button v-permission="'erp:product:inventory-mode'" type="primary" size="small" :loading="saving" @click="handleSave">保存配置</a-button>
              </a-space>
            </template>
            <p class="mode-description">{{ currentModeDescription }}</p>
          </a-result>
          <a-spin v-else />
        </a-card>

        <!-- 模式说明 -->
        <a-card title="管理模式说明" class="config-card">
          <a-table
            :data-source="modeOptions"
            :columns="modeColumns"
            :pagination="false as any"
            size="small"
            row-key="value"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'icon'">
                <CheckCircleOutlined v-if="record.value === selectedMode || record.value === currentMode" style="color: #52c41a;" />
                <MinusOutlined v-else style="color: #d9d9d9;" />
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :span="8">
        <!-- 模式对产品/库存的影响 -->
        <a-card title="模式影响范围" class="config-card">
          <a-timeline>
            <a-timeline-item color="blue">
              <template #dot><AppstoreOutlined /></template>
              <b>产品管理</b>
              <p class="impact-detail">{{ productImpact }}</p>
            </a-timeline-item>
            <a-timeline-item color="green">
              <template #dot><ContainerOutlined /></template>
              <b>库存管理</b>
              <p class="impact-detail">{{ stockImpact }}</p>
            </a-timeline-item>
            <a-timeline-item color="orange">
              <template #dot><ShoppingCartOutlined /></template>
              <b>采购/销售</b>
              <p class="impact-detail">{{ orderImpact }}</p>
            </a-timeline-item>
          </a-timeline>
        </a-card>
      </a-col>
    </a-row>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, defineExpose } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined, SyncOutlined, CheckCircleOutlined, MinusOutlined,
  AppstoreOutlined, ContainerOutlined, ShoppingCartOutlined,
  AuditOutlined, BarcodeOutlined, NumberOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { inventoryModeApi, type InventoryMode, type ModeOption } from '@/api/erp/product'

function handleError(err: any) { console.warn('[库存模式] ErrorBoundary 捕获异常:', err) }

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

const loading = ref(false)
const saving = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const currentMode = ref<InventoryMode>('BATCH')
const selectedMode = ref<InventoryMode>('BATCH')
const modeOptions = ref<ModeOption[]>([])

const modeColumns = [
  { dataIndex: 'icon', key: 'icon', title: '当前', width: 50 },
  { dataIndex: 'value', title: '编码', width: 80 },
  { dataIndex: 'label', title: '模式名称', width: 120 },
  { dataIndex: 'description', title: '说明' }
]

const modeLabels: Record<InventoryMode, string> = {
  BATCH: '批次管理',
  SERIAL: '序列号管理',
  SKU: 'SKU管理'
}

const currentModeLabel = computed(() => modeLabels[currentMode.value])

const currentModeDescription = computed(() => {
  const desc: Record<InventoryMode, string> = {
    BATCH: '按批次追踪库存，记录每批产品的生产日期和有效期，适用于食品、医药、化工等行业。',
    SERIAL: '每件产品有独立序列号，实现一物一码全生命周期追踪，适用于电子设备、高价值资产等。',
    SKU: '按库存单位编码管理，不追踪批次或序列号，适用于简单场景或低价值商品。'
  }
  return desc[currentMode.value]
})

const modeIcon = computed(() => {
  const icons: Record<InventoryMode, 'success' | 'info' | 'warning'> = {
    BATCH: 'info',
    SERIAL: 'success',
    SKU: 'warning'
  }
  return icons[currentMode.value]
})

const modeIconComponent = computed(() => {
  const icons: Record<InventoryMode, any> = {
    BATCH: AuditOutlined,
    SERIAL: BarcodeOutlined,
    SKU: NumberOutlined
  }
  return icons[currentMode.value]
})

const productImpact = computed(() => {
  const map: Record<InventoryMode, string> = {
    BATCH: '产品可以设置SKU。入库时需录入批次号、生产日期、有效期。',
    SERIAL: '产品可以设置SKU。每个产品实例需分配唯一序列号。',
    SKU: '产品必须设置SKU编码，库存按SKU汇总管理。'
  }
  return map[currentMode.value]
})

const stockImpact = computed(() => {
  const map: Record<InventoryMode, string> = {
    BATCH: '库存按产品+批次+仓库维度管理，支持批次追踪和有效期预警。',
    SERIAL: '库存按每件序列号管理，支持单个产品全生命周期追踪。',
    SKU: '库存按产品+仓库维度汇总管理，简单数量增减。'
  }
  return map[currentMode.value]
})

const orderImpact = computed(() => {
  const map: Record<InventoryMode, string> = {
    BATCH: '出库时需指定批次，支持先进先出(FIFO)和批次拣选。',
    SERIAL: '出库时需扫描/指定序列号，精确追踪每件产品去向。',
    SKU: '按SKU数量管理，通知出库数量即可。'
  }
  return map[currentMode.value]
})

async function fetchMode() {
  loading.value = true
  try {
    const [mode, options] = await Promise.all([
      inventoryModeApi.get(),
      inventoryModeApi.options()
    ])
    currentMode.value = mode
    selectedMode.value = mode
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
    modeOptions.value = options.map(o => ({
      ...o,
      key: o.value
    }))
  } catch (e) {
    console.error('[库存模式] 获取配置失败', e)
    message.error('获取配置失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (selectedMode.value === currentMode.value) {
    message.info('模式未变更')
    return
  }
  saving.value = true
  try {
    await inventoryModeApi.set(selectedMode.value)
    currentMode.value = selectedMode.value
    message.success(`库存管理模式已切换为「${modeLabels[selectedMode.value]}」`)
  } catch (e: unknown) {
    console.error('[库存模式] 保存失败', e)
    const msg = e instanceof Error ? e.message : '保存失败'
    message.error(msg)
  } finally {
    saving.value = false
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    debounceClick('refresh', fetchMode)
  }
}

let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  fetchMode()
  document.addEventListener('keydown', handleKeydown)
  autoRefreshCountdown.value = 300
  refreshTimer = setInterval(() => {
    fetchMode()
    autoRefreshCountdown.value = 300
  }, 300000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
})

defineExpose({ fetchData: fetchMode })
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.page-header__left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.page-header__breadcrumb {
  font-size: 12px;
  color: #999;
}
.page-header__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.page-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.config-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.mode-description {
  margin-top: 8px;
  color: #666;
  font-size: 14px;
  line-height: 1.8;
}

.impact-detail {
  margin: 4px 0 0;
  font-size: 13px;
  color: #666;
  line-height: 1.6;
}

.data-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}
.update-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

</style>
