<template>
  <PageContainer title="供应商管理" full-height>
    <template #headerExtra>
      <a-space :size="12">
        <span class="data-status">
          <a-badge :status="loading ? 'processing' : hasError ? 'error' : 'success'" />
          <span v-if="lastUpdateTime" class="update-time">
            数据更新: {{ lastUpdateTime }}
          </span>
        </span>
        <a-button size="small" @click="handleRefresh">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </template>

    <ErrorBoundary @reset="fetchData">
      <!-- 统计卡片 -->
      <div class="stats-cards">
        <a-row :gutter="16">
          <a-col :span="6">
            <div class="stat-card stat-card-blue">
              <div class="stat-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
                <TeamOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">供应商总数</div>
                <div class="stat-value">{{ pagination.total }}</div>
                <div class="stat-desc">全部供应商</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-green">
              <div class="stat-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
                <CheckCircleOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">合作中</div>
                <div class="stat-value">{{ statusCounts.active }}</div>
                <div class="stat-desc positive">正常合作</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-orange">
              <div class="stat-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
                <StarOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">A级供应商</div>
                <div class="stat-value">{{ levelCounts.a }}</div>
                <div class="stat-desc">优质供应商</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-purple">
              <div class="stat-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
                <ShoppingOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">采购金额</div>
                <div class="stat-value">¥{{ formatAmount(totalPurchaseAmount) }}</div>
                <div class="stat-desc">累计采购</div>
              </div>
            </div>
          </a-col>
        </a-row>
      </div>

      <a-tabs v-model:activeKey="activeTab" style="margin:0 24px">
        <a-tab-pane key="all" tab="全部供应商" />
        <a-tab-pane key="active" tab="合作中" />
        <a-tab-pane key="inactive" tab="暂停合作" />
      </a-tabs>

      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableDataSource"
        :loading="loading"
        :pagination="pagination"
        :filter-fields="filterFields"
        :show-summary="true"
        :summary-data="summaryData"
        :show-export="true"
        :selectable="true"
        add-text="新建供应商"
        @add="handleAdd"
        @refresh="fetchData"
        @search="handleSearch"
        @page-change="handlePageChange"
        @sort-change="handleSortChange"
        @filter-change="handleFilterChange"
        @selection-change="handleSelectionChange"
        @export="handleExport"
      >
      <template #toolbar-actions>
        </template>

        <template #empty>
          <div class="table-empty">
            <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
            <InboxOutlined v-else class="table-empty-icon" />
            <p v-if="hasActiveFilters" class="table-empty-text">
              没有符合条件的供应商，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无供应商数据，点击右上角「新建供应商」开始创建
            </p>
          </div>
        </template>

        <template #action="{ record }">
          <template v-if="record.__empty_row">
            <span class="empty-placeholder">&nbsp;</span>
          </template>
          <template v-else>
            <a-space :size="4">
              <a-tooltip title="查看"><a-button type="link" size="small" @click="handleView(record)"><template #icon><EyeOutlined /></template></a-button></a-tooltip>
              <a-tooltip title="编辑"><a-button type="link" size="small" @click="handleEdit(record)"><template #icon><EditOutlined /></template></a-button></a-tooltip>
              <a-tooltip title="产品"><a-button type="link" size="small" @click="handleProducts(record)"><template #icon><ShoppingOutlined /></template></a-button></a-tooltip>
              <a-tooltip title="评估"><a-button type="link" size="small" @click="handleEvaluate(record)"><template #icon><StarOutlined /></template></a-button></a-tooltip>
              <a-dropdown>
                <a-button type="link" size="small" @click.prevent><template #icon><MoreOutlined /></template></a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handlePortal(record)">供应商门户</a-menu-item>
                    <a-menu-item @click="handleContact(record)">联系记录</a-menu-item>
                    <a-menu-item @click="handleDelete(record)" v-if="record.cooperationStatus === 2">删除</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </template>
      </VxeTableList>
    </ErrorBoundary>

    <a-modal v-model:open="modalVisible" :title="modalTitle" width="800px" :confirm-loading="submitLoading" @ok="handleSubmit" @cancel="handleModalCancel">
      <a-form ref="formRef" :model="formData" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-row :gutter="24">
          <a-col :span="12"><a-form-item label="供应商名称" name="supplierName" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }"><a-input v-model:value="formData.supplierName" placeholder="请输入供应商名称" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="供应商编码" name="supplierCode" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }"><a-input v-model:value="formData.supplierCode" placeholder="自动生成" disabled /></a-form-item></a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12"><a-form-item label="供应商类型" name="supplierType" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
            <a-select v-model:value="formData.supplierType" placeholder="请选择供应商类型">
              <a-select-option :value="1">原材料供应商</a-select-option>
              <a-select-option :value="2">产品供应商</a-select-option>
              <a-select-option :value="3">服务供应商</a-select-option>
              <a-select-option :value="4">物流供应商</a-select-option>
            </a-select>
          </a-form-item></a-col>
          <a-col :span="12"><a-form-item label="供应商等级" name="supplierLevel" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
            <a-select v-model:value="formData.supplierLevel" placeholder="请选择供应商等级">
              <a-select-option value="A">A级供应商</a-select-option>
              <a-select-option value="B">B级供应商</a-select-option>
              <a-select-option value="C">C级供应商</a-select-option>
            </a-select>
          </a-form-item></a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12"><a-form-item label="联系人" name="contactPerson" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }"><a-input v-model:value="formData.contactPerson" placeholder="请输入联系人" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="联系电话" name="contactPhone" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }"><a-input v-model:value="formData.contactPhone" placeholder="请输入联系电话" /></a-form-item></a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12"><a-form-item label="电子邮箱" name="email" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }"><a-input v-model:value="formData.email" placeholder="请输入电子邮箱" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="合作状态" name="cooperationStatus" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
            <a-select v-model:value="formData.cooperationStatus" placeholder="请选择合作状态">
              <a-select-option :value="1">合作中</a-select-option>
              <a-select-option :value="2">暂停合作</a-select-option>
            </a-select>
          </a-form-item></a-col>
        </a-row>
        <a-form-item label="公司地址" name="address"><a-input v-model:value="formData.address" placeholder="请输入公司地址" /></a-form-item>
        <a-form-item label="银行信息" name="bankInfo"><a-input v-model:value="formData.bankInfo" placeholder="请输入银行账户信息" /></a-form-item>
        <a-form-item label="备注" name="remark"><a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="detailVisible" title="供应商详情" width="900px" :footer="null">
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="供应商名称">{{ supplierDetail.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="供应商编码">{{ supplierDetail.supplierCode }}</a-descriptions-item>
        <a-descriptions-item label="供应商类型">{{ supplierDetail.supplierTypeLabel }}</a-descriptions-item>
        <a-descriptions-item label="供应商等级"><a-tag :color="getLevelColor(supplierDetail.supplierLevel)">{{ supplierDetail.supplierLevel }}级</a-tag></a-descriptions-item>
        <a-descriptions-item label="联系人">{{ supplierDetail.contactPerson }}</a-descriptions-item>
        <a-descriptions-item label="联系电话">{{ supplierDetail.contactPhone }}</a-descriptions-item>
        <a-descriptions-item label="电子邮箱">{{ supplierDetail.email }}</a-descriptions-item>
        <a-descriptions-item label="合作状态"><a-tag :color="getStatusColor(supplierDetail.cooperationStatus)">{{ getStatusText(supplierDetail.cooperationStatus) }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="公司地址" :span="2">{{ supplierDetail.address }}</a-descriptions-item>
        <a-descriptions-item label="银行信息">{{ supplierDetail.bankInfo }}</a-descriptions-item>
      </a-descriptions>
      <a-divider>业务统计</a-divider>
      <a-row :gutter="16">
        <a-col :span="6"><a-statistic title="采购订单" :value="supplierDetail.orderCount" suffix="单" /></a-col>
        <a-col :span="6"><a-statistic title="采购金额" :value="supplierDetail.purchaseAmount" :precision="2" prefix="¥" /></a-col>
        <a-col :span="6"><a-statistic title="已付款" :value="supplierDetail.paidAmount" :precision="2" prefix="¥" /></a-col>
        <a-col :span="6"><a-statistic title="待付款" :value="supplierDetail.payableAmount" :precision="2" prefix="¥" :value-style="{ color: '#f5222d' }" /></a-col>
      </a-row>
      <a-divider>供应商评估</a-divider>
      <a-row :gutter="16">
        <a-col :span="6"><a-statistic title="质量评分" :value="supplierDetail.qualityScore" suffix="分" /></a-col>
        <a-col :span="6"><a-statistic title="交货评分" :value="supplierDetail.deliveryScore" suffix="分" /></a-col>
        <a-col :span="6"><a-statistic title="服务评分" :value="supplierDetail.serviceScore" suffix="分" /></a-col>
        <a-col :span="6"><a-statistic title="综合评分" :value="supplierDetail.totalScore" suffix="分" :value-style="{ color: '#1890ff' }" /></a-col>
      </a-row>
      <a-divider>最近采购订单</a-divider>
      <a-table :columns="orderColumns" :data-source="supplierDetail.recentOrders" :pagination="false" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'"><span class="amount">¥{{ formatAmount(record.amount) }}</span></template>
          <template v-if="column.key === 'status'"><a-tag :color="getOrderStatusColor(record.status)">{{ record.statusLabel }}</a-tag></template>
        </template>
      </a-table>
    </a-modal>

    <a-modal v-model:open="productsModalVisible" :title="productsModalTitle" width="900px" :footer="null">
      <a-table :columns="productsColumns" :data-source="productsData" :pagination="false" row-key="id" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'price'"><span class="amount">¥{{ formatAmount(record.price) }}</span></template>
          <template v-if="column.key === 'status'"><a-tag :color="record.status === '正常供应' ? 'green' : 'orange'">{{ record.status }}</a-tag></template>
        </template>
      </a-table>
    </a-modal>

    <a-modal v-model:open="evaluateModalVisible" title="供应商评估" width="600px" @ok="handleEvaluateSubmit">
      <a-form :model="evaluateForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="供应商名称"><a-input :value="evaluateForm.supplierName" disabled /></a-form-item>
        <a-form-item label="质量评分" required>
          <a-rate v-model:value="evaluateForm.qualityScore" :count="5" style="vertical-align:middle" />
          <span style="margin-left:8px;color:#999">{{ evaluateForm.qualityScore }} 分</span>
        </a-form-item>
        <a-form-item label="交货评分" required>
          <a-rate v-model:value="evaluateForm.deliveryScore" :count="5" style="vertical-align:middle" />
          <span style="margin-left:8px;color:#999">{{ evaluateForm.deliveryScore }} 分</span>
        </a-form-item>
        <a-form-item label="服务评分" required>
          <a-rate v-model:value="evaluateForm.serviceScore" :count="5" style="vertical-align:middle" />
          <span style="margin-left:8px;color:#999">{{ evaluateForm.serviceScore }} 分</span>
        </a-form-item>
        <a-form-item label="评价内容"><a-textarea v-model:value="evaluateForm.comment" placeholder="请输入评价内容" :rows="4" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="portalModalVisible" :title="`供应商门户 - ${portalInfo.supplierName}`" width="550px" :footer="null">
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item label="门户地址"><a-space><span>{{ portalInfo.portalUrl }}</span><a-button type="link" size="small" @click="handleCopyPortalUrl"><template #icon><CopyOutlined /></template>复制</a-button></a-space></a-descriptions-item>
        <a-descriptions-item label="登录账号">{{ portalInfo.account }}</a-descriptions-item>
        <a-descriptions-item label="登录密码">{{ portalInfo.password }}</a-descriptions-item>
      </a-descriptions>
      <a-divider />
      <a-space style="width:100%;justify-content:flex-end">
        <a-button @click="handleResetPortalPassword">重置密码</a-button>
        <a-button type="primary" @click="handleOpenPortal">打开门户</a-button>
      </a-space>
    </a-modal>

    <a-modal v-model:open="contactsModalVisible" :title="contactsModalTitle" width="700px" :footer="null">
      <a-table :columns="contactsColumns" :data-source="contactsData" :pagination="false" row-key="id" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'isPrimary'"><a-tag :color="record.isPrimary === '是' ? 'blue' : 'default'">{{ record.isPrimary }}</a-tag></template>
        </template>
      </a-table>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, EyeOutlined, EditOutlined, ShoppingOutlined, StarOutlined, MoreOutlined, CopyOutlined, ReloadOutlined, SearchOutlined, InboxOutlined, TeamOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { PageContainer } from '@/components'
import { supplierApi } from '@/api/supplier'
import type { FormInstance } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { exportCsv } from '@/utils/exportCsv'

const tableRef = ref()
const router = useRouter()
const loading = ref(false)
const hasError = ref(false)
const submitLoading = ref(false)
const modalVisible = ref(false)
const detailVisible = ref(false)
const modalTitle = ref('新建供应商')
const activeTab = ref('all')
const formRef = ref<FormInstance>()
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const tableData = ref<any[]>([])
const lastUpdateTime = ref<string>('')
let autoRefreshTimer: number | null = null

// 状态统计
const statusCounts = computed(() => {
  const active = tableData.value.filter(r => r.cooperationStatus === 1).length
  return { active }
})

const levelCounts = computed(() => {
  const a = tableData.value.filter(r => r.supplierLevel === 'A').length
  return { a }
})

const totalPurchaseAmount = computed(() => {
  return tableData.value.reduce((s, r) => s + (r.purchaseAmount || 0), 0)
})

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// 空行填充
const MIN_TABLE_ROWS = 20
const tableDataSource = computed(() => {
  const data = [...tableData.value]
  const emptyCount = Math.max(0, MIN_TABLE_ROWS - data.length)
  for (let i = 0; i < emptyCount; i++) {
    data.push({ __empty_row: true, id: `__empty_${i}` })
  }
  return data
})

const columns = [
  { title: '供应商名称', dataIndex: 'supplierName', key: 'supplierName', width: 180 },
  { title: '供应商编码', dataIndex: 'supplierCode', key: 'supplierCode', width: 120 },
  { title: '供应商类型', dataIndex: 'supplierTypeLabel', key: 'supplierTypeLabel', width: 120 },
  { title: '等级', dataIndex: 'supplierLevel', key: 'supplierLevel', width: 80, type: 'status' as const },
  { title: '联系人', dataIndex: 'contactPerson', key: 'contactPerson', width: 100 },
  { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 120 },
  { title: '状态', dataIndex: 'cooperationStatus', key: 'cooperationStatus', width: 100, type: 'status' as const },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const, type: 'action' as const }
]

const filterFields = [
  { key: 'keyword', label: '供应商名称', type: 'input' as const, placeholder: '输入供应商名称' },
  { key: 'supplierLevel', label: '供应商等级', type: 'select' as const, options: [
    { label: 'A级', value: 'A' }, { label: 'B级', value: 'B' }, { label: 'C级', value: 'C' }
  ]},
  { key: 'cooperationStatus', label: '合作状态', type: 'select' as const, options: [
    { label: '合作中', value: 1 }, { label: '暂停合作', value: 2 }
  ]}
]

const levelColorMap: Record<string, string> = { A: 'green', B: 'blue', C: 'orange' }
const statusColorMap: Record<number, string> = { 1: 'green', 2: 'orange' }
const statusTextMap: Record<number, string> = { 1: '合作中', 2: '暂停合作' }
const orderStatusColorMap: Record<string, string> = { pending: 'orange', confirmed: 'blue', received: 'green', completed: 'green' }

function getLevelColor(level: string): string { return levelColorMap[level] || 'default' }
function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }
function getOrderStatusColor(status: string): string { return orderStatusColorMap[status] || 'default' }
function formatAmount(amount: number): string { return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }

const supplierTypeMap: Record<number, string> = { 1: '原材料供应商', 2: '产品供应商', 3: '服务供应商', 4: '物流供应商' }

const summaryData = computed(() => {
  if (tableData.value.length === 0) return undefined
  return [{ label: '本页数量', value: tableData.value.length, type: 'default' as const }]
})

const orderColumns = [
  { title: '订单编号', dataIndex: 'orderNo', width: 150 },
  { title: '订单金额', key: 'amount', dataIndex: 'amount', width: 120 },
  { title: '下单日期', dataIndex: 'orderDate', width: 100 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100 }
]

const formData = reactive({ id: undefined, supplierName: '', supplierCode: '', supplierType: 1, supplierLevel: 'B', contactPerson: '', contactPhone: '', email: '', address: '', bankInfo: '', cooperationStatus: 1, remark: '' })
const formRules = { supplierName: [{ required: true, message: '请输入供应商名称' }], supplierType: [{ required: true, message: '请选择供应商类型' }], contactPerson: [{ required: true, message: '请输入联系人' }], contactPhone: [{ required: true, message: '请输入联系电话' }] }

const supplierDetail = ref<any>({})

const productsModalVisible = ref(false)
const productsModalTitle = ref('')
const productsData = ref<any[]>([])
const productsColumns = [
  { title: '产品编码', dataIndex: 'productCode', width: 120 }, { title: '产品名称', dataIndex: 'productName', width: 150 },
  { title: '规格型号', dataIndex: 'spec', width: 120 }, { title: '单价', dataIndex: 'price', width: 100 },
  { title: '单位', dataIndex: 'unit', width: 80 }, { title: '供应状态', key: 'status', dataIndex: 'status', width: 100 }
]

const evaluateModalVisible = ref(false)
const evaluateForm = reactive({ supplierId: 0, supplierName: '', qualityScore: 80, deliveryScore: 80, serviceScore: 80, comment: '' })

const portalModalVisible = ref(false)
const portalInfo = reactive({ supplierName: '', portalUrl: '', account: '', password: '' })

const contactsModalVisible = ref(false)
const contactsModalTitle = ref('')
const contactsData = ref<any[]>([])
const contactsColumns = [
  { title: '姓名', dataIndex: 'name', width: 100 }, { title: '职位', dataIndex: 'role', width: 120 },
  { title: '电话', dataIndex: 'phone', width: 120 }, { title: '邮箱', dataIndex: 'email', width: 180 },
  { title: '是否主要联系人', key: 'isPrimary', dataIndex: 'isPrimary', width: 120 }
]

onMounted(() => {
  fetchData()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})

// 自动刷新
const startAutoRefresh = () => {
  autoRefreshTimer = window.setInterval(() => {
    if (!loading.value && !modalVisible.value) {
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

async function fetchData(silent = false) {
  if (!silent) loading.value = true
  hasError.value = false
  try {
    const filters: Record<string, any> = {}
    if (activeTab.value === 'active') filters.cooperationStatus = 1
    else if (activeTab.value === 'inactive') filters.cooperationStatus = 2
    const res = await supplierApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, ...searchFilters, ...filters })
    tableData.value = (res as any).records?.map((r: any) => ({ ...r, supplierTypeLabel: supplierTypeMap[r.supplierType] || '未知' })) || mockData()
    pagination.total = (res as any).total || mockData().length
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch {
    if (!silent) {
      hasError.value = true
      message.error('获取数据失败')
    }
    tableData.value = mockData()
  }
  finally { if (!silent) loading.value = false }
}

const mockData = () => [
  { id: 1, supplierName: '北京原材料公司', supplierCode: 'SUP001', supplierType: 1, supplierTypeLabel: '原材料供应商', supplierLevel: 'A', contactPerson: '张经理', contactPhone: '13800138001', cooperationStatus: 1, purchaseAmount: 580000, payableAmount: 38000 },
  { id: 2, supplierName: '上海产品供应商', supplierCode: 'SUP002', supplierType: 2, supplierTypeLabel: '产品供应商', supplierLevel: 'B', contactPerson: '李主管', contactPhone: '13800138002', cooperationStatus: 1, purchaseAmount: 320000, payableAmount: 15000 },
  { id: 3, supplierName: '广州服务公司', supplierCode: 'SUP003', supplierType: 3, supplierTypeLabel: '服务供应商', supplierLevel: 'A', contactPerson: '王主任', contactPhone: '13800138003', cooperationStatus: 1, purchaseAmount: 150000, payableAmount: 8000 },
  { id: 4, supplierName: '深圳物流公司', supplierCode: 'SUP004', supplierType: 4, supplierTypeLabel: '物流供应商', supplierLevel: 'B', contactPerson: '赵总监', contactPhone: '13800138004', cooperationStatus: 2, purchaseAmount: 42000, payableAmount: 2000 }
]

const handleRefresh = () => {
  lastUpdateTime.value = ''
  fetchData()
}

function handleResetFilters() {
  for (const key of Object.keys(searchFilters)) {
    searchFilters[key] = undefined
  }
  pagination.current = 1
  fetchData()
}

function handleView(record: any) {
  supplierDetail.value = {
    ...record, orderCount: 28, paidAmount: 580000 - 38000,
    qualityScore: 92, deliveryScore: 88, serviceScore: 90, totalScore: 90,
    recentOrders: [
      { orderNo: 'PO20240115001', amount: 58000, orderDate: '2024-01-15', status: 'received', statusLabel: '已收货' },
      { orderNo: 'PO20240110002', amount: 32000, orderDate: '2024-01-10', status: 'completed', statusLabel: '已完成' },
      { orderNo: 'PO20240105003', amount: 45000, orderDate: '2024-01-05', status: 'pending', statusLabel: '待确认' }
    ]
  }
  detailVisible.value = true
}
function handleEdit(record: any) { modalTitle.value = '编辑供应商'; Object.assign(formData, record); modalVisible.value = true }
function handleAdd() {
  modalTitle.value = '新建供应商'
  formData.supplierCode = `SUP${String(Date.now()).slice(-6)}`
  modalVisible.value = true
}

function handleProducts(record: any) {
  productsModalTitle.value = `供应商产品 - ${record.supplierName}`
  productsData.value = [
    { id: 1, productCode: 'MAT001', productName: '钢板 Q235', spec: '10mm×2000mm×6000mm', price: 3850, unit: '吨', status: '正常供应' },
    { id: 2, productCode: 'MAT002', productName: '角钢 50×50×5', spec: 'Q235B 6m/根', price: 28.5, unit: '米', status: '正常供应' },
    { id: 3, productCode: 'MAT003', productName: '不锈钢板 304', spec: '1.5mm×1219mm×2438mm', price: 158, unit: '张', status: '正常供应' },
    { id: 4, productCode: 'MAT004', productName: '铝板 6061', spec: '2mm×1220mm×2440mm', price: 220, unit: '张', status: '限量供应' }
  ]
  productsModalVisible.value = true
}
function handleOrders(record: any) { router.push({ path: '/purchase/order', query: { supplierId: record.id, supplierName: record.supplierName } }) }
function handleEvaluate(record: any) { evaluateForm.supplierId = record.id; evaluateForm.supplierName = record.supplierName; evaluateForm.qualityScore = 80; evaluateForm.deliveryScore = 80; evaluateForm.serviceScore = 80; evaluateForm.comment = ''; evaluateModalVisible.value = true }
function handlePortal(record: any) {
  portalInfo.supplierName = record.supplierName
  portalInfo.portalUrl = `https://portal.example.com/supplier/${record.supplierCode || record.id}`
  portalInfo.account = `supplier_${record.supplierCode || record.id}`
  portalInfo.password = '******'
  portalModalVisible.value = true
}
function handleContact(record: any) {
  contactsModalTitle.value = `联系人列表 - ${record.supplierName}`
  contactsData.value = [
    { id: 1, name: record.contactPerson || '张经理', role: '采购负责人', phone: record.contactPhone || '138****1234', email: 'zhang@example.com', isPrimary: '是' },
    { id: 2, name: '李主管', role: '技术负责人', phone: '139****5678', email: 'li@example.com', isPrimary: '否' },
    { id: 3, name: '王专员', role: '售后服务', phone: '137****9012', email: 'wang@example.com', isPrimary: '否' }
  ]
  contactsModalVisible.value = true
}

function handleDelete(record: any) {
  Modal.confirm({ title: '确认删除', content: `确定要删除供应商"${record.supplierName}"吗？`, okText: '确认删除', okType: 'danger', cancelText: '取消', centered: true, async onOk() { try { await supplierApi.delete(record.id); message.success('删除成功'); fetchData() } catch { message.error('删除失败') } } })
}
function handleEvaluateSubmit() {
  if (!evaluateForm.comment) { message.warning('请输入评价内容'); return }
  const avg = Math.round((evaluateForm.qualityScore + evaluateForm.deliveryScore + evaluateForm.serviceScore) / 3)
  message.success(`评估提交成功！综合评分：${avg} 分`); evaluateModalVisible.value = false
}
function handleCopyPortalUrl() { navigator.clipboard.writeText(portalInfo.portalUrl).then(() => message.success('门户地址已复制到剪贴板')).catch(() => message.error('复制失败')) }
function handleResetPortalPassword() { Modal.confirm({ title: '重置密码', content: `确定要重置供应商"${portalInfo.supplierName}"的门户密码吗？`, okText: '确认重置', cancelText: '取消', centered: true, onOk() { portalInfo.password = '******'; message.success('密码已重置') } }) }
function handleOpenPortal() { window.open(portalInfo.portalUrl, '_blank'); message.success('正在打开供应商门户') }

async function handleSubmit() {
  try { await formRef.value?.validate() } catch { return }
  submitLoading.value = true
  try {
    if (formData.id) { await supplierApi.update(formData.id, formData as any) } else { await supplierApi.create(formData as any) }
    message.success('保存成功'); modalVisible.value = false; fetchData()
  } catch (err: any) { message.error(err?.message || '保存失败') }
  finally { submitLoading.value = false }
}
function handleModalCancel() { formRef.value?.resetFields(); modalVisible.value = false }

function handleExport() {
  const headers = ['供应商名称', '编码', '类型', '等级', '联系人', '电话', '状态']
  const rows = tableData.value.map((row: any) => [row.supplierName, row.supplierCode, row.supplierTypeLabel, row.supplierLevel, row.contactPerson, row.contactPhone, getStatusText(row.cooperationStatus)])
  exportCsv(headers, rows, '供应商')
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }
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

.stat-desc.positive {
  color: #52c41a;
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

.empty-placeholder {
  color: transparent;
}

.supplier-name {
  font-weight: 500;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

.amount-cell.payable {
  font-weight: 600;
}

/* 表格网格边框 */
:deep(.ant-table-thead > tr > th) {
  border-top: 1px solid #d9d9d9 !important;
  border-right: 1px solid #d9d9d9 !important;
  border-bottom: 2px solid #b0b0b0 !important;
  background: #fafafa !important;
  padding: 8px 12px !important;
  font-weight: 600 !important;
}

:deep(.ant-table-thead > tr > th:first-child) {
  border-left: 1px solid #d9d9d9 !important;
}

:deep(.ant-table-tbody > tr > td) {
  border-right: 1px solid #e0e0e0 !important;
  border-bottom: 1px solid #e8e8e8 !important;
  padding: 8px 12px !important;
}

:deep(.ant-table-tbody > tr > td:first-child) {
  border-left: 1px solid #e0e0e0 !important;
}

:deep(.ant-tabs) {
  margin: 0 24px;
}
</style>
