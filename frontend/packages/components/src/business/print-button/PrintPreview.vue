<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { message } from 'ant-design-vue'

interface Props {
  visible: boolean
  templateType: string
  businessId: string | number
  businessType: string
  title?: string
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'print'): void
  (e: 'close'): void
}

const props = withDefaults(defineProps<Props>(), {
  title: '打印预览'
})

const emit = defineEmits<Emits>()

const previewContent = ref<string>('')
const previewLoading = ref(false)
const previewError = ref<string>('')
const zoomLevel = ref(100)
const paperSize = ref('A4')
const orientation = ref<'portrait' | 'landscape'>('portrait')

const modalVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const paperSizes = [
  { value: 'A4', label: 'A4 (210×297mm)' },
  { value: 'A5', label: 'A5 (148×210mm)' },
  { value: 'Letter', label: 'Letter (216×279mm)' },
  { value: 'Custom', label: '自定义' }
]

const zoomLevels = [50, 75, 100, 125, 150, 200]

watch(() => props.visible, async (visible) => {
  if (visible) {
    zoomLevel.value = 100
    paperSize.value = 'A4'
    orientation.value = 'portrait'
    previewError.value = ''
    await loadPreview()
  }
})

onMounted(() => {
  if (props.visible) {
    loadPreview()
  }
})

const loadPreview = async () => {
  previewLoading.value = true
  previewError.value = ''

  try {
    const response = await fetch(`/api/v1/print/templates/preview`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        templateType: props.templateType,
        businessId: props.businessId,
        businessType: props.businessType,
        paperSize: paperSize.value,
        orientation: orientation.value
      })
    })

    const data = await response.json()
    if (data.code === 200) {
      previewContent.value = data.data?.content || generateMockPreview()
    } else {
      previewError.value = data.message || '加载预览失败'
      previewContent.value = generateMockPreview()
    }
  } catch (error: any) {
    previewError.value = error.message
    previewContent.value = generateMockPreview()
  } finally {
    previewLoading.value = false
  }
}

const generateMockPreview = (): string => {
  return `
    <div style="font-family: 'Microsoft YaHei', Arial, sans-serif; padding: 20px; max-width: 800px;">
      <div style="text-align: center; margin-bottom: 30px;">
        <h1 style="font-size: 24px; color: #333; margin-bottom: 10px;">销售订单</h1>
        <p style="font-size: 14px; color: #666;">订单编号: SO20240115001</p>
      </div>
      
      <div style="display: flex; justify-content: space-between; margin-bottom: 20px;">
        <div>
          <p style="font-size: 12px; color: #999;">客户信息</p>
          <p style="font-size: 14px; color: #333; margin-top: 5px;">客户名称: 示例客户A</p>
          <p style="font-size: 14px; color: #333;">联系电话: 13800138000</p>
          <p style="font-size: 14px; color: #333;">地址: 北京市朝阳区xxx路xxx号</p>
        </div>
        <div style="text-align: right;">
          <p style="font-size: 12px; color: #999;">订单信息</p>
          <p style="font-size: 14px; color: #333; margin-top: 5px;">订单日期: 2024-01-15</p>
          <p style="font-size: 14px; color: #333;">交货日期: 2024-01-20</p>
          <p style="font-size: 14px; color: #333;">销售员: 张三</p>
        </div>
      </div>
      
      <table style="width: 100%; border-collapse: collapse; margin-bottom: 20px;">
        <thead>
          <tr style="background: #f7f8fa;">
            <th style="padding: 10px; border: 1px solid #ebedf0; text-align: left;">商品编码</th>
            <th style="padding: 10px; border: 1px solid #ebedf0; text-align: left;">商品名称</th>
            <th style="padding: 10px; border: 1px solid #ebedf0; text-align: right;">数量</th>
            <th style="padding: 10px; border: 1px solid #ebedf0; text-align: right;">单价</th>
            <th style="padding: 10px; border: 1px solid #ebedf0; text-align: right;">金额</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td style="padding: 10px; border: 1px solid #ebedf0;">SP001</td>
            <td style="padding: 10px; border: 1px solid #ebedf0;">优质大米</td>
            <td style="padding: 10px; border: 1px solid #ebedf0; text-align: right;">50</td>
            <td style="padding: 10px; border: 1px solid #ebedf0; text-align: right;">¥100.00</td>
            <td style="padding: 10px; border: 1px solid #ebedf0; text-align: right;">¥5,000.00</td>
          </tr>
          <tr>
            <td style="padding: 10px; border: 1px solid #ebedf0;">SP002</td>
            <td style="padding: 10px; border: 1px solid #ebedf0;">食用油</td>
            <td style="padding: 10px; border: 1px solid #ebedf0; text-align: right;">20</td>
            <td style="padding: 10px; border: 1px solid #ebedf0; text-align: right;">¥80.00</td>
            <td style="padding: 10px; border: 1px solid #ebedf0; text-align: right;">¥1,600.00</td>
          </tr>
        </tbody>
        <tfoot>
          <tr style="background: #f7f8fa;">
            <td colspan="4" style="padding: 10px; border: 1px solid #ebedf0; text-align: right; font-weight: bold;">合计金额:</td>
            <td style="padding: 10px; border: 1px solid #ebedf0; text-align: right; font-weight: bold; color: #f44;">¥6,600.00</td>
          </tr>
        </tfoot>
      </table>
      
      <div style="margin-bottom: 30px;">
        <p style="font-size: 12px; color: #999;">备注</p>
        <p style="font-size: 14px; color: #333; margin-top: 5px;">请按时交货，如有问题请联系销售员。</p>
      </div>
      
      <div style="display: flex; justify-content: space-between; margin-top: 50px;">
        <div>
          <p style="font-size: 12px; color: #999;">销售方签字</p>
          <div style="width: 150px; height: 60px; border: 1px dashed #dcdfe6; margin-top: 10px;"></div>
        </div>
        <div>
          <p style="font-size: 12px; color: #999;">客户方签字</p>
          <div style="width: 150px; height: 60px; border: 1px dashed #dcdfe6; margin-top: 10px;"></div>
        </div>
      </div>
    </div>
  `
}

