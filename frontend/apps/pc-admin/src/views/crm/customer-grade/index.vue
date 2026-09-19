<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        客户分级（CRM → 客户管理 → 客户分级，菜单 70303）
        · 规格：docs/Yh-Spec/手动整理对标开发文档/CRM模块/客户分级开发文档.md
        · 路线 A：ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillDetailTable
        · 左分类树 = 等级过滤（全部 / 未分级 / VIP / 重要 / 普通 / 潜在）
        · 本轮补齐：路线 A 外壳、表头齿轮列配置、页面配置弹窗、统计卡下钻
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="true"
        category-title="客户等级"
        :category-tree-data="levelTree"
        :selected-category-id="selectedLevel"
        :show-table-footer="true"
        @category-select="handleLevelSelect"
      >
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="handleRefresh"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="isButtonEnabled('export')"
              size="small"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
            <a-tooltip
              title="页面配置"
              placement="bottom"
            >
              <a-button
                size="small"
                @click="showPageConfig = true"
              >
                <SettingOutlined />
              </a-button>
            </a-tooltip>
          </a-space>
        </template>

        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div
                v-if="isFieldVisible('keyword')"
                class="search-item"
              >
                <span class="search-label">关键字</span>
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="客户名称/编码"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('customerLevel')"
                class="search-item"
              >
                <span class="search-label">等级</span>
                <a-select
                  v-model:value="searchForm.customerLevel"
                  placeholder="全部等级"
                  size="small"
                  allow-clear
                  :options="LEVEL_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('status')"
                class="search-item"
              >
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部状态"
                  size="small"
                  allow-clear
                  :options="STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div class="search-item search-actions">
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
          </div>
        </template>

        <template #table>
          <div class="table-area">
            <div class="stat-cards">
              <ARStatCards
                :items="levelCards"
                :loading="statsLoading"
                clickable
                @card-click="handleCardClick"
              />
            </div>
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="crm-customer-grade-table-columns"
              global-config-key="crm-customer-grade-table-columns"
            >
              <template #customerNameCell="{ record }">
                <span v-if="record.__ghost" />
                <span v-else>{{ record.customerName || '-' }}</span>
              </template>

              <template #customerLevelCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="levelColor(record.customerLevel)"
                >
                  {{ levelText(record.customerLevel, record.customerLevelDesc) }}
                </a-tag>
                <span v-else>-</span>
              </template>

              <template #creditLimitCell="{ record }">
                <span v-if="record.__ghost" />
                <span v-else>{{ formatMoney(record.creditLimit) }}</span>
              </template>

              <template #currentDebtCell="{ record }">
                <span v-if="record.__ghost" />
                <span v-else>{{ formatMoney(record.currentDebt) }}</span>
              </template>

              <template #tradeAmountCell="{ record }">
                <span v-if="record.__ghost" />
                <span v-else>{{ formatMoney(record.tradeAmount) }}</span>
              </template>

              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="record.status === 1 ? 'green' : 'default'"
                >
                  {{ record.status === 1 ? '正常' : '停用' }}
                </a-tag>
                <span v-else>-</span>
              </template>

              <template #actionCell="{ record }">
                <a-button
                  v-if="!record.__ghost"
                  type="link"
                  size="small"
                  @click="openLevelModal(record)"
                >
                  调整等级
                </a-button>
              </template>
            </BillDetailTable>
          </div>
        </template>

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

      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFieldsConfig"
        :function-buttons-config="functionButtonConfig"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="pageConfigStorageKey"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 调整客户等级 ═══ -->
      <a-modal
        v-model:open="levelVisible"
        title="调整客户等级"
        :confirm-loading="levelSaving"
        ok-text="保存"
        cancel-text="取消"
        :width="480"
        @ok="handleLevelSave"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="客户">
            <span>{{ currentCustomer?.customerName || '-' }}</span>
          </a-form-item>
          <a-form-item label="当前等级">
            <a-tag :color="levelColor(currentCustomer?.customerLevel)">
              {{ levelText(currentCustomer?.customerLevel, currentCustomer?.customerLevelDesc) }}
            </a-tag>
          </a-form-item>
          <a-form-item
            label="新等级"
            required
          >
            <a-select
              v-model:value="newLevel"
              placeholder="请选择新等级"
              :options="LEVEL_OPTIONS"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { ReloadOutlined, DownloadOutlined, SettingOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { QueryFieldSetting, FunctionButtonSetting } from '@/components/PageConfigPanel/index.vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import type { StatCardItem } from '@/components/ARReportPage/types'
import { crmCustomerApi, type CrmCustomer } from '@/api/crm'

defineOptions({ name: 'CrmCustomerGrade' })

// ═══ CRM 客户等级字典（与 crm_customer.customer_level 一致） ═══
// 注意：这是「CRM 服务分级」，与 ERP 的 erp_customer_level（价格等级）是两回事，勿混用。
const LEVEL_OPTIONS = [
  { label: 'VIP客户', value: 1 },
  { label: '重要客户', value: 2 },
  { label: '普通客户', value: 3 },
  { label: '潜在客户', value: 4 }
]
const LEVEL_TEXT: Record<number, string> = { 1: 'VIP客户', 2: '重要客户', 3: '普通客户', 4: '潜在客户' }
const LEVEL_COLOR: Record<number, string> = { 1: 'red', 2: 'orange', 3: 'blue', 4: 'default' }
const STATUS_OPTIONS = [
  { label: '正常', value: 1 },
  { label: '停用', value: 0 }
]

function levelText(v: number | undefined, desc?: string): string {
  return desc || (v && LEVEL_TEXT[v]) || '未分级'
}
function levelColor(v: number | undefined): string {
  return (v && LEVEL_COLOR[v]) || 'default'
}
function formatMoney(val: any): string {
  if (val === null || val === undefined || Number.isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 左分类树：等级下钻 ═══
const selectedLevel = ref<string | number>('all')
const levelTree = computed(() => [
  { key: 'all', title: '全部客户' },
  { key: 'none', title: '未分级' },
  ...LEVEL_OPTIONS.map(o => ({ key: String(o.value), title: o.label }))
])

function handleLevelSelect(keys: (string | number)[]) {
  const key = keys && keys.length ? keys[0] : 'all'
  selectedLevel.value = key
  if (key === 'all') {
    searchForm.customerLevel = undefined
    searchForm.unleveled = undefined
  } else if (key === 'none') {
    searchForm.customerLevel = undefined
    searchForm.unleveled = true
  } else {
    searchForm.customerLevel = Number(key)
    searchForm.unleveled = undefined
  }
  pagination.current = 1
  fetchList()
}

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '关键字', visible: true },
  { key: 'customerLevel', label: '等级', visible: true },
  { key: 'status', label: '状态', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]
const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))
const pageConfigStorageKey = 'crm-customer-grade-page-config'

