<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        拜访规划（CRM → 外勤拜访 → 拜访规划，菜单 70001）
        · CRM 为本系统独有模块（ql361 无 CRM 域），按业界生产级 CRM 的「计划-执行-检视」模型建模
        · 规格：docs/Yh-Spec/手动整理对标开发文档/crm模块/拜访规划开发文档.md
        · 路线 A：ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillDetailTable
        · 列配置齿轮在表头 rowNo 列；页面配置弹窗控制查询条件显隐 + 功能按钮启用
        · 本轮由路线 B（ARReportPage）整体重写为路线 A，并补：取消/删除返回值校验、
                   导出/打印带状态中文文案、下拉加载失败提示、创建时间列（默认隐藏）
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新建计划 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('create')"
            type="primary"
            size="small"
            class="btn-add"
            @click="openCreate"
          >
            <PlusOutlined /> 新建计划
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧：刷新 / 打印(F8) / 导出 / 页面配置 ═══ -->
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
              v-if="isButtonEnabled('printF8')"
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
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
                  :options="salesPersonOptions"
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
                  :options="statusOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('planDateRange')"
                class="search-item"
              >
                <span class="search-label">计划日期</span>
                <a-range-picker
                  v-model:value="searchForm.planDateRange"
                  size="small"
                  value-format="YYYY-MM-DD"
                  :placeholder="['开始日期', '结束日期']"
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

        <!-- ═══ 数据表 ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="crm-visit-plan-table-columns"
              global-config-key="crm-visit-plan-table-columns"
            >
              <template #customerCell="{ record }">
                <a-tooltip
                  v-if="!record.__ghost && record.customerName"
                  :title="record.customerName"
                  placement="bottom"
                >
                  <span class="cell-ellipsis">{{ record.customerName }}</span>
                </a-tooltip>
                <span v-else-if="!record.__ghost">-</span>
                <span v-else />
              </template>

              <template #planDateCell="{ record }">
                <span v-if="record.__ghost" />
                <span
                  v-else
                  :class="{ 'overdue-plan': isOverdue(record) }"
                  :title="isOverdue(record) ? '计划日期已过且仍未执行' : undefined"
                >
                  {{ record.planDate || '-' }}
                </span>
              </template>

              <template #purposeCell="{ record }">
                <a-tooltip
                  v-if="!record.__ghost && record.purpose"
                  :title="record.purpose"
                  placement="bottom"
                >
                  <span class="cell-ellipsis">{{ record.purpose }}</span>
                </a-tooltip>
                <span v-else-if="!record.__ghost">-</span>
                <span v-else />
              </template>

              <template #addressCell="{ record }">
                <a-tooltip
                  v-if="!record.__ghost && record.address"
                  :title="record.address"
                  placement="bottom"
                >
                  <span class="cell-ellipsis">{{ record.address }}</span>
                </a-tooltip>
                <span v-else-if="!record.__ghost">-</span>
                <span v-else />
              </template>

              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost && record.status !== undefined && record.status !== null"
                  :color="STATUS_MAP[record.status]?.color || 'default'"
                >
                  {{ STATUS_MAP[record.status]?.label || '-' }}
                </a-tag>
                <span v-else-if="!record.__ghost">-</span>
                <span v-else />
              </template>

              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="openEdit(record)"
                  >
                    编辑
                  </a-button>
                  <a-popconfirm
                    v-if="record.status === 0 || record.status === 1"
                    title="确认取消该拜访计划？"
                    ok-text="取消计划"
                    cancel-text="返回"
                    @confirm="handleCancel(record)"
                  >
                    <a-button
                      type="link"
                      size="small"
                    >
                      取消计划
                    </a-button>
                  </a-popconfirm>
                  <a-popconfirm
                    title="确认删除该拜访计划？"
                    ok-text="删除"
                    cancel-text="取消"
                    @confirm="handleDelete(record)"
                  >
                    <a-button
                      type="link"
                      size="small"
                      danger
                    >
                      删除
                    </a-button>
                  </a-popconfirm>
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

      <!-- ═══ 新建 / 编辑拜访计划弹窗 ═══ -->
      <a-modal
        v-model:open="modalOpen"
        :title="editingId ? '编辑拜访计划' : '新建拜访计划'"
        :confirm-loading="saving"
        width="560px"
        @ok="handleSave"
      >
        <a-form
          ref="formRef"
          :model="form"
          :rules="rules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item
            label="客户"
            name="customerId"
          >
            <a-select
              v-model:value="form.customerId"
              placeholder="请选择客户"
              show-search
              option-filter-prop="label"
              :options="customerOptions"
            />
          </a-form-item>
          <a-form-item
            label="负责人"
            name="salesPersonId"
          >
            <a-select
              v-model:value="form.salesPersonId"
              placeholder="请选择负责人"
              show-search
              option-filter-prop="label"
              :options="salesPersonOptions"
            />
          </a-form-item>
          <a-form-item
            label="计划日期"
            name="planDate"
          >
            <a-date-picker
              v-model:value="form.planDate"
              value-format="YYYY-MM-DD"
              style="width: 100%"
              placeholder="请选择计划拜访日期"
            />
          </a-form-item>
          <a-form-item
            label="计划时间"
            name="planTime"
          >
            <a-input
              v-model:value="form.planTime"
              placeholder="如 10:00-11:00（选填）"
            />
          </a-form-item>
          <a-form-item
            label="拜访目的"
            name="purpose"
          >
            <a-input
              v-model:value="form.purpose"
              placeholder="请输入拜访目的"
            />
          </a-form-item>
          <a-form-item
            label="拜访地址"
            name="address"
          >
            <a-input
              v-model:value="form.address"
              placeholder="请输入拜访地址（选填）"
            />
          </a-form-item>
          <a-form-item
            label="备注"
            name="remark"
          >
            <a-textarea
              v-model:value="form.remark"
              :rows="2"
              placeholder="请输入备注（选填）"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
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
import { visitPlanApi, crmCustomerApi, type VisitPlan } from '@/api/crm'
import { userApi } from '@/api/user'

