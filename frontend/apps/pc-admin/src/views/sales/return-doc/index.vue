<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- ═══ 工具栏 ═══ -->
      <div class="toolbar-wrapper">
        <!-- 工具栏左侧：查询方案 + 快捷日期 -->
        <div class="toolbar-left">
          <div class="query-scheme-wrap">
            <a-select
              v-model:value="queryScheme"
              style="width: 140px"
              size="small"
              placeholder="--查询方案--"
            >
              <a-select-option value="">--查询方案--</a-select-option>
            </a-select>
            <a-button type="link" size="small" style="padding: 0 4px">
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
        </div>
        <!-- 工具栏右侧：操作按钮 -->
        <div class="toolbar-right">
          <a-space :size="8">
            <a-tooltip title="配置">
              <a-button size="small" @click="showPageConfig = true">
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-tooltip title="列配置">
              <a-button size="small" @click="showColumnConfig = true">
                <ColumnWidthOutlined />
              </a-button>
            </a-tooltip>
            <a-button type="primary" size="small" @click="handleAdd">
              <PlusOutlined /> 新增
            </a-button>
            <a-button size="small" @click="fetchData">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button size="small" @click="handleProductSummary">
              <AppstoreOutlined /> 商品汇总
            </a-button>
            <a-button size="small">
              <PrinterOutlined /> 批量打印
            </a-button>
            <a-tooltip title="打印">
              <a-button size="small">
                <PrinterOutlined /> 打印(F8)
              </a-button>
            </a-tooltip>
            <a-button size="small">
              <ExportOutlined /> 导出
            </a-button>
          </a-space>
        </div>
      </div>

      <!-- ═══ 搜索区域 ═══ -->
      <div class="search-area">
        <div class="search-container" :data-expanded="searchExpanded || null">
        <div class="search-grid" ref="gridRef">
          <!-- 基础字段（始终可见） -->
          <div class="search-field-item">
            <a-range-picker
              v-model:value="dateRange"
              size="small"
              style="width: 100%"
              @change="handleDateChange"
            />
          </div>
          <div class="search-field-item">
            <a-input v-model:value="searchParams.returnDocNo" placeholder="单据编号" allow-clear size="small" />
          </div>
          <div class="search-field-item">
            <a-input v-model:value="searchParams.customerName" placeholder="客户" allow-clear size="small" :suffix="h(SearchOutlined, { style: 'color:#bbb' })" />
          </div>
          <div class="search-field-item">
            <a-input v-model:value="searchParams.handlerName" placeholder="经手人" allow-clear size="small" :suffix="h(SearchOutlined, { style: 'color:#bbb' })" />
          </div>
          <!-- 扩展字段 -->
          <div v-if="searchExpanded" class="search-field-item">
            <a-input v-model:value="searchParams.deptName" placeholder="部门" allow-clear size="small" />
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <a-input v-model:value="searchParams.creatorName" placeholder="制单人" allow-clear size="small" />
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <a-input v-model:value="searchParams.bookkeeperName" placeholder="记账人" allow-clear size="small" />
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <a-input v-model:value="searchParams.warehouseName" placeholder="仓库" allow-clear size="small" :suffix="h(SearchOutlined, { style: 'color:#bbb' })" />
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <div class="search-select-wrap">
              <span class="search-select-label">单据状态</span>
              <a-select v-model:value="searchParams.status" size="small" allow-clear placeholder="全部">
                <a-select-option :value="0">草稿</a-select-option>
                <a-select-option :value="1">待审核</a-select-option>
                <a-select-option :value="2">已审核</a-select-option>
                <a-select-option :value="3">已完成</a-select-option>
                <a-select-option :value="4">已取消</a-select-option>
              </a-select>
            </div>
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <div class="search-select-wrap">
              <span class="search-select-label">结算状态</span>
              <a-select v-model:value="searchParams.settleStatus" size="small" allow-clear placeholder="全部" />
            </div>
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <div class="search-select-wrap">
              <span class="search-select-label">配送方式</span>
              <a-select v-model:value="searchParams.deliveryMethod" size="small" allow-clear placeholder="全部" />
            </div>
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <div class="search-select-wrap">
              <span class="search-select-label">销售类型</span>
              <a-select v-model:value="searchParams.salesType" size="small" allow-clear placeholder="全部" />
            </div>
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <div class="search-select-wrap">
              <span class="search-select-label">商品行属性</span>
              <a-select v-model:value="searchParams.productLineAttr" size="small" allow-clear placeholder="全部" />
            </div>
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <a-input v-model:value="searchParams.remark" placeholder="单据备注" allow-clear size="small" />
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <a-input v-model:value="searchParams.summary" placeholder="摘要" allow-clear size="small" />
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <a-input-number v-model:value="searchParams.extNum1" size="small" style="width: 100%" placeholder="自定义1(数字)" />
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <a-input-number v-model:value="searchParams.extNum2" size="small" style="width: 100%" placeholder="自定义2(数字)" />
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <a-input v-model:value="searchParams.extText1" placeholder="自定义3(文本)" allow-clear size="small" />
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <a-input v-model:value="searchParams.extText2" placeholder="自定义4(文本)" allow-clear size="small" />
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <a-input v-model:value="searchParams.extText3" placeholder="自定义5(文本)" allow-clear size="small" />
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <a-input v-model:value="searchParams.receiverName" placeholder="收货人" allow-clear size="small" />
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <a-input v-model:value="searchParams.receiverPhone" placeholder="联系电话" allow-clear size="small" />
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <a-input v-model:value="searchParams.shippingAddress" placeholder="收货地址" allow-clear size="small" />
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <a-input v-model:value="searchParams.logisticsCompany" placeholder="物流公司" allow-clear size="small" />
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <a-input v-model:value="searchParams.waybillNo" placeholder="运单号" allow-clear size="small" />
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <a-input v-model:value="searchParams.region" placeholder="区域" allow-clear size="small" />
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <div class="search-select-wrap">
              <span class="search-select-label">产生方式</span>
              <a-select v-model:value="searchParams.generateType" size="small" allow-clear placeholder="全部" />
            </div>
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <a-input-number v-model:value="searchParams.printCount" size="small" style="width: 100%" placeholder="打印次数" :min="0" />
          </div>
          <div class="search-action-group" ref="actionRef" :style="{ gridColumn: 'span ' + actionSpan }">
          <div class="search-field-item search-action-item">
            <a-space :size="4">
              <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
              <a-button size="small" @click="fetchData">刷新</a-button>
            </a-space>
          </div>
          <div v-if="searchExpanded" class="search-field-item">
            <label>
              <a-checkbox v-model:checked="searchParams.showRedFlush" /> 显示红冲
            </label>
          </div>
          </div>
        </div>
          <div class="search-more-toggle">
            <a-button v-if="!searchExpanded" type="link" size="small" @click="searchExpanded = true">更多条件</a-button>
            <a-button v-else type="link" size="small" @click="searchExpanded = false">收起</a-button>
          </div>
        </div>
      </div>

      <!-- ══ 表格区域 ═══ -->
      <div class="table-area">
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
          :selectable="true"
          :row-selection="rowSelection"
          row-key="id"
          @page-change="handlePageChange"
        >
          <!-- 单据编号 -->
          <template #orderNoCell="{ record }">
            <a-button type="link" size="small" @click="handleView(record)">
              {{ record.returnDocNo }}
            </a-button>
          </template>
          <!-- 单据状态 -->
          <template #statusCell="{ record }">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <!-- 结算状态 -->
          <template #settleStatusCell="{ record }">
            <a-tag v-if="record.settleStatus" :color="record.settleStatus === 'settled' ? 'success' : 'processing'">
              {{ record.settleStatus === 'settled' ? '已结算' : '未结算' }}
            </a-tag>
            <span v-else>-</span>
          </template>
          <!-- 附件 -->
          <template #attachmentCell="{ record }">
            <a-tooltip v-if="record.attachment" title="有附件">
              <PaperClipOutlined style="color: #1890ff" />
            </a-tooltip>
            <span v-else>-</span>
          </template>
          <!-- 操作列 -->
          <template #actionCell="{ record }">
            <a-space :size="4">
              <a-button type="link" size="small" @click="handleView(record)">详情</a-button>
              <a-button v-if="record.status === 0" type="link" size="small" @click="handleEdit(record)">编辑</a-button>
              <a-button v-if="record.status === 0" type="link" size="small">复制</a-button>
              <a-button v-if="record.status === 0" type="link" size="small" danger @click="handleDelete(record)">删除</a-button>
              <a-dropdown v-if="record.status > 0">
                <a-button type="link" size="small">更多</a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item v-if="record.status === 1" @click="handleApprove(record)">审核通过</a-menu-item>
                    <a-menu-item v-if="record.status === 1" @click="handleReject(record)">审核拒绝</a-menu-item>
                    <a-menu-item v-if="record.status === 2" @click="handleComplete(record)">完成</a-menu-item>
                    <a-menu-item v-if="record.status !== 3 && record.status !== 4" @click="handleCancel(record)">取消</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </BillTableList>
      </div>
    </PageContainer>

    <!-- ═══ 列配置弹窗 ═══ -->
    <ColumnConfigPanel
      :open="showColumnConfig"
      :settings-columns="settingsColumns"
      :is-locked-column="isLockedColumn"
      @update:open="showColumnConfig = $event"
      @change="onSettingChange"
      @reset="resetSettings"
      @drag-end="onSettingChange"
    />

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="pageQueryFields"
      :function-buttons-config="pageFunctionButtons"
      storage-key="sale-return-doc-page-config"
      @update:open="showPageConfig = $event"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  PlusOutlined, ReloadOutlined, PrinterOutlined, ExportOutlined,
  SettingOutlined, SearchOutlined, ColumnWidthOutlined,
  AppstoreOutlined, PaperClipOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useColumnConfig, isLockedColumn } from '@/composables/useColumnConfig'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { saleReturnDocApi } from '@/api/erp'
