import FilePreview from './FilePreview.vue'

export default {
  install(app: any) {
    app.component('FilePreview', FilePreview)
  }
}

export { FilePreview }