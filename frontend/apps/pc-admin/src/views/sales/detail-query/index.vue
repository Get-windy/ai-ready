<template>
  <ErrorBoundary @error="handleError">
    <CategoryListLayout
      :show-category-panel="true"
      :category-tree-data="categoryTreeData"
      :category-loading="categoryLoading"
      :category-title="'商品分类'"
      :category-editable="false"
      :show-table-footer="true"
      @category-select="handleCategorySelect"
    >
      <template #toolbar-left>
        <div class="query-scheme-wrap" style="display:inline-flex; align-items:center; gap:4px; margin-right:8px;">
          <a-select v-model:value="queryScheme" style="width:140px" size="small" placeholder="--查询方案--" @change="handleApplyScheme">
            <a-select-option value="">--查询方案--</a-select-option>
            <a-select-option v-for="s in savedSchemes" :key="s" :value="s">{{ s }}</a-select-option>
          </a-select>
          <a-button type="link" size="small" @click="handleSaveScheme"><PlusOutlined /></a-button>
        </div>
        <a-space :size="4">
          <a-button v-for="q in quickDates" :key="q.key" :type="quickDate === q.key ? 'primary' : 'default'" size="small" @click="setQuickDate(q.key)">{{ q.label }}</a-button>
        </a-space>
      </template>

      <template #toolbar-right>
        <a-space :size="8">
          <a-tooltip v-if="isButtonEnabled('config')" title="页面配置" placement="bottom">
            <a-button size="small" @click="showPageConfig = true">
              <SettingOutlined />
            </a-button>
          </a-tooltip>
          <a-badge :status="loading ? 'processing' : (hasError ? 'error' : 'success')" />
          <span v-if="lastUpdateTime" class="update-time">最后更新: {{ lastUpdateTime }}</span>
          <a-button v-if="isButtonEnabled('refresh')" size="small" @click="fetchData">
            <ReloadOutlined /> 刷新
          </a-button>
          <a-button v-if="isButtonEnabled('export')" size="small" :loading="exporting" @click="handleExport">
            <DownloadOutlined /> 导出
          </a-button>
        </a-space>
      </template>

      <!-- ═══ 搜索区域（字段显隐 / 顺序由「页面配置 → 查询条件」驱动） ═══ -->
      <template #search-fields>
        <div class="search-area">
          <div class="search-container" :data-expanded="showMoreConditions || null">
            <div class="search-grid">
              <div
                v-for="f in gridSearchFields"
                :key="f.key"
                class="search-field-item"
              >
                <!-- 日期区间 -->
                <a-range-picker
                  v-if="f.type === 'dateRange' && f.key === 'documentDate'"
                  v-model:value="documentDateRange"
                  size="small"
                  style="width: 100%"
                  @change="handleDocumentDateChange"
                />
                <a-range-picker
                  v-else-if="f.type === 'dateRange'"
                  v-model:value="settlementDateRange"
                  size="small"
                  style="width: 100%"
                  @change="handleSettlementDateChange"
                />

                <!-- 下拉 -->
                <div v-else-if="isSelectType(f)" class="search-select-wrap">
                  <span class="search-select-label">{{ f.label }}</span>
                  <a-select
                    v-model:value="searchParams[f.key]"
                    size="small"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    :placeholder="f.placeholder || '全部'"
                    :options="fieldOptions(f)"
                  />
                </div>

                <!-- 数值区间 -->
                <div v-else-if="f.type === 'numberRange'" class="range-field" :title="f.label" style="display: flex; align-items: center; width: 100%; gap: 4px;">
                  <span class="range-label">{{ f.label }}</span>
                  <a-input-number v-model:value="searchParams[f.minKey!]" placeholder="最小" size="small" style="flex: 1; min-width: 0;" />
                  <span>-</span>
                  <a-input-number v-model:value="searchParams[f.maxKey!]" placeholder="最大" size="small" style="flex: 1; min-width: 0;" />
                </div>

                <!-- 文本 -->
                <a-input
                  v-else
                  v-model:value="searchParams[f.key]"
                  :placeholder="f.label"
                  allow-clear
                  size="small"
                  :suffix="f.searchIcon ? h(SearchOutlined, { style: 'color:#bbb' }) : undefined"
                />
              </div>
            </div>

            <!-- ═══ 查询操作行：固定排在字段折叠区之外，字段再多也不会被折叠高度裁掉 ═══ -->
            <div class="search-action-row">
              <a-button v-if="isButtonEnabled('search')" type="primary" size="small" @click="handleSearch">
                <SearchOutlined /> 查询
              </a-button>
              <a-button v-if="isButtonEnabled('reset')" size="small" @click="handleReset">
                <ClearOutlined /> 重置
              </a-button>
              <a-checkbox
                v-for="f in actionSearchFields"
                :key="f.key"
                v-model:checked="searchParams[f.key]"
              >
                {{ f.label }}
              </a-checkbox>
            </div>
            <div v-if="isButtonEnabled('more')" class="search-more-toggle">
              <a-button type="link" size="small" @click="toggleMoreConditions">
                {{ showMoreConditions ? '收起' : '更多条件' }}
                <DownOutlined v-if="!showMoreConditions" />
                <UpOutlined v-else />
              </a-button>
            </div>
          </div>
        </div>
      </template>

      <!-- ═══ 数据表格 ═══ -->
      <template #table>
        <BillTableList
          :columns="columns"
          :storage-key="'sales-detail-query-table-columns'"
          global-config-key="sales-detail-query-columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="billPagination"
          :summary-columns="tableFooterColumns"
          :show-toolbar="false"
          :show-search="false"
          :show-add="false"
          :show-export="false"
          :show-batch-delete="false"
          :selectable="false"
          row-key="id"
          :scroll="{ x: 8000 }"
          @page-change="handlePageChange"
        >
          <template #actionCell="{ record }">
            <a-space :size="0">
              <a-button type="link" size="small" style="padding:0 4px;" @click="openBatchModal(record)">批次号</a-button>
              <a-button type="link" size="small" style="padding:0 4px;" @click="openRemarkModal(record)">明细备注</a-button>
            </a-space>
          </template>
        </BillTableList>
      </template>
    </CategoryListLayout>

    <!-- ═══ 页面配置弹窗（查询条件 / 功能按钮 / 打印配置） ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldsConfig"
      :function-buttons-config="functionButtonConfig"
      :print-config-items="printConfigItems"
      storage-key="sales-detail-query-page-config"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- ═══ 批次号：明细行批次追溯 ═══ -->
    <a-modal
      v-model:open="batchModal.open"
      title="批次追溯"
      :footer="null"
      width="720px"
      centered
    >
      <a-descriptions :column="3" size="small" bordered style="margin-bottom:12px;">
        <a-descriptions-item label="单据编号">{{ batchModal.record?.docNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="商品">{{ batchModal.record?.productName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="销售数量">{{ batchModal.record?.salesQuantity ?? '-' }} {{ batchModal.record?.salesQuantityUnit || '' }}</a-descriptions-item>
        <a-descriptions-item label="批次条码">{{ batchModal.record?.batchBarcode || '未关联批次' }}</a-descriptions-item>
        <a-descriptions-item label="生产日期">{{ batchModal.record?.productionDate || '-' }}</a-descriptions-item>
        <a-descriptions-item label="到期日期">{{ batchModal.record?.expiryDate || '-' }}</a-descriptions-item>
      </a-descriptions>
      <a-spin :spinning="batchModal.loading">
        <a-table
          v-if="batchModal.rows.length"
          :data-source="batchModal.rows"
          :columns="batchColumns"
          size="small"
          row-key="id"
          :pagination="false"
          :scroll="{ x: 640 }"
        />
        <a-empty v-else description="该明细行未关联批次库存记录" />
      </a-spin>
    </a-modal>

    <!-- ═══ 明细备注：单据/明细/买家/客户 备注一览 ═══ -->
    <a-modal
      v-model:open="remarkModal.open"
      title="明细备注"
      :footer="null"
      width="620px"
      centered
    >
      <a-descriptions :column="1" size="small" bordered>
        <a-descriptions-item label="单据编号">{{ remarkModal.record?.docNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="商品">{{ remarkModal.record?.productName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="明细备注">{{ remarkModal.record?.itemRemark || '（空）' }}</a-descriptions-item>
        <a-descriptions-item label="单据备注">{{ remarkModal.record?.remark || '（空）' }}</a-descriptions-item>
        <a-descriptions-item label="买家备注">{{ remarkModal.record?.buyerRemark || '（空）' }}</a-descriptions-item>
        <a-descriptions-item label="客户备注">{{ remarkModal.record?.customerRemark || '（空）' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import {
  ReloadOutlined, SearchOutlined, ClearOutlined, DownloadOutlined,
  DownOutlined, UpOutlined, SettingOutlined, PlusOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import request from '@/utils/request'
import { productCategoryApi } from '@/api/erp/product'
import { optionsApi } from '@/api/options'
import { batchApi } from '@/api/erp/batch'

const PAGE_CONFIG_STORAGE_KEY = 'sales-detail-query-page-config'

const loading = ref(false)
const exporting = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')
const showMoreConditions = ref(false)
const showPageConfig = ref(false)

// ═══ 搜索参数 ═══
const searchParams = reactive<Record<string, any>>({
  dateType: 'documentDate',
  startDate: '', endDate: '',
  documentNo: '',
  documentType: '',
  warehouseName: '',
  customerName: '',
  receiverName: '', receiverPhone: '', shippingAddress: '',
  handlerName: '', defaultHandlerName: '', departmentName: '',
  creatorName: '', bookkeeperName: '',
  productName: '',
  brand: '',
  industryCategory: '',
  productAttribute: '',
  minPrice: null as number | null,
  maxPrice: null as number | null,
  isGift: false,
  isPromoProduct: false,
  deliveryMethod: '',
  logisticsCompany: '', trackingNumber: '',
  deliveryDriver: '',
  region: '',
  remark: '', itemRemark: '', buyerRemark: '',
  sourceOrder: '',
  settlementStatus: '',
  generationMethod: '',
  salesType: '',
  settlementTimeStart: '', settlementTimeEnd: '',
  source: '',
  itemExtNum1Min: null as number | null, itemExtNum1Max: null as number | null,
  itemExtNum2Min: null as number | null, itemExtNum2Max: null as number | null,
  itemExtNum3Min: null as number | null, itemExtNum3Max: null as number | null,
  itemExtNum6Min: null as number | null, itemExtNum6Max: null as number | null,
  itemExtNum7Min: null as number | null, itemExtNum7Max: null as number | null,
  itemExtText1: '', itemExtText2: '',
  itemExtPartner: undefined as number | undefined,
  itemExtStaff: undefined as number | undefined,
  itemExtDept: undefined as number | undefined,
  showRed: false,
  onlyVehicleWarehouse: false,
  hasSourceOrder: false,
})

const documentDateRange = ref<[Dayjs, Dayjs] | null>(null)
const settlementDateRange = ref<[Dayjs, Dayjs] | null>(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current, pageSize: pagination.pageSize, total: pagination.total
}))

// ═══ 表体自定义8/9/10 选择项（往来单位 / 职员 / 部门） ═══
const partnerOptions = ref<{ label: string; value: any }[]>([])
const staffOptions = ref<{ label: string; value: any }[]>([])
const deptOptions = ref<{ label: string; value: any }[]>([])
const partnerNameMap = ref<Record<string, string>>({})
const staffNameMap = ref<Record<string, string>>({})
const deptNameMap = ref<Record<string, string>>({})

async function loadExtOptions() {
  try {
    const customers = await optionsApi.getCustomers()
    partnerOptions.value = (customers || []).map(c => ({ label: c.name, value: c.id }))
    partnerNameMap.value = Object.fromEntries((customers || []).map(c => [String(c.id), c.name]))
  } catch { partnerOptions.value = [] }
  try {
    const users = await optionsApi.getUsers()
    const list = (users as any)?.data || users || []
    staffOptions.value = list.map((u: any) => ({ label: u.nickname || u.username || u.name, value: u.id }))
    staffNameMap.value = Object.fromEntries(list.map((u: any) => [String(u.id), u.nickname || u.username || u.name]))
  } catch { staffOptions.value = [] }
  try {
    const depts = await optionsApi.getDepartments()
    deptOptions.value = (depts || []).map(d => ({ label: d.name, value: d.id }))
    deptNameMap.value = Object.fromEntries((depts || []).map(d => [String(d.id), d.name]))
  } catch { deptOptions.value = [] }
}

// ═══ 查询字段定义（48 项，与「页面配置 → 查询条件」key 一一对应） ═══
type SearchFieldType = 'text' | 'select' | 'dateRange' | 'numberRange' | 'checkbox'
  | 'partnerSelect' | 'staffSelect' | 'deptSelect'

interface SearchFieldDef {
  key: string
  label: string
  type: SearchFieldType
  placeholder?: string
  searchIcon?: boolean
  options?: Array<{ label: string; value: any }>
  minKey?: string
  maxKey?: string
}

const OPTION_DATE_TYPE = [
  { label: '单据日期', value: 'documentDate' },
  { label: '制单时间', value: 'createTime' },
  { label: '记账时间', value: 'bookkeepingTime' },
  { label: '结算完成时间', value: 'settlementTime' },
]
const OPTION_DELIVERY_METHOD = [
  { label: '配送', value: 'delivery' },
  { label: '自提', value: 'self' },
  { label: '物流', value: 'logistics' },
  { label: '快递', value: 'express' },
]
const OPTION_SETTLEMENT_STATUS = [
  { label: '未结算', value: 'unsettled' },
  { label: '部分结算', value: 'partial' },
  { label: '已结算', value: 'settled' },
]
const OPTION_GENERATION_METHOD = [
  { label: '手工创建', value: '手工创建' },
  { label: '订单生成', value: '订单生成' },
  { label: '复制', value: '复制' },
  { label: '导入', value: '导入' },
]
const OPTION_DOC_TYPE = [
  { label: '销售出库', value: '0' },
  { label: '换货出库', value: '1' },
  { label: '调拨出库', value: '2' },
  { label: '其他出库', value: '3' },
]
const OPTION_SALES_TYPE = [
  { label: '正常销售', value: '0' },
  { label: '换货', value: '1' },
  { label: '调拨', value: '2' },
  { label: '其他', value: '3' },
]
const OPTION_SOURCE = [
  { label: '电脑端', value: 'PC' },
  { label: '移动端', value: 'MOBILE' },
  { label: 'API接口', value: 'API' },
  { label: '批量导入', value: 'IMPORT' },
]

const SEARCH_FIELD_DEFS: Record<string, SearchFieldDef> = {
  documentDate: { key: 'documentDate', label: '单据日期', type: 'dateRange' },
  dateType: { key: 'dateType', label: '日期类型', type: 'select', options: OPTION_DATE_TYPE },
  documentNo: { key: 'documentNo', label: '单据编号', type: 'text' },
  productName: { key: 'productName', label: '商品', type: 'text', searchIcon: true },
  deliveryMethod: { key: 'deliveryMethod', label: '配送方式', type: 'select', options: OPTION_DELIVERY_METHOD },
  brand: { key: 'brand', label: '品牌', type: 'text' },
  industryCategory: { key: 'industryCategory', label: '所属行业类别', type: 'text' },
  priceQuery: { key: 'priceQuery', label: '价格查询', type: 'numberRange', minKey: 'minPrice', maxKey: 'maxPrice' },
  isGift: { key: 'isGift', label: '是否赠品', type: 'checkbox' },
  isPromoProduct: { key: 'isPromoProduct', label: '促销商品', type: 'checkbox' },
  customerName: { key: 'customerName', label: '客户', type: 'text', searchIcon: true },
  receiverName: { key: 'receiverName', label: '收货人', type: 'text' },
  receiverPhone: { key: 'receiverPhone', label: '联系电话', type: 'text' },
  shippingAddress: { key: 'shippingAddress', label: '收货地址', type: 'text' },
  handlerName: { key: 'handlerName', label: '经手人', type: 'text', searchIcon: true },
  defaultHandlerName: { key: 'defaultHandlerName', label: '默认经手人', type: 'text' },
  departmentName: { key: 'departmentName', label: '部门', type: 'text' },
  creatorName: { key: 'creatorName', label: '制单人', type: 'text' },
  bookkeeperName: { key: 'bookkeeperName', label: '记账人', type: 'text' },
  warehouseName: { key: 'warehouseName', label: '仓库', type: 'text', searchIcon: true },
  settlementStatus: { key: 'settlementStatus', label: '结算状态', type: 'select', options: OPTION_SETTLEMENT_STATUS },
  sourceOrder: { key: 'sourceOrder', label: '来源订单', type: 'text' },
  generationMethod: { key: 'generationMethod', label: '产生方式', type: 'select', options: OPTION_GENERATION_METHOD },
  documentType: { key: 'documentType', label: '单据类型', type: 'select', options: OPTION_DOC_TYPE },
  salesType: { key: 'salesType', label: '销售类型', type: 'select', options: OPTION_SALES_TYPE },
  productAttribute: { key: 'productAttribute', label: '商品行属性', type: 'text' },
  itemRemark: { key: 'itemRemark', label: '明细备注', type: 'text' },
  remark: { key: 'remark', label: '单据备注', type: 'text' },
  buyerRemark: { key: 'buyerRemark', label: '买家备注', type: 'text' },
  logisticsCompany: { key: 'logisticsCompany', label: '物流公司', type: 'text' },
  trackingNumber: { key: 'trackingNumber', label: '运单号', type: 'text' },
  region: { key: 'region', label: '区域', type: 'text' },
  itemExtNum1: { key: 'itemExtNum1', label: '表体自定义1(数字)', type: 'numberRange', minKey: 'itemExtNum1Min', maxKey: 'itemExtNum1Max' },
  itemExtNum2: { key: 'itemExtNum2', label: '表体自定义2(数字)', type: 'numberRange', minKey: 'itemExtNum2Min', maxKey: 'itemExtNum2Max' },
  itemExtNum3: { key: 'itemExtNum3', label: '表体自定义3(数字)', type: 'numberRange', minKey: 'itemExtNum3Min', maxKey: 'itemExtNum3Max' },
  itemExtText1: { key: 'itemExtText1', label: '表体自定义4(文本)', type: 'text' },
  itemExtText2: { key: 'itemExtText2', label: '表体自定义5(文本)', type: 'text' },
  itemExtNum6: { key: 'itemExtNum6', label: '表体自定义6(数字)', type: 'numberRange', minKey: 'itemExtNum6Min', maxKey: 'itemExtNum6Max' },
  itemExtNum7: { key: 'itemExtNum7', label: '表体自定义7(数字)', type: 'numberRange', minKey: 'itemExtNum7Min', maxKey: 'itemExtNum7Max' },
  itemExtPartner: { key: 'itemExtPartner', label: '表体自定义8(往来单位)', type: 'partnerSelect' },
  itemExtStaff: { key: 'itemExtStaff', label: '表体自定义9(职员)', type: 'staffSelect' },
  itemExtDept: { key: 'itemExtDept', label: '表体自定义10(部门)', type: 'deptSelect' },
  settlementTime: { key: 'settlementTime', label: '结算完成时间', type: 'dateRange' },
  source: { key: 'source', label: '来源', type: 'select', options: OPTION_SOURCE },
  deliveryDriver: { key: 'deliveryDriver', label: '配送司机', type: 'text' },
  showRed: { key: 'showRed', label: '显示红冲', type: 'checkbox' },
  onlyVehicleWarehouse: { key: 'onlyVehicleWarehouse', label: '仅统计车辆库', type: 'checkbox' },
  hasSourceOrder: { key: 'hasSourceOrder', label: '包含有来源订单', type: 'checkbox' },
}

const isSelectType = (f: SearchFieldDef) =>
  f.type === 'select' || f.type === 'partnerSelect' || f.type === 'staffSelect' || f.type === 'deptSelect'

function fieldOptions(f: SearchFieldDef) {
  if (f.type === 'partnerSelect') return partnerOptions.value
  if (f.type === 'staffSelect') return staffOptions.value
  if (f.type === 'deptSelect') return deptOptions.value
  return f.options || []
}

// ═══ 页面配置：查询字段（默认勾选 = 折叠区 2 排 + 操作行 3 个勾选项） ═══
const queryFieldsConfig = ref([
  { key: 'documentDate', label: '单据日期', visible: true },
  { key: 'dateType', label: '日期类型', visible: true },
  { key: 'documentNo', label: '单据编号', visible: true },
  { key: 'productName', label: '商品', visible: true },
  { key: 'deliveryMethod', label: '配送方式', visible: true },
  { key: 'brand', label: '品牌', visible: true },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'industryCategory', label: '所属行业类别', visible: false },
  { key: 'priceQuery', label: '价格查询', visible: false },
  { key: 'isGift', label: '是否赠品', visible: false },
  { key: 'isPromoProduct', label: '促销商品', visible: false },
  { key: 'receiverName', label: '收货人', visible: false },
  { key: 'receiverPhone', label: '联系电话', visible: false },
  { key: 'shippingAddress', label: '收货地址', visible: false },
  { key: 'handlerName', label: '经手人', visible: false },
  { key: 'defaultHandlerName', label: '默认经手人', visible: false },
  { key: 'departmentName', label: '部门', visible: false },
  { key: 'creatorName', label: '制单人', visible: false },
  { key: 'bookkeeperName', label: '记账人', visible: false },
  { key: 'settlementStatus', label: '结算状态', visible: false },
  { key: 'sourceOrder', label: '来源订单', visible: false },
  { key: 'generationMethod', label: '产生方式', visible: false },
  { key: 'documentType', label: '单据类型', visible: false },
  { key: 'salesType', label: '销售类型', visible: false },
  { key: 'productAttribute', label: '商品行属性', visible: false },
  { key: 'itemRemark', label: '明细备注', visible: false },
  { key: 'remark', label: '单据备注', visible: false },
  { key: 'buyerRemark', label: '买家备注', visible: false },
  { key: 'logisticsCompany', label: '物流公司', visible: false },
  { key: 'trackingNumber', label: '运单号', visible: false },
  { key: 'region', label: '区域', visible: false },
  { key: 'itemExtNum1', label: '表体自定义1(数字)', visible: false },
  { key: 'itemExtNum2', label: '表体自定义2(数字)', visible: false },
  { key: 'itemExtNum3', label: '表体自定义3(数字)', visible: false },
  { key: 'itemExtText1', label: '表体自定义4(文本)', visible: false },
  { key: 'itemExtText2', label: '表体自定义5(文本)', visible: false },
  { key: 'itemExtNum6', label: '表体自定义6(数字)', visible: false },
  { key: 'itemExtNum7', label: '表体自定义7(数字)', visible: false },
  { key: 'itemExtPartner', label: '表体自定义8(往来单位)', visible: false },
  { key: 'itemExtStaff', label: '表体自定义9(职员)', visible: false },
  { key: 'itemExtDept', label: '表体自定义10(部门)', visible: false },
  { key: 'settlementTime', label: '结算完成时间', visible: false },
  { key: 'source', label: '来源', visible: false },
  { key: 'deliveryDriver', label: '配送司机', visible: false },
  { key: 'showRed', label: '显示红冲', visible: true },
  { key: 'onlyVehicleWarehouse', label: '仅统计车辆库', visible: true },
  { key: 'hasSourceOrder', label: '包含有来源订单', visible: true },
])

/** 搜索区实际渲染字段：按页面配置的勾选与顺序（配置真实生效） */
const visibleSearchFields = computed(() =>
  queryFieldsConfig.value
    .filter(cfg => cfg.visible)
    .map(cfg => SEARCH_FIELD_DEFS[cfg.key])
    .filter((def): def is SearchFieldDef => !!def)
)
/** 字段网格：参与「更多条件」折叠 */
const gridSearchFields = computed(() => visibleSearchFields.value.filter(f => f.type !== 'checkbox'))
/** 操作行：查询/重置按钮旁的勾选项，始终可见 */
const actionSearchFields = computed(() => visibleSearchFields.value.filter(f => f.type === 'checkbox'))

// ═══ 页面配置：功能按钮（与工具栏/搜索区按钮一一对应） ═══
const functionButtonConfig = ref([
  { key: 'search', label: '查询', enabled: true },
  { key: 'reset', label: '重置', enabled: true },
  { key: 'more', label: '更多条件', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '页面配置', enabled: true },
])

const enabledButtons = ref<Record<string, boolean>>({})
const isButtonEnabled = (key: string) => enabledButtons.value[key] !== false

const printConfigItems = [
  { key: 'alwaysLastTemplate', label: '始终使用最后一次打印的模板，打印时不再选择' },
]
const printConfig = reactive<Record<string, boolean>>({ alwaysLastTemplate: false })

function syncPageConfig(config: any) {
  if (!config) return
  if (Array.isArray(config.queryFields) && config.queryFields.length) {
    const orderMap = new Map<string, number>()
    config.queryFields.forEach((f: any, i: number) => orderMap.set(f.key, i))
    queryFieldsConfig.value = [...queryFieldsConfig.value]
      .sort((a, b) => (orderMap.get(a.key) ?? 999) - (orderMap.get(b.key) ?? 999))
      .map(f => {
        const saved = config.queryFields.find((c: any) => c.key === f.key)
        return saved ? { ...f, visible: saved.visible } : f
      })
  }
  if (Array.isArray(config.functionButtons)) {
    const map: Record<string, boolean> = {}
    config.functionButtons.forEach((b: any) => { map[b.key] = b.enabled })
    enabledButtons.value = map
  }
  if (config.printConfig) {
    Object.assign(printConfig, config.printConfig)
  }
}

const handlePageConfigChange = (config: any) => syncPageConfig(config)

/** 首次进入同步已保存配置，避免必须打开弹窗才生效 */
function loadSavedPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY)
    if (raw) syncPageConfig(JSON.parse(raw))
  } catch {
    // 配置损坏时按默认值运行
  }
}

// ═══ 96列表格定义 ═══
const columns = [
  // 序号列（表头内嵌齿轮 = 列配置入口）
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' as const },
  // 操作列
  { title: '操作', key: 'action', type: 'action', width: 120, fixed: 'right' as const, slotName: 'actionCell' },
  // 表头字段 1-27
  { title: '单据日期', field: 'docDate', key: 'docDate', width: 120 },
  { title: '单据编号', field: 'docNo', key: 'docNo', width: 160 },
  { title: '单据类型', field: 'docType', key: 'docType', width: 100 },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '客户', field: 'customerName', key: 'customerName', width: 120 },
  { title: '客户编号', field: 'customerCode', key: 'customerCode', width: 100 },
  { title: '客户级别', field: 'customerLevel', key: 'customerLevel', width: 100 },
  { title: '收货人', field: 'receiverName', key: 'receiverName', width: 100 },
  { title: '联系电话', field: 'receiverPhone', key: 'receiverPhone', width: 120 },
  { title: '收货地址', field: 'shippingAddress', key: 'shippingAddress', width: 180 },
  { title: '客户自定义字段1', field: 'custExtText1', key: 'custExtText1', width: 130, defaultHidden: true },
  { title: '客户自定义字段2', field: 'custExtText2', key: 'custExtText2', width: 130, defaultHidden: true },
  { title: '客户自定义字段3', field: 'custExtText3', key: 'custExtText3', width: 130, defaultHidden: true },
  { title: '客户自定义字段4', field: 'custExtText4', key: 'custExtText4', width: 130, defaultHidden: true },
  { title: '客户自定义字段5', field: 'custExtText5', key: 'custExtText5', width: 130, defaultHidden: true },
  { title: '物流公司', field: 'logisticsCompany', key: 'logisticsCompany', width: 120 },
  { title: '运单号', field: 'trackingNumber', key: 'trackingNumber', width: 140 },
  { title: '区域', field: 'region', key: 'region', width: 100 },
  { title: '买家备注', field: 'buyerRemark', key: 'buyerRemark', width: 150 },
  { title: '客户备注', field: 'customerRemark', key: 'customerRemark', width: 150 },
  { title: '来源订单', field: 'sourceOrder', key: 'sourceOrder', width: 160 },
  { title: '来源订单日期', field: 'sourceOrderDate', key: 'sourceOrderDate', width: 120, defaultHidden: true },
  { title: '产生方式', field: 'generationMethod', key: 'generationMethod', width: 100 },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 100 },
  { title: '部门', field: 'departmentName', key: 'departmentName', width: 100 },
  { title: '默认经手人', field: 'defaultHandlerName', key: 'defaultHandlerName', width: 110, defaultHidden: true },
  { title: '结算状态', field: 'settlementStatus', key: 'settlementStatus', width: 100 },
  // 商品字段 28-36
  { title: '商品名称', field: 'productName', key: 'productName', width: 180 },
  { title: '货号', field: 'productCode', key: 'productCode', width: 120 },
  { title: '条码', field: 'barcode', key: 'barcode', width: 130 },
  { title: '小单位条码', field: 'smallUnitBarcode', key: 'smallUnitBarcode', width: 130, defaultHidden: true },
  { title: '规格', field: 'specification', key: 'specification', width: 120 },
  { title: '型号', field: 'model', key: 'model', width: 100 },
  { title: '产地', field: 'origin', key: 'origin', width: 100 },
  { title: '配送方式', field: 'deliveryMethod', key: 'deliveryMethod', width: 100 },
  { title: '品牌', field: 'brand', key: 'brand', width: 100 },
  // 表体自定义 37-46
  { title: '表体自定义1', field: 'itemExtNum1', key: 'itemExtNum1', width: 120, align: 'right', defaultHidden: true },
  { title: '表体自定义2', field: 'itemExtNum2', key: 'itemExtNum2', width: 120, align: 'right', defaultHidden: true },
  { title: '表体自定义3', field: 'itemExtNum3', key: 'itemExtNum3', width: 120, align: 'right', defaultHidden: true },
  { title: '表体自定义4(文本)', field: 'itemExtText1', key: 'itemExtText1', width: 130, defaultHidden: true },
  { title: '表体自定义5(文本)', field: 'itemExtText2', key: 'itemExtText2', width: 130, defaultHidden: true },
  { title: '表体自定义6', field: 'itemExtNum6', key: 'itemExtNum6', width: 120, align: 'right', defaultHidden: true },
  { title: '表体自定义7', field: 'itemExtNum7', key: 'itemExtNum7', width: 120, align: 'right', defaultHidden: true },
  { title: '表体自定义8(往来单位)', field: 'itemExtPartner', key: 'itemExtPartner', width: 140, defaultHidden: true, formatter: (v: any) => partnerNameMap.value[String(v)] || v || '' },
  { title: '表体自定义9(职员)', field: 'itemExtStaff', key: 'itemExtStaff', width: 120, defaultHidden: true, formatter: (v: any) => staffNameMap.value[String(v)] || v || '' },
  { title: '表体自定义10(部门)', field: 'itemExtDept', key: 'itemExtDept', width: 130, defaultHidden: true, formatter: (v: any) => deptNameMap.value[String(v)] || v || '' },
  // 数量/包装 47-58
  { title: '销售数量', field: 'salesQuantity', key: 'salesQuantity', width: 100, align: 'right' },
  { title: '销售数量单位', field: 'salesQuantityUnit', key: 'salesQuantityUnit', width: 100 },
  { title: '销售常用单位数量', field: 'commonUnitQuantity', key: 'commonUnitQuantity', width: 130, align: 'right', defaultHidden: true },
  { title: '销售常用单位', field: 'commonUnit', key: 'commonUnit', width: 110, defaultHidden: true },
  { title: '批次条码', field: 'batchBarcode', key: 'batchBarcode', width: 130 },
  { title: '生产日期', field: 'productionDate', key: 'productionDate', width: 120 },
  { title: '到期日期', field: 'expiryDate', key: 'expiryDate', width: 120 },
  { title: '大包装', field: 'bigPack', key: 'bigPack', width: 80, align: 'right', defaultHidden: true },
  { title: '中包装', field: 'midPack', key: 'midPack', width: 80, align: 'right', defaultHidden: true },
  { title: '小包装', field: 'smallPack', key: 'smallPack', width: 80, align: 'right', defaultHidden: true },
  { title: '小单位', field: 'smallUnit', key: 'smallUnit', width: 80, defaultHidden: true },
  { title: '小单位数量', field: 'smallUnitQuantity', key: 'smallUnitQuantity', width: 100, align: 'right', defaultHidden: true },
  // 价格等级 59-66（用户自定义昵称，默认餐饮行业名称）
  { title: '餐饮店', field: 'priceLevel1', key: 'priceLevel1', width: 100, align: 'right', defaultHidden: true },
  { title: '食堂团餐', field: 'priceLevel2', key: 'priceLevel2', width: 100, align: 'right', defaultHidden: true },
  { title: '自助vip', field: 'priceLevel3', key: 'priceLevel3', width: 100, align: 'right', defaultHidden: true },
  { title: '大团餐', field: 'priceLevel4', key: 'priceLevel4', width: 100, align: 'right', defaultHidden: true },
  { title: '特价客户', field: 'priceLevel5', key: 'priceLevel5', width: 100, align: 'right', defaultHidden: true },
  { title: '外围餐饮店', field: 'priceLevel6', key: 'priceLevel6', width: 110, align: 'right', defaultHidden: true },
  { title: '重点vip01', field: 'priceLevel7', key: 'priceLevel7', width: 100, align: 'right', defaultHidden: true },
  { title: '连锁vip', field: 'priceLevel8', key: 'priceLevel8', width: 100, align: 'right', defaultHidden: true },
  // 定价 67-80
  { title: '单价', field: 'unitPrice', key: 'unitPrice', width: 100, align: 'right' },
  { title: '小单位单价', field: 'smallUnitPrice', key: 'smallUnitPrice', width: 110, align: 'right', defaultHidden: true },
  { title: '金额', field: 'amount', key: 'amount', width: 110, align: 'right' },
  { title: '折扣(%)', field: 'discountRate', key: 'discountRate', width: 90, align: 'right' },
  { title: '折后单价', field: 'discountedPrice', key: 'discountedPrice', width: 100, align: 'right' },
  { title: '折后金额', field: 'discountedAmount', key: 'discountedAmount', width: 110, align: 'right' },
  { title: '优惠折扣', field: 'favorableDiscountRate', key: 'favorableDiscountRate', width: 100, align: 'right' },
  { title: '优惠后单价', field: 'favorableUnitPrice', key: 'favorableUnitPrice', width: 110, align: 'right' },
  { title: '优惠后金额', field: 'favorableAmount', key: 'favorableAmount', width: 110, align: 'right' },
  { title: '销售收入', field: 'salesRevenue', key: 'salesRevenue', width: 110, align: 'right' },
  { title: '成本单价', field: 'costPrice', key: 'costPrice', width: 100, align: 'right' },
  { title: '成本金额', field: 'costAmount', key: 'costAmount', width: 110, align: 'right' },
  { title: '毛利', field: 'grossProfit', key: 'grossProfit', width: 100, align: 'right' },
  { title: '毛利率(%)', field: 'grossProfitRate', key: 'grossProfitRate', width: 100, align: 'right' },
  // 参考/物理 81-85
  { title: '批发价', field: 'wholesalePrice', key: 'wholesalePrice', width: 100, align: 'right', defaultHidden: true },
  { title: '零售价', field: 'retailPrice', key: 'retailPrice', width: 100, align: 'right', defaultHidden: true },
  { title: '最低售价', field: 'minSalePrice', key: 'minSalePrice', width: 100, align: 'right', defaultHidden: true },
  { title: '重量(kg)', field: 'weight', key: 'weight', width: 90, align: 'right' },
  { title: '体积(m³)', field: 'volume', key: 'volume', width: 90, align: 'right' },
  // 类型属性 86-88
  { title: '销售类型', field: 'salesType', key: 'salesType', width: 100 },
  { title: '商品行属性', field: 'productAttribute', key: 'productAttribute', width: 100 },
  { title: '明细备注', field: 'itemRemark', key: 'itemRemark', width: 150 },
  // 系统字段 89-96
  { title: '单据备注', field: 'remark', key: 'remark', width: 150 },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 100 },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 100 },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 150 },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 150 },
  { title: '结算完成时间', field: 'settlementCompleteTime', key: 'settlementCompleteTime', width: 150, defaultHidden: true },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right' },
  { title: '配送司机', field: 'deliveryDriver', key: 'deliveryDriver', width: 100, defaultHidden: true },
]

