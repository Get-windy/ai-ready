<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- ═══ Tab栏 + 工具栏 ═══ -->
      <CategoryListLayout
        :tabs="tabs"
        :active-tab="activeTab"
        :show-category-panel="activeTab === 'detail'"
        :category-tree-data="categoryTreeData"
        :category-loading="categoryLoading"
        :category-title="'商品分类'"
        :category-editable="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
        @category-select="handleCategorySelect"
        @category-expand="handleCategoryExpand"
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
              <a-select-option value="">
                --查询方案--
              </a-select-option>
            </a-select>
            <a-button
              type="link"
              size="small"
              style="padding: 0 4px"
            >
              <PlusOutlined />
            </a-button>
          </div>
          <a-space
            :size="4"
            class="quick-dates"
          >
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

        <!-- ═══ 工具栏右侧：操作按钮（受页面配置「功能按钮」开关控制） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip v-if="isButtonEnabled('config')" title="页面配置" placement="bottom">
              <a-button
                size="small"
                @click="showPageConfig = true"
              >
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <!-- 按钮权限：与后端 @SaCheckPermission("sale:outbound:*") 同一套码。
                 「商品汇总 / 物流备注」在码族里没有对应项，保持不拦（避免误伤）。 -->
            <a-button
              v-if="isButtonEnabled('add')"
              v-permission="'sale:outbound:create'"
              type="primary"
              size="small"
              @click="handleAdd"
            >
              <PlusOutlined /> 新增
            </a-button>
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              @click="fetchData"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="isButtonEnabled('batchPrint')"
              v-permission="'sale:outbound:print'"
              size="small"
              @click="handleBatchPrint"
            >
              <PrinterOutlined /> 批量打印
            </a-button>
            <a-tooltip v-if="isButtonEnabled('productSummary')" title="商品汇总" placement="bottom">
              <a-button size="small" @click="handleProductSummary">
                <BarChartOutlined /> 商品汇总
              </a-button>
            </a-tooltip>
            <a-dropdown>
              <a-button size="small">
                更多 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item
                    v-if="isButtonEnabled('batchImport')"
                    v-permission="'sale:outbound:import'"
                    key="batch-import"
                    @click="handleBatchImport"
                  >
                    批量导入
                  </a-menu-item>
                  <a-menu-item
                    v-if="isButtonEnabled('logisticsRemark')"
                    key="logistics-remark"
                    @click="handleLogisticsRemark"
                  >
                    物流备注
                  </a-menu-item>
                  <a-menu-item
                    v-if="isButtonEnabled('printF8')"
                    v-permission="'sale:outbound:print'"
                    key="print-f8"
                    @click="handlePrintF8"
                  >
                    打印(F8)
                  </a-menu-item>
                  <a-menu-item
                    v-if="isButtonEnabled('export')"
                    v-permission="'sale:outbound:export'"
                    key="export"
                    @click="handleExport"
                  >
                    导出
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>

        <!-- ═══ 搜索区域（按单据 40 项 / 按明细 18 项查询条件，超出折叠「更多条件」） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <!-- 按单据 Tab 搜索行 -->
            <template v-if="activeTab === 'doc'">
              <div class="search-container">
                <div ref="docGridRef" class="search-grid">
                  <!-- 日期：始终显示在首位 -->
                  <div class="search-field-item">
                    <a-range-picker
                      v-model:value="dateRange"
                      size="small"
                      style="width: 100%"
                      @change="handleDateChange"
                    />
                  </div>
                  <template v-for="f in docRenderFields" :key="f.key">
                    <div class="search-field-item">
                      <div v-if="f.type === 'select'" class="search-select-wrap">
                        <span class="search-select-label">{{ f.label }}</span>
                        <a-select
                          v-model:value="searchParams[f.key]"
                          size="small"
                          allow-clear
                          placeholder="全部"
                        >
                          <a-select-option value="">
                            全部
                          </a-select-option>
                          <a-select-option
                            v-for="opt in f.options"
                            :key="String(opt.value)"
                            :value="opt.value"
                          >
                            {{ opt.label }}
                          </a-select-option>
                        </a-select>
                      </div>
                      <a-input-number
                        v-else-if="f.type === 'number'"
                        v-model:value="searchParams[f.key]"
                        size="small"
                        style="width: 100%"
                        :placeholder="f.label"
                      />
                      <a-input
                        v-else
                        v-model:value="searchParams[f.key]"
                        :placeholder="f.label"
                        allow-clear
                        size="small"
                      />
                    </div>
                  </template>
                  <div class="search-action-group" ref="docActionRef" :style="{ gridColumn: 'span ' + docActionSpan }">
                    <div class="search-field-item search-action-item">
                      <a-button
                        type="primary"
                        size="small"
                        @click="handleSearch"
                      >
                        查询
                      </a-button>
                    </div>
                    <div class="search-field-item">
                      <a-checkbox v-model:checked="searchParams.showRed">
                        显示红冲
                      </a-checkbox>
                    </div>
                    <div class="search-field-item">
                      <a-checkbox v-model:checked="searchParams.showAbnormal">
                        仅显示异常记账单据
                      </a-checkbox>
                    </div>
                  </div>
                </div>
                <div v-if="hasMoreDocConditions" class="search-more-toggle">
                  <a-button
                    type="link"
                    size="small"
                    @click="showMoreDocConditions = !showMoreDocConditions"
                  >
                    {{ showMoreDocConditions ? '收起' : '更多条件' }}
                    <UpOutlined v-if="showMoreDocConditions" style="font-size: 10px; margin-left: 2px" />
                    <DownOutlined v-else style="font-size: 10px; margin-left: 2px" />
                  </a-button>
                </div>
              </div>
            </template>

            <!-- 按明细 Tab 搜索行 -->
            <template v-else>
              <div class="search-container">
                <div ref="detailGridRef" class="search-grid">
                  <div class="search-field-item">
                    <a-range-picker
                      v-model:value="dateRange"
                      size="small"
                      style="width: 100%"
                      @change="handleDateChange"
                    />
                  </div>
                  <template v-for="f in detailRenderFields" :key="f.key">
                    <div class="search-field-item">
                      <div v-if="f.type === 'select'" class="search-select-wrap">
                        <span class="search-select-label">{{ f.label }}</span>
                        <a-select
                          v-model:value="searchParams[f.key]"
                          size="small"
                          allow-clear
                          placeholder="全部"
                        >
                          <a-select-option value="">
                            全部
                          </a-select-option>
                          <a-select-option
                            v-for="opt in f.options"
                            :key="String(opt.value)"
                            :value="opt.value"
                          >
                            {{ opt.label }}
                          </a-select-option>
                        </a-select>
                      </div>
                      <a-input-number
                        v-else-if="f.type === 'number'"
                        v-model:value="searchParams[f.key]"
                        size="small"
                        style="width: 100%"
                        :placeholder="f.label"
                      />
                      <a-input
                        v-else
                        v-model:value="searchParams[f.key]"
                        :placeholder="f.label"
                        allow-clear
                        size="small"
                      />
                    </div>
                  </template>
                  <div class="search-action-group" ref="detailActionRef" :style="{ gridColumn: 'span ' + detailActionSpan }">
                    <div class="search-field-item search-action-item">
                      <a-button
                        type="primary"
                        size="small"
                        @click="handleSearch"
                      >
                        查询
                      </a-button>
                    </div>
                    <div class="search-field-item">
                      <a-checkbox v-model:checked="searchParams.showRed">
                        显示红冲
                      </a-checkbox>
                    </div>
                  </div>
                </div>
                <div v-if="hasMoreDetailConditions" class="search-more-toggle">
                  <a-button
                    type="link"
                    size="small"
                    @click="showMoreDetailConditions = !showMoreDetailConditions"
                  >
                    {{ showMoreDetailConditions ? '收起' : '更多条件' }}
                    <UpOutlined v-if="showMoreDetailConditions" style="font-size: 10px; margin-left: 2px" />
                    <DownOutlined v-else style="font-size: 10px; margin-left: 2px" />
                  </a-button>
                </div>
              </div>
            </template>
          </div>
        </template>

        <!-- ═══ 表格区域 ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="currentColumns"
              :storage-key="activeTab === 'doc' ? 'sale-outbound-table-columns-doc' : 'sale-outbound-table-columns-detail'"
              :global-config-key="activeTab === 'doc' ? 'sale-outbound-columns-doc' : 'sale-outbound-columns-detail'"
              :data-source="tableData"
              :loading="loading"
              :pagination="billPagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="true"
              :summary-columns="tableFooterColumns"
              row-key="id"
              @page-change="handlePageChange"
              @selection-change="handleSelectionChange"
            >
              <!-- 单据编号 -->
              <template #outboundNoCell="{ record }">
                <a-button
                  type="link"
                  size="small"
                  @click="handleView(record)"
                >
                  {{ record.outboundNo }}
                </a-button>
              </template>
              <!-- 单据状态 -->
              <template #statusCell="{ record }">
                <a-tag :color="getStatusColor(record.status)">
                  {{ getStatusText(record.status) }}
                </a-tag>
              </template>
              <!-- 金额 -->
              <template #totalAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.totalAmount) }}</span>
              </template>
              <!-- 已结金额 -->
              <template #settledAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.settledAmount) }}</span>
              </template>
              <!-- 操作列 -->
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button
                    type="link"
                    size="small"
                    @click="handleView(record)"
                  >
                    审核
                  </a-button>
                  <a-button
                    v-if="record.status === 0"
                    type="link"
                    size="small"
                    @click="handleEdit(record)"
                  >
                    修改
                  </a-button>
                  <a-button
                    v-if="record.status === 0"
                    type="link"
                    size="small"
                    danger
                    @click="handleDelete(record)"
                  >
                    删除
                  </a-button>
                  <a-dropdown v-if="record.status > 0">
                    <a-button
                      type="link"
                      size="small"
                    >
                      更多
                    </a-button>
                    <template #overlay>
                      <a-menu>
                        <a-menu-item @click="handleEdit(record)">
                          修改
                        </a-menu-item>
                        <a-menu-item @click="handleDelete(record)">
                          删除
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </a-space>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="activeQueryFields"
      :function-buttons-config="functionButtonConfig"
      :storage-key="activePageConfigStorageKey"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  SettingOutlined,
  BarChartOutlined,
  DownOutlined,
  UpOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { outboundApi } from '@/api/erp'
