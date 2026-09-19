<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        采购准备（分析 → 采销分析 → 采购分析 → 采购准备，菜单 80422）
        对标 ql361：**四策略补货工作台**（4 个页内 Tab，逐 Tab 独立列配置 + 独立查询区）。
          Tab1 库存预警补货 24/13 ｜ Tab2 缺货补货 16/12 ｜ Tab3 智能补货 25/20 ｜ Tab4 以销定购 42/24（含「发货」多级表头 + 固定「操作」列）。
        取数（三个策略端点均已封装，本页接线；以销定购无封装 → request 直调采购模块端点）：
          /erp/stock/alert-replenish/page、/erp/stock/shortage-replenish/page、/erp/stock/smart-replenish/page、
          /erp/purchase/sales-driven/page（Tab4）。
        「页面配置」弹窗：对标实测**仅** 缺货补货 / 以销定购 两个 Tab 有弹窗实据（shots 下有对应 PNG），
          其余两 Tab pageConfig.found=false → 不接（查询区为固定项）。
        ⚠️ 未落地项（后端能力所限，见汇报）：
          ① 左侧商品分类树（对标 库存预警补货 Tab 有）——本页四 Tab 查询区形态不同，未接分类树；
          ② Tab4 的「查询方案」下拉、Tab2 的「客户/经手人」筛选（后端参数为 ID，无对应选择器）；
          ③ 底部合计行（三个策略端点均无 summary 字段，不拿当前页求和冒充）。
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="onTabChange"
      >
        <!-- ═══ 工具栏左侧：Tab4 的时间快捷段 ═══ -->
        <template #toolbar-left>
          <a-space v-if="activeTab === 'salesDriven'" :size="4" class="quick-dates">
            <a-button
              v-for="d in QUICK_DATES"
              :key="d.key"
              :type="drivenQuickDate === d.key ? 'primary' : 'link'"
              size="small"
              @click="setDrivenQuickDate(d.key)"
            >
              {{ d.label }}
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：刷新｜打印(F8)｜导出｜页面配置（仅两个 Tab 有页面配置实据） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button v-if="isButtonEnabled('refresh')" size="small" :loading="loading" @click="handleRefresh">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button v-if="isButtonEnabled('printF8')" size="small" @click="handlePrint">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button v-if="isButtonEnabled('export')" size="small" :loading="exporting" @click="handleExport">
              <DownloadOutlined /> 导出
            </a-button>
            <a-button v-if="hasPageConfig" size="small" @click="showPageConfig = true">
              <SettingOutlined /> 页面配置
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（逐 Tab 各一套，横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <!-- ── Tab1 库存预警补货（对标：仓库单选 + 商品/品牌/所属供应商/备注 + 只显示下限预警） ── -->
              <template v-if="activeTab === 'warn'">
                <div class="search-item">
                  <span class="search-label">仓库</span>
                  <a-radio-group v-model:value="q.warehouseMode" size="small" button-style="solid">
                    <a-radio-button value="all">全部仓库</a-radio-button>
                    <a-radio-button value="one">指定仓库</a-radio-button>
                  </a-radio-group>
                  <a-select
                    v-if="q.warehouseMode === 'one'"
                    v-model:value="q.warehouseId"
                    size="small"
                    placeholder="选择仓库"
                    allow-clear
                    style="width: 160px"
                    :options="warehouseOptions"
                    @change="handleSearch"
                  />
                </div>
                <div class="search-item">
                  <span class="search-label">商品</span>
                  <a-input v-model:value="q.keyword" size="small" placeholder="名称/编码/货号" allow-clear style="width: 180px" @press-enter="handleSearch" />
                </div>
                <div class="search-item">
                  <span class="search-label">品牌</span>
                  <a-input v-model:value="q.brand" size="small" placeholder="品牌" allow-clear style="width: 120px" @press-enter="handleSearch" />
                </div>
                <div class="search-item">
                  <span class="search-label">所属供应商</span>
                  <a-input v-model:value="q.supplierName" size="small" placeholder="所属供应商" allow-clear style="width: 150px" @press-enter="handleSearch" />
                </div>
                <div class="search-item">
                  <span class="search-label">备注</span>
                  <a-input v-model:value="q.remark" size="small" placeholder="备注" allow-clear style="width: 140px" @press-enter="handleSearch" />
                </div>
                <div class="search-item">
                  <a-checkbox v-model:checked="q.onlyLowStock">只显示下限预警的商品</a-checkbox>
                </div>
              </template>

              <!-- ── Tab2 缺货补货（对标页面配置 10 项中可生效的 8 项） ── -->
              <template v-if="activeTab === 'shortage'">
                <div v-if="isQueryVisible('shortage.dateRange')" class="search-item">
                  <span class="search-label">日期</span>
                  <a-range-picker v-model:value="q.dateRange" size="small" style="width: 230px" value-format="YYYY-MM-DD" />
                </div>
                <div v-if="isQueryVisible('shortage.product')" class="search-item">
                  <span class="search-label">商品</span>
                  <a-input v-model:value="q.productKeyword" size="small" placeholder="名称/编码/货号" allow-clear style="width: 180px" @press-enter="handleSearch" />
                </div>
                <div v-if="isQueryVisible('shortage.warehouse')" class="search-item">
                  <span class="search-label">仓库</span>
                  <a-select v-model:value="q.warehouseId" size="small" placeholder="全部仓库" allow-clear style="width: 150px" :options="warehouseOptions" />
                </div>
                <div v-if="isQueryVisible('shortage.supplier')" class="search-item">
                  <span class="search-label">供应商</span>
                  <a-input v-model:value="q.supplierName" size="small" placeholder="供应商" allow-clear style="width: 150px" @press-enter="handleSearch" />
                </div>
                <div v-if="isQueryVisible('shortage.orderStatus')" class="search-item">
                  <span class="search-label">单据状态</span>
                  <a-select v-model:value="q.orderStatus" size="small" placeholder="全部" allow-clear style="width: 120px" :options="ORDER_STATUS_OPTIONS" />
                </div>
                <div v-if="isQueryVisible('shortage.orderSource')" class="search-item">
                  <span class="search-label">订单来源</span>
                  <a-select v-model:value="q.orderSource" size="small" placeholder="全部" allow-clear style="width: 120px" :options="ORDER_SOURCE_OPTIONS" />
                </div>
                <div v-if="isQueryVisible('shortage.shortageMode')" class="search-item">
                  <span class="search-label">缺货数量=</span>
                  <a-select v-model:value="q.shortageMode" size="small" style="width: 220px" :options="SHORTAGE_MODE_OPTIONS" />
                </div>
                <div v-if="isQueryVisible('shortage.onlyShortage')" class="search-item">
                  <a-checkbox v-model:checked="q.onlyShortage">仅显示缺货商品</a-checkbox>
                </div>
              </template>

              <!-- ── Tab3 智能补货（对标查询条件未抓取；按后端可用参数落地） ── -->
              <template v-if="activeTab === 'smart'">
                <div class="search-item">
                  <span class="search-label">销售日期</span>
                  <a-range-picker v-model:value="q.dateRange" size="small" style="width: 230px" value-format="YYYY-MM-DD" />
                </div>
                <div class="search-item">
                  <span class="search-label">备货天数</span>
                  <a-input-number v-model:value="q.stockDays" size="small" :min="0" :precision="0" placeholder="天数" style="width: 100px" />
                </div>
                <div class="search-item">
                  <span class="search-label">商品</span>
                  <a-input v-model:value="q.productKeyword" size="small" placeholder="名称/编码/货号/条码" allow-clear style="width: 190px" @press-enter="handleSearch" />
                </div>
                <div class="search-item">
                  <span class="search-label">仓库</span>
                  <a-select v-model:value="q.warehouseId" size="small" placeholder="全部仓库" allow-clear style="width: 150px" :options="warehouseOptions" />
                </div>
                <div class="search-item">
                  <span class="search-label">供应商</span>
                  <a-input v-model:value="q.supplierName" size="small" placeholder="供应商" allow-clear style="width: 150px" @press-enter="handleSearch" />
                </div>
                <div class="search-item">
                  <span class="search-label">计划采购数量≥</span>
                  <a-input-number v-model:value="q.minPlanQty" size="small" :precision="0" placeholder="下限" style="width: 100px" />
                </div>
              </template>

              <!-- ── Tab4 以销定购（对标页面配置：日期/单据编号/销售类型/客户/经手人/仓库/记账状态/单据状态/显示已取消/仅显示已选中） ── -->
              <template v-if="activeTab === 'salesDriven'">
                <div v-if="isQueryVisible('driven.dateRange')" class="search-item">
                  <span class="search-label">单据日期</span>
                  <a-range-picker v-model:value="q.dateRange" size="small" style="width: 230px" value-format="YYYY-MM-DD" />
                </div>
                <div v-if="isQueryVisible('driven.orderNo')" class="search-item">
                  <span class="search-label">单据编号</span>
                  <a-input v-model:value="q.orderNo" size="small" placeholder="单据编号" allow-clear style="width: 170px" @press-enter="handleSearch" />
                </div>
                <div v-if="isQueryVisible('driven.saleType')" class="search-item">
                  <span class="search-label">销售类型</span>
                  <a-select v-model:value="q.saleType" size="small" placeholder="全部" allow-clear style="width: 120px" :options="SALE_TYPE_OPTIONS" />
                </div>
                <div v-if="isQueryVisible('driven.customer')" class="search-item">
                  <span class="search-label">客户</span>
                  <a-input v-model:value="q.customerName" size="small" placeholder="客户" allow-clear style="width: 150px" @press-enter="handleSearch" />
                </div>
                <div v-if="isQueryVisible('driven.handler')" class="search-item">
                  <span class="search-label">经手人</span>
                  <a-input v-model:value="q.salesmanName" size="small" placeholder="经手人" allow-clear style="width: 130px" @press-enter="handleSearch" />
                </div>
                <div v-if="isQueryVisible('driven.warehouse')" class="search-item">
                  <span class="search-label">仓库</span>
                  <a-select v-model:value="q.warehouseName" size="small" placeholder="全部仓库" allow-clear style="width: 150px" :options="warehouseNameOptions" />
                </div>
                <div v-if="isQueryVisible('driven.bookkeepingStatus')" class="search-item">
                  <span class="search-label">记账状态</span>
                  <a-select v-model:value="q.bookkeepingStatus" size="small" placeholder="全部" allow-clear style="width: 120px" :options="BOOKKEEPING_OPTIONS" />
                </div>
                <div v-if="isQueryVisible('driven.status')" class="search-item">
                  <span class="search-label">单据状态</span>
                  <a-select v-model:value="q.status" size="small" placeholder="全部" allow-clear style="width: 130px" :options="SALE_STATUS_OPTIONS" />
                </div>
                <div v-if="isQueryVisible('driven.showCancelled')" class="search-item">
                  <a-checkbox v-model:checked="q.showCancelled">显示已取消</a-checkbox>
                </div>
                <div v-if="isQueryVisible('driven.onlySelected')" class="search-item">
                  <a-checkbox v-model:checked="q.onlySelected">仅显示已选中</a-checkbox>
                </div>
              </template>

              <div class="search-item search-actions">
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格（逐 Tab 独立列定义 + 独立 storage-key） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              :data-source="dataSource"
              :columns="activeColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="rowKey"
              :storage-key="`analytics-purchase-prep-columns-${activeTab}`"
              :global-config-key="`analytics-purchase-prep-columns-${activeTab}`"
            >
              <template #imageCell="{ record }">
                <span v-if="record.image" class="cell-image">{{ record.image }}</span>
                <span v-else class="cell-empty">-</span>
              </template>
              <template #alertTypeCell="{ record }">
                <a-tag :color="alertTypeColor(record.alertType)">{{ record.alertType || '-' }}</a-tag>
              </template>
              <template #saleTypeCell="{ record }">
                {{ SALE_TYPE_MAP[record.saleType] ?? '-' }}
              </template>
              <template #statusCell="{ record }">
                {{ STATUS_MAP[record.status] ?? '-' }}
              </template>
              <template #bookkeepingCell="{ record }">
                {{ record.bookkeepingStatus == null ? '-' : (BOOKKEEPING_MAP[record.bookkeepingStatus] ?? '-') }}
              </template>
              <template #boolCell="{ record, column }">
                {{ record[column.key] ? '是' : '否' }}
              </template>
              <template #amountCell="{ record, column }">
                {{ formatMoney(record[column.key]) }}
              </template>
              <template #attachmentCell="{ record }">
                <span v-if="record.attachment">有</span>
                <span v-else class="cell-empty">-</span>
              </template>
              <template #drivenActionCell="{ record }">
                <a-space :size="2">
                  <a-button type="link" size="small" @click="handleSalesDrivenPurchase('FINISHED', record)">采购成品</a-button>
                  <a-button type="link" size="small" @click="handleSalesDrivenPurchase('MATERIAL', record)">采购原料</a-button>
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

      <!-- ═══ 页面配置（对标实测仅 缺货补货 / 以销定购 两 Tab 有弹窗实据 → 仅这两个 Tab 接） ═══ -->
      <PageConfigPanel
        v-if="hasPageConfig"
        :open="showPageConfig"
        :query-fields-config="activeCfg.queryFields.value"
        :function-buttons-config="activeCfg.functionButtons.value"
        :default-query-fields-config="activeCfg.defaultQueryFields"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="`analytics-purchase-prep-page-config-${activeTab}`"
        :print-config-items="PRINT_ITEMS"
        @update:open="showPageConfig = $event"
        @change="activeCfg.handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import {
  DownloadOutlined, PrinterOutlined, ReloadOutlined, SettingOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import request from '@/utils/request'
