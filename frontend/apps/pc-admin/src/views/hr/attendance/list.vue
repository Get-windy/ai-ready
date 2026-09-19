<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        考勤记录（人力资源 → 考勤管理 → 考勤记录）
        · 无左分类树（本页无分类维度），单视图，display_mode = 0，无独立表单页
        · 列配置齿轮在数据表表头 rowNo 列（个人配置 / 全局配置）
        · 工具栏：新增考勤记录（补卡/更正） ｜ 考勤规则 ｜ 页面配置 / 刷新 / 打印(F8) / 导出
        · 查询区（受页面配置控制）：月份 · 员工 · 部门 · 考勤状态 · 考勤日期区间
        · 行内：修改 / 重算（迟到·早退·工时）/ 删除
        · 表单：弹窗（考勤记录是系统流水，非人工单据，不用 BillFormPage）
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
              v-if="isButtonEnabled('add')"
              type="primary"
              size="small"
              class="btn-add"
              @click="handleAdd"
            >
              <PlusOutlined /> 新增考勤记录
            </a-button>
            <a-button
              v-if="isButtonEnabled('rule')"
              size="small"
              @click="handleOpenRule"
            >
              <ClockCircleOutlined /> 考勤规则
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

        <!-- ═══ 查询区（字段显隐由页面配置控制） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div
                v-if="isQueryVisible('month')"
                class="search-item"
              >
                <span class="search-label">月份</span>
                <a-month-picker
                  v-model:value="searchForm.month"
                  placeholder="选择月份"
                  size="small"
                  style="width: 140px"
                  value-format="YYYY-MM"
                  allow-clear
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('employeeId')"
                class="search-item"
              >
                <span class="search-label">员工</span>
                <a-select
                  v-model:value="searchForm.employeeId"
                  placeholder="全部员工"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="employeeOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('deptId')"
                class="search-item"
              >
                <span class="search-label">部门</span>
                <a-tree-select
                  v-model:value="searchForm.deptId"
                  placeholder="全部部门"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  tree-default-expand-all
                  :tree-data="deptTreeData"
                  :field-names="{ label: 'departmentName', value: 'id', children: 'children' }"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('status')"
                class="search-item"
              >
                <span class="search-label">考勤状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="statusOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('attendanceDate')"
                class="search-item"
              >
                <span class="search-label">考勤日期</span>
                <a-range-picker
                  v-model:value="searchForm.dateRange"
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
              storage-key="hr-attendance-table-columns"
              global-config-key="hr-attendance-table-columns"
            >
              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="statusOf(record.status)?.color || 'default'"
                >
                  {{ statusOf(record.status)?.text || '-' }}
                </a-tag>
              </template>

              <template #dateCell="{ record, column }">
                <span v-if="!record.__ghost">{{ formatDate(record[column.key]) }}</span>
              </template>

              <template #dateTimeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ formatDateTime(record[column.key]) }}</span>
              </template>

              <template #minutesCell="{ record, column }">
                <span
                  v-if="!record.__ghost && minutesOf(record[column.key]) > 0"
                  class="minutes-late"
                >{{ minutesOf(record[column.key]) }}分钟</span>
                <span v-else-if="!record.__ghost">-</span>
              </template>

              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
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
                    @click="handleRecalculate(record)"
                  >
                    重算
                  </a-button>
                  <a-button
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

      <!-- ═══ 考勤记录弹窗（新增补卡/更正 · 修改） ═══ -->
      <a-modal
        v-model:open="formOpen"
        :title="formState.id ? '修改考勤记录' : '新增考勤记录（补卡 / 更正）'"
        :width="620"
        :mask-closable="false"
        :confirm-loading="saving"
        @ok="handleSave"
      >
        <a-form
          ref="formRef"
          :model="formState"
          :rules="rules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          size="small"
        >
          <a-form-item
            label="员工"
            name="employeeId"
          >
            <a-select
              v-model:value="formState.employeeId"
              placeholder="请选择员工"
              allow-clear
              show-search
              option-filter-prop="label"
              :options="employeeOptions"
            />
          </a-form-item>
          <a-form-item
            label="考勤日期"
            name="attendanceDate"
          >
            <a-date-picker
              v-model:value="formState.attendanceDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
            />
          </a-form-item>
          <a-form-item label="上班打卡">
            <a-time-picker
              v-model:value="formState.clockInTime"
              style="width: 100%"
              value-format="HH:mm:ss"
              placeholder="补上班卡时间"
            />
          </a-form-item>
          <a-form-item label="下班打卡">
            <a-time-picker
              v-model:value="formState.clockOutTime"
              style="width: 100%"
              value-format="HH:mm:ss"
              placeholder="补下班卡时间"
            />
          </a-form-item>
          <a-form-item label="考勤状态">
            <a-select
              v-model:value="formState.status"
              :options="statusOptions"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea
              v-model:value="formState.remark"
              :rows="2"
              :maxlength="200"
              placeholder="补卡 / 更正原因"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 考勤规则弹窗（上下班时间 / 迟到早退阈值，读写 /hr/attendance/rule） ═══ -->
      <a-modal
        v-model:open="ruleOpen"
        title="考勤规则"
        :width="560"
        :mask-closable="false"
        :confirm-loading="ruleSaving"
        @ok="handleSaveRule"
      >
        <a-form
          :model="ruleForm"
          :label-col="{ span: 8 }"
          :wrapper-col="{ span: 14 }"
          size="small"
        >
          <a-form-item label="标准上班时间">
            <a-time-picker
              v-model:value="ruleForm.workStartTime"
              style="width: 100%"
              value-format="HH:mm:ss"
            />
          </a-form-item>
          <a-form-item label="标准下班时间">
            <a-time-picker
              v-model:value="ruleForm.workEndTime"
              style="width: 100%"
              value-format="HH:mm:ss"
            />
          </a-form-item>
          <a-form-item label="迟到宽限(分钟)">
            <a-input-number
              v-model:value="ruleForm.lateGraceMinutes"
              :min="0"
              :precision="0"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="早退宽限(分钟)">
            <a-input-number
              v-model:value="ruleForm.earlyGraceMinutes"
              :min="0"
              :precision="0"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="标准工时(小时)">
            <a-input-number
              v-model:value="ruleForm.standardWorkHours"
              :min="0"
              :precision="1"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="自动标记缺勤">
            <a-switch
              v-model:checked="autoAbsentChecked"
              checked-children="是"
              un-checked-children="否"
            />
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea
              v-model:value="ruleForm.remark"
              :rows="2"
              :maxlength="200"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="hr-attendance-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
