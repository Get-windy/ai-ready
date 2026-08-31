<template>
  <ErrorBoundary>
    <PageContainer title="新建质检单">
      <template #extra>
        <a-space>
          <a-button @click="onCancel">取消</a-button>
          <a-button type="primary" :loading="saving" @click="handleSave">保存</a-button>
        </a-space>
      </template>

      <a-card :bordered="false">
        <a-form ref="formRef" :model="form" :label-col="{ span: 5 }" :wrapper-col="{ span: 16 }">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="来源单号" name="bizNo">
                <a-input v-model:value="form.bizNo" placeholder="关联入库/采购单号（可选）" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="产品名称" name="productName" :rules="[{ required: true, message: '请输入产品名称' }]">
                <a-input v-model:value="form.productName" placeholder="请输入产品名称" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="批次号" name="batchNo">
                <a-input v-model:value="form.batchNo" placeholder="批次号（可选）" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="检验类型" name="inspectionType">
                <a-select v-model:value="form.inspectionType">
                  <a-select-option value="INBOUND">入库检验</a-select-option>
                  <a-select-option value="OUTBOUND">出库检验</a-select-option>
                  <a-select-option value="PROCESS">过程检验</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="检验数量" name="quantity" :rules="[{ required: true, message: '请输入检验数量' }]">
                <a-input-number v-model:value="form.quantity" :min="0" style="width: 100%" placeholder="检验数量" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="抽检数量" name="sampleQuantity">
                <a-input-number v-model:value="form.sampleQuantity" :min="0" style="width: 100%" placeholder="抽检数量" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="检验结果" name="inspectionResult">
                <a-select v-model:value="form.inspectionResult">
                  <a-select-option value="PENDING">待检验</a-select-option>
                  <a-select-option value="PASS">合格</a-select-option>
                  <a-select-option value="CONCESSION">让步接收</a-select-option>
                  <a-select-option value="FAIL">不合格</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="检验员" name="inspectorName">
                <a-input v-model:value="form.inspectorName" placeholder="检验员" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="检验时间" name="inspectionTime">
                <a-date-picker v-model:value="form.inspectionTime" style="width: 100%" placeholder="检验时间" />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item label="备注" name="remark">
                <a-textarea v-model:value="form.remark" :rows="3" placeholder="检验备注（可选）" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </a-card>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { qualityInspectionApi } from '@/api/quality'

const router = useRouter()
const formRef = ref<any>(null)
const saving = ref(false)

const form = reactive({
  bizNo: '',
  productName: '',
  batchNo: '',
  inspectionType: 'INBOUND',
  quantity: 0,
  sampleQuantity: 0,
  inspectionResult: 'PENDING',
  inspectorName: '',
  inspectionTime: null as any,
  remark: ''
})

function onCancel() {
  router.back()
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form,
      inspectionTime: form.inspectionTime ? dayjs(form.inspectionTime).format('YYYY-MM-DD HH:mm:ss') : null
    }
    await qualityInspectionApi.create(payload as any)
    message.success('质检单已创建')
    router.back()
  } catch (e: any) {
    message.error(e?.message || '创建质检单失败')
  } finally {
    saving.value = false
  }
}
</script>
