<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      title="合同审批"
      full-height
    >
      <!--
        合同审批工作台（CRM → 合同管理 → 合同审批，菜单 70341，单入口）
        · CRM 为本系统独有模块（ql361 无 CRM 域），按业界生产级 CRM 的审批工作台建模
        · 规格：docs/Yh-Spec/手动整理对标开发文档/crm模块/合同审批开发文档.md
        · 路线 A：ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillDetailTable
          （原为 ARReportPage，属路线 B，交易/CRM 模块已明令不再新增）
        · 本页职责单一：草稿 → 提交审批（submit）；待审批 → 通过（approve）/ 驳回（reject）
        · 权限：后端 submit/approve/reject 本会话已加校验（crm:contract:edit / crm:contract:approve），
          前端对应补 v-permission，两端口径一致（权限码已由 V11.379.0 补齐种子）
        · 本轮改造：
          ① 外壳由 ARReportPage（路线 B）重写为路线 A：CategoryListLayout + BillDetailTable
             + 表头齿轮列配置（个人/全局）+ StandardPagination 经典分页 + PageConfigPanel
          ② 列定义按后端 VO 字段口径重排；插槽列补 type:'slot'，操作列补 slotName:'actionCell'
          ③ 查询条件扩为 keyword + 状态（11 态）+ 合同类型（后端 7 值整数，原页无法按类型筛）
          ④ 动作按钮补权限码；错误提示保留后端原因
          ⑤ 统计卡维持原「全量聚合」口径（走 /export），但简化为一次请求、动作后统一刷新
          ⑥ 移除「已审批之后无动作」的静态占位说明改为显式 —，不伪造审批层级（后端为单级审批）
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏：刷新 / 导出 / 页面配置 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="handleRefresh"
            >
              <ReloadOutlined /> 刷新
            </a-button>
          </a-space>
        </template>
        <template #toolbar-right>
          <a-space :size="8">
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
                  placeholder="合同编号/名称/客户"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
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
                  :options="CONTRACT_STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('contractType')"
                class="search-item"
              >
                <span class="search-label">合同类型</span>
                <a-select
                  v-model:value="searchForm.contractType"
                  placeholder="全部类型"
                  size="small"
                  allow-clear
                  :options="CONTRACT_TYPE_OPTIONS"
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

        <!-- ═══ 数据表（统计卡置于表格面板内，flex 纵向堆叠） ═══ -->
        <template #table>
          <div class="table-area">
            <div class="stat-strip">
              <div class="stat-card">
                <div class="stat-title">
                  待审批
                </div>
                <div class="stat-value">
                  {{ approvalStats.pending }}
                </div>
                <div class="stat-desc">
                  全量待审批份数
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-title">
                  草稿
                </div>
                <div class="stat-value">
                  {{ approvalStats.draft }}
                </div>
                <div class="stat-desc">
                  可提交审批
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-title">
                  合同总数
                </div>
                <div class="stat-value">
                  {{ approvalStats.total }}
                </div>
                <div class="stat-desc">
                  全量合同份数
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-title">
                  生效/执行金额
                </div>
                <div class="stat-value">
                  ¥{{ formatAmount(approvalStats.effectiveAmount) }}
                </div>
                <div class="stat-desc">
                  生效中 + 执行中
                </div>
              </div>
            </div>

            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="crm-contract-approval-table-columns"
              global-config-key="crm-contract-approval-table-columns"
            >
              <template #contractTypeCell="{ record }">
                <span v-if="record.__ghost" />
                <span v-else>{{ contractTypeLabel(record) }}</span>
              </template>

              <template #amountCell="{ record }">
                <span v-if="record.__ghost" />
                <span
                  v-else
                  class="amount-cell"
                >¥{{ formatAmount(record.contractAmount) }}</span>
              </template>

              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="statusColor(record.status)"
                >
                  {{ record.statusDesc || statusText(record.status) }}
                </a-tag>
              </template>

              <!-- 操作列：仅服务「草稿 → 待审批」与「待审批 → 已审批 / 草稿」两段（后端单级审批） -->
              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <template v-if="Number(record.status) === 1">
                    <a-button
                      v-permission="'crm:contract:approve'"
                      type="link"
                      size="small"
                      @click="openApproveModal(record)"
                    >
                      通过
                    </a-button>
                    <a-button
                      v-permission="'crm:contract:approve'"
                      type="link"
                      size="small"
                      danger
                      @click="openRejectModal(record)"
                    >
                      驳回
                    </a-button>
                  </template>
                  <a-button
                    v-else-if="Number(record.status) === 0"
                    v-permission="'crm:contract:edit'"
                    type="link"
                    size="small"
                    @click="handleSubmit(record)"
                  >
                    提交审批
                  </a-button>
                  <span
                    v-else
                    class="no-action"
                  >—</span>
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

      <!-- ═══ 审批通过弹窗 ═══ -->
      <a-modal
        v-model:open="approveModalVisible"
        title="审批通过"
        :confirm-loading="actionSaving"
        ok-text="确定通过"
        cancel-text="取消"
        @ok="handleApprove"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="合同">
            <span>{{ currentContract?.contractName }}（{{ currentContract?.contractNo }}）</span>
          </a-form-item>
          <a-form-item label="合同金额">
            <span>¥ {{ formatAmount(currentContract?.contractAmount) }}</span>
          </a-form-item>
          <a-form-item label="客户">
            <span>{{ currentContract?.customerName || '无' }}</span>
          </a-form-item>
          <a-form-item label="执行进度">
            <span>{{ currentContract?.executionProgress ?? 0 }}%</span>
          </a-form-item>
          <a-form-item label="审批意见">
            <a-textarea
              v-model:value="approveNote"
              :rows="3"
              placeholder="审批意见（可选）"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 审批驳回弹窗（reason 是后端必填入参） ═══ -->
      <a-modal
        v-model:open="rejectModalVisible"
        title="审批驳回"
        :confirm-loading="actionSaving"
        ok-text="确定驳回"
        ok-type="danger"
        cancel-text="取消"
        @ok="handleReject"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="合同">
            <span>{{ currentContract?.contractName }}（{{ currentContract?.contractNo }}）</span>
          </a-form-item>
          <a-form-item
            label="驳回原因"
            required
          >
            <a-textarea
              v-model:value="rejectReason"
              :rows="3"
              placeholder="请输入驳回原因"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ReloadOutlined, DownloadOutlined, SettingOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { QueryFieldSetting, FunctionButtonSetting } from '@/components/PageConfigPanel/index.vue'
