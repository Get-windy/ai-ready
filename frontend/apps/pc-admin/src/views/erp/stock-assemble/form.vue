<template>
  <ErrorBoundary
    @reset="fetchDetail"
    @error="handleError"
  >
    <PageContainer title="组装单">
      <!-- ═══ 单据信息面板 ═══ -->
      <div class="panel">
        <div class="panel-title">
          <span v-if="formData.assembleNo">NO. {{ formData.assembleNo }}</span>
          <span v-else>组装单</span>
          <a-tag
            v-if="formData.id"
            :color="ASSEMBLE_STATUS[formData.status ?? 0]?.color || 'default'"
            class="panel-tag"
          >
            {{ ASSEMBLE_STATUS[formData.status ?? 0]?.text || '-' }}
          </a-tag>
        </div>
        <a-form
          layout="inline"
          class="header-form"
          :model="formData"
        >
          <a-form-item
            label="成品仓库"
            required
          >
            <a-select
              v-model:value="formData.inWarehouseId"
              style="width: 170px"
              placeholder="请选择成品仓库"
              show-search
              :filter-option="filterOption"
              :options="warehouseOptions"
              :loading="warehouseLoading"
              :disabled="!editable"
              @change="(val: any) => setWarehouseName('in', val)"
            />
          </a-form-item>
          <a-form-item
            label="原料仓库"
            required
          >
            <a-select
              v-model:value="formData.outWarehouseId"
              style="width: 170px"
              placeholder="请选择原料仓库"
              show-search
              :filter-option="filterOption"
              :options="warehouseOptions"
              :loading="warehouseLoading"
              :disabled="!editable"
              @change="(val: any) => setWarehouseName('out', val)"
            />
          </a-form-item>
          <a-form-item
            label="经手人"
            required
          >
            <a-input
              v-model:value="formData.handlerName"
              style="width: 110px"
              placeholder="经手人"
              :disabled="!editable"
            />
          </a-form-item>
          <a-form-item label="生产单位">
            <a-input
              v-model:value="formData.produceUnit"
              style="width: 130px"
              placeholder="生产单位"
              :disabled="!editable"
            />
          </a-form-item>
          <a-form-item
            label="单据日期"
            required
          >
            <a-date-picker
              v-model:value="formData.assembleDate"
              style="width: 130px"
              value-format="YYYY-MM-DD"
              :disabled="!editable"
            />
          </a-form-item>
          <a-form-item
            label="选择BOM模板"
            v-if="editable"
          >
            <a-select
              v-model:value="formData.bomId"
              style="width: 220px"
              placeholder="选择BOM模板带出明细"
              show-search
              :filter-option="filterOption"
              :options="bomTemplateOptions"
              :loading="bomLoading"
              allow-clear
              @change="handleBomChange"
            />
          </a-form-item>
        </a-form>
      </div>

      <!-- ═══ 成品详情(入库)表 ═══ -->
      <div class="panel">
        <div class="panel-title">
          成品详情(入库)
          <a-tag
            v-if="formData.id"
            color="blue"
          >原料出库+成品入库同单</a-tag>
        </div>
        <EditableBillGrid
          ref="productGridRef"
          :rows="formData.productRows"
          :columns="gridColumns"
          :readonly="!editable"
          :show-add="editable"
          @add-row="addProductRow"
          @remove-row="removeRow('productRows', $event)"
        />
        <div class="summary-line">
          <span>成品数量合计: <b>{{ sumQty(formData.productRows).toFixed(2) }}</b></span>
          <span>成品成本合计: <b>¥{{ sumCost(formData.productRows).toFixed(2) }}</b></span>
        </div>
      </div>

      <!-- ═══ 原料详情(出库)表 ═══ -->
      <div class="panel">
        <div class="panel-title">
          原料详情(出库)
          <a-tag
            v-if="stockValid !== undefined"
            :color="stockValid ? 'success' : 'error'"
          >
            {{ stockValid ? '库存充足' : '库存不足' }}
          </a-tag>
        </div>
        <EditableBillGrid
          ref="materialGridRef"
          :rows="formData.materialRows"
          :columns="gridColumns"
          :readonly="!editable"
          :show-add="editable"
          :show-stock="true"
          :stock-map="stockMap"
          @add-row="addMaterialRow"
          @remove-row="removeRow('materialRows', $event)"
        />
        <div class="summary-line">
          <span>原料种类: <b>{{ formData.materialRows.length }}</b></span>
          <span>原料成本合计: <b>¥{{ materialCostTotal.toFixed(2) }}</b></span>
        </div>
      </div>

      <!-- ═══ 加工费用/计算成本 ═══ -->
      <div class="panel fee-panel">
        <span class="fee-label">加工费用</span>
        <a-input-number
          v-model:value="formData.assembleFee"
          :min="0"
          :precision="2"
          style="width: 130px"
          :disabled="!editable"
          @change="recalcTotal"
        />
        <a-button
          type="primary"
          ghost
          :disabled="!editable"
          @click="handleCalcCost"
        >
          计算成本
        </a-button>
        <span class="fee-note">成品成本 = 原料成本合计 + 加工费用</span>
      </div>

      <!-- ═══ 备注区 ═══ -->
      <div class="panel">
        <div class="panel-title">单据备注</div>
        <a-textarea
          v-model:value="formData.remark"
          :rows="2"
          placeholder="单据备注"
          :disabled="!editable"
          style="max-width: 720px"
        />
      </div>

      <!-- ═══ 单据信息行 ═══ -->
      <div class="panel doc-info-row">
        <span>制单人: <b>{{ formData.applicantName || currentUserName || '-' }}</b></span>
        <span>制单时间: <b>{{ formData.createTime || '-' }}</b></span>
        <span>打印次数: <b>{{ formData.printCount ?? 0 }}</b></span>
        <span>打印记录: <b>{{ formData.printRecords || '无' }}</b></span>
      </div>

      <!-- ═══ 本单金额 ═══ -->
      <div class="total-bar">
        <span class="total-label">本单金额</span>
        <span class="total-value">¥{{ totalCost.toFixed(2) }}</span>
      </div>

      <!-- ═══ 操作按钮 ═══ -->
      <div class="btn-row">
        <a-space>
          <template v-if="isNew || editable">
            <a-button
              type="primary"
              :loading="saving"
              @click="handleSaveDraft"
            >
              保存草稿 <span class="shortcut">Ctrl+S</span>
            </a-button>
            <a-button
              type="primary"
              :loading="submitting"
              :disabled="!stockValid"
              @click="handleSubmit"
            >
              记帐 <span class="shortcut">Ctrl+Enter</span>
            </a-button>
          </template>

          <template v-if="!isNew">
            <template v-if="formData.status === 1">
              <a-button
                type="primary"
                :loading="approving"
                @click="handleApprove"
              >
                审批通过
              </a-button>
              <a-button
                danger
                :loading="rejecting"
                @click="handleReject"
              >
                拒绝
              </a-button>
            </template>
            <a-button
              v-if="formData.status === 2"
              type="primary"
              :loading="executing"
              @click="handleExecute"
            >
              执行组装
            </a-button>
          </template>

          <a-button @click="handleBack">
            返回
          </a-button>
        </a-space>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import EditableBillGrid from '@/components/business/EditableBillGrid/EditableBillGrid.vue'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import { ASSEMBLE_STATUS } from '@/utils/statusConfig'

