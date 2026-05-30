import KanbanBoard from './KanbanBoard.vue'
import KanbanColumn from './KanbanColumn.vue'
import KanbanCard from './KanbanCard.vue'
import KanbanView from './KanbanView.vue'
import type { App } from 'vue'

export { KanbanBoard, KanbanColumn, KanbanCard, KanbanView }
export * from './types'

export default {
  install(app: App) {
    app.component('KanbanBoard', KanbanBoard)
    app.component('KanbanColumn', KanbanColumn)
    app.component('KanbanCard', KanbanCard)
    app.component('KanbanView', KanbanView)
  }
}