<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        特价（营销 → 营销活动 → 特价，菜单 80314）
        对标 ql361「营销 → 营销活动 → 特价」：单视图列表页，9 列（全部默认可见）
        列：活动名称 / 起始时间 / 结束时间 / 促销商品 / 使用范围 / 客户范围 / 活动状态 / 创建人 / 创建时间
        对标文档：docs/Yh-Spec/手动整理对标开发文档/营销模块/特价开发文档.md
        实测实据（首条）：活动名称「香芋地瓜丸3件起特价」、使用范围「线上线下」、行级「查看修改更多」
        后端：复用促销活动表（促销方式=特价 SPECIAL_PRICE），与商品促销/整单促销同源
        对标无「页面配置」弹窗；行级 查看 / 修改 / 更多
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
                v-model:value="searchForm.name"
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
              storage-key="marketing-special-price-table-columns"
              global-config-key="marketing-special-price-table-columns"
            >
              <template #nameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openEdit(record)"
                >{{ record.name }}</a>
              </template>

              <template #timeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ fmtTime(record[column.key]) }}</span>
              </template>

              <template #productsCell="{ record }">
                <a
                  v-if="!record.__ghost && record.productIds"
                  class="cell-link"
                  @click="openProducts(record)"
                >查看商品</a>
                <span v-else-if="!record.__ghost">全部商品</span>
              </template>

              <template #customersCell="{ record }">
                <a
                  v-if="!record.__ghost && record.customerIds"
                  class="cell-link"
                  @click="openCustomers(record)"
                >查看客户</a>
                <span v-else-if="!record.__ghost">{{ record.customerLevels || '全部客户' }}</span>
              </template>

              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="PROMO_STATUS_MAP[record.status]?.color || 'default'"
                >
                  {{ PROMO_STATUS_MAP[record.status]?.text || record.status || '-' }}
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
                    @click="openView(record)"
                  >
                    查看
                  </a-button>
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
                        <a-menu-item key="toggle">
                          {{ record.status === 'cancelled' ? '启用活动' : '停用活动' }}
                        </a-menu-item>
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

      <!-- ═══ 新增/修改特价活动 ═══ -->
      <a-modal
        v-model:open="formOpen"
        :title="editingId ? '修改特价' : '新增特价'"
        :confirm-loading="saving"
        width="720px"
        @ok="handleSave"
      >
        <a-form
          ref="formRef"
          :model="form"
          :rules="rules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          size="small"
          style="margin-top: 12px"
        >
          <a-row :gutter="8">
            <a-col :span="12">
              <a-form-item
                label="活动名称"
                name="name"
              >
                <a-input v-model:value="form.name" placeholder="请输入活动名称" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="使用范围">
                <a-select
                  v-model:value="form.promoScope"
                  :options="SCOPE_OPTIONS"
                  placeholder="请选择"
                  allow-clear
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
            <a-col :span="24">
              <a-form-item
                label="促销商品"
                :label-col="{ span: 3 }"
                :wrapper-col="{ span: 20 }"
              >
                <a-space>
                  <a-button
                    size="small"
                    @click="productPickerOpen = true"
                  >
                    选择商品
                  </a-button>
                  <span class="selected-tip">已选 {{ selectedProducts.length }} 个商品</span>
                  <a-button
                    v-if="selectedProducts.length"
                    size="small"
                    type="link"
                    @click="selectedProducts = []"
                  >
                    清空
                  </a-button>
                </a-space>
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item
                label="客户范围"
                :label-col="{ span: 3 }"
                :wrapper-col="{ span: 20 }"
              >
                <a-space>
                  <a-button
                    size="small"
                    @click="customerPickerOpen = true"
                  >
                    选择客户
                  </a-button>
                  <span class="selected-tip">已选 {{ selectedCustomers.length }} 个客户</span>
                  <a-button
                    v-if="selectedCustomers.length"
                    size="small"
                    type="link"
                    @click="selectedCustomers = []"
                  >
                    清空
                  </a-button>
                </a-space>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="特价单价">
                <a-input-number
                  v-model:value="form.promoPrice"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <div class="field-tip">
                服务端按「（行成交价 − 特价单价）× 数量」计算优惠；不填则本活动不产生金额优惠
              </div>
            </a-col>
            <a-col :span="24">
              <a-form-item
                label="特价规则"
                :label-col="{ span: 3 }"
                :wrapper-col="{ span: 20 }"
              >
                <a-textarea
                  v-model:value="form.description"
                  :rows="2"
                  placeholder="请输入特价规则描述"
                />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-divider orientation="left" class="form-divider">
                叠加设置（优先级 / 互斥 / 封顶）
              </a-divider>
            </a-col>
            <a-col :span="12">
              <a-form-item label="优先级">
                <a-input-number
                  v-model:value="form.priority"
                  :min="0"
                  :precision="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="叠加策略">
                <a-select
                  v-model:value="form.stackPolicy"
                  :options="STACK_POLICY_OPTIONS"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="单笔封顶">
                <a-input-number
                  v-model:value="form.maxDiscountAmount"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                  placeholder="不填＝不封顶"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="活动总次数">
                <a-input-number
                  v-model:value="form.quotaTotal"
                  :min="0"
                  :precision="0"
                  style="width: 100%"
                  placeholder="不填＝不限"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </a-modal>

      <a-modal
        v-model:open="productsOpen"
        title="促销商品"
        width="800px"
        :footer="null"
      >
        <a-table
          :columns="PRODUCT_COLUMNS"
          :data-source="productRows"
          :loading="refLoading"
          :pagination="false"
          row-key="id"
          size="small"
        />
      </a-modal>

      <a-modal
        v-model:open="customersOpen"
        title="客户范围"
        width="800px"
        :footer="null"
      >
        <a-table
          :columns="CUSTOMER_COLUMNS"
          :data-source="customerRows"
          :loading="refLoading"
          :pagination="false"
          row-key="id"
          size="small"
        />
      </a-modal>

      <ProductSelectModal
        v-model:open="productPickerOpen"
        multiple
        @confirm="handleProductsPicked"
      />
      <PartnerSelectModal
        v-model:open="customerPickerOpen"
        default-tab="customer"
        @select="handleCustomerPicked"
      />
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="marketing-special-price"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, h } from 'vue'
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
import PartnerSelectModal from '@/components/PartnerSelectModal/index.vue'
import { promoActivityApi, PROMO_STATUS_MAP, type PromoActivity } from '@/api/marketing'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'MarketingSpecialPrice' })

