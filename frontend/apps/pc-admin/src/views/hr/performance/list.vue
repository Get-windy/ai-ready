<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        绩效考核（人力资源 → 薪资管理 → 绩效考核，菜单 90005）
        · 路线 A：ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillDetailTable
        · 本页无分类树（规格 §2：目标无左分类树），全部筛选走查询区
        · 查询区受页面配置控制；列配置齿轮在数据表表头 rowNo 列（个人 / 全局）
        · 工具栏：新建考核 ｜ 页面配置 / 刷新 / 打印(F8) / 导出
        · 行内：修改 / 确认 / 删除，三者均要求「未确认」（status !== 2）
        · 表单：弹窗 + 分区卡片 + 两列栅格（考核单无独立表单页，故未走 BillFormPage）
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新建考核 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('add')"
            type="primary"
            size="small"
            class="btn-add"
            @click="handleAdd"
          >
            <PlusOutlined /> 新建考核
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧：页面配置 / 刷新 / 打印 / 导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
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

        <!-- ═══ 查询区（横向自适应网格；显隐受页面配置控制） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div
                v-if="isQueryVisible('employeeId')"
                class="search-item"
              >
                <span class="search-label">员工</span>
                <a-select
                  v-model:value="searchForm.employeeId"
                  placeholder="全部员工"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="employeeOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('reviewPeriod')"
                class="search-item"
              >
                <span class="search-label">考核周期</span>
                <a-input
                  v-model:value="searchForm.reviewPeriod"
                  placeholder="如：2026-09 / 2026-Q3"
                  size="small"
                  style="width: 170px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('reviewType')"
                class="search-item"
              >
                <span class="search-label">考核类型</span>
                <a-select
                  v-model:value="searchForm.reviewType"
                  placeholder="全部类型"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="PERFORMANCE_TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('level')"
                class="search-item"
              >
                <span class="search-label">考核等级</span>
                <a-select
                  v-model:value="searchForm.level"
                  placeholder="全部等级"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  :options="levelOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('status')"
                class="search-item"
              >
                <span class="search-label">状态</span>
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
                v-if="isQueryVisible('deptId')"
                class="search-item"
              >
                <span class="search-label">部门</span>
                <a-select
                  v-model:value="searchForm.deptId"
                  placeholder="全部部门"
                  size="small"
                  style="width: 160px"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="deptOptions"
                  @change="handleSearch"
                />
              </div>
              <div class="search-actions">
                <a-button
                  type="primary"
                  size="small"
                  @click="handleSearch"
                >
                  查询
                </a-button>
                <a-button
                  size="small"
                  style="margin-left: 8px"
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
              row-key="id"
              storage-key="hr-performance-table-columns"
              global-config-key="hr-performance-table-columns"
            >
              <template #employeeNameCell="{ record, column }">
                <a
                  v-if="!record.__ghost && canEdit(record)"
                  class="cell-link"
                  @click="handleEdit(record)"
                >{{ record[column.key] || '-' }}</a>
                <span v-else-if="!record.__ghost">{{ record[column.key] || '-' }}</span>
              </template>

              <template #reviewTypeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ typeText(record[column.key]) }}</span>
              </template>

              <template #scoreCell="{ record, column }">
                <span
                  v-if="!record.__ghost"
                  :class="['score-value', scoreClass(record[column.key])]"
                >{{ formatScore(record[column.key]) }}</span>
              </template>

              <template #levelCell="{ record, column }">
                <a-tag
                  v-if="!record.__ghost && record[column.key]"
                  :color="levelColor(record[column.key])"
                >
                  {{ levelText(record[column.key]) }}
                </a-tag>
                <span v-else-if="!record.__ghost">-</span>
              </template>

              <template #statusCell="{ record, column }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="statusColor(record[column.key])"
                >
                  {{ statusText(record[column.key]) }}
                </a-tag>
              </template>

              <template #dateTimeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ formatDateTime(record[column.key]) }}</span>
              </template>

              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    v-if="canEdit(record)"
                    type="link"
                    size="small"
                    @click="handleEdit(record)"
                  >
                    修改
                  </a-button>
                  <a-button
                    v-if="canConfirm(record)"
                    type="link"
                    size="small"
                    @click="handleConfirm(record)"
                  >
                    确认
                  </a-button>
                  <a-button
                    v-if="canDelete(record)"
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

      <!-- ═══ 页面配置（查询条件显隐 + 功能按钮启用） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="hr-performance-page-config"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 新建 / 修改考核（弹窗 + 分区卡片 + 两列栅格） ═══ -->
      <a-modal
        v-model:open="formOpen"
        :title="formState.id ? '修改考核' : '新建考核'"
        :width="760"
        :mask-closable="false"
        :confirm-loading="saving"
        ok-text="保存"
        cancel-text="取消"
        @ok="handleSave"
      >
        <a-form
          ref="formRef"
          :model="formState"
          :rules="rules"
          :label-col="{ span: 7 }"
          :wrapper-col="{ span: 16 }"
          size="small"
        >
          <FormSection
            title="考核对象与周期"
            tip="考核评分按三维度加权口径由考核人填写；修改时被考核员工不可变更"
          >
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item
                  label="被考核员工"
                  name="employeeId"
                >
                  <a-select
                    v-model:value="formState.employeeId"
                    placeholder="请选择员工"
                    :disabled="!!formState.id"
                    show-search
                    option-filter-prop="label"
                    :options="employeeOptions"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item
                  label="考核周期"
                  name="reviewPeriod"
                >
                  <a-input
                    v-model:value="formState.reviewPeriod"
                    placeholder="如：2026-09 / 2026-Q3 / 2026"
                    :maxlength="20"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="考核类型">
                  <a-select
                    v-model:value="formState.reviewType"
                    :options="PERFORMANCE_TYPE_OPTIONS"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>

          <FormSection
            title="评分与等级"
            tip="评分 0 ~ 100；等级可选，留空表示不评级"
          >
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item
                  label="考核评分"
                  name="score"
                >
                  <a-input-number
                    v-model:value="formState.score"
                    :min="0"
                    :max="100"
                    :precision="2"
                    style="width: 100%"
                    placeholder="0 ~ 100"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="考核等级">
                  <a-select
                    v-model:value="formState.level"
                    placeholder="S / A / B / C / D"
                    allow-clear
                    :options="levelOptions"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="工作态度评分">
                  <a-input-number
                    v-model:value="formState.attitudeScore"
                    :min="0"
                    :max="100"
                    :precision="2"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="工作能力评分">
                  <a-input-number
                    v-model:value="formState.abilityScore"
                    :min="0"
                    :max="100"
                    :precision="2"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="工作业绩评分">
                  <a-input-number
                    v-model:value="formState.achievementScore"
                    :min="0"
                    :max="100"
                    :precision="2"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="绩效系数">
                  <a-input-number
                    v-model:value="formState.performanceCoefficient"
                    :min="0"
                    :precision="2"
                    :step="0.1"
                    style="width: 100%"
                  />
                  <div class="form-hint">
                    用于薪资侧绩效工资联动：绩效工资 = 绩效基数 × 本系数
                  </div>
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>

          <FormSection title="结论与备注">
            <a-row :gutter="24">
              <a-col :span="24">
                <a-form-item label="综合评语">
                  <a-textarea
                    v-model:value="formState.comment"
                    :rows="2"
                    :maxlength="500"
                    placeholder="本次考核的整体评价"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item label="备注">
                  <a-textarea
                    v-model:value="formState.remark"
                    :rows="2"
                    :maxlength="500"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
