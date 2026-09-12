<template>
  <ErrorBoundary @error="handleError">
    <CategoryListLayout
      :show-category-panel="false"
      :show-table-footer="false"
    >
      <template #toolbar-right>
        <a-space :size="8">
          <a-button size="small" class="toolbar-btn" @click="togglePageConfig">
            <template #icon><SettingOutlined /></template>
            <span style="font-size:12px">页面配置</span>
          </a-button>
          <a-badge :status="loading ? 'processing' : (hasError ? 'error' : 'success')" />
          <span v-if="lastUpdateTime" class="update-time">最后更新: {{ lastUpdateTime }}</span>
          <a-button size="small" class="toolbar-btn" @click="fetchData">
            <template #icon><ReloadOutlined /></template>
          </a-button>
        </a-space>
      </template>

      <!-- ═══ 搜索区域 ═══ -->
      <template #search-fields>
        <div class="search-area">
          <div class="search-container" :data-expanded="showMoreConditions || null">
            <div class="search-grid" ref="gridRef">
              <!-- 默认显示字段 -->
              <div class="search-field-item" v-if="isFieldVisible('dateType')">
                <a-range-picker v-model:value="dateRange" size="small" style="width:100%" @change="handleDateChange" />
              </div>
              <div class="search-field-item" v-if="isFieldVisible('orderNo')">
                <a-input v-model:value="searchParams.orderNo" placeholder="单据编号" allow-clear size="small" />
              </div>
              <div class="search-field-item" v-if="isFieldVisible('productName')">
                <a-input v-model:value="searchParams.productName" placeholder="商品" allow-clear size="small" />
              </div>
              <div class="search-field-item" v-if="isFieldVisible('supplierName')">
                <a-input v-model:value="searchParams.supplierName" placeholder="供应商" allow-clear size="small" />
              </div>
              <div class="search-field-item" v-if="isFieldVisible('isGift')">
                <a-checkbox v-model:checked="searchParams.isGift">赠品</a-checkbox>
              </div>
              <div class="search-field-item" v-if="isFieldVisible('purchaserName')">
                <a-input v-model:value="searchParams.purchaserName" placeholder="经手人" allow-clear size="small" />
              </div>
              <div class="search-field-item" v-if="isFieldVisible('deptName')">
                <a-input v-model:value="searchParams.deptName" placeholder="部门" allow-clear size="small" />
              </div>
              <div class="search-field-item" v-if="isFieldVisible('createByName')">
                <a-input v-model:value="searchParams.createByName" placeholder="制单人" allow-clear size="small" />
              </div>
              <div class="search-field-item" v-if="isFieldVisible('warehouseName')">
                <a-input v-model:value="searchParams.warehouseName" placeholder="仓库" allow-clear size="small" />
              </div>
              <div class="search-field-item" v-if="isFieldVisible('sourceBillNo')">
                <a-input v-model:value="searchParams.sourceBillNo" placeholder="来源订单" allow-clear size="small" />
              </div>

              <!-- 展开区域 -->
              <template v-if="showMoreConditions">
                <div class="search-field-item" v-if="isFieldVisible('bookkeeperName')">
                  <a-input v-model:value="searchParams.bookkeeperName" placeholder="记账人" allow-clear size="small" />
                </div>
                <div class="search-field-item" v-if="isFieldVisible('settlementStatus')">
                  <a-select v-model:value="searchParams.settlementStatus" size="small" allow-clear placeholder="结算状态">
                    <a-select-option value="">全部</a-select-option>
                    <a-select-option value="UNPAID">未结算</a-select-option>
                    <a-select-option value="PARTIAL">部分结算</a-select-option>
                    <a-select-option value="PAID">已结算</a-select-option>
                  </a-select>
                </div>
                <div class="search-field-item" v-if="isFieldVisible('docType')">
                  <a-select v-model:value="searchParams.docType" size="small" allow-clear placeholder="单据类型">
                    <a-select-option value="">全部</a-select-option>
                    <a-select-option value="PURCHASE">采购订单</a-select-option>
                    <a-select-option value="INBOUND">采购入库</a-select-option>
                    <a-select-option value="RETURN">采购退货</a-select-option>
                  </a-select>
                </div>
                <div class="search-field-item" v-if="isFieldVisible('itemRemark')">
                  <a-input v-model:value="searchParams.itemRemark" placeholder="明细备注" allow-clear size="small" />
                </div>
                <div class="search-field-item" v-if="isFieldVisible('remark')">
                  <a-input v-model:value="searchParams.remark" placeholder="单据备注" allow-clear size="small" />
                </div>
                <div class="search-field-item" v-if="isFieldVisible('extNum1')">
                  <div class="range-wrap">
                    <a-input-number v-model:value="searchParams.extNum1Min" placeholder="最小" size="small" />
                    <span class="range-sep">-</span>
                    <a-input-number v-model:value="searchParams.extNum1Max" placeholder="最大" size="small" />
                  </div>
                </div>
                <div class="search-field-item" v-if="isFieldVisible('extNum2')">
                  <div class="range-wrap">
                    <a-input-number v-model:value="searchParams.extNum2Min" placeholder="最小" size="small" />
                    <span class="range-sep">-</span>
                    <a-input-number v-model:value="searchParams.extNum2Max" placeholder="最大" size="small" />
                  </div>
                </div>
                <div class="search-field-item" v-if="isFieldVisible('extNum3')">
                  <div class="range-wrap">
                    <a-input-number v-model:value="searchParams.extNum3Min" placeholder="最小" size="small" />
                    <span class="range-sep">-</span>
                    <a-input-number v-model:value="searchParams.extNum3Max" placeholder="最大" size="small" />
                  </div>
                </div>
                <div class="search-field-item" v-if="isFieldVisible('extText1')">
                  <a-input v-model:value="searchParams.extText1" placeholder="表体自定义4(文本)" allow-clear size="small" />
                </div>
                <div class="search-field-item" v-if="isFieldVisible('extText2')">
                  <a-input v-model:value="searchParams.extText2" placeholder="表体自定义5(文本)" allow-clear size="small" />
                </div>
                <div class="search-field-item" v-if="isFieldVisible('extNum6')">
                  <div class="range-wrap">
                    <a-input-number v-model:value="searchParams.extNum6Min" placeholder="最小" size="small" />
                    <span class="range-sep">-</span>
                    <a-input-number v-model:value="searchParams.extNum6Max" placeholder="最大" size="small" />
                  </div>
                </div>
                <div class="search-field-item" v-if="isFieldVisible('extNum7')">
                  <div class="range-wrap">
                    <a-input-number v-model:value="searchParams.extNum7Min" placeholder="最小" size="small" />
                    <span class="range-sep">-</span>
                    <a-input-number v-model:value="searchParams.extNum7Max" placeholder="最大" size="small" />
                  </div>
                </div>
                <div class="search-field-item" v-if="isFieldVisible('extPartner')">
                  <a-input v-model:value="searchParams.extPartner" placeholder="表体自定义8(往来单位)" allow-clear size="small" />
                </div>
                <div class="search-field-item" v-if="isFieldVisible('extStaff')">
                  <a-input v-model:value="searchParams.extStaff" placeholder="表体自定义9(职员)" allow-clear size="small" />
                </div>
                <div class="search-field-item" v-if="isFieldVisible('extDept')">
                  <a-input v-model:value="searchParams.extDept" placeholder="表体自定义10(部门)" allow-clear size="small" />
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
                  <a-checkbox v-model:checked="searchParams.showRed">显示红冲</a-checkbox>
                </div>
                <div class="search-field-item">
                  <a-checkbox v-model:checked="searchParams.onlyGift">仅看赠品</a-checkbox>
                </div>
                <div class="search-field-item">
                  <a-checkbox v-model:checked="searchParams.hasSourceOrder">有来源订单</a-checkbox>
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
          :columns="columns"
          :storage-key="'purchase-detail-query-table-columns'"
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
          row-key="itemId"
          :scroll="{ x: 9000 }"
          @page-change="handlePageChange"
        >
        </BillTableList>
      </template>
    </CategoryListLayout>

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldsConfig"
      :function-buttons-config="functionButtonConfig"
      storage-key="purchase-detail-query-page-config"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import type { Dayjs } from 'dayjs'
