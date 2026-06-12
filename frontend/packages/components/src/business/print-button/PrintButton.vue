<script setup lang="ts">
import { ref, computed } from 'vue'

interface PrintButtonProps {
  templateType: 'order' | 'invoice' | 'contract' | 'label' | 'report'
  businessId: string | number
  businessType: string
  buttonText?: string
  buttonSize?: 'small' | 'medium' | 'large'
  showPreview?: boolean
}

const props = withDefaults(defineProps<PrintButtonProps>(), {
  buttonText: '打印',
  buttonSize: 'medium',
  showPreview: true
})

const emit = defineEmits<{
  (e: 'print-success', result: any): void
  (e: 'print-error', error: Error): void
}>()

const loading = ref(false)
const showTemplateDialog = ref(false)
const templates = ref<any[]>([])
const selectedTemplate = ref<any>(null)
const showPreviewDialog = ref(false)
const previewContent = ref('')
const printCopies = ref(1)

const buttonClass = computed(() => {
  return {
    'print-btn': true,
    [`size-${props.buttonSize}`]: true
  }
})

const loadTemplates = async () => {
  try {
    const data = await (window as any).electronAPI?.templates?.getByType?.(props.templateType)
    templates.value = data || []
    if (templates.value.length > 0) {
      selectedTemplate.value = templates.value.find(t => t.isDefault) || templates.value[0]
    }
  } catch {
    templates.value = []
  }
}

const handlePrint = async () => {
  if (templates.value.length === 0) {
    await loadTemplates()
  }
  
  if (templates.value.length === 0) {
    alert('暂无可用打印模板，请先创建模板')
    return
  }
  
  if (templates.value.length > 1 && !selectedTemplate.value) {
    showTemplateDialog.value = true
    return
  }
  
  await executePrint()
}

const executePrint = async () => {
  loading.value = true
  try {
    const result = await (window as any).electronAPI?.print?.execute?.({
      templateId: selectedTemplate.value?.id,
      templateType: props.templateType,
      businessId: props.businessId,
      businessType: props.businessType,
      copies: printCopies.value
    })
    
    emit('print-success', result)
    alert('打印任务已发送')
  } catch (err) {
    emit('print-error', err as Error)
    alert('打印失败: ' + err)
  } finally {
    loading.value = false
  }
}

const handlePreview = async () => {
  if (!selectedTemplate.value) {
    await loadTemplates()
  }
  
  loading.value = true
  try {
    const content = await (window as any).electronAPI?.print?.preview?.({
      templateId: selectedTemplate.value?.id,
      templateType: props.templateType,
      businessId: props.businessId,
      businessType: props.businessType
    })
    
    previewContent.value = content || '预览内容加载失败'
    showPreviewDialog.value = true
  } catch (err) {
    alert('预览失败: ' + err)
  } finally {
    loading.value = false
  }
}

const selectTemplate = (template: any) => {
  selectedTemplate.value = template
  showTemplateDialog.value = false
}

const handleBatchPrint = async () => {
  printCopies.value = 3
  await executePrint()
}
</script>

