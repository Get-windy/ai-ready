<template>
  <div class="crm-customer-page">
    <!-- 视图切换 -->
    <div class="view-toggle" v-if="!loading">
      <a-radio-group v-model:value="currentView" button-style="solid" size="small">
        <a-radio-button value="list"><UnorderedListOutlined /> 列表</a-radio-button>
        <a-radio-button value="kanban"><AppstoreOutlined /> 看板</a-radio-button>
      </a-radio-group>
    </div>

    <!-- 列表视图 -->
    <template v-if="currentView === 'list'">
      <TableList
        ref="tableRef"
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        :table-key="'crm-customer-list'"
        :filter-fields="filterFields"
        :show-summary="true"
        :summary-data="summaryData"
        :show-export="true"
        add-text="新增客户"
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
          <a-button @click="handleImport"><template #icon><ImportOutlined /></template>导入</a-button>
        </template>

        <template #batch-actions>
          <a-button size="small" @click="handleBatchAssign">批量分配</a-button>
        </template>

        <template #name="{ record }">
          <a-space>
            <a-avatar :style="{ backgroundColor: getLevelColor(record.level) }" size="small">{{ record.name?.charAt(0) }}</a-avatar>
            <div>
              <div class="customer-name">{{ record.name }}</div>
              <div class="customer-code">{{ record.code }}</div>
            </div>
          </a-space>
        </template>

        <template #level="{ record }">
          <a-tag :color="getLevelColor(record.level)">{{ getLevelName(record.level) }}</a-tag>
        </template>

        <template #status="{ record }">
          <a-tag :color="record.status === 0 ? 'success' : 'error'">{{ record.status === 0 ? '正常' : '停用' }}</a-tag>
        </template>

        <template #action="{ record }">
          <a-space :size="4">
            <a-tooltip title="查看"><a-button type="link" size="small" @click="handleView(record)"><template #icon><EyeOutlined /></template></a-button></a-tooltip>
            <a-tooltip title="编辑"><a-button type="link" size="small" @click="handleEdit(record)"><template #icon><EditOutlined /></template></a-button></a-tooltip>
            <a-tooltip title="跟进"><a-button type="link" size="small" @click="handleFollow(record)"><template #icon><MessageOutlined /></template></a-button></a-tooltip>
            <a-dropdown>
              <a-button type="link" size="small"><MoreOutlined /></a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="handleViewFollows(record)">跟进记录</a-menu-item>
                  <a-menu-item @click="handleViewOrders(record)">订单记录</a-menu-item>
                  <a-menu-divider />
                  <a-menu-item danger @click="handleDelete(record)">删除</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </TableList>
    </template>

    <!-- 看板视图 -->
    <template v-if="currentView === 'kanban'">
      <div class="kanban-container">
        <div v-for="level in levelGroups" :key="level.value" class="kanban-column">
          <div class="kanban-column-header">
            <span class="kanban-column-title">{{ level.label }}</span>
            <span class="kanban-column-count">{{ getCustomersByLevel(level.value).length }}</span>
          </div>
          <div class="kanban-column-body">
            <div v-for="customer in getCustomersByLevel(level.value)" :key="customer.id" class="kanban-card" @click="handleView(customer)">
              <div class="kanban-card-header">
                <span class="kanban-card-name">{{ customer.name }}</span>
                <a-tag :color="customer.status === 0 ? 'success' : 'error'" size="small">{{ customer.status === 0 ? '正常' : '停用' }}</a-tag>
              </div>
              <div class="kanban-card-body">
                <div class="kanban-card-row"><span class="kanban-card-label">联系人:</span><span class="kanban-card-value">{{ customer.contactPerson }}</span></div>
                <div class="kanban-card-row"><span class="kanban-card-label">电话:</span><span class="kanban-card-value">{{ customer.phone }}</span></div>
                <div class="kanban-card-row"><span class="kanban-card-label">行业:</span><span class="kanban-card-value">{{ customer.industry }}</span></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 客户表单弹窗 -->
    <a-modal v-model:open="modalVisible" :title="modalTitle" :confirm-loading="modalLoading" width="700px" @ok="handleModalOk">
      <a-form ref="formRef" :model="formState" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="客户名称" name="name"><a-input v-model:value="formState.name" placeholder="请输入客户名称" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="客户编码" name="code"><a-input v-model:value="formState.code" placeholder="请输入客户编码" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系人" name="contactPerson"><a-input v-model:value="formState.contactPerson" placeholder="请输入联系人" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系电话" name="phone"><a-input v-model:value="formState.phone" placeholder="请输入联系电话" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="邮箱" name="email"><a-input v-model:value="formState.email" placeholder="请输入邮箱" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="客户等级" name="level">
              <a-select v-model:value="formState.level" placeholder="请选择等级">
                <a-select-option :value="1">VIP客户</a-select-option>
                <a-select-option :value="2">重要客户</a-select-option>
                <a-select-option :value="3">普通客户</a-select-option>
                <a-select-option :value="4">潜在客户</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="行业" name="industry">
              <a-select v-model:value="formState.industry" placeholder="请选择行业">
                <a-select-option value="IT">IT/互联网</a-select-option>
                <a-select-option value="制造业">制造业</a-select-option>
                <a-select-option value="金融">金融</a-select-option>
                <a-select-option value="零售">零售</a-select-option>
                <a-select-option value="其他">其他</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态" name="status">
              <a-radio-group v-model:value="formState.status"><a-radio :value="0">正常</a-radio><a-radio :value="1">停用</a-radio></a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="地址" name="address" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }"><a-input v-model:value="formState.address" placeholder="请输入地址" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="description" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }"><a-textarea v-model:value="formState.description" placeholder="请输入备注" :rows="3" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 跟进记录弹窗 -->
    <a-modal v-model:open="followModalVisible" title="添加跟进记录" :confirm-loading="followModalLoading" width="600px" @ok="handleFollowModalOk">
      <a-form :model="followForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="跟进类型" required>
          <a-select v-model:value="followForm.followType" placeholder="请选择跟进类型">
            <a-select-option :value="1">电话</a-select-option><a-select-option :value="2">拜访</a-select-option>
            <a-select-option :value="3">邮件</a-select-option><a-select-option :value="4">微信</a-select-option><a-select-option :value="5">其他</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="跟进内容" required><a-textarea v-model:value="followForm.content" placeholder="请输入跟进内容" :rows="4" /></a-form-item>
        <a-form-item label="跟进结果">
          <a-select v-model:value="followForm.result" placeholder="请选择跟进结果">
            <a-select-option :value="1">有意向</a-select-option><a-select-option :value="2">无意向</a-select-option><a-select-option :value="3">待跟进</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="下次跟进时间"><a-date-picker v-model:value="followForm.nextFollowTime" style="width: 100%" /></a-form-item>
      </a-form>
    </a-modal>

    <!-- CSV导入弹窗 -->
    <a-modal v-model:open="importVisible" title="导入客户" width="700px" :confirm-loading="importLoading" @ok="handleImportConfirm" @cancel="importVisible = false">
      <a-steps :current="importFileList.length > 0 ? 1 : 0" size="small" style="margin-bottom: 24px">
        <a-step title="上传文件" /><a-step title="字段映射" />
      </a-steps>
      <a-upload :file-list="importFileList" :before-upload="() => false" accept=".csv" :max-count="1" @change="handleImportFileChange">
        <a-button><template #icon><PlusOutlined /></template>选择CSV文件</a-button>
      </a-upload>
      <a-divider>字段映射</a-divider>
      <a-table :columns="importMappingColumns" :data-source="importFieldMapping" :pagination="false" size="small">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'csvField'"><a-input v-model:value="importFieldMapping[index].csvField" placeholder="CSV列名" size="small" /></template>
          <template v-if="column.key === 'required'"><a-tag :color="record.required ? 'red' : 'default'">{{ record.required ? '是' : '否' }}</a-tag></template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { PlusOutlined, EyeOutlined, EditOutlined, DeleteOutlined, ImportOutlined, MoreOutlined, MessageOutlined, UnorderedListOutlined, AppstoreOutlined } from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import { customerApi, type CustomerInfo } from '@/api/customer'