// 缺口：后端 HrController 未提供以下端点，故本页不实现——
//   · 考核方案 / 模板与指标权重配置（template）
//   · 按周期批量建单 / 发单（batch）
//   · 员工自评（self-review）
//   · 驳回重评（reject）
//   · 校准与强制分布（calibrate）
//   · 我的绩效员工自助（my）
// 另：绩效系数仅落库，"绩效工资 = 绩效基数 × 系数" 的薪资侧联动后端尚未接线。

import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { QueryFieldSetting, FunctionButtonSetting } from '@/components/PageConfigPanel/index.vue'
import FormSection from '@/components/FormSection/index.vue'
import { departmentApi } from '@/api/department'
import {
  hrEmployeeApi,
  hrPerformanceApi,
  PERFORMANCE_LEVEL_MAP,
  PERFORMANCE_STATUS_MAP,
  PERFORMANCE_TYPE_MAP,
  PERFORMANCE_TYPE_OPTIONS,
  type HrPerformance,
} from '@/api/hr'

defineOptions({ name: 'HrPerformanceList' })

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const saving = ref(false)
const tableData = ref<HrPerformance[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 下拉选项 ═══
const levelOptions = Object.entries(PERFORMANCE_LEVEL_MAP).map(([k, v]) => ({ label: `${k} ${v.text}`, value: k }))
const statusOptions = Object.entries(PERFORMANCE_STATUS_MAP).map(([k, v]) => ({ label: v.text, value: Number(k) }))

const employeeOptions = ref<{ label: string; value: number | string }[]>([])
const deptOptions = ref<{ label: string; value: number | string }[]>([])

/** 员工下拉：取在职(1)/试用(2)，后端 status 只能单值筛选，故前端过滤 */
async function loadEmployees() {
  try {
    const res: any = await hrEmployeeApi.page({ pageNum: 1, pageSize: 500 })
    const list: any[] = res?.records || []
    employeeOptions.value = list
      .filter(e => e.status === 1 || e.status === 2)
      .map(e => ({
        label: e.employeeNo ? `[${e.employeeNo}] ${e.employeeName}` : e.employeeName,
        value: e.id,
      }))
  } catch (error) {
    console.warn('[绩效考核] 员工下拉加载失败', error)
  }
}

/** 部门下拉：取部门树拍平（HR 只读引用，不在此维护部门主数据） */
async function loadDepts() {
  try {
    const res: any = await departmentApi.getTree()
    const list: any[] = Array.isArray(res) ? res : res?.data || []
    const out: { label: string; value: number | string }[] = []
    const walk = (nodes: any[], depth: number) => {
      for (const n of nodes || []) {
        out.push({ label: `${'　'.repeat(depth)}${n.departmentName || n.name || ''}`, value: n.id })
        if (n.children?.length) walk(n.children, depth + 1)
      }
    }
    walk(list, 0)
    deptOptions.value = out
  } catch (error) {
    console.warn('[绩效考核] 部门下拉加载失败', error)
  }
}

// ═══ 数据表列（默认可见 14 列 + 序号 + 操作；其余默认隐藏） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 150, fixed: 'left' },
  { key: 'employeeNo', title: '员工工号', type: 'input', width: 130 },
  { key: 'employeeName', title: '员工姓名', type: 'slot', slotName: 'employeeNameCell', width: 110 },
  { key: 'deptName', title: '部门', type: 'input', width: 130 },
  { key: 'reviewPeriod', title: '考核周期', type: 'input', width: 110 },
  { key: 'reviewType', title: '考核类型', type: 'slot', slotName: 'reviewTypeCell', width: 100 },
  { key: 'score', title: '考核评分', type: 'slot', slotName: 'scoreCell', width: 100, align: 'right' },
  { key: 'level', title: '考核等级', type: 'slot', slotName: 'levelCell', width: 100, align: 'center' },
  { key: 'attitudeScore', title: '工作态度', type: 'input', width: 100, align: 'right', formatter: formatScore },
  { key: 'abilityScore', title: '工作能力', type: 'input', width: 100, align: 'right', formatter: formatScore },
  { key: 'achievementScore', title: '工作业绩', type: 'input', width: 100, align: 'right', formatter: formatScore },
  { key: 'performanceCoefficient', title: '绩效系数', type: 'input', width: 100, align: 'right', formatter: formatCoefficient },
  { key: 'reviewerName', title: '考核人', type: 'input', width: 110 },
  { key: 'reviewTime', title: '考核时间', type: 'slot', slotName: 'dateTimeCell', width: 150 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 100, align: 'center' },
  // ── 默认隐藏（表头齿轮可开启） ──
  { key: 'comment', title: '综合评语', type: 'input', width: 240, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 200, defaultHidden: true },
  { key: 'createTime', title: '创建时间', type: 'slot', slotName: 'dateTimeCell', width: 160, defaultHidden: true },
  { key: 'updateTime', title: '更新时间', type: 'slot', slotName: 'dateTimeCell', width: 160, defaultHidden: true },
  { key: 'createBy', title: '创建人', type: 'input', width: 100, defaultHidden: true },
  { key: 'updateBy', title: '更新人', type: 'input', width: 100, defaultHidden: true },
]

