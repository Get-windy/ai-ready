<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        支付账户（资料 → 支付管理 → 支付账户）
        · 单入口；无左侧分类树；列配置齿轮在数据表表头（个人配置 / 全局配置）
        · 统计卡片走后端 GET /erp/finance/account/statistics（不再前端本地汇总）
        · 工具栏：新增账户 ｜ 页面配置 · 刷新 · 打印(F8) · 导出
        · 查询区（页面配置可控显隐）：筛选条件 / 账户类型 / 账户等级 / 状态 / 币种
        · 单一口径（红线）：资金账户一律 finance_account，增删改与《银行账户》共用同一后端 Service
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增账户 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="fnEnabled('add')"
            type="primary"
            size="small"
            class="btn-add"
            @click="handleAdd"
          >
            <PlusOutlined /> 新增账户
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧：页面配置 + 刷新 + 打印(F8) + 导出 ═══ -->
        <!-- 列配置走数据表表头齿轮（BillTableList 内置），工具栏不重复放入口 -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip
              v-if="fnEnabled('config')"
              title="页面配置"
              placement="bottom"
            >
              <a-button
                size="small"
                class="btn-config"
                @click="showPageConfig = true"
              >
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button
              v-if="fnEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="handleSearch"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="fnEnabled('printF8')"
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="fnEnabled('export')"
              size="small"
              :loading="exporting"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（按页面配置动态渲染） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <template
                v-for="field in visibleSearchFields"
                :key="field.key"
              >
                <div
                  v-if="field.type === 'select'"
                  class="search-field-item"
                >
                  <div class="search-select-wrap">
                    <span class="search-select-label">{{ field.label }}</span>
                    <a-select
                      v-model:value="searchValues[field.key]"
                      :placeholder="field.placeholder || '全部'"
                      :options="field.options"
                      allow-clear
                      size="small"
                      @change="handleSearch"
                    />
                  </div>
                </div>
                <div
                  v-else
                  class="search-field-item"
                >
                  <div class="search-select-wrap">
                    <span class="search-select-label">{{ field.label }}</span>
                    <a-input
                      v-model:value="searchValues[field.key]"
                      :placeholder="field.placeholder || field.label"
                      size="small"
                      allow-clear
                      @press-enter="handleSearch"
                    />
                  </div>
                </div>
              </template>
              <div class="search-field-item">
                <a-space :size="4">
                  <a-button
                    type="primary"
                    size="small"
                    @click="handleSearch"
                  >
                    <SearchOutlined /> 查询
                  </a-button>
                  <a-button
                    size="small"
                    @click="handleReset"
                  >
                    重置
                  </a-button>
                </a-space>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 统计卡片 + 账户列表 ═══ -->
        <template #table>
          <div class="table-area">
            <ARStatCards
              :items="statCards"
              :loading="loading"
              class="stat-cards"
            />
            <BillTableList
              :columns="columns"
              :data-source="tableData"
              :loading="loading"
              :pagination="false"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="false"
              storage-key="md-payment-account-columns"
              global-config-key="md-payment-account-columns-global"
              row-key="id"
            >
              <template #nameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="handleEdit(record)"
                >{{ record.accountName }}</a>
              </template>

              <template #accountTypeCell="{ record }">
                <a-tag :color="ACCOUNT_TYPE_MAP[record.accountType]?.color || 'default'">
                  {{ ACCOUNT_TYPE_MAP[record.accountType]?.label || '-' }}
                </a-tag>
              </template>

              <template #accountLevelCell="{ record }">
                <a-tag
                  v-if="record.accountLevel"
                  :color="ACCOUNT_LEVEL_MAP[record.accountLevel]?.color || 'default'"
                >
                  {{ ACCOUNT_LEVEL_MAP[record.accountLevel]?.label || '-' }}
                </a-tag>
                <span v-else>-</span>
              </template>

              <template #balanceCell="{ record }">
                <span :class="{ 'text-danger': Number(record.balance) < 0 }">
                  {{ formatMoney(record.balance) }}
                </span>
              </template>

              <template #statusCell="{ record }">
                <a-tag :color="record.status === 1 ? 'green' : 'default'">
                  {{ record.status === 1 ? '启用' : '停用' }}
                </a-tag>
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
                    @click="openBalance(record)"
                  >
                    余额调整
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="handleToggleStatus(record)"
                  >
                    {{ record.status === 1 ? '停用' : '启用' }}
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
            </BillTableList>
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
    </PageContainer>

    <!-- ═══ 页面配置弹窗（查询条件 + 功能按钮） ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldConfig"
      :function-buttons-config="functionButtonConfig"
      :default-query-fields-config="DEFAULT_QUERY_FIELDS"
      :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
      :storage-key="PAGE_CONFIG_STORAGE_KEY"
      hide-print-config
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- ═══ 新增 / 修改弹窗 ═══ -->
    <a-modal
      v-model:open="modalVisible"
      :title="formState.id ? '修改支付账户' : '新增支付账户'"
      :width="640"
      :mask-closable="false"
      :confirm-loading="saving"
      ok-text="保存(Enter)"
      cancel-text="关闭(Esc)"
      @ok="handleSave"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        size="small"
      >
        <a-form-item
          label="账户名称"
          name="accountName"
        >
          <a-input
            v-model:value="formState.accountName"
            placeholder="如 基本户-招行 / 库存现金"
            :maxlength="64"
          />
        </a-form-item>

        <a-form-item
          label="账户编号"
          name="subjectCode"
        >
          <a-input
            v-model:value="formState.subjectCode"
            placeholder="资金科目编号，如 1001"
            :maxlength="32"
          />
        </a-form-item>

        <a-form-item
          label="账户类型"
          name="accountType"
        >
          <a-select
            v-model:value="formState.accountType"
            :options="ACCOUNT_TYPE_OPTIONS"
            placeholder="请选择账户类型"
          />
        </a-form-item>

        <a-form-item
          label="账户等级"
          name="accountLevel"
        >
          <a-select
            v-model:value="formState.accountLevel"
            :options="ACCOUNT_LEVEL_OPTIONS"
            placeholder="请选择账户等级"
            allow-clear
          />
        </a-form-item>

        <a-form-item
          label="开户银行"
          name="bankName"
        >
          <a-input
            v-model:value="formState.bankName"
            placeholder="如 招商银行"
            :maxlength="128"
          />
        </a-form-item>

        <a-form-item
          label="银行账号"
          name="bankAccount"
        >
          <a-input
            v-model:value="formState.bankAccount"
            placeholder="请输入银行账号"
            :maxlength="64"
          />
        </a-form-item>

        <a-form-item
          label="币种"
          name="currency"
        >
          <a-select
            v-model:value="formState.currency"
            :options="CURRENCY_OPTIONS"
            placeholder="请选择币种"
          />
        </a-form-item>

        <a-form-item
          v-if="!formState.id"
          label="期初余额"
          name="openingBalance"
        >
          <a-input-number
            v-model:value="formState.openingBalance"
            :precision="2"
            :step="100"
            style="width: 100%"
            placeholder="保存后按该金额登记账户余额"
          />
        </a-form-item>

        <a-form-item
          label="备注"
          name="remark"
        >
          <a-textarea
            v-model:value="formState.remark"
            placeholder="请输入备注"
            :rows="2"
            :maxlength="255"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ═══ 余额调整弹窗（增量口径：正数增加 / 负数减少） ═══ -->
    <a-modal
      v-model:open="balanceVisible"
      title="余额调整"
      :width="480"
      :mask-closable="false"
      :confirm-loading="balanceSaving"
      ok-text="确认调整"
      cancel-text="关闭(Esc)"
      @ok="handleBalanceSave"
    >
      <a-descriptions
        :column="1"
        size="small"
        bordered
        class="balance-desc"
      >
        <a-descriptions-item label="支付账户">
          {{ balanceTarget?.accountName || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="当前余额">
          {{ formatMoney(balanceTarget?.balance) }}
        </a-descriptions-item>
      </a-descriptions>
      <a-form
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        size="small"
        class="balance-form"
      >
        <a-form-item label="调整金额">
          <a-input-number
            v-model:value="balanceAmount"
            :precision="2"
            :step="100"
            style="width: 100%"
            placeholder="正数增加、负数减少"
          />
        </a-form-item>
        <a-form-item label="调整后余额">
          <span :class="{ 'text-danger': previewBalance < 0 }">
            {{ formatMoney(previewBalance) }}
          </span>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="md-payment-account"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PlusOutlined, SearchOutlined, SettingOutlined,
  ReloadOutlined, PrinterOutlined, DownloadOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { StatCardItem } from '@/components/ARReportPage/types'
import { financeAccountApi, type PaymentAccountInfo, type PaymentAccountQuery } from '@/api/md'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'MdPaymentAccount' })