import {
  ReloadOutlined, SearchOutlined, ClearOutlined,
  DownOutlined, UpOutlined, SettingOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { purchaseDocQueryApi } from '@/api/purchase'

// ═══ 单据状态（与后端 OrderStatus 枚举一致：0草稿/1待审批/2已审批/3已下达/4执行中/5部分入库/6已完成/7已取消） ═══
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '待审批', color: 'orange' },
  2: { label: '已审批', color: 'blue' },
  3: { label: '已下达', color: 'blue' },
  4: { label: '执行中', color: 'blue' },
  5: { label: '部分入库', color: 'cyan' },
  6: { label: '已完成', color: 'green' },
  7: { label: '已取消', color: 'red' }
}

const MONEY_COLUMNS = ['unitPrice', 'smallUnitPrice', 'amount', 'discountedUnitPrice', 'discountedAmount']
const QTY_COLUMNS = ['quantity', 'receivedQuantity', 'unreceiveQuantity', 'terminatedQuantity', 'smallUnitQuantity', 'bigPack', 'midPack', 'smallPack']

const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')
const showMoreConditions = ref(false)
const showPageConfig = ref(false)

const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

// ═══ 搜索参数 ═══
const searchParams = reactive({
  startDate: '', endDate: '',
  orderNo: '',
  sourceBillNo: '',
  productName: '',
  supplierName: '',
  isGift: false,
  purchaserName: '',
  deptName: '',
  createByName: '',
  bookkeeperName: '',
  warehouseName: '',
  settlementStatus: '',
  docType: '',
  itemRemark: '',
  remark: '',
  extNum1Min: null as number | null, extNum1Max: null as number | null,
  extNum2Min: null as number | null, extNum2Max: null as number | null,
  extNum3Min: null as number | null, extNum3Max: null as number | null,
  extText1: '', extText2: '',
  extNum6Min: null as number | null, extNum6Max: null as number | null,
  extNum7Min: null as number | null, extNum7Max: null as number | null,
  extPartner: '', extStaff: '', extDept: '',
  showRed: false,
  onlyGift: false,
  hasSourceOrder: false,
})

