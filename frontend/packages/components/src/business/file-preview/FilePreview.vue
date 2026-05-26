<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

interface FilePreviewProps {
  fileUrl: string
  fileType: 'pdf' | 'image' | 'office' | 'text'
  fileName?: string
  showToolbar?: boolean
  showDownload?: boolean
}

const props = withDefaults(defineProps<FilePreviewProps>(), {
  showToolbar: true,
  showDownload: true
})

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'download'): void
}>()

const loading = ref(true)
const error = ref('')
const currentPage = ref(1)
const totalPages = ref(1)
const scale = ref(1)
const content = ref('')
const imageLoaded = ref(false)

const isImage = computed(() => props.fileType === 'image')
const isPDF = computed(() => props.fileType === 'pdf')
const isText = computed(() => props.fileType === 'text')
const isOffice = computed(() => props.fileType === 'office')

onMounted(async () => {
  loadPreview()
})

const loadPreview = async () => {
  loading.value = true
  error.value = ''
  
  try {
    if (isImage.value) {
      await loadImage()
    } else if (isText.value) {
      await loadText()
    } else if (isPDF.value) {
      await loadPDF()
    } else if (isOffice.value) {
      await loadOffice()
    }
  } catch (err) {
    error.value = '预览加载失败'
  } finally {
    loading.value = false
  }
}

const loadImage = async () => {
  return new Promise((resolve) => {
    const img = new Image()
    img.onload = () => {
      imageLoaded.value = true
      resolve(true)
    }
    img.onerror = () => {
      error.value = '图片加载失败'
      resolve(false)
    }
    img.src = props.fileUrl
  })
}

const loadText = async () => {
  try {
    const response = await fetch(props.fileUrl)
    content.value = await response.text()
  } catch {
    error.value = '文本加载失败'
  }
}

const loadPDF = async () => {
  totalPages.value = 10
}

const loadOffice = async () => {
  content.value = 'Office文档预览需要第三方服务支持'
}

const zoomIn = () => {
  scale.value = Math.min(scale.value + 0.25, 3)
}

const zoomOut = () => {
  scale.value = Math.max(scale.value - 0.25, 0.5)
}

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

const handleDownload = () => {
  emit('download')
  
  const a = document.createElement('a')
  a.href = props.fileUrl
  a.download = props.fileName || 'file'
  a.click()
}

const handleClose = () => {
  emit('close')
}

const getFileIcon = () => {
  switch (props.fileType) {
    case 'pdf': return '📄'
    case 'image': return '🖼️'
    case 'office': return '📊'
    case 'text': return '📝'
    default: return '📁'
  }
}
</script>

<template>
  <div class="file-preview">
    <div class="preview-toolbar" v-if="showToolbar">
      <div class="toolbar-left">
        <span class="file-icon">{{ getFileIcon() }}</span>
        <span class="file-name">{{ fileName || '文件预览' }}</span>
      </div>
      
      <div class="toolbar-center" v-if="isPDF">
        <button class="nav-btn" :disabled="currentPage <= 1" @click="prevPage">
          ←
        </button>
        <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
        <button class="nav-btn" :disabled="currentPage >= totalPages" @click="nextPage">
          →
        </button>
      </div>
      
      <div class="toolbar-center" v-if="isImage || isPDF">
        <button class="zoom-btn" @click="zoomOut">−</button>
        <span class="scale-info">{{ scale.toFixed(1) }}x</span>
        <button class="zoom-btn" @click="zoomIn">+</button>
      </div>
      
      <div class="toolbar-right">
        <button class="download-btn" v-if="showDownload" @click="handleDownload">
          <svg viewBox="0 0 24 24" width="16" height="16">
            <path fill="currentColor" d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/>
          </svg>
          下载
        </button>
        <button class="close-btn" @click="handleClose">×</button>
      </div>
    </div>
    
    <div class="preview-content">
      <div class="loading-state" v-if="loading">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>
      
      <div class="error-state" v-if="error">
        <svg viewBox="0 0 24 24" width="48" height="48">
          <path fill="#f44" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
        </svg>
        <p>{{ error }}</p>
        <button @click="loadPreview">重新加载</button>
      </div>
      
      <div class="image-preview" v-if="isImage && imageLoaded" :style="{ transform: `scale(${scale})` }">
        <img :src="fileUrl" :alt="fileName" />
      </div>
      
      <div class="pdf-preview" v-if="isPDF && !loading" :style="{ transform: `scale(${scale})` }">
        <iframe :src="fileUrl" frameborder="0"></iframe>
      </div>
      
      <div class="text-preview" v-if="isText && content">
        <pre>{{ content }}</pre>
      </div>
      
      <div class="office-preview" v-if="isOffice && !loading">
        <div class="office-placeholder">
          <svg viewBox="0 0 24 24" width="64" height="64">
            <path fill="#969799" d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-1-5V3.5L18.5 9H13z"/>
          </svg>
          <p>{{ content }}</p>
          <button class="download-btn" @click="handleDownload">
            下载查看完整文档
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.file-preview {
  display: flex;
  flex-direction: column;
  background: #f7f8fa;
  border-radius: 8px;
  overflow: hidden;
}

.preview-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #ebedf0;
  
  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .file-icon {
      font-size: 20px;
    }
    
    .file-name {
      font-size: 14px;
      font-weight: 600;
      color: #333;
    }
  }
  
  .toolbar-center {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .nav-btn, .zoom-btn {
      padding: 4px 8px;
      background: #f7f8fa;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      cursor: pointer;
      
      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }
    
    .page-info, .scale-info {
      font-size: 12px;
      color: #666;
    }
  }
  
  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .download-btn {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 6px 12px;
      background: #1988fa;
      color: #fff;
      border: none;
      border-radius: 4px;
      font-size: 12px;
      cursor: pointer;
    }
    
    .close-btn {
      padding: 4px 8px;
      background: none;
      border: none;
      font-size: 20px;
      color: #969799;
      cursor: pointer;
    }
  }
}

.preview-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: auto;
  padding: 20px;
  
  .loading-state, .error-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    
    .spinner {
      width: 32px;
      height: 32px;
      border: 3px solid #ebedf0;
      border-top-color: #1988fa;
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }
    
    p {
      color: #969799;
    }
    
    button {
      padding: 8px 16px;
      background: #1988fa;
      color: #fff;
      border: none;
      border-radius: 4px;
      cursor: pointer;
    }
  }
  
  .error-state {
    button {
      background: #f44;
    }
  }
  
  .image-preview {
    transition: transform 0.2s;
    
    img {
      max-width: 100%;
      max-height: 100%;
      object-fit: contain;
    }
  }
  
  .pdf-preview {
    width: 100%;
    height: 100%;
    transition: transform 0.2s;
    
    iframe {
      width: 100%;
      height: 100%;
    }
  }
  
  .text-preview {
    width: 100%;
    height: 100%;
    background: #fff;
    border-radius: 4px;
    padding: 16px;
    overflow: auto;
    
    pre {
      font-size: 12px;
      line-height: 1.5;
      white-space: pre-wrap;
      word-wrap: break-word;
    }
  }
  
  .office-preview {
    .office-placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
      padding: 40px;
      
      p {
        color: #969799;
        font-size: 14px;
      }
      
      .download-btn {
        padding: 12px 24px;
        background: #1988fa;
        color: #fff;
        border: none;
        border-radius: 4px;
        cursor: pointer;
      }
    }
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>