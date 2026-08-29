<template>
  <div class="contract-form-page">
    <!-- 顶部操作栏 -->
    <div class="form-header">
      <div class="header-left">
        <a-button
          size="small"
          @click="handleBack"
        >
          <template #icon>
            <ArrowLeftOutlined />
          </template>
          返回
        </a-button>
        <span class="order-no">NO. {{ formData.contractNo || '待生成' }}</span>
      </div>
      <div class="header-center">
        <h2 class="form-title">
          采购合同
        </h2>
      </div>
      <div class="header-right">
        <a-button
          v-if="formData.contractStatus === ContractStatus.DRAFT || !formData.contractStatus"
          size="small"
          :loading="saving"
          @click="handleSaveDraft"
        >
          <template #icon>
            <SaveOutlined />
          </template>
          保存
        </a-button>
        <a-button
          v-if="formData.contractStatus === ContractStatus.DRAFT || !formData.contractStatus"
          size="small"
          type="primary"
          :loading="saving"
          @click="handleSubmit"
        >
          <template #icon>
            <SendOutlined />
          </template>
          提交
        </a-button>
        <a-button
          v-if="formData.contractStatus === ContractStatus.PENDING_APPROVAL"
          size="small"
          type="primary"
          :loading="saving"
          @click="handleApprove"
        >
          <template #icon>
            <CheckOutlined />
          </template>
          审批
        </a-button>
        <a-button
          v-if="formData.contractStatus === ContractStatus.APPROVED"
          size="small"
          type="primary"
          :loading="saving"
          @click="handleActivate"
        >
          <template #icon>
            <ThunderboltOutlined />
          </template>
          激活
        </a-button>
        <a-button
          v-if="formData.contractStatus === ContractStatus.ACTIVE"
          size="small"
          danger
          :loading="saving"
          @click="handleTerminate"
        >
          <template #icon>
            <StopOutlined />
          </template>
          终止
        </a-button>
        <a-tag
          v-if="formData.contractStatus"
          :color="getStatusColor(formData.contractStatus)"
          style="margin-left: 8px;"
        >
          {{ getStatusText(formData.contractStatus) }}
        </a-tag>
      </div>
    </div>

    <!-- 表单内容区 -->
    <div class="form-body">
      <!-- 基础信息 -->
      <div class="form-section">
        <h3 class="section-title">
          基础信息
        </h3>
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 18 }"
        >
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item
                label="合同名称"
                required
              >
                <a-input
                  v-model:value="formData.contractTitle"
                  placeholder="请输入合同名称"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item
                label="供应商"
                required
              >
                <a-select
                  v-model:value="formData.supplierId"
                  placeholder="请搜索选择供应商"
                  show-search
                  :filter-option="filterOption"
                  :loading="loadingOptions"
                  size="small"
                  @change="handleSupplierChange"
                >
                  <a-select-option
                    v-for="s in supplierOptions"
                    :key="s.id"
                    :value="s.id"
                  >
                    {{ s.name }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item
                label="合同类型"
              >
                <a-select
                  v-model:value="formData.contractType"
                  placeholder="请选择合同类型"
                  size="small"
                >
                  <a-select-option value="FRAMEWORK">框架合同</a-select-option>
                  <a-select-option value="ONE_TIME">一次性合同</a-select-option>
                  <a-select-option value="SERVICE">服务合同</a-select-option>
                  <a-select-option value="OTHER">其他</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item
                label="合同金额"
                required
              >
                <a-input-number
                  v-model:value="formData.totalAmount"
                  placeholder="请输入合同金额"
                  :min="0"
                  :precision="2"
                  :formatter="(val: number) => `¥ ${val}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
                  :parser="(val: string) => val.replace(/¥\s?|(,*)/g, '')"
                  size="small"
                  style="width: 100%;"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item
                label="开始日期"
              >
                <a-date-picker
                  v-model:value="formData.startDate"
                  placeholder="请选择开始日期"
                  value-format="YYYY-MM-DD"
                  size="small"
                  style="width: 100%;"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item
                label="结束日期"
              >
                <a-date-picker
                  v-model:value="formData.endDate"
                  placeholder="请选择结束日期"
                  value-format="YYYY-MM-DD"
                  size="small"
                  style="width: 100%;"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </div>

      <!-- 商务条款 -->
      <div class="form-section">
        <h3 class="section-title">
          商务条款
        </h3>
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 18 }"
        >
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item
                label="付款条件"
              >
                <a-input
                  v-model:value="formData.paymentTerms"
                  placeholder="如：月结30天、预付50%等"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item
                label="交货条件"
              >
                <a-input
                  v-model:value="formData.deliveryTerms"
                  placeholder="如：货到付款、分批交货等"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item
                label="质保期"
              >
                <a-input
                  v-model:value="formData.warrantyPeriod"
                  placeholder="如：12个月、24个月"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item
                label="质量标准"
              >
                <a-input
                  v-model:value="formData.qualityStandard"
                  placeholder="请输入质量标准"
                  size="small"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </div>

      <!-- 合同内容/备注 -->
      <div class="form-section">
        <h3 class="section-title">
          合同内容
        </h3>
        <a-form
          :label-col="{ span: 3 }"
          :wrapper-col="{ span: 21 }"
        >
          <a-form-item label="合同备注">
            <a-textarea
              v-model:value="formData.remark"
              placeholder="请输入合同内容、条款说明或其他备注"
              :rows="4"
              :maxlength="2000"
              show-count
              size="small"
            />
          </a-form-item>
        </a-form>
      </div>

      <!-- 附件上传 -->
      <div class="form-section">
        <h3 class="section-title">
          合同附件
        </h3>
        <div class="upload-area">
          <a-upload
            :file-list="fileList"
            :before-upload="beforeUpload"
            @remove="handleRemoveFile"
          >
            <a-button size="small">
              <template #icon>
                <UploadOutlined />
              </template>
              上传附件
            </a-button>
          </a-upload>
          <div
            v-if="formData.contractFileUrl"
            class="file-url"
          >
            当前合同文件:
            <a
              :href="formData.contractFileUrl"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ formData.contractFileUrl }}
            </a>
          </div>
        </div>
      </div>

      <!-- 商品/服务明细 -->
      <div class="form-section">
        <h3 class="section-title">
          合同明细
          <a-button
            size="small"
            type="link"
            @click="handleAddItem"
          >
            <template #icon>
              <PlusCircleOutlined />
            </template>
            添加商品
          </a-button>
        </h3>
        <a-table
          :columns="itemColumns"
          :data-source="formData.items"
          :pagination="false"
          size="small"
          bordered
          row-key="lineNo"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'lineNo'">
              {{ index + 1 }}
            </template>
            <template v-if="column.key === 'productName'">
              <a-select
                v-model:value="record.productId"
                placeholder="选择商品"
                show-search
                :filter-option="filterOption"
                :loading="loadingProducts"
                size="small"
                style="width: 100%;"
                @change="(val: number) => handleProductChange(val, index)"
              >
                <a-select-option
                  v-for="p in productOptions"
                  :key="p.id"
                  :value="p.id"
                >
                  {{ p.name }}
                </a-select-option>
              </a-select>
            </template>
            <template v-if="column.key === 'quantity'">
              <a-input-number
                v-model:value="record.quantity"
                :min="0"
                :precision="2"
                size="small"
                style="width: 100%;"
                @change="() => calculateAmount(index)"
              />
            </template>
            <template v-if="column.key === 'unitPrice'">
              <a-input-number
                v-model:value="record.unitPrice"
                :min="0"
                :precision="2"
                size="small"
                style="width: 100%;"
                @change="() => calculateAmount(index)"
              />
            </template>
            <template v-if="column.key === 'amount'">
              ¥{{ record.amount?.toFixed(2) || '0.00' }}
            </template>
            <template v-if="column.key === 'action'">
              <a-button
                type="link"
                size="small"
                danger
                @click="handleRemoveItem(index)"
              >
                删除
              </a-button>
            </template>
          </template>
          <template #summary>
            <a-table-summary :fixed="true">
              <a-table-summary-row>
                <a-table-summary-cell
                  :index="0"
                  :col-span="5"
                  align="right"
                >
                  <strong>合计:</strong>
                </a-table-summary-cell>
                <a-table-summary-cell
                  :index="5"
                  align="right"
                >
                  <strong>¥{{ itemsTotalAmount.toFixed(2) }}</strong>
                </a-table-summary-cell>
                <a-table-summary-cell :index="6" />
              </a-table-summary-row>
            </a-table-summary>
          </template>
        </a-table>
      </div>

      <!-- 审批信息（只读） -->
      <div
        v-if="formData.submitTime || formData.approvalTime"
        class="form-section"
      >
        <h3 class="section-title">
          审批信息
        </h3>
        <a-descriptions
          bordered
          :column="3"
          size="small"
        >
          <a-descriptions-item
            v-if="formData.submitTime"
            label="提交时间"
          >
            {{ formData.submitTime }}
          </a-descriptions-item>
          <a-descriptions-item
            v-if="formData.approvalTime"
            label="审批时间"
          >
            {{ formData.approvalTime }}
          </a-descriptions-item>
          <a-descriptions-item
            v-if="formData.approvalComment"
            label="审批意见"
            :span="3"
          >
            {{ formData.approvalComment }}
          </a-descriptions-item>
          <a-descriptions-item
            v-if="formData.terminationTime"
            label="终止时间"
          >
            {{ formData.terminationTime }}
          </a-descriptions-item>
          <a-descriptions-item
            v-if="formData.terminationReason"
            label="终止原因"
            :span="3"
          >
            {{ formData.terminationReason }}
          </a-descriptions-item>
          <a-descriptions-item
            v-if="formData.archiveNo"
            label="归档编号"
          >
            {{ formData.archiveNo }}
          </a-descriptions-item>
          <a-descriptions-item
            v-if="formData.archiveTime"
            label="归档时间"
          >
            {{ formData.archiveTime }}
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowLeftOutlined, SaveOutlined, SendOutlined, CheckOutlined,
  ThunderboltOutlined, StopOutlined, PlusCircleOutlined, UploadOutlined,
} from '@ant-design/icons-vue'
import type { UploadFile } from 'ant-design-vue'
import {
  purchaseContractApi, ContractStatus,
  type PurchaseContract, type PurchaseContractItem,
} from '@/api/purchase-contract'
import { supplierApi } from '@/api/supplier'
import { productApi } from '@/api/erp/product'