const PAGE_CONFIG_STORAGE_KEY = 'md-payment-account-page-config'

// ═══ 字典（与后端 FinanceAccount 注释一致） ═══
const ACCOUNT_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '银行账户', color: 'blue' },
  2: { label: '现金账户', color: 'green' },
  3: { label: '内部账户', color: 'purple' },
  4: { label: '外部账户', color: 'orange' },
}
const ACCOUNT_LEVEL_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '基本账户', color: 'gold' },
  2: { label: '一般账户', color: 'default' },
  3: { label: '专用账户', color: 'cyan' },
}

const ACCOUNT_TYPE_OPTIONS = Object.entries(ACCOUNT_TYPE_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))
const ACCOUNT_LEVEL_OPTIONS = Object.entries(ACCOUNT_LEVEL_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))
const CURRENCY_OPTIONS = ['CNY', 'USD', 'HKD', 'EUR', 'JPY'].map(c => ({ label: c, value: c }))

// ═══ 状态 ═══
const loading = ref(false)
const saving = ref(false)
const exporting = ref(false)
const tableData = ref<PaymentAccountInfo[]>([])
const statSummary = ref<{ total?: number; enabled?: number; disabled?: number; totalBalance?: number }>({})
const showPageConfig = ref(false)

// ═══ 查询条件 ═══
interface SearchFieldDef {
  key: string
  label: string
  type: 'input' | 'select'
  placeholder?: string
  options?: Array<{ label: string; value: any }>
}

