<template>
  <div class="supplier-profile">
    <div class="profile-header">
      <h1 class="profile-title">供应商档案管理</h1>
      <div class="profile-actions">
        <ar-button type="primary" @click="saveProfile">保存档案</ar-button>
        <ar-button @click="printProfile">打印档案</ar-button>
        <ar-button type="success" @click="exportProfile">导出档案</ar-button>
      </div>
    </div>

    <div class="profile-tabs">
      <ar-tabs v-model="activeTab">
        <ar-tab-pane label="基本信息" name="basic">
          <basic-info-form :profile="profile" @update="updateProfile" />
        </ar-tab-pane>
        <ar-tab-pane label="联系人信息" name="contacts">
          <contacts-management :contacts="profile.contacts" @update="updateContacts" />
        </ar-tab-pane>
        <ar-tab-pane label="资质证书" name="certificates">
          <certificates-management :certificates="profile.certificates" @update="updateCertificates" />
        </ar-tab-pane>
        <ar-tab-pane label="经营信息" name="business">
          <business-info-form :business="profile.business" @update="updateBusiness" />
        </ar-tab-pane>
        <ar-tab-pane label="绩效评价" name="performance">
          <performance-evaluation :performance="profile.performance" @update="updatePerformance" />
        </ar-tab-pane>
      </ar-tabs>
    </div>

    <div class="profile-summary">
      <div class="summary-card">
        <div class="summary-title">档案完整度</div>
        <div class="summary-value">
          <span class="completion-rate">{{ completionRate }}%</span>
          <div class="completion-bar">
            <div class="completion-fill" :style="{ width: completionRate + '%' }"></div>
          </div>
        </div>
      </div>
      <div class="summary-card">
        <div class="summary-title">最后更新</div>
        <div class="summary-value">{{ formatDate(profile.updatedAt) }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-title">档案状态</div>
        <div class="summary-value">
          <span :class="['status-badge', profile.status]">{{ getStatusText(profile.status) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ArButton, ArTabs, ArTabPane } from '@ar/components';
import BasicInfoForm from './components/BasicInfoForm.vue';
import ContactsManagement from './components/ContactsManagement.vue';
import CertificatesManagement from './components/CertificatesManagement.vue';
import BusinessInfoForm from './components/BusinessInfoForm.vue';
import PerformanceEvaluation from './components/PerformanceEvaluation.vue';

interface SupplierProfile {
  id: string;
  code: string;
  name: string;
  type: 'manufacturer' | 'distributor' | 'service';
  status: 'active' | 'inactive' | 'suspended' | 'pending';
  industry: string;
  registrationNumber: string;
  taxNumber: string;
  registeredCapital: number;
  establishedDate: string;
  address: string;
  postalCode: string;
  website: string;
  email: string;
  phone: string;
  fax: string;
  
  contacts: Array<{
    id: string;
    name: string;
    position: string;
    phone: string;
    email: string;
    isPrimary: boolean;
  }>;
  
  certificates: Array<{
    id: string;
    name: string;
    type: string;
    number: string;
    issueDate: string;
    expiryDate: string;
    status: 'valid' | 'expired' | 'renewing';
  }>;
  
  business: {
    annualRevenue: number;
    employeeCount: number;
    mainProducts: string[];
    exportCountries: string[];
    qualitySystem: string;
    bankAccounts: Array<{
      bankName: string;
      accountName: string;
      accountNumber: string;
    }>;
  };
  
  performance: {
    score: number;
    rating: 'A' | 'B' | 'C' | 'D';
    deliveryRate: number;
    qualityRate: number;
    serviceRate: number;
    lastEvaluationDate: string;
  };
  
  updatedAt: string;
  createdAt: string;
}

const activeTab = ref('basic');
const profile = ref<SupplierProfile>({
  id: '',
  code: '',
  name: '',
  type: 'manufacturer',
  status: 'pending',
  industry: '',
  registrationNumber: '',
  taxNumber: '',
  registeredCapital: 0,
  establishedDate: '',
  address: '',
  postalCode: '',
  website: '',
  email: '',
  phone: '',
  fax: '',
  contacts: [],
  certificates: [],
  business: {
    annualRevenue: 0,
    employeeCount: 0,
    mainProducts: [],
    exportCountries: [],
    qualitySystem: '',
    bankAccounts: []
  },
  performance: {
    score: 0,
    rating: 'C',
    deliveryRate: 0,
    qualityRate: 0,
    serviceRate: 0,
    lastEvaluationDate: ''
  },
  updatedAt: new Date().toISOString(),
  createdAt: new Date().toISOString()
});

const completionRate = computed(() => {
  const fields = [
    profile.value.name,
    profile.value.industry,
    profile.value.registrationNumber,
    profile.value.address,
    profile.value.email,
    profile.value.phone
  ];
  const filledFields = fields.filter(field => field && field.trim() !== '').length;
  return Math.round((filledFields / fields.length) * 100);
});

const formatDate = (dateString: string) => {
  if (!dateString) return '暂无';
  const date = new Date(dateString);
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  });
};

