import PrintButton from './PrintButton.vue'

export default {
  install(app: any) {
    app.component('PrintButton', PrintButton)
  }
}

export { PrintButton }