<template>
  <ErrorBoundary>
    <PageContainer full-height>
      <!-- ══ 顶栏：标题 + 日期快捷 + 操作按钮 ═══ -->
      <template #header>
        <div class="page-header">
          <div class="page-header__left">
            <span class="breadcrumb">仓储 / 销售价格跟踪</span>
            <h2>销售价格跟踪</h2>
          </div>
          <div class="page-header__right">
            <div class="date-shortcuts">
              <a
                v-for="s in dateShortcuts"
                :key="s.key"
                :class="['date-shortcut', { active: activeShortcut === s.key }]"
                @click="pickShortcut(s.key)"
              >{{ s.label }}</a>
            </div>
            <a-space :size="8">
              <a-button type="primary" size="small"><PlusOutlined /> 新增</a-button>
              <a-button size="small" :loading="loading" @click="loadData"><ReloadOutlined /> 刷新</a-button>
              <a-button size="small"><PrinterOutlined /> 打印(F8)</a-button>
              <a-dropdown>
                <a-button size="small">更多 <DownOutlined /></a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handleExport"><ExportOutlined /> 导出</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </div>
        </div>
      </template>

      <!-- ═══ 搜索区 ═══ -->
      <div class="search-bar">
        <a-form layout="inline">
          <a-form-item>
            <a-range-picker
              v-model:value="dateRange"
              style="width: 240px"
              value-format="YYYY-MM-DD"
            />
          </a-form-item>
          <a-form-item>
            <a-input
              v-model:value="queryValues.partnerName"
              placeholder="往来单位"
              allow-clear
              style="width: 160px"
            />
          </a-form-item>
          <a-form-item>
            <a-input
              v-model:value="queryValues.productName"
              placeholder="商品"
              allow-clear
              style="width: 160px"
            />
          </a-form-item>
          <a-form-item>
            <a-select
              v-model:value="queryValues.unitType"
              placeholder="单位类型"
              allow-clear
              style="width: 120px"
            >
              <a-select-option value="">全部</a-select-option>
              <a-select-option value="small">小单位</a-select-option>
              <a-select-option value="big">大单位</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="handleSearch">查询</a-button>
          </a-form-item>
          <a-form-item>
            <a-checkbox v-model:checked="showSelected">仅显示已选中</a-checkbox>
          </a-form-item>
        </a-form>
      </div>

      <!-- ═══ 主体：分类树 + 表格 ═══ -->
      <div class="main-content">
        <div class="category-tree">
          <a-tree
            v-model:expandedKeys="expandedKeys"
            v-model:selectedKeys="selectedKeys"
            :tree-data="categoryTree"
            @select="handleCategorySelect"
            block-node
            default-expand-all
          />
        </div>
        <div class="table-area">
          <a-table
            :columns="columns"
            :data-source="tableData"
            :loading="loading"
            :pagination="paginationConfig"
            row-key="_rk"
            size="small"
            bordered
            :scroll="{ x: 1400 }"
            @change="handleTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'action'">
                <a-button type="link" size="small">修改</a-button>
                <a-button type="link" size="small" danger>删除</a-button>
              </template>
              <template v-else-if="column.dataIndex === 'recentPrice' || column.dataIndex === 'recentDiscountRate'">
                {{ Number(record[column.dataIndex] ?? 0).toFixed(2) }}
              </template>
            </template>
          </a-table>
        </div>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  PlusOutlined, ReloadOutlined, PrinterOutlined, DownOutlined,
  ExportOutlined, SearchOutlined, ClearOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import request from '@/utils/request'

defineOptions({ name: 'SalesPriceTrack' })

// ═══ 日期快捷 ═══
const dateShortcuts = [
  { key: 'yesterday', label: '昨日' },
  { key: 'today', label: '今日' },
  { key: 'thisWeek', label: '本周' },
  { key: 'thisWeek2', label: '近一周' },
  { key: 'thisMonth', label: '本月' },
  { key: 'lastMonth', label: '上月' },
  { key: 'last3Month', label: '近三月' },
  { key: 'thisYear', label: '本年' },
]
const activeShortcut = ref('thisMonth')
const dateRange = ref<[string, string] | null>(null)

function calcRange(key: string): [string, string] {
  const now = dayjs()
  switch (key) {
    case 'yesterday': return [now.subtract(1, 'day').format('YYYY-MM-DD'), now.subtract(1, 'day').format('YYYY-MM-DD')]
    case 'today': return [now.format('YYYY-MM-DD'), now.format('YYYY-MM-DD')]
    case 'thisWeek': return [now.startOf('week').add(1, 'day').format('YYYY-MM-DD'), now.format('YYYY-MM-DD')]
    case 'thisWeek2': return [now.subtract(6, 'day').format('YYYY-MM-DD'), now.format('YYYY-MM-DD')]
    case 'thisMonth': return [now.startOf('month').format('YYYY-MM-DD'), now.format('YYYY-MM-DD')]
    case 'lastMonth': return [now.subtract(1, 'month').startOf('month').format('YYYY-MM-DD'), now.subtract(1, 'month').endOf('month').format('YYYY-MM-DD')]
    case 'last3Month': return [now.subtract(2, 'month').startOf('month').format('YYYY-MM-DD'), now.format('YYYY-MM-DD')]
    case 'thisYear': return [now.startOf('year').format('YYYY-MM-DD'), now.format('YYYY-MM-DD')]
    default: return [now.subtract(6, 'day').format('YYYY-MM-DD'), now.format('YYYY-MM-DD')]
  }
}

