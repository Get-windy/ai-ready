<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        请假管理（人力资源 → 考勤管理 → 请假管理，菜单 90003 / hr:leave）
        · 单入口单视图、无左分类树 → show-category-panel=false + tabs=[]
        · 列配置齿轮在数据表表头 rowNo 列（个人 localStorage / 全局 userPageConfigApi）
        · 工具栏：提交请假 ｜ 假期额度 ｜ 页面配置 / 刷新 / 打印(F8) / 导出
        · 查询区（受页面配置控制）：员工 · 请假类型 · 申请状态 · 部门 · 请假日期区间
        · 行内操作严守状态机：待审批=批准/拒绝/修改/撤销/删除；已批准=撤销；已拒绝/已撤销=删除
        · 提交/修改用弹窗表单（本页无明细，不走 BillFormPage）
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('create')"
              type="primary"
              size="small"
              class="btn-add"
              @click="handleCreate"
            >
              <PlusOutlined /> 提交请假
            </a-button>
            <a-button
              v-if="isButtonEnabled('quota')"
              size="small"
              @click="handleOpenQuota"
            >
              <ClockCircleOutlined /> 假期额度
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              title="页面配置"
              @click="showPageConfig = true"
            >
              <SettingOutlined />
            </a-button>
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="fetchList"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="isButtonEnabled('printF8')"
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="isButtonEnabled('export')"
              size="small"
              :loading="exporting"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（横向自适应网格；字段显隐由页面配置控制） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div
                v-if="isQueryVisible('employeeId')"
                class="search-item"
              >
                <span class="search-label">员工</span>
                <a-select
                  v-model:value="searchForm.employeeId"
                  placeholder="全部员工"
                  size="small"
                  style="width: 210px"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="employeeOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('leaveType')"
                class="search-item"
              >
                <span class="search-label">请假类型</span>
                <a-select
                  v-model:value="searchForm.leaveType"
                  placeholder="全部类型"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  :options="leaveTypeOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('status')"
                class="search-item"
              >
                <span class="search-label">申请状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  :options="statusOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('deptId')"
                class="search-item"
              >
                <span class="search-label">部门</span>
                <a-select
                  v-model:value="searchForm.deptId"
                  placeholder="全部部门"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="deptOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('startDate')"
                class="search-item"
              >
                <span class="search-label">请假日期</span>
                <a-range-picker
                  v-model:value="searchForm.startDateRange"
                  size="small"
                  style="width: 230px"
                  value-format="YYYY-MM-DD"
                  @change="handleSearch"
                />
              </div>
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
              row-key="id"
              storage-key="hr-leave-table-columns"
              global-config-key="hr-leave-table-columns"
            >
              <template #nameCell="{ record }">
                <span v-if="!record.__ghost">{{ record.employeeName || '-' }}</span>
              </template>

              <template #typeCell="{ record }">
                <span v-if="!record.__ghost">{{ LEAVE_TYPE_MAP[record.leaveType] || record.leaveType || '-' }}</span>
              </template>

              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="LEAVE_STATUS_MAP[record.status]?.color || 'default'"
                >
                  {{ LEAVE_STATUS_MAP[record.status]?.text || '-' }}
                </a-tag>
              </template>

              <template #daysCell="{ record }">
                <span
                  v-if="!record.__ghost"
                  class="days-cell"
                >{{ formatDays(record.days) }}</span>
              </template>

              <template #approverCell="{ record }">
                <span v-if="!record.__ghost">{{ record.approveName || record.approveId || '-' }}</span>
              </template>

              <!-- 插槽必须透传 column：多列共用同一「日期」渲染 -->
              <template #dateCell="{ record, column }">
                <span v-if="!record.__ghost">{{ formatDate(record[column.key]) }}</span>
              </template>

              <template #dateTimeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ formatDateTime(record[column.key]) }}</span>
              </template>

              <!-- 操作列：严格按申请状态出按钮（待审批 5 个 / 已批准 1 个 / 已拒绝·已撤销 1 个） -->
              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <template v-if="Number(record.status) === 0">
                    <a-button
                      type="link"
                      size="small"
                      @click="handleApproveClick(record)"
                    >
                      批准
                    </a-button>
                    <a-button
                      type="link"
                      size="small"
                      danger
                      @click="handleRejectClick(record)"
                    >
                      拒绝
                    </a-button>
                    <a-button
                      type="link"
                      size="small"
                      @click="handleEdit(record)"
                    >
                      修改
                    </a-button>
                    <a-button
                      type="link"
                      size="small"
                      @click="handleCancel(record)"
                    >
                      撤销
                    </a-button>
                    <a-button
                      type="link"
                      size="small"
                      danger
                      @click="handleDelete(record)"
                    >
                      删除
                    </a-button>
                  </template>
                  <a-button
                    v-else-if="Number(record.status) === 1"
                    type="link"
                    size="small"
                    @click="handleCancel(record)"
                  >
                    撤销
                  </a-button>
                  <a-button
                    v-else
                    type="link"
                    size="small"
                    danger
                    @click="handleDelete(record)"
                  >
                    删除
                  </a-button>
                </a-space>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <!-- ═══ 底部：经典分页栏 ═══ -->
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

      <!-- ═══ 提交 / 修改请假申请 ═══ -->
      <a-modal
        v-model:open="formOpen"
        :title="formState.id ? '修改请假申请' : '提交请假申请'"
        :width="560"
        :mask-closable="false"
        :confirm-loading="formSaving"
        @ok="handleSave"
      >
        <a-form
          ref="formRef"
          :model="formState"
          :rules="rules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 17 }"
          size="small"
        >
          <a-form-item
            label="申请人员工"
            name="employeeId"
          >
            <a-select
              v-model:value="formState.employeeId"
              placeholder="请选择申请人员工"
              show-search
              option-filter-prop="label"
              :options="employeeOptions"
            />
          </a-form-item>
          <a-form-item
            label="请假类型"
            name="leaveType"
          >
            <a-select
              v-model:value="formState.leaveType"
              :options="leaveTypeOptions"
            />
          </a-form-item>
          <a-form-item
            label="开始日期"
            name="startDate"
          >
            <a-date-picker
              v-model:value="formState.startDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
              @change="recalcDays"
            />
          </a-form-item>
          <a-form-item
            label="结束日期"
            name="endDate"
          >
            <a-date-picker
              v-model:value="formState.endDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
              @change="recalcDays"
            />
          </a-form-item>
          <a-form-item
            label="请假天数"
            name="days"
            extra="由开始/结束日期自动核算（含首尾两天）"
          >
            <a-input-number
              v-model:value="formState.days"
              :min="0"
              :precision="1"
              disabled
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item
            label="请假原因"
            name="reason"
          >
            <a-textarea
              v-model:value="formState.reason"
              :rows="3"
              :maxlength="500"
              placeholder="请输入请假原因"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 批准 / 拒绝（审批意见必传，否则 approve_comment 恒 NULL） ═══ -->
      <a-modal
        v-model:open="approveOpen"
        :title="approveMode === 'approve' ? '批准请假' : '拒绝请假'"
        :width="480"
        :mask-closable="false"
        :confirm-loading="approveSaving"
        @ok="handleSubmitApprove"
      >
        <a-form
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
          size="small"
        >
          <a-form-item label="申请单">
            <span>{{ approveTarget?.employeeName || approveTarget?.employeeId }} ·
              {{ LEAVE_TYPE_MAP[approveTarget?.leaveType] || approveTarget?.leaveType }} ·
              {{ formatDate(approveTarget?.startDate) }} ~ {{ formatDate(approveTarget?.endDate) }}
              （{{ formatDays(approveTarget?.days) }}）</span>
          </a-form-item>
          <a-form-item label="审批意见">
            <a-textarea
              v-model:value="approveComment"
              :rows="3"
              :maxlength="500"
              placeholder="请填写审批意见（随申请一起保存）"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 假期额度（读写 hr/leave/quota + hr/leave/balance） ═══ -->
      <a-modal
        v-model:open="quotaOpen"
        title="假期额度"
        :width="760"
        :footer="null"
        :mask-closable="false"
      >
        <div class="quota-toolbar">
          <a-space :size="8">
            <span class="quota-label">年度</span>
            <a-input-number
              v-model:value="quotaYear"
              :min="2000"
              :max="2100"
              :precision="0"
              size="small"
              style="width: 100px"
            />
            <a-button
              size="small"
              :loading="quotaLoading"
              @click="loadQuotas"
            >
              <ReloadOutlined /> 刷新
            </a-button>
          </a-space>
        </div>
        <a-table
          :data-source="quotaList"
          :columns="quotaColumns"
          :loading="quotaLoading"
          :pagination="false"
          row-key="leaveType"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'leaveType'">
              {{ LEAVE_TYPE_MAP[record.leaveType] || record.leaveType }}
            </template>
            <template v-else-if="column.key === 'quotaDays'">
              <a-input-number
                v-model:value="record.quotaDays"
                :min="0"
                :precision="1"
                size="small"
                style="width: 120px"
              />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button
                type="link"
                size="small"
                :loading="quotaSavingKey === record.leaveType"
                @click="handleSaveQuota(record)"
              >
                保存
              </a-button>
            </template>
          </template>
        </a-table>

        <div class="quota-balance">
          <div class="quota-balance__head">
            员工余额查询（额度 / 已用 / 剩余）
          </div>
          <a-space :size="8">
            <a-select
              v-model:value="balanceEmployeeId"
              placeholder="选择员工"
              size="small"
              style="width: 220px"
              allow-clear
              show-search
              option-filter-prop="label"
              :options="employeeOptions"
            />
            <a-button
              size="small"
              :loading="balanceLoading"
              @click="handleLoadBalance"
            >
              查询余额
            </a-button>
          </a-space>
          <a-table
            v-if="balanceRows.length"
            :data-source="balanceRows"
            :columns="balanceColumns"
            :pagination="false"
            row-key="leaveType"
            size="small"
            style="margin-top: 8px"
          />
        </div>
      </a-modal>

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="hr-leave-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="hr-leave-list"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
// 缺口（后端无端点 / 无数据源，本页不实现，勿臆造接口）：
//  · 按月/按工龄自动累积额度 accrual（无 POST /hr/leave/accrual）
//  · 销假 / 提前返岗退额度（无 PUT /hr/leave/{id}/cancel-return）
//  · 病假证明等附件上传（hr_leave_request 无附件列，通用上传端点亦未接线）
//  · 我的待审批（无 GET /hr/leave/my-pending，page 也无等价参数）
//  · 列表页无表头排序：后端 /hr/leave/page 只收 pageNum/pageSize/employeeId/deptId/status/leaveType/startDateFrom/startDateTo，
//    无排序入参 → 不提供排序控件，避免控件静默失效
//  · 新增列（年度额度余额 / 审批链节点 / 是否跨月 / 销假日期）无数据源，列配置中不提供
//  · 单列「详情」按钮：任务书行内操作清单未包含；「修改」已走 GET /hr/leave/{id} 取最新数据
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  SettingOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import {
  hrLeaveRequestApi,
  hrEmployeeApi,
  LEAVE_TYPE_MAP,
  LEAVE_TYPE_OPTIONS,
  LEAVE_STATUS_MAP,
  type HrLeaveRequest,
  type HrLeaveQuota,
} from '@/api/hr'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'HrLeaveList' })

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const tableData = ref<HrLeaveRequest[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const leaveTypeOptions = LEAVE_TYPE_OPTIONS
const statusOptions = Object.entries(LEAVE_STATUS_MAP).map(([k, v]) => ({ label: v.text, value: Number(k) }))

// ═══ 数据表列（默认可见：序号/操作 + 10 个业务列；审批与审计列默认隐藏） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 220, fixed: 'left' },
  { key: 'employeeNo', title: '员工工号', type: 'input', width: 140 },
  { key: 'employeeName', title: '员工姓名', type: 'slot', slotName: 'nameCell', width: 110 },
  { key: 'deptName', title: '部门', type: 'input', width: 130 },
  { key: 'leaveType', title: '请假类型', type: 'slot', slotName: 'typeCell', width: 100 },
  { key: 'startDate', title: '开始日期', type: 'slot', slotName: 'dateCell', width: 110 },
  { key: 'endDate', title: '结束日期', type: 'slot', slotName: 'dateCell', width: 110 },
  { key: 'days', title: '请假天数', type: 'slot', slotName: 'daysCell', width: 100, align: 'right' },
  { key: 'status', title: '申请状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'reason', title: '请假原因', type: 'input', width: 200 },
  { key: 'approveName', title: '审批人', type: 'slot', slotName: 'approverCell', width: 110 },
  // ── 默认隐藏 ──
  { key: 'approveComment', title: '审批意见', type: 'input', width: 200, defaultHidden: true },
  { key: 'approveTime', title: '审批时间', type: 'slot', slotName: 'dateTimeCell', width: 160, defaultHidden: true },
  { key: 'createTime', title: '申请时间', type: 'slot', slotName: 'dateTimeCell', width: 160, defaultHidden: true },
  { key: 'updateTime', title: '更新时间', type: 'slot', slotName: 'dateTimeCell', width: 160, defaultHidden: true },
  // createBy / updateBy：MyBatisPlusConfig 填充器未实现这两列 → 恒 NULL，列保留备查
  { key: 'createBy', title: '创建人', type: 'input', width: 100, defaultHidden: true },
  { key: 'updateBy', title: '更新人', type: 'input', width: 100, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 200, defaultHidden: true },
]

// ═══ 查询条件 ═══
const searchForm = reactive({
  employeeId: undefined as number | string | undefined,
  leaveType: undefined as string | undefined,
  status: undefined as number | undefined,
  deptId: undefined as number | string | undefined,
  startDateRange: undefined as [string, string] | undefined,
})

// ═══ 员工 / 部门下拉（部门选项由员工数据推导，避免额外引入部门接口） ═══
const employeeOptions = ref<{ label: string; value: number | string }[]>([])
const deptOptions = ref<{ label: string; value: number | string }[]>([])

async function loadEmployees() {
  try {
    const res: any = await hrEmployeeApi.page({ pageNum: 1, pageSize: 500 })
    const list: any[] = res?.records || []
    // 只取在职(1) / 试用(2)
    const active = list.filter(e => Number(e.status) === 1 || Number(e.status) === 2)
    employeeOptions.value = active.map(e => ({
      label: e.employeeNo ? `[${e.employeeNo}] ${e.employeeName}` : e.employeeName,
      value: e.id,
    }))
    const deptMap = new Map<string, { label: string; value: number | string }>()
    for (const e of active) {
      if (e.deptId === undefined || e.deptId === null) continue
      const key = String(e.deptId)
      if (!deptMap.has(key)) deptMap.set(key, { label: e.deptName || key, value: e.deptId })
    }
    deptOptions.value = Array.from(deptMap.values())
  } catch (error) {
    console.warn('[请假管理] 员工下拉加载失败', error)
  }
}

// ═══ 数据加载 ═══
function buildParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (searchForm.employeeId !== undefined) params.employeeId = searchForm.employeeId
  if (searchForm.leaveType) params.leaveType = searchForm.leaveType
  if (searchForm.status !== undefined) params.status = searchForm.status
  if (searchForm.deptId !== undefined) params.deptId = searchForm.deptId
  if (searchForm.startDateRange?.length === 2) {
    params.startDateFrom = searchForm.startDateRange[0]
    params.startDateTo = searchForm.startDateRange[1]
  }
  return params
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await hrLeaveRequestApi.page(buildParams())
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[请假管理] 加载列表失败', error)
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
  searchForm.employeeId = undefined
  searchForm.leaveType = undefined
  searchForm.status = undefined
  searchForm.deptId = undefined
  searchForm.startDateRange = undefined
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 展示辅助 ═══
function formatDate(val: string | null | undefined): string {
  if (!val) return '-'
  return String(val).slice(0, 10)
}

function formatDateTime(val: string | null | undefined): string {
  if (!val) return '-'
  return dayjs(val).isValid() ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : String(val)
}

function formatDays(val: number | string | null | undefined): string {
  if (val === null || val === undefined || val === '') return '-'
  return `${val}天`
}

// ═══ 提交 / 修改请假申请 ═══
const formOpen = ref(false)
const formRef = ref()
const formSaving = ref(false)

const emptyForm = () => ({
  id: null as number | string | null,
  employeeId: undefined as number | string | undefined,
  leaveType: 'ANNUAL' as string,
  startDate: undefined as string | undefined,
  endDate: undefined as string | undefined,
  days: 0 as number,
  reason: '',
})
const formState = reactive(emptyForm())

/** 天数由日期区间自动核算（含首尾两天），避免手填与区间不符 */
function recalcDays() {
  const { startDate, endDate } = formState
  if (!startDate || !endDate) {
    formState.days = 0
    return
  }
  const start = dayjs(startDate)
  const end = dayjs(endDate)
  formState.days = end.isBefore(start, 'day') ? 0 : end.diff(start, 'day') + 1
}

const rules = {
  employeeId: [{ required: true, message: '请选择申请人员工', trigger: 'change' }],
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [
    { required: true, message: '请选择结束日期', trigger: 'change' },
    {
      validator: async (_rule: any, value: any) => {
        if (value && formState.startDate && dayjs(value).isBefore(dayjs(formState.startDate), 'day')) {
          return Promise.reject('结束日期不能早于开始日期')
        }
        return Promise.resolve()
      },
      trigger: 'change',
    },
  ],
  days: [
    {
      validator: async (_rule: any, value: any) => {
        if (!value || Number(value) <= 0) return Promise.reject('请假天数必须大于 0')
        return Promise.resolve()
      },
      trigger: 'change',
    },
  ],
  reason: [{ required: true, message: '请输入请假原因', trigger: 'blur' }],
}

function resetForm() {
  Object.assign(formState, emptyForm())
}

function handleCreate() {
  resetForm()
  formOpen.value = true
}

/** 修改：先取最新单据（勿复用列表行快照），仅待审批可改 */
async function handleEdit(record: HrLeaveRequest) {
  resetForm()
  formOpen.value = true
  try {
    const detail: any = await hrLeaveRequestApi.getById(record.id)
    if (!detail) return
    formState.id = detail.id
    formState.employeeId = detail.employeeId
    formState.leaveType = detail.leaveType || 'ANNUAL'
    formState.startDate = detail.startDate ? String(detail.startDate).slice(0, 10) : undefined
    formState.endDate = detail.endDate ? String(detail.endDate).slice(0, 10) : undefined
    formState.days = detail.days ?? 0
    formState.reason = detail.reason || ''
    recalcDays()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载请假详情失败')
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  // 日期必须是 YYYY-MM-DD 字符串：dayjs 对象直发会被 Jackson 按 UTC 解析，日期整体前移一天
  const payload = {
    employeeId: formState.employeeId,
    leaveType: formState.leaveType,
    startDate: formState.startDate,
    endDate: formState.endDate,
    days: formState.days,
    reason: formState.reason.trim(),
  }
  formSaving.value = true
  try {
    if (formState.id) {
      await hrLeaveRequestApi.update(formState.id, payload)
      message.success('修改成功')
    } else {
      await hrLeaveRequestApi.submit(payload)
      message.success('提交成功，等待审批')
    }
    formOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    formSaving.value = false
  }
}

// ═══ 批准 / 拒绝（必带审批意见） ═══
const approveOpen = ref(false)
const approveSaving = ref(false)
const approveMode = ref<'approve' | 'reject'>('approve')
const approveTarget = ref<HrLeaveRequest | null>(null)
const approveComment = ref('')

function handleApproveClick(record: HrLeaveRequest) {
  approveTarget.value = record
  approveMode.value = 'approve'
  approveComment.value = ''
  approveOpen.value = true
}

function handleRejectClick(record: HrLeaveRequest) {
  approveTarget.value = record
  approveMode.value = 'reject'
  approveComment.value = ''
  approveOpen.value = true
}

async function handleSubmitApprove() {
  if (!approveTarget.value) return
  approveSaving.value = true
  try {
    const comment = approveComment.value.trim() || undefined
    if (approveMode.value === 'approve') {
      await hrLeaveRequestApi.approve(approveTarget.value.id, comment)
      message.success('批准成功')
    } else {
      await hrLeaveRequestApi.reject(approveTarget.value.id, comment)
      message.success('已拒绝')
    }
    approveOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '审批失败')
  } finally {
    approveSaving.value = false
  }
}

// ═══ 撤销 / 删除 ═══
function handleCancel(record: HrLeaveRequest) {
  Modal.confirm({
    title: '确认撤销',
    content: `确定撤销「${record.employeeName || record.employeeId}」的这条请假申请吗？撤销后不可再审批。`,
    onOk: async () => {
      try {
        await hrLeaveRequestApi.cancel(record.id)
        message.success('已撤销')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '撤销失败')
      }
    },
  })
}

