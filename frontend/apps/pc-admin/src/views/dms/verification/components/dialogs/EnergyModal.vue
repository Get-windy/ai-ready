<template>
  <a-modal
    :open="open"
    :title="form.id ? '修改补能记录' : '新增补能记录'"
    :width="720"
    :confirm-loading="saving"
    :mask-closable="false"
    @ok="onSubmit"
    @cancel="emit('update:open', false)"
  >
    <a-form
      :label-col="{ span: 5 }"
      :wrapper-col="{ span: 18 }"
      size="small"
    >
      <a-form-item
        label="补能主体"
        required
      >
        <a-radio-group
          v-model:value="form.subjectType"
          :disabled="!!form.id"
          @change="onSubjectChange"
        >
          <a-radio-button value="VEHICLE">
            四轮车
          </a-radio-button>
          <a-radio-button value="RIDER">
            骑手两轮车
          </a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item
        v-if="form.subjectType === 'VEHICLE'"
        label="车辆"
        required
      >
        <a-select
          v-model:value="form.vehicleId"
          show-search
          option-filter-prop="label"
          placeholder="请选择车辆"
          :options="vehicleOptions"
        />
      </a-form-item>
      <a-form-item
        v-else
        label="配送员"
        required
      >
        <a-select
          v-model:value="form.riderId"
          show-search
          option-filter-prop="label"
          placeholder="请选择配送员"
          :options="riderOptions"
        />
      </a-form-item>

      <a-row :gutter="12">
        <a-col :span="12">
          <a-form-item
            label="补能类型"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
            required
          >
            <a-select
              v-model:value="form.energyType"
              :options="ENERGY_TYPE_OPTIONS"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="支付方式"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-select
              v-model:value="form.payMode"
              :options="PAY_MODE_OPTIONS"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="数量"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-input-number
              v-model:value="form.quantity"
              :min="0"
              :precision="2"
              style="width: 100%"
              :placeholder="quantityUnit"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="单价"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-input-number
              v-model:value="form.unitPrice"
              :min="0"
              :precision="4"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="金额(元)"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
            required
          >
            <a-input-number
              v-model:value="form.amountYuan"
              :min="0"
              :precision="2"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            v-if="form.subjectType === 'VEHICLE'"
            label="仪表里程(km)"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-input-number
              v-model:value="form.odometer"
              :min="0"
              style="width: 100%"
              placeholder="用于自动算区间与每公里成本"
            />
          </a-form-item>
          <a-form-item
            v-else
            label="当次里程(km)"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-input-number
              v-model:value="form.odometer"
              :min="0"
              style="width: 100%"
              placeholder="月租套餐按里程分摊成本"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="补能时间"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-date-picker
              v-model:value="form.occurredAt"
              show-time
              style="width: 100%"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="站点/商户"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-input v-model:value="form.station" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="卡号/套餐"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-auto-complete
              v-model:value="form.cardNo"
              :options="energyCardOptions"
              placeholder="选卡或手输卡号"
              :filter-option="filterCardOption"
              @change="onCardChange"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="凭证"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-input
              v-model:value="form.voucherUrl"
              placeholder="发票/小票图片地址"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-alert
        v-if="form.payMode === 4"
        type="info"
        show-icon
        message="月租套餐（如骑手换电包月）：金额为月费，填写里程后按里程分摊出每公里成本"
        style="margin-bottom: 8px"
      />
      <a-form-item label="备注">
        <a-input v-model:value="form.remark" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { energyLogApi } from '@/api/dms/vehicle'
import { ENERGY_TYPE_OPTIONS, PAY_MODE_OPTIONS } from '../../config/verification-config'

const props = defineProps<{
  open: boolean
  record?: any
  vehicleOptions: any[]
  riderOptions: any[]
  energyCardOptions: { value: string; label: string }[]
  energyCardList: any[]
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  saved: []
}>()

const saving = ref(false)
const form = reactive<any>({
  id: undefined,
  subjectType: 'VEHICLE',
  vehicleId: undefined,
  riderId: undefined,
  energyType: 1,
  payMode: 1,
  quantity: undefined,
  unitPrice: undefined,
  amountYuan: undefined,
  odometer: undefined,
  occurredAt: undefined,
  station: '',
  cardNo: '',
  voucherUrl: '',
  remark: '',
})

