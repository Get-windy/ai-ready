<template>
  <ErrorBoundary
    @reset="fetchDetail"
    @error="handleError"
  >
    <PageContainer title="拆分单">
      <!-- ═══ 头部工具栏 ═══ -->
      <div class="toolbar">
        <div class="toolbar-left">
          <span v-if="formData.splitNo" class="doc-no">NO. {{ formData.splitNo }}</span>
          <a-tag :color="ASSEMBLE_STATUS[formData.status ?? 0]?.color || 'default'">
            {{ ASSEMBLE_STATUS[formData.status ?? 0]?.text || '-' }}
          </a-tag>
        </div>
        <div class="toolbar-right">
          <a-space :size="6">
            <a-button size="small" @click="handlePrint"><PrinterOutlined /> 打印(F8)</a-button>
            <a-button size="small" @click="handleHistory"><HistoryOutlined /> 历史</a-button>
            <a-button size="small" @click="handleExport"><ExportOutlined /> 导出</a-button>
            <a-button size="small" :disabled="!editable" @click="handleCalcCost"><CalculatorOutlined /> 计算成本</a-button>
            <a-button size="small" @click="openConfig"><SettingOutlined /> 配置</a-button>
          </a-space>
        </div>
      </div>

      <!-- ═══ 基本信息 ═══ -->
      <div class="panel">
        <div class="basic-info">
          <div class="info-flow">
            <div v-for="f in basicInfoFields" :key="f.key" class="info-field" :style="f.style">
              <span class="info-label" :class="{ 'required': f.required }">{{ f.label }}</span>
              <template v-if="f.type === 'select'">
                <a-select
                  v-model:value="formData[f.key]"
                  :placeholder="f.placeholder || '请选择' + f.label"
                  show-search
                  :filter-option="filterOption"
                  :disabled="f.disabled || !editable"
                  size="small"
                  style="width: 100%"
                  @change="(val: any) => handleSelectChange(f.key, val)"
                >
                  <a-select-option v-for="opt in f.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
                </a-select>
              </template>
              <template v-else-if="f.type === 'date'">
                <a-date-picker
                  v-model:value="formData[f.key]"
                  :disabled="f.disabled || !editable"
                  size="small"
                  style="width: 100%"
                  value-format="YYYY-MM-DD"
                  :placeholder="'请选择' + f.label"
                />
              </template>
              <template v-else-if="f.type === 'number'">
                <a-input-number
                  v-model:value="formData[f.key]"
                  :min="0"
                  :precision="2"
                  :disabled="f.disabled || !editable"
                  size="small"
                  style="width: 100%"
                  placeholder="0.00"
                />
              </template>
              <template v-else>
                <a-input
                  v-model:value="formData[f.key]"
                  :disabled="f.disabled || !editable"
                  size="small"
                  style="width: 100%"
                  :placeholder="f.placeholder || ''"
                />
              </template>
            </div>
            <div v-if="editable" class="info-field bom-field">
              <span class="info-label">选择BOM模板</span>
              <a-select
                v-model:value="formData.bomId"
                placeholder="选择BOM模板带出明细"
                show-search
                :filter-option="filterOption"
                :options="bomTemplateOptions"
                :loading="bomLoading"
                size="small"
                style="width: 240px"
                allow-clear
                @change="handleBomChange"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- ═══ 成品详情(出库)表 ═══ -->
      <div class="panel table-panel">
        <div class="panel-title">
          成品详情(出库)
          <a-tag v-if="stockValid !== undefined" :color="stockValid ? 'success' : 'error'">
            {{ stockValid ? '库存充足' : '库存不足' }}
          </a-tag>
        </div>
        <BillDetailTable
          :columns="productColumns"
          :data-source="formData.productRows"
          :min-rows="1"
          :storage-key="'stock-split-product-columns'"
          :summary-columns="productSummary"
          :view-mode="!editable"
          @cell-change="onProductCellChange"
          @expand-change="onExpand"
        >
          <template #productCell="{ record, index }">
            <a-select
              v-model:value="record.productId"
              placeholder="搜索选择商品"
              show-search
              :filter-option="filterOption"
              style="width:100%"
              :disabled="!editable"
              size="small"
              @change="(val: any) => selectProduct(val, index, 'product')"
            >
              <a-select-option v-for="p in productOptions" :key="p.id" :value="p.id">{{ p.name }}</a-select-option>
            </a-select>
          </template>
          <template #actionCell="{ index, empty }">
            <a-space :size="2">
              <a-button type="link" size="small" class="action-btn" :disabled="!editable" @click="addRow('product', index)"><PlusCircleOutlined /></a-button>
              <a-button v-if="!empty" type="link" size="small" class="action-btn danger" :disabled="!editable" @click="removeRow('product', index)"><MinusCircleOutlined /></a-button>
            </a-space>
          </template>
        </BillDetailTable>
      </div>

      <!-- ═══ 原料详情(入库)表 ═══ -->
      <div class="panel table-panel">
        <div class="panel-title">
          原料详情(入库)
          <a-tag color="blue">成品出库+原料入库同单</a-tag>
        </div>
        <BillDetailTable
          :columns="materialColumns"
          :data-source="formData.materialRows"
          :min-rows="5"
          :storage-key="'stock-split-material-columns'"
          :summary-columns="materialSummary"
          :view-mode="!editable"
          @cell-change="onMaterialCellChange"
          @expand-change="onExpand"
        >
          <template #productCell="{ record, index }">
            <a-select
              v-model:value="record.productId"
              placeholder="搜索选择商品"
              show-search
              :filter-option="filterOption"
              style="width:100%"
              :disabled="!editable"
              size="small"
              @change="(val: any) => selectProduct(val, index, 'material')"
            >
              <a-select-option v-for="p in productOptions" :key="p.id" :value="p.id">{{ p.name }}</a-select-option>
            </a-select>
          </template>
          <template #actionCell="{ index, empty }">
            <a-space :size="2">
              <a-button type="link" size="small" class="action-btn" :disabled="!editable" @click="addRow('material', index)"><PlusCircleOutlined /></a-button>
              <a-button v-if="!empty" type="link" size="small" class="action-btn danger" :disabled="!editable" @click="removeRow('material', index)"><MinusCircleOutlined /></a-button>
            </a-space>
          </template>
        </BillDetailTable>
      </div>

      <!-- ═══ 备注区 ═══ -->
      <div class="panel">
        <div class="remark-row">
          <span class="remark-label">单据备注</span>
          <a-input v-model:value="formData.remark" size="small" class="remark-input" placeholder="请输入单据备注" :disabled="!editable" />
        </div>
        <div class="remark-row">
          <span class="remark-label">摘要</span>
          <a-input v-model:value="formData.summary" size="small" class="remark-input" placeholder="请输入摘要" :disabled="!editable" />
        </div>
      </div>

      <!-- ═══ 单据信息 ═══ -->
      <div class="panel doc-info-row">
        <span>制单人: <b>{{ formData.creatorName || currentUserName || '-' }}</b></span>
        <span>制单时间: <b>{{ formData.createTime || '-' }}</b></span>
        <span v-if="formData.bookkeeperName">记账人: <b>{{ formData.bookkeeperName }}</b></span>
        <span v-if="formData.bookkeepingTime">记账时间: <b>{{ formData.bookkeepingTime }}</b></span>
        <span>打印次数: <b>{{ formData.printCount ?? 0 }}</b></span>
        <span>打印记录: <b>{{ formData.printRecords || '无' }}</b></span>
      </div>

      <!-- ═══ 本单金额 ═══ -->
      <div class="total-bar">
        <span class="total-label">本单金额</span>
        <span class="total-value">¥{{ totalCost.toFixed(2) }}</span>
      </div>

      <!-- ═══ 底部操作 ═══ -->
      <div class="btn-row">
        <a-space>
          <template v-if="isNew || editable">
            <a-button :loading="saving" size="large" @click="handleSaveDraft">
              保存草稿 <span class="shortcut">Ctrl+S</span>
            </a-button>
            <a-button type="primary" :loading="submitting" :disabled="!stockValid" size="large" @click="handleSubmit">
              记帐 <span class="shortcut">Ctrl+Enter</span>
            </a-button>
          </template>
          <template v-if="!isNew && formData.status === 1">
            <a-button type="primary" :loading="approving" size="large" @click="handleApprove">审批通过</a-button>
            <a-button danger :loading="rejecting" size="large" @click="handleReject">拒绝</a-button>
          </template>
          <a-button v-if="!isNew && formData.status === 2" type="primary" :loading="executing" size="large" @click="handleExecute">执行拆分</a-button>
          <a-button size="large" @click="handleBack">返回</a-button>
        </a-space>
      </div>

      <!-- ═══ 配置弹窗：页面配置/录单默认值/打印设置 ═══ -->
      <a-modal v-model:open="showFormConfig" title="配置" :width="760" :footer="null" destroy-on-close>
        <a-tabs v-model:active-key="configModalTab" size="small">
          <a-tab-pane key="pageConfig" tab="页面配置">
            <p class="config-hint">勾选后自动保存，该设置对所有操作员生效</p>
            <a-table :columns="pageConfigTableColumns" :data-source="pageConfigFields" :pagination="false" size="small" row-key="key">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'displayName'">
                  <a-input v-model:value="record.displayName" size="small" @blur="saveFormConfig" />
                </template>
                <template v-if="column.key === 'visible'">
                  <a-checkbox :checked="record.visible" @change="(e: any) => handlePageConfigFieldVisibleChange(record.key, e.target.checked)" />
                </template>
                <template v-if="column.key === 'enterJump'">
                  <a-checkbox :checked="record.enterJump" @change="(e: any) => handlePageConfigEnterJumpChange(record.key, e.target.checked)" />
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="defaultValues" tab="录单默认值">
            <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-form-item label="默认成品仓库">
                <a-select v-model:value="formData.defaultOutWarehouseId" show-search size="small" style="width:100%" :filter-option="filterOption" :options="warehouseOptions" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认原料仓库">
                <a-select v-model:value="formData.defaultInWarehouseId" show-search size="small" style="width:100%" :filter-option="filterOption" :options="warehouseOptions" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认经手人">
                <a-input v-model:value="formData.defaultHandlerName" size="small" style="width:100%" @change="saveFormConfig" />
              </a-form-item>
            </a-form>
          </a-tab-pane>
          <a-tab-pane key="printSettings" tab="打印设置">
            <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-form-item label="打印模板">
                <a-select v-model:value="formData.printTemplate" size="small" style="width:100%" @change="saveFormConfig">
                  <a-select-option value="standard">标准模板</a-select-option>
                  <a-select-option value="simple">简化模板</a-select-option>
                  <a-select-option value="detailed">详细模板</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="打印份数">
                <a-input-number v-model:value="formData.printCopies" :precision="0" :min="1" size="small" style="width:100%" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="纸张大小">
                <a-select v-model:value="formData.printPaperSize" size="small" style="width:100%" @change="saveFormConfig">
                  <a-select-option value="A4">A4</a-select-option>
                  <a-select-option value="A5">A5</a-select-option>
                  <a-select-option value="B5">B5</a-select-option>
                </a-select>
              </a-form-item>
            </a-form>
          </a-tab-pane>
        </a-tabs>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PrinterOutlined, HistoryOutlined, ExportOutlined, CalculatorOutlined, SettingOutlined,
  PlusCircleOutlined, MinusCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { stockSplitApi, userPageConfigApi } from '@/api/erp'
