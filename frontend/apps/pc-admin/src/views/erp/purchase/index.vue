<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <DocCenterLayout
        v-model:active-main-tab="mainTab"
        v-model:active-sub-tab="subTab"
        v-model:date-shortcut="dateShortcut"
        v-model:date-range="dateRange"
        v-model:search-values="searchForm"
        v-model:page-current="paginationConfig.current"
        v-model:page-size="paginationConfig.pageSize"
        :main-tabs="mainTabs"
        :sub-tabs="subTabs"
        :search-config="searchConfig"
        :search-checkbox-config="searchCheckboxConfig"
        :hidden-field-keys="hiddenSearchFieldKeys"
        :stat-card-config="statCardConfig"
        :toolbar-config="toolbarConfig"
        :function-buttons-config="functionButtonConfig"
        :show-pagination="true"
        :page-total="paginationConfig.total"
        :stats-data="stats"
        @search="handleSearch"
        @refresh="fetchData"
        @toolbar-action="handleToolbarAction"
        @page-change="handlePageChange"
      >
        <!-- 工具栏左侧：查询方案 + 快捷日期（DocCenterLayout 内置日期快捷） -->
        <template #toolbar-left>
          <div class="query-scheme-wrap">
            <a-select
              v-model:value="queryScheme"
              style="width: 140px"
              size="small"
              placeholder="--查询方案--"
              allow-clear
              @change="handleSchemeChange"
            >
              <a-select-option value="">--查询方案--</a-select-option>
              <a-select-option v-for="s in schemes" :key="s" :value="s">{{ s }}</a-select-option>
            </a-select>
            <a-button type="link" size="small" style="padding: 0 4px" @click="handleSaveScheme">
              <PlusOutlined />
            </a-button>
          </div>
        </template>

        <template #table>
          <BillDetailTable
            :columns="activeColumns"
            :data-source="activeTableData"
            :loading="loading"
            :view-mode="true"
            :storage-key="storageKey"
            style="height: 100%"
            @checkbox-change="handleRowCheck"
            @checkbox-all="handleRowCheckAll"
          >
            <template #actionCell="{ record }">
              <a-space :size="4">
                <a-button type="link" size="small" @click="goDetail(record)">查看</a-button>
                <a-button v-if="record.status === 0" type="link" size="small" @click="goEdit(record)">编辑</a-button>
                <a-button v-if="record.status === 0" type="link" size="small" @click="handleSubmit(record)">提交</a-button>
                <a-button v-if="record.status === 1" type="link" size="small" @click="handleApprove(record)">审批</a-button>
              </a-space>
            </template>
            <template #orderNoCell="{ record }">
              <a-button type="link" size="small" @click="goDetail(record)">{{ record.orderNo }}</a-button>
            </template>
            <template #statusCell="{ record }">
              <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
            </template>
          </BillDetailTable>
        </template>
      </DocCenterLayout>

      <!-- 页面配置弹窗 -->
      <PageConfigPanel
        :key="'purchase-page-config-' + subTab"
        :open="showPageConfig"
        :storage-key="'purchase-order-page-config'"
        :query-fields-config="subTab === 'byDoc' ? docQueryConfig : detailQueryConfig"
        :function-buttons-config="functionButtonConfig"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- 打印（模板渲染，走 erp-printing 系统级路径） -->
      <PrintDialog
        ref="printDialogRef"
        page-code="purchase"
        :document-id="printData.id"
        :print-data="printData"
        @print-success="handlePrintSuccess"
      />

      <!-- 批量导入隐藏文件输入 -->
      <input ref="fileInputRef" type="file" accept=".xlsx,.xls" style="display:none" @change="handleImportFileChange" />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import { PlusOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import DocCenterLayout from '@/components/DocCenterLayout/DocCenterLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { userPageConfigApi } from '@/api/erp'
import { PURCHASE_ORDER_STATUS } from '@/utils/statusConfig'
import request from '@/utils/request'
import { showImportResult } from '@/utils/importResult'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type {
  SearchConfigMap, SearchCheckboxConfigMap,
  StatCardConfigMap, ToolbarConfigMap,
} from '@/components/DocCenterLayout/types'

defineOptions({ name: 'PurchaseOrderList' })
const router = useRouter()

// ═══ Tab 状态 ═══
const mainTab = ref('all')
const subTab = ref('byDoc')
const dateShortcut = ref('month')
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().startOf('month'), dayjs()])

