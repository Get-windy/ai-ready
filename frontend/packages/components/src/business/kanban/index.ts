import KanbanBoard from './KanbanBoard.vue'
import type { App } from 'vue'

export { KanbanBoard }
export * from './types'

export default {
  install(app: App) {
    app.component('KanbanBoard', KanbanBoard)
  }
}