const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    active: '正常',
    inactive: '停用',
    suspended: '暂停',
    pending: '待审核'
  };
  return statusMap[status] || status;
};

const updateProfile = (data: Partial<SupplierProfile>) => {
  profile.value = { ...profile.value, ...data };
};

const updateContacts = (contacts: Array<any>) => {
  profile.value.contacts = contacts;
};

const updateCertificates = (certificates: Array<any>) => {
  profile.value.certificates = certificates;
};

const updateBusiness = (business: any) => {
  profile.value.business = business;
};

const updatePerformance = (performance: any) => {
  profile.value.performance = performance;
};

const saveProfile = async () => {
  try {
    console.log('保存供应商档案:', profile.value);
    // API调用逻辑
  } catch (error) {
    console.error('保存失败:', error);
  }
};

const printProfile = () => {
  window.print();
};

const exportProfile = () => {
  // 导出逻辑
  const dataStr = JSON.stringify(profile.value, null, 2);
  const dataUri = 'data:application/json;charset=utf-8,'+ encodeURIComponent(dataStr);
  const exportFileDefaultName = `${profile.value.code}_${profile.value.name}_档案.json`;
  
  const linkElement = document.createElement('a');
  linkElement.setAttribute('href', dataUri);
  linkElement.setAttribute('download', exportFileDefaultName);
  linkElement.click();
};

onMounted(() => {
  // 加载供应商数据
  // fetchSupplierProfile();
});
</script>

<style lang="scss" scoped>
.supplier-profile {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: 100vh;

  .profile-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    padding-bottom: 16px;
    border-bottom: 1px solid #e4e7ed;

    .profile-title {
      font-size: 24px;
      font-weight: 600;
      color: #303133;
      margin: 0;
    }

    .profile-actions {
      display: flex;
      gap: 12px;
    }
  }

  .profile-tabs {
    background: white;
    border-radius: 8px;
    padding: 24px;
    margin-bottom: 24px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  }

  .profile-summary {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 16px;
    margin-top: 24px;

    .summary-card {
      background: white;
      border-radius: 8px;
      padding: 20px;
      box-shadow: 0 2px 8px 0 rgba(0, 0, 0, 0.08);

      .summary-title {
        font-size: 14px;
        color: #909399;
        margin-bottom: 8px;
      }

      .summary-value {
        font-size: 24px;
        font-weight: 600;
        color: #303133;

        .completion-rate {
          color: #67c23a;
        }

        .completion-bar {
          height: 6px;
          background-color: #ebeef5;
          border-radius: 3px;
          margin-top: 8px;
          overflow: hidden;

          .completion-fill {
            height: 100%;
            background-color: #67c23a;
            transition: width 0.3s ease;
          }
        }

        .status-badge {
          display: inline-block;
          padding: 4px 12px;
          border-radius: 4px;
          font-size: 12px;
          font-weight: 500;

          &.active {
            background-color: #f0f9ff;
            color: #409eff;
          }

          &.inactive {
            background-color: #fef0f0;
            color: #f56c6c;
          }

          &.suspended {
            background-color: #fdf6ec;
            color: #e6a23c;
          }

          &.pending {
            background-color: #f4f4f5;
            color: #909399;
          }
        }
      }
    }
  }
}

@media print {
  .profile-actions,
  .profile-summary {
    display: none !important;
  }
}
</style>