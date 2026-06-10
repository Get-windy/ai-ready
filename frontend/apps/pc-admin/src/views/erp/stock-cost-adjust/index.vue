<template>
  <ErrorBoundary @reset="fetchData" @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="cost-adjust-header">
        <div class="cost-adjust-header__left">
          <span class="cost-adjust-header__breadcrumb">ERP / 库存管理 / 成本调价</span>
          <h2 class="cost-adjust-header__title">成本调价</h2>
        </div>
        <div class="cost-adjust-header__right">
          <a-space :size="12">
            <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
              <SyncOutlined /> {{ autoRefreshCountdown }}s
            </span>
            <span class="data-status">
              <a-badge :status="loading ? 'processing' : 'success'" />
              <span v-if="lastUpdateTime" class="update-time">
                数据更新: {{ lastUpdateTime }}
              </span>
            </span>
            <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
          </a-space>
        </div>
      </div>
    </template>

    <!-- 统计卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px;">
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
            <FileTextOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">调价单总数</div>
            <div class="summary-value">{{ statistics.totalCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
            <ClockCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">待审批</div>
            <div class="summary-value warning">{{ statistics.pendingCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
            <CheckCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">已执行</div>
            <div class="summary-value">{{ statistics.executedCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card highlight">
          <div class="summary-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
            <DollarOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">调价总金额</div>
            <div class="summary-value">¥{{ formatAmount(statistics.totalAmount) }}</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <a-card title="成本调价" style="flex: 1; overflow: hidden;" :bodyStyle="{ display: 'flex', flexDirection: 'column', height: 'calc(100% - 57px)' }">
      <!-- 搜索栏 -->
      <SearchBar
        :fields="searchFields"
        :loading="loading"
        @search="handleSearch"
        @reset="handleReset"
      />

      <!-- 操作按钮 -->
      <div class="action-area">
        <a-space>
          <a-button type="primary" @click="handleCreate">
            <template #icon><PlusOutlined /></template>
            新建调价单
          </a-button>
        </a-space>
      </div>

      <!-- 数据表格 -->
      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
        @cell-dblclick="handleView"
        @page-change="handlePageChange"
      >
        <template #empty>
          <EmptyState v-if="hasError" image="error" title="数据加载异常" description="数据获取失败，请检查后重试" :show-add="false" size="small" @refresh="fetchData" />
          <EmptyState v-else image="no-data" title="暂无调价单" description="当前没有成本调价单数据" add-text="新建调价单" size="small" @refresh="fetchData" @add="handleCreate" />
        </template>
        <template #statusCell="{ record }">
          <StatusTag :status="record.status" :map="COST_ADJUST_STATUS" />
        </template>
        <template #totalAdjustAmountCell="{ record }">
          ¥{{ record.totalAdjustAmount?.toFixed(2) }}
        </template>
        <template #action="{ record }">
          <a-space :size="4">
            <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
            <template v-if="record.status === 0">
              <a-button type="link" size="small" @click="handleSubmitApproval(record)">提交</a-button>
            </template>
            <template v-else-if="record.status === 1">
              <a-dropdown>
                <a-button type="link" size="small">
                  审批 <DownOutlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handleApprove(record)">
                      <CheckOutlined /> 审批通过
                    </a-menu-item>
                    <a-menu-item @click="handleReject(record)">
                      <CloseOutlined /> 拒绝
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </template>
            <template v-else-if="record.status === 2">
              <a-button type="link" size="small" @click="handleExecute(record)">执行</a-button>
            </template>
            <template v-else-if="record.status === 3">
              <PrintButton
                template-type="stock_cost_adjust"
                :business-id="record.id"
                business-type="stock_cost_adjust"
                button-text="打印"
                button-size="small"
                @print-success="() => message.success(`调价单 ${record.adjustNo} 打印成功`)"
                @print-error="(e: any) => message.error(`打印失败: ${e.message || '未知错误'}`)"
              />
            </template>
            <template v-if="record.status === 0 || record.status === 2">
              <a-button type="link" size="small" danger @click="handleCancel(record)">取消</a-button>
            </template>
          </a-space>
        </template>
      </VxeTableList>
    </a-card>

    <!-- 详情弹窗 -->
    <a-drawer v-model:open="detailVisible" title="调价单详情" placement="right" width="80vw">
      <a-spin :spinning="detailLoading">
        <a-descriptions bordered :column="2" v-if="detailData">
          <a-descriptions-item label="调价单号">{{ detailData.adjustNo }}</a-descriptions-item>
          <a-descriptions-item label="仓库">{{ detailData.warehouseName }}</a-descriptions-item>
          <a-descriptions-item label="调价类型">{{ detailData.adjustTypeLabel || '-' }}</a-descriptions-item>
          <a-descriptions-item label="调价日期">{{ detailData.adjustDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="调价总金额">¥{{ detailData.totalAdjustAmount?.toFixed(2) }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <StatusTag :status="detailData.status" :map="COST_ADJUST_STATUS" />
          </a-descriptions-item>
          <a-descriptions-item label="原因类型">{{ detailData.reasonTypeLabel || '-' }}</a-descriptions-item>
          <a-descriptions-item label="申请人">{{ detailData.applicantName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ detailData.createTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">{{ detailData.reasonDesc || '-' }}</a-descriptions-item>
        </a-descriptions>

        <!-- 商品明细 -->
        <div v-if="detailData?.items?.length" style="margin-top: 16px;">
          <h4 style="margin-bottom: 8px; font-weight: 600;">调价明细</h4>
          <a-table
            :dataSource="detailData.items"
            :columns="detailItemColumns"
            :pagination="false"
            size="small"
            row-key="id"
            bordered
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'unitPriceDiff'">
                <span :class="{ 'price-up': record.newCost > record.oldCost, 'price-down': record.newCost < record.oldCost }">
                  {{ record.newCost > record.oldCost ? '+' : '' }}{{ ((record.newCost || 0) - (record.oldCost || 0)).toFixed(2) }}
                </span>
              </template>
              <template v-else-if="column.dataIndex === 'oldCost'">
                ¥{{ (record.oldCost || 0).toFixed(2) }}
              </template>
              <template v-else-if="column.dataIndex === 'newCost'">
                ¥{{ (record.newCost || 0).toFixed(2) }}
              </template>
              <template v-else-if="column.dataIndex === 'amount'">
                ¥{{ ((record.currentQuantity || 0) * ((record.newCost || 0) - (record.oldCost || 0))).toFixed(2) }}
              </template>
            </template>
          </a-table>
        </div>
      </a-spin>

      <template #footer>
        <div style="text-align: right;">
          <a-space>
            <a-button @click="detailVisible = false">关闭</a-button>
            <a-button v-if="detailData?.status === 0" @click="handleSubmitApproval(detailData)">提交审批</a-button>
            <template v-if="detailData?.status === 1">
              <a-button type="primary" @click="handleApprove(detailData)">审批通过</a-button>
              <a-button danger @click="handleReject(detailData)">拒绝</a-button>
            </template>
            <a-button v-if="detailData?.status === 2" type="primary" @click="handleExecute(detailData)">执行</a-button>
            <PrintButton
              v-if="detailData?.status >= 3"
              template-type="stock_cost_adjust"
              :business-id="detailData?.id"
              business-type="stock_cost_adjust"
              button-text="打印"
              button-size="small"
              @print-success="() => message.success(`调价单 ${detailData?.adjustNo} 打印成功`)"
              @print-error="(e: any) => message.error(`打印失败: ${e.message || '未知错误'}`)"
            />
          </a-space>
        </div>
      </template>
    </a-drawer>
  </PageContainer>

  <!-- 新建调价单弹窗 -->
  <a-modal
    v-model:open="createVisible"
    title="新建调价单"
    width="900px"
    :confirm-loading="createLoading"
    @ok="handleCreateSubmit"
    @cancel="handleCreateCancel"
    destroy-on-close
  >
    <a-form ref="createFormRef" :model="createForm" :rules="createRules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item label="调价类型" name="adjustType">
            <a-select v-model:value="createForm.adjustType" placeholder="请选择调价类型">
              <a-select-option :value="1">移动加权</a-select-option>
              <a-select-option :value="2">全月平均</a-select-option>
              <a-select-option :value="3">个别计价</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="仓库" name="warehouseId">
            <a-select v-model:value="createForm.warehouseId" placeholder="请选择仓库" show-search :filter-option="filterOption" allow-clear>
              <a-select-option v-for="w in warehouseOptions" :key="w.id" :value="w.id">{{ w.warehouseName }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="调价日期" name="adjustDate">
            <a-date-picker v-model:value="createForm.adjustDate" style="width: 100%" value-format="YYYY-MM-DD" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item label="原因类型" name="reasonType">
            <a-select v-model:value="createForm.reasonType" placeholder="请选择原因类型">
              <a-select-option :value="1">市场波动</a-select-option>
              <a-select-option :value="2">供应商调价</a-select-option>
              <a-select-option :value="3">汇率变动</a-select-option>
              <a-select-option :value="4">其他</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="16">
          <a-form-item label="原因说明" name="reasonDesc">
            <a-textarea v-model:value="createForm.reasonDesc" :rows="1" placeholder="请输入调价原因说明" />
          </a-form-item>
        </a-col>
      </a-row>

      <!-- 调价明细 -->
      <div class="sub-table-header">
        <span class="sub-table-title">调价明细</span>
        <a-button type="dashed" size="small" @click="addItem"><PlusOutlined /> 添加产品</a-button>
      </div>
      <a-table
        :dataSource="createForm.items"
        :columns="itemColumns"
        :pagination="false"
        size="small"
        row-key="tempId"
        style="margin-bottom: 12px;"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.dataIndex === 'productName'">
            <a-input v-model:value="record.productName" placeholder="产品名称" style="width: 120px" />
            <a-tooltip title="选择产品"><a-button size="small" type="link" @click="selectItemProduct(index)"><SearchOutlined /></a-button></a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'specification'">
            <a-input v-model:value="record.specification" placeholder="规格" style="width: 80px" />
          </template>
          <template v-else-if="column.dataIndex === 'currentQuantity'">
            <a-input-number v-model:value="record.currentQuantity" :min="0" :precision="0" style="width: 80px" />
          </template>
          <template v-else-if="column.dataIndex === 'oldCost'">
            <a-input-number v-model:value="record.oldCost" :min="0" :precision="2" style="width: 100px" />
          </template>
          <template v-else-if="column.dataIndex === 'newCost'">
            <a-input-number v-model:value="record.newCost" :min="0" :precision="2" style="width: 100px" />
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-button type="link" danger size="small" @click="removeItem(index)"><DeleteOutlined /></a-button>
          </template>
        </template>
      </a-table>
    </a-form>
  </a-modal>

  <!-- 商品选择弹窗 -->
  <a-modal v-model:open="productPickerVisible" title="选择产品" width="640px" :footer="null" destroy-on-close>
    <a-input-search v-model:value="productSearchKeyword" placeholder="搜索产品编码/名称" @search="loadProductOptions" />
    <a-table
      :dataSource="productOptions"
      :columns="productPickerColumns"
      :pagination="{ pageSize: 5 }"
      :loading="productLoading"
      size="small"
      row-key="id"
      style="margin-top: 12px;"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'action'">
          <a-button type="primary" size="small" @click="pickProduct(record)">选择</a-button>
        </template>
      </template>
    </a-table>
  </a-modal>

  <!-- 取消原因弹窗 -->
  <a-modal v-model:open="cancelModalVisible" title="取消确认" width="480px" :confirm-loading="cancelLoading" @ok="handleCancelConfirm" @cancel="handleCancelClose" destroy-on-close>
    <a-form layout="vertical">
      <a-form-item label="取消原因" required>
        <a-textarea v-model:value="cancelReason" :rows="3" placeholder="请输入取消原因（必填）" />
      </a-form-item>
    </a-form>
  </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined, ReloadOutlined, SyncOutlined, FileTextOutlined, ClockCircleOutlined,
  CheckCircleOutlined, DollarOutlined, WarningOutlined, SearchOutlined, DeleteOutlined,
  DownOutlined, CheckOutlined, CloseOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { PageContainer, SearchBar, EmptyState } from '@/components'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import request from '@/utils/request'
import { COST_ADJUST_STATUS } from '@/utils/statusConfig'

// ── 类型定义 ──────────────────────────────────────────
interface CostAdjustItem {
  tempId?: number
  productId?: number
  productCode: string
  productName: string
  specification: string
  unit: string
  currentQuantity: number
  oldCost: number
  newCost: number
}

interface CostAdjustOrder {
  id: number
  adjustNo: string
  warehouseName: string
  adjustType: number
  adjustTypeLabel?: string
  adjustDate: string
  totalAdjustAmount: number
  totalItems: number
  status: number
  reasonType: number
  reasonTypeLabel?: string
  reasonDesc: string
  applicantName: string
  createTime: string
  items?: CostAdjustItem[]
}

// ── 防抖与错误处理 ─────────────────────────────────────
function handleError(err: any) { console.warn('[成本调价] ErrorBoundary 捕获异常:', err) }

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

// ── 键盘快捷键 ──────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleCreate(); return }
}

const loading = ref(false)
const hasError = ref(false)
const refreshLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const tableData = ref<any[]>([])
const detailVisible = ref(false)
const currentRecord = ref<any>(null)
const tableRef = ref()

let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// 统计数据
const statistics = ref({
  totalCount: 0,
  pendingCount: 0,
  executedCount: 0,
  totalAmount: 0
})

const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const searchFields: SearchField[] = [
  { name: 'adjustNo', label: '调价单号', type: 'input', placeholder: '请输入调价单号' },
  { name: 'warehouseId', label: '仓库', type: 'select', placeholder: '请选择仓库', options: [] },
  { name: 'status', label: '状态', type: 'select', placeholder: '请选择',
    options: Object.entries(COST_ADJUST_STATUS).map(([k, v]) => ({ label: v.text, value: Number(k) }))
  },
]

const searchParams = reactive({
  adjustNo: '',
  warehouseId: undefined as number | undefined,
  status: undefined as number | undefined
})

const pagination = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true, showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const vxeColumns = computed(() => [
  { field: 'adjustNo', title: '调价单号', width: 150 },
  { field: 'warehouseName', title: '仓库', width: 120 },
  { field: 'adjustTypeLabel', title: '调价类型', width: 100 },
  { field: 'adjustDate', title: '调价日期', width: 120 },
  { field: 'totalAdjustAmount', title: '调价金额', width: 120, slotName: 'totalAdjustAmountCell' },
  { field: 'totalItems', title: '明细项数', width: 90, align: 'center' },
  { field: 'status', title: '状态', width: 100, slotName: 'statusCell' },
  { field: 'applicantName', title: '申请人', width: 100 },
  { field: 'action', title: '操作', width: 220, fixed: 'right', type: 'action' },
])

const fetchData = async () => {
  hasError.value = false
  loading.value = true
  try {
    const res = await request.get('/erp/stock/cost-adjust/page', {
      params: { ...searchParams, pageNum: pagination.current, pageSize: pagination.pageSize }
    })
    const data = res.data || res
    tableData.value = data?.records || []
    pagination.total = data?.total || 0
    if (data.totalCount !== undefined) {
      statistics.value.totalCount = data.totalCount
      statistics.value.pendingCount = data.pendingCount || 0
      statistics.value.executedCount = data.executedCount || 0
      statistics.value.totalAmount = data.totalAmount || 0
    } else {
      statistics.value.totalCount = tableData.value.length
      statistics.value.pendingCount = tableData.value.filter((r: any) => r.status === 1).length
      statistics.value.executedCount = tableData.value.filter((r: any) => r.status === 3).length
      statistics.value.totalAmount = tableData.value.reduce((sum: number, r: any) => sum + (r.totalAdjustAmount || 0), 0)
    }
  } catch (error) {
    hasError.value = true
    console.warn('[成本调价] 获取数据失败', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
    refreshLoading.value = false
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
  }
}

const handleSearch = (values?: Record<string, any>) => {
  if (values) {
    Object.assign(searchParams, values)
  }
  pagination.current = 1
  fetchData()
}
const handleReset = () => {
  Object.assign(searchParams, { adjustNo: '', warehouseId: undefined, status: undefined })
  pagination.current = 1
  fetchData()
}
const handlePageChange = (page: number, size: number) => { pagination.current = page; pagination.pageSize = size; fetchData() }

// ════════════════════════════════════════════════════════════════
// 新建调价单
// ════════════════════════════════════════════════════════════════

let tempIdCounter = 0
function nextTempId() { return ++tempIdCounter }

const createVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref<any>(null)
const createForm = reactive({
  adjustType: 1,
  warehouseId: undefined as number | undefined,
  adjustDate: '',
  reasonType: 1,
  reasonDesc: '',
  items: [] as any[]
})
const createRules: Record<string, any[]> = {
  adjustType: [{ required: true, message: '请选择调价类型' }],
  warehouseId: [{ required: true, message: '请选择仓库' }],
  adjustDate: [{ required: true, message: '请选择调价日期' }],
  reasonType: [{ required: true, message: '请选择原因类型' }]
}

const itemColumns = [
  { title: '产品编码', dataIndex: 'productCode', width: 100 },
  { title: '产品名称', dataIndex: 'productName', width: 180 },
  { title: '规格', dataIndex: 'specification', width: 80 },
  { title: '单位', dataIndex: 'unit', width: 60 },
  { title: '当前库存', dataIndex: 'currentQuantity', width: 80 },
  { title: '原成本价', dataIndex: 'oldCost', width: 100 },
  { title: '新成本价', dataIndex: 'newCost', width: 100 },
  { title: '操作', dataIndex: 'action', width: 60 }
]

const detailItemColumns = [
  { title: '产品编码', dataIndex: 'productCode', width: 120 },
  { title: '产品名称', dataIndex: 'productName', width: 180 },
  { title: '规格', dataIndex: 'specification', width: 100 },
  { title: '单位', dataIndex: 'unit', width: 60 },
  { title: '当前库存', dataIndex: 'currentQuantity', width: 80, align: 'right' },
  { title: '原成本价', dataIndex: 'oldCost', width: 100, align: 'right' },
  { title: '新成本价', dataIndex: 'newCost', width: 100, align: 'right' },
  { title: '价差', dataIndex: 'unitPriceDiff', width: 80, align: 'right' },
  { title: '调价金额', dataIndex: 'amount', width: 120, align: 'right' },
]

// 仓库选项
const warehouseOptions = ref<any[]>([])
// 产品选项
const productOptions = ref<any[]>([])
const productLoading = ref(false)
const productSearchKeyword = ref('')
const productPickerVisible = ref(false)
let pickerTargetIndex = -1

const productPickerColumns = [
  { title: '产品编码', dataIndex: 'productCode', width: 130 },
  { title: '产品名称', dataIndex: 'productName', width: 180 },
  { title: '规格', dataIndex: 'specification', width: 100 },
  { title: '单位', dataIndex: 'unit', width: 60 },
  { title: '操作', dataIndex: 'action', width: 80 }
]

function filterOption(input: string, option: any) {
  return (option.children?.toString() || '').toLowerCase().includes(input.toLowerCase())
}

function addItem() {
  createForm.items.push({
    tempId: nextTempId(),
    productId: undefined,
    productCode: '',
    productName: '',
    specification: '',
    unit: '',
    currentQuantity: 0,
    oldCost: 0,
    newCost: 0
  })
}

function removeItem(index: number) {
  createForm.items.splice(index, 1)
}

function selectItemProduct(index: number) {
  pickerTargetIndex = index
  productPickerVisible.value = true
  productSearchKeyword.value = ''
  productOptions.value = []
  loadProductOptions()
}

async function loadProductOptions() {
  productLoading.value = true
  try {
    const res = await request.get('/erp/product/list', {
      params: { keyword: productSearchKeyword.value || undefined, pageSize: 50 }
    })
    const data = res?.data ?? res
    productOptions.value = Array.isArray(data) ? data : []
  } catch { productOptions.value = [] }
  finally { productLoading.value = false }
}

function pickProduct(product: any) {
  if (pickerTargetIndex >= 0 && pickerTargetIndex < createForm.items.length) {
    const item = createForm.items[pickerTargetIndex]
    item.productId = product.id
    item.productCode = product.productCode
    item.productName = product.productName
    item.specification = product.specification || ''
    item.unit = product.unit || ''
  }
  productPickerVisible.value = false
}

async function loadWarehouseOptions() {
  try {
    const res = await request.get('/erp/stock/warehouses')
    const data = res?.data ?? res
    warehouseOptions.value = Array.isArray(data) ? data : []
    // Update search field options
    const warehouseField = searchFields.find(f => f.name === 'warehouseId')
    if (warehouseField) {
      warehouseField.options = warehouseOptions.value.map(w => ({ label: w.warehouseName, value: w.id }))
    }
  } catch { /* ignore */ }
}

const handleCreate = () => {
  tempIdCounter = 0
  createForm.adjustType = 1
  createForm.warehouseId = undefined
  createForm.adjustDate = new Date().toISOString().slice(0, 10)
  createForm.reasonType = 1
  createForm.reasonDesc = ''
  createForm.items = []
  createVisible.value = true
  nextTick(() => createFormRef.value?.resetFields?.())
}

const handleCreateSubmit = async () => {
  try {
    await createFormRef.value?.validate()
  } catch { return }
  if (createForm.items.length === 0) {
    message.warning('请添加调价明细')
    return
  }
  // Validate items
  const invalidItem = createForm.items.find(i => !i.productId)
  if (invalidItem) {
    message.warning('请完善调价明细中的产品信息')
    return
  }
  createLoading.value = true
  try {
    await request.post('/erp/stock/cost-adjust', {
      adjustType: createForm.adjustType,
      warehouseId: createForm.warehouseId,
      adjustDate: createForm.adjustDate,
      reasonType: createForm.reasonType,
      reasonDesc: createForm.reasonDesc || undefined,
      items: createForm.items.map(item => ({
        productId: item.productId,
        productCode: item.productCode,
        productName: item.productName,
        specification: item.specification || undefined,
        unit: item.unit || undefined,
        currentQuantity: item.currentQuantity,
        oldCost: item.oldCost,
        newCost: item.newCost
      }))
    })
    message.success('调价单创建成功')
    createVisible.value = false
    fetchData()
  } catch (err: any) {
    console.warn('[成本调价] 创建失败', err)
    message.error(err?.message || '创建失败，请稍后重试')
  } finally {
    createLoading.value = false
  }
}

const handleCreateCancel = () => {
  createVisible.value = false
}

function handleParentCreate() { handleCreate() }

// ════════════════════════════════════════════════════════════════
// 详情
// ════════════════════════════════════════════════════════════════
const detailData = ref<any>(null)
const detailLoading = ref(false)

const fetchDetail = async (id: number) => {
  detailLoading.value = true
  try {
    const res = await request.get(`/erp/stock/cost-adjust/${id}`)
    detailData.value = res.data || null
  } catch (err) {
    console.warn('[成本调价] 获取详情失败', err)
    detailData.value = tableData.value.find(item => item.id === id) || null
  } finally {
    detailLoading.value = false
  }
}

const handleView = (record: any) => {
  detailVisible.value = true
  fetchDetail(record.id)
}

// ════════════════════════════════════════════════════════════════
// 操作
// ════════════════════════════════════════════════════════════════

const handleSubmitApproval = async (record: any) => {
  Modal.confirm({
    title: '提交审批',
    content: `确认提交调价单 ${record.adjustNo} 进行审批吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/stock/cost-adjust/${record.id}/submit`)
        message.success('提交成功')
        fetchData()
        if (detailVisible.value) detailVisible.value = false
      } catch (error) {
        console.warn('[成本调价] 提交失败', error)
        message.error('提交失败')
      }
    }
  })
}

const handleApprove = async (record: any) => {
  Modal.confirm({
    title: '审批确认',
    content: '确认审批通过该调价单吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/stock/cost-adjust/${record.id}/approve`)
        message.success('审批通过')
        fetchData()
        if (detailVisible.value) detailVisible.value = false
      } catch (error) {
        console.warn('[成本调价] 审批失败', error)
        message.error('审批失败')
      }
    }
  })
}

const handleReject = async (record: any) => {
  Modal.confirm({
    title: '拒绝确认',
    content: '确认拒绝该调价单吗？',
    okText: '确认拒绝',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: async () => {
      try {
        await request.post(`/erp/stock/cost-adjust/${record.id}/reject`)
        message.success('已拒绝')
        fetchData()
        if (detailVisible.value) detailVisible.value = false
      } catch (error) {
        console.warn('[成本调价] 拒绝失败', error)
        message.error('拒绝失败')
      }
    }
  })
}

const handleExecute = async (record: any) => {
  Modal.confirm({
    title: '执行确认',
    content: '确认执行该调价单吗？执行后将更新库存成本。',
    okText: '确认执行',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/stock/cost-adjust/${record.id}/execute`)
        message.success('调价执行成功')
        fetchData()
        if (detailVisible.value) detailVisible.value = false
      } catch (error) {
        console.warn('[成本调价] 执行失败', error)
        message.error('调价执行失败')
      }
    }
  })
}

// ════════════════════════════════════════════════════════════════
// 取消操作
// ════════════════════════════════════════════════════════════════
const cancelModalVisible = ref(false)
const cancelReason = ref('')
const cancelLoading = ref(false)
let pendingCancelRecord: any = null

const handleCancel = (record: any) => {
  pendingCancelRecord = record
  cancelReason.value = ''
  cancelModalVisible.value = true
}

const handleCancelConfirm = async () => {
  if (!cancelReason.value.trim()) {
    message.warning('请输入取消原因')
    return
  }
  if (!pendingCancelRecord) return
  cancelLoading.value = true
  try {
    await request.post(`/erp/stock/cost-adjust/${pendingCancelRecord.id}/cancel`, null, {
      params: { reason: cancelReason.value.trim() }
    })
    message.success('取消成功')
    cancelModalVisible.value = false
    pendingCancelRecord = null
    fetchData()
    if (detailVisible.value) detailVisible.value = false
  } catch (error) {
    console.warn('[成本调价] 取消失败', error)
    message.error('取消失败')
  } finally {
    cancelLoading.value = false
  }
}

const handleCancelClose = () => {
  cancelModalVisible.value = false
  pendingCancelRecord = null
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
  loadWarehouseOptions()
  fetchData()
  window.addEventListener("erp:create", handleParentCreate)
  window.addEventListener("erp:refresh", fetchData)
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
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener("erp:create", handleParentCreate)
  window.removeEventListener("erp:refresh", fetchData)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.cost-adjust-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.cost-adjust-header__left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.cost-adjust-header__breadcrumb {
  font-size: 12px;
  color: #999;
}

.cost-adjust-header__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.cost-adjust-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #52c41a;
  white-space: nowrap;
}

.data-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.update-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.search-area { margin-bottom: 16px; }
.action-area { margin-bottom: 16px; }

.sub-table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.sub-table-title {
  font-weight: 600;
  font-size: 13px;
  color: #303133;
}

/* 统计卡片样式 */
.summary-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.summary-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.summary-card.highlight {
  background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%);
  border: 1px solid #d3adf7;
}

.summary-icon {
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

.summary-content {
  flex: 1;
}

.summary-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}

.summary-value.warning {
  color: #faad14;
}

/* 表格容器自动撑满 */
:deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
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
  margin-bottom: 12px;
}

.table-empty-text {
  color: #999;
  margin-bottom: 16px;
}

.price-up { color: #f5222d; font-weight: 500; }
.price-down { color: #52c41a; font-weight: 500; }

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
</style>