const dateRange = ref<[Dayjs, Dayjs] | null>(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current, pageSize: pagination.pageSize, total: pagination.total
}))

// ═══ 页面配置：查询字段（26个条目，与文档查询条件对齐） ═══
const queryFieldsConfig = ref([
  { key: 'dateType', label: '日期', visible: true },
  { key: 'orderNo', label: '单据编号', visible: true },
  { key: 'productName', label: '商品', visible: true },
  { key: 'isGift', label: '是否赠品', visible: true },
  { key: 'supplierName', label: '供应商', visible: true },
  { key: 'purchaserName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: true },
  { key: 'createByName', label: '制单人', visible: true },
  { key: 'bookkeeperName', label: '记账人', visible: false },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'settlementStatus', label: '结算状态', visible: false },
  { key: 'sourceBillNo', label: '来源订单', visible: true },
  { key: 'docType', label: '单据类型', visible: false },
  { key: 'itemRemark', label: '明细备注', visible: false },
  { key: 'remark', label: '单据备注', visible: false },
  { key: 'extNum1', label: '表体自定义1(数字)', visible: false },
  { key: 'extNum2', label: '表体自定义2(数字)', visible: false },
  { key: 'extNum3', label: '表体自定义3(数字)', visible: false },
  { key: 'extText1', label: '表体自定义4(文本)', visible: false },
  { key: 'extText2', label: '表体自定义5(文本)', visible: false },
  { key: 'extNum6', label: '表体自定义6(数字)', visible: false },
  { key: 'extNum7', label: '表体自定义7(数字)', visible: false },
  { key: 'extPartner', label: '表体自定义8(往来单位)', visible: false },
  { key: 'extStaff', label: '表体自定义9(职员)', visible: false },
  { key: 'extDept', label: '表体自定义10(部门)', visible: false },
  { key: 'showRed', label: '显示红冲', visible: false },
])

// ═══ 页面配置：功能按钮 ═══
const functionButtonConfig = ref([
  { key: 'search', label: '查询', enabled: true },
  { key: 'reset', label: '重置', enabled: true },
  { key: 'more', label: '更多条件', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '页面配置', enabled: true },
])

