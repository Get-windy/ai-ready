<template>
  <div class="inspection-page">
    <NavBar title="出车验车" left-arrow @click-left="onBack" />

    <Form @submit="onSubmit" class="inspection-form">
      <!-- Vehicle ID -->
      <Field
        v-model="form.vehicle_id"
        name="vehicle_id"
        label="车辆编号"
        placeholder="请输入或扫描车辆编号"
        :rules="[{ required: true, message: '请输入车辆编号' }]"
      />

      <!-- Inspection Type -->
      <Field name="inspection_type" label="检查类型">
        <template #input>
          <Picker
            v-model="form.inspection_type"
            :columns="inspectionTypeOptions"
            title="选择检查类型"
            @confirm="(val) => form.inspection_type = val"
          >
            <template #default>
              <div class="picker-value" @click="showInspectionPicker = true">
                {{ inspectionTypeLabel(form.inspection_type) || '请选择检查类型' }}
              </div>
            </template>
          </Picker>
        </template>
      </Field>

      <!-- Mileage -->
      <Field
        v-model="form.mileage"
        name="mileage"
        label="里程(km)"
        type="digit"
        placeholder="请输入当前里程"
        :rules="[{ required: true, message: '请输入里程' }]"
      />

      <!-- Fuel Level -->
      <Field
        v-model="form.fuel_level"
        name="fuel_level"
        label="油量/电量"
        type="digit"
        placeholder="请输入油量或电量"
        :rules="[{ required: true, message: '请输入油量/电量' }]"
      />

      <!-- Status Toggles -->
      <CellGroup title="车辆状态检查">
        <Cell label="外观状态">
          <template #value>
            <a-radio-group v-model:value="form.exterior_status" size="small">
              <a-radio-button :value="0">正常</a-radio-button>
              <a-radio-button :value="1">异常</a-radio-button>
            </a-radio-group>
          </template>
        </Cell>
        <Cell label="轮胎状态">
          <template #value>
            <a-radio-group v-model:value="form.tire_status" size="small">
              <a-radio-button :value="0">正常</a-radio-button>
              <a-radio-button :value="1">异常</a-radio-button>
            </a-radio-group>
          </template>
        </Cell>
        <Cell label="灯光状态">
          <template #value>
            <a-radio-group v-model:value="form.light_status" size="small">
              <a-radio-button :value="0">正常</a-radio-button>
              <a-radio-button :value="1">异常</a-radio-button>
            </a-radio-group>
          </template>
        </Cell>
        <Cell label="刹车状态">
          <template #value>
            <a-radio-group v-model:value="form.brake_status" size="small">
              <a-radio-button :value="0">正常</a-radio-button>
              <a-radio-button :value="1">异常</a-radio-button>
            </a-radio-group>
          </template>
        </Cell>
        <Cell label="清洁状态">
          <template #value>
            <a-radio-group v-model:value="form.cleanliness_status" size="small">
              <a-radio-button :value="0">正常</a-radio-button>
              <a-radio-button :value="1">异常</a-radio-button>
            </a-radio-group>
          </template>
        </Cell>
      </CellGroup>

      <!-- Checkboxes for equipment -->
      <CellGroup title="随车装备检查">
        <Cell center>
          <template #title>
            <span>灭火器</span>
          </template>
          <template #value>
            <a-radio-group v-model:value="form.fire_extinguisher" size="small">
              <a-radio-button :value="0">正常</a-radio-button>
              <a-radio-button :value="1">缺失</a-radio-button>
            </a-radio-group>
          </template>
        </Cell>
        <Cell center>
          <template #title>
            <span>警示三角牌</span>
          </template>
          <template #value>
            <a-radio-group v-model:value="form.warning_triangle" size="small">
              <a-radio-button :value="0">有</a-radio-button>
              <a-radio-button :value="1">缺失</a-radio-button>
            </a-radio-group>
          </template>
        </Cell>
      </CellGroup>

      <div style="margin: 20px 16px;">
        <Button round block type="primary" native-type="submit" :loading="submitting">
          提交验车
        </Button>
      </div>
    </Form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Form, Field, Cell, CellGroup, Button, Picker, Dialog, Toast } from 'vant'
import { api } from '@/api'
import { dmsApi } from '@/api/dms'

const router = useRouter()

const showInspectionPicker = ref(false)

const inspectionTypeOptions = [
  { text: '出车检查', value: 1 },
  { text: '收车检查', value: 2 }
]

const inspectionTypeLabel = (val: number | undefined): string => {
  const map: Record<number, string> = { 1: '出车检查', 2: '收车检查' }
  return val != null ? map[val] ?? '' : ''
}

const form = reactive({
  vehicle_id: '',
  inspection_type: 1,
  mileage: undefined as number | undefined,
  fuel_level: undefined as number | undefined,
  exterior_status: 0,
  tire_status: 0,
  light_status: 0,
  brake_status: 0,
  cleanliness_status: 0,
  fire_extinguisher: 0,
  warning_triangle: 0
})

const submitting = ref(false)

const onBack = () => {
  router.back()
}

const onSubmit = async () => {
  if (!form.vehicle_id) {
    Toast.fail('请输入车辆编号')
    return
  }
  if (form.mileage == null) {
    Toast.fail('请输入里程')
    return
  }
  if (form.fuel_level == null) {
    Toast.fail('请输入油量/电量')
    return
  }

  submitting.value = true
  try {
    const res = await dmsApi.createInspection({
      vehicle_id: form.vehicle_id,
      inspection_type: form.inspection_type,
      mileage: form.mileage,
      fuel_level: form.fuel_level,
      exterior_status: form.exterior_status,
      tire_status: form.tire_status,
      light_status: form.light_status,
      brake_status: form.brake_status,
      cleanliness_status: form.cleanliness_status,
      fire_extinguisher: form.fire_extinguisher,
      warning_triangle: form.warning_triangle
    })
    const inspectionId = res?.data?.id ?? res?.id
    Toast.success('验车提交成功')
    setTimeout(() => {
      router.push({ path: '/binding', query: { inspectionId: String(inspectionId) } })
    }, 1000)
  } catch (err: any) {
    Toast.fail(err?.message || '验车提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.inspection-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.inspection-form {
  padding-top: 12px;
}

.picker-value {
  color: #323233;
  text-align: right;
  padding: 8px 0;
}
</style>
