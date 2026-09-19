<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        客户跟进（CRM → 客户管理 → 客户跟进，菜单 80201）
        · CRM 为本系统独有模块（ql361 无 CRM 域），按业界生产级 CRM 的 Activity 模型建模
        · 规格：docs/Yh-Spec/手动整理对标开发文档/CRM模块/客户跟进开发文档.md
        · 路线 A：ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillDetailTable
        · 列配置齿轮在表头 rowNo 列；页面配置弹窗控制查询条件显隐 + 功能按钮启用
        · 本轮补齐：编辑/删除行操作（后端 PUT/DELETE 原已就绪但前端未接）、
                   跟进方式/结果/日期区间筛选、「下次跟进」待办口径、跟进人写入
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新建跟进 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('create')"
            type="primary"
            size="small"
            class="btn-add"
            @click="openCreate"
          >
            <PlusOutlined /> 新建跟进
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
                v-if="isFieldVisible('keyword')"
                class="search-item"
              >
                <span class="search-label">关键词</span>
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="单号/客户/内容"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
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
                  :filter-option="filterCustomerOption"
                  :options="customerOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('followUpType')"
                class="search-item"
              >
                <span class="search-label">跟进类型</span>
                <a-select
                  v-model:value="searchForm.followUpType"
                  placeholder="全部类型"
                  size="small"
                  allow-clear
                  :options="FOLLOW_UP_TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('followUpResult')"
                class="search-item"
              >
                <span class="search-label">跟进结果</span>
                <a-select
                  v-model:value="searchForm.followUpResult"
                  placeholder="全部结果"
                  size="small"
                  allow-clear
                  :options="FOLLOW_UP_RESULT_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('followUpDateRange')"
                class="search-item"
              >
                <span class="search-label">跟进日期</span>
                <a-range-picker
                  v-model:value="searchForm.followUpDateRange"
                  size="small"
                  value-format="YYYY-MM-DD"
                  :placeholder="['开始日期', '结束日期']"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('nextFollowUpDateRange')"
                class="search-item"
              >
                <span class="search-label">下次跟进</span>
                <a-range-picker
                  v-model:value="searchForm.nextFollowUpDateRange"
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
                <a-button
                  size="small"
                  @click="handleTodoToday"
                >
                  今日待跟进
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
              storage-key="crm-customer-follow-table-columns"
              global-config-key="crm-customer-follow-table-columns"
            >
              <template #customerCell="{ record }">
                <span v-if="record.__ghost" />
                <span v-else>{{ record.customerName || '-' }}</span>
              </template>

              <template #followUpTypeCell="{ record }">
                <a-tag
                  v-if="!record.__ghost && record.followUpType"
                  :color="followUpTypeColor(record.followUpType)"
                >
                  {{ followUpTypeText(record.followUpType) }}
                </a-tag>
                <span v-else>-</span>
              </template>

              <template #followUpResultCell="{ record }">
                <a-tag
                  v-if="!record.__ghost && record.followUpResult"
                  :color="followUpResultColor(record.followUpResult)"
                >
                  {{ followUpResultText(record.followUpResult) }}
                </a-tag>
                <span v-else>-</span>
              </template>

              <template #contentCell="{ record }">
                <a-tooltip
                  v-if="!record.__ghost && record.content"
                  :title="record.content"
                  placement="bottom"
                >
                  <span class="cell-ellipsis">{{ record.content }}</span>
                </a-tooltip>
                <span v-else>-</span>
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
                    修改
                  </a-button>
                  <a-popconfirm
                    title="确定删除该跟进记录？"
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

      <!-- ═══ 新建 / 编辑跟进弹窗 ═══ -->
      <a-modal
        v-model:open="createVisible"
        :title="editingId ? '编辑跟进记录' : '新建跟进记录'"
        :confirm-loading="createLoading"
        ok-text="保存"
        cancel-text="取消"
        :width="560"
        @ok="handleCreate"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item
            label="客户"
            required
          >
            <a-select
              v-model:value="createForm.customerId"
              placeholder="请选择客户"
              show-search
              :filter-option="filterCustomerOption"
              :options="customerOptions"
            />
          </a-form-item>
          <a-form-item
            label="跟进类型"
            required
          >
            <a-select
              v-model:value="createForm.followUpType"
              placeholder="请选择跟进类型"
              :options="FOLLOW_UP_TYPE_OPTIONS"
            />
          </a-form-item>
          <a-form-item label="联系人">
            <a-input
              v-model:value="createForm.contactName"
              placeholder="联系人姓名"
            />
          </a-form-item>
          <a-form-item label="联系电话">
            <a-input
              v-model:value="createForm.contactPhone"
              placeholder="联系电话"
            />
          </a-form-item>
          <a-form-item label="跟进日期">
            <a-date-picker
              v-model:value="createForm.followUpDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
            />
          </a-form-item>
          <a-form-item
            label="跟进内容"
            required
          >
            <a-textarea
              v-model:value="createForm.content"
              placeholder="请输入跟进内容"
              :rows="3"
            />
          </a-form-item>
          <a-form-item label="跟进结果">
            <a-select
              v-model:value="createForm.followUpResult"
              placeholder="请选择跟进结果"
              :options="FOLLOW_UP_RESULT_OPTIONS"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="下一步行动">
            <a-input
              v-model:value="createForm.nextAction"
              placeholder="下一步行动计划"
            />
          </a-form-item>
          <a-form-item label="下次跟进">
            <a-date-picker
              v-model:value="createForm.nextFollowUpDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
            />
          </a-form-item>
          <a-form-item label="备注">
            <a-input
              v-model:value="createForm.remark"
              placeholder="备注"
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
import { followUpApi, crmCustomerApi, type FollowUpRecord } from '@/api/crm'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'CrmCustomerFollow' })

