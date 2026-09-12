<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="false"
      >
        <!-- ═══ 工具栏：刷新 / 打印(F8) / 导出 / 明细对账（列配置齿轮在表头 rowNo 列，本页无页面配置弹窗） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button size="small" :loading="loading" @click="handleSearch">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button size="small" @click="handlePrint">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button size="small" :disabled="!tableData.length" @click="handleExport">
              <ExportOutlined /> 导出
            </a-button>
            <a-button size="small" @click="openDetailModal">
              <ProfileOutlined /> 明细对账
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（7 项）：查询方案 / 会计月(起) / 会计月(止)* / 科目 / 核算项 / 无本期发生额不显示 / 余额为0不显示 ═══ -->
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
                <span class="tb-search-label">科目</span>
                <a-tree-select
                  v-model:value="subjectId"
                  :tree-data="subjectSelectTree"
                  :field-names="{ label: 'title', value: 'value', children: 'children' }"
                  show-search
                  allow-clear
                  tree-default-expand-all
                  :filter-option="filterSubjectOption"
                  size="small"
                  style="width: 200px"
                  placeholder="科目"
                  @change="handleSearch"
                />
              </div>
              <div class="tb-search-item">
                <span class="tb-search-label">核算项</span>
                <a-select
                  v-model:value="auxType"
                  style="width: 120px"
                  size="small"
                  :options="AUX_TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div class="tb-search-item">
                <a-checkbox v-model:checked="hideNoPeriodAmount" @change="handleSearch">
                  无本期发生额不显示
                </a-checkbox>
              </div>
              <div class="tb-search-item">
                <a-checkbox v-model:checked="hideZeroBalance" @change="handleSearch">
                  余额为0不显示
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

        <!-- ═══ 数据表：序号 + 12 列（科目编码/科目名称/核算项编码/核算项名称 + 期初·本期·本年·期末 各借贷）+ 合计行 ═══ -->
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
              storage-key="finance-aux-balance-columns"
              @refresh="handleSearch"
            >
              <template #subjectNameCell="{ record }">
                <a class="subject-link" @click="gotoDetailLedger(record)">
                  {{ record.subjectName }}
                </a>
              </template>
              <template #auxNameCell="{ record }">
                <a class="subject-link" @click="gotoDetailLedger(record)">
                  {{ record.auxName }}
                </a>
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
      <div class="scheme-tip">保存当前查询条件（会计月起止 / 科目 / 核算项 / 两个显示口径）</div>
    </a-modal>

    <!-- ═══ 明细对账（辅助核算余额 → 明细账穿透） ═══ -->
    <a-modal
      v-model:open="detailModalOpen"
      title="明细对账"
      :width="480"
      ok-text="查看明细账"
      @ok="confirmDetailModal"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="会计月">
          <span>{{ startMonth }} ~ {{ endMonth }}</span>
        </a-form-item>
        <a-form-item label="科目">
          <a-tree-select
            v-model:value="detailSubjectId"
            :tree-data="subjectSelectTree"
            :field-names="{ label: 'title', value: 'value', children: 'children' }"
            show-search
            allow-clear
            tree-default-expand-all
            :filter-option="filterSubjectOption"
            size="small"
            style="width: 100%"
            placeholder="请选择要穿透的科目"
          />
        </a-form-item>
        <a-form-item label="核算项">
          <span>{{ currentAuxTypeLabel }}</span>
        </a-form-item>
      </a-form>
      <div class="scheme-tip">选择科目后跳转《明细账》，按期初 + 逐笔发生额核对核算项余额</div>
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
import { auxiliaryBalanceApi, ledgerApi } from '@/api/finance'
import { exportCsv } from '@/utils/exportCsv'

defineOptions({ name: 'FinanceAuxBalance' })

const router = useRouter()

// ═══ 核算项维度（对标：往来单位/供应商/客户/职员/部门） ═══
const AUX_TYPE_OPTIONS = [
  { label: '往来单位', value: 'PARTNER' },
  { label: '供应商', value: 'SUPPLIER' },
  { label: '客户', value: 'CUSTOMER' },
  { label: '职员', value: 'EMPLOYEE' },
  { label: '部门', value: 'DEPT' },
]

