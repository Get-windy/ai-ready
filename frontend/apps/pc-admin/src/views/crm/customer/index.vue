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
        <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
          <SyncOutlined /> {{ autoRefreshCountdown }}s
        </span>
        <a-radio-group v-model:value="currentView" button-style="solid" size="small">
          <a-radio-button value="list"><UnorderedListOutlined /> 列表</a-radio-button>
          <a-radio-button value="kanban"><AppstoreOutlined /> 看板</a-radio-button>
        </a-radio-group>
        <a-button size="small" :loading="refreshLoading" v-permission="'crm:customer:refresh'" @click="debounceClick('refresh', handleRefresh)">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
<span class="shortcut-hints">
                                              <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
                                              <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
                                            </span>
      </a-space>
    </template>

    <ErrorBoundary @reset="fetchData">
      <!-- 骨架加载 -->
      <div v-if="loading && dataSource.length === 0" class="skeleton-loading">
        <a-skeleton :paragraph="{ rows: 3 }" active />
        <div style="height: 16px" />
        <a-skeleton :paragraph="{ rows: 8 }" active />
      </div>

      <!-- 列表视图 -->
      <template v-if="!(loading && dataSource.length === 0)">
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
          :min-empty-rows="12"
          add-text="新增客户"
          @add="handleAdd"
          @refresh="fetchData"
          @search="handleSearch"
          @page-change="handlePageChange"
          @filter-change="handleFilterChange"
          @selection-change="handleSelectionChange"
          @cell-dblclick="handleView"
          @export="handleExport"
        >
          <template #toolbar-actions>
            <a-button size="small" v-permission="'crm:customer:import'" @click="handleImport">
              <template #icon><ImportOutlined /></template>
              导入
            </a-button>
          </template>

          <template #batch-actions>
            <a-button size="small" type="primary" ghost v-permission="'crm:customer:batchassign'" @click="handleBatchAssign">
              <template #icon><TeamOutlined /></template>
              批量分配
            </a-button>
          </template>

          <template #empty>
            <div class="table-empty">
              <template v-if="hasError">
                <WarningOutlined class="table-empty-icon" style="color: #faad14" />
                <p class="table-empty-text">数据加载失败，请重试</p>
                <a-button type="primary" size="small" @click="fetchData as any">
                  <template #icon><ReloadOutlined /></template>
                  重试
                </a-button>
              </template>
              <template v-else>
                <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
                <InboxOutlined v-else class="table-empty-icon" />
                <p v-if="hasActiveFilters" class="table-empty-text">
                  没有符合条件的客户，<a @click="handleResetFilters">清除筛选</a>
                </p>
                <p v-else class="table-empty-text">
                  暂无客户数据
                </p>
                <div v-if="!hasActiveFilters" class="empty-state-wrapper">
                  <a-button type="primary" v-permission="'crm:customer:create'" @click="handleAdd">
                    <template #icon><PlusOutlined /></template>
                    新增第一个客户
                  </a-button>
                </div>
              </template>
            </div>
          </template>

          <template #action="{ record }">
            <a-space :size="4">
              <a-tooltip title="查看详情">
                <a-button type="link" size="small" v-permission="'crm:customer:view'" @click="handleView(record)">
                  <template #icon><EyeOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip title="编辑">
                <a-button type="link" size="small" v-permission="'crm:customer:edit'" @click="handleEdit(record)">
                  <template #icon><EditOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip title="跟进">
                <a-button type="link" size="small" v-permission="'crm:customer:follow'" @click="handleFollow(record)">
                  <template #icon><MessageOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-dropdown trigger="click">
                <a-button type="link" size="small" class="action-more-btn">
                  <template #icon><MoreOutlined /></template>
                </a-button>
                <template #overlay>
                  <a-menu @click="({ key }) => handleActionMenuClick(key as string, record)">
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
              <a-button type="link" size="small" v-permission="'crm:customer:addtolevel'" @click="handleAddToLevel(level.value)">
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
      </template>
    </ErrorBoundary>

    <!-- 全屏详情抽屉（新建/编辑客户） -->
    <FullScreenDetail
      :visible="modalVisible"
      :title="modalTitle"
      :save-loading="modalLoading"
      :show-save-and-new="!isEdit"
      :dirty="formDirty"
      @close="handleFormClose"
      @save="handleModalOk"
      @save-and-new="handleFormSaveAndNew"
    >
      <a-form ref="formRef" :model="formState" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="客户名称" name="name">
              <a-input v-model:value="formState.name" placeholder="请输入客户名称" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="客户编码" name="code">
              <a-input v-model:value="formState.code" placeholder="请输入客户编码" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系人" name="contactPerson">
              <a-input v-model:value="formState.contactPerson" placeholder="请输入联系人" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系电话" name="phone">
              <a-input v-model:value="formState.phone" placeholder="请输入联系电话" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="邮箱" name="email">
              <a-input v-model:value="formState.email" placeholder="请输入邮箱" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="客户等级" name="level">
              <a-select v-model:value="formState.level" placeholder="请选择等级" size="small">
                <a-select-option :value="1">VIP客户</a-select-option>
                <a-select-option :value="2">重要客户</a-select-option>
                <a-select-option :value="3">普通客户</a-select-option>
                <a-select-option :value="4">潜在客户</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="行业" name="industry">
              <a-select v-model:value="formState.industry" placeholder="请选择行业" size="small">
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
              <a-input v-model:value="formState.address" placeholder="请输入地址" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="description" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
              <a-textarea v-model:value="formState.description" placeholder="请输入备注" :rows="3" size="small" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FullScreenDetail>

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
          <a-select v-model:value="followForm.followType" placeholder="请选择跟进类型" size="small">
            <a-select-option :value="1">电话</a-select-option>
            <a-select-option :value="2">拜访</a-select-option>
            <a-select-option :value="3">邮件</a-select-option>
            <a-select-option :value="4">微信</a-select-option>
            <a-select-option :value="5">其他</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="跟进内容" required>
          <a-textarea v-model:value="followForm.content" placeholder="请输入跟进内容" :rows="4" size="small" />
        </a-form-item>
        <a-form-item label="跟进结果">
          <a-select v-model:value="followForm.result" placeholder="请选择跟进结果" size="small">
            <a-select-option :value="1">有意向</a-select-option>
            <a-select-option :value="2">无意向</a-select-option>
            <a-select-option :value="3">待跟进</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="下次跟进时间">
          <a-date-picker v-model:value="followForm.nextFollowTime" style="width: 100%" size="small" />
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
        :pagination="false as any"
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
          <template #statusCell="{ record }">
            <a-tag :color="record.status === 0 ? 'success' : 'error'">{{ record.status === 0 ? '正常' : '停用' }}</a-tag>
          </template>
      </VxeTableList>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import { customerApi, type CustomerInfo } from '@/api/customer'

