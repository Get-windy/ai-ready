<script setup lang="ts">
import { ref, computed } from 'vue'

interface ExportConfig {
  module: string
  format: 'xlsx' | 'csv' | 'pdf' | 'json'
  fields: ExportField[]
  filters?: Record<string, any>
  fileName?: string
}

interface ExportField {
  key: string
  label: string
  selected: boolean
  order: number
}

interface ImportConfig {
  module: string
  file: File | null
  mode: 'create' | 'update' | 'merge'
  mapping: FieldMapping[]
}

interface FieldMapping {
  sourceField: string
  targetField: string
  transform?: string
}

const props = defineProps<{
  module: string
  moduleLabel: string
  availableFields: { key: string; label: string }[]
}>()

const emit = defineEmits<{
  (e: 'export', config: ExportConfig): void
  (e: 'import', config: ImportConfig): void
}>()

const showExportDialog = ref(false)
const showImportDialog = ref(false)
const exportFormat = ref<'xlsx' | 'csv' | 'pdf' | 'json'>('xlsx')
const exportFields = ref<ExportField[]>([])
const importFile = ref<File | null>(null)
const importMode = ref<'create' | 'update' | 'merge'>('create')
const importMapping = ref<FieldMapping[]>([])
const importPreview = ref<any[]>([])
const importLoading = ref(false)
const exportLoading = ref(false)

const selectedFieldsCount = computed(() => exportFields.value.filter(f => f.selected).length)

const initExportFields = () => {
  exportFields.value = props.availableFields.map((field, index) => ({
    key: field.key,
    label: field.label,
    selected: true,
    order: index
  }))
}

const openExportDialog = () => {
  initExportFields()
  showExportDialog.value = true
}

const openImportDialog = () => {
  importFile.value = null
  importMode.value = 'create'
  importMapping.value = []
  importPreview.value = []
  showImportDialog.value = true
}

const toggleField = (field: ExportField) => {
  field.selected = !field.selected
}

const selectAllFields = () => {
  exportFields.value.forEach(f => f.selected = true)
}

const deselectAllFields = () => {
  exportFields.value.forEach(f => f.selected = false)
}

const moveFieldUp = (index: number) => {
  if (index > 0) {
    const temp = exportFields.value[index]
    exportFields.value[index] = exportFields.value[index - 1]
    exportFields.value[index - 1] = temp
    exportFields.value[index].order = index
    exportFields.value[index - 1].order = index - 1
  }
}

const moveFieldDown = (index: number) => {
  if (index < exportFields.value.length - 1) {
    const temp = exportFields.value[index]
    exportFields.value[index] = exportFields.value[index + 1]
    exportFields.value[index + 1] = temp
    exportFields.value[index].order = index
    exportFields.value[index + 1].order = index + 1
  }
}

const handleExport = async () => {
  if (selectedFieldsCount.value === 0) {
    alert('请至少选择一个导出字段')
    return
  }
  
  exportLoading.value = true
  try {
    const config: ExportConfig = {
      module: props.module,
      format: exportFormat.value,
      fields: exportFields.value.filter(f => f.selected).sort((a, b) => a.order - b.order),
      fileName: `${props.moduleLabel}_${new Date().toISOString().slice(0, 10)}`
    }
    
    emit('export', config)
    showExportDialog.value = false
  } finally {
    exportLoading.value = false
  }
}

const handleFileSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    importFile.value = target.files[0]
    await parseImportFile()
  }
}

const parseImportFile = async () => {
  if (!importFile.value) return
  
  importLoading.value = true
  try {
    const reader = new FileReader()
    reader.onload = (e) => {
      const content = e.target?.result as string
      if (importFile.value?.name.endsWith('.csv')) {
        parseCSV(content)
      } else if (importFile.value?.name.endsWith('.json')) {
        parseJSON(content)
      }
    }
    
    if (importFile.value.name.endsWith('.csv')) {
      reader.readAsText(importFile.value)
    } else if (importFile.value.name.endsWith('.json')) {
      reader.readAsText(importFile.value)
    } else {
      alert('仅支持CSV和JSON格式')
    }
  } finally {
    importLoading.value = false
  }
}

