<template>
  <ModuleLayout
    :breadcrumb-items="breadcrumbItems"
    :current-view="currentView"
    :selected-count="selectedRowKeys.length"
    :current-page="pagination.current"
    :total-pages="Math.ceil(pagination.total / pagination.pageSize)"
    :page-size="pagination.pageSize"
    :total-items="pagination.total"
    @view-change="handleViewChange"
    @clear-selection="handleClearSelection"
    @page-change="handlePageChange"
    @page-size-change="handlePageSizeChange"
  >
    <template #actions>
      <a-button type="primary" @click="handleAdd">
        <template #icon><PlusOutlined /></template>
        新建客户
      </a-button>
      <a-button @click="handleExport">
        <template #icon><ExportOutlined /></template>
        导出
      </a-button>
    </template>

    <template #list-view>
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
    :error="error"
    :empty="!loading && !error && dataSource.length === 0"
        :row-selection="rowSelection"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'">
              {{ record.status === 1 ? '正常' : '停用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'level'">
            <a-tag :color="getLevelColor(record.level)">
              {{ getLevelText(record.level) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a @click="handleEdit(record)">编辑</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </template>
  </ModuleLayout>

  <!-- 新建/编辑客户弹窗 -->
  <a-modal
    v-model:open="formModalVisible"
    :title="formMode === 'add' ? '新建客户' : '编辑客户'"
    width="800px"
    centered
    :confirm-loading="formSubmitting"
    ok-text="确认"
    cancel-text="取消"
    @ok="handleFormSubmit"
    @cancel="formModalVisible = false"
  >
    <a-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      :label-col="{ span: 5 }"
      :wrapper-col="{ span: 19 }"
    >
      <a-row>
        <a-col :span="12">
          <a-form-item label="客户名称" name="customerName" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-input v-model:value="formData.customerName" placeholder="请输入客户名称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="客户编码" name="customerCode" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-input v-model:value="formData.customerCode" placeholder="自动生成或手动输入" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row>
        <a-col :span="12">
          <a-form-item label="联系人" name="contactName" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-input v-model:value="formData.contactName" placeholder="请输入联系人姓名" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="联系电话" name="contactPhone" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-input v-model:value="formData.contactPhone" placeholder="请输入联系电话" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row>
        <a-col :span="12">
          <a-form-item label="电子邮箱" name="email" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-input v-model:value="formData.email" placeholder="请输入电子邮箱" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="客户等级" name="level" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-select v-model:value="formData.level" placeholder="请选择客户等级">
              <a-select-option :value="1">A级（重点客户）</a-select-option>
              <a-select-option :value="2">B级（普通客户）</a-select-option>
              <a-select-option :value="3">C级（潜在客户）</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row>
        <a-col :span="12">
          <a-form-item label="客户类型" name="customerType" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-select v-model:value="formData.customerType" placeholder="请选择客户类型">
              <a-select-option value="企业">企业</a-select-option>
              <a-select-option value="个人">个人</a-select-option>
              <a-select-option value="政府">政府</a-select-option>
              <a-select-option value="其他">其他</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="状态" name="status" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-select v-model:value="formData.status" placeholder="请选择状态">
              <a-select-option :value="1">正常</a-select-option>
              <a-select-option :value="0">停用</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="联系地址" name="address" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
        <a-textarea v-model:value="formData.address" placeholder="请输入联系地址" :rows="2" />
      </a-form-item>
      <a-form-item label="备注" name="remark" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { PlusOutlined, ExportOutlined } from '@ant-design/icons-vue'
import { ModuleLayout } from '@ai-ready/components'

const router = useRouter()

const loading = ref(false)
const error = ref<string | null>(null)
const dataSource = ref<any[]>([])
const selectedRowKeys = ref<number[]>([])
const currentView = ref('list')

const breadcrumbItems = computed(() => [
  { text: '销售管理', path: '/sale' },
  { text: '客户' }
])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const columns = [
  { title: '客户编码', dataIndex: 'customerCode', key: 'customerCode', width: 150 },
  { title: '客户名称', dataIndex: 'customerName', key: 'customerName' },
  { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 100 },
  { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 120 },
  { title: '等级', dataIndex: 'level', key: 'level', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 120 }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: number[]) => {
    selectedRowKeys.value = keys
  }
}))

const getLevelColor = (level: number) => {
  const colors: Record<number, string> = {
    1: 'gold',
    2: 'blue',
    3: 'default'
  }
  return colors[level] || 'default'
}

const getLevelText = (level: number) => {
  const texts: Record<number, string> = {
    1: 'A级',
    2: 'B级',
    3: 'C级'
  }
  return texts[level] || '未知'
}

const fetchData = async () => {
  loading.value = true
  try {
    dataSource.value = []
    pagination.total = 0
  } catch (error) {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleViewChange = (view: string) => {
  currentView.value = view
}

const handleClearSelection = () => {
  selectedRowKeys.value = []
}

const handlePageChange = (page: number) => {
  pagination.current = page
  fetchData()
}

const handlePageSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.current = 1
  fetchData()
}

// ========== 客户表单弹窗 ==========
const formModalVisible = ref(false)
const formSubmitting = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()
const formData = reactive({
  id: undefined as number | undefined,
  customerName: '',
  customerCode: '',
  contactName: '',
  contactPhone: '',
  email: '',
  address: '',
  customerType: undefined as string | undefined,
  level: 3,
  status: 1,
  remark: ''
})

const formRules = {
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  customerCode: [{ required: true, message: '请输入客户编码', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  email: [{ type: 'email' as const, message: '请输入正确的邮箱格式', trigger: 'blur' }],
  customerType: [{ required: true, message: '请选择客户类型', trigger: 'change' }]
}

const handleAdd = () => {
  formMode.value = 'add'
  formData.id = undefined
  formData.customerName = ''
  formData.customerCode = ''
  formData.contactName = ''
  formData.contactPhone = ''
  formData.email = ''
  formData.address = ''
  formData.customerType = undefined
  formData.level = 3
  formData.status = 1
  formData.remark = ''
  formModalVisible.value = true
}

const handleView = (record: any) => {
  router.push(`/crm/customer/${record.id}`)
}

const handleEdit = (record: any) => {
  formMode.value = 'edit'
  formData.id = record.id
  formData.customerName = record.customerName || ''
  formData.customerCode = record.customerCode || ''
  formData.contactName = record.contactName || ''
  formData.contactPhone = record.contactPhone || ''
  formData.email = record.email || ''
  formData.address = record.address || ''
  formData.customerType = record.customerType || undefined
  formData.level = record.level || 3
  formData.status = record.status ?? 1
  formData.remark = record.remark || ''
  formModalVisible.value = true
}

const handleFormSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  formSubmitting.value = true
  try {
    // await customerApi.save(formData)
    if (formMode.value === 'add') {
      message.success('新建客户成功')
    } else {
      message.success('编辑客户成功')
    }
    formModalVisible.value = false
    fetchData()
  } catch {
    message.error('操作失败')
  } finally {
    formSubmitting.value = false
  }
}

const handleExport = async () => {
  const hide = message.loading('正在导出...', 0)
  try {
    // await customerApi.export(pagination.current, pagination.pageSize)
    await new Promise(resolve => setTimeout(resolve, 1000))
    hide()
    message.success('导出成功，文件下载中')
  } catch {
    hide()
    message.error('导出失败')
  }
}

onMounted(() => {
  fetchData()
})
</script>
