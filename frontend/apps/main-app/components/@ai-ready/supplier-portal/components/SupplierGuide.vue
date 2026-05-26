<template>
  <div v-if="showGuide" class="supplier-guide">
    <!-- 引导遮罩层 -->
    <div class="guide-overlay" @click="handleOverlayClick"></div>
    
    <!-- 引导卡片 -->
    <div class="guide-card" :style="cardStyle">
      <!-- 引导头部 -->
      <div class="guide-header">
        <h3 class="guide-title">{{ currentStep.title }}</h3>
        <button class="guide-close" @click="skipGuide">
          <span>×</span>
        </button>
      </div>
      
      <!-- 引导内容 -->
      <div class="guide-content">
        <div v-html="currentStep.content"></div>
      </div>
      
      <!-- 进度指示器 -->
      <div class="guide-progress">
        <div class="progress-dots">
          <span 
            v-for="(step, index) in guideSteps" 
            :key="index"
            class="progress-dot"
            :class="{
              'active': index === currentStepIndex,
              'completed': index < currentStepIndex
            }"
            @click="goToStep(index)"
          ></span>
        </div>
        <div class="progress-text">
          步骤 {{ currentStepIndex + 1 }} / {{ guideSteps.length }}
        </div>
      </div>
      
      <!-- 操作按钮 -->
      <div class="guide-actions">
        <button 
          v-if="currentStepIndex > 0" 
          class="btn-prev" 
          @click="prevStep"
        >
          上一步
        </button>
        
        <button 
          class="btn-next" 
          @click="nextStep"
          :disabled="!canProceed"
        >
          {{ isLastStep ? '完成引导' : '下一步' }}
        </button>
        
        <button 
          class="btn-skip" 
          @click="skipGuide"
        >
          跳过所有引导
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'

// 引导步骤定义
interface GuideStep {
  id: string
  title: string
  content: string
  targetElement?: string
  position?: 'top' | 'bottom' | 'left' | 'right' | 'center'
  action?: () => void
  waitForElement?: boolean
}

// 用户引导进度
interface GuideProgress {
  completedGuides: string[]
  currentGuideId?: string
  currentStepIndex?: number
  skippedGuides: string[]
  lastAccessed: number
}

// 引导配置
const guideConfig = {
  id: 'supplier-portal-introduction',
  title: '供应商门户新手引导',
  description: '帮助您快速熟悉供应商门户的核心功能',
  steps: [
    {
      id: 'welcome',
      title: '欢迎使用供应商门户',
      content: '欢迎来到供应商门户系统！我们将引导您快速了解系统的主要功能和操作方式。',
      position: 'center'
    },
    {
      id: 'dashboard-intro',
      title: '仪表板介绍',
      content: '这里是您的仪表板，可以快速查看待处理订单、报价请求和库存状态。<br><br><strong>主要功能模块：</strong><br>• 订单管理：处理客户订单<br>• 报价管理：响应报价请求<br>• 库存管理：管理产品库存<br>• 财务管理：处理发票和对账',
      targetElement: '.dashboard-section',
      position: 'bottom'
    },
    {
      id: 'order-management',
      title: '订单管理功能',
      content: '在订单管理模块，您可以：<br><br>• 查看新订单和待处理订单<br>• 接受或拒绝订单<br>• 更新订单状态（备货中、已发货等）<br>• 上传发货信息和物流跟踪号',
      targetElement: '.order-management-link',
      position: 'right',
      action: () => {
        // 模拟点击订单管理链接
        const orderLink = document.querySelector('.order-management-link')
        if (orderLink) {
          (orderLink as HTMLElement).click()
        }
      }
    },
    {
      id: 'quotation-system',
      title: '报价管理系统',
      content: '报价管理模块帮助您：<br><br>• 查看报价请求<br>• 填写产品报价<br>• 设置报价有效期<br>• 批量报价功能<br>• 跟踪报价状态',
      targetElement: '.quotation-management-link',
      position: 'right'
    },
    {
      id: 'inventory-control',
      title: '库存管理',
      content: '库存管理功能包括：<br><br>• 添加和管理产品<br>• 设置库存数量和预警<br>• 库存同步配置<br>• 库存状态监控',
      targetElement: '.inventory-management-link',
      position: 'right'
    },
    {
      id: 'completion',
      title: '引导完成',
      content: '恭喜！您已经完成了新手引导。<br><br><strong>后续学习建议：</strong><br>• 查看详细的操作教程<br>• 访问帮助中心获取更多信息<br>• 有疑问可随时联系客服',
      position: 'center'
    }
  ]
}