import { stockReportApi, stockApi } from '@/api/analytics'
import { useExport } from '@/composables/useExport'
import { QUICK_DATES, quickDateRange } from '../shared/docTypes'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'

defineOptions({ name: 'AnalyticsPurchasePrep' })

// ═══ 视图 Tab（对标实测顺序：库存预警补货 / 缺货补货 / 智能补货 / 以销定购） ═══
const TABS = [
  { key: 'warn', label: '库存预警补货' },
  { key: 'shortage', label: '缺货补货' },
  { key: 'smart', label: '智能补货' },
  { key: 'salesDriven', label: '以销定购' }
]
const activeTab = ref('warn')

const loading = ref(false)
const dataSource = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 仓库下拉（id 口径给前三个 Tab；name 口径给以销定购） ═══
const warehouseOptions = ref<{ label: string; value: number }[]>([])
const warehouseNameOptions = ref<{ label: string; value: string }[]>([])

function today(offset = 0): string {
  const d = new Date(Date.now() + offset * 86400000)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

// ═══ 逐 Tab 独立查询态 ═══
const queries = reactive<Record<string, any>>({
  warn: {
    warehouseMode: 'all',
    warehouseId: undefined as number | undefined,
    keyword: '',
    brand: '',
    supplierName: '',
    remark: '',
    onlyLowStock: false
  },
  shortage: {
    dateRange: [today(-30), today()] as [string, string] | null,
    productKeyword: '',
    warehouseId: undefined as number | undefined,
    supplierName: '',
    orderStatus: undefined as number | undefined,
    orderSource: undefined as number | undefined,
    shortageMode: 1,
    onlyShortage: true
  },
  smart: {
    dateRange: [today(-30), today()] as [string, string] | null,
    stockDays: undefined as number | undefined,
    productKeyword: '',
    warehouseId: undefined as number | undefined,
    supplierName: '',
    minPlanQty: undefined as number | undefined
  },
  salesDriven: {
    dateRange: quickDateRange('month') as [string, string] | null,
    orderNo: '',
    saleType: undefined as number | undefined,
    customerName: '',
    salesmanName: '',
    warehouseName: undefined as string | undefined,
    bookkeepingStatus: undefined as number | undefined,
    status: undefined as number | undefined,
    showCancelled: false,
    onlySelected: false
  }
})
const drivenQuickDate = ref('month')

/** 当前 Tab 的查询态（对象本身是 reactive 的，模板 v-model 直接改其属性） */
const q = computed<any>(() => queries[activeTab.value])

const ORDER_STATUS_OPTIONS = [
  { label: '待发货', value: 2 },
  { label: '部分发货', value: 3 },
  { label: '发货完成', value: 4 },
  { label: '交易完成', value: 5 }
]
const ORDER_SOURCE_OPTIONS = [
  { label: '手工', value: 0 },
  { label: '微商城', value: 1 }
]
const SHORTAGE_MODE_OPTIONS = [
  { label: '待发货 − 账面库存', value: 1 },
  { label: '待发货 − 待收货 − 账面库存', value: 2 }
]
const SALE_TYPE_OPTIONS = [
  { label: '正常销售', value: 1 },
  { label: '样品', value: 2 },
  { label: '促销', value: 3 }
]
const SALE_STATUS_OPTIONS = [
  { label: '草稿', value: 0 },
  { label: '待审核', value: 1 },
  { label: '待发货', value: 2 },
  { label: '部分发货', value: 3 },
  { label: '发货完成', value: 4 },
  { label: '交易完成', value: 5 },
  { label: '已取消', value: 6 }
]
const BOOKKEEPING_OPTIONS = [
  { label: '未完成', value: 0 },
  { label: '已完成', value: 1 }
]
const STATUS_MAP: Record<number, string> = {
  0: '草稿', 1: '待审核', 2: '待发货', 3: '部分发货', 4: '发货完成', 5: '交易完成', 6: '已取消'
}
const SALE_TYPE_MAP: Record<number, string> = { 1: '正常销售', 2: '样品', 3: '促销' }
const BOOKKEEPING_MAP: Record<number, string> = { 0: '未完成', 1: '已完成' }

// ═══ 列定义（逐 Tab 独立；列名逐字取自《采购准备开发文档》§3，顺序一致） ═══

/** Tab1 库存预警补货：24 列 / 默认 13（defaultHidden 11） */
const alertColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'warehouseCode', title: '仓库编号', width: 110 },
  { key: 'warehouseName', title: '仓库名称', width: 130 },
  { key: 'productName', title: '商品名称', width: 190 },
  { key: 'productCode', title: '货号', width: 120 },
  { key: 'brand', title: '品牌', width: 100 },
  { key: 'weight', title: '重量（kg）', width: 100, align: 'right', defaultHidden: true, formatter: v => formatQty(v) },
  { key: 'volume', title: '体积（m³）', width: 100, align: 'right', defaultHidden: true, formatter: v => formatQty(v) },
  { key: 'taste', title: '口味', width: 100 },
  { key: 'model', title: '型号', width: 100 },
  { key: 'alertType', title: '预警类型', type: 'slot', slotName: 'alertTypeCell', width: 110 },
  { key: 'barcode', title: '条码', width: 130, defaultHidden: true },
  { key: 'specification', title: '规格', width: 110, defaultHidden: true },
  { key: 'origin', title: '产地', width: 100, defaultHidden: true },
  { key: 'unit', title: '单位', width: 70 },
  { key: 'shortageQty', title: '缺货数量', width: 100, align: 'right', formatter: v => formatQty(v) },
  { key: 'maxStock', title: '库存上限', width: 100, align: 'right', formatter: v => formatQty(v) },
  { key: 'minStock', title: '库存下限', width: 100, align: 'right', formatter: v => formatQty(v) },
  { key: 'remark', title: '备注', width: 140 },
  { key: 'pendingQty', title: '待发货数量', width: 110, align: 'right', defaultHidden: true, formatter: v => formatQty(v) },
  { key: 'bookQty', title: '账面库存', width: 100, align: 'right', defaultHidden: true, formatter: v => formatQty(v) },
  { key: 'inTransitQty', title: '待收货数量', width: 110, align: 'right', defaultHidden: true, formatter: v => formatQty(v) },
  { key: 'lastPurchaseDate', title: '最近采购日期', width: 130, defaultHidden: true },
  { key: 'lastSupplierName', title: '最近采购供货商', width: 160, defaultHidden: true },
  { key: 'lastPurchasePrice', title: '最近采购价', width: 110, align: 'right', defaultHidden: true, formatter: v => formatMoney(v) }
]