const DEFAULT_QUERY_FIELDS = [
  { key: 'keyword', label: '筛选条件', visible: true },
  { key: 'accountType', label: '账户类型', visible: true },
  { key: 'accountLevel', label: '账户等级', visible: true },
  { key: 'status', label: '状态', visible: true },
  { key: 'currency', label: '币种', visible: false },
]

const DEFAULT_FUNCTION_BUTTONS = [
  { key: 'add', label: '新增账户', enabled: true },
  { key: 'config', label: '页面配置', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]

const SEARCH_FIELDS: SearchFieldDef[] = [
  { key: 'keyword', label: '筛选条件', type: 'input', placeholder: '账户名称/账户编号/开户银行/银行账号' },
  { key: 'accountType', label: '账户类型', type: 'select', options: ACCOUNT_TYPE_OPTIONS, placeholder: '全部类型' },
  { key: 'accountLevel', label: '账户等级', type: 'select', options: ACCOUNT_LEVEL_OPTIONS, placeholder: '全部等级' },
  {
    key: 'status', label: '状态', type: 'select', placeholder: '全部状态',
    options: [
      { label: '启用', value: 1 },
      { label: '停用', value: 0 },
    ],
  },
  { key: 'currency', label: '币种', type: 'select', options: CURRENCY_OPTIONS, placeholder: '全部币种' },
]

const searchValues = reactive<Record<string, any>>({
  keyword: '',
  accountType: undefined,
  accountLevel: undefined,
  status: undefined,
  currency: undefined,
})

const queryFieldConfig = ref(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))

const visibleSearchFields = computed(() =>
  SEARCH_FIELDS.filter(f => {
    const c = queryFieldConfig.value.find(q => q.key === f.key)
    return c ? c.visible : true
  })
)

function fnEnabled(key: string): boolean {
  const c = functionButtonConfig.value.find(f => f.key === key)
  return c ? c.enabled : true
}