const userStore = useUserStore()

// ═══ 跟进类型 / 结果 字典（与客户页弹窗逐字一致） ═══
const FOLLOW_UP_TYPE_OPTIONS = [
  { label: '电话', value: 1 },
  { label: '拜访', value: 2 },
  { label: '邮件', value: 3 },
  { label: '微信', value: 4 },
  { label: '其他', value: 5 }
]
const FOLLOW_UP_RESULT_OPTIONS = [
  { label: '有意向', value: 1 },
  { label: '无意向', value: 2 },
  { label: '待跟进', value: 3 }
]
const TYPE_TEXT: Record<number, string> = { 1: '电话', 2: '拜访', 3: '邮件', 4: '微信', 5: '其他' }
const TYPE_COLOR: Record<number, string> = { 1: 'blue', 2: 'green', 3: 'purple', 4: 'cyan', 5: 'default' }
const RESULT_TEXT: Record<number, string> = { 1: '有意向', 2: '无意向', 3: '待跟进' }
const RESULT_COLOR: Record<number, string> = { 1: 'green', 2: 'red', 3: 'orange' }

function followUpTypeText(v: number | undefined): string {
  return (v && TYPE_TEXT[v]) || '-'
}
function followUpTypeColor(v: number | undefined): string {
  return (v && TYPE_COLOR[v]) || 'default'
}
function followUpResultText(v: number | undefined): string {
  return (v && RESULT_TEXT[v]) || '-'
}
function followUpResultColor(v: number | undefined): string {
  return (v && RESULT_COLOR[v]) || 'default'
}

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '关键词', visible: true },
  { key: 'customerId', label: '客户', visible: true },
  { key: 'followUpType', label: '跟进类型', visible: true },
  { key: 'followUpResult', label: '跟进结果', visible: true },
  { key: 'followUpDateRange', label: '跟进日期', visible: true },
  { key: 'nextFollowUpDateRange', label: '下次跟进', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'create', label: '新建跟进', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]
const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))
const pageConfigStorageKey = 'crm-customer-follow-page-config'

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

// ═══ 客户下拉 ═══
const customerOptions = ref<{ label: string; value: number }[]>([])

function filterCustomerOption(input: string, option: any): boolean {
  return String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())
}

