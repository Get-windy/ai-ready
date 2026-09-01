<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <a-alert
        v-if="isLocked"
        :message="`当前单据状态为「${statusText}」，已盘点处理，内容不可修改`"
        type="warning"
        show-icon
        banner
        style="flex-shrink:0"
      />

      <BillFormPage
        v-model="formData"
        :header="headerConfig"
        :basic-info-fields="basicInfoFields"
        :summary="summaryConfig"
        :footer="footerConfig"
        @action="handleAction"
        @field-change="handleFieldChange"
        @search-btn="handleSearchBtn"
        @draft="doSave"
        @submit="doProcess"
      >
        <!-- ═══ Zone 3: 快速录入 + 工具行 + 盘点明细表格 ═══ -->
        <template #detail-table="{ onExpandChange }">
          <div class="stocktake-table-block">
            <!-- 商品快速录入行 -->
            <div class="quick-entry-row">
              <span class="quick-entry-label">商品：</span>
              <a-input
                v-model:value="quickEntry.keyword"
                placeholder="条码 / 名称 / 编码"
                size="small"
                class="quick-entry-input"
                :disabled="isLocked"
                @press-enter="handleQuickAdd"
                ref="quickEntryInputRef"
              />
              <span class="quick-entry-label">数量：</span>
              <a-input-number
                v-model:value="quickEntry.quantity"
                :min="0"
                :precision="2"
                size="small"
                class="quick-entry-qty"
                :disabled="isLocked"
              />
              <a-button type="primary" size="small" :disabled="isLocked" @click="handleQuickAdd">添加</a-button>
            </div>

            <!-- 表头工具行 -->
            <div class="table-tool-row">
              <div class="tool-left">
                <a-checkbox v-model:checked="scanMode" :disabled="isLocked">扫描枪录入</a-checkbox>
                <div class="tool-search">
                  <span class="tool-label">快速定位</span>
                  <a-input v-model:value="quickLocationKeyword" placeholder="输入商品名称 / 条码 / 货号" size="small" allow-clear />
                </div>
                <div class="tool-status">
                  <span class="tool-label">盈亏状态</span>
                  <a-select v-model:value="diffFilter" size="small" style="width: 120px">
                    <a-select-option value="">全部</a-select-option>
                    <a-select-option value="profit">盘盈</a-select-option>
                    <a-select-option value="loss">盘亏</a-select-option>
                    <a-select-option value="none">无差异</a-select-option>
                  </a-select>
                </div>
              </div>
              <div class="tool-right">
                <a-button size="small" :disabled="isLocked" @click="handleRefreshStock"><ReloadOutlined /> 刷新库存</a-button>
              </div>
            </div>

            <BillDetailTable
              :columns="detailColumns"
              :data-source="filteredProducts"
              :max-height="tableMaxHeight"
              :summary-columns="tableSummaryColumns"
              :storage-key="'stock-take-form-columns'"
              @cell-change="handleCellChange"
              @expand-change="onExpandChange"
            >
              <template #actionCell="{ index, empty }">
                <template v-if="!empty">
                  <a-space :size="2">
                    <a-button type="link" size="small" class="action-add-btn" @click="handleInsertProduct(index)"><PlusCircleOutlined /></a-button>
                    <a-button type="link" size="small" class="action-del-btn" @click="handleRemoveProduct(index)"><MinusCircleOutlined /></a-button>
                  </a-space>
                </template>
                <template v-else>
                  <a-space :size="2">
                    <a-button type="link" size="small" class="action-add-btn" @click="handleAddProduct"><PlusCircleOutlined /></a-button>
                    <a-button type="link" size="small" class="action-del-btn" disabled><MinusCircleOutlined /></a-button>
                  </a-space>
                </template>
              </template>
              <template #productCell="{ record, index }">
                <a-select
                  v-model:value="record.productId"
                  placeholder="搜索选择商品"
                  show-search
                  :filter-option="filterOption"
                  style="width:100%"
                  :loading="loadingOptions"
                  size="small"
                  :disabled="isLocked"
                  @change="(val: number) => handleProductChange(val, index)"
                >
                  <a-select-option v-for="p in optionRefs.products" :key="p.id" :value="p.id">{{ p.name }}</a-select-option>
                </a-select>
              </template>
              <template #diffCell="{ record }">
                <span
                  :class="{
                    'positive': (record.diffQuantity ?? 0) > 0,
                    'negative': (record.diffQuantity ?? 0) < 0,
                  }"
                >{{ formatQuantity(record.diffQuantity) }}</span>
              </template>
              <template #checkStatusCell="{ record }">
                <a-tag :color="getCheckStatusColor(record.checkStatus)">{{ getCheckStatusText(record.checkStatus) }}</a-tag>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <!-- ═══ Zone 4: 备注 + 单据信息 ═══ -->
        <template #bottom-extra>
          <div class="remark-section">
            <div class="remark-row">
              <span class="remark-label">单据备注</span>
              <a-input v-model:value="formData.remark" size="small" class="remark-input" placeholder="请输入单据备注" :disabled="isLocked" />
            </div>
            <div class="remark-row">
              <span class="remark-label">摘要</span>
              <a-input v-model:value="formData.summary" size="small" class="remark-input" placeholder="请输入摘要" :disabled="isLocked" />
            </div>
          </div>
          <div class="doc-info-row">
            <span class="doc-info-item">单据状态 <a-tag :color="statusColor">{{ statusText }}</a-tag></span>
            <span class="doc-info-item">盘点方式 <a-tag>{{ checkMethodText }}</a-tag></span>
            <span class="doc-info-item">经手人 <a-tag color="blue">{{ formData.handlerName || currentUserName || '系统' }}</a-tag></span>
            <span class="doc-info-item">制单人 <a-tag color="blue">{{ formData.creatorName || currentUserName || '系统' }}</a-tag></span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
            <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
            <span v-if="formData.processResult" class="doc-info-item">处理结果 <a-tag color="cyan">{{ formData.processResult }}</a-tag></span>
          </div>
        </template>
      </BillFormPage>

      <!-- ═══ 配置弹窗（页面配置/录单默认值/打印设置） ═══ -->
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
                  <a-checkbox :checked="pageConfig[record.key]?.visible !== false" @change="(e: any) => handlePageConfigFieldVisibleChange(record.key, e.target.checked)" />
                </template>
                <template v-if="column.key === 'enterJump'">
                  <a-checkbox :checked="pageConfig[record.key]?.enterJump" @change="(e: any) => handlePageConfigEnterJumpChange(record.key, e.target.checked)" />
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="defaultValues" tab="录单默认值">
            <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-form-item label="默认仓库">
                <a-select v-model:value="formData.defaultWarehouseId" show-search size="small" style="width:100%" :loading="loadingOptions" :options="(optionRefs.warehouses||[]).map((w:any)=>({label:w.warehouseName||w.name,value:w.id}))" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认经手人">
                <a-select v-model:value="formData.defaultHandlerId" show-search size="small" style="width:100%" :loading="loadingOptions" :options="(optionRefs.users||[]).map((u:any)=>({label:u.name,value:u.id}))" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认盘点方式">
                <a-select v-model:value="formData.defaultCheckMethod" size="small" style="width:100%" :options="CHECK_METHOD_OPTIONS" @change="saveFormConfig" />
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
import { computed, ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import {
  PrinterOutlined, ClockCircleOutlined, SettingOutlined, ImportOutlined,
  DownloadOutlined, ReloadOutlined, PlusCircleOutlined, MinusCircleOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { stockTakeApi, userPageConfigApi } from '@/api/erp'
import optionsApi from '@/api/options'
import { generateCodeAsync } from '@/utils/codeGenerator'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'StockTakeForm' })