import { useRouter } from 'vue-router'

const router = useRouter()

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

// ══ 搜索展开 ═══
const searchExpanded = ref(false)

// ═══ 动态 grid-column span ═══
const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

// ═══ 状态 ═══
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])

// ═══ 日期范围 ═══
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs(), dayjs()])

// ═══ 搜索参数 ══
const searchParams = reactive({
  returnDocNo: '',
  customerName: '',
  handlerName: '',
  deptName: '',
  creatorName: '',
  bookkeeperName: '',
  warehouseName: '',
  status: undefined as number | undefined,
  settleStatus: undefined as string | undefined,
  deliveryMethod: undefined as string | undefined,
  salesType: undefined as string | undefined,
  productLineAttr: undefined as string | undefined,
  remark: '',
  summary: '',
  extNum1: undefined as number | undefined,
  extNum2: undefined as number | undefined,
  extText1: '',
  extText2: '',
  extText3: '',
  receiverName: '',
  receiverPhone: '',
  shippingAddress: '',
  logisticsCompany: '',
  waybillNo: '',
  region: '',
  generateType: undefined as string | undefined,
  printCount: undefined as number | undefined,
  showRedFlush: false,
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
})

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total
}))

// ═══ 多选状态 ═══
const selectedRowKeys = ref<any[]>([])
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: any[]) => { selectedRowKeys.value = keys }
}))

