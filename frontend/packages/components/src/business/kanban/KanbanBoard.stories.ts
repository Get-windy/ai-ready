import type { Meta, StoryObj } from '@storybook/vue3'
import { ref } from 'vue'
import KanbanBoard from './KanbanBoard.vue'
import KanbanColumn from './KanbanColumn.vue'
import KanbanCard from './KanbanCard.vue'
import KanbanView from './KanbanView.vue'
import type { KanbanCard as KanbanCardType, KanbanColumn as KanbanColumnType } from './types'

/**
 * KanbanBoard 是 AI-Ready ERP 系统的全功能看板组件。
 *
 * 支持列的增删改、卡片拖拽排序、卡片搜索/添加/编辑、优先级标签、
 * 负责人头像、截止日期、标签等展示，以及 API 数据加载。
 */

/* ── Mock Data ─────────────────────────────────────────── */
const sampleColumns: KanbanColumnType[] = [
  {
    id: 1,
    name: '待处理',
    color: '#909399',
    sortOrder: 0,
    cards: [
      {
        id: 101,
        title: '采购原材料 A-2026Q1',
        description: '向供应商 X 采购 500 吨原材料',
        priority: 'HIGH',
        assignee: { id: 1, name: '张三', avatar: '' },
        dueDate: '2026-06-15',
        tags: ['采购', '紧急'],
        createTime: '2026-05-20',
      },
      {
        id: 102,
        title: '审核供应商资质',
        description: '完成新供应商 Y 的资质审核流程',
        priority: 'MEDIUM',
        assignee: { id: 2, name: '李四', avatar: '' },
        dueDate: '2026-06-20',
        tags: ['审核'],
        createTime: '2026-05-22',
      },
      {
        id: 103,
        title: '更新库存报表模板',
        priority: 'LOW',
        assignee: { id: 1, name: '张三', avatar: '' },
        tags: ['报表'],
        createTime: '2026-05-25',
      },
    ],
  },
  {
    id: 2,
    name: '进行中',
    color: '#409eff',
    sortOrder: 1,
    cards: [
      {
        id: 201,
        title: '仓库盘点任务',
        description: '对华东区 3 个仓库进行季度盘点',
        priority: 'HIGH',
        assignee: { id: 3, name: '王五', avatar: '' },
        dueDate: '2026-06-10',
        tags: ['盘点', '紧急'],
        createTime: '2026-05-18',
      },
      {
        id: 202,
        title: '物流合同续签',
        priority: 'MEDIUM',
        assignee: { id: 2, name: '李四', avatar: '' },
        dueDate: '2026-06-25',
        tags: ['合同'],
        createTime: '2026-05-23',
      },
      {
        id: 203,
        title: 'ERP 系统数据迁移',
        description: '将旧系统数据迁移至新 ERP 平台',
        priority: 'HIGH',
        assignee: { id: 4, name: '赵六', avatar: '' },
        dueDate: '2026-07-01',
        tags: ['系统', '紧急'],
        createTime: '2026-05-15',
      },
    ],
  },
  {
    id: 3,
    name: '已完成',
    color: '#67c23a',
    sortOrder: 2,
    cards: [
      {
        id: 301,
        title: 'Q1 供应商评估报告',
        description: '完成 30 家供应商的季度评估',
        priority: 'MEDIUM',
        assignee: { id: 1, name: '张三', avatar: '' },
        dueDate: '2026-05-30',
        tags: ['评估', '报告'],
        createTime: '2026-05-10',
      },
      {
        id: 302,
        title: '采购预算审批',
        priority: 'LOW',
        assignee: { id: 2, name: '李四', avatar: '' },
        tags: ['预算'],
        createTime: '2026-05-12',
      },
    ],
  },
]

const meta: Meta<typeof KanbanBoard> = {
  title: 'Business/KanbanBoard',
  component: KanbanBoard,
  tags: ['autodocs'],
  argTypes: {
    modelName: {
      control: 'text',
      description: '数据模型名称（用于 API 请求）',
    },
    title: {
      control: 'text',
      description: '看板标题',
    },
    groupField: {
      control: 'text',
      description: '分组字段名',
    },
    onCardClick: { action: 'cardClicked' },
    onCardMove: { action: 'cardMoved' },
    onCardAdd: { action: 'cardAdded' },
    onCardUpdate: { action: 'cardUpdated' },
    onColumnAdd: { action: 'columnAdded' },
    onColumnUpdate: { action: 'columnUpdated' },
    onColumnDelete: { action: 'columnDeleted' },
    onRefresh: { action: 'refreshed' },
  },
  args: {
    modelName: 'purchase_order',
    title: '采购订单看板',
    groupField: 'status',
  },
}

export default meta
type Story = StoryObj<typeof KanbanBoard>

