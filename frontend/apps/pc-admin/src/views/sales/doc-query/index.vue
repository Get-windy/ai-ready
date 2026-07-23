<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 快捷日期 ═══ -->
        <template #toolbar-left>
          <div class="query-scheme-wrap">
            <a-select
              v-model:value="queryScheme"
              style="width: 140px"
              size="small"
              placeholder="--查询方案--"
            >
              <a-select-option value="">--查询方案--</a-select-option>
            </a-select>
            <a-button
              type="link"
              size="small"
              style="padding: 0 4px"
            >
              <PlusOutlined />
            </a-button>
          </div>
          <a-space :size="4" class="quick-dates">
            <a-button
              v-for="d in quickDates"
              :key="d.key"
              :type="quickDate === d.key ? 'primary' : 'link'"
              size="small"
              @click="setQuickDate(d.key)"
            >
              {{ d.label }}
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="页面配置">
              <a-button size="small" @click="showPageConfig = true">
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button size="small" @click="fetchData">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button size="small" @click="handlePrint">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-dropdown>
              <a-button size="small">
                <ExportOutlined /> 导出 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu @click="handleExportMenu">
                  <a-menu-item key="csv">导出CSV</a-menu-item>
                  <a-menu-item key="excel">导出Excel</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-button size="small" @click="showColumnConfig = true">
              <TableOutlined />
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 搜索区域 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-container" :data-expanded="showMoreConditions || null">
            <div class="search-grid" ref="gridRef">
              <!-- 始终显示的字段 -->
              <div class="search-field-item">
                <a-range-picker
                  v-model:value="documentDateRange"
                  size="small"
                  style="width: 100%"
                  @change="handleDocumentDateChange"
                />
              </div>
              <div class="search-field-item">
                <div class="search-select-wrap">
                  <span class="search-select-label">日期类型</span>
                  <a-select
                    v-model:value="searchParams.dateType"
                    size="small"
                  >
                    <a-select-option value="documentDate">单据日期</a-select-option>
                    <a-select-option value="createTime">制单时间</a-select-option>
                    <a-select-option value="bookkeepingTime">记账时间</a-select-option>
                    <a-select-option value="sourceOrderDate">来源订单日期</a-select-option>
                  </a-select>
                </div>
              </div>
              <div class="search-field-item">
                <a-input
                  v-model:value="searchParams.documentNo"
                  placeholder="单据编号"
                  allow-clear
                  size="small"
                />
              </div>
              <div class="search-field-item">
                <a-input
                  v-model:value="searchParams.customerName"
                  placeholder="客户"
                  allow-clear
                  size="small"
                  :suffix="h(SearchOutlined, { style: 'color:#bbb' })"
                />
              </div>
              <div class="search-field-item">
                <a-input
                  v-model:value="searchParams.handlerName"
                  placeholder="经手人"
                  allow-clear
                  size="small"
                  :suffix="h(SearchOutlined, { style: 'color:#bbb' })"
                />
              </div>
              <div class="search-field-item">
                <a-input
                  v-model:value="searchParams.warehouseName"
                  placeholder="仓库"
                  allow-clear
                  size="small"
                  :suffix="h(SearchOutlined, { style: 'color:#bbb' })"
                />
              </div>
              <div class="search-field-item">
                <div class="search-select-wrap">
                  <span class="search-select-label">结算状态</span>
                  <a-select
                    v-model:value="searchParams.settlementStatus"
                    placeholder="全部"
                    allow-clear
                    size="small"
                  >
                    <a-select-option value="">全部</a-select-option>
                    <a-select-option value="UNPAID">未结算</a-select-option>
                    <a-select-option value="PARTIAL_PAID">部分结算</a-select-option>
                    <a-select-option value="PAID">已结算</a-select-option>
                  </a-select>
                </div>
              </div>
              <div class="search-field-item">
                <a-input
                  v-model:value="searchParams.sourceOrder"
                  placeholder="来源订单"
                  allow-clear
                  size="small"
                />
              </div>
              <div class="search-field-item">
                <div class="search-select-wrap">
                  <span class="search-select-label">单据类型</span>
                  <a-select
                    v-model:value="searchParams.documentType"
                    placeholder="全部单据"
                    allow-clear
                    size="small"
                  >
                    <a-select-option value="">全部单据</a-select-option>
                    <a-select-option value="SALE_ORDER">销售订单</a-select-option>
                    <a-select-option value="OUTBOUND">出库单</a-select-option>
                    <a-select-option value="RETURN">退货单</a-select-option>
                    <a-select-option value="EXCHANGE">换货单</a-select-option>
                  </a-select>
                </div>
              </div>
              <div class="search-field-item">
                <div class="search-select-wrap">
                  <span class="search-select-label">商品行属性</span>
                  <a-select
                    v-model:value="searchParams.productAttribute"
                    placeholder="全部"
                    allow-clear
                    size="small"
                  >
                    <a-select-option value="">全部</a-select-option>
                    <a-select-option value="NORMAL">普通商品</a-select-option>
                    <a-select-option value="GIFT">赠品</a-select-option>
                    <a-select-option value="COMBO">组合商品</a-select-option>
                  </a-select>
                </div>
              </div>
              <!-- 更多条件（展开后显示） -->
              <template v-if="showMoreConditions">
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.receiverName"
                    placeholder="收货人"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.receiverPhone"
                    placeholder="联系电话"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.shippingAddress"
                    placeholder="收货地址"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.departmentName"
                    placeholder="部门"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.creatorName"
                    placeholder="制单人"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.bookkeeperName"
                    placeholder="记账人"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-range-picker
                    v-model:value="sourceOrderDateRange"
                    size="small"
                    style="width: 100%"
                    @change="handleSourceOrderDateChange"
                  />
                </div>
                <div class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">产生方式</span>
                    <a-select
                      v-model:value="searchParams.generationMethod"
                      placeholder="全部"
                      allow-clear
                      size="small"
                    >
                      <a-select-option value="">全部</a-select-option>
                      <a-select-option value="MANUAL">手工录入</a-select-option>
                      <a-select-option value="SYSTEM">系统生成</a-select-option>
                      <a-select-option value="IMPORT">批量导入</a-select-option>
                      <a-select-option value="API">API接口</a-select-option>
                    </a-select>
                  </div>
                </div>
                <div class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">销售类型</span>
                    <a-select
                      v-model:value="searchParams.salesType"
                      placeholder="全部"
                      allow-clear
                      size="small"
                    >
                      <a-select-option value="">全部</a-select-option>
                      <a-select-option value="RETAIL">零售</a-select-option>
                      <a-select-option value="WHOLESALE">批发</a-select-option>
                      <a-select-option value="ONLINE">线上</a-select-option>
                      <a-select-option value="OFFLINE">线下</a-select-option>
                    </a-select>
                  </div>
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.remark"
                    placeholder="单据备注"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.buyerRemark"
                    placeholder="买家备注"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input-number
                    v-model:value="searchParams.minTotalAmount"
                    placeholder="最小值"
                    size="small"
                    style="width: 100%"
                  />
                  <span style="margin: 0 4px; flex-shrink: 0">-</span>
                  <a-input-number
                    v-model:value="searchParams.maxTotalAmount"
                    placeholder="最大值"
                    size="small"
                    style="width: 100%"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.logisticsCompany"
                    placeholder="物流公司"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.trackingNumber"
                    placeholder="运单号"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.region"
                    placeholder="区域"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">来源</span>
                    <a-select
                      v-model:value="searchParams.source"
                      placeholder="全部"
                      allow-clear
                      size="small"
                    >
                      <a-select-option value="">全部</a-select-option>
                      <a-select-option value="PC">电脑端</a-select-option>
                      <a-select-option value="MOBILE">移动端</a-select-option>
                      <a-select-option value="API">API接口</a-select-option>
                      <a-select-option value="IMPORT">批量导入</a-select-option>
                    </a-select>
                  </div>
                </div>
                <div class="search-field-item">
                  <a-input-number
                    v-model:value="searchParams.extNum1Min"
                    placeholder="最小值"
                    size="small"
                    style="width: 100%"
                  />
                  <span style="margin: 0 4px; flex-shrink: 0">-</span>
                  <a-input-number
                    v-model:value="searchParams.extNum1Max"
                    placeholder="最大值"
                    size="small"
                    style="width: 100%"
                  />
                </div>
                <div class="search-field-item">
                  <a-input-number
                    v-model:value="searchParams.extNum2Min"
                    placeholder="最小值"
                    size="small"
                    style="width: 100%"
                  />
                  <span style="margin: 0 4px; flex-shrink: 0">-</span>
                  <a-input-number
                    v-model:value="searchParams.extNum2Max"
                    placeholder="最大值"
                    size="small"
                    style="width: 100%"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.extText1"
                    placeholder="自定义字段3"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.extText2"
                    placeholder="自定义字段4"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.extText3"
                    placeholder="自定义字段5"
                    allow-clear
                    size="small"
                  />
                </div>
              </template>
              <!-- 查询按钮 + 勾选框 -->
              <div class="search-action-group" ref="actionRef" :style="{ gridColumn: 'span ' + actionSpan }">
              <div class="search-field-item search-action-item">
                <a-button type="primary" size="small" @click="handleSearch">
                  <SearchOutlined /> 查询
                </a-button>
              </div>
              <div class="search-field-item">
                <a-checkbox v-model:checked="searchParams.onlyVehicleWarehouse">
                  仅统计车辆库
                </a-checkbox>
              </div>
              <div class="search-field-item">
                <a-checkbox v-model:checked="searchParams.showRed">显示红冲单据</a-checkbox>
              </div>
              </div>
            </div>
            <div class="search-more-toggle">
              <a-button size="small" @click="toggleMoreConditions">
                <DownOutlined v-if="!showMoreConditions" />
                <UpOutlined v-else />
                {{ showMoreConditions ? '收起' : '更多条件' }}
              </a-button>
            </div>
          </div>
          </div>
        </template>

        <!-- ═══ 数据表格 ═══ -->
        <template #table>
          <BillTableList
            :columns="visibleColumns"
            :data-source="tableData"
            :loading="loading"
            :pagination="billPagination"
            :show-toolbar="false"
            :show-search="false"
            :show-add="false"
            :show-export="false"
            :show-batch-delete="false"
            :selectable="false"
            row-key="id"
            @page-change="handlePageChange"
          >
            <!-- 操作列 -->
            <template #actionCell="{ record }">
              <a-space :size="4">
                <a-button type="link" size="small" @click="handleCopy(record)">复制</a-button>
                <a-button type="link" size="small" @click="handleNote(record)">整单备注</a-button>
              </a-space>
            </template>
          </BillTableList>
        </template>

        <!-- ═══ 表格底部：合计 ═══ -->
        <template #table-footer>
          <div class="table-footer">
            <div class="footer-label">合计</div>
            <div class="footer-values">
              <span v-if="summaryData.salesQuantity">销售数量: {{ summaryData.salesQuantity }}</span>
              <span v-if="summaryData.amount">金额: {{ summaryData.amount }}</span>
              <span v-if="summaryData.totalAmount">本单金额: {{ summaryData.totalAmount }}</span>
              <span v-if="summaryData.grossProfit">毛利: {{ summaryData.grossProfit }}</span>
            </div>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldsConfig"
      :function-buttons-config="functionButtonConfig"
      storage-key="sales-doc-query-page-config"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- ═══ 列配置弹窗 ═══ -->
    <ColumnConfigPanel
      :open="showColumnConfig"
      :settings-columns="settingsColumns"
      :is-locked-column="isLockedColumn"
      @update:open="showColumnConfig = $event"
      @change="handleColumnConfigChange"
      @reset="handleColumnConfigReset"
      @drag-end="handleColumnConfigChange"
    />

    <!-- ═══ 整单备注弹窗 ═══ -->
    <a-modal
      v-model:open="showNoteModal"
      title="整单备注"
      :width="480"
      @ok="saveNote"
      @cancel="showNoteModal = false"
    >
      <div style="margin-bottom: 8px; color: #666; font-size: 12px">
        单据编号: {{ noteRecord.documentNo }}
      </div>
      <a-textarea
        v-model:value="noteContent"
        :rows="4"
        placeholder="请输入整单备注内容"
      />
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined, SearchOutlined, DownOutlined, UpOutlined,
  SettingOutlined, TableOutlined, PlusOutlined, PrinterOutlined, ExportOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import { useColumnConfig, isLockedColumn } from '@/composables/useColumnConfig'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import request from '@/utils/request'

