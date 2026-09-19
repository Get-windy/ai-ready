<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        推广历史查询（营销 → 营销推广 → 推广历史查询，菜单 80331）
        对标 ql361「营销 → 营销推广 → 推广历史查询」：单视图列表页，10 列（全部默认可见）
        列：分享时间 / 分享人 / 分享类型 / 分享概要 / 浏览次数 / 浏览人数 / 分享领取数 /
            下单人数 / 下单笔数 / 下单金额
        对标文档：docs/Yh-Spec/手动整理对标开发文档/营销模块/推广历史查询开发文档.md
        后端：/erp/marketing/share/page（mkt_share_record 分享触达台账）
        对标无「页面配置」弹窗
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              :loading="loading"
              @click="fetchList"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              size="small"
              :loading="exporting"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <span class="search-label">分享类型</span>
              <a-select
                v-model:value="searchForm.shareType"
                placeholder="全部类型"
                size="small"
                style="width: 140px"
                allow-clear
                :options="SHARE_TYPE_OPTIONS"
                @change="handleSearch"
              />
              <span class="search-label">分享人</span>
              <a-input
                v-model:value="searchForm.sharer"
                placeholder="请输入分享人"
                size="small"
                style="width: 150px"
                allow-clear
                @press-enter="handleSearch"
              />
              <span class="search-label">分享时间</span>
              <a-range-picker
                v-model:value="dateRange"
                size="small"
                style="width: 240px"
                value-format="YYYY-MM-DD"
                @change="handleSearch"
              />
              <a-button
                type="primary"
                size="small"
                @click="handleSearch"
              >
                查询
              </a-button>
            </div>
          </div>
        </template>

        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="id"
              storage-key="marketing-promote-history-table-columns"
              global-config-key="marketing-promote-history-table-columns"
            >
              <template #timeCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtTime(record.shareTime) }}</span>
              </template>
              <template #typeCell="{ record }">
                <span v-if="!record.__ghost">{{ SHARE_TYPE_MAP[record.shareType] || record.shareType || '-' }}</span>
              </template>
              <template #moneyCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtMoney(record.orderAmount) }}</span>
              </template>
            </BillDetailTable>
          </div>
        </template>

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
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { ReloadOutlined, PrinterOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { shareApi, SHARE_TYPE_MAP } from '@/api/marketing'

defineOptions({ name: 'MarketingPromoteHistory' })

const SHARE_TYPE_OPTIONS = Object.entries(SHARE_TYPE_MAP).map(([value, label]) => ({ value, label }))

const loading = ref(false)
const exporting = ref(false)
const tableData = ref<any[]>([])
const searchForm = reactive({ shareType: undefined as string | undefined, sharer: '' })
const dateRange = ref<any>(undefined)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// 对标 10 列（全部默认可见）
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'shareTime', title: '分享时间', type: 'slot', slotName: 'timeCell', width: 170 },
  { key: 'sharerName', title: '分享人', type: 'input', width: 130 },
  { key: 'shareType', title: '分享类型', type: 'slot', slotName: 'typeCell', width: 110 },
  { key: 'shareSummary', title: '分享概要', type: 'input', width: 320 },
  { key: 'viewCount', title: '浏览次数', type: 'input', width: 110 },
  { key: 'viewerCount', title: '浏览人数', type: 'input', width: 110 },
  { key: 'receiveCount', title: '分享领取数', type: 'input', width: 110 },
  { key: 'orderUserCount', title: '下单人数', type: 'input', width: 110 },
  { key: 'orderCount', title: '下单笔数', type: 'input', width: 110 },
  { key: 'orderAmount', title: '下单金额', type: 'slot', slotName: 'moneyCell', width: 120 },
]

function fmtTime(v: any): string {
  return v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-'
}
function fmtMoney(v: any): string {
  return v == null ? '-' : Number(v).toFixed(2)
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await shareApi.page({
      shareType: searchForm.shareType || undefined,
      sharer: searchForm.sharer || undefined,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[推广历史查询] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchList()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}
const PRINT_COLUMNS = [
  { key: 'shareTime', title: '分享时间' },
  { key: 'sharerName', title: '分享人' },
  { key: 'shareTypeText', title: '分享类型' },
  { key: 'shareSummary', title: '分享概要' },
  { key: 'viewCount', title: '浏览次数' },
  { key: 'viewerCount', title: '浏览人数' },
  { key: 'receiveCount', title: '分享领取数' },
  { key: 'orderUserCount', title: '下单人数' },
  { key: 'orderCount', title: '下单笔数' },
  { key: 'orderAmount', title: '下单金额' },
]
function printCell(row: any, key: string): string {
  if (key === 'shareTime') return fmtTime(row.shareTime)
  if (key === 'shareTypeText') return SHARE_TYPE_MAP[row.shareType] || row.shareType || ''
  if (key === 'orderAmount') return fmtMoney(row.orderAmount)
  return row[key] == null ? '' : String(row[key])
}

function handlePrint() {
  const rows = (tableData.value || []).filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const body = rows.map((r: any, i: number) => `<tr><td>${i + 1}</td>${
    PRINT_COLUMNS.map(c => `<td>${escapeHtml(printCell(r, c.key))}</td>`).join('')}</tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" /><title>推广历史查询</title>
    <style>body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px}
    h2{text-align:center;margin:0 0 12px;font-size:18px}
    table{width:100%;border-collapse:collapse;font-size:12px}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left}th{background:#f2f2f2}</style></head><body>
    <h2>推广历史查询</h2>
    <table><thead><tr><th>#</th>${PRINT_COLUMNS.map(c => `<th>${c.title}</th>`).join('')}</tr></thead>
    <tbody>${body}</tbody></table></body></html>`
  const win = window.open('', '_blank', 'width=1200,height=800')
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

function handleExport() {
  const rows = (tableData.value || []).filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  exporting.value = true
  try {
    const csv = '\uFEFF' + [PRINT_COLUMNS.map(c => c.title),
      ...rows.map((r: any) => PRINT_COLUMNS.map(c => printCell(r, c.key)))]
      .map(line => line.map(v => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\r\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `推广历史_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } finally {
    exporting.value = false
  }
}

function handleError(error: Error) {
  console.error('[推广历史查询] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 0; }
.search-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