function handleDelete(record: HrLeaveRequest) {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这条请假申请吗？删除后不再出现在台账中。',
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await hrLeaveRequestApi.remove(record.id)
        message.success('删除成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 假期额度 ═══
const quotaOpen = ref(false)
const quotaLoading = ref(false)
const quotaSavingKey = ref<string>('')
const quotaYear = ref(dayjs().year())
const quotaList = ref<HrLeaveQuota[]>([])
const balanceLoading = ref(false)
const balanceEmployeeId = ref<number | string | undefined>(undefined)
const balanceRows = ref<{ leaveType: string; quota: any; used: any; remain: any }[]>([])

const quotaColumns = [
  { title: '假期类型', dataIndex: 'leaveType', key: 'leaveType', width: 160 },
  { title: '年度额度(天)', dataIndex: 'quotaDays', key: 'quotaDays', width: 180 },
  { title: '操作', key: 'action', width: 100 },
]

const balanceColumns = [
  { title: '假期类型', dataIndex: 'leaveType', key: 'leaveType', width: 140 },
  { title: '额度', dataIndex: 'quota', key: 'quota', width: 110 },
  { title: '已用', dataIndex: 'used', key: 'used', width: 110 },
  { title: '剩余', dataIndex: 'remain', key: 'remain', width: 110 },
]

function handleOpenQuota() {
  quotaOpen.value = true
  if (!quotaList.value.length) loadQuotas()
}

async function loadQuotas() {
  quotaLoading.value = true
  try {
    const res: any = await hrLeaveRequestApi.quotas(quotaYear.value)
    const list: any[] = Array.isArray(res) ? res : (res?.records || [])
    // 后端只为已配置的类型返回行，这里补齐 5 种类型，缺失的按 0 天展示
    const map = new Map<string, any>()
    for (const item of list) {
      if (item?.leaveType) map.set(String(item.leaveType), item)
    }
    quotaList.value = LEAVE_TYPE_OPTIONS.map(opt => {
      const hit = map.get(opt.value)
      return {
        id: hit?.id,
        leaveType: opt.value,
        year: quotaYear.value,
        quotaDays: Number(hit?.quotaDays ?? 0),
        remark: hit?.remark,
      }
    })
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载假期额度失败')
    quotaList.value = LEAVE_TYPE_OPTIONS.map(opt => ({
      leaveType: opt.value,
      year: quotaYear.value,
      quotaDays: 0,
    }))
  } finally {
    quotaLoading.value = false
  }
}

async function handleSaveQuota(row: HrLeaveQuota) {
  quotaSavingKey.value = row.leaveType
  try {
    await hrLeaveRequestApi.saveQuota({
      id: row.id,
      leaveType: row.leaveType,
      year: quotaYear.value,
      quotaDays: Number(row.quotaDays) || 0,
    })
    message.success(`${LEAVE_TYPE_MAP[row.leaveType] || row.leaveType}额度已保存`)
    await loadQuotas()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存额度失败')
  } finally {
    quotaSavingKey.value = ''
  }
}

/** 后端 balance 返回结构未定契约 → 按两种常见形态兜底归一化展示 */
function normalizeBalance(res: any) {
  const rows: { leaveType: string; quota: any; used: any; remain: any }[] = []
  if (!res || typeof res !== 'object') return rows
  const entries: [string, any][] = Array.isArray(res)
    ? res.map((v: any) => [String(v?.leaveType ?? ''), v] as [string, any])
    : Object.entries(res)
  for (const [key, value] of entries) {
    if (!key) continue
    const label = LEAVE_TYPE_MAP[key] || LEAVE_TYPE_MAP[value?.leaveType] || key
    if (value && typeof value === 'object') {
      rows.push({
        leaveType: label,
        quota: value.quotaDays ?? value.quota ?? value.totalDays ?? value.total ?? '-',
        used: value.usedDays ?? value.used ?? '-',
        remain: value.remainDays ?? value.remain ?? value.balance ?? value.available ?? '-',
      })
    } else {
      rows.push({ leaveType: label, quota: '-', used: '-', remain: value })
    }
  }
  return rows
}

async function handleLoadBalance() {
  if (balanceEmployeeId.value === undefined) {
    message.warning('请先选择员工')
    return
  }
  balanceLoading.value = true
  try {
    const res: any = await hrLeaveRequestApi.balance(balanceEmployeeId.value, quotaYear.value)
    balanceRows.value = normalizeBalance(res)
    if (!balanceRows.value.length) message.info('该员工暂无额度记录')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '查询余额失败')
    balanceRows.value = []
  } finally {
    balanceLoading.value = false
  }
}

// ═══ 打印（结果集打印）：请假申请列表 ═══
// 原先是自己拼 HTML + 浏览器打印，现在交给 PrintDialog：列与行由页面给，模板负责版式。
// 打印列与原来的表格逐列对齐（# 行号、类型/状态中文、天数口径都在 formatter 里还原）。
const printColumns: any[] = [
  { title: '#', key: '__seq', width: 40, align: 'center' },
  { title: '工号', key: 'employeeNo' },
  { title: '姓名', key: 'employeeName', formatter: (v: any) => v || '' },
  { title: '部门', key: 'deptName', formatter: (v: any) => v || '' },
  { title: '请假类型', key: 'leaveType', formatter: (_v: any, r: any) => LEAVE_TYPE_MAP[r.leaveType] || r.leaveType || '' },
  { title: '开始日期', key: 'startDate', formatter: (_v: any, r: any) => formatDate(r.startDate) },
  { title: '结束日期', key: 'endDate', formatter: (_v: any, r: any) => formatDate(r.endDate) },
  { title: '天数', key: 'days', formatter: (_v: any, r: any) => formatDays(r.days) },
  { title: '状态', key: 'status', formatter: (_v: any, r: any) => LEAVE_STATUS_MAP[r.status]?.text || '' },
  { title: '审批人', key: 'approveName', formatter: (_v: any, r: any) => r.approveName || r.approveId || '' },
  { title: '审批意见', key: 'approveComment', formatter: (v: any) => v || '' },
  { title: '请假原因', key: 'reason', formatter: (v: any) => v || '' },
]

/** 可打印行（去掉树形占位行）——标题里的记录数与表格行同源 */
function printableRows(): any[] {
  return (tableData.value || []).filter((r: any) => !r.__ghost)
}

const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'hr-leave-list',
  // 原打印抬头的「记录数」元信息行并入标题；打印时间由引擎按本次打印时间给
  title: () => `请假申请单（记录数：${printableRows().length}）`,
  columns: () => printColumns,
  rows: () => printableRows().map((r: any, i: number) => ({ ...r, __seq: i + 1 })),
  emptyTip: '没有可打印的数据',
})

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

