<template>
  <div class="bill-form-page">
    <!-- ═══ Zone 1: 头部操作栏 ═══ -->
    <div class="bill-header">
      <div class="header-left">
        <span class="order-no">NO. {{ header?.orderNo || '待生成' }}</span>
        <a-button
          v-if="header?.showAttachment"
          type="link" size="small" class="attachment-btn"
        >
          <template #icon><PaperClipOutlined /></template>
          附件
        </a-button>
      </div>
      <div class="header-center">
        <h2 class="bill-title">{{ header?.title || '' }}</h2>
      </div>
      <div class="header-right">
        <template v-for="action in header?.actions" :key="action.key">
          <!-- 有子项 → 下拉菜单 -->
          <a-dropdown v-if="action.children?.length">
            <a-button size="small">
              <template v-if="action.icon" #icon><component :is="action.icon" /></template>
              {{ action.label }}
              <DownOutlined />
            </a-button>
            <template #overlay>
              <a-menu @click="(info: any) => emit('action', info.key, action.key)">
                <a-menu-item v-for="child in action.children" :key="child.key">
                  {{ child.label }}
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
          <!-- 无子项 → 普通按钮 -->
          <a-button v-else size="small" @click="emit('action', action.key)">
            <template v-if="action.icon" #icon><component :is="action.icon" /></template>
            {{ action.label }}
          </a-button>
        </template>
      </div>
    </div>

    <!-- ═══ Zone 2: 基本信息选择器 ═══ -->
    <div class="bill-basic-info">
      <div class="info-row">
        <div
          v-for="field in basicInfoFields"
          :key="field.key"
          class="info-field"
          :class="{
            'info-field--required': field.required,
            'info-field--narrow': field.width === 'narrow',
            'info-field--wide': field.width === 'wide',
          }"
        >
          <label>{{ field.label }}</label>
          <div class="field-input-wrap">
            <!-- select -->
            <a-select
              v-if="field.type === 'select'"
              :value="(modelValue as any)[field.key]"
              :placeholder="field.placeholder"
              show-search
              :filter-option="filterOption"
              :loading="field.loading"
              size="small"
              style="flex:1"
              @update:value="(val: any) => emit('update:modelValue', { ...modelValue, [field.key]: val })"
              @change="(val: any) => emit('fieldChange', field.key, val)"
            >
              <a-select-option v-for="opt in field.options" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </a-select-option>
            </a-select>
            <!-- date -->
            <a-date-picker
              v-else-if="field.type === 'date'"
              :value="(modelValue as any)[field.key]"
              style="width:100%"
              :format="field.format || 'YYYY-MM-DD'"
              value-format="YYYY-MM-DD"
              size="small"
              @update:value="(val: any) => emit('update:modelValue', { ...modelValue, [field.key]: val })"
            />
            <!-- number -->
            <a-input-number
              v-else-if="field.type === 'number'"
              :value="(modelValue as any)[field.key]"
              :placeholder="field.placeholder"
              size="small"
              style="flex:1"
              @update:value="(val: any) => emit('update:modelValue', { ...modelValue, [field.key]: val })"
            />
            <!-- input (default) -->
            <a-input
              v-else
              :value="(modelValue as any)[field.key]"
              :placeholder="field.placeholder"
              size="small"
              style="flex:1"
              @update:value="(val: any) => emit('update:modelValue', { ...modelValue, [field.key]: val })"
            />
            <!-- 搜索按钮 -->
            <a-button
              v-if="field.searchBtn"
              type="link" size="small" class="field-search-btn"
            >
              {{ field.searchBtn }}
            </a-button>
          </div>
        </div>
      </div>
    </div>

    <!-- ═══ Zone 3: 明细表格（插槽） ═══ -->
    <div class="bill-table-section">
      <slot name="detail-table" />
    </div>

    <!-- ═══ Zone 4: 底部面板（左标签页 + 右摘要） ═══ -->
    <div class="bill-bottom-panel">
      <div class="bottom-panel-inner">
        <!-- 左侧：标签页 + 备注插槽 -->
        <div class="bottom-left">
          <a-tabs v-if="tabs?.length" v-model:activeKey="activeTab" size="small" class="bottom-tabs">
            <a-tab-pane v-for="tab in tabs" :key="tab.key" :tab="tab.tab">
              <div class="tab-content-row">
                <div v-for="tf in tab.fields" :key="tf.key" class="tab-field">
                  <label>{{ tf.label }}</label>
                  <div class="tab-field-input">
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
                      <a-select-option v-for="opt in tf.options" :key="opt.value" :value="opt.value">
                        {{ opt.label }}
                      </a-select-option>
                    </a-select>
                    <a-input-number
                      v-else-if="tf.type === 'number'"
                      :value="(modelValue as any)[tf.key]"
                      :placeholder="tf.placeholder"
                      :disabled="tf.disabled"
                      size="small"
                      style="flex:1"
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
                      type="link" size="small"
                      :class="{ 'btn-clear': tf.suffixBtnDanger }"
                    >
                      {{ tf.suffixBtn }}
                    </a-button>
                  </div>
                </div>
              </div>
            </a-tab-pane>
          </a-tabs>
          <!-- 备注/额外内容插槽 -->
          <slot name="bottom-extra" />
        </div>

        <!-- 右侧：红色摘要面板 -->
        <div v-if="summary?.length" class="bill-summary-sidebar">
          <template v-for="(row, idx) in summary" :key="idx">
            <div v-if="row.divider" class="sidebar-divider" />
            <div class="sidebar-row">
              <span class="sidebar-label">{{ row.label }}</span>
              <span class="sidebar-value">{{ row.value }}</span>
              <a-button v-if="row.showMore" type="link" size="small" class="sidebar-more">···</a-button>
            </div>
          </template>
        </div>
      </div>
    </div>

    <!-- ═══ Zone 5: 页脚操作栏 ═══ -->
    <div class="bill-footer">
      <div class="footer-left">
        <span class="footer-amount-label">{{ footer?.amountLabel || '本单金额' }}</span>
        <span class="footer-amount-value" :class="{ 'amount-red': footer?.amountHighlight !== false }">
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
          <span v-if="footer?.draftShortcut" class="shortcut-hint">{{ footer.draftShortcut }}</span>
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
          <span v-if="footer?.primaryShortcut" class="shortcut-hint">{{ footer.primaryShortcut }}</span>
        </a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import {
  PaperClipOutlined,
  DownOutlined,
} from '@ant-design/icons-vue'
import type {
  BillHeaderConfig,
  BasicInfoField,
  BillTabConfig,
  SummaryRow,
  BillFooterConfig,
} from './types'

