<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        数据同步（库存同步台账，交易 → 外部平台 → 库存同步，路由 trade/inventory-sync/list，菜单 606）
        · 定位：本系统与外部渠道之间库存推送(PUSH)/拉取(PULL)/查询(QUERY)的明细台账（监控健康度 + 排查失败项）
        · 对标：ql361 无独立页（数据同步开发文档「对标判断」）→ 本系统独有页，按本系统实现 + 业界标准
        · 骨架（路线 A）：CategoryListLayout + BillDetailTable（表头序号齿轮列配置：个人/全局）+ PageConfigPanel
        · 数据：`GET /api/trade/inventory-sync/page`（后端 InventorySyncController 已装配 → 开发文档「后端无控制器」已过时）
                导出：`GET /api/trade/inventory-sync/export`（真实 xlsx）
                统计：`GET /api/trade/inventory-sync/stat`（后端真实聚合，全量口径，非当前页）
                重试：`POST /api/trade/inventory-sync/{id}/retry`（本域管理端端点，不再复用 /api/trade/api-monitor/sync/{id}/retry）
        · 状态口径统一：实体三态 `InventorySyncRecord.syncStatus` = 0 待同步 / 1 成功 / 2 失败（原页面二态映射已修正）
        · 分页口径：后端 `/page` 的 `pageSize` 默认 10，本页**显式传 20**（页码栏可选 [20,50,100]），后端默认值不参与本页
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：失败重试（勾选行）+ 台账说明 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('retryFailed')"
              size="small"
              :disabled="retryableSelected.length === 0"
              :loading="retrying"
              @click="handleBatchRetry"
            >
              <RedoOutlined /> 失败重试{{ retryableSelected.length ? ` (${retryableSelected.length})` : '' }}
            </a-button>
            <span class="toolbar-tip">台账按同步时间倒序；手动同步在「渠道配置」页发起</span>
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
            <a-button v-if="isButtonEnabled('refresh')" size="small" :loading="loading" @click="fetchData">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button v-if="isButtonEnabled('printF8')" size="small" @click="handlePrint">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button v-if="isButtonEnabled('export')" size="small" :loading="exporting" @click="handleExport">
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（渠道 / SKU编码 / 同步类型 / 状态 / 同步时间区间） ═══ -->
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
              <div v-if="isQueryVisible('skuCode')" class="search-item">
                <span class="search-label">SKU编码</span>
                <a-input
                  v-model:value="searchForm.skuCode"
                  placeholder="请输入SKU编码"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('syncType')" class="search-item">
                <span class="search-label">同步类型</span>
                <a-select
                  v-model:value="searchForm.syncType"
                  placeholder="全部类型"
                  size="small"
                  style="width: 110px"
                  allow-clear
                  :options="SYNC_TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('syncStatus')" class="search-item">
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="searchForm.syncStatus"
                  placeholder="全部状态"
                  size="small"
                  style="width: 110px"
                  allow-clear
                  :options="SYNC_STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('errorCategory')" class="search-item">
                <span class="search-label">失败分类</span>
                <a-select
                  v-model:value="searchForm.errorCategory"
                  placeholder="全部分类"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  :options="ERROR_CATEGORY_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('timeRange')" class="search-item">
                <span class="search-label">同步时间</span>
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
          <div class="sync-body">
            <!-- 统计卡片：接后端真实聚合 `GET /api/trade/inventory-sync/stat`（全量口径）；
                 「同步记录数」用分页 total（等于聚合 total） -->
            <a-row :gutter="12" class="stat-row">
              <a-col :span="6">
                <div class="stat-card stat-card-blue">
                  <div class="stat-title">同步记录数</div>
                  <div class="stat-value">{{ pagination.total }}</div>
                  <div class="stat-desc">分页 total</div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-green">
                  <div class="stat-title">成功</div>
                  <div class="stat-value">{{ stats.successCount }}</div>
                  <div class="stat-desc">后端聚合（syncStatus=1）</div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-red">
                  <div class="stat-title">失败</div>
                  <div class="stat-value">{{ stats.failedCount }}</div>
                  <div class="stat-desc">后端聚合（syncStatus=2）</div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-orange">
                  <div class="stat-title">待同步</div>
                  <div class="stat-value">{{ stats.pendingCount }}</div>
                  <div class="stat-desc">后端聚合（syncStatus=0）</div>
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
                storage-key="trade-inventory-sync-table-columns"
                global-config-key="trade-inventory-sync-table-columns"
                @checkbox-change="handleCheckboxChange"
                @checkbox-all="handleCheckboxAll"
              >
                <!-- 渠道（按渠道类型着色） -->
                <template #channelCell="{ record }">
                  <a-tag :color="channelTagColor(record.channelCode)">
                    {{ CHANNEL_CODE_MAP[record.channelCode]?.name || record.channelCode }}
                  </a-tag>
                </template>

                <!-- 同步类型 -->
                <template #syncTypeCell="{ record }">
                  <a-tag :color="SYNC_TYPE_MAP[record.syncType]?.color || 'default'">
                    {{ SYNC_TYPE_MAP[record.syncType]?.label || record.syncType || '-' }}
                  </a-tag>
                </template>

                <!-- 状态（实体三态 0 待同步 / 1 成功 / 2 失败） -->
                <template #syncStatusCell="{ record }">
                  <a-tag :color="SYNC_STATUS_MAP[record.syncStatus]?.color || 'default'">
                    {{ SYNC_STATUS_MAP[record.syncStatus]?.label || '-' }}
                  </a-tag>
                </template>

                <!-- 失败分类 -->
                <template #errorCategoryCell="{ record }">
                  <span v-if="record.errorCategory">{{ ERROR_CATEGORY_MAP[record.errorCategory] || record.errorCategory }}</span>
                  <span v-else>-</span>
                </template>

                <!-- 同步时间 -->
                <template #syncTimeCell="{ record }">
                  {{ fmtTime(record.syncTime) }}
                </template>

                <!-- 最近重试时间 -->
                <template #lastRetryTimeCell="{ record }">
                  {{ record.lastRetryTime ? fmtTime(record.lastRetryTime) : '-' }}
                </template>

                <!-- 操作列（行级：失败重试） -->
                <template #actionCell="{ record }">
                  <a-button
                    v-if="!record.__ghost && record.syncStatus === 2"
                    type="link"
                    size="small"
                    :loading="retryingId === record.id"
                    @click="handleRetry(record)"
                  >
                    重试
                  </a-button>
                  <span v-else-if="!record.__ghost" class="toolbar-tip">-</span>
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

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="trade-inventory-sync-page-config"
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
  ReloadOutlined, PrinterOutlined, DownloadOutlined, SettingOutlined, RedoOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { inventorySyncApi, CHANNEL_CODE_MAP, type InventorySyncRecord } from '@/api/trade'

