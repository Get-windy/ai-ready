<template>
  <PageContainer full-height>
    <template #header>
      <div class="lead-page-header">
        <div class="lead-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>线索管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="lead-page-header-title">线索管理</h2>
        </div>
        <div class="lead-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="fetchData">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <ErrorBoundary @reset="fetchData">
      <!-- 统计卡片 -->
      <div class="stats-cards">
        <a-row :gutter="16">
          <a-col :span="6">
            <div class="stat-card stat-card-blue">
              <div class="stat-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
                <FileAddOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">新线索</div>
                <div class="stat-value">{{ statusCounts.new }}</div>
                <div class="stat-desc">待分配跟进</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-orange">
              <div class="stat-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
                <SyncOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">跟进中</div>
                <div class="stat-value">{{ statusCounts.following }}</div>
                <div class="stat-desc">正在跟进</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-green">
              <div class="stat-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
                <CheckCircleOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">已转化</div>
                <div class="stat-value">{{ statusCounts.converted }}</div>
                <div class="stat-desc">成功转化客户</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-purple">
              <div class="stat-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
                <StarOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">高分线索</div>
                <div class="stat-value">{{ statusCounts.highScore }}</div>
                <div class="stat-desc">评分≥80</div>
              </div>
            </div>
          </a-col>
        </a-row>
      </div>

      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableDataSource"
        :loading="loading"
        :pagination="pagination"
        :filter-fields="filterFields"
        :show-export="true"
        :selectable="true"
        add-text="新建线索"
        @add="handleAdd"
        @refresh="fetchData"
        @search="handleSearch"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
        @selection-change="handleSelectionChange"
        @export="handleExport"
      >
        <template #toolbar-actions>
          <a-button size="small" @click="handleBatchAssign">
            <template #icon><TeamOutlined /></template>
            批量分配
          </a-button>
          <a-button size="small" @click="handleImport">
            <template #icon><ImportOutlined /></template>
            导入线索
          </a-button>
        </template>

        <template #batch-actions>
          <a-button size="small" type="primary" ghost @click="handleBatchAssign">
            <template #icon><TeamOutlined /></template>
            批量分配
          </a-button>
          <a-button size="small" @click="handleBatchConvert">
            <template #icon><SwapRightOutlined /></template>
            批量转化
          </a-button>
        </template>

        <template #empty>
          <div class="table-empty">
            <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
            <InboxOutlined v-else class="table-empty-icon" />
            <p v-if="hasActiveFilters" class="table-empty-text">
              没有符合条件的线索，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无线索数据，点击右上角「新建线索」开始创建
            </p>
          </div>
        </template>

        <template #action="{ record }">
          <a-space :size="4">
            <a-tooltip title="查看详情">
              <a-button type="link" size="small" @click="handleView(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="编辑">
              <a-button type="link" size="small" @click="handleEdit(record)">
                <template #icon><EditOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="分配">
              <a-button type="link" size="small" @click="handleAssign(record)">
                <template #icon><TeamOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status < 2" title="转化为客户">
              <a-button type="link" size="small" @click="handleConvert(record)">
                <template #icon><SwapRightOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-dropdown trigger="click">
              <a-button type="link" size="small" class="action-more-btn">
                <template #icon><MoreOutlined /></template>
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                  <a-menu-item key="follow"><MessageOutlined /> 添加跟进</a-menu-item>
                  <a-menu-item key="history"><HistoryOutlined /> 跟进记录</a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="delete" danger><DeleteOutlined /> 删除</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </VxeTableList>
    </ErrorBoundary>

    <!-- 线索表单弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirm-loading="modalLoading"
      width="700px"
      @ok="handleModalOk"
    >
      <a-form ref="formRef" :model="formState" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="线索名称" name="name">
              <a-input v-model:value="formState.name" placeholder="请输入线索名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="公司名称" name="companyName">
              <a-input v-model:value="formState.companyName" placeholder="请输入公司名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系人" name="contactName">
              <a-input v-model:value="formState.contactName" placeholder="请输入联系人" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系电话" name="phone">
              <a-input v-model:value="formState.phone" placeholder="请输入联系电话" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="手机号码" name="mobile">
              <a-input v-model:value="formState.mobile" placeholder="请输入手机号码" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="邮箱" name="email">
              <a-input v-model:value="formState.email" placeholder="请输入邮箱" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="来源渠道" name="source">
              <a-select v-model:value="formState.source" placeholder="请选择来源渠道">
                <a-select-option value="website">官网咨询</a-select-option>
                <a-select-option value="weixin">微信公众号</a-select-option>
                <a-select-option value="email">邮件咨询</a-select-option>
                <a-select-option value="phone">电话咨询</a-select-option>
                <a-select-option value="social">社交媒体</a-select-option>
                <a-select-option value="other">其他</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="评分" name="score">
              <a-slider v-model:value="formState.score" :min="0" :max="100" :marks="{ 0: '0', 50: '50', 80: '80', 100: '100' }" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
              <a-textarea v-model:value="formState.remark" placeholder="请输入备注" :rows="3" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 分配弹窗 -->
    <a-modal
      v-model:open="assignVisible"
      title="分配线索"
      :confirm-loading="assignLoading"
      width="400px"
      @ok="handleAssignConfirm"
    >
      <a-form layout="vertical">
        <a-form-item label="选择销售人员">
          <a-select v-model:value="assignForm.userId" placeholder="请选择销售人员">
            <a-select-option v-for="user in salesUsers" :key="user.id" :value="user.id">
              {{ user.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="assignForm.remark" placeholder="分配备注" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 转化确认弹窗 -->
    <a-modal
      v-model:open="convertVisible"
      title="线索转化确认"
      :confirm-loading="convertLoading"
      width="500px"
      @ok="handleConvertConfirm"
    >
      <a-alert message="将线索转化为客户" type="info" style="margin-bottom: 16px" />
      <a-descriptions bordered :column="2" size="small" v-if="currentLead">
        <a-descriptions-item label="线索名称">{{ currentLead.name }}</a-descriptions-item>
        <a-descriptions-item label="公司">{{ currentLead.companyName }}</a-descriptions-item>
        <a-descriptions-item label="联系人">{{ currentLead.contactName }}</a-descriptions-item>
        <a-descriptions-item label="电话">{{ currentLead.phone }}</a-descriptions-item>
        <a-descriptions-item label="来源">{{ sourceTextMap[currentLead.source] }}</a-descriptions-item>
        <a-descriptions-item label="评分">{{ currentLead.score }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { PageContainer } from '@/components'
import { leadApi } from '@/api/crm'
import { exportCsv } from '@/utils/exportCsv'
import {
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  TeamOutlined,
  ImportOutlined,
  SwapRightOutlined,
  ReloadOutlined,
  SearchOutlined,
  InboxOutlined,
  MoreOutlined,
  MessageOutlined,
  HistoryOutlined,
  FileAddOutlined,
  SyncOutlined,
  CheckCircleOutlined,
  StarOutlined
} from '@ant-design/icons-vue'

interface Lead {
  id: number
  name: string
  companyName: string
  contactName: string
  phone: string
  mobile?: string
  email?: string
  source: string
  status: number
  score: number
  createdAt: string
}

const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const dataSource = ref<Lead[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const lastUpdateTime = ref<string>('')
const selectedRowKeys = ref<number[]>([])
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// 状态统计
const statusCounts = computed(() => {
  const newLeads = dataSource.value.filter(l => l.status === 0).length
  const following = dataSource.value.filter(l => l.status === 1).length
  const converted = dataSource.value.filter(l => l.status === 2).length
  const highScore = dataSource.value.filter(l => l.score >= 80).length
  return { new: newLeads, following, converted, highScore }
})

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// 数据源
const tableDataSource = dataSource

const vxeColumns = computed(() => [
  { field: 'name', title: '线索名称', width: 180, fixed: 'left', formatter: ({ row }: any) => row.name || '' },
  { field: 'companyName', title: '公司/联系人', width: 180, formatter: ({ row }: any) => `${row.companyName || ''} / ${row.contactName || ''}` },
  { field: 'phone', title: '联系方式', width: 150, formatter: ({ row }: any) => row.phone || row.mobile || '' },
  { field: 'source', title: '来源渠道', width: 110, formatter: ({ cellValue }: any) => sourceTextMap[cellValue] || cellValue },
  { field: 'score', title: '评分', width: 120, formatter: ({ cellValue }: any) => `${cellValue || 0}` },
  { field: 'status', title: '状态', width: 100, align: 'center', formatter: ({ cellValue }: any) => getStatusText(cellValue) },
  { field: 'createdAt', title: '添加时间', width: 160 },
  { field: 'action', title: '操作', width: 180, fixed: 'right', type: 'action' }
])

const filterFields = [
  { key: 'name', label: '线索名称', type: 'input' as const, placeholder: '输入线索名称' },
  { key: 'source', label: '来源渠道', type: 'select' as const, options: [
    { label: '官网咨询', value: 'website' },
    { label: '微信公众号', value: 'weixin' },
    { label: '邮件咨询', value: 'email' },
    { label: '电话咨询', value: 'phone' },
    { label: '社交媒体', value: 'social' },
    { label: '其他', value: 'other' }
  ]},
  { key: 'status', label: '线索状态', type: 'select' as const, options: [
    { label: '新线索', value: 0 },
    { label: '跟进中', value: 1 },
    { label: '已转化', value: 2 },
    { label: '已关闭', value: 3 }
  ]}
]

const statusColorMap: Record<number, string> = { 0: 'blue', 1: 'orange', 2: 'green', 3: 'red' }
const statusTextMap: Record<number, string> = { 0: '新线索', 1: '跟进中', 2: '已转化', 3: '已关闭' }
const sourceColorMap: Record<string, string> = { website: 'blue', weixin: 'green', email: 'purple', phone: 'orange', social: 'lime', other: 'default' }
const sourceTextMap: Record<string, string> = { website: '官网咨询', weixin: '微信公众号', email: '邮件咨询', phone: '电话咨询', social: '社交媒体', other: '其他' }

function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }
function getScoreColor(score: number): string {
  if (score >= 80) return '#52c41a'
  if (score >= 60) return '#1890ff'
  if (score >= 40) return '#faad14'
  return '#ff4d4f'
}
function getAvatarColor(status: number): string {
  return statusColorMap[status] || '#999'
}

// 数据加载
async function fetchData(silent = false) {
  if (!silent) loading.value = true
  hasError.value = false
  try {
    const res = await leadApi.page({
      keyword: searchFilters.keyword,
      leadStatus: searchFilters.status,
      source: searchFilters.source,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    const result = res as any
    dataSource.value = (result.records || result.data?.records || []) as Lead[]
    pagination.total = result.total ?? result.data?.total ?? 0
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (err) {
    if (!silent) {
      hasError.value = true
      message.error('获取线索数据失败')
    }
    console.warn('[CRM线索] 获取线索列表失败', err)
    dataSource.value = []
    pagination.total = 0
  } finally {
    if (!silent) loading.value = false
    refreshLoading.value = false
  }
}

const handleRefresh = () => {
  lastUpdateTime.value = ''
  fetchData()
}

function handleView(record: Lead) { message.info(`查看线索: ${record.name}`) }
function handleEdit(record: Lead) {
  isEdit.value = true
  Object.assign(formState, record)
  modalVisible.value = true
}
function handleAdd() {
  isEdit.value = false
  Object.assign(formState, { id: 0, name: '', companyName: '', contactName: '', phone: '', mobile: '', email: '', source: 'website', score: 50, remark: '' })
  modalVisible.value = true
}

function handleResetFilters() {
  for (const key of Object.keys(searchFilters)) {
    searchFilters[key] = undefined
  }
  pagination.current = 1
  fetchData()
}

function handleActionMenuClick(key: string, record: Lead) {
  switch (key) {
    case 'follow':
      message.info(`添加跟进: ${record.name}`)
      break
    case 'history':
      message.info(`跟进记录: ${record.name}`)
      break
    case 'delete':
      Modal.confirm({
        title: '确认删除',
        content: `确定要删除线索"${record.name}"吗？`,
        onOk: async () => {
          try { await leadApi.delete(record.id); message.success('删除成功'); fetchData() }
          catch (err) { console.warn('[CRM线索] 删除线索失败', err); message.error('删除失败') }
        }
      })
      break
  }
}

async function handleDelete(record: Lead) {
  try { await leadApi.delete(record.id); message.success('删除成功'); fetchData() }
  catch (err) { console.warn('[CRM线索] 删除线索失败', err); message.error('删除失败') }
}

async function handleAssign(record: Lead) {
  currentLead.value = record
  assignForm.userId = undefined
  assignForm.remark = ''
  assignVisible.value = true
}

async function handleConvert(record: Lead) {
  currentLead.value = record
  convertVisible.value = true
}

function handleBatchAssign() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要分配的线索')
    return
  }
  assignForm.userId = undefined
  assignForm.remark = ''
  assignVisible.value = true
}

function handleBatchConvert() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要转化的线索')
    return
  }
  Modal.confirm({
    title: '批量转化确认',
    content: `确定要转化选中的 ${selectedRowKeys.value.length} 个线索吗？`,
    onOk: async () => {
      message.success(`成功转化 ${selectedRowKeys.value.length} 个线索`)
      selectedRowKeys.value = []
      fetchData()
    }
  })
}

function handleImport() { message.info('导入线索') }

function handleExport() {
  const headers = ['线索名称', '公司', '联系人', '电话', '邮箱', '来源', '评分', '状态', '添加时间']
  const rows = dataSource.value.map((row: any) => [
    row.name, row.companyName, row.contactName, row.phone, row.email,
    sourceTextMap[row.source] || row.source, row.score, getStatusText(row.status), row.createdAt
  ])
  exportCsv(headers, rows, '线索')
}

function handleSearch(keyword: string) {
  searchFilters.keyword = keyword || undefined
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  fetchData()
}

function handleSelectionChange(rows: any[], ids: any[]) {
  selectedRowKeys.value = ids
}

// 弹窗相关
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const modalTitle = computed(() => isEdit.value ? '编辑线索' : '新建线索')
const formRef = ref<FormInstance>()
const formState = reactive({
  id: 0,
  name: '',
  companyName: '',
  contactName: '',
  phone: '',
  mobile: '',
  email: '',
  source: 'website',
  score: 50,
  remark: ''
})
const formRules = {
  name: [{ required: true, message: '请输入线索名称', trigger: 'blur' }],
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }]
}

