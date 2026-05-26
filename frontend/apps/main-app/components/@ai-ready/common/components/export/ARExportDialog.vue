<template>
  <el-dialog
    v-model="visible"
    title="导出数据"
    width="500px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="ar-export-dialog">
      <el-form :model="form" label-width="100px">
        <el-form-item label="导出格式">
          <el-radio-group v-model="form.format">
            <el-radio label="csv">CSV</el-radio>
            <el-radio label="excel">Excel</el-radio>
            <el-radio label="pdf">PDF</el-radio>
            <el-radio label="png">PNG</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="导出范围">
          <el-radio-group v-model="form.range">
            <el-radio label="current">当前页</el-radio>
            <el-radio label="all">全部数据</el-radio>
            <el-radio label="selected" :disabled="selectedCount === 0">
              选中项 ({{ selectedCount }})
            </el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item 
          label="数据范围" 
          v-if="form.range === 'all'"
        >
          <el-date-picker
            v-model="form.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="包含字段" v-if="availableFields.length > 0">
          <el-checkbox-group v-model="form.fields">
            <div class="ar-export-dialog__fields">
              <el-checkbox 
                v-for="field in availableFields" 
                :key="field.key"
                :label="field.key"
              >
                {{ field.label }}
              </el-checkbox>
            </div>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      
      <div v-if="exporting" class="ar-export-dialog__progress">
        <el-progress :percentage="progress" />
        <p class="ar-export-dialog__progress-text">
          正在导出数据，请稍候...
        </p>
      </div>
    </div>
    
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button 
        type="primary" 
        @click="handleExport" 
        :loading="exporting"
        :disabled="canExport"
      >
        导出
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import Papa from 'papaparse'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'

export interface ExportField {
  key: string
  label: string
}

const props = defineProps<{
  availableFields?: ExportField[]
  selectedCount?: number
}>()

const emit = defineEmits<{
  close: []
  export: [format: string, data: any]
}>()

const visible = ref(false)
const exporting = ref(false)
const progress = ref(0)

const form = reactive({
  format: 'csv',
  range: 'current',
  dateRange: [] as Date[],
  fields: props.availableFields?.map(f => f.key) || [],
})

const canExport = computed(() => {
  return form.fields.length > 0
})

const handleExport = async () => {
  if (canExport.value) {
    exporting.value = true
    progress.value = 0
    
    try {
      // 模拟导出进度
      const steps = 10
      for (let i = 1; i <= steps; i++) {
        await new Promise(resolve => setTimeout(resolve, 200))
        progress.value = Math.floor((i / steps) * 100)
      }
      
      // 执行实际导出
      await executeExport()
      
      ElMessage.success('导出成功')
      handleClose()
    } catch (error: any) {
      ElMessage.error('导出失败：' + error.message)
    } finally {
      exporting.value = false
      progress.value = 0
    }
  }
}

const executeExport = async () => {
  const format = form.format
  const data = {
    range: form.range,
    dateRange: form.dateRange,
    fields: form.fields,
  }
  
  emit('export', format, data)
  
  // 这里可以实现实际的导出逻辑
  switch (format) {
    case 'csv':
      await exportCSV()
      break
    case 'excel':
      await exportExcel()
      break
    case 'pdf':
      await exportPDF()
      break
    case 'png':
      await exportPNG()
      break
  }
}

const exportCSV = async () => {
  // 示例：使用PapaParse导出CSV
  const data = [
    ['ID', '名称', '状态', '时间'],
    ['1', '示例数据', '正常', '2026-04-28'],
  ]
  
  const csv = Papa.unparse(data)
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  
  link.setAttribute('href', url)
  link.setAttribute('download', 'export.csv')
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const exportExcel = async () => {
  // 示例：使用SheetJS导出Excel
  // 实际实现需要安装 xlsx 库
  ElMessage.info('Excel导出功能需要安装 xlsx 库')
}

const exportPDF = async () => {
  // 示例：使用html2canvas + jsPDF导出PDF
  const element = document.querySelector('.ar-dashboard') as HTMLElement
  
  if (element) {
    const canvas = await html2canvas(element, {
      scale: 2,
      useCORS: true,
    })
    
    const imgData = canvas.toDataURL('image/png')
    const pdf = new jsPDF('p', 'mm', 'a4')
    const pdfWidth = pdf.internal.pageSize.getWidth()
    const pdfHeight = (canvas.height * pdfWidth) / canvas.width
    
    pdf.addImage(imgData, 'PNG', 0, 0, pdfWidth, pdfHeight)
    pdf.save('export.pdf')
  }
}

const exportPNG = async () => {
  // 示例：使用html2canvas导出PNG
  const element = document.querySelector('.ar-dashboard') as HTMLElement
  
  if (element) {
    const canvas = await html2canvas(element, {
      scale: 2,
      useCORS: true,
    })
    
    const link = document.createElement('a')
    link.download = 'export.png'
    link.href = canvas.toDataURL()
    link.click()
  }
}

const handleClose = () => {
  visible.value = false
  emit('close')
}

const open = () => {
  visible.value = true
}

defineExpose({ open })
</script>

<style scoped>
.ar-export-dialog {
  padding: 8px 0;
}

.ar-export-dialog__fields {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  max-height: 200px;
  overflow-y: auto;
  padding: 8px;
  border: 1px solid var(--ar-border-light);
  border-radius: 4px;
}

.ar-export-dialog__progress {
  margin-top: 20px;
  padding: 20px;
  text-align: center;
  background: var(--ar-bg-page);
  border-radius: 4px;
}

.ar-export-dialog__progress-text {
  margin: 12px 0 0 0;
  color: var(--ar-text-secondary);
  font-size: 14px;
}

/* 响应式 */
@media (max-width: 768px) {
  .ar-export-dialog__fields {
    grid-template-columns: 1fr;
  }
}
</style>