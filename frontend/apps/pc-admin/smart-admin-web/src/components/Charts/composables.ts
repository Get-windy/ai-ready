/**
 * 图表导出 composable
 */
import { ref } from 'vue'
import type { ExportOptions, ChartDataPoint } from './types'

/**
 * 导出图表为图片或数据
 */
export function useChartExport() {
  const isExporting = ref(false)
  
  /**
   * 将SVG导出为图片
   */
  const exportAsImage = async (
    svgElement: SVGElement,
    options: ExportOptions
  ): Promise<void> => {
    isExporting.value = true
    
    try {
      const { type, filename = 'chart', pixelRatio = 2 } = options
      
      if (type === 'png' || type === 'svg') {
        // 序列化SVG
        const serializer = new XMLSerializer()
        let svgString = serializer.serializeToString(svgElement)
        
        // 添加命名空间
        if (!svgString.includes('xmlns')) {
          svgString = svgString.replace('<svg', '<svg xmlns="http://www.w3.org/2000/svg"')
        }
        
        if (type === 'svg') {
          // 直接下载SVG
          downloadFile(svgString, `${filename}.svg`, 'image/svg+xml')
        } else {
          // 转换为PNG
          const canvas = document.createElement('canvas')
          const ctx = canvas.getContext('2d')
          const img = new Image()
          
          // 获取SVG尺寸
          const bbox = svgElement.getBoundingClientRect()
          canvas.width = bbox.width * pixelRatio
          canvas.height = bbox.height * pixelRatio
          
          await new Promise<void>((resolve, reject) => {
            img.onload = () => {
              ctx?.drawImage(img, 0, 0, canvas.width, canvas.height)
              resolve()
            }
            img.onerror = reject
            
            const blob = new Blob([svgString], { type: 'image/svg+xml' })
            const url = URL.createObjectURL(blob)
            img.src = url
          })
          
          // 转换为PNG并下载
          canvas.toBlob((blob) => {
            if (blob) {
              const url = URL.createObjectURL(blob)
              const a = document.createElement('a')
              a.href = url
              a.download = `${filename}.png`
              a.click()
              URL.revokeObjectURL(url)
            }
          }, 'image/png')
        }
      } else if (type === 'csv' || type === 'json') {
        // 数据导出（需要传入数据）
        // 这里简化处理，实际可以从外部传入
        console.warn('Data export requires data parameter')
      }
    } finally {
      isExporting.value = false
    }
  }
  
  /**
   * 导出图表数据
   */
  const exportAsData = (
    data: ChartDataPoint[],
    options: ExportOptions
  ): void => {
    const { type, filename = 'chart-data' } = options
    
    if (type === 'json') {
      const json = JSON.stringify(data, null, 2)
      downloadFile(json, `${filename}.json`, 'application/json')
    } else if (type === 'csv') {
      const headers = ['name', 'value']
      const rows = data.map(d => `${d.name},${d.value}`)
      const csv = [headers.join(','), ...rows].join('\n')
      downloadFile(csv, `${filename}.csv`, 'text/csv')
    }
  }
  
  /**
   * 下载文件辅助函数
   */
  const downloadFile = (
    content: string,
    filename: string,
    mimeType: string
  ): void => {
    const blob = new Blob([content], { type: mimeType })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
  }
  
  return {
    isExporting,
    exportAsImage,
    exportAsData
  }
}

/**
 * 图表筛选 composable
 */
export function useChartFilter() {
  const filters = ref<{ field: string; value: any }[]>([])
  
  /**
   * 添加筛选条件
   */
  const addFilter = (field: string, value: any) => {
    const existing = filters.value.findIndex(f => f.field === field)
    if (existing >= 0) {
      filters.value[existing].value = value
    } else {
      filters.value.push({ field, value })
    }
  }
  
  /**
   * 移除筛选条件
   */
  const removeFilter = (field: string) => {
    filters.value = filters.value.filter(f => f.field !== field)
  }
  
  /**
   * 清除所有筛选
   */
  const clearFilters = () => {
    filters.value = []
  }
  
  /**
   * 应用筛选
   */
  const applyFilters = <T extends ChartDataPoint[]>(data: T): T => {
    if (filters.value.length === 0) return data
    
    return data.filter(item => {
      return filters.value.every(filter => {
        const value = item[filter.field as keyof ChartDataPoint]
        return value === filter.value
      })
    }) as T
  }
  
  return {
    filters,
    addFilter,
    removeFilter,
    clearFilters,
    applyFilters
  }
}

/**
 * 图表缩放 composable
 */
export function useChartZoom() {
  const zoomRange = ref({ start: 0, end: 100 })
  const isZoomed = ref(false)
  
  /**
   * 设置缩放范围
   */
  const setZoom = (start: number, end: number) => {
    zoomRange.value = { start, end }
    isZoomed.value = start > 0 || end < 100
  }
  
  /**
   * 重置缩放
   */
  const resetZoom = () => {
    zoomRange.value = { start: 0, end: 100 }
    isZoomed.value = false
  }
  
  /**
   * 放大
   */
  const zoomIn = (step = 10) => {
    const { start, end } = zoomRange.value
    const newStart = Math.min(start + step, end - step)
    const newEnd = Math.max(end - step, newStart + step)
    setZoom(newStart, newEnd)
  }
  
  /**
   * 缩小
   */
  const zoomOut = (step = 10) => {
    const { start, end } = zoomRange.value
    const newStart = Math.max(start - step, 0)
    const newEnd = Math.min(end + step, 100)
    setZoom(newStart, newEnd)
  }
  
  return {
    zoomRange,
    isZoomed,
    setZoom,
    resetZoom,
    zoomIn,
    zoomOut
  }
}