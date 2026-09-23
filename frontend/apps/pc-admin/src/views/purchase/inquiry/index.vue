<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <DocCenterLayout
        v-model:active-main-tab="mainTab"
        v-model:active-sub-tab="subTab"
        v-model:date-shortcut="dateShortcut"
        v-model:date-range="dateRange"
        v-model:search-values="searchForm"
        v-model:page-current="pagination.current"
        v-model:page-size="pagination.pageSize"
        :main-tabs="mainTabs"
        :sub-tabs="subTabs"
        :search-config="searchConfig"
        :hidden-field-keys="hiddenSearchFieldKeys"
        :stat-card-config="statCardConfig"
        :toolbar-config="toolbarConfig"
        :function-buttons-config="functionButtonConfig"
        :show-pagination="true"
        :page-total="pagination.total"
        :stats-data="stats"
        @search="handleSearch"
        @refresh="fetchData"
        @toolbar-action="handleToolbarAction"
        @page-change="handlePageChange"
      >
        <template #table>
          <BillDetailTable
            :columns="columns"
            v-model:data-source="tableData"
            :loading="loading"
            :view-mode="true"
            :storage-key="storageKey"
            style="height: 100%"
          >
            <template #inquiryNoCell="{ record }">
              <a-button type="link" size="small" @click="goDetail(record)">{{ record.inquiryNo }}</a-button>
            </template>
            <template #statusCell="{ record }">
              <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
            </template>
            <template #actionCell="{ record }">
              <a-space :size="4">
                <a-button type="link" size="small" @click="goDetail(record)">查看</a-button>
                <a-button v-if="isDraft(record)" type="link" size="small" @click="goEdit(record)">编辑</a-button>
                <a-button v-if="isDraft(record)" type="link" size="small" @click="handleSend(record)">发送</a-button>
                <a-dropdown v-if="hasMoreActions(record)">
                  <a-button type="link" size="small">
                    更多
                    <DownOutlined />
                  </a-button>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item v-if="canClose(record)" @click="handleClose(record)">关闭</a-menu-item>
                      <a-menu-item v-if="canCancel(record)" @click="handleCancel(record)">取消</a-menu-item>
                      <a-menu-item v-if="canDelete(record)" danger @click="handleDelete(record)">删除</a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </a-space>
            </template>
          </BillDetailTable>
        </template>
      </DocCenterLayout>

      <!-- 页面配置弹窗（查询条件显隐 + 功能按钮） -->
      <PageConfigPanel
        :key="'purchase-inquiry-page-config'"
        :open="showPageConfig"
        :storage-key="'purchase-inquiry-page-config'"
        :query-fields-config="queryFieldsConfig"
        :function-buttons-config="functionButtonConfig"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { DownOutlined } from '@ant-design/icons-vue'
import dayjs, { type Dayjs } from 'dayjs'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import DocCenterLayout from '@/components/DocCenterLayout/DocCenterLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { inquiryApi, userPageConfigApi } from '@/api/erp'
import request from '@/utils/request'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type {
  SearchConfigMap, StatCardConfigMap, ToolbarConfigMap,
} from '@/components/DocCenterLayout/types'

defineOptions({ name: 'PurchaseInquiryList' })
const router = useRouter()

// ═══ Tab 状态（单一页面） ═══
const mainTab = ref('all')
const subTab = ref('list')
const dateShortcut = ref('month')
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().startOf('month'), dayjs()])

const mainTabs = [{ key: 'all', label: '全部' }]
const subTabs = [{ key: 'list', label: '采购询价单' }]

