<template>
  <div class="contacts-management">
    <div class="contacts-header">
      <h4 class="contacts-title">联系人列表</h4>
      <ar-button type="primary" size="small" @click="showAddDialog = true">
        <i class="el-icon-plus"></i>
        新增联系人
      </ar-button>
    </div>
    
    <div class="contacts-list">
      <div v-if="contacts.length === 0" class="empty-state">
        <i class="el-icon-user"></i>
        <p>暂无联系人信息</p>
        <ar-button type="text" @click="showAddDialog = true">添加第一个联系人</ar-button>
      </div>
      
      <div v-else>
        <div v-for="contact in contacts" :key="contact.id" class="contact-card">
          <div class="contact-info">
            <div class="contact-header">
              <span class="contact-name">{{ contact.name }}</span>
              <span v-if="contact.isPrimary" class="primary-badge">主联系人</span>
              <span class="contact-position">{{ contact.position }}</span>
            </div>
            
            <div class="contact-details">
              <div class="contact-detail">
                <i class="el-icon-phone"></i>
                <span>{{ contact.phone }}</span>
              </div>
              <div class="contact-detail">
                <i class="el-icon-message"></i>
                <span>{{ contact.email }}</span>
              </div>
            </div>
          </div>
          
          <div class="contact-actions">
            <ar-button type="text" size="small" @click="editContact(contact)">
              编辑
            </ar-button>
            <ar-button type="text" size="small" @click="setAsPrimary(contact.id)" v-if="!contact.isPrimary">
              设为主联系人
            </ar-button>
            <ar-button type="text" size="small" @click="deleteContact(contact.id)" style="color: #f56c6c;">
              删除
            </ar-button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 添加/编辑联系人弹窗 -->
    <ar-dialog
      v-model="showAddDialog"
      :title="isEditing ? '编辑联系人' : '新增联系人'"
      width="500px"
    >
      <div class="contact-form">
        <div class="form-row">
          <div class="form-field">
            <label class="form-label required">姓名</label>
            <ar-input
              v-model="currentContact.name"
              placeholder="请输入联系人姓名"
              :maxlength="20"
              clearable
            />
          </div>
          
          <div class="form-field">
            <label class="form-label required">职位</label>
            <ar-input
              v-model="currentContact.position"
              placeholder="请输入职位"
              :maxlength="30"
              clearable
            />
          </div>
        </div>
        
        <div class="form-row">
          <div class="form-field">
            <label class="form-label required">手机号</label>
            <ar-input
              v-model="currentContact.phone"
              placeholder="请输入手机号码"
              :maxlength="11"
              clearable
            />
          </div>
          
          <div class="form-field">
            <label class="form-label required">邮箱</label>
            <ar-input
              v-model="currentContact.email"
              placeholder="请输入邮箱地址"
              :maxlength="100"
              clearable
              type="email"
            />
          </div>
        </div>
        
        <div class="form-row">
          <div class="form-field">
            <label class="form-label">设为默认联系人</label>
            <ar-switch v-model="currentContact.isPrimary" />
          </div>
        </div>
      </div>
      
      <template #footer>
        <ar-button @click="showAddDialog = false">取消</ar-button>
        <ar-button type="primary" @click="saveContact" :disabled="!isContactFormValid">
          保存
        </ar-button>
      </template>
    </ar-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue';
import { ArButton, ArInput, ArDialog, ArSwitch } from '@ar/components';

interface Contact {
  id: string;
  name: string;
  position: string;
  phone: string;
  email: string;
  isPrimary: boolean;
}

interface Props {
  contacts: Contact[];
}

const props = defineProps<Props>();
const emit = defineEmits(['update']);

const showAddDialog = ref(false);
const isEditing = ref(false);
const currentContact = reactive<Contact>({
  id: '',
  name: '',
  position: '',
  phone: '',
  email: '',
  isPrimary: false
});

const contacts = computed(() => props.contacts);

const isContactFormValid = computed(() => {
  return (
    currentContact.name.trim() &&
    currentContact.position.trim() &&
    currentContact.phone.trim() &&
    currentContact.email.trim()
  );
});