import { productCategoryApi } from '@/api/erp/product'
import { useRouter } from 'vue-router'

const router = useRouter()

// ═══ Tab 配置 ═══
const tabs = [
  { key: 'doc', label: '按单据' },
  { key: 'detail', label: '按明细' },
]
const activeTab = ref('doc')

// ═══ 快捷日期 ═══
const quickDates = [
  { key: 'yesterday', label: '昨日' },
  { key: 'today', label: '今日' },
  { key: 'week', label: '本周' },
  { key: 'lastWeek', label: '近一周' },
  { key: 'month', label: '本月' },
  { key: 'lastMonth', label: '上月' },
  { key: 'last3Month', label: '近三月' },
  { key: 'year', label: '本年' },
]
const quickDate = ref('week')

// ═══ 查询方案 ═══
const queryScheme = ref('')

// ═══ 状态 ═══
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])

const docGridRef = ref<HTMLElement | null>(null)
const docActionRef = ref<HTMLElement | null>(null)
const detailGridRef = ref<HTMLElement | null>(null)
const detailActionRef = ref<HTMLElement | null>(null)
const { span: docActionSpan } = useAutoGridSpan(docActionRef, docGridRef)
const { span: detailActionSpan } = useAutoGridSpan(detailActionRef, detailGridRef)

// ═══ 日期范围 ═══
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])

// ═══ 搜索参数（对标文档 40 项查询条件 + 按明细 18 项） ═══
const searchParams = reactive<Record<string, any>>({
  keyword: '',
  // 日期
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
  // 单据
  outboundNo: '',
  status: undefined,
  settlementStatus: '',
  settlementMethod: '',
  sourceOrder: '',
  generationMethod: '',
  outboundType: undefined,
  printCount: undefined,
  totalAmount: undefined,
  // 客户/组织
  customerName: '',
  salesPersonName: '',
  departmentName: '',
  warehouseName: '',
  region: '',
  // 备注/摘要
  remark: '',
  buyerRemark: '',
  summary: '',
  // 人员
  creatorName: '',
  auditorName: '',
  bookkeeperName: '',
  // 表头自定义字段
  extNum1: undefined,
  extNum2: undefined,
  extText1: '',
  extText2: '',
  extText3: '',
  // 收货
  receiverName: '',
  receiverPhone: '',
  shippingAddress: '',
  // 物流
  logisticsCompany: '',
  trackingNumber: '',
  deliveryMethod: '',
  deliveryDriver: '',
  // 收款账户
  paymentAccount1: '',
  paymentAccount2: '',
  paymentAccount3: '',
  paymentAccount4: '',
  // 勾选项
  showRed: false,
  showAbnormal: false,
  // 按明细 Tab
  productName: '',
  itemRemark: '',
  gift: undefined,
  promoProduct: undefined,
  productAttribute: undefined,
  // 商品分类树
  categoryId: '0',
})

// ═══ 查询条件渲染定义（key → 控件类型/选项） ═══
interface QueryFieldDef { label: string; type: 'input' | 'select' | 'number'; options?: { label: string; value: any }[] }

const STATUS_OPTIONS = [
  { label: '草稿', value: 0 },
  { label: '待审批', value: 1 },
  { label: '已审批', value: 2 },
  { label: '待拣货', value: 3 },
  { label: '拣货中', value: 4 },
  { label: '已拣货', value: 5 },
  { label: '待打包', value: 6 },
  { label: '打包中', value: 7 },
  { label: '已打包', value: 8 },
  { label: '待发货', value: 9 },
  { label: '已发货', value: 10 },
  { label: '已完成', value: 11 },
  { label: '已取消', value: 12 },
]

