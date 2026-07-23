<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="客户跟进"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      page-param-style="pageNum"
      export-file-name="客户跟进"
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
          </template>新建跟进
        </a-button>
      </template>
      <template #bodyCell="{ column, text }">
        <template v-if="column.dataIndex === 'followUpType'">
          <a-tag :color="followUpTypeColor(text)">
            {{ followUpTypeText(text) }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'followUpResult'">
          <a-tag :color="followUpResultColor(text)">
            {{ followUpResultText(text) }}
          </a-tag>
        </template>
      </template>
    </ARReportPage>

    <!-- ═══ 新建跟进弹窗 ═══ -->
    <a-modal
      v-model:open="createVisible"
      title="新建跟进记录"
      :confirm-loading="createLoading"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleCreate"
    >
      <a-form
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item
          label="客户"
          required
        >
          <a-select
            v-model:value="createForm.customerId"
            placeholder="请选择客户"
            show-search
            :filter-option="filterCustomerOption"
            :options="customerOptions"
          />
        </a-form-item>
        <a-form-item
          label="跟进类型"
          required
        >
          <a-select
            v-model:value="createForm.followUpType"
            placeholder="请选择跟进类型"
            :options="FOLLOW_UP_TYPE_OPTIONS"
          />
        </a-form-item>
        <a-form-item label="联系人">
          <a-input
            v-model:value="createForm.contactName"
            placeholder="联系人姓名"
          />
        </a-form-item>
        <a-form-item label="联系电话">
          <a-input
            v-model:value="createForm.contactPhone"
            placeholder="联系电话"
          />
        </a-form-item>
        <a-form-item label="跟进日期">
          <a-date-picker
            v-model:value="createForm.followUpDate"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </a-form-item>
        <a-form-item
          label="跟进内容"
          required
        >
          <a-textarea
            v-model:value="createForm.content"
            placeholder="请输入跟进内容"
            :rows="3"
          />
        </a-form-item>
        <a-form-item label="跟进结果">
          <a-select
            v-model:value="createForm.followUpResult"
            placeholder="请选择跟进结果"
            :options="FOLLOW_UP_RESULT_OPTIONS"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="下一步行动">
          <a-input
            v-model:value="createForm.nextAction"
            placeholder="下一步行动计划"
          />
        </a-form-item>
        <a-form-item label="下次跟进">
          <a-date-picker
            v-model:value="createForm.nextFollowUpDate"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { followUpApi, crmCustomerApi, type FollowUpRecord } from '@/api/crm'

// ═══ 跟进类型/结果（与现有客户页保持一致） ═══
const FOLLOW_UP_TYPE_OPTIONS = [
  { label: '电话', value: 1 },
  { label: '拜访', value: 2 },
  { label: '邮件', value: 3 },
  { label: '微信', value: 4 },
  { label: '其他', value: 5 }
]
const FOLLOW_UP_RESULT_OPTIONS = [
  { label: '有意向', value: 1 },
  { label: '无意向', value: 2 },
  { label: '待跟进', value: 3 }
]
const TYPE_TEXT: Record<number, string> = { 1: '电话', 2: '拜访', 3: '邮件', 4: '微信', 5: '其他' }
const TYPE_COLOR: Record<number, string> = { 1: 'blue', 2: 'green', 3: 'purple', 4: 'cyan', 5: 'default' }
const RESULT_TEXT: Record<number, string> = { 1: '有意向', 2: '无意向', 3: '待跟进' }
const RESULT_COLOR: Record<number, string> = { 1: 'green', 2: 'red', 3: 'orange' }

function followUpTypeText(v: number | undefined): string {
  return (v && TYPE_TEXT[v]) || '-'
}
function followUpTypeColor(v: number | undefined): string {
  return (v && TYPE_COLOR[v]) || 'default'
}
function followUpResultText(v: number | undefined): string {
  return (v && RESULT_TEXT[v]) || '-'
}
function followUpResultColor(v: number | undefined): string {
  return (v && RESULT_COLOR[v]) || 'default'
}

// ═══ 客户下拉（/api/customer/dropdown） ═══
const customerOptions = ref<{ label: string; value: number }[]>([])

function filterCustomerOption(input: string, option: any): boolean {
  return String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())
}

