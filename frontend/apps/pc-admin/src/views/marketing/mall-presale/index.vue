<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="预售活动"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      page-param-style="pageNum"
      export-file-name="预售活动"
      row-key="id"
    >
      <template #header-extra>
        <a-button type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>新增预售
        </a-button>
      </template>
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="statusColor(text)">{{ statusText(text) }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'depositAmount' || column.dataIndex === 'finalAmount'">
          ¥{{ text?.toFixed(2) }}
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatTime(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
            <a-button v-if="record.status === 0" type="link" size="small" @click="handlePublish(record)">发布</a-button>
            <a-button v-if="record.status === 0 || record.status === 1" type="link" size="small" @click="handleCancel(record)">取消</a-button>
            <a-popconfirm title="确认删除？" ok-text="删除" cancel-text="取消" @confirm="handleDelete(record)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </ARReportPage>

    <a-modal v-model:open="modalOpen" :title="editingId ? '编辑预售' : '新增预售'"
      :confirm-loading="saving" width="700px" @ok="handleSave">
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="活动名称" name="activityName">
          <a-input v-model:value="form.activityName" placeholder="如 618预售抢先购" />
        </a-form-item>
        <a-form-item label="商品ID" name="productId">
          <a-input-number v-model:value="form.productId" :min="1" style="width: 100%" placeholder="商品ID" />
        </a-form-item>
        <a-form-item label="定金金额" name="depositAmount">
          <a-input-number v-model:value="form.depositAmount" :min="0" :precision="2" style="width: 100%" prefix="¥" />
        </a-form-item>
        <a-form-item label="尾款金额" name="finalAmount">
          <a-input-number v-model:value="form.finalAmount" :min="0" :precision="2" style="width: 100%" prefix="¥" />
        </a-form-item>
        <a-form-item label="活动开始" name="startTime">
          <a-date-picker v-model:value="form.startTime" show-time value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </a-form-item>
        <a-form-item label="活动结束" name="endTime">
          <a-date-picker v-model:value="form.endTime" show-time value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </a-form-item>
        <a-form-item label="定金截止">
          <a-date-picker v-model:value="form.depositEndTime" show-time value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </a-form-item>
        <a-form-item label="尾款开始">
          <a-date-picker v-model:value="form.finalStartTime" show-time value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </a-form-item>
        <a-form-item label="库存限制">
          <a-input-number v-model:value="form.stockLimit" :min="0" style="width: 100%" placeholder="0=不限" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="form.sort" :min="0" style="width: 100%" />
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
import { presaleApi, PRESALE_STATUS_MAP } from '@/api/marketing'

defineOptions({ name: 'MallPresale' })

function formatTime(val: string | null | undefined): string {
  return val ? String(val).replace('T', ' ').slice(0, 16) : '-'
}
function statusText(s: number) { return PRESALE_STATUS_MAP[s]?.text || '未知' }
function statusColor(s: number) { return PRESALE_STATUS_MAP[s]?.color || 'default' }

const queryFields: ReportQueryField[] = [
  { key: 'activityName', type: 'input', label: '活动名称', placeholder: '活动名称', width: 200 },
  { key: 'status', type: 'select', label: '状态', placeholder: '全部',
    options: Object.entries(PRESALE_STATUS_MAP).map(([v, m]) => ({ label: m.text, value: Number(v) })) },
]

const columns: any[] = [
  { title: '活动名称', dataIndex: 'activityName', key: 'activityName', width: 180, ellipsis: true },
  { title: '商品', dataIndex: 'productName', key: 'productName', width: 140, ellipsis: true },
  { title: '定金', dataIndex: 'depositAmount', key: 'depositAmount', width: 100, align: 'right' },
  { title: '尾款', dataIndex: 'finalAmount', key: 'finalAmount', width: 100, align: 'right' },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 150 },
  { title: '结束时间', dataIndex: 'endTime', key: 'endTime', width: 150 },
  { title: '已售', dataIndex: 'soldCount', key: 'soldCount', width: 70, align: 'right' },
  { title: '库存', dataIndex: 'stockLimit', key: 'stockLimit', width: 70, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 150 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 200, fixed: 'right' },
]

function fetcher(params: Record<string, any>) {
  return presaleApi.page(params)
}

const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  activityName: '',
  productId: undefined as number | undefined,
  depositAmount: 0,
  finalAmount: 0,
  startTime: undefined as string | undefined,
  endTime: undefined as string | undefined,
  depositEndTime: undefined as string | undefined,
  finalStartTime: undefined as string | undefined,
  stockLimit: 0,
  sort: 0,
})
const form = reactive(emptyForm())
const rules: Record<string, any> = {
  activityName: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  productId: [{ required: true, message: '请输入商品ID', trigger: 'blur' }],
  depositAmount: [{ required: true, message: '请输入定金金额', trigger: 'blur' }],
  finalAmount: [{ required: true, message: '请输入尾款金额', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
}

function resetForm(data?: any) { Object.assign(form, emptyForm(), data || {}) }
function openCreate() { editingId.value = null; resetForm(); modalOpen.value = true }
function openEdit(record: any) {
  editingId.value = record.id
  resetForm({
    activityName: record.activityName,
    productId: record.productId,
    depositAmount: record.depositAmount,
    finalAmount: record.finalAmount,
    startTime: record.startTime,
    endTime: record.endTime,
    depositEndTime: record.depositEndTime,
    finalStartTime: record.finalStartTime,
    stockLimit: record.stockLimit ?? 0,
    sort: record.sort ?? 0,
  })
  modalOpen.value = true
}

async function handleSave() {
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    if (editingId.value) {
      await presaleApi.update(editingId.value, { ...form })
      message.success('更新成功')
    } else {
      await presaleApi.create({ ...form })
      message.success('创建成功')
    }
    modalOpen.value = false
    reportRef.value?.reload()
  } catch (e: any) { message.error(e?.data?.message || '保存失败') }
  finally { saving.value = false }
}

async function handlePublish(record: any) {
  try { await presaleApi.publish(record.id); message.success('已发布'); reportRef.value?.reload() }
  catch (e: any) { message.error(e?.data?.message || '发布失败') }
}

async function handleCancel(record: any) {
  try { await presaleApi.cancel(record.id); message.success('已取消'); reportRef.value?.reload() }
  catch (e: any) { message.error(e?.data?.message || '取消失败') }
}

async function handleDelete(record: any) {
  try { await presaleApi.remove(record.id); message.success('删除成功'); reportRef.value?.reload() }
  catch (e: any) { message.error(e?.data?.message || '删除失败') }
}
</script>
