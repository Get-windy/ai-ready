<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        商城预售（营销 → 商城营销 → 商城预售，菜单 80322）
        对标 ql361「营销 → 商城营销 → 商城预售」：**双视图 Tab 复合页**（逐 Tab 独立列配置）
          · Tab1「商品预售」12 列：活动名称 / 商品名称 / 商品图片 / 货号 / 规格 / 型号 / 预售价 /
            起始时间 / 结束时间 / 是否支付订金 / 活动状态 / 创建人
          · Tab2「预售订单」8 列：客户名称 / 商品名称 / 单据编号 / 单据日期 / 商品金额 /
            订单金额 / 活动名称 / 活动状态
        对标文档：docs/Yh-Spec/手动整理对标开发文档/营销模块/商城预售开发文档.md
        后端：复用 /erp/marketing/presale（mkt_presale，V11.373.0 补 商品图片/规格/型号/预售价/是否支付订金 5 列）
              与新增的 /erp/marketing/presale/order/page（联销售单据 + 往来单位）
        对标无「页面配置」弹窗
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <template #toolbar-left>
          <a-button
            v-if="activeTab === 'presale'"
            type="primary"
            size="small"
            class="btn-add"
            @click="openCreate"
          >
            <PlusOutlined /> 新增预售活动
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
              <template v-if="activeTab === 'presale'">
                <span class="search-label">活动名称</span>
                <a-input
                  v-model:value="searchForm.activityName"
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
              </template>
              <template v-else>
                <span class="search-label">客户名称</span>
                <a-input
                  v-model:value="orderSearch.customer"
                  placeholder="请输入客户名称"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  @press-enter="handleSearch"
                />
                <span class="search-label">单据编号</span>
                <a-input
                  v-model:value="orderSearch.orderNo"
                  placeholder="请输入单据编号"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
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
            <!-- ═══ Tab1 商品预售 ═══ -->
            <BillDetailTable
              v-if="activeTab === 'presale'"
              v-model:data-source="presaleData"
              :columns="presaleColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="id"
              storage-key="marketing-mall-presale-table-columns"
              global-config-key="marketing-mall-presale-table-columns"
            >
              <template #nameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openEdit(record)"
                >{{ record.activityName }}</a>
              </template>
              <template #imageCell="{ record }">
                <img
                  v-if="!record.__ghost && record.productImage"
                  :src="record.productImage"
                  @error="onImageError"
                  class="presale-img"
                  alt="商品图片"
                >
                <span v-else-if="!record.__ghost">-</span>
              </template>
              <template #timeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ fmtTime(record[column.key]) }}</span>
              </template>
              <template #depositCell="{ record }">
                <span v-if="!record.__ghost">{{ record.depositRequired === 1 ? '是' : '否' }}</span>
              </template>
              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="PRESALE_STATUS_MAP[record.status]?.color || 'default'"
                >
                  {{ PRESALE_STATUS_MAP[record.status]?.text || record.status }}
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
                      <a-menu @click="(e: any) => handlePresaleMore(e, record)">
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
                        <a-menu-item key="orders">查看预售订单</a-menu-item>
                        <a-menu-item key="delete">删除</a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </a-space>
              </template>
            </BillDetailTable>

            <!-- ═══ Tab2 预售订单 ═══ -->
            <BillDetailTable
              v-else
              v-model:data-source="orderData"
              :columns="orderColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="id"
              storage-key="marketing-mall-presale-order-table-columns"
              global-config-key="marketing-mall-presale-order-table-columns"
            >
              <template #dateCell="{ record, column }">
                <span v-if="!record.__ghost">{{ fmtTime(record[column.key]) }}</span>
              </template>
              <template #moneyCell="{ record, column }">
                <span v-if="!record.__ghost">{{ fmtMoney(record[column.key]) }}</span>
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

      <!-- ═══ 新增/修改预售活动 ═══ -->
      <a-modal
        v-model:open="formOpen"
        :title="editingId ? '修改预售活动' : '新增预售活动'"
        :confirm-loading="saving"
        width="720px"
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
              v-model:value="form.activityName"
              placeholder="请输入活动名称"
            />
          </a-form-item>
          <a-form-item label="预售商品">
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
              <a-form-item label="预售价">
                <a-input-number
                  v-model:value="form.presalePrice"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="是否支付订金">
                <a-switch
                  v-model:checked="form.depositRequiredBool"
                  checked-children="是"
                  un-checked-children="否"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="定金">
                <a-input-number
                  v-model:value="form.depositAmount"
                  :min="0"
                  :precision="2"
                  :disabled="!form.depositRequiredBool"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="尾款">
                <a-input-number
                  v-model:value="form.finalAmount"
                  :min="0"
                  :precision="2"
                  :disabled="!form.depositRequiredBool"
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
              <a-form-item label="预售限量">
                <a-input-number
                  v-model:value="form.stockLimit"
                  :min="0"
                  :precision="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="商品图片">
                <a-input
                  v-model:value="form.productImage"
                  placeholder="图片URL"
                  allow-clear
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

      <ProductSelectModal
        v-model:open="productPickerOpen"
        @confirm="handleProductPicked"
      />
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="marketing-mall-presale"
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
import { presaleApi, PRESALE_STATUS_MAP } from '@/api/marketing'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'MarketingMallPresale' })

