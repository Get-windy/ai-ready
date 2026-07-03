import { ref, reactive, computed, watch } from 'vue'

/**
 * useColumnConfig - 可复用的列配置管理
 *
 * 提供列可见性、宽度调整、冻结、拖拽排序功能，
 * 并持久化到 localStorage，可配合任意表格组件使用。
 *
 * 用法:
 *   const { visibleColumns, showPanel, onSettingChange, ... } = useColumnConfig(columns, 'my-table-key')
 *   在 a-table 中绑定 :columns="visibleColumns"
 */

/** 存储版本号，用于检测并清除旧版本数据 */
const STORAGE_VERSION = 1

export interface ColumnSetting {
  key: string
  title: string
  visible: boolean
  width: number
  fixed: 'left' | 'right' | ''
}

/** 锁定列（不允许隐藏） */
const LOCKED_COLUMNS = ['rowNo', 'action']

export function isLockedColumn(key: string): boolean {
  return LOCKED_COLUMNS.includes(key)
}

export function useColumnConfig(columnDefs: any[], storageKey: string, fillMode?: boolean) {
  const showPanel = ref(false)

  // 默认配置
  const defaultSettings = computed<ColumnSetting[]>(() =>
    columnDefs.map(col => ({
      key: col.key,
      title: col.title || '',
      visible: col.defaultHidden ? false : true,
      width: col.width || 100,
      fixed: col.fixed || '',
    }))
  )

  const columnSettings = reactive<ColumnSetting[]>([...defaultSettings.value])

  // 从 localStorage 加载，带版本检测
  function loadFromStorage() {
    try {
      const raw = localStorage.getItem(storageKey)
      if (!raw) return

      const parsed = JSON.parse(raw)
      // 版本不匹配或格式不对 → 跳过（下次保存时写入新版本）
      if (parsed._version !== STORAGE_VERSION || !Array.isArray(parsed.columns)) {
        localStorage.removeItem(storageKey)
        return
      }

      const storedCols = parsed.columns as ColumnSetting[]
      const merged = defaultSettings.value.map(def => {
        const storedCol = storedCols.find((s: ColumnSetting) => s.key === def.key)
        return storedCol ? { ...def, ...storedCol } : def
      })
      merged.forEach((col, i) => {
        if (i < columnSettings.length) {
          Object.assign(columnSettings[i], col)
        } else {
          columnSettings.push(col)
        }
      })
      if (merged.length < columnSettings.length) {
        columnSettings.splice(merged.length)
      }
    } catch {
      localStorage.removeItem(storageKey)
    }
  }

  loadFromStorage()

  // 同步列定义变化
  watch(() => columnDefs, (newCols) => {
    const existingMap = new Map(columnSettings.map(s => [s.key, s]))
    const merged = newCols.map(col => {
      const existing = existingMap.get(col.key)
      return {
        key: col.key,
        title: col.title || '',
        visible: existing ? existing.visible : (col.defaultHidden ? false : true),
        width: existing ? existing.width : (col.width || 100),
        fixed: existing ? existing.fixed : (col.fixed || ''),
      }
    })
    columnSettings.splice(0, columnSettings.length, ...merged)
  }, { deep: true })

  // 持久化到 localStorage（带版本号）
  function persistSettings() {
    try {
      localStorage.setItem(storageKey, JSON.stringify({
        _version: STORAGE_VERSION,
        columns: columnSettings,
      }))
    } catch {
      // ignore
    }
  }

  // 可见列（按设置顺序 + 过滤隐藏 + 应用运行时宽度/冻结）
  const visibleColumns = computed<any[]>(() => {
    const cols = columnSettings
      .filter(s => s.visible)
      .map(s => {
        const def = columnDefs.find(c => c.key === s.key)
        if (!def) return null
        return {
          ...def,
          width: s.width,
          fixed: s.fixed || undefined,
        }
      })
      .filter(Boolean)
    // 填充模式：尾部追加空白列占满剩余宽度
    if (fillMode) {
      cols.push({ key: '__filler__', title: '', className: 'ss-filler-col' })
    }
    return cols
  })

  // 设置变更
  function onSettingChange() {
    columnSettings.splice(0, 0) // force reactivity
    persistSettings()
  }

  function resetSettings() {
    columnSettings.splice(0, columnSettings.length, ...defaultSettings.value)
    persistSettings()
  }

  // 拖拽排序
  let dragIndex = -1
  function onDragStart(index: number) { dragIndex = index }
  function onDragOver(index: number) {
    if (dragIndex === -1 || dragIndex === index) return
    const item = columnSettings.splice(dragIndex, 1)[0]
    columnSettings.splice(index, 0, item)
    dragIndex = index
  }
  function onDrop() { dragIndex = -1 }

  // 统一处理拖拽结束事件
  function handleColumnDrag({ from, to }: { from: number; to: number }) {
    onDragStart(from)
    onDragOver(to)
    onDrop()
  }

  // 设置面板渲染用的列（排除虚拟列如 __filler__）
  const settingsColumns = computed(() => {
    return columnSettings.filter(s => s.key && !s.key.startsWith('__'))
  })

  return {
    showPanel,
    columnSettings,
    defaultSettings,
    visibleColumns,
    settingsColumns,
    isLockedColumn,
    onSettingChange,
    resetSettings,
    onDragStart,
    onDragOver,
    onDrop,
    handleColumnDrag,
    persistSettings,
    STORAGE_VERSION,
  }
}