const mainTabs = [{ key: 'all', label: '全部' }]
const subTabs = [
  { key: 'byDoc', label: '按单据' },
  { key: 'byDetail', label: '按明细' },
]

// ═══ 搜索 ═══
const searchForm = reactive<Record<string, any>>({})

// 按单据Tab搜索字段（16个，日期由 DocCenterLayout 固定显示在第一格）
const byDocSearchFields: SearchConfigMap = {
  'all.byDoc': [
    { key: 'orderNo', label: '单据编号', type: 'input' },
    { key: 'supplierName', label: '供应商', type: 'input' },
    { key: 'purchaserName', label: '经手人', type: 'input' },
    { key: 'deptName', label: '部门', type: 'input' },
    { key: 'createByName', label: '制单人', type: 'input' },
    { key: 'warehouseName', label: '仓库', type: 'input' },
    { key: 'status', label: '单据状态', type: 'select', options: [
      { label: '草稿', value: 0 }, { label: '待审批', value: 1 },
      { label: '已审批', value: 2 }, { label: '已下达', value: 3 },
      { label: '已取消', value: 4 }, { label: '已完成', value: 6 },
    ]},
    { key: 'remark', label: '单据备注', type: 'input' },
    { key: 'submitterName', label: '提交人', type: 'input' },
    { key: 'auditorName', label: '审核人', type: 'input' },
    { key: 'extNum1Start', label: '自定义字段1(数字)', type: 'input' },
    { key: 'extNum2Start', label: '自定义字段2(数字)', type: 'input' },
    { key: 'extText1', label: '自定义字段3(文本)', type: 'input' },
    { key: 'extText2', label: '自定义字段4(文本)', type: 'input' },
    { key: 'extText3', label: '自定义字段5(文本)', type: 'input' },
    { key: 'printCountStart', label: '打印次数', type: 'input' },
  ],
}

// 按明细Tab搜索字段（13个，日期由 DocCenterLayout 固定显示在第一格）
const byDetailSearchFields: SearchConfigMap = {
  'all.byDetail': [
    { key: 'orderNo', label: '单据编号', type: 'input' },
    { key: 'productName', label: '商品', type: 'input' },
    { key: 'supplierName', label: '供应商', type: 'input' },
    { key: 'purchaserName', label: '经手人', type: 'input' },
    { key: 'deptName', label: '部门', type: 'input' },
    { key: 'createByName', label: '制单人', type: 'input' },
    { key: 'auditorName', label: '审核人', type: 'input' },
    { key: 'warehouseName', label: '仓库', type: 'input' },
    { key: 'remark', label: '单据备注', type: 'input' },
    { key: 'itemRemark', label: '明细备注', type: 'input' },
    { key: 'isGift', label: '是否赠品', type: 'select', options: [
      { label: '是', value: 1 }, { label: '否', value: 0 },
    ]},
    { key: 'status', label: '单据状态', type: 'select', options: [
      { label: '草稿', value: 0 }, { label: '待审批', value: 1 },
      { label: '已审批', value: 2 }, { label: '已下达', value: 3 },
      { label: '已取消', value: 4 }, { label: '已完成', value: 6 },
    ]},
  ],
}

const searchConfig: SearchConfigMap = {
  'all.byDoc': byDocSearchFields['all.byDoc'],
  'all.byDetail': byDetailSearchFields['all.byDetail'],
}