const router = useRouter()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
const currentUserId = computed(() => userStore?.userInfo?.id)
const tableMaxHeight = ref(400)
const savedId = ref<number | undefined>(undefined)

// ═══ 盘点方式/盘点类型字典 ═══
const CHECK_METHOD_MAP: Record<number, string> = { 1: '手工录入', 2: '扫码盘点', 3: '快速盘点' }
const CHECK_METHOD_OPTIONS = Object.entries(CHECK_METHOD_MAP).map(([k, v]) => ({ label: v, value: Number(k) }))
const checkMethodText = computed(() => CHECK_METHOD_MAP[formData.checkMethod] || '手工录入')

const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '已保存', color: 'blue' },
  2: { text: '已盘点', color: 'green' },
}
const currentStatus = computed(() => Number(formData.status ?? 0))
const statusText = computed(() => STATUS_MAP[currentStatus.value]?.text || '草稿')
const statusColor = computed(() => STATUS_MAP[currentStatus.value]?.color || 'default')
const isLocked = computed(() => currentStatus.value === 2)

// ═══ 表单数据 ═══
const formData = reactive<Record<string, any>>({
  id: undefined, orderNo: '', stockTakeNo: '', date: dayjs().format('YYYY-MM-DD'),
  warehouseId: undefined, warehouseName: '', handlerId: undefined, handlerName: '',
  deptId: undefined, deptName: '', checkMethod: 1, checkType: 1, regionName: '',
  summary: '', remark: '', attachment: '', createTime: '', creatorName: '', printCount: 0,
  processResult: '', products: [] as any[],
  defaultWarehouseId: undefined, defaultHandlerId: undefined, defaultCheckMethod: 1,
  printTemplate: 'standard', printCopies: 1, printPaperSize: 'A4',
})

// ═══ 下拉选项 ═══
const loadingOptions = ref(false)
const optionRefs = reactive<Record<string, any[]>>({ warehouses: [], users: [], products: [] })
const filterOption = (input: string, option: any) => {
  const text = option?.label || option?.name || ''
  return text.toString().toLowerCase().includes(input.toLowerCase())
}
const departmentOptions = ref<any[]>([])

// ═══ 库存映射（按仓库加载） ═══
const stockMap = ref<Record<number, { stockQuantity: number; availableStock: number }>>({})

async function loadOptions() {
  loadingOptions.value = true
  try {
    const [warehouses, users, products] = await Promise.all([
      optionsApi.getWarehouses().catch(() => []),
      optionsApi.getUsers().catch(() => []),
      optionsApi.getProducts().catch(() => []),
    ])
    optionRefs.warehouses = warehouses || []
    optionRefs.users = users || []
    optionRefs.products = products || []
  } finally {
    loadingOptions.value = false
  }
}