defineOptions({ name: 'PurchaseContractForm' })
const router = useRouter()
const route = useRoute()

// ── 表单数据 ──────────────────────────────────────────
const formData = reactive<PurchaseContract & { items: PurchaseContractItem[] }>({
  id: 0,
  contractNo: '',
  supplierId: 0,
  supplierName: '',
  contractTitle: '',
  contractType: 'FRAMEWORK',
  contractStatus: ContractStatus.DRAFT,
  totalAmount: 0,
  executedAmount: 0,
  executedPercent: 0,
  startDate: '',
  endDate: '',
  paymentTerms: '',
  deliveryTerms: '',
  qualityStandard: '',
  warrantyPeriod: '',
  remark: '',
  contractFileUrl: '',
  items: [],
})

const saving = ref(false)
const loadingOptions = ref(false)
const loadingProducts = ref(false)
const supplierOptions = ref<{ id: number; name: string }[]>([])
const productOptions = ref<{ id: number; name: string }[]>([])
const fileList = ref<UploadFile[]>([])

// ── 明细列配置 ──────────────────────────────────────────
const itemColumns = [
  { title: '序号', key: 'lineNo', width: 60, align: 'center' as const },
  { title: '商品名称', key: 'productName', width: 240 },
  { title: '数量', key: 'quantity', width: 120 },
  { title: '单价', key: 'unitPrice', width: 120 },
  { title: '金额', key: 'amount', width: 120, align: 'right' as const },
  { title: '合计', key: 'total', width: 120, align: 'right' as const },
  { title: '操作', key: 'action', width: 80, align: 'center' as const },
]

