import { contextBridge, ipcRenderer } from 'electron'

const api = {
  printers: {
    getPrinters: () => ipcRenderer.invoke('get-printers'),
    getDefaultPrinter: () => ipcRenderer.invoke('get-default-printer'),
    setDefaultPrinter: (name: string) => ipcRenderer.invoke('set-default-printer', name),
    testPrint: (name: string) => ipcRenderer.invoke('test-print', name)
  },

  settings: {
    get: () => ipcRenderer.invoke('get-settings'),
    save: (settings: any) => ipcRenderer.invoke('save-settings', settings),
    getSetting: (key: string) => ipcRenderer.invoke('get-setting', key),
    setSetting: (key: string, value: any) => ipcRenderer.invoke('set-setting', key, value)
  },

  tasks: {
    getTasks: (params?: any) => ipcRenderer.invoke('get-tasks', params),
    getTask: (id: string) => ipcRenderer.invoke('get-task', id),
    getTaskById: (id: string) => ipcRenderer.invoke('get-task-by-id', id),
    print: (id: string) => ipcRenderer.invoke('print-task', id),
    cancel: (id: string) => ipcRenderer.invoke('cancel-task', id),
    retry: (id: string) => ipcRenderer.invoke('retry-task', id),
    reprint: (id: string) => ipcRenderer.invoke('reprint-task', id)
  },

  templates: {
    getTemplates: () => ipcRenderer.invoke('get-templates'),
    getTemplate: (id: string) => ipcRenderer.invoke('get-template', id),
    createTemplate: (template: any) => ipcRenderer.invoke('create-template', template),
    updateTemplate: (template: any) => ipcRenderer.invoke('update-template', template),
    deleteTemplate: (id: string) => ipcRenderer.invoke('delete-template', id),
    setDefault: (id: string) => ipcRenderer.invoke('set-default-template', id)
  },

  connection: {
    connect: (url: string) => ipcRenderer.invoke('connect-server', url),
    disconnect: () => ipcRenderer.invoke('disconnect-server'),
    getStatus: () => ipcRenderer.invoke('get-connection-status')
  },

  auth: {
    login: (serverUrl: string, tenantName: string, username: string, password: string, rememberMe: boolean) =>
      ipcRenderer.invoke('auth-login', serverUrl, tenantName, username, password, rememberMe),
    register: (serverUrl: string, clientName: string, clientVersion?: string) =>
      ipcRenderer.invoke('auth-register', serverUrl, clientName, clientVersion),
    logout: (serverUrl: string) => ipcRenderer.invoke('auth-logout', serverUrl),
    checkToken: (serverUrl: string) => ipcRenderer.invoke('auth-check-token', serverUrl),
    getAuthState: () => ipcRenderer.invoke('auth-get-state'),
    tryAutoLogin: (serverUrl: string) => ipcRenderer.invoke('auth-try-auto-login', serverUrl),
    getCredentials: () => ipcRenderer.invoke('auth-get-credentials')
  },

  app: {
    setAutoStart: (enabled: boolean) => ipcRenderer.invoke('app-set-auto-start', enabled),
    getAutoStart: () => ipcRenderer.invoke('app-get-auto-start'),
    quit: () => ipcRenderer.invoke('app-quit'),
    minimize: () => ipcRenderer.invoke('app-minimize')
  },

  events: {
    onTaskReceived: (callback: (task: any) => void) => {
      ipcRenderer.on('task-received', (_, task) => callback(task))
    },
    onStatusUpdate: (callback: (status: any) => void) => {
      ipcRenderer.on('status-update', (_, status) => callback(status))
    },
    onNavigate: (callback: (path: string) => void) => {
      ipcRenderer.on('navigate', (_, path) => callback(path))
    },
    removeAllListeners: () => {
      ipcRenderer.removeAllListeners('task-received')
      ipcRenderer.removeAllListeners('status-update')
      ipcRenderer.removeAllListeners('navigate')
    }
  }
}

contextBridge.exposeInMainWorld('electronAPI', api)
