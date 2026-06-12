<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="page-header">
          <div class="page-header__left">
            <a-breadcrumb>
              <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
              <a-breadcrumb-item>序列号管理</a-breadcrumb-item>
            </a-breadcrumb>
            <h2 class="page-header__title">序列号管理</h2>
          </div>
          <div class="page-header__right">
            <a-space :size="12">
              <a-button size="small" @click="router.push('/erp/batch')">批次管理</a-button>
              <span class="data-status">
                <a-badge :status="loading ? 'processing' : 'success'" />
                <span v-if="lastUpdateTime" class="update-time">数据更新: {{ lastUpdateTime }}</span>
                <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
                  <SyncOutlined /> {{ autoRefreshCountdown }}s
                </span>
              </span>
              <a-button size="small" :loading="loading" @click="debounceClick('refresh', fetchData)">
                <template #icon><ReloadOutlined /></template>
                刷新
              </a-button>
              <a-button size="small" v-permission="'erp:serial:export'" @click="debounceClick('export', handleExport)">
                <template #icon><DownloadOutlined /></template>
                导出
              </a-button>
              <span class="shortcut-hints">
                <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
                <span class="shortcut-hint"><kbd>Ctrl+E</kbd> 导出</span>
              </span>
            </a-space>
          </div>
        </div>
      </template>

      <!-- 统计卡片 -->
      <a-row :gutter="12" style="margin-bottom: 12px;">
        <a-col :span="6">
          <div class="stat-card" style="border-top: 3px solid #1890ff;">
            <div class="stat-value" style="color:#1890ff">{{ statistics.total }}</div>
            <div class="stat-label">总序列号</div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card" style="border-top: 3px solid #52c41a;">
            <div class="stat-value" style="color:#52c41a">{{ statistics.available }}</div>
            <div class="stat-label">可用</div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card" style="border-top: 3px solid #faad14;">
            <div class="stat-value" style="color:#faad14">{{ statistics.inUse }}</div>
            <div class="stat-label">使用中</div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card" style="border-top: 3px solid #ff4d4f;">
            <div class="stat-value" style="color:#ff4d4f">{{ statistics.scrap }}</div>
            <div class="stat-label">报废</div>
          </div>
        </a-col>
      </a-row>

      <!-- 搜索与表格 -->
      <a-card :body-style="{ padding: '12px' }">
        <div class="serial-toolbar">
          <SearchBar
            :fields="searchFields"
            :loading="loading"
            @search="handleSearch"
            @reset="handleReset"
          />
        </div>

        <vxe-table
          ref="tableRef"
          :data="pagedData"
          :loading="loading"
          border
          size="small"
          align="center"
          height="auto"
          :row-config="{ keyField: 'id' }"
        >
          <template #empty>
            <EmptyState v-if="loading" image="no-data" title="加载中..." description="" :show-actions="false" size="small" />
            <EmptyState v-else image="no-data" title="暂无序列号" description="当前没有序列号数据" size="small" @refresh="fetchData" />
          </template>
          <vxe-column type="seq" title="#" width="50" />
          <vxe-column field="serialNo" title="序列号" min-width="160" />
          <vxe-column field="productCode" title="产品编码" width="120" />
          <vxe-column field="productName" title="产品名称" min-width="140" />
          <vxe-column field="snStatus" title="状态" width="80">
            <template #default="{ row }">
              <StatusTag :status="row.snStatus" :map="SERIAL_STATUS" />
            </template>
          </vxe-column>
          <vxe-column field="snStage" title="阶段" width="90">
            <template #default="{ row }">
              <StatusTag :status="row.snStage" :map="SERIAL_STAGE" />
            </template>
          </vxe-column>
          <vxe-column field="batchNo" title="批次号" width="130" />
          <vxe-column field="warehouseName" title="仓库" width="120" />
          <vxe-column field="warrantyEndDate" title="质保到期" width="110">
            <template #default="{ row }">
              <span v-if="row.warrantyEndDate" :style="warrantyWarningStyle(row.warrantyEndDate)">
                {{ row.warrantyEndDate }}
              </span>
              <span v-else>-</span>
            </template>
          </vxe-column>
          <vxe-column field="manufacturer" title="制造商" width="140">
            <template #default="{ row }">{{ row.manufacturer || '-' }}</template>
          </vxe-column>
          <vxe-column title="操作" width="220" fixed="right">
            <template #default="{ row }">
              <a-space :size="4">
                <a-button type="link" size="small" @click="showDetail(row.id)"><EyeOutlined /> 详情</a-button>
                <a-button type="link" size="small" @click="showDetail(row.id)"><EditOutlined /> 追溯</a-button>
                <PrintButton :record="row" :business-id="row.id" business-type="serial" button-type="link" button-size="small" tooltip="打印" />
                <a-button type="link" size="small" v-permission="'erp:serial:delete'" danger @click="handleDelete(row)"><DeleteOutlined /> 删除</a-button>
              </a-space>
            </template>
          </vxe-column>
        </vxe-table>

        <div class="serial-pagination">
          <a-pagination
            v-model:current="pagination.current"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="['10', '20', '50', '100']"
            show-size-changer
            show-quick-jumper
            :show-total="(total, range) => `共 ${total} 条`"
            size="small"
            @change="onPageChange"
          />
        </div>
      </a-card>

      <!-- 详情抽屉 -->
      <SerialDetailDrawer v-model:visible="detailVisible" :serial-id="selectedSerialId" />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { ReloadOutlined, EyeOutlined, EditOutlined, SyncOutlined, DownloadOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import EmptyState from '@/components/EmptyState/EmptyState.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import { SERIAL_STATUS, SERIAL_STAGE } from '@/utils/statusConfig'
import { serialApi, type SerialNumber } from '@/api/erp/batch'
import SerialDetailDrawer from './detail.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import request from '@/utils/request'

// ── 类型 ──────────────────────────────────────────────
interface StatSummary {
  total: number
  available: number
  inUse: number
  inservice: number
  maintained: number
  scrap: number
}

// ── 防抖 ──────────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

// ── 键盘快捷键 ─────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') { e.preventDefault(); debounceClick('export', handleExport); return }
}