<template>
  <div class="print-button-wrapper">
    <button 
      :class="buttonClass"
      :disabled="loading"
      @click="handlePrint"
    >
      <svg v-if="!loading" viewBox="0 0 24 24" width="16" height="16">
        <path fill="currentColor" d="M19 8H5c-1.66 0-3 1.34-3 3v6h4v4h12v-4h4v-6c0-1.66-1.34-3-3-3zm-3 11H8v-5h8v5zm3-7c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm-1-3H6V4h12v5z"/>
      </svg>
      <span v-if="loading" class="loading-spinner"></span>
      <span>{{ loading ? '打印中...' : buttonText }}</span>
    </button>
    
    <button 
      v-if="showPreview"
      class="preview-btn"
      :disabled="loading"
      @click="handlePreview"
    >
      预览
    </button>
    
    <button 
      class="batch-btn"
      :disabled="loading"
      @click="handleBatchPrint"
    >
      批量打印
    </button>
    
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showTemplateDialog" class="template-dialog-overlay" @click="showTemplateDialog = false">
          <div class="template-dialog" @click.stop>
            <div class="dialog-header">
              <span>选择打印模板</span>
              <button @click="showTemplateDialog = false">×</button>
            </div>
            
            <div class="dialog-body">
              <div 
                v-for="template in templates"
                :key="template.id"
                class="template-item"
                :class="{ selected: selectedTemplate?.id === template.id }"
                @click="selectTemplate(template)"
              >
                <span class="template-name">{{ template.name }}</span>
                <span v-if="template.isDefault" class="default-tag">默认</span>
              </div>
            </div>
            
            <div class="dialog-footer">
              <button class="cancel-btn" @click="showTemplateDialog = false">
                取消
              </button>
              <button class="confirm-btn" @click="executePrint">
                打印
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
    
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showPreviewDialog" class="preview-dialog-overlay" @click="showPreviewDialog = false">
          <div class="preview-dialog" @click.stop>
            <div class="dialog-header">
              <span>打印预览</span>
              <button @click="showPreviewDialog = false">×</button>
            </div>
            
            <div class="dialog-body">
              <div class="preview-content">
                {{ previewContent }}
              </div>
            </div>
            
            <div class="dialog-footer">
              <div class="copies-setting">
                <label>打印份数:</label>
                <input 
                  v-model.number="printCopies"
                  type="number"
                  min="1"
                  max="10"
                />
              </div>
              <button class="print-btn-dialog" @click="executePrint">
                打印
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style lang="scss" scoped>
.print-button-wrapper {
  display: inline-flex;
  gap: 8px;
  align-items: center;
}

.print-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: #1988fa;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  
  &:disabled {
    background: #969799;
    cursor: not-allowed;
  }
  
  &.size-small {
    padding: 4px 8px;
    font-size: 12px;
  }
  
  &.size-medium {
    padding: 8px 16px;
    font-size: 14px;
  }
  
  &.size-large {
    padding: 12px 24px;
    font-size: 16px;
  }
  
  .loading-spinner {
    width: 14px;
    height: 14px;
    border: 2px solid #fff;
    border-top-color: transparent;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }
}

.preview-btn, .batch-btn {
  padding: 8px 12px;
  background: #f7f8fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  
  &:disabled {
    background: #ebedf0;
    cursor: not-allowed;
  }
}

.batch-btn {
  background: #07c160;
  color: #fff;
  border: none;
}

.template-dialog-overlay, .preview-dialog-overlay {
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

.template-dialog, .preview-dialog {
  width: 400px;
  max-width: 90%;
  background: #fff;
  border-radius: 8px;
  
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
    padding: 16px;
    
    .template-item {
      padding: 12px;
      border: 1px solid #ebedf0;
      border-radius: 4px;
      margin-bottom: 8px;
      cursor: pointer;
      
      &:hover {
        border-color: #1988fa;
      }
      
      &.selected {
        border-color: #1988fa;
        background: #f0f7ff;
      }
      
      .template-name {
        font-size: 14px;
      }
      
      .default-tag {
        padding: 2px 6px;
        background: #07c160;
        color: #fff;
        font-size: 10px;
        border-radius: 4px;
        margin-left: 8px;
      }
    }
    
    .preview-content {
      padding: 20px;
      background: #f7f8fa;
      border-radius: 4px;
      min-height: 200px;
      font-size: 12px;
      color: #666;
    }
  }
  
  .dialog-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px;
    border-top: 1px solid #ebedf0;
    
    .copies-setting {
      display: flex;
      align-items: center;
      gap: 8px;
      
      label {
        font-size: 12px;
        color: #666;
      }
      
      input {
        width: 60px;
        padding: 6px 8px;
        border: 1px solid #dcdfe6;
        border-radius: 4px;
      }
    }
    
    .cancel-btn {
      padding: 8px 16px;
      background: #f7f8fa;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      cursor: pointer;
    }
    
    .confirm-btn, .print-btn-dialog {
      padding: 8px 16px;
      background: #1988fa;
      color: #fff;
      border: none;
      border-radius: 4px;
      cursor: pointer;
    }
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>