const TABS = [
  { key: 'presale', label: '商品预售' },
  { key: 'order', label: '预售订单' },
]
const activeTab = ref('presale')

const STATUS_OPTIONS = Object.entries(PRESALE_STATUS_MAP).map(([value, v]) => ({ value: Number(value), label: v.text }))

const loading = ref(false)
const exporting = ref(false)
const presaleData = ref<any[]>([])
const orderData = ref<any[]>([])
const searchForm = reactive({ activityName: '', status: undefined as number | undefined })
const orderSearch = reactive({ customer: '', orderNo: '' })
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// Tab1 商品预售（对标 12 列）
const presaleColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 160, fixed: 'left' },
  { key: 'activityName', title: '活动名称', type: 'slot', slotName: 'nameCell', width: 220 },
  { key: 'productName', title: '商品名称', type: 'input', width: 200 },
  { key: 'productImage', title: '商品图片', type: 'slot', slotName: 'imageCell', width: 90 },
  { key: 'productCode', title: '货号', type: 'input', width: 130 },
  { key: 'productSpec', title: '规格', type: 'input', width: 130 },
  { key: 'productModel', title: '型号', type: 'input', width: 120 },
  { key: 'presalePrice', title: '预售价', type: 'input', width: 110 },
  { key: 'startTime', title: '起始时间', type: 'slot', slotName: 'timeCell', width: 170 },
  { key: 'endTime', title: '结束时间', type: 'slot', slotName: 'timeCell', width: 170 },
  { key: 'depositRequired', title: '是否支付订金', type: 'slot', slotName: 'depositCell', width: 120 },
  { key: 'status', title: '活动状态', type: 'slot', slotName: 'statusCell', width: 110 },
  { key: 'creatorName', title: '创建人', type: 'input', width: 120 },
]