const router = useRouter()

// ═══ 状态 ═══
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const showMoreConditions = ref(false)
const showPageConfig = ref(false)
const showColumnConfig = ref(false)

const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

// ═══ 查询方案 ═══
const queryScheme = ref('')

// ═══ 快捷日期 ═══
const quickDate = ref('thisWeek')
const quickDates = [
  { key: 'yesterday', label: '昨日' },
  { key: 'today', label: '今日' },
  { key: 'thisWeek', label: '本周' },
  { key: 'lastWeek', label: '近一周' },
  { key: 'thisMonth', label: '本月' },
  { key: 'lastMonth', label: '上月' },
  { key: 'last3Months', label: '近三月' },
  { key: 'thisYear', label: '本年' },
]

const documentDateRange = ref<[Dayjs, Dayjs] | null>(null)

function setQuickDate(key: string) {
  quickDate.value = key
  const today = dayjs()
  let start: Dayjs
  let end: Dayjs

  switch (key) {
    case 'yesterday':
      start = end = today.subtract(1, 'day')
      break
    case 'today':
      start = end = today
      break
    case 'thisWeek':
      start = today.startOf('week')
      end = today
      break
    case 'lastWeek':
      start = today.subtract(7, 'day')
      end = today
      break
    case 'thisMonth':
      start = today.startOf('month')
      end = today
      break
    case 'lastMonth':
      start = today.subtract(1, 'month').startOf('month')
      end = today.subtract(1, 'month').endOf('month')
      break
    case 'last3Months':
      start = today.subtract(3, 'month')
      end = today
      break
    case 'thisYear':
      start = today.startOf('year')
      end = today
      break
    default:
      return
  }

  documentDateRange.value = [start, end]
  searchParams.startDate = start.format('YYYY-MM-DD')
  searchParams.endDate = end.format('YYYY-MM-DD')
  handleSearch()
}

