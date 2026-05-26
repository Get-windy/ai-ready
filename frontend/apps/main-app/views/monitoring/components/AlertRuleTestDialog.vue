<template>
  <el-dialog
    v-model="visible"
    title="测试告警规则"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div v-if="rule" class="test-dialog-content">
      <!-- 规则信息 -->
      <el-card class="rule-info-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>规则信息</span>
            <el-tag :type="getSeverityType(rule.severity)" size="small">
              {{ getSeverityLabel(rule.severity) }}
            </el-tag>
          </div>
        </template>
        <div class="rule-info">
          <div class="info-item">
            <span class="info-label">规则名称：</span>
            <span class="info-value">{{ rule.name }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">监控指标：</span>
            <span class="info-value">{{ getMetricLabel(rule.metricType) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">告警条件：</span>
            <span class="info-value">
              {{ getMetricLabel(rule.metricType) }} {{ rule.operator }} {{ rule.threshold }}{{ rule.unit || '' }}
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">持续时间：</span>
            <span class="info-value">{{ rule.duration }} 分钟</span>
          </div>
        </div>
      </el-card>
      
      <!-- 测试输入 -->
      <el-card class="test-input-card" shadow="never">
        <template #header>
          <span>测试数据</span>
        </template>
        <div class="test-input">
          <el-form label-width="120px">
            <el-form-item label="测试值">
              <el-input-number
                v-model="testValue"
                :precision="2"
                :step="1"
                style="width: 200px"
              />
              <span class="unit-text">{{ rule.unit || '' }}</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleTest" :loading="testing">
                <el-icon><VideoPlay /></el-icon>开始测试
              </el-button>
              <el-button @click="handleUseCurrent">
                使用当前值
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-card>
      
      <!-- 测试结果 -->
      <el-card v-if="testResult" class="test-result-card" shadow="never">
        <template #header>
          <span>测试结果</span>
        </template>
        <div class="test-result">
          <div class="result-status" :class="testResult.wouldTrigger ? 'triggered' : 'not-triggered'">
            <el-icon :size="48">
              <CircleCheck v-if="!testResult.wouldTrigger" />
              <WarningFilled v-else />
            </el-icon>
            <div class="result-text">
              <div class="result-title">
                {{ testResult.wouldTrigger ? '将会触发告警' : '不会触发告警' }}
              </div>
              <div class="result-desc">{{ testResult.message }}</div>
            </div>
          </div>
          
          <el-divider />
          
          <div class="result-details">
            <div class="detail-item">
              <span class="detail-label">当前测试值：</span>
              <span class="detail-value">{{ testResult.currentValue }}{{ rule.unit || '' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">告警阈值：</span>
              <span class="detail-value">{{ testResult.threshold }}{{ rule.unit || '' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">比较操作：</span>
              <span class="detail-value">{{ testResult.operator }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">条件判断：</span>
              <span class="detail-value" :class="testResult.wouldTrigger ? 'text-danger' : 'text-success'">
                {{ testResult.currentValue }} {{ testResult.operator }} {{ testResult.threshold }} = {{ testResult.wouldTrigger }}
              </span>
            </div>
          </div>
        </div>
      </el-card>
    </div>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { AlertRule, AlertRuleTestResult } from '../types/alert-rule'

interface Props {
  visible: boolean
  rule?: AlertRule
}

const props = withDefaults(defineProps<Props>(), {
  visible: false
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const testValue = ref(0)
const testing = ref(false)
const testResult = ref<AlertRuleTestResult | null>(null)

// 指标标签映射
const metricLabelMap: Record<string, string> = {
  ORDER_COUNT: '订单数量',
  ORDER_AMOUNT: '订单金额',
  ORDER_SUCCESS_RATE: '订单成功率',
  PAYMENT_COUNT: '支付笔数',
  PAYMENT_AMOUNT: '支付金额',
  PAYMENT_SUCCESS_RATE: '支付成功率',
  INVENTORY_TOTAL: '库存总量',
  INVENTORY_ACCURACY: '库存准确率',
  INVENTORY_WARNING_COUNT: '库存预警数',
  CUSTOMER_ACTIVE: '活跃客户',
  CUSTOMER_SATISFACTION: '客户满意度'
}

const getMetricLabel = (metricType: string): string => {
  return metricLabelMap[metricType] || metricType
}

const getSeverityType = (severity: string): string => {
  const typeMap: Record<string, string> = {
    critical: 'danger',
    warning: 'warning',
    info: 'info'
  }
  return typeMap[severity] || 'info'
}

const getSeverityLabel = (severity: string): string => {
  const labelMap: Record<string, string> = {
    critical: '严重',
    warning: '警告',
    info: '信息'
  }
  return labelMap[severity] || severity
}

// 监听rule变化，初始化测试值
watch(() => props.rule, (newRule) => {
  if (newRule) {
    // 根据规则设置一个合理的测试值
    if (newRule.operator === '<' || newRule.operator === '<=') {
      // 对于小于操作，设置一个低于阈值的值
      testValue.value = Number((newRule.threshold * 0.9).toFixed(2))
    } else if (newRule.operator === '>' || newRule.operator === '>=') {
      // 对于大于操作，设置一个高于阈值的值
      testValue.value = Number((newRule.threshold * 1.1).toFixed(2))
    } else {
      testValue.value = newRule.threshold
    }
    testResult.value = null
  }
}, { immediate: true })

const handleTest = async () => {
  if (!props.rule) return
  
  testing.value = true
  try {
    // TODO: 调用API进行测试
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // 模拟测试结果
    const wouldTrigger = evaluateCondition(
      testValue.value,
      props.rule.threshold,
      props.rule.operator
    )
    
    testResult.value = {
      wouldTrigger,
      currentValue: testValue.value,
      threshold: props.rule.threshold,
      operator: props.rule.operator,
      message: wouldTrigger 
        ? `当前值 ${testValue.value}${props.rule.unit || ''} 满足告警条件 ${props.rule.operator} ${props.rule.threshold}${props.rule.unit || ''}，将会触发告警`
        : `当前值 ${testValue.value}${props.rule.unit || ''} 不满足告警条件 ${props.rule.operator} ${props.rule.threshold}${props.rule.unit || ''}，不会触发告警`
    }
  } catch (error) {
    ElMessage.error('测试失败')
  } finally {
    testing.value = false
  }
}

const handleUseCurrent = async () => {
  if (!props.rule) return
  
  // TODO: 获取当前指标值
  // 模拟获取当前值
  const currentValue = props.rule.threshold * (Math.random() * 0.4 + 0.8)
  testValue.value = Number(currentValue.toFixed(2))
  ElMessage.success('已获取当前指标值')
}

const evaluateCondition = (value: number, threshold: number, operator: string): boolean => {
  switch (operator) {
    case '>': return value > threshold
    case '>=': return value >= threshold
    case '<': return value < threshold
    case '<=': return value <= threshold
    case '==': return value === threshold
    case '!=': return value !== threshold
    default: return false
  }
}

const handleClose = () => {
  visible.value = false
  testResult.value = null
}
</script>

<style scoped>
.test-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.rule-info-card,
.test-input-card,
.test-result-card {
  :deep(.el-card__header) {
    padding: 12px 16px;
    font-weight: 500;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rule-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item {
  display: flex;
  align-items: center;
}

.info-label {
  color: #909399;
  width: 80px;
  flex-shrink: 0;
}

.info-value {
  color: #303133;
  font-weight: 500;
}

.unit-text {
  margin-left: 8px;
  color: #909399;
}

.test-result {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-status {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 8px;
  
  &.triggered {
    background-color: #fef0f0;
    color: #f56c6c;
  }
  
  &.not-triggered {
    background-color: #f0f9ff;
    color: #67c23a;
  }
}

.result-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.result-title {
  font-size: 16px;
  font-weight: 600;
}

.result-desc {
  font-size: 14px;
  opacity: 0.8;
}

.result-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-item {
  display: flex;
  align-items: center;
}

.detail-label {
  color: #909399;
  width: 100px;
  flex-shrink: 0;
}

.detail-value {
  color: #303133;
  font-weight: 500;
}

.text-danger {
  color: #f56c6c;
}

.text-success {
  color: #67c23a;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
