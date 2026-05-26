<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

interface SupplierForm {
  supplierCode: string
  supplierName: string
  shortName: string
  supplierType: number
  supplierLevel: string
  cooperationStatus: number
  contactPerson: string
  contactPhone: string
  email: string
  province: string
  city: string
  address: string
  bankName: string
  bankAccount: string
  taxNumber: string
  remark: string
}

const form = reactive<SupplierForm>({
  supplierCode: '',
  supplierName: '',
  shortName: '',
  supplierType: 1,
  supplierLevel: 'C',
  cooperationStatus: 4,
  contactPerson: '',
  contactPhone: '',
  email: '',
  province: '',
  city: '',
  address: '',
  bankName: '',
  bankAccount: '',
  taxNumber: '',
  remark: ''
})

const loading = ref(false)
const errors = ref<Record<string, string>>({})

const levelOptions = ['A', 'B', 'C', 'D', 'E']
const typeOptions = [
  { value: 1, label: '生产型' },
  { value: 2, label: '贸易型' }
]
const statusOptions = [
  { value: 1, label: '正常合作' },
  { value: 2, label: '暂停合作' },
  { value: 3, label: '终止合作' },
  { value: 4, label: '潜在供应商' }
]

const validateForm = (): boolean => {
  errors.value = {}
  
  if (!form.supplierName.trim()) {
    errors.value.supplierName = '请输入供应商名称'
  }
  if (!form.contactPerson.trim()) {
    errors.value.contactPerson = '请输入联系人'
  }
  if (!form.contactPhone.trim()) {
    errors.value.contactPhone = '请输入联系电话'
  } else if (!/^1[3-9]\d{9}$/.test(form.contactPhone)) {
    errors.value.contactPhone = '请输入正确的手机号码'
  }
  if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    errors.value.email = '请输入正确的邮箱格式'
  }
  if (form.bankAccount && !/^\d{16,19}$/.test(form.bankAccount)) {
    errors.value.bankAccount = '请输入正确的银行账号'
  }
  
  return Object.keys(errors.value).length === 0
}

const generateSupplierCode = () => {
  const timestamp = Date.now().toString().slice(-6)
  form.supplierCode = `SUP${timestamp}`
}