// ═══ 统计卡片（走后端 /statistics，与列表同口径过滤） ═══
const statCards = computed<StatCardItem[]>(() => {
  const s = statSummary.value || {}
  return [
    { label: '账户总数', value: Number(s.total) || 0, suffix: '个' },
    { label: '启用账户', value: Number(s.enabled) || 0, suffix: '个' },
    { label: '停用账户', value: Number(s.disabled) || 0, suffix: '个' },
    { label: '余额合计', value: Number(s.totalBalance) || 0, precision: 2, prefix: '¥' },
  ]
})

// ═══ 数据表列（对标 9 列 + 账户编号/备注/创建时间可选） ═══
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '账户名称', key: 'accountName', type: 'slot', slotName: 'nameCell', width: 190 },
  { title: '账户编号', key: 'subjectCode', width: 110 },
  { title: '账户类型', key: 'accountType', type: 'slot', slotName: 'accountTypeCell', width: 110 },
  { title: '账户等级', key: 'accountLevel', type: 'slot', slotName: 'accountLevelCell', width: 110 },
  { title: '开户银行', key: 'bankName', width: 150 },
  { title: '银行账号', key: 'bankAccount', width: 180 },
  { title: '账户余额', key: 'balance', type: 'slot', slotName: 'balanceCell', width: 140, align: 'right' },
  { title: '币种', key: 'currency', width: 80 },
  { title: '状态', key: 'status', type: 'slot', slotName: 'statusCell', width: 90 },
  { title: '备注', key: 'remark', width: 180, defaultHidden: true },
  { title: '创建时间', key: 'createTime', width: 160, defaultHidden: true },
  { title: '操作', key: 'action', type: 'action', slotName: 'actionCell', width: 250, fixed: 'right' },
]

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 查询参数拼装（列表与统计同口径） ═══
function buildFilterParams(): PaymentAccountQuery {
  const params: PaymentAccountQuery = {}
  if (searchValues.keyword) params.keyword = String(searchValues.keyword).trim()
  if (searchValues.accountType != null) params.accountType = Number(searchValues.accountType)
  if (searchValues.accountLevel != null) params.accountLevel = Number(searchValues.accountLevel)
  if (searchValues.currency) params.currency = String(searchValues.currency)
  if (searchValues.status != null) {
    params.status = Number(searchValues.status)
  } else {
    // 状态=全部：显式要求同时返回停用数据（否则后端默认仅启用）
    params.showDisabled = 1
  }
  return params
}

