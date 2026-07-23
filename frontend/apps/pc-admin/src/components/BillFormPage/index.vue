<template>
  <div class="bill-form-page">
    <!-- ═══ Zone 1: 头部操作栏 ═══ -->
    <div class="bill-header">
      <div class="header-left">
        <span class="order-no">NO. {{ header?.orderNo || '待生成' }}</span>
        <a-button
          v-if="header?.showAttachment"
          type="link"
          size="small"
          class="attachment-btn"
        >
          <template #icon>
            <PaperClipOutlined />
          </template>
          附件
        </a-button>
      </div>
      <div class="header-center">
        <h2 class="bill-title">
          {{ header?.title || '' }}
        </h2>
      </div>
      <div class="header-right">
        <template
          v-for="action in header?.actions"
          :key="action.key"
        >
          <a-dropdown v-if="action.children?.length">
            <a-button size="small">
              <template
                v-if="action.icon"
                #icon
              >
                <component :is="action.icon" />
              </template>
              {{ action.label }}
              <DownOutlined />
            </a-button>
            <template #overlay>
              <a-menu @click="(info: any) => emit('action', info.key, action.key)">
                <a-menu-item
                  v-for="child in action.children"
                  :key="child.key"
                >
                  {{ child.label }}
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
          <a-button
            v-else
            size="small"
            @click="emit('action', action.key)"
          >
            <template
              v-if="action.icon"
              #icon
            >
              <component :is="action.icon" />
            </template>
            {{ action.label }}
          </a-button>
        </template>
      </div>
    </div>

    <!-- ═══ Zone 2: 基本信息选择器 ═══ -->
    <div class="bill-basic-info">
      <!-- 流式布局：inline模式，自动换行 -->
      <div class="info-flow">
        <template
          v-for="field in basicInfoFields"
          :key="field.key"
        >
          <!-- inlineLabel 模式：使用 InlineField -->
          <InlineField
            v-if="field.inlineLabel"
            :type="field.type"
            :label="field.label"
            :model-value="(modelValue as any)[field.key]"
            :options="field.options"
            :format="field.format"
            :precision="field.precision"
            :min="field.min"
            :max="field.max"
            :disabled="field.disabled"
            :loading="field.loading"
            :search-btn="field.searchBtn"
            :width="field.width"
            :view-mode="isViewMode"
            @update:model-value="(val: any) => emit('update:modelValue', { ...modelValue, [field.key]: val })"
            @change="(val: any) => emit('fieldChange', field.key, val)"
            @search-btn="emit('searchBtn', field.key, field.searchBtn)"
          />
          <!-- 默认模式：使用 LabelField -->
          <LabelField
            v-else
            :type="field.type"
            :label="field.label"
            :required="field.required"
            :model-value="(modelValue as any)[field.key]"
            :placeholder="field.placeholder"
            :options="field.options"
            :format="field.format"
            :precision="field.precision"
            :min="field.min"
            :max="field.max"
            :disabled="field.disabled"
            :loading="field.loading"
            :search-btn="field.searchBtn"
            :width="field.width"
            :view-mode="isViewMode"
            @update:model-value="(val: any) => emit('update:modelValue', { ...modelValue, [field.key]: val })"
            @change="(val: any) => emit('fieldChange', field.key, val)"
            @search-btn="emit('searchBtn', field.key, field.searchBtn)"
          />
        </template>
      </div>
    </div>

    <!-- ═══ Zone 3: 明细表格（插槽，有内容时显示） ═══ -->
    <div
      v-if="$slots['detail-table']"
      class="bill-table-section"
      :class="{ 'table-section-expanded': tableExpanded }"
    >
      <slot
        name="detail-table"
        :on-expand-change="handleTableExpand"
      />
    </div>

    <!-- ═══ Zone 4: 底部面板（左标签页 + 右摘要） ═══ -->
    <div
      v-if="showBottomPanel && !tableExpanded && (tabs?.length || summary?.length || $slots['bottom-extra'])"
      class="bill-bottom-panel"
    >
      <div class="bottom-panel-inner">
        <div class="bottom-left">
          <a-tabs
            v-if="tabs?.length"
            v-model:active-key="activeTab"
            size="small"
            class="bottom-tabs"
            @change="(key: string) => emit('tabChange', key)"
          >
            <a-tab-pane
              v-for="tab in tabs"
              :key="tab.key"
              :tab="tab.tab"
            >
              <div class="tab-content-row">
                <div
                  v-for="tf in tab.fields"
                  :key="tf.key"
                  class="tab-field"
                >
                  <label>{{ tf.label }}</label>
                  <div class="tab-field-input">
                    <!-- 查看模式 -->
                    <template v-if="isViewMode">
                      <span class="field-view-text">{{ getTabFieldDisplayValue(tf) }}</span>
                    </template>
                    <!-- 编辑模式 -->
                    <template v-else>
                      <a-select
                        v-if="tf.type === 'select'"
                        :value="(modelValue as any)[tf.key]"
                        :placeholder="tf.placeholder"
                        :disabled="tf.disabled"
                        show-search
                        size="small"
                        style="flex:1"
                        @update:value="(val: any) => emit('update:modelValue', { ...modelValue, [tf.key]: val })"
                      >
                        <a-select-option
                          v-for="opt in tf.options"
                          :key="opt.value"
                          :value="opt.value"
                        >
                          {{ opt.label }}
                        </a-select-option>
                      </a-select>
                      <a-input-number
                        v-else-if="tf.type === 'number'"
                        :value="(modelValue as any)[tf.key]"
                        :placeholder="tf.placeholder"
                        :disabled="tf.disabled"
                        :precision="tf.precision"
                        :min="tf.min"
                        :max="tf.max"
                        size="small"
                        style="flex:1"
                        @update:value="(val: any) => emit('update:modelValue', { ...modelValue, [tf.key]: val })"
                      />
                      <a-date-picker
                        v-else-if="tf.type === 'date'"
                        :value="(modelValue as any)[tf.key]"
                        :placeholder="tf.placeholder || '请选择日期'"
                        :disabled="tf.disabled"
                        size="small"
                        style="flex:1"
                        value-format="YYYY-MM-DD"
                        @update:value="(val: any) => emit('update:modelValue', { ...modelValue, [tf.key]: val })"
                      />
                      <a-input
                        v-else
                        :value="(modelValue as any)[tf.key]"
                        :placeholder="tf.placeholder"
                        :disabled="tf.disabled"
                        size="small"
                        style="flex:1"
                        @update:value="(val: any) => emit('update:modelValue', { ...modelValue, [tf.key]: val })"
                      />
                      <a-button
                        v-if="tf.suffixBtn"
                        type="link"
                        size="small"
                        :class="{ 'btn-clear': tf.suffixBtnDanger }"
                      >
                        {{ tf.suffixBtn }}
                      </a-button>
                    </template>
                  </div>
                </div>
              </div>
            </a-tab-pane>
          </a-tabs>
          <slot name="bottom-extra" />
        </div>

        <div
          v-if="summary?.length"
          class="bill-summary-sidebar"
        >
          <!-- 状态角标（仅首行有 statusLabel 时显示） -->
          <div
            v-if="summary[0]?.statusLabel"
            class="sidebar-status-badge"
          >
            {{ summary[0].statusLabel }}
          </div>
          <template
            v-for="(row, idx) in summary"
            :key="idx"
          >
            <div
              v-if="row.divider"
              class="sidebar-divider"
            />
            <div class="sidebar-row">
              <span class="sidebar-label">{{ row.label }}</span>
              <span class="sidebar-value">{{ row.value }}</span>
              <a-button
                v-if="row.showMore"
                type="link"
                size="small"
                class="sidebar-more"
              >
                ···
              </a-button>
            </div>
          </template>
        </div>
      </div>
    </div>

    <!-- ═══ Zone 5: 页脚操作栏（查看模式/表格展开时隐藏） ═══ -->
    <div
      v-if="!isViewMode && !tableExpanded"
      class="bill-footer"
    >
      <slot name="footer">
        <div class="footer-left">
          <span class="footer-amount-label">{{ footer?.amountLabel || '本单金额' }}</span>
          <span
            class="footer-amount-value"
            :class="{ 'amount-red': footer?.amountHighlight !== false }"
          >
            {{ footer?.amountValue || '¥0.00' }}
          </span>
        </div>
        <div class="footer-right">
          <a-button
            v-if="footer?.draftBtnText"
            size="large"
            :loading="footer?.saving"
            @click="emit('draft')"
          >
            {{ footer.draftBtnText }}
            <br v-if="footer?.draftShortcut">
            <span
              v-if="footer?.draftShortcut"
              class="shortcut-hint"
            >{{ footer.draftShortcut }}</span>
          </a-button>
          <a-button
            v-if="footer?.primaryBtnText"
            type="primary"
            size="large"
            :class="footer?.primaryBtnAudit ? 'btn-audit' : 'btn-submit'"
            :loading="footer?.saving"
            @click="emit('submit')"
          >
            {{ footer.primaryBtnText }}
            <br v-if="footer?.primaryShortcut">
            <span
              v-if="footer?.primaryShortcut"
              class="shortcut-hint"
            >{{ footer.primaryShortcut }}</span>
          </a-button>
        </div>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  PaperClipOutlined,
  DownOutlined,
} from '@ant-design/icons-vue'
import InlineField from '@/components/FormField/InlineField.vue'
import LabelField from '@/components/FormField/LabelField.vue'
import type {
  BillHeaderConfig,
  BasicInfoField,
  BillTabConfig,
  TabField,
  SummaryRow,
  BillFooterConfig,
} from './types'

