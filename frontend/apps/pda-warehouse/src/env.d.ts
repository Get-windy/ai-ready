/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface ElectronAPI {
  batch?: {
    getBatchDetail?: (id: string) => Promise<any>
    getBatchList?: (params?: any) => Promise<any>
    submitQualityCheck?: (data: any) => Promise<any>
  }
}

interface Window {
  electronAPI?: ElectronAPI
}
