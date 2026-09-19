<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        套餐（营销 → 营销活动 → 套餐，菜单 80315）
        对标 ql361「营销 → 营销活动 → 套餐」：单视图列表页，7 列（全部默认可见）
        列：图片 / 套餐名称 / 套餐编号 / 套餐金额 / 套餐条码 / 捆绑销售 / 商品明细
        对标文档：docs/Yh-Spec/手动整理对标开发文档/营销模块/套餐开发文档.md
        实测实据（首条）：套餐名称「700ml 磨砂款+透明连体盖 500套」、套餐编号 tc0001、套餐金额 176、
                        捆绑销售 √、商品明细「90口径-700ml注塑杯-磨砂*1箱、…」；行级「修改停用复制删除」
        后端：复用既有 /erp/product-kit（erp_product_kit + erp_product_kit_item 明细），V11.373.0 补 图片/条码/捆绑销售 三列
        对标无「页面配置」弹窗
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
            <PlusOutlined /> 新增套餐
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
                placeholder="请输入套餐名称/套餐编号"
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
              storage-key="marketing-package-deal-table-columns"
              global-config-key="marketing-package-deal-table-columns"
            >
              <template #imageCell="{ record }">
                <img
                  v-if="!record.__ghost && record.imageUrl"
                  :src="record.imageUrl"
                  @error="onImageError"
                  class="package-img"
                  alt="套餐图片"
                >
                <span v-else-if="!record.__ghost">-</span>
              </template>

              <template #nameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openEdit(record)"
                >{{ record.kitName }}</a>
              </template>

              <template #bundleCell="{ record }">
                <span v-if="!record.__ghost">{{ record.bundleSales === 1 ? '√' : '' }}</span>
              </template>

              <template #itemsCell="{ record }">
                <span v-if="!record.__ghost">{{ record.itemsSummary || '-' }}</span>
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
                    @click="handleToggle(record)"
                  >
                    {{ record.active === false ? '启用' : '停用' }}
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="handleCopy(record)"
                  >
                    复制
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

      <!-- ═══ 新增/修改套餐 ═══ -->
      <a-modal
        v-model:open="formOpen"
        :title="editingId ? '修改套餐' : '新增套餐'"
        :confirm-loading="saving"
        width="900px"
        @ok="handleSave"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          size="small"
          style="margin-top: 12px"
        >
          <a-row :gutter="8">
            <a-col :span="12">
              <a-form-item
                label="套餐名称"
                required
              >
                <a-input
                  v-model:value="form.kitName"
                  placeholder="请输入套餐名称"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="套餐编号">
                <a-input
                  :value="form.kitCode || '（保存时自动生成）'"
                  disabled
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="套餐金额">
                <a-input-number
                  v-model:value="form.kitPrice"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="套餐条码">
                <a-input
                  v-model:value="form.barcode"
                  placeholder="请输入套餐条码"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="捆绑销售">
                <a-switch
                  v-model:checked="form.bundleSalesBool"
                  checked-children="是"
                  un-checked-children="否"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="套餐图片">
                <a-input
                  v-model:value="form.imageUrl"
                  placeholder="图片URL"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item
                label="套餐商品明细"
                :label-col="{ span: 3 }"
                :wrapper-col="{ span: 20 }"
              >
                <a-space style="margin-bottom: 8px">
                  <a-button
                    size="small"
                    @click="productPickerOpen = true"
                  >
                    添加商品
                  </a-button>
                  <span class="selected-tip">共 {{ form.items.length }} 个商品</span>
                </a-space>
                <a-table
                  :columns="ITEM_COLUMNS"
                  :data-source="form.items"
                  :pagination="false"
                  row-key="productId"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'quantity'">
                      <a-input-number
                        v-model:value="record.quantity"
                        :min="0.01"
                        size="small"
                        style="width: 100px"
                      />
                    </template>
                    <template v-else-if="column.key === 'action'">
                      <a-button
                        type="link"
                        size="small"
                        danger
                        @click="removeItem(record)"
                      >
                        移除
                      </a-button>
                    </template>
                  </template>
                </a-table>
              </a-form-item>
            </a-col>
          </a-row>
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
import { productKitApi } from '@/api/marketing'

defineOptions({ name: 'MarketingPackageDeal' })

const loading = ref(false)
const exporting = ref(false)
const tableData = ref<any[]>([])
const searchForm = reactive({ keyword: '' })
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// 对标 7 列（全部默认可见）
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 200, fixed: 'left' },
  { key: 'imageUrl', title: '图片', type: 'slot', slotName: 'imageCell', width: 90 },
  { key: 'kitName', title: '套餐名称', type: 'slot', slotName: 'nameCell', width: 280 },
  { key: 'kitCode', title: '套餐编号', type: 'input', width: 140 },
  { key: 'kitPrice', title: '套餐金额', type: 'input', width: 120 },
  { key: 'barcode', title: '套餐条码', type: 'input', width: 140 },
  { key: 'bundleSales', title: '捆绑销售', type: 'slot', slotName: 'bundleCell', width: 100 },
  { key: 'itemsSummary', title: '商品明细', type: 'slot', slotName: 'itemsCell', width: 400 },
]

