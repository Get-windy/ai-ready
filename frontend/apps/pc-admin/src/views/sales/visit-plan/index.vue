<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="拜访规划"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      export-file-name="拜访规划"
      row-key="id"
      empty-text="暂无拜访计划数据"
    >
      <template #header-extra>
        <a-button
          type="primary"
          @click="openCreate"
        >
          <template #icon>
            <PlusOutlined />
          </template>新建计划
        </a-button>
      </template>
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="STATUS_MAP[text]?.color || 'default'">
            {{ STATUS_MAP[text]?.label || '-' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'planDate'">
          <span :class="{ 'overdue-plan': isOverdue(record) }">
            {{ text || '-' }}
          </span>
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
              v-if="record.status === 0 || record.status === 1"
              title="确认取消该拜访计划？"
              ok-text="取消计划"
              cancel-text="返回"
              @confirm="handleCancel(record)"
            >
              <a-button
                type="link"
                size="small"
              >
                取消计划
              </a-button>
            </a-popconfirm>
            <a-popconfirm
              title="确认删除该拜访计划？"
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
      :title="editingId ? '编辑拜访计划' : '新建拜访计划'"
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
          label="计划日期"
          name="planDate"
        >
          <a-date-picker
            v-model:value="form.planDate"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            placeholder="请选择计划拜访日期"
          />
        </a-form-item>
        <a-form-item
          label="计划时间"
          name="planTime"
        >
          <a-input
            v-model:value="form.planTime"
            placeholder="如 10:00-11:00（选填）"
          />
        </a-form-item>
        <a-form-item
          label="拜访目的"
          name="purpose"
        >
          <a-input
            v-model:value="form.purpose"
            placeholder="请输入拜访目的"
          />
        </a-form-item>
        <a-form-item
          label="拜访地址"
          name="address"
        >
          <a-input
            v-model:value="form.address"
            placeholder="请输入拜访地址（选填）"
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
import { ref, reactive, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { visitPlanApi, crmCustomerApi, type VisitPlan } from '@/api/crm'
import { userApi } from '@/api/user'

// ═══ 计划状态（0待执行 1执行中 2已完成 3已取消，与后端 VisitPlan 一致） ═══
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待执行', color: 'orange' },
  1: { label: '执行中', color: 'blue' },
  2: { label: '已完成', color: 'green' },
  3: { label: '已取消', color: 'red' }
}
const statusOptions = Object.entries(STATUS_MAP).map(([value, v]) => ({
  label: v.label,
  value: Number(value)
}))

function isOverdue(record: VisitPlan): boolean {
  return !!record.planDate && (record.status === 0 || record.status === 1) &&
    dayjs(record.planDate).isBefore(dayjs(), 'day')
}

// ═══ 下拉选项 ═══
const customerOptions = ref<{ label: string; value: number }[]>([])
const salesPersonOptions = ref<{ label: string; value: number }[]>([])

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
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    width: 140,
    options: statusOptions
  },
  {
    key: 'planDateRange',
    type: 'date-range',
    label: '计划日期',
    startKey: 'planDateStart',
    endKey: 'planDateEnd',
    width: 240
  }
])

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '计划编号', dataIndex: 'planNo', key: 'planNo', width: 150 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 170, ellipsis: true },
  { title: '负责人', dataIndex: 'salesPersonName', key: 'salesPersonName', width: 100 },
  { title: '计划日期', dataIndex: 'planDate', key: 'planDate', width: 110 },
  { title: '计划时间', dataIndex: 'planTime', key: 'planTime', width: 110 },
  { title: '拜访目的', dataIndex: 'purpose', key: 'purpose', ellipsis: true },
  { title: '拜访地址', dataIndex: 'address', key: 'address', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', dataIndex: 'action', key: 'action', width: 190, fixed: 'right' }
]

// ═══ 数据请求（GET /api/crm/visit/plan/page，page/size 风格） ═══
function fetcher(params: Record<string, any>) {
  return visitPlanApi.page(params)
}

// ═══ 新建/编辑弹窗 ═══
const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  customerId: undefined as number | undefined,
  salesPersonId: undefined as number | undefined,
  planDate: undefined as string | undefined,
  planTime: '',
  purpose: '',
  address: '',
  remark: ''
})
const form = reactive(emptyForm())

const rules: Record<string, any> = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  salesPersonId: [{ required: true, message: '请选择负责人', trigger: 'change' }],
  planDate: [{ required: true, message: '请选择计划日期', trigger: 'change' }],
  purpose: [{ required: true, message: '请输入拜访目的', trigger: 'blur' }]
}

function resetForm(data?: Partial<VisitPlan>) {
  Object.assign(form, emptyForm(), data || {})
}

function openCreate() {
  editingId.value = null
  resetForm()
  modalOpen.value = true
}

function openEdit(record: VisitPlan) {
  editingId.value = record.id
  resetForm({
    customerId: record.customerId,
    salesPersonId: record.salesPersonId,
    planDate: record.planDate,
    planTime: record.planTime || '',
    purpose: record.purpose || '',
    address: record.address || '',
    remark: record.remark || ''
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
      await visitPlanApi.update(editingId.value, payload)
      message.success('拜访计划更新成功')
    } else {
      await visitPlanApi.create(payload)
      message.success('拜访计划创建成功')
    }
    modalOpen.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[拜访规划] 保存失败', e)
    message.error((e as Error)?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleCancel(record: VisitPlan) {
  try {
    await visitPlanApi.cancel(record.id)
    message.success('计划已取消')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[拜访规划] 取消失败', e)
    message.error((e as Error)?.message || '取消失败')
  }
}

async function handleDelete(record: VisitPlan) {
  try {
    await visitPlanApi.remove(record.id)
    message.success('删除成功')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[拜访规划] 删除失败', e)
    message.error((e as Error)?.message || '删除失败')
  }
}

// ═══ 下拉数据加载 ═══
onMounted(async () => {
  try {
    const list = await crmCustomerApi.dropdown()
    customerOptions.value = (Array.isArray(list) ? list : []).map(c => ({
      label: c.name,
      value: c.id
    }))
  } catch (e) {
    console.warn('[拜访规划] 客户下拉获取失败', e)
  }
  try {
    const res: any = await userApi.getPage({ pageNum: 1, pageSize: 200 })
    const list = res?.records || res?.data?.records || []
    salesPersonOptions.value = list.map((u: any) => ({
      label: u.nickname || u.username,
      value: u.id
    }))
  } catch (e) {
    console.warn('[拜访规划] 负责人下拉获取失败', e)
  }
})
</script>

<style scoped>
.overdue-plan {
  color: #ff4d4f;
}
</style>
