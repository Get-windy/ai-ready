import { ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { exportCsv } from '@/utils/exportCsv'
import dayjs from 'dayjs'

interface ExportOptions {
  /** 导出文件名前缀 */
  fileName: string
  /** CSV 表头 */
  headers: string[]
  /** 从后端接口获取全量数据 */
  fetchAll: () => Promise<any>
  /** 将接口数据转为 CSV 行 */
  mapToRows: (data: any) => string[][]
  /** 前端降级（从当前数据生成行） */
  fallbackRows?: () => string[][]
  /** 最大导出行数 */
  maxRows?: number
  /** 当前总行数 */
  total?: number
}

export function useExport() {
  const exporting = ref(false)

  async function execute(options: ExportOptions) {
    const { fileName, headers, fetchAll, mapToRows, fallbackRows, maxRows = 10000, total } = options

    if (total && total > maxRows) {
      Modal.info({
        title: '导出数量过大',
        content: `当前共 ${total} 条记录，超过单次导出上限 ${maxRows} 条。请添加筛选条件缩小范围后重新导出。`,
        okText: '知道了',
        centered: true,
      })
      return
    }

    Modal.confirm({
      title: '导出确认',
      content: `当前共 ${total ?? '--'} 条记录，将导出当前筛选条件下的所有数据。`,
      okText: '确认导出',
      cancelText: '取消',
      centered: true,
      async onOk() {
        exporting.value = true
        const msgKey = `export_${Date.now()}`
        message.loading({ content: '正在获取导出数据...', key: msgKey, duration: 0 })
        try {
          const res = await fetchAll()
          const list = (res as any).data ?? res
          if (Array.isArray(list) && list.length > 0) {
            const rows = mapToRows(list)
            const finalName = `${fileName}_${dayjs().format('YYYYMMDDHHmmss')}`
            exportCsv(headers, rows, finalName)
            message.success({ content: `已导出 ${rows.length} 条记录`, key: msgKey, duration: 3 })
          } else {
            message.warning({ content: '没有符合条件的数据可导出', key: msgKey, duration: 3 })
          }
        } catch {
          // 后端导出不可用时，降级为前端当前数据导出
          if (fallbackRows) {
            message.warning({ content: '后端导出接口不可用，使用当前页面数据导出', key: msgKey, duration: 3 })
            const rows = fallbackRows()
            if (rows.length > 0) {
              const finalName = `${fileName}_${dayjs().format('YYYYMMDDHHmmss')}`
              exportCsv(headers, rows, finalName)
              message.success({ content: `已导出 ${rows.length} 条记录`, key: msgKey, duration: 3 })
            } else {
              message.warning({ content: '没有可导出的数据', key: msgKey, duration: 3 })
            }
          } else {
            message.error({ content: '导出失败', key: msgKey, duration: 3 })
          }
        } finally {
          exporting.value = false
        }
      },
    })
  }

  return { execute, exporting }
}
