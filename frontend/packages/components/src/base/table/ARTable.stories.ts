import type { Meta, StoryObj } from '@storybook/vue3'
import { fn } from '@storybook/test'
import ARTable from './ARTable.vue'

/**
 * ARTable 是 AI-Ready 设计系统的增强型表格组件，基于 ant-design-vue Table 封装。
 *
 * 支持分页、行选择、排序、斑马纹、加载态、空状态等功能。
 */

// ── Mock Data ────────────────────────────────────────────
const mockColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80, sorter: true },
  { title: '名称', dataIndex: 'name', key: 'name', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 150 },
]

const mockData = Array.from({ length: 25 }, (_, i) => ({
  id: i + 1,
  name: `数据记录 #${i + 1}`,
  status: i % 3 === 0 ? '启用' : i % 3 === 1 ? '停用' : '待审核',
  createdAt: new Date(2026, 0, i + 1).toISOString().split('T')[0],
}))

const meta: Meta<typeof ARTable> = {
  title: 'Base/ARTable',
  component: ARTable,
  tags: ['autodocs'],
  argTypes: {
    loading: {
      control: 'boolean',
      description: '是否显示加载状态',
    },
    bordered: {
      control: 'boolean',
      description: '是否显示边框',
    },
    size: {
      control: 'select',
      options: ['default', 'middle', 'small'],
      description: '表格尺寸',
    },
    stripe: {
      control: 'boolean',
      description: '是否显示斑马纹',
    },
    showHeader: {
      control: 'boolean',
      description: '是否显示表头',
    },
    tableLayout: {
      control: 'select',
      options: ['auto', 'fixed'],
      description: '表格布局模式',
    },
    emptyText: {
      control: 'text',
      description: '空数据时的提示文本',
    },
    onChange: { action: 'changed' },
    onSelect: { action: 'selected' },
    onSelectionChange: { action: 'selectionChanged' },
    onRowClick: { action: 'rowClicked' },
    onPageChange: { action: 'pageChanged' },
    onPageSizeChange: { action: 'pageSizeChanged' },
  },
  args: {
    columns: mockColumns,
    data: mockData,
    rowKey: 'id',
    loading: false,
    bordered: false,
    size: 'default',
    showHeader: true,
    stripe: false,
    fit: true,
    tableLayout: 'auto',
  },
}

export default meta
type Story = StoryObj<typeof ARTable>

// ── 基础表格 ────────────────────────────────────────────
export const Basic: Story = {
  args: {
    columns: mockColumns,
    data: mockData,
  },
  parameters: {
    docs: { description: { story: '基础表格 — 包含数据、列定义和默认样式' } },
  },
  render: (args) => ({
    components: { ARTable },
    setup: () => ({ args }),
    template: '<ARTable v-bind="args" />',
  }),
}

// ── 带分页 ──────────────────────────────────────────────
export const WithPagination: Story = {
  args: {
    columns: mockColumns,
    data: mockData.slice(0, 10),
    pagination: {
      current: 1,
      pageSize: 5,
      total: 25,
      showSizeChanger: true,
      pageSizeOptions: ['5', '10', '20'],
      showTotal: (total: number) => `共 ${total} 条`,
    },
  },
  parameters: {
    docs: { description: { story: '带分页的表格 — 支持页码切换和每页条数选择' } },
  },
  render: (args) => ({
    components: { ARTable },
    setup: () => ({ args }),
    template: '<ARTable v-bind="args" />',
  }),
}

// ── 带行选择 ────────────────────────────────────────────
export const WithSelection: Story = {
  args: {
    columns: mockColumns,
    data: mockData.slice(0, 8),
    rowSelection: {
      type: 'checkbox',
      selectedRowKeys: [1, 3],
    },
    pagination: false,
  },
  parameters: {
    docs: { description: { story: '带行选择的表格 — 支持多选 checkbox' } },
  },
  render: (args) => ({
    components: { ARTable },
    setup: () => ({ args }),
    template: '<ARTable v-bind="args" />',
  }),
}

// ── 加载态 ──────────────────────────────────────────────
export const Loading: Story = {
  args: {
    columns: mockColumns,
    data: [],
    loading: true,
    pagination: false,
  },
  parameters: {
    docs: { description: { story: '加载状态 — 显示骨架加载动画' } },
  },
  render: (args) => ({
    components: { ARTable },
    setup: () => ({ args }),
    template: '<ARTable v-bind="args" />',
  }),
}

// ── 空状态 ──────────────────────────────────────────────
export const Empty: Story = {
  args: {
    columns: mockColumns,
    data: [],
    emptyText: '暂无数据，请添加记录',
    pagination: false,
  },
  parameters: {
    docs: { description: { story: '空数据状态 — 自定义空状态文案' } },
  },
  render: (args) => ({
    components: { ARTable },
    setup: () => ({ args }),
    template: '<ARTable v-bind="args" />',
  }),
}

// ── 斑马纹 ──────────────────────────────────────────────
export const Stripe: Story = {
  args: {
    columns: mockColumns,
    data: mockData.slice(0, 10),
    stripe: true,
    bordered: true,
    pagination: false,
  },
  parameters: {
    docs: { description: { story: '斑马纹样式 — 奇数行和偶数行交替背景色' } },
  },
  render: (args) => ({
    components: { ARTable },
    setup: () => ({ args }),
    template: '<ARTable v-bind="args" />',
  }),
}

// ── 小型表格 ────────────────────────────────────────────
export const Small: Story = {
  args: {
    columns: mockColumns,
    data: mockData.slice(0, 10),
    size: 'small',
    bordered: true,
    pagination: false,
  },
  parameters: {
    docs: { description: { story: '紧凑尺寸 — 适合数据密集场景' } },
  },
  render: (args) => ({
    components: { ARTable },
    setup: () => ({ args }),
    template: '<ARTable v-bind="args" />',
  }),
}

// ── 可滚动 ──────────────────────────────────────────────
export const Scrollable: Story = {
  args: {
    columns: mockColumns,
    data: mockData,
    scrollX: 800,
    scrollY: 300,
    pagination: false,
  },
  parameters: {
    docs: { description: { story: '固定表头 + 横向滚动 — 适用于宽表格或大量数据' } },
  },
  render: (args) => ({
    components: { ARTable },
    setup: () => ({ args }),
    template: '<ARTable v-bind="args" />',
  }),
}

// ── 完整功能 Playground ─────────────────────────────────
export const Playground: Story = {
  args: {
    columns: mockColumns,
    data: mockData.slice(0, 12),
    bordered: true,
    stripe: false,
    size: 'default',
    pagination: {
      current: 1,
      pageSize: 5,
      total: 12,
      showSizeChanger: true,
      pageSizeOptions: ['5', '10', '20'],
    },
  },
  parameters: {
    docs: { description: { story: '完整功能 Playground — 通过 Controls 面板自由调整' } },
  },
  render: (args) => ({
    components: { ARTable },
    setup: () => ({ args }),
    template: '<ARTable v-bind="args" />',
  }),
}
