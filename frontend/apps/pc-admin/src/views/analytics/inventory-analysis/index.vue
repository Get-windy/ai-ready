<template>
  <div
    class="inv-analysis-page"
    @table-expand-change="onTableExpandChange"
  >
    <!-- ═══ 视图 Tab（对标：按商品/仓库调拨分析/商品调拨分析） ═══ -->
    <div class="view-tabs">
      <a-tabs v-model:active-key="activeView" size="small">
        <a-tab-pane key="product" tab="按商品" />
        <a-tab-pane key="warehouse" tab="仓库调拨分析" />
        <a-tab-pane key="productTransfer" tab="商品调拨分析" />
      </a-tabs>
      <div class="view-toolbar">
        <a-button size="small" @click="message.info('导出已触发')">导出</a-button>
      </div>
    </div>

    <!-- ═══ 查询区（对标：日期快捷+仓库/商品/品牌/是否发生业务/数量显示） ═══ -->
    <div class="query-area">
      <a-range-picker v-model:value="dateRange" size="small" style="width: 240px" />
      <a-select v-model:value="query.warehouseId" placeholder="仓库" allow-clear size="small" style="width: 130px" :options="warehouseOptions" />
      <a-input v-model:value="query.productName" placeholder="商品" allow-clear size="small" style="width: 150px" />
      <a-input v-model:value="query.brand" placeholder="品牌" allow-clear size="small" style="width: 120px" />
      <a-select v-model:value="query.occurred" placeholder="是否发生业务" allow-clear size="small" style="width: 130px" :options="occurredOptions" />
      <a-select v-model:value="query.qtyType" placeholder="数量显示" allow-clear size="small" style="width: 110px" :options="qtyTypeOptions" />
      <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
      <a-button size="small" @click="handleReset">重置</a-button>
    </div>

    <!-- ═══ 数据表格（多级表头：期初/五类入库/五类出库/期末，分项列待后端扩容） ═══ -->
    <div class="table-wrap">
      <BillDetailTable
        :columns="columns"
        v-model:data-source="tableData"
        :loading="loading"
        :view-mode="true"
        :storage-key="'inventory-analysis-columns'"
        style="height: 100%"
      >
        <template #actionCell="{ record }">
          <a-button type="link" size="small" @click="message.info('对账待对接')">对账</a-button>
        </template>
        <template #productNameCell="{ record }">
          <span :class="{ 'stopped-product': record.stopped }">{{ record.productName }}</span>
        </template>
      </BillDetailTable>
      <div v-if="!tableExpanded" class="table-pagination">
        <StandardPagination
          variant="classic"
          :current="pagination.current"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          @change="onPageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { stockReportApi, stockApi } from '@/api/analytics'
// 自建页面骨架（无 CategoryListLayout 等宿主布局）：自己当展开联动的宿主，收起下方分页区
import { useTableExpandHost } from '@/composables/useTableExpandHost'

defineOptions({ name: 'InventoryAnalysis' })

const { tableExpanded, onTableExpandChange } = useTableExpandHost()

