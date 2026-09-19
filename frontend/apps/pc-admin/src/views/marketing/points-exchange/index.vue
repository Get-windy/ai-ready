<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        积分兑换（营销 → 会员中心 → 积分兑换，菜单 80301）
        对标 ql361「营销 → 会员中心 → 积分兑换」：**可兑换商品目录**维护页
        13 列（默认 7 列）：商品名称/货号/单位/兑换所需积分/规格/型号/产地 + 6 个价格列（默认隐藏）
        对标文档：docs/Yh-Spec/手动整理对标开发文档/营销模块/积分兑换开发文档.md
        查询区 1 项（货号/商品名称复合检索）；工具栏 新增兑换商品 ｜ 刷新 / 打印(F8) / 导出
        对标无「页面配置」弹窗
        口径：商品名称/货号/单位/规格/型号/产地与 6 个价格列实时取自商品主数据，本页只维护「兑换所需积分」
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <template #toolbar-left>
          <a-button
            type="primary"
            size="small"
            class="btn-add"
            @click="openCreate"
          >
            <PlusOutlined /> 新增兑换商品
          </a-button>
        </template>

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
              <span class="search-label">筛选条件</span>
              <a-input
                v-model:value="searchForm.keyword"
                placeholder="请输入货号/商品名称"
                size="small"
                style="width: 220px"
                allow-clear
                @press-enter="handleSearch"
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
              storage-key="marketing-points-exchange-table-columns"
              global-config-key="marketing-points-exchange-table-columns"
            >
              <template #nameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openEdit(record)"
                >{{ record.productName }}</a>
              </template>

              <template #pointsCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtPoints(record.exchangePoints) }}</span>
              </template>

              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="openEdit(record)"
                  >
                    修改
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="handleDelete(record)"
                  >
                    删除
                  </a-button>
                </a-space>
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

      <!-- ═══ 新增兑换商品（选品入目录 + 兑换所需积分） ═══ -->
      <a-modal
        v-model:open="formOpen"
        :title="editingId ? '修改兑换商品' : '新增兑换商品'"
        :confirm-loading="saving"
        width="560px"
        @ok="handleSave"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          size="small"
          style="margin-top: 12px"
        >
          <a-form-item label="商品">
            <a-space v-if="!editingId">
              <a-button
                size="small"
                @click="productPickerOpen = true"
              >
                选择商品
              </a-button>
              <span class="selected-tip">{{ form.productName || '未选择' }}</span>
            </a-space>
            <span v-else>{{ form.productName }}（{{ form.productCode }}）</span>
          </a-form-item>
          <a-form-item
            label="兑换所需积分"
            required
          >
            <a-input-number
              v-model:value="form.exchangePoints"
              :min="0"
              :precision="0"
              style="width: 180px"
            />
          </a-form-item>
          <a-form-item label="排序">
            <a-input-number
              v-model:value="form.sort"
              :min="0"
              :precision="0"
              style="width: 180px"
            />
          </a-form-item>
          <a-form-item label="备注">
            <a-input
              v-model:value="form.remark"
              placeholder="请输入备注"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <ProductSelectModal
        v-model:open="productPickerOpen"
        multiple
        @confirm="handleProductsPicked"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import { PlusOutlined, ReloadOutlined, PrinterOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import ProductSelectModal from '@/components/ProductSelectModal/index.vue'
import { pointsExchangeApi, type PointsExchangeRow } from '@/api/marketing'

defineOptions({ name: 'MarketingPointsExchange' })

const loading = ref(false)
const exporting = ref(false)
const tableData = ref<PointsExchangeRow[]>([])
const searchForm = reactive({ keyword: '' })
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// 对标 13 列（默认 7 列，6 个价格列默认隐藏）
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 130, fixed: 'left' },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'nameCell', width: 260 },
  { key: 'productCode', title: '货号', type: 'input', width: 140 },
  { key: 'unit', title: '单位', type: 'input', width: 80 },
  { key: 'exchangePoints', title: '兑换所需积分', type: 'slot', slotName: 'pointsCell', width: 130 },
  { key: 'spec', title: '规格', type: 'input', width: 140 },
  { key: 'model', title: '型号', type: 'input', width: 120 },
  { key: 'origin', title: '产地', type: 'input', width: 120 },
  { key: 'presetPurchasePrice', title: '预设进价', type: 'input', width: 110, defaultHidden: true },
  { key: 'referenceCost', title: '参考成本', type: 'input', width: 110, defaultHidden: true },
  { key: 'recentPurchasePrice', title: '最近进价', type: 'input', width: 110, defaultHidden: true },
  { key: 'wholesalePrice', title: '批发价', type: 'input', width: 110, defaultHidden: true },
  { key: 'retailPrice', title: '零售价', type: 'input', width: 110, defaultHidden: true },
  { key: 'minSalePrice', title: '最低售价', type: 'input', width: 110, defaultHidden: true },
]

