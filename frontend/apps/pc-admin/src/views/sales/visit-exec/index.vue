<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        拜访执行（CRM → 外勤拜访 → 拜访执行，菜单 70002）
        · CRM 为本系统独有模块（ql361 无 CRM 域），按业界生产级 CRM 的「计划-执行-检视」模型建模
        · 规格：docs/Yh-Spec/手动整理对标开发文档/crm模块/拜访执行开发文档.md
        · 路线 A：ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillDetailTable
        · 本轮由路线 B（ARReportPage）整体重写为路线 A，并补：
            ① 签到打卡采集浏览器定位（写 crm_visit_record.longitude/latitude，两列原本全链路闲置）
               —— 定位不可用/被拒绝时留空，不写任何假数据；
            ② 「关联计划」列（plan_id 此前不可见，无法区分计划拜访与临时拜访）；
            ③ 编辑历史记录时把已完成计划并入下拉，避免下拉回显裸 ID；
            ④ 删除返回值校验、导出/打印套中文文案。
        · 未做：服务端时间戳、地理围栏校验、现场照片（attachments 列闲置）—— 属后端能力缺口，见报告
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：签到打卡 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('checkIn')"
            type="primary"
            size="small"
            class="btn-add"
            @click="openCheckIn"
          >
            <EnvironmentOutlined /> 签到打卡
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
                v-if="isFieldVisible('result')"
                class="search-item"
              >
                <span class="search-label">拜访结果</span>
                <a-select
                  v-model:value="searchForm.result"
                  placeholder="全部结果"
                  size="small"
                  allow-clear
                  :options="resultOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('visitDateRange')"
                class="search-item"
              >
                <span class="search-label">拜访日期</span>
                <a-range-picker
                  v-model:value="searchForm.visitDateRange"
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
              storage-key="crm-visit-record-table-columns"
              global-config-key="crm-visit-record-table-columns"
            >
              <template #planCell="{ record }">
                <span v-if="record.__ghost" />
                <span
                  v-else
                  class="cell-ellipsis"
                  :title="planLabelById(record.planId)"
                >{{ planLabelById(record.planId) }}</span>
              </template>

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

              <template #visitTypeCell="{ record }">
                <a-tag
                  v-if="!record.__ghost && record.visitType"
                  :color="VISIT_TYPE_MAP[record.visitType]?.color || 'default'"
                >
                  {{ VISIT_TYPE_MAP[record.visitType]?.label || '-' }}
                </a-tag>
                <span v-else-if="!record.__ghost">-</span>
                <span v-else />
              </template>

              <template #visitTimeCell="{ record }">
                <span v-if="record.__ghost" />
                <span v-else>{{ formatTime(record.visitTime) }}</span>
              </template>

              <template #locationCell="{ record }">
                <a-tooltip
                  v-if="!record.__ghost && record.location"
                  :title="record.location"
                  placement="bottom"
                >
                  <span class="cell-ellipsis">{{ record.location }}</span>
                </a-tooltip>
                <span v-else-if="!record.__ghost">-</span>
                <span v-else />
              </template>

              <template #contentCell="{ record }">
                <a-tooltip
                  v-if="!record.__ghost && record.content"
                  :title="record.content"
                  placement="bottom"
                >
                  <span class="cell-ellipsis">{{ record.content }}</span>
                </a-tooltip>
                <span v-else-if="!record.__ghost">-</span>
                <span v-else />
              </template>

              <template #resultCell="{ record }">
                <a-tag
                  v-if="!record.__ghost && record.result"
                  :color="RESULT_MAP[record.result]?.color || 'default'"
                >
                  {{ RESULT_MAP[record.result]?.label || '-' }}
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
                    title="确认删除该拜访记录？"
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

      <!-- ═══ 签到打卡 / 编辑拜访记录弹窗 ═══ -->
      <a-modal
        v-model:open="modalOpen"
        :title="editingId ? '编辑拜访记录' : '签到打卡'"
        :confirm-loading="saving"
        width="600px"
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
            label="关联计划"
            name="planId"
          >
            <a-select
              v-model:value="form.planId"
              placeholder="无计划临时拜访可不选"
              allow-clear
              show-search
              option-filter-prop="label"
              :options="planOptions"
              @change="handlePlanChange"
            />
          </a-form-item>
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
            label="拜访方式"
            name="visitType"
          >
            <a-select
              v-model:value="form.visitType"
              placeholder="请选择拜访方式"
              :options="visitTypeOptions"
            />
          </a-form-item>
          <a-form-item
            label="打卡时间"
            name="visitTime"
          >
            <a-date-picker
              v-model:value="form.visitTime"
              show-time
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
              placeholder="默认当前时间"
            />
          </a-form-item>
          <!-- 定位采集：只读展示，需用户显式点击「获取定位」；浏览器拿不到定位时如实提示并留空 -->
          <a-form-item label="定位">
            <div class="loc-row">
              <span
                class="loc-text"
                :class="{ 'loc-fail': geoStatus === 'fail' }"
              >
                {{ geoText }}
              </span>
              <a-button
                size="small"
                :loading="geoStatus === 'loading'"
                @click="locate"
              >
                获取定位
              </a-button>
            </div>
          </a-form-item>
          <a-form-item
            label="拜访地点"
            name="location"
          >
            <a-input
              v-model:value="form.location"
              placeholder="请输入拜访地点"
            />
          </a-form-item>
          <a-form-item
            label="拜访内容"
            name="content"
          >
            <a-textarea
              v-model:value="form.content"
              :rows="3"
              placeholder="请输入拜访内容"
            />
          </a-form-item>
          <a-form-item
            label="拜访结果"
            name="result"
          >
            <a-select
              v-model:value="form.result"
              placeholder="请选择拜访结果"
              :options="resultOptions"
            />
          </a-form-item>
          <a-form-item
            label="下一步行动"
            name="nextAction"
          >
            <a-input
              v-model:value="form.nextAction"
              placeholder="请输入下一步行动（选填）"
            />
          </a-form-item>
          <a-form-item
            label="下次拜访日期"
            name="nextVisitDate"
          >
            <a-date-picker
              v-model:value="form.nextVisitDate"
              value-format="YYYY-MM-DD"
              style="width: 100%"
              placeholder="请选择下次拜访日期（选填）"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
  EnvironmentOutlined,
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
import {
  visitPlanApi, visitRecordApi, crmCustomerApi,
  type VisitPlan, type VisitRecord
} from '@/api/crm'
import { userApi } from '@/api/user'

