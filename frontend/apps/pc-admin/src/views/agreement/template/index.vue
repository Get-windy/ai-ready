<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      full-height
      title="契约模板"
    >
      <template #headerContent>
        <span class="view-hint">
          平台模板全员可选；租户模板仅本租户可选。模板是显式选择的起点，不是自动套用的默认值。
        </span>
      </template>

      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新建模板 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('create')"
              v-permission="'agreement:template:create'"
              type="primary"
              size="small"
              @click="openCreate"
            >
              <PlusOutlined /> 新建模板
            </a-button>
            <a-tooltip
              title="平台可读全部模板（含租户模板）用于合规抽查，但不能改租户的模板"
              placement="bottom"
            >
              <a-button
                size="small"
                @click="goWizard"
              >
                <FileAddOutlined /> 用模板发起契约
              </a-button>
            </a-tooltip>
          </a-space>
        </template>

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
                  v-if="isFieldVisible('scope')"
                  class="search-field-item"
                >
                  <div class="search-select-wrap">
                    <span class="search-select-label">模板级别</span>
                    <a-select
                      v-model:value="searchParams.scope"
                      size="small"
                      allow-clear
                      placeholder="全部"
                      :options="scopeOptions"
                      @change="handleSearch"
                    />
                  </div>
                </div>
                <div
                  v-if="isFieldVisible('agreementType')"
                  class="search-field-item"
                >
                  <div class="search-select-wrap">
                    <span class="search-select-label">适用协议</span>
                    <a-select
                      v-model:value="searchParams.agreementType"
                      size="small"
                      allow-clear
                      placeholder="全部"
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
                    <span class="search-select-label">启用状态</span>
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
                    placeholder="模板名称 / 说明"
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

        <!-- ═══ 数据表（列配置走表头齿轮） ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="allColumns"
              :data-source="tableData"
              :storage-key="'agreement-template-table-columns'"
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
              <template #nameCell="{ record }">
                <a-button
                  type="link"
                  size="small"
                  @click="openDetail(record)"
                >
                  {{ record.templateName || '（未命名模板）' }}
                </a-button>
                <div
                  v-if="record.description"
                  class="tpl-desc"
                >
                  {{ record.description }}
                </div>
              </template>
              <template #scopeCell="{ record }">
                <a-tag :color="TEMPLATE_SCOPE_COLOR[record.scope] || 'default'">
                  {{ record.scopeLabel || TEMPLATE_SCOPE_TEXT[record.scope] || record.scope }}
                </a-tag>
                <div
                  v-if="record.scope === 'TENANT'"
                  class="tpl-desc"
                >
                  {{ record.tenantName || '' }}
                </div>
              </template>
              <template #typeCell="{ record }">
                {{ typeLabel(record.agreementType) }}
              </template>
              <template #contentCell="{ record }">
                <span class="tpl-count">字段 {{ record.settingCount ?? 0 }}</span>
                <span class="tpl-count">条款 {{ record.termCount ?? 0 }}</span>
                <span class="tpl-count">文字 {{ record.narrativeCount ?? 0 }}</span>
              </template>
              <template #legalCell="{ record }">
                <a-tag :color="LEGAL_REVIEW_COLOR[record.legalReviewStatus] || 'default'">
                  {{ record.legalReviewStatusLabel || LEGAL_REVIEW_TEXT[record.legalReviewStatus] || '—' }}
                </a-tag>
              </template>
              <template #statusCell="{ record }">
                <a-tag :color="record.status === 1 ? 'green' : 'default'">
                  {{ record.statusLabel || (record.status === 1 ? '启用' : '停用') }}
                </a-tag>
              </template>
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button
                    type="link"
                    size="small"
                    @click="openDetail(record)"
                  >
                    查看
                  </a-button>
                  <a-tooltip
                    :title="record.manageable ? '修改模板内容' : (record.manageableHint || '当前账号不能修改这个模板')"
                    placement="bottom"
                  >
                    <a-button
                      v-permission="'agreement:template:update'"
                      type="link"
                      size="small"
                      :disabled="!record.manageable"
                      @click="openEdit(record)"
                    >
                      修改
                    </a-button>
                  </a-tooltip>
                  <a-tooltip
                    :title="record.manageable ? '删除模板（已基于它发起的协议不受影响）' : (record.manageableHint || '当前账号不能删除这个模板')"
                    placement="bottom"
                  >
                    <a-button
                      v-permission="'agreement:template:delete'"
                      type="link"
                      size="small"
                      danger
                      :disabled="!record.manageable"
                      @click="handleDelete(record)"
                    >
                      删除
                    </a-button>
                  </a-tooltip>
                </a-space>
                <div
                  v-if="!record.manageable && record.manageableHint"
                  class="tpl-hint"
                >
                  {{ record.manageableHint }}
                </div>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryConfig"
      :function-buttons-config="functionButtonConfig"
      :storage-key="PAGE_CONFIG_STORAGE_KEY"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- ═══ 模板详情（含平台合规抽查读） ═══ -->
    <a-modal
      v-model:open="detailVisible"
      :title="`模板详情 · ${detailData?.templateName || ''}`"
      :width="820"
      :footer="null"
    >
      <a-alert
        v-if="!detailData?.manageable"
        type="info"
        show-icon
        class="tpl-alert"
        message="只读查看"
        :description="detailData?.manageableHint || '当前账号只能查看这个模板，不能修改。'"
      />
      <a-alert
        type="warning"
        show-icon
        class="tpl-alert"
        message="模板里的内容是「预填建议」，不是已经约定"
        description="从模板发起后，双方仍要在那一版上逐项确认并签署；模板里有不等于已经约定，已约定只以双方签署的那一版为准。"
      />
      <a-descriptions
        :column="2"
        size="small"
        bordered
        class="tpl-desc-block"
      >
        <a-descriptions-item label="模板级别">
          {{ detailData?.scopeLabel || TEMPLATE_SCOPE_TEXT[detailData?.scope || ''] || '—' }}
          <span v-if="detailData?.tenantName">（{{ detailData.tenantName }}）</span>
        </a-descriptions-item>
        <a-descriptions-item label="适用协议">
          {{ typeLabel(detailData?.agreementType) }}
        </a-descriptions-item>
        <a-descriptions-item label="法务审核">
          {{ detailData?.legalReviewStatusLabel || LEGAL_REVIEW_TEXT[detailData?.legalReviewStatus || ''] || '—' }}
        </a-descriptions-item>
        <a-descriptions-item label="启用状态">
          {{ detailData?.statusLabel || (detailData?.status === 1 ? '启用' : '停用') }}
        </a-descriptions-item>
      </a-descriptions>

      <a-tabs size="small">
        <a-tab-pane
          key="settings"
          :tab="`字段建议值（${detailData?.settings?.length || 0}）`"
        >
          <div
            v-if="detailData?.settings?.length"
            class="tpl-list"
          >
            <div
              v-for="s in detailData.settings"
              :key="s.settingKey"
              class="tpl-item"
            >
              <div class="tpl-item-head">
                <span class="tpl-item-name">{{ s.label || s.settingKey }}</span>
                <a-tag
                  v-if="s.consumerPointLabel"
                  color="blue"
                >
                  影响：{{ s.consumerPointLabel }}
                </a-tag>
              </div>
              <div class="tpl-item-value">
                {{ s.displayValue || s.value || '（未填）' }}
              </div>
              <div
                v-if="s.semantics"
                class="tpl-desc"
              >
                {{ s.semantics }}
              </div>
            </div>
          </div>
          <div
            v-else
            class="tpl-empty"
          >
            这个模板没有预填字段设定。
          </div>
        </a-tab-pane>
        <a-tab-pane
          key="terms"
          :tab="`条款建议（${detailData?.terms?.length || 0}）`"
        >
          <div
            v-if="detailData?.terms?.length"
            class="tpl-list"
          >
            <div
              v-for="t in detailData.terms"
              :key="t.termCode"
              class="tpl-item"
            >
              <div class="tpl-item-head">
                <span class="tpl-item-name">{{ t.termName || t.termCode }}</span>
                <a-tag
                  v-if="t.required"
                  color="red"
                >
                  必填
                </a-tag>
              </div>
              <div class="tpl-item-value">
                {{ t.optionLabel || t.optionCode || '（未选）' }}
                <span v-if="t.paramValue">（参数：{{ t.paramValue }}）</span>
              </div>
              <div
                v-if="t.semantics"
                class="tpl-desc"
              >
                {{ t.semantics }}
              </div>
            </div>
          </div>
          <div
            v-else
            class="tpl-empty"
          >
            这个模板没有预填条款。
          </div>
        </a-tab-pane>
        <a-tab-pane
          key="narratives"
          :tab="`文字条款（${detailData?.narratives?.length || 0}）`"
        >
          <a-alert
            type="warning"
            show-icon
            class="tpl-alert"
            message="此类条款系统不会自动执行，需人工处理"
            :description="detailData?.narratives?.[0]?.manualNotice || NARRATIVE_MANUAL_NOTICE_FALLBACK"
          />
          <div
            v-if="detailData?.narratives?.length"
            class="tpl-list"
          >
            <div
              v-for="n in detailData.narratives"
              :key="n.sectionCode"
              class="tpl-item"
            >
              <div class="tpl-item-head">
                <span class="tpl-item-name">{{ n.sectionTitle || n.sectionCode }}</span>
              </div>
              <div class="tpl-item-text">
                {{ n.contentText }}
              </div>
            </div>
          </div>
          <div
            v-else
            class="tpl-empty"
          >
            这个模板没有预填文字条款。
          </div>
        </a-tab-pane>
      </a-tabs>

      <div class="tpl-detail-actions">
        <a-button
          type="primary"
          size="small"
          @click="applyTemplate(detailData)"
        >
          用这个模板发起契约
        </a-button>
      </div>
    </a-modal>

    <!-- ═══ 新建 / 修改模板 ═══ -->
    <a-modal
      v-model:open="formVisible"
      :title="formMode === 'create' ? '新建契约模板' : '修改契约模板'"
      :width="880"
      :mask-closable="false"
      :confirm-loading="submitting"
      ok-text="保存模板"
      cancel-text="取消"
      @ok="handleSubmitForm"
    >
      <a-alert
        type="warning"
        show-icon
        class="tpl-alert"
        message="模板是显式选择的起点，不是自动套用的默认值"
        description="这里填的是「发起时替你预填的内容」。从模板发起后，双方仍要在那一版上逐项确认并签署 —— 模板里有不等于已经约定。"
      />
      <a-form
        layout="vertical"
        class="tpl-form"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="模板名称">
              <a-input
                v-model:value="form.templateName"
                size="small"
                :maxlength="64"
                placeholder="例如：标准代销模板（含 30 天账期）"
              />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="模板级别">
              <a-select
                v-model:value="form.scope"
                size="small"
                :disabled="formMode === 'edit'"
                :options="formScopeOptions"
              />
              <div class="tpl-desc">
                平台模板只有平台侧能建；级别建好后不可更改。
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="适用协议类型">
              <a-select
                v-model:value="form.agreementType"
                size="small"
                :options="typeOptions"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="法务审核">
              <a-select
                v-model:value="form.legalReviewStatus"
                size="small"
                :options="legalOptions"
              />
              <div class="tpl-desc">
                判定含不合法条款的模板不能用于发起契约（违法条款无效）。
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="启用状态">
              <a-select
                v-model:value="form.status"
                size="small"
                :options="[{ label: '启用', value: 1 }, { label: '停用', value: 0 }]"
              />
              <div class="tpl-desc">
                停用的模板不能用发起，但仍可在列表里看到。
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="18">
            <a-form-item label="模板说明（选填）">
              <a-input
                v-model:value="form.description"
                size="small"
                :maxlength="200"
                placeholder="这个模板适合什么场景、双方要注意什么"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>

      <a-tabs
        v-model:activeKey="formTab"
        size="small"
      >
        <!-- ── 预填：字段设定 ── -->
        <a-tab-pane
          key="settings"
          tab="预填字段设定"
        >
          <div
            v-if="!settingDefs.length"
            class="tpl-empty"
          >
            取不到字段元数据（可能是当前账号没有「协议字段字典」的查看权限）。
            模板仍可保存，只是这次不能预填字段设定。
          </div>
          <div
            v-else
            class="tpl-setting-list"
          >
            <div
              v-for="def in settingDefs"
              :key="def.settingKey"
              class="tpl-setting"
            >
              <div class="tpl-setting-head">
                <span class="tpl-item-name">{{ def.label || def.settingKey }}</span>
                <a-tag
                  v-if="def.consumerPointLabel"
                  color="blue"
                >
                  影响：{{ def.consumerPointLabel }}
                </a-tag>
                <span
                  v-if="def.consumerSemantics"
                  class="tpl-desc tpl-inline"
                >
                  {{ def.consumerSemantics }}
                </span>
              </div>
              <div class="tpl-setting-control">
                <a-select
                  v-if="def.valueType === 'ENUM'"
                  :value="settingValues[def.settingKey] || undefined"
                  size="small"
                  class="tpl-control"
                  allow-clear
                  placeholder="不预填"
                  :options="(def.options || []).map(c => ({ value: c, label: settingEnumOptionLabel(def.settingKey, c) }))"
                  @change="(v: any) => (settingValues[def.settingKey] = v === undefined || v === null ? '' : String(v))"
                />
                <template v-else-if="def.valueType === 'BOOL'">
                  <a-select
                    :value="settingValues[def.settingKey] || undefined"
                    size="small"
                    class="tpl-control"
                    allow-clear
                    placeholder="不预填"
                    :options="[{ label: '是', value: '是' }, { label: '否', value: '否' }]"
                    @change="(v: any) => (settingValues[def.settingKey] = v ? String(v) : '')"
                  />
                </template>
                <a-date-picker
                  v-else-if="def.valueType === 'DATE'"
                  :value="settingValues[def.settingKey] || undefined"
                  size="small"
                  class="tpl-control"
                  value-format="YYYY-MM-DD"
                  placeholder="不预填"
                  @change="(v: any) => (settingValues[def.settingKey] = v || '')"
                />
                <a-input-number
                  v-else-if="def.valueType === 'NUMBER' || def.valueType === 'DURATION'"
                  :value="settingValues[def.settingKey] ? Number(settingValues[def.settingKey]) : undefined"
                  size="small"
                  class="tpl-control"
                  :addon-after="def.valueType === 'DURATION' ? '天' : undefined"
                  placeholder="不预填"
                  @change="(v: any) => (settingValues[def.settingKey] = v === null || v === undefined ? '' : String(v))"
                />
                <a-input
                  v-else
                  :value="settingValues[def.settingKey] || ''"
                  size="small"
                  class="tpl-control"
                  placeholder="不预填"
                  @change="(e: any) => (settingValues[def.settingKey] = e.target.value)"
                />
              </div>
            </div>
          </div>
        </a-tab-pane>

        <!-- ── 预填：条款 ── -->
        <a-tab-pane
          key="terms"
          tab="预填条款"
        >
          <div
            v-if="!termGroups.length"
            class="tpl-empty"
          >
            取不到条款字典（平台还没有定义可选项），或当前账号没有查看权限。
          </div>
          <div
            v-else
            class="tpl-list"
          >
            <div
              v-for="group in termGroups"
              :key="group.termCode"
              class="tpl-term"
            >
              <div class="tpl-item-head">
                <span class="tpl-item-name">{{ group.termName || group.termCode }}</span>
                <a-tag
                  v-if="group.required || group.options?.some(o => o.required)"
                  color="red"
                >
                  必填
                </a-tag>
              </div>
              <div class="tpl-setting-control">
                <a-select
                  :value="termSelection[group.termCode]?.optionCode"
                  size="small"
                  class="tpl-control"
                  allow-clear
                  placeholder="不预填（平台不给默认值）"
                  :options="group.options.map(o => ({ value: o.optionCode, label: o.optionLabel }))"
                  @change="(v: any) => (termSelection[group.termCode] = { optionCode: v, paramValue: '' })"
                />
                <a-input
                  v-if="paramNameOf(group)"
                  v-model:value="termSelection[group.termCode].paramValue"
                  size="small"
                  class="tpl-param"
                  :placeholder="`请填写${paramNameOf(group)}`"
                />
              </div>
              <div
                v-if="semanticsOf(group)"
                class="tpl-desc"
              >
                选这一项后系统会这样执行：{{ semanticsOf(group) }}
              </div>
            </div>
          </div>
        </a-tab-pane>

        <!-- ── 预填：文字条款 ── -->
        <a-tab-pane
          key="narratives"
          tab="预填文字条款"
        >
          <a-alert
            type="warning"
            show-icon
            class="tpl-alert"
            message="此类条款系统不会自动执行，需人工处理"
            :description="NARRATIVE_MANUAL_NOTICE_FALLBACK"
          />
          <div
            v-for="sec in NARRATIVE_SECTION_OPTIONS"
            :key="sec.value"
            class="tpl-narrative"
          >
            <div class="tpl-item-head">
              <span class="tpl-item-name">{{ sec.label }}</span>
            </div>
            <a-textarea
              v-model:value="narrativeTexts[sec.value]"
              :rows="3"
              :maxlength="2000"
              show-count
              :placeholder="sec.placeholder"
            />
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
/**
 * 契约模板管理 —— §13.9
 *
 * 三条必须做对的事：
 *   ① 模板分**平台级**（scope=PLATFORM，全员可选）与**租户级**（scope=TENANT，本租户内可选）；
 *      平台侧另有"合规抽查读"（agreement:platform:template:read）—— 能读不等于能改；
 *   ② 列表行的 manageable / manageableHint 由**服务端算好**，前端只据此决定按钮是否可用，
 *      不自己猜"我能不能改这个模板"；
 *   ③ 模板是**显式选择的起点**，不是自动套用的默认值：界面必须讲清"模板里有 ≠ 已经约定"。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined, ReloadOutlined, SettingOutlined, FileAddOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { usePermission } from '@/composables/usePermission'
import {
  agreementApi,
  AGREEMENT_TYPE_META,
  TEMPLATE_SCOPE_TEXT,
  TEMPLATE_SCOPE_COLOR,
  LEGAL_REVIEW_TEXT,
  LEGAL_REVIEW_COLOR,
  NARRATIVE_SECTION_OPTIONS,
  NARRATIVE_MANUAL_NOTICE_FALLBACK,
  settingEnumOptionLabel,
  type AgreementTemplate,
  type AgreementTemplateDetail,
  type AgreementTemplateQuery,
  type AgreementSettingDef,
  type AgreementTermOptionGroup,
  type AgreementType
} from '@/api/agreement'

const router = useRouter()
const { checkPermission } = usePermission()

// ═══ 查询区 ═══
const searchGridRef = ref<HTMLElement | null>(null)
const searchActionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(searchActionRef, searchGridRef)

const searchParams = reactive({
  scope: undefined as string | undefined,
  agreementType: undefined as AgreementType | undefined,
  status: undefined as number | undefined,
  keyword: ''
})

const scopeOptions = [
  { label: TEMPLATE_SCOPE_TEXT.PLATFORM, value: 'PLATFORM' },
  { label: TEMPLATE_SCOPE_TEXT.TENANT, value: 'TENANT' }
]
const typeOptions = (Object.keys(AGREEMENT_TYPE_META) as AgreementType[]).map(code => ({
  label: AGREEMENT_TYPE_META[code].label,
  value: code
}))
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]
const legalOptions = [
  { label: LEGAL_REVIEW_TEXT.PENDING, value: 'PENDING' },
  { label: LEGAL_REVIEW_TEXT.APPROVED, value: 'APPROVED' },
  { label: LEGAL_REVIEW_TEXT.REJECTED, value: 'REJECTED' }
]

function typeLabel(type?: string): string {
  return AGREEMENT_TYPE_META[type as AgreementType]?.label || type || '—'
}

// ═══ 分页与数据 ═══
const loading = ref(false)
const tableData = ref<AgreementTemplate[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total
}))

const allColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 230, fixed: 'right', slotName: 'actionCell' },
  { title: '模板名称 / 说明', key: 'templateName', field: 'templateName', width: 280, type: 'slot', slotName: 'nameCell' },
  { title: '模板级别', key: 'scope', field: 'scope', width: 140, type: 'slot', slotName: 'scopeCell' },
  { title: '适用协议', key: 'agreementType', field: 'agreementType', width: 150, type: 'slot', slotName: 'typeCell' },
  { title: '预填内容', key: 'content', width: 210, type: 'slot', slotName: 'contentCell' },
  { title: '法务审核', key: 'legalReviewStatus', field: 'legalReviewStatus', width: 170, type: 'slot', slotName: 'legalCell' },
  { title: '状态', key: 'status', field: 'status', width: 90, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '更新时间', key: 'updateTime', field: 'updateTime', width: 170, defaultHidden: true }
]

// ═══ 页面配置 ═══
const PAGE_CONFIG_STORAGE_KEY = 'agreement-template-page-config'
const showPageConfig = ref(false)
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'scope', label: '模板级别', visible: true },
  { key: 'agreementType', label: '适用协议', visible: true },
  { key: 'status', label: '启用状态', visible: true },
  { key: 'keyword', label: '模板名称 / 说明', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'create', label: '新建模板', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true }
]
const queryConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

function isFieldVisible(key: string): boolean {
  const found = queryConfig.value.find(f => f.key === key)
  return found ? found.visible : true
}
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
  } catch { /* 忽略写入失败 */ }
  loadPageConfig()
}

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: AgreementTemplateQuery = { current: pagination.current, size: pagination.pageSize }
    if (searchParams.scope) params.scope = searchParams.scope
    if (searchParams.agreementType) params.agreementType = searchParams.agreementType
    if (searchParams.status !== undefined) params.status = searchParams.status
    if (searchParams.keyword) params.keyword = searchParams.keyword.trim()
    const res: any = await agreementApi.templatePage(params)
    const body = res?.data ?? res ?? {}
    tableData.value = body.records || body.list || []
    pagination.total = Number(body.total) || 0
  } catch (error: any) {
    console.warn('[契约模板] 加载失败', error)
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
  searchParams.scope = undefined
  searchParams.agreementType = undefined
  searchParams.status = undefined
  searchParams.keyword = ''
  handleSearch()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ═══ 元数据（字段字典 / 条款字典）—— 权限不足时降级，不阻断页面 ═══
const settingDefs = ref<AgreementSettingDef[]>([])
const termGroups = ref<AgreementTermOptionGroup[]>([])

function normalizeGroups(raw: any): AgreementTermOptionGroup[] {
  if (!raw) return []
  if (Array.isArray(raw)) {
    return raw.map((g: any) => ({
      termCode: g.termCode,
      termName: g.termName,
      required: g.required,
      options: g.options || g.children || []
    })).filter((g: any) => !!g.termCode)
  }
  if (typeof raw === 'object') {
    return Object.keys(raw).map(code => {
      const opts: any[] = raw[code] || []
      return {
        termCode: code,
        termName: opts[0]?.termName,
        required: opts.some((o: any) => o.required),
        options: opts
      }
    })
  }
  return []
}

async function loadMeta() {
  // ⚠️ 先看权限再发请求：没权限时接口会返回 403，响应拦截器会弹一个"没有操作权限"的错误，
  // 而那其实不是故障、是权限边界 —— 不该在每次进页面时打扰用户
  if (checkPermission('agreement:setting-def:list')) {
    try {
      const res: any = await agreementApi.listSettingDefs()
      const list: any = res?.data ?? res ?? []
      settingDefs.value = (Array.isArray(list) ? list : []).filter((d: any) => d.status === 1 || d.status === undefined)
    } catch {
      settingDefs.value = []
    }
  } else {
    settingDefs.value = []
  }

  if (checkPermission('agreement:term-option:list') || checkPermission('agreement:platform:term-option:manage')) {
    try {
      const res: any = await agreementApi.listTermOptionGroups()
      termGroups.value = normalizeGroups(res?.data ?? res)
    } catch {
      termGroups.value = []
    }
  } else {
    termGroups.value = []
  }
}

/**
 * 新建模板时可选的级别：平台模板只有平台侧能建（码 agreement:platform:template:manage），
 * 因此没有这个码就把「平台模板」这一项去掉，而不是让用户填完再被拒。
 */
const formScopeOptions = computed(() => (
  checkPermission('agreement:platform:template:manage')
    ? scopeOptions
    : scopeOptions.filter(o => o.value === 'TENANT')
))

// ═══ 详情（含平台合规抽查读） ═══
const detailVisible = ref(false)
const detailData = ref<AgreementTemplateDetail | null>(null)

async function openDetail(record: AgreementTemplate) {
  detailVisible.value = true
  detailData.value = null
  try {
    const res: any = await agreementApi.templateDetail(record.id)
    detailData.value = (res?.data ?? res) || null
  } catch (error: any) {
    console.warn('[契约模板] 详情加载失败', error)
    detailVisible.value = false
  }
}

// ═══ 新建 / 修改 ═══
const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const formTab = ref('settings')
const editingId = ref<number | string | null>(null)

const form = reactive({
  scope: 'TENANT',
  templateName: '',
  agreementType: 'DISTRIBUTION' as AgreementType,
  description: '',
  legalReviewStatus: 'PENDING',
  status: 1
})
/** 设定预填：字段编码 → 取值（'' = 不预填） */
const settingValues = reactive<Record<string, string>>({})
/** 条款预填：条款类别 → 选项 */
const termSelection = reactive<Record<string, { optionCode?: string; paramValue?: string }>>({})
/** 文字预填：段落编码 → 正文 */
const narrativeTexts = reactive<Record<string, string>>({})

function resetForm() {
  form.scope = 'TENANT'
  form.templateName = ''
  form.agreementType = 'DISTRIBUTION'
  form.description = ''
  form.legalReviewStatus = 'PENDING'
  form.status = 1
  for (const k of Object.keys(settingValues)) delete settingValues[k]
  for (const k of Object.keys(termSelection)) delete termSelection[k]
  for (const k of Object.keys(narrativeTexts)) delete narrativeTexts[k]
  formTab.value = 'settings'
}

async function openCreate() {
  resetForm()
  formMode.value = 'create'
  editingId.value = null
  formVisible.value = true
  if (!settingDefs.value.length && !termGroups.value.length) await loadMeta()
}

async function openEdit(record: AgreementTemplate) {
  resetForm()
  formMode.value = 'edit'
  editingId.value = record.id
  form.scope = (record.scope as string) || 'TENANT'
  form.templateName = record.templateName || ''
  form.agreementType = (record.agreementType as AgreementType) || 'DISTRIBUTION'
  form.description = record.description || ''
  form.legalReviewStatus = record.legalReviewStatus || 'PENDING'
  form.status = record.status ?? 1
  formVisible.value = true
  if (!settingDefs.value.length && !termGroups.value.length) await loadMeta()
  // 回填已有的预填内容（不传 = 不动、传空数组 = 清空；这里按"原样回填"提交）
  try {
    const res: any = await agreementApi.templateDetail(record.id)
    const detail: AgreementTemplateDetail = (res?.data ?? res) || {}
    for (const s of detail.settings || []) {
      settingValues[s.settingKey] = s.value === null || s.value === undefined ? '' : String(s.value)
    }
    for (const t of detail.terms || []) {
      termSelection[t.termCode] = {
        optionCode: t.optionCode,
        paramValue: t.paramValue === null || t.paramValue === undefined ? '' : String(t.paramValue)
      }
    }
    for (const n of detail.narratives || []) {
      narrativeTexts[n.sectionCode] = n.contentText || ''
    }
  } catch (error: any) {
    console.warn('[契约模板] 预填内容回填失败', error)
  }
}

function paramNameOf(group: AgreementTermOptionGroup): string {
  const code = termSelection[group.termCode]?.optionCode
  if (!code) return ''
  return group.options.find(o => o.optionCode === code)?.needsParam || ''
}
function semanticsOf(group: AgreementTermOptionGroup): string {
  const code = termSelection[group.termCode]?.optionCode
  if (!code) return ''
  return group.options.find(o => o.optionCode === code)?.semantics || ''
}

async function handleSubmitForm() {
  if (!form.templateName.trim()) {
    message.warning('请填写模板名称')
    return
  }
  if (!form.agreementType) {
    message.warning('请选择适用协议类型')
    return
  }
  submitting.value = true
  try {
    const body: any = {
      scope: form.scope,
      templateName: form.templateName.trim(),
      agreementType: form.agreementType,
      description: form.description.trim() || undefined,
      legalReviewStatus: form.legalReviewStatus,
      status: form.status,
      // 三份清单整份覆盖：未出现的即视为"不预填 / 清空"
      settings: settingDefs.value
        .filter(d => {
          const v = settingValues[d.settingKey]
          return v !== undefined && v !== null && String(v).trim() !== ''
        })
        .map(d => ({ settingKey: d.settingKey, value: String(settingValues[d.settingKey]) })),
      terms: Object.keys(termSelection)
        .filter(code => !!termSelection[code]?.optionCode)
        .map(code => ({
          termCode: code,
          optionCode: termSelection[code].optionCode,
          paramValue: termSelection[code].paramValue || undefined
        })),
      narratives: NARRATIVE_SECTION_OPTIONS
        .filter(s => !!narrativeTexts[s.value]?.trim())
        .map(s => ({ sectionCode: s.value, contentText: narrativeTexts[s.value].trim() }))
    }
    if (formMode.value === 'create') {
      await agreementApi.createTemplate(body)
      message.success('模板已建立（平台模板需平台侧账号创建）')
    } else if (editingId.value !== null) {
      await agreementApi.updateTemplate(editingId.value, body)
      message.success('模板已更新')
    }
    formVisible.value = false
    fetchData()
  } catch (error: any) {
    console.warn('[契约模板] 保存失败', error)
  } finally {
    submitting.value = false
  }
}

// ═══ 删除 ═══
function handleDelete(record: AgreementTemplate) {
  Modal.confirm({
    title: '删除契约模板',
    content: `确定删除「${record.templateName}」这个模板吗？已基于它发起的协议不受影响。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await agreementApi.deleteTemplate(record.id)
        message.success('已删除')
        fetchData()
      } catch (error: any) {
        console.warn('[契约模板] 删除失败', error)
      }
    }
  })
}

// ═══ 用模板发起（跳到发起向导） ═══
function applyTemplate(record?: AgreementTemplate | AgreementTemplateDetail | null) {
  if (!record?.id) return
  if (record.status !== 1) {
    message.warning('这个模板已停用，不能用于发起新契约')
    return
  }
  if (record.legalReviewStatus === 'REJECTED') {
    message.warning('这个模板被判定含不合法条款，不能用于发起契约')
    return
  }
  detailVisible.value = false
  router.push(`/agreement/wizard?templateId=${record.id}`)
}
function goWizard() {
  router.push('/agreement/wizard')
}

const handleError = (error: Error) => {
  console.error('[契约模板] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  loadPageConfig()
  loadMeta()
  fetchData()
})
</script>

<style scoped>
/* ═══ 查询区：横向自适应网格 ═══ */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; align-items: center; }
.search-field-item { display: flex; min-width: 0; }
.search-field-item :deep(.ant-input-wrapper),
.search-field-item :deep(.ant-input-affix-wrapper) { width: 100%; font-size: 13px; }
.search-field-item :deep(.ant-select) { width: 100%; }
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
.view-hint { font-size: 12px; color: #999; }

/* 列表 */
.tpl-desc { font-size: 12px; color: #999; line-height: 1.5; }
.tpl-hint { font-size: 12px; color: #d46b08; line-height: 1.5; }
.tpl-count { display: inline-block; font-size: 12px; color: #595959; margin-right: 8px; }

/* 详情 */
.tpl-alert { margin-bottom: 10px; }
.tpl-desc-block { margin-bottom: 10px; }
.tpl-list { display: flex; flex-direction: column; gap: 8px; max-height: 380px; overflow-y: auto; }
.tpl-item { border: 1px solid #f0f0f0; border-radius: 4px; padding: 8px 10px; }
.tpl-item-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 4px; }
.tpl-item-name { font-size: 13px; font-weight: 600; color: #333; }
.tpl-item-value { font-size: 13px; color: #1890ff; }
.tpl-item-text { font-size: 12px; color: #595959; line-height: 1.7; white-space: pre-wrap; }
.tpl-empty { font-size: 13px; color: #999; line-height: 1.7; }
.tpl-detail-actions { margin-top: 12px; }

/* 表单 */
.tpl-form :deep(.ant-form-item) { margin-bottom: 12px; }
.tpl-setting-list { display: flex; flex-direction: column; gap: 8px; max-height: 380px; overflow-y: auto; }
.tpl-setting { border-bottom: 1px dashed #f0f0f0; padding-bottom: 8px; }
.tpl-setting-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 4px; }
.tpl-inline { flex: 1 1 100%; }
.tpl-setting-control { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.tpl-control { width: 260px; }
.tpl-param { width: 180px; }
.tpl-term { border-bottom: 1px dashed #f0f0f0; padding-bottom: 8px; }
.tpl-narrative { margin-bottom: 10px; }
</style>