const quantityUnit = computed(() => {
  switch (form.energyType) {
    case 3: return 'kWh'
    case 5: return 'm³'
    case 4: return '次'
    default: return 'L'
  }
})

const filterCardOption = (input: string, option: any) =>
  String(option?.value || '').toLowerCase().includes(String(input || '').toLowerCase())

// 打开时按当前记录回填（等价于原 openEnergyModal）
watch(() => props.open, (v) => {
  if (!v) return
  const record = props.record
  Object.assign(form, {
    id: record?.id,
    subjectType: record ? (record.subjectType === 'RIDER' ? 'RIDER' : 'VEHICLE') : 'VEHICLE',
    vehicleId: record?.vehicleId != null ? String(record.vehicleId) : undefined,
    riderId: record?.riderId != null ? String(record.riderId) : undefined,
    energyType: record?.energyType ?? 1,
    payMode: record?.payMode ?? 1,
    quantity: record?.quantity,
    unitPrice: record?.unitPrice,
    amountYuan: record?.amountYuan,
    odometer: record?.odometer,
    occurredAt: record?.occurredAt || dayjs().format('YYYY-MM-DD HH:mm:ss'),
    station: record?.station || '',
    cardNo: record?.cardNo || '',
    voucherUrl: record?.voucherUrl || '',
    remark: record?.remark || '',
  })
})

/**
 * 选卡后自动带出绑定主体与对应补能类型 ——
 * 「一卡一车一人」的前置对齐（避免用 A 车卡给 B 车记账而被稽核标记）
 */
function onCardChange(cardNo: any) {
  const card = props.energyCardList.find(c => c.cardNo === cardNo)
  if (!card) {
    return
  }
  if (card.riderId != null) {
    form.subjectType = 'RIDER'
    form.riderId = String(card.riderId)
  } else if (card.vehicleId != null) {
    form.subjectType = 'VEHICLE'
    form.vehicleId = String(card.vehicleId)
  }
  const typeMap: Record<number, number> = { 1: 1, 2: 3, 3: 4, 4: 3, 5: 5 }
  if (typeMap[card.cardType]) {
    form.energyType = typeMap[card.cardType]
  }
  if (!form.payMode || form.payMode === 1) {
    form.payMode = card.cardType === 1 ? 2 : (card.cardType === 5 ? 1 : (card.cardType >= 3 ? 4 : 3))
  }
}

function onSubjectChange() {
  form.vehicleId = undefined
  form.riderId = undefined
}

async function onSubmit() {
  if (form.subjectType === 'VEHICLE' && !form.vehicleId) {
    message.warning('请选择车辆')
    return
  }
  if (form.subjectType === 'RIDER' && !form.riderId) {
    message.warning('请选择配送员')
    return
  }
  if (form.amountYuan == null || Number(form.amountYuan) <= 0) {
    message.warning('请填写补能金额')
    return
  }
  saving.value = true
  try {
    const payload = {
      vehicleId: form.subjectType === 'VEHICLE' ? form.vehicleId : undefined,
      riderId: form.subjectType === 'RIDER' ? form.riderId : undefined,
      energyType: form.energyType,
      payMode: form.payMode,
      quantity: form.quantity,
      unitPrice: form.unitPrice,
      amountYuan: form.amountYuan,
      odometer: form.odometer,
      occurredAt: form.occurredAt || undefined,
      station: form.station || undefined,
      cardNo: form.cardNo || undefined,
      voucherUrl: form.voucherUrl || undefined,
      remark: form.remark || undefined,
    }
    const res: any = form.id
      ? await energyLogApi.update(form.id, payload)
      : await energyLogApi.create(payload)
    const saved = res?.data ?? res
    emit('update:open', false)
    if (Number(saved?.abnormalFlag) === 1) {
      message.warning(`已保存，但标记为异常：${saved.abnormalReason || ''}`)
    } else if (saved?.unitCost != null) {
      message.success(`已保存：区间 ${saved.mileageSinceLast ?? '-'} km，每公里成本 ${saved.unitCost} 元`)
    } else {
      message.success('已保存')
    }
    emit('saved')
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>