// ── 带示例数据（KanbanView 简化版） ─────────────────────
export const SimpleKanbanView: Story = {
  parameters: {
    docs: {
      description: {
        story:
          '使用 KanbanView 组件的简化看板视图。通过传入 data 和 columns props 渲染，适合数据已在客户端准备好的场景。',
      },
    },
  },
  render: () => ({
    components: { KanbanView, KanbanColumn },
    setup() {
      const kanbanData = ref<KanbanCardType[]>([
        {
          id: 1,
          no: 'PO-2026-001',
          status: 1,
          statusText: '待审批',
          customerName: '华东科技',
          totalAmount: 128000,
          date: '2026-05-28',
          userName: '张三',
        },
        {
          id: 2,
          no: 'PO-2026-002',
          status: 1,
          statusText: '待审批',
          customerName: '南京电子',
          totalAmount: 56000,
          date: '2026-05-29',
          userName: '李四',
        },
        {
          id: 3,
          no: 'PO-2026-003',
          status: 2,
          statusText: '已审批',
          customerName: '苏州科技',
          totalAmount: 230000,
          date: '2026-05-27',
          userName: '王五',
        },
        {
          id: 4,
          no: 'PO-2026-004',
          status: 3,
          statusText: '进行中',
          customerName: '杭州电商',
          totalAmount: 89000,
          date: '2026-05-26',
          userName: '赵六',
        },
        {
          id: 5,
          no: 'PO-2026-005',
          status: 4,
          statusText: '已完成',
          customerName: '无锡制造',
          totalAmount: 450000,
          date: '2026-05-20',
          userName: '张三',
        },
        {
          id: 6,
          no: 'PO-2026-006',
          status: 3,
          statusText: '进行中',
          customerName: '常州化工',
          totalAmount: 175000,
          date: '2026-05-25',
          userName: '李四',
        },
      ])
      return { kanbanData }
    },
    template: `
      <div style="height: 500px; border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden;">
        <KanbanView
          :data="kanbanData"
          :show-add-button="true"
          @card-click="(card) => console.log('card clicked:', card)"
          @add="(status) => console.log('add to status:', status)"
          @refresh="() => console.log('refreshed')"
        >
          <template #card="{ card }">
            <KanbanColumn>
              <template #card>
                <div
                  style="background: #fff; border: 1px solid #ebeef5; border-radius: 6px; padding: 12px; cursor: pointer;"
                  @click="() => console.log('clicked:', card.no)"
                >
                  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                    <span style="font-weight: 600; font-size: 14px;">{{ card.no }}</span>
                    <span style="font-size: 12px; padding: 2px 8px; border-radius: 10px; background: #ecf5ff; color: #409eff;">
                      {{ card.statusText }}
                    </span>
                  </div>
                  <div style="font-size: 13px; color: #606266; margin-bottom: 4px;">
                    客户: {{ card.customerName }}
                  </div>
                  <div style="display: flex; justify-content: space-between; align-items: center;">
                    <span style="font-weight: 500; color: #f56c6c;">¥{{ card.totalAmount?.toLocaleString() }}</span>
                    <span style="font-size: 12px; color: #909399;">{{ card.date }}</span>
                  </div>
                </div>
              </template>
            </KanbanColumn>
          </template>
        </KanbanView>
      </div>
    `,
  }),
}

// ── 卡片展示示例 ────────────────────────────────────────
export const CardShowcase: Story = {
  parameters: {
    docs: {
      description: {
        story: 'KanbanCard 组件的各种展示状态 — 含描述、负责人、截止日期、标签。',
      },
    },
  },
  render: () => ({
    components: { KanbanCard },
    setup() {
      const cards = [
        {
          id: 1,
          no: 'PO-2026-001',
          status: 1,
          statusText: '待审批',
          customerName: '华东科技',
          totalAmount: 128000,
          date: '2026-05-28',
          userName: '张三',
        },
        {
          id: 2,
          no: 'PO-2026-002',
          status: 2,
          statusText: '已审批',
          customerName: '南京电子',
          totalAmount: 56000,
          date: '2026-05-29',
          userName: '李四',
        },
        {
          id: 3,
          no: 'PO-2026-003',
          status: 3,
          statusText: '进行中',
          customerName: '苏州科技',
          totalAmount: 230000,
          date: '2026-05-27',
          userName: '王五',
        },
        {
          id: 4,
          no: 'PO-2026-004',
          status: 4,
          statusText: '已完成',
          customerName: '杭州电商',
          totalAmount: 89000,
          date: '2026-05-26',
          userName: '赵六',
        },
      ]
      return { cards }
    },
    template: `
      <div style="display: flex; gap: 16px; flex-wrap: wrap; padding: 24px; background: #f2f3f5;">
        <div v-for="card in cards" :key="card.id" style="min-width: 240px; max-width: 280px;">
          <KanbanCard
            :card="card"
            :show-actions="true"
            :can-edit="true"
            @click="(c) => console.log('clicked:', c)"
            @view="(c) => console.log('view:', c)"
            @edit="(c) => console.log('edit:', c)"
          />
        </div>
      </div>
    `,
  }),
}