// ═══ 配置弹窗 ═══
const showColumnConfig = ref(false)
const showPageConfig = ref(false)

// ═══ 查询条件配置（PageConfigPanel 格式） ═══
const pageQueryFields = ref([
  { key: 'date', label: '日期', visible: true },
  { key: 'returnDocNo', label: '单据编号', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: false },
  { key: 'creatorName', label: '制单人', visible: false },
  { key: 'bookkeeperName', label: '记账人', visible: false },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'settleStatus', label: '结算状态', visible: true },
  { key: 'deliveryMethod', label: '配送方式', visible: false },
  { key: 'salesType', label: '销售类型', visible: false },
  { key: 'productLineAttr', label: '商品行属性', visible: false },
  { key: 'remark', label: '单据备注', visible: false },
  { key: 'summary', label: '摘要', visible: false },
  { key: 'extNum1', label: '自定义字段1(数字)', visible: false },
  { key: 'extNum2', label: '自定义字段2(数字)', visible: false },
  { key: 'extText1', label: '自定义字段3(文本)', visible: false },
  { key: 'extText2', label: '自定义字段4(文本)', visible: false },
  { key: 'extText3', label: '自定义字段5(文本)', visible: false },
  { key: 'receiverName', label: '收货人', visible: false },
  { key: 'receiverPhone', label: '联系电话', visible: false },
  { key: 'shippingAddress', label: '收货地址', visible: false },
  { key: 'logisticsCompany', label: '物流公司', visible: false },
  { key: 'waybillNo', label: '运单号', visible: false },
  { key: 'region', label: '区域', visible: false },
  { key: 'generateType', label: '产生方式', visible: false },
  { key: 'printCount', label: '打印次数', visible: false },
  { key: 'showRedFlush', label: '显示红冲', visible: false },
])

