<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        支付记录（交易 → 支付管理 → 支付记录，路由 payment/record/list，菜单 90302）
        · 定位：在线支付**逐笔流水**（渠道回调落库的每笔成功/失败记录）——只读查询 + 详情
        · 对标：ql361 无对应页（支付记录开发文档）→ 按金标准结构实现
        · 骨架（路线 A）：CategoryListLayout + BillDetailTable（表头序号齿轮列配置：个人/全局）+ PageConfigPanel
        · 数据：`GET /api/payment/record/page`（本轮扩充：渠道/状态/渠道订单号/支付时间区间）
                统计：`GET /api/payment/record/stat?channel=`（后端聚合，随查询区「支付渠道」联动）
        · 状态口径（如实记录）：`payment_record.status` 后端字典 = 0 待支付 / 1 支付中 / 2 成功 / 3 失败；
          本页状态列仍按**二值展示**（status===2 成功，其余失败），但**筛选下拉**已按真实字典取值：
          成功=2、失败=3（原「失败=1」是错的，会把「支付中」筛出来——本轮已修正）
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
              支付记录由渠道回调自动落库（无新增/编辑）；「业务单号」需经支付请求关联查询
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

        <!-- ═══ 查询区（渠道订单号 / 支付渠道 / 状态 / 支付时间区间） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div v-if="isQueryVisible('channelOrderNo')" class="search-item">
                <span class="search-label">渠道订单号</span>
                <a-input
                  v-model:value="searchForm.channelOrderNo"
                  placeholder="请输入渠道订单号"
                  size="small"
                  style="width: 190px"
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
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 110px"
                  allow-clear
                  :options="statusOptions"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('timeRange')" class="search-item">
                <span class="search-label">支付时间</span>
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
          <div class="record-body">
            <!-- 统计卡片（后端聚合：GET /api/payment/record/stat） -->
            <a-row :gutter="12" class="stat-row">
              <a-col :span="6">
                <div class="stat-card stat-card-green">
                  <div class="stat-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);"><CheckCircleOutlined /></div>
                  <div class="stat-content">
                    <div class="stat-title">成功</div>
                    <div class="stat-value">{{ stats.successCount }}</div>
                    <div class="stat-desc">后端聚合（status=2）</div>
                  </div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-red">
                  <div class="stat-icon" style="background: linear-gradient(135deg, #ff4d4f 0%, #cf1322 100%);"><CloseCircleOutlined /></div>
                  <div class="stat-content">
                    <div class="stat-title">失败</div>
                    <div class="stat-value">{{ stats.failedCount }}</div>
                    <div class="stat-desc">后端聚合（status≠2）</div>
                  </div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-purple">
                  <div class="stat-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);"><BarChartOutlined /></div>
                  <div class="stat-content">
                    <div class="stat-title">成功金额</div>
                    <div class="stat-value">¥{{ formatAmount(stats.successAmount) }}</div>
                    <div class="stat-desc">后端聚合</div>
                  </div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-blue">
                  <div class="stat-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);"><DollarOutlined /></div>
                  <div class="stat-content">
                    <div class="stat-title">笔数</div>
                    <div class="stat-value">{{ stats.total }}</div>
                    <div class="stat-desc">后端聚合（渠道口径）</div>
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
                storage-key="payment-record-table-columns"
                global-config-key="payment-record-table-columns"
              >
                <!-- 支付渠道 -->
                <template #channelCell="{ record }">
                  <a-tag :color="PAYMENT_CHANNEL_MAP[record.channel]?.color || 'default'">
                    {{ PAYMENT_CHANNEL_MAP[record.channel]?.name || record.channel || '-' }}
                  </a-tag>
                </template>

                <!-- 支付金额 -->
                <template #amountCell="{ record }">
                  <span class="amount-success">¥{{ formatAmount(record.amount) }}</span>
                </template>

                <!-- 状态 -->
                <template #statusCell="{ record }">
                  <a-tag :color="record.status === 2 ? 'success' : 'error'">
                    {{ record.status === 2 ? '成功' : '失败' }}
                  </a-tag>
                </template>

                <!-- 回调时间 -->
                <template #callbackTimeCell="{ record }">
                  {{ fmtTime(record.callbackTime) }}
                </template>

                <!-- 创建时间 -->
                <template #createTimeCell="{ record }">
                  {{ fmtTime(record.createTime) }}
                </template>

                <!-- 操作列（详情） -->
                <template #actionCell="{ record }">
                  <a-button v-if="!record.__ghost" type="link" size="small" @click="handleView(record)">详情</a-button>
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

      <!-- ═══ 详情抽屉（补 errorCode / errorMsg / 回调原始数据） ═══ -->
      <a-drawer v-model:open="detailVisible" title="支付记录详情" placement="right" :width="640" :footer="null">
        <a-descriptions v-if="detailData" :column="1" bordered size="small">
          <a-descriptions-item label="记录ID">{{ detailData.id }}</a-descriptions-item>
          <a-descriptions-item label="支付请求ID">{{ detailData.requestId ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="渠道订单号">{{ detailData.channelOrderNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="渠道交易号">{{ detailData.channelTradeNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="支付渠道">
            <a-tag :color="PAYMENT_CHANNEL_MAP[detailData.channel]?.color || 'default'">
              {{ PAYMENT_CHANNEL_MAP[detailData.channel]?.name || detailData.channel }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="支付金额">
            <span class="amount-success">¥{{ formatAmount(detailData.amount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="detailData.status === 2 ? 'success' : 'error'">
              {{ detailData.status === 2 ? '成功' : '失败' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="回调时间">{{ fmtTime(detailData.callbackTime) }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ fmtTime(detailData.createTime) }}</a-descriptions-item>
          <a-descriptions-item label="错误码">{{ detailData.errorCode || '-' }}</a-descriptions-item>
          <a-descriptions-item label="错误信息">{{ detailData.errorMsg || '-' }}</a-descriptions-item>
        </a-descriptions>

        <a-divider>回调原始数据</a-divider>
        <pre class="json-view">{{ prettyCallbackData }}</pre>
      </a-drawer>

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="payment-record-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
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
import { paymentApi, PAYMENT_CHANNEL_MAP } from '@/api/payment'

defineOptions({ name: 'PaymentRecordList' })

// ═══ 常量字典 ═══
const channelOptions = Object.entries(PAYMENT_CHANNEL_MAP).map(([value, v]) => ({ value, label: v.name }))
/**
 * 状态筛选：取后端 `payment_record.status` 真实字典（0 待支付 / 1 支付中 / 2 成功 / 3 失败），
 * 后端 `status` 为**等值匹配**，故「失败」必须传 3（修正前误传 1 = 支付中）。
 * 页面状态列仍按二值展示（2=成功，其余=失败）；0/1 需全量口径时请依赖后端后续提供「status≠2」语义。
 */
const statusOptions = [
  { value: 2, label: '成功' },
  { value: 3, label: '失败' }
]

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const timeRange = ref<[string, string] | undefined>()
const stats = reactive<Record<string, any>>({})

const searchForm = reactive<{
  channelOrderNo?: string
  channel?: string
  status?: number
}>({})

// 序号列承载表头「列配置」齿轮；操作列为固定列（8 列 + 行号 + 操作）
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 80, fixed: 'left' },
  { key: 'channelOrderNo', title: '渠道订单号', type: 'input', width: 170 },
  { key: 'channelTradeNo', title: '渠道交易号', type: 'input', width: 170 },
  { key: 'channel', title: '支付渠道', type: 'slot', slotName: 'channelCell', width: 110 },
  { key: 'amount', title: '支付金额', type: 'slot', slotName: 'amountCell', width: 120, align: 'right' },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 90 },
  { key: 'callbackTime', title: '回调时间', type: 'slot', slotName: 'callbackTimeCell', width: 160 },
  { key: 'createTime', title: '创建时间', type: 'slot', slotName: 'createTimeCell', width: 160 },
  { key: 'requestId', title: '支付请求ID', type: 'input', width: 110, defaultHidden: true },
  { key: 'errorCode', title: '错误码', type: 'input', width: 120, defaultHidden: true },
  { key: 'errorMsg', title: '错误信息', type: 'input', width: 220, defaultHidden: true }
]

// ═══ 页面配置 ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'channelOrderNo', label: '渠道订单号', visible: true },
  { key: 'channel', label: '支付渠道', visible: true },
  { key: 'status', label: '状态', visible: true },
  { key: 'timeRange', label: '支付时间', visible: true }
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
  if (searchForm.channelOrderNo) params.channelOrderNo = searchForm.channelOrderNo.trim()
  if (searchForm.channel) params.channel = searchForm.channel
  if (searchForm.status != null) params.status = searchForm.status
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
    const res: any = await paymentApi.pageRecord(buildQuery())
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[支付记录] 加载列表失败', error)
    message.error(error?.response?.data?.message || '获取支付记录失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 统计走后端聚合（`GET /api/payment/record/stat?channel=`），按查询区「支付渠道」联动 */
async function loadStats() {
  try {
    const res: any = await paymentApi.statRecord({ channel: searchForm.channel || undefined })
    Object.assign(stats, res || {})
  } catch (error) {
    console.warn('[支付记录] 统计加载失败', error)
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
  Object.assign(searchForm, { channelOrderNo: undefined, channel: undefined, status: undefined })
  timeRange.value = undefined
  pagination.current = 1
  refreshAll()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ═══ 详情 ═══
const detailVisible = ref(false)
const detailData = ref<any>(null)

const prettyCallbackData = computed(() => {
  const raw = detailData.value?.callbackData
  if (!raw) return '（无回调数据）'
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return String(raw)
  }
})

function handleView(record: any) {
  detailData.value = record
  detailVisible.value = true
}

// ═══ 打印(F8)：真实打印模板（与列表同口径） ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function printableRows(): any[] {
  return tableData.value.filter((r: any) => !r.__ghost)
}

function handlePrint() {
  const rows = printableRows()
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const body = rows.map((r: any, i: number) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(r.channelOrderNo || '')}</td>
      <td>${escapeHtml(r.channelTradeNo || '')}</td>
      <td>${escapeHtml(PAYMENT_CHANNEL_MAP[r.channel]?.name || r.channel || '')}</td>
      <td style="text-align:right">${formatAmount(r.amount)}</td>
      <td>${r.status === 2 ? '成功' : '失败'}</td>
      <td>${escapeHtml(fmtTime(r.callbackTime))}</td>
      <td>${escapeHtml(fmtTime(r.createTime))}</td>
      <td>${escapeHtml(r.errorCode || '')}</td>
      <td>${escapeHtml(r.errorMsg || '')}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>支付记录</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>支付记录</h2>
    <div class="meta">
      <span>渠道：${escapeHtml(searchForm.channel || '全部')}</span>
      <span>渠道订单号：${escapeHtml(searchForm.channelOrderNo || '全部')}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr><th>#</th><th>渠道订单号</th><th>渠道交易号</th><th>支付渠道</th><th>支付金额</th><th>状态</th><th>回调时间</th><th>创建时间</th><th>错误码</th><th>错误信息</th></tr></thead>
      <tbody>${body}</tbody>
    </table></body></html>`
  const win = window.open('', '_blank', 'width=1200,height=700')
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
  const headers = ['渠道订单号', '渠道交易号', '支付渠道', '支付金额', '状态', '回调时间', '创建时间', '支付请求ID', '错误码', '错误信息']
  const csv = [
    headers.join(','),
    ...rows.map((r: any) => [
      r.channelOrderNo || '',
      r.channelTradeNo || '',
      PAYMENT_CHANNEL_MAP[r.channel]?.name || r.channel || '',
      r.amount ?? '',
      r.status === 2 ? '成功' : '失败',
      r.callbackTime ? fmtTime(r.callbackTime) : '',
      r.createTime ? fmtTime(r.createTime) : '',
      r.requestId ?? '',
      r.errorCode || '',
      r.errorMsg || ''
    ].map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(','))
  ].join('\r\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `支付记录_${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

function handleError(error: Error) {
  console.error('[支付记录] 页面错误', error)
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

.record-body { flex: 1; min-height: 0; display: flex; flex-direction: column; padding: 12px 16px 0; }
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

.amount-success { color: #52c41a; font-weight: 600; font-family: 'SFMono-Regular', Consolas, monospace; }
.json-view {
  margin: 0; padding: 10px; max-height: 320px; overflow: auto;
  background: #f6f8fa; border: 1px solid #eee; border-radius: 4px;
  font-size: 12px; line-height: 1.6; white-space: pre-wrap; word-break: break-all;
}

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
