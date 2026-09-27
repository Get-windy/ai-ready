<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        外部订单（交易 → 外部平台 → 外部订单，路由 trade/external-order/list，菜单 605）
        · 定位：外部平台订单接入台账（原始 JSON 留存 + 处理状态机 + 失败重试 + 已入库跳转内部订单）
        · 对标：ql361 无独立页（外部订单开发文档「对标判断」）→ 本系统独有页，按本系统实现 + 业界标准
        · 骨架（路线 A）：CategoryListLayout + BillDetailTable（表头序号齿轮列配置：个人/全局）+ PageConfigPanel
        · 数据：`GET /api/trade/external-order/page`、`POST /api/trade/external-order/{id}/retry`、
                `GET /api/trade/external-order/pending-count`（管理端端点，已不再调开放接口 `/api/open/order/pending-count`）、
                `GET /api/trade/external-order/stat`（后端真实聚合，全量口径）
          —— 管理端端点由 `ExternalOrderController` 装配（原仅 OpenApiController 供外部回调调用）
        · 状态机：`ExternalOrderRaw.processStatus` = 0 待处理 / 1 已转换 / 2 已入库 / 3 失败
        · 统计口径：卡片全部走后端聚合；后端 `stat` 的「已处理」= status 1（已转换）+ 2（已入库）
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：待处理量提示 + 导出原始台账说明 ═══ -->
        <template #toolbar-left>
          <a-space :size="12">
            <a-tag color="warning">待处理 {{ pendingCount }} 笔</a-tag>
            <span class="toolbar-tip">原始报文按渠道回调留存；失败单可重试，已入库单可跳转内部订单</span>
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

        <!-- ═══ 查询区（渠道 / 处理状态 / 外部订单号 / 接收时间区间） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div v-if="isQueryVisible('channelCode')" class="search-item">
                <span class="search-label">渠道</span>
                <a-select
                  v-model:value="searchForm.channelCode"
                  placeholder="全部渠道"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  :options="channelOptions"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('status')" class="search-item">
                <span class="search-label">处理状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('externalOrderId')" class="search-item">
                <span class="search-label">外部订单号</span>
                <a-input
                  v-model:value="searchForm.externalOrderId"
                  placeholder="请输入外部订单号"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('timeRange')" class="search-item">
                <span class="search-label">接收时间</span>
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
          <div class="order-body">
            <!-- 统计卡片：接后端真实聚合（台账总数/失败/已处理 走 `GET /api/trade/external-order/stat`；
                 「待处理」走 `GET /api/trade/external-order/pending-count`，支持随渠道筛选联动） -->
            <a-row :gutter="12" class="stat-row">
              <a-col :span="6">
                <div class="stat-card stat-card-blue">
                  <div class="stat-title">台账总条数</div>
                  <div class="stat-value">{{ stats.total }}</div>
                  <div class="stat-desc">后端聚合（全量台账）</div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-orange">
                  <div class="stat-title">待处理</div>
                  <div class="stat-value">{{ pendingCount }}</div>
                  <div class="stat-desc">/pending-count（后端聚合）</div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-red">
                  <div class="stat-title">失败</div>
                  <div class="stat-value">{{ stats.failedCount }}</div>
                  <div class="stat-desc">后端聚合（processStatus=3）</div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-green">
                  <div class="stat-title">已处理</div>
                  <div class="stat-value">{{ stats.processedCount }}</div>
                  <div class="stat-desc">后端聚合（已转换/已入库 1·2）</div>
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
                storage-key="trade-external-order-table-columns"
                global-config-key="trade-external-order-table-columns"
              >
                <!-- 渠道（按渠道类型着色） -->
                <template #channelCell="{ record }">
                  <a-tag :color="channelTagColor(record.channelCode)">
                    {{ CHANNEL_CODE_MAP[record.channelCode]?.name || record.channelCode }}
                  </a-tag>
                </template>

                <!-- 外部订单号 -->
                <template #externalOrderIdCell="{ record }">
                  <span class="mono">{{ record.externalOrderId || '-' }}</span>
                </template>

                <!-- 处理状态 -->
                <template #statusCell="{ record }">
                  <a-tag :color="STATUS_COLOR_MAP[record.processStatus] || 'default'">
                    {{ STATUS_TEXT_MAP[record.processStatus] || record.processStatus }}
                  </a-tag>
                </template>

                <!-- 内部订单ID（点击跳转商城订单） -->
                <template #internalOrderCell="{ record }">
                  <a
                    v-if="record.internalOrderId"
                    class="cell-link"
                    @click="goToInternalOrder(record)"
                  >{{ record.internalOrderId }}</a>
                  <span v-else>-</span>
                </template>

                <!-- 接收时间 -->
                <template #receiveTimeCell="{ record }">
                  {{ fmtTime(record.receiveTime) }}
                </template>

                <!-- 原始数据 -->
                <template #rawDataCell="{ record }">
                  <a-button
                    v-if="!record.__ghost"
                    type="link"
                    size="small"
                    @click="showRawData(record)"
                  >
                    查看原始数据
                  </a-button>
                </template>

                <!-- 操作列（重试 / 查看订单） -->
                <template #actionCell="{ record }">
                  <a-space v-if="!record.__ghost" :size="0">
                    <a-button
                      v-if="record.processStatus === 3"
                      type="link"
                      size="small"
                      :loading="retryingId === record.id"
                      @click="handleRetry(record)"
                    >
                      重试
                    </a-button>
                    <a-button
                      v-if="record.processStatus === 2"
                      type="link"
                      size="small"
                      @click="goToInternalOrder(record)"
                    >
                      查看订单
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

      <!-- ═══ 查看原始数据弹窗 ═══ -->
      <a-modal
        v-model:open="rawDataModalVisible"
        title="原始订单数据"
        :width="700"
        :footer="null"
      >
        <a-descriptions v-if="rawRecord" :column="2" size="small" bordered style="margin-bottom: 12px">
          <a-descriptions-item label="渠道">
            {{ CHANNEL_CODE_MAP[rawRecord.channelCode]?.name || rawRecord.channelCode || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="外部订单号">{{ rawRecord.externalOrderId || '-' }}</a-descriptions-item>
          <a-descriptions-item label="接收时间">{{ fmtTime(rawRecord.receiveTime) }}</a-descriptions-item>
          <a-descriptions-item label="处理状态">
            <a-tag :color="STATUS_COLOR_MAP[rawRecord.processStatus] || 'default'">
              {{ STATUS_TEXT_MAP[rawRecord.processStatus] || '-' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="错误信息" :span="2">{{ rawRecord.errorMsg || '-' }}</a-descriptions-item>
        </a-descriptions>
        <pre class="json-view">{{ rawJsonData }}</pre>
      </a-modal>

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="trade-external-order-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="trade-external-order-list"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import {
  ReloadOutlined, PrinterOutlined, DownloadOutlined, SettingOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { externalOrderApi, CHANNEL_CODE_MAP, type ExternalOrderRaw } from '@/api/trade'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'TradeExternalOrder' })

// ═══ 常量字典（与后端 ExternalOrderRaw.processStatus 注释一致） ═══
const STATUS_TEXT_MAP: Record<number, string> = { 0: '待处理', 1: '已转换', 2: '已入库', 3: '失败' }
const STATUS_COLOR_MAP: Record<number, string> = { 0: 'warning', 1: 'processing', 2: 'success', 3: 'error' }
const STATUS_OPTIONS = Object.entries(STATUS_TEXT_MAP).map(([value, label]) => ({ value: Number(value), label }))
const channelOptions = Object.entries(CHANNEL_CODE_MAP).map(([value, v]) => ({ value, label: v.name }))

function channelTagColor(code: string): string {
  const type = CHANNEL_CODE_MAP[code]?.type
  if (type === 'ECOMMERCE') return 'blue'
  if (type === 'SOCIAL') return 'green'
  if (type === 'ERP') return 'orange'
  return 'purple'
}

// ═══ 状态 ═══
const router = useRouter()
const loading = ref(false)
const retryingId = ref<number | null>(null)
const tableData = ref<ExternalOrderRaw[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const pendingCount = ref(0)
const timeRange = ref<[string, string] | undefined>()

const searchForm = reactive<{
  channelCode?: string
  status?: number
  externalOrderId?: string
}>({})

// 序号列承载表头「列配置」齿轮；操作列为固定列（8 列 + 行号 + 操作）
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 110, fixed: 'left' },
  { key: 'channelCode', title: '渠道', type: 'slot', slotName: 'channelCell', width: 120 },
  { key: 'externalOrderId', title: '外部订单号', type: 'slot', slotName: 'externalOrderIdCell', width: 180 },
  { key: 'receiveTime', title: '接收时间', type: 'slot', slotName: 'receiveTimeCell', width: 160 },
  { key: 'processStatus', title: '处理状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'internalOrderId', title: '内部订单ID', type: 'slot', slotName: 'internalOrderCell', width: 140 },
  { key: 'retryCount', title: '重试次数', type: 'input', width: 90, align: 'right' },
  { key: 'errorMsg', title: '错误信息', type: 'input', width: 240 },
  { key: 'rawData', title: '原始数据', type: 'slot', slotName: 'rawDataCell', width: 120 }
]

// ═══ 统计卡片（后端真实聚合：GET /api/trade/external-order/stat，全量口径；
//     「待处理」另有 pendingCount，走后端 /pending-count 且支持渠道筛选） ═══
const stats = reactive<Record<string, any>>({})

// ═══ 页面配置 ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'channelCode', label: '渠道', visible: true },
  { key: 'status', label: '处理状态', visible: true },
  { key: 'externalOrderId', label: '外部订单号', visible: true },
  { key: 'timeRange', label: '接收时间', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
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
  if (config?.functionButtons) functionButtons.value = config.functionButtons.map(b => ({ ...b }))
}

// ═══ 格式化 ═══
function fmtTime(val?: string | null): string {
  if (!val) return '-'
  return dayjs(val).format('YYYY-MM-DD HH:mm:ss')
}

// ═══ 查询参数（与管理端 ExternalOrderController 的参数名/类型严格一致，非 any 透传） ═══
function buildQuery(): {
  pageNum: number
  pageSize: number
  channelCode?: string
  status?: number
  externalOrderId?: string
  startTime?: string
  endTime?: string
} {
  const params: {
    pageNum: number
    pageSize: number
    channelCode?: string
    status?: number
    externalOrderId?: string
    startTime?: string
    endTime?: string
  } = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize
  }
  if (searchForm.channelCode) params.channelCode = searchForm.channelCode
  if (searchForm.status != null) params.status = searchForm.status
  if (searchForm.externalOrderId) params.externalOrderId = searchForm.externalOrderId.trim()
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
    const res: any = await externalOrderApi.page(buildQuery())
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[外部订单] 加载台账失败', error)
    message.error(error?.response?.data?.message || '加载外部订单失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 台账聚合统计（`GET /api/trade/external-order/stat`）；失败只告警，不影响列表 */
async function loadStats() {
  try {
    const res: any = await externalOrderApi.stat()
    Object.assign(stats, res || {})
  } catch (error) {
    console.warn('[外部订单] 统计加载失败', error)
  }
}

/** 待处理量（管理端 `GET /api/trade/external-order/pending-count`，支持渠道筛选） */
async function loadPendingCount() {
  try {
    const res: any = await externalOrderApi.countPending(searchForm.channelCode || undefined)
    pendingCount.value = Number(res) || 0
  } catch (error) {
    pendingCount.value = 0
    console.warn('[外部订单] 待处理量加载失败', error)
  }
}

async function refreshAll() {
  await Promise.all([fetchData(), loadStats(), loadPendingCount()])
}

function handleSearch() {
  pagination.current = 1
  refreshAll()
}

function handleReset() {
  Object.assign(searchForm, { channelCode: undefined, status: undefined, externalOrderId: undefined })
  timeRange.value = undefined
  pagination.current = 1
  refreshAll()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ═══ 行级操作 ═══
async function handleRetry(record: any) {
  retryingId.value = record.id
  try {
    const res: any = await externalOrderApi.retry(record.id)
    if (res === false) {
      message.warning('重试次数已超限（≥5 次），请人工处理')
    } else {
      message.success('已重新加入处理队列')
    }
    await refreshAll()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '重试失败')
  } finally {
    retryingId.value = null
  }
}

/**
 * 跳转内部订单（商城订单页）
 * ⚠️ 目标页暂无单据深链路由参数支持，故以 query 传 id（`/trade/mall-order?orderId=…`）
 */
function goToInternalOrder(record: any) {
  if (!record?.internalOrderId) {
    message.warning('该订单尚未生成内部订单')
    return
  }
  router.push({ path: '/trade/mall-order', query: { orderId: String(record.internalOrderId) } })
}

// ═══ 原始数据弹窗 ═══
const rawDataModalVisible = ref(false)
const rawJsonData = ref('')
const rawRecord = ref<any>(null)

function showRawData(record: any) {
  rawRecord.value = record
  try {
    rawJsonData.value = JSON.stringify(JSON.parse(record.rawData), null, 2)
  } catch {
    rawJsonData.value = record.rawData || '（无原始报文）'
  }
  rawDataModalVisible.value = true
}

// ═══ 打印（结果集打印）：外部订单 ═══
// 原先是自己拼 HTML + 浏览器打印，现在交给 PrintDialog：列与行由页面给，模板负责版式。
// 打印列与原来的表格逐列对齐（# 行号、渠道/处理状态中文、时间格式都在 printRows 里先算好）。
const printColumns: any[] = [
  { title: '#', key: '__seq', width: 40, align: 'center' },
  { title: '渠道', key: 'channelLabel' },
  { title: '外部订单号', key: 'externalOrderId' },
  { title: '接收时间', key: 'receiveTimeText' },
  { title: '处理状态', key: 'processStatusText' },
  { title: '内部订单ID', key: 'internalOrderId' },
  { title: '重试次数', key: 'retryCount', align: 'right' },
  { title: '错误信息', key: 'errorMsg' },
]

/** 可打印行（去掉树形占位行） */
function printableRows(): any[] {
  return tableData.value.filter((r: any) => !r.__ghost)
}

/** 打印行：先把单元格文本按原打印口径算好，模板只负责排版 */
function printRows(): any[] {
  return printableRows().map((r: any, i: number) => ({
    __seq: i + 1,
    channelLabel: CHANNEL_CODE_MAP[r.channelCode]?.name || r.channelCode || '',
    externalOrderId: r.externalOrderId || '',
    receiveTimeText: fmtTime(r.receiveTime),
    processStatusText: STATUS_TEXT_MAP[r.processStatus] || '',
    internalOrderId: r.internalOrderId ?? '',
    retryCount: r.retryCount ?? 0,
    errorMsg: r.errorMsg || '',
  }))
}

const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'trade-external-order-list',
  // 原打印抬头的筛选/记录数元信息行并入标题；打印时间由引擎按本次打印时间给
  title: () => `外部订单（渠道：${searchForm.channelCode || '全部'}，处理状态：${searchForm.status != null ? STATUS_TEXT_MAP[searchForm.status] : '全部'}，记录数：${printRows().length}）`,
  columns: () => printColumns,
  rows: () => printRows(),
  emptyTip: '没有可打印的数据',
})

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

// ═══ 导出（CSV，前端拼装；后端无外部订单导出端点） ═══
function handleExport() {
  const rows = printableRows()
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const headers = ['渠道', '外部订单号', '接收时间', '处理状态', '内部订单ID', '重试次数', '错误信息']
  const csv = [
    headers.join(','),
    ...rows.map((r: any) => [
      CHANNEL_CODE_MAP[r.channelCode]?.name || r.channelCode || '',
      r.externalOrderId || '',
      r.receiveTime ? fmtTime(r.receiveTime) : '',
      STATUS_TEXT_MAP[r.processStatus] || '',
      r.internalOrderId ?? '',
      r.retryCount ?? 0,
      r.errorMsg || ''
    ].map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(','))
  ].join('\r\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `外部订单_${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

function handleError(error: Error) {
  console.error('[外部订单] 页面错误', error)
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

.order-body { flex: 1; min-height: 0; display: flex; flex-direction: column; padding: 12px 16px 0; }
.stat-row { flex-shrink: 0; margin-bottom: 12px; }
.stat-card { padding: 12px 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06); }
.stat-card.stat-card-blue { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); border: 1px solid #91d5ff; }
.stat-card.stat-card-green { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); border: 1px solid #b7eb8f; }
.stat-card.stat-card-red { background: linear-gradient(135deg, #fff2f0 0%, #ffd8d2 100%); border: 1px solid #ffbcb3; }
.stat-card.stat-card-orange { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); border: 1px solid #ffd591; }
.stat-title { font-size: 13px; color: #666; }
.stat-value { font-size: 22px; font-weight: 600; color: #303133; font-family: 'SFMono-Regular', Consolas, monospace; }
.stat-desc { font-size: 12px; color: #999; }

/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.mono { font-family: 'Consolas', 'Monaco', monospace; font-size: 12px; }
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
