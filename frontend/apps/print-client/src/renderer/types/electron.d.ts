/**
 * Electron API 类型声明
 * 由 preload/index.ts 通过 contextBridge 暴露
 */
interface ElectronAPI {
  printers: {
    getPrinters: () => Promise<any[]>
    getDefaultPrinter: () => Promise<string>
    setDefaultPrinter: (name: string) => Promise<boolean>
    testPrint: (name: string) => Promise<any>
  }
  settings: {
    get: () => Promise<Record<string, any>>
    save: (settings: any) => Promise<boolean>
    getSetting: (key: string) => Promise<any>
    setSetting: (key: string, value: any) => Promise<boolean>
  }
  tasks: {
    getTasks: (params?: any) => Promise<any[]>
    getTask: (id: string) => Promise<any>
    getTaskById: (id: string) => Promise<any>
    print: (id: string) => Promise<any>
    cancel: (id: string) => Promise<any>
    retry: (id: string) => Promise<any>
    reprint: (id: string) => Promise<any>
  }
  templates: {
    getTemplates: () => Promise<any[]>
    getTemplate: (id: string) => Promise<any>
    createTemplate: (template: any) => Promise<any>
    updateTemplate: (template: any) => Promise<any>
    deleteTemplate: (id: string) => Promise<any>
    setDefault: (id: string) => Promise<any>
  }
  connection: {
    connect: (url: string) => Promise<boolean>
    disconnect: () => Promise<boolean>
    getStatus: () => Promise<'connected' | 'disconnected' | 'connecting'>
  }
  auth: {
    login: (serverUrl: string, tenantName: string, username: string, password: string, rememberMe: boolean) => Promise<any>
    register: (serverUrl: string, clientName: string, clientVersion?: string) => Promise<any>
    logout: (serverUrl: string) => Promise<boolean>
    checkToken: (serverUrl: string) => Promise<boolean>
    getAuthState: () => Promise<{
      accessToken: string | null
      tenantId: number | null
      tenantName: string | null
      userId: number | null
      username: string | null
      clientId: number | null
      clientName: string | null
      clientCode: string | null
      authKey: string | null
      clientStatus: string | null
      isLoggedIn: boolean
    }>
    tryAutoLogin: (serverUrl: string) => Promise<any>
    getCredentials: () => Promise<{ tenantName: string; username: string; password: string } | null>
  }
  app: {
    setAutoStart: (enabled: boolean) => Promise<boolean>
    getAutoStart: () => Promise<boolean>
    quit: () => Promise<boolean>
    minimize: () => Promise<boolean>
  }
  events: {
    onTaskReceived: (callback: (task: any) => void) => void
    onStatusUpdate: (callback: (status: any) => void) => void
    onNavigate: (callback: (path: string) => void) => void
    removeAllListeners: () => void
  }
}

declare global {
  interface Window {
    electronAPI: ElectronAPI
  }
}

export {}