// 响应式状态
const showGuide = ref(false)
const currentStepIndex = ref(0)
const guideSteps = ref<GuideStep[]>(guideConfig.steps)
const cardStyle = ref({})
const canProceed = ref(true)

// 计算属性
const currentStep = computed(() => guideSteps.value[currentStepIndex.value])
const isLastStep = computed(() => currentStepIndex.value === guideSteps.value.length - 1)

// 本地存储键名
const STORAGE_KEY = 'supplier-portal-guide-progress'

// 加载用户引导进度
const loadProgress = (): GuideProgress => {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) {
      return JSON.parse(saved)
    }
  } catch (error) {
    console.error('Failed to load guide progress:', error)
  }
  
  return {
    completedGuides: [],
    skippedGuides: [],
    lastAccessed: Date.now()
  }
}

// 保存用户引导进度
const saveProgress = (progress: GuideProgress) => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(progress))
  } catch (error) {
    console.error('Failed to save guide progress:', error)
  }
}

// 检查是否需要显示引导
const shouldShowGuide = (): boolean => {
  const progress = loadProgress()
  
  // 如果用户已经跳过这个引导
  if (progress.skippedGuides.includes(guideConfig.id)) {
    return false
  }
  
  // 如果用户已经完成这个引导
  if (progress.completedGuides.includes(guideConfig.id)) {
    return false
  }
  
  // 如果是首次登录或者7天内没有访问
  const oneWeekAgo = Date.now() - 7 * 24 * 60 * 60 * 1000
  if (progress.lastAccessed < oneWeekAgo) {
    return true
  }
  
  // 默认显示
  return true
}

// 初始化引导
const initializeGuide = () => {
  if (shouldShowGuide()) {
    showGuide.value = true
    updateCardPosition()
    
    // 更新访问时间
    const progress = loadProgress()
    progress.lastAccessed = Date.now()
    saveProgress(progress)
  }
}

// 更新卡片位置
const updateCardPosition = () => {
  const step = currentStep.value
  
  if (step.targetElement && step.position) {
    const target = document.querySelector(step.targetElement)
    if (target) {
      const rect = target.getBoundingClientRect()
      
      switch (step.position) {
        case 'top':
          cardStyle.value = {
            top: `${rect.top - 20}px`,
            left: `${rect.left}px`,
            transform: 'translateY(-100%)'
          }
          break
        case 'bottom':
          cardStyle.value = {
            top: `${rect.bottom + 20}px`,
            left: `${rect.left}px`
          }
          break
        case 'left':
          cardStyle.value = {
            top: `${rect.top}px`,
            left: `${rect.left - 20}px`,
            transform: 'translateX(-100%)'
          }
          break
        case 'right':
          cardStyle.value = {
            top: `${rect.top}px`,
            left: `${rect.right + 20}px`
          }
          break
      }
    }
  } else {
    // 居中显示
    cardStyle.value = {
      top: '50%',
      left: '50%',
      transform: 'translate(-50%, -50%)'
    }
  }
}

// 下一步
const nextStep = () => {
  if (currentStep.value.action) {
    currentStep.value.action()
  }
  
  if (currentStepIndex.value < guideSteps.value.length - 1) {
    currentStepIndex.value++
    updateCardPosition()
  } else {
    completeGuide()
  }
}

// 上一步
const prevStep = () => {
  if (currentStepIndex.value > 0) {
    currentStepIndex.value--
    updateCardPosition()
  }
}

// 跳转到指定步骤
const goToStep = (index: number) => {
  if (index >= 0 && index < guideSteps.value.length) {
    currentStepIndex.value = index
    updateCardPosition()
  }
}

