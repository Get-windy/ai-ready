<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="会计期间"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      row-key="id"
    >
      <template #header-extra>
        <a-button
          type="primary"
          @click="openCreate"
        >
          <template #icon>
            <PlusOutlined />
          </template>新增期间
        </a-button>
      </template>
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="text === 1 ? 'green' : 'default'">
            {{ text === 1 ? '开启' : '关闭（已月结）' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'periodMonth'">
          {{ text }}月
        </template>
        <template v-else-if="['closedBy', 'closedTime', 'remark'].includes(column.dataIndex as string)">
          {{ text || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-popconfirm
            :title="record.status === 1 ? '确认关闭该期间？' : '确认开启该期间？'"
            :description="record.status === 1 ? `期间 ${record.periodCode} 关闭后不可录入/过账凭证` : `期间 ${record.periodCode} 将重新开启`"
            :ok-text="record.status === 1 ? '关闭' : '开启'"
            cancel-text="取消"
            @confirm="toggleStatus(record)"
          >
            <a-button
              type="link"
              size="small"
              :danger="record.status === 1"
            >
              {{ record.status === 1 ? '关闭' : '开启' }}
            </a-button>
          </a-popconfirm>
        </template>
      </template>
    </ARReportPage>

    <a-modal
      v-model:open="modalOpen"
      title="新增会计期间"
      :confirm-loading="saving"
      width="480px"
      @ok="handleSave"
    >
      <a-form
        ref="formRef"
        :model="form"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item
          label="会计年度"
          name="periodYear"
        >
          <a-input-number
            v-model:value="form.periodYear"
            :min="2000"
            :max="2099"
            :precision="0"
            style="width: 100%"
            placeholder="请输入会计年度"
          />
        </a-form-item>
        <a-form-item
          label="会计月份"
          name="periodMonth"
        >
          <a-select
            v-model:value="form.periodMonth"
            :options="monthOptions"
            placeholder="请选择会计月份"
          />
        </a-form-item>
        <a-form-item
          label="备注"
          name="remark"
        >
          <a-textarea
            v-model:value="form.remark"
            :rows="2"
            placeholder="请输入备注（选填）"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { accountingPeriodApi } from '@/api/finance'

const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)

// ═══ 查询字段 ═══
const queryFields: ReportQueryField[] = [
  { key: 'periodYear', type: 'input', label: '年度', placeholder: '会计年度', width: 140 },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: [
      { label: '开启', value: 1 },
      { label: '关闭（已月结）', value: 0 }
    ]
  }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '年度', dataIndex: 'periodYear', key: 'periodYear', width: 80 },
  { title: '月份', dataIndex: 'periodMonth', key: 'periodMonth', width: 70 },
  { title: '期间编码', dataIndex: 'periodCode', key: 'periodCode', width: 110 },
  { title: '开始日期', dataIndex: 'startDate', key: 'startDate', width: 110 },
  { title: '结束日期', dataIndex: 'endDate', key: 'endDate', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '月结操作人', dataIndex: 'closedBy', key: 'closedBy', width: 110 },
  { title: '月结时间', dataIndex: 'closedTime', key: 'closedTime', width: 170 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', dataIndex: 'action', key: 'action', width: 90, fixed: 'right' }
]

// ═══ 数据请求（后端分页参数为 page/size，与 ARReportPage 默认一致） ═══
function fetcher(params: Record<string, any>) {
  const { periodYear, ...rest } = params
  return accountingPeriodApi.getPage({
    ...rest,
    periodYear: periodYear !== undefined && periodYear !== '' ? Number(periodYear) : undefined
  })
}

// ═══ 新增期间弹窗 ═══
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)

const monthOptions = Array.from({ length: 12 }, (_, i) => ({ label: `${i + 1}月`, value: i + 1 }))

const emptyForm = () => ({
  periodYear: undefined as number | undefined,
  periodMonth: undefined as number | undefined,
  remark: ''
})
const form = reactive(emptyForm())

const rules: Record<string, any> = {
  periodYear: [{ required: true, message: '请输入会计年度', trigger: 'blur' }],
  periodMonth: [{ required: true, message: '请选择会计月份', trigger: 'change' }]
}

function openCreate() {
  Object.assign(form, emptyForm())
  modalOpen.value = true
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    await accountingPeriodApi.create({
      periodYear: form.periodYear!,
      periodMonth: form.periodMonth!,
      remark: form.remark || undefined
    })
    message.success('会计期间创建成功')
    modalOpen.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[会计期间] 创建失败', e)
  } finally {
    saving.value = false
  }
}

// ═══ 启停切换 ═══
async function toggleStatus(record: any) {
  const target = record.status === 1 ? 0 : 1
  try {
    await accountingPeriodApi.updateStatus(record.id, target)
    message.success(target === 1 ? `期间 ${record.periodCode} 已开启` : `期间 ${record.periodCode} 已关闭`)
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[会计期间] 状态切换失败', e)
  }
}
</script>
