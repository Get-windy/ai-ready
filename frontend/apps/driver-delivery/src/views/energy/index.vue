<template>
  <div class="energy-page">
    <NavBar title="补能登记" left-arrow @click-left="onBack" />

    <Form class="energy-form" @submit="onSubmit">
      <!-- 主体：默认我自己（两轮车换电/充电），有在途绑定时可选所驾车辆 -->
      <CellGroup title="补能主体">
        <Cell title="主体">
          <template #value>
            <RadioGroup v-model="subjectType" direction="horizontal">
              <Radio name="RIDER">
                我（两轮车）
              </Radio>
              <Radio name="VEHICLE">
                所驾车辆
              </Radio>
            </RadioGroup>
          </template>
        </Cell>
        <Cell
          v-if="subjectType === 'VEHICLE'"
          title="车辆"
          :value="bindingPlate || '未绑定车辆'"
        />
      </CellGroup>

      <Field
        v-model="form.energyType"
        label="补能类型"
        readonly
        is-link
        @click="showTypePicker = true"
      >
        <template #input>
          <span>{{ typeLabel(form.energyType) }}</span>
        </template>
      </Field>
      <Popup v-model:show="showTypePicker" position="bottom" round>
        <Picker
          :columns="typeColumns"
          title="选择补能类型"
          @confirm="onTypeConfirm"
          @cancel="showTypePicker = false"
        />
      </Popup>

      <Field
        v-model="form.amountYuan"
        label="金额(元)"
        type="number"
        placeholder="小票金额"
        :rules="[{ required: true, message: '请输入金额' }]"
      />
      <Field
        v-model="form.quantity"
        :label="`数量(${quantityUnit})`"
        type="number"
        placeholder="选填"
      />
      <Field
        v-model="form.odometer"
        label="里程(km)"
        type="number"
        placeholder="仪表/当次里程，用于算每公里成本"
      />
      <Field
        v-model="form.cardNo"
        label="卡号/套餐"
        placeholder="选填，刷卡/换电套餐填"
      />
      <Field
        v-model="form.station"
        label="站点/商户"
        placeholder="选填"
      />
      <Field
        v-model="form.occurredAt"
        label="补能时间"
        readonly
        is-link
        @click="showDatePicker = true"
      />
      <Popup v-model:show="showDatePicker" position="bottom" round>
        <DatePicker
          v-model="dateValue"
          type="datetime"
          title="选择补能时间"
          :min-date="minDate"
          :max-date="maxDate"
          @confirm="onDateConfirm"
          @cancel="showDatePicker = false"
        />
      </Popup>

      <!-- 凭证：拍照/相册上传，落库为图片 URL -->
      <CellGroup title="凭证">
        <Cell
          title="小票/发票照片"
          :value="uploading ? '上传中…' : (form.voucherUrl ? '已上传' : '未上传')"
          is-link
          @click="pickVoucher"
        />
        <Cell v-if="form.voucherUrl">
          <template #title>
            <img
              :src="form.voucherUrl"
              class="voucher-thumb"
              alt="凭证"
            >
          </template>
          <template #value>
            <Button
              size="mini"
              @click="form.voucherUrl = ''"
            >
              移除
            </Button>
          </template>
        </Cell>
      </CellGroup>

      <Field
        v-model="form.remark"
        label="备注"
        type="textarea"
        rows="2"
        autosize
        placeholder="选填"
      />

      <div style="margin: 20px 16px;">
        <Button
          round
          block
          type="primary"
          native-type="submit"
          :loading="submitting"
        >
          提交补能
        </Button>
      </div>
    </Form>

    <input
      ref="fileRef"
      type="file"
      accept="image/*"
      capture="environment"
      style="display: none"
      @change="onVoucherChange"
    >
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  NavBar, Form, Field, Cell, CellGroup, Button, Picker, Popup, Radio, RadioGroup, DatePicker, Toast,
} from 'vant'
import { dmsApi } from '@/api/dms'
import request from '@/utils/request'

const router = useRouter()

const TYPE_OPTIONS = [
  { text: '汽油', value: 1 },
  { text: '柴油', value: 2 },
  { text: '充电', value: 3 },
  { text: '换电', value: 4 },
  { text: '加气', value: 5 },
]
const typeColumns = TYPE_OPTIONS.map(o => ({ text: o.text, value: o.value }))
const typeLabel = (v: number) => TYPE_OPTIONS.find(o => o.value === v)?.text || ''

