<template>
  <ARReportPage
    ref="reportRef"
    title="秒杀场次"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="秒杀场次"
    row-key="id"
  >
    <template #header-extra>
      <a-button
        type="primary"
        size="small"
        @click="openCreate"
      >
        <template #icon>
          <PlusOutlined />
        </template>新增秒杀
      </a-button>
    </template>
    <template #bodyCell="{ column, record, text }">
      <template v-if="column.dataIndex === 'productName'">
        <div class="product-cell">
          <span>{{ record.productName || productName(record.productId) }}</span>
          <span
            v-if="record.productCode"
            class="product-code"
          >{{ record.productCode }}</span>
        </div>
      </template>
      <template v-else-if="['flashPrice', 'originalPrice'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
      <template v-else-if="column.dataIndex === 'progress'">
        {{ formatNum(record.soldCount) }} / {{ formatNum(record.stockLimit) }}
      </template>
      <template v-else-if="column.dataIndex === 'startTime'">
        {{ fmtTime(text) }}
      </template>
      <template v-else-if="column.dataIndex === 'endTime'">
        {{ fmtTime(text) }}
      </template>
      <template v-else-if="column.dataIndex === 'status'">
        <a-tag :color="STATUS_MAP[text]?.color">
          {{ STATUS_MAP[text]?.label || text || '-' }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'action'">
        <a-space :size="4">
          <a-button
            type="link"
            size="small"
            @click="openParticipants(record)"
          >
            参与记录
          </a-button>
          <a-button
            v-if="[0, 1].includes(record.status)"
            type="link"
            size="small"
            @click="openEdit(record)"
          >
            编辑
          </a-button>
          <a-popconfirm
            v-if="record.status === 0"
            title="确认发布该秒杀场次？"
            ok-text="发布"
            cancel-text="取消"
            @confirm="handlePublish(record)"
          >
            <a-button
              type="link"
              size="small"
            >
              发布
            </a-button>
          </a-popconfirm>
          <a-popconfirm
            v-if="[0, 1].includes(record.status)"
            title="确认取消该秒杀场次？"
            ok-text="确定"
            cancel-text="再想想"
            @confirm="handleCancel(record)"
          >
            <a-button
              type="link"
              size="small"
              danger
            >
              取消
            </a-button>
          </a-popconfirm>
          <a-popconfirm
            v-if="[0, 2].includes(record.status)"
            title="确认删除该秒杀场次？"
            ok-text="删除"
            cancel-text="取消"
            @confirm="handleDelete(record)"
          >
            <a-button
              type="link"
              size="small"
              danger
            >
              删除
            </a-button>
          </a-popconfirm>
        </a-space>
      </template>
    </template>
  </ARReportPage>

  <!-- 新增/编辑秒杀弹窗 -->
  <a-modal
    v-model:open="modalOpen"
    :title="editingId ? '编辑秒杀场次' : '新增秒杀场次'"
    :confirm-loading="saving"
    :width="600"
    @ok="handleSave"
  >
    <a-form
      ref="formRef"
      :model="form"
      :rules="rules"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
      style="margin-top: 16px"
    >
      <a-form-item
        label="场次标题"
        name="title"
      >
        <a-input
          v-model:value="form.title"
          :maxlength="100"
          placeholder="如：618 限时秒杀专场"
        />
      </a-form-item>
      <a-form-item
        label="秒杀商品"
        name="productId"
      >
        <a-select
          v-model:value="form.productId"
          :options="productOptions"
          :filter-option="filterOption"
          show-search
          placeholder="搜索并选择商品"
          @change="handleProductChange"
        />
      </a-form-item>
      <a-form-item
        label="秒杀价"
        name="flashPrice"
      >
        <a-input-number
          v-model:value="form.flashPrice"
          :min="0"
          :precision="2"
          style="width: 100%"
          placeholder="秒杀活动价"
        />
      </a-form-item>
      <a-form-item
        label="原价"
        name="originalPrice"
      >
        <a-input-number
          v-model:value="form.originalPrice"
          :min="0"
          :precision="2"
          style="width: 100%"
          placeholder="选择商品后自动带出，可修改"
        />
      </a-form-item>
      <a-form-item
        label="场次库存"
        name="stockLimit"
      >
        <a-input-number
          v-model:value="form.stockLimit"
          :min="1"
          :precision="0"
          style="width: 100%"
          placeholder="秒杀限量库存"
        />
      </a-form-item>
      <a-form-item
        label="起止时间"
        name="timeRange"
      >
        <a-range-picker
          v-model:value="form.timeRange"
          show-time
          style="width: 100%"
          :placeholder="['开始时间', '结束时间']"
        />
      </a-form-item>
      <a-form-item
        label="排序"
        name="sort"
      >
        <a-input-number
          v-model:value="form.sort"
          :min="0"
          :precision="0"
          style="width: 100%"
          placeholder="数值越小越靠前"
        />
      </a-form-item>
      <a-form-item
        label="备注"
        name="remark"
      >
        <a-textarea
          v-model:value="form.remark"
          :rows="2"
          :maxlength="200"
          placeholder="选填"
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 参与记录抽屉 -->
  <a-drawer
    v-model:open="participantsOpen"
    :title="`参与记录 - ${participantsSession?.title || ''}`"
    :width="720"
  >
    <a-table
      :columns="participantColumns"
      :data-source="participants"
      :loading="participantsLoading"
      :pagination="participantsPagination"
      row-key="id"
      size="small"
      @change="handleParticipantsChange"
    >
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.dataIndex === 'quantity'">
          {{ formatNum(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'amount'">
          {{ formatMoney(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tag :color="PARTICIPANT_STATUS_MAP[text]?.color">
            {{ PARTICIPANT_STATUS_MAP[text]?.label || text || '-' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'orderId'">
          {{ text ? `#${text}` : '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ fmtTime(text) }}
        </template>
      </template>
    </a-table>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { flashSaleApi, type FlashSale, type FlashSaleParticipant } from '@/api/marketing'
import { optionsApi, type OptionItem } from '@/api/options'

// ═══ 场次状态（与后端 FlashSale.status 一致） ═══
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '已发布', color: 'green' },
  2: { label: '已取消', color: 'red' },
  3: { label: '已结束', color: 'blue' }
}

// ═══ 参与状态（与后端 FlashSaleOrder.status 一致） ═══
const PARTICIPANT_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '已参与', color: 'orange' },
  1: { label: '已下单', color: 'green' },
  2: { label: '已取消', color: 'red' }
}

const statusOptions = Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))

const reportRef = ref<InstanceType<typeof ARReportPage>>()

const queryFields: ReportQueryField[] = [
  { key: 'title', type: 'input', label: '标题', placeholder: '场次标题', width: 180 },
  { key: 'status', type: 'select', label: '状态', placeholder: '全部状态', options: statusOptions }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '场次标题', dataIndex: 'title', key: 'title', width: 180, ellipsis: true },
  { title: '秒杀商品', dataIndex: 'productName', key: 'productName', width: 200, ellipsis: true },
  { title: '秒杀价', dataIndex: 'flashPrice', key: 'flashPrice', width: 100, align: 'right' },
  { title: '原价', dataIndex: 'originalPrice', key: 'originalPrice', width: 100, align: 'right' },
  { title: '已售/库存', dataIndex: 'progress', key: 'progress', width: 110, align: 'right' },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 150 },
  { title: '结束时间', dataIndex: 'endTime', key: 'endTime', width: 150 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 70, align: 'right' },
  { title: '操作', key: 'action', width: 260, fixed: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatNum(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '0'
  return Number(val).toLocaleString('zh-CN')
}

function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

// ═══ 数据请求 ═══
function fetcher(params: Record<string, any>) {
  return flashSaleApi.page(params)
}

// ═══ 商品下拉（/erp/product/page 真实端点，与拼团页同一数据源） ═══
const productOptions = ref<{ label: string; value: number }[]>([])
const productMap = new Map<number, OptionItem>()

function productName(productId: number | undefined): string {
  if (!productId) return '-'
  const p = productMap.get(productId)
  return p?.name || `#${productId}`
}

function filterOption(input: string, option: any) {
  return (option?.label || '').toLowerCase().includes(input.toLowerCase())
}

function handleProductChange(value: any) {
  const p = productMap.get(Number(value))
  if (!p) return
  form.productCode = p.code || ''
  form.productName = p.name || ''
  if (form.originalPrice === undefined || form.originalPrice === null) {
    form.originalPrice = p.salePrice ?? undefined
  }
}

/** 编辑时若商品不在下拉缓存中，用场次快照补齐选项 */
function ensureProductOption(record: FlashSale) {
  if (!record.productId) return
  if (productOptions.value.some(o => o.value === record.productId)) return
  const label = `${record.productName || '商品 #' + record.productId}${record.productCode ? `(${record.productCode})` : ''}`
  productOptions.value = [{ label, value: record.productId }, ...productOptions.value]
}

// ═══ 新增/编辑弹窗 ═══
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  title: '' as string,
  productId: undefined as number | undefined,
  productCode: '' as string,
  productName: '' as string,
  flashPrice: undefined as number | undefined,
  originalPrice: undefined as number | undefined,
  stockLimit: undefined as number | undefined,
  timeRange: undefined as [Dayjs, Dayjs] | undefined,
  sort: 0 as number,
  remark: '' as string
})
const form = reactive(emptyForm())

const rules: Record<string, any> = {
  title: [{ required: true, message: '请输入场次标题', trigger: 'blur' }],
  productId: [{ required: true, message: '请选择秒杀商品', trigger: 'change' }],
  flashPrice: [{ required: true, message: '请输入秒杀价', trigger: 'blur' }],
  stockLimit: [{ required: true, message: '请输入场次库存', trigger: 'blur' }],
  timeRange: [{ required: true, message: '请选择起止时间', trigger: 'change' }]
}

function resetForm(data?: Partial<typeof form>) {
  Object.assign(form, emptyForm(), data || {})
}

function openCreate() {
  editingId.value = null
  resetForm()
  modalOpen.value = true
}

function openEdit(record: any) {
  editingId.value = record.id
  ensureProductOption(record)
  resetForm({
    title: record.title || '',
    productId: record.productId,
    productCode: record.productCode || '',
    productName: record.productName || '',
    flashPrice: record.flashPrice,
    originalPrice: record.originalPrice,
    stockLimit: record.stockLimit,
    timeRange: record.startTime && record.endTime ? [dayjs(record.startTime), dayjs(record.endTime)] : undefined,
    sort: record.sort ?? 0,
    remark: record.remark || ''
  })
  modalOpen.value = true
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  if (form.flashPrice !== undefined && form.originalPrice !== undefined && form.flashPrice > form.originalPrice) {
    message.warning('秒杀价不能高于原价')
    return
  }
  saving.value = true
  try {
    const payload: Partial<FlashSale> = {
      title: form.title,
      productId: form.productId,
      productCode: form.productCode,
      productName: form.productName,
      flashPrice: form.flashPrice,
      originalPrice: form.originalPrice,
      stockLimit: form.stockLimit,
      sort: form.sort,
      remark: form.remark,
      startTime: form.timeRange?.[0]?.format('YYYY-MM-DDTHH:mm:ss'),
      endTime: form.timeRange?.[1]?.format('YYYY-MM-DDTHH:mm:ss')
    }
    if (editingId.value) {
      await flashSaleApi.update(editingId.value, payload)
      message.success('秒杀场次更新成功')
    } else {
      await flashSaleApi.create(payload)
      message.success('秒杀场次创建成功')
    }
    modalOpen.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[秒杀场次] 保存失败', e)
  } finally {
    saving.value = false
  }
}

// ═══ 发布/取消/删除 ═══
async function handlePublish(record: any) {
  try {
    await flashSaleApi.publish(record.id)
    message.success('发布成功')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[秒杀场次] 发布失败', e)
  }
}

async function handleCancel(record: any) {
  try {
    await flashSaleApi.cancel(record.id)
    message.success('已取消')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[秒杀场次] 取消失败', e)
  }
}

async function handleDelete(record: any) {
  try {
    await flashSaleApi.remove(record.id)
    message.success('删除成功')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[秒杀场次] 删除失败', e)
  }
}

// ═══ 参与记录抽屉（后端分页） ═══
const participantsOpen = ref(false)
const participantsLoading = ref(false)
const participants = ref<FlashSaleParticipant[]>([])
const participantsSession = ref<FlashSale | null>(null)
const participantsPage = reactive({ current: 1, pageSize: 10, total: 0 })

const participantColumns: any[] = [
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 140, ellipsis: true },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80, align: 'right' },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 110, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '关联订单', dataIndex: 'orderId', key: 'orderId', width: 110 },
  { title: '参与时间', dataIndex: 'createTime', key: 'createTime', width: 150 }
]

