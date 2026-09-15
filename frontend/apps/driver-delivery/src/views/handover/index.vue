<template>
  <div class="handover-page">
    <NavBar title="交车" left-arrow @click-left="onBack" />

    <div v-if="loading" style="text-align: center; padding: 40px 0;">
      <Loading />
    </div>

    <template v-else>
      <!-- 当前绑定（服务端按登录用户反查配送员，不再用 userId 当 riderId） -->
      <CellGroup v-if="bindingInfo" title="当前绑定信息">
        <Cell title="车牌号" :value="bindingInfo.plateNo || '--'" />
        <Cell title="出车时间" :value="fmtTime(bindingInfo.bindTime)" />
        <Cell title="出车里程(km)" :value="String(bindingInfo.bindMileage ?? '--')" />
      </CellGroup>

      <CellGroup v-else title="当前绑定信息">
        <Cell title="状态" value="暂无绑定信息，请先完成出车登记" />
      </CellGroup>

      <Form v-if="bindingInfo" class="handover-form" @submit="onSubmit">
        <Field
          v-model="form.handoverMileage"
          name="handoverMileage"
          label="交车里程(km)"
          type="digit"
          :placeholder="`不得小于出车里程 ${bindingInfo.bindMileage ?? 0}`"
          :rules="[{ required: true, message: '请输入交车里程' }]"
        />
        <Field
          v-model="form.handoverLocation"
          name="handoverLocation"
          label="交车地点"
          placeholder="请输入交车地点（选填）"
        />

        <!-- 收车后检查（必做单证：异常将自动转《车辆维护》并把车辆置为维修中） -->
        <CellGroup title="收车后检查">
          <template v-for="item in CHECK_ITEMS" :key="item.key">
            <Cell :title="item.label">
              <template #value>
                <RadioGroup v-model="form.check[item.key]" direction="horizontal">
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
              v-if="form.check[item.key] === 1"
              v-model="form.check[item.remarkKey]"
              :label="`${item.label}异常描述`"
              placeholder="请描述异常现象"
            />
          </template>
          <Cell title="灭火器">
            <template #value>
              <RadioGroup v-model="form.check.fireExtinguisher" direction="horizontal">
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
              <RadioGroup v-model="form.check.warningTriangle" direction="horizontal">
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
          name="remark"
          label="备注"
          type="textarea"
          rows="2"
          autosize
          placeholder="请输入备注（选填）"
        />

        <NoticeBar
          v-if="willFail"
          type="danger"
          :text="`存在否决项（${vetoText}）：交车后车辆将置「维修中」并自动生成《车辆维护》待办`"
          wrapable
          :scrollable="false"
        />

        <div style="margin: 20px 16px;">
          <Button round block type="primary" native-type="submit" :loading="submitting">
            确认交车
          </Button>
        </div>
      </Form>
    </template>

    <Dialog
      v-model:show="showSuccess"
      :title="successTitle"
      :message="successMessage"
      @confirm="onSuccessConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  NavBar, Form, Field, Cell, CellGroup, Button, Dialog, Toast, Loading, Radio, RadioGroup, NoticeBar,
} from 'vant'
import { dmsApi } from '@/api/dms'

const router = useRouter()

/** 检查项（key 与后端字段一致） */
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

const loading = ref(false)
const submitting = ref(false)
const showSuccess = ref(false)
const successTitle = ref('交车成功')
const successMessage = ref('交车操作已完成，即将返回主页')
const bindingInfo = ref<any>(null)
const bindingId = ref<string | null>(null)

const form = reactive<any>({
  handoverMileage: undefined,
  handoverLocation: '',
  remark: '',
  check: {
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
  },
})

const abnormalItems = computed(() => VETO_ITEMS.filter(i => form.check[i.key] === 1).map(i => i.label))
const willFail = computed(() => abnormalItems.value.length > 0)
const vetoText = computed(() => abnormalItems.value.join('、'))

const fetchBindingInfo = async () => {
  loading.value = true
  try {
    const riderRes: any = await dmsApi.getMyRider()
    const rider = riderRes?.data ?? riderRes
    if (!rider?.id) {
      return
    }
    const res: any = await dmsApi.getActiveBinding(rider.id)
    const data = res?.data ?? res ?? null
    if (data && data.id) {
      bindingInfo.value = data
      bindingId.value = String(data.id)
      form.handoverMileage = data.bindMileage ?? undefined
    }
  } catch {
    // 无绑定 / 未关联配送员：展示空态
  } finally {
    loading.value = false
  }
}

/** 日期时间格式化（后端返回 ISO，直接展示会有 T 与微秒） */
const fmtTime = (v?: string) => (v ? String(v).replace('T', ' ').slice(0, 19) : '--')

const onBack = () => {
  router.back()
}

const onSubmit = async () => {
  if (!bindingId.value) {
    Toast.fail('未找到当前绑定信息，请先完成出车登记')
    return
  }
  if (form.handoverMileage == null) {
    Toast.fail('请输入交车里程')
    return
  }
  if (bindingInfo.value?.bindMileage != null && Number(form.handoverMileage) < Number(bindingInfo.value.bindMileage)) {
    Toast.fail(`交车里程不得小于出车里程 ${bindingInfo.value.bindMileage}`)
    return
  }

  submitting.value = true
  try {
    // 定位（失败不阻断，缺省不带经纬度）
    let coords: { lat?: number; lng?: number } = {}
    try {
      const pos = await new Promise<GeolocationPosition>((resolve, reject) => {
        navigator.geolocation.getCurrentPosition(resolve, reject, { enableHighAccuracy: true, timeout: 8000 })
      })
      coords = { lat: pos.coords.latitude, lng: pos.coords.longitude }
    } catch {
      coords = {}
    }

    await dmsApi.handover(bindingId.value, {
      handoverMileage: Number(form.handoverMileage),
      handoverLocation: form.handoverLocation || undefined,
      handoverLat: coords.lat,
      handoverLng: coords.lng,
      remark: form.remark || undefined,
      inspection: { inspectionType: 2, mileage: Number(form.handoverMileage), ...form.check },
    })
    successTitle.value = willFail.value ? '交车完成（检查异常）' : '交车成功'
    successMessage.value = willFail.value
      ? `${vetoText.value}存在异常，车辆已置「维修中」并自动生成《车辆维护》待办`
      : '交车操作已完成，即将返回主页'
    showSuccess.value = true
  } catch (err: any) {
    Toast.fail(err?.message || '交车失败')
  } finally {
    submitting.value = false
  }
}

const onSuccessConfirm = () => {
  router.push({ path: '/' })
}

onMounted(fetchBindingInfo)
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
