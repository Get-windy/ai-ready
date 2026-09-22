<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      full-height
      :title="pageTitle"
    >
      <!-- 标题旁的说明：讲清当前入口看的是哪一类协议，别让人分不清两个入口 -->
      <template #headerContent>
        <span class="view-hint">{{ pageHint }}</span>
      </template>
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：快速新建 / 发起向导 / 模板 / 条款字典 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('create')"
              v-permission="'agreement:create'"
              type="primary"
              size="small"
              @click="openCreate"
            >
              <PlusOutlined /> 新建协议
            </a-button>
            <a-tooltip
              v-if="isButtonEnabled('wizard')"
              title="发起契约向导：选模板或从零起草 → 选两端 → 逐项约定设定与文字条款 → 预览提交"
              placement="bottom"
            >
              <a-button
                v-permission="'agreement:create'"
                size="small"
                @click="goWizardPage"
              >
                <FileAddOutlined /> 发起契约
              </a-button>
            </a-tooltip>
            <a-tooltip
              v-if="canViewTemplate && isButtonEnabled('template')"
              title="契约模板：平台模板全员可选，租户模板本租户内可选；模板是起点不是默认值"
              placement="bottom"
            >
              <a-button
                size="small"
                @click="goTemplatePage"
              >
                <ProfileOutlined /> 契约模板
              </a-button>
            </a-tooltip>
            <a-tooltip
              v-if="canViewTermOption && isButtonEnabled('termOption')"
              title="条款字典维护（平台侧）：平台只定义选项、不设默认值"
              placement="bottom"
            >
              <a-button
                size="small"
                @click="goTermOptionPage"
              >
                <BookOutlined /> 条款字典
              </a-button>
            </a-tooltip>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：页面配置 / 刷新（列配置走数据表表头齿轮） ═══ -->
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
              @click="fetchData"
            >
              <ReloadOutlined /> 刷新
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区：横向自适应网格 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-container">
              <div
                ref="searchGridRef"
                class="search-grid"
              >
                <div
                  v-if="isFieldVisible('agreementType')"
                  class="search-field-item"
                >
                  <div class="search-select-wrap">
                    <span class="search-select-label">协议类型</span>
                    <a-select
                      v-model:value="searchParams.agreementType"
                      size="small"
                      allow-clear
                      :placeholder="typePlaceholder"
                      :options="typeOptions"
                      @change="handleSearch"
                    />
                  </div>
                </div>
                <div
                  v-if="isFieldVisible('status')"
                  class="search-field-item"
                >
                  <div class="search-select-wrap">
                    <span class="search-select-label">协议状态</span>
                    <a-select
                      v-model:value="searchParams.status"
                      size="small"
                      allow-clear
                      placeholder="全部"
                      :options="statusOptions"
                      @change="handleSearch"
                    />
                  </div>
                </div>
                <div
                  v-if="isFieldVisible('keyword')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="searchParams.keyword"
                    placeholder="协议编号 / 标题"
                    size="small"
                    allow-clear
                    @press-enter="handleSearch"
                  />
                </div>
                <div
                  ref="searchActionRef"
                  class="search-action-group"
                  :style="{ gridColumn: 'span ' + actionSpan }"
                >
                  <div class="search-field-item search-action-item">
                    <a-button
                      type="primary"
                      size="small"
                      @click="handleSearch"
                    >
                      查询
                    </a-button>
                  </div>
                  <div class="search-field-item search-action-item">
                    <a-button
                      size="small"
                      @click="handleReset"
                    >
                      重置
                    </a-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表 ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="allColumns"
              :data-source="tableData"
              :storage-key="'agreement-table-columns'"
              :loading="loading"
              :pagination="billPagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :show-delete="false"
              :selectable="false"
              row-key="id"
              @page-change="handlePageChange"
            >
              <template #agreementNoCell="{ record }">
                <a-button
                  v-permission="'agreement:view'"
                  type="link"
                  size="small"
                  @click="goDetail(record)"
                >
                  {{ record.agreementNo || '（暂无编号）' }}
                </a-button>
              </template>
              <template #typeCell="{ record }">
                <a-tag :color="typeMeta(record.agreementType).color">
                  {{ typeMeta(record.agreementType).label }}
                </a-tag>
              </template>
              <template #partyACell="{ record }">
                {{ formatParty(record.partyAName, record.partyATenantName) }}
              </template>
              <template #partyBCell="{ record }">
                {{ formatParty(record.partyBName, record.partyBTenantName, record.agreementType) }}
              </template>
              <template #versionCell="{ record }">
                <span v-if="record.currentVersionNo">
                  第 {{ record.currentVersionNo }} 版
                </span>
                <span
                  v-else
                  class="muted"
                >尚未生效</span>
              </template>
              <template #statusCell="{ record }">
                <a-tag :color="statusColor(record.status)">
                  {{ statusText(record.status) }}
                </a-tag>
              </template>
              <template #periodCell="{ record }">
                {{ formatPeriod(record.effectiveFrom, record.effectiveTo) }}
              </template>
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button
                    v-permission="'agreement:view'"
                    type="link"
                    size="small"
                    @click="goDetail(record)"
                  >
                    查看详情
                  </a-button>
                  <a-button
                    v-if="record.status === 'DRAFT'"
                    v-permission="'agreement:update'"
                    type="link"
                    size="small"
                    @click="openEdit(record)"
                  >
                    编辑
                  </a-button>
                  <a-button
                    v-if="record.status === 'ACTIVE' || record.status === 'SUSPENDED'"
                    v-permission="'agreement:version:create'"
                    type="link"
                    size="small"
                    @click="openChangeVersion(record)"
                  >
                    发起变更
                  </a-button>
                  <a-button
                    v-if="record.status === 'DRAFT'"
                    v-permission="'agreement:delete'"
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
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置（查询条件显隐 / 功能按钮） ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryConfig"
      :function-buttons-config="functionButtonConfig"
      :storage-key="PAGE_CONFIG_STORAGE_KEY"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- ═══ 新建 / 编辑协议弹窗（按三种合作对象分别引导） ═══ -->
    <a-modal
      v-model:open="formVisible"
      :title="formMode === 'create' ? '新建协议' : '修改协议草稿'"
      :width="860"
      :mask-closable="false"
      :confirm-loading="submitting"
      ok-text="保存草稿"
      cancel-text="取消"
      @ok="handleSubmitForm"
    >
      <div class="type-guide">
        <p class="type-guide-tip">
          {{ formMode === 'create'
            ? '先选清楚「这份协议是和谁签的」——三种合作对象的含义不同，条款与生效条件也不同。'
            : '协议类型在草稿建立后不可更改；要换一种合作对象，请另建一份协议。' }}
        </p>
        <div
          v-for="group in TYPE_GROUPS"
          :key="group.category"
          class="type-group"
        >
          <div class="type-group-title">
            {{ group.category }}
          </div>
          <div class="type-cards">
            <div
              v-for="t in group.types"
              :key="t"
              :class="['type-card', { active: form.agreementType === t }]"
              @click="handleTypeChange(t)"
            >
              <div class="type-card-head">
                <span class="type-card-name">{{ AGREEMENT_TYPE_META[t].label }}</span>
                <CheckCircleFilled v-if="form.agreementType === t" />
              </div>
              <div class="type-card-desc">
                {{ AGREEMENT_TYPE_META[t].detail }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <a-form
        layout="vertical"
        class="agreement-form"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item
              label="协议标题"
              required
            >
              <a-input
                v-model:value="form.title"
                placeholder="例如：2026 年度华东区代销合作框架"
                :maxlength="100"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="form.agreementType === 'CONSUMER_PROMISE' ? '承诺方主体' : '甲方主体'">
              <a-input
                :value="partyALabel"
                readonly
                placeholder="点击右侧按钮选择往来单位"
              >
                <template #suffix>
                  <a-button
                    type="link"
                    size="small"
                    @click="pickerSide = 'A'"
                  >
                    选择
                  </a-button>
                </template>
              </a-input>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="甲方所属租户">
              <a-select
                v-model:value="form.partyATenantId"
                size="small"
                show-search
                allow-clear
                option-filter-prop="label"
                placeholder="请选择租户"
                :options="tenantSelectOptions"
                :disabled="form.agreementType === 'PLATFORM_SERVICE'"
              />
              <div
                v-if="form.agreementType === 'PLATFORM_SERVICE'"
                class="field-hint"
              >
                平台服务协议的甲方固定为平台方（平台主体 + 系统租户），无需修改。
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="生效期">
              <a-range-picker
                v-model:value="formPeriod"
                style="width: 100%"
                @change="handlePeriodChange"
              />
              <div class="field-hint">
                留空表示长期有效；到期前发起变更可续签。
              </div>
            </a-form-item>
          </a-col>
          <template v-if="form.agreementType !== 'CONSUMER_PROMISE'">
            <a-col :span="12">
              <a-form-item label="乙方主体">
                <a-input
                  :value="partyBLabel"
                  readonly
                  placeholder="点击右侧按钮选择往来单位"
                >
                  <template #suffix>
                    <a-button
                      type="link"
                      size="small"
                      @click="pickerSide = 'B'"
                    >
                      选择
                    </a-button>
                  </template>
                </a-input>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="乙方所属租户">
                <a-select
                  v-model:value="form.partyBTenantId"
                  size="small"
                  show-search
                  allow-clear
                  option-filter-prop="label"
                  placeholder="请选择租户"
                  :options="tenantSelectOptions"
                />
              </a-form-item>
            </a-col>
          </template>
          <a-col
            v-else
            :span="24"
          >
            <a-alert
              type="info"
              show-icon
              message="这项承诺面向不特定消费者，不需要指定乙方。"
              description="承诺只能高于法定与平台底线（例如法定的无理由退货天数），低于底线的内容不允许保存；具体条款在详情页按类别逐项约定。"
            />
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- ═══ 发起变更弹窗 ═══ -->
    <a-modal
      v-model:open="changeVisible"
      title="发起变更"
      :width="560"
      :confirm-loading="submitting"
      ok-text="发起变更"
      cancel-text="取消"
      @ok="handleSubmitChange"
    >
      <a-alert
        type="warning"
        show-icon
        message="变更谈成之前，现行版本继续有效"
        description="发起变更只是复制出一份新的待确认版本，双方都确认后新版本才生效；现行版本在执行中的交易、结算不受影响。"
      />
      <a-form
        layout="vertical"
        style="margin-top: 12px"
      >
        <a-form-item
          label="变更原因"
          required
        >
          <a-textarea
            v-model:value="changeReason"
            :rows="3"
            :maxlength="255"
            show-count
            placeholder="例如：佣金比例由 8% 调整为 10%，自下月起执行"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ═══ 往来单位选择（复用通用弹窗，不另建候选接口） ═══ -->
    <PartnerSelectModal
      :open="pickerVisible"
      :default-tab="'customer'"
      @update:open="pickerVisible = $event"
      @select="handlePartnerSelected"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
/**
 * 协议列表页（协议模块 → 协议）
 *
 * 设计依据：docs/DOMAIN-MODEL-USER-PARTY-TENANT-v1.md §十二「协议模块」
 *   · 三类协议：平台↔租户 / 租户↔租户（代销、购销框架）/ 租户↔消费者（单方承诺，只能加码）
 *   · 新建入口按三类分别引导，讲清「和谁签、要约定什么」
 *   · 已生效版本只读：列表只对「洽谈中」给编辑与删除，对「生效中」只给「发起变更」
 *   · 列配置走数据表表头齿轮（BillDetailTable 内置），页面配置只管查询条件与功能按钮
 */
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import {
  PlusOutlined, ReloadOutlined, SettingOutlined, BookOutlined, CheckCircleFilled,
  FileAddOutlined, ProfileOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import PartnerSelectModal from '@/components/PartnerSelectModal/index.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { usePermission } from '@/composables/usePermission'
import { useUserStore } from '@/stores/user'
import { tenantApi } from '@/api/tenant'
import {
  agreementApi,
  AGREEMENT_TYPE_META,
  AGREEMENT_STATUS_TEXT,
  AGREEMENT_STATUS_COLOR,
  type Agreement,
  type AgreementType,
  type AgreementStatus,
  type AgreementCreateBody,
  type AgreementQuery
} from '@/api/agreement'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { checkPermission } = usePermission()

/** 平台侧条款字典入口（用户可见性由权限码决定，租户侧不会有这个码） */
const canViewTermOption = computed(() =>
  checkPermission('agreement:term-option:list') || checkPermission('agreement:platform:term-option:manage')
)

/**
 * 契约模板入口：租户侧用 agreement:template:list，平台侧用合规抽查读 agreement:platform:template:read ——
 * 与后端模板接口的「或」关系一致（两类码任一即可读）。
 */
const canViewTemplate = computed(() =>
  checkPermission('agreement:template:list') || checkPermission('agreement:platform:template:read')
)

// ═══ 入口视图：同一个组件被两条菜单路径复用（本仓既有做法：入口直达型菜单） ═══
// · 「系统 → 协议契约 → 平台协议」 /agreement/platform：只看平台 ↔ 租户的平台服务协议；
// · 「设置 → 协议契约 → 协议列表」  /agreement           ：租户级协议（代销 / 购销框架 / 消费者单方承诺）。
// 两个入口共用本页，差别只在「协议范围（agreementScope，由服务端过滤）+ 标题」，
// 用户进来后仍可手动改类型筛选，不做锁死。
const isPlatformView = computed(() => route.path.startsWith('/agreement/platform'))

/**
 * 本入口对应的协议范围（传给后端 agreementScope，**由服务端进 SQL 过滤**）。
 *
 * 为什么必须交给服务端：后端分页是按查询条件算 total 的。早先版本是「取回后在本地剔除平台协议行」，
 * 结果 total 仍是服务端口径（含被剔除的行），出现「第 1 页只显示 3 行、却写着共 10 条」。
 * 现在范围条件进 SQL，分页组件的 total 就是过滤后的条数，显示与统计天然一致。
 */
const entryScope = computed<'PLATFORM' | 'TENANT'>(() => (isPlatformView.value ? 'PLATFORM' : 'TENANT'))

const pageTitle = computed(() => (isPlatformView.value ? '平台协议' : '协议列表'))
const pageHint = computed(() => (isPlatformView.value
  ? '平台与租户之间的服务协议'
  : '租户级协议：代销 / 购销框架 / 消费者单方承诺'))

/** 本列表页的两个入口路径（比对前先去掉末尾斜杠）；详情页、条款字典等不算 */
function isListViewPath(path: string): boolean {
  const normalized = path.replace(/\/+$/, '')
  return normalized === '/agreement' || normalized === '/agreement/platform'
}

/**
 * 类型筛选复位到「不限」：本入口看哪一档协议由 agreementScope 决定，类型下拉只是用户可选的进一步收窄。
 * 切入口 / 点重置时都调它，避免上一个入口选过的具体类型残留下来。
 */
function applyEntryDefaultFilter() {
  searchParams.agreementType = undefined
}

// ═══ 查询区（横向自适应网格） ═══
const searchGridRef = ref<HTMLElement | null>(null)
const searchActionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(searchActionRef, searchGridRef)

const searchParams = reactive({
  agreementType: undefined as AgreementType | undefined,
  status: undefined as AgreementStatus | undefined,
  keyword: ''
})

// 起始时类型筛选为空（本入口看哪一档由 entryScope 决定，见上）
applyEntryDefaultFilter()

const typeOptions = (Object.keys(AGREEMENT_TYPE_META) as AgreementType[]).map(code => ({
  label: AGREEMENT_TYPE_META[code].label,
  value: code
}))

/** 类型筛选的占位文案：讲清本入口默认看哪一类（清空类型筛选后也会显示这句） */
const typePlaceholder = computed(() => (isPlatformView.value ? '默认：平台服务协议' : '默认：全部租户级协议'))

const statusOptions = (Object.keys(AGREEMENT_STATUS_TEXT) as AgreementStatus[]).map(code => ({
  label: AGREEMENT_STATUS_TEXT[code],
  value: code
}))

// ═══ 分页与数据 ═══
const loading = ref(false)
const tableData = ref<Agreement[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total
}))