import { contractApi, contractApprovalApi, type ContractItem } from '@/api/crm'
import { exportCsv } from '@/utils/exportCsv'

defineOptions({ name: 'CrmContractApproval' })

// ═══ 字典（唯一真源 = 后端枚举；本页状态 11/11 完整） ═══
const CONTRACT_STATUS_OPTIONS = [
  { label: '草稿', value: 0 },
  { label: '待审批', value: 1 },
  { label: '已审批', value: 2 },
  { label: '待签署', value: 3 },
  { label: '已签署', value: 4 },
  { label: '生效中', value: 5 },
  { label: '执行中', value: 6 },
  { label: '已完成', value: 7 },
  { label: '已终止', value: 8 },
  { label: '已过期', value: 9 },
  { label: '已取消', value: 10 },
]
const STATUS_TEXT: Record<number, string> = {
  0: '草稿', 1: '待审批', 2: '已审批', 3: '待签署', 4: '已签署',
  5: '生效中', 6: '执行中', 7: '已完成', 8: '已终止', 9: '已过期', 10: '已取消',
}
// 颜色与合同列表页收敛为同一套（原本页 3 cyan / 9 default 与列表页不一致）
const STATUS_COLOR: Record<number, string> = {
  0: 'default', 1: 'orange', 2: 'blue', 3: 'geekblue', 4: 'purple',
  5: 'green', 6: 'green', 7: 'green', 8: 'red', 9: 'red', 10: 'red',
}
const CONTRACT_TYPE_OPTIONS = [
  { label: '销售合同', value: 1 },
  { label: '采购合同', value: 2 },
  { label: '服务合同', value: 3 },
  { label: '项目合同', value: 4 },
  { label: '框架合同', value: 5 },
  { label: '合作伙伴合同', value: 6 },
  { label: '其他合同', value: 7 },
]
const CONTRACT_TYPE_TEXT: Record<number, string> = {
  1: '销售合同', 2: '采购合同', 3: '服务合同', 4: '项目合同',
  5: '框架合同', 6: '合作伙伴合同', 7: '其他合同',
}

