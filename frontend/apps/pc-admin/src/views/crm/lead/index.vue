<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        线索（CRM → 线索管理 → 线索，菜单 80210）
        · CRM 为本系统独有模块（ql361 无 CRM 域），按业界生产级 CRM（Odoo/SAP/金蝶/用友）建模
        · 规格：docs/Yh-Spec/手动整理对标开发文档/crm模块/线索开发文档.md
        · 路线 A：ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillDetailTable
        · 列配置齿轮在表头 rowNo 列（个人 + 全局）；页面配置弹窗控制查询条件显隐 + 功能按钮启用
        · 打印判 N/A：线索台账是跟进型数据、不是票据（《_开发指南-金标准》§七之二 第 7 项）→ 不挂打印入口
        · 本轮改造（换外壳 + 补金标准能力 + 修缺陷）：
          ① 外壳升级为路线 A（原为 BillTableList 平铺、ErrorBoundary 在 PageContainer 内部）
          ② 修 3 处 P0：列 formatter 由对象解构改位置参数 / 插槽列补 type:'slot' / action 列补 slotName
          ③ 字段名统一改后端实体名（leadStatus / leadName / contactPhone …）——原读 record.status 致
             状态列恒「未知」、转化为客户按钮恒不显示、4 张统计卡恒 0
          ④ 行操作「修改/删除/分配/转化为客户」与「批量分配/批量转化」真实接线（原分配/批量转化为桩）
          ⑤ 导出改为后端 /crm/lead/export 全量 + 中文映射；统计卡由「当前页前端聚合」改全量口径
          ⑥ 新增/编辑收敛到独立表单页 crm/lead/form（原内嵌弹窗字段名与实体不对齐，新增必失败）
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增 / 导入 / 批量分配 / 批量转化 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('create')"
              v-permission="'crm:lead:create'"
              type="primary"
              size="small"
              class="btn-add"
              @click="handleAdd"
            >
              <PlusOutlined /> 新建线索
            </a-button>
            <a-button
              v-if="isButtonEnabled('import')"
              v-permission="'crm:lead:import'"
              size="small"
              @click="handleImport"
            >
              <ImportOutlined /> 导入线索
            </a-button>
            <a-button
              v-if="isButtonEnabled('batchAssign')"
              v-permission="'crm:lead:edit'"
              size="small"
              :disabled="!selectedRows.length"
              @click="handleBatchAssign"
            >
              <TeamOutlined /> 批量分配{{ selectedRows.length ? `(${selectedRows.length})` : '' }}
            </a-button>
            <a-button
              v-if="isButtonEnabled('batchConvert')"
              v-permission="'crm:lead:batchconvert'"
              size="small"
              :disabled="!selectedRows.length"
              @click="handleBatchConvert"
            >
              <SwapRightOutlined /> 批量转化{{ selectedRows.length ? `(${selectedRows.length})` : '' }}
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：刷新（含自动刷新倒计时）/ 导出 / 页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <span
              v-if="isButtonEnabled('autoRefresh') && autoRefreshCountdown > 0"
              class="auto-refresh-badge"
            >
              <SyncOutlined /> {{ autoRefreshCountdown }}s
            </span>
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="handleRefresh"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="isButtonEnabled('export')"
              size="small"
              :loading="exporting"
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
                  placeholder="线索名称/公司/联系人"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('leadStatus')"
                class="search-item"
              >
                <span class="search-label">线索状态</span>
                <a-select
                  v-model:value="searchForm.leadStatus"
                  placeholder="全部状态"
                  size="small"
                  allow-clear
                  :options="LEAD_STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('leadLevel')"
                class="search-item"
              >
                <span class="search-label">线索等级</span>
                <a-select
                  v-model:value="searchForm.leadLevel"
                  placeholder="全部等级"
                  size="small"
                  allow-clear
                  :options="LEAD_LEVEL_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('salesPersonId')"
                class="search-item"
              >
                <span class="search-label">负责销售</span>
                <a-select
                  v-model:value="searchForm.salesPersonId"
                  placeholder="全部销售"
                  size="small"
                  show-search
                  allow-clear
                  :filter-option="filterOption"
                  :options="salesOptions"
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

        <!-- ═══ 统计卡 + 数据表 ═══ -->
        <template #table>
          <!-- 统计卡：取 /crm/lead/export 全量聚合（受当前查询条件约束），非当前页聚合 -->
          <div class="stat-strip">
            <div class="stat-card">
              <div class="stat-title">
                新线索
              </div>
              <div class="stat-value">
                {{ stats.newCount }}
              </div>
              <div class="stat-desc">
                待分配跟进
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-title">
                跟进中
              </div>
              <div class="stat-value">
                {{ stats.following }}
              </div>
              <div class="stat-desc">
                正在跟进
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-title">
                已转化
              </div>
              <div class="stat-value">
                {{ stats.converted }}
              </div>
              <div class="stat-desc">
                已转为 CRM 客户
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-title">
                预计金额
              </div>
              <div class="stat-value">
                {{ formatAmount(stats.estimatedAmount) }}
              </div>
              <div class="stat-desc">
                当前条件全量合计
              </div>
            </div>
          </div>

          <div class="table-area">
            <BillDetailTable
              ref="tableRef"
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="crm-lead-table-columns"
              global-config-key="crm-lead-table-columns"
              @checkbox-change="handleCheckboxChange"
              @checkbox-all="handleCheckboxAll"
            >
              <template #industryCell="{ record }">
                <span>{{ record.__ghost ? '' : dictText(INDUSTRY_TYPE_TEXT, record.industryType) }}</span>
              </template>

              <template #sourceCell="{ record }">
                <span>{{ record.__ghost ? '' : dictText(LEAD_SOURCE_TEXT, record.leadSource) }}</span>
              </template>

              <template #levelCell="{ record }">
                <a-tag
                  v-if="!record.__ghost && record.leadLevel"
                  :color="LEAD_LEVEL_COLOR[record.leadLevel] || 'default'"
                >
                  {{ dictText(LEAD_LEVEL_TEXT, record.leadLevel) }}
                </a-tag>
                <span v-else>-</span>
              </template>

              <template #amountCell="{ record }">
                <span>{{ record.__ghost ? '' : formatAmount(record.estimatedAmount) }}</span>
              </template>

              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost && record.leadStatus !== undefined && record.leadStatus !== null"
                  :color="LEAD_STATUS_COLOR[record.leadStatus] || 'default'"
                >
                  {{ leadStatusText(record) }}
                </a-tag>
                <span v-else>-</span>
              </template>

              <template #requirementCell="{ record }">
                <a-tooltip
                  v-if="!record.__ghost && record.requirement"
                  :title="record.requirement"
                  placement="bottom"
                >
                  <span class="cell-ellipsis">{{ record.requirement }}</span>
                </a-tooltip>
                <span v-else>-</span>
              </template>

              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    v-permission="'crm:lead:edit'"
                    type="link"
                    size="small"
                    @click="handleEdit(record)"
                  >
                    修改
                  </a-button>
                  <a-popconfirm
                    title="确定删除该线索？"
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
                  <a-button
                    v-permission="'crm:lead:edit'"
                    type="link"
                    size="small"
                    @click="handleAssign(record)"
                  >
                    分配
                  </a-button>
                  <a-button
                    v-if="canConvert(record)"
                    v-permission="'crm:lead:convert'"
                    type="link"
                    size="small"
                    @click="handleConvert(record)"
                  >
                    转化
                  </a-button>
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

      <!-- ═══ 分配线索弹窗（单条 / 批量共用） ═══
           后端无线索分配日志表，故只写 sales_person_id / sales_person_name，不收集备注（避免收集后丢弃） -->
      <a-modal
        v-model:open="assignVisible"
        :title="assignTargets.length > 1 ? `批量分配线索（${assignTargets.length} 条）` : '分配线索'"
        :confirm-loading="assignLoading"
        ok-text="确定"
        cancel-text="取消"
        :width="420"
        @ok="handleAssignConfirm"
      >
        <a-form layout="vertical">
          <a-form-item
            label="销售人员"
            required
          >
            <a-select
              v-model:value="assignForm.userId"
              placeholder="请选择销售人员"
              size="small"
              show-search
              allow-clear
              :filter-option="filterOption"
              :options="salesOptions"
            />
          </a-form-item>
          <div class="assign-tip">
            将把所选线索的「负责销售」写为上述人员；候选人取自平台用户列表。
          </div>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  DownloadOutlined,
  SettingOutlined,
  ImportOutlined,
  TeamOutlined,
  SwapRightOutlined,
  SyncOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { QueryFieldSetting, FunctionButtonSetting } from '@/components/PageConfigPanel/index.vue'