// ═══ 功能按钮配置（PageConfigPanel 格式） ═══
const pageFunctionButtons = ref([
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'productSummary', label: '商品汇总', enabled: true },
  { key: 'batchPrint', label: '批量打印', enabled: true },
  { key: 'print', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
])

// ═══ 状态映射 ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审核', color: 'processing' },
  2: { text: '已审核', color: 'blue' },
  3: { text: '已完成', color: 'success' },
  4: { text: '已取消', color: 'default' },
}

function getStatusText(status: number): string {
  return STATUS_MAP[status]?.text || '未知'
}

function getStatusColor(status: number): string {
  return STATUS_MAP[status]?.color || 'default'
}

// ═══ 44列配置 ═══
const docColumns = [
  { title: '操作', key: 'action', type: 'action', width: 140, fixed: 'right' as const, slotName: 'actionCell' },
  { title: '单据日期', field: 'orderDate', key: 'orderDate', width: 110, sortable: true },
  { title: '单据编号', field: 'returnDocNo', key: 'returnDocNo', width: 170, type: 'slot' as const, slotName: 'orderNoCell', sortable: true },
  { title: '源单', field: 'sourceOrder', key: 'sourceOrder', width: 150 },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center' as const, type: 'slot' as const, slotName: 'statusCell', sortable: true },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 120, sortable: true },
  { title: '客户', field: 'customerName', key: 'customerName', width: 160, sortable: true },
  { title: '客户编号', field: 'customerCode', key: 'customerCode', width: 120, sortable: true },
  { title: '客户级别', field: 'customerLevel', key: 'customerLevel', width: 100, sortable: true },
  { title: '收货人', field: 'receiverName', key: 'receiverName', width: 100, sortable: true },
  { title: '联系电话', field: 'receiverPhone', key: 'receiverPhone', width: 120, sortable: true },
  { title: '收货地址', field: 'shippingAddress', key: 'shippingAddress', width: 150, sortable: true },
  { title: '客户一票通', field: 'customerTicket', key: 'customerTicket', width: 100 },
  { title: '客户备注', field: 'customerRemark', key: 'customerRemark', width: 120 },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 80, sortable: true },
  { title: '部门', field: 'deptName', key: 'deptName', width: 100, sortable: true },
  { title: '数量', field: 'totalQuantity', key: 'totalQuantity', width: 100, align: 'right' as const, sortable: true },
  { title: '金额', field: 'totalAmount', key: 'totalAmount', width: 100, align: 'right' as const, sortable: true },
  { title: '折后金额', field: 'discountBillAmount', key: 'discountBillAmount', width: 100, align: 'right' as const, sortable: true },
  { title: '本单金额', field: 'billAmount', key: 'billAmount', width: 100, align: 'right' as const, sortable: true },
  { title: '已结金额', field: 'settledAmount', key: 'settledAmount', width: 100, align: 'right' as const, sortable: true },
  { title: '结算状态', field: 'settleStatus', key: 'settleStatus', width: 100, align: 'center' as const, type: 'slot' as const, slotName: 'settleStatusCell' },
  { title: '重量(kg)', field: 'totalWeight', key: 'totalWeight', width: 80, align: 'right' as const },
  { title: '体积(m³)', field: 'totalVolume', key: 'totalVolume', width: 80, align: 'right' as const },
  { title: '单据备注', field: 'remark', key: 'remark', width: 120 },
  { title: '表头自定义1(数字)', field: 'extNum1', key: 'extNum1', width: 80, align: 'right' as const },
  { title: '表头自定义2(数字)', field: 'extNum2', key: 'extNum2', width: 80, align: 'right' as const },
  { title: '表头自定义3(文本)', field: 'extText1', key: 'extText1', width: 100 },
  { title: '表头自定义4(文本)', field: 'extText2', key: 'extText2', width: 100 },
  { title: '表头自定义5(文本)', field: 'extText3', key: 'extText3', width: 100 },
  { title: '物流公司', field: 'logisticsCompany', key: 'logisticsCompany', width: 120 },
  { title: '运单号', field: 'waybillNo', key: 'waybillNo', width: 120 },
  { title: '配送方式', field: 'deliveryMethod', key: 'deliveryMethod', width: 100 },
  { title: '区域', field: 'region', key: 'region', width: 100 },
  { title: '产生方式', field: 'generateType', key: 'generateType', width: 100 },
  { title: '销售类型', field: 'salesType', key: 'salesType', width: 100 },
  { title: '附件', field: 'attachment', key: 'attachment', width: 60, align: 'center' as const, type: 'slot' as const, slotName: 'attachmentCell' },
  { title: '摘要', field: 'summary', key: 'summary', width: 120 },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 80 },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 80 },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 140 },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 140 },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 60, align: 'center' as const },
  { title: '商品行数', field: 'productLineCount', key: 'productLineCount', width: 60, align: 'center' as const },
]