// 按单据 Tab：40 项查询条件的渲染器
const DOC_QUERY_FIELD_DEFS: Record<string, QueryFieldDef> = {
  outboundNo: { label: '单据编号', type: 'input' },
  customerName: { label: '客户', type: 'input' },
  salesPersonName: { label: '经手人', type: 'input' },
  departmentName: { label: '部门', type: 'input' },
  warehouseName: { label: '仓库', type: 'input' },
  status: { label: '单据状态', type: 'select', options: STATUS_OPTIONS },
  settlementStatus: {
    label: '结算状态', type: 'select',
    options: [{ label: '未结算', value: 'unsettled' }, { label: '部分结算', value: 'partial' }, { label: '已结算', value: 'settled' }],
  },
  settlementMethod: {
    label: '结款方式', type: 'select',
    options: [
      { label: '现金', value: 'cash' }, { label: '转账', value: 'transfer' },
      { label: '微信', value: 'wechat' }, { label: '支付宝', value: 'alipay' },
      { label: '月结', value: 'monthly' }, { label: '货到付款', value: 'cod' },
    ],
  },
  sourceOrder: { label: '来源订单', type: 'input' },
  remark: { label: '单据备注', type: 'input' },
  buyerRemark: { label: '买家备注', type: 'input' },
  summary: { label: '摘要', type: 'input' },
  creatorName: { label: '制单人', type: 'input' },
  auditorName: { label: '审核人', type: 'input' },
  bookkeeperName: { label: '记账人', type: 'input' },
  extNum1: { label: '表头自定义字段1', type: 'number' },
  extNum2: { label: '表头自定义字段2', type: 'number' },
  extText1: { label: '表头自定义字段3', type: 'input' },
  extText2: { label: '表头自定义字段4', type: 'input' },
  extText3: { label: '表头自定义字段5', type: 'input' },
  receiverName: { label: '收货人', type: 'input' },
  receiverPhone: { label: '联系电话', type: 'input' },
  shippingAddress: { label: '收货地址', type: 'input' },
  logisticsCompany: { label: '物流公司', type: 'input' },
  trackingNumber: { label: '运单号', type: 'input' },
  paymentAccount1: { label: '收款账户1', type: 'input' },
  paymentAccount2: { label: '收款账户2', type: 'input' },
  paymentAccount3: { label: '收款账户3', type: 'input' },
  paymentAccount4: { label: '收款账户4', type: 'input' },
  region: { label: '区域', type: 'input' },
  generationMethod: {
    label: '产生方式', type: 'select',
    options: [
      { label: '手工创建', value: '手工创建' }, { label: '订单生成', value: '订单生成' },
      { label: '复制', value: '复制' }, { label: '导入', value: '导入' },
    ],
  },
  outboundType: {
    label: '销售类型', type: 'select',
    options: [
      { label: '正常销售', value: 0 }, { label: '换货', value: 1 },
      { label: '调拨', value: 2 }, { label: '其他', value: 3 },
    ],
  },
  productAttribute: {
    label: '商品行属性', type: 'select',
    options: [{ label: '正常', value: '正常' }, { label: '赠品', value: '赠品' }, { label: '促销', value: '促销' }],
  },
  deliveryMethod: {
    label: '配送方式', type: 'select',
    options: [
      { label: '自提', value: 'self' }, { label: '快递', value: 'express' },
      { label: '物流', value: 'logistics' }, { label: '配送', value: 'delivery' },
    ],
  },
  deliveryDriver: { label: '配送司机', type: 'input' },
  printCount: { label: '打印次数', type: 'number' },
  totalAmount: { label: '本单金额', type: 'number' },
}

// 按明细 Tab：18 项查询条件的渲染器
const DETAIL_QUERY_FIELD_DEFS: Record<string, QueryFieldDef> = {
  outboundNo: { label: '单据编号', type: 'input' },
  productName: { label: '商品', type: 'input' },
  customerName: { label: '客户', type: 'input' },
  salesPersonName: { label: '经手人', type: 'input' },
  departmentName: { label: '部门', type: 'input' },
  creatorName: { label: '制单人', type: 'input' },
  auditorName: { label: '审核人', type: 'input' },
  bookkeeperName: { label: '记账人', type: 'input' },
  warehouseName: { label: '仓库', type: 'input' },
  status: { label: '单据状态', type: 'select', options: STATUS_OPTIONS },
  settlementStatus: DOC_QUERY_FIELD_DEFS.settlementStatus,
  sourceOrder: { label: '来源订单', type: 'input' },
  remark: { label: '单据备注', type: 'input' },
  itemRemark: { label: '明细备注', type: 'input' },
  gift: { label: '是否赠品', type: 'select', options: [{ label: '是', value: true }, { label: '否', value: false }] },
  promoProduct: { label: '促销商品', type: 'select', options: [{ label: '是', value: true }, { label: '否', value: false }] },
}

// ═══ 折叠：默认显示 2 排，其余收进「更多条件」 ═══
const DEFAULT_VISIBLE_COUNT = 8
const showMoreDocConditions = ref(false)
const showMoreDetailConditions = ref(false)

/** 按配置顺序（支持拖拽排序）生成渲染列表，并应用显隐 + 折叠 */
function buildRenderFields(
  config: QueryFieldSetting[],
  defs: Record<string, QueryFieldDef>,
  expanded: boolean
): (QueryFieldDef & { key: string })[] {
  const visible = config
    .filter(f => f.key !== 'date' && f.visible && defs[f.key])
    .map(f => ({ key: f.key, ...defs[f.key] }))
  return expanded ? visible : visible.slice(0, DEFAULT_VISIBLE_COUNT)
}

const docRenderFields = computed(() => buildRenderFields(docQueryConfig.value, DOC_QUERY_FIELD_DEFS, showMoreDocConditions.value))
const detailRenderFields = computed(() => buildRenderFields(detailQueryConfig.value, DETAIL_QUERY_FIELD_DEFS, showMoreDetailConditions.value))

const docVisibleFieldCount = computed(() => docQueryConfig.value.filter(f => f.key !== 'date' && f.visible && DOC_QUERY_FIELD_DEFS[f.key]).length)
const detailVisibleFieldCount = computed(() => detailQueryConfig.value.filter(f => f.key !== 'date' && f.visible && DETAIL_QUERY_FIELD_DEFS[f.key]).length)
const hasMoreDocConditions = computed(() => docVisibleFieldCount.value > DEFAULT_VISIBLE_COUNT)
const hasMoreDetailConditions = computed(() => detailVisibleFieldCount.value > DEFAULT_VISIBLE_COUNT)

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

// ═══ 多选状态（由 BillTableList 的 selection-change 事件驱动） ═══
const selectedRowKeys = ref<any[]>([])