function pickShortcut(key: string) {
  activeShortcut.value = key
  dateRange.value = calcRange(key)
  loadData()
}

// ═══ 查询条件 ═══
const queryValues = reactive({
  partnerName: '',
  productName: '',
  unitType: '',
})
const showSelected = ref(false)

// ═══ 商品分类树 ═══
const expandedKeys = ref<string[]>([])
const selectedKeys = ref<string[]>([])
const categoryTree = ref<any[]>([
  { title: '全部商品', key: 'all', children: [
    { title: '饮品原料', key: 'drink' },
    { title: '低温熟食', key: 'cold' },
    { title: '调理冻品', key: 'frozen' },
    { title: '米面制品', key: 'flour' },
    { title: '烘焙西点', key: 'bakery' },
    { title: '预制品', key: 'premade' },
    { title: '常温食品', key: 'normal' },
    { title: '生鲜冻品', key: 'fresh' },
    { title: '包装耗材', key: 'package' },
    { title: '吧台用品', key: 'bar' },
    { title: '餐具器皿', key: 'tableware' },
    { title: '餐饮设备', key: 'equipment' },
    { title: '其他', key: 'other' },
  ]},
])

function handleCategorySelect(keys: any[]) {
  selectedKeys.value = keys
}

// ═══ 表格列（对标 13 列） ═══
const columns = [
  { title: '操作', key: 'action', width: 100, fixed: 'left' as const },
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 180, ellipsis: true },
  { title: '货号', dataIndex: 'productCode', key: 'productCode', width: 90 },
  { title: '商品单位', dataIndex: 'unit', key: 'unit', width: 70 },
  { title: '条码', dataIndex: 'barcode', key: 'barcode', width: 110 },
  { title: '规格', dataIndex: 'specification', key: 'specification', width: 90, ellipsis: true },
  { title: '型号', dataIndex: 'model', key: 'model', width: 90 },
  { title: '产地', dataIndex: 'origin', key: 'origin', width: 90 },
  { title: '往来单位编号', dataIndex: 'partnerCode', key: 'partnerCode', width: 110 },
  { title: '往来单位名称', dataIndex: 'partnerName', key: 'partnerName', width: 160, ellipsis: true },
  { title: '最近销售价', dataIndex: 'recentPrice', key: 'recentPrice', width: 100, align: 'right' as const },
  { title: '最近销售折扣（%）', dataIndex: 'recentDiscountRate', key: 'recentDiscountRate', width: 130, align: 'right' as const },
  { title: '最近销售日期', dataIndex: 'recentSaleDate', key: 'recentSaleDate', width: 110 },
]

// ═══ 数据状态 ═══
const tableData = ref<any[]>([])
const loading = ref(false)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const paginationConfig = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条记录`,
  showQuickJumper: true,
}))

// ═══ 数据加载 ═══
async function loadData() {
  loading.value = true
  try {
    const res: any = await request.get('/sales/price-track/recent-price/page', {
      params: {
        current: pagination.current,
        size: pagination.pageSize,
        productName: queryValues.productName || undefined,
        partnerName: queryValues.partnerName || undefined,
        startDate: dateRange.value?.[0],
        endDate: dateRange.value?.[1],
      },
    })
    // request 拦截器对 Page 结构直接返回 {records,total}，对数组直接返回数组
    const page: any = res?.records ? res : { records: Array.isArray(res) ? res : [], total: res?.total }
    const records = (page.records || []) as any[]
    tableData.value = records.map((r: any, i: number) => ({
      ...r,
      _rk: `${r.productId}-${r.partnerId}-${i}`,
    }))
    pagination.total = Number(page.total) || 0
  } catch {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleTableChange(pag: { current?: number; pageSize?: number }) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 20
  loadData()
}

function handleExport() {
  const header = columns.map(c => c.title).join(',')
  const lines = tableData.value.map(r => [
    '', r.productName || '', r.productCode || '', r.unit || '', r.barcode || '',
    r.specification || '', r.model || '', r.origin || '', r.partnerCode || '',
    r.partnerName || '', r.recentPrice ?? 0, r.recentDiscountRate ?? 0, r.recentSaleDate || '',
  ].join(','))
  const csv = '\uFEFF' + [header, ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `销售价格跟踪-${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(() => {
  dateRange.value = calcRange('thisMonth')
  loadData()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background: #fff;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  flex-wrap: wrap;
  gap: 12px;
}
.page-header__left .breadcrumb { font-size: 12px; color: #999; }
.page-header__left h2 { font-size: 18px; font-weight: 600; color: #303133; margin: 4px 0 0; }
.page-header__right { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.date-shortcuts { display: flex; gap: 4px; align-items: center; }
.date-shortcut {
  font-size: 12px; color: #606266; padding: 2px 8px; border-radius: 4px; cursor: pointer;
}
.date-shortcut:hover { color: #1890ff; }
.date-shortcut.active { background: #e6f4ff; color: #1890ff; font-weight: 600; }
.search-bar {
  background: #fff; border-radius: 8px; padding: 12px 16px; margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.main-content {
  display: flex; gap: 12px; height: calc(100vh - 260px);
}
.category-tree {
  width: 180px; background: #fff; border-radius: 8px; padding: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06); overflow-y: auto; flex-shrink: 0;
}
.table-area {
  flex: 1; background: #fff; border-radius: 8px; padding: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06); overflow: auto;
}
</style>
