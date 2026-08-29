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
          <a-select v-model:value="queryScheme" style="width:140px" size="small" placeholder="--查询方案--">
            <a-select-option value="">--查询方案--</a-select-option>
          </a-select>
          <a-button type="link" size="small"><PlusOutlined /></a-button>
        </div>
        <a-space :size="4">
          <a-button v-for="q in quickDates" :key="q.key" :type="quickDate === q.key ? 'primary' : 'default'" size="small" @click="setQuickDate(q.key)">{{ q.label }}</a-button>
        </a-space>
      </template>

      <template #toolbar-right>
        <a-space :size="12">
          <a-button size="small" @click="showPageConfig = true">
            <template #icon><SettingOutlined /></template>
          </a-button>
          <a-badge :status="loading ? 'processing' : (hasError ? 'error' : 'success')" />
          <span v-if="lastUpdateTime" class="update-time">最后更新: {{ lastUpdateTime }}</span>
          <a-button size="small" @click="fetchData">
            <template #icon><ReloadOutlined /></template>
          </a-button>
        </a-space>
      </template>

      <!-- ═══ 搜索区域 ═══ -->
      <template #search-fields>
      <div class="search-area">
        <div class="search-container" :data-expanded="showMoreConditions || null">
        <div class="search-grid" ref="gridRef">
          <!-- 始终可见字段 -->
          <div class="search-field-item">
            <a-range-picker v-model:value="documentDateRange" size="small" style="width: 100%" @change="handleDocumentDateChange" />
          </div>
          <div class="search-field-item">
            <div class="search-select-wrap">
              <span class="search-select-label">日期类型</span>
              <a-select v-model:value="searchParams.dateType" size="small" allow-clear placeholder="请选择">
                <a-select-option value="documentDate">单据日期</a-select-option>
                <a-select-option value="createTime">制单时间</a-select-option>
                <a-select-option value="bookkeepingTime">记账时间</a-select-option>
                <a-select-option value="settlementTime">结算完成时间</a-select-option>
              </a-select>
            </div>
          </div>
          <div class="search-field-item">
            <a-input v-model:value="searchParams.documentNo" placeholder="单据编号" allow-clear size="small" />
          </div>
          <div class="search-field-item">
            <a-input v-model:value="searchParams.productName" placeholder="商品" allow-clear size="small" />
          </div>
          <div class="search-field-item">
            <a-input v-model:value="searchParams.customerName" placeholder="客户" allow-clear size="small" />
          </div>
          <div class="search-field-item">
            <div class="search-select-wrap">
              <span class="search-select-label">配送方式</span>
              <a-select v-model:value="searchParams.deliveryMethod" size="small" allow-clear placeholder="全部">
                <a-select-option value="">全部</a-select-option>
                <a-select-option value="DELIVERY">送货上门</a-select-option>
                <a-select-option value="SELF">客户自提</a-select-option>
                <a-select-option value="LOGISTICS">物流配送</a-select-option>
                <a-select-option value="EXPRESS">快递</a-select-option>
              </a-select>
            </div>
          </div>
          <div class="search-field-item">
            <a-input v-model:value="searchParams.brand" placeholder="品牌" allow-clear size="small" />
          </div>
          <div class="search-field-item">
            <a-input v-model:value="searchParams.warehouseName" placeholder="仓库" allow-clear size="small" />
          </div>

          <!-- 展开区域 -->
          <template v-if="showMoreConditions">
            <!-- Row 3: 行业类别 + 价格查询 + 是否赠品 + 促销商品 -->
            <div class="search-field-item">
              <a-input v-model:value="searchParams.industryCategory" placeholder="所属行业类别" allow-clear size="small" />
            </div>
            <div class="search-field-item">
              <div style="display: flex; align-items: center; width: 100%; gap: 4px;">
                <a-input-number v-model:value="searchParams.minPrice" placeholder="最低" size="small" style="flex: 1; min-width: 0;" />
                <span>-</span>
                <a-input-number v-model:value="searchParams.maxPrice" placeholder="最高" size="small" style="flex: 1; min-width: 0;" />
              </div>
            </div>
            <div class="search-field-item">
              <a-checkbox v-model:checked="searchParams.isGift">赠品</a-checkbox>
            </div>
            <div class="search-field-item">
              <a-checkbox v-model:checked="searchParams.isPromoProduct">促销品</a-checkbox>
            </div>

            <!-- Row 4: 收货人 + 联系电话 + 收货地址 + 经手人 -->
            <div class="search-field-item">
              <a-input v-model:value="searchParams.receiverName" placeholder="收货人" allow-clear size="small" />
            </div>
            <div class="search-field-item">
              <a-input v-model:value="searchParams.receiverPhone" placeholder="联系电话" allow-clear size="small" />
            </div>
            <div class="search-field-item">
              <a-input v-model:value="searchParams.shippingAddress" placeholder="收货地址" allow-clear size="small" />
            </div>
            <div class="search-field-item">
              <a-input v-model:value="searchParams.handlerName" placeholder="经手人" allow-clear size="small" />
            </div>

            <!-- Row 5: 默认经手人 + 部门 + 制单人 + 记账人 -->
            <div class="search-field-item">
              <a-input v-model:value="searchParams.defaultHandlerName" placeholder="默认经手人" allow-clear size="small" />
            </div>
            <div class="search-field-item">
              <a-input v-model:value="searchParams.departmentName" placeholder="部门" allow-clear size="small" />
            </div>
            <div class="search-field-item">
              <a-input v-model:value="searchParams.creatorName" placeholder="制单人" allow-clear size="small" />
            </div>
            <div class="search-field-item">
              <a-input v-model:value="searchParams.bookkeeperName" placeholder="记账人" allow-clear size="small" />
            </div>

            <!-- Row 6: 结算状态 + 来源订单 + 产生方式 + 单据类型 -->
            <div class="search-field-item">
              <div class="search-select-wrap">
                <span class="search-select-label">结算状态</span>
                <a-select v-model:value="searchParams.settlementStatus" size="small" allow-clear placeholder="全部">
                  <a-select-option value="">全部</a-select-option>
                  <a-select-option value="UNPAID">未结算</a-select-option>
                  <a-select-option value="PARTIAL_PAID">部分结算</a-select-option>
                  <a-select-option value="PAID">已结算</a-select-option>
                </a-select>
              </div>
            </div>
            <div class="search-field-item">
              <a-input v-model:value="searchParams.sourceOrder" placeholder="来源订单" allow-clear size="small" />
            </div>
            <div class="search-field-item">
              <div class="search-select-wrap">
                <span class="search-select-label">产生方式</span>
                <a-select v-model:value="searchParams.generationMethod" size="small" allow-clear placeholder="全部">
                  <a-select-option value="">全部</a-select-option>
                  <a-select-option value="MANUAL">手工录入</a-select-option>
                  <a-select-option value="SYSTEM">系统生成</a-select-option>
                </a-select>
              </div>
            </div>
            <div class="search-field-item">
              <div class="search-select-wrap">
                <span class="search-select-label">单据类型</span>
                <a-select v-model:value="searchParams.documentType" size="small" allow-clear placeholder="全部">
                  <a-select-option value="">全部</a-select-option>
                  <a-select-option value="0">销售出库</a-select-option>
                  <a-select-option value="1">退货出库</a-select-option>
                  <a-select-option value="2">换货出库</a-select-option>
                  <a-select-option value="3">调拨出库</a-select-option>
                </a-select>
              </div>
            </div>

            <!-- Row 7: 销售类型 + 商品行属性 + 明细备注 + 单据备注 -->
            <div class="search-field-item">
              <div class="search-select-wrap">
                <span class="search-select-label">销售类型</span>
                <a-select v-model:value="searchParams.salesType" size="small" allow-clear placeholder="全部">
                  <a-select-option value="">全部</a-select-option>
                  <a-select-option value="0">正常销售</a-select-option>
                  <a-select-option value="1">换货</a-select-option>
                  <a-select-option value="2">调拨</a-select-option>
                </a-select>
              </div>
            </div>
            <div class="search-field-item">
              <a-input v-model:value="searchParams.productAttribute" placeholder="商品行属性" allow-clear size="small" />
            </div>
            <div class="search-field-item">
              <a-input v-model:value="searchParams.itemRemark" placeholder="明细备注" allow-clear size="small" />
            </div>
            <div class="search-field-item">
              <a-input v-model:value="searchParams.remark" placeholder="单据备注" allow-clear size="small" />
            </div>

            <!-- Row 8: 买家备注 + 物流公司 + 运单号 + 区域 -->
            <div class="search-field-item">
              <a-input v-model:value="searchParams.buyerRemark" placeholder="买家备注" allow-clear size="small" />
            </div>
            <div class="search-field-item">
              <a-input v-model:value="searchParams.logisticsCompany" placeholder="物流公司" allow-clear size="small" />
            </div>
            <div class="search-field-item">
              <a-input v-model:value="searchParams.trackingNumber" placeholder="运单号" allow-clear size="small" />
            </div>
            <div class="search-field-item">
              <a-input v-model:value="searchParams.region" placeholder="区域" allow-clear size="small" />
            </div>

            <!-- Row 9: 表体自定义1-3(数字范围) + 表体自定义4(文本) -->
            <div class="search-field-item">
              <div style="display: flex; align-items: center; width: 100%; gap: 4px;">
                <a-input-number v-model:value="searchParams.itemExtNum1Min" placeholder="最小" size="small" style="flex: 1; min-width: 0;" />
                <span>-</span>
                <a-input-number v-model:value="searchParams.itemExtNum1Max" placeholder="最大" size="small" style="flex: 1; min-width: 0;" />
              </div>
            </div>
            <div class="search-field-item">
              <div style="display: flex; align-items: center; width: 100%; gap: 4px;">
                <a-input-number v-model:value="searchParams.itemExtNum2Min" placeholder="最小" size="small" style="flex: 1; min-width: 0;" />
                <span>-</span>
                <a-input-number v-model:value="searchParams.itemExtNum2Max" placeholder="最大" size="small" style="flex: 1; min-width: 0;" />
              </div>
            </div>
            <div class="search-field-item">
              <div style="display: flex; align-items: center; width: 100%; gap: 4px;">
                <a-input-number v-model:value="searchParams.itemExtNum3Min" placeholder="最小" size="small" style="flex: 1; min-width: 0;" />
                <span>-</span>
                <a-input-number v-model:value="searchParams.itemExtNum3Max" placeholder="最大" size="small" style="flex: 1; min-width: 0;" />
              </div>
            </div>
            <div class="search-field-item">
              <a-input v-model:value="searchParams.itemExtText1" placeholder="表体自定义4(文本)" allow-clear size="small" />
            </div>

            <!-- Row 10: 表体自定义5(文本) + 配送司机 + 结算完成时间 + 来源 -->
            <div class="search-field-item">
              <a-input v-model:value="searchParams.itemExtText2" placeholder="表体自定义5(文本)" allow-clear size="small" />
            </div>
            <div class="search-field-item">
              <a-input v-model:value="searchParams.deliveryDriver" placeholder="配送司机" allow-clear size="small" />
            </div>
            <div class="search-field-item">
              <a-range-picker v-model:value="settlementDateRange" size="small" style="width: 100%" @change="handleSettlementDateChange" />
            </div>
            <div class="search-field-item">
              <div class="search-select-wrap">
                <span class="search-select-label">来源</span>
                <a-select v-model:value="searchParams.source" size="small" allow-clear placeholder="全部">
                  <a-select-option value="">全部</a-select-option>
                  <a-select-option value="PC">电脑端</a-select-option>
                  <a-select-option value="MOBILE">移动端</a-select-option>
                  <a-select-option value="API">API接口</a-select-option>
                </a-select>
              </div>
            </div>

          </template>

          <!-- 操作按钮 -->
          <div class="search-action-group" ref="actionRef" :style="{ gridColumn: 'span ' + actionSpan }">
          <div class="search-field-item search-action-item">
            <a-space :size="4">
              <a-button type="primary" size="small" @click="handleSearch">
                <SearchOutlined /> 查询
              </a-button>
              <a-button size="small" @click="handleReset">
                <ClearOutlined /> 重置
              </a-button>
            </a-space>
          </div>
          <div class="search-field-item">
            <a-checkbox v-model:checked="searchParams.showRed">显示红冲单据</a-checkbox>
          </div>
          <div class="search-field-item">
            <a-checkbox v-model:checked="searchParams.onlyVehicleWarehouse">仅统计车辆库</a-checkbox>
          </div>
          <div class="search-field-item">
            <a-checkbox v-model:checked="searchParams.hasSourceOrder">包含有来源订单</a-checkbox>
          </div>
          </div>
        </div>
        <div class="search-more-toggle">
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
          :columns="visibleColumns"
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
              <a-button type="link" size="small" style="padding:0 4px;" @click="viewBatch(record)">批次号</a-button>
              <a-button type="link" size="small" style="padding:0 4px;" @click="viewItemRemark(record)">明细备注</a-button>
            </a-space>
          </template>
        </BillTableList>
      </template>
    </CategoryListLayout>

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldsConfig"
      :function-buttons-config="functionButtonConfig"
      storage-key="sales-detail-query-page-config"
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
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  ReloadOutlined, SearchOutlined, ClearOutlined,
  DownOutlined, UpOutlined, SettingOutlined, PlusOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import { useColumnConfig, isLockedColumn } from '@/composables/useColumnConfig'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import request from '@/utils/request'