const router = useRouter()
const loading = ref(false)
const lastUpdateTime = ref('')
const allData = ref<SerialNumber[]>([])
const detailVisible = ref(false)
const selectedSerialId = ref<number>(0)
const tableRef = ref()
const autoRefreshCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null
let refreshTimer: ReturnType<typeof setInterval> | null = null

const statistics = ref<StatSummary>({ total: 0, available: 0, inUse: 0, inservice: 0, maintained: 0, scrap: 0 })

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: true,
})

const searchFields: any = [
  { key: 'serialNo', label: '序列号', type: 'input', span: 1 },
  { key: 'productCode', label: '产品编码', type: 'input', span: 1 },
  { key: 'batchNo', label: '批次号', type: 'input', span: 1 },
  { key: 'status', label: '状态', type: 'select', span: 1, options: [
    { value: '', label: '全部' },
    { value: 'AVAILABLE', label: '可用' },
    { value: 'IN_USE', label: '使用中' },
    { value: 'INSERVICE', label: '售后中' },
    { value: 'MAINTAINED', label: '维修中' },
    { value: 'SCRAP', label: '报废' },
  ]},
]

// 当前页数据（客户端分页）
const pagedData = computed(() => {
  const start = (pagination.current - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  return allData.value.slice(start, end)
})

// ── 状态/阶段标签映射 ──
function warrantyWarningStyle(dateStr: string): Record<string, string> | undefined {
  if (!dateStr) return undefined
  const now = new Date()
  const end = new Date(dateStr)
  const diffDays = Math.ceil((end.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
  if (diffDays <= 30) return { color: '#f5222d', fontWeight: 'bold' }
  return undefined
}

function handleError(err: any) {
  console.warn('[序列号] ErrorBoundary 捕获异常:', err)
}

// ── 数据请求 ──
const searchParams = reactive({ serialNo: '', productCode: '', batchNo: '', status: '' })

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {}
    if (searchParams.serialNo) params.serialNo = searchParams.serialNo
    if (searchParams.productCode) params.productCode = searchParams.productCode
    if (searchParams.batchNo) params.batchNo = searchParams.batchNo
    if (searchParams.status) params.status = searchParams.status

    const result = await serialApi.page({ ...params, pageNum: 1, pageSize: 9999 })
    allData.value = result || []
    pagination.total = allData.value.length
    pagination.current = 1

    // 统计
    const stats: StatSummary = { total: allData.value.length, available: 0, inUse: 0, inservice: 0, maintained: 0, scrap: 0 }
    allData.value.forEach((s) => {
      if (s.snStatus === 'AVAILABLE') stats.available++
      else if (s.snStatus === 'IN_USE') stats.inUse++
      else if (s.snStatus === 'INSERVICE') stats.inservice++
      else if (s.snStatus === 'MAINTAINED') stats.maintained++
      else if (s.snStatus === 'SCRAP') stats.scrap++
    })
    statistics.value = stats
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
  } catch (err) {
    console.warn('[序列号] 加载失败', err)
    message.error('加载序列号数据失败')
    allData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch(values: Record<string, any>) {
  Object.assign(searchParams, values)
  fetchData()
}

function handleReset() {
  searchParams.serialNo = ''
  searchParams.productCode = ''
  searchParams.batchNo = ''
  searchParams.status = ''
  fetchData()
}

function onPageChange() {
  // pagedData computed 自动响应
}

function showDetail(id: number) {
  selectedSerialId.value = id
  detailVisible.value = true
}

async function handleExport() {
  try {
    const blob = await request.get('/erp/batch-sn/serials/export', { responseType: 'blob' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url; a.download = `序列号_${new Date().toISOString().slice(0, 10)}.xlsx`; a.click()
    window.URL.revokeObjectURL(url); message.success('导出成功')
  } catch {
    message.warning('导出失败')
  }
}

function handleDelete(row: SerialNumber) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除序列号 "${row.serialNo}" 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await request.delete(`/erp/batch-sn/serials/${row.id}`)
        message.success('删除成功')
        fetchData()
      } catch (err: any) {
        message.error(err?.message || '删除失败')
      }
    }
  })
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  fetchData()
  autoRefreshCountdown.value = 300
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 300
  }, 300000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.page-header__left { display: flex; flex-direction: column; gap: 2px; }
.page-header__breadcrumb { font-size: 12px; color: #999; }
.page-header__title { font-size: 18px; font-weight: 600; color: #303133; margin: 4px 0 0 0; }
.page-header__right { display: flex; align-items: center; gap: 12px; }

.data-status { display: inline-flex; align-items: center; gap: 6px; }
.update-time { font-size: 12px; color: #999; white-space: nowrap; }
.auto-refresh-badge {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 12px; color: #909399;
  padding: 2px 8px; border-radius: 4px;
  background: #f5f7fa; user-select: none;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.stat-value { font-size: 22px; font-weight: 700; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace; line-height: 1.2; }
.stat-label { font-size: 13px; color: #666; margin-top: 4px; }

.serial-toolbar {
  margin-bottom: 12px;
}
.serial-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
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