/** Tab2 缺货补货：16 列 / 默认 12（defaultHidden 4：规格/型号/产地/品牌） */
const shortageColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'image', title: '图片', type: 'slot', slotName: 'imageCell', width: 70, align: 'center' },
  { key: 'productName', title: '商品名称', width: 190 },
  { key: 'productCode', title: '商品货号', width: 120 },
  { key: 'specification', title: '规格', width: 110, defaultHidden: true },
  { key: 'model', title: '型号', width: 100, defaultHidden: true },
  { key: 'origin', title: '产地', width: 100, defaultHidden: true },
  { key: 'brand', title: '品牌', width: 100, defaultHidden: true },
  { key: 'unit', title: '单位', width: 70 },
  { key: 'orderQty', title: '订单数量', width: 100, align: 'right', formatter: v => formatQty(v) },
  { key: 'amountWithTax', title: '价税合计', width: 110, align: 'right', formatter: v => formatMoney(v) },
  { key: 'shippedQty', title: '已发货数量', width: 110, align: 'right', formatter: v => formatQty(v) },
  { key: 'unshippedQty', title: '待发货数量', width: 110, align: 'right', formatter: v => formatQty(v) },
  { key: 'inTransitQty', title: '待收货数量', width: 110, align: 'right', formatter: v => formatQty(v) },
  { key: 'bookQty', title: '账面库存', width: 100, align: 'right', formatter: v => formatQty(v) },
  { key: 'shortageQty', title: '缺货数量', width: 100, align: 'right', formatter: v => formatQty(v) },
  { key: 'remark', title: '备注', width: 140 }
]

