<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="全部操作员"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      page-param-style="pageNum"
      export-file-name="操作员列表"
      row-key="id"
    >
      <template #header-extra>
        <a-button
          type="primary"
          @click="openCreate"
        >
          <template #icon>
            <PlusOutlined />
          </template>新增操作员
        </a-button>
      </template>
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="text === 1 ? 'green' : 'default'">
            {{ text === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'gender'">
          {{ GENDER_MAP[text] || '-' }}
        </template>
        <template v-else-if="['lastLoginTime', 'createTime'].includes(column.dataIndex as string)">
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
              title="确认删除该操作员？"
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
      :title="editingId ? '编辑操作员' : '新增操作员'"
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
          label="登录账号"
          name="username"
        >
          <a-input
            v-model:value="form.username"
            placeholder="3-50位登录账号"
            :disabled="!!editingId"
          />
        </a-form-item>
        <a-form-item
          v-if="!editingId"
          label="初始密码"
          name="password"
        >
          <a-input-password
            v-model:value="form.password"
            placeholder="6-100位初始密码"
          />
        </a-form-item>
        <a-form-item
          label="姓名"
          name="nickname"
        >
          <a-input
            v-model:value="form.nickname"
            placeholder="请输入姓名"
          />
        </a-form-item>
        <a-form-item
          label="手机号"
          name="phone"
        >
          <a-input
            v-model:value="form.phone"
            placeholder="请输入手机号"
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
          label="性别"
          name="gender"
        >
          <a-select
            v-model:value="form.gender"
            placeholder="请选择性别"
            allow-clear
            :options="genderOptions"
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
import { userApi } from '@/api/user'
import { departmentApi, type DepartmentInfo } from '@/api/department'

const GENDER_MAP: Record<number, string> = { 0: '未知', 1: '男', 2: '女' }

const genderOptions = [
  { label: '男', value: 1 },
  { label: '女', value: 2 },
  { label: '未知', value: 0 }
]

const queryFields: ReportQueryField[] = [
  { key: 'username', type: 'input', label: '登录账号', placeholder: '登录账号', width: 180 },
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
  { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
  { title: '登录账号', dataIndex: 'username', key: 'username', width: 130 },
  { title: '姓名', dataIndex: 'nickname', key: 'nickname', width: 110, ellipsis: true },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 170, ellipsis: true },
  { title: '性别', dataIndex: 'gender', key: 'gender', width: 70 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '最近登录', dataIndex: 'lastLoginTime', key: 'lastLoginTime', width: 160 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 170, fixed: 'right' }
]

function formatTime(val: string | null | undefined): string {
  return val ? String(val).replace('T', ' ').slice(0, 19) : '-'
}

// ═══ 数据请求：GET /api/user/page ═══
function fetcher(params: Record<string, any>) {
  return userApi.getPage(params)
}

// ═══ 部门选项 ═══
const deptOptions = ref<{ label: string; value: number }[]>([])

async function loadDeptOptions() {
  try {
    const res = (await departmentApi.getList()) as any
    const list: DepartmentInfo[] = Array.isArray(res) ? res : res?.data || []
    deptOptions.value = list.map(d => ({ label: d.departmentName, value: d.id }))
  } catch (e) {
    console.warn('[全部操作员] 部门列表获取失败', e)
  }
}

// ═══ 新增/编辑弹窗 ═══
const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: '',
  gender: undefined as number | undefined,
  deptId: undefined as number | undefined
})
const form = reactive(emptyForm())

const rules: Record<string, any> = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度3-50字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度6-100字符', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
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
    username: record.username,
    nickname: record.nickname || '',
    phone: record.phone || '',
    email: record.email || '',
    gender: record.gender,
    deptId: record.deptId
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
      // 后端 UserUpdateRequest 仅接收 昵称/邮箱/手机/头像/性别/部门/岗位
      const { nickname, email, phone, gender, deptId } = form
      await userApi.update(editingId.value, { nickname, email, phone, gender, deptId })
      message.success('操作员更新成功')
    } else {
      await userApi.create({ ...form } as any)
      message.success('操作员创建成功')
    }
    modalOpen.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[全部操作员] 保存失败', e)
  } finally {
    saving.value = false
  }
}

async function toggleStatus(record: any) {
  const target = record.status === 1 ? 0 : 1
  try {
    await userApi.updateStatus(record.id, target)
    message.success(target === 1 ? '操作员已启用' : '操作员已禁用')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[全部操作员] 状态切换失败', e)
  }
}

async function handleDelete(record: any) {
  try {
    await userApi.delete(record.id)
    message.success('删除成功')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[全部操作员] 删除失败', e)
  }
}

onMounted(loadDeptOptions)
</script>