const subjectType = ref<'RIDER' | 'VEHICLE'>('RIDER')
const bindingPlate = ref('')
const bindingVehicleId = ref<string | null>(null)
const riderId = ref<string | null>(null)

const form = reactive<any>({
  energyType: 4,
  amountYuan: undefined as number | undefined,
  quantity: undefined as number | undefined,
  odometer: undefined as number | undefined,
  cardNo: '',
  station: '',
  occurredAt: '',
  voucherUrl: '',
  remark: '',
})

const quantityUnit = computed(() => ({ 1: 'L', 2: 'L', 3: 'kWh', 4: '次', 5: 'm³' }[form.energyType] || ''))

const showTypePicker = ref(false)
const onTypeConfirm = ({ selectedOptions }: any) => {
  form.energyType = Number(selectedOptions?.[0]?.value ?? 4)
  showTypePicker.value = false
}

const showDatePicker = ref(false)
const dateValue = ref<string[]>([])
const minDate = new Date(2024, 0, 1)
const maxDate = new Date(new Date().getFullYear() + 1, 11, 31)
const fmt = (d: Date) => {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
const onDateConfirm = ({ selectedValues }: any) => {
  const [y, mo, d, h, mi] = selectedValues.map((v: any) => Number(v))
  form.occurredAt = fmt(new Date(y, mo - 1, d, h, mi))
  showDatePicker.value = false
}

const fileRef = ref<HTMLInputElement>()
const uploading = ref(false)
const pickVoucher = () => fileRef.value?.click()
const onVoucherChange = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res: any = await request.post('/file/upload', fd)
    const url = typeof res === 'string' ? res : (res?.url || res?.data?.url)
    if (!url) {
      Toast.fail('照片上传失败')
      return
    }
    form.voucherUrl = url
  } catch {
    Toast.fail('照片上传失败')
  } finally {
    uploading.value = false
  }
}

const submitting = ref(false)
const onBack = () => router.back()

const onSubmit = async () => {
  if (form.amountYuan == null || Number(form.amountYuan) <= 0) {
    Toast.fail('请输入补能金额')
    return
  }
  if (subjectType.value === 'VEHICLE' && !bindingVehicleId.value) {
    Toast.fail('当前未绑定车辆，无法按车辆补能')
    return
  }
  submitting.value = true
  try {
    const res: any = await dmsApi.createEnergyLog({
      vehicleId: subjectType.value === 'VEHICLE' ? bindingVehicleId.value : undefined,
      riderId: subjectType.value === 'RIDER' ? riderId.value : undefined,
      energyType: form.energyType,
      amountYuan: Number(form.amountYuan),
      quantity: form.quantity == null ? undefined : Number(form.quantity),
      odometer: form.odometer == null ? undefined : Number(form.odometer),
      cardNo: form.cardNo || undefined,
      station: form.station || undefined,
      occurredAt: form.occurredAt || undefined,
      voucherUrl: form.voucherUrl || undefined,
      remark: form.remark || undefined,
    })
    const saved = res?.data ?? res
    if (Number(saved?.abnormalFlag) === 1) {
      Toast.fail(`已登记，但标记异常：${saved.abnormalReason || ''}`)
    } else if (saved?.unitCost != null) {
      Toast.success(`已登记：区间 ${saved.mileageSinceLast}km，每公里 ${saved.unitCost} 元`)
    } else {
      Toast.success('补能已登记')
    }
    router.replace({ path: '/' })
  } catch (e: any) {
    Toast.fail(e?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  form.occurredAt = fmt(new Date())
  try {
    const res: any = await dmsApi.getMyRider()
    const rider = res?.data ?? res
    riderId.value = rider?.id != null ? String(rider.id) : null
    if (!riderId.value) {
      Toast.fail('当前账号未关联配送员档案')
      return
    }
    const bindRes: any = await dmsApi.getActiveBinding(riderId.value).catch(() => null)
    const binding = bindRes?.data ?? bindRes
    if (binding?.id) {
      bindingPlate.value = binding.plateNo || ''
      bindingVehicleId.value = binding.vehicleId != null ? String(binding.vehicleId) : null
    }
  } catch {
    // 未关联配送员：仍可提交但不带主体，由后端校验给出提示
  }
})
</script>

<style scoped>
.energy-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.energy-form {
  padding-top: 12px;
}

.voucher-thumb {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  object-fit: cover;
}
</style>
