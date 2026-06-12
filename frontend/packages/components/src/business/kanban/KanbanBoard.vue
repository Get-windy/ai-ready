<template>
  <div class="kanban-board">
    <div class="kanban-header">
      <div class="kanban-title">
        <h3>{{ title }}</h3>
        <span class="kanban-count">{{ totalCards }} 条记录</span>
      </div>
      <div class="kanban-actions">
        <a-input-search
          v-model:value="searchText"
          placeholder="搜索卡片..."
          style="width: 200px"
          @search="onSearch"
        />
        <a-button type="primary" @click="onRefresh">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>
    </div>

    <div class="kanban-columns" ref="columnsRef">
      <div
        v-for="column in columns"
        :key="column.id"
        class="kanban-column"
        :data-column-id="column.id"
      >
        <div class="kanban-column-header">
          <div class="column-title">
            <span class="column-name">{{ column.name }}</span>
            <a-badge :count="column.cards.length" :number-style="{ backgroundColor: column.color }" />
          </div>
          <div class="column-actions">
            <a-dropdown>
              <a-button type="text" size="small">
                <template #icon><MoreOutlined /></template>
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="onAddCard(column)">添加卡片</a-menu-item>
                  <a-menu-item @click="onEditColumn(column)">编辑列</a-menu-item>
                  <a-menu-divider />
                  <a-menu-item @click="onDeleteColumn(column)">删除列</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
        </div>

        <div class="kanban-column-body" ref="columnBodyRef">
          <div
            v-for="card in column.cards"
            :key="card.id"
            class="kanban-card"
            :data-card-id="card.id"
            draggable="true"
            @dragstart="onDragStart($event, card, column)"
            @dragend="onDragEnd"
            @click="onCardClick(card)"
          >
            <div class="card-header">
              <span class="card-title">{{ card.title }}</span>
              <a-tag v-if="card.priority" :color="getPriorityColor(card.priority)">
                {{ card.priority }}
              </a-tag>
            </div>
            <div class="card-body">
              <div v-if="card.description" class="card-description">
                {{ card.description }}
              </div>
              <div class="card-meta">
                <span v-if="card.assignee" class="card-assignee">
                  <a-avatar :size="24" :src="card.assignee.avatar">
                    {{ card.assignee.name?.charAt(0) }}
                  </a-avatar>
                  <span class="assignee-name">{{ card.assignee.name }}</span>
                </span>
                <span v-if="card.dueDate" class="card-due-date">
                  <ClockCircleOutlined />
                  {{ formatDate(card.dueDate) }}
                </span>
              </div>
              <div v-if="card.tags && card.tags.length" class="card-tags">
                <a-tag v-for="tag in card.tags" :key="tag" size="small">
                  {{ tag }}
                </a-tag>
              </div>
            </div>
            <div class="card-footer">
              <span class="card-id">#{{ card.id }}</span>
              <span class="card-create-time">{{ formatDate(card.createTime) }}</span>
            </div>
          </div>

          <div v-if="column.cards.length === 0" class="kanban-empty">
            <a-empty description="暂无卡片" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
          </div>
        </div>

        <div class="kanban-column-footer">
          <a-button type="dashed" block @click="onAddCard(column)">
            <template #icon><PlusOutlined /></template>
            添加卡片
          </a-button>
        </div>
      </div>

      <div class="kanban-column-add">
        <a-button type="dashed" block @click="onAddColumn">
          <template #icon><PlusOutlined /></template>
          添加列
        </a-button>
      </div>
    </div>

    <a-modal
      v-model:open="cardModalVisible"
      :title="editingCard ? '编辑卡片' : '添加卡片'"
      width="600px"
      @ok="onCardModalOk"
    >
      <a-form :model="cardForm" layout="vertical">
        <a-form-item label="标题" required>
          <a-input v-model:value="cardForm.title" placeholder="请输入卡片标题" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="cardForm.description" :rows="3" placeholder="请输入卡片描述" />
        </a-form-item>
        <a-form-item label="优先级">
          <a-select v-model:value="cardForm.priority" placeholder="选择优先级">
            <a-select-option value="HIGH">高</a-select-option>
            <a-select-option value="MEDIUM">中</a-select-option>
            <a-select-option value="LOW">低</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="负责人">
          <a-select v-model:value="cardForm.assigneeId" placeholder="选择负责人" show-search>
            <a-select-option v-for="user in users" :key="user.id" :value="user.id">
              {{ user.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="截止日期">
          <a-date-picker v-model:value="cardForm.dueDate" style="width: 100%" />
        </a-form-item>
        <a-form-item label="标签">
          <a-select v-model:value="cardForm.tags" mode="tags" placeholder="添加标签">
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="columnModalVisible"
      :title="editingColumn ? '编辑列' : '添加列'"
      @ok="onColumnModalOk"
    >
      <a-form :model="columnForm" layout="vertical">
        <a-form-item label="列名称" required>
          <a-input v-model:value="columnForm.name" placeholder="请输入列名称" />
        </a-form-item>
        <a-form-item label="列颜色">
          <a-input v-model:value="columnForm.color" type="color" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="columnForm.sortOrder" :min="0" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { Empty } from 'ant-design-vue'
import {
  ReloadOutlined,
  MoreOutlined,
  PlusOutlined,
  ClockCircleOutlined
} from '@ant-design/icons-vue'
import type { KanbanColumn, KanbanCard, KanbanConfig } from './types'
import dayjs from 'dayjs'

interface Props {
  modelName: string
  title?: string
  groupField?: string
  config?: KanbanConfig
}

const props = withDefaults(defineProps<Props>(), {
  title: '看板视图',
  groupField: 'state'
})

const emit = defineEmits<{
  (e: 'card-click', card: KanbanCard): void
  (e: 'card-move', card: KanbanCard, fromColumn: KanbanColumn, toColumn: KanbanColumn): void
  (e: 'card-add', card: Partial<KanbanCard>, column: KanbanColumn): void
  (e: 'card-update', card: KanbanCard): void
  (e: 'column-add', column: Partial<KanbanColumn>): void
  (e: 'column-update', column: KanbanColumn): void
  (e: 'column-delete', column: KanbanColumn): void
  (e: 'refresh'): void
}>()

const columns = ref<KanbanColumn[]>([])
const searchText = ref('')
const cardModalVisible = ref(false)
const columnModalVisible = ref(false)
const editingCard = ref<KanbanCard | null>(null)
const editingColumn = ref<KanbanColumn | null>(null)
const currentColumn = ref<KanbanColumn | null>(null)
const users = ref<any[]>([])
const draggedCard = ref<KanbanCard | null>(null)
const draggedFromColumn = ref<KanbanColumn | null>(null)

const cardForm = ref({
  title: '',
  description: '',
  priority: '',
  assigneeId: '',
  dueDate: null as any,
  tags: [] as string[]
})

const columnForm = ref({
  name: '',
  color: '#1890ff',
  sortOrder: 0
})

const totalCards = computed(() => {
  return columns.value.reduce((sum, col) => sum + col.cards.length, 0)
})

const formatDate = (date: string | Date) => {
  return dayjs(date).format('YYYY-MM-DD')
}

const getPriorityColor = (priority: string) => {
  const colors: Record<string, string> = {
    HIGH: 'red',
    MEDIUM: 'orange',
    LOW: 'green'
  }
  return colors[priority] || 'default'
}

const onSearch = () => {
  loadKanbanData()
}

const onRefresh = () => {
  loadKanbanData()
  emit('refresh')
}

const loadKanbanData = async () => {
  try {
    const response = await fetch(`/api/kanban/${props.modelName}/data?groupField=${props.groupField}&search=${searchText.value}`)
    const result = await response.json()
    if (result.code === 200) {
      columns.value = result.data.columns
    }
  } catch (error) {
    console.error('加载看板数据失败:', error)
  }
}

const loadUsers = async () => {
  try {
    const response = await fetch('/api/user/list')
    const result = await response.json()
    if (result.code === 200) {
      users.value = result.data.records
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
  }
}

const onCardClick = (card: KanbanCard) => {
  emit('card-click', card)
}

const onAddCard = (column: KanbanColumn) => {
  editingCard.value = null
  currentColumn.value = column
  cardForm.value = {
    title: '',
    description: '',
    priority: '',
    assigneeId: '',
    dueDate: null,
    tags: []
  }
  cardModalVisible.value = true
}

const onCardModalOk = async () => {
  if (!cardForm.value.title) {
    return
  }

  if (editingCard.value) {
    const updatedCard = {
      ...editingCard.value,
      ...cardForm.value
    }
    emit('card-update', updatedCard as KanbanCard)
  } else if (currentColumn.value) {
    emit('card-add', cardForm.value as any, currentColumn.value)
  }

  cardModalVisible.value = false
  loadKanbanData()
}

const onAddColumn = () => {
  editingColumn.value = null
  columnForm.value = {
    name: '',
    color: '#1890ff',
    sortOrder: columns.value.length
  }
  columnModalVisible.value = true
}

const onEditColumn = (column: KanbanColumn) => {
  editingColumn.value = column
  columnForm.value = {
    name: column.name,
    color: column.color || '#1890ff',
    sortOrder: column.sortOrder || 0
  }
  columnModalVisible.value = true
}

const onDeleteColumn = async (column: KanbanColumn) => {
  emit('column-delete', column)
  loadKanbanData()
}

const onColumnModalOk = async () => {
  if (!columnForm.value.name) {
    return
  }

  if (editingColumn.value) {
    const updatedColumn = {
      ...editingColumn.value,
      ...columnForm.value
    }
    emit('column-update', updatedColumn as KanbanColumn)
  } else {
    emit('column-add', columnForm.value)
  }

  columnModalVisible.value = false
  loadKanbanData()
}

const onDragStart = (event: DragEvent, card: KanbanCard, column: KanbanColumn) => {
  draggedCard.value = card
  draggedFromColumn.value = column
  event.dataTransfer!.effectAllowed = 'move'
  event.dataTransfer!.setData('text/plain', card.id.toString())
}

const onDragEnd = () => {
  draggedCard.value = null
  draggedFromColumn.value = null
}

const setupDragDrop = () => {
  const columnsEl = document.querySelectorAll('.kanban-column-body')

  columnsEl.forEach(columnEl => {
    columnEl.addEventListener('dragover', (e: any) => {
      e.preventDefault()
      e.dataTransfer!.dropEffect = 'move'
      columnEl.classList.add('drag-over')
    })

    columnEl.addEventListener('dragleave', () => {
      columnEl.classList.remove('drag-over')
    })

    columnEl.addEventListener('drop', async (e: Event) => {
      e.preventDefault()
      columnEl.classList.remove('drag-over')

      const dragEvent = e as DragEvent
      const cardId = dragEvent.dataTransfer!.getData('text/plain')
      const targetColumnId = columnEl.closest('.kanban-column')!.getAttribute('data-column-id')

      if (draggedCard.value && draggedFromColumn.value && targetColumnId) {
        const targetColumn = columns.value.find(c => c.id.toString() === targetColumnId)
        if (targetColumn && targetColumn.id !== draggedFromColumn.value.id) {
          emit('card-move', draggedCard.value, draggedFromColumn.value, targetColumn)
          loadKanbanData()
        }
      }
    })
  })
}

onMounted(() => {
  loadKanbanData()
  loadUsers()
  setTimeout(setupDragDrop, 100)
})

watch(() => props.modelName, () => {
  loadKanbanData()
})
</script>

<style scoped lang="scss">
.kanban-board {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
  padding: 16px;
}

.kanban-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.kanban-title {
  display: flex;
  align-items: center;
  gap: 12px;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
  }

  .kanban-count {
    color: #666;
    font-size: 14px;
  }
}

.kanban-actions {
  display: flex;
  gap: 12px;
}

.kanban-columns {
  display: flex;
  gap: 16px;
  flex: 1;
  overflow-x: auto;
  padding-bottom: 8px;
}

.kanban-column {
  min-width: 280px;
  max-width: 280px;
  background: #ebecf0;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
}

.kanban-column-header {
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
}

.column-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.kanban-column-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  min-height: 100px;

  &.drag-over {
    background: rgba(24, 144, 255, 0.1);
  }
}

.kanban-card {
  background: white;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);

  &:hover {
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
    transform: translateY(-2px);
  }

  &:active {
    cursor: grabbing;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.card-title {
  font-weight: 500;
  font-size: 14px;
  line-height: 1.4;
}

.card-body {
  margin-bottom: 8px;
}

.card-description {
  color: #666;
  font-size: 12px;
  margin-bottom: 8px;
  line-height: 1.4;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 12px;
  color: #666;
}

.card-assignee {
  display: flex;
  align-items: center;
  gap: 6px;
}

.assignee-name {
  font-size: 12px;
}

.card-due-date {
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #999;
}

.kanban-empty {
  padding: 24px;
  text-align: center;
}

.kanban-column-footer {
  padding: 8px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
}

.kanban-column-add {
  min-width: 280px;
  max-width: 280px;
  padding: 8px;
}
</style>