const activeView = ref('product')
const loading = ref(false)
const tableData = ref<any[]>([])
const warehouseOptions = ref<{ label: string; value: number }[]>([])
const occurredOptions = [
  { label: '全部', value: undefined },
  { label: '有业务', value: 1 },
  { label: '无业务', value: 0 },
]
const qtyTypeOptions = [
  { label: '数量', value: 'qty' },
  { label: '小单位数量', value: 'smallQty' },
]
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().startOf('week'), dayjs()])
const query = reactive({
  warehouseId: undefined as number | undefined,
  productName: '',
  brand: '',
  occurred: undefined as number | undefined,
  qtyType: 'qty' as string,
})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 表格列（多级表头骨架：期初/五类入库/五类出库/期末；分项列当前后端未返回，先预留展示汇总列） ═══
const columns: DetailColumnConfig[] = [
  { key: 'action', title: '操作', width: 70, fixed: 'left', slotName: 'actionCell' },
  { key: 'productCode', title: '货号', width: 110 },
  { key: 'productName', title: '商品名称', width: 200, slotName: 'productNameCell' },
  { key: 'warehouseName', title: '仓库', width: 120 },
  // 期初
  { key: 'openingQty', title: '期初数量', width: 100, align: 'right' },
  { key: 'openingAmt', title: '期初金额', width: 110, align: 'right' },
  // 五类入库（分项待后端扩容）
  { key: 'purchaseInQty', title: '采购入库数量', width: 110, align: 'right' },
  { key: 'transferInQty', title: '调拨入库数量', width: 110, align: 'right' },
  { key: 'otherInQty', title: '其他入库数量', width: 110, align: 'right' },
  { key: 'borrowInQty', title: '借进入库数量', width: 110, align: 'right' },
  { key: 'saleReturnInQty', title: '销退入库数量', width: 110, align: 'right' },
  { key: 'inQty', title: '入库合计', width: 100, align: 'right' },
  // 五类出库（分项待后端扩容）
  { key: 'saleOutQty', title: '销售出库数量', width: 110, align: 'right' },
  { key: 'transferOutQty', title: '调拨出库数量', width: 110, align: 'right' },
  { key: 'otherOutQty', title: '其他出库数量', width: 110, align: 'right' },
  { key: 'borrowOutQty', title: '借出出库数量', width: 110, align: 'right' },
  { key: 'purchaseReturnOutQty', title: '采退出库数量', width: 110, align: 'right' },
  { key: 'outQty', title: '出库合计', width: 100, align: 'right' },
  // 期末
  { key: 'closingQty', title: '期末数量', width: 100, align: 'right' },
  { key: 'closingAmt', title: '期末金额', width: 110, align: 'right' },
]

/** 分页变化：StandardPagination 不像 antd 分页那样自带 v-model 双绑，需先回写分页状态再取数 */
function onPageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current, pageSize: pagination.pageSize,
      startDate: dateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: dateRange.value?.[1]?.format('YYYY-MM-DD'),
      ...query,
    }
    Object.keys(params).forEach(k => { if (params[k] === undefined || params[k] === null || params[k] === '') delete params[k] })
    const res: any = await stockReportApi.invSummaryPage(params)
    const data = res?.data || res || {}
    tableData.value = data?.records || data?.list || []
    pagination.total = Number(data?.total) || 0
  } catch (e) {
    console.warn('[进销存分析] 获取数据失败', e)
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handleReset = () => {
  Object.assign(query, { warehouseId: undefined, productName: '', brand: '', occurred: undefined, qtyType: 'qty' })
  dateRange.value = [dayjs().startOf('week'), dayjs()]
  handleSearch()
}

onMounted(async () => {
  try {
    const list: any = await stockApi.getWarehouses()
    warehouseOptions.value = (Array.isArray(list) ? list : []).map((w: any) => ({ label: w.warehouseName, value: w.id }))
  } catch { /* ignore */ }
  fetchData()
})
</script>

<style scoped>
.inv-analysis-page { display: flex; flex-direction: column; height: 100%; padding: 8px 12px; gap: 8px; }
.view-tabs { display: flex; justify-content: space-between; align-items: center; }
.view-tabs :deep(.ant-tabs) { margin-bottom: 0; }
.view-toolbar { flex-shrink: 0; }
.query-area { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; padding: 8px; background: #fff; border-radius: 4px; }
.table-wrap { flex: 1; overflow: hidden; display: flex; flex-direction: column; background: #fff; border-radius: 4px; }
/* 分页栏统一走 StandardPagination 经典形态（自带边框/内边距/居中），这里只保留让位 */
.table-pagination { flex-shrink: 0; }
.stopped-product { color: #ff4d4f; }
</style>