// ═══ 58列表格定义（key 与后端字段对齐；无数据源字段用 defaultHidden 隐藏） ═══
const columns = [
  // 序号列
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' as const },
  // 单据信息 1-14
  { title: '单据日期', field: 'orderDate', key: 'orderDate', width: 120 },
  { title: '单据编号', field: 'orderNo', key: 'orderNo', width: 160 },
  { title: '入库仓库', field: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '出库仓库', field: 'outWarehouseName', key: 'outWarehouseName', width: 120, defaultHidden: true },
  { title: '供应商名称', field: 'supplierName', key: 'supplierName', width: 150, ellipsis: true },
  { title: '供应商编号', field: 'supplierCode', key: 'supplierCode', width: 110, defaultHidden: true },
  { title: '联系人', field: 'contactName', key: 'contactName', width: 100, defaultHidden: true },
  { title: '联系电话', field: 'contactPhone', key: 'contactPhone', width: 120, defaultHidden: true },
  { title: '联系地址', field: 'contactAddress', key: 'contactAddress', width: 180, ellipsis: true, defaultHidden: true },
  { title: '供应商备注', field: 'supplierRemark', key: 'supplierRemark', width: 140, ellipsis: true, defaultHidden: true },
  { title: '来源订单', field: 'sourceBillNo', key: 'sourceBillNo', width: 140, ellipsis: true },
  { title: '经手人', field: 'purchaserName', key: 'purchaserName', width: 100 },
  { title: '部门', field: 'deptName', key: 'deptName', width: 100 },
  { title: '结算状态', field: 'settlementStatus', key: 'settlementStatus', width: 100, defaultHidden: true },
  // 商品信息 15-21
  { title: '商品名称', field: 'productName', key: 'productName', width: 180, ellipsis: true },
  { title: '货号', field: 'itemCode', key: 'itemCode', width: 110 },
  { title: '条码', field: 'barcode', key: 'barcode', width: 120 },
  { title: '规格', field: 'specification', key: 'specification', width: 110, ellipsis: true },
  { title: '型号', field: 'model', key: 'model', width: 100, defaultHidden: true },
  { title: '产地', field: 'origin', key: 'origin', width: 100, defaultHidden: true },
  { title: '品牌', field: 'brand', key: 'brand', width: 100 },
  // 表体自定义 22-31
  { title: '表体自定义1(数字)', field: 'customField1', key: 'customField1', width: 120, align: 'right', defaultHidden: true },
  { title: '表体自定义2(数字)', field: 'customField2', key: 'customField2', width: 120, align: 'right', defaultHidden: true },
  { title: '表体自定义3(数字)', field: 'customField3', key: 'customField3', width: 120, align: 'right', defaultHidden: true },
  { title: '表体自定义4(文本)', field: 'customField4', key: 'customField4', width: 140, defaultHidden: true },
  { title: '表体自定义5(文本)', field: 'customField5', key: 'customField5', width: 140, defaultHidden: true },
  { title: '表体自定义6(数字)', field: 'customField6', key: 'customField6', width: 120, align: 'right', defaultHidden: true },
  { title: '表体自定义7(数字)', field: 'customField7', key: 'customField7', width: 120, align: 'right', defaultHidden: true },
  { title: '表体自定义8(往来单位)', field: 'customField8', key: 'customField8', width: 140, defaultHidden: true },
  { title: '表体自定义9(职员)', field: 'customField9', key: 'customField9', width: 120, defaultHidden: true },
  { title: '表体自定义10(部门)', field: 'customField10', key: 'customField10', width: 130, defaultHidden: true },
  // 数量/包装 32-43
  { title: '单位', field: 'unit', key: 'unit', width: 70 },
  { title: '数量', field: 'quantity', key: 'quantity', width: 100, align: 'right' },
  { title: '批次条码', field: 'batchCode', key: 'batchCode', width: 130, defaultHidden: true },
  { title: '生产日期', field: 'productionDate', key: 'productionDate', width: 120, defaultHidden: true },
  { title: '到期日期', field: 'expiryDate', key: 'expiryDate', width: 120, defaultHidden: true },
  { title: '换算关系', field: 'conversionRelation', key: 'conversionRelation', width: 100, defaultHidden: true },
  { title: '换算结果', field: 'convertedQuantity', key: 'convertedQuantity', width: 100, align: 'right', defaultHidden: true },
  { title: '大包装', field: 'bigPack', key: 'bigPack', width: 90, align: 'right', defaultHidden: true },
  { title: '中包装', field: 'midPack', key: 'midPack', width: 90, align: 'right', defaultHidden: true },
  { title: '小包装', field: 'smallPack', key: 'smallPack', width: 90, align: 'right', defaultHidden: true },
  { title: '小单位', field: 'smallUnit', key: 'smallUnit', width: 90, defaultHidden: true },
  { title: '小单位数量', field: 'smallUnitQuantity', key: 'smallUnitQuantity', width: 110, align: 'right', defaultHidden: true },
  // 价格 44-51
  { title: '单价', field: 'unitPrice', key: 'unitPrice', width: 100, align: 'right' },
  { title: '小单位单价', field: 'smallUnitPrice', key: 'smallUnitPrice', width: 110, align: 'right', defaultHidden: true },
  { title: '金额', field: 'amount', key: 'amount', width: 110, align: 'right' },
  { title: '优惠折扣(%)', field: 'discountRate', key: 'discountRate', width: 100, align: 'right', defaultHidden: true },
  { title: '优惠后单价', field: 'discountedUnitPrice', key: 'discountedUnitPrice', width: 110, align: 'right' },
  { title: '优惠后金额', field: 'discountedAmount', key: 'discountedAmount', width: 110, align: 'right' },
  { title: '重量(kg)', field: 'weight', key: 'weight', width: 90, align: 'right' },
  { title: '体积(m³)', field: 'volume', key: 'volume', width: 90, align: 'right' },
  // 备注/系统 52-58
  { title: '明细备注', field: 'itemRemark', key: 'itemRemark', width: 150, ellipsis: true },
  { title: '单据备注', field: 'remark', key: 'remark', width: 150, ellipsis: true },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 100, defaultHidden: true },
  { title: '制单人', field: 'createByName', key: 'createByName', width: 100 },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 150, defaultHidden: true },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 150 },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 90, align: 'right' },
]