defineOptions({ name: 'SalesVisitPlan' })

// ═══ 计划状态（0待执行 1执行中 2已完成 3已取消，与后端 VisitPlan 一致） ═══
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待执行', color: 'orange' },
  1: { label: '执行中', color: 'blue' },
  2: { label: '已完成', color: 'green' },
  3: { label: '已取消', color: 'red' }
}
/** 状态下拉选项由 STATUS_MAP 反推（字典单一真源） */
const statusOptions = Object.entries(STATUS_MAP).map(([value, v]) => ({
  label: v.label,
  value: Number(value)
}))

/**
 * 超期判定：有计划日期 + 状态待执行/执行中 + 计划日期早于今天（自然日）
 * 已完成（2）与已取消（3）不参与
 */
function isOverdue(record: VisitPlan): boolean {
  return !!record.planDate && (record.status === 0 || record.status === 1) &&
    dayjs(record.planDate).isBefore(dayjs(), 'day')
}

// ═══ 下拉选项 ═══
const customerOptions = ref<{ label: string; value: number }[]>([])
const salesPersonOptions = ref<{ label: string; value: number }[]>([])

function filterOption(input: string, option: any): boolean {
  return String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())
}

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'customerId', label: '客户', visible: true },
  { key: 'salesPersonId', label: '负责人', visible: true },
  { key: 'status', label: '状态', visible: true },
  { key: 'planDateRange', label: '计划日期', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'create', label: '新建计划', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]
const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))
const pageConfigStorageKey = 'crm-visit-plan-page-config'

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

