<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'

interface PrintItem {
  id: string | number
  documentNo: string
  documentType: string
  templateType: string
  businessType: string
  selected?: boolean
}

interface Props {
  visible: boolean
  items: PrintItem[]
  templateType: string
  businessType: string
  title?: string
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'print-success', results: any[]): void
  (e: 'print-error', error: any): void
}

const props = withDefaults(defineProps<Props>(), {
  title: '批量打印'
})

const emit = defineEmits<Emits>()

const selectedItems = ref<PrintItem[]>([])
const selectAll = ref(false)
const printing = ref(false)
const printProgress = ref({ current: 0, total: 0, success: 0, failed: 0 })
const printResults = ref<any[]>([])

const modalVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const selectedCount = computed(() => selectedItems.value.length)

const canPrint = computed(() => selectedItems.value.length > 0 && !printing.value)

watch(() => props.visible, (visible) => {
  if (visible) {
    selectedItems.value = []
    selectAll.value = false
    printProgress.value = { current: 0, total: 0, success: 0, failed: 0 }
    printResults.value = []
  }
})

watch(selectAll, (all) => {
  if (all) {
    selectedItems.value = [...props.items]
  } else {
    selectedItems.value = []
  }
})

const handleSelectItem = (item: PrintItem) => {
  const index = selectedItems.value.findIndex(i => i.id === item.id)
  if (index === -1) {
    selectedItems.value.push(item)
  } else {
    selectedItems.value.splice(index, 1)
  }
  selectAll.value = selectedItems.value.length === props.items.length
}

const handleBatchPrint = async () => {
  if (selectedItems.value.length === 0) {
    message.warning('请选择要打印的单据')
    return
  }

  printing.value = true
  printProgress.value.total = selectedItems.value.length
  printResults.value = []

  for (let i = 0; i < selectedItems.value.length; i++) {
    const item = selectedItems.value[i]
    printProgress.value.current = i + 1

    try {
      const result = await window.electronAPI?.print?.print?.({
        taskId: `batch-${Date.now()}-${item.id}`,
        templateType: props.templateType,
        businessId: item.id,
        businessType: props.businessType
      })

      if (result?.success) {
        printProgress.value.success++
        printResults.value.push({ item, success: true })
      } else {
        printProgress.value.failed++
        printResults.value.push({ item, success: false, error: result?.error })
      }
    } catch (error: any) {
      printProgress.value.failed++
      printResults.value.push({ item, success: false, error: error.message })
    }
  }

  printing.value = false

  if (printProgress.value.failed === 0) {
    message.success(`批量打印完成，共 ${printProgress.value.success} 个单据`)
    emit('print-success', printResults.value)
    modalVisible.value = false
  } else {
    message.warning(`打印完成，成功 ${printProgress.value.success} 个，失败 ${printProgress.value.failed} 个`)
    emit('print-error', { 
      success: printProgress.value.success, 
      failed: printProgress.value.failed,
      results: printResults.value
    })
  }
}

const handleCancel = () => {
  modalVisible.value = false
}

const getProgressPercent = computed(() => {
  if (printProgress.value.total === 0) return 0
  return Math.round((printProgress.value.current / printProgress.value.total) * 100)
})

const documentTypeColors = {
  '采购订单': '#07c160',
  '销售订单': '#1988fa',
  '入库单': '#07c160',
  '出库单': '#1988fa',
  '发票': '#ff976a',
  '送货单': '#969799'
}
</script>

<template>
  <a-modal
    v-model:open="modalVisible"
    :title="title"
    width="600px"
    :confirm-loading="printing"
    :ok-button-props="{ disabled: !canPrint }"
    @ok="handleBatchPrint"
    @cancel="handleCancel"
  >
    <div class="batch-print-content">
      <div class="selection-header">
        <a-checkbox v-model:checked="selectAll">
          全选 (共 {{ items.length }} 个)
        </a-checkbox>
        <span class="selected-count">
          已选择 {{ selectedCount }} 个
        </span>
      </div>

      <div class="items-list">
        <div 
          v-for="item in items"
          :key="item.id"
          :class="['item-row', { selected: selectedItems.some(i => i.id === item.id) }]"
          @click="handleSelectItem(item)"
        >
          <a-checkbox 
            :checked="selectedItems.some(i => i.id === item.id)"
            @click.stop
          />
          <div class="item-info">
            <span class="document-no">{{ item.documentNo }}</span>
            <span 
              class="document-type"
              :style="{ color: documentTypeColors[item.documentType] || '#666' }"
            >
              {{ item.documentType }}
            </span>
          </div>
        </div>
      </div>

      <div v-if="printing" class="progress-section">
        <a-progress 
          :percent="getProgressPercent"
          :status="printProgress.failed > 0 ? 'exception' : 'active'"
        />
        <div class="progress-stats">
          <span>正在打印: {{ printProgress.current }} / {{ printProgress.total }}</span>
          <span class="success">成功: {{ printProgress.success }}</span>
          <span class="failed">失败: {{ printProgress.failed }}</span>
        </div>
      </div>

      <div v-if="!printing && printResults.length > 0" class="results-section">
        <div class="results-header">
          <span>打印结果</span>
          <span class="success">成功: {{ printProgress.success }}</span>
          <span class="failed">失败: {{ printProgress.failed }}</span>
        </div>
        <div class="results-list">
          <div 
            v-for="result in printResults"
            :key="result.item.id"
            :class="['result-item', result.success ? 'success' : 'failed']"
          >
            <span class="result-icon">
              <svg v-if="result.success" viewBox="0 0 24 24" width="16" height="16">
                <path fill="#07c160" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" width="16" height="16">
                <path fill="#f44" d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
              </svg>
            </span>
            <span class="result-document">{{ result.item.documentNo }}</span>
            <span v-if="!result.success" class="result-error">{{ result.error }}</span>
          </div>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<style lang="scss" scoped>
.batch-print-content {
  .selection-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #ebedf0;

    .selected-count {
      font-size: 14px;
      color: #1988fa;
    }
  }

  .items-list {
    max-height: 300px;
    overflow-y: auto;

    .item-row {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      border-bottom: 1px solid #f7f8fa;
      cursor: pointer;

      &:hover {
        background: #f7f8fa;
      }

      &.selected {
        background: #e8f4ff;
      }

      .item-info {
        display: flex;
        align-items: center;
        gap: 12px;

        .document-no {
          font-size: 14px;
          color: #333;
        }

        .document-type {
          font-size: 12px;
          padding: 2px 8px;
          background: #f7f8fa;
          border-radius: 4px;
        }
      }
    }
  }

  .progress-section {
    padding: 16px 0;

    .progress-stats {
      display: flex;
      justify-content: space-between;
      margin-top: 8px;
      font-size: 12px;
      color: #666;

      .success { color: #07c160; }
      .failed { color: #f44; }
    }
  }

  .results-section {
    padding: 16px 0;

    .results-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 12px;
      font-size: 14px;
      color: #333;

      .success { color: #07c160; }
      .failed { color: #f44; }
    }

    .results-list {
      max-height: 200px;
      overflow-y: auto;

      .result-item {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px;
        border-radius: 4px;

        &.success {
          background: #e8f7e8;
        }

        &.failed {
          background: #ffe8e8;
        }

        .result-document {
          font-size: 14px;
          color: #333;
        }

        .result-error {
          font-size: 12px;
          color: #f44;
        }
      }
    }
  }
}
</style>