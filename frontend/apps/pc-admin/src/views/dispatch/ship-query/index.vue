<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        发货查询（配发收 → 发货业务 → 发货查询，菜单 70156；对标 ql361 menuId 3752 / billType 2357）
        · 数据：销售出库单 XSCKD 台账（来源订单 XSDD 回链、结算状态/已结金额对账口径）
        · 金标准骨架：CategoryListLayout + BillTableList（表头齿轮列配置：个人/全局）+ PageConfigPanel + 经典分页
        · 列：对标实测 49 列（默认 14 可见 + 操作固定）
        · 查询：页面配置 25 项 + 页面固定项「配送状态 / 配送线路」（不在配置清单内）
        · 功能按钮：批量打印、商品汇总 ｜ 配置(齿轮)、刷新、打印(F8)、导出
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：批量打印 + 商品汇总 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('batchPrint')"
              size="small"
              :disabled="selectedRowKeys.length === 0"
              @click="openBatchPrintModal"
            >
              <PrinterOutlined /> 批量打印<template v-if="selectedRowKeys.length">({{ selectedRowKeys.length }})</template>
            </a-button>
            <a-button
              v-if="isButtonEnabled('productSummary')"
              size="small"
              @click="handleProductSummary"
            >
              <BarChartOutlined /> 商品汇总
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：配置 + 刷新 + 打印(F8) + 导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              title="页面配置"
              @click="showPageConfig = true"
            >
              <SettingOutlined />
            </a-button>
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="handleSearch"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="isButtonEnabled('printF8')"
              size="small"
              @click="handlePrintF8"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="isButtonEnabled('export')"
              size="small"
              :loading="exporting"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区：固定项（配送状态/配送线路）+ 配置驱动 25 项 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <!-- 对标实测：配送状态、配送线路为页面固定项，不参与显隐配置 -->
              <div class="search-item">
                <span class="search-label">配送状态</span>
                <a-select
                  v-model:value="searchForm.deliveryStatus"
                  size="small"
                  style="width: 110px"
                  allow-clear
                  placeholder="全部"
                  :options="DELIVERY_STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">配送线路</span>
                <a-select
                  v-model:value="searchForm.routeId"
                  size="small"
                  style="width: 190px"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  placeholder="全部线路"
                  :options="routeOptionList"
                  :loading="routeLoading"
                  @change="handleSearch"
                />
              </div>

              <!-- 页面配置驱动的 25 项查询条件 -->
              <template
                v-for="field in renderQueryFields"
                :key="field.key"
              >
                <div class="search-item">
                  <span class="search-label">{{ field.label }}</span>
                  <a-range-picker
                    v-if="field.key === 'date'"
                    v-model:value="searchForm.dateRange"
                    size="small"
                    style="width: 230px"
                    value-format="YYYY-MM-DD"
                    @change="handleSearch"
                  />
                  <a-select
                    v-else-if="field.type === 'select'"
                    v-model:value="searchForm[field.key]"
                    size="small"
                    style="width: 130px"
                    allow-clear
                    :placeholder="field.placeholder || '全部'"
                    :options="field.options || []"
                    @change="handleSearch"
                  />
                  <a-input-number
                    v-else-if="field.type === 'number'"
                    v-model:value="searchForm[field.key]"
                    size="small"
                    style="width: 130px"
                    :placeholder="field.placeholder || '请输入'"
                  />
                  <a-checkbox
                    v-else-if="field.type === 'checkbox'"
                    v-model:checked="searchForm[field.key]"
                    @change="handleSearch"
                  />
                  <a-input
                    v-else
                    v-model:value="searchForm[field.key]"
                    size="small"
                    style="width: 150px"
                    allow-clear
                    :placeholder="field.placeholder || '请输入'"
                    @press-enter="handleSearch"
                  />
                </div>
              </template>

              <a-button
                type="primary"
                size="small"
                @click="handleSearch"
              >
                查询
              </a-button>
              <a-button
                size="small"
                @click="handleReset"
              >
                重置
              </a-button>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表（列配置齿轮在表头 rowNo 列） ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="columns"
              :data-source="tableData"
              :loading="loading"
              :pagination="false"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              storage-key="dispatch-ship-query-columns"
              global-config-key="dispatch-ship-query-columns"
              row-key="id"
              @selection-change="handleSelectionChange"
            >
              <template #outboundNoCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="handleOpenDetail(record)"
                >{{ record.outboundNo }}</a>
              </template>

              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="STATUS_MAP[record.status]?.color || 'default'"
                >
                  {{ STATUS_MAP[record.status]?.label || record.status }}
                </a-tag>
              </template>

              <template #settlementStatusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost && record.settlementStatus"
                  :color="SETTLEMENT_STATUS_MAP[record.settlementStatus]?.color || 'default'"
                >
                  {{ SETTLEMENT_STATUS_MAP[record.settlementStatus]?.label || record.settlementStatus }}
                </a-tag>
                <span v-else-if="!record.__ghost">-</span>
              </template>

              <!-- 本单金额 = 商品金额 − 促销优惠 − 优惠劵 − 直接优惠 + 运费 + 其他费用（与后端导出同口径） -->
              <template #finalAmountCell="{ record }">
                <span v-if="record.__ghost">-</span>
                <span v-else>{{ formatMoney(billAmount(record)) }}</span>
              </template>

              <template #attachmentCell="{ record }">
                <span v-if="!record.__ghost">{{ record.attachment ? '有' : '-' }}</span>
              </template>

              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="handleOpenDetail(record)"
                  >
                    查看
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="openPrint(record)"
                  >
                    打印
                  </a-button>
                </a-space>
              </template>
            </BillTableList>
          </div>
        </template>

        <!-- ═══ 底部：经典分页栏 ═══ -->
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

      <!-- ═══ 打印（模板渲染；成功后回写出库单打印次数） ═══ -->
      <PrintDialog
        ref="printDialogRef"
        page-code="ship-query"
        :print-data="printData"
        :always-last-template="printConfig.alwaysLastTemplate"
        @print-success="handlePrintSuccess"
      />

      <!-- ═══ 批量打印（对标：可选「仅打印未打印过的单据」） ═══ -->
      <a-modal
        v-model:open="showBatchPrint"
        title="批量打印"
        :width="420"
        ok-text="开始打印"
        @ok="confirmBatchPrint"
      >
        <p>已选择 <b>{{ selectedRowKeys.length }}</b> 张销售出库单</p>
        <a-checkbox v-model:checked="onlyUnprinted">
          仅打印未打印过的单据
        </a-checkbox>
      </a-modal>

      <!-- ═══ 商品汇总（按当前查询条件聚合明细行） ═══ -->
      <a-modal
        v-model:open="showProductSummary"
        title="商品汇总"
        :width="780"
        :footer="null"
      >
        <a-spin :spinning="summaryLoading">
          <a-table
            :data-source="summaryRows"
            :columns="summaryColumns"
            size="small"
            row-key="key"
            :pagination="false"
            :scroll="{ y: 380 }"
          />
          <div class="summary-total">
            共 {{ summaryRows.length }} 个商品，合计数量
            {{ summaryTotalQty.toLocaleString('zh-CN') }}，合计金额 {{ formatMoney(summaryTotalAmt) }}
          </div>
        </a-spin>
      </a-modal>

      <!-- ═══ 出库单明细（单据头 + 商品明细） ═══ -->
      <a-modal
        v-model:open="showDetail"
        title="销售出库单明细"
        :width="920"
        :footer="null"
      >
        <a-spin :spinning="detailLoading">
          <a-descriptions
            bordered
            size="small"
            :column="3"
          >
            <a-descriptions-item label="单据编号">
              {{ detail.outboundNo || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="单据日期">
              {{ detail.outboundDate || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="来源订单">
              {{ detail.orderNo || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="客户">
              {{ detail.customerName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="仓库">
              {{ detail.warehouseName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="经手人">
              {{ detail.salesPersonName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="本单金额">
              {{ formatMoney(billAmount(detail)) }}
            </a-descriptions-item>
            <a-descriptions-item label="已结金额">
              {{ formatMoney(detail.settledAmount) }}
            </a-descriptions-item>
            <a-descriptions-item label="结算状态">
              {{ SETTLEMENT_STATUS_MAP[detail.settlementStatus]?.label || detail.settlementStatus || '-' }}
            </a-descriptions-item>
          </a-descriptions>
          <a-table
            class="detail-items"
            :data-source="detailItems"
            :columns="detailColumns"
            size="small"
            row-key="id"
            :pagination="false"
            :scroll="{ y: 320 }"
          />
        </a-spin>
      </a-modal>

      <!-- ═══ 页面配置（查询条件 25 项 / 功能按钮 / 打印配置） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="dispatch-ship-query-page-config"
        :print-config-items="PRINT_CONFIG_ITEMS"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'
import {
  PrinterOutlined,
  BarChartOutlined,
  ReloadOutlined,
  DownloadOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { outboundApi } from '@/api/erp'
import { mdRouteApi } from '@/api/md'
import { shipQueryApi } from '@/api/dms/ship-query'

defineOptions({ name: 'DispatchShipQuery' })

// ═══ 单据状态（与后端 OutboundStatus 一致） ═══
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '待审批', color: 'orange' },
  2: { label: '已审批', color: 'blue' },
  3: { label: '待拣货', color: 'orange' },
  4: { label: '拣货中', color: 'processing' },
  5: { label: '已拣货', color: 'cyan' },
  6: { label: '待打包', color: 'orange' },
  7: { label: '打包中', color: 'processing' },
  8: { label: '已打包', color: 'orange' },
  9: { label: '待发货', color: 'geekblue' },
  10: { label: '已发货', color: 'blue' },
  11: { label: '已完成', color: 'green' },
  12: { label: '已取消', color: 'red' },
}

const SETTLEMENT_STATUS_MAP: Record<string, { label: string; color: string }> = {
  unsettled: { label: '未结算', color: 'default' },
  partial: { label: '部分结算', color: 'orange' },
  settled: { label: '已结算', color: 'green' },
}

/** 配送状态（页面固定项）：对外三值口径，与《配送查询》一致 */
const DELIVERY_STATUS_OPTIONS = [
  { label: '待配送', value: 'PENDING' },
  { label: '配送中', value: 'DELIVERING' },
  { label: '已配送', value: 'DELIVERED' },
]

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const tableData = ref<any[]>([])
const selectedRowKeys = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 查询条件（25 项由页面配置驱动 + 2 项固定） ═══
const searchForm = reactive<Record<string, any>>({
  deliveryStatus: undefined,
  routeId: undefined,
  dateRange: [],
})
for (const f of [
  'outboundNo', 'customerName', 'salesPersonName', 'departmentName', 'creatorName', 'auditorName',
  'bookkeeperName', 'warehouseName', 'settlementStatus', 'generationMethod', 'sourceOrder', 'remark',
  'receiverName', 'receiverPhone', 'shippingAddress', 'logisticsCompany', 'trackingNumber',
  'extNum1', 'extNum2', 'extText1', 'extText2', 'extText3', 'deliveryMethod',
]) {
  searchForm[f] = undefined
}
searchForm.showRed = false

// ═══ 配送线路选择器（线路档案，仅启用） ═══
const routeLoading = ref(false)
const routeOptions = ref<any[]>([])
const routeOptionList = computed(() =>
  routeOptions.value.map((r: any) => ({
    label: r.routeCode ? `${r.routeName} [${r.routeCode}]` : r.routeName,
    value: r.id,
  })))

async function loadRouteOptions() {
  routeLoading.value = true
  try {
    const res: any = await mdRouteApi.options()
    routeOptions.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (error) {
    console.warn('[发货查询] 配送线路加载失败', error)
    routeOptions.value = []
  } finally {
    routeLoading.value = false
  }
}

// ═══ 查询条件定义（对标实测 25 项） ═══
interface QueryFieldDef {
  label: string
  type: 'input' | 'select' | 'number' | 'checkbox'
  placeholder?: string
  options?: Array<{ label: string; value: any }>
}

const QUERY_FIELD_DEFS: Record<string, QueryFieldDef> = {
  outboundNo: { label: '单据编号', type: 'input' },
  customerName: { label: '客户', type: 'input' },
  salesPersonName: { label: '经手人', type: 'input' },
  departmentName: { label: '部门', type: 'input' },
  creatorName: { label: '制单人', type: 'input' },
  auditorName: { label: '审核人', type: 'input' },
  bookkeeperName: { label: '记账人', type: 'input' },
  warehouseName: { label: '仓库', type: 'input' },
  settlementStatus: {
    label: '结算状态', type: 'select',
    options: [
      { label: '未结算', value: 'unsettled' },
      { label: '部分结算', value: 'partial' },
      { label: '已结算', value: 'settled' },
    ],
  },
  generationMethod: {
    label: '产生方式', type: 'select',
    options: [
      { label: '手工创建', value: '手工创建' }, { label: '订单生成', value: '订单生成' },
      { label: '复制', value: '复制' }, { label: '导入', value: '导入' },
    ],
  },
  sourceOrder: { label: '来源订单', type: 'input' },
  remark: { label: '单据备注', type: 'input' },
  receiverName: { label: '收货人', type: 'input' },
  receiverPhone: { label: '联系电话', type: 'input' },
  shippingAddress: { label: '收货地址', type: 'input' },
  logisticsCompany: { label: '物流公司', type: 'input' },
  trackingNumber: { label: '运单号', type: 'input' },
  extNum1: { label: '表头自定义字段1(数字)', type: 'number' },
  extNum2: { label: '表头自定义字段2(数字)', type: 'number' },
  extText1: { label: '表头自定义字段3(文本)', type: 'input' },
  extText2: { label: '表头自定义字段4(文本)', type: 'input' },
  extText3: { label: '表头自定义字段5(文本)', type: 'input' },
  deliveryMethod: {
    label: '配送方式', type: 'select',
    options: [
      { label: '自提', value: 'self' }, { label: '快递', value: 'express' },
      { label: '物流', value: 'logistics' }, { label: '配送', value: 'delivery' },
    ],
  },
  showRed: { label: '显示红冲', type: 'checkbox' },
}

interface QueryFieldSetting { key: string; label: string; visible: boolean }

/** 默认查询字段（对标实测 25 项；默认显隐按同单据《销售出库单》页口径对齐） */
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '日期', visible: true },
  { key: 'outboundNo', label: '单据编号', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'salesPersonName', label: '经手人', visible: true },
  { key: 'departmentName', label: '部门', visible: true },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'settlementStatus', label: '结算状态', visible: true },
  { key: 'generationMethod', label: '产生方式', visible: true },
  { key: 'sourceOrder', label: '来源订单', visible: true },
  { key: 'remark', label: '单据备注', visible: true },
  { key: 'showRed', label: '显示红冲', visible: true },
  { key: 'receiverName', label: '收货人', visible: false },
  { key: 'receiverPhone', label: '联系电话', visible: false },
  { key: 'shippingAddress', label: '收货地址', visible: false },
  { key: 'logisticsCompany', label: '物流公司', visible: false },
  { key: 'trackingNumber', label: '运单号', visible: false },
  { key: 'extNum1', label: '表头自定义字段1(数字)', visible: false },
  { key: 'extNum2', label: '表头自定义字段2(数字)', visible: false },
  { key: 'extText1', label: '表头自定义字段3(文本)', visible: false },
  { key: 'extText2', label: '表头自定义字段4(文本)', visible: false },
  { key: 'extText3', label: '表头自定义字段5(文本)', visible: false },
  { key: 'creatorName', label: '制单人', visible: false },
  { key: 'auditorName', label: '审核人', visible: false },
  { key: 'bookkeeperName', label: '记账人', visible: false },
  { key: 'deliveryMethod', label: '配送方式', visible: false },
]

const queryFields = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))

/** 渲染的查询条件：配置顺序 + 显隐 + 类型（「日期」为区间固定渲染） */
const renderQueryFields = computed(() =>
  queryFields.value
    .filter(f => f.visible && (f.key === 'date' || QUERY_FIELD_DEFS[f.key]))
    .map(f => ({ key: f.key, label: f.label, ...(QUERY_FIELD_DEFS[f.key] || { type: 'input' as const }) })))

// ═══ 功能按钮（对标实测 6 项；「配置」即右上角齿轮，恒显不参与开关） ═══
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'batchPrint', label: '批量打印', enabled: true },
  { key: 'productSummary', label: '商品汇总', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]

const functionButtons = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))
const showPageConfig = ref(false)

const PRINT_CONFIG_ITEMS = [
  { key: 'alwaysLastTemplate', label: '始终使用最后一次打印的模板，打印时不再选择' },
]
const printConfig = reactive<{ alwaysLastTemplate: boolean }>({ alwaysLastTemplate: false })

function isButtonEnabled(key: string): boolean {
  const hit = functionButtons.value.find(b => b.key === key)
  return hit ? hit.enabled : false
}

function handlePageConfigChange(config: any) {
  if (config?.queryFields) queryFields.value = config.queryFields.map((f: any) => ({ ...f }))
  if (config?.functionButtons) functionButtons.value = config.functionButtons.map((b: any) => ({ ...b }))
  if (config?.printConfig) printConfig.alwaysLastTemplate = !!config.printConfig.alwaysLastTemplate
}

// ═══ 数据表列（对标实测 49 列；默认 14 列可见） ═══
const money = (key: string, title: string, width = 110, defaultHidden = false) => ({
  title, key, width, align: 'right' as const, defaultHidden,
  formatter: (val: any) => formatMoney(val),
})

const columns: any[] = [
  { title: '', key: 'checkbox', type: 'checkbox', width: 40, fixed: 'left' },
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', slotName: 'actionCell', width: 110, fixed: 'left' },
  // ── 默认显示（14 列） ──
  { title: '单据日期', key: 'outboundDate', width: 110 },
  { title: '单据编号', key: 'outboundNo', type: 'slot', slotName: 'outboundNoCell', width: 165 },
  { title: '单据状态', key: 'status', type: 'slot', slotName: 'statusCell', width: 100 },
  { title: '来源订单', key: 'orderNo', width: 150 },
  { title: '仓库', key: 'warehouseName', width: 120 },
  { title: '客户', key: 'customerName', width: 190 },
  { title: '经手人', key: 'salesPersonName', width: 90 },
  { title: '本单金额', key: 'finalAmount', type: 'slot', slotName: 'finalAmountCell', width: 120, align: 'right' },
  money('settledAmount', '已结金额'),
  { title: '结算状态', key: 'settlementStatus', type: 'slot', slotName: 'settlementStatusCell', width: 100 },
  { title: '数量', key: 'totalQuantity', width: 90, align: 'right', formatter: (v: any) => v == null ? '-' : Number(v).toLocaleString('zh-CN') },
  { title: '产生方式', key: 'generationMethod', width: 100 },
  { title: '单据备注', key: 'remark', width: 150 },
  { title: '打印次数', key: 'printCount', width: 90, align: 'right' },
  // ── 默认隐藏（35 列） ──
  { title: '客户编号', key: 'customerCode', width: 110, defaultHidden: true },
  { title: '客户级别', key: 'customerLevel', width: 90, defaultHidden: true },
  { title: '收货人', key: 'receiverName', width: 100, defaultHidden: true },
  { title: '联系电话', key: 'receiverPhone', width: 125, defaultHidden: true },
  { title: '收货地址', key: 'shippingAddress', width: 200, defaultHidden: true },
  { title: '物流公司', key: 'logisticsCompany', width: 130, defaultHidden: true },
  { title: '运单号', key: 'trackingNumber', width: 150, defaultHidden: true },
  { title: '客户一票通', key: 'customerTicket', width: 110, defaultHidden: true },
  { title: '客户备注', key: 'customerRemark', width: 150, defaultHidden: true },
  { title: '部门', key: 'departmentName', width: 110, defaultHidden: true },
  money('totalAmount', '商品金额', 110, true),
  money('promoDiscount', '促销优惠', 110, true),
  money('couponAmount', '优惠劵', 100, true),
  money('directDiscount', '直接优惠', 100, true),
  { title: '运费承担方', key: 'freightPayer', width: 100, defaultHidden: true },
  money('freight', '运费', 90, true),
  money('otherFee', '其他费用', 100, true),
  { title: '配送方式', key: 'deliveryMethod', width: 100, defaultHidden: true },
  { title: '重量（kg）', key: 'totalWeight', width: 100, defaultHidden: true },
  { title: '体积（m³）', key: 'totalVolume', width: 100, defaultHidden: true },
  { title: '摘要', key: 'summary', width: 150, defaultHidden: true },
  { title: '附件', key: 'attachment', type: 'slot', slotName: 'attachmentCell', width: 80, defaultHidden: true },
  { title: '表头自定义字段1(数字)', key: 'extNum1', width: 140, defaultHidden: true },
  { title: '表头自定义字段2(数字)', key: 'extNum2', width: 140, defaultHidden: true },
  { title: '表头自定义字段3(文本)', key: 'extText1', width: 140, defaultHidden: true },
  { title: '表头自定义字段4(文本)', key: 'extText2', width: 140, defaultHidden: true },
  { title: '表头自定义字段5(文本)', key: 'extText3', width: 140, defaultHidden: true },
  { title: '表尾自定义字段1(文本)', key: 'footerExtText1', width: 150, defaultHidden: true },
  { title: '表尾自定义字段2(文本)', key: 'footerExtText2', width: 150, defaultHidden: true },
  { title: '制单人', key: 'creatorName', width: 90, defaultHidden: true },
  { title: '记账人', key: 'bookkeeperName', width: 90, defaultHidden: true },
  { title: '审核人', key: 'auditorName', width: 90, defaultHidden: true },
  { title: '记账时间', key: 'bookkeepingTime', width: 150, defaultHidden: true },
  { title: '制单时间', key: 'createTime', width: 150, defaultHidden: true },
  { title: '打印时间', key: 'printTime', width: 150, defaultHidden: true },
]

function formatMoney(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/**
 * 本单金额 = 商品金额 − 促销优惠 − 优惠劵 − 直接优惠 + 运费 + 其他费用
 * 与后端导出接口 `SaleOutboundController#billAmount` 同一口径（分单位整数运算，规避浮点误差）。
 */
function billAmount(record: any): number {
  const cents = (v: any) => Math.round((Number(v) || 0) * 100)
  return (cents(record?.totalAmount) - cents(record?.promoDiscount) - cents(record?.couponAmount)
    - cents(record?.directDiscount) + cents(record?.freight) + cents(record?.otherFee)) / 100
}

function handleSelectionChange(_rows: any[], ids: any[]) {
  selectedRowKeys.value = ids || []
}

const selectedRows = computed(() =>
  tableData.value.filter((r: any) => selectedRowKeys.value.includes(r.id)))

// ═══ 查询参数拼装 ═══
/**
 * 配送状态/配送线路 → 出库单号集合（DMS 侧按来源单据号反查）。
 * 两项都没选时不调用，保持销售出库单原生查询口径不变。
 */
async function resolveOutboundNos(): Promise<string[] | undefined> {
  if (!searchForm.deliveryStatus && searchForm.routeId == null) return undefined
  const res: any = await shipQueryApi.outboundFilter({
    deliveryStatus: searchForm.deliveryStatus || undefined,
    routeId: searchForm.routeId ?? undefined,
  })
  if (res?.truncated) {
    message.warning(`按配送条件命中的出库单超过上限（${res.matched} 条），请收窄日期或增加条件`)
  }
  return res?.sourceBillNos || []
}

function buildQueryParams(outboundNos?: string[]): Record<string, any> {
  const params: Record<string, any> = {}
  if (searchForm.dateRange?.length === 2) {
    params.dateStart = searchForm.dateRange[0]
    params.dateEnd = searchForm.dateRange[1]
  }
  for (const key of [
    'outboundNo', 'customerName', 'salesPersonName', 'departmentName', 'creatorName', 'auditorName',
    'bookkeeperName', 'warehouseName', 'settlementStatus', 'generationMethod', 'sourceOrder', 'remark',
    'receiverName', 'receiverPhone', 'shippingAddress', 'logisticsCompany', 'trackingNumber',
    'extNum1', 'extNum2', 'extText1', 'extText2', 'extText3', 'deliveryMethod',
  ]) {
    const val = searchForm[key]
    if (val !== undefined && val !== null && val !== '') params[key] = val
  }
  params.showRed = !!searchForm.showRed
  if (outboundNos) {
    // 配送条件已启用：命中为空时用不存在单号占位，保证「无数据」而不是「全量」
    params.outboundNos = outboundNos.length ? outboundNos.join(',') : '__NONE__'
  }
  return params
}

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const outboundNos = await resolveOutboundNos()
    const res: any = await outboundApi.page({
      ...buildQueryParams(outboundNos),
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[发货查询] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handleReset() {
  searchForm.deliveryStatus = undefined
  searchForm.routeId = undefined
  searchForm.dateRange = []
  for (const key of Object.keys(QUERY_FIELD_DEFS)) searchForm[key] = undefined
  searchForm.showRed = false
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 明细查看 ═══
const showDetail = ref(false)
const detailLoading = ref(false)
const detail = ref<any>({})
const detailItems = ref<any[]>([])
const detailColumns: any[] = [
  { title: '商品名称', dataIndex: 'productName', width: 200 },
  { title: '货号', dataIndex: 'productCode', width: 110 },
  { title: '规格', dataIndex: 'specification', width: 100 },
  { title: '单位', dataIndex: 'productUnit', width: 70 },
  { title: '数量', dataIndex: 'quantity', width: 90, align: 'right' },
  { title: '单价', dataIndex: 'unitPrice', width: 100, align: 'right' },
  { title: '金额', dataIndex: 'lineAmount', width: 110, align: 'right' },
  { title: '备注', dataIndex: 'remark', width: 140 },
]

async function handleOpenDetail(record: any) {
  showDetail.value = true
  detailLoading.value = true
  detail.value = record || {}
  detailItems.value = []
  try {
    const [head, items] = await Promise.all([
      outboundApi.getById(record.id),
      outboundApi.getItems(record.id),
    ])
    if (head) detail.value = head
    detailItems.value = Array.isArray(items) ? items : []
  } catch (error) {
    console.warn('[发货查询] 明细加载失败', error)
    message.error('明细加载失败')
  } finally {
    detailLoading.value = false
  }
}

// ═══ 打印（模板渲染；成功后回写打印次数） ═══
const printDialogRef = ref<InstanceType<typeof PrintDialog> | null>(null)
const printData = ref<Record<string, any>>({})
const printQueue = ref<any[]>([])
const showBatchPrint = ref(false)
const onlyUnprinted = ref(false)

async function openPrint(record: any) {
  printData.value = {
    ...record,
    finalAmount: billAmount(record),
    statusText: STATUS_MAP[record.status]?.label || record.status,
    settlementStatusText: SETTLEMENT_STATUS_MAP[record.settlementStatus]?.label || '',
  }
  await nextTick()
  printDialogRef.value?.open()
}

/** 打印(F8)：打印勾选的第一条 */
async function handlePrintF8() {
  if (!selectedRowKeys.value.length) {
    message.warning('请先勾选要打印的出库单')
    return
  }
  printQueue.value = []
  await openPrint(selectedRows.value[0])
}

function openBatchPrintModal() {
  if (!selectedRowKeys.value.length) {
    message.warning('请选中至少一条数据！')
    return
  }
  showBatchPrint.value = true
}

async function confirmBatchPrint() {
  showBatchPrint.value = false
  const list = onlyUnprinted.value
    ? selectedRows.value.filter((r: any) => !Number(r.printCount))
    : selectedRows.value
  if (!list.length) {
    message.warning('没有符合打印条件的出库单（均已打印过）')
    return
  }
  const [first, ...rest] = list
  printQueue.value = rest
  await openPrint(first)
}

/** 打印成功：回写出库单打印次数（真实落库）并驱动打印队列 */
async function handlePrintSuccess() {
  const id = printData.value?.id
  if (id) {
    try {
      await outboundApi.print(id)
    } catch (error) {
      console.warn('[发货查询] 打印次数回写失败', error)
    }
  }
  const next = printQueue.value.shift()
  if (next) {
    await openPrint(next)
  } else {
    fetchList()
  }
}

// ═══ 商品汇总（按当前查询条件聚合明细行） ═══
const showProductSummary = ref(false)
const summaryLoading = ref(false)
const summaryRows = ref<any[]>([])
const summaryColumns: any[] = [
  { title: '商品编码', dataIndex: 'code', width: 140 },
  { title: '商品名称', dataIndex: 'name', width: 280 },
  { title: '数量', dataIndex: 'totalQty', width: 110, align: 'right', formatter: (v: any) => Number(v || 0).toLocaleString('zh-CN') },
  { title: '金额', dataIndex: 'totalAmt', width: 130, align: 'right', formatter: (v: any) => formatMoney(v) },
]
const summaryTotalQty = computed(() => summaryRows.value.reduce((s, r) => s + Number(r.totalQty || 0), 0))
const summaryTotalAmt = computed(() => summaryRows.value.reduce((s, r) => s + Number(r.totalAmt || 0), 0))

async function handleProductSummary() {
  showProductSummary.value = true
  summaryLoading.value = true
  summaryRows.value = []
  try {
    const outboundNos = await resolveOutboundNos()
    const res: any = await outboundApi.pageDetail({
      ...buildQueryParams(outboundNos),
      pageNum: 1,
      pageSize: 9999,
    })
    const records: any[] = res?.records || []
    if (!records.length) {
      message.info('当前查询条件下无数据')
      return
    }
    const map = new Map<string, any>()
    for (const item of records) {
      const key = item.productName || item.productCode || '未知商品'
      const row = map.get(key) || { key, name: item.productName || '', code: item.productCode || '', totalQty: 0, totalAmt: 0 }
      row.totalQty += Number(item.quantity || 0)
      row.totalAmt += Number(item.lineAmount ?? item.amount ?? 0)
      map.set(key, row)
    }
    summaryRows.value = Array.from(map.values()).sort((a, b) => b.totalAmt - a.totalAmt)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '商品汇总失败')
  } finally {
    summaryLoading.value = false
  }
}

// ═══ 导出（后端真实 xlsx） ═══
async function handleExport() {
  exporting.value = true
  try {
    const outboundNos = await resolveOutboundNos()
    await outboundApi.exportExcel(buildQueryParams(outboundNos))
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══ F8 快捷键 ═══
function onKeydown(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrintF8()
  }
}

function handleError(error: Error) {
  console.error('[发货查询] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  loadRouteOptions()
  fetchList()
  window.addEventListener('keydown', onKeydown)
})
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.table-area { flex: 1; min-height: 0; overflow: hidden; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.summary-total { margin-top: 10px; font-size: 13px; color: #333; }
.detail-items { margin-top: 12px; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
