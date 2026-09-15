<script setup lang="ts">
/**
 * 司机端「签收」—— 真实提交到 DMS 签收接口（`POST /api/dms/sign/submit`）。
 *
 * 2026-09-13：原页面提交到不存在的 `POST /orders/{id}/sign`，且只采集"签名 + 备注"，
 * 与《签收管理开发文档》§3.6 的四要素/场景强制不一致。本次对齐：
 *   1) 四要素：照片 + 手写签名 + 定位 + 时间戳（服务端落库）；
 *   2) 签收类型 1 正常 / 2 部分 / 3 拒收（与后端 `SignTypeEnum` 同一套枚举，§3.6.8）；
 *   3) 部分签收必填实际数量；拒收必填原因 + 照片（服务端强校验，前端先拦一道）；
 *   4) 照片/签名先上传通用文件服务换取 URL，再提交；定位取浏览器 GPS 由服务端算偏差（§3.6.2）。
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NavBar, Field, Button, Uploader, RadioGroup, Radio, CellGroup, Cell,
  showToast, showLoadingToast, closeToast, showConfirmDialog,
} from 'vant'
import { dmsApi, uploadFile, dataUrlToBlob, SIGN_TYPE, SIGN_TYPE_OPTIONS } from '@/api/dms'
import SignaturePad from '@/components/common/SignaturePad.vue'

const router = useRouter()
const route = useRoute()

const taskId = computed(() => route.params.id as string)

const detail = ref<any>(null)
const signType = ref<number>(SIGN_TYPE.NORMAL)
const receiverName = ref('')
const receiverPhone = ref('')
const actualQuantity = ref<string>('')
const rejectReason = ref('')
const remark = ref('')
const signatureImage = ref('')
const photos = ref<any[]>([])
const location = ref<{ lat: number; lng: number } | null>(null)
const locationError = ref('')
const submitting = ref(false)

const plannedQuantity = computed(() => detail.value?.totalQuantity ?? null)

const isPartial = computed(() => signType.value === SIGN_TYPE.PARTIAL)
const isReject = computed(() => signType.value === SIGN_TYPE.REJECT)

/** 切换签收类型时清理不适用的字段，避免把脏值带进提交 */
watch(signType, (val) => {
  if (val !== SIGN_TYPE.PARTIAL) actualQuantity.value = ''
  if (val !== SIGN_TYPE.REJECT) rejectReason.value = ''
})

onMounted(async () => {
  try {
    const res: any = await dmsApi.getTaskDetail(taskId.value)
    detail.value = res?.data ?? res ?? null
    receiverName.value = detail.value?.customerName || ''
    receiverPhone.value = detail.value?.customerPhone || ''
  } catch {
    detail.value = null
  }
  // 已有待审核签收记录时不允许重复提交（服务端也会拦，前端先提示）
  try {
    const sres: any = await dmsApi.getTaskSign(taskId.value)
    const exist = sres?.data ?? null
    if (exist && exist.auditStatus === 0) {
      showConfirmDialog({
        title: '已提交签收',
        message: '该任务已提交签收、等待审核，请勿重复提交。',
        showCancelButton: false,
      }).then(() => router.replace(`/delivery/${taskId.value}`)).catch(() => {})
    }
  } catch { /* 无签收记录属正常 */ }
  getCurrentLocation()
})

const getCurrentLocation = () => {
  if (!navigator.geolocation) {
    locationError.value = '设备不支持定位，将不带签收位置提交'
    return
  }
  navigator.geolocation.getCurrentPosition(
    (position) => {
      location.value = { lat: position.coords.latitude, lng: position.coords.longitude }
      locationError.value = ''
    },
    () => { locationError.value = '定位获取失败，可继续提交（管理端将标记为无位置）' },
    { enableHighAccuracy: true, timeout: 8000 },
  )
}

const handleSignatureComplete = (image: string) => {
  signatureImage.value = image
}

const afterRead = async (item: any) => {
  const items = Array.isArray(item) ? item : [item]
  for (const it of items) {
    it.status = 'uploading'
    it.message = '上传中'
    try {
      it.url = await uploadFile(it.file, it.file?.name)
      it.status = 'done'
      it.message = ''
    } catch {
      it.status = 'failed'
      it.message = '上传失败'
      showToast('照片上传失败')
    }
  }
}

const handlePhotoDelete = (item: any) => {
  const idx = photos.value.indexOf(item)
  if (idx >= 0) photos.value.splice(idx, 1)
}

const photoUrls = () => photos.value.filter((p) => p.status === 'done' && p.url).map((p) => p.url)

const validate = (): string => {
  if (isReject.value) {
    if (!photoUrls().length) return '拒收必须上传照片凭证'
    if (!rejectReason.value.trim()) return '拒收必须填写拒收原因'
    return ''
  }
  if (!receiverName.value.trim()) return '请填写签收人姓名'
  if (!signatureImage.value) return '请完成手写签名'
  if (isPartial.value) {
    const qty = Number(actualQuantity.value)
    if (!actualQuantity.value || Number.isNaN(qty) || qty < 0) return '部分签收必须填写实际签收数量'
    if (plannedQuantity.value != null && qty > Number(plannedQuantity.value)) {
      return `实际签收数量不能大于应签收数量 ${plannedQuantity.value}`
    }
  }
  return ''
}

