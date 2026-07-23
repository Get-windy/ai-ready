<template>
  <div>
    <a-alert
      type="info"
      show-icon
      message="岗位权限页当前对接岗位主数据接口（/api/position）"
      description="后端暂无岗位-权限/角色关联接口（仅有岗位对用户的分配接口），岗位的权限配置能力待后端补充端点后接入。"
      style="margin-bottom: 16px"
    />
    <ARReportPage
      ref="reportRef"
      title="岗位权限"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      page-param-style="pageNum"
      export-file-name="岗位列表"
      row-key="id"
    >
      <template #header-extra>
        <a-button
          type="primary"
          @click="openCreate"
        >
          <template #icon>
            <PlusOutlined />
          </template>新增岗位
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
              title="确认删除该岗位？"
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
      :title="editingId ? '编辑岗位' : '新增岗位'"
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
          label="岗位编码"
          name="positionCode"
        >
          <a-input
            v-model:value="form.positionCode"
            placeholder="请输入岗位编码"
            :disabled="!!editingId"
          />
        </a-form-item>
        <a-form-item
          label="岗位名称"
          name="positionName"
        >
          <a-input
            v-model:value="form.positionName"
            placeholder="请输入岗位名称"
          />
        </a-form-item>
        <a-form-item
          label="所属部门"
          name="deptId"
        >
          <a-select
            v-model:value="form.deptId"
            placeholder="请选择所属部门"
            allow-clear
            :options="deptOptions"
          />
        </a-form-item>
        <a-form-item
          label="岗位级别"
          name="level"
        >
          <a-input-number
            v-model:value="form.level"
            :min="1"
            :max="20"
            style="width: 100%"
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
          label="岗位描述"
          name="description"
        >
          <a-textarea
            v-model:value="form.description"
            :rows="2"
            placeholder="请输入岗位描述"
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
import { mdPositionApi } from '@/api/md'
import { departmentApi, type DepartmentInfo } from '@/api/department'

const queryFields: ReportQueryField[] = [
  { key: 'positionName', type: 'input', label: '岗位名称', placeholder: '岗位名称', width: 180 },
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
  { title: '岗位编码', dataIndex: 'positionCode', key: 'positionCode', width: 120 },
  { title: '岗位名称', dataIndex: 'positionName', key: 'positionName', width: 150, ellipsis: true },
  { title: '所属部门', dataIndex: 'deptName', key: 'deptName', width: 140, ellipsis: true },
  { title: '级别', dataIndex: 'level', key: 'level', width: 70, align: 'right' },
  { title: '在岗人数', dataIndex: 'userCount', key: 'userCount', width: 90, align: 'right' },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 70, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 170, fixed: 'right' }
]

function formatTime(val: string | null | undefined): string {
  return val ? String(val).replace('T', ' ').slice(0, 19) : '-'
}

// ═══ 数据请求：GET /api/position/page ═══
function fetcher(params: Record<string, any>) {
  return mdPositionApi.getPage(params)
}

// ═══ 部门选项（用于岗位所属部门） ═══
const deptOptions = ref<{ label: string; value: number }[]>([])

async function loadDeptOptions() {
  try {
    const res = (await departmentApi.getList()) as any
    const list: DepartmentInfo[] = Array.isArray(res) ? res : res?.data || []
    deptOptions.value = list.map(d => ({ label: d.departmentName, value: d.id }))
  } catch (e) {
    console.warn('[岗位权限] 部门列表获取失败', e)
  }
}

// ═══ 新增/编辑弹窗 ═══
const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  positionCode: '',
  positionName: '',
  deptId: undefined as number | undefined,
  level: 1,
  sort: 0,
  status: 1,
  description: ''
})
const form = reactive(emptyForm())

const rules: Record<string, any> = {
  positionCode: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }],
  positionName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }]
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
    positionCode: record.positionCode,
    positionName: record.positionName,
    deptId: record.deptId,
    level: record.level ?? 1,
    sort: record.sort ?? 0,
    status: record.status ?? 1,
    description: record.description || ''
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
      await mdPositionApi.update(editingId.value, { ...form })
      message.success('岗位更新成功')
    } else {
      await mdPositionApi.create({ ...form })
      message.success('岗位创建成功')
    }
    modalOpen.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[岗位权限] 保存失败', e)
  } finally {
    saving.value = false
  }
}

async function toggleStatus(record: any) {
  const target = record.status === 1 ? 0 : 1
  try {
    await mdPositionApi.updateStatus(record.id, target)
    message.success(target === 1 ? '岗位已启用' : '岗位已禁用')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[岗位权限] 状态切换失败', e)
  }
}

async function handleDelete(record: any) {
  try {
    await mdPositionApi.delete(record.id)
    message.success('删除成功')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[岗位权限] 删除失败', e)
  }
}

onMounted(loadDeptOptions)
</script>