/** Tab3 智能补货：25 列 / 默认 20（defaultHidden 5：品牌/两个换算结果/最近销售日期/最近进货日期） */
const smartColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'image', title: '图片', type: 'slot', slotName: 'imageCell', width: 70, align: 'center' },
  { key: 'productName', title: '商品名称', width: 190 },
  { key: 'productCode', title: '货号', width: 120 },
  { key: 'unit', title: '单位', width: 70 },
  { key: 'barcode', title: '条码', width: 130 },
  { key: 'specification', title: '规格', width: 110 },
  { key: 'model', title: '型号', width: 100 },
  { key: 'origin', title: '产地', width: 100 },
  { key: 'brand', title: '品牌', width: 100, defaultHidden: true },
  { key: 'remark', title: '备注', width: 140 },
  { key: 'salesQty', title: '销售数量', width: 100, align: 'right', formatter: v => formatQty(v) },
  { key: 'salesAmount', title: '销售金额', width: 110, align: 'right', formatter: v => formatMoney(v) },
  { key: 'purchaseQty', title: '采购数量', width: 100, align: 'right', formatter: v => formatQty(v) },
  { key: 'purchaseAmount', title: '采购金额', width: 110, align: 'right', formatter: v => formatMoney(v) },
  { key: 'avgDailySales', title: '日均销量', width: 100, align: 'right', formatter: v => formatQty(v) },
  { key: 'stockDays', title: '备货天数', width: 90, align: 'right', formatter: v => formatQty(v) },
  { key: 'inTransitQty', title: '待收货数量', width: 110, align: 'right', formatter: v => formatQty(v) },
  { key: 'pendingShipQty', title: '待发货数量', width: 110, align: 'right', formatter: v => formatQty(v) },
  { key: 'bookQty', title: '账面库存', width: 100, align: 'right', formatter: v => formatQty(v) },
  { key: 'bookQtyConverted', title: '账面库存换算结果', width: 130, align: 'right', defaultHidden: true, formatter: v => formatQty(v) },
  { key: 'planPurchaseQty', title: '计划采购数量', width: 120, align: 'right', formatter: v => formatQty(v) },
  { key: 'availableQty', title: '可用库存', width: 100, align: 'right', formatter: v => formatQty(v) },
  { key: 'availableQtyConverted', title: '可用库存换算结果', width: 130, align: 'right', defaultHidden: true, formatter: v => formatQty(v) },
  { key: 'lastSaleDate', title: '最近销售日期', width: 130, defaultHidden: true },
  { key: 'lastPurchaseDate', title: '最近进货日期', width: 130, defaultHidden: true }
]