function buildParams(): PaymentAccountQuery {
  return {
    ...buildFilterParams(),
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
}

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await financeAccountApi.page(buildParams())
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[支付账户] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

async function fetchStatistics() {
  try {
    const res: any = await financeAccountApi.getStatistics(buildFilterParams())
    statSummary.value = res || {}
  } catch (error) {
    console.warn('[支付账户] 加载统计失败', error)
    statSummary.value = {}
  }
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  fetchStatistics()
  fetchList()
}

function handleReset() {
  searchValues.keyword = ''
  searchValues.accountType = undefined
  searchValues.accountLevel = undefined
  searchValues.status = undefined
  searchValues.currency = undefined
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 新增 / 修改 ═══
const modalVisible = ref(false)
const formRef = ref()
const formState = reactive({
  id: null as number | null,
  accountName: '',
  subjectCode: '',
  accountType: 1 as number | undefined,
  accountLevel: undefined as number | undefined,
  bankName: '',
  bankAccount: '',
  currency: 'CNY',
  openingBalance: 0 as number | undefined,
  remark: '',
})

const formRules = {
  accountName: [{ required: true, message: '请输入账户名称', trigger: 'blur' }],
  subjectCode: [{ required: true, message: '请输入账户编号', trigger: 'blur' }],
  accountType: [{ required: true, message: '请选择账户类型', trigger: 'change' }],
}

function resetForm() {
  formState.id = null
  formState.accountName = ''
  formState.subjectCode = ''
  formState.accountType = 1
  formState.accountLevel = undefined
  formState.bankName = ''
  formState.bankAccount = ''
  formState.currency = 'CNY'
  formState.openingBalance = 0
  formState.remark = ''
}

async function handleAdd() {
  resetForm()
  modalVisible.value = true
  try {
    const code: any = await financeAccountApi.nextCode()
    formState.subjectCode = typeof code === 'string' ? code : (code?.data || '')
  } catch (error) {
    console.warn('[支付账户] 生成账户编号失败', error)
  }
}

async function handleEdit(record: PaymentAccountInfo) {
  resetForm()
  modalVisible.value = true
  try {
    const detail: any = await financeAccountApi.detail(record.id)
    const d = detail?.data ?? detail
    if (!d) return
    formState.id = d.id
    formState.accountName = d.accountName || ''
    formState.subjectCode = d.subjectCode || ''
    formState.accountType = d.accountType ?? 1
    formState.accountLevel = d.accountLevel ?? undefined
    formState.bankName = d.bankName || ''
    formState.bankAccount = d.bankAccount || ''
    formState.currency = d.currency || 'CNY'
    formState.remark = d.remark || ''
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载账户详情失败')
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  const opBalance = Number(formState.openingBalance) || 0
  const payload: Partial<PaymentAccountInfo> = {
    accountName: formState.accountName.trim(),
    subjectCode: formState.subjectCode.trim(),
    accountType: formState.accountType,
    accountLevel: formState.accountLevel,
    bankName: formState.bankName,
    bankAccount: formState.bankAccount,
    currency: formState.currency,
    remark: formState.remark,
  }

  saving.value = true
  try {
    if (formState.id) {
      await financeAccountApi.update(formState.id, payload)
      message.success('修改成功')
    } else {
      const created: any = await financeAccountApi.create(payload)
      const newId = created?.id ?? created?.data?.id
      // 期初余额：新增后按增量登记（后端写入口不直接接受 balance，避免越权改账）
      if (newId && opBalance !== 0) {
        await financeAccountApi.updateBalance(newId, opBalance)
      }
      message.success('新增成功')
    }
    modalVisible.value = false
    handleSearch()
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 余额调整 ═══
const balanceVisible = ref(false)
const balanceSaving = ref(false)
const balanceTarget = ref<PaymentAccountInfo | null>(null)
const balanceAmount = ref<number | undefined>(undefined)

const previewBalance = computed(() =>
  (Number(balanceTarget.value?.balance) || 0) + (Number(balanceAmount.value) || 0)
)

function openBalance(record: PaymentAccountInfo) {
  balanceTarget.value = record
  balanceAmount.value = undefined
  balanceVisible.value = true
}

async function handleBalanceSave() {
  const amount = Number(balanceAmount.value)
  if (!amount) {
    message.warning('请输入调整金额（正数增加、负数减少）')
    return
  }
  balanceSaving.value = true
  try {
    await financeAccountApi.updateBalance(balanceTarget.value!.id, amount)
    message.success('余额调整成功')
    balanceVisible.value = false
    handleSearch()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '余额调整失败')
  } finally {
    balanceSaving.value = false
  }
}

// ═══ 删除 / 启停 ═══
function handleDelete(record: PaymentAccountInfo) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除支付账户「${record.accountName}」吗？系统预置账户、存在下级账户或余额不为 0 的账户不可删除。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await financeAccountApi.remove(record.id)
        message.success('删除成功')
        handleSearch()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

function handleToggleStatus(record: PaymentAccountInfo) {
  const target = record.status === 1 ? 0 : 1
  const actionText = target === 1 ? '启用' : '停用'
  Modal.confirm({
    title: '确认',
    content: `确定要${actionText}账户「${record.accountName}」吗？`,
    onOk: async () => {
      try {
        await financeAccountApi.updateStatus(record.id, target)
        message.success(`已${actionText}`)
        handleSearch()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '操作失败')
      }
    },
  })
}

// ═══ 导出（后端真实 xlsx） ═══
async function handleExport() {
  exporting.value = true
  try {
    const blob: any = await financeAccountApi.export(buildParams())
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `支付账户_${dayjs().format('YYYY-MM-DD')}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML + 新开窗口打印，现在交给 PrintDialog：列与行由页面给，模板负责版式。
// 列取自原打印表格的 <th>（原「#」行号列由模板/引擎处理，不再由页面拼）。
const printColumns: any[] = [
  { title: '账户名称', key: 'accountName' },
  { title: '账户编号', key: 'subjectCode' },
  { title: '账户类型', key: 'accountType', formatter: (v: any) => ACCOUNT_TYPE_MAP[v]?.label || '' },
  { title: '账户等级', key: 'accountLevel', formatter: (v: any) => ACCOUNT_LEVEL_MAP[v]?.label || '' },
  { title: '开户银行', key: 'bankName' },
  { title: '银行账号', key: 'bankAccount' },
  // 账户余额是数值语义列：原样传数值，模板用 digits 显示（前端格式化会让模板求不了和）
  { title: '账户余额', key: 'balance' },
  { title: '币种', key: 'currency' },
  { title: '状态', key: 'status', formatter: (v: any) => (v === 1 ? '启用' : '停用') },
]

const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'md-payment-account',
  title: '支付账户',
  rows: () => tableData.value,
  columns: () => printColumns,
  // 原打印抬头的一行元信息（打印时间由模板 pageHeader 负责）
  totalText: () => `筛选条件：${searchValues.keyword || '全部'}，记录数：${tableData.value.length}`,
  emptyTip: '没有可打印的数据',
})

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
  // 弹窗内 Enter 保存（多行文本域不触发）
  if (e.key === 'Enter' && modalVisible.value && !e.ctrlKey && !e.altKey && !e.metaKey) {
    const target = e.target as HTMLElement | null
    if (target && target.tagName === 'TEXTAREA') return
    e.preventDefault()
    handleSave()
  }
}

// ═══ 页面配置 ═══
function loadPageConfig() {
  const fields = DEFAULT_QUERY_FIELDS.map(f => ({ ...f }))
  const buttons = DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b }))
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed.queryFields)) {
        fields.forEach(df => {
          const saved = parsed.queryFields.find((f: any) => f.key === df.key)
          if (saved) df.visible = saved.visible !== false
        })
      }
      if (Array.isArray(parsed.functionButtons)) {
        buttons.forEach(b => {
          const saved = parsed.functionButtons.find((f: any) => f.key === b.key)
          if (saved) b.enabled = saved.enabled !== false
        })
      }
    }
  } catch {
    // ignore
  }
  queryFieldConfig.value = fields
  functionButtonConfig.value = buttons
}

function handlePageConfigChange(config: any) {
  const queryFields = config.queryFields || []
  const functions = config.functionButtons || []
  queryFieldConfig.value = DEFAULT_QUERY_FIELDS.map(f => {
    const saved = queryFields.find((q: any) => q.key === f.key)
    return saved ? { ...f, ...saved } : { ...f }
  })
  functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(b => {
    const saved = functions.find((q: any) => q.key === b.key)
    return saved ? { ...b, ...saved } : { ...b }
  })
}

// ═══ 工具 ═══
function formatMoney(val: number | string | null | undefined): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function handleError(error: Error) {
  console.error('[支付账户] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadPageConfig()
  fetchStatistics()
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.toolbar-title-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.list-title {
  font-size: 14px;
  font-weight: 600;
}

.search-area {
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}
.search-grid {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.search-field-item {
  display: flex;
  align-items: center;
  min-width: 0;
}
.search-select-wrap {
  display: flex;
  align-items: center;
  width: 100%;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: #fff;
}
.search-select-wrap:hover {
  border-color: #4096ff;
}
.search-select-label {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  white-space: nowrap;
  flex-shrink: 0;
  padding-left: 8px;
}
.search-select-wrap :deep(.ant-select) {
  flex: 1;
  min-width: 118px;
}
.search-select-wrap :deep(.ant-input) {
  flex: 1;
  min-width: 210px;
  border: none !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  background: transparent;
}
.search-select-wrap :deep(.ant-select .ant-select-selector) {
  border: none !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  display: flex;
  align-items: center;
}

.table-area {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.stat-cards {
  flex-shrink: 0;
  margin-bottom: 8px;
}

.cell-link {
  color: #1890ff;
  cursor: pointer;
}
.cell-link:hover {
  text-decoration: underline;
}
.text-danger {
  color: #ff4d4f;
  font-weight: 600;
}

/* 橙色新增按钮（资料模块统一） */
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-add:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}

.balance-desc {
  margin-bottom: 12px;
}
.balance-form {
  margin-top: 4px;
}

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
</style>