async function handleModalOk() {
  try { await formRef.value?.validate() } catch (err) { console.warn('[CRM线索] 表单验证失败', err); return }
  modalLoading.value = true
  try {
    if (isEdit.value) {
      await leadApi.update(formState.id, formState)
      message.success('更新成功')
    } else {
      await leadApi.create(formState)
      message.success('创建成功')
    }
    modalVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[CRM线索] 保存线索失败', err)
    message.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    modalLoading.value = false
  }
}

// 分配弹窗
const assignVisible = ref(false)
const assignLoading = ref(false)
const currentLead = ref<Lead | null>(null)
const assignForm = reactive({ userId: undefined as number | undefined, remark: '' })
const salesUsers = ref([
  { id: 1, name: '张三' },
  { id: 2, name: '李四' },
  { id: 3, name: '王五' }
])

async function handleAssignConfirm() {
  if (!assignForm.userId) {
    message.warning('请选择销售人员')
    return
  }
  assignLoading.value = true
  try {
    const userId = assignForm.userId
    message.success('分配成功')
    assignVisible.value = false
    selectedRowKeys.value = []
    fetchData()
  } catch (err) {
    console.warn('[CRM线索] 分配线索失败', err)
    message.error('分配失败')
  } finally {
    assignLoading.value = false
  }
}

