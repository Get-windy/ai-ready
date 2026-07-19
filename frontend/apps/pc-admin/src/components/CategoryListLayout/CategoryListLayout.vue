<template>
  <div class="category-list-layout">
    <!-- Tab标签页（居中，深色背景） -->
    <div
      v-if="tabs.length > 0"
      class="tab-bar"
    >
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
          <a-button
            type="primary"
            size="small"
            class="btn-search"
            @click="$emit('search')"
          >
            查询
          </a-button>
        </div>
        <div
          v-if="showHierarchyToggle"
          class="search-row second-row"
        >
          <a-checkbox
            :checked="showHierarchy"
            @change="(e: any) => $emit('hierarchy-change', e.target.checked)"
          >
            显示层次结构
          </a-checkbox>
        </div>
      </slot>
    </div>

    <!-- 内容行：左侧分类树 + 右侧表格 -->
    <div class="content-row">
      <!-- 左侧分类面板（直通页面底部） -->
      <div
        v-if="!categoryCollapsed && showCategoryPanel"
        class="category-panel"
      >
        <div class="category-header">
          <span class="category-title">{{ categoryTitle }}</span>
          <div class="category-header-actions">
            <a-button
              v-if="categoryEditable"
              type="link"
              size="small"
              title="新增分类"
              @click="$emit('category-add')"
            >
              <PlusOutlined />
            </a-button>
            <a-button
              type="link"
              size="small"
              title="折叠分类树"
              @click="toggleCollapse"
            >
              <MenuFoldOutlined />
            </a-button>
          </div>
        </div>
        <div class="category-tree-container">
          <a-spin :spinning="categoryLoading">
            <template v-if="categoryError">
              <div class="category-error">
                <a-result
                  status="warning"
                  title="加载失败"
                  sub-title="点击重试"
                >
                  <template #extra>
                    <a-button
                      size="small"
                      @click="$emit('category-retry')"
                    >
                      <ReloadOutlined /> 重试
                    </a-button>
                  </template>
                </a-result>
              </div>
            </template>
            <template v-else-if="!categoryLoading && categoryTreeData.length === 0">
              <div class="category-empty">
                <InboxOutlined class="category-empty-icon" />
                <p class="category-empty-text">
                  暂无分类
                </p>
              </div>
            </template>
            <a-tree
              v-else-if="!categoryLoading"
              :tree-data="categoryTreeData"
              :field-names="{ key: 'id', title: 'categoryName', children: 'children' }"
              :selected-keys="[selectedCategoryId]"
              :expanded-keys="categoryExpandedKeys"
              show-icon
              block-node
              @select="(keys: any[]) => $emit('category-select', keys)"
              @expand="(keys: any[]) => $emit('category-expand', keys)"
            >
              <template #title="{ categoryName, productCount }">
                <span>{{ categoryName }}</span>
                <span
                  v-if="productCount !== undefined"
                  class="cat-count"
                >({{ productCount }})</span>
              </template>
              <template #icon="{ expanded }">
                <FolderOpenOutlined
                  v-if="expanded"
                  style="color: #faad14"
                />
                <FolderOutlined
                  v-else
                  style="color: #faad14"
                />
              </template>
            </a-tree>
          </a-spin>
        </div>
        <!-- 底部路径 -->
        <div class="category-breadcrumb">
          <span class="breadcrumb-text">当前路径：</span>
          <span class="breadcrumb-path">{{ currentPath }}</span>
        </div>
      </div>

      <!-- 折叠状态：显示展开拉手 -->
      <div
        v-else-if="categoryCollapsed && showCategoryPanel"
        class="category-collapse-bar"
      >
        <a-button
          type="text"
          class="collapse-toggle-btn"
          title="展开分类树"
          @click="toggleCollapse"
        >
          <MenuUnfoldOutlined />
        </a-button>
      </div>

      <!-- 右侧表格区域（直通页面底部） -->
      <div class="table-panel">
        <div class="table-section">
          <slot name="table" />
        </div>
        <!-- 表格底部插槽（放分页器等） -->
        <div
          v-if="showTableFooter && $slots['table-footer']"
          class="table-footer-section"
        >
          <slot name="table-footer" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  PlusOutlined,
  ReloadOutlined,
  FolderOutlined,
  FolderOpenOutlined,
  InboxOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons-vue'