const handleSubmit = async () => {
  if (!validateForm()) {
    return
  }
  
  if (!form.supplierCode) {
    generateSupplierCode()
  }
  
  loading.value = true
  try {
    const response = await fetch('/api/supplier', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form)
    })
    const data = await response.json()
    if (data.code === 200) {
      alert('供应商创建成功')
      router.push('/supplier')
    } else {
      alert(data.message || '创建失败')
    }
  } catch {
    alert('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  router.push('/supplier')
}
</script>

<template>
  <div class="supplier-create-page">
    <div class="page-header">
      <button class="back-btn" @click="handleCancel">← 返回</button>
      <h1>新增供应商</h1>
    </div>

    <div class="form-container">
      <div class="form-section">
        <h3>基本信息</h3>
        <div class="form-grid">
          <div class="form-item">
            <label>供应商编码</label>
            <div class="input-with-btn">
              <input 
                v-model="form.supplierCode"
                type="text"
                placeholder="系统自动生成"
                readonly
              />
              <button class="generate-btn" @click="generateSupplierCode">自动生成</button>
            </div>
          </div>
          <div class="form-item required">
            <label>供应商名称 <span class="required-mark">*</span></label>
            <input 
              v-model="form.supplierName"
              type="text"
              placeholder="请输入供应商名称"
            />
            <span v-if="errors.supplierName" class="error">{{ errors.supplierName }}</span>
          </div>
          <div class="form-item">
            <label>简称</label>
            <input 
              v-model="form.shortName"
              type="text"
              placeholder="请输入简称"
            />
          </div>
          <div class="form-item">
            <label>供应商类型</label>
            <select v-model="form.supplierType">
              <option v-for="type in typeOptions" :key="type.value" :value="type.value">
                {{ type.label }}
              </option>
            </select>
          </div>
          <div class="form-item">
            <label>供应商等级</label>
            <select v-model="form.supplierLevel">
              <option v-for="level in levelOptions" :key="level" :value="level">
                {{ level }}级
              </option>
            </select>
          </div>
          <div class="form-item">
            <label>合作状态</label>
            <select v-model="form.cooperationStatus">
              <option v-for="status in statusOptions" :key="status.value" :value="status.value">
                {{ status.label }}
              </option>
            </select>
          </div>
        </div>
      </div>

      <div class="form-section">
        <h3>联系信息</h3>
        <div class="form-grid">
          <div class="form-item required">
            <label>联系人 <span class="required-mark">*</span></label>
            <input 
              v-model="form.contactPerson"
              type="text"
              placeholder="请输入联系人姓名"
            />
            <span v-if="errors.contactPerson" class="error">{{ errors.contactPerson }}</span>
          </div>
          <div class="form-item required">
            <label>联系电话 <span class="required-mark">*</span></label>
            <input 
              v-model="form.contactPhone"
              type="text"
              placeholder="请输入联系电话"
            />
            <span v-if="errors.contactPhone" class="error">{{ errors.contactPhone }}</span>
          </div>
          <div class="form-item">
            <label>邮箱</label>
            <input 
              v-model="form.email"
              type="text"
              placeholder="请输入邮箱"
            />
            <span v-if="errors.email" class="error">{{ errors.email }}</span>
          </div>
          <div class="form-item">
            <label>省份</label>
            <input 
              v-model="form.province"
              type="text"
              placeholder="请输入省份"
            />
          </div>
          <div class="form-item">
            <label>城市</label>
            <input 
              v-model="form.city"
              type="text"
              placeholder="请输入城市"
            />
          </div>
          <div class="form-item">
            <label>详细地址</label>
            <input 
              v-model="form.address"
              type="text"
              placeholder="请输入详细地址"
            />
          </div>
        </div>
      </div>

      <div class="form-section">
        <h3>财务信息</h3>
        <div class="form-grid">
          <div class="form-item">
            <label>开户银行</label>
            <input 
              v-model="form.bankName"
              type="text"
              placeholder="请输入开户银行"
            />
          </div>
          <div class="form-item">
            <label>银行账号</label>
            <input 
              v-model="form.bankAccount"
              type="text"
              placeholder="请输入银行账号"
            />
            <span v-if="errors.bankAccount" class="error">{{ errors.bankAccount }}</span>
          </div>
          <div class="form-item">
            <label>税号</label>
            <input 
              v-model="form.taxNumber"
              type="text"
              placeholder="请输入纳税人识别号"
            />
          </div>
        </div>
      </div>

      <div class="form-section">
        <h3>其他信息</h3>
        <div class="form-item full-width">
          <label>备注</label>
          <textarea 
            v-model="form.remark"
            placeholder="请输入备注信息"
            rows="3"
          ></textarea>
        </div>
      </div>

      <div class="form-actions">
        <button class="cancel-btn" @click="handleCancel">取消</button>
        <button 
          class="submit-btn"
          :disabled="loading"
          @click="handleSubmit"
        >
          {{ loading ? '提交中...' : '提交' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.supplier-create-page {
  padding: 20px;
  background: #f5f7fa;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;

  .back-btn {
    padding: 8px 15px;
    background: #fff;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    cursor: pointer;
  }

  h1 {
    font-size: 20px;
    font-weight: 600;
    color: #333;
  }
}

.form-container {
  background: #fff;
  border-radius: 8px;
  padding: 20px;

  .form-section {
    margin-bottom: 25px;

    h3 {
      font-size: 14px;
      font-weight: 600;
      color: #333;
      margin-bottom: 15px;
      padding-bottom: 10px;
      border-bottom: 1px solid #ebedf0;
    }

    .form-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 20px;
    }

    .form-item {
      display: flex;
      flex-direction: column;

      &.full-width {
        grid-column: 1 / -1;
      }

      &.required .required-mark {
        color: #f44;
      }

      label {
        font-size: 12px;
        color: #666;
        margin-bottom: 8px;
      }

      input, select, textarea {
        padding: 10px 12px;
        border: 1px solid #dcdfe6;
        border-radius: 4px;
        font-size: 14px;

        &:focus {
          border-color: #1988fa;
        }

        &[readonly] {
          background: #f7f8fa;
        }
      }

      .input-with-btn {
        display: flex;
        gap: 10px;

        input {
          flex: 1;
        }

        .generate-btn {
          padding: 10px 15px;
          background: #1988fa;
          color: #fff;
          border: none;
          border-radius: 4px;
          cursor: pointer;
        }
      }

      .error {
        font-size: 12px;
        color: #f44;
        margin-top: 5px;
      }
    }
  }

  .form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 15px;
    padding-top: 20px;
    border-top: 1px solid #ebedf0;

    .cancel-btn, .submit-btn {
      padding: 12px 30px;
      border-radius: 4px;
      cursor: pointer;
    }

    .cancel-btn {
      background: #f7f8fa;
      color: #333;
      border: 1px solid #dcdfe6;
    }

    .submit-btn {
      background: #1988fa;
      color: #fff;
      border: none;

      &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
    }
  }
}
</style>