/** Tab4 以销定购：42 列 / 默认 24（defaultHidden 18）；「发货」为多级表头（1 组 3 叶子）；固定「操作」列不进列配置 */
const drivenColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'drivenActionCell', width: 150, fixed: 'left' },
  { key: 'orderDate', title: '单据日期', width: 110 },
  { key: 'orderNo', title: '单据编号', width: 170 },
  { key: 'saleType', title: '单据子类型', type: 'slot', slotName: 'saleTypeCell', width: 100 },
  { key: 'generationMethod', title: '录入方式', width: 100 },
  { key: 'status', title: '单据状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'useCoupon', title: '是否使用优惠券', type: 'slot', slotName: 'boolCell', width: 120, align: 'center' },
  { key: 'warehouseName', title: '仓库', width: 120 },
  { key: 'customerCode', title: '客户编号', width: 130 },
  { key: 'customerName', title: '客户', width: 150 },
  { key: 'bookkeepingStatus', title: '记账状态', type: 'slot', slotName: 'bookkeepingCell', width: 100 },
  { key: 'forcedTerminated', title: '强制终止', type: 'slot', slotName: 'boolCell', width: 90, align: 'center' },
  { key: 'hasPurchased', title: '是否采购', type: 'slot', slotName: 'boolCell', width: 90, align: 'center' },
  {
    key: 'shipGroup',
    title: '发货',
    children: [
      { key: 'totalQuantity', title: '商品数量', width: 100, align: 'right', formatter: v => formatQty(v) },
      { key: 'shippedQuantity', title: '已发数量', width: 100, align: 'right', formatter: v => formatQty(v) },
      { key: 'unshippedQuantity', title: '未发数量', width: 100, align: 'right', formatter: v => formatQty(v) }
    ]
  },
  { key: 'totalWeight', title: '重量（kg）', width: 100, align: 'right', formatter: v => formatQty(v) },
  { key: 'totalVolume', title: '体积（m³）', width: 100, align: 'right', formatter: v => formatQty(v) },
  { key: 'billAmount', title: '订单金额', width: 110, align: 'right', type: 'slot', slotName: 'amountCell' },
  { key: 'depositAccount1', title: '订金账户1', width: 110, defaultHidden: true },
  { key: 'depositAmount1', title: '订金金额1', width: 100, align: 'right', defaultHidden: true },
  { key: 'depositAccount2', title: '订金账户2', width: 110, defaultHidden: true },
  { key: 'depositAmount2', title: '订金金额2', width: 100, align: 'right', defaultHidden: true },
  { key: 'logisticsCompany', title: '物流公司', width: 120, defaultHidden: true },
  { key: 'waybillNo', title: '运单号', width: 140, defaultHidden: true },
  { key: 'shippingFee', title: '运费', width: 90, align: 'right', defaultHidden: true },
  { key: 'couponAmount', title: '优惠劵', width: 90, align: 'right', defaultHidden: true },
  { key: 'discountAmount', title: '优惠金额', width: 100, align: 'right', defaultHidden: true },
  { key: 'receiverName', title: '收货人', width: 90, defaultHidden: true },
  { key: 'receiverPhone', title: '联系电话', width: 120, defaultHidden: true },
  { key: 'shippingAddress', title: '收货地址', width: 160, defaultHidden: true },
  { key: 'expectedShipTime', title: '预计发货', width: 130 },
  { key: 'salesmanName', title: '经手人', width: 90 },
  { key: 'deptName', title: '部门', width: 100 },
  { key: 'creatorName', title: '制单人', width: 90, defaultHidden: true },
  { key: 'submitterName', title: '提交人', width: 90, defaultHidden: true },
  { key: 'auditorName', title: '审核人', width: 90, defaultHidden: true },
  { key: 'orderRemark', title: '卖家备注', width: 130 },
  { key: 'buyerRemark', title: '买家备注', width: 130 },
  { key: 'attachment', title: '附件', type: 'slot', slotName: 'attachmentCell', width: 80, align: 'center' },
  { key: 'printCount', title: '打印次数', width: 90, align: 'right', defaultHidden: true },
  { key: 'submitTime', title: '提交时间', width: 160, defaultHidden: true },
  { key: 'bookkeepingTime', title: '制单时间', width: 160, defaultHidden: true }
]

