<template>
  <div class="binding-page">
    <NavBar title="车辆绑定" left-arrow @click-left="onBack" />

    <!-- Inspection Result -->
    <CellGroup v-if="inspectionId" title="验车结果">
      <Cell title="验车已通过" icon="success" title-style="color: #07c160;" />
      <Cell title="验车编号" :value="inspectionId" />
    </CellGroup>

    <Form @submit="onSubmit" class="binding-form">
      <Field
        v-model="form.vehicleId"
        name="vehicleId"
        label="车辆编号"
        placeholder="请输入车辆编号"
        :rules="[{ required: true, message: '请输入车辆编号' }]"
      />

      <Field
        v-model="form.bindReason"
        name="bindReason"
        label="绑定原因"
        type="textarea"
        placeholder="请输入绑定原因（选填）"
        :autosize="{ minHeight: 60 }"
      />

      <!-- hidden inspectionId -->
      <div style="display: none;">
        <Field v-model="form.inspectionId" name="inspectionId" />
      </div>

      <div style="margin: 20px 16px;">
        <Button round block type="primary" native-type="submit" :loading="submitting">
          确认绑定
        </Button>
      </div>
    </Form>

    <!-- Success Dialog -->
    <Dialog v-model:show="showSuccess" title="绑定成功" message="车辆绑定成功，即将跳转至配送任务页面" @confirm="onSuccessConfirm" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NavBar, Form, Field, Cell, CellGroup, Button, Dialog, Toast } from 'vant'
import { dmsApi } from '@/api/dms'

const route = useRoute()
const router = useRouter()

const inspectionId = route.query.inspectionId as string || ''

const form = reactive({
  vehicleId: '',
  bindReason: '',
  inspectionId
})

const submitting = ref(false)
const showSuccess = ref(false)

const onBack = () => {
  router.back()
}

const onSubmit = async () => {
  if (!form.vehicleId) {
    Toast.fail('请输入车辆编号')
    return
  }

  submitting.value = true
  try {
    await dmsApi.bindVehicle({
      vehicle_id: form.vehicleId,
      bind_reason: form.bindReason,
      inspection_id: form.inspectionId || undefined
    })
    showSuccess.value = true
  } catch (err: any) {
    Toast.fail(err?.message || '绑定失败')
  } finally {
    submitting.value = false
  }
}

const onSuccessConfirm = () => {
  router.push({ path: '/delivery' })
}
</script>

<style scoped>
.binding-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.binding-form {
  padding-top: 12px;
}
</style>