function handleSelectionChange(_rows: any[], ids: any[]) {
  selectedRowKeys.value = ids || []
}

const showPageConfig = ref(false)

// ═══ 页面配置（查询条件显隐、功能按钮、打印配置） ═══
const PAGE_CONFIG_STORAGE_KEY = 'sale-outbound-page-config'

interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PrintConfigSetting { alwaysLastTemplate: boolean; linkReturnApply: boolean }

// 默认查询字段 - 按单据tab（对标开发文档 40 项查询条件）
const DEFAULT_DOC_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '日期', visible: true },
  { key: 'outboundNo', label: '单据编号', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'salesPersonName', label: '经手人', visible: true },
  { key: 'departmentName', label: '部门', visible: true },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'settlementStatus', label: '结算状态', visible: true },
  { key: 'settlementMethod', label: '结款方式', visible: true },
  { key: 'sourceOrder', label: '来源订单', visible: true },
  { key: 'remark', label: '单据备注', visible: true },
  { key: 'buyerRemark', label: '买家备注', visible: false },
  { key: 'summary', label: '摘要', visible: false },
  { key: 'creatorName', label: '制单人', visible: false },
  { key: 'auditorName', label: '审核人', visible: false },
  { key: 'bookkeeperName', label: '记账人', visible: false },
  { key: 'extNum1', label: '表头自定义字段1(数字)', visible: false },
  { key: 'extNum2', label: '表头自定义字段2(数字)', visible: false },
  { key: 'extText1', label: '表头自定义字段3(文本)', visible: false },
  { key: 'extText2', label: '表头自定义字段4(文本)', visible: false },
  { key: 'extText3', label: '表头自定义字段5(文本)', visible: false },
  { key: 'receiverName', label: '收货人', visible: false },
  { key: 'receiverPhone', label: '联系电话', visible: false },
  { key: 'shippingAddress', label: '收货地址', visible: false },
  { key: 'logisticsCompany', label: '物流公司', visible: false },
  { key: 'trackingNumber', label: '运单号', visible: false },
  { key: 'paymentAccount1', label: '收款账户1', visible: false },
  { key: 'paymentAccount2', label: '收款账户2', visible: false },
  { key: 'paymentAccount3', label: '收款账户3', visible: false },
  { key: 'paymentAccount4', label: '收款账户4', visible: false },
  { key: 'region', label: '区域', visible: false },
  { key: 'generationMethod', label: '产生方式', visible: false },
  { key: 'outboundType', label: '销售类型', visible: false },
  { key: 'productAttribute', label: '商品行属性', visible: false },
  { key: 'deliveryMethod', label: '配送方式', visible: false },
  { key: 'deliveryDriver', label: '配送司机', visible: false },
  { key: 'printCount', label: '打印次数', visible: false },
  { key: 'totalAmount', label: '本单金额', visible: false },
  { key: 'showRed', label: '显示红冲', visible: true },
  { key: 'showAbnormal', label: '仅显示异常记账单据', visible: false },
]

// 默认查询字段 - 按明细tab
const DEFAULT_DETAIL_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '日期', visible: true },
  { key: 'outboundNo', label: '单据编号', visible: true },
  { key: 'productName', label: '商品', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'salesPersonName', label: '经手人', visible: true },
  { key: 'departmentName', label: '部门', visible: true },
  { key: 'creatorName', label: '制单人', visible: false },
  { key: 'auditorName', label: '审核人', visible: false },
  { key: 'bookkeeperName', label: '记账人', visible: false },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'settlementStatus', label: '结算状态', visible: true },
  { key: 'sourceOrder', label: '来源订单', visible: true },
  { key: 'remark', label: '单据备注', visible: false },
  { key: 'itemRemark', label: '明细备注', visible: false },
  { key: 'gift', label: '是否赠品', visible: false },
  { key: 'promoProduct', label: '促销商品', visible: false },
  { key: 'showRed', label: '显示红冲', visible: true },
]

// 默认功能按钮
// 注：「电子面单」入口已下线——全系统无任何快递/物流面单（运单号获取）服务端能力，
//     该入口此前仅弹「对接中」提示属假成功桩；对标文档裁决为「接面单 API 或下线入口」，此处选择下线。
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'batchPrint', label: '批量打印', enabled: true },
  { key: 'productSummary', label: '商品汇总', enabled: true },
  { key: 'batchImport', label: '批量导入', enabled: true },
  { key: 'logisticsRemark', label: '物流备注', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]

const DEFAULT_PRINT_CONFIG: PrintConfigSetting = {
  alwaysLastTemplate: false,
  linkReturnApply: false,
}

// 当前tab的查询字段配置（按单据tab）
const docQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_DOC_QUERY_FIELDS.map(f => ({ ...f })))
// 当前tab的查询字段配置（按明细tab）
const detailQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_DETAIL_QUERY_FIELDS.map(f => ({ ...f })))
// 功能按钮配置
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))
// 打印配置
const printConfig = reactive<PrintConfigSetting>({ ...DEFAULT_PRINT_CONFIG })

// 当前活动tab对应的查询字段和存储键
const activeQueryFields = computed(() =>
  activeTab.value === 'doc' ? docQueryConfig.value : detailQueryConfig.value
)
const activePageConfigStorageKey = computed(() =>
  `${PAGE_CONFIG_STORAGE_KEY}-${activeTab.value}`
)

/** 功能按钮 / 打印配置为全局共享（非 Tab 维度），固定存这个键，避免只写进当前 Tab 的键而另一 Tab 读不到 */
const SHARED_CONFIG_STORAGE_KEY = `${PAGE_CONFIG_STORAGE_KEY}-shared`
const tabStorageKey = (tab: 'doc' | 'detail') => `${PAGE_CONFIG_STORAGE_KEY}-${tab}`

/** 合并「默认查询字段」与已存配置：保留显隐，并应用配置面板的拖拽排序 */
function applyQueryConfig(defaults: QueryFieldSetting[], saved: any): QueryFieldSetting[] {
  if (!Array.isArray(saved) || saved.length === 0) {
    return defaults.map(f => ({ ...f }))
  }
  const merged = defaults.map(df => {
    const s = saved.find((f: QueryFieldSetting) => f.key === df.key)
    return s ? { ...df, visible: s.visible !== false } : { ...df }
  })
  const ordered = saved.map((s: QueryFieldSetting) => merged.find(m => m.key === s.key)).filter(Boolean)
  return ordered.length === merged.length ? (ordered as QueryFieldSetting[]) : merged
}