// ═══ 事件处理 ═══
const handleDocumentDateChange = (dates: [Dayjs, Dayjs] | null) => {
  if (dates?.length === 2) {
    searchParams.startDate = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.endDate = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    searchParams.startDate = ''
    searchParams.endDate = ''
  }
}

const handleSettlementDateChange = (dates: [Dayjs, Dayjs] | null) => {
  if (dates?.length === 2) {
    searchParams.settlementTimeStart = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.settlementTimeEnd = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    searchParams.settlementTimeStart = ''
    searchParams.settlementTimeEnd = ''
  }
}

const toggleMoreConditions = () => {
  showMoreConditions.value = !showMoreConditions.value
}

// ═══ 数据获取 ═══
/** 组装查询参数：剔除空值，保留显式勾选的布尔项 */
function buildQueryParams(): Record<string, any> {
  const params: Record<string, any> = {
    ...searchParams,
    ...(selectedCategoryId.value ? { categoryId: selectedCategoryId.value } : {}),
  }
  Object.keys(params).forEach(key => {
    const v = params[key]
    if (v === '' || v === null || v === undefined || v === false) {
      delete params[key]
    }
  })
  ;(['showRed', 'onlyVehicleWarehouse', 'hasSourceOrder', 'isGift', 'isPromoProduct'] as const).forEach(k => {
    if (searchParams[k]) params[k] = true
  })
  return params
}