defineOptions({ name: 'BillFormPage' })

const route = useRoute()

const props = withDefaults(defineProps<{
  /** v-model 表单数据 */
  modelValue: Record<string, any>
  /** Zone 1: 头部配置 */
  header?: BillHeaderConfig
  /** Zone 2: 基本信息字段 */
  basicInfoFields?: BasicInfoField[]
  /** Zone 4: 标签页配置 */
  tabs?: BillTabConfig[]
  /** Zone 4: 摘要面板数据 */
  summary?: SummaryRow[]
  /** Zone 5: 页脚配置 */
  footer?: BillFooterConfig
  /** 页面模式：不指定时从路由参数自动推断 */
  mode?: 'create' | 'edit' | 'view'
  /** 是否显示底部面板（Zone 4），默认 true */
  showBottomPanel?: boolean
}>(), {
  header: undefined,
  basicInfoFields: () => [],
  tabs: () => [],
  summary: () => [],
  footer: undefined,
  mode: undefined,
  showBottomPanel: true,
})

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, any>]
  'fieldChange': [fieldKey: string, value: any]
  'action': [actionKey: string, parentKey?: string]
  'searchBtn': [fieldKey: string, btnText: string]
  'draft': []
  'submit': []
  'tabChange': [tabKey: string]
}>()

