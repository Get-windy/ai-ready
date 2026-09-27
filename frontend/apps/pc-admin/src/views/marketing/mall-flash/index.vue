<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        商城秒杀（营销 → 商城营销 → 商城秒杀，菜单 80321）
        对标 ql361「营销 → 商城营销 → 商城秒杀」：单视图列表页，5 列（全部默认可见）
        列：活动名称 / 起始时间 / 结束时间 / 活动状态 / 创建时间
        对标文档：docs/Yh-Spec/手动整理对标开发文档/营销模块/商城秒杀开发文档.md
        后端：复用既有 /erp/marketing/flash-sale（一行 = 一个秒杀活动，含商品与秒杀价）
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
            <PlusOutlined /> 新增
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
              <span class="search-label">活动名称</span>
              <a-input
                v-model:value="searchForm.title"
                placeholder="请输入活动名称"
                size="small"
                style="width: 200px"
                allow-clear
                @press-enter="handleSearch"
              />
              <span class="search-label">活动状态</span>
              <a-select
                v-model:value="searchForm.status"
                placeholder="全部状态"
                size="small"
                style="width: 140px"
                allow-clear
                :options="STATUS_OPTIONS"
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
              storage-key="marketing-mall-flash-table-columns"
              global-config-key="marketing-mall-flash-table-columns"
            >
              <template #nameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openEdit(record)"
                >{{ record.title }}</a>
              </template>

              <template #timeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ fmtTime(record[column.key]) }}</span>
              </template>

              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="FLASH_STATUS_MAP[record.status]?.color || 'default'"
                >
                  {{ FLASH_STATUS_MAP[record.status]?.text || record.status }}
                </a-tag>
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
                  <a-dropdown>
                    <a-button
                      type="link"
                      size="small"
                    >
                      更多
                    </a-button>
                    <template #overlay>
                      <a-menu @click="(e: any) => handleMore(e, record)">
                        <a-menu-item
                          v-if="record.status !== 1"
                          key="publish"
                        >
                          发布
                        </a-menu-item>
                        <a-menu-item
                          v-if="record.status === 1"
                          key="cancel"
                        >
                          取消
                        </a-menu-item>
                        <a-menu-item key="participants">参与记录</a-menu-item>
                        <a-menu-item key="delete">删除</a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
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

      <!-- ═══ 新增/修改秒杀活动 ═══ -->
      <a-modal
        v-model:open="formOpen"
        :title="editingId ? '修改秒杀活动' : '新增秒杀活动'"
        :confirm-loading="saving"
        width="680px"
        @ok="handleSave"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          size="small"
          style="margin-top: 12px"
        >
          <a-form-item
            label="活动名称"
            required
          >
            <a-input
              v-model:value="form.title"
              placeholder="请输入活动名称"
            />
          </a-form-item>
          <a-form-item label="秒杀商品">
            <a-space>
              <a-button
                size="small"
                @click="productPickerOpen = true"
              >
                选择商品
              </a-button>
              <span class="selected-tip">{{ form.productName || '未选择' }}</span>
            </a-space>
          </a-form-item>
          <a-row :gutter="8">
            <a-col :span="12">
              <a-form-item label="秒杀价">
                <a-input-number
                  v-model:value="form.flashPrice"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="原价">
                <a-input-number
                  v-model:value="form.originalPrice"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="起始时间">
                <a-date-picker
                  v-model:value="form.startTime"
                  show-time
                  style="width: 100%"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="结束时间">
                <a-date-picker
                  v-model:value="form.endTime"
                  show-time
                  style="width: 100%"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="限量库存">
                <a-input-number
                  v-model:value="form.stockLimit"
                  :min="0"
                  :precision="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="排序">
                <a-input-number
                  v-model:value="form.sort"
                  :min="0"
                  :precision="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="备注">
            <a-textarea
              v-model:value="form.remark"
              :rows="2"
              placeholder="请输入备注"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 参与记录 ═══ -->
      <a-modal
        v-model:open="participantsOpen"
        title="秒杀参与记录"
        width="860px"
        :footer="null"
      >
        <a-table
          :columns="PARTICIPANT_COLUMNS"
          :data-source="participantRows"
          :loading="participantsLoading"
          :pagination="false"
          row-key="id"
          size="small"
          :scroll="{ y: 340 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'createTime'">
              {{ fmtTime(record.createTime) }}
            </template>
          </template>
        </a-table>
      </a-modal>

      <ProductSelectModal
        v-model:open="productPickerOpen"
        @confirm="handleProductPicked"
      />
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="marketing-mall-flash"
      :print-data="printData"
    />
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
import { flashSaleApi } from '@/api/marketing'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'MarketingMallFlash' })

/** 秒杀状态：0 草稿 / 1 已发布 / 2 已取消 / 3 已结束（与后端 FlashSale 注释一致） */
const FLASH_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '未开始', color: 'default' },
  1: { text: '进行中', color: 'green' },
  2: { text: '已取消', color: 'red' },
  3: { text: '已结束', color: 'orange' },
}
const STATUS_OPTIONS = Object.entries(FLASH_STATUS_MAP).map(([value, v]) => ({ value: Number(value), label: v.text }))