function parseStored(key: string): any {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function loadPageConfig() {
  const docParsed = parseStored(tabStorageKey('doc'))
  const detailParsed = parseStored(tabStorageKey('detail'))
  docQueryConfig.value = applyQueryConfig(DEFAULT_DOC_QUERY_FIELDS, docParsed?.queryFields)
  detailQueryConfig.value = applyQueryConfig(DEFAULT_DETAIL_QUERY_FIELDS, detailParsed?.queryFields)

  // 共享配置：优先读固定共享键，兼容历史按 Tab 存储
  const shared = parseStored(SHARED_CONFIG_STORAGE_KEY) || docParsed || detailParsed
  if (Array.isArray(shared?.functionButtons)) {
    functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
      const s = shared.functionButtons.find((f: FunctionButtonSetting) => f.key === bf.key)
      return s ? { ...bf, enabled: s.enabled } : { ...bf }
    })
  }
  if (shared?.printConfig) {
    Object.assign(printConfig, { ...DEFAULT_PRINT_CONFIG, ...shared.printConfig })
  }
}

/** 页面配置变更：查询条件按 Tab 分开存，功能按钮/打印配置写共享键（三处键互不覆盖） */
function handlePageConfigChange(config: any) {
  const tab: 'doc' | 'detail' = activeTab.value === 'doc' ? 'doc' : 'detail'
  if (Array.isArray(config.queryFields)) {
    const defaults = tab === 'doc' ? DEFAULT_DOC_QUERY_FIELDS : DEFAULT_DETAIL_QUERY_FIELDS
    const merged = applyQueryConfig(defaults, config.queryFields)
    if (tab === 'doc') {
      docQueryConfig.value = merged
    } else {
      detailQueryConfig.value = merged
    }
    localStorage.setItem(tabStorageKey(tab), JSON.stringify({ queryFields: config.queryFields }))
  }
  if (Array.isArray(config.functionButtons)) {
    functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
      const s = config.functionButtons.find((f: FunctionButtonSetting) => f.key === bf.key)
      return s ? { ...bf, enabled: s.enabled } : { ...bf }
    })
  }
  if (config.printConfig) {
    Object.assign(printConfig, { ...DEFAULT_PRINT_CONFIG, ...config.printConfig })
  }
  localStorage.setItem(SHARED_CONFIG_STORAGE_KEY, JSON.stringify({
    functionButtons: functionButtonConfig.value,
    printConfig: { ...printConfig },
  }))
}

// 判断功能按钮是否启用（真实参与工具栏渲染）
function isButtonEnabled(key: string): boolean {
  const btn = functionButtonConfig.value.find(b => b.key === key)
  return btn ? btn.enabled : true
}

// ═══ 列定义 ═══

/** 按单据 Tab 列 */
const docColumns = [
  // 行选择框：批量打印 / 物流备注 / 导出选中 等批量操作依赖选中行
  { title: '', key: 'checkbox', type: 'checkbox', width: 40, fixed: 'left' },
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 160, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'outboundDate', key: 'outboundDate', width: 110, sortable: true },
  { title: '单据编号', field: 'outboundNo', key: 'outboundNo', width: 170, type: 'slot', slotName: 'outboundNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '来源订单', field: 'orderNo', key: 'orderNo', width: 150, sortable: true },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 120, sortable: true },
  { title: '客户', field: 'customerName', key: 'customerName', width: 200, sortable: true },
  { title: '结款方式', field: 'settlementMethod', key: 'settlementMethod', width: 80, sortable: true },
  { title: '客户编号', field: 'customerCode', key: 'customerCode', width: 100 },
  { title: '客户级别', field: 'customerLevel', key: 'customerLevel', width: 80 },
  { title: '收货人', field: 'receiverName', key: 'receiverName', width: 100 },
  { title: '联系电话', field: 'receiverPhone', key: 'receiverPhone', width: 120 },
  { title: '收货地址', field: 'shippingAddress', key: 'shippingAddress', width: 200 },
  { title: '客户一票通', field: 'customerTicket', key: 'customerTicket', width: 100 },
  { title: '客户备注', field: 'customerRemark', key: 'customerRemark', width: 150 },
  { title: '经手人', field: 'salesPersonName', key: 'salesPersonName', width: 80, sortable: true },
  { title: '部门', field: 'departmentName', key: 'departmentName', width: 100 },
  { title: '商品金额', field: 'totalAmount', key: 'totalAmount', width: 120, align: 'right', type: 'slot', slotName: 'totalAmountCell', sortable: true },
  { title: '促销优惠', field: 'promoDiscount', key: 'promoDiscount', width: 100, align: 'right' },
  { title: '优惠劵', field: 'couponAmount', key: 'couponAmount', width: 100, align: 'right' },
  { title: '直接优惠', field: 'directDiscount', key: 'directDiscount', width: 100, align: 'right' },
  { title: '运费承担方', field: 'freightPayer', key: 'freightPayer', width: 90 },
  { title: '运费', field: 'freight', key: 'freight', width: 80, align: 'right' },
  { title: '其他费用', field: 'otherFee', key: 'otherFee', width: 100, align: 'right' },
  { title: '抹零金额', field: 'roundingAmount', key: 'roundingAmount', width: 90, align: 'right' },
  { title: '本单金额', field: 'totalAmount', key: 'totalAmountFinal', width: 120, align: 'right', type: 'slot', slotName: 'totalAmountCell', sortable: true },
  { title: '金额', field: 'amount', key: 'amount', width: 120, align: 'right' },
  { title: '折后金额', field: 'discountedAmount', key: 'discountedAmount', width: 120, align: 'right' },
  { title: '销售收入', field: 'salesRevenue', key: 'salesRevenue', width: 120, align: 'right' },
  { title: '已结金额', field: 'settledAmount', key: 'settledAmount', width: 100, align: 'right', type: 'slot', slotName: 'settledAmountCell', sortable: true },
  { title: '结算状态', field: 'settlementStatus', key: 'settlementStatus', width: 100, sortable: true },
  { title: '数量', field: 'totalQuantity', key: 'totalQuantity', width: 80, align: 'right', sortable: true },
  { title: '销售类型', field: 'outboundType', key: 'outboundType', width: 80 },
  { title: '配送方式', field: 'deliveryMethod', key: 'deliveryMethod', width: 100 },
  { title: '重量（kg）', field: 'totalWeight', key: 'totalWeight', width: 90, align: 'right' },
  { title: '体积（m³）', field: 'totalVolume', key: 'totalVolume', width: 90, align: 'right' },
  { title: '退货数量', field: 'returnQuantity', key: 'returnQuantity', width: 100, align: 'right' },
  { title: '退货金额', field: 'returnAmount', key: 'returnAmount', width: 100, align: 'right' },
  { title: '单据备注', field: 'remark', key: 'remark', width: 120 },
  { title: '买家备注', field: 'buyerRemark', key: 'buyerRemark', width: 120 },
  { title: '摘要', field: 'summary', key: 'summary', width: 150 },
  { title: '附件', field: 'attachment', key: 'attachment', width: 80 },
  { title: '表头自定义字段1(数字)', field: 'extNum1', key: 'extNum1', width: 130 },
  { title: '表头自定义字段2(数字)', field: 'extNum2', key: 'extNum2', width: 130 },
  { title: '表头自定义字段3(文本)', field: 'extText1', key: 'extText1Doc', width: 130 },
  { title: '表头自定义字段4(文本)', field: 'extText2', key: 'extText2Doc', width: 130 },
  { title: '表头自定义字段5(文本)', field: 'extText3', key: 'extText3Doc', width: 130 },
  { title: '表尾自定义字段1(文本)', field: 'footerExtText1', key: 'footerExtText1', width: 130 },
  { title: '表尾自定义字段2(文本)', field: 'footerExtText2', key: 'footerExtText2', width: 130 },
  { title: '物流公司', field: 'logisticsCompany', key: 'logisticsCompany', width: 120 },
  { title: '运单号', field: 'trackingNumber', key: 'trackingNumber', width: 140 },
  { title: '物流网点', field: 'logisticsBranch', key: 'logisticsBranch', width: 120 },
  { title: '收款账户1', field: 'paymentAccount1', key: 'paymentAccount1', width: 150 },
  { title: '收款账户2', field: 'paymentAccount2', key: 'paymentAccount2', width: 150 },
  { title: '收款账户3', field: 'paymentAccount3', key: 'paymentAccount3', width: 150 },
  { title: '收款账户4', field: 'paymentAccount4', key: 'paymentAccount4', width: 150 },
  { title: '区域', field: 'region', key: 'region', width: 100 },
  { title: '产生方式', field: 'generationMethod', key: 'generationMethod', width: 100 },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 80, sortable: true },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 80, sortable: true },
  { title: '审核人', field: 'auditorName', key: 'auditorName', width: 80, sortable: true },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 140 },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 140 },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right' },
  { title: '打印时间', field: 'printTime', key: 'printTime', width: 140 },
  { title: '商品行数', field: 'boxCount', key: 'boxCount', width: 80, align: 'right' },
]

