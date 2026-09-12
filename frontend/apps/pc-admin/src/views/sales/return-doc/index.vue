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
              <a-select-option value="">
                --查询方案--
              </a-select-option>
            </a-select>
            <a-tooltip title="另存为查询方案" placement="bottom">
              <a-button type="link" size="small" style="padding: 0 4px" @click="handleSaveQueryScheme">
                <PlusOutlined />
              </a-button>
            </a-tooltip>
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
        <!-- 工具栏右侧：操作按钮（由页面配置「功能按钮」控制启停） -->
        <div class="toolbar-right">
          <a-space :size="8">
            <a-tooltip v-if="isButtonEnabled('config')" title="配置" placement="bottom">
              <a-button size="small" @click="showPageConfig = true">
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button v-if="isButtonEnabled('add')" type="primary" size="small" @click="handleAdd">
              <PlusOutlined /> 新增
            </a-button>
            <a-button v-if="isButtonEnabled('refresh')" size="small" @click="fetchData">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button v-if="isButtonEnabled('productSummary')" size="small" @click="handleProductSummary">
              <AppstoreOutlined /> 商品汇总
            </a-button>
            <a-tooltip v-if="isButtonEnabled('batchPrint')" title="批量打印选中单据" placement="bottom">
              <a-button size="small" @click="handleBatchPrint">
                <PrinterOutlined /> 批量打印
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="isButtonEnabled('print')" title="打印(F8)" placement="bottom">
              <a-button size="small" @click="handlePrintF8">
                <PrinterOutlined /> 打印(F8)
              </a-button>
            </a-tooltip>
            <a-button v-if="isButtonEnabled('export')" size="small" @click="handleExport">
              <ExportOutlined /> 导出
            </a-button>
            <!-- 批量操作（仅在有选中行时显示） -->
            <a-button v-if="selectedRowKeys.length > 0" size="small" @click="handleBatchApprove">
              <CheckOutlined /> 批量审核
            </a-button>
            <a-button v-if="selectedRowKeys.length > 0" size="small" danger @click="handleBatchDelete">
              <DeleteOutlined /> 批量删除
            </a-button>
          </a-space>
        </div>
      </div>

      <!-- ═══ 搜索区域（默认 2 排，超出折叠；字段显隐由页面配置「查询条件」控制） ═══ -->
      <div class="search-area">
        <div class="search-container" :data-expanded="searchExpanded || null">
          <div ref="gridRef" class="search-grid">
            <!-- 主查询字段（前 8 个默认排前，查询/刷新/显示红冲紧随其后） -->
            <div v-for="f in primaryQueryFields" :key="f.key" class="search-field-item">
              <a-range-picker
                v-if="f.key === 'date'"
                v-model:value="dateRange"
                size="small"
                style="width: 100%"
                @change="handleDateChange"
              />
              <div v-else-if="fieldMeta[f.key]?.type === 'select'" class="search-select-wrap">
                <span class="search-select-label">{{ f.label }}</span>
                <a-select
                  v-model:value="(searchParams as any)[f.key]"
                  size="small"
                  allow-clear
                  placeholder="全部"
                >
                  <a-select-option
                    v-for="opt in fieldMeta[f.key].options"
                    :key="String(opt.value)"
                    :value="opt.value"
                  >
                    {{ opt.label }}
                  </a-select-option>
                </a-select>
              </div>
              <a-input-number
                v-else-if="fieldMeta[f.key]?.type === 'number'"
                v-model:value="(searchParams as any)[f.key]"
                size="small"
                style="width: 100%"
                :placeholder="f.label"
              />
              <a-input
                v-else
                v-model:value="(searchParams as any)[f.key]"
                :placeholder="f.label"
                allow-clear
                size="small"
                :suffix="fieldMeta[f.key]?.search ? h(SearchOutlined, { style: 'color:#bbb' }) : undefined"
              />
            </div>

            <!-- 查询 / 重置 / 显示红冲（对标：2 排内必含查询及后续勾选项） -->
            <div ref="actionRef" class="search-field-item search-action-group" :style="{ gridColumn: 'span ' + actionSpan }">
              <a-space :size="4">
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="resetSearch">重置</a-button>
              </a-space>
              <label class="search-red-flush">
                <a-checkbox v-model:checked="searchParams.showRedFlush" @change="handleSearch" /> 显示红冲
              </label>
            </div>

            <!-- 折叠区：其余勾选可见的查询条件 -->
            <template v-if="searchExpanded">
              <div v-for="f in extraQueryFields" :key="f.key" class="search-field-item">
                <div v-if="fieldMeta[f.key]?.type === 'select'" class="search-select-wrap">
                  <span class="search-select-label">{{ f.label }}</span>
                  <a-select
                    v-model:value="(searchParams as any)[f.key]"
                    size="small"
                    allow-clear
                    placeholder="全部"
                  >
                    <a-select-option
                      v-for="opt in fieldMeta[f.key].options"
                      :key="String(opt.value)"
                      :value="opt.value"
                    >
                      {{ opt.label }}
                    </a-select-option>
                  </a-select>
                </div>
                <a-input-number
                  v-else-if="fieldMeta[f.key]?.type === 'number'"
                  v-model:value="(searchParams as any)[f.key]"
                  size="small"
                  style="width: 100%"
                  :placeholder="f.label"
                />
                <a-input
                  v-else
                  v-model:value="(searchParams as any)[f.key]"
                  :placeholder="f.label"
                  allow-clear
                  size="small"
                  :suffix="fieldMeta[f.key]?.search ? h(SearchOutlined, { style: 'color:#bbb' }) : undefined"
                />
              </div>
            </template>
          </div>
          <div v-if="hasMoreConditions" class="search-more-toggle">
            <a-button type="link" size="small" @click="searchExpanded = !searchExpanded">
              {{ searchExpanded ? '收起' : '更多条件' }}
            </a-button>
          </div>
        </div>
      </div>

      <!-- ══ 表格区域 ═══ -->
      <div class="table-area">
        <BillTableList
          :columns="docColumns"
          :storage-key="'sale-return-doc-table-columns'"
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
            <a-tag v-if="record.settleStatus" :color="record.settleStatus === '已结算' ? 'success' : 'processing'">
              {{ record.settleStatus }}
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
              <a-button v-if="record.status === 0" type="link" size="small" @click="handleCopy(record)">复制</a-button>
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

    <!-- ═══ 页面配置弹窗（3 Tab：查询条件/功能按钮/打印配置） ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="pageQueryFields"
      :function-buttons-config="pageFunctionButtons"
      storage-key="sale-return-doc-page-config"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
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
  SettingOutlined, SearchOutlined, CheckOutlined, DeleteOutlined,
  AppstoreOutlined, PaperClipOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
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