defineOptions({ name: 'SalesVisitExec' })

// ═══ 拜访方式/结果（与后端 VisitRecord 注释一致） ═══
const VISIT_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '上门', color: 'blue' },
  2: { label: '电话', color: 'cyan' },
  3: { label: '其他', color: 'default' }
}
const RESULT_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '有意向', color: 'green' },
  2: { label: '一般', color: 'orange' },
  3: { label: '无意向', color: 'red' }
}
/** 两个下拉选项由 MAP 反推（字典单一真源） */
const visitTypeOptions = Object.entries(VISIT_TYPE_MAP).map(([value, v]) => ({
  label: v.label,
  value: Number(value)
}))
const resultOptions = Object.entries(RESULT_MAP).map(([value, v]) => ({
  label: v.label,
  value: Number(value)
}))

function formatTime(val: string | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

// ═══ 下拉选项 ═══
const customerOptions = ref<{ label: string; value: number }[]>([])
const salesPersonOptions = ref<{ label: string; value: number }[]>([])
/** 待执行/执行中的计划，供打卡关联（后端 status 只支持单值，故取第 1 页 200 条后客户端过滤） */
const activePlans = ref<VisitPlan[]>([])
/** 编辑历史记录时，其关联计划可能已完成而不在待执行列表中 → 单独并入，避免下拉回显裸 ID */
const extraPlans = ref<VisitPlan[]>([])

function filterOption(input: string, option: any): boolean {
  return String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())
}

function planLabel(p: VisitPlan): string {
  const no = p.planNo || (p.id != null ? `计划#${p.id}` : '计划')
  return `${no}｜${p.customerName || ''}｜${p.planDate || ''}`
}