const parseCSV = (content: string) => {
  const lines = content.split('\n')
  if (lines.length < 2) return
  
  const headers = lines[0].split(',').map(h => h.trim())
  importPreview.value = lines.slice(1, 6).map(line => {
    const values = line.split(',')
    const obj: Record<string, string> = {}
    headers.forEach((h, i) => {
      obj[h] = values[i]?.trim() || ''
    })
    return obj
  })
  
  initImportMapping(headers)
}

const parseJSON = (content: string) => {
  try {
    const data = JSON.parse(content)
    if (Array.isArray(data)) {
      importPreview.value = data.slice(0, 5)
      const keys = Object.keys(data[0] || {})
      initImportMapping(keys)
    }
  } catch {
    alert('JSON格式解析失败')
  }
}

const initImportMapping = (sourceFields: string[]) => {
  importMapping.value = sourceFields.map(source => {
    const target = props.availableFields.find(f => 
      f.key.toLowerCase() === source.toLowerCase() ||
      f.label.toLowerCase() === source.toLowerCase()
    )
    return {
      sourceField: source,
      targetField: target?.key || ''
    }
  })
}

const updateMapping = (index: number, targetField: string) => {
  importMapping.value[index].targetField = targetField
}

const handleImport = async () => {
  if (!importFile.value) {
    alert('请选择导入文件')
    return
  }
  
  const mappedFields = importMapping.value.filter(m => m.targetField)
  if (mappedFields.length === 0) {
    alert('请至少映射一个字段')
    return
  }
  
  importLoading.value = true
  try {
    const config: ImportConfig = {
      module: props.module,
      file: importFile.value,
      mode: importMode.value,
      mapping: importMapping.value.filter(m => m.targetField)
    }
    
    emit('import', config)
    showImportDialog.value = false
  } finally {
    importLoading.value = false
  }
}