// ═══ 搜索参数 ═══
const searchParams = reactive({
  dateType: 'documentDate',
  documentNo: '',
  documentType: '',
  startDate: '',
  endDate: '',
  customerName: '',
  receiverName: '',
  receiverPhone: '',
  shippingAddress: '',
  handlerName: '',
  departmentName: '',
  creatorName: '',
  bookkeeperName: '',
  warehouseName: '',
  settlementStatus: '',
  sourceOrder: '',
  sourceOrderStartDate: '',
  sourceOrderEndDate: '',
  generationMethod: '',
  salesType: '',
  productAttribute: '',
  remark: '',
  buyerRemark: '',
  extNum1Min: null as number | null,
  extNum1Max: null as number | null,
  extNum2Min: null as number | null,
  extNum2Max: null as number | null,
  extText1: '',
  extText2: '',
  extText3: '',
  logisticsCompany: '',
  trackingNumber: '',
  region: '',
  minTotalAmount: null as number | null,
  maxTotalAmount: null as number | null,
  source: '',
  showRed: false,
  onlyVehicleWarehouse: false
})

const sourceOrderDateRange = ref<[Dayjs, Dayjs] | null>(null)

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total
}))

// ═══ 51个表格列（按文档要求顺序，BillTableList格式） ═══
const columns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' as const },
  { title: '操作', key: 'action', type: 'action', width: 120, fixed: 'right' as const, slotName: 'actionCell' },
  { title: '单据日期', field: 'documentDate', key: 'documentDate', width: 100, sortable: true },
  { title: '单据编号', field: 'documentNo', key: 'documentNo', width: 140, sortable: true },
  { title: '单据类型', field: 'documentType', key: 'documentType', width: 90 },
  { title: '入库仓库', field: 'inboundWarehouse', key: 'inboundWarehouse', width: 100 },
  { title: '出库仓库', field: 'outboundWarehouse', key: 'outboundWarehouse', width: 100, sortable: true },
  { title: '客户', field: 'customerName', key: 'customerName', width: 120, sortable: true },
  { title: '客户编号', field: 'customerCode', key: 'customerCode', width: 110 },
  { title: '客户级别', field: 'customerLevel', key: 'customerLevel', width: 90 },
  { title: '收货人', field: 'receiverName', key: 'receiverName', width: 80 },
  { title: '联系电话', field: 'receiverPhone', key: 'receiverPhone', width: 110 },
  { title: '收货地址', field: 'shippingAddress', key: 'shippingAddress', width: 140 },
  { title: '表头自定义字段1(数字)', field: 'extNum1', key: 'extNum1', width: 80, align: 'right' as const },
  { title: '表头自定义字段2(数字)', field: 'extNum2', key: 'extNum2', width: 80, align: 'right' as const },
  { title: '表头自定义字段3(文本)', field: 'extText1', key: 'extText1', width: 100 },
  { title: '表头自定义字段4(文本)', field: 'extText2', key: 'extText2', width: 100 },
  { title: '表头自定义字段5(文本)', field: 'extText3', key: 'extText3', width: 100 },
  { title: '买家备注', field: 'buyerRemark', key: 'buyerRemark', width: 120 },
  { title: '客户备注', field: 'customerRemark', key: 'customerRemark', width: 120 },
  { title: '来源订单', field: 'sourceOrder', key: 'sourceOrder', width: 140 },
  { title: '来源订单日期', field: 'sourceOrderDate', key: 'sourceOrderDate', width: 100 },
  { title: '物流公司', field: 'logisticsCompany', key: 'logisticsCompany', width: 100 },
  { title: '运单号', field: 'trackingNumber', key: 'trackingNumber', width: 120 },
  { title: '区域', field: 'region', key: 'region', width: 80 },
  { title: '产生方式', field: 'generationMethod', key: 'generationMethod', width: 80 },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 80 },
  { title: '部门', field: 'departmentName', key: 'departmentName', width: 80 },
  { title: '结算状态', field: 'settlementStatus', key: 'settlementStatus', width: 80 },
  { title: '销售数量', field: 'salesQuantity', key: 'salesQuantity', width: 80, align: 'right' as const },
  { title: '金额', field: 'amount', key: 'amount', width: 90, align: 'right' as const },
  { title: '折后金额', field: 'discountedAmount', key: 'discountedAmount', width: 90, align: 'right' as const },
  { title: '销售收入', field: 'salesRevenue', key: 'salesRevenue', width: 90, align: 'right' as const },
  { title: '运费承担方', field: 'freightPayer', key: 'freightPayer', width: 90 },
  { title: '运费', field: 'freight', key: 'freight', width: 80, align: 'right' as const },
  { title: '其它费用', field: 'otherFee', key: 'otherFee', width: 80, align: 'right' as const },
  { title: '抹零金额', field: 'roundingAmount', key: 'roundingAmount', width: 80, align: 'right' as const },
  { title: '本单金额', field: 'totalAmount', key: 'totalAmount', width: 90, align: 'right' as const },
  { title: '促销优惠', field: 'promoDiscount', key: 'promoDiscount', width: 80, align: 'right' as const },
  { title: '优惠券优惠', field: 'couponAmount', key: 'couponAmount', width: 90, align: 'right' as const },
  { title: '直接优惠', field: 'directDiscount', key: 'directDiscount', width: 80, align: 'right' as const },
  { title: '积分抵扣', field: 'pointsDeduction', key: 'pointsDeduction', width: 80, align: 'right' as const },
  { title: '成本金额', field: 'costAmount', key: 'costAmount', width: 90, align: 'right' as const },
  { title: '毛利', field: 'grossProfit', key: 'grossProfit', width: 80, align: 'right' as const },
  { title: '销售类型', field: 'salesType', key: 'salesType', width: 80 },
  { title: '单据备注', field: 'remark', key: 'remark', width: 120 },
  { title: '摘要', field: 'summary', key: 'summary', width: 120 },
  { title: '附件', field: 'attachment', key: 'attachment', width: 60 },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 80 },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 80 },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 100 },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 100 },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 60, align: 'right' as const }
]

