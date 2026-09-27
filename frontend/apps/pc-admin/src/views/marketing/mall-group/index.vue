<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        商城拼团（营销 → 商城营销 → 商城拼团，菜单 80320）
        对标 ql361「营销 → 商城营销 → 商城拼团」：**双视图 Tab 复合页**（逐 Tab 独立列配置）
          · Tab1「拼团活动」9 列：活动ID / 活动名称 / 起始时间 / 结束时间 / 成团类型 /
            开团个数 / 成功团个数 / 活动状态 / 创建时间
          · Tab2「拼团订单」11 列：拼团编号 / 客户名称 / 商品名称 / 订单编号 / 提交时间 /
            单据时间 / 商品金额 / 订单金额 / 活动名称 / 活动ID / 拼团状态
        对标文档：docs/Yh-Spec/手动整理对标开发文档/营销模块/商城拼团开发文档.md
        后端：复用 /erp/marketing/group-buy（erp_group_buy_activity + erp_group_buy_participant），
              两个 Tab 走新增的 /activity/page（含开团/成功团聚合）与 /order/page（联销售单据）
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
            v-if="activeTab === 'activity'"
            type="primary"
            size="small"
            class="btn-add"
            @click="openCreate"
          >
            <PlusOutlined /> 新增拼团活动
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
              <template v-if="activeTab === 'activity'">
                <span class="search-label">活动名称</span>
                <a-input
                  v-model:value="activitySearch.name"
                  placeholder="请输入活动名称"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  @press-enter="handleSearch"
                />
                <span class="search-label">活动状态</span>
                <a-select
                  v-model:value="activitySearch.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  :options="STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </template>
              <template v-else>
                <span class="search-label">拼团编号</span>
                <a-input
                  v-model:value="orderSearch.groupId"
                  placeholder="请输入拼团编号"
                  size="small"
                  style="width: 170px"
                  allow-clear
                  @press-enter="handleSearch"
                />
                <span class="search-label">客户名称</span>
                <a-input
                  v-model:value="orderSearch.customer"
                  placeholder="请输入客户名称"
                  size="small"
                  style="width: 170px"
                  allow-clear
                  @press-enter="handleSearch"
                />
                <span class="search-label">订单编号</span>
                <a-input
                  v-model:value="orderSearch.orderNo"
                  placeholder="请输入订单编号"
                  size="small"
                  style="width: 170px"
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
            <!-- ═══ Tab1 拼团活动 ═══ -->
            <BillDetailTable
              v-if="activeTab === 'activity'"
              v-model:data-source="activityData"
              :columns="activityColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="id"
              storage-key="marketing-mall-group-activity-table-columns"
              global-config-key="marketing-mall-group-activity-table-columns"
            >
              <template #nameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openEdit(record)"
                >{{ record.activityName }}</a>
              </template>
              <template #timeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ fmtTime(record[column.key]) }}</span>
              </template>
              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="GROUP_STATUS_MAP[record.status]?.color || 'default'"
                >
                  {{ GROUP_STATUS_MAP[record.status]?.text || record.status }}
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
                      <a-menu @click="(e: any) => handleActivityMore(e, record)">
                        <a-menu-item key="toggle">
                          {{ record.status === 'ACTIVE' ? '停用' : '启用' }}
                        </a-menu-item>
                        <a-menu-item key="orders">查看拼团订单</a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </a-space>
              </template>
            </BillDetailTable>

            <!-- ═══ Tab2 拼团订单 ═══ -->
            <BillDetailTable
              v-else
              v-model:data-source="orderData"
              :columns="orderColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="id"
              storage-key="marketing-mall-group-order-table-columns"
              global-config-key="marketing-mall-group-order-table-columns"
            >
              <template #timeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ fmtTime(record[column.key]) }}</span>
              </template>
              <template #moneyCell="{ record, column }">
                <span v-if="!record.__ghost">{{ fmtMoney(record[column.key]) }}</span>
              </template>
              <template #groupStatusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="record.groupStatus === 'SUCCESS' ? 'green' : 'default'"
                >
                  {{ record.groupStatus === 'SUCCESS' ? '已成团' : (record.groupStatus || '-') }}
                </a-tag>
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

      <!-- ═══ 新增/修改拼团活动 ═══ -->
      <a-modal
        v-model:open="formOpen"
        :title="editingId ? '修改拼团活动' : '新增拼团活动'"
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
              v-model:value="form.activityName"
              placeholder="请输入活动名称"
            />
          </a-form-item>
          <a-form-item label="拼团商品">
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
              <a-form-item label="拼团价">
                <a-input-number
                  v-model:value="form.groupPrice"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="成团人数">
                <a-input-number
                  v-model:value="form.minGroupSize"
                  :min="2"
                  :precision="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="最大成团人数">
                <a-input-number
                  v-model:value="form.maxGroupSize"
                  :min="2"
                  :precision="0"
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
              <a-form-item label="团购总量">
                <a-input-number
                  v-model:value="form.totalQuantity"
                  :min="0"
                  :precision="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="成团时限(分钟)">
                <a-input-number
                  v-model:value="form.timeLimitMinutes"
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

      <ProductSelectModal
        v-model:open="productPickerOpen"
        @confirm="handleProductPicked"
      />
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="marketing-mall-group"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { PlusOutlined, ReloadOutlined, PrinterOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import ProductSelectModal from '@/components/ProductSelectModal/index.vue'
import { groupBuyApi } from '@/api/marketing'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'MarketingMallGroup' })

