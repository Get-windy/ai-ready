import ARChart from './ARChart.vue'

export default {
  install(app: any) {
    app.component('ARChart', ARChart)
  }
}

export { ARChart }