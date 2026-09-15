<template>
  <a-modal
    :open="open"
    title="出车登记（绑定 + 出车前检查）"
    :width="780"
    :confirm-loading="saving"
    :mask-closable="false"
    :footer="null"
    @cancel="emit('update:open', false)"
  >
    <a-steps
      :current="step"
      size="small"
      style="margin-bottom: 16px"
    >
      <a-step title="绑定信息" />
      <a-step title="出车前检查" />
    </a-steps>

    <!-- Step1 绑定信息 -->
    <a-form
      v-show="step === 0"
      :label-col="{ span: 5 }"
      :wrapper-col="{ span: 17 }"
      size="small"
    >
      <a-form-item
        label="配送员"
        required
      >
        <a-select
          v-model:value="form.riderId"
          show-search
          option-filter-prop="label"
          placeholder="请选择配送员"
          :options="riderOptions"
          @change="onRiderChange"
        />
      </a-form-item>
      <a-form-item
        label="车辆"
        required
      >
        <a-select
          v-model:value="form.vehicleId"
          show-search
          option-filter-prop="label"
          placeholder="请选择车辆（仅显示可出车车辆）"
          :options="vehicleOptions"
          @change="onVehicleChange"
        />
      </a-form-item>
      <a-form-item label="绑定原因">
        <a-input
          v-model:value="form.bindReason"
          placeholder="任务描述 / 绑定原因"
        />
      </a-form-item>
      <a-alert
        v-if="eligibilityReason"
        type="warning"
        show-icon
        :message="eligibilityReason"
        style="margin-bottom: 8px"
      />
    </a-form>

    <!-- Step2 出车前检查 -->
    <div v-show="step === 1">
      <VehicleCheckForm
        ref="checkRef"
        v-model="check"
        mileage-label="出车里程(km)"
      />
      <a-alert
        v-if="checkRef?.willFail"
        type="warning"
        show-icon
        message="检查存在否决项，提交后将被判定为「不通过」并禁止出车，检查单仍会留档（可转《车辆维护》处理）"
        style="margin-top: 4px"
      />
    </div>

    <div class="wizard-footer">
      <a-button
        v-if="step === 1"
        @click="step = 0"
      >
        上一步
      </a-button>
      <a-button @click="emit('update:open', false)">
        取消
      </a-button>
      <a-button
        v-if="step === 0"
        type="primary"
        @click="next"
      >
        下一步
      </a-button>
      <a-button
        v-else
        type="primary"
        :loading="saving"
        @click="onSubmit"
      >
        确认出车
      </a-button>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { verificationApi } from '@/api/dms/verification'
import VehicleCheckForm from '../VehicleCheckForm.vue'
import type { VehicleCheckData } from '../VehicleCheckForm.vue'
import { emptyCheck } from '../../config/verification-config'

const props = defineProps<{
  open: boolean
  riderOptions: any[]
  vehicleRawList: any[]
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  saved: []
}>()

const saving = ref(false)
const step = ref(0)
const eligibilityReason = ref('')
const form = reactive<any>({ riderId: undefined, vehicleId: undefined, bindReason: '' })
const checkRef = ref<any>(null)
const check = ref<VehicleCheckData>(emptyCheck())

/** 可出车车辆：排除已报废；「绑定中」的车辆后端会再次校验 */
const vehicleOptions = computed(() =>
  props.vehicleRawList
    .filter((v: any) => Number(v.status) !== 3)
    .map((v: any) => ({
      value: String(v.id),
      label: `${v.plateNo || v.vehicleCode || v.id}${v.statusText ? '（' + v.statusText + '）' : ''}`,
      mileage: v.currentMileage,
    })))

// 打开时重置（等价于原 openBindModal）
watch(() => props.open, (v) => {
  if (v) {
    form.riderId = undefined
    form.vehicleId = undefined
    form.bindReason = ''
    eligibilityReason.value = ''
    check.value = emptyCheck()
    step.value = 0
  }
})

async function onRiderChange(value: any) {
  eligibilityReason.value = ''
  if (!value) return
  try {
    const res: any = await verificationApi.kycEligibility(value)
    const elig = res?.data ?? res
    if (elig && elig.eligible === false) {
      eligibilityReason.value = '该配送员不具备接单资质：' + (elig.reasons || []).join('；')
    }
  } catch {
    // 资质查询失败不阻断绑定操作
  }
}

/** 选车后带出车辆当前里程作为出车里程（可改） */
function onVehicleChange(value: any) {
  const hit = vehicleOptions.value.find(o => o.value === String(value))
  check.value.mileage = hit?.mileage ?? undefined
}

function next() {
  if (!form.riderId || !form.vehicleId) {
    message.warning('请选择配送员与车辆')
    return
  }
  step.value = 1
}

async function onSubmit() {
  saving.value = true
  try {
    // 1) 先落出车前检查（无论通过与否都留档）
    const insRes: any = await verificationApi.inspectionCreate({
      vehicleId: form.vehicleId,
      riderId: form.riderId,
      inspectionType: 1,
      ...check.value,
    })
    const inspectionId = insRes?.data ?? insRes?.id
    // 2) 取回检查结论（否决项由后端统一判定，前端不重复实现规则）
    const detailRes: any = await verificationApi.inspectionDetail(inspectionId)
    const inspection = detailRes?.data ?? detailRes
    if (Number(inspection?.result) !== 1) {
      message.error(`出车前检查不通过（异常项：${checkRef.value?.abnormalItems?.join('、') || '见检查单'}），已留档巡检单 #${inspectionId}，禁止出车；请转《车辆维护》处理`)
      return
    }
    // 3) 检查通过 → 出车绑定（后端再次校验）
    await verificationApi.bind({
      riderId: form.riderId,
      vehicleId: form.vehicleId,
      inspectionId,
      bindReason: form.bindReason || undefined,
    })
    message.success('出车登记成功（已完成出车前检查）')
    emit('update:open', false)
    emit('saved')
  } catch (e: any) {
    message.error(e?.message || '出车登记失败')
  } finally {
    saving.value = false
  }
}
</script>
