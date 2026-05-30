import type { Meta, StoryObj } from '@storybook/vue3'
import { ref } from 'vue'
import ModuleLayout from './ModuleLayout.vue'
import type { BreadcrumbItem, FilterItem, ModuleTabsItem } from './types'

/**
 * ModuleLayout 是 AI-Ready ERP 系统的标准模块页面布局组件。
 *
 * 集成了面包屑导航、视图切换（列表/看板/日历/图表）、搜索筛选面板、
 * 模块级 Tab 导航、分页控件，以及加载/错误/空状态展示。
 */
const meta: Meta<typeof ModuleLayout> = {
  title: 'Layout/ModuleLayout',
  component: ModuleLayout,
  tags: ['autodocs'],
  argTypes: {
    currentView: {
      control: 'select',
      options: ['list', 'kanban', 'calendar', 'graph'],
      description: '当前激活的视图模式',
    },
    loading: { control: 'boolean', description: '是否显示加载状态' },
    error: { control: 'text', description: '错误消息（非空时显示错误状态）' },
    empty: { control: 'boolean', description: '是否显示空状态' },
    emptyText: { control: 'text', description: '空状态提示文本' },
    showViewSwitch: { control: 'boolean', description: '是否显示视图切换按钮' },
    showSearch: { control: 'boolean', description: '是否显示搜索框' },
    showFilterButton: { control: 'boolean', description: '是否显示筛选按钮' },
    showPagination: { control: 'boolean', description: '是否显示分页控件' },
    selectedCount: { control: 'number', description: '已选中条目数' },
    searchValue: { control: 'text', description: '搜索关键词' },
    currentPage: { control: 'number', description: '当前页码' },
    totalPages: { control: 'number', description: '总页数' },
    totalItems: { control: 'number', description: '总条目数' },
    pageSize: { control: 'number', description: '每页条数' },
    activeFilterCount: { control: 'number', description: '激活的筛选条件数' },
  },
  args: {
    breadcrumbItems: [
      { text: '首页', path: '/' },
      { text: '采购管理', path: '/purchase' },
      { text: '采购订单', path: '/purchase/order' },
    ] as BreadcrumbItem[],
    currentView: 'list',
    showViewSwitch: true,
    showSearch: true,
    searchPlaceholder: '搜索订单号/供应商...',
    showFilterButton: true,
    activeFilterCount: 0,
    showPagination: true,
    currentPage: 1,
    totalPages: 5,
    pageSize: 20,
    totalItems: 98,
    loading: false,
    error: null,
    empty: false,
    emptyText: '暂无数据',
  },
}

export default meta
type Story = StoryObj<typeof ModuleLayout>

// ── 列表示图 ────────────────────────────────────────────
export const WithListView: Story = {
  args: {
    currentView: 'list',
  },
  parameters: {
    docs: {
      description: {
        story: '列表模式 — 最常见的数据展示方式，包含面包屑、搜索、分页。',
      },
    },
  },
  render: (args) => ({
    components: { ModuleLayout },
    setup() {
      const currentView = ref(args.currentView)
      const currentPage = ref(args.currentPage)
      const searchValue = ref(args.searchValue || '')
      return { args, currentView, currentPage, searchValue }
    },
    template: `
      <div style="height: 600px; border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden;">
        <ModuleLayout
          v-bind="args"
          :current-view="currentView"
          :current-page="currentPage"
          :search-value="searchValue"
          @view-change="currentView = $event"
          @page-change="currentPage = $event"
          @search-input="searchValue = $event"
        >
          <template #list-view>
            <div style="padding: 52px 0; text-align: center; color: #909399;">
              <div style="font-size: 48px; margin-bottom: 12px;">📋</div>
              <p style="font-size: 16px; margin: 0;">列表视图内容区域</p>
              <p style="font-size: 13px; margin-top: 8px;">
                此处放置 ARTable 等列表组件
              </p>
            </div>
          </template>
        </ModuleLayout>
      </div>
    `,
  }),
}

// ── 看板视图 ────────────────────────────────────────────
export const WithKanbanView: Story = {
  args: {
    currentView: 'kanban',
    availableViews: [
      { value: 'list', label: '列表', icon: '☰' },
      { value: 'kanban', label: '看板', icon: '▦' },
    ],
  },
  parameters: {
    docs: {
      description: {
        story: '看板模式 — 以卡片列的形式展示数据，适用于流程管理。',
      },
    },
  },
  render: (args) => ({
    components: { ModuleLayout },
    setup() {
      const currentView = ref(args.currentView)
      return { args, currentView }
    },
    template: `
      <div style="height: 600px; border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden;">
        <ModuleLayout
          v-bind="args"
          :current-view="currentView"
          @view-change="currentView = $event"
        >
          <template #kanban-view>
            <div style="display: flex; gap: 12px; overflow-x: auto; padding: 8px;">
              <div v-for="col in ['待处理', '进行中', '已完成']" :key="col"
                style="min-width: 240px; background: #f5f7fa; border-radius: 8px; padding: 12px;">
                <h4 style="margin: 0 0 12px; font-size: 14px; font-weight: 600;">{{ col }}</h4>
                <div v-for="i in 3" :key="i"
                  style="background: #fff; padding: 10px; margin-bottom: 8px; border-radius: 4px; border: 1px solid #ebeef5; font-size: 13px;">
                  卡片 {{ col === '待处理' ? i : col === '进行中' ? i + 3 : i + 6 }}
                </div>
              </div>
            </div>
          </template>
        </ModuleLayout>
      </div>
    `,
  }),
}