import optionsApi from '@/api/options'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { ASSEMBLE_STATUS } from '@/utils/statusConfig'

defineOptions({ name: 'StockSplitForm' })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const idParam = computed(() => route.params.id as string)
const isNew = computed(() => !idParam.value || idParam.value === 'new')
const editable = computed(() => isNew.value || formData.status === 0)
const currentUserName = computed(() => userStore.nickname || userStore.username || '')

// ── 选项数据 ──────────────────────────────────────────
const bomTemplateOptions = ref<any[]>([])
const productOptions = ref<any[]>([])
const warehouseOptions = ref<any[]>([])
const bomLoading = ref(false)

// 仓库下拉 options（label/value）
const warehouseSelectOptions = computed(() =>
  warehouseOptions.value.map((w: any) => ({ label: w.warehouseName || w.name, value: w.id }))
)

// ── 表单数据 ──────────────────────────────────────────
let keyCounter = 0
function nextKey() { return `item_${++keyCounter}_${Date.now()}` }

const formData = reactive<any>({
  id: undefined,
  splitNo: '',
  bomId: undefined,
  inWarehouseId: undefined,
  inWarehouseName: '',
  outWarehouseId: undefined,
  outWarehouseName: '',
  handlerName: '',
  deptName: '',
  splitDate: '',
  summary: '',
  remark: '',
  status: 0,
  creatorName: '',
  createTime: '',
  bookkeeperName: '',
  bookkeepingTime: '',
  printCount: 0,
  printRecords: '',
  defaultOutWarehouseId: undefined,
  defaultInWarehouseId: undefined,
  defaultHandlerName: '',
  printTemplate: 'standard',
  printCopies: 0,
  printPaperSize: 'A4',
  productRows: [] as any[],
  materialRows: [] as any[],
})

