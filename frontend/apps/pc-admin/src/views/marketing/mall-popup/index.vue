<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="弹窗广告"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      page-param-style="pageNum"
      export-file-name="弹窗广告"
      row-key="id"
    >
      <template #header-extra>
        <a-button type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>新增广告
        </a-button>
      </template>
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="statusColor(text)">{{ statusText(text) }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'showType'">
          {{ SHOW_TYPE_MAP[text] || text }}
        </template>
        <template v-else-if="column.dataIndex === 'targetUser'">
          {{ TARGET_USER_MAP[text] || text }}
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatTime(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
            <a-button v-if="record.status === 0" type="link" size="small" @click="handlePublish(record)">投放</a-button>
            <a-button v-if="record.status === 1" type="link" size="small" @click="handleOffline(record)">下架</a-button>
            <a-popconfirm title="确认删除？" ok-text="删除" cancel-text="取消" @confirm="handleDelete(record)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </ARReportPage>

    <a-modal v-model:open="modalOpen" :title="editingId ? '编辑广告' : '新增广告'"
      :confirm-loading="saving" width="650px" @ok="handleSave">
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="标题" name="title">
          <a-input v-model:value="form.title" placeholder="弹窗广告标题" />
        </a-form-item>
        <a-form-item label="图片URL">
          <a-input v-model:value="form.imageUrl" placeholder="https://..." />
        </a-form-item>
        <a-form-item label="跳转链接">
          <a-input v-model:value="form.linkUrl" placeholder="点击跳转链接" />
        </a-form-item>
        <a-form-item label="展示频次" name="showType">
          <a-select v-model:value="form.showType" :options="SHOW_TYPE_OPTIONS" />
        </a-form-item>
        <a-form-item label="目标用户">
          <a-select v-model:value="form.targetUser" :options="TARGET_USER_OPTIONS" />
        </a-form-item>
        <a-form-item label="投放开始">
          <a-date-picker v-model:value="form.startTime" show-time value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </a-form-item>
        <a-form-item label="投放结束">
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
import { popupAdApi, POPUP_STATUS_MAP, SHOW_TYPE_MAP, TARGET_USER_MAP } from '@/api/marketing'

defineOptions({ name: 'MallPopupAd' })

const SHOW_TYPE_OPTIONS = Object.entries(SHOW_TYPE_MAP).map(([v, l]) => ({ label: l, value: v }))
const TARGET_USER_OPTIONS = Object.entries(TARGET_USER_MAP).map(([v, l]) => ({ label: l, value: v }))

function formatTime(val: string | null | undefined): string {
  return val ? String(val).replace('T', ' ').slice(0, 16) : '-'
}
function statusText(s: number) { return POPUP_STATUS_MAP[s]?.text || '未知' }
function statusColor(s: number) { return POPUP_STATUS_MAP[s]?.color || 'default' }

const queryFields: ReportQueryField[] = [
  { key: 'title', type: 'input', label: '标题', placeholder: '标题', width: 200 },
  { key: 'status', type: 'select', label: '状态', placeholder: '全部',
    options: Object.entries(POPUP_STATUS_MAP).map(([v, m]) => ({ label: m.text, value: Number(v) })) },
]

const columns: any[] = [
  { title: '标题', dataIndex: 'title', key: 'title', width: 200, ellipsis: true },
  { title: '展示频次', dataIndex: 'showType', key: 'showType', width: 100 },
  { title: '目标用户', dataIndex: 'targetUser', key: 'targetUser', width: 90 },
  { title: '投放开始', dataIndex: 'startTime', key: 'startTime', width: 150 },
  { title: '投放结束', dataIndex: 'endTime', key: 'endTime', width: 150 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 70, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 150 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 190, fixed: 'right' },
]

function fetcher(params: Record<string, any>) { return popupAdApi.page(params) }

const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  title: '',
  imageUrl: '',
  linkUrl: '',
  showType: 'once',
  targetUser: 'all',
  startTime: undefined as string | undefined,
  endTime: undefined as string | undefined,
  sort: 0,
})
const form = reactive(emptyForm())
const rules: Record<string, any> = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  showType: [{ required: true, message: '请选择展示频次', trigger: 'change' }],
}

function resetForm(data?: any) { Object.assign(form, emptyForm(), data || {}) }
function openCreate() { editingId.value = null; resetForm(); modalOpen.value = true }
function openEdit(record: any) {
  editingId.value = record.id
  resetForm({
    title: record.title,
    imageUrl: record.imageUrl || '',
    linkUrl: record.linkUrl || '',
    showType: record.showType || 'once',
    targetUser: record.targetUser || 'all',
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
      await popupAdApi.update(editingId.value, { ...form })
      message.success('更新成功')
    } else {
      await popupAdApi.create({ ...form })
      message.success('创建成功')
    }
    modalOpen.value = false
    reportRef.value?.reload()
  } catch (e: any) { message.error(e?.data?.message || '保存失败') }
  finally { saving.value = false }
}

async function handlePublish(record: any) {
  try { await popupAdApi.publish(record.id); message.success('已投放'); reportRef.value?.reload() }
  catch (e: any) { message.error(e?.data?.message || '投放失败') }
}

async function handleOffline(record: any) {
  try { await popupAdApi.offline(record.id); message.success('已下架'); reportRef.value?.reload() }
  catch (e: any) { message.error(e?.data?.message || '下架失败') }
}

async function handleDelete(record: any) {
  try { await popupAdApi.remove(record.id); message.success('删除成功'); reportRef.value?.reload() }
  catch (e: any) { message.error(e?.data?.message || '删除失败') }
}
</script>