// 跳过引导
const skipGuide = () => {
  const progress = loadProgress()
  if (!progress.skippedGuides.includes(guideConfig.id)) {
    progress.skippedGuides.push(guideConfig.id)
  }
  saveProgress(progress)
  showGuide.value = false
}

// 完成引导
const completeGuide = () => {
  const progress = loadProgress()
  if (!progress.completedGuides.includes(guideConfig.id)) {
    progress.completedGuides.push(guideConfig.id)
  }
  saveProgress(progress)
  showGuide.value = false
  
  // 触发完成事件
  emit('guide-completed', guideConfig.id)
}

// 处理遮罩层点击
const handleOverlayClick = (event: MouseEvent) => {
  // 阻止遮罩层点击关闭引导
  event.stopPropagation()
}

// 键盘快捷键支持
const handleKeyDown = (event: KeyboardEvent) => {
  if (!showGuide.value) return
  
  switch (event.key) {
    case 'Escape':
      skipGuide()
      break
    case 'ArrowLeft':
      prevStep()
      break
    case 'ArrowRight':
      nextStep()
      break
  }
}

// 窗口大小变化时更新位置
const handleResize = () => {
  if (showGuide.value) {
    updateCardPosition()
  }
}

// 事件发射器
const emit = defineEmits<{
  'guide-completed': [guideId: string]
}>()

// 组件挂载
onMounted(() => {
  // 延迟显示引导，确保DOM已加载
  setTimeout(() => {
    initializeGuide()
  }, 1000)
  
  // 添加事件监听器
  window.addEventListener('keydown', handleKeyDown)
  window.addEventListener('resize', handleResize)
})

// 组件卸载
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
  window.removeEventListener('resize', handleResize)
})

// 监听步骤变化
watch(currentStepIndex, () => {
  // 检查目标元素是否存在（如果需要）
  if (currentStep.value.waitForElement && currentStep.value.targetElement) {
    canProceed.value = false
    
    const checkElement = () => {
      const element = document.querySelector(currentStep.value.targetElement!)
      if (element) {
        canProceed.value = true
      } else {
        setTimeout(checkElement, 100)
      }
    }
    
    checkElement()
  } else {
    canProceed.value = true
  }
})

// 暴露方法给父组件
defineExpose({
  startGuide: () => {
    showGuide.value = true
    currentStepIndex.value = 0
    updateCardPosition()
  },
  skipGuide,
  completeGuide
})
</script>

<style scoped>
.supplier-guide {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
}

.guide-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(2px);
}

.guide-card {
  position: absolute;
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  width: 400px;
  max-width: 90vw;
  z-index: 10000;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.guide-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 20px 0 20px;
}

.guide-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.guide-close {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background-color 0.2s;
}

.guide-close:hover {
  background-color: #f5f5f5;
  color: #666;
}

.guide-content {
  padding: 20px;
  color: #555;
  line-height: 1.6;
  font-size: 14px;
  max-height: 200px;
  overflow-y: auto;
}

.guide-content strong {
  color: #333;
  font-weight: 600;
}

.guide-progress {
  padding: 0 20px 20px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.progress-dots {
  display: flex;
  gap: 8px;
}

.progress-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #e0e0e0;
  cursor: pointer;
  transition: all 0.2s;
}

.progress-dot.active {
  background-color: #1890ff;
  transform: scale(1.2);
}

.progress-dot.completed {
  background-color: #52c41a;
}

.progress-text {
  font-size: 12px;
  color: #999;
}

.guide-actions {
  display: flex;
  justify-content: space-between;
  padding: 0 20px 20px 20px;
  gap: 12px;
}

.guide-actions button {
  padding: 8px 16px;
  border-radius: 6px;
  border: none;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-prev {
  background-color: #f5f5f5;
  color: #666;
}

.btn-prev:hover {
  background-color: #e8e8e8;
}

.btn-next {
  background-color: #1890ff;
  color: white;
  flex: 1;
}

.btn-next:hover:not(:disabled) {
  background-color: #096dd9;
}

.btn-next:disabled {
  background-color: #bae7ff;
  cursor: not-allowed;
}

.btn-skip {
  background: none;
  color: #999;
  border: 1px solid #e0e0e0;
}

.btn-skip:hover {
  background-color: #f5f5f5;
  color: #666;
}
</style>