// ═══ 页面配置（查询条件显隐 / 功能按钮启用） ═══
const PAGE_CONFIG_STORAGE_KEY = 'agreement-page-config'
const showPageConfig = ref(false)

interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'agreementType', label: '协议类型', visible: true },
  { key: 'status', label: '协议状态', visible: true },
  { key: 'keyword', label: '协议编号 / 标题', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'create', label: '新建协议', enabled: true },
  { key: 'wizard', label: '发起契约', enabled: true },
  { key: 'template', label: '契约模板', enabled: true },
  { key: 'termOption', label: '条款字典', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true }
]

const queryConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

function isFieldVisible(key: string): boolean {
  const found = queryConfig.value.find(f => f.key === key)
  return found ? found.visible : true
}

/** 功能按钮启用状态（页面配置里可关，关掉即不显示，避免出现点了没用的按钮） */
function isButtonEnabled(key: string): boolean {
  const found = functionButtonConfig.value.find(f => f.key === key)
  return found ? found.enabled : true
}

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed.queryFields)) {
      queryConfig.value = DEFAULT_QUERY_FIELDS.map(df => {
        const saved = parsed.queryFields.find((f: QueryFieldSetting) => f.key === df.key)
        return saved ? { ...df, ...saved } : { ...df }
      })
    }
    if (Array.isArray(parsed.functionButtons)) {
      functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
        const saved = parsed.functionButtons.find((f: FunctionButtonSetting) => f.key === bf.key)
        return saved ? { ...bf, ...saved } : { ...bf }
      })
    }
  } catch { /* 本地配置损坏时按默认展示 */ }
}

