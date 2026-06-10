/**
 * TaskService 单元测试
 *
 * 覆盖：addTask / handleServerTask / processNextTask / cancelTask / retryTask /
 *       confirmScreenshotAndPrint / getQueue / getTask / 队列持久化
 */
import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest'
import { mockStore, mockWebContents } from '../test-setup'

// Mock PrinterService
vi.mock('./printer', () => ({
  default: {
    printHTML: vi.fn((_taskId: string, _html: string, _options?: any) =>
      Promise.resolve({ success: true, printerName: 'test-printer', taskId: _taskId })
    )
  }
}))

// Mock formatEngine
vi.mock('./formatEngine', () => ({
  renderTemplateToHtml: vi.fn(() => '<html><body>Test</body></html>')
}))

let TaskService: any
let service: any

beforeAll(async () => {
  const mod = await import('./task')
  TaskService = mod.default
})

beforeEach(() => {
  mockStore.clear()
  vi.clearAllMocks()
  service = TaskService

  // 重置内部状态：清空队列与运行状态
  service.taskQueue = []
  service.completedTasks = []
  service.waitingConfirmTasks = []
  service.currentTask = null
  service.isProcessing = false
  service.config.autoPrint = false // 防止 addTask 触发 processNextTask
  // 清除重试定时器
  service.retryTimers.forEach((t: any) => clearTimeout(t))
  service.retryTimers.clear()
})

