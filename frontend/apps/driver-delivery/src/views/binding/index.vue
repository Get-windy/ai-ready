<template>
  <div class="binding-page">
    <NavBar title="车辆绑定" left-arrow @click-left="onBack" />

    <!-- 出车前检查：必须先做检查，检查通过才能绑定（后端强制门控） -->
    <CellGroup v-if="inspectionId" title="出车前检查">
      <Cell
        title="检查结果"
        :value="inspectionResult === 1 ? '通过' : '不通过'"
        :title-style="inspectionResult === 1 ? 'color: #07c160;' : 'color: #ee0a24;'"
      />
      <Cell title="检查单号" :value="String(inspectionId)" />
    </CellGroup>
    <div v-else class="empty-tip">
      <Empty description="尚未完成出车前检查">
        <Button type="primary" size="small" @click="goInspection">
          去做出车前检查
        </Button>
      </Empty>
    </div>

    <Form v-if="inspectionId" class="binding-form" @submit="onSubmit">
      <Field
        :model-value="selectedVehicleLabel"
        label="车辆"
        placeholder="请选择车辆"
        readonly
        is-link
        :rules="[{ required: true, message: '请选择车辆' }]"
        @click="showVehiclePicker = true"
      />
      <Popup v-model:show="showVehiclePicker" position="bottom" round>
        <Picker
          :columns="vehicleColumns"
          title="选择车辆"
          @confirm="onVehicleConfirm"
          @cancel="showVehiclePicker = false"
        />
      </Popup>

      <Field
        v-model="form.bindReason"
        name="bindReason"
        label="绑定原因"
        type="textarea"
        placeholder="请输入绑定原因（选填）"
        :autosize="{ minHeight: 60 }"
      />

      <div style="margin: 20px 16px;">
        <Button
          round
          block
          type="primary"
          native-type="submit"
          :loading="submitting"
        >
          确认绑定
        </Button>
      </div>
    </Form>

    <Dialog
      v-model:show="showSuccess"
      title="绑定成功"
      message="车辆绑定成功，即将跳转至配送任务页面"
      @confirm="onSuccessConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NavBar, Form, Field, Cell, CellGroup, Button, Dialog, Toast, Picker, Popup, Empty } from 'vant'
import { dmsApi } from '@/api/dms'

const route = useRoute()
const router = useRouter()

/** 出车前检查单（由「出车验车」页带入；缺省时禁止绑定） */
const inspectionId = ref<string>(String(route.query.inspectionId || ''))
const inspectionResult = ref<number>(Number(route.query.result ?? 1))
const vehicleId = ref<string>('')

const form = reactive({
  bindReason: '',
})

const vehicleColumns = ref<{ text: string; value: string }[]>([])
const showVehiclePicker = ref(false)

const selectedVehicleLabel = computed(() => {
  const hit = vehicleColumns.value.find(v => v.value === vehicleId.value)
  return hit ? hit.text : ''
})

const submitting = ref(false)
const showSuccess = ref(false)

const onBack = () => {
  router.back()
}

const goInspection = () => {
  router.push({ path: '/inspection', query: { type: '1' } })
}

const loadVehicles = async () => {
  try {
    const res: any = await dmsApi.getVehicleOptions()
    const list = res?.data ?? res ?? []
    vehicleColumns.value = (Array.isArray(list) ? list : []).map((v: any) => ({
      text: `${v.plateNo || v.vehicleCode || v.id}${v.statusText ? '（' + v.statusText + '）' : ''}`,
      value: String(v.id),
    }))
  } catch {
    vehicleColumns.value = []
  }
}

const onVehicleConfirm = ({ selectedOptions }: any) => {
  vehicleId.value = String(selectedOptions?.[0]?.value ?? '')
  showVehiclePicker.value = false
}

const onSubmit = async () => {
  if (!inspectionId.value) {
    Toast.fail('请先完成出车前检查')
    return
  }
  if (inspectionResult.value !== 1) {
    Toast.fail('出车前检查不通过，禁止出车')
    return
  }
  if (!vehicleId.value) {
    Toast.fail('请选择车辆')
    return
  }

  submitting.value = true
  try {
    const riderRes: any = await dmsApi.getMyRider()
    const rider = riderRes?.data ?? riderRes
    if (!rider?.id) {
      Toast.fail('当前账号未关联配送员档案')
      return
    }
    await dmsApi.bindVehicle({
      riderId: rider.id,
      vehicleId: vehicleId.value,
      inspectionId: inspectionId.value,
      bindReason: form.bindReason || undefined,
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

onMounted(async () => {
  await loadVehicles()
  if (!inspectionId.value) {
    return
  }
  // 页面刷新后回填检查结论与车辆
  try {
    const res: any = await dmsApi.getInspection(inspectionId.value)
    const ins = res?.data ?? res
    if (ins?.result != null) {
      inspectionResult.value = Number(ins.result)
      if (ins.vehicleId) {
        vehicleId.value = String(ins.vehicleId)
      }
    }
  } catch {
    // 忽略：提交绑定后端仍会强校验
  }
})
</script>

<style scoped>
.binding-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.binding-form {
  padding-top: 12px;
}

.empty-tip {
  padding: 24px 0;
}
</style>