defineOptions({ name: 'TradeInventorySync' })

// ═══ 常量字典（与后端 InventorySyncRecord 注释一致：0待同步 1成功 2失败） ═══
const SYNC_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待同步', color: 'default' },
  1: { label: '成功', color: 'success' },
  2: { label: '失败', color: 'error' }
}
const SYNC_STATUS_OPTIONS = Object.entries(SYNC_STATUS_MAP).map(([value, v]) => ({ value: Number(value), label: v.label }))
const SYNC_TYPE_MAP: Record<string, { label: string; color: string }> = {
  PUSH: { label: '推送', color: 'blue' },
  PULL: { label: '拉取', color: 'orange' },
  QUERY: { label: '查询', color: 'green' }
}
const SYNC_TYPE_OPTIONS = Object.entries(SYNC_TYPE_MAP).map(([value, v]) => ({ value, label: v.label }))
const ERROR_CATEGORY_MAP: Record<string, string> = {
  NETWORK: '网络异常', AUTH: '鉴权失败', PARAM: '参数错误',
  RATE_LIMIT: '渠道限流', BIZ_REJECT: '业务拒绝', UNKNOWN: '未知原因'
}
/** 失败分类筛选项（取值与后端 `ErrorCategory` 枚举一致） */
const ERROR_CATEGORY_OPTIONS = Object.entries(ERROR_CATEGORY_MAP).map(([value, label]) => ({ value, label }))
const channelOptions = Object.entries(CHANNEL_CODE_MAP).map(([value, v]) => ({ value, label: v.name }))