async function loadStockMap(warehouseId?: number) {
  if (!warehouseId) return
  try {
    const res: any = await stockTakeApi.uncheckedProducts({ checkWarehouseId: warehouseId, pageSize: 9999 })
    const body = (res as any)?.data ?? res
    const list = Array.isArray(body) ? body : []
    const map: Record<number, { stockQuantity: number; availableStock: number }> = {}
    list.forEach((p: any) => { if (p.productId != null) map[p.productId] = { stockQuantity: p.stockQuantity ?? 0, availableStock: p.availableStock ?? 0 } })
    stockMap.value = map
  } catch {
    stockMap.value = {}
  }
}

// ═══ 单据号 ═══
async function generateBillNo() {
  try {
    formData.orderNo = await generateCodeAsync('KCPDD', '/erp/stock/take/next-no')
  } catch {
    formData.orderNo = `KCPDD-${dayjs().format('YYYYMMDD')}-001`
  }
}

// ═══ 明细行 ═══
function newProductDefaults() {
  return {
    id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
    productId: undefined, itemCode: '', productCode: '', productName: '', barcode: '',
    itemSpec: '', specification: '', model: '', origin: '', brand: '', region: '', location: '',
    unit: '', itemUnit: '', productUnit: '', image: '',
    stockQuantity: 0, conversionResult: 0, checkQuantity: 0,
    checkQuantityConversionResult: 0, pieceQuantity: 0, diffQuantity: 0,
    productionDate: '', conversionRelation: '', shelfLife: '', expiryDate: '', batchCode: '',
    bigPack: 0, midPack: 0, smallPack: 0, checkStatus: 1, costPrice: 0,
    diffConversionResult: 0, diffAmount: 0,
    itemExtNum1: 0, itemExtNum2: 0, itemExtNum3: 0, itemExtText1: '', itemExtText2: '',
    remark: '',
  }
}
function handleAddProduct() { formData.products.push(newProductDefaults()) }
function handleInsertProduct(index: number) {
  handleAddProduct()
  const item = formData.products.pop()
  if (item) formData.products.splice(index + 1, 0, item)
}
function handleRemoveProduct(index: number) { formData.products.splice(index, 1) }

function handleProductChange(val: number, index: number) {
  const p = optionRefs.products.find((x: any) => x.id === val)
  if (p && formData.products[index]) {
    const row = formData.products[index]
    row.productCode = p.code || ''
    row.itemCode = p.code || ''
    row.productName = p.name || ''
    row.barcode = p.barcode || ''
    row.specification = p.specification || ''
    row.itemSpec = p.specification || ''
    row.model = p.model || ''
    row.origin = p.origin || ''
    row.brand = p.brand || ''
    row.unit = p.unit || ''
    row.itemUnit = p.unit || ''
    row.productUnit = p.unit || ''
    row.costPrice = p.costPrice ?? p.purchasePrice ?? 0
    row.productId = val
    const stock = stockMap.value[val]
    if (stock) {
      row.stockQuantity = stock.stockQuantity
      row.checkQuantity = row.checkQuantity || stock.stockQuantity
    } else if (p.stock != null) {
      row.stockQuantity = p.stock
      row.checkQuantity = row.checkQuantity || p.stock
    }
    recalcRow(row)
  }
}

function recalcRow(row: any) {
  const stock = Number(row.stockQuantity ?? 0)
  const check = Number(row.checkQuantity ?? 0)
  row.diffQuantity = check - stock
  row.diffAmount = row.diffQuantity * (Number(row.costPrice) || 0)
  if (row.checkQuantity !== 0) row.checkStatus = 2
}

function handleCellChange(record: any, fieldKey: string, _value: any) {
  if (['checkQuantity', 'stockQuantity', 'costPrice'].includes(fieldKey)) recalcRow(record)
}

// ═══ 快速录入 ═══
const quickEntryInputRef = ref()
const quickEntry = reactive({ keyword: '', quantity: 1 })
function handleQuickAdd() {
  const kw = quickEntry.keyword.trim()
  if (!kw) { message.warning('请输入条码/名称/编码'); return }
  const p = optionRefs.products.find((x: any) =>
    (x.barcode && x.barcode === kw) || (x.code && x.code === kw) || (x.name && x.name === kw)
  )
  if (!p) {
    // 模糊匹配
    const fuzzy = optionRefs.products.find((x: any) =>
      (x.name && x.name.includes(kw)) || (x.code && x.code.includes(kw)) || (x.barcode && x.barcode.includes(kw))
    )
    if (!fuzzy) { message.warning('未匹配到商品，请从下拉中选择'); return }
    return addQuickRow(fuzzy)
  }
  addQuickRow(p)
}

function addQuickRow(p: any) {
  const existing = formData.products.find((x: any) => x.productId === p.id && x.checkQuantity !== null)
  const row = existing || (formData.products.push(newProductDefaults()), formData.products[formData.products.length - 1])
  if (!existing) {
    row.productCode = p.code || ''; row.itemCode = p.code || ''; row.productName = p.name || ''
    row.barcode = p.barcode || ''; row.specification = p.specification || ''; row.itemSpec = p.specification || ''
    row.model = p.model || ''; row.origin = p.origin || ''; row.brand = p.brand || ''
    row.unit = p.unit || ''; row.itemUnit = p.unit || ''; row.productUnit = p.unit || ''
    row.costPrice = p.costPrice ?? p.purchasePrice ?? 0; row.productId = p.id
    const stock = stockMap.value[p.id]
    if (stock) { row.stockQuantity = stock.stockQuantity }
    else if (p.stock != null) { row.stockQuantity = p.stock }
    row.checkQuantity = row.stockQuantity
  } else {
    row.checkQuantity = Number(row.checkQuantity || 0) + Number(quickEntry.quantity || 1)
  }
  recalcRow(row)
  quickEntry.keyword = ''
  quickEntry.quantity = 1
  message.success(`已添加 ${existing ? '累加盘点数量' : '商品'}：${p.name}`)
}