import { leadApi, leadConvertApi, type LeadQuery } from '@/api/crm'
import { optionsApi } from '@/api/options'
import { exportCsv } from '@/utils/exportCsv'

defineOptions({ name: 'CrmLead' })

const router = useRouter()

// ═══ 字典（与表单页 form.vue 逐字一致；来源/等级为数字值域，此前页面用的字符串 source 无对应库列） ═══
const LEAD_SOURCE_TEXT: Record<number, string> = { 1: '网络推广', 2: '客户介绍', 3: '电话营销', 4: '展会', 5: '其他' }
const LEAD_LEVEL_TEXT: Record<number, string> = { 1: '高', 2: '中', 3: '低' }
const LEAD_LEVEL_COLOR: Record<number, string> = { 1: 'red', 2: 'blue', 3: 'default' }
const INDUSTRY_TYPE_TEXT: Record<number, string> = { 1: '食品加工', 2: '餐饮服务', 3: '批发零售', 4: '其他' }

/**
 * 线索状态口径（后端现状，跨页不一致已登记为缺口）：
 *  · 后端 convertToCustomer 回写 leadStatus = 3 且 leadStatusDesc = '已转化'；
 *  · 表单页可选 2 = 已转化 / 3 = 已关闭。
 *  故展示一律「leadStatusDesc 优先，回落本字典」——转化产生的行显示「已转化」，
 *  表单手工置位的行按本字典显示；「是否已转化」判定一律以 convertedCustomerId/convertedTime 为准。
 */