const downloadTemplate = () => {
  const headers = props.availableFields.map(f => f.label).join(',')
  const content = headers + '\n'
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${props.moduleLabel}_导入模板.csv`
  a.click()
  URL.revokeObjectURL(url)
}
</script>

<template>
  <div class="import-export-buttons">
    <button class="export-btn" @click="openExportDialog">
      <svg viewBox="0 0 24 24" width="16" height="16">
        <path fill="currentColor" d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/>
      </svg>
      导出数据
    </button>
    
    <button class="import-btn" @click="openImportDialog">
      <svg viewBox="0 0 24 24" width="16" height="16">
        <path fill="currentColor" d="M9 16h6v-6h4l-7-7-7 7h4zm-4 2h14v2H5z"/>
      </svg>
      导入数据
    </button>
    
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showExportDialog" class="dialog-overlay" @click="showExportDialog = false">
          <div class="export-dialog" @click.stop>
            <div class="dialog-header">
              <span>导出 {{ moduleLabel }} 数据</span>
              <button @click="showExportDialog = false">×</button>
            </div>
            
            <div class="dialog-body">
              <div class="format-section">
                <label>导出格式</label>
                <div class="format-options">
                  <label class="format-option">
                    <input type="radio" v-model="exportFormat" value="xlsx" />
                    <span>Excel (XLSX)</span>
                  </label>
                  <label class="format-option">
                    <input type="radio" v-model="exportFormat" value="csv" />
                    <span>CSV</span>
                  </label>
                  <label class="format-option">
                    <input type="radio" v-model="exportFormat" value="pdf" />
                    <span>PDF</span>
                  </label>
                  <label class="format-option">
                    <input type="radio" v-model="exportFormat" value="json" />
                    <span>JSON</span>
                  </label>
                </div>
              </div>
              
              <div class="fields-section">
                <div class="fields-header">
                  <label>导出字段 (已选 {{ selectedFieldsCount }}/{{ exportFields.length }})</label>
                  <div class="field-actions">
                    <button @click="selectAllFields">全选</button>
                    <button @click="deselectAllFields">全不选</button>
                  </div>
                </div>
                
                <div class="fields-list">
                  <div 
                    v-for="(field, index) in exportFields"
                    :key="field.key"
                    class="field-item"
                    :class="{ selected: field.selected }"
                  >
                    <input 
                      type="checkbox"
                      :checked="field.selected"
                      @change="toggleField(field)"
                    />
                    <span class="field-label">{{ field.label }}</span>
                    <div class="order-buttons">
                      <button 
                        :disabled="index === 0"
                        @click="moveFieldUp(index)"
                      >↑</button>
                      <button 
                        :disabled="index === exportFields.length - 1"
                        @click="moveFieldDown(index)"
                      >↓</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="dialog-footer">
              <button class="cancel-btn" @click="showExportDialog = false">
                取消
              </button>
              <button 
                class="export-btn-dialog"
                :disabled="exportLoading || selectedFieldsCount === 0"
                @click="handleExport"
              >
                {{ exportLoading ? '导出中...' : '开始导出' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
    
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showImportDialog" class="dialog-overlay" @click="showImportDialog = false">
          <div class="import-dialog" @click.stop>
            <div class="dialog-header">
              <span>导入 {{ moduleLabel }} 数据</span>
              <button @click="showImportDialog = false">×</button>
            </div>
            
            <div class="dialog-body">
              <div class="file-section">
                <label>选择文件</label>
                <div class="file-upload">
                  <input 
                    type="file"
                    accept=".csv,.json"
                    @change="handleFileSelect"
                  />
                  <div class="file-info" v-if="importFile">
                    <span>{{ importFile.name }}</span>
                    <span class="file-size">{{ (importFile.size / 1024).toFixed(1) }}KB</span>
                  </div>
                </div>
                <button class="template-btn" @click="downloadTemplate">
                  下载导入模板
                </button>
              </div>
              
              <div class="mode-section">
                <label>导入模式</label>
                <div class="mode-options">
                  <label class="mode-option">
                    <input type="radio" v-model="importMode" value="create" />
                    <span>新增记录</span>
                  </label>
                  <label class="mode-option">
                    <input type="radio" v-model="importMode" value="update" />
                    <span>更新记录</span>
                  </label>
                  <label class="mode-option">
                    <input type="radio" v-model="importMode" value="merge" />
                    <span>合并记录</span>
                  </label>
                </div>
              </div>
              
              <div class="mapping-section" v-if="importMapping.length > 0">
                <label>字段映射</label>
                <div class="mapping-list">
                  <div 
                    v-for="(mapping, index) in importMapping"
                    :key="index"
                    class="mapping-item"
                  >
                    <span class="source">{{ mapping.sourceField }}</span>
                    <span class="arrow">→</span>
                    <select 
                      v-model="mapping.targetField"
                      @change="updateMapping(index, mapping.targetField)"
                    >
                      <option value="">不映射</option>
                      <option 
                        v-for="field in availableFields"
                        :key="field.key"
                        :value="field.key"
                      >
                        {{ field.label }}
                      </option>
                    </select>
                  </div>
                </div>
              </div>
              
              <div class="preview-section" v-if="importPreview.length > 0">
                <label>数据预览 (前5条)</label>
                <div class="preview-table">
                  <table>
                    <thead>
                      <tr>
                        <th v-for="key in Object.keys(importPreview[0])" :key="key">
                          {{ key }}
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, i) in importPreview" :key="i">
                        <td v-for="key in Object.keys(row)" :key="key">
                          {{ row[key] }}
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
            
            <div class="dialog-footer">
              <button class="cancel-btn" @click="showImportDialog = false">
                取消
              </button>
              <button 
                class="import-btn-dialog"
                :disabled="importLoading || !importFile"
                @click="handleImport"
              >
                {{ importLoading ? '导入中...' : '开始导入' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style lang="scss" scoped>
.import-export-buttons {
  display: inline-flex;
  gap: 8px;
}

.export-btn, .import-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  
  &:hover {
    background: #f7f8fa;
  }
}

.export-btn {
  background: #fff;
  color: #333;
}

.import-btn {
  background: #1988fa;
  color: #fff;
  border-color: #1988fa;
  
  &:hover {
    background: #0e7cd3;
  }
}

.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.export-dialog, .import-dialog {
  width: 600px;
  max-width: 90%;
  max-height: 80vh;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  
  .dialog-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px;
    border-bottom: 1px solid #ebedf0;
    
    span {
      font-size: 16px;
      font-weight: 600;
    }
    
    button {
      background: none;
      border: none;
      font-size: 20px;
      color: #969799;
      cursor: pointer;
    }
  }
  
  .dialog-body {
    flex: 1;
    padding: 16px;
    overflow-y: auto;
    
    label {
      display: block;
      font-size: 14px;
      font-weight: 600;
      color: #333;
      margin-bottom: 8px;
    }
    
    .format-section, .mode-section {
      margin-bottom: 16px;
      
      .format-options, .mode-options {
        display: flex;
        gap: 16px;
        
        .format-option, .mode-option {
          display: flex;
          align-items: center;
          gap: 4px;
          cursor: pointer;
          
          input {
            cursor: pointer;
          }
          
          span {
            font-size: 14px;
          }
        }
      }
    }
    
    .fields-section {
      margin-bottom: 16px;
      
      .fields-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;
        
        .field-actions {
          display: flex;
          gap: 8px;
          
          button {
            padding: 4px 8px;
            background: #f7f8fa;
            border: 1px solid #dcdfe6;
            border-radius: 4px;
            font-size: 12px;
            cursor: pointer;
          }
        }
      }
      
      .fields-list {
        border: 1px solid #ebedf0;
        border-radius: 4px;
        max-height: 200px;
        overflow-y: auto;
        
        .field-item {
          display: flex;
          align-items: center;
          padding: 8px 12px;
          border-bottom: 1px solid #ebedf0;
          
          &:last-child {
            border-bottom: none;
          }
          
          &.selected {
            background: #f0f7ff;
          }
          
          input {
            margin-right: 8px;
          }
          
          .field-label {
            flex: 1;
            font-size: 14px;
          }
          
          .order-buttons {
            display: flex;
            gap: 4px;
            
            button {
              padding: 2px 6px;
              background: #f7f8fa;
              border: 1px solid #dcdfe6;
              border-radius: 2px;
              font-size: 12px;
              cursor: pointer;
              
              &:disabled {
                opacity: 0.5;
                cursor: not-allowed;
              }
            }
          }
        }
      }
    }
    
    .file-section {
      margin-bottom: 16px;
      
      .file-upload {
        margin-bottom: 8px;
        
        input[type="file"] {
          width: 100%;
          padding: 12px;
          border: 1px dashed #dcdfe6;
          border-radius: 4px;
          cursor: pointer;
        }
        
        .file-info {
          display: flex;
          justify-content: space-between;
          padding: 8px 12px;
          background: #f7f8fa;
          border-radius: 4px;
          margin-top: 8px;
          
          .file-size {
            color: #969799;
          }
        }
      }
      
      .template-btn {
        padding: 8px 16px;
        background: #f7f8fa;
        border: 1px solid #dcdfe6;
        border-radius: 4px;
        font-size: 12px;
        cursor: pointer;
      }
    }
    
    .mapping-section {
      margin-bottom: 16px;
      
      .mapping-list {
        border: 1px solid #ebedf0;
        border-radius: 4px;
        max-height: 150px;
        overflow-y: auto;
        
        .mapping-item {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 8px 12px;
          border-bottom: 1px solid #ebedf0;
          
          &:last-child {
            border-bottom: none;
          }
          
          .source {
            flex: 1;
            font-size: 12px;
            color: #666;
          }
          
          .arrow {
            color: #969799;
          }
          
          select {
            width: 150px;
            padding: 4px 8px;
            border: 1px solid #dcdfe6;
            border-radius: 4px;
          }
        }
      }
    }
    
    .preview-section {
      .preview-table {
        border: 1px solid #ebedf0;
        border-radius: 4px;
        overflow: auto;
        
        table {
          width: 100%;
          border-collapse: collapse;
          
          th, td {
            padding: 8px 12px;
            border: 1px solid #ebedf0;
            font-size: 12px;
            text-align: left;
          }
          
          th {
            background: #f7f8fa;
            font-weight: 600;
          }
        }
      }
    }
  }
  
  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    padding: 16px;
    border-top: 1px solid #ebedf0;
    
    .cancel-btn {
      padding: 8px 16px;
      background: #f7f8fa;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      cursor: pointer;
    }
    
    .export-btn-dialog, .import-btn-dialog {
      padding: 8px 16px;
      background: #1988fa;
      color: #fff;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      
      &:disabled {
        background: #969799;
        cursor: not-allowed;
      }
    }
  }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>