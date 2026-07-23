<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="支付方式"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      page-param-style="pageNum"
      export-file-name="支付方式"
      row-key="id"
    >
      <template #header-extra>
        <a-button type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>新增支付方式
        </a-button>
      </template>
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.dataIndex === 'methodType'">
          {{ methodTypeText(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tag :color="text === 1 ? 'green' : 'default'">{{ text === 1 ? '启用' : '停用' }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'isDefault'">
          <a-tag v-if="text === 1" color="blue">默认</a-tag>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.dataIndex === 'feeRate'">
          {{ text ? (text * 100).toFixed(2) + '%' : '-' }}
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
            <a-popconfirm title="确认删除该支付方式？" ok-text="删除" cancel-text="取消" @confirm="handleDelete(record)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </ARReportPage>

    <a-modal v-model:open="modalOpen" :title="editingId ? '编辑支付方式' : '新增支付方式'"
      :confirm-loading="saving" width="600px" @ok="handleSave">
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="编码" name="methodCode">
          <a-input v-model:value="form.methodCode" placeholder="如 CASH/BANK/WECHAT" :disabled="!!editingId" />
        </a-form-item>
        <a-form-item label="名称" name="methodName">
          <a-input v-model:value="form.methodName" placeholder="如 现金/银行转账/微信支付" />
        </a-form-item>
        <a-form-item label="类型" name="methodType">
          <a-select v-model:value="form.methodType" placeholder="请选择" :options="METHOD_TYPE_OPTIONS" />
        </a-form-item>
        <a-form-item label="默认入账账户">
          <a-select v-model:value="form.accountId" placeholder="不选则不关联" allow-clear :options="accountOptions" />
        </a-form-item>
        <a-form-item label="手续费率(%)">
          <a-input-number v-model:value="form.feeRatePercent" :min="0" :max="100" :step="0.1"
            style="width: 100%" placeholder="如 0.6 表示 0.6%" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="form.sort" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="默认">
          <a-switch :checked="form.isDefault === 1" checked-children="是" un-checked-children="否"
            @change="(v: any) => (form.isDefault = v ? 1 : 0)" />
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
import { paymentMethodApi, METHOD_TYPE_OPTIONS, METHOD_TYPE_MAP } from '@/api/payment/md'

defineOptions({ name: 'MdPaymentMethod' })

function methodTypeText(val: string) { return METHOD_TYPE_MAP[val] || val || '-' }
function formatTime(val: string | null | undefined): string {
  return val ? String(val).replace('T', ' ').slice(0, 19) : '-'
}

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '编码/名称', placeholder: '编码或名称', width: 200 },
  { key: 'status', type: 'select', label: '状态', placeholder: '全部状态',
    options: [{ label: '启用', value: 1 }, { label: '停用', value: 0 }] },
]

const columns: any[] = [
  { title: '编码', dataIndex: 'methodCode', key: 'methodCode', width: 110 },
  { title: '名称', dataIndex: 'methodName', key: 'methodName', width: 160, ellipsis: true },
  { title: '类型', dataIndex: 'methodType', key: 'methodType', width: 100 },
  { title: '手续费率', dataIndex: 'feeRate', key: 'feeRate', width: 100, align: 'right' },
  { title: '默认', dataIndex: 'isDefault', key: 'isDefault', width: 70 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 70, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 180, fixed: 'right' },
]

function fetcher(params: Record<string, any>) {
  return paymentMethodApi.page(params)
}

// ═══ 账户选项（下拉选择） ═══
const accountOptions = ref<{ label: string; value: number }[]>([])
async function loadAccounts() {
  try {
    const res: any = await paymentMethodApi.list()
    const list: any[] = Array.isArray(res) ? res : res?.data || []
    accountOptions.value = list.map(a => ({ label: a.methodName, value: a.id }))
  } catch { /* ignore */ }
}

// ═══ 弹窗 ═══
const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  methodCode: '',
  methodName: '',
  methodType: undefined as string | undefined,
  accountId: undefined as number | undefined,
  feeRatePercent: 0,
  sort: 0,
  isDefault: 0,
  status: 1,
})
const form = reactive(emptyForm())

const rules: Record<string, any> = {
  methodCode: [{ required: true, message: '请输入支付方式编码', trigger: 'blur' }],
  methodName: [{ required: true, message: '请输入支付方式名称', trigger: 'blur' }],
  methodType: [{ required: true, message: '请选择支付方式类型', trigger: 'change' }],
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
    methodCode: record.methodCode,
    methodName: record.methodName,
    methodType: record.methodType,
    accountId: record.accountId,
    feeRatePercent: record.feeRate ? Number((record.feeRate * 100).toFixed(2)) : 0,
    sort: record.sort ?? 0,
    isDefault: record.isDefault ?? 0,
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
      methodCode: form.methodCode,
      methodName: form.methodName,
      methodType: form.methodType,
      accountId: form.accountId || undefined,
      feeRate: form.feeRatePercent ? Number((form.feeRatePercent / 100).toFixed(4)) : 0,
      sort: form.sort,
      isDefault: form.isDefault,
      status: form.status,
    }
    if (editingId.value) {
      await paymentMethodApi.update(editingId.value, data)
      message.success('更新成功')
    } else {
      await paymentMethodApi.create(data)
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
    await paymentMethodApi.updateStatus(record.id, target)
    message.success(target === 1 ? '已启用' : '已停用')
    reportRef.value?.reload()
  } catch (e: any) {
    message.error(e?.data?.message || '状态切换失败')
  }
}

async function handleDelete(record: any) {
  try {
    await paymentMethodApi.remove(record.id)
    message.success('删除成功')
    reportRef.value?.reload()
  } catch (e: any) {
    message.error(e?.data?.message || '删除失败')
  }
}

onMounted(loadAccounts)
</script>