/**
 * 后端能力缺口（本页据实不实现，勿误以为遗漏）：
 * · 缺口：无考勤机/文件导入端点（hrAttendanceApi 中无 import）→ 工具栏「导入考勤机数据」未实现
 * · 缺口：无考勤月结与锁定端点（无 month-close）→ 工具栏「考勤月结」未实现
 * · 缺口：无工作日历（节假日/调休）端点 → 未提供工作日历配置
 * · 缺口：无补卡申请与审批端点（无 correct / approve）→ 补卡只能以「新增考勤记录」直接补录，无审批闭环
 * · 缺口：无批量删除按钮需求外的批量操作；批量删除接口存在但本页不提供入口
 * · 缺口：无 sys_user ↔ hr_employee 映射（SecurityUtils 取到的是 sys_user.id，与 hr_employee.id 不同套）
 *   → 工具栏不提供「签到 / 签退」按钮：接口要求 employeeId，硬编码或猜值都会落到员工 0 的脏数据上
 *     （clock-out 查不到当天记录时后端还会静默返回成功）。待后端按当前登录人解析员工后再补该入口。
 * · 缺口：考勤规则端点存在（GET/PUT /hr/attendance/rule），但班次表、打卡来源、定位列均无 → 规则仅覆盖上下班时间与迟到早退阈值
 */
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
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
import { departmentApi } from '@/api/department'
import {
  hrAttendanceApi,
  hrEmployeeApi,
  ATTENDANCE_STATUS_MAP,
  type HrAttendance,
  type HrAttendanceRule,
} from '@/api/hr'

