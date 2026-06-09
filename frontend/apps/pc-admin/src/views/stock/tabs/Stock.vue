<template>
  <div class="stock-list-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ formatNumber(totalQuantity) }}</div>
          <div class="stat-card-label">库存总量</div>
        </div>
        <DatabaseOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-available">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ formatNumber(totalAvailable) }}</div>
          <div class="stat-card-label">可用库存</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-frozen">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ formatNumber(totalFrozen) }}</div>
          <div class="stat-card-label">冻结库存</div>
        </div>
        <LockOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-count">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">产品种类</div>
        </div>
        <AppstoreOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      :filter-fields="filterFields"
      :show-summary="true"
      :selectable="false"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @sort-change="handleSortChange"
      @filter-change="handleFilterChange"
      @export="handleExport"
    >
      <template #toolbar-actions>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
      </template>

      <template #action="{ record }">
        <a-space :size="4">
          <a-tooltip title="查看">
            <a-button type="link" size="small" @click="handleView(record)">
              <template #icon><EyeOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip title="盘点">
            <a-button type="link" size="small" @click="handleCheck(record)">
              <template #icon><CheckSquareOutlined /></template>
            </a-button>
          </a-tooltip>
        </a-space>
      </template>

      <template #empty>
        <div class="table-empty">
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <InboxOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的库存记录，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无库存记录
          </p>
        </div>
      </template>
    </VxeTableList>

    <!-- 盘点弹窗 -->
    <a-modal
      v-model:open="checkVisible"
      title="库存盘点"
      width="900px"
      :confirm-loading="checkSubmitting"
      @ok="handleCheckSubmit"
      @cancel="handleCheckCancel"
    >
      <a-form
        ref="checkFormRef"
        :model="checkForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        :rules="checkFormRules"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="盘点仓库" name="warehouseId">
              <a-select v-model:value="checkForm.warehouseId" placeholder="请选择仓库" :options="warehouseOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="盘点日期" name="checkDate">
              <a-date-picker v-model:value="checkForm.checkDate" style="width: 100%" placeholder="请选择盘点日期" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
          <a-textarea v-model:value="checkForm.remark" :rows="2" placeholder="请输入盘点备注" />
        </a-form-item>
      </a-form>

      <a-divider style="margin: 12px 0">盘点明细</a-divider>

      <VxeTableList
        :data-source="checkItems"
        :pagination="false"
        row-key="id"
        :show-toolbar="false" :selectable="false" :show-add="false" :show-search="false"
        :show-export="false" :show-batch-delete="false"
        :columns="checkItemColumns"
      >
        <template #systemQtyCell="{ record }">
          {{ record.quantity || 0 }} {{ record.unit || '' }}
        </template>
        <template #actualQtyCell="{ record, rowIndex }">
          <a-input-number v-model:value="checkItems[rowIndex].actualQty" :min="0" style="width: 100%" placeholder="实盘数量" />
        </template>
        <template #diffCell="{ record, rowIndex }">
          <a-tag :color="getDiffColor(rowIndex)">{{ getDiffQty(rowIndex) }}</a-tag>
        </template>
      </VxeTableList>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  EyeOutlined, CheckSquareOutlined, SearchOutlined, InboxOutlined,
  DatabaseOutlined, CheckCircleOutlined, LockOutlined, AppstoreOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { stockApi, stockCheckApi } from '@/api/erp'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'

const emit = defineEmits(['update-count'])

const router = useRouter()
const tableRef = ref()
const loading = ref(false)
const tableData = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// ── 统计数据 ────────────────────────────────────────────
const totalQuantity = computed(() => tableData.value.reduce((s, r) => s + (r.quantity || 0), 0))
const totalAvailable = computed(() => tableData.value.reduce((s, r) => s + (r.availableQuantity || 0), 0))
const totalFrozen = computed(() => tableData.value.reduce((s, r) => s + (r.frozenQuantity || 0), 0))



// vxe-table 列定义
const vxeColumns = computed(() => [
  { field: 'productCode', title: '产品编码', width: 150 },
  { field: 'productName', title: '产品名称', width: 160, sortable: true },
  { field: 'specification', title: '规格型号', width: 120 },
  { field: 'warehouseName', title: '仓库', width: 120 },
  {
    field: 'quantity',
    title: '库存数量',
    width: 100,
    align: 'right',
    formatter: ({ cellValue, row }: any) => `${cellValue || 0} ${row.unit || ''}`,
  },
  {
    field: 'availableQuantity',
    title: '可用数量',
    width: 100,
    align: 'center',
    formatter: ({ cellValue, row }: any) => {
      const color = cellValue > 0 ? 'green' : 'red'
      return `<span class="ant-tag ant-tag-${color}">${cellValue} ${row.unit || ''}</span>`
    },
  },
  {
    field: 'frozenQuantity',
    title: '冻结数量',
    width: 100,
    align: 'right',
    formatter: ({ cellValue }: any) => `<span class="qty-cell frozen">${cellValue || 0}</span>`,
  },
  { field: 'action', title: '操作', width: 120, fixed: 'right', type: 'action' },
])

const filterFields = [
  { key: 'productCode', label: '产品编码', type: 'input' as const, placeholder: '输入产品编码' },
  { key: 'productName', label: '产品名称', type: 'input' as const, placeholder: '输入产品名称' },
  { key: 'warehouseName', label: '仓库', type: 'input' as const, placeholder: '输入仓库名称' }
]