// ═══ 表头工具 ═══
const scanMode = ref(false)
const quickLocationKeyword = ref('')
const diffFilter = ref('')

const filteredProducts = computed(() => {
  let list = formData.products
  if (quickLocationKeyword.value.trim()) {
    const kw = quickLocationKeyword.value.trim().toLowerCase()
    list = list.filter((r: any) =>
      (r.productName && r.productName.toLowerCase().includes(kw)) ||
      (r.barcode && r.barcode.toLowerCase().includes(kw)) ||
      (r.itemCode && r.itemCode.toLowerCase().includes(kw))
    )
  }
  if (diffFilter.value) {
    list = list.filter((r: any) => {
      const d = Number(r.diffQuantity ?? 0)
      if (diffFilter.value === 'profit') return d > 0
      if (diffFilter.value === 'loss') return d < 0
      return d === 0
    })
  }
  return list
})

async function handleRefreshStock() {
  if (!formData.warehouseId) { message.warning('请先选择盘点仓库'); return }
  await loadStockMap(formData.warehouseId)
  let count = 0
  formData.products.forEach((row: any) => {
    const s = stockMap.value[row.productId]
    if (s) { row.stockQuantity = s.stockQuantity; recalcRow(row); count++ }
  })
  message.success(`已刷新 ${count} 行库存`)
}

// ═══ 字段变更 ═══
function handleFieldChange(fieldKey: string, val: any) {
  if (fieldKey === 'warehouseId') {
    const w = optionRefs.warehouses.find((x: any) => x.id === val)
    formData.warehouseName = w?.warehouseName || w?.name || ''
    loadStockMap(val)
  }
  if (fieldKey === 'handlerId') {
    const u = optionRefs.users.find((x: any) => x.id === val)
    formData.handlerName = u?.name || ''
  }
  if (fieldKey === 'deptId') {
    const d = departmentOptions.value.find((x: any) => x.id === val)
    formData.deptName = d?.name || ''
  }
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  message.info(`${fieldKey} 快速查询功能暂不可用`)
}

// ═══ 保存（Ctrl+S）→ status=1，不跳转 ═══
const saving = ref(false)
function buildPayload(status: number): any {
  return {
    status,
    stockTakeNo: formData.stockTakeNo || formData.orderNo || undefined,
    stockTakeDate: formData.date || undefined,
    checkMethod: formData.checkMethod ?? 1,
    checkType: formData.checkType ?? 1,
    warehouseId: formData.warehouseId,
    warehouseName: formData.warehouseName,
    regionName: formData.regionName || undefined,
    handlerId: formData.handlerId,
    handlerName: formData.handlerName,
    deptId: formData.deptId || undefined,
    deptName: formData.deptName || undefined,
    linkedBillNo: formData.linkedBillNo || undefined,
    summary: formData.summary || undefined,
    remark: formData.remark || undefined,
    attachment: formData.attachment || undefined,
    totalDiffQuantity: totalDiffQuantity.value,
    totalDiffAmount: totalDiffAmount.value,
    items: (formData.products || []).filter((p: any) => p.productId != null).map((p: any) => ({
      productId: p.productId,
      productCode: p.productCode || p.itemCode || undefined,
      productName: p.productName || undefined,
      productSpec: p.specification || p.itemSpec || undefined,
      productUnit: p.unit || p.productUnit || undefined,
      barcode: p.barcode || undefined,
      model: p.model || undefined,
      origin: p.origin || undefined,
      brand: p.brand || undefined,
      region: p.region || undefined,
      location: p.location || undefined,
      image: p.image || undefined,
      stockQuantity: p.stockQuantity ?? 0,
      conversionResult: p.conversionResult ?? 0,
      checkQuantity: p.checkQuantity ?? 0,
      checkQuantityConversionResult: p.checkQuantityConversionResult ?? 0,
      pieceQuantity: p.pieceQuantity ?? 0,
      diffQuantity: p.diffQuantity ?? 0,
      productionDate: p.productionDate || undefined,
      conversionRelation: p.conversionRelation || undefined,
      shelfLife: p.shelfLife || undefined,
      expiryDate: p.expiryDate || undefined,
      batchCode: p.batchCode || undefined,
      bigPack: p.bigPack ?? 0, midPack: p.midPack ?? 0, smallPack: p.smallPack ?? 0,
      checkStatus: p.checkStatus ?? 1,
      costPrice: p.costPrice ?? 0,
      diffConversionResult: p.diffConversionResult ?? 0,
      diffAmount: p.diffAmount ?? 0,
      itemExtNum1: p.itemExtNum1 ?? 0, itemExtNum2: p.itemExtNum2 ?? 0, itemExtNum3: p.itemExtNum3 ?? 0,
      itemExtText1: p.itemExtText1 || undefined, itemExtText2: p.itemExtText2 || undefined,
      remark: p.remark || undefined,
    })),
  }
}

