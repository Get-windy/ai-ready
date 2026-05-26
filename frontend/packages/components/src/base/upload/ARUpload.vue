<template>
  <div class="ar-upload">
    <div class="upload-area" :class="{ disabled: disabled }">
      <input
        ref="inputRef"
        type="file"
        :accept="accept"
        :multiple="multiple"
        :disabled="disabled"
        class="upload-input"
        @change="handleFileChange"
      />
      
      <div 
        v-if="!hasFiles"
        class="upload-trigger"
        @click="handleClick"
        @dragover.prevent="handleDragOver"
        @dragleave.prevent="handleDragLeave"
        @drop.prevent="handleDrop"
      >
        <div class="trigger-icon">
          <svg viewBox="0 0 24 24" width="32" height="32">
            <path fill="currentColor" d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z"/>
          </svg>
        </div>
        <div class="trigger-text">
          <span class="primary">{{ triggerText }}</span>
          <span class="hint">{{ hint }}</span>
        </div>
      </div>
      
      <div v-else class="upload-files">
        <div 
          v-for="(file, index) in fileList"
          :key="index"
          class="file-item"
        >
          <div class="file-preview">
            <img 
              v-if="isImage(file)"
              :src="file.url || file.preview"
              alt="preview"
            />
            <div v-else class="file-icon">
              <svg viewBox="0 0 24 24" width="24" height="24">
                <path fill="currentColor" d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm4 18H6V4h7v5h5v11z"/>
              </svg>
            </div>
          </div>
          
          <div class="file-info">
            <div class="file-name">{{ file.name }}</div>
            <div class="file-size">{{ formatSize(file.size) }}</div>
            <div v-if="file.status === 'uploading'" class="file-progress">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: file.progress + '%' }"></div>
              </div>
              <span class="progress-text">{{ file.progress }}%</span>
            </div>
            <div v-if="file.status === 'success'" class="file-status success">
              上传成功
            </div>
            <div v-if="file.status === 'error'" class="file-status error">
              上传失败
            </div>
          </div>
          
          <button 
            class="file-remove"
            @click="handleRemove(index)"
          >
            <svg viewBox="0 0 24 24" width="16" height="16">
              <path fill="currentColor" d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
            </svg>
          </button>
        </div>
        
        <div 
          v-if="fileList.length < maxCount"
          class="add-more"
          @click="handleClick"
        >
          <svg viewBox="0 0 24 24" width="24" height="24">
            <path fill="currentColor" d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
          </svg>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface UploadFile {
  file: File
  name: string
  size: number
  url?: string
  preview?: string
  status: 'pending' | 'uploading' | 'success' | 'error'
  progress: number
}

interface Props {
  accept?: string
  multiple?: boolean
  disabled?: boolean
  maxCount?: number
  maxSize?: number
  triggerText?: string
  hint?: string
  autoUpload?: boolean
  action?: string
  headers?: Record<string, string>
  data?: Record<string, any>
}

const props = withDefaults(defineProps<Props>(), {
  accept: '*',
  multiple: false,
  disabled: false,
  maxCount: 1,
  maxSize: 10 * 1024 * 1024,
  triggerText: '点击或拖拽上传',
  hint: '支持 jpg、png、pdf 格式，最大 10MB',
  autoUpload: true
})

const emit = defineEmits<{
  (e: 'change', files: UploadFile[]): void
  (e: 'success', file: UploadFile, response: any): void
  (e: 'error', file: UploadFile, error: any): void
  (e: 'remove', file: UploadFile, index: number): void
}>()

const inputRef = ref<HTMLInputElement | null>(null)
const fileList = ref<UploadFile[]>([])
const isDragging = ref(false)

const hasFiles = computed(() => fileList.value.length > 0)

const handleClick = () => {
  if (props.disabled) return
  inputRef.value?.click()
}

const handleDragOver = () => {
  if (props.disabled) return
  isDragging.value = true
}

const handleDragLeave = () => {
  isDragging.value = false
}

const handleDrop = (e: DragEvent) => {
  if (props.disabled) return
  isDragging.value = false
  
  const files = e.dataTransfer?.files
  if (files) {
    processFiles(Array.from(files))
  }
}

const handleFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (files) {
    processFiles(Array.from(files))
  }
  target.value = ''
}

