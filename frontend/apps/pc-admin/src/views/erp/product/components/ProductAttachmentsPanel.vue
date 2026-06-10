<template>
  <div>
    <div class="panel-toolbar">
      <a-upload :before-upload="handleUpload" :show-upload-list="false" accept="image/*,.pdf,.doc,.docx">
        <a-button v-permission="'erp:product:attachment-edit'" size="small" type="primary">
          <UploadOutlined /> 上传附件
        </a-button>
      </a-upload>
    </div>
    <vxe-table :data="list" border size="small" max-height="300" align="center">
      <vxe-column type="seq" title="#" width="50" />
      <vxe-column field="category" title="分类" width="80">
        <template #default="{ row }">
          <a-tag>{{ row.category === 'IMAGE' ? '图片' : row.category === 'DOC' ? '文档' : '其他' }}</a-tag>
        </template>
      </vxe-column>
      <vxe-column field="fileName" title="文件名" />
      <vxe-column field="fileSize" title="大小" width="100">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </vxe-column>
      <vxe-column title="操作" width="80">
        <template #default="{ row }">
          <a-button v-permission="'erp:product:attachment-edit'" type="link" size="small" danger @click="debounceClick('del_' + row.id, () => handleDelete(row.id))">删除</a-button>
        </template>
      </vxe-column>
    </vxe-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import { productAttachmentApi, type ProductAttachment } from '@/api/erp/product'
import request from '@/utils/request'

const props = defineProps<{ productId: number }>()

// ── 防抖 ──
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

const list = ref<ProductAttachment[]>([])

async function load() {
  list.value = await productAttachmentApi.getByProduct(props.productId)
}

function formatSize(bytes: number) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1024 / 1024).toFixed(1) + 'MB'
}

async function handleUpload(file: File) {
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request.post('/file/upload', formData)
    const attachment: Partial<ProductAttachment> = {
      productId: props.productId,
      fileName: file.name,
      fileUrl: typeof res === 'string' ? res : (res as any).url || res,
      fileSize: file.size,
      fileType: file.type,
      category: file.type.startsWith('image') ? 'IMAGE' : 'DOC'
    }
    await productAttachmentApi.create(attachment)
    message.success('上传成功')
    await load()
  } catch { message.error('上传失败') }
  return false
}

async function handleDelete(id: number) {
  try { await productAttachmentApi.delete(id); message.success('删除成功'); await load() }
  catch { message.error('删除失败') }
}

onMounted(load)
onUnmounted(() => { debounceMap.clear() })
</script>

<style scoped>
.panel-toolbar { margin-bottom: 8px; }
</style>
