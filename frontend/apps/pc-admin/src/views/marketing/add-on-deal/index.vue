<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="加价购规则"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      page-param-style="pageNum"
      export-file-name="加价购规则"
      row-key="id"
    >
      <template #header-extra>
        <a-button type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>新增规则
        </a-button>
      </template>
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="text === 1 ? 'green' : 'default'">{{ text === 1 ? '启用' : '停用' }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'addonPrice'">
          ¥{{ text?.toFixed(2) }}
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
            <a-popconfirm title="确认删除？" ok-text="删除" cancel-text="取消" @confirm="handleDelete(record)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </ARReportPage>

    <a-modal v-model:open="modalOpen" :title="editingId ? '编辑规则' : '新增规则'"
      :confirm-loading="saving" width="650px" @ok="handleSave">
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="规则名称" name="ruleName">
          <a-input v-model:value="form.ruleName" placeholder="如 满100加价购" />
        </a-form-item>
        <a-form-item label="主商品ID" name="mainProductId">
          <a-input-number v-model:value="form.mainProductId" :min="1" style="width: 100%" placeholder="购主品商品ID" />
        </a-form-item>
        <a-form-item label="加价商品ID" name="addonProductId">
          <a-input-number v-model:value="form.addonProductId" :min="1" style="width: 100%" placeholder="加价换购商品ID" />
        </a-form-item>
        <a-form-item label="加价金额" name="addonPrice">
          <a-input-number v-model:value="form.addonPrice" :min="0" :precision="2" style="width: 100%" prefix="¥" />
        </a-form-item>
        <a-form-item label="每单限购">
          <a-input-number v-model:value="form.maxPerOrder" :min="1" style="width: 100%" />
        </a-form-item>
        <a-form-item label="生效开始" name="startTime">
          <a-date-picker v-model:value="form.startTime" show-time value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </a-form-item>
        <a-form-item label="生效结束" name="endTime">
          <a-date-picker v-model:value="form.endTime" show-time value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
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
import { addonRuleApi } from '@/api/marketing'

defineOptions({ name: 'AddonDeal' })

function formatTime(val: string | null | undefined): string {
  return val ? String(val).replace('T', ' ').slice(0, 16) : '-'
}

const queryFields: ReportQueryField[] = [
  { key: 'ruleName', type: 'input', label: '规则名称', placeholder: '规则名称', width: 200 },
  { key: 'status', type: 'select', label: '状态', placeholder: '全部',
    options: [{ label: '启用', value: 1 }, { label: '停用', value: 0 }] },
]

const columns: any[] = [
  { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName', width: 180, ellipsis: true },
  { title: '主商品', dataIndex: 'mainProductName', key: 'mainProductName', width: 130, ellipsis: true },
  { title: '加价商品', dataIndex: 'addonProductName', key: 'addonProductName', width: 130, ellipsis: true },
  { title: '加价金额', dataIndex: 'addonPrice', key: 'addonPrice', width: 100, align: 'right' },
  { title: '限购', dataIndex: 'maxPerOrder', key: 'maxPerOrder', width: 70, align: 'right' },
  { title: '有效期起', dataIndex: 'startTime', key: 'startTime', width: 150 },
  { title: '有效期止', dataIndex: 'endTime', key: 'endTime', width: 150 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 150 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 180, fixed: 'right' },
]

function fetcher(params: Record<string, any>) { return addonRuleApi.page(params) }

const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  ruleName: '',
  mainProductId: undefined as number | undefined,
  addonProductId: undefined as number | undefined,
  addonPrice: 0,
  maxPerOrder: 1,
  startTime: undefined as string | undefined,
  endTime: undefined as string | undefined,
  sort: 0,
})
const form = reactive(emptyForm())
const rules: Record<string, any> = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  mainProductId: [{ required: true, message: '请输入主商品ID', trigger: 'blur' }],
  addonProductId: [{ required: true, message: '请输入加价商品ID', trigger: 'blur' }],
  addonPrice: [{ required: true, message: '请输入加价金额', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择生效开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择生效结束时间', trigger: 'change' }],
}

function resetForm(data?: any) { Object.assign(form, emptyForm(), data || {}) }
function openCreate() { editingId.value = null; resetForm(); modalOpen.value = true }
function openEdit(record: any) {
  editingId.value = record.id
  resetForm({
    ruleName: record.ruleName,
    mainProductId: record.mainProductId,
    addonProductId: record.addonProductId,
    addonPrice: record.addonPrice,
    maxPerOrder: record.maxPerOrder ?? 1,
    startTime: record.startTime,
    endTime: record.endTime,
    sort: record.sort ?? 0,
  })
  modalOpen.value = true
}

async function handleSave() {
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    if (editingId.value) {
      await addonRuleApi.update(editingId.value, { ...form })
      message.success('更新成功')
    } else {
      await addonRuleApi.create({ ...form })
      message.success('创建成功')
    }
    modalOpen.value = false
    reportRef.value?.reload()
  } catch (e: any) { message.error(e?.data?.message || '保存失败') }
  finally { saving.value = false }
}

async function toggleStatus(record: any) {
  const target = record.status === 1 ? 0 : 1
  try {
    if (target === 1) { await addonRuleApi.enable(record.id) }
    else { await addonRuleApi.disable(record.id) }
    message.success(target === 1 ? '已启用' : '已停用')
    reportRef.value?.reload()
  } catch (e: any) { message.error(e?.data?.message || '操作失败') }
}

async function handleDelete(record: any) {
  try { await addonRuleApi.remove(record.id); message.success('删除成功'); reportRef.value?.reload() }
  catch (e: any) { message.error(e?.data?.message || '删除失败') }
}
</script>