const processFiles = (files: File[]) => {
  const validFiles = files.filter(file => {
    if (file.size > props.maxSize) {
      console.warn(`文件 ${file.name} 超过最大限制`)
      return false
    }
    return true
  })
  
  if (!props.multiple && validFiles.length > 0) {
    fileList.value = []
  }
  
  const remainingSlots = props.maxCount - fileList.value.length
  const filesToAdd = validFiles.slice(0, remainingSlots)
  
  filesToAdd.forEach(file => {
    const uploadFile: UploadFile = {
      file,
      name: file.name,
      size: file.size,
      status: 'pending',
      progress: 0
    }
    
    if (isImage(file)) {
      uploadFile.preview = URL.createObjectURL(file)
    }
    
    fileList.value.push(uploadFile)
    
    if (props.autoUpload && props.action) {
      uploadFile(uploadFile)
    }
  })
  
  emit('change', fileList.value)
}

const uploadFile = async (uploadFile: UploadFile) => {
  uploadFile.status = 'uploading'
  
  try {
    const formData = new FormData()
    formData.append('file', uploadFile.file)
    
    if (props.data) {
      Object.entries(props.data).forEach(([key, value]) => {
        formData.append(key, value)
      })
    }
    
    const xhr = new XMLHttpRequest()
    
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) {
        uploadFile.progress = Math.round((e.loaded / e.total) * 100)
      }
    }
    
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        uploadFile.status = 'success'
        uploadFile.url = xhr.responseText
        emit('success', uploadFile, xhr.responseText)
      } else {
        uploadFile.status = 'error'
        emit('error', uploadFile, xhr.statusText)
      }
    }
    
    xhr.onerror = () => {
      uploadFile.status = 'error'
      emit('error', uploadFile, '网络错误')
    }
    
    xhr.open('POST', props.action!)
    
    if (props.headers) {
      Object.entries(props.headers).forEach(([key, value]) => {
        xhr.setRequestHeader(key, value)
      })
    }
    
    xhr.send(formData)
  } catch (error) {
    uploadFile.status = 'error'
    emit('error', uploadFile, error)
  }
}

const handleRemove = (index: number) => {
  const file = fileList.value[index]
  if (file.preview) {
    URL.revokeObjectURL(file.preview)
  }
  fileList.value.splice(index, 1)
  emit('remove', file, index)
}

const isImage = (file: UploadFile | File): boolean => {
  const type = file instanceof File ? file.type : file.file?.type
  return type?.startsWith('image/') ?? false
}

const formatSize = (size: number): string => {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}
</script>

<style lang="scss" scoped>
.ar-upload {
  .upload-area {
    &.disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }
  
  .upload-input {
    display: none;
  }
  
  .upload-trigger {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 24px;
    background: #f7f8fa;
    border: 1px dashed #dcdfe6;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;
    
    &:hover {
      border-color: #1988fa;
      background: #f0f7ff;
    }
    
    .trigger-icon {
      color: #969799;
      margin-bottom: 8px;
    }
    
    .trigger-text {
      text-align: center;
      
      .primary {
        color: #1988fa;
        font-size: 14px;
      }
      
      .hint {
        color: #969799;
        font-size: 12px;
        margin-top: 4px;
      }
    }
  }
  
  .upload-files {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    
    .file-item {
      display: flex;
      align-items: center;
      padding: 8px;
      background: #fff;
      border: 1px solid #ebedf0;
      border-radius: 4px;
      width: 100%;
      
      .file-preview {
        width: 48px;
        height: 48px;
        border-radius: 4px;
        overflow: hidden;
        
        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
        
        .file-icon {
          width: 100%;
          height: 100%;
          display: flex;
          align-items: center;
          justify-content: center;
          background: #f7f8fa;
          color: #969799;
        }
      }
      
      .file-info {
        flex: 1;
        margin-left: 8px;
        
        .file-name {
          font-size: 14px;
          color: #333;
        }
        
        .file-size {
          font-size: 12px;
          color: #969799;
          margin-top: 2px;
        }
        
        .file-progress {
          display: flex;
          align-items: center;
          margin-top: 4px;
          
          .progress-bar {
            flex: 1;
            height: 4px;
            background: #ebedf0;
            border-radius: 2px;
            
            .progress-fill {
              height: 100%;
              background: #1988fa;
              transition: width 0.3s;
            }
          }
          
          .progress-text {
            font-size: 12px;
            color: #1988fa;
            margin-left: 8px;
          }
        }
        
        .file-status {
          font-size: 12px;
          margin-top: 4px;
          
          &.success {
            color: #07c160;
          }
          
          &.error {
            color: #f44;
          }
        }
      }
      
      .file-remove {
        padding: 4px;
        background: none;
        border: none;
        color: #969799;
        cursor: pointer;
        
        &:hover {
          color: #f44;
        }
      }
    }
    
    .add-more {
      width: 48px;
      height: 48px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f7f8fa;
      border: 1px dashed #dcdfe6;
      border-radius: 4px;
      color: #969799;
      cursor: pointer;
      
      &:hover {
        border-color: #1988fa;
        color: #1988fa;
      }
    }
  }
}
</style>