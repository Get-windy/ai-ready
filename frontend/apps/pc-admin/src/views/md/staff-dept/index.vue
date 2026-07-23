<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="职员部门"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      page-param-style="pageNum"
      export-file-name="职员部门"
      row-key="id"
    >
      <template #header-extra>
        <a-button
          type="primary"
          @click="openCreate"
        >
          <template #icon>
            <PlusOutlined />
          </template>新增部门
        </a-button>
      </template>
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="text === 1 ? 'green' : 'default'">
            {{ text === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
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
            <a-button
              type="link"
              size="small"
              @click="toggleStatus(record)"
            >
              {{ record.status === 1 ? '禁用' : '启用' }}
            </a-button>
            <a-popconfirm
              title="确认删除该部门？"
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
      :title="editingId ? '编辑部门' : '新增部门'"
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
          label="部门编码"
          name="departmentCode"
        >
          <a-input
            v-model:value="form.departmentCode"
            placeholder="请输入部门编码"
            :disabled="!!editingId"
          />
        </a-form-item>
        <a-form-item
          label="部门名称"
          name="departmentName"
        >
          <a-input
            v-model:value="form.departmentName"
            placeholder="请输入部门名称"
          />
        </a-form-item>
        <a-form-item
          label="上级部门"
          name="parentId"
        >
          <a-select
            v-model:value="form.parentId"
            placeholder="不选则为顶级部门"
            allow-clear
            :options="parentOptions"
          />
        </a-form-item>
        <a-form-item
          label="联系电话"
          name="phone"
        >
          <a-input
            v-model:value="form.phone"
            placeholder="请输入联系电话"
          />
        </a-form-item>
        <a-form-item
          label="邮箱"
          name="email"
        >
          <a-input
            v-model:value="form.email"
            placeholder="请输入邮箱"
          />
        </a-form-item>
        <a-form-item
          label="排序"
          name="sort"
        >
          <a-input-number
            v-model:value="form.sort"
            :min="0"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item
          label="状态"
          name="status"
        >
          <a-switch
            :checked="form.status === 1"
            checked-children="启用"
            un-checked-children="禁用"
            @change="(v: any) => (form.status = v ? 1 : 0)"
          />
        </a-form-item>
        <a-form-item
          label="描述"
          name="description"
        >
          <a-textarea
            v-model:value="form.description"
            :rows="2"
            placeholder="请输入部门描述"
          />
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
import { departmentApi, type DepartmentInfo } from '@/api/department'

const queryFields: ReportQueryField[] = [
  { key: 'departmentName', type: 'input', label: '部门名称', placeholder: '部门名称', width: 180 },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: [
      { label: '启用', value: 1 },
      { label: '禁用', value: 0 }
    ]
  }
]

const columns: any[] = [
  { title: '部门编码', dataIndex: 'departmentCode', key: 'departmentCode', width: 120 },
  { title: '部门名称', dataIndex: 'departmentName', key: 'departmentName', width: 160, ellipsis: true },
  { title: '上级部门', dataIndex: 'parentName', key: 'parentName', width: 140, ellipsis: true },
  { title: '负责人', dataIndex: 'leaderName', key: 'leaderName', width: 100 },
  { title: '联系电话', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 70, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 170, fixed: 'right' }
]

function formatTime(val: string | null | undefined): string {
  return val ? String(val).replace('T', ' ').slice(0, 19) : '-'
}

// ═══ 数据请求：GET /api/department/page ═══
function fetcher(params: Record<string, any>) {
  return departmentApi.getPage(params)
}

// ═══ 上级部门选项 ═══
const parentOptions = ref<{ label: string; value: number }[]>([])

async function loadParentOptions() {
  try {
    const res = (await departmentApi.getList()) as any
    const list: DepartmentInfo[] = Array.isArray(res) ? res : res?.data || []
    parentOptions.value = list
      .filter(d => d.id !== editingId.value)
      .map(d => ({ label: d.departmentName, value: d.id }))
  } catch (e) {
    console.warn('[职员部门] 部门列表获取失败', e)
  }
}

// ═══ 新增/编辑弹窗 ═══
const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  departmentCode: '',
  departmentName: '',
  parentId: undefined as number | undefined,
  phone: '',
  email: '',
  sort: 0,
  status: 1,
  description: ''
})
const form = reactive(emptyForm())

const rules: Record<string, any> = {
  departmentCode: [{ required: true, message: '请输入部门编码', trigger: 'blur' }],
  departmentName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

function resetForm(data?: Partial<typeof form>) {
  Object.assign(form, emptyForm(), data || {})
}

function openCreate() {
  editingId.value = null
  resetForm()
  loadParentOptions()
  modalOpen.value = true
}

function openEdit(record: any) {
  editingId.value = record.id
  resetForm({
    departmentCode: record.departmentCode,
    departmentName: record.departmentName,
    parentId: record.parentId,
    phone: record.phone || '',
    email: record.email || '',
    sort: record.sort ?? 0,
    status: record.status ?? 1,
    description: record.description || ''
  })
  loadParentOptions()
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
      await departmentApi.update(editingId.value, { ...form })
      message.success('部门更新成功')
    } else {
      await departmentApi.create({ ...form })
      message.success('部门创建成功')
    }
    modalOpen.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[职员部门] 保存失败', e)
  } finally {
    saving.value = false
  }
}

async function toggleStatus(record: any) {
  const target = record.status === 1 ? 0 : 1
  try {
    await departmentApi.updateStatus(record.id, target)
    message.success(target === 1 ? '部门已启用' : '部门已禁用')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[职员部门] 状态切换失败', e)
  }
}

async function handleDelete(record: any) {
  try {
    await departmentApi.delete(record.id)
    message.success('删除成功')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[职员部门] 删除失败', e)
  }
}

onMounted(loadParentOptions)
</script>
