<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="false"
      >
        <!-- ═══ 工具栏：刷新 / 打印(F8) / 导出 / 明细对账（列配置齿轮在表头 rowNo 列） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip :title="balanceText">
              <a-tag :color="balanced ? 'green' : 'red'" class="balance-tag">
                {{ balanced ? '试算平衡' : '试算不平衡' }}
              </a-tag>
            </a-tooltip>
            <a-button size="small" :loading="loading" @click="handleSearch">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button size="small" @click="handlePrint">
              <PrinterOutlined /> 打印
            </a-button>
            <a-button size="small" :disabled="!tableData.length" @click="handleExport">
              <ExportOutlined /> 导出
            </a-button>
            <a-button size="small" @click="openDetailModal">
              <ProfileOutlined /> 明细对账
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（6 项）：查询方案 / 会计月(起) / 会计月(止)* / 科目层级 / 科目 / 显示无数据科目 ═══ -->
        <template #search-fields>
          <div class="tb-search-area">
            <div class="tb-search-row">
              <div class="tb-search-item">
                <span class="tb-search-label">查询方案</span>
                <a-select
                  v-model:value="schemeName"
                  style="width: 150px"
                  size="small"
                  allow-clear
                  placeholder="--查询方案--"
                  :options="schemeOptions"
                  @change="applyScheme"
                />
                <a-button type="link" size="small" @click="openSchemeModal">
                  <SaveOutlined /> 存为方案
                </a-button>
                <a-button
                  v-if="schemeName"
                  type="link"
                  size="small"
                  danger
                  @click="removeScheme"
                >
                  <DeleteOutlined /> 删除
                </a-button>
              </div>
              <div class="tb-search-item">
                <span class="tb-search-label">会计月(起)</span>
                <a-date-picker
                  v-model:value="startMonth"
                  picker="month"
                  size="small"
                  style="width: 120px"
                  value-format="YYYY-MM"
                  :allow-clear="false"
                  @change="handleSearch"
                />
              </div>
              <div class="tb-search-item">
                <span class="tb-search-label required">会计月(止)</span>
                <a-date-picker
                  v-model:value="endMonth"
                  picker="month"
                  size="small"
                  style="width: 120px"
                  value-format="YYYY-MM"
                  :allow-clear="false"
                  @change="handleSearch"
                />
              </div>
              <div class="tb-search-item">
                <span class="tb-search-label">科目层级</span>
                <a-select
                  v-model:value="subjectLevel"
                  style="width: 90px"
                  size="small"
                  :options="levelOptions"
                  @change="handleSearch"
                />
              </div>
              <div class="tb-search-item">
                <span class="tb-search-label">科目</span>
                <a-input
                  v-model:value="subjectKeyword"
                  placeholder="科目编码/名称"
                  style="width: 160px"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div class="tb-search-item">
                <a-checkbox v-model:checked="showNoData" @change="handleSearch">
                  显示无数据科目
                </a-checkbox>
              </div>
              <div class="tb-search-item">
                <a-button type="primary" size="small" :loading="loading" @click="handleSearch">
                  <SearchOutlined /> 查询
                </a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表：序号 + 10 列（科目类型/科目编码/科目名称/层级/期初余额/本期发生额借·贷/本年累计借·贷/期末余额）+ 合计行 ═══ -->
        <template #table>
          <div class="tb-table-area">
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
              :min-empty-rows="0"
              :summary-data="footerColumns"
              storage-key="finance-trial-balance-columns"
              @refresh="handleSearch"
            >
              <template #subjectNameCell="{ record }">
                <a class="subject-link" @click="gotoDetailLedger(record)">
                  {{ record.subjectName }}
                </a>
              </template>
              <template #openingBalanceCell="{ record }">
                <span class="num-value">{{ formatBalance(record) }}</span>
              </template>
              <template #closingBalanceCell="{ record }">
                <span class="num-value">{{ formatClosing(record) }}</span>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 存为方案 ═══ -->
    <a-modal
      v-model:open="schemeModalOpen"
      title="存为查询方案"
      :width="420"
      @ok="saveScheme"
    >
      <a-input
        v-model:value="newSchemeName"
        placeholder="请输入方案名称"
        @press-enter="saveScheme"
      />
      <div class="scheme-tip">保存当前查询条件（会计月起止 / 科目层级 / 科目 / 显示无数据科目）</div>
    </a-modal>

    <!-- ═══ 明细对账（科目 → 明细账穿透） ═══ -->
    <a-modal
      v-model:open="detailModalOpen"
      title="明细对账"
      :width="460"
      ok-text="查看明细账"
      @ok="confirmDetailModal"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="会计月">
          <span>{{ startMonth }} ~ {{ endMonth }}</span>
        </a-form-item>
        <a-form-item label="科目">
          <a-select
            v-model:value="detailSubjectId"
            show-search
            allow-clear
            placeholder="请选择要穿透的科目"
            option-filter-prop="label"
            style="width: 100%"
            :options="detailSubjectOptions"
          />
        </a-form-item>
      </a-form>
      <div class="scheme-tip">选择科目后跳转《明细账》，按期初 + 逐笔发生额核对余额</div>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined, PrinterOutlined, ExportOutlined, ProfileOutlined,
  SearchOutlined, SaveOutlined, DeleteOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { reportApi } from '@/api/finance'