// ═══ 查询条件（查询方案 / 会计月(起) / 会计月(止)* / 科目 / 核算项 / 无本期发生额不显示 / 余额为0不显示） ═══
const currentMonth = dayjs().format('YYYY-MM')
const startMonth = ref<string>(currentMonth)
const endMonth = ref<string>(currentMonth)
const subjectId = ref<number | undefined>(undefined)
const auxType = ref<string>('PARTNER')
const hideNoPeriodAmount = ref<boolean>(false)
const hideZeroBalance = ref<boolean>(false)

const currentAuxTypeLabel = computed(
  () => AUX_TYPE_OPTIONS.find(o => o.value === auxType.value)?.label || '往来单位',
)

// ═══ 查询方案（本地保存，可复用） ═══
const SCHEME_STORAGE_KEY = 'finance-aux-balance-schemes'
interface QueryScheme {
  name: string
  startMonth: string
  endMonth: string
  subjectId?: number
  auxType: string
  hideNoPeriodAmount: boolean
  hideZeroBalance: boolean
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
    subjectId: subjectId.value,
    auxType: auxType.value,
    hideNoPeriodAmount: hideNoPeriodAmount.value,
    hideZeroBalance: hideZeroBalance.value,
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
  subjectId.value = scheme.subjectId
  auxType.value = scheme.auxType || 'PARTNER'
  hideNoPeriodAmount.value = !!scheme.hideNoPeriodAmount
  hideZeroBalance.value = !!scheme.hideZeroBalance
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

// ═══ 科目下拉（科目分类树 → 仅保留具体科目节点） ═══
const subjectTree = ref<any[]>([])
const subjectSelectTree = computed(() => {
  const build = (nodes: any[]): any[] => {
    const result: any[] = []
    for (const node of nodes || []) {
      if (node.subjectId != null) {
        result.push({ value: node.subjectId, title: node.categoryName, children: build(node.children) })
      } else {
        result.push(...build(node.children))
      }
    }
    return result
  }
  return build(subjectTree.value)
})

async function fetchSubjectTree() {
  try {
    const res: any = await ledgerApi.getSubjectTree()
    subjectTree.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (error) {
    console.warn('[辅助核算余额表] 加载科目树失败', error)
    subjectTree.value = []
  }
}

function filterSubjectOption(input: string, option: any) {
  const text = option?.title || option?.label || ''
  return String(text).toLowerCase().includes(input.toLowerCase())
}

// ═══ 表格列（对标 12 列：科目编码/科目名称/核算项编码/核算项名称 + 期初·本期·本年·期末 借贷四段） ═══
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '科目编码', field: 'subjectCode', key: 'subjectCode', width: 110 },
  {
    title: '科目名称', field: 'subjectName', key: 'subjectName', width: 160,
    type: 'slot', slotName: 'subjectNameCell',
  },
  { title: '核算项编码', field: 'auxCode', key: 'auxCode', width: 120 },
  {
    title: '核算项名称', field: 'auxName', key: 'auxName', width: 180,
    type: 'slot', slotName: 'auxNameCell',
  },
  {
    title: '期初余额', key: 'groupBegin', children: [
      { title: '借方', field: 'beginDebit', key: 'beginDebit', width: 120, align: 'right', formatter: moneyFormatter },
      { title: '贷方', field: 'beginCredit', key: 'beginCredit', width: 120, align: 'right', formatter: moneyFormatter },
    ],
  },
  {
    title: '本期发生额', key: 'groupPeriod', children: [
      { title: '借方', field: 'periodDebit', key: 'periodDebit', width: 120, align: 'right', formatter: moneyFormatter },
      { title: '贷方', field: 'periodCredit', key: 'periodCredit', width: 120, align: 'right', formatter: moneyFormatter },
    ],
  },
  {
    title: '本年累计', key: 'groupYear', children: [
      { title: '借方', field: 'yearDebit', key: 'yearDebit', width: 120, align: 'right', formatter: moneyFormatter },
      { title: '贷方', field: 'yearCredit', key: 'yearCredit', width: 120, align: 'right', formatter: moneyFormatter },
    ],
  },
  {
    title: '期末余额', key: 'groupEnd', children: [
      { title: '借方', field: 'endDebit', key: 'endDebit', width: 120, align: 'right', formatter: moneyFormatter },
      { title: '贷方', field: 'endCredit', key: 'endCredit', width: 120, align: 'right', formatter: moneyFormatter },
    ],
  },
]

