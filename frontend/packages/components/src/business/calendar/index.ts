import CalendarView from './CalendarView.vue'
import type { App } from 'vue'

export { CalendarView }

export default {
  install(app: App) {
    app.component('CalendarView', CalendarView)
  }
}