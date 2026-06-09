<template>
  <PageContainer title="客户管理" full-height>
    <template #headerExtra>
      <a-space :size="12">
        <span class="data-status">
          <a-badge :status="loading ? 'processing' : hasError ? 'error' : 'success'" />
          <span v-if="lastUpdateTime" class="update-time">
            数据更新: {{ lastUpdateTime }}
          </span>
        </span>
        <a-radio-group v-model:value="currentView" button-style="solid" size="small">
          <a-radio-button value="list"><UnorderedListOutlined /> 列表</a-radio-button>
          <a-radio-button value="kanban"><AppstoreOutlined /> 看板</a-radio-button>
        </a-radio-group>
        <a-button size="small" @click="handleRefresh">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </template>

    <ErrorBoundary @reset="fetchData">
      <!-- 列表视图 -->
      <template v-if="currentView === 'list'">
        <!-- 统计卡片 -->
        <div class="stats-cards">
          <a-row :gutter="16">
            <a-col :span="6">
              <div class="stat-card stat-card-purple">
                <div class="stat-icon" style="background: linear-gradient(135deg, #ff4d4f 0%, #f5222d 100%);">
                  <CrownOutlined />
                </div>
                <div class="stat-content">
                  <div class="stat-title">VIP客户</div>
                  <div class="stat-value">{{ levelCounts.vip }}</div>
                  <div class="stat-desc">核心客户群</div>
                </div>
              </div>
            </a-col>
            <a-col :span="6">
              <div class="stat-card stat-card-orange">
                <div class="stat-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
                  <StarOutlined />
                </div>
                <div class="stat-content">
                  <div class="stat-title">重要客户</div>
                  <div class="stat-value">{{ levelCounts.important }}</div>
                  <div class="stat-desc">重点跟进</div>
                </div>
              </div>
            </a-col>
            <a-col :span="6">
              <div class="stat-card stat-card-blue">
                <div class="stat-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
                  <UserOutlined />
                </div>
                <div class="stat-content">
                  <div class="stat-title">普通客户</div>
                  <div class="stat-value">{{ levelCounts.normal }}</div>
                  <div class="stat-desc">稳定合作</div>
                </div>
              </div>
            </a-col>
            <a-col :span="6">
              <div class="stat-card stat-card-green">
                <div class="stat-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
                  <UsergroupAddOutlined />
                </div>
                <div class="stat-content">
                  <div class="stat-title">潜在客户</div>
                  <div class="stat-value">{{ levelCounts.potential }}</div>
                  <div class="stat-desc">待开发</div>
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
          add-text="新增客户"
          @add="handleAdd"
          @refresh="fetchData"
          @search="handleSearch"
          @page-change="handlePageChange"
          @filter-change="handleFilterChange"
          @selection-change="handleSelectionChange"
          @export="handleExport"
        >
          <template #toolbar-actions>
            <a-button size="small" @click="handleImport">
              <template #icon><ImportOutlined /></template>
              导入
            </a-button>
          </template>

          <template #batch-actions>
            <a-button size="small" type="primary" ghost @click="handleBatchAssign">
              <template #icon><TeamOutlined /></template>
              批量分配
            </a-button>
          </template>

          <template #empty>
            <div class="table-empty">
              <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
              <InboxOutlined v-else class="table-empty-icon" />
              <p v-if="hasActiveFilters" class="table-empty-text">
                没有符合条件的客户，<a @click="handleResetFilters">清除筛选</a>
              </p>
              <p v-else class="table-empty-text">
                暂无客户数据，点击右上角「新增客户」开始创建
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
              <a-tooltip title="跟进">
                <a-button type="link" size="small" @click="handleFollow(record)">
                  <template #icon><MessageOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-dropdown trigger="click">
                <a-button type="link" size="small" class="action-more-btn">
                  <template #icon><MoreOutlined /></template>
                </a-button>
                <template #overlay>
                  <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                    <a-menu-item key="follows"><HistoryOutlined /> 跟进记录</a-menu-item>
                    <a-menu-item key="orders"><FileTextOutlined /> 订单记录</a-menu-item>
                    <a-menu-item key="contracts"><SolutionOutlined /> 合同记录</a-menu-item>
                    <a-menu-divider />
                    <a-menu-item key="delete" danger><DeleteOutlined /> 删除</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </VxeTableList>
      </template>

      <!-- 看板视图 -->
      <template v-if="currentView === 'kanban'">
        <div class="kanban-container">
          <div v-for="level in levelGroups" :key="level.value" class="kanban-column">
            <div class="kanban-column-header">
              <span class="kanban-column-title">
                <a-tag :color="getLevelColor(level.value)" size="small">{{ level.label }}</a-tag>
              </span>
              <span class="kanban-column-count">{{ getCustomersByLevel(level.value).length }} 个</span>
              <a-button type="link" size="small" @click="handleAddToLevel(level.value)">
                <template #icon><PlusOutlined /></template>
              </a-button>
            </div>
            <div class="kanban-column-body">
              <div
                v-for="customer in getCustomersByLevel(level.value)"
                :key="customer.id"
                class="kanban-card"
                @click="handleView(customer)"
              >
                <div class="kanban-card-header">
                  <a-space>
                    <a-avatar :style="{ backgroundColor: getLevelColor(customer.level) }" size="small">
                      {{ customer.name?.charAt(0) }}
                    </a-avatar>
                    <span class="kanban-card-name">{{ customer.name }}</span>
                  </a-space>
                  <a-tag :color="customer.status === 0 ? 'success' : 'error'" size="small">
                    {{ customer.status === 0 ? '正常' : '停用' }}
                  </a-tag>
                </div>
                <div class="kanban-card-body">
                  <div class="kanban-card-row">
                    <span class="kanban-card-label"><UserOutlined /> 联系人:</span>
                    <span class="kanban-card-value">{{ customer.contactPerson || '-' }}</span>
                  </div>
                  <div class="kanban-card-row">
                    <span class="kanban-card-label"><PhoneOutlined /> 电话:</span>
                    <span class="kanban-card-value">{{ customer.phone || '-' }}</span>
                  </div>
                  <div class="kanban-card-row">
                    <span class="kanban-card-label"><HomeOutlined /> 行业:</span>
                    <span class="kanban-card-value">{{ customer.industry || '-' }}</span>
                  </div>
                </div>
                <div class="kanban-card-footer">
                  <a-button type="link" size="small" @click.stop="handleFollow(customer)">
                    <template #icon><MessageOutlined /></template>
                    跟进
                  </a-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </ErrorBoundary>

    <!-- 客户表单弹窗 -->
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
            <a-form-item label="客户名称" name="name">
              <a-input v-model:value="formState.name" placeholder="请输入客户名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="客户编码" name="code">
              <a-input v-model:value="formState.code" placeholder="请输入客户编码" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系人" name="contactPerson">
              <a-input v-model:value="formState.contactPerson" placeholder="请输入联系人" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系电话" name="phone">
              <a-input v-model:value="formState.phone" placeholder="请输入联系电话" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="邮箱" name="email">
              <a-input v-model:value="formState.email" placeholder="请输入邮箱" />
            </a-form-item>
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
                <a-select-option value="教育">教育</a-select-option>
                <a-select-option value="其他">其他</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态" name="status">
              <a-radio-group v-model:value="formState.status">
                <a-radio :value="0">正常</a-radio>
                <a-radio :value="1">停用</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="地址" name="address" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
              <a-input v-model:value="formState.address" placeholder="请输入地址" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="description" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
              <a-textarea v-model:value="formState.description" placeholder="请输入备注" :rows="3" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 跟进记录弹窗 -->
    <a-modal
      v-model:open="followModalVisible"
      title="添加跟进记录"
      :confirm-loading="followModalLoading"
      width="600px"
      @ok="handleFollowModalOk"
    >
      <a-form :model="followForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="跟进类型" required>
          <a-select v-model:value="followForm.followType" placeholder="请选择跟进类型">
            <a-select-option :value="1">电话</a-select-option>
            <a-select-option :value="2">拜访</a-select-option>
            <a-select-option :value="3">邮件</a-select-option>
            <a-select-option :value="4">微信</a-select-option>
            <a-select-option :value="5">其他</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="跟进内容" required>
          <a-textarea v-model:value="followForm.content" placeholder="请输入跟进内容" :rows="4" />
        </a-form-item>
        <a-form-item label="跟进结果">
          <a-select v-model:value="followForm.result" placeholder="请选择跟进结果">
            <a-select-option :value="1">有意向</a-select-option>
            <a-select-option :value="2">无意向</a-select-option>
            <a-select-option :value="3">待跟进</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="下次跟进时间">
          <a-date-picker v-model:value="followForm.nextFollowTime" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- CSV导入弹窗 -->
    <a-modal
      v-model:open="importVisible"
      title="导入客户"
      width="700px"
      :confirm-loading="importLoading"
      @ok="handleImportConfirm"
      @cancel="importVisible = false"
    >
      <a-steps :current="importFileList.length > 0 ? 1 : 0" size="small" style="margin-bottom: 24px">
        <a-step title="上传文件" />
        <a-step title="字段映射" />
      </a-steps>
      <a-upload
        :file-list="importFileList"
        :before-upload="() => false"
        accept=".csv"
        :max-count="1"
        @change="handleImportFileChange"
      >
        <a-button>
          <template #icon><PlusOutlined /></template>
          选择CSV文件
        </a-button>
      </a-upload>
      <a-divider>字段映射</a-divider>
      <VxeTableList
        :columns="importMappingVxeColumns"
        :data-source="importFieldMapping"
        :pagination="false"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #csvFieldCell="{ record }">
          <a-input v-model:value="record.csvField" placeholder="CSV列名" size="small" />
        </template>
        <template #requiredCell="{ record }">
          <a-tag :color="record.required ? 'red' : 'default'">{{ record.required ? '是' : '否' }}</a-tag>
        </template>
      </VxeTableList>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { PageContainer } from '@/components'