const activeTab = ref(props.tabs?.[0]?.key || '')

/** 表格展开状态（控制底部面板和页脚的显示/隐藏） */
const tableExpanded = ref(false)

/** 处理表格展开/收起事件 */
function handleTableExpand(expanded: boolean) {
  tableExpanded.value = expanded
}

/** 有效模式：显式指定 > 路由推断 */
const effectiveMode = computed(() => {
  if (props.mode) return props.mode
  const editId = route.params.id || route.query.id
  return editId ? 'edit' : 'create'
})

/** 是否为查看模式 */
const isViewMode = computed(() => effectiveMode.value === 'view')

/** select 搜索过滤 */
const filterOption = (input: string, option: any) => {
  const text = option?.label || option?.children?.[0]?.children || ''
  return text.toString().toLowerCase().includes(input.toLowerCase())
}

/** 获取基本信息字段的显示值（查看模式） */
function getFieldDisplayValue(field: BasicInfoField): string {
  const raw = (props.modelValue as any)?.[field.key]
  if (raw === undefined || raw === null || raw === '') return '—'
  if (field.type === 'select' && field.options) {
    const opt = field.options.find(o => o.value === raw)
    return opt?.label || String(raw)
  }
  return String(raw)
}

/** 获取标签页字段的显示值（查看模式） */
function getTabFieldDisplayValue(tf: TabField): string {
  const raw = (props.modelValue as any)?.[tf.key]
  if (raw === undefined || raw === null || raw === '') return '—'
  if (tf.type === 'select' && tf.options) {
    const opt = tf.options.find(o => o.value === raw)
    return opt?.label || String(raw)
  }
  return String(raw)
}
</script>

<style scoped>
/* ═══ 整体布局 ═══ */
.bill-form-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f0f2f5;
  overflow: hidden;
  font-size: 13px;
}

/* ═══ Zone 1: 头部 ═══ */
.bill-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.order-no {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  font-family: 'Consolas', 'Monaco', monospace;
}