// ═══ 查询条件 ═══
const searchForm = reactive({
  customerId: undefined as number | undefined,
  salesPersonId: undefined as number | undefined,
  status: undefined as number | undefined,
  planDateRange: undefined as [string, string] | undefined,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const loading = ref(false)
const tableData = ref<any[]>([])

// ═══ 表格列（rowNo 承载列配置齿轮；action 为锁定列） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 190, fixed: 'left' },
  { key: 'planNo', title: '计划编号', type: 'input', width: 150 },
  { key: 'customerName', title: '客户', type: 'slot', slotName: 'customerCell', width: 170 },
  { key: 'salesPersonName', title: '负责人', type: 'input', width: 100 },
  { key: 'planDate', title: '计划日期', type: 'slot', slotName: 'planDateCell', width: 110 },
  { key: 'planTime', title: '计划时间', type: 'input', width: 110 },
  { key: 'purpose', title: '拜访目的', type: 'slot', slotName: 'purposeCell', width: 200 },
  { key: 'address', title: '拜访地址', type: 'slot', slotName: 'addressCell', width: 200 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 90 },
  { key: 'remark', title: '备注', type: 'input', width: 160, defaultHidden: true },
  // 创建时间：crm_visit_plan 已有 created_at 列、后端已返回；默认隐藏避免打乱常用列集
  {
    key: 'createdAt',
    title: '创建时间',
    type: 'input',
    width: 160,
    defaultHidden: true,
    // ⚠️ BillDetailTable 的 formatter 是位置参数 (raw, record)，不可写成解构
    formatter: (raw: any) => (raw ? dayjs(raw).format('YYYY-MM-DD HH:mm') : '-')
  },
]

// ═══ 数据加载（GET /crm/visit/plan/page，page/size 风格，后端同时兼容 pageNum/pageSize） ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await visitPlanApi.page({
      page: pagination.current,
      size: pagination.pageSize,
      customerId: searchForm.customerId,
      salesPersonId: searchForm.salesPersonId,
      status: searchForm.status,
      planDateStart: searchForm.planDateRange?.[0],
      planDateEnd: searchForm.planDateRange?.[1],
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[拜访规划] 加载列表失败', error)
    message.error(error?.message || '加载列表失败')
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
  searchForm.customerId = undefined
  searchForm.salesPersonId = undefined
  searchForm.status = undefined
  searchForm.planDateRange = undefined
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

// ═══ 新建 / 编辑弹窗 ═══
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  customerId: undefined as number | undefined,
  salesPersonId: undefined as number | undefined,
  planDate: undefined as string | undefined,
  planTime: '',
  purpose: '',
  address: '',
  remark: ''
})
const form = reactive(emptyForm())

const rules: Record<string, any> = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  salesPersonId: [{ required: true, message: '请选择负责人', trigger: 'change' }],
  planDate: [{ required: true, message: '请选择计划日期', trigger: 'change' }],
  purpose: [{ required: true, message: '请输入拜访目的', trigger: 'blur' }]
}

function resetForm(data?: Partial<VisitPlan>) {
  Object.assign(form, emptyForm(), data || {})
}

function openCreate() {
  editingId.value = null
  resetForm()
  modalOpen.value = true
}

function openEdit(record: VisitPlan) {
  editingId.value = record.id ?? null
  resetForm({
    customerId: record.customerId,
    salesPersonId: record.salesPersonId,
    planDate: record.planDate,
    planTime: record.planTime || '',
    purpose: record.purpose || '',
    address: record.address || '',
    remark: record.remark || ''
  })
  modalOpen.value = true
}