const activeColumns = computed<DetailColumnConfig[]>(() => {
  if (activeTab.value === 'shortage') return shortageColumns
  if (activeTab.value === 'smart') return smartColumns
  if (activeTab.value === 'salesDriven') return drivenColumns
  return alertColumns
})

/** 叶子列（分组表头展开后的数据列），供打印/导出使用 */
const leafColumns = computed<DetailColumnConfig[]>(() =>
  activeColumns.value.flatMap(c => (c.children?.length ? c.children : [c]))
)

// ═══ 取数（逐 Tab 分发；分页参数口径不同：前三个 Tab = pageNum/pageSize，以销定购 = current/size） ═══
function clean(params: Record<string, any>): Record<string, any> {
  const out: Record<string, any> = {}
  Object.keys(params).forEach(k => {
    const v = params[k]
    if (v !== undefined && v !== null && v !== '') out[k] = v
  })
  return out
}

function normalizeRow(r: any, index: number): any {
  const base = { ...r, rowKey: String(r.id ?? `${r.productId}-${r.warehouseId ?? 'x'}-${index}`) }
  if (activeTab.value === 'salesDriven') {
    base.statusText = STATUS_MAP[r.status] ?? '-'
    base.saleTypeText = SALE_TYPE_MAP[r.saleType] ?? '-'
    base.bookkeepingStatusText = r.bookkeepingStatus == null ? '-' : (BOOKKEEPING_MAP[r.bookkeepingStatus] ?? '-')
  }
  return base
}

async function fetchTab(type: string, page: number, size: number): Promise<{ records: any[]; total: number }> {
  const c = queries[type]
  if (type === 'warn') {
    const res: any = await stockReportApi.alertReplenishPage(clean({
      pageNum: page,
      pageSize: size,
      warehouseId: c.warehouseMode === 'one' ? c.warehouseId : undefined,
      keyword: c.keyword,
      brand: c.brand,
      supplierName: c.supplierName,
      remark: c.remark,
      onlyLowStock: c.onlyLowStock ? true : undefined
    }))
    return { records: res?.records || [], total: Number(res?.total) || 0 }
  }
  if (type === 'shortage') {
    const res: any = await stockReportApi.shortageReplenishPage(clean({
      pageNum: page,
      pageSize: size,
      startDate: c.dateRange?.[0],
      endDate: c.dateRange?.[1],
      productKeyword: c.productKeyword,
      warehouseId: c.warehouseId,
      supplierName: c.supplierName,
      orderStatus: c.orderStatus,
      orderSource: c.orderSource,
      shortageMode: c.shortageMode,
      onlyShortage: c.onlyShortage ? true : undefined
    }))
    return { records: res?.records || [], total: Number(res?.total) || 0 }
  }
  if (type === 'smart') {
    const res: any = await stockReportApi.smartReplenishPage(clean({
      pageNum: page,
      pageSize: size,
      startDate: c.dateRange?.[0],
      endDate: c.dateRange?.[1],
      stockDays: c.stockDays,
      productKeyword: c.productKeyword,
      warehouseId: c.warehouseId,
      supplierName: c.supplierName,
      minPlanQty: c.minPlanQty
    }))
    return { records: res?.records || [], total: Number(res?.total) || 0 }
  }
  // 以销定购：api/analytics.ts 无封装 → request 直调采购模块端点（相对路径，分页口径 current/size）
  const res: any = await request.get('/erp/purchase/sales-driven/page', clean({
    current: page,
    size,
    dateStart: c.dateRange?.[0],
    dateEnd: c.dateRange?.[1],
    orderNo: c.orderNo,
    saleType: c.saleType,
    customerName: c.customerName,
    salesmanName: c.salesmanName,
    warehouseName: c.warehouseName,
    bookkeepingStatus: c.bookkeepingStatus,
    status: c.status,
    showCancelled: c.showCancelled ? true : undefined,
    onlySelected: c.onlySelected ? true : undefined
  }))
  return { records: res?.records || [], total: Number(res?.total) || 0 }
}

