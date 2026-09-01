<template>
  <div class="form-page-container" style="height:100%;display:flex;flex-direction:column;">
    <BillFormPage
      v-model="formData"
      :header="headerConfig"
      :basic-info-fields="basicInfoFields"
      :summary="summaryConfig"
      :footer="footerConfig"
      @action="handleAction"
      @field-change="handleFieldChange"
      @draft="handleSaveDraft"
      @submit="handleSubmit"
    >
      <!-- Zone 3: 原料构成明细表格 -->
      <template #detail-table="{ onExpandChange }">
        <BillDetailTable
          :columns="visibleDetailColumns"
          :data-source="formData.products"
          :max-height="tableMaxHeight"
          :summary-columns="tableSummaryColumns"
          @cell-change="handleCellChange"
          @expand-change="onExpandChange"
        >
          <template #actionCell="{ index }">
            <a-space :size="2">
              <a-button type="link" size="small" class="action-add-btn" @click="handleInsertProduct(index)">
                <PlusCircleOutlined />
              </a-button>
              <a-button type="link" size="small" class="action-del-btn" @click="handleRemoveProduct(index)">
                <MinusCircleOutlined />
              </a-button>
            </a-space>
          </template>
          <template #productCell="{ record, index }">
            <a-select
              v-model:value="record.productId"
              placeholder="搜索选择物料"
              show-search
              :filter-option="filterOption"
              style="width:100%"
              :loading="loadingOptions"
              size="small"
              @change="(val: number) => handleProductChange(val, index)"
            >
              <a-select-option v-for="p in optionRefs.products" :key="p.id" :value="p.id">
                {{ p.code ? p.code + ' - ' : '' }}{{ p.name }}
              </a-select-option>
            </a-select>
          </template>
          <template #imageCell="{ record }">
            <a-image
              v-if="record.imageUrl"
              :src="record.imageUrl"
              :width="36"
              :height="36"
              style="border-radius:4px;object-fit:cover;"
            />
          </template>
        </BillDetailTable>
      </template>

      <!-- Zone 4: 页脚附加信息（无单据编号/状态流） -->
      <template #bottom-extra>
        <div class="doc-info-row">
          <span class="doc-info-item">制单人 <a-tag color="blue">{{ currentUserName || '系统' }}</a-tag></span>
          <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
        </div>
      </template>
    </BillFormPage>

    <!-- ═══ 页面配置弹窗（模板头字段显隐 + 打印设置） ═══ -->
    <a-modal v-model:open="pageConfigOpen" title="配置" :footer="null" :width="720" centered>
      <a-tabs v-model:active-key="pageConfigActiveTab" class="page-config-tabs">
        <a-tab-pane key="fields" tab="页面配置">
          <div class="tab-tip">勾选后自动保存，该设置对所有操作员生效</div>
          <table class="page-config-table">
            <thead>
              <tr>
                <th style="width:40px">序号</th>
                <th>名称</th>
                <th style="width:120px">显示名</th>
                <th style="width:60px">显示</th>
                <th style="width:80px">回车键跳转</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(f, i) in pageFieldsConfig" :key="f.key">
                <td class="cell-center">{{ i + 1 }}</td>
                <td>{{ f.label }}</td>
                <td><a-input v-model:value="f.displayName" size="small" /></td>
                <td class="cell-center"><a-checkbox v-model:checked="f.visible" @change="savePageConfig" /></td>
                <td class="cell-center"><a-checkbox v-model:checked="f.enterJump" @change="savePageConfig" /></td>
              </tr>
            </tbody>
          </table>
          <div class="config-footer">
            <a-button @click="resetPageConfig">恢复默认值</a-button>
            <a-button type="primary" @click="pageConfigOpen = false">关闭</a-button>
          </div>
        </a-tab-pane>
        <a-tab-pane key="print" tab="打印设置">
          <div class="tab-tip">打印配置设置后只针对当前操作员有效</div>
          <a-checkbox v-model:checked="printConfig.alwaysLastTemplate" @change="savePageConfig">
            始终使用最后一次打印的模板，打印时不再选择
          </a-checkbox>
          <div class="config-footer">
            <a-button type="primary" @click="savePageConfig(true)">保存</a-button>
            <a-button @click="pageConfigOpen = false">取消</a-button>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-modal>

    <!-- ═══ 列配置弹窗（明细22列） ═══ -->
    <ColumnConfigPanel
      :open="columnConfigOpen"
      :settings-columns="settingsColumns"
      :is-locked-column="isLockedColumn"
      global-config-key="stock-bom-detail"
      @update:open="columnConfigOpen = $event"
      @change="onSettingChange"
      @reset="resetSettings"
      @drag-end="handleColumnDrag"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PlusCircleOutlined, MinusCircleOutlined, PrinterOutlined, ExportOutlined,
  TableOutlined, SettingOutlined,
} from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import { useColumnConfig, isLockedColumn } from '@/composables/useColumnConfig'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { stockBomApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'StockBomForm' })

