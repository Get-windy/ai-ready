<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Field, Button, Cell, Picker, Popup, showToast, showLoadingToast, closeToast, Dialog } from 'vant'
import { api } from '@/api'

const router = useRouter()

const sourceLocation = ref('')
const targetLocation = ref('')
const productCode = ref('')
const productName = ref('')
const quantity = ref(1)
const remark = ref('')
const submitting = ref(false)

const showSourcePicker = ref(false)
const showTargetPicker = ref(false)
const locations = ref<{ text: string; value: string }[]>([])

onMounted(async () => {
  showLoadingToast({ message: '加载库位...', forbidClick: true })
  try {
    const res = await api.location.getList()
    const list = (Array.isArray(res) ? res : (res as any)?.records || (res as any)?.data || []) as any[]
    locations.value = list.map((loc: any) => ({
      text: loc.code + (loc.name ? ` - ${loc.name}` : ''),
      value: loc.code
    }))
  } catch (err) {
    console.error('[移库] 加载库位失败', err)
  } finally {
    closeToast()
  }
})

const handleSubmit = async () => {
  if (!sourceLocation.value) {
    showToast('请选择源库位')
    return
  }
  if (!targetLocation.value) {
    showToast('请选择目标库位')
    return
  }
  if (!productCode.value) {
    showToast('请输入商品编码')
    return
  }
  if (quantity.value <= 0) {
    showToast('请输入移库数量')
    return
  }

  Dialog.confirm({
    title: '确认移库',
    message: `将 ${productCode.value} 从 ${sourceLocation.value} 移至 ${targetLocation.value}，数量 ${quantity.value} 件？`
  }).then(async () => {
    submitting.value = true
    showLoadingToast({ message: '提交中...', forbidClick: true })

    try {
      await api.move.create({
        sourceLocationCode: sourceLocation.value,
        targetLocationCode: targetLocation.value,
        productCode: productCode.value,
        quantity: quantity.value,
        remark: remark.value
      })
      showToast('移库申请已提交')
      router.push('/move')
    } catch (err) {
      console.error('[移库] 提交失败', err)
      showToast('提交失败')
    } finally {
      submitting.value = false
      closeToast()
    }
  }).catch(() => {})
}

const onSourceConfirm = ({ selectedOptions }: any) => {
  sourceLocation.value = selectedOptions[0]?.value || ''
  showSourcePicker.value = false
}

const onTargetConfirm = ({ selectedOptions }: any) => {
  targetLocation.value = selectedOptions[0]?.value || ''
  showTargetPicker.value = false
}
</script>

<template>
  <div class="move-create-page">
    <NavBar
      title="创建移库"
      left-arrow
      @click-left="router.back()"
    />

    <div class="form-container">
      <Cell
        title="源库位"
        :value="sourceLocation || '请选择'"
        is-link
        @click="showSourcePicker = true"
      />

      <Cell
        title="目标库位"
        :value="targetLocation || '请选择'"
        is-link
        @click="showTargetPicker = true"
      />

      <Field
        v-model:value="productCode"
        label="商品编码"
        placeholder="请输入商品编码"
        clearable
      />

      <Field
        v-model:value="productName"
        label="商品名称"
        placeholder="请输入商品名称（可选）"
        clearable
      />

      <Field
        v-model:value="quantity"
        label="移库数量"
        placeholder="请输入移库数量"
        type="digit"
        clearable
      />

      <Field
        v-model:value="remark"
        label="备注"
        placeholder="请输入备注（可选）"
        type="textarea"
        rows="2"
      />

      <div class="submit-btn">
        <Button
          type="primary"
          size="large"
          :loading="submitting"
          @click="handleSubmit"
        >
          提交移库
        </Button>
      </div>
    </div>

    <Popup
      v-model:show="showSourcePicker"
      position="bottom"
      round
    >
      <Picker
        :columns="locations"
        @confirm="onSourceConfirm"
        @cancel="showSourcePicker = false"
        title="选择源库位"
      />
    </Popup>

    <Popup
      v-model:show="showTargetPicker"
      position="bottom"
      round
    >
      <Picker
        :columns="locations"
        @confirm="onTargetConfirm"
        @cancel="showTargetPicker = false"
        title="选择目标库位"
      />
    </Popup>
  </div>
</template>

<style lang="scss" scoped>
.move-create-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.form-container {
  padding: 16px;
  background: #fff;
  margin: 12px;
  border-radius: 8px;

  .submit-btn {
    margin-top: 24px;
  }
}
</style>
