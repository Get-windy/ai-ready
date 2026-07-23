<template>
  <ErrorBoundary
    @reset="fetchDetail"
    @error="handleError"
  >
    <PageContainer title="生产模板（BOM）">
      <a-alert
        v-if="!isNew && formData.id"
        type="info"
        show-icon
        class="gap-alert"
        message="BOM版本为只读视图，如需修改请基于当前版本创建新版本。"
      />

      <!-- ═══ 基本信息面板 ═══ -->
      <div class="panel">
        <div class="panel-title">
          基本信息
          <a-tag
            v-if="formData.id"
            :color="BOM_TEMPLATE_STATUS[formData.status ?? 1]?.color || 'default'"
            class="panel-tag"
          >
            {{ BOM_TEMPLATE_STATUS[formData.status ?? 1]?.text || '-' }}
          </a-tag>
        </div>
        <a-form
          layout="inline"
          class="header-form"
          :model="formData"
        >
          <a-form-item label="BOM编号">
            <a-input
              v-model:value="formData.bomNo"
              style="width: 180px"
              placeholder="自动生成"
              :disabled="!isNew"
            />
          </a-form-item>
          <a-form-item
            label="BOM名称"
            required
          >
            <a-input
              v-model:value="formData.bomName"
              style="width: 260px"
              placeholder="请输入BOM名称"
              :disabled="!editable"
            />
          </a-form-item>
          <a-form-item
            label="成品"
            required
          >
            <a-select
              v-model:value="formData.productId"
              style="width: 260px"
              placeholder="请选择成品"
              show-search
              :filter-option="filterOption"
              :options="productOptions"
              :loading="productLoading"
              :disabled="!editable"
              @change="handleProductChange"
            />
          </a-form-item>
          <a-form-item label="BOM类型">
            <a-select
              v-model:value="formData.bomType"
              style="width: 140px"
              :options="bomTypeOptions"
              :disabled="!editable"
            />
          </a-form-item>
          <a-form-item label="版本号">
            <a-input
              v-model:value="formData.version"
              style="width: 140px"
              placeholder="V1.0"
              :disabled="!editable"
            />
          </a-form-item>
          <a-form-item label="产出数量">
            <a-input-number
              v-model:value="formData.outputQuantity"
              :min="1"
              :precision="2"
              style="width: 120px"
              :disabled="!editable"
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

      <!-- ═══ 组件物料明细面板 ═══ -->
      <div class="panel">
        <div class="panel-title">
          组件物料清单
          <a-button
            v-if="editable"
            size="small"
            type="link"
            @click="addComponentRow"
          >
            + 添加物料
          </a-button>
        </div>
        <a-table
          :columns="componentColumns"
          :data-source="formData.components"
          :loading="detailLoading"
          :pagination="false"
          row-key="_key"
          size="small"
          bordered
        >
          <template #bodyCell="{ column, record, index }">
            <!-- 序号 -->
            <template v-if="column.key === 'index'">
              {{ index + 1 }}
            </template>
            <!-- 物料选择 -->
            <template v-if="column.key === 'product'">
              <a-select
                v-model:value="record.productId"
                placeholder="搜索选择物料"
                show-search
                :filter-option="filterOption"
                style="width: 100%"
                :options="productOptions"
                :disabled="!editable"
                @change="(val: number) => handleComponentProductChange(val, index)"
              />
            </template>
            <!-- 编码 -->
            <template v-if="column.key === 'productCode'">
              {{ record.productCode || '-' }}
            </template>
            <!-- 规格 -->
            <template v-if="column.key === 'spec'">
              {{ record.spec || '-' }}
            </template>
            <!-- 用量 -->
            <template v-if="column.key === 'quantity'">
              <a-input-number
                v-model:value="record.quantity"
                :min="0"
                :precision="2"
                style="width: 100%"
                size="small"
                :disabled="!editable"
                @change="recalcRowCost(record)"
              />
            </template>
            <!-- 损耗率(%) -->
            <template v-if="column.key === 'wastageRate'">
              <a-input-number
                v-model:value="record.wastageRate"
                :min="0"
                :max="100"
                :precision="2"
                style="width: 100%"
                size="small"
                :disabled="!editable"
              >
                <template #addonAfter>%</template>
              </a-input-number>
            </template>
            <!-- 单位成本 -->
            <template v-if="column.key === 'unitCost'">
              <a-input-number
                v-model:value="record.unitCost"
                :min="0"
                :precision="4"
                style="width: 100%"
                size="small"
                :disabled="!editable"
                @change="recalcRowCost(record)"
              />
            </template>
            <!-- 成本小计 -->
            <template v-if="column.key === 'cost'">
              ¥{{ ((record.cost ?? 0)).toFixed(2) }}
            </template>
            <!-- 操作 -->
            <template v-if="column.key === 'action'">
              <a-button
                v-if="editable"
                type="link"
                danger
                size="small"
                @click="removeComponentRow(index)"
              >
                删除
              </a-button>
            </template>
          </template>
        </a-table>

        <!-- 汇总行 -->
        <div class="summary-row">
          <span>物料种类: <b>{{ componentCount }}</b></span>
          <span>物料总用量: <b>{{ totalComponentQty }}</b></span>
          <span>BOM总成本: <b>¥{{ totalBomCost.toFixed(2) }}</b></span>
        </div>
      </div>

      <!-- ═══ 操作按钮 ═══ -->
      <div class="btn-row">
        <a-space>
          <a-button
            v-if="isNew || editable"
            type="primary"
            :loading="saving"
            @click="handleSaveAsDraft"
          >
            保存草稿
          </a-button>
          <a-button
            v-if="isNew || editable"
            type="primary"
            :loading="publishing"
            @click="handlePublish"
          >
            发布
          </a-button>
          <a-button
            v-if="!isNew && formData.status === 1"
            danger
            :loading="obsoleting"
            @click="handleObsolete"
          >
            作废
          </a-button>
          <a-button
            v-if="!isNew && formData.status === 0"
            type="primary"
            ghost
            :loading="publishing"
            @click="handlePublish"
          >
            发布
          </a-button>
          <a-button @click="handleBack">
            返回
          </a-button>
        </a-space>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import request from '@/utils/request'
