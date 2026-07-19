<template>
  <a-modal
    v-model:open="visible"
    title="页面配置"
    :width="720"
    :footer="null"
    :closable="true"
    :mask-closable="false"
    @cancel="handleClose"
  >
    <a-tabs v-model:active-key="activeTab" class="config-tabs">
      <!-- ═══ Tab 1: 查询条件 ═══ -->
      <a-tab-pane key="fields" tab="查询条件">
        <div class="tab-tip">勾选控制搜索条件显隐，拖拽可排序</div>
        <div class="config-table-wrap">
          <table class="config-table">
            <thead>
              <tr>
                <th style="width: 40px" />
                <th>名称</th>
                <th style="width: 80px">显示</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(field, index) in queryFields"
                :key="field.key"
                draggable="true"
                @dragstart="onDragStart(index)"
                @dragover.prevent="onDragOver(index)"
                @drop="onDrop"
                @dragend="dragIndex = -1"
              >
                <td class="cell-center drag-handle">⠿</td>
                <td>{{ field.label }}</td>
                <td class="cell-center">
                  <a-checkbox v-model:checked="field.visible" @change="handleFieldChange" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="tab-footer">
          <a-button @click="handleResetFields">恢复默认</a-button>
          <a-button type="primary" @click="handleClose">关闭</a-button>
        </div>
      </a-tab-pane>

      <!-- ═══ Tab 2: 功能按钮 ═══ -->
      <a-tab-pane key="buttons" tab="功能按钮">
        <div class="tab-tip">勾选控制工具栏按钮启用/禁用</div>
        <div class="config-table-wrap">
          <table class="config-table">
            <thead>
              <tr>
                <th>按钮</th>
                <th style="width: 80px">启用</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="btn in functionButtons" :key="btn.key">
                <td>{{ btn.label }}</td>
                <td class="cell-center">
                  <a-checkbox v-model:checked="btn.enabled" @change="handleButtonChange" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="tab-footer">
          <a-button @click="handleResetButtons">恢复默认</a-button>
          <a-button type="primary" @click="handleClose">关闭</a-button>
        </div>
      </a-tab-pane>

      <!-- ═══ Tab 3: 打印配置 ═══ -->
      <a-tab-pane key="print" tab="打印配置">
        <div class="tab-tip">打印配置仅对该操作员有效</div>
        <div class="print-settings">
          <a-checkbox v-model:checked="printConfig.alwaysLastTemplate" @change="handlePrintChange">
            始终使用最后一次打印的模板，打印时不再选择
          </a-checkbox>
          <a-checkbox v-model:checked="printConfig.linkReturnApply" @change="handlePrintChange">
            批量打印关联退货申请单
          </a-checkbox>
        </div>
        <div class="tab-footer">
          <a-button @click="handleResetPrint">恢复默认</a-button>
          <a-button type="primary" @click="handleClose">关闭</a-button>
        </div>
      </a-tab-pane>
    </a-tabs>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'

defineOptions({ name: 'PageConfigPanel' })

const props = defineProps<{
  open: boolean
  /** 可选：覆盖默认查询条件字段列表 */
  queryFieldsConfig?: QueryFieldSetting[]
  /** 可选：覆盖默认功能按钮列表 */
  functionButtonsConfig?: FunctionButtonSetting[]
  /** 可选：覆盖默认存储键 */
  storageKey?: string
}>()
const emit = defineEmits<{
  'update:open': [v: boolean]
  'change': [config: PageConfig]
}>()

// ── Types ──
export interface QueryFieldSetting {
  key: string
  label: string
  visible: boolean
}

export interface FunctionButtonSetting {
  key: string
  label: string
  enabled: boolean
}

export interface PrintConfigSetting {
  alwaysLastTemplate: boolean
  linkReturnApply: boolean
}

export interface PageConfig {
  queryFields: QueryFieldSetting[]
  functionButtons: FunctionButtonSetting[]
  printConfig: PrintConfigSetting
}

// ── Storage (支持父组件覆盖) ──
const STORAGE_KEY_DEFAULT = 'sale-order-page-config'
const activeStorageKey = computed(() => props.storageKey || STORAGE_KEY_DEFAULT)
const activeDefaultQueryFields = computed(() => props.queryFieldsConfig || DEFAULT_QUERY_FIELDS)
const activeDefaultFunctionButtons = computed(() => props.functionButtonsConfig || DEFAULT_FUNCTION_BUTTONS)

