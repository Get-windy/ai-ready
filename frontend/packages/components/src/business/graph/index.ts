import GraphView from './GraphView.vue'
import type { App } from 'vue'

export { GraphView }

export default {
  install(app: App) {
    app.component('GraphView', GraphView)
  }
}