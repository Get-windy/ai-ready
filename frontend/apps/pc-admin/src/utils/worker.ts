/**
 * Web Worker 工具
 * 用于将计算密集型任务移到后台线程，避免阻塞 UI
 */

/**
 * Worker 任务类型
 */
export type WorkerTask<T = any> = {
  /** 任务 ID */
  id: string
  /** 任务数据 */
  data: T
  /** 任务类型 */
  type?: string
}

/**
 * Worker 结果类型
 */
export type WorkerResult<T = any> = {
  /** 任务 ID */
  id: string
  /** 结果数据 */
  data: T
  /** 是否成功 */
  success: boolean
  /** 错误信息 */
  error?: string
}

/**
 * Worker 消息类型
 */
export type WorkerMessage<T = any, R = any> =
  | ({ type: 'task' } & WorkerTask<T>)
  | ({ type: 'result' } & WorkerResult<R>)
  | { type: 'ready' }
  | { type: 'error', error: string }

/**
 * Worker 配置
 */
export interface WorkerConfig {
  /** Worker 脚本 URL */
  scriptUrl?: string
  /** Worker 脚本内容 */
  scriptContent?: string
  /** 最大重试次数 */
  maxRetries?: number
  /** 超时时间（ms） */
  timeout?: number
}

/**
 * Web Worker 管理器
 */
export class WorkerManager {
  private worker: Worker | null = null
  private tasks: Map<string, {
    resolve: (value: any) => void
    reject: (reason: any) => void
    timeout?: number
  }> = new Map()
  private taskIdCounter = 0
  private config: Required<WorkerConfig>

  constructor(config: WorkerConfig = {}) {
    this.config = {
      scriptUrl: config.scriptUrl,
      scriptContent: config.scriptContent,
      maxRetries: config.maxRetries ?? 3,
      timeout: config.timeout ?? 30000
    }
  }

  /**
   * 初始化 Worker
   */
  private initWorker() {
    if (this.worker) {
      this.worker.terminate()
    }

    if (this.config.scriptUrl) {
      this.worker = new Worker(this.config.scriptUrl)
    } else if (this.config.scriptContent) {
      const blob = new Blob([this.config.scriptContent], { type: 'application/javascript' })
      const url = URL.createObjectURL(blob)
      this.worker = new Worker(url)
      URL.revokeObjectURL(url)
    } else {
      throw new Error('Either scriptUrl or scriptContent must be provided')
    }

    this.worker.onmessage = this.handleMessage.bind(this)
    this.worker.onerror = this.handleError.bind(this)
  }

  /**
   * 处理 Worker 消息
   */
  private handleMessage(event: MessageEvent<WorkerMessage>) {
    const message = event.data

    if (message.type === 'result') {
      const task = this.tasks.get(message.id)
      if (task) {
        if (message.success) {
          task.resolve(message.data)
        } else {
          task.reject(new Error(message.error || 'Task failed'))
        }
        this.tasks.delete(message.id)
      }
    }
  }

  /**
   * 处理 Worker 错误
   */
  private handleError(error: ErrorEvent) {
    console.error('Worker error:', error)
  }

  /**
   * 执行任务
   */
  async execute<T = any, R = any>(data: T, type?: string): Promise<R> {
    if (!this.worker) {
      this.initWorker()
    }

    const taskId = `task-${this.taskIdCounter++}`

    return new Promise<R>((resolve, reject) => {
      const timeoutId = setTimeout(() => {
        this.tasks.delete(taskId)
        reject(new Error(`Task timeout after ${this.config.timeout}ms`))
      }, this.config.timeout)

      this.tasks.set(taskId, {
        resolve: (value) => {
          clearTimeout(timeoutId)
          resolve(value)
        },
        reject: (reason) => {
          clearTimeout(timeoutId)
          reject(reason)
        }
      })

      this.worker?.postMessage({
        type: 'task',
        id: taskId,
        data,
        type
      } as WorkerMessage<T>)
    })
  }

  /**
   * 终止 Worker
   */
  terminate() {
    if (this.worker) {
      this.worker.terminate()
      this.worker = null
    }

    // 拒绝所有待处理的任务
    this.tasks.forEach(({ reject }) => {
      reject(new Error('Worker terminated'))
    })
    this.tasks.clear()
  }

  /**
   * 获取待处理任务数量
   */
  getPendingTaskCount(): number {
    return this.tasks.size
  }
}

/**
 * 创建简单的内联 Worker
 * @param script Worker 脚本
 */
export function createInlineWorker<T = any, R = any>(script: string) {
  const blob = new Blob([script], { type: 'application/javascript' })
  const url = URL.createObjectURL(blob)
  const worker = new Worker(url)

  return {
    worker,
    execute: (data: T): Promise<R> => {
      return new Promise((resolve, reject) => {
        worker.onmessage = (e) => {
          if (e.data.success) {
            resolve(e.data.data)
          } else {
            reject(new Error(e.data.error))
          }
        }
        worker.onerror = reject
        worker.postMessage(data)
      })
    },
    terminate: () => {
      worker.terminate()
      URL.revokeObjectURL(url)
    }
  }
}