// ═══ 列配置（使用 useColumnConfig composable） ═══
const {
  visibleColumns,
  onSettingChange,
  resetSettings,
  settingsColumns,
} = useColumnConfig(docColumns, 'sale-return-doc-list-columns')

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  hasError.value = false
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchParams.returnDocNo) params.keyword = searchParams.returnDocNo
    if (searchParams.customerName) params.customerName = searchParams.customerName
    if (searchParams.handlerName) params.handlerName = searchParams.handlerName
    if (searchParams.deptName) params.deptName = searchParams.deptName
    if (searchParams.creatorName) params.creatorName = searchParams.creatorName
    if (searchParams.bookkeeperName) params.bookkeeperName = searchParams.bookkeeperName
    if (searchParams.warehouseName) params.warehouseName = searchParams.warehouseName
    if (searchParams.status !== undefined) params.status = searchParams.status
    if (searchParams.settleStatus) params.settleStatus = searchParams.settleStatus
    if (searchParams.deliveryMethod) params.deliveryMethod = searchParams.deliveryMethod
    if (searchParams.salesType) params.salesType = searchParams.salesType
    if (searchParams.productLineAttr) params.productLineAttr = searchParams.productLineAttr
    if (searchParams.remark) params.remark = searchParams.remark
    if (searchParams.summary) params.summary = searchParams.summary
    if (searchParams.extNum1 !== undefined) params.extNum1 = searchParams.extNum1
    if (searchParams.extNum2 !== undefined) params.extNum2 = searchParams.extNum2
    if (searchParams.extText1) params.extText1 = searchParams.extText1
    if (searchParams.extText2) params.extText2 = searchParams.extText2
    if (searchParams.extText3) params.extText3 = searchParams.extText3
    if (searchParams.receiverName) params.receiverName = searchParams.receiverName
    if (searchParams.receiverPhone) params.receiverPhone = searchParams.receiverPhone
    if (searchParams.shippingAddress) params.shippingAddress = searchParams.shippingAddress
    if (searchParams.logisticsCompany) params.logisticsCompany = searchParams.logisticsCompany
    if (searchParams.waybillNo) params.waybillNo = searchParams.waybillNo
    if (searchParams.region) params.region = searchParams.region
    if (searchParams.generateType) params.generateType = searchParams.generateType
    if (searchParams.printCount !== undefined) params.printCount = searchParams.printCount
    if (searchParams.showRedFlush) params.showRedFlush = true
    if (searchParams.startDate) params.startDate = searchParams.startDate
    if (searchParams.endDate) params.endDate = searchParams.endDate

    const res = await saleReturnDocApi.page(params)
    if (res) {
      tableData.value = res?.records || []
      pagination.total = res?.total || 0
    }
  } catch (error: any) {
    hasError.value = true
    console.warn('[销售退货单] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

// ═══ 快捷日期 ═══
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

// ═══ 操作 ═══
function handleAdd() {
  router.push('/sales/return-doc/create')
}

function handleView(record: any) {
  router.push(`/sales/return-doc/form/${record.id}`)
}

function handleEdit(record: any) {
  router.push(`/sales/return-doc/form/${record.id}`)
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除退货单 ${record.returnDocNo} 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await saleReturnDocApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch (error: any) {
        console.warn('[销售退货单] 删除失败', error)
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '确认审核',
    content: `确定要审核通过退货单 ${record.returnDocNo} 吗？`,
    onOk: async () => {
      try {
        await saleReturnDocApi.approve(record.id)
        message.success('审核成功')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '审核失败')
      }
    },
  })
}