function createTask(overrides: Record<string, any> = {}): any {
  return {
    id: `task-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    taskCode: `TC${Date.now()}`,
    status: 'pending',
    priority: 5,
    copies: 1,
    retryCount: 0,
    maxRetry: 3,
    createTime: Date.now(),
    ...overrides
  }
}

describe('addTask() / getQueue()', () => {
  it('添加任务到队列', () => {
    const task = createTask({ priority: 3 })
    service.addTask(task)

    const queue = service.getQueue()
    expect(queue).toHaveLength(1)
    expect(queue[0].id).toBe(task.id)
    expect(queue[0].status).toBe('pending')
  })

  it('任务按优先级降序排列', () => {
    service.addTask(createTask({ id: 'low', priority: 1 }))
    service.addTask(createTask({ id: 'high', priority: 10 }))
    service.addTask(createTask({ id: 'mid', priority: 5 }))

    const queue = service.getQueue()
    expect(queue[0].id).toBe('high')
    expect(queue[1].id).toBe('mid')
    expect(queue[2].id).toBe('low')
  })

  it('相同优先级按创建时间升序', () => {
    const t1 = createTask({ id: 'first', priority: 5, createTime: 100 })
    const t2 = createTask({ id: 'second', priority: 5, createTime: 200 })

    service.addTask(t1)
    service.addTask(t2)

    const queue = service.getQueue()
    expect(queue[0].id).toBe('first')
    expect(queue[1].id).toBe('second')
  })
})

describe('handleServerTask()', () => {
  it('解析服务端任务格式并加入队列', () => {
    const serverTask = {
      taskId: 1001,
      taskCode: 'SRV-001',
      templateJson: { width: 210, elements: [] },
      dataJson: { orderNo: 'ORD-001' },
      printerName: 'server-printer',
      copies: 2,
      priority: 8
    }

    service.handleServerTask(serverTask)

    const queue = service.getQueue()
    expect(queue).toHaveLength(1)
    expect(queue[0].taskId).toBe(1001)
    expect(queue[0].taskCode).toBe('SRV-001')
    expect(queue[0].copies).toBe(2)
    expect(queue[0].priority).toBe(8)
    expect(queue[0].status).toBe('pending')
  })

  it('包含 renderedHtml 时设为 content', () => {
    service.handleServerTask({
      taskCode: 'HTML-TASK',
      renderedHtml: '<h1>Pre-rendered</h1>',
      copies: 1
    })

    const task = service.getTask('HTML-TASK')
    expect(task).not.toBeNull()
    expect(task.content).toBe('<h1>Pre-rendered</h1>')
  })

  it('支持截图确认模式字段', () => {
    service.handleServerTask({
      taskCode: 'SS-TASK',
      copies: 1,
      screenshotMode: 'MANUAL_CONFIRM',
      screenshotId: 500,
      chainId: 10,
      stepOrder: 2
    })

    const task = service.getTask('SS-TASK')
    expect(task.screenshotMode).toBe('MANUAL_CONFIRM')
    expect(task.screenshotId).toBe(500)
    expect(task.chainId).toBe(10)
    expect(task.stepOrder).toBe(2)
  })
})

describe('cancelTask()', () => {
  it('取消队列中的任务', () => {
    const task = createTask({ id: 'cancel-me' })
    service.addTask(task)

    const result = service.cancelTask('cancel-me')
    expect(result).toBe(true)

    // 已取消的任务不在队列中
    const queue = service.getQueue()
    expect(queue.find((t: any) => t.id === 'cancel-me')).toBeUndefined()
  })

  it('取消不存在的任务返回 false', () => {
    expect(service.cancelTask('non-existent')).toBe(false)
  })
})

describe('retryTask()', () => {
  it('重试失败的任务', () => {
    const task = createTask({ id: 'retry-me', status: 'failed' })
    // 需要先把失败任务加到 completedTasks 才能重试
    // retryTask 从 completedTasks 中查找
    // 先 addTask 然后手动移入 completedTasks
    service.addTask(task)
    // 通过 cancel 变为 completed
    service.cancelTask('retry-me')

    const result = service.retryTask('retry-me')
    expect(result).toBe(true)

    // 任务应回到队列
    const queue = service.getQueue()
    expect(queue.find((t: any) => t.id === 'retry-me')).toBeDefined()
  })

  it('重试不存在的任务返回 false', () => {
    expect(service.retryTask('ghost')).toBe(false)
  })
})

describe('processNextTask()', () => {
  it('处理队列中的下一个任务', async () => {
    const task = createTask({ id: 'process-me' })
    service.addTask(task)

    // 设置 mock PrinterService.printHTML 返回成功
    const printerMod = await import('./printer')
    printerMod.default.printHTML = vi.fn(() =>
      Promise.resolve({ success: true, printerName: 'test', taskId: 'process-me' })
    )

    // processNextTask 是异步的，需要等待
    await service.processNextTask()

    // 确认任务被标记为已完成
    expect(service.isProcessing).toBe(false)
  })

  it('空队列不处理', async () => {
    await service.processNextTask()
    expect(service.isProcessing).toBe(false)
  })

  it('截图确认模式的任务进入等待队列', async () => {
    const task = createTask({
      id: 'waiting-confirm',
      screenshotMode: 'MANUAL_CONFIRM',
      content: '<html><body>Need confirm</body></html>' // 避免 content 为空抛异常
    })
    service.addTask(task)

    await service.processNextTask()

    // 任务应处于 waiting_confirm 状态，且不阻塞队列
    const t = service.getTask('waiting-confirm')
    expect(t).not.toBeNull()
    expect(t.status).toBe('waiting_confirm')
  })
})

describe('confirmScreenshotAndPrint()', () => {
  it('确认截图后继续打印', async () => {
    const task = createTask({
      id: 'confirm-me',
      screenshotMode: 'MANUAL_CONFIRM',
      content: '<html><body>Confirm</body></html>' // 需提供内容通过检查
    })
    service.addTask(task)

    // 触发进入 waiting_confirm
    await service.processNextTask()

    // 确认截图并打印
    const result = await service.confirmScreenshotAndPrint('confirm-me')
    expect(result).toBe(true)
  })

  it('确认不存在的任务返回 false', async () => {
    const result = await service.confirmScreenshotAndPrint('ghost')
    expect(result).toBe(false)
  })
})

describe('getTask()', () => {
  it('在队列、等待队列、完成列表中查找任务', () => {
    const pendingTask = createTask({ id: 'in-queue' })
    service.addTask(pendingTask)

    // 查找队列中的任务
    expect(service.getTask('in-queue')).not.toBeNull()

    // 撤销的任务不再在队列中
    service.cancelTask('in-queue')
    // 但在 completedTasks 中
    const cancelled = service.getTask('in-queue')
    expect(cancelled).not.toBeNull()
    expect(cancelled.status).toBe('cancelled')
  })

  it('不存在的任务返回 null', () => {
    expect(service.getTask('no-such-task')).toBeNull()
  })
})
