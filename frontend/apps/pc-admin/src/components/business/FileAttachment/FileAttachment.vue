<template>
  <div class="file-attachment">
    <div class="file-attachment-header">
      <span class="file-attachment-title">
        <PaperClipOutlined /> 附件 ({{ fileList.length }})
      </span>
      <a-space>
        <a-upload
          :multiple="multiple"
          :accept="acceptTypes"
          :before-upload="handleBeforeUpload"
          :show-upload-list="false"
          :disabled="readonly"
        >
          <a-button size="small" type="primary" ghost :disabled="readonly">
            <template #icon><UploadOutlined /></template>
            上传附件
          </a-button>
        </a-upload>
      </a-space>
    </div>

    <!-- 上传进度 -->
    <div v-if="uploadProgress > 0 && uploadProgress < 100" class="upload-progress">
      <a-progress :percent="uploadProgress" size="small" />
    </div>

    <!-- 文件列表 -->
    <div v-if="fileList.length > 0" class="file-list">
      <div
        v-for="(file, index) in displayList"
        :key="file.id || file.uid"
        class="file-item"
        :class="{ 'file-item-uploading': file.status === 'uploading' }"
      >
        <!-- 文件图标 -->
        <div class="file-icon">
          <component :is="getFileIcon(file)" />
        </div>

        <!-- 文件信息 -->
        <div class="file-info">
          <div class="file-name">
            <span :title="file.fileName || file.name">{{ file.fileName || file.name }}</span>
            <a-tag v-if="file.status === 'uploading'" color="processing" size="small">上传中</a-tag>
            <a-tag v-if="file.status === 'error'" color="error" size="small">失败</a-tag>
          </div>
          <div class="file-meta">
            <span v-if="file.fileSize">{{ formatSize(file.fileSize) }}</span>
            <span v-if="file.uploadTime">{{ formatDate(file.uploadTime) }}</span>
            <span v-if="file.uploader">{{ file.uploader }}</span>
          </div>
        </div>

        <!-- 操作 -->
        <div class="file-actions">
          <a-tooltip title="预览">
            <a-button type="link" size="small" @click="handlePreview(file)">
              <template #icon><EyeOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip title="下载">
            <a-button type="link" size="small" @click="handleDownload(file)">
              <template #icon><DownloadOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-popconfirm
            v-if="!readonly"
            title="确定删除此附件？"
            @confirm="handleDelete(file, index)"
          >
            <a-tooltip title="删除">
              <a-button type="link" size="small" danger>
                <template #icon><DeleteOutlined /></template>
              </a-button>
            </a-tooltip>
          </a-popconfirm>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="file-empty">
      <PaperClipOutlined class="file-empty-icon" />
      <p>暂无附件</p>
    </div>

    <!-- 文件预览 Modal -->
    <a-modal
      v-model:open="previewVisible"
      :title="previewFile?.fileName || '文件预览'"
      :width="previewWidth"
      :footer="null"
      :destroy-on-close="true"
    >
      <!-- 图片预览 -->
      <img
        v-if="previewType === 'image'"
        :src="previewUrl"
        class="preview-image"
        alt="预览"
      />
      <!-- PDF预览 -->
      <iframe
        v-else-if="previewType === 'pdf'"
        :src="previewUrl"
        class="preview-iframe"
      />
      <!-- 其他文件 -->
      <div v-else class="preview-unsupported">
        <FileOutlined class="preview-unsupported-icon" />
        <p>该文件类型暂不支持在线预览</p>
        <a-button type="primary" @click="handleDownload(previewFile)">
          <template #icon><DownloadOutlined /></template>
          下载文件
        </a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  PaperClipOutlined,
  UploadOutlined,
  EyeOutlined,
  DownloadOutlined,
  DeleteOutlined,
  FileOutlined,
  FilePdfOutlined,
  FileImageOutlined,
  FileExcelOutlined,
  FileWordOutlined,
  FileZipOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'

export interface AttachmentFile {
  id?: number
  uid?: string
  fileName?: string
  name?: string
  fileSize?: number
  fileType?: string
  fileUrl?: string
  uploadTime?: string
  uploader?: string
  status?: 'done' | 'uploading' | 'error'
  bizType?: string
  bizId?: number
  [key: string]: any
}

const props = withDefaults(defineProps<{
  fileList?: AttachmentFile[]
  readonly?: boolean
  multiple?: boolean
  maxSize?: number // MB
  acceptTypes?: string
  bizType?: string
  bizId?: number
}>(), {
  fileList: () => [],
  readonly: false,
  multiple: true,
  maxSize: 10,
  acceptTypes: '*/*'
})

const emit = defineEmits<{
  'upload': [file: File]
  'delete': [file: AttachmentFile]
  'preview': [file: AttachmentFile]
  'download': [file: AttachmentFile]
  'update:fileList': [list: AttachmentFile[]]
}>()

const uploadProgress = ref(0)
const previewVisible = ref(false)
const previewFile = ref<AttachmentFile | null>(null)
const previewUrl = ref('')
const previewType = ref<'image' | 'pdf' | 'other'>('other')

const displayList = computed(() => props.fileList)

const previewWidth = computed(() => {
  if (previewType.value === 'image') return 800
  if (previewType.value === 'pdf') return 900
  return 500
})

