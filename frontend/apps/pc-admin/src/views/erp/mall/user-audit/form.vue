<template>
  <ErrorBoundary @error="handleError">
    <BillFormPage
      v-model="formData"
      title="用户审核"
      :fields="formFields"
      :loading="loading"
      :saving="saving"
      @save="handleSave"
      @back="handleBack"
    >
      <template #extra>
        <a-space>
          <a-badge :status="loading ? 'processing' : 'success'" />
          <a-button
            size="small"
            @click="fetchDetail"
          >
            <template #icon>
              <ReloadOutlined />
            </template>
          </a-button>
        </a-space>
      </template>
      <template #auditStatus="{ value }">
        <a-tag
          v-if="value === 0"
          color="orange"
        >
          待审核
        </a-tag>
        <a-tag
          v-else-if="value === 1"
          color="green"
        >
          已通过
        </a-tag>
        <a-tag
          v-else-if="value === 2"
          color="red"
        >
          已驳回
        </a-tag>
        <span v-else>未知</span>
      </template>
    </BillFormPage>
  </ErrorBoundary>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const formData = reactive<Record<string, any>>({})

const formFields = [
  { key: 'username', label: '用户名', type: 'input' as const, span: 12 },
  { key: 'companyName', label: '公司名称', type: 'input' as const, span: 12 },
  { key: 'nickname', label: '昵称', type: 'input' as const, span: 12 },
  { key: 'phone', label: '手机号', type: 'input' as const, span: 12 },
  { key: 'auditStatus', label: '审核状态', type: 'slot' as const, slotName: 'auditStatus', span: 12 },
  { key: 'auditRemark', label: '审核备注', type: 'textarea' as const, span: 24 },
]

const fetchDetail = async () => {
  loading.value = true
  try {
    const id = route.params.id
    if (id) {
      const res: any = await request.get(`/api/v1/mall/user/audit/${id}`)
      // form data handled by BillFormPage
    }
  } catch (e) { console.warn('[用户审核] 获取详情失败', e)
  } finally { loading.value = false }
}

const handleSave = async (data: any) => {
  saving.value = true
  try {
    await request.post(`/api/v1/mall/user/audit/${route.params.id}`, data)
    message.success('保存成功')
    router.back()
  } catch { message.error('保存失败')
  } finally { saving.value = false }
}

const handleBack = () => router.back()
const handleError = (e: Error) => console.error(e)
onMounted(fetchDetail)
</script>