// ═══ 查询条件 ═══
const searchForm = reactive({
  employeeId: undefined as number | string | undefined,
  reviewPeriod: '',
  reviewType: undefined as string | undefined,
  level: undefined as string | undefined,
  status: undefined as number | undefined,
  deptId: undefined as number | string | undefined,
})

function buildParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (searchForm.employeeId !== undefined && searchForm.employeeId !== null) params.employeeId = searchForm.employeeId
  if (searchForm.reviewPeriod) params.reviewPeriod = searchForm.reviewPeriod.trim()
  if (searchForm.reviewType) params.reviewType = searchForm.reviewType
  if (searchForm.level) params.level = searchForm.level
  if (searchForm.status !== undefined && searchForm.status !== null) params.status = searchForm.status
  if (searchForm.deptId !== undefined && searchForm.deptId !== null) params.deptId = searchForm.deptId
  return params
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await hrPerformanceApi.page(buildParams())
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[绩效考核] 加载列表失败', error)
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
  searchForm.reviewPeriod = ''
  searchForm.reviewType = undefined
  searchForm.level = undefined
  searchForm.status = undefined
  searchForm.deptId = undefined
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 展示辅助 ═══
function textOf(map: Record<string, any>, value: any): string {
  if (value === null || value === undefined || value === '') return '-'
  const hit = map[value]
  if (!hit) return String(value)
  return typeof hit === 'string' ? hit : hit.text
}