/** 按明细 Tab 列（对标文档 71 列） */
const detailColumns = [
  { title: '', key: 'checkbox', type: 'checkbox', width: 40, fixed: 'left' },
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 120, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'outboundDate', key: 'outboundDate', width: 110, sortable: true },
  { title: '单据编号', field: 'outboundNo', key: 'outboundNo', width: 170, type: 'slot', slotName: 'outboundNoCell', sortable: true },
  { title: '来源订单', field: 'orderNo', key: 'orderNo', width: 150, sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '结算状态', field: 'settlementStatus', key: 'settlementStatus', width: 100, sortable: true },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 120, sortable: true },
  { title: '客户', field: 'customerName', key: 'customerName', width: 160, sortable: true },
  { title: '客户编号', field: 'customerCode', key: 'customerCode', width: 100 },
  { title: '客户级别', field: 'customerLevel', key: 'customerLevel', width: 80 },
  { title: '收货人', field: 'receiverName', key: 'receiverName', width: 100 },
  { title: '联系电话', field: 'receiverPhone', key: 'receiverPhone', width: 120 },
  { title: '收货地址', field: 'shippingAddress', key: 'shippingAddress', width: 200 },
  { title: '客户一票通', field: 'customerTicket', key: 'customerTicket', width: 100 },
  { title: '客户备注', field: 'customerRemark', key: 'customerRemark', width: 150 },
  { title: '经手人', field: 'salesPersonName', key: 'salesPersonName', width: 80, sortable: true },
  { title: '部门', field: 'departmentName', key: 'departmentName', width: 100 },
  { title: '商品名称', field: 'productName', key: 'productName', width: 200, sortable: true },
  { title: '货号', field: 'productCode', key: 'productCode', width: 100 },
  { title: '条码', field: 'barcode', key: 'barcode', width: 110 },
  { title: '规格', field: 'specification', key: 'specification', width: 100 },
  { title: '型号', field: 'model', key: 'model', width: 80 },
  { title: '产地', field: 'origin', key: 'origin', width: 80 },
  { title: '品牌', field: 'brand', key: 'brand', width: 80 },
  // ── 表体自定义字段 1~10 ──
  { title: '表体自定义1', field: 'extNum1', key: 'extNum1', width: 120 },
  { title: '表体自定义2', field: 'extNum2', key: 'extNum2', width: 120 },
  { title: '表体自定义3', field: 'extNum3', key: 'extNum3', width: 120 },
  { title: '表体自定义4', field: 'extText1', key: 'extText1Detail', width: 120 },
  { title: '表体自定义5', field: 'extText2', key: 'extText2Detail', width: 120 },
  { title: '表体自定义6', field: 'extNum4', key: 'extNum4', width: 120 },
  { title: '表体自定义7', field: 'extNum5', key: 'extNum5', width: 120 },
  { title: '表体自定义8(往来单位)', field: 'extPartner', key: 'extPartner', width: 130 },
  { title: '表体自定义9(职员)', field: 'extStaff', key: 'extStaff', width: 120 },
  { title: '表体自定义10(部门)', field: 'extDept', key: 'extDept', width: 120 },
  { title: '单位', field: 'productUnit', key: 'productUnit', width: 80 },
  { title: '数量', field: 'quantity', key: 'quantity', width: 80, align: 'right', sortable: true },
  { title: '换算关系', field: 'conversionRelation', key: 'conversionRelation', width: 100 },
  { title: '换算结果', field: 'conversionResult', key: 'conversionResult', width: 100 },
  { title: '大包装', field: 'bigPack', key: 'bigPack', width: 70, align: 'right' },
  { title: '中包装', field: 'midPack', key: 'midPack', width: 70, align: 'right' },
  { title: '小包装', field: 'smallPack', key: 'smallPack', width: 70, align: 'right' },
  { title: '小单位', field: 'smallUnit', key: 'smallUnit', width: 70 },
  { title: '小单位数量', field: 'smallUnitQuantity', key: 'smallUnitQuantity', width: 90, align: 'right' },
  { title: '单价', field: 'unitPrice', key: 'unitPrice', width: 80, align: 'right' },
  { title: '小单位单价', field: 'smallUnitPrice', key: 'smallUnitPrice', width: 90, align: 'right' },
  { title: '金额', field: 'lineAmount', key: 'lineAmount', width: 100, align: 'right', sortable: true },
  { title: '折扣(%)', field: 'discountRate', key: 'discountRate', width: 70, align: 'right' },
  { title: '折后单价', field: 'discountedPrice', key: 'discountedPrice', width: 80, align: 'right' },
  { title: '折后金额', field: 'discountedAmount', key: 'discountedAmount', width: 100, align: 'right' },
  { title: '优惠折扣(%)', field: 'favorableDiscountRate', key: 'favorableDiscountRate', width: 90, align: 'right' },
  { title: '优惠后单价', field: 'favorableUnitPrice', key: 'favorableUnitPrice', width: 90, align: 'right' },
  { title: '优惠后金额', field: 'favorableAmount', key: 'favorableAmount', width: 100, align: 'right' },
  { title: '重量（kg）', field: 'weight', key: 'weight', width: 80, align: 'right' },
  { title: '体积（m³）', field: 'volume', key: 'volume', width: 80, align: 'right' },
  { title: '箱号', field: 'boxNo', key: 'boxNo', width: 80 },
  { title: '明细备注', field: 'remark', key: 'itemRemark', width: 120 },
  { title: '单据备注', field: 'docRemark', key: 'docRemark', width: 120 },
  { title: '摘要', field: 'summary', key: 'summary', width: 120 },
  { title: '附件', field: 'attachment', key: 'attachment', width: 80 },
  // ── 表头自定义字段 1~5 ──
  { title: '表头自定义字段1(数字)', field: 'headerExtNum1', key: 'headerExtNum1', width: 130 },
  { title: '表头自定义字段2(数字)', field: 'headerExtNum2', key: 'headerExtNum2', width: 130 },
  { title: '表头自定义字段3(文本)', field: 'headerExtText1', key: 'headerExtText1', width: 130 },
  { title: '表头自定义字段4(文本)', field: 'headerExtText2', key: 'headerExtText2', width: 130 },
  { title: '表头自定义字段5(文本)', field: 'headerExtText3', key: 'headerExtText3', width: 130 },
  // ── 表尾自定义字段 1~2 ──
  { title: '表尾自定义字段1(文本)', field: 'footerExtText1', key: 'footerExtText1', width: 130 },
  { title: '表尾自定义字段2(文本)', field: 'footerExtText2', key: 'footerExtText2', width: 130 },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 80, sortable: true },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 80, sortable: true },
  { title: '审核人', field: 'auditorName', key: 'auditorName', width: 80, sortable: true },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 140 },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 140 },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right' },
]

