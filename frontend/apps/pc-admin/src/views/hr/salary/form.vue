<template>
  <ErrorBoundary>
    <PageContainer title="薪资结构">
      <template #extra>
        <a-button type="text" @click="handleBack">
          <template #icon><ArrowLeftOutlined /></template>返回
        </a-button>
      </template>

      <div class="form-scroll-area">
        <a-form ref="formRef" :model="form" layout="vertical">
          <!-- 基本信息 -->
          <FormSection title="基本信息">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="员工" name="employeeId" required>
                  <a-select v-model:value="form.employeeId" show-search :filter-option="filterEmployee" placeholder="搜索选择员工">
                    <a-select-option v-for="e in employeeOptions" :key="e.id" :value="e.id">{{ e.employeeName }} ({{ e.employeeNo }})</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="生效日期" name="effectiveDate" required>
                  <a-date-picker v-model:value="form.effectiveDate" style="width:100%" />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>

          <!-- 薪资构成 -->
          <FormSection title="薪资构成">
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="基本工资">
                  <a-input-number v-model:value="form.baseSalary" :precision="2" :min="0" style="width:100%" placeholder="0.00" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="绩效工资">
                  <a-input-number v-model:value="form.performanceSalary" :precision="2" :min="0" style="width:100%" placeholder="0.00" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="岗位津贴">
                  <a-input-number v-model:value="form.positionAllowance" :precision="2" :min="0" style="width:100%" placeholder="0.00" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="交通补贴">
                  <a-input-number v-model:value="form.transportAllowance" :precision="2" :min="0" style="width:100%" placeholder="0.00" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="餐补">
                  <a-input-number v-model:value="form.mealAllowance" :precision="2" :min="0" style="width:100%" placeholder="0.00" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="住房补贴">
                  <a-input-number v-model:value="form.housingAllowance" :precision="2" :min="0" style="width:100%" placeholder="0.00" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="其他补贴">
                  <a-input-number v-model:value="form.otherAllowance" :precision="2" :min="0" style="width:100%" placeholder="0.00" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="社保基数">
                  <a-input-number v-model:value="form.socialBase" :precision="2" :min="0" style="width:100%" placeholder="0.00" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="公积金基数">
                  <a-input-number v-model:value="form.fundBase" :precision="2" :min="0" style="width:100%" placeholder="0.00" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="薪资合计">
              <span style="font-size:18px;font-weight:bold;color:#52c41a">
                ¥{{ totalSalary.toFixed(2) }}
              </span>
            </a-form-item>
          </FormSection>

          <!-- 备注 -->
          <FormSection>
            <a-form-item label="备注">
              <a-textarea v-model:value="form.remark" :rows="3" placeholder="请输入备注" />
            </a-form-item>
          </FormSection>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import FormSection from '@/components/FormSection/index.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { hrSalaryApi, hrEmployeeApi } from '@/api/hr'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const saving = ref(false)
const editingId = ref<number | null>(null)
const employeeOptions = ref<any[]>([])

const form = reactive({
  employeeId: undefined as number | undefined,
  baseSalary: 0, performanceSalary: 0, positionAllowance: 0,
  transportAllowance: 0, mealAllowance: 0, housingAllowance: 0,
  otherAllowance: 0, socialBase: 0, fundBase: 0,
  effectiveDate: null as any, remark: '', status: 1,
})

const totalSalary = computed(() => {
  return (form.baseSalary || 0) + (form.performanceSalary || 0) + (form.positionAllowance || 0)
    + (form.transportAllowance || 0) + (form.mealAllowance || 0) + (form.housingAllowance || 0) + (form.otherAllowance || 0)
})

function filterEmployee(input: string, option: any) {
  return (option.children || '').toLowerCase().includes(input.toLowerCase())
}

async function loadEmployeeOptions() {
  try {
    const employees = await hrEmployeeApi.page({ pageNum: 1, pageSize: 200 })
    employeeOptions.value = employees.records || []
  } catch { employeeOptions.value = [] }
}

async function loadData(id: number) {
  try {
    const res = await hrSalaryApi.getStructure(id)
    Object.assign(form, res)
  } catch { message.error('加载失败') }
}

function handleBack() { router.push('/hr/salary/index') }

async function handleSubmit() {
  if (!form.employeeId) {
    message.warning('请选择员工')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await hrSalaryApi.createStructure(form as any)
      message.success('更新成功')
    } else {
      await hrSalaryApi.createStructure(form as any)
      message.success('创建成功')
    }
    router.push('/hr/salary/index')
  } catch (e: any) { message.error(e?.response?.data?.message || '保存失败') }
  finally { saving.value = false }
}

onMounted(() => {
  loadEmployeeOptions()
  const id = route.params.id
  if (id) { editingId.value = Number(id); loadData(Number(id)) }
})
</script>

<style scoped>
.form-scroll-area { flex: 1; overflow-y: auto; padding: 0 16px 16px; }
.form-footer { background: #fff; border-radius: 6px; padding: 16px 24px; text-align: right; margin-top: 12px; }
</style>
