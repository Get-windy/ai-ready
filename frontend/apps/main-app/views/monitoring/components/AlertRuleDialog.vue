<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑告警规则' : '新建告警规则'"
    width="800px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      class="alert-rule-form"
    >
      <!-- 基本信息 -->
      <el-divider content-position="left">基本信息</el-divider>
      
      <el-form-item label="规则名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入规则名称" maxlength="50" show-word-limit />
      </el-form-item>
      
      <el-form-item label="规则描述" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="2"
          placeholder="请输入规则描述"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
      
      <el-form-item label="告警级别" prop="severity">
        <el-radio-group v-model="formData.severity">
          <el-radio-button label="critical">
            <el-icon><CircleClose /></el-icon> 严重
          </el-radio-button>
          <el-radio-button label="warning">
            <el-icon><Warning /></el-icon> 警告
          </el-radio-button>
          <el-radio-button label="info">
            <el-icon><InfoFilled /></el-icon> 信息
          </el-radio-button>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item label="规则状态" prop="status">
        <el-switch
          v-model="formData.status"
          :active-value="'enabled'"
          :inactive-value="'disabled'"
          active-text="启用"
          inactive-text="禁用"
        />
      </el-form-item>
      
      <!-- 告警条件 -->
      <el-divider content-position="left">告警条件</el-divider>
      
      <el-form-item label="监控指标" prop="metricType">
        <el-select v-model="formData.metricType" placeholder="请选择监控指标" style="width: 100%">
          <el-option-group label="订单指标">
            <el-option label="订单数量" value="ORDER_COUNT" />
            <el-option label="订单金额" value="ORDER_AMOUNT" />
            <el-option label="订单成功率" value="ORDER_SUCCESS_RATE" />
          </el-option-group>
          <el-option-group label="支付指标">
            <el-option label="支付笔数" value="PAYMENT_COUNT" />
            <el-option label="支付金额" value="PAYMENT_AMOUNT" />
            <el-option label="支付成功率" value="PAYMENT_SUCCESS_RATE" />
          </el-option-group>
          <el-option-group label="库存指标">
            <el-option label="库存总量" value="INVENTORY_TOTAL" />
            <el-option label="库存准确率" value="INVENTORY_ACCURACY" />
            <el-option label="库存预警数" value="INVENTORY_WARNING_COUNT" />
          </el-option-group>
          <el-option-group label="客户指标">
            <el-option label="活跃客户" value="CUSTOMER_ACTIVE" />
            <el-option label="客户满意度" value="CUSTOMER_SATISFACTION" />
          </el-option-group>
        </el-select>
      </el-form-item>
      
      <el-form-item label="告警条件" prop="threshold" class="condition-item">
        <div class="condition-row">
          <span class="condition-label">当</span>
          <el-select v-model="formData.operator" style="width: 100px">
            <el-option label=">" value=">" />
            <el-option label=">=" value=">=" />
            <el-option label="<" value="<" />
            <el-option label="<=" value="<=" />
            <el-option label="=" value="==" />
            <el-option label="≠" value="!=" />
          </el-select>
          <el-input-number v-model="formData.threshold" :precision="2" style="width: 150px" />
          <el-input v-model="formData.unit" placeholder="单位" style="width: 80px" />
          <span class="condition-label">时触发告警</span>
        </div>
      </el-form-item>
      
      <el-form-item label="持续时间" prop="duration">
        <el-input-number v-model="formData.duration" :min="1" :max="1440" style="width: 150px" />
        <span class="form-unit">分钟</span>
        <span class="form-hint">指标持续满足条件的时间</span>
      </el-form-item>
      
      <el-form-item label="评估周期" prop="timeRange">
        <el-input-number v-model="formData.timeRange" :min="1" :max="1440" style="width: 150px" />
        <span class="form-unit">分钟</span>
        <span class="form-hint">用于计算指标值的时间窗口</span>
      </el-form-item>
      
      <!-- 通知配置 -->
      <el-divider content-position="left">通知配置</el-divider>
      
      <el-form-item label="通知渠道" prop="notificationChannels">
        <el-checkbox-group v-model="formData.notificationChannels">
          <el-checkbox label="wecom">
            <el-icon><ChatDotRound /></el-icon> 企业微信
          </el-checkbox>
          <el-checkbox label="email">
            <el-icon><Message /></el-icon> 邮件
          </el-checkbox>
          <el-checkbox label="sms">
            <el-icon><Phone /></el-icon> 短信
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      
      <el-form-item label="通知用户" prop="notifyUsers">
        <el-select
          v-model="formData.notifyUsers"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="选择或输入用户ID"
          style="width: 100%"
        >
          <el-option label="运维团队" value="ops-team" />
          <el-option label="业务团队" value="business-team" />
          <el-option label="管理员" value="admin" />
        </el-select>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, FormInstance } from 'element-plus'
