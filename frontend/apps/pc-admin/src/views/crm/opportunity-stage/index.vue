<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        商机阶段（CRM → 商机管理 → 商机阶段，菜单 70321，单入口）
        · CRM 为本系统独有模块（ql361 无 CRM 域），按业界生产级 CRM 建模
        · 规格：docs/Yh-Spec/手动整理对标开发文档/crm模块/商机阶段开发文档.md
        · 路线 A：ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillDetailTable
          本页是「阶段推进工作台」（推进/赢单/输单），不是纯报表，故不走 ARReportPage（路线 B）
        · 阶段值域唯一权威：数字 1-5（与后端 opportunity_stage 一致）；
          终态「赢单/输单」由 status（1/2/3）表达，不进阶段枚举
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏右侧：刷新 / 导出 / 页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="fetchList"
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

        <!-- ═══ 查询区（横向网格；显隐受页面配置控制） ═══ -->
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
                  placeholder="商机名称/客户"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('opportunityStage')"
                class="search-item"
              >
                <span class="search-label">阶段</span>
                <a-select
                  v-model:value="searchForm.opportunityStage"
                  placeholder="全部阶段"
                  size="small"
                  allow-clear
                  :options="STAGE_OPTIONS"
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
              <div
                v-if="isFieldVisible('customerId')"
                class="search-item"
              >
                <span class="search-label">客户</span>
                <a-select
                  v-model:value="searchForm.customerId"
                  placeholder="全部客户"
                  size="small"
                  show-search
                  allow-clear
                  :filter-option="filterOption"
                  :options="customerOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('salesPersonId')"
                class="search-item"
              >
                <span class="search-label">负责人</span>
                <a-select
                  v-model:value="searchForm.salesPersonId"
                  placeholder="全部负责人"
                  size="small"
                  show-search
                  allow-clear
                  :filter-option="filterOption"
                  :options="userOptions"
                  @change="handleSearch"
                />
              </div>
              <div class="search-item search-actions">
                <a-button
                  type="primary"
                  size="small"
                  :loading="loading"
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

        <!-- ═══ 数据表 ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="crm-opportunity-stage-table-columns"
              global-config-key="crm-opportunity-stage-table-columns"
            >
              <template #nameCell="{ record }">
                <a-button
                  v-if="!record.__ghost"
                  type="link"
                  size="small"
                  class="name-link"
                  @click="goDetail(record)"
                >
                  {{ record.opportunityName || '-' }}
                </a-button>
              </template>

              <template #stageCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="stageColor(record.opportunityStage)"
                >
                  {{ stageText(record) }}
                </a-tag>
              </template>

              <template #probabilityCell="{ record }">
                <a-progress
                  v-if="!record.__ghost"
                  :percent="Number(record.probability) || 0"
                  size="small"
                  :stroke-color="Number(record.probability) >= 70 ? '#52c41a' : '#1890ff'"
                />
              </template>

              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="statusColor(record.status)"
                >
                  {{ statusText(record) }}
                </a-tag>
              </template>

              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <!-- 阶段为空时后端 int 拆箱会 500，故一并禁用（不能只判 >= 5） -->
                  <a-button
                    type="link"
                    size="small"
                    :disabled="!isActive(record) || !record.opportunityStage || Number(record.opportunityStage) >= 5"
                    @click="handleAdvance(record)"
                  >
                    推进
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    :disabled="!isActive(record)"
                    @click="openWinModal(record)"
                  >
                    赢单
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    :disabled="!isActive(record)"
                    @click="openLoseModal(record)"
                  >
                    输单
                  </a-button>
                </a-space>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <!-- ═══ 经典分页栏 ═══ -->
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

      <!-- ═══ 页面配置（查询条件显隐 + 功能按钮启用） ═══ -->
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

      <!-- ═══ 赢单弹窗 ═══ -->
      <a-modal
        v-model:open="winModalVisible"
        title="商机赢单"
        :confirm-loading="actionSaving"
        ok-text="确定赢单"
        cancel-text="取消"
        @ok="handleWin"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="商机">
            <span>{{ currentOpp?.opportunityName }}</span>
          </a-form-item>
          <a-form-item
            label="成交金额"
            required
          >
            <a-input-number
              v-model:value="winAmount"
              :min="0"
              :precision="2"
              style="width: 100%"
              placeholder="请输入实际成交金额"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 输单弹窗 ═══ -->
      <a-modal
        v-model:open="loseModalVisible"
        title="商机输单"
        :confirm-loading="actionSaving"
        ok-text="确定输单"
        ok-type="danger"
        cancel-text="取消"
        @ok="handleLose"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="商机">
            <span>{{ currentOpp?.opportunityName }}</span>
          </a-form-item>
          <a-form-item
            label="输单原因"
            required
          >
            <a-textarea
              v-model:value="loseReason"
              :rows="3"
              placeholder="请输入输单原因"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { QueryFieldSetting, FunctionButtonSetting } from '@/components/PageConfigPanel/index.vue'
