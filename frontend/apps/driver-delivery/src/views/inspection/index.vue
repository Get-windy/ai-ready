<template>
  <div class="inspection-page">
    <NavBar :title="pageTitle" left-arrow @click-left="onBack" />

    <Form class="inspection-form" @submit="onSubmit">
      <!-- 车辆选择（复用车辆选择器数据源，不再手输编号） -->
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
        v-model="form.inspectionType"
        label="检查类型"
        readonly
        is-link
        @click="showTypePicker = true"
      >
        <template #input>
          <span>{{ inspectionTypeLabel(form.inspectionType) }}</span>
        </template>
      </Field>
      <Popup v-model:show="showTypePicker" position="bottom" round>
        <Picker
          :columns="typeColumns"
          title="选择检查类型"
          @confirm="onTypeConfirm"
          @cancel="showTypePicker = false"
        />
      </Popup>

      <Field
        v-model="form.mileage"
        name="mileage"
        label="里程(km)"
        type="digit"
        placeholder="请输入当前仪表里程"
        :rules="[{ required: true, message: '请输入里程' }]"
      />
      <Field
        v-model="form.fuelLevel"
        name="fuelLevel"
        label="油量/电量(%)"
        type="digit"
        placeholder="0-100"
        :rules="[{ required: true, message: '请输入油量/电量' }]"
      />
      <Field
        v-model="form.inspectionLocation"
        name="inspectionLocation"
        label="检查地点"
        placeholder="停车场 / 站点名称（选填）"
      />

      <!-- 车况检查项：正常 / 异常（异常必须填写描述，便于留档与转维修） -->
      <CellGroup title="车况检查">
        <template v-for="item in CHECK_ITEMS" :key="item.key">
          <Cell :title="item.label">
            <template #value>
              <RadioGroup v-model="form[item.key]" direction="horizontal">
                <Radio :name="0">
                  正常
                </Radio>
                <Radio :name="1">
                  异常
                </Radio>
              </RadioGroup>
            </template>
          </Cell>
          <Field
            v-if="form[item.key] === 1"
            v-model="form[item.remarkKey]"
            :label="`${item.label}异常描述`"
            placeholder="请描述异常现象"
          />
        </template>
      </CellGroup>

      <CellGroup title="随车装备">
        <Cell title="灭火器">
          <template #value>
            <RadioGroup v-model="form.fireExtinguisher" direction="horizontal">
              <Radio :name="0">
                正常
              </Radio>
              <Radio :name="1">
                缺失/过期
              </Radio>
            </RadioGroup>
          </template>
        </Cell>
        <Cell title="三角警示牌">
          <template #value>
            <RadioGroup v-model="form.warningTriangle" direction="horizontal">
              <Radio :name="0">
                有
              </Radio>
              <Radio :name="1">
                缺失
              </Radio>
            </RadioGroup>
          </template>
        </Cell>
      </CellGroup>

      <Field
        v-model="form.remark"
        label="备注"
        type="textarea"
        rows="2"
        autosize
        placeholder="检查备注（选填）"
      />

      <NoticeBar
        v-if="willFail"
        type="danger"
        :text="`存在否决项（${vetoText}），提交后将判定为「不通过」并禁止出车`"
        wrapable
        :scrollable="false"
      />

      <div style="margin: 20px 16px;">
        <Button round block type="primary" native-type="submit" :loading="submitting">
          提交检查
        </Button>
      </div>
    </Form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NavBar, Form, Field, Cell, CellGroup, Button, Picker, Popup, Radio, RadioGroup, NoticeBar, Dialog, Toast,
} from 'vant'
import { dmsApi } from '@/api/dms'

const route = useRoute()
const router = useRouter()

/** 检查项（key 与后端字段一致） */
const CHECK_ITEMS = [
  { key: 'exteriorStatus', label: '车辆外观', remarkKey: 'exteriorRemark' },
  { key: 'tireStatus', label: '轮胎', remarkKey: 'tireRemark' },
  { key: 'lightStatus', label: '灯光', remarkKey: 'lightRemark' },
  { key: 'brakeStatus', label: '刹车', remarkKey: 'brakeRemark' },
  { key: 'cleanlinessStatus', label: '车内清洁', remarkKey: 'cleanlinessRemark' },
] as const