const ITEM_COLUMNS = [
  { title: '商品编号', dataIndex: 'productCode', key: 'productCode', width: 140 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName' },
  { title: '规格', dataIndex: 'productSpec', key: 'productSpec', width: 140 },
  { title: '单位', dataIndex: 'productUnit', key: 'productUnit', width: 80 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 130 },
  { title: '操作', key: 'action', width: 80 },
]

// ═══ 数据 ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await productKitApi.page({
      keyword: searchForm.keyword || undefined,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    const records = res?.records || []
    tableData.value = records.map((r: any) => ({
      ...r,
      itemsSummary: buildItemsSummary(r.items),
    }))
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[套餐] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 商品明细摘要（对标实据形态：商品名*数量单位、…） */
function buildItemsSummary(items: any[]): string {
  if (!Array.isArray(items) || !items.length) return ''
  return items.map((i: any) => {
    const name = i.componentProductName || i.productName || ''
    const qty = i.quantity == null ? '' : String(i.quantity)
    const unit = i.componentProductUnit || i.productUnit || ''
    return `${name}*${qty}${unit}`
  }).join('、')
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

// ═══ 表单 ═══
const formOpen = ref(false)
const saving = ref(false)
const editingId = ref<any>(null)
const productPickerOpen = ref(false)

const emptyForm = () => ({
  kitCode: '',
  kitName: '',
  kitPrice: 0 as number | undefined,
  barcode: '',
  imageUrl: '',
  bundleSalesBool: true,
  items: [] as any[],
})
const form = reactive(emptyForm())

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  formOpen.value = true
}

async function openEdit(record: any) {
  editingId.value = record.id
  try {
    const detail: any = await productKitApi.getById(record.id)
    Object.assign(form, emptyForm(), {
      kitCode: detail?.kitCode || '',
      kitName: detail?.kitName || '',
      kitPrice: detail?.kitPrice ?? 0,
      barcode: detail?.barcode || '',
      imageUrl: detail?.imageUrl || '',
      bundleSalesBool: detail?.bundleSales !== 0,
      items: (detail?.items || []).map((i: any) => ({
        productId: i.componentProductId,
        productCode: i.componentProductCode,
        productName: i.componentProductName,
        productSpec: i.componentProductSpec,
        productUnit: i.componentProductUnit,
        quantity: i.quantity,
      })),
    })
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载套餐明细失败')
    return
  }
  formOpen.value = true
}

function handleProductsPicked(products: any[]) {
  for (const p of products || []) {
    if (form.items.some((i: any) => String(i.productId) === String(p.id))) continue
    form.items.push({
      productId: p.id,
      productCode: p.productCode || p.code,
      productName: p.productName || p.name,
      productSpec: p.spec || p.specification,
      productUnit: p.unit,
      quantity: 1,
    })
  }
}
function removeItem(row: any) {
  form.items = form.items.filter((i: any) => String(i.productId) !== String(row.productId))
}

async function handleSave() {
  if (!form.kitName?.trim()) {
    message.warning('请输入套餐名称')
    return
  }
  if (!form.items.length) {
    message.warning('请至少添加一个套餐商品')
    return
  }
  saving.value = true
  try {
    const payload: any = {
      kitName: form.kitName,
      kitType: 1,
      kitPrice: form.kitPrice,
      barcode: form.barcode || undefined,
      imageUrl: form.imageUrl || undefined,
      bundleSales: form.bundleSalesBool ? 1 : 0,
      items: form.items.map((i: any) => ({
        componentProductId: i.productId,
        componentProductCode: i.productCode,
        componentProductName: i.productName,
        componentProductSpec: i.productSpec,
        componentProductUnit: i.productUnit,
        quantity: i.quantity,
      })),
    }
    if (editingId.value) {
      await productKitApi.update(editingId.value, payload)
      message.success('套餐已更新')
    } else {
      await productKitApi.create(payload)
      message.success('套餐已新增')
    }
    formOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleToggle(record: any) {
  const next = record.active === false
  try {
    if (next) {
      await productKitApi.activate(record.id)
    } else {
      await productKitApi.deactivate(record.id)
    }
    message.success(next ? '套餐已启用' : '套餐已停用')
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '操作失败')
  }
}

async function handleCopy(record: any) {
  try {
    await productKitApi.copy(record.id)
    message.success('套餐已复制')
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '复制失败')
  }
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除套餐「${record.kitName}」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await productKitApi.remove(record.id)
        message.success('删除成功')
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
  { key: 'kitName', title: '套餐名称' },
  { key: 'kitCode', title: '套餐编号' },
  { key: 'kitPrice', title: '套餐金额' },
  { key: 'barcode', title: '套餐条码' },
  { key: 'bundleText', title: '捆绑销售' },
  { key: 'itemsSummary', title: '商品明细' },
]
function printCell(row: any, key: string): string {
  if (key === 'bundleText') return row.bundleSales === 1 ? '√' : ''
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
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" /><title>套餐</title>
    <style>body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px}
    h2{text-align:center;margin:0 0 12px;font-size:18px}
    table{width:100%;border-collapse:collapse;font-size:12px}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left}th{background:#f2f2f2}</style></head><body>
    <h2>套餐</h2>
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
    a.download = `套餐_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } finally {
    exporting.value = false
  }
}

/** 图片源缺失时不显示破图（商品图片可能为历史遗留 URL，文件已不存在） */
function onImageError(e: Event) {
  const el = e.target as HTMLImageElement
  if (el) el.style.display = 'none'
}

function handleError(error: Error) {
  console.error('[套餐] 页面错误', error)
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
.package-img { width: 48px; height: 48px; object-fit: cover; border-radius: 4px; }
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