function typeText(value: any): string {
  return textOf(PERFORMANCE_TYPE_MAP, value)
}

function levelText(value: any): string {
  return textOf(PERFORMANCE_LEVEL_MAP, value)
}

function levelColor(value: any): string {
  return PERFORMANCE_LEVEL_MAP[value]?.color || 'default'
}

function statusText(value: any): string {
  return PERFORMANCE_STATUS_MAP[Number(value)]?.text || '-'
}

function statusColor(value: any): string {
  return PERFORMANCE_STATUS_MAP[Number(value)]?.color || 'default'
}

function formatScore(value: any): string {
  if (value === null || value === undefined || value === '') return '-'
  const n = Number(value)
  return Number.isNaN(n) ? '-' : n.toFixed(2)
}

function formatCoefficient(value: any): string {
  if (value === null || value === undefined || value === '') return '1.00'
  const n = Number(value)
  return Number.isNaN(n) ? '-' : n.toFixed(2)
}

/** 评分着色：>=90 绿 / >=70 蓝 / 其余橙（对齐对标页口径） */
function scoreClass(value: any): string {
  if (value === null || value === undefined || value === '') return ''
  const n = Number(value)
  if (Number.isNaN(n)) return ''
  if (n >= 90) return 'score-high'
  if (n >= 70) return 'score-mid'
  return 'score-low'
}

function formatDateTime(val: string | null | undefined): string {
  if (!val) return '-'
  return String(val).replace('T', ' ').slice(0, 16)
}

// ═══ 行内操作（后端规则：已确认不可修改/删除；已确认不可重复确认） ═══
function isConfirmed(record: any): boolean {
  return Number(record?.status) === 2
}
function canEdit(record: any): boolean {
  return !isConfirmed(record)
}
function canConfirm(record: any): boolean {
  return !isConfirmed(record)
}
function canDelete(record: any): boolean {
  return !isConfirmed(record)
}