// 搜索复选框配置
const searchCheckboxConfig: SearchCheckboxConfigMap = {
  'all.byDoc': [
    { key: 'hideCancelled', label: '不显示已取消的单据' },
    { key: 'showSelected', label: '仅显示已选中' },
  ],
  'all.byDetail': [
    { key: 'hideCancelled', label: '不显示已取消的单据' },
    { key: 'showSelected', label: '仅显示已选中' },
  ],
}

// 搜索字段隐藏配置（由 PageConfigPanel 控制，从 localStorage 加载）
const hiddenSearchFieldKeys = ref<string[]>([])

// ═══ 统计卡片 ═══
const stats = ref<Record<string, number>>({
  draft: 0, pending: 0, approved: 0, completed: 0, totalAmount: 0,
})

const statCardConfig: StatCardConfigMap = {
  'all.byDoc': [
    { label: '草稿', valueKey: 'draft', color: '#d9d9d9' },
    { label: '待审批', valueKey: 'pending', color: '#faad14' },
    { label: '已审批', valueKey: 'approved', color: '#1890ff' },
    { label: '已完成', valueKey: 'completed', color: '#52c41a' },
    { label: '订单金额', valueKey: 'totalAmount', color: '#722ed1' },
  ],
  'all.byDetail': [
    { label: '草稿', valueKey: 'draft', color: '#d9d9d9' },
    { label: '待审批', valueKey: 'pending', color: '#faad14' },
    { label: '已审批', valueKey: 'approved', color: '#1890ff' },
    { label: '已完成', valueKey: 'completed', color: '#52c41a' },
    { label: '订单金额', valueKey: 'totalAmount', color: '#722ed1' },
  ],
}

// ═══ 工具栏 ═══
const toolbarConfig: ToolbarConfigMap = {
  'all.byDoc': [
    { key: 'add', label: '新增', type: 'primary' },
    { key: 'refresh', label: '刷新' },
    { key: 'batchImport', label: '批量导入' },
    { key: 'batchPrint', label: '批量打印' },
    { key: 'printF8', label: '打印(F8)' },
    { key: 'export', label: '导出' },
    { key: 'pageConfig', label: '配置', icon: 'SettingsOutlined', visibleFor: () => true },
  ],
  'all.byDetail': [
    { key: 'add', label: '新增', type: 'primary' },
    { key: 'refresh', label: '刷新' },
    { key: 'printF8', label: '打印(F8)' },
    { key: 'export', label: '导出' },
    { key: 'pageConfig', label: '配置', icon: 'SettingsOutlined', visibleFor: () => true },
  ],
}

