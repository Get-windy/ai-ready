<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="拜访执行"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      export-file-name="拜访执行"
      row-key="id"
      empty-text="暂无拜访执行数据"
    >
      <template #header-extra>
        <a-button
          type="primary"
          @click="openCheckIn"
        >
          <template #icon>
            <EnvironmentOutlined />
          </template>签到打卡
        </a-button>
      </template>
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.dataIndex === 'visitType'">
          <a-tag :color="VISIT_TYPE_MAP[text]?.color || 'default'">
            {{ VISIT_TYPE_MAP[text]?.label || '-' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'result'">
          <a-tag :color="RESULT_MAP[text]?.color || 'default'">
            {{ RESULT_MAP[text]?.label || '-' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'visitTime'">
          {{ formatTime(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-space>
            <a-button
              type="link"
              size="small"
              @click="openEdit(record)"
            >
              编辑
            </a-button>
            <a-popconfirm
              title="确认删除该拜访记录？"
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

    <a-modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑拜访记录' : '签到打卡'"
      :confirm-loading="saving"
      width="560px"
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
          label="关联计划"
          name="planId"
        >
          <a-select
            v-model:value="form.planId"
            placeholder="无计划临时拜访可不选"
            allow-clear
            show-search
            option-filter-prop="label"
            :options="planOptions"
            @change="handlePlanChange"
          />
        </a-form-item>
        <a-form-item
          label="客户"
          name="customerId"
        >
          <a-select
            v-model:value="form.customerId"
            placeholder="请选择客户"
            show-search
            option-filter-prop="label"
            :options="customerOptions"
          />
        </a-form-item>
        <a-form-item
          label="负责人"
          name="salesPersonId"
        >
          <a-select
            v-model:value="form.salesPersonId"
            placeholder="请选择负责人"
            show-search
            option-filter-prop="label"
            :options="salesPersonOptions"
          />
        </a-form-item>
        <a-form-item
          label="拜访方式"
          name="visitType"
        >
          <a-select
            v-model:value="form.visitType"
            placeholder="请选择拜访方式"
            :options="visitTypeOptions"
          />
        </a-form-item>
        <a-form-item
          label="打卡时间"
          name="visitTime"
        >
          <a-date-picker
            v-model:value="form.visitTime"
            show-time
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
            placeholder="默认当前时间"
          />
        </a-form-item>
        <a-form-item
          label="拜访地点"
          name="location"
        >
          <a-input
            v-model:value="form.location"
            placeholder="请输入拜访地点"
          />
        </a-form-item>
        <a-form-item
          label="拜访内容"
          name="content"
        >
          <a-textarea
            v-model:value="form.content"
            :rows="3"
            placeholder="请输入拜访内容"
          />
        </a-form-item>
        <a-form-item
          label="拜访结果"
          name="result"
        >
          <a-select
            v-model:value="form.result"
            placeholder="请选择拜访结果"
            :options="resultOptions"
          />
        </a-form-item>
        <a-form-item
          label="下一步行动"
          name="nextAction"
        >
          <a-input
            v-model:value="form.nextAction"
            placeholder="请输入下一步行动（选填）"
          />
        </a-form-item>
        <a-form-item
          label="下次拜访日期"
          name="nextVisitDate"
        >
          <a-date-picker
            v-model:value="form.nextVisitDate"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            placeholder="请选择下次拜访日期（选填）"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { EnvironmentOutlined } from '@ant-design/icons-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import {
  visitPlanApi, visitRecordApi, crmCustomerApi,
  type VisitPlan, type VisitRecord
} from '@/api/crm'
import { userApi } from '@/api/user'

// ═══ 拜访方式/结果（与后端 VisitRecord 注释一致） ═══
const VISIT_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '上门', color: 'blue' },
  2: { label: '电话', color: 'cyan' },
  3: { label: '其他', color: 'default' }
}
const RESULT_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '有意向', color: 'green' },
  2: { label: '一般', color: 'orange' },
  3: { label: '无意向', color: 'red' }
}
const visitTypeOptions = Object.entries(VISIT_TYPE_MAP).map(([value, v]) => ({
  label: v.label,
  value: Number(value)
}))
const resultOptions = Object.entries(RESULT_MAP).map(([value, v]) => ({
  label: v.label,
  value: Number(value)
}))

function formatTime(val: string | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

// ═══ 下拉选项 ═══
const customerOptions = ref<{ label: string; value: number }[]>([])
const salesPersonOptions = ref<{ label: string; value: number }[]>([])
/** 待执行/执行中的计划，供打卡关联 */
const activePlans = ref<VisitPlan[]>([])
const planOptions = computed(() =>
  activePlans.value.map(p => ({
    label: `${p.planNo || ''}｜${p.customerName || ''}｜${p.planDate || ''}`,
    value: p.id
  }))
)

const queryFields = computed<ReportQueryField[]>(() => [
  {
    key: 'customerId',
    type: 'select',
    label: '客户',
    placeholder: '全部客户',
    width: 220,
    options: customerOptions.value
  },
  {
    key: 'salesPersonId',
    type: 'select',
    label: '负责人',
    placeholder: '全部负责人',
    width: 180,
    options: salesPersonOptions.value
  },
  {
    key: 'result',
    type: 'select',
    label: '拜访结果',
    placeholder: '全部结果',
    width: 140,
    options: resultOptions
  },
  {
    key: 'visitDateRange',
    type: 'date-range',
    label: '拜访日期',
    startKey: 'visitDateStart',
    endKey: 'visitDateEnd',
    width: 240
  }
])

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 170, ellipsis: true },
  { title: '负责人', dataIndex: 'salesPersonName', key: 'salesPersonName', width: 100 },
  { title: '拜访方式', dataIndex: 'visitType', key: 'visitType', width: 90 },
  { title: '打卡时间', dataIndex: 'visitTime', key: 'visitTime', width: 130 },
  { title: '拜访地点', dataIndex: 'location', key: 'location', ellipsis: true },
  { title: '拜访内容', dataIndex: 'content', key: 'content', ellipsis: true },
  { title: '拜访结果', dataIndex: 'result', key: 'result', width: 90 },
  { title: '下一步行动', dataIndex: 'nextAction', key: 'nextAction', width: 150, ellipsis: true },
  { title: '下次拜访', dataIndex: 'nextVisitDate', key: 'nextVisitDate', width: 110 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 130, fixed: 'right' }
]

// ═══ 数据请求（GET /api/crm/visit/record/page，page/size 风格） ═══
function fetcher(params: Record<string, any>) {
  return visitRecordApi.page(params)
}

// ═══ 打卡/编辑弹窗 ═══
const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  planId: undefined as number | undefined,
  customerId: undefined as number | undefined,
  salesPersonId: undefined as number | undefined,
  visitType: 1 as number,
  visitTime: dayjs().format('YYYY-MM-DDTHH:mm:ss') as string,
  location: '',
  content: '',
  result: undefined as number | undefined,
  nextAction: '',
  nextVisitDate: undefined as string | undefined
})
const form = reactive(emptyForm())

