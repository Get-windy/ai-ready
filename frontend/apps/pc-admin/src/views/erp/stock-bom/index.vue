<template>
  <ErrorBoundary @reset="fetchData" @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="bom-header">
          <div class="bom-header__left">
            <span class="bom-header__breadcrumb">ERP / 库存管理 / 生产模板</span>
            <h2 class="bom-header__title">生产模板-列表</h2>
          </div>
          <div class="bom-header__right">
            <a-space :size="8">
              <!-- 列配置走数据表表头齿轮 -->
              <a-button type="primary" size="small" @click="handleCreate">
                <PlusOutlined /> 新增模板
              </a-button>
              <a-button size="small" @click="debounceClick('refresh', fetchData)">
                <ReloadOutlined /> 刷新
              </a-button>
              <a-button size="small" @click="handlePrintF8">
                <PrinterOutlined /> 打印(F8)
              </a-button>
              <a-button size="small" @click="handleExport">
                <ExportOutlined /> 导出
              </a-button>
            </a-space>
          </div>
        </div>
      </template>

      <!-- ═══ 搜索区 ═══ -->
      <div class="search-row">
        <a-input
          v-model:value="searchFilters.keyword"
          placeholder="模板名称"
          allow-clear
          size="small"
          style="width: 180px"
          @press-enter="handleSearch"
        />
        <a-input
          v-model:value="searchFilters.productName"
          placeholder="商品"
          allow-clear
          size="small"
          style="width: 180px"
          @press-enter="handleSearch"
        />
        <a-button type="primary" size="small" @click="handleSearch">
          查询
        </a-button>
      </div>

      <!-- ═══ 表格 ═══ -->
      <BillTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableData"
        :storage-key="'stock-bom-table-columns'"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :selectable="false"
        :show-toolbar="false"
        :show-search="false"
        :show-add="false"
        style="flex: 1; margin-top: 12px;"
        @page-change="handlePageChange"
      >
        <template #action="{ record }">
          <a-space :size="4">
            <a-tooltip title="查看">
              <a-button type="link" size="small" @click="handleView(record)">
                <EyeOutlined />
              </a-button>
            </a-tooltip>
            <a-tooltip title="编辑">
              <a-button type="link" size="small" @click="handleEdit(record)">
                <EditOutlined />
              </a-button>
            </a-tooltip>
            <a-tooltip title="删除">
              <a-button type="link" size="small" danger @click="handleDelete(record)">
                <DeleteOutlined />
              </a-button>
            </a-tooltip>
          </a-space>
        </template>
      </BillTableList>
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="erp-stock-bom"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined, ReloadOutlined, PrinterOutlined, ExportOutlined,
  EyeOutlined, EditOutlined, DeleteOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { stockBomApi } from '@/api/erp'
import request from '@/utils/request'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'StockBomList' })

// ── 路由 ──
const router = useRouter()

// ── 防抖 ──
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

// ── 状态 ──
const loading = ref(false)
const tableData = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({
  current: 1, pageSize: 20, total: 0,
  showSizeChanger: true, showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

// ── 列表列定义（对齐 ql361 生产模板 9 列，操作列为独立固定列，不参与列配置） ──
const columnDefs: any[] = [
  { key: 'bomName', field: 'bomName', title: '模板名称', width: 200 },
  { key: 'productName', field: 'productName', title: '商品名称', width: 160 },
  { key: 'productCode', field: 'productCode', title: '货号', width: 120 },
  { key: 'productUnit', field: 'productUnit', title: '单位', width: 80 },
  { key: 'barcode', field: 'barcode', title: '条码', width: 120, defaultHidden: true },
  { key: 'productSpec', field: 'productSpec', title: '规格', width: 100, defaultHidden: true },
  { key: 'model', field: 'model', title: '型号', width: 100, defaultHidden: true },
  { key: 'origin', field: 'origin', title: '产地', width: 100, defaultHidden: true },
  { key: 'brand', field: 'brand', title: '品牌', width: 100, defaultHidden: true },
]

// 序号列（表头齿轮 = 列配置唯一入口）+ 操作列（固定显示，不参与列配置）
const vxeColumns = computed<any[]>(() => [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  ...columnDefs,
  { key: 'action', field: 'action', title: '操作', width: 150, fixed: 'right', type: 'action' },
])

// ── 数据获取 ──
async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/erp/stock/bom/page', {
      params: {
        keyword: searchFilters.keyword || undefined,
        productName: searchFilters.productName || undefined,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      },
    })
    const data = res?.data ?? res
    tableData.value = data?.records || []
    pagination.total = data?.total || 0
  } catch (err: any) {
    message.error(err?.response?.data?.message || '获取列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

// ── 跳转 ──
function handleCreate() {
  router.push('/erp/stock-bom/form')
}

function handleEdit(record: any) {
  router.push(`/erp/stock-bom/form/${record.id}`)
}

function handleView(record: any) {
  router.push(`/erp/stock-bom/form/${record.id}`)
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确认删除生产模板「${record.bomName}」吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockBomApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch (err: any) {
        message.error(err?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ── 打印(F8) ──
// ═══ 打印（结果集打印） ═══
// 原先是 window.print() —— 打出来是整个后台界面（菜单/工具栏/翻页都跟着上纸）。
// 列取页面自己的列定义，模板按数据里的列画表头（改列不用改模板）。
const { printDialogRef, printData, handlePrint: handlePrintF8 } = useListPrint({
  pageCode: 'erp-stock-bom',
  // 列是 computed（随 Tab / 列配置变），静态生成器写不进模板 → 明确按数据列打
  useDataColumns: true,
  title: 'BOM 清单',
  columns: () => vxeColumns,
  rows: () => tableData.value,
  emptyTip: '没有可打印的数据',
})

// ── 导出（前端 CSV） ──
function handleExport() {
  const data = tableData.value
  if (data.length === 0) {
    message.warning('没有可导出的数据')
    return
  }
  const headers = ['模板名称', '商品名称', '货号', '单位', '条码', '规格', '型号', '产地', '品牌']
  const lines = data.map((row: any) => [
    row.bomName, row.productName, row.productCode, row.productUnit,
    row.barcode, row.productSpec, row.model, row.origin, row.brand,
  ].map((v) => `"${(v ?? '').toString().replace(/"/g, '""')}"`).join(','))
  const csv = [headers.join(','), ...lines].join('\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `生产模板_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

function handleError(err: any) {
  console.warn('[生产模板] ErrorBoundary:', err)
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.bom-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.bom-header__left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.bom-header__breadcrumb {
  font-size: 12px;
  color: #999;
}
.bom-header__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.bom-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.search-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  margin-bottom: 12px;
}
</style>