// ═══ 列配置走数据表表头齿轮（storage-key=sale-outbound-table-columns-doc/-detail） ═══
const currentColumns = computed(() => (activeTab.value === 'doc' ? docColumns : detailColumns))

// ═══ 商品分类树 ═══
const categoryTreeData = ref<any[]>([])
const categoryLoading = ref(false)
const selectedCategoryId = ref<string | number>('0')
const categoryExpandedKeys = ref<(string | number)[]>([])

// ══ 状态映射 ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'processing' },
  2: { text: '已审批', color: 'blue' },
  3: { text: '待拣货', color: 'cyan' },
  4: { text: '拣货中', color: 'processing' },
  5: { text: '已拣货', color: 'cyan' },
  6: { text: '待打包', color: 'orange' },
  7: { text: '打包中', color: 'processing' },
  8: { text: '已打包', color: 'orange' },
  9: { text: '待发货', color: 'geekblue' },
  10: { text: '已发货', color: 'geekblue' },
  11: { text: '已完成', color: 'green' },
  12: { text: '已取消', color: 'red' },
}

function getStatusText(status: number): string {
  return STATUS_MAP[status]?.text || '未知'
}

function getStatusColor(status: number): string {
  return STATUS_MAP[status]?.color || 'default'
}

// ═══ 表格底部合计 ═══
const tableFooterColumns = computed(() => {
  if (activeTab.value !== 'doc') return []
  const totalAmount = tableData.value.reduce((s: number, r: any) => s + (r.totalAmount || 0), 0)
  const settledAmount = tableData.value.reduce((s: number, r: any) => s + (r.settledAmount || 0), 0)
  return [
    { key: 'totalAmount', value: totalAmount, highlight: true },
    { key: 'settledAmount', value: settledAmount, highlight: true },
  ]
})

// ═══ 数据加载 ═══

/** 参与后端筛选的查询项（DTO 字段名） */
const QUERY_PARAM_KEYS = [
  'outboundNo', 'status', 'settlementStatus', 'settlementMethod', 'sourceOrder', 'generationMethod',
  'outboundType', 'printCount', 'totalAmount',
  'customerName', 'salesPersonName', 'departmentName', 'warehouseName', 'region',
  'remark', 'buyerRemark', 'summary', 'creatorName', 'auditorName', 'bookkeeperName',
  'extNum1', 'extNum2', 'extText1', 'extText2', 'extText3',
  'receiverName', 'receiverPhone', 'shippingAddress',
  'logisticsCompany', 'trackingNumber', 'deliveryMethod', 'deliveryDriver',
  'paymentAccount1', 'paymentAccount2', 'paymentAccount3', 'paymentAccount4',
  'showAbnormal',
  // 按明细 Tab
  'productName', 'itemRemark', 'gift', 'promoProduct', 'productAttribute',
] as const

/** 组装查询参数：仅提交有值项，避免空串覆盖后端默认口径 */
function buildQueryParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  for (const key of QUERY_PARAM_KEYS) {
    const v = searchParams[key]
    if (v === undefined || v === null || v === '') continue
    // 布尔筛选（是否赠品/促销商品）仅在选中时提交
    if (v === false && (key === 'gift' || key === 'promoProduct' || key === 'showAbnormal')) continue
    params[key] = v
  }
  if (searchParams.keyword) params.keyword = searchParams.keyword
  if (searchParams.startDate) params.dateStart = searchParams.startDate
  if (searchParams.endDate) params.dateEnd = searchParams.endDate
  params.showRed = !!searchParams.showRed
  if (activeTab.value === 'detail' && searchParams.categoryId && searchParams.categoryId !== '0') {
    params.categoryId = searchParams.categoryId
  }
  return params
}

