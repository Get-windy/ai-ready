import type { Meta, StoryObj } from '@storybook/vue3'
import { fn } from '@storybook/test'
import { ref } from 'vue'
import ARDialog from './ARDialog.vue'
import ARButton from '../button/ARButton.vue'

/**
 * ARDialog 是 AI-Ready 设计系统的对话框组件，基于 ant-design-vue Modal 封装。
 *
 * 支持基本的打开/关闭、自定义标题/内容/底部、全屏模式、居中显示、关闭前确认等功能。
 */
const meta: Meta<typeof ARDialog> = {
  title: 'Base/ARDialog',
  component: ARDialog,
  tags: ['autodocs'],
  argTypes: {
    title: { control: 'text', description: '对话框标题' },
    width: {
      control: 'text',
      description: '对话框宽度',
      table: { defaultValue: { summary: '50%' } },
    },
    modelValue: { control: 'boolean', description: '是否显示对话框' },
    closeOnClickModal: {
      control: 'boolean',
      description: '点击遮罩层是否关闭',
      table: { defaultValue: { summary: 'true' } },
    },
    closeOnPressEscape: {
      control: 'boolean',
      description: '按 ESC 是否关闭',
      table: { defaultValue: { summary: 'true' } },
    },
    destroyOnClose: { control: 'boolean', description: '关闭时是否销毁子元素' },
    center: { control: 'boolean', description: '是否居中显示' },
    alignCenter: { control: 'boolean', description: '是否垂直居中' },
    showClose: { control: 'boolean', description: '是否显示关闭按钮' },
    onOpen: { action: 'opened' },
    onClose: { action: 'closed' },
    onClosed: { action: 'afterClosed' },
  },
  args: {
    title: '对话框标题',
    width: '520px',
    closeOnClickModal: true,
    closeOnPressEscape: true,
    destroyOnClose: false,
    center: false,
    alignCenter: false,
    showClose: true,
    modelValue: false,
  },
}

export default meta
type Story = StoryObj<typeof ARDialog>

/* ── 对话框触发包装器（复用） ─────────────────────────── */
function useDialogTrigger(initialVisible: boolean) {
  const visible = ref(initialVisible)
  const open = () => { visible.value = true }
  const handleClose = () => { visible.value = false }
  return { visible, open, handleClose }
}

// ── 基础对话框 ──────────────────────────────────────────
export const Basic: Story = {
  args: { title: '基础对话框', width: '520px', modelValue: true },
  parameters: {
    docs: {
      description: {
        story: '最基础的对话框 — 标题 + 内容区域 + 默认底部按钮。',
      },
    },
  },
  render: (args) => ({
    components: { ARDialog, ARButton },
    setup() {
      const { visible, open, handleClose } = useDialogTrigger(args.modelValue as boolean)
      return { args, visible, open, handleClose }
    },
    template: `
      <div>
        <ARButton type="primary" @click="open">打开对话框</ARButton>
        <ARDialog
          v-model:model-value="visible"
          :title="args.title"
          :width="args.width"
          @close="handleClose"
        >
          <p>这是对话框的基本内容区域。</p>
          <p style="color: #909399; margin-top: 8px;">
            你可以在这里放置表单、详情或任意内容。
          </p>
        </ARDialog>
      </div>
    `,
  }),
}

// ── 确认对话框 ──────────────────────────────────────────
export const Confirm: Story = {
  args: { title: '确认删除', width: '420px', modelValue: true, center: true },
  parameters: {
    docs: {
      description: {
        story: '确认型对话框 — 用于危险操作确认，居中显示。',
      },
    },
  },
  render: (args) => ({
    components: { ARDialog, ARButton },
    setup() {
      const { visible, open, handleClose } = useDialogTrigger(args.modelValue as boolean)
      return { args, visible, open, handleClose }
    },
    template: `
      <div>
        <ARButton type="danger" @click="open">删除确认</ARButton>
        <ARDialog
          v-model:model-value="visible"
          title="确认删除"
          width="420px"
          close-on-click-modal
          center
          @close="handleClose"
        >
          <div style="text-align: center; padding: 20px 0;">
            <p style="font-size: 16px; color: #303133;">
              确定要删除这条记录吗？
            </p>
            <p style="color: #909399; margin-top: 8px;">
              此操作不可撤销，请谨慎操作。
            </p>
          </div>
          <template #footer>
            <div style="display: flex; gap: 12px; justify-content: flex-end;">
              <ARButton type="default" @click="handleClose">取消</ARButton>
              <ARButton type="danger" @click="handleClose">确认删除</ARButton>
            </div>
          </template>
        </ARDialog>
      </div>
    `,
  }),
}