// ═══ 查询区（后端 /crm/followUp/page 仅支持 customerId 等精确条件） ═══
const queryFields = computed<ReportQueryField[]>(() => [
  {
    key: 'customerId',
    type: 'select',
    label: '客户',
    placeholder: '全部客户',
    width: 220,
    options: customerOptions.value
  }
])

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '跟进单号', dataIndex: 'followUpCode', key: 'followUpCode', width: 150 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 160, ellipsis: true },
  { title: '类型', dataIndex: 'followUpType', key: 'followUpType', width: 80 },
  { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 100 },
  { title: '跟进日期', dataIndex: 'followUpDate', key: 'followUpDate', width: 110 },
  { title: '跟进内容', dataIndex: 'content', key: 'content', ellipsis: true },
  { title: '跟进结果', dataIndex: 'followUpResult', key: 'followUpResult', width: 90 },
  { title: '下次跟进', dataIndex: 'nextFollowUpDate', key: 'nextFollowUpDate', width: 110 },
  { title: '跟进人', dataIndex: 'salesPersonName', key: 'salesPersonName', width: 100 }
]

// ═══ 数据请求 ═══
const reportRef = ref<InstanceType<typeof ARReportPage>>()

function fetcher(params: Record<string, any>) {
  return followUpApi.page(params)
}

// ═══ 新建跟进 ═══
const createVisible = ref(false)
const createLoading = ref(false)
const createForm = reactive({
  customerId: undefined as number | undefined,
  followUpType: 1,
  contactName: '',
  contactPhone: '',
  followUpDate: undefined as string | undefined,
  content: '',
  followUpResult: undefined as number | undefined,
  nextAction: '',
  nextFollowUpDate: undefined as string | undefined
})

function openCreate() {
  createForm.customerId = undefined
  createForm.followUpType = 1
  createForm.contactName = ''
  createForm.contactPhone = ''
  createForm.followUpDate = undefined
  createForm.content = ''
  createForm.followUpResult = undefined
  createForm.nextAction = ''
  createForm.nextFollowUpDate = undefined
  createVisible.value = true
}

async function handleCreate() {
  if (!createForm.customerId) {
    message.warning('请选择客户')
    return
  }
  if (!createForm.content.trim()) {
    message.warning('请输入跟进内容')
    return
  }
  const customer = customerOptions.value.find(o => o.value === createForm.customerId)
  const payload: Partial<FollowUpRecord> = {
    customerId: createForm.customerId,
    customerName: customer?.label,
    followUpType: createForm.followUpType,
    followUpTypeDesc: TYPE_TEXT[createForm.followUpType],
    contactName: createForm.contactName || undefined,
    contactPhone: createForm.contactPhone || undefined,
    followUpDate: createForm.followUpDate,
    content: createForm.content.trim(),
    followUpResult: createForm.followUpResult,
    followUpResultDesc: createForm.followUpResult ? RESULT_TEXT[createForm.followUpResult] : undefined,
    nextAction: createForm.nextAction || undefined,
    nextFollowUpDate: createForm.nextFollowUpDate
  }
  createLoading.value = true
  try {
    await followUpApi.create(payload)
    message.success('跟进记录已保存')
    createVisible.value = false
    reportRef.value?.reload()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    createLoading.value = false
  }
}

onMounted(async () => {
  try {
    const list = await crmCustomerApi.dropdown()
    customerOptions.value = (Array.isArray(list) ? list : []).map(c => ({
      label: c.name,
      value: c.id
    }))
  } catch (e) {
    console.warn('[客户跟进] 客户下拉获取失败', e)
  }
})
</script>