import { exportCsv } from '@/utils/exportCsv'

defineOptions({ name: 'FinanceTrialBalance' })

const router = useRouter()

// ═══ 查询条件（查询方案 / 会计月(起) / 会计月(止)* / 科目层级 / 科目 / 显示无数据科目） ═══
const currentMonth = dayjs().format('YYYY-MM')
const startMonth = ref<string>(currentMonth)
const endMonth = ref<string>(currentMonth)
const subjectLevel = ref<number>(1)
const subjectKeyword = ref<string>('')
const showNoData = ref<boolean>(false)

const levelOptions = [
  { label: '一级', value: 1 },
  { label: '二级', value: 2 },
  { label: '三级', value: 3 },
  { label: '四级', value: 4 },
]

// ═══ 查询方案（本地保存，可复用） ═══
const SCHEME_STORAGE_KEY = 'finance-trial-balance-schemes'
interface QueryScheme {
  name: string
  startMonth: string
  endMonth: string
  subjectLevel: number
  subjectKeyword: string
  showNoData: boolean
}
const schemes = ref<QueryScheme[]>([])
const schemeName = ref<string | undefined>(undefined)
const schemeModalOpen = ref(false)
const newSchemeName = ref('')
const schemeOptions = computed(() => schemes.value.map(s => ({ label: s.name, value: s.name })))