// ── 带搜索面板 ──────────────────────────────────────────
export const WithSearchPanel: Story = {
  args: {
    currentView: 'list',
    showSearchPanel: true,
    searchPanelCollapsed: false,
    activeFilterCount: 2,
    filters: [
      {
        key: 'status',
        label: '订单状态',
        type: 'checkbox' as const,
        options: [
          { value: 'draft', label: '草稿' },
          { value: 'pending', label: '待审批' },
          { value: 'approved', label: '已审批' },
          { value: 'rejected', label: '已驳回' },
        ],
      },
      {
        key: 'supplier',
        label: '供应商',
        type: 'select' as const,
        options: [
          { value: 's1', label: '供应商 A' },
          { value: 's2', label: '供应商 B' },
          { value: 's3', label: '供应商 C' },
        ],
      },
      {
        key: 'dateRange',
        label: '创建日期',
        type: 'daterange' as const,
      },
    ] as FilterItem[],
  },
  parameters: {
    docs: {
      description: {
        story: '展开搜索面板 — 左侧显示筛选条件面板，右侧为列表内容。',
      },
    },
  },
  render: (args) => ({
    components: { ModuleLayout },
    setup() {
      const currentView = ref(args.currentView)
      return { args, currentView }
    },
    template: `
      <div style="height: 600px; border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden;">
        <ModuleLayout
          v-bind="args"
          :current-view="currentView"
          @view-change="currentView = $event"
        >
          <template #list-view>
            <div style="padding: 52px 0; text-align: center; color: #909399;">
              <p>列表视图 + 搜索面板</p>
            </div>
          </template>
        </ModuleLayout>
      </div>
    `,
  }),
}

// ── 加载态 ──────────────────────────────────────────────
export const Loading: Story = {
  args: {
    loading: true,
    currentView: 'list',
  },
  parameters: {
    docs: {
      description: { story: '加载状态 — 显示进度条，页面操作被禁用。' },
    },
  },
  render: (args) => ({
    components: { ModuleLayout },
    setup() {
      return { args }
    },
    template: `
      <div style="height: 600px; border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden;">
        <ModuleLayout v-bind="args" />
      </div>
    `,
  }),
}

// ── 错误态 ──────────────────────────────────────────────
export const ErrorState: Story = {
  args: {
    error: '加载数据失败，请检查网络连接后重试。',
    currentView: 'list',
  },
  parameters: {
    docs: {
      description: { story: '错误状态 — 显示错误信息和重试按钮。' },
    },
  },
  render: (args) => ({
    components: { ModuleLayout },
    setup() {
      return { args }
    },
    template: `
      <div style="height: 600px; border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden;">
        <ModuleLayout v-bind="args" />
      </div>
    `,
  }),
}

// ── 空状态 ──────────────────────────────────────────────
export const EmptyState: Story = {
  args: {
    empty: true,
    emptyText: '暂无采购订单数据',
    currentView: 'list',
    showPagination: false,
  },
  parameters: {
    docs: {
      description: { story: '空状态 — 数据为空时的友好提示。' },
    },
  },
  render: (args) => ({
    components: { ModuleLayout },
    setup() {
      return { args }
    },
    template: `
      <div style="height: 600px; border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden;">
        <ModuleLayout v-bind="args" />
      </div>
    `,
  }),
}

// ── 带 Tab ──────────────────────────────────────────────
export const WithTabs: Story = {
  args: {
    currentView: 'list',
    tabs: [
      { key: 'all', label: '全部', count: 98 },
      { key: 'draft', label: '草稿', count: 12 },
      { key: 'pending', label: '待审批', count: 23 },
      { key: 'approved', label: '已审批', count: 45 },
      { key: 'rejected', label: '已驳回', count: 18 },
    ] as ModuleTabsItem[],
    activeTab: 'all',
  },
  parameters: {
    docs: {
      description: {
        story: '模块级 Tab 导航 — 按状态分类切换数据视图。',
      },
    },
  },
  render: (args) => ({
    components: { ModuleLayout },
    setup() {
      const currentView = ref(args.currentView)
      const activeTab = ref(args.activeTab)
      return { args, currentView, activeTab }
    },
    template: `
      <div style="height: 600px; border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden;">
        <ModuleLayout
          v-bind="args"
          :current-view="currentView"
          :active-tab="activeTab"
          @view-change="currentView = $event"
          @update:active-tab="activeTab = $event"
        >
          <template #list-view>
            <div style="padding: 52px 0; text-align: center; color: #909399;">
              <p>当前 Tab：{{ activeTab }}</p>
            </div>
          </template>
        </ModuleLayout>
      </div>
    `,
  }),
}

// ── 带批量操作 ──────────────────────────────────────────
export const WithBatchActions: Story = {
  args: {
    currentView: 'list',
    selectedCount: 3,
  },
  parameters: {
    docs: {
      description: {
        story: '批量操作 — 选中条目后显示批量操作按钮。',
      },
    },
  },
  render: (args) => ({
    components: { ModuleLayout },
    setup() {
      const currentView = ref(args.currentView)
      return { args, currentView }
    },
    template: `
      <div style="height: 600px; border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden;">
        <ModuleLayout
          v-bind="args"
          :current-view="currentView"
          @view-change="currentView = $event"
        >
          <template #batch-actions>
            <button style="padding: 4px 12px; border: 1px solid #dcdfe6; background: #fff; border-radius: 4px; cursor: pointer; font-size: 13px;">批量审批</button>
            <button style="padding: 4px 12px; border: 1px solid #f56c6c; background: #fff; color: #f56c6c; border-radius: 4px; cursor: pointer; font-size: 13px;">批量删除</button>
          </template>
          <template #list-view>
            <div style="padding: 52px 0; text-align: center; color: #909399;">
              <p>已选中 {{ args.selectedCount }} 项</p>
            </div>
          </template>
        </ModuleLayout>
      </div>
    `,
  }),
}
