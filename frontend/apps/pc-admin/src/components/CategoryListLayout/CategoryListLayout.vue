<template>
  <div class="category-list-layout">
    <!-- Tab标签页（居中，深色背景） -->
    <div v-if="tabs.length > 0" class="tab-bar">
      <div class="tab-items">
        <div
          v-for="tab in tabs"
          :key="tab.key"
          :class="['tab-item', { active: activeTab === tab.key }]"
          @click="$emit('tab-change', tab.key)"
        >
          {{ tab.label }}
        </div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar-section">
      <div class="toolbar-left">
        <slot name="toolbar-left" />
      </div>
      <div class="toolbar-right">
        <slot name="toolbar-right" />
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-section">
      <slot name="search-fields">
        <div class="search-row">
          <div
            v-for="field in searchFields"
            :key="field.name"
            class="search-item"
          >
            <span class="search-label">{{ field.label }}</span>
            <component
              :is="getSearchComponent(field)"
              v-bind="getSearchProps(field)"
            />
          </div>
          <a-button type="primary" size="small" class="btn-search" @click="$emit('search')">
            查询
          </a-button>
        </div>
        <div v-if="showHierarchyToggle" class="search-row second-row">
          <a-checkbox :checked="showHierarchy" @change="(e: any) => $emit('hierarchy-change', e.target.checked)">
            显示层次结构
          </a-checkbox>
        </div>
      </slot>
    </div>

    <!-- 内容行：左侧分类树 + 右侧表格 -->
    <div class="content-row">
      <!-- 左侧分类面板 -->
      <div v-if="showCategoryPanel" class="category-panel">
        <div class="category-header">
          <span class="category-title">{{ categoryTitle }}</span>
          <div class="category-header-actions">
            <a-button v-if="categoryEditable" type="link" size="small" @click="$emit('category-add')">
              <PlusOutlined />
            </a-button>
            <a-button v-if="categoryCollapsible" type="link" size="small" @click="$emit('category-collapse')">
              <MenuFoldOutlined />
            </a-button>
          </div>
        </div>
        <div class="category-tree-container">
          <a-spin :spinning="categoryLoading">
            <template v-if="categoryError">
              <div class="category-error">
                <a-result status="warning" title="加载失败" sub-title="点击重试">
                  <template #extra>
                    <a-button size="small" @click="$emit('category-retry')">
                      <ReloadOutlined /> 重试
                    </a-button>
                  </template>
                </a-result>
              </div>
            </template>
            <template v-else-if="!categoryLoading && categoryTreeData.length === 0">
              <div class="category-empty">
                <InboxOutlined class="category-empty-icon" />
                <p class="category-empty-text">暂无分类</p>
              </div>
            </template>
            <a-tree
              v-else-if="!categoryLoading"
              :tree-data="categoryTreeData"
              :selected-keys="[selectedCategoryId]"
              :expanded-keys="categoryExpandedKeys"
              show-icon
              block-node
              @select="(keys: any[]) => $emit('category-select', keys)"
              @expand="(keys: any[]) => $emit('category-expand', keys)"
            >
              <template #title="{ categoryName, productCount }">
                <span>{{ categoryName }}</span>
                <span v-if="productCount !== undefined" class="cat-count">({{ productCount }})</span>
              </template>
              <template #icon="{ status }">
                <FolderOutlined v-if="status !== 0" style="color: #faad14" />
                <FolderOpenOutlined v-else />
              </template>
            </a-tree>
          </a-spin>
        </div>
      </div>

      <!-- 右侧表格区域 -->
      <div class="table-panel">
        <div class="table-section">
          <slot name="table" />
        </div>
      </div>
    </div>

    <!-- 底部分页 -->
    <div class="pagination-section">
      <div class="pagination-left">
        <span class="breadcrumb-text">{{ currentPath }}</span>
      </div>
      <div class="pagination-right">
        <a-pagination
          :current="paginationCurrent"
          :pageSize="paginationPageSize"
          :total="paginationTotal"
          :show-size-changer="true"
          :show-quick-jumper="true"
          :page-size-options="pageSizeOptions"
          size="small"
          @change="(page: number, size: number) => $emit('page-change', page, size)"
        />
        <span class="total-text">共 {{ paginationTotal }} 条记录</span>
        <span class="page-size-text">每页显示</span>
        <a-select
          :value="paginationPageSize"
          size="small"
          style="width: 60px"
          @change="(size: number) => $emit('page-change', paginationCurrent, size)"
        >
          <a-select-option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</a-select-option>
        </a-select>
        <span class="page-size-text">行</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  PlusOutlined,
  ReloadOutlined,
  FolderOutlined,
  FolderOpenOutlined,
  InboxOutlined,
  MenuFoldOutlined,
} from '@ant-design/icons-vue'
import { Input, Select, DatePicker, Checkbox } from 'ant-design-vue'

// ── Types ──
export interface TabItem {
  key: string
  label: string
}

export interface SearchField {
  name: string
  label: string
  type: 'input' | 'select' | 'date' | 'checkbox'
  width?: number
  placeholder?: string
  options?: Array<{ label: string; value: any }>
  value?: any
}