// ═══ 按单据 Tab 表格列（41 列，含序号/齿轮与选择框） ═══
const byDocColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'rowCheck', title: '', type: 'checkbox', width: 40 },
  { key: 'orderDate', title: '单据日期', width: 110 },
  { key: 'orderNo', title: '单据编号', width: 150, slotName: 'orderNoCell' },
  { key: 'sourceBillNo', title: '源单', width: 130 },
  { key: 'status', title: '单据状态', width: 90, slotName: 'statusCell' },
  { key: 'warehouseName', title: '仓库', width: 120 },
  { key: 'supplierName', title: '供应商名称', width: 180 },
  { key: 'supplierCode', title: '供应商编号', width: 110 },
  { key: 'contactName', title: '联系人', width: 90 },
  { key: 'contactPhone', title: '联系电话', width: 120 },
  { key: 'contactAddress', title: '联系地址', width: 180 },
  { key: 'supplierRemark', title: '供应商备注', width: 150 },
  { key: 'purchaserName', title: '经手人', width: 90 },
  { key: 'deptName', title: '部门', width: 100 },
  { key: 'productAmount', title: '商品金额', width: 110, align: 'right' },
  { key: 'discountAmount', title: '直接优惠', width: 100, align: 'right' },
  { key: 'otherExpense', title: '其他费用', width: 100, align: 'right' },
  { key: 'billAmount', title: '本单金额', width: 110, align: 'right' },
  { key: 'settledAmount', title: '已结金额', width: 110, align: 'right' },
  { key: 'expectedReceiveTime', title: '预计收货时间', width: 130 },
  { key: 'totalQuantity', title: '订货数量', width: 100, align: 'right' },
  { key: 'receivedQuantity', title: '已收数量', width: 100, align: 'right' },
  { key: 'unreceiveQuantity', title: '未收数量', width: 100, align: 'right' },
  { key: 'returnQuantity', title: '退货数量', width: 100, align: 'right' },
  { key: 'returnAmount', title: '退货金额', width: 100, align: 'right' },
  { key: 'weight', title: '重量(kg)', width: 90, align: 'right' },
  { key: 'volume', title: '体积(m³)', width: 90, align: 'right' },
  { key: 'remark', title: '单据备注', width: 150 },
  { key: 'summary', title: '摘要', width: 130 },
  { key: 'attachment', title: '附件', width: 70 },
  { key: 'extNum1', title: '自定义字段1(数字)', width: 120 },
  { key: 'extNum2', title: '自定义字段2(数字)', width: 120 },
  { key: 'extText1', title: '自定义字段3(文本)', width: 120 },
  { key: 'extText2', title: '自定义字段4(文本)', width: 120 },
  { key: 'extText3', title: '自定义字段5(文本)', width: 120 },
  { key: 'submitTime', title: '提交时间', width: 130 },
  { key: 'createByName', title: '制单人', width: 90 },
  { key: 'submitterName', title: '提交人', width: 90 },
  { key: 'auditorName', title: '审核人', width: 90 },
  { key: 'printCount', title: '打印次数', width: 90 },
]