function statusText(v: any): string { return STATUS_TEXT[Number(v)] || '未知' }
function statusColor(v: any): string { return STATUS_COLOR[Number(v)] || 'default' }
function contractTypeLabel(record: any): string {
  if (record?.contractTypeDesc) return record.contractTypeDesc
  return CONTRACT_TYPE_TEXT[Number(record?.contractType)] || '-'
}
function formatAmount(val: any): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function handleError(error: Error) {
  console.error('[合同审批] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '关键字', visible: true },
  { key: 'status', label: '状态', visible: true },
  { key: 'contractType', label: '合同类型', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]
const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))
const pageConfigStorageKey = 'crm-contract-approval-page-config'

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

// ═══ 查询条件（keyword / status / contractType 均为后端 page 端点真实支持的参数） ═══
const searchForm = reactive({
  keyword: '',
  status: undefined as number | undefined,
  contractType: undefined as number | undefined,
})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const loading = ref(false)
const tableData = ref<any[]>([])

// ═══ 表格列 ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 140, fixed: 'left' },
  { key: 'contractNo', title: '合同编号', type: 'input', width: 160 },
  { key: 'contractName', title: '合同名称', type: 'input', width: 180 },
  { key: 'customerName', title: '客户', type: 'input', width: 160 },
  { key: 'contractType', title: '合同类型', type: 'slot', slotName: 'contractTypeCell', width: 110 },
  { key: 'contractAmount', title: '合同金额', type: 'slot', slotName: 'amountCell', width: 130, align: 'right' },
  { key: 'signDate', title: '签署日期', type: 'input', width: 110 },
  { key: 'startDate', title: '开始日期', type: 'input', width: 110 },
  { key: 'endDate', title: '结束日期', type: 'input', width: 110 },
  { key: 'salesPersonName', title: '负责人', type: 'input', width: 100 },
  { key: 'executionProgress', title: '执行进度', type: 'input', width: 100, defaultHidden: true },
  { key: 'approvedTime', title: '审批时间', type: 'input', width: 160, defaultHidden: true },
  { key: 'approvedNote', title: '审批意见', type: 'input', width: 160, defaultHidden: true },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 100, align: 'center' },
  { key: 'createTime', title: '创建时间', type: 'input', width: 160, defaultHidden: true },
]

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
      status: searchForm.status,
      contractType: searchForm.contractType,
    }
    const result: any = await contractApi.page(params)
    tableData.value = result?.records || result?.data?.records || []
    pagination.total = Number(result?.total ?? result?.data?.total ?? 0)
  } catch (err: any) {
    console.warn('[合同审批] 获取合同列表失败', err)
    message.error(err?.response?.data?.message || err?.message || '获取合同列表失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}
function handleReset() {
  searchForm.keyword = ''
  searchForm.status = undefined
  searchForm.contractType = undefined
  pagination.current = 1
  fetchData()
}
function handleRefresh() {
  fetchData()
  loadApprovalStats()
}
function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

// ═══ 统计卡（全量口径，走 /export 一次拉取；本页职责是工作台，故保持全量） ═══
const allContracts = ref<ContractItem[]>([])
const approvalStats = computed(() => {
  const list = allContracts.value
  return {
    pending: list.filter(c => Number(c.status) === 1).length,
    draft: list.filter(c => Number(c.status) === 0).length,
    total: list.length,
    effectiveAmount: list
      .filter(c => Number(c.status) === 5 || Number(c.status) === 6)
      .reduce((acc, c) => acc + (Number(c.contractAmount) || 0), 0),
  }
})

async function loadApprovalStats() {
  try {
    const list = await contractApprovalApi.exportList()
    allContracts.value = Array.isArray(list) ? list : []
  } catch (e) {
    console.warn('[合同审批] 审批统计获取失败', e)
  }
}

// ═══ 审批操作 ═══
const actionSaving = ref(false)
// 当前操作行：用 any 承载（ContractItem 只是查询接口的窄类型，本页还要读 executionProgress 等 VO 字段）
const currentContract = ref<any>(null)

// ── 提交审批（仅草稿；后端前置校验 status==0，权限码 crm:contract:edit） ──
function handleSubmit(record: any) {
  Modal.confirm({
    title: '提交审批',
    content: `确定将合同「${record.contractName}」提交审批吗？`,
    okText: '确定',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      try {
        await contractApprovalApi.submit(record.id)
        message.success('已提交审批')
        afterAction()
      } catch (e: any) {
        message.error(e?.response?.data?.message || e?.message || '提交失败')
      }
    },
  })
}

// ── 通过（后端前置校验 status==1） ──
const approveModalVisible = ref(false)
const approveNote = ref('')

function openApproveModal(record: any) {
  currentContract.value = record
  approveNote.value = ''
  approveModalVisible.value = true
}

async function handleApprove() {
  if (!currentContract.value) return
  actionSaving.value = true
  try {
    await contractApprovalApi.approve(currentContract.value.id, approveNote.value.trim() || undefined)
    message.success('审批已通过')
    approveModalVisible.value = false
    afterAction()
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '审批失败')
  } finally {
    actionSaving.value = false
  }
}

