<template>
  <!--
    基础资料导入向导（对标 ql361「基本信息导入」三步向导）
      步骤1 下载模板（下载模版 + 下一步）
      步骤2 导入Excel（选择文件 + 上一步 / 下一步）
      步骤3 完成（导入结果 + 完成）
    客户 / 供应商 / 物流公司 / 其他往来单位共用同一组件，避免重复实现。
  -->
  <a-modal
    v-model:open="visible"
    :title="title"
    :width="520"
    :footer="null"
    :mask-closable="false"
    :destroy-on-close="true"
    @cancel="handleCancel"
  >
    <a-steps
      :current="step"
      size="small"
      class="import-steps"
    >
      <a-step title="下载模板" />
      <a-step title="导入Excel" />
      <a-step title="完成" />
    </a-steps>

    <!-- ═══ 步骤 1：下载模板 ═══ -->
    <div
      v-if="step === 0"
      class="step-body"
    >
      <p class="step-tip">
        1.请使用Microsoft Excel对模板进行编辑，请勿编辑首行黑体标题。
      </p>
      <p class="step-tip">
        2.可参考模板内首行黑体标题的批注信息，帮助您更正确录入数据。
      </p>
      <div class="step-center">
        <a-button
          type="link"
          :loading="downloading"
          @click="handleDownloadTemplate"
        >
          下载模版
        </a-button>
      </div>
      <div class="step-actions">
        <a-button
          type="primary"
          @click="step = 1"
        >
          下一步
        </a-button>
      </div>
    </div>

    <!-- ═══ 步骤 2：导入Excel ═══ -->
    <div
      v-else-if="step === 1"
      class="step-body"
    >
      <a-upload-dragger
        v-model:file-list="fileList"
        :before-upload="() => false"
        :max-count="1"
        accept=".xlsx,.xls"
      >
        <p class="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p class="ant-upload-text">
          点击或拖拽文件到此区域上传
        </p>
        <p class="ant-upload-hint">
          仅支持 .xlsx / .xls 格式的 Excel 文件
        </p>
      </a-upload-dragger>
      <div class="step-actions">
        <a-button @click="step = 0">
          上一步
        </a-button>
        <a-button
          type="primary"
          :loading="importing"
          @click="handleImport"
        >
          下一步
        </a-button>
        <a-button
          type="link"
          danger
          @click="handleCancel"
        >
          退出
        </a-button>
      </div>
    </div>

    <!-- ═══ 步骤 3：完成 ═══ -->
    <div
      v-else
      class="step-body"
    >
      <a-result
        :status="result && result.failure > 0 ? 'warning' : 'success'"
        :title="resultTitle"
      >
        <template #subTitle>
          <span>共 {{ result?.total || 0 }} 条，成功 {{ result?.success || 0 }} 条，失败 {{ result?.failure || 0 }} 条</span>
        </template>
      </a-result>
      <ul
        v-if="result && result.errors && result.errors.length"
        class="error-list"
      >
        <li
          v-for="(err, idx) in result.errors"
          :key="idx"
        >
          {{ err }}
        </li>
      </ul>
      <div class="step-actions">
        <a-button
          type="primary"
          @click="handleFinish"
        >
          完成
        </a-button>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { InboxOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

interface ImportResult {
  total: number
  success: number
  failure: number
  errors?: string[]
}

const props = withDefaults(defineProps<{
  open: boolean
  /** 弹窗标题（对标「基本信息导入」） */
  title?: string
  /** 模板下载接口（相对 /api） */
  templateUrl: string
  /** 模板下载参数（如 { partnerType: 'logistics' }） */
  templateParams?: Record<string, any>
  /** 模板下载文件名前缀（不含扩展名） */
  templateFileName?: string
  /** Excel 导入接口（相对 /api） */
  importUrl: string
  /** 导入参数（如 { partnerType: 'logistics' }） */
  importParams?: Record<string, any>
}>(), {
  title: '基本信息导入',
  templateParams: () => ({}),
  templateFileName: '导入模板',
  importParams: () => ({}),
})

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success', result: ImportResult | null): void
}>()

const visible = computed({
  get: () => props.open,
  set: (v: boolean) => emit('update:open', v),
})

const step = ref(0)
const downloading = ref(false)
const importing = ref(false)
const fileList = ref<any[]>([])
const result = ref<ImportResult | null>(null)

const resultTitle = computed(() => {
  if (!result.value) return '导入完成'
  return result.value.failure > 0 ? '导入完成（部分失败）' : '导入完成'
})

watch(() => props.open, (v) => {
  if (v) {
    step.value = 0
    fileList.value = []
    result.value = null
  }
})

/** 下载模板：真实 xlsx（后端生成，含首行批注说明） */
async function handleDownloadTemplate() {
  downloading.value = true
  try {
    const blob: any = await request.get(props.templateUrl, {
      responseType: 'blob',
      params: props.templateParams,
    })
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.error('模板下载失败')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${props.templateFileName}_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '模板下载失败')
  } finally {
    downloading.value = false
  }
}

/** 导入：上传 Excel → 后端真实落库 */
async function handleImport() {
  if (fileList.value.length === 0) {
    message.warning('请先选择要导入的 Excel 文件')
    return
  }
  const file = fileList.value[0]
  const formData = new FormData()
  formData.append('file', file.originFileObj || file)

  importing.value = true
  try {
    const res: any = await request.post(props.importUrl, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      params: props.importParams,
    })
    result.value = {
      total: Number(res?.total) || 0,
      success: Number(res?.success) || 0,
      failure: Number(res?.failure) || 0,
      errors: Array.isArray(res?.errors) ? res.errors : [],
    }
    step.value = 2
    emit('success', result.value)
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '导入失败')
  } finally {
    importing.value = false
  }
}

function handleFinish() {
  visible.value = false
}

function handleCancel() {
  visible.value = false
}
</script>

<style scoped>
.import-steps { margin: 4px 0 20px; }
.step-body { min-height: 240px; display: flex; flex-direction: column; }
.step-tip { margin: 0 0 8px; font-size: 13px; color: #595959; line-height: 1.6; }
.step-center { flex: 1; display: flex; align-items: center; justify-content: center; }
.step-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  padding-top: 12px;
}
.error-list {
  max-height: 120px;
  overflow-y: auto;
  margin: 0 0 8px;
  padding-left: 18px;
  color: #d4380d;
  font-size: 12px;
  line-height: 1.8;
}
</style>