// ══ 搜索展开 ══
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
const showPageConfig = ref(false)

// ═══ 查询条件配置（PageConfigPanel 格式，29 项，与开发文档一致） ═══
const DEFAULT_QUERY_FIELDS = [
  { key: 'date', label: '日期', visible: true },
  { key: 'returnDocNo', label: '单据编号', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: true },
  { key: 'creatorName', label: '制单人', visible: true },
  { key: 'bookkeeperName', label: '记账人', visible: true },
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
  { key: 'showRedFlush', label: '显示红冲', visible: true },
]
const pageQueryFields = ref(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))

/** 查询字段渲染元信息（类型 / 下拉选项 / 是否带搜索图标） */
const fieldMeta: Record<string, { type: 'input' | 'select' | 'number'; search?: boolean; options?: { label: string; value: any }[] }> = {
  date: { type: 'input' },
  returnDocNo: { type: 'input', search: true },
  customerName: { type: 'input', search: true },
  handlerName: { type: 'input', search: true },
  deptName: { type: 'input' },
  creatorName: { type: 'input' },
  bookkeeperName: { type: 'input' },
  warehouseName: { type: 'input', search: true },
  status: { type: 'select', options: [
    { label: '草稿', value: 0 },
    { label: '待审核', value: 1 },
    { label: '已审核', value: 2 },
    { label: '已完成', value: 3 },
    { label: '已取消', value: 4 },
  ] },
  settleStatus: { type: 'select', options: [
    { label: '未结算', value: '未结算' },
    { label: '部分结算', value: '部分结算' },
    { label: '已结算', value: '已结算' },
  ] },
  deliveryMethod: { type: 'select', options: [
    { label: '自提', value: '自提' },
    { label: '配送', value: '配送' },
    { label: '快递', value: '快递' },
  ] },
  salesType: { type: 'select', options: [
    { label: '正常销售', value: 'normal' },
    { label: '退货', value: 'return' },
  ] },
  productLineAttr: { type: 'select', options: [
    { label: '正常', value: '正常' },
    { label: '赠品', value: '赠品' },
  ] },
  generateType: { type: 'select', options: [
    { label: '手动创建', value: '手动创建' },
    { label: '销售退货申请', value: '销售退货申请' },
  ] },
  remark: { type: 'input' },
  summary: { type: 'input' },
  extNum1: { type: 'number' },
  extNum2: { type: 'number' },
  extText1: { type: 'input' },
  extText2: { type: 'input' },
  extText3: { type: 'input' },
  receiverName: { type: 'input' },
  receiverPhone: { type: 'input' },
  shippingAddress: { type: 'input' },
  logisticsCompany: { type: 'input' },
  waybillNo: { type: 'input' },
  region: { type: 'input' },
  printCount: { type: 'number' },
}