// ── 明细行默认值 ──
function emptyRow(partial: Record<string, any> = {}) {
  return {
    _key: nextKey(),
    productId: undefined,
    productName: '',
    productCode: '',
    location: '',
    barcode: '',
    region: '',
    productSpec: '',
    model: '',
    origin: '',
    brand: '',
    image: '',
    itemExtNum1: 0,
    itemExtNum2: 0,
    itemExtNum3: 0,
    itemExtText1: '',
    itemExtText2: '',
    wholesalePrice: 0,
    retailPrice: 0,
    productUnit: '',
    availableStock: undefined,
    availableStockConverted: 0,
    batchCode: '',
    batchNo: '',
    productionDate: '',
    shelfLife: '',
    expiryDate: '',
    quantity: 1,
    conversionRelation: '',
    conversionResult: 0,
    pieceQuantity: 0,
    bigPack: 0,
    midPack: 0,
    smallPack: 0,
    smallUnit: '',
    smallUnitQuantity: 0,
    smallUnitPrice: 0,
    unitCost: 0,
    cost: 0,
    weight: 0,
    volume: 0,
    remark: '',
    ...partial,
  }
}

// ── 列定义（成品/原料均37列，对齐文档） ──
const PRODUCT_BASE_COLUMNS: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 70, fixed: 'left' },
  { key: 'image', title: '图片', type: 'input', width: 60, defaultHidden: true },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productCell', width: 230 },
  { key: 'productCode', title: '货号', type: 'input', width: 100 },
  { key: 'location', title: '货位', type: 'input', width: 90 },
  { key: 'barcode', title: '条码', type: 'input', width: 120 },
  { key: 'region', title: '区域', type: 'input', width: 90 },
  { key: 'productSpec', title: '规格', type: 'input', width: 100, defaultHidden: true },
  { key: 'model', title: '型号', type: 'input', width: 100, defaultHidden: true },
  { key: 'origin', title: '产地', type: 'input', width: 100, defaultHidden: true },
  { key: 'brand', title: '品牌', type: 'input', width: 100, defaultHidden: true },
  { key: 'itemExtNum1', title: '单据自定义1(数字)', type: 'number', width: 130, precision: 2, defaultHidden: true },
  { key: 'itemExtNum2', title: '单据自定义2(数字)', type: 'number', width: 130, precision: 2, defaultHidden: true },
  { key: 'itemExtNum3', title: '单据自定义3(数字)', type: 'number', width: 130, precision: 2, defaultHidden: true },
  { key: 'itemExtText1', title: '单据自定义4(文本)', type: 'input', width: 130, defaultHidden: true },
  { key: 'itemExtText2', title: '单据自定义5(文本)', type: 'input', width: 130, defaultHidden: true },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'retailPrice', title: '零售价', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'productUnit', title: '计价单位', type: 'input', width: 80 },
  { key: 'availableStock', title: '可用库存', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'availableStockConverted', title: '可用库存换算结果', type: 'number', width: 140, precision: 2, readonly: true, defaultHidden: true },
  { key: 'batchCode', title: '批次条码', type: 'input', width: 120 },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 110 },
  { key: 'shelfLife', title: '保质期', type: 'input', width: 100 },
  { key: 'expiryDate', title: '到期日期', type: 'date', width: 110 },
  { key: 'quantity', title: '数量', type: 'number', width: 100, precision: 2 },
  { key: 'conversionRelation', title: '换算关系', type: 'input', width: 100 },
  { key: 'pieceQuantity', title: '件散数量', type: 'number', width: 90, precision: 2 },
  { key: 'bigPack', title: '大包装', type: 'number', width: 80 },
  { key: 'midPack', title: '中包装', type: 'number', width: 80 },
  { key: 'smallPack', title: '小包装', type: 'number', width: 80 },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 70, defaultHidden: true },
  { key: 'smallUnitQuantity', title: '小单位数量', type: 'number', width: 100, precision: 2, defaultHidden: true },
  { key: 'unitCost', title: '成本单价', type: 'number', width: 100, precision: 2 },
  { key: 'cost', title: '成本金额', type: 'number', width: 110, precision: 2, readonly: true },
  { key: 'volume', title: '体积（m³）', type: 'number', width: 90, precision: 4, defaultHidden: true },
  { key: 'weight', title: '重量（kg）', type: 'number', width: 90, precision: 4, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
]

