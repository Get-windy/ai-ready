<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        线索转化（CRM → 线索管理 → 线索转化，菜单 70311，单入口）
        · CRM 为本系统独有模块（ql361 无 CRM 域），按业界生产级 CRM（Odoo/SAP/金蝶/用友）建模
        · 规格：docs/Yh-Spec/手动整理对标开发文档/crm模块/线索转化开发文档.md
        · 路线 A：ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillDetailTable
        · 定位：转化工作台——筛选线索 + 4 步向导弹窗（选择线索 → 客户信息预览 → 创建商机 → 完成）
        · 打印判 N/A（工作台页、非票据，《_开发指南-金标准》§七之二 第 7 项）
        · 本轮改造：
          ① 外壳升级为路线 A（原为统计卡 + a-collapse + BillTableList 平铺）
          ② 修 3 处 P0：列 formatter 位置参数 / 插槽列补 type:'slot' / action 列补 slotName
          ③ 字段名统一改后端实体名（leadStatus / leadName / contactPhone / contactEmail / leadSource）——
             原读 record.status/phone/email/source 致统计卡恒 0、已转化行仍显示「转化」、向导预填恒空
          ④ 向导 Step1 由「可编辑但不落库的假字段」改为「后端转化规则只读预览」（原 payload 构建后全被丢弃）
          ⑤ 勾选商机时在转化成功后由本页调用商机创建端点真实建商机（转化端点不支持带体，故分两步，
             商机创建失败会如实提示，不谎报「商机已创建」）
          ⑥ 批量转化接线 POST /crm/lead/batch-convert；新增导出；「新建线索」改真实跳转表单页
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新建线索 / 批量转化 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('create')"
              type="primary"
              size="small"
              class="btn-add"
              @click="handleAddLead"
            >
              <PlusOutlined /> 新建线索
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

        <!-- ═══ 工具栏右侧：刷新 / 导出 / 页面配置 ═══ -->
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

        <!-- ═══ 查询区（横向网格；后端 /crm/lead/page 仅支持 keyword/leadStatus/leadLevel） ═══ -->
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
                待转化线索
              </div>
              <div class="stat-value">
                {{ stats.pending }}
              </div>
              <div class="stat-desc">
                未转化且未关闭
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
                预估金额
              </div>
              <div class="stat-value">
                {{ formatAmount(stats.estimatedAmount) }}
              </div>
              <div class="stat-desc">
                当前条件全量合计
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-title">
                转化率
              </div>
              <div class="stat-value">
                {{ stats.rate }}%
              </div>
              <div class="stat-desc">
                已转化 / 当前条件总数
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
              storage-key="crm-lead-convert-table-columns"
              global-config-key="crm-lead-convert-table-columns"
              @checkbox-change="handleCheckboxChange"
              @checkbox-all="handleCheckboxAll"
            >
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
                    v-if="canConvert(record)"
                    v-permission="'crm:lead:convert'"
                    type="link"
                    size="small"
                    @click="openConvertWizard(record)"
                  >
                    转化
                  </a-button>
                  <a-tag
                    v-else
                    color="green"
                  >
                    已转化
                  </a-tag>
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

      <!-- ═══ 线索转化向导 ═══ -->
      <a-modal
        v-model:open="wizardVisible"
        title="线索转化向导"
        :width="720"
        :footer="null"
        :closable="true"
        @cancel="handleWizardClose"
      >
        <a-steps
          v-model:current="wizardStep"
          size="small"
          style="margin-bottom: 24px"
        >
          <a-step title="选择线索" />
          <a-step title="客户信息" />
          <a-step title="创建商机" />
          <a-step title="完成" />
        </a-steps>

        <!-- Step 0: 确认线索 -->
        <div v-if="wizardStep === 0">
          <a-descriptions
            v-if="wizardLead"
            bordered
            :column="2"
            size="small"
          >
            <a-descriptions-item label="线索编号">
              {{ wizardLead.leadCode || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="线索名称">
              {{ wizardLead.leadName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="公司名称">
              {{ wizardLead.companyName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="联系人">
              {{ wizardLead.contactName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="联系电话">
              {{ wizardLead.contactPhone || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="来源渠道">
              {{ dictText(LEAD_SOURCE_TEXT, wizardLead.leadSource) }}
            </a-descriptions-item>
            <a-descriptions-item label="线索等级">
              {{ dictText(LEAD_LEVEL_TEXT, wizardLead.leadLevel) }}
            </a-descriptions-item>
            <a-descriptions-item label="预计金额">
              {{ formatAmount(wizardLead.estimatedAmount) }}
            </a-descriptions-item>
          </a-descriptions>
          <div
            v-else
            class="step-empty"
          >
            <p>请选择一个线索进行转化</p>
          </div>
        </div>

        <!-- Step 1: 客户信息（只读预览：后端转化接口按线索字段自动生成客户，向导不可修改） -->
        <div v-if="wizardStep === 1">
          <a-alert
            message="客户信息由转化接口按线索字段自动生成，向导中不可修改；如需调整请先编辑线索。"
            type="info"
            show-icon
            style="margin-bottom: 16px"
          />
          <a-descriptions
            v-if="wizardLead"
            bordered
            :column="1"
            size="small"
          >
            <a-descriptions-item label="客户名称">
              {{ wizardLead.companyName || wizardLead.leadName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="联系人 / 电话">
              {{ wizardLead.contactName || '-' }} / {{ wizardLead.contactPhone || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="邮箱">
              {{ wizardLead.contactEmail || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="地区">
              {{ [wizardLead.province, wizardLead.city].filter(Boolean).join(' ') || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="详细地址">
              {{ wizardLead.address || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="来源 / 等级">
              {{ dictText(LEAD_SOURCE_TEXT, wizardLead.leadSource) }} /
              {{ dictText(LEAD_LEVEL_TEXT, wizardLead.leadLevel) }}
            </a-descriptions-item>
          </a-descriptions>
        </div>

        <!-- Step 2: 创建商机（可选；转化成功后由本页调用商机端点创建） -->
        <div v-if="wizardStep === 2">
          <a-form
            :model="wizardForm"
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 16 }"
          >
            <a-form-item label="创建商机">
              <a-switch
                v-model:checked="wizardForm.createOpportunity"
                checked-children="是"
                un-checked-children="否"
              />
            </a-form-item>
            <template v-if="wizardForm.createOpportunity">
              <a-form-item
                label="商机名称"
                required
              >
                <a-input
                  v-model:value="wizardForm.opportunityName"
                  placeholder="请输入商机名称"
                  size="small"
                />
              </a-form-item>
              <a-form-item label="预计金额">
                <a-input-number
                  v-model:value="wizardForm.expectedAmount"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                  size="small"
                />
              </a-form-item>
              <a-form-item label="成交概率">
                <a-slider
                  v-model:value="wizardForm.winProbability"
                  :min="0"
                  :max="100"
                  :marks="{ 0: '0%', 50: '50%', 100: '100%' }"
                />
              </a-form-item>
              <a-form-item label="预计成交日期">
                <a-date-picker
                  v-model:value="wizardForm.expectedCloseDate"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                  size="small"
                />
              </a-form-item>
              <a-form-item label="商机阶段">
                <a-select
                  v-model:value="wizardForm.opportunityStage"
                  placeholder="请选择阶段"
                  size="small"
                  :options="OPP_STAGE_OPTIONS"
                />
              </a-form-item>
            </template>
          </a-form>
        </div>

        <!-- Step 3: 完成（回显真实编号，不谎报结果） -->
        <div v-if="wizardStep === 3">
          <a-result
            status="success"
            title="转化成功!"
          >
            <template #subTitle>
              <p>线索已成功转化为 CRM 客户{{ resultCustomerCode ? `（客户编号 ${resultCustomerCode}）` : '' }}</p>
              <p v-if="resultOpportunityCode">
                商机已创建（商机编号 {{ resultOpportunityCode }}）
              </p>
              <p
                v-else-if="wizardForm.createOpportunity"
                style="color: #faad14"
              >
                商机未创建成功，可稍后在「商机」页手工补录
              </p>
            </template>
            <template #extra>
              <a-button
                type="primary"
                @click="handleWizardDone"
              >
                完成
              </a-button>
              <a-button @click="openNewWizard">
                继续转化
              </a-button>
            </template>
          </a-result>
        </div>

        <!-- 向导按钮 -->
        <div
          v-if="wizardStep < 3"
          class="wizard-footer"
        >
          <a-space>
            <a-button
              v-if="wizardStep > 0"
              @click="wizardStep--"
            >
              上一步
            </a-button>
            <a-button
              v-if="wizardStep < 2"
              type="primary"
              :disabled="!wizardLead"
              @click="wizardStep++"
            >
              下一步
            </a-button>
            <a-button
              v-if="wizardStep === 2"
              type="primary"
              :loading="converting"
              @click="handleConvertConfirm"
            >
              确认转化
            </a-button>
          </a-space>
        </div>
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
  SwapRightOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { QueryFieldSetting, FunctionButtonSetting } from '@/components/PageConfigPanel/index.vue'
import { leadApi, leadConvertApi, opportunityApi, type LeadQuery } from '@/api/crm'
import { exportCsv } from '@/utils/exportCsv'

defineOptions({ name: 'CrmLeadConvert' })

const router = useRouter()

// ═══ 字典（与线索列表页 / 表单页逐字一致；来源/等级为数字值域） ═══
const LEAD_SOURCE_TEXT: Record<number, string> = { 1: '网络推广', 2: '客户介绍', 3: '电话营销', 4: '展会', 5: '其他' }
const LEAD_LEVEL_TEXT: Record<number, string> = { 1: '高', 2: '中', 3: '低' }
const LEAD_LEVEL_COLOR: Record<number, string> = { 1: 'red', 2: 'blue', 3: 'default' }
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
/** 商机阶段：后端 opportunity_stage 为 Integer（1-6），故向导直接用数字值域，不做字符串猜映射 */
const OPP_STAGE_OPTIONS = [
  { label: '初步接触', value: 1 },
  { label: '需求确认', value: 2 },
  { label: '方案报价', value: 3 },
  { label: '商务谈判', value: 4 },
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

/** 可转化：未转化且非「已关闭(3)」 */
function canConvert(record: any): boolean {
  if (!record || record.__ghost) return false
  if (isConverted(record)) return false
  return Number(record.leadStatus) !== 3
}

function leadStatusText(record: any): string {
  if (record?.leadStatusDesc) return record.leadStatusDesc
  return dictText(LEAD_STATUS_TEXT, record?.leadStatus)
}

function formatAmount(raw: any): string {
  if (raw === undefined || raw === null || raw === '') return '-'
  const n = Number(raw)
  if (!Number.isFinite(n)) return '-'
  return `¥${n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function formatDateTime(raw: any): string {
  if (!raw) return ''
  const s = String(raw).replace('T', ' ')
  return s.length > 16 ? s.slice(0, 16) : s
}

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '关键词', visible: true },
  { key: 'leadStatus', label: '线索状态', visible: true },
  { key: 'leadLevel', label: '线索等级', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'create', label: '新建线索', enabled: true },
  { key: 'batchConvert', label: '批量转化', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]
const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))
const pageConfigStorageKey = 'crm-lead-convert-page-config'

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

// ═══ 查询条件（后端仅支持 keyword/leadStatus/leadLevel；来源渠道无对应入参故不提供假筛选） ═══
const searchForm = reactive({
  keyword: '',
  leadStatus: undefined as number | undefined,
  leadLevel: undefined as number | undefined,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const loading = ref(false)
const exporting = ref(false)
const tableData = ref<any[]>([])
const tableRef = ref<any>()

function buildQuery(): LeadQuery {
  return {
    keyword: searchForm.keyword || undefined,
    leadStatus: searchForm.leadStatus,
    leadLevel: searchForm.leadLevel,
  }
}

// ═══ 表格列 ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 100, fixed: 'left' },
  { key: 'leadCode', title: '线索编号', type: 'input', width: 150 },
  { key: 'leadName', title: '线索名称', type: 'input', width: 180 },
  { key: 'companyName', title: '公司名称', type: 'input', width: 170 },
  { key: 'contactName', title: '联系人', type: 'input', width: 100 },
  { key: 'contactPhone', title: '联系电话', type: 'input', width: 130 },
  { key: 'leadSource', title: '来源渠道', type: 'slot', slotName: 'sourceCell', width: 110 },
  { key: 'leadStatus', title: '状态', type: 'slot', slotName: 'statusCell', width: 90, align: 'center' },
  { key: 'estimatedAmount', title: '预估金额', type: 'slot', slotName: 'amountCell', width: 120, align: 'right' },
  { key: 'leadLevel', title: '线索等级', type: 'slot', slotName: 'levelCell', width: 90, align: 'center', defaultHidden: true },
  { key: 'salesPersonName', title: '负责销售', type: 'input', width: 100, defaultHidden: true },
  { key: 'expectedCloseDate', title: '预计成交日期', type: 'input', width: 120, defaultHidden: true },
  { key: 'requirement', title: '需求描述', type: 'slot', slotName: 'requirementCell', width: 200, defaultHidden: true },
  { key: 'convertedTime', title: '转化时间', type: 'input', width: 140, defaultHidden: true, formatter: (raw: any) => formatDateTime(raw) },
  { key: 'createdAt', title: '添加时间', type: 'input', width: 140, defaultHidden: true, formatter: (raw: any) => formatDateTime(raw) },
  { key: 'updatedAt', title: '更新时间', type: 'input', width: 140, defaultHidden: true, formatter: (raw: any) => formatDateTime(raw) },
]

// ═══ 统计卡（全量口径） ═══
const stats = reactive({ pending: 0, converted: 0, estimatedAmount: 0, rate: 0 })

function computeStats(rows: any[]) {
  const total = rows.length
  stats.converted = rows.filter(r => isConverted(r)).length
  stats.pending = rows.filter(r => canConvert(r)).length
  stats.estimatedAmount = rows.reduce((sum, r) => sum + (Number(r.estimatedAmount) || 0), 0)
  stats.rate = total ? Math.round((stats.converted / total) * 100) : 0
}

async function fetchStats() {
  try {
    const res: any = await leadConvertApi.exportList(buildQuery())
    const rows = Array.isArray(res) ? res : (res?.data ?? res?.records ?? [])
    computeStats(Array.isArray(rows) ? rows : [])
  } catch (e) {
    console.warn('[线索转化] 统计汇总失败，回落当前页聚合', e)
    computeStats(tableData.value.filter(r => !r.__ghost))
  }
}

// ═══ 勾选 ═══
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
    selectedRows.value = []
    await nextTick()
    tableRef.value?.clearSelection?.()
  } catch (error: any) {
    console.error('[线索转化] 加载列表失败', error)
    message.error(error?.response?.data?.message || '获取线索数据失败')
    tableData.value = []
    pagination.total = 0
    selectedRows.value = []
  } finally {
    loading.value = false
  }
}

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

/** 转化工作台不建线索，跳转独立表单页（原为 window 事件，本页无监听方 → 点击无反应） */
function handleAddLead() {
  router.push('/crm/lead/form')
}

// ═══ 批量转化（POST /crm/lead/batch-convert，逐条独立事务、单条失败不中断） ═══
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
        console.warn('[线索转化] 批量转化失败', e)
        message.error(e?.message || '批量转化失败')
      }
    },
  })
}

// ═══ 转化向导 ═══
const wizardVisible = ref(false)
const wizardStep = ref(0)
const wizardLead = ref<any>(null)
const converting = ref(false)
const resultCustomerCode = ref('')
const resultOpportunityCode = ref('')
const wizardForm = reactive({
  createOpportunity: false,
  opportunityName: '',
  expectedAmount: undefined as number | undefined,
  winProbability: 50,
  expectedCloseDate: undefined as string | undefined,
  opportunityStage: 1 as number | undefined,
})

function openConvertWizard(record: any) {
  wizardLead.value = record
  wizardStep.value = 0
  resultCustomerCode.value = ''
  resultOpportunityCode.value = ''
  // 预填：字段名一律用后端实体名（原用 record.name/phone/email/source，恒空）
  wizardForm.createOpportunity = false
  wizardForm.opportunityName = `${record.leadName || record.companyName || '线索'} - 商机`
  wizardForm.expectedAmount = record.estimatedAmount ?? undefined
  wizardForm.winProbability = 50
  wizardForm.expectedCloseDate = record.expectedCloseDate || undefined
  wizardForm.opportunityStage = 1
  wizardVisible.value = true
}

function openNewWizard() {
  wizardLead.value = null
  wizardStep.value = 0
  resultCustomerCode.value = ''
  resultOpportunityCode.value = ''
  wizardForm.createOpportunity = false
  wizardForm.opportunityName = ''
  wizardForm.expectedAmount = undefined
  wizardForm.winProbability = 50
  wizardForm.expectedCloseDate = undefined
  wizardForm.opportunityStage = 1
  wizardVisible.value = false
  selectedRows.value = []
  loadAll()
}

/**
 * 确认转化：
 *  ① POST /crm/lead/{id}/convert（后端按线索实体生成 CRM 客户，端点不接受请求体，
 *     故向导不收集客户字段——原实现收集后整包丢弃，属误导）；
 *  ② 勾选「创建商机」时，用返回的客户 id 调 POST /crm/opportunity 真实建商机（两步分事务，
 *     商机失败不回滚客户，且如实提示，不谎报「商机已创建」）。
 */
async function handleConvertConfirm() {
  if (!wizardLead.value) return
  if (wizardForm.createOpportunity && !wizardForm.opportunityName?.trim()) {
    message.warning('请填写商机名称')
    return
  }
  converting.value = true
  try {
    const customer: any = await leadApi.convertToCustomer(wizardLead.value.id)
    resultCustomerCode.value = customer?.customerCode || ''

    if (wizardForm.createOpportunity) {
      try {
        const opp: any = await opportunityApi.create({
          opportunityName: wizardForm.opportunityName.trim(),
          customerId: customer?.id,
          customerName: customer?.customerName,
          leadId: wizardLead.value.id,
          opportunityStage: wizardForm.opportunityStage,
          estimatedAmount: wizardForm.expectedAmount,
          probability: wizardForm.winProbability,
          expectedCloseDate: wizardForm.expectedCloseDate,
          salesPersonId: wizardLead.value.salesPersonId,
          salesPersonName: wizardLead.value.salesPersonName,
        } as any)
        resultOpportunityCode.value = opp?.opportunityCode || ''
      } catch (e: any) {
        console.warn('[线索转化] 商机创建失败', e)
        resultOpportunityCode.value = ''
        message.warning(`线索已转化为客户，但商机创建失败：${e?.message || '未知错误'}`)
      }
    } else {
      resultOpportunityCode.value = ''
    }

    wizardStep.value = 3
    loadAll()
  } catch (e: any) {
    console.warn('[线索转化] 转化失败', e)
    message.error(e?.message || '转化失败')
  } finally {
    converting.value = false
  }
}

function handleWizardClose() {
  // Step3 关闭相当于「完成」，统一走重置 + 刷新
  if (wizardStep.value === 3) openNewWizard()
  else wizardVisible.value = false
}

function handleWizardDone() {
  openNewWizard()
}

// ═══ 导出（GET /crm/lead/export 全量 + 中文映射） ═══
const EXPORT_HEADERS = ['线索编号', '线索名称', '公司名称', '联系人', '联系电话', '来源渠道', '线索等级',
  '状态', '预估金额', '预计成交日期', '负责销售', '转化时间', '添加时间']

function buildExportRows(rows: any[]): (string | number)[][] {
  return rows.map(r => [
    r.leadCode || '', r.leadName || '', r.companyName || '', r.contactName || '', r.contactPhone || '',
    dictText(LEAD_SOURCE_TEXT, r.leadSource), dictText(LEAD_LEVEL_TEXT, r.leadLevel), leadStatusText(r),
    r.estimatedAmount ?? '', formatDateTime(r.expectedCloseDate), r.salesPersonName || '',
    formatDateTime(r.convertedTime), formatDateTime(r.createdAt),
  ])
}

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
    exportCsv(EXPORT_HEADERS, buildExportRows(list), '线索转化')
  } catch (e) {
    console.warn('[线索转化] 后端导出失败，回落导出当前页', e)
    const list = tableData.value.filter(r => !r.__ghost)
    if (!list.length) {
      message.warning('没有可导出的数据')
      return
    }
    exportCsv(EXPORT_HEADERS, buildExportRows(list), '线索转化')
  } finally {
    exporting.value = false
  }
}

// ═══ 快捷键（F5 刷新）+ 跨页事件 ═══
function handleF5Key(e: KeyboardEvent) {
  if (e.key === 'F5') {
    e.preventDefault()
    handleRefresh()
  }
}
function handleParentRefresh() { handleRefresh() }

function handleError(error: Error) {
  console.error('[线索转化] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  loadAll()
  document.addEventListener('keydown', handleF5Key)
  window.addEventListener('crm:refresh' as any, handleParentRefresh as any)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleF5Key)
  window.removeEventListener('crm:refresh' as any, handleParentRefresh as any)
})
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

.wizard-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
.step-empty { text-align: center; padding: 48px 0; color: #999; }

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