import { opportunityApi, opportunityStageApi, crmCustomerApi, type OpportunityRecord } from '@/api/crm'
import { userApi } from '@/api/user'
import { exportCsv } from '@/utils/exportCsv'
import {
  ReloadOutlined,
  DownloadOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'

defineOptions({ name: 'CrmOpportunityStage' })

const router = useRouter()

// ═══ 商机阶段 / 状态（与后端 CustomerOpportunityServiceImpl 一致） ═══
const STAGE_TEXT: Record<number, string> = { 1: '初步接触', 2: '需求确认', 3: '方案报价', 4: '商务谈判', 5: '成交' }
const STAGE_COLOR: Record<number, string> = { 1: 'default', 2: 'blue', 3: 'cyan', 4: 'purple', 5: 'green' }
const STAGE_OPTIONS = [1, 2, 3, 4, 5].map(v => ({ label: STAGE_TEXT[v], value: v }))
const STATUS_TEXT: Record<number, string> = { 1: '跟进中', 2: '赢单', 3: '输单' }
const STATUS_COLOR: Record<number, string> = { 1: 'orange', 2: 'green', 3: 'red' }
const STATUS_OPTIONS = [1, 2, 3].map(v => ({ label: STATUS_TEXT[v], value: v }))

function stageText(record: any): string {
  return record?.opportunityStageDesc || STAGE_TEXT[record?.opportunityStage] || '未设置'
}
function stageColor(v: number | undefined): string {
  return (v && STAGE_COLOR[v]) || 'default'
}
function statusText(record: any): string {
  return record?.statusDesc || STATUS_TEXT[record?.status] || '跟进中'
}
function statusColor(v: number | undefined): string {
  return STATUS_COLOR[Number(v) || 1] || STATUS_COLOR[1]
}
function isActive(record: any): boolean {
  const status = Number(record?.status)
  return status !== 2 && status !== 3
}
function formatAmount(raw: any): string {
  if (raw === null || raw === undefined || raw === '') return '-'
  const n = Number(raw)
  if (Number.isNaN(n)) return '-'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function filterOption(input: string, option: any): boolean {
  return String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())
}

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '关键字（商机名称/客户）', visible: true },
  { key: 'opportunityStage', label: '阶段', visible: true },
  { key: 'status', label: '状态', visible: true },
  { key: 'customerId', label: '客户', visible: true },
  { key: 'salesPersonId', label: '负责人', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]
const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))
const pageConfigStorageKey = 'crm-opportunity-stage-page-config'

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