// ═══ 按明细 Tab 表格列（61 列，含序号/齿轮与选择框） ═══
const byDetailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'rowCheck', title: '', type: 'checkbox', width: 40 },
  { key: 'orderDate', title: '单据日期', width: 110 },
  { key: 'orderNo', title: '单据编号', width: 150, slotName: 'orderNoCell' },
  { key: 'status', title: '单据状态', width: 90, slotName: 'statusCell' },
  { key: 'warehouseName', title: '仓库', width: 120 },
  { key: 'supplierName', title: '供应商名称', width: 180 },
  { key: 'supplierCode', title: '供应商编号', width: 110 },
  { key: 'contactName', title: '联系人', width: 90 },
  { key: 'contactPhone', title: '联系电话', width: 120 },
  { key: 'contactAddress', title: '联系地址', width: 180 },
  { key: 'supplierRemark', title: '供应商备注', width: 150 },
  { key: 'purchaserName', title: '经手人', width: 90 },
  { key: 'deptName', title: '部门', width: 100 },
  { key: 'productName', title: '商品名称', width: 200 },
  { key: 'itemCode', title: '货号', width: 100 },
  { key: 'barcode', title: '条码', width: 120 },
  { key: 'specification', title: '规格', width: 100 },
  { key: 'model', title: '型号', width: 100 },
  { key: 'origin', title: '产地', width: 100 },
  { key: 'brand', title: '品牌', width: 80 },
  { key: 'customField1', title: '表体自定义1(数字)', width: 120 },
  { key: 'customField2', title: '表体自定义2(数字)', width: 120 },
  { key: 'customField3', title: '表体自定义3(数字)', width: 120 },
  { key: 'customField4', title: '表体自定义4(文本)', width: 120 },
  { key: 'customField5', title: '表体自定义5(文本)', width: 120 },
  { key: 'customField6', title: '表体自定义6(数字)', width: 120 },
  { key: 'customField7', title: '表体自定义7(数字)', width: 120 },
  { key: 'customField8', title: '表体自定义8(往来单位)', width: 130 },
  { key: 'customField9', title: '表体自定义9(职员)', width: 120 },
  { key: 'customField10', title: '表体自定义10(部门)', width: 120 },
  { key: 'unit', title: '单位', width: 70 },
  { key: 'smallUnit', title: '小单位', width: 70 },
  { key: 'smallUnitQuantity', title: '小单位数量', width: 100, align: 'right' },
  { key: 'conversionRelation', title: '换算关系', width: 100 },
  { key: 'convertedQuantity', title: '换算结果', width: 100, align: 'right' },
  { key: 'bigPack', title: '大包装', width: 80, align: 'right' },
  { key: 'midPack', title: '中包装', width: 80, align: 'right' },
  { key: 'smallPack', title: '小包装', width: 80, align: 'right' },
  { key: 'quantity', title: '订货数量', width: 90, align: 'right' },
  { key: 'receivedQuantity', title: '已收数量', width: 90, align: 'right' },
  { key: 'unreceiveQuantity', title: '未收数量', width: 90, align: 'right' },
  { key: 'terminatedQuantity', title: '终止数量', width: 90, align: 'right' },
  { key: 'terminatedAmount', title: '终止金额', width: 100, align: 'right' },
  { key: 'unitPrice', title: '单价', width: 100, align: 'right' },
  { key: 'smallUnitPrice', title: '小单位单价', width: 110, align: 'right' },
  { key: 'amount', title: '金额', width: 100, align: 'right' },
  { key: 'discountRate', title: '优惠折扣(%)', width: 100, align: 'right' },
  { key: 'discountedUnitPrice', title: '优惠后单价', width: 110, align: 'right' },
  { key: 'discountedAmount', title: '优惠后金额', width: 110, align: 'right' },
  { key: 'weight', title: '重量(kg)', width: 90, align: 'right' },
  { key: 'volume', title: '体积(m³)', width: 90, align: 'right' },
  { key: 'itemRemark', title: '明细备注', width: 150 },
  { key: 'remark', title: '单据备注', width: 150 },
  { key: 'summary', title: '摘要', width: 130 },
  { key: 'attachment', title: '附件', width: 70 },
  { key: 'createByName', title: '制单人', width: 90 },
  { key: 'auditorName', title: '审核人', width: 90 },
  { key: 'createTime', title: '制单时间', width: 130 },
  { key: 'submitTime', title: '提交时间', width: 130 },
  { key: 'printCount', title: '打印次数', width: 90 },
  { key: 'action', title: '操作', width: 120, fixed: 'right', slotName: 'actionCell' },
]

const activeColumns = computed(() => subTab.value === 'byDoc' ? byDocColumns : byDetailColumns)
const storageKey = computed(() => subTab.value === 'byDoc' ? 'purchase-list-columns-doc' : 'purchase-list-columns-detail')

// ═══ 分页 ═══
const paginationConfig = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 数据 ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const activeTableData = computed(() => tableData.value)