const itemsTotalAmount = computed(() => {
  return formData.items.reduce((sum, item) => sum + (item.amount || 0), 0)
})

// ── 商品筛选 ──────────────────────────────────────────
const filterOption = (input: string, option: any) => {
  return option.children?.()[0]?.children?.toLowerCase?.().includes(input.toLowerCase())
}

// ── 数据加载 ──────────────────────────────────────────
const loadOptions = async () => {
  loadingOptions.value = true
  try {
    const res = await supplierApi.list()
    supplierOptions.value = (res as any).data || res || []
  } catch (error) {
    console.warn('[采购合同] 加载供应商失败', error)
  } finally {
    loadingOptions.value = false
  }
}

const loadProducts = async () => {
  loadingProducts.value = true
  try {
    const res = await productApi.list()
    productOptions.value = (res as any).data || res || []
  } catch (error) {
    console.warn('[采购合同] 加载商品失败', error)
  } finally {
    loadingProducts.value = false
  }
}

const loadContract = async (id: number) => {
  try {
    const res = await purchaseContractApi.get(id)
    const data = (res as any).data || res
    Object.assign(formData, data)
    if (!formData.items) formData.items = []
  } catch (error) {
    console.warn('[采购合同] 加载合同失败', error)
    message.error('加载合同失败')
  }
}

// ── 事件处理 ──────────────────────────────────────────
const handleBack = () => {
  router.back()
}

const handleSupplierChange = (val: number) => {
  const supplier = supplierOptions.value.find(s => s.id === val)
  if (supplier) {
    formData.supplierName = supplier.name
  }
}

const handleAddItem = () => {
  formData.items.push({
    lineNo: formData.items.length + 1,
    productId: 0,
    productName: '',
    quantity: 0,
    unitPrice: 0,
    amount: 0,
  })
}

const handleRemoveItem = (index: number) => {
  formData.items.splice(index, 1)
  // 重新编号
  formData.items.forEach((item, idx) => {
    item.lineNo = idx + 1
  })
}

