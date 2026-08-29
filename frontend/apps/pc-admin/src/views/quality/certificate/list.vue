<template>
  <ErrorBoundary>
    <PageContainer title="质量证书">
      <template #header-extra>
        <a-button type="primary" size="small" @click="showCreateModal">
          <template #icon><PlusOutlined /></template>新增证书
        </a-button>
      </template>

      <ARReportPage
        ref="reportRef"
        title="质量证书"
        :query-fields="queryFields"
        :columns="columns"
        :fetcher="fetcher"
        export-file-name="质量证书"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'certificateType'">
            <a-tag :color="certTypeColor(record.certificateType)">{{ certTypeText(record.certificateType) }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'result'">
            <a-tag :color="resultColor(record.result)">{{ resultText(record.result) }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'certificateUrl'">
            <a v-if="record.certificateUrl" :href="record.certificateUrl" target="_blank">查看附件</a>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space :size="4">
              <a-button type="link" size="small" @click="showEditModal(record)">编辑</a-button>
              <a-popconfirm title="确定删除该证书？" @confirm="handleDelete(record)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </ARReportPage>

      <a-modal v-model:open="modalVisible" :title="editingId ? '编辑证书' : '新增证书'" width="700px" @ok="handleSave" :confirm-loading="saving">
        <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="证书编号">
                <a-input v-model:value="form.certificateNo" placeholder="留空自动生成" :disabled="!!editingId" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="证书类型" required>
                <a-select v-model:value="form.certificateType" placeholder="请选择证书类型">
                  <a-select-option value="COA">COA 分析证书</a-select-option>
                  <a-select-option value="COC">COC 合格证书</a-select-option>
                  <a-select-option value="ISO">ISO 认证</a-select-option>
                  <a-select-option value="OTHER">其他</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="产品名称" required>
                <a-input v-model:value="form.productName" placeholder="请输入产品名称" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="产品编码">
                <a-input v-model:value="form.productCode" placeholder="请输入产品编码" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="批次号">
                <a-input v-model:value="form.batchNo" placeholder="请输入批次号" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="供应商名称">
                <a-input v-model:value="form.supplierName" placeholder="请输入供应商名称" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="检验日期">
                <a-date-picker v-model:value="form.inspectionDate" style="width: 100%" value-format="YYYY-MM-DD" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="发证日期">
                <a-date-picker v-model:value="form.issueDate" style="width: 100%" value-format="YYYY-MM-DD" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="有效期至">
                <a-date-picker v-model:value="form.expiryDate" style="width: 100%" value-format="YYYY-MM-DD" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="检验结论" required>
                <a-select v-model:value="form.result" placeholder="请选择检验结论">
                  <a-select-option value="QUALIFIED">合格</a-select-option>
                  <a-select-option value="UNQUALIFIED">不合格</a-select-option>
                  <a-select-option value="CONDITIONAL">有条件放行</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="检验员">
                <a-input v-model:value="form.inspectorName" placeholder="请输入检验员" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="状态">
                <a-select v-model:value="form.status" placeholder="请选择状态">
                  <a-select-option :value="0">草稿</a-select-option>
                  <a-select-option :value="1">已生效</a-select-option>
                  <a-select-option :value="2">已过期</a-select-option>
                  <a-select-option :value="3">已撤销</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="证书附件" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
            <a-input v-model:value="form.certificateUrl" placeholder="请输入证书附件URL" />
          </a-form-item>
          <a-form-item label="备注" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
            <a-textarea v-model:value="form.remark" placeholder="请输入备注" :rows="3" />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { qualityCertificateApi } from '@/api/quality'

const reportRef = ref<any>(null)
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)

const defaultForm = () => ({
  certificateNo: '',
  certificateType: 'COA' as string,
  productName: '',
  productCode: '',
  batchNo: '',
  supplierName: '',
  inspectionDate: '',
  issueDate: '',
  expiryDate: '',
  result: '' as string,
  inspectorId: null as number | null,
  inspectorName: '',
  certificateUrl: '',
  remark: '',
  status: 0,
})
const form = reactive(defaultForm())

function certTypeText(t: string) {
  const map: Record<string, string> = { COA: 'COA 分析证书', COC: 'COC 合格证书', ISO: 'ISO 认证', OTHER: '其他' }
  return map[t] || t || '-'
}
function certTypeColor(t: string) {
  const map: Record<string, string> = { COA: 'blue', COC: 'green', ISO: 'purple', OTHER: 'default' }
  return map[t] || 'default'
}
function resultText(r: string) {
  const map: Record<string, string> = { QUALIFIED: '合格', UNQUALIFIED: '不合格', CONDITIONAL: '有条件放行' }
  return map[r] || r || '-'
}
function resultColor(r: string) {
  const map: Record<string, string> = { QUALIFIED: 'success', UNQUALIFIED: 'error', CONDITIONAL: 'warning' }
  return map[r] || 'default'
}
function statusText(s: number | string) {
  const map: Record<number, string> = { 0: '草稿', 1: '已生效', 2: '已过期', 3: '已撤销' }
  return map[Number(s)] ?? '-'
}
function statusColor(s: number | string) {
  const map: Record<number, string> = { 0: 'default', 1: 'success', 2: 'error', 3: 'warning' }
  return map[Number(s)] ?? 'default'
}

const queryFields: ReportQueryField[] = [
  { key: 'productName', type: 'input', label: '产品名称', placeholder: '产品名称', width: 140 },
  { key: 'batchNo', type: 'input', label: '批次号', placeholder: '批次号', width: 120 },
  { key: 'result', type: 'select', label: '检验结论', placeholder: '全部', options: [
    { label: '全部', value: '' },
    { label: '合格', value: 'QUALIFIED' },
    { label: '不合格', value: 'UNQUALIFIED' },
    { label: '有条件放行', value: 'CONDITIONAL' },
  ]},
  { key: 'startDate', type: 'date', label: '检验日期起', placeholder: '起始日期', width: 140 },
  { key: 'endDate', type: 'date', label: '检验日期止', placeholder: '结束日期', width: 140 },
]

const columns: any[] = [
  { title: '证书编号', dataIndex: 'certificateNo', key: 'certificateNo', width: 180 },
  { title: '证书类型', dataIndex: 'certificateType', key: 'certificateType', width: 110 },
  { title: '产品名称', dataIndex: 'productName', key: 'productName', width: 160, ellipsis: true },
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '批次号', dataIndex: 'batchNo', key: 'batchNo', width: 120 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 150, ellipsis: true },
  { title: '检验日期', dataIndex: 'inspectionDate', key: 'inspectionDate', width: 110 },
  { title: '有效期至', dataIndex: 'expiryDate', key: 'expiryDate', width: 110 },
  { title: '检验结论', dataIndex: 'result', key: 'result', width: 100 },
  { title: '检验员', dataIndex: 'inspectorName', key: 'inspectorName', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '附件', dataIndex: 'certificateUrl', key: 'certificateUrl', width: 80 },
  { title: '操作', key: 'action', width: 110, fixed: 'right' },
]