const router = useRouter()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
const tableMaxHeight = ref(400)

// ═══ 页面配置：模板头字段显隐（存 localStorage） ═══
const PAGE_CONFIG_KEY = 'stock-bom-page-config'
const DEFAULT_PAGE_FIELDS = [
  { key: 'bomName', label: '模板名称', displayName: '模板名称', visible: true, enterJump: true },
  { key: 'productId', label: '成品名称', displayName: '成品名称', visible: true, enterJump: true },
  { key: 'productUnit', label: '成品单位', displayName: '成品单位', visible: true, enterJump: true },
  { key: 'outputQuantity', label: '成品数量', displayName: '成品数量', visible: true, enterJump: false },
  { key: 'taste', label: '口味', displayName: '口味', visible: false, enterJump: false },
  { key: 'model', label: '型号', displayName: '型号', visible: false, enterJump: false },
]
const pageFieldsConfig = ref<any[]>(JSON.parse(JSON.stringify(DEFAULT_PAGE_FIELDS)))
const printConfig = ref({ alwaysLastTemplate: false })
const pageConfigOpen = ref(false)
const pageConfigActiveTab = ref('fields')

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed.fields)) {
      pageFieldsConfig.value = DEFAULT_PAGE_FIELDS.map(df => {
        const saved = parsed.fields.find((f: any) => f.key === df.key)
        return saved ? { ...df, ...saved } : { ...df }
      })
    }
    if (parsed.printConfig) printConfig.value = { ...printConfig.value, ...parsed.printConfig }
  } catch { /* ignore */ }
}
function savePageConfig(close?: boolean) {
  localStorage.setItem(PAGE_CONFIG_KEY, JSON.stringify({
    fields: pageFieldsConfig.value,
    printConfig: printConfig.value,
  }))
  if (close) pageConfigOpen.value = false
}
function resetPageConfig() {
  pageFieldsConfig.value = JSON.parse(JSON.stringify(DEFAULT_PAGE_FIELDS))
  savePageConfig()
}
loadPageConfig()