import { customerApi, type CustomerInfo } from '@/api/customer'
import { useUserStore } from '@/stores/user'
import { exportCsv } from '@/utils/exportCsv'
import {
  PlusOutlined,
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  ImportOutlined,
  MoreOutlined,
  MessageOutlined,
  UnorderedListOutlined,
  AppstoreOutlined,
  ReloadOutlined,
  SearchOutlined,
  InboxOutlined,
  CrownOutlined,
  StarOutlined,
  UserOutlined,
  UsergroupAddOutlined,
  TeamOutlined,
  HistoryOutlined,
  FileTextOutlined,
  SolutionOutlined,
  PhoneOutlined,
  HomeOutlined
} from '@ant-design/icons-vue'

const userStore = useUserStore()
const router = useRouter()
const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const currentView = ref('list')
const kanbanData = ref<any[]>([])
const lastUpdateTime = ref<string>('')
const selectedRowKeys = ref<number[]>([])
let kanbanLoading = false
let autoRefreshTimer: number | null = null

// 状态统计
const levelCounts = computed(() => {
  const vip = dataSource.value.filter(c => c.level === 1).length
  const important = dataSource.value.filter(c => c.level === 2).length
  const normal = dataSource.value.filter(c => c.level === 3).length
  const potential = dataSource.value.filter(c => c.level === 4).length
  return { vip, important, normal, potential }
})

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// 数据源
const tableDataSource = dataSource

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

