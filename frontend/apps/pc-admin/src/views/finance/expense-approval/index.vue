<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 快捷日期 ═══ -->
        <template #toolbar-left>
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
          </div>
          <a-space
            :size="4"
            class="quick-dates"
          >
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
        </template>

        <!-- ═══ 工具栏右侧：页面配置/审批人配置/刷新/导出（列配置用数据表表头齿轮） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <!-- 悬停提示位置/延迟由全局组件默认值统一控制（utils/antdDefaults.ts：下置 + 0.4s） -->
            <a-tooltip title="页面配置">
              <a-button
                size="small"
                @click="showPageConfig = true"
              >
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-tooltip title="审批人配置（自动指派）">
              <a-button
                size="small"
                @click="showApproverConfig = true"
              >
                <TeamOutlined />
              </a-button>
            </a-tooltip>
            <a-button
              v-if="btnEnabled('refresh')"
              size="small"
              @click="handleSearch"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="btnEnabled('export')"
              size="small"
              @click="handleExport"
            >
              <ExportOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 搜索区域 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-container">
              <div
                ref="searchGridRef"
                class="search-grid"
              >
                <div
                  v-if="fieldVisible('date')"
                  class="search-field-item"
                >
                  <a-range-picker
                    v-model:value="dateRange"
                    size="small"
                    style="width:100%"
                    @change="handleDateChange"
                  />
                </div>
                <div
                  v-if="fieldVisible('docNo')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="search.docNo"
                    placeholder="单据编号"
                    allow-clear
                    size="small"
                    @press-enter="handleSearch"
                  />
                </div>
                <div
                  v-if="fieldVisible('partnerName')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="search.partnerName"
                    placeholder="往来单位"
                    allow-clear
                    size="small"
                    @press-enter="handleSearch"
                  />
                </div>
                <div
                  v-if="fieldVisible('handlerName')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="search.handlerName"
                    placeholder="经手人"
                    allow-clear
                    size="small"
                    @press-enter="handleSearch"
                  />
                </div>
                <div
                  v-if="fieldVisible('deptName')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="search.deptName"
                    placeholder="部门"
                    allow-clear
                    size="small"
                    @press-enter="handleSearch"
                  />
                </div>
                <div
                  v-if="fieldVisible('expenseType')"
                  class="search-field-item"
                >
                  <div class="search-select-wrap">
                    <span class="search-select-label">费用类型</span>
                    <a-select
                      v-model:value="search.expenseType"
                      size="small"
                      allow-clear
                    >
                      <a-select-option value="">
                        全部
                      </a-select-option>
                      <a-select-option value="0">
                        往来单位费用
                      </a-select-option>
                      <a-select-option value="1">
                        内部费用
                      </a-select-option>
                    </a-select>
                  </div>
                </div>
                <div
                  v-if="fieldVisible('creatorName')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="search.creatorName"
                    placeholder="制单人"
                    allow-clear
                    size="small"
                    @press-enter="handleSearch"
                  />
                </div>
                <div
                  v-if="fieldVisible('approvalStatus')"
                  class="search-field-item"
                >
                  <div class="search-select-wrap">
                    <span class="search-select-label">审批状态</span>
                    <a-select
                      v-model:value="search.approvalStatus"
                      size="small"
                      allow-clear
                    >
                      <a-select-option value="1">
                        审批中
                      </a-select-option>
                      <a-select-option value="3">
                        审批驳回
                      </a-select-option>
                      <a-select-option value="2">
                        审批通过
                      </a-select-option>
                      <a-select-option value="0">
                        未提交
                      </a-select-option>
                    </a-select>
                  </div>
                </div>
                <div
                  v-if="fieldVisible('currentApproverName')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="search.currentApproverName"
                    placeholder="当前审批人"
                    allow-clear
                    size="small"
                    @press-enter="handleSearch"
                  />
                </div>
                <div
                  v-if="fieldVisible('summary')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="search.summary"
                    placeholder="摘要"
                    allow-clear
                    size="small"
                    @press-enter="handleSearch"
                  />
                </div>
                <div
                  ref="searchActionRef"
                  class="search-action-group"
                  :style="{ gridColumn: 'span ' + actionSpan }"
                >
                  <div class="search-field-item search-action-item">
                    <a-button
                      type="primary"
                      size="small"
                      @click="handleSearch"
                    >
                      查询
                    </a-button>
                  </div>
                  <div
                    v-if="fieldVisible('onlyMine')"
                    class="search-field-item"
                  >
                    <a-checkbox v-model:checked="search.onlyMine">
                      仅看我的待办
                    </a-checkbox>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 表格区域 ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="ALL_COLUMNS"
              :data-source="tableData"
              :storage-key="'expense-approval-table-columns'"
              :loading="loading"
              :pagination="billPagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="false"
              :summary-columns="tableFooterColumns"
              row-key="id"
              @page-change="handlePageChange"
            >
              <template #docNoCell="{ record }">
                <a-button
                  type="link"
                  size="small"
                  @click="openDetail(record)"
                >
                  {{ record.docNo }}
                </a-button>
              </template>
              <template #approvalStatusCell="{ record }">
                <a-tag :color="approvalStatusColor(record.approvalStatus)">
                  {{ approvalStatusText(record) }}
                </a-tag>
              </template>
              <template #approvalLevelCell="{ record }">
                <span v-if="Number(record.approvalStatus) === 1">
                  {{ levelName(record.approvalLevel) }}（{{ record.approvalLevel }}/{{ record.totalApprovalLevel }}）
                </span>
                <span v-else>-</span>
              </template>
              <template #statusCell="{ record }">
                <a-tag :color="docStatusColor(record.status)">
                  {{ docStatusText(record.status) }}
                </a-tag>
              </template>
              <template #expenseTypeCell="{ record }">
                {{ expenseTypeText(record.expenseType) }}
              </template>
              <template #totalAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.totalAmount) }}</span>
              </template>
              <template #payAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.payAmount) }}</span>
              </template>
              <template #submitTimeCell="{ record }">
                {{ formatDateTime(record.submitTime) }}
              </template>
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <template v-if="isPending(record)">
                    <a-button
                      type="link"
                      size="small"
                      @click="openApproveModal(record, 'APPROVE')"
                    >
                      通过
                    </a-button>
                    <a-button
                      type="link"
                      size="small"
                      danger
                      @click="openApproveModal(record, 'REJECT')"
                    >
                      驳回
                    </a-button>
                  </template>
                  <a-button
                    v-if="canSubmit(record)"
                    type="link"
                    size="small"
                    @click="openSubmitModal(record)"
                  >
                    提交审批
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="openDetail(record)"
                  >
                    详情
                  </a-button>
                </a-space>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldsConfig"
      :function-buttons-config="functionButtonsConfig"
      storage-key="expense-approval-page-config"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- ═══ 审批操作弹窗（通过/驳回） ═══ -->
    <a-modal
      v-model:open="approveModalOpen"
      :title="approveAction === 'APPROVE' ? '审批通过' : '审批驳回'"
      :confirm-loading="submitting"
      :ok-text="approveAction === 'APPROVE' ? '确认通过' : '确认驳回'"
      :ok-button-props="{ danger: approveAction === 'REJECT' }"
      @ok="handleApproveSubmit"
      @cancel="resetApproveModal"
    >
      <a-form layout="vertical">
        <a-form-item label="费用单">
          <span>{{ currentRecord?.docNo }} — {{ currentRecord?.partnerName || currentRecord?.deptName || '内部费用' }}（{{ formatAmount(currentRecord?.totalAmount) }}）</span>
        </a-form-item>
        <a-form-item label="审批级别">
          <span>{{ levelName(currentRecord?.approvalLevel) }}（{{ currentRecord?.approvalLevel || 0 }}/{{ currentRecord?.totalApprovalLevel || 0 }}）</span>
        </a-form-item>
        <a-form-item
          v-if="approveAction === 'APPROVE' && isNotLastLevel"
          label="下一级审批人"
          :required="!nextAutoAssigned"
        >
          <a-select
            v-model:value="nextApproverId"
            show-search
            allow-clear
            :options="approverOptions"
            :loading="loadingApprovers"
            placeholder="请选择下一级审批人"
            style="width:100%"
            @change="nextAutoAssigned = false"
          />
          <div
            v-if="nextAutoAssigned"
            class="auto-assign-hint"
          >
            <CheckCircleOutlined /> 已按「审批人配置」自动指派，可手动改选
          </div>
          <div
            v-else-if="nextSuggestLoaded && !nextApproverId"
            class="auto-assign-hint auto-assign-hint--warn"
          >
            <ExclamationCircleOutlined /> 未配置该级默认审批人，可先在「审批人配置」中预设
          </div>
        </a-form-item>
        <a-form-item
          :label="approveAction === 'APPROVE' ? '审批意见' : '驳回原因'"
          :required="approveAction === 'REJECT'"
        >
          <a-textarea
            v-model:value="comment"
            :rows="3"
            :maxlength="200"
            show-count
            :placeholder="approveAction === 'APPROVE' ? '选填' : '必填，将回写至费用单驳回原因'"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ═══ 提交审批弹窗（与费用单页共用） ═══ -->
    <SubmitApprovalModal
      v-model:open="submitModalOpen"
      :record="currentRecord"
      @success="fetchData"
    />

    <!-- ═══ 审批人配置弹窗（自动指派） ═══ -->
    <ApproverConfigModal
      v-model:open="showApproverConfig"
      @saved="fetchData"
    />

    <!-- ═══ 审批详情弹窗（费用单 + 费用项明细 + 审批记录） ═══ -->
    <a-modal
      v-model:open="detailModalOpen"
      title="费用审批详情"
      :width="1080"
      :footer="null"
      destroy-on-close
    >
      <a-spin :spinning="detailLoading">
        <div
          v-if="detail"
          class="detail-body"
        >
          <a-descriptions
            :column="4"
            size="small"
            bordered
            title="费用单"
          >
            <a-descriptions-item label="单据编号">
              {{ detail.doc?.docNo }}
            </a-descriptions-item>
            <a-descriptions-item label="单据日期">
              {{ detail.doc?.docDate }}
            </a-descriptions-item>
            <a-descriptions-item label="费用类型">
              {{ expenseTypeText(detail.doc?.expenseType) }}
            </a-descriptions-item>
            <a-descriptions-item label="本单金额">
              <span class="currency-value">{{ formatAmount(detail.doc?.totalAmount) }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="往来单位">
              {{ detail.doc?.partnerName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="往来编号">
              {{ detail.doc?.partnerCode || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="经手人">
              {{ detail.doc?.handlerName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="部门">
              {{ detail.doc?.deptName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="制单人">
              {{ detail.doc?.creatorName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="制单时间">
              {{ formatDateTime(detail.doc?.createTime) }}
            </a-descriptions-item>
            <a-descriptions-item label="单据状态">
              <a-tag :color="docStatusColor(detail.doc?.status)">
                {{ docStatusText(detail.doc?.status) }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="审批状态">
              <a-tag :color="approvalStatusColor(detail.doc?.approvalStatus)">
                {{ approvalStatusText(detail.doc) }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="当前审批人">
              {{ detail.doc?.currentApproverName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="提交时间">
              {{ formatDateTime(detail.doc?.submitTime) }}
            </a-descriptions-item>
            <a-descriptions-item label="付款账户1">
              {{ detail.doc?.payAccountName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="付款金额1">
              <span class="currency-value">{{ formatAmount(detail.doc?.payAmount) }}</span>
            </a-descriptions-item>
            <a-descriptions-item
              label="摘要"
              :span="2"
            >
              {{ detail.doc?.summary || '-' }}
            </a-descriptions-item>
            <a-descriptions-item
              label="单据备注"
              :span="2"
            >
              {{ detail.doc?.remark || '-' }}
            </a-descriptions-item>
            <a-descriptions-item
              v-if="detail.doc?.rejectReason"
              label="驳回原因"
              :span="4"
            >
              <span style="color:#ff4d4f">{{ detail.doc?.rejectReason }}</span>
            </a-descriptions-item>
          </a-descriptions>

          <div class="detail-block-title">
            费用项明细
            <span class="detail-block-extra">合计 ¥{{ formatAmount(detailTotalAmount) }}</span>
          </div>
          <a-table
            :columns="itemColumns"
            :data-source="detail.doc?.items || []"
            :pagination="false"
            size="small"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'amount'">
                <span class="currency-value">{{ formatAmount(record.amount) }}</span>
              </template>
            </template>
          </a-table>

          <div class="detail-block-title">
            审批记录
          </div>
          <a-table
            :columns="recordColumns"
            :data-source="detail.records || []"
            :pagination="false"
            size="small"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'approvalAction'">
                <a-tag :color="actionColor(record.approvalAction)">
                  {{ actionText(record.approvalAction) }}
                </a-tag>
              </template>
              <template v-if="column.key === 'approvalTime'">
                {{ formatDateTime(record.approvalTime) }}
              </template>
            </template>
          </a-table>
        </div>
      </a-spin>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  ReloadOutlined, SettingOutlined, ExportOutlined, TeamOutlined,
  CheckCircleOutlined, ExclamationCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import SubmitApprovalModal from './SubmitApprovalModal.vue'
import ApproverConfigModal from './ApproverConfigModal.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { expenseApprovalApi } from '@/api/finance'
import optionsApi from '@/api/options'

defineOptions({ name: 'FinanceExpenseApprovalPage' })

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
const quickDate = ref('last3Month')
const queryScheme = ref('')

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const searchGridRef = ref<HTMLElement | null>(null)
const searchActionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(searchActionRef, searchGridRef)

// ═══ 搜索参数（11 查询条件） ═══
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(3, 'month'), dayjs()])
const dateStart = ref(dateRange.value?.[0]?.format('YYYY-MM-DD') || '')
const dateEnd = ref(dateRange.value?.[1]?.format('YYYY-MM-DD') || '')
const search = reactive({
  docNo: '',
  partnerName: '',
  handlerName: '',
  deptName: '',
  expenseType: undefined as string | undefined,
  creatorName: '',
  approvalStatus: '1' as string | undefined,
  currentApproverName: '',
  summary: '',
  onlyMine: true,
})

function handleDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates && dates.length === 2) {
    dateStart.value = dates[0]?.format('YYYY-MM-DD') || ''
    dateEnd.value = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    dateStart.value = ''
    dateEnd.value = ''
  }
}

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
}))

// ═══ 页面配置 ═══
// 列配置：用数据表表头齿轮（BillDetailTable 内置，storage-key=expense-approval-table-columns），工具栏不再放重复入口
const showPageConfig = ref(false)

const ALL_COLUMNS = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 190, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'docDate', key: 'docDate', width: 110, sortable: true },
  { title: '单据编号', field: 'docNo', key: 'docNo', width: 170, type: 'slot', slotName: 'docNoCell' },
  { title: '审批状态', field: 'approvalStatus', key: 'approvalStatus', width: 110, align: 'center', type: 'slot', slotName: 'approvalStatusCell' },
  { title: '审批进度', field: 'approvalLevel', key: 'approvalLevel', width: 150, align: 'center', type: 'slot', slotName: 'approvalLevelCell' },
  { title: '当前审批人', field: 'currentApproverName', key: 'currentApproverName', width: 110 },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '往来单位', field: 'partnerName', key: 'partnerName', width: 180, sortable: true },
  { title: '往来单位编号', field: 'partnerCode', key: 'partnerCode', width: 130, defaultHidden: true },
  { title: '费用类型', field: 'expenseType', key: 'expenseType', width: 110, align: 'center', type: 'slot', slotName: 'expenseTypeCell' },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 90 },
  { title: '部门', field: 'deptName', key: 'deptName', width: 100, defaultHidden: true },
  { title: '金额', field: 'totalAmount', key: 'totalAmount', width: 120, align: 'right', type: 'slot', slotName: 'totalAmountCell', sortable: true },
  { title: '提交时间', field: 'submitTime', key: 'submitTime', width: 150, type: 'slot', slotName: 'submitTimeCell' },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 90 },
  { title: '摘要', field: 'summary', key: 'summary', width: 160, defaultHidden: true },
  { title: '单据备注', field: 'remark', key: 'remark', width: 140, defaultHidden: true },
  { title: '驳回原因', field: 'rejectReason', key: 'rejectReason', width: 160, defaultHidden: true },
  { title: '付款账户1', field: 'payAccountName', key: 'payAccountName', width: 150, defaultHidden: true },
  { title: '付款金额1', field: 'payAmount', key: 'payAmount', width: 110, align: 'right', type: 'slot', slotName: 'payAmountCell', defaultHidden: true },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 90, defaultHidden: true },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 150, defaultHidden: true },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 150, defaultHidden: true },
]

// ═══ 页面配置（查询条件 + 功能按钮） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '日期', visible: true },
  { key: 'docNo', label: '单据编号', visible: true },
  { key: 'partnerName', label: '往来单位', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: true },
  { key: 'expenseType', label: '费用类型', visible: true },
  { key: 'creatorName', label: '制单人', visible: true },
  { key: 'approvalStatus', label: '审批状态', visible: true },
  { key: 'currentApproverName', label: '当前审批人', visible: true },
  { key: 'summary', label: '摘要', visible: false },
  { key: 'onlyMine', label: '仅看我的待办', visible: true },
]
const FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'pageConfig', label: '页面配置', enabled: true },
]