// ── 完整看板（模拟数据） ────────────────────────────────
export const FullBoard: Story = {
  parameters: {
    docs: {
      description: {
        story:
          '完整看板展示 — 手动构建看板结构，展示带有所有卡片信息的完整看板视图。',
      },
    },
  },
  render: () => ({
    components: { KanbanBoard, KanbanColumn, KanbanCard },
    setup() {
      const columns = ref<KanbanColumnType[]>(JSON.parse(JSON.stringify(sampleColumns)))
      const dragInfo = ref('拖拽卡片到其他列试试（Demo 模式）')

      const handleCardClick = (card: KanbanCardType) => {
        console.log('card clicked:', card)
      }

      const handleDragDemo = (card: KanbanCardType, fromCol: KanbanColumnType, toCol: KanbanColumnType) => {
        // 模拟拖拽：从源列移除，添加到目标列
        const from = columns.value.find((c) => c.id === fromCol.id)
        const to = columns.value.find((c) => c.id === toCol.id)
        if (from && to && from.id !== to.id) {
          const idx = from.cards.findIndex((c) => c.id === card.id)
          if (idx !== -1) {
            const [moved] = from.cards.splice(idx, 1)
            to.cards.push({ ...moved, columnId: to.id })
            dragInfo.value = `已将卡片 "${moved.title}" 从「${from.name}」移动到「${to.name}」`
          }
        }
      }

      return { columns, dragInfo, handleCardClick, handleDragDemo }
    },
    template: `
      <div style="padding: 24px; background: #f2f3f5; min-height: 500px;">
        <div style="margin-bottom: 16px; padding: 8px 16px; background: #ecf5ff; border-radius: 6px; font-size: 14px; color: #409eff;">
          💡 {{ dragInfo }}
        </div>
        <div style="display: flex; gap: 16px; overflow-x: auto; padding-bottom: 8px;">
          <div v-for="column in columns" :key="column.id"
            style="min-width: 300px; max-width: 300px; background: #ebecf0; border-radius: 8px; display: flex; flex-direction: column;">
            <div style="padding: 12px 16px; display: flex; justify-content: space-between; align-items: center;">
              <div style="display: flex; align-items: center; gap: 8px;">
                <span :style="{ display: 'inline-block', width: 10, height: 10, borderRadius: '50%', backgroundColor: column.color }"></span>
                <span style="font-weight: 600; font-size: 14px;">{{ column.name }}</span>
                <span style="background: #ddd; color: #666; font-size: 12px; padding: 1px 8px; border-radius: 10px;">{{ column.cards.length }}</span>
              </div>
            </div>
            <div style="flex: 1; overflow-y: auto; padding: 8px; max-height: 420px;">
              <div v-for="card in column.cards" :key="card.id"
                draggable="true"
                @dragstart="(e) => { e.dataTransfer.setData('cardId', String(card.id)); e.dataTransfer.setData('fromColumnId', String(column.id)); }"
                @click="handleCardClick(card)"
                style="background: #fff; border-radius: 6px; padding: 12px; margin-bottom: 8px; cursor: pointer; box-shadow: 0 1px 2px rgba(0,0,0,0.1);">
                <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px;">
                  <span style="font-weight: 500; font-size: 14px;">{{ card.title }}</span>
                  <span v-if="card.priority"
                    :style="{
                      fontSize: '11px',
                      padding: '1px 6px',
                      borderRadius: '4px',
                      color: '#fff',
                      backgroundColor: card.priority === 'HIGH' ? '#f56c6c' : card.priority === 'MEDIUM' ? '#e6a23c' : '#67c23a'
                    }">
                    {{ card.priority === 'HIGH' ? '高' : card.priority === 'MEDIUM' ? '中' : '低' }}
                  </span>
                </div>
                <div v-if="card.description" style="font-size: 12px; color: #909399; margin-bottom: 8px;">
                  {{ card.description }}
                </div>
                <div style="display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: #999;">
                  <span v-if="card.assignee" style="display: flex; align-items: center; gap: 4px;">
                    <span style="display: inline-flex; align-items: center; justify-content: center; width: 20px; height: 20px; border-radius: 50%; background: #409eff; color: #fff; font-size: 11px;">
                      {{ card.assignee.name?.charAt(0) }}
                    </span>
                    {{ card.assignee.name }}
                  </span>
                  <span v-if="card.dueDate">{{ card.dueDate }}</span>
                </div>
                <div v-if="card.tags && card.tags.length" style="display: flex; gap: 4px; margin-top: 8px; flex-wrap: wrap;">
                  <span v-for="tag in card.tags" :key="tag"
                    style="font-size: 11px; padding: 1px 6px; border-radius: 4px; background: #f0f2f5; color: #909399;">
                    {{ tag }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    `,
  }),
}