const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/svg+xml']
const ALLOWED_PDF_TYPES = ['application/pdf']
const ALLOWED_EXCEL_TYPES = ['application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'application/vnd.ms-excel']
const ALLOWED_WORD_TYPES = ['application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'application/msword']

function getFileIcon(file: AttachmentFile) {
  const type = file.fileType || ''
  if (ALLOWED_IMAGE_TYPES.includes(type) || /\.(jpg|jpeg|png|gif|webp|svg)$/i.test(file.fileName || '')) {
    return FileImageOutlined
  }
  if (ALLOWED_PDF_TYPES.includes(type) || /\.pdf$/i.test(file.fileName || '')) {
    return FilePdfOutlined
  }
  if (ALLOWED_EXCEL_TYPES.includes(type) || /\.(xlsx|xls|csv)$/i.test(file.fileName || '')) {
    return FileExcelOutlined
  }
  if (ALLOWED_WORD_TYPES.includes(type) || /\.(docx|doc)$/i.test(file.fileName || '')) {
    return FileWordOutlined
  }
  if (/\.(zip|rar|7z|tar|gz)$/i.test(file.fileName || '')) {
    return FileZipOutlined
  }
  return FileOutlined
}

function formatSize(bytes: number): string {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm')
}

function handleBeforeUpload(file: File): boolean {
  // 大小检查
  if (file.size > props.maxSize * 1024 * 1024) {
    message.warning(`文件大小不能超过 ${props.maxSize}MB`)
    return false
  }
  // 类型检查
  if (props.acceptTypes !== '*/*') {
    const acceptList = props.acceptTypes.split(',')
    const fileType = file.type || file.name.split('.').pop() || ''
    if (!acceptList.some(t => fileType.includes(t.replace('/*', '/').replace('*', '')))) {
      message.warning('不支持的文件类型')
      return false
    }
  }
  emit('upload', file)
  // 模拟上传进度
  simulateUpload()
  return false // 阻止默认上传
}

function simulateUpload() {
  uploadProgress.value = 0
  const timer = setInterval(() => {
    uploadProgress.value += Math.random() * 30
    if (uploadProgress.value >= 100) {
      uploadProgress.value = 100
      clearInterval(timer)
      setTimeout(() => { uploadProgress.value = 0 }, 500)
    }
  }, 200)
}

function handlePreview(file: AttachmentFile) {
  const type = file.fileType || ''
  const fileName = file.fileName || file.name || ''

  if (ALLOWED_IMAGE_TYPES.includes(type) || /\.(jpg|jpeg|png|gif|webp|svg)$/i.test(fileName)) {
    previewType.value = 'image'
  } else if (ALLOWED_PDF_TYPES.includes(type) || /\.pdf$/i.test(fileName)) {
    previewType.value = 'pdf'
  } else {
    previewType.value = 'other'
  }

  previewFile.value = file
  previewUrl.value = file.fileUrl || ''
  previewVisible.value = true
  emit('preview', file)
}

function handleDownload(file: AttachmentFile) {
  if (file.fileUrl) {
    const a = document.createElement('a')
    a.href = file.fileUrl
    a.download = file.fileName || file.name || 'download'
    a.click()
  } else {
    emit('download', file)
  }
}

function handleDelete(file: AttachmentFile, index: number) {
  const newList = [...props.fileList]
  newList.splice(index, 1)
  emit('update:fileList', newList)
  emit('delete', file)
  message.success('附件已删除')
}

defineExpose({
  uploadProgress,
  displayList
})
</script>

<style scoped>
.file-attachment {
  border: 1px dashed var(--color-border-base, #d9d9d9);
  border-radius: 6px;
  padding: 12px;
  background: var(--color-bg-container, #fff);
}

.file-attachment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.file-attachment-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text, #333);
}

.upload-progress {
  margin-bottom: 8px;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid var(--color-border-secondary, #f0f0f0);
  border-radius: 4px;
  transition: background 0.15s;
}

.file-item:hover {
  background: var(--color-bg-layout, #fafafa);
}

.file-item-uploading {
  opacity: 0.7;
}

.file-icon {
  font-size: 24px;
  color: var(--color-primary, #1890ff);
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

/* 文件类型颜色 */
.file-item :deep(.anticon-file-pdf) { color: #f5222d; }
.file-item :deep(.anticon-file-image) { color: #1890ff; }
.file-item :deep(.anticon-file-excel) { color: #52c41a; }
.file-item :deep(.anticon-file-word) { color: #2b579a; }
.file-item :deep(.anticon-file-zip) { color: #faad14; }

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
}

.file-name span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: var(--color-text-tertiary, #999);
  margin-top: 2px;
}

.file-actions {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
}

.file-empty {
  text-align: center;
  padding: 16px;
  color: var(--color-text-tertiary, #999);
}

.file-empty-icon {
  font-size: 24px;
  opacity: 0.4;
  margin-bottom: 4px;
}

.file-empty p {
  margin: 0;
  font-size: 12px;
}

/* 预览 */
.preview-image {
  max-width: 100%;
  max-height: 70vh;
  display: block;
  margin: 0 auto;
}

.preview-iframe {
  width: 100%;
  height: 70vh;
  border: none;
}

.preview-unsupported {
  text-align: center;
  padding: 40px;
  color: var(--color-text-tertiary, #999);
}

.preview-unsupported-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.3;
}
</style>
