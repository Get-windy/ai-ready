<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        会计期间（设置 → 财务设置 → 会计期间，菜单 70570 / set:accounting-period，单入口）
        · 对标 ql361「设置 → 财务设置 → 会计期间」实测形态：只读年度信息 + 年度下拉 + 固定 12 行矩阵（4 列）+ 底部保存
        · 路线 A 外壳：ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillDetailTable
          （原实现走路线 B 的 ARReportPage，该路线已明令不再新增 —— 本次升级为路线 A）
        · 本系统领先项一律保留：状态列 + 行内启停（带二次确认）、月结操作人 / 月结时间 / 备注（默认隐藏，可由表头齿轮开启）
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/设置模块/会计期间开发文档.md
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增期间（保留既有建期能力：新建年度时需先补期） ═══ -->
        <template #toolbar-left>
          <a-button
            v-permission="'finance:period:create'"
            type="primary"
            size="small"
            @click="openCreate"
          >
            <PlusOutlined /> 新增期间
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧：刷新 ═══ -->
        <template #toolbar-right>
          <a-tooltip
            title="刷新"
            placement="bottom"
          >
            <a-button
              size="small"
              :loading="loading"
              @click="handleRefresh"
            >
              <ReloadOutlined />
            </a-button>
          </a-tooltip>
        </template>

        <!-- ═══ 查询区（横向自适应网格，禁止纵向单列） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div
              ref="gridRef"
              class="search-grid"
            >
              <!-- 会计年：对标为「年度下拉切换」（切换后表格显示该年 12 期） -->
              <div class="search-field-item">
                <a-select
                  v-model:value="searchForm.periodYear"
                  size="small"
                  placeholder="会计年"
                  :options="yearOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                ref="actionRef"
                class="search-action-group"
                :style="{ gridColumn: 'span ' + actionSpan }"
              >
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

        <!-- ═══ 数据表：固定 12 期矩阵（不分页，理由见 :min-rows 处注释） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="rows"
              :columns="columns"
              :loading="loading"
              :min-rows="12"
              row-key="id"
              storage-key="set-accounting-period-table-columns"
              global-config-key="set-accounting-period-table-columns"
            >
              <!-- 期间号：对标显示「1期」，由 period_month 派生（period_code 为唯一键，另作默认隐藏列保留） -->
              <template #periodMonthCell="{ record }">
                <span v-if="!record.__ghost">{{ record.periodMonth }}期</span>
              </template>

              <!-- 天数：结账日期 - 起始日期 + 1，前端派生（开发文档 §7.3，不落库） -->
              <template #daysCell="{ record }">
                <span>{{ calcDays(record) }}</span>
              </template>

              <!-- 状态：本系统领先项（对标无），文案只用「开启 / 关闭」——不再等同「已月结」（月结是独立路径） -->
              <template #statusCell="{ record }">
                <a-tag :color="record.status === 1 ? 'green' : 'default'">
                  {{ record.status === 1 ? '开启' : '关闭' }}
                </a-tag>
              </template>

              <!-- 操作：启停翻转（保留本系统既有的二次确认与后果说明） -->
              <template #actionCell="{ record }">
                <a-popconfirm
                  :title="record.status === 1 ? '确认关闭该期间？' : '确认开启该期间？'"
                  :description="record.status === 1 ? `期间 ${record.periodCode} 关闭后不可录入/过账凭证` : `期间 ${record.periodCode} 将重新开启`"
                  :ok-text="record.status === 1 ? '关闭' : '开启'"
                  cancel-text="取消"
                  @confirm="toggleStatus(record)"
                >
                  <a-button
                    v-permission="'finance:period:update'"
                    type="link"
                    size="small"
                    :danger="record.status === 1"
                  >
                    {{ record.status === 1 ? '关闭' : '开启' }}
                  </a-button>
                </a-popconfirm>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <!-- ═══ 底部：保存（对标底部一个「保存」，批量提交本年度 12 期） ═══ -->
        <template #table-footer>
          <div class="matrix-footer">
            <a-button
              v-permission="'finance:period:update'"
              type="primary"
              :loading="saving"
              @click="handleSave"
            >
              保存
            </a-button>
            <span class="matrix-footer-tip">
              保存本年度各期的起始日期 / 结账日期（天数由起止日期自动派生）；已关闭的期间不可修改日期
            </span>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 新增期间弹窗（保留既有建期入口；起止日期与期间编码由后端按月自动生成） ═══ -->
    <a-modal
      v-model:open="modalOpen"
      title="新增会计期间"
      :confirm-loading="creating"
      width="480px"
      @ok="handleCreate"
    >
      <a-form
        ref="formRef"
        :model="form"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item
          label="会计年度"
          name="periodYear"
        >
          <a-input-number
            v-model:value="form.periodYear"
            :min="2000"
            :max="2099"
            :precision="0"
            style="width: 100%"
            placeholder="请输入会计年度"
          />
        </a-form-item>
        <a-form-item
          label="会计月份"
          name="periodMonth"
        >
          <a-select
            v-model:value="form.periodMonth"
            :options="monthOptions"
            placeholder="请选择会计月份"
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
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { accountingPeriodApi } from '@/api/finance'