// ═══ 表格列（实体字段名；rowNo 承载列配置齿轮，action 为固定列） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 150, fixed: 'left' },
  { key: 'opportunityCode', title: '商机编号', type: 'input', width: 160 },
  { key: 'opportunityName', title: '商机名称', type: 'slot', slotName: 'nameCell', width: 180 },
  { key: 'customerName', title: '客户', type: 'input', width: 160 },
  { key: 'opportunityStage', title: '阶段', type: 'slot', slotName: 'stageCell', width: 100, align: 'center' },
  { key: 'probability', title: '赢单概率', type: 'slot', slotName: 'probabilityCell', width: 130 },
  { key: 'estimatedAmount', title: '预计金额', type: 'input', width: 120, align: 'right', formatter: formatAmount },
  { key: 'actualAmount', title: '成交金额', type: 'input', width: 120, align: 'right', formatter: formatAmount },
  { key: 'expectedCloseDate', title: '预计成交日', type: 'input', width: 120 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 90, align: 'center' },
  { key: 'salesPersonName', title: '负责人', type: 'input', width: 100 },
  // 以下为默认隐藏列（表头齿轮可开启）
  { key: 'opportunityStageDesc', title: '阶段说明', type: 'input', width: 100, defaultHidden: true },
  { key: 'statusDesc', title: '状态说明', type: 'input', width: 100, defaultHidden: true },
  { key: 'departmentName', title: '部门', type: 'input', width: 120, defaultHidden: true },
  { key: 'actualCloseDate', title: '实际关闭日期', type: 'input', width: 120, defaultHidden: true },
  { key: 'winReason', title: '赢单原因', type: 'input', width: 160, defaultHidden: true },
  { key: 'loseReason', title: '输单原因', type: 'input', width: 160, defaultHidden: true },
  { key: 'requirement', title: '需求描述', type: 'input', width: 200, defaultHidden: true },
  { key: 'competitor', title: '竞争对手', type: 'input', width: 140, defaultHidden: true },
  { key: 'createdAt', title: '创建时间', type: 'input', width: 160, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 160, defaultHidden: true },
]

// ═══ 查询 / 分页 ═══
const searchForm = reactive({
  keyword: '',
  opportunityStage: undefined as number | undefined,
  status: undefined as number | undefined,
  customerId: undefined as number | undefined,
  salesPersonId: undefined as number | undefined,
})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const loading = ref(false)
const tableData = ref<any[]>([])
const customerOptions = ref<{ label: string; value: number }[]>([])
const userOptions = ref<{ label: string; value: number }[]>([])