function validate(): boolean {
  if (!formData.warehouseId) { message.warning('请选择盘点仓库'); return false }
  if (!formData.handlerId) { message.warning('请选择经手人'); return false }
  if (!formData.date) { message.warning('请选择单据日期'); return false }
  return true
}

async function doSave() {
  if (isLocked.value) return
  if (!validate()) return
  saving.value = true
  try {
    const payload = buildPayload(1)
    if (savedId.value) {
      const res: any = await stockTakeApi.update(savedId.value, payload)
      const saved = (res as any)?.data ?? res
      if (saved?.stockTakeNo) formData.stockTakeNo = saved.stockTakeNo
      message.success('保存成功')
    } else {
      const res: any = await stockTakeApi.create(payload)
      const saved = (res as any)?.data ?? res
      savedId.value = saved?.id
      formData.id = saved?.id
      formData.stockTakeNo = saved?.stockTakeNo || formData.stockTakeNo
      message.success(`保存成功：${saved?.stockTakeNo || ''}`)
    }
  } catch (err: any) {
    message.error(err?.response?.data?.message || err?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 盘点处理（Ctrl+Enter）═══
async function doProcess() {
  if (isLocked.value) return
  Modal.confirm({
    title: '盘点处理确认',
    content: '确定执行盘点处理吗？处理后将按盈亏自动生成报损单/报溢单，且单据不可再修改。',
    okText: '确认处理',
    cancelText: '取消',
    onOk: async () => {
      try {
        if (!savedId.value) await doSave()
        if (!savedId.value) { message.warning('盘点单尚未保存成功，无法处理'); return }
        const res: any = await stockTakeApi.process(savedId.value)
        const processed = (res as any)?.data ?? res
        message.success('盘点处理完成')
        await reloadDetail(savedId.value)
      } catch (err: any) {
        message.error(err?.response?.data?.message || err?.message || '盘点处理失败')
      }
    },
  })
}

async function reloadDetail(id: number) {
  try {
    const res: any = await stockTakeApi.getById(id)
    const data = (res as any)?.data ?? res
    if (data) {
      formData.status = data.status
      formData.processResult = data.processResult || ''
      formData.creatorName = data.creatorName || ''
      formData.createTime = data.createTime || ''
      formData.printCount = data.printCount || 0
      formData.bookkeeperName = data.bookkeeperName || ''
    }
  } catch { /* 静默 */ }
}

// ═══ 页眉/基本信息/摘要/页脚 ═══
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '盘点单',
  orderNo: formData.stockTakeNo || formData.orderNo,
  showAttachment: true,
  actions: [
    { key: 'quickCheck', label: '快速盘点', icon: ThunderboltOutlined },
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'more', label: '更多', icon: ImportOutlined, children: [
      { key: 'import', label: '导入' },
      { key: 'export', label: '导出' },
    ]},
    { key: 'config', label: '配置', icon: SettingOutlined },
  ],
}))

const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  { key: 'orderNo', label: '编号', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'warehouseId', label: '盘点仓库', type: 'select', required: true, inlineLabel: true, width: 210, searchBtn: '+Q', loading: true },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 210, searchBtn: '+Q', loading: true },
  { key: 'deptId', label: '部门', type: 'select', inlineLabel: true, width: 160, searchBtn: '+Q', loading: true },
  { key: 'date', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 210 },
  { key: 'summary', label: '摘要', type: 'input', inlineLabel: true, width: 300 },
  { key: 'remark', label: '单据备注', type: 'input', inlineLabel: true, width: 300 },
  { key: 'creatorName', label: '制单人', type: 'display', inlineLabel: true, width: 100 },
  { key: 'createTime', label: '制单时间', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'printCount', label: '打印次数', type: 'display', inlineLabel: true, width: 90 },
  { key: 'totalDiffAmount', label: '盈亏金额', type: 'number', inlineLabel: true, width: 150, precision: 2 },
]
const DEFAULT_HIDDEN_FIELDS = ['summary', 'remark', 'creatorName', 'createTime', 'printCount', 'totalDiffAmount']

type FieldConfig = { visible: boolean; enterJump: boolean }
const pageConfig = reactive<Record<string, FieldConfig>>({})
for (const f of ALL_BASIC_INFO_FIELDS) {
  pageConfig[f.key] = {
    visible: !DEFAULT_HIDDEN_FIELDS.includes(f.key),
    enterJump: ['select', 'date', 'number', 'input'].includes(f.type),
  }
}

const FORM_CONFIG_MODULE = 'stock-take-form'
const FORM_CONFIG_PAGE = 'form'

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
        defaultWarehouseId: parsed.defaults.defaultWarehouseId,
        defaultHandlerId: parsed.defaults.defaultHandlerId,
        defaultCheckMethod: parsed.defaults.defaultCheckMethod ?? 1,
        printTemplate: parsed.defaults.printTemplate,
        printCopies: parsed.defaults.printCopies,
        printPaperSize: parsed.defaults.printPaperSize,
      })
    }
  } catch { /* 保持默认 */ }
}

