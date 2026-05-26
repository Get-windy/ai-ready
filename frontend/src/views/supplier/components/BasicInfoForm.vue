<template>
  <div class="basic-info-form">
    <div class="form-section">
      <h3 class="section-title">基本信息</h3>
      <div class="form-grid">
        <div class="form-group">
          <label class="form-label required">供应商编码</label>
          <ar-input
            v-model="localProfile.code"
            placeholder="请输入供应商编码"
            :maxlength="20"
            clearable
          />
        </div>
        
        <div class="form-group">
          <label class="form-label required">供应商名称</label>
          <ar-input
            v-model="localProfile.name"
            placeholder="请输入供应商全称"
            :maxlength="100"
            clearable
          />
        </div>
        
        <div class="form-group">
          <label class="form-label required">供应商类型</label>
          <ar-select
            v-model="localProfile.type"
            :options="supplierTypeOptions"
            placeholder="请选择供应商类型"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label required">行业分类</label>
          <ar-input
            v-model="localProfile.industry"
            placeholder="请输入行业分类"
            :maxlength="50"
            clearable
          />
        </div>
        
        <div class="form-group">
          <label class="form-label required">统一社会信用代码</label>
          <ar-input
            v-model="localProfile.registrationNumber"
            placeholder="请输入18位统一社会信用代码"
            :maxlength="18"
            clearable
          />
        </div>
        
        <div class="form-group">
          <label class="form-label required">纳税人识别号</label>
          <ar-input
            v-model="localProfile.taxNumber"
            placeholder="请输入纳税人识别号"
            :maxlength="20"
            clearable
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">注册资金（万元）</label>
          <ar-input
            v-model.number="localProfile.registeredCapital"
            type="number"
            placeholder="请输入注册资金"
            :min="0"
            :step="0.01"
          >
            <template #append>万元</template>
          </ar-input>
        </div>
        
        <div class="form-group">
          <label class="form-label">成立日期</label>
          <ar-date-picker
            v-model="localProfile.establishedDate"
            type="date"
            placeholder="选择成立日期"
            value-format="YYYY-MM-DD"
          />
        </div>
      </div>
    </div>
    
    <div class="form-section">
      <h3 class="section-title">联系方式</h3>
      <div class="form-grid">
        <div class="form-group">
          <label class="form-label required">地址</label>
          <ar-input
            v-model="localProfile.address"
            placeholder="请输入详细地址"
            :maxlength="200"
            clearable
            type="textarea"
            :rows="2"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">邮政编码</label>
          <ar-input
            v-model="localProfile.postalCode"
            placeholder="请输入邮政编码"
            :maxlength="6"
            clearable
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">网址</label>
          <ar-input
            v-model="localProfile.website"
            placeholder="请输入公司网址"
            :maxlength="100"
            clearable
          />
        </div>
        
        <div class="form-group">
          <label class="form-label required">邮箱</label>
          <ar-input
            v-model="localProfile.email"
            placeholder="请输入联系邮箱"
            :maxlength="100"
            clearable
            type="email"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label required">电话</label>
          <ar-input
            v-model="localProfile.phone"
            placeholder="请输入联系电话"
            :maxlength="20"
            clearable
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">传真</label>
          <ar-input
            v-model="localProfile.fax"
            placeholder="请输入传真号码"
            :maxlength="20"
            clearable
          />
        </div>
      </div>
    </div>
    
    <div class="form-actions">
      <ar-button type="primary" @click="saveChanges">保存修改</ar-button>
      <ar-button @click="resetForm">重置表单</ar-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { ArInput, ArSelect, ArDatePicker, ArButton } from '@ar/components';

interface SupplierProfile {
  id: string;
  code: string;
  name: string;
  type: 'manufacturer' | 'distributor' | 'service';
  status: string;
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
}

interface Props {
  profile: SupplierProfile;
}

const props = defineProps<Props>();
const emit = defineEmits(['update']);

const localProfile = ref<SupplierProfile>({ ...props.profile });

const supplierTypeOptions = [
  { value: 'manufacturer', label: '生产商' },
  { value: 'distributor', label: '分销商' },
  { value: 'service', label: '服务商' },
  { value: 'retailer', label: '零售商' },
  { value: 'wholesaler', label: '批发商' },
  { value: 'other', label: '其他' }
];

watch(
  () => props.profile,
  (newProfile) => {
    localProfile.value = { ...newProfile };
  },
  { deep: true }
);

watch(
  localProfile,
  (newProfile) => {
    emit('update', newProfile);
  },
  { deep: true, immediate: true }
);

const saveChanges = () => {
  // 验证必填字段
  const requiredFields = ['code', 'name', 'type', 'industry', 'registrationNumber', 'taxNumber', 'address', 'email', 'phone'];
  const missingFields = requiredFields.filter(field => !localProfile.value[field as keyof SupplierProfile]?.trim());
  
  if (missingFields.length > 0) {
    console.error('必填字段未填写:', missingFields);
    return;
  }
  
  console.log('保存基本信息:', localProfile.value);
  // API调用逻辑
};

const resetForm = () => {
  localProfile.value = { ...props.profile };
};
</script>

<style lang="scss" scoped>
.basic-info-form {
  .form-section {
    margin-bottom: 32px;
    
    &:last-child {
      margin-bottom: 0;
    }
    
    .section-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 20px;
      padding-bottom: 12px;
      border-bottom: 1px solid #e4e7ed;
    }
  }
  
  .form-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 20px 24px;
  }
  
  .form-group {
    display: flex;
    flex-direction: column;
    
    .form-label {
      font-size: 14px;
      color: #606266;
      margin-bottom: 8px;
      line-height: 1.4;
      
      &.required::before {
        content: '*';
        color: #f56c6c;
        margin-right: 4px;
      }
    }
  }
  
  .form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 32px;
    padding-top: 20px;
    border-top: 1px solid #e4e7ed;
  }
}

@media (max-width: 768px) {
  .basic-info-form {
    .form-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>