<template>
  <a-modal
    :open="open"
    title="新增抽检 / 定检"
    :width="780"
    :confirm-loading="saving"
    :mask-closable="false"
    @ok="onOk"
    @cancel="emit('update:open', false)"
  >
    <a-form
      :label-col="{ span: 5 }"
      :wrapper-col="{ span: 17 }"
      size="small"
    >
      <a-row :gutter="12">
        <a-col :span="12">
          <a-form-item
            label="车辆"
            required
          >
            <a-select
              v-model:value="form.vehicleId"
              show-search
              option-filter-prop="label"
              placeholder="请选择车辆"
              :options="vehicleOptions"
              @change="onVehicleChange"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="检查类型">
            <a-select
              v-model:value="form.inspectionType"
              :options="[{ value: 3, label: '随机抽检' }, { value: 4, label: '定期检查' }]"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="检查人">
            <a-select
              v-model:value="form.riderId"
              show-search
              option-filter-prop="label"
              allow-clear
              placeholder="缺省为当前登录人"
              :options="riderOptions"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <VehicleCheckForm
        v-model="check"
        mileage-label="当前里程(km)"
      />
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import VehicleCheckForm from '../VehicleCheckForm.vue'
import type { VehicleCheckData } from '../VehicleCheckForm.vue'
import { emptyCheck } from '../../config/verification-config'

const props = defineProps<{
  open: boolean
  saving?: boolean
  vehicleOptions: any[]
  riderOptions: any[]
  vehicleRawList: any[]
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  submit: [values: any]
}>()

const form = reactive<any>({ vehicleId: undefined, riderId: undefined, inspectionType: 3 })
const check = ref<VehicleCheckData>(emptyCheck())

// 打开时重置（等价于原 openSpotCheck）
watch(() => props.open, (v) => {
  if (v) {
    form.vehicleId = undefined
    form.riderId = undefined
    form.inspectionType = 3
    check.value = emptyCheck()
  }
})

// 选择车辆后带出当前里程（等价于原 onSpotCheckVehicleChange）
function onVehicleChange(value: any) {
  const hit = props.vehicleRawList.find((v: any) => String(v.id) === String(value))
  check.value.mileage = hit?.currentMileage ?? undefined
}

function onOk() {
  emit('submit', { ...form, ...check.value })
}
</script>