const LEAD_STATUS_TEXT: Record<number, string> = { 0: '新线索', 1: '跟进中', 2: '已转化', 3: '已关闭' }
const LEAD_STATUS_COLOR: Record<number, string> = { 0: 'blue', 1: 'orange', 2: 'green', 3: 'red' }
const LEAD_STATUS_OPTIONS = [
  { label: '新线索', value: 0 },
  { label: '跟进中', value: 1 },
  { label: '已转化', value: 2 },
  { label: '已关闭', value: 3 },
]
const LEAD_LEVEL_OPTIONS = [
  { label: '高', value: 1 },
  { label: '中', value: 2 },
  { label: '低', value: 3 },
]

function dictText(map: Record<number, string>, v: any): string {
  const n = Number(v)
  return (Number.isFinite(n) && map[n]) || '-'
}

/** 已转化判定：转化端点回写 convertedCustomerId / convertedTime；表单手工置 2 = 已转化 */
function isConverted(record: any): boolean {
  if (!record) return false
  if ((record.convertedCustomerId !== null && record.convertedCustomerId !== undefined) || record.convertedTime) return true
  return Number(record.leadStatus) === 2
}

/** 可转化：未转化且非「已关闭(3)」——与原实现的 status < 2 口径一致，只是不再读错字段 */
function canConvert(record: any): boolean {
  if (!record || record.__ghost) return false
  if (isConverted(record)) return false
  return Number(record.leadStatus) !== 3
}

function leadStatusText(record: any): string {
  if (record?.leadStatusDesc) return record.leadStatusDesc
  return dictText(LEAD_STATUS_TEXT, record?.leadStatus)
}