// 转化弹窗
const convertVisible = ref(false)
const convertLoading = ref(false)

async function handleConvertConfirm() {
  if (!currentLead.value) return
  convertLoading.value = true
  try {
    await leadApi.convertToCustomer(currentLead.value.id)
    message.success('转化成功')
    convertVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[CRM线索] 转化线索失败', err)
    message.error('转化失败')
  } finally {
    convertLoading.value = false
  }
}

onMounted(() => {
  fetchData()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})
defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.lead-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.lead-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.lead-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.lead-page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.update-time {
  font-size: 12px;
  color: #999;
}
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

.stats-cards {
  flex-shrink: 0;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.stat-card.stat-card-blue {
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
  border: 1px solid #91d5ff;
}

.stat-card.stat-card-green {
  background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
  border: 1px solid #b7eb8f;
}

.stat-card.stat-card-orange {
  background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%);
  border: 1px solid #ffd591;
}

.stat-card.stat-card-purple {
  background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%);
  border: 1px solid #d3adf7;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  margin-right: 16px;
}

.stat-content {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
}

.stat-desc {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.lead-name {
  font-weight: 500;
}

.company-name {
  font-weight: 500;
}

.contact-name {
  font-size: 12px;
  color: #999;
}

.mobile-text {
  font-size: 12px;
  color: #999;
}

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


.action-more-btn {
  padding: 0 4px;
}
</style>