defineOptions({ name: 'SetAccountingPeriod' })

// ═══ 状态 ═══
const loading = ref(false)
const saving = ref(false)
/** 当前年度的会计期间（固定 12 期矩阵，一年一页） */
const rows = ref<any[]>([])
/** 会计年下拉选项（来自库中真实存在的年度，不虚构年份） */
const yearOptions = ref<Array<{ label: string; value: number }>>([])

const searchForm = reactive<{ periodYear?: number }>({ periodYear: undefined })

// 查询区动作组自适应占位（横向网格）
const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

// ═══ 表格列 ═══
/**
 * 列清单 = 对标 4 列（期间号 / 起始日期 / 结账日期 / 天数）+ 本系统领先项（状态 / 期间编码 / 月结操作人 / 月结时间 / 备注）
 * · 金标准路线 A：首列 rowNo 承载表头齿轮（个人 / 全局列配置）
 * · 「起始日期 / 结账日期」行内可编（点击单元格进入原生日期选择器）
 * · 技术列默认隐藏，靠表头齿轮开启 —— 默认视图收敛到对标形态，同时不丢失既有能力
 */
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '序号', type: 'rowNo', width: 50, fixed: 'left' },
  { key: 'periodMonth', title: '期间号', type: 'slot', slotName: 'periodMonthCell', width: 100, align: 'center' },
  { key: 'startDate', title: '起始日期', type: 'date', width: 150 },
  { key: 'endDate', title: '结账日期', type: 'date', width: 150 },
  { key: 'days', title: '天数', type: 'slot', slotName: 'daysCell', width: 90, align: 'center' },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 90, align: 'center' },
  { key: 'periodCode', title: '期间编码', width: 110, readonly: true, defaultHidden: true },
  {
    key: 'closedBy',
    title: '月结操作人',
    width: 120,
    readonly: true,
    defaultHidden: true,
    formatter: (v: any) => (v ? String(v) : '-')
  },
  {
    key: 'closedTime',
    title: '月结时间',
    width: 170,
    readonly: true,
    defaultHidden: true,
    formatter: (v: any) => (v ? formatDateTime(v) : '-')
  },
  {
    key: 'remark',
    title: '备注',
    width: 200,
    readonly: true,
    defaultHidden: true,
    formatter: (v: any) => (v ? String(v) : '-')
  },
  { key: 'action', title: '操作', type: 'slot', slotName: 'actionCell', width: 90, fixed: 'right' }
]

// ═══ 格式化与派生 ═══
function formatDateTime(value?: string): string {
  if (!value) return '-'
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? String(value) : d.toLocaleString('zh-CN', { hour12: false })
}

function currentYear(): number {
  return new Date().getFullYear()
}

/** 日期归一化为「YYYY-MM-DD」：原生日期编辑器与派生天数都只认这个格式 */
function toDateStr(value: any): string {
  return String(value ?? '').slice(0, 10)
}

/** 「YYYY-MM-DD」→ UTC 时间戳（避开本地时区导致的跨天误差）；非法值返回 null */
function toUtcTs(value: any): number | null {
  const m = toDateStr(value).match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!m) return null
  return Date.UTC(Number(m[1]), Number(m[2]) - 1, Number(m[3]))
}

/**
 * 天数 = 结账日期 - 起始日期 + 1
 * 口径依开发文档 §7.3：前端派生，不新增数据库列（实测 2026-02-01~2026-02-28 → 28 天，与对标一致）
 */
function calcDays(record: any): string {
  const s = toUtcTs(record?.startDate)
  const e = toUtcTs(record?.endDate)
  if (s === null || e === null || e < s) return '-'
  return String(Math.round((e - s) / 86400000) + 1)
}

/** 默认年度：优先当前自然年，其次库中最大年度 */
function defaultYear(): number {
  const years = yearOptions.value.map(o => Number(o.value))
  if (years.includes(currentYear())) return currentYear()
  return years.length ? Math.max(...years) : currentYear()
}

// ═══ 数据加载 ═══
/** 会计年下拉：只列库中真实存在的年度（不虚构年份） */
async function loadYearOptions() {
  try {
    const list: any[] = (await accountingPeriodApi.getList()) || []
    const years = Array.from(
      new Set(list.map((r: any) => Number(r?.periodYear)).filter((y: number) => !Number.isNaN(y)))
    ).sort((a: number, b: number) => a - b)
    yearOptions.value = years.map((y: number) => ({ label: `${y}年`, value: y }))
  } catch (e) {
    console.error('[会计期间] 会计年度加载失败', e)
    yearOptions.value = []
  }
}

/**
 * 加载所选年度的会计期间
 * 不分页：一年固定 12 期（对标即「单年 12 行矩阵 + 底部保存」），页内即全年，无需翻页
 */
