<template>
  <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :table-key="'sale-customer-list'"
    :filter-fields="filterFields"
    :show-summary="true"
    :summary-data="summaryData"
    :show-export="true"
    add-text="新建客户"
    add-permission="'crm:customer:create'"
    @add="handleAdd"
    @edit="handleEdit"
    @view="handleView"
    @delete="handleDelete"
    @batch-delete="handleBatchDelete"
    @refresh="fetchData"
    @search="handleSearch"
    @page-change="handlePageChange"
    @sort-change="handleSortChange"
    @filter-change="handleFilterChange"
    @export="handleExport"
  >
    <template #toolbar-actions>
      <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
        更新 {{ dayjs(lastUpdated).format('HH:mm') }}
      </span>
    </template>

    <template #empty>
      <a-empty v-if="hasActiveFilters" description="当前筛选条件下无匹配客户">
        <template #image><SearchOutlined style="font-size: 48px; color: #faad14" /></template>
        <a-button @click="handleResetFilters">清除筛选</a-button>
      </a-empty>
      <a-empty v-else description="暂无客户数据">
        <template #image><InboxOutlined style="font-size: 48px; color: #d9d9d9" /></template>
        <a-button type="primary" @click="handleAdd">新建客户</a-button>
      </a-empty>
    </template>

    <template #status="{ record }">
      <a-tag :color="record.status === 1 ? 'green' : 'default'">
        {{ record.status === 1 ? '正常' : '停用' }}
      </a-tag>
    </template>

    <template #level="{ record }">
      <a-tag :color="getLevelColor(record.level)">
        {{ getLevelText(record.level) }}
      </a-tag>
    </template>

    <template #action="{ record }">
      <a-space :size="4">
        <a-tooltip title="查看">
          <a-button v-permission="'crm:customer:list'" type="link" size="small" @click="handleView(record)">
            <template #icon><EyeOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip title="编辑">
          <a-button v-permission="'crm:customer:update'" type="link" size="small" @click="handleEdit(record)">
            <template #icon><EditOutlined /></template>
          </a-button>
        </a-tooltip>
      </a-space>
    </template>
  </TableList>

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
      </a-row>
      <a-row>
        <a-col :span="12">
          <a-form-item label="状态" name="status" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-select v-model:value="formData.status" placeholder="请选择状态">
              <a-select-option :value="1">正常</a-select-option>
              <a-select-option :value="0">停用</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="联系地址" name="address">
        <a-textarea v-model:value="formData.address" placeholder="请输入联系地址" :rows="2" />
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
defineOptions({ name: 'SaleCustomersTab' })

import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'
import { EyeOutlined, EditOutlined, DeleteOutlined, InboxOutlined, SearchOutlined } from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import { customerApi } from '@/api/customer'
import { useUserStore } from '@/stores/user'
import { useExport } from '@/composables/useExport'

const { execute: executeExport } = useExport()

const router = useRouter()
const userStore = useUserStore()
const tableRef = ref()
const loading = ref(false)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const columns = [
  { title: '客户编码', dataIndex: 'code', key: 'code', width: 130 },
  { title: '客户名称', dataIndex: 'name', key: 'name', width: 160, sortable: true },
  { title: '联系人', dataIndex: 'contactPerson', key: 'contactPerson', width: 100 },
  { title: '联系电话', dataIndex: 'phone', key: 'phone', width: 120 },
  { title: '等级', dataIndex: 'level', key: 'level', width: 80, slotName: 'level' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80, type: 'status' as const, slotName: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160, type: 'date' as const },
  { title: '操作', key: 'action', width: 120, fixed: 'right' as const, type: 'action' as const }
]

const filterFields = [
  { key: 'code', label: '客户编码', type: 'input' as const, placeholder: '输入客户编码' },
  { key: 'name', label: '客户名称', type: 'input' as const, placeholder: '输入客户名称' },
  { key: 'contactPerson', label: '联系人', type: 'input' as const, placeholder: '输入联系人' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '正常', value: 1 }, { label: '停用', value: 0 }
  ]},
  { key: 'level', label: '等级', type: 'select' as const, options: [
    { label: 'A级', value: 1 }, { label: 'B级', value: 2 }, { label: 'C级', value: 3 }
  ]},
  { key: 'dateRange', label: '创建日期', type: 'dateRange' as const }
]