function loadSchemes() {
  try {
    const raw = localStorage.getItem(SCHEME_STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : []
    schemes.value = Array.isArray(parsed) ? parsed : []
  } catch {
    schemes.value = []
  }
}

function openSchemeModal() {
  newSchemeName.value = ''
  schemeModalOpen.value = true
}

function saveScheme() {
  const name = newSchemeName.value.trim()
  if (!name) {
    message.warning('请输入方案名称')
    return
  }
  const scheme: QueryScheme = {
    name,
    startMonth: startMonth.value,
    endMonth: endMonth.value,
    subjectLevel: subjectLevel.value,
    subjectKeyword: subjectKeyword.value,
    showNoData: showNoData.value,
  }
  const idx = schemes.value.findIndex(s => s.name === name)
  if (idx > -1) {
    schemes.value.splice(idx, 1, scheme)
  } else {
    schemes.value.push(scheme)
  }
  localStorage.setItem(SCHEME_STORAGE_KEY, JSON.stringify(schemes.value))
  schemeName.value = name
  schemeModalOpen.value = false
  message.success(`查询方案「${name}」已保存`)
}

function applyScheme(name?: string) {
  if (!name) return
  const scheme = schemes.value.find(s => s.name === name)
  if (!scheme) return
  startMonth.value = scheme.startMonth
  endMonth.value = scheme.endMonth
  subjectLevel.value = scheme.subjectLevel ?? 1
  subjectKeyword.value = scheme.subjectKeyword || ''
  showNoData.value = !!scheme.showNoData
  handleSearch()
}

function removeScheme() {
  const name = schemeName.value
  if (!name) return
  schemes.value = schemes.value.filter(s => s.name !== name)
  localStorage.setItem(SCHEME_STORAGE_KEY, JSON.stringify(schemes.value))
  schemeName.value = undefined
  message.success(`查询方案「${name}」已删除`)
}

// ═══ 表格列（对齐对标 10 列；本期发生额/本年累计为分组表头） ═══
const moneyFormatter = (val: any) => {
  if (val === null || val === undefined || val === '') return '0.00'
  const num = Number(val)
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '科目类型', field: 'subjectTypeName', key: 'subjectTypeName', width: 90, align: 'center' },
  { title: '科目编码', field: 'subjectCode', key: 'subjectCode', width: 110 },
  {
    title: '科目名称', field: 'subjectName', key: 'subjectName', width: 220,
    type: 'slot', slotName: 'subjectNameCell',
  },
  { title: '层级', field: 'level', key: 'level', width: 60, align: 'center' },
  {
    title: '期初余额', field: 'openingBalance', key: 'openingBalance', width: 140,
    align: 'right', type: 'slot', slotName: 'openingBalanceCell',
  },
  {
    title: '本期发生额', key: 'groupPeriod', children: [
      { title: '借方', field: 'periodDebit', key: 'periodDebit', width: 130, align: 'right', formatter: moneyFormatter },
      { title: '贷方', field: 'periodCredit', key: 'periodCredit', width: 130, align: 'right', formatter: moneyFormatter },
    ],
  },
  {
    title: '本年累计', key: 'groupYear', children: [
      { title: '借方', field: 'yearDebit', key: 'yearDebit', width: 130, align: 'right', formatter: moneyFormatter },
      { title: '贷方', field: 'yearCredit', key: 'yearCredit', width: 130, align: 'right', formatter: moneyFormatter },
    ],
  },
  {
    title: '期末余额', field: 'closingBalance', key: 'closingBalance', width: 140,
    align: 'right', type: 'slot', slotName: 'closingBalanceCell',
  },
]

