<template>
  <div
    class="check-stock-page"
    @table-expand-change="onTableExpandChange"
  >
    <!-- ═══ 视图 Tab（对标：当前库存/按属性/库存分布） ═══ -->
    <div class="view-tabs">
      <a-tabs v-model:active-key="activeView" size="small">
        <a-tab-pane key="current" tab="当前库存" />
        <a-tab-pane key="attribute" tab="按属性" />
        <a-tab-pane key="distribution" tab="库存分布" />
      </a-tabs>
      <!-- 工具栏：批量上架/批量下架 -->
      <a-space class="view-toolbar" :size="8">
        <a-button size="small" @click="message.info('批量上架待对接商城接口')">批量上架</a-button>
        <a-button size="small" @click="message.info('批量下架待对接商城接口')">批量下架</a-button>
      </a-space>
    </div>

    <!-- ═══ 查询区 ═══ -->
    <div class="query-area">
      <a-input v-model:value="query.keyword" placeholder="商品名称/货号/条码/规格/型号" allow-clear size="small" style="width: 240px" />
      <a-select v-model:value="query.warehouseId" placeholder="仓库" allow-clear size="small" style="width: 140px" :options="warehouseOptions" />
      <a-input v-model:value="query.brand" placeholder="品牌" allow-clear size="small" style="width: 120px" />
      <a-select v-model:value="query.onShelf" placeholder="上架状态" allow-clear size="small" style="width: 120px" :options="onShelfOptions" />
      <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
      <a-button size="small" @click="handleReset">重置</a-button>
    </div>

    <!-- ═══ 左侧分类树 + 数据表格（对标：商品分类树 13 分类） ═══ -->
    <div class="content-area">
      <div class="category-panel">
        <div class="category-title">商品分类</div>
        <a-tree
          :tree-data="categoryTree"
          :loading="categoryLoading"
          :selected-keys="selectedCategoryId ? [String(selectedCategoryId)] : []"
          default-expand-all
          :selectable="true"
          @select="onCategorySelect"
        />
      </div>
    <div class="table-wrap">
      <BillDetailTable
        :columns="columns"
        v-model:data-source="tableData"
        :loading="loading"
        :view-mode="true"
        :storage-key="'check-stock-columns'"
        style="height: 100%"
      >
        <template #actionCell="{ record }">
          <a-space :size="2">
            <a-button type="link" size="small" @click="message.info('分布视图待对接')">分布</a-button>
            <a-button type="link" size="small" @click="handleDetail(record)">明细</a-button>
            <a-button type="link" size="small" @click="message.info('对账待对接')">对账</a-button>
            <a-button type="link" size="small" @click="message.info('批次号待对接')">批次号</a-button>
          </a-space>
        </template>
        <template #productNameCell="{ record }">
          <span :class="{ 'zero-stock': !Number(record.quantity) }">{{ record.productName }}</span>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { stockApi } from '@/api/erp'
import { productCategoryApi } from '@/api/erp/product'
// 自建页面骨架（无 CategoryListLayout 等宿主布局）：自己当展开联动的宿主，收起下方分页区
import { useTableExpandHost } from '@/composables/useTableExpandHost'

defineOptions({ name: 'CheckStock' })

const { tableExpanded, onTableExpandChange } = useTableExpandHost()

const activeView = ref('current')
const loading = ref(false)
const tableData = ref<any[]>([])
const warehouseOptions = ref<{ label: string; value: number }[]>([])
const onShelfOptions = [
  { label: '已上架', value: 1 },
  { label: '未上架', value: 0 },
]
const query = reactive({ keyword: '', warehouseId: undefined as number | undefined, brand: '', onShelf: undefined as number | undefined, categoryId: undefined as number | undefined })
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 左侧商品分类树（对标：全部商品 + 13 分类） ═══
const categoryTree = ref<any[]>([])
const categoryLoading = ref(false)
const selectedCategoryId = ref<number | undefined>(undefined)

function buildTreeData(cats: any[]): any[] {
  return (cats || [])
    .filter((c: any) => c && c.id != null)
    .map((c: any) => ({
      key: String(c.id),
      title: c.categoryName || c.name || c.categoryCode,
      value: c.id,
      children: c.children?.length ? buildTreeData(c.children) : undefined,
    }))
}

