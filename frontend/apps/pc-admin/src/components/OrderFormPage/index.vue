<template>
  <div class="order-form-page">
    <!-- 顶部操作栏 -->
    <div class="form-header">
      <div class="header-left">
        <span class="bill-no">NO. {{ formData.orderNo || '待生成' }}</span>
        <a-button type="link" size="small">
          <template #icon><PaperClipOutlined /></template>
          附件
        </a-button>
      </div>
      <div class="header-center">
        <h2 class="form-title">{{ title }}</h2>
      </div>
      <div class="header-right">
        <a-button size="small" :loading="saving" @click="handleSaveDraft">
          <template #icon><SaveOutlined /></template>
          保存
        </a-button>
        <a-button size="small" type="primary" :loading="saving" @click="handleSubmit">
          <template #icon><SendOutlined /></template>
          提交
        </a-button>
      </div>
    </div>

    <!-- 基本信息区 -->
    <div class="form-section">
      <a-row :gutter="16">
        <template v-for="field in headerFields" :key="field.name">
          <a-col :span="field.span || 6">
            <a-form-item :label="field.label" :required="field.required">
              <!-- 自定义插槽 -->
              <template v-if="field.type === 'slot' && field.slotName">
                <slot :name="field.slotName" :formData="formData" />
              </template>
              <!-- 输入框 -->
              <a-input
                v-else-if="field.type === 'input'"
                v-model:value="formData[field.name]"
                :placeholder="field.placeholder || `请输入${field.label}`"
                size="small"
              />
              <!-- 下拉选择 -->
              <a-select
                v-else-if="field.type === 'select'"
                v-model:value="formData[field.name]"
                :placeholder="field.placeholder || `请选择${field.label}`"
                :show-search="field.showSearch !== false"
                :filter-option="filterOption"
                :loading="loadingOptions"
                style="width: 100%"
                size="small"
              >
                <a-select-option
                  v-for="opt in getOptions(field)"
                  :key="opt.id"
                  :value="opt.id"
                >
                  {{ opt.name }}
                </a-select-option>
              </a-select>
              <!-- 日期选择 -->
              <a-date-picker
                v-else-if="field.type === 'date'"
                v-model:value="formData[field.name]"
                style="width: 100%"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                size="small"
              />
              <!-- 数字输入 -->
              <a-input-number
                v-else-if="field.type === 'number'"
                v-model:value="formData[field.name]"
                :min="field.min ?? 0"
                :max="field.max"
                :precision="field.precision ?? 2"
                style="width: 100%"
                size="small"
              />
              <!-- 文本域 -->
              <a-textarea
                v-else-if="field.type === 'textarea'"
                v-model:value="formData[field.name]"
                :placeholder="field.placeholder || `请输入${field.label}`"
                :rows="2"
                size="small"
              />
            </a-form-item>
          </a-col>
        </template>
      </a-row>
    </div>

    <!-- 商品明细表格 -->
    <div class="form-section product-section">
      <div class="section-header">
        <span class="section-title">商品明细</span>
        <a-button type="primary" size="small" @click="handleAddProduct">
          <template #icon><PlusOutlined /></template>
          添加商品
        </a-button>
      </div>

      <VxeTableList
        :columns="detailColumns"
        :data-source="formData.products"
        :pagination="false as any"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <!-- 商品选择插槽 -->
        <template #productCell="{ record, index }">
          <a-select
            v-model:value="record.productId"
            placeholder="请选择商品"
            show-search
            :filter-option="filterOption"
            style="width: 100%"
            :loading="loadingOptions"
            size="small"
            @change="(val: number) => handleProductChange(val, index)"
          >
            <a-select-option v-for="p in optionRefs.products" :key="p.id" :value="p.id">
              {{ p.name }}
            </a-select-option>
          </a-select>
        </template>
        <!-- 数量插槽 -->
        <template #quantityCell="{ record }">
          <a-input-number v-model:value="record.quantity" :min="0" :precision="2" style="width: 100%" size="small" />
        </template>
        <!-- 单价插槽 -->
        <template #unitPriceCell="{ record }">
          <a-input-number v-model:value="record.unitPrice" :min="0" :precision="2" style="width: 100%" size="small" />
        </template>
        <!-- 金额插槽 -->
        <template #amountCell="{ record }">
          <span class="amount-text">{{ ((record.quantity || 0) * (record.unitPrice || 0)).toFixed(2) }}</span>
        </template>
        <!-- 税率插槽 -->
        <template #taxRateCell="{ record }">
          <a-input-number v-model:value="record.taxRate" :min="0" :max="100" :precision="1" style="width: 100%" size="small" />
        </template>
        <!-- 税额插槽 -->
        <template #taxAmountCell="{ record }">
          <span>{{ ((record.quantity || 0) * (record.unitPrice || 0) * (record.taxRate || 0) / 100).toFixed(2) }}</span>
        </template>
        <!-- 价税合计插槽 -->
        <template #totalAmountCell="{ record }">
          <span class="amount-text">{{ ((record.quantity || 0) * (record.unitPrice || 0) * (1 + (record.taxRate || 0) / 100)).toFixed(2) }}</span>
        </template>
        <!-- 操作插槽 -->
        <template #action="{ index }">
          <a-button type="link" danger size="small" @click="handleRemoveProduct(index)">
            <template #icon><DeleteOutlined /></template>
          </a-button>
        </template>

        <!-- 外部自定义插槽透传 -->
        <slot v-for="(_, name) in $slots" :name="name" />
      </VxeTableList>

      <!-- 合计行 -->
      <div v-if="summaryItems && summaryItems.length > 0" class="product-summary">
        <a-row :gutter="24">
          <a-col v-for="item in summaryItems" :key="item.label" :span="6">
            <div class="summary-item">
              <span class="summary-label">{{ item.label }}：</span>
              <span :class="['summary-value', { highlight: item.highlight }]">
                {{ formatSummary(item) }}
              </span>
            </div>
          </a-col>
        </a-row>
      </div>
    </div>

    <!-- 底部标签页 -->
    <div v-if="tabs && tabs.length > 0" class="form-section bottom-section">
      <a-tabs v-model:activeKey="activeTab" size="small">
        <a-tab-pane v-for="tab in tabs" :key="tab.key" :tab="tab.tab">
          <a-row :gutter="16">
            <a-col v-for="field in tab.fields" :key="field.name" :span="field.span || 8">
              <a-form-item :label="field.label">
                <a-input
                  v-if="field.type === 'input'"
                  v-model:value="formData[field.name]"
                  :placeholder="field.placeholder || `请输入${field.label}`"
                  size="small"
                />
                <a-select
                  v-else-if="field.type === 'select'"
                  v-model:value="formData[field.name]"
                  :placeholder="field.placeholder || `请选择${field.label}`"
                  style="width: 100%"
                  size="small"
                >
                  <a-select-option v-for="opt in (field.options || [])" :key="opt.value" :value="opt.value">
                    {{ opt.label }}
                  </a-select-option>
                </a-select>
                <a-input-number
                  v-else-if="field.type === 'number'"
                  v-model:value="formData[field.name]"
                  :min="field.min ?? 0"
                  :precision="field.precision ?? 2"
                  style="width: 100%"
                  size="small"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-tab-pane>
      </a-tabs>
    </div>

    <!-- 底部操作栏 -->
    <div class="form-footer">
      <div class="footer-left">
        <span class="footer-amount">
          本单金额：<span class="amount-highlight">{{ totalWithTaxFormatted }}</span>
        </span>
      </div>
      <div class="footer-right">
        <a-button size="large" :loading="saving" @click="handleSaveDraft">
          <template #icon><SaveOutlined /></template>
          保存草稿
          <span class="shortcut-hint">Ctrl+S</span>
        </a-button>
        <a-button type="primary" size="large" :loading="saving" @click="handleSubmit">
          <template #icon><SendOutlined /></template>
          提交
          <span class="shortcut-hint">Ctrl+Enter</span>
        </a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  PaperClipOutlined,
  SaveOutlined,
  SendOutlined,
  PlusOutlined,
  DeleteOutlined,
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { useFormLogic } from './useFormLogic'
import type { HeaderField, DetailColumn, TabConfig, SummaryItem, FormApi } from './types'

