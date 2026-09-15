<template>
  <a-modal
    :open="open"
    :title="form.id ? '修改补能卡' : '新增补能卡'"
    :width="640"
    :confirm-loading="saving"
    :mask-closable="false"
    @ok="onSubmit"
    @cancel="emit('update:open', false)"
  >
    <a-alert
      type="info"
      show-icon
      message="一卡一车一人：一张卡只能绑定一个主体（四轮车 或 配送员），补能时用卡主体必须与绑定主体一致，否则自动标记异常"
      style="margin-bottom: 12px"
    />
    <a-form
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
      size="small"
    >
      <a-row :gutter="12">
        <a-col :span="12">
          <a-form-item
            label="卡号/套餐号"
            required
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-input
              v-model:value="form.cardNo"
              placeholder="唯一，用于补能稽核"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="卡名称"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-input v-model:value="form.cardName" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="卡类型"
            required
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-select
              v-model:value="form.cardType"
              :options="CARD_TYPE_OPTIONS"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="发卡方"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-input v-model:value="form.issuer" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="绑定主体"
            required
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-radio-group
              v-model:value="form.subjectType"
              @change="onSubjectChange"
            >
              <a-radio-button value="VEHICLE">
                四轮车
              </a-radio-button>
              <a-radio-button value="RIDER">
                配送员
              </a-radio-button>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            v-if="form.subjectType === 'VEHICLE'"
            label="车辆"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-select
              v-model:value="form.vehicleId"
              show-search
              option-filter-prop="label"
              :options="vehicleOptions"
            />
          </a-form-item>
          <a-form-item
            v-else
            label="配送员"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-select
              v-model:value="form.riderId"
              show-search
              option-filter-prop="label"
              :options="riderOptions"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="月费(元)"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-input-number
              v-model:value="form.monthlyFee"
              :min="0"
              :precision="2"
              style="width: 100%"
              placeholder="月租套餐填"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="余额(元)"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-input-number
              v-model:value="form.balance"
              :precision="2"
              style="width: 100%"
              placeholder="储值卡填"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="额度"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-input-number
              v-model:value="form.quota"
              :min="0"
              :precision="2"
              style="width: 100%"
              placeholder="留空=不限量"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="生效日期"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-date-picker
              v-model:value="form.startDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="有效期至"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-date-picker
              v-model:value="form.expireDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="状态"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-select
              v-model:value="form.status"
              :options="[{ value: 1, label: '启用' }, { value: 0, label: '停用' }]"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="备注">
        <a-input v-model:value="form.remark" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { message } from 'ant-design-vue'
import { energyCardApi } from '@/api/dms/vehicle'
import { CARD_TYPE_OPTIONS } from '../../config/verification-config'

const props = defineProps<{
  open: boolean
  record?: any
  vehicleOptions: any[]
  riderOptions: any[]
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  saved: []
}>()

const saving = ref(false)
const form = reactive<any>({
  id: undefined,
  cardNo: '',
  cardName: '',
  cardType: 1,
  subjectType: 'VEHICLE',
  vehicleId: undefined,
  riderId: undefined,
  issuer: '',
  monthlyFee: undefined,
  balance: undefined,
  quota: undefined,
  startDate: undefined,
  expireDate: undefined,
  status: 1,
  remark: '',
})

// 打开时按当前记录回填（等价于原 openCardModal）
watch(() => props.open, (v) => {
  if (!v) return
  const record = props.record
  Object.assign(form, {
    id: record?.id,
    cardNo: record?.cardNo || '',
    cardName: record?.cardName || '',
    cardType: record?.cardType ?? 1,
    subjectType: record?.subjectType || (record?.riderId ? 'RIDER' : 'VEHICLE'),
    vehicleId: record?.vehicleId != null ? String(record.vehicleId) : undefined,
    riderId: record?.riderId != null ? String(record.riderId) : undefined,
    issuer: record?.issuer || '',
    monthlyFee: record?.monthlyFee,
    balance: record?.balance,
    quota: record?.quota,
    startDate: record?.startDate || undefined,
    expireDate: record?.expireDate || undefined,
    status: record?.status ?? 1,
    remark: record?.remark || '',
  })
})

function onSubjectChange() {
  form.vehicleId = undefined
  form.riderId = undefined
}

async function onSubmit() {
  if (!form.cardNo) {
    message.warning('请填写卡号/套餐号')
    return
  }
  if (form.subjectType === 'VEHICLE' && !form.vehicleId) {
    message.warning('请选择绑定车辆（一卡一车一人）')
    return
  }
  if (form.subjectType === 'RIDER' && !form.riderId) {
    message.warning('请选择绑定配送员（一卡一车一人）')
    return
  }
  saving.value = true
  try {
    const payload = {
      cardNo: form.cardNo,
      cardName: form.cardName || undefined,
      cardType: form.cardType,
      vehicleId: form.subjectType === 'VEHICLE' ? form.vehicleId : undefined,
      riderId: form.subjectType === 'RIDER' ? form.riderId : undefined,
      issuer: form.issuer || undefined,
      monthlyFee: form.monthlyFee,
      balance: form.balance,
      quota: form.quota,
      startDate: form.startDate || undefined,
      expireDate: form.expireDate || undefined,
      status: form.status,
      remark: form.remark || undefined,
    }
    if (form.id) {
      await energyCardApi.update(form.id, payload)
    } else {
      await energyCardApi.create(payload)
    }
    message.success('已保存')
    emit('update:open', false)
    emit('saved')
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>