const editContact = (contact: Contact) => {
  Object.assign(currentContact, contact);
  isEditing.value = true;
  showAddDialog.value = true;
};

const saveContact = () => {
  if (!isContactFormValid.value) return;
  
  const updatedContacts = [...contacts.value];
  
  if (isEditing.value) {
    // 更新联系人
    const index = updatedContacts.findIndex(c => c.id === currentContact.id);
    if (index !== -1) {
      // 如果设为默认，先取消其他默认联系人
      if (currentContact.isPrimary) {
        updatedContacts.forEach(c => {
          if (c.id !== currentContact.id) c.isPrimary = false;
        });
      }
      updatedContacts[index] = { ...currentContact };
    }
  } else {
    // 新增联系人
    const newContact = {
      ...currentContact,
      id: Date.now().toString()
    };
    
    // 如果设为默认，先取消其他默认联系人
    if (newContact.isPrimary) {
      updatedContacts.forEach(c => { c.isPrimary = false; });
    }
    
    updatedContacts.push(newContact);
  }
  
  emit('update', updatedContacts);
  resetContactForm();
  showAddDialog.value = false;
};

const setAsPrimary = (contactId: string) => {
  const updatedContacts = contacts.value.map(contact => ({
    ...contact,
    isPrimary: contact.id === contactId
  }));
  
  emit('update', updatedContacts);
};

const deleteContact = (contactId: string) => {
  const updatedContacts = contacts.value.filter(contact => contact.id !== contactId);
  
  // 如果删除的是默认联系人，设置第一个联系人为默认
  const deletedContact = contacts.value.find(c => c.id === contactId);
  if (deletedContact?.isPrimary && updatedContacts.length > 0) {
    updatedContacts[0].isPrimary = true;
  }
  
  emit('update', updatedContacts);
};

const resetContactForm = () => {
  currentContact.id = '';
  currentContact.name = '';
  currentContact.position = '';
  currentContact.phone = '';
  currentContact.email = '';
  currentContact.isPrimary = false;
  isEditing.value = false;
};
</script>

<style lang="scss" scoped>
.contacts-management {
  .contacts-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    .contacts-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin: 0;
    }
  }
  
  .contacts-list {
    .empty-state {
      text-align: center;
      padding: 40px 20px;
      color: #909399;
      
      i {
        font-size: 48px;
        margin-bottom: 16px;
        color: #dcdfe6;
      }
      
      p {
        margin: 8px 0 16px;
        font-size: 14px;
      }
    }
    
    .contact-card {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      background-color: #f9fafc;
      border-radius: 8px;
      margin-bottom: 12px;
      transition: background-color 0.2s ease;
      
      &:hover {
        background-color: #f0f2f5;
      }
      
      .contact-info {
        flex: 1;
        
        .contact-header {
          display: flex;
          align-items: center;
          margin-bottom: 8px;
          
          .contact-name {
            font-size: 16px;
            font-weight: 500;
            color: #303133;
            margin-right: 8px;
          }
          
          .primary-badge {
            background-color: #ecf5ff;
            color: #409eff;
            font-size: 12px;
            padding: 2px 8px;
            border-radius: 4px;
            margin-right: 8px;
          }
          
          .contact-position {
            font-size: 14px;
            color: #909399;
          }
        }
        
        .contact-details {
          display: flex;
          gap: 20px;
          
          .contact-detail {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 14px;
            color: #606266;
            
            i {
              color: #909399;
            }
          }
        }
      }
      
      .contact-actions {
        display: flex;
        gap: 8px;
      }
    }
  }
  
  .contact-form {
    .form-row {
      display: flex;
      gap: 20px;
      margin-bottom: 20px;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .form-field {
        flex: 1;
        
        .form-label {
          font-size: 14px;
          color: #606266;
          margin-bottom: 8px;
          display: block;
          
          &.required::before {
            content: '*';
            color: #f56c6c;
            margin-right: 4px;
          }
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .contact-card {
    flex-direction: column;
    align-items: flex-start !important;
    
    .contact-actions {
      margin-top: 12px;
      width: 100%;
      justify-content: flex-end;
    }
  }
  
  .contact-form {
    .form-row {
      flex-direction: column;
      gap: 16px !important;
    }
  }
}
</style>