// ═══ 状态定义（对齐后端 InquiryStatus 枚举声明顺序，0-7） ═══
// 后端实体用 @Enumerated(EnumType.STRING)，库里/接口里的 status 可能是枚举名字符串（如 'DRAFT'），
// 故下面统一用 toStatusCode 归一化成数字编号后再做展示与判断。
const STATUS_LIST = [
  { code: 0, enum: 'DRAFT', label: '草稿', color: 'default' },
  { code: 1, enum: 'PUBLISHED', label: '已发布', color: 'blue' },
  { code: 2, enum: 'QUOTING', label: '报价中', color: 'orange' },
  { code: 3, enum: 'DECISION_MADE', label: '已决策', color: 'cyan' },
  { code: 4, enum: 'CONTRACT_CREATED', label: '已生成合同', color: 'purple' },
  { code: 5, enum: 'CLOSED', label: '已关闭', color: 'default' },
  { code: 6, enum: 'COMPLETED', label: '已完成', color: 'green' },
  { code: 7, enum: 'CANCELLED', label: '已取消', color: 'red' },
] as const

/** 状态编号常量：语义化引用，避免代码里散落魔法数字 */
const STATUS = {
  DRAFT: 0, PUBLISHED: 1, QUOTING: 2, DECISION_MADE: 3,
  CONTRACT_CREATED: 4, CLOSED: 5, COMPLETED: 6, CANCELLED: 7,
} as const

/** 归一化状态：数字直接返回；枚举名字符串按枚举名/数字串兜底；无法识别返回 -1 */
const toStatusCode = (status: number | string | null | undefined): number => {
  if (status === undefined || status === null || status === '') return -1
  if (typeof status === 'number') return status
  const byName = STATUS_LIST.find(s => s.enum === status)
  if (byName) return byName.code
  const num = Number(status)
  return Number.isNaN(num) ? -1 : num
}

const getStatusText = (status: number | string) => {
  const item = STATUS_LIST[toStatusCode(status)]
  return item ? item.label : '未知'
}

const getStatusColor = (status: number | string) => {
  const item = STATUS_LIST[toStatusCode(status)]
  return item ? item.color : 'default'
}

/** 草稿态才允许编辑 / 发送 */
const isDraft = (record: any) => toStatusCode(record?.status) === STATUS.DRAFT
/** 发布后流转中的单据可关闭（已决策/已生成合同仍可手工关闭） */
const canClose = (record: any) => [
  STATUS.PUBLISHED, STATUS.QUOTING, STATUS.DECISION_MADE, STATUS.CONTRACT_CREATED,
].includes(toStatusCode(record?.status) as any)
/** 草稿或询价尚在途时可取消 */
const canCancel = (record: any) => [
  STATUS.DRAFT, STATUS.PUBLISHED, STATUS.QUOTING,
].includes(toStatusCode(record?.status) as any)
/** 仅未流转（草稿）或已终结（已关闭/已取消）的单据允许删除，已完成单据保留备查 */
const canDelete = (record: any) => [
  STATUS.DRAFT, STATUS.CLOSED, STATUS.CANCELLED,
].includes(toStatusCode(record?.status) as any)
const hasMoreActions = (record: any) => canClose(record) || canCancel(record) || canDelete(record)

const statusOptions = STATUS_LIST.map(s => ({ label: s.label, value: s.code }))

// ═══ 搜索 ═══
const searchForm = reactive<Record<string, any>>({})

// 搜索字段（日期由 DocCenterLayout 固定显示在第一格；7 列自适应横向网格由组件内置）
const listSearchFields: SearchConfigMap = {
  'all.list': [
    { key: 'inquiryNo', label: '询价单号', type: 'input' },
    { key: 'title', label: '询价标题', type: 'input' },
    { key: 'status', label: '询价状态', type: 'select', options: statusOptions },
  ],
}

const searchConfig: SearchConfigMap = listSearchFields

// 搜索字段隐藏配置
const hiddenSearchFieldKeys = ref<string[]>([])

// ═══ 统计卡片（按当前页近似统计） ═══
const stats = ref<Record<string, number>>({ draft: 0, published: 0, quoting: 0, decided: 0, completed: 0 })

const statCardConfig: StatCardConfigMap = {
  'all.list': [
    { label: '草稿', valueKey: 'draft', color: '#d9d9d9' },
    { label: '已发布', valueKey: 'published', color: '#1890ff' },
    { label: '报价中', valueKey: 'quoting', color: '#faad14' },
    { label: '已决策', valueKey: 'decided', color: '#13c2c2' },
    { label: '已完成', valueKey: 'completed', color: '#52c41a' },
  ],
}

