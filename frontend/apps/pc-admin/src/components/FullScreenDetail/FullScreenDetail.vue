<template>
  <div v-if="visible" class="fullscreen-detail-overlay" @click.self="handleClose">
    <div class="fullscreen-detail">
      <!-- 顶部标题栏 -->
      <div class="detail-header">
        <div class="detail-header-left">
          <span class="detail-title">{{ title }}</span>
        </div>
        <div class="detail-header-right">
          <a-tooltip title="关闭 (Esc)">
            <a-button size="small" class="detail-close-btn" @click="handleClose">
              <template #icon><CloseOutlined /></template>
            </a-button>
          </a-tooltip>
        </div>
      </div>

      <!-- 中间内容区（独立滚动） -->
      <div class="detail-body">
        <slot />
      </div>

      <!-- 底部固定操作栏 -->
      <div v-if="showFooter" class="detail-footer">
        <a-space>
          <a-button :loading="saveLoading" type="primary" @click="handleSave">
            <template #icon><SaveOutlined /></template>
            保存
          </a-button>
          <a-button v-if="showSaveAndNew" :loading="saveLoading" @click="handleSaveAndNew">
            <template #icon><PlusOutlined /></template>
            保存并新增
          </a-button>
          <a-button @click="handleClose">关闭</a-button>
        </a-space>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, watch, computed } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { Modal } from 'ant-design-vue'
import { CloseOutlined, SaveOutlined, PlusOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  visible: boolean
  title: string
  saveLoading?: boolean
  showFooter?: boolean
  showSaveAndNew?: boolean
  /** 表单是否被修改过（脏检查） */
  dirty?: boolean
  /** 是否在关闭时确认（脏数据时自动启用） */
  confirmOnClose?: boolean
  /** 顶部偏移量，避免遮盖标签导航栏（默认48px适配标签栏高度） */
  topOffset?: number
}>()

const emit = defineEmits<{
  close: []
  save: []
  'save-and-new': []
}>()

const topOffset = computed(() => props.topOffset ?? 48)

// ── 脏检查确认 ──────────────────────────────────────
function confirmIfDirty(): Promise<boolean> {
  if (!props.dirty) return Promise.resolve(true)
  return new Promise((resolve) => {
    Modal.confirm({
      title: '未保存的更改',
      content: '您有未保存的修改，确认关闭吗？',
      okText: '确认关闭',
      cancelText: '取消',
      centered: true,
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && props.visible) {
    // 如果有脏数据，先确认再关闭
    if (props.dirty && props.confirmOnClose !== false) {
      confirmIfDirty().then((confirmed) => {
        if (confirmed) emit('close')
      })
    } else {
      emit('close')
    }
  }
}

// 路由离开守卫
onBeforeRouteLeave(async () => {
  if (props.visible && props.dirty) {
    const confirmed = await confirmIfDirty()
    return confirmed
  }
  return true
})

// 关闭弹窗时重置脏状态通知
watch(() => props.visible, (val) => {
  if (!val) {
    // visible 变为 false 时，不清除脏状态（由父组件控制）
  }
})

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

async function handleClose() {
  if (props.dirty && props.confirmOnClose !== false) {
    const confirmed = await confirmIfDirty()
    if (confirmed) emit('close')
  } else {
    emit('close')
  }
}

function handleSave() {
  emit('save')
}

function handleSaveAndNew() {
  emit('save-and-new')
}
</script>

<style scoped>
.fullscreen-detail-overlay {
  position: fixed;
  top: v-bind(topOffset + 'px');
  left: 0;
  width: 100vw;
  height: calc(100vh - v-bind(topOffset + 'px'));
  z-index: 90;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  justify-content: center;
  align-items: center;
}

.fullscreen-detail {
  width: 90vw;
  max-width: 1200px;
  height: 85vh;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.detail-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.detail-close-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  min-height: 0;
}

.detail-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 12px 20px;
  border-top: 1px solid #e8e8e8;
  flex-shrink: 0;
  background: #fafafa;
}
</style>