const planOptions = computed(() => {
  const seen = new Set<string>()
  const list: { label: string; value: number }[] = []
  for (const p of [...activePlans.value, ...extraPlans.value]) {
    const key = String(p.id ?? '')
    if (!key || seen.has(key)) continue
    seen.add(key)
    list.push({ label: planLabel(p), value: p.id as number })
  }
  return list
})

/**
 * 列表「关联计划」列取值：按计划 id 反查标签。
 * ⚠️ 雪花 ID 精度陷阱：id 一律按字符串比对，禁止 Number(id) 做 Map key。
 */
const planLabelMap = computed(() => {
  const map = new Map<string, string>()
  for (const item of planOptions.value) map.set(String(item.value), item.label)
  return map
})
function planLabelById(planId: number | undefined): string {
  if (planId === undefined || planId === null) return '临时拜访'
  return planLabelMap.value.get(String(planId)) || `计划#${planId}`
}

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'customerId', label: '客户', visible: true },
  { key: 'salesPersonId', label: '负责人', visible: true },
  { key: 'result', label: '拜访结果', visible: true },
  { key: 'visitDateRange', label: '拜访日期', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'checkIn', label: '签到打卡', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]
const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))
// ⚠️ 键名刻意避开 `exec`：`SecurityAspect` 的参数敏感词检查会把含 `exec` 的路径参数
// 判为非法字符并直接返回 400，导致列配置/页面配置的持久化请求全部失败
// （2026-09-18 真机实测：`GET /system/user-config/col-config/crm-visit-exec-table-columns` → 400）。
// 改用与后端表 `crm_visit_record` 一致的名字。
const pageConfigStorageKey = 'crm-visit-record-page-config'

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
  result: undefined as number | undefined,
  visitDateRange: undefined as [string, string] | undefined,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const loading = ref(false)
const tableData = ref<any[]>([])

// ═══ 表格列（rowNo 承载列配置齿轮；action 为锁定列） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 110, fixed: 'left' },
  // 关联计划：plan_id 是这张表的核心外键（决定计划是否被回写完成），此前未上列
  { key: 'planId', title: '关联计划', type: 'slot', slotName: 'planCell', width: 200 },
  { key: 'customerName', title: '客户', type: 'slot', slotName: 'customerCell', width: 170 },
  { key: 'salesPersonName', title: '负责人', type: 'input', width: 100 },
  { key: 'visitType', title: '拜访方式', type: 'slot', slotName: 'visitTypeCell', width: 90 },
  { key: 'visitTime', title: '打卡时间', type: 'slot', slotName: 'visitTimeCell', width: 130 },
  { key: 'location', title: '拜访地点', type: 'slot', slotName: 'locationCell', width: 180 },
  { key: 'content', title: '拜访内容', type: 'slot', slotName: 'contentCell', width: 220 },
  { key: 'result', title: '拜访结果', type: 'slot', slotName: 'resultCell', width: 90 },
  { key: 'nextAction', title: '下一步行动', type: 'input', width: 150, defaultHidden: true },
  { key: 'nextVisitDate', title: '下次拜访', type: 'input', width: 110, defaultHidden: true },
  // 定位坐标：打卡时采集（longitude/latitude 两列原本全链路闲置），默认隐藏
  {
    key: 'longitude',
    title: '定位坐标',
    type: 'input',
    width: 180,
    defaultHidden: true,
    // ⚠️ BillDetailTable 的 formatter 是位置参数 (raw, record)，不可写成解构
    formatter: (raw: any, record: any) => (
      raw !== undefined && raw !== null && record?.latitude !== undefined && record?.latitude !== null
        ? `${raw}, ${record.latitude}`
        : '-'
    )
  },
]

// ═══ 数据加载（GET /crm/visit/record/page，page/size 风格，后端同时兼容 pageNum/pageSize） ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await visitRecordApi.page({
      page: pagination.current,
      size: pagination.pageSize,
      customerId: searchForm.customerId,
      salesPersonId: searchForm.salesPersonId,
      result: searchForm.result,
      visitDateStart: searchForm.visitDateRange?.[0],
      visitDateEnd: searchForm.visitDateRange?.[1],
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[拜访执行] 加载列表失败', error)
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
  searchForm.result = undefined
  searchForm.visitDateRange = undefined
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