// ═══ 工具栏 ═══
const toolbarConfig: ToolbarConfigMap = {
  'all.list': [
    { key: 'refresh', label: '刷新' },
    { key: 'pageConfig', label: '配置', icon: 'SettingsOutlined', visibleFor: () => true },
  ],
}

// ═══ 表格列 ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'inquiryNo', title: '询价单号', width: 160, fixed: 'left', slotName: 'inquiryNoCell' },
  { key: 'title', title: '询价标题', width: 200 },
  { key: 'inquiryType', title: '询价类型', width: 110 },
  { key: 'supplierName', title: '供应商', width: 180 },
  { key: 'inquiryDate', title: '询价日期', width: 110, formatter: (v: any, r: any) => v || r?.createdAt || '' },
  { key: 'deadlineDate', title: '报价截止日期', width: 150 },
  { key: 'urgencyLevel', title: '紧急程度', width: 90 },
  { key: 'quoteCount', title: '报价数', width: 80, align: 'right' },
  { key: 'budget', title: '预算金额', width: 110, align: 'right' },
  { key: 'status', title: '状态', width: 100, slotName: 'statusCell' },
  { key: 'requirementDesc', title: '需求说明', width: 200 },
  { key: 'creatorName', title: '创建人', width: 100, formatter: (v: any, r: any) => v || (r?.createdBy ? `用户${r.createdBy}` : '') },
  { key: 'createTime', title: '创建时间', width: 160, formatter: (v: any, r: any) => v || r?.createdAt || '' },
  { key: 'action', title: '操作', width: 190, fixed: 'right', slotName: 'actionCell' },
]

const storageKey = 'purchase-inquiry-list-columns'

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 数据 ═══
const loading = ref(false)
const tableData = ref<any[]>([])

// ═══ 数据加载 ═══
const fetchData = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    // 后端分页接口仅支持 keyword（匹配询价标题/询价单号）与 status 两个条件
    const keywordParts: string[] = []
    if (searchForm.inquiryNo) keywordParts.push(String(searchForm.inquiryNo))
    if (searchForm.title) keywordParts.push(String(searchForm.title))
    if (keywordParts.length) params.keyword = keywordParts.join(' ')
    // 查询区用数字编号，后端按枚举名（varchar）过滤，这里转成枚举名字符串下发
    if (searchForm.status !== undefined && searchForm.status !== null && searchForm.status !== '') {
      const matched = STATUS_LIST.find(s => s.code === Number(searchForm.status))
      params.status = matched ? matched.enum : searchForm.status
    }
    if (dateRange.value) {
      const [from, to] = dateRange.value
      if (from) params.startDate = from.format('YYYY-MM-DD')
      if (to) params.endDate = to.format('YYYY-MM-DD')
    }

    const res: any = await inquiryApi.page(params)
    const data = res?.records ? res : (res?.data || res)
    tableData.value = data?.records || []
    pagination.total = Number(data?.total) || 0

    // 更新统计（基于当前页近似）
    const countByStatus = (code: number) => tableData.value.filter((r: any) => toStatusCode(r?.status) === code).length
    stats.value.draft = countByStatus(STATUS.DRAFT)
    stats.value.published = countByStatus(STATUS.PUBLISHED)
    stats.value.quoting = countByStatus(STATUS.QUOTING)
    stats.value.decided = countByStatus(STATUS.DECISION_MADE)
    stats.value.completed = countByStatus(STATUS.COMPLETED)
  } catch (error) {
    console.warn('[采购询价] 获取数据失败', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handlePageChange = (page: number, size: number) => { pagination.current = page; pagination.pageSize = size; fetchData() }

// ═══ 页面配置（查询条件显隐 + 功能按钮） ═══
const PAGE_CONFIG_MODULE = 'purchase-inquiry'
const PAGE_CONFIG_PAGE = 'list'

interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = listSearchFields['all.list'].map(f => ({ key: f.key, label: f.label, visible: true }))
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = toolbarConfig['all.list'].map(b => ({ key: b.key, label: b.label, enabled: true }))

const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

async function loadPageConfig() {
  try {
    const raw = await userPageConfigApi.get(PAGE_CONFIG_MODULE, PAGE_CONFIG_PAGE)
    if (raw) {
      const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
      if (parsed.queryFields) {
        queryFieldsConfig.value = DEFAULT_QUERY_FIELDS.map(df => {
          const saved = parsed.queryFields.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        })
      }
      if (parsed.functionButtons) {
        functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
          const saved = parsed.functionButtons.find((f: FunctionButtonSetting) => f.key === bf.key)
          return saved ? { ...bf, ...saved } : { ...bf }
        })
      }
      localStorage.setItem('purchase-inquiry-page-config', JSON.stringify({
        queryFields: queryFieldsConfig.value,
        functionButtons: functionButtonConfig.value,
      }))
    }
  } catch { /* API 不可用时保持默认 */ }
}