const TABS = [
  { key: 'activity', label: '拼团活动' },
  { key: 'order', label: '拼团订单' },
]
const activeTab = ref('activity')

/** 拼团活动状态（erp_group_buy_activity.status，字符串枚举） */
const GROUP_STATUS_MAP: Record<string, { text: string; color: string }> = {
  DRAFT: { text: '未开始', color: 'default' },
  ACTIVE: { text: '进行中', color: 'green' },
  FINISHED: { text: '已结束', color: 'orange' },
  CANCELLED: { text: '已取消', color: 'red' },
}
const STATUS_OPTIONS = Object.entries(GROUP_STATUS_MAP).map(([value, v]) => ({ value, label: v.text }))

const loading = ref(false)
const exporting = ref(false)
const activityData = ref<any[]>([])
const orderData = ref<any[]>([])
const activitySearch = reactive({ name: '', status: undefined as string | undefined })
const orderSearch = reactive({ groupId: '', customer: '', orderNo: '' })
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// Tab1 拼团活动（对标 9 列）
const activityColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 150, fixed: 'left' },
  { key: 'activityCode', title: '活动ID', type: 'input', width: 140 },
  { key: 'activityName', title: '活动名称', type: 'slot', slotName: 'nameCell', width: 240 },
  { key: 'startTime', title: '起始时间', type: 'slot', slotName: 'timeCell', width: 170 },
  { key: 'endTime', title: '结束时间', type: 'slot', slotName: 'timeCell', width: 170 },
  { key: 'groupType', title: '成团类型', type: 'input', width: 110 },
  { key: 'groupCount', title: '开团个数', type: 'input', width: 100 },
  { key: 'successGroupCount', title: '成功团个数', type: 'input', width: 110 },
  { key: 'status', title: '活动状态', type: 'slot', slotName: 'statusCell', width: 110 },
  { key: 'createTime', title: '创建时间', type: 'slot', slotName: 'timeCell', width: 170 },
]