// ── Defaults ──
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'orderDate', label: '单据日期', visible: true },
  { key: 'orderNo', label: '单据编号', visible: true },
  { key: 'deliveryRoute', label: '配送线路', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'salesmanName', label: '经手人', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'settlementMethod', label: '结款方式', visible: true },
  { key: 'supplementType', label: '补单类型', visible: true },
  { key: 'submitTime', label: '提交时间', visible: true },
  { key: 'productName', label: '商品', visible: true },
  { key: 'brand', label: '品牌', visible: false },
  { key: 'industryCategory', label: '所属行业类别', visible: false },
  { key: 'deptName', label: '部门', visible: false },
  { key: 'warehouseName', label: '仓库', visible: false },
  { key: 'generationMethod', label: '产生方式', visible: false },
  { key: 'orderRemark', label: '单据备注', visible: false },
  { key: 'detailRemark', label: '明细备注', visible: false },
  { key: 'isGift', label: '是否赠品', visible: false },
  { key: 'creatorName', label: '制单人', visible: false },
  { key: 'auditorName', label: '审核人', visible: false },
  { key: 'submitterName', label: '提交人', visible: false },
  { key: 'extNum1', label: '表头自定义字段1(数字)', visible: false },
  { key: 'extNum2', label: '表头自定义字段2(数字)', visible: false },
  { key: 'extText1', label: '表头自定义字段3(文本)', visible: false },
  { key: 'extText2', label: '表头自定义字段4(文本)', visible: false },
  { key: 'extText3', label: '表头自定义字段5(文本)', visible: false },
  { key: 'footerExtText1', label: '表尾自定义1(文本)', visible: false },
  { key: 'footerExtText2', label: '表尾自定义2(文本)', visible: false },
  { key: 'shipDateStart', label: '发货日期(起)', visible: false },
  { key: 'shipDateEnd', label: '发货日期(止)', visible: false },
  { key: 'receiverName', label: '收货人', visible: false },
  { key: 'receiverPhone', label: '联系电话', visible: false },
  { key: 'shippingAddress', label: '收货地址', visible: false },
  { key: 'logisticsCompany', label: '物流公司', visible: false },
  { key: 'waybillNo', label: '运单号', visible: false },
  { key: 'region', label: '区域', visible: false },
  { key: 'saleType', label: '销售类型', visible: false },
  { key: 'itemProperty', label: '商品行属性', visible: false },
  { key: 'deliveryMethod', label: '配送方式', visible: false },
  { key: 'deliveryDriver', label: '配送司机', visible: false },
  { key: 'deliveryVehicle', label: '配送车辆', visible: false },
  { key: 'sellerRemark', label: '卖家备注', visible: false },
  { key: 'buyerRemark', label: '买家备注', visible: false },
  { key: 'summary', label: '摘要', visible: false },
  { key: 'printCount', label: '打印次数', visible: false },
  { key: 'minAmount', label: '本单金额最小', visible: false },
  { key: 'maxAmount', label: '本单金额最大', visible: false },
  { key: 'auditTime', label: '审核时间', visible: false },
  { key: 'source', label: '来源', visible: false },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'batchPrint', label: '批量打印', enabled: true },
  { key: 'productSummary', label: '商品汇总', enabled: true },
  { key: 'import', label: '批量导入', enabled: true },
  { key: 'logisticsRemark', label: '物流备注', enabled: true },
  { key: 'print', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '列配置', enabled: true },
  { key: 'auditPass', label: '审核通过', enabled: true },
  { key: 'pickComplete', label: '拣完批量发货', enabled: true },
]

const DEFAULT_PRINT_CONFIG: PrintConfigSetting = {
  alwaysLastTemplate: false,
  linkReturnApply: false,
}

// ── State ──
const visible = ref(false)
const activeTab = ref('fields')
const queryFields = ref<QueryFieldSetting[]>([])
const functionButtons = ref<FunctionButtonSetting[]>([])
const printConfig = reactive<PrintConfigSetting>({ ...DEFAULT_PRINT_CONFIG })

// ═══ Drag state ═══
let dragIndex = -1
function onDragStart(index: number) { dragIndex = index }
function onDragOver(index: number) {
  if (dragIndex === -1 || dragIndex === index) return
  const item = queryFields.value.splice(dragIndex, 1)[0]
  queryFields.value.splice(index, 0, item)
  dragIndex = index
}
function onDrop() { dragIndex = -1 }

