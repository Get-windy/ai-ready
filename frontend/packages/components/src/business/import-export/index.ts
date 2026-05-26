import ImportExport from './ImportExport.vue'

export default {
  install(app: any) {
    app.component('ImportExport', ImportExport)
  }
}

export { ImportExport }