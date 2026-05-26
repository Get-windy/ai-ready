<template>
  <div class="expense-list">
    <a-card title="费用单管理">
      <!-- 搜索和操作栏 -->
      <div class="toolbar">
        <a-space>
          <a-input v-model:value="searchText" placeholder="搜索费用单" style="width: 200px" />
          <a-select v-model:value="statusFilter" placeholder="状态筛选" style="width: 150px" allowClear>
            <a-select-option value="draft">草稿</a-select-option>
            <a-select-option value="pending">待审批</a-select-option>
            <a-select-option value="approved">已审批</a-select-option>
            <a-select-option value="rejected">已拒绝</a-select-option>
            <a-select-option value="paid">已支付</a-select-option>
          </a-select>
          <a-button type="primary" @click="handleCreate">新建费用单</a-button>
        </a-space>
      </div>

      <!-- 费用单列表 -->
      <a-table :columns="columns" :data-source="expenseList" :loading="loading" :pagination="pagination"
               @change="handleTableChange" rowKey="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusName(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'amount'">
            ¥{{ record.amount?.toFixed(2) }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button size="small" @click="handleView(record)">查看</a-button>
              <a-button size="small" v-if="record.status === 'draft'" @click="handleEdit(record)">编辑</a-button>
              <a-button size="small" v-if="record.status === 'draft'" danger @click="handleDelete(record)">删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 创建/编辑对话框 -->
    <a-modal v-model:open="modalVisible" :title="modalTitle" width="800px" @ok="handleSubmit">
      <a-form :model="formState" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="标题" required>
          <a-input v-model:value="formState.title" />
        </a-form-item>
        <a-form-item label="费用类型" required>
          <a-select v-model:value="formState.typeId">
            <a-select-option v-for="type in expenseTypes" :key="type.id" :value="type.id">
              {{ type.typeName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="总金额" required>
          <a-input-number v-model:value="formState.amount" :precision="2" :min="0" />
        </a-form-item>
        <a-form-item label="费用日期" required>
          <a-date-picker v-model:value="formState.expenseDate" />
        </a-form-item>
        <a-form-item label="费用说明">
          <a-textarea v-model:value="formState.description" :rows="3" />
        </a-form-item>
        
        <!-- 费用明细 -->
        <a-divider>费用明细</a-divider>
        <a-button type="dashed" @click="addDetail" style="width: 100%">添加明细</a-button>
        <div v-for="(detail, index) in formState.details" :key="index" class="detail-item">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-input v-model:value="detail.itemName" placeholder="项目名称" />
            </a-col>
            <a-col :span="6">
              <a-input-number v-model:value="detail.amount" :precision="2" :min="0" placeholder="金额" />
            </a-col>
            <a-col :span="6">
              <a-input v-model:value="detail.invoiceNo" placeholder="发票号码" />
            </a-col>
            <a-col :span="4">
              <a-button danger size="small" @click="removeDetail(index)">删除</a-button>
            </a-col>
          </a-row>
        </div>
      </a-form>
    </a-modal>

    <!-- 详情对话框 -->
    <a-modal v-model:open="detailVisible" title="费用单详情" width="800px" :footer="null">
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="费用单号">{{ detailData.sheetNo }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(detailData.status)">{{ getStatusName(detailData.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="标题">{{ detailData.title }}</a-descriptions-item>
        <a-descriptions-item label="费用类型">{{ detailData.typeName }}</a-descriptions-item>
        <a-descriptions-item label="总金额">¥{{ detailData.amount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="申请人">{{ detailData.applicantName }}</a-descriptions-item>
        <a-descriptions-item label="费用日期">{{ detailData.expenseDate }}</a-descriptions-item>
        <a-descriptions-item label="申请日期">{{ detailData.applyDate }}</a-descriptions-item>
        <a-descriptions-item label="费用说明" :span="2">{{ detailData.description }}</a-descriptions-item>
      </a-descriptions>
      
      <!-- 费用明细 -->
      <a-divider>费用明细</a-divider>
      <a-table :columns="detailColumns" :data-source="detailData.details" :pagination="false" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            ¥{{ record.amount?.toFixed(2) }}
          </template>
        </template>
      </a-table>

      <!-- 审批操作 -->
      <a-divider>审批操作</a-divider>
      <a-space v-if="detailData.status === 'pending'">
        <a-button type="primary" @click="handleApprove">批准</a-button>
        <a-button danger @click="handleReject">拒绝</a-button>
        <a-button @click="handleReturn">退回</a-button>
      </a-space>
      <a-space v-if="detailData.status === 'approved'">
        <a-button type="primary" @click="handlePay">标记已支付</a-button>
      </a-space>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import axios from 'axios';

const expenseList = ref([]);
const expenseTypes = ref([]);
const loading = ref(false);
const searchText = ref('');
const statusFilter = ref('');
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
});

const modalVisible = ref(false);
const modalTitle = ref('新建费用单');
const isEdit = ref(false);

const formState = reactive({
  id: null,
  title: '',
  typeId: null,
  amount: 0,
  expenseDate: null,
  description: '',
  details: []
});

const detailVisible = ref(false);
const detailData = ref({});

const columns = [
  { title: '费用单号', dataIndex: 'sheetNo', key: 'sheetNo' },
  { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '费用类型', dataIndex: 'typeName', key: 'typeName' },
  { title: '金额', dataIndex: 'amount', key: 'amount' },
  { title: '申请人', dataIndex: 'applicantName', key: 'applicantName' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '费用日期', dataIndex: 'expenseDate', key: 'expenseDate' },
  { title: '操作', key: 'action' }
];

const detailColumns = [
  { title: '项目名称', dataIndex: 'itemName' },
  { title: '金额', dataIndex: 'amount', key: 'amount' },
  { title: '发票号码', dataIndex: 'invoiceNo' },
  { title: '说明', dataIndex: 'description' }
];

onMounted(() => {
  loadExpenseList();
  loadExpenseTypes();
});

const loadExpenseList = async () => {
  loading.value = true;
  try {
    const res = await axios.get('/api/erp/expense/sheets', {
      params: { page: pagination.current, size: pagination.pageSize }
    });
    if (res.data.success) {
      expenseList.value = res.data.data.list;
      pagination.total = res.data.data.total;
    }
  } catch (error) {
    message.error('加载费用单列表失败');
  }
  loading.value = false;
};

const loadExpenseTypes = async () => {
  try {
    const res = await axios.get('/api/erp/expense/types');
    if (res.data.success) {
      expenseTypes.value = res.data.data;
    }
  } catch (error) {
    message.error('加载费用类型失败');
  }
};

const handleCreate = () => {
  modalTitle.value = '新建费用单';
  isEdit.value = false;
  resetForm();
  modalVisible.value = true;
};

const handleEdit = (record) => {
  modalTitle.value = '编辑费用单';
  isEdit.value = true;
  formState.id = record.id;
  formState.title = record.title;
  formState.typeId = record.typeId;
  formState.amount = record.amount;
  formState.expenseDate = record.expenseDate;
  formState.description = record.description;
  modalVisible.value = true;
};

const handleView = async (record) => {
  try {
    const res = await axios.get(`/api/erp/expense/sheets/${record.id}`);
    if (res.data.success) {
      detailData.value = res.data.data;
      detailVisible.value = true;
    }
  } catch (error) {
    message.error('加载费用单详情失败');
  }
};

const handleDelete = async (record) => {
  try {
    const res = await axios.delete(`/api/erp/expense/sheets/${record.id}`);
    if (res.data.success) {
      message.success('删除成功');
      loadExpenseList();
    }
  } catch (error) {
    message.error('删除失败');
  }
};

const handleSubmit = async () => {
  try {
    const url = isEdit.value 
      ? `/api/erp/expense/sheets/${formState.id}`
      : '/api/erp/expense/sheets';
    const method = isEdit.value ? 'put' : 'post';
    
    const res = await axios[method](url, formState);
    if (res.data.success) {
      message.success(isEdit.value ? '更新成功' : '创建成功');
      modalVisible.value = false;
      loadExpenseList();
    }
  } catch (error) {
    message.error(isEdit.value ? '更新失败' : '创建失败');
  }
};

const handleApprove = async () => {
  try {
    const res = await axios.post(`/api/erp/expense/sheets/${detailData.value.id}/approve`, null, {
      params: { approverId: 1, approverName: '审批人', comment: '同意' }
    });
    if (res.data.success) {
      message.success('批准成功');
      detailVisible.value = false;
      loadExpenseList();
    }
  } catch (error) {
    message.error('批准失败');
  }
};

const handleReject = async () => {
  try {
    const res = await axios.post(`/api/erp/expense/sheets/${detailData.value.id}/reject`, null, {
      params: { approverId: 1, approverName: '审批人', comment: '拒绝' }
    });
    if (res.data.success) {
      message.success('拒绝成功');
      detailVisible.value = false;
      loadExpenseList();
    }
  } catch (error) {
    message.error('拒绝失败');
  }
};

const handleReturn = async () => {
  try {
    const res = await axios.post(`/api/erp/expense/sheets/${detailData.value.id}/return`, null, {
      params: { approverId: 1, approverName: '审批人' }
    });
    if (res.data.success) {
      message.success('退回成功');
      detailVisible.value = false;
      loadExpenseList();
    }
  } catch (error) {
    message.error('退回失败');
  }
};

const handlePay = async () => {
  try {
    const res = await axios.post(`/api/erp/expense/sheets/${detailData.value.id}/pay`);
    if (res.data.success) {
      message.success('标记已支付成功');
      detailVisible.value = false;
      loadExpenseList();
    }
  } catch (error) {
    message.error('标记支付失败');
  }
};

const handleTableChange = (pag) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  loadExpenseList();
};

const addDetail = () => {
  formState.details.push({
    itemName: '',
    amount: 0,
    invoiceNo: '',
    description: ''
  });
};

const removeDetail = (index) => {
  formState.details.splice(index, 1);
};

const resetForm = () => {
  formState.id = null;
  formState.title = '';
  formState.typeId = null;
  formState.amount = 0;
  formState.expenseDate = null;
  formState.description = '';
  formState.details = [];
};

const getStatusColor = (status) => {
  const colors = {
    draft: 'default',
    pending: 'blue',
    approved: 'green',
    rejected: 'red',
    paid: 'purple',
    cancelled: 'gray'
  };
  return colors[status] || 'default';
};

const getStatusName = (status) => {
  const names = {
    draft: '草稿',
    pending: '待审批',
    approved: '已审批',
    rejected: '已拒绝',
    paid: '已支付',
    cancelled: '已取消'
  };
  return names[status] || status;
};
</script>

<style scoped>
.expense-list {
  padding: 24px;
}
.toolbar {
  margin-bottom: 16px;
}
.detail-item {
  margin-top: 8px;
  padding: 8px 0;
}
</style>