<template>
  <PageContainer full-height>
    <template #header>
      <div class="shipment-page-header">
        <div class="shipment-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>发货管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="shipment-page-title">发货管理</h2>
        </div>
        <div class="shipment-page-header-right">
          <a-switch size="small" v-model:checked="autoRefreshEnabled" checked-children="自动" un-checked-children="手动" @change="handleAutoRefreshChange" />
          <span class="data-status">
            <a-badge :status="loading ? 'processing' : hasError ? 'error' : 'success'" />
            <span v-if="lastUpdateTime" class="update-time">
              数据更新: {{ lastUpdateTime }}
            </span>
          </span>
          <span v-if="autoRefreshEnabled && autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="loading" v-permission="'erp:shipment:refresh'" @click="handleRefresh">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          </span>
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
            <div class="summary-title">出库单总数</div>
            <div class="summary-value">{{ statusCounts.total }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
            <ClockCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">待审核</div>
            <div class="summary-value warning">{{ statusCounts.pending }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
            <ExportOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">待出库</div>
            <div class="summary-value">{{ statusCounts.processing }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card highlight">
          <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
            <CheckCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">已完成</div>
            <div class="summary-value">{{ statusCounts.completed }}</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 搜索栏 -->
    <SearchBar
      :fields="searchFields"
      :loading="loading"
      @search="(e: any) => handleSearch(e)"
      @reset="handleReset"
    />

    <ErrorBoundary @error="handleError">
      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="'id'"
        :filter-fields="filterFields"
        :selectable="true"
        :show-export="true"
        export-permission="shipment:export"
        add-text="新建出库单"
        add-permission="shipment:create"
        @add="handleCreate"
        @refresh="() => fetchData()"
        @export="handleExport"
        @search="(e: any) => handleSearch(e)"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
        @selection-change="handleSelectionChange"
        @cell-dblclick="handleView"
      >
        <template #toolbar-actions>
          <span class="stats-summary">
            <span class="stats-item">
              <span class="stats-label">待审核:</span>
              <span class="stats-value pending">{{ statusCounts.pending }}</span>
            </span>
            <span class="stats-item">
              <span class="stats-label">待出库:</span>
              <span class="stats-value processing">{{ statusCounts.processing }}</span>
            </span>
            <span class="stats-item">
              <span class="stats-label">已完成:</span>
              <span class="stats-value completed">{{ statusCounts.completed }}</span>
            </span>
          </span>
        </template>

        <template #empty>
          <div v-if="hasError" class="table-empty">
            <WarningOutlined class="table-empty-icon" />
            <p class="table-empty-text">数据加载异常，请重试</p>
            <a-button type="primary" @click="() => fetchData()"><ReloadOutlined /> 重试</a-button>
          </div>
          <EmptyState v-else title="暂无数据" description="暂无出库单数据" size="small" :show-actions="false" />
        </template>

        <template #action="{ record }">
          <a-space>
            <a-tooltip title="查看详情">
              <a-button type="link" size="small" v-permission="'erp:shipment:view'" @click="handleView(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 0" title="审核">
              <a-button type="link" size="small" v-permission="'erp:shipment:approve'" @click="handleApprove(record)">
                <template #icon><CheckCircleOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 1" title="出库">
              <a-button type="link" size="small" v-permission="'erp:shipment:ship'" @click="handleShip(record)">
                <template #icon><ExportOutlined /></template>
              </a-button>
            </a-tooltip>
            <PrintButton
              v-if="record.status >= 2"
              templateType="stock_out"
              :businessId="record.id"
              businessType="shipment"
              buttonText="打印"
              buttonSize="small"
              @print-success="handlePrintSuccess(record)"
              @print-error="handlePrintError"
            />
            <a-dropdown trigger="click">
              <a-button type="link" size="small" class="action-more-btn">
                <template #icon><EllipsisOutlined /></template>
              </a-button>
              <template #overlay>
                <a-menu @click="(e) => handleActionMenuClick(e.key, record)">
                  <a-menu-item key="edit" v-if="record.status === 0">
                    <EditOutlined /> 编辑
                  </a-menu-item>
                  <a-menu-item key="delete" v-if="record.status === 0">
                    <DeleteOutlined /> 删除
                  </a-menu-item>
                  <a-menu-item key="tracking" v-if="record.status >= 2 && !record.trackingNo">
                    <NumberOutlined /> 填写物流单号
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </VxeTableList>
    </ErrorBoundary>

    <!-- 详情弹窗 -->
    <a-drawer
      v-model:open="detailVisible"
      title="出库单详情"
      placement="right"
      width="80vw"
      @update:open="(v: boolean) => { if (!v) editMode = false }"
    >
      <template #extra>
        <a-space>
          <a-button v-if="detailData?.status === 0 && !editMode" size="small" v-permission="'erp:shipment:startedit'" @click="handleStartEdit">编辑</a-button>
          <PrintButton :business-id="detailData?.id" business-type="shipment" button-size="small" tooltip="打印" />
        </a-space>
      </template>

      <a-spin :spinning="detailLoading">
        <a-descriptions bordered :column="2" v-if="detailData" size="small">
          <a-descriptions-item label="出库单号">
            <span class="code-text">{{ detailData.shipmentNo }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="销售订单">
            <a @click="handleViewOrder">{{ detailData.orderNo }}</a>
          </a-descriptions-item>
          <a-descriptions-item label="客户名称">
            <span class="customer-name">{{ detailData.customerName }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="仓库">{{ detailData.warehouseName }}</a-descriptions-item>
          <a-descriptions-item label="出库类型">
            <span>{{ detailData.outboundTypeName || '-' }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="出库金额">
            <span class="amount-cell">¥{{ formatAmount(detailData.totalAmount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <StatusTag :status="detailData.status" :map="SHIPMENT_STATUS" />
          </a-descriptions-item>
          <a-descriptions-item label="出库日期">{{ detailData.shipmentDate }}</a-descriptions-item>
          <a-descriptions-item label="联系人">
            <template v-if="editMode">
              <a-input v-model:value="editForm.contactName" size="small" placeholder="请输入联系人" />
            </template>
            <template v-else>
              <span>{{ detailData.contactName || '-' }}</span>
            </template>
          </a-descriptions-item>
          <a-descriptions-item label="联系电话">
            <template v-if="editMode">
              <a-input v-model:value="editForm.contactPhone" size="small" placeholder="请输入联系电话" />
            </template>
            <template v-else>
              <span>{{ detailData.contactPhone || '-' }}</span>
            </template>
          </a-descriptions-item>
          <a-descriptions-item label="业务员">
            <span>{{ detailData.salesPersonName || '-' }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="部门">
            <span>{{ detailData.departmentName || '-' }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="收货人">
            <template v-if="editMode">
              <a-input v-model:value="editForm.receiverName" size="small" placeholder="请输入收货人" />
            </template>
            <template v-else>
              <span>{{ detailData.receiverName || '-' }}</span>
            </template>
          </a-descriptions-item>
          <a-descriptions-item label="收货电话">
            <template v-if="editMode">
              <a-input v-model:value="editForm.receiverPhone" size="small" placeholder="请输入收货电话" />
            </template>
            <template v-else>
              <span>{{ detailData.receiverPhone || '-' }}</span>
            </template>
          </a-descriptions-item>
          <a-descriptions-item label="收货地址" :span="2">
            <template v-if="editMode">
              <a-input v-model:value="editForm.shippingAddress" size="small" placeholder="请输入收货地址" />
            </template>
            <template v-else>
              <span>{{ detailData.shippingAddress || '-' }}</span>
            </template>
          </a-descriptions-item>
          <a-descriptions-item label="物流单号">
            <span v-if="detailData.trackingNo">{{ detailData.trackingNo }}</span>
            <span v-else class="empty-text">未填写</span>
          </a-descriptions-item>
          <a-descriptions-item label="操作人">{{ detailData.operator }}</a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">
            <template v-if="editMode">
              <a-textarea v-model:value="editForm.remark" :rows="2" placeholder="请输入备注" />
            </template>
            <template v-else>
              <span v-if="detailData.remark">{{ detailData.remark }}</span>
              <span v-else class="empty-text">无</span>
            </template>
          </a-descriptions-item>
        </a-descriptions>

        <h4 style="margin: 16px 0 8px;">出库明细</h4>
        <a-table
          :data-source="detailDataItems"
          :columns="detailItemColumns"
          :pagination="false as any"
          size="small"
          bordered
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'unitPrice' || column.key === 'amount'">
              <span class="amount-cell">¥{{ formatAmount(column.key === 'amount' ? record.amount : record.unitPrice) }}</span>
            </template>
          </template>
        </a-table>
      </a-spin>

      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 8px;">
          <template v-if="editMode">
            <a-button v-permission="'erp:shipment:canceledit'" @click="handleCancelEdit">取消</a-button>
            <a-button type="primary" v-permission="'erp:shipment:saveedit'" @click="handleSaveEdit">保存</a-button>
          </template>
          <template v-else>
            <a-button @click="detailVisible = false">关闭</a-button>
            <a-button v-if="detailData?.status === 0" type="primary" v-permission="'erp:shipment:approve'" @click="handleApprove(detailData)">
              审核
            </a-button>
            <a-button v-if="detailData?.status === 1" type="primary" v-permission="'erp:shipment:ship'" @click="handleShip(detailData)">
              出库
            </a-button>
            <PrintButton
              v-if="detailData && detailData.status >= 2"
              templateType="stock_out"
              :businessId="detailData.id"
              businessType="shipment"
            />
          </template>
        </div>
      </template>
    </a-drawer>

    <!-- 物流单号填写弹窗 -->
    <a-modal
      v-model:open="trackingVisible"
      title="填写物流单号"
      width="400px"
      @ok="handleSaveTracking"
    >
      <a-form layout="vertical">
        <a-form-item label="物流单号">
          <a-input size="small" v-model:value="trackingForm.trackingNo" placeholder="请输入物流单号" />
        </a-form-item>
        <a-form-item label="物流公司">
          <a-select size="small" v-model:value="trackingForm.carrier" placeholder="请选择物流公司">
            <a-select-option value="SF">顺丰速运</a-select-option>
            <a-select-option value="EMS">EMS</a-select-option>
            <a-select-option value="JD">京东物流</a-select-option>
            <a-select-option value="YT">圆通速递</a-select-option>
            <a-select-option value="ZT">中通快递</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>


    <FullScreenDetail
      :visible="createFormVisible"
      title="新建出库单"
      :save-loading="createFormSubmitting"
      @close="handleCreateFormCancel"
      @save="submitCreateForm"
    >
      <a-form
        ref="createFormRef"
        :model="createForm"
        :rules="formRules"
        layout="vertical"
      >
        <!-- 基本信息 -->
        <a-divider orientation="left">基本信息</a-divider>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="出库类型" name="outboundType" required>
              <a-select v-model:value="createForm.outboundType" placeholder="请选择出库类型" allow-clear>
                <a-select-option :value="1">销售出库</a-select-option>
                <a-select-option :value="2">换货出库</a-select-option>
                <a-select-option :value="3">调拨出库</a-select-option>
                <a-select-option :value="4">其他出库</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="出库日期" name="outboundDate" required>
              <a-date-picker v-model:value="createForm.outboundDate" style="width: 100%" placeholder="请选择出库日期" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="仓库" name="warehouseId" required>
              <a-select
                v-model:value="createForm.warehouseId"
                placeholder="请选择仓库"
                :options="warehouseOptions"
                :loading="warehouseLoading"
                allow-clear
                show-search
                option-filter-prop="label"
                @change="handleWarehouseChange"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 客户信息 -->
        <a-divider orientation="left">客户信息</a-divider>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="客户" name="customerId" required>
              <a-select
                v-model:value="createForm.customerId"
                placeholder="请选择客户"
                :options="customerOptions"
                :loading="customerLoading"
                allow-clear
                show-search
                option-filter-prop="label"
                @change="handleCustomerChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系人">
              <a-select
                v-model:value="createForm.contactId"
                placeholder="请选择联系人"
                allow-clear
                show-search
                option-filter-prop="label"
              >
                <a-select-option
                  v-for="c in contactOptions"
                  :key="c.id"
                  :value="c.id"
                  :label="c.name"
                >
                  {{ c.name }}{{ c.phone ? ` (${c.phone})` : '' }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 人员信息 -->
        <a-divider orientation="left">人员信息</a-divider>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="销售人员">
              <a-input v-model:value="createForm.salesPersonName" placeholder="销售人员姓名" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="所属部门">
              <a-input v-model:value="createForm.departmentName" placeholder="所属部门" />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 收货信息 -->
        <a-divider orientation="left">收货信息</a-divider>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="收货地址">
              <a-input v-model:value="createForm.shippingAddress" placeholder="请输入收货地址" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="收货人">
              <a-input v-model:value="createForm.receiverName" placeholder="请输入收货人" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="收货电话">
              <a-input v-model:value="createForm.receiverPhone" placeholder="请输入收货电话" />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 出库明细 -->
        <a-divider orientation="left">出库明细</a-divider>
        <a-table
          :data-source="createForm.items"
          :columns="itemFormColumns"
          :pagination="false as any"
          row-key="tempId"
          size="small"
          bordered
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'product'">
              <a-select
                v-model:value="record.productId"
                placeholder="搜索选择商品"
                style="width: 100%"
                show-search
                allow-clear
                :filter-option="false"
                :options="productOptions"
                :loading="productLoading"
                @search="(val: any) => handleProductSearch(val)"
                @change="(val) => handleProductChange(val, index)"
              >
                <template #option="{ label, productCode, productName, productSpec }">
                  <div>
                    <div>{{ productName || label }}</div>
                    <div style="font-size: 12px; color: #999;">
                      {{ productCode }}{{ productSpec ? ` / ${productSpec}` : '' }}
                    </div>
                  </div>
                </template>
              </a-select>
            </template>
            <template v-else-if="column.key === 'quantity'">
              <a-input-number
                v-model:value="record.orderQuantity"
                :min="0"
                :precision="0"
                style="width: 100%"
                placeholder="数量"
              />
            </template>
            <template v-else-if="column.key === 'unitPrice'">
              <a-input-number
                v-model:value="record.unitPrice"
                :min="0"
                :precision="2"
                style="width: 100%"
                placeholder="单价"
              />
            </template>
            <template v-else-if="column.key === 'remark'">
              <a-input v-model:value="record.remark" placeholder="备注" style="width: 100%" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button
                type="link"
                danger
                size="small"
 v-permission="'erp:shipment:removeitem'" @click="handleRemoveItem(index)"
                :disabled="createForm.items.length <= 1"
              >
                <template #icon><MinusCircleOutlined /></template>
                删除
              </a-button>
            </template>
          </template>
        </a-table>
        <a-button type="dashed" block style="margin-top: 8px;" v-permission="'erp:shipment:additem'" @click="handleAddItem">
          <template #icon><PlusOutlined /></template>
          添加商品行
        </a-button>

        <!-- 备注 -->
        <a-divider orientation="left">备注</a-divider>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="备注">
              <a-textarea v-model:value="createForm.remark" :rows="2" placeholder="备注信息" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="内部备注">
              <a-textarea v-model:value="createForm.internalNote" :rows="2" placeholder="内部备注" />
            </a-form-item>
          </a-col>
        </a-row>

      </a-form>
    </FullScreenDetail>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import dayjs from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import EmptyState from '@/components/EmptyState/EmptyState.vue'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import { SHIPMENT_STATUS } from '@/utils/statusConfig'
import request from '@/utils/request'
import {
  EyeOutlined,
  CheckCircleOutlined,
  ExportOutlined,
  EllipsisOutlined,
  DeleteOutlined,
  EditOutlined,
  SearchOutlined,
  InboxOutlined,
  ReloadOutlined,
  NumberOutlined,
  FileTextOutlined,
  ClockCircleOutlined,
  SyncOutlined,
  WarningOutlined,
  PlusOutlined,
  MinusCircleOutlined
} from '@ant-design/icons-vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

function handleError(err: any) {
  hasError.value = true
  console.warn('[发货管理] ErrorBoundary 捕获异常:', err)
}

interface ShipmentItem {
  id: number
  productName: string
  productCode: string
  productSpec?: string
  quantity: number
  unitPrice: number
  amount: number
}

interface Shipment {
  id: number
  shipmentNo: string
  orderNo: string
  customerName: string
  warehouseName: string
  totalAmount: number
  status: number
  shipmentDate: string
  operator: string
  trackingNo?: string
  remark?: string
  outboundType?: number
  outboundTypeName?: string
  contactName?: string
  contactPhone?: string
  salesPersonName?: string
  departmentName?: string
  shippingAddress?: string
  receiverName?: string
  receiverPhone?: string
  items?: ShipmentItem[]
}

const loading = ref(false)
const hasError = ref(false)
const dataSource = ref<Shipment[]>([])
const detailVisible = ref(false)
const trackingVisible = ref(false)
const currentRecord = ref<Shipment | null>(null)
const editMode = ref(false)
const editForm = reactive({
  remark: '',
  contactName: '',
  contactPhone: '',
  shippingAddress: '',
  receiverName: '',
  receiverPhone: ''
})
const tableRef = ref()
const lastUpdateTime = ref<string>('')
const autoRefreshCountdown = ref(0)
const autoRefreshEnabled = ref(true)
const selectedRows = ref<Shipment[]>([])
const selectedIds = ref<number[]>([])
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const searchFilters = reactive<Record<string, any>>({})
const trackingForm = reactive({
  trackingNo: '',
  carrier: ''
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

// 服务端统计（如果 API 返回）
const serverStats = reactive({
  total: 0,
  pending: 0,
  processing: 0,
  completed: 0
})

// 状态统计 - 优先使用服务端统计数据，否则从当前页计算
const statusCounts = computed(() => {
  if (serverStats.total > 0) {
    return { ...serverStats }
  }
  const pending = dataSource.value.filter(item => item.status === 0).length
  const processing = dataSource.value.filter(item => item.status === 1).length
  const completed = dataSource.value.filter(item => item.status >= 2).length
  const total = dataSource.value.length
  return { pending, processing, completed, total }
})

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})


// 详情明细
const detailDataItems = computed(() => {
  return detailData.value?.items || []
})

const vxeColumns: any = computed(() => [
  { field: 'shipmentNo', title: '出库单号', width: 150 },
  { field: 'orderNo', title: '销售订单', width: 150 },
  { field: 'customerName', title: '客户名称', width: 150 },
  { field: 'warehouseName', title: '仓库', width: 120 },
  { field: 'totalAmount', title: '出库金额', width: 130, align: 'right', formatter: ({ cellValue }) => `¥${formatAmount(cellValue)}` },
  { field: 'status', title: '状态', width: 100, align: 'center', formatter: ({ cellValue }) => SHIPMENT_STATUS[cellValue]?.text || '' },
  { field: 'shipmentDate', title: '出库日期', width: 120 },
  { field: 'operator', title: '操作人', width: 100 },
  { type: 'action', title: '操作', width: 200, fixed: 'right' },
])

const detailItemColumns: any = [
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '产品名称', dataIndex: 'productName', key: 'productName', width: 200 },
  { title: '规格', dataIndex: 'productSpec', key: 'productSpec', width: 120 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80, align: 'right' },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100, align: 'right' },
  { title: '小计', dataIndex: 'amount', key: 'amount', width: 120, align: 'right' }
]

const filterFields = [
  { key: 'shipmentNo', label: '出库单号', type: 'input' as const, placeholder: '请输入出库单号' },
  { key: 'orderNo', label: '销售订单', type: 'input' as const, placeholder: '请输入订单号' },
  { key: 'customerName', label: '客户名称', type: 'input' as const, placeholder: '请输入客户名称' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '待审核', value: 0 },
    { label: '已审核', value: 1 },
    { label: '已出库', value: 2 },
    { label: '已签收', value: 3 }
  ]}
]

const searchFields: any = [
  { name: 'shipmentNo', label: '出库单号', type: 'input', placeholder: '请输入出库单号' },
  { name: 'orderNo', label: '销售订单', type: 'input', placeholder: '请输入订单号' },
  { name: 'customerName', label: '客户名称', type: 'input', placeholder: '请输入客户名称' },
  { name: 'status', label: '状态', type: 'select', options: [
    { label: '待审核', value: 0 },
    { label: '已审核', value: 1 },
    { label: '已出库', value: 2 },
    { label: '已签收', value: 3 }
  ]},
]

const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const handleRefresh = async () => {
  lastUpdateTime.value = ''
  await fetchData()
}

const handleAutoRefreshChange = (checked: boolean) => {
  if (checked) {
    autoRefreshCountdown.value = 30
  } else {
    autoRefreshCountdown.value = 0
  }
}

const handleSearch = (values?: Record<string, any>) => {
  if (values) {
    Object.assign(searchFilters, values)
  }
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  for (const key of Object.keys(searchFilters)) {
    searchFilters[key] = undefined
  }
  pagination.current = 1
  fetchData()
}

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  fetchData()
}

const handleResetFilters = () => {
  for (const key of Object.keys(searchFilters)) {
    searchFilters[key] = undefined
  }
  pagination.current = 1
  fetchData()
}

// ── 新建出库单表单 ────────────────────────────────────
const createFormVisible = ref(false)
const createFormSubmitting = ref(false)
const createFormRef = ref<any>(null)

let tempIdCounter = 0
function nextTempId() {
  return --tempIdCounter
}

const initFormData = () => ({
  orderId: null,
  orderNo: '',
  customerId: null,
  customerName: '',
  contactId: null,
  contactName: '',
  outboundDate: dayjs(),
  outboundType: null,
  warehouseId: null,
  warehouseName: '',
  salesPersonId: null,
  salesPersonName: '',
  departmentId: null,
  departmentName: '',
  shippingAddress: '',
  receiverName: '',
  receiverPhone: '',
  remark: '',
  internalNote: '',
  items: [] as any[]
})

const createForm = reactive<Record<string, any>>(initFormData())

const formRules: Record<string, any> = {
  outboundType: [{ required: true, message: '请选择出库类型', trigger: 'change' }],
  outboundDate: [{ required: true, message: '请选择出库日期', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }]
}

// ── 仓库 ──────────────────────────────────────────────
const warehouseOptions = ref<any[]>([])
const warehouseLoading = ref(false)

async function loadWarehouses() {
  warehouseLoading.value = true
  try {
    const res = await request.get('/erp/stock/warehouses')
    warehouseOptions.value = (res.data || []).map((w: any) => ({
      value: w.id,
      label: w.name
    }))
  } catch (err) {
    console.warn('[发货管理] 加载仓库列表失败', err)
  } finally {
    warehouseLoading.value = false
  }
}

function handleWarehouseChange(val: any) {
  const found = warehouseOptions.value.find(o => o.value === val)
  createForm.warehouseName = found?.label || ''
}

// ── 客户 ──────────────────────────────────────────────
const customerOptions = ref<any[]>([])
const customerLoading = ref(false)
const contactOptions = ref<any[]>([])

async function loadCustomers() {
  customerLoading.value = true
  try {
    const res = await request.get('/erp/partner/list', {
      params: { partnerType: 'CUSTOMER', pageSize: 200 }
    })
    customerOptions.value = (res.data?.records || res.data || []).map((c: any) => ({
      value: c.id,
      label: c.name,
      customerName: c.name,
      contactId: c.contactId,
      contactName: c.contactName,
      contacts: c.contacts || []
    }))
  } catch (err) {
    console.warn('[发货管理] 加载客户列表失败', err)
  } finally {
    customerLoading.value = false
  }
}

function handleCustomerChange(val: any) {
  if (val) {
    const id = val.value ?? val
    const found = customerOptions.value.find(o => o.value === id)
    if (found) {
      createForm.customerName = found.customerName || found.label
      createForm.contactId = found.contactId || null
      createForm.contactName = found.contactName || ''
      contactOptions.value = (found.contacts || []).map((c: any) => ({
        id: c.id,
        name: c.name,
        phone: c.phone
      }))
    }
  } else {
    createForm.customerName = ''
    createForm.contactId = null
    createForm.contactName = ''
    contactOptions.value = []
  }
}

// ── 商品 ──────────────────────────────────────────────
const productOptions = ref<any[]>([])
const productLoading = ref(false)
let productSearchTimer: ReturnType<typeof setTimeout> | null = null

async function loadProducts(keyword = '') {
  productLoading.value = true
  try {
    const res = await request.get('/erp/product/list', {
      params: { keyword, pageSize: 50 }
    })
    productOptions.value = (res.data?.records || res.data || []).map((p: any) => ({
      value: p.id,
      label: p.name,
      productName: p.name,
      productCode: p.code,
      productSpec: p.spec,
      productUnit: p.unit
    }))
  } catch (err) {
    console.warn('[发货管理] 加载商品列表失败', err)
  } finally {
    productLoading.value = false
  }
}

function handleProductSearch(keyword: string) {
  if (productSearchTimer) clearTimeout(productSearchTimer)
  productSearchTimer = setTimeout(() => {
    loadProducts(keyword)
  }, 300)
}

function handleProductChange(val: any, index: number) {
  const item = createForm.items[index]
  if (!item || !val) return
  const found = productOptions.value.find(o => o.value === (val.value ?? val))
  if (found) {
    item.productId = found.value
    item.productCode = found.productCode
    item.productName = found.productName
    item.productSpec = found.productSpec
    item.productUnit = found.productUnit
  }
}

// ── 出库明细 ──────────────────────────────────────────
const itemFormColumns: any = [
  { title: '商品', key: 'product', width: 250 },
  { title: '数量', key: 'quantity', width: 100 },
  { title: '单价', key: 'unitPrice', width: 120 },
  { title: '备注', key: 'remark', width: 150 },
  { title: '操作', key: 'action', width: 80 }
]

function handleAddItem() {
  createForm.items.push({
    tempId: nextTempId(),
    productId: null,
    productCode: '',
    productName: '',
    productSpec: '',
    productUnit: '',
    orderItemId: null,
    orderQuantity: 1,
    unitPrice: 0,
    remark: ''
  })
}

function handleRemoveItem(index: number) {
  if (createForm.items.length <= 1) return
  createForm.items.splice(index, 1)
}

function resetCreateForm() {
  Object.assign(createForm, initFormData())
  createForm.items = []
  handleAddItem()
  contactOptions.value = []
  productOptions.value = []
  nextTick(() => {
    createFormRef.value?.clearValidate?.()
  })
}

const handleCreateFormCancel = () => {
  createFormVisible.value = false
}

async function submitCreateForm() {
  try {
    await createFormRef.value?.validate()
  } catch {
    return
  }
  if (!createForm.items.length || createForm.items.every((i: any) => !i.productId)) {
    message.warning('请至少添加一个出库商品')
    return
  }

  createFormSubmitting.value = true
  try {
    const payload = { ...createForm }
    if (payload.outboundDate) {
      payload.outboundDate = dayjs(payload.outboundDate).format('YYYY-MM-DD')
    }
    payload.items = payload.items.map((item: any) => {
      const { tempId, ...rest } = item
      return rest
    })
    await request.post('/erp/sale/outbound', payload)
    message.success('出库单创建成功')
    createFormVisible.value = false
    fetchData()
  } catch (error: any) {
    console.warn('[发货管理] 创建出库单失败', error)
    message.error(error?.response?.data?.message || error?.message || '创建出库单失败')
  } finally {
    createFormSubmitting.value = false
  }
}

function handleParentCreate() { handleCreate() }

const handleCreate = () => {
  createFormVisible.value = true
  resetCreateForm()
  loadWarehouses()
  loadCustomers()
}

const detailData = ref<Shipment | null>(null)
const detailLoading = ref(false)

const fetchDetail = async (id: number) => {
  detailLoading.value = true
  try {
    const res = await request.get(`/erp/sale/outbound/${id}`)
    detailData.value = res.data || null
  } catch (err) {
    console.warn('[发货管理] 获取详情失败', err)
    detailData.value = dataSource.value.find(item => item.id === id) || null
  } finally {
    detailLoading.value = false
  }
}

const handleView = (record: Shipment) => {
  editMode.value = false
  detailVisible.value = true
  fetchDetail(record.id)
}

const handleViewOrder = () => {
  if (currentRecord.value?.orderNo) {
    message.info(`查看销售订单: ${currentRecord.value.orderNo}`)
  }
}

const handleApprove = (record: Shipment) => {
  Modal.confirm({
    title: '审核确认',
    content: `确认审核出库单 ${record.shipmentNo} 吗？`,
    okText: '确认审核',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.put(`/erp/sale/outbound/${record.id}/approve`)
        message.success('审核成功')
        fetchData()
        if (detailVisible.value && detailData.value?.id === record.id) {
          fetchDetail(record.id)
        }
      } catch (error) {
        console.warn('[发货管理] 审核失败', error)
        message.error('审核失败')
      }
    }
  })
}

const handleShip = (record: Shipment) => {
  Modal.confirm({
    title: '出库确认',
    content: `确认出库单 ${record.shipmentNo} 已完成出库吗？`,
    okText: '确认出库',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.put(`/erp/sale/outbound/${record.id}/ship`)
        message.success('出库成功')
        fetchData()
        if (detailVisible.value && detailData.value?.id === record.id) {
          fetchDetail(record.id)
        }
      } catch (error) {
        console.warn('[发货管理] 出库失败', error)
        message.error('出库失败')
      }
    }
  })
}

const handlePrintSuccess = (record: Shipment) => {
  message.success(`出库单 ${record.shipmentNo} 打印成功`)
}

const handlePrintError = (error: any) => {
  message.error(`打印失败: ${error.message || '未知错误'}`)
}

const handleDelete = async (record: Shipment) => {
  try {
    await request.delete(`/erp/sale/outbound/${record.id}`)
    message.success('删除成功')
    fetchData()
  } catch (error) {
    console.warn('[发货管理] 删除失败', error)
    message.error('删除失败')
  }
}

const handleActionMenuClick = (key: any, record: Shipment) => {
  switch (key) {
    case 'edit':
      handleView(record)
      break
    case 'delete':
      Modal.confirm({
        title: '确认删除',
        content: '删除后数据不可恢复，确定要删除该出库单吗？',
        okText: '确定',
        cancelText: '取消',
        onOk: () => handleDelete(record)
      })
      break
    case 'tracking':
      currentRecord.value = record
      trackingForm.trackingNo = record.trackingNo || ''
      trackingForm.carrier = ''
      trackingVisible.value = true
      break
  }
}

const handleSaveTracking = async () => {
  if (!trackingForm.trackingNo) {
    message.warning('请输入物流单号')
    return
  }
  try {
    if (currentRecord.value) {
      await request.put(`/erp/sale/outbound/${currentRecord.value.id}/tracking`, trackingForm)
      message.success('物流单号已保存')
      trackingVisible.value = false
      fetchData()
    }
  } catch (error) {
    console.warn('[发货管理] 保存物流信息失败', error)
    message.error('保存失败')
  }
}

const handleExport = async () => {
  try {
    const res = await request.get('/erp/shipment/export', {
      params: { ...searchFilters },
      responseType: 'blob'
    })
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `出库单_${dayjs().format('YYYYMMDDHHmmss')}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    console.warn('[发货管理] 导出失败', error)
    message.error(error?.response?.data?.message || '导出失败')
  }
}

// ── 编辑模式 ──────────────────────────────────────────
const handleStartEdit = () => {
  if (detailData.value) {
    editForm.remark = detailData.value.remark || ''
    editForm.contactName = detailData.value.contactName || ''
    editForm.contactPhone = detailData.value.contactPhone || ''
    editForm.shippingAddress = detailData.value.shippingAddress || ''
    editForm.receiverName = detailData.value.receiverName || ''
    editForm.receiverPhone = detailData.value.receiverPhone || ''
  }
  editMode.value = true
}

const handleSaveEdit = async () => {
  if (!detailData.value) return
  try {
    await request.put(`/erp/sale/outbound/${detailData.value.id}`, {
      remark: editForm.remark,
      contactName: editForm.contactName,
      contactPhone: editForm.contactPhone,
      shippingAddress: editForm.shippingAddress,
      receiverName: editForm.receiverName,
      receiverPhone: editForm.receiverPhone
    })
    message.success('编辑成功')
    editMode.value = false
    fetchDetail(detailData.value.id)
    fetchData()
  } catch (error: any) {
    console.warn('[发货管理] 编辑失败', error)
    message.error(error?.response?.data?.message || '编辑失败')
  }
}

const handleCancelEdit = () => {
  editMode.value = false
}

const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

const handleSelectionChange = (rows: Shipment[], ids: number[]) => {
  selectedRows.value = rows
  selectedIds.value = ids
}

const handleKeydown = (e: KeyboardEvent) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    handleCreate()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') {
    e.preventDefault()
    handleExport()
  }
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey) {
    e.preventDefault()
    handleRefresh()
  }
}

const fetchData = async (silent = false) => {
  if (!silent) loading.value = true
  hasError.value = false
  try {
    const params: any = {
      ...searchFilters,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    const res = await request.get('/erp/sale/outbound/page', { params })
    if (res.data) {
      // 如果 API 返回了统计数据，优先使用（如 totalCount, pendingCount 等）
      if (res.totalCount !== undefined) {
        serverStats.total = res.totalCount || 0
        serverStats.pending = res.pendingCount || 0
        serverStats.processing = res.processingCount || 0
        serverStats.completed = res.completedCount || 0
      }
      if (res.records) {
        dataSource.value = res.records
        pagination.total = res.total || 0
      } else {
        dataSource.value = []
        pagination.total = 0
      }
    } else {
      dataSource.value = []
      pagination.total = 0
    }
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (error) {
    console.warn('[发货管理] 获取数据失败', error)
    if (!silent) {
      hasError.value = true
      message.error('获取数据失败')
    }
    dataSource.value = []
  } finally {
    if (!silent) loading.value = false
  }
}

onMounted(() => {
  fetchData()
  loadWarehouses()
  loadCustomers()
  window.addEventListener("erp:create", handleParentCreate)
  window.addEventListener("erp:refresh", () => fetchData())
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    if (autoRefreshEnabled.value) {
      fetchData(true)
      autoRefreshCountdown.value = 30
    }
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshEnabled.value && autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
})
</script>

<style scoped>
.shipment-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.shipment-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.shipment-page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.shipment-page-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
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
  background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
  border: 1px solid #b7eb8f;
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

.stats-summary {
  display: flex;
  gap: 16px;
  margin-right: 16px;
}

.stats-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.stats-label {
  color: #666;
}

.stats-value {
  font-weight: 500;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
}

.stats-value.pending {
  color: #faad14;
}

.stats-value.processing {
  color: #1890ff;
}

.stats-value.completed {
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

.action-more-btn {
  padding: 0 4px;
}



.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

.code-text {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-size: 13px;
}

.customer-name {
  font-weight: 500;
}

.empty-text {
  color: #999;
}

.detail-items-section {
  margin-top: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}






/* 详情弹窗表格 */
.detail-modal :deep(.ant-descriptions-item-label) {
  font-weight: 500;
  background: #fafafa;
}

/* 表格容器自动撑满 */
:deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
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