const productColumns = ref<DetailColumnConfig[]>(PRODUCT_BASE_COLUMNS.map(c => ({ ...c })))
const materialColumns = ref<DetailColumnConfig[]>(PRODUCT_BASE_COLUMNS.map(c => ({ ...c })))

// ── 计算属性 ──────────────────────────────────────────
const productCostTotal = computed(() =>
  formData.productRows.reduce((s: number, row: any) => s + ((Number(row.unitCost) || 0) * (Number(row.quantity) || 0)), 0)
)
const materialCostTotal = computed(() =>
  formData.materialRows.reduce((s: number, row: any) => s + ((Number(row.unitCost) || 0) * (Number(row.quantity) || 0)), 0)
)
const totalCost = computed(() => productCostTotal.value)
const stockValid = computed(() => {
  if (formData.productRows.length === 0) return undefined
  return formData.productRows.every((row: any) => {
    if (row.availableStock === undefined || row.availableStock === null) return true
    return Number(row.availableStock) >= Number(row.quantity || 0)
  })
})

const productSummary = computed(() => [
  { key: 'quantity', value: sumQty(formData.productRows), highlight: true },
  { key: 'cost', value: Number(productCostTotal.value.toFixed(2)), highlight: true },
])
const materialSummary = computed(() => [
  { key: 'quantity', value: sumQty(formData.materialRows), highlight: true },
  { key: 'cost', value: Number(materialCostTotal.value.toFixed(2)), highlight: true },
])

function sumQty(rows: any[]) {
  return Number(rows.reduce((s: number, r: any) => s + (Number(r.quantity) || 0), 0).toFixed(2))
}

// ── 基本信息字段（可配置显隐，无生产单位） ──
const ALL_BASIC_INFO_FIELDS = [
  { key: 'splitNo', label: '编号', type: 'input', disabled: true },
  { key: 'outWarehouseId', label: '成品仓库', type: 'select', required: true },
  { key: 'inWarehouseId', label: '原料仓库', type: 'select', required: true },
  { key: 'handlerName', label: '经手人', type: 'input', required: true },
  { key: 'splitDate', label: '单据日期', type: 'date', required: true },
]

const DEFAULT_HIDDEN_FIELDS: string[] = []
type FieldConfig = { visible: boolean; enterJump: boolean }
const pageConfig = reactive<Record<string, FieldConfig>>({})
for (const f of ALL_BASIC_INFO_FIELDS) {
  pageConfig[f.key] = {
    visible: !DEFAULT_HIDDEN_FIELDS.includes(f.key),
    enterJump: ['select', 'date', 'number', 'input'].includes(f.type),
  }
}

const basicInfoFields = computed(() =>
  ALL_BASIC_INFO_FIELDS
    .filter(f => pageConfig[f.key]?.visible !== false)
    .map(f => {
      const options = f.key === 'inWarehouseId' || f.key === 'outWarehouseId'
        ? warehouseSelectOptions.value
        : []
      return {
        ...f,
        style: { width: f.key === 'splitNo' ? '200px' : '180px' },
        options,
      }
    })
)

// ── 页面配置弹窗 ──
const showFormConfig = ref(false)
const configModalTab = ref('pageConfig')
const FORM_CONFIG_MODULE = 'stock-split-form'
const FORM_CONFIG_PAGE = 'form'

const pageConfigTableColumns = [
  { title: '序号', key: 'index', width: 60 },
  { title: '名称', key: 'name', width: 120 },
  { title: '显示名', key: 'displayName', width: 160 },
  { title: '显示', key: 'visible', width: 70, align: 'center' as const },
  { title: '回车键跳转', key: 'enterJump', width: 100, align: 'center' as const },
]
const pageConfigFields = computed(() =>
  ALL_BASIC_INFO_FIELDS.map((f, i) => ({
    key: f.key,
    index: i + 1,
    name: f.label,
    displayName: f.label,
    visible: pageConfig[f.key]?.visible !== false,
    enterJump: pageConfig[f.key]?.enterJump ?? false,
  }))
)

