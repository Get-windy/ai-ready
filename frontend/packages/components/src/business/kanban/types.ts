export interface KanbanCard {
  id: number | string
  title: string
  description?: string
  priority?: 'HIGH' | 'MEDIUM' | 'LOW'
  assignee?: {
    id: number | string
    name: string
    avatar?: string
  }
  assigneeId?: number | string
  dueDate?: string | Date
  tags?: string[]
  columnId?: number | string
  modelName?: string
  recordId?: number | string
  createTime?: string | Date
  updateTime?: string | Date
  [key: string]: any
}

export interface KanbanColumn {
  id: number | string
  name: string
  color?: string
  sortOrder?: number
  cards: KanbanCard[]
  modelName?: string
  groupValue?: string
  [key: string]: any
}

export interface KanbanConfig {
  modelName: string
  groupField: string
  titleField?: string
  descriptionField?: string
  priorityField?: string
  assigneeField?: string
  dueDateField?: string
  tagsField?: string
  columns?: KanbanColumnConfig[]
  cardFields?: CardFieldConfig[]
}

export interface KanbanColumnConfig {
  value: string
  label: string
  color?: string
  sortOrder?: number
}

export interface CardFieldConfig {
  field: string
  label: string
  type?: 'text' | 'date' | 'user' | 'priority' | 'tags' | 'number' | 'select'
  visible?: boolean
  editable?: boolean
}

export interface KanbanDataResult {
  columns: KanbanColumn[]
  total: number
  modelName: string
  groupField: string
}

export interface CardMoveRequest {
  cardId: number | string
  fromColumnId: number | string
  toColumnId: number | string
  newSortOrder?: number
}

export interface CardCreateRequest {
  title: string
  description?: string
  priority?: string
  assigneeId?: number | string
  dueDate?: string | Date
  tags?: string[]
  columnId: number | string
  modelName: string
}

export interface ColumnCreateRequest {
  name: string
  color?: string
  sortOrder?: number
  modelName: string
  groupField: string
  groupValue: string
}