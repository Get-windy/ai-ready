<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="600px"
    :before-close="handleBeforeClose"
    destroy-on-close
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="100px"
      label-position="right"
    >
      <el-form-item label="角色编码" prop="code">
        <el-input
          v-model="formData.code"
          placeholder="请输入角色编码，如：admin、user-manager"
          maxlength="50"
          show-word-limit
          :disabled="!!roleId"
        />
        <div class="form-tip">角色编码创建后不可修改，建议使用英文、数字和下划线</div>
      </el-form-item>

      <el-form-item label="角色名称" prop="name">
        <el-input
          v-model="formData.name"
          placeholder="请输入角色名称"
          maxlength="100"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="角色类型" prop="type">
        <el-select
          v-model="formData.type"
          placeholder="请选择角色类型"
          style="width: 100%"
        >
          <el-option label="系统角色" value="system" />
          <el-option label="自定义角色" value="custom" />
          <el-option label="部门角色" value="department" />
          <el-option label="项目角色" value="project" />
        </el-select>
      </el-form-item>

      <el-form-item label="角色级别" prop="level">
        <el-select
          v-model="formData.level"
          placeholder="请选择角色级别"
          style="width: 100%"
        >
          <el-option
            v-for="level in [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]"
            :key="level"
            :label="`L${level}`"
            :value="level"
          />
        </el-select>
        <div class="form-tip">数字越小级别越高，系统管理员通常为 L1</div>
      </el-form-item>

      <el-form-item label="是否默认" prop="isDefault">
        <el-switch v-model="formData.isDefault" />
        <div class="form-tip">新用户注册时是否自动分配此角色</div>
      </el-form-item>

      <el-form-item label="角色描述" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="3"
          placeholder="请输入角色描述信息"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="启用状态" prop="enabled">
        <el-switch v-model="formData.enabled" />
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRoleStore } from '../store/roleStore'

const props = defineProps<{
  modelValue: boolean
  roleId?: string | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'success': []
}>()

// Store
const roleStore = useRoleStore()

// Refs
const formRef = ref<FormInstance>()
const submitting = ref(false)

// 表单数据
const formData = ref({
  code: '',
  name: '',
  type: 'custom' as 'system' | 'custom' | 'department' | 'project',
  level: 5,
  description: '',
  enabled: true,
  isDefault: false
})

// 表单验证规则
const rules = ref<FormRules>({
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_-]{2,49}$/, message: '角色编码必须以字母开头，3-50位字母、数字、下划线或横线', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { min: 2, max: 100, message: '角色名称长度在2到100个字符之间', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择角色类型', trigger: 'change' }
  ],
  level: [
    { required: true, message: '请选择角色级别', trigger: 'change' },
    { type: 'number', min: 1, max: 10, message: '级别必须在1-10之间', trigger: 'change' }
  ],
  description: [
    { max: 500, message: '描述不能超过500个字符', trigger: 'blur' }
  ]
})

// 计算属性
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const title = computed(() => {
  return props.roleId ? '编辑角色' : '新建角色'
})

// 生命周期
watch(
  () => props.roleId,
  async (newRoleId) => {
    if (newRoleId) {
      await loadRoleData(newRoleId)
    } else {
      resetForm()
    }
  },
  { immediate: true }
)

// 方法
async function loadRoleData(roleId: string) {
  try {
    const role = await roleStore.fetchRole(roleId)
    if (role) {
      formData.value = {
        code: role.code,
        name: role.name,
        type: role.type,
        level: role.level,
        description: role.description || '',
        enabled: role.enabled,
        isDefault: role.isDefault
      }
    }
  } catch (error) {
    console.error('加载角色数据失败:', error)
    ElMessage.error('加载角色数据失败')
    visible.value = false
  }
}

function resetForm() {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  formData.value = {
    code: '',
    name: '',
    type: 'custom',
    level: 5,
    description: '',
    enabled: true,
    isDefault: false
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  try {
    // 表单验证
    const valid = await formRef.value.validate()
    if (!valid) return

    submitting.value = true

    if (props.roleId) {
      // 更新角色
      await roleStore.updateRole(props.roleId, {
        name: formData.value.name,
        type: formData.value.type,
        level: formData.value.level,
        description: formData.value.description,
        enabled: formData.value.enabled,
        isDefault: formData.value.isDefault
      })
      ElMessage.success('角色更新成功')
    } else {
      // 创建角色
      await roleStore.createRole({
        code: formData.value.code,
        name: formData.value.name,
        type: formData.value.type,
        level: formData.value.level,
        description: formData.value.description,
        enabled: formData.value.enabled,
        isDefault: formData.value.isDefault,
        permissionIds: []
      })
      ElMessage.success('角色创建成功')
    }

    // 触发成功事件
    emit('success')
    visible.value = false
  } catch (error) {
    console.error('保存角色失败:', error)
    ElMessage.error(props.roleId ? '更新角色失败' : '创建角色失败')
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  visible.value = false
}

async function handleBeforeClose(done: () => void) {
  try {
    // 检查表单是否有修改
    const isDirty = await checkFormDirty()
    if (isDirty) {
      await ElMessageBox.confirm('表单内容已修改，确定要关闭吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
    }
    done()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('关闭对话框失败:', error)
    }
  }
}

async function checkFormDirty(): Promise<boolean> {
  // 这里可以添加更复杂的脏检查逻辑
  if (!props.roleId) {
    // 新建表单，检查是否有任何字段被填写
    return Object.values(formData.value).some(value => {
      if (typeof value === 'string') return value.trim() !== ''
      if (typeof value === 'number') return value !== 5 // 默认级别是5
      if (typeof value === 'boolean') return !value // 默认启用状态是true
      return false
    })
  }
  return false
}
</script>

<style scoped>
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>