const STATUS_OPTIONS = Object.entries(PROMO_STATUS_MAP).map(([value, v]) => ({ value, label: v.text }))
const SCOPE_OPTIONS = [
  { value: '线下使用', label: '线下使用' },
  { value: '线上线下', label: '线上线下' },
  { value: '商城使用', label: '商城使用' },
]
/** 叠加策略：STACK 可叠加 / EXCLUSIVE 独占（命中后更低优先级活动不再计算） */
const STACK_POLICY_OPTIONS = [
  { value: 'STACK', label: '可叠加（STACK）' },
  { value: 'EXCLUSIVE', label: '独占（EXCLUSIVE）' },
]

const loading = ref(false)
const exporting = ref(false)
const refLoading = ref(false)
const tableData = ref<PromoActivity[]>([])
const searchForm = reactive({ name: '', status: undefined as string | undefined })
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// 对标 9 列（全部默认可见）
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 150, fixed: 'left' },
  { key: 'name', title: '活动名称', type: 'slot', slotName: 'nameCell', width: 260 },
  { key: 'startTime', title: '起始时间', type: 'slot', slotName: 'timeCell', width: 170 },
  { key: 'endTime', title: '结束时间', type: 'slot', slotName: 'timeCell', width: 170 },
  { key: 'productIds', title: '促销商品', type: 'slot', slotName: 'productsCell', width: 110 },
  { key: 'promoScope', title: '使用范围', type: 'input', width: 120 },
  { key: 'customerLevels', title: '客户范围', type: 'slot', slotName: 'customersCell', width: 130 },
  { key: 'status', title: '活动状态', type: 'slot', slotName: 'statusCell', width: 110 },
  { key: 'creatorName', title: '创建人', type: 'input', width: 110 },
  { key: 'createTime', title: '创建时间', type: 'slot', slotName: 'timeCell', width: 170 },
]