// ═══ 保存/提交（保存即生效，无状态流） ═══
const {
  formData,
  loadingOptions,
  saving,
  optionRefs,
  filterOption,
  effectiveMode,
  handleAddProduct,
  handleRemoveProduct,
  handleProductChange: baseProductChange,
  handleFieldChange: baseFieldChange,
  handleSaveDraft,
  handleSubmit,
  totalQuantity,
} = useBillForm({
  billPrefix: 'BOM',
  api: {
    create: (data: any) => stockBomApi.create(data),
    update: (id: number, data: any) => stockBomApi.update(id, data),
    getById: async (id: number) => {
      const res: any = await stockBomApi.getById(id)
      const data = res?.data || res || {}
      let rawItems: any[] = []
      try {
        const itemsRes: any = await stockBomApi.getItems(id)
        rawItems = itemsRes?.data || []
      } catch { rawItems = [] }
      return {
        ...data,
        items: rawItems.map((d: any, i: number) => ({
          ...d,
          id: d.id || `detail-${i}`,
          productCode: d.productCode || '',
          productName: d.productName || '',
          barcode: d.barcode || '',
          productSpec: d.productSpec || '',
          productUnit: d.productUnit || '',
          model: d.model || '',
          origin: d.origin || '',
          brand: d.brand || '',
          imageUrl: d.imageUrl || '',
          quantity: d.quantity ?? 1,
          unitCost: d.unitCost ?? 0,
          cost: d.cost ?? (d.quantity ?? 0) * (d.unitCost ?? 0),
          smallUnit: d.smallUnit || '',
          smallUnitQty: d.smallUnitQty ?? 0,
          smallUnitPrice: d.smallUnitPrice ?? 0,
          remark: d.remark || '',
          extNum1: d.extNum1 ?? 0,
          extNum2: d.extNum2 ?? 0,
          extNum3: d.extNum3 ?? 0,
          extText4: d.extText4 || '',
          extText5: d.extText5 || '',
        })),
      }
    },
  },
  redirectPath: '/erp/stock-bom',
  optionTypes: ['products'],
  fields: [
    { key: 'bomName', label: '模板名称', type: 'input', required: true },
    { key: 'productId', label: '成品名称', type: 'select', required: true },
    { key: 'productUnit', label: '成品单位', type: 'input', required: true },
    { key: 'outputQuantity', label: '成品数量', type: 'number', required: true },
    { key: 'taste', label: '口味', type: 'input' },
    { key: 'model', label: '型号', type: 'input' },
  ],
  productDefaults: {
    productCode: '', productName: '', barcode: '', productSpec: '', productUnit: '',
    model: '', origin: '', brand: '', imageUrl: '',
    quantity: 1, unitCost: 0, cost: 0,
    smallUnit: '', smallUnitQty: 0, smallUnitPrice: 0,
    remark: '', extNum1: 0, extNum2: 0, extNum3: 0, extText4: '', extText5: '',
  },
  transformPayload: (fd): any => {
    const validItems = fd.products.filter((p: any) => p.productId != null)
    if (validItems.length === 0) {
      throw new Error('请添加原料明细并选择商品')
    }
    return {
      bomName: fd.bomName,
      productId: fd.productId,
      productUnit: fd.productUnit,
      outputQuantity: fd.outputQuantity,
      taste: fd.taste || undefined,
      model: fd.model || undefined,
      remark: fd.remark || undefined,
      items: validItems.map((p: any) => ({
        productId: p.productId,
        productCode: p.productCode || p.code || '',
        productName: p.productName || p.name || '',
        barcode: p.barcode || '',
        productSpec: p.productSpec || p.specification || '',
        productUnit: p.productUnit || p.unit || '',
        model: p.model || '',
        origin: p.origin || '',
        brand: p.brand || '',
        imageUrl: p.imageUrl || p.image || '',
        quantity: p.quantity,
        unitCost: p.unitCost,
        smallUnit: p.smallUnit || '',
        smallUnitQty: p.smallUnitQty || 0,
        smallUnitPrice: p.smallUnitPrice || 0,
        remark: p.remark || undefined,
        extNum1: p.extNum1 ?? 0,
        extNum2: p.extNum2 ?? 0,
        extNum3: p.extNum3 ?? 0,
        extText4: p.extText4 || '',
        extText5: p.extText5 || '',
      })),
    }
  },
})

// 初始化模板头字段默认值
if (formData.bomName === undefined) formData.bomName = ''
if (formData.productUnit === undefined) formData.productUnit = ''
if (!formData.outputQuantity) formData.outputQuantity = 1
if (formData.taste === undefined) formData.taste = ''
if (formData.model === undefined) formData.model = ''
if (formData.remark === undefined) formData.remark = ''
if (formData.createTime === undefined) formData.createTime = ''

// ═══ 计算：合计成本（原料） ═══
const totalCost = computed(() =>
  formData.products.reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitCost || 0), 0))

const isEdit = computed(() => effectiveMode.value === 'edit')

// ═══ 页眉配置：标题 + 列配置/页面配置/打印/导出 ═══
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: isEdit.value ? '编辑生产模板' : '创建生产模板',
  orderNo: formData.orderNo,
  showAttachment: false,
  actions: [
    { key: 'columnConfig', label: '', icon: TableOutlined },
    { key: 'pageConfig', label: '', icon: SettingOutlined },
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'export', label: '导出', icon: ExportOutlined },
  ],
}))

