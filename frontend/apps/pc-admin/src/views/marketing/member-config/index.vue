<template>
  <ARReportPage
    ref="reportRef"
    title="会员配置"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="营销规则"
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
        </template>新增规则
      </a-button>
    </template>
    <template #bodyCell="{ column, record, text }">
      <template v-if="column.dataIndex === 'ruleType'">
        {{ RULE_TYPE_MAP[text] || text || '-' }}
      </template>
      <template v-else-if="column.dataIndex === 'period'">
        {{ fmtTime(record.startTime) }} ~ {{ fmtTime(record.endTime) }}
      </template>
      <template v-else-if="column.dataIndex === 'minOrderAmount'">
        {{ formatMoney(text) }}
      </template>
      <template v-else-if="column.dataIndex === 'maxDiscountAmount'">
        {{ formatMoney(text) }}
      </template>
      <template v-else-if="column.dataIndex === 'isStackable'">
        <a-tag :color="record.isStackable === 1 ? 'green' : 'default'">
          {{ record.isStackable === 1 ? '可叠加' : '不可叠加' }}
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
            title="确认删除该规则？"
            @confirm="handleDelete(record)"
          >
            <a class="text-danger">删除</a>
          </a-popconfirm>
        </a-space>
      </template>
    </template>
  </ARReportPage>

  <!-- 新增/编辑规则弹窗 -->
  <a-modal
    v-model:open="modalVisible"
    :title="editingRule ? '编辑规则' : '新增规则'"
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
        label="规则编码"
        required
      >
        <a-input
          v-model:value="modalForm.ruleCode"
          placeholder="请输入规则编码"
          :disabled="!!editingRule"
        />
      </a-form-item>
      <a-form-item
        label="规则名称"
        required
      >
        <a-input
          v-model:value="modalForm.ruleName"
          placeholder="请输入规则名称"
        />
      </a-form-item>
      <a-form-item label="规则类型">
        <a-select
          v-model:value="modalForm.ruleType"
          :options="ruleTypeOptions"
          placeholder="请选择"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="优先级">
        <a-input-number
          v-model:value="modalForm.priority"
          :min="0"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="生效时间">
        <a-range-picker
          v-model:value="modalForm.timeRange"
          show-time
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="订单金额门槛">
        <a-input-number
          v-model:value="modalForm.minOrderAmount"
          :min="0"
          :precision="2"
          placeholder="最低订单金额"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="最大优惠金额">
        <a-input-number
          v-model:value="modalForm.maxDiscountAmount"
          :min="0"
          :precision="2"
          placeholder="优惠上限"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="可叠加">
        <a-switch
          v-model:checked="modalForm.isStackable"
          checked-children="是"
          un-checked-children="否"
        />
      </a-form-item>
      <a-form-item label="状态">
        <a-radio-group v-model:value="modalForm.status">
          <a-radio value="ACTIVE">
            启用
          </a-radio>
          <a-radio value="INACTIVE">
            停用
          </a-radio>
        </a-radio-group>
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
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { marketingRuleApi, type MarketingRule } from '@/api/marketing'

// ═══ 规则类型/状态字典 ═══
const RULE_TYPE_MAP: Record<string, string> = {
  DISCOUNT: '折扣',
  FULL_REDUCTION: '满减',
  GIFT: '赠品',
  POINTS: '积分',
  FREIGHT: '运费'
}
const STATUS_MAP: Record<string, { label: string; color: string }> = {
  ACTIVE: { label: '启用', color: 'green' },
  INACTIVE: { label: '停用', color: 'default' }
}
const ruleTypeOptions = Object.entries(RULE_TYPE_MAP).map(([value, label]) => ({ label, value }))

const reportRef = ref<InstanceType<typeof ARReportPage>>()