const participantsPagination = computed(() => ({
  current: participantsPage.current,
  pageSize: participantsPage.pageSize,
  total: participantsPage.total,
  showTotal: (t: number) => `共 ${t} 条`
}))

async function loadParticipants() {
  if (!participantsSession.value) return
  participantsLoading.value = true
  try {
    const res = await flashSaleApi.participants(participantsSession.value.id, {
      pageNum: participantsPage.current,
      pageSize: participantsPage.pageSize
    })
    participants.value = res?.records || []
    participantsPage.total = Number(res?.total) || 0
  } catch (e) {
    participants.value = []
    participantsPage.total = 0
    console.warn('[秒杀场次] 参与记录获取失败', e)
  } finally {
    participantsLoading.value = false
  }
}

function openParticipants(record: any) {
  participantsSession.value = record
  participantsPage.current = 1
  participantsOpen.value = true
  loadParticipants()
}

function handleParticipantsChange(pag: { current?: number; pageSize?: number }) {
  participantsPage.current = pag.current || 1
  participantsPage.pageSize = pag.pageSize || 10
  loadParticipants()
}

onMounted(async () => {
  try {
    const list: OptionItem[] = await optionsApi.getProducts()
    productOptions.value = list.map(p => ({ label: `${p.name}${p.code ? `(${p.code})` : ''}`, value: p.id }))
    list.forEach(p => productMap.set(p.id, p))
  } catch (e) {
    console.warn('[秒杀场次] 商品列表获取失败', e)
  }
})
</script>

<style scoped>
.product-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}
.product-code {
  font-size: 12px;
  color: #999;
}
</style>