const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res: any = await request.get('/sales/detail-query/page', {
      params: { current: pagination.current, size: pagination.pageSize, ...buildQueryParams() },
    })
    if (res) {
      const data = res.data || res
      tableData.value = data.records || data.content || data.list || []
      pagination.total = data.total || 0
      lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    }
  } catch (e: any) {
    hasError.value = true
    message.error(e?.response?.data?.message || '查询失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  Object.assign(searchParams, {
    dateType: 'documentDate',
    startDate: '', endDate: '',
    documentNo: '', documentType: '', warehouseName: '', customerName: '',
    receiverName: '', receiverPhone: '', shippingAddress: '',
    handlerName: '', defaultHandlerName: '', departmentName: '',
    creatorName: '', bookkeeperName: '',
    productName: '', brand: '', industryCategory: '', productAttribute: '',
    minPrice: null, maxPrice: null,
    isGift: false, isPromoProduct: false,
    deliveryMethod: '', logisticsCompany: '', trackingNumber: '', deliveryDriver: '',
    region: '', remark: '', itemRemark: '', buyerRemark: '',
    sourceOrder: '', settlementStatus: '', generationMethod: '', salesType: '',
    settlementTimeStart: '', settlementTimeEnd: '', source: '',
    itemExtNum1Min: null, itemExtNum1Max: null,
    itemExtNum2Min: null, itemExtNum2Max: null,
    itemExtNum3Min: null, itemExtNum3Max: null,
    itemExtNum6Min: null, itemExtNum6Max: null,
    itemExtNum7Min: null, itemExtNum7Max: null,
    itemExtText1: '', itemExtText2: '',
    itemExtPartner: undefined, itemExtStaff: undefined, itemExtDept: undefined,
    showRed: false, onlyVehicleWarehouse: false, hasSourceOrder: false,
  })
  documentDateRange.value = null
  settlementDateRange.value = null
  pagination.current = 1
  fetchData()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

