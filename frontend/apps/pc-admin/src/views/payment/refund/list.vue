<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        退款管理（交易 → 支付管理 → 退款管理，路由 payment/refund/list，菜单 90303）
        · 定位：在线支付**退款请求审批中心**（批准/拒绝 + 批量审批 + 详情留痕）
        · 对标：ql361 无独立退款列表页；最接近对标为「商城 → 订单处理 → 退货申请处理」（按单据/按明细 两 Tab）
          → 本页按该形态拆「按退款单 / 按退款明细」双 Tab
        · 骨架（路线 A）：CategoryListLayout（双 Tab）+ BillDetailTable（表头序号齿轮列配置，逐 Tab 独立 storage-key）+ PageConfigPanel
        · 数据：`GET /api/refund/request/page`（本轮扩充：状态/单号/退款日期区间/渠道）、`POST /request/{id}/approve`
                `GET /api/refund/record/page`（按退款明细）、`GET /api/refund/request/stat`（本轮新增后端聚合）
        · 留痕：详情补 审批人 / 审批备注 / 处理时间（processTime，迁移 V11.361.2 补列）
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧：批量批准 / 批量拒绝 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <template v-if="activeTab === 'request'">
              <a-button
                v-if="isButtonEnabled('batchApprove')"
                size="small"
                type="primary"
                :disabled="selectedRowKeys.length === 0"
                @click="handleBatchApprove(true)"
              >
                批量批准{{ selectedRowKeys.length ? ` (${selectedRowKeys.length})` : '' }}
              </a-button>
              <a-button
                v-if="isButtonEnabled('batchReject')"
                size="small"
                danger
                :disabled="selectedRowKeys.length === 0"
                @click="handleBatchApprove(false)"
              >
                批量拒绝{{ selectedRowKeys.length ? ` (${selectedRowKeys.length})` : '' }}
              </a-button>
            </template>
            <span v-else class="toolbar-tip">按退款明细（渠道回调记录）只读查询</span>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：页面配置 / 刷新 / 打印(F8) / 导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="页面配置" placement="bottom" :mouse-enter-delay="0.4">
              <a-button size="small" @click="showPageConfig = true">
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button v-if="isButtonEnabled('refresh')" size="small" :loading="loading" @click="refreshAll">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button v-if="isButtonEnabled('printF8')" size="small" @click="handlePrint">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button v-if="isButtonEnabled('export')" size="small" @click="handleExport">
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（状态 / 单号 / 退款日期区间 / 渠道） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div v-if="isQueryVisible('status')" class="search-item">
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="statusOptions"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('refundNo')" class="search-item">
                <span class="search-label">{{ activeTab === 'request' ? '单号' : '渠道退款号' }}</span>
                <a-input
                  v-model:value="searchForm.refundNo"
                  :placeholder="activeTab === 'request' ? '渠道退款号 / 支付请求ID' : '请输入渠道退款号'"
                  size="small"
                  style="width: 190px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('dateRange')" class="search-item">
                <span class="search-label">退款日期</span>
                <a-range-picker
                  v-model:value="searchForm.dateRange"
                  size="small"
                  show-time
                  value-format="YYYY-MM-DD HH:mm:ss"
                  :placeholder="['开始时间', '结束时间']"
                  style="width: 320px"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('channel')" class="search-item">
                <span class="search-label">渠道</span>
                <a-select
                  v-model:value="searchForm.channel"
                  placeholder="全部渠道"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  :options="channelOptions"
                  @change="handleSearch"
                />
              </div>
              <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
              <a-button size="small" @click="handleReset">重置</a-button>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表（含统计卡片；列配置齿轮在表头 rowNo 列） ═══ -->
        <template #table>
          <div class="refund-body">
            <!-- 统计卡片（后端聚合：GET /api/refund/request/stat） -->
            <a-row :gutter="12" class="stat-row">
              <a-col :span="6">
                <div class="stat-card stat-card-orange">
                  <div class="stat-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);"><ClockCircleOutlined /></div>
                  <div class="stat-content">
                    <div class="stat-title">待审批</div>
                    <div class="stat-value">{{ stats.pendingCount }}</div>
                    <div class="stat-desc">后端聚合（status=0）</div>
                  </div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-green">
                  <div class="stat-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);"><CheckCircleOutlined /></div>
                  <div class="stat-content">
                    <div class="stat-title">已批准</div>
                    <div class="stat-value">{{ stats.approvedCount }}</div>
                    <div class="stat-desc">后端聚合（status 1/2）</div>
                  </div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-red">
                  <div class="stat-icon" style="background: linear-gradient(135deg, #ff4d4f 0%, #cf1322 100%);"><CloseCircleOutlined /></div>
                  <div class="stat-content">
                    <div class="stat-title">已拒绝</div>
                    <div class="stat-value">{{ stats.rejectedCount }}</div>
                    <div class="stat-desc">后端聚合（status=3）</div>
                  </div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-purple">
                  <div class="stat-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);"><DollarOutlined /></div>
                  <div class="stat-content">
                    <div class="stat-title">退款金额</div>
                    <div class="stat-value">¥{{ formatAmount(stats.totalAmount) }}</div>
                    <div class="stat-desc">后端聚合</div>
                  </div>
                </div>
              </a-col>
            </a-row>

            <div class="table-area">
              <!-- ── Tab1 按退款单 ── -->
              <BillDetailTable
                v-if="activeTab === 'request'"
                v-model:data-source="tableData"
                :columns="requestColumns"
                :loading="loading"
                :view-mode="true"
                :min-rows="20"
                storage-key="payment-refund-request-table-columns"
                global-config-key="payment-refund-request-table-columns"
                @checkbox-change="handleCheckboxChange"
                @checkbox-all="handleCheckboxAll"
              >
                <!-- 退款金额 -->
                <template #amountCell="{ record }">
                  <span class="amount-danger">¥{{ formatAmount(record.amount) }}</span>
                </template>

                <!-- 状态 -->
                <template #statusCell="{ record }">
                  <a-tag :color="REFUND_STATUS_MAP[record.status]?.color || 'default'">
                    {{ REFUND_STATUS_MAP[record.status]?.text || record.status }}
                  </a-tag>
                </template>

                <!-- 创建时间 -->
                <template #createTimeCell="{ record }">
                  {{ fmtTime(record.createTime) }}
                </template>

                <!-- 处理时间 -->
                <template #processTimeCell="{ record }">
                  {{ fmtTime(record.processTime) }}
                </template>

                <!-- 操作列（详情 / 批准 / 拒绝） -->
                <template #actionCell="{ record }">
                  <a-space v-if="!record.__ghost" :size="0">
                    <a-button type="link" size="small" @click="handleView(record)">详情</a-button>
                    <a-button
                      v-if="record.status === 0 && isButtonEnabled('approve')"
                      type="link"
                      size="small"
                      @click="handleApprove(record, true)"
                    >
                      批准
                    </a-button>
                    <a-button
                      v-if="record.status === 0 && isButtonEnabled('reject')"
                      type="link"
                      size="small"
                      danger
                      @click="handleApprove(record, false)"
                    >
                      拒绝
                    </a-button>
                  </a-space>
                </template>
              </BillDetailTable>

              <!-- ── Tab2 按退款明细（渠道回调记录，只读） ── -->
              <BillDetailTable
                v-else
                v-model:data-source="recordData"
                :columns="recordColumns"
                :loading="loading"
                :view-mode="true"
                :min-rows="20"
                storage-key="payment-refund-record-table-columns"
                global-config-key="payment-refund-record-table-columns"
              >
                <template #channelCell="{ record }">
                  <a-tag :color="PAYMENT_CHANNEL_MAP[record.channel]?.color || 'default'">
                    {{ PAYMENT_CHANNEL_MAP[record.channel]?.name || record.channel || '-' }}
                  </a-tag>
                </template>

                <template #recordStatusCell="{ record }">
                  <!-- refund_record.status：0 处理中 / 1 成功 / 2 失败 -->
                  <a-tag :color="REFUND_RECORD_STATUS_MAP[record.status]?.color || 'default'">
                    {{ REFUND_RECORD_STATUS_MAP[record.status]?.text || record.status }}
                  </a-tag>
                </template>

                <template #recordAmountCell="{ record }">
                  <span class="amount-danger">¥{{ formatAmount(record.amount) }}</span>
                </template>

                <template #callbackTimeCell="{ record }">
                  {{ fmtTime(record.callbackTime) }}
                </template>

                <template #recordActionCell="{ record }">
                  <a-button v-if="!record.__ghost" type="link" size="small" @click="showRecordRaw(record)">
                    查看回调
                  </a-button>
                </template>
              </BillDetailTable>
            </div>
          </div>
        </template>

        <!-- ═══ 底部：经典分页栏 ═══ -->
        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 退款详情抽屉（补审批人 / 审批备注 / 处理时间） ═══ -->
      <a-drawer v-model:open="detailVisible" title="退款详情" placement="right" :width="620" :footer="null">
        <a-descriptions v-if="detailData" :column="1" bordered size="small">
          <a-descriptions-item label="退款单ID">{{ detailData.id }}</a-descriptions-item>
          <a-descriptions-item label="支付请求ID">{{ detailData.paymentId ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="退款金额">
            <span class="amount-danger">¥{{ formatAmount(detailData.amount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="退款原因">{{ detailData.reason || '-' }}</a-descriptions-item>
          <a-descriptions-item label="申请人">{{ detailData.applicantName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="REFUND_STATUS_MAP[detailData.status]?.color || 'default'">
              {{ REFUND_STATUS_MAP[detailData.status]?.text || detailData.status }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="渠道退款号">{{ detailData.channelRefundNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="审批人">
            {{ detailData.approverName || detailData.approverId || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="审批备注">{{ detailData.approveRemark || '-' }}</a-descriptions-item>
          <a-descriptions-item label="处理时间">{{ fmtTime(detailData.processTime) }}</a-descriptions-item>
          <a-descriptions-item label="退款完成时间">{{ fmtTime(detailData.refundedTime) }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ fmtTime(detailData.createTime) }}</a-descriptions-item>
        </a-descriptions>
      </a-drawer>

      <!-- ═══ 退款回调原始数据弹窗 ═══ -->
      <a-modal v-model:open="rawModalVisible" title="退款回调数据" :width="700" :footer="null">
        <pre class="json-view">{{ rawJsonData }}</pre>
      </a-modal>

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="`payment-refund-page-config-${activeTab}`"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import dayjs from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import {
  ReloadOutlined, PrinterOutlined, DownloadOutlined, SettingOutlined,
  DollarOutlined, CheckCircleOutlined, CloseCircleOutlined, ClockCircleOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { refundApi, PAYMENT_CHANNEL_MAP, REFUND_STATUS_MAP } from '@/api/payment'

defineOptions({ name: 'PaymentRefundList' })

// ═══ 双 Tab（对标 ql361 退货申请处理「按单据 / 按明细」） ═══
const TABS = [
  { key: 'request', label: '按退款单' },
  { key: 'record', label: '按退款明细' }
]

// ═══ 常量字典 ═══
const channelOptions = Object.entries(PAYMENT_CHANNEL_MAP).map(([value, v]) => ({ value, label: v.name }))
const statusOptions = Object.entries(REFUND_STATUS_MAP).map(([value, v]) => ({ value: Number(value), label: v.text }))
/** 退款明细（回调记录）状态：与 refund_record.status 注释一致 */
const REFUND_RECORD_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '处理中', color: 'processing' },
  1: { text: '成功', color: 'success' },
  2: { text: '失败', color: 'error' }
}

// ═══ 状态 ═══
const activeTab = ref('request')
const loading = ref(false)
const tableData = ref<any[]>([])
const recordData = ref<any[]>([])
const selectedRowKeys = ref<number[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const stats = reactive<Record<string, any>>({})

const searchForm = reactive<{
  status?: number
  refundNo?: string
  dateRange?: [string, string]
  channel?: string
}>({})

// ── Tab1 列（8 列 + 行号 + 勾选 + 操作） ──
const requestColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 150, fixed: 'left' },
  { key: 'paymentId', title: '支付请求ID', type: 'input', width: 110 },
  { key: 'amount', title: '退款金额', type: 'slot', slotName: 'amountCell', width: 120, align: 'right' },
  { key: 'reason', title: '退款原因', type: 'input', width: 200 },
  { key: 'applicantName', title: '申请人', type: 'input', width: 110 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'channelRefundNo', title: '渠道退款号', type: 'input', width: 170 },
  { key: 'createTime', title: '创建时间', type: 'slot', slotName: 'createTimeCell', width: 160 },
  { key: 'processTime', title: '处理时间', type: 'slot', slotName: 'processTimeCell', width: 160, defaultHidden: true },
  { key: 'approveRemark', title: '审批备注', type: 'input', width: 200, defaultHidden: true },
  { key: 'approverId', title: '审批人ID', type: 'input', width: 110, defaultHidden: true },
  { key: 'refundedTime', title: '退款完成时间', type: 'input', width: 160, defaultHidden: true }
]

// ── Tab2 列（refund_record：渠道回调明细） ──
const recordColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'recordActionCell', width: 100, fixed: 'left' },
  { key: 'requestId', title: '退款单ID', type: 'input', width: 110 },
  { key: 'channel', title: '渠道', type: 'slot', slotName: 'channelCell', width: 110 },
  { key: 'channelRefundNo', title: '渠道退款号', type: 'input', width: 180 },
  { key: 'amount', title: '退款金额', type: 'slot', slotName: 'recordAmountCell', width: 120, align: 'right' },
  { key: 'status', title: '状态', type: 'slot', slotName: 'recordStatusCell', width: 100 },
  { key: 'callbackTime', title: '回调时间', type: 'slot', slotName: 'callbackTimeCell', width: 160 },
  { key: 'errorCode', title: '错误码', type: 'input', width: 120, defaultHidden: true },
  { key: 'errorMsg', title: '错误信息', type: 'input', width: 220, defaultHidden: true }
]

// ═══ 页面配置（逐 Tab 独立勾选与持久化） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'status', label: '状态', visible: true },
  { key: 'refundNo', label: '单号', visible: true },
  { key: 'dateRange', label: '退款日期', visible: true },
  { key: 'channel', label: '渠道', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'approve', label: '批准', enabled: true },
  { key: 'reject', label: '拒绝', enabled: true },
  { key: 'batchApprove', label: '批量批准', enabled: true },
  { key: 'batchReject', label: '批量拒绝', enabled: true }
]
const queryFields = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtons = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))
const showPageConfig = ref(false)

function isQueryVisible(key: string): boolean {
  const hit = queryFields.value.find(f => f.key === key)
  return hit ? hit.visible : false
}

function isButtonEnabled(key: string): boolean {
  const hit = functionButtons.value.find(b => b.key === key)
  return hit ? hit.enabled : true
}

function handlePageConfigChange(config: { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }) {
  if (config?.queryFields) queryFields.value = config.queryFields.map(f => ({ ...f }))
  if (config?.functionButtons) functionButtons.value = config.functionButtons.map(f => ({ ...f }))
}

// ═══ 格式化 ═══
function formatAmount(val: number | undefined | null): string {
  return (val ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function fmtTime(val?: string | null): string {
  if (!val) return '-'
  return dayjs(val).format('YYYY-MM-DD HH:mm:ss')
}

// ═══ 查询参数 ═══
function buildQuery(): any {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize
  }
  if (searchForm.status != null) params.status = searchForm.status
  if (searchForm.refundNo) params.refundNo = searchForm.refundNo.trim()
  if (searchForm.channel) params.channel = searchForm.channel
  if (searchForm.dateRange?.length === 2) {
    params.startTime = searchForm.dateRange[0]
    params.endTime = searchForm.dateRange[1]
  }
  return params
}

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    if (activeTab.value === 'request') {
      const res: any = await refundApi.pageRequest(buildQuery())
      tableData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
      selectedRowKeys.value = []
    } else {
      // 退款明细：后端 `/api/refund/record/page` 仅支持 channel 过滤
      // （状态/单号/退款日期在退款单上，明细表无这些列 → 不传，见「退款管理开发文档 → 待完善」）
      const params: any = {
        pageNum: pagination.current,
        pageSize: pagination.pageSize
      }
      if (searchForm.channel) params.channel = searchForm.channel
      const res: any = await refundApi.pageRecord(params)
      recordData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    }
  } catch (error: any) {
    console.error('[退款管理] 加载列表失败', error)
    message.error(error?.response?.data?.message || '获取退款列表失败')
    if (activeTab.value === 'request') tableData.value = []
    else recordData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 统计走后端聚合（`GET /api/refund/request/stat`，按退款单口径） */
async function loadStats() {
  try {
    const params: Record<string, any> = {}
    if (searchForm.channel) params.channel = searchForm.channel
    const res: any = await refundApi.statRequest(params)
    Object.assign(stats, res || {})
  } catch (error) {
    console.warn('[退款管理] 统计加载失败', error)
  }
}

async function refreshAll() {
  await Promise.all([fetchData(), loadStats()])
}

function handleSearch() {
  pagination.current = 1
  refreshAll()
}

function handleReset() {
  Object.assign(searchForm, { status: undefined, refundNo: undefined, dateRange: undefined, channel: undefined })
  pagination.current = 1
  refreshAll()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

function handleTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
  pagination.pageSize = 20
  selectedRowKeys.value = []
  tableData.value = []
  recordData.value = []
  // 切 Tab 必须重载「列配置」与「页面配置」：列配置 storage-key 已在模板按 activeTab 切换；
  // 页面配置（queryFields/functionButtons）在此处按新 Tab 的存储键重载
  loadPageConfigFromStorage()
  fetchData()
}

const QUERY_FIELDS_STORAGE = () => `payment-refund-page-config-${activeTab.value}`

/** 从 localStorage 读取当前 Tab 的页面配置（与 PageConfigPanel 同键同结构） */
function loadPageConfigFromStorage() {
  queryFields.value = DEFAULT_QUERY_FIELDS.map(f => ({ ...f }))
  functionButtons.value = DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f }))
  try {
    const raw = localStorage.getItem(QUERY_FIELDS_STORAGE())
    if (!raw) return
    const parsed = JSON.parse(raw)
    const storedQuery = parsed?.queryFields
    const storedButtons = parsed?.functionButtons
    if (Array.isArray(storedQuery) && storedQuery.length) {
      queryFields.value = storedQuery.map((f: any) => ({
        key: f.key,
        label: f.label,
        visible: f.visible !== false
      }))
    }
    if (Array.isArray(storedButtons) && storedButtons.length) {
      functionButtons.value = storedButtons.map((b: any) => ({
        key: b.key,
        label: b.label,
        enabled: b.enabled !== false
      }))
    }
  } catch (error) {
    console.warn('[退款管理] 页面配置读取失败，已回落默认', error)
  }
}

// ═══ 勾选 / 审批 ═══
function handleCheckboxChange(record: any, _rowIndex: number, checked: boolean) {
  const id = record?.id
  if (id == null) return
  if (checked) {
    if (!selectedRowKeys.value.includes(id)) selectedRowKeys.value.push(id)
  } else {
    selectedRowKeys.value = selectedRowKeys.value.filter(key => key !== id)
  }
}

function handleCheckboxAll(_checked: boolean, records: any[]) {
  selectedRowKeys.value = (records || []).map((r: any) => r.id).filter((id: any) => id != null)
}

function handleApprove(record: any, approved: boolean) {
  Modal.confirm({
    title: approved ? '确认批准' : '确认拒绝',
    content: approved
      ? `确定批准该退款请求（¥${formatAmount(record.amount)}）？`
      : `确定拒绝该退款请求（¥${formatAmount(record.amount)}）？`,
    okType: approved ? undefined : 'danger',
    onOk: async () => {
      try {
        await refundApi.approve(record.id, approved)
        message.success(approved ? '已批准' : '已拒绝')
        await refreshAll()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '操作失败')
      }
    }
  })
}

function handleBatchApprove(approved: boolean) {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先勾选退款请求')
    return
  }
  const count = selectedRowKeys.value.length
  Modal.confirm({
    title: approved ? '批量批准' : '批量拒绝',
    content: `确定${approved ? '批准' : '拒绝'}选中的 ${count} 个退款请求？`,
    okType: approved ? undefined : 'danger',
    onOk: async () => {
      try {
        for (const id of selectedRowKeys.value) {
          await refundApi.approve(id, approved)
        }
        message.success(`批量${approved ? '批准' : '拒绝'}成功（${count} 条）`)
        selectedRowKeys.value = []
        await refreshAll()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '批量操作失败')
        await refreshAll()
      }
    }
  })
}