// ═══ 数据加载 ═══
const fetchData = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = { ...searchForm, current: paginationConfig.current, size: paginationConfig.pageSize }
    if (dateRange.value) {
      params.dateStart = dateRange.value[0].format('YYYY-MM-DD')
      params.dateEnd = dateRange.value[1].format('YYYY-MM-DD')
    }
    Object.keys(params).forEach(k => { if (params[k] === undefined || params[k] === null || params[k] === '') delete params[k] })

    if (subTab.value === 'byDoc') {
      const res = await request.get('/erp/purchase/order/doc-query/page', { params })
      const data = res.data || res
      tableData.value = data?.records || []
      paginationConfig.total = Number(data?.total) || 0
    } else {
      const res = await request.get('/erp/purchase/order/detail-query/page', { params })
      const data = res.data || res
      tableData.value = data?.records || []
      paginationConfig.total = Number(data?.total) || 0
    }

    // 更新统计
    stats.value.draft = tableData.value.filter((r: any) => r.status === 0).length
    stats.value.pending = tableData.value.filter((r: any) => r.status === 1).length
    stats.value.approved = tableData.value.filter((r: any) => r.status === 2).length
    stats.value.completed = tableData.value.filter((r: any) => r.status >= 4).length
    stats.value.totalAmount = tableData.value.reduce((s: number, r: any) => s + (r.billAmount || r.amount || 0), 0)
  } catch (error) {
    console.warn('[采购订单] 获取数据失败', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { paginationConfig.current = 1; fetchData() }
const handlePageChange = (page: number, size: number) => { paginationConfig.current = page; paginationConfig.pageSize = size; fetchData() }

// Tab 切换
watch(subTab, () => {
  applyHiddenSearchFields()
  handleSearch()
})

// ═══ 查询方案 ═══
const SCHEME_STORAGE_KEY = 'purchase-order-schemes'
const queryScheme = ref('')
const schemes = ref<string[]>([])
function loadSchemes() {
  try {
    const raw = localStorage.getItem(SCHEME_STORAGE_KEY)
    schemes.value = raw ? JSON.parse(raw) : []
  } catch { schemes.value = [] }
}
function persistSchemes() {
  try { localStorage.setItem(SCHEME_STORAGE_KEY, JSON.stringify(schemes.value)) } catch { /* ignore */ }
}
function handleSaveScheme() {
  const name = window.prompt('请输入方案名称：')
  if (!name) return
  if (!schemes.value.includes(name)) {
    schemes.value.push(name)
    persistSchemes()
  }
  queryScheme.value = name
  // 保存当前搜索条件作为方案快照
  try { localStorage.setItem(`purchase-order-scheme:${name}`, JSON.stringify({ ...searchForm, dateRange: dateRange.value })) } catch { /* ignore */ }
  message.success(`查询方案「${name}」已保存`)
}
function handleSchemeChange(val: string) {
  if (!val) return
  try {
    const raw = localStorage.getItem(`purchase-order-scheme:${val}`)
    if (raw) {
      const saved = JSON.parse(raw)
      Object.assign(searchForm, saved)
      if (saved.dateRange) dateRange.value = saved.dateRange
      handleSearch()
    }
  } catch { /* ignore */ }
}

// ═══ 行选择（批量操作） ═══
const selectedRowKeys = ref<any[]>([])
function handleRowCheck(record: any, index: number, checked: boolean) {
  const key = record.orderId ?? record.id
  if (checked) {
    if (!selectedRowKeys.value.includes(key)) selectedRowKeys.value.push(key)
  } else {
    selectedRowKeys.value = selectedRowKeys.value.filter((k: any) => k !== key)
  }
}
function handleRowCheckAll(checked: boolean, records: any[]) {
  if (checked) {
    selectedRowKeys.value = records.map((r: any) => r.orderId ?? r.id)
  } else {
    selectedRowKeys.value = []
  }
}

// ═══ 页面配置/打印/导入 ═══
const showPageConfig = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)
const importLoading = ref(false)

// ═══ 页面配置（查询条件显隐 + 功能按钮），持久化到后端 ═══
const PAGE_CONFIG_MODULE = 'purchase'
const PAGE_CONFIG_PAGE = 'order'

interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_DOC_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'orderNo', label: '单据编号', visible: true },
  { key: 'supplierName', label: '供应商', visible: true },
  { key: 'purchaserName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: false },
  { key: 'createByName', label: '制单人', visible: false },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'remark', label: '单据备注', visible: false },
  { key: 'submitterName', label: '提交人', visible: false },
  { key: 'auditorName', label: '审核人', visible: false },
  { key: 'extNum1Start', label: '自定义字段1(数字)', visible: false },
  { key: 'extNum2Start', label: '自定义字段2(数字)', visible: false },
  { key: 'extText1', label: '自定义字段3(文本)', visible: false },
  { key: 'extText2', label: '自定义字段4(文本)', visible: false },
  { key: 'extText3', label: '自定义字段5(文本)', visible: false },
  { key: 'printCountStart', label: '打印次数', visible: false },
]