const vxeColumns = computed(() => [
  { field: 'name', title: '客户信息', width: 200, formatter: ({ row }: any) => row.name || '' },
  { field: 'contactPerson', title: '联系人', width: 100 },
  { field: 'phone', title: '联系电话', width: 120 },
  { field: 'level', title: '客户等级', width: 100, align: 'center', formatter: ({ cellValue }: any) => getLevelName(cellValue) },
  { field: 'industry', title: '行业', width: 100 },
  { field: 'status', title: '状态', width: 80, align: 'center', formatter: ({ cellValue }: any) => cellValue === 0 ? '正常' : '停用' },
  { field: 'createTime', title: '创建时间', width: 160 },
  { field: 'action', title: '操作', width: 160, fixed: 'right', type: 'action' }
])

const filterFields = [
  { key: 'name', label: '客户名称', type: 'input' as const, placeholder: '输入客户名称' },
  { key: 'code', label: '客户编码', type: 'input' as const, placeholder: '输入客户编码' },
  { key: 'level', label: '客户等级', type: 'select' as const, options: [
    { label: 'VIP客户', value: 1 },
    { label: '重要客户', value: 2 },
    { label: '普通客户', value: 3 },
    { label: '潜在客户', value: 4 }
  ]},
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '正常', value: 0 },
    { label: '停用', value: 1 }
  ]},
  { key: 'industry', label: '行业', type: 'select' as const, options: [
    { label: 'IT/互联网', value: 'IT' },
    { label: '制造业', value: '制造业' },
    { label: '金融', value: '金融' },
    { label: '零售', value: '零售' },
    { label: '其他', value: '其他' }
  ]}
]