// ═══ 详情 / 回调数据 ═══
const detailVisible = ref(false)
const detailData = ref<any>(null)

function handleView(record: any) {
  detailData.value = record
  detailVisible.value = true
}

const rawModalVisible = ref(false)
const rawJsonData = ref('')

function showRecordRaw(record: any) {
  try {
    rawJsonData.value = JSON.stringify(JSON.parse(record.callbackData), null, 2)
  } catch {
    rawJsonData.value = record.callbackData || '（无回调数据）'
  }
  rawModalVisible.value = true
}

// ═══ 打印(F8)：真实打印模板（与列表同口径） ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function printableRows(): any[] {
  const source = activeTab.value === 'request' ? tableData.value : recordData.value
  return source.filter((r: any) => !r.__ghost)
}

function handlePrint() {
  const rows = printableRows()
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const isRequest = activeTab.value === 'request'
  const body = isRequest
    ? rows.map((r: any, i: number) => `
      <tr>
        <td>${i + 1}</td>
        <td>${escapeHtml(r.paymentId ?? '')}</td>
        <td style="text-align:right">${formatAmount(r.amount)}</td>
        <td>${escapeHtml(r.reason || '')}</td>
        <td>${escapeHtml(r.applicantName || '')}</td>
        <td>${escapeHtml(REFUND_STATUS_MAP[r.status]?.text || '')}</td>
        <td>${escapeHtml(r.channelRefundNo || '')}</td>
        <td>${escapeHtml(fmtTime(r.createTime))}</td>
      </tr>`).join('')
    : rows.map((r: any, i: number) => `
      <tr>
        <td>${i + 1}</td>
        <td>${escapeHtml(r.requestId ?? '')}</td>
        <td>${escapeHtml(PAYMENT_CHANNEL_MAP[r.channel]?.name || r.channel || '')}</td>
        <td>${escapeHtml(r.channelRefundNo || '')}</td>
        <td style="text-align:right">${formatAmount(r.amount)}</td>
        <td>${escapeHtml(REFUND_RECORD_STATUS_MAP[r.status]?.text || '')}</td>
        <td>${escapeHtml(fmtTime(r.callbackTime))}</td>
      </tr>`).join('')
  const head = isRequest
    ? '<tr><th>#</th><th>支付请求ID</th><th>退款金额</th><th>退款原因</th><th>申请人</th><th>状态</th><th>渠道退款号</th><th>创建时间</th></tr>'
    : '<tr><th>#</th><th>退款单ID</th><th>渠道</th><th>渠道退款号</th><th>退款金额</th><th>状态</th><th>回调时间</th></tr>'
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>退款管理</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>退款管理 · ${isRequest ? '按退款单' : '按退款明细'}</h2>
    <div class="meta">
      <span>渠道：${escapeHtml(searchForm.channel || '全部')}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table><thead>${head}</thead><tbody>${body}</tbody></table></body></html>`
  const win = window.open('', '_blank', 'width=1100,height=700')
  if (!win) {
    message.warning('浏览器阻止了打印窗口，请允许弹出窗口后重试')
    return
  }
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

// ═══ 导出（CSV，前端拼装） ═══
function handleExport() {
  const rows = printableRows()
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const isRequest = activeTab.value === 'request'
  const headers = isRequest
    ? ['支付请求ID', '退款金额', '退款原因', '申请人', '状态', '渠道退款号', '审批人ID', '审批备注', '处理时间', '创建时间']
    : ['退款单ID', '渠道', '渠道退款号', '退款金额', '状态', '回调时间', '错误码', '错误信息']
  const csv = [
    headers.join(','),
    ...rows.map((r: any) => (isRequest
      ? [
        r.paymentId ?? '', r.amount ?? '', r.reason || '', r.applicantName || '',
        REFUND_STATUS_MAP[r.status]?.text || '', r.channelRefundNo || '',
        r.approverId ?? '', r.approveRemark || '',
        r.processTime ? fmtTime(r.processTime) : '', r.createTime ? fmtTime(r.createTime) : ''
      ]
      : [
        r.requestId ?? '', PAYMENT_CHANNEL_MAP[r.channel]?.name || r.channel || '',
        r.channelRefundNo || '', r.amount ?? '',
        REFUND_RECORD_STATUS_MAP[r.status]?.text || '',
        r.callbackTime ? fmtTime(r.callbackTime) : '', r.errorCode || '', r.errorMsg || ''
      ]).map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(','))
  ].join('\r\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `退款管理_${isRequest ? '按退款单' : '按退款明细'}_${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

function handleError(error: Error) {
  console.error('[退款管理] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadPageConfigFromStorage()
  refreshAll()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.toolbar-tip { font-size: 12px; color: #8c8c8c; }

.refund-body { flex: 1; min-height: 0; display: flex; flex-direction: column; padding: 12px 16px 0; }
.stat-row { flex-shrink: 0; margin-bottom: 12px; }
.stat-card { display: flex; align-items: center; padding: 12px 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06); }
.stat-card.stat-card-green { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); border: 1px solid #b7eb8f; }
.stat-card.stat-card-red { background: linear-gradient(135deg, #fff2f0 0%, #ffd8d2 100%); border: 1px solid #ffbcb3; }
.stat-card.stat-card-orange { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); border: 1px solid #ffd591; }
.stat-card.stat-card-purple { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); border: 1px solid #d3adf7; }
.stat-icon { width: 42px; height: 42px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 20px; margin-right: 12px; flex-shrink: 0; }
.stat-content { flex: 1; min-width: 0; }
.stat-title { font-size: 13px; color: #666; }
.stat-value { font-size: 22px; font-weight: 600; color: #303133; font-family: 'SFMono-Regular', Consolas, monospace; }
.stat-desc { font-size: 12px; color: #999; }

/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

.amount-danger { color: #f5222d; font-weight: 600; font-family: 'SFMono-Regular', Consolas, monospace; }
.json-view {
  margin: 0; padding: 10px; max-height: 400px; overflow: auto;
  background: #f6f8fa; border: 1px solid #eee; border-radius: 4px;
  font-size: 12px; line-height: 1.6; white-space: pre-wrap; word-break: break-all;
}

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