// ═══ 查询条件 ═══
const searchForm = reactive({
  keyword: '',
  customerId: undefined as number | undefined,
  followUpType: undefined as number | undefined,
  followUpResult: undefined as number | undefined,
  followUpDateRange: undefined as [string, string] | undefined,
  nextFollowUpDateRange: undefined as [string, string] | undefined,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const loading = ref(false)
const tableData = ref<any[]>([])

// ═══ 表格列（rowNo 承载列配置齿轮；action 为固定列） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 110, fixed: 'left' },
  { key: 'followUpCode', title: '跟进单号', type: 'input', width: 160 },
  { key: 'customerName', title: '客户', type: 'slot', slotName: 'customerCell', width: 160 },
  { key: 'opportunityName', title: '商机', type: 'input', width: 160, defaultHidden: true },
  { key: 'leadName', title: '线索', type: 'input', width: 160, defaultHidden: true },
  { key: 'followUpType', title: '类型', type: 'slot', slotName: 'followUpTypeCell', width: 90 },
  { key: 'contactName', title: '联系人', type: 'input', width: 100 },
  { key: 'contactPhone', title: '联系电话', type: 'input', width: 130, defaultHidden: true },
  { key: 'followUpDate', title: '跟进日期', type: 'input', width: 110 },
  { key: 'content', title: '跟进内容', type: 'slot', slotName: 'contentCell', width: 240 },
  { key: 'nextAction', title: '下一步行动', type: 'input', width: 160 },
  { key: 'followUpResult', title: '跟进结果', type: 'slot', slotName: 'followUpResultCell', width: 100 },
  { key: 'nextFollowUpDate', title: '下次跟进', type: 'input', width: 110 },
  { key: 'salesPersonName', title: '跟进人', type: 'input', width: 100 },
  { key: 'departmentName', title: '部门', type: 'input', width: 120, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 160, defaultHidden: true },
  { key: 'createdAt', title: '创建时间', type: 'input', width: 160, defaultHidden: true },
]

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await followUpApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
      customerId: searchForm.customerId,
      followUpType: searchForm.followUpType,
      followUpResult: searchForm.followUpResult,
      followUpDateStart: searchForm.followUpDateRange?.[0],
      followUpDateEnd: searchForm.followUpDateRange?.[1],
      nextFollowUpDateStart: searchForm.nextFollowUpDateRange?.[0],
      nextFollowUpDateEnd: searchForm.nextFollowUpDateRange?.[1],
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[客户跟进] 加载列表失败', error)
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
  searchForm.customerId = undefined
  searchForm.followUpType = undefined
  searchForm.followUpResult = undefined
  searchForm.followUpDateRange = undefined
  searchForm.nextFollowUpDateRange = undefined
  pagination.current = 1
  fetchList()
}