defineOptions({ name: 'BillFormPage' })

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
}>(), {
  header: undefined,
  basicInfoFields: () => [],
  tabs: () => [],
  summary: () => [],
  footer: undefined,
})

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, any>]
  'fieldChange': [fieldKey: string, value: any]
  'action': [actionKey: string, parentKey?: string]
  'draft': []
  'submit': []
}>()

const activeTab = ref(props.tabs?.[0]?.key || '')

/** select 搜索过滤 */
const filterOption = (input: string, option: any) => {
  const text = option?.label || option?.children?.[0]?.children || ''
  return text.toString().toLowerCase().includes(input.toLowerCase())
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
  padding: 8px 16px;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.info-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.info-field {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
  min-width: 120px;
}

.info-field--narrow {
  flex: 0 0 160px;
}

.info-field--wide {
  flex: 2;
  min-width: 200px;
}

.info-field label {
  font-size: 12px;
  color: #595959;
  white-space: nowrap;
}

.info-field--required label::after {
  content: '*';
  color: #ff4d4f;
  margin-left: 2px;
}

.field-input-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
}

.field-search-btn {
  padding: 0 4px;
  font-size: 11px;
  color: #1890ff;
  flex-shrink: 0;
  line-height: 1;
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
  margin-bottom: 8px;
}

.tab-content-row {
  display: flex;
  gap: 12px;
  padding: 4px 0;
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
  width: 180px;
  background: #fff5f5;
  border-left: 1px solid #ffccc7;
  padding: 12px 10px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
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
}

.btn-submit:hover {
  background: #ff7875;
  border-color: #ff7875;
}

.btn-audit {
  background: #52c41a;
  border-color: #52c41a;
}

.btn-audit:hover {
  background: #73d13d;
  border-color: #73d13d;
}

.shortcut-hint {
  margin-left: 4px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.65);
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

:deep(.ant-tabs-small .ant-tabs-tab) {
  padding: 4px 12px;
  font-size: 13px;
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