const DEFAULT_DETAIL_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'orderNo', label: '单据编号', visible: true },
  { key: 'productName', label: '商品', visible: true },
  { key: 'supplierName', label: '供应商', visible: true },
  { key: 'purchaserName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: false },
  { key: 'createByName', label: '制单人', visible: false },
  { key: 'auditorName', label: '审核人', visible: false },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'priceStatus', label: '单价状态', visible: true },
  { key: 'remark', label: '单据备注', visible: false },
  { key: 'itemRemark', label: '明细备注', visible: false },
  { key: 'isGift', label: '是否赠品', visible: false },
  { key: 'status', label: '单据状态', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'batchImport', label: '批量导入', enabled: true },
  { key: 'batchPrint', label: '批量打印', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'pageConfig', label: '配置', enabled: true },
]

const docQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_DOC_QUERY_FIELDS.map(f => ({ ...f })))
const detailQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_DETAIL_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

async function loadPageConfig() {
  try {
    const raw = await userPageConfigApi.get(PAGE_CONFIG_MODULE, PAGE_CONFIG_PAGE)
    if (raw) {
      const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
      if (parsed.queryFields) {
        docQueryConfig.value = DEFAULT_DOC_QUERY_FIELDS.map(df => {
          const saved = parsed.queryFields.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        })
      }
      if (parsed.detailQueryFields) {
        detailQueryConfig.value = DEFAULT_DETAIL_QUERY_FIELDS.map(df => {
          const saved = parsed.detailQueryFields.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        })
      }
      if (parsed.functionButtons) {
        functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
          const saved = parsed.functionButtons.find((f: FunctionButtonSetting) => f.key === bf.key)
          return saved ? { ...bf, ...saved } : { ...bf }
        })
      }
      // 同步 localStorage，供 PageConfigPanel 打开时读取
      localStorage.setItem('purchase-order-page-config', JSON.stringify({
        queryFields: docQueryConfig.value,
        detailQueryFields: detailQueryConfig.value,
        functionButtons: functionButtonConfig.value,
      }))
    }
  } catch {
    // API 不可用时保持默认
  }
}

async function handlePageConfigChange(config: any) {
  const payload = {
    queryFields: config.queryFields || docQueryConfig.value,
    detailQueryFields: config.detailQueryFields || detailQueryConfig.value,
    functionButtons: config.functionButtons || functionButtonConfig.value,
  }
  try {
    await userPageConfigApi.save(PAGE_CONFIG_MODULE, PAGE_CONFIG_PAGE, JSON.stringify(payload))
  } catch {
    // 静默失败
  }
  await loadPageConfig()
  applyHiddenSearchFields()
}

function applyHiddenSearchFields() {
  const config = subTab.value === 'byDoc' ? docQueryConfig.value : detailQueryConfig.value
  hiddenSearchFieldKeys.value = config.filter(f => !f.visible).map(f => f.key)
}

// ═══ 工具栏操作 ═══
const handleToolbarAction = (action: string) => {
  switch (action) {
    case 'add': router.push('/erp/purchase/form'); break
    case 'refresh': fetchData(); break
    case 'batchImport': fileInputRef.value?.click(); break
    case 'batchPrint': handleBatchPrint(); break
    case 'printF8': handleBatchPrint(); break
    case 'export': handleExport(); break
    case 'pageConfig': showPageConfig.value = true; break
  }
}

// ═══ 打印（模板渲染，走 erp-printing 系统级路径）═══
// 后端已为 pageCode='purchase' 注册 PurchaseOrderPrintDataProvider，并有已发布的默认模板，
// 所以这里只需给单据主键（取数 / 挑模板 / 渲染都在服务端）；
// 原先是自写的「打印模板下拉 + 只把打印次数 +1」的假弹窗，点了不会出纸。
const printDialogRef = ref<InstanceType<typeof PrintDialog> | null>(null)
const printData = ref<Record<string, any>>({})
/** 本次打印涉及的订单 ID，打印成功后回写打印次数 */
const printingIds = ref<string[]>([])

