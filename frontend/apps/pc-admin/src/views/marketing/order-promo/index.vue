<template>
  <ARReportPage
    ref="reportRef"
    title="整单促销"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="整单促销"
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
        </template>新增满减
      </a-button>
    </template>
    <template #bodyCell="{ column, record, text }">
      <template v-if="['minAmount', 'reductionAmount'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
      <template v-else-if="column.dataIndex === 'period'">
        {{ fmtTime(record.startTime) }} ~ {{ fmtTime(record.endTime) }}
      </template>
      <template v-else-if="column.dataIndex === 'stackable'">
        <a-tag :color="record.stackable ? 'green' : 'default'">
          {{ record.stackable ? '可叠加' : '不可叠加' }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'status'">
        <a-tag :color="STATUS_MAP[text]?.color">
          {{ STATUS_MAP[text]?.label || text || '-' }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'action'">
        <a-space>
          <a @click="openEdit(record)">编辑</a>
          <a-divider type="vertical" />
          <a-popconfirm
            v-if="record.status === 'draft'"
            title="确认发布该促销？"
            @confirm="handlePublish(record)"
          >
            <a>发布</a>
          </a-popconfirm>
          <a-popconfirm
            v-if="record.status === 'published'"
            title="确认取消该促销？"
            @confirm="handleCancel(record)"
          >
            <a class="text-warning">取消</a>
          </a-popconfirm>
          <a-popconfirm
            v-if="record.status === 'draft' || record.status === 'cancelled'"
            title="确认删除该促销？"
            @confirm="handleDelete(record)"
          >
            <a class="text-danger">删除</a>
          </a-popconfirm>
        </a-space>
      </template>
    </template>
  </ARReportPage>

  <!-- 新增/编辑满减弹窗 -->
  <a-modal
    v-model:open="modalVisible"
    :title="editingPromo ? '编辑整单促销' : '新增整单促销'"
    :confirm-loading="modalLoading"
    :width="560"
    @ok="handleModalOk"
    @cancel="modalVisible = false"
  >
    <a-form
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
      style="margin-top: 16px"
    >
      <a-form-item
        label="促销名称"
        required
      >
        <a-input
          v-model:value="modalForm.name"
          placeholder="如：满 1000 减 100"
        />
      </a-form-item>
      <a-form-item
        label="促销时间"
        required
      >
        <a-range-picker
          v-model:value="modalForm.timeRange"
          show-time
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item
        label="满减门槛"
        required
      >
        <a-input-number
          v-model:value="modalForm.minAmount"
          :min="0"
          :precision="2"
          placeholder="订单满 X 元"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item
        label="立减金额"
        required
      >
        <a-input-number
          v-model:value="modalForm.reductionAmount"
          :min="0"
          :precision="2"
          placeholder="减 Y 元"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="最大使用次数">
        <a-input-number
          v-model:value="modalForm.maxUsageCount"
          :min="1"
          placeholder="留空表示不限"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="每客户限次">
        <a-input-number
          v-model:value="modalForm.usageLimitPerCustomer"
          :min="1"
          placeholder="留空表示不限"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="可叠加">
        <a-switch
          v-model:checked="modalForm.stackable"
          checked-children="是"
          un-checked-children="否"
        />
      </a-form-item>
      <a-form-item label="促销描述">
        <a-textarea
          v-model:value="modalForm.description"
          :rows="2"
          placeholder="选填"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { promotionApi, type PromotionActivity } from '@/api/marketing'

// ═══ 促销状态（与后端 PromotionActivity.status 一致） ═══
const STATUS_MAP: Record<string, { label: string; color: string }> = {
  draft: { label: '草稿', color: 'default' },
  published: { label: '已发布', color: 'green' },
  expired: { label: '已过期', color: 'orange' },
  cancelled: { label: '已取消', color: 'red' }
}

const reportRef = ref<InstanceType<typeof ARReportPage>>()

const queryFields: ReportQueryField[] = [
  { key: 'name', type: 'input', label: '名称', placeholder: '促销名称' },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value }))
  }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '促销名称', dataIndex: 'name', key: 'name', width: 200, ellipsis: true },
  { title: '满减门槛', dataIndex: 'minAmount', key: 'minAmount', width: 110, align: 'right' },
  { title: '立减金额', dataIndex: 'reductionAmount', key: 'reductionAmount', width: 110, align: 'right' },
  { title: '促销时间', dataIndex: 'period', key: 'period', width: 240 },
  { title: '每客户限次', dataIndex: 'usageLimitPerCustomer', key: 'usageLimitPerCustomer', width: 100, align: 'right' },
  { title: '叠加', dataIndex: 'stackable', key: 'stackable', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 190, fixed: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

// ═══ 数据请求（固定 full_reduction 类型 = 整单满减） ═══
function fetcher(params: Record<string, any>) {
  return promotionApi.page({ ...params, type: 'full_reduction' })
}

// ═══ 新增/编辑弹窗 ═══
const modalVisible = ref(false)
const modalLoading = ref(false)
const editingPromo = ref<PromotionActivity | null>(null)
const modalForm = reactive<{
  name?: string
  timeRange?: [Dayjs, Dayjs]
  minAmount?: number
  reductionAmount?: number
  maxUsageCount?: number
  usageLimitPerCustomer?: number
  stackable?: boolean
  description?: string
}>({})

function openCreate() {
  editingPromo.value = null
  Object.assign(modalForm, { name: undefined, timeRange: undefined, minAmount: undefined, reductionAmount: undefined, maxUsageCount: undefined, usageLimitPerCustomer: undefined, stackable: false, description: undefined })
  modalVisible.value = true
}

function openEdit(record: any) {
  editingPromo.value = record
  Object.assign(modalForm, {
    name: record.name,
    timeRange: record.startTime && record.endTime ? [dayjs(record.startTime), dayjs(record.endTime)] : undefined,
    minAmount: record.minAmount,
    reductionAmount: record.reductionAmount,
    maxUsageCount: record.maxUsageCount,
    usageLimitPerCustomer: record.usageLimitPerCustomer,
    stackable: !!record.stackable,
    description: record.description
  })
  modalVisible.value = true
}

async function handleModalOk() {
  if (!modalForm.name) {
    message.warning('请输入促销名称')
    return
  }
  if (!modalForm.timeRange?.[0] || !modalForm.timeRange?.[1]) {
    message.warning('请选择促销时间')
    return
  }
  if (modalForm.minAmount === undefined || modalForm.minAmount === null) {
    message.warning('请输入满减门槛')
    return
  }
  if (!modalForm.reductionAmount) {
    message.warning('请输入立减金额')
    return
  }
  modalLoading.value = true
  try {
    const payload: Partial<PromotionActivity> = {
      name: modalForm.name,
      type: 'full_reduction',
      minAmount: modalForm.minAmount,
      reductionAmount: modalForm.reductionAmount,
      maxUsageCount: modalForm.maxUsageCount,
      usageLimitPerCustomer: modalForm.usageLimitPerCustomer,
      stackable: modalForm.stackable,
      description: modalForm.description,
      startTime: modalForm.timeRange[0].format('YYYY-MM-DDTHH:mm:ss'),
      endTime: modalForm.timeRange[1].format('YYYY-MM-DDTHH:mm:ss')
    }
    if (editingPromo.value) {
      await promotionApi.update(editingPromo.value.id, payload)
      message.success('更新成功')
    } else {
      await promotionApi.create(payload)
      message.success('创建成功')
    }
    modalVisible.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[整单促销] 保存失败', e)
  } finally {
    modalLoading.value = false
  }
}

// ═══ 发布 / 取消 / 删除 ═══
async function handlePublish(record: any) {
  try {
    await promotionApi.publish(record.id)
    message.success('发布成功')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[整单促销] 发布失败', e)
  }
}

async function handleCancel(record: any) {
  try {
    await promotionApi.cancel(record.id)
    message.success('已取消')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[整单促销] 取消失败', e)
  }
}

async function handleDelete(record: any) {
  try {
    await promotionApi.remove(record.id)
    message.success('删除成功')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[整单促销] 删除失败', e)
  }
}
</script>

<style scoped>
.text-danger {
  color: #ff4d4f;
}
.text-warning {
  color: #faad14;
}
</style>