// ═══ 签到打卡 / 编辑弹窗 ═══
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  planId: undefined as number | undefined,
  customerId: undefined as number | undefined,
  salesPersonId: undefined as number | undefined,
  visitType: 1 as number,
  visitTime: dayjs().format('YYYY-MM-DDTHH:mm:ss') as string,
  location: '',
  content: '',
  result: undefined as number | undefined,
  nextAction: '',
  nextVisitDate: undefined as string | undefined,
  longitude: undefined as number | undefined,
  latitude: undefined as number | undefined,
})
const form = reactive(emptyForm())

const rules: Record<string, any> = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  salesPersonId: [{ required: true, message: '请选择负责人', trigger: 'change' }],
  visitType: [{ required: true, message: '请选择拜访方式', trigger: 'change' }],
  content: [{ required: true, message: '请输入拜访内容', trigger: 'blur' }],
  result: [{ required: true, message: '请选择拜访结果', trigger: 'change' }]
}

// ═══ 定位采集（浏览器 Geolocation；拿不到就留空，不造假数据） ═══
const geoStatus = ref<'idle' | 'loading' | 'ok' | 'fail'>('idle')
const geoHint = ref('')

const geoText = computed(() => {
  if (geoStatus.value === 'ok' && form.longitude != null && form.latitude != null) {
    return `已采集：${form.longitude}, ${form.latitude}`
  }
  if (geoStatus.value === 'loading') return '定位采集中…'
  if (geoStatus.value === 'fail') return `定位不可用（${geoHint.value}），将不记录坐标`
  if (form.longitude != null && form.latitude != null) {
    return `记录坐标：${form.longitude}, ${form.latitude}`
  }
  return '未采集定位（浏览器未授权或设备无定位能力）'
})

function locate() {
  if (!('geolocation' in navigator)) {
    geoStatus.value = 'fail'
    geoHint.value = '当前浏览器不支持定位'
    return
  }
  geoStatus.value = 'loading'
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      // 保留 6 位小数（约 0.1m 精度），与 crm_visit_record.longitude/latitude 数值列对应
      form.longitude = Number(pos.coords.longitude.toFixed(6))
      form.latitude = Number(pos.coords.latitude.toFixed(6))
      geoStatus.value = 'ok'
      geoHint.value = ''
    },
    (err) => {
      // 采不到定位时不写入任何坐标 → 后端保持 null，不用「0,0」之类的假值占位
      geoStatus.value = 'fail'
      geoHint.value = err.code === 1
        ? '定位权限被拒绝'
        : err.code === 3 ? '定位超时' : '定位服务不可用'
    },
    { enableHighAccuracy: true, timeout: 8000, maximumAge: 60000 }
  )
}

function resetForm(data?: Partial<VisitRecord>) {
  Object.assign(form, emptyForm(), data || {})
}

/** 关联计划后自动带出客户/负责人/地点 */
function handlePlanChange(planId: number | undefined) {
  const plan = planOptions.value.length
    ? [...activePlans.value, ...extraPlans.value].find(p => p.id === planId)
    : undefined
  if (plan) {
    form.customerId = plan.customerId
    form.salesPersonId = plan.salesPersonId
    if (plan.address && !form.location) form.location = plan.address
  }
}

function openCheckIn() {
  editingId.value = null
  resetForm()
  geoStatus.value = 'idle'
  geoHint.value = ''
  modalOpen.value = true
  // 打开弹窗即尝试采集定位（失败不阻塞打卡，仅提示并在提交时留空）
  locate()
}