async function loadFormConfig() {
  try {
    const raw = await userPageConfigApi.get(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE)
    if (!raw) return
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (parsed.fields && typeof parsed.fields === 'object') {
      Object.keys(parsed.fields).forEach((k) => {
        if (pageConfig[k]) {
          pageConfig[k].visible = parsed.fields[k].visible !== false
          pageConfig[k].enterJump = !!parsed.fields[k].enterJump
        }
      })
    }
    if (parsed.defaults) {
      Object.assign(formData, {
        defaultOutWarehouseId: parsed.defaults.defaultOutWarehouseId,
        defaultInWarehouseId: parsed.defaults.defaultInWarehouseId,
        defaultHandlerName: parsed.defaults.defaultHandlerName || '',
        printTemplate: parsed.defaults.printTemplate,
        printCopies: parsed.defaults.printCopies,
        printPaperSize: parsed.defaults.printPaperSize,
      })
    }
  } catch { /* API 不可用时保持默认 */ }
}

async function saveFormConfig() {
  const payload = {
    fields: Object.fromEntries(Object.entries(pageConfig).map(([k, v]) => [k, { visible: v.visible, enterJump: v.enterJump }])),
    defaults: {
      defaultOutWarehouseId: formData.defaultOutWarehouseId,
      defaultInWarehouseId: formData.defaultInWarehouseId,
      defaultHandlerName: formData.defaultHandlerName,
      printTemplate: formData.printTemplate,
      printCopies: formData.printCopies,
      printPaperSize: formData.printPaperSize,
    },
  }
  try {
    await userPageConfigApi.save(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE, JSON.stringify(payload))
  } catch { /* 静默失败 */ }
}

function handlePageConfigFieldVisibleChange(fieldKey: string, visible: boolean) {
  if (pageConfig[fieldKey]) pageConfig[fieldKey].visible = visible
  saveFormConfig()
}
function handlePageConfigEnterJumpChange(fieldKey: string, checked: boolean) {
  if (pageConfig[fieldKey]) pageConfig[fieldKey].enterJump = checked
  saveFormConfig()
}

function openConfig() { showFormConfig.value = true }

// ── 行操作 ──────────────────────────────────────────
function addRow(kind: 'product' | 'material', index?: number) {
  if (!editable.value) return
  const row = emptyRow()
  const target = kind === 'product' ? formData.productRows : formData.materialRows
  if (index !== undefined && index >= 0 && index < target.length) {
    target.splice(index + 1, 0, row)
  } else {
    target.push(row)
  }
}
function removeRow(kind: 'product' | 'material', index: number) {
  if (!editable.value) return
  const target = kind === 'product' ? formData.productRows : formData.materialRows
  if (index >= 0 && index < target.length) target.splice(index, 1)
}

function selectProduct(val: number, index: number, kind: 'product' | 'material') {
  const p = productOptions.value.find((x: any) => x.id === val)
  if (!p) return
  const target = kind === 'product' ? formData.productRows : formData.materialRows
  const row = target[index]
  if (!row) return
  row.productId = p.id
  row.productName = p.name || ''
  row.productCode = p.code || ''
  row.barcode = p.barcode || ''
  row.productSpec = p.specification || ''
  row.model = p.model || ''
  row.origin = p.origin || ''
  row.brand = p.brand || ''
  row.image = p.image || ''
  row.productUnit = p.unit || ''
  row.wholesalePrice = p.wholesalePrice || 0
  row.retailPrice = p.retailPrice || 0
  row.unitCost = p.costPrice || p.purchasePrice || 0
  row.weight = p.weight || 0
  row.volume = p.volume || 0
  row.shelfLife = p.shelfLife || ''
  row.cost = (Number(row.quantity) || 0) * (Number(row.unitCost) || 0)
  checkStockAvailability()
}

function onCellChange(record: any, fieldKey: string) {
  if (['quantity', 'unitCost'].includes(fieldKey)) {
    record.cost = (Number(record.quantity) || 0) * (Number(record.unitCost) || 0)
  }
}
function onProductCellChange(record: any, fieldKey: string) {
  onCellChange(record, fieldKey)
  if (fieldKey === 'quantity') checkStockAvailability()
}
function onMaterialCellChange(record: any, fieldKey: string) {
  onCellChange(record, fieldKey)
}

function onExpand() { /* 表格展开/收起，交给BillDetailTable内部 */ }

function handleSelectChange(fieldKey: string, val: any) {
  if (fieldKey === 'outWarehouseId') {
    const w = warehouseOptions.value.find((x: any) => x.id === val)
    formData.outWarehouseName = w?.warehouseName || w?.name || ''
    checkStockAvailability()
  }
  if (fieldKey === 'inWarehouseId') {
    const w = warehouseOptions.value.find((x: any) => x.id === val)
    formData.inWarehouseName = w?.warehouseName || w?.name || ''
  }
}