function handleReject(record: any) {
  Modal.confirm({
    title: '确认拒绝',
    content: `确定要拒绝退货单 ${record.returnDocNo} 吗？`,
    onOk: async () => {
      try {
        await saleReturnDocApi.reject(record.id, '审核拒绝')
        message.success('已拒绝')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '操作失败')
      }
    },
  })
}

function handleComplete(record: any) {
  Modal.confirm({
    title: '确认完成',
    content: `确定要完成退货单 ${record.returnDocNo} 吗？`,
    onOk: async () => {
      try {
        await saleReturnDocApi.complete(record.id)
        message.success('已完成')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '操作失败')
      }
    },
  })
}

function handleCancel(record: any) {
  Modal.confirm({
    title: '确认取消',
    content: `确定要取消退货单 ${record.returnDocNo} 吗？`,
    onOk: async () => {
      try {
        await saleReturnDocApi.cancel(record.id, '手动取消')
        message.success('已取消')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '操作失败')
      }
    },
  })
}

async function handleProductSummary() {
  try {
    const res = await saleReturnDocApi.pageDetail({
      pageNum: 1,
      pageSize: 999,
      startDate: searchParams.startDate,
      endDate: searchParams.endDate,
    })
    const records = res?.records || []
    if (records.length === 0) {
      message.info('当前查询条件下无数据')
      return
    }
    const map = new Map<string, any>()
    records.forEach((r: any) => {
      const key = r.productName || r.product_code || '未知商品'
      const existing = map.get(key)
      if (existing) {
        existing.quantity = (existing.quantity || 0) + (parseFloat(r.return_quantity) || 0)
        existing.amount = (existing.amount || 0) + (parseFloat(r.line_amount) || 0)
      } else {
        map.set(key, {
          productName: key,
          productCode: r.product_code || '',
          quantity: parseFloat(r.return_quantity) || 0,
          amount: parseFloat(r.line_amount) || 0,
        })
      }
    })
    const summaryLines = Array.from(map.values())
      .sort((a: any, b: any) => b.amount - a.amount)
      .map((p: any) => `${p.productName}(${p.productCode || '-'}): ${p.quantity}件 / ¥${p.amount.toFixed(2)}`)
      .join('\n')
    Modal.info({
      title: '商品汇总',
      content: h('pre', { style: 'max-height:400px;overflow:auto;font-size:13px;line-height:1.8' }, summaryLines || '无汇总数据'),
      width: 500,
    })
  } catch {
    message.error('获取商品汇总失败')
  }
}

const handleError = (error: Error) => {
  hasError.value = true
  console.error('[销售退货单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  setQuickDate('week')
  fetchData()
})
</script>

<style scoped>
.toolbar-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.query-scheme-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
}

.quick-dates :deep(.ant-btn) {
  font-size: 13px;
  padding: 2px 8px;
}
.quick-dates :deep(.ant-btn-primary) {
  color: #fff;
  background: #ff7a45;
  border-color: #ff7a45;
}

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

.table-area {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
</style>
