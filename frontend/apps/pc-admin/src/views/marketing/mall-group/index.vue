<template>
  <ARReportPage
    ref="reportRef"
    title="拼团活动"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="拼团活动"
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
        </template>新增拼团
      </a-button>
    </template>
    <template #bodyCell="{ column, record, text }">
      <template v-if="column.dataIndex === 'productId'">
        {{ productName(text) }}
      </template>
      <template v-else-if="['originalPrice', 'groupPrice'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
      <template v-else-if="column.dataIndex === 'groupSize'">
        {{ record.minGroupSize ?? '-' }} ~ {{ record.maxGroupSize ?? '-' }} 人
      </template>
      <template v-else-if="column.dataIndex === 'progress'">
        {{ formatNum(record.soldQuantity) }} / {{ formatNum(record.totalQuantity) }}
      </template>
      <template v-else-if="column.dataIndex === 'period'">
        {{ fmtTime(record.startTime) }} ~ {{ fmtTime(record.endTime) }}
      </template>
      <template v-else-if="column.dataIndex === 'status'">
        <a-tag :color="STATUS_MAP[text]?.color">
          {{ STATUS_MAP[text]?.label || text || '-' }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'action'">
        <a-space>
          <a @click="openParticipants(record)">参与记录</a>
          <a-divider type="vertical" />
          <a @click="openEdit(record)">编辑</a>
          <a-divider type="vertical" />
          <a-dropdown>
            <a @click.prevent>状态</a>
            <template #overlay>
              <a-menu @click="(info: any) => handleStatus(record, String(info.key))">
                <a-menu-item
                  v-for="(v, k) in STATUS_MAP"
                  :key="k"
                  :disabled="record.status === k"
                >
                  {{ v.label }}
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>
    </template>
  </ARReportPage>

  <!-- 新增/编辑拼团弹窗 -->
  <a-modal
    v-model:open="modalVisible"
    :title="editingActivity ? '编辑拼团活动' : '新增拼团活动'"
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
        label="活动名称"
        required
      >
        <a-input
          v-model:value="modalForm.activityName"
          placeholder="请输入活动名称"
        />
      </a-form-item>
      <a-form-item
        label="拼团商品"
        required
      >
        <a-select
          v-model:value="modalForm.productId"
          :options="productOptions"
          placeholder="请选择商品"
          :filter-option="filterOption"
          show-search
        />
      </a-form-item>
      <a-form-item
        label="原价"
        required
      >
        <a-input-number
          v-model:value="modalForm.originalPrice"
          :min="0"
          :precision="2"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item
        label="拼团价"
        required
      >
        <a-input-number
          v-model:value="modalForm.groupPrice"
          :min="0"
          :precision="2"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="成团人数">
        <a-space>
          <a-input-number
            v-model:value="modalForm.minGroupSize"
            :min="2"
            placeholder="最少"
          />
          <span>~</span>
          <a-input-number
            v-model:value="modalForm.maxGroupSize"
            :min="2"
            placeholder="最多"
          />
        </a-space>
      </a-form-item>
      <a-form-item label="成团时限(分)">
        <a-input-number
          v-model:value="modalForm.timeLimitMinutes"
          :min="1"
          placeholder="如 1440 表示 24 小时"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="活动库存">
        <a-input-number
          v-model:value="modalForm.totalQuantity"
          :min="1"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item
        label="活动时间"
        required
      >
        <a-range-picker
          v-model:value="modalForm.timeRange"
          show-time
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="备注">
        <a-textarea
          v-model:value="modalForm.remark"
          :rows="2"
          placeholder="选填"
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 参与记录抽屉 -->
  <a-drawer
    v-model:open="participantsVisible"
    :title="`参与记录 - ${participantsActivity?.activityName || ''}`"
    :width="640"
  >
    <a-table
      :columns="participantColumns"
      :data-source="participants"
      :loading="participantsLoading"
      row-key="id"
      size="small"
      :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }"
    >
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.dataIndex === 'isCreator'">
          <a-tag :color="record.isCreator === 1 ? 'gold' : 'default'">
            {{ record.isCreator === 1 ? '团长' : '团员' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'groupStatus'">
          <a-tag :color="GROUP_STATUS_MAP[text]?.color">
            {{ GROUP_STATUS_MAP[text]?.label || text || '-' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'joinTime'">
          {{ fmtTime(text) }}
        </template>
      </template>
    </a-table>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { groupBuyApi, type GroupBuyActivity, type GroupBuyParticipant } from '@/api/marketing'
import { optionsApi, type OptionItem } from '@/api/options'

// ═══ 活动状态（后端为自由字符串，本页统一维护取值） ═══
const STATUS_MAP: Record<string, { label: string; color: string }> = {
  PENDING: { label: '未开始', color: 'orange' },
  ONGOING: { label: '进行中', color: 'green' },
  ENDED: { label: '已结束', color: 'default' },
  CANCELLED: { label: '已取消', color: 'red' }
}
const GROUP_STATUS_MAP: Record<string, { label: string; color: string }> = {
  FORMING: { label: '拼团中', color: 'orange' },
  SUCCESS: { label: '已成团', color: 'green' },
  FAILED: { label: '成团失败', color: 'red' }
}

const reportRef = ref<InstanceType<typeof ARReportPage>>()

const queryFields: ReportQueryField[] = [
  { key: 'activityName', type: 'input', label: '名称', placeholder: '活动名称（客户端过滤）' }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '活动编码', dataIndex: 'activityCode', key: 'activityCode', width: 130 },
  { title: '活动名称', dataIndex: 'activityName', key: 'activityName', width: 170, ellipsis: true },
  { title: '拼团商品', dataIndex: 'productId', key: 'productId', width: 150, ellipsis: true },
  { title: '原价', dataIndex: 'originalPrice', key: 'originalPrice', width: 100, align: 'right' },
  { title: '拼团价', dataIndex: 'groupPrice', key: 'groupPrice', width: 100, align: 'right' },
  { title: '成团人数', dataIndex: 'groupSize', key: 'groupSize', width: 110 },
  { title: '已售/库存', dataIndex: 'progress', key: 'progress', width: 110, align: 'right' },
  { title: '活动时间', dataIndex: 'period', key: 'period', width: 230 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
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

// ═══ 商品名称映射（/erp/product/page 真实端点） ═══
const productOptions = ref<{ label: string; value: number }[]>([])

function productName(productId: number | undefined): string {
  if (!productId) return '-'
  const p = productOptions.value.find(item => item.value === productId)
  return p?.label || `#${productId}`
}

function filterOption(input: string, option: any) {
  return (option?.label || '').toLowerCase().includes(input.toLowerCase())
}

// ═══ 数据请求（后端分页不支持名称过滤，名称关键字在归一化时客户端过滤） ═══
function fetcher(params: Record<string, any>) {
  const { activityName, ...rest } = params
  return groupBuyApi.page(rest).then(res => {
    if (activityName && res?.records) {
      const kw = String(activityName).toLowerCase()
      return { ...res, records: res.records.filter(r => (r.activityName || '').toLowerCase().includes(kw)) }
    }
    return res
  })
}

// ═══ 新增/编辑弹窗 ═══
const modalVisible = ref(false)
const modalLoading = ref(false)
const editingActivity = ref<GroupBuyActivity | null>(null)
const modalForm = reactive<{
  activityName?: string
  productId?: number
  originalPrice?: number
  groupPrice?: number
  minGroupSize?: number
  maxGroupSize?: number
  timeLimitMinutes?: number
  totalQuantity?: number
  timeRange?: [Dayjs, Dayjs]
  remark?: string
}>({})

function openCreate() {
  editingActivity.value = null
  Object.assign(modalForm, { activityName: undefined, productId: undefined, originalPrice: undefined, groupPrice: undefined, minGroupSize: 2, maxGroupSize: 10, timeLimitMinutes: 1440, totalQuantity: undefined, timeRange: undefined, remark: undefined })
  modalVisible.value = true
}

function openEdit(record: any) {
  editingActivity.value = record
  Object.assign(modalForm, {
    activityName: record.activityName,
    productId: record.productId,
    originalPrice: record.originalPrice,
    groupPrice: record.groupPrice,
    minGroupSize: record.minGroupSize,
    maxGroupSize: record.maxGroupSize,
    timeLimitMinutes: record.timeLimitMinutes,
    totalQuantity: record.totalQuantity,
    timeRange: record.startTime && record.endTime ? [dayjs(record.startTime), dayjs(record.endTime)] : undefined,
    remark: record.remark
  })
  modalVisible.value = true
}

async function handleModalOk() {
  if (!modalForm.activityName) {
    message.warning('请输入活动名称')
    return
  }
  if (!modalForm.productId) {
    message.warning('请选择拼团商品')
    return
  }
  if (modalForm.originalPrice === undefined || modalForm.groupPrice === undefined) {
    message.warning('请输入原价与拼团价')
    return
  }
  if (!modalForm.timeRange?.[0] || !modalForm.timeRange?.[1]) {
    message.warning('请选择活动时间')
    return
  }
  modalLoading.value = true
  try {
    const payload: Partial<GroupBuyActivity> = {
      activityName: modalForm.activityName,
      productId: modalForm.productId,
      originalPrice: modalForm.originalPrice,
      groupPrice: modalForm.groupPrice,
      minGroupSize: modalForm.minGroupSize,
      maxGroupSize: modalForm.maxGroupSize,
      timeLimitMinutes: modalForm.timeLimitMinutes,
      totalQuantity: modalForm.totalQuantity,
      remark: modalForm.remark,
      startTime: modalForm.timeRange[0].format('YYYY-MM-DDTHH:mm:ss'),
      endTime: modalForm.timeRange[1].format('YYYY-MM-DDTHH:mm:ss')
    }
    if (editingActivity.value) {
      await groupBuyApi.update(editingActivity.value.id, payload)
      message.success('更新成功')
    } else {
      await groupBuyApi.create({ ...payload, status: 'PENDING', soldQuantity: 0 })
      message.success('创建成功')
    }
    modalVisible.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[拼团活动] 保存失败', e)
  } finally {
    modalLoading.value = false
  }
}

// ═══ 状态变更 ═══
async function handleStatus(record: any, status: string) {
  try {
    await groupBuyApi.updateStatus(record.id, status)
    message.success('状态已更新')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[拼团活动] 状态更新失败', e)
  }
}

// ═══ 参与记录抽屉 ═══
const participantsVisible = ref(false)
const participantsLoading = ref(false)
const participants = ref<GroupBuyParticipant[]>([])
const participantsActivity = ref<GroupBuyActivity | null>(null)

const participantColumns: any[] = [
  { title: '团编号', dataIndex: 'groupId', key: 'groupId', width: 130 },
  { title: '用户', dataIndex: 'userName', key: 'userName', width: 120 },
  { title: '角色', dataIndex: 'isCreator', key: 'isCreator', width: 80 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 70, align: 'right' },
  { title: '团状态', dataIndex: 'groupStatus', key: 'groupStatus', width: 100 },
  { title: '参团时间', dataIndex: 'joinTime', key: 'joinTime', width: 150 }
]

async function openParticipants(record: any) {
  participantsActivity.value = record
  participantsVisible.value = true
  participantsLoading.value = true
  try {
    participants.value = await groupBuyApi.participants(record.id)
  } catch (e) {
    participants.value = []
    console.warn('[拼团活动] 参与记录获取失败', e)
  } finally {
    participantsLoading.value = false
  }
}

onMounted(async () => {
  try {
    const list: OptionItem[] = await optionsApi.getProducts()
    productOptions.value = list.map(p => ({ label: `${p.name}(${p.code})`, value: p.id }))
  } catch (e) {
    console.warn('[拼团活动] 商品列表获取失败', e)
  }
})
</script>