const loading = ref(false)
const exporting = ref(false)
const tableData = ref<any[]>([])
const searchForm = reactive({ title: '', status: undefined as number | undefined })
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// 对标 5 列（全部默认可见）
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 160, fixed: 'left' },
  { key: 'title', title: '活动名称', type: 'slot', slotName: 'nameCell', width: 300 },
  { key: 'startTime', title: '起始时间', type: 'slot', slotName: 'timeCell', width: 170 },
  { key: 'endTime', title: '结束时间', type: 'slot', slotName: 'timeCell', width: 170 },
  { key: 'status', title: '活动状态', type: 'slot', slotName: 'statusCell', width: 120 },
  { key: 'createTime', title: '创建时间', type: 'slot', slotName: 'timeCell', width: 170 },
]

const PARTICIPANT_COLUMNS = [
  { title: '客户名称', dataIndex: 'customerName', key: 'customerName', width: 180 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 100 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 120 },
  { title: '下单时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
]

function fmtTime(v: any): string {
  return v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-'
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await flashSaleApi.page({
      title: searchForm.title || undefined,
      status: searchForm.status,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[商城秒杀] 加载列表失败', error)
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

// ═══ 表单 ═══
const formOpen = ref(false)
const saving = ref(false)
const editingId = ref<any>(null)
const productPickerOpen = ref(false)
const emptyForm = () => ({
  title: '',
  productId: undefined as any,
  productCode: '',
  productName: '',
  flashPrice: 0 as number | undefined,
  originalPrice: 0 as number | undefined,
  stockLimit: 0 as number | undefined,
  sort: 0 as number | undefined,
  startTime: undefined as any,
  endTime: undefined as any,
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
    title: record.title || '',
    productId: record.productId,
    productCode: record.productCode || '',
    productName: record.productName || '',
    flashPrice: record.flashPrice ?? 0,
    originalPrice: record.originalPrice ?? 0,
    stockLimit: record.stockLimit ?? 0,
    sort: record.sort ?? 0,
    startTime: record.startTime || undefined,
    endTime: record.endTime || undefined,
    remark: record.remark || '',
  })
  formOpen.value = true
}
function handleProductPicked(products: any[]) {
  const p = products?.[0]
  if (!p) return
  form.productId = p.id
  form.productCode = p.productCode || p.code || ''
  form.productName = p.productName || p.name || ''
  if (!form.title) form.title = `${form.productName} 秒杀`
}

async function handleSave() {
  if (!form.title?.trim()) {
    message.warning('请输入活动名称')
    return
  }
  if (!form.productId) {
    message.warning('请选择秒杀商品')
    return
  }
  if (form.startTime && form.endTime && dayjs(form.endTime).isBefore(dayjs(form.startTime))) {
    message.warning('结束时间不得早于起始时间')
    return
  }
  saving.value = true
  try {
    const payload: any = { ...form, status: editingId.value ? undefined : 0 }
    if (editingId.value) {
      await flashSaleApi.update(editingId.value, payload)
      message.success('秒杀活动已更新')
    } else {
      await flashSaleApi.create(payload)
      message.success('秒杀活动已新增')
    }
    formOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleMore(e: any, record: any) {
  if (e.key === 'publish') {
    try {
      await flashSaleApi.publish(record.id)
      message.success('已发布')
      fetchList()
    } catch (error: any) {
      message.error(error?.response?.data?.message || '发布失败')
    }
  } else if (e.key === 'cancel') {
    try {
      await flashSaleApi.cancel(record.id)
      message.success('已取消')
      fetchList()
    } catch (error: any) {
      message.error(error?.response?.data?.message || '取消失败')
    }
  } else if (e.key === 'participants') {
    participantsOpen.value = true
    participantsLoading.value = true
    try {
      const res: any = await flashSaleApi.participants(record.id, { pageNum: 1, pageSize: 200 })
      participantRows.value = res?.records || []
    } catch (error: any) {
      message.error(error?.response?.data?.message || '加载参与记录失败')
      participantRows.value = []
    } finally {
      participantsLoading.value = false
    }
  } else if (e.key === 'delete') {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除秒杀活动「${record.title}」吗？`,
      okText: '确认删除',
      okType: 'danger',
      onOk: async () => {
        try {
          await flashSaleApi.remove(record.id)
          message.success('删除成功')
          fetchList()
        } catch (error: any) {
          message.error(error?.response?.data?.message || '删除失败')
        }
      },
    })
  }
}

const participantsOpen = ref(false)
const participantsLoading = ref(false)
const participantRows = ref<any[]>([])

// ═══ 打印 / 导出 ═══
const PRINT_COLUMNS = [
  { key: 'title', title: '活动名称' },
  { key: 'startTime', title: '起始时间' },
  { key: 'endTime', title: '结束时间' },
  { key: 'statusText', title: '活动状态' },
  { key: 'createTime', title: '创建时间' },
]
function printCell(row: any, key: string): string {
  if (key === 'statusText') return FLASH_STATUS_MAP[row.status]?.text || ''
  if (key === 'startTime' || key === 'endTime' || key === 'createTime') return fmtTime(row[key])
  return row[key] == null ? '' : String(row[key])
}

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML + window.open()，现在交给 PrintDialog：列与行由页面给，模板负责版式。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'marketing-mall-flash',
  title: '商城秒杀',
  rows: () => (tableData.value || []).filter((r: any) => !r.__ghost),
  // 列与导出一致；状态/时间列在送打印机前先按页面口径格式化
  columns: () => PRINT_COLUMNS.map(c => ({
    key: c.key,
    title: c.title,
    formatter: (_v: any, row: any) => printCell(row, c.key),
  })),
  emptyTip: '没有可打印的数据',
})

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
    a.download = `商城秒杀_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } finally {
    exporting.value = false
  }
}

function handleError(error: Error) {
  console.error('[商城秒杀] 页面错误', error)
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
