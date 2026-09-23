<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        客户公海（CRM → 客户管理 → 客户公海，菜单 80202）
        · 本页把后端 `CustomerPoolController` 的 10 个端点接出来 ——
          此前这些能力**前端零引用、无菜单**，用户完全不可达（CRM README §2.3 / §7 P0）
        · 业界依据：公海池（自动回收 / 领取上限 / 分配规则）在 Odoo **无官方文档**，
          依据取自用友云社区《CRM-客户公海&客户》（B 级）与同业实践（C 级），
          故本页按「本系统标准形态」实现，不作"业界标准如此"的表述
        · 路线 A：ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillDetailTable
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="true"
        category-title="池状态"
        :category-tree-data="statusTree"
        :selected-category-id="selectedStatusKey"
        :show-table-footer="true"
        @category-select="handleStatusSelect"
      >
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('autoRecovery')"
              size="small"
              :loading="recovering"
              @click="handleAutoRecovery"
            >
              <SyncOutlined /> 执行自动回收
            </a-button>
            <a-button
              v-if="isButtonEnabled('checkExpired')"
              size="small"
              :loading="checking"
              @click="handleCheckExpired"
            >
              <ClockCircleOutlined /> 检查过期
            </a-button>
          </a-space>
        </template>

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
                <span class="search-label">关键词</span>
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="客户名称/编码"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('poolReason')"
                class="search-item"
              >
                <span class="search-label">放入原因</span>
                <a-select
                  v-model:value="searchForm.poolReason"
                  placeholder="全部原因"
                  size="small"
                  allow-clear
                  :options="REASON_OPTIONS"
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
                :items="statCards"
                :loading="loading"
              />
            </div>
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="crm-customer-pool-table-columns"
              global-config-key="crm-customer-pool-table-columns"
            >
              <template #customerNameCell="{ record }">
                <span v-if="record.__ghost" />
                <span v-else>{{ record.customerName || '-' }}</span>
              </template>

              <template #poolReasonCell="{ record }">
                <span v-if="record.__ghost" />
                <a-tag
                  v-else
                  :color="reasonColor(record.poolReason)"
                >
                  {{ record.poolReasonDesc || reasonText(record.poolReason) }}
                </a-tag>
              </template>

              <template #statusCell="{ record }">
                <span v-if="record.__ghost" />
                <a-tag
                  v-else
                  :color="statusColor(record.status)"
                >
                  {{ record.statusDesc || statusText(record.status) }}
                </a-tag>
              </template>

              <template #expireTimeCell="{ record }">
                <span v-if="record.__ghost" />
                <span
                  v-else
                  :class="{ 'text-overdue': isOverdue(record) }"
                >
                  {{ record.expireTime ? fmtTime(record.expireTime) : '-' }}
                </span>
              </template>

              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    v-if="record.status === 1"
                    type="link"
                    size="small"
                    :loading="actingId === record.id"
                    @click="handleClaim(record)"
                  >
                    领取
                  </a-button>
                  <a-popconfirm
                    v-if="record.status === 1 || record.status === 2"
                    title="确定退回公海池？"
                    ok-text="退回"
                    cancel-text="取消"
                    @confirm="handleReturn(record)"
                  >
                    <a-button
                      type="link"
                      size="small"
                      danger
                    >
                      退回
                    </a-button>
                  </a-popconfirm>
                  <span v-if="record.status !== 1 && record.status !== 2">-</span>
                </a-space>
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

      <!-- ═══ 退回公海池 ═══ -->
      <a-modal
        v-model:open="returnVisible"
        title="退回公海池"
        :confirm-loading="returnSaving"
        ok-text="确定退回"
        cancel-text="取消"
        :width="480"
        @ok="handleReturnConfirm"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="客户">
            <span>{{ returnTarget?.customerName || '-' }}</span>
          </a-form-item>
          <a-form-item label="退回原因">
            <a-textarea
              v-model:value="returnRemark"
              placeholder="请输入退回原因"
              :rows="3"
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
import { message, Modal } from 'ant-design-vue'
import {
  ReloadOutlined,
  DownloadOutlined,
  SettingOutlined,
  SyncOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons-vue'
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
import { customerPoolApi } from '@/api/crm'

defineOptions({ name: 'CrmCustomerPool' })

// ═══ 字典（与后端 PoolReason / PoolStatus 枚举逐字一致） ═══
const REASON_OPTIONS = [
  { label: '长期未跟进', value: 1 },
  { label: '销售人员离职', value: 2 },
  { label: '客户要求', value: 3 },
  { label: '手动放入', value: 4 },
  { label: '系统自动', value: 5 }
]
const REASON_TEXT: Record<number, string> = {
  1: '长期未跟进', 2: '销售人员离职', 3: '客户要求', 4: '手动放入', 5: '系统自动'
}
const REASON_COLOR: Record<number, string> = {
  1: 'orange', 2: 'red', 3: 'blue', 4: 'default', 5: 'purple'
}
const STATUS_OPTIONS = [
  { label: '可领取', value: 1 },
  { label: '已领取', value: 2 },
  { label: '已过期', value: 3 },
  { label: '已退回', value: 4 }
]
const STATUS_TEXT: Record<number, string> = { 1: '可领取', 2: '已领取', 3: '已过期', 4: '已退回' }
const STATUS_COLOR: Record<number, string> = { 1: 'green', 2: 'blue', 3: 'red', 4: 'default' }

function reasonText(v?: number): string {
  return (v && REASON_TEXT[v]) || '-'
}
function reasonColor(v?: number): string {
  return (v && REASON_COLOR[v]) || 'default'
}
function statusText(v?: number): string {
  return (v && STATUS_TEXT[v]) || '-'
}
function statusColor(v?: number): string {
  return (v && STATUS_COLOR[v]) || 'default'
}
function fmtTime(v?: string): string {
  return v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-'
}
/** 过期判定：已过 expireTime 且仍处于「可领取」 */
function isOverdue(record: any): boolean {
  if (!record?.expireTime || record.status !== 1) return false
  return dayjs(record.expireTime).isBefore(dayjs())
}

// ═══ 左树：池状态 ═══
const selectedStatusKey = ref<string | number>('all')
const statusTree = computed(() => [
  { key: 'all', title: '全部' },
  ...STATUS_OPTIONS.map(o => ({ key: String(o.value), title: o.label }))
])

function handleStatusSelect(keys: (string | number)[]) {
  const key = keys && keys.length ? keys[0] : 'all'
  selectedStatusKey.value = key
  searchForm.status = key === 'all' ? undefined : Number(key)
  pagination.current = 1
  fetchList()
}

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '关键词', visible: true },
  { key: 'poolReason', label: '放入原因', visible: true },
  { key: 'status', label: '状态', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'autoRecovery', label: '执行自动回收', enabled: true },
  { key: 'checkExpired', label: '检查过期', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]