function handleBatchPrint() {
  if (!selectedRowKeys.value.length) {
    message.warning('请先勾选要打印的采购订单')
    return
  }
  printData.value = { id: selectedRowKeys.value[0] }
  printingIds.value = selectedRowKeys.value.map((k: any) => String(k))
  printDialogRef.value?.open()
}

/** 打印成功 → 累加「打印次数」后刷新列表 */
async function handlePrintSuccess() {
  const ids = printingIds.value
  printingIds.value = []
  if (ids.length) {
    try {
      await request.post('/erp/purchase/order/batch-print', { ids })
    } catch (e) {
      console.warn('[采购订单] 打印次数回写失败', e)
    }
  }
  fetchData()
}


async function handleImportFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  const fd = new FormData()
  fd.append('file', file)
  importLoading.value = true
  try {
    const res: any = await request.post('/erp/purchase/order/import', fd)
    showImportResult(res?.data)
    fetchData()
  } catch {
    message.error('导入失败，请检查文件格式')
  } finally {
    importLoading.value = false
    input.value = ''
  }
}

const handleExport = async () => {
  try {
    const blob = await request.get('/erp/purchase/order/export', { params: { ...searchForm }, responseType: 'blob' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `采购订单_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch { message.error('导出失败') }
}

// ═══ 行操作 ═══
const goDetail = (record: any) => { router.push(`/erp/purchase/form?id=${record.orderId || record.id}`) }
const goEdit = (record: any) => { router.push(`/erp/purchase/form?id=${record.orderId || record.id}`) }

const handleSubmit = (record: any) => {
  Modal.confirm({
    title: '提交审批', content: `确认提交采购单 ${record.orderNo} 进行审批吗？`,
    okText: '确认提交', cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/purchase/order/${record.orderId || record.id}/submit`)
        message.success('已提交审批'); fetchData()
      } catch { message.error('提交失败') }
    },
  })
}

const handleApprove = (record: any) => {
  Modal.confirm({
    title: '审批确认', content: `确认审批通过采购单 ${record.orderNo} 吗？`,
    okText: '确认审批', cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/purchase/order/${record.orderId || record.id}/approve`)
        message.success('审批通过'); fetchData()
      } catch { message.error('审批失败') }
    },
  })
}

// ═══ 状态辅助 ═══
const getStatusColor = (status: number) => {
  const map: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'blue', 3: 'blue', 4: 'red', 5: 'processing', 6: 'green' }
  return map[status] || 'default'
}
const getStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已审批', 3: '已下达', 4: '已取消', 5: '履行中', 6: '已完成' }
  return map[status] || '未知'
}

const handleError = (err: any) => { console.warn('[采购订单] ErrorBoundary:', err) }

// ═══ 键盘快捷键 ═══
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); fetchData() }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); router.push('/erp/purchase/form') }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') { e.preventDefault(); handleExport() }
}

onMounted(async () => {
  window.addEventListener('keydown', handleKeydown)
  loadSchemes()
  // 清理旧格式列配置（早期 useColumnConfig 写入 {_version, columns} 包装，BillDetailTable 期望纯数组）
  try {
    for (const k of ['purchase-list-columns-doc', 'purchase-list-columns-detail']) {
      const raw = localStorage.getItem(k)
      if (raw) {
        const parsed = JSON.parse(raw)
        if (parsed && typeof parsed === 'object' && !Array.isArray(parsed) && Array.isArray(parsed.columns)) {
          localStorage.removeItem(k)
        }
      }
    }
  } catch { /* ignore */ }
  await loadPageConfig()
  applyHiddenSearchFields()
  fetchData()
})
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>