// ── 防抖工具 ──────────────────────────────────────────
function handleError(err: any) { console.warn('[CRM客户]', err) }

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}
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
  HomeOutlined,
  WarningOutlined,
  SyncOutlined
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
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

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
  } catch (err) {
    console.warn('[CRM客户] 加载看板数据失败', err)
    kanbanData.value = []
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
  { field: 'status', title: '状态', width: 80, align: 'center', slotName: 'statusCell' },
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

// 自动刷新 (30秒)
const startAutoRefresh = () => {
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    if (!loading.value && !modalVisible.value && !followModalVisible.value) {
      fetchData()
    }
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
}

const stopAutoRefresh = () => {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
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
  } catch (err) {
    if (!silent) {
      hasError.value = true
      message.error('加载客户列表失败')
    }
    console.warn('[CRM客户] 加载客户列表失败', err)
    dataSource.value = []
    pagination.total = 0
  } finally {
    if (!silent) loading.value = false
    refreshLoading.value = false
  }
}

// 刷新
const handleRefresh = async () => {
  lastUpdateTime.value = ''
  kanbanData.value = []
  refreshLoading.value = true
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
  name: [{ required: true, message: '请输入客户名称', trigger: 'blur', type: 'string' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur', type: 'string' }]
} as any

// ── 表单脏状态跟踪 ──────────────────────────────────────
const initialFormSnapshot = ref<string>('')
function saveFormSnapshot() {
  initialFormSnapshot.value = JSON.stringify({ ...formState })
}
const formDirty = computed(() => {
  if (!modalVisible.value) return false
  return JSON.stringify({ ...formState }) !== initialFormSnapshot.value
})

// ── 路由离开守卫 ─────────────────────────────────────────
onBeforeRouteLeave((_to, _from, next) => {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认离开',
      content: '当前表单有未保存的内容，确定离开吗？',
      onOk: () => { next() },
      onCancel: () => { next(false) },
    })
  } else {
    next()
  }
})

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
  nextTick(() => saveFormSnapshot())
}