const queryFields: ReportQueryField[] = [
  { key: 'ruleType', type: 'select', label: '规则类型', placeholder: '全部类型', options: ruleTypeOptions },
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
  { title: '规则编码', dataIndex: 'ruleCode', key: 'ruleCode', width: 130 },
  { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName', width: 160, ellipsis: true },
  { title: '类型', dataIndex: 'ruleType', key: 'ruleType', width: 90 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 80, align: 'right' },
  { title: '生效时间', dataIndex: 'period', key: 'period', width: 230 },
  { title: '金额门槛', dataIndex: 'minOrderAmount', key: 'minOrderAmount', width: 110, align: 'right' },
  { title: '优惠上限', dataIndex: 'maxDiscountAmount', key: 'maxDiscountAmount', width: 110, align: 'right' },
  { title: '叠加', dataIndex: 'isStackable', key: 'isStackable', width: 100 },
  { title: '已用次数', dataIndex: 'useCount', key: 'useCount', width: 90, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '不限'
}

// ═══ 数据请求 ═══
function fetcher(params: Record<string, any>) {
  return marketingRuleApi.page(params)
}

// ═══ 新增/编辑弹窗 ═══
const modalVisible = ref(false)
const modalLoading = ref(false)
const editingRule = ref<MarketingRule | null>(null)
const modalForm = reactive<{
  ruleCode?: string
  ruleName?: string
  ruleType?: string
  priority?: number
  timeRange?: [Dayjs, Dayjs]
  minOrderAmount?: number
  maxDiscountAmount?: number
  isStackable?: boolean
  status?: string
  remark?: string
}>({})

function openCreate() {
  editingRule.value = null
  Object.assign(modalForm, { ruleCode: undefined, ruleName: undefined, ruleType: undefined, priority: 0, timeRange: undefined, minOrderAmount: undefined, maxDiscountAmount: undefined, isStackable: false, status: 'ACTIVE', remark: undefined })
  modalVisible.value = true
}

function openEdit(record: any) {
  editingRule.value = record
  Object.assign(modalForm, {
    ruleCode: record.ruleCode,
    ruleName: record.ruleName,
    ruleType: record.ruleType,
    priority: record.priority,
    timeRange: record.startTime && record.endTime ? [dayjs(record.startTime), dayjs(record.endTime)] : undefined,
    minOrderAmount: record.minOrderAmount,
    maxDiscountAmount: record.maxDiscountAmount,
    isStackable: record.isStackable === 1,
    status: record.status || 'ACTIVE',
    remark: record.remark
  })
  modalVisible.value = true
}

async function handleModalOk() {
  if (!modalForm.ruleCode) {
    message.warning('请输入规则编码')
    return
  }
  if (!modalForm.ruleName) {
    message.warning('请输入规则名称')
    return
  }
  modalLoading.value = true
  try {
    const payload: Partial<MarketingRule> = {
      ruleCode: modalForm.ruleCode,
      ruleName: modalForm.ruleName,
      ruleType: modalForm.ruleType,
      priority: modalForm.priority,
      minOrderAmount: modalForm.minOrderAmount,
      maxDiscountAmount: modalForm.maxDiscountAmount,
      isStackable: modalForm.isStackable ? 1 : 0,
      status: modalForm.status,
      remark: modalForm.remark,
      startTime: modalForm.timeRange?.[0] ? modalForm.timeRange[0].format('YYYY-MM-DDTHH:mm:ss') : undefined,
      endTime: modalForm.timeRange?.[1] ? modalForm.timeRange[1].format('YYYY-MM-DDTHH:mm:ss') : undefined
    }
    if (editingRule.value) {
      await marketingRuleApi.update(editingRule.value.id, payload)
      message.success('更新成功')
    } else {
      await marketingRuleApi.create(payload)
      message.success('创建成功')
    }
    modalVisible.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[会员配置] 保存失败', e)
  } finally {
    modalLoading.value = false
  }
}

// ═══ 删除 ═══
async function handleDelete(record: any) {
  try {
    await marketingRuleApi.remove(record.id)
    message.success('删除成功')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[会员配置] 删除失败', e)
  }
}
</script>

<style scoped>
.text-danger {
  color: #ff4d4f;
}
</style>