const handleZoomIn = () => {
  if (zoomLevel.value < 200) {
    zoomLevel.value += 25
  }
}

const handleZoomOut = () => {
  if (zoomLevel.value > 50) {
    zoomLevel.value -= 25
  }
}

const handlePaperSizeChange = () => {
  loadPreview()
}

const handleOrientationChange = () => {
  loadPreview()
}

const handlePrint = () => {
  emit('print')
  message.success('正在发送打印任务...')
}

const handleClose = () => {
  emit('close')
  modalVisible.value = false
}

const previewStyle = computed(() => ({
  transform: `scale(${zoomLevel.value / 100})`,
  transformOrigin: 'top center',
  width: orientation.value === 'portrait' ? '210mm' : '297mm',
  minHeight: orientation.value === 'portrait' ? '297mm' : '210mm'
}))
</script>

<template>
  <a-modal
    v-model:open="modalVisible"
    :title="title"
    width="900px"
    :footer="null"
    @cancel="handleClose"
  >
    <div class="print-preview-content">
      <div class="toolbar">
        <div class="toolbar-left">
          <a-select 
            v-model:value="paperSize"
            :options="paperSizes"
            style="width: 150px"
            @change="handlePaperSizeChange"
          />
          <a-radio-group 
            v-model:value="orientation"
            button-style="solid"
            @change="handleOrientationChange"
          >
            <a-radio-button value="portrait">纵向</a-radio-button>
            <a-radio-button value="landscape">横向</a-radio-button>
          </a-radio-group>
        </div>
        
        <div class="toolbar-center">
          <a-button-group>
            <a-button @click="handleZoomOut" :disabled="zoomLevel <= 50">
              <template #icon>
                <svg viewBox="0 0 24 24" width="14" height="14">
                  <path fill="currentColor" d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 7 9.5 7 14 9.01 14 9.5 11.99 14 9.5 14z"/>
                </svg>
              </template>
              缩小
            </a-button>
            <a-button disabled>
              {{ zoomLevel }}%
            </a-button>
            <a-button @click="handleZoomIn" :disabled="zoomLevel >= 200">
              <template #icon>
                <svg viewBox="0 0 24 24" width="14" height="14">
                  <path fill="currentColor" d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 7 9.5 7 14 9.01 14 9.5 11.99 14 9.5 14zM13 7h-2v2H9v2h2v2h2v-2h2V9h-2z"/>
                </svg>
              </template>
              放大
            </a-button>
          </a-button-group>
        </div>
        
        <div class="toolbar-right">
          <a-button type="primary" @click="handlePrint">
            <template #icon>
              <svg viewBox="0 0 24 24" width="14" height="14">
                <path fill="currentColor" d="M19 8H5c-1.66 0-3 1.34-3 3v6h4v4h12v-4h4v-6c0-1.66-1.34-3-3-3zm-3 11H8v-5h8v5zm3-7c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm-1-9H6v4h12V3z"/>
              </svg>
            </template>
            打印
          </a-button>
        </div>
      </div>

      <div class="preview-container">
        <div v-if="previewLoading" class="loading-state">
          <a-spin size="large" />
          <p>正在加载预览...</p>
        </div>

        <div v-else-if="previewError" class="error-state">
          <a-result
            status="error"
            :title="previewError"
            sub-title="无法加载打印预览"
          >
            <template #extra>
              <a-button type="primary" @click="loadPreview">重新加载</a-button>
            </template>
          </a-result>
        </div>

        <div v-else class="preview-paper" :style="previewStyle">
          <div class="preview-content" v-html="previewContent"></div>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<style lang="scss" scoped>
.print-preview-content {
  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    background: #f7f8fa;
    border-bottom: 1px solid #ebedf0;

    .toolbar-left, .toolbar-center, .toolbar-right {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }

  .preview-container {
    height: 600px;
    overflow: auto;
    background: #e8e8e8;
    padding: 20px;
    display: flex;
    justify-content: center;

    .loading-state, .error-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
    }

    .preview-paper {
      background: #fff;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
      padding: 20mm;
      transition: transform 0.2s;

      .preview-content {
        font-family: 'Microsoft YaHei', Arial, sans-serif;
        color: #333;
        line-height: 1.6;
      }
    }
  }
}
</style>