const handleError = (e: Error) => {
  hasError.value = true
  console.error(e)
}

/** 导出：后端返回真实 Excel 流（与查询同一过滤口径） */
const handleExport = async () => {
  exporting.value = true
  try {
    const blob: any = await request.get('/sales/detail-query/export', {
      params: buildQueryParams(),
      responseType: 'blob',
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `销售明细查询_${dayjs().format('YYYY-MM-DD')}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e: any) {
    message.error(e?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══ 商品分类树 ═══
const categoryTreeData = ref<any[]>([])
const categoryLoading = ref(false)
const categoryExpandedKeys = ref<(string | number)[]>([])
const selectedCategoryId = ref<string | number>('')

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

function handleCategorySelect(keys: any[]) {
  selectedCategoryId.value = keys?.[0] ?? ''
  pagination.current = 1
  fetchData()
}

// ═══ 查询方案 + 快捷日期 ═══
const SCHEME_PREFIX = 'sales-detail-query-scheme-'
const queryScheme = ref('')
const savedSchemes = ref<string[]>([])
const quickDate = ref('')

/** 载入已保存的查询方案名称（与保存共用同一存储前缀） */
function loadSavedSchemes() {
  const names: string[] = []
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i)
    if (key && key.startsWith(SCHEME_PREFIX)) names.push(key.slice(SCHEME_PREFIX.length))
  }
  savedSchemes.value = names.sort()
}
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

/** 保存当前查询条件为查询方案（真实落库 localStorage，可再次选择复用） */
function handleSaveScheme() {
  const nameRef = { value: queryScheme.value || '' }
  Modal.confirm({
    title: '保存查询方案',
    width: 420,
    content: () => h('div', [
      h('p', { style: 'margin-bottom:8px;color:#666;font-size:13px' }, '将当前查询条件保存为方案，便于下次一键复用'),
      h('input', {
        class: 'ant-input',
        style: 'width:100%',
        placeholder: '请输入方案名称',
        value: nameRef.value,
        onInput: (e: any) => { nameRef.value = e.target.value },
      }),
    ]),
    onOk: () => {
      const name = String(nameRef.value || '').trim()
      if (!name) {
        message.warning('请输入方案名称')
        return Promise.reject(new Error('empty'))
      }
      try {
        localStorage.setItem(SCHEME_PREFIX + name, JSON.stringify({ ...searchParams }))
        loadSavedSchemes()
        queryScheme.value = name
        message.success(`查询方案「${name}」已保存`)
      } catch {
        message.error('保存失败')
      }
      return Promise.resolve()
    },
  })
}

/** 应用查询方案：回填查询参数并重新检索（真实可用，非死按钮） */
function handleApplyScheme(name: string) {
  if (!name) return
  try {
    const raw = localStorage.getItem(SCHEME_PREFIX + name)
    if (!raw) return
    const saved = JSON.parse(raw)
    Object.assign(searchParams, saved)
    if (saved.startDate && saved.endDate) {
      documentDateRange.value = [dayjs(saved.startDate), dayjs(saved.endDate)]
    }
    if (saved.settlementTimeStart && saved.settlementTimeEnd) {
      settlementDateRange.value = [dayjs(saved.settlementTimeStart), dayjs(saved.settlementTimeEnd)]
    }
    pagination.current = 1
    fetchData()
    message.success(`已应用查询方案「${name}」`)
  } catch {
    message.error('查询方案读取失败')
  }
}

function setQuickDate(key: string) {
  quickDate.value = key
  const now = dayjs()
  let start: Dayjs, end: Dayjs
  switch (key) {
    case 'yesterday': start = now.subtract(1, 'day'); end = now.subtract(1, 'day'); break
    case 'today': start = now; end = now; break
    case 'week': start = now.startOf('week'); end = now; break
    case 'lastWeek': start = now.subtract(7, 'day'); end = now; break
    case 'month': start = now.startOf('month'); end = now; break
    case 'lastMonth': start = now.subtract(1, 'month').startOf('month'); end = now.subtract(1, 'month').endOf('month'); break
    case 'last3Month': start = now.subtract(3, 'month'); end = now; break
    case 'year': start = now.startOf('year'); end = now; break
    default: start = now.subtract(7, 'day'); end = now
  }
  documentDateRange.value = [start, end]
  searchParams.startDate = start.format('YYYY-MM-DD')
  searchParams.endDate = end.format('YYYY-MM-DD')
  pagination.current = 1
  fetchData()
}

// ═══ 底部合计 ═══
const tableFooterColumns = computed(() => {
  const total = tableData.value.reduce((s: number, r: any) => s + (Number(r.amount) || 0), 0)
  return total ? [{ label: '合计', value: total.toFixed(2) }] : []
})

// ═══ 操作列：批次号（真实批次追溯） ═══
const batchModal = reactive<{ open: boolean; record: any; rows: any[]; loading: boolean }>({
  open: false, record: null, rows: [], loading: false,
})
const batchColumns = [
  { title: '批次号', dataIndex: 'batchNo', key: 'batchNo', width: 160 },
  { title: '生产日期', dataIndex: 'productionDate', key: 'productionDate', width: 110 },
  { title: '到期日期', dataIndex: 'expirationDate', key: 'expirationDate', width: 110 },
  { title: '可用数量', dataIndex: 'availableQuantity', key: 'availableQuantity', width: 90, align: 'right' as const },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 110 },
  { title: '质检状态', dataIndex: 'qualityStatus', key: 'qualityStatus', width: 90 },
  { title: '来源类型', dataIndex: 'sourceType', key: 'sourceType', width: 90 },
  { title: '来源单号', dataIndex: 'sourceRefNo', key: 'sourceRefNo', width: 150 },
]

async function openBatchModal(record: any) {
  batchModal.record = record
  batchModal.rows = []
  batchModal.open = true
  if (!record?.batchBarcode) return
  batchModal.loading = true
  try {
    const res = await batchApi.page({ batchNo: record.batchBarcode, pageSize: 20 })
    batchModal.rows = res?.records || []
  } catch {
    batchModal.rows = []
  } finally {
    batchModal.loading = false
  }
}

// ═══ 操作列：明细备注一览 ═══
const remarkModal = reactive<{ open: boolean; record: any }>({ open: false, record: null })

function openRemarkModal(record: any) {
  remarkModal.record = record
  remarkModal.open = true
}

onMounted(() => {
  loadSavedPageConfig()
  loadSavedSchemes()
  loadExtOptions()
  fetchData()
  loadCategoryTree()
})
</script>

<style scoped>
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
.range-label {
  font-size: 12px;
  color: rgba(0,0,0,0.65);
  white-space: nowrap;
  max-width: 74px;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 0;
}
.search-action-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
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
.update-time { font-size: 12px; color: #999; }
</style>