const route = useRoute()

const props = withDefaults(defineProps<{
  title: string
  billPrefix?: string
  headerFields: HeaderField[]
  detailColumns: DetailColumn[]
  tabs?: TabConfig[]
  summaryItems?: SummaryItem[]
  api: FormApi
  redirectPath?: string
  /** 显式指定模式；不指定时根据路由参数自动推断 */
  mode?: 'create' | 'edit' | 'view'
  showTax?: boolean
}>(), {
  billPrefix: 'NO',
  mode: undefined,
  showTax: true,
  redirectPath: '',
})

// 根据路由参数自动推断模式：存在 id 参数则进入编辑模式
const effectiveMode = computed(() => {
  if (props.mode) return props.mode
  const editId = route.params.id || route.query.id
  return editId ? 'edit' : 'create'
})

const {
  formData,
  activeTab,
  loadingOptions,
  saving,
  optionRefs,
  filterOption,
  handleAddProduct,
  handleRemoveProduct,
  handleProductChange,
  handleSaveDraft,
  handleSubmit,
} = useFormLogic({
  billPrefix: props.billPrefix,
  api: props.api,
  redirectPath: props.redirectPath,
  mode: effectiveMode.value,
  headerFields: props.headerFields,
})

// 根据字段配置获取选项列表
function getOptions(field: HeaderField) {
  if (field.options) return field.options
  if (field.optionsRef) {
    return optionRefs[field.optionsRef] || []
  }
  // 根据字段名推断
  const nameMap: Record<string, string> = {
    customerId: 'customers',
    supplierId: 'suppliers',
    warehouseId: 'warehouses',
    handlerId: 'users',
    salespersonId: 'users',
    buyerId: 'users',
  }
  const refKey = nameMap[field.name]
  return refKey ? optionRefs[refKey] || [] : []
}