function channelTagColor(code: string): string {
  const type = CHANNEL_CODE_MAP[code]?.type
  if (type === 'ECOMMERCE') return 'blue'
  if (type === 'SOCIAL') return 'green'
  if (type === 'ERP') return 'orange'
  return 'purple'
}

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const retrying = ref(false)
const retryingId = ref<number | string | null>(null)
const tableData = ref<InventorySyncRecord[]>([])
const selectedRowKeys = ref<Array<number | string>>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const timeRange = ref<[string, string] | undefined>()

const searchForm = reactive<{
  channelCode?: string
  skuCode?: string
  syncType?: string
  syncStatus?: number
  errorCategory?: string
}>({})

// 序号列承载表头「列配置」齿轮；勾选列/操作列为锁定列（9 列 + 行号 + 勾选 + 操作）
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 80, fixed: 'left' },
  { key: 'channelCode', title: '渠道', type: 'slot', slotName: 'channelCell', width: 120 },
  { key: 'skuCode', title: 'SKU编码', type: 'input', width: 150 },
  { key: 'internalQty', title: '内部库存', type: 'input', width: 100, align: 'right' },
  { key: 'externalQty', title: '外部库存', type: 'input', width: 100, align: 'right' },
  { key: 'syncQty', title: '同步数量', type: 'input', width: 100, align: 'right' },
  { key: 'syncType', title: '同步类型', type: 'slot', slotName: 'syncTypeCell', width: 100 },
  { key: 'syncStatus', title: '状态', type: 'slot', slotName: 'syncStatusCell', width: 90 },
  { key: 'syncTime', title: '同步时间', type: 'slot', slotName: 'syncTimeCell', width: 160 },
  { key: 'errorMsg', title: '错误信息', type: 'input', width: 220 },
  { key: 'errorCategory', title: '失败分类', type: 'slot', slotName: 'errorCategoryCell', width: 110, defaultHidden: true },
  { key: 'retryCount', title: '重试次数', type: 'input', width: 90, defaultHidden: true },
  { key: 'lastRetryTime', title: '最近重试时间', type: 'slot', slotName: 'lastRetryTimeCell', width: 160, defaultHidden: true }
]

// ═══ 统计卡片（后端真实聚合：GET /api/trade/inventory-sync/stat，全量口径，不随查询区/分页变化） ═══
const stats = reactive<Record<string, any>>({})

/** 勾选行中的「失败」记录（仅失败可重试） */
const retryableSelected = computed(() => {
  const ids = new Set(selectedRowKeys.value)
  return tableData.value.filter((r: any) => !r.__ghost && ids.has(r.id) && r.syncStatus === 2)
})

// ═══ 页面配置 ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'channelCode', label: '渠道', visible: true },
  { key: 'skuCode', label: 'SKU编码', visible: true },
  { key: 'syncType', label: '同步类型', visible: true },
  { key: 'syncStatus', label: '状态', visible: true },
  { key: 'errorCategory', label: '失败分类', visible: true },
  { key: 'timeRange', label: '同步时间', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'retryFailed', label: '失败重试', enabled: true }
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

// ═══ 查询参数（与 InventorySyncController 的参数名严格一致） ═══
function buildQuery(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize
  }
  if (searchForm.channelCode) params.channelCode = searchForm.channelCode
  if (searchForm.skuCode) params.skuCode = searchForm.skuCode.trim()
  if (searchForm.syncType) params.syncType = searchForm.syncType
  if (searchForm.syncStatus != null) params.syncStatus = searchForm.syncStatus
  if (searchForm.errorCategory) params.errorCategory = searchForm.errorCategory
  if (timeRange.value?.length === 2) {
    params.startTime = timeRange.value[0]
    params.endTime = timeRange.value[1]
  }
  return params
}