const queryFieldsConfig = ref<QueryFieldSetting[]>(QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonsConfig = ref<FunctionButtonSetting[]>(FUNCTION_BUTTONS.map(f => ({ ...f })))

function loadPageConfig() {
  try {
    const raw = localStorage.getItem('expense-approval-page-config')
    if (!raw) return
    const parsed = JSON.parse(raw)
    if (parsed.queryFields) {
      queryFieldsConfig.value = QUERY_FIELDS.map(df => {
        const saved = parsed.queryFields.find((f: QueryFieldSetting) => f.key === df.key)
        return saved ? { ...df, ...saved } : { ...df }
      })
    }
    if (parsed.functionButtons) {
      functionButtonsConfig.value = FUNCTION_BUTTONS.map(bf => {
        const saved = parsed.functionButtons.find((f: FunctionButtonSetting) => f.key === bf.key)
        return saved ? { ...bf, ...saved } : { ...bf }
      })
    }
  } catch { /* ignore */ }
}

function handlePageConfigChange(config: any) {
  localStorage.setItem('expense-approval-page-config', JSON.stringify({
    queryFields: config.queryFields || [],
    functionButtons: config.functionButtons || functionButtonsConfig.value,
  }))
  if (config.queryFields) queryFieldsConfig.value = config.queryFields
  if (config.functionButtons) functionButtonsConfig.value = config.functionButtons
  loadPageConfig()
}

function fieldVisible(key: string): boolean {
  return queryFieldsConfig.value.find(f => f.key === key)?.visible !== false
}
function btnEnabled(key: string): boolean {
  return functionButtonsConfig.value.find(b => b.key === key)?.enabled !== false
}

// ═══ 状态映射 ═══
const APPROVAL_STATUS_MAP: Record<string, { text: string; color: string }> = {
  '0': { text: '未提交', color: 'default' },
  '1': { text: '审批中', color: 'orange' },
  '2': { text: '审批通过', color: 'green' },
  '3': { text: '审批驳回', color: 'red' },
}
const DOC_STATUS_MAP: Record<string, { text: string; color: string }> = {
  '0': { text: '草稿', color: 'default' },
  '1': { text: '已记账', color: 'blue' },
  '2': { text: '已取消', color: 'red' },
}

function approvalStatusText(record: any): string {
  const status = String(record?.approvalStatus ?? '0')
  const base = APPROVAL_STATUS_MAP[status]?.text || '未提交'
  if (status === '1') {
    return `${levelName(record?.approvalLevel)}审批中`
  }
  return base
}
function approvalStatusColor(status: any): string {
  return APPROVAL_STATUS_MAP[String(status ?? '0')]?.color || 'default'
}
function docStatusText(status: any): string {
  return DOC_STATUS_MAP[String(status ?? '0')]?.text || '-'
}
function docStatusColor(status: any): string {
  return DOC_STATUS_MAP[String(status ?? '0')]?.color || 'default'
}
function levelName(level: any): string {
  const n = Number(level)
  if (n === 1) return '部门'
  if (n === 2) return '财务'
  if (n === 3) return '总经理'
  return '第' + (n || 0) + '级'
}
function expenseTypeText(type: any): string {
  return String(type) === '1' ? '内部费用' : '往来单位费用'
}
function actionText(action: string): string {
  if (action === 'SUBMIT') return '提交'
  if (action === 'APPROVE') return '通过'
  if (action === 'REJECT') return '驳回'
  return action || '-'
}
function actionColor(action: string): string {
  if (action === 'SUBMIT') return 'blue'
  if (action === 'APPROVE') return 'green'
  if (action === 'REJECT') return 'red'
  return 'default'
}
function isPending(record: any): boolean {
  return String(record?.approvalStatus ?? '0') === '1'
}
function canSubmit(record: any): boolean {
  const approval = String(record?.approvalStatus ?? '0')
  return String(record?.status ?? '0') === '0' && (approval === '0' || approval === '3')
}

function formatAmount(amount: any): string {
  if (amount === undefined || amount === null || amount === '') return '0.00'
  return Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function formatDateTime(value: any): string {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

// ═══ 表格底部合计 ═══
const tableFooterColumns = computed(() => {
  const total = tableData.value.reduce((s: number, r: any) => s + (Number(r.totalAmount) || 0), 0)
  return [{ key: 'totalAmount', value: total, highlight: true }]
})

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (search.docNo) params.docNo = search.docNo
    if (search.partnerName) params.partnerName = search.partnerName
    if (search.handlerName) params.handlerName = search.handlerName
    if (search.deptName) params.deptName = search.deptName
    if (search.creatorName) params.creatorName = search.creatorName
    if (search.currentApproverName) params.currentApproverName = search.currentApproverName
    if (search.summary) params.summary = search.summary
    if (search.expenseType !== undefined && search.expenseType !== '') params.expenseType = search.expenseType
    if (search.approvalStatus !== undefined && search.approvalStatus !== '') params.approvalStatus = search.approvalStatus
    if (dateStart.value) params.dateStart = dateStart.value
    if (dateEnd.value) params.dateEnd = dateEnd.value
    if (search.onlyMine) params.onlyMine = true
    const res: any = await expenseApprovalApi.getPending(params)
    const body = res ?? {}
    tableData.value = body?.records || []
    pagination.total = Number(body?.total) || 0
  } catch (error: any) {
    console.warn('[费用审批] 获取待审批列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
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
    default: start = now.subtract(3, 'month'); end = now
  }
  dateRange.value = [start, end]
  dateStart.value = start.format('YYYY-MM-DD')
  dateEnd.value = end.format('YYYY-MM-DD')
  handleSearch()
}

// ═══ 审批人下拉 ═══
const approverOptions = ref<any[]>([])
const loadingApprovers = ref(false)
async function loadApprovers() {
  loadingApprovers.value = true
  try {
    const users = await optionsApi.getUsers()
    approverOptions.value = (users || []).map((u: any) => ({
      label: u.nickname || u.name || u.realName || u.username || String(u.id),
      value: u.id,
    }))
  } catch (error) {
    console.warn('[费用审批] 加载审批人失败', error)
    approverOptions.value = []
  } finally {
    loadingApprovers.value = false
  }
}

// ═══ 审批操作 ═══
const approveModalOpen = ref(false)
const submitModalOpen = ref(false)
const showApproverConfig = ref(false)
const submitting = ref(false)
const approveAction = ref<'APPROVE' | 'REJECT'>('APPROVE')
const currentRecord = ref<any>(null)
const comment = ref('')
const nextApproverId = ref<number | undefined>(undefined)
const nextAutoAssigned = ref(false)
const nextSuggestLoaded = ref(false)

const isNotLastLevel = computed(() => {
  const level = Number(currentRecord.value?.approvalLevel || 0)
  const total = Number(currentRecord.value?.totalApprovalLevel || 0)
  return level > 0 && total > 0 && level < total
})

/** 自动指派：解析下一级建议审批人（审批人配置） */
async function loadNextSuggest() {
  nextAutoAssigned.value = false
  nextSuggestLoaded.value = false
  nextApproverId.value = undefined
  const docId = currentRecord.value?.id
  const total = Number(currentRecord.value?.totalApprovalLevel) || 3
  if (!docId) return
  try {
    const res: any = await expenseApprovalApi.getSuggest(docId, total)
    const list: any[] = res?.data || res || []
    const nextLevel = Number(currentRecord.value?.approvalLevel || 0) + 1
    const next = list.find((s: any) => Number(s.level) === nextLevel)
    if (next?.approverId) {
      nextApproverId.value = next.approverId
      nextAutoAssigned.value = true
    }
  } catch {
    /* 建议失败不阻断手选 */
  } finally {
    nextSuggestLoaded.value = true
  }
}

function openApproveModal(record: any, action: 'APPROVE' | 'REJECT') {
  currentRecord.value = record
  approveAction.value = action
  comment.value = ''
  nextApproverId.value = undefined
  nextAutoAssigned.value = false
  nextSuggestLoaded.value = false
  if (approverOptions.value.length === 0) loadApprovers()
  approveModalOpen.value = true
  if (action === 'APPROVE' && isNotLastLevel.value) loadNextSuggest()
}

function resetApproveModal() {
  approveModalOpen.value = false
  currentRecord.value = null
  comment.value = ''
  nextApproverId.value = undefined
}

async function handleApproveSubmit() {
  if (!currentRecord.value) return
  if (approveAction.value === 'REJECT' && !comment.value.trim()) {
    message.warning('请填写驳回原因')
    return
  }
  if (approveAction.value === 'APPROVE' && isNotLastLevel.value && !nextApproverId.value && !nextAutoAssigned.value) {
    message.warning('请选择下一级审批人（可在「审批人配置」中预设实现自动指派）')
    return
  }
  submitting.value = true
  try {
    const next = approverOptions.value.find(o => o.value === nextApproverId.value)
    await expenseApprovalApi.process({
      docId: currentRecord.value.id,
      action: approveAction.value,
      comment: comment.value.trim() || undefined,
      nextApproverId: approveAction.value === 'APPROVE' ? nextApproverId.value : undefined,
      nextApproverName: approveAction.value === 'APPROVE' ? next?.label : undefined,
    })
    message.success(approveAction.value === 'APPROVE' ? '审批已通过' : '审批已驳回')
    resetApproveModal()
    fetchData()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '审批操作失败')
  } finally {
    submitting.value = false
  }
}

function openSubmitModal(record: any) {
  currentRecord.value = record
  submitModalOpen.value = true
}

// ═══ 详情 ═══
const detailModalOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)