const handleSubmit = async () => {
  const err = validate()
  if (err) {
    showToast({ type: 'fail', message: err })
    return
  }
  showConfirmDialog({ title: '签收确认', message: '确定提交签收信息吗？' })
    .then(async () => {
      await doSubmit()
    })
    .catch(() => { /* 取消 */ })
}

const doSubmit = async () => {
  submitting.value = true
  showLoadingToast({ message: '提交中...', forbidClick: true })
  try {
    let signatureUrl = ''
    if (signatureImage.value) {
      signatureUrl = await uploadFile(dataUrlToBlob(signatureImage.value), `sign-${taskId.value}.png`)
    }
    const payload: any = {
      taskId: taskId.value,
      signType: signType.value,
      signatureUrl: signatureUrl || undefined,
      photoUrls: photoUrls().length ? JSON.stringify(photoUrls()) : undefined,
      remark: (isReject.value ? rejectReason.value : remark.value) || undefined,
    }
    if (location.value) {
      payload.signLat = location.value.lat
      payload.signLng = location.value.lng
    }
    if (isPartial.value) {
      payload.actualQuantity = Number(actualQuantity.value)
    }
    await dmsApi.submitSign(payload)
    showToast({ type: 'success', message: '签收成功，等待审核' })
    setTimeout(() => router.replace(`/delivery/${taskId.value}`), 600)
  } catch (e: any) {
    showToast({ type: 'fail', message: e?.message || '签收提交失败' })
  } finally {
    submitting.value = false
    closeToast()
  }
}
</script>

<template>
  <div class="sign-page">
    <NavBar title="签收" left-arrow @click-left="router.back()" />

    <div class="sign-content">
      <div class="task-brief">
        <div class="line"><span class="label">任务编号</span><span>{{ detail?.taskNo || taskId }}</span></div>
        <div class="line"><span class="label">客户</span><span>{{ detail?.customerName || '-' }}</span></div>
        <div class="line"><span class="label">应签收</span><span>{{ plannedQuantity != null ? plannedQuantity : '-' }}</span></div>
      </div>

      <CellGroup inset title="签收类型">
        <Cell>
          <RadioGroup v-model="signType" direction="horizontal">
            <Radio
              v-for="opt in SIGN_TYPE_OPTIONS"
              :key="opt.value"
              :name="opt.value"
            >
              {{ opt.text }}
            </Radio>
          </RadioGroup>
        </Cell>
      </CellGroup>

      <template v-if="isReject">
        <div class="section-title">拒收原因（必填）</div>
        <Field
          v-model="rejectReason"
          type="textarea"
          rows="3"
          maxlength="200"
          show-word-limit
          placeholder="请填写拒收原因，将同步给管理端审核"
        />
      </template>

      <template v-else>
        <div class="section-title">签收人信息</div>
        <Field v-model="receiverName" label="签收人" placeholder="请输入签收人姓名" />
        <Field v-model="receiverPhone" label="联系电话" placeholder="请输入联系电话" type="tel" />

        <template v-if="isPartial">
          <div class="section-title">实际签收数量（必填）</div>
          <Field
            v-model="actualQuantity"
            type="number"
            label="实收数量"
            :placeholder="plannedQuantity != null ? `应签收 ${plannedQuantity}` : '请输入实际签收数量'"
          />
        </template>

        <div class="section-title">签名</div>
        <div class="signature-container">
          <SignaturePad @complete="handleSignatureComplete" />
        </div>
        <div class="tip" v-if="signatureImage">已采集手写签名</div>
      </template>

      <div class="section-title">拍照凭证{{ isReject ? '（必传）' : '' }}</div>
      <Uploader
        v-model="photos"
        :max-count="5"
        :after-read="afterRead"
        @delete="handlePhotoDelete"
      />

      <template v-if="!isReject">
        <div class="section-title">备注</div>
        <Field v-model="remark" label="备注" placeholder="选填" type="textarea" rows="2" />
      </template>

      <div class="location-tip" :class="{ error: !!locationError }">
        <template v-if="location">已获取签收定位：{{ location.lat.toFixed(6) }}, {{ location.lng.toFixed(6) }}</template>
        <template v-else>{{ locationError || '正在获取定位…' }}</template>
      </div>

      <div class="submit-section">
        <Button
          type="primary"
          size="large"
          block
          :loading="submitting"
          :disabled="submitting"
          @click="handleSubmit"
        >
          提交签收
        </Button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.sign-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.sign-content {
  padding: 12px;
}

.task-brief {
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;

  .line {
    display: flex;
    font-size: 14px;
    padding: 4px 0;

    .label {
      width: 72px;
      color: #969799;
      flex: none;
    }
  }
}

.section-title {
  font-size: 14px;
  color: #969799;
  padding: 12px 4px 8px;
}

.signature-container {
  background: #fff;
  border-radius: 8px;
  padding: 8px;
}

.tip {
  font-size: 12px;
  color: #07c160;
  padding: 6px 4px 0;
}

.location-tip {
  font-size: 12px;
  color: #07c160;
  padding: 10px 4px 0;

  &.error {
    color: #ff976a;
  }
}

.submit-section {
  padding: 24px 0;
}
</style>