// Tab2 拼团订单（对标 11 列）
const orderColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'groupId', title: '拼团编号', type: 'input', width: 160 },
  { key: 'customerName', title: '客户名称', type: 'input', width: 200 },
  { key: 'productName', title: '商品名称', type: 'input', width: 200 },
  { key: 'orderNo', title: '订单编号', type: 'input', width: 170 },
  { key: 'submitTime', title: '提交时间', type: 'slot', slotName: 'timeCell', width: 170 },
  { key: 'billDate', title: '单据时间', type: 'slot', slotName: 'timeCell', width: 170 },
  { key: 'productAmount', title: '商品金额', type: 'slot', slotName: 'moneyCell', width: 120 },
  { key: 'orderAmount', title: '订单金额', type: 'slot', slotName: 'moneyCell', width: 120 },
  { key: 'activityName', title: '活动名称', type: 'input', width: 200 },
  { key: 'activityCode', title: '活动ID', type: 'input', width: 140 },
  { key: 'groupStatus', title: '拼团状态', type: 'slot', slotName: 'groupStatusCell', width: 110 },
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
    if (activeTab.value === 'activity') {
      const res: any = await groupBuyApi.activityPage({
        name: activitySearch.name || undefined,
        status: activitySearch.status || undefined,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      })
      activityData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    } else {
      const res: any = await groupBuyApi.orderPage({
        groupId: orderSearch.groupId || undefined,
        customer: orderSearch.customer || undefined,
        orderNo: orderSearch.orderNo || undefined,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      })
      orderData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    }
  } catch (error: any) {
    console.error('[商城拼团] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    if (activeTab.value === 'activity') activityData.value = []
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
  activityCode: '',
  activityName: '',
  productId: undefined as any,
  productName: '',
  originalPrice: 0 as number | undefined,
  groupPrice: 0 as number | undefined,
  minGroupSize: 2 as number | undefined,
  maxGroupSize: 2 as number | undefined,
  timeLimitMinutes: 0 as number | undefined,
  totalQuantity: 0 as number | undefined,
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
async function openEdit(record: any) {
  editingId.value = record.id
  // 列表行来自「拼团活动」聚合视图（只含 9 列）；编辑需要原价/成团时限/团购总量等完整字段，故回查详情
  let detail: any = record
  try {
    detail = await groupBuyApi.getById(record.id)
  } catch (e) {
    console.error('[商城拼团] 回查活动详情失败，改用列表行', e)
  }
  Object.assign(form, emptyForm(), {
    activityCode: detail.activityCode || '',
    activityName: detail.activityName || '',
    productId: detail.productId,
    productName: detail.productName || '',
    originalPrice: detail.originalPrice ?? 0,
    groupPrice: detail.groupPrice ?? 0,
    minGroupSize: detail.minGroupSize ?? 2,
    maxGroupSize: detail.maxGroupSize ?? 2,
    timeLimitMinutes: detail.timeLimitMinutes ?? 0,
    totalQuantity: detail.totalQuantity ?? 0,
    startTime: detail.startTime || undefined,
    endTime: detail.endTime || undefined,
    remark: detail.remark || '',
  })
  formOpen.value = true
}
function handleProductPicked(products: any[]) {
  const p = products?.[0]
  if (!p) return
  form.productId = p.id
  form.productName = p.productName || p.name || ''
  if (!form.activityName) form.activityName = `${form.productName} 拼团`
}

async function handleSave() {
  if (!form.activityName?.trim()) {
    message.warning('请输入活动名称')
    return
  }
  if (!form.productId) {
    message.warning('请选择拼团商品')
    return
  }
  if (form.minGroupSize && form.maxGroupSize && form.maxGroupSize < form.minGroupSize) {
    message.warning('最大成团人数不得小于成团人数')
    return
  }
  saving.value = true
  try {
    const payload: any = {
      activityName: form.activityName,
      productId: form.productId,
      originalPrice: form.originalPrice,
      groupPrice: form.groupPrice,
      minGroupSize: form.minGroupSize,
      maxGroupSize: form.maxGroupSize,
      timeLimitMinutes: form.timeLimitMinutes,
      totalQuantity: form.totalQuantity,
      startTime: form.startTime || undefined,
      endTime: form.endTime || undefined,
      remark: form.remark,
    }
    if (editingId.value) {
      await groupBuyApi.update(editingId.value, payload)
      message.success('拼团活动已更新')
    } else {
      payload.status = 'DRAFT'
      await groupBuyApi.create(payload)
      message.success('拼团活动已新增')
    }
    formOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleActivityMore(e: any, record: any) {
  if (e.key === 'toggle') {
    const next = record.status === 'ACTIVE' ? 'FINISHED' : 'ACTIVE'
    try {
      await groupBuyApi.updateStatus(record.id, next)
      message.success(next === 'ACTIVE' ? '活动已启用' : '活动已停用')
      fetchList()
    } catch (error: any) {
      message.error(error?.response?.data?.message || '操作失败')
    }
  } else if (e.key === 'orders') {
    activeTab.value = 'order'
    orderSearch.groupId = ''
    orderSearch.customer = ''
    orderSearch.orderNo = ''
    pagination.current = 1
    fetchList()
  }
}

// ═══ 打印 / 导出 ═══
const PRINT_COLUMNS_ACTIVITY = [
  { key: 'activityCode', title: '活动ID' },
  { key: 'activityName', title: '活动名称' },
  { key: 'startTime', title: '起始时间' },
  { key: 'endTime', title: '结束时间' },
  { key: 'groupType', title: '成团类型' },
  { key: 'groupCount', title: '开团个数' },
  { key: 'successGroupCount', title: '成功团个数' },
  { key: 'statusText', title: '活动状态' },
  { key: 'createTime', title: '创建时间' },
]
const PRINT_COLUMNS_ORDER = [
  { key: 'groupId', title: '拼团编号' },
  { key: 'customerName', title: '客户名称' },
  { key: 'productName', title: '商品名称' },
  { key: 'orderNo', title: '订单编号' },
  { key: 'submitTime', title: '提交时间' },
  { key: 'billDate', title: '单据时间' },
  { key: 'productAmount', title: '商品金额' },
  { key: 'orderAmount', title: '订单金额' },
  { key: 'activityName', title: '活动名称' },
  { key: 'activityCode', title: '活动ID' },
  { key: 'groupStatus', title: '拼团状态' },
]

function currentPrint() {
  if (activeTab.value === 'activity') {
    return {
      title: '拼团活动',
      cols: PRINT_COLUMNS_ACTIVITY,
      rows: (activityData.value || []).filter((r: any) => !r.__ghost),
      cell: (r: any, key: string) => (key === 'statusText' ? (GROUP_STATUS_MAP[r.status]?.text || '')
        : (['startTime', 'endTime', 'createTime'].includes(key) ? fmtTime(r[key]) : (r[key] ?? ''))),
    }
  }
  return {
    title: '拼团订单',
    cols: PRINT_COLUMNS_ORDER,
    rows: (orderData.value || []).filter((r: any) => !r.__ghost),
    cell: (r: any, key: string) => (['submitTime', 'billDate'].includes(key) ? fmtTime(r[key])
      : (['productAmount', 'orderAmount'].includes(key) ? fmtMoney(r[key]) : (r[key] ?? ''))),
  }
}

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML + window.open()，现在交给 PrintDialog：列与行由页面给，模板负责版式。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'marketing-mall-group',
  // 列/行/标题随 Tab（拼团活动 / 拼团订单）变，静态生成器写不进模板 → 明确按数据列打
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
    a.download = `商城拼团_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } finally {
    exporting.value = false
  }
}

function handleError(error: Error) {
  console.error('[商城拼团] 页面错误', error)
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
