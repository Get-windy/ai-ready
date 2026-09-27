<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        支付请求（交易 → 支付管理 → 支付请求，路由 payment/request/list，菜单 90301）
        · 定位：在线支付单统一查询/处理（只读查询 + 线下「确认收款」，无创建入口）
        · 对标：ql361 无对应页（支付请求开发文档）→ 按金标准结构实现
        · 骨架（路线 A）：CategoryListLayout + BillDetailTable（表头序号齿轮列配置：个人/全局）+ PageConfigPanel
        · 数据：`GET /api/payment/request/page`（本轮扩充：业务类型/业务单号/渠道/状态/创建时间区间）
                统计：`GET /api/payment/request/stat?channel=`（后端聚合，随查询区「支付渠道」联动）
                确认收款：`POST /api/payment/request/{id}/confirm?channelOrderNo=`
                取消支付：`POST /api/payment/request/{id}/cancel`（后端仅 `status≠2` 可取消，行级入口按同口径显示）
        · 状态机（PAYMENT_STATUS_MAP）：0 待支付 / 1 支付中 / 2 已支付 / 3 已取消 / 4 支付失败
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：口径提示 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <span class="toolbar-tip">
              支付请求由业务流程发起（无新增入口）；现金/银行转账渠道走「确认收款」
            </span>
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

        <!-- ═══ 查询区（业务类型 / 业务单号 / 渠道 / 状态 / 创建时间区间） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div v-if="isQueryVisible('bizType')" class="search-item">
                <span class="search-label">业务类型</span>
                <a-input
                  v-model:value="searchForm.bizType"
                  placeholder="如 SALE_ORDER"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('bizNo')" class="search-item">
                <span class="search-label">业务单号</span>
                <a-input
                  v-model:value="searchForm.bizNo"
                  placeholder="请输入业务单号"
                  size="small"
                  style="width: 170px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('channel')" class="search-item">
                <span class="search-label">支付渠道</span>
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
              <div v-if="isQueryVisible('status')" class="search-item">
                <span class="search-label">支付状态</span>
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
              <div v-if="isQueryVisible('payerName')" class="search-item">
                <span class="search-label">付款人</span>
                <a-input
                  v-model:value="searchForm.payerName"
                  placeholder="请输入付款人"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('timeRange')" class="search-item">
                <span class="search-label">创建时间</span>
                <a-range-picker
                  v-model:value="timeRange"
                  size="small"
                  show-time
                  value-format="YYYY-MM-DD HH:mm:ss"
                  :placeholder="['开始时间', '结束时间']"
                  style="width: 320px"
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
          <div class="request-body">
            <!-- 统计卡片（后端聚合：GET /api/payment/request/stat） -->
            <a-row :gutter="12" class="stat-row">
              <a-col :span="6">
                <div class="stat-card stat-card-blue">
                  <div class="stat-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);"><DollarOutlined /></div>
                  <div class="stat-content">
                    <div class="stat-title">待支付</div>
                    <div class="stat-value">{{ stats.pendingCount }}</div>
                    <div class="stat-desc">后端聚合（status 0/1）</div>
                  </div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-green">
                  <div class="stat-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);"><CheckCircleOutlined /></div>
                  <div class="stat-content">
                    <div class="stat-title">已支付</div>
                    <div class="stat-value">{{ stats.successCount }}</div>
                    <div class="stat-desc">后端聚合（status=2）</div>
                  </div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-red">
                  <div class="stat-icon" style="background: linear-gradient(135deg, #ff4d4f 0%, #cf1322 100%);"><CloseCircleOutlined /></div>
                  <div class="stat-content">
                    <div class="stat-title">已取消/失败</div>
                    <div class="stat-value">{{ stats.failedCount }}</div>
                    <div class="stat-desc">后端聚合（status 3/4）</div>
                  </div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-purple">
                  <div class="stat-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);"><BarChartOutlined /></div>
                  <div class="stat-content">
                    <div class="stat-title">累计金额</div>
                    <div class="stat-value">¥{{ formatAmount(stats.totalAmount) }}</div>
                    <div class="stat-desc">后端聚合</div>
                  </div>
                </div>
              </a-col>
            </a-row>

            <div class="table-area">
              <BillDetailTable
                v-model:data-source="tableData"
                :columns="columns"
                :loading="loading"
                :view-mode="true"
                :min-rows="20"
                storage-key="payment-request-table-columns"
                global-config-key="payment-request-table-columns"
              >
                <!-- 业务类型（后端 bizType 原文展示，无枚举字典） -->
                <template #bizTypeCell="{ record }">
                  {{ record.bizType || '-' }}
                </template>

                <!-- 业务单号 -->
                <template #bizNoCell="{ record }">
                  <a class="cell-link" @click="handleView(record)">{{ record.bizNo || '-' }}</a>
                </template>

                <!-- 支付金额 -->
                <template #amountCell="{ record }">
                  <span class="amount-success">¥{{ formatAmount(record.amount) }}</span>
                </template>

                <!-- 支付渠道 -->
                <template #channelCell="{ record }">
                  <a-tag :color="PAYMENT_CHANNEL_MAP[record.channel]?.color || 'default'">
                    {{ PAYMENT_CHANNEL_MAP[record.channel]?.name || record.channel || '-' }}
                  </a-tag>
                </template>

                <!-- 状态 -->
                <template #statusCell="{ record }">
                  <a-tag :color="PAYMENT_STATUS_MAP[record.status]?.color || 'default'">
                    {{ PAYMENT_STATUS_MAP[record.status]?.text || record.status }}
                  </a-tag>
                </template>

                <!-- 创建时间 -->
                <template #createTimeCell="{ record }">
                  {{ fmtTime(record.createTime) }}
                </template>

                <!-- 操作列（详情 / 确认收款 / 取消支付） -->
                <template #actionCell="{ record }">
                  <a-space v-if="!record.__ghost" :size="0">
                    <a-button type="link" size="small" @click="handleView(record)">详情</a-button>
                    <a-button
                      v-if="canConfirmOffline(record)"
                      type="link"
                      size="small"
                      @click="showConfirmModal(record)"
                    >
                      确认收款
                    </a-button>
                    <a-popconfirm
                      v-if="canCancel(record)"
                      title="确定取消该支付请求？取消后不可恢复。"
                      ok-text="确定"
                      cancel-text="再想想"
                      @confirm="handleCancel(record)"
                    >
                      <a-button
                        type="link"
                        size="small"
                        danger
                        :loading="cancellingId === record.id"
                      >
                        取消支付
                      </a-button>
                    </a-popconfirm>
                  </a-space>
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

      <!-- ═══ 确认线下收款弹窗 ═══ -->
      <a-modal
        v-model:open="confirmModalVisible"
        title="确认线下收款"
        :confirm-loading="confirming"
        @ok="handleConfirm"
      >
        <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
          <a-form-item label="收款金额">
            <span class="amount-success">¥{{ formatAmount(confirmForm.amount) }}</span>
          </a-form-item>
          <a-form-item label="收款单号" required>
            <a-input v-model:value="confirmForm.channelOrderNo" placeholder="请输入收款单号/流水号" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 详情抽屉 ═══ -->
      <a-drawer v-model:open="detailVisible" title="支付请求详情" placement="right" :width="600" :footer="null">
        <a-descriptions v-if="detailData" :column="1" bordered size="small">
          <a-descriptions-item label="支付请求ID">{{ detailData.id }}</a-descriptions-item>
          <a-descriptions-item label="业务类型">{{ detailData.bizType || '-' }}</a-descriptions-item>
          <a-descriptions-item label="业务单号">{{ detailData.bizNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="支付金额">
            <span class="amount-success">¥{{ formatAmount(detailData.amount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="支付渠道">
            <a-tag :color="PAYMENT_CHANNEL_MAP[detailData.channel]?.color || 'default'">
              {{ PAYMENT_CHANNEL_MAP[detailData.channel]?.name || detailData.channel }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="PAYMENT_STATUS_MAP[detailData.status]?.color || 'default'">
              {{ PAYMENT_STATUS_MAP[detailData.status]?.text || detailData.status }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="渠道订单号">{{ detailData.channelOrderNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="渠道交易号">{{ detailData.channelTradeNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="付款人">{{ detailData.payerName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ fmtTime(detailData.createTime) }}</a-descriptions-item>
          <a-descriptions-item label="支付完成时间">{{ fmtTime(detailData.paidTime) }}</a-descriptions-item>
          <a-descriptions-item label="过期时间">{{ fmtTime(detailData.expireTime) }}</a-descriptions-item>
          <a-descriptions-item label="备注">{{ detailData.remark || '-' }}</a-descriptions-item>
        </a-descriptions>
      </a-drawer>

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="payment-request-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="payment-request-list"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined, PrinterOutlined, DownloadOutlined, SettingOutlined,
  DollarOutlined, CheckCircleOutlined, CloseCircleOutlined, BarChartOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { paymentApi, PAYMENT_CHANNEL_MAP, PAYMENT_STATUS_MAP } from '@/api/payment'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'PaymentRequestList' })

// ═══ 常量字典 ═══
const channelOptions = Object.entries(PAYMENT_CHANNEL_MAP).map(([value, v]) => ({ value, label: v.name }))
const statusOptions = Object.entries(PAYMENT_STATUS_MAP).map(([value, v]) => ({ value: Number(value), label: v.text }))

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const timeRange = ref<[string, string] | undefined>()
const stats = reactive<Record<string, any>>({})

const searchForm = reactive<{
  bizType?: string
  bizNo?: string
  channel?: string
  status?: number
  payerName?: string
}>({})

// 序号列承载表头「列配置」齿轮；操作列为固定列（8 列 + 行号 + 操作）
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 130, fixed: 'left' },
  { key: 'bizType', title: '业务类型', type: 'slot', slotName: 'bizTypeCell', width: 130 },
  { key: 'bizNo', title: '业务单号', type: 'slot', slotName: 'bizNoCell', width: 170 },
  { key: 'amount', title: '支付金额', type: 'slot', slotName: 'amountCell', width: 120, align: 'right' },
  { key: 'channel', title: '支付渠道', type: 'slot', slotName: 'channelCell', width: 110 },
  { key: 'channelOrderNo', title: '渠道订单号', type: 'input', width: 170 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'createTime', title: '创建时间', type: 'slot', slotName: 'createTimeCell', width: 160 },
  { key: 'channelTradeNo', title: '渠道交易号', type: 'input', width: 170, defaultHidden: true },
  { key: 'payerName', title: '付款人', type: 'input', width: 110, defaultHidden: true },
  { key: 'paidTime', title: '支付完成时间', type: 'input', width: 160, defaultHidden: true },
  { key: 'expireTime', title: '过期时间', type: 'input', width: 160, defaultHidden: true }
]

// ═══ 页面配置 ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'bizType', label: '业务类型', visible: true },
  { key: 'bizNo', label: '业务单号', visible: true },
  { key: 'channel', label: '支付渠道', visible: true },
  { key: 'status', label: '支付状态', visible: true },
  { key: 'payerName', label: '付款人', visible: true },
  { key: 'timeRange', label: '创建时间', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'confirmOffline', label: '确认收款', enabled: true }
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

/** 线下渠道（现金/银行转账）且支付中可确认收款 */
function canConfirmOffline(record: any): boolean {
  return isButtonEnabled('confirmOffline')
    && record?.status === 1
    && (record?.channel === 'CASH' || record?.channel === 'BANK')
}

/**
 * 取消支付入口显示条件：`status !== 2`（与后端 `cancelPayment` 校验一致——status==2 抛「支付已完成，无法取消」）。
 * 注：3 已取消 / 4 支付失败 亦满足 `status !== 2`，后端幂等置 3，故入口仍显示。
 */
function canCancel(record: any): boolean {
  return record?.status !== 2
}

// ═══ 查询参数（业务单号 bizNo 由本轮后端 pagePaymentRequest 扩充支持） ═══
function buildQuery(): any {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize
  }
  if (searchForm.bizType) params.bizType = searchForm.bizType.trim()
  if (searchForm.bizNo) params.bizNo = searchForm.bizNo.trim()
  if (searchForm.channel) params.channel = searchForm.channel
  if (searchForm.status != null) params.status = searchForm.status
  if (searchForm.payerName) params.payerName = searchForm.payerName.trim()
  if (timeRange.value?.length === 2) {
    params.startTime = timeRange.value[0]
    params.endTime = timeRange.value[1]
  }
  return params
}

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const res: any = await paymentApi.pageRequest(buildQuery())
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[支付请求] 加载列表失败', error)
    message.error(error?.response?.data?.message || '获取支付请求失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 统计走后端聚合（`GET /api/payment/request/stat?channel=`），按查询区「支付渠道」联动 */
async function loadStats() {
  try {
    const res: any = await paymentApi.statRequest({ channel: searchForm.channel || undefined })
    Object.assign(stats, res || {})
  } catch (error) {
    console.warn('[支付请求] 统计加载失败', error)
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
  Object.assign(searchForm, { bizType: undefined, bizNo: undefined, channel: undefined, status: undefined, payerName: undefined })
  timeRange.value = undefined
  pagination.current = 1
  refreshAll()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ═══ 确认收款 ═══
const confirmModalVisible = ref(false)
const confirming = ref(false)
const confirmForm = reactive({ id: 0, amount: 0, channelOrderNo: '' })

function showConfirmModal(record: any) {
  confirmForm.id = record.id
  confirmForm.amount = record.amount
  confirmForm.channelOrderNo = ''
  confirmModalVisible.value = true
}

async function handleConfirm() {
  if (!confirmForm.channelOrderNo) {
    message.warning('请输入收款单号')
    return
  }
  confirming.value = true
  try {
    // 后端签名为 @RequestParam channelOrderNo（非请求体）→ 使用 confirmOffline
    await paymentApi.confirmOffline(confirmForm.id, confirmForm.channelOrderNo)
    message.success('确认收款成功')
    confirmModalVisible.value = false
    await refreshAll()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '确认收款失败')
  } finally {
    confirming.value = false
  }
}

// ═══ 取消支付 ═══
const cancellingId = ref<number | null>(null)

/** 取消支付请求（`POST /api/payment/request/{id}/cancel`，二次确认后调用，成功后刷新列表+统计） */
async function handleCancel(record: any) {
  cancellingId.value = record.id
  try {
    await paymentApi.cancel(record.id)
    message.success('已取消支付请求')
    await refreshAll()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '取消支付失败')
  } finally {
    cancellingId.value = null
  }
}

// ═══ 详情 ═══
const detailVisible = ref(false)
const detailData = ref<any>(null)

function handleView(record: any) {
  detailData.value = record
  detailVisible.value = true
}

function printableRows(): any[] {
  return tableData.value.filter((r: any) => !r.__ghost)
}

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML 调浏览器打印，现在交给 PrintDialog：列与行由页面给，模板负责版式。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'payment-request-list',
  title: '支付请求',
  rows: () => printableRows(),
  columns: () => columns,
  totalText: () => `业务单号：${searchForm.bizNo || '全部'} ｜ 渠道：${searchForm.channel || '全部'} ｜ 共 ${printableRows().length} 条`,
  emptyTip: '没有可打印的数据',
})

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
  const headers = ['业务类型', '业务单号', '支付金额', '支付渠道', '渠道订单号', '渠道交易号', '状态', '付款人', '创建时间', '支付完成时间']
  const csv = [
    headers.join(','),
    ...rows.map((r: any) => [
      r.bizType || '',
      r.bizNo || '',
      r.amount ?? '',
      PAYMENT_CHANNEL_MAP[r.channel]?.name || r.channel || '',
      r.channelOrderNo || '',
      r.channelTradeNo || '',
      PAYMENT_STATUS_MAP[r.status]?.text || '',
      r.payerName || '',
      r.createTime ? fmtTime(r.createTime) : '',
      r.paidTime ? fmtTime(r.paidTime) : ''
    ].map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(','))
  ].join('\r\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `支付请求_${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

function handleError(error: Error) {
  console.error('[支付请求] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
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

.request-body { flex: 1; min-height: 0; display: flex; flex-direction: column; padding: 12px 16px 0; }
.stat-row { flex-shrink: 0; margin-bottom: 12px; }
.stat-card { display: flex; align-items: center; padding: 12px 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06); }
.stat-card.stat-card-blue { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); border: 1px solid #91d5ff; }
.stat-card.stat-card-green { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); border: 1px solid #b7eb8f; }
.stat-card.stat-card-red { background: linear-gradient(135deg, #fff2f0 0%, #ffd8d2 100%); border: 1px solid #ffbcb3; }
.stat-card.stat-card-purple { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); border: 1px solid #d3adf7; }
.stat-icon { width: 42px; height: 42px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 20px; margin-right: 12px; flex-shrink: 0; }
.stat-content { flex: 1; min-width: 0; }
.stat-title { font-size: 13px; color: #666; }
.stat-value { font-size: 22px; font-weight: 600; color: #303133; font-family: 'SFMono-Regular', Consolas, monospace; }
.stat-desc { font-size: 12px; color: #999; }

/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.amount-success { color: #52c41a; font-weight: 600; font-family: 'SFMono-Regular', Consolas, monospace; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
