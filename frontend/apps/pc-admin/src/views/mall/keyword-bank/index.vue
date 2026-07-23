<template>
  <ARReportPage
    ref="reportRef"
    title="关键词库"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="关键词库"
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
        </template>新增关键词
      </a-button>
    </template>
    <template #bodyCell="{ column, record, text }">
      <template v-if="column.dataIndex === 'keywordType'">
        <a-tag :color="TYPE_MAP[text]?.color">
          {{ TYPE_MAP[text]?.label || text || '-' }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'status'">
        <a-switch
          :checked="record.status === 1"
          :loading="togglingId === record.id"
          checked-children="启用"
          un-checked-children="禁用"
          @change="(checked: any) => handleToggle(record, checked)"
        />
      </template>
      <template v-else-if="column.dataIndex === 'createTime'">
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
            title="确认删除该关键词？"
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

  <!-- 新增/编辑关键词弹窗 -->
  <a-modal
    v-model:open="modalOpen"
    :title="editingId ? '编辑关键词' : '新增关键词'"
    :confirm-loading="saving"
    :width="480"
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
        label="关键词"
        name="keyword"
      >
        <a-input
          v-model:value="form.keyword"
          :maxlength="50"
          placeholder="如：矿泉水、办公用品"
        />
      </a-form-item>
      <a-form-item
        label="类型"
        name="keywordType"
      >
        <a-select
          v-model:value="form.keywordType"
          :options="typeOptions"
          placeholder="请选择关键词类型"
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
import { mallKeywordApi } from '@/api/erp/mall'

defineOptions({ name: 'MallKeywordBank' })

// ═══ 关键词类型（与后端 MallKeyword.keywordType 一致） ═══
const TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '热门', color: 'orange' },
  2: { label: '置顶', color: 'blue' },
  3: { label: '屏蔽', color: 'default' }
}

const typeOptions = Object.entries(TYPE_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

const reportRef = ref<InstanceType<typeof ARReportPage>>()

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键词', placeholder: '关键词（模糊）', width: 180 },
  { key: 'keywordType', type: 'select', label: '类型', placeholder: '全部类型', options: typeOptions },
  { key: 'status', type: 'select', label: '状态', placeholder: '全部状态', options: statusOptions }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '关键词', dataIndex: 'keyword', key: 'keyword', width: 220, ellipsis: true },
  { title: '类型', dataIndex: 'keywordType', key: 'keywordType', width: 100 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 110 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
]

function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

// ═══ 数据请求 ═══
function fetcher(params: Record<string, any>) {
  return mallKeywordApi.page(params)
}

// ═══ 新增/编辑弹窗 ═══
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  keyword: '' as string,
  keywordType: 1 as number,
  sort: 0 as number
})
const form = reactive(emptyForm())

const rules: Record<string, any> = {
  keyword: [{ required: true, message: '请输入关键词', trigger: 'blur' }],
  keywordType: [{ required: true, message: '请选择关键词类型', trigger: 'change' }]
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
    keyword: record.keyword || '',
    keywordType: record.keywordType ?? 1,
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
      await mallKeywordApi.update(editingId.value, { ...form })
      message.success('关键词更新成功')
    } else {
      await mallKeywordApi.create({ ...form })
      message.success('关键词创建成功')
    }
    modalOpen.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[关键词库] 保存失败', e)
  } finally {
    saving.value = false
  }
}

// ═══ 启停切换/删除 ═══
const togglingId = ref<number | null>(null)

async function handleToggle(record: any, checked: boolean | string | number) {
  const target = checked ? 1 : 0
  togglingId.value = record.id
  try {
    await mallKeywordApi.toggleStatus(record.id, target)
    message.success(target === 1 ? '已启用' : '已禁用')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[关键词库] 状态切换失败', e)
  } finally {
    togglingId.value = null
  }
}

async function handleDelete(record: any) {
  try {
    await mallKeywordApi.delete(record.id)
    message.success('删除成功')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[关键词库] 删除失败', e)
  }
}
</script>