function isFieldVisible(key: string): boolean {
  const found = queryFieldsConfig.value.find(f => f.key === key)
  return found ? found.visible : true
}
function isButtonEnabled(key: string): boolean {
  const found = functionButtonConfig.value.find(b => b.key === key)
  return found ? found.enabled : true
}
function handlePageConfigChange(config: any) {
  if (config?.queryFields) queryFieldsConfig.value = config.queryFields
  if (config?.functionButtons) functionButtonConfig.value = config.functionButtons
}

// ═══ 查询 / 分页 ═══
const searchForm = reactive({
  keyword: '',
  customerLevel: undefined as number | undefined,
  status: undefined as number | undefined,
  unleveled: undefined as boolean | undefined,
})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const loading = ref(false)
const tableData = ref<any[]>([])

const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 100, fixed: 'left' },
  { key: 'customerCode', title: '客户编码', type: 'input', width: 140 },
  { key: 'customerName', title: '客户名称', type: 'slot', slotName: 'customerNameCell', width: 200 },
  { key: 'customerLevel', title: '等级', type: 'slot', slotName: 'customerLevelCell', width: 110 },
  { key: 'creditLimit', title: '信用额度', type: 'slot', slotName: 'creditLimitCell', width: 120 },
  { key: 'currentDebt', title: '当前欠款', type: 'slot', slotName: 'currentDebtCell', width: 120 },
  { key: 'tradeCount', title: '交易次数', type: 'input', width: 100 },
  { key: 'tradeAmount', title: '交易金额', type: 'slot', slotName: 'tradeAmountCell', width: 130 },
  { key: 'lastTradeDate', title: '最近交易', type: 'input', width: 120 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 90 },
  { key: 'salesPersonName', title: '负责人', type: 'input', width: 110 },
  { key: 'departmentName', title: '部门', type: 'input', width: 130, defaultHidden: true },
  { key: 'businessContact', title: '联系人', type: 'input', width: 110, defaultHidden: true },
  { key: 'businessContactPhone', title: '联系电话', type: 'input', width: 130, defaultHidden: true },
  { key: 'customerType', title: '客户类型', type: 'input', width: 110, defaultHidden: true },
  { key: 'customerSource', title: '客户来源', type: 'input', width: 110, defaultHidden: true },
  { key: 'potentialAmount', title: '潜在金额', type: 'input', width: 120, defaultHidden: true },
  { key: 'createdAt', title: '创建时间', type: 'input', width: 160, defaultHidden: true },
]