// 合计计算
const totalQuantity = computed(() =>
  formData.products.reduce((sum: number, p: any) => sum + (p.quantity || 0), 0)
)
const totalAmount = computed(() =>
  formData.products.reduce((sum: number, p: any) => sum + (p.quantity || 0) * (p.unitPrice || 0), 0)
)
const totalTaxAmount = computed(() =>
  formData.products.reduce((sum: number, p: any) => sum + (p.quantity || 0) * (p.unitPrice || 0) * (p.taxRate || 0) / 100, 0)
)
const totalWithTax = computed(() =>
  formData.products.reduce((sum: number, p: any) => sum + (p.quantity || 0) * (p.unitPrice || 0) * (1 + (p.taxRate || 0) / 100), 0)
)

const totalWithTaxFormatted = computed(() => `¥${totalWithTax.value.toFixed(2)}`)

// 格式化合计项
function formatSummary(item: SummaryItem): string {
  if (item.compute) {
    const val = item.compute(formData.products)
    return item.format ? item.format(val) : val.toFixed(2)
  }
  if (item.valueKey) {
    const valMap: Record<string, any> = {
      totalQuantity: totalQuantity.value,
      totalAmount: totalAmount.value,
      totalTaxAmount: totalTaxAmount.value,
      totalWithTax: totalWithTax.value,
    }
    const val = valMap[item.valueKey] || 0
    return item.format ? item.format(val) : `¥${val.toFixed(2)}`
  }
  return '0.00'
}
</script>

<style scoped>
.order-form-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f5f5;
  overflow: hidden;
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.bill-no {
  font-size: 16px;
  font-weight: 600;
  color: #1890ff;
}

.header-center {
  flex: 1;
  text-align: center;
}

.form-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #262626;
}

.header-right {
  display: flex;
  gap: 8px;
}

.form-section {
  background: #fff;
  padding: 16px 24px;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.product-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.product-section :deep(.vxe-table-list-container) {
  flex: 1;
  overflow: auto;
}

.product-summary {
  margin-top: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
  flex-shrink: 0;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-label {
  font-size: 14px;
  color: #595959;
}

.summary-value {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.summary-value.highlight {
  color: #f5222d;
  font-size: 18px;
}

.bottom-section {
  flex-shrink: 0;
}

.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.footer-amount {
  font-size: 14px;
  color: #595959;
}

.amount-highlight {
  font-size: 20px;
  font-weight: 600;
  color: #f5222d;
}

.footer-right {
  display: flex;
  gap: 12px;
}

.shortcut-hint {
  margin-left: 4px;
  font-size: 12px;
  color: #8c8c8c;
}

.amount-text {
  font-weight: 600;
  color: #262626;
}

/* 紧凑尺寸覆盖 */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}

:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}

:deep(.ant-input-number-sm input) {
  height: 26px;
}
</style>