// ═══ 基本信息字段（根据页面配置过滤） ═══
const FIELD_DEFS: Record<string, any> = {
  bomName: { key: 'bomName', label: '模板名称', type: 'input', required: true, inlineLabel: true, width: 260 },
  productId: {
    key: 'productId', label: '成品名称', type: 'select', required: true, inlineLabel: true, width: 260,
    options: optionRefs.products.map((p: any) => ({ label: `${p.code ? p.code + ' - ' : ''}${p.name}`, value: p.id })),
    searchBtn: '+Q', loading: loadingOptions.value,
  },
  productUnit: { key: 'productUnit', label: '成品单位', type: 'input', required: true, inlineLabel: true, width: 160 },
  outputQuantity: { key: 'outputQuantity', label: '成品数量', type: 'number', required: true, inlineLabel: true, width: 140, precision: 2, min: 0 },
  taste: { key: 'taste', label: '口味', type: 'input', inlineLabel: true, width: 160 },
  model: { key: 'model', label: '型号', type: 'input', inlineLabel: true, width: 160 },
}

const basicInfoFields = computed<BasicInfoField[]>(() =>
  pageFieldsConfig.value.filter(f => f.visible).map(f => {
    const def = FIELD_DEFS[f.key]
    return { ...def, label: f.displayName || f.label }
  }))

// ═══ 摘要 ═══
const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '物料种类', value: formData.products.filter((p: any) => p.productId != null).length },
  { label: '物料总用量', value: totalQuantity.value },
  { label: '成品数量', value: formData.outputQuantity ?? 1 },
  { label: '原料合计成本', value: totalCost.value.toFixed(2), divider: true },
])

// ═══ 页脚：保存(Ctrl+Enter)，无草稿 ═══
const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '原料合计成本',
  amountValue: `¥${totalCost.value.toFixed(2)}`,
  amountHighlight: true,
  primaryBtnText: '保存',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══ 明细22列（列配置控制显隐） ═══
const detailColumnDefs: any[] = [
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 60, fixed: 'left' },
  { key: 'image', title: '图片', type: 'slot', slotName: 'imageCell', width: 60, defaultHidden: true },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productCell', width: 220 },
  { key: 'productCode', title: '货号', type: 'input', width: 110 },
  { key: 'barcode', title: '条码', type: 'input', width: 110 },
  { key: 'productSpec', title: '规格', type: 'input', width: 100, defaultHidden: true },
  { key: 'model', title: '型号', type: 'input', width: 100, defaultHidden: true },
  { key: 'origin', title: '产地', type: 'input', width: 100, defaultHidden: true },
  { key: 'brand', title: '品牌', type: 'input', width: 100, defaultHidden: true },
  { key: 'productUnit', title: '计价单位', type: 'input', width: 90 },
  { key: 'quantity', title: '数量', type: 'number', width: 90, precision: 2, min: 0 },
  { key: 'unitCost', title: '成本均价', type: 'number', width: 100, precision: 2, min: 0 },
  { key: 'cost', title: '成本金额', type: 'number', width: 110, precision: 2, readonly: true },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 90, defaultHidden: true },
  { key: 'smallUnitQty', title: '小单位数量', type: 'number', width: 110, precision: 2, defaultHidden: true },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 110, precision: 2, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
  { key: 'extNum1', title: '单据自定义1(数字)', type: 'number', width: 110, defaultHidden: true },
  { key: 'extNum2', title: '单据自定义2(数字)', type: 'number', width: 110, defaultHidden: true },
  { key: 'extNum3', title: '单据自定义3(数字)', type: 'number', width: 110, defaultHidden: true },
  { key: 'extText4', title: '单据自定义4(文本)', type: 'input', width: 120, defaultHidden: true },
  { key: 'extText5', title: '单据自定义5(文本)', type: 'input', width: 120, defaultHidden: true },
]

const {
  showPanel: columnConfigOpen,
  visibleColumns: visibleDetailColumns,
  settingsColumns,
  onSettingChange,
  resetSettings,
  handleColumnDrag,
} = useColumnConfig(detailColumnDefs, 'stock-bom-detail-columns')

const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'cost', value: totalCost.value.toFixed(2), highlight: true },
])

// ═══ 事件处理 ═══
function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
  if (fieldKey === 'productId') {
    const p = optionRefs.products.find((x: any) => x.id === val)
    if (p) {
      formData.productName = p.name || ''
      formData.productUnit = p.unit || ''
      formData.productCode = p.code || ''
      formData.productSpec = p.specification || ''
      formData.model = p.model || ''
      formData.barcode = p.barcode || ''
      formData.origin = p.origin || ''
      formData.brand = p.brand || ''
    }
  }
}