function fmtTime(v: any): string {
  return v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-'
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await promoActivityApi.page({
      activityType: 'SPECIAL_PRICE',
      name: searchForm.name || undefined,
      status: searchForm.status || undefined,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[特价] 加载列表失败', error)
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

const formRef = ref()
const formOpen = ref(false)
const saving = ref(false)
const editingId = ref<any>(null)
const selectedProducts = ref<any[]>([])
const selectedCustomers = ref<any[]>([])
const productPickerOpen = ref(false)
const customerPickerOpen = ref(false)

const emptyForm = () => ({
  name: '',
  startTime: undefined as any,
  endTime: undefined as any,
  promoScope: undefined as any,
  description: '',
  // ── 促销引擎结构化配置 ──
  promoPrice: undefined as number | undefined,
  priority: 0 as number | undefined,
  stackPolicy: 'STACK' as string,
  maxDiscountAmount: undefined as number | undefined,
  quotaTotal: undefined as number | undefined,
})
const form = reactive(emptyForm())
const rules: Record<string, any> = {
  name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  selectedProducts.value = []
  selectedCustomers.value = []
  formOpen.value = true
}

function openEdit(record: any) {
  editingId.value = record.id
  Object.assign(form, emptyForm(), {
    name: record.name || '',
    startTime: record.startTime || undefined,
    endTime: record.endTime || undefined,
    promoScope: record.promoScope || undefined,
    description: record.description || '',
    promoPrice: record.promoPrice ?? undefined,
    priority: record.priority ?? 0,
    stackPolicy: record.stackPolicy || 'STACK',
    maxDiscountAmount: record.maxDiscountAmount ?? undefined,
    quotaTotal: record.quotaTotal ?? undefined,
  })
  selectedProducts.value = parseIds(record.productIds).map((id: string) => ({ id }))
  selectedCustomers.value = parseIds(record.customerIds).map((id: string) => ({ id }))
  formOpen.value = true
}

function openView(record: any) {
  const lines = [
    '促销方式：特价',
    `使用范围：${record.promoScope || '-'}`,
    `特价规则：${record.description || '-'}`,
    `起始时间：${fmtTime(record.startTime)}`,
    `结束时间：${fmtTime(record.endTime)}`,
  ]
  Modal.info({
    title: record.name,
    width: 600,
    content: h('div', { style: 'font-size:13px;line-height:24px' }, lines.map(l => h('div', l))),
  })
}

function parseIds(csv: any): string[] {
  if (!csv) return []
  return String(csv).split(',').map((s: string) => s.trim()).filter(Boolean)
}

function handleProductsPicked(products: any[]) {
  selectedProducts.value = (products || []).map(p => ({ id: p.id, name: p.productName || p.name }))
}
function handleCustomerPicked(record: any) {
  if (!record?.id) return
  if (!selectedCustomers.value.some(c => String(c.id) === String(record.id))) {
    selectedCustomers.value.push({ id: record.id, name: record.partyName || record.partnerName })
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const payload: any = {
      name: form.name,
      type: 'SPECIAL_PRICE',
      startTime: form.startTime || undefined,
      endTime: form.endTime || undefined,
      promoScope: form.promoScope,
      description: form.description,
      // 促销引擎结构化配置（不填则该特价活动不产生金额优惠）
      promoPrice: form.promoPrice ?? undefined,
      priority: form.priority ?? 0,
      stackPolicy: form.stackPolicy || 'STACK',
      maxDiscountAmount: form.maxDiscountAmount ?? undefined,
      quotaTotal: form.quotaTotal ?? undefined,
      productIds: selectedProducts.value.map(p => p.id).join(','),
      customerIds: selectedCustomers.value.map(c => c.id).join(','),
      customerLevels: selectedCustomers.value.length
        ? `已指定 ${selectedCustomers.value.length} 个客户` : '全部客户',
    }
    if (editingId.value) {
      await promoActivityApi.update(editingId.value, payload)
      message.success('特价活动已更新')
    } else {
      await promoActivityApi.create(payload)
      message.success('特价活动已新增')
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
  if (e.key === 'toggle') {
    const next = record.status === 'cancelled' ? 'published' : 'cancelled'
    try {
      await promoActivityApi.changeStatus(record.id, next)
      message.success(next === 'published' ? '活动已启用' : '活动已停用')
      fetchList()
    } catch (error: any) {
      message.error(error?.response?.data?.message || '操作失败')
    }
  } else if (e.key === 'delete') {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除特价活动「${record.name}」吗？此操作不可恢复。`,
      okText: '确认删除',
      okType: 'danger',
      onOk: async () => {
        try {
          await promoActivityApi.remove(record.id)
          message.success('删除成功')
          fetchList()
        } catch (error: any) {
          message.error(error?.response?.data?.message || '删除失败')
        }
      },
    })
  }
}

const productsOpen = ref(false)
const customersOpen = ref(false)
const productRows = ref<any[]>([])
const customerRows = ref<any[]>([])

const PRODUCT_COLUMNS = [
  { title: '货号', dataIndex: 'productCode', key: 'productCode', width: 140 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName' },
  { title: '规格', dataIndex: 'spec', key: 'spec', width: 140 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 80 },
  { title: '零售价', dataIndex: 'retailPrice', key: 'retailPrice', width: 100 },
  { title: '批发价', dataIndex: 'wholesalePrice', key: 'wholesalePrice', width: 100 },
]
const CUSTOMER_COLUMNS = [
  { title: '客户编号', dataIndex: 'partyCode', key: 'partyCode', width: 140 },
  { title: '客户名称', dataIndex: 'partyName', key: 'partyName' },
  { title: '会员级别', dataIndex: 'memberLevel', key: 'memberLevel', width: 120 },
  { title: '联系电话', dataIndex: 'phone', key: 'phone', width: 140 },
]

async function openProducts(record: any) {
  productsOpen.value = true
  refLoading.value = true
  try {
    productRows.value = await promoActivityApi.products(record.id)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载促销商品失败')
    productRows.value = []
  } finally {
    refLoading.value = false
  }
}
async function openCustomers(record: any) {
  customersOpen.value = true
  refLoading.value = true
  try {
    customerRows.value = await promoActivityApi.customers(record.id)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载客户范围失败')
    customerRows.value = []
  } finally {
    refLoading.value = false
  }
}

const PRINT_COLUMNS = [
  { key: 'name', title: '活动名称' },
  { key: 'startTime', title: '起始时间' },
  { key: 'endTime', title: '结束时间' },
  { key: 'promoScope', title: '使用范围' },
  { key: 'customerLevels', title: '客户范围' },
  { key: 'statusText', title: '活动状态' },
  { key: 'creatorName', title: '创建人' },
  { key: 'createTime', title: '创建时间' },
]
function printCell(row: any, key: string): string {
  if (key === 'statusText') return PROMO_STATUS_MAP[row.status]?.text || ''
  if (key === 'startTime' || key === 'endTime' || key === 'createTime') return fmtTime(row[key])
  return row[key] == null ? '' : String(row[key])
}

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML + window.open()，现在交给 PrintDialog：列与行由页面给，模板负责版式。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'marketing-special-price',
  title: '特价',
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
    a.download = `特价_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } finally {
    exporting.value = false
  }
}

function handleError(error: Error) {
  console.error('[特价] 页面错误', error)
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
.selected-tip { font-size: 12px; color: #999; }
.form-divider { font-size: 13px; color: #666; margin: 4px 0 12px; }
.field-tip { font-size: 12px; color: #999; padding-top: 6px; }
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