import { BOM_STATUS } from '@/utils/statusConfig'

defineOptions({ name: 'ProductionTemplateForm' })

// ── 状态映射 ──────────────────────────────────────────
// 0=草稿 1=已发布 2=已作废
const BOM_TEMPLATE_STATUS: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '已发布', color: 'success' },
  2: { text: '已作废', color: 'error' },
}

const bomTypeOptions = [
  { label: '组装BOM', value: 1 },
  { label: '拆分BOM', value: 2 },
  { label: '通用', value: 3 },
]

// ── 路由 ──────────────────────────────────────────────
const router = useRouter()
const route = useRoute()
const idParam = computed(() => route.params.id as string)
const isNew = computed(() => !idParam.value || idParam.value === 'new')

// ── 选项数据 ──────────────────────────────────────────
const productOptions = ref<any[]>([])
const productLoading = ref(false)

async function loadProducts() {
  productLoading.value = true
  try {
    const res = await request.get('/erp/product/list', { params: { pageSize: 500 } })
    const data = res?.data || res || []
    productOptions.value = (Array.isArray(data) ? data : data?.records || []).map((p: any) => ({
      label: `${p.code || p.productCode || ''} - ${p.name || p.productName || ''}`,
      value: p.id,
      code: p.code || p.productCode || '',
      name: p.name || p.productName || '',
      spec: p.spec || p.specification || '',
      unit: p.unit || '',
      purchasePrice: p.purchasePrice || 0,
    }))
  } catch { productOptions.value = [] }
  finally { productLoading.value = false }
}

// ── 表单数据 ──────────────────────────────────────────
let keyCounter = 0
function nextKey() { return `comp_${++keyCounter}_${Date.now()}` }