function openEdit(record: VisitRecord) {
  editingId.value = record.id ?? null
  resetForm({
    planId: record.planId,
    customerId: record.customerId,
    salesPersonId: record.salesPersonId,
    visitType: record.visitType ?? 1,
    visitTime: record.visitTime || dayjs().format('YYYY-MM-DDTHH:mm:ss'),
    location: record.location || '',
    content: record.content || '',
    result: record.result,
    nextAction: record.nextAction || '',
    nextVisitDate: record.nextVisitDate,
    longitude: record.longitude,
    latitude: record.latitude
  })
  // 编辑不回写历史坐标：仅当该记录关联的计划不在待执行列表中时，并入下拉避免回显裸 ID
  if (record.planId != null) {
    const known = [...activePlans.value, ...extraPlans.value].some(p => String(p.id) === String(record.planId))
    if (!known) {
      extraPlans.value = [
        ...extraPlans.value,
        { id: record.planId, customerName: record.customerName, planDate: '' } as VisitPlan
      ]
    }
  }
  geoStatus.value = record.longitude != null && record.latitude != null ? 'ok' : 'idle'
  geoHint.value = ''
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
      await visitRecordApi.update(editingId.value, payload)
      message.success('拜访记录更新成功')
    } else {
      await visitRecordApi.checkIn(payload)
      message.success('打卡成功')
    }
    modalOpen.value = false
    fetchList()
    loadActivePlans()
  } catch (e: any) {
    console.warn('[拜访执行] 保存失败', e)
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 变更类接口走原生 axios 解包，未生效时返回裸 false（不抛错）→ 必须判返回值
async function handleDelete(record: VisitRecord) {
  try {
    const ok = await visitRecordApi.remove(record.id as number)
    if (ok === false) {
      message.warning('删除未生效：该记录可能已被删除，请刷新后重试')
    } else {
      message.success('删除成功')
    }
    fetchList()
  } catch (e: any) {
    console.warn('[拜访执行] 删除失败', e)
    message.error(e?.message || '删除失败')
  }
}

// ═══ 导出（套方式/结果的中文文案与时间格式） ═══
function exportCsv() {
  const rows = (tableData.value || []).filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const headers = ['关联计划', '客户', '负责人', '拜访方式', '打卡时间', '拜访地点',
    '拜访内容', '拜访结果', '下一步行动', '下次拜访']
  const lines = rows.map((r: any) => [
    planLabelById(r.planId), r.customerName, r.salesPersonName,
    VISIT_TYPE_MAP[r.visitType]?.label || '-', formatTime(r.visitTime), r.location,
    r.content, RESULT_MAP[r.result]?.label || '-', r.nextAction, r.nextVisitDate
  ])
  const escape = (v: any) => `"${String(v ?? '').replace(/"/g, '""')}"`
  const csv = '\uFEFF' + [headers, ...lines].map(row => row.map(escape).join(',')).join('\r\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `拜访执行_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
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
      <td>${escapeHtml(planLabelById(r.planId))}</td>
      <td>${escapeHtml(r.customerName || '')}</td>
      <td>${escapeHtml(r.salesPersonName || '')}</td>
      <td>${escapeHtml(VISIT_TYPE_MAP[r.visitType]?.label || '-')}</td>
      <td>${escapeHtml(formatTime(r.visitTime))}</td>
      <td>${escapeHtml(r.location || '')}</td>
      <td>${escapeHtml(r.content || '')}</td>
      <td>${escapeHtml(RESULT_MAP[r.result]?.label || '-')}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>拜访执行</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>拜访执行</h2>
    <div class="meta">
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr><th>#</th><th>关联计划</th><th>客户</th><th>负责人</th><th>拜访方式</th>
      <th>打卡时间</th><th>拜访地点</th><th>拜访内容</th><th>拜访结果</th></tr></thead>
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
  console.error('[拜访执行] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 下拉数据加载 ═══
async function loadActivePlans() {
  try {
    const res: any = await visitPlanApi.page({ page: 1, size: 200 })
    activePlans.value = (res?.records || []).filter((p: VisitPlan) => p.status === 0 || p.status === 1)
  } catch (e) {
    console.warn('[拜访执行] 计划下拉获取失败', e)
    message.warning('关联计划下拉加载失败，请稍后重试')
  }
}

onMounted(async () => {
  loadActivePlans()
  try {
    const list = await crmCustomerApi.dropdown()
    customerOptions.value = (Array.isArray(list) ? list : []).map((c: any) => ({
      label: c.name,
      value: c.id
    }))
  } catch (e) {
    console.warn('[拜访执行] 客户下拉获取失败', e)
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
    console.warn('[拜访执行] 负责人下拉获取失败', e)
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
/* 定位行：文案 + 按钮同行 */
.loc-row { display: flex; align-items: center; gap: 8px; }
.loc-text { font-size: 12px; color: #666; }
.loc-fail { color: #faad14; }

/* 橙色主按钮（CRM 模块统一） */
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