async function fetchCategoryTree() {
  categoryLoading.value = true
  try {
    const res: any = await productCategoryApi.getTree()
    const list: any[] = Array.isArray(res) ? res : res?.data || []
    categoryTree.value = buildTreeData(list)
  } catch {
    categoryTree.value = []
  } finally {
    categoryLoading.value = false
  }
}

function onCategorySelect(selectedKeys: any[]) {
  query.categoryId = selectedKeys && selectedKeys.length ? Number(selectedKeys[0]) : undefined
  handleSearch()
}

// ═══ 表格列（对标：多级表头 可用库存/账面库存/成本单价 + 操作列分布/明细/对账/批次号） ═══
const columns: DetailColumnConfig[] = [
  { key: 'productCode', title: '货号', width: 110 },
  { key: 'productName', title: '商品名称', width: 200, slotName: 'productNameCell' },
  { key: 'specification', title: '规格', width: 120 },
  { key: 'warehouseName', title: '仓库', width: 120 },
  { key: 'unit', title: '单位', width: 70 },
  { key: 'convertedResult', title: '换算结果', width: 100, align: 'right' },
  { key: 'availableQuantity', title: '可用数量', width: 100, align: 'right' },
  { key: 'quantity', title: '账面数量', width: 100, align: 'right' },
  { key: 'minStock', title: '安全库存', width: 90, align: 'right' },
  { key: 'unitPrice', title: '成本单价', width: 100, align: 'right' },
  { key: 'stockAmount', title: '库存金额', width: 110, align: 'right' },
  { key: 'action', title: '操作', width: 170, fixed: 'right', slotName: 'actionCell' },
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
      ...query,
      categoryId: query.categoryId,
    }
    Object.keys(params).forEach(k => { if (params[k] === undefined || params[k] === null || params[k] === '') delete params[k] })
    const res: any = await stockApi.page(params)
    const data = res?.data || res || {}
    tableData.value = (data?.records || data?.list || []).map((r: any) => ({
      ...r,
      unitPrice: r.unitPrice ?? r.costPrice,
      stockAmount: (Number(r.quantity) || 0) * (Number(r.unitPrice ?? r.costPrice) || 0),
    }))
    pagination.total = Number(data?.total) || 0
  } catch (e) {
    console.warn('[查库存] 获取数据失败', e)
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handleReset = () => {
  Object.assign(query, { keyword: '', warehouseId: undefined, brand: '', onShelf: undefined })
  handleSearch()
}
const handleDetail = (record: any) => {
  message.info(`查看 ${record.productName} 明细`)
}

onMounted(async () => {
  try {
    const list: any = await stockApi.getWarehouses()
    warehouseOptions.value = (Array.isArray(list) ? list : []).map((w: any) => ({ label: w.warehouseName, value: w.id }))
  } catch { /* ignore */ }
  fetchCategoryTree()
  fetchData()
})
</script>

<style scoped>
.check-stock-page { display: flex; flex-direction: column; height: 100%; padding: 8px 12px; gap: 8px; }
.view-tabs { display: flex; justify-content: space-between; align-items: center; }
.view-tabs :deep(.ant-tabs) { margin-bottom: 0; }
.view-toolbar { flex-shrink: 0; }
.query-area { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; padding: 8px; background: #fff; border-radius: 4px; }
.content-area { flex: 1; overflow: hidden; display: flex; gap: 8px; }
.category-panel { width: 220px; flex-shrink: 0; background: #fff; border-radius: 4px; padding: 8px; overflow-y: auto; }
.category-title { font-size: 13px; font-weight: 600; color: #333; margin-bottom: 8px; }
.table-wrap { flex: 1; overflow: hidden; display: flex; flex-direction: column; background: #fff; border-radius: 4px; }
/* 分页栏统一走 StandardPagination 经典形态（自带边框/内边距/居中），这里只保留让位 */
.table-pagination { flex-shrink: 0; }
.zero-stock { color: #ff4d4f; }
</style>