function fetcher(params: Record<string, any>) {
  return qualityCertificateApi.page({ ...params, pageNum: params.page, pageSize: params.size })
}

function resetForm() {
  Object.assign(form, defaultForm())
}

function showCreateModal() {
  editingId.value = null
  resetForm()
  modalVisible.value = true
}

function showEditModal(record: any) {
  editingId.value = record.id
  form.certificateNo = record.certificateNo || ''
  form.certificateType = record.certificateType || 'COA'
  form.productName = record.productName || ''
  form.productCode = record.productCode || ''
  form.batchNo = record.batchNo || ''
  form.supplierName = record.supplierName || ''
  form.inspectionDate = record.inspectionDate || ''
  form.issueDate = record.issueDate || ''
  form.expiryDate = record.expiryDate || ''
  form.result = record.result || ''
  form.inspectorId = record.inspectorId || null
  form.inspectorName = record.inspectorName || ''
  form.certificateUrl = record.certificateUrl || ''
  form.remark = record.remark || ''
  form.status = record.status ?? 0
  modalVisible.value = true
}

async function handleSave() {
  if (!form.productName) { message.warning('请填写产品名称'); return }
  if (!form.certificateType) { message.warning('请选择证书类型'); return }
  if (!form.result) { message.warning('请选择检验结论'); return }

  saving.value = true
  try {
    const payload = { ...form }
    if (editingId.value) {
      await qualityCertificateApi.update(editingId.value, payload as any)
      message.success('更新成功')
    } else {
      await qualityCertificateApi.create(payload as any)
      message.success('创建成功')
    }
    modalVisible.value = false
    reportRef.value?.reload()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: any) {
  try {
    await qualityCertificateApi.delete(record.id)
    message.success('删除成功')
    reportRef.value?.reload()
  } catch (e: any) {
    message.error(e?.message || '删除失败')
  }
}
</script>