const levelColorMap: Record<number, string> = { 1: 'gold', 2: 'blue', 3: 'default' }
const levelTextMap: Record<number, string> = { 1: 'A级', 2: 'B级', 3: 'C级' }
const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  return [{ label: '本页数量', value: dataSource.value.length, type: 'default' as const }]
})
function getLevelColor(level: number): string { return levelColorMap[level] || 'default' }
function getLevelText(level: number): string { return levelTextMap[level] || '未知' }

// ── 客户表单弹窗 ──
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

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      tenantId: userStore.tenantId,
      ...searchFilters
    }
    // 映射搜索字段到API字段名
    if (params.name === undefined && searchFilters.name) params.name = searchFilters.name
    if (params.code === undefined && searchFilters.code) params.code = searchFilters.code
    if (params.contactPerson === undefined && searchFilters.contactPerson) params.contactPerson = searchFilters.contactPerson

    const res = await customerApi.getPage(params)
    const pageData = (res as any).data ?? res
    dataSource.value = pageData?.records || []
    pagination.total = pageData?.total || 0
    lastUpdated.value = new Date().toISOString()
  } catch { message.error('获取客户列表失败') }
  finally { loading.value = false }
}

function handleView(record: any) {
  router.push(`/crm/customer/${record.id}`)
}

function handleEdit(record: any) {
  formMode.value = 'edit'
  formData.id = record.id
  formData.customerName = record.name || ''
  formData.customerCode = record.code || ''
  formData.contactName = record.contactPerson || ''
  formData.contactPhone = record.phone || ''
  formData.email = record.email || ''
  formData.address = record.address || ''
  formData.customerType = record.industry || undefined
  formData.level = record.level || 3
  formData.status = record.status ?? 1
  formData.remark = record.description || ''
  formModalVisible.value = true
}

function handleAdd() {
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

async function handleDelete(record: any) {
  Modal.confirm({
    title: '删除客户', content: `确认删除客户 "${record.name}"？`, okText: '确认删除', cancelText: '取消', centered: true,
    async onOk() {
      try { await customerApi.delete(record.id); message.success('删除成功'); fetchData() }
      catch { message.error('删除失败') }
    }
  })
}

async function handleBatchDelete(ids: number[]) {
  Modal.confirm({
    title: '批量删除', content: `确认删除选中的 ${ids.length} 个客户？`, okText: '确认删除', cancelText: '取消', centered: true,
    async onOk() {
      try { await customerApi.batchDelete(ids); message.success('批量删除成功'); fetchData() }
      catch { message.error('批量删除失败') }
    }
  })
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.current = 1; fetchData()
}

const handleFormSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  formSubmitting.value = true
  try {
    const payload = {
      name: formData.customerName,
      code: formData.customerCode,
      contactPerson: formData.contactName,
      phone: formData.contactPhone,
      email: formData.email,
      address: formData.address,
      industry: formData.customerType,
      level: formData.level,
      status: formData.status,
      description: formData.remark
    }
    if (formMode.value === 'add') {
      await customerApi.create(payload)
      message.success('新建客户成功')
    } else {
      await customerApi.update(formData.id!, payload)
      message.success('编辑客户成功')
    }
    formModalVisible.value = false
    pagination.current = 1
    fetchData()
  } catch {
    message.error(formMode.value === 'add' ? '新建客户失败' : '编辑客户失败')
  } finally {
    formSubmitting.value = false
  }
}

function handleExport() {
  executeExport({
    fileName: '客户列表',
    headers: ['客户编码', '客户名称', '联系人', '联系电话', '等级', '状态', '创建时间'],
    fetchAll: () => customerApi.getPage({ pageNum: 1, pageSize: pagination.total, tenantId: userStore.tenantId, ...searchFilters }),
    mapToRows: (list: any[]) => list.map((row: any) => [
      row.code || '', row.name || '', row.contactPerson || '', row.phone || '',
      getLevelText(row.level), row.status === 1 ? '正常' : '停用', row.createTime || ''
    ]),
    fallbackRows: () => dataSource.value.map((row: any) => [
      row.code || '', row.name || '', row.contactPerson || '', row.phone || '',
      getLevelText(row.level), row.status === 1 ? '正常' : '停用', row.createTime || ''
    ]),
    total: pagination.total,
  })
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
const debouncedFetch = ref(0)
function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters); pagination.current = 1
  clearTimeout(debouncedFetch.value)
  debouncedFetch.value = window.setTimeout(() => fetchData(), 400)
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('sale:refresh', fetchData)
})
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('sale:refresh', fetchData)
})

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleAdd() }
}
</script>

<style scoped>
.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}
</style>