async function saveFormConfig() {
  const payload = {
    fields: Object.fromEntries(Object.entries(pageConfig).map(([k, v]) => [k, { visible: v.visible, enterJump: v.enterJump }])),
    defaults: {
      defaultWarehouseId: formData.defaultWarehouseId,
      defaultHandlerId: formData.defaultHandlerId,
      defaultCheckMethod: formData.defaultCheckMethod,
      printTemplate: formData.printTemplate,
      printCopies: formData.printCopies,
      printPaperSize: formData.printPaperSize,
    },
  }
  try {
    await userPageConfigApi.save(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE, JSON.stringify(payload))
  } catch { /* 静默 */ }
}

const showFormConfig = ref(false)
const configModalTab = ref('pageConfig')

const basicInfoFields = computed<BasicInfoField[]>(() =>
  ALL_BASIC_INFO_FIELDS
    .filter(f => pageConfig[f.key]?.visible !== false)
    .map(f => ({
      ...f,
      options: f.key === 'warehouseId'
        ? (optionRefs.warehouses || []).map((w: any) => ({ label: w.warehouseName || w.name, value: w.id }))
        : f.key === 'handlerId'
          ? (optionRefs.users || []).map((u: any) => ({ label: u.name, value: u.id }))
          : f.key === 'deptId'
            ? (departmentOptions.value || []).map((d: any) => ({ label: d.name, value: d.id }))
            : (f as any).options,
      loading: (f.key === 'warehouseId' || f.key === 'handlerId' || f.key === 'deptId') ? loadingOptions.value : (f as any).loading,
      searchBtn: (f.key === 'warehouseId' || f.key === 'handlerId' || f.key === 'deptId') ? '+Q' : (f as any).searchBtn,
    }))
)

const pageConfigFields = computed(() =>
  ALL_BASIC_INFO_FIELDS.map((f, i) => ({
    key: f.key, index: i + 1, name: f.label, displayName: f.label,
    visible: pageConfig[f.key]?.visible !== false,
    enterJump: pageConfig[f.key]?.enterJump ?? false,
  }))
)
const pageConfigTableColumns = [
  { title: '序号', key: 'index', width: 60 },
  { title: '名称', key: 'name', width: 120 },
  { title: '显示名', key: 'displayName', width: 160 },
  { title: '显示', key: 'visible', width: 70, align: 'center' as const },
  { title: '回车键跳转', key: 'enterJump', width: 100, align: 'center' as const },
]
function handlePageConfigFieldVisibleChange(fieldKey: string, visible: boolean) {
  if (pageConfig[fieldKey]) pageConfig[fieldKey].visible = visible
  saveFormConfig()
}
function handlePageConfigEnterJumpChange(fieldKey: string, checked: boolean) {
  if (pageConfig[fieldKey]) pageConfig[fieldKey].enterJump = checked
  saveFormConfig()
}

// ═══ 摘要 ═══
const totalDiffQuantity = computed(() => formData.products.reduce((s: number, p: any) => s + (Number(p.diffQuantity) || 0), 0))
const totalDiffAmount = computed(() => formData.products.reduce((s: number, p: any) => s + (Number(p.diffAmount) || 0), 0))