function handleProductChange(val: number, index: number) {
  baseProductChange(val, index)
  const p = optionRefs.products.find((x: any) => x.id === val)
  const row = formData.products[index]
  if (p && row) {
    row.productCode = p.code || ''
    row.productName = p.name || ''
    row.barcode = p.barcode || ''
    row.productSpec = p.specification || ''
    row.productUnit = p.unit || ''
    row.model = p.model || ''
    row.origin = p.origin || ''
    row.brand = p.brand || ''
    row.imageUrl = p.image || ''
    if (!row.unitCost) row.unitCost = p.costPrice || p.purchasePrice || 0
    row.cost = (row.quantity || 0) * (row.unitCost || 0)
  }
}

function handleInsertProduct(index: number) {
  handleAddProduct()
  const item = formData.products.pop()
  if (item) formData.products.splice(index + 1, 0, item)
}

function handleCellChange(record: any, fieldKey: string, _value: any) {
  if (['quantity', 'unitCost'].includes(fieldKey)) {
    record.cost = (record.quantity || 0) * (record.unitCost || 0)
  }
}

function handleAction(actionKey: string) {
  switch (actionKey) {
    case 'columnConfig':
      columnConfigOpen.value = true
      break
    case 'pageConfig':
      pageConfigOpen.value = true
      break
    case 'print':
      window.print()
      break
    case 'export':
      handleExport()
      break
  }
}

function handleExport() {
  const esc = (v: any) => `"${(v ?? '').toString().replace(/"/g, '""')}"`
  const head = [
    ['模板名称', formData.bomName || ''],
    ['成品名称', formData.productName || ''],
    ['成品单位', formData.productUnit || ''],
    ['成品数量', formData.outputQuantity ?? ''],
    ['口味', formData.taste || ''],
    ['型号', formData.model || ''],
  ]
  const itemHeaders = ['序号', '商品名称', '货号', '条码', '规格', '型号', '产地', '品牌', '计价单位', '数量', '成本均价', '成本金额', '备注']
  const itemRows = formData.products.map((p: any, i: number) => [
    i + 1, p.productName || '', p.productCode || '', p.barcode || '', p.productSpec || '',
    p.model || '', p.origin || '', p.brand || '', p.productUnit || '',
    p.quantity ?? '', p.unitCost ?? '', ((p.quantity || 0) * (p.unitCost || 0)).toFixed(2), p.remark || '',
  ])
  const lines = [
    ...head.map(r => r.map(esc).join(',')),
    '',
    itemHeaders.join(','),
    ...itemRows.map(r => r.map(esc).join(',')),
  ]
  const blob = new Blob([`\uFEFF${lines.join('\n')}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  const d = new Date()
  const ts = `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}${String(d.getDate()).padStart(2, '0')}_${String(d.getHours()).padStart(2, '0')}${String(d.getMinutes()).padStart(2, '0')}`
  a.download = `生产模板_${ts}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  if (effectiveMode.value !== 'edit' && formData.products.length === 0) {
    for (let i = 0; i < 5; i++) handleAddProduct()
  }
  nextTick(() => {
    tableMaxHeight.value = Math.max(200, window.innerHeight - 420)
  })
})
</script>

<style scoped>
.action-add-btn { color: #1890ff; padding: 0; font-size: 14px; }
.action-del-btn { color: #ff4d4f; padding: 0; font-size: 14px; }
.doc-info-row { display: flex; align-items: center; gap: 16px; padding: 6px 0; font-size: 12px; color: #8c8c8c; border-top: 1px solid #f0f0f0; flex-wrap: wrap; }
.page-config-tabs .tab-tip { font-size: 12px; color: #fa8c16; margin-bottom: 12px; }
.page-config-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.page-config-table thead th { background: #fafafa; padding: 8px 6px; border-bottom: 1px solid #e8e8e8; text-align: left; font-weight: 500; font-size: 12px; color: #666; }
.page-config-table tbody td { padding: 6px; border-bottom: 1px solid #f5f5f5; vertical-align: middle; }
.page-config-table tbody tr:hover { background: #fafafa; }
.cell-center { text-align: center; }
.config-footer { display: flex; justify-content: flex-end; gap: 8px; padding: 12px 0 0; }
</style>