function handlePageConfigChange(config: any) {
  try {
    localStorage.setItem(PAGE_CONFIG_STORAGE_KEY, JSON.stringify({
      queryFields: config?.queryFields || [],
      functionButtons: config?.functionButtons || functionButtonConfig.value
    }))
  } catch { /* 忽略写入失败（隐私模式） */ }
  loadPageConfig()
}

// ═══ 列定义（列配置齿轮在数据表表头） ═══
const allColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 260, fixed: 'right', slotName: 'actionCell' },
  { title: '协议编号', key: 'agreementNo', field: 'agreementNo', width: 170, type: 'slot', slotName: 'agreementNoCell' },
  { title: '协议标题', key: 'title', field: 'title', width: 240 },
  { title: '协议类型', key: 'agreementType', field: 'agreementType', width: 130, type: 'slot', slotName: 'typeCell' },
  { title: '甲方（主体 / 租户）', key: 'partyA', width: 200, type: 'slot', slotName: 'partyACell' },
  { title: '乙方（主体 / 租户）', key: 'partyB', width: 200, type: 'slot', slotName: 'partyBCell' },
  { title: '当前版本', key: 'currentVersionNo', field: 'currentVersionNo', width: 100, align: 'center', type: 'slot', slotName: 'versionCell' },
  { title: '协议状态', key: 'status', field: 'status', width: 110, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '生效期', key: 'period', width: 200, type: 'slot', slotName: 'periodCell' },
  { title: '创建时间', key: 'createTime', field: 'createTime', width: 160, defaultHidden: true }
]