import type { AlertRule, AlertRuleFormData } from '../types/alert-rule'

interface Props {
  visible: boolean
  rule?: AlertRule
}

const props = withDefaults(defineProps<Props>(), {
  visible: false
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const formRef = ref<FormInstance>()
const submitting = ref(false)

const isEdit = computed(() => !!props.rule)

const formData = reactive<AlertRuleFormData>({
  name: '',
  description: '',
  type: 'threshold',
  severity: 'warning',
  status: 'enabled',
  metricType: 'ORDER_COUNT',
  operator: '>',
  threshold: 0,
  unit: '',
  duration: 5,
  timeRange: 60,
  notificationChannels: ['wecom']
})

const formRules = {
  name: [
    { required: true, message: '请输入规则名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入规则描述', trigger: 'blur' }
  ],
  severity: [
    { required: true, message: '请选择告警级别', trigger: 'change' }
  ],
  metricType: [
    { required: true, message: '请选择监控指标', trigger: 'change' }
  ],
  threshold: [
    { required: true, message: '请输入阈值', trigger: 'blur' }
  ],
  duration: [
    { required: true, message: '请输入持续时间', trigger: 'blur' }
  ],
  timeRange: [
    { required: true, message: '请输入评估周期', trigger: 'blur' }
  ],
  notificationChannels: [
    { required: true, message: '请选择通知渠道', trigger: 'change', type: 'array' }
  ]
}

// 监听rule变化，初始化表单数据
watch(() => props.rule, (newRule) => {
  if (newRule) {
    Object.assign(formData, {
      id: newRule.id,
      name: newRule.name,
      description: newRule.description,
      type: newRule.type,
      severity: newRule.severity,
      status: newRule.status,
      metricType: newRule.metricType,
      operator: newRule.operator,
      threshold: newRule.threshold,
      unit: newRule.unit,
      duration: newRule.duration,
      timeRange: newRule.timeRange,
      notificationChannels: newRule.notificationChannels,
      notifyUsers: newRule.notifyUsers,
      notifyGroups: newRule.notifyGroups
    })
  } else {
    // 重置表单
    Object.assign(formData, {
      name: '',
      description: '',
      type: 'threshold',
      severity: 'warning',
      status: 'enabled',
      metricType: 'ORDER_COUNT',
      operator: '>',
      threshold: 0,
      unit: '',
      duration: 5,
      timeRange: 60,
      notificationChannels: ['wecom'],
      notifyUsers: [],
      notifyGroups: []
    })
  }
}, { immediate: true })

const handleClose = () => {
  visible.value = false
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        // TODO: 调用API保存规则
        await new Promise(resolve => setTimeout(resolve, 500))
        
        ElMessage.success(isEdit.value ? '保存成功' : '创建成功')
        emit('success')
        handleClose()
      } catch (error) {
        ElMessage.error('操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}
</script>

<style scoped>
.alert-rule-form {
  max-height: 600px;
  overflow-y: auto;
}

.condition-item {
  :deep(.el-form-item__content) {
    display: flex;
    align-items: center;
  }
}

.condition-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.condition-label {
  color: #606266;
  font-size: 14px;
}

.form-unit {
  margin-left: 8px;
  color: #909399;
}

.form-hint {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