async function fetchData() {
  loading.value = true
  hasError.value = false
  try {
    const params = buildQueryParams()
    const res = activeTab.value === 'detail'
      ? await outboundApi.pageDetail(params)
      : await outboundApi.page(params)
    if (res) {
      tableData.value = res.records || []
      pagination.total = Number(res.total) || 0
    }
  } catch (error: any) {
    hasError.value = true
    console.warn('[销售出库单] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

async function loadCategoryTree() {
  categoryLoading.value = true
  try {
    const tree = await productCategoryApi.getTree()
    categoryTreeData.value = tree || []
    if (categoryTreeData.value.length > 0) {
      categoryExpandedKeys.value = [categoryTreeData.value[0].id]
    }
  } catch {
    categoryTreeData.value = []
  } finally {
    categoryLoading.value = false
  }
}

// ═══ 事件处理 ═══

function handleTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
  fetchData()
}

function setQuickDate(key: string) {
  quickDate.value = key
  const now = dayjs()
  let start: Dayjs, end: Dayjs
  switch (key) {
    case 'yesterday':
      start = now.subtract(1, 'day'); end = now.subtract(1, 'day'); break
    case 'today':
      start = now; end = now; break
    case 'week':
      start = now.startOf('week'); end = now; break
    case 'lastWeek':
      start = now.subtract(7, 'day'); end = now; break
    case 'month':
      start = now.startOf('month'); end = now; break
    case 'lastMonth':
      start = now.subtract(1, 'month').startOf('month'); end = now.subtract(1, 'month').endOf('month'); break
    case 'last3Month':
      start = now.subtract(3, 'month'); end = now; break
    case 'year':
      start = now.startOf('year'); end = now; break
    default:
      start = now.subtract(7, 'day'); end = now
  }
  dateRange.value = [start, end]
  searchParams.startDate = start.format('YYYY-MM-DD')
  searchParams.endDate = end.format('YYYY-MM-DD')
  handleSearch()
}

function handleDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates && dates.length === 2) {
    searchParams.startDate = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.endDate = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    searchParams.startDate = ''
    searchParams.endDate = ''
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

function handleCategorySelect(keys: (string | number)[]) {
  if (keys.length > 0) {
    selectedCategoryId.value = keys[0]
  } else {
    selectedCategoryId.value = '0'
  }
  // 分类树筛选真实下推到后端（按明细 Tab）
  searchParams.categoryId = String(selectedCategoryId.value)
  pagination.current = 1
  fetchData()
}

function handleCategoryExpand(keys: (string | number)[]) {
  categoryExpandedKeys.value = keys
}

// ═══ 操作 ═══

function handleAdd() {
  // 走菜单注册的表单路由（/sales/outbound/create 未在动态路由中注册，会落到 404）
  router.push('/sales/outbound/form')
}

function handleView(record: any) {
  router.push(`/sales/outbound/form/${record.id}`)
}

function handleEdit(record: any) {
  router.push(`/sales/outbound/form/${record.id}`)
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除出库单 ${record.outboundNo} 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await outboundApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch (error: any) {
        console.warn('[销售出库单] 删除失败', error)
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

const handleError = (error: Error) => {
  hasError.value = true
  console.error('[销售出库单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

function formatAmount(amount: number): string {
  if (!amount) return '0.00'
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 功能按钮事件 ═══

function handleBatchPrint() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要打印的出库单')
    return
  }
  // 打印配置：勾选「始终使用最后一次打印的模板」后不再让操作员选择模板
  const skipTemplateSelect = printConfig.alwaysLastTemplate
  Modal.confirm({
    title: '批量打印',
    content: skipTemplateSelect
      ? `将使用上次打印模板打印选中的 ${selectedRowKeys.value.length} 条出库单，确认继续？`
      : `确定要打印选中的 ${selectedRowKeys.value.length} 条出库单吗？`,
    onOk: async () => {
      try {
        await outboundApi.batchPrint(selectedRowKeys.value)
        message.success('打印任务已提交')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '批量打印失败')
      }
    },
  })
}

function handleProductSummary() {
  // 商品汇总：按当前查询条件取「按明细」数据按商品维度聚合（真实明细行，非单据行）
  const params = buildQueryParams()
  params.pageNum = 1
  params.pageSize = 9999

  outboundApi.pageDetail(params).then((res: any) => {
    const records = res?.records || []
    if (records.length === 0) {
      message.info('当前查询条件下无数据')
      return
    }
    const productMap = new Map<string, { name: string; code: string; totalQty: number; totalAmt: number }>()
    for (const item of records) {
      const key = item.productName || item.productCode || '未知商品'
      const existing = productMap.get(key) || { name: item.productName || '', code: item.productCode || '', totalQty: 0, totalAmt: 0 }
      existing.totalQty += Number(item.quantity || 0)
      existing.totalAmt += Number(item.lineAmount || 0)
      productMap.set(key, existing)
    }
    const summaryLines = Array.from(productMap.values())
      .sort((a, b) => b.totalAmt - a.totalAmt)
      .map(p => `${p.name}(${p.code || '-'}): ${p.totalQty}件 / ¥${p.totalAmt.toFixed(2)}`)
      .join('\n')
    Modal.info({
      title: '商品汇总',
      content: h('pre', { style: 'max-height:400px;overflow:auto;font-size:13px;line-height:1.8' }, summaryLines || '无汇总数据'),
      width: 500,
    })
  }).catch(() => {
    message.error('获取商品汇总失败')
  })
}

function handleBatchImport() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.xlsx,.xls,.csv'
  input.onchange = async (e: any) => {
    const file = e.target.files[0]
    if (!file) return
    try {
      const res = await outboundApi.batchImport(file)
      if (res?.success) {
        message.success(res.message || '导入成功')
        fetchData()
      } else {
        message.error(res?.message || '导入失败')
      }
    } catch (error: any) {
      message.error(error?.response?.data?.message || '批量导入失败')
    }
  }
  input.click()
}

function handlePrintF8() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要打印的出库单')
    return
  }
  handleBatchPrint()
}

/** 导出：调用后端真实 Excel 流（application/vnd.openxmlformats-officedocument.spreadsheetml.sheet） */
async function handleExport() {
  try {
    const params = buildQueryParams()
    delete params.pageNum
    delete params.pageSize
    await outboundApi.exportExcel(params)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  }
}

/** 物流备注：收集文本后真实批量写入 erp_sale_outbound.logistics_remark */
function handleLogisticsRemark() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择出库单')
    return
  }
  const remarkRef = { value: '' }
  Modal.confirm({
    title: '物流备注',
    width: 480,
    content: h('div', [
      h('p', `已选择 ${selectedRowKeys.value.length} 张出库单`),
      h('textarea', {
        placeholder: '请输入物流备注内容...',
        rows: 3,
        style: 'width:100%;margin-top:8px;padding:4px 8px;border:1px solid #d9d9d9;border-radius:4px;resize:vertical',
        onInput: (e: any) => { remarkRef.value = e.target.value },
      }),
    ]),
    onOk: async () => {
      const logisticsRemark = remarkRef.value.trim()
      if (!logisticsRemark) {
        message.warning('请输入物流备注内容')
        return Promise.reject()
      }
      try {
        const res: any = await outboundApi.batchLogisticsRemark(selectedRowKeys.value, logisticsRemark)
        message.success(`物流备注已写入 ${res?.updated ?? selectedRowKeys.value.length} 张出库单`)
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '物流备注写入失败')
        return Promise.reject()
      }
      return undefined
    },
  })
}

// ═══ 初始化 ═══
onMounted(() => {
  loadPageConfig()
  setQuickDate('lastWeek')
  fetchData()
  loadCategoryTree()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

/** 打印(F8)：F8 触发批量打印（打印配置生效于弹窗文案） */
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrintF8()
  }
}
</script>

<style scoped>
/* ═══ 查询方案 ═══ */
.query-scheme-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
}

/* ═══ 快捷日期 ═══ */
.quick-dates :deep(.ant-btn) {
  font-size: 13px;
  padding: 2px 8px;
}
.quick-dates :deep(.ant-btn-primary) {
  color: #fff;
  background: #ff7a45;
  border-color: #ff7a45;
}

/* ═══ 搜索区域 ═══ */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
/* 折叠由「渲染字段数量」控制（DEFAULT_VISIBLE_COUNT），不用 max-height 裁剪，
   否则「查询」按钮会被裁掉导致无法展开 */
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

/* ═══ 表格区域 ═══ */
.table-area {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.currency-value {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
}

/* ═══ 紧凑尺寸 ═══ */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
</style>