// ── 拖拽演示 ────────────────────────────────────────────
export const DragDemo: Story = {
  parameters: {
    docs: {
      description: {
        story:
          '拖拽演示 — 通过原生 HTML5 Drag & Drop API 实现卡片在不同列之间移动。将卡片拖到目标列放下即可完成移动。',
      },
    },
  },
  render: () => ({
    components: { KanbanBoard },
    setup() {
      const columns = ref<KanbanColumnType[]>(JSON.parse(JSON.stringify(sampleColumns)))
      const dragOverColumnId = ref<number | null>(null)

      const onDragStart = (e: DragEvent, card: KanbanCardType, column: KanbanColumnType) => {
        if (e.dataTransfer) {
          e.dataTransfer.setData('cardId', String(card.id))
          e.dataTransfer.setData('fromColumnId', String(column.id))
          e.dataTransfer.effectAllowed = 'move'
        }
      }

      const onDragOver = (e: DragEvent, column: KanbanColumnType) => {
        e.preventDefault()
        if (e.dataTransfer) {
          e.dataTransfer.dropEffect = 'move'
        }
        dragOverColumnId.value = column.id as number
      }

      const onDragLeave = () => {
        dragOverColumnId.value = null
      }

      const onDrop = (e: DragEvent, toColumn: KanbanColumnType) => {
        e.preventDefault()
        dragOverColumnId.value = null
        if (!e.dataTransfer) return

        const cardId = Number(e.dataTransfer.getData('cardId'))
        const fromColumnId = Number(e.dataTransfer.getData('fromColumnId'))

        if (fromColumnId === toColumn.id) return

        const fromCol = columns.value.find((c) => c.id === fromColumnId)
        const toCol = columns.value.find((c) => c.id === toColumn.id)
        if (!fromCol || !toCol) return

        const cardIdx = fromCol.cards.findIndex((c) => c.id === cardId)
        if (cardIdx === -1) return

        const [moved] = fromCol.cards.splice(cardIdx, 1)
        toCol.cards.push({ ...moved, columnId: toCol.id })
      }

      return { columns, dragOverColumnId, onDragStart, onDragOver, onDragLeave, onDrop }
    },
    template: `
      <div style="padding: 24px; background: #f2f3f5;">
        <p style="margin-bottom: 16px; color: #909399; font-size: 14px;">
          🖱️ 拖拽任意卡片到其他列，松开即完成移动。高亮边框表示可放置目标。
        </p>
        <div style="display: flex; gap: 16px; overflow-x: auto;">
          <div v-for="column in columns" :key="column.id"
            @dragover="onDragOver($event, column)"
            @dragleave="onDragLeave"
            @drop="onDrop($event, column)"
            :style="{
              minWidth: '280px',
              maxWidth: '280px',
              background: dragOverColumnId === column.id ? '#e6f7ff' : '#ebecf0',
              border: dragOverColumnId === column.id ? '2px dashed #409eff' : '2px dashed transparent',
              borderRadius: '8px',
              padding: '12px',
              transition: 'all 0.2s',
            }">
            <div style="margin-bottom: 12px; font-weight: 600; font-size: 14px; display: flex; align-items: center; gap: 8px;">
              <span :style="{ display: 'inline-block', width: 10, height: 10, borderRadius: '50%', backgroundColor: column.color }"></span>
              {{ column.name }}
              <span style="background: #ddd; color: #666; font-size: 12px; padding: 1px 8px; border-radius: 10px;">{{ column.cards.length }}</span>
            </div>
            <div v-for="card in column.cards" :key="card.id"
              draggable="true"
              @dragstart="onDragStart($event, card, column)"
              style="background: #fff; border-radius: 6px; padding: 10px 12px; margin-bottom: 8px; cursor: grab; box-shadow: 0 1px 2px rgba(0,0,0,0.08); font-size: 13px;">
              <div style="font-weight: 500; margin-bottom: 4px;">{{ card.title }}</div>
              <div v-if="card.assignee" style="font-size: 11px; color: #909399;">
                {{ card.assignee.name }}
              </div>
            </div>
            <div v-if="column.cards.length === 0" style="text-align: center; padding: 24px; color: #c0c4cc; font-size: 13px;">
              拖放卡片到此处
            </div>
          </div>
        </div>
      </div>
    `,
  }),
}
