<template>
  <div class="vehicle-check-form">
    <a-row :gutter="12">
      <a-col
        v-if="!hideMileage"
        :span="8"
      >
        <a-form-item :label="mileageLabel">
          <a-input-number
            v-model:value="model.mileage"
            :min="0"
            size="small"
            style="width: 100%"
            placeholder="仪表里程(km)"
          />
        </a-form-item>
      </a-col>
      <a-col :span="8">
        <a-form-item label="油量/电量(%)">
          <a-input-number
            v-model:value="model.fuelLevel"
            :min="0"
            :max="100"
            size="small"
            style="width: 100%"
            placeholder="0-100"
          />
        </a-form-item>
      </a-col>
      <a-col :span="8">
        <a-form-item label="检查地点">
          <a-input
            v-model:value="model.inspectionLocation"
            size="small"
            placeholder="停车场 / 站点名称"
          />
        </a-form-item>
      </a-col>
    </a-row>

    <!-- 车况检查项：0-正常 1-异常（异常项会随检查单一并留档并触发拦截） -->
    <a-form-item
      v-for="item in CHECK_ITEMS"
      :key="item.key"
      :label="item.label"
      :label-col="{ span: 4 }"
      :wrapper-col="{ span: 20 }"
    >
      <a-radio-group
        v-model:value="model[item.key]"
        size="small"
        style="margin-right: 12px"
      >
        <a-radio-button :value="0">
          正常
        </a-radio-button>
        <a-radio-button :value="1">
          异常
        </a-radio-button>
      </a-radio-group>
      <a-input
        v-if="model[item.key] === 1"
        v-model:value="model[item.remarkKey]"
        size="small"
        style="width: 260px"
        :placeholder="`${item.label}异常描述`"
      />
    </a-form-item>

    <a-row :gutter="12">
      <a-col :span="8">
        <a-form-item label="灭火器">
          <a-select
            v-model:value="model.fireExtinguisher"
            size="small"
            style="width: 100%"
            :options="[{ value: 0, label: '正常' }, { value: 1, label: '缺失/过期' }]"
          />
        </a-form-item>
      </a-col>
      <a-col :span="8">
        <a-form-item label="三角警示牌">
          <a-select
            v-model:value="model.warningTriangle"
            size="small"
            style="width: 100%"
            :options="[{ value: 0, label: '有' }, { value: 1, label: '缺失' }]"
          />
        </a-form-item>
      </a-col>
      <a-col :span="8">
        <a-form-item label="外观照片">
          <a-input
            v-model:value="model.exteriorPhotos"
            size="small"
            placeholder="照片地址或点击上传"
          >
            <template #suffix>
              <a @click.prevent="pickPhoto">上传</a>
            </template>
          </a-input>
        </a-form-item>
      </a-col>
    </a-row>

    <a-form-item
      v-if="!hideRemark"
      label="备注"
    >
      <a-input
        v-model:value="model.remark"
        size="small"
        placeholder="检查备注"
      />
    </a-form-item>

    <!-- 拦截口径与后端一致：刹车 / 灯光 / 灭火器 任一异常即不通过 -->
    <a-alert
      v-if="willFail"
      type="error"
      show-icon
      :message="`存在否决项（${abnormalItems.join('、')}），将判定为「不通过」`"
    />
    <input
      ref="fileRef"
      type="file"
      accept="image/*"
      style="display: none"
      @change="onPhotoChange"
    >
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import request from '@/utils/request'

/** 检查表单数据（出车前 / 收车后 / 抽检定检三处共用同一结构） */
export interface VehicleCheckData {
  mileage?: number
  fuelLevel?: number
  exteriorStatus?: number
  exteriorRemark?: string
  exteriorPhotos?: string
  tireStatus?: number
  tireRemark?: string
  lightStatus?: number
  lightRemark?: string
  brakeStatus?: number
  brakeRemark?: string
  cleanlinessStatus?: number
  cleanlinessRemark?: string
  fireExtinguisher?: number
  warningTriangle?: number
  inspectionLocation?: string
  remark?: string
}

const model = defineModel<VehicleCheckData>({ required: true })

withDefaults(defineProps<{
  /** 里程项标签：出车/收车/抽检语义不同 */
  mileageLabel?: string
  /** 隐藏里程输入（由父级统一提供，如交车场景用「交车里程」） */
  hideMileage?: boolean
  /** 隐藏备注输入（由父级统一提供，如交车场景用交车备注） */
  hideRemark?: boolean
}>(), {
  mileageLabel: '里程(km)',
  hideMileage: false,
  hideRemark: false,
})

/** 检查项（key 与后端字段一致；remarkKey 为对应异常描述字段） */
const CHECK_ITEMS = [
  { key: 'exteriorStatus', label: '车辆外观', remarkKey: 'exteriorRemark' },
  { key: 'tireStatus', label: '轮胎', remarkKey: 'tireRemark' },
  { key: 'lightStatus', label: '灯光', remarkKey: 'lightRemark' },
  { key: 'brakeStatus', label: '刹车', remarkKey: 'brakeRemark' },
  { key: 'cleanlinessStatus', label: '车内清洁', remarkKey: 'cleanlinessRemark' },
] as const

/** 否决项（与后端 evaluateResult 同口径） */
const VETO_ITEMS = [
  { key: 'brakeStatus', label: '刹车' },
  { key: 'lightStatus', label: '灯光' },
  { key: 'fireExtinguisher', label: '灭火器' },
] as const

const abnormalItems = computed(() =>
  VETO_ITEMS.filter(i => model.value?.[i.key] === 1).map(i => i.label))
const willFail = computed(() => abnormalItems.value.length > 0)

const fileRef = ref<HTMLInputElement>()
function pickPhoto() {
  fileRef.value?.click()
}

async function onPhotoChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res: any = await request.post('/file/upload', formData)
    const url = typeof res === 'string' ? res : (res?.url || res?.data?.url)
    if (!url) {
      message.error('照片上传失败')
      return
    }
    model.value.exteriorPhotos = model.value.exteriorPhotos
      ? `${model.value.exteriorPhotos},${url}` : url
  } catch {
    message.error('照片上传失败')
  }
}

defineExpose({ abnormalItems, willFail })
</script>

<style scoped>
.vehicle-check-form :deep(.ant-form-item) {
  margin-bottom: 10px;
}
</style>