/**
 * 数据处理 Worker 模板
 */
export const dataProcessingWorkerScript = `
self.onmessage = function(e) {
  const { id, data, type } = e.data;

  try {
    let result;

    switch (type) {
      case 'filter':
        result = data.items.filter(data.predicate);
        break;
      case 'map':
        result = data.items.map(data.mapper);
        break;
      case 'reduce':
        result = data.items.reduce(data.reducer, data.initialValue);
        break;
      case 'sort':
        result = [...data.items].sort(data.comparer);
        break;
      case 'groupBy':
        result = data.items.reduce((acc, item) => {
          const key = data.keyGetter(item);
          if (!acc[key]) acc[key] = [];
          acc[key].push(item);
          return acc;
        }, {});
        break;
      case 'chunk':
        const chunks = [];
        for (let i = 0; i < data.items.length; i += data.size) {
          chunks.push(data.items.slice(i, i + data.size));
        }
        result = chunks;
        break;
      case 'search':
        result = data.items.filter(item =>
          Object.values(item).some(value =>
            String(value).toLowerCase().includes(data.query.toLowerCase())
          )
        );
        break;
      default:
        throw new Error(\`Unknown task type: \${type}\`);
    }

    self.postMessage({
      type: 'result',
      id,
      data: result,
      success: true
    });
  } catch (error) {
    self.postMessage({
      type: 'result',
      id,
      error: error.message,
      success: false
    });
  }
};
`;

/**
 * 图片处理 Worker 模板
 */
export const imageProcessingWorkerScript = `
self.onmessage = function(e) {
  const { id, data, type } = e.data;

  try {
    let result;

    switch (type) {
      case 'resize':
        result = 'Resize result placeholder'; // 实际实现需要 Canvas API
        break;
      case 'compress':
        result = 'Compress result placeholder';
        break;
      case 'crop':
        result = 'Crop result placeholder';
        break;
      default:
        throw new Error(\`Unknown task type: \${type}\`);
    }

    self.postMessage({
      type: 'result',
      id,
      data: result,
      success: true
    });
  } catch (error) {
    self.postMessage({
      type: 'result',
      id,
      error: error.message,
      success: false
    });
  }
};
`;

/**
 * 数据处理 Worker 管理器（单例）
 */
class DataProcessingWorkerManager {
  private static instance: WorkerManager | null = null

  static getInstance(): WorkerManager {
    if (!DataProcessingWorkerManager.instance) {
      DataProcessingWorkerManager.instance = new WorkerManager({
        scriptContent: dataProcessingWorkerScript
      })
    }
    return DataProcessingWorkerManager.instance
  }
}

/**
 * 数据处理工具函数
 */
export const dataProcess = {
  /**
   * 过滤数据
   */
  filter: async <T>(items: T[], predicate: (item: T) => boolean) => {
    const worker = DataProcessingWorkerManager.getInstance()
    return worker.execute<T[], T[]>({
      items,
      predicate
    }, 'filter')
  },

  /**
   * 映射数据
   */
  map: async <T, R>(items: T[], mapper: (item: T) => R) => {
    const worker = DataProcessingWorkerManager.getInstance()
    return worker.execute<T[], R[]>({
      items,
      mapper
    }, 'map')
  },

  /**
   * 归约数据
   */
  reduce: async <T, R>(items: T[], reducer: (acc: R, item: T) => R, initialValue: R) => {
    const worker = DataProcessingWorkerManager.getInstance()
    return worker.execute<{ items: T[], reducer: (acc: R, item: T) => R, initialValue: R }, R>({
      items,
      reducer,
      initialValue
    }, 'reduce')
  },

  /**
   * 排序数据
   */
  sort: async <T>(items: T[], comparer: (a: T, b: T) => number) => {
    const worker = DataProcessingWorkerManager.getInstance()
    return worker.execute<T[], T[]>({
      items,
      comparer
    }, 'sort')
  },

  /**
   * 分组数据
   */
  groupBy: async <T, K extends string | number>(items: T[], keyGetter: (item: T) => K) => {
    const worker = DataProcessingWorkerManager.getInstance()
    return worker.execute<T[], Record<K, T[]>>({
      items,
      keyGetter
    }, 'groupBy')
  },

  /**
   * 分块数据
   */
  chunk: async <T>(items: T[], size: number) => {
    const worker = DataProcessingWorkerManager.getInstance()
    return worker.execute<T[], T[][]>({
      items,
      size
    }, 'chunk')
  },

  /**
   * 搜索数据
   */
  search: async <T extends Record<string, any>>(items: T[], query: string) => {
    const worker = DataProcessingWorkerManager.getInstance()
    return worker.execute<T[], T[]>({
      items,
      query
    }, 'search')
  }
}

export default WorkerManager