const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '盘点项', value: `${formData.products.length} 项`, statusLabel: statusText.value },
  { label: '盈亏数量', value: totalDiffQuantity.value.toFixed(4), divider: true },
  { label: '盈亏金额', value: `¥${totalDiffAmount.value.toFixed(2)}` },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '盈亏金额',
  amountValue: `¥${totalDiffAmount.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: isLocked.value ? undefined : '保存',
  draftShortcut: isLocked.value ? undefined : 'Ctrl+S',
  primaryBtnText: isLocked.value ? '已盘点' : '盘点处理',
  primaryShortcut: isLocked.value ? undefined : 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══ 明细表格列（默认14列 · 全部34列） ═══
const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 60, fixed: 'left' },
  { key: 'image', title: '图片', type: 'input', width: 60, defaultHidden: true },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productCell', width: 220 },
  { key: 'itemCode', title: '货号', type: 'input', width: 100 },
  { key: 'barcode', title: '条码', type: 'input', width: 110, defaultHidden: true },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'model', title: '型号', type: 'input', width: 90, defaultHidden: true },
  { key: 'origin', title: '产地', type: 'input', width: 90, defaultHidden: true },
  { key: 'region', title: '区域', type: 'input', width: 90, defaultHidden: true },
  { key: 'location', title: '货位', type: 'input', width: 90, defaultHidden: true },
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'stockQuantity', title: '库存数量', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'conversionResult', title: '换算结果', type: 'number', width: 90, precision: 2 },
  { key: 'checkQuantityConversionResult', title: '盘点数量换算结果', type: 'number', width: 130, precision: 2, defaultHidden: true },
  { key: 'checkQuantity', title: '盘点数量', type: 'number', width: 100, precision: 2 },
  { key: 'pieceQuantity', title: '件散数量', type: 'number', width: 90, precision: 2 },
  { key: 'diffQuantity', title: '盈亏数量', type: 'slot', slotName: 'diffCell', width: 100 },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 110 },
  { key: 'conversionRelation', title: '换算关系', type: 'input', width: 100 },
  { key: 'shelfLife', title: '保质期', type: 'input', width: 90 },
  { key: 'expiryDate', title: '到期日期', type: 'date', width: 110, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
  { key: 'bigPack', title: '大包装', type: 'number', width: 80, defaultHidden: true },
  { key: 'midPack', title: '中包装', type: 'number', width: 80, defaultHidden: true },
  { key: 'smallPack', title: '小包装', type: 'number', width: 80, defaultHidden: true },
  { key: 'checkStatus', title: '盘点状态', type: 'slot', slotName: 'checkStatusCell', width: 90, defaultHidden: true },
  { key: 'costPrice', title: '成本单价', type: 'number', width: 100, precision: 2, defaultHidden: true },
  { key: 'diffConversionResult', title: '盈亏换算结果', type: 'number', width: 120, precision: 2, defaultHidden: true },
  { key: 'diffAmount', title: '盈亏金额', type: 'number', width: 110, precision: 2, defaultHidden: true },
  { key: 'itemExtNum1', title: '单据自定义1(数字)', type: 'number', width: 120, precision: 2, defaultHidden: true },
  { key: 'itemExtNum2', title: '单据自定义2(数字)', type: 'number', width: 120, precision: 2, defaultHidden: true },
  { key: 'itemExtNum3', title: '单据自定义3(数字)', type: 'number', width: 120, precision: 2, defaultHidden: true },
  { key: 'itemExtText1', title: '单据自定义4(文本)', type: 'input', width: 120, defaultHidden: true },
  { key: 'itemExtText2', title: '单据自定义5(文本)', type: 'input', width: 120, defaultHidden: true },
]

const tableSummaryColumns = computed(() => [
  { key: 'diffQuantity', value: totalDiffQuantity.value, highlight: true },
] as { key: string; value: number; highlight?: boolean }[])

function getCheckStatusText(s: number): string {
  return { 1: '未盘', 2: '已盘', 3: '已确认' }[s] || '未盘'
}
function getCheckStatusColor(s: number): string {
  return { 1: 'default', 2: 'blue', 3: 'green' }[s] || 'default'
}

// ═══ 执行动作 ═══
async function handleAction(actionKey: string) {
  switch (actionKey) {
    case 'history': router.push('/erp/stocktake'); break
    case 'config': showFormConfig.value = true; break
    case 'print': handlePrint(); break
    case 'import': handleImport(); break
    case 'export': handleExport(); break
    case 'quickCheck': handleQuickCheck(); break
  }
}

function handlePrint() {
  if (formData.stockTakeNo || formData.orderNo) window.print()
  else message.warning('请先保存单据后再打印')
}

function handleQuickCheck() {
  message.info('快速盘点：请扫码连续录入，将自动计入「盘点数量」')
  scanMode.value = true
  nextTick(() => quickEntryInputRef.value?.focus())
}

function handleImport() {
  const fileInput = document.createElement('input')
  fileInput.type = 'file'
  fileInput.accept = '.csv,.txt'
  fileInput.onchange = () => {
    const file = fileInput.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = (e: any) => {
      const text = String(e.target?.result || '')
      const lines = text.split(/\r?\n/).map(l => l.trim()).filter(Boolean)
      if (lines.length <= 1) { message.warning('未解析到有效明细数据'); return }
      const header = lines[0].split(/[,\t]/).map(h => h.trim())
      const nameIdx = header.findIndex(h => h.includes('商品'))
      if (nameIdx < 0) { message.warning('导入文件需包含「商品」列'); return }
      for (let i = 1; i < lines.length; i++) {
        const cols = lines[i].split(/[,\t]/).map(c => c.trim())
        const p = optionRefs.products.find(x => x.name === cols[nameIdx])
        if (p) addQuickRow(p)
      }
      message.success(`已导入 ${lines.length - 1} 行`)
    }
    reader.readAsText(file)
  }
  fileInput.click()
}

function handleExport() {
  if (formData.products.length === 0) { message.warning('没有可导出的明细'); return }
  const headers = ['商品名称', '货号', '条码', '规格', '库存数量', '盘点数量', '盈亏数量', '备注']
  const rows = formData.products.filter((p: any) => p.productName).map((p: any) => [
    p.productName, p.itemCode || '', p.barcode || '', p.specification || '',
    p.stockQuantity, p.checkQuantity, p.diffQuantity, p.remark || '',
  ])
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `盘点单明细_${formData.stockTakeNo || new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// ═══ 快捷键 ═══
function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') { e.preventDefault(); doSave() }
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') { e.preventDefault(); doProcess() }
}

function formatQuantity(qty: number): string {
  if (qty === undefined || qty === null) return '0'
  return qty.toLocaleString('zh-CN', { maximumFractionDigits: 4 })
}

function handleError(err: any) {
  console.warn('[盘点单] ErrorBoundary:', err)
}