const formData = reactive({
  id: undefined as number | undefined,
  bomNo: '',
  bomName: '',
  productId: undefined as number | undefined,
  bomType: 1,
  version: 'V1.0',
  outputQuantity: 1,
  status: 0,
  remark: '',
  components: [] as any[],
  createTime: '',
  updateTime: '',
})

const editable = computed(() => isNew.value || formData.status === 0)

// ── 明细计算 ──────────────────────────────────────────
const componentCount = computed(() => formData.components.filter((c: any) => c.productId != null).length)
const totalComponentQty = computed(() => formData.components.reduce((s: number, c: any) => s + (c.quantity || 0), 0))
const totalBomCost = computed(() => formData.components.reduce((s: number, c: any) => s + (c.cost ?? 0), 0))

function recalcRowCost(record: any) {
  record.cost = (record.quantity || 0) * (record.unitCost || 0)
}

function handleComponentProductChange(productId: number, index: number) {
  const p = productOptions.value.find((x: any) => x.value === productId)
  const row = formData.components[index]
  if (p && row) {
    row.productCode = p.code
    row.productName = p.name
    row.spec = p.spec
    row.unit = p.unit
    if (!row.unitCost) row.unitCost = p.purchasePrice || 0
    recalcRowCost(row)
  }
}

function handleProductChange(_productId: number) {
  // 成品变更时清空物料，避免引用错误
  formData.components = []
  addComponentRow()
}

// ── 明细操作 ──────────────────────────────────────────
function addComponentRow() {
  formData.components.push({
    _key: nextKey(),
    productId: undefined,
    productCode: '',
    productName: '',
    spec: '',
    unit: '',
    quantity: 1,
    wastageRate: 0,
    unitCost: 0,
    cost: 0,
  })
}

function removeComponentRow(index: number) {
  formData.components.splice(index, 1)
}

// ── 表格列定义 ──────────────────────────────────────────
const componentColumns: any = [
  { title: '#', key: 'index', width: 40, align: 'center' },
  { title: '物料', key: 'product', width: 200 },
  { title: '物料编码', key: 'productCode', width: 100 },
  { title: '规格', key: 'spec', width: 100 },
  { title: '用量', key: 'quantity', width: 80, align: 'right' },
  { title: '损耗率', key: 'wastageRate', width: 100, align: 'right' },
  { title: '单位成本', key: 'unitCost', width: 100, align: 'right' },
  { title: '成本小计', key: 'cost', width: 100, align: 'right' },
  { title: '操作', key: 'action', width: 60, align: 'center' },
]

// ── 筛选 ──────────────────────────────────────────────
function filterOption(input: string, option: any) {
  return (option?.label?.toString() || '').toLowerCase().includes(input.toLowerCase())
}

// ── 加载详情 ──────────────────────────────────────────
const detailLoading = ref(false)

async function fetchDetail() {
  if (isNew.value) {
    addComponentRow()
    return
  }
  detailLoading.value = true
  try {
    const res = await request.get(`/wh/production-template/${idParam.value}`)
    const data = res?.data || res
    if (data) {
      formData.id = data.id
      formData.bomNo = data.bomNo || ''
      formData.bomName = data.bomName || ''
      formData.productId = data.productId
      formData.bomType = data.bomType ?? 1
      formData.version = data.version || 'V1.0'
      formData.outputQuantity = data.outputQuantity ?? 1
      formData.status = data.status ?? 0
      formData.remark = data.remark || ''
      formData.createTime = data.createTime || ''
      formData.updateTime = data.updateTime || ''
    }
    // 加载组件明细
    try {
      const itemsRes = await request.get(`/wh/production-template/${idParam.value}/components`)
      const items = itemsRes?.data || []
      formData.components = items.map((item: any) => ({
        _key: nextKey(),
        id: item.id,
        productId: item.productId,
        productCode: item.productCode || '',
        productName: item.productName || '',
        spec: item.spec || '',
        unit: item.unit || '',
        quantity: item.quantity ?? 1,
        wastageRate: item.wastageRate ?? 0,
        unitCost: item.unitCost ?? 0,
        cost: item.cost ?? ((item.quantity ?? 0) * (item.unitCost ?? 0)),
      }))
    } catch { formData.components = [] }
  } catch {
    message.error('加载BOM详情失败')
  } finally {
    detailLoading.value = false
  }
}