// ── 驳回（reason 必填：前端先校验，后端 @RequestParam 也必填） ──
const rejectModalVisible = ref(false)
const rejectReason = ref('')

function openRejectModal(record: any) {
  currentContract.value = record
  rejectReason.value = ''
  rejectModalVisible.value = true
}

async function handleReject() {
  if (!currentContract.value) return
  if (!rejectReason.value.trim()) {
    message.warning('请输入驳回原因')
    return
  }
  actionSaving.value = true
  try {
    await contractApprovalApi.reject(currentContract.value.id, rejectReason.value.trim())
    message.success('已驳回')
    rejectModalVisible.value = false
    afterAction()
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '驳回失败')
  } finally {
    actionSaving.value = false
  }
}

/** 动作后刷新：列表 + 统计卡 */
function afterAction() {
  fetchData()
  loadApprovalStats()
}

// ═══ 导出（当前页） ═══
function handleExport() {
  const rows = tableData.value.filter(r => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const headers = ['合同编号', '合同名称', '客户', '合同类型', '合同金额', '签署日期',
    '开始日期', '结束日期', '负责人', '状态']
  const lines = rows.map((r: any) => [
    r.contractNo || '', r.contractName || '', r.customerName || '', contractTypeLabel(r),
    formatAmount(r.contractAmount), r.signDate || '', r.startDate || '', r.endDate || '',
    r.salesPersonName || '', r.statusDesc || statusText(r.status),
  ])
  exportCsv(headers, lines, '合同审批')
}

onMounted(() => {
  fetchData()
  loadApprovalStats()
})
</script>

<style scoped>
.search-area { padding: 0; }
.search-grid { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-item :deep(.ant-select) { min-width: 140px; }
.search-item :deep(.ant-input-affix-wrapper) { width: 200px; }
.search-actions { margin-left: auto; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }

/* 统计卡：置于表格面板内（CategoryListLayout 的 #table 插槽，flex 纵向堆叠） */
.stat-strip {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  padding: 10px 12px 8px;
}
.stat-card {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 12px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  background: #fafafa;
}
.stat-title { font-size: 13px; color: #666; }
.stat-value { font-size: 20px; font-weight: 600; color: #303133; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; }
.stat-desc { font-size: 12px; color: #999; }

/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

.amount-cell { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; color: #f5222d; font-weight: 500; }
.no-action { color: #bbb; }
</style>
