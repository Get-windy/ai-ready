<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        每日对账（交易 → 支付管理 → 支付对账，路由 payment/reconciliation/daily，菜单 90304）
        · 定位：在线支付**渠道路由对账**（执行日对账 → 产出总/成/差笔数与金额 → 处理差异）
        · 对标：ql361「财务 → 收款 → 在线支付对账单」（形态/列配置/查询/按钮可对标；字段口径不同）
        · 骨架（路线 A）：CategoryListLayout + BillDetailTable（表头序号齿轮列配置：个人/全局）+ PageConfigPanel
        · 数据：`GET /api/reconciliation/page`、`POST /{id}/handle`（本轮补 method 落库）、`GET /stat`（本轮新增后端聚合）
        · 状态机对齐：RECON_STATUS_MAP 补 status 3「处理中」（0 待对账 / 1 已对账 / 2 有差异 / 3 处理中）
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：执行对账 ═══ -->
        <template #toolbar-left>
          <a-button v-if="isButtonEnabled('execute')" type="primary" size="small" @click="showExecuteModal">
            <PlayCircleOutlined /> 执行对账
          </a-button>
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

        <!-- ═══ 查询区（渠道 / 状态 / 对账日期区间） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
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
              <div v-if="isQueryVisible('dateRange')" class="search-item">
                <span class="search-label">对账日期</span>
                <a-range-picker
                  v-model:value="searchForm.dateRange"
                  size="small"
                  value-format="YYYY-MM-DD"
                  :placeholder="['开始日期', '结束日期']"
                  style="width: 240px"
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
          <div class="recon-body">
            <!-- 统计卡片（后端聚合：GET /api/reconciliation/stat） -->
            <a-row :gutter="12" class="stat-row">
              <a-col :span="6">
                <div class="stat-card stat-card-green">
                  <div class="stat-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);"><CheckCircleOutlined /></div>
                  <div class="stat-content">
                    <div class="stat-title">已对平</div>
                    <div class="stat-value">{{ stats.matchedCount }}</div>
                    <div class="stat-desc">后端聚合（status=1）</div>
                  </div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-orange">
                  <div class="stat-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);"><WarningOutlined /></div>
                  <div class="stat-content">
                    <div class="stat-title">有差异</div>
                    <div class="stat-value">{{ stats.diffCount }}</div>
                    <div class="stat-desc">后端聚合（status=2）</div>
                  </div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-blue">
                  <div class="stat-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);"><SyncOutlined /></div>
                  <div class="stat-content">
                    <div class="stat-title">处理中</div>
                    <div class="stat-value">{{ stats.processingCount }}</div>
                    <div class="stat-desc">后端聚合（status=3）</div>
                  </div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-purple">
                  <div class="stat-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);"><DollarOutlined /></div>
                  <div class="stat-content">
                    <div class="stat-title">差异金额</div>
                    <div class="stat-value">¥{{ formatAmount(stats.diffAmount) }}</div>
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
                storage-key="payment-reconciliation-table-columns"
                global-config-key="payment-reconciliation-table-columns"
              >
                <!-- 对账日期 -->
                <template #reconcileDateCell="{ record }">
                  {{ record.reconcileDate || '-' }}
                </template>

                <!-- 渠道 -->
                <template #channelCell="{ record }">
                  <a-tag :color="PAYMENT_CHANNEL_MAP[record.channel]?.color || 'default'">
                    {{ PAYMENT_CHANNEL_MAP[record.channel]?.name || record.channel || '-' }}
                  </a-tag>
                </template>

                <!-- 总金额 -->
                <template #totalAmountCell="{ record }">
                  <span class="amount-plain">¥{{ formatAmount(record.totalAmount) }}</span>
                </template>

                <!-- 成功金额 -->
                <template #successAmountCell="{ record }">
                  <span class="amount-success">¥{{ formatAmount(record.successAmount) }}</span>
                </template>

                <!-- 差异金额 -->
                <template #diffAmountCell="{ record }">
                  <span v-if="Number(record.diffAmount) > 0" class="amount-danger">¥{{ formatAmount(record.diffAmount) }}</span>
                  <span v-else class="text-muted">-</span>
                </template>

                <!-- 状态 -->
                <template #statusCell="{ record }">
                  <a-tag :color="RECON_STATUS_MAP[record.status]?.color || 'default'">
                    {{ RECON_STATUS_MAP[record.status]?.text || record.status }}
                  </a-tag>
                </template>

                <!-- 操作列（详情 / 处理差异） -->
                <template #actionCell="{ record }">
                  <a-space v-if="!record.__ghost" :size="0">
                    <a-button type="link" size="small" @click="handleView(record)">详情</a-button>
                    <a-button
                      v-if="record.status === 2 && isButtonEnabled('handleDiff')"
                      type="link"
                      size="small"
                      @click="showHandleModal(record)"
                    >
                      处理差异
                    </a-button>
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

      <!-- ═══ 执行对账弹窗 ═══ -->
      <a-modal
        v-model:open="executeModalVisible"
        title="执行日对账"
        :confirm-loading="executing"
        @ok="handleExecute"
      >
        <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
          <a-form-item label="对账日期" required>
            <a-date-picker v-model:value="executeForm.date" value-format="YYYY-MM-DD" style="width: 100%" />
          </a-form-item>
          <a-form-item label="渠道">
            <a-select
              v-model:value="executeForm.channel"
              placeholder="全部渠道"
              allow-clear
              :options="channelOptions"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 处理差异弹窗（method 已落后端：MANUAL/IGNORE/REPROCESS） ═══ -->
      <a-modal
        v-model:open="handleModalVisible"
        title="处理差异"
        :confirm-loading="handling"
        @ok="handleDifference"
      >
        <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
          <a-form-item label="差异金额">
            <span v-if="Number(handleForm.diffAmount) > 0" class="amount-danger">¥{{ formatAmount(handleForm.diffAmount) }}</span>
            <span v-else class="text-muted">¥0.00</span>
          </a-form-item>
          <a-form-item label="处理方式" required>
            <a-radio-group v-model:value="handleForm.method">
              <a-radio value="MANUAL">手工调账</a-radio>
              <a-radio value="IGNORE">忽略差异</a-radio>
              <a-radio value="REPROCESS">重新对账</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="处理备注" required>
            <a-textarea v-model:value="handleForm.remark" placeholder="请输入处理备注" :rows="3" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 对账详情抽屉（含差异明细子表） ═══ -->
      <a-drawer v-model:open="detailVisible" title="对账详情" placement="right" :width="760" :footer="null">
        <a-descriptions v-if="detailData" :column="1" bordered size="small">
          <a-descriptions-item label="对账日期">{{ detailData.reconcileDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="渠道">
            <a-tag :color="PAYMENT_CHANNEL_MAP[detailData.channel]?.color || 'default'">
              {{ PAYMENT_CHANNEL_MAP[detailData.channel]?.name || detailData.channel }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="总笔数 / 总金额">
            {{ detailData.totalCount ?? 0 }} 笔 / ¥{{ formatAmount(detailData.totalAmount) }}
          </a-descriptions-item>
          <a-descriptions-item label="成功笔数 / 成功金额">
            {{ detailData.successCount ?? 0 }} 笔 / <span class="amount-success">¥{{ formatAmount(detailData.successAmount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="差异笔数 / 差异金额">
            {{ detailData.diffCount ?? 0 }} 笔 / <span class="amount-danger">¥{{ formatAmount(detailData.diffAmount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="RECON_STATUS_MAP[detailData.status]?.color || 'default'">
              {{ RECON_STATUS_MAP[detailData.status]?.text || detailData.status }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="对账时间">{{ fmtTime(detailData.reconciledTime) }}</a-descriptions-item>
          <a-descriptions-item label="处理方式">{{ HANDLE_METHOD_MAP[detailData.handleMethod] || '-' }}</a-descriptions-item>
          <a-descriptions-item label="备注">{{ detailData.remark || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ fmtTime(detailData.createTime) }}</a-descriptions-item>
        </a-descriptions>

        <a-divider>差异明细</a-divider>
        <!-- 差异明细取自对账详情返回的 diffRecords 字段 -->
        <a-table
          :columns="diffColumns"
          :data-source="detailData?.diffRecords || []"
          :pagination="false"
          size="small"
          row-key="id"
          :locale="{ emptyText: '暂无差异明细' }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'ourAmount'">
              ¥{{ formatAmount(record.ourAmount) }}
            </template>
            <template v-else-if="column.key === 'channelAmount'">
              ¥{{ formatAmount(record.channelAmount) }}
            </template>
            <template v-else-if="column.key === 'amount'">
              <span class="amount-danger">¥{{ formatAmount(record.amount) }}</span>
            </template>
          </template>
        </a-table>
      </a-drawer>

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="payment-reconciliation-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="payment-reconciliation-daily"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined, PrinterOutlined, DownloadOutlined, SettingOutlined, PlayCircleOutlined,
  DollarOutlined, CheckCircleOutlined, WarningOutlined, SyncOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { reconciliationApi, PAYMENT_CHANNEL_MAP, RECON_STATUS_MAP } from '@/api/payment'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'PaymentReconciliationDaily' })

// ═══ 常量字典 ═══
const channelOptions = Object.entries(PAYMENT_CHANNEL_MAP).map(([value, v]) => ({ value, label: v.name }))
const statusOptions = Object.entries(RECON_STATUS_MAP).map(([value, v]) => ({ value: Number(value), label: v.text }))
/** 差异处理方式（与后端 handle_method 值域一致：MANUAL / IGNORE / REPROCESS） */
const HANDLE_METHOD_MAP: Record<string, string> = {
  MANUAL: '手工调账',
  IGNORE: '忽略差异',
  REPROCESS: '重新对账'
}

// ═══ 状态 ═══
const loading = ref(false)
const executing = ref(false)
const handling = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const stats = reactive<Record<string, any>>({})

const searchForm = reactive<{
  channel?: string
  status?: number
  dateRange?: [string, string]
}>({})

// 序号列承载表头「列配置」齿轮；操作列为固定列（10 列 + 行号 + 操作）
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 150, fixed: 'left' },
  { key: 'reconcileDate', title: '对账日期', type: 'slot', slotName: 'reconcileDateCell', width: 110 },
  { key: 'channel', title: '渠道', type: 'slot', slotName: 'channelCell', width: 110 },
  { key: 'totalCount', title: '总笔数', type: 'input', width: 90, align: 'right' },
  { key: 'totalAmount', title: '总金额', type: 'slot', slotName: 'totalAmountCell', width: 120, align: 'right' },
  { key: 'successCount', title: '成功笔数', type: 'input', width: 90, align: 'right' },
  { key: 'successAmount', title: '成功金额', type: 'slot', slotName: 'successAmountCell', width: 120, align: 'right' },
  { key: 'diffCount', title: '差异笔数', type: 'input', width: 90, align: 'right' },
  { key: 'diffAmount', title: '差异金额', type: 'slot', slotName: 'diffAmountCell', width: 120, align: 'right' },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'handleMethod', title: '处理方式', type: 'input', width: 110, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 200, defaultHidden: true },
  { key: 'reconciledTime', title: '对账时间', type: 'input', width: 120, defaultHidden: true },
  { key: 'createTime', title: '创建时间', type: 'input', width: 160, defaultHidden: true }
]

/** 差异明细子表列（对应后端差异记录结构：type/ourAmount/channelAmount/amount/remark） */
const diffColumns = [
  { title: '类型', dataIndex: 'type', key: 'type', width: 110 },
  { title: '我方金额', dataIndex: 'ourAmount', key: 'ourAmount', width: 120, align: 'right' as const },
  { title: '渠道金额', dataIndex: 'channelAmount', key: 'channelAmount', width: 120, align: 'right' as const },
  { title: '差异金额', dataIndex: 'amount', key: 'amount', width: 120, align: 'right' as const },
  { title: '说明', dataIndex: 'remark', key: 'remark', width: 200 }
]

// ═══ 页面配置 ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'channel', label: '渠道', visible: true },
  { key: 'status', label: '状态', visible: true },
  { key: 'dateRange', label: '对账日期', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'execute', label: '执行对账', enabled: true },
  { key: 'handleDiff', label: '处理差异', enabled: true }
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
  // reconciledTime 为 LocalDate（yyyy-MM-dd），createTime 为时间戳
  return String(val).length <= 10 ? String(val) : dayjs(val).format('YYYY-MM-DD HH:mm:ss')
}

// ═══ 查询参数 ═══
function buildQuery(): any {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize
  }
  if (searchForm.channel) params.channel = searchForm.channel
  if (searchForm.status != null) params.status = searchForm.status
  if (searchForm.dateRange?.length === 2) {
    params.startDate = searchForm.dateRange[0]
    params.endDate = searchForm.dateRange[1]
  }
  return params
}

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const res: any = await reconciliationApi.page(buildQuery())
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[每日对账] 加载列表失败', error)
    message.error(error?.response?.data?.message || '获取对账数据失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 统计走后端聚合（`GET /api/reconciliation/stat`），随对账日期区间与渠道联动 */
async function loadStats() {
  try {
    const params: Record<string, any> = {}
    if (searchForm.channel) params.channel = searchForm.channel
    if (searchForm.dateRange?.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const res: any = await reconciliationApi.stat(params)
    Object.assign(stats, res || {})
  } catch (error) {
    console.warn('[每日对账] 统计加载失败', error)
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
  Object.assign(searchForm, { channel: undefined, status: undefined, dateRange: undefined })
  pagination.current = 1
  refreshAll()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ═══ 执行对账 ═══
const executeModalVisible = ref(false)
const executeForm = reactive<{ date: string; channel?: string }>({ date: dayjs().format('YYYY-MM-DD'), channel: undefined })

function showExecuteModal() {
  executeForm.date = dayjs().format('YYYY-MM-DD')
  executeForm.channel = undefined
  executeModalVisible.value = true
}

async function handleExecute() {
  if (!executeForm.date) {
    message.warning('请选择对账日期')
    return
  }
  executing.value = true
  try {
    await reconciliationApi.execute(executeForm.date, executeForm.channel)
    message.success('对账执行完成')
    executeModalVisible.value = false
    await refreshAll()
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '对账执行失败')
  } finally {
    executing.value = false
  }
}

// ═══ 处理差异（method 落后端） ═══
const handleModalVisible = ref(false)
const handleForm = reactive({ id: 0, diffAmount: 0, method: 'MANUAL', remark: '' })

function showHandleModal(record: any) {
  handleForm.id = record.id
  handleForm.diffAmount = record.diffAmount || 0
  handleForm.method = 'MANUAL'
  handleForm.remark = ''
  handleModalVisible.value = true
}

async function handleDifference() {
  if (!handleForm.remark) {
    message.warning('请输入处理备注')
    return
  }
  handling.value = true
  try {
    await reconciliationApi.handleDifference(handleForm.id, {
      method: handleForm.method,
      remark: handleForm.remark
    })
    message.success('处理完成')
    handleModalVisible.value = false
    await refreshAll()
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '处理失败')
  } finally {
    handling.value = false
  }
}

// ═══ 详情 ═══
const detailVisible = ref(false)
const detailData = ref<any>(null)

function handleView(record: any) {
  // 先用列表行数据即时打开，再拉详情：详情返回 ReconciliationDetailVO，携带 diffRecords 差异明细
  detailData.value = record
  detailVisible.value = true
  if (record?.id == null) return
  reconciliationApi.get(record.id)
    .then((res: any) => {
      if (res) detailData.value = { ...record, ...res }
    })
    .catch((error: any) => {
      message.error(error?.response?.data?.message || '对账详情加载失败')
    })
}

function printableRows(): any[] {
  return tableData.value.filter((r: any) => !r.__ghost)
}

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML 调浏览器打印，现在交给 PrintDialog：列与行由页面给，模板负责版式。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'payment-reconciliation-daily',
  title: '每日对账',
  rows: () => printableRows(),
  columns: () => columns,
  totalText: () => `渠道：${searchForm.channel || '全部'} ｜ 日期区间：${searchForm.dateRange?.join(' ~ ') || '全部'} ｜ 共 ${printableRows().length} 条`,
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
  const headers = ['对账日期', '渠道', '总笔数', '总金额', '成功笔数', '成功金额', '差异笔数', '差异金额', '状态', '处理方式', '备注']
  const csv = [
    headers.join(','),
    ...rows.map((r: any) => [
      r.reconcileDate || '',
      PAYMENT_CHANNEL_MAP[r.channel]?.name || r.channel || '',
      r.totalCount ?? 0,
      r.totalAmount ?? 0,
      r.successCount ?? 0,
      r.successAmount ?? 0,
      r.diffCount ?? 0,
      r.diffAmount ?? 0,
      RECON_STATUS_MAP[r.status]?.text || '',
      HANDLE_METHOD_MAP[r.handleMethod] || '',
      r.remark || ''
    ].map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(','))
  ].join('\r\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `每日对账_${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

function handleError(error: Error) {
  console.error('[每日对账] 页面错误', error)
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

.recon-body { flex: 1; min-height: 0; display: flex; flex-direction: column; padding: 12px 16px 0; }
.stat-row { flex-shrink: 0; margin-bottom: 12px; }
.stat-card { display: flex; align-items: center; padding: 12px 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06); }
.stat-card.stat-card-blue { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); border: 1px solid #91d5ff; }
.stat-card.stat-card-green { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); border: 1px solid #b7eb8f; }
.stat-card.stat-card-orange { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); border: 1px solid #ffd591; }
.stat-card.stat-card-purple { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); border: 1px solid #d3adf7; }
.stat-icon { width: 42px; height: 42px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 20px; margin-right: 12px; flex-shrink: 0; }
.stat-content { flex: 1; min-width: 0; }
.stat-title { font-size: 13px; color: #666; }
.stat-value { font-size: 22px; font-weight: 600; color: #303133; font-family: 'SFMono-Regular', Consolas, monospace; }
.stat-desc { font-size: 12px; color: #999; }

/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

.amount-success { color: #52c41a; font-weight: 600; font-family: 'SFMono-Regular', Consolas, monospace; }
.amount-danger { color: #f5222d; font-weight: 600; font-family: 'SFMono-Regular', Consolas, monospace; }
.amount-plain { font-weight: 500; font-family: 'SFMono-Regular', Consolas, monospace; }
.text-muted { color: #999; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