async function fetchData() {
  const type = activeTab.value
  loading.value = true
  try {
    const { records, total } = await fetchTab(type, pagination.current, pagination.pageSize)
    dataSource.value = records.map(normalizeRow)
    pagination.total = total
  } catch (e) {
    console.warn('[采购准备] 取数失败', e)
    message.error('获取数据失败')
    dataSource.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  return fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  return fetchData()
}

function handleRefresh() {
  return fetchData()
}

function onTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
  fetchData()
}

function handleReset() {
  const type = activeTab.value
  if (type === 'alert') {
    Object.assign(queries.warn, { warehouseMode: 'all', warehouseId: undefined, keyword: '', brand: '', supplierName: '', remark: '', onlyLowStock: false })
  } else if (type === 'shortage') {
    Object.assign(queries.shortage, {
      dateRange: [today(-30), today()], productKeyword: '', warehouseId: undefined,
      supplierName: '', orderStatus: undefined, orderSource: undefined, shortageMode: 1, onlyShortage: true
    })
  } else if (type === 'smart') {
    Object.assign(queries.smart, {
      dateRange: [today(-30), today()], stockDays: undefined, productKeyword: '',
      warehouseId: undefined, supplierName: '', minPlanQty: undefined
    })
  } else {
    drivenQuickDate.value = 'month'
    Object.assign(queries.salesDriven, {
      dateRange: quickDateRange('month'), orderNo: '', saleType: undefined, customerName: '',
      salesmanName: '', warehouseName: undefined, bookkeepingStatus: undefined, status: undefined,
      showCancelled: false, onlySelected: false
    })
  }
  return handleSearch()
}

function setDrivenQuickDate(key: string) {
  drivenQuickDate.value = key
  queries.salesDriven.dateRange = quickDateRange(key)
  handleSearch()
}

// ═══ 以销定购：行级「采购成品 / 采购原料」（真实端点，自动按供应商生成采购订单并提交） ═══
function handleSalesDrivenPurchase(mode: 'FINISHED' | 'MATERIAL', record: any) {
  Modal.confirm({
    title: mode === 'FINISHED' ? '采购成品' : '采购原料',
    content: `将按「${record.orderNo || record.id}」的商品供应商自动生成采购订单并提交，是否确认？`,
    okText: '确认生成',
    cancelText: '取消',
    onOk: async () => {
      const url = mode === 'FINISHED'
        ? `/erp/purchase/sales-driven/${record.id}/purchase-finished`
        : `/erp/purchase/sales-driven/${record.id}/purchase-material`
      const res: any = await request.post(url)
      const orderNos: string[] = res?.purchaseOrderNos || []
      message.success(`已生成采购订单 ${orderNos.length} 张${orderNos.length ? '：' + orderNos.join(',') : ''}`)
      await fetchData()
    }
  })
}

// ═══ 页面配置（仅 缺货补货 / 以销定购 两 Tab 有对标实测弹窗） ═══
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]
const PRINT_ITEMS = [
  { key: 'showCompany', label: '打印抬头显示公司名' },
  { key: 'showSummary', label: '打印底部显示合计行' }
]

/** 缺货补货 Tab：对标弹窗 10 项中本页可生效的 8 项（客户/经手人后端为 ID 参数，无选择器） */
const SHORTAGE_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'shortage.dateRange', label: '日期', visible: true },
  { key: 'shortage.product', label: '商品', visible: true },
  { key: 'shortage.warehouse', label: '仓库', visible: true },
  { key: 'shortage.supplier', label: '供应商', visible: true },
  { key: 'shortage.orderStatus', label: '单据状态', visible: true },
  { key: 'shortage.orderSource', label: '订单来源', visible: true },
  { key: 'shortage.shortageMode', label: '缺货数量=', visible: true },
  { key: 'shortage.onlyShortage', label: '仅显示缺货商品', visible: true }
]

/** 以销定购 Tab：对标查询区可生效项（查询方案 无存储能力，未落地） */
const DRIVEN_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'driven.dateRange', label: '单据日期', visible: true },
  { key: 'driven.orderNo', label: '单据编号', visible: true },
  { key: 'driven.saleType', label: '销售类型', visible: true },
  { key: 'driven.customer', label: '客户', visible: true },
  { key: 'driven.handler', label: '经手人', visible: true },
  { key: 'driven.warehouse', label: '仓库', visible: true },
  { key: 'driven.bookkeepingStatus', label: '记账状态', visible: true },
  { key: 'driven.status', label: '单据状态', visible: true },
  { key: 'driven.showCancelled', label: '显示已取消', visible: true },
  { key: 'driven.onlySelected', label: '仅显示已选中', visible: true }
]

type Cfg = ReturnType<typeof useAnalyticsPageConfig> & { defaultQueryFields: QueryFieldSetting[] }

function makeCfg(storageKey: string, defaultQueryFields: QueryFieldSetting[]): Cfg {
  const cfg = useAnalyticsPageConfig({
    storageKey,
    defaultQueryFields,
    defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
  })
  return Object.assign(cfg, { defaultQueryFields })
}

const cfgByTab: Record<string, Cfg> = {
  shortage: makeCfg('analytics-purchase-prep-page-config-shortage', SHORTAGE_QUERY_FIELDS),
  salesDriven: makeCfg('analytics-purchase-prep-page-config-salesDriven', DRIVEN_QUERY_FIELDS)
}