/** 待办口径：下次跟进日期落在今天（含逾期未跟进的） */
function handleTodoToday() {
  const today = dayjs().format('YYYY-MM-DD')
  searchForm.nextFollowUpDateRange = ['2000-01-01', today]
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

// ═══ 新建 / 编辑 ═══
const createVisible = ref(false)
const createLoading = ref(false)
const editingId = ref<number | null>(null)
const createForm = reactive({
  customerId: undefined as number | undefined,
  followUpType: 1 as number,
  contactName: '',
  contactPhone: '',
  followUpDate: undefined as string | undefined,
  content: '',
  followUpResult: undefined as number | undefined,
  nextAction: '',
  nextFollowUpDate: undefined as string | undefined,
  remark: ''
})

function resetCreateForm() {
  createForm.customerId = undefined
  createForm.followUpType = 1
  createForm.contactName = ''
  createForm.contactPhone = ''
  // 跟进日期默认今天：原先留空会落 NULL，而列表按 follow_up_date DESC 排序时
  // PostgreSQL 是 NULLS FIRST，导致「没填日期」的记录反而排在最前
  createForm.followUpDate = dayjs().format('YYYY-MM-DD')
  createForm.content = ''
  createForm.followUpResult = undefined
  createForm.nextAction = ''
  createForm.nextFollowUpDate = undefined
  createForm.remark = ''
}

function openCreate() {
  editingId.value = null
  resetCreateForm()
  createVisible.value = true
}

function openEdit(record: any) {
  editingId.value = record.id
  createForm.customerId = record.customerId
  createForm.followUpType = record.followUpType ?? 1
  createForm.contactName = record.contactName || ''
  createForm.contactPhone = record.contactPhone || ''
  createForm.followUpDate = record.followUpDate || undefined
  createForm.content = record.content || ''
  createForm.followUpResult = record.followUpResult ?? undefined
  createForm.nextAction = record.nextAction || ''
  createForm.nextFollowUpDate = record.nextFollowUpDate || undefined
  createForm.remark = record.remark || ''
  createVisible.value = true
}

async function handleCreate() {
  if (!createForm.customerId) {
    message.warning('请选择客户')
    return
  }
  if (!createForm.content.trim()) {
    message.warning('请输入跟进内容')
    return
  }
  const customer = customerOptions.value.find(o => o.value === createForm.customerId)
  const payload: Partial<FollowUpRecord> = {
    customerId: createForm.customerId,
    customerName: customer?.label,
    followUpType: createForm.followUpType,
    followUpTypeDesc: TYPE_TEXT[createForm.followUpType],
    contactName: createForm.contactName || undefined,
    contactPhone: createForm.contactPhone || undefined,
    followUpDate: createForm.followUpDate,
    content: createForm.content.trim(),
    followUpResult: createForm.followUpResult,
    followUpResultDesc: createForm.followUpResult ? RESULT_TEXT[createForm.followUpResult] : undefined,
    nextAction: createForm.nextAction || undefined,
    nextFollowUpDate: createForm.nextFollowUpDate,
    remark: createForm.remark || undefined,
    // 跟进人：此前 payload 不含该字段，导致「跟进人」列恒空、无「谁录的」审计
    salesPersonId: userStore.userId || undefined,
    salesPersonName: userStore.nickname || userStore.username || undefined,
  }
  createLoading.value = true
  try {
    if (editingId.value) {
      await followUpApi.update(editingId.value, payload)
      message.success('跟进记录已更新')
    } else {
      await followUpApi.create(payload)
      message.success('跟进记录已保存')
    }
    createVisible.value = false
    fetchList()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    createLoading.value = false
  }
}

async function handleDelete(record: any) {
  try {
    await followUpApi.delete(record.id)
    message.success('删除成功')
    fetchList()
  } catch (e: any) {
    message.error(e?.message || '删除失败')
  }
}

// ═══ 导出（带中文映射，避免类型/结果导出成数字） ═══
function exportCsv() {
  const rows = (tableData.value || []).filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const headers = ['跟进单号', '客户', '类型', '联系人', '联系电话', '跟进日期', '跟进内容',
    '下一步行动', '跟进结果', '下次跟进', '跟进人']
  const lines = rows.map((r: any) => [
    r.followUpCode, r.customerName, followUpTypeText(r.followUpType), r.contactName, r.contactPhone,
    r.followUpDate, r.content, r.nextAction, followUpResultText(r.followUpResult),
    r.nextFollowUpDate, r.salesPersonName
  ])
  const escape = (v: any) => `"${String(v ?? '').replace(/"/g, '""')}"`
  const csv = '\uFEFF' + [headers, ...lines].map(row => row.map(escape).join(',')).join('\r\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `客户跟进_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
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
      <td>${escapeHtml(r.followUpCode)}</td>
      <td>${escapeHtml(r.customerName || '')}</td>
      <td>${escapeHtml(followUpTypeText(r.followUpType))}</td>
      <td>${escapeHtml(r.contactName || '')}</td>
      <td>${escapeHtml(r.followUpDate || '')}</td>
      <td>${escapeHtml(r.content || '')}</td>
      <td>${escapeHtml(followUpResultText(r.followUpResult))}</td>
      <td>${escapeHtml(r.nextFollowUpDate || '')}</td>
      <td>${escapeHtml(r.salesPersonName || '')}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>客户跟进</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>客户跟进</h2>
    <div class="meta">
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr><th>#</th><th>跟进单号</th><th>客户</th><th>类型</th><th>联系人</th>
      <th>跟进日期</th><th>跟进内容</th><th>跟进结果</th><th>下次跟进</th><th>跟进人</th></tr></thead>
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
  console.error('[客户跟进] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(async () => {
  try {
    const list = await crmCustomerApi.dropdown()
    customerOptions.value = (Array.isArray(list) ? list : []).map((c: any) => ({
      label: c.name,
      value: c.id
    }))
  } catch (e) {
    console.warn('[客户跟进] 客户下拉获取失败', e)
    message.warning('客户下拉加载失败，请稍后重试')
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
.cell-ellipsis { display: inline-block; max-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; vertical-align: bottom; }

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