// ═══ 功能按钮配置（PageConfigPanel 格式，7 项，与开发文档一致） ═══
const DEFAULT_FUNCTION_BUTTONS = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'productSummary', label: '商品汇总', enabled: true },
  { key: 'batchPrint', label: '批量打印', enabled: true },
  { key: 'print', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]
const pageFunctionButtons = ref(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))

// ═══ 查询条件显隐联动 ═══
const DEFAULT_PRIMARY_COUNT = 8
const visibleQueryFields = computed(() => pageQueryFields.value.filter(f => f.visible !== false))
const primaryQueryFields = computed(() => visibleQueryFields.value.slice(0, DEFAULT_PRIMARY_COUNT))
const extraQueryFields = computed(() => visibleQueryFields.value.slice(DEFAULT_PRIMARY_COUNT))
const hasMoreConditions = computed(() => extraQueryFields.value.length > 0)

function isButtonEnabled(key: string): boolean {
  const btn = pageFunctionButtons.value.find(b => b.key === key)
  return btn ? btn.enabled !== false : true
}

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

// ═══ 列配置走数据表表头齿轮（storage-key=sale-return-doc-table-columns，含个人/全局双配置） ═══

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
    // 已取消(4) 即本单据口径下的「红冲」：筛已取消时必须放开红冲过滤，否则条件互斥查不到
    if (searchParams.showRedFlush || searchParams.status === 4) params.showRedFlush = true
    if (searchParams.startDate) params.startDate = searchParams.startDate
    if (searchParams.endDate) params.endDate = searchParams.endDate

    const res = await saleReturnDocApi.page(params)
    if (res) {
      tableData.value = (res as any)?.records || []
      pagination.total = (res as any)?.total || 0
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

/** 重置查询条件（保留当前快捷日期口径） */
function resetSearch() {
  Object.assign(searchParams, {
    returnDocNo: '', customerName: '', handlerName: '', deptName: '', creatorName: '',
    bookkeeperName: '', warehouseName: '', status: undefined, settleStatus: undefined,
    deliveryMethod: undefined, salesType: undefined, productLineAttr: undefined,
    remark: '', summary: '', extNum1: undefined, extNum2: undefined,
    extText1: '', extText2: '', extText3: '', receiverName: '', receiverPhone: '',
    shippingAddress: '', logisticsCompany: '', waybillNo: '', region: '',
    generateType: undefined, printCount: undefined, showRedFlush: false,
  })
  setQuickDate(quickDate.value)
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

/** 另存为查询方案：把当前查询条件存入 localStorage 供下次复用 */
function handleSaveQueryScheme() {
  Modal.confirm({
    title: '另存为查询方案',
    content: h('div', [
      h('div', { style: 'margin-bottom:8px;font-size:12px;color:#8c8c8c' }, '保存当前搜索区全部查询条件，便于下次一键调用'),
      h('input', {
        id: 'sale-return-doc-scheme-name',
        class: 'ant-input',
        placeholder: '请输入方案名称',
        style: 'width:100%',
      }),
    ]),
    okText: '保存',
    cancelText: '取消',
    onOk: () => {
      const el = document.getElementById('sale-return-doc-scheme-name') as HTMLInputElement | null
      const name = (el?.value || '').trim()
      if (!name) {
        message.warning('请输入方案名称')
        return Promise.reject()
      }
      const schemes = JSON.parse(localStorage.getItem('sale-return-doc-query-schemes') || '{}')
      schemes[name] = { ...searchParams }
      localStorage.setItem('sale-return-doc-query-schemes', JSON.stringify(schemes))
      message.success(`查询方案「${name}」已保存`)
    },
  })
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

/** 复制：以原单为模板新建草稿（新号段单号 + 相同明细），对标金蝶/管家婆「复制单据」 */
async function handleCopy(record: any) {
  try {
    const detail = await saleReturnDocApi.getById(record.id)
    if (!detail) {
      message.error('源单不存在')
      return
    }
    const noRes: any = await saleReturnDocApi.nextNo()
    const newNo = typeof noRes === 'string' ? noRes : noRes?.data
    const payload: any = {
      ...detail,
      id: undefined,
      returnDocNo: newNo,
      status: 0,
      createTime: undefined,
      updateTime: undefined,
      bookkeepingTime: undefined,
      bookkeeperName: undefined,
      approvedBy: undefined,
      approvedTime: undefined,
      approvedNote: undefined,
      auditor: undefined,
      auditorId: undefined,
      auditorName: undefined,
      auditTime: undefined,
      submitBy: undefined,
      submitTime: undefined,
      printCount: 0,
      items: ((detail as any).items || []).map((it: any) => ({ ...it, id: undefined, returnDocId: undefined })),
    }
    await saleReturnDocApi.create(payload)
    message.success('复制成功，已生成新的退货单草稿')
    fetchData()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '复制失败')
  }
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

async function handleBatchDelete() {
  const ids = selectedRowKeys.value
  if (!ids.length) {
    message.warning('请先选择单据')
    return
  }
  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${ids.length} 条单据吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await Promise.all(ids.map((id: any) => saleReturnDocApi.delete(id)))
        message.success(`已删除 ${ids.length} 条单据`)
        selectedRowKeys.value = []
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '批量删除失败')
      }
    },
  })
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '确认审核',
    content: `确定要审核通过退货单 ${record.returnDocNo} 吗？审核后将回写库存并生成冲销凭证。`,
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
    content: `确定要取消退货单 ${record.returnDocNo} 吗？已记账单据将冲回库存并生成反向凭证。`,
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