/** 后端按实体原样保存，两个名称快照列由前端按选中项回填（省后端二次查询） */
function resolveNames() {
  const customer = customerOptions.value.find(o => o.value === form.customerId)
  const salesPerson = salesPersonOptions.value.find(o => o.value === form.salesPersonId)
  return {
    customerName: customer?.label,
    salesPersonName: salesPerson?.label
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const payload = { ...form, ...resolveNames() }
    if (editingId.value) {
      await visitPlanApi.update(editingId.value, payload)
      message.success('拜访计划更新成功')
    } else {
      await visitPlanApi.create(payload)
      message.success('拜访计划创建成功')
    }
    modalOpen.value = false
    fetchList()
  } catch (e: any) {
    console.warn('[拜访规划] 保存失败', e)
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 变更类接口走原生 axios 解包，未生效时返回裸 false（不抛错）→ 必须判返回值，否则会「提示成功但实际未生效」
async function handleCancel(record: VisitPlan) {
  try {
    const ok = await visitPlanApi.cancel(record.id as number)
    if (ok === false) {
      message.warning('取消失败：该计划可能已完成或已被删除，请刷新后重试')
    } else {
      message.success('计划已取消')
    }
    fetchList()
  } catch (e: any) {
    console.warn('[拜访规划] 取消失败', e)
    message.error(e?.message || '取消失败')
  }
}

async function handleDelete(record: VisitPlan) {
  try {
    const ok = await visitPlanApi.remove(record.id as number)
    if (ok === false) {
      message.warning('删除未生效：该计划可能已被删除，请刷新后重试')
    } else {
      message.success('删除成功')
    }
    fetchList()
  } catch (e: any) {
    console.warn('[拜访规划] 删除失败', e)
    message.error(e?.message || '删除失败')
  }
}

// ═══ 导出（套状态下拉的中文文案，避免导出成数字） ═══
function exportCsv() {
  const rows = (tableData.value || []).filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const headers = ['计划编号', '客户', '负责人', '计划日期', '计划时间', '拜访目的', '拜访地址', '状态', '备注']
  const lines = rows.map((r: any) => [
    r.planNo, r.customerName, r.salesPersonName, r.planDate, r.planTime,
    r.purpose, r.address, STATUS_MAP[r.status]?.label || '-', r.remark
  ])
  const escape = (v: any) => `"${String(v ?? '').replace(/"/g, '""')}"`
  const csv = '\uFEFF' + [headers, ...lines].map(row => row.map(escape).join(',')).join('\r\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `拜访规划_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

function handleExport() {
  exportCsv()
}

// ═══ 打印(F8) ═══
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
      <td>${escapeHtml(r.planNo)}</td>
      <td>${escapeHtml(r.customerName || '')}</td>
      <td>${escapeHtml(r.salesPersonName || '')}</td>
      <td>${escapeHtml(r.planDate || '')}</td>
      <td>${escapeHtml(r.planTime || '')}</td>
      <td>${escapeHtml(r.purpose || '')}</td>
      <td>${escapeHtml(r.address || '')}</td>
      <td>${escapeHtml(STATUS_MAP[r.status]?.label || '-')}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>拜访规划</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>拜访规划</h2>
    <div class="meta">
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr><th>#</th><th>计划编号</th><th>客户</th><th>负责人</th><th>计划日期</th>
      <th>计划时间</th><th>拜访目的</th><th>拜访地址</th><th>状态</th></tr></thead>
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

function handleError(error: Error) {
  console.error('[拜访规划] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 下拉数据加载 ═══
onMounted(async () => {
  try {
    const list = await crmCustomerApi.dropdown()
    customerOptions.value = (Array.isArray(list) ? list : []).map((c: any) => ({
      label: c.name,
      value: c.id
    }))
  } catch (e) {
    console.warn('[拜访规划] 客户下拉获取失败', e)
    message.warning('客户下拉加载失败，请稍后重试')
  }
  try {
    const res: any = await userApi.getPage({ pageNum: 1, pageSize: 200 })
    const list = res?.records || res?.data?.records || []
    salesPersonOptions.value = list.map((u: any) => ({
      label: u.nickname || u.username,
      value: u.id
    }))
  } catch (e) {
    console.warn('[拜访规划] 负责人下拉获取失败', e)
    message.warning('负责人下拉加载失败，请稍后重试')
  }
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-item :deep(.ant-select) { min-width: 150px; }
.search-item :deep(.ant-input-affix-wrapper) { width: 180px; }
.search-actions { margin-left: auto; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-ellipsis { display: inline-block; max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; vertical-align: bottom; }
/* 超期计划红字（仅待执行/执行中参与） */
.overdue-plan { color: #ff4d4f; }

/* 橙色新增按钮（CRM 模块统一） */
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-add:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