// ── 数据加载 ──
async function loadBomTemplates() {
  bomLoading.value = true
  try {
    const res = await requestGet('/erp/stock/bom/page', { pageSize: 500, status: 1 })
    const records = res?.records || (Array.isArray(res) ? res : [])
    bomTemplateOptions.value = records.map((b: any) => ({
      label: `${b.bomNo || ''} - ${b.bomName || ''}`,
      value: b.id,
      bomNo: b.bomNo,
      bomName: b.bomName,
      productId: b.productId,
      productCode: b.productCode,
      productName: b.productName || b.productName2,
      productSpec: b.productSpec || b.spec,
      productUnit: b.productUnit || b.unit,
      outputQuantity: b.outputQuantity ?? 1,
      version: b.version,
    }))
  } catch { bomTemplateOptions.value = [] }
  finally { bomLoading.value = false }
}

async function loadProductOptions() {
  try {
    productOptions.value = await optionsApi.getProducts()
  } catch { productOptions.value = [] }
}

async function loadWarehouses() {
  try {
    const list = await optionsApi.getWarehouses()
    warehouseOptions.value = Array.isArray(list) ? list : []
  } catch { warehouseOptions.value = [] }
}

async function checkStockAvailability() {
  const warehouseId = formData.outWarehouseId
  const rows = formData.productRows
  if (!warehouseId || rows.length === 0) return
  const productIds = rows.map((r: any) => r.productId).filter(Boolean)
  if (productIds.length === 0) return
  try {
    const res = await requestPost('/erp/stock/batch-query', { warehouseId, productIds })
    const map: Record<number, number> = res || {}
    for (const row of rows) {
      row.availableStock = map[row.productId] ?? row.availableStock
    }
  } catch {
    // 静默失败，不影响主流程
  }
}

// ── 请求辅助 ──
async function requestGet(url: string, params?: any) {
  const res = await request.get(url, { params })
  return res?.data ?? res
}
async function requestPost(url: string, body?: any) {
  const res = await request.post(url, body)
  return res?.data ?? res
}

// ── BOM 模板驱动 ──
async function handleBomChange(bomId?: number) {
  formData.productRows = []
  formData.materialRows = []
  formData.bomId = bomId
  if (!bomId) return
  const bom = bomTemplateOptions.value.find((b: any) => b.value === bomId)
  if (!bom) return
  formData.productRows = [emptyRow({
    productId: bom.productId,
    productCode: bom.productCode || '',
    productName: bom.productName || '',
    productSpec: bom.productSpec || '',
    productUnit: bom.productUnit || '',
    quantity: bom.outputQuantity ?? 1,
  })]
  try {
    const components = await requestGet(`/erp/stock/bom/${bomId}/items`)
    formData.materialRows = (components || []).map((comp: any) => emptyRow({
      productId: comp.productId,
      productCode: comp.productCode || '',
      productName: comp.productName || '',
      productSpec: comp.productSpec || comp.spec || '',
      productUnit: comp.productUnit || comp.unit || '',
      quantity: (comp.quantity ?? 1) * (Number(formData.productRows[0]?.quantity) || 1),
      unitCost: comp.unitCost ?? 0,
    }))
    formData.materialRows.forEach((r: any) => { r.cost = (Number(r.quantity) || 0) * (Number(r.unitCost) || 0) })
  } catch {
    message.warning('加载BOM组件明细失败')
    formData.materialRows = []
  }
}

// ── 计算成本 / 金额 ──
function recalcTotal() {
  // 本单金额由 totalCost 计算属性联动
}
function handleCalcCost() {
  const total = materialCostTotal.value
  const rows = formData.productRows
  if (rows.length > 0) {
    const qty = Number(rows[0].quantity) || 1
    const unitCost = qty > 0 ? total / qty : 0
    rows[0].unitCost = Math.round(unitCost * 100) / 100
    rows[0].cost = (Number(rows[0].quantity) || 0) * rows[0].unitCost
  }
  message.success(`计算成本完成，本单金额 ¥${total.toFixed(2)}`)
}

// ── 校验 / 入参 ──
function validate(): boolean {
  if (!formData.outWarehouseId) { message.warning('请选择成品仓库'); return false }
  if (!formData.inWarehouseId) { message.warning('请选择原料仓库'); return false }
  if (!formData.handlerName) { message.warning('请输入经手人'); return false }
  if (!formData.splitDate) { message.warning('请选择单据日期'); return false }
  if (formData.productRows.length === 0 || !formData.productRows[0]?.productId) { message.warning('成品详情不能为空'); return false }
  if (formData.materialRows.length === 0) { message.warning('原料详情不能为空'); return false }
  if (formData.materialRows.some((r: any) => !r.productId)) { message.warning('原料明细存在未选择商品的行'); return false }
  return true
}