import { Input, Select, DatePicker, Checkbox } from 'ant-design-vue'

// ─ Types ──
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

// ── Props ──
const props = withDefaults(defineProps<{
  // 分类面板
  showCategoryPanel?: boolean
  categoryTitle?: string
  categoryEditable?: boolean
  categoryTreeData?: any[]
  categoryLoading?: boolean
  categoryError?: boolean
  selectedCategoryId?: string | number
  categoryExpandedKeys?: (string | number)[]

  // Tab
  tabs?: TabItem[]
  activeTab?: string

  // 搜索
  searchFields?: SearchField[]
  showHierarchyToggle?: boolean
  showHierarchy?: boolean

  // 路径
  currentPath?: string

  // 表格底部插槽显示控制
  showTableFooter?: boolean
}>(), {
  showCategoryPanel: true,
  categoryTitle: '分类',
  categoryEditable: true,
  categoryTreeData: () => [],
  categoryLoading: false,
  categoryError: false,
  selectedCategoryId: '0',
  categoryExpandedKeys: () => [],
  tabs: () => [],
  activeTab: '',
  searchFields: () => [],
  showHierarchyToggle: false,
  showHierarchy: false,
  currentPath: '',
  showTableFooter: true,
})

// ── Emits ──
defineEmits<{
  'category-add': []
  'category-retry': []
  'category-select': [keys: (string | number)[]]
  'category-expand': [keys: (string | number)[]]

  'tab-change': [key: string]

  'search': []
  'hierarchy-change': [checked: boolean]
}>()

// ── 折叠状态 ──
const categoryCollapsed = ref(false)

function toggleCollapse() {
  categoryCollapsed.value = !categoryCollapsed.value
}

// ── Methods ──
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
/* ── 整体布局：垂直堆叠 ── */
.category-list-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #f0f2f5;
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
.tab-item.active::before,
.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  width: 9999px;
  height: 1px;
  background: rgba(255, 255, 255, 0.15);
}
.tab-item.active::before { right: 100%; }
.tab-item.active::after { left: 100%; }

/* ── 工具栏 ── */
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

/* ─ 内容行：分类树 + 表格（两者均直通页面底部） ── */
.content-row {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* ── 左侧分类面板（直通页面底部） ── */
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
  background: #fafafa;
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
  min-height: 0;
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

/* 分类面板底部路径 */
.category-breadcrumb {
  padding: 8px 12px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
  flex-shrink: 0;
  font-size: 12px;
  color: #888;
}
.breadcrumb-text {
  color: #888;
}
.breadcrumb-path {
  color: #409eff;
}

/* ── 分类折叠状态：窄条拉手 ── */
.category-collapse-bar {
  width: 28px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 4px;
  flex-shrink: 0;
}
.collapse-toggle-btn {
  padding: 2px 4px;
  font-size: 14px;
  color: #999;
}
.collapse-toggle-btn:hover {
  color: #409eff;
}

/* ─ 右侧表格面板（直通页面底部） ─ */
.table-panel {
  flex: 1;
  min-width: 0;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.table-section {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* ── 表格底部区域（分页器） ── */
.table-footer-section {
  flex-shrink: 0;
}

/* ── 分页栏（表格面板底部，居中） ── */
.pagination-bar {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 8px 16px;
  background: #fafafa;
  border-top: 1px solid #e8e8e8;
  flex-shrink: 0;
  gap: 24px;
}

.pagination-center {
  display: flex;
  align-items: center;
}

.pagination-info {
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.total-text,
.page-size-label {
  font-size: 13px;
  color: #666;
}

/* ── a-tree 选中节点高亮（橙色） ── */
:deep(.ant-tree-node-selected) {
  background: #fff3e0 !important;
}
:deep(.ant-tree-node-selected:hover) {
  background: #ffe0b2 !important;
}

/* ── a-tree 节点基础样式 ── */
:deep(.ant-tree-node-content-wrapper) {
  padding: 2px 4px;
  border-radius: 3px;
}
:deep(.ant-tree-node-content-wrapper:hover) {
  background: #f5f7fa;
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