const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))
const pageConfigStorageKey = 'crm-customer-pool-page-config'

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
  poolReason: undefined as number | undefined,
  status: undefined as number | undefined,
})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const loading = ref(false)
const tableData = ref<any[]>([])

const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 120, fixed: 'left' },
  { key: 'customerCode', title: '客户编码', type: 'input', width: 140 },
  { key: 'customerName', title: '客户名称', type: 'slot', slotName: 'customerNameCell', width: 200 },
  { key: 'poolReason', title: '放入原因', type: 'slot', slotName: 'poolReasonCell', width: 120 },
  { key: 'originalSalesPersonName', title: '原负责人', type: 'input', width: 110 },
  { key: 'originalDepartmentName', title: '原部门', type: 'input', width: 130, defaultHidden: true },
  { key: 'poolTime', title: '放入时间', type: 'input', width: 160 },
  { key: 'poolDays', title: '已放天数', type: 'input', width: 100 },
  { key: 'expireTime', title: '过期时间', type: 'slot', slotName: 'expireTimeCell', width: 160 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'claimSalesPersonName', title: '领取人', type: 'input', width: 110 },
  { key: 'claimTime', title: '领取时间', type: 'input', width: 160, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 180, defaultHidden: true },
]

async function fetchList() {
  loading.value = true
  try {
    const res: any = await customerPoolApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
      status: searchForm.status,
    })
    let records = res?.records || []
    // 放入原因是前端本地过滤（后端 /page 只收 keyword/poolType/status）
    if (searchForm.poolReason) {
      records = records.filter((r: any) => r.poolReason === searchForm.poolReason)
    }
    tableData.value = records
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[客户公海] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  try {
    const stats = await customerPoolApi.statistics()
    availableCount.value = Number(stats?.availableCount) || 0
    myClaimCount.value = Number(stats?.myClaimCount) || 0
  } catch (e) {
    console.warn('[客户公海] 统计获取失败', e)
  }
}