function buildPayload(status?: number) {
  const productRow = formData.productRows[0] || {}
  const items = formData.materialRows
    .filter((r: any) => r.productId)
    .map((r: any) => ({
      productId: r.productId,
      productCode: r.productCode || undefined,
      productName: r.productName,
      productSpec: r.productSpec || undefined,
      productUnit: r.productUnit || undefined,
      barcode: r.barcode || undefined,
      model: r.model || undefined,
      origin: r.origin || undefined,
      brand: r.brand || undefined,
      region: r.region || undefined,
      location: r.location || undefined,
      batchCode: r.batchCode || undefined,
      batchNo: r.batchNo || undefined,
      productionDate: r.productionDate || undefined,
      shelfLife: r.shelfLife || undefined,
      expiryDate: r.expiryDate || undefined,
      conversionRelation: r.conversionRelation || undefined,
      conversionResult: r.conversionResult ?? 0,
      pieceQuantity: r.pieceQuantity || 0,
      bigPack: r.bigPack || 0,
      midPack: r.midPack || 0,
      smallPack: r.smallPack || 0,
      smallUnit: r.smallUnit || undefined,
      smallUnitQuantity: r.smallUnitQuantity || 0,
      smallUnitPrice: r.smallUnitPrice || 0,
      wholesalePrice: r.wholesalePrice ?? 0,
      retailPrice: r.retailPrice ?? 0,
      unitCost: r.unitCost || 0,
      availableStock: r.availableStock ?? 0,
      availableStockConverted: r.availableStockConverted ?? 0,
      image: r.image || undefined,
      weight: r.weight ?? 0,
      volume: r.volume ?? 0,
      itemExtNum1: r.itemExtNum1 ?? 0,
      itemExtNum2: r.itemExtNum2 ?? 0,
      itemExtNum3: r.itemExtNum3 ?? 0,
      itemExtText1: r.itemExtText1 || undefined,
      itemExtText2: r.itemExtText2 || undefined,
      remark: r.remark || undefined,
    }))
  return {
    bomId: formData.bomId,
    inWarehouseId: formData.inWarehouseId,
    inWarehouseName: formData.inWarehouseName,
    outWarehouseId: formData.outWarehouseId,
    outWarehouseName: formData.outWarehouseName,
    handlerName: formData.handlerName,
    deptName: formData.deptName || undefined,
    splitDate: formData.splitDate,
    totalCost: totalCost.value,
    summary: formData.summary || undefined,
    creatorName: formData.creatorName || currentUserName.value,
    remark: formData.remark || undefined,
    productId: productRow.productId,
    productCode: productRow.productCode,
    productName: productRow.productName,
    productSpec: productRow.productSpec,
    productUnit: productRow.productUnit,
    splitQuantity: Number(productRow.quantity) || 1,
    status: status ?? formData.status,
    items,
  }
}

// ── 保存/提交/审批/执行 ──
const saving = ref(false)
const submitting = ref(false)
const approving = ref(false)
const rejecting = ref(false)
const executing = ref(false)