function handleConfirm(record: HrPerformance) {
  Modal.confirm({
    title: '确认考核',
    content: `确定要确认「${record.employeeName || ''}」${record.reviewPeriod || ''} 的考核结果吗？确认后不可再修改或删除。`,
    okText: '确认考核',
    onOk: async () => {
      try {
        await hrPerformanceApi.confirm(record.id)
        message.success('确认成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '确认失败')
      }
    },
  })
}

function handleDelete(record: HrPerformance) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除「${record.employeeName || ''}」${record.reviewPeriod || ''} 的考核记录吗？`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await hrPerformanceApi.remove(record.id)
        message.success('删除成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 新建 / 修改表单 ═══
const formOpen = ref(false)
const formRef = ref()

const emptyForm = () => ({
  id: null as number | string | null,
  employeeId: undefined as number | string | undefined,
  reviewPeriod: '',
  reviewType: 'MONTHLY' as string,
  score: undefined as number | undefined,
  level: undefined as string | undefined,
  attitudeScore: undefined as number | undefined,
  abilityScore: undefined as number | undefined,
  achievementScore: undefined as number | undefined,
  performanceCoefficient: 1.0 as number,
  comment: '',
  remark: '',
})
const formState = reactive(emptyForm())

const rules = {
  employeeId: [{ required: true, message: '请选择被考核员工', trigger: 'change' }],
  reviewPeriod: [{ required: true, message: '请填写考核周期', trigger: 'blur' }],
  score: [
    { required: true, message: '请填写考核评分', trigger: 'change' },
    {
      validator: (_rule: any, value: any) => {
        if (value === undefined || value === null || value === '') return Promise.resolve()
        const n = Number(value)
        if (Number.isNaN(n) || n < 0 || n > 100) return Promise.reject('考核评分必须在 0 ~ 100 之间')
        return Promise.resolve()
      },
      trigger: 'change',
    },
  ],
}

function resetForm() {
  Object.assign(formState, emptyForm())
}

/** 后端 Decimal 字段可能以字符串下发，转数字后再交给 a-input-number */
function toNumberOrUndefined(value: any): number | undefined {
  if (value === null || value === undefined || value === '') return undefined
  const n = Number(value)
  return Number.isNaN(n) ? undefined : n
}

function handleAdd() {
  resetForm()
  formOpen.value = true
}

async function handleEdit(record: HrPerformance) {
  resetForm()
  formOpen.value = true
  try {
    const detail: any = await hrPerformanceApi.getById(record.id)
    if (!detail) return
    formState.id = detail.id
    formState.employeeId = detail.employeeId
    formState.reviewPeriod = detail.reviewPeriod || ''
    formState.reviewType = detail.reviewType || 'MONTHLY'
    // Decimal/Long 可能被序列化为字符串，统一转 Number 后再回填数字控件
    formState.score = toNumberOrUndefined(detail.score)
    formState.level = detail.level || undefined
    formState.attitudeScore = toNumberOrUndefined(detail.attitudeScore)
    formState.abilityScore = toNumberOrUndefined(detail.abilityScore)
    formState.achievementScore = toNumberOrUndefined(detail.achievementScore)
    formState.performanceCoefficient = toNumberOrUndefined(detail.performanceCoefficient) ?? 1.0
    formState.comment = detail.comment || ''
    formState.remark = detail.remark || ''
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载考核详情失败')
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  const payload = {
    employeeId: formState.employeeId ?? null,
    reviewPeriod: formState.reviewPeriod.trim(),
    reviewType: formState.reviewType || null,
    score: formState.score ?? null,
    level: formState.level || null,
    attitudeScore: formState.attitudeScore ?? null,
    abilityScore: formState.abilityScore ?? null,
    achievementScore: formState.achievementScore ?? null,
    performanceCoefficient: formState.performanceCoefficient ?? 1.0,
    comment: formState.comment || null,
    remark: formState.remark || null,
  }
  saving.value = true
  try {
    if (formState.id) {
      // 后端 updateReview 会忽略 employeeId，被考核员工不可变更
      await hrPerformanceApi.update(formState.id, payload)
      message.success('修改成功')
    } else {
      await hrPerformanceApi.submit(payload)
      message.success('新建成功')
    }
    formOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 打印(F8)：绩效考核表 ═══
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
      <td>${escapeHtml(r.reviewPeriod || '')}</td>
      <td>${escapeHtml(typeText(r.reviewType))}</td>
      <td>${escapeHtml(formatScore(r.score))}</td>
      <td>${escapeHtml(levelText(r.level))}</td>
      <td>${escapeHtml(formatScore(r.attitudeScore))}</td>
      <td>${escapeHtml(formatScore(r.abilityScore))}</td>
      <td>${escapeHtml(formatScore(r.achievementScore))}</td>
      <td>${escapeHtml(formatCoefficient(r.performanceCoefficient))}</td>
      <td>${escapeHtml(r.reviewerName || '')}</td>
      <td>${escapeHtml(formatDateTime(r.reviewTime))}</td>
      <td>${escapeHtml(statusText(r.status))}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>绩效考核表</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>绩效考核表</h2>
    <div class="meta">
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr>
        <th>#</th><th>工号</th><th>姓名</th><th>部门</th><th>考核周期</th><th>考核类型</th>
        <th>考核评分</th><th>考核等级</th><th>工作态度</th><th>工作能力</th><th>工作业绩</th>
        <th>绩效系数</th><th>考核人</th><th>考核时间</th><th>状态</th>
      </tr></thead>
      <tbody>${body}</tbody>
    </table></body></html>`
  const win = window.open('', '_blank', 'width=1200,height=700')
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
    const res: any = await hrPerformanceApi.page({ ...params, pageNum: 1, pageSize: 10000 })
    const rows: HrPerformance[] = res?.records || []
    if (!rows.length) {
      message.warning('没有可导出的数据')
      return
    }
    const header = ['员工工号', '员工姓名', '部门', '考核周期', '考核类型', '考核评分', '考核等级',
      '工作态度', '工作能力', '工作业绩', '绩效系数', '考核人', '考核时间', '状态', '综合评语', '备注']
    const lines = rows.map(r => [
      r.employeeNo, r.employeeName, r.deptName, r.reviewPeriod, typeText(r.reviewType),
      formatScore(r.score), levelText(r.level), formatScore(r.attitudeScore),
      formatScore(r.abilityScore), formatScore(r.achievementScore),
      formatCoefficient(r.performanceCoefficient), r.reviewerName, formatDateTime(r.reviewTime),
      statusText(r.status), r.comment, r.remark,
    ].map(cell => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(','))
    const csv = `\uFEFF${header.join(',')}\n${lines.join('\n')}`
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `绩效考核_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
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
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'employeeId', label: '员工', visible: true },
  { key: 'reviewPeriod', label: '考核周期', visible: true },
  { key: 'reviewType', label: '考核类型', visible: true },
  { key: 'level', label: '考核等级', visible: true },
  { key: 'status', label: '状态', visible: true },
  { key: 'deptId', label: '部门', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新建考核', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]

const queryFields = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtons = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))
const showPageConfig = ref(false)

function isQueryVisible(key: string): boolean {
  const hit = queryFields.value.find(f => f.key === key)
  return hit ? hit.visible : true
}

function isButtonEnabled(key: string): boolean {
  const hit = functionButtons.value.find(b => b.key === key)
  return hit ? hit.enabled : true
}

function handlePageConfigChange(config: { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }) {
  if (config?.queryFields) queryFields.value = config.queryFields.map(f => ({ ...f }))
  if (config?.functionButtons) functionButtons.value = config.functionButtons.map(b => ({ ...b }))
}

function handleError(error: Error) {
  console.error('[绩效考核] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadEmployees()
  loadDepts()
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
/* 查询区（插槽内容样式必须自备：scoped 不作用于布局组件内的插槽内容） */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.search-actions { margin-left: auto; }

/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }

/* 评分着色：>=90 绿 / >=70 蓝 / 其余橙 */
.score-value { font-weight: 600; }
.score-high { color: #52c41a; }
.score-mid { color: #1890ff; }
.score-low { color: #fa8c16; }

.form-hint { margin-top: 4px; font-size: 12px; color: #999; line-height: 1.4; }

.btn-add { background: #ff6b35 !important; border-color: #ff6b35 !important; }
.btn-add:hover { background: #e55a2b !important; border-color: #e55a2b !important; }

/* ── 紧凑尺寸覆盖：28px 输入框 ── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
:deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
:deep(.ant-input-number-sm input) { height: 26px; }
</style>