/** 仅这两个 Tab 有对标「页面配置」弹窗实据 */
const hasPageConfig = computed(() => activeTab.value === 'shortage' || activeTab.value === 'salesDriven')
const activeCfg = computed<Cfg>(() => cfgByTab[activeTab.value] ?? cfgByTab.shortage)

const showPageConfig = ref(false)

function isQueryVisible(key: string): boolean {
  return activeCfg.value.isQueryVisible(key)
}

function isButtonEnabled(key: string): boolean {
  return activeCfg.value.isButtonEnabled(key)
}

// ═══ 格式化 ═══
function formatQty(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { maximumFractionDigits: 3 })
}

function formatMoney(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function alertTypeColor(type: string): string {
  if (type === '缺货') return 'red'
  if (type === '下限预警') return 'orange'
  if (type === '超储') return 'blue'
  return 'default'
}

function cellText(col: DetailColumnConfig, r: any): string {
  const v = r[col.key]
  if (col.type === 'slot' && (col.slotName === 'amountCell' || col.slotName === 'drivenActionCell')) {
    return col.slotName === 'amountCell' ? formatMoney(v) : ''
  }
  if (col.slotName === 'boolCell') return v ? '是' : '否'
  if (col.slotName === 'statusCell') return r.statusText ?? '-'
  if (col.slotName === 'saleTypeCell') return r.saleTypeText ?? '-'
  if (col.slotName === 'bookkeepingCell') return r.bookkeepingStatusText ?? '-'
  if (col.slotName === 'alertTypeCell') return r.alertType ?? '-'
  if (col.slotName === 'attachmentCell') return v ? '有' : ''
  if (col.slotName === 'imageCell') return ''
  if (/价|金额|运费|优惠/.test(col.title)) return formatMoney(v)
  if (/数量|重量|体积|天数|库存|销量/.test(col.title)) return formatQty(v)
  return v === null || v === undefined || v === '' ? '' : String(v)
}

/** 打印 / 导出列：剔除序号列与操作列 */
const printableColumns = computed<DetailColumnConfig[]>(() =>
  leafColumns.value.filter(c => c.key !== 'rowNo' && c.key !== 'action')
)

// ═══ 打印(F8) ═══
function handlePrint() {
  const cols = printableColumns.value
  const header = cols.map(c => c.title)
  const body = dataSource.value.map(r => cols.map(c => cellText(c, r)))
  const win = window.open('', '_blank', 'width=1500,height=800')
  if (!win) {
    message.warning('浏览器拦截了打印窗口，请允许弹窗后重试')
    return
  }
  const tabLabel = TABS.find(t => t.key === activeTab.value)?.label || '采购准备'
  const html = `<html><head><meta charset="utf-8"><title>采购准备-${tabLabel}</title>
    <style>body{font-family:system-ui,sans-serif;font-size:12px;padding:12px}
    h3{margin:0 0 8px}table{border-collapse:collapse;width:100%}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left;white-space:nowrap}</style></head><body>
    <h3>采购准备 · ${tabLabel}</h3>
    <table><thead><tr>${header.map(h => `<th>${h}</th>`).join('')}</tr></thead>
    <tbody>${body.map(row => `<tr>${row.map(v => `<td>${v}</td>`).join('')}</tr>`).join('')}</tbody>
    </table></body></html>`
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

/** F8 快捷键（对标工具栏「打印(F8)」） */
function handleF8Key(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

// ═══ 导出（逐页取全量 → CSV） ═══
const { execute: executeExport, exporting } = useExport()

async function fetchAllRows(): Promise<any[]> {
  const type = activeTab.value
  const size = 100
  const all: any[] = []
  const pages = Math.max(1, Math.ceil(pagination.total / size))
  for (let p = 1; p <= pages; p++) {
    const { records } = await fetchTab(type, p, size)
    all.push(...records.map(normalizeRow))
    if (records.length < size) break
  }
  return all
}

function handleExport() {
  const cols = printableColumns.value
  executeExport({
    fileName: `采购准备-${TABS.find(t => t.key === activeTab.value)?.label || ''}`,
    headers: cols.map(c => c.title),
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: (list: any[]) => list.map(r => cols.map(c => cellText(c, r))),
    fallbackRows: () => dataSource.value.map(r => cols.map(c => cellText(c, r)))
  })
}

function handleError(err: any) {
  console.error('[采购准备] 页面异常', err)
}

onMounted(async () => {
  try {
    const list: any = await stockApi.getWarehouses()
    const arr = Array.isArray(list) ? list : []
    warehouseOptions.value = arr.map((w: any) => ({ label: w.warehouseName, value: w.id }))
    warehouseNameOptions.value = arr.map((w: any) => ({ label: w.warehouseName, value: w.warehouseName }))
  } catch (e) {
    console.warn('[采购准备] 仓库列表获取失败', e)
  }
  fetchData()
  window.addEventListener('keydown', handleF8Key)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleF8Key)
})
</script>

<style scoped>
/* 插槽内容不受宿主 scoped 样式影响，查询区样式随页面自带 */
.search-area { width: 100%; }
.search-grid { display: flex; flex-wrap: wrap; gap: 8px 16px; align-items: center; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { color: #666; font-size: 13px; white-space: nowrap; }
.search-actions { margin-left: auto; }
.quick-dates { margin-left: 8px; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-image { color: #1677ff; }
.cell-empty { color: #bfbfbf; }
</style>