import { useUserStore } from '@/stores/user'
import { exportCsv } from '@/utils/exportCsv'

const userStore = useUserStore()
const router = useRouter()
const tableRef = ref()
const loading = ref(false)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const currentView = ref('list')
const kanbanData = ref<any[]>([])
let kanbanLoading = false

// 切到看板视图时加载全部客户数据用于看板展示
const fetchKanbanData = async () => {
  if (kanbanData.value.length > 0 || kanbanLoading) return
  kanbanLoading = true
  try {
    const res = await customerApi.getPage({ tenantId: userStore.tenantId, pageNum: 1, pageSize: 9999 })
    const pageData = (res as any).data
    kanbanData.value = pageData?.records || []
  } catch {
    kanbanData.value = dataSource.value.slice()
  } finally {
    kanbanLoading = false
  }
}

const columns = [
  { title: '客户信息', key: 'name', width: 200, slotName: 'name' },
  { title: '联系人', dataIndex: 'contactPerson', width: 100 },
  { title: '联系电话', dataIndex: 'phone', width: 120 },
  { title: '客户等级', dataIndex: 'level', key: 'level', width: 100, slotName: 'level' },
  { title: '行业', dataIndex: 'industry', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80, type: 'status' as const, slotName: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160, type: 'date' as const },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const, type: 'action' as const }
]

