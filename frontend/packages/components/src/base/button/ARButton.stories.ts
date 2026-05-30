import type { Meta, StoryObj } from '@storybook/vue3'
import { fn } from '@storybook/test'
import ARButton from './ARButton.vue'

/**
 * ARButton 是 AI-Ready 设计系统的基础按钮组件。
 *
 * 支持 primary / success / warning / danger / info / default 六种语义类型，
 * large / default / small / mini 四种尺寸，以及 loading、disabled、block、round 等状态。
 */
const meta: Meta<typeof ARButton> = {
  title: 'Base/ARButton',
  component: ARButton,
  tags: ['autodocs'],
  argTypes: {
    type: {
      control: 'select',
      options: ['primary', 'success', 'warning', 'danger', 'info', 'default'],
      description: '按钮语义类型',
      table: { defaultValue: { summary: 'default' } },
    },
    size: {
      control: 'select',
      options: ['large', 'default', 'small', 'mini'],
      description: '按钮尺寸',
      table: { defaultValue: { summary: 'default' } },
    },
    icon: {
      control: 'text',
      description: '图标类名 (Font icon class)',
    },
    loading: {
      control: 'boolean',
      description: '是否显示加载状态',
    },
    disabled: {
      control: 'boolean',
      description: '是否禁用',
    },
    round: {
      control: 'boolean',
      description: '是否为圆形按钮',
    },
    block: {
      control: 'boolean',
      description: '是否撑满父容器宽度',
    },
    nativeType: {
      control: 'select',
      options: ['button', 'submit', 'reset'],
      description: '原生 button type 属性',
      table: { defaultValue: { summary: 'button' } },
    },
    onClick: {
      action: 'clicked',
      description: '点击事件',
    },
  },
  args: {
    type: 'primary',
    size: 'default',
    disabled: false,
    loading: false,
    block: false,
    round: false,
    nativeType: 'button',
    onClick: fn(),
  },
}

export default meta
type Story = StoryObj<typeof ARButton>

// ── 基础类型 ────────────────────────────────────────────
export const Primary: Story = {
  args: { type: 'primary' },
  render: (args) => ({
    components: { ARButton },
    setup: () => ({ args }),
    template: '<ARButton v-bind="args">主要按钮</ARButton>',
  }),
}

export const Success: Story = {
  args: { type: 'success' },
  render: (args) => ({
    components: { ARButton },
    setup: () => ({ args }),
    template: '<ARButton v-bind="args">成功按钮</ARButton>',
  }),
}

export const Warning: Story = {
  args: { type: 'warning' },
  render: (args) => ({
    components: { ARButton },
    setup: () => ({ args }),
    template: '<ARButton v-bind="args">警告按钮</ARButton>',
  }),
}

export const Danger: Story = {
  args: { type: 'danger' },
  render: (args) => ({
    components: { ARButton },
    setup: () => ({ args }),
    template: '<ARButton v-bind="args">危险按钮</ARButton>',
  }),
}

export const Info: Story = {
  args: { type: 'info' },
  render: (args) => ({
    components: { ARButton },
    setup: () => ({ args }),
    template: '<ARButton v-bind="args">信息按钮</ARButton>',
  }),
}

export const Default: Story = {
  args: { type: 'default' },
  render: (args) => ({
    components: { ARButton },
    setup: () => ({ args }),
    template: '<ARButton v-bind="args">默认按钮</ARButton>',
  }),
}

// ── 状态 ────────────────────────────────────────────────
export const Loading: Story = {
  args: { type: 'primary', loading: true },
  parameters: {
    docs: { description: { story: '加载状态 — 按钮不可点击，显示 loading 动画' } },
  },
  render: (args) => ({
    components: { ARButton },
    setup: () => ({ args }),
    template: '<ARButton v-bind="args">提交中...</ARButton>',
  }),
}

export const Disabled: Story = {
  args: { type: 'primary', disabled: true },
  render: (args) => ({
    components: { ARButton },
    setup: () => ({ args }),
    template: '<ARButton v-bind="args">禁用按钮</ARButton>',
  }),
}

export const Block: Story = {
  args: { type: 'primary', block: true },
  parameters: {
    docs: { description: { story: '块级按钮 — 撑满父容器宽度' } },
  },
  render: (args) => ({
    components: { ARButton },
    setup: () => ({ args }),
    template: '<ARButton v-bind="args">块级按钮</ARButton>',
  }),
}

export const Round: Story = {
  args: { type: 'primary', round: true },
  render: (args) => ({
    components: { ARButton },
    setup: () => ({ args }),
    template: '<ARButton v-bind="args">圆角按钮</ARButton>',
  }),
}

// ── 尺寸 ────────────────────────────────────────────────
export const Sizes: Story = {
  parameters: {
    docs: { description: { story: '所有尺寸变体一览' } },
  },
  render: () => ({
    components: { ARButton },
    template: `
      <div style="display: flex; gap: 12px; align-items: center; flex-wrap: wrap">
        <ARButton type="primary" size="large">大号按钮</ARButton>
        <ARButton type="primary" size="default">默认按钮</ARButton>
        <ARButton type="primary" size="small">小号按钮</ARButton>
        <ARButton type="primary" size="mini">迷你按钮</ARButton>
      </div>
    `,
  }),
}

// ── 组合展示 ────────────────────────────────────────────
export const AllVariants: Story = {
  parameters: {
    docs: { description: { story: '全部六种语义类型并排展示' } },
  },
  render: () => ({
    components: { ARButton },
    template: `
      <div style="display: flex; gap: 12px; flex-wrap: wrap">
        <ARButton type="primary">主要</ARButton>
        <ARButton type="success">成功</ARButton>
        <ARButton type="warning">警告</ARButton>
        <ARButton type="danger">危险</ARButton>
        <ARButton type="info">信息</ARButton>
        <ARButton type="default">默认</ARButton>
      </div>
    `,
  }),
}

// ── 交互: 点击日志 ──────────────────────────────────────
export const Playground: Story = {
  args: { type: 'primary' },
  parameters: {
    docs: { description: { story: '可交互 Playground — 通过 Controls 面板调整所有属性' } },
  },
  render: (args) => ({
    components: { ARButton },
    setup: () => ({ args }),
    template: '<ARButton v-bind="args">{{ args.default || "按钮" }}</ARButton>',
  }),
}