async function fetchList() {
  loading.value = true
  try {
    const res: any = await crmCustomerApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
      customerLevel: searchForm.customerLevel,
      status: searchForm.status,
    })
    let records = res?.records || []
    // 「未分级」是左树上的独立节点，后端无该参数 —— 在本页返回集上二次过滤
    if (searchForm.unleveled) {
      records = records.filter((r: any) => !r.customerLevel)
    }
    tableData.value = records
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[客户分级] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.customerLevel = undefined
  searchForm.status = undefined
  searchForm.unleveled = undefined
  selectedLevel.value = 'all'
  pagination.current = 1
  fetchList()
}

function handleRefresh() {
  fetchList()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 统计卡（全量聚合，反映整体分布；点击下钻到对应等级） ═══
const allCustomers = ref<CrmCustomer[]>([])
const statsLoading = ref(false)
const levelCards = computed<StatCardItem[]>(() => {
  const list = allCustomers.value
  const cards: StatCardItem[] = [{ label: '客户总数', value: list.length, suffix: '家' }]
  for (const opt of LEVEL_OPTIONS) {
    cards.push({
      label: opt.label,
      value: list.filter(c => c.customerLevel === opt.value).length,
      suffix: '家'
    })
  }
  return cards
})

async function loadLevelStats() {
  statsLoading.value = true
  try {
    const list = await crmCustomerApi.exportList()
    allCustomers.value = Array.isArray(list) ? list : []
  } catch (e) {
    console.warn('[客户分级] 等级分布统计获取失败', e)
    allCustomers.value = []
  } finally {
    statsLoading.value = false
  }
}

function handleCardClick(item: StatCardItem) {
  if (item.label === '客户总数') {
    handleLevelSelect(['all'])
    return
  }
  const found = LEVEL_OPTIONS.find(o => o.label === item.label)
  if (found) handleLevelSelect([String(found.value)])
}

// ═══ 调整等级 ═══
const levelVisible = ref(false)
const levelSaving = ref(false)
const currentCustomer = ref<any>(null)
const newLevel = ref<number | undefined>(undefined)

function openLevelModal(record: any) {
  // 深拷贝：原先直接持有表格行对象，刷新后弹窗会指向已失效的行
  currentCustomer.value = { ...record }
  newLevel.value = record.customerLevel
  levelVisible.value = true
}

async function handleLevelSave() {
  if (!currentCustomer.value) return
  if (!newLevel.value) {
    message.warning('请选择新等级')
    return
  }
  if (newLevel.value === currentCustomer.value.customerLevel) {
    message.info('新等级与当前等级相同，无需保存')
    return
  }
  levelSaving.value = true
  try {
    // 只提交需要变更的两个字段（MyBatis-Plus updateById 忽略 null，不会误清其它列）
    await crmCustomerApi.update(currentCustomer.value.id, {
      customerLevel: newLevel.value,
      customerLevelDesc: LEVEL_TEXT[newLevel.value]
    })
    message.success('客户等级已更新')
    levelVisible.value = false
    fetchList()
    loadLevelStats()
  } catch (e: any) {
    message.error(e?.message || '更新失败')
  } finally {
    levelSaving.value = false
  }
}

// ═══ 导出（带中文映射） ═══
function handleExport() {
  const rows = (tableData.value || []).filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const headers = ['客户编码', '客户名称', '等级', '信用额度', '当前欠款', '交易次数',
    '交易金额', '最近交易', '状态', '负责人']
  const lines = rows.map((r: any) => [
    r.customerCode, r.customerName, levelText(r.customerLevel, r.customerLevelDesc),
    r.creditLimit, r.currentDebt, r.tradeCount, r.tradeAmount, r.lastTradeDate,
    r.status === 1 ? '正常' : '停用', r.salesPersonName
  ])
  const escape = (v: any) => `"${String(v ?? '').replace(/"/g, '""')}"`
  const csv = '\uFEFF' + [headers, ...lines].map(row => row.map(escape).join(',')).join('\r\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `客户分级_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

function handleError(error: Error) {
  console.error('[客户分级] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  fetchList()
  loadLevelStats()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-item :deep(.ant-select) { min-width: 150px; }
.search-item :deep(.ant-input-affix-wrapper) { width: 180px; }
.search-actions { margin-left: auto; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.stat-cards { flex-shrink: 0; padding: 8px 0; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