// ═══ 数据加载 ═══
/** 统计走后端聚合（`GET /api/trade/inventory-sync/stat`）；失败只告警，不影响列表 */
async function loadStats() {
  try {
    const res: any = await inventorySyncApi.stat()
    Object.assign(stats, res || {})
  } catch (error) {
    console.warn('[数据同步] 统计加载失败', error)
  }
}

async function fetchData() {
  loading.value = true
  loadStats()
  try {
    const res: any = await inventorySyncApi.page(buildQuery())
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
    selectedRowKeys.value = []
  } catch (error: any) {
    console.error('[数据同步] 加载台账失败', error)
    message.error(error?.response?.data?.message || '加载同步台账失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  Object.assign(searchForm, {
    channelCode: undefined, skuCode: undefined,
    syncType: undefined, syncStatus: undefined, errorCategory: undefined
  })
  timeRange.value = undefined
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ═══ 勾选 / 重试 ═══
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

/**
 * 同步失败重试（本域管理端端点 `POST /api/trade/inventory-sync/{id}/retry`）
 * —— 与 `/api/trade/api-monitor/sync/{id}/retry` 等价（后端共用 ApiMonitorService#retrySync），此处已切到本域端点。
 * 批量重试：后端**无批量端点**，故对勾选的失败行**前端逐条串行**调用本端点。
 */
async function doRetry(id: number | string, record?: any) {
  if (record) retryingId.value = id
  try {
    const res: any = await inventorySyncApi.retry(id)
    if (res?.success === false) {
      message.warning(res?.message || '重试失败')
    } else {
      message.success(res?.message || '已重新加入同步队列')
    }
    await fetchData()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '重试失败')
  } finally {
    retryingId.value = null
  }
}

function handleRetry(record: any) {
  doRetry(record.id, record)
}

async function handleBatchRetry() {
  const rows = retryableSelected.value
  if (rows.length === 0) {
    message.warning('请先勾选「失败」状态的同步记录')
    return
  }
  retrying.value = true
  try {
    for (const row of rows) {
      await doRetry(row.id)
    }
    message.success(`已重试 ${rows.length} 条失败记录`)
  } finally {
    retrying.value = false
  }
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
      <td>${escapeHtml(CHANNEL_CODE_MAP[r.channelCode]?.name || r.channelCode || '')}</td>
      <td>${escapeHtml(r.skuCode || '')}</td>
      <td style="text-align:right">${escapeHtml(r.internalQty ?? '')}</td>
      <td style="text-align:right">${escapeHtml(r.externalQty ?? '')}</td>
      <td style="text-align:right">${escapeHtml(r.syncQty ?? '')}</td>
      <td>${escapeHtml(SYNC_TYPE_MAP[r.syncType]?.label || r.syncType || '')}</td>
      <td>${escapeHtml(SYNC_STATUS_MAP[r.syncStatus]?.label || '')}</td>
      <td>${escapeHtml(fmtTime(r.syncTime))}</td>
      <td>${escapeHtml(r.errorMsg || '')}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>库存同步记录</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>库存同步记录</h2>
    <div class="meta">
      <span>渠道：${escapeHtml(searchForm.channelCode || '全部')}</span>
      <span>SKU编码：${escapeHtml(searchForm.skuCode || '全部')}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr><th>#</th><th>渠道</th><th>SKU编码</th><th>内部库存</th><th>外部库存</th><th>同步数量</th><th>同步类型</th><th>状态</th><th>同步时间</th><th>错误信息</th></tr></thead>
      <tbody>${body}</tbody>
    </table></body></html>`
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

// ═══ 导出（后端真实 xlsx：GET /api/trade/inventory-sync/export） ═══
async function handleExport() {
  exporting.value = true
  try {
    const params = buildQuery()
    delete params.pageNum
    delete params.pageSize
    const blob: any = await inventorySyncApi.export(params)
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `库存同步记录_${dayjs().format('YYYYMMDD')}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

function handleError(error: Error) {
  console.error('[数据同步] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  fetchData()
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

.sync-body { flex: 1; min-height: 0; display: flex; flex-direction: column; padding: 12px 16px 0; }
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

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
