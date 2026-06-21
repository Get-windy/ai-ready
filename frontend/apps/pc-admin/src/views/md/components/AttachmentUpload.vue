<template>
  <div>
    <div class="section-title">附件</div>
    <div class="attach-toolbar">
      <a-upload
        :before-upload="handleUpload"
        :show-upload-list="false"
        :accept="accept"
        :multiple="true"
      >
        <a-button size="small" type="primary">
          <template #icon><UploadOutlined /></template>
          上传附件
        </a-button>
      </a-upload>
    </div>

    <div v-if="list.length === 0" class="attach-empty">暂无附件</div>
    <div v-else class="attach-list">
      <div v-for="item in list" :key="item.id" class="attach-item">
        <div class="attach-icon">
          <FilePdfOutlined v-if="item.fileType?.includes('pdf')" style="color:#f5222d" />
          <FileImageOutlined v-else-if="item.fileType?.startsWith('image')" style="color:#52c41a" />
          <FileWordOutlined v-else-if="item.fileType?.includes('word') || item.fileType?.includes('document')" style="color:#1890ff" />
          <FileWordOutlined v-else-if="item.fileName?.endsWith('.doc') || item.fileName?.endsWith('.docx')" style="color:#1890ff" />
          <FileExcelOutlined v-else-if="item.fileType?.includes('sheet') || item.fileName?.endsWith('.xlsx') || item.fileName?.endsWith('.xls')" style="color:#52c41a" />
          <FileOutlined v-else style="color:#8c8c8c" />
        </div>
        <div class="attach-info">
          <div class="attach-name" :title="item.fileName">{{ item.fileName }}</div>
          <div class="attach-meta">{{ formatSize(item.fileSize) }}</div>
        </div>
        <div class="attach-actions">
          <a-button type="link" size="small" @click="handlePreview(item)">预览</a-button>
          <a-popconfirm title="确定删除此附件？" @confirm="handleDelete(item.id)">
            <a-button type="link" size="small" danger>删除</a-button>
          </a-popconfirm>
        </div>
      </div>
    </div>

    <!-- 预览弹窗 -->
    <a-modal v-model:open="previewVisible" :title="previewItem?.fileName || ''" width="600px" :footer="null">
      <img v-if="previewItem?.fileType?.startsWith('image')" :src="previewItem.fileUrl" style="width:100%" />
      <iframe v-else-if="previewItem?.fileType?.includes('pdf')" :src="previewItem.fileUrl" style="width:100%;height:500px" />
      <div v-else style="text-align:center;padding:40px">
        <p>该文件类型不支持预览</p>
        <a-button type="primary" @click="handleDownload(previewItem!)">下载</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { UploadOutlined, FileOutlined, FilePdfOutlined, FileImageOutlined, FileWordOutlined, FileExcelOutlined } from '@ant-design/icons-vue'
import { partnerAttachmentApi, type PartnerAttachment } from '@/api/erp/partner'
import request from '@/utils/request'

const props = defineProps<{
  partnerId?: number
  accept?: string
}>()

const list = ref<PartnerAttachment[]>([])
const previewVisible = ref(false)
const previewItem = ref<PartnerAttachment | null>(null)

watch(() => props.partnerId, (id) => { if (id) load() })

async function load() {
  if (!props.partnerId) return
  try { list.value = await partnerAttachmentApi.getByPartner(props.partnerId) } catch { list.value = [] }
}

function formatSize(bytes: number) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1024 / 1024).toFixed(1) + 'MB'
}

async function handleUpload(file: File) {
  if (!props.partnerId) { message.warning('请先保存单位后再上传附件'); return false }
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request.post('/file/upload', formData)
    const fileUrl = typeof res === 'string' ? res : (res as any)?.url || (res as any)?.data?.url
    if (!fileUrl) { message.error('文件上传失败'); return false }

    const attachment: Partial<PartnerAttachment> = {
      partnerId: props.partnerId,
      fileName: file.name,
      fileUrl,
      fileSize: file.size,
      fileType: file.type,
      category: file.type.startsWith('image') ? 'IMAGE' : 'DOC',
    }
    await partnerAttachmentApi.create(attachment)
    message.success('上传成功')
    await load()
  } catch { message.error('上传失败') }
  return false
}

async function handleDelete(id: number) {
  try {
    await partnerAttachmentApi.delete(id)
    message.success('删除成功')
    await load()
  } catch { message.error('删除失败') }
}

function handlePreview(item: PartnerAttachment) {
  previewItem.value = item
  previewVisible.value = true
}

function handleDownload(item: PartnerAttachment) {
  window.open(item.fileUrl, '_blank')
}

onMounted(() => { if (props.partnerId) load() })

/** 外部可调用刷新 */
defineExpose({ load })
</script>

<style scoped>
.section-title { font-size: 14px; font-weight: 600; color: #262626; margin-bottom: 12px; padding-bottom: 10px; border-bottom: 1px solid #f0f0f0; }
.attach-toolbar { margin-bottom: 12px; }
.attach-empty { text-align: center; padding: 20px; color: #bfbfbf; background: #fafafa; border-radius: 6px; }
.attach-list { display: flex; flex-direction: column; gap: 6px; }
.attach-item {
  display: flex; align-items: center; gap: 12px;
  padding: 8px 12px; background: #fafafa; border-radius: 6px; border: 1px solid #f0f0f0;
}
.attach-item:hover { background: #f5f5f5; }
.attach-icon { font-size: 20px; flex-shrink: 0; }
.attach-info { flex: 1; min-width: 0; }
.attach-name { font-size: 13px; color: #262626; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.attach-meta { font-size: 12px; color: #bfbfbf; }
.attach-actions { display: flex; gap: 4px; flex-shrink: 0; }
</style>
