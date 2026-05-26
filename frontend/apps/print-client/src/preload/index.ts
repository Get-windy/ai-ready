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
    save: (settings: any) => ipcRenderer.invoke('save-settings', settings)
  },
  
  tasks: {
    getTasks: (params?: any) => ipcRenderer.invoke('get-tasks', params),
    getTask: (id: string) => ipcRenderer.invoke('get-task', id),
    print: (id: string) => ipcRenderer.invoke('print-task', id),
    cancel: (id: string) => ipcRenderer.invoke('cancel-task', id),
    retry: (id: string) => ipcRenderer.invoke('retry-task', id)
  },
  
  templates: {
    getTemplates: () => ipcRenderer.invoke('get-templates')
  },
  
  connection: {
    connect: (url: string) => ipcRenderer.invoke('connect-server', url),
    disconnect: () => ipcRenderer.invoke('disconnect-server'),
    getStatus: () => ipcRenderer.invoke('get-connection-status')
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