import { productCategoryApi } from '@/api/erp/product'

const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')
const showMoreConditions = ref(false)
const showPageConfig = ref(false)
const showColumnConfig = ref(false)

const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

// ═══ 搜索参数 ═══
const searchParams = reactive({
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
  productCode: '',
  barcode: '',
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
  itemExtText1: '', itemExtText2: '',
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

// ═══ 页面配置：查询字段（50个条目） ═══
const queryFieldsConfig = ref([
  { key: 'dateType', label: '日期类型', visible: true },
  { key: 'documentDate', label: '单据日期', visible: true },
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
  { key: 'settlementTime', label: '结算完成时间', visible: false },
  { key: 'source', label: '来源', visible: false },
  { key: 'deliveryDriver', label: '配送司机', visible: false },
  { key: 'showRed', label: '显示红冲', visible: false },
  { key: 'onlyVehicleWarehouse', label: '仅统计车辆库', visible: false },
  { key: 'hasSourceOrder', label: '包含有来源订单', visible: false },
])

// ═══ 页面配置：功能按钮 ═══
const functionButtonConfig = ref([
  { key: 'search', label: '查询', enabled: false },
  { key: 'reset', label: '重置', enabled: false },
  { key: 'more', label: '更多条件', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true },
])

// ═══ 96列表格定义 ═══
const columns = [
  // 序号列
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
  { title: '表体自定义8(往来单位)', field: 'itemExtPartner', key: 'itemExtPartner', width: 140, defaultHidden: true },
  { title: '表体自定义9(职员)', field: 'itemExtStaff', key: 'itemExtStaff', width: 120, defaultHidden: true },
  { title: '表体自定义10(部门)', field: 'itemExtDept', key: 'itemExtDept', width: 130, defaultHidden: true },
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

// ═══ 列配置 ═══
const columnDefs = computed(() => columns.map(col => ({ ...col })))
const {
  visibleColumns,
  settingsColumns,
  onSettingChange: onColumnSettingChange,
  resetSettings: resetColumnSettings,
} = useColumnConfig(columnDefs.value, 'sales-detail-query-list-columns')

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

const handlePageConfigChange = () => {
  console.log('[销售明细查询] 页面配置已更新')
}

const handleColumnConfigChange = () => {
  onColumnSettingChange()
}

const handleColumnConfigReset = () => {
  resetColumnSettings()
}

// ═══ 数据获取 ═══
const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const apiParams: Record<string, any> = {
      current: pagination.current,
      size: pagination.pageSize,
      ...(selectedCategoryId.value ? { categoryId: selectedCategoryId.value } : {}),
      ...searchParams,
    }
    // 清理空值参数
    Object.keys(apiParams).forEach(key => {
      if (apiParams[key] === '' || apiParams[key] === null || apiParams[key] === false) {
        // 保留 showRed/onlyVehicleWarehouse/hasSourceOrder/isGift/isPromoProduct 为 false 时不传
        delete apiParams[key]
      }
    })
    // 显式传布尔勾选字段
    if (searchParams.showRed) apiParams.showRed = true
    if (searchParams.onlyVehicleWarehouse) apiParams.onlyVehicleWarehouse = true
    if (searchParams.hasSourceOrder) apiParams.hasSourceOrder = true
    if (searchParams.isGift) apiParams.isGift = true
    if (searchParams.isPromoProduct) apiParams.isPromoProduct = true

    const res: any = await request.get('/sales/detail-query/page', { params: apiParams })
    if (res) {
      const data = res.data || res
      tableData.value = data.records || data.content || data.list || []
      pagination.total = data.total || 0
      lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    }
  } catch (e: any) {
    hasError.value = true
    console.warn('[销售明细查询] 获取失败', e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  searchParams.dateType = 'documentDate'
  searchParams.startDate = ''
  searchParams.endDate = ''
  searchParams.documentNo = ''
  searchParams.documentType = ''
  searchParams.warehouseName = ''
  searchParams.customerName = ''
  searchParams.receiverName = ''
  searchParams.receiverPhone = ''
  searchParams.shippingAddress = ''
  searchParams.handlerName = ''
  searchParams.defaultHandlerName = ''
  searchParams.departmentName = ''
  searchParams.creatorName = ''
  searchParams.bookkeeperName = ''
  searchParams.productName = ''
  searchParams.productCode = ''
  searchParams.barcode = ''
  searchParams.brand = ''
  searchParams.industryCategory = ''
  searchParams.productAttribute = ''
  searchParams.minPrice = null
  searchParams.maxPrice = null
  searchParams.isGift = false
  searchParams.isPromoProduct = false
  searchParams.deliveryMethod = ''
  searchParams.logisticsCompany = ''
  searchParams.trackingNumber = ''
  searchParams.deliveryDriver = ''
  searchParams.region = ''
  searchParams.remark = ''
  searchParams.itemRemark = ''
  searchParams.buyerRemark = ''
  searchParams.sourceOrder = ''
  searchParams.settlementStatus = ''
  searchParams.generationMethod = ''
  searchParams.salesType = ''
  searchParams.settlementTimeStart = ''
  searchParams.settlementTimeEnd = ''
  searchParams.source = ''
  searchParams.itemExtNum1Min = null
  searchParams.itemExtNum1Max = null
  searchParams.itemExtNum2Min = null
  searchParams.itemExtNum2Max = null
  searchParams.itemExtNum3Min = null
  searchParams.itemExtNum3Max = null
  searchParams.itemExtText1 = ''
  searchParams.itemExtText2 = ''
  searchParams.showRed = false
  searchParams.onlyVehicleWarehouse = false
  searchParams.hasSourceOrder = false
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

function handleCategoryExpand(keys: (string | number)[]) {
  categoryExpandedKeys.value = keys
}

// ═══ 查询方案 + 快捷日期 ═══
const queryScheme = ref('')
const quickDate = ref('')
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
  pagination.current = 1
  fetchData()
}

// ═══ 底部合计 ═══
const tableFooterColumns = computed(() => {
  const total = tableData.value.reduce((s: number, r: any) => s + (r.amount || 0), 0)
  return total ? [{ label: '合计', value: total.toFixed(2) }] : []
})

// ═══ 操作列：批次号 / 明细备注 ═══
function viewBatch(record: any) {
  console.warn('查看批次号', record)
}

function viewItemRemark(record: any) {
  console.warn('查看明细备注', record)
}

onMounted(() => {
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
.table-area { flex: 1; min-height: 0; display: flex; flex-direction: row; gap: 12px; align-items: stretch; background: #fff; padding: 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.table-toolbar { display: flex; justify-content: flex-end; margin-bottom: 8px; }
.update-time { font-size: 12px; color: #999; }
</style>