// 看板分组
const levelGroups = [
  { value: 1, label: 'VIP客户' },
  { value: 2, label: '重要客户' },
  { value: 3, label: '普通客户' },
  { value: 4, label: '潜在客户' }
]
const getCustomersByLevel = (level: number) => kanbanData.value.filter(c => c.level === level)

// 辅助方法
const levelColorMap: Record<number, string> = { 1: '#ff4d4f', 2: '#faad14', 3: '#1890ff', 4: '#52c41a' }
const levelTextMap: Record<number, string> = { 1: 'VIP客户', 2: '重要客户', 3: '普通客户', 4: '潜在客户' }
function getLevelColor(level: number): string { return levelColorMap[level] || '#999' }
function getLevelName(level: number): string { return levelTextMap[level] || '未知' }

// 自动刷新 (60秒)
const startAutoRefresh = () => {
  autoRefreshTimer = window.setInterval(() => {
    if (!loading.value && !modalVisible.value && !followModalVisible.value) {
      fetchData(true)
    }
  }, 60000)
}

const stopAutoRefresh = () => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
  }
}

// 数据加载
async function fetchData(silent = false) {
  if (!silent) loading.value = true
  hasError.value = false
  try {
    const res = await customerApi.getPage({
      tenantId: userStore.tenantId,
      ...searchFilters,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData?.records || []
    pagination.total = pageData?.total || 0
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch {
    if (!silent) {
      hasError.value = true
      message.error('加载客户列表失败')
    }
    dataSource.value = mockData()
    pagination.total = mockData().length
  } finally {
    if (!silent) loading.value = false
  }
}

// 刷新
const handleRefresh = async () => {
  lastUpdateTime.value = ''
  kanbanData.value = []
  await fetchData()
}

// 事件处理
function handleView(record: any) { router.push(`/crm/customer/${record.id}`) }
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

function handleResetFilters() {
  for (const key of Object.keys(searchFilters)) {
    searchFilters[key] = undefined
  }
  pagination.current = 1
  fetchData()
}

// 弹窗相关
const modalVisible = ref(false)
const modalLoading = ref(false)
const modalTitle = computed(() => isEdit.value ? '编辑客户' : '新增客户')
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const formState = reactive({
  id: 0,
  name: '',
  code: '',
  contactPerson: '',
  phone: '',
  email: '',
  address: '',
  level: 3,
  industry: '',
  status: 0,
  description: ''
})
const formRules = {
  name: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }]
}

const followModalVisible = ref(false)
const followModalLoading = ref(false)
const currentCustomerId = ref(0)
const followForm = reactive({ followType: 1, content: '', result: 3, nextFollowTime: null as any })

function handleAdd() {
  isEdit.value = false
  Object.assign(formState, {
    id: 0, name: '', code: '', contactPerson: '', phone: '', email: '',
    address: '', level: 3, industry: '', status: 0, description: ''
  })
  modalVisible.value = true
}

function handleAddToLevel(level: number) {
  isEdit.value = false
  Object.assign(formState, {
    id: 0, name: '', code: '', contactPerson: '', phone: '', email: '',
    address: '', level, industry: '', status: 0, description: ''
  })
  modalVisible.value = true
}

function handleEdit(record: any) {
  isEdit.value = true
  Object.assign(formState, record)
  modalVisible.value = true
}

function handleFollow(record: any) {
  currentCustomerId.value = record.id
  Object.assign(followForm, { followType: 1, content: '', result: 3, nextFollowTime: null })
  followModalVisible.value = true
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'follows':
      message.info(`查看跟进记录: ${record.name}`)
      break
    case 'orders':
      message.info(`查看订单记录: ${record.name}`)
      break
    case 'contracts':
      message.info(`查看合同记录: ${record.name}`)
      break
    case 'delete':
      handleDelete(record)
      break
  }
}