async function handleBatchApprove() {
  const ids = selectedRowKeys.value
  if (!ids.length) {
    message.warning('请先选择单据')
    return
  }
  Modal.confirm({
    title: '批量审核',
    content: `确定要审核选中的 ${ids.length} 条单据吗？审核后将逐单回写库存并生成凭证。`,
    onOk: async () => {
      try {
        const res: any = await saleReturnDocApi.batchApprove(ids)
        const approved = res?.approved ?? ids.length
        message.success(`已审核 ${approved} 条单据${approved < ids.length ? `，${ids.length - approved} 条跳过(非待审核状态)` : ''}`)
        selectedRowKeys.value = []
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '批量审核失败')
      }
    },
  })
}

/** 商品汇总：按当前查询条件聚合明细数量/金额，数据来源 /page-detail（真实聚合） */
async function handleProductSummary() {
  try {
    const res: any = await saleReturnDocApi.pageDetail({
      pageNum: 1,
      pageSize: 999,
      startDate: searchParams.startDate,
      endDate: searchParams.endDate,
      customerName: searchParams.customerName || undefined,
      warehouseName: searchParams.warehouseName || undefined,
      status: searchParams.status,
    } as any)
    const records: any[] = res?.records || []
    if (records.length === 0) {
      message.info('当前查询条件下无数据')
      return
    }
    const pick = (r: any, camel: string, snake: string) => r[camel] ?? r[snake]
    const map = new Map<string, { productName: string; productCode: string; quantity: number; amount: number }>()
    records.forEach((r: any) => {
      const name = pick(r, 'productName', 'product_name') || '未知商品'
      const code = pick(r, 'productCode', 'product_code') || ''
      const key = `${name}__${code}`
      const qty = parseFloat(pick(r, 'returnQuantity', 'return_quantity')) || 0
      const amt = parseFloat(pick(r, 'lineAmount', 'line_amount')) || 0
      const existing = map.get(key)
      if (existing) {
        existing.quantity += qty
        existing.amount += amt
      } else {
        map.set(key, { productName: name, productCode: code, quantity: qty, amount: amt })
      }
    })
    const summaryLines = Array.from(map.values())
      .sort((a, b) => b.amount - a.amount)
      .map(p => `${p.productName}(${p.productCode || '-'}): ${p.quantity} 件 / ¥${p.amount.toFixed(2)}`)
      .join('\n')
    Modal.info({
      title: `商品汇总（${map.size} 个商品）`,
      content: h('pre', { style: 'max-height:400px;overflow:auto;font-size:13px;line-height:1.8' }, summaryLines || '无汇总数据'),
      width: 560,
    })
  } catch {
    message.error('获取商品汇总失败')
  }
}