/** 金额显示：¥ + 千分位；空值显示 '-'（0 显示 ¥0.00，避免与「无数据」混淆） */
function formatAmount(raw: any): string {
  if (raw === undefined || raw === null || raw === '') return '-'
  const n = Number(raw)
  if (!Number.isFinite(n)) return '-'
  return `¥${n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

/** 后端时间字段为 LocalDateTime（JSON 形如 2026-09-18T10:30:00），统一显示到分钟 */
function formatDateTime(raw: any): string {
  if (!raw) return ''
  const s = String(raw).replace('T', ' ')
  return s.length > 16 ? s.slice(0, 16) : s
}

function filterOption(input: string, option: any): boolean {
  return String(option?.label ?? '').toLowerCase().includes(String(input ?? '').toLowerCase())
}

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '关键词', visible: true },
  { key: 'leadStatus', label: '线索状态', visible: true },
  { key: 'leadLevel', label: '线索等级', visible: true },
  { key: 'salesPersonId', label: '负责销售', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'create', label: '新建线索', enabled: true },
  { key: 'import', label: '导入线索', enabled: true },
  { key: 'batchAssign', label: '批量分配', enabled: true },
  { key: 'batchConvert', label: '批量转化', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'autoRefresh', label: '自动刷新(30s)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]
const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))
const pageConfigStorageKey = 'crm-lead-page-config'

function isFieldVisible(key: string): boolean {
  const found = queryFieldsConfig.value.find(f => f.key === key)
  return found ? found.visible : true
}
function isButtonEnabled(key: string): boolean {
  const found = functionButtonConfig.value.find(b => b.key === key)
  return found ? found.enabled : true
}
function handlePageConfigChange(config: any) {
  const before = isButtonEnabled('autoRefresh')
  if (config?.queryFields) queryFieldsConfig.value = config.queryFields
  if (config?.functionButtons) functionButtonConfig.value = config.functionButtons
  // 自动刷新开关切换后立即生效
  const after = isButtonEnabled('autoRefresh')
  if (before !== after) (after ? startAutoRefresh : stopAutoRefresh)()
}

// ═══ 查询条件（后端 /crm/lead/page 仅支持 keyword/leadStatus/leadLevel/salesPersonId，
//     故不提供「来源渠道」筛选——传了也会被后端静默忽略，属假筛选，缺口已登记） ═══
const searchForm = reactive({
  keyword: '',
  leadStatus: undefined as number | undefined,
  leadLevel: undefined as number | undefined,
  salesPersonId: undefined as number | undefined,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const loading = ref(false)
const exporting = ref(false)
const tableData = ref<any[]>([])
const tableRef = ref<any>()

/** 查询参数（列表与统计/导出共用，保证口径一致） */
function buildQuery(): LeadQuery {
  return {
    keyword: searchForm.keyword || undefined,
    leadStatus: searchForm.leadStatus,
    leadLevel: searchForm.leadLevel,
    salesPersonId: searchForm.salesPersonId,
  }
}

// ═══ 表格列（rowNo 承载列配置齿轮；checkbox/action 为锁定列，不进配置面板） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 190, fixed: 'left' },
  { key: 'leadCode', title: '线索编号', type: 'input', width: 150 },
  { key: 'leadName', title: '线索名称', type: 'input', width: 180 },
  { key: 'companyName', title: '公司名称', type: 'input', width: 180 },
  { key: 'contactName', title: '联系人', type: 'input', width: 100 },
  { key: 'contactPhone', title: '联系电话', type: 'input', width: 130 },
  { key: 'leadSource', title: '来源渠道', type: 'slot', slotName: 'sourceCell', width: 110 },
  { key: 'leadStatus', title: '状态', type: 'slot', slotName: 'statusCell', width: 90, align: 'center' },
  { key: 'salesPersonName', title: '负责销售', type: 'input', width: 100 },
  { key: 'leadLevel', title: '线索等级', type: 'slot', slotName: 'levelCell', width: 90, align: 'center', defaultHidden: true },
  { key: 'industryType', title: '所属行业', type: 'slot', slotName: 'industryCell', width: 100, defaultHidden: true },
  { key: 'contactEmail', title: '联系邮箱', type: 'input', width: 160, defaultHidden: true },
  { key: 'estimatedAmount', title: '预计金额', type: 'slot', slotName: 'amountCell', width: 120, align: 'right', defaultHidden: true },
  { key: 'expectedCloseDate', title: '预计成交日期', type: 'input', width: 120, defaultHidden: true },
  { key: 'requirement', title: '需求描述', type: 'slot', slotName: 'requirementCell', width: 200, defaultHidden: true },
  { key: 'province', title: '省份', type: 'input', width: 100, defaultHidden: true },
  { key: 'city', title: '城市', type: 'input', width: 100, defaultHidden: true },
  { key: 'address', title: '详细地址', type: 'input', width: 180, defaultHidden: true },
  { key: 'departmentName', title: '部门', type: 'input', width: 120, defaultHidden: true },
  { key: 'convertedTime', title: '转化时间', type: 'input', width: 140, defaultHidden: true, formatter: (raw: any) => formatDateTime(raw) },
  { key: 'actualCloseDate', title: '实际关闭日期', type: 'input', width: 120, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 160, defaultHidden: true },
  { key: 'createdAt', title: '添加时间', type: 'input', width: 140, formatter: (raw: any) => formatDateTime(raw) },
  { key: 'updatedAt', title: '更新时间', type: 'input', width: 140, defaultHidden: true, formatter: (raw: any) => formatDateTime(raw) },
]

// ═══ 统计卡（全量口径） ═══
const stats = reactive({ newCount: 0, following: 0, converted: 0, estimatedAmount: 0 })

function computeStats(rows: any[]) {
  stats.newCount = rows.filter(r => Number(r.leadStatus) === 0).length
  stats.following = rows.filter(r => Number(r.leadStatus) === 1).length
  stats.converted = rows.filter(r => isConverted(r)).length
  stats.estimatedAmount = rows.reduce((sum, r) => sum + (Number(r.estimatedAmount) || 0), 0)
}

async function fetchStats() {
  try {
    const res: any = await leadConvertApi.exportList(buildQuery())
    const rows = Array.isArray(res) ? res : (res?.data ?? res?.records ?? [])
    computeStats(Array.isArray(rows) ? rows : [])
  } catch (e) {
    // 统计失败不阻塞列表：回落为当前页聚合并在控制台留痕
    console.warn('[CRM线索] 统计汇总失败，回落当前页聚合', e)
    computeStats(tableData.value.filter(r => !r.__ghost))
  }
}

// ═══ 勾选（BillDetailTable 按 rowIndex 持有勾选，故页面侧按记录 id 另行持有，翻页/换查询即清空） ═══
const selectedRows = ref<any[]>([])

function handleCheckboxChange(record: any, _rowIndex: number, checked: boolean) {
  const key = String(record?.id)
  if (checked) {
    if (!selectedRows.value.some(r => String(r.id) === key)) selectedRows.value.push(record)
  } else {
    selectedRows.value = selectedRows.value.filter(r => String(r.id) !== key)
  }
}
function handleCheckboxAll(checked: boolean, records: any[]) {
  selectedRows.value = checked ? [...(records || [])] : []
}

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await leadApi.page({
      ...buildQuery(),
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    const result: any = res || {}
    tableData.value = (result.records || result.data?.records || []) as any[]
    pagination.total = Number(result.total ?? result.data?.total ?? 0)
    // 换了一批数据 → 同步清空页面侧勾选（组件内部也会按 dataSource 变化自动清空）
    selectedRows.value = []
    await nextTick()
    tableRef.value?.clearSelection?.()
  } catch (error: any) {
    console.error('[CRM线索] 加载列表失败', error)
    message.error(error?.response?.data?.message || '获取线索数据失败')
    tableData.value = []
    pagination.total = 0
    selectedRows.value = []
  } finally {
    loading.value = false
  }
}

/** 列表 + 统计一起刷新 */
async function loadAll() {
  await Promise.all([fetchList(), fetchStats()])
}

function handleSearch() {
  pagination.current = 1
  loadAll()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.leadStatus = undefined
  searchForm.leadLevel = undefined
  searchForm.salesPersonId = undefined
  pagination.current = 1
  loadAll()
}

function handleRefresh() {
  loadAll()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 自动刷新（30s，可在「页面配置 → 功能按钮」关闭；原实现间隔硬编码且无法关闭） ═══
const AUTO_REFRESH_MS = 30000
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

function startAutoRefresh() {
  stopAutoRefresh()
  autoRefreshCountdown.value = AUTO_REFRESH_MS / 1000
  refreshTimer = setInterval(() => {
    // 静默刷新：不打断用户操作（loading 态由表格自身呈现）
    Promise.all([fetchList(), fetchStats()]).finally(() => {
      autoRefreshCountdown.value = AUTO_REFRESH_MS / 1000
    })
  }, AUTO_REFRESH_MS)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
}
function stopAutoRefresh() {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
  autoRefreshCountdown.value = 0
}

// ═══ 新增 / 修改（收敛到独立表单页 crm/lead/form，避免列表页内嵌表单与实体字段不对齐） ═══
function handleAdd() {
  router.push('/crm/lead/form')
}
function handleEdit(record: any) {
  router.push({ path: '/crm/lead/form', query: { id: String(record.id) } })
}

async function handleDelete(record: any) {
  try {
    await leadApi.delete(record.id)
    message.success('删除成功')
    loadAll()
  } catch (e: any) {
    console.warn('[CRM线索] 删除线索失败', e)
    message.error(e?.message || '删除失败')
  }
}

// ═══ 分配（单条 / 批量，写 sales_person_id + sales_person_name） ═══
const assignVisible = ref(false)
const assignLoading = ref(false)
const assignTargets = ref<any[]>([])
const assignForm = reactive({ userId: undefined as number | undefined })

function handleAssign(record: any) {
  assignTargets.value = [record]
  assignForm.userId = record.salesPersonId ?? undefined
  assignVisible.value = true
}

function handleBatchAssign() {
  if (!selectedRows.value.length) {
    message.warning('请先勾选要分配的线索')
    return
  }
  assignTargets.value = [...selectedRows.value]
  assignForm.userId = undefined
  assignVisible.value = true
}

async function handleAssignConfirm() {
  if (!assignForm.userId) {
    message.warning('请选择销售人员')
    return
  }
  const user = salesOptions.value.find(o => String(o.value) === String(assignForm.userId))
  assignLoading.value = true
  try {
    const results = await Promise.allSettled(
      assignTargets.value.map(r => leadApi.update(r.id, {
        salesPersonId: assignForm.userId,
        salesPersonName: user?.label,
      } as any))
    )
    const ok = results.filter(r => r.status === 'fulfilled').length
    const fail = results.length - ok
    if (fail === 0) {
      message.success(assignTargets.value.length > 1 ? `已分配 ${ok} 条线索` : '分配成功')
      assignVisible.value = false
    } else {
      // 部分失败如实回报，不谎报全量成功
      message.warning(`分配完成：成功 ${ok} 条，失败 ${fail} 条`)
      assignVisible.value = false
      console.warn('[CRM线索] 分配部分失败', results.filter(r => r.status === 'rejected'))
    }
    loadAll()
  } catch (e: any) {
    console.warn('[CRM线索] 分配线索失败', e)
    message.error(e?.message || '分配失败')
  } finally {
    assignLoading.value = false
  }
}

// ═══ 转化为客户（POST /crm/lead/{id}/convert；后端按线索实体自动生成客户） ═══
async function handleConvert(record: any) {
  Modal.confirm({
    title: '线索转化确认',
    content: `确定将线索「${record.leadName || record.companyName || ''}」转化为 CRM 客户？客户名称取线索公司名称，转化后线索状态置为已转化。`,
    okText: '确定转化',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res: any = await leadApi.convertToCustomer(record.id)
        message.success(`转化成功${res?.customerCode ? `，客户编号 ${res.customerCode}` : ''}`)
        loadAll()
      } catch (e: any) {
        console.warn('[CRM线索] 转化线索失败', e)
        message.error(e?.message || '转化失败')
      }
    },
  })
}

// ═══ 批量转化（POST /crm/lead/batch-convert，body 为 id 数组；后端逐条独立事务、单条失败不中断） ═══
function handleBatchConvert() {
  const targets = selectedRows.value.filter(canConvert)
  if (!selectedRows.value.length) {
    message.warning('请先勾选要转化的线索')
    return
  }
  if (!targets.length) {
    message.warning('所选线索均为已转化/已关闭，无需转化')
    return
  }
  const skipped = selectedRows.value.length - targets.length
  Modal.confirm({
    title: '批量转化确认',
    content: `确定转化选中的 ${targets.length} 条线索为 CRM 客户？${skipped > 0 ? `（另有 ${skipped} 条已转化/已关闭将跳过）` : ''}`,
    okText: '确定转化',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res: any = await leadApi.batchConvert(targets.map(r => r.id))
        const ok = Number(res?.successCount ?? 0)
        const fail = Number(res?.failCount ?? 0)
        const reasons = (res?.failures || [])
          .slice(0, 3)
          .map((f: any) => f?.reason)
          .filter(Boolean)
          .join('；')
        if (fail === 0) message.success(`成功转化 ${ok} 条线索`)
        else message.warning(`转化完成：成功 ${ok} 条，失败 ${fail} 条${reasons ? `（${reasons}）` : ''}`)
        loadAll()
      } catch (e: any) {
        console.warn('[CRM线索] 批量转化失败', e)
        message.error(e?.message || '批量转化失败')
      }
    },
  })
}

// ═══ 导入（后端暂无线索导入端点；不伪造成功提示，如实告知缺口） ═══
function handleImport() {
  message.info('线索导入需后端导入端点支持，当前版本尚未提供（缺口已登记）')
}

// ═══ 导出（走 GET /crm/lead/export 全量 + 中文映射；后端不可用时回落当前页） ═══
function buildExportRows(rows: any[]): (string | number)[][] {
  return rows.map(r => [
    r.leadCode || '', r.leadName || '', r.companyName || '', r.contactName || '', r.contactPhone || '',
    r.contactEmail || '', dictText(LEAD_SOURCE_TEXT, r.leadSource), dictText(LEAD_LEVEL_TEXT, r.leadLevel),
    dictText(INDUSTRY_TYPE_TEXT, r.industryType), leadStatusText(r), r.salesPersonName || '',
    r.estimatedAmount ?? '', formatDateTime(r.expectedCloseDate), formatDateTime(r.createdAt),
    formatDateTime(r.convertedTime), r.remark || '',
  ])
}
const EXPORT_HEADERS = ['线索编号', '线索名称', '公司名称', '联系人', '联系电话', '联系邮箱', '来源渠道',
  '线索等级', '所属行业', '状态', '负责销售', '预计金额', '预计成交日期', '添加时间', '转化时间', '备注']

async function handleExport() {
  exporting.value = true
  try {
    const res: any = await leadConvertApi.exportList(buildQuery())
    const rows = Array.isArray(res) ? res : (res?.data ?? res?.records ?? [])
    const list = Array.isArray(rows) && rows.length ? rows : tableData.value.filter(r => !r.__ghost)
    if (!list.length) {
      message.warning('没有可导出的数据')
      return
    }
    exportCsv(EXPORT_HEADERS, buildExportRows(list), '线索')
  } catch (e) {
    console.warn('[CRM线索] 后端导出失败，回落导出当前页', e)
    const list = tableData.value.filter(r => !r.__ghost)
    if (!list.length) {
      message.warning('没有可导出的数据')
      return
    }
    exportCsv(EXPORT_HEADERS, buildExportRows(list), '线索')
  } finally {
    exporting.value = false
  }
}

// ═══ 销售人员下拉（平台用户列表；原分配弹窗为硬编码「张三/李四/王五」） ═══
const salesOptions = ref<{ label: string; value: number }[]>([])

async function loadSalesOptions() {
  try {
    const users: any = await optionsApi.getUsers()
    const list = (users as any)?.data || users || []
    salesOptions.value = (Array.isArray(list) ? list : []).map((u: any) => ({
      label: u.nickname || u.name || u.realName || u.username || String(u.id),
      value: u.id,
    }))
  } catch (e) {
    console.warn('[CRM线索] 销售人员下拉获取失败', e)
    salesOptions.value = []
  }
}

// ═══ 快捷键 / 跨页事件（保留既有交互：Ctrl+N 新增、F5 刷新、crm:create / crm:refresh） ═══
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

function handleF5Key(e: KeyboardEvent) {
  if (e.key === 'F5') {
    e.preventDefault()
    debounceClick('refresh', handleRefresh)
    return
  }
  if (e.ctrlKey && (e.key === 'n' || e.key === 'N')) {
    e.preventDefault()
    debounceClick('add', handleAdd)
  }
}

function handleParentCreate() { handleAdd() }
function handleParentRefresh() { handleRefresh() }

function handleError(error: Error) {
  console.error('[CRM线索] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(async () => {
  loadSalesOptions()
  loadAll()
  if (isButtonEnabled('autoRefresh')) startAutoRefresh()
  document.addEventListener('keydown', handleF5Key)
  window.addEventListener('crm:create' as any, handleParentCreate as any)
  window.addEventListener('crm:refresh' as any, handleParentRefresh as any)
})

onUnmounted(() => {
  stopAutoRefresh()
  document.removeEventListener('keydown', handleF5Key)
  window.removeEventListener('crm:create' as any, handleParentCreate as any)
  window.removeEventListener('crm:refresh' as any, handleParentRefresh as any)
})

defineExpose({ handleQuery: loadAll })
</script>

<style scoped>
.search-area { padding: 0; }
.search-grid { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-item :deep(.ant-select) { min-width: 140px; }
.search-item :deep(.ant-input-affix-wrapper) { width: 200px; }
.search-actions { margin-left: auto; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }

/* 统计卡：置于表格面板内（CategoryListLayout 的 #table 插槽，flex 纵向堆叠） */
.stat-strip {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  padding: 10px 12px 8px;
}
.stat-card {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 12px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  background: #fafafa;
}
.stat-title { font-size: 13px; color: #666; }
.stat-value { font-size: 20px; font-weight: 600; color: #303133; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; }
.stat-desc { font-size: 12px; color: #999; }

/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

.cell-ellipsis {
  display: inline-block;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

.assign-tip { font-size: 12px; color: #999; line-height: 1.6; }

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