defineOptions({ name: 'StockAssembleForm' })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const idParam = computed(() => route.params.id as string)
const isNew = computed(() => !idParam.value || idParam.value === 'new')
const editable = computed(() => isNew.value || formData.status === 0)
const currentUserName = computed(() => userStore.nickname || userStore.username || '')

// ── 选项数据 ──────────────────────────────────────────
const bomTemplateOptions = ref<any[]>([])
const warehouseOptions = ref<any[]>([])
const bomLoading = ref(false)
const warehouseLoading = ref(false)

async function loadBomTemplates() {
  bomLoading.value = true
  try {
    const res = await request.get('/erp/stock/bom/page', { params: { pageSize: 500, status: 1 } })
    const data = res?.data || res
    const records = data?.records || (Array.isArray(data) ? data : [])
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

async function loadWarehouses() {
  warehouseLoading.value = true
  try {
    const res = await request.get('/erp/warehouse/list')
    const list = res?.data || res
    warehouseOptions.value = (Array.isArray(list) ? list : []).map((w: any) => ({
      label: w.warehouseName,
      value: w.id,
      warehouseName: w.warehouseName,
    }))
  } catch { warehouseOptions.value = [] }
  finally { warehouseLoading.value = false }
}

function setWarehouseName(kind: 'in' | 'out', id: any) {
  const opt = warehouseOptions.value.find((w: any) => w.value === id)
  const name = opt?.warehouseName || ''
  if (kind === 'in') formData.inWarehouseName = name
  else formData.outWarehouseName = name
}

// ── 表单数据 ──────────────────────────────────────────
let keyCounter = 0
function nextKey() { return `item_${++keyCounter}_${Date.now()}` }

const formData = reactive({
  id: undefined as number | undefined,
  assembleNo: '',
  bomId: undefined as number | undefined,
  inWarehouseId: undefined as number | undefined,
  inWarehouseName: '',
  outWarehouseId: undefined as number | undefined,
  outWarehouseName: '',
  handlerName: '',
  produceUnit: '',
  assembleDate: '',
  assembleFee: 0,
  remark: '',
  status: 0,
  applicantName: '',
  createTime: '',
  printCount: 0,
  printRecords: '',
  productRows: [] as any[],
  materialRows: [] as any[],
})

// 成品/原料通用网格列（对标 15 列）
const gridColumns = [
  { title: '商品名称', key: 'productName', width: 170, editable: true },
  { title: '货号', key: 'productCode', width: 100, editable: true },
  { title: '货位', key: 'location', width: 80, editable: true },
  { title: '条码', key: 'barcode', width: 120, editable: true },
  { title: '计价单位', key: 'unit', width: 70, editable: true },
  { title: '批次条码', key: 'batchNo', width: 110, editable: true },
  { title: '生产日期', key: 'produceDate', width: 105, editable: true },
  { title: '保质期', key: 'shelfLife', width: 80, editable: true },
  { title: '到期日期', key: 'expireDate', width: 105, editable: true },
  { title: '数量', key: 'quantity', width: 90, editable: true, valueType: 'number' },
  { title: '件散数量', key: 'pieceQty', width: 90, editable: true, valueType: 'number' },
  { title: '成本单价', key: 'unitCost', width: 100, editable: true, valueType: 'money' },
  { title: '成本金额', key: 'cost', width: 110, valueType: 'money' },
  { title: '备注', key: 'remark', width: 120, editable: true },
]

const stockMap = ref<Record<number, number>>({})
const saving = ref(false)
const submitting = ref(false)
const approving = ref(false)
const rejecting = ref(false)
const executing = ref(false)

// ── 计算属性 ──────────────────────────────────────────
const materialCostTotal = computed(() =>
  formData.materialRows.reduce((s: number, row: any) => s + ((row.unitCost || 0) * (row.quantity || 0)), 0)
)
const totalCost = computed(() => materialCostTotal.value + (formData.assembleFee || 0))
const stockValid = computed(() => {
  if (formData.materialRows.length === 0) return undefined
  return formData.materialRows.every((row: any) => {
    if (row.availableStock === undefined || row.availableStock === null) return true
    return row.availableStock >= (row.quantity || 0)
  })
})

function sumQty(rows: any[]) {
  return rows.reduce((s: number, r: any) => s + (Number(r.quantity) || 0), 0)
}
function sumCost(rows: any[]) {
  return rows.reduce((s: number, r: any) => s + ((Number(r.unitCost) || 0) * (Number(r.quantity) || 0)), 0)
}

function emptyRow(partial: Record<string, any> = {}) {
  return {
    _key: nextKey(),
    productId: undefined as number | undefined,
    productCode: '',
    productName: '',
    location: '',
    barcode: '',
    unit: '',
    batchNo: '',
    produceDate: '',
    shelfLife: '',
    expireDate: '',
    quantity: 1,
    pieceQty: 0,
    unitCost: 0,
    cost: 0,
    remark: '',
    availableStock: undefined as number | undefined,
    ...partial,
  }
}

function addProductRow() {
  if (!editable.value) return
  formData.productRows.push(emptyRow())
}
function addMaterialRow() {
  if (!editable.value) return
  const row = emptyRow()
  row.availableStock = undefined
  formData.materialRows.push(row)
}
function removeRow(kind: 'productRows' | 'materialRows', index: number) {
  if (!editable.value) return
  formData[kind].splice(index, 1)
  recalcRows()
}

function rowCost(row: any) {
  row.cost = (Number(row.unitCost) || 0) * (Number(row.quantity) || 0)
}
function recalcRows() {
  ;[...formData.productRows, ...formData.materialRows].forEach(rowCost)
}

// ── 事件处理 ──────────────────────────────────────────

async function handleBomChange(bomId?: number) {
  formData.productRows = []
  formData.materialRows = []
  formData.bomId = bomId

  if (!bomId) return

  const bom = bomTemplateOptions.value.find((b: any) => b.value === bomId)
  if (!bom) return

  // 成品表：BOM 产物一行
  formData.productRows = [emptyRow({
    productId: bom.productId,
    productCode: bom.productCode || '',
    productName: bom.productName || '',
    unit: bom.productUnit || '',
    quantity: bom.outputQuantity ?? 1,
  })]

  // 加载原料明细
  try {
    const res = await request.get(`/erp/stock/bom/${bomId}/items`)
    const components = res?.data || []
    formData.materialRows = components.map((comp: any) => emptyRow({
      productId: comp.productId,
      productCode: comp.productCode || '',
      productName: comp.productName || '',
      unit: comp.productUnit || comp.unit || '',
      quantity: (comp.quantity ?? 1) * (formData.productRows[0]?.quantity || 1),
      unitCost: comp.unitCost ?? 0,
    }))
    recalcRows()
    checkStockAvailability()
  } catch {
    message.warning('加载BOM组件明细失败')
    formData.materialRows = []
  }
}

async function checkStockAvailability() {
  const warehouseId = formData.outWarehouseId
  const rows = formData.materialRows
  if (!warehouseId || rows.length === 0) return
  try {
    const productIds = rows.map((r: any) => r.productId).filter(Boolean)
    if (productIds.length === 0) return
    const res = await request.post('/erp/stock/batch-query', { warehouseId, productIds })
    const map: Record<number, number> = res?.data || {}
    stockMap.value = map
    for (const row of rows) {
      row.availableStock = map[row.productId] ?? 0
    }
  } catch {
    // 静默失败，不影响主流程
  }
}

function recalcTotal() {
  // 本单金额联动
}

function handleCalcCost() {
  // 成品成本 = 原料成本合计 + 加工费用，按成品数量分摊到成品成本单价
  const total = materialCostTotal.value + (formData.assembleFee || 0)
  if (formData.productRows.length > 0) {
    const qty = Number(formData.productRows[0].quantity) || 1
    const unitCost = qty > 0 ? total / qty : 0
    formData.productRows[0].unitCost = Math.round(unitCost * 100) / 100
    rowCost(formData.productRows[0])
  }
  message.success(`计算成本完成，本单金额 ¥${total.toFixed(2)}`)
}

// ── 保存/提交/审批/执行 ──────────────────────────────

function validate(): boolean {
  if (!formData.inWarehouseId) { message.warning('请选择成品仓库'); return false }
  if (!formData.outWarehouseId) { message.warning('请选择原料仓库'); return false }
  if (!formData.handlerName) { message.warning('请输入经手人'); return false }
  if (!formData.assembleDate) { message.warning('请选择单据日期'); return false }
  if (formData.productRows.length === 0) { message.warning('成品详情不能为空'); return false }
  if (formData.materialRows.length === 0) { message.warning('原料详情不能为空'); return false }
  for (const row of formData.materialRows) {
    if (!row.productId) { message.warning('原料明细存在未选择商品的行'); return false }
  }
  return true
}

function buildPayload(status?: number) {
  const productRow = formData.productRows[0] || {}
  return {
    bomId: formData.bomId,
    warehouseId: formData.inWarehouseId,
    warehouseName: formData.inWarehouseName,
    inWarehouseId: formData.inWarehouseId,
    inWarehouseName: formData.inWarehouseName,
    outWarehouseId: formData.outWarehouseId,
    outWarehouseName: formData.outWarehouseName,
    handlerName: formData.handlerName,
    produceUnit: formData.produceUnit || undefined,
    assembleDate: formData.assembleDate,
    assembleFee: formData.assembleFee || undefined,
    totalCost: totalCost.value,
    remark: formData.remark || undefined,
    status: status ?? formData.status,
    productId: productRow.productId,
    productCode: productRow.productCode,
    productName: productRow.productName,
    productSpec: productRow.location,
    productUnit: productRow.unit,
    assembleQuantity: Number(productRow.quantity) || 1,
    outputQuantity: Number(productRow.quantity) || 1,
    items: formData.materialRows.map((row: any) => ({
      productId: row.productId,
      productCode: row.productCode,
      productName: row.productName,
      spec: row.location,
      unit: row.unit,
      quantity: row.quantity,
      unitCost: row.unitCost,
    })),
  }
}

async function handleSaveDraft() {
  if (!validate()) return
  saving.value = true
  try {
    if (formData.id) {
      await request.put(`/erp/stock/assemble/${formData.id}`, buildPayload(0))
      message.success('组装单已更新')
    } else {
      const res = await request.post('/erp/stock/assemble', buildPayload(0))
      formData.id = res?.data?.id || res?.id
      message.success('组装单草稿已保存')
    }
    handleBack()
  } catch (err: any) {
    message.error(err?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleSubmit() {
  if (!validate()) return
  if (stockValid.value === false) {
    Modal.confirm({
      title: '库存不足',
      content: '部分原料库存不足，确认仍要记帐提交吗？',
      okText: '确认提交',
      cancelText: '取消',
      onOk: async () => doSubmit(),
    })
  } else {
    await doSubmit()
  }
}

async function doSubmit() {
  submitting.value = true
  try {
    if (formData.id) {
      await request.put(`/erp/stock/assemble/${formData.id}`, buildPayload(1))
      await request.post(`/erp/stock/assemble/${formData.id}/submit`)
    } else {
      const res = await request.post('/erp/stock/assemble', buildPayload(1))
      formData.id = res?.data?.id || res?.id
      if (formData.id) {
        await request.post(`/erp/stock/assemble/${formData.id}/submit`)
      }
    }
    message.success('组装单已提交审批')
    handleBack()
  } catch (err: any) {
    message.error(err?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

async function handleApprove() {
  if (!formData.id) return
  Modal.confirm({
    title: '审批确认',
    content: '确认审批通过该组装单吗？',
    okText: '确认',
    onOk: async () => {
      approving.value = true
      try {
        await request.post(`/erp/stock/assemble/${formData.id}/approve`)
        message.success('审批通过')
        handleBack()
      } catch (err: any) {
        message.error(err?.message || '审批失败')
      } finally {
        approving.value = false
      }
    },
  })
}

async function handleReject() {
  if (!formData.id) return
  Modal.confirm({
    title: '拒绝确认',
    content: '确认拒绝该组装单吗？',
    okText: '确认拒绝',
    okButtonProps: { danger: true },
    onOk: async () => {
      rejecting.value = true
      try {
        await request.post(`/erp/stock/assemble/${formData.id}/reject`, null, { params: { reason: '审批拒绝' } })
        message.success('已拒绝')
        handleBack()
      } catch (err: any) {
        message.error(err?.message || '拒绝失败')
      } finally {
        rejecting.value = false
      }
    },
  })
}

async function handleExecute() {
  if (!formData.id) return
  Modal.confirm({
    title: '执行组装',
    content: `确认执行该组装单吗？执行后将：\n1. 按原料详情扣减原料仓库库存\n2. 按成品详情增加成品仓库库存\n3. 总成本 = 原料成本 + 加工费用`,
    okText: '确认执行',
    onOk: async () => {
      executing.value = true
      try {
        await request.post(`/erp/stock/assemble/${formData.id}/execute`)
        message.success('组装执行成功')
        handleBack()
      } catch (err: any) {
        message.error(err?.message || '执行失败')
      } finally {
        executing.value = false
      }
    },
  })
}

// ── 加载详情 ──────────────────────────────────────────

async function fetchDetail() {
  if (isNew.value) return
  try {
    const res = await request.get(`/erp/stock/assemble/${idParam.value}`)
    const data = res?.data || res
    if (data) {
      formData.id = data.id
      formData.assembleNo = data.assembleNo || ''
      formData.bomId = data.bomId
      formData.inWarehouseId = data.inWarehouseId ?? data.warehouseId
      formData.inWarehouseName = data.inWarehouseName || data.warehouseName || ''
      formData.outWarehouseId = data.outWarehouseId ?? data.warehouseId
      formData.outWarehouseName = data.outWarehouseName || data.warehouseName || ''
      formData.handlerName = data.handlerName || ''
      formData.produceUnit = data.produceUnit || ''
      formData.assembleDate = data.assembleDate || ''
      formData.assembleFee = data.assembleFee ?? 0
      formData.remark = data.remark || ''
      formData.status = data.status ?? 0
      formData.applicantName = data.applicantName || ''
      formData.createTime = data.createTime || ''
      formData.printCount = data.printCount ?? 0
      formData.printRecords = data.printRecords || ''

      // 成品表：单行（兼容旧数据回填）
      formData.productRows = [emptyRow({
        productId: data.productId,
        productCode: data.productCode || '',
        productName: data.productName || '',
        location: data.productSpec || '',
        unit: data.productUnit || '',
        quantity: data.outputQuantity ?? data.assembleQuantity ?? 1,
        unitCost: data.totalCost && data.outputQuantity
          ? Math.round((data.totalCost / data.outputQuantity) * 100) / 100
          : 0,
      })]
    }
    // 加载原料明细
    try {
      const itemsRes = await request.get(`/erp/stock/assemble/${idParam.value}/items`)
      const items = itemsRes?.data || []
      formData.materialRows = items.map((item: any) => emptyRow({
        productId: item.productId,
        productCode: item.productCode || '',
        productName: item.productName || '',
        location: item.spec || '',
        unit: item.unit || '',
        quantity: item.quantity ?? 1,
        unitCost: item.unitCost ?? 0,
      }))
      recalcRows()
    } catch { formData.materialRows = [] }
  } catch {
    message.error('加载组装单详情失败')
  }
}

function handleBack() {
  router.push('/erp/stock-assemble')
}

function handleError(err: any) {
  console.warn('[组装单] ErrorBoundary 捕获:', err)
}

function filterOption(input: string, option: any) {
  return (option?.label?.toString() || '').toLowerCase().includes(input.toLowerCase())
}

// ── 快捷键：保存草稿 Ctrl+S / 记帐 Ctrl+Enter ────────
function onKeydown(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || (e.target as HTMLElement)?.isContentEditable) {
    return
  }
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
    e.preventDefault()
    handleSaveDraft()
  } else if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    handleSubmit()
  }
}

onMounted(async () => {
  if (!currentUserName.value) {
    // 等待登录信息可用
  }
  if (!formData.handlerName && isNew.value) {
    formData.handlerName = currentUserName.value
  }
  if (isNew.value && !formData.assembleDate) {
    const now = new Date()
    formData.assembleDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  }
  await Promise.all([loadBomTemplates(), loadWarehouses()])
  await fetchDetail()
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.panel {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.panel-tag { margin-left: 8px; }
.header-form { display: flex; flex-wrap: wrap; gap: 8px; }
.btn-row { margin-top: 16px; }
.shortcut {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.75);
  margin-left: 4px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 3px;
  padding: 0 4px;
}
.summary-line {
  display: flex;
  gap: 24px;
  padding: 8px 12px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
  margin-top: 8px;
}
.summary-line b { color: #303133; }
.fee-panel {
  display: flex;
  align-items: center;
  gap: 12px;
}
.fee-label { font-weight: 600; color: #303133; }
.fee-note { color: #999; font-size: 12px; }
.doc-info-row {
  display: flex;
  gap: 28px;
  font-size: 13px;
  color: #606266;
  flex-wrap: wrap;
}
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
.empty-hint { color: #999; padding: 12px; text-align: center; }
</style>