/** 否决项（与后端 evaluateResult 同口径：刹车/灯光/灭火器任一异常 → 不通过） */
const VETO_ITEMS = [
  { key: 'brakeStatus', label: '刹车' },
  { key: 'lightStatus', label: '灯光' },
  { key: 'fireExtinguisher', label: '灭火器' },
] as const

const form = reactive<any>({
  inspectionType: Number(route.query.type || 1),
  mileage: undefined,
  fuelLevel: undefined,
  inspectionLocation: '',
  exteriorStatus: 0,
  exteriorRemark: '',
  tireStatus: 0,
  tireRemark: '',
  lightStatus: 0,
  lightRemark: '',
  brakeStatus: 0,
  brakeRemark: '',
  cleanlinessStatus: 0,
  cleanlinessRemark: '',
  fireExtinguisher: 0,
  warningTriangle: 0,
  remark: '',
})

const pageTitle = computed(() => (form.inspectionType === 2 ? '收车检查' : '出车验车'))
const inspectionTypeLabel = (val: number) => ({ 1: '出车前检查', 2: '收车后检查', 3: '随机抽检', 4: '定期检查' }[val] || '')
const typeColumns = [
  { text: '出车前检查', value: 1 },
  { text: '收车后检查', value: 2 },
]

const showTypePicker = ref(false)
const onTypeConfirm = ({ selectedOptions }: any) => {
  form.inspectionType = Number(selectedOptions?.[0]?.value ?? 1)
  showTypePicker.value = false
}

const vehicleId = ref<string>('')
const vehicleColumns = ref<{ text: string; value: string }[]>([])
const showVehiclePicker = ref(false)
const selectedVehicleLabel = computed(() => {
  const hit = vehicleColumns.value.find(v => v.value === vehicleId.value)
  return hit ? hit.text : ''
})

const abnormalItems = computed(() => VETO_ITEMS.filter(i => form[i.key] === 1).map(i => i.label))
const willFail = computed(() => abnormalItems.value.length > 0)
const vetoText = computed(() => abnormalItems.value.join('、'))

const submitting = ref(false)

const onBack = () => {
  router.back()
}

const onVehicleConfirm = ({ selectedOptions }: any) => {
  vehicleId.value = String(selectedOptions?.[0]?.value ?? '')
  showVehiclePicker.value = false
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

const onSubmit = async () => {
  if (!vehicleId.value) {
    Toast.fail('请选择车辆')
    return
  }
  if (form.mileage == null) {
    Toast.fail('请输入里程')
    return
  }
  if (form.fuelLevel == null) {
    Toast.fail('请输入油量/电量')
    return
  }

  submitting.value = true
  try {
    const riderRes: any = await dmsApi.getMyRider().catch(() => null)
    const rider = riderRes?.data ?? riderRes
    const createRes: any = await dmsApi.createInspection({
      vehicleId: vehicleId.value,
      riderId: rider?.id,
      ...form,
    })
    const inspectionId = createRes?.data ?? createRes?.id

    // 取回后端判定结论（不在前端重复实现否决规则）
    const detailRes: any = await dmsApi.getInspection(inspectionId)
    const inspection = detailRes?.data ?? detailRes
    const passed = Number(inspection?.result) === 1

    if (form.inspectionType === 1) {
      if (!passed) {
        await Dialog.alert({
          title: '出车前检查不通过',
          message: `${vetoText.value}存在异常，检查单已留档（#${inspectionId}），禁止出车；请联系车管员转《车辆维护》处理。`,
        })
        return
      }
      Toast.success('检查通过，请完成车辆绑定')
      router.replace({ path: '/binding', query: { inspectionId: String(inspectionId), result: '1' } })
      return
    }

    await Dialog.alert({
      title: passed ? '收车检查已完成' : '收车检查存在异常',
      message: passed
        ? '收车检查通过。'
        : `${vetoText.value}存在异常，检查单已留档（#${inspectionId}），车辆将转《车辆维护》处理。`,
    })
    router.back()
  } catch (err: any) {
    Toast.fail(err?.message || '检查提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadVehicles)
</script>

<style scoped>
.inspection-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.inspection-form {
  padding-top: 12px;
}
</style>