/** 余额展示：方向 + 绝对值（0 显示 -） */
function formatBalance(record: any): string {
  return formatDirectionBalance(record.openingBalance, record.openingDirection)
}
function formatClosing(record: any): string {
  return formatDirectionBalance(record.closingBalance, record.closingDirection)
}
function formatDirectionBalance(balance: any, direction: any): string {
  const num = Number(balance)
  if (!num) return '-'
  const text = Math.abs(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  return `${direction || (num < 0 ? '贷' : '借')} ${text}`
}

// ═══ 数据加载（无分页，一次性返回科目行 + 合计行） ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const summary = ref<any>(null)

async function fetchData() {
  const startYear = Number((startMonth.value || '').slice(0, 4))
  const endYear = Number((endMonth.value || '').slice(0, 4))
  const startPeriod = Number((startMonth.value || '').slice(5, 7))
  const endPeriod = Number((endMonth.value || '').slice(5, 7))
  if (!startYear || !endYear || !startPeriod || !endPeriod) {
    message.warning('请选择会计月(起)与会计月(止)')
    return
  }
  if (startYear !== endYear) {
    message.warning('会计月(起)与会计月(止)需在同一会计年度内')
    return
  }
  if (startPeriod > endPeriod) {
    message.warning('会计月(起)不能晚于会计月(止)')
    return
  }

  loading.value = true
  try {
    const res: any = await reportApi.getTrialBalancePage({
      fiscalYear: endYear,
      startPeriod,
      endPeriod,
      subjectLevel: subjectLevel.value,
      subjectKeyword: subjectKeyword.value || undefined,
      showNoData: showNoData.value,
    })
    tableData.value = res?.records || []
    summary.value = res?.summary || null
  } catch (err: any) {
    console.warn('[科目余额表] 加载失败', err)
    message.error(err?.response?.data?.message || '查询科目余额表失败')
    tableData.value = []
    summary.value = null
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  fetchData()
}

// ═══ 合计行（借贷合计 + 试算平衡校验） ═══
const balanced = computed(() => !!summary.value?.periodBalanced && !!summary.value?.yearBalanced)
const balanceText = computed(() => {
  if (!summary.value) return '暂无数据'
  return balanced.value
    ? '试算平衡：Σ本期借方 = Σ本期贷方，Σ本年累计借方 = Σ本年累计贷方'
    : '试算不平衡：请检查凭证借贷是否守恒'
})

const footerColumns = computed(() => {
  const s = summary.value
  if (!s) return []
  return [
    { key: 'openingBalance', value: `借 ${moneyFormatter(s.openingDebit)} / 贷 ${moneyFormatter(s.openingCredit)}` },
    { key: 'periodDebit', value: moneyFormatter(s.periodDebit) },
    { key: 'periodCredit', value: moneyFormatter(s.periodCredit) },
    { key: 'yearDebit', value: moneyFormatter(s.yearDebit) },
    { key: 'yearCredit', value: moneyFormatter(s.yearCredit) },
    { key: 'closingBalance', value: `借 ${moneyFormatter(s.closingDebit)} / 贷 ${moneyFormatter(s.closingCredit)}` },
  ]
})

// ═══ 明细对账：科目 → 明细账穿透 ═══
const detailModalOpen = ref(false)
const detailSubjectId = ref<string | undefined>(undefined)
const detailSubjectOptions = computed(() =>
  tableData.value
    .filter(r => r.subjectCode)
    .map(r => ({ label: `${r.subjectCode} ${r.subjectName}`, value: r.subjectCode })),
)

function openDetailModal() {
  if (!tableData.value.length) {
    message.warning('请先查询出科目数据')
    return
  }
  detailSubjectId.value = undefined
  detailModalOpen.value = true
}

function confirmDetailModal() {
  const code = detailSubjectId.value
  const record = tableData.value.find(r => r.subjectCode === code)
  detailModalOpen.value = false
  gotoDetailLedger(record || { subjectCode: code })
}

/** 跳转《明细账》并带上科目与会计月起止（账簿勾稽：科目余额表 → 明细账） */
function gotoDetailLedger(record: any) {
  if (!record?.subjectCode) {
    message.warning('请选择要穿透的科目')
    return
  }
  router.push({
    path: '/finance/detail-ledger',
    query: {
      subjectCode: record.subjectCode,
      dateStart: `${startMonth.value}-01`,
      dateEnd: dayjs(`${endMonth.value}-01`).endOf('month').format('YYYY-MM-DD'),
    },
  })
}

// ═══ 导出（当前条件全量科目行 + 合计行） ═══
function handleExport() {
  const s = summary.value
  const headers = ['科目类型', '科目编码', '科目名称', '层级', '期初余额', '本期发生额-借方', '本期发生额-贷方', '本年累计-借方', '本年累计-贷方', '期末余额']
  const rows: (string | number)[][] = tableData.value.map(r => [
    r.subjectTypeName ?? '',
    r.subjectCode ?? '',
    r.subjectName ?? '',
    r.level ?? '',
    formatBalance(r),
    moneyFormatter(r.periodDebit),
    moneyFormatter(r.periodCredit),
    moneyFormatter(r.yearDebit),
    moneyFormatter(r.yearCredit),
    formatClosing(r),
  ])
  if (s) {
    rows.push([
      '合计', '', '', '',
      `借 ${moneyFormatter(s.openingDebit)} / 贷 ${moneyFormatter(s.openingCredit)}`,
      moneyFormatter(s.periodDebit),
      moneyFormatter(s.periodCredit),
      moneyFormatter(s.yearDebit),
      moneyFormatter(s.yearCredit),
      `借 ${moneyFormatter(s.closingDebit)} / 贷 ${moneyFormatter(s.closingCredit)}`,
    ])
  }
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  exportCsv(headers, rows, `科目余额表_${startMonth.value}_${endMonth.value}`)
  message.success('导出成功')
}

// ═══ 打印（F8）：独立窗口输出分组表头 + 合计行 ═══
function handlePrint() {
  if (!tableData.value.length) {
    message.warning('没有可打印的数据')
    return
  }
  const s = summary.value
  const bodyRows = tableData.value.map(r => `
    <tr>
      <td>${r.subjectTypeName ?? ''}</td>
      <td>${r.subjectCode ?? ''}</td>
      <td>${r.subjectName ?? ''}</td>
      <td class="c">${r.level ?? ''}</td>
      <td class="r">${formatBalance(r)}</td>
      <td class="r">${moneyFormatter(r.periodDebit)}</td>
      <td class="r">${moneyFormatter(r.periodCredit)}</td>
      <td class="r">${moneyFormatter(r.yearDebit)}</td>
      <td class="r">${moneyFormatter(r.yearCredit)}</td>
      <td class="r">${formatClosing(r)}</td>
    </tr>`).join('')
  const totalRow = s ? `
    <tr class="total">
      <td colspan="4">合计</td>
      <td class="r">借 ${moneyFormatter(s.openingDebit)} / 贷 ${moneyFormatter(s.openingCredit)}</td>
      <td class="r">${moneyFormatter(s.periodDebit)}</td>
      <td class="r">${moneyFormatter(s.periodCredit)}</td>
      <td class="r">${moneyFormatter(s.yearDebit)}</td>
      <td class="r">${moneyFormatter(s.yearCredit)}</td>
      <td class="r">借 ${moneyFormatter(s.closingDebit)} / 贷 ${moneyFormatter(s.closingCredit)}</td>
    </tr>` : ''

  const html = `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>科目余额表</title>
<style>
  body { font-family: "Microsoft YaHei", Arial, sans-serif; font-size: 12px; color: #262626; }
  h2 { text-align: center; margin: 0 0 4px; }
  .meta { text-align: center; color: #666; margin-bottom: 10px; }
  table { width: 100%; border-collapse: collapse; }
  th, td { border: 1px solid #999; padding: 3px 6px; }
  th { background: #f2f2f2; text-align: center; }
  td.c { text-align: center; }
  td.r { text-align: right; }
  tr.total td { font-weight: 700; background: #fafafa; }
</style>
</head>
<body>
  <h2>科目余额表</h2>
  <div class="meta">会计月：${startMonth.value} ~ ${endMonth.value} ｜ 科目层级：${subjectLevel.value} 级 ｜ 制表时间：${dayjs().format('YYYY-MM-DD HH:mm')}</div>
  <table>
    <thead>
      <tr>
        <th rowspan="2">科目类型</th>
        <th rowspan="2">科目编码</th>
        <th rowspan="2">科目名称</th>
        <th rowspan="2">层级</th>
        <th rowspan="2">期初余额</th>
        <th colspan="2">本期发生额</th>
        <th colspan="2">本年累计</th>
        <th rowspan="2">期末余额</th>
      </tr>
      <tr><th>借方</th><th>贷方</th><th>借方</th><th>贷方</th></tr>
    </thead>
    <tbody>${bodyRows}${totalRow}</tbody>
  </table>
  <div class="meta" style="margin-top:8px;">${balanceText.value}</div>
</body>
</html>`

  const win = window.open('', '_blank', 'width=1280,height=800')
  if (!win) {
    message.warning('浏览器阻止了打印窗口，请允许弹出窗口后重试')
    return
  }
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

// ═══ 快捷键 F8 = 打印 ═══
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

function handleError(error: Error) {
  console.error('[科目余额表] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  loadSchemes()
  fetchData()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.tb-search-area {
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}
.tb-search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.tb-search-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.tb-search-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}
.tb-search-label.required::after {
  content: '*';
  color: #ff4d4f;
  margin-left: 2px;
}
.tb-table-area {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.balance-tag {
  margin-right: 2px;
  cursor: default;
}
.num-value {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-variant-numeric: tabular-nums;
}
.subject-link {
  color: #1677ff;
  cursor: pointer;
}
.subject-link:hover {
  text-decoration: underline;
}
.scheme-tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}
:deep(.ant-input-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
</style>
