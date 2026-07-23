<template>
  <ARReportPage
    ref="reportRef"
    title="商城公告"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="商城公告"
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
        </template>新增公告
      </a-button>
    </template>
    <template #bodyCell="{ column, record, text }">
      <template v-if="column.dataIndex === 'noticeType'">
        <a-tag :color="TYPE_MAP[text]?.color">
          {{ TYPE_MAP[text]?.label || text || '-' }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'status'">
        <a-tag :color="STATUS_MAP[text]?.color">
          {{ STATUS_MAP[text]?.label || text || '-' }}
        </a-tag>
      </template>
      <template v-else-if="['publishTime', 'createTime'].includes(column.dataIndex as string)">
        {{ fmtTime(text) }}
      </template>
      <template v-else-if="column.key === 'action'">
        <a-space :size="4">
          <a-button
            type="link"
            size="small"
            @click="openEdit(record)"
          >
            编辑
          </a-button>
          <a-popconfirm
            v-if="record.status !== 1"
            title="确认发布该公告？发布后商城端立即可见"
            ok-text="发布"
            cancel-text="取消"
            @confirm="handlePublish(record)"
          >
            <a-button
              type="link"
              size="small"
            >
              发布
            </a-button>
          </a-popconfirm>
          <a-popconfirm
            v-if="record.status === 1"
            title="确认下线该公告？下线后商城端不再展示"
            ok-text="下线"
            cancel-text="取消"
            @confirm="handleOffline(record)"
          >
            <a-button
              type="link"
              size="small"
            >
              下线
            </a-button>
          </a-popconfirm>
          <a-popconfirm
            title="确认删除该公告？"
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

  <!-- 新增/编辑公告弹窗 -->
  <a-modal
    v-model:open="modalOpen"
    :title="editingId ? '编辑公告' : '新增公告'"
    :confirm-loading="saving"
    :width="600"
    @ok="handleSave"
  >
    <a-form
      ref="formRef"
      :model="form"
      :rules="rules"
      :label-col="{ span: 5 }"
      :wrapper-col="{ span: 17 }"
      style="margin-top: 16px"
    >
      <a-form-item
        label="公告标题"
        name="title"
      >
        <a-input
          v-model:value="form.title"
          :maxlength="100"
          placeholder="展示在商城首页的公告标题"
        />
      </a-form-item>
      <a-form-item
        label="公告类型"
        name="noticeType"
      >
        <a-select
          v-model:value="form.noticeType"
          :options="typeOptions"
          placeholder="请选择公告类型"
        />
      </a-form-item>
      <a-form-item
        label="公告内容"
        name="content"
      >
        <a-textarea
          v-model:value="form.content"
          :rows="5"
          :maxlength="500"
          show-count
          placeholder="公告正文内容"
        />
      </a-form-item>
      <a-form-item
        label="排序"
        name="sort"
      >
        <a-input-number
          v-model:value="form.sort"
          :min="0"
          :precision="0"
          style="width: 100%"
          placeholder="数值越小越靠前"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { mallNoticeApi } from '@/api/erp/mall'

defineOptions({ name: 'MallNoticeConfig' })

// ═══ 公告类型（与后端 MallNotice.noticeType 一致） ═══
const TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '公告', color: 'blue' },
  2: { label: '活动', color: 'orange' },
  3: { label: '系统', color: 'purple' }
}

// ═══ 公告状态（与后端 MallNotice.status 一致） ═══
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '已发布', color: 'green' },
  2: { label: '已下线', color: 'orange' }
}

const typeOptions = Object.entries(TYPE_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))
const statusOptions = Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))

const reportRef = ref<InstanceType<typeof ARReportPage>>()

const queryFields: ReportQueryField[] = [
  { key: 'title', type: 'input', label: '标题', placeholder: '公告标题', width: 180 },
  { key: 'noticeType', type: 'select', label: '类型', placeholder: '全部类型', options: typeOptions },
  { key: 'status', type: 'select', label: '状态', placeholder: '全部状态', options: statusOptions }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '公告标题', dataIndex: 'title', key: 'title', width: 220, ellipsis: true },
  { title: '类型', dataIndex: 'noticeType', key: 'noticeType', width: 90 },
  { title: '内容', dataIndex: 'content', key: 'content', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 70, align: 'right' },
  { title: '发布时间', dataIndex: 'publishTime', key: 'publishTime', width: 150 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 150 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]

function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

// ═══ 数据请求 ═══
function fetcher(params: Record<string, any>) {
  return mallNoticeApi.page(params)
}

// ═══ 新增/编辑弹窗 ═══
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  title: '' as string,
  noticeType: 1 as number,
  content: '' as string,
  sort: 0 as number
})
const form = reactive(emptyForm())

const rules: Record<string, any> = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  noticeType: [{ required: true, message: '请选择公告类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
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
    title: record.title || '',
    noticeType: record.noticeType ?? 1,
    content: record.content || '',
    sort: record.sort ?? 0
  })
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
    if (editingId.value) {
      await mallNoticeApi.update(editingId.value, { ...form })
      message.success('公告更新成功')
    } else {
      await mallNoticeApi.create({ ...form })
      message.success('公告创建成功（草稿状态，发布后商城可见）')
    }
    modalOpen.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[商城公告] 保存失败', e)
  } finally {
    saving.value = false
  }
}

// ═══ 发布/下线/删除 ═══
async function handlePublish(record: any) {
  try {
    await mallNoticeApi.publish(record.id)
    message.success('公告已发布')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[商城公告] 发布失败', e)
  }
}

async function handleOffline(record: any) {
  try {
    await mallNoticeApi.offline(record.id)
    message.success('公告已下线')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[商城公告] 下线失败', e)
  }
}

async function handleDelete(record: any) {
  try {
    await mallNoticeApi.delete(record.id)
    message.success('删除成功')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[商城公告] 删除失败', e)
  }
}
</script>