const itemColumns = [
  { title: '行号', key: 'lineNo', dataIndex: 'lineNo', width: 70 },
  { title: '费用编号', key: 'expenseCode', dataIndex: 'expenseCode', width: 140 },
  { title: '费用名称', key: 'expenseName', dataIndex: 'expenseName', width: 200 },
  { title: '费用科目', key: 'subjectCode', dataIndex: 'subjectCode', width: 120 },
  { title: '金额', key: 'amount', dataIndex: 'amount', width: 130, align: 'right' },
  { title: '备注', key: 'remark', dataIndex: 'remark' },
]
const recordColumns = [
  { title: '审批级别', key: 'approvalLevel', dataIndex: 'approvalLevel', width: 100 },
  { title: '审批人', key: 'approverName', dataIndex: 'approverName', width: 120 },
  { title: '动作', key: 'approvalAction', dataIndex: 'approvalAction', width: 90 },
  { title: '审批意见', key: 'approvalComment', dataIndex: 'approvalComment' },
  { title: '审批时间', key: 'approvalTime', dataIndex: 'approvalTime', width: 170 },
  { title: '审批前状态', key: 'previousStatus', dataIndex: 'previousStatus', width: 140 },
  { title: '审批后状态', key: 'currentStatus', dataIndex: 'currentStatus', width: 140 },
]