// ═══ 导出（CSV，按当前筛选全量） ═══
async function handleExport() {
  exporting.value = true
  try {
    const params = buildParams()
    delete params.pageNum
    delete params.pageSize
    const res: any = await hrLeaveRequestApi.page({ ...params, pageNum: 1, pageSize: 10000 })
    const rows: HrLeaveRequest[] = res?.records || []
    if (!rows.length) {
      message.warning('没有可导出的数据')
      return
    }
    const header = ['工号', '姓名', '部门', '请假类型', '开始日期', '结束日期', '请假天数', '状态', '审批人', '审批意见', '审批时间', '请假原因']
    const lines = rows.map(r => [
      r.employeeNo, r.employeeName, r.deptName,
      LEAVE_TYPE_MAP[r.leaveType] || r.leaveType || '',
      formatDate(r.startDate), formatDate(r.endDate), r.days,
      LEAVE_STATUS_MAP[r.status]?.text || '',
      r.approveName || r.approveId || '', r.approveComment, formatDateTime(r.approveTime), r.reason,
    ].map(cell => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(','))
    const csv = `\uFEFF${header.join(',')}\n${lines.join('\n')}`
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `请假申请_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success(`导出成功，共 ${rows.length} 条`)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══ 页面配置 ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'employeeId', label: '员工', visible: true },
  { key: 'leaveType', label: '请假类型', visible: true },
  { key: 'status', label: '申请状态', visible: true },
  { key: 'deptId', label: '部门', visible: true },
  { key: 'startDate', label: '请假日期', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'create', label: '提交请假', enabled: true },
  { key: 'quota', label: '假期额度', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]

const queryFields = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtons = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))
const showPageConfig = ref(false)

function isQueryVisible(key: string): boolean {
  const hit = queryFields.value.find(f => f.key === key)
  return hit ? hit.visible : false
}

function isButtonEnabled(key: string): boolean {
  const hit = functionButtons.value.find(b => b.key === key)
  return hit ? hit.enabled : false
}

function handlePageConfigChange(config: { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }) {
  if (config?.queryFields) queryFields.value = config.queryFields.map(f => ({ ...f }))
  if (config?.functionButtons) functionButtons.value = config.functionButtons.map(b => ({ ...b }))
}

function handleError(error: Error) {
  console.error('[请假管理] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadEmployees()
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
/* 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.days-cell { font-weight: 600; }
.quota-toolbar { display: flex; align-items: center; justify-content: flex-start; margin-bottom: 8px; }
.quota-label { font-size: 13px; color: #666; }
.quota-balance { margin-top: 16px; padding-top: 12px; border-top: 1px dashed #e8e8e8; }
.quota-balance__head { font-size: 13px; font-weight: 600; margin-bottom: 8px; }

.btn-add { background: #ff6b35 !important; border-color: #ff6b35 !important; }
.btn-add:hover { background: #e55a2b !important; border-color: #e55a2b !important; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