// ── Props ─
const props = withDefaults(defineProps<{
  // 分类面板
  showCategoryPanel?: boolean
  categoryTitle?: string
  categoryEditable?: boolean
  categoryCollapsible?: boolean
  categoryTreeData?: any[]
  categoryLoading?: boolean
  categoryError?: boolean
  selectedCategoryId?: number
  categoryExpandedKeys?: number[]

  // Tab
  tabs?: TabItem[]
  activeTab?: string

  // 搜索
  searchFields?: SearchField[]
  showHierarchyToggle?: boolean
  showHierarchy?: boolean

  // 分页
  paginationCurrent?: number
  paginationPageSize?: number
  paginationTotal?: number
  pageSizeOptions?: string[]

  // 路径
  currentPath?: string
}>(), {
  showCategoryPanel: true,
  categoryTitle: '分类',
  categoryEditable: true,
  categoryCollapsible: false,
  categoryTreeData: () => [],
  categoryLoading: false,
  categoryError: false,
  selectedCategoryId: 0,
  categoryExpandedKeys: () => [],
  tabs: () => [],
  activeTab: '',
  searchFields: () => [],
  showHierarchyToggle: false,
  showHierarchy: false,
  paginationCurrent: 1,
  paginationPageSize: 20,
  paginationTotal: 0,
  pageSizeOptions: () => ['10', '20', '50', '100'],
  currentPath: '',
})

// ── Emits ──
defineEmits<{
  // 分类事件
  'category-add': []
  'category-collapse': []
  'category-retry': []
  'category-select': [keys: number[]]
  'category-expand': [keys: number[]]

  // Tab事件
  'tab-change': [key: string]

  // 搜索事件
  'search': []
  'hierarchy-change': [checked: boolean]

  // 分页事件
  'page-change': [page: number, pageSize: number]
}>()

// ── Methods
function getSearchComponent(field: SearchField) {
  const map: Record<string, any> = {
    input: Input,
    select: Select,
    date: DatePicker,
    checkbox: Checkbox,
  }
  return map[field.type] || Input
}

function getSearchProps(field: SearchField) {
  const baseProps: any = {
    size: 'small',
    style: { width: `${field.width || 140}px` },
    placeholder: field.placeholder || `请输入${field.label}`,
    allowClear: true,
  }

  if (field.type === 'select') {
    baseProps.options = field.options
  }

  return baseProps
}
</script>

<style scoped>
/* ── 整体布局：垂直堆叠 ──
 * ⚠️ overflow:hidden 必须加！
 * 不加的话页面级滚动会影响 BillDetailTable 的 sticky 表头定位，
 * 导致表头与表格顶部出现 1px 缝隙透底。
 * 与 BillFormPage 的 .bill-form-page 保持一致。
 */
.category-list-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #f0f2f5;
  padding-top: 5px;
  overflow: hidden;
}

/* ── Tab标签栏：居中 + 深色背景 ── */
.tab-bar {
  background: #4a4a4a;
  padding: 0;
  flex-shrink: 0;
  position: relative;
  z-index: 3;
}
.tab-items {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 4px;
  height: 38px;
  padding: 0 20px;
}
.tab-item {
  padding: 6px 22px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: color 0.2s;
  user-select: none;
  white-space: nowrap;
  position: relative;
  border-radius: 6px 6px 0 0;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-bottom: none;
}
.tab-item:hover:not(.active) {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}
.tab-item.active {
  color: #333;
  background: #fff;
  font-weight: 500;
  border-color: #fff;
  padding-bottom: 8px;
  margin-bottom: -1px;
  z-index: 2;
}
/* 底部开口：::before/::after 延伸底部横线保持边框连续 */
.tab-item.active::before,
.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  width: 9999px;
  height: 1px;
  background: rgba(255, 255, 255, 0.15);
}
.tab-item.active::before {
  right: 100%;
}
.tab-item.active::after {
  left: 100%;
}

/* ─ 工具栏 ── */
.toolbar-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  height: 40px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
  position: relative;
  z-index: 2;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ── 搜索栏 ── */
.search-section {
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
  position: relative;
  z-index: 2;
}

.search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-row.second-row {
  margin-top: 8px;
}

.search-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.search-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}

.btn-search {
  margin-left: 8px;
}

/* ── 内容行：分类树 + 表格 左右分栏 ── */
.content-row {
  display: flex;
  flex: 1;
  min-height: 0;
  gap: 0;
}

/* ── 左侧分类面板 ── */
.category-panel {
  width: 220px;
  min-width: 180px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-shrink: 0;
}

.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.category-header-actions {
  display: flex;
  align-items: center;
}

.category-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}

.category-tree-container {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.cat-count {
  font-size: 12px;
  color: #999;
  margin-left: 4px;
}

.category-error {
  padding: 12px;
}

.category-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 12px;
  text-align: center;
}

.category-empty-icon {
  font-size: 36px;
  color: #d9d9d9;
  margin-bottom: 8px;
}

.category-empty-text {
  font-size: 13px;
  color: #999;
  margin: 0 0 8px 0;
}

/* ── 右侧表格面板 ── */
.table-panel {
  flex: 1;
  min-width: 0;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ⚠️ 必须 display:flex + flex-direction:column，
 * 否则内部 BillDetailTable 的 flex:1 不生效，高度无约束，
 * 导致 sticky 表头在滚动时跳动/出现缝隙。
 * 与 BillFormPage 的 .bill-table-section 保持一致。
 */
.table-section {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* ── 底部分页 ── */
.pagination-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #fafafa;
  border-top: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.pagination-left {
  display: flex;
  align-items: center;
}

.breadcrumb-text {
  font-size: 13px;
  color: #666;
}

.pagination-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.total-text,
.page-size-text {
  font-size: 13px;
  color: #666;
}

/* ── 紧凑尺寸 ── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
</style>