const filterFields = [
  { key: 'name', label: '客户名称', type: 'input' as const, placeholder: '输入客户名称' },
  { key: 'code', label: '客户编码', type: 'input' as const, placeholder: '输入客户编码' },
  { key: 'level', label: '客户等级', type: 'select' as const, options: [
    { label: 'VIP客户', value: 1 }, { label: '重要客户', value: 2 }, { label: '普通客户', value: 3 }, { label: '潜在客户', value: 4 }
  ]},
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '正常', value: 0 }, { label: '停用', value: 1 }
  ]},
  { key: 'dateRange', label: '创建时间', type: 'dateRange' as const }
]

const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  return [{ label: '本页数量', value: dataSource.value.length, type: 'default' as const }]
})

// ── 看板分组 ──
const levelGroups = [
  { value: 1, label: 'VIP客户' }, { value: 2, label: '重要客户' },
  { value: 3, label: '普通客户' }, { value: 4, label: '潜在客户' }
]
const getCustomersByLevel = (level: number) => kanbanData.value.filter(c => c.level === level)

// ── 辅助方法 ──
const levelColorMap: Record<number, string> = { 1: '#ff4d4f', 2: '#faad14', 3: '#1890ff', 4: '#52c41a' }
const levelTextMap: Record<number, string> = { 1: 'VIP客户', 2: '重要客户', 3: '普通客户', 4: '潜在客户' }
function getLevelColor(level: number): string { return levelColorMap[level] || '#999' }
function getLevelName(level: number): string { return levelTextMap[level] || '未知' }

// ── 数据加载 ──
async function fetchData() {
  loading.value = true
  try {
    const res = await customerApi.getPage({ tenantId: userStore.tenantId, ...searchFilters, pageNum: pagination.current, pageSize: pagination.pageSize })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData?.records || []; pagination.total = pageData?.total || 0
  } catch { message.error('加载客户列表失败') }
  finally { loading.value = false }
}

// ── 事件处理 ──
function handleView(record: any) { router.push(`/crm/customer/${record.id}`) }
function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }

// ── 弹窗相关 ──
const modalVisible = ref(false)
const modalLoading = ref(false)
const modalTitle = computed(() => isEdit.value ? '编辑客户' : '新增客户')
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const formState = reactive({ id: 0, name: '', code: '', contactPerson: '', phone: '', email: '', address: '', level: 3, industry: '', status: 0, description: '' })
const formRules = { name: [{ required: true, message: '请输入客户名称', trigger: 'blur' }], phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }] }

const followModalVisible = ref(false)
const followModalLoading = ref(false)
const currentCustomerId = ref(0)
const followForm = reactive({ followType: 1, content: '', result: 3, nextFollowTime: null as any })

function handleAdd() {
  isEdit.value = false; Object.assign(formState, { id: 0, name: '', code: '', contactPerson: '', phone: '', email: '', address: '', level: 3, industry: '', status: 0, description: '' })
  modalVisible.value = true
}
function handleEdit(record: any) { isEdit.value = true; Object.assign(formState, record); modalVisible.value = true }
function handleFollow(record: any) { currentCustomerId.value = record.id; Object.assign(followForm, { followType: 1, content: '', result: 3, nextFollowTime: null }); followModalVisible.value = true }
function handleViewFollows(record: any) { message.info(`查看跟进记录: ${record.name}`) }
function handleViewOrders(record: any) { message.info(`查看订单记录: ${record.name}`) }
async function handleModalOk() {
  try { await formRef.value?.validate() } catch { return }
  modalLoading.value = true
  try {
    if (isEdit.value) { await customerApi.update(formState.id, formState); message.success('更新成功') }
    else { await customerApi.create(formState); message.success('创建成功') }
    modalVisible.value = false; fetchData()
  } catch { message.error(isEdit.value ? '更新失败' : '创建失败') }
  finally { modalLoading.value = false }
}
async function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除', content: `确定要删除客户"${record.name}"吗？`, centered: true,
    async onOk() { try { await customerApi.delete(record.id); message.success('删除成功'); fetchData() } catch { message.error('删除失败') } }
  })
}
async function handleBatchDelete(ids: number[]) {
  Modal.confirm({
    title: '批量删除', content: `确定要删除选中的 ${ids.length} 个客户吗？`, centered: true,
    async onOk() { try { await customerApi.batchDelete(ids); message.success('批量删除成功'); fetchData() } catch { message.error('批量删除失败') } }
  })
}
async function handleFollowModalOk() {
  if (!followForm.content) { message.error('请输入跟进内容'); return }
  followModalLoading.value = true
  try {
    await customerApi.addFollowRecord(currentCustomerId.value, { customerId: currentCustomerId.value, followType: followForm.followType, content: followForm.content, result: followForm.result, nextFollowTime: followForm.nextFollowTime })
    message.success('跟进记录添加成功'); followModalVisible.value = false
  } catch { message.error('添加失败') }
  finally { followModalLoading.value = false }
}
function handleBatchAssign() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) { message.warning('请选择要分配的客户'); return }
  Modal.confirm({ title: '批量分配', content: `确定要批量分配选中的 ${keys.length} 个客户吗？`, centered: true, onOk() { message.success(`成功分配 ${keys.length} 个客户`); fetchData() } })
}

