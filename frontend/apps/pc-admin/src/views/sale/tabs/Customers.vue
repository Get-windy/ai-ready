<template>
  <div class="customers-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ totalCount }}</div>
          <div class="stat-card-label">客户总数</div>
        </div>
        <TeamOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-active">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ activeCount }}</div>
          <div class="stat-card-label">正常客户</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-a-level">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ aLevelCount }}</div>
          <div class="stat-card-label">A级客户</div>
        </div>
        <StarOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-inactive">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ inactiveCount }}</div>
          <div class="stat-card-label">停用客户</div>
        </div>
        <StopOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :table-key="'sale-customer-list'"
      :filter-fields="filterFields"
      :show-summary="true"
      :summary-data="summaryData"
      :show-export="true"
      :selectable="true"
      add-text="新建客户"
      add-permission="'crm:customer:create'"
      @add="handleAdd"
      @edit="handleEdit"
      @view="handleView"
      @delete="handleDelete"
      @batch-delete="handleBatchDelete"
      @refresh="debounceClick('refresh', fetchData)"
      @search="handleSearch"
      @page-change="handlePageChange"
      @sort-change="handleSortChange"
      @filter-change="handleFilterChange"
      @export="handleExport"
      @cell-dblclick="handleView"
      @selection-change="handleSelectionChange"
    >
    <template #toolbar-actions>
      <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
        更新 {{ dayjs(lastUpdated).format('HH:mm') }}
      </span>
    </template>

    <template #empty>
      <div class="table-empty">
        <template v-if="hasError">
          <WarningOutlined class="table-empty-icon" style="color: #faad14" />
          <p class="table-empty-text">加载失败</p>
          <a-button type="primary" size="small" @click="fetchData" class="table-empty-action">
            <ReloadOutlined /> 重试
          </a-button>
        </template>
        <template v-else>
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <InboxOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            当前筛选条件下无匹配客户，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无客户数据，点击「新建客户」开始创建
          </p>
        </template>
      </div>
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
  </VxeTableList>
  </div>

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
            <a-input v-model:value="formData.customerName" placeholder="请输入客户名称" size="small" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="客户编码" name="customerCode" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-input v-model:value="formData.customerCode" placeholder="自动生成或手动输入" size="small" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row>
        <a-col :span="12">
          <a-form-item label="联系人" name="contactName" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-input v-model:value="formData.contactName" placeholder="请输入联系人姓名" size="small" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="联系电话" name="contactPhone" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-input v-model:value="formData.contactPhone" placeholder="请输入联系电话" size="small" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row>
        <a-col :span="12">
          <a-form-item label="电子邮箱" name="email" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-input v-model:value="formData.email" placeholder="请输入电子邮箱" size="small" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="客户等级" name="level" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-select v-model:value="formData.level" placeholder="请选择客户等级" size="small">
              <a-select-option :value="1">A级（重点客户）</a-select-option>
              <a-select-option :value="2">B级（普通客户）</a-select-option>
              <a-select-option :value="3">C级（潜在客户）</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="客户类型" name="customerType" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-select v-model:value="formData.customerType" placeholder="请选择客户类型" size="small">
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
            <a-select v-model:value="formData.status" placeholder="请选择状态" size="small">
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
import { EyeOutlined, EditOutlined, DeleteOutlined, InboxOutlined, SearchOutlined, TeamOutlined, CheckCircleOutlined, StarOutlined, StopOutlined, WarningOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { customerApi } from '@/api/customer'
import { useUserStore } from '@/stores/user'
import { useExport } from '@/composables/useExport'

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

const { execute: executeExport } = useExport()

const router = useRouter()
const userStore = useUserStore()
const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const lastUpdated = ref('')

// ── 统计数据 ────────────────────────────────────────────
const totalCount = computed(() => dataSource.value.length)
const activeCount = computed(() => dataSource.value.filter(r => r.status === 1).length)
const inactiveCount = computed(() => dataSource.value.filter(r => r.status === 0).length)
const aLevelCount = computed(() => dataSource.value.filter(r => r.level === 1).length)

const tableDataSource = dataSource

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const vxeColumns = computed(() => [
    { title: '客户编码', field: 'code', width: 130 },
    { title: '客户名称', field: 'name', width: 160, sortable: true },
    { title: '联系人', field: 'contactPerson', width: 100 },
    { title: '联系电话', field: 'phone', width: 120 },
    { title: '等级', field: 'level', width: 80, formatter: ({ cellValue }) => getLevelText(cellValue) },
    { title: '状态', field: 'status', width: 80, formatter: ({ cellValue }) => `<span class="ant-tag ant-tag-${cellValue === 1 ? 'green' : 'red'}">${cellValue === 1 ? '正常' : '停用'}</span>` },
    { title: '创建时间', field: 'createTime', width: 160 },
    { title: '操作', field: 'action', width: 120, fixed: 'right', type: 'action' }
  ])

  // 选择变化处理
  const selectedRowKeys = ref<number[]>([])
  function handleSelectionChange(keys: number[]) {
    selectedRowKeys.value = keys
  }

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
const formRules: any = {
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
    hasError.value = false
  } catch (err) { console.warn('[销售客户] 获取客户列表', err); hasError.value = true }
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

function handleParentCreate() {
  handleAdd()
}

async function handleDelete(record: any) {
  Modal.confirm({
    title: '删除客户', content: `确认删除客户 "${record.name}"？`, okText: '确认删除', cancelText: '取消', centered: true,
    async onOk() {
      try { await customerApi.delete(record.id); message.success('删除成功'); fetchData() }
      catch (err) { console.warn('[销售客户] 删除客户', err); message.error('删除失败') }
    }
  })
}

async function handleBatchDelete(ids: number[]) {
  Modal.confirm({
    title: '批量删除', content: `确认删除选中的 ${ids.length} 个客户？`, okText: '确认删除', cancelText: '取消', centered: true,
    async onOk() {
      try { await customerApi.batchDelete(ids); message.success('批量删除成功'); fetchData() }
      catch (err) { console.warn('[销售客户] 批量删除客户', err); message.error('批量删除失败') }
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
  } catch (err) {
    console.warn('[销售客户] 保存客户', err); message.error(formMode.value === 'add' ? '新建客户失败' : '编辑客户失败')
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
  window.addEventListener('sale:create', handleParentCreate)
})
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('sale:refresh', fetchData)
  window.removeEventListener('sale:create', handleParentCreate)
})

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); debounceClick('add', handleAdd); return }
}
defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.customers-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  padding: 16px;
}

.customers-page > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
}

/* 空状态 */
.table-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0;
}

.table-empty-icon {
  font-size: 48px;
  color: #d9d9d9;
}

.table-empty-text {
  color: #999;
  margin-top: 12px;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-active { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-a-level { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-inactive { background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}


.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}





/* 响应式 */
@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 45%;
    min-width: 120px;
  }
  .customers-page {
    padding: 8px;
  }
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

</style>
