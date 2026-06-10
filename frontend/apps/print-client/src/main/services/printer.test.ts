/**
 * PrinterService 单元测试
 *
 * 覆盖：getPrinterList / getDefaultPrinter / setDefaultPrinter /
 *       printHTML / captureScreenshot / printFromTemplate / isPrinting
 */
import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest'
import { mockStore, mockWebContents, mockBrowserWindow } from '../test-setup'

// Mock formatEngine 避免真实模板引擎依赖
vi.mock('./formatEngine', () => ({
  renderTemplateToHtml: vi.fn(() => '<html><body>Mocked Print Content</body></html>')
}))

let PrinterService: any
let service: any

beforeAll(async () => {
  const mod = await import('./printer')
  PrinterService = mod.default
})

beforeEach(() => {
  mockStore.clear()
  vi.clearAllMocks()

  // 重置 mainWindow 为 null，每个测试独立设置
  service = PrinterService
  service.setMainWindow(null)
})

describe('getPrinterList()', () => {
  it('mainWindow 为 null 时返回空数组', async () => {
    const list = await service.getPrinterList()
    expect(list).toEqual([])
  })

  it('返回已格式化的打印机列表', async () => {
    // 给 mock webContents 添加 getPrintersAsync
    mockWebContents.getPrintersAsync = vi.fn(() => Promise.resolve([
      { name: 'printer-01', displayName: '打印机1号', status: 0, isDefault: true, options: {} },
      { name: 'printer-02', displayName: '打印机2号', status: 1, isDefault: false, options: {} },
      { name: 'printer-03', displayName: '打印机3号', status: 3, isDefault: false, options: {} }
    ]))

    const mockWindow = { webContents: mockWebContents }
    service.setMainWindow(mockWindow)

    const list = await service.getPrinterList()

    expect(list).toHaveLength(3)
    expect(list[0]).toMatchObject({ name: 'printer-01', displayName: '打印机1号', status: 'ready', isDefault: true })
    expect(list[1]).toMatchObject({ name: 'printer-02', status: 'printing' })
    expect(list[2]).toMatchObject({ name: 'printer-03', status: 'offline' })
  })

  it('getPrintersAsync 不可用时回退到 getPrinters', async () => {
    mockWebContents.getPrintersAsync = undefined
    mockWebContents.getPrinters = vi.fn(() => [
      { name: 'printer-fallback', displayName: '回退打印机', status: 0, isDefault: true, options: {} }
    ])

    const mockWindow = { webContents: mockWebContents }
    service.setMainWindow(mockWindow)

    const list = await service.getPrinterList()
    expect(list).toHaveLength(1)
    expect(list[0].name).toBe('printer-fallback')
  })
})

describe('setDefaultPrinter() / getDefaultPrinter()', () => {
  it('setDefaultPrinter 保存到 store', () => {
    service.setDefaultPrinter('my-printer')
    expect(mockStore.get('printer.default')).toBe('my-printer')
  })

  it('getDefaultPrinter 从 printerList 匹配已设置的默认打印机', async () => {
    service.setDefaultPrinter('printer-b')
    // 先获取打印机列表
    mockWebContents.getPrintersAsync = vi.fn(() => Promise.resolve([
      { name: 'printer-a', displayName: '打印机A', status: 0, isDefault: false, options: {} },
      { name: 'printer-b', displayName: '打印机B', status: 0, isDefault: false, options: {} }
    ]))
    service.setMainWindow({ webContents: mockWebContents })
    await service.getPrinterList()

    const def = service.getDefaultPrinter()
    expect(def).not.toBeNull()
    expect(def.name).toBe('printer-b')
  })
})

describe('printHTML()', () => {
  it('创建 BrowserWindow 并执行打印', async () => {
    const result = await service.printHTML('task-001', '<h1>Test</h1>', { copies: 2 })

    expect(result.success).toBe(true)
    expect(result.taskId).toBe('task-001')
    expect(mockBrowserWindow).toHaveBeenCalled()
  })

  it('打印失败时返回 error', async () => {
    // printHTML 使用 await print(options)，需 reject 触发 catch 分支
    // 使用 mockRejectedValueOnce 只影响本次调用，不污染后续测试
    mockWebContents.print = vi.fn().mockRejectedValueOnce(new Error('打印机离线'))

    const result = await service.printHTML('task-002', '<h1>Fail</h1>')
    expect(result.success).toBe(false)
    expect(result.error).toBe('打印机离线')
  })

  it('finally 中关闭 BrowserWindow', async () => {
    let closed = false
    const mockWin = {
      webContents: { ...mockWebContents, print: vi.fn((_opts: any, cb?: Function) => cb?.(true)) },
      loadURL: vi.fn(() => Promise.resolve()),
      close: vi.fn(() => { closed = true }),
      isDestroyed: vi.fn(() => false)
    }
    // @ts-ignore
    mockBrowserWindow.mockReturnValueOnce(mockWin)

    await service.printHTML('task-003', '<h1>Cleanup</h1>')
    expect(closed).toBe(true)
  })
})

describe('captureScreenshot()', () => {
  it('创建窗口并截图返回 dataURL', async () => {
    mockWebContents.capturePage = vi.fn(() => Promise.resolve({
      toDataURL: vi.fn(() => 'data:image/png;base64,mock-screenshot')
    }))

    const dataUrl = await service.captureScreenshot(
      { width: 210, height: 297, elements: [] },
      { orderNo: '123' }
    )

    expect(dataUrl).toBe('data:image/png;base64,mock-screenshot')
    // 验证窗口被关闭
  })

  it('finally 中关闭 BrowserWindow', async () => {
    let closed = false
    const mockWin = {
      webContents: {
        ...mockWebContents,
        capturePage: vi.fn(() => Promise.resolve({
          toDataURL: vi.fn(() => 'data:image/png;base64,test')
        }))
      },
      loadURL: vi.fn(() => Promise.resolve()),
      close: vi.fn(() => { closed = true }),
      isDestroyed: vi.fn(() => false)
    }
    // @ts-ignore
    mockBrowserWindow.mockReturnValueOnce(mockWin)

    await service.captureScreenshot({ width: 210, height: 297, elements: [] }, {})
    expect(closed).toBe(true)
  })
})

describe('isPrinting()', () => {
  it('初始状态不打印', () => {
    expect(service.isPrinting()).toBe(false)
  })

  it('printHTML 进行时标记为打印中', async () => {
    // 在 printHTML 中，任务被添加到 printingTasks 后移除
    // 由于 mock print 是同步的，我们可以检查异步执行期间的状态
    const promise = service.printHTML('task-print', '<h1>Test</h1>')
    expect(service.isPrinting()).toBe(true)
    await promise
    expect(service.isPrinting()).toBe(false)
  })
})

describe('printFromTemplate()', () => {
  it('渲染模板后委托给 printHTML', async () => {
    const result = await service.printFromTemplate(
      'task-tpl',
      { width: 210, height: 297, elements: [] },
      { name: 'test' }
    )
    expect(result.success).toBe(true)
  })
})