const detailTotalAmount = computed(() => {
  const items = detail.value?.doc?.items || []
  return items.reduce((s: number, i: any) => s + (Number(i.amount) || 0), 0)
})

async function openDetail(record: any) {
  detail.value = null
  detailModalOpen.value = true
  detailLoading.value = true
  try {
    const res: any = await expenseApprovalApi.getDetail(record.id)
    detail.value = res || null
  } catch (error: any) {
    console.warn('[费用审批] 加载详情失败', error)
    message.error(error?.response?.data?.message || '加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

// ═══ 导出 ═══
async function handleExport() {
  try {
    const params: any = { pageNum: 1, pageSize: 9999 }
    if (search.approvalStatus !== undefined && search.approvalStatus !== '') params.approvalStatus = search.approvalStatus
    if (dateStart.value) params.dateStart = dateStart.value
    if (dateEnd.value) params.dateEnd = dateEnd.value
    const res: any = await expenseApprovalApi.getPending(params)
    const data = Array.isArray(res) ? res : (res?.records || [])
    if (data.length === 0) {
      message.warning('没有可导出的数据')
      return
    }
    const headers = ['单据日期', '单据编号', '审批状态', '审批进度', '当前审批人', '单据状态', '往来单位', '费用类型', '经手人', '金额', '提交时间', '制单人']
    const rows = data.map((r: any) => [
      r.docDate, r.docNo, approvalStatusText(r),
      Number(r.approvalStatus) === 1 ? `${levelName(r.approvalLevel)}(${r.approvalLevel}/${r.totalApprovalLevel})` : '-',
      r.currentApproverName || '', docStatusText(r.status), r.partnerName || '',
      expenseTypeText(r.expenseType), r.handlerName || '', r.totalAmount ?? 0,
      formatDateTime(r.submitTime), r.creatorName || '',
    ])
    const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `费用审批_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  }
}

function handleError(error: Error) {
  console.error('[费用审批] 页面错误', error)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadPageConfig()
  fetchData()
})
</script>

<style scoped>
.query-scheme-wrap { display: flex; align-items: center; gap: 2px; }
.quick-dates :deep(.ant-btn) { font-size: 13px; padding: 2px 8px; }
.quick-dates :deep(.ant-btn-primary) { color: #fff; background: #ff7a45; border-color: #ff7a45; }
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-container > .search-grid { max-height: 80px; overflow: hidden; }
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
.search-action-group { display: flex; flex-wrap: nowrap; align-items: center; }
.search-action-group .search-field-item { width: auto; flex: 0 0 auto; margin-right: 4px; }
.search-action-group .search-field-item:last-child { margin-right: 0; }
.table-area { flex: 1; min-height: 0; overflow: hidden; }
.currency-value { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; }
.detail-body { max-height: 70vh; overflow-y: auto; }
.detail-block-title { margin: 16px 0 8px; font-size: 14px; font-weight: 600; color: #262626; }
.auto-assign-hint { margin-top: 4px; font-size: 12px; color: #52c41a; line-height: 18px; }
.auto-assign-hint--warn { color: #fa8c16; }
.detail-block-extra { margin-left: 12px; font-size: 12px; font-weight: 400; color: #8c8c8c; }
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