// ═══ 搜索字段可见性（页面配置驱动） ═══
function isFieldVisible(key: string): boolean {
  const f = queryFieldsConfig.value.find(x => x.key === key)
  return f ? f.visible : true
}

// ═══ 事件处理 ═══
const handleDateChange = (dates: [Dayjs, Dayjs] | null) => {
  if (dates?.length === 2) {
    searchParams.startDate = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.endDate = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    searchParams.startDate = ''
    searchParams.endDate = ''
  }
}

const toggleMoreConditions = () => {
  showMoreConditions.value = !showMoreConditions.value
}

const handlePageConfigChange = () => {
  // 页面配置变化后，重新同步搜索字段显隐（isFieldVisible 为响应式读取）
}

function togglePageConfig() { showPageConfig.value = true }

// ═══ 数据获取 ═══
const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const apiParams: Record<string, any> = {
      current: pagination.current,
      size: pagination.pageSize,
      dateStart: searchParams.startDate ? `${searchParams.startDate}T00:00:00` : undefined,
      dateEnd: searchParams.endDate ? `${searchParams.endDate}T23:59:59` : undefined,
      orderNo: searchParams.orderNo,
      sourceBillNo: searchParams.sourceBillNo,
      productName: searchParams.productName,
      supplierName: searchParams.supplierName,
      purchaserName: searchParams.purchaserName,
      deptName: searchParams.deptName,
      createByName: searchParams.createByName,
      bookkeeperName: searchParams.bookkeeperName,
      warehouseName: searchParams.warehouseName,
      settlementStatus: searchParams.settlementStatus,
      remark: searchParams.remark,
      itemRemark: searchParams.itemRemark,
      isGift: searchParams.isGift ? 1 : undefined,
      // 表体自定义范围（后端暂不精确过滤，传参无害）
      extNum1Min: searchParams.extNum1Min, extNum1Max: searchParams.extNum1Max,
      extNum2Min: searchParams.extNum2Min, extNum2Max: searchParams.extNum2Max,
      extNum3Min: searchParams.extNum3Min, extNum3Max: searchParams.extNum3Max,
      extText1: searchParams.extText1, extText2: searchParams.extText2,
      extNum6Min: searchParams.extNum6Min, extNum6Max: searchParams.extNum6Max,
      extNum7Min: searchParams.extNum7Min, extNum7Max: searchParams.extNum7Max,
    }
    // 清理空值参数
    Object.keys(apiParams).forEach(key => {
      if (apiParams[key] === '' || apiParams[key] === null || apiParams[key] === undefined) {
        delete apiParams[key]
      }
    })
    const res: any = await purchaseDocQueryApi.detailPage(apiParams)
    if (res) {
      const data = res.data || res
      let records: any[] = data.records || data.content || data.list || []
      records = normalizeRecords(records)
      tableData.value = records
      pagination.total = data.total || 0
      lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    }
  } catch (e: any) {
    hasError.value = true
    console.warn('[采购明细查询] 获取失败', e)
  } finally {
    loading.value = false
  }
}