async function handleSaveDraft() {
  if (!validate()) return
  saving.value = true
  try {
    if (formData.id) {
      await stockSplitApi.update(formData.id, buildPayload(0))
    } else {
      const res: any = await stockSplitApi.create(buildPayload(0))
      formData.id = res?.data?.id || res?.id
    }
    message.success('拆分单草稿已保存')
    handleBack()
  } catch (err: any) {
    message.error(err?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleSubmit() {
  if (!validate()) return
  if (stockValid.value === false) {
    Modal.confirm({
      title: '库存不足',
      content: '部分成品库存不足，确认仍要提交审批吗？',
      okText: '确认提交',
      cancelText: '取消',
      onOk: async () => doSubmit(),
    })
  } else {
    doSubmit()
  }
}

async function doSubmit() {
  submitting.value = true
  try {
    if (formData.id) {
      await stockSplitApi.update(formData.id, buildPayload(1))
      await stockSplitApi.submit(formData.id)
    } else {
      const res: any = await stockSplitApi.create(buildPayload(1))
      formData.id = res?.data?.id || res?.id
      if (formData.id) await stockSplitApi.submit(formData.id)
    }
    message.success('拆分单已提交审批')
    handleBack()
  } catch (err: any) {
    message.error(err?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

function handleApprove() {
  if (!formData.id) return
  Modal.confirm({
    title: '审批确认',
    content: '确认审批通过该拆分单吗？',
    okText: '确认',
    onOk: async () => {
      approving.value = true
      try {
        await stockSplitApi.approve(formData.id)
        message.success('审批通过')
        handleBack()
      } catch (err: any) {
        message.error(err?.message || '审批失败')
      } finally { approving.value = false }
    },
  })
}

function handleReject() {
  if (!formData.id) return
  Modal.confirm({
    title: '拒绝确认',
    content: '确认拒绝该拆分单吗？',
    okText: '确认拒绝',
    okButtonProps: { danger: true },
    onOk: async () => {
      rejecting.value = true
      try {
        await stockSplitApi.reject(formData.id, '审批拒绝')
        message.success('已拒绝')
        handleBack()
      } catch (err: any) {
        message.error(err?.message || '拒绝失败')
      } finally { rejecting.value = false }
    },
  })
}

function handleExecute() {
  if (!formData.id) return
  Modal.confirm({
    title: '执行拆分',
    content: '确认执行该拆分单吗？执行后将：1. 按成品详情扣减成品仓库库存 2. 按原料详情增加原料仓库库存',
    okText: '确认执行',
    onOk: async () => {
      executing.value = true
      try {
        await stockSplitApi.execute(formData.id)
        message.success('拆分执行成功')
        handleBack()
      } catch (err: any) {
        message.error(err?.message || '执行失败')
      } finally { executing.value = false }
    },
  })
}

// ── 工具栏 ──
function handlePrint() {
  if (!formData.splitNo) { message.warning('请先保存单据后再打印'); return }
  window.print()
}
function handleHistory() {
  router.push('/erp/stock-split')
}
function handleExport() {
  const headers = ['商品', '货号', '数量', '成本单价', '成本金额', '备注']
  const rows = formData.materialRows
    .filter((r: any) => r.productName)
    .map((r: any) => [r.productName, r.productCode, r.quantity, r.unitCost, r.cost, r.remark])
  const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `拆分单原料明细_${formData.splitNo || new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}
function handleBack() {
  router.push('/erp/stock-split')
}

// ── 详情加载 ──
async function fetchDetail() {
  if (isNew.value) return
  try {
    const res: any = await stockSplitApi.getById(idParam.value)
    const data = res?.data || res
    if (data) {
      formData.id = data.id
      formData.splitNo = data.splitNo || ''
      formData.bomId = data.bomId
      formData.inWarehouseId = data.inWarehouseId ?? data.warehouseId
      formData.inWarehouseName = data.inWarehouseName || data.warehouseName || ''
      formData.outWarehouseId = data.outWarehouseId ?? data.warehouseId
      formData.outWarehouseName = data.outWarehouseName || data.warehouseName || ''
      formData.handlerName = data.handlerName || ''
      formData.deptName = data.deptName || ''
      formData.splitDate = data.splitDate || ''
      formData.summary = data.summary || ''
      formData.remark = data.remark || ''
      formData.status = data.status ?? 0
      formData.creatorName = data.creatorName || ''
      formData.createTime = data.createTime || ''
      formData.bookkeeperName = data.bookkeeperName || ''
      formData.bookkeepingTime = data.bookkeepingTime || ''
      formData.printCount = data.printCount ?? 0
      formData.printRecords = data.printRecords || ''
      formData.productRows = [emptyRow({
        productId: data.productId,
        productCode: data.productCode || '',
        productName: data.productName || '',
        productSpec: data.productSpec || '',
        productUnit: data.productUnit || '',
        quantity: data.splitQuantity ?? 1,
        unitCost: data.outputTotalCost && data.splitQuantity
          ? Math.round((data.outputTotalCost / data.splitQuantity) * 100) / 100
          : 0,
      })]
    }
    try {
      const itemsRes: any = await requestGet(`/erp/stock/split/${idParam.value}/items`)
      formData.materialRows = (itemsRes || []).map((item: any) => emptyRow({ ...item, _key: nextKey(), quantity: item.quantity ?? 1 }))
    } catch { formData.materialRows = [] }
  } catch {
    message.error('加载拆分单详情失败')
  }
}

function handleError(err: any) {
  console.warn('[拆分单] ErrorBoundary 捕获:', err)
}

function filterOption(input: string, option: any) {
  const text = option?.label || option?.children?.[0]?.children || option?.children || ''
  return text.toString().toLowerCase().includes(input.toLowerCase())
}

// ── 快捷键 ──
function onKeydown(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || (e.target as HTMLElement)?.isContentEditable) return
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') { e.preventDefault(); handleSaveDraft() }
  else if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') { e.preventDefault(); handleSubmit() }
}

onMounted(async () => {
  if (!formData.handlerName && isNew.value) formData.handlerName = currentUserName.value
  if (isNew.value && !formData.splitDate) {
    const now = new Date()
    formData.splitDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  }
  await Promise.all([loadBomTemplates(), loadProductOptions(), loadWarehouses()])
  await loadFormConfig()
  await fetchDetail()
  if (isNew.value) {
    if (formData.defaultOutWarehouseId) {
      formData.outWarehouseId = formData.defaultOutWarehouseId
      const w = warehouseOptions.value.find((x: any) => x.id === formData.defaultOutWarehouseId)
      formData.outWarehouseName = w?.warehouseName || w?.name || ''
    }
    if (formData.defaultInWarehouseId) {
      formData.inWarehouseId = formData.defaultInWarehouseId
      const w = warehouseOptions.value.find((x: any) => x.id === formData.defaultInWarehouseId)
      formData.inWarehouseName = w?.warehouseName || w?.name || ''
    }
    if (formData.defaultHandlerName) formData.handlerName = formData.defaultHandlerName
  }
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 8px;
  padding: 10px 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.toolbar-left { display: flex; align-items: center; gap: 10px; }
.doc-no { font-size: 15px; font-weight: 600; color: #262626; font-family: 'Consolas', 'Monaco', monospace; }

.panel {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.basic-info { background: #fff; }
.info-flow { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; }
.info-field { display: flex; flex-direction: column; gap: 4px; }
.info-label { font-size: 12px; color: #595959; }
.info-label.required::before { content: '*'; color: #ff4d4f; margin-right: 2px; }

.table-panel { display: flex; flex-direction: column; height: 380px; }
.table-panel .panel-title { flex-shrink: 0; }
.table-panel :deep(.bill-detail-table) { flex: 1; min-height: 0; }

.action-btn { color: #1890ff; padding: 0; font-size: 14px; }
.action-btn.danger { color: #ff4d4f; }

.remark-row { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.remark-label { font-size: 12px; color: #595959; white-space: nowrap; min-width: 60px; }
.remark-input { flex: 1; }

.doc-info-row { display: flex; gap: 26px; font-size: 13px; color: #606266; flex-wrap: wrap; }
.doc-info-row b { color: #303133; }

.total-bar {
  display: flex;
  justify-content: flex-end;
  align-items: baseline;
  gap: 10px;
  background: #fff;
  border-radius: 8px;
  padding: 14px 20px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.total-label { font-size: 14px; color: #606266; }
.total-value { font-size: 22px; font-weight: 700; color: #1890ff; }

.btn-row { margin-top: 8px; }
.shortcut {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.75);
  margin-left: 4px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 3px;
  padding: 0 4px;
}
.config-hint { font-size: 12px; color: #8c8c8c; margin-bottom: 8px; }
</style>
