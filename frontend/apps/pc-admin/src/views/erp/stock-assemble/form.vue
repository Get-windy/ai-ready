<template>
  <ErrorBoundary
    @reset="fetchDetail"
    @error="handleError"
  >
    <PageContainer title="组装单">
      <!-- ═══ 单据信息面板 ═══ -->
      <div class="panel">
        <div class="panel-title">
          单据信息
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
          <a-form-item label="组装单号">
            <a-input
              v-model:value="formData.assembleNo"
              style="width: 180px"
              placeholder="自动生成"
              :disabled="!isNew"
            />
          </a-form-item>
          <a-form-item
            label="选择BOM模板"
            required
          >
            <a-select
              v-model:value="formData.bomId"
              style="width: 300px"
              placeholder="请选择BOM模板"
              show-search
              :filter-option="filterOption"
              :options="bomTemplateOptions"
              :loading="bomLoading"
              :disabled="!editable"
              @change="handleBomChange"
            />
          </a-form-item>
          <a-form-item
            label="仓库"
            required
          >
            <a-select
              v-model:value="formData.warehouseId"
              style="width: 200px"
              placeholder="请选择仓库"
              show-search
              :filter-option="filterOption"
              :options="warehouseOptions"
              :loading="warehouseLoading"
              :disabled="!editable"
            />
          </a-form-item>
          <a-form-item label="组装数量">
            <a-input-number
              v-model:value="formData.assembleQuantity"
              :min="1"
              :precision="0"
              style="width: 100px"
              :disabled="!editable"
              @change="handleQuantityChange"
            />
          </a-form-item>
          <a-form-item label="人工费用">
            <a-input-number
              v-model:value="formData.laborFee"
              :min="0"
              :precision="2"
              style="width: 120px"
              :disabled="!editable"
              @change="recalcTotal"
            />
          </a-form-item>
          <a-form-item label="备注">
            <a-input
              v-model:value="formData.remark"
              style="width: 260px"
              placeholder="备注"
              :disabled="!editable"
            />
          </a-form-item>
        </a-form>
      </div>

      <!-- ═══ 成品信息面板 ═══ -->
      <div class="panel">
        <div class="panel-title">
          成品信息
        </div>
        <a-descriptions
          v-if="formData.productId"
          size="small"
          :column="4"
          bordered
        >
          <a-descriptions-item label="成品名称">
            {{ formData.productName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="成品编码">
            {{ formData.productCode || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="规格">
            {{ formData.productSpec || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="单位">
            {{ formData.productUnit || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="产出数量">
            {{ formData.outputQuantity ?? 1 }}
          </a-descriptions-item>
          <a-descriptions-item label="BOM版本">
            {{ formData.bomVersion || '-' }}
          </a-descriptions-item>
        </a-descriptions>
        <div v-else class="empty-hint">
          请先选择BOM模板
        </div>
      </div>

      <!-- ═══ 组件物料明细面板 ═══ -->
      <div class="panel">
        <div class="panel-title">
          组件物料明细
          <a-tag
            v-if="stockValid !== undefined"
            :color="stockValid ? 'success' : 'error'"
          >
            {{ stockValid ? '库存充足' : '库存不足' }}
          </a-tag>
        </div>
        <a-table
          :columns="itemColumns"
          :data-source="formData.items"
          :loading="itemLoading"
          :pagination="false"
          row-key="_key"
          size="small"
          bordered
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'index'">
              {{ formData.items.indexOf(record) + 1 }}
            </template>
            <template v-if="column.key === 'cost' || column.key === 'unitCost'">
              ¥{{ (record[column.key] || 0).toFixed(2) }}
            </template>
            <template v-if="column.key === 'stockStatus'">
              <a-tag
                v-if="record.availableStock !== undefined"
                :color="record.availableStock >= record.requiredQty ? 'success' : 'error'"
              >
                {{ record.availableStock >= record.requiredQty ? '充足' : '不足' }}
                (可用: {{ record.availableStock ?? 0 }})
              </a-tag>
            </template>
          </template>
        </a-table>

        <!-- 成本汇总 -->
        <div class="summary-row">
          <span>物料成本: <b>¥{{ materialCost.toFixed(2) }}</b></span>
          <span>人工费用: <b>¥{{ (formData.laborFee || 0).toFixed(2) }}</b></span>
          <span style="font-size: 15px;">
            预计总成本: <b style="color: #1890ff;">¥{{ totalCost.toFixed(2) }}</b>
          </span>
        </div>
      </div>

      <!-- ═══ 操作按钮 ═══ -->
      <div class="btn-row">
        <a-space>
          <template v-if="isNew || editable">
            <a-button
              v-if="!formData.id"
              type="primary"
              :loading="saving"
              @click="handleSaveDraft"
            >
              保存草稿
            </a-button>
            <a-button
              type="primary"
              :loading="submitting"
              :disabled="!stockValid"
              @click="handleSubmit"
            >
              提交审批
            </a-button>
          </template>

          <template v-if="!isNew">
            <!-- 待审批 -> 审批/拒绝 -->
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
            <!-- 已审核 -> 执行 -->
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import request from '@/utils/request'
import { ASSEMBLE_STATUS } from '@/utils/statusConfig'

defineOptions({ name: 'StockAssembleForm' })

const router = useRouter()
const route = useRoute()
const idParam = computed(() => route.params.id as string)
const isNew = computed(() => !idParam.value || idParam.value === 'new')
const editable = computed(() => isNew.value || formData.status === 0)

// ── 选项数据 ──────────────────────────────────────────
const bomTemplateOptions = ref<any[]>([])
const warehouseOptions = ref<any[]>([])
const bomLoading = ref(false)
const warehouseLoading = ref(false)

async function loadBomTemplates() {
  bomLoading.value = true
  try {
    const res = await request.get('/wh/production-template/page', { params: { pageSize: 500, status: 1 } })
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
    }))
  } catch { warehouseOptions.value = [] }
  finally { warehouseLoading.value = false }
}

// ── 表单数据 ──────────────────────────────────────────
let keyCounter = 0
function nextKey() { return `item_${++keyCounter}_${Date.now()}` }

const formData = reactive({
  id: undefined as number | undefined,
  assembleNo: '',
  bomId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  assembleQuantity: 1,
  laborFee: 0,
  remark: '',
  status: 0,
  // 成品快照
  productId: undefined as number | undefined,
  productCode: '',
  productName: '',
  productSpec: '',
  productUnit: '',
  outputQuantity: 1,
  bomVersion: '',
  // 物料明细
  items: [] as any[],
  createTime: '',
})

const itemColumns = [
  { title: '序号', key: 'index', width: 50 },
  { title: '物料名称', dataIndex: 'productName', key: 'productName', width: 180 },
  { title: '规格型号', dataIndex: 'productSpec', key: 'productSpec', width: 100 },
  { title: '单位', dataIndex: 'productUnit', key: 'productUnit', width: 60 },
  { title: '需用数量', dataIndex: 'requiredQty', key: 'requiredQty', width: 80 },
  { title: '单位成本', dataIndex: 'unitCost', key: 'unitCost', width: 100 },
  { title: '物料成本', key: 'cost', width: 100 },
  { title: '可用库存', dataIndex: 'availableStock', key: 'availableStock', width: 80 },
  { title: '库存状态', key: 'stockStatus', width: 100 }
]

const itemLoading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const approving = ref(false)
const rejecting = ref(false)
const executing = ref(false)

// ── 计算属性 ──────────────────────────────────────────
const materialCost = computed(() =>
  formData.items.reduce((s: number, item: any) => s + ((item.unitCost || 0) * (item.requiredQty || 0)), 0)
)
const totalCost = computed(() => materialCost.value + (formData.laborFee || 0))
const stockValid = computed(() => {
  if (formData.items.length === 0) return undefined
  return formData.items.every((item: any) => {
    if (item.availableStock === undefined) return true
    return item.availableStock >= (item.requiredQty || 0)
  })
})

// ── 事件处理 ──────────────────────────────────────────

async function handleBomChange(bomId: number) {
  formData.items = []
  formData.productId = undefined
  formData.productCode = ''
  formData.productName = ''
  formData.productSpec = ''
  formData.productUnit = ''
  formData.outputQuantity = 1
  formData.bomVersion = ''

  if (!bomId) return

  const bom = bomTemplateOptions.value.find((b: any) => b.value === bomId)
  if (bom) {
    formData.productId = bom.productId
    formData.productCode = bom.productCode
    formData.productName = bom.productName
    formData.productSpec = bom.productSpec
    formData.productUnit = bom.productUnit
    formData.outputQuantity = bom.outputQuantity ?? 1
    formData.bomVersion = bom.version || ''
  }

  itemLoading.value = true
  try {
    // 从生产模板接口加载组件物料
    const res = await request.get(`/wh/production-template/${bomId}/components`)
    const components = res?.data || []
    formData.items = components.map((comp: any) => ({
      _key: nextKey(),
      productId: comp.productId,
      productCode: comp.productCode || '',
      productName: comp.productName || '',
      spec: comp.spec || '',
      unit: comp.unit || '',
      baseQty: comp.quantity ?? 1,        // 单个成品的用料量
      wastageRate: comp.wastageRate ?? 0,   // 损耗率
      requiredQty: (comp.quantity ?? 1) * formData.assembleQuantity,  // 组装数量 * 单位用量
      unitCost: comp.unitCost ?? 0,
      availableStock: undefined,
    }))
    // 查询各物料库存可用量
    if (formData.warehouseId) {
      checkStockAvailability()
    }
  } catch {
    message.warning('加载BOM组件明细失败')
    formData.items = []
  } finally {
    itemLoading.value = false
  }
}

function handleQuantityChange(val: number | null) {
  const qty = val || 1
  formData.assembleQuantity = qty
  for (const item of formData.items) {
    item.requiredQty = (item.baseQty || 1) * qty
  }
  recalcTotal()
}

async function checkStockAvailability() {
  if (!formData.warehouseId || formData.items.length === 0) return
  try {
    const productIds = formData.items.map((i: any) => i.productId).filter(Boolean)
    if (productIds.length === 0) return
    const res = await request.post('/erp/stock/batch-query', {
      warehouseId: formData.warehouseId,
      productIds,
    })
    const stockMap: Record<number, number> = res?.data || {}
    for (const item of formData.items) {
      item.availableStock = stockMap[item.productId] ?? 0
    }
  } catch {
    // 静默失败，不影响主流程
    console.warn('[组装单] 库存查询失败')
  }
}

function recalcTotal() {
  // 总成本自动根据物料成本 + 人工费用重新计算
}

// ── 保存/提交/审批/执行 ──────────────────────────────

function validate(): boolean {
  if (!formData.bomId) { message.warning('请选择BOM模板'); return false }
  if (!formData.warehouseId) { message.warning('请选择仓库'); return false }
  if (!formData.assembleQuantity || formData.assembleQuantity < 1) { message.warning('组装数量必须大于0'); return false }
  if (formData.items.length === 0) { message.warning('BOM组件明细为空'); return false }
  return true
}

function buildPayload(status?: number) {
  return {
    bomId: formData.bomId,
    warehouseId: formData.warehouseId,
    assembleQuantity: formData.assembleQuantity,
    laborFee: formData.laborFee || undefined,
    totalCost: totalCost.value,
    remark: formData.remark || undefined,
    status: status ?? formData.status,
    items: formData.items.map((item: any) => ({
      productId: item.productId,
      productCode: item.productCode,
      productName: item.productName,
      spec: item.spec,
      unit: item.unit,
      quantity: item.baseQty,
      requiredQty: item.requiredQty,
      wastageRate: item.wastageRate,
      unitCost: item.unitCost,
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
      content: '部分物料库存不足，确认仍要提交审批吗？',
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
  let reason = ''
  Modal.confirm({
    title: '拒绝确认',
    content: '确认拒绝该组装单吗？',
    okText: '确认拒绝',
    okButtonProps: { danger: true },
    onOk: async () => {
      rejecting.value = true
      try {
        await request.post(`/erp/stock/assemble/${formData.id}/reject`, null, { params: { reason: reason || '审批拒绝' } })
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
    content: `确认执行该组装单吗？执行后将：\n1. 扣除组件库存\n2. 增加成品库存\n3. 总成本 = 物料成本 + 人工费用`,
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
      formData.warehouseId = data.warehouseId
      formData.assembleQuantity = data.assembleQuantity ?? 1
      formData.laborFee = data.laborFee ?? 0
      formData.remark = data.remark || ''
      formData.status = data.status ?? 0
      formData.productId = data.productId
      formData.productCode = data.productCode || ''
      formData.productName = data.productName || ''
      formData.productSpec = data.productSpec || ''
      formData.productUnit = data.productUnit || ''
      formData.outputQuantity = data.outputQuantity ?? 1
      formData.bomVersion = data.bomVersion || ''
      formData.createTime = data.createTime || ''
    }
    // 加载明细
    try {
      const itemsRes = await request.get(`/erp/stock/assemble/${idParam.value}/items`)
      const items = itemsRes?.data || []
      formData.items = items.map((item: any) => ({
        _key: nextKey(),
        productId: item.productId,
        productCode: item.productCode || '',
        productName: item.productName || '',
        spec: item.spec || '',
        unit: item.unit || '',
        baseQty: item.quantity ?? 1,
        wastageRate: item.wastageRate ?? 0,
        requiredQty: item.requiredQty ?? (item.quantity ?? 1) * (formData.assembleQuantity || 1),
        unitCost: item.unitCost ?? 0,
        availableStock: item.availableStock,
      }))
    } catch { formData.items = [] }
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

onMounted(async () => {
  await Promise.all([loadBomTemplates(), loadWarehouses()])
  await fetchDetail()
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
.empty-hint { color: #999; padding: 12px; text-align: center; }
.summary-row {
  display: flex;
  gap: 24px;
  padding: 8px 12px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
}
.summary-row b { color: #303133; }
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  height: 28px;
  line-height: 28px;
}
</style>