// ═══ 日期字段规整（后端 LocalDateTime → YYYY-MM-DD 展示） ═══
function normalizeRecords(records: any[]): any[] {
  const dateFields = ['orderDate', 'createTime', 'submitTime']
  return records.map(r => {
    const copy: any = { ...r }
    dateFields.forEach(f => {
      if (copy[f]) copy[f] = String(copy[f]).slice(0, 10)
    })
    return copy
  })
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  searchParams.startDate = ''
  searchParams.endDate = ''
  searchParams.orderNo = ''
  searchParams.sourceBillNo = ''
  searchParams.productName = ''
  searchParams.supplierName = ''
  searchParams.isGift = false
  searchParams.purchaserName = ''
  searchParams.deptName = ''
  searchParams.createByName = ''
  searchParams.bookkeeperName = ''
  searchParams.warehouseName = ''
  searchParams.settlementStatus = ''
  searchParams.docType = ''
  searchParams.itemRemark = ''
  searchParams.remark = ''
  searchParams.extNum1Min = null
  searchParams.extNum1Max = null
  searchParams.extNum2Min = null
  searchParams.extNum2Max = null
  searchParams.extNum3Min = null
  searchParams.extNum3Max = null
  searchParams.extText1 = ''
  searchParams.extText2 = ''
  searchParams.extNum6Min = null
  searchParams.extNum6Max = null
  searchParams.extNum7Min = null
  searchParams.extNum7Max = null
  searchParams.extPartner = ''
  searchParams.extStaff = ''
  searchParams.extDept = ''
  searchParams.showRed = false
  searchParams.onlyGift = false
  searchParams.hasSourceOrder = false
  dateRange.value = null
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

// ═══ 底部合计 ═══
const tableFooterColumns = computed(() => {
  const totalAmount = tableData.value.reduce((s: number, r: any) => s + (Number(r.amount) || 0), 0)
  const totalQty = tableData.value.reduce((s: number, r: any) => s + (Number(r.quantity) || 0), 0)
  if (!totalAmount && !totalQty) return []
  return [
    ...(totalQty ? [{ label: '数量合计', value: totalQty.toFixed(2) }] : []),
    ...(totalAmount ? [{ label: '金额合计', value: totalAmount.toFixed(2) }] : []),
  ]
})

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-container:not(:has([data-expanded])) > .search-grid { max-height: 84px; overflow: hidden; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; align-items: center; }
.search-field-item { display: flex; min-width: 0; }
.search-field-item :deep(.ant-input-wrapper),
.search-field-item :deep(.ant-input-affix-wrapper) { width: 100%; font-size: 13px; }
.search-field-item :deep(.ant-select) { width: 100%; }
.search-field-item :deep(.ant-select .ant-select-selector) { font-size: 13px; }
.search-field-item :deep(.ant-picker) { width: 100%; }
.range-wrap { display: flex; align-items: center; width: 100%; gap: 4px; }
.range-wrap :deep(.ant-input-number) { flex: 1; min-width: 0; }
.range-sep { color: #999; flex-shrink: 0; }
.search-action-item { flex-shrink: 0; }
.search-action-group { display: flex; flex-wrap: nowrap; align-items: center; }
.search-action-group .search-field-item { width: auto; flex: 0 0 auto; margin-right: 4px; }
.search-more-toggle { margin-top: 4px; display: flex; align-items: center; justify-content: center; gap: 8px; }
.search-more-toggle::before,
.search-more-toggle::after { content: ''; flex: 1; height: 1px; background: #e8e8e8; }
.update-time { font-size: 12px; color: #999; }
.toolbar-btn { font-size: 13px; }
</style>