async function handlePageConfigChange(config: any) {
  const payload = {
    queryFields: config.queryFields || queryFieldsConfig.value,
    functionButtons: config.functionButtons || functionButtonConfig.value,
  }
  try { await userPageConfigApi.save(PAGE_CONFIG_MODULE, PAGE_CONFIG_PAGE, JSON.stringify(payload)) } catch { /* 静默失败 */ }
  await loadPageConfig()
  applyHiddenSearchFields()
}

function applyHiddenSearchFields() {
  hiddenSearchFieldKeys.value = queryFieldsConfig.value.filter(f => !f.visible).map(f => f.key)
}

// ═══ 工具栏操作 ═══
const showPageConfig = ref(false)

const handleToolbarAction = (action: string) => {
  switch (action) {
    case 'refresh': fetchData(); break
    case 'pageConfig': showPageConfig.value = true; break
  }
}

// ═══ 行操作 ═══
const goDetail = (record: any) => { router.push(`/purchase/inquiry/${record.id}`) }
const goEdit = (record: any) => { router.push(`/purchase/inquiry/${record.id}`) }

/** 发送询价：后端 send 与 publish 为同一接口，统一走 inquiryApi.send */
const handleSend = (record: any) => {
  Modal.confirm({
    title: '发送询价',
    content: `确认发送询价单 ${record.inquiryNo} 给供应商吗？`,
    okText: '确认发送', cancelText: '取消',
    onOk: async () => {
      try {
        await inquiryApi.send(record.id)
        message.success('已发送')
        fetchData()
      } catch { message.error('发送失败') }
    },
  })
}

/** 关闭询价：inquiryApi 未封装，直接调后端接口 */
const handleClose = (record: any) => {
  Modal.confirm({
    title: '关闭询价',
    content: `确认关闭询价单 ${record.inquiryNo} 吗？关闭后不再接收报价。`,
    okText: '确认关闭', cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/purchase/inquiry/${record.id}/close`)
        message.success('已关闭')
        fetchData()
      } catch { message.error('关闭失败') }
    },
  })
}

/** 取消询价：inquiryApi 未封装，直接调后端接口 */
const handleCancel = (record: any) => {
  Modal.confirm({
    title: '取消询价',
    content: `确认取消询价单 ${record.inquiryNo} 吗？`,
    okText: '确认取消', cancelText: '再想想',
    onOk: async () => {
      try {
        await request.post(`/erp/purchase/inquiry/${record.id}/cancel`)
        message.success('已取消')
        fetchData()
      } catch { message.error('取消失败') }
    },
  })
}

const handleDelete = (record: any) => {
  Modal.confirm({
    title: '删除询价',
    content: `确认删除询价单 ${record.inquiryNo} 吗？删除后不可恢复。`,
    okText: '确认删除', okType: 'danger', cancelText: '取消',
    onOk: async () => {
      try {
        await inquiryApi.delete(record.id)
        message.success('已删除')
        fetchData()
      } catch { message.error('删除失败') }
    },
  })
}

const handleError = (err: any) => { console.warn('[采购询价] ErrorBoundary:', err) }

onMounted(async () => {
  await loadPageConfig()
  applyHiddenSearchFields()
  fetchData()
})
</script>