// ═══ Sync open prop ═══
watch(() => props.open, (val) => {
  visible.value = val
  if (val) loadConfig()
})
watch(visible, (val) => emit('update:open', val))

// 当外部配置prop变化时（如切换tab），直接从prop加载显示
watch(
  () => props.queryFieldsConfig,
  (newFields) => {
    if (visible.value && newFields && newFields.length > 0) {
      queryFields.value = newFields.map(f => ({ ...f }))
    }
  },
  { deep: true }
)

// ═══ Storage ═══
function loadConfig() {
  try {
    const raw = localStorage.getItem(activeStorageKey.value)
    if (raw) {
      const parsed = JSON.parse(raw)
      queryFields.value = activeDefaultQueryFields.value.map(df => {
        const saved = parsed.queryFields?.find((f: QueryFieldSetting) => f.key === df.key)
        return saved ? { ...df, ...saved } : { ...df }
      })
      functionButtons.value = activeDefaultFunctionButtons.value.map(bf => {
        const saved = parsed.functionButtons?.find((f: FunctionButtonSetting) => f.key === bf.key)
        return saved ? { ...bf, ...saved } : { ...bf }
      })
      Object.assign(printConfig, { ...DEFAULT_PRINT_CONFIG, ...parsed.printConfig })
    } else {
      resetToDefaults()
    }
  } catch {
    resetToDefaults()
  }
  // 加载完成后通知父组件，以便初始化搜索字段显隐
  emitChange()
}

function persistConfig() {
  localStorage.setItem(activeStorageKey.value, JSON.stringify({
    queryFields: queryFields.value,
    functionButtons: functionButtons.value,
    printConfig: { ...printConfig },
  }))
  emitChange()
}

function resetToDefaults() {
  queryFields.value = activeDefaultQueryFields.value.map(f => ({ ...f }))
  functionButtons.value = activeDefaultFunctionButtons.value.map(f => ({ ...f }))
  Object.assign(printConfig, { ...DEFAULT_PRINT_CONFIG })
}

function emitChange() {
  emit('change', {
    queryFields: queryFields.value.map(f => ({ ...f })),
    functionButtons: functionButtons.value.map(f => ({ ...f })),
    printConfig: { ...printConfig },
  })
}

// ═══ Handlers ═══
function handleFieldChange() { persistConfig() }
function handleButtonChange() { persistConfig() }
function handlePrintChange() { persistConfig() }
function handleResetFields() {
  queryFields.value = activeDefaultQueryFields.value.map(f => ({ ...f }))
  persistConfig()
}
function handleResetButtons() {
  functionButtons.value = activeDefaultFunctionButtons.value.map(f => ({ ...f }))
  persistConfig()
}
function handleResetPrint() {
  Object.assign(printConfig, { ...DEFAULT_PRINT_CONFIG })
  persistConfig()
}
function handleClose() { emit('update:open', false) }

onMounted(() => { if (props.open) loadConfig() })
</script>

<style scoped>
.config-tabs :deep(.ant-tabs-nav) { padding: 0 16px; margin-bottom: 0; }
.config-tabs :deep(.ant-tabs-content-holder) { border: 1px solid #d9d9d9; border-top: none; border-radius: 0 0 4px 4px; }
.config-tabs :deep(.ant-tabs-content) { padding: 0 16px 16px; }
.config-tabs :deep(.ant-tabs-ink-bar) { display: none; }
.config-tabs :deep(.ant-tabs-nav::before) { border-bottom: none; }

.tab-tip { font-size: 12px; color: #fa8c16; margin-bottom: 12px; }
.config-table-wrap { max-height: 420px; overflow-y: auto; border: 1px solid #e8e8e8; border-radius: 4px; }
.config-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.config-table thead { position: sticky; top: 0; z-index: 1; background: #fafafa; }
.config-table th { padding: 8px 12px; text-align: left; font-weight: 600; color: #262626; border-bottom: 1px solid #e8e8e8; }
.config-table td { padding: 6px 12px; border-bottom: 1px solid #f0f0f0; vertical-align: middle; }
.config-table tbody tr { transition: background 0.15s; }
.config-table tbody tr:hover { background: #e6f7ff; }
.cell-center { text-align: center; }
.drag-handle { cursor: grab; color: #bbb; font-size: 14px; letter-spacing: 2px; user-select: none; }
.tab-footer { display: flex; justify-content: flex-end; align-items: center; padding: 12px 0 0; gap: 8px; }
.print-settings { display: flex; flex-direction: column; gap: 16px; padding: 8px 0; }
</style>
