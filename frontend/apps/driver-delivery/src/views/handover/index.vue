<template>
  <div class="handover-page">
    <NavBar title="交车" left-arrow @click-left="onBack" />

    <div v-if="loading" style="text-align: center; padding: 40px 0;">
      <Loading />
    </div>

    <template v-else>
      <!-- Current binding info -->
      <CellGroup v-if="bindingInfo" title="当前绑定信息">
        <Cell title="车牌号" :value="bindingInfo.plate_no || '--'" />
        <Cell title="绑定时间" :value="bindingInfo.bind_time || '--'" />
        <Cell title="车辆编号" :value="bindingInfo.vehicle_id || '--'" />
      </CellGroup>

      <CellGroup v-else title="当前绑定信息">
        <Cell title="状态" value="暂无绑定信息" />
      </CellGroup>

      <Form @submit="onSubmit" class="handover-form">
        <Field
          v-model="form.handover_mileage"
          name="handover_mileage"
          label="交车里程(km)"
          type="digit"
          placeholder="请输入交车里程"
          :rules="[{ required: true, message: '请输入交车里程' }]"
        />

        <Field
          v-model="form.remark"
          name="remark"
          label="备注"
          type="textarea"
          placeholder="请输入备注（选填）"
          :autosize="{ minHeight: 60 }"
        />

        <div style="margin: 20px 16px;">
          <Button round block type="primary" native-type="submit" :loading="submitting">
            确认交车
          </Button>
        </div>
      </Form>
    </template>

    <Dialog v-model:show="showSuccess" title="交车成功" message="交车操作已完成，即将返回主页" @confirm="onSuccessConfirm" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Form, Field, Cell, CellGroup, Button, Dialog, Toast, Loading } from 'vant'
import { api } from '@/api'
import { dmsApi } from '@/api/dms'

const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const showSuccess = ref(false)
const bindingInfo = ref<any>(null)
const bindingId = ref<number | null>(null)

const form = reactive({
  handover_mileage: undefined as number | undefined,
  remark: ''
})

const fetchBindingInfo = async () => {
  loading.value = true
  try {
    const userId = api.getCurrentUserId?.() || 0
    const res = await dmsApi.getActiveBinding(userId)
    const data = res?.data ?? res ?? null
    if (data) {
      bindingInfo.value = data
      bindingId.value = data.id ?? null
    }
  } catch {
    // no binding found
  } finally {
    loading.value = false
  }
}

const onBack = () => {
  router.back()
}

const onSubmit = async () => {
  if (form.handover_mileage == null) {
    Toast.fail('请输入交车里程')
    return
  }
  if (!bindingId.value) {
    Toast.fail('未找到当前绑定信息')
    return
  }

  submitting.value = true
  try {
    // Get current location
    const pos = await new Promise<GeolocationPosition>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: true,
        timeout: 10000
      })
    })

    await dmsApi.handover(bindingId.value, {
      handover_mileage: form.handover_mileage,
      remark: form.remark,
      handover_lat: pos.coords.latitude,
      handover_lng: pos.coords.longitude
    })
    showSuccess.value = true
  } catch (err: any) {
    if (err instanceof GeolocationPositionError) {
      // location failed, submit without location
      try {
        await dmsApi.handover(bindingId.value, {
          handover_mileage: form.handover_mileage,
          remark: form.remark
        })
        showSuccess.value = true
        return
      } catch (e2: any) {
        Toast.fail(e2?.message || '交车失败')
      }
    } else {
      Toast.fail(err?.message || '交车失败')
    }
  } finally {
    submitting.value = false
  }
}

const onSuccessConfirm = () => {
  router.push({ path: '/' })
}

onMounted(() => {
  fetchBindingInfo()
})
</script>

<style scoped>
.handover-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.handover-form {
  padding-top: 12px;
}
</style>