.attachment-btn {
  color: #8c8c8c;
  font-size: 12px;
}

.header-center {
  flex: 1;
  text-align: center;
}

.bill-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #262626;
}

.header-right {
  display: flex;
  gap: 6px;
  align-items: center;
}

/* ═══ Zone 2: 基本信息 ═══ */
.bill-basic-info {
  background: #fff;
  padding: 6px 12px;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.info-flow {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.field-view-text {
  font-size: 13px;
  color: #262626;
  line-height: 26px;
  padding: 0 4px;
}

/* ═══ Zone 3: 明细表格 ═══ */
.bill-table-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  overflow: hidden;
}

.table-section-expanded {
  border-bottom: none;
}

/* ═══ Zone 4: 底部面板 ═══ */
.bill-bottom-panel {
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.bottom-panel-inner {
  display: flex;
  gap: 0;
}

.bottom-left {
  flex: 1;
  min-width: 0;
  padding: 0 12px;
}

.bottom-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 0;
}

.bottom-tabs :deep(.ant-tabs-nav::before) {
  border-bottom: none;
}

.bottom-tabs :deep(.ant-tabs-ink-bar) {
  height: 2px;
}

.tab-content-row {
  display: flex;
  gap: 12px;
  padding: 2px 0;
  flex-wrap: wrap;
}

.tab-field {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 140px;
}

.tab-field label {
  font-size: 12px;
  color: #595959;
}

.tab-field-input {
  display: flex;
  align-items: center;
  gap: 2px;
}

.btn-clear {
  font-size: 11px;
  color: #ff4d4f;
  flex-shrink: 0;
}

/* ═══ 右侧摘要面板 ═══ */
.bill-summary-sidebar {
  position: relative;
  width: 220px;
  background: #fff0f0;
  border-left: 1px solid #ffccc7;
  padding: 28px 12px 12px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sidebar-status-badge {
  position: absolute;
  top: -1px;
  right: -1px;
  background: #ff4d4f;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 0 4px 0 8px;
  line-height: 18px;
  z-index: 1;
}

.sidebar-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.sidebar-divider {
  border-top: 1px dashed #ffccc7;
  padding-top: 0;
  margin-top: 0;
}

.sidebar-label {
  color: #595959;
}

.sidebar-value {
  font-weight: 600;
  color: #262626;
}

.sidebar-more {
  padding: 0 2px;
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1;
}

/* ═══ Zone 5: 页脚 ═══ */
.bill-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #fff;
  flex-shrink: 0;
}

.footer-left {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.footer-amount-label {
  font-size: 13px;
  color: #595959;
}

.footer-amount-value {
  font-size: 22px;
  font-weight: 700;
}

.amount-red {
  color: #ff4d4f;
}

.footer-right {
  display: flex;
  gap: 10px;
}

.btn-submit {
  background: #ff4d4f;
  border-color: #ff4d4f;
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  line-height: 1.2;
}

.btn-submit:hover {
  background: #ff7875;
  border-color: #ff7875;
}

.btn-audit {
  background: #52c41a;
  border-color: #52c41a;
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  line-height: 1.2;
}

.btn-audit:hover {
  background: #73d13d;
  border-color: #73d13d;
}

.shortcut-hint {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.65);
  display: block;
  margin-left: 0;
}

/* ═══ 紧凑输入框覆写 ═══ */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small) {
  height: 26px;
  line-height: 24px;
}

:deep(.ant-input-number-sm input) {
  height: 24px;
}

:deep(.ant-tabs-small > .ant-tabs-nav .ant-tabs-tab) {
  padding: 0 8px;
  font-size: 12px;
  line-height: 20px;
}

:deep(.ant-tabs-small > .ant-tabs-nav .ant-tabs-tab-btn) {
  line-height: 20px;
}

:deep(.ant-tabs-small > .ant-tabs-nav) {
  min-height: 22px;
}

:deep(.ant-tabs-small > .ant-tabs-nav .ant-tabs-nav-wrap) {
  padding: 0;
}

:deep(.ant-tabs-small > .ant-tabs-nav .ant-tabs-tab + .ant-tabs-tab) {
  margin: 0;
}

:deep(.ant-btn-sm) {
  height: 26px;
  line-height: 24px;
}

:deep(.ant-tag) {
  font-size: 11px;
  line-height: 18px;
}
</style>