// ── 导入 ──
const importVisible = ref(false)
const importLoading = ref(false)
const importFileList = ref<any[]>([])
const importMappingColumns = [
  { title: '系统字段', dataIndex: 'label', width: 120 },
  { title: 'CSV列名', key: 'csvField', dataIndex: 'csvField' },
  { title: '必填', key: 'required', dataIndex: 'required', width: 60 }
]
const importFieldMapping = reactive([
  { csvField: '', systemField: 'name', required: true, label: '客户名称' }, { csvField: '', systemField: 'code', required: true, label: '客户编码' },
  { csvField: '', systemField: 'contactPerson', required: false, label: '联系人' }, { csvField: '', systemField: 'phone', required: true, label: '联系电话' },
  { csvField: '', systemField: 'email', required: false, label: '邮箱' }, { csvField: '', systemField: 'industry', required: false, label: '行业' },
  { csvField: '', systemField: 'address', required: false, label: '地址' }
])
function handleImport() { importFileList.value = []; importFieldMapping.forEach(m => { m.csvField = '' }); importVisible.value = true }
function handleImportFileChange(info: any) { importFileList.value = info.fileList.slice(-1) }
async function handleImportConfirm() {
  if (importFileList.value.length === 0) { message.warning('请先上传CSV文件'); return }
  const unmappedRequired = importFieldMapping.filter(f => f.required && !f.csvField)
  if (unmappedRequired.length > 0) { message.warning(`请为必填字段配置CSV映射：${unmappedRequired.map(f => f.label).join('、')}`); return }
  importLoading.value = true
  try {
    await customerApi.importCustomers({ file: importFileList.value[0], mapping: importFieldMapping.reduce((acc, m) => { if (m.csvField) acc[m.systemField] = m.csvField; return acc }, {} as Record<string, string>) })
    message.success('导入成功'); importVisible.value = false; fetchData()
  } catch { message.error('导入失败，请检查文件格式') }
  finally { importLoading.value = false }
}

// ── 导出 ──
function handleExport() {
  const headers = ['客户名称', '客户编码', '联系人', '联系电话', '邮箱', '行业', '等级', '状态', '创建时间']
  const rows = dataSource.value.map((row: any) => [row.name, row.code, row.contactPerson, row.phone, row.email, row.industry, getLevelName(row.level), row.status === 0 ? '正常' : '停用', row.createTime])
  exportCsv(headers, rows, '客户数据')
}

// 监听视图切换：切到看板时主动加载全量数据
watch(currentView, (val) => {
  if (val === 'kanban') {
    fetchKanbanData()
  }
})

onMounted(() => fetchData())
</script>

<style scoped>
.crm-customer-page { padding: 0; }
.view-toggle { margin-bottom: 16px; text-align: right; }
.customer-name { font-weight: 500; line-height: 1.2; }
.customer-code { font-size: 12px; color: #999; line-height: 1.2; }
.kanban-container { display: flex; gap: 16px; height: 100%; overflow: auto; }
.kanban-column { flex: 1; min-width: 260px; background-color: #f5f5f5; border-radius: 4px; display: flex; flex-direction: column; }
.kanban-column-header { padding: 12px 16px; background-color: #fff; border-bottom: 1px solid #e8e8e8; display: flex; justify-content: space-between; align-items: center; }
.kanban-column-title { font-size: 14px; font-weight: 500; color: #303133; }
.kanban-column-count { font-size: 12px; color: #909399; }
.kanban-column-body { flex: 1; padding: 8px; overflow: auto; }
.kanban-card { background-color: #fff; border-radius: 4px; padding: 12px; margin-bottom: 8px; cursor: pointer; transition: box-shadow 0.2s; }
.kanban-card:hover { box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1); }
.kanban-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.kanban-card-name { font-size: 14px; font-weight: 500; color: #303133; }
.kanban-card-body { display: flex; flex-direction: column; gap: 4px; }
.kanban-card-row { display: flex; justify-content: space-between; font-size: 12px; }
.kanban-card-label { color: #909399; }
.kanban-card-value { color: #606266; }
</style>