const handleProductChange = (val: number, index: number) => {
  const product = productOptions.value.find(p => p.id === val)
  if (product && formData.items[index]) {
    formData.items[index].productName = product.name
  }
}

const calculateAmount = (index: number) => {
  const item = formData.items[index]
  if (item) {
    item.amount = (item.quantity || 0) * (item.unitPrice || 0)
  }
}

const beforeUpload = (file: File) => {
  fileList.value.push({
    uid: file.uid,
    name: file.name,
    status: 'done',
    originFileObj: file,
  })
  return false
}

const handleRemoveFile = (file: UploadFile) => {
  const index = fileList.value.findIndex(f => f.uid === file.uid)
  if (index > -1) {
    fileList.value.splice(index, 1)
  }
}

const handleSaveDraft = async () => {
  saving.value = true
  try {
    if (formData.id) {
      await purchaseContractApi.update(formData.id, formData)
    } else {
      const res = await purchaseContractApi.create(formData)
      formData.id = (res as any).data || (res as any)
    }
    message.success('已保存')
  } catch {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleSubmit = async () => {
  if (!formData.contractTitle) {
    message.warning('请输入合同名称')
    return
  }
  if (!formData.supplierId) {
    message.warning('请选择供应商')
    return
  }
  saving.value = true
  try {
    // 先保存
    if (formData.id) {
      await purchaseContractApi.update(formData.id, formData)
    } else {
      const res = await purchaseContractApi.create(formData)
      formData.id = (res as any).data || (res as any)
    }
    // 再提交
    await purchaseContractApi.submit(formData.id, '提交审批')
    message.success('已提交审批')
    formData.contractStatus = ContractStatus.PENDING_APPROVAL
  } catch {
    message.error('提交失败')
  } finally {
    saving.value = false
  }
}

const handleApprove = async () => {
  saving.value = true
  try {
    await purchaseContractApi.approve(formData.id, 0, '审批通过', true)
    message.success('审批通过')
    formData.contractStatus = ContractStatus.APPROVED
  } catch {
    message.error('审批失败')
  } finally {
    saving.value = false
  }
}

const handleActivate = async () => {
  saving.value = true
  try {
    await purchaseContractApi.activate(formData.id)
    message.success('合同已激活')
    formData.contractStatus = ContractStatus.ACTIVE
  } catch {
    message.error('激活失败')
  } finally {
    saving.value = false
  }
}

const handleTerminate = async () => {
  saving.value = true
  try {
    await purchaseContractApi.terminate(formData.id, '手动终止')
    message.success('合同已终止')
    formData.contractStatus = ContractStatus.TERMINATED
  } catch {
    message.error('终止失败')
  } finally {
    saving.value = false
  }
}

const getStatusText = (status: ContractStatus): string => {
  const map: Record<ContractStatus, string> = {
    [ContractStatus.DRAFT]: '草稿',
    [ContractStatus.PENDING_APPROVAL]: '待审批',
    [ContractStatus.APPROVED]: '已审批',
    [ContractStatus.ACTIVE]: '生效中',
    [ContractStatus.COMPLETED]: '已完成',
    [ContractStatus.REJECTED]: '已驳回',
    [ContractStatus.TERMINATED]: '已终止',
    [ContractStatus.ARCHIVED]: '已归档',
  }
  return map[status] || '未知'
}

const getStatusColor = (status: ContractStatus): string => {
  const map: Record<ContractStatus, string> = {
    [ContractStatus.DRAFT]: 'default',
    [ContractStatus.PENDING_APPROVAL]: 'orange',
    [ContractStatus.APPROVED]: 'blue',
    [ContractStatus.ACTIVE]: 'green',
    [ContractStatus.COMPLETED]: 'cyan',
    [ContractStatus.REJECTED]: 'red',
    [ContractStatus.TERMINATED]: 'volcano',
    [ContractStatus.ARCHIVED]: 'purple',
  }
  return map[status] || 'default'
}

onMounted(async () => {
  await Promise.all([loadOptions(), loadProducts()])
  const id = route.query.id
  if (id) {
    await loadContract(Number(id))
  }
})
</script>

<style scoped>
.contract-form-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f5f5;
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.order-no {
  font-size: 14px;
  font-weight: 500;
  color: #595959;
  font-family: monospace;
}

.header-center {
  flex: 1;
  text-align: center;
}

.form-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
}

.form-section {
  background: #fff;
  border-radius: 6px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 16px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.upload-area {
  padding: 8px 0;
}

.file-url {
  margin-top: 12px;
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 4px;
  font-size: 13px;
  color: #595959;
}

.file-url a {
  color: #1890ff;
  margin-left: 8px;
}
</style>