defineOptions({ name: 'HrAttendanceList' })

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const saving = ref(false)
const ruleSaving = ref(false)
const tableData = ref<HrAttendance[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// 考勤状态下拉（值域 NORMAL/LATE/EARLY/ABSENT/LEAVE，映射真源在 api/hr/index.ts）
const statusOptions = Object.entries(ATTENDANCE_STATUS_MAP).map(([value, item]) => ({
  label: item.text,
  value,
}))

// ═══ 数据表列（默认显示 12 列含序号与操作；备注/创建时间/更新时间默认隐藏） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 140, fixed: 'left' },
  { key: 'employeeNo', title: '员工工号', type: 'input', width: 130 },
  { key: 'employeeName', title: '员工姓名', type: 'input', width: 110 },
  { key: 'deptName', title: '部门', type: 'input', width: 130 },
  { key: 'attendanceDate', title: '考勤日期', type: 'slot', slotName: 'dateCell', width: 120 },
  { key: 'clockInTime', title: '上班打卡', type: 'input', width: 110 },
  { key: 'clockOutTime', title: '下班打卡', type: 'input', width: 110 },
  { key: 'status', title: '考勤状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'lateMinutes', title: '迟到(分钟)', type: 'slot', slotName: 'minutesCell', width: 100 },
  { key: 'earlyMinutes', title: '早退(分钟)', type: 'slot', slotName: 'minutesCell', width: 100 },
  { key: 'workHours', title: '工时(小时)', type: 'input', width: 100 },
  // ── 默认隐藏 ──
  { key: 'remark', title: '备注', type: 'input', width: 200, defaultHidden: true },
  { key: 'createTime', title: '创建时间', type: 'slot', slotName: 'dateTimeCell', width: 150, defaultHidden: true },
  { key: 'updateTime', title: '更新时间', type: 'slot', slotName: 'dateTimeCell', width: 150, defaultHidden: true },
]

// ═══ 查询条件 ═══
const searchForm = reactive({
  month: undefined as string | undefined,
  employeeId: undefined as number | string | undefined,
  deptId: undefined as number | string | undefined,
  status: undefined as string | undefined,
  dateRange: undefined as [string, string] | undefined,
})

// ═══ 部门下拉（数据源 sys_department，HR 只读引用） ═══
const deptTreeData = ref<any[]>([])

async function loadDeptTree() {
  try {
    const res: any = await departmentApi.getTree()
    const list: any[] = Array.isArray(res) ? res : res?.data || []
    deptTreeData.value = list.map((d: any) => ({
      id: d.id,
      departmentName: d.departmentName,
      children: d.children,
    }))
  } catch (error) {
    console.warn('[考勤记录] 部门树加载失败', error)
    deptTreeData.value = []
  }
}

// ═══ 员工下拉 ═══
const employeeOptions = ref<{ label: string; value: number | string }[]>([])

async function loadEmployees() {
  try {
    const res: any = await hrEmployeeApi.page({ pageNum: 1, pageSize: 500 })
    const list: any[] = res?.records || []
    employeeOptions.value = list.map((e: any) => ({
      label: e.employeeNo ? `[${e.employeeNo}] ${e.employeeName}` : e.employeeName,
      value: e.id,
    }))
  } catch (error) {
    console.warn('[考勤记录] 员工列表加载失败', error)
    employeeOptions.value = []
  }
}

// ═══ 数据加载 ═══
function buildParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (searchForm.month) params.month = searchForm.month
  if (searchForm.employeeId !== undefined && searchForm.employeeId !== null) params.employeeId = searchForm.employeeId
  if (searchForm.deptId !== undefined && searchForm.deptId !== null) params.deptId = searchForm.deptId
  if (searchForm.status) params.status = searchForm.status
  if (searchForm.dateRange?.length === 2) {
    params.dateStart = searchForm.dateRange[0]
    params.dateEnd = searchForm.dateRange[1]
  }
  return params
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await hrAttendanceApi.page(buildParams())
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[考勤记录] 加载列表失败', error)
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
  searchForm.month = undefined
  searchForm.employeeId = undefined
  searchForm.deptId = undefined
  searchForm.status = undefined
  searchForm.dateRange = undefined
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 展示辅助 ═══
/** 状态取值：走显式判空，避免把原型链上的键当成状态值 */
function statusOf(status: any) {
  if (status === null || status === undefined || status === '') return undefined
  return ATTENDANCE_STATUS_MAP[String(status)]
}

function formatDate(val: any): string {
  if (!val) return '-'
  return String(val).slice(0, 10)
}

function formatDateTime(val: any): string {
  if (!val) return '-'
  return String(val).slice(0, 16).replace('T', ' ')
}

function minutesOf(val: any): number {
  const n = Number(val)
  return Number.isFinite(n) ? n : 0
}

// ═══ 考勤记录表单（新增补卡 / 更正 · 修改） ═══
const formOpen = ref(false)
const formRef = ref()
const emptyForm = () => ({
  id: null as number | string | null,
  employeeId: undefined as number | string | undefined,
  attendanceDate: undefined as string | undefined,
  clockInTime: undefined as string | undefined,
  clockOutTime: undefined as string | undefined,
  status: 'NORMAL' as string | undefined,
  remark: '',
})
const formState = reactive(emptyForm())

const rules = {
  employeeId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  attendanceDate: [{ required: true, message: '请选择考勤日期', trigger: 'change' }],
}

function resetForm() {
  Object.assign(formState, emptyForm())
}

function handleAdd() {
  resetForm()
  formOpen.value = true
}

async function handleEdit(record: HrAttendance) {
  resetForm()
  formOpen.value = true
  try {
    const detail: any = await hrAttendanceApi.getById(record.id)
    if (!detail) return
    formState.id = detail.id
    formState.employeeId = detail.employeeId
    formState.attendanceDate = detail.attendanceDate || undefined
    formState.clockInTime = detail.clockInTime || undefined
    formState.clockOutTime = detail.clockOutTime || undefined
    formState.status = detail.status || 'NORMAL'
    formState.remark = detail.remark || ''
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载考勤详情失败')
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  const payload = {
    employeeId: formState.employeeId,
    attendanceDate: formState.attendanceDate || null,
    clockInTime: formState.clockInTime || null,
    clockOutTime: formState.clockOutTime || null,
    status: formState.status || 'NORMAL',
    remark: formState.remark || null,
  }
  saving.value = true
  try {
    if (formState.id) {
      await hrAttendanceApi.update(formState.id, payload)
      message.success('修改成功')
    } else {
      await hrAttendanceApi.create(payload)
      message.success('新增成功')
    }
    formOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 重算（迟到 / 早退 / 工时，按后端考勤规则） ═══
function handleRecalculate(record: HrAttendance) {
  Modal.confirm({
    title: '确认重算',
    content: `确定按考勤规则重算「${record.employeeName || record.employeeId} ${formatDate(record.attendanceDate)}」的迟到/早退/工时吗？`,
    onOk: async () => {
      try {
        await hrAttendanceApi.recalculate(record.id)
        message.success('重算完成')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '重算失败')
      }
    },
  })
}

// ═══ 删除 ═══
function handleDelete(record: HrAttendance) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除「${record.employeeName || record.employeeId} ${formatDate(record.attendanceDate)}」的考勤记录吗？删除后不再出现在台账中。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await hrAttendanceApi.remove(record.id)
        message.success('删除成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 考勤规则 ═══
const ruleOpen = ref(false)
const ruleForm = reactive<HrAttendanceRule>({
  workStartTime: '09:00:00',
  workEndTime: '18:00:00',
  lateGraceMinutes: 0,
  earlyGraceMinutes: 0,
  standardWorkHours: 8,
  autoAbsent: 1,
  remark: '',
})

const autoAbsentChecked = computed({
  get: () => Number(ruleForm.autoAbsent) === 1,
  set: (v: boolean) => { ruleForm.autoAbsent = v ? 1 : 0 },
})

async function handleOpenRule() {
  ruleOpen.value = true
  try {
    const rule: any = await hrAttendanceApi.getRule()
    if (rule) {
      ruleForm.id = rule.id
      ruleForm.workStartTime = rule.workStartTime || '09:00:00'
      ruleForm.workEndTime = rule.workEndTime || '18:00:00'
      ruleForm.lateGraceMinutes = Number(rule.lateGraceMinutes) || 0
      ruleForm.earlyGraceMinutes = Number(rule.earlyGraceMinutes) || 0
      ruleForm.standardWorkHours = rule.standardWorkHours ?? 8
      ruleForm.autoAbsent = rule.autoAbsent ?? 1
      ruleForm.remark = rule.remark || ''
    }
  } catch (error: any) {
    console.warn('[考勤记录] 读取考勤规则失败', error)
    message.warning(error?.response?.data?.message || '读取考勤规则失败，已载入默认值')
  }
}

async function handleSaveRule() {
  ruleSaving.value = true
  try {
    await hrAttendanceApi.saveRule({
      id: ruleForm.id,
      workStartTime: ruleForm.workStartTime,
      workEndTime: ruleForm.workEndTime,
      lateGraceMinutes: ruleForm.lateGraceMinutes ?? 0,
      earlyGraceMinutes: ruleForm.earlyGraceMinutes ?? 0,
      standardWorkHours: ruleForm.standardWorkHours ?? 8,
      autoAbsent: ruleForm.autoAbsent ?? 1,
      remark: ruleForm.remark || undefined,
    })
    message.success('考勤规则已保存')
    ruleOpen.value = false
    // 规则变化会影响迟到/早退判定口径，提示用户重算（不自动改历史数据）
    message.info('规则已更新，可对历史记录执行「重算」以刷新迟到/早退/工时')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存考勤规则失败')
  } finally {
    ruleSaving.value = false
  }
}

// ═══ 打印(F8)：考勤台账 ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function handlePrint() {
  const rows = (tableData.value || []).filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const body = rows.map((r: any, i: number) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(r.employeeNo)}</td>
      <td>${escapeHtml(r.employeeName)}</td>
      <td>${escapeHtml(r.deptName || '')}</td>
      <td>${escapeHtml(formatDate(r.attendanceDate))}</td>
      <td>${escapeHtml(r.clockInTime || '-')}</td>
      <td>${escapeHtml(r.clockOutTime || '-')}</td>
      <td>${escapeHtml(statusOf(r.status)?.text || '-')}</td>
      <td>${escapeHtml(minutesOf(r.lateMinutes) > 0 ? `${minutesOf(r.lateMinutes)}分钟` : '-')}</td>
      <td>${escapeHtml(minutesOf(r.earlyMinutes) > 0 ? `${minutesOf(r.earlyMinutes)}分钟` : '-')}</td>
      <td>${escapeHtml(r.workHours ?? '-')}</td>
      <td>${escapeHtml(r.remark || '')}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>考勤台账</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>考勤台账</h2>
    <div class="meta">
      <span>月份：${escapeHtml(searchForm.month || '全部')}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr>
        <th>#</th><th>工号</th><th>姓名</th><th>部门</th><th>考勤日期</th><th>上班打卡</th>
        <th>下班打卡</th><th>考勤状态</th><th>迟到</th><th>早退</th><th>工时(小时)</th><th>备注</th>
      </tr></thead>
      <tbody>${body}</tbody>
    </table></body></html>`
  const win = window.open('', '_blank', 'width=1100,height=700')
  if (!win) {
    message.warning('浏览器阻止了打印窗口，请允许弹出窗口后重试')
    return
  }
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

// ═══ 导出（CSV，全量按当前筛选） ═══
async function handleExport() {
  exporting.value = true
  try {
    const params = buildParams()
    delete params.pageNum
    delete params.pageSize
    const res: any = await hrAttendanceApi.page({ ...params, pageNum: 1, pageSize: 10000 })
    const rows: HrAttendance[] = res?.records || []
    if (!rows.length) {
      message.warning('没有可导出的数据')
      return
    }
    const header = ['员工工号', '员工姓名', '部门', '考勤日期', '上班打卡', '下班打卡', '考勤状态', '迟到(分钟)', '早退(分钟)', '工时(小时)', '备注']
    const lines = rows.map(r => [
      r.employeeNo, r.employeeName, r.deptName, formatDate(r.attendanceDate),
      r.clockInTime || '', r.clockOutTime || '', statusOf(r.status)?.text || '',
      minutesOf(r.lateMinutes), minutesOf(r.earlyMinutes), r.workHours, r.remark,
    ].map(cell => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(','))
    const csv = `\uFEFF${header.join(',')}\n${lines.join('\n')}`
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `考勤台账_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
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
  { key: 'month', label: '月份', visible: true },
  { key: 'employeeId', label: '员工', visible: true },
  { key: 'deptId', label: '部门', visible: true },
  { key: 'status', label: '考勤状态', visible: true },
  { key: 'attendanceDate', label: '考勤日期', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增考勤记录', enabled: true },
  { key: 'rule', label: '考勤规则', enabled: true },
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
  console.error('[考勤记录] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadDeptTree()
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
/* 迟到 / 早退分钟数：>0 显示橙色，0 显示「-」 */
.minutes-late { color: #fa8c16; }

.btn-add { background: #ff6b35 !important; border-color: #ff6b35 !important; }
.btn-add:hover { background: #e55a2b !important; border-color: #e55a2b !important; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
