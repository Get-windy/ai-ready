<template>
  <ErrorBoundary>
    <PageContainer title="员工档案">
      <template #extra>
        <a-button type="text" @click="handleBack">
          <template #icon><ArrowLeftOutlined /></template>返回
        </a-button>
      </template>

      <div class="form-scroll-area">
        <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
          <!-- 基本信息 -->
          <div class="section-card">
            <div class="section-title">基本信息</div>
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="员工编号" name="employeeNo">
                  <a-input v-model:value="form.employeeNo" placeholder="自动生成" :disabled="!!editingId" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="员工姓名" name="employeeName" required>
                  <a-input v-model:value="form.employeeName" placeholder="请输入" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="性别">
                  <a-select v-model:value="form.gender">
                    <a-select-option :value="1">男</a-select-option>
                    <a-select-option :value="2">女</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="出生日期">
                  <a-date-picker v-model:value="form.birthDate" style="width:100%" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="手机号">
                  <a-input v-model:value="form.phone" placeholder="请输入" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="邮箱">
                  <a-input v-model:value="form.email" placeholder="请输入" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="身份证号">
                  <a-input v-model:value="form.idCard" placeholder="请输入" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="学历">
                  <a-select v-model:value="form.education">
                    <a-select-option :value="3">高中</a-select-option>
                    <a-select-option :value="4">大专</a-select-option>
                    <a-select-option :value="5">本科</a-select-option>
                    <a-select-option :value="6">硕士</a-select-option>
                    <a-select-option :value="7">博士</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="毕业院校">
                  <a-input v-model:value="form.school" placeholder="请输入" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="专业">
                  <a-input v-model:value="form.major" placeholder="请输入" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="头像">
                  <a-upload :before-upload="handleAvatarUpload" :show-upload-list="false" accept="image/*">
                    <a-avatar v-if="form.avatarUrl" :src="form.avatarUrl" :size="64" />
                    <a-avatar v-else icon="user" :size="64" style="background:#eee;cursor:pointer" />
                  </a-upload>
                </a-form-item>
              </a-col>
            </a-row>
          </div>

          <!-- 入职信息 -->
          <div class="section-card">
            <div class="section-title">入职信息</div>
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="入职日期" name="hireDate" required>
                  <a-date-picker v-model:value="form.hireDate" style="width:100%" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="员工类型">
                  <a-select v-model:value="form.employeeType">
                    <a-select-option :value="1">全职</a-select-option>
                    <a-select-option :value="2">兼职</a-select-option>
                    <a-select-option :value="3">实习</a-select-option>
                    <a-select-option :value="4">外包</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="状态">
                  <a-tag v-if="editingId" :color="EMPLOYEE_STATUS_MAP[form.status]?.color">
                    {{ EMPLOYEE_STATUS_MAP[form.status]?.text }}
                  </a-tag>
                  <span v-else>-</span>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="紧急联系人">
                  <a-input v-model:value="form.emergencyContact" placeholder="请输入" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="紧急电话">
                  <a-input v-model:value="form.emergencyPhone" placeholder="请输入" />
                </a-form-item>
              </a-col>
            </a-row>
          </div>

          <!-- 备注 -->
          <div class="section-card">
            <div class="section-title">其他</div>
            <a-form-item label="备注">
              <a-textarea v-model:value="form.remark" :rows="3" placeholder="请输入备注" />
            </a-form-item>
          </div>
        </a-form>

        <div class="form-footer">
          <a-space>
            <a-button @click="handleBack">取消</a-button>
            <a-button :loading="saving" type="primary" @click="handleSubmit">保存</a-button>
          </a-space>
        </div>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { hrEmployeeApi, EMPLOYEE_STATUS_MAP } from '@/api/hr'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const saving = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  employeeNo: '', employeeName: '', gender: 1, birthDate: null as any,
  phone: '', email: '', idCard: '', education: 5,
  school: '', major: '', avatarUrl: '',
  hireDate: null as any, employeeType: 1, status: 2,
  emergencyContact: '', emergencyPhone: '', remark: '',
})

const rules: Record<string, any> = {
  employeeName: [{ required: true, message: '请输入员工姓名', trigger: 'blur' }],
  hireDate: [{ required: true, message: '请选择入职日期', trigger: 'change' }],
}

async function loadData(id: number) {
  try {
    const res = await hrEmployeeApi.getById(id)
    Object.assign(form, res)
  } catch { message.error('加载失败') }
}

async function handleAvatarUpload(file: File): Promise<boolean> {
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await request.post('/file/upload', formData)
    form.avatarUrl = typeof res === 'string' ? res : (res as any)?.url || (res as any)?.data?.url
    message.success('上传成功')
  } catch { message.error('上传失败') }
  return false
}

async function handleSubmit() {
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    if (editingId.value) {
      await hrEmployeeApi.update(editingId.value, form as any)
      message.success('更新成功')
    } else {
      await hrEmployeeApi.create(form as any)
      message.success('创建成功')
    }
    router.push('/hr/employee/index')
  } catch (e: any) { message.error(e?.response?.data?.message || '保存失败') }
  finally { saving.value = false }
}

function handleBack() { router.push('/hr/employee/index') }

onMounted(() => {
  const id = route.params.id
  if (id) { editingId.value = Number(id); loadData(Number(id)) }
})
</script>

<style scoped>
.form-scroll-area { flex: 1; overflow-y: auto; padding: 0 16px 16px; }
.section-card { background: #fff; border-radius: 6px; padding: 20px 24px 12px; margin-bottom: 12px; }
.section-title { font-size: 14px; font-weight: 600; color: #262626; margin-bottom: 16px; padding-bottom: 10px; border-bottom: 1px solid #f0f0f0; }
.form-footer { background: #fff; border-radius: 6px; padding: 16px 24px; text-align: right; margin-top: 12px; }
</style>