async function load() {
  loading.value = true
  try {
    const list: any[] = (await accountingPeriodApi.getList({ periodYear: searchForm.periodYear })) || []
    // 起止日期归一化为 YYYY-MM-DD（行内原生日期编辑器只接受该格式）
    rows.value = list.map((r: any) => ({
      ...r,
      startDate: toDateStr(r.startDate),
      endDate: toDateStr(r.endDate)
    }))
  } catch (e) {
    console.error('[会计期间] 加载失败', e)
    message.error('会计期间加载失败')
    rows.value = []
  } finally {
    loading.value = false
  }
}

async function handleRefresh() {
  await loadYearOptions()
  await load()
}

async function handleSearch() {
  await load()
}

/** 重置：回到默认年度（本页是年度矩阵，不支持「全部年度」——那会破坏 12 期矩阵） */
async function handleReset() {
  searchForm.periodYear = defaultYear()
  await load()
}

// ═══ 底部保存：批量提交本年度各期起止日期 ═══
async function handleSave() {
  // 只提交真实行（:min-rows 在年度期数不足时会填充 __ghost 占位行，占位行没有 id，不参与保存）
  const targets = rows.value.filter((r: any) => r && r.id && !r.__ghost)
  if (!targets.length) {
    message.warning('当前年度没有可保存的会计期间')
    return
  }
  for (const r of targets) {
    const s = toUtcTs(r.startDate)
    const e = toUtcTs(r.endDate)
    if (s === null || e === null) {
      message.error(`期间 ${r.periodMonth}期 的起始日期与结账日期不能为空`)
      return
    }
    if (s > e) {
      message.error(`期间 ${r.periodMonth}期 的起始日期不能晚于结账日期`)
      return
    }
  }
  saving.value = true
  try {
    // 雪花 ID 按字符串提交，禁止 Number(id)（超出 JS 安全整数范围会丢精度）
    await accountingPeriodApi.saveDates(
      targets.map((r: any) => ({
        id: String(r.id),
        startDate: String(r.startDate).slice(0, 10),
        endDate: String(r.endDate).slice(0, 10)
      }))
    )
    message.success('会计期间保存成功')
    await load()
  } catch (e: any) {
    console.error('[会计期间] 保存失败', e)
    message.error(e?.message || '会计期间保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 启停 ═══
async function toggleStatus(record: any) {
  const target = record.status === 1 ? 0 : 1
  try {
    await accountingPeriodApi.updateStatus(record.id, target)
    message.success(target === 1 ? `期间 ${record.periodCode} 已开启` : `期间 ${record.periodCode} 已关闭`)
    await load()
  } catch (e: any) {
    // 修 P1「失败静默」：原先只 console.warn，用户点了没反应
    console.error('[会计期间] 状态切换失败', e)
    message.error(e?.message || '状态切换失败')
  }
}

// ═══ 新增期间弹窗 ═══
const formRef = ref()
const modalOpen = ref(false)
const creating = ref(false)

const monthOptions = Array.from({ length: 12 }, (_, i) => ({ label: `${i + 1}月`, value: i + 1 }))

const emptyForm = () => ({
  periodYear: undefined as number | undefined,
  periodMonth: undefined as number | undefined,
  remark: ''
})
const form = reactive(emptyForm())

const rules: Record<string, any> = {
  periodYear: [{ required: true, message: '请输入会计年度', trigger: 'blur' }],
  periodMonth: [{ required: true, message: '请选择会计月份', trigger: 'change' }]
}

function openCreate() {
  // 先复位再开弹窗，避免上次的脏数据
  Object.assign(form, emptyForm())
  modalOpen.value = true
}

async function handleCreate() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  creating.value = true
  try {
    await accountingPeriodApi.create({
      periodYear: form.periodYear!,
      periodMonth: form.periodMonth!,
      remark: form.remark || undefined
    })
    message.success('会计期间创建成功')
    modalOpen.value = false
    await loadYearOptions()
    // 新建的年度可能不在当前筛选里，切到该年度便于立即看到结果
    searchForm.periodYear = form.periodYear!
    await load()
  } catch (e: any) {
    // 修 P1「失败静默」：原先只 console.warn
    console.error('[会计期间] 创建失败', e)
    message.error(e?.message || '会计期间创建失败')
  } finally {
    creating.value = false
  }
}

function handleError(error: Error) {
  console.error('[会计期间] 页面错误', error)
}

onMounted(async () => {
  await loadYearOptions()
  searchForm.periodYear = defaultYear()
  await nextTick()
  await load()
})
</script>

<style scoped>
/* 查询区：横向自适应网格（禁止纵向单列） */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 8px 12px; }
.search-field-item { min-width: 150px; }
.search-action-group { display: flex; align-items: center; justify-content: flex-end; gap: 8px; min-width: 150px; }

/* 表格区：flex 撑满剩余高度（BillDetailTable 不传 max-height，由本容器驱动高度） */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

/* 底部保存（对标：底部居中一个「保存」） */
.matrix-footer { display: flex; align-items: center; justify-content: center; gap: 12px; padding: 8px 16px; background: #fafafa; border-top: 1px solid #e8e8e8; }
.matrix-footer-tip { color: #999; font-size: 12px; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
