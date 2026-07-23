<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="支付渠道"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      page-param-style="pageNum"
      export-file-name="支付渠道"
      row-key="id"
    >
      <template #header-extra>
        <a-button type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>新增支付渠道
        </a-button>
      </template>
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="text === 1 ? 'green' : 'default'">{{ text === 1 ? '启用' : '停用' }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'methodId'">
          {{ methodNameMap.get(text) || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatTime(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
            <a-button type="link" size="small" @click="toggleStatus(record)">
              {{ record.status === 1 ? '停用' : '启用' }}
            </a-button>
            <a-popconfirm title="确认删除该支付渠道？" ok-text="删除" cancel-text="取消" @confirm="handleDelete(record)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </ARReportPage>

    <a-modal v-model:open="modalOpen" :title="editingId ? '编辑支付渠道' : '新增支付渠道'"
      :confirm-loading="saving" width="600px" @ok="handleSave">
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="编码" name="channelCode">
          <a-input v-model:value="form.channelCode" placeholder="如 WECHAT_MP/ALIPAY_WEB" :disabled="!!editingId" />
        </a-form-item>
        <a-form-item label="名称" name="channelName">
          <a-input v-model:value="form.channelName" placeholder="如 微信-公众号支付" />
        </a-form-item>
        <a-form-item label="支付方式" name="methodId">
          <a-select v-model:value="form.methodId" placeholder="请选择支付方式" :options="methodOptions" />
        </a-form-item>
        <a-form-item label="商户号">
          <a-input v-model:value="form.merchantNo" placeholder="商户号/合作者ID等" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="form.sort" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="状态">
          <a-switch :checked="form.status === 1" checked-children="启用" un-checked-children="停用"
            @change="(v: any) => (form.status = v ? 1 : 0)" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { paymentChannelApi, paymentMethodApi } from '@/api/payment/md'

defineOptions({ name: 'MdPaymentChannel' })

function formatTime(val: string | null | undefined): string {
  return val ? String(val).replace('T', ' ').slice(0, 19) : '-'
}

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '编码/名称', placeholder: '编码或名称', width: 200 },
  { key: 'status', type: 'select', label: '状态', placeholder: '全部状态',
    options: [{ label: '启用', value: 1 }, { label: '停用', value: 0 }] },
]

const columns: any[] = [
  { title: '编码', dataIndex: 'channelCode', key: 'channelCode', width: 130 },
  { title: '名称', dataIndex: 'channelName', key: 'channelName', width: 180, ellipsis: true },
  { title: '支付方式', dataIndex: 'methodId', key: 'methodId', width: 100 },
  { title: '商户号', dataIndex: 'merchantNo', key: 'merchantNo', width: 160, ellipsis: true },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 70, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 180, fixed: 'right' },
]

function fetcher(params: Record<string, any>) {
  return paymentChannelApi.page(params)
}

// ═══ 支付方式选项（下拉选择） ═══
const methodOptions = ref<{ label: string; value: number }[]>([])
const methodNameMap = ref<Map<number, string>>(new Map())
async function loadMethods() {
  try {
    const res: any = await paymentMethodApi.list()
    const list: any[] = Array.isArray(res) ? res : res?.data || []
    methodOptions.value = list.map(m => ({ label: `[${m.methodCode}] ${m.methodName}`, value: m.id }))
    methodNameMap.value = new Map(list.map(m => [m.id, `[${m.methodCode}] ${m.methodName}`]))
  } catch { /* ignore */ }
}

// ═══ 弹窗 ═══
const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  channelCode: '',
  channelName: '',
  methodId: undefined as number | undefined,
  merchantNo: '',
  sort: 0,
  status: 1,
})
const form = reactive(emptyForm())

const rules: Record<string, any> = {
  channelCode: [{ required: true, message: '请输入渠道编码', trigger: 'blur' }],
  channelName: [{ required: true, message: '请输入渠道名称', trigger: 'blur' }],
  methodId: [{ required: true, message: '请选择支付方式', trigger: 'change' }],
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
  resetForm({
    channelCode: record.channelCode,
    channelName: record.channelName,
    methodId: record.methodId,
    merchantNo: record.merchantNo || '',
    sort: record.sort ?? 0,
    status: record.status ?? 1,
  })
  modalOpen.value = true
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch { return }
  saving.value = true
  try {
    const data = {
      channelCode: form.channelCode,
      channelName: form.channelName,
      methodId: form.methodId,
      merchantNo: form.merchantNo || undefined,
      sort: form.sort,
      status: form.status,
    }
    if (editingId.value) {
      await paymentChannelApi.update(editingId.value, data)
      message.success('更新成功')
    } else {
      await paymentChannelApi.create(data)
      message.success('创建成功')
    }
    modalOpen.value = false
    reportRef.value?.reload()
  } catch (e: any) {
    message.error(e?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(record: any) {
  const target = record.status === 1 ? 0 : 1
  try {
    await paymentChannelApi.updateStatus(record.id, target)
    message.success(target === 1 ? '已启用' : '已停用')
    reportRef.value?.reload()
  } catch (e: any) {
    message.error(e?.data?.message || '状态切换失败')
  }
}

async function handleDelete(record: any) {
  try {
    await paymentChannelApi.remove(record.id)
    message.success('删除成功')
    reportRef.value?.reload()
  } catch (e: any) {
    message.error(e?.data?.message || '删除失败')
  }
}

onMounted(loadMethods)
</script>
