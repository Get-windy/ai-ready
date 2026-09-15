<template>
  <a-modal
    :open="open"
    title="交车（收车登记 + 收车后检查）"
    :width="780"
    :confirm-loading="saving"
    :mask-closable="false"
    @ok="onOk"
    @cancel="emit('update:open', false)"
  >
    <!-- 工具栏进入时先选「绑定中」记录 -->
    <a-form
      v-if="!cur"
      :label-col="{ span: 4 }"
      :wrapper-col="{ span: 20 }"
      size="small"
    >
      <a-form-item label="待交车辆">
        <a-select
          v-model:value="pickId"
          show-search
          option-filter-prop="label"
          placeholder="选择「绑定中」的人车绑定"
          :options="activeBindingOptions"
          @change="onPickChange"
        />
      </a-form-item>
    </a-form>

    <template v-else>
      <a-descriptions
        :column="3"
        bordered
        size="small"
        style="margin-bottom: 12px"
      >
        <a-descriptions-item label="配送员">
          {{ cur.riderName }}
        </a-descriptions-item>
        <a-descriptions-item label="车牌号">
          {{ cur.plateNo }}
        </a-descriptions-item>
        <a-descriptions-item label="出车里程">
          {{ cur.bindMileage ?? '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="出车时间">
          {{ formatDateTime(cur.bindTime) }}
        </a-descriptions-item>
        <a-descriptions-item
          label="状态"
          :span="2"
        >
          <a-tag :color="BINDING_STATUS_MAP[cur.status]?.color">
            {{ BINDING_STATUS_MAP[cur.status]?.text }}
          </a-tag>
        </a-descriptions-item>
      </a-descriptions>

      <a-form
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 20 }"
        size="small"
      >
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="交车里程(km)">
              <a-input-number
                v-model:value="form.handoverMileage"
                :min="cur.bindMileage || 0"
                size="small"
                style="width: 100%"
                :placeholder="`不得小于出车里程 ${cur.bindMileage ?? 0}`"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="交车地点">
              <a-input
                v-model:value="form.handoverLocation"
                size="small"
                placeholder="请输入交车地点"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>

      <a-divider style="margin: 4px 0 12px">
        收车后检查（异常项将自动转《车辆维护》并置车辆「维修中」）
      </a-divider>
      <VehicleCheckForm
        ref="checkRef"
        v-model="check"
        hide-mileage
        hide-remark
      />

      <a-form
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 20 }"
        size="small"
      >
        <a-form-item label="备注">
          <a-textarea
            v-model:value="form.remark"
            :rows="2"
          />
        </a-form-item>
      </a-form>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { message } from 'ant-design-vue'
import { verificationApi } from '@/api/dms/verification'
import VehicleCheckForm from '../VehicleCheckForm.vue'
import type { VehicleCheckData } from '../VehicleCheckForm.vue'
import { emptyCheck, BINDING_STATUS_MAP, formatDateTime } from '../../config/verification-config'

const props = defineProps<{
  open: boolean
  record?: any
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  saved: []
}>()

const saving = ref(false)
/** 当前待交记录（工具进入时可能为空 → 由下拉选择） */
const cur = ref<any>(null)
const pickId = ref<string | undefined>(undefined)
const checkRef = ref<any>(null)
const check = ref<VehicleCheckData>(emptyCheck())
const form = reactive<any>({ handoverMileage: undefined, handoverLocation: '', remark: '' })
/** 工具栏进入时的「绑定中」候选 */
const activeBindingOptions = ref<{ value: string; label: string }[]>([])

// 打开时初始化（等价于原 openHandover）
watch(() => props.open, (v) => {
  if (v) init()
})

async function init() {
  const r = props.record || null
  cur.value = r
  pickId.value = r ? String(r.id) : undefined
  check.value = emptyCheck()
  form.handoverMileage = r?.bindMileage ?? undefined
  form.handoverLocation = ''
  form.remark = ''
  if (!r) {
    try {
      const res: any = await verificationApi.bindingPage({ page: 1, size: 100, status: 0 })
      const list = (res?.data ?? res)?.records || []
      activeBindingOptions.value = list.map((b: any) => ({
        value: String(b.id),
        label: `${b.plateNo || ''} / ${b.riderName || ''}（出车里程 ${b.bindMileage ?? '-'}）`,
      }))
      // 不默认选中：交车必须由操作人明确指定车辆，避免交错车
      pickId.value = undefined
      cur.value = null
      if (activeBindingOptions.value.length === 0) {
        message.warning('当前没有「绑定中」的人车记录，无需交车')
      }
    } catch (e) {
      console.warn('[实名认证] 加载绑定中记录失败', e)
    }
  }
}

/** 选中待交车辆后带出出车里程（交车里程默认=出车里程，且不得更小） */
async function onPickChange(value: any) {
  if (!value) {
    cur.value = null
    return
  }
  const res: any = await verificationApi.bindingDetail(value)
  cur.value = (res?.data ?? res)?.binding || null
  form.handoverMileage = cur.value?.bindMileage ?? undefined
}

/** 交车里程即收车检查里程（单一来源），交车备注即检查备注 */
function buildReturnInspection(): Record<string, any> {
  const { mileage: _ignored, remark: _ignoredRemark, ...rest } = check.value
  return {
    inspectionType: 2,
    mileage: form.handoverMileage,
    ...rest,
  }
}

async function onOk() {
  if (!cur.value?.id) {
    message.warning('请选择待交车辆')
    return
  }
  if (form.handoverMileage == null) {
    message.warning('请填写交车里程')
    return
  }
  saving.value = true
  try {
    const failed = checkRef.value?.willFail
    await verificationApi.bindingHandover(cur.value.id, {
      handoverMileage: form.handoverMileage,
      handoverLocation: form.handoverLocation || undefined,
      remark: form.remark || undefined,
      inspection: buildReturnInspection(),
    })
    if (failed) {
      message.warning('交车完成，但收车检查存在异常：车辆已置「维修中」并自动生成《车辆维护》维修待办')
    } else {
      message.success('交车成功（已完成收车后检查）')
    }
    emit('update:open', false)
    cur.value = null
    emit('saved')
  } catch (e: any) {
    message.error(e?.message || '交车失败')
  } finally {
    saving.value = false
  }
}
</script>