// Tab2 预售订单（对标 8 列）
const orderColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'customerName', title: '客户名称', type: 'input', width: 220 },
  { key: 'productName', title: '商品名称', type: 'input', width: 220 },
  { key: 'orderNo', title: '单据编号', type: 'input', width: 180 },
  { key: 'billDate', title: '单据日期', type: 'slot', slotName: 'dateCell', width: 170 },
  { key: 'productAmount', title: '商品金额', type: 'slot', slotName: 'moneyCell', width: 120 },
  { key: 'orderAmount', title: '订单金额', type: 'slot', slotName: 'moneyCell', width: 120 },
  { key: 'activityName', title: '活动名称', type: 'input', width: 200 },
  { key: 'activityStatus', title: '活动状态', type: 'input', width: 110 },
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
    if (activeTab.value === 'presale') {
      const res: any = await presaleApi.page({
        activityName: searchForm.activityName || undefined,
        status: searchForm.status,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      })
      presaleData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    } else {
      const res: any = await presaleApi.orderPage({
        customer: orderSearch.customer || undefined,
        orderNo: orderSearch.orderNo || undefined,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      })
      orderData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    }
  } catch (error: any) {
    console.error('[商城预售] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    if (activeTab.value === 'presale') presaleData.value = []
    else orderData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
  fetchList()
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
  activityName: '',
  productId: undefined as any,
  productName: '',
  productCode: '',
  productImage: '',
  productSpec: '',
  productModel: '',
  presalePrice: 0 as number | undefined,
  depositRequiredBool: false,
  depositAmount: 0 as number | undefined,
  finalAmount: 0 as number | undefined,
  stockLimit: 0 as number | undefined,
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
    activityName: record.activityName || '',
    productId: record.productId,
    productName: record.productName || '',
    productCode: record.productCode || '',
    productImage: record.productImage || '',
    productSpec: record.productSpec || '',
    productModel: record.productModel || '',
    presalePrice: record.presalePrice ?? 0,
    depositRequiredBool: record.depositRequired === 1,
    depositAmount: record.depositAmount ?? 0,
    finalAmount: record.finalAmount ?? 0,
    stockLimit: record.stockLimit ?? 0,
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
  form.productName = p.productName || p.name || ''
  form.productCode = p.productCode || p.code || ''
  form.productSpec = p.spec || p.specification || ''
  form.productModel = p.model || ''
  form.productImage = p.imageUrl || ''
  if (!form.activityName) form.activityName = `${form.productName} 预售`
}

async function handleSave() {
  if (!form.activityName?.trim()) {
    message.warning('请输入活动名称')
    return
  }
  if (!form.productId) {
    message.warning('请选择预售商品')
    return
  }
  if (form.startTime && form.endTime && dayjs(form.endTime).isBefore(dayjs(form.startTime))) {
    message.warning('结束时间不得早于起始时间')
    return
  }
  saving.value = true
  try {
    const payload: any = {
      activityName: form.activityName,
      productId: form.productId,
      productName: form.productName,
      productCode: form.productCode,
      productImage: form.productImage || undefined,
      productSpec: form.productSpec || undefined,
      productModel: form.productModel || undefined,
      presalePrice: form.presalePrice,
      depositRequired: form.depositRequiredBool ? 1 : 0,
      depositAmount: form.depositRequiredBool ? form.depositAmount : 0,
      finalAmount: form.depositRequiredBool ? form.finalAmount : 0,
      stockLimit: form.stockLimit,
      startTime: form.startTime || undefined,
      endTime: form.endTime || undefined,
      remark: form.remark,
    }
    if (editingId.value) {
      await presaleApi.update(editingId.value, payload)
      message.success('预售活动已更新')
    } else {
      await presaleApi.create(payload)
      message.success('预售活动已新增')
    }
    formOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handlePresaleMore(e: any, record: any) {
  if (e.key === 'publish') {
    try {
      await presaleApi.publish(record.id)
      message.success('已发布')
      fetchList()
    } catch (error: any) {
      message.error(error?.response?.data?.message || '发布失败')
    }
  } else if (e.key === 'cancel') {
    try {
      await presaleApi.cancel(record.id)
      message.success('已取消')
      fetchList()
    } catch (error: any) {
      message.error(error?.response?.data?.message || '取消失败')
    }
  } else if (e.key === 'orders') {
    activeTab.value = 'order'
    orderSearch.customer = ''
    orderSearch.orderNo = ''
    pagination.current = 1
    fetchList()
  } else if (e.key === 'delete') {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除预售活动「${record.activityName}」吗？`,
      okText: '确认删除',
      okType: 'danger',
      onOk: async () => {
        try {
          await presaleApi.remove(record.id)
          message.success('删除成功')
          fetchList()
        } catch (error: any) {
          message.error(error?.response?.data?.message || '删除失败')
        }
      },
    })
  }
}

// ═══ 打印 / 导出 ═══
const PRINT_COLUMNS_PRESALE = [
  { key: 'activityName', title: '活动名称' },
  { key: 'productName', title: '商品名称' },
  { key: 'productCode', title: '货号' },
  { key: 'productSpec', title: '规格' },
  { key: 'productModel', title: '型号' },
  { key: 'presalePrice', title: '预售价' },
  { key: 'startTime', title: '起始时间' },
  { key: 'endTime', title: '结束时间' },
  { key: 'depositText', title: '是否支付订金' },
  { key: 'statusText', title: '活动状态' },
  { key: 'creatorName', title: '创建人' },
]
const PRINT_COLUMNS_ORDER = [
  { key: 'customerName', title: '客户名称' },
  { key: 'productName', title: '商品名称' },
  { key: 'orderNo', title: '单据编号' },
  { key: 'billDate', title: '单据日期' },
  { key: 'productAmount', title: '商品金额' },
  { key: 'orderAmount', title: '订单金额' },
  { key: 'activityName', title: '活动名称' },
  { key: 'activityStatus', title: '活动状态' },
]

function currentPrint() {
  if (activeTab.value === 'presale') {
    return {
      title: '商品预售',
      cols: PRINT_COLUMNS_PRESALE,
      rows: (presaleData.value || []).filter((r: any) => !r.__ghost),
      cell: (r: any, key: string) => {
        if (key === 'depositText') return r.depositRequired === 1 ? '是' : '否'
        if (key === 'statusText') return PRESALE_STATUS_MAP[r.status]?.text || ''
        if (key === 'startTime' || key === 'endTime') return fmtTime(r[key])
        return r[key] ?? ''
      },
    }
  }
  return {
    title: '预售订单',
    cols: PRINT_COLUMNS_ORDER,
    rows: (orderData.value || []).filter((r: any) => !r.__ghost),
    cell: (r: any, key: string) => {
      if (key === 'billDate') return fmtTime(r[key])
      if (key === 'productAmount' || key === 'orderAmount') return fmtMoney(r[key])
      return r[key] ?? ''
    },
  }
}

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML + window.open()，现在交给 PrintDialog：列与行由页面给，模板负责版式。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'marketing-mall-presale',
  // 列/行/标题随 Tab（商品预售 / 预售订单）变，静态生成器写不进模板 → 明确按数据列打
  useDataColumns: true,
  title: () => currentPrint().title,
  columns: () => {
    const { cols, cell } = currentPrint()
    return cols.map(c => ({
      key: c.key,
      title: c.title,
      formatter: (_v: any, row: any) => String(cell(row, c.key) ?? ''),
    }))
  },
  rows: () => currentPrint().rows,
  emptyTip: '没有可打印的数据',
})

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

function handleExport() {
  const { cols, rows, cell } = currentPrint()
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  exporting.value = true
  try {
    const csv = '\uFEFF' + [cols.map(c => c.title),
      ...rows.map((r: any) => cols.map(c => cell(r, c.key)))]
      .map(line => line.map(v => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\r\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `商城预售_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
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
  console.error('[商城预售] 页面错误', error)
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
.presale-img { width: 48px; height: 48px; object-fit: cover; border-radius: 4px; }
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