const rules: Record<string, any> = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  salesPersonId: [{ required: true, message: '请选择负责人', trigger: 'change' }],
  visitType: [{ required: true, message: '请选择拜访方式', trigger: 'change' }],
  content: [{ required: true, message: '请输入拜访内容', trigger: 'blur' }],
  result: [{ required: true, message: '请选择拜访结果', trigger: 'change' }]
}

function resetForm(data?: Partial<VisitRecord>) {
  Object.assign(form, emptyForm(), data || {})
}

/** 关联计划后自动带出客户/地点 */
function handlePlanChange(planId: number | undefined) {
  const plan = activePlans.value.find(p => p.id === planId)
  if (plan) {
    form.customerId = plan.customerId
    form.salesPersonId = plan.salesPersonId
    if (plan.address && !form.location) form.location = plan.address
  }
}

function openCheckIn() {
  editingId.value = null
  resetForm()
  modalOpen.value = true
}

function openEdit(record: VisitRecord) {
  editingId.value = record.id
  resetForm({
    planId: record.planId,
    customerId: record.customerId,
    salesPersonId: record.salesPersonId,
    visitType: record.visitType ?? 1,
    visitTime: record.visitTime || dayjs().format('YYYY-MM-DDTHH:mm:ss'),
    location: record.location || '',
    content: record.content || '',
    result: record.result,
    nextAction: record.nextAction || '',
    nextVisitDate: record.nextVisitDate
  })
  modalOpen.value = true
}

/** 后端按实体原样保存，名称字段由前端按选中项回填 */
function resolveNames() {
  const customer = customerOptions.value.find(o => o.value === form.customerId)
  const salesPerson = salesPersonOptions.value.find(o => o.value === form.salesPersonId)
  return {
    customerName: customer?.label,
    salesPersonName: salesPerson?.label
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const payload = { ...form, ...resolveNames() }
    if (editingId.value) {
      await visitRecordApi.update(editingId.value, payload)
      message.success('拜访记录更新成功')
    } else {
      await visitRecordApi.checkIn(payload)
      message.success('打卡成功')
    }
    modalOpen.value = false
    reportRef.value?.reload()
    loadActivePlans()
  } catch (e) {
    console.warn('[拜访执行] 保存失败', e)
    message.error((e as Error)?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: VisitRecord) {
  try {
    await visitRecordApi.remove(record.id)
    message.success('删除成功')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[拜访执行] 删除失败', e)
    message.error((e as Error)?.message || '删除失败')
  }
}

// ═══ 下拉数据加载 ═══
async function loadActivePlans() {
  try {
    const res = await visitPlanApi.page({ page: 1, size: 200 })
    activePlans.value = (res?.records || []).filter(p => p.status === 0 || p.status === 1)
  } catch (e) {
    console.warn('[拜访执行] 计划下拉获取失败', e)
  }
}

onMounted(async () => {
  loadActivePlans()
  try {
    const list = await crmCustomerApi.dropdown()
    customerOptions.value = (Array.isArray(list) ? list : []).map(c => ({
      label: c.name,
      value: c.id
    }))
  } catch (e) {
    console.warn('[拜访执行] 客户下拉获取失败', e)
  }
  try {
    const res: any = await userApi.getPage({ pageNum: 1, pageSize: 200 })
    const list = res?.records || res?.data?.records || []
    salesPersonOptions.value = list.map((u: any) => ({
      label: u.nickname || u.username,
      value: u.id
    }))
  } catch (e) {
    console.warn('[拜访执行] 负责人下拉获取失败', e)
  }
})
</script>