/** 导出：按当前可见列导出 Excel 可直接打开的 CSV */
function handleExport() {
  const rows = tableData.value
  if (!rows.length) {
    message.warning('暂无数据可导出')
    return
  }
  const cols = docColumns.filter((c: any) => c.field && c.type !== 'action')
  const headers = cols.map((c: any) => c.title)
  const csvRows = rows.map(r =>
    cols.map((c: any) => {
      const v = (r as any)[c.field]
      if (v === null || v === undefined) return ''
      return String(v).replace(/[",\n\r]/g, ' ')
    })
  )
  const csv = [headers.join(','), ...csvRows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `销售退货单_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success(`已导出 ${rows.length} 行`)
}

/** 打印：跳转单据表单页并携带 print=1，由表单页唤起打印 */
function openPrintForm(id: any) {
  router.push(`/sales/return-doc/form/${id}?print=1`)
}

function handlePrintF8() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要打印的单据')
    return
  }
  openPrintForm(selectedRowKeys.value[0])
}

/** 批量打印：逐张打开选中单据打印（对标管家婆批量打印） */
function handleBatchPrint() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要批量打印的单据')
    return
  }
  Modal.confirm({
    title: '批量打印',
    content: `将依次打开选中的 ${selectedRowKeys.value.length} 张单据进行打印，确定继续吗？`,
    okText: '开始打印',
    cancelText: '取消',
    onOk: () => openPrintForm(selectedRowKeys.value[0]),
  })
}

// ═══ 页面配置变更与回读（勾选/排序真实生效） ═══
// 存档由 PageConfigPanel 统一写入 storage-key（sale-return-doc-page-config），本页只负责读取与应用
const PAGE_CONFIG_STORAGE_KEY = 'sale-return-doc-page-config'

function handlePageConfigChange(config: any) {
  if (config.queryFields) {
    config.queryFields.forEach((f: any) => {
      const target = pageQueryFields.value.find(d => d.key === f.key)
      if (target) target.visible = f.visible !== false
    })
    // 拖拽排序真实生效：按配置面板顺序重排查询字段
    const ordered = config.queryFields
      .map((f: any) => pageQueryFields.value.find(d => d.key === f.key))
      .filter((d: any) => !!d)
    if (ordered.length === pageQueryFields.value.length) {
      pageQueryFields.value.splice(0, pageQueryFields.value.length, ...ordered)
    }
  }
  if (config.functionButtons) {
    config.functionButtons.forEach((b: any) => {
      const target = pageFunctionButtons.value.find(d => d.key === b.key)
      if (target) target.enabled = b.enabled !== false
    })
  }
}

/** 合并存档配置：按存档顺序重排，新增字段沿用默认，已移除字段丢弃 */
function mergeQueryFields(
  defaults: Array<{ key: string; label: string; visible: boolean }>,
  saved: any[],
) {
  const merged = saved
    .map(s => {
      const def = defaults.find(d => d.key === s.key)
      return def ? { ...def, visible: s.visible !== false } : null
    })
    .filter((f): f is { key: string; label: string; visible: boolean } => !!f)
  defaults.forEach(def => {
    if (!merged.find(m => m.key === def.key)) merged.push({ ...def })
  })
  return merged
}

function loadPageConfigFromStorage() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed.queryFields)) {
      pageQueryFields.value = mergeQueryFields(DEFAULT_QUERY_FIELDS, parsed.queryFields)
    }
    if (Array.isArray(parsed.functionButtons)) {
      parsed.functionButtons.forEach((b: any) => {
        const target = pageFunctionButtons.value.find(d => d.key === b.key)
        if (target) target.enabled = b.enabled !== false
      })
    }
  } catch {
    // 存档损坏时沿用默认配置
  }
}

const handleError = (error: Error) => {
  hasError.value = true
  console.error('[销售退货单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadPageConfigFromStorage()
  setQuickDate('week')
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
/* 折叠态：只显示 2 排（28px 控件 + 12px 行距 ≈ 68px） */
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
.search-action-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}
.search-red-flush {
  font-size: 13px;
  color: rgba(0,0,0,0.65);
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 4px;
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