// ── 表单对话框 ──────────────────────────────────────────
export const WithForm: Story = {
  args: { title: '新增用户', width: '600px', modelValue: true },
  parameters: {
    docs: {
      description: {
        story: '带表单的对话框 — 常用于新增/编辑操作场景。',
      },
    },
  },
  render: (args) => ({
    components: { ARDialog, ARButton },
    setup() {
      const { visible, open, handleClose } = useDialogTrigger(args.modelValue as boolean)
      const formData = ref({ name: '', email: '', role: '' })
      return { args, visible, open, handleClose, formData }
    },
    template: `
      <div>
        <ARButton type="primary" @click="open">新增用户</ARButton>
        <ARDialog
          v-model:model-value="visible"
          title="新增用户"
          width="600px"
          @close="handleClose"
        >
          <div style="display: flex; flex-direction: column; gap: 16px; padding: 8px 0;">
            <div>
              <label style="display: block; margin-bottom: 4px; font-size: 14px; color: #606266;">姓名</label>
              <input v-model="formData.name" placeholder="请输入姓名"
                style="width: 100%; padding: 8px 12px; border: 1px solid #dcdfe6; border-radius: 4px; font-size: 14px;" />
            </div>
            <div>
              <label style="display: block; margin-bottom: 4px; font-size: 14px; color: #606266;">邮箱</label>
              <input v-model="formData.email" placeholder="请输入邮箱"
                style="width: 100%; padding: 8px 12px; border: 1px solid #dcdfe6; border-radius: 4px; font-size: 14px;" />
            </div>
            <div>
              <label style="display: block; margin-bottom: 4px; font-size: 14px; color: #606266;">角色</label>
              <select v-model="formData.role"
                style="width: 100%; padding: 8px 12px; border: 1px solid #dcdfe6; border-radius: 4px; font-size: 14px;">
                <option value="">请选择角色</option>
                <option value="admin">管理员</option>
                <option value="user">普通用户</option>
                <option value="viewer">只读用户</option>
              </select>
            </div>
          </div>
          <template #footer>
            <div style="display: flex; gap: 12px; justify-content: flex-end;">
              <ARButton type="default" @click="handleClose">取消</ARButton>
              <ARButton type="primary" @click="handleClose">确认</ARButton>
            </div>
          </template>
        </ARDialog>
      </div>
    `,
  }),
}

// ── 全屏对话框 ──────────────────────────────────────────
export const Fullscreen: Story = {
  args: {
    title: '全屏对话框',
    width: '100%',
    closeOnClickModal: false,
    closeOnPressEscape: true,
    modelValue: true,
  },
  parameters: {
    docs: {
      description: {
        story: '全屏对话框 — 适用于复杂表单或详情展示页面。',
      },
    },
  },
  render: (args) => ({
    components: { ARDialog, ARButton },
    setup() {
      const { visible, open, handleClose } = useDialogTrigger(args.modelValue as boolean)
      return { args, visible, open, handleClose }
    },
    template: `
      <div>
        <ARButton type="primary" @click="open">打开全屏对话框</ARButton>
        <ARDialog
          v-model:model-value="visible"
          title="全屏对话框"
          width="100%"
          :close-on-click-modal="false"
          @close="handleClose"
        >
          <div style="min-height: 400px;">
            <p>这是一个全屏对话框，适用于需要大量空间的场景。</p>
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 16px;">
              <div style="padding: 24px; background: #f5f7fa; border-radius: 8px; border: 1px solid #e4e7ed;">
                <h4 style="margin: 0 0 8px;">面板 A</h4>
                <p style="color: #909399;">左侧内容区域</p>
              </div>
              <div style="padding: 24px; background: #f5f7fa; border-radius: 8px; border: 1px solid #e4e7ed;">
                <h4 style="margin: 0 0 8px;">面板 B</h4>
                <p style="color: #909399;">右侧内容区域</p>
              </div>
            </div>
          </div>
        </ARDialog>
      </div>
    `,
  }),
}

// ── Playground ──────────────────────────────────────────
export const Playground: Story = {
  args: {
    title: 'Playground',
    width: '520px',
    closeOnClickModal: true,
    closeOnPressEscape: true,
    center: false,
    alignCenter: false,
    modelValue: true,
  },
  parameters: {
    docs: {
      description: {
        story: '通过右侧 Controls 面板自由调整对话框所有属性。',
      },
    },
  },
  render: (args) => ({
    components: { ARDialog, ARButton },
    setup() {
      const { visible, open, handleClose } = useDialogTrigger(args.modelValue as boolean)
      return { args, visible, open, handleClose }
    },
    template: `
      <div>
        <ARButton type="primary" @click="open">打开 Playground 对话框</ARButton>
        <ARDialog
          v-model:model-value="visible"
          :title="args.title"
          :width="args.width"
          :close-on-click-modal="args.closeOnClickModal"
          :close-on-press-escape="args.closeOnPressEscape"
          :center="args.center"
          :align-center="args.alignCenter"
          @close="handleClose"
        >
          <p>通过右侧 Controls 面板调整对话框的各种属性。</p>
          <p style="color: #909399; margin-top: 8px;">
            试试切换 title、width、center 等参数。
          </p>
        </ARDialog>
      </div>
    `,
  }),
}