const availableCount = ref(0)
const myClaimCount = ref(0)
const statCards = computed<StatCardItem[]>(() => [
  { label: '可领取客户', value: availableCount.value, suffix: '家' },
  { label: '我领取的客户', value: myClaimCount.value, suffix: '家' },
  { label: '当前页记录', value: tableData.value.filter((r: any) => !r.__ghost).length, suffix: '条' },
])

function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.poolReason = undefined
  searchForm.status = undefined
  selectedStatusKey.value = 'all'
  pagination.current = 1
  fetchList()
}

function handleRefresh() {
  fetchList()
  loadStatistics()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 领取 / 退回 ═══
const actingId = ref<number | null>(null)

async function handleClaim(record: any) {
  actingId.value = record.id
  try {
    await customerPoolApi.claim(record.id)
    message.success(`已领取客户「${record.customerName || ''}」`)
    handleRefresh()
  } catch (e: any) {
    message.error(e?.message || '领取失败')
  } finally {
    actingId.value = null
  }
}

const returnVisible = ref(false)
const returnSaving = ref(false)
const returnTarget = ref<any>(null)
const returnRemark = ref('')

function handleReturn(record: any) {
  returnTarget.value = { ...record }
  returnRemark.value = ''
  returnVisible.value = true
}

async function handleReturnConfirm() {
  if (!returnTarget.value) return
  returnSaving.value = true
  try {
    await customerPoolApi.returnToPool(returnTarget.value.id, returnRemark.value || undefined)
    message.success('已退回公海池')
    returnVisible.value = false
    handleRefresh()
  } catch (e: any) {
    message.error(e?.message || '退回失败')
  } finally {
    returnSaving.value = false
  }
}

// ═══ 回收 / 过期检查 ═══
const recovering = ref(false)
const checking = ref(false)

function handleAutoRecovery() {
  Modal.confirm({
    title: '执行自动回收',
    // 口径与后端 CustomerPoolServiceImpl#autoRecovery 对齐：只回收「有归属 + 有跟进记录但超期」的客户；
    // 从未跟进的客户不回收（无基准可算，避免把当天新建的客户立刻回收）
    content: '将把「已分配给业务员、且最近一次跟进已超过 30 天」的客户放入公海池。'
      + '从未有跟进记录的客户不在本次回收范围内。确定执行？',
    okText: '执行',
    cancelText: '取消',
    onOk: async () => {
      recovering.value = true
      try {
        await customerPoolApi.autoRecovery(30)
        message.success('自动回收已执行')
        handleRefresh()
      } catch (e: any) {
        message.error(e?.message || '自动回收失败')
      } finally {
        recovering.value = false
      }
    }
  })
}

async function handleCheckExpired() {
  checking.value = true
  try {
    await customerPoolApi.checkExpired()
    message.success('过期检查已执行')
    handleRefresh()
  } catch (e: any) {
    message.error(e?.message || '过期检查失败')
  } finally {
    checking.value = false
  }
}

// ═══ 导出 ═══
function handleExport() {
  const rows = (tableData.value || []).filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const headers = ['客户编码', '客户名称', '放入原因', '原负责人', '放入时间', '已放天数', '过期时间', '状态', '领取人', '备注']
  const lines = rows.map((r: any) => [
    r.customerCode, r.customerName, reasonText(r.poolReason), r.originalSalesPersonName,
    r.poolTime, r.poolDays, r.expireTime, statusText(r.status), r.claimSalesPersonName, r.remark
  ])
  const escape = (v: any) => `"${String(v ?? '').replace(/"/g, '""')}"`
  const csv = '\uFEFF' + [headers, ...lines].map(row => row.map(escape).join(',')).join('\r\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `客户公海_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

function handleError(error: Error) {
  console.error('[客户公海] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  fetchList()
  loadStatistics()
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
.text-overdue { color: #ff4d4f; font-weight: 500; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