// ═══ 展示辅助 ═══
function typeMeta(type?: string) {
  return AGREEMENT_TYPE_META[type as AgreementType] || { label: type || '未知', color: 'default', shortDesc: '', detail: '' }
}
function statusText(status?: string) {
  return AGREEMENT_STATUS_TEXT[status as AgreementStatus] || status || '未知'
}
function statusColor(status?: string) {
  return AGREEMENT_STATUS_COLOR[status as AgreementStatus] || 'default'
}
/** 主体（租户）合并展示；对消费者承诺的乙方是不特定消费者 */
function formatParty(partyName?: string, tenantName?: string, type?: string): string {
  if (type === 'CONSUMER_PROMISE' && !partyName && !tenantName) return '不特定消费者'
  const name = partyName || '（未指定主体）'
  return tenantName ? `${name}（${tenantName}）` : name
}
/** 生效期展示；两端都空表示长期有效 */
function formatPeriod(from?: string, to?: string): string {
  if (!from && !to) return '长期有效'
  return `${from || '—'} 至 ${to || '长期'}`
}

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: AgreementQuery = { current: pagination.current, size: pagination.pageSize }
    // 入口决定的范围条件交给服务端（agreementScope 进 SQL）：平台协议入口只看平台级，
    // 协议列表入口只看租户级三类。这样 total 就是过滤后的条数，不再需要本地剔除行。
    params.agreementScope = entryScope.value
    // 用户手动选的具体类型更具体，服务端以它为准（此时范围条件相当于被收窄）
    if (searchParams.agreementType) params.agreementType = searchParams.agreementType
    if (searchParams.status) params.status = searchParams.status
    if (searchParams.keyword) params.keyword = searchParams.keyword.trim()
    const res: any = await agreementApi.page(params)
    const body = res?.data ?? res ?? {}
    tableData.value = body.records || body.list || []
    // 分页组件的 total 直接用服务端返回值（服务端已按 agreementScope 过滤，口径一致）
    pagination.total = Number(body.total) || 0
  } catch (error: any) {
    console.warn('[协议列表] 加载失败', error)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  // 重置回「本入口的默认条件」（范围由入口决定、类型不限），
  // 而不是清成不带任何条件 —— 范围条件不随搜索条件，故租户入口不会把平台协议又露出来
  searchParams.status = undefined
  searchParams.keyword = ''
  applyEntryDefaultFilter()
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ═══ 导航 ═══
function goDetail(record: Agreement) {
  router.push(`/agreement/detail/${record.id}`)
}
function goTermOptionPage() {
  router.push('/agreement/term-option')
}
/** 发起契约向导（隐藏路由，入口在本页工具栏；选模板或从零起草都在向导里） */
function goWizardPage() {
  router.push('/agreement/wizard')
}
/** 契约模板管理（隐藏路由，入口在本页工具栏；平台侧/租户侧共用同一页，可见范围由服务端判定） */
function goTemplatePage() {
  router.push('/agreement/template')
}

// ═══ 新建 / 编辑 ═══
const TYPE_GROUPS: Array<{ category: string; types: AgreementType[] }> = [
  { category: '平台 ↔ 租户（入驻 / 服务约定）', types: ['PLATFORM_SERVICE'] },
  { category: '租户 ↔ 租户（双方对等，条款需双方确认）', types: ['DISTRIBUTION', 'GOODS_FRAMEWORK'] },
  { category: '租户 ↔ 消费者（单方公开承诺，只能加码）', types: ['CONSUMER_PROMISE'] }
]

const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const editingId = ref<number | string | null>(null)
const formPeriod = ref<[Dayjs, Dayjs] | null>(null)

const form = reactive({
  agreementType: 'DISTRIBUTION' as AgreementType,
  title: '',
  partyAId: undefined as number | string | undefined,
  partyATenantId: undefined as number | string | undefined,
  partyBId: undefined as number | string | undefined,
  partyBTenantId: undefined as number | string | undefined,
  effectiveFrom: '' as string | undefined,
  effectiveTo: '' as string | undefined
})

const partyALabel = ref('')
const partyBLabel = ref('')

// 往来单位选择
const pickerSide = ref<'A' | 'B' | null>(null)
const pickerVisible = computed({
  get: () => pickerSide.value !== null,
  set: (v: boolean) => { if (!v) pickerSide.value = null }
})

function handlePartnerSelected(record: any) {
  const side = pickerSide.value
  const name = record?.partnerName || record?.partnerShortName || ''
  if (side === 'A') {
    form.partyAId = record?.id
    partyALabel.value = name
  } else if (side === 'B') {
    form.partyBId = record?.id
    partyBLabel.value = name
  }
  pickerSide.value = null
}

// 租户下拉（平台可见；租户侧取不到时降级为可手填，不阻断录入）
const tenantOptions = ref<Array<{ label: string; value: number | string }>>([])
const tenantSelectOptions = computed(() => tenantOptions.value)

async function loadTenantOptions() {
  try {
    const res: any = await tenantApi.getPage({ pageNum: 1, pageSize: 200 })
    const body = res?.data ?? res ?? {}
    const list = body.records || body.list || []
    tenantOptions.value = list.map((t: any) => ({
      label: t.tenantName || t.tenantCode || String(t.id),
      value: t.id
    }))
  } catch {
    // 租户侧无平台租户查询权限属正常：保留空列表，不打扰用户
    tenantOptions.value = []
  }
}

function resetForm() {
  form.agreementType = 'DISTRIBUTION'
  form.title = ''
  form.partyAId = undefined
  form.partyATenantId = undefined
  form.partyBId = undefined
  form.partyBTenantId = undefined
  form.effectiveFrom = ''
  form.effectiveTo = ''
  formPeriod.value = null
  partyALabel.value = ''
  partyBLabel.value = ''
}

/** 切换协议类型：平台协议锁定甲方租户为平台方；消费者承诺清空乙方 */
function handleTypeChange(type: AgreementType) {
  // 草稿建立后不允许改类型：两端主体与条款语义都跟着类型走，改类型等同换一份协议
  if (formMode.value === 'edit') return
  form.agreementType = type
  if (type === 'PLATFORM_SERVICE') {
    // 平台方 = 平台主体 + 系统租户（租户 1）
    form.partyATenantId = 1
  }
  if (type === 'CONSUMER_PROMISE') {
    form.partyBId = undefined
    form.partyBTenantId = undefined
    partyBLabel.value = ''
  }
}

function openCreate() {
  resetForm()
  formMode.value = 'create'
  editingId.value = null
  // 默认带出本方租户，减少建档时的手工选择
  form.partyATenantId = userStore.tenantId || 1
  formVisible.value = true
}

function openEdit(record: Agreement) {
  resetForm()
  formMode.value = 'edit'
  editingId.value = record.id
  form.agreementType = record.agreementType
  form.title = record.title || ''
  form.partyAId = record.partyAId
  form.partyATenantId = record.partyATenantId
  form.partyBId = record.partyBId
  form.partyBTenantId = record.partyBTenantId
  form.effectiveFrom = record.effectiveFrom || ''
  form.effectiveTo = record.effectiveTo || ''
  partyALabel.value = record.partyAName || ''
  partyBLabel.value = record.partyBName || ''
  formVisible.value = true
}

function handlePeriodChange(dates: any) {
  form.effectiveFrom = dates?.[0] ? String(dates[0].format('YYYY-MM-DD')) : ''
  form.effectiveTo = dates?.[1] ? String(dates[1].format('YYYY-MM-DD')) : ''
}

/** 提交前校验：三种类型的必填项不同，提示要具体 */
function validateForm(): string | null {
  if (!form.title.trim()) return '请填写协议标题'
  if (!form.partyAId) return form.agreementType === 'CONSUMER_PROMISE' ? '请选择承诺方主体' : '请选择甲方主体'
  if (!form.partyATenantId) return '请选择甲方所属租户'
  if (form.agreementType !== 'CONSUMER_PROMISE') {
    if (!form.partyBId) return '请选择乙方主体'
    if (!form.partyBTenantId) return '请选择乙方所属租户'
  }
  return null
}

async function handleSubmitForm() {
  const invalid = validateForm()
  if (invalid) {
    message.warning(invalid)
    return
  }
  const body: AgreementCreateBody = {
    agreementType: form.agreementType,
    partyAId: form.partyAId as number | string,
    partyATenantId: form.partyATenantId as number | string,
    title: form.title.trim(),
    effectiveFrom: form.effectiveFrom || undefined,
    effectiveTo: form.effectiveTo || undefined
  }
  if (form.agreementType !== 'CONSUMER_PROMISE') {
    body.partyBId = form.partyBId
    body.partyBTenantId = form.partyBTenantId
  }
  submitting.value = true
  try {
    if (formMode.value === 'create') {
      const newId: any = await agreementApi.create(body)
      message.success('协议草稿已建立，请到详情页逐项约定条款')
      formVisible.value = false
      await fetchData()
      const id = newId?.data ?? newId
      if (id) router.push(`/agreement/detail/${id}`)
    } else if (editingId.value !== null) {
      // 改草稿时不提交协议类型：类型决定两端主体与条款语义，草稿建立后不再变更
      const updateBody: any = { ...body }
      delete updateBody.agreementType
      await agreementApi.update(editingId.value, updateBody)
      message.success('草稿已保存')
      formVisible.value = false
      fetchData()
    }
  } catch (error: any) {
    console.warn('[协议列表] 保存失败', error)
  } finally {
    submitting.value = false
  }
}

// ═══ 发起变更 ═══
const changeVisible = ref(false)
const changeReason = ref('')
const changingId = ref<number | string | null>(null)

function openChangeVersion(record: Agreement) {
  changingId.value = record.id
  changeReason.value = ''
  changeVisible.value = true
}

async function handleSubmitChange() {
  if (!changeReason.value.trim()) {
    message.warning('请填写变更原因，双方确认时要看这一条')
    return
  }
  if (changingId.value === null) return
  submitting.value = true
  try {
    const newVersionId: any = await agreementApi.createVersion(changingId.value, {
      changeReason: changeReason.value.trim()
    })
    message.success('已发起变更，现行版本继续有效，等对方确认')
    changeVisible.value = false
    await fetchData()
    const vid = newVersionId?.data ?? newVersionId
    const url = `/agreement/detail/${changingId.value}`
    router.push(vid ? `${url}?versionId=${vid}` : url)
  } catch (error: any) {
    console.warn('[协议列表] 发起变更失败', error)
  } finally {
    submitting.value = false
  }
}

// ═══ 删除（仅草稿） ═══
function handleDelete(record: Agreement) {
  Modal.confirm({
    title: '删除协议草稿',
    content: `确定删除「${record.title || record.agreementNo}」这份草稿吗？删除后不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await agreementApi.remove(record.id)
        message.success('已删除')
        fetchData()
      } catch (error: any) {
        console.warn('[协议列表] 删除失败', error)
      }
    }
  })
}

const handleError = (error: Error) => {
  console.error('[协议列表] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// 本页可能被 keep-alive 缓存（两个入口是同组件）：路径在两个入口之间切换时
// 按新入口重算默认条件并重新查询，否则会沿用上一个入口的筛选，两个入口看起来还是一样。
watch(() => route.path, (path) => {
  if (!isListViewPath(path)) return
  applyEntryDefaultFilter()
  handleSearch()
})

onMounted(() => {
  loadPageConfig()
  loadTenantOptions()
  fetchData()
})
</script>

<style scoped>
/* ═══ 查询区：横向自适应网格（禁止纵向单列） ═══ */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; align-items: center; }
.search-field-item { display: flex; min-width: 0; }
.search-field-item :deep(.ant-input-wrapper),
.search-field-item :deep(.ant-input-affix-wrapper) { width: 100%; font-size: 13px; }
.search-field-item :deep(.ant-select) { width: 100%; }
.search-field-item :deep(.ant-picker) { width: 100%; }
.search-select-wrap { display: flex; align-items: center; width: 100%; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; }
.search-select-wrap:hover { border-color: #4096ff; }
.search-select-label { font-size: 13px; color: rgba(0, 0, 0, 0.65); white-space: nowrap; flex-shrink: 0; padding-left: 8px; }
.search-select-wrap :deep(.ant-select) { flex: 1; min-width: 0; }
.search-select-wrap :deep(.ant-select .ant-select-selector) {
  border: none !important; border-radius: 0 !important; box-shadow: none !important;
  padding-top: 0 !important; padding-bottom: 0 !important; display: flex; align-items: center;
}
.search-action-group { display: flex; flex-wrap: nowrap; align-items: center; }
.search-action-group .search-field-item { width: auto; flex: 0 0 auto; margin-right: 4px; }
.table-area { flex: 1; min-height: 0; overflow: hidden; }
.muted { color: #999; }
/* 标题旁的入口说明 */
.view-hint { font-size: 12px; color: #999; }

/* ═══ 新建弹窗：三类合作对象的引导卡片 ═══ */
.type-guide { margin-bottom: 16px; }
.type-guide-tip { margin: 0 0 8px; font-size: 13px; color: #666; }
.type-group { margin-bottom: 10px; }
.type-group-title { font-size: 13px; font-weight: 600; color: #333; margin-bottom: 6px; }
.type-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 10px; }
.type-card {
  border: 1px solid #d9d9d9; border-radius: 6px; padding: 10px 12px;
  cursor: pointer; background: #fff; transition: all 0.2s; min-height: 84px;
}
.type-card:hover { border-color: #4096ff; }
.type-card.active { border-color: #1890ff; background: #e6f7ff; box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.12); }
.type-card-head { display: flex; align-items: center; justify-content: space-between; font-weight: 600; font-size: 13px; color: #333; margin-bottom: 4px; }
.type-card-head :deep(.anticon) { color: #1890ff; }
.type-card-desc { font-size: 12px; line-height: 1.6; color: #666; }
.field-hint { font-size: 12px; color: #999; line-height: 1.5; margin-top: 2px; }
.agreement-form :deep(.ant-form-item) { margin-bottom: 12px; }
</style>