function formatNumber(num: number): string {
  return num?.toLocaleString?.('zh-CN') || '0'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await stockApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, ...searchFilters })
    const pageData = (res as any).data ?? res
    tableData.value = pageData?.records || []
    pagination.total = pageData?.totalElements ?? pageData?.total ?? 0
    lastUpdated.value = new Date().toISOString()
    emit('update-count', pagination.total)
  } catch (err) {
    console.warn('[库存管理] 获取库存列表失败', err)
    tableData.value = []
    pagination.total = 0
    emit('update-count', pagination.total)
  }
  finally { loading.value = false }
}


function handleView(record: any) {
  router.push(`/stock/detail/${record.id}`)
}

// ── 盘点 ──
const checkVisible = ref(false)
const checkSubmitting = ref(false)
const checkFormRef = ref<FormInstance>()
const checkForm = reactive({ warehouseId: undefined as number | undefined, checkDate: dayjs(), remark: '' })
const checkFormRules = {
  warehouseId: [{ required: true, message: '请选择盘点仓库', trigger: 'change' }],
  checkDate: [{ required: true, message: '请选择盘点日期', trigger: 'change' }]
}
const warehouseOptions = [
  { value: 1, label: '主仓库' }, { value: 2, label: '备品仓库' },
  { value: 3, label: '半成品仓库' }, { value: 4, label: '成品仓库' }
]
interface CheckItem { id: number; productCode: string; productName: string; specification?: string; unit?: string; quantity: number; actualQty: number | null; warehouseName?: string }
const checkItems = ref<CheckItem[]>([])
const checkItemColumns = [
  { title: '产品编码', field: 'productCode', width: 140 },
  { title: '产品名称', field: 'productName' },
  { title: '规格型号', field: 'specification', width: 100 },
  { title: '系统库存', field: 'systemQty', width: 110, slotName: 'systemQtyCell' },
  { title: '实盘数量', field: 'actualQty', width: 120, slotName: 'actualQtyCell' },
  { title: '差异', field: 'diff', width: 100, slotName: 'diffCell' }
]
function getDiffQty(index: number) {
  const item = checkItems.value[index]
  if (item.actualQty === null || item.actualQty === undefined) return '-'
  const diff = item.actualQty - (item.quantity || 0)
  return diff === 0 ? '无差异' : (diff > 0 ? `+${diff}` : `${diff}`)
}
function getDiffColor(index: number) {
  const item = checkItems.value[index]
  if (item.actualQty === null || item.actualQty === undefined) return 'default'
  const diff = item.actualQty - (item.quantity || 0)
  if (diff === 0) return 'green'
  if (diff > 0) return 'blue'
  return 'red'
}
function handleCheck(record: any) {
  checkForm.warehouseId = undefined
  checkForm.checkDate = dayjs()
  checkForm.remark = ''
  if (record.warehouseName) {
    const matched = warehouseOptions.find(w => w.label === record.warehouseName)
    if (matched) checkForm.warehouseId = matched.value
  }
  checkItems.value = [{
    id: record.id || 1, productCode: record.productCode || '', productName: record.productName || '',
    specification: record.specification || '', unit: record.unit || '个', quantity: record.quantity || 0, actualQty: null
  }]
  checkVisible.value = true
}
async function handleCheckSubmit() {
  try { await checkFormRef.value?.validate() } catch (err) { console.warn('[库存管理] 表单验证失败', err); return }
  checkSubmitting.value = true
  try {
    await stockCheckApi.create({
      warehouseId: checkForm.warehouseId, checkDate: checkForm.checkDate.format('YYYY-MM-DD'),
      remark: checkForm.remark, items: checkItems.value.map(item => ({
        productId: item.id, systemQty: item.quantity, actualQty: item.actualQty ?? 0
      }))
    })
    message.success('盘点单创建成功')
    checkVisible.value = false
    fetchData()
  } catch (err: any) { console.warn('[库存管理] 提交盘点失败', err); message.error(err?.message || '盘点提交失败') }
  finally { checkSubmitting.value = false }
}
function handleCheckCancel() { checkVisible.value = false }

// ── 导出 ──
function handleExport() {
  const headers = ['产品编码', '产品名称', '规格型号', '仓库', '库存数量', '可用数量', '冻结数量']
  const rows = tableData.value.map((row: any) => [
    row.productCode || '', row.productName || '', row.specification || '', row.warehouseName || '',
    row.quantity || 0, row.availableQuantity || 0, row.frozenQuantity || 0
  ])
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `库存报表_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  console.warn('[库存管理] 导出库存报表（客户端模拟）')
  message.success('导出成功')
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.current = 1; fetchData()
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleView(tableData.value[0]) }
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.stock-list-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;

}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-available { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-frozen { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-count { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}

/* 空状态 */
.table-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0;
}

.table-empty-icon {
  font-size: 48px;
  color: #d9d9d9;
}

.table-empty-text {
  color: #999;
  margin-top: 12px;
}


.qty-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
}

.qty-cell.frozen {
  color: #faad14;
}

.list-update-timestamp {
  font-size: 12px;
  color: var(--color-text-tertiary, #bbb);
  white-space: nowrap;
  cursor: help;
  margin-left: 8px;
  line-height: 32px;
  vertical-align: middle;
}





/* 响应式 */
@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 45%;
    min-width: 120px;
  }
}
</style>