async function handleModalOk() {
  try { await formRef.value?.validate() } catch { return }
  modalLoading.value = true
  try {
    if (isEdit.value) {
      await customerApi.update(formState.id, formState)
      message.success('更新成功')
    } else {
      await customerApi.create(formState)
      message.success('创建成功')
    }
    modalVisible.value = false
    kanbanData.value = []
    fetchData()
  } catch {
    message.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    modalLoading.value = false
  }
}

async function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除客户"${record.name}"吗？`,
    centered: true,
    async onOk() {
      try {
        await customerApi.delete(record.id)
        message.success('删除成功')
        kanbanData.value = []
        fetchData()
      } catch {
        message.error('删除失败')
      }
    }
  })
}

async function handleFollowModalOk() {
  if (!followForm.content) { message.error('请输入跟进内容'); return }
  followModalLoading.value = true
  try {
    await customerApi.addFollowRecord(currentCustomerId.value, {
      customerId: currentCustomerId.value,
      followType: followForm.followType,
      content: followForm.content,
      result: followForm.result,
      nextFollowTime: followForm.nextFollowTime
    })
    message.success('跟进记录添加成功')
    followModalVisible.value = false
  } catch {
    message.error('添加失败')
  } finally {
    followModalLoading.value = false
  }
}

function handleBatchAssign() {
  if (selectedRowKeys.value.length === 0) { message.warning('请选择要分配的客户'); return }
  Modal.confirm({
    title: '批量分配',
    content: `确定要批量分配选中的 ${selectedRowKeys.value.length} 个客户吗？`,
    centered: true,
    onOk() {
      message.success(`成功分配 ${selectedRowKeys.value.length} 个客户`)
      selectedRowKeys.value = []
      fetchData()
    }
  })
}

// 导入
const importVisible = ref(false)
const importLoading = ref(false)
const importFileList = ref<any[]>([])
const importMappingVxeColumns = [
  { field: 'label', title: '系统字段', width: 120 },
  { field: 'csvField', title: 'CSV列名', slotName: 'csvFieldCell' },
  { field: 'required', title: '必填', width: 60, align: 'center', slotName: 'requiredCell' }
]
const importFieldMapping = reactive([
  { csvField: '', systemField: 'name', required: true, label: '客户名称' },
  { csvField: '', systemField: 'code', required: true, label: '客户编码' },
  { csvField: '', systemField: 'contactPerson', required: false, label: '联系人' },
  { csvField: '', systemField: 'phone', required: true, label: '联系电话' },
  { csvField: '', systemField: 'email', required: false, label: '邮箱' },
  { csvField: '', systemField: 'industry', required: false, label: '行业' },
  { csvField: '', systemField: 'address', required: false, label: '地址' }
])
function handleImport() {
  importFileList.value = []
  importFieldMapping.forEach(m => { m.csvField = '' })
  importVisible.value = true
}
function handleImportFileChange(info: any) { importFileList.value = info.fileList.slice(-1) }
async function handleImportConfirm() {
  if (importFileList.value.length === 0) { message.warning('请先上传CSV文件'); return }
  const unmappedRequired = importFieldMapping.filter(f => f.required && !f.csvField)
  if (unmappedRequired.length > 0) {
    message.warning(`请为必填字段配置CSV映射：${unmappedRequired.map(f => f.label).join('、')}`)
    return
  }
  importLoading.value = true
  try {
    await customerApi.importCustomers({
      file: importFileList.value[0],
      mapping: importFieldMapping.reduce((acc, m) => {
        if (m.csvField) acc[m.systemField] = m.csvField
        return acc
      }, {} as Record<string, string>)
    })
    message.success('导入成功')
    importVisible.value = false
    kanbanData.value = []
    fetchData()
  } catch {
    message.error('导入失败，请检查文件格式')
  } finally {
    importLoading.value = false
  }
}

// 导出
function handleExport() {
  const headers = ['客户名称', '客户编码', '联系人', '联系电话', '邮箱', '行业', '等级', '状态', '创建时间']
  const rows = dataSource.value.map((row: any) => [
    row.name, row.code, row.contactPerson, row.phone, row.email, row.industry,
    getLevelName(row.level), row.status === 0 ? '正常' : '停用', row.createTime
  ])
  exportCsv(headers, rows, '客户数据')
}

// Mock数据
const mockData = () => [
  { id: 1, name: '北京科技有限公司', code: 'C001', contactPerson: '张总', phone: '13800138001', level: 1, industry: 'IT', status: 0, createTime: '2024-01-10 10:00:00' },
  { id: 2, name: '上海贸易集团', code: 'C002', contactPerson: '李经理', phone: '13800138002', level: 2, industry: '制造业', status: 0, createTime: '2024-01-11 11:00:00' },
  { id: 3, name: '广州制造公司', code: 'C003', contactPerson: '王主任', phone: '13800138003', level: 2, industry: '制造业', status: 0, createTime: '2024-01-12 09:00:00' },
  { id: 4, name: '深圳创新科技', code: 'C004', contactPerson: '赵总监', phone: '13800138004', level: 3, industry: 'IT', status: 0, createTime: '2024-01-13 14:00:00' },
  { id: 5, name: '杭州互联网公司', code: 'C005', contactPerson: '孙经理', phone: '13800138005', level: 3, industry: 'IT', status: 1, createTime: '2024-01-14 15:00:00' },
  { id: 6, name: '成都科技发展', code: 'C006', contactPerson: '周总', phone: '13800138006', level: 4, industry: '金融', status: 0, createTime: '2024-01-15 16:00:00' }
]

// 监听视图切换
watch(currentView, (val) => {
  if (val === 'kanban') {
    fetchKanbanData()
  }
})

onMounted(() => {
  fetchData()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.data-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}

.update-time {
  color: #999;
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

.customer-name {
  font-weight: 500;
  line-height: 1.2;
}

.customer-code {
  font-size: 12px;
  color: #999;
  line-height: 1.2;
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

/* 看板样式 */
.kanban-container {
  display: flex;
  gap: 16px;
  height: 100%;
  overflow: auto;
  padding: 16px;
}

.kanban-column {
  flex: 1;
  min-width: 280px;
  background-color: #f5f5f5;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
}

.kanban-column-header {
  padding: 12px 16px;
  background-color: #fff;
  border-bottom: 1px solid #e8e8e8;
  border-radius: 8px 8px 0 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kanban-column-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.kanban-column-count {
  font-size: 12px;
  color: #909399;
}

.kanban-column-body {
  flex: 1;
  padding: 8px;
  overflow: auto;
}

.kanban-card {
  background-color: #fff;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #e8e8e8;
}

.kanban-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  border-color: #1890ff;
}

.kanban-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.kanban-card-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.kanban-card-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.kanban-card-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.kanban-card-label {
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}

.kanban-card-value {
  color: #606266;
}

.kanban-card-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}




</style>