// ═══ 列配置 ═══
const columnDefs = computed(() => columns.map(col => ({ ...col })))
const {
  visibleColumns,
  settingsColumns,
  onSettingChange: onColumnSettingChange,
  resetSettings: resetColumnSettings,
} = useColumnConfig(columnDefs.value, 'sales-doc-query-list-columns')

// ═══ 合计汇总 ═══
const summaryData = computed(() => {
  const data = tableData.value
  if (!data.length) return {}
  const sum = (key: string) => {
    const val = data.reduce((acc, r) => acc + (Number(r[key]) || 0), 0)
    return val ? val.toFixed(2) : ''
  }
  return {
    salesQuantity: sum('salesQuantity'),
    amount: sum('amount'),
    totalAmount: sum('totalAmount'),
    grossProfit: sum('grossProfit'),
  }
})

// ═══ 页面配置 ═══
const queryFieldsConfig = ref([
  { key: 'dateRange', label: '日期', visible: true },
  { key: 'dateType', label: '日期类型', visible: true },
  { key: 'documentNo', label: '单据编号', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'settlementStatus', label: '结算状态', visible: true },
  { key: 'sourceOrder', label: '来源订单', visible: true },
  { key: 'documentType', label: '单据类型', visible: true },
  { key: 'productAttribute', label: '商品行属性', visible: true },
  { key: 'receiverName', label: '收货人', visible: false },
  { key: 'receiverPhone', label: '联系电话', visible: false },
  { key: 'shippingAddress', label: '收货地址', visible: false },
  { key: 'departmentName', label: '部门', visible: false },
  { key: 'creatorName', label: '制单人', visible: false },
  { key: 'bookkeeperName', label: '记账人', visible: false },
  { key: 'sourceOrderDate', label: '来源订单日期', visible: false },
  { key: 'generationMethod', label: '产生方式', visible: false },
  { key: 'salesType', label: '销售类型', visible: false },
  { key: 'remark', label: '单据备注', visible: false },
  { key: 'buyerRemark', label: '买家备注', visible: false },
  { key: 'extNum1', label: '自定义字段1(数字)', visible: false },
  { key: 'extNum2', label: '自定义字段2(数字)', visible: false },
  { key: 'extText1', label: '自定义字段3(文本)', visible: false },
  { key: 'extText2', label: '自定义字段4(文本)', visible: false },
  { key: 'extText3', label: '自定义字段5(文本)', visible: false },
  { key: 'logisticsCompany', label: '物流公司', visible: false },
  { key: 'trackingNumber', label: '运单号', visible: false },
  { key: 'region', label: '区域', visible: false },
  { key: 'totalAmount', label: '本单金额', visible: false },
  { key: 'source', label: '来源', visible: false },
  { key: 'showRed', label: '显示红冲', visible: false },
  { key: 'onlyVehicleWarehouse', label: '仅统计车辆库', visible: false }
])