function fmtPoints(v: any): string {
  return v == null ? '-' : String(v)
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await pointsExchangeApi.page({
      keyword: searchForm.keyword || undefined,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[积分兑换] 加载列表失败', error)
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

// ═══ 新增 / 修改 ═══
const formOpen = ref(false)
const saving = ref(false)
const editingId = ref<any>(null)
const productPickerOpen = ref(false)
const emptyForm = () => ({
  productId: undefined as any,
  productName: '',
  productCode: '',
  exchangePoints: 0 as number | undefined,
  sort: 0 as number | undefined,
  remark: '',
})
const form = reactive(emptyForm())

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  formOpen.value = true
}

function openEdit(record: any) {
  editingId.value = record.id
  Object.assign(form, emptyForm(), {
    productId: record.productId,
    productName: record.productName || '',
    productCode: record.productCode || '',
    exchangePoints: record.exchangePoints ?? 0,
    sort: record.sort ?? 0,
    remark: record.remark || '',
  })
  formOpen.value = true
}

/** 选品入目录：多选时按「每商品一条」批量入目录 */
async function handleProductsPicked(products: any[]) {
  if (!products?.length) return
  if (editingId.value) return
  if (products.length === 1) {
    const p = products[0]
    form.productId = p.id
    form.productName = p.productName || p.name || ''
    form.productCode = p.productCode || p.code || ''
    return
  }
  try {
    const n = await pointsExchangeApi.batchCreate(products.map(p => ({
      productId: p.id,
      exchangePoints: 0,
      sort: 0,
    })))
    message.success(`已加入目录 ${n} 个商品，请逐行维护兑换所需积分`)
    formOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加入目录失败')
  }
}

async function handleSave() {
  if (!form.productId) {
    message.warning('请先选择商品')
    return
  }
  if (form.exchangePoints == null || form.exchangePoints < 0) {
    message.warning('兑换所需积分须为非负数值')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await pointsExchangeApi.update(editingId.value, {
        exchangePoints: form.exchangePoints,
        sort: form.sort,
        remark: form.remark,
      })
      message.success('已更新')
    } else {
      await pointsExchangeApi.create({
        productId: form.productId,
        exchangePoints: form.exchangePoints,
        sort: form.sort,
        remark: form.remark,
        status: 1,
      })
      message.success('已加入兑换目录')
    }
    formOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要把商品「${record.productName}」移出积分兑换目录吗？`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await pointsExchangeApi.remove(record.id)
        message.success('已移出兑换目录')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 打印 / 导出 ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}
const PRINT_COLUMNS = [
  { key: 'productName', title: '商品名称' },
  { key: 'productCode', title: '货号' },
  { key: 'unit', title: '单位' },
  { key: 'exchangePoints', title: '兑换所需积分' },
  { key: 'spec', title: '规格' },
  { key: 'model', title: '型号' },
  { key: 'origin', title: '产地' },
  { key: 'presetPurchasePrice', title: '预设进价' },
  { key: 'referenceCost', title: '参考成本' },
  { key: 'recentPurchasePrice', title: '最近进价' },
  { key: 'wholesalePrice', title: '批发价' },
  { key: 'retailPrice', title: '零售价' },
  { key: 'minSalePrice', title: '最低售价' },
]
function printCell(row: any, key: string): string {
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
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" /><title>积分兑换</title>
    <style>body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px}
    h2{text-align:center;margin:0 0 12px;font-size:18px}
    table{width:100%;border-collapse:collapse;font-size:12px}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left}th{background:#f2f2f2}</style></head><body>
    <h2>积分兑换商品目录</h2>
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
    a.download = `积分兑换_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } finally {
    exporting.value = false
  }
}

function handleError(error: Error) {
  console.error('[积分兑换] 页面错误', error)
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
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.selected-tip { font-size: 12px; color: #666; }
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-add:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