function handleAddToLevel(level: number) {
  isEdit.value = false
  Object.assign(formState, {
    id: 0, name: '', code: '', contactPerson: '', phone: '', email: '',
    address: '', level, industry: '', status: 0, description: ''
  })
  modalVisible.value = true
  nextTick(() => saveFormSnapshot())
}

function handleEdit(record: any) {
  isEdit.value = true
  Object.assign(formState, record)
  modalVisible.value = true
  nextTick(() => saveFormSnapshot())
}

function handleFormClose() {
  if (formRef.value && formDirty.value) {
    Modal.confirm({
      title: '确认关闭',
      content: '当前表单有未保存的内容，确定关闭吗？',
      onOk: () => { modalVisible.value = false }
    })
  } else {
    modalVisible.value = false
  }
}

async function handleFormSaveAndNew() {
  await handleModalOk()
  if (!modalLoading.value) {
    Object.assign(formState, {
      id: 0, name: '', code: '', contactPerson: '', phone: '', email: '',
      address: '', level: 3, industry: '', status: 0, description: ''
    })
    isEdit.value = false
    modalVisible.value = true
    nextTick(() => saveFormSnapshot())
  }
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
  try { await formRef.value?.validate() } catch (err) { console.warn('[CRM客户] 表单验证失败', err); return }
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
  } catch (err) {
    console.warn('[CRM客户] 保存客户失败', err)
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
      } catch (err) {
        console.warn('[CRM客户] 删除客户失败', err)
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
  } catch (err) {
    console.warn('[CRM客户] 添加跟进记录失败', err)
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
  } catch (err) {
    console.warn('[CRM客户] 导入客户失败', err)
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

// 监听视图切换
watch(currentView, (val) => {
  if (val === 'kanban') {
    fetchKanbanData()
  }
})

function handleParentCreate() { handleAdd() }

// ── 快捷键 ──────────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') {
    e.preventDefault()
    debounceClick('refresh', handleRefresh)
    return
  }
  if (e.ctrlKey && e.key === 'n') {
    e.preventDefault()
    debounceClick('add', handleAdd)
    return
  }
}

onMounted(() => {
  fetchData()
  startAutoRefresh()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('crm:create' as any, handleParentCreate as any)
  window.addEventListener('crm:refresh' as any, fetchData as any)
})

onUnmounted(() => {
  stopAutoRefresh()
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('crm:create' as any, handleParentCreate as any)
  window.removeEventListener('crm:refresh' as any, fetchData as any)
})
defineExpose({ handleQuery: fetchData })
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




/* 让 VxeTableList 填满剩余空间 */
.vxe-table-list-wrapper {
  flex: 1;
  min-height: 0;
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

/* ── 自动刷新倒计时徽章 ──────────────────── */
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

/* ── 空状态 wrapper ──────────────────────── */
.empty-state-wrapper {
  margin-top: 16px;
  text-align: center;
}

/* ── 骨架加载 ────────────────────────────── */
.skeleton-loading {
  padding: 24px;
  background: #fff;
  border-radius: 8px;
}

/* ── VxeTable 表头 2px 底部边框 ──────────── */
:deep(.vxe-table .vxe-header--row) {
  border-bottom: 2px solid #e8e8e8;
}

</style>