// ═══ 生命周期 ═══
onMounted(async () => {
  document.addEventListener('keydown', handleKeydown)
  const route = router.currentRoute.value
  const id = route.query.id || route.params.id
  await loadOptions()
  if (id) {
    savedId.value = Number(id)
    const res: any = await stockTakeApi.getById(Number(id))
    const data = (res as any)?.data ?? res
    if (data) {
      formData.id = data.id
      formData.stockTakeNo = data.stockTakeNo || ''
      formData.orderNo = data.stockTakeNo || ''
      formData.date = data.stockTakeDate || formData.date
      formData.warehouseId = data.warehouseId
      formData.warehouseName = data.warehouseName || ''
      formData.handlerId = data.handlerId
      formData.handlerName = data.handlerName || ''
      formData.deptId = data.deptId || undefined
      formData.deptName = data.deptName || ''
      formData.checkMethod = data.checkMethod ?? 1
      formData.checkType = data.checkType ?? 1
      formData.regionName = data.regionName || ''
      formData.summary = data.summary || ''
      formData.remark = data.remark || ''
      formData.status = data.status ?? 1
      formData.processResult = data.processResult || ''
      formData.creatorName = data.creatorName || ''
      formData.createTime = data.createTime || ''
      formData.printCount = data.printCount || 0
      const rawItems = data.items || []
      formData.products = rawItems.map((d: any, i: number) => ({
        id: d.id || `detail-${i}`,
        productId: d.productId,
        itemCode: d.productCode || '', productCode: d.productCode || '', productName: d.productName || '',
        barcode: d.barcode || '', specification: d.productSpec || '', itemSpec: d.productSpec || '',
        model: d.model || '', origin: d.origin || '', brand: d.brand || '', region: d.region || '',
        location: d.location || '', unit: d.productUnit || '', itemUnit: d.productUnit || '', productUnit: d.productUnit || '',
        image: d.image || '', stockQuantity: d.stockQuantity ?? 0, conversionResult: d.conversionResult ?? 0,
        checkQuantity: d.checkQuantity ?? 0, checkQuantityConversionResult: d.checkQuantityConversionResult ?? 0,
        pieceQuantity: d.pieceQuantity ?? 0, diffQuantity: d.diffQuantity ?? 0,
        productionDate: d.productionDate || '', conversionRelation: d.conversionRelation || '',
        shelfLife: d.shelfLife || '', expiryDate: d.expiryDate || '', batchCode: d.batchCode || '',
        bigPack: d.bigPack ?? 0, midPack: d.midPack ?? 0, smallPack: d.smallPack ?? 0,
        checkStatus: d.checkStatus ?? 1, costPrice: d.costPrice ?? 0,
        diffConversionResult: d.diffConversionResult ?? 0, diffAmount: d.diffAmount ?? 0,
        itemExtNum1: d.itemExtNum1 ?? 0, itemExtNum2: d.itemExtNum2 ?? 0, itemExtNum3: d.itemExtNum3 ?? 0,
        itemExtText1: d.itemExtText1 || '', itemExtText2: d.itemExtText2 || '', remark: d.remark || '',
      }))
      if (formData.warehouseId) loadStockMap(formData.warehouseId)
    }
  } else {
    await generateBillNo()
    for (let i = 0; i < 5; i++) handleAddProduct()
  }
  // 经手人默认当前登录人
  if (!formData.handlerId && currentUserId.value) {
    formData.handlerId = currentUserId.value
    const u = optionRefs.users.find((x: any) => x.id === currentUserId.value)
    formData.handlerName = u?.name || currentUserName.value || ''
  }
  if (formData.defaultWarehouseId && !formData.warehouseId) {
    formData.warehouseId = formData.defaultWarehouseId
    const w = optionRefs.warehouses.find((x: any) => x.id === formData.defaultWarehouseId)
    if (w) formData.warehouseName = w.warehouseName || w.name || ''
    loadStockMap(formData.warehouseId)
  }
  if (formData.defaultCheckMethod) formData.checkMethod = formData.defaultCheckMethod
  nextTick(() => { tableMaxHeight.value = Math.max(200, window.innerHeight - 420) })
  loadFormConfig()
  loadDepartments()
})

async function loadDepartments() {
  try { departmentOptions.value = await optionsApi.getDepartments() } catch { departmentOptions.value = [] }
}

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.stocktake-table-block { display: flex; flex-direction: column; height: 100%; min-height: 0; }
.quick-entry-row { display: flex; align-items: center; gap: 6px; padding: 8px 12px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.quick-entry-label { font-size: 12px; color: #595959; white-space: nowrap; }
.quick-entry-input { flex: 1; }
.quick-entry-qty { width: 120px; }
.table-tool-row { display: flex; justify-content: space-between; align-items: center; padding: 6px 12px; background: #fafafa; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.tool-left { display: flex; align-items: center; gap: 16px; }
.tool-search, .tool-status { display: flex; align-items: center; gap: 6px; }
.tool-label { font-size: 12px; color: #595959; white-space: nowrap; }
.active-row { display: flex; align-items: center; gap: 4px; }
.action-add-btn { color: #1890ff; padding: 0; font-size: 14px; }
.action-del-btn { color: #ff4d4f; padding: 0; font-size: 14px; }
.positive { color: #3f8600; font-weight: 600; }
.negative { color: #ff4d4f; font-weight: 600; }
.remark-section { padding: 4px 0; }
.remark-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.remark-label { font-size: 12px; color: #595959; white-space: nowrap; min-width: 60px; }
.remark-input { flex: 1; }
.doc-info-row { display: flex; align-items: center; gap: 16px; padding: 6px 0; font-size: 12px; color: #8c8c8c; border-top: 1px solid #f0f0f0; flex-wrap: wrap; }
.config-hint { font-size: 12px; color: #8c8c8c; margin-bottom: 8px; }
</style>