const functionButtonConfig = ref([
  { key: 'search', label: '查询', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'print', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'more', label: '更多条件', enabled: true }
])

const handlePageConfigChange = () => {
  // 页面配置变更后由 PageConfigPanel 自行持久化
}

const handleColumnConfigChange = () => {
  onColumnSettingChange()
}

const handleColumnConfigReset = () => {
  resetColumnSettings()
}

// ═══ 日期处理 ═══
const handleDocumentDateChange = (dates: [Dayjs, Dayjs] | null) => {
  if (dates?.length === 2) {
    searchParams.startDate = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.endDate = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    searchParams.startDate = ''
    searchParams.endDate = ''
  }
}

const handleSourceOrderDateChange = (dates: [Dayjs, Dayjs] | null) => {
  if (dates?.length === 2) {
    searchParams.sourceOrderStartDate = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.sourceOrderEndDate = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    searchParams.sourceOrderStartDate = ''
    searchParams.sourceOrderEndDate = ''
  }
}

const toggleMoreConditions = () => {
  showMoreConditions.value = !showMoreConditions.value
}

// ═══ 数据请求 ═══
const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const apiParams: Record<string, any> = {
      current: pagination.current,
      size: pagination.pageSize,
      ...searchParams
    }
    // 清理空字符串参数
    Object.keys(apiParams).forEach(key => {
      if (apiParams[key] === '' || apiParams[key] === null || apiParams[key] === undefined) {
        delete apiParams[key]
      }
    })

    const res: any = await request.get('/sales/doc-query/page', { params: apiParams })
    if (res) {
      const data = res.data || res
      tableData.value = data.records || data.content || data.list || []
      pagination.total = data.total || 0
    }
  } catch (e: any) {
    hasError.value = true
    message.error('查询失败，请检查网络后重试')
    console.warn('[销售单据查询] 获取失败', e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ═══ 导出 ═══
const DOC_TYPE_LABELS: Record<string, string> = {
  SALE_ORDER: '销售订单',
  OUTBOUND: '出库单',
  RETURN: '退货单',
  EXCHANGE: '换货单',
}

function handleExportMenu({ key }: { key: string | number }) {
  if (tableData.value.length === 0) {
    message.warning('没有可导出的数据')
    return
  }

  // 导出当前可见列
  const exportCols = visibleColumns.value.filter((c: any) => c.key !== 'rowNo' && c.key !== 'operation')
  const headers = exportCols.map((c: any) => c.title)
  const rows = tableData.value.map(row =>
    exportCols.map((col: any) => {
      const val = row[col.field]
      if (val === null || val === undefined) return ''
      if (col.key === 'documentType') return DOC_TYPE_LABELS[val] || val
      return String(val)
    })
  )

  if (key === 'csv') {
    const csv = [headers.join(','), ...rows.map(r => r.map(v => `"${v}"`).join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `销售单据查询_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } else {
    // Excel (简单TSV格式，浏览器端生成)
    const tsv = [headers.join('\t'), ...rows.map(r => r.join('\t'))].join('\n')
    const blob = new Blob(['\uFEFF' + tsv], { type: 'application/vnd.ms-excel;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `销售单据查询_${dayjs().format('YYYYMMDD_HHmmss')}.xls`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  }
}

// ═══ 打印 ═══
function handlePrint() {
  window.print()
}

// ═══ 复制 ═══
const COPY_ROUTE_MAP: Record<string, string> = {
  SALE_ORDER: '/sales/order',
  OUTBOUND: '/sales/outbound/create',
  RETURN: '/sales/return-doc/create',
  EXCHANGE: '/sales/exchange/create',
}

function handleCopy(record: any) {
  const route = COPY_ROUTE_MAP[record.documentType]
  if (route) {
    router.push({ path: route, query: { copyFrom: record.id } })
  } else {
    message.warning('暂不可用该单据类型的复制')
  }
}

// ═══ 整单备注 ═══
const showNoteModal = ref(false)
const noteRecord = reactive<any>({ documentNo: '' })
const noteContent = ref('')

function handleNote(record: any) {
  noteRecord.documentNo = record.documentNo
  noteRecord.id = record.id
  noteRecord.documentType = record.documentType
  noteContent.value = record.remark || ''
  showNoteModal.value = true
}

async function saveNote() {
  try {
    const docType = noteRecord.documentType
    let apiUrl = ''
    switch (docType) {
      case 'SALE_ORDER':
        apiUrl = `/sales/order/${noteRecord.id}/remark`
        break
      case 'OUTBOUND':
        apiUrl = `/sales/outbound/${noteRecord.id}/remark`
        break
      case 'RETURN':
        apiUrl = `/sales/return-doc/${noteRecord.id}/remark`
        break
      case 'EXCHANGE':
        apiUrl = `/sales/exchange/${noteRecord.id}/remark`
        break
      default:
        message.error('未知单据类型')
        return
    }
    await request.put(apiUrl, { remark: noteContent.value })
    message.success('备注保存成功')
    showNoteModal.value = false
    fetchData()
  } catch {
    message.error('备注保存失败')
  }
}

// ═══ 错误处理 ═══
const handleError = (e: Error) => {
  hasError.value = true
  console.error(e)
}

onMounted(() => {
  // 初始化默认快捷日期（本周）
  setQuickDate('thisWeek')
})
</script>

<style scoped>
/* ═══ 查询方案 ═══ */
.query-scheme-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
}

/* ═══ 快捷日期 ═══ */
.quick-dates :deep(.ant-btn-link) {
  color: #555;
  padding: 0 8px;
  height: 24px;
  line-height: 24px;
}
.quick-dates :deep(.ant-btn-primary) {
  color: #fff;
  background: #fa8c16;
  border-color: #fa8c16;
}
.quick-dates :deep(.ant-btn-primary:hover) {
  background: #fa8c16;
  border-color: #fa8c16;
}

/* ═══ 搜索区域 ═══ */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-container:not(:has([data-expanded])) > .search-grid { max-height: 80px; overflow: hidden; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; align-items: center; }
.search-field-item { display: flex; min-width: 0; }
.search-field-item :deep(.ant-input-wrapper),
.search-field-item :deep(.ant-input-affix-wrapper) { width: 100%; font-size: 13px; }
.search-field-item :deep(.ant-select) { width: 100%; }
.search-field-item :deep(.ant-select .ant-select-selector) { font-size: 13px; }
.search-field-item :deep(.ant-picker) { width: 100%; }
.search-select-wrap { display: flex; align-items: center; width: 100%; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; }
.search-select-wrap:hover { border-color: #4096ff; }
.search-select-label { font-size: 13px; color: rgba(0,0,0,0.65); white-space: nowrap; flex-shrink: 0; padding-left: 8px; }
.search-select-wrap :deep(.ant-select) { flex: 1; min-width: 0; }
.search-select-wrap :deep(.ant-select .ant-select-selector) { border: none !important; border-radius: 0 !important; box-shadow: none !important; padding-top: 0 !important; padding-bottom: 0 !important; display: flex; align-items: center; }
.search-action-item { flex-shrink: 0; }
.search-action-group {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
}
.search-action-group .search-field-item {
  width: auto;
  flex: 0 0 auto;
  margin-right: 4px;
}
.search-action-group .search-field-item:last-child {
  margin-right: 0;
}
.search-more-toggle {
  margin-top: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.search-more-toggle::before,
.search-more-toggle::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e8e8e8;
}
.search-more-toggle :deep(.ant-btn) {
  font-size: 13px;
}

/* ═══ 表格底部合计 ═══ */
.table-footer {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-top: none;
  font-size: 12px;
  color: #333;
}
.footer-label {
  font-weight: 600;
  min-width: 170px;
}
.footer-values {
  flex: 1;
  display: flex;
  gap: 16px;
}
</style>