// ── 保存/发布/作废 ──────────────────────────────────────
const saving = ref(false)
const publishing = ref(false)
const obsoleting = ref(false)

function validate(): boolean {
  if (!formData.bomName.trim()) { message.warning('请输入BOM名称'); return false }
  if (!formData.productId) { message.warning('请选择成品'); return false }
  const validComponents = formData.components.filter((c: any) => c.productId != null)
  if (validComponents.length === 0) { message.warning('请添加至少一个物料组件'); return false }
  for (const c of validComponents) {
    if (!c.quantity || c.quantity <= 0) { message.warning('物料用量必须大于0'); return false }
  }
  return true
}

function buildPayload(status: number) {
  const validComponents = formData.components.filter((c: any) => c.productId != null)
  return {
    bomNo: formData.bomNo || undefined,
    bomName: formData.bomName.trim(),
    productId: formData.productId,
    bomType: formData.bomType,
    version: formData.version,
    outputQuantity: formData.outputQuantity,
    status,
    remark: formData.remark || undefined,
    components: validComponents.map((c: any) => ({
      id: c.id,
      productId: c.productId,
      productCode: c.productCode,
      productName: c.productName,
      spec: c.spec,
      unit: c.unit,
      quantity: c.quantity,
      wastageRate: c.wastageRate,
      unitCost: c.unitCost,
    })),
  }
}

async function handleSaveAsDraft() {
  if (!validate()) return
  saving.value = true
  try {
    const payload = buildPayload(0)
    if (formData.id) {
      await request.put(`/wh/production-template/${formData.id}`, payload)
      message.success('BOM已更新')
    } else {
      const res = await request.post('/wh/production-template', payload)
      formData.id = res?.data?.id || res?.id
      message.success('BOM草稿已保存')
    }
    handleBack()
  } catch (err: any) {
    message.error(err?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handlePublish() {
  if (!validate()) return
  publishing.value = true
  try {
    const payload = buildPayload(1)
    if (formData.id) {
      await request.put(`/wh/production-template/${formData.id}`, payload)
      // 发布确认
      await request.post(`/wh/production-template/${formData.id}/publish`)
    } else {
      const res = await request.post('/wh/production-template', payload)
      formData.id = res?.data?.id || res?.id
      if (formData.id) {
        await request.post(`/wh/production-template/${formData.id}/publish`)
      }
    }
    message.success('BOM已发布')
    handleBack()
  } catch (err: any) {
    message.error(err?.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

async function handleObsolete() {
  if (!formData.id) return
  Modal.confirm({
    title: '确认作废',
    content: `确认将BOM ${formData.bomNo} 标记为作废吗？`,
    okText: '确认作废',
    okButtonProps: { danger: true },
    onOk: async () => {
      obsoleting.value = true
      try {
        await request.post(`/wh/production-template/${formData.id}/obsolete`)
        message.success('BOM已作废')
        handleBack()
      } catch (err: any) {
        message.error(err?.message || '操作失败')
      } finally {
        obsoleting.value = false
      }
    },
  })
}

function handleBack() {
  router.push('/wh/production-template')
}

function handleError(err: any) {
  console.warn('[生产模板] ErrorBoundary 捕获:', err)
}

onMounted(async () => {
  await loadProducts()
  await fetchDetail()
})
</script>

<style scoped>
.gap-alert { margin-bottom: 12px; }
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
.summary-row {
  display: flex;
  gap: 24px;
  padding: 8px 12px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
}
.summary-row b { color: #1890ff; }
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  height: 28px;
  line-height: 28px;
}
</style>