function buildQuery() {
  return {
    keyword: searchForm.keyword || undefined,
    opportunityStage: searchForm.opportunityStage,
    status: searchForm.status,
    customerId: searchForm.customerId,
    salesPersonId: searchForm.salesPersonId,
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await opportunityApi.page({
      ...buildQuery(),
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (e: any) {
    console.error('[商机阶段] 加载列表失败', e)
    message.error(e?.message || '获取商机列表失败')
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
  searchForm.opportunityStage = undefined
  searchForm.status = undefined
  searchForm.customerId = undefined
  searchForm.salesPersonId = undefined
  pagination.current = 1
  fetchList()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

/** 点击商机名称直达商机编辑页（工作台直达的刚需入口） */
function goDetail(record: any) {
  router.push({ path: '/crm/opportunity/form', query: { id: String(record.id) } })
}

// ═══ 阶段动作 ═══
const actionSaving = ref(false)
const currentOpp = ref<OpportunityRecord | null>(null)

function handleAdvance(record: any) {
  const nextStage = (Number(record.opportunityStage) || 0) + 1
  Modal.confirm({
    title: '推进商机阶段',
    content: `确定将商机「${record.opportunityName}」推进到下一阶段（${STAGE_TEXT[nextStage] || '—'}）吗？`,
    okText: '确定',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      try {
        await opportunityStageApi.advance(record.id)
        message.success('商机阶段已推进')
        fetchList()
      } catch (e: any) {
        message.error(e?.message || '推进失败')
      }
    },
  })
}

// ── 赢单 ──
const winModalVisible = ref(false)
const winAmount = ref<number>()

function openWinModal(record: any) {
  currentOpp.value = record
  winAmount.value = record.estimatedAmount ? Number(record.estimatedAmount) : undefined
  winModalVisible.value = true
}

async function handleWin() {
  if (!currentOpp.value) return
  if (winAmount.value === undefined || winAmount.value === null) {
    message.warning('请输入成交金额')
    return
  }
  // 0 元赢单无业务意义，此前 min=0 允许提交，属校验缺口
  if (Number(winAmount.value) <= 0) {
    message.warning('成交金额须大于 0')
    return
  }
  actionSaving.value = true
  try {
    await opportunityStageApi.win(currentOpp.value.id, winAmount.value)
    message.success('已标记为赢单')
    winModalVisible.value = false
    fetchList()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  } finally {
    actionSaving.value = false
  }
}

// ── 输单 ──
const loseModalVisible = ref(false)
const loseReason = ref('')

function openLoseModal(record: any) {
  currentOpp.value = record
  loseReason.value = ''
  loseModalVisible.value = true
}

async function handleLose() {
  if (!currentOpp.value) return
  if (!loseReason.value.trim()) {
    message.warning('请输入输单原因')
    return
  }
  actionSaving.value = true
  try {
    await opportunityStageApi.lose(currentOpp.value.id, loseReason.value.trim())
    message.success('已标记为输单')
    loseModalVisible.value = false
    fetchList()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  } finally {
    actionSaving.value = false
  }
}

// ═══ 导出（全量 + 中文映射，替代原先「仅当前页 + 导出数字」的口径） ═══
async function handleExport() {
  try {
    const list: any = await opportunityStageApi.exportList(buildQuery())
    const rows = Array.isArray(list) ? list : []
    if (!rows.length) {
      message.warning('没有可导出的数据')
      return
    }
    const headers = ['商机编号', '商机名称', '客户', '阶段', '赢单概率', '预计金额', '成交金额',
      '预计成交日', '状态', '负责人', '实际关闭日期', '输单原因']
    const body = rows.map((r: any) => [
      r.opportunityCode || '',
      r.opportunityName || '',
      r.customerName || '',
      stageText(r),
      `${Number(r.probability) || 0}%`,
      formatAmount(r.estimatedAmount),
      formatAmount(r.actualAmount),
      r.expectedCloseDate || '',
      statusText(r),
      r.salesPersonName || '',
      r.actualCloseDate || '',
      r.loseReason || '',
    ])
    exportCsv(headers, body, `商机阶段_${dayjs().format('YYYYMMDD_HHmmss')}`)
    message.success('导出成功')
  } catch (e: any) {
    message.error(e?.message || '导出失败')
  }
}

function handleError(error: Error) {
  console.error('[商机阶段] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

async function loadOptions() {
  try {
    const list = await crmCustomerApi.dropdown()
    customerOptions.value = (Array.isArray(list) ? list : []).map((c: any) => ({ label: c.name, value: c.id }))
  } catch (e) {
    console.warn('[商机阶段] 客户下拉获取失败', e)
  }
  try {
    const res: any = await userApi.getPage({ pageNum: 1, pageSize: 200, status: 1 })
    userOptions.value = (res?.records || []).map((u: any) => ({ label: u.nickname || u.username, value: u.id }))
  } catch (e) {
    console.warn('[商机阶段] 负责人下拉获取失败', e)
  }
}

onMounted(async () => {
  await loadOptions()
  fetchList()
})
</script>

<style scoped>
/* 查询区（插槽内容样式必须自备：scoped 不作用于布局组件内的插槽内容） */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-item :deep(.ant-select) { min-width: 150px; }
.search-item :deep(.ant-input-affix-wrapper) { width: 180px; }
.search-actions { margin-left: auto; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }

/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

.name-link { padding: 0; height: auto; }

/* ── 紧凑尺寸覆盖：28px 输入框 ── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
:deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
:deep(.ant-input-number-sm input) { height: 26px; }
</style>