function moneyFormatter(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据加载（报表无分页：单次取全量 + 表尾合计） ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const summary = ref<any>(null)

function buildParams() {
  return {
    startMonth: startMonth.value || undefined,
    endMonth: endMonth.value || undefined,
    subjectId: subjectId.value ?? undefined,
    auxType: auxType.value,
    hideNoPeriodAmount: hideNoPeriodAmount.value,
    hideZeroBalance: hideZeroBalance.value,
    pageNum: 1,
    pageSize: 9999,
  }
}

async function fetchData() {
  if (!startMonth.value || !endMonth.value) {
    message.warning('请选择会计月(起)与会计月(止)')
    return
  }
  loading.value = true
  try {
    const res: any = await auxiliaryBalanceApi.getPage(buildParams())
    tableData.value = res?.records || []
    summary.value = res?.summary || null
  } catch (err: any) {
    console.warn('[辅助核算余额表] 加载失败', err)
    message.error(err?.response?.data?.message || '查询辅助核算余额表失败')
    tableData.value = []
    summary.value = null
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  fetchData()
}

// ═══ 合计行（全量口径：四段余额逐列加总） ═══
const footerColumns = computed(() => {
  const s = summary.value
  if (!s) return []
  return [
    { key: 'beginDebit', value: moneyFormatter(s.beginDebit) },
    { key: 'beginCredit', value: moneyFormatter(s.beginCredit) },
    { key: 'periodDebit', value: moneyFormatter(s.periodDebit) },
    { key: 'periodCredit', value: moneyFormatter(s.periodCredit) },
    { key: 'yearDebit', value: moneyFormatter(s.yearDebit) },
    { key: 'yearCredit', value: moneyFormatter(s.yearCredit) },
    { key: 'endDebit', value: moneyFormatter(s.endDebit) },
    { key: 'endCredit', value: moneyFormatter(s.endCredit) },
  ]
})

/** 余额勾稽展示：期初借 − 期初贷 + 本期借 − 本期贷 = 期末借 − 期末贷 */
const balancedText = computed(() => {
  const s = summary.value
  if (!s) return ''
  const n = (v: any) => Number(v || 0)
  const left = n(s.beginDebit) - n(s.beginCredit) + n(s.periodDebit) - n(s.periodCredit)
  const right = n(s.endDebit) - n(s.endCredit)
  return `勾稽校验：期初(${moneyFormatter(left)}) = 期末(${moneyFormatter(right)})`
})

// ═══ 明细对账：辅助核算余额 → 明细账穿透 ═══
const detailModalOpen = ref(false)
const detailSubjectId = ref<number | undefined>(undefined)

function openDetailModal() {
  if (!tableData.value.length) {
    message.warning('请先查询出余额数据')
    return
  }
  detailSubjectId.value = subjectId.value
  detailModalOpen.value = true
}

function confirmDetailModal() {
  const id = detailSubjectId.value
  const record = tableData.value.find(r => r.subjectId === id) || { subjectId: id }
  detailModalOpen.value = false
  gotoDetailLedger(record)
}

/** 跳转《明细账》并带上科目 + 会计月起止 + 当前核算项（账簿勾稽：辅助核算余额表 → 明细账） */
function gotoDetailLedger(record: any) {
  if (!record?.subjectId) {
    message.warning('请选择要穿透的科目')
    return
  }
  const query: Record<string, string> = {
    subjectId: String(record.subjectId),
    dateStart: `${startMonth.value}-01`,
    dateEnd: dayjs(`${endMonth.value}-01`).endOf('month').format('YYYY-MM-DD'),
  }
  if (auxType.value === 'PARTNER' || auxType.value === 'CUSTOMER' || auxType.value === 'SUPPLIER') {
    if (record.auxName) query.settleUnit = record.auxName
  } else if (auxType.value === 'EMPLOYEE') {
    if (record.auxName) query.settleStaff = record.auxName
  } else if (auxType.value === 'DEPT') {
    if (record.auxName) query.settleDept = record.auxName
  }
  router.push({ path: '/finance/detail-ledger', query })
}

// ═══ 导出（当前条件全量核算项行 + 合计行） ═══
function handleExport() {
  const s = summary.value
  const headers = [
    '科目编码', '科目名称', '核算项编码', '核算项名称',
    '期初余额-借方', '期初余额-贷方',
    '本期发生额-借方', '本期发生额-贷方',
    '本年累计-借方', '本年累计-贷方',
    '期末余额-借方', '期末余额-贷方',
  ]
  const rows: (string | number)[][] = tableData.value.map(r => [
    r.subjectCode ?? '',
    r.subjectName ?? '',
    r.auxCode ?? '',
    r.auxName ?? '',
    moneyFormatter(r.beginDebit),
    moneyFormatter(r.beginCredit),
    moneyFormatter(r.periodDebit),
    moneyFormatter(r.periodCredit),
    moneyFormatter(r.yearDebit),
    moneyFormatter(r.yearCredit),
    moneyFormatter(r.endDebit),
    moneyFormatter(r.endCredit),
  ])
  if (s) {
    rows.push([
      '合计', '', '', '',
      moneyFormatter(s.beginDebit), moneyFormatter(s.beginCredit),
      moneyFormatter(s.periodDebit), moneyFormatter(s.periodCredit),
      moneyFormatter(s.yearDebit), moneyFormatter(s.yearCredit),
      moneyFormatter(s.endDebit), moneyFormatter(s.endCredit),
    ])
  }
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  exportCsv(headers, rows, `辅助核算余额表_${startMonth.value}_${endMonth.value}`)
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
      <td>${r.subjectCode ?? ''}</td>
      <td>${r.subjectName ?? ''}</td>
      <td>${r.auxCode ?? ''}</td>
      <td>${r.auxName ?? ''}</td>
      <td class="r">${moneyFormatter(r.beginDebit)}</td>
      <td class="r">${moneyFormatter(r.beginCredit)}</td>
      <td class="r">${moneyFormatter(r.periodDebit)}</td>
      <td class="r">${moneyFormatter(r.periodCredit)}</td>
      <td class="r">${moneyFormatter(r.yearDebit)}</td>
      <td class="r">${moneyFormatter(r.yearCredit)}</td>
      <td class="r">${moneyFormatter(r.endDebit)}</td>
      <td class="r">${moneyFormatter(r.endCredit)}</td>
    </tr>`).join('')
  const totalRow = s ? `
    <tr class="total">
      <td colspan="4">合计</td>
      <td class="r">${moneyFormatter(s.beginDebit)}</td>
      <td class="r">${moneyFormatter(s.beginCredit)}</td>
      <td class="r">${moneyFormatter(s.periodDebit)}</td>
      <td class="r">${moneyFormatter(s.periodCredit)}</td>
      <td class="r">${moneyFormatter(s.yearDebit)}</td>
      <td class="r">${moneyFormatter(s.yearCredit)}</td>
      <td class="r">${moneyFormatter(s.endDebit)}</td>
      <td class="r">${moneyFormatter(s.endCredit)}</td>
    </tr>` : ''

  const html = `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>辅助核算余额表</title>
<style>
  body { font-family: "Microsoft YaHei", Arial, sans-serif; font-size: 12px; color: #262626; }
  h2 { text-align: center; margin: 0 0 4px; }
  .meta { text-align: center; color: #666; margin-bottom: 10px; }
  table { width: 100%; border-collapse: collapse; }
  th, td { border: 1px solid #999; padding: 3px 6px; }
  th { background: #f2f2f2; text-align: center; }
  td.r { text-align: right; }
  tr.total td { font-weight: 700; background: #fafafa; }
</style>
</head>
<body>
  <h2>辅助核算余额表</h2>
  <div class="meta">会计月：${startMonth.value} ~ ${endMonth.value} ｜ 核算项：${currentAuxTypeLabel.value} ｜ 制表时间：${dayjs().format('YYYY-MM-DD HH:mm')}</div>
  <table>
    <thead>
      <tr>
        <th rowspan="2">科目编码</th>
        <th rowspan="2">科目名称</th>
        <th rowspan="2">核算项编码</th>
        <th rowspan="2">核算项名称</th>
        <th colspan="2">期初余额</th>
        <th colspan="2">本期发生额</th>
        <th colspan="2">本年累计</th>
        <th colspan="2">期末余额</th>
      </tr>
      <tr><th>借方</th><th>贷方</th><th>借方</th><th>贷方</th><th>借方</th><th>贷方</th><th>借方</th><th>贷方</th></tr>
    </thead>
    <tbody>${bodyRows}${totalRow}</tbody>
  </table>
  <div class="meta" style="margin-top:8px;">${balancedText.value}</div>
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
  console.error('[辅助核算余额表] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  loadSchemes()
  fetchSubjectTree()
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
